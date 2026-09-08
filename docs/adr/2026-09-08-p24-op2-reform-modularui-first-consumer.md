# ADR-P24 草稿：OP(2) 收窄放开重裁 + ModularUI 首消费接线（ACT 机器双决策）

日期：2026-09-08 ｜ 前置：ADR-P20 ModularUI fork 裁决与 ADR-P23 子仓化（vendored→submodule 落点链） ｜ 评审基线 main=9938a4d4（P24 实现面收官） ｜ 决策卡 state decisions.p24-creative-form-op2-narrowing（被重裁的前裁定）／decisions.p24-builder-wand-op2-reform／decisions.p24-act-be-form／decisions.p24-act-ghost-form ｜ 实现卡 work/p24-act-machine（merged 9938a4d4，2026-09-08）／work/p24-builder-wand（consumed_by）

- 状态：Accepted（P24 收官主会话正典化 2026-09-08）

## 0. 摘要

builder wand 卡落地触发 OP(2) 重裁：creative-form 收窄裁定（creative-or-OP(2)）按其内嵌 reform_constraint 放开为上游生存语义——缝先行红利：仅两处内联喂法变更，FormSeamTest 4 臂零改动。ACT 机器同时成为 ModularUI vendored 后首消费者：mdk 双腿 build 接线（forge modCompileOnly/neo compileOnly+回退臂模式）与幽灵格混合形态（服务端背衬+phantom 呈现）。

## 1. OP(2) 重裁链：收窄→放开

### 1.1 前裁定及其自嵌约束（decisions.p24-creative-form-op2-narrowing，2026-09-07）

本仓多方块 canEdit 当时裁定**维持收窄=creative-or-OP(2)**（ITileEntityMultiBlockController.java:166-168 与 GTMultiBlockStructureChecker.java:259 两处内联同源）；上游 GT6 语义=UT.Entities.canEdit（UT.java:3159）=非玩家放行||vanilla canPlayerEdit，生存玩家实践为真——上游生存可带库存 scaffold，本仓裁掉该臂。当时不可达（builder wand 物品未移植），故卡内写死 **reform_constraint_for_wand_card**：未来 wand 卡拆卡时必须重裁本裁定；若放开上游语义，p24-creative-form-seam 的 (mayEdit,infiniteItems) boolean 缝已能表达生存臂 (T,F)——「只是参数喂法变更，非缝位重构，测试臂即回归基座」。

### 1.2 重裁（decisions.p24-builder-wand-op2-reform，2026-09-07）

放开为上游语义：**mayEdit = aPlayer==null || aPlayer.getAbilities().mayBuild**（vanilla canPlayerEdit 的 1.20.1 等价，survival/creative 过、adventure 拒）。证据链（上游生存带料双证，逐行复核）：

1. 上游 scaffold 臂生存真实可达：gregapi ITileEntityMultiBlockController.java:58-66 生存库存倒序 ST.use(1) 消耗分支，其门 :53 UT.Entities.canEdit（UT.java:3159-3167）；
2. 上游表面臂同依赖：Behavior_Builderwand.java:94-95/:119-120 canPlayerEdit 双查+:123-128 生存 tryPlaceItemIntoWorld+ST.use+doDamage(1)。

否决维持收窄：生存非 OP 持 wand 在 checker:284 得 'no permission to scaffold' 硬错=wand 变 OP 限定道具，与上游可见偏离且 UX 不可接受。/gtmultiblock 命令面行为实际不变（null player 本就放行，真玩家敲命令自身已需 OP2）。

### 1.3 模式红利：缝先行、重裁=喂法变更

落地仅改**两处内联喂法**（ITileEntityMultiBlockController.java:113 与 GTMultiBlockStructureChecker.java:247；check() 的 SET walk 经 Util 公开包装同源传递），seam 重载（Util:127-167/checker:261-317）与 **FormSeamTest 4 臂零改动**；arm2 (T,F) 恰耗一绿臂（FormSeamTest:158）与 arm3 短料事务拒臂（:174）=重裁回归基座。测试边界：wrapper 喂法本身离线不可测（Forge patch Entity ctor 强制 FluidType.SIZE，FormTest:74-80 墙）——两处内联必须同源注释互引本 decision，防单边漂移。**「收窄裁定内嵌 reform constraint」的做法（当时断言未来重裁成本）被本次兑现，可复用为裁窄类 ADR 的标准配件。**

## 2. ModularUI 首消费接线（ACT 卡）

### 2.1 BE 形态（decisions.p24-act-be-form）

