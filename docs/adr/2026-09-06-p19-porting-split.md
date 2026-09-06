# ADR 草稿 2026-09-06 — P19 移植批拆卡（5 卡 2 波）

状态：正典（P19 收官成文，main 01031e47；decisions key=2026-09-06-p19-porting-split）
基线：main b19a02db（root 205+mdk 1081=1286 全绿；1.21.1 IDENTICAL-0）
输入：tasks.p19-research-{stonechiseled,circuit-interactions,drying-rows}（三卡全回）
总纲：state key=tasks.p19-arch-porting-split；KG：PORT_phase19 -SPLIT_INTO-> 5 卡 + 2 条 DEPENDS_ON

## 卡清单与波次

| slug | 波 | 一句话 | 合并序 |
|---|---|---|---|
| p19-stoneblocks-registry | W1 | GTStoneBlocks 独立注册面：17 石×16 变体 EnumProperty+四映射表+EnUs，无 datagen 中间态 | W1 第 1 |
| p19-drying-rows-backfill-2 | W1 | 矿物 8+Clay 2 行 pour+clay→terracotta+seawater/waterdirty SIMPLE_LIQUID_SPECS+2 行+死行全集审计 | W1 第 2（GT6EnUs rebase） |
| p19-distillery-front-canonical | W1 | colored+overlay bake 三张 front 正典化（声明派生+脚本+sha256） | W1 第 3（零共享面） |
| p19-stoneblocks-render | W2 | addStoneBlocks 272 变体 JSON+272 PNG census 借入+loot；DEPENDS_ON registry | W2 第 1 |
| p19-chisel-recipes | W2 | CHISEL 图（RM.java:138）+三源 pour+GTChiselItem 右键通用门；DEPENDS_ON registry | W2 第 2 |

并行度峰值 3（≤4）。共享 append-only 面：GT6EnUs（registry+drying 两卡，按上序）；其余文件面两两互斥：
GT6RecipesDrying/GTFluids=drying 独占；GT6BlockStates/assets=render 独占；GT6RecipeMaps/GTChiselItem=chisel 独占。
在途卡零交叠已核（waitdone-8kb=framework 运行器域、curator=harvest 域；rcon chains 只新增 p19_*.py）。

## 裁决（对研究员建议的独立复核）

- R1 落点：背书独立 GTStoneBlocks，否决 GTMaterialBlocks 扩展。证据：GTMaterialBlocks.java:96-100
  七前缀编译锚定 + :116-130 prefix×material 全材质 enumerate；stoneChiseled=CS.java:1668 手挑 17 石，
  扩展需发明黑名单钩。承载=每石 1 Block+EnumProperty（P6 先例），否决 272 独立块（P8 反膨胀）。
- R2 ToolCompat 门：右键通用门（ToolCompat.java:224-229 findRecipe→blockINblockOUT→setBlock→付 10000）
  本波随 chisel-recipes 落——配方面无此门是死数据；挖掘掉落转换（GT_Tool_Chisel.java:57-79）裁池
  （1.20.1 无 HarvestDropsEvent，需 loot 拦截框架，回池=挖掘框架泛化卡）。
- R3 distillery：背书 bake 立即正典化，否决等渲染池。overlay 背景透明=必须 colored 叠底合成不透明
  16x16（声明派生+可复跑脚本）；零代码改动；渲染池忠实分层是未来升级非前置。等池=留灰度占位且无排期。
- R4 盐流体：注册进 GTFluids 但用新 SIMPLE_LIQUID_SPECS 独立表（复用 AquaFluidSpec record），否决
  AQUA_SPECS 追加——FL.java:125/:127 仅 SIMPLE+LIQUID 无 FOOD/WATER/BATH，aqua 家族语义被 GT6EnUs.java:111-119
  walk+GTFluidsAquaFamilyTest:38-39 精确断言钉死。water_hot 维持不注册（IC2 别名 parity）。
- R5 circuit 配方面否决：row0 'h'/'w'、config1-24 'd' 全是 GT 工具网格键（ItemIntegratedCircuit.java:58/:61-85），
  本仓无 GT 锤/扳手/螺丝刀物品且全仓无 vanilla crafting RecipeProvider（sym_query 零命中）；削键=无先例的
  玩法偏离。整面裁池，回池=工具物品+首个 crafting 桥卡（含工具键耐久消耗）。P19 circuit 域只留
  distillery-front 卡。

## 门禁

- runData 必：drying（GT6EnUs）、render（272+ JSON+loot，1.21.1 腿同跑）。免：registry（P8 ADR④ 中间态）、
  chisel（零 provider）、front（资源路径不变零 JSON）。
- RCON 新臂必 2：p19_chisel.py（place→click→变体转换+耐久扣）、p19_drying.py（seawater 行过机实弹）；
  其余三卡免。两链均为新文件，不碰已有 27 链。
- 全卡 1.21.1 compileJava+test 0 红（IDENTICAL-0）；useOn 若需 //? 分叉按 ADR-P15-3。
- 红线遵守：GT6Mod/GTModBusListener 冻结（ADR-P3-4），注册自持 @EventBusSubscriber。

## 裁池（回池条件）

1. circuit crafting 面（row0+:59 重置+config1-24）→ 工具物品+crafting 桥卡立项
2. chisel convertBlockDrops :57-79 → 挖掘框架泛化卡
3. CR.shaped 手工行 RM:471/:511-512/:517-518/BlockStones:419-424 → 同 1
4. BlockStones.run Hammer/Crusher/Shredder 机器行 :398-399/:406-408/:414-416 → 石材机器行池
5. RecipeMapChisel.findRecipe oredict 环合成器（RecipeMapChisel.java:47-64）→ OM 运行时面卡；
   CHISEL 图用基类+声明偏离
6. WitherProof/slab/dungeon worldgen/Chisel-mod Carving 桥/TE Mana Bath/LaserEngraver/Pocket Chisel/
   cover 三处/Mold/Basin/ButtonAdvanced/RailRoad → 研究卡裁池维持
7. circuit cover 三配方/ACT slot30/CoverSelectorTag/OD_CIRCUITS/damage>24/手册页/256 icon 梯 → 维持
   （icon 梯回池=config1-24 可获得性）
8. drying 三大主体（生物/树脂/食物族+CFoam/混凝土/染料 32 行+Ar+sluice）→ 缺物品/流体族，独立立项
9. water_hot 注册 → 维持 absent-fluid skip（无-IC2 parity）
