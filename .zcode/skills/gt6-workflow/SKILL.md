---
name: gt6-workflow
description: "GT6 项目的 Git 工作流铁律：蓝领 Agent 必须在独立 worktree 工作、用 GPG 签名 + Signoff 提交、把 commit hash 返还主 Agent 派发 review-merge Agent 审查合并。当要写代码、做提交、创建分支、处理合并冲突、或用户提到 'worktree'、'提交规范'、'开始编码任务' 时使用。"
---

# GT6 编码工作流（worktree + GPG）

## 角色与流程

```
主 Agent（派发任务）
   │  ① 下发任务卡（含任务名 task-slug）
   ▼
蓝领 Coder Agent
   ② git worktree add ../MGT6GA-trees/<task-slug> -b work/<task-slug>
   ③ 在 worktree 内小步实现，每步编译/测试通过
   ④ 提交：git commit -S -s（GPG 签名 + Signoff，消息格式见下）
   ⑤ 把 commit hash + worktree 路径 + 变更摘要返还主 Agent
   ▼
Review-Merge Agent（主 Agent 另行派发）
   ⑥ 审查 work/<task-slug> 分支：代码、约束、架构红线
   ⑦ 冲突：在 worktree 内 rebase main 解决，禁止 -f 推送
   ⑧ 通过后 fast-forward/--no-ff 合入 main，删除 worktree
```

## 硬约束（违反即返工）

1. **禁止在 main 工作区直接提交代码**——main 只接受 review-merge Agent 的合并。
2. **禁止绕过 GPG**：永远 `git commit -S -s`。项目已配置 `commit.gpgsign=true` 与
   `core.hooksPath=.githooks`；PreToolUse 钩子会拦截不带 `--gpg-sign` 的 `git commit`。
3. **提交消息格式**（commit-msg 钩子校验）：
   ```
   <type>(<scope>): <一句话主题>

   <变更要点，为什么这么改；引用了哪些检索结果>
   Task: <任务名>
   Signed-off-by: brokestar233 <3765589194@qq.com>
   ```
   type ∈ feat|fix|refactor|docs|chore|test|port|arch|qa|research
4. **原子提交**：一个提交只做一件事。禁止一次重构 20 个文件；改一个功能 → 编译 → 提交。
5. **worktree 路径约定**：`../MGT6GA-trees/<task-slug>`（在仓库根同级目录）。
6. **编译验证**：提交前必须通过编译或语法检查；带病提交 = 违纪。
7. **回滚**：重构陷入死循环时，`git worktree remove` 后基于 main 重新思考，不要在烂摊子上修补。

## 常用命令速查

```bash
# 开工
git worktree add ../MGT6GA-trees/<task-slug> -b work/<task-slug>
# 提交（唯一合法姿势）
git add <files> && git commit -S -s -m "port(materials): 描述

Task: <task-slug>
Signed-off-by: brokestar233 <3765589194@qq.com>"
# 收工清理（合入后）
git worktree remove ../MGT6GA-trees/<task-slug> && git branch -d work/<task-slug>
```

## 冲突解决（Review-Merge Agent 专用）

- 先 `git log main..work/<slug>` 理解提交串，再逐个 rebase。
- GT6 相关语义冲突（同一老代码被两个分支各自翻译）必须回查 `search_code` 里的 1.7.10 原始语义再裁决，记录进 KG。
- 解决后仍需 `git verify-commit` 全部通过才能合入。