独立 BE 直译，不走 BasicMachine 能量族：上游 ACT extends TileEntityBase09FacingSingle（:72）非 TileEntityBasicMachine——零能量、零 tick 自动合成（refill 空桩 :186-188，纯玩家点击驱动）。本仓形态=新 BE extends TileEntityBase03TicksAndSync+MenuProvider（TileEntityOven.java:120 亲验先例），71 槽 ItemStackHandler+槽段常量（上游 :495-503），NBT saveAdditional/load；注册 GTMachines 三连追加行（:58-70 形态），id gt6:advanced_crafting_table 单变体。否决复用 GTBasicMachine 骨架（能量/tier/recipe-map 语义错位）与 TileEntityOven 裁剪复用。

### 2.2 首消费=编译接线首次发生

P20 卡①②只完成 vendored+jarJar 嵌装，mdk 主树零 import brachy.*（sym_query 实证），compile 接线一直未发生。ACT 卡 C2 前置门=mdk 双腿 build 脚本声明对 third-party/modularui 的 **compile+runtime 依赖**：forge 腿 modCompileOnly / neo 腿 compileOnly，运行时 class 由既有 jarJar 嵌装供给；烟测断言类路径出现 brachy 类。**接线门通过，回退臂未启用**（tasks.p24-act-machine note）——回退臂=vanilla MenuType+自研 phantom 语义（dryer MenuType+GTBasicMachineMenu p16 先例），且回退须回报 architect 再动（防静默降级）。

### 2.3 幽灵格混合形态（decisions.p24-act-ghost-form）

三层承载，size-0 栈永不落 IItemHandler（正典 GT6Circuits.java:40-41）：

1. **服务端 BE 背衬**：ItemStack[9] mPattern（仅 item+components，count 恒规范化 1，saveAdditional/load 持久化）；slot30=Selector 时按 meta2-9 图案表（上游 :222-283 八形态）写入；铺料前实物扫拢语义（:215-221 扫至槽24）由 paving 时校验/转移保持。
2. **GUI 呈现**：ModularUI PhantomItemSlot 绑 mPattern（先例 third-party/modularui/.../PhantomItemSlot.java:16+PhantomItemSlotSyncHandler.readOnServer :68-79 双腿同形，phantomClick 服务端 set 语义=可承载图案编辑）。
3. **配方查找**：构建虚拟 3x3（1.20.1 CraftingInput）喂 RecipeManager，绝不从真实槽读幽灵；真实 21-29 槽保留 IItemHandler 实物语义（手动放置+消耗源）。

否决：纯 phantom widget 无服务端背衬（图案不可持久化+离线测试不可达）；背衬用独立 IItemHandler（泄漏给自动化/掉落，裸数组+显式 NBT 才对位上游幽灵非实物语义）。

## 3. 验收留痕（tasks.p24-act-machine）

- 门禁合并态：forge 168 suites 1392/0/0/0+neo 1394/0/0/2skip；runData 双腿二跑 written:0+git 零漂移；RCON /gt6act 九臂链双腿 [0,0]；runServer Done 1.817s+ACT 注册行 71 槽+mPattern 幽灵背衬。
- tag 快照 PIN 3621→3622（ACT 行 +1，三方归一，见 tags-provider 范式篇 §5）；C1 BE+C2 接线+修复 d99d96d3（rebase 时 LootTables 双 provider 冲突正则误并复原）共 3 笔，30 文件 +2719/-13；files_scope 外申报越界=GT6CapabilityWiring/SeamTest（item-only 第二实例），S4 审 5/5 通过。
- 遗留：runClient GUI 目视留用户。

## 4. 后果

- 正面：缝先行投资兑现——重裁成本被前裁定精确预言并压到「两处喂法+零测试改动」，收窄裁定的 reform_constraint 模板化可复用；ModularUI 从「嵌装完而无消费者」转为在产依赖，GUI 框架选型债出清；幽灵格混合形态成为后续 ghost/phantom 需求（Canner 灌装、cover 配方等池项）的先例。
- 义务：未来任何 canEdit 类缝新增消费点必须引用同源注释锚定 vanilla canPlayerEdit 语义（adventure 拒）；ModularUI 接线形态（modCompileOnly/compileOnly+jarJar 运行时）成为后续 GUI 卡的标准前置门。
- 边界：OP(2) 语义以 vanilla 为锚而非自定义权限面；生存带料 scaffold 与上游 parity 由双证锚定，若上游行为变更须重开裁定点。
