---
name: gt6-context-loader
description: "GT6 现代复兴计划会话上下文装载器。每次会话开始、或任何 Agent 接到任务准备动手前使用：恢复项目状态记忆（决策/TODO/已知Bug/进度），汇报检索库覆盖情况，并给出下一步行动指引。当用户提到 '恢复上下文'、'项目状态'、'继续移植工作' 或任何会话开始需要项目背景时也应触发。"
---

# GT6 上下文装载

在开始任何工作之前，按顺序执行：

1. 用 MCP 工具 `state_read` 读取全部项目状态（decisions / todo / known_bugs / progress / architecture）。
2. 用 `project_status()` 查看检索索引规模、KG 条数、worktree 列表与最近提交。
3. 读取 `docs/PROJECT_STATE.md` 与 `docs/TODO.md`（若存在）——文件版状态以 MCP state 为准，文件版只做镜像。
4. 若 `progress` 显示有未完成的里程碑，或 `todo` 非空，先向用户简报现状再继续。

## 汇报格式

```
◆ GT6 复兴计划 · 上下文恢复
· 阶段: <progress.phase>
· 最近决策: <decisions 最新 3 条>
· 已知 Bug: <known_bugs 数量> 项
· TODO: <todo 数量> 项
· 活跃 worktree: <列表>
· 建议: <下一步>
```

## 冷启动（首次 / 索引为空）

若 `project_status()` 显示索引为空：
1. 提示用户索引尚未建立。
2. 后台执行 `tools/.venv/bin/python -m gt6_rag.index all`（在 `tools/` 目录下运行），完成前不要进行需要检索的工作。
3. 索引完成后调用 `state_update` 写入初始 progress。

## 铁律提醒（装载后必须遵守）

- 不猜测任何现代 API：动手前用 `search_code` / `sym_query` / `get_source` 查证。
- 提交必须 GPG 签名且经 review-merge Agent 审查（见 gt6-workflow 技能）。
- 每完成一个子任务：`state_update` 更新状态 + `kg_add` 记录关键关系。
