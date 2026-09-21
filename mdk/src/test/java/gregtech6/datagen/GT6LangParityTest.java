/**
 * Offline guard test for task p20-i18n-zhcn-provider (ADR 2026-09-06-p20-i18n-zhcn-pipeline
 * §1.3): the zh_cn key set is structurally pinned to the en_us key set, the not-yet-composed
 * domains are pinned ABSENT from zh until their B-wave cards land, and the zh coverage floor
 * is a ratchet. Same posture as GT6EnUsJeiInfoTest:52-64 — a recording LanguageProvider
 * subclass (add is public and non-final) captures every entry addTranslations() would emit,
 * in a bare JVM, no datagen run needed (the generated files themselves are gated by runData
 * + the second-run written:0 check).
 *
 * <p>Task p20-i18n-compose-wires FLIPPED the B1 half of the guard: the wire + conveyor/
 * robot-arm domains are now asserted COMPOSED on BOTH sides (no pre-installed full-string
 * key survives on en or zh outside the three atomic exemptions, and the template keys exist
 * with their argument slots in both locales) — the A-wave "absent from zh" form is gone for
 * that domain, the B2 assertions remain.
 *
 * <p>Task p23-i18n-material-fill-fix: the two {@code %s} fill points (MaterialPrefixItem /
 * GTMaterialPrefixBlockItem getName) fill the gt6.tagprefix.* templates with the
 * {@code gt6.material.<snake>} translatable small unit instead of the raw mNameLocal English
 * literal — the root fix that lets the 1769 gt6.material.* zh keys get consumed at runtime
 * (「青铜锭」 instead of "Bronze锭"). The pins below guard the fill seam: key construction,
 * single-slug consistency with the en walk, en-face equivalence (en display unchanged) and
 * the registry-pair keyface guard (both registration universes vs the en recording face).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.stone.StoneVariant;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GT6Attachments;
import gregtech6.registry.GT6Boilers;
import gregtech6.registry.GT6DynamoHousings;
import gregtech6.registry.GT6Turbines;
import gregtech6.registry.GT6BurningBoxes;
import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6ElectricDynamos;
import gregtech6.registry.GT6ElectricTransformers;
import gregtech6.registry.GT6FeBatteries;
import gregtech6.registry.GT6FeConverters;
import gregtech6.registry.GT6FluxDynamos;
import gregtech6.registry.GT6FoamBlocks;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Kitchen;
import gregtech6.registry.GT6Lasers;
import gregtech6.registry.GT6Logistics;
import gregtech6.registry.GT6MagicAbsorbers;
import gregtech6.registry.GT6Molds;
import gregtech6.registry.GT6Placeables;
import gregtech6.registry.GT6QuantumEnergizers;
import gregtech6.registry.GT6Sensors;
import gregtech6.registry.GT6StaticStorages;
import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.registry.GTBarrels;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GTEnergySources;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTGrassBlocks;
import gregtech6.registry.GTItemPipes;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GT6Tanks;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GTWires;
import gregtech6.registry.GTWireSpecs;

public class GT6LangParityTest {

	/**
	 * The zh_cn coverage floor — a RATCHET: it may only be raised (by an explicit PR changing
	 * this constant, with the new measured key count in the message), never lowered. Initial
	 * value per the task card: material ~1770 + tabs ~110 + tagprefix ~102 + misc ~17.
	 * Task p20-i18n-compose-rows (the B2 closeout): raised to the measured 2089 — the zh
	 * face now carries the B2 template/unit faces (16 stone variants + 62 rows units + the
	 * four atomic rows) on top of the A/B1 waves.
	 *
	 * <p>Task p24-micro-fixes-2 (the P24 micro card): raised to the measured 2550 — P23 left
	 * it at 2531 while the dye-chemical (+17, p24-dye-chemical-fluids) and empty-can/tool
	 * (+2, 3870458a) lang rows landed on main. Both committed generated faces count exactly
	 * 2550 keys with zero set difference (zh=en zero-debt contract holds). Rebase
	 * reconciliation (the S2 merge session): the p24-canner-machine landing added the +5
	 * Canner keys (the family template + the four VN row units), so the measured total on
	 * the merged main is 2555 — the ratchet follows the measurement.
	 *
	 * <p>Task p24-lightning-rod: raised to the measured 2571 — the Lightning Rod family
	 * added the +16 keys (the controller + the three atomic part names + the nine
	 * structure/efficiency tooltip lines + the two composed energy lines, both locales,
	 * zh values from the reference table's hand rows). Both committed generated faces
	 * count exactly 2571 keys with zero set difference.
	 *
	 * <p>Rebase reconciliation (the S4 merge session): the p24-screwdriver-item landing
	 * (01a1096e) added the +1 item.gt6.screwdriver row, so the measured total on the
	 * merged main is 2581 — the ratchet follows the measurement.
	 *
	 * <p>Task p25-tool-hammer-wrench: raised to the measured 2583 — the hammer + wrench
	 * pair added the +2 item.gt6.hammer/item.gt6.wrench rows (both locales, the zh
	 * values from the reference table's hand rows). Both committed generated faces count
	 * exactly 2583 keys with zero set difference.
	 *
	 * <p>Task p25-c-foam-pipe-spray: raised to the measured 2617 — the C-Foam spray family
	 * added the +35 keys (32 item names + the foam_sprays tab + the 2 tooltip templates,
	 * both locales, zh values from the dump rows tmp/gregtech.lang:9350-9431 + hand).
	 * (Main's committed faces measured 2582 — one above the S4 note's 2581; the ratchet
	 * follows the measurement: 2582 + 35 = 2617.)
	 *
	 * <p>Rebase reconciliation (the S1 merge session): the hammer +2 rows and the C-Foam
	 * +35 rows both ride the 38365b89 baseline (measured 2582), so the measured total on
	 * the merged main is 2584 + 35 = 2619 — the ratchet follows the measurement.
	 *
	 * <p>Task p25-food-can-row0: raised to the measured 2594 on its own branch — the row0
	 * food-can subset added the +10 rows (item.gt6.food_can_empty + the six
	 * item.gt6.food_can_rotten_* tiers + item.gt6.food_can_cookies_huge +
	 * item.gt6.bending_cylinder_small + the itemGroup.gt6.food_cans tab title; both
	 * locales, the zh values from the reference table's hand rows).
	 *
	 * <p>Rebase reconciliation (the S2 merge session): the C-Foam +35 rows landed first
	 * (dd259287/41e5049f), so the merged faces measure 2619 + 10 = 2629 — the ratchet
	 * follows the measurement. Task p26-c-foam-fluid-refill adds 33 hand rows (the
	 * gt6:cfoam base + the 32 C-Foam fluid display names, tmp/gregtech.lang:130-161/:361),
	 * measuring 2629 + 33 = 2662.
	 *
	 * <p>Task p26-c-foam-block-family: raised to the measured 2666 — the C-Foam block
	 * family added the +4 block.gt6 rows (cfoam/cfoam_fresh + the two slabs, both locales,
	 * the zh values from the reference table's hand rows). Both committed generated faces
	 * count exactly 2666 keys with zero set difference.
	 *
	 * <p>Task p27-machine-energy-display-fix: lowered to the measured 2663 — the SPEC retires
	 * the three gt6.row.tier.2/3/4 ordinal units (both locales, the tier slot now rides the
	 * Kinetic_T material words, already-present gt6.row.mat.* keys — a name-face correction
	 * to the upstream "Shredder ("+aMat.getLocal()+")" caliber, not a coverage regression;
	 * the only ratchet movement this task card will ever make downward).
	 *
 * <p>Task p27-lang-fix: lowered to the measured 2662 — the SPEC retires the kitchen
 * creative tab (itemGroup.gt6.kitchen, both locales, -1 key; the user ruling kept NO
 * renamed successor — the four pot/bowl/clay items ride the machines tab through the
 * GT6Kitchen BuildCreativeModeTabContentsEvent join). A registration/creative-tab
 * retirement, not a coverage regression — the display-name keys all stay. The zh VALUE
 * fixes of that card (the P0/P1 ledger rows + the B4/B5 rules) are value-only and
 * move no counts.
 *
 * <p>Task p27-lang-fix-batch2: raised to the measured 2793 — the SPEC closes the ledger §6
 * zh gap: the +70 p26 mold/crucible/faucet chain faces (32 mold displays + 31 raw clay
 * molds + the raw faucet + the smeltery trio + the faucet template/mat words) ride the new
 * addMoldCrucibleUnits walk with dump-verbatim values (tmp/gregtech.lang:10026-10092/
 * :10102/:10104/:10772/:10893/:10895/:11199/:11224). zh == en, the zero-debt state. The en
 * recording face now replays the mold chain (GT6MoldDatagen.Lang is final), so the zh ⊆ en
 * subset assertion covers the chain too. The ~79 zh VALUE fixes of this card (the P2 ledger
 * rows + the ultimet unification) are value-only and move no counts.
 *
 * <p>Task p28-cfoam-lang-key: raised to the measured 2796 — the +1 block.gt6.cfoam_owned
 * row (the SPEC target) PLUS the +2 block.gt6.test_machine[_idle] rows the new
 * registry-coverage gate lit up (the same whole-string-key-block omission class, fixed
 * same-shape per the SPEC's staged discipline — both locales, zh values from the reference
 * table's hand rows). The 17 zh VALUE renames of that card (the owned cfoam block/fluid
 * family, user ruling 2026-09-12; the same-day follow-up ruling unified the 16 owned
 * spray items onto the same word root) are value-only and move no counts.
 *
 * <p>Task p28-crucible-jade-face: +4 more — the crucible Jade face keys
 * (gt6.jade.crucible.temperature/total/empty/more, both locales; the zh values ride
 * the reference table's hand rows). Rebased onto the cfoam state the re-measured floor
 * is 2796 + 4 = 2800 (the branch-local 2793+4=2797 estimate preceded the merge; the
 * rebased re-measure is authoritative). zh == en, the zero-debt state holds.
 *
 * <p>Task p28-c-ulv-machine-ladder: raised to the measured 2805 — the +5 ULV ladder keys
 * (the Shredder/Crusher one-slot display templates + the Rolling Mill family template +
 * the any_wood/ulv row-mat units, both locales; the zh values ride the reference table's
 * hand rows — 辊压机 = the dump gt.multitileentity.20111 column, 木 = the dump
 * gt.material.AnyWood row, ULV = the VN[0] id). zh == en, the zero-debt state holds.
 *
 * <p>Task p28-c-ulv-dynamo-row: raised to the measured 2816 — the +11 dynamo block keys
 * (the W1 flux/electric families' deferred name faces + the T0 ULV row, both locales;
 * the electric values are the dump faces verbatim gt.multitileentity.10111-10115 =
 * 发电机 (LV..IV), the flux the :10997-11001 红石通量发电机 faces, the T0 row the
 * 发电机 (ULV) hand row). The same card walks BOTH dynamo registration classes into the
 * registry-coverage gate (the W1 handoff's must-do leftover, the cfoam lesson): all
 * eleven blocks resolve the vanilla descriptionId, so checked 90 → 101.
 *
 * <p>Task p29-w2-eu-special: raised to the measured 2842 — the +3 eu-special family
 * templates (gt6.row.autocrafter.display / gt6.row.lightningprocessor.display /
 * gt6.row.laminator.display, both locales, the en==zh==2842 zero-delta state).
 *
 * <p>Task p29-w2-energy-types-5tier: raised to the measured 2839 — the +1 gt6.row.mat.iv
 * voltage rung (VN[5] = "IV", CS.java:154; the card gloss said EV, refuted — ev is
 * already T4's word; the GTMachines.EV_TIER_INPUTS doc carries the erratum), both
 * locales. The re-measure also landed the en-side 71-key replay union (the mold/faucet/
 * crucible display keys the GT6MoldDatagen.Lang chain below GT6EnUs emits whenever it
 * registers last — the deterministic datagen output this card's runData stabilized; the
 * previously committed en file lacked the replay keys while zh carried them, so en == zh
 * 2839 = the first ZERO both-way delta state). zh == en, the zero-debt state holds.
 *
 * <p>Task p29-w2-eu-core-5tier: raised to the measured 2844 — the +5 eu-core family
 * display templates (Electrolyzer/Injector/Printer/Scanner (Visuals)/Slicer over the
 * VN[1..5] = LV/MV/HV/EV/IV ladder, the dump words 电解器/流体灌装机/打印机/扫描仪/
 * 切片器, gt.multitileentity.20091/20261/20271/20281/20381), both locales.
 *
 * <p>Task p29-w2-hu-tu-piggyback: raised to the measured 2867 — the +7 hu-tu keys
 * (gt6.row.machine.steam_cracker / catalytic_cracker / loom one-slot unit keys +
 * gt6.row.coagulator / generifier / bath / autoclave atomic display keys, the dump
 * words 蒸汽裂解器/催化裂解器/织布机/凝结器/转换器/浸洗器/高压釜), both locales; zh == en,
 * the zero-debt state holds.
 *
 * <p>Task p29-w3-heat-smelter: raised to the measured 2888 — the +2 heat-family keys
 * (gt6.row.machine.smelter one-slot unit key + gt6.row.melter.display atomic display,
 * the dump words 液化炉 (钢/殷钢/钛/碳化钨) at gt.multitileentity.20241-20244 + the
 * hand-translated 熔化炉 for the ASCII 22010 "Melter" dump row) + block.gt6.large_heat_exchanger
 * (大型热交换器, the dump face verbatim), both locales. The
 * re-measure also replayed the en-side mold/faucet union keys onto the committed en file
 * (the GT6MoldDatagen.Lang registration-order replay — the W2 note), so this card's
 * runData lands en == zh 2888, the zero-delta state again (the committed zh face led by
 * one stray key at the card-① merge — this card's floor absorbs it).
 *
 * <p>Task p29-w5-t8-armor-24: raised to the measured 3101 — the +48 Hazmat armor keys
 * (24 item.gt6.hazmat_* display names + the 24 per-piece .tooltip keys, the dump faces
 * verbatim tmp/gregtech.lang:927-974 via the ARMOR_BACKFILL hand layer; all both
 * locales). zh == en, the zero-debt state holds.
 */
