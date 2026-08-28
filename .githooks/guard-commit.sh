#!/usr/bin/env bash
# PreToolUse(Bash) 钩子：拦截不带 --gpg-sign 的 git commit。
# 输入: stdin 上的 JSON（含 tool_input.command）；输出 JSON 决定放行/拦截。
# 退出码 2 = 拦截。只认 git commit；git cz / commitizen 等别名不在管辖范围。
input=$(cat)
cmd=$(printf '%s' "$input" | /home/brokestar/workspace/MGT6GA/gregtech6/tools/.venv/bin/python -c '
import json,sys
try:
    d=json.load(sys.stdin)
    print(d.get("tool_input",{}).get("command",""))
except Exception:
    print("")
' 2>/dev/null)

case "$cmd" in
  *"git commit"*)
    case "$cmd" in
      *--gpg-sign*|*-S\ *|*-S" "*|*"-S"*) exit 0 ;;
      *)
        printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"提交必须 GPG 签名：请使用 git commit --gpg-sign -S（并在 commit message 中包含 Signoff/任务标签）。这是本项目的硬约束。"}}'
        exit 0
        ;;
    esac
    ;;
  *)
    exit 0
    ;;
esac
