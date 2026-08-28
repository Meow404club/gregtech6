#!/usr/bin/env bash
# GT6 精排服务：llama-server rerank 端点 + Qwen3-Reranker-0.6B Q8
# 与嵌入服务（端口 8937）并存；同族 tokenizer，原生 /v1/rerank 端点。
set -euo pipefail

LLAMA_BIN="${LLAMA_BIN:-/home/brokestar/workspace/meow-translator/projects/.runtime/llama-cpp-gfx1100-build-full/bin/llama-server}"
MODEL="${MODEL:-/home/brokestar/workspace/MGT6GA/gregtech6/tmp/models/Qwen3-Reranker-0.6B.Q8_0.gguf}"
PORT="${PORT:-8938}"
HOST="${HOST:-127.0.0.1}"
# 调用量小 → 少槽多上下文：8 槽×3k 改 4 槽×6k，单请求可吞 ~2k token 的 (query+候选)
SLOTS="${SLOTS:-4}"
CTX_PER_SLOT="${CTX_PER_SLOT:-6144}"
CTX=$((SLOTS * CTX_PER_SLOT))
UBATCH="${UBATCH:-4096}"   # 物理批上限：rerank 输入是整段 (query+doc)，必须 ≥ 单输入 token 数

export HSA_ENABLE_DXG_DETECTION=1
export KFD_NPS_RELAX=1
export PATH="$PATH:/opt/rocm/bin"

exec "$LLAMA_BIN" \
  --model "$MODEL" \
  --rerank \
  --parallel "$SLOTS" \
  --ctx-size "$CTX" \
  -b "$UBATCH" \
  -ub "$UBATCH" \
  --port "$PORT" \
  --host "$HOST" \
  --threads 8 \
  --flash-attn on \
  --jinja
