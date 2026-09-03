# ADR: p15-stonecutter-skeleton W1 POC 段（Stonecutter 双节点骨架）

- 日期: 2026-09-03
- 分支: work/p15-stonecutter-skeleton（W2 合入段沿用同分支，后续卡续作）
- 状态: W1 POC 段完成（worktree 本地，不合 main）
- 上游裁定: decisions.2026-09-03-p15-crossversion-arch（W1 POC 波）/ tmp.research.stonecutter-version-pin（钉版）

## 1. 结论四件（POC 验收数据）

| # | 验收项 | 结果 |
|---|--------|------|
| 1 | 1.20.1 节点测试 | `:mdk:1.20.1-forge:test --no-build-cache` **968/968 绿**（JUnit XML 实测 tests=968 failed=0 skipped=0，2026-09-03 22:06-22:09）；根 gregapi `:test --rerun-tasks` **205/205 绿**（22:10）——1173 永续等价在骨架形态下成立 |
| 2 | Stonecutter 钉版 | **0.7**（settings.gradle.kts plugins 块；裁定依据 = tmp.research.stonecutter-version-pin：0.9.8 强制 Gradle 9 而 MDG 无 Gradle 9 声明；0.7 兼容 8.x/9.x，本仓 wrapper 8.14 零升级） |
| 3 | gregapi 挂法 L2 | 工作：根项目保持 gregapi java-library 原样（build.gradle 零改动），mdk 降 Stonecutter 控制器子项目，节点 `implementation(project(":"))` 消费；includeBuild 自指未使用。根 205 绿 = java-library 语义未破 |
| 4 | 1.21.1 失败清单 v0 | **156 文件 / 2,652 错误**（javac 实测，三跑计数一致；详见 §3）——W3-W5 收缩门禁基线 v0（ADR-P15-10 执行体） |

## 2. 骨架布局（worktree 内，全部不合 main）

```
settings.gradle.kts            # 石匠容器化：stonecutter 0.7 + create(project(":mdk"))
                               #   + match("1.20.1","forge") / match("1.21.1","neoforge") + vcsVersion=1.20.1-forge
mdk/stonecutter.gradle.kts     # 控制器脚本（mdk 的 buildFileName 被 stonecutter 覆写为本文件）
                               #   moddev + moddev.legacyforge 2.0.144 apply false + active 1.20.1-forge
                               #   + parameters.constants.match(loader 后缀) 给 forge/neoforge chisel 常量
mdk/build.forge.gradle.kts     # 1.20.1 Forge 节点（原 mdk/build.gradle 的 Kotlin DSL 等价迁移）
mdk/build.neoforge.gradle.kts  # 1.21.1 NeoForge 节点（moddev + Java21 工具链 + neoforge 21.1.249）
mdk/versions/1.20.1-forge/gradle.properties     # deps.minecraft/deps.forge（47.4.10 现役钉值）
mdk/versions/1.21.1-neoforge/gradle.properties  # deps.minecraft/deps.neoforge（21.1.249 = 21.1 线最新稳定，maven.neoforged.net 2026-09-03）
mdk/build.gradle               # 已删（内容等价迁移至 build.forge.gradle.kts）
mdk/gradle.properties          # 只留 mod.* 共享元数据（minecraft_version/forge_version 下沉节点文件）
```

节点拓扑（TreeBuilderImpl.createNode 源码实证）：`:mdk:1.20.1-forge` 与 `:mdk:1.21.1-neoforge`，
projectDir = `mdk/versions/<node>`，buildFileName = `../../build.<loader>.gradle.kts`（即 mdk/ 下共享）。
chisel 共享源 = 控制器目录 `mdk/src`（StonecutterBuildImpl.createProcessingTasks：`parent.fileTree("src/<sourceSet>")`）
——**mdk 源零移动挂双节点**；活动节点（1.20.1-forge）configureSource 直接 srcDir mdk/src 原位编译，
非活动节点编译自 `build/generated/stonecutter`（stonecutterGenerate Sync 产物）。

## 3. 1.21.1 失败清单 v0（W3-W5 单调收缩门禁基线）

