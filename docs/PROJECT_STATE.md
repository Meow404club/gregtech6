# 项目状态（镜像·维护期版 2026-09-28）

> 权威数据在 MCP `gt6-brain` 的 state/记忆/KG 里。本文件自 2026-09-27 起改为**维护期精简镜像**：
> P1-P38 逐阶段详账已蒸馏归档至 [docs/archive/ARCHIVE-2026-09-27-p1-p37.md](archive/ARCHIVE-2026-09-27-p1-p37.md)
> （阶段总表+还债期卡终态表+被归档记忆全文+删除清单）。ADR 在 docs/adr/（44+ 篇），已完成卡的过程明细随 git 历史。

## 项目

GT6（1.7.10 GregTech6）→ 现代移植：1.20.1 Forge + 1.21.1 NeoForge 双节点（Stonecutter）。
内容完成度 97-98%+，2026-09-24 发布 v0.1.0（GitHub Release，双 jar），现处**还债/维护阶段**（2026-09-26 起）。

## 当前态（2026-09-28 · r5-r7 波收官）

- **r5/r6/r7 三波 26 卡全部合入并推送**（main=d3fb2acd7，CI 全绿 36428109853）：Jade 信息面（锅炉热量+全机器状态行）、CI 手动 sweep 工作流、issue 模板+标签体系（[Bug]/[Feat]/[RFC] triage+p1-3+may fix）、README 重写、轨道空白/EMI 几何/蜂巢染色修复、**主世界 C 融合档落地**（原版团块屏蔽+GT 晶洞+budding+透镜伴生矿+矿石轴 53→122 两波扩轴+GT 铜唯一+深层地质带）、JEI/EMI 图标汉化+能源拆分、工具头组装链归位、模具资产+形状几何、泡沫防水、三 NPE 修复。
- **#17/#31 判决非 bug**（火盒档位饥饿观感死机/负坐标普查四象限绿），判决文案在任务板。
- may fix 已挂 17 张修复 issue（提出者验证后自关）；未处理：#27/#28/#35-38。
- 工作流纪律：流水线审查（恒单会话滚动）/push 攒批点名制/may fix=push 后挂/全量 sweep 退役（宪法③）/不主动关评（铁律 9）。
- 残余池：1% 层内小宝石小代码面、crack 可发现性、mold_stone 视觉同步、机器贴图债、镜像新鲜度门、信息页深带显示。
- 治理待办：KG 蒸馏（3355 节点超阈）、handoff 历史软删（锚点 id1051 已蒸馏）。
- 基础设施注记：gt6-brain MCP 写路径偶发慢响（写超时≠写失败）；datagen treecheck 假红处置=rm -rf build/datagen-output。

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
