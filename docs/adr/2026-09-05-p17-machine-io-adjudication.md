# ADR：21.1 机器 capability/IO 面拆卡 + IDENTICAL-22 逐条裁决

- **日期**：2026-09-05（P17①，architect 拆卡；2026-09-06 收官成文）
- **state 权威条目**：`decisions` 账本「P17① 21.1 机器 capability/IO 面合卡拆卡 + IDENTICAL-22 逐条裁决」
- **落地**：`work/p17-2111-machine-io` → merge **3dee46ff**；`work/p17-2111-test-fixture-22red` → merge **6e1d0d55**

## Context

P16 收口（`docs/adr/2026-09-05-p16-closeout-gate-ledger.md`）遗留两案 21.1 运行时 delta 与
22 个测试预存红（IDENTICAL-22，自 P15 M4 起与基线逐条全等）：

1. **Delta 1** `p16_drying_rows` 步 19/21/22 [3,3]——snowball 经料斗（hopper facing=down）→dryer
   推送断：21.1 侧 snowball 全程留料斗、机器槽恒 air、零产出。
2. **Delta 2** `p16_side_io` 步 16/17 [2,2]——跨机流体 auto-IO：B 机经 auto_in 抽桶正常（桶四 BET
   已注册），A 机共享面被推/抽恒不动。
3. **22 红**：16 红家族（RecipeMapFurnaceBridge 6 + OvenRecipe 6 + OvenEnergy 4）+ CoverClient 1 +
   MaterialBlocks 1 + FluidBridge 1 + Barrel 1 + EngineFuels 1 + OreChain 1。

**单根因裁定（两 delta 同源）**：`GT6CapabilityWiring`（P15 fork 时点产物，21.1-only capability
注册面）机器面只有 SHREDDER/CRUSHER/LATHE/OVEN（:90-142）+cokeoven（:150-156）；
`DRYER_BE`（GTMachines.java:274-277）与 `DISTILLERY_BE`（:382-385）不在面内。
- Delta 1 机制：NeoForge 1.21.1 hopper 推送首行走 `VanillaInventoryCodeHooks.insertHook`
  （HopperBlockEntity.java:139-141 → getItemHandlerAt level 级 `getCapability(Capabilities.ItemHandler.BLOCK,…)`,
  VanillaInventoryCodeHooks.java:209-240），未注册→Optional.empty→vanilla fallback（:392-405）
  只认 Container 系→死。1.20.1 活 = Forge BE.getCapability 覆写直答，不走注册面。
- Delta 2 机制：机器推/拉臂 `fluidHandlerAt`（TileEntityBasicMachine.java:1031-1046）——Forge 腿
  :1038-1040 BE 直查永活；21.1 腿 :1044 level 级查询只落已注册 provider；B 推 A 死 =
  DRYER_BE 未注册（doOutputFluids :1055-1070 null→continue）。seam 方法本体健在（:1398-1415）。
- 同型先例：桶族 BET 漏注册活体 CAPABILITY MISSING（GT6CapabilityWiring.java:164-169，
  ADR-P15-4）——本缺口是**第二次**，故本次机制化防第三次。

22 红逐条根因（四类）：
- **16 红家族**：罐头玻璃配方 fixture 用 1.20.1 字符串 `result` 形；1.21.1
  `RecipeManager.apply`（:60）走 `Recipe.CONDITIONAL_CODEC`、`SimpleCookingSerializer.codec`
  （:22-25）`result=ItemStack.CODEC`（1.20.5+ 对象形）→炸 `Not a JSON object: 'minecraft:glass'`
  →配方零加载→findRecipe 全空。fixture 副本两处：RecipeMapFurnaceBridgeTest.java:38 +
  GTMachinesOfflineTestBase.java:82（smeltingLevel）。
- **CoverClient 1**：1.21.1 `ModelResourceLocation` 是 record 不再继承 ResourceLocation，
  GTCoverClientListenerTest.java:119 裸 RL 查 `Map<MRL,…>` 恒 null（生产面 :113-117 已绿）。
- **MaterialBlocks 1 / FluidBridge 1**：21.1 测试 JVM 走 FML 全引导（GTOfflineTestBase:25-33），
  DeferredHolder 构造期有句柄且注册已发生——"注册前 null seam"（:198）与
  "seeded-unregistered"（FluidBridgeTest:23）前提在 21.1 不可达。
- **Barrel 1（permanent-delta）**：keepFilter 0 量载荷无 codec 键——FluidTankGT.java:99-107
  已声明 KNOWN 21.1 DELTA。

## Decision

**拆两卡并行（files_scope 互斥）**：

