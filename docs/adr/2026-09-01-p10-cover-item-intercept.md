# ADR 2026-09-01-p10-cover-item-intercept：ICover item 族冻结面扩面（恰八钩）

> 状态：accepted（已落地，merge 8496595）。
> 本文为 state `decisions` 账本同名条目 + tasks.p10-cover-item-intercept 卡 +
> docs/PROJECT_STATE.md 第 10 阶段收官记录的成文整理（p12-hygiene-style-adr 转正），
> 只整理结构不改决策内容；全部 `文件:行号` 锚点原样保留。

## Context

- ICover 的 item 族钩子自 P4 起（spec ②）裁池冻结：ICover.java javadoc 记为
  "pooled D card"（item/redstone/GUI 钩区裁池在案）。P9 已解冻红石三钩
  （2026-09-01-p9-redstone-hooks），item 族仍是冻结面。
- 上游钩子面：ICover.java:209-216 恰八方法——interceptItemInsert（:210）、
  interceptItemExtract（:211-213）、getAccessibleSlotsFromSideOverride /
  canInsertItemOverride / canExtractItemOverride（:214-216）、
  getAccessibleSlotsFromSide / canInsertItem / canExtractItem。
- **勘误**：任务书旧口径 ":320-343" 有误——上游 ICover 全文 226 行，:320-343
  实为宿主侧 TileEntityBase04Covers 分发区（:343-365），不在 ICover 内。
- 宿主现状：本仓唯一 ICoverableTE 宿主是 TileEntityOven；其 capability 链
  （mItemHandlerCap，ADR-P3-2）尚无 cover 层拦截语义。
- 消费面缺席：消费盖五件（Shutter/Conveyor/RobotArm/FilterItem/RetrieverItem）
  未移植，框架卡无真消费盖，验收只能走离线 double + RCON 负回归/真值表。

## Decision

冻结面扩面显式裁决（本 ADR 是扩面的唯一 sanctioned 一次）：

1. **ICover 解冻恰八钩**（javadoc 解封 "pooled D card"），范围写死为上游
   ICover.java:209-216 的八个方法签名，零新增语义。
   红线维持：GUI 钩对（ICover.java:198-199）上游死代码不复刻（全仓零 GUI 盖）；
   fluid override 族（ICover.java:220-225）与消费盖五件维持池。
2. **AbstractCoverDefault 八默认**（上游 AbstractCoverDefault.java:93-100）：
   intercept 恒 F/F、override 恒 F/F/F、slots 透传 aDefault、answering 恒 T/T。
3. **ICoverableTE 三宿主门 default**（上游 TileEntityBase04Covers.java:343-365
   的 final 分发形状）：intercept 命中 → 拒；override 命中 → cover 决定；
   默认透传。composition 模型下上游 `&&canInsertItem2` 的宿主半边由 wrapper
   内层 IItemHandler 承担，门 true = cover 层不阻拦。
4. **wrapper 收口 = TileEntityOven capability 链窄域**：getCapability(ITEM_HANDLER,
   side != null) 按请求面捕获 Direction，六面各惰性缓存一个 LazyOptional 装饰器
   （null side 走 Root raw = 上游 SIDES_INVALID :355 形状）；insert/extract/
   isItemValid 前跑该面三门；extract 的 stack 取槽内实物（06Covers:336 形状）。
   BasicMachine/MultiBlockMachine 的 gated handler 追加 = 后续机器卡。

红线零触：CoverData / TileEntityBasicMachine / TileEntityBase10MultiBlockMachine /
hoe 三谓词 / GTOvenBlock。

## Deviations

- **宿主半边等价替换**：上游 `&&canInsertItem2`（TileEntityBase04Covers.java:329/:353）
  由 wrapper 内层 handler 承担 = 语义等价——wrapper 门先、内层后，拒绝时零触碰
  内层（wrapperRefusesOnlyOnTheCoveredFace 实证）。
- **wrapper 失效即时**：盖拆即时生效（三门逐调用重读 mBehaviours）；
  invalidateCaps 六面齐清 + super（Forge setRemoved/onChunkUnloaded 补丁驱动）；
  mInventory 实例稳定。
- **池项维持**：消费盖五件（CoverShutter.java:82 / CoverConveyor.java:89 /
  CoverRobotArm.java:115 / CoverFilterItem.java:115 / CoverRetrieverItem.java:138）、
  GUI 钩对 :198-199、fluid override 族 :220-225 全部不入本卡。
- **离线测试面**：getCapability 分派面（ForgeCapabilities）离线不可测，
  由 runServer 链 + 后续机器卡承接。

## Consequences

- **落地**：merge 8496595（链位 P10 批 2）；rebase dd801d0 → c5c5cf2 零冲突，
  patch-id 25e8c4cce234a45a7ba50117034a620adfd3a460 前后逐位一致；
  分支提交 6304b56 + dd801d0。
- **门禁**：根 205/0 + mdk 544/0（rebase 后口径）全绿；:mdk:build 绿；
  runData 二跑 written:0 树净。
- **测试**：CoverItemInterceptTest 11 测（协调者口径 "13" 系笔误，门禁总数 544
  为准——P10 收官教训⑥勘正）：三门真值表 / 侧隔离双层（门层 + wrapper 层双证）/
  slots 收窄含 isItemValid 镜像 / simulate 双分支 / 环回 / 裸炉 = raw 负回归。
- **RCON 活证**：chain_exit=0——energy rig（1 64 0 贴 oven）+ 裸链 stonex8 +
  plate 盖负回归 stonex16（同炉装盖前后持续烧）+ dismantle store=null +
  emitter signal 7/0 outWeak 跟随 + log ERROR=0。
- **遗留**：① BasicMachine/MultiBlockMachine 的侧感知 gated handler 追加 =
  后续机器卡；② 消费盖五件等池项见 Deviations；③ 本 ADR 原为 decisions 账本
  字符串形状条目，本文即其转正（P11 移交基建池项 "decisions ADR 字符串形状
  整理" 的第一篇）。
