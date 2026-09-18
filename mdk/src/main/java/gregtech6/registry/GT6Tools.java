package gregtech6.registry;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.items.tools.GT6BendingCylinderItem;
import gregtech6.items.armor.GT6ArmorItem;
import gregtech6.items.armor.GT6ArmorMaterials;
import gregtech6.items.tools.GT6BendingCylinderSmallItem;
import gregtech6.items.tools.GT6BuilderWandItem;
import gregtech6.items.tools.GT6FileItem;
import gregtech6.items.tools.GT6ScrewdriverItem;
import gregtech6.items.tools.GTAxeDoubleItem;
import gregtech6.items.tools.GTAxeItem;
import gregtech6.items.tools.GTButcheryKnifeItem;
import gregtech6.items.tools.GTClubItem;
import gregtech6.items.tools.GTKnifeItem;
import gregtech6.items.tools.GTBranchCutterItem;
import gregtech6.items.tools.GTHandDrillItem;
import gregtech6.items.tools.GTHoeItem;
import gregtech6.items.tools.GTPlowItem;
import gregtech6.items.tools.GTSenseItem;
import gregtech6.items.tools.GTPickaxeConstructionItem;
import gregtech6.items.tools.GTPickaxeGemItem;
import gregtech6.items.tools.GTPickaxeItem;
import gregtech6.items.tools.GTSwordItem;
import gregtech6.items.tools.GTFlintAndTinderItem;
import gregtech6.items.tools.GTPlungerItem;
import gregtech6.items.tools.GTRollingPinItem;
import gregtech6.items.tools.GTScoopItem;
import gregtech6.items.tools.GTScissorsItem;
import gregtech6.items.tools.GTShovelItem;
import gregtech6.items.tools.GTSpadeItem;
import gregtech6.items.tools.GTUniversalSpadeItem;
import gregtech6.items.tools.GTHammerItem;
import gregtech6.items.tools.GTSawItem;
import gregtech6.items.tools.GTChiselItem;
import gregtech6.items.tools.GTCrowbarItem;
import gregtech6.items.tools.GTCutterItem;
import gregtech6.items.tools.GTMagnifyingGlassItem;
import gregtech6.items.tools.GTMonkeyWrenchItem;
import gregtech6.items.tools.GTPincersItem;
import gregtech6.items.tools.GTSoftHammerItem;
import gregtech6.items.tools.GTWrenchItem;
import gregtech6.items.tools.electric.GT6ElectricToolItem;
import gregtech6.items.tools.pocket.GTPocketMultitoolItem;

