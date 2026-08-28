---
name: gt6-workflow
description: "GT6 项目的 Git 工作流铁律：蓝领 Agent 必须在独立 worktree 工作、用 GPG 签名 + Signoff 提交、把 commit hash 返还主 Agent 派发 review-merge Agent 审查合并。当要写代码、做提交、创建分支、处理合并冲突、或用户提到 'worktree'、'提交规范'、'开始编码任务' 时使用。"
---

# GT6 编码工作流（worktree + GPG + 并行 PR）

## 角色与流程（开源项目式并行）

```
主 Agent（组织者，登记任务板 state key="tasks"）
   │  ① 下发任务卡（SLUG/SPEC/EVIDENCE/FILES_SCOPE/ACCEPTANCE/BRANCH）
   ▼
蓝领 Coder Agent（可并行 2~4 个，文件域互不重叠）
   ② git worktree add ../MGT6GA-trees/<SLUG> -b work/<SLUG>
   ③ 小步实现，每步编译/测试通过
   ④ git commit -S -s（GPG 签名 + Signoff + Task: 行）
   ⑤ 返还 commit hash + 变更摘要 + 自测结果
   ▼
Review-Merge Agent（串行，main 全局锁）
   ⑥ 审查 work/<SLUG>：GPG 核验 / 架构红线 / 语义对照 1.7.10 / 编译
   ⑦ 冲突：worktree 内 rebase main 解决（语义冲突回查老代码）
   ⑧ --no-ff 合入 main → 清理 worktree → 落账
   ⑨ 其余在途分支在下轮 review 前 rebase main
```

## 硬约束（违反即返工）

1. **禁止在 main 工作区直接提交代码**——main 只接受 review-merge 的合并。
2. **禁止绕过 GPG**：永远 `git commit -S -s`。项目配置 `commit.gpgsign=true` +
   `core.hooksPath=.githooks`；ZCode PreToolUse 钩子会拦截不带 `--gpg-sign` 的 commit。
3. **提交消息格式**（commit-msg 钩子校验）：
   ```
   <type>(<scope>): <一句话主题>

   <变更要点：为什么这么改；引用的检索证据 文件:行号>
   Task: <SLUG>
   Signed-off-by: brokestar233 <3765589194@qq.com>
   ```
   type ∈ feat|fix|refactor|docs|chore|test|port|arch|qa|research
4. **原子提交**：一个提交一个意图。改一个功能 → 编译 → 提交。禁止 20 文件大重构。
5. **文件域隔离**：只碰任务卡 FILES_SCOPE 内的文件（并行安全的前提）。
6. **编译验证**：提交前必须编译通过；带病提交 = 违纪。
7. **回滚**：死循环 → `git worktree remove` + 删分支 + status=aborted，重新拆卡。

## 常用命令速查

```bash
# 开工
git worktree add ../MGT6GA-trees/<SLUG> -b work/<SLUG>
# 唯一合法的提交姿势
git add <files> && git commit -S -s -m "port(materials): 描述

Task: <SLUG>
Signed-off-by: brokestar233 <3765589194@qq.com>"
# 收工清理（合入后由 review-merge 执行）
git worktree remove ../MGT6GA-trees/<SLUG> && git branch -d work/<SLUG>
```

## 冲突解决（Review-Merge 专用）

- 先 `git log main..work/<SLUG>` 理解提交串，再逐个 rebase。
- GT6 语义冲突（同一老代码被两个分支各自翻译）必须回查
  `search_code(sources=["gt6"])` 的 1.7.10 原始语义再裁决，记入 KG。
- 解决后所有提交仍须 `git verify-commit` 通过。