1. `p17-2111-machine-io`（生产域）：
   - `GT6CapabilityWiring.registerMachineBlockEntities` 增 DRYER_BE、DISTILLERY_BE 两族注册，
     各 ItemHandler.BLOCK + FluidHandler.BLOCK 两行，与既有 shredder/crusher/lathe 行同 lambda 形
     （整文件 `//? if neoforge` 包裹 = 21.1-only，1.20.1 腿零编译单元、零行级分叉）；
   - `GT6CapabilityWiringSeamTest.beWiringPathsPinnedToRegistryRows` 9→11 行；
   - **家族覆盖面守卫**：`BASIC_MACHINE_FAMILY_FACES` 常量表（家族=item+fluid，oven 例外=item-only）
     + census 测试（getDeclaredFields 扫 GTMachines 全部 `*_BE` 字段，双腿反射 getId）——
     防下一个新机器家族再漏注册。
2. `p17-2111-test-fixture-22red`（纯测试域，恰五文件）：
   - 罐头玻璃 JSON 双副本 21.1 形 fork（forge 腿字符串形原样；21.1 腿 result=`{"id":"minecraft:glass"}`，
     type/ingredient/experience/cookingtime 不动）；
   - CoverClient 死键探针 21.1 腿改 `ModelResourceLocation.standalone` 键取（镜像同测试 :101-103）；
   - MaterialBlocks seam 测试 leg-conditional（21.1 断 FML 引导后 DeferredHolder 非空悬挂句柄
     getId()=block_ingot_coal，forge 保留 null 断言）；
   - FluidBridge 两腿共同断 map-miss（未 seed 材质 copper→null），21.1 删 seeded-unregistered 臂。

**IDENTICAL-22 裁决表**：

| 类（条数） | verdict | 理由 |
| --- | --- | --- |
| RecipeMapFurnaceBridgeTest (6) / TileEntityOvenRecipeTest (6) / TileEntityOvenEnergyTest (4) | fix-in-P17 | 罐头 fixture 21.1 codec 形，证据级可修 |
| GTCoverClientListenerTest (1) | fix-in-P17 | MRL record 化死键探针 |
| GTMaterialBlocksRegistrationTest (1) / FluidBridgeTest (1) | fix-in-P17 | FML 引导前提下前提不可达，改 leg-conditional / map-miss |
| TileEntityBase08BarrelTest (1) | **permanent-delta** | 0 量 keepFilter 无 codec 键，FluidTankGT:99-107 已声明 KNOWN 21.1 DELTA，不修不触 |
| GTEngineFuelsTest (1) / GT6RecipesOreChainTest (1) | **defer（池）** | FML 引导下 GT6RecipeMaps 静态生命周期 × JUnit 方法序，机制未证（同套 4/5、12/13 绿），禁猜修，待活体诊断 |

残集等式 **IDENTICAL-3**：两卡全合后 21.1 基线重录为恰此 3 红且（类名,测试名,首行消息）三元组
逐条与旧基线等值。

**Delta 3**（p16_form_scaffold 每遍 server ERROR ×1，gt6:multiblock_coke_oven BE load）：
定性 = BE NBT 序列化域、与 IO 面正交，defer 池观察，不入卡。

## Deviations

- 19 红以测试域 fork/leg-conditional 修复而非生产代码改动——21.1 失败本体是**测试前提
  锁死 1.20.1 语义**，生产面两节点均已正确（CoverClient 生产面 :113-117 已绿为实证）。
- 守卫常量表而非反射扫 RegisterCapabilitiesEvent——离线测试不可见事件注册，常量表+census
  扫 `*_BE` 字段是离线可测的等价防漏面。

## Consequences

- 落地：machine-io merge **3dee46ff**（f7347461+3657b974 重签名）、fixture merge **6e1d0d55**
  （095f1020/28c4b8ee/08181bd9/14d7ab4d），双审查 approve 零打回（会话 3/3 追加式×2）。
- 门禁终态：root 205 + mdk 1074 = 1279 全绿（+1 census/seam）；1.21.1 失败集 **IDENTICAL-3**；
  21.1 compile 0 红；runData 五步门禁（ADR-P17-1）1.20.1 二跑 written:0。
- 两 delta 修复 RCON 实证（审查官 rebase 后亲跑）：p16_drying_rows 21.1 双遍 [0,0]（snowball
  入槽→inject 4000→out[0]=250L DistW）；p16_side_io 21.1 双遍 [0,0]（E 相 in[0]=1000L distw 平衡态）。
  1.21.1 RCON 终态 = 25 绿（P16 收口基线）+ 2 delta 链转绿 = **27/27 推定全绿**（全集 sweep 未重跑，
  按链级双遍实证推定）。
- 防再漏机制化：census 测试扫 GTMachines 全部 6 个 `*_BE` 字段 + 双向 set 等值 + oven 例外 pin——
  下一个机器家族注册时 seam 测试即红。
- defer 两案入池（EngineFuels/OreChain 活体诊断：打印方法序+每代 guard/pour 计数，禁猜修）。
