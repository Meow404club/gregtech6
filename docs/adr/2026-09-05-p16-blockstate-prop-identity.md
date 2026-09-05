# ADR-P16-2：跨类 BlockState Property 常量单实例收拢（GTBlockProperties holder + 类内别名）

- 日期：2026-09-05
- 裁定人：架构师（P16 追加立卡任务）
- 状态：已裁定，落地卡 = state `tasks.p16-blockstates-2111-prop-intern`（status=queued）
- 原文：本文件即裁定全文；摘要同步 state `decisions` 账本（topic=2026-09-05-p16-blockstate-prop-identity）。

## 1. 背景

p16-clienthandlers-2111（d515d093，在审）修复 1.21.1 GT6Mod 构造崩后，1.21.1 runData
揭开下一环被遮蔽缺陷 `gt6blockstates-2111-active-property-intern`（known_bugs 已立，
remember id284/id285）：`GT6BlockStates.addMachine` 的 forAllStates lambda（:275-276）
用 `GTOvenBlock.ACTIVE/RUNNING` 读全部机器块状态，对 gt6:shredder（GTBasicMachineBlock）
抛 "Cannot get property BooleanProperty{name=active} as it does not exist"
（/tmp/p16ch_1211_rundata1.log:87-91）。main 上该路径从未到达（被构造崩遮蔽）。

## 2. 机制勘误（对 id284 "intern" 表述的修正）

census 实读双节点反编译，"1.20.1 存在 intern" 的表述**不成立**，历年绿的真因是
**StateHolder 查找语义换了**：

| | 1.20.1（tmp/vanilla-1.20.1/） | 1.21.x（tmp/refs/vanilla-mc/1.21.11/，与 1.21.1 实崩行为一致） |
|---|---|---|
| `BooleanProperty.create` | 裸 new，无缓存（BooleanProperty.java:19-21） | 裸 new，无缓存（:20-22，类 final） |
| `BooleanProperty.equals/hashCode` | 值语义（按 name+values，:33-46） | 无覆写（identity） |
| `StateHolder.values` 类型 | `ImmutableMap`（StateHolder.java:40） | `Reference2ObjectArrayMap`（:36）＝identity 查找 |
| 异实例同名常量 `getValue` | **命中**（hash+equals 按值） | **必炸**（:86-93 get 落空抛 IllegalArgumentException） |

结论：跨类读属性在 1.20.1 是**靠值等价侥幸成立**，不是靠 intern；GT6BlockStates.java
:246-250 javadoc 的 "BooleanProperty interning (BY_NAME cache)" 前提自始就是错误理论
（碰巧预测对了行为）。修复必须制造**真同一实例**，不得再依赖任何 equals/intern 语义。

## 3. Census（波及清单，全仓 grep，main aa242411）

### 3.1 崩点（必修，1.21.1 必炸）
- `mdk/src/main/java/gregtech6/datagen/GT6BlockStates.java:275`（`aState.getValue(GTOvenBlock.ACTIVE)`）
- `mdk/src/main/java/gregtech6/datagen/GT6BlockStates.java:276`（`aState.getValue(GTOvenBlock.RUNNING)`）
- 经 `addMachine` 可达 **16 块**：oven(:82)＋shredder/crusher/lathe T1(:83-85)＋T2-T4 阶梯 9 块(:88-96)＋dryer 4 行(:121-125)；oven 是 GTOvenBlock 同实例先行通过，**其余 15 块全为 GTBasicMachineBlock → 全炸**（与实崩首块 shredder 一致）。:269 的 FACING 不在崩点内（见 3.3）。

### 3.2 同名异实例常量定义对（缺陷类全集，收拢对象＝6 常量/4 类）
- `mdk/src/main/java/gregtech6/block/GTOvenBlock.java:58` ACTIVE("active")、`:61` RUNNING("running")
- `mdk/src/main/java/gregtech6/block/GTBasicMachineBlock.java:154` ACTIVE、`:157` RUNNING（与 GTOvenBlock 同名异实例＝本次病灶）
- `mdk/src/main/java/gregtech6/block/wire/GTWireBlock.java:71` CONNECTIONS("connections",0,63)
- `mdk/src/main/java/gregtech6/block/pipe/GTFluidPipeBlock.java:66` CONNECTIONS（与 GTWireBlock 同名异实例＝**潜在同类**：当前零跨类读，纯侥幸不炸）

### 3.3 跨类但同实例（安全面，本卡禁动）
- FORMED：全仓唯一定义 `TileEntityBase10MultiBlockBase.java:58`，跨 4 文件 8 引用点
  （GT6BlockStates.java:163/:169、GTMultiBlockControllerBlock.java:35/:36/:41/:47、
  GTMultiBlockCommand.java:333/:811、GTWrenchHighlightListener.java:100/:101）
  ——单 owner，identity 天然成立，双节点安全。
- FACING：9 处定义（GT6BurningBoxes.java:335、GT6Boilers.java:228、GT6Kinetics.java:328、
  GTOvenBlock.java:55、GTBasicMachineBlock.java:54、TileEntityBase10MultiBlockBase.java:61、
  GTCrankBlock.java:46、GTDieselEngineBlock.java:42、GTTransformerRotationBlock.java:35）
  全部 = `BlockStateProperties.HORIZONTAL_FACING` 同一 vanilla 实例
  （GTAttachmentSmallBlock.java:73 = BlockStateProperties.FACING，亦 vanilla 共享实例）。

