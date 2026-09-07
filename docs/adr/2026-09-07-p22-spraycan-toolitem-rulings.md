# ADR 草稿：P22 喷漆罐工具物品域裁定（含 painted-item-domain 并篇裁定）

- 日期：2026-09-07
- 状态：Accepted（P22 收官主会话正典化 2026-09-07）
- 研究依据：state key=`tasks.p22-research-spraycan-toolitem`
- 实现卡：state key=`tasks.p22-spraycan-items`（commits 026716ff / 86beddfc / ffb4a38f，双腿 build 绿+12 离线测试）与 `tasks.p22-painted-item-domain`（merged `7ba12b14`，commits 669ca7fc / 42a2920c / 982053d1）
- 架构修正：state key=`tasks.p22-arch-feature-wave` review_record.corrections C-3（桶漆域出卡）
- 证据路径根：上游=`tmp/gt6-1.7.10/src/main/java/`，port=`mdk/src/`，GTCEu 参照=`tmp/refs/gtceu-modern/`

## 背景与证据

1. **上游工具物品域语义**（Behavior_Spray_Color.java:45-94）：ctor `mUses=aUses*10`（:54，内部×10 单位制；方块 -10/次 :78、实体 -50/次）；NBT 键 `gt.remaining`（:68/:83）；首用 full→used 中间罐替换（:70-74）、归零→empty 罐 swap（:85-92）；SFX.IC_SPRAY（:77）；stackSize==1 不可叠（:61）；tooltip 剩余=remaining/10.%10（:174-180）。注册面 `MultiItemRandomTools.java:243-245`：16 色+512 用+used 隐形罐（TD.Creative.HIDDEN）；:235 空罐、:269-273 除漆剂 256 用。
2. **路由链**：onItemUseFirst→colorize（:146-167，原版白名单 grass/glass/pane/stained/carpet/hardened_clay + BlockColored/rockwool 走 setBlock，色码 `~mColor&15`）→其余方块 `recolourBlock`（:166）→`MultiTileEntityBlock.java:240` IMTE 路由→TE.recolourBlock（`TileEntityBase04MultiTileEntities.java:227-235`）：**已漆=UT.Code.mixRGBInt 通道平均混色、未漆=直上、无色参=unpaint**——P21 已证反码复合恒等（免反码直存）。port 面 `IPaintableTE.java:23-54`（javadoc :12 预留 instanceof 路由）+`GTMachineCommand.java:106-109` DYES_INT 表。
3. **GTCEu 对照**：`ColorSprayBehaviour.java:191-203` setPaintingColor **直接覆盖不混色**——与 GT6 通道平均语义不同，P21 裁定已钉不采；`GTItems.java:2205-2213` 单物品/色+耐久条（:126-145）+无 used 隐形罐（:2212）可为先例。
4. **物品域缺口**：上游 getDrops 带漆（`TileEntityBase04MultiTileEntities.java:151` 无条件 self，notick03:157-162 writeItemNBT 写 gt.color/gt.painted）；port 机器 loot 表原本无 copy_components（唯一 round-trip 先例=桶 `GTBarrelBlockItem.java:89-95`）；vanilla `ItemColors.java:25-93` createDefault **无 BlockItem 自动委托**（:72-88 草叶手工转发）=物品染色必须显式注册的铁证。

## 裁定

### D1：工具形态=单物品/色 + gt.remaining 内部×10 单位制

17 新物品（16 色 512 用喷漆罐+除漆剂 256 用）+空罐物品（仅作耗尽 swap 目标）。单物品/色+`gt.remaining` NBT（long 载体、×10 内部单位制沿上游 :54 语义）+耐久条（isBarVisible 族）+stackSize 1。否决 used 隐形中间罐（见偏离①）。

### D2：paintPaintableTE 路由逐字 + GT6 混色保真

useOn 服务端 `instanceof IPaintableTE` 路由：已漆 `mixPaint` / 未漆 `paint(DYES_INT)` / 除漆 `unpaint`（上游 04:227-235 三分支逐字）；命中 -10+音效+耗尽换空罐。**混色语义保 GT6 通道平均，不采 GTCEu 覆盖式**（语义分歧实证见背景 3）。原版白名单族 setBlock 映射沿上游 colorize :144 表（除漆=Remover :101-103 反向三族逐字；羊毛/地毯无素色变体不反向，上游 remover :96-106 同样无羊毛臂）。

### D3：声明偏离（每条带先例）

1. **used 隐形中间罐裁掉**——GTCEu :2212 先例，省 17 物品+17 模型；`gt.remaining` 贯穿 full→empty 全生命周期。
2. **原版白名单两臂无目标裁掉列池**——`grass_block→BlocksGT.Grass`（GT 草族未移植）与 `TE_Rockwool`（TE 缺席），白名单表其余族完整。
3. **IC_SPRAY→SoundEvents.FIRE_EXTINGUISH 占位**——上游 IC2 tools.Painter 音效本仓无 IC2（CS.java:2249）；GTCEu 自定义音效留池。

v1 获取=创造栏+/give（灌装= Canner 未移植、空罐 crafting=弯板前缀不可达，均留池）。

### D4：并篇裁定——painted-item-domain（merged `7ba12b14`）

- **掉落携行**：21 机器方块 loot 表加 `copy_nbt`（source=block_entity→BlockEntityTag，恰 2 REPLACE op 写 `gt.color`/`gt.painted`，键常量与 saveAdditional 单决策点闭环）；放置回读走 vanilla `BlockItem.updateCustomBlockEntityTag`（:147 BlockEntityTag→BE.load）免 BlockItem 覆写。
- **ItemColor 显式注册**：`RegisterColorHandlersEvent.Item` 给 21 块 BlockItem 注册 `GTItemPaintTint`（读 stack NBT gt.color→tint，未漆 -1，与世界侧 `GTMachinePaintTint.tintARGB` 数值恒等 0xFF000000|0xFFFFFF==-1）；否决依赖 vanilla 自动委托（背景 4 铁证：无此机制）。
- **13 块 loot 缺失修复（既有缺口，非新偏离）**：上游 getDrops 无条件 self 忽略 silkTouch/fortune——13 块补 self-drop 忠实上游、8 块修改，21 表 census 测试钉形。
- 21.1 唯一分叉：CopyCustomDataFunction+CUSTOM_DATA 信封对称分叉，rides ADR-P17-1 未扩界。
- **C-3 边界钉死**：桶漆域出本卡（barrel 不在 paintableBlockArray，世界侧未覆盖，只做物品 tint=物品有色/世界无色割裂）→池项 `p22-pool-barrel-paint-domain`。

## 后果与池项移交

- 22 lang 行+18 模型 datagen（append-only）；runData 二跑 written:0；runServer 冒烟打印 16×512+256+空罐注册行。
- 留池：羊/狼实体腿（上游 :97-142）、C-Foam 泡沫族、Canner 灌装+空罐 crafting、GTCEu 自定义音效、桶漆域、物品栏染色 runClient 目验（收官清单）。
- 螺丝刀/circuit 不同车（依赖工具物品基座决策，沿研究卡 circuit_verdict）。
