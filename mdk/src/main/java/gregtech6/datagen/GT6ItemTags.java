package gregtech6.datagen;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;

//? if forge {
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.RegistryObject;
//?} else {
/*import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
*///?}

import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GT6Tools;

/**
 * The GT6 item-tag datagen home — task p24-tool-system spec ④, the FIRST TagsProvider of
 * the port (the card ruling decisions.p24-tool-system-recipe-provider-first: the minimal
 * provider lands here, the tags-foundation card TAKES OVER this file and extends it band
 * by band). The base class is vanilla {@link TagsProvider} over {@link Registries#ITEM};
 * the five-argument super constructor is shape-identical on both legs (forge 1.20.1
 * TagsProvider.java:51 / NeoForge 21.1 TagsProvider.java:47 — PackOutput, ResourceKey,
 * lookup future, mod id, nullable ExistingFileHelper), so this file carries ZERO
 * {@code //?} except the {@link Tags} import (the class is not on the stonecutter swap
 * table and moved to {@code c:} constants on NeoForge).
 *
 * <p>Tag-naming ruling (decisions.p24-tool-system-tag-strategy): the crafting-tool
 * ingredients are SELF-OWNED tags translating the upstream oredict names by the
 * snake_case rule — {@code craftingToolFile}/{@code craftingToolSaw}
 * (CS.java:1867/:1864) → {@code #gt6:tools/file}/{@code #gt6:tools/saw}; the recipe
 * materials likewise — {@code dustRedstone} → {@code #gt6:redstone},
 * {@code plateCurvedSn} → {@code #gt6:plate_curved_tin} (vanilla offers no
 * redstone-dust item tag on either leg, only REDSTONE_ORES — the vanilla route is
 * ruled out). The five formal tools ride BOTH their gt6 tag and the ecosystem tag:
 * forge 1.20.1 {@code #forge:tools} (Tags.Items.TOOLS, Tags.java:419 — no saw/file
 * subdivision exists, which is what legitimizes the self-owned pair) and NeoForge 21.1
 * {@code #c:tools} (the same constant, namespace "c" — Tags.java:799/:923).
 *
 * <p>Element validation note: TagsProvider checks element references against the live
 * registry (the forge :114 verifyIfPresent chain over lookupProvider), and datagen runs
 * after mod construction — every gt6 element below is a registered item by then.
 */
public final class GT6ItemTags extends TagsProvider<Item> {

	/** The craftingToolFile oredient translation — #gt6:tools/file (CS.java:1867 snake). */
	public static final TagKey<Item> TOOLS_FILE = gt6("tools/file");

	/** The craftingToolSaw oredient translation — #gt6:tools/saw (CS.java:1864 snake). */
	public static final TagKey<Item> TOOLS_SAW = gt6("tools/saw");

	/**
	 * The builder-wand tool tag — #gt6:tools/builder_wand (task p24-builder-wand, the
	 * TOOLS_FILE/TOOLS_SAW snake shape). Upstream carries NO oredict crafting key for the
	 * wand (id402 proven, the research card) — the tag exists so future relay code and
	 * the tool-family recipe card key on the TAG, not the item.
	 */
	public static final TagKey<Item> TOOLS_BUILDER_WAND = gt6("tools/builder_wand");

	/** The craftingToolScrewdriver oredient translation — #gt6:tools/screwdriver (CS.java:1895 snake). */
	public static final TagKey<Item> TOOLS_SCREWDRIVER = gt6("tools/screwdriver");

	/**
	 * The craftingToolHardHammer oredient translation — #gt6:tools/hard_hammer (task
	 * p25-tool-hammer-wrench spec ④, the TOOLS_FILE/TOOLS_SAW snake shape). Upstream key
	 * {@code OreDictToolNames.hammer = "craftingToolHardHammer"} (CS.java:1890); the
	 * naming ruling (decisions.p25-tool-hammer-wrench-rulings ①) keeps the HARD semantic
	 * — "craftingTool" strips and snakes, "Hammer" hard-hammer precision preserved against
	 * the soft-hammer/electric-hammer ambiguity (display names are not tag evidence).
	 */
	public static final TagKey<Item> TOOLS_HARD_HAMMER = gt6("tools/hard_hammer");

	/** The craftingToolWrench oredient translation — #gt6:tools/wrench (CS.java:1876 snake). */
	public static final TagKey<Item> TOOLS_WRENCH = gt6("tools/wrench");

	/**
	 * The craftingToolWirecutter oredient translation — #gt6:tools/wire_cutter (task
	 * p29-w3-nbtdesign-parts ③ — the coil crafting rows' 'x' letter, CR.java:359
	 * {@code case 'x': OreDictToolNames.wirecutter}; the TOOLS_WRENCH snake shape).
	 */
	public static final TagKey<Item> TOOLS_WIRE_CUTTER = gt6("tools/wire_cutter");

	/**
	 * The craftingToolBendingCylinderSmall oredient translation —
	 * #gt6:tools/bending_cylinder_small (task p25-food-can-row0 spec ②, the TOOLS_FILE
	 * snake shape). Upstream key {@code OreDictToolNames.bendingcylindersmall =
	 * "craftingToolBendingCylinderSmall"} (CS.java:1903); the empty-can crafting row's
	 * 'o' letter (CR.java:207 alphabet) keys on THIS tag.
	 */
	public static final TagKey<Item> TOOLS_BENDING_CYLINDER_SMALL = gt6("tools/bending_cylinder_small");

	/** The craftingToolPickaxe translation — #gt6:tools/pickaxe (task p29-w5-t1-dig-six, the snake rule). */
	public static final TagKey<Item> TOOLS_PICKAXE = gt6("tools/pickaxe");

	/** The gem-pickaxe tool tag — #gt6:tools/pickaxe_gem (task p29-w5-t1-dig-six). */
	public static final TagKey<Item> TOOLS_PICKAXE_GEM = gt6("tools/pickaxe_gem");

	/** The construction-pickaxe tool tag — #gt6:tools/pickaxe_construction (task p29-w5-t1-dig-six). */
	public static final TagKey<Item> TOOLS_PICKAXE_CONSTRUCTION = gt6("tools/pickaxe_construction");

	/** The craftingToolShovel translation — #gt6:tools/shovel (task p29-w5-t1-dig-six). */
	public static final TagKey<Item> TOOLS_SHOVEL = gt6("tools/shovel");

	/** The craftingToolSpade translation — #gt6:tools/spade (task p29-w5-t1-dig-six). */
	public static final TagKey<Item> TOOLS_SPADE = gt6("tools/spade");

