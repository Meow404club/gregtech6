# ADR：配方输入 tag fallback——上游 unification 语义的现代原语化（tag 双向范式第②向收官）

日期：2026-09-08 ｜ 前置：ADR-P24 tags-provider 双向范式（§3「用」方向欠账、§6 遗留指针「配方输入 tag 匹配的新映射层归配方系统研究卡」，docs/adr/2026-09-08-p24-tags-provider-paradigm.md） ｜ 评审基线 main=38365b89 ｜ 决策卡 state decisions.p25-tag-input-fallback-rulings／research.p25-r-tag-recipe-inputs ｜ 实现卡 work/p25-tag-input-machine-fallback（merged 71bd4286，5 提交 b0a5fc9c→5ef58ec9，S1 approve + S2 放行重签重验：rebase 3267e4ba 零冲突、forge cleanTest 1454/0/0/0 + neo 1456/0/0/2skip 精确对账、RCON 双腿重跑 [0,0]）

- 状态：Accepted（P25 收官主会话正典化 2026-09-08）

## 0. 摘要

GT6 1.7.10 的跨 mod 配方输入互通从来不是 tag 匹配，而是 unification 语义：机器输入→OreDict 关联→统一 canonical 栈→精确相等。本裁定把该语义原语化为现代 tag 形态：机器配方匹配升级为**两段式**（精确相等优先＋材料族 tag fallback），配**方向铁律**（TagKey 只从配方输入侧派生、只测机器输入侧）与**谓词注入缝**（`sTagTest`）——tag 双向范式第②向（移植配方输入用 tag 匹配、纳他 mod 同功能物品）自此闭合。

## 1. Context

### 1.1 上游 unification 机制本体（research.p25-r-tag-recipe-inputs census）

- 机器匹配链：上游 `RecipeMap.findRecipeInternal`（tmp/gt6-1.7.10 gregapi/recipes/Recipe.java:469-516）——:484 `aNotUnificated`→`OreDictManager.getStackArray` 统一输入；:498-515 `mRecipeItemMap` 哈希索引（(item,meta)＋meta 通配 W＋统一目标重试）。
- 比较委托：`Recipe.checkStacksEqual`（Recipe.java:773-793），:780 核心行＝`OreDictManager.INSTANCE.equal_(F, aInput, tInput, ...)`；probe→consume 两段在 `isRecipeInputEqual`（Recipe.java:800-818）。
- unification equal：`OreDictManager.equal_`（gregapi/data/OreDictManager.java:628-634）＝①`ST.equal` 精确；②机器输入 `getAssociation_`→`mUnificationTarget`（其自身 (prefix,mat) 的 canonical GT6 栈）→再 `ST.equal`。跨 mod 互通的机制本体是「输入栈关联 (P,M) 且 canonical==配方输入」，**非 tag**。
- blacklist：`mBlocked`（OreDictManager.java:659）。

### 1.2 本仓欠账

P24 收官时本仓 `Recipe.checkStacksEqual` 只有纯物品相等（javadoc 自述 OreDict manager equality replaced by item+tag equality，无 unification 面）；ADR-P24 §6 明示「配方输入 tag 匹配的新映射层（材料栈→tag）归配方系统研究卡」。本篇即其收官。

## 2. Decision

### 2.1 两段式等值（唯一插入点）

`mdk/src/main/java/gregtech6/recipes/Recipe.java`：

- `checkStacksEqual`（:343-372）：count 门卫**两阶段**（:358-359）——`isSameItemAndTag(aInput, tInput, tIgnoreNBT) || matchesByMaterialTag(aInput, tInput)`；精确分支语义原样（:375-383，`mNoNBTChecks || !hasTag` 规则保持）。
- fallback 三门全在 `matchesByMaterialTag`（:393-398）：①配方输入 `tInput.getItem() instanceof MaterialPrefixItem`（:394，public final `prefix`/`material` 字段＝零新索引的关联数据，MaterialPrefixItem.java:40-41）；②其 prefix 携带平台族 `GT6ItemTags.itemTagFamily` 非 null（:395）；③机器输入 `aInput` 是族 tag `<ns>:<family>/<material>` 成员（:397，经 `sTagTest`）。精确失败恰重试一次，非 MaterialPrefixItem 回退现状。
- 装载侧零触碰自动生效：`RecipeMap.findRecipe` 线性扫描（GT6RecipeMaps.java:100-151，含 mLastRecipe 快路径 :141）单插入点即全链生效；`GT6RecipesOreChain.java:162-178` 装载态不改。

### 2.2 方向铁律

