# 项目状态（镜像·维护期版 2026-09-28）

> 权威数据在 MCP `gt6-brain` 的 state/记忆/KG 里。本文件自 2026-09-27 起改为**维护期精简镜像**：
> P1-P38 逐阶段详账已蒸馏归档至 [docs/archive/ARCHIVE-2026-09-27-p1-p37.md](archive/ARCHIVE-2026-09-27-p1-p37.md)
> （阶段总表+还债期卡终态表+被归档记忆全文+删除清单）。ADR 在 docs/adr/（44+ 篇），已完成卡的过程明细随 git 历史。

## 项目

GT6（1.7.10 GregTech6）→ 现代移植：1.20.1 Forge + 1.21.1 NeoForge 双节点（Stonecutter）。
内容完成度 97-98%+，2026-09-24 发布 v0.1.0（GitHub Release，双 jar），现处**还债/维护阶段**（2026-09-26 起）。

## 当前态（2026-09-28 · r5 波收口）

- **r5 波五卡全清**（本地 main=52ce67e59，**攒批未 push**——领先 origin 5 合并，等用户点名一次推）：Jade 锅炉热量面（热量条/需求/半满门提示/潜行水垢缺水）、Jade 转换器族状态行（≈19 BE）、CI 手动 RCON sweep 工作流（workflow_dispatch，替代退役的本地全量）、issue 模板+六标签（[Bug]/[Feat]/[RFC] 前缀 triage 自动打标、组合前缀可选 rfc、p1-p3 仅手动）、README 人类语言重写。
- **issue 第四轮（#17-#26）已收口**：11 卡合入已推（5153c1dbe）；issue 重开待提出者测试关闭（不主动关评纪律，铁律 9）；矿石三裁定落地。
- 工作流纪律更新：全量 sweep 退役（宪法③）；不主动关 issue/评论（铁律 9）；push 攒批（ops.discipline.push-policy）。
- 低优池：大型机器 overlay 保真 / 多输入行 GUI / mTanksInput 遮蔽 / 无 getInventory 家族掉落 / 矿石用户侧复验（破损矿颗粒渲染+4.48M 显示疑点）。
- backlog：issue #29-#43 十五张新报告（含 Create/TFC 兼容等 Feat），待开工。
- 基础设施注记：gt6-brain MCP 写路径偶发 30s 慢响（写超时≠写失败，读验证后再重试；根因=嵌入索引残留进程持锁，kill 即恢复）。

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
