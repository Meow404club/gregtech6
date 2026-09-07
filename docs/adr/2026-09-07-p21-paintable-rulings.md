# ADR 草稿（architect draft）：P21 机器喷漆 Paintable 拆卡五裁定

- 日期：2026-09-07
- 状态：Accepted（P21 收官主会话正典化 2026-09-07）
- 拆卡：card_A = `p21-paintable-storage-sync`，card_B = `p21-paintable-tint-render`（DEPENDS_ON card_A）
- 落账：state key=`tasks.p21-arch-paintable`；上游全证 state key=`tasks.p21-research-paintable`
- P20 W3 census 停手裁定衔接：未喷漆=白×灰图已保真，本两卡补真喷漆功能（P20 贴图 census 的 C1"mRGBa tint"卡由本两卡吸收，不再单拆）

## 裁定 1：paint 写入入口（本波无喷漆罐物品）

**裁定：最窄可验收入口 = IPaintableTE API 本体（离线单测直调）+ `/gt6machine paint|unpaint` 子命令臂；喷漆罐/除漆剂物品不进本卡。**

- 离线面：单测直调 `paint/mixPaint/unpaint`（`GTOfflineTestBase` 先例，NBT round-trip 对齐 `TileEntityBasicMachineNBTTest`）。
- 活体臂：`GTMachineCommand`（`mdk/src/main/java/gregtech6/command/`，literal `/gt6machine` :86 先例；p11 双注册劫持后为独占根）加 `paint <pos> <dye0-15|none>` 子命令，服务端经同一 IPaintableTE 写入并回读 RGB——对齐 p19 chisel 越卡偏离先例（无 RCON 活体臂则客户端染色物理不可达）。
- RCON 链入库 `tools/rcon/chains/p21_paintable.py`（三层框架，p14/p19 链形态）。
- 否决：测试 fixture 直写字段（绕过 API 无同步面可测）；新独立命令（违反"一特性一根/现根复用"惯例）。

## 裁定 2：paint 能力挂载点

**裁定：字段+API+NBT+getModelData 基础实现全部落在 `TileEntityBase03TicksAndSync`（`mdk/src/main/java/gregtech6/tileentity/TileEntityBase03TicksAndSync.java:44`），并抽 `IPaintableTE` 接口供 instanceof 路由。**

- 本仓机器域 BE census（sym_query sources=["project"] 实证）：BE 类仅两个——`TileEntityOven`（:120）与 `TileEntityBasicMachine`（:158）；block 面=oven(1)+shredder/crusher/lathe 各 T1-T4(12)+dryer(4)+distillery(4)=21 块（p16 ADR 旧口径 16 块 + distillery 4 行）。另有多块控制器（CokeOven/LargeBoiler，10 族）与连接器/桶（09/08 族）同挂 03 之下。
- 上游对位：07Paintable 本就是 01-10 链中段全家族继承层（机器 09→08→07、connectors、barrel、battery 26+ 类）；本仓 03 是 01-07 链折叠点，挂 03 = 全部已移植机器（及未来连接器/桶）免费获得，与上游拓扑同构。
- 接口 `IPaintableTE`（新文件 `mdk/src/main/java/gregtech6/tileentity/IPaintableTE.java`）：`paint(int rgb)/mixPaint(int rgb)/unpaint()/isPainted()/getPaint()`，镜像上游 Paintable:83-86 + 04:227-235；03 implements，喷漆罐物品卡（池）将来 `instanceof IPaintableTE` 即达。
- 否决：只放两个机器 BE（连接器/桶扩展时字段重复）；新增 07 层抽象类（单继承链重构，收益为零）。
- getModelData 组合纪律：03 基类实现=未漆返回 `ModelData.EMPTY` 语义、已漆经 `GTModelProperties.derive(super)` 放 `PAINT`（Integer immutable，ModelData 铁律 GTModelProperties.java:21-28）；`TileEntityOven` 既有 override 改 derive-from-super，PAINT/RENDER_SNAPSHOT/OVEN_SNAPSHOT 三键共存（单值属性各占一键，GTModelProperties:46-55 共存裁定先例）。

## 裁定 3：混色语义——直存免反码 + 通道平均混色跟 GT6

**裁定：存储=直存最终 0xRRGGBB int（不做 dye-index 字段、不做反码绕路）；已漆再喷=通道平均混色（GT6 语义），两分支现在就实现。**

