#!/usr/bin/env python3
"""Generate the zh_cn reference TSV for the GT6ZhCn datagen provider (task p20-i18n-zhcn-provider).

Reads the GT6 1.7.10 zh lang dump (tmp/gregtech.lang, the LanguageHandler dump — the sole
residual value of the cut GT_LangManager port, research card tasks.p20-research-i18n-zh) and
writes the translation DATA SOURCE the provider reads from the classpath at datagen time:
mdk/src/main/resources/gregtech6/lang/zh_cn_ref.tsv (columns kind/source/value/status, ADR
2026-09-06-p20-i18n-zhcn-pipeline §1.1).

Pipeline ruling implemented here (ADR §1.1/§1.2):
  - The dump lines look like "    S:<key>=<value>" (Forge Configuration render). Only three
    key families are collected: gt.material.* / itemGroup.* / gt.multitileentity.*; addon and
    out-of-face keys (ktfru.* = KekzTech addon, written.book.*, enchantment.*) are dropped.
  - ZERO key-name logic on the Python side: family rows carry the dump key suffix verbatim
    (material Pascal internal / itemGroup camel internal / mte numeric id); the target gt6
    lang keys are derived provider-side by the single MaterialPrefixItem.snakeCase
    implementation (or joined by walk source), so the snakeCase algorithm never lives in two
    languages. kind=direct rows are the exception — their source column IS the final gt6 key,
    hand-entered below (ADR §1.2: the hand-translation layer).
  - A family row whose value is pure ASCII is marked status=review (suspected untranslated,
    evidence sample "gt.material.Magnite=Magnite", tmp/gregtech.lang:5003); the provider skips
    review rows and the runtime falls back to English per key (vanilla LanguageManager.java:
    50-52 bilingual chain, zero port code).
  - The mte family (gt.multitileentity.<ID>, ~4.4k rows) is collected as inert census data:
    the provider has no ID->port-key join yet (that is the follow-up MTE subface, arch card
    tasks.p20-arch-i18n — needs the upstream Loader_MultiTileEntities ID table as evidence).
  - Hand rows (HAND_TRANSLATIONS below): the ~103 in-use prefix display templates (one per
    creative-visible prefix tab, the arch card's "手译在用 105 条" ruling — templates only,
    never machine-translated values; each row was authored against the dump's oredict
    composed-name evidence, e.g. oredict.ingotAbyssalnite=深渊锭 -> "%s锭") plus the handful
    of special creative tabs / atomic misc keys. kind=direct, status=hand.

Idempotent: same dump + same hand table -> byte-identical TSV (rows sorted by (kind, source),
first dump occurrence wins on duplicate keys, LF line endings, trailing newline).

Usage:
  python3 gen_zhcn_ref.py --dump /path/to/gregtech.lang \
      [--out ../mdk/src/main/resources/gregtech6/lang/zh_cn_ref.tsv]
The dump path is a required argument (the coder worktree does not carry tmp/, ADR §1.1 CI
principle — the TSV is the committed, reviewable distillate of the dump).
"""
import argparse
import re
import sys
from pathlib import Path

DUMP_LINE = re.compile(r"^    S:([^=]+)=(.*)$")

# Out-of-face keys: the KekzTech addon family and non-item lang keys (ADR §1.1 filter).
DROP_KEY_PREFIXES = ("ktfru.", "written.book.", "enchantment.")

# kind -> dump key prefix (the three collected families; everything else is ignored).
FAMILIES = (
    ("gt.material.", "material"),
    ("itemGroup.", "itemgroup"),
    ("gt.multitileentity.", "mte"),
)

REVIEW_NOTE = "value is pure ASCII — suspected untranslated dump entry (e.g. Magnite=Magnite, tmp/gregtech.lang:5003)"

