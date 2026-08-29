#!/usr/bin/env bash
# GT6 Brain MCP 服务（Streamable HTTP 常驻守护）。
# 用法: tools/gt6_brain_server.sh {start|stop|restart|status}
set -euo pipefail

PORT="${GT6_BRAIN_PORT:-8939}"
TOOLS_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG="$TOOLS_DIR/../tmp/index/brain-server.log"
PIDFILE="$TOOLS_DIR/../tmp/index/brain-server.pid"

is_up() { curl -s -m 2 "http://127.0.0.1:$PORT/health" > /dev/null 2>&1; }

case "${1:-start}" in
  start)
    if is_up; then echo "already running on :$PORT"; exit 0; fi
    mkdir -p "$(dirname "$LOG")"
    nohup "$TOOLS_DIR/.venv/bin/python" "$TOOLS_DIR/gt6_rag/server.py" >> "$LOG" 2>&1 &
    echo $! > "$PIDFILE"
    for _ in $(seq 1 20); do is_up && break; sleep 0.5; done
    is_up && echo "started (pid $(cat "$PIDFILE")) :$PORT" || { echo "FAILED to start, see $LOG"; exit 1; }
    ;;
  stop)
    if [[ -f "$PIDFILE" ]]; then
      kill "$(cat "$PIDFILE")" 2>/dev/null || true
      rm -f "$PIDFILE"
    fi
    # 兜底：按端口清残留
    pkill -f "gt6_rag/server.py" 2>/dev/null || true
    echo "stopped"
    ;;
  restart) "$0" stop; sleep 1; "$0" start ;;
  status)
    if is_up; then curl -s "http://127.0.0.1:$PORT/health"; echo; else echo "down"; exit 1; fi
    ;;
  *) echo "usage: $0 {start|stop|restart|status}"; exit 1 ;;
esac
