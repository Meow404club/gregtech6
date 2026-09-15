package gregtech6.registry;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.items.tools.GT6BendingCylinderSmallItem;
import gregtech6.items.tools.GT6BuilderWandItem;
import gregtech6.items.tools.GT6FileItem;
import gregtech6.items.tools.GT6ScrewdriverItem;
import gregtech6.items.tools.GTPickaxeConstructionItem;
import gregtech6.items.tools.GTPickaxeGemItem;
import gregtech6.items.tools.GTPickaxeItem;
import gregtech6.items.tools.GTShovelItem;
import gregtech6.items.tools.GTSpadeItem;
import gregtech6.items.tools.GTUniversalSpadeItem;
import gregtech6.items.tools.GTHammerItem;
import gregtech6.items.tools.GTSawItem;
import gregtech6.items.tools.GTChiselItem;
import gregtech6.items.tools.GTCrowbarItem;
import gregtech6.items.tools.GTCutterItem;
import gregtech6.items.tools.GTWrenchItem;

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
 * <p>Declared pool cuts (ADR 2026-09-01-p10-tools-covers-split, zero code in this card):
 * <ol>
 * <li><b>Crowbar crafting recipe</b> — upstream shapes {@code {"hVS","VSV","SVf"}} per
 *     material, requiring the {@code h} hammer + {@code f} file TOOL PIECES plus a blue
 *     dye auxiliary (Loader_Tools.java:314, the OreProcessing_Tool row over
 *     toolHeadWrench); the port has no tool-piece item family and inventing vanilla
 *     substitutes is not done — unlocks with the tool-family card.</li>
 * <li><b>Crowbar material ladder</b> — upstream registers ONE meta id with an NBT-chosen
 *     material, per-material recipes (the whole ToolsGT block Loader_Tools.java:114-145;
 *     the crowbar row :128 carries {@code setMaterialAmount(3*U2)} = per-material
 *     durability); the port keeps the single steel tier at durability 512 (the pinned
 *     ADR value).</li>
 * <li><b>Crowbar runtime tint</b> — the material RGBa recolouring, coupled to the ladder:
 *     upstream {@code getRGBa} returns the primary material {@code mRGBaSolid}
 *     (GT_Tool_Crowbar.java:146-149, gregtech/items/tools/machine/); the port renders
 *     the single steel texture untinted until the ladder lands.</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Tools {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/**
	 * The formal crowbar — item id {@code gt6:crowbar}. Single steel tier, durability
	 * 512 (the declared ADR value; upstream scales per material, Loader_Tools:128 —
	 * the ladder is a pool cut). Durability semantics: one vanilla point per 10000
	 * upstream tool-damage units, so one cover dismantle = one point.
	 */
	public static final RegistryObject<Item> CROWBAR = ITEMS.register("crowbar",
			() -> new GTCrowbarItem(new Item.Properties().durability(GTCrowbarItem.DURABILITY_POINTS)));

	/**
	 * The formal wire cutter — item id {@code gt6:cutter} (task p10-tool-cutter spec ③).
	 * Single steel tier, durability 512 (upstream {@code 4*U} material-scaled,
	 * Loader_Tools.java:131 — the ladder is a pool cut, the same ruling as the crowbar).
	 * Upstream display name "Wire Cutter" (the same :131 registration row); no attack
	 * attributes — the cutter is not a weapon (GT_Tool_WireCutter :53-65 cut).
	 */
	public static final RegistryObject<Item> CUTTER = ITEMS.register("cutter",
			() -> new GTCutterItem(new Item.Properties().durability(GTCutterItem.DURABILITY_POINTS)));

	/**
	 * The formal chisel — item id {@code gt6:chisel} (task p16-chisel-decalcify spec ①).
	 * Single steel tier, durability 512 (the crowbar/cutter pinned family value; upstream
	 * scales per material, Loader_Tools.java:142 — the ladder is the same pool cut).
	 * Upstream display name "Chisel" (the same :142 registration row); the decalcify
	 * durability mapping is the upstream behaviour's mDamage=25 conversion
	 * (GT_Tool_Chisel.java:98, see {@link GTChiselItem#durabilityPoints}).
	 */
	public static final RegistryObject<Item> CHISEL = ITEMS.register("chisel",
			() -> new GTChiselItem(new Item.Properties().durability(GTChiselItem.DURABILITY_POINTS)));

	/**
	 * The formal file — item id {@code gt6:file} (task p24-tool-system spec ①/⑤). Single
	 * steel tier, durability 512 (the crowbar/cutter/chisel pinned family value; upstream
	 * scales per material, the ladder is the same pool cut). Upstream display name "File"
	 * (CS.java:1095); the crafting-loss face rides
	 * {@link GT6FileItem#getCraftingRemainingItem} (one point per craft, the
	 * damage-mapping decision); the crafting INGREDIENT face is the {@code #gt6:tools/file}
	 * item tag (GT6ItemTags, the craftingToolFile oredict translation).
	 */
	public static final RegistryObject<Item> FILE = ITEMS.register("file",
			() -> new GT6FileItem(new Item.Properties().durability(GT6FileItem.DURABILITY_POINTS)));

	/**
	 * The formal saw — item id {@code gt6:saw} (task p24-tool-system spec ①/⑤). Single
	 * steel tier, durability 512 (the family value; upstream scales per material, the
	 * same pool cut). Upstream display name "Saw" (CS.java:1094); the crafting-loss face
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
	 * per material with the ×0.1 multiplier, GT_Tool_Builderwand :42 — the ladder is the
	 * same pool cut, the declared single-tier ruling: radius 2 ≈ the mid-gem
	 * quality+1). Upstream display name "Builder Wand" (the :153 registration row and
	 * the TOOL_LOCALISER row CS.java:1112 verbatim); the scaffold click pays one point
	 * PER CLICK — the upstream 10-unit return folded through Behavior_Tool :63, with the
	 * no-creative-exemption ruling ({@link GT6BuilderWandItem#payClick}). The crafting
	 * recipe stays pooled (the upstream tool-head rows, Loader_Tools.java:293/:336).
	 */
	public static final RegistryObject<Item> BUILDER_WAND = ITEMS.register("builder_wand",
			() -> new GT6BuilderWandItem(new Item.Properties().durability(GT6BuilderWandItem.DURABILITY_POINTS)));

	/**
	 * The formal screwdriver — item id {@code gt6:screwdriver} (task p24-screwdriver-item
	 * spec ①/③). Single steel tier, durability 512 (the family value; upstream scales per
	 * material via {@code setMaterialAmount(toolHeadScrewdriver.mAmount)},
	 * Loader_Tools.java:129 — the same pool cut). Upstream display name "Screwdriver"
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
	 * spec ③). Single steel tier, durability 512 (the family value; upstream scales per
	 * material via {@code toolHeadHammer.mAmount}, Loader_Tools.java:124 — the same pool
	 * cut). Upstream display name "Hammer" (CS.java:1096, the same :124 registration row);
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
	 * ③). Single steel tier, durability 512 (the family value; upstream scales per
	 * material via {@code 4*U}, Loader_Tools.java:126 — the same pool cut). Upstream
	 * display name "Wrench" (CS.java:1083, the same :126 registration row); the
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
	 * {@code setMaterialAmount(3*U)}, Loader_Tools.java:146 — the same pool cut). Upstream
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

	public static final RegistryObject<Item> UNIVERSAL_SPADE = ITEMS.register("universal_spade",
			() -> new GTUniversalSpadeItem(new Item.Properties().durability(GTUniversalSpadeItem.DURABILITY_POINTS)));

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
	 * universal_spade).
	 */
	public static final List<RegistryObject<Item>> TAB_TABLE = List.of(CROWBAR, CUTTER, CHISEL, FILE, SAW, BUILDER_WAND, SCREWDRIVER, HAMMER, WRENCH, BENDING_CYLINDER_SMALL,
			PICKAXE, PICKAXE_GEM, PICKAXE_CONSTRUCTION, SHOVEL, SPADE, UNIVERSAL_SPADE);

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
			// The registry lookup (not the field name) makes this line real registration
			// evidence — an unregistered tab would throw here and fail the runServer gate.
			GT6Mod.LOGGER.info("GT6 creative tab registered: {} ({} display rows)",
					BuiltInRegistries.CREATIVE_MODE_TAB.getKey(TOOLS_TAB.get()), TAB_TABLE.size());
		});
	}
}