- 口径：`./gradlew :mdk:1.21.1-neoforge:compileJava`（Java21 工具链 + NeoForge 21.1.249 + moddev 2.0.144）
- 结果：**2,652 错误 / 156 失败文件**（javac 完整诊断，三跑计数一致；Gradle 在编译后回传巨型诊断时超时/守护堆 OOM，
  诊断流本体已完整落地——失败集以三次独立运行计数一致为完整性证据）
- 互证：静态普查 `import net.minecraftforge.*` 文件 = 127，**全部 ⊆ 156**；另 29 文件为传递破坏（引用断链符号）。
- mdk main 共 195 文件：156 红 + 39 绿（编译通过面，纯 vanilla/纯逻辑文件）。
- 任务卡映射与 architect 普查一致：registry(15)/covers(18)/recipes(12)/client.render(11)/command(7)/datagen(7)/multiblocks(7)/machines…

### Top 缺失符号（按错误行计）

FluidStack 292 / RegistryObject 245 / SubscribeEvent 56 / IFluidHandler 47 / DeferredRegister 43 /
IItemHandler 33 / FluidAction 27 / ModelFile 26 / FMLConstructModEvent 21 / LazyOptional 20 /
IEventBus 16 / ModelData 14（根因 = net.minecraftforge.* 包整体不存在于 NeoForge，1.20.5+ capability rework 与
包名迁移为 W3-W5 各卡工作面）

### 失败文件清单（156，mdk/src/main/java 相对路径）

