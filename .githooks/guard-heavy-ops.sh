#!/usr/bin/env bash
# PreToolUse(Bash) 钩子：拦截未经 tools/gt6testgate.py 门禁的 gradle 调用。
# （test-gating-v3，2026-09-29 用户追补裁定：所有 gradle 必须走脚本——
#   python3 tools/gt6testgate.py run -- <原命令>，门禁=runner，预测+采样闭环。）
# 输入: stdin 上的 JSON（含 tool_input.command）；输出 JSON deny + 指路。
#
# 作用域自检（无需硬编码仓库根路径，可安全注册到用户级 ~/.zcode/cli/config.json，
# 形态同 .githooks/guard-commit.sh）：
#   1. 仅当 cwd 所在 git 仓库的主仓库根存在 tools/gt6testgate.py（opt-in 标记，
#      门禁工具在才拦——handler 不存在就等于没装）时生效；
#   2. 作用范围 = 主仓库根 + 两个 worktree 约定目录（../MGT6GA-trees/ 实际布局
#      与 ../<仓库名>-trees/ 通用形，guard-commit.sh 只认后者）；
#   3. GITHUB_ACTIONS 环境变量置位即零开销透传（CI 跑不了 WSL 门禁）。
# 因此挂用户级等价于项目级约束，且不影响其他仓库。
#
# ponytail: 子串匹配会误伤 echo/grep 等提到 gradle 字样的只读命令——
# fail-closed 是有意的（纪律闸不是安全边界）；命中后照指路改走 gate 即可。
input=$(cat)

[ -n "$GITHUB_ACTIONS" ] && exit 0

cmd=$(printf '%s' "$input" | python3 -c '
import json,sys
try:
    d=json.load(sys.stdin)
    print(d.get("tool_input",{}).get("command",""))
except Exception:
    print("")
' 2>/dev/null)

case "$cmd" in
  *gradle*) ;;
  *) exit 0 ;;                        # 不涉 gradle，无关
esac
case "$cmd" in
  *gt6testgate.py*) exit 0 ;;         # 已走门禁
esac

# 解析主仓库根（兼容 worktree 内执行）；不在 git 仓库 = 不管
common=$(git rev-parse --git-common-dir 2>/dev/null) || exit 0
[ -n "$common" ] || exit 0
case "$common" in
  /*) ;;
  *) common="$PWD/$common" ;;
esac
MAIN_ROOT=$(cd "$common/.." && pwd)

# opt-in 标记：门禁工具存在才拦截
[ -f "$MAIN_ROOT/tools/gt6testgate.py" ] || exit 0

TREES_DIR="$(dirname "$MAIN_ROOT")/MGT6GA-trees"
TREES_DIR_GENERIC="$(dirname "$MAIN_ROOT")/$(basename "$MAIN_ROOT")-trees"
case "$PWD" in
  "$MAIN_ROOT"|"$MAIN_ROOT"/*) ;;
  "$TREES_DIR"|"$TREES_DIR"/*) ;;
  "$TREES_DIR_GENERIC"|"$TREES_DIR_GENERIC"/*) ;;
  *) exit 0 ;;
esac

printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"gradle 调用必须走统一门禁（test-gating-v3 硬约束，2026-09-29 WSL 崩溃裁定）：python3 tools/gt6testgate.py run -- <原命令>。门禁会预测内存（台账估算）、占并发槽、采样峰值 RSS 回写台账并透传退出码；派发前可先 --dry-run 看预测。"}}'
exit 0