### 3.4 同类自读（无关面，别名后零改动自动安全）
TileEntityOven.java:676-678、TileEntityBasicMachine.java:1459-1461（各写自家块常量）、
GTCrankBlockEntity.java:321-322、GTSteamEngineBlockEntity.java:413-414、
GTBoilerTankBlockEntity.java:600-601、GTTransformerRotationBlockEntity.java:165-166、
GTAxleBlockEntity.java:368-369、GTWireBlockEntity.java:792-794、
GTFluidPipeBlockEntity.java:311-313（hasProperty 守卫）、GTWireBakedModel.java:194-195、
GTWireStaleMaskTest.java:195、GTOvenOverlayModel.java:139-140、
GTWrenchHighlightListener.java:86。

## 4. 裁定：方案 (a) 单一 holder 收拢 + 类内别名保面

新建 `mdk/src/main/java/gregtech6/block/GTBlockProperties.java`（唯一 owner，纯静态
Property 工厂常量，私构）：`ACTIVE`/`RUNNING`（"active"/"running"）＋`CONNECTIONS`
（"connections",0,63）。四个重复定义点（3.2）全部改为**别名赋值**
（`public static final BooleanProperty ACTIVE = GTBlockProperties.ACTIVE;`），
GT6BlockStates.java:246-250 javadoc 改写为单 owner identity 表述。
新增 identity 钉测 `GTBlockPropertyIdentityTest`（assertSame 三对；注意：该测在现 main
上**两节点都红**——1.20.1 也是异实例、靠值等价侥幸——修后双节点绿，这正是回归钉的价值）。

裁定标准逐条：
- **双节点同源**：纯 Java 静态字段别名，零 chisel 分叉。✔
- **迁移面最小**：既有消费点（TileEntityOven/TileEntityBasicMachine/datagen lambda/
  wire/pipe 全部读写点）**零改动零重编译语义变化**（别名保编译面）；触碰 = 5 现有文件
  （其中 GT6BlockStates 仅 javadoc）＋2 新文件。✔
- **不破坏既有 blockstate JSON/datagen 产物**：property **name** 不变（"active"/
  "running"/"connections"），blockstate JSON variant 键取自各块自家 StateDefinition，
  与 lambda 用哪个常量实例无关 → 产物 byte-identical，由 runData 一跑 diff=空＋二跑
  written:0 验证。✔
- **P12/P8 冻结面**：FORMED/FACING 面（3.3）零触碰；P12 pattern API、P8 cokeoven GUI
  均不消费这 4 个常量。✔

### 否决的备选
- **(b) datagen lambda 按名动态取**（`aState.getBlock().getStateDefinition().getProperty("active")`）：
  只修 1 个 lambda，留下 4 个异实例常量＋错误 javadoc＋CONNECTIONS 潜在同款；引入
  stringly-typed nullable 查找；未来任何新跨类读（datagen 或运行时）重开同类崩。
  **缺陷类意义上不是最小面，是最小补丁。**
- **(c1) lambda 内 instanceof 分派用各块自家常量**：lambda 变形更大，异实例仍在。
- **(c2) AT/Mixin 恢复 vanilla intern**：本项目不碰 vanilla 字节码，禁。
- **(c3) 删别名全仓改引 holder**：无收益的引用面翻倍（~15 处），别名已保 identity。

## 5. 与在途卡的 FILES_SCOPE 关系

- p16-form-scaffold（multiblock 域：GTMultiBlockStructureChecker/GTMultiBlockCommand/
  test multiblock/rcon chain）：**零文件重叠**。
- p16-distillery-family（GTMachines 行追加＋item/＋recipes/＋"datagen append"）：
  **GT6BlockStates.java 存在同文件不同 hunk 的可能**（其 datagen append 大概率是
  :83-110 区新增行或新方法；本卡只动 :246-250 javadoc）。hunk 不相交、双 append-and-javadoc，
  后合者 rebase 重签即可（P8 A/B 卡先例）。本卡新增 alias 后 distillery 新块若走
  addMachine 自动被修复覆盖。
- p16-drying-rows-backfill：已合（aa242411），无冲突。
- 派发序：p16-clienthandlers-2111 先合（1.21.1 runData 验收的基线前提，不依赖其在审
  分支内容）→ 本卡（小卡快合，解除 1.21.1 runData 判据③ BLOCKED）→ 其余顺延。

## 6. 验收锚

1. 1.20.1 runData 一跑与 main 基线产物零 diff、二跑 written:0＋git porcelain 空。
2. 1.21.1 runData 完整跑通、零 IllegalArgumentException（判据③解除）；若再揭新缺陷，
   按本卡先例另立 followup，不扩本卡 scope。
3. 根 check 205/0＋mdk cleanTest 全绿；GTBlockPropertyIdentityTest 双节点绿。
4. 1.21.1 红文件门禁双口径 0 新增。
5. 收拢后全仓 grep：holder 之外无第二处 `BooleanProperty.create("active"/"running")` /
   `IntegerProperty.create("connections")` 定义点。