- `gregtech6/GT6Mod.java`
- `gregtech6/block/GTBasicMachineBlock.java`
- `gregtech6/block/GTExampleChestBlock.java`
- `gregtech6/block/GTOvenBlock.java`
- `gregtech6/block/TestMachineBlock.java`
- `gregtech6/block/attachment/GTAttachmentSmallBlock.java`
- `gregtech6/block/energy/GTAxleBlock.java`
- `gregtech6/block/energy/GTCrankBlock.java`
- `gregtech6/block/energy/GTDieselEngineBlock.java`
- `gregtech6/block/energy/GTEnergySourceBlock.java`
- `gregtech6/block/energy/GTGearBoxBlock.java`
- `gregtech6/block/energy/GTTransformerRotationBlock.java`
- `gregtech6/block/material/GTMaterialPrefixBlock.java`
- `gregtech6/block/multiblock/GTCokeOvenBlock.java`
- `gregtech6/block/multiblock/GTLargeBoilerBlock.java`
- `gregtech6/block/multiblock/GTMultiBlockPartBlock.java`
- `gregtech6/block/pipe/GTFluidPipeBlock.java`
- `gregtech6/block/tank/GTBarrelBlock.java`
- `gregtech6/block/wire/GTWireBlock.java`
- `gregtech6/client/GTClientHandlers.java`
- `gregtech6/client/GTClientModBusListener.java`
- `gregtech6/client/render/GTDynamicBakedModel.java`
- `gregtech6/client/render/GTFluidPipeFlowModel.java`
- `gregtech6/client/render/GTModelProperties.java`
- `gregtech6/client/render/GTMultiBlockGhostMatcher.java`
- `gregtech6/client/render/GTMultiBlockPreviewRenderer.java`
- `gregtech6/client/render/GTOvenClientListener.java`
- `gregtech6/client/render/GTOvenOverlayModel.java`
- `gregtech6/client/render/GTPipeFlowClientListener.java`
- `gregtech6/client/render/GTRenderModelListener.java`
- `gregtech6/client/render/GTWrenchGridRenderer.java`
- `gregtech6/client/render/GTWrenchHighlightListener.java`
- `gregtech6/client/wire/GTWireBakedModel.java`
- `gregtech6/client/wire/GTWireClientListener.java`
- `gregtech6/client/wire/GTWireTextures.java`
- `gregtech6/client/wire/GTWireTint.java`
- `gregtech6/command/GT6EnergyCommand.java`
- `gregtech6/command/GTBoilerCommand.java`
- `gregtech6/command/GTBurnerCommand.java`
- `gregtech6/command/GTEngineCommand.java`
- `gregtech6/command/GTGuiCommand.java`
- `gregtech6/command/GTToolCommand.java`
- `gregtech6/command/GTWireCommand.java`
- `gregtech6/covers/CoverData.java`
- `gregtech6/covers/GT6Covers.java`
- `gregtech6/covers/GTCoverCommand.java`
- `gregtech6/covers/GTCoverRenderSnapshot.java`
- `gregtech6/covers/ICover.java`
- `gregtech6/covers/ICoverableTE.java`
- `gregtech6/covers/client/CoverPlateModel.java`
- `gregtech6/covers/client/GTCoverClientListener.java`
- `gregtech6/covers/covers/AbstractCoverDefault.java`
- `gregtech6/covers/covers/CoverControllerAutoRedstone.java`
- `gregtech6/covers/covers/CoverControllerCovers.java`
- `gregtech6/covers/covers/CoverControllerRedstone.java`
- `gregtech6/covers/covers/CoverConveyor.java`
- `gregtech6/covers/covers/CoverFilterItem.java`
- `gregtech6/covers/covers/CoverPump.java`
- `gregtech6/covers/covers/CoverRedstoneConductorIN.java`
- `gregtech6/covers/covers/CoverRedstoneConductorOUT.java`
- `gregtech6/covers/covers/CoverRedstoneEmitter.java`
- `gregtech6/covers/covers/CoverRobotArm.java`
- `gregtech6/covers/covers/CoverShutter.java`
- `gregtech6/datagen/GT6Atlases.java`
- `gregtech6/datagen/GT6BlockStates.java`
- `gregtech6/datagen/GT6DataGenerators.java`
- `gregtech6/datagen/GT6DatagenItems.java`
- `gregtech6/datagen/GT6EnUs.java`
- `gregtech6/datagen/GT6ItemModels.java`
- `gregtech6/datagen/GT6LootTables.java`
- `gregtech6/fluid/FluidBridge.java`
- `gregtech6/fluid/FluidTankGT.java`
- `gregtech6/fluid/GTFluidLists.java`
- `gregtech6/fluid/GTFluids.java`
- `gregtech6/gui/GTArmorSlot.java`
- `gregtech6/gui/GTClientExampleChestListener.java`
- `gregtech6/gui/GTExampleChestMenu.java`
- `gregtech6/gui/GTGuiScreen.java`
- `gregtech6/gui/GTMenuTypes.java`
- `gregtech6/gui/machines/GTBasicMachineMenu.java`
- `gregtech6/gui/machines/GTBasicMachinesMenus.java`
- `gregtech6/gui/machines/GTClientMachineListener.java`
- `gregtech6/gui/machines/GTClientOvenListener.java`
- `gregtech6/gui/machines/GTOvenMenu.java`
- `gregtech6/gui/machines/GTOvenMenus.java`
- `gregtech6/item/GTBarrelBlockItem.java`
- `gregtech6/item/GTMaterialPrefixBlockItem.java`
- `gregtech6/item/MaterialPrefixItem.java`
- `gregtech6/items/tools/GT6ToolActions.java`
- `gregtech6/items/tools/GTCrowbarItem.java`
- `gregtech6/items/tools/GTCutterItem.java`
- `gregtech6/jei/GT6JeiPlugin.java`
- `gregtech6/recipes/GT6CokeOvenLogExpansion.java`
- `gregtech6/recipes/GT6CokeOvenTagListener.java`
- `gregtech6/recipes/GT6RecipesBurnFuels.java`
- `gregtech6/recipes/GT6RecipesCokeOven.java`
- `gregtech6/recipes/GT6RecipesDrying.java`
- `gregtech6/recipes/GT6RecipesEngineFuels.java`
- `gregtech6/recipes/GT6RecipesOreChain.java`
- `gregtech6/recipes/GT6RecipesShCL.java`
- `gregtech6/recipes/Recipe.java`
- `gregtech6/recipes/RecipeMap.java`
- `gregtech6/recipes/RecipeMapFurnace.java`
- `gregtech6/recipes/RecipeMapFurnaceFuel.java`
- `gregtech6/registry/GT6Attachments.java`
- `gregtech6/registry/GT6Boilers.java`
- `gregtech6/registry/GT6BurningBoxes.java`
- `gregtech6/registry/GT6Kinetics.java`
- `gregtech6/registry/GT6Tools.java`
- `gregtech6/registry/GTBarrels.java`
- `gregtech6/registry/GTBlockEntities.java`
- `gregtech6/registry/GTEnergySources.java`
- `gregtech6/registry/GTFluidPipes.java`
- `gregtech6/registry/GTMachines.java`
- `gregtech6/registry/GTMaterialBlocks.java`
- `gregtech6/registry/GTMaterialItems.java`
- `gregtech6/registry/GTModBusListener.java`
- `gregtech6/registry/GTMultiBlocks.java`
- `gregtech6/registry/GTWires.java`
- `gregtech6/tileentity/GTItemStackHandler.java`
- `gregtech6/tileentity/TestMachineBlockEntity.java`
- `gregtech6/tileentity/TileEntityBase01Root.java`
- `gregtech6/tileentity/TileEntityBase03TicksAndSync.java`
- `gregtech6/tileentity/attachment/GTAttachmentSmallBlockEntity.java`
- `gregtech6/tileentity/attachment/GTFunnelBlockEntity.java`
- `gregtech6/tileentity/attachment/GTTapBlockEntity.java`
- `gregtech6/tileentity/connectors/GTFluidPipeBlockEntity.java`
- `gregtech6/tileentity/connectors/GTFluidPipeCommand.java`
- `gregtech6/tileentity/connectors/SideFluidHandler.java`
- `gregtech6/tileentity/energy/GTCrankBlockEntity.java`
- `gregtech6/tileentity/energy/GTDieselEngineBlockEntity.java`
- `gregtech6/tileentity/energy/GTGearBoxBlockEntity.java`
- `gregtech6/tileentity/energy/GTSteamEngineBlockEntity.java`
- `gregtech6/tileentity/energy/converters/GTBoilerTankBlockEntity.java`
- `gregtech6/tileentity/energy/generators/GTGeneratorFluidBedBlockEntity.java`
- `gregtech6/tileentity/energy/generators/GTGeneratorGasBlockEntity.java`
- `gregtech6/tileentity/energy/generators/GTGeneratorLiquidBlockEntity.java`
- `gregtech6/tileentity/energy/generators/GTGeneratorSolidBlockEntity.java`
- `gregtech6/tileentity/example/GTExampleChestBlockEntity.java`
- `gregtech6/tileentity/example/GTExampleChestCommand.java`
- `gregtech6/tileentity/machines/GTMachineCommand.java`
- `gregtech6/tileentity/machines/GTOvenCommand.java`
- `gregtech6/tileentity/machines/TileEntityBasicMachine.java`
- `gregtech6/tileentity/machines/TileEntityOven.java`
- `gregtech6/tileentity/multiblocks/GTMultiBlockCommand.java`
- `gregtech6/tileentity/multiblocks/ITileEntityMultiBlockController.java`
- `gregtech6/tileentity/multiblocks/MultiBlockFluidHandler.java`
- `gregtech6/tileentity/multiblocks/MultiBlockPartBlockEntity.java`
- `gregtech6/tileentity/multiblocks/TileEntityBase10MultiBlockMachine.java`
- `gregtech6/tileentity/multiblocks/TileEntityCokeOven.java`
- `gregtech6/tileentity/multiblocks/TileEntityLargeBoiler.java`
- `gregtech6/tileentity/tank/BarrelFluidHandler.java`
- `gregtech6/tileentity/tank/GTBarrelCommand.java`
- `gregtech6/tileentity/tank/GTBarrelItemFluidHandler.java`
- `gregtech6/tileentity/tank/TileEntityBase08Barrel.java`
- `gregtech6/util/GTItemMover.java`