- 等价性：上游路由 `~mColor&15` + `DYES_INT_INVERTED` 表复合恒等于 `DYES_INT[mColor]`（喷哪色得哪色；04:227-235 + UT.java:1576-1578 + CS.java:468-471），直存=声明等价简化，研究卡已证。
- 混色分支保留：`isPainted ? 通道平均 : 直存`（`mixRGBInt` UT.java:1576-1578 三行直译）。GTCEu `ColorSprayBehaviour:191-203` 覆盖不混色=与 GT6 偏离，不采。
- 客户端不依赖 `gt.painted`：上游 painted bool 本就不进客户端（isPainted 靠 RGB≠材质色推断）；本仓 BlockColor 只读 RGB，0xFFFFFF=白=不染=未漆视觉。

## 裁定 4：NBT 键名照抄上游，零迁移

**裁定：键名 `gt.color`(Integer)+`gt.painted`(Boolean) 逐字照抄上游（CS.java:1161-1162），写读走 `saveAdditional/load`；声明：1.7.10 世界本就不跨版本互迁，无迁移代码。**

- 免费同步面：03 的 `getUpdateTag()=saveWithoutMetadata()`（:172-183 双腿 fork 已在），paint 进 saveAdditional 后 chunk 包与 block-update 包两通道自动携带，客户端 `load()` 收敛——零新增包类。
- 写点三件套：`setChanged()+sendClientData()(:161-165 sendBlockUpdated 链)+requestModelDataUpdate()`；双腿唯一分叉=getModelData/requestModelDataUpdate 宿主接口名（Forge `IForgeBlockEntity`:153/:174 vs Neo `IBlockEntityExtension`:76/:96），卡内 stonecutter 注释锚定，无新 fork 预期（ModelProperty/ModelData 包名走既有 import swap 机制，GTModelProperties:10-11 无标记先例）。

## 裁定 5：files_scope 与合并序（对在途整备批三卡的核对）

**裁定：card_A WAVE-1 先行，card_B DEPENDS_ON card_A 合入后 rebase 派发；与整备批三卡无有效文件交集。**

- 整备批核对（state `tasks.p21-arch-hygiene-batch`）：卡① i18n=GT6EnUs/GT6ZhCn 两文件——零交集；卡② stoneblocks=GTStoneBlocks/GTStoneBlock/StoneVariant+GT6BlockStates(stone 段 :116/:848)+GT6LootTables+GT6ItemModels——**GT6BlockStates.java 文件级相邻，card_B 触 machineModel 段 :273-309，区段不相交，后合者 rebase（唯一相邻点，显式声明）**；卡③ modularui=third-party/modularui——零交集。
- card_B 其余文件（GTClientHandlers/GTMachines/GTMachinePaintTint 新文件）无在途卡触碰；machineModel 区 main 无在途卡（任务书核实项成立）。
- 池项（不入本两卡）：喷漆罐/除漆剂物品（工具物品卡）；物品域带漆 ItemColor（上游 :131 writeItemNBT 语义）；管线/桶/连接器的 paint 渲染消费（block 模型非 machineModel 生成）；多块控制器（CokeOven/LargeBoiler）paint 消费；RGB 直喷 onPainting（仅 Botania 可达）不移植。

## card_B 消费端锚点（备查）

- BlockColor 返回值=BE `getModelData().get(PAINT)`，缺省 0xFFFFFF；等价于上游 MultiTileEntityBasicMachine.java:1014 灰图×mRGBa 乘法（白=原样）。BlockColor 不染 BlockItem（GTClientHandlers:53-56 既有结论）。
- listener 注册：`GTClientHandlers.init`(:42-47) +1 concrete `.Block` listener（Neo 21.1 abstract 拒注册教训 :34-40，known_bugs gtclienthandlers-2111）；RegisterColorHandlersEvent.Block 两腿同形（Forge :48-78 / Neo :42-68）。
- datagen：`GT6BlockStates.machineModel`(:304-309) `models().cube(...)` 改 element+per-face `tintindex(0)` 形（`tintedCubeAll`:687-697 先例；机器六面异纹理，需 per-face 版非 cube_all）；`addMachine`(:284-301) 三模型 inactive/active/running 全带 tintindex 0；reachable census=21 块（钉数测试，GTStoneBlocksRenderDatagenTest 先例）。