# ---------------------------------------------------------------------------
# Hand-translation layer (kind=direct rows). Key = the FINAL gt6 lang key, value =
# (zh value, status). These are human-authored rows: the ~103 in-use prefix display
# templates (one per creative-visible prefix tab of the port — GTMaterialItems.tabPrefixes()
# + GTMaterialBlocks.tabPrefixes() face, the arch card's "手译在用 105 条" ruling) and the
# special creative tabs + atomic misc keys. Every template was authored against the dump's
# oredict composed-name evidence (oredict.<prefix><Material>=<zh组合>, e.g.
# oredict.ingotAbyssalnite=深渊锭 -> "%s锭"; gem mirrors the bare en template "%s").
# Values contain exactly one "%s" slot where the runtime fills the material name
# (MaterialPrefixItem.getName template contract). NEVER machine-translate here: a bad row is
# skipped by fixing its status to "review", not by generating a guess.
# ---------------------------------------------------------------------------
HAND_TRANSLATIONS = {
    # ---- in-use prefix display templates (gt6.tagprefix.<snake>, status=hand) ----
    "gt6.tagprefix.ingot": ("%s锭", "hand"),
    "gt6.tagprefix.ingot_double": ("双重%s锭", "hand"),
    "gt6.tagprefix.ingot_triple": ("三重%s锭", "hand"),
    "gt6.tagprefix.ingot_quadruple": ("四重%s锭", "hand"),
    "gt6.tagprefix.ingot_quintuple": ("五重%s锭", "hand"),
    "gt6.tagprefix.dust": ("%s粉", "hand"),
    "gt6.tagprefix.dust_small": ("小堆%s粉", "hand"),
    "gt6.tagprefix.dust_tiny": ("小撮%s粉", "hand"),
    "gt6.tagprefix.dust_div72": ("1/72%s粉", "hand"),
    "gt6.tagprefix.plate": ("%s板", "hand"),
    "gt6.tagprefix.plate_tiny": ("小块%s板", "hand"),
    "gt6.tagprefix.plate_double": ("双重%s板", "hand"),
    "gt6.tagprefix.plate_triple": ("三重%s板", "hand"),
    "gt6.tagprefix.plate_quadruple": ("四重%s板", "hand"),
    "gt6.tagprefix.plate_quintuple": ("五重%s板", "hand"),
    "gt6.tagprefix.plate_dense": ("致密%s板", "hand"),
    "gt6.tagprefix.plate_curved": ("弯曲%s板", "hand"),
    "gt6.tagprefix.plate_gem": ("结晶%s板", "hand"),
    "gt6.tagprefix.plate_gem_tiny": ("小块结晶%s板", "hand"),
    "gt6.tagprefix.gem": ("%s", "hand"),
    "gt6.tagprefix.gem_chipped": ("碎裂%s", "hand"),
    "gt6.tagprefix.gem_flawed": ("瑕疵%s", "hand"),
    "gt6.tagprefix.gem_exquisite": ("精美%s", "hand"),
    "gt6.tagprefix.gem_flawless": ("完美%s", "hand"),
    "gt6.tagprefix.gem_legendary": ("传奇%s", "hand"),
    "gt6.tagprefix.nugget": ("%s粒", "hand"),
    "gt6.tagprefix.block_raw": ("粗%s矿石块", "hand"),
    # task p28-lang-batch1-fix (user ruling 2026-09-12): the gem/crystalline split — gem =
    # 宝石, crystalline = 结晶 (mcmod "Block of Ruby" = 红宝石块 across GT5/GTCE/GT6U/GT6;
    # upstream OP.java:346 blockGem="Block of %s" has NO crystalline word either). The zh
    # face now collides with block_ingot %s块 exactly like the en face does ("Block of %s"
    # both) — disambiguation rides the material word, the community form wins.
    "gt6.tagprefix.block_gem": ("%s块", "hand"),
    "gt6.tagprefix.block_dust": ("%s粉块", "hand"),
    # task p27-lang-fix-batch2 P2 (ledger §3): %s锭块 was clunky — %s块 is the community form
    # (GTCEu "Block of %s"); the block family stays mutually distinct (%s块 / %s板块 / %s粉块 /
    # 实心%s块 — block_gem rejoined %s块 per the p28 gem/crystalline split). block_plate/block_dust
    # keep their faces (the ledger marks them 低优).
    "gt6.tagprefix.block_ingot": ("%s块", "hand"),
    "gt6.tagprefix.block_plate": ("%s板块", "hand"),
    # task p28-lang-batch1-fix: crystalline word moves to the head — en "Crystalline %s Plate
    # Block" (OP.java:197 plateGem) puts the adjective first, 结晶红宝石板块 per mcmod.
    "gt6.tagprefix.block_plate_gem": ("结晶%s板块", "hand"),
    "gt6.tagprefix.block_solid": ("实心%s块", "hand"),  # task p27-lang-fix-batch2 P2: solid≠铸造 (en "Block of solid %s")
    "gt6.tagprefix.ore_raw": ("粗%s矿石", "hand"),
    "gt6.tagprefix.rock_gt": ("含%s岩石", "hand"),  # task p27-lang-fix-batch2 P2 (ledger §3): 石块→岩石 (en "%s bearing Rock")
    "gt6.tagprefix.chunk_gt": ("%s碎块", "hand"),
    "gt6.tagprefix.crushed": ("破碎%s矿", "hand"),
    "gt6.tagprefix.crushed_tiny": ("小撮破碎%s矿", "hand"),
    "gt6.tagprefix.crushed_purified": ("洗净%s矿", "hand"),
    "gt6.tagprefix.crushed_purified_tiny": ("小撮洗净%s矿", "hand"),
    "gt6.tagprefix.crushed_centrifuged": ("离心%s矿", "hand"),
    "gt6.tagprefix.crushed_centrifuged_tiny": ("小撮离心%s矿", "hand"),
    "gt6.tagprefix.billet": ("%s坯料", "hand"),
    "gt6.tagprefix.bolt": ("%s螺栓", "hand"),
    "gt6.tagprefix.round": ("%s垫片", "hand"),
    "gt6.tagprefix.screw": ("%s螺丝", "hand"),
    "gt6.tagprefix.ring": ("%s环", "hand"),
    "gt6.tagprefix.spring": ("%s弹簧", "hand"),
    "gt6.tagprefix.spring_small": ("小%s弹簧", "hand"),
    "gt6.tagprefix.stick": ("%s杆", "hand"),
    "gt6.tagprefix.stick_long": ("长%s杆", "hand"),
    "gt6.tagprefix.gear_gt": ("%s齿轮", "hand"),
    "gt6.tagprefix.gear_gt_small": ("小%s齿轮", "hand"),
    "gt6.tagprefix.rotor": ("%s转子", "hand"),
    "gt6.tagprefix.foil": ("%s箔", "hand"),
    "gt6.tagprefix.lens": ("%s透镜", "hand"),
    "gt6.tagprefix.chain": ("%s锁链", "hand"),
    "gt6.tagprefix.rail_gt": ("%s轨道", "hand"),
    "gt6.tagprefix.minecart_wheels": ("%s车轮", "hand"),
    "gt6.tagprefix.wire_fine": ("细%s导线", "hand"),  # task p27-lang-fix P1 §2.2: wire=导线 (gt6.wire.form.wire zh:2529), 线缆=cable already owns 线缆 — 精细 was unattested
    "gt6.tagprefix.casing_small": ("小%s外壳", "hand"),
    "gt6.tagprefix.chemtube": ("含%s试管", "hand"),
    "gt6.tagprefix.arrow_gt_wood": ("%s箭", "hand"),
    "gt6.tagprefix.arrow_gt_plastic": ("轻质%s箭", "hand"),
    "gt6.tagprefix.bullet_gt_small": ("小号%s子弹", "hand"),
    "gt6.tagprefix.bullet_gt_large": ("大号%s子弹", "hand"),
    "gt6.tagprefix.tool_head_sword": ("%s剑刃", "hand"),
    "gt6.tagprefix.tool_head_pickaxe": ("%s镐头", "hand"),
    "gt6.tagprefix.tool_head_shovel": ("%s锹头", "hand"),
    "gt6.tagprefix.tool_head_spade": ("%s铲头", "hand"),
    "gt6.tagprefix.tool_head_axe": ("%s斧头", "hand"),
    "gt6.tagprefix.tool_head_axe_double": ("%s双刃斧头", "hand"),
    "gt6.tagprefix.tool_head_hoe": ("%s锄头", "hand"),
    "gt6.tagprefix.tool_head_sense": ("%s镰刀刃", "hand"),
    "gt6.tagprefix.tool_head_plow": ("%s犁头", "hand"),
    "gt6.tagprefix.tool_head_saw": ("%s锯片", "hand"),
    "gt6.tagprefix.tool_head_file": ("%s锉刀头", "hand"),
    "gt6.tagprefix.tool_head_hammer": ("%s锤头", "hand"),
    "gt6.tagprefix.tool_head_drill": ("%s钻头", "hand"),
    "gt6.tagprefix.tool_head_chainsaw": ("%s链锯头", "hand"),
    "gt6.tagprefix.tool_head_buzz_saw": ("%s圆锯片", "hand"),
    "gt6.tagprefix.tool_head_wrench": ("%s扳手头", "hand"),
    "gt6.tagprefix.tool_head_screwdriver": ("%s螺丝刀头", "hand"),
    "gt6.tagprefix.tool_head_chisel": ("%s凿子头", "hand"),
    "gt6.tagprefix.tool_head_universal_spade": ("%s万用铲头", "hand"),
    "gt6.tagprefix.tool_head_builderwand": ("%s建筑之杖头", "hand"),
    "gt6.tagprefix.tool_head_construction_pickaxe": ("%s建筑镐头", "hand"),
    "gt6.tagprefix.tool_head_pickaxe_gem": ("%s镶尖镐头", "hand"),  # task p27-lang-fix-batch2 P2 (ledger §3): tipped=镶尖, unified with the tab face
    "gt6.tagprefix.tool_head_arrow": ("%s箭头", "hand"),
    "gt6.tagprefix.tool_head_raw_sword": ("%s剑刃毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_pickaxe": ("%s镐头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_shovel": ("%s锹头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_spade": ("%s铲头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_axe": ("%s斧头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_axe_double": ("%s双刃斧毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_hoe": ("%s锄头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_sense": ("%s镰刀刃毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_plow": ("%s犁头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_saw": ("%s锯片毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_arrow": ("%s箭头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_chisel": ("%s凿子头毛坯", "hand"),
    "gt6.tagprefix.tool_head_raw_universal_spade": ("%s万用铲头毛坯", "hand"),
    # ---- special creative tabs (upstream MTE-registry categories + port tabs, no dump face) ----
    "itemGroup.gt6.chests": ("箱子", "hand"),
    "itemGroup.gt6.machines": ("机器", "hand"),
    "itemGroup.gt6.multiblocks": ("多方块", "hand"),
    "itemGroup.gt6.fluid_containers": ("流体容器", "hand"),
    "itemGroup.gt6.fluid_pipes": ("流体管道", "hand"),
    # task p27-lang-fix-batch2 P2 (ledger §3): the three wire tabs said 电线/红石线/激光导线 —
    # the wire noun unifies on 导线 (the gt6.wire.form.wire face); laser_wires already carried it.
    "itemGroup.gt6.electric_wires": ("导线", "hand"),
    "itemGroup.gt6.redstone_wires": ("红石导线", "hand"),
    "itemGroup.gt6.laser_wires": ("激光导线", "hand"),
    "itemGroup.gt6.tools": ("工具", "hand"),
    # ---- atomic misc keys (tools / covers / circuits / JEI info / example / fluids) ----
    "item.gt6.crowbar": ("撬棍", "hand"),
    "item.gt6.cutter": ("线缆剪", "hand"),
    "item.gt6.chisel": ("凿子", "hand"),
    "item.gt6.cover_redstone_emitter": ("红石发射器", "hand"),
    "item.gt6.cover_redstone_conductor_in": ("红石导线面板（接收）", "hand"),
    "item.gt6.cover_redstone_conductor_out": ("红石导线面板（发出）", "hand"),
    "item.gt6.cover_redstone_machine_switch": ("红石机器开关", "hand"),
    "item.gt6.cover_shutter": ("挡板面板", "hand"),
    "item.gt6.cover_item_filter": ("物品过滤器", "hand"),
    "item.gt6.cover_auto_redstone_machine_switch": ("自动红石机器开关", "hand"),
    "item.gt6.cover_controller": ("面板控制器", "hand"),
    "item.gt6.integrated_circuit": ("选择器标签", "hand"),
    "item.gt6.integrated_circuit.configuration": ("配置：%s", "hand"),
    # ---- B1 wire-domain composed-display units (task p20-i18n-compose-wires) ----
    # The wire family composes at runtime over gt6.wire.display[.plain] + the form units;
    # the material slot rides the gt6.material.* small units (zero new material debt).
    # Wording evidence:
    #   wire/cable = GTCEu Modern zh_cn.json (tmp/refs/gtceu-modern assets/gtceu/lang,
    #   the community-standard SPLIT: tagprefix.wire_gt_single="1x%s导线" /
    #   cable_gt_single="1x%s线缆") — keeps the two forms distinguishable, unlike the
    #   1.7.10 dump's uniform 线缆 (gt.multitileentity.27000/27006 both end in 线缆);
    #   × = the CJK multiplication sign replacing the en "x" (免空格, CJK has no
    #   inter-word spaces, the size slot glues directly onto the material);
    #   wirelamp = 灯导线 (the Loader:1900 Lumium lamp-wire — no dump face; the 导线
    #   head stays consistent with the wire unit);
    #   conveyor/robot arm = dump-verbatim nouns (gt.multiitem.technological.12040=
    #   "输送机模块 (ULV)" / :12080="机械臂 (ULV)", tmp/gregtech.lang);
    #   the laser atomic key = the dump "光纤" (gt.multitileentity.24900) qualified with the
    #     dropped Laser word (task p27-lang-fix-batch2 P2 — 激光光纤导线).
    #   NOTE (review R2): gt6.material.superconductor is DELIBERATELY absent here —
    #   Superconductor is a tier material (mID -1, MT.java:986) with no dump zh face and
    #   an en proper noun identical in both locales; the composed slot falls back to the
    #   English "Superconductor" per-key at runtime (the declared state). A hand row is
    #   a one-line TSV PR for the translation layer, NOT a datagen concern.
    "gt6.wire.display": ("%s×%s%s", "hand"),
    "gt6.wire.display.plain": ("%s%s", "hand"),
    "gt6.wire.form.wire": ("导线", "hand"),
    "gt6.wire.form.cable": ("线缆", "hand"),
    "gt6.wire.form.wirelamp": ("灯导线", "hand"),
    # task p27-lang-fix-batch2 P2 (ledger §3): the en "Compact" faces — the dump's tier-less
    # 输送机模块/机械臂 words (gt.multiitem.technological.12040/:12080) dropped the Compact
    # Electric qualifier; 小型电动/小型 restore it (declared deviation from the dump rows).
    "gt6.cover.conveyor.display": ("小型电动输送机 (%s)", "hand"),
    "gt6.cover.robot_arm.display": ("小型机械臂 (%s)", "hand"),
    # task p27-lang-fix-batch2 P2 (ledger §3): the dump's bare 光纤 (gt.multitileentity.24900)
    # drops the Laser word of en "Laser Fiber Wire"; 激光光纤导线 keeps all three words and the
    # unified wire=导线 noun.
    "block.gt6.wire_laser": ("激光光纤导线", "hand"),
    "block.gt6.example_chest": ("GT 示例箱子", "hand"),
    # task p28-cfoam-lang-key: the TestMachine framework blocks joined the registry-coverage
    # gate — no getName override anywhere on them, so their Jade line resolves the vanilla
    # block.gt6.test_machine[_idle] keys no provider face ever wrote. Faces follow the
    # TestMachineBlock javadoc's ticking/passive vocabulary (en "Test Machine (Passive)").
    "block.gt6.test_machine": ("测试机器", "hand"),
    "block.gt6.test_machine_idle": ("测试机器 (被动)", "hand"),
    # ---- B2 stone + rows composed-display units (task p20-i18n-compose-rows) ----
    # The stone variants compose over gt6.stone.variant.<snake> (16 templates, the stone
    # name slot = the gt6.material.* small units); the rows families over gt6.row.*.
    # Wording evidence (dump = tmp/gregtech.lang):
    #   stone templates = the gt.stone.andesite.N family split verbatim (:15246-15262):
    #     .1 安山岩圆石 -> "%s圆石", .2 苔藓安山岩圆石 -> "苔藓%s圆石", .3 安山岩砖块,
    #     .4 裂纹..., .5 苔藓...砖块, .6 錾制安山岩, .7 平滑安山岩, .8 钢筋...砖块,
    #     .9 红石化...砖块, .10 安山岩方块, .11 小型...方块, .12 小型...砖块,
    #     .13/.14 安山岩风车纹砖块A/B, .15 安山岩方砖; .0 = the bare material name;
    #   axle = "%s%s轴" (dump gt.multitileentity.24780 "小型砷铜轴" word order, :24800-24803
    #     "小型木制轴" -> Wooden = 木制; the size words 小型/中型/大型/巨型 = :3343/:24781/
    #     :8790/:24783);
    #   steam engine = 蒸汽引擎 (%s) (:11052) / 强化蒸汽引擎 (%s) (:11066);
    #   diesel = %s柴油引擎 (the dump :15048 face 燃油引擎 (砷青铜) reordered to the en
    #     prefix form + the 柴油 material word — task p27-lang-fix-batch2 P2 deviation);
    #   burning box = 燃烧室 (%s, %s) (:10960) / 致密燃烧室 (:11010) / 流化床燃烧室 (:15018)
    #     / 致密流化床燃烧室 (:15031); family words 固体/液体/气体 = :10960/:11091/:11176;
    #   boiler = 蒸汽锅炉箱 (%s) (the dump :11025/:11038 faces + the en "Tank" word —
    #     task p27-lang-fix-batch2 P2 deviation);
    #   dryer = 干燥器 (%s) (:11603 "干燥器 (钢)");
    #   distillery = 蒸馏器 (%s) — NO dump face (the :1398-1401 rows are absent from the
    #     dump); DECLARED DEVIATION: hand translation, the GTCEu Modern community noun;
    #   large boiler = %s锅炉气压计核心 (:11256 "不锈钢锅炉气压计核心");
    #   dense wall = 致密%s壁板 (:11368 "致密不锈钢壁板");
    #   tiers = "%s (%s)" over the machine word + the ordinal unit — NO dump face;
    #     DECLARED DEVIATION: hand translation (等级 = the GT6 tier noun; the bracket
    #     shape follows the dump's half-width parens);
    #   shredder = 粉碎机 — NO dump face (the :1294 key is absent); DECLARED DEVIATION:
    #     hand translation, kept distinct from the dump-verbatim 破碎机 (:20021 Crusher)
    #     and 车床 (:20041 Lathe);
    #   tap/funnel = %s龙头 / %s漏斗 (:13549 陶瓷龙头 / :13544 陶瓷漏斗);
    #   row-material words = the gt.material.<Pascal> dump lines verbatim (Bronze :4384
    #     青铜, Brass :4375 黄铜, ArsenicCopper :4159 砷铜, ArsenicBronze :4158 砷青铜,
    #     Steel :5506 钢, Titanium :5589 钛, TungstenSteel :5725 钨钢, Iridium :4897 铱,
    #     Iritanium :4901 钛铱合金, Trinitanium :5645 特林钛合金, Lead :4950 铅, TinAlloy
    #     :5584 锡合金, Invar :4891 殷钢, IronWood :4908 铁木, FierySteel :4691 炙热钢,
    #     Chromium :4467 铬, Tungsten :5721 钨, Bismuth :4308 铋, Netherite :5104 下界合金,
    #     TantalumHafniumCarbide :5548 碳化钽铪, Ultimet :5736 哈氏合金, TungstenCarbide
    #     :5722 碳化钨, StainlessSteel :5500 不锈钢, Adamantium :3589 艾德曼合金,
    #     Ceramic :4428 陶瓷, Plastic :5245 塑料); the axle Wooden word = the dump AXLE
    #     rows (:24800 小型木制轴), NOT the WoodTreated material face (防腐木);
    #   atomic keys = brick_burning_box 砖块燃烧室 (固体) (:11023), heat_transmitter
    #     传热装置 (task p27-lang-fix P1 §2.2 — the dump :11386 row 热吸收装置 mistranslates
    #     transmit as 吸收; zh follows the en "Heat Transmitter" semantics), gearbox
    #     木制可调变速箱 (:24809), transformer_rotation 木制变速箱 (:24808).
    "gt6.stone.variant.stone": ("%s", "hand"),
    "gt6.stone.variant.cobble": ("%s圆石", "hand"),
    "gt6.stone.variant.cobble_mossy": ("苔藓%s圆石", "hand"),
    "gt6.stone.variant.bricks": ("%s砖块", "hand"),
    "gt6.stone.variant.bricks_cracked": ("裂纹%s砖块", "hand"),
    "gt6.stone.variant.bricks_mossy": ("苔藓%s砖块", "hand"),
    "gt6.stone.variant.bricks_chiseled": ("錾制%s", "hand"),
    "gt6.stone.variant.smooth": ("平滑%s", "hand"),
    "gt6.stone.variant.bricks_reinforced": ("钢筋%s砖块", "hand"),
    # task p27-lang-fix-batch2 P2 (ledger §3): 红石化 (dump :15254 verbatim) reads clunky —
    # 红石浸染 follows the vanilla redstone-dye reading (declared deviation, low-stakes face).
    "gt6.stone.variant.bricks_redstone": ("红石浸染%s砖块", "hand"),
    "gt6.stone.variant.tiles": ("%s方块", "hand"),
    "gt6.stone.variant.small_tiles": ("小型%s方块", "hand"),
    "gt6.stone.variant.small_bricks": ("小型%s砖块", "hand"),
    "gt6.stone.variant.windmill_tiles_a": ("%s风车纹砖块A", "hand"),
    "gt6.stone.variant.windmill_tiles_b": ("%s风车纹砖块B", "hand"),
    "gt6.stone.variant.square_bricks": ("%s方砖", "hand"),
    "gt6.row.axle.display": ("%s%s轴", "hand"),
    "gt6.row.size.small": ("小型", "hand"),
    "gt6.row.size.medium": ("中型", "hand"),
    "gt6.row.size.large": ("大型", "hand"),
    "gt6.row.size.huge": ("巨型", "hand"),
    "gt6.row.steam_engine.display": ("蒸汽引擎 (%s)", "hand"),
    "gt6.row.steam_engine.display.strong": ("强化蒸汽引擎 (%s)", "hand"),
    # task p27-lang-fix-batch2 P2 (ledger §3): en "%s Diesel Engine" prefixes the material;
    # the dump's suffix form (燃油引擎 (砷青铜) :15048) becomes the prefix form and 燃油→柴油
    # (the material face diesel=柴油) — declared deviation from the dump column.
    "gt6.row.diesel.display": ("%s柴油引擎", "hand"),
    "gt6.row.burning_box.display": ("燃烧室 (%s, %s)", "hand"),
    "gt6.row.burning_box.display.dense": ("致密燃烧室 (%s, %s)", "hand"),
    "gt6.row.burning_box.display.fluidbed": ("流化床燃烧室 (%s)", "hand"),
    "gt6.row.burning_box.display.fluidbed_dense": ("致密流化床燃烧室 (%s)", "hand"),
    "gt6.row.burning_box.family.solid": ("固体", "hand"),
    "gt6.row.burning_box.family.liquid": ("液体", "hand"),
    "gt6.row.burning_box.family.gas": ("气体", "hand"),
    # task p27-lang-fix-batch2 P2 (ledger §3): en "Steam Boiler Tank" — the Tank word was
    # dropped (蒸汽锅炉); 箱 restores it (declared deviation from the dump :11025/:11038 faces).
    "gt6.row.boiler.display": ("蒸汽锅炉箱 (%s)", "hand"),
    "gt6.row.boiler.display.strong": ("强化蒸汽锅炉箱 (%s)", "hand"),
    "gt6.row.dryer.display": ("干燥器 (%s)", "hand"),
    "gt6.row.distillery.display": ("蒸馏器 (%s)", "hand"),
    # task p27-oven-heat-t-ladder — the Oven Heat_T family (the :1288-1291 name column
    # "Oven ("+Heat_T local+")"): the machine word = the dump row 烤箱 verbatim (the
    # retired atomic block.gt6.oven backfill row joined the composed face); the
    # steel/invar/titanium/tungsten_carbide mat units already exist in this table.
    "gt6.row.oven.display": ("烤箱 (%s)", "hand"),
    # task p24-canner-machine — the Canner family: the upstream display is the VN voltage
    # ladder (Loader_MultiTileEntities.java:1379-1382 "Canning Machine ("+VN[tier]+")",
    # CS.java:154) — the voltage numerals stay Latin in zh (the wire-domain proper-noun
    # ruling), the machine word is the 罐装机 community form.
    "gt6.row.canner.display": ("罐装机 (%s)", "hand"),
    "gt6.row.mat.lv": ("LV", "hand"),
    "gt6.row.mat.mv": ("MV", "hand"),
    "gt6.row.mat.hv": ("HV", "hand"),
    "gt6.row.mat.ev": ("EV", "hand"),
    "gt6.row.large_boiler.display": ("%s锅炉气压计核心", "hand"),
    "gt6.row.dense_wall.display": ("致密%s壁板", "hand"),
    "gt6.row.machine.display": ("%s (%s)", "hand"),
    "gt6.row.machine.shredder": ("粉碎机", "hand"),
    "gt6.row.machine.crusher": ("破碎机", "hand"),
    "gt6.row.machine.lathe": ("车床", "hand"),
    # task p27-machine-energy-display-fix: the gt6.row.tier.2/3/4 ordinal units are RETIRED —
    # the P7 tier slot rides the Kinetic_T material words (gt6.row.mat.bronze/steel/titanium/
    # tungstensteel, already hand rows below; upstream "Shredder ("+aMat.getLocal()+")"
    # Loader_MultiTileEntities.java:1294-1309, Kinetic_T[1..4] MT.java:3690)
    "gt6.row.tap.display": ("%s龙头", "hand"),
    "gt6.row.funnel.display": ("%s漏斗", "hand"),
    "gt6.row.mat.wood_treated": ("木制", "hand"),
    "gt6.row.mat.bronze": ("青铜", "hand"),
    "gt6.row.mat.brass": ("黄铜", "hand"),
    # task p26-pipe-item — the item pipe family: the six variant templates over the three
    # material words. task p27-lang-fix P1 §2.2 + batch rule B5: the dump's 物流管道 word
    # set (tmp/gregtech.lang 25002..25007 黄铜物流管道 family / 25027..25032 康铜 /
    # 25052..25057 钴黄铜) collides with Logistics (barrel_logistics 物流储罐) — the family
    # word follows the tab face 物品管道 (itemGroup dump :17993) instead, and the dump's
    # 限制 prefix rows (:11852-11857) unify on 限流 (the tagprefix pipe_restrictive lane
    # already says 限流) — both DECLARED DEVIATIONS from the dump-verbatim rows.
    # task p26-storage-hopper-family — the storage hopper family: the two kind templates
    # over the bronze/steel material words (already in the boiler walk).
    # task p28-lang-batch1-fix (user ruling 2026-09-12 = community option A): Funnel keeps
    # 漏斗, Hopper splits to 料斗 / Queue Hopper = 队列料斗 — the TeamNED patterns.yml:876-890
    # convention + the mcmod Funnel entry note ("Hopper 最新汉化已改料斗"). The batch1 漏斗
    # wording had mis-anchored on the FUNNEL dump rows (tmp/gregtech.lang:13544 陶瓷漏斗 /
    # :13546 不锈钢漏斗 are the funnel family, not the hoppers); the dump's own hopper MTE
    # rows (gt.multitileentity.8000-8053) already say 料斗. The queue kind stays a hand-layer
    # word (no dump row — the MTE names never entered the lang dump).
    # tsv-orphan回写 (the item-pipe card's rule): the eu-bridge card added this row to the
    # committed TSV without a HAND_TRANSLATIONS entry — a faithful rerun washed it out
    # (2026-09-09, found by this card's regeneration)
    "block.gt6.fe_battery": ("FE 测试电池", "hand"),  # task p27-lang-fix-batch2 P2 (ledger §3): the missing CJK-Latin space
    # task p28-b-fe-converter-machine: the ULV FE->EU machine + the extractable fixture
    # (the CJK-Latin spacing style of the FE 测试电池 row; the converter is a real
    # machine word 转换机, the source fixture keeps the 测试 prefix)
    "block.gt6.fe_converter": ("FE 转换机", "hand"),
    "block.gt6.fe_source": ("FE 测试源", "hand"),
    "gt6.row.hopper.display": ("%s料斗", "hand"),
    "gt6.row.queue_hopper.display": ("%s队列料斗", "hand"),
    "gt6.row.item_pipe.display.medium": ("%s物品管道", "hand"),
    "gt6.row.item_pipe.display.large": ("大型%s物品管道", "hand"),
    "gt6.row.item_pipe.display.huge": ("巨型%s物品管道", "hand"),
    "gt6.row.item_pipe.display.restrictive_medium": ("限流%s物品管道", "hand"),
    "gt6.row.item_pipe.display.restrictive_large": ("限流大型%s物品管道", "hand"),
    "gt6.row.item_pipe.display.restrictive_huge": ("限流巨型%s物品管道", "hand"),
    "gt6.row.mat.constantan": ("康铜", "hand"),      # dump 25027 康铜物流管道 word root
    "gt6.row.mat.cobalt_brass": ("钴黄铜", "hand"),  # dump 25052 钴黄铜物流管道 word root
    "itemGroup.gt6.item_pipes": ("物品管道", "hand"),  # dump :17993 verbatim
    "gt6.row.mat.arsenic_copper": ("砷铜", "hand"),
    "gt6.row.mat.arsenic_bronze": ("砷青铜", "hand"),
    "gt6.row.mat.steel": ("钢", "hand"),
    "gt6.row.mat.titanium": ("钛", "hand"),
    "gt6.row.mat.tungstensteel": ("钨钢", "hand"),
    "gt6.row.mat.iridium": ("铱", "hand"),
    "gt6.row.mat.titanium_iridium": ("钛铱合金", "hand"),
    "gt6.row.mat.trinitanium": ("特林钛合金", "hand"),
    "gt6.row.mat.lead": ("铅", "hand"),
    "gt6.row.mat.tin_alloy": ("锡合金", "hand"),
    "gt6.row.mat.invar": ("殷钢", "hand"),
    "gt6.row.mat.iron_wood": ("铁木", "hand"),
    "gt6.row.mat.fiery_steel": ("炙热钢", "hand"),
    "gt6.row.mat.chromium": ("铬", "hand"),
    "gt6.row.mat.tungsten": ("钨", "hand"),
    "gt6.row.mat.bismuth": ("铋", "hand"),
    "gt6.row.mat.netherite": ("下界合金", "hand"),
    "gt6.row.mat.tantalum_hafnium_carbide": ("碳化钽铪", "hand"),
    # task p27-lang-fix-batch2 ⑤ (the lang-fix residue): the two Ultimet faces UNIFY on
    # 钴铬钨合金 (= gt6.material.ultimet, the batch1 P1 ruling). The dump's 哈氏合金
    # (:5736, also GTCEu Modern zh_cn.json:5753 — the same mistaken lineage) misattributes a
    # NICKEL-family name (Hastelloy) to a COBALT-based alloy: the upstream definition is
    # Co-majority (54% Co, 26% Cr, 9% Ni, 5% Mo, 2% W — MT.java:1760 uumAloy + comment), and
    # the real-world Ultimet (Haynes, UNS R31233) is Co-Cr-Ni-Mo-W. Two independent sources
    # (upstream MT composition + real-world metallurgy) against the dump's single error face.
    "gt6.row.mat.ultimet": ("钴铬钨合金", "hand"),
    "gt6.row.mat.tungsten_carbide": ("碳化钨", "hand"),
    "gt6.row.mat.stainless_steel": ("不锈钢", "hand"),
    "gt6.row.mat.adamantium": ("艾德曼合金", "hand"),
    "gt6.row.attachment.mat.ceramic": ("陶瓷", "hand"),
    "gt6.row.attachment.mat.plastic": ("塑料", "hand"),
    "gt6.row.attachment.mat.stainless_steel": ("不锈钢", "hand"),
    "gt6.row.attachment.mat.tungsten": ("钨", "hand"),
    "gt6.row.attachment.mat.tantalum_hafnium_carbide": ("碳化钽铪", "hand"),
    "gt6.row.attachment.mat.adamantium": ("艾德曼合金", "hand"),
    "gt6.row.mat.brick": ("砖", "hand"),
    "block.gt6.brick_burning_box": ("砖块燃烧室 (固体)", "hand"),
    "block.gt6.heat_transmitter": ("传热装置", "hand"),  # task p27-lang-fix P1 §2.2 (transmit≠absorb, ledger en_us.json:64)
    # task p27-lang-fix-batch2 P2 (ledger §3): 可调≠Custom / the rotation gearbox de-collides
    # from the adjustable one (en "Custom Wooden Gearbox" / "Wooden Transformer Gearbox").
    "block.gt6.gearbox": ("定制木制齿轮箱", "hand"),
    "block.gt6.transformer_rotation": ("木制转向齿轮箱", "hand"),
    "fluid.gt6.iron_molten": ("熔融铁", "hand"),
    "fluid.gt6.natural_gas": ("天然气", "hand"),
    "gt6.jei.info.multiblock_coke_oven": (
        "焦炉是一个 3x3x3 的立方体：将焦炉控制器放在其中一面的中央并朝外，"
        "保持立方体中心格为空，其余 25 格全部放满焦炉砖。点燃控制器即可启动"
        "——它自行积攒热量，结构下方一层的储罐会收集杂酚油。",
        "hand",
    ),

    # ---- restoration batch (task p26-pipe-item takeover salvage 2026-09-09): direct
    # hand rows earlier cards authored straight into the TSV without writing back here —
    # a faithful rerun (same dump + same hand table -> byte-identical TSV, the module
    # docstring contract) wiped them from zh_cn_ref.tsv and the regenerated zh_cn.json.
    # Re-entered VERBATIM from the pre-wipe TSV (git show cbd779bc:...zh_cn_ref.tsv):
    # lightning rod (p10) / grass (p13 family) / tools (p24) / food cans (p25) / misc.
    "block.gt6.grass": ("草方块", "hand"),
    "block.gt6.grass_black": ("草方块", "hand"),
    "block.gt6.grass_brown": ("草方块", "hand"),
    "block.gt6.grass_light_gray": ("草方块", "hand"),
    "block.gt6.grass_lime": ("草方块", "hand"),
    "block.gt6.grass_yellow": ("草方块", "hand"),
    "block.gt6.lightning_rod": ("避雷针", "hand"),
    "block.gt6.machine_wall_tungsten": ("钨壁板", "hand"),
    "block.gt6.multiblock_lightning_rod": ("避雷针电力输出口", "hand"),  # task p27-lang-fix P1 §2.2 — de-collides with the rod block 避雷针 (en "Lightning Rod Electric Output", en_us.json:73; the structure tooltip line 1 already used the long face)
    "block.gt6.niobium_titanium_coil": ("大型铌钛合金线圈", "hand"),
    "gt.grass.tooltip": ("不会传播、被吃、改变颜色, 也不需要光照", "hand"),
    "gt.grass.tooltip.spray": ("喷漆罐也可以用来给草染色！", "hand"),
    "gt6.tooltip.lightningrod.1": ("最底层是3x3的钨壁板, 中间是避雷针电力输出口", "hand"),
    "gt6.tooltip.lightningrod.2": ("第二层是3x3的大型铌钛合金线圈", "hand"),
    "gt6.tooltip.lightningrod.3": ("第三层是3x3的钨壁板", "hand"),
    "gt6.tooltip.lightningrod.4": ("第四层是3x3的大型铌钛合金线圈", "hand"),
    "gt6.tooltip.lightningrod.5": ("第五层是3x3的钨壁板", "hand"),
    "gt6.tooltip.lightningrod.6": ("最后在中心一格造一根避雷针柱", "hand"),
    "gt6.tooltip.lightningrod.7": ("最高的避雷针的Y轴高度需要大于100", "hand"),
    "gt6.tooltip.lightningrod.8": ("避雷针柱长度达到100m时达到最大效率", "hand"),
    "gt6.tooltip.lightningrod.9": ("256m内存在别的避雷针会降低效率", "hand"),
    "gt6.tooltip.lightningrod.capacity": ("%s EU 每次雷击", "hand"),
    "gt6.tooltip.lightningrod.energy": ("能量输出: %s EU/p（至多16安）", "hand"),
    "gt6.tooltip.lightningrod.structure": ("结构：", "hand"),
    "item.gt6.bending_cylinder_small": ("小型弯曲绕筒", "hand"),
    "item.gt6.builder_wand": ("建筑之杖", "hand"),
    "item.gt6.file": ("锉刀", "hand"),
    "item.gt6.food_can_cookies_huge": ("超大食物罐头 (饼干)", "hand"),
    "item.gt6.food_can_empty": ("空食物罐头", "hand"),
    "item.gt6.food_can_rotten_huge": ("超大食物罐头 (腐肉)", "hand"),
    "item.gt6.food_can_rotten_large": ("大食物罐头 (腐肉)", "hand"),
    "item.gt6.food_can_rotten_small": ("小食物罐头 (腐肉)", "hand"),
    "item.gt6.food_can_rotten_tall": ("高食物罐头 (腐肉)", "hand"),
    "item.gt6.food_can_rotten_tiny": ("迷你食物罐头 (腐肉)", "hand"),
    "item.gt6.food_can_rotten_wide": ("宽食物罐头 (腐肉)", "hand"),
    "item.gt6.hammer": ("锤", "hand"),
    "item.gt6.saw": ("锯", "hand"),
    "item.gt6.screwdriver": ("螺丝刀", "hand"),
    "item.gt6.wrench": ("扳手", "hand"),
    "itemGroup.gt6.food_cans": ("格雷科技: 罐头", "hand"),

    # ---- rebase write-back (review session 2026-09-09): the 33 direct rows the merged
    # p26-c-foam-fluid-refill card authored straight into the TSV (its 2ab32e16 fluid
    # family) without a py write-back — the same tsv-orphan drift class b91f65c3 fixed.
    # Surfaced by the p26-pipe-item rebase onto main: a faithful rerun over the merged
    # TSV silently wiped them. Re-entered VERBATIM from the merged TSV rows.
    "fluid.gt6.cfoam": ("建筑泡沫", "hand"),
    "fluid.gt6.cfoam_black": ("黑色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_red": ("红色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_green": ("绿色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_brown": ("褐色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_blue": ("蓝色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_purple": ("紫色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_cyan": ("青色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_light_gray": ("淡灰色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_gray": ("灰色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_pink": ("粉色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_lime": ("黄绿色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_yellow": ("黄色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_light_blue": ("淡蓝色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_magenta": ("品红建筑泡沫", "hand"),
    "fluid.gt6.cfoam_orange": ("橙色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_white": ("白色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_black": ("强化黑色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_red": ("强化红色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_green": ("强化绿色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_brown": ("强化褐色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_blue": ("强化蓝色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_purple": ("强化紫色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_cyan": ("强化青色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_light_gray": ("强化淡灰色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_gray": ("强化灰色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_pink": ("强化粉色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_lime": ("强化黄绿色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_yellow": ("强化黄色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_light_blue": ("强化淡蓝色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_magenta": ("强化品红建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_orange": ("强化橙色建筑泡沫", "hand"),
    "fluid.gt6.cfoam_owned_white": ("强化白色建筑泡沫", "hand"),

    # ---- rerun write-back (task p27-lang-fix 2026-09-11): 48 direct rows earlier cards
    # authored straight into the committed TSV without a py write-back — the same
    # tsv-orphan drift class the restoration/rebase batches fixed (a faithful rerun over
    # the hand table silently wipes them; surfaced by this card's pre-edit reproduction
    # run, which measured 48 truly-sourceless rows + 18 rows the committed TSV carries
    # TWICE from git-merge stitching — the rerun canonicalizes those away). Re-entered
    # VERBATIM from the committed TSV at main 28347d92:
    #   static storage (p26-storage-static-batch): bookshelf x10 / bottlecrate x10 /
    #     drawer x2 / locker x2 / safe x4;
    #   sensors (p26-sensors-core): electrometer / fluidometer / progressmeter;
    #   crucible (p26-crucible-multiblock): crucible_steel / crucible_steel_wall;
    #   C-Foam blocks (p26-c-foam-block-family): the four block rows;
    #   W1 trio (p26-w1-sifter-compressor-wiremill): compressor / sifter / wiremill;
    #   press+extruder (p26-w1-press-extruder-molds): the press/extruder family rows +
    #     the two shape_extruder molds.
    "block.gt6.bookshelf_acacia": ("金合欢木书架", "hand"),
    "block.gt6.bookshelf_birch": ("白桦木书架", "hand"),
    "block.gt6.bookshelf_cherry": ("樱花木书架", "hand"),
    "block.gt6.bookshelf_crimson": ("绯红木书架", "hand"),
    "block.gt6.bookshelf_dark_oak": ("深色橡木书架", "hand"),
    "block.gt6.bookshelf_jungle": ("丛林木书架", "hand"),
    "block.gt6.bookshelf_mangrove": ("红树木书架", "hand"),
    "block.gt6.bookshelf_oak": ("橡木书架", "hand"),
    "block.gt6.bookshelf_spruce": ("云杉木书架", "hand"),
    "block.gt6.bookshelf_warped": ("诡异木书架", "hand"),
    "block.gt6.bottlecrate_acacia": ("金合欢木瓶箱", "hand"),
    "block.gt6.bottlecrate_birch": ("白桦木瓶箱", "hand"),
    "block.gt6.bottlecrate_cherry": ("樱花木瓶箱", "hand"),
    "block.gt6.bottlecrate_crimson": ("绯红木瓶箱", "hand"),
    "block.gt6.bottlecrate_dark_oak": ("深色橡木瓶箱", "hand"),
    "block.gt6.bottlecrate_jungle": ("丛林木瓶箱", "hand"),
    "block.gt6.bottlecrate_mangrove": ("红树木瓶箱", "hand"),
    "block.gt6.bottlecrate_oak": ("橡木瓶箱", "hand"),
    "block.gt6.bottlecrate_spruce": ("云杉木瓶箱", "hand"),
    "block.gt6.bottlecrate_warped": ("诡异木瓶箱", "hand"),
    "block.gt6.drawer_bronze": ("青铜四格抽屉", "hand"),
    "block.gt6.drawer_steel": ("钢四格抽屉", "hand"),
    "block.gt6.electrometer": ("静电计传感器", "hand"),  # task p27-lang-fix-batch2 P2 (ledger §3): en "Electrometer Sensor" — 电力 reads power, the electrometer measures charge
    "block.gt6.fluidometer": ("流体体积传感器(单位: 升)", "hand"),
    "block.gt6.locker_bronze": ("青铜储物柜", "hand"),
    "block.gt6.locker_steel": ("钢储物柜", "hand"),
    "block.gt6.crucible_steel": ("大型钢坩埚", "hand"),
    "block.gt6.crucible_steel_wall": ("钢壁板", "hand"),
    "block.gt6.progressmeter": ("进度传感器", "hand"),
    "block.gt6.safe_keylocked_bronze": ("钥匙锁定青铜保险箱", "hand"),
    "block.gt6.safe_keylocked_steel": ("钥匙锁定钢保险箱", "hand"),
    "block.gt6.safe_mechanical_bronze": ("机械青铜保险箱", "hand"),
    "block.gt6.safe_mechanical_steel": ("机械钢保险箱", "hand"),
    "block.gt6.cfoam_fresh": ("新鲜建筑泡沫", "hand"),
    "block.gt6.cfoam": ("建筑泡沫", "hand"),
    # task p28-cfoam-lang-key: the owned carrier's display key was never written by any
    # provider face (the p26 card stopped at the 4 item-less-but-keyed forms above) — Jade
    # resolved block.gt6.cfoam_owned on the foam a player sprayed and fell back to the raw
    # key. Value = 强化建筑泡沫 (user ruling 2026-09-12: the strengthened-spray foam must
    # carry 强化, matching the en "Advanced C-Foam" face — the upstream owned foam is
    # exactly what the "Advanced C-Foam Spray" sprays, MultiItemRandomTools.java:259).
    "block.gt6.cfoam_owned": ("强化建筑泡沫", "hand"),
    "block.gt6.cfoam_fresh_slab": ("新鲜建筑泡沫台阶", "hand"),
    "block.gt6.cfoam_slab": ("建筑泡沫台阶", "hand"),
    "gt6.row.machine.compressor": ("压缩机 (%s)", "hand"),
    "gt6.row.machine.sifter": ("筛选机 (%s)", "hand"),
    "gt6.row.machine.wiremill": ("线材机 (%s)", "hand"),
    # task p28-lang-batch1-fix (user ruling 2026-09-12, research.p28-r-translation-community):
    # the batch1 压板机/挤压机 LOW-CONFIDENCE guesses are REVERTED to the community forms the
    # dump already carried — Press = 压力成型机 (mcmod GT6/GT6U 9 hits + TeamNED gt_recipe:69 +
    # the dump column; 压板机 0 hits) and Extruder = 压模器 (mcmod + TeamNED gt_recipe:38 +
    # GTCEu Modern zh_cn.json:258 + the dump column, 4 sources). The Low Heat tier keeps its
    # 低温 prefix over the new machine word (低温压模器 — the p27 P1 tier fix, preferred over
    # mcmod's 低热容). The shape_extruder molds (item.gt6.shape_extruder_*) KEEP their
    # dump-verbatim 模具 (板)/(杆) faces — the mold is a mold in the dump regardless of the
    # machine word.
    "gt6.row.machine.press.display": ("压力成型机 (%s)", "hand"),
    "gt6.row.machine.extruder.display": ("压模器 (%s)", "hand"),
    # task p27-lang-fix P1 §2.2 + batch2: the Low Heat tier display — the dump :11543 row (and
    # the pre-writeback TSV value) flattened T1 into the plain machine word; en "Low Heat
    # Extruder (%s)" (en_us.json) keeps the tier — DECLARED DEVIATION from the dump column.
    "gt6.row.machine.extruder.low_heat.display": ("低温压模器 (%s)", "hand"),
    "gt6.row.machine.press": ("压力成型机", "hand"),
    "gt6.row.machine.extruder": ("压模器", "hand"),
    "gt6.row.machine.extruder_low_heat": ("低温压模器", "hand"),
    "item.gt6.shape_extruder_plate": ("模具 (板)", "hand"),
    "item.gt6.shape_extruder_rod": ("模具 (杆)", "hand"),
    # ---- crucible Jade face (task p28-crucible-jade-face, 4 keys — the final key set after
    # the TFRU 33c22beb ruling): no dump face (upstream 1.7.10 has zero WAILA integration,
    # research.p28-r-crucible-jade-face), so these are pure hand rows. The total line's
    # 内容物 label IS the TFRU LH.CONTENT prefix form (first-row label shape); the K unit in
    # the temperature template stays untranslated (the upstream thermometer face
    # "Temperature: NK" MultiTileEntitySmeltery.java:512 carries it bare).
    "gt6.jade.crucible.temperature": ("温度: %s K / %s K", "hand"),
    "gt6.jade.crucible.total": ("内容物: %s U", "hand"),
    "gt6.jade.crucible.empty": ("空", "hand"),
    "gt6.jade.crucible.more": ("+%s 种", "hand"),
}

# ---------------------------------------------------------------------------
# The 442-key zh backfill (task p23-i18n-zh-442-backfill). Every missing zh key
# (en_us.json 2531 − zh_cn.json 2089, main census 2026-09-07) gets a hand-layer row:
#   365 gt6.tagprefix.*  = 213 identity (en template == "%s" -> zh keeps "%s", the
#                          TSV gem precedent) + 152 affixed templates;
#   28 block.gt6.* + 21 fluid.gt6.* + 18 item.gt6.spray* + 3 gt6.spraycan.* tooltips
#     + 5 gt6.material.* + 2 itemGroup.gt6.* tabs.
# Affix arbitration posture (the research card's voting aid): PRIMARY vote = the
# tmp/gregtech.lang dump's oredict.<camelPrefix><Material> composed rows (run
# gen_zhcn_ref.py --vote to replay it); RE-VOTE = the TeamNED package's oredict rows
# (tmp/harvest/p23-i18n-zh-teamned, --teamned). A prefix whose two votes agree or whose
# dump plurality is unambiguous (>=5 rows) is recorded below as status=hand with the
# winning form; the rulings flagged for human re-check are listed in the TAGPREFIX_RECHECK
# comment. Dump-verbatim wins are marked inline with the vote count.
# ---------------------------------------------------------------------------
IDENTITY_TAGPREFIXES = (
    # en template is exactly "%s" — zh keeps "%s" (the word IS the material name; the
    # TSV:135 gem precedent). Verified against en_us.json, census 2026-09-07.
    "alloy", "armor", "armor_boots", "armor_chestplate", "armor_helmet", "armor_leggings", "arrow", "bamboo",
    "bar", "bars", "battery", "battery_singleuse", "bauble", "beach", "beam", "beans",
    "bee", "berrybush", "bit", "blade", "block", "block_", "block_bamboo", "block_glass",
    "block_ore", "block_wool", "book", "boule", "bowl", "brick", "bud", "cable",
    "cactus", "chest", "chipset", "chunk", "circuit", "clean_gravel", "cloth", "clump",
    "cobblestone", "coin", "component", "compressed_cobblestone", "compressed_dirt", "compressed_gravel", "compressed_sand", "compressed_stone",
    "computer", "cones", "consumable", "cooking", "coral", "craft", "crafting", "crafting_tool",
    "crate_gt64_ore", "crate_gt_ore", "crop", "crystalline", "denseore", "desert", "dinosaur", "dirt",
    "dirty_gravel", "door", "drop", "dust_dirty", "dye", "dye_ceramic", "dye_mixable", "element",
    "elven", "epiphyte", "essence", "fabric", "fence", "fern", "fertilizer", "floating",
    "flower", "food", "forest", "frame", "frame_gt", "fuel", "fungus", "ganys",
    "gate", "gear", "glass", "glowstone", "grafter", "grass", "gravel", "ground",
    "handle", "hanging", "head", "immersed", "ingot_quad", "item", "item_", "item_dust",
    "jungle", "junk", "ladder", "lamp", "leaf", "leafy", "leaves", "liquid",
    "list", "log", "lumar", "lump", "mana", "material", "mffs", "molecule",
    "motor", "mountain", "mushroom", "mystic", "obsidian", "ocean", "orb", "ore_gem",
    "pane_glass", "panel", "paper", "part", "pearl", "pebbles", "pellet", "petal",
    "plains", "plank", "plant", "plasma", "plate_quad", "plating", "pole", "powder",
    "projred", "pulp", "quartz", "raw", "reactor", "record", "reduced", "reed",
    "river", "rock", "rod", "rubble", "rune", "sand", "sapling", "savanna",
    "scoop", "scrap", "scraps", "seed", "shard", "shears", "sheet", "sheet_double",
    "shrub", "skull", "slab", "soulsand", "stained_clay", "stained_glass", "stair", "stone",
    "stone_brick", "stone_bricks", "stone_bricks_mossy", "stone_chiseled", "stone_cobble", "stone_cracked", "stone_mossy", "stone_mossy_bricks",
    "stone_polished", "stone_smooth", "stonebrick", "storage", "tiny", "tome", "tool", "tool_axe",
    "tool_hoe", "tool_pickaxe", "tool_shears", "tool_shovel", "tool_sword", "torch", "trapdoor", "travelgear",
    "tree", "tree_leaves", "tree_sapling", "tube", "turbine", "vine", "wafer", "wall",
    "water", "wax", "wetlands", "wire", "wood",
)

VOTED_TAGPREFIXES = {
    # ---- dump-primary vote wins (composed oredict rows, plurality form cited) ----
    "boule_gt": "单晶%s",                       # dump 单晶铝 etc., 148 rows
    "bullet_gt_medium": "中号%s子弹",            # 850 rows (dump 中号..子弹)
    "casing_machine": "%s机器外壳",              # 209 rows
    "casing_machine_dense": "致密%s机器外壳",     # 209 rows
    "casing_machine_double": "强化%s机器外壳",    # 209 rows
    "casing_machine_quadruple": "高强%s机器外壳",  # 209 rows
    "crate_gt64_dust": "箱装%s粉",               # 1164 rows
    "crate_gt64_gem": "箱装%s晶体",              # 217 rows
    "crate_gt64_ingot": "箱装%s锭",              # 488 rows
    "crate_gt64_plate": "箱装%s板",              # 678 rows (the 335 副票 = plate_gem rows)
    "crate_gt64_plate_gem": "箱装结晶%s板",      # 335 rows — p28 gem/crystalline split: 结晶 heads the composed plate word (箱装结晶红宝石板), a declared re-order of the dump column
    "crate_gt64_raw": "箱装%s矿",                # 615 rows
    "crate_gt_dust": "小箱装%s粉",               # 1164 rows
    "crate_gt_gem": "小箱装%s",                  # 219 rows (dump verbatim: the gem rows carry no tail)
    "crate_gt_ingot": "小箱装%s锭",              # 488 rows
    "crate_gt_plate": "小箱装%s板",              # 678 rows
    "crate_gt_plate_gem": "小箱装结晶%s板",       # 335 rows — p28 split, same re-order as the gt64 row
    "crate_gt_raw": "小箱装%s矿",                # 615 rows
    "ingot_hot": "热%s锭",                      # 275 rows
    "ore": "%s矿",                              # the family tail shared by every ore<Stone> row
    "ore_andesite": "安山岩%s矿",                # 301 rows
    "ore_basalt": "玄武岩%s矿",                  # 316 rows
    "ore_bedrock": "基岩%s矿",                   # 418 rows
    "ore_blackgranite": "黑花岗岩%s矿",           # 351 rows
    "ore_blueschist": "蓝片岩%s矿",              # 320 rows
    "ore_darkprismarine": "暗海晶石%s矿",         # 305 rows
    "ore_deadrock": "死石%s矿",                  # 615 rows
    "ore_deepslate": "深板岩%s矿",               # 336 rows
    "ore_diorite": "闪长岩%s矿",                 # 338 rows
    "ore_endstone": "末地石%s矿",                # 443 rows
    "ore_gravel": "砾石%s矿",                    # 615 rows
    "ore_greenschist": "绿片岩%s矿",             # 352 rows
    "ore_holystone": "圣石%s矿",                 # 453 rows
    "ore_kimberlite": "金伯利岩%s矿",            # 252 rows
    "ore_komatiite": "科马提岩%s矿",             # 252 rows
    "ore_lightprismarine": "亮海晶石%s矿",        # 305 rows
    "ore_limestone": "石灰岩%s矿",               # 612 rows
    "ore_livingrock": "活石%s矿",                # 459 rows
    "ore_marble": "大理石%s矿",                  # 363 rows
    "ore_mud": "淤泥%s矿",                      # 467 rows
    "ore_netherrack": "下界岩%s矿",              # 465 rows
    "ore_quartzite": "石英岩%s矿",               # 381 rows
    "ore_red_sand": "红沙%s矿",                  # 479 rows
    "ore_redgranite": "红花岗岩%s矿",            # 351 rows
    "ore_sand": "沙%s矿",                       # 515 rows
    "ore_sandstone": "砂岩%s矿",                 # 612 rows
    "ore_shale": "页岩%s矿",                     # 481 rows
    "ore_slate": "板岩%s矿",                     # 481 rows
    "ore_small": "贫瘠%s矿",                     # 615 rows — the dump's Small Ore word
    "ore_vanillagranite": "花岗岩%s矿",           # 351 rows
    "ore_vanillastone": "石头%s矿",              # 413 rows
    "plant_gt_berry": "%s莓",                   # 1175 rows (plantGtBerry=%s莓)
    "plant_gt_blossom": "%s花",                  # 1175 rows
    "plant_gt_fiber": "%s线",                    # 1175 rows (dump verbatim)
    "plant_gt_twig": "%s枝",                     # 1175 rows
    "plant_gt_wart": "%s疣",                     # 1175 rows
    "scrap_gt": "%s废料",                        # 1176 rows
}

# The rulings below were authored by hand where BOTH votes fell empty (the dump has no
# composed row for the prefix). Anchors: the dump's established word roots (致密/下界/
# 岩石词根/管道, the TSV rows-size words 小型/中型/大型/巨型) and en semantics. The
# "×" glue follows the TSV gt6.wire.display convention; wire=线 vs cable=线缆 keeps the
# B1 form split.
# task p28-lang-batch1-fix (user ruling 2026-09-12): the 24 planet heads are LOCALIZED per
# research.p28-r-translation-community — the cross-mod convention (火星 across GTOCore/
# Galaxy Space/Ad Astra/Mekanism-AA) plus the in-package precedents (dump 谷神星 / 火星沙).
# Watch the two collisions the research table flags: Mercury = 水星 (the planet) never
# 水银 (the element), Titan = 土卫六 (the moon) never the 钛 material word.
HAND_TAGPREFIXES = {
    "bottle": "%s瓶",
    "bucket": "%s桶",
    "capcellcon": "%s胶囊电池容器",      # Capsule Cell Container
    "capsule": "%s胶囊",
    "cell": "%s电池单元",                # RECHECK: Cell noun (GT fluid cell)
    "cluster": "天然%s晶簇",             # Native %s Cluster
    "compressed": "压缩%s",
    "crystal": "%s晶体",                 # the dump gem/crate word (gemAlexandrite=紫翠玉晶体)
    "crystal_pure": "纯净%s晶体",
    "dust_impure": "杂质%s粉",           # RECHECK: Impure Pile of %s Dust
    "dust_pure": "纯净%s粉",
    "dust_refined": "精炼%s粉",
    "gem_ore": "%s矿石",
    "gem_polished": "抛光%s",
    "gem_raw": "生%s",
    "gem_uncut": "未切割%s",
    # ore_<stone> faces with NO dump row: vanilla/geology words translated; the 24 planet
    # heads localized per the p28 ruling (the comment block above carries the evidence and
    # the collision watches).
    "ore_betweenstone": "交错石%s矿",
    "ore_blackstone": "黑石%s矿",
    "ore_callisto": "木卫四%s矿",
    "ore_ceres": "谷神星%s矿",
    "ore_deimos": "火卫二%s矿",
    "ore_dense": "致密%s矿",                 # the dump 致密 word root
    "ore_end": "末地%s矿",
    "ore_eris": "阋神星%s矿",
    "ore_europa": "木卫二%s矿",
    "ore_ganymede": "木卫三%s矿",
    "ore_gneiss": "片麻岩%s矿",
    "ore_grayschist": "灰片岩%s矿",          # RECHECK: en says "Schist %s Ore"; dump colors 蓝片岩/绿片岩 extended
    "ore_iapetus": "土卫八%s矿",
    "ore_io": "木卫一%s矿",
    "ore_jupiter": "木星%s矿",
    "ore_kepler22b": "开普勒22b%s矿",
    "ore_mars": "火星%s矿",
    "ore_mercury": "水星%s矿",               # the planet 水星, never the element 水银
    "ore_moon": "月球%s矿",
    "ore_nether": "下界%s矿",
    "ore_normal": "普通%s矿",
    "ore_neptune": "海王星%s矿",
    "ore_oberon": "天卫四%s矿",
    "ore_phobos": "火卫一%s矿",
    "ore_pinkschist": "粉片岩%s矿",          # RECHECK: dump naming-pattern extension
    "ore_pitstone": "麻纹石%s矿",
    "ore_pluto": "冥王星%s矿",
    "ore_poor": "劣质%s矿",                  # RECHECK: Poor Ore (ore_small already owns 贫瘠 per the dump)
    "ore_rich": "富集%s矿",
    "ore_rhea": "土卫五%s矿",
    "ore_saturn": "土星%s矿",
    "ore_siltstone": "粉砂岩%s矿",
    "ore_space": "太空%s矿",
    "ore_strangesand": "异沙%s矿",
    "ore_titan": "土卫六%s矿",               # the moon 土卫六, never the 钛 material word
    "ore_titania": "天卫三%s矿",
    "ore_triton": "海卫一%s矿",
    "ore_umberstone": "棕石%s矿",            # the Umber material (en face "Umberstone", en_us.json:1764)
    "ore_uranus": "天王星%s矿",
    "ore_venus": "金星%s矿",
    "oreberry": "%s莓",                      # the plant_gt_berry dump word
    "orebush": "%s灌木",
    # pipes: dump MTE 26060-26066 word set (微型/小型/裸/大型/巨型 + 四合一/九合一...流体管道)
    "pipe": "%s管道",
    "pipe_huge": "巨型%s管道",
    "pipe_large": "大型%s管道",
    "pipe_medium": "中型%s管道",
    "pipe_nonuple": "九合一%s管道",
    "pipe_quadruple": "四合一%s管道",
    "pipe_restrictive_huge": "巨型限流%s管道",
    "pipe_restrictive_large": "大型限流%s管道",
    "pipe_restrictive_medium": "中型限流%s管道",
    "pipe_restrictive_small": "小型限流%s管道",
    "pipe_restrictive_tiny": "微型限流%s管道",
    "pipe_small": "小型%s管道",
    "pipe_tiny": "微型%s管道",
    "plate_steamcraft": "薄%s板",
    "raw_ore_chunk": "生%s矿石块",           # RECHECK: Raw Chunk of %s Ore
    "sheet_gt": "%s薄板",                    # RECHECK: Sheet vs the existing %s板 plate
    # wire_gt/cable_gt: BOTH votes empty (the 1.7.10 dump's oredict section has no wireGt
    # rows) — the task-card ruling applies: "%s×%s线" with the × sign (the TSV
    # gt6.wire.display convention), the numeral literal riding the template like the en;
    # cable keeps the B1 wire=线 / cable=线缆 split.
    "wire_gt01": "1×%s线",
    "wire_gt02": "2×%s线",
    "wire_gt03": "3×%s线",
    "wire_gt04": "4×%s线",
    "wire_gt05": "5×%s线",
    "wire_gt06": "6×%s线",
    "wire_gt07": "7×%s线",
    "wire_gt08": "8×%s线",
    "wire_gt09": "9×%s线",
    "wire_gt10": "10×%s线",
    "wire_gt11": "11×%s线",
    "wire_gt12": "12×%s线",
    "wire_gt13": "13×%s线",
    "wire_gt14": "14×%s线",
    "wire_gt15": "15×%s线",
    "wire_gt16": "16×%s线",
    "cable_gt01": "1×%s线缆",
    "cable_gt02": "2×%s线缆",
    "cable_gt04": "4×%s线缆",
    "cable_gt08": "8×%s线缆",
    "cable_gt12": "12×%s线缆",
}

for _snake in IDENTITY_TAGPREFIXES:
    _key = "gt6.tagprefix." + _snake
    if _key in HAND_TRANSLATIONS:
        sys.exit(f"identity prefix {_key} already in the hand layer")
    HAND_TRANSLATIONS[_key] = ("%s", "hand")
for _table in (VOTED_TAGPREFIXES, HAND_TAGPREFIXES):
    for _snake, _value in _table.items():
        _key = "gt6.tagprefix." + _snake
        if _key in HAND_TRANSLATIONS:
            sys.exit(f"voted/hand prefix {_key} already in the hand layer")
        HAND_TRANSLATIONS[_key] = (_value, "hand")

# ---- block.gt6.* (28): the barrel/fluid-pipe rows join the dump MTE face where one
# exists ( Wooden Barrel -> gt.multitileentity.6990 木制储物桶); the rest follow en
# semantics over the dump's established material words (钨钢/碳化钽铪/艾德曼合金/下界合金
# ...). Drum = 储罐 per the P0 audit ruling (task p27-lang-fix, ledger §1.1): the drums
# are FLUID TANKS, not drums-as-cylinders — three independent sources (the upstream
# registration rides the "Fluid Containers" category with NBT_TANK_CAPACITY,
# Loader_MultiTileEntities.java:2168-2171; the community dump itself renders the family
# as 储罐, tmp/gregtech.lang:13535-13540 木制储罐/塑料储罐/不锈钢储罐/钨钢储罐/钨储罐/
# 艾德曼合金储罐; and no dictionary supports 鼓 for a container). The 13 鼓 rows flip to
# 储罐; the two already-correct faces stay (barrel_logistics 物流储罐 = the in-family
# exemplar, barrel_plastic 塑料罐 = Canister); barrel_wood 木制储物桶 keeps its dump join
# (item barrel, P2 ledger row, out of this card's scope). Draconium rides the dump's
# own word (bouleGtDraconiumAwakened = 单晶觉醒龙 -> 龙). The machine words reuse the
# committed TSV rows units (粉碎机/破碎机/车床) verbatim; 烤箱 per the task-card
# spot-check. wire_electric = the × convention on the atomic legacy keys.
BLOCK_BACKFILL = {
    "block.gt6.barrel_adamantium": "艾德曼合金储罐",
    "block.gt6.barrel_awakened_draconium": "觉醒龙合金储罐",
    "block.gt6.barrel_draconium": "龙合金储罐",      # RECHECK: dump 龙 word root (单晶觉醒龙)
    "block.gt6.barrel_gaia_spirit": "盖亚储罐",
    "block.gt6.barrel_infinity": "无限储罐",
    "block.gt6.barrel_logistics": "物流储罐",
    "block.gt6.barrel_metal": "青铜储罐",            # the row material word (TSV gt6.row.mat.bronze)
    "block.gt6.barrel_netherite": "下界合金储罐",
    "block.gt6.barrel_plastic": "塑料罐",            # Canister = 罐
    "block.gt6.barrel_tantalum_hafnium_carbide": "碳化钽铪储罐",
    "block.gt6.barrel_titanium": "钛储罐",
    "block.gt6.barrel_tungsten": "钨储罐",
    "block.gt6.barrel_tungsten_alloy": "钨合金储罐",
    "block.gt6.barrel_tungstensteel": "钨钢储罐",
    "block.gt6.barrel_void_metal": "虚空金属储罐",
    "block.gt6.advanced_crafting_table": "高级合成台",  # task p24-act-machine — the GT6 community zh name (no upstream zh_CN.lang dump anchor)
    "block.gt6.barrel_wood": "木制储物桶",           # dump join: gt.multitileentity.6990 木制储物桶
    "block.gt6.crank": "手摇曲柄",
    "block.gt6.crusher": "破碎机",                   # = the TSV gt6.row.machine.crusher unit verbatim
    "block.gt6.energy_source": "测试能源",
    "block.gt6.lathe": "车床",                       # = the TSV gt6.row.machine.lathe unit verbatim
    "block.gt6.multiblock_coke_oven": "焦炉",        # = the JEI info page word (gt6.jei.info row)
    "block.gt6.multiblock_coke_oven_bricks": "焦炉砖",
    "block.gt6.shredder": "粉碎机",                  # = the TSV gt6.row.machine.shredder unit verbatim
    "block.gt6.wire_electric_1x": "1×导线",  # task p27-lang-fix-batch2 P2 (ledger §3): wire=导线 unified
    "block.gt6.wire_electric_2x": "2×导线",  # (en "1x/2x Electric Wire"; the × convention kept)
    "block.gt6.wood_fluid_pipe_medium": "木制流体管道",  # dump 26xxx ...流体管道 word set
    "block.gt6.wood_fluid_pipe_small": "小型木制流体管道",
}

# ---- fluid.gt6.* (21): hand translations with dump anchors (蒸馏水/柴油/幻露 per the
# research card; the lowercase code keys (mnwtr/spdew/waterdirty) follow their en
# semantics per the task-card ruling).
FLUID_BACKFILL = {
    "fluid.gt6.cactuswater": "仙人掌汁",
    "fluid.gt6.cold_water": "冷水",
    "fluid.gt6.diesel": "柴油",
    "fluid.gt6.distilled_water": "蒸馏水",
    "fluid.gt6.ethanol": "乙醇",
    "fluid.gt6.fuel": "燃料",
    "fluid.gt6.hot_water": "热水",
    "fluid.gt6.jetfuel": "喷气燃料",
    "fluid.gt6.kerosine": "煤油",
    "fluid.gt6.maplesap": "枫树汁液",      # task p27-lang-fix-batch2 P2 (ledger §3): sap is the sap, not syrup
    "fluid.gt6.mnwtr": "矿泉水",           # en semantics: Mineral Water (task p27-lang-fix-batch2 P2: the community face)
    "fluid.gt6.nitrofuel": "硝基燃料",
    "fluid.gt6.petrol": "汽油",
    "fluid.gt6.reedwater": "芦苇水",
    "fluid.gt6.sap": "树液",
    "fluid.gt6.seawater": "海水",
    "fluid.gt6.spdew": "幻露",             # en semantics: Spectral Dew (research-card anchor)
    "fluid.gt6.steam": "蒸汽",
    "fluid.gt6.water_boiling": "沸水",
    "fluid.gt6.water_geothermal": "温泉水",
    "fluid.gt6.waterdirty": "污水",        # en semantics: Dirty Water
}

# ---- dye-chemical fluids + chlorine (17): hand translations with dump anchors — the
# dump carries the whole family (S:fluid.dye.chemical.* tmp/gregtech.lang:230-244,
# S:fluid.chlorine=氯 :168). Keys are the port snake ids (dye_chemical_<DYE_IDS[i]>,
# GTFluids.dyeChemicalName — the upstream 1.7.10 keys fold "Light Gray" to "lightgray",
# the port stays the spray-can snake); values are the dump faces verbatim, keyed by
# colour (the DYE_NAMES order, same index the port ids walk).
DYE_CHEMICAL_BACKFILL = {
    "fluid.gt6.dye_chemical_black": "黑色化学染料",       # dump S:fluid.dye.chemical.black
    "fluid.gt6.dye_chemical_red": "红色化学染料",         # dump S:fluid.dye.chemical.red
    "fluid.gt6.dye_chemical_green": "绿色化学染料",       # dump S:fluid.dye.chemical.green
    "fluid.gt6.dye_chemical_brown": "褐色化学染料",       # dump S:fluid.dye.chemical.brown
    "fluid.gt6.dye_chemical_blue": "蓝色化学染料",        # dump S:fluid.dye.chemical.blue
    "fluid.gt6.dye_chemical_purple": "紫色化学染料",      # dump S:fluid.dye.chemical.purple
    "fluid.gt6.dye_chemical_cyan": "青色化学染料",        # dump S:fluid.dye.chemical.cyan
    "fluid.gt6.dye_chemical_light_gray": "淡灰色化学染料", # dump S:fluid.dye.chemical.lightgray
    "fluid.gt6.dye_chemical_gray": "灰色化学染料",        # dump S:fluid.dye.chemical.gray
    "fluid.gt6.dye_chemical_pink": "粉色化学染料",        # dump S:fluid.dye.chemical.pink
    "fluid.gt6.dye_chemical_lime": "黄绿色化学染料",      # dump S:fluid.dye.chemical.lime
    "fluid.gt6.dye_chemical_yellow": "黄色化学染料",      # dump S:fluid.dye.chemical.yellow
    "fluid.gt6.dye_chemical_light_blue": "淡蓝色化学染料", # dump S:fluid.dye.chemical.lightblue
    "fluid.gt6.dye_chemical_magenta": "品红化学染料",     # dump S:fluid.dye.chemical.magenta
    "fluid.gt6.dye_chemical_orange": "橙色化学染料",      # dump S:fluid.dye.chemical.orange
    "fluid.gt6.dye_chemical_white": "白色化学染料",       # dump S:fluid.dye.chemical.white
    "fluid.gt6.chlorine": "氯",                           # dump S:fluid.chlorine :168
}

# ---- spray domain (18 items + 3 tooltips) + 2 tabs: ALL hand (P22 keys, no upstream
# face anywhere). Color names = the vanilla zh_cn dye words; the tooltip template keeps
# the en "%s.%s" remaining-uses shape verbatim.
SPRAY_BACKFILL = {
    "item.gt6.spray_can_empty": "空喷罐",
    "item.gt6.spray_paint_black": "喷漆（黑）",
    "item.gt6.spray_paint_blue": "喷漆（蓝）",
    "item.gt6.spray_paint_brown": "喷漆（棕）",
    "item.gt6.spray_paint_cyan": "喷漆（青）",
    "item.gt6.spray_paint_gray": "喷漆（灰）",
    "item.gt6.spray_paint_green": "喷漆（绿）",
    "item.gt6.spray_paint_light_blue": "喷漆（淡蓝）",
    "item.gt6.spray_paint_light_gray": "喷漆（淡灰）",
    "item.gt6.spray_paint_lime": "喷漆（黄绿）",
    "item.gt6.spray_paint_magenta": "喷漆（品红）",
    "item.gt6.spray_paint_orange": "喷漆（橙）",
    "item.gt6.spray_paint_pink": "喷漆（粉）",
    "item.gt6.spray_paint_purple": "喷漆（紫）",
    "item.gt6.spray_paint_red": "喷漆（红）",
    "item.gt6.spray_paint_remover": "除漆喷剂",
    "item.gt6.spray_paint_white": "喷漆（白）",
    "item.gt6.spray_paint_yellow": "喷漆（黄）",
    "gt6.spraycan.decolor": "可为物品除色",
    "gt6.spraycan.paint": "可将物品喷成%s",
    "gt6.spraycan.remaining": "剩余使用次数：%s.%s",
}

# ---- C-Foam spray domain (35 rows: 32 items + tab + 2 tooltips) — task
# p25-c-foam-pipe-spray. The 32 item names are the DUMP faces verbatim (metas 1100+2i
# "建筑泡沫喷罐" / 1132+2i, tmp/gregtech.lang:9350-9431) with the owned-word evolution
# trail: the dump's owned-spray rows read 加强建筑泡沫喷罐 — task p27-lang-fix batch
# rule B4 unified the 16 owned items onto 高级 (then the fluid face's word); task
# p28-cfoam-lang-key (user rulings 2026-09-12) renamed the owned-FLUID faces to
# 强化X建筑泡沫 and the owned BLOCK face to 强化建筑泡沫, and the follow-up ruling the
# same day unified the 16 owned ITEM sprays onto 强化建筑泡沫喷罐 (…) too — the dump's
# 加强 and the B4 高级 interlude are both superseded; the whole owned cfoam family
# (block + fluids + item sprays) now carries the single 强化 word root.
# The tooltip template splits the dump's gt.behaviour.foamspray.N.tooltip rows (:992-1007)
# at the colour word (the %s slot rides the untranslated DYE_NAMES arg, the p22
# spraycan.paint form); the owned warning has no dump face (upstream :259 en only) — hand.
FOAM_SPRAY_BACKFILL = {
    "item.gt6.foam_spray_black": "建筑泡沫喷罐 (黑色)",            # dump S:gt.multiitem.randomtools.1100
    "item.gt6.foam_spray_black": "建筑泡沫喷罐 (黑色)",            # dump S:gt.multiitem.randomtools.1100
    "item.gt6.foam_spray_red": "建筑泡沫喷罐 (红色)",              # :1102
    "item.gt6.foam_spray_green": "建筑泡沫喷罐 (绿色)",            # :1104
    "item.gt6.foam_spray_brown": "建筑泡沫喷罐 (褐色)",            # :1106
    "item.gt6.foam_spray_blue": "建筑泡沫喷罐 (蓝色)",             # :1108
    "item.gt6.foam_spray_purple": "建筑泡沫喷罐 (紫色)",           # :1110
    "item.gt6.foam_spray_cyan": "建筑泡沫喷罐 (青色)",             # :1112
    "item.gt6.foam_spray_light_gray": "建筑泡沫喷罐 (淡灰色)",     # :1114
    "item.gt6.foam_spray_gray": "建筑泡沫喷罐 (灰色)",             # :1116
    "item.gt6.foam_spray_pink": "建筑泡沫喷罐 (粉色)",             # :1118
    "item.gt6.foam_spray_lime": "建筑泡沫喷罐 (黄绿色)",           # :1120
    "item.gt6.foam_spray_yellow": "建筑泡沫喷罐 (黄色)",           # :1122
    "item.gt6.foam_spray_light_blue": "建筑泡沫喷罐 (淡蓝色)",     # :1124
    "item.gt6.foam_spray_magenta": "建筑泡沫喷罐 (品红)",          # :1126
    "item.gt6.foam_spray_orange": "建筑泡沫喷罐 (橙色)",           # :1128
    "item.gt6.foam_spray_white": "建筑泡沫喷罐 (白色)",            # :1130
    "item.gt6.foam_spray_owned_black": "强化建筑泡沫喷罐 (黑色)",  # dump S:gt.multiitem.randomtools.1132
    "item.gt6.foam_spray_owned_red": "强化建筑泡沫喷罐 (红色)",    # :1134
    "item.gt6.foam_spray_owned_green": "强化建筑泡沫喷罐 (绿色)",  # :1136
    "item.gt6.foam_spray_owned_brown": "强化建筑泡沫喷罐 (褐色)",  # :1138
    "item.gt6.foam_spray_owned_blue": "强化建筑泡沫喷罐 (蓝色)",   # :1140
    "item.gt6.foam_spray_owned_purple": "强化建筑泡沫喷罐 (紫色)", # :1142
    "item.gt6.foam_spray_owned_cyan": "强化建筑泡沫喷罐 (青色)",   # :1144
    "item.gt6.foam_spray_owned_light_gray": "强化建筑泡沫喷罐 (淡灰色)", # :1146
    "item.gt6.foam_spray_owned_gray": "强化建筑泡沫喷罐 (灰色)",   # :1148
    "item.gt6.foam_spray_owned_pink": "强化建筑泡沫喷罐 (粉色)",   # :1150
    "item.gt6.foam_spray_owned_lime": "强化建筑泡沫喷罐 (黄绿色)", # :1152
    "item.gt6.foam_spray_owned_yellow": "强化建筑泡沫喷罐 (黄色)", # :1154
    "item.gt6.foam_spray_owned_light_blue": "强化建筑泡沫喷罐 (淡蓝色)", # :1156
    "item.gt6.foam_spray_owned_magenta": "强化建筑泡沫喷罐 (品红)", # :1158
    "item.gt6.foam_spray_owned_orange": "强化建筑泡沫喷罐 (橙色)", # :1160
    "item.gt6.foam_spray_owned_white": "强化建筑泡沫喷罐 (白色)",  # :1162
    "itemGroup.gt6.foam_sprays": "建筑泡沫喷罐",
    "gt6.foamspray.paint": "可以喷射%s泡沫",                       # dump S:gt.behaviour.foamspray.N.tooltip :992-1007 split
    "gt6.foamspray.owned": "干燥后仅所有者可破坏",                 # hand — upstream :259 en has no dump face
}

# ---- gt6.material.* (5): the dump rows are pure ASCII (review) or absent — Breeze/
# Carminite/Fireleaf/Golden Amber per the research-card rulings (marked for re-check);
# Superconductor = the TeamNED double-source cross (lang/gt_material/zh_cn.lang:1705 +
# source/translated/GregTech.lang:5889).
MATERIAL_BACKFILL = {
    "gt6.material.breeze": "微风",            # RECHECK
    "gt6.material.carminite": "胭脂石",        # RECHECK
    "gt6.material.fireleaf": "火叶",           # RECHECK
    "gt6.material.golden_amber": "金琥珀",     # RECHECK
    "gt6.material.superconductor": "超导体",   # TeamNED double-source cross
}

# ---- itemGroup.gt6.* (2): the P22 spray-can tab + the billet prefix tab (the billet
# word = the existing %s坯料 template's noun).
TAB_BACKFILL = {
    "itemGroup.gt6.billet": "坯料",
    "itemGroup.gt6.spray_cans": "喷漆罐",
}

# ---------------------------------------------------------------------------
# The p26 mold/crucible/faucet chain zh gap (task p27-lang-fix-batch2, ledger §6: en 2793 −
# zh 2723 = 70 keys the MoldLang/CrucibleLang en faces carry with no zh face). Dump-verbatim
# priority per the card:
#   raw clay items  = tmp/gregtech.lang:10026-10092 (粘土模具 (X), the MultiItemRandomTools
#                     900-929 chain) + :10102 blank (991 Clay Mold) + :10104 faucet (992);
#   formed molds    = NO dump face (upstream had ONE mold item that hardens in place; the
#                     port split raw→fired into block+raw pairs) — the raw face's 粘土→陶瓷
#                     swap on the SAME dump shape words (the fired-mold dump faces 模具 (X)
#                     :10127+ already belong to the shape_extruder items, so the 陶瓷 head
#                     keeps the two families distinguishable);
#   stone mold      = NO dump face — en "Stone Mold" semantics (石头模具);
#   smelteries      = :10772/:10893/:10895 verbatim (熔炼坩埚 (石头)/(青铜)/(钢), MTE
#                     1000/1020/1022; en renamed the rows "Smeltery", zh keeps the dump word);
#   faucets         = the composed template over the dump faces :11199/:11224 (坩埚浇铸口
#                     (石头)/(陶瓷)) — the template word order follows the dump column
#                     (坩埚浇铸口 (%s), the CJK reorder precedent; NOT the attachment tap's
#                     龙头 — the dump names the faucet family 浇铸口);
#   the shape-word map is name-joined against the upstream en faces (MultiItemRandomTools
#   913 "Clay Spade Mold"=铲 / 914 "Clay Shovel Mold"=锹 / 928 Nugget=粒 / 929 Billet=胚料 /
#   901 Chunk=小块 — NOT the tagprefix faces 碎块/坯料).
# ---------------------------------------------------------------------------
MOLD_SHAPE_WORDS = {
    "ingot": "锭", "billet": "胚料", "chunk": "小块", "plate": "板", "tiny_plate": "小块板",
    "bolt": "螺栓", "rod": "杆", "long_rod": "长杆", "item_casing": "外壳", "ring": "环",
    "gear": "齿轮", "small_gear": "小型齿轮", "sword": "剑", "pickaxe": "镐", "spade": "铲",
    "shovel": "锹", "universal_spade": "万用铲", "axe": "斧", "double_axe": "双刃斧",
    "saw": "锯", "hammer": "锤", "file": "锉刀", "screwdriver": "螺丝刀", "chisel": "凿子",
    "arrow": "箭", "hoe": "锄", "sense": "镰刀", "plow": "犁", "builderwand": "建筑之杖",
    "nugget": "粒",
}

MOLD_CRUCIBLE_GAP_BACKFILL = {
    "gt6.row.crucible.display.smeltery_stone": "熔炼坩埚 (石头)",     # dump :10772 verbatim (MTE 1000)
    "gt6.row.crucible.display.smeltery_bronze": "熔炼坩埚 (青铜)",    # dump :10893 verbatim (MTE 1020)
    "gt6.row.crucible.display.smeltery_steel": "熔炼坩埚 (钢)",       # dump :10895 verbatim (MTE 1022)
    "gt6.row.mold.display.mold_ceramic": "陶瓷模具",                  # the formed blank (raw = dump :10102)
    "item.gt6.mold_ceramic_raw": "粘土模具",                          # dump :10102 verbatim (Clay Mold, 991)
    "gt6.row.mold.display.mold_stone": "石头模具",                    # no dump face — en "Stone Mold" semantics
    "gt6.row.faucet.display": "坩埚浇铸口 (%s)",                      # dump :11199 word order (坩埚浇铸口 (石头))
    "gt6.row.faucet.mat.stone": "石头",                               # renders dump :11199 坩埚浇铸口 (石头)
    "gt6.row.faucet.mat.ceramic": "陶瓷",                             # renders dump :11224 坩埚浇铸口 (陶瓷)
    "item.gt6.faucet_ceramic_raw": "粘土浇铸口",                      # dump :10104 verbatim (Clay Faucet, 992)
}
for _shape, _word in MOLD_SHAPE_WORDS.items():
    MOLD_CRUCIBLE_GAP_BACKFILL["gt6.row.mold.display.mold_ceramic_" + _shape] = "陶瓷模具 (%s)" % _word
    MOLD_CRUCIBLE_GAP_BACKFILL["item.gt6.mold_ceramic_" + _shape + "_raw"] = "粘土模具 (%s)" % _word

for _backfill in (BLOCK_BACKFILL, FLUID_BACKFILL, DYE_CHEMICAL_BACKFILL, SPRAY_BACKFILL, FOAM_SPRAY_BACKFILL, MATERIAL_BACKFILL, TAB_BACKFILL, MOLD_CRUCIBLE_GAP_BACKFILL):
    for _key, _value in _backfill.items():
        if _key in HAND_TRANSLATIONS:
            sys.exit(f"backfill row {_key} already in the hand layer")
        HAND_TRANSLATIONS[_key] = (_value, "hand")


# ---------------------------------------------------------------------------
# Family-row override layer (task p27-lang-fix P1, ledger tmp/p27-lang-audit/
# ledger.md §2): dump family rows whose VALUE is wrong or misleading get a hand
# value here — the TSV stays a function of dump + hand tables (ADR §1.1), and the
# overridden rows carry status=hand so the review face shows they left the
# dump-verbatim lane. Key = (kind, dump-source): material rows are the
# gt.material.<Pascal> internal name, itemgroup rows the itemGroup.<camel> name.
# The two ledger KEEP rows (force=力量, blue_alloy=蓝石合金) stay dump-verbatim,
# and TritaniumElemental=三钛 already agrees with the fixed Tritanium=三钛合金
# (element vs alloy faces) — neither gets a row. The two ledger-flagged
# low-confidence rulings (ElectrotineAlloy / NikolineAlloy, no strong community
# convention) are translated conservatively and declared in the card report.
FAMILY_OVERRIDES = {
    # -- §2.1 material names: 误译/撞名/语义丢失 (29 rows) --
    ("material", "Adamantine"): "阿达曼金",                 # 血石 is a DIFFERENT stone (bloodstone); de-collides 精金/艾德曼合金
    ("material", "Chromite"): "铬铁矿",                     # 铬铁 reads as the ferrochromium alloy
    ("material", "Ferberite"): "钨铁矿",                    # 钨铁 reads as ferrotungsten
    ("material", "Huebnerite"): "钨锰矿",                   # the same -ite mineral-family face
    ("material", "Ilmenite"): "钛铁矿",                     # 钛铁 reads as the Fe-Ti alloy
    ("material", "Magnetite"): "磁铁矿",                    # 磁铁 = magnet, the device
    ("material", "VanadiumMagnetite"): "钒磁铁矿",
    ("material", "Endstone"): "末地石",                     # 末地 is the dimension; vanilla zh 末地石
    ("material", "Netherrack"): "下界岩",                   # 下界 is the dimension; vanilla zh 下界岩
    ("material", "Phosphorus"): "磷",                       # 磷酸钙 = calcium phosphate (apatite component), not the element P
    ("material", "Orichalcum"): "奥利哈刚",                 # 自然之石 unattested; oriharukon already owns 山铜
    ("material", "SkyStone"): "天空石",                     # 陨石 collides with meteorite 陨石
    ("material", "WoodBleached"): "漂白木",                 # 星光木 unattested (en "Bleached Wood")
    ("material", "WoodVarnished"): "上漆木",                # 漆包 is the wire-enamel term (en "Varnished Wood")
    ("material", "TungstenSintered"): "烧结钨",             # sintered = 烧结, not 结晶
    ("material", "Tritanium"): "三钛合金",                  # en "Tritanium Alloy"; 钛金 collides with titanium_gold
    ("material", "Trinaquadalloy"): "特林-硅岩-钍瑞铌合金",  # Tri-Naqua-Duranium split; en carries no carbon — 去「碳化」
    ("material", "Ultimet"): "钴铬钨合金",                  # Ultimet = Co-Cr-W; Hastelloy = Ni family — misattributed
    ("material", "VoidCrystal"): "虚空",                    # en "Void" — no coal; joins void_metal 虚空金属
    ("material", "LavenderQuartz"): "薰衣草石英",           # 熏香 = incense
    ("material", "ElectricalSteel"): "电工钢",              # 磁钢 = magnet steel
    ("material", "FishCooked"): "熟鱼粉",                   # en Fishmeal = feed, not edible fish meat
    ("material", "FishRaw"): "生鱼粉",
    ("material", "FishRotten"): "腐鱼粉",
    ("material", "Forcicium"): "力素",                      # the three-force family de-collision (force 力量 stays)
    ("material", "Forcillium"): "力晶",
    ("material", "ElectrotineAlloy"): "蓝石荧合金",         # 低置信待社区核 (ledger §2.1: community has no strong convention)
    ("material", "NikolineAlloy"): "蓝石镍合金",            # 低置信待社区核 (ledger §2.1)
    ("material", "Infuscolium"): "因弗斯科姆",              # 精金 collides with adamantite — phonetic
    # -- §2.2 prefix-tab titles (itemgroup family, 2 rows) --
    ("itemgroup", "toolHeadUniversalSpade"): "万用铲头",    # 铲头毛坯 pasted the RAW group's name onto the finished group (en "Universal Spade Heads"; the raw group owns 毛坯)
    ("itemgroup", "wireFine"): "细导线",                    # 细线缆 mixed the cable word into the wire tab (en "Fine Wires")
    # -- §3 P2 rows (task p27-lang-fix-batch2, ledger §3) --
    # prefix-tab titles (itemgroup family): the en-qualifier restores + the two unification
    # rulings (洗净 for purified, 双重/三重/四重/五重 for the multiplicity ladder — the dump's
    # 2x/3x faces split from the tagprefix's 双重%s锭 templates; the ledger's first-listed
    # option wins). crushedCentrifuged(+Tiny) KEEP 离心 — in sync with the tagprefix rows,
    # the en "Refined Ores" inconsistency is an en-side report (ledger §4 #4). toolHeadBuilderwand
    # keeps 建筑之杖帽 (en "Caps" vs the tagprefix "Cap" — already faithful).
    ("itemgroup", "casingSmall"): "物品外壳",               # en "Item Casings" — the missing qualifier
    ("itemgroup", "chemtube"): "玻璃试管",                  # en "Glass Tubes" — the missing glass word
    ("itemgroup", "rockGt"): "岩石",                        # en "Rocks" — 石头 collides with stone
    ("itemgroup", "oreRaw"): "生矿石",                      # en "Raw Ores" — the missing raw word
    ("itemgroup", "crushedPurified"): "洗净矿石",           # unified with tagprefix.crushed_purified 洗净%s矿
    ("itemgroup", "crushedPurifiedTiny"): "小撮洗净矿石",   # unified with tagprefix.crushed_purified_tiny
    ("itemgroup", "toolHeadSense"): "镰刀刃",               # 镰刀片 vs the tagprefix 刃 — unified on 刃 (en "Sense Blades")
    ("itemgroup", "toolHeadPickaxeGem"): "镶尖镐头",        # unified tipped=镶尖 with the tagprefix %s镶尖镐头
    ("itemgroup", "stick"): "杆",                           # 杆/棍 slash pair — 棍 dropped (the tagprefix is %s杆)
    ("itemgroup", "stickLong"): "长杆",                     # same unification
    ("itemgroup", "dustDiv72"): "1/72小撮粉末",             # the verbose 七十二分之一 spelling (the tagprefix is 1/72%s粉)
    ("itemgroup", "ingotDouble"): "双重锭",                 # the 2x faces join the tagprefix number words
    ("itemgroup", "ingotTriple"): "三重锭",
    ("itemgroup", "ingotQuadruple"): "四重锭",
    ("itemgroup", "ingotQuintuple"): "五重锭",
    ("itemgroup", "plateDouble"): "双重板",
    ("itemgroup", "plateTriple"): "三重板",
    ("itemgroup", "plateQuadruple"): "四重板",
    ("itemgroup", "plateQuintuple"): "五重板",
    # -- §3 P2 material names (29 rows) --
    ("material", "Basalz"): "岩石元素",                     # TE element family aligns on 元素 (岩石 collides with stone=石头)
    ("material", "Aerotheum"): "气元素",                    # 和谐之空气 unattested machine-ese
    ("material", "Petrotheum"): "地元素",                   # 构造之地岩 machine-ese
    ("material", "Cryotheum"): "冰元素",                    # 凛冰 renamed for the family face (ledger: 同族一并对齐)
    ("material", "Pyrotheum"): "炎元素",                    # same
    ("material", "Chert"): "硅质岩",                        # de-collides with flint 燧石 (en Chert vs Flint are distinct)
    ("material", "Dilithium"): "二锂",                      # the Star-Trek community word (双锂 unattested)
    ("material", "Dolamide"): "多拉迈德",                   # the DS9 fictional mineral — phonetic, 酰胺 is a real chemistry suffix
    ("material", "SoylentGreen"): "祖母绿",                 # en "Emerald Green" (the pigment face)
    ("material", "Craponite"): "克拉邦石",                  # the joke name, phonetic over the vulgar literal
    ("material", "Ectoplasm"): "灵外质",                    # 灵气 = aura collision
    ("material", "Honeydew"): "蜜露",                       # the established word
    ("material", "Licorice"): "甘草",                       # the generic word over the over-specific candy face
    ("material", "Rye"): "黑麦",                            # the grain (en "Rye"); 面 = flour, not carried
    ("material", "EnergiumRed"): "红色能量水晶",            # the colour restored (sibling energium_cyan 青色能量水晶)
    ("material", "Prismarine"): "亮海晶石",                 # the vanilla word; 亮色 — (aligns ore_lightprismarine 亮海晶石%s矿)
    ("material", "QuartzBlack"): "黑色石英",                # a gem, not a rock (en "Black Quartz")
    ("material", "Maxixe"): "马克西绿柱石",                 # the colour word is uncarried in en ("Maxixe")
    ("material", "Inolashite"): "影秘蓝金",                 # de-collides from ceruclase 暗影秘银
    ("material", "Valonite"): "瓦罗奈特",                   # phonetic — 法罗钠 carried a stray 钠 element reading
    ("material", "Octine"): "奥克廷",                       # phonetic — 炽炎铁 carried a false iron reading
    ("material", "DeepIron"): "深层铁",                     # 深渊 collides with the abyssal family
    ("material", "ElectrumFlux"): "通量琥珀金",             # task p28-lang-batch1-fix: the TE Fluxed Electrum community form (TeamNED glossary:952 + mcmod GT6/GT6U 148 hits; batch1's 充能 was an Avaritia singularity single-point)
    ("material", "VibrantAlloy"): "脉冲合金",               # task p28-lang-batch1-fix: the EIO community form (mcmod ~241 hits / 10 mods all 脉冲 + TeamNED gt_material:2004; 脉动 single-source unverified)
    ("material", "VividAlloy"): "生动合金",                 # task p28-lang-batch1-fix: the Endergy community form (mcmod 19 hits / 5 mods + the dump column; 鲜艳 was self-coined)
    ("material", "Ludicrite"): "路德里特",                  # phonetic over the over-translated 滑稽
    ("material", "Dreadium"): "恐惧素",                     # the bare abstract 恐惧 gains the substance suffix
    ("material", "Continuum"): "连续体",                    # en "Continuum" (命运 was a pun reading)
    ("material", "WoodScorched"): "烧焦木",                 # scorched = 烧焦 (焦皮枯木 stacked three morphemes)
}


def dump_rows(dump_path: Path):
    """Collect the three families from the dump. First occurrence wins; returns {kind: {source: value}}."""
    collected: dict[str, dict[str, str]] = {kind: {} for _, kind in FAMILIES}
    dropped = 0
    duplicates = 0
    with dump_path.open("r", encoding="utf-8") as handle:
        for line_no, raw in enumerate(handle, 1):
            match = DUMP_LINE.match(raw.rstrip("\n"))
            if not match:
                continue
            key, value = match.group(1), match.group(2)
            if key.startswith(DROP_KEY_PREFIXES):
                dropped += 1
                continue
            for prefix, kind in FAMILIES:
                if key.startswith(prefix):
                    source = key[len(prefix):]
                    table = collected[kind]
                    if source in table:
                        duplicates += 1
                    else:
                        table[source] = value.strip()
                    break
            if line_no == 0:  # pragma: no cover — keeps the loop shape honest
                break
    return collected, dropped, duplicates


def ascii_review(value: str) -> bool:
    """Pure-ASCII values are suspected untranslated dump entries -> status=review."""
    return value.isascii()


# ---------------------------------------------------------------------------
# Affix voting aid (task p23-i18n-zh-442-backfill): replays the evidence behind the
# tagprefix hand layer. For every gt6.tagprefix.* hand row, collect composed-name
# votes for the zh template shape (head + %s + tail) from oredict.<camelPrefix><Pascal>
# rows: the dump is the PRIMARY vote, each --teamned file a RE-VOTE. The material zh
# names come from the dump's gt.material.<Pascal> family; the LONGEST name occurring
# inside a composed value determines the (head, tail) split; the plurality across rows
# is the prefix's template. Purely advisory: the TSV remains a function of the dump +
# HAND_TRANSLATIONS only (ADR §1.1 — zero key logic enters the generation path).
# ---------------------------------------------------------------------------
DUMP_MATERIAL_LINE = re.compile(r"^    S:gt\.material\.([^=]+)=(.*)$")
OREDICT_COMPOSED = re.compile(r"^oredict\.([^.=]+)$")


def _snake_to_camel(snake: str) -> tuple:
    parts = snake.split("_")
    camel = "".join(p[:1].upper() + p[1:] for p in parts)
    return camel, camel[:1].lower() + camel[1:]


def load_votes(dump_path: Path, teamned_paths=()):
    """Parse the dump (gt.material + oredict faces) and the optional TeamNED files."""
    zh_names: list[str] = []
    faces: list[dict[str, dict[str, str]]] = []
    for source in (dump_path, *teamned_paths):
        oredict: dict[str, str] = {}
        with source.open("r", encoding="utf-8", errors="replace") as handle:
            for raw in handle:
                match = DUMP_LINE.match(raw.rstrip("\n"))
                if not match:
                    continue
                key, value = match.group(1), match.group(2).strip()
                material = DUMP_MATERIAL_LINE.match(raw.rstrip("\n"))
                if material and source == dump_path:
                    if value and not value.isascii() and value not in zh_names:
                        zh_names.append(value)
                    continue
                composed = OREDICT_COMPOSED.match(key)
                if composed:
                    oredict[composed.group(1)] = value
        faces.append(oredict)
    zh_names.sort(key=len, reverse=True)
    return faces, zh_names


def vote_tagprefixes(faces, zh_names):
    """Vote every tagprefix hand row; returns {snake: [per-face [(template_str, count)]]}."""
    rVotes: dict[str, list] = {}
    for key, (value, status) in HAND_TRANSLATIONS.items():
        if not key.startswith("gt6.tagprefix.") or status != "hand":
            continue
        snake = key[len("gt6.tagprefix."):]
        per_face = []
        for oredict in faces:
            votes: dict[tuple, int] = {}
            for camel_form in _snake_to_camel(snake):
                for okey, oval in oredict.items():
                    if not okey.startswith(camel_form):
                        continue
                    rest = okey[len(camel_form):]
                    if not rest or rest[0].islower() or oval.isascii():
                        continue
                    for name in zh_names:
                        idx = oval.find(name)
                        if idx >= 0:
                            slot = (oval[:idx], oval[idx + len(name):])
                            votes[slot] = votes.get(slot, 0) + 1
                            break
            ranked = sorted(votes.items(), key=lambda kv: -kv[1])[:2]
            per_face.append([(h + "{%s}" + t, n) for (h, t), n in ranked])
        rVotes[snake] = per_face
    return rVotes


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", required=True, type=Path,
                        help="path to the GT6 1.7.10 zh lang dump (tmp/gregtech.lang)")
    parser.add_argument("--out", type=Path,
                        default=Path(__file__).resolve().parent.parent / "src/main/resources/gregtech6/lang/zh_cn_ref.tsv",
                        help="output TSV path (default: the shared resources tree)")
    parser.add_argument("--vote", action="store_true",
                        help="print the tagprefix affix votes (primary dump + re-votes) instead of writing the TSV")
    parser.add_argument("--teamned", type=Path, action="append", default=[],
                        help="extra TeamNED oredict lang file for the affix re-vote (repeatable)")
    args = parser.parse_args(argv)

    if not args.dump.is_file():
        parser.error(f"dump not found: {args.dump}")
    for teamned in args.teamned:
        if not teamned.is_file():
            parser.error(f"teamned re-vote file not found: {teamned}")

    if args.vote:
        faces, zh_names = load_votes(args.dump, args.teamned)
        votes = vote_tagprefixes(faces, zh_names)
        face_names = ["dump(primary)"] + [f"teamned{i + 1}(re-vote)" for i in range(len(faces) - 1)]
        for snake in sorted(votes):
            cell = " | ".join(
                f"{name}:{rows}" for name, rows in zip(face_names, votes[snake])
            )
            print(f"{snake}\t{cell}")
        return 0

    collected, dropped, duplicates = dump_rows(args.dump)

    # Family-override layer (task p27-lang-fix): applied AFTER dump collection so the
    # TSV remains a function of dump + hand tables (ADR §1.1). A missing dump row is a
    # hard error — the layer REFINES existing rows, it never invents keys.
    for (_kind, _source), _value in FAMILY_OVERRIDES.items():
        if _kind not in collected or _source not in collected[_kind]:
            sys.exit(f"family override ({_kind}, {_source}): no dump row to override")
        if "\t" in _value or "\n" in _value:
            sys.exit(f"family override ({_kind}, {_source}): value contains tab/newline")
        collected[_kind][_source] = _value

    # Validate the hand layer up front: one %s slot for template keys, no tabs/newlines anywhere.
    for key, (value, status) in HAND_TRANSLATIONS.items():
        if value is None:
            continue  # review rows may be empty — the provider skips them
        if "\t" in value or "\n" in value:
            sys.exit(f"hand row {key}: value contains tab/newline")
        if key.startswith("gt6.tagprefix.") and status == "hand" and value.count("%s") != 1:
            sys.exit(f"hand row {key}: tagprefix template must contain exactly one %s slot, got {value!r}")

    lines: list[str] = [
        "# zh_cn reference table — GENERATED by mdk/tools/gen_zhcn_ref.py, git-tracked data source",
        "# for the GT6ZhCn datagen provider (ADR 2026-09-06-p20-i18n-zhcn-pipeline §1.1).",
        "#",
        "# columns: kind<TAB>source<TAB>value<TAB>status",
        "#   kind   material | itemgroup | mte | direct",
        "#   source family rows: the dump key suffix verbatim (material Pascal internal / itemGroup",
        "#          camel internal / mte numeric id) — the gt6 lang key is derived provider-side by",
        "#          the single MaterialPrefixItem.snakeCase implementation, never here;",
        "#          direct rows: the FINAL gt6 lang key (hand-entered in the script's table).",
        "#   status auto   = dump copy with a translatable value;",
        "#          review = pure-ASCII dump value, suspected untranslated — provider skips;",
        "#          hand   = human translation (script HAND_TRANSLATIONS table + the",
        "#                   task p27-lang-fix FAMILY_OVERRIDES family-row layer).",
        f"# filters: dropped {'/'.join(p.rstrip('.') for p in DROP_KEY_PREFIXES)} keys; duplicate keys: first occurrence wins.",
        "#   mte rows are inert census data until the MTE ID-join subface lands (arch tasks.p20-arch-i18n).",
        "#",
    ]

    counts: dict[str, int] = {}
    for kind in ("direct", "material", "itemgroup", "mte"):
        rows: list[tuple[str, str, str]] = []
        if kind == "direct":
            for key, (value, status) in sorted(HAND_TRANSLATIONS.items()):
                if value is None:
                    continue
                rows.append((key, value, status))
        else:
            for source, value in sorted(collected[kind].items()):
                if (kind, source) in FAMILY_OVERRIDES:
                    rows.append((source, value, "hand"))  # overridden value = human-authored (task p27-lang-fix)
                else:
                    rows.append((source, value, "review" if ascii_review(value) else "auto"))
        counts[kind] = len(rows)
        for source, value, status in rows:
            lines.append(f"{kind}\t{source}\t{value}\t{status}")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")

    review_family = sum(1 for kind in ("material", "itemgroup", "mte") for v in collected[kind].values() if ascii_review(v))
    print(f"wrote {args.out}")
    print(f"  direct={counts['direct']} (hand) material={counts['material']} itemgroup={counts['itemgroup']} mte={counts['mte']}")
    print(f"  family review (skipped by provider): {review_family} {REVIEW_NOTE}")
    print(f"  dump housekeeping: dropped={dropped} duplicate-keys={duplicates}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
