# ADR-P18 草稿：keepFilter 读侧重建裁决（21.1 permanent-delta 处置）

日期：2026-09-06 ｜ 状态：正典（P18 收官成文，main fc494c6b） ｜ 前置：ADR-P17（2026-09-05-p17-machine-io-adjudication.md:43-44/:73） ｜ main=2c3fc966

## Context

P17 将 IDENTICAL-22 中唯一的 permanent-delta 判给了 `TileEntityBase08BarrelTest.logisticsBarrelIdentitySurvivesTheZeroAmountRoundTrip`（21.1 节点 1 红，判词"0 量载荷无 codec 键——FluidTankGT.java:99-107 已声明 KNOWN 21.1 DELTA，不修不触"，docs/adr/2026-09-05-p17-machine-io-adjudication.md:43-44/:73）。本卡按池项"keepFilter 读侧重建裁决（跨重启 RETAINED/LOST，需架构师裁）"完成机制取证并裁决。

**a) 21.1 跨重启丢失的精确机制（file:line 证据链）**

1. **写侧：身份在盘上，不丢。** mdk FluidTankGT.writeToNBT 空分支双腿都写 GT6 合同键：forge 腿 `FluidName`（FluidTankGT.java:144）、21.1 腿 `FluidName`（:146，取自 live mFluid——21.1 载体在 empty-flag 态 getFluid() 仍读真身，:141-142 注释）、`Amount: 0` 两腿共用（:148）。21.1 基线测试 XML 实证写半段断言全过且落盘载荷为 `{Amount:0, FluidName:"minecraft:water"}`（mdk/versions/1.21.1-neoforge/build/test-results/test/TEST-gregtech6.tank.TileEntityBase08BarrelTest.xml:16-17 失败在 ：341 读半段、:30 system-out 的 codec 报错原文含 MapLike 载荷）。
2. **读侧：DataResult 失败被静默吞。** 21.1 腿 readFromNBT 走 `FluidStack.parseOptional(nbtAccess(), tNBT)`（FluidTankGT.java:108）；NeoForge 1.21.1 FluidStack codec 面要求小写键 `id` + `amount` 且 `amount` 是 **POSITIVE_INT**（tmp/harvest/neoforge-api-12111 .../fluids/FluidStack.java:65-73，:70 严格 >0），`parseOptional = parse(...).orElse(EMPTY)`（同文件 OPTIONAL_CODEC :98-99 同构；21.1 运行时报错原文 "No key amount …; No key id …" 见上述 XML:30）→ 载荷 `{FluidName, Amount:0}` 无 codec 键 → EMPTY → :109-111 折叠为 null 罐 → 测试 :341 红。
3. **修复通道排查（禁猜修结论）：** "写侧补 codec 键"**结构性不可行**——即使写 `id`，`amount:0` 被 POSITIVE_INT 拒（FluidStack.java:70），0 量栈在 21.1 codec 面不可表示；且 21.1 `isEmpty()` 按 `amount <= 0` 全量塌缩、`getFluid()` 无 getRawFluid 逃生门（FluidStack.java:205-207/:235-237），forge 腿的"从 live 栈 raw-fluid 重建"在 21.1 无供给点（:104 注释所断言的正是这一条，它不关闭"从盘上 FluidName 键反查注册表重建"通道）。"fork 平台 codec"违反 P15 不碰平台字节码纪律，否。

**b) 上游 1.7.10 对齐基准 = RETAINED。** MultiTileEntityBarrelLogistics.java:40 `keepsFilter()=T`（:39 canBeSealed=F）；Base08Barrel.readFromNBT2 装载时重臂 `setPreventDraining(keepsFilter())`（tmp/gt6-1.7.10 .../tank/TileEntityBase08Barrel.java:69）；FluidTankGT.remove 在 preventDraining 下只清量不清身份（tmp/gt6-1.7.10 gregapi/fluid/FluidTankGT.java:144-156）；writeToNBT 在 `mPreventDraining || mAmount > 0` 时照写 mFluid（:70-75，1.7.10 FluidStack 无 empty-flag，0 量也写 FluidName）；readFromNBT 经 `FL.load_` 按 `FluidName` 键重建、无 0 量过滤（gregapi/data/FL.java:1035-1045）→ 跨重启身份保留。

