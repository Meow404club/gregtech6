# ADR-P24 草稿：datagen TagsProvider 立项与「移植内容×原版功能 tag」双向范式

日期：2026-09-08 ｜ 前置：用户裁定 2026-09-07（tag 双向+澄清，state todo.current user_rulings_in_force 倒数第二、三条） ｜ 评审基线 main=9938a4d4（P24 实现面收官） ｜ 决策卡 state decisions.p24-first-tag-batch-freeze／decisions.p24-tool-system-tag-strategy／decisions.p24-material-name-normalization／research.p24-r-tags-foundation／decisions.p24-grass-behavior-trim／decisions.p24-recipe-seam-contract ｜ 实现卡 work/p24-tool-system（a95d9573）→ work/p24-tags-provider-skeleton（264169e2）→ work/p24-grass-block（a6a1a7f7）→ work/p24-tags-prefix-materials（f476ea90）

- 状态：Accepted（P24 收官主会话正典化 2026-09-08）

## 0. 摘要

用户裁定：移植内容须携带原版功能 tag 且双向——挂 tag 供他 mod 配方匹配、配方用 tag 纳他 mod 物品。据此立项 datagen TagsProvider 家族（GT6BlockTags/GT6ItemTags），四卡 takeover 共写不另起平行；零 optional 严格 throw 即验收资产；材料名归一用最小实证清单（aluminium 双挂）；快照 PIN 有意识更新义务。范式首例=草卡（动物 spawnable 有意识不挂=canCreatureSpawn=F 的 tag 等价面）。

## 1. 用户裁定（决策源头）