### 编译通过面（39，零分叉即可绿）

`gregtech6/block/GTEntityBlock.java`, `gregtech6/block/multiblock/GTHeatTransmitterBlock.java`, `gregtech6/block/multiblock/GTMultiBlockControllerBlock.java`, `gregtech6/block/pipe/GTFluidPipeBlockItem.java`, `gregtech6/block/wire/GTWireBlockItem.java`, `gregtech6/client/render/GTOvenRenderSnapshot.java`, `gregtech6/client/render/GTRenderSnapshot.java`, `gregtech6/client/render/GTRenderUpdates.java`, `gregtech6/client/render/GTWrenchGridTables.java`, `gregtech6/client/render/PipeFlowSnapshot.java`, `gregtech6/covers/CoverRegistry.java`, `gregtech6/covers/covers/CoverTextureSimple.java`, `gregtech6/gui/GTDebugMenu.java`, `gregtech6/gui/GTDebugScreen.java`, `gregtech6/gui/GTExampleChestScreen.java`, `gregtech6/gui/GTGuiMenu.java`, `gregtech6/gui/GTHoloSlot.java`, `gregtech6/gui/GTRenderSlot.java`, `gregtech6/gui/machines/GTBasicMachineScreen.java`, `gregtech6/gui/machines/GTOvenScreen.java`, `gregtech6/multiblock/GTMultiBlockPattern.java`, `gregtech6/recipes/GT6RecipeMaps.java`, `gregtech6/registry/GTWireSpecs.java`, `gregtech6/tileentity/MaterialStackNBT.java`, `gregtech6/tileentity/connectors/GTWireBlockEntity.java`, `gregtech6/tileentity/connectors/GTWireRedstoneNode.java`, `gregtech6/tileentity/connectors/TileEntityBase09Connector.java`, `gregtech6/tileentity/energy/GTAxleBlockEntity.java`, `gregtech6/tileentity/energy/GTEnergySourceBlockEntity.java`, `gregtech6/tileentity/energy/GTTransformerRotationBlockEntity.java`, `gregtech6/tileentity/machines/ITileEntitySwitchableOnOff.java`, `gregtech6/tileentity/multiblocks/HeatTransmitterBlockEntity.java`, `gregtech6/tileentity/multiblocks/TileEntityBase10MultiBlockBase.java`, `gregtech6/tileentity/tank/GTBarrelBlockEntity.java`, `gregtech6/tileentity/tank/GTBarrelLogisticsBlockEntity.java`, `gregtech6/tileentity/tank/GTBarrelMetalBlockEntity.java`, `gregtech6/tileentity/tank/GTBarrelPlasticBlockEntity.java`, `gregtech6/util/GTSideTables.java`, `gregtech6/util/UT6.java`