	/** The universal-spade tool tag — #gt6:tools/universal_spade (task p29-w5-t1-dig-six). */
	public static final TagKey<Item> TOOLS_UNIVERSAL_SPADE = gt6("tools/universal_spade");

	/** The sword tool tag — #gt6:tools/sword (task p29-w5-t2-blade-six, the snake rule). */
	public static final TagKey<Item> TOOLS_SWORD = gt6("tools/sword");

	/** The knife tool tag — #gt6:tools/knife (task p29-w5-t2-blade-six). */
	public static final TagKey<Item> TOOLS_KNIFE = gt6("tools/knife");

	/** The butchery-knife tool tag — #gt6:tools/butchery_knife (task p29-w5-t2-blade-six). */
	public static final TagKey<Item> TOOLS_BUTCHERY_KNIFE = gt6("tools/butchery_knife");

	/** The club tool tag — #gt6:tools/club (task p29-w5-t2-blade-six). */
	public static final TagKey<Item> TOOLS_CLUB = gt6("tools/club");

	/** The axe tool tag — #gt6:tools/axe (task p29-w5-t2-blade-six). */
	public static final TagKey<Item> TOOLS_AXE = gt6("tools/axe");

	/** The double-axe tool tag — #gt6:tools/axe_double (task p29-w5-t2-blade-six). */
	public static final TagKey<Item> TOOLS_AXE_DOUBLE = gt6("tools/axe_double");
	/**
	 * The craftingToolSoftHammer oredient translation — #gt6:tools/soft_hammer (task
	 * p29-w5-t3-machine-face-four ①, the TOOLS_HARD_HAMMER snake shape). Upstream key
	 * {@code OreDictToolNames.softhammer = "craftingToolSoftHammer"} (CS.java:1891).
	 */
	public static final TagKey<Item> TOOLS_SOFT_HAMMER = gt6("tools/soft_hammer");

	/**
	 * The craftingToolMonkeyWrench oredient translation — #gt6:tools/monkey_wrench (task
	 * p29-w5-t3-machine-face-four ②, the snake shape). The upstream :144 row carries the
	 * DOUBLE oredict name (OreDictToolNames.monkeywrench + wrench) — the port folds onto
	 * the ONE tag (the card's tag ruling: no wrench substitution in recipes).
	 */
	public static final TagKey<Item> TOOLS_MONKEY_WRENCH = gt6("tools/monkey_wrench");

	/**
	 * The craftingToolMagnifyingglass oredient translation — #gt6:tools/magnifying_glass
	 * (task p29-w5-t3-machine-face-four ③, the snake shape; upstream
	 * {@code OreDictToolNames.magnifyingglass}, CS.java TOOL family).
	 */
	public static final TagKey<Item> TOOLS_MAGNIFYING_GLASS = gt6("tools/magnifying_glass");

	/**
	 * The craftingToolPincers oredient translation — #gt6:tools/pincers (task
	 * p29-w5-t3-machine-face-four ④, the snake shape; upstream
	 * {@code OreDictToolNames.pincers = "craftingToolPincers"}, CS.java:1880).
	 */
	public static final TagKey<Item> TOOLS_PINCERS = gt6("tools/pincers");
	/** The craftingToolHoe translation — #gt6:tools/hoe (task p29-w5-t4-field-five, the snake rule). */
	public static final TagKey<Item> TOOLS_HOE = gt6("tools/hoe");

	/** The plow tool tag — #gt6:tools/plow (task p29-w5-t4-field-five). */
	public static final TagKey<Item> TOOLS_PLOW = gt6("tools/plow");

	/** The branch-cutter tool tag — #gt6:tools/branch_cutter (task p29-w5-t4-field-five). */
	public static final TagKey<Item> TOOLS_BRANCH_CUTTER = gt6("tools/branch_cutter");

	/** The sense tool tag — #gt6:tools/sense (task p29-w5-t4-field-five). */
	public static final TagKey<Item> TOOLS_SENSE = gt6("tools/sense");

	/** The hand-drill tool tag — #gt6:tools/hand_drill (task p29-w5-t4-field-five). */
	public static final TagKey<Item> TOOLS_HAND_DRILL = gt6("tools/hand_drill");
	/** The craftingToolScissors/shears dual-key fold — #gt6:tools/scissors (task p29-w5-t5-scene-six, the snake rule; CS.java:1906/:1907). */
	public static final TagKey<Item> TOOLS_SCISSORS = gt6("tools/scissors");

	/** The craftingToolScoop translation — #gt6:tools/scoop (task p29-w5-t5-scene-six, CS.java:1894). */
	public static final TagKey<Item> TOOLS_SCOOP = gt6("tools/scoop");

	/** The craftingToolPlunger translation — #gt6:tools/plunger (task p29-w5-t5-scene-six, CS.java:1892). */
	public static final TagKey<Item> TOOLS_PLUNGER = gt6("tools/plunger");

	/** The craftingFirestarter/flintandtinder registration-row dual key — #gt6:tools/flint_and_tinder (task p29-w5-t5-scene-six, Loader_Tools.java:143). */
	public static final TagKey<Item> TOOLS_FLINT_AND_TINDER = gt6("tools/flint_and_tinder");

	/** The craftingToolRollingPin translation — #gt6:tools/rolling_pin (task p29-w5-t5-scene-six, CS.java:1901). */
	public static final TagKey<Item> TOOLS_ROLLING_PIN = gt6("tools/rolling_pin");

