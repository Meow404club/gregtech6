# ADR-P19 草稿：W4 NBT provider 穿透池项裁决——本体缓做（条件触发池化），注册表覆盖缺口另立研究卡（B' level-less 全量 RegistryAccess 抓取）

日期：2026-09-06 ｜ 状态：正典（P19 收官成文，main 01031e47） ｜ 前置：ADR-P18（docs/adr/2026-09-06-p18-beload-provider.md，merge 21ceab6e） ｜ 评审基线 main=b19a02db ｜ 池项 tasks.pool-w4-nbt-provider-threading ｜ 裁决卡 tasks.p19-arch-w4-nbt-provider

## Context（全部实测，main=b19a02db）

**a) 池项原始动机已被 P18 消解——「全树唯此一类带病」复核结论：成立且已清零。**

P18 之后全树 21.1 NBT 活腿的 level 依赖调用点 = 0：

- `getLevel().registryAccess()` 病灶形态全仓仅剩 5 处命中，无一在 BE NBT 面：RecipeMapFurnace.java:78/:85（配方结果解析，持活 level 的加工路径，非 NBT 面）+ 测试 javadoc/注释 3 处（GTRecipesOfflineTestBase.java:88、TestMachineBlockEntityNBTTest.java:142、GTWireGlowLightTest.java:283）。
- `getLevel()` 全树 BE 命中（*BlockEntity*.java 37 处 + *TileEntity*.java 26 处）逐位点与 load/save 方法体行号交叉比对：全部落在 tick/交互/capability/爆炸/同步面，NBT 体零命中。`this.level` 直引 0 处。
- 21.1 腿 provider 消费点全部已冻结静态化（7 类 ~14 点）：TileEntityOven :709/:739、TileEntityBasicMachine :1507/:1543、GTExampleChestBlockEntity :171/:186、GTGeneratorSolidBlockEntity :515/:531、TestMachineBlockEntity :68/:82、TileEntityBase10MultiBlockMachine :812/:834（P18 两点，`aProvider = NBT_ACCESS` 局部变量再喂 serializeNBT/parseOptional 族）。全树 NBT 面合同归一为「item id lookup only」冻结视图。

即：池项 MOTIVATION 里「根治 NPE 病灶」的一半已由 21ceab6e 完成；剩余价值仅剩注册表覆盖面（见 c）。

**b) level-less 窗口是 vanilla 双腿共性，1.20.1 未发病纯因腿内无 level 访问。**

vanilla 1.20.1 反编译实测（sources=["vanilla"]）：ChunkSerializer.postLoadChunk:380 → `BlockEntity.loadStatic`（ChunkSerializer.java:396）→ `be.load(tag)`（BlockEntity.java:115，BET.create 刚建、无 level）→ catch Throwable 记 ERROR 返回 null（BlockEntity:117-119）→ `setBlockEntity` 跳过（ChunkSerializer:397-399）。与 1.21.1 同窗同死法（P18 诊断的 1.20.1 镜像）。1.20.1 腿安全是**约定性**的：load 签名无 provider 概念、项目 forge 腿 load 体零 level 调用——不是结构性保证。任何未来提交往 forge 腿 load 体加 `getLevel().xxx()` 都会在 1.20.1 复刻 P1 同型（且 1.20.1 是正典主节点）。防御手段同款：P18 已配 `loadWithoutLevelRestoresMachineState` 合同用例，覆盖约定。

**c) 「saveToItem 等 level-less 域」实测=本仓无此暴露面；真正剩余缺口只有 dynamic 注册表一族。**

- `saveToItem` 全仓 0 调用点、0 override。该 vanilla 1.20.5+ API（BE→item 表单域）对本仓无投影——池项 SPEC ①条款描述的是 vanilla 能力面，非仓内现实。
- 生产代码 level-less provider 面（21.1）实际只有两个：①chunk loadStatic（P18 已闭）②测试域 saveWithoutMetadata/getUpdateTag 无参调用（swap 表 :269-271 三条注入条款消化，命中全部在 mdk/src/test，~28 位点；main 活代码 0 处——stonecutter.gradle.kts:264 注释与实测一致）。
- 同步面（getUpdateTag/handleUpdateTag/onDataPacket）21.1 已透传 vanilla 真 provider：TicksAndSync :178-182、TileEntityOven :798-808、TileEntityBase08Barrel :433——且同步只发生在 BE 已挂 world 时，无 level-less 风险。swap 条款退役半径 = 3 条目 + ~28 测试位点 + 2-3 个 override 定义腿，独立不值一次签名波。
- **仅剩的理论缺口 = NBT_ACCESS 缺 dynamic/datapack 注册表**（ADR-P18 Deviations 已载 banner_pattern/jukebox_song/instrument 族）：1.21 附魔已数据驱动（minecraft.wiki Data pack 页 + Mojang 24w18a/v42 changelog 实证），即**任意附魔物品**在 21.1 机器槽经 chunk 重载时 decode 需 enchantment 注册表查找 → 冻结视图缺位 → 方向性结论：组件解析失败（预期 parseOptional 空栈静默丢物品；终态行为待研究卡钉证）。物品域不丢 BE（P18 死法不复现），是窄域 P2。1.20.1 腿零暴露（附魔静态注册表 + ItemStack.of）。save 侧对 Holder.Reference 编码走 key 字符串的理论旁路未经 21.1 javap 钉证，不入裁决主体。
- Level 实例语义需求面（维度区分/客户端判别）实测不在 NBT 面：isClientSide 判别全在 tick/交互面，NBT 体无需 Level 实例——provider 穿透不带来注册表覆盖之外的语义收益。

