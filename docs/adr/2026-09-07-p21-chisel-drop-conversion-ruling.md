# ADR 草稿 2026-09-07 — P21 凿石挖掘掉落转换（chisel 挖石 → 变体 item）落点与范围裁决

状态：Accepted（P21 收官主会话正典化 2026-09-07）

## 背景

上游 GT_Tool_Chisel.java:57-79 `convertBlockDrops`（1.7.10 Forge HarvestDropsEvent 层）在凿子挖掉方块时清空掉落列表并重填：
- 臂① :58-62 `Blocks.stone` → chiseled stone brick（1.7.10 stonebrick meta 3）
- 臂② :63-72 `Blocks.stonebrick` meta0→cracked / meta1(mossy brick)→mossy cobble / meta2(cracked)→cobble / 其余→self
- 臂③ :73-77 `BlockStones` → `ST.make(aBlock, 1, CHISEL_MAPPINGS[meta & 15])`（BlockStones.java:81 byte[16]，石盲）

触发链：GT_Proxy 采掘事件 → MultiItemTool.onHarvestBlockEvent（MultiItemTool.java:203-213），守卫 = getDigSpeed>0（即 isMinableBlock 域，GT_Tool_Chisel.java:82-84）。返回值在 MultiItemTool.java:212 × getToolDamagePerDropConversion(=100, :46) 付耐久；三臂全 return 0 → **掉落转换零额外耐久**。:75 无视 aSilkTouch/aFortune（总是转换）。

与已合入机制的区别（不许混）：GT6RecipesStoneChisel + GTChiselItem.stoneToolClick = **右键世界内 setBlock 换形态**（ToolCompat.java:224-229 门）；本卡 = **挖掘掉落侧只换 item、不出方块**。上游也是两套机制。

与 loot 层基线（已落地：variant0→同石 COBBL 直译 BlockStones.java:731，余 271 dropSelf）逐 variant 比对 CHISEL_MAPPINGS：
- identity 6 个（1,2,6,8,9,11）：凿挖与普通掉落同 item，loot 层已覆盖，无增量。
- 非 identity 10 个（0→7, 3→4, 4→1, 5→2, 7→6, 10→11, 12→11, 13→14, 14→13, 15→11）：tool 层独有增量，×17 石 = **170 表需分支、102 表 pass-through**。

## 裁决一：现代落点 = loot datagen（alternatives + match_tool），否决 block override 与 GLM

**选**：GT6LootTables.GT6StoneBlockLoot 逐表生成 `alternatives[ match_tool(gt6:chisel) → CHISEL_MAPPINGS 变体 item , 无条件 → 现基线掉落 ]`。理由：
1. 保真：转换语义落在掉落通道本身；爆炸/漏斗等无 TOOL 参数场景 match_tool=false 落 else 臂 = 上游无 HarvestDropsEvent 的同语义；silk/fortune 本仓 GT 石表无臂（p21 审查确认），上游 :75 无视语义自动成立。
2. 双腿对称：1.20.1 已在盘钉证 MatchTool（tmp/vanilla-1.20.1/.../loot/predicates/MatchTool.java，LootItemConditions.java:22 注册 "match_tool"）+ AlternativesEntry（.../entries/AlternativesEntry.java:11），且 vanilla BlockLootSubProvider.java:77 自身即在 block loot datagen 中用 MatchTool.toolMatches（HAS_SILK_TOUCH）= 同姿势先例；1.21.1 无 vendored ref，不预写结论，以双腿 runData 二跑 written:0 + 生成树 byte-diff 硬门实证（JSON 名 minecraft:match_tool / minecraft:alternatives 跨 1.20/1.21 稳定，如分叉按腿 split，先例 GTChiselItem.stackEquals）。
3. 最小面：GTStoneBlock 与 GTChiselItem **零改动**——刚退化的纯块（p21 0e3f8524）不被重新污染，不引入 block→tool 反向耦合。

**否决 Block.getDrops / playerDestroy override**：per-pair 272 实例共类，加 chisel 逻辑 = 纯块退化成果回退 + block 认识 tool 的反向耦合；且与"掉落语义在 loot 表"的已落地格局（GT6StoneBlockLoot）分裂。

**否决 Global Loot Modifier**：为 272 张自有表上全局机制过度工程，且双腿 serializer 注册 API 分叉（Forge 显式 registry vs NeoForge codec）；留作 vanilla 臂若被拉起时的研究卡。

**可达性（关键前提，已证）**：GTStoneBlock 的 Properties 未调 requiresCorrectToolForDrops（GTStoneBlock.java:56-59）→ 任意工具含凿子破坏都有掉落，match_tool 即达。上游 getDigSpeed>0 守卫映射为"凿子实际破坏了方块"。上游 isMinableBlock 的采掘面（工具类/速度）维持 p16 裁池不动。

**chisel 匹配保真**：match_tool 用 item-id 精确匹配 gt6:chisel（GT6Tools.java:88）。本仓凿子单物品单钢级，上游多材质凿石族不复存在——声明偏离，随工具族池。

## 裁决二：v1 范围 = 仅 BlockStones 272 族；vanilla stone/stonebrick 两臂入池

臂①②（GT_Tool_Chisel.java:58-72）需要动 vanilla 自有 loot 表（minecraft:blocks/stone 等含 silk 臂，override 即长期维护负担）或 GLM。合成侧 vanilla pair 已落（GT6RecipesStoneChisel.vanillaTable，Loader_Recipes_Vanilla.java:772-773），矿侧转换随 GLM 研究卡再议。GT6RecipesStoneChisel.SKIPPED_UPSTREAM :251 行改写为："vanilla 臂仍在池；BlockStones 臂由本卡经 loot match_tool 落地"。

## 落卡与验收

见 state `tasks.p21-arch-chisel-drops`：census 单测（170/102 钉数 + per-variant 期望表）、双腿 runData 分跑（id327 铁律）、RCON 链正臂（fake player 持 gt6:chisel 挖 marble BRICK→断言 CRACK 变体 item；GT6ChiselCommand.java:71 持凿先例）+ 负臂（空手/非凿→loot 基线）。RCON fake-player 挖掘臂现不存在（command 包无 destroyBlock），需扩 GT6ChiselCommand 或新增 debug 命令——P19 越卡偏离 ACCEPT 先例，卡内声明。