	/** The craftingToolBendingCylinder (LARGE) translation — #gt6:tools/bending_cylinder (task p29-w5-t5-scene-six, CS.java:1902; the Small :1903 keeps its own tag). */
	public static final TagKey<Item> TOOLS_BENDING_CYLINDER = gt6("tools/bending_cylinder");
	// task p29-w5-t6-electric-nineteen — the nineteen electric-tool tags (the same snake
	// band, one member each; the crafting rows and the future consumers key on the TAG).
	public static final TagKey<Item> TOOLS_MINING_DRILL_LV = gt6("tools/mining_drill_lv");
	public static final TagKey<Item> TOOLS_MINING_DRILL_MV = gt6("tools/mining_drill_mv");
	public static final TagKey<Item> TOOLS_MINING_DRILL_HV = gt6("tools/mining_drill_hv");
	public static final TagKey<Item> TOOLS_CHAINSAW_LV = gt6("tools/chainsaw_lv");
	public static final TagKey<Item> TOOLS_CHAINSAW_MV = gt6("tools/chainsaw_mv");
	public static final TagKey<Item> TOOLS_CHAINSAW_HV = gt6("tools/chainsaw_hv");
	public static final TagKey<Item> TOOLS_WRENCH_LV = gt6("tools/wrench_lv");
	public static final TagKey<Item> TOOLS_WRENCH_MV = gt6("tools/wrench_mv");
	public static final TagKey<Item> TOOLS_WRENCH_HV = gt6("tools/wrench_hv");
	public static final TagKey<Item> TOOLS_JACKHAMMER_HV_NORMAL = gt6("tools/jackhammer_hv_normal");
	public static final TagKey<Item> TOOLS_JACKHAMMER_HV_NO_ORES = gt6("tools/jackhammer_hv_no_ores");
	public static final TagKey<Item> TOOLS_BUZZSAW_LV = gt6("tools/buzzsaw_lv");
	public static final TagKey<Item> TOOLS_SCREWDRIVER_LV = gt6("tools/screwdriver_lv");
	public static final TagKey<Item> TOOLS_HAND_DRILL_LV = gt6("tools/hand_drill_lv");
	public static final TagKey<Item> TOOLS_HAND_MIXER_LV = gt6("tools/hand_mixer_lv");
	public static final TagKey<Item> TOOLS_MONKEY_WRENCH_LV = gt6("tools/monkey_wrench_lv");
	public static final TagKey<Item> TOOLS_MONKEY_WRENCH_MV = gt6("tools/monkey_wrench_mv");
	public static final TagKey<Item> TOOLS_MONKEY_WRENCH_HV = gt6("tools/monkey_wrench_hv");
	public static final TagKey<Item> TOOLS_TRIMMER_LV = gt6("tools/trimmer_lv");

	/** The dustRedstone recipe-material translation — #gt6:redstone (snake ruling). */
	public static final TagKey<Item> REDSTONE_DUSTS = gt6("redstone");

	/**
	 * The shelf-book material translation — #gt6:books (task p26-storage-static-batch).
	 * The {@code BooksGT.BOOK_REGISTER.containsKey} modern equivalent: the bookshelf
	 * insert gate keys on THIS tag (datapack tier-a, pack authors extend the shelf's
	 * book universe); the datagen band fills the vanilla book items.
	 */
	public static final TagKey<Item> BOOKS = gt6("books");

	/** The plateCurvedSn recipe-material translation — #gt6:plate_curved_tin (snake ruling). */
	public static final TagKey<Item> PLATE_CURVED_TIN = gt6("plate_curved_tin");

	/**
	 * The extruder-mold family tag — #gt6:extruder_shapes (task p26-w1-press-extruder-molds).
	 * The upstream census has no oredict key for the Shape_Extruder_* items (they match
	 * recipes by exact item), so the tag is the port's family face — the bidirectional
	 * paradigm: this provider fills the MEMBERSHIP (both row0 molds) and the runtime
	 * not-consumable predicate READS it ({@code GT6ExtruderMolds.isMold}, the
	 * TOOLS_BUILDER_WAND tag-face ruling).
	 */
	public static final TagKey<Item> EXTRUDER_SHAPES = gt6("extruder_shapes");

	/**
	 * The platform material-tag namespace (task p24-tags-provider-skeleton, the
	 * decisions.p24-tool-system-tag-strategy namespace face): {@code forge} on 1.20.1 (the
	 * Tags.Items constants — Tags.java:310-312/:220/:256) and {@code c} on NeoForge 21.1
	 * (Tags.java:799/:923). The emitted JSON location follows the tag id's namespace
	 * automatically — {@code data/forge/tags/items/**} vs {@code data/c/tags/items/**} —
	 * which is the whole fork surface: the family paths below are namespace-free.
	 */
	//? if forge {
	public static final String MATERIALS_NAMESPACE = "forge";
	//?} else {
	/*public static final String MATERIALS_NAMESPACE = "c";
	*///?}

	/**
	 * The forward-twin common-tag namespace — {@code c} on BOTH legs (task
	 * p27-vanilla-tag-dual-tree spec ①): the 1.21 canonical namespace (NeoForge docs
	 * resources/server/tags.md:50 "would other mods want to use this tag as well? —
	 * the c namespace"; the Mekanism 1.21.x generated tree is {@code data/c}-only)
	 * produced ahead of time on the 1.20.1 leg for the {@link #VANILLA_INTERSECTION}
	 * face only, while the neo leg's own {@link #MATERIALS_NAMESPACE} already IS c.
	 * The emitted JSON location follows the tag id — {@code data/c/tags/items/**} on
	 * the 1.20.1 leg (the plural directory form the 1.20.1 TagsProvider writes; the
	 * neo leg's own c tree is the singular {@code tags/item} form, natively).
	 */
	public static final String COMMON_NAMESPACE = "c";

	/** The ingot family — GTCEu TagPrefix.java:279 {@code defaultTagPath("ingots/%s")}. */
	public static final String INGOTS_FAMILY = "ingots/%s";

	/** The dust family — GTCEu TagPrefix.java:405 {@code defaultTagPath("dusts/%s")}. */
	public static final String DUSTS_FAMILY = "dusts/%s";

	/** The gem family — GTCEu TagPrefix.java:290 {@code defaultTagPath("gems/%s")}. */
	public static final String GEMS_FAMILY = "gems/%s";

	/** The nugget family — GTCEu TagPrefix.java:416 {@code defaultTagPath("nuggets/%s")}. */
	public static final String NUGGETS_FAMILY = "nuggets/%s";

	/**
	 * The storage-block family — GTCEu TagPrefix.java:727-729, the "Block of %s" prefix
	 * ("consisting out of 9 Ingots/Gems/Dusts") over the GT6 {@code blockIngot}/{@code
	 * blockGem}/{@code blockDust} block items.
	 */
	public static final String STORAGE_BLOCKS_FAMILY = "storage_blocks/%s";

	/**
	 * The raw-storage-block family — GTCEu TagPrefix.java:223, the rawOreBlock prefix
	 * {@code defaultTagPath("storage_blocks/raw_%s")} over the GT6 {@code blockRaw} items.
	 */
	public static final String STORAGE_BLOCKS_RAW_FAMILY = "storage_blocks/raw_%s";

	/** The plate family — GTCEu TagPrefix.java:456 {@code defaultTagPath("plates/%s")} (rolling batch 2). */
	public static final String PLATES_FAMILY = "plates/%s";

