"""Embedding client.

Two backends, selected by config `backend`:
  - "local":  llama-server (llama.cpp) OpenAI-compatible endpoint on localhost
              (Qwen3-Embedding-4B GGUF q8, HIP/ROCm). Fast, free, deterministic.
  - "remote": ai.segfault.top relay (fallback).

Local llama-server returns L2-normalized vectors (--embd-normalize 2), so
cosine similarity == dot product. MRL truncation (client-side head slicing +
renormalize) is still applied per config `truncate_dims`.
"""
from __future__ import annotations

import json
import math
import os
import random
import time
import urllib.error
import urllib.request
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parent.parent
CONFIG_PATH = TOOLS_DIR / "config.json"

_config: dict | None = None
# 直连本地服务必须绕过系统代理（本机曾因 http_proxy 环境吃 502）
_opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def load_config() -> dict:
    global _config
    if _config is None:
        cfg_path = Path(os.environ.get("GT6_RAG_CONFIG", CONFIG_PATH))
        with open(cfg_path, encoding="utf-8") as f:
            _config = json.load(f)["embedding"]
    return _config


def _post_batch(texts: list[str], retries: int = 7) -> list[list[float]]:
    cfg = load_config()
    payload = json.dumps({"model": cfg["model"], "input": texts}).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if cfg.get("api_key"):
        headers["Authorization"] = f"Bearer {cfg['api_key']}"
    url = cfg["base_url"].rstrip("/") + "/embeddings"
    last_err: Exception | None = None
    for attempt in range(retries):
        req = urllib.request.Request(url, data=payload, headers=headers, method="POST")
        try:
            with _opener.open(req, timeout=240) as resp:
                data = json.loads(resp.read().decode("utf-8"))
            return [d["embedding"] for d in sorted(data["data"], key=lambda d: d["index"])]
        except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, OSError,
                json.JSONDecodeError, KeyError) as e:
            last_err = e
            time.sleep(min(60, (2 ** attempt) * 2) + random.random() * 2)
    raise RuntimeError(f"embedding request failed after {retries} retries: {last_err}")


def _truncate(v: list[float], dims: int) -> list[float]:
    if dims <= 0 or len(v) <= dims:
        return v
    head = v[:dims]
    norm = math.sqrt(sum(x * x for x in head)) or 1.0
    return [x / norm for x in head]


def _token_estimate(texts: list[str]) -> int:
    """Rough token estimate: ~4 chars/token for code+EN, CJK ~1.5 chars/token.

    Conservative overcount is fine (smaller batches, no failures).
    """
    import unicodedata
    cjk = sum(1 for t in texts for ch in t if unicodedata.east_asian_width(ch) in "WF")
    total_chars = sum(len(t) for t in texts)
    return int((total_chars - cjk) / 4 + cjk / 1.5)


def embed_texts(texts: list[str]) -> list[list[float]]:
    """Embed with dynamic batch sizing + pipelined concurrent requests.

    The GPU-side server (llama-server, --parallel 12) splits one request's
    inputs across slots, but consecutive HTTP requests are serialized —
    GPU idles between them. We therefore cut batches first, then fly
    `request_workers` requests concurrently (order-preserving), keeping all
    slots busy. Prefix caching (LCP) still applies per file since batches
    keep sibling chunks together.
    """
    cfg = load_config()
    dims = int(cfg.get("truncate_dims", 0))
    max_items = int(cfg.get("batch_size", 32))
    max_tokens = int(cfg.get("max_batch_tokens", 30000))
    item_cap = int(cfg.get("max_item_chars", 12000))
    workers = int(cfg.get("request_workers", 8))

    prepared: list[str] = []
    for t in texts:
        if len(t) > item_cap:  # ~3000 tokens
            head, tail = item_cap * 3 // 4, item_cap // 4
            prepared.append(t[:head] + "\n...\n" + t[-tail:])
        else:
            prepared.append(t)

    # cut batches by item count and token budget
    batches: list[list[str]] = []
    batch: list[str] = []
    batch_tokens = 0
    for t in prepared:
        est = _token_estimate([t])
        if batch and (len(batch) >= max_items or batch_tokens + est > max_tokens):
            batches.append(batch)
            batch, batch_tokens = [], 0
        batch.append(t)
        batch_tokens += est
    if batch:
        batches.append(batch)

    if workers <= 1 or len(batches) <= 1:
        raw = [_post_batch(b) for b in batches]
    else:
        import concurrent.futures as _cf
        with _cf.ThreadPoolExecutor(max_workers=workers) as ex:
            raw = list(ex.map(_post_batch, batches))  # ex.map preserves order

    out: list[list[float]] = []
    for group in raw:
        out.extend(_truncate(v, dims) for v in group)
    return out


def embed_query(text: str) -> list[float]:
    cfg = load_config()
    instruction = cfg.get("query_instruction", "")
    dims = int(cfg.get("truncate_dims", 0))
    q = instruction + text if instruction else text
    return _truncate(_post_batch([q])[0], dims)
