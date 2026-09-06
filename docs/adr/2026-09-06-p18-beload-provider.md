# ADR-P18 草稿：多方块机 21.1 BE load NPE（P1 数据丢失）修复裁决——A 先落（NBT_ACCESS 对齐）+ B 根治登池（W4 provider 穿透）

日期：2026-09-06 ｜ 状态：正典（P18 收官成文，main fc494c6b） ｜ 前置：ADR-P17 machine-io（2026-09-05-p17-machine-io-adjudication.md，Delta 3 定性修正） ｜ main=2c3fc966 ｜ 诊断全文 state tmp.p18.diag-formscaffold-beload（debugger 三遍活体） ｜ 卡 tasks.p18-beload-2111-nbtaccess ｜ 池 tasks.pool-w4-nbt-provider-threading

## Context

**a) 病灶与死因（已证，file:line）**

`TileEntityBase10MultiBlockMachine` 的 21.1 neoforge 分叉 NBT 面在 `load()` 首行取 `net.minecraft.core.HolderLookup.Provider aProvider = getLevel().registryAccess()`（TileEntityBase10MultiBlockMachine.java:830）。vanilla 1.21.1 的 chunk 盘面加载路径在 BE 还没 setLevel 时就调 loadAdditional：`ChunkSerializer.postLoadChunk` lambda（:440）→`BlockEntity.loadStatic`（:176，`BET.create(pos,state)` 刚建、无 level）→`loadWithComponents`（:83）→`TileEntityBase01Root.loadAdditional(aNBT,aProvider)`（Root:139-141，**丢弃 provider 参数**裸调项目形 `load(aNBT)`）→:830 对 null level 调 `registryAccess()` → NPE → vanilla `loadStatic` 捕 Throwable 记 ERROR 返回 null（BlockEntity:181-185）→ `ChunkSerializer:441-443` 跳过 `setBlockEntity`（:442）→ **BE 实例静默丢弃**：controller 成死方块，formed/输入输出物品/输出流体/能量/进度/facing 全弃于加载时刻。save 侧 :808 同式，仅因现有保存时机（chunk save 等）BE 已挂 level 而幸存——同隐患域。1.20.1 forge 分叉 load（:771-794）无 registry 访问，天然无此病。

活体取证：p16_form_scaffold 链同一 run 目录第二遍起链（持久世界残留 controller BE × forceload 触发盘面晋升）稳定 2 ERROR+2 WARN（/tmp/p18_form1211_run2.out、run3.out 栈逐字一致）；全新世界首遍 0 ERROR 证触发条件恰是「存档有 BE → chunk 重载」。

**b) provider 到底用来干什么（禁猜，读码）**

:830 取到的 `aProvider` 在整个 `load()` 里恰有**三个消费点**，全部是注册表 id 查找面：

1. `:831` `mInventory.deserializeNBT(aProvider, aNBT.getCompound(NBT_INVENTORY))` —— GT6ItemStackHandler 槽位；
2. `:843` `ItemStack.parseOptional(aProvider, tOutputItems.getCompound(i))` —— 输出物品；
3. `:848` `FluidStack.parseOptional(aProvider, tOutputFluids.getCompound(i))` —— 输出流体。

**c) NBT_ACCESS 等价面（读码证明，非推测）**

全 BE 树其余类 21.1 分叉统一用冻结静态 `TileEntityBase03TicksAndSync.NBT_ACCESS = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)`（TicksAndSync.java:190-191）：类初始化即成的 `HolderLookup.Provider`，**无 level 依赖**，经根注册表覆盖全部 built-in 注册表；TicksAndSync:186-188 注释即全树既定合同「item id lookup only」。三个消费点（b 节）解析的恰是 ITEM/FLUID 这类 built-in 面。生产先例：TileEntityOven（:709/:723/:739）、TileEntityBasicMachine（:1495/:1510/:1519）、GTExampleChestBlockEntity（:171/:186）、GTGeneratorSolidBlockEntity（:515/:531）的同三面全部经 NBT_ACCESS 在 21.1 生产跑通——诊断活体里同一条 loadStatic 路径上只有本类 NPE，即其余类的等价性实证。既有测试自知此耦合：TileEntityBase10MultiBlockMachineTest.java:288-290 注释「the load face reads level.registryAccess() (21.1)」靠 fixture setLevel 供给。