	/**
	 * The mold crafting base ingredient — {@code forge:plates/tungsten_carbide} (task
	 * p26-w1-press-extruder-molds): the row0 flattening of the upstream crafting chain
	 * Empty(:182, plateDouble WC) → Rod(:221)/Foil(:222) → Plate(:247) — the Empty/Foil
	 * intermediates are POOLED (not row0 items), so the two row0 molds key their 'P'
	 * ingredient on the upstream chain ROOT's material face (the GT6FoodCans
	 * plate_curved_tin precedent: the upstream OreDict ingredient → the existing platform
	 * material tag, single-sourced here).
	 */
	public static final TagKey<Item> EXTRUDER_SHAPE_BASE = materialTag(PLATES_FAMILY, "tungsten_carbide");

	/**
	 * The rod family — GTCEu TagPrefix.java:502 {@code defaultTagPath("rods/%s")} over the
	 * GT6 {@code stick} prefix (the GT6 rod naming; the GTCEu RODS_WOODEN special case —
	 * ItemTagLoader:87-88, treated-wood-rod-to-forge-rods/wooden — is a recipe-INPUT face
	 * and stays with the recipe-consumer card).
	 */
	public static final String RODS_FAMILY = "rods/%s";

	/** The hot-ingot family — GTCEu TagPrefix.java:267 {@code defaultTagPath("hot_ingots/%s")} (rolling batch 2). */
	public static final String HOT_INGOTS_FAMILY = "hot_ingots/%s";

	/** The double-plate family — GTCEu TagPrefix.java:442 {@code defaultTagPath("double_plates/%s")} (task p26-sensors-core). */
	public static final String DOUBLE_PLATES_FAMILY = "double_plates/%s";

	/** The fine-wire family — GTCEu TagPrefix.java:575 {@code defaultTagPath("fine_wires/%s")} (task p26-sensors-core). */
	public static final String FINE_WIRES_FAMILY = "fine_wires/%s";

	/** The bolt family — GTCEu TagPrefix.java:514 {@code defaultTagPath("bolts/%s")} (task p26-sensors-core). */
	public static final String BOLTS_FAMILY = "bolts/%s";

	/** The small-gear family — GTCEu TagPrefix.java:599 {@code defaultTagPath("small_gears/%s")} (task p26-sensors-core). */
	public static final String SMALL_GEARS_FAMILY = "small_gears/%s";

	/**
	 * The cross-mod material-name normalization map (task p24-tags-prefix-materials, the
	 * decisions.p24-material-name-normalization ruling): GT {@code mNameInternal} snake →
	 * the ecosystem-conventional alias snake. Each entry is backed by the upstream
	 * identical-name alias (OreDictMaterial.put(String) → addIdenticalNames): MT.java:970
	 * Aluminium→"Aluminum", MT.java:2517 AluminiumBrass→"AluminumBrass". The ecosystem face
	 * is NOT platform-maintained (forge 1.20.1 / NeoForge 21.1 Tags.java carry zero aluminum
	 * constants; the c:ingots official generated tree lists only iron/gold/copper/netherite),
	 * so this map is the conservative census-backed MINIMUM; the GTCEu Modern precedent
	 * generates the GT main name only (zero "aluminum" strings in its source tree) — the
	 * alias twin tags below ADD the second convention so both consumer faces meet the GT6
	 * items. Every remaining oredict synonym alias (Co60, Gibbsite, SulphurDioxide, ...) has
	 * no ecosystem tag convention and stays single-named.
	 */
	private static final Map<String, String> ECOSYSTEM_ALIASES = Map.of(
			"aluminium", "aluminum",
			"aluminium_brass", "aluminum_brass");

	/**
	 * The (family, material)-granularity canonical-name map (task p27-vanilla-tag-dual-tree
	 * spec ③ — the ECOSYSTEM_ALIASES dimension lift, the research.p27-vanilla-unify gap ①):
	 * key = the GT-internal formatted tag path ("&lt;familyBase&gt;&lt;materialSnake&gt;", e.g.
	 * {@code gems/nether_quartz}), value = the ecosystem-CANONICAL material snake. Unlike
	 * the name-level {@link #ECOSYSTEM_ALIASES} twins (where the ecosystem has NO enforced
	 * convention, so the GT main name keeps the crown), these entries REPLACE the main tag
	 * name with the platform-canonical one — Forge 1.20.1 ships
	 * {@code Tags.Items.GEMS_QUARTZ = "gems/quartz"} with the vanilla member
	 * (ForgeItemTagsProvider.java:77 {@code tag(Tags.Items.GEMS_QUARTZ).add(Items.QUARTZ)})
	 * and {@code STORAGE_BLOCKS_QUARTZ} (:152) — while the GT-internal
	 * {@code nether_quartz} face is KEPT as the compatibility twin (the grep-verified
	 * reference surface: generated-tree-only, 7 JSON files, ZERO main/test readers —
	 * pure datapack compat). Families WITHOUT an ecosystem quartz tag (dusts / rods /
	 * bolts / small_gears / the raw storage) stay single-named {@code nether_quartz} —
	 * this map keys them out.
	 */
	private static final Map<String, String> FAMILY_MATERIAL_CANONICAL = Map.of(
			"gems/nether_quartz", "quartz",
			"storage_blocks/nether_quartz", "quartz");

	/**
	 * The vanilla-intersection face of the forward twin tree (task
	 * p27-vanilla-tag-dual-tree spec ① — the research.p27-vanilla-unify census, every
	 * entry re-verified): the (family, material) tags where BOTH faces exist — this port
	 * emits the material tag (the registration walk) AND the platform ships vanilla
	 * members into the same tag id (ForgeItemTagsProvider.java:56-157 — ingots :89-:92,
	 * nuggets :99-:100, gems :72-:77, dusts :56-:59 (the default dusts are EXACTLY
	 * glowstone/prismarine/redstone — NO clay, the research census line over-listed),
	 * storage_blocks :143-:157) — so the runtime merge closes the unification loop
	 * ({@code #forge:ingots/iron = [minecraft:iron_ingot, gt6:ingot_iron]}, the
	 * GT6RecipeTagFallbackTest.java:53 precedent). 26 paths, in CANONICAL names (quartz,
	 * not nether_quartz). Deliberately NOT listed: gems/prismarine and the
	 * storage_blocks/diamond|emerald faces (the task card's census list names five gems
	 * and twelve storage blocks — the wider platform-shipped faces stay a future card),
	 * the raw_materials / blaze|wooden rods (no GT item path), and every non-vanilla
	 * material (the ruling: ONLY the vanilla intersection enters this card).
	 */
	private static final Set<String> VANILLA_INTERSECTION = Set.of(
			"ingots/iron", "ingots/copper", "ingots/gold", "ingots/netherite",
			"nuggets/iron", "nuggets/gold",
			"gems/diamond", "gems/emerald", "gems/lapis", "gems/amethyst", "gems/quartz",
			"dusts/redstone", "dusts/glowstone", "dusts/prismarine",
			"storage_blocks/iron", "storage_blocks/gold", "storage_blocks/copper",
			"storage_blocks/netherite", "storage_blocks/amethyst", "storage_blocks/lapis",
			"storage_blocks/coal", "storage_blocks/redstone", "storage_blocks/quartz",
			"storage_blocks/raw_iron", "storage_blocks/raw_gold", "storage_blocks/raw_copper");