/**
 * The GT6 tool registration home — task p9-tool-crowbar spec ③, the ADR
 * 2026-09-01-p9-tool-crowbar ① surface. Card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event
 * (the GTBarrels precedent, ADR 2026-08-31-p8-prefixblocks): GT6Mod.java /
 * GTModBusListener.java stay untouched.
 *
 * <p>Task p10-tool-creative-tab adds the self-owned "Tools" creative tab
 * ({@link #TOOLS_TAB}, id {@code gt6:tools}) over a table-driven
 * {@code displayItems} ({@link #TAB_TABLE}, the GTWires.ELECTRIC_WIRES_TAB form, the
 * W1 ADR ①c) — the p9 ADR cut ("the item is {@code /give} reachable") closes here.
 *
 * <p>Declared pool cuts (ADR 2026-09-01-p10-tools-covers-split):
 * <ol>
 * <li><b>Crowbar crafting recipe</b> — upstream shapes {@code {"hVS","VSV","SVf"}} per
 *     material, requiring the {@code h} hammer + {@code f} file TOOL PIECES plus a blue
 *     dye auxiliary (Loader_Tools.java:314, the OreProcessing_Tool row over
 *     toolHeadWrench); the port has no tool-piece item family and inventing vanilla
 *     substitutes is not done — unlocks with the tool-family card.</li>
 * <li><b>Crowbar material ladder</b> — UNLOCKED by task p31-identity-seam (was: the
 *     single steel tier at durability 512). Upstream registers ONE meta id with an
 *     NBT-chosen material (the whole ToolsGT block Loader_Tools.java:114-145; the
 *     crowbar row :128 carries {@code setMaterialAmount(3*U2)}); the port isomorph
 *     stores the material identity in the {@code gregtech6.itemdata} seam
 *     ({@code GT.ToolStats}, the upstream MultiItemTool.java:192 compound) and scales
 *     durability per material ({@link GTCrowbarItem#durabilityPoints}, :182 at the
 *     pinned 100 units = 1 point ratio — Steel stays at the ADR 512). The per-material
 *     CRAFTING rows still unlock with the tool-family card (cut ① above).</li>
 * <li><b>Crowbar runtime tint</b> — UNLOCKED with the same card (was: untinted single
 *     texture): the material RGBa recolouring over the seam identity, upstream
 *     {@code getRGBa} primary {@code mRGBaSolid} with the steel fallback verbatim
 *     (GT_Tool_Crowbar.java:146-149, gregtech/items/tools/machine/), registered from
 *     GTClientHandlers.</li>
 * </ol>
 *
 * <p>Task p31-single-tier-ruling walked every registration row below and ruled, per
 * item, either PERMANENT single tier (no material identity, zero {@code GT6ItemData}
 * seam consumption) or LADDER CANDIDATE (recorded in decisions.p31-single-tier-ruling).
 * The upstream test: the material axis exists (per-material obtainment rows, the
 * OreProcessing_Tool table Loader_Tools.java:293-330 and the material-parameterized
 * recipes around it) and the tool is neither EU-bound (the electric nineteen ride the
 * voltage axis) nor identity-blind (the flint-and-tinder's closed striker list). The
 * per-row verdicts sit in the row javadocs below.</p>
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Tools {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/**
	 * The formal crowbar — item id {@code gt6:crowbar}. Base durability 512 = the
	 * IDENTITY-LESS legacy value (the ADR pin); identity-carrying stacks scale per
	 * material through the p31-identity-seam (upstream Loader_Tools:128 row,
	 * {@link GTCrowbarItem#getMaxDamage}). Durability semantics: one vanilla point per
	 * 10000 upstream tool-damage units, so one cover dismantle = one point.
	 */
	public static final RegistryObject<Item> CROWBAR = ITEMS.register("crowbar",
			() -> new GTCrowbarItem(new Item.Properties().durability(GTCrowbarItem.DURABILITY_POINTS)));

	/**
	 * The formal wire cutter — item id {@code gt6:cutter} (task p10-tool-cutter spec ③).
	 * Single steel tier, durability 512 (upstream {@code 4*U} material-scaled,
	 * Loader_Tools.java:131 — the ladder UNLOCKED by task p31-machine-ladder, the
	 * {@link gregtech6.items.tools.GT6ToolLadder} faces over the {@code GT.ToolStats}
	 * identity; the identity-less arm keeps the 512).
	 * Upstream display name "Wire Cutter" (the same :131 registration row); no attack
	 * attributes — the cutter is not a weapon (GT_Tool_WireCutter :53-65 cut).
	 */
	public static final RegistryObject<Item> CUTTER = ITEMS.register("cutter",
			() -> new GTCutterItem(new Item.Properties().durability(GTCutterItem.DURABILITY_POINTS)));

	/**
	 * The formal chisel — item id {@code gt6:chisel} (task p16-chisel-decalcify spec ①).
	 * Single steel tier, durability 512 (the crowbar/cutter pinned family value; the
	 * material ladder UNLOCKED by task p31-machine-ladder, the identity-less arm keeps
	 * the 512).
	 * Upstream display name "Chisel" (the same :142 registration row); the decalcify
	 * durability mapping is the upstream behaviour's mDamage=25 conversion
	 * (GT_Tool_Chisel.java:98, see {@link GTChiselItem#durabilityPoints}).
	 */
	public static final RegistryObject<Item> CHISEL = ITEMS.register("chisel",
			() -> new GTChiselItem(new Item.Properties().durability(GTChiselItem.DURABILITY_POINTS)));

	/**
	 * The formal file — item id {@code gt6:file} (task p24-tool-system spec ①/⑤). Single
	 * steel tier, durability 512 (the crowbar/cutter/chisel pinned family value; upstream
	 * scales per material — the p31-single-tier-ruling: LADDER CANDIDATE of the
	 * machine-face family, the per-material rows Loader_Tools.java:304 ("P"/"Pk",
	 * typemin(2) qualmax(2)), the toolHeadFile amount :127, the AdvancedCraftingTool :347;
	 * {@code GT6FileItem} carries no seam face until its ladder card). Upstream display
	 * name "File"
	 * (CS.java:1095); the crafting-loss face rides
	 * {@link GT6FileItem#getCraftingRemainingItem} (one point per craft, the
	 * damage-mapping decision); the crafting INGREDIENT face is the {@code #gt6:tools/file}
	 * item tag (GT6ItemTags, the craftingToolFile oredict translation).
	 */
	public static final RegistryObject<Item> FILE = ITEMS.register("file",
			() -> new GT6FileItem(new Item.Properties().durability(GT6FileItem.DURABILITY_POINTS)));

	/**
	 * The formal saw — item id {@code gt6:saw} (task p24-tool-system spec ①/⑤). Single
	 * steel tier, durability 512 (the family value; the material ladder UNLOCKED by
	 * task p31-machine-ladder). Upstream display name "Saw" (CS.java:1094); the crafting-loss face
	 * rides {@link GTSawItem#getCraftingRemainingItem} (the shared one-point mapping);
	 * the crafting INGREDIENT face is the {@code #gt6:tools/saw} item tag (GT6ItemTags,
	 * the craftingToolSaw oredict translation). The world arms (bark strip, sapling/
	 * workbench placement) stay pooled with the interaction card.
	 */
	public static final RegistryObject<Item> SAW = ITEMS.register("saw",
			() -> new GTSawItem(new Item.Properties().durability(GTSawItem.DURABILITY_POINTS)));

	/**
	 * The formal builder's wand — item id {@code gt6:builder_wand} (task
	 * p24-builder-wand). Single tier, durability 512 (the family value; upstream scales
	 * per material with the ×0.1 multiplier, GT_Tool_Builderwand :42 — the
	 * p31-single-tier-ruling: LADDER CANDIDATE (the per-material rows Loader_Tools.java:
	 * 293, the AdvancedCraftingTool YellowSapphire :336); the radius-2-flat face stays
	 * the declared port behaviour — a material identity would scale the durability
	 * budget, not the radius). Upstream display name "Builder Wand" (the :153 registration row and
	 * the TOOL_LOCALISER row CS.java:1112 verbatim); the scaffold click pays one point
	 * PER CLICK — the upstream 10-unit return folded through Behavior_Tool :63, with the
	 * no-creative-exemption ruling ({@link GT6BuilderWandItem#payClick}). The crafting
	 * recipe stays pooled (the upstream tool-head rows, Loader_Tools.java:293/:336).
	 */
	public static final RegistryObject<Item> BUILDER_WAND = ITEMS.register("builder_wand",
			() -> new GT6BuilderWandItem(new Item.Properties().durability(GT6BuilderWandItem.DURABILITY_POINTS)));

	/**
	 * The formal screwdriver — item id {@code gt6:screwdriver} (task p24-screwdriver-item
	 * spec ①/③). Single steel tier, durability 512 (the family value; the material
	 * ladder UNLOCKED by task p31-machine-ladder). Upstream display name "Screwdriver"
	 * (CS.java:1102); the crafting-loss face rides
	 * {@link GT6ScrewdriverItem#getCraftingRemainingItem} (the shared one-point mapping,
	 * the upstream :70-72 400-unit row folded); the crafting INGREDIENT face is the
	 * {@code #gt6:tools/screwdriver} item tag (GT6ItemTags, the craftingToolScrewdriver
	 * oredict translation). The world arms (the TOOL_screwdriver-harvestable +
	 * Material.circuits surface, GT_Tool_Screwdriver.java:105-112) stay pooled with the
	 * machine interaction card.
	 */
	public static final RegistryObject<Item> SCREWDRIVER = ITEMS.register("screwdriver",
			() -> new GT6ScrewdriverItem(new Item.Properties().durability(GT6ScrewdriverItem.DURABILITY_POINTS)));

	/**
	 * The formal hard hammer — item id {@code gt6:hammer} (task p25-tool-hammer-wrench
	 * spec ③). Single steel tier at the identity-less arm, durability 512 (the family
	 * value; the material ladder UNLOCKED by task p31-machine-ladder — upstream scales
	 * per material via {@code toolHeadHammer.mAmount}, Loader_Tools.java:124, and
	 * {@link GTHammerItem} implements the {@code GT6ToolLadder.LadderTool} face).
	 * Upstream display name "Hammer" (CS.java:1096, the same :124 registration row);
	 * the crafting-loss face rides {@link GTHammerItem#getCraftingRemainingItem} (the
	 * shared one-point mapping, the upstream :70 400-unit row folded); the crafting
	 * INGREDIENT face is the {@code #gt6:tools/hard_hammer} item tag (GT6ItemTags, the
	 * craftingToolHardHammer snake, the naming ruling). The world arms (ore-crush drop
	 * conversion + the mining surface) stay pooled with the world-interaction card.
	 */
	public static final RegistryObject<Item> HAMMER = ITEMS.register("hammer",
			() -> new GTHammerItem(new Item.Properties().durability(GTHammerItem.DURABILITY_POINTS)));

	/**
	 * The formal wrench — item id {@code gt6:wrench} (task p25-tool-hammer-wrench spec
	 * ③). Single steel tier, durability 512 (the family value; the material ladder
	 * UNLOCKED by task p31-machine-ladder). Upstream display name "Wrench" (CS.java:1083, the same :126 registration row); the
	 * crafting-loss face rides {@link GTWrenchItem#getCraftingRemainingItem} (the shared
	 * one-point mapping, the upstream :59 800-unit row folded); the crafting INGREDIENT
	 * face is the {@code #gt6:tools/wrench} item tag (GT6ItemTags, the craftingToolWrench
	 * snake). RED LINE: zero world-interaction surface — the machine-dismantle/rotation
	 * pool (the three HOE_DIG predicates) stays untouched.
	 */
	public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench",
			() -> new GTWrenchItem(new Item.Properties().durability(GTWrenchItem.DURABILITY_POINTS)));

	/**
	 * The formal small bending cylinder — item id {@code gt6:bending_cylinder_small} (task
	 * p25-food-can-row0 spec ②, the GT6FileItem form with the census OFF). Single steel
	 * tier, durability 512 (the family value; upstream scales per material via
	 * {@code setMaterialAmount(3*U)}, Loader_Tools.java:146 — the
	 * p31-single-tier-ruling: LADDER CANDIDATE, the crafting-consumable axis (the
	 * per-material ingot rows :313, typemin(2))). Upstream
	 * display name "Small Bending Cylinder" (the same :146 registration row); the
	 * crafting-loss face rides {@link GT6BendingCylinderSmallItem#getCraftingRemainingItem}
	 * (the shared one-point mapping, the upstream 25-unit row folded); the crafting
	 * INGREDIENT face is the {@code #gt6:tools/bending_cylinder_small} item tag (GT6ItemTags,
	 * the craftingToolBendingCylinderSmall snake, CS.java:1903). RED LINE: zero ToolAction
	 * surface — the upstream cylinder isMinableBlock returns false verbatim
	 * (GT_Tool_BendingCylinderSmall.java:59-60) and carries no Behavior_Tool machine face,
	 * so the census is structurally empty; no {@code canPerformAction} override exists.
	 */
	public static final RegistryObject<Item> BENDING_CYLINDER_SMALL = ITEMS.register("bending_cylinder_small",
			() -> new GT6BendingCylinderSmallItem(new Item.Properties().durability(GT6BendingCylinderSmallItem.DURABILITY_POINTS)));

	// ─── the pocket multitool family (task p29-w5-t7-pocket-eight, tail-append) ───
	// Eight forms of one tool (Loader_Tools.java:176-183): the closed multitool plus the
	// seven switch faces, ring-chained by GTPocketMultitoolItem.next = (i+1)%8 (the :187-196
	// NEI-redirect walk, which itself has no modern counterpart — the declared deviation
	// puts all eight in the creative tab, the hide-face rides the JEI binding pool). One
	// class, eight registrations (the card's base + form-parameter shape); pure durability
	// 512 (the family value, ruling d) — NO battery/EU face (the reversal ruling: the :354
	// recipe row carries no battery slot).
	// p31-single-tier-ruling: LADDER CANDIDATE, GATED on the tool-piece family — the
	// upstream material identity is injected by the five tool heads the :354 recipe
	// consumes (typemin(3) qualmin(1)), and this port cut that head family (the class
	// javadoc cut ①), so the eight forms stay identity-less until the family lands.

	/** The closed form — item id {@code gt6:pocket_multitool} ("7 useful Tools in one!"). */
	public static final RegistryObject<Item> POCKET_MULTITOOL = ITEMS.register("pocket_multitool",
			() -> new GTPocketMultitoolItem(GTPocketMultitoolItem.MULTITOOL, new Item.Properties().durability(GTPocketMultitoolItem.DURABILITY_POINTS)));

	/** The knife form — item id {@code gt6:pocket_multitool_knife} (the attack face). */
	public static final RegistryObject<Item> POCKET_MULTITOOL_KNIFE = ITEMS.register("pocket_multitool_knife",
			() -> new GTPocketMultitoolItem(GTPocketMultitoolItem.KNIFE, new Item.Properties().durability(GTPocketMultitoolItem.DURABILITY_POINTS)));

	/** The saw form — item id {@code gt6:pocket_multitool_saw} (wood+ice mining). */
	public static final RegistryObject<Item> POCKET_MULTITOOL_SAW = ITEMS.register("pocket_multitool_saw",
			() -> new GTPocketMultitoolItem(GTPocketMultitoolItem.SAW, new Item.Properties().durability(GTPocketMultitoolItem.DURABILITY_POINTS)));

	/** The file form — item id {@code gt6:pocket_multitool_file} (iron-bars mining, ×3). */
	public static final RegistryObject<Item> POCKET_MULTITOOL_FILE = ITEMS.register("pocket_multitool_file",
			() -> new GTPocketMultitoolItem(GTPocketMultitoolItem.FILE, new Item.Properties().durability(GTPocketMultitoolItem.DURABILITY_POINTS)));

	/** The screwdriver form — item id {@code gt6:pocket_multitool_screwdriver}. */
	public static final RegistryObject<Item> POCKET_MULTITOOL_SCREWDRIVER = ITEMS.register("pocket_multitool_screwdriver",
			() -> new GTPocketMultitoolItem(GTPocketMultitoolItem.SCREWDRIVER, new Item.Properties().durability(GTPocketMultitoolItem.DURABILITY_POINTS)));

	/** The wire cutter form — item id {@code gt6:pocket_multitool_wire_cutter} (the cutter useOn face delegates). */
	public static final RegistryObject<Item> POCKET_MULTITOOL_WIRE_CUTTER = ITEMS.register("pocket_multitool_wire_cutter",
			() -> new GTPocketMultitoolItem(GTPocketMultitoolItem.WIRE_CUTTER, new Item.Properties().durability(GTPocketMultitoolItem.DURABILITY_POINTS)));

	/** The scissors form — item id {@code gt6:pocket_multitool_scissors} (the attack face). */
	public static final RegistryObject<Item> POCKET_MULTITOOL_SCISSORS = ITEMS.register("pocket_multitool_scissors",
			() -> new GTPocketMultitoolItem(GTPocketMultitoolItem.SCISSORS, new Item.Properties().durability(GTPocketMultitoolItem.DURABILITY_POINTS)));

	/** The chisel form — item id {@code gt6:pocket_multitool_chisel} (the chisel useOn arms delegate). */
	public static final RegistryObject<Item> POCKET_MULTITOOL_CHISEL = ITEMS.register("pocket_multitool_chisel",
			() -> new GTPocketMultitoolItem(GTPocketMultitoolItem.CHISEL, new Item.Properties().durability(GTPocketMultitoolItem.DURABILITY_POINTS)));

	/**
	 * The pocket ring in switch order (task p29-w5-t7-pocket-eight) — the registration-row
	 * sequence Loader_Tools.java:176-183 pins: each form's switch target is the NEXT entry,
	 * the chisel wrapping back to the multitool (GTPocketMultitoolItem.next = (i+1)%8).
	 */
	public static final List<RegistryObject<Item>> POCKET_FORMS = List.of(POCKET_MULTITOOL, POCKET_MULTITOOL_KNIFE,
			POCKET_MULTITOOL_SAW, POCKET_MULTITOOL_FILE, POCKET_MULTITOOL_SCREWDRIVER, POCKET_MULTITOOL_WIRE_CUTTER,
			POCKET_MULTITOOL_SCISSORS, POCKET_MULTITOOL_CHISEL);

	/**
	 * The six dig tools — task p29-w5-t1-dig-six (the W5 tool wave card 1; rows 11-16 of
	 * the table). Single steel tier, durability 512 (the family value; upstream scales
	 * per material, the same pool cut — the gem pick's upstream ×0.25 multiplier folds
	 * into its flat 128, GTPickaxeGemItem javadoc). Display names = the upstream
	 * registration-row wordings (Loader_Tools.java:147/:151/:336-341). The world arms:
	 * <ul>
	 * <li>{@code gt6:pickaxe} (upstream GT_Tool_Pickaxe, the TOOL_pickaxe surface + the
	 *     Place_Torch arm);</li>
	 * <li>{@code gt6:pickaxe_gem} (upstream GT_Tool_PickaxeGem — the fragility
	 *     variant);</li>
	 * <li>{@code gt6:pickaxe_construction} (upstream GT_Tool_PickaxeConstruction — speed
	 *     ×2, ore-stone ×0.25, the ender-chest drop conversion riding the
	 *     {@code GT6ToolLootModifiers} loot seam);</li>
	 * <li>{@code gt6:shovel} (upstream GT_Tool_Shovel — path/torch arms);</li>
	 * <li>{@code gt6:spade} (upstream GT_Tool_Spade — speed ×1.5, the harvestableSpade
	 *     drop conversion riding the loot seam);</li>
	 * <li>{@code gt6:universal_spade} (upstream GT_Tool_UniversalSpade — the five-face
	 *     surface + the openableCrowbar Unboxinator conversion riding the loot seam).</li>
	 * </ul>
	 *
	 * <p>MATERIAL LADDER (task p31-dig-ladder): the pickaxe trio + the shovel/spade/hoe
	 * ride the {@code GT6ToolLadder} face over the {@code GT.ToolStats} identity (the
	 * per-material durability/speed/level/tint/name, the upstream MultiItemTool :482/:483
	 * formulas; the identity-less arm = the steel fallback bit-exact). The AXE joins the
	 * dig ladder per the upstream family table (row :120 sits in the dig rows, harvest
	 * TOOL_axe, OreDictToolNames.axe — the blade family = sword/universal_spade/knife/
	 * butchery_knife/sense). THE W3 SINGLE-TIER RULING (task p31-single-tier-ruling):
	 * the axe_double is a LADDER CANDIDATE of THIS dig family — row :121 sits inside the
	 * dig block :117-122 (between the axe :120 and the hoe :122), the harvest tag is
	 * TOOL_axe, the oredict name is {@code axe} with no blade (the three-evidence
	 * attribution; the per-material rows :301 typemin(2), the AdvancedCraftingTool :343)
	 * and GTAxeDoubleItem already inherits the {@code GT6ToolLadder} faces through
	 * GTAxeItem — only the {@code gt6:material_tool} row and the ×1.5 multiplier wiring
	 * are missing; the universal spade is a LADDER CANDIDATE of the BLADE family — the
	 * blade oredict name LEADS its four names :134 (blade, shovel, crowbar, saw), the row
	 * sits outside the dig block beside the blade cluster :135-138, and its axis condition
	 * :298 drops the dig family's COATED.NOT gate (the harvest tag TOOL_crowbar is a
	 * role tag, not a family marker; the family rule the blade card keyed on is the
	 * blade oredict name, which this row carries first). The per-material CRAFTING rows
	 * ride the shared
	 * gt6:material_tool serializer (GT6MaterialToolRecipe, the Loader_Tools :293-300
	 * OreProcessing_Tool axis), cut ① above now unlocked for this family.
	 */
	public static final RegistryObject<Item> PICKAXE = ITEMS.register("pickaxe",
			() -> new GTPickaxeItem(new Item.Properties().durability(GTPickaxeItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> PICKAXE_GEM = ITEMS.register("pickaxe_gem",
			() -> new GTPickaxeGemItem(new Item.Properties().durability(GTPickaxeGemItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> PICKAXE_CONSTRUCTION = ITEMS.register("pickaxe_construction",
			() -> new GTPickaxeConstructionItem(new Item.Properties().durability(GTPickaxeConstructionItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> SHOVEL = ITEMS.register("shovel",
			() -> new GTShovelItem(new Item.Properties().durability(GTShovelItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> SPADE = ITEMS.register("spade",
			() -> new GTSpadeItem(new Item.Properties().durability(GTSpadeItem.DURABILITY_POINTS)));

	/**
	 * The universal spade — item id {@code gt6:universal_spade}. p31-single-tier-ruling:
	 * LADDER CANDIDATE of the BLADE family (the evidence on the dig block javadoc above;
	 * the per-material head rows Loader_Tools.java:298, the toolHeadUniversalSpade amount
	 * :134) — identity-less until its ladder card.
	 */
	public static final RegistryObject<Item> UNIVERSAL_SPADE = ITEMS.register("universal_spade",
			() -> new GTUniversalSpadeItem(new Item.Properties().durability(GTUniversalSpadeItem.DURABILITY_POINTS)));

	/**
	 * The six blade tools — task p29-w5-t2-blade-six (the W5 tool wave card 2; rows
	 * 17-22 of the table). Single steel tier, durability 512 (the family value; the
	 * double axe carries the upstream ×1.5 multiplier as the flat 768). Display names =
	 * the upstream registration-row wordings (Loader_Tools.java:118-136). The world arms:
	 * <ul>
	 * <li>{@code gt6:sword} (upstream GT_Tool_Sword — base damage 4.0F, the grass/stick/
	 *     vine drop conversion riding the loot seam mode SWORD_HARVEST);</li>
 * <li>{@code gt6:knife} (upstream GT_Tool_Knife, the sword subclass — 2.0F, ×0.5
	 *     speed);</li>
	 * <li>{@code gt6:butchery_knife} (upstream GT_Tool_ButcheryKnife — 1.0F, the Looting
	 *     face riding the LootingLevelEvent at the constant 2, no mining face);</li>
	 * <li>{@code gt6:club} (upstream GT_Tool_Club, the HardHammer subclass — 5.0F
	 *     inherited, the rockGt crush riding the loot seam mode CLUB_ROCK_CRUSH);</li>
	 * <li>{@code gt6:axe} (upstream GT_Tool_Axe — the whole-tree felling riding the
	 *     loot seam gt6_tree_fell, vanilla trees only);</li>
	 * <li>{@code gt6:axe_double} (upstream GT_Tool_AxeDouble — 6.0F, durability 768).</li>
	 * </ul>
	 *
	 * <p>MATERIAL LADDER (task p31-blade-ladder): the sword/knife/butchery_knife read the
	 * {@code GT.ToolStats} identity per stack (durability, the MultiItemTool.java:392
	 * attack fold, the name template, the tint — {@link GT6ToolLadder}); the axe pair and
	 * the club were ruled by task p31-single-tier-ruling: the axe_double is a DIG-family
	 * ladder candidate (the dig block javadoc above) and the club is a HAMMER-family
	 * ladder candidate (upstream GT_Tool_Club extends GT_Tool_HardHammer,
	 * early/GT_Tool_Club.java:47; the per-material ingot rows Loader_Tools.java:329; the
	 * harvest tag TOOL_hammer :130; GTClubItem carries no seam face until its ladder
	 * card). The {@code gt6:axe} itself rides the dig ladder (GTAxeItem, the LadderTool
	 * face).</p>
	 */
	public static final RegistryObject<Item> SWORD = ITEMS.register("sword",
			() -> new GTSwordItem(new Item.Properties().durability(GTSwordItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> KNIFE = ITEMS.register("knife",
			() -> new GTKnifeItem(new Item.Properties().durability(GTKnifeItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> BUTCHERY_KNIFE = ITEMS.register("butchery_knife",
			() -> new GTButcheryKnifeItem(new Item.Properties().durability(GTButcheryKnifeItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> CLUB = ITEMS.register("club",
			() -> new GTClubItem(new Item.Properties().durability(GTClubItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> AXE = ITEMS.register("axe",
			() -> new GTAxeItem(new Item.Properties().durability(GTAxeItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> AXE_DOUBLE = ITEMS.register("axe_double",
			() -> new GTAxeDoubleItem(new Item.Properties().durability(GTAxeDoubleItem.DURABILITY_POINTS)));

	/**
	 * The formal soft hammer — item id {@code gt6:soft_hammer} (task p29-w5-t3-machine-face-four
	 * spec ①). The 8.0x durability multiplier is LIVE over the material ladder
	 * (task p31-machine-ladder): {@link GTSoftHammerItem#MAX_DURABILITY_MULTIPLIER}
	 * (GT_Tool_SoftHammer.java:79-81) rides the {@code GT.ToolStats} j/100 budget —
	 * a Steel soft hammer is 512 ×8 = 4096 points (the declared behaviour change).
	 * Upstream display name "Soft Hammer" (the :125 registration row); the vanilla-ish rotation face
	 * (the :125 tagline) lives on the item — see
	 * {@link GTSoftHammerItem#rotatedForm}; the crafting INGREDIENT face is the
	 * {@code #gt6:tools/soft_hammer} item tag (the craftingToolSoftHammer snake, CS.java:1891).
	 */
	public static final RegistryObject<Item> SOFT_HAMMER = ITEMS.register("soft_hammer",
			() -> new GTSoftHammerItem(new Item.Properties().durability(GTSoftHammerItem.DURABILITY_POINTS)));

	/**
	 * The formal monkey wrench — item id {@code gt6:monkey_wrench} (task
	 * p29-w5-t3-machine-face-four spec ②, the {@link GTWrenchItem} subclass). Single steel
	 * tier, durability 512 (the family value; the material ladder UNLOCKED by
	 * task p31-machine-ladder). Upstream display name "Monkey Wrench" (the
	 * same :144 registration row); the crafting INGREDIENT face is the
	 * {@code #gt6:tools/monkey_wrench} item tag (the craftingToolMonkeyWrench snake) — the
	 * upstream wrench double-name folds to the single tag, the card's tag ruling. RED LINE:
	 * the machine-dismantle face stays the machine-interaction pool, zero useOn.
	 */
	public static final RegistryObject<Item> MONKEY_WRENCH = ITEMS.register("monkey_wrench",
			() -> new GTMonkeyWrenchItem(new Item.Properties().durability(GTMonkeyWrenchItem.DURABILITY_POINTS)));

	/**
	 * The formal magnifying glass — item id {@code gt6:magnifying_glass} (task
	 * p29-w5-t3-machine-face-four spec ③). Single steel tier, durability 512 (the family
	 * value; the material ladder UNLOCKED by task p31-machine-ladder). Upstream display
	 * name "Magnifying Glass" (the same :148 registration
	 * row); the pure right-click check face rides {@link GTMagnifyingGlassItem#useOn}
	 * (AHA/HMM, zero change); the crafting INGREDIENT face is the
	 * {@code #gt6:tools/magnifying_glass} item tag (the craftingToolMagnifyingglass snake).
	 */
	public static final RegistryObject<Item> MAGNIFYING_GLASS = ITEMS.register("magnifying_glass",
			() -> new GTMagnifyingGlassItem(new Item.Properties().durability(GTMagnifyingGlassItem.DURABILITY_POINTS)));

	/**
	 * The formal pincers — item id {@code gt6:pincers} (task p29-w5-t3-machine-face-four spec
	 * ④). Single steel tier, durability 512 (the family value; the material ladder
	 * UNLOCKED by task p31-machine-ladder). Upstream display name "Pincers" (the same :150 registration row); the dragon-egg collect face
	 * (the Material.dragonEgg isMinableBlock arm + canCollect, GT_Tool_Pincers.java:105-110)
	 * rides {@link GTPincersItem#useOn} — sneak right-click pops the egg; the crafting
	 * INGREDIENT face is the {@code #gt6:tools/pincers} item tag (the craftingToolPincers
	 * snake, CS.java:1880). RED LINE: the machine wire-pulling face stays the pool.
	 */
	public static final RegistryObject<Item> PINCERS = ITEMS.register("pincers",
			() -> new GTPincersItem(new Item.Properties().durability(GTPincersItem.DURABILITY_POINTS)));

	/**
	 * The five field tools — task p29-w5-t4-field-five (the W5 tool wave card 4; rows
	 * 27-31 of the table). Single steel tier over the family durability mapping (512 ×
	 * the upstream getMaxDurabilityMultiplier). Display names = the upstream
	 * registration-row wordings (Loader_Tools.java:122/:133/:138/:139/:152). The world
	 * arms:
	 * <ul>
	 * <li>{@code gt6:hoe} (upstream GT_Tool_Hoe — the hoe-tag + gourd surface; the
	 *     buildHoe achievement face is the declared cut: no 1.20.1 node);</li>
	 * <li>{@code gt6:plow} (upstream GT_Tool_Plow — the snow/fire surface, the 3x3
	 *     neighbourhood sweep on {@code GT6ToolSweep}, the Snowman ×4 damage);</li>
	 * <li>{@code gt6:branch_cutter} (upstream GT_Tool_BranchCutter — the Grafter leaf
	 *     tool: leaves→sapling/apple conversion riding the {@code GT6ToolLootModifiers}
	 *     loot seam + the drop-chance floor formula);</li>
	 * <li>{@code gt6:sense} (upstream GT_Tool_Sense — the scythe: plants/leaves/vine
	 *     surface with the lily-pad exclusion, the 3x3 sweep, the grass/stick
	 *     conversion riding the loot seam);</li>
	 * <li>{@code gt6:hand_drill} (upstream GT_Tool_HandDrill — the isMiningTool-F
	 *     prospecting face, the DECLARED EMPTY surface in this universe).</li>
	 * </ul>
	 *
	 * <p>p31-single-tier-ruling: ALL FOUR un-laddered rows here are LADDER CANDIDATES —
	 * the plow (the per-material rows Loader_Tools.java:303 typemin(2), the
	 * AdvancedCraftingTool Spruce handle :346 — the {@code secondaryOf} Spruce fallback
	 * precedent), the branch_cutter (:325 typemin(2), the 5*U amount :133), the sense
	 * (:302 typemin(2); the blade oredict name :138 makes it a BLADE-family member for
	 * the future tint/name dispatch), the hand_drill (:330 typemin(2) qualmin(2) — the
	 * port face stays declared-EMPTY, a ladder adds numbers, not behaviour).</p>
	 */
	public static final RegistryObject<Item> HOE = ITEMS.register("hoe",
			() -> new GTHoeItem(new Item.Properties().durability(GTHoeItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> PLOW = ITEMS.register("plow",
			() -> new GTPlowItem(new Item.Properties().durability(GTPlowItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> BRANCH_CUTTER = ITEMS.register("branch_cutter",
			() -> new GTBranchCutterItem(new Item.Properties().durability(GTBranchCutterItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> SENSE = ITEMS.register("sense",
			() -> new GTSenseItem(new Item.Properties().durability(GTSenseItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> HAND_DRILL = ITEMS.register("hand_drill",
			() -> new GTHandDrillItem(new Item.Properties().durability(GTHandDrillItem.DURABILITY_POINTS)));

	/**
	 * The six scene tools — task p29-w5-t5-scene-six (the W5 tool wave card 5; rows 32-37
	 * of the table). Single steel tier, durability 512 (the family value; the flint-and-
	 * tinder's upstream ×0.25 multiplier folds into its flat 128, the gem-pick precedent).
	 * Display names = the upstream registration-row wordings (Loader_Tools.java:132/:140/
	 * :141/:143/:145/:149). The world arms:
	 * <ul>
	 * <li>{@code gt6:scissors} (upstream GT_Tool_Scissors — the ShearsItem base carries the
	 *     cloth/web shear universe; the vine self-drop rides the loot seam);</li>
	 * <li>{@code gt6:scoop} (upstream GT_Tool_Scoop — the shears base + the declarative
	 *     BeehiveBlock honeycomb mapping; the cobweb/vine full-drop rides the loot seam);</li>
	 * <li>{@code gt6:plunger} (upstream GT_Tool_Plunger — the 1000 L fluid-void drain over
	 *     the platform capability seam; the item arm is upstream-dead code);</li>
	 * <li>{@code gt6:flint_and_tinder} (upstream GT_Tool_FlintAndTinder — the 30% chance
	 *     strike over the vanilla ignition faces + the creeper ignite);</li>
	 * <li>{@code gt6:rolling_pin} (upstream GT_Tool_RollingPin — the pure-crafting
	 *     consumable, the GT6BendingCylinderSmallItem structure-empty form);</li>
	 * <li>{@code gt6:bending_cylinder} (upstream GT_Tool_BendingCylinder, the LARGE form —
	 *     the Small size landed with p25-food-can-row0; same structure-empty form).</li>
	 * </ul>
	 *
	 * <p>p31-single-tier-ruling: LADDER CANDIDATES = the scissors (the per-material rows
	 * Loader_Tools.java:326 typemin(2), the screw+ring amount :149), the scoop (:320, the
	 * material-stick rows, the 3*U amount :132), the plunger (:315 — the WEAKEST axis:
	 * material sticks only, the registration amount is 0 :140 so the upstream scrap face
	 * is dead, ToolStats.java:187), the rolling_pin (the wood-plank loop
	 * Loader_Recipes_Woods.java:237-238 + the plastic family :252-253 — an open family
	 * axis, no OreProcessing_Tool row), the bending cylinder pair (:312/:313, the per-
	 * material ingot rows). PERMANENT SINGLE TIER = the flint_and_tinder: no
	 * OreProcessing_Tool axis row, the obtainment variants are a CLOSED striker
	 * enumeration (:207-247, the flint secondary pinned) with zero behaviour spread, and
	 * the registration amount is 0 (:143) — no material identity, zero seam consumption.</p>
	 */
	public static final RegistryObject<Item> SCISSORS = ITEMS.register("scissors",
			() -> new GTScissorsItem(new Item.Properties().durability(GTScissorsItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> SCOOP = ITEMS.register("scoop",
			() -> new GTScoopItem(new Item.Properties().durability(GTScoopItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> PLUNGER = ITEMS.register("plunger",
			() -> new GTPlungerItem(new Item.Properties().durability(GTPlungerItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> FLINT_AND_TINDER = ITEMS.register("flint_and_tinder",
			() -> new GTFlintAndTinderItem(new Item.Properties().durability(GTFlintAndTinderItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> ROLLING_PIN = ITEMS.register("rolling_pin",
			() -> new GTRollingPinItem(new Item.Properties().durability(GTRollingPinItem.DURABILITY_POINTS)));

	public static final RegistryObject<Item> BENDING_CYLINDER = ITEMS.register("bending_cylinder",
			() -> new GT6BendingCylinderItem(new Item.Properties().durability(GT6BendingCylinderItem.DURABILITY_POINTS)));

	/**
	 * The nineteen electric tools — task p29-w5-t6-electric-nineteen (rows 38-56 of the
	 * table, the upstream registration order Loader_Tools.java:156-174). ONE shared base
	 * class ({@code GT6ElectricToolItem}) over its {@code Spec} table: EU pool per tier
	 * (lead-acid representative capacities 64000/256000/1024000 — the declared 收敛 of the
	 * upstream capacity-sum face :427-450), the :433 random-wear shell (512 = the family
	 * value), the sneak bare-target twin swap (:162-166 cross-references), the mining
	 * surfaces (the upstream isMinableBlock mappings; zero machine face — the RED LINE).
	 *
	 * <p>p31-single-tier-ruling: PERMANENT single-material — the tier axis is VOLTAGE
	 * (the per-voltage rows Loader_Tools.java:156-174; the battery/motor recipes :357-378
	 * pin the structural materials to the DATA.Electric tables), not the raw-material
	 * ladder; zero {@code GT6ItemData} seam consumption.</p>
	 */
	public static final RegistryObject<Item> MINING_DRILL_LV = ITEMS.register(GT6ElectricToolItem.MINING_DRILL_LV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.MINING_DRILL_LV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> MINING_DRILL_MV = ITEMS.register(GT6ElectricToolItem.MINING_DRILL_MV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.MINING_DRILL_MV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> MINING_DRILL_HV = ITEMS.register(GT6ElectricToolItem.MINING_DRILL_HV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.MINING_DRILL_HV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> CHAINSAW_LV = ITEMS.register(GT6ElectricToolItem.CHAINSAW_LV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.CHAINSAW_LV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> CHAINSAW_MV = ITEMS.register(GT6ElectricToolItem.CHAINSAW_MV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.CHAINSAW_MV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> CHAINSAW_HV = ITEMS.register(GT6ElectricToolItem.CHAINSAW_HV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.CHAINSAW_HV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> WRENCH_LV = ITEMS.register(GT6ElectricToolItem.WRENCH_LV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.WRENCH_LV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> WRENCH_MV = ITEMS.register(GT6ElectricToolItem.WRENCH_MV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.WRENCH_MV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> WRENCH_HV = ITEMS.register(GT6ElectricToolItem.WRENCH_HV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.WRENCH_HV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> JACKHAMMER_HV_NORMAL = ITEMS.register(GT6ElectricToolItem.JACKHAMMER_HV_NORMAL.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.JACKHAMMER_HV_NORMAL, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> JACKHAMMER_HV_NO_ORES = ITEMS.register(GT6ElectricToolItem.JACKHAMMER_HV_NO_ORES.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.JACKHAMMER_HV_NO_ORES, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> BUZZSAW_LV = ITEMS.register(GT6ElectricToolItem.BUZZSAW_LV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.BUZZSAW_LV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> SCREWDRIVER_LV = ITEMS.register(GT6ElectricToolItem.SCREWDRIVER_LV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.SCREWDRIVER_LV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> HAND_DRILL_LV = ITEMS.register(GT6ElectricToolItem.HAND_DRILL_LV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.HAND_DRILL_LV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> HAND_MIXER_LV = ITEMS.register(GT6ElectricToolItem.HAND_MIXER_LV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.HAND_MIXER_LV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> MONKEY_WRENCH_LV = ITEMS.register(GT6ElectricToolItem.MONKEY_WRENCH_LV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.MONKEY_WRENCH_LV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> MONKEY_WRENCH_MV = ITEMS.register(GT6ElectricToolItem.MONKEY_WRENCH_MV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.MONKEY_WRENCH_MV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> MONKEY_WRENCH_HV = ITEMS.register(GT6ElectricToolItem.MONKEY_WRENCH_HV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.MONKEY_WRENCH_HV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	public static final RegistryObject<Item> TRIMMER_LV = ITEMS.register(GT6ElectricToolItem.TRIMMER_LV.aPath(),
			() -> new GT6ElectricToolItem(GT6ElectricToolItem.TRIMMER_LV, new Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));

	/** The nineteen electric rows in TAB order (the SPECS source — one walk, no drift). */
	public static final List<RegistryObject<Item>> ELECTRIC_TOOLS = List.of(
			MINING_DRILL_LV, MINING_DRILL_MV, MINING_DRILL_HV,
			CHAINSAW_LV, CHAINSAW_MV, CHAINSAW_HV,
			WRENCH_LV, WRENCH_MV, WRENCH_HV,
			JACKHAMMER_HV_NORMAL, JACKHAMMER_HV_NO_ORES,
			BUZZSAW_LV, SCREWDRIVER_LV, HAND_DRILL_LV, HAND_MIXER_LV,
			MONKEY_WRENCH_LV, MONKEY_WRENCH_MV, MONKEY_WRENCH_HV,
			TRIMMER_LV);

	/** The path → the electric RegistryObject (the recipe builder + the RCON command seam). */
	public static RegistryObject<Item> electricTool(String aPath) {
		for (RegistryObject<Item> tRow : ELECTRIC_TOOLS) {
			if (tRow.getId().getPath().equals(aPath)) return tRow;
		}
		return null;
	}

	/**
	 * The Hazmat armor family — task p29-w5-t8-armor-24 (tail-append,
	 * decisions.p30-w5-split-rulings): 24 FLAT items (6 suits x 4 slots, non-NBT, the
	 * research ruling over the upstream per-piece rows Loader_Tools.java:68-96),
	 * registered into THIS single DeferredRegister (the card boundary — no second DR)
	 * and appended to the {@link #TAB_TABLE} tail below. Registration order = the
	 * {@link GT6ArmorMaterials#SUITS} walk x the four {@link ArmorItem.Type} slots; the
	 * datagen bands' row lookup rides {@link #armorRow}.
	 */
	public static final List<RegistryObject<Item>> ARMOR_ROWS = buildArmorRows();

	private static List<RegistryObject<Item>> buildArmorRows() {
		List<RegistryObject<Item>> rRows = new ArrayList<>();
		for (GT6ArmorMaterials.SuitRow tSuit : GT6ArmorMaterials.SUITS) {
			for (int i = 0; i < GT6ArmorMaterials.PIECE_TYPES.length; i++) {
				ArmorItem.Type tType = GT6ArmorMaterials.PIECE_TYPES[i];
				String tPath = tSuit.pieceId(i);
				String tTexture = tSuit.textureName();
				rRows.add(ITEMS.register(tPath,
						() -> new GT6ArmorItem(tSuit.suit(), tType, new Item.Properties(), tTexture)));
			}
		}
		return List.copyOf(rRows);
	}

	/** One armor row by suit + slot — the datagen bands' lookup face (the SUITS-walk order). */
	public static RegistryObject<Item> armorRow(GT6ArmorMaterials.SuitRow aSuit, int aSlot) {
		return ARMOR_ROWS.get(GT6ArmorMaterials.SUITS.indexOf(aSuit) * GT6ArmorMaterials.PIECE_TYPES.length + aSlot);
	}

	/**
	 * The "Tools" tab display table — one row per registered tool item, in display order.
	 * Table-driven so the tool-family cards append ONE row each. Pure data:
	 * {@link RegistryObject#getId()} reads the pre-registration name field
	 * (RegistryObject.java:287) and nothing here resolves {@code get()} — the offline test
	 * asserts the table shape and the ITEMS parity without touching the frozen registry;
	 * the displayItems generator below does the runtime resolution (the
	 * GTWires.ELECTRIC_WIRES_TAB form). Task p24-tool-system appended rows 3/4 (file,
	 * saw); task p24-builder-wand appended row 5 (the builder wand); task
	 * p24-screwdriver-item appends row 6 (the screwdriver); task p25-tool-hammer-wrench
	 * appends rows 7/8 (the hammer, the wrench); task p25-food-can-row0 appends row 9
	 * (the small bending cylinder); task p29-w5-t1-dig-six appends rows 10-15 (the six
	 * dig tools — pickaxe, pickaxe_gem, pickaxe_construction, shovel, spade,
	 * universal_spade); task p29-w5-t2-blade-six appends rows 17-22 (the six blade
	 * tools — sword, knife, butchery_knife, club, axe, axe_double); task
	 * p29-w5-t3-machine-face-four appends rows 23-26 (the machine-face four: the soft
	 * hammer, the monkey wrench, the magnifying glass, the pincers); task
	 * p29-w5-t4-field-five appends rows 27-31 (the five field tools — hoe,
	 * branch_cutter, sense, plow, hand_drill, the upstream Loader_Tools id order
	 * 122/133/138/139/152); task p29-w5-t5-scene-six appends rows 32-37 (the six scene
	 * tools — scissors, scoop, plunger, flint_and_tinder, rolling_pin, bending_cylinder);
	 * task p29-w5-t6-electric-nineteen appends rows 38-56 (the nineteen electric tools —
	 * the upstream Loader_Tools.java:156-174 registration-row order).
	 */
	public static final List<RegistryObject<Item>> TAB_TABLE = ImmutableList.<RegistryObject<Item>>builder()
			.add(CROWBAR, CUTTER, CHISEL, FILE, SAW, BUILDER_WAND, SCREWDRIVER, HAMMER, WRENCH, BENDING_CYLINDER_SMALL,
			PICKAXE, PICKAXE_GEM, PICKAXE_CONSTRUCTION, SHOVEL, SPADE, UNIVERSAL_SPADE,
			SWORD, KNIFE, BUTCHERY_KNIFE, CLUB, AXE, AXE_DOUBLE,
			SOFT_HAMMER, MONKEY_WRENCH, MAGNIFYING_GLASS, PINCERS,
			HOE, BRANCH_CUTTER, SENSE, PLOW, HAND_DRILL,
			SCISSORS, SCOOP, PLUNGER, FLINT_AND_TINDER, ROLLING_PIN, BENDING_CYLINDER,
			MINING_DRILL_LV, MINING_DRILL_MV, MINING_DRILL_HV,
			CHAINSAW_LV, CHAINSAW_MV, CHAINSAW_HV,
			WRENCH_LV, WRENCH_MV, WRENCH_HV,
			JACKHAMMER_HV_NORMAL, JACKHAMMER_HV_NO_ORES,
			BUZZSAW_LV, SCREWDRIVER_LV, HAND_DRILL_LV, HAND_MIXER_LV,
			MONKEY_WRENCH_LV, MONKEY_WRENCH_MV, MONKEY_WRENCH_HV,
			TRIMMER_LV,
			POCKET_MULTITOOL, POCKET_MULTITOOL_KNIFE, POCKET_MULTITOOL_SAW, POCKET_MULTITOOL_FILE, POCKET_MULTITOOL_SCREWDRIVER, POCKET_MULTITOOL_WIRE_CUTTER, POCKET_MULTITOOL_SCISSORS, POCKET_MULTITOOL_CHISEL)
			// task p29-w5-t8-armor-24 — the 24 hazmat armor rows, tail-append (rows 65-88;
			// the wave-final tally 10 base tools + the 54 W5 tool-card rows + these 24 =
			// the 88-row census the wave gate re-measures)
			.addAll(ARMOR_ROWS)
			.build();

	/**
	 * The tab title lang key — the single source both the builder and the GT6EnUs datagen
	 * row use, so the two faces cannot drift (the offline test pins the literal).
	 */
	public static final String TAB_TITLE_KEY = "itemGroup.gt6.tools";

	/**
	 * The "Tools" category tab — id {@code gt6:tools}, title key {@link #TAB_TITLE_KEY},
	 * icon the crowbar, entries the {@link #TAB_TABLE} rows (the crowbar then the cutter;
	 * the upstream analogue is the ToolsGT meta-tool block living in its own creative
	 * category, the registration rows Loader_Tools.java:114-145).
	 */
	public static final RegistryObject<CreativeModeTab> TOOLS_TAB = CREATIVE_MODE_TABS.register("tools",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable(TAB_TITLE_KEY))
					.icon(() -> new ItemStack(CROWBAR.get()))
					.displayItems((aParameters, aOutput) -> {
						for (RegistryObject<Item> tRow : TAB_TABLE) {
							aOutput.accept(new ItemStack(tRow.get()));
						}
					})
					.build());

	private GT6Tools() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBarrels.onModConstruct shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (javap loader-4.0.44:
		//ModContainer.getEventBus public abstract) — the GTMachines fork precedent.
		*///?}
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
	}

	/** Registration smoke evidence (the GTFluids/GTBarrels onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.CROWBAR.get()), GTCrowbarItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.CUTTER.get()), GTCutterItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.CHISEL.get()), GTChiselItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.FILE.get()), GT6FileItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.SAW.get()), GTSawItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.BUILDER_WAND.get()), GT6BuilderWandItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.SCREWDRIVER.get()), GT6ScrewdriverItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.HAMMER.get()), GTHammerItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.WRENCH.get()), GTWrenchItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.BENDING_CYLINDER_SMALL.get()), GT6BendingCylinderSmallItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.UNIVERSAL_SPADE.get()), GTUniversalSpadeItem.DURABILITY_POINTS);
			// task p29-w5-t1-dig-six — the six dig tools join the registration smoke log
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.PICKAXE.get()), GTPickaxeItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.PICKAXE_GEM.get()), GTPickaxeGemItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.PICKAXE_CONSTRUCTION.get()), GTPickaxeConstructionItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.SHOVEL.get()), GTShovelItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.SPADE.get()), GTSpadeItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.UNIVERSAL_SPADE.get()), GTUniversalSpadeItem.DURABILITY_POINTS);
			// task p29-w5-t2-blade-six — the six blade tools join the registration smoke log
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.SWORD.get()), GTSwordItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.KNIFE.get()), GTKnifeItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.BUTCHERY_KNIFE.get()), GTButcheryKnifeItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.CLUB.get()), GTClubItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.AXE.get()), GTAxeItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.AXE_DOUBLE.get()), GTAxeDoubleItem.DURABILITY_POINTS);
			// task p29-w5-t3-machine-face-four — the machine-face four join the registration smoke log
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.SOFT_HAMMER.get()), GTSoftHammerItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.MONKEY_WRENCH.get()), GTMonkeyWrenchItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.MAGNIFYING_GLASS.get()), GTMagnifyingGlassItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.PINCERS.get()), GTPincersItem.DURABILITY_POINTS);
			// task p29-w5-t4-field-five — the five field tools join the registration smoke log
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.HOE.get()), GTHoeItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.PLOW.get()), GTPlowItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.BRANCH_CUTTER.get()), GTBranchCutterItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.SENSE.get()), GTSenseItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.HAND_DRILL.get()), GTHandDrillItem.DURABILITY_POINTS);
			// task p29-w5-t5-scene-six — the six scene tools join the registration smoke log
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.SCISSORS.get()), GTScissorsItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.SCOOP.get()), GTScoopItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.PLUNGER.get()), GTPlungerItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.FLINT_AND_TINDER.get()), GTFlintAndTinderItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.ROLLING_PIN.get()), GTRollingPinItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.BENDING_CYLINDER.get()), GT6BendingCylinderItem.DURABILITY_POINTS);
			// task p29-w5-t6-electric-nineteen — the nineteen electric tools join the smoke log
			for (RegistryObject<Item> tRow : ELECTRIC_TOOLS) {
				GT6Mod.LOGGER.info("GT6 tool registered: {} durability {} (electric)",
						ForgeRegistries.ITEMS.getKey(tRow.get()), GT6ElectricToolItem.DURABILITY_POINTS);
			}
			// task p29-w5-t7-pocket-eight — one line for the ring (the census rides the tab line below)
			GT6Mod.LOGGER.info("GT6 tool registered: gt6:pocket_multitool + 7 switch forms, durability {} (ring {})",
					GTPocketMultitoolItem.DURABILITY_POINTS, POCKET_FORMS.size());
			// task p29-w5-t8-armor-24 — the armor family as ONE evidence line (the 24
			// per-item lines would drown the log; the family registration rides the same
			// ITEMS DR whose registry lookup the tab line below already pins)
			GT6Mod.LOGGER.info("GT6 armor registered: {} pieces across {} hazmat suits",
					ARMOR_ROWS.size(), GT6ArmorMaterials.SUITS.size());
			// The registry lookup (not the field name) makes this line real registration
			// evidence — an unregistered tab would throw here and fail the runServer gate.
			GT6Mod.LOGGER.info("GT6 creative tab registered: {} ({} display rows)",
					BuiltInRegistries.CREATIVE_MODE_TAB.getKey(TOOLS_TAB.get()), TAB_TABLE.size());
		});
	}
}
