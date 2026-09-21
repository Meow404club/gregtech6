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
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTGrassBlocks;
import gregtech6.registry.GT6Sensors;
import gregapi.data.MT;

/**
 * The GT6 vanilla-crafting datagen home — task p24-tool-system spec ③, the FIRST
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
 * p25-food-can-row0 appends the food-can second row itself ({@code "fh"}/{@code "oP"},
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
	 * The tool-family recipe ids (task p25-tool-hammer-wrench spec ⑤) — the result-path
	 * vanilla naming convention; the two hammer routes cannot share the result's own id,
	 * so the route names the suffix (the vanilla two-recipe-per-result precedent shape).
	 */
	public static final ResourceLocation HAMMER_STONE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "hammer_stone");
	public static final ResourceLocation HAMMER_INGOTS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "hammer_ingots");
	public static final ResourceLocation WRENCH_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "wrench");
	/** The bending-cylinder self-craft row (task p25-food-can-row0 spec ②) — the result-path convention. */
	public static final ResourceLocation BENDING_CYLINDER_SMALL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "bending_cylinder_small");
	/** The pocket multitool recipe id (task p29-w5-t7-pocket-eight, the result-path convention). */
	public static final ResourceLocation POCKET_MULTITOOL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pocket_multitool");
	/** The empty-food-can crafting row (task p25-food-can-row0 spec ③, MultiItemRandomTools.java:239). */
	public static final ResourceLocation FOOD_CAN_EMPTY_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "food_can_empty");
	/** The machine-face four self-craft rows (task p29-w5-t3-machine-face-four) — the result-path convention, one per tool. */
	public static final ResourceLocation SOFT_HAMMER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "soft_hammer");
	public static final ResourceLocation MONKEY_WRENCH_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "monkey_wrench");
	public static final ResourceLocation MAGNIFYING_GLASS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "magnifying_glass");
	public static final ResourceLocation PINCERS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pincers");
	/** The plate-mold crafting row (task p26-w1-press-extruder-molds, MultiItemTechnological.java:247 stroke). */
	public static final ResourceLocation SHAPE_EXTRUDER_PLATE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_extruder_plate");
	/** The rod-mold crafting row (task p26-w1-press-extruder-molds, MultiItemTechnological.java:221 stroke). */
	public static final ResourceLocation SHAPE_EXTRUDER_ROD_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_extruder_rod");
	/** The LARGE Steel Crucible crafting row (task p26-crucible-multiblock SPEC ⑦). */
	public static final ResourceLocation LARGE_STEEL_CRUCIBLE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "large_steel_crucible");
	/**
	 * The kitchen band (task p26-kitchen-pot-bowl): the steel pot's crafting row (the
	 * result-path convention) + the clay-bowl reverse shapeless + the Raw-bowl hardening
	 * smelt (the :2177 tail — the result path is the vanilla convention again).
	 */
	public static final ResourceLocation BATHING_POT_STEEL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "bathing_pot_steel");
	public static final ResourceLocation CLAY_BOWL_REVERSE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "clay_bowl_reverse");
	public static final ResourceLocation CLAY_BOWL_SMELT_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "mixing_bowl");

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

	/** The Progress Sensor crafting row (task p26-sensors-core, Loader_MultiTileEntities.java:1995) — the result-path convention. */
	public static final ResourceLocation PROGRESSMETER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "progressmeter");
	/** The Fluid-O-Meter Sensor crafting row (task p26-sensors-core, Loader :1986). */
	public static final ResourceLocation FLUIDOMETER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "fluidometer");
	/**
	 * The ULV FE→EU converter crafting row (task p28-b-fe-converter-machine) — DECLARED
	 * NEW DESIGN, no upstream recipe exists (the machine itself is the declared deviation):
	 * the tin-alloy double plates + red-alloy fine wires carry the signal side, the copper
	 * ingots the conductor core; the result-path vanilla convention.
	 */
	public static final ResourceLocation FE_CONVERTER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "fe_converter");
	/**
	 * The Water Wheel crafting row (task p28-c-water-wheel) — the kTFRUAddon registration
	 * row QUANTITIES (tileEntityInit0.java:112 "Water Mill": {@code "PPP","SRS","PPP"} =
	 * 6 planks + 2 bronze rings + 1 axle part, the clean-room semantic anchor) over the
	 * port carriers; the result-path vanilla convention.
	 */
	public static final ResourceLocation WATER_WHEEL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "water_wheel");
	/** The Greg o'Lantern crafting row id (task p32-placeables, the result-path convention). */
	public static final ResourceLocation GREG_O_LANTERN_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "greg_o_lantern");
	/**
	 * The Electric Transformer (ULV-LV) crafting row (task p28-c-ulv-lv-transformer) —
	 * the Loader_MultiTileEntities.java:881 row SHAPE ("WIW","XMx","WIW" — the unbound
	 * 'm' dead cell folds to a space; CR has no 'm' tool letter) over the LV-era
	 * MATERIAL-LOCK carriers (decisions.p28-ulv-tier-rulings transformer_ruling): the
	 * casing key upgrades Electric_T[0] TinAlloy → Electric_T[1] galvanized steel (the
	 * conditional entry — whoever crafts this already commands LV power), the wire keys
	 * fold to the copper fine-wire tag, the 'I' plate key stays iron double plates.
	 */
	public static final ResourceLocation ELECTRIC_TRANSFORMER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "electric_transformer");

	/** The six dig-tool row ids (task p29-w5-t1-dig-six — the CR row id per tool, the WRENCH_ID shape). */
	public static final ResourceLocation PICKAXE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pickaxe");
	public static final ResourceLocation PICKAXE_GEM_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pickaxe_gem");
	public static final ResourceLocation PICKAXE_CONSTRUCTION_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pickaxe_construction");
	public static final ResourceLocation SHOVEL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "shovel");
	public static final ResourceLocation SPADE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "spade");
	public static final ResourceLocation UNIVERSAL_SPADE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "universal_spade");
	public static final ResourceLocation CLUB_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "club");
	public static final ResourceLocation AXE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "axe");
	public static final ResourceLocation AXE_DOUBLE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "axe_double");

	/** The five field-tool row ids (task p29-w5-t4-field-five — the t1 row-id shape). */
	public static final ResourceLocation HOE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "hoe");
	public static final ResourceLocation PLOW_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "plow");
	public static final ResourceLocation BRANCH_CUTTER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "branch_cutter");
	public static final ResourceLocation SENSE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "sense");
	public static final ResourceLocation HAND_DRILL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "hand_drill");

	/** The six scene-tool row ids (task p29-w5-t5-scene-six — the same CR row id shape; the flint pair is TWO rows). */
	public static final ResourceLocation SCISSORS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "scissors");
	public static final ResourceLocation SCOOP_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "scoop");
	public static final ResourceLocation PLUNGER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "plunger");
	public static final ResourceLocation FLINT_AND_TINDER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "flint_and_tinder");
	public static final ResourceLocation FLINT_AND_STEEL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "flint_and_steel");
	public static final ResourceLocation ROLLING_PIN_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "rolling_pin");
	public static final ResourceLocation BENDING_CYLINDER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "bending_cylinder");

	/** The CR.shapeless self-recast row id of a sensor path (task p26-sensors-core) — the path + the {@code _recast} suffix (the grass reverse-row suffix shape). */
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
		clayBowlReverseBuilder().save(aConsumer, CLAY_BOWL_REVERSE_ID);
		clayBowlSmeltingBuilder().save(aConsumer, CLAY_BOWL_SMELT_ID);
		anvilBuilder(GT6Anvils.STONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.STONE).save(aConsumer, STONE_ANVIL_ID);
		anvilBuilder(GT6Anvils.BLACKSTONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.BLACKSTONE).save(aConsumer, BLACKSTONE_ANVIL_ID);
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) {
			hopperRecipeBuilder(tRow).save(aConsumer, hopperRecipeId(tRow));
		}
		progressmeterBuilder().save(aConsumer, PROGRESSMETER_ID);
		fluidometerBuilder().save(aConsumer, FLUIDOMETER_ID);
		feConverterBuilder().save(aConsumer, FE_CONVERTER_ID);
		waterWheelBuilder().save(aConsumer, WATER_WHEEL_ID);
		transformerBuilder().save(aConsumer, ELECTRIC_TRANSFORMER_ID);
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
		for (GT6Batteries.BatteryRow tRow : GT6Batteries.ROWS) {
			if (tRow.family().startsWith("energium")) continue; // the crystals carry NO rows (upstream :1079-:1092, the declared cut)
			batteryRecipeBuilder(tRow).save(aConsumer, batteryRecipeId(tRow));
		}
		// task p32-placeables — the Greg o'Lantern row
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
		// task p29-w5-t1-dig-six — the six dig-tool steel-route rows (the wrench row shape)
		pickaxeBuilder().save(aConsumer, PICKAXE_ID);
		pickaxeGemBuilder().save(aConsumer, PICKAXE_GEM_ID);
		pickaxeConstructionBuilder().save(aConsumer, PICKAXE_CONSTRUCTION_ID);
		shovelBuilder().save(aConsumer, SHOVEL_ID);
		spadeBuilder().save(aConsumer, SPADE_ID);
		universalSpadeBuilder().save(aConsumer, UNIVERSAL_SPADE_ID);
		// task p29-w5-t2-blade-six — the six blade-tool steel-route rows (the dig-tool row shape)
		clubBuilder().save(aConsumer, CLUB_ID);
		// task p31-blade-ladder — the three blade forms move to the per-material rows
		// (the OreProcessing_Tool rows Loader_Tools.java:321-323; the t1 steel-route
		// convergence placeholders for sword/knife/butchery retire — club stays the
		// single-tier convergence row, the single-tier-ruling card owns it)
		bladeLadderRows(aConsumer);
		axeBuilder().save(aConsumer, AXE_ID);
		axeDoubleBuilder().save(aConsumer, AXE_DOUBLE_ID);
		// task p29-w5-t4-field-five — the five field-tool steel-route rows (the t1 shape)
		hoeBuilder().save(aConsumer, HOE_ID);
		plowBuilder().save(aConsumer, PLOW_ID);
		branchCutterBuilder().save(aConsumer, BRANCH_CUTTER_ID);
		senseBuilder().save(aConsumer, SENSE_ID);
		handDrillBuilder().save(aConsumer, HAND_DRILL_ID);
		// task p29-w5-t5-scene-six — the six scene-tool rows (the same steel-route shape)
		scissorsBuilder().save(aConsumer, SCISSORS_ID);
		scoopBuilder().save(aConsumer, SCOOP_ID);
		net.minecraft.world.item.Item tRubberPlate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Rubber);
		if (tRubberPlate != null) plungerBuilder(tRubberPlate).save(aConsumer, PLUNGER_ID); // the CR.ONLY_IF_HAS_RESULT face — no rubber plate, no row
		flintAndTinderFromFlintAndSteelBuilder().save(aConsumer, FLINT_AND_TINDER_ID);
		flintAndSteelBuilder().save(aConsumer, FLINT_AND_STEEL_ID);
		rollingPinBuilder().save(aConsumer, ROLLING_PIN_ID);
		bendingCylinderBuilder().save(aConsumer, BENDING_CYLINDER_ID);
		// task p29-w5-t6-electric-nineteen — the fifteen electric rows (the :356-377 convergence)
		for (ElectricToolRow tRow : electricToolRows()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		pocketMultitoolBuilder().save(aConsumer, POCKET_MULTITOOL_ID);
		// task p29-w5-t8-armor-24 — the 24 hazmat rows (tail-append)
		for (GT6ArmorMaterials.SuitRow tSuit : GT6ArmorMaterials.SUITS) {
			for (int i = 0; i < GT6ArmorMaterials.PIECE_TYPES.length; i++) {
				armorPieceBuilder(tSuit, i).save(aConsumer, armorRecipeId(tSuit, i));
			}
		}
		// task p31-dig-ladder — the per-material identity-stamped rows (the axis walk)
		digLadderRows(aConsumer);
		// task p31-machine-ladder — the machine family material rows (the identity-stamped walk)
		machineLadderRows(aConsumer);
		// task p33-circuits-crafting-c — the circuits band: the 26 integrated-circuit rows
		// (gt6:circuit_program) + the ventilation/processor-unit six (gt6 shaped)
		circuitProgramRows(aConsumer);
		partCircuitRows(aConsumer);
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
		clayBowlReverseBuilder().save(aOutput, CLAY_BOWL_REVERSE_ID);
		clayBowlSmeltingBuilder().save(aOutput, CLAY_BOWL_SMELT_ID);
		anvilBuilder(GT6Anvils.STONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.STONE).save(aOutput, STONE_ANVIL_ID);
		anvilBuilder(GT6Anvils.BLACKSTONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.BLACKSTONE).save(aOutput, BLACKSTONE_ANVIL_ID);
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) {
			hopperRecipeBuilder(tRow).save(aOutput, hopperRecipeId(tRow));
		}
		progressmeterBuilder().save(aOutput, PROGRESSMETER_ID);
		fluidometerBuilder().save(aOutput, FLUIDOMETER_ID);
		feConverterBuilder().save(aOutput, FE_CONVERTER_ID);
		waterWheelBuilder().save(aOutput, WATER_WHEEL_ID); // task p30-pool-waterwheel-neo-recipes — the forge branch row (this file :195), the 21.1 face was born without it
		transformerBuilder().save(aOutput, ELECTRIC_TRANSFORMER_ID);
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
		for (GT6Batteries.BatteryRow tRow : GT6Batteries.ROWS) {
			if (tRow.family().startsWith("energium")) continue; // the crystals carry NO rows (upstream :1079-:1092, the declared cut)
			batteryRecipeBuilder(tRow).save(aOutput, batteryRecipeId(tRow));
		}
		// task p32-placeables — the Greg o'Lantern row
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
		// task p29-w5-t1-dig-six — the six dig-tool steel-route rows (the wrench row shape)
		pickaxeBuilder().save(aOutput, PICKAXE_ID);
		pickaxeGemBuilder().save(aOutput, PICKAXE_GEM_ID);
		pickaxeConstructionBuilder().save(aOutput, PICKAXE_CONSTRUCTION_ID);
		shovelBuilder().save(aOutput, SHOVEL_ID);
		spadeBuilder().save(aOutput, SPADE_ID);
		universalSpadeBuilder().save(aOutput, UNIVERSAL_SPADE_ID);
		// task p29-w5-t2-blade-six — the six blade-tool steel-route rows (the dig-tool row shape)
		clubBuilder().save(aOutput, CLUB_ID);
		// task p31-blade-ladder — the three blade forms move to the per-material rows
		// (the OreProcessing_Tool rows Loader_Tools.java:321-323; the t1 steel-route
		// convergence placeholders for sword/knife/butchery retire — club stays the
		// single-tier convergence row, the single-tier-ruling card owns it)
		bladeLadderRows(aOutput);
		axeBuilder().save(aOutput, AXE_ID);
		axeDoubleBuilder().save(aOutput, AXE_DOUBLE_ID);
		// task p29-w5-t4-field-five — the five field-tool steel-route rows (the t1 shape)
		hoeBuilder().save(aOutput, HOE_ID);
		plowBuilder().save(aOutput, PLOW_ID);
		branchCutterBuilder().save(aOutput, BRANCH_CUTTER_ID);
		senseBuilder().save(aOutput, SENSE_ID);
		handDrillBuilder().save(aOutput, HAND_DRILL_ID);
		// task p29-w5-t5-scene-six — the six scene-tool rows (the same steel-route shape)
		scissorsBuilder().save(aOutput, SCISSORS_ID);
		scoopBuilder().save(aOutput, SCOOP_ID);
		net.minecraft.world.item.Item tRubberPlate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Rubber);
		if (tRubberPlate != null) plungerBuilder(tRubberPlate).save(aOutput, PLUNGER_ID); // the CR.ONLY_IF_HAS_RESULT face — no rubber plate, no row
		flintAndTinderFromFlintAndSteelBuilder().save(aOutput, FLINT_AND_TINDER_ID);
		flintAndSteelBuilder().save(aOutput, FLINT_AND_STEEL_ID);
		rollingPinBuilder().save(aOutput, ROLLING_PIN_ID);
		bendingCylinderBuilder().save(aOutput, BENDING_CYLINDER_ID);
		// task p29-w5-t6-electric-nineteen — the fifteen electric rows (the :356-377 convergence)
		for (ElectricToolRow tRow : electricToolRows()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		pocketMultitoolBuilder().save(aOutput, POCKET_MULTITOOL_ID);
		// task p29-w5-t8-armor-24 — the 24 hazmat rows (tail-append)
		for (GT6ArmorMaterials.SuitRow tSuit : GT6ArmorMaterials.SUITS) {
			for (int i = 0; i < GT6ArmorMaterials.PIECE_TYPES.length; i++) {
				armorPieceBuilder(tSuit, i).save(aOutput, armorRecipeId(tSuit, i));
			}
		}
		// task p31-dig-ladder — the per-material identity-stamped rows (the axis walk)
		digLadderRows(aOutput);
		machineLadderRows(aOutput);
		// task p33-circuits-crafting-c — the circuits band: the 26 integrated-circuit rows
		// (gt6:circuit_program) + the ventilation/processor-unit six (gt6 shaped)
		circuitProgramRows(aOutput);
		partCircuitRows(aOutput);
	}
	*///?}

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
	 * The static storage crafting rows (task p26-storage-static-batch — one per row):
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
	 * The grass dye band (task p24-grass-block) — the upstream BlockGrass.java:72-80
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
	 * The hammer STONE route (task p25-tool-hammer-wrench spec ⑤α) — the upstream
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
	 * The hammer METAL route (task p25-tool-hammer-wrench spec ⑤β) — the upstream
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
	 * The wrench self-craft row (task p25-tool-hammer-wrench spec ⑤γ) — the upstream
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
	 * The bending-cylinder SELF-CRAFT row (task p25-food-can-row0 spec ②) — the upstream
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
	 * The storage-hopper crafting rows (task p26-storage-hopper-family — the Loader
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
	 * The soft-hammer self-craft row (task p29-w5-t3-machine-face-four spec ①) — the
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
	 * The monkey-wrench self-craft row (task p29-w5-t3-machine-face-four spec ②) — the
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
	 * The POCKET MULTITOOL crafting row (task p29-w5-t7-pocket-eight) — the upstream
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
	 * The magnifying-glass self-craft row (task p29-w5-t3-machine-face-four spec ③) — the
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
	 * The pincers self-craft row (task p29-w5-t3-machine-face-four spec ④) — the upstream
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
	 * The empty-food-can crafting row (task p25-food-can-row0 spec ③) — the upstream
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
	 * The LARGE Steel Crucible crafting row (task p26-crucible-multiblock SPEC ⑦) — the
	 * upstream "hMy" row (Loader_MultiTileEntities.java:1270, 'M' = the wall item 18009
	 * 对位) with the declared port deviation that the soldering-tool family is not ported
	 * yet: the row runs "hM" ('h' = the hard-hammer tool tag, the hammerFromIngotsBuilder
	 * key; 'M' = gt6:crucible_steel_wall). The in-grid hammer pays one durability point
	 * and rides along (the container-item channel). Result 1x the controller block item.
	 */
	/**
	 * The crucible ladder crafting rows (task p29-w3-distill-crucible ③) — the
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
	 * The Diesel Engine crafting rows (task p29-w4-hot-lube spec ④) — the Loader
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
	// task p33-cracker-machines — the two Cracker crafting families (Loader
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

	/**
	 * The plate-mold crafting row (task p26-w1-press-extruder-molds) — the upstream
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
	 * The rod-mold crafting row (task p26-w1-press-extruder-molds) — the upstream
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
	// the kitchen band (task p26-kitchen-pot-bowl)
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
	 * The two stone anvil crafting rows (task p28-c-anvil) — the upstream registration
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
	 * The Greg o'Lantern crafting row (task p32-placeables) — the upstream
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
	 * The Clay Bowl reverse shapeless — the upstream :119 tail VERBATIM
	 * ({@code CR.shapeless(ST.make(Items.clay_ball, 5, 0), CR.DEF_NCC, new Object[] {last()})}):
	 * 1 raw bowl back to its 5 clay balls (the {@code OreDictItemData(MT.Clay, U*5)} mass).
	 * Result 5x {@code minecraft:clay_ball}. The FORWARD shaped row (:132, the rolling-pin
	 * pattern {@code "k R","C C","CCC"}) is DECLARED DORMANT — its 'R' key is
	 * {@code OreDictToolNames.rollingpin} and the port carries no rolling-pin tool (the
	 * tools pool), so the Raw bowl has no crafting source yet; the smelt below stays the
	 * live half of the hardening chain.
	 */
	private ShapelessRecipeBuilder clayBowlReverseBuilder() {
		return ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 5)
				.requires(GT6Kitchen.CLAY_BOWL_RAW.get())
				.unlockedBy("has_clay_bowl_raw", has(GT6Kitchen.CLAY_BOWL_RAW.get()));
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
	 * The Progress Sensor crafting row (task p26-sensors-core) — the upstream
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
	 * The Fluid-O-Meter Sensor crafting row (task p26-sensors-core) — the upstream
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
	 * The ULV FE→EU converter crafting row (task p28-b-fe-converter-machine, retuned by
	 * task p28-ulv-recipe-retune — the stone-crucible ruling): the progressmeter vocabulary
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
	 * The Water Wheel crafting row (task p28-c-water-wheel) — the kTFRUAddon
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
	 * The Electric Transformer (ULV-LV) crafting row (task p28-c-ulv-lv-transformer) —
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
	// task p29-w4-battery-storage — the battery + BatteryBox crafting rows (the Loader
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
					// the LARGE rows carry the TIER TRANSFORMER item (getItem(10040+i)) — only 10040 exists
			if (tRow.slots() == 16) {
				if (tRow.tier() != 0) continue; // the declared cut: large tiers 1..5 ride the transformer-ladder pool
				tM = GT6ElectricTransformers.ELECTRIC_TRANSFORMER_ITEM.get();
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

	private ShapedRecipeBuilder transformerBuilder() {
		TagKey<Item> tFineWires = GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY, MT.Copper);
		TagKey<Item> tDoublePlates = GT6ItemTags.materialTag(GT6ItemTags.DOUBLE_PLATES_FAMILY, MT.Iron);
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6ElectricTransformers.ELECTRIC_TRANSFORMER_ITEM.get())
				.pattern(GT6ElectricTransformers.RECIPE_PATTERN[0])
				.pattern(GT6ElectricTransformers.RECIPE_PATTERN[1])
				.pattern(GT6ElectricTransformers.RECIPE_PATTERN[2])
				.define('W', tFineWires)
				.define('X', tFineWires)
				.define('I', tDoublePlates)
				.define('M', GTMaterialItems.get(gregapi.data.OP.casingSmall, gregapi.data.MT.SteelGalvanized).get())
				.unlockedBy("has_fine_wire", has(tFineWires));
	}

	// -------------------------------------------------------------------------
	// task p29-w4-eu-bridge — the EU-bridge crafting rows (Loader :817-821/:833-837/
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

	/** The Electric_T[1..5] rung material by ladder index (upstream MT.java:3691 members, the dynamo family's ladder face). */
	private static gregapi.oredict.OreDictMaterial bridgeMat(int aTier) {
		gregapi.oredict.OreDictMaterial[] tMats = {gregapi.data.MT.SteelGalvanized, gregapi.data.MT.Al, gregapi.data.MT.StainlessSteel, gregapi.data.MT.Cr, gregapi.data.MT.Ti};
		return tMats[aTier];
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
	 * The three CR.shapeless self-recast rows (task p26-sensors-core) — the upstream
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
    // task p29-w3-nbtdesign-parts ③ — the part-family crafting rows (Loader
    // :1138-1182 recipe strings, the tool letters per CR.java:339-361: h = hard
    // hammer, s = saw, w = wrench, x = wirecutter, d = screwdriver). Declared CUTS
    // (the absent-input rows ride the pool, the sensor-shaped-row precedent):
    //   - coils 18043/18044 (SiC/Os): the materials carry NO port item rows;
    //   - electrolyzer_part :18105 ('W' wireGt01 Pt + 'C' OD_CIRCUITS[6] absent);
    //   - distill_part :18102 / sluice_part :18106 ('P' pipeSmall/pipeMedium — the
    //     pipe prefixes are off the port item path);
    //   - ventilation_unit :1184 ('F' IL.Cover_Vent + 'E' IL.MOTORS[1] absent — RESOLVED
    //     by the p33-circuits-crafting-c band below, the ruling-B substitutions);
    //   - processor units :1185-1189 ('S/D/R/E' IL.Processor_Crystal_* absent — RESOLVED
    //     by the p33-circuits-crafting-c band below, the gem-tag substitutions).
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
    // task p33-circuits-crafting-c — the circuits C-column band (coordinator ruling B,
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
    // task p33-circuits-crafting-c — the 26 integrated-circuit rows (the upstream
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
    // task p29-w3-tank-valves — the 25 Tank Main Valve rows (Loader :1195-1222 recipe
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
    // The six dig-tool steel-route rows (task p29-w5-t1-dig-six). The upstream
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
    // The six blade-tool steel-route rows (task p29-w5-t2-blade-six). The upstream
    // AdvancedCraftingTool rows (Loader_Tools.java:337/:342-343 — SWORD toolHeadSword,
    // AXE toolHeadAxe, DOUBLE_AXE toolHeadAxeDouble; KNIFE/BUTCHERYKNIFE/CLUB carry NO
    // AdvancedCraftingTool row — the knife's grid face is the cutting-board pool, the
    // club's is the 6*U material scale) over the single steel tier: the plate counts
    // keep the head-amount PROPORTIONS (the t1 mapping; the exact toolHead mAmount
    // ladder is the standing pool cut). The worn hammer/file letters ride the dig-tool
    // builder (each pays one point through the crafting-remaining face).
    // -----------------------------------------------------------------------
    // The five field-tool steel-route rows (task p29-w5-t4-field-five). The upstream
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
    // task p29-w5-t5-scene-six — the six scene-tool rows. The OreProcessing_Tool
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
    // task p29-w5-t6-electric-nineteen — the fifteen electric-tool crafting rows
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

    // ─── the Hazmat armor band (task p29-w5-t8-armor-24, tail-append) ───

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
	// The dig ladder material rows (task p31-dig-ladder) — the upstream
	// OreProcessing_Tool shapes on the toolHead prefixes (Loader_Tools.java:293-300,
	// the And(ANTIMATTER.NOT, MT.Wood.NOT, COATED.NOT) axis), ONE gt6:material_tool
	// row per (dig form x plate+ingot material). The plain steel anchors above are the
	// identity-less steel arm; every other axis material gets its own stamped row (the
	// upstream second P-variant rows are cut: the C variant rides the plateGem ITEM and
	// the G variant the gem — the OreProcessing_Tool letter alphabet, Loader_Tools
	// :393-404 — the blade ladder kept C where its plateGem item truth exists; the dig
	// family keeps the P/I rows only, the declared cut).

	/**
	 * M5 unification (task p31-machine-ladder): the upstream {@code MT.Wood.NOT} axis
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
	private static final String[][] DIG_LADDER_FORMS = {
			{"pickaxe", "PII", "f h"},
			{"pickaxe_construction", "PIP", "f h"},
			{"shovel", "fPh"},
			{"spade", "fPh", " s "},
			{"hoe", "PIh", "f  "},
			{"axe", "PIh", "P  ", "f  "},
	};

	/** The upstream axis filter ∩ the port's plate+ingot item truth; Steel excluded (the anchors above own it). */
	private static java.util.List<gregapi.oredict.OreDictMaterial> digLadderMaterials() {
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

	/** The result item of a dig ladder form (the GT6Tools registry face). */
	private static net.minecraft.world.item.Item digLadderResult(String aForm) {
		return switch (aForm) {
			case "pickaxe" -> GT6Tools.PICKAXE.get();
			case "pickaxe_construction" -> GT6Tools.PICKAXE_CONSTRUCTION.get();
			case "shovel" -> GT6Tools.SHOVEL.get();
			case "spade" -> GT6Tools.SPADE.get();
			case "hoe" -> GT6Tools.HOE.get();
			case "axe" -> GT6Tools.AXE.get();
			default -> throw new IllegalArgumentException("unknown dig ladder form: " + aForm);
		};
	}

	/** The row letters (the upstream OreProcessing_Tool alphabet over the P/I variant). */
	private static TagKey<Item> digLadderIngredient(char aKey, gregapi.oredict.OreDictMaterial aMaterial) {
		String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(aMaterial.mNameInternal);
		return switch (aKey) {
			case 'P' -> GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake);
			case 'I' -> GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, tSnake);
			case 'h' -> GT6ItemTags.TOOLS_HARD_HAMMER;
			case 'f' -> GT6ItemTags.TOOLS_FILE;
			case 's' -> Tags.Items.RODS_WOODEN;
			default -> throw new IllegalArgumentException("unknown dig ladder letter: " + aKey);
		};
	}

	/** The row id (the result-path convention with the material leaf). */
	private static ResourceLocation digLadderRowId(String aForm, String aSnake) {
		String tPath = aForm + "/" + aSnake;
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

//? if forge {
	private void digLadderRows(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		for (gregapi.oredict.OreDictMaterial tMaterial : digLadderMaterials()) {
			String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(tMaterial.mNameInternal);
			TagKey<Item> tPlate = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake);
			for (String[] tForm : DIG_LADDER_FORMS) {
				ResourceLocation tId = digLadderRowId(tForm[0], tSnake);
				java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
				java.util.List<String> tPattern = new ArrayList<>();
				for (int i = 1; i < tForm.length; i++) {
					tPattern.add(tForm[i]);
					for (char tChar : tForm[i].toCharArray()) {
						if (tChar != ' ') tKey.put(tChar, net.minecraft.world.item.crafting.Ingredient.of(digLadderIngredient(tChar, tMaterial)));
					}
				}
				net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
						.recipeAdvancement()
						.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
						.addCriterion("has_" + tSnake, has(tPlate))
						.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
						.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
						.requirements(net.minecraft.advancements.RequirementsStrategy.OR);
				aConsumer.accept(new MaterialToolRow(tId, tId.withPrefix("recipes/tools/"),
						net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT, tPattern, tKey,
						digLadderResult(tForm[0]), tSnake, tAdvancement));
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
			java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> aKey, net.minecraft.world.item.Item aResult, String aMaterial,
			net.minecraft.advancements.Advancement.Builder aAdvancement)
			implements net.minecraft.data.recipes.FinishedRecipe {

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
				ResourceLocation tId = digLadderRowId(tForm[0], tSnake);
				java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
				java.util.List<String> tPattern = new ArrayList<>();
				for (int i = 1; i < tForm.length; i++) {
					tPattern.add(tForm[i]);
					for (char tChar : tForm[i].toCharArray()) {
						if (tChar != ' ') tKey.put(tChar, net.minecraft.world.item.crafting.Ingredient.of(digLadderIngredient(tChar, tMaterial)));
					}
				}
				net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
						.recipeAdvancement()
						.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
						.addCriterion("has_" + tSnake, has(tPlate))
						.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
						.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
						.requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR);
				gregtech6.items.tools.GT6MaterialToolRecipe tRecipe = new gregtech6.items.tools.GT6MaterialToolRecipe("",
						net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
						net.minecraft.world.item.crafting.ShapedRecipePattern.of(tKey, tPattern),
						new net.minecraft.world.item.ItemStack(digLadderResult(tForm[0])), true, tSnake);
				aOutput.accept(tId, tRecipe, tAdvancement.build(tId.withPrefix("recipes/tools/")));
			}
		}
	}
	*///?}

	// ------------------------------------------------------------------------
	// The blade ladder material rows (task p31-blade-ladder) — the upstream
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

	/** The result item of a blade ladder form (the GT6Tools registry face). */
	private static net.minecraft.world.item.Item bladeLadderResult(String aForm) {
		return switch (aForm) {
			case "sword", "sword_gem" -> GT6Tools.SWORD.get();
			case "knife", "knife_gem" -> GT6Tools.KNIFE.get();
			case "butchery_knife", "butchery_knife_gem" -> GT6Tools.BUTCHERY_KNIFE.get();
			default -> throw new IllegalArgumentException("unknown blade ladder form: " + aForm);
		};
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
				net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
						.recipeAdvancement()
						.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
						.addCriterion("has_head_material", has(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake)))
						.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
						.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
						.requirements(net.minecraft.advancements.RequirementsStrategy.OR);
				aConsumer.accept(new MaterialToolRow(tId, tId.withPrefix("recipes/tools/"),
						net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT, tPattern, tKey,
						bladeLadderResult(tForm.aId()), tSnake, tAdvancement));
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
						new net.minecraft.world.item.ItemStack(bladeLadderResult(tForm.aId())), true, tSnake);
				aOutput.accept(tId, tRecipe, tAdvancement.build(tId.withPrefix("recipes/tools/")));
			}
		}
	}
	*///?}

	// ------------------------------------------------------------------------
	// The machine ladder material rows (task p31-machine-ladder) — the upstream
	// OreProcessing_Tool mToolRecipes over the machine rows (Loader_Tools.java:305-316
	// chisel/screwdriver/saw, :310-311 wrench/monkey wrench, :314 crowbar, :324 cutter,
	// :327-328 hammer/soft hammer, :316 pincers), ONE gt6:material_tool row per
	// (machine form x axis material). Letters (the OreProcessing_Tool alphabet
	// Loader_Tools:393-404 + the CR.java:193-217 lowercase TOOL letters): P = plate,
	// I = ingot, S = stick, T = screw, X = plateCurved (ALL OF THE MATERIAL); h/f/d/r
	// = the hammer/file/screwdriver/soft-hammer TOOL tags; V = the blue dye (the
	// crowbar's :314 special). The C/G (plateGem/gem) SECOND variants stay cut (the
	// dig-family declared cut; the blade family is the only plateGem consumer). The
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

	/** One machine ladder form: the id, the axis gates, the Steel inclusion, the stamp multiplier, the upstream row shape. */
	private record MachineLadderForm(String aId, int aTypeMin, boolean aNoCoated, boolean aNoSoftTag, boolean aSoftTag,
			boolean aNoExtruder, int aQualMin, boolean aIncludeSteel, String[] aPattern) {
	}

	private static final MachineLadderForm[] MACHINE_LADDER_FORMS = {
			new MachineLadderForm("screwdriver", 2, true, false, false, false, 0, true, new String[] {"hS", "Sf"}), // :306
			new MachineLadderForm("saw", 2, true, false, false, false, 0, true, new String[] {"PP", "fh"}), // :307
			new MachineLadderForm("chisel", 2, true, false, false, false, 0, true, new String[] {"hPf", " S "}), // :305
			new MachineLadderForm("crowbar", 0, false, false, false, false, 0, true, new String[] {"hVS", "VSV", "SVf"}), // :314
			new MachineLadderForm("cutter", 2, false, true, false, false, 0, true, new String[] {"PfP", "hPd", "STS"}), // :324
			new MachineLadderForm("hammer", 0, true, true, false, false, 0, false, new String[] {"II ", "IIh", "II "}), // :327
			new MachineLadderForm("wrench", 2, false, false, false, false, 1, false, new String[] {"PhP", " P ", " P "}), // :310
			new MachineLadderForm("monkey_wrench", 2, false, false, false, false, 1, false, new String[] {"PPd", "hPT", " P "}), // :311
			new MachineLadderForm("pincers", 2, false, false, false, false, 0, false, new String[] {"XhX", " T ", "SdS"}), // :316
	};

	/** The result item of a machine ladder form (the GT6Tools registry face). */
	private static net.minecraft.world.item.Item machineLadderResult(String aForm) {
		return switch (aForm) {
			case "screwdriver" -> GT6Tools.SCREWDRIVER.get();
			case "saw" -> GT6Tools.SAW.get();
			case "chisel" -> GT6Tools.CHISEL.get();
			case "crowbar" -> GT6Tools.CROWBAR.get();
			case "cutter" -> GT6Tools.CUTTER.get();
			case "hammer" -> GT6Tools.HAMMER.get();
			case "soft_hammer" -> GT6Tools.SOFT_HAMMER.get();
			case "wrench" -> GT6Tools.WRENCH.get();
			case "monkey_wrench" -> GT6Tools.MONKEY_WRENCH.get();
			case "pincers" -> GT6Tools.PINCERS.get();
			default -> throw new IllegalArgumentException("unknown machine ladder form: " + aForm);
		};
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
			case 'h' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_HARD_HAMMER);
			case 'f' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_FILE);
			case 'd' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_SCREWDRIVER);
			case 'r' -> net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_SOFT_HAMMER);
			case 'V' -> net.minecraft.world.item.crafting.Ingredient.of(Tags.Items.DYES_BLUE);
			default -> throw new IllegalArgumentException("unknown machine ladder letter: " + aKey);
		};
	}

	/** The per-form axis gate — the upstream And() rows verbatim (see the block javadoc). */
	private static boolean machineLadderAxis(gregapi.oredict.OreDictMaterial aMaterial, MachineLadderForm aForm) {
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
		for (char tKey : new char[] {'S', 'T', 'X'}) {
			gregapi.oredict.OreDictPrefix tPrefix = tKey == 'S' ? gregapi.data.OP.stick : tKey == 'T' ? gregapi.data.OP.screw : gregapi.data.OP.plateCurved;
			if (tLetters.contains(tKey) && gregtech6.registry.GTMaterialItems.get(tPrefix, aMaterial) != null) {
				return gregtech6.registry.GTMaterialItems.get(tPrefix, aMaterial).get();
			}
		}
		throw new IllegalArgumentException("no material-carrying letter to anchor the advancement: " + aForm.aId());
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
				if (!tForm.aIncludeSteel() && tMaterial == gregapi.data.MT.Steel) continue; // the steel anchors own it
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
						machineLadderResult(tForm.aId()), tSnake, tAdvancement));
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
				if (!tForm.aIncludeSteel() && tMaterial == gregapi.data.MT.Steel) continue; // the steel anchors own it
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
						new net.minecraft.world.item.ItemStack(machineLadderResult(tForm.aId())), true, tSnake);
				aOutput.accept(tId, tRecipe, tAdvancement.build(tId.withPrefix("recipes/tools/")));
			}
		}
	}

*///?}

}

