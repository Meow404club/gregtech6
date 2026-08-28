---
name: gt6-knowledge
description: "GT6 知识图谱与项目状态记忆的写入规范：哪些事件必须记录进 KG、如何写 decisions/todo/known_bugs/progress、实体命名约定。当完成一个研究结论、一个里程碑、发现一个 bug、做出一个架构决策，或用户要求 '记录'、'更新状态'、'记住这个结论' 时使用。"
---

# GT6 知识写入规范

记忆分两处，职责不同：

| 载体 | 藏在哪 | 记什么 |
|---|---|---|
| **状态 state** | `state_kv` 表 | 时间性的项目事实：决策、TODO、Bug、进度、架构现状 |
| **知识图谱 KG** | `kg_nodes/kg_edges` 表 | 永久性结构关系：类↔职责、老↔新映射、机器↔纹理↔配方 |

## 状态（state_update）

- `decisions`：`merge=true` 追加 `{"topic","decision","evidence","date"}`。**每个架构选择都记**，包括否决的方案与理由。
- `todo`：`merge=true` 追加 `{"id","what","who","status"}`；状态变化用覆盖式更新整表。
- `known_bugs`：`merge=true` 追加 `{"symptom","root_cause","fix","status"}`。
- `progress`：覆盖式 `{"phase":"第N阶段-xxx","done":[...],"current":"...","next":"..."}`。
- `architecture`：覆盖式，维护当前模块树与红线清单。

## 知识图谱（kg_add）

命名约定：老代码 `GT6_` 前缀、现代 MC `MC_`、Forge `FORGE_`、NeoForge `NEO_`、GTCEu `GTCEU_`、
项目新代码 `PORT_`、材质 `TEX_`、配方 `RECIPE_`、材料 `MAT_`。

必须记录的关系（rel 用大写蛇形）：

- `MAPS_TO`：1.7.10 概念 → 现代等价物（考古最重要产出）
- `DOES` / `OWNS`：类 → 职责；机器 → 子系统
- `USES_TEXTURE` / `USES_MODEL`：方块/物品 → 资源
- `DEPENDS_ON`：模块 → 模块（用于评估改动波及面）
- `UPGRADES_TO` / `REPLACED_BY`：移植演进
- `BLOCKED_BY`：任务依赖（配合 todo）

写入时用 `node_types` 标注类型：Class|Machine|Material|Texture|Recipe|Concept|Task|System。

## 同步镜像

重大状态变更后，把 `state_render` 级别的摘要同步写进 `docs/PROJECT_STATE.md` /
`docs/TODO.md`（人可读），MCP state 为权威源，文件为镜像。

## 读规则

任何 Agent 动手前：`state_read()` + `kg_query(entity=<相关系统>)` 先行——
已有结论不许重新考古一遍。
