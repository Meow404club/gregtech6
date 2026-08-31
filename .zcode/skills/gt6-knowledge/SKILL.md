---
name: gt6-knowledge
description: "GT6 知识与状态记忆的写入规范：remember/recall 语义记忆、KG 三元组（自动嵌入）、state 账本、任务板。当完成一个研究结论、一个里程碑、发现一个 bug、做出一个架构决策，或用户要求 '记录'、'更新状态'、'记住这个结论' 时使用。"
---

# GT6 知识写入规范

记忆分三层，职责不同（全部通过 MCP `gt6-brain`）：

| 层 | 工具 | 记什么 | 特性 |
|---|---|---|---|
| **语义记忆** | `remember` / `recall` / `forget` | 结论性知识：研究结论、决策理由、bug 根因、交接要点 | 自动嵌入；≥0.97 去重、≥0.90 自动 supersede（旧行失效+新行 supersedes_id，演化链可查）、0.80~0.90 文本合并；recall 带时效衰减 |
| **知识图谱** | `kg_add` / `kg_invalidate` / `kg_search` / `kg_query` / `kg_prune` / `kg_stats` | 结构关系：类↔职责、老↔新映射、机器↔纹理 | 三元组自动嵌入；双时态（关系过时用 kg_invalidate 置 invalid_at 保留历史）；kg_query 必须带过滤（禁全图导出） |
| **状态账本** | `state_read` / `state_search` / `state_update` | 时间性事实：进度、TODO、任务板、已知 Bug 列表 | 精确读写，"唯一真相源"；无参=目录页，state_search 语义定位；点分前缀即 namespace，tmp.* + ttl_seconds 即临时键 |

## 语义记忆（remember）

- `kind` 规范：`decision`（架构决策）| `research`（考古结论）| `bug`（根因+修法）|
  `merge`（合入记录）| `handoff`（跨会话交接）| `lesson`（踩坑教训）。
- text 里**必须带证据**（文件:行号），因为它是会被 `recall` 语义召回的独立知识单元。
- 近同事实（相似度 ≥0.90）会自动 supersede 旧记忆——放心写修订版结论，不用怕重复。

## 知识图谱（kg_add）

命名约定：老代码 `GT6_`、现代 MC `MC_`、Forge `FORGE_`、NeoForge `NEO_`、
GTCEu `GTCEU_`、项目新代码 `PORT_`、材质 `TEX_`、配方 `RECIPE_`、材料 `MAT_`。

必须记录的关系：`MAPS_TO`（老→新，考古最重要产出）、`DOES`/`OWNS`（职责）、
`USES_TEXTURE`/`USES_MODEL`、`DEPENDS_ON`（改动波及面）、`UPGRADES_TO`/`REPLACED_BY`、
`BLOCKED_BY`（任务依赖）、`TRAPS`（陷阱→正确做法）。
**关系过时用 `kg_invalidate(src, rel, dst, reason)`**（双时态，保留"曾经成立"的历史），
不要 kg_del——那会抹掉演化链；kg_del 只用于清理错误/烟雾数据。
`node_types`：Class|Machine|Material|Texture|Recipe|Concept|Task|System。

## 状态账本（state_update）

- **namespace 约定**：点分前缀即命名空间（`tasks.<slug>` / `decisions` / `tmp.<用途>`）；
  临时键一律 `tmp.*` 并设 `ttl_seconds`（到期惰性清扫）——有界 schema 即增长控制。

- `tasks`：任务板（唯一真相源），`{"<slug>": {"status","branch","worktree","commits","owner","files_scope","note"}}`，
  status ∈ research|queued|in_progress|in_review|merged|aborted。
- `decisions`：merge 追加 `{"topic","decision","alternatives","evidence","date"}`。
- `known_bugs`：merge 追加 `{"symptom","root_cause","fix","status"}`。
- `progress`：覆盖式 `{"phase","done":[],"current","next"}`。
- 重要变更后同步镜像到 `docs/PROJECT_STATE.md` / `docs/TODO.md`。

## 读规则

任何 Agent 动手前：`recall()` + `state_read()`（无参=目录页，看哪些账本最近更新）
+ `kg_search()` 先行——已有结论不许重新考古一遍。需要某本账的内容再
`state_read(key)`（超长自动截断，最新在前）；定位不清用 `state_search(query, prefix)`。