## Decision

**裁 A 先落：:808 与 :830 两点同卡弃 `getLevel().registryAccess()` 改 `NBT_ACCESS`**（卡 tasks.p18-beload-2111-nbtaccess，branch work/p18-beload-2111-nbtaccess）。**根治 B（provider 沿 Root 委托链穿透）登池 tasks.pool-w4-nbt-provider-threading，归 W4 NBT 波**，不与 P18 并行。

- 两点一并换的理由：:808 是同一条分叉腿里的同式地雷（未来 saveToItem/level-less 测试/任何 vanilla 保存时序变化全踩）；对 built-in 消费面两 provider 解析同一注册表，**byte 等价**，换 :808 零额外风险，且立即让本类与全树先例合同一致。
- 1.20.1 零改动由 chisel 结构性保证：改动全在 `//? if neoforge` 注释腿 :797-852 内，forge 腿 :749-795 一字节不动。
- 验收主腿=21.1 持久世界 RCON 链（改前基线=第二遍起链 2 ERROR+2 WARN；改后 0；`data get block` 快照 A_pre/A/B/C 逐键与逐字节 diff，A_pre vs A 兼证 :808 换面后序列化产物 byte 级不变）。配套 1.20.1 腿 `loadWithoutLevelRestoresMachineState` 用例钉「load 无 level 不抛」合同。

**否决的备选**：仅改 :830 不动 :808（留同域地雷，无收益）；立即做 B（全 BE 树签名波=W4 级大爆炸，A 已消 P1 症状，B 按既定 W4 路线）；try-catch 兜底 NPE（掩盖病灶、BE 域半初始化更危险）。

## Deviations

- **NBT_ACCESS 不含 dynamic/datapack 注册表**：携带 dynamic 注册表引用组件的 ItemStack（banner_pattern/jukebox_song/instrument 族等）经冻结视图 parse/save 会降级，而 level 的分层 registryAccess 含全量。此暴露面与全树既有 21.1 腿完全相同（非本卡新增），机器槽内容物域（机器组件/锭/板/工具）不触及；根治 B 移除该局限，已写入池项 MOTIVATION。
- **save 面在「BE 已挂 level」时机上能力微降**：同上一条——现状下 :808 用 level 视图，换后用冻结视图；built-in 面 byte 等价（验收 A_pre/A 快照逐字节 diff 实证），仅 dynamic 组件场景理论上变窄，与全树先例一致。
- 本卡不触 Root:138-146 委托结构（provider 丢弃点原样保留）——那是 B 的落点，混入即扩大爆炸半径。

## Consequences

- 正面：21.1 controller 经世界重启/chunk 卸载重载后 BE 完整存活（P1 数据丢失闭合）；全树 NBT 面合同归一（唯一带病类清除）；每 boot 2 ERROR+2 WARN 日志噪声消失（P17 Delta 3 的「观察」定性正式关闭）；:808 的未来时机地雷一并拆除。
- 风险与边界：改动锁 neoforge 注释腿，1.20.1 回归面≈0；行为验收依赖 21.1 RCON 真机链（21.1 腿测试 CI 现为 compile-only），链步骤零改动、断言走裸 RCON 不碰 rcon 域文件。
- 后续：B 登池（Root:133-137 注释与 :800-801 旧注释都已预留该路线；池项含全覆盖面：chunk load、saveToItem 等 item-form 域、getUpdateTag/saveWithoutMetadata 面、swap 表 :263-270 注入条款退役）；B 落地后 NBT_ACCESS 收缩为测试域（GTRecipesOfflineTestBase:204），本卡新增注释中的 W4 池指针即交接缝。
