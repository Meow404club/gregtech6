package gregtech6.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

//? if forge {
import net.minecraftforge.common.Tags;
//?} else {
/*import net.neoforged.neoforge.common.Tags;
*///?}

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.datagen.GT6ItemTags;
import gregtech6.items.armor.GT6ArmorMaterials;
import gregtech6.item.GT6Circuits;
import gregtech6.items.GT6CircuitProgramRecipe;
import gregtech6.registry.GT6Batteries;
import gregtech6.registry.GT6BeeHives;
import gregtech6.registry.GT6Placeables;
import gregtech6.registry.GT6ElectricTransformers;
import gregtech6.registry.GTWires;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GT6ExtruderMolds;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6FoodCans;
import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GT6Kitchen;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6SprayCans;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GT6StaticStorages;
import gregtech6.registry.GT6Rails;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTGrassBlocks;
import gregtech6.registry.GT6Sensors;
import gregapi.data.MT;

/**
 * The GT6 vanilla-crafting datagen home — task tool-system spec ③, the FIRST
 * RecipeProvider of the port (decisions.p24-tool-system-recipe-provider-first: single
 * file, {@code //?} forks confined to the constructor and the {@code buildRecipes}
 * signature, the builder chain shared; the tags-foundation/tool-family cards take over
 * and append bands). Upstream anchor: the empty spray can is CRAFTED
 * (MultiItemRandomTools.java:240 — pattern {@code "Rf"}/{@code "Cs"} over the oredict
 * keys {@code R=dust} redstone, {@code f=craftingToolFile}, {@code C=plateCurved Sn},
 * {@code s=craftingToolSaw}); the port translates every key onto the self-owned item
 * tags of {@link GT6ItemTags} (the tag-strategy ruling — four keys, zero bare items),
 * result {@code gt6:spray_can_empty} ({@link GT6SprayCans#SPRAY_CAN_EMPTY}, the p22
 * depletion-swap target — the crafted can is what the colour cans deplete INTO, so
 * this recipe closes the p22 "v1 acquisition" note's crafting cut). Task
 * food-can-row0 appends the food-can second row itself ({@code "fh"}/{@code "oP"},
 * :239 — {@link #foodCanEmptyBuilder}) plus the bending-cylinder self-craft
 * ({@code "sfh"}/{@code "III"}, Loader_Tools.java:313 — {@link #bendingCylinderSmallBuilder}),
 * the row that consumes the hammer/file/saw trio through its own tool letters.
 *
 * <p>Leg split (the decision's pinned lines): 1.20.1 —
 * {@code RecipeProvider(PackOutput)} (RecipeProvider.java:81) + abstract
 * {@code buildRecipes(Consumer<FinishedRecipe>)} (:108) + {@code save(Consumer,
 * ResourceLocation)} (ShapedRecipeBuilder.java:102); 21.1 —
 * {@code RecipeProvider(PackOutput, CompletableFuture<HolderLookup.Provider>)}
 * (RecipeProvider.java:70) + overridable {@code buildRecipes(RecipeOutput)} (:127) +
 * {@code save(RecipeOutput, ResourceLocation)} (ShapedRecipeBuilder.java:108). The
 * shared chain calls are name-identical on both legs: {@code shaped(RecipeCategory,
 * ItemLike)} (:45/:47), {@code define(char, TagKey)} (:53/:59), {@code pattern(String)}
 * (:72/:78), {@code unlockedBy(String, has(TagKey))} (:81/:87 — the {@code has(TagKey)}
 * helper returns the leg-native criterion type, :580/:709). The registration site in
 * {@link GT6DataGenerators} stays single-source because BOTH legs construct through the
 * two-argument form (the forge leg ignores the lookup future).
 */
public class GT6CraftingRecipes extends RecipeProvider {

	/** The recipe id — the result path, the vanilla naming convention (gt6:spray_can_empty). */
	public static final ResourceLocation SPRAY_CAN_EMPTY_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "spray_can_empty");

	/**
	 * The tool-family recipe ids (task tool-hammer-wrench spec ⑤) — the result-path
	 * vanilla naming convention; the two hammer routes cannot share the result's own id,
	 * so the route names the suffix (the vanilla two-recipe-per-result precedent shape).
	 */
	public static final ResourceLocation HAMMER_STONE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "hammer_stone");
	public static final ResourceLocation HAMMER_INGOTS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "hammer_ingots");
	public static final ResourceLocation WRENCH_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "wrench");
	/** The bending-cylinder self-craft row (task food-can-row0 spec ②) — the result-path convention. */
	public static final ResourceLocation BENDING_CYLINDER_SMALL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "bending_cylinder_small");
	/** The pocket multitool recipe id (task w5-t7-pocket-eight, the result-path convention). */
	public static final ResourceLocation POCKET_MULTITOOL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pocket_multitool");
	/** The empty-food-can crafting row (task food-can-row0 spec ③, MultiItemRandomTools.java:239). */
	public static final ResourceLocation FOOD_CAN_EMPTY_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "food_can_empty");
	/** The machine-face four self-craft rows (task w5-t3-machine-face-four) — the result-path convention, one per tool. */
	public static final ResourceLocation SOFT_HAMMER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "soft_hammer");
	public static final ResourceLocation MONKEY_WRENCH_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "monkey_wrench");
	public static final ResourceLocation MAGNIFYING_GLASS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "magnifying_glass");
	public static final ResourceLocation PINCERS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pincers");
	/** The plate-mold crafting row (task w1-press-extruder-molds, MultiItemTechnological.java:247 stroke). */
	public static final ResourceLocation SHAPE_EXTRUDER_PLATE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_extruder_plate");
	/** The rod-mold crafting row (task w1-press-extruder-molds, MultiItemTechnological.java:221 stroke). */
	public static final ResourceLocation SHAPE_EXTRUDER_ROD_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_extruder_rod");
	/** The LARGE Steel Crucible crafting row (task crucible-multiblock SPEC ⑦). */
	public static final ResourceLocation LARGE_STEEL_CRUCIBLE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "large_steel_crucible");
	/**
	 * The kitchen band (task kitchen-pot-bowl): the steel pot's crafting row (the
	 * result-path convention) + the clay-bowl reverse shapeless + the Raw-bowl hardening
	 * smelt (the :2177 tail — the result path is the vanilla convention again).
	 */
	public static final ResourceLocation BATHING_POT_STEEL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "bathing_pot_steel");
	public static final ResourceLocation CLAY_BOWL_REVERSE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "clay_bowl_reverse");
	public static final ResourceLocation CLAY_BOWL_SMELT_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "mixing_bowl");
	/**
	 * The issue #45 C1 clay-band ids: the bowl's forward shaped source (:132), the
	 * juicer's three faces — the shaped craft (:131), the reverse :118 tail, the :2184
	 * hardening smelt (output-path named, the CLAY_BOWL_SMELT_ID convention).
	 */
	public static final ResourceLocation CLAY_BOWL_FORWARD_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "clay_bowl");
	public static final ResourceLocation CLAY_JUICER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "clay_juicer");
	public static final ResourceLocation CLAY_JUICER_REVERSE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "clay_juicer_reverse");
	public static final ResourceLocation CLAY_JUICER_SMELT_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "juicer");

	public static final ResourceLocation STONE_ANVIL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "stone_anvil");
	public static final ResourceLocation BLACKSTONE_ANVIL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "blackstone_anvil");

	/** The storage-hopper crafting ids — the result-path convention, one per row (Loader :145-146). */
	public static final java.util.List<ResourceLocation> HOPPER_RECIPE_IDS = gregtech6.registry.GT6Hoppers.ROWS.stream()
			.map(GT6CraftingRecipes::hopperRecipeId)
			.collect(java.util.stream.Collectors.toList());

	/**
	 * The id of one hopper row's recipe (the save calls' join seam). The path rides a
	 * local so the two-arg RL ctor args stay bare identifiers — the swap-table regex
	 * never matches parenthesized argument expressions (the grassRecipeId precedent,
	 * {@code tRow.path()} was a 21.1 compile red through exactly that documented gap).
	 */
	public static ResourceLocation hopperRecipeId(GT6Hoppers.HopperRow aRow) {
		String tPath = aRow.path();
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/** The Progress Sensor crafting row (task sensors-core, Loader_MultiTileEntities.java:1995) — the result-path convention. */
	public static final ResourceLocation PROGRESSMETER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "progressmeter");
	/** The Miniature Nether Portal crafting row (task portals-mini-nether-end, Loader :2003) — the result-path convention. */
	public static final ResourceLocation MINI_PORTAL_NETHER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "mini_portal_nether");
	/** The Miniature End Portal crafting row (task portals-mini-nether-end, Loader :2004). */
	public static final ResourceLocation MINI_PORTAL_END_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "mini_portal_end");
	/** The Fluid-O-Meter Sensor crafting row (task sensors-core, Loader :1986). */
	public static final ResourceLocation FLUIDOMETER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "fluidometer");
	/**
	 * The ULV FE→EU converter crafting row (task b-fe-converter-machine) — DECLARED
	 * NEW DESIGN, no upstream recipe exists (the machine itself is the declared deviation):
	 * the tin-alloy double plates + red-alloy fine wires carry the signal side, the copper
	 * ingots the conductor core; the result-path vanilla convention.
	 */
	public static final ResourceLocation FE_CONVERTER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "fe_converter");
	/**
	 * The Water Wheel crafting row (task c-water-wheel) — the kTFRUAddon registration
	 * row QUANTITIES (tileEntityInit0.java:112 "Water Mill": {@code "PPP","SRS","PPP"} =
	 * 6 planks + 2 bronze rings + 1 axle part, the clean-room semantic anchor) over the
	 * port carriers; the result-path vanilla convention.
	 */
	public static final ResourceLocation WATER_WHEEL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "water_wheel");
	/** The Greg o'Lantern crafting row id (task placeables, the result-path convention). */
	public static final ResourceLocation GREG_O_LANTERN_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "greg_o_lantern");
	/**
	 * The Electric Transformer (ULV-LV) crafting row (task c-ulv-lv-transformer) —
	 * the Loader_MultiTileEntities.java:881 row SHAPE ("WIW","XMx","WIW" — the unbound
	 * 'm' dead cell folds to a space; CR has no 'm' tool letter) over the LV-era
	 * MATERIAL-LOCK carriers (decisions.p28-ulv-tier-rulings transformer_ruling): the
	 * casing key upgrades Electric_T[0] TinAlloy → Electric_T[1] galvanized steel (the
	 * conditional entry — whoever crafts this already commands LV power), the wire keys
	 * fold to the copper fine-wire tag, the 'I' plate key stays iron double plates.
	 */
	public static final ResourceLocation ELECTRIC_TRANSFORMER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "electric_transformer");

	/** The Bumbliary pair row ids (task bumbliary-recipes — the result-path convention). */
	public static final ResourceLocation BUMBLIARY_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "bumbliary");
	public static final ResourceLocation BUMBLIARY_ADVANCED_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "bumbliary_advanced");

	/** The six dig-tool row ids (task w5-t1-dig-six — the CR row id per tool, the WRENCH_ID shape). */
	public static final ResourceLocation PICKAXE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pickaxe");
	public static final ResourceLocation PICKAXE_GEM_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pickaxe_gem");
	public static final ResourceLocation PICKAXE_CONSTRUCTION_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pickaxe_construction");
	public static final ResourceLocation SHOVEL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "shovel");
	public static final ResourceLocation SPADE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "spade");
	public static final ResourceLocation UNIVERSAL_SPADE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "universal_spade");
	public static final ResourceLocation CLUB_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "club");
	public static final ResourceLocation AXE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "axe");
	public static final ResourceLocation AXE_DOUBLE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "axe_double");

	/** The five field-tool row ids (task w5-t4-field-five — the t1 row-id shape). */
	public static final ResourceLocation HOE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "hoe");
	public static final ResourceLocation PLOW_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "plow");
	public static final ResourceLocation BRANCH_CUTTER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "branch_cutter");
	public static final ResourceLocation SENSE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "sense");
	public static final ResourceLocation HAND_DRILL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "hand_drill");

	/** The six scene-tool row ids (task w5-t5-scene-six — the same CR row id shape; the flint pair is TWO rows). */
	public static final ResourceLocation SCISSORS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "scissors");
	public static final ResourceLocation SCOOP_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "scoop");
	public static final ResourceLocation PLUNGER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "plunger");
	public static final ResourceLocation FLINT_AND_TINDER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "flint_and_tinder");
	public static final ResourceLocation FLINT_AND_STEEL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "flint_and_steel");
	public static final ResourceLocation ROLLING_PIN_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "rolling_pin");
	public static final ResourceLocation BENDING_CYLINDER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "bending_cylinder");

	/** The CR.shapeless self-recast row id of a sensor path (task sensors-core) — the path + the {@code _recast} suffix (the grass reverse-row suffix shape). */
	public static ResourceLocation sensorRecastId(String aPath) {
		return new ResourceLocation(GT6DataGenerators.MOD_ID, aPath + "_recast");
	}

	public GT6CraftingRecipes(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookupProvider) {
		//? if forge {
		super(aOutput);
		//?} else {
		/*super(aOutput, aLookupProvider);
		*///?}
	}

	//? if forge {
	@Override
	protected void buildRecipes(Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		sprayCanEmptyBuilder().save(aConsumer, SPRAY_CAN_EMPTY_ID);
		hammerFromStoneBuilder().save(aConsumer, HAMMER_STONE_ID);
		hammerFromIngotsBuilder().save(aConsumer, HAMMER_INGOTS_ID);
		wrenchBuilder().save(aConsumer, WRENCH_ID);
		bendingCylinderSmallBuilder().save(aConsumer, BENDING_CYLINDER_SMALL_ID);
		foodCanEmptyBuilder().save(aConsumer, FOOD_CAN_EMPTY_ID);
		softHammerBuilder().save(aConsumer, SOFT_HAMMER_ID);
		monkeyWrenchBuilder().save(aConsumer, MONKEY_WRENCH_ID);
		magnifyingGlassBuilder().save(aConsumer, MAGNIFYING_GLASS_ID);
		pincersBuilder().save(aConsumer, PINCERS_ID);
		shapeExtruderPlateBuilder().save(aConsumer, SHAPE_EXTRUDER_PLATE_ID);
		shapeExtruderRodBuilder().save(aConsumer, SHAPE_EXTRUDER_ROD_ID);
		largeSteelCrucibleBuilder().save(aConsumer, LARGE_STEEL_CRUCIBLE_ID);
		bathingPotSteelBuilder().save(aConsumer, BATHING_POT_STEEL_ID);
		clayBowlForwardBuilder().save(aConsumer, CLAY_BOWL_FORWARD_ID);
		clayBowlReverseBuilder().save(aConsumer, CLAY_BOWL_REVERSE_ID);
		clayBowlSmeltingBuilder().save(aConsumer, CLAY_BOWL_SMELT_ID);
		clayJuicerBuilder().save(aConsumer, CLAY_JUICER_ID);
		clayJuicerReverseBuilder().save(aConsumer, CLAY_JUICER_REVERSE_ID);
		clayJuicerSmeltingBuilder().save(aConsumer, CLAY_JUICER_SMELT_ID);
		anvilBuilder(GT6Anvils.STONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.STONE).save(aConsumer, STONE_ANVIL_ID);
		anvilBuilder(GT6Anvils.BLACKSTONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.BLACKSTONE).save(aConsumer, BLACKSTONE_ANVIL_ID);
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) {
			hopperRecipeBuilder(tRow).save(aConsumer, hopperRecipeId(tRow));
		}
		progressmeterBuilder().save(aConsumer, PROGRESSMETER_ID);
		miniPortalNetherBuilder().save(aConsumer, MINI_PORTAL_NETHER_ID); // task portals-mini-nether-end
		miniPortalEndBuilder().save(aConsumer, MINI_PORTAL_END_ID); // task portals-mini-nether-end
		fluidometerBuilder().save(aConsumer, FLUIDOMETER_ID);
		feConverterBuilder().save(aConsumer, FE_CONVERTER_ID);
		waterWheelBuilder().save(aConsumer, WATER_WHEEL_ID);
		for (BridgeCraftRow tRow : transformerCraftingRows()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (BridgeCraftRow tRow : ldTransformerCraftingRows()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (BridgeCraftRow tRow : ldWireCraftingRows()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (BridgeCraftRow tRow : euBridgeCraftingRows()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (SensorRecastRow tRow : sensorRecastBuilders()) {
			tRow.builder().save(aConsumer, sensorRecastId(tRow.path()));
		}
		for (GrassRecipeRow tRow : grassRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (gregtech6.registry.GT6StaticStorages.StaticRow tRow : gregtech6.registry.GT6StaticStorages.ROWS) {
			staticStorageRecipeBuilder(tRow).save(aConsumer, staticStorageRecipeId(tRow));
		}
		for (PartFamilyRecipeRow tRow : partFamilyRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (PartFamilyRecipeRow tRow : tankValveRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (CrucibleLadderRecipeRow tRow : crucibleLadderRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		// task crucible-wall-obtainability — the 8 dedicated crucible-wall rows
		for (PartFamilyRecipeRow tRow : crucibleWallRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		// task recipes-obtainability — the 11 shared machine-wall rows
		for (PartFamilyRecipeRow tRow : machineWallRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		// task recipes-obtainability — the 8 slicer-blade rows (frame + 7 forms, ruling B)
		for (PartFamilyRecipeRow tRow : slicerBladeRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (GT6Batteries.BatteryRow tRow : GT6Batteries.ROWS) {
			if (tRow.family().startsWith("energium")) continue; // the crystals carry NO rows (upstream :1079-:1092, the declared cut)
			batteryRecipeBuilder(tRow).save(aConsumer, batteryRecipeId(tRow));
		}
		// task placeables — the Greg o'Lantern row
		gregOLanternBuilder().save(aConsumer, GREG_O_LANTERN_ID);
		for (BatteryBoxRecipeRow tRow : batteryBoxRecipeBuilders()) {
			tRow.builder().save(aConsumer, batteryBoxRecipeId(tRow.row()));
		}
		for (DieselEngineRecipeRow tRow : dieselEngineRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (CrackerRecipeRow tRow : crackerRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (CrackerRecipeRow tRow : burnerMixerRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		// task usb-peripherals — the 8 USB peripheral rows (the cables :808-811, the HDDs :819-822)
		for (BridgeCraftRow tRow : usbCableRecipeRows()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (BridgeCraftRow tRow : usbDriveRecipeRows()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		// task debt-scanner-t3-usb-stick — the 4 USB Stick rows (:796-799) + the
		// Molecular Scanner T3 controller row (:1551, the Q1=(b) seam restore)
		for (BridgeCraftRow tRow : usbStickRecipeRows()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		CrackerRecipeRow tScanner = molecularScannerRow();
		tScanner.builder().save(aConsumer, tScanner.id());
		// task w5-t1-dig-six — the six dig-tool steel-route rows (the wrench row shape)
		pickaxeBuilder().save(aConsumer, PICKAXE_ID);
		pickaxeGemBuilder().save(aConsumer, PICKAXE_GEM_ID);
		pickaxeConstructionBuilder().save(aConsumer, PICKAXE_CONSTRUCTION_ID);
		shovelBuilder().save(aConsumer, SHOVEL_ID);
		spadeBuilder().save(aConsumer, SPADE_ID);
		universalSpadeBuilder().save(aConsumer, UNIVERSAL_SPADE_ID);
		// task w5-t2-blade-six — the six blade-tool steel-route rows (the dig-tool row shape)
		clubBuilder().save(aConsumer, CLUB_ID);
		// task blade-ladder — the three blade forms move to the per-material rows
		// (the OreProcessing_Tool rows Loader_Tools.java:321-323; the t1 steel-route
		// convergence placeholders for sword/knife/butchery retire — club stays the
		// single-tier convergence row, the single-tier-ruling card owns it)
		bladeLadderRows(aConsumer);
		axeBuilder().save(aConsumer, AXE_ID);
		axeDoubleBuilder().save(aConsumer, AXE_DOUBLE_ID);
		// task w5-t4-field-five — the five field-tool steel-route rows (the t1 shape)
		hoeBuilder().save(aConsumer, HOE_ID);
		plowBuilder().save(aConsumer, PLOW_ID);
		branchCutterBuilder().save(aConsumer, BRANCH_CUTTER_ID);
		senseBuilder().save(aConsumer, SENSE_ID);
		handDrillBuilder().save(aConsumer, HAND_DRILL_ID);
		// task w5-t5-scene-six — the six scene-tool rows (the same steel-route shape)
		scissorsBuilder().save(aConsumer, SCISSORS_ID);
		scoopBuilder().save(aConsumer, SCOOP_ID);
		net.minecraft.world.item.Item tRubberPlate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Rubber);
		if (tRubberPlate != null) plungerBuilder(tRubberPlate).save(aConsumer, PLUNGER_ID); // the CR.ONLY_IF_HAS_RESULT face — no rubber plate, no row
		flintAndTinderFromFlintAndSteelBuilder().save(aConsumer, FLINT_AND_TINDER_ID);
		flintAndSteelBuilder().save(aConsumer, FLINT_AND_STEEL_ID);
		rollingPinBuilder().save(aConsumer, ROLLING_PIN_ID);
		bendingCylinderBuilder().save(aConsumer, BENDING_CYLINDER_ID);
		// task w5-t6-electric-nineteen — the fifteen electric rows (the :356-377 convergence)
		for (ElectricToolRow tRow : electricToolRows()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		pocketMultitoolBuilder().save(aConsumer, POCKET_MULTITOOL_ID);
		// task w5-t8-armor-24 — the 24 hazmat rows (tail-append)
		for (GT6ArmorMaterials.SuitRow tSuit : GT6ArmorMaterials.SUITS) {
			for (int i = 0; i < GT6ArmorMaterials.PIECE_TYPES.length; i++) {
				armorPieceBuilder(tSuit, i).save(aConsumer, armorRecipeId(tSuit, i));
			}
		}
		// task dig-ladder — the per-material identity-stamped rows (the axis walk)
		digLadderRows(aConsumer);
		// task machine-ladder — the machine family material rows (the identity-stamped walk)
		machineLadderRows(aConsumer);
		// task 39-toolhead-rows — the arg-9 completion band: the 9 missing head families
		// + the dig/chisel/saw C variants (GitHub #39)
		toolHeadRows(aConsumer);
		// task 39-toolhead-assembly — the 17 head+handle assembly rows (the ACT :332-350 port)
		toolAssemblyRows(aConsumer);
		// task circuits-crafting-c — the circuits band: the 26 integrated-circuit rows
		// (gt6:circuit_program) + the ventilation/processor-unit six (gt6 shaped)
		circuitProgramRows(aConsumer);
		partCircuitRows(aConsumer);
		// task bumbliary-recipes — the Bumbliary pair rows (the :2222/:2223 line-tail varargs)
		bumbliaryBuilder().save(aConsumer, BUMBLIARY_ID);
		advancedBumbliaryBuilder().save(aConsumer, BUMBLIARY_ADVANCED_ID);
		// task rails-31-blocks — the 30 rail rows (the :107-140 no-RC fallback band)
		for (GT6Rails.RailRow tRailRow : GT6Rails.ROWS) {
			railRecipeBuilder(tRailRow).save(aConsumer, railRecipeId(tRailRow));
		}
		// task craftfrom-plategem — the CraftFrom hand-craft family (Loader_OreProcessing.java:171-178)
		for (CraftFromRow tRow : craftFromDatagenRows()) {
			craftFromBuilder(tRow).save(aConsumer, tRow.aId());
		}
		// task craftfrom-stick — the stick/stickLong CraftFrom family (Loader_OreProcessing.java:156-163)
		for (StickCraftFromRow tRow : stickCraftFromDatagenRows()) {
			stickCraftFromBuilder(tRow).save(aConsumer, tRow.aId());
		}
		// task craftfrom-foil — the fine-wire CraftFrom batch (Loader_OreProcessing.java:168-169)
		for (FineWireCraftFromRow tRow : fineWireCraftFromDatagenRows()) {
			fineWireCraftFromBuilder(tRow).save(aConsumer, tRow.aId());
		}
		// task craftfrom-rockgt — the rockGt CraftFrom batch (Loader_OreProcessing.java:148)
		for (RockGtCraftFromRow tRow : rockGtCraftFromDatagenRows()) {
			rockGtCraftFromBuilder(tRow).save(aConsumer, tRow.aId());
		}
		// task craftfrom-residual — the 28 residual CraftFrom statements (the panel closeout:
		// the gear/rotor-buzzSaw/casing/small-parts/minecartWheels shaped bands + the shapeless panel)
		for (GearGtCraftFromRow tRow : gearGtCraftFromDatagenRows()) {
			gearGtCraftFromBuilder(tRow).save(aConsumer, tRow.aId());
		}
		for (ToolHeadCraftFromRow tRow : toolHeadCraftFromDatagenRows()) {
			toolHeadCraftFromBuilder(tRow).save(aConsumer, tRow.aId());
		}
		for (CasingCraftFromRow tRow : casingCraftFromDatagenRows()) {
			casingCraftFromBuilder(tRow).save(aConsumer, tRow.aId());
		}
		for (SmallPartCraftFromRow tRow : smallPartCraftFromDatagenRows()) {
			smallPartCraftFromBuilder(tRow).save(aConsumer, tRow.aId());
		}
		for (MinecartWheelsCraftFromRow tRow : minecartWheelsCraftFromDatagenRows()) {
			minecartWheelsCraftFromBuilder(tRow).save(aConsumer, tRow.aId());
		}
		for (ShapelessCraftFromRow tRow : shapelessCraftFromDatagenRows()) {
			shapelessCraftFromBuilder(tRow).save(aConsumer, tRow.aId());
		}
		// task debt-stairs-wall-vanilla-recipes — the upstream BlockStones vanilla-degradation rows
		for (PartFamilyRecipeRow tRow : stairsFromRocksBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (PartFamilyRecipeRow tRow : cobbleStairsWallBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		// task debt-slab-gap — the upstream mSlabs[0] conversion band (BlockMetaType.java:89/:93
		// generic rows x272 pairs + BlockStones.java:269/:327 stone rows x17)
		for (gregtech6.registry.GTStoneBlocks.VariantKey tKey : gregtech6.registry.GTStoneBlocks.registrationOrder()) {
			slabToBlockBuilder(tKey).save(aConsumer, slabToBlockId(tKey));
			slabSawBuilder(tKey).save(aConsumer, slabSawId(tKey));
		}
		for (gregtech6.registry.GTStoneBlocks.StoneSpec tStone : gregtech6.registry.GTStoneBlocks.STONES) {
			slabFromRocksBuilder(tStone).save(aConsumer, slabFromRocksId(tStone));
			slabFromCobbleBuilder(tStone).save(aConsumer, slabFromCobbleId(tStone));
		}

		// task debt-emitter-sensor-generators — the 20 live self-crafting rows of the three
		// technological component families (the pour map in the band javadoc below)
		compactComponentRows(aConsumer);
		// task debt-dungeon-keys-recipes — the ten dungeon-key rows (MultiItemRandomTools.java:589-598)
		for (PartFamilyRecipeRow tRow : keyRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		// task c4-copper-bridge — the four vanilla copper-ingot rows (the minecraft-namespace
		// overrides, the vanilla row ids verbatim; the copper-bridge band javadoc at the tail)
		vanillaBrushBuilder().save(aConsumer, VANILLA_BRUSH_ID);
		vanillaCopperBlockBuilder().save(aConsumer, VANILLA_COPPER_BLOCK_ID);
		vanillaLightningRodBuilder().save(aConsumer, VANILLA_LIGHTNING_ROD_ID);
		vanillaSpyglassBuilder().save(aConsumer, VANILLA_SPYGLASS_ID);
	}
	//?} else {
	/*@Override
	protected void buildRecipes(net.minecraft.data.recipes.RecipeOutput aOutput) {
		sprayCanEmptyBuilder().save(aOutput, SPRAY_CAN_EMPTY_ID);
		hammerFromStoneBuilder().save(aOutput, HAMMER_STONE_ID);
		hammerFromIngotsBuilder().save(aOutput, HAMMER_INGOTS_ID);
		wrenchBuilder().save(aOutput, WRENCH_ID);
		bendingCylinderSmallBuilder().save(aOutput, BENDING_CYLINDER_SMALL_ID);
		foodCanEmptyBuilder().save(aOutput, FOOD_CAN_EMPTY_ID);
		softHammerBuilder().save(aOutput, SOFT_HAMMER_ID);
		monkeyWrenchBuilder().save(aOutput, MONKEY_WRENCH_ID);
		magnifyingGlassBuilder().save(aOutput, MAGNIFYING_GLASS_ID);
		pincersBuilder().save(aOutput, PINCERS_ID);
		shapeExtruderPlateBuilder().save(aOutput, SHAPE_EXTRUDER_PLATE_ID);
		shapeExtruderRodBuilder().save(aOutput, SHAPE_EXTRUDER_ROD_ID);
		largeSteelCrucibleBuilder().save(aOutput, LARGE_STEEL_CRUCIBLE_ID);
		bathingPotSteelBuilder().save(aOutput, BATHING_POT_STEEL_ID);
		clayBowlForwardBuilder().save(aOutput, CLAY_BOWL_FORWARD_ID);
		clayBowlReverseBuilder().save(aOutput, CLAY_BOWL_REVERSE_ID);
		clayBowlSmeltingBuilder().save(aOutput, CLAY_BOWL_SMELT_ID);
		clayJuicerBuilder().save(aOutput, CLAY_JUICER_ID);
		clayJuicerReverseBuilder().save(aOutput, CLAY_JUICER_REVERSE_ID);
		clayJuicerSmeltingBuilder().save(aOutput, CLAY_JUICER_SMELT_ID);
		anvilBuilder(GT6Anvils.STONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.STONE).save(aOutput, STONE_ANVIL_ID);
		anvilBuilder(GT6Anvils.BLACKSTONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.BLACKSTONE).save(aOutput, BLACKSTONE_ANVIL_ID);
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) {
			hopperRecipeBuilder(tRow).save(aOutput, hopperRecipeId(tRow));
		}
		progressmeterBuilder().save(aOutput, PROGRESSMETER_ID);
		miniPortalNetherBuilder().save(aOutput, MINI_PORTAL_NETHER_ID); // task portals-mini-nether-end
		miniPortalEndBuilder().save(aOutput, MINI_PORTAL_END_ID); // task portals-mini-nether-end
		fluidometerBuilder().save(aOutput, FLUIDOMETER_ID);
		feConverterBuilder().save(aOutput, FE_CONVERTER_ID);
		waterWheelBuilder().save(aOutput, WATER_WHEEL_ID); // task pool-waterwheel-neo-recipes — the forge branch row (this file :195), the 21.1 face was born without it
		for (BridgeCraftRow tRow : transformerCraftingRows()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (BridgeCraftRow tRow : ldTransformerCraftingRows()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (BridgeCraftRow tRow : ldWireCraftingRows()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (BridgeCraftRow tRow : euBridgeCraftingRows()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (SensorRecastRow tRow : sensorRecastBuilders()) {
			tRow.builder().save(aOutput, sensorRecastId(tRow.path()));
		}
		for (GrassRecipeRow tRow : grassRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (gregtech6.registry.GT6StaticStorages.StaticRow tRow : gregtech6.registry.GT6StaticStorages.ROWS) {
			staticStorageRecipeBuilder(tRow).save(aOutput, staticStorageRecipeId(tRow));
		}
		for (PartFamilyRecipeRow tRow : partFamilyRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (PartFamilyRecipeRow tRow : tankValveRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (CrucibleLadderRecipeRow tRow : crucibleLadderRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		// task crucible-wall-obtainability — the 8 dedicated crucible-wall rows
		for (PartFamilyRecipeRow tRow : crucibleWallRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		// task recipes-obtainability — the 11 shared machine-wall rows
		for (PartFamilyRecipeRow tRow : machineWallRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		// task recipes-obtainability — the 8 slicer-blade rows (frame + 7 forms, ruling B)
		for (PartFamilyRecipeRow tRow : slicerBladeRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (GT6Batteries.BatteryRow tRow : GT6Batteries.ROWS) {
			if (tRow.family().startsWith("energium")) continue; // the crystals carry NO rows (upstream :1079-:1092, the declared cut)
			batteryRecipeBuilder(tRow).save(aOutput, batteryRecipeId(tRow));
		}
		// task placeables — the Greg o'Lantern row
		gregOLanternBuilder().save(aOutput, GREG_O_LANTERN_ID);
		for (BatteryBoxRecipeRow tRow : batteryBoxRecipeBuilders()) {
			tRow.builder().save(aOutput, batteryBoxRecipeId(tRow.row()));
		}
		for (DieselEngineRecipeRow tRow : dieselEngineRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (CrackerRecipeRow tRow : crackerRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (CrackerRecipeRow tRow : burnerMixerRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		// task usb-peripherals — the 8 USB peripheral rows (the cables :808-811, the HDDs :819-822)
		for (BridgeCraftRow tRow : usbCableRecipeRows()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (BridgeCraftRow tRow : usbDriveRecipeRows()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		// task debt-scanner-t3-usb-stick — the 4 USB Stick rows (:796-799) + the
		// Molecular Scanner T3 controller row (:1551, the Q1=(b) seam restore)
		for (BridgeCraftRow tRow : usbStickRecipeRows()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		CrackerRecipeRow tScanner = molecularScannerRow();
		tScanner.builder().save(aOutput, tScanner.id());
		// task w5-t1-dig-six — the six dig-tool steel-route rows (the wrench row shape)
		pickaxeBuilder().save(aOutput, PICKAXE_ID);
		pickaxeGemBuilder().save(aOutput, PICKAXE_GEM_ID);
		pickaxeConstructionBuilder().save(aOutput, PICKAXE_CONSTRUCTION_ID);
		shovelBuilder().save(aOutput, SHOVEL_ID);
		spadeBuilder().save(aOutput, SPADE_ID);
		universalSpadeBuilder().save(aOutput, UNIVERSAL_SPADE_ID);
		// task w5-t2-blade-six — the six blade-tool steel-route rows (the dig-tool row shape)
		clubBuilder().save(aOutput, CLUB_ID);
		// task blade-ladder — the three blade forms move to the per-material rows
		// (the OreProcessing_Tool rows Loader_Tools.java:321-323; the t1 steel-route
		// convergence placeholders for sword/knife/butchery retire — club stays the
		// single-tier convergence row, the single-tier-ruling card owns it)
		bladeLadderRows(aOutput);
		axeBuilder().save(aOutput, AXE_ID);
		axeDoubleBuilder().save(aOutput, AXE_DOUBLE_ID);
		// task w5-t4-field-five — the five field-tool steel-route rows (the t1 shape)
		hoeBuilder().save(aOutput, HOE_ID);
		plowBuilder().save(aOutput, PLOW_ID);
		branchCutterBuilder().save(aOutput, BRANCH_CUTTER_ID);
		senseBuilder().save(aOutput, SENSE_ID);
		handDrillBuilder().save(aOutput, HAND_DRILL_ID);
		// task w5-t5-scene-six — the six scene-tool rows (the same steel-route shape)
		scissorsBuilder().save(aOutput, SCISSORS_ID);
		scoopBuilder().save(aOutput, SCOOP_ID);
		net.minecraft.world.item.Item tRubberPlate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Rubber);
		if (tRubberPlate != null) plungerBuilder(tRubberPlate).save(aOutput, PLUNGER_ID); // the CR.ONLY_IF_HAS_RESULT face — no rubber plate, no row
		flintAndTinderFromFlintAndSteelBuilder().save(aOutput, FLINT_AND_TINDER_ID);
		flintAndSteelBuilder().save(aOutput, FLINT_AND_STEEL_ID);
		rollingPinBuilder().save(aOutput, ROLLING_PIN_ID);
		bendingCylinderBuilder().save(aOutput, BENDING_CYLINDER_ID);
		// task w5-t6-electric-nineteen — the fifteen electric rows (the :356-377 convergence)
		for (ElectricToolRow tRow : electricToolRows()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		pocketMultitoolBuilder().save(aOutput, POCKET_MULTITOOL_ID);
		// task w5-t8-armor-24 — the 24 hazmat rows (tail-append)
		for (GT6ArmorMaterials.SuitRow tSuit : GT6ArmorMaterials.SUITS) {
			for (int i = 0; i < GT6ArmorMaterials.PIECE_TYPES.length; i++) {
				armorPieceBuilder(tSuit, i).save(aOutput, armorRecipeId(tSuit, i));
			}
		}
		// task dig-ladder — the per-material identity-stamped rows (the axis walk)
		digLadderRows(aOutput);
		machineLadderRows(aOutput);
		toolHeadRows(aOutput);
		toolAssemblyRows(aOutput);
		// task circuits-crafting-c — the circuits band: the 26 integrated-circuit rows
		// (gt6:circuit_program) + the ventilation/processor-unit six (gt6 shaped)
		circuitProgramRows(aOutput);
		partCircuitRows(aOutput);
		// task bumbliary-recipes — the Bumbliary pair rows (the :2222/:2223 line-tail varargs)
		bumbliaryBuilder().save(aOutput, BUMBLIARY_ID);
		advancedBumbliaryBuilder().save(aOutput, BUMBLIARY_ADVANCED_ID);
		// task rails-31-blocks — the 30 rail rows (the :107-140 no-RC fallback band)
		for (GT6Rails.RailRow tRailRow : GT6Rails.ROWS) {
			railRecipeBuilder(tRailRow).save(aOutput, railRecipeId(tRailRow));
		}
		// task craftfrom-plategem — the CraftFrom hand-craft family (Loader_OreProcessing.java:171-178)
		for (CraftFromRow tRow : craftFromDatagenRows()) {
			craftFromBuilder(tRow).save(aOutput, tRow.aId());
		}
		// task craftfrom-stick — the stick/stickLong CraftFrom family (Loader_OreProcessing.java:156-163)
		for (StickCraftFromRow tRow : stickCraftFromDatagenRows()) {
			stickCraftFromBuilder(tRow).save(aOutput, tRow.aId());
		}
		// task craftfrom-foil — the fine-wire CraftFrom batch (Loader_OreProcessing.java:168-169)
		for (FineWireCraftFromRow tRow : fineWireCraftFromDatagenRows()) {
			fineWireCraftFromBuilder(tRow).save(aOutput, tRow.aId());
		}
		// task craftfrom-rockgt — the rockGt CraftFrom batch (Loader_OreProcessing.java:148)
		for (RockGtCraftFromRow tRow : rockGtCraftFromDatagenRows()) {
			rockGtCraftFromBuilder(tRow).save(aOutput, tRow.aId());
		}
		// task craftfrom-residual — the 28 residual CraftFrom statements (the panel closeout:
		// the gear/rotor-buzzSaw/casing/small-parts/minecartWheels shaped bands + the shapeless panel)
		for (GearGtCraftFromRow tRow : gearGtCraftFromDatagenRows()) {
			gearGtCraftFromBuilder(tRow).save(aOutput, tRow.aId());
		}
		for (ToolHeadCraftFromRow tRow : toolHeadCraftFromDatagenRows()) {
			toolHeadCraftFromBuilder(tRow).save(aOutput, tRow.aId());
		}
		for (CasingCraftFromRow tRow : casingCraftFromDatagenRows()) {
			casingCraftFromBuilder(tRow).save(aOutput, tRow.aId());
		}
		for (SmallPartCraftFromRow tRow : smallPartCraftFromDatagenRows()) {
			smallPartCraftFromBuilder(tRow).save(aOutput, tRow.aId());
		}
		for (MinecartWheelsCraftFromRow tRow : minecartWheelsCraftFromDatagenRows()) {
			minecartWheelsCraftFromBuilder(tRow).save(aOutput, tRow.aId());
		}
		for (ShapelessCraftFromRow tRow : shapelessCraftFromDatagenRows()) {
			shapelessCraftFromBuilder(tRow).save(aOutput, tRow.aId());
		}
		// task debt-stairs-wall-vanilla-recipes — the upstream BlockStones vanilla-degradation rows
		for (PartFamilyRecipeRow tRow : stairsFromRocksBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (PartFamilyRecipeRow tRow : cobbleStairsWallBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		// task debt-slab-gap — the upstream mSlabs[0] conversion band (the forge-leg tail mirror)
		for (gregtech6.registry.GTStoneBlocks.VariantKey tKey : gregtech6.registry.GTStoneBlocks.registrationOrder()) {
			slabToBlockBuilder(tKey).save(aOutput, slabToBlockId(tKey));
			slabSawBuilder(tKey).save(aOutput, slabSawId(tKey));
		}
		for (gregtech6.registry.GTStoneBlocks.StoneSpec tStone : gregtech6.registry.GTStoneBlocks.STONES) {
			slabFromRocksBuilder(tStone).save(aOutput, slabFromRocksId(tStone));
			slabFromCobbleBuilder(tStone).save(aOutput, slabFromCobbleId(tStone));
		}

		// task debt-emitter-sensor-generators — the 20 live self-crafting rows of the three
		// technological component families (the pour map in the band javadoc below)
		compactComponentRows(aOutput);
		// task debt-dungeon-keys-recipes — the ten dungeon-key rows (MultiItemRandomTools.java:589-598)
		for (PartFamilyRecipeRow tRow : keyRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		// task c4-copper-bridge — the four vanilla copper-ingot rows (the minecraft-namespace
		// overrides, the vanilla row ids verbatim; the copper-bridge band javadoc at the tail)
		vanillaBrushBuilder().save(aOutput, VANILLA_BRUSH_ID);
		vanillaCopperBlockBuilder().save(aOutput, VANILLA_COPPER_BLOCK_ID);
		vanillaLightningRodBuilder().save(aOutput, VANILLA_LIGHTNING_ROD_ID);
		vanillaSpyglassBuilder().save(aOutput, VANILLA_SPYGLASS_ID);
	}
	*///?}

	/**
	 * The stone-slab conversion band ids (task debt-slab-gap) — foldered, the
	 * stairs-card naming convention: {@code slab_rock/<stone>} (BlockStones.java:269),
	 * {@code slab_cobble/<stone>} (:327), {@code slab_to_block/<pair>} and
	 * {@code slab_saw/<pair>} (the BlockMetaType.java:89/:93 generic rows). The paths
	 * ride locals so the two-arg RL ctor args stay bare identifiers (the swap-table
	 * regex note).
	 */
	public static ResourceLocation slabFromRocksId(gregtech6.registry.GTStoneBlocks.StoneSpec aStone) {
		String tPath = "slab_rock/" + aStone.snake();
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	public static ResourceLocation slabFromCobbleId(gregtech6.registry.GTStoneBlocks.StoneSpec aStone) {
		String tPath = "slab_cobble/" + aStone.snake();
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	public static ResourceLocation slabToBlockId(gregtech6.registry.GTStoneBlocks.VariantKey aKey) {
		String tPath = "slab_to_block/" + gregtech6.registry.GTStoneBlocks.path(aKey.stone().snake(), aKey.variant());
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	public static ResourceLocation slabSawId(gregtech6.registry.GTStoneBlocks.VariantKey aKey) {
		String tPath = "slab_saw/" + gregtech6.registry.GTStoneBlocks.path(aKey.stone().snake(), aKey.variant());
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/**
	 * The 2-slab-to-block row (BlockMetaType.java:89, {@code CR.shaped(this, "X","X", 'X'
	 * = mSlabs[0])}) — one per (stone, variant) pair: two slabs stacked vertically make
	 * the full block. The ONLY input is the pair's own slab item; the result is the pair's
	 * own block item.
	 */
	private ShapedRecipeBuilder slabToBlockBuilder(gregtech6.registry.GTStoneBlocks.VariantKey aKey) {
		Item tSlabItem = gregtech6.registry.GTStoneSlabBlocks.item(aKey.stone().snake(), aKey.variant()).get();
		return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS,
						gregtech6.registry.GTStoneBlocks.item(aKey.stone().snake(), aKey.variant()).get())
				.pattern("X")
				.pattern("X")
				.define('X', tSlabItem)
				.unlockedBy("has_slab", has(tSlabItem));
	}

	/**
	 * The block-plus-saw row (BlockMetaType.java:93, {@code CR.shaped(mSlabs[0] x2, "sX",
	 * 'X' = this)} — the crafting-table face of the :92 sawmill row): 's' = the port saw
	 * tool tag ({@link GT6ItemTags#TOOLS_SAW}, the spray-can row's s-key precedent), 'X'
	 * = the pair's own block item, result TWO slabs (the 1:2 material ratio the :92/:93
	 * pair pins both ways).
	 */
	private ShapedRecipeBuilder slabSawBuilder(gregtech6.registry.GTStoneBlocks.VariantKey aKey) {
		Item tBlockItem = gregtech6.registry.GTStoneBlocks.item(aKey.stone().snake(), aKey.variant()).get();
		return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS,
						gregtech6.registry.GTStoneSlabBlocks.item(aKey.stone().snake(), aKey.variant()).get(), 2)
				.pattern("sX")
				.define('s', GT6ItemTags.TOOLS_SAW)
				.define('X', tBlockItem)
				.unlockedBy("has_stone", has(tBlockItem));
	}

	/**
	 * The rocks row (BlockStones.java:269, {@code CR.shaped(mSlabs[0] x1 COBBL, "  ","XX",
	 * 'X' = OP.rockGt.dat(mMaterial))}) — two small rocks make ONE COBBL-variant slab
	 * (two rocks = half a cobble block per the :261-262 4-rocks row, one slab = half a
	 * block — the 1:2 stone economy). Unconditional over all 17 families (:268-269 sit
	 * OUTSIDE the NePl/NeLi basalt gate). Input resolves through GTMaterialItems (the
	 * rock_gt_<snake> items; the PrismarineLight family's rock id is rock_gt_prismarine,
	 * the MT.java:2397 internal-name quirk).
	 */
	private ShapedRecipeBuilder slabFromRocksBuilder(gregtech6.registry.GTStoneBlocks.StoneSpec aStone) {
		Item tRock = GTMaterialItems.get(gregapi.data.OP.rockGt, aStone.material().get()).get();
		return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS,
						gregtech6.registry.GTStoneSlabBlocks.item(aStone.snake(), gregtech6.block.stone.StoneVariant.COBBL).get())
				.pattern("  ")
				.pattern("XX")
				.define('X', tRock)
				.unlockedBy("has_rock", has(tRock));
	}

	/**
	 * The cobble row (BlockStones.java:327, inside the {@code mEqualBlocks[COBBL]} loop —
	 * exactly the 17 family cobblestones, the :254 self-add): two COBBL blocks make FOUR
	 * COBBL-variant slabs (the 1:2 expansion again — 1 block = 2 slabs, the sawmill ratio).
	 */
	private ShapedRecipeBuilder slabFromCobbleBuilder(gregtech6.registry.GTStoneBlocks.StoneSpec aStone) {
		Item tCobble = gregtech6.registry.GTStoneBlocks.item(aStone.snake(), gregtech6.block.stone.StoneVariant.COBBL).get();
		return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS,
						gregtech6.registry.GTStoneSlabBlocks.item(aStone.snake(), gregtech6.block.stone.StoneVariant.COBBL).get(), 4)
				.pattern("  ")
				.pattern("XX")
				.define('X', tCobble)
				.unlockedBy("has_cobble", has(tCobble));
	}

	/**
	 * The id of one static storage row's recipe (the result-path convention, one per row —
	 * the hopperRecipeId shape). The path rides a local so the two-arg RL ctor args stay
	 * bare identifiers (the swap-table regex note).
	 */
	public static ResourceLocation staticStorageRecipeId(gregtech6.registry.GT6StaticStorages.StaticRow aRow) {
		String tPath = aRow.path();
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/**
	 * The static storage crafting rows (task storage-static-batch — one per row):
	 * <ul>
	 * <li>Locker (:138 "SdS","LCL","TMT"): 'T' = screw, 'M' = casing — the upstream
	 *     casingMachine column folds to casingSmall (the prefix has no port item row, the
	 *     declared deviation), 'L' = leather, 'C' = the chest tag — the upstream
	 *     same-material metal chest center folds to the platform chest tag (the metal
	 *     chest ladder is not in this port universe, the declared deviation);</li>
	 * <li>Drawer (:140 "CTC","TdT","CTC"): 'd' = the screwdriver tool tag;</li>
	 * <li>Safes (:134-135 "PGP","GOS"/"OGS","PGP"): 'P' = plateQuintuple, 'G' =
	 *     gearGtSmall, 'O' = gearGt, 'S' = stick;</li>
	 * <li>Wooden Bookshelf (:177-179 "PPP","sfr","PPP"): 'P' = THE ROW'S plank item, 's'
	 *     = the hard hammer tag (the upstream soft-hammer letter folds — no soft-hammer
	 *     tag in the port), 'f' = the file tag, 'r' = the screwdriver tag;</li>
	 * <li>Wooden Bottlecrate (:180 "sfr","PGP","BPB"): 'B' = the wood bolt, 'G' = a slime
	 *     ball (the upstream itemGlue column folds — no glue item in the port universe,
	 *     the declared deviation).</li>
	 * </ul>
	 * Tool letters key on the gt6 tool tags (the p24/p25 tag rulings), material items
	 * resolve through GTMaterialItems (the hopper plateCurved shape).
	 */
	/**
	 * The rail rows (task rails-31-blocks) — the upstream :107-140 no-RC fallback band,
	 * walked over the {@link GT6Rails#ROWS} table: 10 normals ("RSR"/"RSR"/"RSR", railGt +
	 * treated-wood stick, 4 out), 10 boosters ("RSR"/"GDG"/"RSR", the gold-family railGt
	 * column + redstone) and 10 detectors ("RSR"/"RPR"/"RDR", redstone + the stone
	 * pressure plate). The upstream :74-106 RC branch is CUT (IL.RC_Bed_Wood absent from
	 * the port universe — the card ruling) and the vanilla-replacement band (:141-156,
	 * the DEL_OTHER_SHAPED_RECIPES overrides) stays out — coexistence, the card
	 * investigation declaration.
	 */
	/** The recipe id — the result path (the vanilla naming convention, the grassRecipeId form). */
	private static ResourceLocation railRecipeId(GT6Rails.RailRow aRow) {
		String tPath = aRow.path(); // a local so the two-arg RL ctor args stay bare identifiers (the swap-table regex note)
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/** One rail row's builder — the :108-139 column pairs over the shared "R"/"S" frame. */
	private ShapedRecipeBuilder railRecipeBuilder(GT6Rails.RailRow aRow) {
		ShapedRecipeBuilder rBuilder = ShapedRecipeBuilder.shaped(net.minecraft.data.recipes.RecipeCategory.TRANSPORTATION,
				gregtech6.registry.GT6Rails.ITEMS_BY_PATH.get(aRow.path()).get(), 4)
				.pattern("RSR")
				.pattern(middleRow(aRow))
				.pattern(bottomRow(aRow))
				.define('R', GTMaterialItems.get(gregapi.data.OP.railGt, aRow.material()).get())
				.define('S', GTMaterialItems.get(gregapi.data.OP.stick, gregapi.data.MT.WoodTreated).get())
				.unlockedBy("has_rail", has(GTMaterialItems.get(gregapi.data.OP.railGt, aRow.material()).get()));
		if (aRow.kind() == GT6Rails.RailKind.BOOSTER) {
			rBuilder.define('D', net.minecraft.world.item.Items.REDSTONE);
			rBuilder.define('G', GTMaterialItems.get(gregapi.data.OP.railGt, boosterGoldColumn(aRow.material())).get());
		} else if (aRow.kind() == GT6Rails.RailKind.DETECTOR) {
			rBuilder.define('D', net.minecraft.world.item.Items.REDSTONE);
			rBuilder.define('P', net.minecraft.world.item.Items.STONE_PRESSURE_PLATE);
		}
		return rBuilder;
	}

	/** The middle pattern row: boosters "GDG", detectors "RPR", normals "RSR" (the :119/:130 frame columns). */
	private static String middleRow(GT6Rails.RailRow aRow) {
		return switch (aRow.kind()) {
			case BOOSTER -> "GDG";
			case DETECTOR -> "RPR";
			case NORMAL -> "RSR";
		};
	}

	/** The bottom pattern row: the detector's "RDR" arm (:130, the redstone sits bottom-middle), the others "RSR". */
	private static String bottomRow(GT6Rails.RailRow aRow) {
		return aRow.kind() == GT6Rails.RailKind.DETECTOR ? "RDR" : "RSR";
	}

	/** The booster 'G' rail material column (the :119-128 rows read line by line). */
	private static gregapi.oredict.OreDictMaterial boosterGoldColumn(gregapi.oredict.OreDictMaterial aMaterial) {
		if (aMaterial == gregapi.data.MT.Steel || aMaterial == gregapi.data.MT.StainlessSteel) return gregapi.data.MT.Au;
		if (aMaterial == gregapi.data.MT.TungstenSteel || aMaterial == gregapi.data.MT.TungstenCarbide) return gregapi.data.MT.Pt;
		if (aMaterial == gregapi.data.MT.Titanium || aMaterial == gregapi.data.MT.W) return gregapi.data.MT.Electrum;
		if (aMaterial == gregapi.data.MT.Adamantium) return gregapi.data.MT.Os;
		return gregapi.data.MT.Ag; // Al / Magnalium / Bronze
	}

	private ShapedRecipeBuilder staticStorageRecipeBuilder(gregtech6.registry.GT6StaticStorages.StaticRow aRow) {
		ShapedRecipeBuilder rBuilder = switch (aRow.kind()) {
			case LOCKER -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, resultOf(aRow))
					.pattern("SdS").pattern("LCL").pattern("TMT")
					.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
					.define('S', GTMaterialItems.get(gregapi.data.OP.stick, aRow.material().mt()).get())
					.define('T', GTMaterialItems.get(gregapi.data.OP.screw, aRow.material().mt()).get())
					.define('M', GTMaterialItems.get(gregapi.data.OP.casingSmall, aRow.material().mt()).get())
					.define('L', Items.LEATHER)
					.define('C', Tags.Items.CHESTS);
			case DRAWER -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, resultOf(aRow))
					.pattern("CTC").pattern("TdT").pattern("CTC")
					.define('C', Tags.Items.CHESTS)
					.define('T', GTMaterialItems.get(gregapi.data.OP.screw, aRow.material().mt()).get())
					.define('d', GT6ItemTags.TOOLS_SCREWDRIVER);
			case SAFE_MECHANICAL -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, resultOf(aRow))
					.pattern("PGP").pattern("GOS").pattern("PGP")
					.define('P', GTMaterialItems.get(gregapi.data.OP.plateQuintuple, aRow.material().mt()).get())
					.define('G', GTMaterialItems.get(gregapi.data.OP.gearGtSmall, aRow.material().mt()).get())
					.define('O', GTMaterialItems.get(gregapi.data.OP.gearGt, aRow.material().mt()).get())
					.define('S', GTMaterialItems.get(gregapi.data.OP.stick, aRow.material().mt()).get());
			case SAFE_KEYLOCKED -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, resultOf(aRow))
					.pattern("PGP").pattern("OGS").pattern("PGP")
					.define('P', GTMaterialItems.get(gregapi.data.OP.plateQuintuple, aRow.material().mt()).get())
					.define('G', GTMaterialItems.get(gregapi.data.OP.gearGtSmall, aRow.material().mt()).get())
					.define('O', GTMaterialItems.get(gregapi.data.OP.gearGt, aRow.material().mt()).get())
					.define('S', GTMaterialItems.get(gregapi.data.OP.stick, aRow.material().mt()).get());
			case BOOKSHELF -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, resultOf(aRow))
					.pattern("PPP").pattern("sfr").pattern("PPP")
					.define('P', aRow.plank().item())
					.define('s', GT6ItemTags.TOOLS_HARD_HAMMER)
					.define('f', GT6ItemTags.TOOLS_FILE)
					.define('r', GT6ItemTags.TOOLS_SCREWDRIVER);
			case BOTTLECRATE -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, resultOf(aRow))
					.pattern("sfr").pattern("PGP").pattern("BPB")
					.define('P', aRow.plank().item())
					.define('B', GTMaterialItems.get(gregapi.data.OP.bolt, gregapi.data.MT.Wood).get())
					.define('G', Items.SLIME_BALL)
					.define('s', GT6ItemTags.TOOLS_HARD_HAMMER)
					.define('f', GT6ItemTags.TOOLS_FILE)
					.define('r', GT6ItemTags.TOOLS_SCREWDRIVER);
		};
		// the unlock arms: the material plate column for the metal rows, the plank for the wooden
		if (aRow.material() != null) {
			rBuilder.unlockedBy("has_plate", has(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, aRow.material().slug())));
		} else {
			rBuilder.unlockedBy("has_plank", has(aRow.plank().item()));
		}
		return rBuilder;
	}

	/** The recipe result item of a row (the registered BlockItem). */
	private static Item resultOf(gregtech6.registry.GT6StaticStorages.StaticRow aRow) {
		return gregtech6.registry.GT6StaticStorages.ITEMS_BY_PATH.get(aRow.path()).get();
	}

	/** One staged recipe: the shared builder + the id its save face writes (the save type is the one leg split). */
	private record GrassRecipeRow(ShapelessRecipeBuilder builder, ResourceLocation id) {}

	/** The forward recipe id of variant i — the result path (the vanilla naming convention). */
	public static ResourceLocation grassRecipeId(int aVariant) {
		String tPath = GTGrassBlocks.PATHS.get(aVariant); // a local so the two-arg RL ctor args stay bare identifiers (the swap-table regex note)
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/** The reverse recipe id of variant i — the variant path + the {@code _reverse} suffix. */
	public static ResourceLocation grassReverseRecipeId(int aVariant) {
		String tPath = GTGrassBlocks.PATHS.get(aVariant) + "_reverse"; // the same bare-identifier discipline
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/**
	 * The grass dye band (task grass-block) — the upstream BlockGrass.java:72-80
	 * registrations as 12 shapeless rows, ONLY the save face forked (the ctor rule):
	 * <ul>
	 * <li><b>forward ×6</b>: 8 vanilla grass BLOCKS (the literal {@code Items.GRASS_BLOCK}
	 * — the 1.20.1 name of what 1.7.10 called {@code Blocks.grass}; no generic grass tag
	 * exists, the census verdict) + 1 dye → 8 of the variant item (BlockGrass.java:75-80,
	 * count 8 on the result). The dye input is the PLATFORM dye tag per variant colour —
	 * {@code Tags.Items.DYES_<COLOR>} (forge Tags.java:226-241 {@code DyeColor.getTag()}
	 * face; the NeoForge constant is the same name over the {@code c:} namespace, the
	 * import fork above carries the whole fork surface). No bare dye item anywhere.</li>
	 * <li><b>reverse ×6</b>: 1 variant item → 1 vanilla grass block (the upstream
	 * {@code RM.generify} + {@code CR.shapeless(ST.make(Blocks.grass, 1, 0), new
	 * Object[] {this})} pair, :72-73 — the generify face has no modern equivalent, the
	 * shapeless row IS the portable half, the shapeless id notes it).</li>
	 * </ul>
	 * Row order = the {@link GTGrassBlocks#VARIANTS} order (forward, reverse alternating
	 * would be fine — each row saves under its own id; the appender order is variant-major
	 * forward-first, deterministic output).
	 *
	 * <p>NOT this card: the Bath dye-fluid rows (Loader_Recipes_Other.java:467-472 — the
	 * Canner/fluid domain) and the Sifting table (Loader_Recipes_Ores.java:225 — pooled,
	 * coarse_dirt conversion + cross-mod bait cut).
	 */
	private List<GrassRecipeRow> grassRecipeBuilders() {
		List<GrassRecipeRow> rRows = new ArrayList<>(GTGrassBlocks.VARIANTS.size() * 2);
		for (int i = 0; i < GTGrassBlocks.VARIANTS.size(); i++) {
			Item tVariantItem = GTGrassBlocks.ITEMS.get(i).get();
			// forward: 8 vanilla grass + 1 dye → 8 variant items (BlockGrass.java:75-80)
			rRows.add(new GrassRecipeRow(ShapelessRecipeBuilder
					.shapeless(RecipeCategory.DECORATIONS, tVariantItem, 8)
					.requires(Items.GRASS_BLOCK, 8)
					.requires(dyeTagOf(GTGrassBlocks.VARIANTS.get(i).dyeIndex()))
					.unlockedBy("has_grass_block", has(Items.GRASS_BLOCK)),
					grassRecipeId(i)));
			// reverse: 1 variant item → 1 vanilla grass block (BlockGrass.java:72-73)
			rRows.add(new GrassRecipeRow(ShapelessRecipeBuilder
					.shapeless(RecipeCategory.DECORATIONS, Items.GRASS_BLOCK)
					.requires(tVariantItem)
					.unlockedBy("has_gt6_grass", has(tVariantItem)),
					grassReverseRecipeId(i)));
		}
		return rRows;
	}

	/** The platform dye tag of a GT6 spray dye index (the six grass-effective colours). */
	private static TagKey<Item> dyeTagOf(byte aDyeIndex) {
		return switch (aDyeIndex) {
			case 2 -> Tags.Items.DYES_GREEN; // variant 0 (Behavior_Spray_Color.java:154)
			case 10 -> Tags.Items.DYES_LIME; // variant 1 (:155)
			case 0 -> Tags.Items.DYES_BLACK; // variant 2 (:156)
			case 7 -> Tags.Items.DYES_LIGHT_GRAY; // variant 3 (:157)
			case 11 -> Tags.Items.DYES_YELLOW; // variant 4 (:158)
			case 3 -> Tags.Items.DYES_BROWN; // variant 5 (:159)
			default -> throw new IllegalArgumentException("not a grass dye: " + aDyeIndex);
		};
	}

	/**
	 * The shared builder chain — the upstream MultiItemRandomTools.java:240 shape with
	 * every key on a {@link GT6ItemTags} tag. NOT the save face: the save call stays in
	 * the {@code buildRecipes} forks because the sink type is the one leg split the
	 * builder chain cannot paper over.
	 */
	private ShapedRecipeBuilder sprayCanEmptyBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6SprayCans.SPRAY_CAN_EMPTY.get())
				.pattern("Rf")
				.pattern("Cs")
				.define('R', GT6ItemTags.REDSTONE_DUSTS)
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('C', GT6ItemTags.PLATE_CURVED_TIN)
				.define('s', GT6ItemTags.TOOLS_SAW)
				.unlockedBy("has_redstone_dust", has(GT6ItemTags.REDSTONE_DUSTS));
	}

	/**
	 * The hammer STONE route (task tool-hammer-wrench spec ⑤α) — the upstream
	 * {@code "XX "}/{@code "XXS"}/{@code "XX "} row (Loader_Tools.java:277/:285,
	 * {@code CR.DEF_MIR}) with the input translated: upstream 'X' walks
	 * {@code rockGt.dat(tRock)} over the stone/pebble material universe, the port keys
	 * the WHOLE input face on the vanilla {@code #minecraft:stone_tool_materials} tag
	 * ({@link ItemTags#STONE_TOOL_MATERIALS}) — the cobblestone/blackstone/
	 * cobbled_deepslate trio IS the portable form of the rockGt early-game face.
	 * Double-source probe (the card's不许猜 duty): 1.20.1 decompile ItemTags.java:83;
	 * 1.21.1 client jar {@code data/minecraft/tags/item/stone_tool_materials.json} +
	 * mojmap VanillaRecipeProvider usage — single-source, ZERO leg fork. 'S' stays the
	 * vanilla stick (upstream {@code tHandle[1]}, the handle-family pool cut). Vanilla
	 * shaped recipes auto-match the horizontal mirror, which IS the {@code CR.DEF_MIR}
	 * semantics. Result 1x {@code gt6:hammer}.
	 */
	private ShapedRecipeBuilder hammerFromStoneBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.HAMMER.get())
				.pattern("XX ")
				.pattern("XXS")
				.pattern("XX ")
				.define('X', ItemTags.STONE_TOOL_MATERIALS)
				.define('S', Items.STICK)
				.unlockedBy("has_stone_tool_materials", has(ItemTags.STONE_TOOL_MATERIALS));
	}

	/**
	 * The hammer METAL route (task tool-hammer-wrench spec ⑤β) — the upstream
	 * {@code "II "}/{@code "IIh"}/{@code "II "} row (Loader_Tools.java:327, the
	 * OreProcessing_Tool material loop flattened to ONE tag-keyed row): 'I' = the
	 * ecosystem generic ingots tag ({@code Tags.Items.INGOTS} — forge:ingots on 1.20.1,
	 * c:ingots on 21.1; the per-material loop folds onto the whole tag), 'h' =
	 * {@code #gt6:tools/hard_hammer} (the craftingToolHardHammer snake). The in-grid
	 * hammer pays one durability point per craft and rides along (the container-item
	 * channel, GT6FileItem.craftRemaining via GTHammerItem). Result 1x
	 * {@code gt6:hammer} — zero dead items (the hammer is consumed by its own making
	 * route's h-key just as upstream intended).
	 */
	private ShapedRecipeBuilder hammerFromIngotsBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.HAMMER.get())
				.pattern("II ")
				.pattern("IIh")
				.pattern("II ")
				.define('I', Tags.Items.INGOTS)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_ingot", has(Tags.Items.INGOTS));
	}

	/**
	 * The wrench self-craft row (task tool-hammer-wrench spec ⑤γ) — the upstream
	 * {@code "PhP"}/{@code " P "}/{@code " P "} row (Loader_Tools.java:310): 'P' =
	 * the steel plate platform tag ({@code gregtech6.datagen.GT6ItemTags.materialTag(PLATES_FAMILY,
	 * "steel")} — the forge:plates/steel form the plate family band already emits),
	 * 'h' = {@code #gt6:tools/hard_hammer} (the hammer MUST exist first — the
	 * dependency direction the card's merge order pins). The in-grid hammer pays one
	 * point and rides along. Result 1x {@code gt6:wrench} — the wrench's ONLY crafting
	 * row (the upstream consumption face is ≈zero, the research card's ripgrep verdict;
	 * the machine-dismantle interaction pool stays out of this card).
	 */
	private ShapedRecipeBuilder wrenchBuilder() {
		TagKey<Item> tSteelPlates = gregtech6.datagen.GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "steel");
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.WRENCH.get())
				.pattern("PhP")
				.pattern(" P ")
				.pattern(" P ")
				.define('P', tSteelPlates)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_steel_plate", has(tSteelPlates));
	}

	/**
	 * The bending-cylinder SELF-CRAFT row (task food-can-row0 spec ②) — the upstream
	 * {"sfh", "III"} row (Loader_Tools.java:313, the OreProcessing_Tool material loop
	 * flattened to ONE tag-keyed row, the hammer-ingots-route precedent; the per-material
	 * {@code typemin(2)} gate folds onto the whole INGOTS tag): 's' = #gt6:tools/saw,
	 * 'f' = #gt6:tools/file, 'h' = #gt6:tools/hard_hammer (the CR.java:200/201/211 tool
	 * alphabet — the three p25 tools this row CONSUMES in-grid, the live consumption
	 * chain), 'I' = the ecosystem generic ingots tag ({@code Tags.Items.INGOTS}; upstream
	 * walks {@code ingot.dat(tMat)} per metal — {@code setMaterialAmount(3*U)} = the
	 * 3-ingot row). Each tool pays one durability point per craft and rides along (the
	 * container-item channel via Recipe.getRemainingItems). Result 1x
	 * {@code gt6:bending_cylinder_small}.
	 */
	/**
	 * The storage-hopper crafting rows (task storage-hopper-family — the Loader
	 * metalset :145-146 recipes, one per Bronze/Steel × hopper/queue row): the upstream
	 * "PwP"/"XCX"/" Xh" (hopper) and "PCP"/"XCX"/"wXh" (queue) grids with 'P' =
	 * {@code OP.plate.dat(aMat)} → the {@code #forge:plates/<mat>} platform tag, 'X' =
	 * {@code OP.plateCurved.dat(aMat)} → the GTMaterialItems curved-plate item, 'C' =
	 * {@code OD.craftingChest} → {@code Tags.Items.CHESTS}, 'w'/'h' = the wrench/hammer
	 * tool tags (the bending-cylinder tool-letter mapping).
	 */
	private ShapedRecipeBuilder hopperRecipeBuilder(GT6Hoppers.HopperRow aRow) {
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GT6Hoppers.ITEMS_BY_PATH.get(aRow.path()).get())
				.pattern(aRow.queue() ? "PCP" : "PwP")
				.pattern("XCX")
				.pattern(aRow.queue() ? "wXh" : " Xh")
				.define('P', gregtech6.datagen.GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, aRow.material().slug()))
				.define('X', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateCurved, aRow.material().mt()).get())
				.define('C', Tags.Items.CHESTS)
				.define('w', GT6ItemTags.TOOLS_WRENCH)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_chest", has(Tags.Items.CHESTS));
	}

	private ShapedRecipeBuilder bendingCylinderSmallBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.BENDING_CYLINDER_SMALL.get())
				.pattern("sfh")
				.pattern("III")
				.define('s', GT6ItemTags.TOOLS_SAW)
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.define('I', Tags.Items.INGOTS)
				.unlockedBy("has_ingot", has(Tags.Items.INGOTS));
	}

	/**
	 * The soft-hammer self-craft row (task w5-t3-machine-face-four spec ①) — the
	 * upstream AdvancedCraftingTool(SOFTHAMMER, toolHeadHammer, …) head+handle row
	 * (Loader_Tools.java:334) flattened to ONE steel-tier row: 'H' =
	 * {@code OP.toolHeadHammer.dat(MT.Steel)} → the GTMaterialItems tool_head_hammer_steel
	 * item (the upstream toolHeadHammer head, :334 verbatim), 'S' = the vanilla stick (the
	 * handle, the hammerFromStoneBuilder 'S' precedent). The in-grid tools pay one point
	 * and ride along (the container-item channel). Result 1x {@code gt6:soft_hammer}.
	 */
	private ShapedRecipeBuilder softHammerBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.SOFT_HAMMER.get())
				.pattern("H")
				.pattern("S")
				.define('H', GTMaterialItems.get(gregapi.data.OP.toolHeadHammer, MT.Steel).get())
				.define('S', Items.STICK)
				.unlockedBy("has_tool_head_hammer", has(GTMaterialItems.get(gregapi.data.OP.toolHeadHammer, MT.Steel).get()));
	}

	/**
	 * The monkey-wrench self-craft row (task w5-t3-machine-face-four spec ②) — the
	 * wrenchBuilder "PhP"/" P "/" P " grid over the monkey-wrench result (the upstream
	 * monkey wrench IS the wrench-family variant, GT_Tool_MonkeyWrench extends
	 * GT_Tool_Wrench, machine/GT_Tool_MonkeyWrench.java:33; the :311 OreProcessing_Tool
	 * row folds — the per-material loop flattens onto the steel-plate tag, the wrench
	 * builder precedent). Result 1x {@code gt6:monkey_wrench} — ingredient tag
	 * {@code #gt6:tools/monkey_wrench} single-name (no wrench fold, the card ruling).
	 */
	private ShapedRecipeBuilder monkeyWrenchBuilder() {
		TagKey<Item> tSteelPlates = gregtech6.datagen.GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "steel");
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.MONKEY_WRENCH.get())
				.pattern("PhP")
				.pattern(" P ")
				.pattern(" P ")
				.define('P', tSteelPlates)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_steel_plate", has(tSteelPlates));
	}

	/**
	 * The POCKET MULTITOOL crafting row (task w5-t7-pocket-eight) — the upstream
	 * OreProcessing_Tool row over toolHeadScrewdriver (Loader_Tools.java:354) with its
	 * {"AXO","ZPV","OWY"} grid kept letter-verbatim and the tool-head universe folded to
	 * the steel convergence (the W5 ruling d: the per-material listener loop flattens to
	 * ONE steel row): 'A' (the listening screwdriver HEAD) = the {@code #gt6:tools/screwdriver}
	 * tag, 'X' = the saw tag, 'Z' = the file tag, 'Y' (the chisel head) = the
	 * {@code gt6:chisel} item (no TOOLS_CHISEL tag exists, the in-grid tool precedent),
	 * 'V'/'W' (the two SWORD heads — no sword item in the port) = the {@code ingots/steel}
	 * tag (the raw blade material), 'O' = the steel ring (the GTMaterialItems bare-item
	 * form, the :1121 bronze-ring precedent), 'P' = {@code plates/steel} (the wrench-row
	 * key). NO battery slot — the reversal ruling (the :354 row's null battery column).
	 * The in-grid tools pay one point each and ride along (the container-item channel).
	 * Result 1x {@code gt6:pocket_multitool} — the seven switch forms are NOT crafted,
	 * they are the sneak-right-click ring (the :176-183 chain).
	 */
	private ShapedRecipeBuilder pocketMultitoolBuilder() {
		TagKey<Item> tSteelPlates = gregtech6.datagen.GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "steel");
		TagKey<Item> tSteelIngots = gregtech6.datagen.GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, "steel");
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.POCKET_MULTITOOL.get())
				.pattern("AXO")
				.pattern("ZPV")
				.pattern("OWY")
				.define('A', GT6ItemTags.TOOLS_SCREWDRIVER)
				.define('X', GT6ItemTags.TOOLS_SAW)
				.define('Z', GT6ItemTags.TOOLS_FILE)
				.define('Y', GT6Tools.CHISEL.get())
				.define('V', tSteelIngots)
				.define('W', tSteelIngots)
				.define('O', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.ring, gregapi.data.MT.Steel).get())
				.define('P', tSteelPlates)
				.unlockedBy("has_steel_plate", has(tSteelPlates));
	}

	/**
	 * The magnifying-glass self-craft row (task w5-t3-machine-face-four spec ③) — the
	 * upstream tagline "Crafted with a Stick and a Lens" (:148) over the
	 * AdvancedCraftingTool(MAGNIFYING_GLASS, lens, MT.Glass) row (Loader_Tools.java:332):
	 * 'L' = {@code OP.lens.dat(MT.Glass)} → the GTMaterialItems lens_glass item (the
	 * GT6RecipesShCL :240 lens×glass pair precedent — the lens family IS registered), 'S' =
	 * the vanilla stick. Result 1x {@code gt6:magnifying_glass}.
	 */
	private ShapedRecipeBuilder magnifyingGlassBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.MAGNIFYING_GLASS.get())
				.pattern("L")
				.pattern("S")
				.define('L', GTMaterialItems.get(gregapi.data.OP.lens, MT.Glass).get())
				.define('S', Items.STICK)
				.unlockedBy("has_lens", has(GTMaterialItems.get(gregapi.data.OP.lens, MT.Glass).get()));
	}

	/**
	 * The pincers self-craft row (task w5-t3-machine-face-four spec ④) — the upstream
	 * {"XhX"," T ","SdS"} row (Loader_Tools.java:316, plateCurved prefix) with the
	 * material scale {@code U*2 + screw + 2*stick} (the :150 registration amount):
	 * 'X' = {@code OP.plateCurved.dat(MT.Steel)} (the GTMaterialItems curved plate, the
	 * hopper builder precedent), 'h' = {@code #gt6:tools/hard_hammer}, 'd' =
	 * {@code OP.screw.dat(MT.Steel)}, 'S' = the vanilla stick. Result 1x
	 * {@code gt6:pincers}.
	 */
	private ShapedRecipeBuilder pincersBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.PINCERS.get())
				.pattern("XhX")
				.pattern(" d ")
				.pattern("S S")
				.define('X', GTMaterialItems.get(gregapi.data.OP.plateCurved, MT.Steel).get())
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.define('d', GTMaterialItems.get(gregapi.data.OP.screw, MT.Steel).get())
				.define('S', Items.STICK)
				.unlockedBy("has_steel_plate_curved", has(GTMaterialItems.get(gregapi.data.OP.plateCurved, MT.Steel).get()));
	}

	/**
	 * The empty-food-can crafting row (task food-can-row0 spec ③) — the upstream
	 * {"fh", "oP"} row VERBATIM (MultiItemRandomTools.java:239, CR.DEF_NCC): 'f' =
	 * {@code #gt6:tools/file} (the CR.java:200 craftingToolFile letter), 'h' =
	 * {@code #gt6:tools/hard_hammer} (:201), 'o' = {@code #gt6:tools/bending_cylinder_small}
	 * (:207 — the OreDictToolNames.bendingcylindersmall letter, the bending cylinder IS a
	 * crafting ingredient here exactly like upstream), 'P' = {@code OP.plateCurved.dat(
	 * MT.TinAlloy)} → the existing {@code #gt6:plate_curved_tin} material tag (the
	 * GTMaterialItems plate_curved_tin item, the spray-can row's 'C' key precedent).
	 * Result 1x {@code gt6:food_can_empty} — the canning machine's consumable input.
	 */
	/**
	 * The LARGE Steel Crucible crafting row (task crucible-multiblock SPEC ⑦) — the
	 * upstream "hMy" row (Loader_MultiTileEntities.java:1270, 'M' = the wall item 18009
	 * 对位) with the declared port deviation that the soldering-tool family is not ported
	 * yet: the row runs "hM" ('h' = the hard-hammer tool tag, the hammerFromIngotsBuilder
	 * key; 'M' = gt6:crucible_steel_wall). The in-grid hammer pays one durability point
	 * and rides along (the container-item channel). Result 1x the controller block item.
	 */
	/**
	 * The crucible ladder crafting rows (task w3-distill-crucible ③) — the
	 * largeSteelCrucibleBuilder "hM" form (the upstream "hMy" row with the soldering-tool
	 * family cut, the same declared deviation) over each of the seven ladder rungs:
	 * 'M' = the rung's own wall item, result = the rung controller.
	 */
	private java.util.List<CrucibleLadderRecipeRow> crucibleLadderRecipeBuilders() {
		java.util.List<CrucibleLadderRecipeRow> rRows = new java.util.ArrayList<>();
		for (gregtech6.registry.GT6Crucibles.CrucibleRow tRow : gregtech6.registry.GT6Crucibles.CRUCIBLE_ROWS) {
			if ("crucible_steel".equals(tRow.path())) continue; // the single-rung row above
			Item tWall = gregtech6.registry.GT6Crucibles.CRUCIBLE_WALL_ITEMS_BY_PATH.get(tRow.wallPath()).get();
			Item tController = gregtech6.registry.GT6Crucibles.CRUCIBLE_ITEMS_BY_PATH.get(tRow.path()).get();
			String tIdPath = "crucible_ladder/" + tRow.path(); // the precomputed arg — the stonecutter ctor swap rewrites simple-arg calls only
			rRows.add(new CrucibleLadderRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, tController)
					.pattern("hM")
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
					.define('M', tWall)
					.unlockedBy("has_crucible_wall", has(tWall)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tIdPath)));
		}
		return rRows;
	}

	/**
	 * The crucible WALL crafting rows (task crucible-wall-obtainability) — the upstream
	 * wall rows {@code "wPP","hPP"} (Loader_MultiTileEntities.java:1143-1153, 'w' = wrench,
	 * 'h' = hard hammer per CR.java:344-358, 'P' = {@code OP.plate.dat(aMat)}) replayed over
	 * the EIGHT DEDICATED crucible-wall blocks (the port-side twins of the metalwall items
	 * the :1270-1277 controllers reference — the GTCrucibleWallBlock deviation means the
	 * shared machine-wall rows do not cover them). FOUR plates of the tier material + the
	 * two in-grid tools produce ONE wall block; a tier whose plate item is unregistered
	 * skips its row (the CR.ONLY_IF_HAS_RESULT face — all eight resolve today).
	 */
	private java.util.List<PartFamilyRecipeRow> crucibleWallRecipeBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new java.util.ArrayList<>();
		for (gregtech6.registry.GT6Crucibles.CrucibleRow tRow : gregtech6.registry.GT6Crucibles.CRUCIBLE_ROWS) {
			Item tWall = crucibleWallItem(tRow.wallPath());
			gregapi.oredict.OreDictMaterial tMat = tRow.material();
			if (tWall == null || tMat == null) continue; // the unregistered silent skip
			Item tPlate = itemOrNull(gregapi.data.OP.plate, tMat);
			if (tPlate == null) continue; // the CR.ONLY_IF_HAS_RESULT face
			String tIdPath = "crucible_wall/" + tRow.wallPath(); // the precomputed arg — the stonecutter ctor swap rewrites simple-arg calls only
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tWall)
					.pattern("wPP")
					.pattern("hPP")
					.define('w', GT6ItemTags.TOOLS_WRENCH)
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
					.define('P', tPlate)
					.unlockedBy("has_plate", has(tPlate)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tIdPath)));
		}
		return rRows;
	}

	/**
	 * The shared machine-WALL crafting rows (task recipes-obtainability) — the SAME
	 * {@code "wPP","hPP"} four-plate grid as the dedicated crucible band above, replayed
	 * over the ELEVEN {@link gregtech6.registry.GTMultiBlocks#METAL_WALL_ROWS} blocks
	 * (Loader_MultiTileEntities.java:1143-1153, {@code 'P' = OP.plate.dat(aMat)}). The
	 * WELDER face of the same rows already pours in {@code GT6RecipesWelder} — this band
	 * closes the crafting half, the crucible-wall card's sister gap. The row material
	 * resolves through {@link gregtech6.recipes.GT6RecipesWelder#materialNameOf} (the
	 * registry-key switch the welder rows ride); a wall whose plate item is unregistered
	 * skips its row (the CR.ONLY_IF_HAS_RESULT face — all eleven resolve today).
	 */
	private java.util.List<PartFamilyRecipeRow> machineWallRecipeBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new java.util.ArrayList<>();
		for (gregtech6.registry.GTMultiBlocks.PartRow tRow : gregtech6.registry.GTMultiBlocks.METAL_WALL_ROWS) {
			gregtech6.block.multiblock.GTMultiBlockPartBlock tBlock = gregtech6.registry.GTMultiBlocks.anyPartBlock(tRow.path());
			gregapi.oredict.OreDictMaterial tMat = gregapi.oredict.OreDictMaterial.get(gregtech6.recipes.GT6RecipesWelder.materialNameOf(tRow.path()));
			if (tBlock == null || tMat == null) continue; // the unregistered silent skip
			Item tPlate = itemOrNull(gregapi.data.OP.plate, tMat);
			if (tPlate == null) continue; // the CR.ONLY_IF_HAS_RESULT face (SteelGalvanized)
			String tIdPath = "machine_wall/" + tRow.path(); // the precomputed arg — the stonecutter ctor swap rewrites simple-arg calls only
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tBlock.asItem())
					.pattern("wPP")
					.pattern("hPP")
					.define('w', GT6ItemTags.TOOLS_WRENCH)
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
					.define('P', tPlate)
					.unlockedBy("has_plate", has(tPlate)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tIdPath)));
		}
		return rRows;
	}

	/**
	 * The slicer-blade crafting rows (task recipes-obtainability, coordinator ruling B
	 * — the obtainability domain = items + recipes inseparable) — the upstream EIGHT rows
	 * VERBATIM (MultiItemTechnological.java:364 the frame + :374-380 the seven blade
	 * forms): the frame row {" R ","RhR"," R "} over StainlessSteel sticks + the hard
	 * hammer, then per blade {@code 'O' = the Shape_Slicer_Empty frame} + StainlessSteel
	 * plateTiny {@code 'B'} + the file {@code 'f'}/saw {@code 's'} tool keys (CR.java:344/
	 * :356 — 's' is the SAW; 'd' would be the screwdriver) with the StainlessSteel ring
	 * {@code 'R'} on the two hollow forms. Ids ride the result-path convention; a missing
	 * StainlessSteel item (stick/plateTiny/ring) skips the band (the CR.ONLY_IF_HAS_RESULT
	 * face — all three resolve today).
	 */
	private java.util.List<PartFamilyRecipeRow> slicerBladeRecipeBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new java.util.ArrayList<>();
		Item tStick = itemOrNull(gregapi.data.OP.stick, MT.StainlessSteel);
		Item tPlateTiny = itemOrNull(gregapi.data.OP.plateTiny, MT.StainlessSteel);
		Item tRing = itemOrNull(gregapi.data.OP.ring, MT.StainlessSteel);
		if (tStick == null || tPlateTiny == null || tRing == null) return rRows; // the CR.ONLY_IF_HAS_RESULT face
		Item tFrame = gregtech6.registry.GT6SlicerBlades.SHAPE_SLICER_EMPTY.get();
		// the frame row :364 — " R ","RhR"," R "
		rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tFrame)
				.pattern(" R ")
				.pattern("RhR")
				.pattern(" R ")
				.define('R', tStick)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_stick", has(tStick)),
				new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_slicer_empty")));
		// the seven blade rows :374-380 — 'O' frame + 'B' plateTiny + 'f' file + 's' saw
		rRows.add(slicerBladeRow("shape_slicer_flat",              gregtech6.registry.GT6SlicerBlades.SHAPE_SLICER_FLAT.get(),           "B f", "BO ", "B s", tFrame, tPlateTiny, null));
		rRows.add(slicerBladeRow("shape_slicer_grid",              gregtech6.registry.GT6SlicerBlades.SHAPE_SLICER_GRID.get(),           " Bf", "BOB", " Bs", tFrame, tPlateTiny, null));
		rRows.add(slicerBladeRow("shape_slicer_eigths",            gregtech6.registry.GT6SlicerBlades.SHAPE_SLICER_EIGHTS.get(),         "B B", "s f", "BOB", tFrame, tPlateTiny, null));
		rRows.add(slicerBladeRow("shape_slicer_eigths_hollow",     gregtech6.registry.GT6SlicerBlades.SHAPE_SLICER_EIGHTS_HOLLOW.get(),  "B B", "sRf", "BOB", tFrame, tPlateTiny, tRing));
		rRows.add(slicerBladeRow("shape_slicer_split",             gregtech6.registry.GT6SlicerBlades.SHAPE_SLICER_SPLIT.get(),          " Of", "BBB", "  s", tFrame, tPlateTiny, null));
		rRows.add(slicerBladeRow("shape_slicer_quarters",          gregtech6.registry.GT6SlicerBlades.SHAPE_SLICER_QUARTERS.get(),       "fB ", "B s", " O ", tFrame, tPlateTiny, null));
		rRows.add(slicerBladeRow("shape_slicer_quarters_hollow",   gregtech6.registry.GT6SlicerBlades.SHAPE_SLICER_QUARTERS_HOLLOW.get(),"fB ", "BRs", " O ", tFrame, tPlateTiny, tRing));
		return rRows;
	}

	/** One slicer blade row body — the shared 'O'/'B'/'f'/'s' defines + the hollow ring (null = the plain forms). */
	private PartFamilyRecipeRow slicerBladeRow(String aPath, Item aBlade, String aL1, String aL2, String aL3,
			Item aFrame, Item aPlateTiny, Item aRing) {
		ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aBlade)
				.pattern(aL1)
				.pattern(aL2)
				.pattern(aL3)
				.define('O', aFrame)
				.define('B', aPlateTiny)
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('s', GT6ItemTags.TOOLS_SAW)
				.unlockedBy("has_frame", has(aFrame));
		if (aRing != null) tBuilder.define('R', aRing);
		return new PartFamilyRecipeRow(tBuilder, new ResourceLocation(GT6DataGenerators.MOD_ID, aPath));
	}

	/** The crucible wall item of a wall path (the {@link gregtech6.registry.GT6Crucibles#wallBlockOf} twin — the steel rung rides the single-rung handle). */
	private static Item crucibleWallItem(String aWallPath) {
		var tHandle = gregtech6.registry.GT6Crucibles.CRUCIBLE_WALL_ITEMS_BY_PATH.get(aWallPath);
		if (tHandle == null && "crucible_steel_wall".equals(aWallPath)) return gregtech6.registry.GT6Crucibles.CRUCIBLE_STEEL_WALL_ITEM.get();
		return tHandle == null ? null : tHandle.get();
	}

	/** One staged crucible-ladder row: the shared builder + the id its save face ids from. */
	private record CrucibleLadderRecipeRow(ShapedRecipeBuilder builder, ResourceLocation id) {}

	private ShapedRecipeBuilder largeSteelCrucibleBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,
				gregtech6.registry.GT6Crucibles.CRUCIBLE_ITEMS_BY_PATH.get("crucible_steel").get())
				.pattern("hM")
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.define('M', gregtech6.registry.GT6Crucibles.CRUCIBLE_STEEL_WALL_ITEM.get())
				.unlockedBy("has_crucible_wall", has(gregtech6.registry.GT6Crucibles.CRUCIBLE_STEEL_WALL_ITEM.get()));
	}

	/** One staged diesel-engine row: the shared builder + the id its save face ids from. */
	private record DieselEngineRecipeRow(ShapedRecipeBuilder builder, ResourceLocation id) {}

	/**
	 * The Diesel Engine crafting rows (task w4-hot-lube spec ④) — the Loader
	 * MultiTileEntities.java:722-729 grids VERBATIM: "PLP"/"SMS"/"GPC" per material with
	 * 'M' = {@code OP.casingMachineDouble.dat(aMat)} folded to casingSmall (the prefix has
	 * no port item row — the Locker 'M' fold precedent), 'P' = plateCurved, 'S' = stick,
	 * 'G' = gearGt, 'C' = gearGtSmall (all per-material through GTMaterialItems), and 'L' =
	 * {@code OD.itemLubricant} → the port's single-item carrier
	 * {@code gt6:lubricant_bucket} (the declared crafting-only face, GT6LubricantBucket
	 * class doc). One row per DIESEL_SPECS material, result = the engine block item; ids
	 * ride the result path ("diesel_engine_&lt;mat&gt;", the hopper result-path convention).
	 */
	private java.util.List<DieselEngineRecipeRow> dieselEngineRecipeBuilders() {
		java.util.List<DieselEngineRecipeRow> rRows = new java.util.ArrayList<>();
		for (gregtech6.registry.GT6Kinetics.DieselSpec tSpec : gregtech6.registry.GT6Kinetics.DIESEL_SPECS) {
			gregapi.oredict.OreDictMaterial tMat = dieselMaterial(tSpec.material());
			Item tEngine = gregtech6.registry.GT6Kinetics.DIESEL_ITEMS.get(gregtech6.registry.GT6Kinetics.dieselName(tSpec.material())).get();
			String tIdPath = gregtech6.registry.GT6Kinetics.dieselName(tSpec.material()); // the precomputed arg — the stonecutter ctor swap rewrites simple-arg calls only
			rRows.add(new DieselEngineRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, tEngine)
					.pattern("PLP")
					.pattern("SMS")
					.pattern("GPC")
					.define('M', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.casingSmall, tMat).get())
					.define('P', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateCurved, tMat).get())
					.define('S', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.stick, tMat).get())
					.define('G', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.gearGt, tMat).get())
					.define('C', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.gearGtSmall, tMat).get())
					.define('L', gregtech6.item.GT6LubricantBucket.LUBRICANT_BUCKET.get())
					.unlockedBy("has_lubricant_bucket", has(gregtech6.item.GT6LubricantBucket.LUBRICANT_BUCKET.get())),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tIdPath)));
		}
		return rRows;
	}

	/** The DIESEL_SPECS slug → the loader material (the GT6Hoppers.HopperMaterial.mt() switch shape). */
	private static gregapi.oredict.OreDictMaterial dieselMaterial(String aSlug) {
		return switch (aSlug) {
			case "bronze" -> MT.Bronze;
			case "arsenic_copper" -> MT.ArsenicCopper;
			case "arsenic_bronze" -> MT.ArsenicBronze;
			case "steel" -> MT.Steel;
			case "invar" -> MT.Invar;
			case "titanium" -> MT.Ti;
			case "tungstensteel" -> MT.TungstenSteel;
			case "iridium" -> MT.Ir;
			default -> throw new IllegalStateException("no loader material for diesel slug " + aSlug);
		};
	}

	// -------------------------------------------------------------------------
	// task cracker-machines — the two Cracker crafting families (Loader
	// MultiTileEntities.java:1570-1579 grids VERBATIM, the 'IwI','PMP','ICI'
	// SteamCracker / 'IPI','ZMZ','ICI' CatalyticCracker three rows per tier):
	//   'I' = plateDouble/plateTriple/plateQuadruple/plateQuintuple Invar (per
	//   tier, the upstream plateD/T/Q/Quint column) — rides the
	//   gt6material-items face directly (multi-plates carry items, only the
	//   plateDouble tag family exists, the transformer 'I' column precedent);
	//   'C' = the same plate ladder over ANY.Cu → Copper (the upstream ANY.Cu
	//   lowest-price fold, the ANY-material single-representative precedent);
	//   'Z' = OP.dust Zeolite → gt6:dust_zeolite (the port item exists);
	//   'w' = the wrench (CR.java tool letter, the #gt6:tools/wrench tag);
	//   'M' = casingMachineDouble.dat(aMat) → casingSmall (the transformer fold —
	//   no casingMachineDouble item row in the port);
	//   'P' = pipeQuadruple/pipeMedium.dat(aMat) → gt6:wood_fluid_pipe_medium
	//   (the DECLARED FOLD: the pipe prefixes are off the port item path — the
	//   distill_part/sluice_part 'P' CUT precedent — but this family keeps ONE
	//   representative pipe item instead of cutting the row; pipeQuadruple
	//   semantic = 4-pipe stack, the borrowed single item carries count 1 per
	//   cell and the pipeMedium steam rung is exact — the quadruple rung's
	//   quantity differential is NOT expressible without a count override, kept
	//   1:1 with the upstream per-cell count of 1).
	// One row per tier (T1-T4, the Heat_T ladder), result = the machine item,
	// ids ride the block path (the diesel result-path convention).
	// -------------------------------------------------------------------------

	/** One staged cracker row: the shared builder + the id its save face ids from (the DieselEngineRecipeRow shape). */
	private record CrackerRecipeRow(ShapedRecipeBuilder builder, ResourceLocation id) {}

	/** The tier plate ladder: T1 double, T2 triple, T3 quadruple, T4 quintuple (the upstream plateD/T/Q/Quint column). */
	private static gregapi.oredict.OreDictPrefix crackerPlate(int aTier) {
		return switch (aTier) {
			case 0 -> gregapi.data.OP.plateDouble;
			case 1 -> gregapi.data.OP.plateTriple;
			case 2 -> gregapi.data.OP.plateQuadruple;
			default -> gregapi.data.OP.plateQuintuple;
		};
	}

	private java.util.List<CrackerRecipeRow> crackerRecipeBuilders() {
		gregapi.oredict.OreDictMaterial[] tMats = {MT.Steel, MT.Invar, MT.Ti, MT.TungstenSteel};
		String[] tPaths = {"steamcracker", "steamcracker_t2", "steamcracker_t3", "steamcracker_t4"};
		String[] tCatPaths = {"catalyticcracker", "catalyticcracker_t2", "catalyticcracker_t3", "catalyticcracker_t4"};
		java.util.List<CrackerRecipeRow> rRows = new java.util.ArrayList<>();
		for (int i = 0; i < 4; i++) {
			gregapi.oredict.OreDictPrefix tPlate = crackerPlate(i);
			gregapi.oredict.OreDictMaterial tMat = tMats[i];
			// the Steam Cracker :1576-1579 — "IwI","PMP","ICI"
			rRows.add(new CrackerRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,
					gregtech6.registry.GTMachines.STEAM_CRACKER_ITEMS_BY_PATH.get(tPaths[i]).get())
					.pattern("IwI").pattern("PMP").pattern("ICI")
					.define('I', gregtech6.registry.GTMaterialItems.get(tPlate, MT.Invar).get())
					.define('w', GT6ItemTags.TOOLS_WRENCH)
					.define('P', gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM.get())
					.define('M', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.casingSmall, tMat).get())
					.define('C', gregtech6.registry.GTMaterialItems.get(tPlate, MT.Copper).get())
					.unlockedBy("has_invar_plate", has(gregtech6.registry.GTMaterialItems.get(tPlate, MT.Invar).get())),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tPaths[i])));
			// the Catalytic Cracker :1570-1573 — "IPI","ZMZ","ICI"
			rRows.add(new CrackerRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,
					gregtech6.registry.GTMachines.CATALYTIC_CRACKER_ITEMS_BY_PATH.get(tCatPaths[i]).get())
					.pattern("IPI").pattern("ZMZ").pattern("ICI")
					.define('I', gregtech6.registry.GTMaterialItems.get(tPlate, MT.Invar).get())
					.define('P', gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM.get())
					.define('Z', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.dust, gregapi.data.MT.OREMATS.Zeolite).get())
					.define('M', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.casingSmall, tMat).get())
					.define('C', gregtech6.registry.GTMaterialItems.get(tPlate, MT.Copper).get())
					.unlockedBy("has_invar_plate", has(gregtech6.registry.GTMaterialItems.get(tPlate, MT.Invar).get())),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tCatPaths[i])));
		}
		return rRows;
	}

	// -------------------------------------------------------------------------
	// task machines-burner-plantalyzer — the Burner Mixer crafting family
	// (Loader MultiTileEntities.java:1595-1598 grids VERBATIM, "PMP","PRP","hSw"
	// four rows over the Kinetic_T ladder):
	//   'M' = casingMachine.dat(aMat) → casingSmall (the cracker/transformer fold —
	//   the prefix has no port item row);
	//   'S' = stick.dat(aMat) — the port stick row exists (the locker precedent);
	//   'R' = rotor.dat(MT.Invar);
	//   'P' = plate/plateDouble/plateTriple/plateQuadruple.dat(MT.Invar) — the tier
	//   ladder (single plate on T1, multi-plates carry items, the cracker 'I' column
	//   precedent);
	//   'h' = the hard hammer (GT6ItemTags.TOOLS_HARD_HAMMER, the food-can 'h' letter);
	//   'w' = the wrench (GT6ItemTags.TOOLS_WRENCH, the cracker 'w' letter).
	// One row per tier (T1-T4), result = the machine item, ids ride the block path
	// (the diesel result-path convention). The Plantalyzer "WXW","ZMP","CYC" rows are
	// CUT — the absent-component ruling (the GTMachines family note: the CABLES_01/
	// EMITTERS/SENSORS columns and IL.Processor_Crystal_Diamond are absent port
	// identities, the molecular-scanner ruling). Task usb-peripherals, coordinator
	// ruling Q1=(b) SUPERSEDED by task debt-scanner-t3-usb-stick: the molecular-scanner
	// T3 controller row (:1551 "DXE","FMF","RYS") is RESTORED (the molecularScannerRow
	// band below) — the component pool card (debt-emitter-sensor-generators) landed the
	// F/X/Y columns (field_generator_hv/signal_emitter_hv/sensor_hv), so the seam's
	// await condition no longer holds.
	// -------------------------------------------------------------------------

	private java.util.List<CrackerRecipeRow> burnerMixerRecipeBuilders() {
		gregapi.oredict.OreDictMaterial[] tMats = {MT.Bronze, MT.Steel, MT.Ti, MT.TungstenSteel};
		String[] tPaths = {"burner_mixer", "burner_mixer_t2", "burner_mixer_t3", "burner_mixer_t4"};
		gregapi.oredict.OreDictPrefix[] tPlates = {gregapi.data.OP.plate, gregapi.data.OP.plateDouble, gregapi.data.OP.plateTriple, gregapi.data.OP.plateQuadruple};
		java.util.List<CrackerRecipeRow> rRows = new java.util.ArrayList<>();
		for (int i = 0; i < 4; i++) {
			gregapi.oredict.OreDictMaterial tMat = tMats[i];
			// the Burner Mixer :1595-1598 — "PMP","PRP","hSw"
			rRows.add(new CrackerRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,
					gregtech6.registry.GTMachines.BURNER_MIXER_ITEMS_BY_PATH.get(tPaths[i]).get())
					.pattern("PMP").pattern("PRP").pattern("hSw")
					.define('P', gregtech6.registry.GTMaterialItems.get(tPlates[i], MT.Invar).get())
					.define('M', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.casingSmall, tMat).get())
					.define('R', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.rotor, MT.Invar).get())
					.define('S', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.stick, tMat).get())
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
					.define('w', GT6ItemTags.TOOLS_WRENCH)
					.unlockedBy("has_invar_plate", has(gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plate, MT.Invar).get())),
						new ResourceLocation(GT6DataGenerators.MOD_ID, tPaths[i])));
		}
		return rRows;
	}

	// -------------------------------------------------------------------------
	// task debt-scanner-t3-usb-stick — the Molecular Scanner T3 controller row,
	// Loader_MultiTileEntities.java:1551 VERBATIM (the ONLY active scanner rung —
	// T1/T2/T4/T5 :1549-1550/:1552-1553 are commented out upstream; "DXE","FMF","RYS"
	// over MT.Osmiridium). Column map:
	//   - 'M' = casingMachine.dat(Osmiridium) → casingSmall (the cracker/transformer
	//     fold — the prefix has no port item row);
	//   - 'D'/'E'/'R'/'S' = IL.Processor_Crystal_Diamond/Emerald/Ruby/Sapphire → the
	//     matching GEM TAGS (the circuits-c gem-tag fold, the crystal circuits'
	//     material carriers);
	//   - 'F'/'X'/'Y' = IL.FIELD_GENERATORS[3]/IL.EMITTERS[3]/IL.SENSORS[3] → the HV
	//     rungs of the three component families (VN[3] = HV, the
	//     debt-emitter-sensor-generators items) — the F/X/Y columns the Q1=(b) seam
	//     was awaiting; the restore closes it.
	// Result = the machine item, id rides the block path (the machine result-path
	// convention).
	// -------------------------------------------------------------------------

	/** One Molecular Scanner T3 row: the shared builder + the id its save face ids from (the CrackerRecipeRow shape). */
	private CrackerRecipeRow molecularScannerRow() {
		Item tFieldGen = gregtech6.items.GT6Emitters.ITEMS_BY_PATH.get(gregtech6.items.GT6Emitters.FAMILY_FIELD_GENERATORS + "_hv").get();
		Item tEmitter = gregtech6.items.GT6Emitters.ITEMS_BY_PATH.get(gregtech6.items.GT6Emitters.FAMILY_EMITTERS + "_hv").get();
		Item tSensor = gregtech6.items.GT6Emitters.ITEMS_BY_PATH.get(gregtech6.items.GT6Emitters.FAMILY_SENSORS + "_hv").get();
		Item tCasing = GTMaterialItems.get(gregapi.data.OP.casingSmall, gregapi.data.MT.Osmiridium).get();
		return new CrackerRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,
				gregtech6.registry.GTMachines.MOLECULAR_SCANNER_ITEMS_BY_PATH.get("molecular_scanner_t3").get())
				.pattern("DXE").pattern("FMF").pattern("RYS")
				.define('D', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "diamond"))
				.define('E', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "emerald"))
				.define('R', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "ruby"))
				.define('S', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "sapphire"))
				.define('F', tFieldGen)
				.define('M', tCasing)
				.define('X', tEmitter)
				.define('Y', tSensor)
				.unlockedBy("has_field_generator", has(tFieldGen)),
				new ResourceLocation(GT6DataGenerators.MOD_ID, "molecular_scanner_t3"));
	}

	private ShapedRecipeBuilder foodCanEmptyBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6FoodCans.FOOD_CAN_EMPTY.get())
				.pattern("fh")
				.pattern("oP")
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.define('o', GT6ItemTags.TOOLS_BENDING_CYLINDER_SMALL)
				.define('P', GT6ItemTags.PLATE_CURVED_TIN)
				.unlockedBy("has_plate_curved_tin", has(GT6ItemTags.PLATE_CURVED_TIN));
	}

	// -------------------------------------------------------------------------
	// task usb-peripherals — the USB peripheral crafting rows, upstream
	// MultiItemTechnological.java:808-811 (the four USB Cable rows, "xWd","PCP","TCT")
	// and :819-822 (the four USB HDD rows, "PLT","dRW","TCP"). Column map (VERBATIM):
	//   - 'x' = the wirecutter (CR.java:359), 'd' = the screwdriver (CR.java:342) —
	//     the battery row's tool-letter convention;
	//   - 'W' = MT.DATA.WIRES_01[3..6] = wireGt01 Au/Al/Pt/Graphene (MT.java:3595-3610);
	//   - 'C' (cable rows) = MT.DATA.CABLES_01[3..6] — WITH the upstream quirk kept:
	//     CABLES_01[6] carries wireGt01.dat(Graphene) (MT.java:3646), the graphene rung
	//     has NO cable form, so T4's 'C' is the SAME wire item as its 'W';
	//   - 'C' (HDD rows) = OD_CIRCUITS[3..6] → the #gt6:circuit3..6 TAGS (the battery
	//     'X' column convention, NOT a deviation);
	//   - 'W' (HDD rows) = IL.USB_Cable_1..4 — the items this card registers;
	//   - 'L' = IL.Comp_Laser_Gas_He — the coordinator's single-item exemption
	//     (GT6LaserGas.COMP_LASER_GAS_HE, the consumption chain this row IS);
	//   - 'R' = OD.record → #minecraft:music_discs (the vanilla tag is the "record"
	//     oredict's modern face — the coordinator-approved mapping);
	//   - 'P'/'T' = OP.plate/OP.screw dat(Al/StainlessSteel/Cr/Ti) — the same tier
	//     ladder as the USB Stick rows (:796-799).
	// The HDD_1 row carries upstream's own TODO verbatim (the :819 tail comment
	// "Replace record with a CD (made of aluminium foils and plastic plates in a Press)")
	// — upstream reality, unimplemented here by the card's explicit 不做 clause.
	// -------------------------------------------------------------------------

	/** The 'W' column ladder: MT.DATA.WIRES_01[3..6] = wireGt01 Au/Al/Pt/Graphene. */
	private static final String[] USB_WIRE_COL_PATHS = {"wire_gold_gt01", "wire_aluminium_gt01", "wire_platinum_gt01", "wire_graphene_gt01"};
	/** The 'C' column ladder: MT.DATA.CABLES_01[3..6] — [6] is the wireGt01(Graphene) quirk (MT.java:3646). */
	private static final String[] USB_CABLE_COL_PATHS = {"cable_gold_gt01", "cable_aluminium_gt01", "cable_platinum_gt01", "wire_graphene_gt01"};
	/** The 'P'/'T' material ladder: the :808-811/:819-822 Al/StainlessSteel/Cr/Ti column. */
	private static final gregapi.oredict.OreDictMaterial[] USB_PLATE_MATS = {MT.Al, MT.StainlessSteel, MT.Cr, MT.Ti};

	/** The USB Cable rows (:808-811, "xWd","PCP","TCT") — one per tier, ids on the item path. */
	private java.util.List<BridgeCraftRow> usbCableRecipeRows() {
		java.util.List<BridgeCraftRow> rRows = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Item tWire = wireItemByPath(USB_WIRE_COL_PATHS[i]);
			Item tCable = wireItemByPath(USB_CABLE_COL_PATHS[i]);
			Item tPlate = GTMaterialItems.get(gregapi.data.OP.plate, USB_PLATE_MATS[i]).get();
			Item tScrew = GTMaterialItems.get(gregapi.data.OP.screw, USB_PLATE_MATS[i]).get();
			String tPath = "usb_cable_" + (i + 1);
			rRows.add(new BridgeCraftRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, cableItem(i + 1))
					.pattern("xWd").pattern("PCP").pattern("TCT")
					.define('x', GT6ItemTags.TOOLS_WIRE_CUTTER)
					.define('W', tWire)
					.define('P', tPlate)
					.define('C', tCable)
					.define('T', tScrew)
					.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
					.unlockedBy("has_data_wire", has(tWire)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tPath)));
		}
		return rRows;
	}

	/** The USB HDD rows (:819-822, "PLT","dRW","TCP") — one per tier, ids on the item path. */
	private java.util.List<BridgeCraftRow> usbDriveRecipeRows() {
		java.util.List<BridgeCraftRow> rRows = new ArrayList<>();
		// OD.record → #minecraft:music_discs: the vanilla tag IS the "record" oredict's
		// modern face; bound through TagKey.create because the 1.21.1 leg dropped the
		// ItemTags.MUSIC_DISCS constant (the tag itself survives in vanilla data on both legs).
		String tDiscPath = "music_discs";
		TagKey<Item> tRecords = TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation("minecraft", tDiscPath));
		for (int i = 0; i < 4; i++) {
			Item tPlate = GTMaterialItems.get(gregapi.data.OP.plate, USB_PLATE_MATS[i]).get();
			Item tScrew = GTMaterialItems.get(gregapi.data.OP.screw, USB_PLATE_MATS[i]).get();
			TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit" + (i + 3)); // OD_CIRCUITS[3..6]
			String tPath = "usb_drive_" + (i + 1);
			rRows.add(new BridgeCraftRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, driveItem(i + 1))
					.pattern("PLT").pattern("dRW").pattern("TCP")
					.define('P', tPlate)
					.define('L', gregtech6.items.GT6LaserGas.COMP_LASER_GAS_HE.get()) // the exemption item
					.define('T', tScrew)
					.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
					.define('R', tRecords)
					.define('W', cableItem(i + 1))
					.define('C', tCircuit)
					.unlockedBy("has_circuit", has(tCircuit)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tPath)));
		}
		return rRows;
	}

	/** The USB Cable item of a tier (1-4). */
	private static Item cableItem(int aTier) {
		return switch (aTier) {
			case 1 -> gregtech6.items.GT6UsbSticks.USB_CABLE_1.get();
			case 2 -> gregtech6.items.GT6UsbSticks.USB_CABLE_2.get();
			case 3 -> gregtech6.items.GT6UsbSticks.USB_CABLE_3.get();
			default -> gregtech6.items.GT6UsbSticks.USB_CABLE_4.get();
		};
	}

	/** The USB HDD item of a tier (1-4). */
	private static Item driveItem(int aTier) {
		return switch (aTier) {
			case 1 -> gregtech6.items.GT6UsbSticks.USB_DRIVE_1.get();
			case 2 -> gregtech6.items.GT6UsbSticks.USB_DRIVE_2.get();
			case 3 -> gregtech6.items.GT6UsbSticks.USB_DRIVE_3.get();
			default -> gregtech6.items.GT6UsbSticks.USB_DRIVE_4.get();
		};
	}

	// -------------------------------------------------------------------------
	// task debt-scanner-t3-usb-stick — the USB Stick crafting rows, upstream
	// MultiItemTechnological.java:796-799 VERBATIM ("xWd","PCP","TCT" — the same grid
	// as the cable/HDD rows above). Column map (VERBATIM):
	//   - 'C' = OD_CIRCUITS[3..6] → the #gt6:circuit3..6 TAGS (the HDD 'C' column
	//     convention, NOT a deviation);
	//   - 'W' = MT.DATA.WIRES_01[3..6] = wireGt01 Au/Al/Pt/Graphene (MT.java:3595-3610)
	//     — the shared USB_WIRE_COL_PATHS ladder;
	//   - 'P'/'T' = OP.plate/OP.screw dat(Al/StainlessSteel/Cr/Ti) — the shared
	//     USB_PLATE_MATS ladder (the tier column the peripheral rows already ride);
	//   - 'x' = the wirecutter (CR.java:359), 'd' = the screwdriver (CR.java:342).
	// The usb-peripherals Q3 pool ruling closes here: the sticks carry the
	// Behavior_DataStorage data plane since usb-data, the rows complete their
	// obtainability chain.
	// -------------------------------------------------------------------------

	/** The USB Stick rows (:796-799, "xWd","PCP","TCT") — one per tier, ids on the item path. */
	private java.util.List<BridgeCraftRow> usbStickRecipeRows() {
		java.util.List<BridgeCraftRow> rRows = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Item tWire = wireItemByPath(USB_WIRE_COL_PATHS[i]);
			Item tPlate = GTMaterialItems.get(gregapi.data.OP.plate, USB_PLATE_MATS[i]).get();
			Item tScrew = GTMaterialItems.get(gregapi.data.OP.screw, USB_PLATE_MATS[i]).get();
			TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit" + (i + 3)); // OD_CIRCUITS[3..6]
			String tPath = "usb_stick_" + (i + 1);
			rRows.add(new BridgeCraftRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, stickItem(i + 1))
					.pattern("xWd").pattern("PCP").pattern("TCT")
					.define('x', GT6ItemTags.TOOLS_WIRE_CUTTER)
					.define('W', tWire)
					.define('P', tPlate)
					.define('C', tCircuit)
					.define('T', tScrew)
					.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
					.unlockedBy("has_circuit", has(tCircuit)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tPath)));
		}
		return rRows;
	}

	/** The USB Stick item of a tier (1-4). */
	private static Item stickItem(int aTier) {
		return switch (aTier) {
			case 1 -> gregtech6.items.GT6UsbSticks.USB_STICK_1.get();
			case 2 -> gregtech6.items.GT6UsbSticks.USB_STICK_2.get();
			case 3 -> gregtech6.items.GT6UsbSticks.USB_STICK_3.get();
			default -> gregtech6.items.GT6UsbSticks.USB_STICK_4.get();
		};
	}

	/**
	 * The plate-mold crafting row (task w1-press-extruder-molds) — the upstream
	 * {@code "x  ", " P ", "   "} stroke VERBATIM (MultiItemTechnological.java:247, the
	 * plate-identity file position): 'x' = {@code #gt6:tools/file} (the CR.java:200
	 * craftingToolFile letter), 'P' = {@link GT6ItemTags#EXTRUDER_SHAPE_BASE} (the declared
	 * row0 flattening — upstream chains Plate←Foil+file through the pooled Empty/Foil
	 * intermediates; the port keys both row0 molds on the chain root's tungsten-carbide
	 * plate face, one file stroke per mold identity). Result 1x plate mold — the RM.Extruder
	 * plate row's shaping tool (RM.java:405).
	 */
	private ShapedRecipeBuilder shapeExtruderPlateBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE.get())
				.pattern("x  ")
				.pattern(" P ")
				.pattern("   ")
				.define('x', GT6ItemTags.TOOLS_FILE)
				.define('P', GT6ItemTags.EXTRUDER_SHAPE_BASE)
				.unlockedBy("has_shape_base", has(GT6ItemTags.EXTRUDER_SHAPE_BASE));
	}

	/**
	 * The rod-mold crafting row (task w1-press-extruder-molds) — the upstream
	 * {@code "   ", " Px", "   "} stroke VERBATIM (MultiItemTechnological.java:221, the
	 * rod-identity file position; the flattened base ingredient per the plate-mold doc).
	 * Result 1x rod mold — the RM.Extruder rod row's shaping tool (RM.java:407).
	 */
	private ShapedRecipeBuilder shapeExtruderRodBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6ExtruderMolds.SHAPE_EXTRUDER_ROD.get())
				.pattern("   ")
				.pattern(" Px")
				.pattern("   ")
				.define('x', GT6ItemTags.TOOLS_FILE)
				.define('P', GT6ItemTags.EXTRUDER_SHAPE_BASE)
				.unlockedBy("has_shape_base", has(GT6ItemTags.EXTRUDER_SHAPE_BASE));
	}

	// -------------------------------------------------------------------------
	// the kitchen band (task kitchen-pot-bowl)
	// -------------------------------------------------------------------------

	/**
	 * The steel Bathing Pot crafting row — the upstream registration-line pattern VERBATIM
	 * (Loader_MultiTileEntities.java:2175 {@code " f ", "PhP", "PPP"}): 'P' =
	 * {@code OP.plate.dat(MT.StainlessSteel)} → the plate material tag
	 * ({@code #gt6:plates/stainless_steel}, the wrench row's steel-plates precedent),
	 * 'f' = {@code #gt6:tools/file}, 'h' = {@code #gt6:tools/hard_hammer}. Result 1x
	 * {@code gt6:bathing_pot_steel} (the 8000 L RM.Bath carrier).
	 *
	 * <p>DECLARED DORMANT sibling: the WOODEN pot row :2173 ({@code "sGh","PLP","PPP"}) —
	 * its 'G' key is {@code OD.itemGlue}, an oredict SOFT key upstream satisfied by foreign
	 * glue items (GT6 registers no glue item — Loader_OreDictionary has no itemGlue bind),
	 * so the port universe has no resolvable carrier for the key. Pooled with the
	 * wood-chemistry face, not dropped.
	 */
	/**
	 * The two stone anvil crafting rows (task c-anvil) — the upstream registration
	 * pattern VERBATIM (Loader_MultiTileEntities.java:2185-2186 {@code "RRR","hR ","RRR"}):
	 * 'R' = the row's stone carrier ({@code Blocks.stone} for MT.Stone, the vanilla
	 * blackstone item for OP.stone.dat(MT.STONES.Blackstone) — the OP.stone.dat 1.20.1
	 * identity is the plain block item), 'h' = {@code #gt6:tools/hard_hammer} (the tool
	 * letter, not consumed). Result 1x the anvil block. The vanilla shaped auto-mirror
	 * carries CR.DEF_MIR (the bathing-pot note).
	 */
	private ShapedRecipeBuilder anvilBuilder(net.minecraft.world.level.block.Block aResult, net.minecraft.world.level.ItemLike aStone) {
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, aResult)
				.pattern("RRR")
				.pattern("hR ")
				.pattern("RRR")
				.define('R', aStone)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_stone", has(aStone));
	}

	/**
	 * The Greg o'Lantern crafting row (task placeables) — the upstream
	 * "Greg o'Lantern" registration tail (Loader_MultiTileEntities.java:2031,
	 * {@code "Pk", "T ", 'P' Blocks.pumpkin, 'T' OD.blockTorch}): the pumpkin OVER the
	 * torch. DECLARED FOLD: the 'k' knife tool letter rides the CR tool-letter face the
	 * port vanilla-crafting bridge has no carrier for (the clay-bowl 'R' rollingpin
	 * precedent, the dormant-row ruling) — the shape keeps the pumpkin+torch column.
	 */
	private ShapedRecipeBuilder gregOLanternBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GT6Placeables.GREG_O_LANTERN.get())
				.pattern("P")
				.pattern("T")
				.define('P', net.minecraft.world.level.block.Blocks.PUMPKIN)
				.define('T', net.minecraft.world.item.Items.TORCH)
				.unlockedBy("has_pumpkin", has(net.minecraft.world.item.Items.PUMPKIN));
	}

	private ShapedRecipeBuilder bathingPotSteelBuilder() {
		TagKey<Item> tSteelPlates = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "stainless_steel");
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GT6Kitchen.BATHING_POT_STEEL.get())
				.pattern(" f ")
				.pattern("PhP")
				.pattern("PPP")
				.define('P', tSteelPlates)
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_stainless_steel_plate", has(tSteelPlates));
	}

	/**
	 * The Clay Bowl FORWARD shaped — the upstream :132 row VERBATIM
	 * ({@code CR.shaped(IL.Ceramic_Bowl_Raw.get(1), CR.DEF_NCC, "k R", "C C", "CCC",
	 * 'C', OD.itemClay, 'R', OreDictToolNames.rollingpin)}): the tool keys ride the port
	 * tool tags — 'k' = {@code #gt6:tools/knife} (the upstream {@code OreDictToolNames
	 * .knife} translation, the p29-w5-t2 band), 'R' = {@code #gt6:tools/rolling_pin}
	 * (CS.java:1888, GTRollingPinItem; the p24 Recipe patch takes the 1-damage craft
	 * toll). The "no rolling-pin tool" dormancy note of the pre-#45 port is RETIRED —
	 * the tools pool landed p29-w5 scene-six. Issue #45 C1: this row is the raw bowl's
	 * crafting source (it previously had none — the smelt was the only live face).
	 */
	private ShapedRecipeBuilder clayBowlForwardBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6Kitchen.CLAY_BOWL_RAW.get())
				.pattern("k R")
				.pattern("C C")
				.pattern("CCC")
				.define('C', Items.CLAY_BALL)
				.define('k', GT6ItemTags.TOOLS_KNIFE)
				.define('R', GT6ItemTags.TOOLS_ROLLING_PIN)
				.unlockedBy("has_clay", has(Items.CLAY_BALL));
	}

	/**
	 * The Clay Bowl reverse shapeless — the upstream :119 tail VERBATIM
	 * ({@code CR.shapeless(ST.make(Items.clay_ball, 5, 0), CR.DEF_NCC, new Object[] {last()})}):
	 * 1 raw bowl back to its 5 clay balls (the {@code OreDictItemData(MT.Clay, U*5)} mass).
	 * Result 5x {@code minecraft:clay_ball}.
	 */
	private ShapelessRecipeBuilder clayBowlReverseBuilder() {
		return ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 5)
				.requires(GT6Kitchen.CLAY_BOWL_RAW.get())
				.unlockedBy("has_clay_bowl_raw", has(GT6Kitchen.CLAY_BOWL_RAW.get()));
	}

	/**
	 * The Clay Juicer FORWARD shaped — the upstream :131 row VERBATIM
	 * ({@code CR.shaped(IL.Juicer_Raw.get(1), CR.DEF_NCC, "kCR", "CCC", ...)}), the same
	 * knife+rolling-pin tag-define face as the bowl row (issue #45 C1 — before this row
	 * the Juicer block was creative-only: no raw item, no crafting face at all).
	 */
	private ShapedRecipeBuilder clayJuicerBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6Kitchen.CLAY_JUICER_RAW.get())
				.pattern("kCR")
				.pattern("CCC")
				.define('C', Items.CLAY_BALL)
				.define('k', GT6ItemTags.TOOLS_KNIFE)
				.define('R', GT6ItemTags.TOOLS_ROLLING_PIN)
				.unlockedBy("has_clay", has(Items.CLAY_BALL));
	}

	/**
	 * The Clay Juicer reverse shapeless — the upstream :118 tail VERBATIM
	 * ({@code CR.shapeless(ST.make(Items.clay_ball, 4, 0), ...)}): 1 raw juicer back to
	 * its 4 clay balls (the {@code OreDictItemData(MT.Clay, U*4)} mass).
	 */
	private ShapelessRecipeBuilder clayJuicerReverseBuilder() {
		return ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 4)
				.requires(GT6Kitchen.CLAY_JUICER_RAW.get())
				.unlockedBy("has_clay_juicer", has(GT6Kitchen.CLAY_JUICER_RAW.get()));
	}

	/**
	 * The Clay Juicer hardening smelt — the upstream :2184 registration-line tail VERBATIM
	 * ({@code RM.add_smelting(IL.Juicer_Raw.get(1), IL.Juicer.get(1))}):
	 * {@code gt6:clay_juicer} → {@code gt6:juicer}. The honest vanilla defaults carry the
	 * unspecified upstream columns (xp 0, 200 ticks — the clayBowlSmeltingBuilder shape).
	 * The factory signature is the one leg split beyond the save face: 1.20.1
	 * {@code smelting(Ingredient, RecipeCategory, ItemLike, float, int)}, 21.1 takes the
	 * result as an {@code ItemStack} (the neoforge-api-1211 patch :61).
	 */
	private SimpleCookingRecipeBuilder clayJuicerSmeltingBuilder() {
		//? if forge {
		return SimpleCookingRecipeBuilder.smelting(
						net.minecraft.world.item.crafting.Ingredient.of(GT6Kitchen.CLAY_JUICER_RAW.get()),
						RecipeCategory.MISC, GT6Kitchen.JUICER_ITEM.get(), 0.0F, 200)
				.unlockedBy("has_clay_juicer", has(GT6Kitchen.CLAY_JUICER_RAW.get()));
		//?} else {
		/*return SimpleCookingRecipeBuilder.smelting(
						net.minecraft.world.item.crafting.Ingredient.of(GT6Kitchen.CLAY_JUICER_RAW.get()),
						RecipeCategory.MISC, new ItemStack(GT6Kitchen.JUICER_ITEM.get()), 0.0F, 200)
				.unlockedBy("has_clay_juicer", has(GT6Kitchen.CLAY_JUICER_RAW.get()));
		*///?}
	}

	/**
	 * The Clay Bowl hardening smelt — the upstream :2177 registration-line tail VERBATIM
	 * ({@code RM.add_smelting(IL.Ceramic_Bowl_Raw.get(1), IL.Ceramic_Bowl.get(1))}):
	 * {@code gt6:clay_bowl} → {@code gt6:mixing_bowl}. The honest vanilla defaults carry
	 * the unspecified upstream columns (xp 0, 200 ticks — the vanilla smelt constant).
	 * The factory signature is the one leg split beyond the save face: 1.20.1
	 * {@code smelting(Ingredient, RecipeCategory, ItemLike, float, int)} (vanilla
	 * SimpleCookingRecipeBuilder.java:59), 21.1 takes the result as an
	 * {@code ItemStack} (the neoforge-api-1211 patch :61).
	 */
	private SimpleCookingRecipeBuilder clayBowlSmeltingBuilder() {
		//? if forge {
		return SimpleCookingRecipeBuilder.smelting(
						net.minecraft.world.item.crafting.Ingredient.of(GT6Kitchen.CLAY_BOWL_RAW.get()),
						RecipeCategory.MISC, GT6Kitchen.MIXING_BOWL.get(), 0.0F, 200)
				.unlockedBy("has_clay_bowl_raw", has(GT6Kitchen.CLAY_BOWL_RAW.get()));
		//?} else {
		/*return SimpleCookingRecipeBuilder.smelting(
						net.minecraft.world.item.crafting.Ingredient.of(GT6Kitchen.CLAY_BOWL_RAW.get()),
						RecipeCategory.MISC, new ItemStack(GT6Kitchen.MIXING_BOWL.get()), 0.0F, 200)
				.unlockedBy("has_clay_bowl_raw", has(GT6Kitchen.CLAY_BOWL_RAW.get()));
		*///?}
	}

	/**
	 * The Progress Sensor crafting row (task sensors-core) — the upstream
	 * {@code "WGW"}/{"CXC"}/{"WPW"} row VERBATIM (Loader_MultiTileEntities.java:1995,
	 * CR.DEF) with the keys translated onto the port faces: 'P' = {@code OP.plateDouble
	 * .dat(MT.TinAlloy)} → the new {@code #forge:double_plates/tin_alloy} material tag
	 * (the DOUBLE_PLATES family band, the GTCEu TagPrefix.java:442 path precedent), 'W' =
	 * {@code OP.wireFine.dat(MT.RedAlloy)} → {@code #forge:fine_wires/red_alloy}
	 * (TagPrefix.java:575), 'R' = {@code OD.itemRedstone} → {@code #gt6:redstone} (the
	 * dust_redstone member, the spray-can 'R' precedent), 'G' = {@code
	 * OD.blockGlassColorless} → vanilla {@code minecraft:glass} (vanilla glass IS the
	 * colorless variant; no colourless-glass tag exists), 'B' = {@code OP.bolt.dat(
	 * MT.TinAlloy)} → {@code #forge:bolts/tin_alloy} (TagPrefix.java:514), 'C' = the
	 * vanilla comparator ({@code Items.comparator}), 'X' = {@code OP.gearGtSmall.dat(
	 * MT.Brass)} → {@code #forge:small_gears/brass} (TagPrefix.java:599). The upstream key
	 * map also binds 'R' (redstone) and 'B' (bolt) — UNUSED by the pattern; upstream CR
	 * tolerated that, the vanilla builder throws ("Ingredients are defined but not used"),
	 * so those two keys stay out. Result 1x {@code gt6:progressmeter}.
	 */
	private ShapedRecipeBuilder progressmeterBuilder() {
		TagKey<Item> tDoublePlates = GT6ItemTags.materialTag(GT6ItemTags.DOUBLE_PLATES_FAMILY, MT.TinAlloy);
		TagKey<Item> tFineWires = GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY, MT.RedAlloy);
		TagKey<Item> tBolts = GT6ItemTags.materialTag(GT6ItemTags.BOLTS_FAMILY, MT.TinAlloy);
		TagKey<Item> tSmallGears = GT6ItemTags.materialTag(GT6ItemTags.SMALL_GEARS_FAMILY, MT.Brass);
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Sensors.BLOCKS_BY_PATH.get("progressmeter").get())
				.pattern("WGW")
				.pattern("CXC")
				.pattern("WPW")
				.define('P', tDoublePlates)
				.define('W', tFineWires)
				.define('G', Items.GLASS)
				.define('C', Items.COMPARATOR)
				.define('X', tSmallGears)
				.unlockedBy("has_fine_wire", has(tFineWires));
	}

	/**
	 * The Miniature Nether Portal crafting row (task portals-mini-nether-end) — the
	 * upstream "SSS"/"SsS"/"SSS" SHAPE (Loader_MultiTileEntities.java:2003) with the GT6
	 * dead-cell 's' folded to a vanilla space — the 8-S ring is the vanilla furnace pattern.
	 * DECLARED DEVIATION on the key: upstream 'S' = {@code OP.stickLong.dat(MT.Obsidian)},
	 * but this port generates no Obsidian long rods — the material lacks the STICKS
	 * item-generator tag (MT.java:2395) and the GTMaterialItems.forceItemGeneration table
	 * is a FROZEN seam — so 'S' folds to the vanilla obsidian block (the vanilla-carrier
	 * form; 8 rods ≈ 1 block of stock, the count shape is preserved: 8 S cells).
	 * Result 1x {@code gt6:mini_portal_nether}.
	 */
	private ShapedRecipeBuilder miniPortalNetherBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Portals.PORTAL_NETHER.get())
				.pattern("SSS")
				.pattern("S S")
				.pattern("SSS")
				.define('S', net.minecraft.world.item.Items.OBSIDIAN)
				.unlockedBy("has_obsidian", has(net.minecraft.world.item.Items.OBSIDIAN));
	}

	/**
	 * The Miniature End Portal crafting row (task portals-mini-nether-end) — the
	 * upstream "ESE"/"SGS"/"ESE" row VERBATIM (Loader :2004): 'S' = Endstone long rods
	 * → vanilla end_stone (the same no-rods fold as the Nether row above), 'E' =
	 * {@code OP.gem.dat(MT.EnderEye)} → vanilla {@code minecraft:ender_eye}, 'G' =
	 * {@code Items.ghast_tear} → vanilla {@code minecraft:ghast_tear}. Result 1x
	 * {@code gt6:mini_portal_end}.
	 */
	private ShapedRecipeBuilder miniPortalEndBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Portals.PORTAL_END.get())
				.pattern("ESE")
				.pattern("SGS")
				.pattern("ESE")
				.define('S', net.minecraft.world.item.Items.END_STONE)
				.define('E', net.minecraft.world.item.Items.ENDER_EYE)
				.define('G', net.minecraft.world.item.Items.GHAST_TEAR)
				.unlockedBy("has_ender_eye", has(net.minecraft.world.item.Items.ENDER_EYE));
	}

	/**
	 * The Fluid-O-Meter Sensor crafting row (task sensors-core) — the upstream
	 * {@code "WYW"}/{"BXB"}/{"WPW"} row VERBATIM (Loader_MultiTileEntities.java:1986):
	 * the shared P/W/R/G/B/C alphabet as {@link #progressmeterBuilder}, plus 'X' = {@code
	 * OD.pressurePlateStone} → vanilla {@code minecraft:stone_pressure_plate} and 'Y' =
	 * {@code Items.bucket} → vanilla {@code minecraft:bucket}. Result 1x
	 * {@code gt6:fluidometer}.
	 */
	private ShapedRecipeBuilder fluidometerBuilder() {
		TagKey<Item> tDoublePlates = GT6ItemTags.materialTag(GT6ItemTags.DOUBLE_PLATES_FAMILY, MT.TinAlloy);
		TagKey<Item> tFineWires = GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY, MT.RedAlloy);
		TagKey<Item> tBolts = GT6ItemTags.materialTag(GT6ItemTags.BOLTS_FAMILY, MT.TinAlloy);
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Sensors.BLOCKS_BY_PATH.get("fluidometer").get())
				.pattern("WYW")
				.pattern("BXB")
				.pattern("WPW")
				.define('P', tDoublePlates)
				.define('W', tFineWires)
				.define('B', tBolts)
				.define('X', Items.STONE_PRESSURE_PLATE)
				.define('Y', Items.BUCKET)
				.unlockedBy("has_fine_wire", has(tFineWires));
	}

	/**
	 * The ULV FE→EU converter crafting row (task b-fe-converter-machine, retuned by
	 * task ulv-recipe-retune — the stone-crucible ruling): the progressmeter vocabulary
	 * keeps its shape (double plates + RedAlloy fine wires) but the shell drops from
	 * TinAlloy to plain Tin double plates, killing the row's only mid-game gate — TinAlloy
	 * is 1 Fe + 1 Sn (Loader_Recipes_Alloys.java:57) and melting iron (1811 K) exceeds the
	 * stone crucible's 1375 K ceiling, while tin (505 K) and copper (1358 K) both sit inside
	 * the day-one stone-crucible chain (research.p28-ulv-create-compat). The pattern is
	 * unchanged:
	 * <pre>"PWP" / "PCP" / "PWP"</pre>
	 * P = {@code #forge:double_plates/tin} (the tag is real on the item path — generated
	 * {@code data/forge/tags/items/double_plates/tin.json} = {@code [gt6:plate_double_tin]},
	 * OP.plateDouble is in {@code itemPathPrefixes}), W = {@code #forge:fine_wires/red_alloy},
	 * C = {@code #forge:ingots/copper}. Result 1x {@code gt6:fe_converter}.
	 */
	private ShapedRecipeBuilder feConverterBuilder() {
		TagKey<Item> tDoublePlates = GT6ItemTags.materialTag(GT6ItemTags.DOUBLE_PLATES_FAMILY, MT.Sn);
		TagKey<Item> tFineWires = GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY, MT.RedAlloy);
		TagKey<Item> tIngots = GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, MT.Copper);
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6FeConverters.FE_CONVERTER_ITEM.get())
				.pattern("PWP")
				.pattern("PCP")
				.pattern("PWP")
				.define('P', tDoublePlates)
				.define('W', tFineWires)
				.define('C', tIngots)
				.unlockedBy("has_fine_wire", has(tFineWires));
	}

	/**
	 * The Water Wheel crafting row (task c-water-wheel) — the kTFRUAddon
	 * "Water Mill" registration-row SHAPE + QUANTITIES over the port carriers
	 * (tileEntityInit0.java:112, CR.DEF "PPP"/"SRS"/"PPP"): the grid carries the
	 * research-card census 6 planks + 2 bronze rings + 1 axle part. Key translation (the
	 * early-QoL ruling — every input pre-ULV reachable):
	 * <ul>
	 * <li>'P' = {@code #minecraft:planks} ({@code ItemTags.PLANKS}) — the upstream
	 *     WoodTreated planks fold onto the whole plank tag (the vanilla-material tag
	 *     precedent of the hammer-stone route);</li>
	 * <li>'R' = the bronze ring — the kTFRU ring-bearing semantics, resolved through
	 *     {@code GTMaterialItems.get(OP.ring, MT.Bronze)} (the BARE-item form is the
	 *     hopper 'X' plateCurved precedent: no RINGS tag family exists in
	 *     {@link GT6ItemTags}); bronze = the first machine-material tier, smeltable under
	 *     the stone-crucible 1375 K ceiling (the ULV chain's own progress gate);</li>
	 * <li>'A' = {@code gt6:axle_wood_treated_small} — the rotation core IS the kinetics
	 *     family's own carrier part (the wheel is a kinetics machine; the upstream 轴件
	 *     slot reads exactly this).</li>
	 * </ul>
	 * Result 1x {@code gt6:water_wheel}.
	 */
	/**
	 * The Electric Transformer (ULV-LV) crafting row (task c-ulv-lv-transformer) —
	 * the :881 shape over the MATERIAL-LOCK carriers: 'M' =
	 * {@code OP.casingSmall.dat(MT.SteelGalvanized)} (the Electric_T[1] LV-era rung —
	 * the DECLARED DEVIATION from the :881 {@code casingMachine.dat(Electric_T[0])}
	 * = TinAlloy lowest-price housing; the casingMachine → casingSmall prefix fold is
	 * the static-storage 'M' precedent), 'W'/'X' = {@code #forge:fine_wires/copper}
	 * (the :881 wireGt01/wireGt04 Cu columns fold — no 1x/4x wire item rows in the
	 * port, the count differential folds into the 7 wire cells), 'I' =
	 * {@code #forge:double_plates/iron} (the :881 column verbatim). Result 1x
	 * {@code gt6:electric_transformer}.
	 */
	// -------------------------------------------------------------------------
	// task w4-battery-storage — the battery + BatteryBox crafting rows (the Loader
	// :1009-:1068 battery strings and the :893-:896 box strings; the pattern columns ride
	// the GT6Batteries mapping helpers, the datagen and the tests share that one source).
	// Declared CUTS (the absent-input rows ride the pool, the electrolyzer_part precedent):
	//   - the LARGE BatteryBox tiers 1..5 ('M' = the tier transformer 10041..10045 — the
	//     port transformer ladder carries only the ULV-LV row 10040; the rows land when
	//     their transformer tiers do, the GT6ElectricTransformers extension-seat ruling);
	//   - the Energium crystals carry NO rows — upstream registers none either
	//     (:1079-:1092 are bare registrations).
	// The 'x' tool letter = the wire cutter tool tag (the tank-valve h/s fold precedent);
	// the 'C' circuit column keys the #gt6:circuit<i> TAG (the OD_CIRCUITS oredict
	// semantics — any item of that circuit tier matches).
	// -------------------------------------------------------------------------

	/** The battery row's recipe id: battery/&lt;path&gt; (the part_family/&lt;path&gt; convention). */
	public static ResourceLocation batteryRecipeId(GT6Batteries.BatteryRow aRow) {
		// the path rides a local so the two-arg RL ctor args stay bare identifiers (the
		// swap-table regex note, staticStorageRecipeId form)
		String tPath = "battery/" + aRow.path();
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/** The BatteryBox row's recipe id: battery_box/&lt;path&gt; (the same convention). */
	public static ResourceLocation batteryBoxRecipeId(GT6Batteries.BoxRow aRow) {
		String tPath = "battery_box/" + aRow.path();
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/** One staged BatteryBox row: the shared builder + the row its save face ids from (the PartFamilyRecipeRow shape). */
	record BatteryBoxRecipeRow(ShapedRecipeBuilder builder, GT6Batteries.BoxRow row) {}

	/** The wire/cable item face of a tier+size+form (the GTWires parallel-list composition). */
	private static Item wireItem(int aTier, int aSize, boolean aInsulated) {
		String tPath = GT6Batteries.wirePath(aTier, aSize, aInsulated);
		java.util.List<GTWireSpecs.Variant> tVariants = GTWireSpecs.variants();
		for (int i = 0; i < tVariants.size(); i++) {
			if (GTWireSpecs.registryName(tVariants.get(i)).equals(tPath)) return GTWires.FAMILY_ITEMS.get(i).get();
		}
		throw new IllegalStateException("gt6 batteries: no wire item for " + tPath);
	}

	private ShapedRecipeBuilder batteryRecipeBuilder(GT6Batteries.BatteryRow aRow) {
		TagKey<Item> tPlates = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "battery_alloy");
		String[] tPattern = GT6Batteries.batteryPattern(aRow);
		String tKeys = tPattern[0] + tPattern[1] + tPattern[2];
		ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder
				.shaped(RecipeCategory.MISC, GT6Batteries.BATTERY_ITEMS.get(aRow.path()).get())
				.pattern(tPattern[0]).pattern(tPattern[1]).pattern(tPattern[2])
				.define('W', wireItem(aRow.tier(), 1, true)); // the CABLES_01 column (1x insulated cable)
		if (tKeys.indexOf('x') >= 0) tBuilder.define('x', GT6ItemTags.TOOLS_WIRE_CUTTER); // the CR 'x' wirecutter letter (absent on the EV rows)
		tBuilder
				.define('B', GT6Batteries.CELL_ITEMS.get(GT6Batteries.batteryCellPath(aRow)).get())
				.define('P', tPlates); // the OP.plate.dat(MT.BatteryAlloy) column
		int tCircuit = GT6Batteries.batteryCircuitTier(aRow);
		if (tCircuit >= 0) tBuilder.define('C', GT6ItemTags.gt6("circuit" + tCircuit)); // the OD_CIRCUITS column
		return tBuilder.unlockedBy("has_battery_alloy", has(tPlates));
	}

	private java.util.List<BatteryBoxRecipeRow> batteryBoxRecipeBuilders() {
		java.util.List<BatteryBoxRecipeRow> rRows = new ArrayList<>();
		for (GT6Batteries.BoxRow tRow : GT6Batteries.BOX_ROWS) {
			int tSize = tRow.slots() == 16 ? 4 : 1; // the CABLES_01 vs CABLES_04 column (wire sizes ride the same ladder)
			Item tM; // the 'M' column: casingMachine(Electric_T[i]) folds to casingSmall (the transformer fold);
					// the LARGE rows carry the TIER TRANSFORMER item (getItem(10040+i)) — the p35
					// transformer ladder landed, the declared cut closes (the extension-seat ruling)
			if (tRow.slots() == 16) {
				tM = gregtech6.registry.GT6ElectricTransformers.itemOfTier(tRow.tier());
			} else {
				tM = GTMaterialItems.get(gregapi.data.OP.casingSmall,
						gregtech6.registry.GT6ElectricDynamos.ELECTRIC_T_LADDER.get(tRow.tier()).get()).get();
			}
			ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder
					.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Batteries.BATTERY_BOX_ITEMS.get(tRow.path()).get())
					.pattern(GT6Batteries.BOX_PATTERN[0]).pattern(GT6Batteries.BOX_PATTERN[1]).pattern(GT6Batteries.BOX_PATTERN[2])
					.define('W', wireItem(tRow.tier(), tSize, false)) // the WIRES_01/04 column
					.define('C', wireItem(tRow.tier(), tSize, true)) // the CABLES_01/04 column
					.define('X', GT6ItemTags.gt6("circuit" + tRow.tier())) // the OD_CIRCUITS column
					.define('M', tM)
					.unlockedBy("has_circuit", has(GT6ItemTags.gt6("circuit" + tRow.tier())));
			rRows.add(new BatteryBoxRecipeRow(tBuilder, tRow));
		}
		return rRows;
	}

	// -------------------------------------------------------------------------
	// task energy-tail-machines — the Long Distance families (the :909-:913
	// transformer strings and the Loader_Blocks.java:162-177 wire strings). Declared
	// folds: the LD 'M' column = the SAME-tier electric transformer item (the p35
	// ladder); the unbound 'x' dead cell folds to a space; the wire rows guard every
	// column with itemOrNull — an absent plateCurved/Rubber plate skips the row (the
	// plunger CR.ONLY_IF_HAS_RESULT face).
	// -------------------------------------------------------------------------

	/** The cable item of an exact registry path (the wireItem general form — here the exact-token walk for cable_annealed_copper_gt04). */
	private Item wireItemByPath(String aPath) {
		java.util.List<GTWireSpecs.Variant> tVariants = GTWireSpecs.variants();
		for (int i = 0; i < tVariants.size(); i++) {
			if (GTWireSpecs.registryName(tVariants.get(i)).equals(aPath)) return gregtech6.registry.GTWires.FAMILY_ITEMS.get(i).get();
		}
		throw new IllegalStateException("gt6 longdist: no wire item for " + aPath);
	}

	/** The LD transformer rows: "WMW","M ","WMW" over the tier transformer + the annealed-copper 4x cable (:909-:913). */
	private java.util.List<BridgeCraftRow> ldTransformerCraftingRows() {
		java.util.List<BridgeCraftRow> rRows = new java.util.ArrayList<>();
		Item tCable = wireItemByPath("cable_annealed_copper_gt04"); // the 'W' column, cableGt04(AnnealedCopper) verbatim
		for (gregtech6.registry.GT6LongDistanceTransformers.LDRow tRow : gregtech6.registry.GT6LongDistanceTransformers.ROWS) {
			Item tTransformer = gregtech6.registry.GT6ElectricTransformers.itemOfTier(tRow.tier()); // the 'M' column (getItem(10044+i))
			String tPath = tRow.path();
			rRows.add(new BridgeCraftRow(ShapedRecipeBuilder
					.shaped(RecipeCategory.MISC, gregtech6.registry.GT6LongDistanceTransformers.ITEMS_BY_PATH.get(tPath).get())
					.pattern("WMW").pattern("M  ").pattern("WMW")
					.define('W', tCable)
					.define('M', tTransformer)
					.unlockedBy("has_transformer", has(tTransformer)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tPath)));
		}
		return rRows;
	}

	/** The LD wire material tokens, meta order (Loader_Blocks.java:162-177 'W' column: Sn Pb Cu Ag Au Electrum BlueAlloy ElectrotineAlloy Steel Al W TungstenSteel Os Pt Nq Graphene) = the GTWireSpecs tokens. */
	private static final String[] LD_WIRE_TOKENS = {"tin", "lead", "copper", "silver", "gold", "electrum", "blue_alloy",
			"electrotine_alloy", "steel", "aluminium", "tungsten", "tungstensteel", "osmium_elemental", "platinum", "naquadah", "graphene"};

	/** The 16 LD wire rows: "RSR","PWP","RSR" — R = plate(Rubber), P = plateCurved(Cu), S = plateCurved(Al), W = wireGt16 of the row material. */
	private java.util.List<BridgeCraftRow> ldWireCraftingRows() {
		java.util.List<BridgeCraftRow> rRows = new java.util.ArrayList<>();
		Item tRubberPlate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Rubber);
		Item tCuCurved = itemOrNull(gregapi.data.OP.plateCurved, gregapi.data.MT.Cu);
		Item tAlCurved = itemOrNull(gregapi.data.OP.plateCurved, gregapi.data.MT.Al);
		if (tRubberPlate == null || tCuCurved == null || tAlCurved == null) return rRows; // the declared skip: the column carriers are absent
		for (gregtech6.registry.GT6LongDistWires.WireRow tRow : gregtech6.registry.GT6LongDistWires.ROWS) {
			Item tWire16 = wireItemByPath("wire_" + LD_WIRE_TOKENS[tRow.meta()] + "_gt16"); // the 'W' column, wireGt16.dat(mat)
			String tPath = gregtech6.registry.GT6LongDistWires.pathOf(tRow.meta());
			rRows.add(new BridgeCraftRow(ShapedRecipeBuilder
					.shaped(RecipeCategory.MISC, gregtech6.registry.GT6LongDistWires.ITEMS_BY_META.get(tRow.meta()).get())
					.pattern("RSR").pattern("PWP").pattern("RSR")
					.define('R', tRubberPlate)
					.define('P', tCuCurved)
					.define('S', tAlCurved)
					.define('W', tWire16)
					.unlockedBy("has_wire", has(tWire16)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tPath)));
		}
		return rRows;
	}

	private java.util.List<BridgeCraftRow> transformerCraftingRows() {
		java.util.List<BridgeCraftRow> rRows = new java.util.ArrayList<>();
		TagKey<Item> tDoublePlates = GT6ItemTags.materialTag(GT6ItemTags.DOUBLE_PLATES_FAMILY, MT.Iron);
		for (GT6ElectricTransformers.TransformerRow tRow : GT6ElectricTransformers.ROWS) {
			// the casing column: row 0 keeps the LV-era LOCK (the p28 declared deviation,
			// SteelGalvanized); rows 1-8 use the VERBATIM Electric_T[i] member (the
			// CASING_LADDER column — SteelGalvanized coincides with the lock from tier 1 up).
			// casingMachine folds to casingSmall everywhere (the static-storage fold);
			// an absent casing item skips the row (the plunger CR.ONLY_IF_HAS_RESULT face).
			net.minecraft.world.item.Item tCasing = tRow.tier() == 0
					? itemOrNull(GT6ElectricTransformers.CASING_LOCK_PREFIX.get(), GT6ElectricTransformers.CASING_LOCK_MATERIAL.get())
					: itemOrNull(gregapi.data.OP.casingSmall, GT6ElectricTransformers.CASING_LADDER.get(tRow.tier()).get());
			if (tCasing == null) continue; // the declared skip: no casingSmall item row for the tier material
			// the wire columns: rows :881-:883 carry wireGt01/04(ANY.Cu), rows :884-:889
			// switch to MT.AnnealedCopper (the upstream :884 dat() switch) — both fold to
			// the fine_wires tag (no 1x/4x wire item rows in the port).
			TagKey<Item> tFineWires = GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY,
					"annealed_copper".equals(tRow.wireToken()) ? MT.AnnealedCopper : MT.Copper);
			String tPath = tRow.path();
			rRows.add(new BridgeCraftRow(ShapedRecipeBuilder
					.shaped(RecipeCategory.MISC, GT6ElectricTransformers.ITEMS_BY_PATH.get(tPath).get())
					.pattern(GT6ElectricTransformers.RECIPE_PATTERN[0])
					.pattern(GT6ElectricTransformers.RECIPE_PATTERN[1])
					.pattern(GT6ElectricTransformers.RECIPE_PATTERN[2])
					.define('W', tFineWires)
					.define('X', tFineWires)
					.define('I', tDoublePlates)
					.define('M', tCasing)
					.unlockedBy("has_fine_wire", has(tFineWires)),
					tRow.tier() == 0 ? ELECTRIC_TRANSFORMER_ID : new ResourceLocation(GT6DataGenerators.MOD_ID, tPath)));
		}
		return rRows;
	}

	// -------------------------------------------------------------------------
	// task w4-eu-bridge — the EU-bridge crafting rows (Loader :817-821/:833-837/
	// :849-853 recipe strings, the tool letters per CR.java:339-361: d = screwdriver,
	// h = hard hammer, w = wrench). Declared folds and CUTS:
	//   - casingMachineDouble → casingSmall (the transformerBuilder casing fold —
	//     no casingMachineDouble item row in the port);
	//   - the wire columns → the fine_wires tags (the count differential folded, the
	//     transformer fold precedent);
	//   - the Heater T5 row CUT: wireGt16(SiC) has NO port fine-wires face (SiC carries
	//     no WIRES flag — the part-family absent-input CUT precedent, the SiC coil row).
	// The T1 result ids equal the block path, T2-T5 the _tN suffix (the vanilla
	// result-path naming convention).
	// -------------------------------------------------------------------------

	/** One EU-bridge crafting row — the builder + the result-path id its save face ids from (the PartFamilyRecipeRow shape). */
	private record BridgeCraftRow(ShapedRecipeBuilder builder, ResourceLocation id) {}

	/** The rung path of a family ladder index (the GTMachines.bridgePath form mirrored locally). */
	private static String bridgePath(String aFamily, int aTier) {
		return aTier == 0 ? aFamily : aFamily + "_t" + (aTier + 1);
	}

	private static java.util.List<BridgeCraftRow> euBridgeCraftingRows() {
		java.util.List<BridgeCraftRow> rRows = new java.util.ArrayList<>();
		// --- the Heaters :817-821 ("TCT","CMC","TCd") — T5 CUT (SiC wires absent) ---
		gregapi.oredict.OreDictMaterial[] tHeaterWires = {gregapi.data.MT.Copper, gregapi.data.MT.Constantan, gregapi.data.MT.Kanthal, gregapi.data.MT.Nichrome};
		for (int i = 0; i < 4; i++) {
			gregapi.oredict.OreDictMaterial tMat = bridgeMat(i);
			String tPath = bridgePath("electric_heater", i);
			rRows.add(new BridgeCraftRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTMachines.ELECTRIC_HEATER_ITEMS_BY_PATH.get(tPath).get())
					.pattern("TCT").pattern("CMC").pattern("TCd")
					.define('T', GTMaterialItems.get(gregapi.data.OP.screw, tMat).get())
					.define('C', GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY, tHeaterWires[i]))
					.define('M', GTMaterialItems.get(gregapi.data.OP.casingSmall, tMat).get())
					.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
					.unlockedBy("has_screw", has(GTMaterialItems.get(gregapi.data.OP.screw, tMat).get())), new ResourceLocation(GT6DataGenerators.MOD_ID, tPath)));
		}
		// --- the Engines :833-837 ("PhP","CIC","PwP") ---
		gregapi.oredict.OreDictMaterial[] tMagnets = {gregapi.data.MT.IronMagnetic, gregapi.data.MT.SteelMagnetic, gregapi.data.MT.SteelMagnetic, gregapi.data.MT.NeodymiumMagnetic, gregapi.data.MT.NeodymiumMagnetic};
		gregapi.oredict.OreDictMaterial[] tCopperWires = {gregapi.data.MT.Copper, gregapi.data.MT.Copper, gregapi.data.MT.AnnealedCopper, gregapi.data.MT.AnnealedCopper, gregapi.data.MT.AnnealedCopper};
		for (int i = 0; i < 5; i++) {
			gregapi.oredict.OreDictMaterial tMat = bridgeMat(i);
			String tPath = bridgePath("electric_engine", i);
			rRows.add(new BridgeCraftRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTMachines.ELECTRIC_ENGINE_ITEMS_BY_PATH.get(tPath).get())
					.pattern("PhP").pattern("CIC").pattern("PwP")
					.define('P', GTMaterialItems.get(gregapi.data.OP.plateTriple, tMat).get())
					.define('I', GTMaterialItems.get(gregapi.data.OP.stickLong, tMagnets[i]).get())
					.define('C', GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY, tCopperWires[i]))
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
					.define('w', GT6ItemTags.TOOLS_WRENCH)
					.unlockedBy("has_plate", has(GTMaterialItems.get(gregapi.data.OP.plateTriple, tMat).get())), new ResourceLocation(GT6DataGenerators.MOD_ID, tPath)));
		}
		// --- the Motors :849-853 ("TIT","CMC","TGd") ---
		for (int i = 0; i < 5; i++) {
			gregapi.oredict.OreDictMaterial tMat = bridgeMat(i);
			String tPath = bridgePath("electric_motor", i);
			rRows.add(new BridgeCraftRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTMachines.ELECTRIC_MOTOR_ITEMS_BY_PATH.get(tPath).get())
					.pattern("TIT").pattern("CMC").pattern("TGd")
					.define('T', GTMaterialItems.get(gregapi.data.OP.screw, tMat).get())
					.define('I', GTMaterialItems.get(gregapi.data.OP.stickLong, tMagnets[i]).get())
					.define('C', GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY, tCopperWires[i]))
					.define('M', GTMaterialItems.get(gregapi.data.OP.casingSmall, tMat).get())
					.define('G', GTMaterialItems.get(gregapi.data.OP.gearGt, tMat).get())
					.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
					.unlockedBy("has_gear", has(GTMaterialItems.get(gregapi.data.OP.gearGt, tMat).get())), new ResourceLocation(GT6DataGenerators.MOD_ID, tPath)));
		}
		return rRows;
	}

	/** The Electric_T[1..5] rung material by ladder index (upstream MT.java:3691 members, the dynamo family's ladder face) — the single source lives in {@code GTMachines.electricTierMat} (task c2-controller-tint: the bridge/laser tint rows ride the same ladder). */
	private static gregapi.oredict.OreDictMaterial bridgeMat(int aTier) {
		return gregtech6.registry.GTMachines.electricTierMat(aTier);
	}

	private ShapedRecipeBuilder waterWheelBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6Kinetics.WATER_WHEEL_ITEM.get())
				.pattern("PPP")
				.pattern("RAR")
				.pattern("PPP")
				.define('P', ItemTags.PLANKS)
				.define('R', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.ring, gregapi.data.MT.Bronze).get())
				.define('A', GT6Kinetics.AXLE_ITEMS.get(GT6Kinetics.axleName("wood_treated", 0)).get())
				.unlockedBy("has_planks", has(ItemTags.PLANKS));
	}

	/**
	 * The three CR.shapeless self-recast rows (task sensors-core) — the upstream
	 * {@code CR.shapeless(aRegistry.getItem(), CR.DEF_NCC, new Object[] {aRegistry.getItem()})}
	 * per-row companion (Loader :1979-:1999 every row's tail): 1 sensor item → 1 clean
	 * sensor item, the NBT-reset recast face (a configured sensor re-crafted back to the
	 * clean item; the vanilla builder carries no NBT so the recast is structurally exact).
	 * All three rows land even though the Electrometer SHAPED row does not — its 'X' key
	 * is {@code IL.Electro_Meter} (a dedicated GT6 item, NOT on the port's item path) and
	 * its 'Y' key is {@code OP.wireGt01.dat(ANY.Cu)} (the 1/8x wire prefix, not on the
	 * port's itemPathPrefixes gate) — the shaped row is CUT declared and rides the
	 * sensors-batch2 pool with the meter item.
	 */
	private List<SensorRecastRow> sensorRecastBuilders() {
		List<SensorRecastRow> rRows = new ArrayList<>(gregtech6.registry.GT6Sensors.ROWS.size());
		for (gregtech6.registry.GT6Sensors.SensorRow tRow : gregtech6.registry.GT6Sensors.ROWS) {
			Item tItem = gregtech6.registry.GT6Sensors.ITEMS_BY_PATH.get(tRow.path()).get();
			rRows.add(new SensorRecastRow(ShapelessRecipeBuilder
					.shapeless(RecipeCategory.MISC, tItem)
					.requires(tItem)
					.unlockedBy("has_sensor", has(tItem)),
					tRow.path()));
		}
		return rRows;
	}

	/** One staged sensor recast: the shared builder + the path its save face ids from (the GrassRecipeRow shape). */
	private record SensorRecastRow(ShapelessRecipeBuilder builder, String path) {}

    // -------------------------------------------------------------------------
    // task w3-nbtdesign-parts ③ — the part-family crafting rows (Loader
    // :1138-1182 recipe strings, the tool letters per CR.java:339-361: h = hard
    // hammer, s = saw, w = wrench, x = wirecutter, d = screwdriver). Declared CUTS
    // (the absent-input rows ride the pool, the sensor-shaped-row precedent):
    //   - coils 18043/18044 (SiC/Os): the materials carry NO port item rows;
    //   - electrolyzer_part :18105 ('W' wireGt01 Pt + 'C' OD_CIRCUITS[6] absent);
    //   - distill_part :18102 / sluice_part :18106 ('P' pipeSmall/pipeMedium — the
    //     pipe prefixes are off the port item path);
    //   - ventilation_unit :1184 ('F' IL.Cover_Vent + 'E' IL.MOTORS[1] absent — RESOLVED
    //     by the circuits-crafting-c band below, the ruling-B substitutions);
    //   - processor units :1185-1189 ('S/D/R/E' IL.Processor_Crystal_* absent — RESOLVED
    //     by the circuits-crafting-c band below, the gem-tag substitutions).
    // The coil 'W' fold: wireGt04 has no port items — ONE fine wire per cell (the
    // transformerBuilder fold precedent, the count differential declared).
    // -------------------------------------------------------------------------
    private static final ResourceLocation WOOD_WALL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "part_family/wood_wall");
    private static final ResourceLocation CENTRIFUGE_PART_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "part_family/centrifuge_part");
    private static final ResourceLocation CRUSHER_WHEELS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "part_family/crusher_wheels");
    private static final ResourceLocation SHREDDER_BLADES_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "part_family/shredder_blades");

    /** One staged part-family row: the shared builder + the id its save face ids from. */
    private record PartFamilyRecipeRow(ShapedRecipeBuilder builder, ResourceLocation id) {}

    private java.util.List<PartFamilyRecipeRow> partFamilyRecipeBuilders() {
        java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
        TagKey<Item> tTreatedPlates = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "wood_treated");
        TagKey<Item> tLeadPlates = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "lead");
        // the Wood Wall (:1139 "W W","sPh","W W" — W = plate WoodTreated, P = plate Pb)
        rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("wood_wall").get())
                .pattern("W W").pattern("sPh").pattern("W W")
                .define('W', tTreatedPlates)
                .define('P', tLeadPlates)
                .define('s', GT6ItemTags.TOOLS_SAW)
                .define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
                .unlockedBy("has_plates", has(tTreatedPlates)), WOOD_WALL_ID));
        // the Centrifuge Part (:1174 "TwT","GMG","TdT" — casingMachine → casingSmall, the
        // transformerBuilder fold precedent)
        Item tWsCasing = GTMaterialItems.get(gregapi.data.OP.casingSmall, gregapi.data.MT.TungstenSteel).get();
        Item tWsGear = GTMaterialItems.get(gregapi.data.OP.gearGt, gregapi.data.MT.TungstenSteel).get();
        Item tWsScrew = GTMaterialItems.get(gregapi.data.OP.screw, gregapi.data.MT.TungstenSteel).get();
        rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("centrifuge_part").get())
                .pattern("TwT").pattern("GMG").pattern("TdT")
                .define('T', tWsScrew)
                .define('w', GT6ItemTags.TOOLS_WRENCH)
                .define('G', tWsGear)
                .define('M', tWsCasing)
                .define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
                .unlockedBy("has_plates", has(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "tungstensteel"))), CENTRIFUGE_PART_ID));
        // the Crusher Wheels (:1181 "DDD","GDG","GMG" — casingMachineDouble → casingSmall fold)
        rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("crusher_wheels").get())
                .pattern("DDD").pattern("GDG").pattern("GMG")
                .define('D', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "diamond"))
                .define('G', tWsGear)
                .define('M', tWsCasing)
                .unlockedBy("has_plates", has(Items.DIAMOND)), CRUSHER_WHEELS_ID));
        // the Shredder Blades (:1182 "DGD","GwG","GMG" — D = plateGem Diamond)
        rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("shredder_blades").get())
                .pattern("DGD").pattern("GwG").pattern("GMG")
                .define('D', GTMaterialItems.get(gregapi.data.OP.plateGem, gregapi.data.MT.Diamond).get())
                .define('G', tWsGear)
                .define('w', GT6ItemTags.TOOLS_WRENCH)
                .define('M', tWsCasing)
                .unlockedBy("has_plates", has(Items.DIAMOND)), SHREDDER_BLADES_ID));
        // the three resolvable coils (:1167/:1169/:1172 "WWW","WxW","WWW" — the wireGt04
        // column folds to one fine wire per cell, the transformerBuilder fold)
        coilBuilder("large_copper_coil", gregapi.data.MT.AnnealedCopper)
                .ifPresent(tPair -> rRows.add(new PartFamilyRecipeRow(tPair, new ResourceLocation(GT6DataGenerators.MOD_ID, "part_family/large_copper_coil"))));
        coilBuilder("large_nichrome_coil", gregapi.data.MT.Nichrome)
                .ifPresent(tPair -> rRows.add(new PartFamilyRecipeRow(tPair, new ResourceLocation(GT6DataGenerators.MOD_ID, "part_family/large_nichrome_coil"))));
        coilBuilder("large_iridium_coil", gregapi.data.MT.Ir)
                .ifPresent(tPair -> rRows.add(new PartFamilyRecipeRow(tPair, new ResourceLocation(GT6DataGenerators.MOD_ID, "part_family/large_iridium_coil"))));
        return rRows;
    }

    // -------------------------------------------------------------------------
    // task circuits-crafting-c — the circuits C-column band (coordinator ruling B,
    // every substitution row DECLARED): the Ventilation Unit (Loader_MultiTileEntities
    // .java:1184) + the five Quadcore Processor Units (:1185-1189) crafting rows. The
    // 'M' casingMachine(SteelGalvanized) column folds to casingSmall (the Locker/diesel
    // fold precedent); the 'C' OD_CIRCUITS[3]/[6] columns key the #gt6:circuit3/6 TAGS
    // (the battery-box 'X' column precedent, NOT a deviation). The absent-input columns
    // substitute per the ruling-B map (each DEVIATION declared in-line):
    //   - 'F' IL.Cover_Vent → 6x iron rods + 1x iron rotor folded IN-PLACE (the upstream
    //     Cover_Vent crafting row itself, MultiItemTechnological.java:161 "RRR","RXR",
    //     "RRR" — the cover item has no port identity, its own recipe is the nearest
    //     semantic carrier);
    //   - 'E' IL.MOTORS[1] (LV Electric Motor) → gt6:electric_motor_t2 (the port bridge
    //     ladder T2 = upstream 10022 LV, GTMachines.java:4711);
    //   - the four IL.Processor_Crystal_* colors → the matching GEM TAGS (ruby/emerald/
    //     sapphire/diamond — #forge:gems/<color>, the crystal circuits' material carriers;
    //     the versatile row's S/D/R/E columns map Sapphire/Diamond/Ruby/Emerald per
    //     upstream :1185, the four ladder rows' 'P' column per :1186-1189).
    // -------------------------------------------------------------------------
    private static final ResourceLocation VENTILATION_UNIT_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "part_circuit/ventilation_unit");

    /** The PU ladder row (path, gem tag snake) — the 'P' column per upstream :1186-1189. */
    private record ProcessorCircuitRow(String path, String gemSnake) {}

    //? if forge {
    private void partCircuitRows(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
        TagKey<Item> tGalvPlates = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "steel_galvanized");
        Item tCasing = itemOrNull(gregapi.data.OP.casingSmall, gregapi.data.MT.SteelGalvanized);
        Item tRotor = itemOrNull(gregapi.data.OP.rotor, gregapi.data.MT.Iron);
        Item tStick = itemOrNull(gregapi.data.OP.stick, gregapi.data.MT.Iron);
        Item tMotorLV = gregtech6.registry.GTMachines.ELECTRIC_MOTOR_ITEMS_BY_PATH.get("electric_motor_t2").get();
        TagKey<Item> tCircuit6 = GT6ItemTags.gt6("circuit6");
        TagKey<Item> tCircuit3 = GT6ItemTags.gt6("circuit3");
        if (tCasing == null || tRotor == null || tStick == null || tMotorLV == null) return; // the silent-skip guard

        // the Ventilation Unit :1184 "FwF"/"CMC"/"EdE" — F = the folded 6-rods+rotor vent
        // (DEV, see band doc), w = wrench, E = the LV motor bridge item (DEV), C = #gt6:circuit3
        TagKey<Item> tIronRods = GT6ItemTags.materialTag(GT6ItemTags.RODS_FAMILY, MT.Iron);
        ShapedRecipeBuilder tVent = ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("ventilation_unit").get())
                .pattern("FFF").pattern("wCw").pattern("EEE")
                .define('F', tIronRods)                       // DEV: Cover_Vent folds to its own :161 iron-rod field (rotor folded away, the count face)
                .define('w', GT6ItemTags.TOOLS_WRENCH)
                .define('C', tCircuit3)
                .define('E', tMotorLV)                        // DEV: IL.MOTORS[1] → the T2 bridge motor item
                .unlockedBy("has_circuit3", has(tCircuit3));
        tVent.save(aConsumer, VENTILATION_UNIT_ID);

        // the Versatile PU :1185 "DCS"/"CMC"/"RCE" — D/S/R/E gems per :1185, C = #gt6:circuit6
        ShapedRecipeBuilder tVersatile = ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("processor_unit_versatile").get())
                .pattern("DCS").pattern("CMC").pattern("RCE")
                .define('D', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "diamond"))
                .define('S', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "sapphire"))
                .define('R', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "ruby"))
                .define('E', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "emerald"))
                .define('C', tCircuit6)
                .define('M', tCasing)
                .unlockedBy("has_circuit6", has(tCircuit6));
        tVersatile.save(aConsumer, new ResourceLocation(GT6DataGenerators.MOD_ID, "part_circuit/processor_unit_versatile"));

        // the four ladder PUs :1186-1189 "PCP"/"CMC"/"PCP" — P = the row's crystal gem tag
        ProcessorCircuitRow[] tLadder = {
                new ProcessorCircuitRow("processor_unit_logic"     , "diamond"  ), // :1186 IL.Processor_Crystal_Diamond
                new ProcessorCircuitRow("processor_unit_control"   , "ruby"     ), // :1187 IL.Processor_Crystal_Ruby
                new ProcessorCircuitRow("processor_unit_storage"   , "emerald"  ), // :1188 IL.Processor_Crystal_Emerald
                new ProcessorCircuitRow("processor_unit_conversion", "sapphire" ), // :1189 IL.Processor_Crystal_Sapphire
        };
        for (ProcessorCircuitRow tRow : tLadder) {
            TagKey<Item> tGem = GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, tRow.gemSnake());
            String tPath = "part_circuit/" + tRow.path(); // the precomputed arg — the swap-table regex note
            ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                            gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get(tRow.path()).get())
                    .pattern("PCP").pattern("CMC").pattern("PCP")
                    .define('P', tGem)
                    .define('C', tCircuit6)
                    .define('M', tCasing)
                    .unlockedBy("has_circuit6", has(tCircuit6));
            tBuilder.save(aConsumer, new ResourceLocation(GT6DataGenerators.MOD_ID, tPath));
        }
    }
    //?} else {
    /*private void partCircuitRows(net.minecraft.data.recipes.RecipeOutput aOutput) {
        Item tCasing = itemOrNull(gregapi.data.OP.casingSmall, gregapi.data.MT.SteelGalvanized);
        Item tMotorLV = gregtech6.registry.GTMachines.ELECTRIC_MOTOR_ITEMS_BY_PATH.get("electric_motor_t2").get();
        TagKey<Item> tCircuit6 = GT6ItemTags.gt6("circuit6");
        TagKey<Item> tCircuit3 = GT6ItemTags.gt6("circuit3");
        if (tCasing == null || tMotorLV == null) return; // the silent-skip guard
        TagKey<Item> tIronRods = GT6ItemTags.materialTag(GT6ItemTags.RODS_FAMILY, MT.Iron);
        ShapedRecipeBuilder tVent = ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("ventilation_unit").get())
                .pattern("FFF").pattern("wCw").pattern("EEE")
                .define('F', tIronRods)
                .define('w', GT6ItemTags.TOOLS_WRENCH)
                .define('C', tCircuit3)
                .define('E', tMotorLV)
                .unlockedBy("has_circuit3", has(tCircuit3));
        tVent.save(aOutput, VENTILATION_UNIT_ID);
        ShapedRecipeBuilder tVersatile = ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("processor_unit_versatile").get())
                .pattern("DCS").pattern("CMC").pattern("RCE")
                .define('D', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "diamond"))
                .define('S', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "sapphire"))
                .define('R', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "ruby"))
                .define('E', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "emerald"))
                .define('C', tCircuit6)
                .define('M', tCasing)
                .unlockedBy("has_circuit6", has(tCircuit6));
        tVersatile.save(aOutput, new ResourceLocation(GT6DataGenerators.MOD_ID, "part_circuit/processor_unit_versatile"));
        ProcessorCircuitRow[] tLadder = {
                new ProcessorCircuitRow("processor_unit_logic"     , "diamond"  ),
                new ProcessorCircuitRow("processor_unit_control"   , "ruby"     ),
                new ProcessorCircuitRow("processor_unit_storage"   , "emerald"  ),
                new ProcessorCircuitRow("processor_unit_conversion", "sapphire" ),
        };
        for (ProcessorCircuitRow tRow : tLadder) {
            TagKey<Item> tGem = GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, tRow.gemSnake());
            String tPath = "part_circuit/" + tRow.path(); // the precomputed arg — the swap-table regex note
            ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                            gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get(tRow.path()).get())
                    .pattern("PCP").pattern("CMC").pattern("PCP")
                    .define('P', tGem)
                    .define('C', tCircuit6)
                    .define('M', tCasing)
                    .unlockedBy("has_circuit6", has(tCircuit6));
            tBuilder.save(aOutput, new ResourceLocation(GT6DataGenerators.MOD_ID, tPath));
        }
    }
    *///?}

    // -------------------------------------------------------------------------
    // task circuits-crafting-c — the 26 integrated-circuit rows (the upstream
    // ItemIntegratedCircuit.java:58-85 self-crafting block, verbatim): the base row
    // (:58 "GhG"/"SSS"/"GwG" — G = gearGtSmall Iron, S = stick Iron, h/w = the tool
    // tags, configuration 0), the shapeless reset (:59, configuration 0 over ANY
    // circuit stack), and the 24 configuration-programming rows (:61-85, each producing
    // the circuit at the FIXED Damage configuration 1..24; 'P' = the base circuit item,
    // 'd' = the screwdriver tag). All ride the gt6:circuit_program serializer — vanilla
    // shaped JSON cannot carry a result data tag (ShapedRecipe.java:274), the
    // GT6MaterialToolRecipe serializer precedent stamps the configuration at assemble.
    // -------------------------------------------------------------------------
    private static final ResourceLocation CIRCUIT_BASE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "integrated_circuit");

    //? if forge {
    private void circuitProgramRows(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
        Item tCircuit = GT6Circuits.INTEGRATED_CIRCUIT.get();
        TagKey<Item> tSmallGears = GT6ItemTags.materialTag(GT6ItemTags.SMALL_GEARS_FAMILY, MT.Iron);
        TagKey<Item> tIronSticks = GT6ItemTags.materialTag(GT6ItemTags.RODS_FAMILY, MT.Iron);

        // the base row :58 — configuration 0 (the vanilla-shaped grid through the shared seam)
        var tBaseKey = new java.util.LinkedHashMap<Character, net.minecraft.world.item.crafting.Ingredient>();
        tBaseKey.put('G', net.minecraft.world.item.crafting.Ingredient.of(tSmallGears));
        tBaseKey.put('S', net.minecraft.world.item.crafting.Ingredient.of(tIronSticks));
        tBaseKey.put('h', net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_HARD_HAMMER));
        tBaseKey.put('w', net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_WRENCH));
        saveCircuitProgram(aConsumer, "shaped", java.util.List.of("GhG", "SSS", "GwG"), tBaseKey, tCircuit, CIRCUIT_BASE_ID, 0);

        // the reset :59 — the circuit re-crafted back to configuration 0. The upstream
        // shapeless form is re-expressed as the 1x1 SHAPED row (semantically identical:
        // one circuit anywhere in the grid) — the 21.1 leg's shared serializer codec is
        // the shaped pattern codec, and a forked shapeless codec would be a second parse
        // face for one row (the ponytail cut).
        var tResetKey = new java.util.LinkedHashMap<Character, net.minecraft.world.item.crafting.Ingredient>();
        tResetKey.put('P', net.minecraft.world.item.crafting.Ingredient.of(tCircuit));
        saveCircuitProgram(aConsumer, "shaped", java.util.List.of("P"), tResetKey, tCircuit, CIRCUIT_BASE_ID.withSuffix("_reset"), 0);

        // the 24 programming rows :61-85
        for (int i = 1; i <= 24; i++) {
            java.util.List<String> tPattern = circuitPattern(i - 1);
            var tKey = new java.util.LinkedHashMap<Character, net.minecraft.world.item.crafting.Ingredient>();
            for (String tLine : tPattern) {
                for (char tChar : tLine.toCharArray()) {
                    if (tChar == ' ') continue;
                    tKey.put(tChar, tChar == 'P'
                            ? net.minecraft.world.item.crafting.Ingredient.of(tCircuit)
                            : net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_SCREWDRIVER));
                }
            }
            String tIdPath = "integrated_circuit/config_" + i; // the precomputed arg — the swap-table regex note
            saveCircuitProgram(aConsumer, "shaped", tPattern, tKey, tCircuit,
                    new ResourceLocation(GT6DataGenerators.MOD_ID, tIdPath), i);
        }
    }
    //?} else {
    /*private void circuitProgramRows(net.minecraft.data.recipes.RecipeOutput aOutput) {
        Item tCircuit = GT6Circuits.INTEGRATED_CIRCUIT.get();
        TagKey<Item> tSmallGears = GT6ItemTags.materialTag(GT6ItemTags.SMALL_GEARS_FAMILY, MT.Iron);
        TagKey<Item> tIronSticks = GT6ItemTags.materialTag(GT6ItemTags.RODS_FAMILY, MT.Iron);
        var tBaseKey = new java.util.LinkedHashMap<Character, net.minecraft.world.item.crafting.Ingredient>();
        tBaseKey.put('G', net.minecraft.world.item.crafting.Ingredient.of(tSmallGears));
        tBaseKey.put('S', net.minecraft.world.item.crafting.Ingredient.of(tIronSticks));
        tBaseKey.put('h', net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_HARD_HAMMER));
        tBaseKey.put('w', net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_WRENCH));
        saveCircuitProgram(aOutput, "shaped", java.util.List.of("GhG", "SSS", "GwG"), tBaseKey, tCircuit, CIRCUIT_BASE_ID, 0);
        var tResetKey = new java.util.LinkedHashMap<Character, net.minecraft.world.item.crafting.Ingredient>();
        tResetKey.put('P', net.minecraft.world.item.crafting.Ingredient.of(tCircuit));
        saveCircuitProgram(aOutput, "shaped", java.util.List.of("P"), tResetKey, tCircuit, CIRCUIT_BASE_ID.withSuffix("_reset"), 0);
        for (int i = 1; i <= 24; i++) {
            java.util.List<String> tPattern = circuitPattern(i - 1);
            var tKey = new java.util.LinkedHashMap<Character, net.minecraft.world.item.crafting.Ingredient>();
            for (String tLine : tPattern) {
                for (char tChar : tLine.toCharArray()) {
                    if (tChar == ' ') continue;
                    tKey.put(tChar, tChar == 'P'
                            ? net.minecraft.world.item.crafting.Ingredient.of(tCircuit)
                            : net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_SCREWDRIVER));
                }
            }
            String tIdPath = "integrated_circuit/config_" + i; // the precomputed arg — the swap-table regex note
            saveCircuitProgram(aOutput, "shaped", tPattern, tKey, tCircuit,
                    new ResourceLocation(GT6DataGenerators.MOD_ID, tIdPath), i);
        }
    }
    *///?}

    /** The per-configuration pattern rows (the upstream :61-85 sparse-grid shapes verbatim). */
    private static java.util.List<String> circuitPattern(int aIndex) {
        // the upstream grid shapes per configuration (ItemIntegratedCircuit.java:61-85 verbatim)
        return switch (aIndex) {
            case 0 -> java.util.List.of("d ", " P");
            case 1 -> java.util.List.of("d ", "P ");
            case 2 -> java.util.List.of(" d", "P ");
            case 3 -> java.util.List.of("Pd", "  ");
            case 4 -> java.util.List.of("P ", " d");
            case 5 -> java.util.List.of("P ", "d ");
            case 6 -> java.util.List.of(" P", "d ");
            case 7 -> java.util.List.of("dP", "  ");
            case 8 -> java.util.List.of("P d");
            case 9 -> java.util.List.of("P  ", "  d");
            case 10 -> java.util.List.of("P  ", "   ", "  d");
            case 11 -> java.util.List.of("P  ", "   ", " d ");
            case 12 -> java.util.List.of("  P", "   ", "  d");
            case 13 -> java.util.List.of("  P", "   ", " d ");
            case 14 -> java.util.List.of("  P", "   ", "d  ");
            case 15 -> java.util.List.of("  P", "d  ", "   ");
            case 16 -> java.util.List.of("   ", "   ", "d P");
            case 17 -> java.util.List.of("   ", "d  ", "  P");
            case 18 -> java.util.List.of("d  ", "   ", "  P");
            case 19 -> java.util.List.of(" d ", "   ", "  P");
            case 20 -> java.util.List.of("d  ", "   ", "P  ");
            case 21 -> java.util.List.of(" d ", "   ", "P  ");
            case 22 -> java.util.List.of("  d", "   ", "P  ");
            default -> java.util.List.of("   ", "  d", "P  "); // :85 config 24
        };
    }

    /**
     * The circuit-program save seam — every row emits through the hand-rolled
     * FinishedRecipe (the MaterialToolRow face): the vanilla shaped/shapeless JSON plus
     * the ONE configuration field, the gt6:circuit_program serializer type.
     */
    //? if forge {
    private void saveCircuitProgram(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer,
            String aType, java.util.List<String> aPattern, java.util.LinkedHashMap<Character, net.minecraft.world.item.crafting.Ingredient> aKey,
            Item aResult, ResourceLocation aId, int aConfiguration) {
        ResourceLocation tAdvancementId = aId.withPrefix("recipes/misc/");
        net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
                .recipeAdvancement()
                .parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
                .addCriterion("has_circuit", has(GT6Circuits.INTEGRATED_CIRCUIT.get()))
                .addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(aId))
                .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(aId))
                .requirements(net.minecraft.advancements.RequirementsStrategy.OR);
        aConsumer.accept(new CircuitProgramRow(aId, aType, aPattern, aKey, aResult, aConfiguration, tAdvancement, tAdvancementId));
    }

    /**
     * The forge FinishedRecipe face — the vanilla shaped/shapeless JSON + the ONE
     * configuration field (the MaterialToolRow shape, the gt6:circuit_program serializer).
     */
    private record CircuitProgramRow(ResourceLocation aId, String aType, java.util.List<String> aPattern,
            java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> aKey, Item aResult, int aConfiguration,
            net.minecraft.advancements.Advancement.Builder aAdvancement, ResourceLocation aAdvancementId)
            implements net.minecraft.data.recipes.FinishedRecipe {

        @Override
        public void serializeRecipeData(com.google.gson.JsonObject aJson) {
            aJson.addProperty("category", net.minecraft.world.item.crafting.CraftingBookCategory.MISC.getSerializedName());
            if ("shapeless".equals(aType)) {
                com.google.gson.JsonArray tIngredients = new com.google.gson.JsonArray();
                for (net.minecraft.world.item.crafting.Ingredient tIngredient : aKey.values()) {
                    tIngredients.add(tIngredient.toJson());
                }
                aJson.add("ingredients", tIngredients);
            } else {
                com.google.gson.JsonArray tPattern = new com.google.gson.JsonArray();
                for (String tRow : aPattern) {
                    tPattern.add(tRow);
                }
                aJson.add("pattern", tPattern);
                com.google.gson.JsonObject tKey = new com.google.gson.JsonObject();
                for (java.util.Map.Entry<Character, net.minecraft.world.item.crafting.Ingredient> tEntry : aKey.entrySet()) {
                    tKey.add(String.valueOf(tEntry.getKey()), tEntry.getValue().toJson());
                }
                aJson.add("key", tKey);
            }
            com.google.gson.JsonObject tResult = new com.google.gson.JsonObject();
            tResult.addProperty("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aResult).toString());
            tResult.addProperty("count", 1);
            aJson.add("result", tResult);
            aJson.addProperty("show_notification", true);
            aJson.addProperty("configuration", aConfiguration);
        }

        @Override
        public ResourceLocation getId() { return aId; }

        @Override
        public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() {
            return GT6CircuitProgramRecipe.Registration.SERIALIZER.get();
        }

        @Override
        public com.google.gson.JsonObject serializeAdvancement() { return aAdvancement.serializeToJson(); }

        @Override
        public ResourceLocation getAdvancementId() { return aAdvancementId; }
    }
    //?} else {
    /*private void saveCircuitProgram(net.minecraft.data.recipes.RecipeOutput aOutput,
            String aType, java.util.List<String> aPattern, java.util.LinkedHashMap<Character, net.minecraft.world.item.crafting.Ingredient> aKey,
            Item aResult, ResourceLocation aId, int aConfiguration) {
        ResourceLocation tAdvancementId = aId.withPrefix("recipes/misc/");
        net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
                .recipeAdvancement()
                .addCriterion("has_circuit", has(GT6Circuits.INTEGRATED_CIRCUIT.get()))
                .addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(aId))
                .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(aId))
                .requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR);
        gregtech6.items.GT6CircuitProgramRecipe tRecipe;
        if ("shapeless".equals(aType)) {
            tRecipe = new gregtech6.items.GT6CircuitProgramRecipe("", net.minecraft.world.item.crafting.CraftingBookCategory.MISC,
                    net.minecraft.world.item.crafting.ShapedRecipePattern.of(java.util.Map.of(), java.util.List.of("")),
                    new net.minecraft.world.item.ItemStack(aResult), true, aConfiguration);
        } else {
            tRecipe = new gregtech6.items.GT6CircuitProgramRecipe("", net.minecraft.world.item.crafting.CraftingBookCategory.MISC,
                    net.minecraft.world.item.crafting.ShapedRecipePattern.of(aKey, aPattern),
                    new net.minecraft.world.item.ItemStack(aResult), true, aConfiguration);
        }
        aOutput.accept(aId, tRecipe, tAdvancement.build(tAdvancementId));
    }
    *///?}

    // -------------------------------------------------------------------------
    // task w3-tank-valves — the 25 Tank Main Valve rows (Loader :1195-1222 recipe
    // strings: wood " R ","rMs"," R "; the small pair " R ","hMs"," R " over the ROW's
    // wall; the large pair "PPP","hMs","PPP" over the SMALL valve + the material plate
    // (plateDense on the dense larges); R/r = OP.ring of Pb (wood) or the row material,
    // h = the hard-hammer tool tag, s = the saw tool tag — the lowercase r TOOL letter
    // folds to the same ring item, the vanilla-JSON consume-all rule, the coil 'W' fold
    // precedent). Declared CUTS: none — every input resolves (the ring/plate bare items
    // null-guard to a silent row skip, the coilBuilder precedent).
    // -------------------------------------------------------------------------
    private static final String TANK_VALVE_RECIPE_PREFIX = "tank_valve/";

    /** One tank row's recipe id: tank_valve/&lt;path&gt; (the part_family/&lt;path&gt; convention). */
    private ResourceLocation tankValveRecipeId(String aPath) {
        return new ResourceLocation(GT6DataGenerators.MOD_ID, TANK_VALVE_RECIPE_PREFIX + aPath);
    }

    private java.util.List<PartFamilyRecipeRow> tankValveRecipeBuilders() {
        java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
        Item tLeadRing = itemOrNull(gregapi.data.OP.ring, gregapi.data.MT.Pb);
        for (gregtech6.registry.GT6Tanks.TankValveRow tRow : gregtech6.registry.GT6Tanks.ROWS) {
            String tWallPath = tRow.wallPath();
            if (tRow.flammable()) {
                // :1195 wood — " R ","rMs"," R " over the wood wall
                if (tLeadRing == null) continue;
                net.minecraft.world.level.block.Block tWoodWall = gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get(tWallPath).get();
                rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Tanks.BLOCKS_BY_PATH.get(tRow.path()).get())
                        .pattern(" R ").pattern("rMs").pattern(" R ")
                        .define('R', tLeadRing)
                        .define('r', tLeadRing)
                        .define('M', tWoodWall)
                        .define('s', GT6ItemTags.TOOLS_SAW)
                        .unlockedBy("has_ring", has(tLeadRing)), tankValveRecipeId(tRow.path())));
                continue;
            }
            net.minecraft.world.level.block.Block tWall = gregtech6.registry.GTMultiBlocks.anyPartBlock(tWallPath);
            if (tWall == null) continue;
            if (tRow.size() == 3) {
                // :1196-1208 — the small pair: " R ","hMs"," R " over the row's wall
                var tRing = gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.ring, tRow.material().get()); // the bare-local form — the RegistryObject/DeferredHolder swap keeps the type arguments
                if (tRing == null) continue;
                rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Tanks.BLOCKS_BY_PATH.get(tRow.path()).get())
                        .pattern(" R ").pattern("hMs").pattern(" R ")
                        .define('R', tRing.get())
                        .define('M', tWall)
                        .define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
                        .define('s', GT6ItemTags.TOOLS_SAW)
                        .unlockedBy("has_ring", has(tRing.get())), tankValveRecipeId(tRow.path())));
            } else {
                // :1210-1222 — the large pair: "PPP","hMs","PPP" over the SMALL valve + plates
                boolean tDense = tWallPath.startsWith("dense_wall_");
                String tMatSlug = tRow.path().substring((tDense ? "tank_large_dense_" : "tank_large_").length());
                String tSmallPath = (tDense ? "tank_small_dense_" : "tank_small_") + tMatSlug;
                net.minecraft.world.level.block.Block tSmallValve = gregtech6.registry.GT6Tanks.BLOCKS_BY_PATH.get(tSmallPath).get();
                var tPlate = gregtech6.registry.GTMaterialItems.get(
                        tDense ? gregapi.data.OP.plateDense : gregapi.data.OP.plate, tRow.material().get()); // the bare-local form (the swap note above)
                if (tPlate == null) continue;
                rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Tanks.BLOCKS_BY_PATH.get(tRow.path()).get())
                        .pattern("PPP").pattern("hMs").pattern("PPP")
                        .define('P', tPlate.get())
                        .define('M', tSmallValve)
                        .define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
                        .define('s', GT6ItemTags.TOOLS_SAW)
                        .unlockedBy("has_plate", has(tPlate.get())), tankValveRecipeId(tRow.path())));
            }
        }
        return rRows;
    }

    /** A bare item by prefix+material — null when the flood has no row (the silent-skip guard). */
    private Item itemOrNull(gregapi.oredict.OreDictPrefix aPrefix, gregapi.oredict.OreDictMaterial aMaterial) {
        var tHandle = gregtech6.registry.GTMaterialItems.get(aPrefix, aMaterial); // the bare-local form (the swap note above)
        return tHandle == null ? null : tHandle.get();
    }

    /** One coil row builder — empty when the material's fine-wire tag has no members (the silent skip). */
    private java.util.Optional<ShapedRecipeBuilder> coilBuilder(String aPath, gregapi.oredict.OreDictMaterial aMaterial) {
        net.minecraft.world.level.block.Block tBlock = gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get(aPath).get();
        TagKey<Item> tFineWires = GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY, aMaterial);
        return java.util.Optional.of(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tBlock)
                .pattern("WWW").pattern("WxW").pattern("WWW")
                .define('W', tFineWires)
                .define('x', GT6ItemTags.TOOLS_WIRE_CUTTER)
                .unlockedBy("has_plates", has(tFineWires)));
    }

    // -----------------------------------------------------------------------
    // The six dig-tool steel-route rows (task w5-t1-dig-six). The upstream
    // AdvancedCraftingTool rows (Loader_Tools.java:336-341 — head + sticks + the worn
    // hammer/file) over the single steel tier ruling d: the tool-HEAD system is the pool
    // cut, so the head folds to STEEL PLATES (#forge:plates/steel, the wrench-row key),
    // the sticks to the ecosystem rod tag, and the worn crafting tools ride the hammer/
    // file TAGS (the bending-cylinder tool-letter mapping; each pays one durability point
    // per craft through its crafting-remaining face). Per-tool head counts keep the
    // upstream mAmount ratios: pickaxe 3, construction 5 (the heavy head), gem = the
    // diamond tip (the Loader_Tools.java:338 Amber-suggestion row, the tip is the gem
    // identity the single-tier ruling keeps), shovel/spade 1, universal 5 + the file
    // pair (the multi-tool face).
    private static final TagKey<Item> DIG_STEEL_PLATES = gregtech6.datagen.GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "steel");

    private ShapedRecipeBuilder digToolBuilder(net.minecraft.world.item.Item aResult, String[] aPattern, Object... aKeyValues) {
        ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, aResult);
        tBuilder.pattern(aPattern[0]);
        if (aPattern.length > 1) tBuilder.pattern(aPattern[1]);
        if (aPattern.length > 2) tBuilder.pattern(aPattern[2]);
        for (int tIndex = 0; tIndex + 1 < aKeyValues.length; tIndex += 2) {
            Character tKey = (Character) aKeyValues[tIndex];
            Object tValue = aKeyValues[tIndex + 1];
            if (tValue instanceof TagKey<?> tTag) tBuilder.define(tKey, (TagKey<Item>) tTag);
            else tBuilder.define(tKey, (net.minecraft.world.item.Item) tValue);
        }
        return tBuilder.unlockedBy("has_steel_plate", has(DIG_STEEL_PLATES));
    }

    /** The pickaxe row — the upstream :339 PICKAXE head (3 steel plates) + 2 rods, hammer+file worn. */
    private ShapedRecipeBuilder pickaxeBuilder() {
        return digToolBuilder(GT6Tools.PICKAXE.get(), new String[] {"PPP", " S ", "hSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The gem pickaxe row — the upstream :338 GEM_PICK head + the diamond tip (the gem identity). */
    private ShapedRecipeBuilder pickaxeGemBuilder() {
        return digToolBuilder(GT6Tools.PICKAXE_GEM.get(), new String[] {" G ", " S ", "hSf"},
                'G', Tags.Items.GEMS_DIAMOND, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The construction pickaxe row — the upstream :337 CONSTRUCTION_PICK heavy head (5 plates). */
    private ShapedRecipeBuilder pickaxeConstructionBuilder() {
        return digToolBuilder(GT6Tools.PICKAXE_CONSTRUCTION.get(), new String[] {"PPP", "PSP", "hSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The shovel row — the upstream :340 SHOVEL head (1 plate) + 2 rods, hammer+file worn. */
    private ShapedRecipeBuilder shovelBuilder() {
        return digToolBuilder(GT6Tools.SHOVEL.get(), new String[] {"hPf", " S ", " S "},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The spade row — the upstream :341 SPADE head (1 plate) + 2 rods, hammer+file worn. */
    private ShapedRecipeBuilder spadeBuilder() {
        return digToolBuilder(GT6Tools.SPADE.get(), new String[] {" P ", " S ", "hSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The universal spade row — the heavy head + the file pair (the multi-tool face). */
    private ShapedRecipeBuilder universalSpadeBuilder() {
        return digToolBuilder(GT6Tools.UNIVERSAL_SPADE.get(), new String[] {"PPP", "PSP", "fSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'f', GT6ItemTags.TOOLS_FILE);
    }

    // ---------------------------------------------------------------------
    // The six blade-tool steel-route rows (task w5-t2-blade-six). The upstream
    // AdvancedCraftingTool rows (Loader_Tools.java:337/:342-343 — SWORD toolHeadSword,
    // AXE toolHeadAxe, DOUBLE_AXE toolHeadAxeDouble; KNIFE/BUTCHERYKNIFE/CLUB carry NO
    // AdvancedCraftingTool row — the knife's grid face is the cutting-board pool, the
    // club's is the 6*U material scale) over the single steel tier: the plate counts
    // keep the head-amount PROPORTIONS (the t1 mapping; the exact toolHead mAmount
    // ladder is the standing pool cut). The worn hammer/file letters ride the dig-tool
    // builder (each pays one point through the crafting-remaining face).
    // -----------------------------------------------------------------------
    // The five field-tool steel-route rows (task w5-t4-field-five). The upstream
    // AdvancedCraftingTool rows for hoe/sense/plow (Loader_Tools.java:344-346 — the
    // :344 Birch and :346 Spruce suggestions converge steel per the t1 single-tier
    // ruling); the branch cutter and hand drill ride the same family shape. Plate
    // counts keep the upstream tool-head material ratios (OP.java:244-246/:254):
    // hoe 2 plates (toolHeadHoe U*2), sense 3 (toolHeadSense U*3), plow 4
    // (toolHeadPlow U*4), branch cutter 5 (the :133 setMaterialAmount(5*U)),
    // hand drill 1 (the :152 toolHeadArrow+2*bolt row — the arrow head folds to a
    // plate, the two bolts to two rods, the file worn like the :349 screwdriver row).
    /** The hoe row — the 2-plate head (the :344 Birch-suggestion row, steel converged). */
    private ShapedRecipeBuilder hoeBuilder() {
        return digToolBuilder(GT6Tools.HOE.get(), new String[] {"PP ", " S ", "hSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The plow row — the 4-plate head (the :346 Spruce-suggestion row, steel converged). */
    private ShapedRecipeBuilder plowBuilder() {
        return digToolBuilder(GT6Tools.PLOW.get(), new String[] {"PP ", "PP ", "hSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The branch cutter row — the 5-plate material (the :133 row, the scissors-form file pair). */
    private ShapedRecipeBuilder branchCutterBuilder() {
        return digToolBuilder(GT6Tools.BRANCH_CUTTER.get(), new String[] {"PPP", "PSP", "fSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The sense row — the 3-plate scythe blade (the :345 row). */
    private ShapedRecipeBuilder senseBuilder() {
        return digToolBuilder(GT6Tools.SENSE.get(), new String[] {"PPP", " S ", "hSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The club row — the 6*U heavy mass (6 plates + the rod, the biggest blade tier). */
    private ShapedRecipeBuilder clubBuilder() {
        return digToolBuilder(GT6Tools.CLUB.get(), new String[] {"PPP", "PPP", "hSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The axe row — the upstream :342 AXE head (3 plates) + the rod. */
    private ShapedRecipeBuilder axeBuilder() {
        return digToolBuilder(GT6Tools.AXE.get(), new String[] {"PP ", "PS ", "hSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The double-axe row — the upstream :343 DOUBLE_AXE heavy head (5 plates). */
    private ShapedRecipeBuilder axeDoubleBuilder() {
        return digToolBuilder(GT6Tools.AXE_DOUBLE.get(), new String[] {"PP ", "PPS", "hSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The hand drill row — the arrow head + 2 bolts (the :152 row, the worn file). */
    private ShapedRecipeBuilder handDrillBuilder() {
        return digToolBuilder(GT6Tools.HAND_DRILL.get(), new String[] {"P  ", " S ", "hSf"},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    // ------------------------------------------------------------------
    // task w5-t5-scene-six — the six scene-tool rows. The OreProcessing_Tool
    // uppercase alphabet (Loader_Tools.java:308-318 comment): I=ingot P=plate
    // T=screw O=ring S=stick G=gem C=plateGem R=stone; the lowercase letters are
    // the CR.java:339-361 tool keys. Single-steel-tier convergence folds the
    // material loops onto the steel plates/screw/ring items (the t7 pocket
    // letter-by-letter precedent).

    /**
     * The scissors row — the upstream {"PfP"," T ","OdO"} pattern (Loader_Tools.java:326,
     * the plateGem variant "CfC" CUT with the gem-plate system): 2 steel plates + the
     * file tool + the steel screw (the 'T' centre) + 2 steel rings + the screwdriver tool.
     * The screw/ring ride the BARE GTMaterialItems items (the ring_steel bare-item
     * precedent, the tank-valve rows); the two tools pay one point per craft through
     * their crafting-remaining faces.
     */
    private ShapedRecipeBuilder scissorsBuilder() {
        return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.SCISSORS.get())
                .pattern("PfP").pattern(" T ").pattern("OdO")
                .define('P', DIG_STEEL_PLATES)
                .define('f', GT6ItemTags.TOOLS_FILE)
                .define('T', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.screw, gregapi.data.MT.Steel).get())
                .define('O', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.ring, gregapi.data.MT.Steel).get())
                .define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
                .unlockedBy("has_steel_plate", has(DIG_STEEL_PLATES));
    }

    /**
     * The scoop row — the upstream {"SVS","SSS","xSh"} pattern verbatim (Loader_Tools.java:320
     * with V = the special auxiliary wool, S = stick): 6 rods frame the wool net ('V' =
     * {@code ItemTags.WOOL}, the auxiliary identity kept; the vanilla tag rides both legs — the 21.1 neoforge Tags.Items has no WOOL field) + the wire cutter + the hammer
     * tools (the 'x'/'h' letters, the CR.java:359/:346 alphabet).
     */
    private ShapedRecipeBuilder scoopBuilder() {
        return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.SCOOP.get())
                .pattern("SVS").pattern("SSS").pattern("xSh")
                .define('S', Tags.Items.RODS_WOODEN)
                .define('V', ItemTags.WOOL)
                .define('x', GT6ItemTags.TOOLS_WIRE_CUTTER)
                .define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
                // the key sorts BEFORE "has_the_recipe": the canonical criteria/requirements
                // order must stay insertion==alphabetical or the datagen_tree_check
                // requirements normalizer cannot bridge the 1.21 leg ("has_wool" > "has_the_recipe").
                .unlockedBy("has_fleece", has(ItemTags.WOOL));
    }

    /**
     * The plunger row — the upstream {"xVV"," SV","S f"} pattern (Loader_Tools.java:315
     * with V = the special auxiliary rubber plate): the wire cutter + 2 rubber plates +
     * 2 rods + the file tool. CALLER GUARDS the null rubber plate (the
     * CR.ONLY_IF_HAS_RESULT face — no registered plate item, no row).
     */
    private ShapedRecipeBuilder plungerBuilder(net.minecraft.world.item.Item aRubberPlate) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.PLUNGER.get())
                .pattern("xVV").pattern(" SV").pattern("S f")
                .define('x', GT6ItemTags.TOOLS_WIRE_CUTTER)
                .define('V', aRubberPlate)
                .define('S', Tags.Items.RODS_WOODEN)
                .define('f', GT6ItemTags.TOOLS_FILE)
                .unlockedBy("has_rubber_plate", has(aRubberPlate));
    }

    /**
     * The flint-and-tinder SELF-RECAST row — the upstream :207 Steel shapeless row
     * verbatim: flint_and_steel → gt6:flint_and_tinder (one-way; the :208 reverse row
     * below is the counterpart).
     */
    private ShapelessRecipeBuilder flintAndTinderFromFlintAndSteelBuilder() {
        return ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, GT6Tools.FLINT_AND_TINDER.get())
                .requires(Items.FLINT_AND_STEEL)
                .unlockedBy("has_flint_and_steel", has(Items.FLINT_AND_STEEL));
    }

    /**
     * The flint-and-steel reverse row — the upstream :208 {"T "," F"} Steel row
     * (flint + steel NUGGET → vanilla flint_and_steel). DECLARED deviation: the upstream
     * {@code CR.DEL_OTHER_NATIVE_RECIPES} deletes the vanilla iron-ingot row, the port
     * datagen cannot delete — the two rows coexist (the vanilla iron route stays live).
     */
    private ShapedRecipeBuilder flintAndSteelBuilder() {
        TagKey<Item> tSteelNuggets = gregtech6.datagen.GT6ItemTags.materialTag(GT6ItemTags.NUGGETS_FAMILY, "steel");
        return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.FLINT_AND_STEEL)
                .pattern("T ").pattern(" F")
                .define('T', tSteelNuggets)
                .define('F', Items.FLINT)
                .unlockedBy("has_flint", has(Items.FLINT));
    }

    /**
     * The rolling-pin row — the upstream wood route (Loader_Recipes_Woods.java:237,
     * {"  S"," P ","S f"}): planks + 2 rods + the file tool (the 'P' letter folds to the
     * vanilla planks tag — the upstream MT.Wood I/P special case; the :238 knife variant
     * and the :250-253 metal/plastic ladder are the pool cuts).
     */
    private ShapedRecipeBuilder rollingPinBuilder() {
        return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.ROLLING_PIN.get())
                .pattern("  S").pattern(" P ").pattern("S f")
                .define('S', Tags.Items.RODS_WOODEN)
                .define('P', ItemTags.PLANKS)
                .define('f', GT6ItemTags.TOOLS_FILE)
                .unlockedBy("has_planks", has(ItemTags.PLANKS));
    }

    /**
     * The LARGE bending-cylinder row — the upstream {"sfh","III","III"} self-craft row
     * (Loader_Tools.java:312, the Small :313 with ONE more ingot row — the 6*U amount
     * made literal): saw+file+hammer worn, 6 generic ingots (the
     * bendingCylinderSmallBuilder shape, one ingot row longer).
     */
    private ShapedRecipeBuilder bendingCylinderBuilder() {
        return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.BENDING_CYLINDER.get())
                .pattern("sfh").pattern("III").pattern("III")
                .define('s', GT6ItemTags.TOOLS_SAW)
                .define('f', GT6ItemTags.TOOLS_FILE)
                .define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
                .define('I', Tags.Items.INGOTS)
                .unlockedBy("has_ingot", has(Tags.Items.INGOTS));
    }

    // -------------------------------------------------------------------------
    // task w5-t6-electric-nineteen — the fifteen electric-tool crafting rows
    // (the upstream OreProcessing_Tool rows Loader_Tools.java:356-377 converged to the
    // single steel tier; the VANILLA pattern strings byte-kept, the keys remapped):
    //   'A' = the steel tool head item (the per-material head loop folds to steel);
    //   'S'/'T' = stick/screw of Steel (the tool-head material columns);
    //   'X' = plateCurved(Electric_T[i]);
    //   'Y' = ring (plate on the buzzsaw, spring on the jackhammer) of the tier;
    //   'Z' = plate (stick_long on the trimmer) of the tier;
    //   'V' = #gt6:re_battery<i> (the W4 tag seam — ANY battery of the tier, the oredict
    //         semantics; the tool CAPACITY pins the lead-acid representative regardless);
    //   'W' = the MOTOR/PISTON component ruling (the card open question, declared):
    //         IL.MOTORS[i] → stick(Electric_T[i]), IL.PISTONS[i] → gear_gt_small(
    //         Electric_T[i]) — the motor/piston ITEM families do not exist in this
    //         universe, the Electric_T-machined parts are the closest live equivalent;
    //   'd'/'f'/'h' = the crafting tool tags (the in-grid tool wear channel).
    // The lv batch: MixerLV/DrillLV/ScrewdriverLV/BuzzsawLV/TrimmerLV/WrenchLV/
    // MiningDrillLV/ChainsawLV (:357-365); the mv trio (:368-370); the hv trio + the
    // JackHammer (:373-377). The monkey-wrench and no-ores forms have NO crafting row
    // upstream — they exist only through the sneak swap.
    // -------------------------------------------------------------------------

    /** One staged electric row: the shared builder + the row id its save face ids from. */
    record ElectricToolRow(ShapedRecipeBuilder builder, ResourceLocation id) {}

    /** The electric row id: {@code electric_tool/<path>} (the battery/&lt;path&gt; convention). */
    public static ResourceLocation electricToolRecipeId(String aPath) {
        return new ResourceLocation(GT6DataGenerators.MOD_ID, "electric_tool/" + aPath);
    }

    /** The Electric_T[i] material (tier 1..3 = SteelGalvanized/Al/StainlessSteel, upstream MT.java:3691). */
    private static gregapi.oredict.OreDictMaterial electricMaterial(int aTier) {
        return gregtech6.registry.GTMachines.ELECTRIC_T_LADDER.get(aTier - 1).get();
    }

    /** The tier-tagged battery column ('V') — #gt6:re_battery&lt;i&gt; (the W4 seam). */
    private static TagKey<Item> batteryTag(int aTier) {
        return GT6ItemTags.gt6("re_battery" + aTier);
    }

    /** The fifteen rows, the upstream registration order :356-377. */
    public java.util.List<ElectricToolRow> electricToolRows() {
        java.util.List<ElectricToolRow> rRows = new ArrayList<>();
        gregtech6.items.tools.electric.GT6ElectricToolItem.Spec[] tSpecs = {
                gregtech6.items.tools.electric.GT6ElectricToolItem.HAND_MIXER_LV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.HAND_DRILL_LV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.SCREWDRIVER_LV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.BUZZSAW_LV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.TRIMMER_LV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.WRENCH_LV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.MINING_DRILL_LV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.CHAINSAW_LV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.WRENCH_MV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.MINING_DRILL_MV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.CHAINSAW_MV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.WRENCH_HV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.MINING_DRILL_HV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.CHAINSAW_HV,
                gregtech6.items.tools.electric.GT6ElectricToolItem.JACKHAMMER_HV_NORMAL};
        for (gregtech6.items.tools.electric.GT6ElectricToolItem.Spec tSpec : tSpecs) {
            rRows.add(new ElectricToolRow(electricToolBuilder(tSpec), electricToolRecipeId(tSpec.aPath())));
        }
        return rRows;
    }

    /**
     * One row's builder — the pattern/key columns live in the two upstream tables
     * (the shape strings :357-377 and the OreProcessing_Tool ctor args per row).
     */
    private ShapedRecipeBuilder electricToolBuilder(gregtech6.items.tools.electric.GT6ElectricToolItem.Spec aSpec) {
        String tPath = aSpec.aPath();
        int tTier = aSpec.aTier();
        gregapi.oredict.OreDictMaterial tTierMat = electricMaterial(tTier);
        gregapi.oredict.OreDictMaterial tSteel = gregapi.data.MT.Steel;
        // the shape string per row (the :357-377 byte-forms); the wrench/miningdrill/
        // chainsaw ladder rows share the {"dAT","XWX","XVX"} shape.
        String[] tPattern = switch (tPath) {
            case "hand_mixer_lv" -> new String[] {"SSY", "SXW", "hVZ"};
            case "hand_drill_lv" -> new String[] {"fSY", "TXW", "dVZ"};
            case "screwdriver_lv" -> new String[] {"XdA", "TWY", "VYX"};
            case "buzzsaw_lv" -> new String[] {"YXV", "TWX", "AdY"};
            case "trimmer_lv" -> new String[] {"XAT", "ZYA", "VWd"};
            case "jackhammer_hv_normal" -> new String[] {"SVS", "XWX", "YSY"};
            default -> new String[] {"dAT", "XWX", "XVX"};
        };
        // the head prefix per row (the listener column: the mixer/drill/jackhammer rows
        // ride toolHeadDrill upstream, :357-358/:364/:377)
        gregapi.oredict.OreDictPrefix tHead = switch (tPath) {
            case "screwdriver_lv" -> gregapi.data.OP.toolHeadScrewdriver;
            case "buzzsaw_lv" -> gregapi.data.OP.toolHeadBuzzSaw;
            case "trimmer_lv" -> gregapi.data.OP.toolHeadSword; // 2x toolHeadSword (the :174 material amount)
            case "chainsaw_lv", "chainsaw_mv", "chainsaw_hv" -> gregapi.data.OP.toolHeadChainsaw;
            case "wrench_lv", "wrench_mv", "wrench_hv" -> gregapi.data.OP.toolHeadWrench;
            default -> gregapi.data.OP.toolHeadDrill;
        };
        // the 'Y' column: ring on the lv batch, plate on the buzzsaw (:360), spring on the
        // jackhammer (:377); the 'Z' column: plate (mixer/drill), stick_long (trimmer :361);
        // the 'W' column: the motor equivalent (stick) except the pistons rows
        // (trimmer/jackhammer = the gear equivalent).
        gregapi.oredict.OreDictPrefix tY = switch (tPath) {
            case "buzzsaw_lv" -> gregapi.data.OP.plate;
            case "jackhammer_hv_normal" -> gregapi.data.OP.spring;
            default -> gregapi.data.OP.ring;
        };
        gregapi.oredict.OreDictPrefix tZ = tPath.equals("trimmer_lv") ? gregapi.data.OP.stickLong : gregapi.data.OP.plate;
        gregapi.oredict.OreDictPrefix tW = tPath.equals("trimmer_lv") || tPath.equals("jackhammer_hv_normal")
                ? gregapi.data.OP.gearGtSmall : gregapi.data.OP.stick;
        String tKeys = tPattern[0] + tPattern[1] + tPattern[2];
        ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder
                .shaped(RecipeCategory.TOOLS, GT6Tools.electricTool(tPath).get());
        for (String tRow : tPattern) tBuilder.pattern(tRow);
        // define ONLY the keys the shape actually uses — ShapedRecipeBuilder throws on a
        // defined-but-unused ingredient (the hand_mixer row carries no head/screw key)
        if (tKeys.indexOf('A') >= 0) tBuilder.define('A', GTMaterialItems.get(tHead, tSteel).get());
        if (tKeys.indexOf('S') >= 0) tBuilder.define('S', GTMaterialItems.get(gregapi.data.OP.stick, tSteel).get());
        if (tKeys.indexOf('T') >= 0) tBuilder.define('T', GTMaterialItems.get(gregapi.data.OP.screw, tSteel).get());
        if (tKeys.indexOf('X') >= 0) tBuilder.define('X', GTMaterialItems.get(gregapi.data.OP.plateCurved, tTierMat).get());
        if (tKeys.indexOf('V') >= 0) tBuilder.define('V', batteryTag(tTier));
        if (tKeys.indexOf('W') >= 0) tBuilder.define('W', GTMaterialItems.get(tW, tTierMat).get());
        if (tKeys.indexOf('Y') >= 0) tBuilder.define('Y', GTMaterialItems.get(tY, tTierMat).get());
        if (tKeys.indexOf('Z') >= 0) tBuilder.define('Z', GTMaterialItems.get(tZ, tTierMat).get());
        if (tKeys.indexOf('d') >= 0) tBuilder.define('d', GT6ItemTags.TOOLS_SCREWDRIVER);
        if (tKeys.indexOf('f') >= 0) tBuilder.define('f', GT6ItemTags.TOOLS_FILE);
        if (tKeys.indexOf('h') >= 0) tBuilder.define('h', GT6ItemTags.TOOLS_HARD_HAMMER);
        return tBuilder.unlockedBy("has_battery", has(batteryTag(tTier)));
    }

    // ─── the Hazmat armor band (task w5-t8-armor-24, tail-append) ───

    /**
     * The id of one armor recipe — the result-path convention ({@code gt6:hazmat_<suit>_<piece>}).
     * The path rides a local so the two-arg RL ctor args stay bare identifiers (the
     * swap-table regex note).
     */
    public static ResourceLocation armorRecipeId(GT6ArmorMaterials.SuitRow aSuit, int aSlot) {
        String tPath = aSuit.pieceId(aSlot);
        return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
    }

    /**
     * The 24 armor rows (Loader_Tools.java:68-96 verbatim grids, tool letters dropped).
     * Base suits: the helmet mask carries the glass pane 'G' (black stained for the
     * insect/frost/heat rows :68/:73/:78, plain for radiation/biochemgas :83/:88); the
     * material column 'M' per suit = rubber FOIL (:68), asbestos PLATE (:73), aluminium
     * FOIL (:78), lead PLATE (:83), rubber PLATE (:88) — the ANY.Rubber rows flatten to
     * MT.Rubber (the upstream ANY.Rubber group holds exactly that member,
     * ANY.java:144). UNIVERSAL (:93-96): one 3x3 over the five suits' SAME-SLOT piece +
     * the vanilla chainmail piece 'F'.
     *
     * <p>DECLARED DEVIATION: the upstream lowercase tool letters ('q' scissors, 'l'
     * magnifying glass, 'x' wire cutter — the CR.java:193-216 alphabet) are DROPPED, the
     * cell alignment kept by spaces. The scissors/magnifying-glass items do not exist on
     * this baseline (the W5 t3/t5 tool cards land later in the merge queue, the cutter
     * alone is ported), and a half-faithful grid mixing present/absent tools would drift
     * per suit; the recipe-precision rework is a tool-wave follow-up pool item.
     */
    private ShapedRecipeBuilder armorPieceBuilder(GT6ArmorMaterials.SuitRow aSuit, int aSlot) {
        Item tResult = GT6Tools.armorRow(aSuit, aSlot).get();
        if (aSuit.suit() == GT6ArmorMaterials.UNIVERSAL) {
            Item tGas = GT6Tools.armorRow(GT6ArmorMaterials.rowOf(GT6ArmorMaterials.BIOCHEMGAS), aSlot).get();
            Item tInsect = GT6Tools.armorRow(GT6ArmorMaterials.rowOf(GT6ArmorMaterials.INSECTS), aSlot).get();
            Item tFrost = GT6Tools.armorRow(GT6ArmorMaterials.rowOf(GT6ArmorMaterials.FROST), aSlot).get();
            Item tHeat = GT6Tools.armorRow(GT6ArmorMaterials.rowOf(GT6ArmorMaterials.HEAT), aSlot).get();
            Item tRadiation = GT6Tools.armorRow(GT6ArmorMaterials.rowOf(GT6ArmorMaterials.RADIATION), aSlot).get();
            Item tChainmail = new Item[] { net.minecraft.world.item.Items.CHAINMAIL_HELMET,
                    net.minecraft.world.item.Items.CHAINMAIL_CHESTPLATE,
                    net.minecraft.world.item.Items.CHAINMAIL_LEGGINGS,
                    net.minecraft.world.item.Items.CHAINMAIL_BOOTS }[aSlot];
            return ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, tResult)
                    .pattern("A B").pattern("C D").pattern("E F")
                    .define('A', tGas).define('B', tInsect).define('C', tFrost)
                    .define('D', tHeat).define('E', tRadiation).define('F', tChainmail)
                    .unlockedBy("has_hazmat_piece", has(tGas));
        }
        Item tMaterial = suitMaterial(aSuit);
        String[][] tPattern = new String[][] {
                {"MMM", "MGM"},         // helmet mask (the 'G' pane row, :68/:73/:78/:83/:88)
                {"M M", "MMM", "MMM"},  // chest (:69/:74/:79/:84/:89)
                {"MMM", "M M", "M M"},  // legs (:70/:75/:80/:85/:90)
                {"M M", "M M"}};        // boots (:71/:76/:81/:86/:91)
        ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, tResult);
        boolean tHasGlass = false;
        for (String tRow : tPattern[aSlot]) {
            tBuilder = tBuilder.pattern(tRow);
            tHasGlass |= tRow.indexOf('G') >= 0;
        }
        tBuilder = tBuilder.define('M', tMaterial);
        if (tHasGlass) tBuilder = tBuilder.define('G', suitGlass(aSuit));
        return tBuilder.unlockedBy("has_material", has(tMaterial));
    }

    /** The 'M' column per base suit — the Loader_Tools.java:68-91 material rows. */
    private Item suitMaterial(GT6ArmorMaterials.SuitRow aSuit) {
        return switch (aSuit.suit()) {
            case INSECTS -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.foil, gregapi.data.MT.Rubber).get();
            case FROST -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plate, gregapi.data.MT.Asbestos).get();
            case HEAT -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.foil, gregapi.data.MT.Al).get();
            case RADIATION -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plate, gregapi.data.MT.Pb).get();
            case BIOCHEMGAS -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plate, gregapi.data.MT.Rubber).get();
            default -> throw new IllegalArgumentException("base suit only: " + aSuit.suit());
        };
    }

    /** The 'G' pane column — black stained on the :68/:73/:78 rows, plain on :83/:88. */
    private Item suitGlass(GT6ArmorMaterials.SuitRow aSuit) {
        return switch (aSuit.suit()) {
            case INSECTS, FROST, HEAT -> net.minecraft.world.item.Items.BLACK_STAINED_GLASS_PANE;
            case RADIATION, BIOCHEMGAS -> net.minecraft.world.item.Items.GLASS_PANE;
            default -> throw new IllegalArgumentException("base suit only: " + aSuit.suit());
        };
    }

	// ------------------------------------------------------------------------
	// The dig ladder material rows (task dig-ladder) — the upstream
	// OreProcessing_Tool shapes on the toolHead prefixes (Loader_Tools.java:293-300,
	// the And(ANTIMATTER.NOT, MT.Wood.NOT, COATED.NOT) axis), ONE gt6:material_tool
	// row per (dig form x plate+ingot material). The plain steel anchors above are the
	// identity-less steel arm; every other axis material gets its own stamped row (the
	// upstream second C-variant rows ride the head-row completion band below — the
	// 39-toolhead-rows card; the blade ladder kept C where its plateGem item truth
	// exists).
	// NOTE (39-toolhead-rows): the spade row's lowercase 's' cell is the CR.java:211
	// SAW tool letter (the decisions.r9-toolhead-s-letter ruling) — #39 had emitted
	// the wooden-rod tag there; the dig walk carries the reform.

	/**
	 * M5 unification (task machine-ladder): the upstream {@code MT.Wood.NOT} axis
	 * gate is the MATERIAL IDENTITY test — {@code OreDictMaterial.isTrue} is
	 * {@code aObject == this} (OreDictMaterial.java:1507-1509) — so every ladder walk
	 * (dig/blade/machine) excludes exactly MT.Wood through this ONE expression. The
	 * upstream tag-level {@code WOOD} conditions (the soft hammer's {@code Or(WOOD,…)}
	 * row, Loader_Tools.java:328) are a DIFFERENT thing — the TD.Properties.WOOD tag
	 * test — and stay tag tests. IronWood (WOOD-tagged, ≠ MT.Wood) therefore gets rows
	 * upstream-true; the wood() helper family is excluded by the item-truth gates (no
	 * ingots) and COATED.NOT, not by this gate.
	 */
	private static boolean woodExcluded(gregapi.oredict.OreDictMaterial aMaterial) {
		return aMaterial == MT.Wood;
	}

	/** One ladder form: the id prefix + the upstream P/I row shape (Loader_Tools.java:294-300 verbatim). */
	static final String[][] DIG_LADDER_FORMS = { // package-private: the GT6ToolHeadAssemblyDatagenTest walk face
			{"pickaxe", "PII", "f h"},
			{"pickaxe_construction", "PIP", "f h"},
			{"shovel", "fPh"},
			{"spade", "fPh", " s "},
			{"hoe", "PIh", "f  "},
			{"axe", "PIh", "P  ", "f  "},
	};

	/** The upstream axis filter ∩ the port's plate+ingot item truth; Steel excluded (the anchors above own it). */
	static java.util.List<gregapi.oredict.OreDictMaterial> digLadderMaterials() { // package-private: the row-count pin walk
		java.util.List<gregapi.oredict.OreDictMaterial> rMaterials = new ArrayList<>();
		java.util.Set<gregapi.oredict.OreDictMaterial> tIngots = new java.util.HashSet<>();
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() == gregapi.data.OP.ingot) tIngots.add(tPair.material());
		}
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder()) {
			gregapi.oredict.OreDictMaterial tMaterial = tPair.material();
			if (tPair.prefix() != gregapi.data.OP.plate || !tIngots.contains(tMaterial) || tMaterial == gregapi.data.MT.Steel) continue;
			if (woodExcluded(tMaterial)) continue; // MT.Wood.NOT (the M5 identity form)
			if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
			if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
			rMaterials.add(tMaterial);
		}
		return rMaterials;
	}

	/**
	 * The head prefix a dig form's row emits (the upstream tStack face — the LISTENING
	 * prefix, Loader_Tools.java:451 {@code tStack = aEvent.mPrefix.mat(aEvent.mMaterial, 1)}
	 * + the :514-516 emission): pickaxe :295, construction :294, shovel :296, spade :297,
	 * hoe :299, axe :300 — each row's output is the TOOL HEAD, never the tool (the
	 * #39 inversion fix; the tool itself rides the head+handle assembly band below and
	 * the steel anchors).
	 */
	static gregapi.oredict.OreDictPrefix digLadderHeadPrefix(String aForm) { // package-private: the test prefix ledger
		return switch (aForm) {
			case "pickaxe" -> gregapi.data.OP.toolHeadPickaxe; // :295
			case "pickaxe_construction" -> gregapi.data.OP.toolHeadConstructionPickaxe; // :294
			case "shovel" -> gregapi.data.OP.toolHeadShovel; // :296
			case "spade" -> gregapi.data.OP.toolHeadSpade; // :297
			case "hoe" -> gregapi.data.OP.toolHeadHoe; // :299
			case "axe" -> gregapi.data.OP.toolHeadAxe; // :300
			default -> throw new IllegalArgumentException("unknown dig ladder form: " + aForm);
		};
	}

	/** The head item a dig form's row emits — null = the head item miss (no row, never an unresolvable result). */
	static net.minecraft.world.item.Item digLadderHeadItem(String aForm, gregapi.oredict.OreDictMaterial aMaterial) {
		return gregtech6.registry.GTMaterialItems.get(digLadderHeadPrefix(aForm), aMaterial) == null ? null
				: gregtech6.registry.GTMaterialItems.get(digLadderHeadPrefix(aForm), aMaterial).get();
	}

	/** One assembled head-row body: the pattern/key maps → the vanilla shaped builder (the heads need NO identity stamp — the item IS the identity). */
	private static net.minecraft.data.recipes.ShapedRecipeBuilder headRowBuilder(net.minecraft.world.item.Item aHead,
			java.util.List<String> aPattern, java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> aKey) {
		net.minecraft.data.recipes.ShapedRecipeBuilder rBuilder = net.minecraft.data.recipes.ShapedRecipeBuilder.shaped(net.minecraft.data.recipes.RecipeCategory.TOOLS, aHead);
		for (String tRow : aPattern) rBuilder.pattern(tRow);
		for (java.util.Map.Entry<Character, net.minecraft.world.item.crafting.Ingredient> tEntry : aKey.entrySet()) rBuilder.define(tEntry.getKey(), tEntry.getValue());
		return rBuilder;
	}

	/**
	 * The row letters (the upstream OreProcessing_Tool alphabet over the P/I variant).
	 * 's' = the CR.java:211 lowercase SAW tool letter — the decisions.r9-toolhead-s-letter
	 * ruling (#39 had misread it as the wooden rod; the tool-damage slot is a saw tag).
	 */
	private static TagKey<Item> digLadderIngredient(char aKey, gregapi.oredict.OreDictMaterial aMaterial) {
		String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(aMaterial.mNameInternal);
		return switch (aKey) {
			case 'P' -> GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake);
			case 'I' -> GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, tSnake);
			case 'h' -> GT6ItemTags.TOOLS_HARD_HAMMER;
			case 'f' -> GT6ItemTags.TOOLS_FILE;
			case 's' -> GT6ItemTags.TOOLS_SAW;
			default -> throw new IllegalArgumentException("unknown dig ladder letter: " + aKey);
		};
	}

	/** The row id (the result-path convention with the material leaf). */
	private static ResourceLocation digLadderRowId(String aForm, String aSnake) {
		String tPath = aForm + "/" + aSnake;
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	// -------------------------------------------------------------------------
	// task debt-emitter-sensor-generators — the three technological component families'
	// self-crafting rows, MultiItemTechnological.java:425-434 (FIELD_GENERATORS
	// "WPW","CGC","WPW"), :436-445 (EMITTERS "SPC","WQP","CWS") and :447-456 (SENSORS
	// "P Q","PS ","CPP"), grids and keys VERBATIM over the column ladders:
	//   - the 'C' circuit column = OD_CIRCUITS[i] → the #gt6:circuit<i> TAG (the battery
	//     'X' precedent, CS.java:166);
	//   - the plate columns ride Electric_T[i] (MT.java:3691 = TinAlloy/SteelGalvanized/
	//     Al/StainlessSteel/Cr/Ti/Ir/Os/Trinitanium/Trinaquadalloy) — plateDouble on the
	//     generators, plateCurved on emitters/sensors;
	//   - the gem columns = OP.gem.dat(...) → #forge:gems/<snake> tags (the crusher 'D'
	 //    precedent): FG EnderPearl×2/EnderEye×2/NetherStar×6, EM+SN SiO2×3/Emerald/
	//     EnderPearl/EnderEye/NetherStar×4 (ANY.SiO2 → quartz);
	//   - the wire columns ride the MT.DATA.WIRES_01/WIRES_04/CABLES_01 material ladders
	//     (MT.java:3595/:3613/:3631 = Pb/Sn/Cu/Au/Al/Pt then Graphene) and the generator
	//     'W' Osmium wire gauges (fine/01/02/04/06/08/10/12/14/16 — :425-434) over the
	//     GTWires item paths (the wireItemByPath walk); CABLES_01 folds Graphene to the
	//     bare wireGt01 (upstream mixes cableGt01 metals with wireGt01 Graphene, :3638).
	// POUR MAP (the live-row set, the laser-gas upstream-FL.exists posture): the port
	// carries circuit tags 0-6 ONLY (no #gt6:circuit7..9), so every family's rungs
	// 7-9 (ZPM/UV/PUV1) are CUT — pooled until the circuit ladder grows; the
	// FIELD_GENERATORS ULV rung is CUT — OP.wireFine carries no port item/tag face at
	// all (the coil 'W' fold note). Live: FIELD_GENERATORS 1-6, EMITTERS 0-6,
	// SENSORS 0-6 = 20 rows.
	// -------------------------------------------------------------------------

	/** The WIRES_01/WIRES_04 material ladder as GTWireSpecs tokens (MT.java:3595/:3613, index = tier). */
	private static final String[] COMPONENT_WIRE_TOKENS = {"lead", "tin", "copper", "gold", "aluminium", "platinum", "graphene", "graphene", "graphene", "graphene"};

	/** The FIELD_GENERATORS gem column per tier (:425-434 — EnderPearl×2/EnderEye×2/NetherStar×6). */
	private static final String[] FIELD_GENERATOR_GEMS = {"ender_pearl", "ender_pearl", "ender_eye", "ender_eye", "nether_star", "nether_star", "nether_star", "nether_star", "nether_star", "nether_star"};

	/** The EMITTERS/SENSORS gem column per tier (:436-456 — SiO2×3/Emerald/EnderPearl/EnderEye/NetherStar×4). */
	private static final String[] EMITTER_SENSOR_GEMS = {"quartz", "quartz", "quartz", "emerald", "ender_pearl", "ender_eye", "nether_star", "nether_star", "nether_star", "nether_star"};

	/** The Electric_T[i] ladder (MT.java:3691, index = tier). */
	private static gregapi.oredict.OreDictMaterial[] componentElectricT() {
		return new gregapi.oredict.OreDictMaterial[]{gregapi.data.MT.TinAlloy, gregapi.data.MT.SteelGalvanized,
				gregapi.data.MT.Al, gregapi.data.MT.StainlessSteel, gregapi.data.MT.Cr, gregapi.data.MT.Ti,
				gregapi.data.MT.Ir, gregapi.data.MT.Os, gregapi.data.MT.Trinitanium, gregapi.data.MT.Trinaquadalloy};
	}

	/** The FIELD_GENERATORS 'W' Osmium wire gauges per tier (:425-434; index 0 = the unported fine wire). */
	private static final int[] FIELD_GENERATOR_OS_GAUGES = {1, 2, 4, 6, 8, 10, 12, 14, 16};

	/** The wire item path of (form, token, gauge) — the GTWireSpecs.registryName composition, zero-padded. */
	private static String componentWirePath(boolean aInsulated, String aToken, int aGauge) {
		return (aInsulated ? "cable_" : "wire_") + aToken + "_gt" + (aGauge < 10 ? "0" : "") + aGauge;
	}

//? if forge {
	private void compactComponentRows(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		gregapi.oredict.OreDictMaterial[] tElectricT = componentElectricT();
		for (int i = 1; i <= 6; i++) { // FIELD_GENERATORS :425-434, live rungs 1-6 (see the band doc)
			Item tWire = wireItemByPath(componentWirePath(false, "osmium_elemental", FIELD_GENERATOR_OS_GAUGES[i - 1]));
			Item tPlate = itemOrNull(gregapi.data.OP.plateDouble, tElectricT[i]);
			Item tResult = gregtech6.items.GT6Emitters.ITEMS_BY_PATH.get(gregtech6.items.GT6Emitters.FAMILY_FIELD_GENERATORS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]).get();
			TagKey<Item> tGem = GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, FIELD_GENERATOR_GEMS[i]);
			TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit" + i);
			if (tPlate == null) continue; // the silent-skip guard (the laser-gas FL.exists posture)
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tResult)
					.pattern("WPW").pattern("CGC").pattern("WPW")
					.define('W', tWire).define('P', tPlate).define('C', tCircuit).define('G', tGem)
					.unlockedBy("has_circuit" + i, has(tCircuit))
					.save(aConsumer, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/" + gregtech6.items.GT6Emitters.FAMILY_FIELD_GENERATORS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]));
		}
		for (int i = 0; i <= 6; i++) { // EMITTERS :436-445, live rungs 0-6
			Item tWire04 = wireItemByPath(componentWirePath(false, COMPONENT_WIRE_TOKENS[i], 4));
			Item tCable01 = wireItemByPath(i <= 5 ? componentWirePath(true, COMPONENT_WIRE_TOKENS[i], 1) : componentWirePath(false, "graphene", 1)); // CABLES_01 Graphene fold (:3638)
			Item tPlate = itemOrNull(gregapi.data.OP.plateCurved, tElectricT[i]);
			Item tResult = gregtech6.items.GT6Emitters.ITEMS_BY_PATH.get(gregtech6.items.GT6Emitters.FAMILY_EMITTERS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]).get();
			TagKey<Item> tGem = GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, EMITTER_SENSOR_GEMS[i]);
			TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit" + i);
			if (tPlate == null) continue; // the silent-skip guard
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tResult)
					.pattern("SPC").pattern("WQP").pattern("CWS")
					.define('S', tWire04).define('P', tPlate).define('C', tCircuit).define('Q', tGem).define('W', tCable01)
					.unlockedBy("has_circuit" + i, has(tCircuit))
					.save(aConsumer, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/" + gregtech6.items.GT6Emitters.FAMILY_EMITTERS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]));
		}
		for (int i = 0; i <= 6; i++) { // SENSORS :447-456, live rungs 0-6
			Item tWire01 = wireItemByPath(componentWirePath(false, COMPONENT_WIRE_TOKENS[i], 1));
			Item tPlate = itemOrNull(gregapi.data.OP.plateCurved, tElectricT[i]);
			Item tResult = gregtech6.items.GT6Emitters.ITEMS_BY_PATH.get(gregtech6.items.GT6Emitters.FAMILY_SENSORS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]).get();
			TagKey<Item> tGem = GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, EMITTER_SENSOR_GEMS[i]);
			TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit" + i);
			if (tPlate == null) continue; // the silent-skip guard
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tResult)
					.pattern("P Q").pattern("PS ").pattern("CPP")
					.define('Q', tGem).define('S', tWire01).define('P', tPlate).define('C', tCircuit)
					.unlockedBy("has_circuit" + i, has(tCircuit))
					.save(aConsumer, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/" + gregtech6.items.GT6Emitters.FAMILY_SENSORS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]));
		}
	}
	//?} else {
	/*private void compactComponentRows(net.minecraft.data.recipes.RecipeOutput aOutput) {
		gregapi.oredict.OreDictMaterial[] tElectricT = componentElectricT();
		for (int i = 1; i <= 6; i++) { // FIELD_GENERATORS :425-434, live rungs 1-6 (see the band doc)
			Item tWire = wireItemByPath(componentWirePath(false, "osmium_elemental", FIELD_GENERATOR_OS_GAUGES[i - 1]));
			Item tPlate = itemOrNull(gregapi.data.OP.plateDouble, tElectricT[i]);
			Item tResult = gregtech6.items.GT6Emitters.ITEMS_BY_PATH.get(gregtech6.items.GT6Emitters.FAMILY_FIELD_GENERATORS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]).get();
			TagKey<Item> tGem = GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, FIELD_GENERATOR_GEMS[i]);
			TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit" + i);
			if (tPlate == null) continue; // the silent-skip guard (the laser-gas FL.exists posture)
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tResult)
					.pattern("WPW").pattern("CGC").pattern("WPW")
					.define('W', tWire).define('P', tPlate).define('C', tCircuit).define('G', tGem)
					.unlockedBy("has_circuit" + i, has(tCircuit))
					.save(aOutput, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/" + gregtech6.items.GT6Emitters.FAMILY_FIELD_GENERATORS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]));
		}
		for (int i = 0; i <= 6; i++) { // EMITTERS :436-445, live rungs 0-6
			Item tWire04 = wireItemByPath(componentWirePath(false, COMPONENT_WIRE_TOKENS[i], 4));
			Item tCable01 = wireItemByPath(i <= 5 ? componentWirePath(true, COMPONENT_WIRE_TOKENS[i], 1) : componentWirePath(false, "graphene", 1)); // CABLES_01 Graphene fold (:3638)
			Item tPlate = itemOrNull(gregapi.data.OP.plateCurved, tElectricT[i]);
			Item tResult = gregtech6.items.GT6Emitters.ITEMS_BY_PATH.get(gregtech6.items.GT6Emitters.FAMILY_EMITTERS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]).get();
			TagKey<Item> tGem = GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, EMITTER_SENSOR_GEMS[i]);
			TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit" + i);
			if (tPlate == null) continue; // the silent-skip guard
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tResult)
					.pattern("SPC").pattern("WQP").pattern("CWS")
					.define('S', tWire04).define('P', tPlate).define('C', tCircuit).define('Q', tGem).define('W', tCable01)
					.unlockedBy("has_circuit" + i, has(tCircuit))
					.save(aOutput, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/" + gregtech6.items.GT6Emitters.FAMILY_EMITTERS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]));
		}
		for (int i = 0; i <= 6; i++) { // SENSORS :447-456, live rungs 0-6
			Item tWire01 = wireItemByPath(componentWirePath(false, COMPONENT_WIRE_TOKENS[i], 1));
			Item tPlate = itemOrNull(gregapi.data.OP.plateCurved, tElectricT[i]);
			Item tResult = gregtech6.items.GT6Emitters.ITEMS_BY_PATH.get(gregtech6.items.GT6Emitters.FAMILY_SENSORS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]).get();
			TagKey<Item> tGem = GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, EMITTER_SENSOR_GEMS[i]);
			TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit" + i);
			if (tPlate == null) continue; // the silent-skip guard
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tResult)
					.pattern("P Q").pattern("PS ").pattern("CPP")
					.define('Q', tGem).define('S', tWire01).define('P', tPlate).define('C', tCircuit)
					.unlockedBy("has_circuit" + i, has(tCircuit))
					.save(aOutput, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/" + gregtech6.items.GT6Emitters.FAMILY_SENSORS + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[i]));
		}
	}
	*///?}

//? if forge {
	private void digLadderRows(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		for (gregapi.oredict.OreDictMaterial tMaterial : digLadderMaterials()) {
			String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(tMaterial.mNameInternal);
			TagKey<Item> tPlate = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake);
			for (String[] tForm : DIG_LADDER_FORMS) {
				net.minecraft.world.item.Item tHead = digLadderHeadItem(tForm[0], tMaterial);
				if (tHead == null) continue; // the head item miss — no row (never an unresolvable result)
				ResourceLocation tId = digLadderRowId(tForm[0], tSnake);
				java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
				java.util.List<String> tPattern = new ArrayList<>();
				for (int i = 1; i < tForm.length; i++) {
					tPattern.add(tForm[i]);
					for (char tChar : tForm[i].toCharArray()) {
						if (tChar != ' ') tKey.put(tChar, net.minecraft.world.item.crafting.Ingredient.of(digLadderIngredient(tChar, tMaterial)));
					}
				}
				// the vanilla shaped emission (NO identity stamp — the head item IS the identity; the
				// vanilla save() writes the same has_the_recipe-OR advancement face the rows above hand-roll)
				headRowBuilder(tHead, tPattern, tKey).unlockedBy("has_" + tSnake, has(tPlate)).save(aConsumer, tId);
			}
		}
	}

	/**
	 * The forge FinishedRecipe face — the vanilla shaped JSON (the vanilla "type" field
	 * rides the default serializeRecipe() over the registered gt6:material_tool
	 * serializer) + the ONE material field the serializer parses.
	 */
	private record MaterialToolRow(ResourceLocation aId, ResourceLocation aAdvancementId,
			net.minecraft.world.item.crafting.CraftingBookCategory aCategory, java.util.List<String> aPattern,
			java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> aKey, net.minecraft.world.item.Item aResult, String aMaterial, float aMultiplier,
			net.minecraft.advancements.Advancement.Builder aAdvancement)
			implements net.minecraft.data.recipes.FinishedRecipe {

		/** The ladder-row face: every grid row carries the ×1.0 multiplier (the dig card's form). */
		private MaterialToolRow(ResourceLocation aId, ResourceLocation aAdvancementId,
				net.minecraft.world.item.crafting.CraftingBookCategory aCategory, java.util.List<String> aPattern,
				java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> aKey, net.minecraft.world.item.Item aResult, String aMaterial,
				net.minecraft.advancements.Advancement.Builder aAdvancement) {
			this(aId, aAdvancementId, aCategory, aPattern, aKey, aResult, aMaterial, 1.0F, aAdvancement);
		}

		@Override
		public void serializeRecipeData(com.google.gson.JsonObject aJson) {
			aJson.addProperty("category", aCategory.getSerializedName());
			com.google.gson.JsonArray tPattern = new com.google.gson.JsonArray();
			for (String tRow : aPattern) {
				tPattern.add(tRow);
			}
			aJson.add("pattern", tPattern);
			com.google.gson.JsonObject tKey = new com.google.gson.JsonObject();
			for (java.util.Map.Entry<Character, net.minecraft.world.item.crafting.Ingredient> tEntry : aKey.entrySet()) {
				tKey.add(String.valueOf(tEntry.getKey()), tEntry.getValue().toJson());
			}
			aJson.add("key", tKey);
			com.google.gson.JsonObject tResult = new com.google.gson.JsonObject();
			tResult.addProperty("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aResult).toString());
			tResult.addProperty("count", 1);
			aJson.add("result", tResult);
			aJson.addProperty("show_notification", true);
			aJson.addProperty("material", aMaterial);
			if (aMultiplier != 1.0F) aJson.addProperty("multiplier", aMultiplier); // the assembly-band non-1.0 forms (the gem pick ×0.25)
		}

		@Override
		public ResourceLocation getId() {
			return aId;
		}

		@Override
		public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() {
			return gregtech6.items.tools.GT6MaterialToolRecipe.Registration.SERIALIZER.get();
		}

		@Override
		@javax.annotation.Nullable
		public com.google.gson.JsonObject serializeAdvancement() {
			return aAdvancement.serializeToJson();
		}

		@Override
		@javax.annotation.Nullable
		public ResourceLocation getAdvancementId() {
			return aAdvancementId;
		}
	}
	//?} else {
	/*private void digLadderRows(net.minecraft.data.recipes.RecipeOutput aOutput) {
		for (gregapi.oredict.OreDictMaterial tMaterial : digLadderMaterials()) {
			String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(tMaterial.mNameInternal);
			TagKey<Item> tPlate = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake);
			for (String[] tForm : DIG_LADDER_FORMS) {
				net.minecraft.world.item.Item tHead = digLadderHeadItem(tForm[0], tMaterial);
				if (tHead == null) continue; // the head item miss — no row (never an unresolvable result)
				ResourceLocation tId = digLadderRowId(tForm[0], tSnake);
				java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
				java.util.List<String> tPattern = new ArrayList<>();
				for (int i = 1; i < tForm.length; i++) {
					tPattern.add(tForm[i]);
					for (char tChar : tForm[i].toCharArray()) {
						if (tChar != ' ') tKey.put(tChar, net.minecraft.world.item.crafting.Ingredient.of(digLadderIngredient(tChar, tMaterial)));
					}
				}
				// the vanilla shaped emission (NO identity stamp — the head item IS the identity)
				headRowBuilder(tHead, tPattern, tKey).unlockedBy("has_" + tSnake, has(tPlate)).save(aOutput, tId);
			}
		}
	}
	*///?}

	// ------------------------------------------------------------------------
	// The blade ladder material rows (task blade-ladder) — the upstream
	// OreProcessing_Tool shapes on the toolHeadSword prefix (Loader_Tools.java:321-323),
	// ONE gt6:material_tool row per (blade form x plate/plateGem material), riding the
	// SAME serializer + MaterialToolRow face as the dig ladder above (the S31-3 seam
	// unification). The steel row is INCLUDED (the blade family retired its plain
	// steel-route anchors — the stamped row IS the steel row now); the C (plateGem)
	// variant is KEPT (the port has the plateGem item truth dig's cut lacked). The
	// axis conditions are the upstream And() gates verbatim: sword :321 (typemin(1),
	// ANTIMATTER/WOOD/COATED excluded), knife :322 (typemin(1), ANTIMATTER/WOOD
	// excluded), butchery :323 (typemin(2) + BOUNCY/STRETCHY excluded) — typemin = the
	// toolHeadSword prefix condition (OP.java:239 .setCondition(typemin(1))). Letters
	// (Loader_OreProcessing.java:550-552): P = plate, C = plateGem, H = the handle
	// stick (the wood-rod tag — the virtual mHandleMaterial port face), h/f = the
	// hammer/file tool letters.

	/** One blade ladder form: the id prefix, the axis gates, the gem flag, the upstream row shape (:321-323 verbatim). */
	private record BladeLadderForm(String aId, int aTypeMin, boolean aNoCoated, boolean aGem, String[] aPattern) {
	}

	private static final BladeLadderForm[] BLADE_LADDER_FORMS = {
			new BladeLadderForm("sword", 1, true, false, new String[] {" P ", "fPh"}),
			new BladeLadderForm("sword_gem", 1, true, true, new String[] {" C ", "fC "}),
			new BladeLadderForm("knife", 1, false, false, new String[] {"fP", "hH"}),
			new BladeLadderForm("knife_gem", 1, false, true, new String[] {"fC", "hH"}),
			new BladeLadderForm("butchery_knife", 2, false, false, new String[] {"fPP", "hPP", "  H"}),
			new BladeLadderForm("butchery_knife_gem", 2, false, true, new String[] {"fCC", " CC", "  H"}),
	};

	/**
	 * The result item of a blade ladder form (the GT6Tools registry face) — the DIRECT
	 * rows only: knife/butchery ride the upstream mToolRecipes (:322/:323, the arg-8 face)
	 * and stay tool rows. The sword rows are NOT here — upstream :321 is an mToolHeadRecipes
	 * row (the arg-9 face) so they emit the head ({@link #bladeLadderHeadItem}), the #39
	 * inversion fix.
	 */
	private static net.minecraft.world.item.Item bladeLadderResult(String aForm) {
		return switch (aForm) {
			case "knife", "knife_gem" -> GT6Tools.KNIFE.get();
			case "butchery_knife", "butchery_knife_gem" -> GT6Tools.BUTCHERY_KNIFE.get();
			default -> null; // the sword forms ride bladeLadderHeadItem
		};
	}

	/**
	 * The head item of the SWORD blade rows (the upstream tStack face — Loader_Tools.java:451
	 * listening prefix + :514-516 emission; the sword row :321 has BOTH the P and the C
	 * variant shapes emitting the SAME {@code OP.toolHeadSword} head). Null = the tool rows
	 * (knife/butchery) and the head item miss.
	 */
	private static net.minecraft.world.item.Item bladeLadderHeadItem(String aForm, gregapi.oredict.OreDictMaterial aMaterial) {
		if (!aForm.startsWith("sword")) return null;
		return gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.toolHeadSword, aMaterial) == null ? null
				: gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.toolHeadSword, aMaterial).get();
	}

	/** The row letters (the upstream OreProcessing_Tool alphabet; 'C' rides the plateGem ITEM — the prefix has no platform-tag family). */
	private static net.minecraft.world.item.crafting.Ingredient bladeLadderIngredient(char aKey, gregapi.oredict.OreDictMaterial aMaterial) {
		String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(aMaterial.mNameInternal);
		return switch (aKey) {
			case 'P' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake));
			case 'C' -> net.minecraft.world.item.crafting.Ingredient.of(gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateGem, aMaterial).get());
			case 'H' -> net.minecraft.world.item.crafting.Ingredient.of(Tags.Items.RODS_WOODEN);
			case 'h' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_HARD_HAMMER);
			case 'f' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_FILE);
			default -> throw new IllegalArgumentException("unknown blade ladder letter: " + aKey);
		};
	}

	/** The per-form axis gate (the upstream And() gates + the typemin prefix condition) ∩ the plate/plateGem item truth. */
	private static boolean bladeLadderAxis(gregapi.oredict.OreDictMaterial aMaterial, int aTypeMin, boolean aNoCoated, boolean aGem) {
		if (aMaterial.mToolTypes < aTypeMin) return false;
		if (aMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) return false;
		if (woodExcluded(aMaterial)) return false; // MT.Wood.NOT — the M5 identity form (was the WOOD-tag test)
		if (aNoCoated && aMaterial.contains(gregapi.data.TD.Compounds.COATED)) return false;
		if (aTypeMin >= 2 && (aMaterial.contains(gregapi.data.TD.Properties.BOUNCY)
				|| aMaterial.contains(gregapi.data.TD.Properties.STRETCHY))) return false;
		return gregtech6.registry.GTMaterialItems.get(aGem ? gregapi.data.OP.plateGem : gregapi.data.OP.plate, aMaterial) != null;
	}

//? if forge {
	private void bladeLadderRows(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		java.util.Set<String> tSeen = new java.util.HashSet<>();
		for (gregapi.oredict.OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0) continue;
			tMaterial = gregapi.oredict.MaterialRegistry.INSTANCE.get(tMaterial); // the alias merge
			if (tMaterial == null || tMaterial.mID < 0 || !tSeen.add(tMaterial.mNameInternal)) continue;
			String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(tMaterial.mNameInternal);
			for (BladeLadderForm tForm : BLADE_LADDER_FORMS) {
				if (!bladeLadderAxis(tMaterial, tForm.aTypeMin(), tForm.aNoCoated(), tForm.aGem())) continue;
				ResourceLocation tId = digLadderRowId(tForm.aId(), tSnake);
				java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
				java.util.List<String> tPattern = new ArrayList<>();
				for (String tRow : tForm.aPattern()) {
					tPattern.add(tRow);
					for (char tChar : tRow.toCharArray()) {
						if (tChar != ' ') tKey.put(tChar, bladeLadderIngredient(tChar, tMaterial));
					}
				}
				// the #39 inversion fix: the sword rows are upstream HEAD rows (:321, the
				// mToolHeadRecipes face) — vanilla shaped emission, the head item IS the identity
				net.minecraft.world.item.Item tHead = bladeLadderHeadItem(tForm.aId(), tMaterial);
				if (tHead != null) {
					headRowBuilder(tHead, tPattern, tKey).unlockedBy("has_head_material", has(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake))).save(aConsumer, tId);
					continue;
				}
				net.minecraft.world.item.Item tResult = bladeLadderResult(tForm.aId());
				if (tResult == null) continue; // the sword head item miss — no row (never an unresolvable result)
				net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
						.recipeAdvancement()
						.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
						.addCriterion("has_head_material", has(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake)))
						.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
						.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
						.requirements(net.minecraft.advancements.RequirementsStrategy.OR);
				aConsumer.accept(new MaterialToolRow(tId, tId.withPrefix("recipes/tools/"),
						net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT, tPattern, tKey,
						tResult, tSnake, tAdvancement));
			}
		}
	}
//?} else {
/*	private void bladeLadderRows(net.minecraft.data.recipes.RecipeOutput aOutput) {
		java.util.Set<String> tSeen = new java.util.HashSet<>();
		for (gregapi.oredict.OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0) continue;
			tMaterial = gregapi.oredict.MaterialRegistry.INSTANCE.get(tMaterial); // the alias merge
			if (tMaterial == null || tMaterial.mID < 0 || !tSeen.add(tMaterial.mNameInternal)) continue;
			String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(tMaterial.mNameInternal);
			for (BladeLadderForm tForm : BLADE_LADDER_FORMS) {
				if (!bladeLadderAxis(tMaterial, tForm.aTypeMin(), tForm.aNoCoated(), tForm.aGem())) continue;
				ResourceLocation tId = digLadderRowId(tForm.aId(), tSnake);
				java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
				java.util.List<String> tPattern = new ArrayList<>();
				for (String tRow : tForm.aPattern()) {
					tPattern.add(tRow);
					for (char tChar : tRow.toCharArray()) {
						if (tChar != ' ') tKey.put(tChar, bladeLadderIngredient(tChar, tMaterial));
					}
				}
					// the #39 inversion fix: the sword rows are upstream HEAD rows (:321) — vanilla shaped emission
					net.minecraft.world.item.Item tHead = bladeLadderHeadItem(tForm.aId(), tMaterial);
					if (tHead != null) {
						headRowBuilder(tHead, tPattern, tKey).unlockedBy("has_head_material", has(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake))).save(aOutput, tId);
						continue;
					}
					net.minecraft.world.item.Item tResult = bladeLadderResult(tForm.aId());
					if (tResult == null) continue; // the sword head item miss — no row
					net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
							.recipeAdvancement()
							.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
							.addCriterion("has_head_material", has(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake)))
							.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
							.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
							.requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR);
					gregtech6.items.tools.GT6MaterialToolRecipe tRecipe = new gregtech6.items.tools.GT6MaterialToolRecipe("",
							net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
							net.minecraft.world.item.crafting.ShapedRecipePattern.of(tKey, tPattern),
							new net.minecraft.world.item.ItemStack(tResult), true, tSnake);
					aOutput.accept(tId, tRecipe, tAdvancement.build(tId.withPrefix("recipes/tools/")));
				}
			}
		}
	*///?}

	// ------------------------------------------------------------------------
	// The machine ladder material rows (task machine-ladder) — the upstream
	// OreProcessing_Tool mToolRecipes over the machine rows (Loader_Tools.java:305-316
	// chisel/screwdriver/saw, :310-311 wrench/monkey wrench, :314 crowbar, :324 cutter,
	// :327-328 hammer/soft hammer, :316 pincers), ONE gt6:material_tool row per
	// (machine form x axis material). Letters (the OreProcessing_Tool alphabet
	// Loader_Tools:393-404 + the CR.java:193-217 lowercase TOOL letters): P = plate,
	// I = ingot, S = stick, T = screw, X = plateCurved (ALL OF THE MATERIAL); h/f/d/r
	// = the hammer/file/screwdriver/soft-hammer TOOL tags; V = the blue dye (the
	// crowbar's :314 special). The C/G (plateGem/gem) SECOND variants of the wrench pair
	// (:310 arg-8 second shapes) and the hammer (:327 arg-9 G shape) landed with task
	// debt-gem-sisters — the #39 card's declared remainder: 'C' = the plateGem ITEM
	// (no tag family, the blade-family precedent), 'G' = the gems tag (item-truth gated);
	// the gem variants swap the in-grid hammer for the file ('f' = CR.java:196 — gem
	// plates/gems are FILED, not hammered, the upstream letter choice verbatim). The
	// axis gates are the upstream And() rows verbatim through the M5 identity form
	// ({@link #woodExcluded}): the mToolTypes>0 listener gate (:426), typemin,
	// ANTIMATTER.NOT, COATED.NOT, the hammer's Nor(WOOD,BOUNCY,STRETCHY) and the soft
	// hammer's Or(...) + EXTRUDER.NOT (TAG tests — TD.Properties/TD.Processing), the
	// wrench pair's qualmin(1). Forms whose plain steel-route anchors already exist
	// (hammer/wrench/monkey_wrench/soft_hammer/pincers) EXCLUDE Steel; the forms
	// landing here FIRST (screwdriver/saw/chisel/crowbar/cutter) INCLUDE it (the
	// blade-family ruling: the stamped row IS the steel row). The magnifying glass has
	// NO material row — its upstream route is the AdvancedCraftingTool lens-head row
	// (Loader_Tools.java:332), not an OreProcessing_Tool walk; its steel anchor stays
	// the whole crafting face (declared).
	// The SOFT HAMMER (:328) is the declared EMPTY row: its axis Or(WOOD,BOUNCY,STRETCHY)
	// + EXTRUDER.NOT ∩ the port item truth = ∅ — every soft-tag material either carries
	// the EXTRUDER tag (the Rubbers/Plastics AND the machine-alloy IronWood, the probe-
	// verified TD.Processing.EXTRUDER membership) or lacks the ingot item (the wood()
	// family), so the walk emits nothing; the :334 AdvancedCraftingTool route is the
	// steel anchor (softHammerBuilder), and the ×8 form multiplier (GT_Tool_SoftHammer
	// :79-81) rides the ladder through /give-stamped identities only (no serializer
	// multiplier field — the dig card's yagni ruling stands: no generated row needs one).

	/** One machine ladder form: the id, the axis gates, the Steel inclusion, the stamp multiplier, the upstream row shape. (package-private: the offline gem-sister census walk, the TOOL_HEAD_ROW_FORMS precedent) */
	record MachineLadderForm(String aId, int aTypeMin, boolean aNoCoated, boolean aNoSoftTag, boolean aSoftTag,
			boolean aNoExtruder, int aQualMin, boolean aIncludeSteel, String[] aPattern) {
	}

	static final MachineLadderForm[] MACHINE_LADDER_FORMS = {
			new MachineLadderForm("screwdriver", 2, true, false, false, false, 0, true, new String[] {"hS", "Sf"}), // :306
			new MachineLadderForm("saw", 2, true, false, false, false, 0, true, new String[] {"PP", "fh"}), // :307
			new MachineLadderForm("chisel", 2, true, false, false, false, 0, true, new String[] {"hPf", " S "}), // :305
			new MachineLadderForm("crowbar", 0, false, false, false, false, 0, true, new String[] {"hVS", "VSV", "SVf"}), // :314
			new MachineLadderForm("cutter", 2, false, true, false, false, 0, true, new String[] {"PfP", "hPd", "STS"}), // :324
			new MachineLadderForm("hammer", 0, true, true, false, false, 0, false, new String[] {"II ", "IIh", "II "}), // :327
			new MachineLadderForm("hammer_gem", 0, true, true, false, false, 0, false, new String[] {"GG ", "GGf", "GG "}), // :327 G — the gem head row, 'f' not 'h'
			new MachineLadderForm("wrench", 2, false, false, false, false, 1, false, new String[] {"PhP", " P ", " P "}), // :310
			new MachineLadderForm("wrench_gem", 2, false, false, false, false, 1, false, new String[] {"CfC", " C ", " C "}), // :310 C — 'f' not 'h'
			new MachineLadderForm("monkey_wrench", 2, false, false, false, false, 1, false, new String[] {"PPd", "hPT", " P "}), // :311
			new MachineLadderForm("monkey_wrench_gem", 2, false, false, false, false, 1, false, new String[] {"CCd", "fCT", " C "}), // :311 C — 'f' not 'h'
			new MachineLadderForm("pincers", 2, false, false, false, false, 0, false, new String[] {"XhX", " T ", "SdS"}), // :316
	};

	/**
	 * The result item of a machine ladder form (the GT6Tools registry face) — the DIRECT
	 * rows only (the upstream mToolRecipes arg-8 face: crowbar :314 / cutter(wire) :324 /
	 * wrench :310 / monkey wrench :311 / pincers :316). The screwdriver :306 / saw :307 /
	 * chisel :305 / hammer :327 shapes are upstream mToolHeadRecipes rows (the arg-9 face)
	 * — they emit the head via {@link #machineLadderHeadItem} (the #39 inversion fix),
	 * and the soft hammer's :328 head shape is EMPTY (its axis ∩ the port item truth = ∅,
	 * the softHammerBuilder steel anchor owns the tool face).
	 */
	private static net.minecraft.world.item.Item machineLadderResult(String aForm) {
		return switch (aForm) {
			case "crowbar" -> GT6Tools.CROWBAR.get();
			case "cutter" -> GT6Tools.CUTTER.get();
			case "soft_hammer" -> GT6Tools.SOFT_HAMMER.get();
			case "wrench" -> GT6Tools.WRENCH.get();
			case "wrench_gem" -> GT6Tools.WRENCH.get();
			case "monkey_wrench" -> GT6Tools.MONKEY_WRENCH.get();
			case "monkey_wrench_gem" -> GT6Tools.MONKEY_WRENCH.get();
			case "pincers" -> GT6Tools.PINCERS.get();
			default -> null; // the head rows ride machineLadderHeadItem
		};
	}

	/**
	 * The head item of the machine rows whose shapes are upstream HEAD rows (the tStack
	 * face — Loader_Tools.java:451 listening prefix + :514-516 emission): screwdriver
	 * {@code OP.toolHeadScrewdriver} :306, saw {@code OP.toolHeadSaw} :307, chisel
	 * {@code OP.toolHeadChisel} :305, hammer {@code OP.toolHeadHammer} :327. Null = the
	 * direct tool rows and the head item miss.
	 */
	private static net.minecraft.world.item.Item machineLadderHeadItem(String aForm, gregapi.oredict.OreDictMaterial aMaterial) {
		gregapi.oredict.OreDictPrefix tPrefix = switch (aForm) {
			case "screwdriver" -> gregapi.data.OP.toolHeadScrewdriver;
			case "saw" -> gregapi.data.OP.toolHeadSaw;
			case "chisel" -> gregapi.data.OP.toolHeadChisel;
			case "hammer", "hammer_gem" -> gregapi.data.OP.toolHeadHammer;
			default -> null;
		};
		if (tPrefix == null) return null;
		return gregtech6.registry.GTMaterialItems.get(tPrefix, aMaterial) == null ? null
				: gregtech6.registry.GTMaterialItems.get(tPrefix, aMaterial).get();
	}

	/**
	 * The row letters (the alphabet + TOOL letters). {@code null} = the item-truth miss
	 * (the material carries no registered item for a material-carrying letter) — the row
	 * is skipped, never emitted with an unresolvable ingredient.
	 */
	private static net.minecraft.world.item.crafting.Ingredient machineLadderIngredient(char aKey, gregapi.oredict.OreDictMaterial aMaterial) {
		String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(aMaterial.mNameInternal);
		return switch (aKey) {
			case 'P' -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plate, aMaterial) == null ? null
					: net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake));
			case 'I' -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.ingot, aMaterial) == null ? null
					: net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, tSnake));
			case 'S' -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.stick, aMaterial) == null ? null
					: net.minecraft.world.item.crafting.Ingredient.of(gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.stick, aMaterial).get());
			case 'T' -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.screw, aMaterial) == null ? null
					: net.minecraft.world.item.crafting.Ingredient.of(gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.screw, aMaterial).get());
			case 'X' -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateCurved, aMaterial) == null ? null
					: net.minecraft.world.item.crafting.Ingredient.of(gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateCurved, aMaterial).get());
			case 'C' -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateGem, aMaterial) == null ? null
					: net.minecraft.world.item.crafting.Ingredient.of(gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateGem, aMaterial).get());
			case 'G' -> gregapi.data.OP.gem.isGeneratingItem(aMaterial) ? net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, tSnake)) : null;
			case 'h' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_HARD_HAMMER);
			case 'f' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_FILE);
			case 'd' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_SCREWDRIVER);
			case 'r' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_SOFT_HAMMER);
			case 'V' -> net.minecraft.world.item.crafting.Ingredient.of(Tags.Items.DYES_BLUE);
			default -> throw new IllegalArgumentException("unknown machine ladder letter: " + aKey);
		};
	}

	/** The per-form axis gate — the upstream And() rows verbatim (see the block javadoc). (package-private: the offline gem-sister census walk) */
	static boolean machineLadderAxis(gregapi.oredict.OreDictMaterial aMaterial, MachineLadderForm aForm) {
		if (aMaterial.mToolTypes <= 0 || aMaterial.mToolTypes < aForm.aTypeMin()) return false; // the :426 listener gate + typemin
		if (aMaterial.mToolQuality < aForm.aQualMin()) return false; // qualmin
		if (aMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) return false; // ANTIMATTER.NOT
		if (woodExcluded(aMaterial)) return false; // MT.Wood.NOT — the M5 identity form
		if (aForm.aNoCoated() && aMaterial.contains(gregapi.data.TD.Compounds.COATED)) return false; // COATED.NOT
		boolean tSoftTag = aMaterial.contains(gregapi.data.TD.Properties.WOOD)
				|| aMaterial.contains(gregapi.data.TD.Properties.BOUNCY)
				|| aMaterial.contains(gregapi.data.TD.Properties.STRETCHY);
		if (aForm.aNoSoftTag() && tSoftTag) return false; // the hammer's Nor(WOOD, BOUNCY, STRETCHY)
		if (aForm.aSoftTag() && !tSoftTag) return false; // the soft hammer's Or(WOOD, BOUNCY, STRETCHY)
		if (aForm.aNoExtruder() && aMaterial.contains(gregapi.data.TD.Processing.EXTRUDER)) return false; // EXTRUDER.NOT
		return true;
	}

	/** The row's distinct letters (the pattern walk — NOT the id). */
	private static java.util.Set<Character> machineLadderLetters(MachineLadderForm aForm) {
		java.util.Set<Character> rLetters = new java.util.HashSet<>();
		for (String tRow : aForm.aPattern()) {
			for (char tChar : tRow.toCharArray()) {
				if (tChar != ' ') rLetters.add(tChar);
			}
		}
		return rLetters;
	}

	/**
	 * The advancement anchor — the plates/ingots tag when the row carries P/I, else the
	 * first material item (stick/screw/curved plate). Leg-agnostic value: a
	 * {@link TagKey} the forge caller feeds {@code has(TagKey)} and the neo caller feeds
	 * {@code has(TagKey)} likewise.
	 */
	private static Object machineLadderCriterion(MachineLadderForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {
		java.util.Set<Character> tLetters = machineLadderLetters(aForm);
		String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(aMaterial.mNameInternal);
		if (tLetters.contains('P')) return GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake);
		if (tLetters.contains('I')) return GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, tSnake);
		if (tLetters.contains('C')) return gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateGem, aMaterial).get(); // the resolvable gate ran first
		if (tLetters.contains('G')) return GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, tSnake);
		for (char tKey : new char[] {'S', 'T', 'X'}) {
			gregapi.oredict.OreDictPrefix tPrefix = tKey == 'S' ? gregapi.data.OP.stick : tKey == 'T' ? gregapi.data.OP.screw : gregapi.data.OP.plateCurved;
			if (tLetters.contains(tKey) && gregtech6.registry.GTMaterialItems.get(tPrefix, aMaterial) != null) {
				return gregtech6.registry.GTMaterialItems.get(tPrefix, aMaterial).get();
			}
		}
		throw new IllegalArgumentException("no material-carrying letter to anchor the advancement: " + aForm.aId());
	}

	// ------------------------------------------------------------------------
	// The head-row completion band (task 39-toolhead-rows, GitHub #39) — the arg-9
	// tool-head rows #39 left uncrafted: the 9 families builderwand (Loader_Tools
	// :293), axeDouble (:301), sense (:302), plow (:303), file (:304), chainsaw (:308),
	// drill (:309), wrench head (:310), plus the C (plateGem)/G (gem) SECOND variants of
	// the dig six (:294-300) and the machine chisel (:305)/saw (:307). ONE vanilla
	// crafting_shaped row per (form x head-item material) through the headRowBuilder
	// face — the head item IS the identity. The walk = the head-prefix item truth (the
	// registrationOrder; the ported OP prefix conditions carry the typemin/BOUNCY/
	// STRETCHY/chain gates, OP.java:1281-1298) ∩ the listener gates (the :426
	// mToolTypes>0 + the :393-405 letter alphabet rows verbatim) ∩ the letter item
	// truth. Letters: P = the plates tag, I = the ingots tag, G = the gems tag (all
	// item-truth gated), C = the plateGem ITEM (the bladeLadderIngredient 'C' precedent
	// — no tag family), S = the stick item; f/h/s/k/d = the CR.java:193-217 lowercase
	// TOOL letters (the tool tags; 's' = TOOLS_SAW, the decisions.r9-toolhead-s-letter
	// ruling). The chainsaw/drill/wrench fixed letters ride the per-form specials
	// (Loader_Tools :308-:310 arg-13/14: chainsaw W=plate/V=ring Steel + X=the row
	// material's chain, drill V=plateCurved Steel, wrench head W=screw/V=ring Steel).
	// Row ids: the C variant is <form>_gem (the blade-ladder convention), the builderwand
	// :293 third shape (the bare-gem 'G' variant) is builder_wand_pure_gem, and the
	// wrench HEAD rows are wrench_head (wrench/<mat> is the machine band's :310 arg-8
	// tool row). Steel keeps its head rows (the machine-band ruling: the p29-w5 steel
	// anchors own the TOOL face only). DECLARED: the wood() family folds out through the
	// letter item truth (no plate/plateGem/gem items) — the dig-band plank fold stays
	// unported, and the builderwand :293 axis lacking the upstream MT.Wood.NOT is
	// unobservable for the same reason (the uniform woodExcluded here).
	// ------------------------------------------------------------------------

	/** One head-row form: the id, the head prefix, the listener gates ({@code aQualMax} Integer.MAX_VALUE = no gate), the shape, the fixed-prefix specials. */
	record ToolHeadRowForm(String aId, gregapi.oredict.OreDictPrefix aHead, int aTypeMin, int aQualMin, int aQualMax,
			boolean aNoCoated, String[] aPattern, java.util.Map<Character, ToolHeadSpecial> aSpecials) {}

	/** A fixed-prefix letter: {@code aFixed} null = the walk material, else the fixed material (the ANY.Steel fold). */
	record ToolHeadSpecial(gregapi.oredict.OreDictPrefix aPrefix, gregapi.oredict.OreDictMaterial aFixed) {}

	/** One walked head row (the test-visible census unit). */
	record ToolHeadRow(ToolHeadRowForm aForm, gregapi.oredict.OreDictMaterial aMaterial, String aSnake) {}

	/** The :293-:310 arg-9 completion table + the dig/chisel/saw C variants — upstream order, shapes verbatim. */
	static final java.util.List<ToolHeadRowForm> TOOL_HEAD_ROW_FORMS = java.util.List.of(
			// builderwand :293 — the And(ANTIMATTER.NOT) axis ONLY (no Wood/COATED gate upstream)
			new ToolHeadRowForm("builder_wand", gregapi.data.OP.toolHeadBuilderwand, 0, 0, Integer.MAX_VALUE, false, new String[] {" P ", "f h", " s "}, null),
			new ToolHeadRowForm("builder_wand_gem", gregapi.data.OP.toolHeadBuilderwand, 0, 0, Integer.MAX_VALUE, false, new String[] {" C ", "f h", " s "}, null),
			new ToolHeadRowForm("builder_wand_pure_gem", gregapi.data.OP.toolHeadBuilderwand, 0, 0, Integer.MAX_VALUE, false, new String[] {" G ", "f h", " s "}, null),
			// the dig six C variants (:294-300 second shapes) — the dig axis (no typemin)
			new ToolHeadRowForm("pickaxe_construction_gem", gregapi.data.OP.toolHeadConstructionPickaxe, 0, 0, Integer.MAX_VALUE, true, new String[] {"CGC", "f  "}, null),
			new ToolHeadRowForm("pickaxe_gem", gregapi.data.OP.toolHeadPickaxe, 0, 0, Integer.MAX_VALUE, true, new String[] {"CGG", "f  "}, null),
			new ToolHeadRowForm("shovel_gem", gregapi.data.OP.toolHeadShovel, 0, 0, Integer.MAX_VALUE, true, new String[] {"fC "}, null),
			new ToolHeadRowForm("spade_gem", gregapi.data.OP.toolHeadSpade, 0, 0, Integer.MAX_VALUE, true, new String[] {"fC ", " s "}, null),
			new ToolHeadRowForm("hoe_gem", gregapi.data.OP.toolHeadHoe, 0, 0, Integer.MAX_VALUE, true, new String[] {"CG ", "f  "}, null),
			new ToolHeadRowForm("axe_gem", gregapi.data.OP.toolHeadAxe, 0, 0, Integer.MAX_VALUE, true, new String[] {"CG ", "C  ", "f  "}, null),
			// axeDouble :301 / sense :302 / plow :303 — the typemin(2) axis
			new ToolHeadRowForm("axe_double", gregapi.data.OP.toolHeadAxeDouble, 2, 0, Integer.MAX_VALUE, true, new String[] {"PIP", "P P", "f h"}, null),
			new ToolHeadRowForm("axe_double_gem", gregapi.data.OP.toolHeadAxeDouble, 2, 0, Integer.MAX_VALUE, true, new String[] {"CGC", "C C", "f  "}, null),
			new ToolHeadRowForm("sense", gregapi.data.OP.toolHeadSense, 2, 0, Integer.MAX_VALUE, true, new String[] {"PPI", "f h"}, null),
			new ToolHeadRowForm("sense_gem", gregapi.data.OP.toolHeadSense, 2, 0, Integer.MAX_VALUE, true, new String[] {"CCG", "f  "}, null),
			new ToolHeadRowForm("plow", gregapi.data.OP.toolHeadPlow, 2, 0, Integer.MAX_VALUE, true, new String[] {"PPP", "PPP", "f h"}, null),
			new ToolHeadRowForm("plow_gem", gregapi.data.OP.toolHeadPlow, 2, 0, Integer.MAX_VALUE, true, new String[] {"CCC", "CCC", "f  "}, null),
			// file :304 — typemin(2) + qualmax(2), k = the knife tool letter (CR.java:204)
			new ToolHeadRowForm("file", gregapi.data.OP.toolHeadFile, 2, 0, 2, true, new String[] {" P ", " Pk"}, null),
			// the machine chisel C (:305) / saw C (:307) — the machine typemin(2) axis
			new ToolHeadRowForm("chisel_gem", gregapi.data.OP.toolHeadChisel, 2, 0, Integer.MAX_VALUE, true, new String[] {"Cf", "S "}, null),
			new ToolHeadRowForm("saw_gem", gregapi.data.OP.toolHeadSaw, 2, 0, Integer.MAX_VALUE, true, new String[] {"CC", "f "}, null),
			// chainsaw :308 — W/V Steel + the material chain; NO COATED gate upstream
			new ToolHeadRowForm("chainsaw", gregapi.data.OP.toolHeadChainsaw, 2, 0, Integer.MAX_VALUE, false, new String[] {"WVW", "XhX", "WVW"},
					java.util.Map.of('W', new ToolHeadSpecial(gregapi.data.OP.plate, gregapi.data.MT.Steel),
							'V', new ToolHeadSpecial(gregapi.data.OP.ring, gregapi.data.MT.Steel),
							'X', new ToolHeadSpecial(gregapi.data.OP.chain, null))),
			// drill :309 — V = plateCurved Steel
			new ToolHeadRowForm("drill", gregapi.data.OP.toolHeadDrill, 2, 0, Integer.MAX_VALUE, false, new String[] {"PVP", "PVP", "VhV"},
					java.util.Map.of('V', new ToolHeadSpecial(gregapi.data.OP.plateCurved, gregapi.data.MT.Steel))),
			new ToolHeadRowForm("drill_gem", gregapi.data.OP.toolHeadDrill, 2, 0, Integer.MAX_VALUE, false, new String[] {"CVC", "CVC", "VhV"},
					java.util.Map.of('V', new ToolHeadSpecial(gregapi.data.OP.plateCurved, gregapi.data.MT.Steel))),
			// wrench head :310 — qualmin(1), NO COATED gate upstream
			new ToolHeadRowForm("wrench_head", gregapi.data.OP.toolHeadWrench, 2, 1, Integer.MAX_VALUE, false, new String[] {"hPW", "PVP", "WPd"},
					java.util.Map.of('W', new ToolHeadSpecial(gregapi.data.OP.screw, gregapi.data.MT.Steel),
							'V', new ToolHeadSpecial(gregapi.data.OP.ring, gregapi.data.MT.Steel))),
			new ToolHeadRowForm("wrench_head_gem", gregapi.data.OP.toolHeadWrench, 2, 1, Integer.MAX_VALUE, false, new String[] {"hCW", "CVC", "WCd"},
					java.util.Map.of('W', new ToolHeadSpecial(gregapi.data.OP.screw, gregapi.data.MT.Steel),
							'V', new ToolHeadSpecial(gregapi.data.OP.ring, gregapi.data.MT.Steel))));

	/** The per-form listener gate — the upstream And() rows verbatim (OreDictMaterialCondition :67-89). */
	private static boolean toolHeadRowAxis(gregapi.oredict.OreDictMaterial aMaterial, ToolHeadRowForm aForm) {
		if (aMaterial.mToolTypes <= 0) return false; // the :426 listener gate
		if (aMaterial.mToolTypes < aForm.aTypeMin()) return false; // typemin
		if (aMaterial.mToolQuality < aForm.aQualMin()) return false; // qualmin
		if (aMaterial.mToolQuality > aForm.aQualMax()) return false; // qualmax
		if (aMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) return false; // ANTIMATTER.NOT (every family)
		if (woodExcluded(aMaterial)) return false; // MT.Wood.NOT — the M5 identity form (builderwand note above)
		if (aForm.aNoCoated() && aMaterial.contains(gregapi.data.TD.Compounds.COATED)) return false; // COATED.NOT
		return true;
	}

	/**
	 * The row letters — {@code null} = the item-truth miss (the row is skipped, never
	 * emitted with an unresolvable ingredient; the machineLadderIngredient :4073 form).
	 */
	private static net.minecraft.world.item.crafting.Ingredient toolHeadRowIngredient(ToolHeadRowForm aForm, char aKey,
			gregapi.oredict.OreDictMaterial aMaterial) {
		ToolHeadSpecial tSpecial = aForm.aSpecials() != null ? aForm.aSpecials().get(aKey) : null;
		if (tSpecial != null) {
			gregapi.oredict.OreDictMaterial tMaterial = tSpecial.aFixed() != null ? tSpecial.aFixed() : aMaterial;
			return gregtech6.registry.GTMaterialItems.get(tSpecial.aPrefix(), tMaterial) == null ? null
					: net.minecraft.world.item.crafting.Ingredient.of(gregtech6.registry.GTMaterialItems.get(tSpecial.aPrefix(), tMaterial).get());
		}
		String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(aMaterial.mNameInternal);
		return switch (aKey) {
			case 'P' -> gregapi.data.OP.plate.isGeneratingItem(aMaterial) ? net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake)) : null;
			case 'I' -> gregapi.data.OP.ingot.isGeneratingItem(aMaterial) ? net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, tSnake)) : null;
			case 'G' -> gregapi.data.OP.gem.isGeneratingItem(aMaterial) ? net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, tSnake)) : null;
			case 'C' -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateGem, aMaterial) == null ? null
					: net.minecraft.world.item.crafting.Ingredient.of(gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateGem, aMaterial).get());
			case 'S' -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.stick, aMaterial) == null ? null
					: net.minecraft.world.item.crafting.Ingredient.of(gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.stick, aMaterial).get());
			case 'f' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_FILE);
			case 'h' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_HARD_HAMMER);
			case 's' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_SAW);
			case 'k' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_KNIFE);
			case 'd' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_SCREWDRIVER);
			default -> throw new IllegalArgumentException("unknown tool-head row letter: " + aKey);
		};
	}

	/**
	 * Every material-carrying letter's ITEM TRUTH holds for the material (the row-skip
	 * gate). OFFLINE-PURE: the predicate pass ({@code isGeneratingItem} — the
	 * PrefixItem.java:104 registration criterion, the SAME face registerItems walks), so
	 * the census walk reproducible in the headless test JVM; the emission resolver's
	 * get() faces stay datagen-JVM-only (a registrationOrder pair is guaranteed a
	 * registered item there — first-wins id-drops are folded out by enumerate()).
	 */
	private static boolean toolHeadRowResolvable(ToolHeadRowForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {
		for (String tPatternRow : aForm.aPattern()) {
			for (char tChar : tPatternRow.toCharArray()) {
				if (tChar == ' ') continue;
				ToolHeadSpecial tSpecial = aForm.aSpecials() != null ? aForm.aSpecials().get(tChar) : null;
				if (tSpecial != null) {
					if (!tSpecial.aPrefix().isGeneratingItem(tSpecial.aFixed() != null ? tSpecial.aFixed() : aMaterial)) return false;
				} else {
					boolean tTruth = switch (tChar) {
						case 'P' -> gregapi.data.OP.plate.isGeneratingItem(aMaterial);
						case 'I' -> gregapi.data.OP.ingot.isGeneratingItem(aMaterial);
						case 'G' -> gregapi.data.OP.gem.isGeneratingItem(aMaterial);
						case 'C' -> gregapi.data.OP.plateGem.isGeneratingItem(aMaterial);
						case 'S' -> gregapi.data.OP.stick.isGeneratingItem(aMaterial);
						case 'f', 'h', 's', 'k', 'd' -> true; // the tool tags carry no material truth
						default -> throw new IllegalArgumentException("unknown tool-head row letter: " + tChar);
					};
					if (!tTruth) return false;
				}
			}
		}
		return true;
	}

	/** The walk face (offline-pure, the census unit): head truth ∩ the listener axis ∩ the letter item truth. */
	static java.util.List<ToolHeadRow> toolHeadRows() {
		java.util.List<ToolHeadRow> rRows = new ArrayList<>();
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder()) {
			for (ToolHeadRowForm tForm : TOOL_HEAD_ROW_FORMS) {
				if (tForm.aHead() != tPair.prefix()) continue;
				gregapi.oredict.OreDictMaterial tMaterial = tPair.material();
				if (!toolHeadRowAxis(tMaterial, tForm)) continue;
				if (!toolHeadRowResolvable(tForm, tMaterial)) continue;
				rRows.add(new ToolHeadRow(tForm, tMaterial, gregtech6.registry.GTMaterialItems.snakeCase(tMaterial.mNameInternal)));
			}
		}
		return rRows;
	}

//? if forge {
	private void toolHeadRows(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		for (ToolHeadRow tRow : toolHeadRows()) {
			net.minecraft.world.item.Item tHead = gregtech6.registry.GTMaterialItems.get(tRow.aForm().aHead(), tRow.aMaterial()).get();
			ResourceLocation tId = digLadderRowId(tRow.aForm().aId(), tRow.aSnake());
			java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
			java.util.List<String> tPattern = new ArrayList<>();
			for (String tPatternRow : tRow.aForm().aPattern()) {
				tPattern.add(tPatternRow);
				for (char tChar : tPatternRow.toCharArray()) {
					if (tChar != ' ' && !tKey.containsKey(tChar)) tKey.put(tChar, toolHeadRowIngredient(tRow.aForm(), tChar, tRow.aMaterial()));
				}
			}
			// the head item IS the identity — the vanilla save() writes the recipes/tools advancement face
			headRowBuilder(tHead, tPattern, tKey).unlockedBy("has_head", has(tHead)).save(aConsumer, tId);
		}
	}

//?} else {
/*	private void toolHeadRows(net.minecraft.data.recipes.RecipeOutput aOutput) {
		for (ToolHeadRow tRow : toolHeadRows()) {
			net.minecraft.world.item.Item tHead = gregtech6.registry.GTMaterialItems.get(tRow.aForm().aHead(), tRow.aMaterial()).get();
			ResourceLocation tId = digLadderRowId(tRow.aForm().aId(), tRow.aSnake());
			java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
			java.util.List<String> tPattern = new java.util.ArrayList<>();
			for (String tPatternRow : tRow.aForm().aPattern()) {
				tPattern.add(tPatternRow);
				for (char tChar : tPatternRow.toCharArray()) {
					if (tChar != ' ' && !tKey.containsKey(tChar)) tKey.put(tChar, toolHeadRowIngredient(tRow.aForm(), tChar, tRow.aMaterial()));
				}
			}
			// the head item IS the identity — the vanilla save() writes the recipes/tools advancement face
			headRowBuilder(tHead, tPattern, tKey).unlockedBy("has_head", has(tHead)).save(aOutput, tId);
		}
	}
*///?}

	// ------------------------------------------------------------------
	// task bumbliary-recipes — the Bumbliary pair rows: the registration-line
	// varargs transcriptions (Loader_MultiTileEntities.java:2222/:2223). The
	// NBT_RECIPEMAP RM.BumbleQueens tag upstream rides the same lines — the DISPLAY-only
	// fold (a null-backend fake-recipe map, MultiItemBumbles.java:614; the port BE has
	// no RM consumption face — declared in the card account, the gui card owns the face).
	// SHARED REGION: both builders are leg-neutral (the gregOLantern/bathing-pot builder
	// form) — only the .save(aConsumer/aOutput, id) call site is forked per leg.

	/**
	 * The Bumbliary row — the upstream :2222 line-tail varargs VERBATIM
	 * {@code "PPP","PBP","TdT", 'B', getItem(32755), 'P', plate(WoodTreated), 'T',
	 * screw(ANY.Iron)}: 'B' = the R2 hive BlockItem (the carryable wild hive,
	 * {@link GT6BeeHives#HIVE_ITEM}), 'P' = the {@code plates/wood_treated} material tag
	 * (the bathing-pot steel-plate precedent), 'T' = the iron screw — the ANY.Iron
	 * oredict folds onto the iron member (the single-representative convergence, the
	 * scissors steel-screw precedent), 'd' = the screwdriver tool tag (the drawer-row
	 * shape). Result 1x {@code gt6:bumbliary}.
	 */
	private ShapedRecipeBuilder bumbliaryBuilder() {
		TagKey<Item> tWoodPlates = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "wood_treated");
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GT6BeeHives.BUMBLIARY_ITEM.get())
				.pattern("PPP").pattern("PBP").pattern("TdT")
				.define('P', tWoodPlates)
				.define('B', GT6BeeHives.HIVE_ITEM.get())
				.define('T', GTMaterialItems.get(gregapi.data.OP.screw, gregapi.data.MT.Iron).get())
				.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
				.unlockedBy("has_bumble_hive", has(GT6BeeHives.HIVE_ITEM.get()));
	}

	/**
	 * The Advanced Bumbliary row — the upstream :2223 line-tail varargs VERBATIM
	 * {@code "PRP","HBH","PCP", 'B', getItem(32741), 'P', plate(StainlessSteel), 'C',
	 * craftingChest, 'R', beeCombCrossbred, 'H', container1000honey}: 'B' = the Bumbliary
	 * BlockItem, 'P' = the {@code plates/stainless_steel} material tag, 'C' = the platform
	 * chest tag (the static-storage LOCKER precedent), 'R' =
	 * {@link GT6ItemTags#COMBS_CROSSBRED} (the OD.beeCombCrossbred translation), 'H' = the
	 * vanilla honey bottle — THE DECLARED FOLD: {@code OD.container1000honey} is a SOFT
	 * oredict upstream, satisfied by foreign-mod carriers only (LoaderItemList
	 * :1123-:2038 — Forestry/GrowthCraft/ERE; GT6 core registers no carrier — the
	 * itemGlue dormant-row class), so the port universe's vanilla-native honey container
	 * stands in (the gregOLantern OD.blockTorch → Items.TORCH fold precedent; the
	 * 1000 L quantity token is unobservable through the vanilla crafting face). Result
	 * 1x {@code gt6:bumbliary_advanced}.
	 */
	private ShapedRecipeBuilder advancedBumbliaryBuilder() {
		TagKey<Item> tSteelPlates = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "stainless_steel");
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GT6BeeHives.BUMBLIARY_ADVANCED_ITEM.get())
				.pattern("PRP").pattern("HBH").pattern("PCP")
				.define('P', tSteelPlates)
				.define('B', GT6BeeHives.BUMBLIARY_ITEM.get())
				.define('C', Tags.Items.CHESTS)
				.define('R', GT6ItemTags.COMBS_CROSSBRED)
				.define('H', Items.HONEY_BOTTLE)
				.unlockedBy("has_bumbliary", has(GT6BeeHives.BUMBLIARY_ITEM.get()));
	}

//? if forge {
	private void machineLadderRows(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		java.util.Set<String> tSeen = new java.util.HashSet<>();
		for (gregapi.oredict.OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0) continue;
			tMaterial = gregapi.oredict.MaterialRegistry.INSTANCE.get(tMaterial); // the alias merge
			if (tMaterial == null || tMaterial.mID < 0 || !tSeen.add(tMaterial.mNameInternal)) continue;
			String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(tMaterial.mNameInternal);
			for (MachineLadderForm tForm : MACHINE_LADDER_FORMS) {
				net.minecraft.world.item.Item tHead = machineLadderHeadItem(tForm.aId(), tMaterial);
				// the steel anchors own the TOOL face only — the HEAD rows keep Steel (upstream
				// :305-:327 verbatim; the steel hammer HEAD is the softHammerBuilder 'H' input)
				if (tHead == null && !tForm.aIncludeSteel() && tMaterial == gregapi.data.MT.Steel) continue;
				if (!machineLadderAxis(tMaterial, tForm)) continue;
				java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
				java.util.List<String> tPattern = new java.util.ArrayList<>();
				boolean tResolvable = true;
				for (String tRow : tForm.aPattern()) {
					tPattern.add(tRow);
					for (char tChar : tRow.toCharArray()) {
						if (tChar == ' ' || tKey.containsKey(tChar)) continue;
						net.minecraft.world.item.crafting.Ingredient tIngredient = machineLadderIngredient(tChar, tMaterial);
						if (tIngredient == null) {
							tResolvable = false;
							break;
						}
						tKey.put(tChar, tIngredient);
					}
					if (!tResolvable) break;
				}
				if (!tResolvable) continue; // the item-truth miss — no row
				ResourceLocation tId = digLadderRowId(tForm.aId(), tSnake);
				// the #39 inversion fix: the :305/:306/:307/:327 rows are upstream HEAD rows —
				// vanilla shaped emission, the head item IS the identity
				if (tHead != null) {
					// the #39 inversion fix: the :305/:306/:307/:327 rows are upstream HEAD rows —
					// vanilla shaped emission, the head item IS the identity. The G-carrying gem rows
					// anchor the head item (the toolHeadRows band convention) — the gem universe
					// carries no plates tag to anchor.
					if (machineLadderLetters(tForm).contains('G')) {
						headRowBuilder(tHead, tPattern, tKey).unlockedBy("has_head", has(tHead)).save(aConsumer, tId);
					} else {
						headRowBuilder(tHead, tPattern, tKey).unlockedBy("has_head_material", has(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake))).save(aConsumer, tId);
					}
					continue;
				}
				net.minecraft.world.item.Item tResult = machineLadderResult(tForm.aId());
				if (tResult == null) continue; // unreachable: every non-head form carries a tool face
				Object tAnchor = machineLadderCriterion(tForm, tMaterial);
				net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
						.recipeAdvancement()
						.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
						.addCriterion("has_head_material", tAnchor instanceof TagKey ? has((TagKey<Item>) tAnchor) : has((net.minecraft.world.item.Item) tAnchor))
						.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
						.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
						.requirements(net.minecraft.advancements.RequirementsStrategy.OR);
				aConsumer.accept(new MaterialToolRow(tId, tId.withPrefix("recipes/tools/"),
						net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT, tPattern, tKey,
						tResult, tSnake, tAdvancement));
			}
		}
	}

//?} else {
/*	private void machineLadderRows(net.minecraft.data.recipes.RecipeOutput aOutput) {
		java.util.Set<String> tSeen = new java.util.HashSet<>();
		for (gregapi.oredict.OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0) continue;
			tMaterial = gregapi.oredict.MaterialRegistry.INSTANCE.get(tMaterial); // the alias merge
			if (tMaterial == null || tMaterial.mID < 0 || !tSeen.add(tMaterial.mNameInternal)) continue;
			String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(tMaterial.mNameInternal);
			for (MachineLadderForm tForm : MACHINE_LADDER_FORMS) {
				net.minecraft.world.item.Item tHead = machineLadderHeadItem(tForm.aId(), tMaterial);
				// the steel anchors own the TOOL face only — the HEAD rows keep Steel (upstream :305-:327)
				if (tHead == null && !tForm.aIncludeSteel() && tMaterial == gregapi.data.MT.Steel) continue;
				if (!machineLadderAxis(tMaterial, tForm)) continue;
				java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
				java.util.List<String> tPattern = new java.util.ArrayList<>();
				boolean tResolvable = true;
				for (String tRow : tForm.aPattern()) {
					tPattern.add(tRow);
					for (char tChar : tRow.toCharArray()) {
						if (tChar == ' ' || tKey.containsKey(tChar)) continue;
						net.minecraft.world.item.crafting.Ingredient tIngredient = machineLadderIngredient(tChar, tMaterial);
						if (tIngredient == null) {
							tResolvable = false;
							break;
						}
						tKey.put(tChar, tIngredient);
					}
					if (!tResolvable) break;
				}
				if (!tResolvable) continue; // the item-truth miss — no row
				ResourceLocation tId = digLadderRowId(tForm.aId(), tSnake);
				// the #39 inversion fix: the :305/:306/:307/:327 rows are upstream HEAD rows
				if (tHead != null) {
					// the #39 inversion fix: the :305/:306/:307/:327 rows are upstream HEAD rows.
					// The G-carrying gem rows anchor the head item — the gem universe carries no
					// plates tag to anchor (the forge-leg mirror).
					if (machineLadderLetters(tForm).contains('G')) {
						headRowBuilder(tHead, tPattern, tKey).unlockedBy("has_head", has(tHead)).save(aOutput, tId);
					} else {
						headRowBuilder(tHead, tPattern, tKey).unlockedBy("has_head_material", has(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake))).save(aOutput, tId);
					}
					continue;
				}
				net.minecraft.world.item.Item tResult = machineLadderResult(tForm.aId());
				if (tResult == null) continue; // unreachable: every non-head form carries a tool face
				net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
						.recipeAdvancement()
						.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
						.addCriterion("has_head_material", machineLadderCriterion(tForm, tMaterial) instanceof net.minecraft.tags.TagKey ? has((net.minecraft.tags.TagKey<Item>) machineLadderCriterion(tForm, tMaterial)) : has((net.minecraft.world.item.Item) machineLadderCriterion(tForm, tMaterial)))
						.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
						.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
						.requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR);
				gregtech6.items.tools.GT6MaterialToolRecipe tRecipe = new gregtech6.items.tools.GT6MaterialToolRecipe("",
						net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
						net.minecraft.world.item.crafting.ShapedRecipePattern.of(tKey, tPattern),
						new net.minecraft.world.item.ItemStack(tResult), true, tSnake);
				aOutput.accept(tId, tRecipe, tAdvancement.build(tId.withPrefix("recipes/tools/")));
			}
		}
	}

*///?}

	// -----------------------------------------------------------------------
	// The head+handle assembly band (task 39-toolhead-assembly, GitHub #39) — the
	// upstream AdvancedCraftingTool registrations (Loader_Tools.java:332-350, 19 tools),
	// the shapeless workbench head+stick → tool row whose matches gate is
	// stick-material == head.mHandleMaterial (AdvancedCraftingTool.java:105). Ported as
	// the soft_hammer precedent's stamped shaped face (softHammerBuilder :1125-1132):
	// ONE "H"/"S" column per (form x head item) — 'H' = the tool_head_<form>_<mat> item
	// (the head row output above is its producer), 'S' = the handle. 17 of the 19
	// registrations land here: the soft hammer (:334) rides softHammerBuilder, the
	// magnifying glass (:332) is the lens-prefix row whose port anchor is
	// magnifyingGlassBuilder. THE HANDLE GATE IS THE DECLARED DEVIATION: upstream binds
	// the stick to the head's mHandleMaterial (and its mToThis aliases, :66-83); the
	// port relaxes to ANY wooden rod tag (Tags.Items.RODS_WOODEN) — the per-material
	// handle universe is too strict under the modern material set (an iron head demands
	// an iron-handle stick upstream). The result rides the gt6:material_tool stamp seam
	// with the item's own durabilityMultiplier() (the gem pick ×0.25 — the optional
	// serializer field the :65 javadoc pre-declared). The upstream RM.ToolHeads machine
	// mirror (:66-83 addRecipeX) stays DEFERRED: the port has no ACT machine face
	// consuming the map (GT6RecipeMaps.java:1726 declared-empty), and a pour JSON for a
	// consumerless map is ceremony.
	// -----------------------------------------------------------------------

	/** One assembly form: the id, the head prefix, the :333 soft-tag gate. A method face — see {@link #assemblyForms}. */
	record AssemblyForm(String aId, gregapi.oredict.OreDictPrefix aHead, boolean aNoSoftTag) {}

	/** The 17 assembly forms — the upstream :332-350 list minus the soft hammer (:334, ported) and the magnifying glass (:332, lens prefix), in upstream order. */
	static java.util.List<AssemblyForm> assemblyForms() {
		return java.util.List.of(
				new AssemblyForm("hard_hammer", gregapi.data.OP.toolHeadHammer, true), // :333
				new AssemblyForm("sword", gregapi.data.OP.toolHeadSword, false), // :335
				new AssemblyForm("builder_wand", gregapi.data.OP.toolHeadBuilderwand, false), // :336
				new AssemblyForm("pickaxe_construction", gregapi.data.OP.toolHeadConstructionPickaxe, false), // :337
				new AssemblyForm("pickaxe_gem", gregapi.data.OP.toolHeadPickaxeGem, false), // :338
				new AssemblyForm("pickaxe", gregapi.data.OP.toolHeadPickaxe, false), // :339
				new AssemblyForm("shovel", gregapi.data.OP.toolHeadShovel, false), // :340
				new AssemblyForm("spade", gregapi.data.OP.toolHeadSpade, false), // :341
				new AssemblyForm("axe", gregapi.data.OP.toolHeadAxe, false), // :342
				new AssemblyForm("axe_double", gregapi.data.OP.toolHeadAxeDouble, false), // :343
				new AssemblyForm("hoe", gregapi.data.OP.toolHeadHoe, false), // :344
				new AssemblyForm("sense", gregapi.data.OP.toolHeadSense, false), // :345
				new AssemblyForm("plow", gregapi.data.OP.toolHeadPlow, false), // :346
				new AssemblyForm("file", gregapi.data.OP.toolHeadFile, false), // :347
				new AssemblyForm("chisel", gregapi.data.OP.toolHeadChisel, false), // :348
				new AssemblyForm("screwdriver", gregapi.data.OP.toolHeadScrewdriver, false), // :349
				new AssemblyForm("saw", gregapi.data.OP.toolHeadSaw, false)); // :350
	}

	/** The material face of one assembly row (the test-visible walk unit). */
	record AssemblyRow(AssemblyForm aForm, gregapi.oredict.OreDictMaterial aMaterial, String aSnake) {}

	/**
	 * The assembly walk face (offline-pure): per form, every registered head item (the
	 * registrationOrder membership = the head item truth) minus the :333 soft-tag gate.
	 * The handle gate is the declared wood-rod relaxation — no material face beyond the
	 * head truth.
	 */
	static java.util.List<AssemblyRow> toolAssemblyRows() {
		java.util.Map<gregapi.oredict.OreDictPrefix, java.util.List<OreDictMaterial>> tByHead = new java.util.HashMap<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tByHead.computeIfAbsent(tPair.prefix(), tKey -> new ArrayList<>()).add(tPair.material());
		}
		java.util.List<AssemblyRow> rRows = new ArrayList<>();
		for (AssemblyForm tForm : assemblyForms()) {
			for (OreDictMaterial tMaterial : tByHead.getOrDefault(tForm.aHead(), java.util.List.of())) {
				if (tForm.aNoSoftTag() && (tMaterial.contains(gregapi.data.TD.Properties.WOOD)
						|| tMaterial.contains(gregapi.data.TD.Properties.BOUNCY)
						|| tMaterial.contains(gregapi.data.TD.Properties.STRETCHY))) continue; // :333 Nor(WOOD, BOUNCY, STRETCHY)
				rRows.add(new AssemblyRow(tForm, tMaterial, GTMaterialItems.snakeCase(tMaterial.mNameInternal)));
			}
		}
		return rRows;
	}

	/** The row id — the craftFrom <output>_from_<input> convention over the material leaf. */
	static ResourceLocation assemblyRowId(String aForm, String aSnake) {
		return new ResourceLocation(GT6DataGenerators.MOD_ID, aForm + "_from_head/" + aSnake);
	}

	/** The assembly's tool face (the GT6Tools registry; the switch IS the 17-form ledger). */
	private static net.minecraft.world.item.Item assemblyToolItem(String aForm) {
		return switch (aForm) {
			case "hard_hammer" -> GT6Tools.HAMMER.get();
			case "sword" -> GT6Tools.SWORD.get();
			case "builder_wand" -> GT6Tools.BUILDER_WAND.get();
			case "pickaxe_construction" -> GT6Tools.PICKAXE_CONSTRUCTION.get();
			case "pickaxe_gem" -> GT6Tools.PICKAXE_GEM.get();
			case "pickaxe" -> GT6Tools.PICKAXE.get();
			case "shovel" -> GT6Tools.SHOVEL.get();
			case "spade" -> GT6Tools.SPADE.get();
			case "axe" -> GT6Tools.AXE.get();
			case "axe_double" -> GT6Tools.AXE_DOUBLE.get();
			case "hoe" -> GT6Tools.HOE.get();
			case "sense" -> GT6Tools.SENSE.get();
			case "plow" -> GT6Tools.PLOW.get();
			case "file" -> GT6Tools.FILE.get();
			case "chisel" -> GT6Tools.CHISEL.get();
			case "screwdriver" -> GT6Tools.SCREWDRIVER.get();
			case "saw" -> GT6Tools.SAW.get();
			default -> throw new IllegalArgumentException("unknown assembly form: " + aForm);
		};
	}

	/** The assembly's head-item face — null = the head item miss (no row, never an unresolvable ingredient). */
	private static net.minecraft.world.item.Item assemblyHeadItem(AssemblyRow aRow) {
		return gregtech6.registry.GTMaterialItems.get(aRow.aForm().aHead(), aRow.aMaterial()) == null ? null
				: gregtech6.registry.GTMaterialItems.get(aRow.aForm().aHead(), aRow.aMaterial()).get();
	}

	/** The stamp multiplier: the item's own LadderTool face (the gem pick ×0.25), 1.0 otherwise. */
	private static float assemblyMultiplier(net.minecraft.world.item.Item aTool) {
		return aTool instanceof gregtech6.items.tools.GT6ToolLadder.LadderTool tLadder ? tLadder.durabilityMultiplier() : 1.0F;
	}

//? if forge {
	private void toolAssemblyRows(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		for (AssemblyRow tRow : toolAssemblyRows()) {
			net.minecraft.world.item.Item tTool = assemblyToolItem(tRow.aForm().aId());
			net.minecraft.world.item.Item tHead = assemblyHeadItem(tRow);
			if (tHead == null) continue; // the head item miss — no row (never an unresolvable ingredient)
			ResourceLocation tId = assemblyRowId(tRow.aForm().aId(), tRow.aSnake());
			net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
					.recipeAdvancement()
					.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
					.addCriterion("has_head", has(tHead))
					.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
					.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
					.requirements(net.minecraft.advancements.RequirementsStrategy.OR);
			aConsumer.accept(new MaterialToolRow(tId, tId.withPrefix("recipes/tools/"),
					net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
					java.util.List.of("H", "S"),
					java.util.Map.of('H', net.minecraft.world.item.crafting.Ingredient.of(tHead),
							'S', net.minecraft.world.item.crafting.Ingredient.of(Tags.Items.RODS_WOODEN)),
					tTool, tRow.aSnake(), assemblyMultiplier(tTool), tAdvancement));
		}
	}

//?} else {
/*	private void toolAssemblyRows(net.minecraft.data.recipes.RecipeOutput aOutput) {
		for (AssemblyRow tRow : toolAssemblyRows()) {
			net.minecraft.world.item.Item tTool = assemblyToolItem(tRow.aForm().aId());
			net.minecraft.world.item.Item tHead = assemblyHeadItem(tRow);
			if (tHead == null) continue; // the head item miss — no row (never an unresolvable ingredient)
			ResourceLocation tId = assemblyRowId(tRow.aForm().aId(), tRow.aSnake());
			net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
					.recipeAdvancement()
					.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
					.addCriterion("has_head", has(tHead))
					.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
					.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
					.requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR);
			gregtech6.items.tools.GT6MaterialToolRecipe tRecipe = new gregtech6.items.tools.GT6MaterialToolRecipe("",
					net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
					net.minecraft.world.item.crafting.ShapedRecipePattern.of(
							java.util.Map.of('H', net.minecraft.world.item.crafting.Ingredient.of(tHead),
									'S', net.minecraft.world.item.crafting.Ingredient.of(Tags.Items.RODS_WOODEN)),
							java.util.List.of("H", "S")),
					new net.minecraft.world.item.ItemStack(tTool), true, tRow.aSnake(), assemblyMultiplier(tTool));
			aOutput.accept(tId, tRecipe, tAdvancement.build(tId.withPrefix("recipes/tools/")));
		}
	}

*///?}

	// -----------------------------------------------------------------------
	// The CraftFrom band (task craftfrom-plategem) — the upstream plateGem/plateGemTiny
	// hand-craft family (Loader_OreProcessing.java:171-178): every row is the same 2x2
	// frame "s "/" X" — 's' = the saw tool letter (the spray-can band translation),
	// 'X' = the same-material input. The upstream IOreDictListenerEvent dispatch rides
	// the OUTPUT prefix (:512-556) — no 1.20.1 face, so the digLadder band translation
	// applies: the registrationOrder walk replaces the listener trigger, the
	// And(ANTIMATTER.NOT, COATED.NOT) row condition folds to the material filter (the
	// digLadderMaterials :2861-2862 form), the upstream ConfigsGT.RECIPES per-material
	// config gate (:548, default T) is dropped, and the never-null dat() descriptor
	// (upstream OreDictPrefix.java:555-557 — rows upstream could silently lack their
	// input item) closes on the ITEM TRUTH: a material rows only when BOTH prefixes'
	// items exist (the declared semantic-equivalence deviation).
	// -----------------------------------------------------------------------

	/** One upstream row form: the id key + the output prefix + count + the input prefix (Loader_OreProcessing.java:171-178, the amounts verbatim). Package-private for the pin test. */
	record CraftFromForm(String aKey, gregapi.oredict.OreDictPrefix aOutput, int aCount, gregapi.oredict.OreDictPrefix aInput) {}

	/**
	 * The eight row forms of the upstream family (Loader_OreProcessing.java:171-178): :178
	 * the boule cut (1 boule + saw → 3 plateGem), :171 the plate split (plateGem + saw → 8
	 * plateGemTiny), :172-177 the six gem tiers (chipped/flawed/regular → plateGemTiny
	 * 2/4/8, flawless/exquisite/legendary → plateGem 1/3/7). A method, not a field — the
	 * OP fields live only after OP.init (the class-load-order guard). The keys are the
	 * upstream category names lowercased (the ResourceLocation path charset — vanilla
	 * rejects uppercase).
	 *
	 * <p>THE UNIVERSE IS THE ITEM TRUTH (coordinator ruling A on the declared口径
	 * conflict): the plateGem/plateGemTiny faces measure 205 materials (the whole
	 * GEMS∧PLATES walk — the research card's "11" misread the ForceTest quartet pins as
	 * the full set; the p8 census "PlateGem 203" agrees) and the gem-tier faces 109
	 * (regular gem 201). The forms table carries the FULL upstream set — the
	 * registrationOrder intersection walk does the scoping, no human-made subsets.
	 */
	static List<CraftFromForm> craftFromForms() {
		return List.of(
				new CraftFromForm("boule2plate_gem", gregapi.data.OP.plateGem, 3, gregapi.data.OP.bouleGt),
				new CraftFromForm("plate2plate_tiny", gregapi.data.OP.plateGemTiny, 8, gregapi.data.OP.plateGem),
				new CraftFromForm("gem2plate_gem/chipped", gregapi.data.OP.plateGemTiny, 2, gregapi.data.OP.gemChipped),
				new CraftFromForm("gem2plate_gem/flawed", gregapi.data.OP.plateGemTiny, 4, gregapi.data.OP.gemFlawed),
				new CraftFromForm("gem2plate_gem/regular", gregapi.data.OP.plateGemTiny, 8, gregapi.data.OP.gem),
				new CraftFromForm("gem2plate_gem/flawless", gregapi.data.OP.plateGem, 1, gregapi.data.OP.gemFlawless),
				new CraftFromForm("gem2plate_gem/exquisite", gregapi.data.OP.plateGem, 3, gregapi.data.OP.gemExquisite),
				new CraftFromForm("gem2plate_gem/legendary", gregapi.data.OP.plateGem, 7, gregapi.data.OP.gemLegendary));
	}

	/** The material face of one band row (the test-visible walk unit). */
	record CraftFromMaterialRow(CraftFromForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/**
	 * The material face of the band: per form, the materials whose INPUT and OUTPUT items
	 * both exist (the registrationOrder intersection, in registration order) minus the
	 * COATED/ANTIMATTER condition rows.
	 */
	static List<CraftFromMaterialRow> craftFromMaterialRows() {
		List<CraftFromMaterialRow> rRows = new ArrayList<>();
		for (CraftFromForm tForm : craftFromForms()) {
			java.util.Set<OreDictMaterial> tInputs = new java.util.HashSet<>();
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				if (tPair.prefix() == tForm.aInput()) tInputs.add(tPair.material());
			}
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tInputs.contains(tMaterial)) continue; // the item-truth intersection
				if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
				if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
				rRows.add(new CraftFromMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** The row id — the digLadderRowId form: the form key + the material leaf. Package-private for the pin test. */
	static ResourceLocation craftFromRowId(String aKey, String aSnake) {
		return new ResourceLocation(GT6DataGenerators.MOD_ID, aKey + "/" + aSnake);
	}

	/** The datagen row: the id + the output item + count + the input item. */
	private record CraftFromRow(ResourceLocation aId, net.minecraft.world.item.Item aResult, int aCount, net.minecraft.world.item.Item aInput) {}

	/** The datagen face: the material walk resolved onto the live items (the silent-skip guard rides itemOrNull). */
	private List<CraftFromRow> craftFromDatagenRows() {
		List<CraftFromRow> rRows = new ArrayList<>();
		for (CraftFromMaterialRow tMaterialRow : craftFromMaterialRows()) {
			String tSnake = GTMaterialItems.snakeCase(tMaterialRow.aMaterial().mNameInternal);
			net.minecraft.world.item.Item tResult = itemOrNull(tMaterialRow.aForm().aOutput(), tMaterialRow.aMaterial());
			net.minecraft.world.item.Item tInput = itemOrNull(tMaterialRow.aForm().aInput(), tMaterialRow.aMaterial());
			if (tResult == null || tInput == null) continue; // the item-truth guard (belt and braces over the walk)
			rRows.add(new CraftFromRow(craftFromRowId(tMaterialRow.aForm().aKey(), tSnake), tResult, tMaterialRow.aForm().aCount(), tInput));
		}
		return rRows;
	}

	/** One row's builder — the shared 2x2 frame, 's' = the saw tag (the spray-can band translation), 'X' = the input item. */
	private ShapedRecipeBuilder craftFromBuilder(CraftFromRow aRow) {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aRow.aResult(), aRow.aCount())
				.pattern("s ")
				.pattern(" X")
				.define('s', GT6ItemTags.TOOLS_SAW)
				.define('X', aRow.aInput())
				.unlockedBy("has_input", has(aRow.aInput()));
	}

	// -----------------------------------------------------------------------
	// The stick CraftFrom band (task craftfrom-stick) — the upstream stick/
	// stickLong hand-craft family (Loader_OreProcessing.java:156-163, the
	// coordinator-approved family boundary: BOTH stick* output prefixes, the
	// P36 plategem card's plateGem+plateGemTiny companion shape). Eight row
	// forms in three grids, all And(ANTIMATTER.NOT, COATED.NOT):
	//  - :156-158 gem2stickLong — {"sf"," X"}: saw + file + gem tier →
	//    stickLong 1/2/4 (flawless/exquisite/legendary);
	//  - :159 stickLong2stick — {"s "," X"}: saw + stickLong → stick 2;
	//  - :160-163 gem2stick — {"s ","fX"}: saw + file + gem tier → stick
	//    1/2/4/8 (regular/flawless/exquisite/legendary).
	// The same digLadder band translation as the plateGem family above (the
	// listener walk, the condition fold, the config-gate drop, the item-truth
	// intersection) — an independent band, per the P36 census ruling that the
	// CraftFrom families share no infrastructure. Tool letters 's'/'f' = the
	// saw/file tool tags (upstream CR.java:211/:231).
	// -----------------------------------------------------------------------

	/** One upstream row form: the id key + the output prefix + count + the input prefix + the two pattern rows (Loader_OreProcessing.java:156-163, the grids and amounts verbatim). Package-private for the pin test. */
	record StickCraftFromForm(String aKey, gregapi.oredict.OreDictPrefix aOutput, int aCount, gregapi.oredict.OreDictPrefix aInput, String aTop, String aBottom) {}

	/**
	 * The eight row forms of the stick family (Loader_OreProcessing.java:156-163): :156-158 the
	 * gem-tier cuts to stickLong (1/2/4), :159 the stickLong split to stick (2), :160-163 the
	 * gem-tier cuts to stick (regular gem 1, flawless 2, exquisite 4, legendary 8). A method,
	 * not a field — the OP fields live only after OP.init. The keys are the upstream category
	 * names snake-cased ("gem2stickLong" → gem2stick_long, the plateGem card's charset rule)
	 * with the tier leaf, and the universe rides the same item-truth walk — no human subsets.
	 */
	static List<StickCraftFromForm> stickCraftFromForms() {
		return List.of(
				new StickCraftFromForm("gem2stick_long/flawless", gregapi.data.OP.stickLong, 1, gregapi.data.OP.gemFlawless, "sf", " X"),
				new StickCraftFromForm("gem2stick_long/exquisite", gregapi.data.OP.stickLong, 2, gregapi.data.OP.gemExquisite, "sf", " X"),
				new StickCraftFromForm("gem2stick_long/legendary", gregapi.data.OP.stickLong, 4, gregapi.data.OP.gemLegendary, "sf", " X"),
				new StickCraftFromForm("stick_long2stick", gregapi.data.OP.stick, 2, gregapi.data.OP.stickLong, "s ", " X"),
				new StickCraftFromForm("gem2stick/regular", gregapi.data.OP.stick, 1, gregapi.data.OP.gem, "s ", "fX"),
				new StickCraftFromForm("gem2stick/flawless", gregapi.data.OP.stick, 2, gregapi.data.OP.gemFlawless, "s ", "fX"),
				new StickCraftFromForm("gem2stick/exquisite", gregapi.data.OP.stick, 4, gregapi.data.OP.gemExquisite, "s ", "fX"),
				new StickCraftFromForm("gem2stick/legendary", gregapi.data.OP.stick, 8, gregapi.data.OP.gemLegendary, "s ", "fX"));
	}

	/** The material face of one stick-band row (the test-visible walk unit). */
	record StickCraftFromMaterialRow(StickCraftFromForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/**
	 * The material face of the stick band: per form, the materials whose INPUT and OUTPUT
	 * items both exist (the registrationOrder intersection, in registration order) minus the
	 * COATED/ANTIMATTER condition rows — the plateGem walk verbatim.
	 */
	static List<StickCraftFromMaterialRow> stickCraftFromMaterialRows() {
		List<StickCraftFromMaterialRow> rRows = new ArrayList<>();
		for (StickCraftFromForm tForm : stickCraftFromForms()) {
			java.util.Set<OreDictMaterial> tInputs = new java.util.HashSet<>();
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				if (tPair.prefix() == tForm.aInput()) tInputs.add(tPair.material());
			}
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tInputs.contains(tMaterial)) continue; // the item-truth intersection
				if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
				if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
				rRows.add(new StickCraftFromMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** The datagen row: the id + the output item + count + the input item + the pattern rows. */
	private record StickCraftFromRow(ResourceLocation aId, net.minecraft.world.item.Item aResult, int aCount, net.minecraft.world.item.Item aInput, String aTop, String aBottom) {}

	/** The datagen face: the material walk resolved onto the live items (the silent-skip guard rides itemOrNull). */
	private List<StickCraftFromRow> stickCraftFromDatagenRows() {
		List<StickCraftFromRow> rRows = new ArrayList<>();
		for (StickCraftFromMaterialRow tMaterialRow : stickCraftFromMaterialRows()) {
			String tSnake = GTMaterialItems.snakeCase(tMaterialRow.aMaterial().mNameInternal);
			net.minecraft.world.item.Item tResult = itemOrNull(tMaterialRow.aForm().aOutput(), tMaterialRow.aMaterial());
			net.minecraft.world.item.Item tInput = itemOrNull(tMaterialRow.aForm().aInput(), tMaterialRow.aMaterial());
			if (tResult == null || tInput == null) continue; // the item-truth guard (belt and braces over the walk)
			rRows.add(new StickCraftFromRow(craftFromRowId(tMaterialRow.aForm().aKey(), tSnake), tResult, tMaterialRow.aForm().aCount(), tInput,
					tMaterialRow.aForm().aTop(), tMaterialRow.aForm().aBottom()));
		}
		return rRows;
	}

	/** One row's builder — the form's own two-row grid, 's' = the saw tag, 'f' = the file tag (the shape-driven defines), 'X' = the input item. */
	private ShapedRecipeBuilder stickCraftFromBuilder(StickCraftFromRow aRow) {
		ShapedRecipeBuilder rBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aRow.aResult(), aRow.aCount())
				.pattern(aRow.aTop())
				.pattern(aRow.aBottom())
				.define('s', GT6ItemTags.TOOLS_SAW)
				.define('X', aRow.aInput())
				.unlockedBy("has_input", has(aRow.aInput()));
		if (aRow.aTop().indexOf('f') >= 0 || aRow.aBottom().indexOf('f') >= 0) rBuilder.define('f', GT6ItemTags.TOOLS_FILE);
		return rBuilder;
	}

	// -----------------------------------------------------------------------
	// The fine-wire CraftFrom band (task craftfrom-foil — the coordinator
	// fine-wire-batch ruling: the "foil family" upstream OUTPUT face is the
	// EMPTY set — foil never appears as a CraftFrom output in
	// Loader_OreProcessing.java, its production rides the AnvilBendSmall/
	// ClusterMill/Extruder machine domain, Loader_Recipes_Handlers.java:213/
	// :309-311/:768/:801 — so this batch merges the two fine-wire/wire-domain
	// rows of the residual pool):
	//  - :168 foil2wireFine — {"Xx"}: wire cutter + foil → wireFine 1;
	//  - :169 plate2wire — {"Px"}: wire cutter + plate → wireGt01 1 (the X
	//    slot rides the null-SpecialPrefix plate default, :535).
	// The same digLadder band translation as the plateGem/stick families above
	// (the listener walk, the And(ANTIMATTER.NOT, COATED.NOT) fold, the
	// config-gate drop, the item-truth intersection). Tool letter 'x' = the
	// wire-cutter tag (upstream CR.java:359). THE :169 ROWS POUR ZERO today —
	// the wireGt01 output face lives in the GTWires block domain (one
	// BlockItem per band block, the material rides the blockstate — no
	// per-material MaterialPrefixItems), the GT6RecipesWiremill seam verbatim;
	// the form stays in the table so the rows unlock with that item family.
	// -----------------------------------------------------------------------

	/** One upstream row form: the id key + the output prefix + count + the input prefix + the single pattern row (Loader_OreProcessing.java:168-169, the grid and amount verbatim). Package-private for the pin test. */
	record FineWireCraftFromForm(String aKey, gregapi.oredict.OreDictPrefix aOutput, int aCount, gregapi.oredict.OreDictPrefix aInput, String aRow) {}

	/**
	 * The two row forms of the fine-wire batch (Loader_OreProcessing.java:168-169). A method,
	 * not a field — the OP fields live only after OP.init. The keys are the upstream category
	 * names snake-cased ("foil2wireFine" → foil2wire_fine, "plate2wire" already lowercase),
	 * and the universe rides the same item-truth walk — no human subsets.
	 */
	static List<FineWireCraftFromForm> fineWireCraftFromForms() {
		return List.of(
				new FineWireCraftFromForm("foil2wire_fine", gregapi.data.OP.wireFine, 1, gregapi.data.OP.foil, "Xx"),
				new FineWireCraftFromForm("plate2wire", gregapi.data.OP.wireGt01, 1, gregapi.data.OP.plate, "Px"));
	}

	/** The material face of one fine-wire-band row (the test-visible walk unit). */
	record FineWireCraftFromMaterialRow(FineWireCraftFromForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/**
	 * The material face of the fine-wire band: per form, the materials whose INPUT and OUTPUT
	 * items both exist (the registrationOrder intersection, in registration order) minus the
	 * COATED/ANTIMATTER condition rows — the stick walk verbatim.
	 */
	static List<FineWireCraftFromMaterialRow> fineWireCraftFromMaterialRows() {
		List<FineWireCraftFromMaterialRow> rRows = new ArrayList<>();
		for (FineWireCraftFromForm tForm : fineWireCraftFromForms()) {
			java.util.Set<OreDictMaterial> tInputs = new java.util.HashSet<>();
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				if (tPair.prefix() == tForm.aInput()) tInputs.add(tPair.material());
			}
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tInputs.contains(tMaterial)) continue; // the item-truth intersection
				if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
				if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
				rRows.add(new FineWireCraftFromMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** The datagen row: the id + the output item + count + the input item + the pattern row. */
	private record FineWireCraftFromRow(ResourceLocation aId, net.minecraft.world.item.Item aResult, int aCount, net.minecraft.world.item.Item aInput, String aRow) {}

	/** The datagen face: the material walk resolved onto the live items (the silent-skip guard rides itemOrNull). */
	private List<FineWireCraftFromRow> fineWireCraftFromDatagenRows() {
		List<FineWireCraftFromRow> rRows = new ArrayList<>();
		for (FineWireCraftFromMaterialRow tMaterialRow : fineWireCraftFromMaterialRows()) {
			String tSnake = GTMaterialItems.snakeCase(tMaterialRow.aMaterial().mNameInternal);
			net.minecraft.world.item.Item tResult = itemOrNull(tMaterialRow.aForm().aOutput(), tMaterialRow.aMaterial());
			net.minecraft.world.item.Item tInput = itemOrNull(tMaterialRow.aForm().aInput(), tMaterialRow.aMaterial());
			if (tResult == null || tInput == null) continue; // the item-truth guard (belt and braces over the walk)
			rRows.add(new FineWireCraftFromRow(craftFromRowId(tMaterialRow.aForm().aKey(), tSnake), tResult, tMaterialRow.aForm().aCount(), tInput,
					tMaterialRow.aForm().aRow()));
		}
		return rRows;
	}

	/** One row's builder — the single-row grid, the input letter = the row's leading char ('X'/:168, 'P'/:169), 'x' = the wire-cutter tag. */
	private ShapedRecipeBuilder fineWireCraftFromBuilder(FineWireCraftFromRow aRow) {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aRow.aResult(), aRow.aCount())
				.pattern(aRow.aRow())
				.define(aRow.aRow().charAt(0), aRow.aInput())
				.define('x', GT6ItemTags.TOOLS_WIRE_CUTTER)
				.unlockedBy("has_input", has(aRow.aInput()));
	}

	// -----------------------------------------------------------------------
	// The rockGt CraftFrom band (task craftfrom-rockgt) — the rockGt batch
	// of the 47-statement CraftFrom panorama (the fine-wire card's residual
	// ledger): :148 is the ONLY rockGt statement of the shaped panel (:322/
	// :323/:338 ride the Crusher/Hammer/ByProductList machine domain), and
	// :149-150 output gearGt from stick — the gearGt output prefix is not this
	// family, the residual closeout card owns them. The single form:
	//  - :148 rock_gt2gear_gt — {"XYX","YfY","XYX"}: file + 4x rockGt (the
	//    corners) + 4x stone (the edges) → gearGt 1 — the FIRST band with a
	//    POSITIVE material flag: And(ANTIMATTER.NOT, COATED.NOT, STONE,
	//    MT.Stone.NOT, MT.Bedrock.NOT) verbatim (the STONE rock face plus the
	//    vanilla-covered Stone/Bedrock identity exclusions).
	// The same digLadder band translation as the plateGem/stick/fine-wire
	// families above (the listener walk, the config-gate drop — :148 carries
	// the null category — the item-truth intersection extended to the three
	// faces). DECLARED CARRIER DEVIATION: upstream 'Y' is the OP.stone
	// oredict key satisfied by every GTStoneBlocks variant of the family
	// (BlockStones OM.reg, the GTStoneBlocks.oreDictMappings face) — the port
	// keys the row on the family's STONE-variant block item (the mappings'
	// first face, the anvil band's explicit-carrier precedent), and the walk
	// face stays offline-pure on the family MEMBERSHIP (the StoneSpec
	// supplier) while the datagen face resolves the RegistryObject item.
	// Non-family stones (MT.STONES.Blackstone, Deepslate, ...) carry no stone
	// item yet — their rows pour zero today and unlock with that bridge. The
	// key composes from the prefixes (rockGt→rock_gt, gearGt→gear_gt, the
	// stick-card snake law) because :148 has the null category. Tool letter
	// 'f' = the file tag (upstream CR.java:231).
	// -----------------------------------------------------------------------

	/** One upstream row form: the id key + the output prefix + count + the input prefix + the companion prefix + the three pattern rows (Loader_OreProcessing.java:148, the grid and amount verbatim). Package-private for the pin test. */
	record RockGtCraftFromForm(String aKey, gregapi.oredict.OreDictPrefix aOutput, int aCount,
			gregapi.oredict.OreDictPrefix aInput, gregapi.oredict.OreDictPrefix aCompanion, String[] aRows) {}

	/**
	 * The single row form of the rockGt batch (Loader_OreProcessing.java:148). A method, not a
	 * field — the OP fields live only after OP.init. The X slot = rockGt (4 corners), the Y
	 * slot = stone (4 edges), 'f' = the file tool (center).
	 */
	static List<RockGtCraftFromForm> rockGtCraftFromForms() {
		return List.of(new RockGtCraftFromForm("rock_gt2gear_gt", gregapi.data.OP.gearGt, 1,
				gregapi.data.OP.rockGt, gregapi.data.OP.stone, new String[] {"XYX", "YfY", "XYX"}));
	}

	/** The material face of one rockGt-band row (the test-visible walk unit). */
	record RockGtCraftFromMaterialRow(RockGtCraftFromForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/**
	 * The material face of the rockGt band: the materials whose gearGt AND rockGt items both
	 * exist (the registrationOrder intersection, in registration order) AND whose stone
	 * companion rides a GTStoneBlocks family (the stone face is block-path, outside the item
	 * walk) — minus the COATED/ANTIMATTER rows, plus the STONE flag, minus Stone/Bedrock (the
	 * :148 condition verbatim). The stick walk with the three-face extension.
	 */
	static List<RockGtCraftFromMaterialRow> rockGtCraftFromMaterialRows() {
		List<RockGtCraftFromMaterialRow> rRows = new ArrayList<>();
		for (RockGtCraftFromForm tForm : rockGtCraftFromForms()) {
			java.util.Set<OreDictMaterial> tInputs = new java.util.HashSet<>();
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				if (tPair.prefix() == tForm.aInput()) tInputs.add(tPair.material());
			}
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tInputs.contains(tMaterial)) continue; // the item-truth intersection
				if (stoneFamilyOrNull(tMaterial) == null) continue; // the stone companion item truth (the GTStoneBlocks family face)
				if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
				if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
				if (!tMaterial.contains(gregapi.data.TD.Properties.STONE)) continue; // STONE — the positive rock face
				if (tMaterial == gregapi.data.MT.Stone) continue; // MT.Stone.NOT
				if (tMaterial == gregapi.data.MT.Bedrock) continue; // MT.Bedrock.NOT
				rRows.add(new RockGtCraftFromMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** The OP.stone.dat(M) family face — the GTStoneBlocks family whose material is M (offline-pure: the StoneSpec supplier), null off the 17-family universe. Package-private for the pin test. */
	static gregtech6.registry.GTStoneBlocks.StoneSpec stoneFamilyOrNull(OreDictMaterial aMaterial) {
		for (gregtech6.registry.GTStoneBlocks.StoneSpec tStone : gregtech6.registry.GTStoneBlocks.STONES) {
			if (tStone.material().get() == aMaterial) return tStone;
		}
		return null;
	}

	/** The datagen row: the id + the output item + count + the two input items + the pattern rows. */
	private record RockGtCraftFromRow(ResourceLocation aId, net.minecraft.world.item.Item aResult, int aCount,
			net.minecraft.world.item.Item aInput, net.minecraft.world.item.Item aCompanion, String[] aRows) {}

	/** The datagen face: the material walk resolved onto the live items (the silent-skip guard rides itemOrNull and the stone handle). */
	private List<RockGtCraftFromRow> rockGtCraftFromDatagenRows() {
		List<RockGtCraftFromRow> rRows = new ArrayList<>();
		for (RockGtCraftFromMaterialRow tMaterialRow : rockGtCraftFromMaterialRows()) {
			String tSnake = GTMaterialItems.snakeCase(tMaterialRow.aMaterial().mNameInternal);
			net.minecraft.world.item.Item tResult = itemOrNull(tMaterialRow.aForm().aOutput(), tMaterialRow.aMaterial());
			net.minecraft.world.item.Item tInput = itemOrNull(tMaterialRow.aForm().aInput(), tMaterialRow.aMaterial());
			gregtech6.registry.GTStoneBlocks.StoneSpec tStoneFamily = stoneFamilyOrNull(tMaterialRow.aMaterial());
			if (tResult == null || tInput == null || tStoneFamily == null) continue; // the item-truth guard (belt and braces over the walk)
			rRows.add(new RockGtCraftFromRow(craftFromRowId(tMaterialRow.aForm().aKey(), tSnake), tResult, tMaterialRow.aForm().aCount(),
					tInput, gregtech6.registry.GTStoneBlocks.item(tStoneFamily.snake(), gregtech6.block.stone.StoneVariant.STONE).get(),
					tMaterialRow.aForm().aRows()));
		}
		return rRows;
	}

	/** One row's builder — the form's 3x3 grid, 'f' = the file tag, 'X' = the rockGt item, 'Y' = the stone item. */
	private ShapedRecipeBuilder rockGtCraftFromBuilder(RockGtCraftFromRow aRow) {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aRow.aResult(), aRow.aCount())
				.pattern(aRow.aRows()[0])
				.pattern(aRow.aRows()[1])
				.pattern(aRow.aRows()[2])
				.define('X', aRow.aInput())
				.define('Y', aRow.aCompanion())
				.define('f', GT6ItemTags.TOOLS_FILE)
				.unlockedBy("has_input", has(aRow.aInput()));
	}

	// -----------------------------------------------------------------------
	// The residual CraftFrom bands (task craftfrom-residual — the 47-
	// statement panel closeout: the 28 statements the plategem/stick/fine-wire/
	// rockGt batches did not land). Five shaped bands + one shapeless band,
	// each an independent band per the census law, all riding the digLadder
	// band translation (the registrationOrder walk replaces the :512-556
	// listener trigger, the condition rows fold to material filters, the
	// ConfigsGT.RECIPES per-material config gate drops, the item-truth
	// intersection closes the never-null dat() seam on the live items —
	// extended to EVERY material-dependent input face of the row). Shared
	// leaf helpers below (itemTruth/gridHas/...) sit beside craftFromRowId —
	// walk utilities, not band infrastructure.
	//
	// Tool letters = the upstream CR.java:342-359 oredict keys: 'd'
	// screwdriver, 'f' file, 'h' the HARD hammer (craftingToolHardHammer,
	// the tool-hammer-wrench ruling), 's' saw, 'w' wrench, 'x' wire
	// cutter. The fixed uppercase vocabulary rides the :520-533 key table:
	// S = stick, P = plate, C = plateGem, T = screw (X/Y = the special
	// prefixes, null defaulting to plate :535-537).
	//
	// THE ZERO-ROW FORMS POUR ZERO HONESTLY (the fine-wire :169 precedent):
	// casingMachine+Double/Quadruple/Dense, cableGt01/02 and plank carry no
	// MaterialPrefixItems (upstream Loader_Items.java:57-171 never built a
	// PrefixItem for them — the MTE-block/plank domains), so their forms
	// stay in the tables and the rows unlock with those item families.
	// NULL-CATEGORY KEY LAW: snake(output) + a "/from_<distinguisher>" leaf
	// where the output face alone is ambiguous (the P36 leaf law; the
	// rockGt card's <in>2<out> glue stays untouched on its own form).
	// -----------------------------------------------------------------------

	/** The item-truth intersection of the given prefix faces — every listed face must carry the material's item (the registrationOrder walk). */
	static java.util.Set<OreDictMaterial> itemTruth(java.util.List<gregapi.oredict.OreDictPrefix> aFaces) {
		java.util.Set<OreDictMaterial> rFaces = null;
		for (gregapi.oredict.OreDictPrefix tFace : aFaces) {
			java.util.Set<OreDictMaterial> tFaceSet = new java.util.HashSet<>();
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				if (tPair.prefix() == tFace) tFaceSet.add(tPair.material());
			}
			if (rFaces == null) rFaces = tFaceSet; else rFaces.retainAll(tFaceSet);
		}
		return rFaces == null ? new java.util.HashSet<>() : rFaces;
	}

	/** Whether the item flood carries the exact prefix x material pair (the fixed-face guard, e.g. the MT.Empty arrow shaft). */
	static boolean itemPairExists(gregapi.oredict.OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() == aPrefix && tPair.material() == aMaterial) return true;
		}
		return false;
	}

	/** Whether the item flood carries the prefix for ANY member of the ANY alias group (membership = group.mToThis, the MT.java:4162 face). */
	static boolean anyGroupFaceExists(gregapi.oredict.OreDictPrefix aPrefix, OreDictMaterial aGroup) {
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() == aPrefix && aGroup.mToThis.contains(tPair.material())) return true;
		}
		return false;
	}

	/** Whether the grid uses the letter (the shape-driven define law, the stick band's 'f' arm generalized). */
	private static boolean gridHas(String[] aRows, char aLetter) {
		for (String tRow : aRows) if (tRow.indexOf(aLetter) >= 0) return true;
		return false;
	}

	/** The tool letters of a grid to their tags — CR.java:342-359 ('h' = the HARD hammer, the p25 ruling). */
	private static void defineTools(ShapedRecipeBuilder aBuilder, String[] aRows) {
		for (String tRow : aRows) for (int i = 0; i < tRow.length(); i++) {
			char tChar = tRow.charAt(i);
			TagKey<Item> tTag = switch (tChar) {
				case 'd' -> GT6ItemTags.TOOLS_SCREWDRIVER;
				case 'f' -> GT6ItemTags.TOOLS_FILE;
				case 'h' -> GT6ItemTags.TOOLS_HARD_HAMMER;
				case 's' -> GT6ItemTags.TOOLS_SAW;
				case 'w' -> GT6ItemTags.TOOLS_WRENCH;
				case 'x' -> GT6ItemTags.TOOLS_WIRE_CUTTER;
				default -> null;
			};
			if (tTag != null) aBuilder.define(tChar, tTag);
		}
	}

	/** Puts the resolved defines onto the builder (tool tags and items share the map). */
	@SuppressWarnings("unchecked")
	private void applyDefines(ShapedRecipeBuilder aBuilder, java.util.Map<Character, Object> aDefines) {
		for (java.util.Map.Entry<Character, Object> tEntry : aDefines.entrySet()) {
			if (tEntry.getValue() instanceof TagKey) aBuilder.define(tEntry.getKey(), (TagKey<Item>)tEntry.getValue());
			else aBuilder.define(tEntry.getKey(), (Item)tEntry.getValue());
		}
	}

	/** The fixed-vocabulary defines of a grid for a material: X = the primary input (null input = the :535 plate default, the rotor's X), S/P/C/T = the :520-533 fixed letters; tool letters ride defineTools separately. */
	private java.util.Map<Character, Object> craftFromDefines(OreDictMaterial aMaterial, gregapi.oredict.OreDictPrefix aInput, String[] aRows) {
		java.util.Map<Character, Object> rDefines = new java.util.LinkedHashMap<>();
		gregapi.oredict.OreDictPrefix tXFace = aInput != null ? aInput : gregapi.data.OP.plate; // the null-SpecialPrefix plate default :535
		for (String tRow : aRows) for (int i = 0; i < tRow.length(); i++) {
			char tChar = tRow.charAt(i);
			if (rDefines.containsKey(tChar)) continue;
			switch (tChar) {
				case 'X' -> rDefines.put(tChar, itemOrNull(tXFace, aMaterial));
				case 'S' -> rDefines.put(tChar, itemOrNull(gregapi.data.OP.stick, aMaterial));
				case 'P' -> rDefines.put(tChar, itemOrNull(gregapi.data.OP.plate, aMaterial));
				case 'C' -> rDefines.put(tChar, itemOrNull(gregapi.data.OP.plateGem, aMaterial));
				case 'T' -> rDefines.put(tChar, itemOrNull(gregapi.data.OP.screw, aMaterial));
				default -> {}
			}
		}
		return rDefines;
	}

	// -- Residual band 1: the gear face (:149-151) — :149 is the :148 rockGt
	// row's stick twin (the same grid and the same five-condition tail, the
	// family STONE-variant block item carrier), :150 the +SMITHABLE plated
	// twin (wrench center), :151 the plank cut to gearGtSmall (+MT.Wood.NOT;
	// the plank face carries no items — 0 rows, the form stays).

	/** One gear-face row form (Loader_OreProcessing.java:149-151, the grids and amounts verbatim). Package-private for the pin test. */
	record GearGtCraftFromForm(String aKey, gregapi.oredict.OreDictPrefix aOutput, int aCount,
			gregapi.oredict.OreDictPrefix aInput, gregapi.oredict.OreDictPrefix aCompanion,
			boolean aSmithable, boolean aStoneFace, boolean aNoWood, String[] aRows) {}

	/** The three row forms of the gear face (null upstream categories — the composed keys, the null-category key law). */
	static List<GearGtCraftFromForm> gearGtCraftFromForms() {
		return List.of(
				new GearGtCraftFromForm("gear_gt/from_stick_stone", gregapi.data.OP.gearGt, 1, gregapi.data.OP.stick, gregapi.data.OP.stone, false, true, false, new String[] {"XYX", "YfY", "XYX"}),
				new GearGtCraftFromForm("gear_gt/from_stick_plate", gregapi.data.OP.gearGt, 1, gregapi.data.OP.stick, gregapi.data.OP.plate, true, false, false, new String[] {"XYX", "YwY", "XYX"}),
				new GearGtCraftFromForm("gear_gt_small/from_plank", gregapi.data.OP.gearGtSmall, 1, gregapi.data.OP.plank, null, false, false, true, new String[] {"X ", " s"}));
	}

	/** The material face of one gear-face row (the test-visible walk unit). */
	record GearGtCraftFromMaterialRow(GearGtCraftFromForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/** The material face of the gear band: the three-face intersection under the per-form condition tails (:149-151 verbatim); the stone face rides the family (OP.stone is block-path, outside the item walk — the rockGt seam). */
	static List<GearGtCraftFromMaterialRow> gearGtCraftFromMaterialRows() {
		List<GearGtCraftFromMaterialRow> rRows = new ArrayList<>();
		for (GearGtCraftFromForm tForm : gearGtCraftFromForms()) {
			List<gregapi.oredict.OreDictPrefix> tFaces = new ArrayList<>();
			tFaces.add(tForm.aInput());
			if (tForm.aCompanion() != null && !tForm.aStoneFace()) tFaces.add(tForm.aCompanion()); // the plate third face; the stone face is block-path
			java.util.Set<OreDictMaterial> tInputs = itemTruth(tFaces);
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tInputs.contains(tMaterial)) continue; // the item-truth intersection
				if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
				if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
				if (tForm.aStoneFace()) {
					if (stoneFamilyOrNull(tMaterial) == null) continue; // the stone companion (block-path, the GTStoneBlocks family face)
					if (!tMaterial.contains(gregapi.data.TD.Properties.STONE)) continue; // STONE — the positive rock face
					if (tMaterial == gregapi.data.MT.Stone) continue; // MT.Stone.NOT
					if (tMaterial == gregapi.data.MT.Bedrock) continue; // MT.Bedrock.NOT
				}
				if (tForm.aSmithable() && !tMaterial.contains(gregapi.data.TD.Processing.SMITHABLE)) continue; // SMITHABLE — the positive smith face
				if (tForm.aNoWood() && tMaterial == gregapi.data.MT.Wood) continue; // MT.Wood.NOT
				rRows.add(new GearGtCraftFromMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** The datagen row: the id + the output item + count + the input item + the companion item + the pattern rows. */
	private record GearGtCraftFromRow(ResourceLocation aId, Item aResult, int aCount, Item aInput, Item aCompanion, String[] aRows) {}

	/** The datagen face: the material walk resolved onto the live items (the companion rides the family STONE block item on the stone face). */
	private List<GearGtCraftFromRow> gearGtCraftFromDatagenRows() {
		List<GearGtCraftFromRow> rRows = new ArrayList<>();
		for (GearGtCraftFromMaterialRow tMaterialRow : gearGtCraftFromMaterialRows()) {
			GearGtCraftFromForm tForm = tMaterialRow.aForm();
			String tSnake = GTMaterialItems.snakeCase(tMaterialRow.aMaterial().mNameInternal);
			Item tResult = itemOrNull(tForm.aOutput(), tMaterialRow.aMaterial());
			Item tInput = itemOrNull(tForm.aInput(), tMaterialRow.aMaterial());
			Item tCompanion = null;
			if (tForm.aStoneFace()) {
				gregtech6.registry.GTStoneBlocks.StoneSpec tStoneFamily = stoneFamilyOrNull(tMaterialRow.aMaterial());
				if (tStoneFamily != null) tCompanion = gregtech6.registry.GTStoneBlocks.item(tStoneFamily.snake(), gregtech6.block.stone.StoneVariant.STONE).get();
			} else if (tForm.aCompanion() != null) {
				tCompanion = itemOrNull(tForm.aCompanion(), tMaterialRow.aMaterial());
			}
			if (tResult == null || tInput == null || tCompanion == null) continue; // the item-truth guard (belt and braces over the walk)
			rRows.add(new GearGtCraftFromRow(craftFromRowId(tForm.aKey(), tSnake), tResult, tForm.aCount(), tInput, tCompanion, tForm.aRows()));
		}
		return rRows;
	}

	/** One row's builder — 'X' = the stick/plank input, 'Y' = the plate/stone companion, the shape-driven tool defines. */
	private ShapedRecipeBuilder gearGtCraftFromBuilder(GearGtCraftFromRow aRow) {
		ShapedRecipeBuilder rBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aRow.aResult(), aRow.aCount());
		for (String tPattern : aRow.aRows()) rBuilder.pattern(tPattern); // the 3x3 gear grids and the 2x2 plank cut
		rBuilder.define('X', aRow.aInput())
				.unlockedBy("has_input", has(aRow.aInput()));
		if (gridHas(aRow.aRows(), 'Y')) rBuilder.define('Y', aRow.aCompanion());
		defineTools(rBuilder, aRow.aRows());
		return rBuilder;
	}

	// -- Residual band 2: the rotor/buzzSaw faces (:145-147). :145 the rotor
	// ("YhY","TXf","YdY" — 4x curved plate, 1 plate, 1 screw, hammer+file+
	// screwdriver, +SMITHABLE); :146/:147 the buzzsaw blades off plate and
	// plateGem ("wPh","P P","fPx" / "wCh","C C","fCx" — wrench+hammer+file+
	// wire cutter). X rides the null-SpecialPrefix plate default :535.

	/** One rotor/buzzSaw row form (Loader_OreProcessing.java:145-147, the grids and amounts verbatim). Package-private for the pin test. */
	record ToolHeadCraftFromForm(String aKey, gregapi.oredict.OreDictPrefix aOutput, int aCount, boolean aSmithable, String[] aRows) {}

	/** The three row forms (the :145 category snake + the buzzSaw pair with the body-face leaf). */
	static List<ToolHeadCraftFromForm> toolHeadCraftFromForms() {
		return List.of(
				new ToolHeadCraftFromForm("rotor", gregapi.data.OP.rotor, 1, true, new String[] {"YhY", "TXf", "YdY"}),
				new ToolHeadCraftFromForm("tool_head_buzz_saw/plate", gregapi.data.OP.toolHeadBuzzSaw, 1, false, new String[] {"wPh", "P P", "fPx"}),
				new ToolHeadCraftFromForm("tool_head_buzz_saw/gem", gregapi.data.OP.toolHeadBuzzSaw, 1, false, new String[] {"wCh", "C C", "fCx"}));
	}

	/** The material face of one rotor/buzzSaw row (the test-visible walk unit). */
	record ToolHeadCraftFromMaterialRow(ToolHeadCraftFromForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/** The material face: the output ∩ the row's material-letter faces (Y = the curved plate, X/P = the plate, C = the plateGem, T = the screw) under the condition tails. */
	static List<ToolHeadCraftFromMaterialRow> toolHeadCraftFromMaterialRows() {
		List<ToolHeadCraftFromMaterialRow> rRows = new ArrayList<>();
		for (ToolHeadCraftFromForm tForm : toolHeadCraftFromForms()) {
			List<gregapi.oredict.OreDictPrefix> tFaces = new ArrayList<>();
			if (gridHas(tForm.aRows(), 'Y')) tFaces.add(gregapi.data.OP.plateCurved);
			tFaces.add(gridHas(tForm.aRows(), 'C') ? gregapi.data.OP.plateGem : gregapi.data.OP.plate);
			if (gridHas(tForm.aRows(), 'T')) tFaces.add(gregapi.data.OP.screw);
			java.util.Set<OreDictMaterial> tInputs = itemTruth(tFaces);
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tInputs.contains(tMaterial)) continue; // the item-truth intersection
				if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
				if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
				if (tForm.aSmithable() && !tMaterial.contains(gregapi.data.TD.Processing.SMITHABLE)) continue; // SMITHABLE
				rRows.add(new ToolHeadCraftFromMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** The datagen row: the id + the output item + count + the input item + the resolved defines + the pattern rows. */
	private record ToolHeadCraftFromRow(ResourceLocation aId, Item aResult, int aCount, Item aInput, java.util.Map<Character, Object> aDefines, String[] aRows) {}

	/** The datagen face: the material walk resolved onto the live items. */
	private List<ToolHeadCraftFromRow> toolHeadCraftFromDatagenRows() {
		List<ToolHeadCraftFromRow> rRows = new ArrayList<>();
		for (ToolHeadCraftFromMaterialRow tMaterialRow : toolHeadCraftFromMaterialRows()) {
			ToolHeadCraftFromForm tForm = tMaterialRow.aForm();
			OreDictMaterial tMaterial = tMaterialRow.aMaterial();
			String tSnake = GTMaterialItems.snakeCase(tMaterial.mNameInternal);
			Item tResult = itemOrNull(tForm.aOutput(), tMaterial);
			Item tInput = gridHas(tForm.aRows(), 'Y') ? itemOrNull(gregapi.data.OP.plateCurved, tMaterial)
					: itemOrNull(gridHas(tForm.aRows(), 'C') ? gregapi.data.OP.plateGem : gregapi.data.OP.plate, tMaterial);
			if (tResult == null || tInput == null) continue; // the item-truth guard
			java.util.Map<Character, Object> tDefines = craftFromDefines(tMaterial, null, tForm.aRows());
			if (gridHas(tForm.aRows(), 'Y')) tDefines.put('Y', tInput); // the curved-plate edges (the unlock face too)
			rRows.add(new ToolHeadCraftFromRow(craftFromRowId(tForm.aKey(), tSnake), tResult, tForm.aCount(), tInput, tDefines, tForm.aRows()));
		}
		return rRows;
	}

	/** One row's builder — the fixed vocabulary defines + the shape-driven tool defines. */
	private ShapedRecipeBuilder toolHeadCraftFromBuilder(ToolHeadCraftFromRow aRow) {
		ShapedRecipeBuilder rBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aRow.aResult(), aRow.aCount())
				.pattern(aRow.aRows()[0])
				.pattern(aRow.aRows()[1])
				.pattern(aRow.aRows()[2])
				.unlockedBy("has_input", has(aRow.aInput()));
		applyDefines(rBuilder, aRow.aDefines());
		defineTools(rBuilder, aRow.aRows());
		return rBuilder;
	}

	// -- Residual band 3: the casingMachine faces (:152-155) — 7x plate-tier
	// ("YXX","XwX","XXY", Y = the stickLong pair) + wrench, count 1,
	// ANTIMATTER.NOT ONLY (no COATED leg). THE FORMS POUR ZERO TODAY: the
	// casingMachine family carries no MaterialPrefixItems (the MTE-block
	// domain, Loader_Items.java:57-171) — the fine-wire :169 seam, the rows
	// unlock with that item family.

	/** One casing row form (Loader_OreProcessing.java:152-155, the grids and amounts verbatim). Package-private for the pin test. */
	record CasingCraftFromForm(String aKey, gregapi.oredict.OreDictPrefix aOutput, int aCount, gregapi.oredict.OreDictPrefix aInput, gregapi.oredict.OreDictPrefix aCompanion, String[] aRows) {}

	/** The four row forms (null upstream categories — the output-prefix snake keys). */
	static List<CasingCraftFromForm> casingCraftFromForms() {
		return List.of(
				new CasingCraftFromForm("casing_machine", gregapi.data.OP.casingMachine, 1, gregapi.data.OP.plate, gregapi.data.OP.stickLong, new String[] {"YXX", "XwX", "XXY"}),
				new CasingCraftFromForm("casing_machine_double", gregapi.data.OP.casingMachineDouble, 1, gregapi.data.OP.plateDouble, gregapi.data.OP.stickLong, new String[] {"YXX", "XwX", "XXY"}),
				new CasingCraftFromForm("casing_machine_quadruple", gregapi.data.OP.casingMachineQuadruple, 1, gregapi.data.OP.plateQuadruple, gregapi.data.OP.stickLong, new String[] {"YXX", "XwX", "XXY"}),
				new CasingCraftFromForm("casing_machine_dense", gregapi.data.OP.casingMachineDense, 1, gregapi.data.OP.plateDense, gregapi.data.OP.stickLong, new String[] {"YXX", "XwX", "XXY"}));
	}

	/** The material face of one casing row (the test-visible walk unit). */
	record CasingCraftFromMaterialRow(CasingCraftFromForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/** The material face: the three-face intersection under ANTIMATTER.NOT only — zero today (no casing items), the walk stays honest. */
	static List<CasingCraftFromMaterialRow> casingCraftFromMaterialRows() {
		List<CasingCraftFromMaterialRow> rRows = new ArrayList<>();
		for (CasingCraftFromForm tForm : casingCraftFromForms()) {
			java.util.Set<OreDictMaterial> tInputs = itemTruth(List.of(tForm.aInput(), tForm.aCompanion()));
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tInputs.contains(tMaterial)) continue; // the item-truth intersection
				if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT — the ONLY upstream condition leg
				rRows.add(new CasingCraftFromMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** The datagen row: the id + the output item + count + the input item + the companion item + the pattern rows. */
	private record CasingCraftFromRow(ResourceLocation aId, Item aResult, int aCount, Item aInput, Item aCompanion, String[] aRows) {}

	/** The datagen face: the material walk resolved onto the live items. */
	private List<CasingCraftFromRow> casingCraftFromDatagenRows() {
		List<CasingCraftFromRow> rRows = new ArrayList<>();
		for (CasingCraftFromMaterialRow tMaterialRow : casingCraftFromMaterialRows()) {
			CasingCraftFromForm tForm = tMaterialRow.aForm();
			String tSnake = GTMaterialItems.snakeCase(tMaterialRow.aMaterial().mNameInternal);
			Item tResult = itemOrNull(tForm.aOutput(), tMaterialRow.aMaterial());
			Item tInput = itemOrNull(tForm.aInput(), tMaterialRow.aMaterial());
			Item tCompanion = itemOrNull(tForm.aCompanion(), tMaterialRow.aMaterial());
			if (tResult == null || tInput == null || tCompanion == null) continue; // the item-truth guard
			rRows.add(new CasingCraftFromRow(craftFromRowId(tForm.aKey(), tSnake), tResult, tForm.aCount(), tInput, tCompanion, tForm.aRows()));
		}
		return rRows;
	}

	/** One row's builder — 'X' = the plate tier, 'Y' = the stickLong pair, the wrench center. */
	private ShapedRecipeBuilder casingCraftFromBuilder(CasingCraftFromRow aRow) {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aRow.aResult(), aRow.aCount())
				.pattern(aRow.aRows()[0])
				.pattern(aRow.aRows()[1])
				.pattern(aRow.aRows()[2])
				.define('X', aRow.aInput())
				.define('Y', aRow.aCompanion())
				.define('w', GT6ItemTags.TOOLS_WRENCH)
				.unlockedBy("has_input", has(aRow.aInput()));
	}

	// -- Residual band 4: the small-parts faces (:164-167, :170) — bolt (saw
	// + stick → 2, +MT.Wood.NOT), screw (file + bolt), ring (file + gem),
	// round (file + chunkGt), plateTiny (saw + plate → 8, +MT.Paper.NOT
	// +MT.Wood.NOT). The fixed letters carry the input where the row uses
	// them ('S' = stick :164, 'P' = plate :170), 'X' = the special prefix.

	/** One small-parts row form (Loader_OreProcessing.java:164-167/:170, the grids and amounts verbatim). Package-private for the pin test. */
	record SmallPartCraftFromForm(String aKey, gregapi.oredict.OreDictPrefix aOutput, int aCount, gregapi.oredict.OreDictPrefix aInput, boolean aNoWood, boolean aNoPaper, String[] aRows) {}

	/** The five row forms (the category snakes verbatim; :170 gets the leaf — its upstream category collides with the P36 form's). */
	static List<SmallPartCraftFromForm> smallPartCraftFromForms() {
		return List.of(
				new SmallPartCraftFromForm("stick2bolt", gregapi.data.OP.bolt, 2, gregapi.data.OP.stick, true, false, new String[] {"s ", " S"}),
				new SmallPartCraftFromForm("bolt2screw", gregapi.data.OP.screw, 1, gregapi.data.OP.bolt, false, false, new String[] {"fX", "X "}),
				new SmallPartCraftFromForm("gem2ring", gregapi.data.OP.ring, 1, gregapi.data.OP.gem, false, false, new String[] {"f ", " X"}),
				new SmallPartCraftFromForm("chunk2round", gregapi.data.OP.round, 1, gregapi.data.OP.chunkGt, false, false, new String[] {"f ", " X"}),
				new SmallPartCraftFromForm("plate2plate_tiny/regular", gregapi.data.OP.plateTiny, 8, gregapi.data.OP.plate, true, true, new String[] {"s ", " P"}));
	}

	/** The material face of one small-parts row (the test-visible walk unit). */
	record SmallPartCraftFromMaterialRow(SmallPartCraftFromForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/** The material face: the two-face intersection minus COATED/ANTIMATTER and the per-form Wood/Paper identity exclusions (registration order). */
	static List<SmallPartCraftFromMaterialRow> smallPartCraftFromMaterialRows() {
		List<SmallPartCraftFromMaterialRow> rRows = new ArrayList<>();
		for (SmallPartCraftFromForm tForm : smallPartCraftFromForms()) {
			java.util.Set<OreDictMaterial> tInputs = itemTruth(List.of(tForm.aOutput(), tForm.aInput()));
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tInputs.contains(tMaterial)) continue; // the item-truth intersection
				if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
				if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
				if (tForm.aNoWood() && tMaterial == gregapi.data.MT.Wood) continue; // MT.Wood.NOT
				if (tForm.aNoPaper() && tMaterial == gregapi.data.MT.Paper) continue; // MT.Paper.NOT
				rRows.add(new SmallPartCraftFromMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** The datagen row: the id + the output item + count + the input item + the resolved defines + the pattern rows. */
	private record SmallPartCraftFromRow(ResourceLocation aId, Item aResult, int aCount, Item aInput, java.util.Map<Character, Object> aDefines, String[] aRows) {}

	/** The datagen face: the material walk resolved onto the live items. */
	private List<SmallPartCraftFromRow> smallPartCraftFromDatagenRows() {
		List<SmallPartCraftFromRow> rRows = new ArrayList<>();
		for (SmallPartCraftFromMaterialRow tMaterialRow : smallPartCraftFromMaterialRows()) {
			SmallPartCraftFromForm tForm = tMaterialRow.aForm();
			String tSnake = GTMaterialItems.snakeCase(tMaterialRow.aMaterial().mNameInternal);
			Item tResult = itemOrNull(tForm.aOutput(), tMaterialRow.aMaterial());
			Item tInput = itemOrNull(tForm.aInput(), tMaterialRow.aMaterial());
			if (tResult == null || tInput == null) continue; // the item-truth guard
			rRows.add(new SmallPartCraftFromRow(craftFromRowId(tForm.aKey(), tSnake), tResult, tForm.aCount(), tInput,
					craftFromDefines(tMaterialRow.aMaterial(), tForm.aInput(), tForm.aRows()), tForm.aRows()));
		}
		return rRows;
	}

	/** One row's builder — the fixed vocabulary defines + the shape-driven tool defines (saw/file). */
	private ShapedRecipeBuilder smallPartCraftFromBuilder(SmallPartCraftFromRow aRow) {
		ShapedRecipeBuilder rBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aRow.aResult(), aRow.aCount())
				.pattern(aRow.aRows()[0])
				.pattern(aRow.aRows()[1])
				.unlockedBy("has_input", has(aRow.aInput()));
		applyDefines(rBuilder, aRow.aDefines());
		defineTools(rBuilder, aRow.aRows());
		return rBuilder;
	}

	// -- Residual band 5: the minecartWheels face (:179) — 2x ring (X) +
	// 1x stick (S) around the hammer/wrench cross, ANTIMATTER.NOT ONLY (no
	// COATED leg, like the casing band).

	/** One minecartWheels row form (Loader_OreProcessing.java:179, the grid and amount verbatim). Package-private for the pin test. */
	record MinecartWheelsCraftFromForm(String aKey, gregapi.oredict.OreDictPrefix aOutput, int aCount, gregapi.oredict.OreDictPrefix aInput, gregapi.oredict.OreDictPrefix aStickFace, String[] aRows) {}

	/** The single row form. */
	static List<MinecartWheelsCraftFromForm> minecartWheelsCraftFromForms() {
		return List.of(new MinecartWheelsCraftFromForm("minecart_wheels", gregapi.data.OP.minecartWheels, 1, gregapi.data.OP.ring, gregapi.data.OP.stick, new String[] {" h ", "XSX", " w "}));
	}

	/** The material face of the minecartWheels row (the test-visible walk unit). */
	record MinecartWheelsCraftFromMaterialRow(MinecartWheelsCraftFromForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/** The material face: the three-face intersection (wheels ∩ ring ∩ stick) under ANTIMATTER.NOT only. */
	static List<MinecartWheelsCraftFromMaterialRow> minecartWheelsCraftFromMaterialRows() {
		List<MinecartWheelsCraftFromMaterialRow> rRows = new ArrayList<>();
		for (MinecartWheelsCraftFromForm tForm : minecartWheelsCraftFromForms()) {
			java.util.Set<OreDictMaterial> tInputs = itemTruth(List.of(tForm.aInput(), tForm.aStickFace()));
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tInputs.contains(tMaterial)) continue; // the item-truth intersection
				if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT — the ONLY upstream condition leg
				rRows.add(new MinecartWheelsCraftFromMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** The datagen row: the id + the output item + count + the input item + the resolved defines + the pattern rows. */
	private record MinecartWheelsCraftFromRow(ResourceLocation aId, Item aResult, int aCount, Item aInput, java.util.Map<Character, Object> aDefines, String[] aRows) {}

	/** The datagen face: the material walk resolved onto the live items. */
	private List<MinecartWheelsCraftFromRow> minecartWheelsCraftFromDatagenRows() {
		List<MinecartWheelsCraftFromRow> rRows = new ArrayList<>();
		for (MinecartWheelsCraftFromMaterialRow tMaterialRow : minecartWheelsCraftFromMaterialRows()) {
			MinecartWheelsCraftFromForm tForm = tMaterialRow.aForm();
			String tSnake = GTMaterialItems.snakeCase(tMaterialRow.aMaterial().mNameInternal);
			Item tResult = itemOrNull(tForm.aOutput(), tMaterialRow.aMaterial());
			Item tInput = itemOrNull(tForm.aInput(), tMaterialRow.aMaterial());
			if (tResult == null || tInput == null) continue; // the item-truth guard
			rRows.add(new MinecartWheelsCraftFromRow(craftFromRowId(tForm.aKey(), tSnake), tResult, tForm.aCount(), tInput,
					craftFromDefines(tMaterialRow.aMaterial(), tForm.aInput(), tForm.aRows()), tForm.aRows()));
		}
		return rRows;
	}

	/** One row's builder — 'X' = the ring, 'S' = the stick, the hammer/wrench cross. */
	private ShapedRecipeBuilder minecartWheelsCraftFromBuilder(MinecartWheelsCraftFromRow aRow) {
		ShapedRecipeBuilder rBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aRow.aResult(), aRow.aCount())
				.pattern(aRow.aRows()[0])
				.pattern(aRow.aRows()[1])
				.pattern(aRow.aRows()[2])
				.unlockedBy("has_input", has(aRow.aInput()));
		applyDefines(rBuilder, aRow.aDefines());
		defineTools(rBuilder, aRow.aRows());
		return rBuilder;
	}

	// -- Residual band 6: the SHAPELESS panel (:181-192, 12 statements).
	// The upstream OreProcessing_Shapeless (:480-510) substitutes every BARE
	// OreDictPrefix slot with .dat(m) (:501-502) — pre-resolved slots
	// (prefix.dat(fixed material), prefix.dat(ANY group)) pass through
	// verbatim. Per form: ONE per-material slot (the walk input face) plus
	// fixed/group/tool slots. The ingredient ORDER normalizes to
	// per-material/fixed/group/tools (the vanilla shapeless match is a
	// multiset — order is semantically dead). Condition kinds upstream
	// verbatim (see the COND_ constants). :183/:184 pour zero (the
	// wire/cable block domain, the fine-wire :169 seam).

	/** The condition kinds of the shapeless panel, upstream verbatim. */
	static final int COND_COATED_ANTIMATTER = 0; // new And(ANTIMATTER.NOT, COATED.NOT) — :187-189
	static final int COND_ANTIMATTER = 1;        // ANTIMATTER.NOT — :181-184/:190-192
	static final int COND_TRUE = 2;              // ICondition.TRUE — :185
	static final int COND_MELT_MIN_ENV = 3;      // meltmin(DEF_ENV_TEMP) — :186 (C+20 = 293, CS.java:135)

	/** One shapeless row form (Loader_OreProcessing.java:181-192, the amounts and conditions verbatim). Package-private for the pin test. */
	record ShapelessCraftFromForm(String aKey, gregapi.oredict.OreDictPrefix aOutput, int aCount, int aCondition,
			gregapi.oredict.OreDictPrefix aPerMaterial, int aPerMaterialRepeat,
			gregapi.oredict.OreDictPrefix aFixedPrefix, gregapi.oredict.OreDictMaterial aFixedMaterial,
			gregapi.oredict.OreDictPrefix aGroupPrefix, gregapi.oredict.OreDictMaterial aGroup, String[] aTools) {}

	/** The twelve row forms (the :181/:182 category snakes; the null-category forms compose the keys). */
	static List<ShapelessCraftFromForm> shapelessCraftFromForms() {
		return List.of(
				new ShapelessCraftFromForm("arrows_wooden", gregapi.data.OP.arrowGtWood, 1, COND_ANTIMATTER, gregapi.data.OP.toolHeadArrow, 1, gregapi.data.OP.arrowGtWood, gregapi.data.MT.Empty, null, null, new String[0]),
				new ShapelessCraftFromForm("arrows_plastic", gregapi.data.OP.arrowGtPlastic, 1, COND_ANTIMATTER, gregapi.data.OP.toolHeadArrow, 1, gregapi.data.OP.arrowGtPlastic, gregapi.data.MT.Empty, null, null, new String[0]),
				new ShapelessCraftFromForm("cable_gt01/from_wire_gt01", gregapi.data.OP.cableGt01, 1, COND_ANTIMATTER, gregapi.data.OP.wireGt01, 1, null, null, gregapi.data.OP.plate, gregapi.data.ANY.Rubber, new String[0]),
				new ShapelessCraftFromForm("cable_gt02/from_wire_gt02", gregapi.data.OP.cableGt02, 1, COND_ANTIMATTER, gregapi.data.OP.wireGt02, 1, null, null, gregapi.data.OP.plate, gregapi.data.ANY.Rubber, new String[0]),
				new ShapelessCraftFromForm("chemtube/from_dust_tiny", gregapi.data.OP.chemtube, 1, COND_TRUE, gregapi.data.OP.dustTiny, 1, gregapi.data.OP.chemtube, gregapi.data.MT.Empty, null, null, new String[0]),
				new ShapelessCraftFromForm("dust_tiny/from_chemtube", gregapi.data.OP.dustTiny, 1, COND_MELT_MIN_ENV, gregapi.data.OP.chemtube, 1, null, null, null, null, new String[0]),
				new ShapelessCraftFromForm("tool_head_raw_universal_spade/from_shovel", gregapi.data.OP.toolHeadRawUniversalSpade, 1, COND_COATED_ANTIMATTER, gregapi.data.OP.toolHeadShovel, 1, null, null, null, null, new String[] {"file", "saw"}),
				new ShapelessCraftFromForm("tool_head_raw_universal_spade/from_spade", gregapi.data.OP.toolHeadRawUniversalSpade, 1, COND_COATED_ANTIMATTER, gregapi.data.OP.toolHeadSpade, 1, null, null, null, null, new String[] {"file", "saw"}),
				new ShapelessCraftFromForm("tool_head_construction_pickaxe/from_raw_pickaxe", gregapi.data.OP.toolHeadConstructionPickaxe, 1, COND_COATED_ANTIMATTER, gregapi.data.OP.toolHeadRawPickaxe, 1, null, null, null, null, new String[] {"file", "hammer"}),
				new ShapelessCraftFromForm("tool_head_pickaxe_gem/from_raw_any_iron", gregapi.data.OP.toolHeadPickaxeGem, 1, COND_ANTIMATTER, gregapi.data.OP.gemFlawed, 2, null, null, gregapi.data.OP.toolHeadRawPickaxe, gregapi.data.ANY.Iron, new String[] {"file", "hammer", "saw"}),
				new ShapelessCraftFromForm("tool_head_pickaxe_gem/from_any_iron", gregapi.data.OP.toolHeadPickaxeGem, 1, COND_ANTIMATTER, gregapi.data.OP.gemFlawed, 2, null, null, gregapi.data.OP.toolHeadPickaxe, gregapi.data.ANY.Iron, new String[] {"file", "hammer", "saw"}),
				new ShapelessCraftFromForm("tool_head_pickaxe_gem/retip", gregapi.data.OP.toolHeadPickaxeGem, 1, COND_ANTIMATTER, gregapi.data.OP.gemFlawed, 2, gregapi.data.OP.toolHeadPickaxeGem, gregapi.data.MT.Empty, null, null, new String[] {"file", "hammer", "saw"}));
	}

	/** The material face of one shapeless row (the test-visible walk unit). */
	record ShapelessCraftFromMaterialRow(ShapelessCraftFromForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/** The material face: output ∩ the per-material slot, minus the per-form condition kind, gated on the fixed/group slot item truth (once per form) — registration order. */
	static List<ShapelessCraftFromMaterialRow> shapelessCraftFromMaterialRows() {
		List<ShapelessCraftFromMaterialRow> rRows = new ArrayList<>();
		for (ShapelessCraftFromForm tForm : shapelessCraftFromForms()) {
			boolean tFixedOk = tForm.aFixedPrefix() == null || itemPairExists(tForm.aFixedPrefix(), tForm.aFixedMaterial());
			boolean tGroupOk = tForm.aGroup() == null || anyGroupFaceExists(tForm.aGroupPrefix(), tForm.aGroup());
			java.util.Set<OreDictMaterial> tInputs = itemTruth(List.of(tForm.aOutput(), tForm.aPerMaterial()));
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tInputs.contains(tMaterial)) continue; // the item-truth intersection
				switch (tForm.aCondition()) {
					case COND_COATED_ANTIMATTER -> {
						if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
						if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
					}
					case COND_ANTIMATTER -> {
						if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
					}
					case COND_TRUE -> {} // the :185 unconditional face
					default -> {
						if (tMaterial.mMeltingPoint < 293) continue; // meltmin(DEF_ENV_TEMP), CS.java:135 C+20
					}
				}
				if (!tFixedOk || !tGroupOk) continue; // the fixed/group slot item truth (form-global)
				rRows.add(new ShapelessCraftFromMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** The datagen row: the id + the output item + count + the primary input + the resolved ingredients (the normalized order). */
	private record ShapelessCraftFromRow(ResourceLocation aId, Item aResult, int aCount, Item aPrimary, java.util.List<net.minecraft.world.item.crafting.Ingredient> aIngredients) {}

	/** The datagen face: the material walk resolved onto the live items and tags. */
	private List<ShapelessCraftFromRow> shapelessCraftFromDatagenRows() {
		List<ShapelessCraftFromRow> rRows = new ArrayList<>();
		for (ShapelessCraftFromMaterialRow tMaterialRow : shapelessCraftFromMaterialRows()) {
			ShapelessCraftFromForm tForm = tMaterialRow.aForm();
			String tSnake = GTMaterialItems.snakeCase(tMaterialRow.aMaterial().mNameInternal);
			Item tResult = itemOrNull(tForm.aOutput(), tMaterialRow.aMaterial());
			Item tPrimary = itemOrNull(tForm.aPerMaterial(), tMaterialRow.aMaterial());
			if (tResult == null || tPrimary == null) continue; // the item-truth guard
			java.util.List<net.minecraft.world.item.crafting.Ingredient> tIngredients = new ArrayList<>();
			for (int i = 0; i < tForm.aPerMaterialRepeat(); i++) tIngredients.add(net.minecraft.world.item.crafting.Ingredient.of(tPrimary));
			if (tForm.aFixedPrefix() != null) {
				Item tFixed = itemOrNull(tForm.aFixedPrefix(), tForm.aFixedMaterial());
				if (tFixed == null) continue; // the fixed-slot guard
				tIngredients.add(net.minecraft.world.item.crafting.Ingredient.of(tFixed));
			}
			if (tForm.aGroup() != null) {
				java.util.List<Item> tGroupItems = new ArrayList<>();
				for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
					if (tPair.prefix() == tForm.aGroupPrefix() && tForm.aGroup().mToThis.contains(tPair.material())) {
						Item tItem = itemOrNull(tPair.prefix(), tPair.material());
						if (tItem != null) tGroupItems.add(tItem);
					}
				}
				if (tGroupItems.isEmpty()) continue; // the group-slot guard
				tIngredients.add(net.minecraft.world.item.crafting.Ingredient.of(tGroupItems.toArray(new Item[0])));
			}
			for (String tTool : tForm.aTools()) tIngredients.add(net.minecraft.world.item.crafting.Ingredient.of(shapelessToolTag(tTool)));
			rRows.add(new ShapelessCraftFromRow(craftFromRowId(tForm.aKey(), tSnake), tResult, tForm.aCount(), tPrimary, tIngredients));
		}
		return rRows;
	}

	/** The shapeless tool names to their tags — the OreDictToolNames slots of :187-192 ('hammer' = the HARD hammer, the p25 ruling). */
	private static TagKey<Item> shapelessToolTag(String aName) {
		return switch (aName) {
			case "file" -> GT6ItemTags.TOOLS_FILE;
			case "hammer" -> GT6ItemTags.TOOLS_HARD_HAMMER;
			case "saw" -> GT6ItemTags.TOOLS_SAW;
			default -> throw new IllegalArgumentException("unknown shapeless tool: " + aName);
		};
	}

	/** One row's builder — the shapeless requires chain over the resolved ingredients. */
	private ShapelessRecipeBuilder shapelessCraftFromBuilder(ShapelessCraftFromRow aRow) {
		ShapelessRecipeBuilder rBuilder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, aRow.aResult(), aRow.aCount())
				.unlockedBy("has_input", has(aRow.aPrimary()));
		for (net.minecraft.world.item.crafting.Ingredient tIngredient : aRow.aIngredients()) rBuilder.requires(tIngredient);
		return rBuilder;
	}

	// -----------------------------------------------------------------------
	// task debt-stairs-wall-vanilla-recipes — the upstream stairs/wall
	// VANILLA-DEGRADATION rows (BlockStones.java run(), the generify philosophy
	// as shipped): per GT stone family the decoration shapes degrade onto
	// VANILLA outputs. Two upstream loops:
	//
	//  - :268, the per-material body: 3× rockGt (" X","XX") → 1 vanilla
	//    cobblestone stairs. Input count is the pattern VERBATIM (3 X's — the
	//    card's "2 inputs" was a misread, coder correction #2; ruling
	//    decisions.2026-09-26-debt-stairs-walls-slab coder_corrections).
	//  - :328/:329, the mEqualBlocks[COBBL] loop body: 3× family cobble → 4
	//    stairs and 6× family cobble ("XXX","XXX") → 6 vanilla cobblestone
	//    wall. That set self-adds only the family's own COBBL variant
	//    (BlockStones.java:254; the lone external grafts Loader_Rocks.java
	//    :203-205 target mEqualBlocks[0] and never COBBL), so it walks exactly
	//    the 17 family cobbles.
	//
	// The upstream output {@code Blocks.stone_stairs} is 1.7.10 block id 67 =
	// COBBLESTONE stairs (the MCP naming quirk) — the modern face
	// {@code Items.COBBLESTONE_STAIRS} (Items.java:303, the 1.13 flattening
	// rename), NOT the 1.20.1 {@code Items.STONE_STAIRS} (Items.java:650 — the
	// 1.14 stone-textured block, absent from the upstream universe; coder
	// correction #1). CR.DEF_MIR = the vanilla shaped auto-mirror. The upstream
	// {@code // TODO Stairs} comments mark the never-built GT-textured stairs —
	// the 34-block GT-material build was ruled CUT; this IS the upstream real
	// behaviour.

	/** The stairs-from-rocks rows (BlockStones.java:268) — 17× 3 rockGt → 1 vanilla cobblestone stairs. */
	private List<PartFamilyRecipeRow> stairsFromRocksBuilders() {
		List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (gregtech6.registry.GTStoneBlocks.StoneSpec tStone : gregtech6.registry.GTStoneBlocks.STONES) {
			Item tRock = itemOrNull(gregapi.data.OP.rockGt, tStone.material().get());
			if (tRock == null) continue; // the CR.ONLY_IF_HAS_RESULT face (all 17 resolve today; rock_gt_prismarine = MT.PrismarineLight, internal name "Prismarine")
			String tIdPath = "stairs_rock/" + tStone.snake(); // the precomputed arg — the stonecutter ctor swap rewrites simple-arg calls only
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, Items.COBBLESTONE_STAIRS)
					.pattern(" X")
					.pattern("XX")
					.define('X', tRock)
					.unlockedBy("has_rock", has(tRock)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tIdPath)));
		}
		return rRows;
	}

	/**
	 * The mEqualBlocks[COBBL] loop rows (BlockStones.java:328-329) — per family BOTH shapes:
	 * 3× the family's COBBL variant → 4 vanilla cobblestone stairs, then 6× → 6 vanilla
	 * cobblestone wall (the loop-body order, family-major).
	 */
	private List<PartFamilyRecipeRow> cobbleStairsWallBuilders() {
		List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (gregtech6.registry.GTStoneBlocks.StoneSpec tStone : gregtech6.registry.GTStoneBlocks.STONES) {
			var tCobblHandle = gregtech6.registry.GTStoneBlocks.item(tStone.snake(), gregtech6.block.stone.StoneVariant.COBBL);
			if (tCobblHandle == null) continue; // pre-registration defensive skip (all 272 register before datagen)
			Item tCobbl = tCobblHandle.get();
			String tStairsPath = "stairs_cobble/" + tStone.snake(); // the precomputed args — the stonecutter ctor swap rewrites simple-arg calls only
			String tWallPath = "wall_cobble/" + tStone.snake();
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, Items.COBBLESTONE_STAIRS, 4)
					.pattern(" X")
					.pattern("XX")
					.define('X', tCobbl)
					.unlockedBy("has_cobble", has(tCobbl)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tStairsPath)));
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, Items.COBBLESTONE_WALL, 6)
					.pattern("XXX")
					.pattern("XXX")
					.define('X', tCobbl)
					.unlockedBy("has_cobble", has(tCobbl)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tWallPath)));
		}
		return rRows;
	}

	// -----------------------------------------------------------------------
	// The dungeon-key band (task debt-dungeon-keys-recipes — the P38
	// dungeon-keys card's explicit defer, closed per the r-dungeon-key-trace
	// research): the ten material keys of MultiItemRandomTools.java:589-598,
	// each CR.shaped(IL.Key_X.get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(mat))
	// — 1 plate → 3 keys, the single-row grid [file | plate | wire cutter].
	// Tool letters: 'f' = the CR.java:200 craftingToolFile letter →
	// #gt6:tools/file, 'x' = the CR.java:214 wirecutter letter →
	// #gt6:tools/wire_cutter (the fine-wire band translation). The 'P' column
	// rides the per-material plate family tag — the oredict input face's
	// modern carrier; the three upstream multi faces (ANY.Iron/ANY.Cu/
	// ANY.Plastic) pin to the canonical member snake (plates/iron — the
	// GT6ElectricTransformers ANY-face convention, NOT the whole
	// Iron-Or-Steel union). CR.DEF_NCC's NO_COLLISION_CHECK is the 1.7.10
	// shaped face — the vanilla datagen form carries no collision flag, the
	// natural equivalent (the research verdict). Ids on the output item path
	// (the usb_drive_&lt;tier&gt; convention).
	// -----------------------------------------------------------------------

	/**
	 * The ten key rows — MultiItemRandomTools.java:589-598 registration order
	 * (iron/gold/copper/tin/bronze/brass/silver/platinum/lead/plastic, ids 30000-30009),
	 * one {@code "fPx"} CR.shaped each onto the {@link gregtech6.items.GT6Keys} family.
	 */
	private java.util.List<PartFamilyRecipeRow> keyRecipeBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		keyRow(rRows, gregtech6.items.GT6Keys.KEY_IRON.get(), "iron");         // :589 ANY.Iron    (30000)
		keyRow(rRows, gregtech6.items.GT6Keys.KEY_GOLD.get(), "gold");         // :590 MT.Au       (30001)
		keyRow(rRows, gregtech6.items.GT6Keys.KEY_COPPER.get(), "copper");     // :591 ANY.Cu      (30002)
		keyRow(rRows, gregtech6.items.GT6Keys.KEY_TIN.get(), "tin");           // :592 MT.Sn       (30003)
		keyRow(rRows, gregtech6.items.GT6Keys.KEY_BRONZE.get(), "bronze");     // :593 MT.Bronze   (30004)
		keyRow(rRows, gregtech6.items.GT6Keys.KEY_BRASS.get(), "brass");       // :594 MT.Brass    (30005)
		keyRow(rRows, gregtech6.items.GT6Keys.KEY_SILVER.get(), "silver");     // :595 MT.Ag       (30006)
		keyRow(rRows, gregtech6.items.GT6Keys.KEY_PLATINUM.get(), "platinum"); // :596 MT.Pt       (30007)
		keyRow(rRows, gregtech6.items.GT6Keys.KEY_LEAD.get(), "lead");         // :597 MT.Pb       (30008)
		keyRow(rRows, gregtech6.items.GT6Keys.KEY_PLASTIC.get(), "plastic");   // :598 ANY.Plastic (30009)
		return rRows;
	}

	/** One CR.shaped(Key.get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(mat)) row — the id on the output item path. */
	private void keyRow(java.util.List<PartFamilyRecipeRow> rRows, Item aKey, String aPlateSnake) {
		TagKey<Item> tPlate = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, aPlateSnake);
		String tPath = "key_" + aPlateSnake; // the precomputed arg — the stonecutter two-arg-ctor shift skips parenthesized args
		rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aKey, 3)
				.pattern("fPx")
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('P', tPlate)
				.define('x', GT6ItemTags.TOOLS_WIRE_CUTTER)
				.unlockedBy("has_plate", has(tPlate)),
				new ResourceLocation(GT6DataGenerators.MOD_ID, tPath)));
	}

	// -----------------------------------------------------------------------
	// task c4-copper-bridge — the vanilla-copper recipe bridge (the C4
	// fusion ruling: GT copper is the ONLY copper source — #32 suppresses the
	// vanilla copper ore blobs; this band keeps the vanilla copper USES unbroken
	// by re-keying the four 1.20.1 rows that consume {@code minecraft:copper_ingot}
	// onto the platform ingot tag). The tag face needs NO new datagen:
	// forge:ingots/copper already carries BOTH members at runtime — the Forge
	// jar's own default (tmp/refs/forge-api/forge-1.20.1 .../ingots/copper.json
	// = [minecraft:copper_ingot]) plus this port's datagen member
	// gt6:ingot_copper (data/forge/tags/items/ingots/copper.json, and the 21.1
	// c: twin via GT6ItemTags MATERIALS_NAMESPACE + the neoforgeTagFaces graft).
	// The 1.20.1 census of the copper_ingot crafting-INPUT face is exactly these
	// four rows (brush / copper_block / lightning_rod / spyglass — no copper
	// armor, doors or trapdoors exist in 1.20.1; the task card's "自查确认后声明"
	// face). Scope cuts, each self-consistent:
	//   - the reverse rows copper_ingot.json / copper_ingot_from_waxed_copper_block
	//     consume the VANILLA copper block item and stay vanilla — the overridden
	//     copper_block row still outputs the vanilla block, so the oxidation/wax
	//     chain keeps working (the GT storage block gt6:block_ingot_copper is a
	//     different block and deliberately NOT on this face);
	//   - the smelting/blasting rows are the OUTPUT face (vanilla copper ingot
	//     from the suppressed vanilla ores) — untouched; the GT ore chain
	//     already yields GT ingots (the card's 产物面 ruling).
	// Each row reproduces the vanilla pattern/category/criterion verbatim
	// (criterion name has_copper_ingot, the vanilla JSONs), so the generated
	// unlock advancements land at the vanilla ids (minecraft:recipes/&lt;folder&gt;/...)
	// and override them with the tag criteria — the unlock toast then fires on
	// EITHER copper face. On the 1.21.1 leg the mirror (GT6DualDirectoryFaces
	// .adaptRecipes21) re-keys the singular face to c:ingots/copper; the plural
	// advancement face is unlock-toast-only sugar there (the repo's accepted
	// 21.1 advancement gap), the recipes themselves ride the mirrored singular
	// band.
	// -----------------------------------------------------------------------

	/** The copper-bridge input tag — {@code forge:ingots/copper} (1.20.1) / {@code c:ingots/copper} (21.1). */
	private static TagKey<Item> copperIngotTag() {
		return GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, MT.Copper);
	}

	/** The four vanilla-namespace override ids — the vanilla row ids verbatim. */
	public static final ResourceLocation VANILLA_BRUSH_ID = new ResourceLocation("minecraft", "brush");
	public static final ResourceLocation VANILLA_COPPER_BLOCK_ID = new ResourceLocation("minecraft", "copper_block");
	public static final ResourceLocation VANILLA_LIGHTNING_ROD_ID = new ResourceLocation("minecraft", "lightning_rod");
	public static final ResourceLocation VANILLA_SPYGLASS_ID = new ResourceLocation("minecraft", "spyglass");

	/** The vanilla brush row (pattern/tools category verbatim) with the tag input. */
	private ShapedRecipeBuilder vanillaBrushBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.BRUSH)
				.pattern("X")
				.pattern("#")
				.pattern("I")
				.define('X', Items.FEATHER)
				.define('#', copperIngotTag())
				.define('I', Items.STICK)
				.unlockedBy("has_copper_ingot", has(copperIngotTag()));
	}

	/** The vanilla copper-block row (the 9-ingot cube; the OUTPUT stays the vanilla block — the oxidation chain rides it). */
	private ShapedRecipeBuilder vanillaCopperBlockBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, Items.COPPER_BLOCK)
				.pattern("###")
				.pattern("###")
				.pattern("###")
				.define('#', copperIngotTag())
				.unlockedBy("has_copper_ingot", has(copperIngotTag()));
	}

	/** The vanilla lightning-rod row (the 3-ingot column). */
	private ShapedRecipeBuilder vanillaLightningRodBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, Items.LIGHTNING_ROD)
				.pattern("#")
				.pattern("#")
				.pattern("#")
				.define('#', copperIngotTag())
				.unlockedBy("has_copper_ingot", has(copperIngotTag()));
	}

	/** The vanilla spyglass row ('#' = amethyst shard, 'X' = the copper tag). */
	private ShapedRecipeBuilder vanillaSpyglassBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.SPYGLASS)
				.pattern(" # ")
				.pattern(" X ")
				.pattern(" X ")
				.define('#', Items.AMETHYST_SHARD)
				.define('X', copperIngotTag())
				.unlockedBy("has_copper_ingot", has(copperIngotTag()));
	}

}

