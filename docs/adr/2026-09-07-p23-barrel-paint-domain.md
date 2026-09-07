# ADR 草稿：桶漆域拆卡（p22-pool-barrel-paint-domain → 3 卡）

- 状态：Accepted（P23 收官主会话正典化 2026-09-07）
- 日期：2026-09-07
- 基线：main b9b0b23f（P22 收官态）
- 全卡：state `tasks.p23-arch-barrel-paint`；决策 `decisions.p23-barrel-paint-domain-split`

## 背景与题面重定性

P22 移池注记（tasks.p22-arch-feature-wave）称"barrel 不在 paintableBlockArray，只做物品侧会物品有色/世界无色割裂"。本次考古把题面三缺口重定性：

1. **喷涂路由桶已通**：port 桶基类 `mdk/src/main/java/gregtech6/tileentity/tank/TileEntityBase08Barrel.java:98` `extends TileEntityBase03TicksAndSync`（:54 `implements IPaintableTE`）——P21 把 IPaintableTE 挂 03 全族时桶已随继承入 Paintable 家族（上游对位：`tmp/gt6-1.7.10/.../gregapi/tileentity/tank/TileEntityBase08Barrel.java:52` extends 07Paintable）。`GTSprayCanItem.useOn`（:189 instanceof IPaintableTE）与 `/gt6machine paint` 臂（`GTMachineCommand.paintableAt` :655-659，javadoc 明言"the future connector/barrel rides"）今天就能漆桶——NBT 层生效、零视觉（ Spray 白付 10 units 的活体怪相）。
2. **世界侧缺的是消费面**：存储/NBT（gt.color/gt.painted）/双通道同步/getModelData PAINT 供给全部已在 03 基类（:319-362），桶零覆写。缺的是：桶模型 `cubeAll` 无 tintindex（`GT6BlockStates.addBarrel` :226-245）+桶 16 块不在任何 BlockColor/ItemColor 注册（`GTMachines.paintableBlockArray` :395-417 只含 21 机器块，javadoc 明言桶扩展留池=本卡）。
3. **物品侧缺两半**：桶掉落不带漆键——`GTBarrelBlock.writeItemNBT`（:157-174）只写 tank+covers，`GTBarrelBlockItem.applyItemNBT`（:98-113）同；桶物品无 ItemColor 注册。
4. **副产物 bug（机器域）**：机器 loot copy_nbt 把漆键写进 `BlockEntityTag.gt.*`（`GT6LootTables.paintCopyNbt` :512-516），而 `GTItemPaintTint` 读 stack 根 tag（:56-62）——两缝不相交，loot 掉落的已漆机器物品栏不显色。已入 known_bugs（kb-painted-item-tag-mismatch），拆独立修复卡。

## 裁定

### R1 桶物品携漆走根键 writeItemNBT/applyItemNBT 对（否决照抄机器 loot 路线）

上游正缝就是根键：1.7.10 无 MTE loot 表，掉落带漆 = `getDrops → writeItemNBT`（notick03:157-162），漆键随整 TE 序列化落 stack 根（`TileEntityBase07Paintable.java:89` recolorItem `UT.NBT.set(aStack, writeItemNBT(...))`）。桶族 table-less + `GTBarrelBlockItem.applyItemNBT` 显式读回已存在——加两键零新机制。门=仅 `mIsPainted` 时写（镜像 03 saveAdditional :322-325），空桶无盖无漆保持 null tag 与 pre-card 字节同形。

### R2 桶模型 v1 = tinted 单 element 整桶染；两层借图入池