1. 【2026-09-07 用户裁定】移植内容必须携带原版对应功能 tag（草/树叶/原木/石/泥土等功能族）——GT 草卡立范式（先 census 原版哪些行为面是 tag 驱动 vs 身份硬编码，tag 形的加入）；推广=移植内容×原版功能 tag 覆盖审计。立项时本仓 datagen 无 TagsProvider，3773 前缀方块+机器+线缆均不在 mineable/* 等任何功能 tag（GTStoneBlock 自带 workaround 孤例；#minecraft:logs listener 是既有消费面先例）。
2. 【2026-09-07 用户澄清】『无世界生成』仅指 GT 草方块卡本身（非全局移植惯例）；『携带 tag』范围=全部移植内容且**双向**——①移植物品挂 tag（原版功能 tag+forge 生态 tag），使他 mod 用 tag 配方时能匹配我们的物品；②移植配方输入用 tag 匹配，使他 mod 同功能物品能用于我们的配方。

## 2. Provider 家族形态

### 2.1 双腿分叉面=import-only（+ItemTagsProvider ctor 真分叉）

- API 讹误修正（research 卡→冻结卡）：双腿 BlockTagsProvider ctor **同为 4 参** (output, lookupProvider, MOD_ID, existingFileHelper)——forge refs `tmp/refs/forge-api/forge-1.20.1/.../BlockTagsProvider.java:19` vs neo 21.1 `tmp/harvest/neoforge-api-1211/.../BlockTagsProvider.java:16-20` 全源；研究卡『Neo 3 参』作废，双腿分叉=**纯 import 行**（GT6Atlases.java:46-51 同构先例）。
- 真 ctor 分叉仅在 ItemTagsProvider：forge 腿 vanilla 3 参（vanilla ItemTagsProvider.java:19）、neo 21.1 腿 patch 5 参（`tmp/harvest/neoforge-api-1211/patches/.../ItemTagsProvider.java.patch:32-41`；官方用法 RemoveTagDatagenTest.java:40/:58）。
- 文档正本=`tmp/refs/neoforge-docs/neoforge/versioned_docs/version-1.21.1/resources/server/tags.md:161-166/:252-256`（curator 裁决引 refs 正本，重复 harvest 已 exclude）；26.1 同页 ctor 去 EFH=『分叉仅 import+ctor』纪律的未来佐证。

### 2.2 takeover 扩展链（四卡共写，禁平行 provider）

decisions.p24-recipe-seam-contract 冻结合同：**绝不自建平行 provider**，GT6DataGenerators 各卡一行 addProvider 追加（:51-53 server 区），lang 缝 tail-append（P23 教训）。链序：

1. tool 卡落 GT6ItemTags 最小版（#gt6:tools/file|saw+#gt6:redstone+#gt6:plate_curved_tin+生态 tools 五件）；
2. skeleton 卡（264169e2）新建 GT6BlockTags+扩展 ItemTags 首批：mineable/pickaxe←stone272+机器+材料金属/宝石/原矿、mineable/axe←木桶、物品侧 forge:ingots|dusts|gems|storage_blocks|nuggets/<mat>（forge 腿 data/forge 与 neo 腿 data/c 各 4005 文件，产出路径随 tag location 自动分腿）；
3. grass 卡（a6a1a7f7）追加草族 section（dirt/mineable-shovel/sniffer_diggable/items-dirt）；
4. prefix 卡（f476ea90）滚动批 1816 文件：blockDust shovel 新带 1096+线缆 629/管线 2/桶族 15+物品 plates/rods/hot_ingots 三族+ECOSYSTEM_ALIASES 别名孪生双挂。

生态 namespace 实证：forge 1.20.1 腿 #forge:（Tags.java:405，TOOLS 无 saw/file 细分=自建正当）、neo 21.1 腿 #c:（Tags.java:799，tag() namespace "c" :923/:280）。

### 2.3 严格性=验收资产

禁 addOptional/addOptionalTag：`tmp/vanilla-1.20.1/net/minecraft/data/tags/TagsProvider.java:86-94` 缺失引用直接 throw（抽验证实）——成员全仓内已注册，静默缺失=审计噪声被结构性排除；GT6TagsDatagenTest 快照 7 钉（总量/values 全原语/物品侧 EXACT/namespace 分腿）。

## 3. 双向范式实例

- **挂（原版功能 tag）**：pickaxe/axe/dirt/shovel 等功能族全覆盖（§2.2 链）。
- **挂（生态 tag）**：物品双向挂 #forge:（1.20.1）/#c:（21.1）。
- **用（配方输入 tag 匹配）**：空罐配方四 key 全 TagKey ingredient 形态无裸 item（pattern ["Rf","Cs"]，上游 MultiItemRandomTools.java:240）；草卡染料输入 Tags.Items.DYES_<COLOR> 双腿同文常量（forge Tags.java:225-227/neo Tags.java:520-522）。
- **有意识缺席也是范式面（范式首例=草卡）**：上游 canCreatureSpawn=F（BlockGrass.java:107）用 tag 缺席实现动物系——animals/wolves/foxes/rabbits/parrots/frogs_spawnable_on 六 tag 全不挂，1.20.1 动物刷怪面是 tag 驱动（Animal.java:109 消费），缺席即禁止=语义等价；单测断言六非成员钉死（grass ACCEPTANCE 5，canCreatureSpawn=F 等价面）；javadoc+decision 双落（decisions.p24-grass-behavior-trim）。同卡 valid_spawn 不挂（无世界生成永不触达，白挂引审计噪声）。

## 4. 材料名归一：最小实证清单法（aluminium 双挂案）

decisions.p24-material-name-normalization：材料生态 tag 主名=GT mNameInternal snake_case（与物品 id 同一组合规则；GTCEu Modern 先例=全库 grep "aluminum" 零命中，即 GTCEu 不做归一别名）。生态别名差异用**同 namespace 别名 tag 直接元素双挂**——主名 tag 与别名 tag 各自持有同一 gt6 物品成员（与 storage_blocks 多前缀并集同构，零对象形态，单测原语断言不变）。

实证清单（双挂仅两项）：Aluminium→aluminium+aluminum、AluminiumBrass→aluminium_brass+aluminum_brass。证据三链：①GTCEu 源树零 aluminum=生态无正典；②双腿平台 Tags 常量区 grep aluminum/aluminium 双零命中=平台不维护（c:ingots 官方仅 iron/gold/copper/netherite）；③本仓 OreDictMaterial.put alias 槽位（addIdenticalNames :253-264，ID=-1）被归并不产第二物品 id/第二 tag——Co60/Au198/Astatine209 等 String 别名=oredict 同义词仅主名；ANY.* 组材料不产物品不进 tag。

## 5. 快照 PIN 有意识更新义务

GT6TagsDatagenTest JSON 快照 PIN 实现**三方归一：provider 生成=PIN 常量=产物 JSON 顶层**。PIN 演化链 2974→2975→2976→3620→3621→3622（prefix 卡 3620，rebase 归一 3621=272+25+1(lightning)+2677+629+2+15，pickaxe.json 程序化并集 2975∪3620=3621；ACT 卡 +1=3622=272+26+1+2677+629+2+15）。每次追加卡 rebase 时 PIN 行手工归一是**义务而非意外**：PIN 漂移者必须证明并集/归一正确（重跑 runData 二跑 written:0+三方对账），不得掩盖或改弱断言。

## 6. 后果

- 正面：3773 前缀方块+机器+线缆从「不在任何功能 tag」变为功能族全覆盖；他 mod tag 配方可匹配 GT 物品、GT 配方可消费他 mod 同功能物品（双向兑现）；审计面结构化（快照测试+tree check tags 带 byte 1:1）。
- 义务：未来每张移植卡自带 tag 面（provider section 追加+PIN 三方归一）；材料别名双挂需维护最小实证清单；生态 namespace 分腿随版本演进（26.1+ ctor 去 EFH 将来跟进）。
- 边界/遗留：requires_correct_tool_for_drops+needs_* gate 未入（GT6LootTables:669-685 chisel match_tool 链+harvestLevel 耦合，挂工具系统后续卡）；valid_spawn 1.20.1 运行时消费点未定位故不挂；配方输入 tag 匹配的新映射层（材料栈→tag）归配方系统研究卡。