TagKey **从配方输入侧派生、对机器输入侧测试**，永不反向（javadoc :313-321 与 :385-391 双处钉死；实现上 `aInput` 只进 `sTagTest.test`、从不当 MaterialPrefixItem 解析）。反向派生会让 plateIron 配方吞进任意 `ingots/iron` 成员。方向臂双向测试入 `GT6RecipeTagFallbackTest`。

### 2.3 NBT 语义：两规则并列声明（decisions.p25-tag-input-fallback-rulings ①）

tag fallback 分支**恒忽略 NBT**——上游 unification 把关联栈统一到 canonical 后 NBT 即 GT canonical 的 NBT（equal_ →ST.equal），机器输入携带无关 NBT 不应阻止匹配；精确分支的 `tIgnoreNBT` 规则原样保留，两者互不覆写（:323-330）。circuit `Damage` 配置路由不受扰：电路不是 MaterialPrefixItem，fallback 对其永不触发，`Damage` 精确匹配走精确分支（p14 偏离不破）。否决「fallback 也查 NBT」（翻上游 unification 口径）与「全分支恒 ignore」（毁 circuit 承重面）。

### 2.4 谓词注入缝 `sTagTest`（decisions ②）

`ItemStack.is(TagKey)` 离线不可解析（测试 JVM tag 管理器不启动，tag 全读空）。裁定＝静态缝：`VANILLA_TAG_TEST = ItemStack::is` 生产绑定＋`public static sTagTest` 测试可换（Recipe.java:98-114，creative-form-seam/ownerDestroyProgress 静态缝先例、与 `sNotConsumable` 同形）。验收双道非互斥：离线 stub 注入为主验收（红绿两向），RCON runServer 活体链为真 registry 实弹回证。否决 runServer-only（离线不可测违静态缝正典）与离线启动 tag 管理器（重量级 bootstrap＋时序彩票）。

### 2.5 族映射上提共享缝

`GT6ItemTags.itemTagFamily` 从 private 上提 public static（takeover hoist 兑现，提交 b0a5fc9c；research 卡 family_seam 在 GT6ItemTags.java:159-161 预留的注释位）。族覆盖随 prefix 滚动卡自动扩大，本机制卡不写族清单。

### 2.6 池冻结（decisions ③）

流体 tag 匹配（上游 fluids 无 oredict 互通，Recipe.java:804-808 精确量语义现状保留）；AdvancedCraftingTool 语义配对（头/柄材质配对＝语义配对器，AdvancedCraftingTool.java:86-106，静态 JSON 不可表达、代码配方才可另卡）；全 421 prefix 族铺开（随 prefix 滚动卡渐进）；meta 通配族 tag（1.20.1 无 meta，按需自建组 tag）。

## 3. Deviations（声明偏离与风险存档）

- **tag 分支恒 ignore NBT**：上游无 tag 分支（本分支即新原语），NBT 口径承 unification 等价面（§2.3，证据同）。
- **mBlocked blacklist 无逐栈复刻**（OreDictManager.java:659）：tag 是集合级真值；材料族 tag 成员全为本仓 authored 注册物品＋生态 twin（严格 throw 无 optional），集合面干净。他 mod 往生态 tag 挂病态物品会进机器＝双向语义本身（非缺陷），存档不设防。
- **活体替代声明**：port 无 (ingot,Iron) 机器行，RCON foreign 正例臂用 (stick,Blaze) 行（GT6RecipesShCL.java:174-175）×forge 自带 `rods/blaze`（21.1 `c:` 同绿）；iron_ingot＝活体负例；ingots/iron 面由离线 stub 套件覆盖。

## 4. Consequences

- **正面**：第②向闭合（ADR-P24 §6 欠账清偿）；此后任何 RM 配方行自动获得跨 mod 输入互通（零装载侧改动）；方向铁律＋谓词缝成为后续配方域卡的模板件。
- **验收基线**：`GT6RecipeTagFallbackTest` 三臂 11 法（正例/负例/回归＋seam 红绿两向＋方向双向）；cleanTest forge 1454/0/0/0＋neo 1456/0/0/2skip；RCON 双腿链 p25_tag_input_machine_fallback.py [0,0]（/tmp/gt6_p25taginput_forge.log、/tmp/gt6_p25taginput_neo.log）；runData 双腿二跑 written:0（itemTagFamily 上提零产出漂移）；冻结面恰 4 文件（Recipe/GT6ItemTags/新测试/新链）。
- **义务**：crafting 侧写作纪律＝新配方材质输入一律 TagKey（`materialTag`/`gt6`/平台既有），进卡模板（随配方卡渐进，不单独立卡）；触碰匹配面的卡必须带方向臂双向测试。
- **边界/遗留**：§2.6 池冻结清单；（ingot,Iron) 真实机器行待 prefix 滚动卡自然补入。
