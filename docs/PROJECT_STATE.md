# 项目状态（镜像·维护期版 2026-09-27）

> 权威数据在 MCP `gt6-brain` 的 state/记忆/KG 里。本文件自 2026-09-27 起改为**维护期精简镜像**：
> P1-P38 逐阶段详账已蒸馏归档至 [docs/archive/ARCHIVE-2026-09-27-p1-p37.md](archive/ARCHIVE-2026-09-27-p1-p37.md)
> （阶段总表+还债期卡终态表+被归档记忆全文+删除清单）。ADR 在 docs/adr/（44+ 篇），已完成卡的过程明细随 git 历史。

## 项目

GT6（1.7.10 GregTech6）→ 现代移植：1.20.1 Forge + 1.21.1 NeoForge 双节点（Stonecutter）。
内容完成度 97-98%+，2026-09-24 发布 v0.1.0（GitHub Release，双 jar），现处**还债/维护阶段**（2026-09-26 起）。

## 当前态（2026-09-27）

- main 本地 515ac9375→（还债卡流水合入中，见 `ops.reboot-snapshot-2026-09-27`）；origin 落后——push 攒批策略=审查全部收口后一次推（ops.discipline.push-policy）。
- 在途：审查持有 10 卡 + 停摆重派 6 卡（清单见 archive md §2 与 tasks.debt-*/tasks.p37-* 活键）。
- 验证面：离线测试套件（root+mdk 双腿 cleanTest）+ RCON 组 sweep（tools/rcon/sweep.py --group <组>）；全量 sweep 只在阶段末、只 neo 腿、禁 subagent 自发。
- 统一测试门禁：ops-testgate-hard-gate 硬闸 + 30G 内存闸（ops.discipline.test-gating-v3）。

## 维护期记忆入口

| 层 | 用法 |
|---|---|
| 语义记忆 | `recall("<查询>")`（按 kind 过滤）；维护期保留 lesson/bug/decision 全量 + 2026-09-26 后 handoff/merge/research |
| state 账本 | `state_read()` 目录页（现仅 ~230 键：known_bugs*/ops.discipline*/decisions.*/还债期活键/*pool*）；语义定位 `state_search(query)` |
| 知识图谱 | `kg_search`（语义）/`kg_query`（带过滤）；已合卡过程边已双时态失效（历史可查，检索不再命中） |
| 归档 | `docs/archive/ARCHIVE-2026-09-27-p1-p37.md`——P1-P38 全史唯一归档（软删记忆全文可按 id 恢复） |
| 检索库 | `search_code`（GT6 1.7.10/原版反编译/NeoForge API/GTCEu Modern/project 本仓） |

## 活跃 state 键速查

- `known_bugs`+`known_bugs.*`：现存 bug 台账（open/pool/field_test 状态在条目内）
- `ops.discipline.*`：工作流纪律（test-gating v1-v3/push 策略/审查流水线/subagent 模型）
- `decisions.*`：全部裁决（不归档，持续追加）
- `ops.reboot-snapshot-2026-09-27`：重启快照（在途/持有/重派/收官批/恢复协议）
- `tasks.debt-*`/`tasks.p37-*`（活跃卡）/`tasks.r3-*`/`tasks.issues-round3*`：还债期流水
- `tasks.p37-pool` 等池键：权威池册与活跃缝
