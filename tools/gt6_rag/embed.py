"""Embedding client for the OpenAI-compatible /v1/embeddings endpoint.

Notes (from Qwen3-Embedding-4B model card):
- Query side carries an instruction: `Instruct: {task}\nQuery: {query}`;
  document side carries none.
- Native dim 2560, MRL-supported: we truncate client-side to
  `truncate_dims` and L2-renormalize (Matryoshka slices live at the head).
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
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {cfg['api_key']}",
    }
    last_err: Exception | None = None
    for attempt in range(retries):
        req = urllib.request.Request(
            cfg["base_url"].rstrip("/") + "/embeddings",
            data=payload, headers=headers, method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=240) as resp:
                data = json.loads(resp.read().decode("utf-8"))
            return [d["embedding"] for d in sorted(data["data"], key=lambda d: d["index"])]
        except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, OSError,
                json.JSONDecodeError, KeyError) as e:
            last_err = e
            # 网关偶发 404/断连：指数退避 + 抖动，最长约 3 分钟
            time.sleep(min(60, (2 ** attempt) * 2) + random.random() * 2)
    raise RuntimeError(f"embedding request failed after {retries} retries: {last_err}")


def _truncate(v: list[float], dims: int) -> list[float]:
    if dims <= 0 or len(v) <= dims:
        return v
    head = v[:dims]
    norm = math.sqrt(sum(x * x for x in head)) or 1.0
    return [x / norm for x in head]


def embed_texts(texts: list[str]) -> list[list[float]]:
    """Embed in batches; returns one (possibly MRL-truncated) vector per text."""
    cfg = load_config()
    bs = int(cfg.get("batch_size", 32))
    dims = int(cfg.get("truncate_dims", 0))
    out: list[list[float]] = []
    for s in range(0, len(texts), bs):
        out.extend(_truncate(v, dims) for v in _post_batch(texts[s : s + bs]))
    return out


def embed_query(text: str) -> list[float]:
    cfg = load_config()
    instruction = cfg.get("query_instruction", "")
    dims = int(cfg.get("truncate_dims", 0))
    q = instruction + text if instruction else text
    return _truncate(_post_batch([q])[0], dims)