上游桶渲染是两层制（`MultiTileEntityBarrelWood.java:42-55`：`BlockTextureMulti(colored/×mRGBa, overlay/ 不染)`；Plastic:42/plasticcan、Metal:39/drum、Logistics:45/logistics 同构，与机器 getTexture2:1014 同构）。但 port 桶占位 PNG 已目验全灰度（barrel_wood/metal/plastic）——单纹理=上游 colored/ 层等价，整桶染不产生"染错层"错误，仅缺 overlay decal 视觉细节。v1 用 `tintedCubeAll` 形态（:740-750 wire 先例，桶是一纹理 shortcut 适用）；colored/+overlay/ 24 PNG 借图+7 element 模型沿 P22 front-overlay 拆层先例入池（视觉保真独立于漆功能）。未漆=-1 恒等零回归（P21 已证数值）。

### R3 桶注册走新 `GTBarrels.paintableBlockArray()`（16 块），机器 census 21 不动

`GTMachines.paintableBlockArray` 被 3 个既有测试钉死 21；域不分。新 walker = BARREL/BARREL_PLASTIC/BARREL_METAL/BARREL_LOGISTICS + METAL_DRUM_BLOCKS 12 = 16。`GTClientHandlers` 用同一 `GTMachinePaintTint.blockColor()`/`GTItemPaintTint.itemColor()` 对桶数组双注册——两类文件 diff 为空（lambda 本就域无关，PAINT property 查找即门）。

### R4 死缝修复独立卡（p23-painted-item-tag-fix），不夹带桶域

读键升两级：BlockEntityTag 优先（loot 形）+根回退（桶载体/creative 形）。`GT6LootTables` 零改动——BlockEntityTag 是 vanilla `updateCustomBlockEntityTag` 放置回读的承重键（:472-477），不能挪。

### R5 桶 unpaint=白（声明偏离沿机器）

上游桶 unpaint 恢复 `mMaterial.fRGBaSolid`（桶有材质），port 桶与机器同样无材质字段——沿用 IPaintableTE 声明偏离返回白。

## 分叉点清单（桶域 vs 机器域，file:line）

| 面 | 机器域 | 桶域 | 证据 |
|---|---|---|---|
| 掉落携漆 | loot 表 copy_nbt→BlockEntityTag（GT6LootTables:512-516） | getDrops 覆写→根键（GTBarrelBlock:141-174；上游 notick03:157-162 正缝） | 桶族 table-less 是 p12 裁定 |
| 放置回读 | vanilla updateCustomBlockEntityTag 零代码 | GTBarrelBlockItem.applyItemNBT 显式（:98-113） | 机器 items 是纯 BlockItem |
| 物品 tint 读键 | 根 tag（现实现，loot 形错位=bug） | 根 tag（R1 载体天然对齐） | GTItemPaintTint:56-62 |
| 世界模型 | 多 element（body tintindex0+decal 无 tint，P22 拆层） | v1 单 element 整桶染（两层入池） | GT6BlockStates:312-362 vs :226-245 |
| census | GTMachines.paintableBlockArray=21（3 测钉死） | GTBarrels.paintableBlockArray=16（新） | GTMachines:395-417 |

## 在途冲突检查

零重叠。tools/rcon（桶 RCON 链=新数据文件 chains/p23_barrel_paint.py，expect 串写法等在途 poll/expect 修复合入后对齐——时序注记）；mdk gregtech6/jade 包、third-party/modularui+settings.gradle.kts、mdk/build.*.gradle.kts 均零触碰。共享面观察：GT6BlockStates/GTClientHandlers/GTBarrels 当前无在途写者；排队的 p8-prefixblock-render B 也触 GT6BlockStates——后合者 rebase 重签。

## 派发

| 卡 | 分支 | 成本 | 依赖 |
|---|---|---|---|
| p23-barrel-paint-item-seam | work/p23-barrel-paint-item-seam | 小 | 无 |
| p23-barrel-paint-render | work/p23-barrel-paint-render | 中 | 文件域与 A 不相交可并行；合入序 A→B |
| p23-painted-item-tag-fix | work/p23-painted-item-tag-fix | 微 | 无（独立域） |

验收细节与 files_scope 全量见 state `tasks.p23-arch-barrel-paint`。