## Decision

**裁决：部分做/缓做。W4 provider 全树签名波本体「不做（现在）」，改判为条件触发池化；注册表覆盖缺口由新研究卡 B' 接管，通过后按 1 文件小卡落地。**

1. **本体（Root:138-146 委托链重形 + ~24 文件 load/save provider 签名波）缓做**。理由：
   - 原始动机（P1 NPE 病灶）已闭合且全树带病面清零（a 节实数）；
   - 剩余缺口是窄域 P2（21.1 附魔/组件物品降级），正典主节点 1.20.1 零暴露；
   - 成本不可承受：签名波 ≈25 文件 × load/save 双向 + swap 表 NBT 族条款联动 + 双腿编译门 + 21.1 CI 现为 compile-only（ADR-P18 后续注记），行为验收全靠真机 RCON 链——P18 单类两点尚需快照 byte 等价三遍活体，25 文件波的真机回归成本与其收益完全失配；
   - 防御面已有替代：约定（load 面无 level）+ 合同用例（loadWithoutLevel）+ 双腿零字节门，见 b 节。
2. **新研究卡 `p19-research-registryaccess-frozen-provider`（B'，W4 波内先行，禁与任何域并行开码农卡）**：钉证 21.1.249 上「level-less 全量 RegistryAccess 抓取」方案——RegistryAccess.Frozen 是否公开实现 HolderLookup.Provider 契约、ServerAboutToStart/ServerStarted 事件抓取时序与 RegistrySynchronization 动态注册表加载完成时序、datagen/离线测试无 server 的回退（维持现 frozen builtin）、非 final 静态重绑的类初始化/可见性债、附魔 ItemStack round-trip 实证。产出二选一：
   - 通过 → coder 小卡：TicksAndSync :185-192（仅 neoforge 注释腿）NBT_ACCESS 重绑化，全树 14 消费点自动受益，零签名波。FILES_SCOPE = `mdk/src/main/java/gregtech6/tileentity/TileEntityBase03TicksAndSync.java` + 测试域（GTRecipesOfflineTestBase.java:204 维持 frozen builtin 或换 MinimalLevel 供给）。验收=①21.1 持久世界 RCON 链 0 ERROR（P18 同链回归）②序列化快照 byte 等价（P18 A_pre/A 方法同款）③新增附魔物品槽 round-trip 用例 ④1.20.1 腿 stonecutterPrepare swap 零变化断言。
   - 证伪 → 本池项维持原 medium 优先级回到既定 W4 路线。
3. **本体重新触发条件（满足其一才再议）**：①B' 落地后仍存在需真 provider 的覆盖缺口（direct/Holder 引用组件族）；②21.1 腿 CI 升级为可跑测试/真机链自动化，签名波验收成本降到可承受；③移植目标升到 provider-mandatory 的未来 MC 版本。触发前池项不得并行开卡（W4 级爆炸半径纪律不变）。

**否决的备选**：
- 立即做全树穿透（本轮池项原案）：成本/收益失配如上；
- try-catch 兜底附魔 parse：掩盖病灶，半初始化物品域更危险（P18 同款否决理由）；
- 仅退役 swap 条款不顺带签名波：条款是测试域唯一的 21.1 编译通道，退役=测试面重写，无独立收益。

## Consequences

- 正面：避免一次 25 文件级签名波压在正典节点冲刺期；21.1 窄域数据丢失获得一条 1 文件成本的修复路径（B'）；池项从「既定路线」改为「条件触发」，W4 波排产自由度回收。
- 风险与边界：B' 若引入非 final 静态可变量，新增一处全局状态债（时序/可见性）——这正是先研究后码农的原因；附魔降级的实际频率未经遥测（GT6 机器槽主流内容为矿/锭/工具），若 B' 证伪且无人报障，本缺口可无限期池化。
- 遗留：1.20.1 腿「load 面无 level」是约定非结构保证——本 ADR 即约定成文处；后续 coder 卡向 forge 腿 load 体引入 level 调用视为违约，审查官可拒。