**c) 1.20.1 正典节点现状 = RETAINED。** 路径：forge 写侧真名 0 量落盘（FluidTankGT.java:143-148）→ `loadFluidStackFromNBT` 原生读 FluidName 得 empty-flag 但 raw fluid 完好的栈 → forge 读侧 ：87-94 判 `isEmpty()` 后以单位量载体重建 `new FluidStack(rawFluid, 1)`、`mAmount` 保持权威 0 → 身份在内存存活。共享罐逻辑全以 mAmount 为权威（getFluid :228-236、fill :255-275、drain :279-298、isEmpty :349-351=`mFluid==null`、contains :366-379），该重建态已被 1.20.1 节点 14/14 测试 + P12 活体验证（1279 绿基线）。

## Decision

**裁 ②：实现 21.1 读侧重建**（任务卡 `p18-keepfilter-2111-readback`，分支 `work/p18-keepfilter-2111-readback`）。

- 落点：恰 FluidTankGT.readFromNBT 的 21.1 else 腿（:99-115），在 ：109-111 折叠点前插 FluidName 回退：parseOptional 得 EMPTY 且 `tNBT` 含 `FluidName`(TAG_STRING) 时，反查 `BuiltInRegistries.FLUID`（键名解析加守卫，垃圾名/未知名落空罐不炸），命中则 `mFluid = new FluidStack(fluid, 1); mAmount = 0`——与 forge 腿 ：87-94 重建态逐语义同构。写侧零改动（载荷已是上游 1.7.10 同形 `{FluidName, Amount}`）。
- 基准对齐：修复后双腿跨重启均 RETAINED = 上游 (b) 基准闭合；`logisticsBarrelIdentitySurvivesTheZeroAmountRoundTrip` 断言零改动双腿全绿；21.1 基线残集 IDENTICAL-3 → IDENTICAL-2（残红恰剩 EngineFuels/OreChain 两条 defer 池项）。
- 配套：:99-115 KNOWN DELTA 注释块改写为重建语义记录；FluidTankGTTest 增 1 条双腿负例（垃圾 FluidName → load 后空罐）。

**裁决理由（对①③）**：①（维持 permanent-delta）与 ③（leg-conditional 断言 LOST）都在"机制不可证/不可修"前提下才成立；本卡已证明机制可证且可修（单一 chisel 腿内回退、forge 腿字节不动、零冻结面），而 21.1 是一等公民交付节点（ADR-P15 路线 A′），Logistics Tank 是已注册活体方块（GTBarrels.java:312-313），接受 LOST = 该节点头部特性静默回归。③ 还会令 21.1 节点永久失去该特性守护断言，严格劣于 ②。P17 的"不修不触"是当时拆卡 scope 纪律（:73），非不可能性证明，被本裁决取代。

## Deviations

- **0 量载荷不持久 fluid components/tag（双腿既有，声明不修）**：上游 1.7.10 写侧 0 量连 tag 一起写；本仓空分支只写 FluidName+Amount（FluidTankGT.java:143-148），重启后 kept 栈 components 丢。对原版流体零影响；带组件模组流体的保留语义归池（p12 池遗留，非本卡回归）。
- **21.1 载体重建态与 forge 腿同构但实现面不同**：forge 腿经 raw-fluid 重建、21.1 腿经注册表反查重建——同一落点状态（单位量载体 + mAmount=0），语义无偏离。
- 反查 API 以 1.21.1 实际签名为准（编译钉死）；`tmp/refs/vanilla-mc/1.21.11` 树已是 Identifier/getValue 形（Registry.java:61-76），不可直接当 1.21.1 引用。

## Consequences

- 正面：上游 RETAINED 语义双腿闭合；21.1 红测转绿且断言零弱化；IDENTICAL 基线 3→2；载荷形态与上游 1.7.10 byte-contract 一致（FluidName/Amount）。
- 风险与边界：改动被 chisel 锁在 21.1 腿，1.20.1 编译单元字节不动（回归面≈0）；共享逻辑不触；垃圾 NBT 由守卫兜底为空罐（与 codec 面 parse 失败同待遇）。
- 回归风险对比：① 零代码风险但永久特性回归+永久红测记账成本；③ 同 ① 且测试失效；② 一次性的窄腿生产改动，风险由"forge 腿已验证的同一目标态"对冲。综合裁 ②。
- 后续触发器：若 1.21.x 后续版本改 `amount` 为非负（NON_NEGATIVE_INT）或恢复 raw 访问器，可简化重建路径（池观察，不预防性设计——ADR-P15-7 矩阵纪律）。