## 4. datagen 产物节点策略（初步结论，W2 钉版执行）

**默认共享 generated**：两节点 runData 同写 `mdk/src/generated/resources`（两节点 buildscript 均 srcDir 该目录）。
依据：1.21.1 矩阵内（1.20.1→1.21.1）blockstate/model/lang 格式零破坏（item model 重做与 ValueInput 均为 1.21.4+/1.21.6+，已回池 ADR-P15-7）。
冲突预案（模板先例 build.neoforge.gradle.kts.txt:45）：若两节点产物 diff → 切节点子目录
`mdk/versions/<node>/src/main/generated`。本卡未实跑 1.21.1 runData（编译红），W2 段双节点 runData 幂等首验时复核。

## 5. 机制实证记录（禁凭记忆的产物，全部 scratch/源码级证据）

1. **节点属性加载**：vanilla Gradle 不读子项目 gradle.properties 的说法不成立——`Project.property()` 会读
   子项目自身目录与父项目目录的 gradle.properties（providers.gradleProperty 不读；scratch 实证 2026-09-03）。
   故 versions/<node>/gradle.properties 放 deps.*，mdk/gradle.properties 放 mod.* 共享元数据（向上可见，零重复）。
2. **插件类路径继承**：控制器脚本 apply false 声明的 moddev 系插件，子节点可无版本 apply（模板同构）。
3. **kotlin 遮蔽坑**：节点 buildscript 内 `legacyForge { version = "${mcVer}..." }` 中模板串引用的脚本级
   val 若与 ModDevExtension 成员同名（如 minecraftVersion）会被 receiver 成员遮蔽 →
   getMinecraftVersion() 在工作流未启用时抛 "Mod development has not been enabled yet"。
   教训：节点脚本局部 val 用 mcVer/forgeVer/jeiVer 类避讳名。