	public GT6ItemTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookupProvider,
			ExistingFileHelper aExistingFileHelper) {
		super(aOutput, Registries.ITEM, aLookupProvider, GT6DataGenerators.MOD_ID, aExistingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider aProvider) {
		// The takeover seam: the tags-foundation card appends its own add*Tags(aProvider)
		// bands AFTER the tool band, one band per logical family (the GT6EnUs table-tail
		// append convention); the hoist clause has fired — itemTagFamily is the shared
		// static (task p25-tag-input-machine-fallback, the second caller is Recipe).
		addToolTags(aProvider);
		addMaterialTags(aProvider);
		addGrassTags(aProvider); // task p24-grass-block
		addBatteryTags(aProvider); // task p29-w4-battery-storage — the re-battery/re-crystal/circuit tag seams
	}

	/**
	 * The grass-family item band (task p24-grass-block): the 6 grass BLOCK ITEMS join the
	 * vanilla {@code minecraft:dirt} ITEM tag ({@link net.minecraft.tags.ItemTags#DIRT};
	 * the vanilla tag file carries exactly the nine dirt-family block items —
	 * vanilla-1.20.1 data/minecraft/tags/items/dirt.json). This is the item-identity half
	 * of the per-pair split: dye recipes keying the block tag would be meaningless without
	 * the item face. The six animal spawnable block tags and valid_spawn have no item-side
	 * counterpart and stay unjoined on the block face too (the GT6BlockTags.addGrassBand
	 * absence ruling).
	 */
	private void addGrassTags(HolderLookup.Provider aProvider) {
		for (String tPath : gregtech6.registry.GTGrassBlocks.PATHS) {
			tag(net.minecraft.tags.ItemTags.DIRT).add(item(gt6Rl(tPath)));
		}
	}

	/**
	 * The battery-family tag band (task p29-w4-battery-storage): the upstream oredict seams
	 * translated — {@code gt:re-battery0..4}/{@code gt:re-crystal0..5} (the Loader
	 * :1009-:1092 tail columns; the W5 electric-tool capacity-sum face iterates exactly
	 * these, Loader_Tools.java:356-377) become {@code #gt6:re_battery0..4}/
	 * {@code #gt6:re_crystal0..5} (the dash→underscore snake rule, the tier digits stay
	 * attached) and the circuit ladder {@code OD_CIRCUITS[i] = "gt:circuit0..9"} (upstream
	 * CS.java:166) becomes {@code #gt6:circuit0..6} over the seven carrier items. The
	 * recipe rows key their 'C' column on the TAG (the oredict semantics — any item of
	 * that circuit tier matches), not the carrier item.
	 */
	private void addBatteryTags(HolderLookup.Provider aProvider) {
		for (gregtech6.registry.GT6Batteries.BatteryRow tRow : gregtech6.registry.GT6Batteries.ROWS) {
			tag(gt6(tRow.tagPath())).add(item(gt6Rl(tRow.path())));
		}
		for (gregtech6.registry.GT6Batteries.CircuitRow tRow : gregtech6.registry.GT6Batteries.CIRCUIT_ROWS) {
			tag(gt6(tRow.tagPath())).add(item(gt6Rl(tRow.path())));
		}
	}

	/**
	 * The p24 tool band: the crafting-tool tags (one member each — the tag exists so
	 * recipes and future relay code key on the TAG, not the item; the file/saw pair from
	 * p24-tool-system, the screwdriver from p24-screwdriver-item, the hard-hammer/wrench
	 * pair from p25-tool-hammer-wrench, the bending cylinder from p25-food-can-row0),
	 * the two recipe-material tags, and the ecosystem append (the ten formal tools into
	 * the platform tools tag — the user ruling's bidirectional face; the platform
	 * constant resolves to {@code forge:tools} on 1.20.1 and {@code c:tools} on 1.21.1).
	 */
	private void addToolTags(HolderLookup.Provider aProvider) {
		tag(TOOLS_FILE).add(item(GT6Tools.FILE.getId()));
		tag(TOOLS_SAW).add(item(GT6Tools.SAW.getId()));
		tag(TOOLS_BUILDER_WAND).add(item(GT6Tools.BUILDER_WAND.getId()));
		tag(TOOLS_SCREWDRIVER).add(item(GT6Tools.SCREWDRIVER.getId())); // task p24-screwdriver-item — the craftingToolScrewdriver snake
		tag(TOOLS_HARD_HAMMER).add(item(GT6Tools.HAMMER.getId())); // task p25-tool-hammer-wrench — the craftingToolHardHammer snake
		tag(TOOLS_WRENCH).add(item(GT6Tools.WRENCH.getId())); // task p25-tool-hammer-wrench — the craftingToolWrench snake
		tag(TOOLS_WIRE_CUTTER).add(item(GT6Tools.CUTTER.getId())); // task p29-w3-nbtdesign-parts — the CR 'x' wirecutter letter
		tag(TOOLS_BENDING_CYLINDER_SMALL).add(item(GT6Tools.BENDING_CYLINDER_SMALL.getId())); // task p25-food-can-row0 — the craftingToolBendingCylinderSmall snake
		// task p29-w5-t1-dig-six — the six dig-tool tags (one member each, the p24 band shape)
		tag(TOOLS_PICKAXE).add(item(GT6Tools.PICKAXE.getId()));
		tag(TOOLS_PICKAXE_GEM).add(item(GT6Tools.PICKAXE_GEM.getId()));
		tag(TOOLS_PICKAXE_CONSTRUCTION).add(item(GT6Tools.PICKAXE_CONSTRUCTION.getId()));
		tag(TOOLS_SHOVEL).add(item(GT6Tools.SHOVEL.getId()));
		tag(TOOLS_SPADE).add(item(GT6Tools.SPADE.getId()));
		tag(TOOLS_UNIVERSAL_SPADE).add(item(GT6Tools.UNIVERSAL_SPADE.getId()));
		// task p29-w5-t2-blade-six — the six blade-tool tags (one member each, the p24 band shape)
		tag(TOOLS_SWORD).add(item(GT6Tools.SWORD.getId()));
		tag(TOOLS_KNIFE).add(item(GT6Tools.KNIFE.getId()));
		tag(TOOLS_BUTCHERY_KNIFE).add(item(GT6Tools.BUTCHERY_KNIFE.getId()));
		tag(TOOLS_CLUB).add(item(GT6Tools.CLUB.getId()));
		tag(TOOLS_AXE).add(item(GT6Tools.AXE.getId()));
		tag(TOOLS_AXE_DOUBLE).add(item(GT6Tools.AXE_DOUBLE.getId()));
		tag(TOOLS_SOFT_HAMMER).add(item(GT6Tools.SOFT_HAMMER.getId())); // task p29-w5-t3-machine-face-four — the craftingToolSoftHammer snake
		tag(TOOLS_MONKEY_WRENCH).add(item(GT6Tools.MONKEY_WRENCH.getId())); // task p29-w5-t3-machine-face-four — the single-name ruling (no wrench fold)
		tag(TOOLS_MAGNIFYING_GLASS).add(item(GT6Tools.MAGNIFYING_GLASS.getId())); // task p29-w5-t3-machine-face-four — the craftingToolMagnifyingglass snake
		tag(TOOLS_PINCERS).add(item(GT6Tools.PINCERS.getId())); // task p29-w5-t3-machine-face-four — the craftingToolPincers snake
		// task p29-w5-t4-field-five — the five field-tool tags (one member each, the t1 band shape)
		tag(TOOLS_HOE).add(item(GT6Tools.HOE.getId()));
		tag(TOOLS_PLOW).add(item(GT6Tools.PLOW.getId()));
		tag(TOOLS_BRANCH_CUTTER).add(item(GT6Tools.BRANCH_CUTTER.getId()));
		tag(TOOLS_SENSE).add(item(GT6Tools.SENSE.getId()));
		tag(TOOLS_HAND_DRILL).add(item(GT6Tools.HAND_DRILL.getId()));
		// task p29-w5-t5-scene-six — the six scene-tool tags (one member each, the same band shape)
		tag(TOOLS_SCISSORS).add(item(GT6Tools.SCISSORS.getId()));
		tag(TOOLS_SCOOP).add(item(GT6Tools.SCOOP.getId()));
		tag(TOOLS_PLUNGER).add(item(GT6Tools.PLUNGER.getId()));
		tag(TOOLS_FLINT_AND_TINDER).add(item(GT6Tools.FLINT_AND_TINDER.getId()));
		tag(TOOLS_ROLLING_PIN).add(item(GT6Tools.ROLLING_PIN.getId()));
		tag(TOOLS_BENDING_CYLINDER).add(item(GT6Tools.BENDING_CYLINDER.getId()));
		// task p29-w5-t6-electric-nineteen — the nineteen electric tags (the p24 band shape)
		tag(TOOLS_MINING_DRILL_LV).add(item(GT6Tools.MINING_DRILL_LV.getId()));
		tag(TOOLS_MINING_DRILL_MV).add(item(GT6Tools.MINING_DRILL_MV.getId()));
		tag(TOOLS_MINING_DRILL_HV).add(item(GT6Tools.MINING_DRILL_HV.getId()));
		tag(TOOLS_CHAINSAW_LV).add(item(GT6Tools.CHAINSAW_LV.getId()));
		tag(TOOLS_CHAINSAW_MV).add(item(GT6Tools.CHAINSAW_MV.getId()));
		tag(TOOLS_CHAINSAW_HV).add(item(GT6Tools.CHAINSAW_HV.getId()));
		tag(TOOLS_WRENCH_LV).add(item(GT6Tools.WRENCH_LV.getId()));
		tag(TOOLS_WRENCH_MV).add(item(GT6Tools.WRENCH_MV.getId()));
		tag(TOOLS_WRENCH_HV).add(item(GT6Tools.WRENCH_HV.getId()));
		tag(TOOLS_JACKHAMMER_HV_NORMAL).add(item(GT6Tools.JACKHAMMER_HV_NORMAL.getId()));
		tag(TOOLS_JACKHAMMER_HV_NO_ORES).add(item(GT6Tools.JACKHAMMER_HV_NO_ORES.getId()));
		tag(TOOLS_BUZZSAW_LV).add(item(GT6Tools.BUZZSAW_LV.getId()));
		tag(TOOLS_SCREWDRIVER_LV).add(item(GT6Tools.SCREWDRIVER_LV.getId()));
		tag(TOOLS_HAND_DRILL_LV).add(item(GT6Tools.HAND_DRILL_LV.getId()));
		tag(TOOLS_HAND_MIXER_LV).add(item(GT6Tools.HAND_MIXER_LV.getId()));
		tag(TOOLS_MONKEY_WRENCH_LV).add(item(GT6Tools.MONKEY_WRENCH_LV.getId()));
		tag(TOOLS_MONKEY_WRENCH_MV).add(item(GT6Tools.MONKEY_WRENCH_MV.getId()));
		tag(TOOLS_MONKEY_WRENCH_HV).add(item(GT6Tools.MONKEY_WRENCH_HV.getId()));
		tag(TOOLS_TRIMMER_LV).add(item(GT6Tools.TRIMMER_LV.getId()));
		tag(REDSTONE_DUSTS).add(item(gt6Rl("dust_redstone")));
		tag(PLATE_CURVED_TIN).add(item(gt6Rl("plate_curved_tin")));
		// task p26-w1-press-extruder-molds — the mold-family membership face (the row0 pair,
		// upstream meta 10001/:212; the not-consumable predicate's read side). The entry
		// handle rides the SIMPLE-NAME import (the stonecutter rewrite touches imports, not
		// inline qualified names — the neo-leg compile break lesson, the GT6BlockTags form).
		for (RegistryObject<Item> tMold : gregtech6.registry.GT6ExtruderMolds.MOLDS) {
			tag(EXTRUDER_SHAPES).add(item(tMold.getId()));
		}
		// task p26-storage-static-batch — the shelf-book band: the vanilla book family (the
		// BooksGT.BOOK_REGISTER modern equivalent; pack authors extend the shelf universe)
		tag(BOOKS).add(
				item(net.minecraft.resources.ResourceLocation.withDefaultNamespace("book")),
				item(net.minecraft.resources.ResourceLocation.withDefaultNamespace("writable_book")),
				item(net.minecraft.resources.ResourceLocation.withDefaultNamespace("written_book")),
				item(net.minecraft.resources.ResourceLocation.withDefaultNamespace("enchanted_book")));
		tag(Tags.Items.TOOLS).add(
				item(GT6Tools.FILE.getId()), item(GT6Tools.SAW.getId()),
				item(GT6Tools.CROWBAR.getId()), item(GT6Tools.CUTTER.getId()), item(GT6Tools.CHISEL.getId()),
				item(GT6Tools.BUILDER_WAND.getId()), item(GT6Tools.SCREWDRIVER.getId()),
				item(GT6Tools.HAMMER.getId()), item(GT6Tools.WRENCH.getId()), // task p25-tool-hammer-wrench — the family band 7 → 9
				item(GT6Tools.BENDING_CYLINDER_SMALL.getId()), // task p25-food-can-row0 — the family band 9 → 10
				item(GT6Tools.PICKAXE.getId()), item(GT6Tools.PICKAXE_GEM.getId()), item(GT6Tools.PICKAXE_CONSTRUCTION.getId()),
				item(GT6Tools.SHOVEL.getId()), item(GT6Tools.SPADE.getId()), item(GT6Tools.UNIVERSAL_SPADE.getId()), // task p29-w5-t1-dig-six — the family band 10 → 16
				item(GT6Tools.SWORD.getId()), item(GT6Tools.KNIFE.getId()), item(GT6Tools.BUTCHERY_KNIFE.getId()),
				item(GT6Tools.CLUB.getId()), item(GT6Tools.AXE.getId()), item(GT6Tools.AXE_DOUBLE.getId()), // task p29-w5-t2-blade-six — the family band 16 → 22
				item(GT6Tools.HOE.getId()), item(GT6Tools.BRANCH_CUTTER.getId()), item(GT6Tools.SENSE.getId()),
				item(GT6Tools.PLOW.getId()), item(GT6Tools.HAND_DRILL.getId()), // task p29-w5-t4-field-five — the family band 22 → 27 (the machine-face four stay the t3 one-member tag shape, outside the family band)
				item(GT6Tools.SCISSORS.getId()), item(GT6Tools.SCOOP.getId()), item(GT6Tools.PLUNGER.getId()),
				item(GT6Tools.FLINT_AND_TINDER.getId()), item(GT6Tools.ROLLING_PIN.getId()), item(GT6Tools.BENDING_CYLINDER.getId())); // task p29-w5-t5-scene-six — the family band 27 → 33
				item(GT6Tools.SHOVEL.getId()), item(GT6Tools.SPADE.getId()), item(GT6Tools.UNIVERSAL_SPADE.getId())); // task p29-w5-t1-dig-six — the family band 10 → 16
		// task p29-w5-t6-electric-nineteen — the family band 16 → 35 (the electric nineteen)
		for (RegistryObject<Item> tRow : GT6Tools.ELECTRIC_TOOLS) {
			tag(Tags.Items.TOOLS).add(item(tRow.getId()));
		}
	}

	/**
	 * The p24 tags-foundation material band + the p24-tags-prefix-materials rolling batch 2:
	 * one {@code <platform>:<family>/<material>} tag per (family, material) that actually has
	 * a registered item, strictly NO {@code addOptional} (TagsProvider.java:85-94 throws on
	 * a missing reference, and every id below is a live-registered item at datagen time —
	 * both walks are the registration walks, so a gap fails runData loudly). Family paths
	 * are the GTCEu {@code defaultTagPath} precedents (the constants above); the material
	 * segment is the GT {@code mNameInternal} snake — the same composition as the item ids
	 * ({@link GTMaterialItems#itemIdOf}), keeping tag and member derivable from one rule;
	 * cross-mod name normalization rides {@link #ECOSYSTEM_ALIASES} (the alias twin tags).
	 * The namespace is {@link #MATERIALS_NAMESPACE}; a future band that needs to COPY block
	 * tags migrates to the {@code contentsGetter()} ItemTagsProvider shape (vanilla
	 * TagsProvider.java:119-121, RemoveTagDatagenTest.java:58) — this batch has only element
	 * members, so the plain TagsProvider surface stays.
	 *
	 * <p>storage_blocks union closure (the census open_items item 4, the
	 * decisions.p24-material-name-normalization text): the multi-prefix union
	 * (blockIngot/blockGem/blockDust → one storage_blocks/&lt;mat&gt; tag) is the skeleton
	 * ruling kept verbatim; the MATERIAL-GROUP mapping closes as — alias slots
	 * (addIdenticalNames, ID=-1, mTargetRegistration-merged) produce NO second tag name and
	 * NO second member (the walk's {@code MaterialRegistry.get} merge already guarantees
	 * it), and ANY.* group materials produce no items and stay out (the isGeneratingItem
	 * gate), so the per-concrete-material face IS the whole strategy.
	 */
	private void addMaterialTags(HolderLookup.Provider aProvider) {
		// item-path families: the ingot/dust/gem/nugget/plate/rod/hot-ingot items over the
		// material universe
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			addFamilyFace(itemTagFamily(tPair.prefix()), tPair);
		}
		// storage-block families: the blockPath prefixes' block ITEMS (the registered truth,
		// not the offline walk — a defensively-deduped id has no item and must not dangle)
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.items().keySet()) {
			addFamilyFace(storageTagFamily(tPair.prefix()), tPair);
		}
	}

	/**
	 * One (family, pair) face — the p27 dual-name closure: the CANONICAL-name platform tag
	 * ({@link #FAMILY_MATERIAL_CANONICAL} overrides where the ecosystem enforces a name —
	 * quartz, not nether_quartz), plus the compatibility twins — the GT-internal name when
	 * the canonical differs, and the {@link #ECOSYSTEM_ALIASES} alias snake — all holding
	 * the same live-registered item element (the storage_blocks union form: direct
	 * members, no tag-to-tag references, values stay all-primitives). On the forge leg the
	 * {@link #VANILLA_INTERSECTION} canonical face additionally emits the forward twin tag
	 * in {@link #COMMON_NAMESPACE} (spec ①: the second {@code data/c/tags/items} tree, same
	 * members) — the neo leg skips it: its own {@link #MATERIALS_NAMESPACE} already IS c,
	 * the whole family walk there rides the c namespace natively.
	 */
	private void addFamilyFace(String aFamilyPath, GTMaterialItems.PrefixMaterial aPair) {
		if (aFamilyPath == null) return;
		ResourceKey<Item> tMember = item(gt6Rl(GTMaterialItems.itemIdOf(aPair.prefix(), aPair.material())));
		String tSnake = GTMaterialItems.snakeCase(aPair.material().mNameInternal);
		String tCanonical = canonicalMaterialName(aFamilyPath, tSnake);
		tag(materialTag(aFamilyPath, tCanonical)).add(tMember);
		if (!tCanonical.equals(tSnake)) {
			// the GT-internal-name twin — the generated-tree face carried over verbatim
			// (zero code references, the p27 grep; pure datapack compat for existing packs)
			tag(materialTag(aFamilyPath, tSnake)).add(tMember);
		}
		String tAlias = ECOSYSTEM_ALIASES.get(tSnake);
		if (tAlias != null) {
			tag(materialTag(aFamilyPath, tAlias)).add(tMember);
		}
		//? if forge {
		String tMainPath = aFamilyPath.formatted(tCanonical);
		if (VANILLA_INTERSECTION.contains(tMainPath)) {
			tag(commonTag(aFamilyPath, tCanonical)).add(tMember);
		}
		//?}
	}

	/**
	 * The (family, material)-granularity canonical-name lookup — the formatted GT-internal
	 * path keys {@link #FAMILY_MATERIAL_CANONICAL}; families without an ecosystem-enforced
	 * name keep the GT snake verbatim.
	 */
	private static String canonicalMaterialName(String aFamilyPath, String aMaterialSnake) {
		return FAMILY_MATERIAL_CANONICAL.getOrDefault(aFamilyPath.formatted(aMaterialSnake), aMaterialSnake);
	}

	/** The platform tag key of one (family, material) face — {@code <namespace>:<family>/<materialSnake>}. */
	public static TagKey<Item> materialTag(String aFamilyPath, OreDictMaterial aMaterial) {
		return materialTag(aFamilyPath, GTMaterialItems.snakeCase(aMaterial.mNameInternal));
	}

	/**
	 * The snake-parameter form — the overload the alias twins ride. The composed path lives
	 * in a local so both ctor args are bare identifiers — the stonecutter two-arg-ctor shift
	 * deliberately skips parenthesized argument expressions (mdk/stonecutter.gradle.kts regex
	 * note), so an inline formatted(...) argument would stay un-shifted and break the 21.1
	 * leg compile.
	 */
	public static TagKey<Item> materialTag(String aFamilyPath, String aMaterialSnake) {
		String tPath = aFamilyPath.formatted(aMaterialSnake);
		// the 1.20.1 two-arg ctor form; shifted to fromNamespaceAndPath on the 21.1 leg
		return TagKey.create(Registries.ITEM, new ResourceLocation(MATERIALS_NAMESPACE, tPath));
	}

	/**
	 * The forward-twin tag key — same composition as {@link #materialTag(String, String)}
	 * but in {@link #COMMON_NAMESPACE} (task p27-vanilla-tag-dual-tree spec ①). Consumed
	 * on the forge leg only (the {@code //? if forge} band in {@link #addFamilyFace});
	 * the path lives in a local so both ctor args are bare identifiers — the stonecutter
	 * two-arg-ctor shift deliberately skips parenthesized argument expressions
	 * (mdk/stonecutter.gradle.kts regex note).
	 */
	public static TagKey<Item> commonTag(String aFamilyPath, String aMaterialSnake) {
		String tPath = aFamilyPath.formatted(aMaterialSnake);
		// the 1.20.1 two-arg ctor form; shifted to fromNamespaceAndPath on the 21.1 leg
		return TagKey.create(Registries.ITEM, new ResourceLocation(COMMON_NAMESPACE, tPath));
	}

	/**
	 * The item-path family path of a prefix, or null when the prefix carries no P0 platform
	 * tag. SHARED static (the :159-161 takeover hoist, task p25-tag-input-machine-fallback —
	 * the second caller is the machine-side tag fallback in {@code gregtech6.recipes.Recipe}):
	 * the family list itself stays a datagen-side census ("this card writes no family list",
	 * the rolling prefix cards grow it and the fallback inherits automatically).
	 */
	public static String itemTagFamily(OreDictPrefix aPrefix) {
		if (aPrefix == OP.ingot) return INGOTS_FAMILY;
		if (aPrefix == OP.dust) return DUSTS_FAMILY;
		if (aPrefix == OP.gem) return GEMS_FAMILY;
		if (aPrefix == OP.nugget) return NUGGETS_FAMILY;
		// rolling batch 2 (p24-tags-prefix-materials)
		if (aPrefix == OP.plate) return PLATES_FAMILY;
		if (aPrefix == OP.stick) return RODS_FAMILY;
		if (aPrefix == OP.ingotHot) return HOT_INGOTS_FAMILY;
		// rolling batch 3 (p26-sensors-core — the three sensor crafting rows' shared keys)
		if (aPrefix == OP.plateDouble) return DOUBLE_PLATES_FAMILY;
		if (aPrefix == OP.wireFine) return FINE_WIRES_FAMILY;
		if (aPrefix == OP.bolt) return BOLTS_FAMILY;
		if (aPrefix == OP.gearGtSmall) return SMALL_GEARS_FAMILY;
		return null;
	}

	/**
	 * The storage-block family path of a prefix, or null when the prefix is not a storage
	 * block (plate/solid). The union itself (three prefixes → one storage_blocks/%s tag) is
	 * the skeleton ruling; the material-GROUP closure text lives on {@link #addMaterialTags}.
	 */
	private static String storageTagFamily(OreDictPrefix aPrefix) {
		if (aPrefix == OP.blockIngot || aPrefix == OP.blockGem || aPrefix == OP.blockDust) return STORAGE_BLOCKS_FAMILY;
		if (aPrefix == OP.blockRaw) return STORAGE_BLOCKS_RAW_FAMILY;
		return null;
	}

	// ------------------------------------------------------------------ shared helpers

	/** The gt6-namespaced tag key factory (single-source so no path can drift). */
	public static TagKey<Item> gt6(String aPath) {
		return TagKey.create(Registries.ITEM, gt6Rl(aPath));
	}

	/** The gt6-namespaced resource location factory (the 1.20.1 two-arg ctor form). */
	public static ResourceLocation gt6Rl(String aPath) {
		return new ResourceLocation(GT6DataGenerators.MOD_ID, aPath);
	}

	/** The element face TagsProvider appends with — a registry key over the gt6 id. */
	private static ResourceKey<Item> item(ResourceLocation aId) {
		return ResourceKey.create(Registries.ITEM, aId);
	}
}
