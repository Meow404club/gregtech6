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

import gregtech6.datagen.GT6ItemTags;
import gregtech6.registry.GT6Batteries;
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
	/** The empty-food-can crafting row (task p25-food-can-row0 spec ③, MultiItemRandomTools.java:239). */
	public static final ResourceLocation FOOD_CAN_EMPTY_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "food_can_empty");
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
	public static final ResourceLocation SWORD_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "sword");
	public static final ResourceLocation KNIFE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "knife");
	public static final ResourceLocation BUTCHERY_KNIFE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "butchery_knife");
	public static final ResourceLocation CLUB_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "club");
	public static final ResourceLocation AXE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "axe");
	public static final ResourceLocation AXE_DOUBLE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "axe_double");

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
		for (BatteryBoxRecipeRow tRow : batteryBoxRecipeBuilders()) {
			tRow.builder().save(aConsumer, batteryBoxRecipeId(tRow.row()));
		}
		for (DieselEngineRecipeRow tRow : dieselEngineRecipeBuilders()) {
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
		swordBuilder().save(aConsumer, SWORD_ID);
		knifeBuilder().save(aConsumer, KNIFE_ID);
		butcheryKnifeBuilder().save(aConsumer, BUTCHERY_KNIFE_ID);
		clubBuilder().save(aConsumer, CLUB_ID);
		axeBuilder().save(aConsumer, AXE_ID);
		axeDoubleBuilder().save(aConsumer, AXE_DOUBLE_ID);
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
		for (BatteryBoxRecipeRow tRow : batteryBoxRecipeBuilders()) {
			tRow.builder().save(aOutput, batteryBoxRecipeId(tRow.row()));
		}
		for (DieselEngineRecipeRow tRow : dieselEngineRecipeBuilders()) {
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
		swordBuilder().save(aOutput, SWORD_ID);
		knifeBuilder().save(aOutput, KNIFE_ID);
		butcheryKnifeBuilder().save(aOutput, BUTCHERY_KNIFE_ID);
		clubBuilder().save(aOutput, CLUB_ID);
		axeBuilder().save(aOutput, AXE_ID);
		axeDoubleBuilder().save(aOutput, AXE_DOUBLE_ID);
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
    //   - ventilation_unit :1184 ('F' IL.Cover_Vent + 'E' IL.MOTORS[1] absent);
    //   - processor units :1185-1189 ('S/D/R/E' IL.Processor_Crystal_* absent).
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
    /** The sword row — the upstream :337 SWORD head + the rod, hammer+file worn. */
    private ShapedRecipeBuilder swordBuilder() {
        return digToolBuilder(GT6Tools.SWORD.get(), new String[] {"hPf", " S "},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The knife row — the small blade (1 plate + the rod). */
    private ShapedRecipeBuilder knifeBuilder() {
        return digToolBuilder(GT6Tools.KNIFE.get(), new String[] {"hP ", " S ", " f "},
                'P', DIG_STEEL_PLATES, 'S', Tags.Items.RODS_WOODEN,
                'h', GT6ItemTags.TOOLS_HARD_HAMMER, 'f', GT6ItemTags.TOOLS_FILE);
    }

    /** The butchery knife row — the heavier blade (2 plates, the 4*U scale). */
    private ShapedRecipeBuilder butcheryKnifeBuilder() {
        return digToolBuilder(GT6Tools.BUTCHERY_KNIFE.get(), new String[] {"PP", " S", "hf"},
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
}
