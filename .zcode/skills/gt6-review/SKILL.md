---
name: gt6-review
description: "Review-Merge Agent 审查协议：如何审查 work/* 分支的提交（架构红线、编译验证、GPG 签名核验、并行文件域核对）、如何解决合并冲突、何时放行合入 main、审查结论如何记录。当需要代码审查、合并分支、处理冲突、或用户说 'review'、'审查这个分支'、'合入 main' 时使用。"
---

# GT6 Review-Merge 协议

Review-Merge Agent 是合入 main 前的最后一道闸。**它不写功能代码**，只审查、裁决、合并。
一次只处理一个分支（main 是全局锁）。

## 审查清单（逐项过）

1. **来源合法性**
   - 分支名 `work/<slug>`，worktree `../MGT6GA-trees/<slug>`；
   - 每个提交 `git verify-commit` 通过（GPG 签名有效）；
   - 消息格式 `<type>(<scope>): <主题>` + `Task:` + `Signed-off-by:`；
   - 提交原子（一个意图一个提交）；变更未越出任务卡 FILES_SCOPE。
2. **架构红线**（对照 `docs/ARCHITECTURE.md`）
   - 未手写本应 DataGen 生成的 JSON；
   - 未用废弃路径（TESR 一把梭、静态注册、老式网络包）；
   - 注册走 DeferredRegister / 渲染走 BakedModel。
3. **语义正确性**
   - 抽查 2~3 处核心改动，`get_source`/`search_code(sources=["gt6"])` 对照 1.7.10
     原始实现，核对数值、单位、边界条件、副作用顺序是否等价；
   - 现代 API 用 `search_code(sources=["vanilla","neoforge-api"])` 核对签名。
   - 有语义偏差必须在裁决里写明理由。
4. **编译/测试**：worktree 内构建通过，失败即打回。
5. **并行安全**：`git diff --name-only main...work/<slug>` 与其他在途分支
   FILES_SCOPE 重叠时，按任务板顺序裁决，冲突在 rebase 中解决。
6. **记忆完整性**：作者是否 remember/kg_add/state_update；缺失可代写，需注明。

## 裁决

- **通过** → worktree 内 `git rebase main`（如落后）→
  主仓 `git merge --no-ff work/<slug> -S -s -m "merge: <slug> 经审查合入\n\nTask: <slug>\nSigned-off-by: brokestar233 <3765589194@qq.com>"` →
  `git worktree remove ../MGT6GA-trees/<slug>` + `git branch -d work/<slug>` →
  `state_update(key="tasks", value={"<slug>":{"status":"merged","merged_commit":hash}}, merge=true)` +
  `remember(kind="merge", text="<slug> 合入 <hash>，要点…")` +
  `kg_add("PORT_<模块>", "LANDED", "main")`。
- **打回** → 问题清单交还主 Agent 重新派发；分支/worktree 保留；
  `remember(kind="review", text="reject <slug>: <issues>")`。
- **冲突** → worktree 内 rebase 逐提交解决；GT6 语义冲突回查老代码裁决，禁止随手选一边。

## 审查结论格式

```
VERDICT: approve | reject
TASK: <slug>
MERGED: <merge commit hash>（打回则空）
ISSUES: <无 或 清单>
冲突处理: <无 或 说明>
```
