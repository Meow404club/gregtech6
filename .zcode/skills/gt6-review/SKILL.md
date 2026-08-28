---
name: gt6-review
description: "Review-Merge Agent 审查协议：如何审查 work/* 分支的提交（架构红线、编译验证、GPG 签名核验）、如何解决合并冲突、何时放行合入 main、审查结论如何记录。当需要代码审查、合并分支、处理冲突、或用户说 'review'、'审查这个分支'、'合入 main' 时使用。"
---

# GT6 Review-Merge 协议

Review-Merge Agent 是合入 main 前的最后一道闸。**它不写功能代码**，只审查、裁决、合并。

## 审查清单（逐项过）

1. **来源合法性**
   - 分支名 `work/<task-slug>`，worktree 位置符合约定；
   - 每个提交 `git verify-commit` 通过（GPG 签名有效）；
   - 提交消息符合 `<type>(<scope>): <主题> + Task: + Signed-off-by:` 格式；
   - 每个提交原子（一个意图一个提交），pre-commit 未被绕过。
2. **架构红线**
   - 未手写本应 DataGen 的 JSON；
   - 未使用已废弃路径（TESR 渲染一切、静态注册、老式网络包）；
   - 现代 API 用法有据可查：抽查 2~3 处调用，用 `get_source`/`search_code(sources=["vanilla","neoforge-api"])` 核对签名。
3. **语义正确性**
   - 对照 GT6 1.7.10 原始实现（`search_code(sources=["gt6"])`）核对移植语义：
     数值、单位、边界条件、副作用顺序是否等价；
   - 有语义偏差时必须在合并决定里写明理由。
4. **编译/测试**：在 worktree 里跑构建，失败即打回。
5. **记忆完整性**：作者是否更新了 state/KG；缺了可以代写，但要在结论里注明。

## 裁决

- **通过** → 在 worktree 内 `git rebase main`（如落后），确认无冲突或已解决 →
  主仓 `git merge --no-ff work/<task-slug> -S -s -m "merge: <task-slug> 经审查合入"` →
  `git worktree remove ../MGT6GA-trees/<task-slug>` → `git branch -d work/<task-slug>` →
  `state_update(key="progress", merge…)` + `kg_add("PORT_<模块>", "LANDED", "main")`。
- **打回** → 列出问题清单交还主 Agent 重新派发；分支保留。
- **冲突** → 在 worktree 内解决；涉及 GT6 语义冲突必须回查 1.7.10 原码裁决，禁止"随手选一边"。

## 记录

每次审查把结论追加进 `state_read("reviews")`：
`{"task","verdict":"approve|reject","issues":[...],"merged_commit":"<hash>","date"}`。