private static final int ZH_KEY_FLOOR = 3651; // +10 (task p33-food-tail: the 10 card-block-outside drink-fluid display names — the golden-apple brews Loader_Fluids.java:607-610 + the coffee family :637-642; the dump carries 9 S:fluid.potion.* faces verbatim (清咖啡/欧蕾黑咖啡/欧蕾咖啡/法式牛奶咖啡/苦味巧克力牛奶/金苹果汁/金苹果酒/伊顿苹果汁/炖Notch), plain potion.coffee is the one hand row 咖啡; all both locales; 3641 -> 3651) // 3538 +103 (task p33-food-fluids-b2: the 103 drink-seam fluid display names — 94 potion brews + 9 residual FOOD-flag drink fluids, the dump carries 102 S:fluid.* faces verbatim + the poison hand row 毒液; all both locales) +1 (task p33-food-machines-kitchen: block.gt6.juicer, the dump gt.multitileentity.32722 face 榨汁机 at tmp/gregtech.lang:13543; both locales) // 3321 +216 (task p33-food-fluids-b1: the 216 food-fluid display names — the dump carries 153 S:fluid.* faces verbatim + the 67 binnie-external/derived hand rows in the reference table; all both locales) // 2839 +3 (task p29-w2-eu-special) +13 (task p29-w2-exotic-energy: the 6 exotic templates + the galvanized_steel/aluminium/t1..t5 units) +5 (task p29-w2-eu-core-5tier) +7 (task p29-w2-hu-tu-piggyback: the 3 one-slot unit keys + the 4 TU atomic display keys) +18 (task p29-w3-nbtdesign-parts: the metal_wall template + the 17 atomic part-family names; all both locales) +1 (the merged-main 4f68532a face) +7 (task p29-w3-tank-valves: the two valve templates + the four size words + the wood unit; all both locales) +12 (task p29-w3-turbine-dynamo: the twelve controller names; all both locales) +9 (task p29-w3-distill-crucible: the 7 crucible ladder rungs + the twin towers; both locales — the ladder walls compose over the EXISTING gt6.row.mat words, zero new keys) +12 (task p29-w3-large-12: the 12 large-machine display names; all both locales +1 (task p29-w3-heat-smelter: block.gt6.large_heat_exchanger, the dump word 大型热交换器 at gt.multitileentity.17197; both locales) +25 (task p29-w4-f1-chemicals: the 25 chemical fluid display names — the oils + hydrocarbons + gas closure + liquidoxygen + the two plasmas; the dump carries every face, all both locales) +61 (task p29-w4-battery-storage: the 37 battery display names + the 5 Filled cells + the 7 circuit carriers + the 12 BatteryBox display names; all both locales) +22 (task p29-w4-hot-lube: the 20 hot-family fluid display names (12 hot + 7 closure carriers + lubricant) + the Lubricant Bucket item + its tooltip; all both locales) +6 (task p29-w5-t1-dig-six: the six dig-tool display names Pickaxe/Gem tipped Pickaxe/Construction Pick/Shovel/Spade/Universal Spade; all both locales — the zh faces ride the dump tagprefix head words minus 头) +1 (task p30-pool-drillhead-18103: block.gt6.bedrock_drill_head, the dump word 基岩钻头 at mte 18103; both locales) +10 (task p29-w5-t2-blade-six: the six blade-tool display names Sword/Knife/Butchery Knife/Club/Axe/Double Axe — the zh faces of sword/knife/axe ride the dump gt.lang.tool.name rows verbatim, butchery knife/club/double axe are the hand words the dump lacks; plus the four registration-desc tooltips, hand zh — the dump gt.lang.tool.tooltip rows are a mismatched build) +4 (task p29-w5-t3-machine-face-four: the four machine-face display names Soft Hammer/Monkey Wrench/Magnifying Glass/Pincers; both locales — the zh faces ride the dump gt.metatool.01 rows :14/:52/:62/:66) +5 (task p29-w5-t4-field-five: the five field-tool display names Hoe/Branch Cutter/Sense/Plow/Hand Drill; all both locales — the zh faces ride the dump tagprefix head words minus 头/刃 for hoe/sense/plow, the hand rows 修枝剪/手钻 for the two dump-absent ids) +6 (task p29-w5-t5-scene-six: the six scene-tool display names Scissors/Scoop/Plunger/Flint and Tinder/Rolling Pin/Bending Cylinder; all both locales — the zh faces ride the dump words 剪刀/捕虫网/搋子/打火石/擀面杖/弯曲绕筒) +33 (task p29-w5-t6-electric-nineteen: the 19 electric-tool display names + the 14 tooltips (13 row tooltips + the mode_switch line); all both locales — the zh faces are hand words over the existing dump roots, the upstream dump carries zero electric rows, provenance p29-w5-t6-electric-nineteen+hand33) +15 (task p29-w5-t7-pocket-eight: the 8 pocket-form display names + the 7 mode-switch tooltips; all both locales — the zh faces are hand words, the upstream dump carries zero pocket rows, provenance p29-w5-t7-pocket-eight+hand15) +48 (task p29-w5-t8-armor-24: the 24 hazmat display names + the 24 per-piece tooltip keys; both locales) +27 (task p30-w6-t1-trees-nine: the 27 tree-family block names — 9 saplings + 9 logs + 9 leaves; all both locales, the hand zh rows) +1 (task p30-ore-3-datagen: itemGroup.gt6.ore_vanillastone, the dump itemgroup oreVanillastone row 矿石; both locales — the ore universe's ONLY new lang key, the 27 ore template keys predate this card) +8 (task p30-w6-t2-surface-blocks: the 8 surface-plant/fallen-log block names — Glowtus/Berry Bush/Black Sand/Turf + Dead/Rotten/Mossy/Frozen Log; all both locales, hand zh rows 荧光莲/浆果丛/黑沙/草皮/枯原木/腐朽原木/苔藓原木/冰冻原木 +1 (task p30-pool-prospector: item.gt6.hammer.tooltip_prospector, the CS.java:1154 prospector second-behavior tooltip; both locales — en verbatim, zh hand 勘探周围的矿石 over the mismatched-build dump tooltip family, the t2 ruling) +3 (task p31-qu-a-foundation: the 3 QU-matter fluid display names chargedmatter/neutralmatter/enderpearl_molten; both locales — hand zh rows 带电物质/中性物质/熔融末影珍珠, no upstream dump face) +32 (task p31-bees-lv1: the 20 comb display names + the Bees tab title + the 11 honey-family/bee-row fluid display names; all both locales — hand zh rows 蜜脾 family over the dump 黄蜂蜂巢 hive face, the fluid words reuse the dump material faces 蜂蜜/蜜露/胶乳) +1 (task p31-graagg: block.gt6.von_da_graagg, the Loader :1280 name column; en verbatim, zh hand 冯·达·格拉格发电机 — the upstream dump carries zero Graagg rows; both locales) +9 (task p31-qu-b-materials: the 9 isotope-batch chemical fluid display names deuterium/tritium/helium3 + the 6 molten rows lithium6/beryllium7/beryllium8/boron11/carbon13/ancientdebris; all both locales — the zh faces are the dump S:fluid.* rows verbatim 氘/氚/氦-3/熔融锂-6/熔融铍-7/熔融铍-8/熔融硼-11/熔融碳-13/熔融远古残骸) +2 (task p31-massfab: block.gt6.large_massfab the Loader :1241 name column — en verbatim, zh hand 大型物质制造机 — plus the gt6.row.machine.massfab tier-word template 物质制造机 (%s) over the EXISTING t1..t5 units; both locales) +10 (P31 tail reconciliation, task p32-hygiene-lang-assets: re-measured at the P31 closeout state — the fusion bump itself landed on a 3269-key tree (the arithmetic under-counted the walks by 5), then e7b7dad5b added block.gt6.fusion_reactor + the 4 molten fluid rows adamantium/carbon/lithium/tungsten with no bump; main 4fe176ca5 measures 3274, parity en==zh 3274/0 both ways; the P32 handoff card cites 3267, the measured tree is authoritative and 3274 > 3267 — the floor only ever goes UP) +14 (task p32-qu-laser-domain: the 10 laser-domain block names CO2 Laser/Laser Absorber + the 2 gas emitter items + the 2 tooltips; all both locales — the zh faces are the dump gt.multitileentity.10101-10105/:10151-10155/:10379/:10395 rows verbatim, the tooltips the :384/:394 description hand rows) +1 (task p32-logistics-lv2: block.gt6.logistics_wire, the dump row tmp/gregtech.lang:11847 物流线缆 — both locales one key, the zh face rides the tsv direct band; review seam: the floor ratchet beside the provider rows the card landed without the committed-face regen +1 (task p32-magic-absorber: block.gt6.magic_absorber, the dump :10891 face 魔法力场吸收器; both locales) +5 (task p32-qu-energizer: block.gt6.quantum_energizer[_t2.._t5], the dump gt.multitileentity.10121-10125 rows 量子充能器 (T1..T5) verbatim; both locales) +8 (task p32-placeables: block.gt6.greg_o_lantern / block.gt6.sandwich / block.gt6.placed_{rock,stick,ingot,plate,gem_plate,scrap}, the hand rows 格雷灯笼/三明治/石头/木棍/锭/板/宝石板/废料; both locales, review seam — the card landed the rows without the floor bump) +1 (task p32-logistics-lv3: block.gt6.logistics_core, the dump mte multiblock.logisticscore row 物流核心 — both locales one key, the zh face rides the tsv auto band; rebase reconciliation over the merged placeables state: 3303 -> 3304) +3 (task p32-qu-scanner-replicator: gt6.row.machine.molecular_scanner / gt6.row.machine.replicator tier-word templates + fluid.gt6.redstone_molten 熔融红石; both locales, rebase reconciliation over the merged 3304 state: 3304 -> 3307) +14 (task p33-logistics-covers-12: the 14 logistics cover item display names — 4 CPU displays + the filtered fluid/item trios + the generic trio + the dump; both locales, the zh faces are the upstream tmp/gregtech.lang 1086-1099 name-column hand words appended to the tsv direct band: 3307 -> 3321)
	/**
	 * A-wave negative assertions (ADR §1.3): the COMPOSED domains stay absent from zh — those
	 * en keys are pre-installed full strings that the B-wave cards replace with template keys;
	 * every card SHRINKS its assertion as it lands, until the list is empty. B1
	 * (p20-i18n-compose-wires) has landed: its four entries are gone from this list and now
	 * live under {@link #WIRES_COMPOSED_PREFIXES} in the FLIPPED both-sides form. Substring
	 * form: every entry is anchored by its {@code block.gt6.}/{@code item.gt6.} namespace so
	 * the gt6.tagprefix./gt6.material. small-unit faces can never false-positive.
	 */
	private static final List<String> COMPOSED_DOMAIN_KEYS = List.of(
		// task p20-i18n-compose-rows CLOSEOUT: the B2 card landed its composed faces (the
		// stone templates + every rows family), so the list is EMPTY — the closeout gate.
		// Any future composed domain must ADD its prefixes here while the pre-installed
		// full strings still exist, and REMOVE them when its template face lands.
		);

	/**
	 * The FLIPPED B1 guard (task p20-i18n-compose-wires): these prefixes must match NO
	 * pre-installed full-string key on EITHER locale side — the names compose at runtime
	 * (GTWireBlock.displayNameOf / the GT6Covers cover templates) from the gt6.wire.* and
	 * gt6.cover.* template keys.
	 */
	private static final List<String> WIRES_COMPOSED_PREFIXES = List.of(
		"block.gt6.wire_",
		"block.gt6.cable_",
		"item.gt6.cover_conveyor_",
		"item.gt6.cover_robot_arm_");

	/**
	 * The B1 atomic exemptions — the material-less forms that keep whole-string keys on both
	 * faces: the two p7 legacy electric blocks (no row identity to compose from) and the
	 * laser family (material-less, Loader:1815 verbatim).
	 */
	private static final Set<String> WIRES_ATOMIC_KEYS = Set.of(
		"block.gt6.wire_electric_1x",
		"block.gt6.wire_electric_2x",
		"block.gt6.wire_laser");

	/** The B1 template keys and their argument-slot counts (display=3: size/material/form; plain=2; the cover templates=1; the form units are slot-less NOUNS the templates consume). */
	private static final Map<String, Integer> WIRES_TEMPLATE_SLOTS = Map.of(
		"gt6.wire.display", 3,
		"gt6.wire.display.plain", 2,
		"gt6.wire.form.wire", 0,
		"gt6.wire.form.cable", 0,
		"gt6.wire.form.wirelamp", 0,
		"gt6.cover.conveyor.display", 1,
		"gt6.cover.robot_arm.display", 1);


	private static Map<String, String> enEntries;
	private static Map<String, String> zhEntries;

	@BeforeAll
	public static void boot() {
		// the walks load Item subclasses (snakeCase) and the full OP/material registries —
		// vanilla bootstrap first (GT6EnUsJeiInfoTest.boot shape; offline-expected throwables)
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline-expected
		}
		GTMaterialItems.initMaterials();
	}

	/** Records every add() the provider makes (duplicate key = hard failure, as in the jei test). */
	private static Map<String, String> collect(boolean aZh) {
		Map<String, String> tEntries = new HashMap<>();
		PackOutput tOutput = new PackOutput(Path.of("build", "tmp", aZh ? "gt6zhcn-parity-test" : "gt6enus-parity-test"));
		// the recording subclasses live in this package, so the protected addTranslations() is
		// reachable on their exact type (a LanguageProvider-typed variable would not be)
		if (aZh) {
			new GT6ZhCn(tOutput) {
				@Override
				public void add(String aKey, String aValue) {
					recordNew(tEntries, aKey, aValue);
				}
			}.addTranslations();
		} else {
			// task p27-lang-fix-batch2: the en face rides the Lang CHAIN — GT6EnUs alone no
			// longer covers the zh face now that the mold/crucible/faucet gap closed (the zh
			// walk emits the 70 chain keys). GT6MoldDatagen.Lang is FINAL (its add() cannot be
			// intercepted), so the recording subclass takes the GT6CrucibleDatagen.Lang rung
			// (base + crucible + stone-mold via super) and REPLAYS the chain tail from the
			// SAME registry rows the real tail walks (GT6MoldDatagen.Lang:151-161 derivation);
			// the committed en_us.json face itself stays gated by runData two-pass + tree_check.
			new GT6CrucibleDatagen.Lang(tOutput) {
				@Override
				public void add(String aKey, String aValue) {
					recordNew(tEntries, aKey, aValue);
				}

				@Override
				protected void addTranslations() {
					super.addTranslations(); // the full base set + the smeltery trio + the stone mold
					add("gt6.row.mold.display.mold_ceramic", "Ceramic Mold");
					add("item.gt6.mold_ceramic_raw", "Ceramic Mold (Raw)");
					for (GT6Molds.MoldRow tRow : GT6Molds.CERAMIC_ROWS) {
						String tShape = moldShapeName(tRow.path());
						add("gt6.row.mold.display." + tRow.path(), "Ceramic " + tShape + " Mold");
						add("item.gt6." + tRow.path() + "_raw", "Ceramic " + tShape + " Mold (Raw)");
					}
					add("gt6.row.faucet.display", "%s Crucible Faucet");
					add("gt6.row.faucet.mat.stone", "Stone");
					add("gt6.row.faucet.mat.ceramic", "Ceramic");
					add("item.gt6.faucet_ceramic_raw", "Ceramic Crucible Faucet (Raw)");
				}
			}.addTranslations();
		}
		return tEntries;
	}

	/** {@code mold_ceramic_tiny_plate} → {@code Tiny Plate} — the GT6MoldDatagen.shapeName mirror for the replayed en values. */
	private static String moldShapeName(String aPath) {
		String tTail = aPath.substring("mold_ceramic_".length());
		String[] tWords = tTail.split("_");
		StringBuilder r = new StringBuilder();
		for (String tWord : tWords) {
			if (r.length() > 0) r.append(' ');
			r.append(Character.toUpperCase(tWord.charAt(0))).append(tWord.substring(1));
		}
		return r.toString();
	}

	private static void recordNew(Map<String, String> aEntries, String aKey, String aValue) {
		if (aEntries.put(aKey, aValue) != null) {
			throw new IllegalStateException("Duplicate translation key " + aKey);
		}
	}

	private static Map<String, String> en() {
		if (enEntries == null) enEntries = collect(false);
		return enEntries;
	}

	/**
	 * The chained en face for sibling zh-face tests (task p27-lang-fix-batch2: the zh face
	 * now covers the mold/crucible/faucet chain, so the spot-check cardinality pin compares
	 * against THIS recording — a plain GT6EnUs face would measure the pre-gap 2723). Memoized
	 * like {@link #en()}; the boot fixture is identical across the datagen test classes.
	 */
	static Map<String, String> chainedEnFace() {
		return en();
	}

	private static Map<String, String> zh() {
		if (zhEntries == null) zhEntries = collect(true);
		return zhEntries;
	}

	@Test
	public void zhKeysAreASubsetOfEnKeys() {
		List<String> tMissing = new ArrayList<>();
		for (String tKey : zh().keySet()) {
			if (!en().containsKey(tKey)) tMissing.add(tKey);
		}
		assertTrue(tMissing.isEmpty(),
			"zh keys must be a subset of en keys (a TSV join or walk drifted off the en face): " + tMissing);
	}

	/**
	 * The B2 closeout form of the A-wave negative assertion (task p20-i18n-compose-rows):
	 * the list is EMPTY — every composed domain (wires B1, stones + rows B2) now carries
	 * template faces on BOTH sides, asserted by the flipped pins below. The walk stays as
	 * the structural gate for any future composed domain.
	 */
	@Test
	public void composedDomainsStayAbsentFromZh() {
		List<String> tViolations = new ArrayList<>();
		for (String tKey : zh().keySet()) {
			for (String tDomain : COMPOSED_DOMAIN_KEYS) {
				if (tKey.contains(tDomain)) tViolations.add(tKey + " (B-wave domain " + tDomain + ")");
			}
		}
		assertTrue(tViolations.isEmpty(),
			"zh must not carry pre-installed composed-domain strings before the B-wave template"
			+ " conversion lands (each card shrinks its assertion): " + tViolations);
	}

	/**
	 * The FLIPPED B1 guard (task p20-i18n-compose-wires): the wire + conveyor/robot-arm
	 * domains carry NO pre-installed full-string key on EITHER side — the en strings retired
	 * with the B1 shrink, zh never had them — outside the three atomic exemptions (the two
	 * material-less legacy blocks and the laser family, whose descriptionId keys are the
	 * name face on both locales).
	 */
	@Test
	public void wireConveyorDomainsAreComposedOnBothSides() {
		List<String> tViolations = new ArrayList<>();
		Map<String, Map<String, String>> tSides = Map.of("en_us", en(), "zh_cn", zh());
		tSides.forEach((tLocale, tEntries) -> {
			for (String tKey : tEntries.keySet()) {
				for (String tPrefix : WIRES_COMPOSED_PREFIXES) {
					if (tKey.startsWith(tPrefix) && !WIRES_ATOMIC_KEYS.contains(tKey)) {
						tViolations.add(tLocale + ":" + tKey + " (composed domain " + tPrefix + ")");
					}
				}
			}
		});
		assertTrue(tViolations.isEmpty(),
			"the wire/conveyor domains must compose at runtime: no pre-installed full-string key"
			+ " may survive on either side (atomic exemptions only): " + tViolations);
	}

	/**
	 * The B1 template face: every wire/cover template key exists in BOTH locales with its
	 * exact argument-slot count — the parity subset alone would tolerate a zh template whose
	 * slots drifted from the en compose contract (e.g. a lost size slot would render
	 * "12×锡线缆" as "×锡线缆").
	 */
	@Test
	public void wireTemplatesExistWithTheirSlotsOnBothSides() {
		Map<String, Map<String, String>> tSides = Map.of("en_us", en(), "zh_cn", zh());
		tSides.forEach((tLocale, tEntries) -> WIRES_TEMPLATE_SLOTS.forEach((tKey, tCount) -> {
			String tValue = tEntries.get(tKey);
			assertTrue(tValue != null, tLocale + " is missing the B1 template key " + tKey);
			int tSeen = tValue.split("%s", -1).length - 1;
			assertEquals(tCount, tSeen, tLocale + " template " + tKey + " = \"" + tValue + "\" slot count");
		}));
	}

	/**
	 * The FULL-EXPANSION pin (the review round 1 finding: the slot structure alone let the
	 * en template ship as {@code "%sx %s%s"} with bare-noun form units — every composed name
	 * rendered "1x TinWire" and no test noticed). The template + small-unit faces recorded
	 * above are substituted slot-by-slot (the reviewer's programmatic comparison posture) and
	 * pinned EQUAL to the upstream row strings word for word — the space between the material
	 * and the form RIDES THE EN TEMPLATE ({@code "%sx %s %s"}); zh stays the no-space CJK
	 * shape. The B2 rows domain must repeat this pin shape.
	 */
	@Test
	public void wireTemplatesExpandToTheUpstreamStrings() {
		// electric: "%sx %s %s" over (size, gt6.material.<snake>, form unit)
		assertEquals("1x Tin Wire", expandWire(1, "tin", "gt6.wire.form.wire"));
		assertEquals("12x Tin Cable", expandWire(12, "tin", "gt6.wire.form.cable"));
		assertEquals("16x Tungsten Wire", expandWire(16, "tungsten", "gt6.wire.form.wire"));
		// redstone: the size-less plain template, bare Lumium wire = the Wirelamp unit
		assertEquals("Red Alloy Wire", expandWire(0, "red_alloy", "gt6.wire.form.wire"));
		assertEquals("Lumium Wirelamp", expandWire(0, "lumium", "gt6.wire.form.wirelamp"));
		// the covers: the template carries the parens, the tier rides a literal
		assertEquals("Compact Electric Conveyor (LV)", substitute(en().get("gt6.cover.conveyor.display"), "LV"));
		assertEquals("Compact Robot Arm (PUV1)", substitute(en().get("gt6.cover.robot_arm.display"), "PUV1"));
	}

	/**
	 * The B2 template face (task p20-i18n-compose-rows): every stone/rows template key
	 * exists in BOTH locales with its exact argument-slot count (stone = 1 stone-name slot;
	 * the rows templates carry their family slot shapes).
	 */
	@Test
	public void stoneAndRowsTemplatesExistWithTheirSlotsOnBothSides() {
		Map<String, Integer> tSlots = new java.util.LinkedHashMap<>();
		for (StoneVariant tVariant : StoneVariant.VALUES) tSlots.put(tVariant.key(), 1);
		tSlots.put("gt6.row.axle.display", 2);
		tSlots.put("gt6.row.steam_engine.display", 1);
		tSlots.put("gt6.row.steam_engine.display.strong", 1);
		tSlots.put("gt6.row.diesel.display", 1);
		tSlots.put("gt6.row.burning_box.display", 2);
		tSlots.put("gt6.row.burning_box.display.dense", 2);
		tSlots.put("gt6.row.burning_box.display.fluidbed", 1);
		tSlots.put("gt6.row.burning_box.display.fluidbed_dense", 1);
		tSlots.put("gt6.row.boiler.display", 1);
		tSlots.put("gt6.row.boiler.display.strong", 1);
		tSlots.put("gt6.row.dryer.display", 1);
		tSlots.put("gt6.row.distillery.display", 1);
		tSlots.put("gt6.row.large_boiler.display", 1);
		tSlots.put("gt6.row.dense_wall.display", 1);
		tSlots.put("gt6.row.machine.display", 2);
		tSlots.put("gt6.row.machine.shredder.display", 1); // task p28-c-ulv-machine-ladder — the ULV row-carrier templates
		tSlots.put("gt6.row.machine.crusher.display", 1);
		tSlots.put("gt6.row.machine.rolling_mill", 1);
		tSlots.put("gt6.row.machine.roll_bender", 1); // task p29-w1-kinetic-roll-ladder — the roll-ladder one-slot templates
		tSlots.put("gt6.row.machine.roll_former", 1);
		tSlots.put("gt6.row.machine.cluster_mill", 1);
		tSlots.put("gt6.row.tap.display", 1);
		tSlots.put("gt6.row.funnel.display", 1);
		Map<String, Map<String, String>> tSides = Map.of("en_us", en(), "zh_cn", zh());
		tSides.forEach((tLocale, tEntries) -> tSlots.forEach((tKey, tCount) -> {
			String tValue = tEntries.get(tKey);
			assertTrue(tValue != null, tLocale + " is missing the B2 template key " + tKey);
			int tSeen = tValue.split("%s", -1).length - 1;
			assertEquals(tCount, tSeen, tLocale + " template " + tKey + " = \"" + tValue + "\" slot count");
		}));
		// the B2 small-unit faces: slot-less nouns + material words, both sides
		List<String> tUnits = new ArrayList<>();
		for (int tSize = 0; tSize < GT6Kinetics.AXLE_SIZE_NAMES.length; tSize++) tUnits.add(GT6Kinetics.axleSizeUnitKey(tSize));
		tUnits.add(GT6BurningBoxes.FAMILY_SOLID_UNIT_KEY);
		tUnits.add(GT6BurningBoxes.FAMILY_LIQUID_UNIT_KEY);
		tUnits.add(GT6BurningBoxes.FAMILY_GAS_UNIT_KEY);
		// task p27-machine-energy-display-fix: the P7 tier slot rides the Kinetic_T material
		// words (gt6.row.mat.bronze/steel/titanium/tungstensteel — already walked below), the
		// ordinal gt6.row.tier.* units are RETIRED
		tUnits.add(GTMachines.MACHINE_SHREDDER_UNIT_KEY);
		tUnits.add(GTMachines.MACHINE_CRUSHER_UNIT_KEY);
		tUnits.add(GTMachines.MACHINE_LATHE_UNIT_KEY);
		tUnits.add(GTMachines.MACHINE_ROLLING_MILL_UNIT_KEY); // task p28-c-ulv-machine-ladder
		for (GT6Kinetics.AxleSpec tSpec : GT6Kinetics.AXLE_SPECS) tUnits.add(GT6Kinetics.axleMatUnitKey(tSpec));
		for (GT6Kinetics.SteamEngineRow tRow : GT6Kinetics.STEAM_ENGINES) tUnits.add(GT6Kinetics.steamMatUnitKey(tRow));
		for (GT6Kinetics.DieselSpec tSpec : GT6Kinetics.DIESEL_SPECS) tUnits.add(GT6Kinetics.dieselMatUnitKey(tSpec));
		for (GT6BurningBoxes.BurningBoxRow tRow : GT6BurningBoxes.allRows()) tUnits.add(GT6BurningBoxes.matUnitKeyOf(tRow));
		for (GT6Boilers.BoilerRow tRow : GT6Boilers.allRows()) tUnits.add(GT6Boilers.matUnitKeyOf(tRow));
		for (GTMultiBlocks.LargeBoilerRow tRow : GTMultiBlocks.LARGE_BOILER_ROWS) tUnits.add(GTMultiBlocks.boilerMatUnitKeyOf(tRow));
		for (GTMultiBlocks.MultiblockPartRow tRow : GTMultiBlocks.WALL_ROWS) tUnits.add(GTMultiBlocks.wallMatUnitKeyOf(tRow));
		for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) tUnits.add(GT6Attachments.matUnitKeyOf(tRow));
		tSides.forEach((tLocale, tEntries) -> {
			for (String tUnit : tUnits) {
				assertTrue(tEntries.containsKey(tUnit), tLocale + " is missing the B2 unit key " + tUnit);
			}
		});
		// the atomic B2 rows stay whole-string on BOTH faces (the brick prefix form, the
		// bare-noun transmitter, the two wood gearboxes)
		for (String tAtomic : List.of("block.gt6.brick_burning_box", "block.gt6.heat_transmitter",
				"block.gt6.gearbox", "block.gt6.transformer_rotation")) {
			assertTrue(en().containsKey(tAtomic), "en is missing the B2 atomic key " + tAtomic);
			assertTrue(zh().containsKey(tAtomic), "zh is missing the B2 atomic key " + tAtomic);
		}
	}

	/**
	 * The B2 full-expansion pin (the B1-review posture: the slot structure alone let the en
	 * template lose its spaces — the final strings must be pinned). One or two rows per
	 * family, replayed through the substitute() face exactly like the game renders a fully
	 * translated template, pinned EQUAL to the upstream row strings; the zh side pins the
	 * CJK compose shape (免空格 gluing, the dump word order).
	 */
	@Test
	public void stoneAndRowsTemplatesExpandToTheUpstreamStrings() {
		// stone: 16 variants over the "Black Granite" stone word (the old compose column)
		String tStone = en().get("gt6.material.granite_black");
		assertEquals("Black Granite", tStone);
		assertEquals("Black Granite Cobblestone", substitute(en().get("gt6.stone.variant.cobble"), tStone));
		assertEquals("Chiseled Black Granite", substitute(en().get("gt6.stone.variant.bricks_chiseled"), tStone));
		assertEquals("Small Black Granite Bricks", substitute(en().get("gt6.stone.variant.small_bricks"), tStone));
		assertEquals("黑色花岗岩圆石", substitute(zh().get("gt6.stone.variant.cobble"), zh().get("gt6.material.granite_black")));
		// axle
		assertEquals("Small Wooden Axle", expandAxle(0, "wood_treated"));
		assertEquals("Huge Trinitanium Axle", expandAxle(3, "trinitanium"));
		assertEquals("小型木制轴", expandAxleZh(0, "wood_treated"));
		// steam engines
		assertEquals("Steam Engine (Lead)", substitute(en().get("gt6.row.steam_engine.display"), en().get("gt6.row.mat.lead")));
		assertEquals("Strong Steam Engine (Tungstensteel)", substitute(en().get("gt6.row.steam_engine.display.strong"), en().get("gt6.row.mat.tungstensteel")));
		assertEquals("蒸汽引擎 (铅)", substitute(zh().get("gt6.row.steam_engine.display"), zh().get("gt6.row.mat.lead")));
		// diesel
		assertEquals("Bronze Diesel Engine", substitute(en().get("gt6.row.diesel.display"), en().get("gt6.row.mat.bronze")));
		// burning boxes
		assertEquals("Burning Box (Solid, Lead)", expandBurning(GT6BurningBoxes.FAMILY_SOLID_UNIT_KEY, false, "lead"));
		assertEquals("Dense Burning Box (Gas, Tungsten)", expandBurning(GT6BurningBoxes.FAMILY_GAS_UNIT_KEY, true, "tungsten"));
		assertEquals("Fluidized Bed Burning Box (Ta4HfC5)", expandBurning(null, false, "tantalum_hafnium_carbide"));
		assertEquals("Dense Fluidized Bed Burning Box (Ultimet)", expandBurning(null, true, "ultimet"));
		assertEquals("燃烧室 (固体, 铅)", expandBurningZh(GT6BurningBoxes.FAMILY_SOLID_UNIT_KEY, false, "lead"));
		// boilers
		assertEquals("Steam Boiler Tank (Lead)", substitute(en().get("gt6.row.boiler.display"), en().get("gt6.row.mat.lead")));
		assertEquals("Strong Steam Boiler Tank (Ultimet)", substitute(en().get("gt6.row.boiler.display.strong"), en().get("gt6.row.mat.ultimet")));
		// dryer/distillery
		assertEquals("Dryer (Steel)", substitute(en().get("gt6.row.dryer.display"), en().get("gt6.row.mat.steel")));
		assertEquals("Distillery (Tungsten Carbide)", substitute(en().get("gt6.row.distillery.display"), en().get("gt6.row.mat.tungsten_carbide")));
		// large boiler + dense wall
		assertEquals("Invar Boiler Main Barometer", substitute(en().get("gt6.row.large_boiler.display"), en().get("gt6.row.mat.invar")));
		assertEquals("Dense Invar Wall", substitute(en().get("gt6.row.dense_wall.display"), en().get("gt6.row.mat.invar")));
		// machine tiers (task p27-machine-energy-display-fix): the tier slot rides the
		// Kinetic_T MATERIAL word (upstream "Shredder ("+aMat.getLocal()+")", :1294-1309,
		// Kinetic_T[1..4] = Bronze/Steel/Titanium/Tungstensteel MT.java:3690) — the retired
		// gt6.row.tier.* ordinals must stay gone on BOTH faces
		assertEquals("Shredder (Steel)", substitute(en().get("gt6.row.machine.display"),
				en().get(GTMachines.MACHINE_SHREDDER_UNIT_KEY), en().get("gt6.row.mat.steel")));
		assertEquals("粉碎机 (钛)", substitute(zh().get("gt6.row.machine.display"),
				zh().get(GTMachines.MACHINE_SHREDDER_UNIT_KEY), zh().get("gt6.row.mat.titanium")));
		// the tier→material ladder caliber: the tierOf index + 1 selects the Kinetic_T word
		// (T1 rides the atomic block.gt6.shredder name, T2-T4 compose)
		for (int tTier = 1; tTier <= 4; tTier++) {
			assertEquals(List.of("bronze", "steel", "titanium", "tungstensteel").get(tTier - 1),
				GTMachines.KINETIC_TIER_MAT_SLUGS[tTier - 1], "Kinetic tier " + tTier + " material slug");
			assertFalse(en().containsKey("gt6.row.tier." + tTier), "the ordinal tier unit must stay retired on en");
			assertFalse(zh().containsKey("gt6.row.tier." + tTier), "the ordinal tier unit must stay retired on zh");
		}
		// task p29-w2-exotic-energy — the six exotic-energy families: the Polarizer/
		// Magnetic Separator ride the MATERIAL word (the Electric_T[1]/[3] locals), the
		// Laser Engraver / Laser Welder / Freezer / Cryo Mixer the LITERAL "(T1)".."(T5)"
		// tier words (the :1483/:1490/:1621/:1628 name columns — the declaration-fidelity
		// acceptance pin, BOTH faces and BOTH ends of the five-rung ladder)
		assertEquals("Polarizer (Galvanized Steel)", substitute(en().get(GTMachines.MACHINE_POLARIZER_UNIT_KEY), en().get("gt6.row.mat.galvanized_steel")));
		assertEquals("电磁偏振器 (钛)", substitute(zh().get(GTMachines.MACHINE_POLARIZER_UNIT_KEY), zh().get("gt6.row.mat.titanium")));
		assertEquals("Magnetic Separator (Stainless Steel)", substitute(en().get(GTMachines.MACHINE_MAGNETIC_SEPARATOR_UNIT_KEY), en().get("gt6.row.mat.stainless_steel")));
		assertEquals("磁选机 (铝)", substitute(zh().get(GTMachines.MACHINE_MAGNETIC_SEPARATOR_UNIT_KEY), zh().get("gt6.row.mat.aluminium")));
		assertEquals("Laser Engraver (T1)", substitute(en().get(GTMachines.MACHINE_LASER_ENGRAVER_UNIT_KEY), en().get("gt6.row.mat.t1")));
		assertEquals("Laser Engraver (T5)", substitute(en().get(GTMachines.MACHINE_LASER_ENGRAVER_UNIT_KEY), en().get("gt6.row.mat.t5")));
		assertEquals("激光刻蚀机 (T5)", substitute(zh().get(GTMachines.MACHINE_LASER_ENGRAVER_UNIT_KEY), zh().get("gt6.row.mat.t5")));
		assertEquals("Laser Welder (T3)", substitute(en().get(GTMachines.MACHINE_LASER_WELDER_UNIT_KEY), en().get("gt6.row.mat.t3")));
		assertEquals("激光焊接器 (T1)", substitute(zh().get(GTMachines.MACHINE_LASER_WELDER_UNIT_KEY), zh().get("gt6.row.mat.t1")));
		assertEquals("Freezer (T4)", substitute(en().get(GTMachines.MACHINE_FREEZER_UNIT_KEY), en().get("gt6.row.mat.t4")));
		assertEquals("冷冻机 (T1)", substitute(zh().get(GTMachines.MACHINE_FREEZER_UNIT_KEY), zh().get("gt6.row.mat.t1")));
		assertEquals("Cryo Mixer (T5)", substitute(en().get(GTMachines.MACHINE_CRYO_MIXER_UNIT_KEY), en().get("gt6.row.mat.t5")));
		assertEquals("低温搅拌机 (T2)", substitute(zh().get(GTMachines.MACHINE_CRYO_MIXER_UNIT_KEY), zh().get("gt6.row.mat.t2")));
		// attachments
		assertEquals("Ceramic Tap", substitute(en().get("gt6.row.tap.display"), en().get("gt6.row.attachment.mat.ceramic")));
		assertEquals("Tantalum Hafnium Carbide Funnel", substitute(en().get("gt6.row.funnel.display"), en().get("gt6.row.attachment.mat.tantalum_hafnium_carbide")));
	}

	/** The axle expansion over the en face. */
	private static String expandAxle(int aSizeIndex, String aMatSlug) {
		return substitute(en().get("gt6.row.axle.display"),
				en().get(GT6Kinetics.axleSizeUnitKey(aSizeIndex)), en().get("gt6.row.mat." + aMatSlug));
	}

	/** The axle expansion over the zh face (the CJK gluing pin). */
	private static String expandAxleZh(int aSizeIndex, String aMatSlug) {
		return substitute(zh().get("gt6.row.axle.display"),
				zh().get(GT6Kinetics.axleSizeUnitKey(aSizeIndex)), zh().get("gt6.row.mat." + aMatSlug));
	}

	/** The burning-box expansion over the en face (aFamilyUnit null = the fluidbed form). */
	private static String expandBurning(String aFamilyUnit, boolean aDense, String aMatSlug) {
		String tKey = aFamilyUnit == null
				? (aDense ? "gt6.row.burning_box.display.fluidbed_dense" : "gt6.row.burning_box.display.fluidbed")
				: (aDense ? "gt6.row.burning_box.display.dense" : "gt6.row.burning_box.display");
		return aFamilyUnit == null
				? substitute(en().get(tKey), en().get("gt6.row.mat." + aMatSlug))
				: substitute(en().get(tKey), en().get(aFamilyUnit), en().get("gt6.row.mat." + aMatSlug));
	}

	/** The burning-box expansion over the zh face. */
	private static String expandBurningZh(String aFamilyUnit, boolean aDense, String aMatSlug) {
		String tKey = aDense ? "gt6.row.burning_box.display.dense" : "gt6.row.burning_box.display";
		return substitute(zh().get(tKey), zh().get(aFamilyUnit), zh().get("gt6.row.mat." + aMatSlug));
	}

	/**
	 * The compose material-slot presence pin (review R2 finding): the compose references
	 * {@code gt6.material.<snake>} UNCONDITIONALLY, but tier materials are created with
	 * {@code mID -1} (Superconductor, MT.java:986 {@code tier()}) and never reach the
	 * registration-face material walk (GT6EnUs.addMaterialNames {@code mID < 0} continue) —
	 * the 16 superconductor variants composed the RAW key while every slot-structure and
	 * expansion pin stayed green, because none checked the material KEY FACE. This walks the
	 * whole 626-variant compose domain (620 electric + 6 redstone) against the en recording
	 * face, so a keyface/compose-domain mismatch of this class is structurally red — the B2
	 * rows domain reuses the shape.
	 */
	@Test
	public void everyComposedVariantMaterialKeyIsOnTheEnFace() {
		List<String> tMissing = new ArrayList<>();
		int tChecked = 0;
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) {
			tChecked++;
			String tKey = "gt6.material." + MaterialPrefixItem.snakeCase(tVariant.row().material().get().mNameInternal);
			if (!en().containsKey(tKey)) tMissing.add(GTWireSpecs.registryName(tVariant) + " -> " + tKey);
		}
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) {
			tChecked++;
			String tKey = "gt6.material." + MaterialPrefixItem.snakeCase(tVariant.row().material().get().mNameInternal);
			if (!en().containsKey(tKey)) tMissing.add(GTWireSpecs.registryName(tVariant) + " -> " + tKey);
		}
		assertEquals(626, tChecked, "the wire compose domain census (the atomic laser/legacy forms are NOT in scope)");
		assertTrue(tMissing.isEmpty(), "every composed variant's material key must exist on the en face"
			+ " (a missing face renders the RAW key at runtime): " + tMissing);
	}

	/**
	 * The B2 keyface pin (the review R2 shape, rows + stone edition): every rows row's
	 * compose references a small-unit key UNCONDITIONALLY — this walks the whole B2 domain
	 * (233 rows-material slots + the 17 stone-name slots) against the en recording face, so
	 * a row table entry whose unit key missed both the template face and the material walk
	 * is structurally red instead of a silent raw-key render. The stone walk checks the
	 * gt6.material small unit each of the 17 stones' getName composes with.
	 */
	@Test
	public void everyComposedRowAndStoneUnitKeyIsOnTheEnFace() {
		List<String> tMissing = new ArrayList<>();
		int tChecked = 0;
		// stones: the variant-0 compose of each of the 17 blocks
		for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) {
			tChecked++;
			String tKey = "gt6.material." + MaterialPrefixItem.snakeCase(tStone.material().get().mNameInternal);
			if (!en().containsKey(tKey)) tMissing.add("stone:" + tStone.snake() + " -> " + tKey);
		}
		// axles
		for (GT6Kinetics.AxleSpec tSpec : GT6Kinetics.AXLE_SPECS) {
			for (int tSize = 0; tSize < GT6Kinetics.AXLE_DIAMETERS.length; tSize++) {
				tChecked++;
				if (!en().containsKey(GT6Kinetics.axleSizeUnitKey(tSize))) tMissing.add("size:" + tSize);
				if (!en().containsKey(GT6Kinetics.axleMatUnitKey(tSpec))) tMissing.add("axle:" + tSpec.material());
			}
		}
		// steam + diesel
		for (GT6Kinetics.SteamEngineRow tRow : GT6Kinetics.STEAM_ENGINES) {
			tChecked++;
			if (!en().containsKey(GT6Kinetics.steamMatUnitKey(tRow))) tMissing.add("steam:" + tRow.path());
		}
		for (GT6Kinetics.DieselSpec tSpec : GT6Kinetics.DIESEL_SPECS) {
			tChecked++;
			if (!en().containsKey(GT6Kinetics.dieselMatUnitKey(tSpec))) tMissing.add("diesel:" + tSpec.material());
		}
		// burning (the Brick atom excluded — no compose)
		for (GT6BurningBoxes.BurningBoxRow tRow : GT6BurningBoxes.allRows()) {
			if (tRow == GT6BurningBoxes.BRICK_ROW) continue;
			tChecked++;
			if (!en().containsKey(GT6BurningBoxes.matUnitKeyOf(tRow))) tMissing.add("burning:" + tRow.path());
		}
		// boilers
		for (GT6Boilers.BoilerRow tRow : GT6Boilers.allRows()) {
			tChecked++;
			if (!en().containsKey(GT6Boilers.matUnitKeyOf(tRow))) tMissing.add("boiler:" + tRow.path());
		}
		// dryer + distillery + the p28 ULV rows (the row-carrier compose domain)
		for (java.util.List<gregtech6.block.GTBasicMachineBlock.MachineRow> tRows
				: java.util.List.of(GTMachines.DRYER_ROWS, GTMachines.DISTILLERY_ROWS,
						GTMachines.CANNER_ULV_ROWS, GTMachines.SIFTER_ULV_ROWS, GTMachines.WIREMILL_ULV_ROWS,
						GTMachines.SHREDDER_ULV_ROWS, GTMachines.CRUSHER_ULV_ROWS, GTMachines.ROLLINGMILL_ROWS)) {
			for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : tRows) {
				tChecked++;
				if (!en().containsKey("gt6.row.mat." + tRow.matSlug())) tMissing.add(tRow.path());
			}
		}
		// large boiler + dense wall
		for (GTMultiBlocks.LargeBoilerRow tRow : GTMultiBlocks.LARGE_BOILER_ROWS) {
			tChecked++;
			if (!en().containsKey(GTMultiBlocks.boilerMatUnitKeyOf(tRow))) tMissing.add("lb:" + tRow.path());
		}
		for (GTMultiBlocks.MultiblockPartRow tRow : GTMultiBlocks.WALL_ROWS) {
			tChecked++;
			if (!en().containsKey(GTMultiBlocks.wallMatUnitKeyOf(tRow))) tMissing.add("wall:" + tRow.path());
		}
		// attachments
		for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) {
			tChecked++;
			if (!en().containsKey(GT6Attachments.matUnitKeyOf(tRow))) tMissing.add("att:" + tRow.path());
		}
		// task p29-w3-tank-valves — the 25 Tank Main Valve rows: the size word + the
		// material unit word per row (the metal materials ride the EXISTING dense-wall
		// units, the wood row the dedicated wood unit — both faces must carry them)
		for (GT6Tanks.TankValveRow tRow : GT6Tanks.ROWS) {
			tChecked++;
			if (!en().containsKey(GT6Tanks.sizeUnitKeyOf(tRow))) tMissing.add("tank_size:" + tRow.path());
			if (!en().containsKey(GT6Tanks.matUnitKeyOf(tRow))) tMissing.add("tank_mat:" + tRow.path());
		}
		assertEquals(286, tChecked, "the B2 compose domain census: 17 stone blocks + the rows"
            + " (44 axle + 28 steam + 8 diesel + 96 burning + 26 boiler + 4 dryer + 4 distillery"
            + " + 6 p28 ULV rows + 5 large boiler + 11 wall (the 6 p29-w3 dense additions joined)"
            + " + 12 attachments + 2 dry/dist shares not double-counted + 25 tank valves (task"
            + " p29-w3-tank-valves))"
            + " — bump this pin ONLY with a real row-table change");
		assertTrue(tMissing.isEmpty(), "every composed row/stone unit key must exist on the en face"
			+ " (a missing face renders the RAW key at runtime): " + tMissing);
	}

	/**
	 * The p23-i18n-material-fill-fix pin set ① — key construction + single-slug consistency.
	 * Both {@code %s} fill points share the {@link MaterialPrefixItem#materialFill} seam, and
	 * the seam derives its slug with the SAME {@code snakeCase} the en lang walk uses
	 * (GT6EnUs.addMaterialNames / addWireRowMaterialNames) — a second case/underscore rule set
	 * anywhere on the fill path would break the en walk consistency loop below. Offline pure
	 * seam: no Item instance is constructed (the GTWireDisplayNameTest posture).
	 */
	@Test
	public void materialFillPinsTheEnWalkSlugs() {
		// the key construction pins: bronze/steel -> gt6.material.bronze/steel
		assertEquals("gt6.material.bronze", materialFillKey(MT.Bronze));
		assertEquals("gt6.material.steel", materialFillKey(MT.Steel));
		// consistency with the en walk: every key the registration-face material walk emits is
		// EXACTLY the key the fill seam composes for the same (merged) material
		List<String> tDrift = new ArrayList<>();
		for (Map.Entry<String, OreDictMaterial> tEmitted : GT6EnUs.materialWalkEmittedKeys().entrySet()) {
			String tFillKey = materialFillKey(tEmitted.getValue());
			if (!tFillKey.equals(tEmitted.getKey())) tDrift.add(tEmitted.getKey() + " != fill " + tFillKey);
		}
		assertTrue(tDrift.isEmpty(),
			"the material fill must ride the en walk's single snakeCase derivation: " + tDrift);
	}

	/**
	 * The p23-i18n-material-fill-fix pin set ② — display-name Component content + en equivalence.
	 * The fill is a NESTED slot-less translatable unit (each locale resolves the material word in
	 * its own language); on the en face the unit resolves to the EXACT word the old raw mNameLocal
	 * literal carried (the en key values ARE the mNameLocal faces), so en display behavior is
	 * unchanged — pinned by the full-template expansions in the game render posture
	 * (en "Bronze Ingot"; zh finally renders 青铜锭 from the consumed zh keys).
	 */
	@Test
	public void materialFillIsANestedTranslatableResolvingToTheSameEnWord() {
		Component tBronze = MaterialPrefixItem.materialFill(MT.Bronze);
		assertTrue(tBronze.getContents() instanceof TranslatableContents, "the fill must be a nested translatable unit");
		assertEquals("gt6.material.bronze", ((TranslatableContents)tBronze.getContents()).getKey());
		assertEquals(0, ((TranslatableContents)tBronze.getContents()).getArgs().length, "the unit is a slot-less noun");
		// en equivalence: the unit's en face == the raw literal the old fill carried
		assertEquals(MT.Bronze.mNameLocal, en().get("gt6.material.bronze"));
		// the full template, expanded the way the game renders it
		assertEquals("Bronze Ingot", substitute(en().get("gt6.tagprefix.ingot"), en().get("gt6.material.bronze")));
		assertEquals("青铜锭", substitute(zh().get("gt6.tagprefix.ingot"), zh().get("gt6.material.bronze")));
	}

	/**
	 * The p23-i18n-material-fill-fix registry-pair guard: EVERY registered (prefix, material)
	 * pair — the 105-prefix item universe AND the 7-prefix storage-block universe — now fills
	 * its {@code gt6.tagprefix.*} template with the {@code gt6.material.<snake>} key
	 * UNCONDITIONALLY, so a pair whose material key missed both the registration walk
	 * (GT6EnUs.addMaterialNames, {@code mID >= 0} + mNameLocal guard) and the wire-row backfill
	 * would render the RAW key in game. Walks both registration faces against the en recording
	 * face (the everyComposedVariantMaterialKeyIsOnTheEnFace shape, registry-pair edition).
	 */
	@Test
	public void everyRegisteredPairMaterialKeyIsOnTheEnFace() {
		List<String> tMissing = new ArrayList<>();
		int tChecked = 0;
		for (List<GTMaterialItems.PrefixMaterial> tUniverse
				: List.of(GTMaterialItems.registrationOrder(), GTMaterialBlocks.registrationOrder())) {
			for (GTMaterialItems.PrefixMaterial tPair : tUniverse) {
				tChecked++;
				String tKey = materialFillKey(tPair.material());
				if (!en().containsKey(tKey)) {
					tMissing.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()) + " -> " + tKey);
				}
			}
		}
		assertEquals(60026, tChecked, "the registry-pair compose domain census: the item universe (56253)"
			+ " + the storage-block universe (3773) — bump this pin ONLY with a real registration change");
		assertTrue(tMissing.isEmpty(), "every registered pair's material key must exist on the en face"
			+ " (a missing face renders the RAW key at runtime): " + tMissing);
	}

	/** The material fill seam's key (the p23 shared derivation, pinned by materialFillPinsTheEnWalkSlugs). */
	private static String materialFillKey(OreDictMaterial aMaterial) {
		return ((TranslatableContents)MaterialPrefixItem.materialFill(aMaterial).getContents()).getKey();
	}

	/** The wire-name expansion over the en face: aSize 0 = the size-less plain template. */
	private static String expandWire(int aSize, String aMaterialSnake, String aFormKey) {
		String tTemplate = en().get(aSize > 0 ? "gt6.wire.display" : "gt6.wire.display.plain");
		String tMaterial = en().get("gt6.material." + aMaterialSnake);
		assertTrue(tTemplate != null && tMaterial != null, "the expansion face must be recorded");
		return substitute(tTemplate, aSize > 0 ? new String[] {String.valueOf(aSize), tMaterial, en().get(aFormKey)}
			: new String[] {tMaterial, en().get(aFormKey)});
	}

	/** Substitutes the {@code %s} slots in order — the game-side rendering of a fully-translated template face. */
	private static String substitute(String aTemplate, String... aArgs) {
		StringBuilder rBuilder = new StringBuilder();
		int tArg = 0;
		for (int i = 0; i < aTemplate.length(); i++) {
			if (aTemplate.charAt(i) == '%' && i + 1 < aTemplate.length() && aTemplate.charAt(i + 1) == 's') {
				assertTrue(tArg < aArgs.length, "template " + aTemplate + " consumed more slots than provided");
				rBuilder.append(aArgs[tArg++]);
				i++;
			} else {
				rBuilder.append(aTemplate.charAt(i));
			}
		}
		assertTrue(tArg == aArgs.length, "template " + aTemplate + " left " + (aArgs.length - tArg) + " slots unfilled");
		return rBuilder.toString();
	}

	@Test
	public void zhCoverageMeetsTheRatchetFloor() {
		int tSize = zh().size();
		assertTrue(tSize >= ZH_KEY_FLOOR,
			"zh key count " + tSize + " fell below the ZH_KEY_FLOOR ratchet " + ZH_KEY_FLOOR
			+ " — the floor only ever goes UP: raise it via an explicit PR constant bump");
	}

	@Test
	public void allZhValuesAreNonBlankTemplatesHaveTheirSlot() {
		zh().forEach((tKey, tValue) -> {
			assertFalse(tValue == null || tValue.isBlank(), "blank zh value for " + tKey);
			if (tKey.startsWith("gt6.tagprefix.")) {
				assertTrue(tValue.contains("%s"),
					"zh prefix template " + tKey + " lost its %s material slot (the runtime fills it)");
			}
		});
	}

	@Test
	public void noHandRowIsOrphaned() {
		// a typo'd hand-row key (or a key whose walk was removed) would otherwise be silently
		// skipped — surface it. review-status direct rows may legitimately stay unemitted.
		List<String> tOrphans = new ArrayList<>();
		for (Map.Entry<String, GT6ZhCn.RefRow> tRow : GT6ZhCn.loadReference().get("direct").entrySet()) {
			if ("hand".equals(tRow.getValue().status()) && !zh().containsKey(tRow.getKey())) {
				tOrphans.add(tRow.getKey());
			}
		}
		assertTrue(tOrphans.isEmpty(), "hand rows never emitted by the zh walks: " + tOrphans);
	}

	/**
	 * The registry-coverage gate (task p28-cfoam-lang-key): EVERY gt6 block registered
	 * through the port's DeferredRegisters whose Jade name line resolves the VANILLA
	 * descriptionId ({@code block.gt6.<path>}) must carry that key on BOTH lang faces —
	 * Jade's name row walks Block.getName → getDescriptionId (vanilla Block.java:308-313),
	 * so a missing key renders the raw key on hover. The gate exists because the parity
	 * subset + floor can never catch an en/zh-symmetric omission (block.gt6.cfoam_owned was
	 * missing from all four faces for two phases while every gate stayed green).
	 *
	 * <p>The walk is offline-safe: it reads the DeferredRegister ENTRIES (the
	 * RegistryObjects' stored ids, DeferredRegister.getEntries + RegistryObject.getId), never
	 * the registry itself — no supplier runs. Blocks whose class OVERRIDES getName() with a
	 * composed face never resolve the vanilla key and are EXEMPT — the exemption is DERIVED
	 * from the same row tables the registrations loop over (the P27 batch2 bounded-replay
	 * posture: a new row lands in the table, the exemption follows it automatically), and
	 * every family cites its overriding class. The two RegisterEvent families (GTStoneBlocks
	 * 272 + GTMaterialBlocks 3773, dynamic loops not visible here) are composed-name
	 * universes too (GTStoneBlock.getName; the material blocks' stack name rides
	 * GTMaterialPrefixBlockItem.getName(ItemStack)) with their own template pins
	 * (stoneAndRowsTemplates*, the registry-pair guard) — documented here, not walkable.
	 *
	 * <p>The census pins make every direction of drift loud: a NEW block with a lang key
	 * breaks the walked pin, a NEW composed row breaks the exempt pin, and a NEW
	 * vanilla-default block WITHOUT a key breaks the missing-key assertion itself.
	 */
	@Test
	public void everyRegisteredBlockDescriptionIdHasLangKeysOnBothFaces() {
		List<Class<?>> tRegClasses = List.of(
			GT6Anvils.class,
			GT6Attachments.class, GT6Boilers.class, GT6BurningBoxes.class, GT6Crucibles.class,
			GT6ElectricDynamos.class, // task p28-c-ulv-dynamo-row — the W1 leftover: the dynamo classes join the gate
			GT6ElectricTransformers.class, GT6FeBatteries.class, GT6FeConverters.class, GT6FluxDynamos.class,
			GT6FoamBlocks.class, GT6Hoppers.class, GT6Kinetics.class, GT6Kitchen.class, GT6Lasers.class,
			// task p32-logistics-lv2 — the logistics domain carrier joins the gate (review seam:
			// the single logistics_wire block, the atomic key, both provider faces)
			GT6Logistics.class,
			// task p32-magic-absorber — the absorber joins the gate (the atomic key; the
			// rebase reconciliation counts BOTH W2 singles: census 173 -> 174, floor 3289 -> 3290)
			GT6MagicAbsorbers.class,
			// task p32-qu-energizer — the five energizer rungs join the gate (the atomic
			// quantum_energizer keys; the rebase reconciliation over the merged W2 state:
			// census 174 -> 179, floor 3290 -> 3295)
			GT6Molds.class,
			// task p32-placeables — the deco family joins the gate (review seam: the card
			// landed its 8 blocks + 8 lang rows without joining the walk — the silent-
			// exemption shape this census exists to catch; the atomic keys both faces:
			// census 179 -> 187, floor 3295 -> 3303)
			GT6Placeables.class, GT6QuantumEnergizers.class,
			GT6Sensors.class, GT6StaticStorages.class,
			GT6SurfaceBlocks.class, // task p30-w6-rocks-sticks — the surface deco blocks (all four composed-name, EXEMPT below)
			// task p29-w3-turbine-dynamo — the turbine + dynamo controller carriers (ATOMIC
			// vanilla-key names, so they ride the CHECKED leg, no exemptions)
			GT6Turbines.class, GT6DynamoHousings.class,
			GTBarrels.class, GTBlockEntities.class, GTEnergySources.class, GTFluidPipes.class,
			GTGrassBlocks.class, GTItemPipes.class, GTMachines.class, GTMultiBlocks.class,
			GTWires.class);
		// The composed-name exemption, derived per family. Each line cites the block class
		// whose getName() override composes the name from template keys (so the vanilla
		// descriptionId is deliberately NOT backed by a lang key for that family).
		Set<String> tExempt = new java.util.HashSet<>();
		for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) tExempt.add(tRow.path()); // GTAttachmentSmallBlock.getName
		for (GT6Boilers.BoilerRow tRow : GT6Boilers.allRows()) tExempt.add(tRow.path()); // BoilerTankBlock.getName (GT6Boilers:273)
		for (GT6BurningBoxes.BurningBoxRow tRow : GT6BurningBoxes.allRows()) { // BurningBoxBlock.getName (GT6BurningBoxes:434); the BRICK atom keeps its key checked (B2 atomic pin)
			if (tRow != GT6BurningBoxes.BRICK_ROW) tExempt.add(tRow.path());
		}
		for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) tExempt.add(tRow.path()); // CrucibleBlock.getName (GT6Crucibles:182)
		for (GT6Crucibles.CrucibleRow tRow : GT6Crucibles.CRUCIBLE_ROWS) tExempt.add(tRow.path()); // GTCrucibleControllerBlock.getName
		// task p29-w3-distill-crucible — the seven ladder wall carriers (GTCrucibleWallBlock
		// getName -> the mComposedName metal-wall template over the EXISTING gt6.row.mat words;
		// the Steel wall keeps its vanilla key — the p26 single-rung atomic pin)
		for (var tWallEntry : GT6Crucibles.CRUCIBLE_WALL_BLOCKS_BY_PATH.entrySet()) tExempt.add(tWallEntry.getKey());
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) tExempt.add(tRow.path()); // GT6HopperBlock.getName -> displayOf (GT6Hoppers:235)
		for (GT6Kinetics.SteamEngineRow tRow : GT6Kinetics.STEAM_ENGINES) tExempt.add(tRow.path()); // SteamEngineBlock.getName (GT6Kinetics:401)
		for (GT6Molds.MoldRow tRow : GT6Molds.ROWS) tExempt.add(tRow.path()); // MoldBlock.getName (GT6Molds:316)
		for (GT6Molds.MoldRow tRow : GT6Molds.CERAMIC_ROWS) tExempt.add(tRow.path()); // MoldBlock.getName — the pre-carved band
		tExempt.add("mold_ceramic"); // the carvable blank the GT6Molds.withBlank(CERAMIC_ROWS) walk prepends (private helper, same MoldBlock class)
		for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) tExempt.add(tRow.path()); // TileEntityFaucet.FaucetBlock.getName
		for (GTItemPipes.ItemPipeRow tRow : GTItemPipes.ROWS) tExempt.add(tRow.path()); // GTItemPipeBlock.getName
		// task p30-w6-rocks-sticks — the four surface deco carriers (GT6SurfaceRockBlock
		// .getName: the gt6.surface.rock template + material unit, or the stick's atomic
		// template key — the vanilla descriptionId is deliberately NOT backed)
		for (var tSurfaceRow : GT6SurfaceBlocks.ALL) tExempt.add(tSurfaceRow.getId().getPath());
		// task p29-w3-nbtdesign-parts — the ten composed metal-wall carriers (the tungsten
		// wall rides the Lightning Rod family's registration, not the expansion rows)
		for (GTMultiBlocks.PartRow tRow : GTMultiBlocks.METAL_WALL_ROWS) {
			if (!tRow.path().equals("machine_wall_tungsten")) tExempt.add(tRow.path()); // GTMultiBlockPartBlock.getName -> mComposedName
		}
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.DRYER_ROWS) tExempt.add(tRow.path()); // GTBasicMachineBlock.getName — the mRow carriers
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CANNER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.PRESS_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.EXTRUDER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SIFTER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.COMPRESSOR_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.WIREMILL_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.DISTILLERY_ROWS) tExempt.add(tRow.path());
		// task p28-c-ulv-machine-ladder — the six ULV row carriers (GTBasicMachineBlock.getName)
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CANNER_ULV_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SIFTER_ULV_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.WIREMILL_ULV_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SHREDDER_ULV_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CRUSHER_ULV_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLLINGMILL_ROWS) tExempt.add(tRow.path());
		// task p29-w1-kinetic-roll-ladder — the sixteen roll-ladder row carriers (GTBasicMachineBlock.getName)
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLLINGMILL_RU_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLL_BENDER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLL_FORMER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CLUSTER_MILL_ROWS) tExempt.add(tRow.path());
		// task p29-w1-kinetic-process-ladder — the six process-family row carriers (GTBasicMachineBlock.getName)
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.BUZZSAW_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SQUEEZER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CENTRIFUGE_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SLUICE_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SANDING_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.PRESSURE_WASHER_ROWS) tExempt.add(tRow.path());
		// task p29-w1-eu-hu-families — the 25 eu-hu row carriers (GTBasicMachineBlock.getName)
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.MIXER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ELECTRIC_MIXER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ELECTRIC_LOOM_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ELECTRIC_SIFTER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.BOXINATOR_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.UNBOXINATOR_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.FERMENTER_ROWS) tExempt.add(tRow.path());
		// task p29-w2-eu-special — the 14 eu-special row carriers (GTBasicMachineBlock.getName)
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.AUTOCRAFTER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.LIGHTNING_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.LAMINATOR_ROWS) tExempt.add(tRow.path());
		// task p29-w2-exotic-energy — the 30 exotic-energy row carriers (GTBasicMachineBlock.getName)
		for (java.util.List<gregtech6.block.GTBasicMachineBlock.MachineRow> tExoticRows : java.util.List.of(
				GTMachines.POLARIZER_ROWS, GTMachines.MAGNETIC_SEPARATOR_ROWS, GTMachines.LASER_ENGRAVER_ROWS,
				GTMachines.LASER_WELDER_ROWS, GTMachines.FREEZER_ROWS, GTMachines.CRYO_MIXER_ROWS)) {
			for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : tExoticRows) tExempt.add(tRow.path());
		}
		// task p29-w2-eu-core-5tier — the 25 eu-core row carriers (GTBasicMachineBlock.getName,
		// the FIRST 5-tier ladders: 5 rows x 5 families)
		// task p31-massfab — the 5 small Massfab row carriers (GTBasicMachineBlock.getName,
		// the family displayKey template over the literal tier words)
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.MASSFAB_SMALL_ROWS) tExempt.add(tRow.path());
		// task p32-qu-scanner-replicator — the 4 QU machine row carriers (the scanner
		// single + the replicator three-rung, the GTBasicMachineBlock.getName composed face)
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.MOLECULAR_SCANNER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.REPLICATOR_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ELECTROLYZER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.INJECTOR_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.PRINTER_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SCANNER_VISUALS_ROWS) tExempt.add(tRow.path());
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SLICER_ROWS) tExempt.add(tRow.path());
		// task p29-w2-hu-tu-piggyback — the 16 hu-tu row carriers (GTBasicMachineBlock.getName)
		for (java.util.List<gregtech6.block.GTBasicMachineBlock.MachineRow> tHuTuRows : java.util.List.of(
				GTMachines.STEAM_CRACKER_ROWS, GTMachines.CATALYTIC_CRACKER_ROWS, GTMachines.COAGULATOR_ROWS,
				GTMachines.GENERIFIER_ROWS, GTMachines.BATH_ROWS, GTMachines.AUTOCLAVE_ROWS, GTMachines.LOOM_ROWS)) {
			for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : tHuTuRows) tExempt.add(tRow.path());
		}
		// task p29-w3-heat-smelter — the 5 heat-smelter row carriers (GTBasicMachineBlock.getName)
		for (java.util.List<gregtech6.block.GTBasicMachineBlock.MachineRow> tHeatRows : java.util.List.of(
				GTMachines.SMELTER_ROWS, GTMachines.MELTER_ROWS, GTMachines.ROASTING_ROWS)) {
			for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : tHeatRows) tExempt.add(tRow.path());
		}
		for (GTMachines.OvenRow tRow : GTMachines.OVEN_ROWS) tExempt.add(tRow.path()); // GTOvenBlock.getName — the composed Heat_T ladder (p27-oven-heat-t-ladder)
		// the Kinetic_T tier carriers T2-T4 (GTBasicMachineBlock mComposedName, p27-machine-
		// energy-display-fix) — T1 keeps its vanilla atomic key (block.gt6.shredder/lathe/
		// crusher) and stays CHECKED; there is no row table for the ladder, so the paths
		// replay explicitly
		for (String tFamily : List.of("shredder", "lathe", "crusher")) {
			for (int tTier = 2; tTier <= 4; tTier++) tExempt.add(tFamily + "_t" + tTier);
		}
		for (GTMultiBlocks.MultiblockPartRow tRow : GTMultiBlocks.WALL_ROWS) tExempt.add(tRow.path()); // GTMultiBlockPartBlock.getName
		for (GTMultiBlocks.LargeBoilerRow tRow : GTMultiBlocks.LARGE_BOILER_ROWS) tExempt.add(tRow.path()); // GTLargeBoilerBlock.getName
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) tExempt.add(GTWireSpecs.registryName(tVariant)); // GTWireBlock.getName (the WIRES_COMPOSED_PREFIXES negative pins)
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) tExempt.add(GTWireSpecs.registryName(tVariant));
		// The CONSTRUCT-PHASE composed blocks (GT6Kinetics.onModConstruct: 44 GTAxleBlock + 8
		// GTDieselEngineBlock — both compose over gt6.row.* templates): the forge leg's bare
		// JVM never fires the construct event, so its DeferredRegister never carries them and
		// the walk below never sees them; the 21.1 leg runs tests inside the mod classloader
		// (the construct event HAS fired), so its walk DOES see them. They ride a SEPARATE
		// set OUTSIDE the stale-exemption accounting (a forge-leg walk would report them as
		// stale) — skipped by name on whichever leg carries them.
		Set<String> tPhaseExempt = new java.util.HashSet<>();
		for (GT6Kinetics.AxleSpec tSpec : GT6Kinetics.AXLE_SPECS) {
			for (int tSize = 0; tSize < GT6Kinetics.AXLE_DIAMETERS.length; tSize++) tPhaseExempt.add(GT6Kinetics.axleName(tSpec.material(), tSize));
		}
		for (GT6Kinetics.DieselSpec tSpec : GT6Kinetics.DIESEL_SPECS) tPhaseExempt.add(GT6Kinetics.dieselName(tSpec.material()));
		assertEquals(52, tPhaseExempt.size(), "the construct-phase composed census (44 axles + 8 diesels)");

		List<String> tMissing = new ArrayList<>();
		int tSeen = 0;
		int tChecked = 0;
		int tExemptTotal = tExempt.size();
		Map<String, Integer> tWalkedByClass = new java.util.LinkedHashMap<>();
		for (Class<?> tClass : tRegClasses) {
			// The register and its entries are read via PURE REFLECTION: the Forge 1.20.1
			// DeferredRegister/RegistryObject take ONE type parameter while the NeoForge 21.1
			// forms take TWO (DeferredRegister<T, R extends T>) — no compile-time reference to
			// either class compiles on both legs, but the METHOD faces (getEntries/getId) are
			// identical, so the ids are read reflectively (the field lookup above is already
			// reflective; this only extends it one call deeper).
			Object tRegister = null;
			for (String tFieldName : List.of("BLOCKS", "BLOCKS_REG")) {
				try {
					Object tField = tClass.getField(tFieldName).get(null);
					// GTGrassBlocks spells the DeferredRegister BLOCKS_REG (its BLOCKS is a
					// List<RegistryObject<Block>> convenience face) — dispatch on the VALUE type
					if (tField != null && tField.getClass().getName().endsWith("DeferredRegister")) {
						tRegister = tField;
						break;
					}
				} catch (ReflectiveOperationException tDenied) {
					// try the next field name, then fail below
				}
			}
			if (tRegister == null) throw new IllegalStateException(tClass + " has no BLOCKS/BLOCKS_REG DeferredRegister");
			try {
				@SuppressWarnings("unchecked")
				java.util.Collection<Object> tEntries = (java.util.Collection<Object>)tRegister.getClass().getMethod("getEntries").invoke(tRegister);
				for (Object tEntry : tEntries) {
					net.minecraft.resources.ResourceLocation tId =
						(net.minecraft.resources.ResourceLocation)tEntry.getClass().getMethod("getId").invoke(tEntry);
					assertEquals("gt6", tId.getNamespace(), tClass + " block namespace");
					String tPath = tId.getPath();
					tSeen++;
					if (tExempt.contains(tPath)) { tExempt.remove(tPath); continue; }
					if (tPhaseExempt.contains(tPath)) continue;
					tChecked++;
					tWalkedByClass.merge(tClass.getSimpleName(), 1, Integer::sum);
					String tKey = "block.gt6." + tPath;
					if (!en().containsKey(tKey)) tMissing.add("en:" + tKey + " (" + tClass.getSimpleName() + ")");
					if (!zh().containsKey(tKey)) tMissing.add("zh:" + tKey + " (" + tClass.getSimpleName() + ")");
				}
			} catch (ReflectiveOperationException tDenied) {
				throw new IllegalStateException(tClass + " register walk failed", tDenied);
			}
		}
		// the census pins: bump ONLY with a real registration change (the numbers are the
		// p28 discovery-run measurements over the 21 DeferredRegisters, +2 the dynamo pair
		// joined at p28-c-ulv-dynamo-row). The exempt pin is
		// the DERIVED size (a new composed row grows it); the checked pin is every block NOT
		// exempted (a new vanilla-default block grows it); the leftover must stay 0. All
		// three are LEG-INVARIANT: the 21.1 leg carries the +52 construct-phase kinetics
		// entries, but those are phase-exempted before the checked count.
		assertEquals(0, tExempt.size(), "every exempted path must name a REGISTERED block (a stale"
			+ " exemption = a row table shrank or a path typo'd)");
		assertEquals(1142, tExemptTotal, "the derived composed-name exemption census"
			+ " (+16 task p29-w3-nbtdesign-parts: the ten composed metal-wall carriers"
			+ " + the six dense-wall additions joined the carrier set — the five"
			+ " pre-existing dense rows were already exempted)"
			+ " (+5 task p31-massfab: the small Massfab row carriers, 1133 + 5)"
			+ " (the six p28 ULV row carriers joined at 903 + 6; the sixteen roll-ladder"
			+ " row carriers joined at task p29-w1-kinetic-roll-ladder, 909 + 16; the six"
			+ " p29 process families joined at 925 + 24, task p29-w1-kinetic-process-ladder;"
			+ " the 25 eu-hu row carriers joined at 949 + 25, task p29-w1-eu-hu-families;"
			+ " the 14 eu-special row carriers joined at 974 + 14, task p29-w2-eu-special;"
			+ " the 30 exotic-energy row carriers joined at 988 + 30, task p29-w2-exotic-energy;"
			+ " the 25 eu-core 5-tier row carriers joined at 1018 + 25, task p29-w2-eu-core-5tier;"
			+ " the 16 hu-tu row carriers joined at 1043 + 16, task p29-w2-hu-tu-piggyback)"
			+ " (+14 task p29-w3-distill-crucible: the seven crucible ladder rung carriers"
			+ " (the CRUCIBLE_ROWS walk grew 1 -> 8) + the seven dedicated GTCrucibleWallBlock"
			+ " wall carriers over the metal-wall template; the 5 heat-smelter row carriers joined at task p29-w3-heat-smelter (1089 + 5 = 1094); the 4 Roasting row carriers joined at task p29-w4-eu-bridge (1094 + 4 = 1098); the 4 surface deco carriers joined at task p30-w6-rocks-sticks (1098 + 4 = 1102); the 31 vein-indicator rock carriers joined at task p30-w6-t3-large-veins (1102 + 31 = 1133, the spec ⑤ compensation set)"
			+ " (+4 task p32-qu-scanner-replicator: the QU machine row carriers, 1138 + 4 = 1142, the molecular_scanner/replicator tier-word composed rungs the moment they registered)");
		assertEquals(189, tChecked,  "the checked block census: every DeferredRegister block NOT"
			+ " twelve turbine/dynamo controllers joined at task p29-w3-turbine-dynamo (122 + 12;"
			+ " the eleven dynamo rows joined at task p28-c-ulv-dynamo-row (90 + 11; the anvil pair"
			+ " and the transformer joined at 323c4ae4/1e07061d; the 28 p29-w3 part blocks"
			+ " joined at bff8400a — GTMultiBlocks 7 -> 25 checked (the ten composed metal"
			+ " walls exempt) + the six dense additions); the 5 heat-smelter row carriers joined at task p29-w3-heat-smelter (134 + 5; the HEX controller joins at task p29-w3-heat-smelter — GT6HeatExchangers 1 checked; the 15 EU-bridge converter rows joined at task p29-w4-eu-bridge (134 + 15 = 149 checked — GTMachines 38 checked, the bridges' atomic names), the 4 Roasting rows exempt as composed carriers); the missed 18103 bedrock_drill_head joined at task p30-pool-drillhead-18103 (149 + 1 = 150 — GTMultiBlocks 25 -> 26 checked)"
			+ " ; the 8 obtainable surface carriers joined at task p30-w6-t2-surface-blocks (150 + 8 = 158 — GT6SurfaceBlocks 0 -> 8 checked, the plant quartet + the 4 fallen-log woods, atomic keys both locales)"
			+ " ; the Implosion Compressor controller joined at task p31-implosion (158 + 1 = 159 — GTMultiBlocks 28 checked, the atomic block.gt6.implosion_compressor key)"
			+ " ; the Von da Graagg controller joined at task p31-graagg (159 + 1 = 160 — GTMultiBlocks 29 checked, the atomic block.gt6.von_da_graagg key)"
			+ " ; the Large Matter Fabricator controller joined at task p31-massfab (160 + 1 = 161 — GTMultiBlocks 30 checked, the atomic block.gt6.large_massfab key; the five small Massfab row carriers ride the composed-name exemption, GTBasicMachineBlock.getName)"
			+ " ; the Fusion Reactor controller joined at task p31-fusion (161 + 1 = 162 — GTMultiBlocks 31 checked, the atomic block.gt6.fusion_reactor key)"
			+ " ; the 10 laser-domain rungs joined at task p32-qu-laser-domain (162 + 10 = 172 — GT6Lasers 10 checked, the atomic co2_laser/laser_absorber keys; the reused GT6DynamoBlock carrier keeps the vanilla descriptionId, the bridge posture)"
			+ " ; the logistics wire joined at task p32-logistics-lv2 (172 + 1 = 173 — GT6Logistics 1 checked, the atomic block.gt6.logistics_wire key; review seam: the class joins the census walk beside its provider rows)"
			+ " ; the Magic Field Absorber joined at task p32-magic-absorber (173 + 1 = 174 — GT6MagicAbsorbers 1 checked, the atomic block.gt6.magic_absorber key; the rebase reconciliation over the merged logistics state)"
			+ " ; the 5 quantum-energizer rungs joined at task p32-qu-energizer (174 + 5 = 179 — GT6QuantumEnergizers 5 checked, the atomic quantum_energizer keys; the REUSED GT6LaserConverterBlockEntity family keeps the vanilla descriptionId, the third type-pair instance)"
			+ " ; the 8 placeables deco blocks joined at task p32-placeables (179 + 8 = 187 — GT6Placeables 8 checked: the greg_o_lantern + the sandwich + the six placed piles, atomic keys both locales; review seam: the class joins the census walk — the card landed its rows without the walk join, the silent-exemption shape this pin exists to catch)"
			+ " ; the Logistics Core controller joined at task p32-logistics-lv3 (187 + 1 = 188 — GT6Logistics 2 checked, the atomic block.gt6.logistics_core key)"
			+ " ; the 4 QU machine rungs joined as EXEMPT composed row carriers at task p32-qu-scanner-replicator (the checked pin HOLDS at 188 — the molecular_scanner/replicator rungs rode the composed-name exemption the moment they registered, the exempt pin carries the +4; rebase reconciliation over the merged 188 state)"
			+ " ; the Juicer joined at task p33-food-machines-kitchen (188 + 1 = 189 — GT6Kitchen 4 -> 5 checked, the atomic block.gt6.juicer key, the dump gt.multitileentity.32722 face 榨汁机 both locales)"
			+ " — per-class checked: " + tWalkedByClass);
		assertTrue(tMissing.isEmpty(),
			"every registered block's vanilla descriptionId key must exist on BOTH lang faces"
			+ " (Jade resolves block.gt6.<path>; a missing key hovers the raw key): " + tMissing);
	}

	/**
	 * The ore universe template pins (task p30-ore-3-datagen spec ③ — the "26 families x 3
	 * forms" face): every ore form resolves its display name through the
	 * {@code gt6.tagprefix.<prefix_snake>} template (GTMaterialPrefixBlockItem.getName),
	 * normal/broken share the family prefix and small rides OP.oreSmall — so the 74
	 * form-rows per material collapse onto exactly 27 distinct template keys (26 family
	 * prefixes + ore_small), pinned here on BOTH faces with the {@code %s} slot intact.
	 * This is the RegisterEvent-family analogue of the stoneAndRows template pins (the
	 * documented-not-walked composed-name universe note at the registry-coverage gate —
	 * GT6OreBlocks' 3922 RegisterEvent blocks join that exemption roster with this test as
	 * their template pin). Plus the one creative-tab title (GT6OreBlocks.TAB_TITLE_KEY) on
	 * both faces: en = mNameCategory ("Stone Ores", OP.java:1101), zh = the dump itemgroup
	 * row (矿石, the addOreTabTitle family join).
	 */
	@Test
	public void oreUniverseTemplatePins() {
		Set<String> tKeys = new HashSet<>();
		for (GT6OreBlocks.OreFamily tFamily : GT6OreBlocks.FAMILIES) {
			for (GT6OreBlocks.FormKind tKind : tFamily.kinds()) {
				tKeys.add("gt6.tagprefix." + MaterialPrefixItem.snakeCase(tFamily.prefix(tKind).mNameInternal));
			}
		}
		assertEquals(27, tKeys.size(), "26 family prefixes + the shared ore_small = the pinned 27 keys");
		List<String> tBroken = new ArrayList<>();
		for (String tKey : tKeys) {
			if (!en().containsKey(tKey) || !en().get(tKey).contains("%s")) tBroken.add("en/" + tKey);
			if (!zh().containsKey(tKey) || !zh().get(tKey).contains("%s")) tBroken.add("zh/" + tKey);
		}
		assertTrue(tBroken.isEmpty(),
			"the ore template keys must exist with their %s slot on BOTH faces (the 74 form-rows"
			+ " per material compose their names through them): " + tBroken);
		assertEquals("Stone Ores", en().get(GT6OreBlocks.TAB_TITLE_KEY), "the en tab title = mNameCategory (OP.java:1101)");
		assertEquals("矿石", zh().get(GT6OreBlocks.TAB_TITLE_KEY), "the zh tab title = the dump itemgroup oreVanillastone row");
	}
}