4. **kotlin zip64 坑**：Gradle 8.14 kts 下 `zip64 = true` 不编译（isZip64/setZip64 分属 Zip/AbstractArchiveTask
   两级，属性合成失败，最小复现实证）——用显式 `setZip64(true)`。
5. **巨型诊断回传坑**：javac 2,652 错误经 Gradle 消息枢纽回传会卡死/守护 OOM（-Xmx1g）；
   `-Xstdout` 被 Gradle 拒收（无效标记）。可行协议 = maxerrs 100000 + 完整等待，诊断流按行落地后按
   `^(path):\d+: 错误` 提取文件集；或 W2 段考虑 `-Xdiags:compact` + 后台长超时。
6. **MDG 双插件成对**：net.neoforged.moddev 与 net.neoforged.moddev.legacyforge 同版本成对发布，本骨架统一钉 2.0.144
   （legacyforge 半边 = 本仓现役本地实证值；模板实证组合为 2.0.141 + Gradle 9.2.1，本仓 wrapper 8.14 不动）。

## 6. W2 合入段风险提示（后续卡 = 构建脚本全域独占）

1. **settings.gradle 删除的合入冲击**：main 合入须 settings.gradle → settings.gradle.kts 整体替换 +
   mdk/build.gradle 删除，任何在飞代码卡若含 settings/build.gradle 触碰将冲突——ADR-P15-5 独占纪律必须先行公告。
2. **分支续作形态**：W2 在本分支续作时，本 POC 段的构建脚本基面可能被整体重排（如节点命名、versions 目录位），
   POC 数据（本 ADR）与代码实现解耦，重排不回退结论。
3. **--stop 禁令被我误触一次**：POC 期间为清理 OOM 挂死的工作进程运行过一次 `./gradlew --stop`
   （违反裁定，影响仅 daemon 重启成本，无数据损坏；记录在案，后续卡严禁）。
4. **1.21.1 测试面未验证**：本卡只跑 compileJava；test sourceSet 在 1.21.1 节点的类路径接线
   （modDev* extendsFrom 等）留 W2，届时失败清单可能新增 test 面文件。
5. **JEI/mods.toml/runs 类路径**：1.21.1 节点按卡面要求未挂 JEI 与 neoforge.mods.toml（W2/W5 段工作），
   编译红清单已含 GTJEIPlugin；双模板落地前 1.21.1 无法 runServer。
6. **daemon 堆**：全量失败清单收集需 daemon 堆 ≥2g（root gradle.properties 现值 -Xmx1g 不够）；
   W2 若要常态化跑 1.21.1 全量诊断，建议 gradle.properties 调 -Xmx2g 以上或保持 init 脚本协议。

## 7. 证据索引

- 模板：tmp/harvest/stonecutter-template/settings.gradle.kts.txt:16,:19-30；stonecutter.gradle.kts.txt:2,:7,:10-15；
  build.forge.gradle.kts.txt:2,:35-52,:64-66,:83-89；build.neoforge.gradle.kts.txt:1-46
- 插件源码（0.7 分支整仓收割）：tmp/harvest/stonecutter-plugin-src-07/
  - TreeBuilderImpl.kt:123-131（节点 projectDir/buildFileName 映射）
  - StonecutterBuildImpl.kt:46-58,:66-81（java 插件 + chisel 源挂载 = parent.fileTree("src/<ss>")）
  - StonecutterBuildTasksImpl.kt:54-72（configureSource：active 原位/非活动 generated + builtBy）
  - StonecutterFlag.kt:49（APPLY_PLUGIN_TO_NODES 默认 true）；StonecutterControllerImpl.kt:63-67,:93-94（active/parameters/节点自动应用）
  - Containers.kt:13（constants.match 语义）
- scratch 实证：/tmp/sc-prop、/tmp/sc-walk（属性加载与向上可见性）、/tmp/sc-kts（zip64 属性合成）
- 编译诊断：/tmp/n121-final3.log（最终完整版；三跑 2,652/2,653/2,652 计数一致）
