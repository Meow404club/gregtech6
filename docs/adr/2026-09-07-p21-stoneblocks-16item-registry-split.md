# ADR 草稿：P21 stoneblocks 16-item registry 拆分（凿石 loot 池项落地形态）

日期：2026-09-07 ｜ 状态：Accepted（P21 收官主会话正典化 2026-09-07）｜ 基线：main 53ba0f31
卡：p21-stoneblocks-16item-registry-split ｜ 池项出处：GT6LootTables.java:448-450（P19 收官时声明入池）

## 背景与证据

P19 stoneblocks 落地为「17 石 × 1 Block（StoneVariant EnumProperty 16 态）× 1 BlockItem」
（GTStoneBlocks.java:58-175）。loot 因此聚合：一石一表覆盖 16 态、全体 dropSelf，并在
GT6LootTables.java:439-456 声明偏离——上游 BlockStones.getDrops（BlockStones.java:731）
meta0 STONE → COBBL、其余 15 meta 自掉；单 item id 形态下两结果坍缩为同一 ItemStack，
「stone-yields-cobble 不可还原，除非每石拆 16 item（registry 形态变更）」。

上游变体 item 的消费面（拆分后解锁）：
- 凿石掉落转换 GT_Tool_Chisel.java:73-77：BlockStones → `ST.make(aBlock, 1, CHISEL_MAPPINGS[meta])`
  ——产出**变体 item**；现形态无变体 id，无法直译（GT6RecipesStoneChisel.java:245 既定池项）。
- JEI/创造域每变体 item 存在性。

## 裁决

1. **粒度 = 272 个 (stone,variant) 逐对 Block+BlockItem**（P8 ADR ④ per-pair 先例，
   GTMaterialBlocks/GTMaterialPrefixBlock）。否决继续扩 property 面（IntegerProperty/单块
   多 variant 的 blockstate 膨胀教训，ADR-P8 ③）。
2. **id 方案**：variant 0（STONE）保留现 id `gt6:<snake>`（存量引用与已生成 JSON 键面最小
   diff）；其余 15 变体 `gt6:<snake>_<variant 段>`，段名与现 item model path 段核对一致。
3. **loot 粒度 = 逐方块独立表 272 张**（vanilla `gt6:blocks/<path>` 默认位，Block.getLootTable
   零块代码，GT6LootTables.java:23-28 既定约定）。variant0 表 = 同石 COBBL variant item 单池
   （BlockStones.java:731 直接译）；其余 271 张 dropSelf。GT6StoneBlockLoot 保持单一
   sub-provider、getKnownBlocks 窄化为 272。
4. **名称/lang 面零新增键**：每块固名后 getName 仍组合 `gt6.stone.variant.*` + `gt6.material.*`
   小单元（P20 B2 组合式模板原样），GT6LangParityTest 预期零改动全绿。
5. **四映射表**（CHISEL/FILE/HAMMER/MOSS，GTStoneBlocks.java:101-126）键面由 meta 字节改
   (stone,variant) 索引，映射语义逐字不变（CHISEL_MAPPINGS[6]==[7]==CHISL 自映射钉保留）。
6. **本卡边界**：只做注册形态 + datagen（blockstate/item model/loot）+ 行为面等价切换；
   凿石采掘掉落转换的 1.20.1 直译仍归其既定池项（GT6RecipesStoneChisel.java:245），本卡
   仅声明其解锁依赖；创造 tab 归属维持 GTStoneBlocks.java:54-56「无 tab」现状，tab 裁决留池。

## 声明偏离（延续 P19 台账）

- 旧世界已放置石块：blockstate property 消失 = 放置态迁移损失。项目 pre-release 期接受，
  本 ADR 记录即声明。
- JSON 面从 17×3 增至 272×3（~816 文件），量级在 P3（56k 物品）/P8（3773 loot 表）先例内。

## 验收锚

注册 census 钉 272+272；runData 首跑写全量、二跑 written:0；loot 形态测试（variant0 引同石
cobble item、其余 dropSelf）；RCON/gametest 行为断言（setblock→destroy→变体 item）；全测绿
+ runServer 零 ERROR；GT6LangParityTest 零改动全绿。
