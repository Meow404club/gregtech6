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
import net.minecraftforge.registries.RegistryObject;

//? if forge {
import net.minecraftforge.common.Tags;
//?} else {
/*import net.neoforged.neoforge.common.Tags;
*///?}

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
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
import gregtech6.registry.GT6PressMolds;
import gregtech6.registry.GT6BakeFoods;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6FoodCans;
import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GT6Kitchen;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6SprayCans;
import gregtech6.registry.GT6SurfaceBlocks;
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
	/** The pocket multitool recipe id (task w5-t7-pocket-eight, the result-path convention). */
	public static final ResourceLocation POCKET_MULTITOOL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pocket_multitool");
	/** The empty-food-can crafting row (task food-can-row0 spec ③, MultiItemRandomTools.java:239). */
	public static final ResourceLocation FOOD_CAN_EMPTY_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "food_can_empty");
	/** The machine-face four self-craft rows (task w5-t3-machine-face-four) — the result-path convention, one per tool. */
	public static final ResourceLocation SOFT_HAMMER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "soft_hammer");
	public static final ResourceLocation MONKEY_WRENCH_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "monkey_wrench");
	public static final ResourceLocation MAGNIFYING_GLASS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "magnifying_glass");
	public static final ResourceLocation PINCERS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "pincers");
	/** The bullet-casing mold crafting trio (task explosives-chain, MultiItemTechnological.java:356-358 strokes). */
	public static final ResourceLocation SHAPE_PRESS_SMALL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_press_bullet_casing_small");
	public static final ResourceLocation SHAPE_PRESS_MEDIUM_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_press_bullet_casing_medium");
	public static final ResourceLocation SHAPE_PRESS_LARGE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_press_bullet_casing_large");
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
	/** The Sifting Table crafting row (task sifting-table-family, Loader_MultiTileEntities.java:2227) — the result-path convention. */
	public static final ResourceLocation SIFTING_TABLE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "sifting_table");

	/** The Grindstone crafting row id (task grindstone-family, the :2226 row, result-path convention). */
	public static final ResourceLocation GRINDSTONE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "grindstone");

	/** The storage-hopper crafting ids — the result-path convention, one per row (Loader :145-146). */
	public static final java.util.List<ResourceLocation> HOPPER_RECIPE_IDS = gregtech6.registry.GT6Hoppers.ROWS.stream()
			.map(GT6CraftingRecipes::hopperRecipeId)
			.collect(java.util.stream.Collectors.toList());

	/** The Sap Bag crafting row id (task block-family-32xxx-port, the :2221 row, the result-path convention). */
	public static final ResourceLocation SAP_BAG_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "sap_bag");

	/**
	 * The id of one charging-locker row's recipe (the hopperRecipeId shape). The path
	 * rides a local so the two-arg RL ctor args stay bare identifiers (the swap-table
	 * regex note).
	 */
	public static ResourceLocation chargingLockerRecipeId(gregtech6.registry.GT6ChargingLockers.ChargingLockerRow aRow) {
		String tPath = aRow.path();
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

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
	/** The circuit wiring crafting row (task circuit-chain-recipes, MIT:571) — the result-path convention. */
	public static final ResourceLocation CIRCUIT_WIRE_COPPER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "circuit_wire_copper");
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
	public static final ResourceLocation AXE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "axe");
	public static final ResourceLocation AXE_DOUBLE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "axe_double");

	/** The five field-tool row ids (task w5-t4-field-five — the t1 row-id shape). */
	public static final ResourceLocation HOE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "hoe");
	public static final ResourceLocation PLOW_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "plow");
	public static final ResourceLocation SENSE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "sense");

	/** The scene-tool row ids (task w5-t5-scene-six — the same CR row id shape; the flint pair is TWO rows). */
	public static final ResourceLocation FLINT_AND_TINDER_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "flint_and_tinder");
	public static final ResourceLocation FLINT_AND_STEEL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "flint_and_steel");
	/** The rolling-pin WOOD-route row (Loader_Recipes_Woods.java:237) — the metal/plastic ladder (:250-253) rides the arg-8 band's per-material rows. */
	public static final ResourceLocation ROLLING_PIN_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "rolling_pin");

	/** The CR.shapeless self-recast row id of a sensor path (task sensors-core) — the path + the {@code _recast} suffix (the grass reverse-row suffix shape). */
	public static ResourceLocation sensorRecastId(String aPath) {
		return new ResourceLocation(GT6DataGenerators.MOD_ID, aPath + "_recast");
	}

	//? if forge {
	/** The recipe JSON path provider — the run() override re-derives it (the vanilla 1.20.1 field is private). */
	private final PackOutput.PathProvider mRecipePaths;
	//?}

	public GT6CraftingRecipes(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookupProvider) {
		//? if forge {
		super(aOutput);
		mRecipePaths = aOutput.createPathProvider(PackOutput.Target.DATA_PACK, "recipes");
		//?} else {
		/*super(aOutput, aLookupProvider);
		*///?}
	}

	/**
	 * The unlock-advancement stop (2026-10-03 user ruling, remember id1359: a tech mod's
	 * players run JEI/EMI — the vanilla recipe book is dead weight). Both legs run the
	 * vanilla flow with the advancement persistence branch removed: recipe JSONs
	 * byte-identical, zero advancement files (the 32009+4 committed stock deleted by the
	 * same card). Forge: overrides {@code RecipeProvider.run} (1.20.1 RecipeProvider.java:87,
	 * overridable) verbatim minus the {@code serializeAdvancement()} branch. 21.1: the one-arg
	 * {@code run} (:77) is final, so the two-arg (:81) is overridden with an
	 * advancement-dropping RecipeOutput — accept() never persists the holder (vanilla's own
	 * anonymous output already guards {@code if (advancement != null)}).
	 */
	//? if forge {
	@Override
	public CompletableFuture<?> run(net.minecraft.data.CachedOutput aCache) {
		java.util.Set<ResourceLocation> tSeen = new java.util.HashSet<>();
		java.util.List<CompletableFuture<?>> tFutures = new ArrayList<>();
		this.buildRecipes(tRow -> {
			if (!tSeen.add(tRow.getId())) throw new IllegalStateException("Duplicate recipe " + tRow.getId());
			// the row-level convergence seam (task parse-errors-registration-convergence): rows
			// referencing seed-hidden foreign ids carry forge:mod_loaded conditions — the ONE
			// consumer every crafting family flows through, so this is the family-uniform form.
			tRow = GT6ForeignRowConvergence.converged(tRow);
			tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, tRow.serializeRecipe(), mRecipePaths.json(tRow.getId())));
		});
		return CompletableFuture.allOf(tFutures.toArray(new CompletableFuture[0]));
	}
	//?} else {
	/*@Override
	protected CompletableFuture<?> run(net.minecraft.data.CachedOutput aCache, HolderLookup.Provider aRegistries) {
		java.util.Set<ResourceLocation> tSeen = new java.util.HashSet<>();
		java.util.List<CompletableFuture<?>> tFutures = new ArrayList<>();
		PackOutput.PathProvider tRecipePaths = recipePathProvider;
		this.buildRecipes(new net.minecraft.data.recipes.RecipeOutput() {
			@Override
			public void accept(ResourceLocation aId, net.minecraft.world.item.crafting.Recipe<?> aRecipe,
					net.minecraft.advancements.AdvancementHolder aAdvancement,
					net.neoforged.neoforge.common.conditions.ICondition... aConditions) {
				if (!tSeen.add(aId)) throw new IllegalStateException("Duplicate recipe " + aId);
				tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, aRegistries,
						net.minecraft.world.item.crafting.Recipe.CONDITIONAL_CODEC,
						java.util.Optional.of(GT6ForeignRowConvergence.conditioned(aRecipe, aRegistries, aConditions)),
						tRecipePaths.json(aId)));
			}

			@Override
			public net.minecraft.advancements.Advancement.Builder advancement() {
				return net.minecraft.advancements.Advancement.Builder.recipeAdvancement()
						.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
			}
		}, aRegistries);
		return CompletableFuture.allOf(tFutures.toArray(new CompletableFuture[0]));
	}
	*///?}

	//? if forge {
	@Override
	protected void buildRecipes(Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		sprayCanEmptyBuilder().save(aConsumer, SPRAY_CAN_EMPTY_ID);
		hammerFromStoneBuilder().save(aConsumer, HAMMER_STONE_ID);
		hammerFromIngotsBuilder().save(aConsumer, HAMMER_INGOTS_ID);
		wrenchBuilder().save(aConsumer, WRENCH_ID);
		foodCanEmptyBuilder().save(aConsumer, FOOD_CAN_EMPTY_ID);
		softHammerBuilder().save(aConsumer, SOFT_HAMMER_ID);
		monkeyWrenchBuilder().save(aConsumer, MONKEY_WRENCH_ID);
		magnifyingGlassBuilder().save(aConsumer, MAGNIFYING_GLASS_ID);
		pincersBuilder().save(aConsumer, PINCERS_ID);
		for (MoldRecipeRow tRow : extruderMoldChainRows()) tRow.builder().save(aConsumer, tRow.id()); // task mold-extruder-shapes — the full 64-row mold chain
		shapePressBulletCasingSmallBuilder().save(aConsumer, SHAPE_PRESS_SMALL_ID); // task explosives-chain
		shapePressBulletCasingMediumBuilder().save(aConsumer, SHAPE_PRESS_MEDIUM_ID);
		shapePressBulletCasingLargeBuilder().save(aConsumer, SHAPE_PRESS_LARGE_ID);
		for (FoodMoldRecipeRow tRow : foodMoldRecipeBuilders()) tRow.builder().save(aConsumer, tRow.id()); // task food-bake-items
		for (BakeCraftRow tRow : bakeCraftRows()) {if (tRow.shaped() != null) tRow.shaped().save(aConsumer, tRow.id()); else tRow.shapeless().save(aConsumer, tRow.id());} // task food-bake-recipes
		for (BakeCraftRow tRow : bottleCraftRows()) {if (tRow.shaped() != null) tRow.shaped().save(aConsumer, tRow.id()); else tRow.shapeless().save(aConsumer, tRow.id());} // task food-bottles-min — the independent bottles band
		for (BakeSmeltRow tRow : bakeSmeltRows()) tRow.builder().save(aConsumer, tRow.id()); // task food-bake-recipes
		largeSteelCrucibleBuilder().save(aConsumer, LARGE_STEEL_CRUCIBLE_ID);
		bathingPotSteelBuilder().save(aConsumer, BATHING_POT_STEEL_ID);
		clayBowlForwardBuilder().save(aConsumer, CLAY_BOWL_FORWARD_ID);
		clayBowlReverseBuilder().save(aConsumer, CLAY_BOWL_REVERSE_ID);
		clayBowlSmeltingBuilder().save(aConsumer, CLAY_BOWL_SMELT_ID);
		clayJuicerBuilder().save(aConsumer, CLAY_JUICER_ID);
		clayJuicerReverseBuilder().save(aConsumer, CLAY_JUICER_REVERSE_ID);
		clayJuicerSmeltingBuilder().save(aConsumer, CLAY_JUICER_SMELT_ID);
		for (BakeSmeltRow tRow : clayPitSmeltRows()) tRow.builder().save(aConsumer, tRow.id()); // task worldgen-diggables-pits — the 4 colored-clay hardening smelts
		anvilBuilder(GT6Anvils.STONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.STONE).save(aConsumer, STONE_ANVIL_ID);
		anvilBuilder(GT6Anvils.BLACKSTONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.BLACKSTONE).save(aConsumer, BLACKSTONE_ANVIL_ID);
		for (gregtech6.registry.GT6Mortars.MortarRow tRow : gregtech6.registry.GT6Mortars.ROWS) { // task mortar-family
			ShapelessRecipeBuilder tMortarBuilder = mortarBuilder(tRow);
			if (tMortarBuilder == null) continue; // the row's ingredient is driver-hidden — the JSON skip semantics
			tMortarBuilder.save(aConsumer, mortarRecipeId(tRow));
		}
		siftingTableBuilder().save(aConsumer, SIFTING_TABLE_ID); // task sifting-table-family
		grindstoneBuilder().save(aConsumer, GRINDSTONE_ID); // task grindstone-family — the :2226 row
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) {
			ShapedRecipeBuilder tBuilder = hopperRecipeBuilder(tRow);
			if (tBuilder == null) continue; // the row's material is driver-hidden — the JSON/wall skip semantics (mdh-3 wave readiness)
			tBuilder.save(aConsumer, hopperRecipeId(tRow));
		}
		for (gregtech6.registry.GT6Chests.ChestRow tRow : gregtech6.registry.GT6Chests.ROWS) { // task material-mc-a-storage-chests
			ShapedRecipeBuilder tBuilder = chestRecipeBuilder(tRow);
			if (tBuilder == null) continue; // the row's material is driver-hidden — the JSON skip semantics (mdh-3 wave readiness)
			tBuilder.save(aConsumer, chestRecipeId(tRow));
		}
		// task block-family-32xxx-port — the charging locker rows ride the A RULING: only
		// the rows whose 'M' (the SAME-material plain locker) exists in the port land, the
		// rest stay ungenerated (the registry-class doc; the mc-A1 backfill owns the rest)
		for (gregtech6.registry.GT6ChargingLockers.ChargingLockerRow tRow : gregtech6.registry.GT6ChargingLockers.ROWS) {
			ShapedRecipeBuilder tBuilder = chargingLockerRecipeBuilder(tRow);
			if (tBuilder == null) continue; // the A ruling — no plain locker at this material
			tBuilder.save(aConsumer, chargingLockerRecipeId(tRow));
		}
		sapBagBuilder().save(aConsumer, SAP_BAG_ID);
		// the plant pot row DEFERS: its 'U' column is IL.Ceramic_Basin — an unported item
		// (the class doc of the builder; the reported ruling seat)
		progressmeterBuilder().save(aConsumer, PROGRESSMETER_ID);
		circuitWireCopperBuilder().save(aConsumer, CIRCUIT_WIRE_COPPER_ID); // task circuit-chain-recipes — MIT:571
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
			ShapedRecipeBuilder tBuilder = staticStorageRecipeBuilder(tRow);
			if (tBuilder == null) continue; // the row's material is driver-hidden — the hopper walk's skip semantics
			tBuilder.save(aConsumer, staticStorageRecipeId(tRow));
		}
		for (PartFamilyRecipeRow tRow : partFamilyRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (PartFamilyRecipeRow tRow : tankValveRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		// task crafting-barrels-boilers — the barrel pair + the 26 boiler rows
		for (PartFamilyRecipeRow tRow : barrelRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		for (PartFamilyRecipeRow tRow : boilerRecipeBuilders()) {
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
		// task w5-t2-blade-six — the six blade-tool steel-route rows (the dig-tool row shape)
		// task blade-ladder — the three blade forms move to the per-material rows
		// (the OreProcessing_Tool rows Loader_Tools.java:321-323; the t1 steel-route
		// convergence placeholders for sword/knife/butchery retired with the ladder)
		bladeLadderRows(aConsumer);
		axeBuilder().save(aConsumer, AXE_ID);
		axeDoubleBuilder().save(aConsumer, AXE_DOUBLE_ID);
		// task w5-t4-field-five — the five field-tool steel-route rows (the t1 shape)
		hoeBuilder().save(aConsumer, HOE_ID);
		plowBuilder().save(aConsumer, PLOW_ID);
		senseBuilder().save(aConsumer, SENSE_ID);
		// task w5-t5-scene-six — the scene-tool rows (the same steel-route shape; the
		// universal spade/club/branch cutter/hand drill/scissors/scoop/plunger/
		// bending cylinders ride the arg-8 per-material band — the w5 anchors retired)
		flintAndTinderFromFlintAndSteelBuilder().save(aConsumer, FLINT_AND_TINDER_ID);
		flintAndSteelBuilder().save(aConsumer, FLINT_AND_STEEL_ID);
		rollingPinBuilder().save(aConsumer, ROLLING_PIN_ID);
		// task w5-t6-electric-nineteen — the fifteen electric rows (the :356-377 convergence)
		for (ElectricToolRow tRow : electricToolRows()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
		pocketMultitoolBuilder().save(aConsumer, POCKET_MULTITOOL_ID);
		// task w5-t8-armor-24 — the 24 hazmat rows (tail-append)
		for (GT6ArmorMaterials.SuitRow tSuit : GT6ArmorMaterials.SUITS) {
			for (int i = 0; i < GT6ArmorMaterials.PIECE_TYPES.length; i++) {
				ShapedRecipeBuilder tArmorBuilder = armorPieceBuilder(tSuit, i);
				if (tArmorBuilder == null) continue; // the suit's 'M' material is driver-hidden (HEAT = Al) — the skip semantics
				tArmorBuilder.save(aConsumer, armorRecipeId(tSuit, i));
			}
		}
		// task dig-ladder — the per-material identity-stamped rows (the axis walk)
		digLadderRows(aConsumer);
		// task machine-ladder — the machine family material rows (the identity-stamped walk)
		machineLadderRows(aConsumer);
		// task tool-arg8-nine-families — the nine arg-8 direct-craft families + the
		// rolling-pin metal/plastic ladder (the identity-stamped walk, the w5 anchors retired)
		toolArg8Rows(aConsumer);
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
			ShapedRecipeBuilder tRailBuilder = railRecipeBuilder(tRailRow);
			if (tRailBuilder == null) continue; // the row's rail material is driver-hidden — the JSON/wall skip semantics (mdh-clearout-batch2 sweep)
			tRailBuilder.save(aConsumer, railRecipeId(tRailRow));
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
		// task toolhead-r11a-file-belt — the raw→finished tool-head file belt (Loader_Recipes_Handlers.java:420-434)
		for (FileBeltCraftFromRow tRow : fileBeltCraftFromDatagenRows()) {
			fileBeltCraftFromBuilder(tRow).save(aConsumer, tRow.aId());
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
			ShapedRecipeBuilder tRocksBuilder = slabFromRocksBuilder(tStone);
			if (tRocksBuilder == null) continue; // the family's rock material is driver-hidden (EtFu prismarines) — the skip semantics (mdh-clearout-batch2 sweep)
			tRocksBuilder.save(aConsumer, slabFromRocksId(tStone));
			slabFromCobbleBuilder(tStone).save(aConsumer, slabFromCobbleId(tStone));
		}

		// task debt-emitter-sensor-generators — the 20 live self-crafting rows of the three
		// technological component families (the pour map in the band javadoc below)
		compactComponentRows(aConsumer);
		// task robotics-chain — the 10 Autocrafter tip rows + the 37 live component-family
		// self-crafting rows (the pour map in the band doc below)
		robotTipRows(aConsumer);
		roboticsComponentRows(aConsumer);
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
		// task crafting-machines-steam-band — the steam-age bootstrap band: the
		// machine grids + the anvil forging ladder (the band javadocs at the tail)
		for (PartFamilyRecipeRow tRow : steamEngineRecipeBuilders()) tRow.builder().save(aConsumer, tRow.id());
		for (PartFamilyRecipeRow tRow : burningBoxRecipeBuilders()) tRow.builder().save(aConsumer, tRow.id());
		for (PartFamilyRecipeRow tRow : electricDynamoRecipeBuilders()) tRow.builder().save(aConsumer, tRow.id());
		for (PartFamilyRecipeRow tRow : sifterRecipeBuilders()) tRow.builder().save(aConsumer, tRow.id());
		for (PartFamilyRecipeRow tRow : compressorRecipeBuilders()) tRow.builder().save(aConsumer, tRow.id());
		for (PartFamilyRecipeRow tRow : rollBenderRecipeBuilders()) tRow.builder().save(aConsumer, tRow.id());
		for (PartFamilyRecipeRow tRow : machinePourBand()) tRow.builder().save(aConsumer, tRow.id()); // task machines-crafting-pour — the non-steam machine bodies (the band javadoc at the tail)
		for (PartFamilyRecipeRow tRow : forgeLadderBuilders()) tRow.builder().save(aConsumer, tRow.id());
	}
	//?} else {
	/*@Override
	protected void buildRecipes(net.minecraft.data.recipes.RecipeOutput aOutput) {
		sprayCanEmptyBuilder().save(aOutput, SPRAY_CAN_EMPTY_ID);
		hammerFromStoneBuilder().save(aOutput, HAMMER_STONE_ID);
		hammerFromIngotsBuilder().save(aOutput, HAMMER_INGOTS_ID);
		wrenchBuilder().save(aOutput, WRENCH_ID);
		foodCanEmptyBuilder().save(aOutput, FOOD_CAN_EMPTY_ID);
		softHammerBuilder().save(aOutput, SOFT_HAMMER_ID);
		monkeyWrenchBuilder().save(aOutput, MONKEY_WRENCH_ID);
		magnifyingGlassBuilder().save(aOutput, MAGNIFYING_GLASS_ID);
		pincersBuilder().save(aOutput, PINCERS_ID);
		for (MoldRecipeRow tRow : extruderMoldChainRows()) tRow.builder().save(aOutput, tRow.id()); // task mold-extruder-shapes — the full 64-row mold chain
		shapePressBulletCasingSmallBuilder().save(aOutput, SHAPE_PRESS_SMALL_ID); // task explosives-chain
		shapePressBulletCasingMediumBuilder().save(aOutput, SHAPE_PRESS_MEDIUM_ID);
		shapePressBulletCasingLargeBuilder().save(aOutput, SHAPE_PRESS_LARGE_ID);
		for (FoodMoldRecipeRow tRow : foodMoldRecipeBuilders()) tRow.builder().save(aOutput, tRow.id()); // task food-bake-items
		for (BakeCraftRow tRow : bakeCraftRows()) {if (tRow.shaped() != null) tRow.shaped().save(aOutput, tRow.id()); else tRow.shapeless().save(aOutput, tRow.id());} // task food-bake-recipes
		for (BakeCraftRow tRow : bottleCraftRows()) {if (tRow.shaped() != null) tRow.shaped().save(aOutput, tRow.id()); else tRow.shapeless().save(aOutput, tRow.id());} // task food-bottles-min — the independent bottles band
		for (BakeSmeltRow tRow : bakeSmeltRows()) tRow.builder().save(aOutput, tRow.id()); // task food-bake-recipes
		largeSteelCrucibleBuilder().save(aOutput, LARGE_STEEL_CRUCIBLE_ID);
		bathingPotSteelBuilder().save(aOutput, BATHING_POT_STEEL_ID);
		clayBowlForwardBuilder().save(aOutput, CLAY_BOWL_FORWARD_ID);
		clayBowlReverseBuilder().save(aOutput, CLAY_BOWL_REVERSE_ID);
		clayBowlSmeltingBuilder().save(aOutput, CLAY_BOWL_SMELT_ID);
		clayJuicerBuilder().save(aOutput, CLAY_JUICER_ID);
		clayJuicerReverseBuilder().save(aOutput, CLAY_JUICER_REVERSE_ID);
		clayJuicerSmeltingBuilder().save(aOutput, CLAY_JUICER_SMELT_ID);
		for (BakeSmeltRow tRow : clayPitSmeltRows()) tRow.builder().save(aOutput, tRow.id()); // task worldgen-diggables-pits — the 4 colored-clay hardening smelts
		anvilBuilder(GT6Anvils.STONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.STONE).save(aOutput, STONE_ANVIL_ID);
		anvilBuilder(GT6Anvils.BLACKSTONE_ANVIL.get(), net.minecraft.world.level.block.Blocks.BLACKSTONE).save(aOutput, BLACKSTONE_ANVIL_ID);
		for (gregtech6.registry.GT6Mortars.MortarRow tRow : gregtech6.registry.GT6Mortars.ROWS) { // task mortar-family
			ShapelessRecipeBuilder tMortarBuilder = mortarBuilder(tRow);
			if (tMortarBuilder == null) continue; // the row's ingredient is driver-hidden — the JSON skip semantics
			tMortarBuilder.save(aOutput, mortarRecipeId(tRow));
		}
		siftingTableBuilder().save(aOutput, SIFTING_TABLE_ID); // task sifting-table-family
		grindstoneBuilder().save(aOutput, GRINDSTONE_ID); // task grindstone-family — the :2226 row
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) {
			ShapedRecipeBuilder tBuilder = hopperRecipeBuilder(tRow);
			if (tBuilder == null) continue; // the row's material is driver-hidden — the JSON/wall skip semantics (mdh-3 wave readiness)
			tBuilder.save(aOutput, hopperRecipeId(tRow));
		}
		for (gregtech6.registry.GT6Chests.ChestRow tRow : gregtech6.registry.GT6Chests.ROWS) { // task material-mc-a-storage-chests
			ShapedRecipeBuilder tBuilder = chestRecipeBuilder(tRow);
			if (tBuilder == null) continue; // the row's material is driver-hidden — the JSON skip semantics (mdh-3 wave readiness)
			tBuilder.save(aOutput, chestRecipeId(tRow));
		}
		// task block-family-32xxx-port — the charging locker A ruling + the sap bag row
		// (the forge-leg twin above carries the full comment band)
		for (gregtech6.registry.GT6ChargingLockers.ChargingLockerRow tRow : gregtech6.registry.GT6ChargingLockers.ROWS) {
			ShapedRecipeBuilder tBuilder = chargingLockerRecipeBuilder(tRow);
			if (tBuilder == null) continue; // the A ruling — no plain locker at this material
			tBuilder.save(aOutput, chargingLockerRecipeId(tRow));
		}
		sapBagBuilder().save(aOutput, SAP_BAG_ID);
		// the plant pot row DEFERS (the 'U' Ceramic Basin item is unported — the class doc)
		progressmeterBuilder().save(aOutput, PROGRESSMETER_ID);
		circuitWireCopperBuilder().save(aOutput, CIRCUIT_WIRE_COPPER_ID); // task circuit-chain-recipes — MIT:571
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
			ShapedRecipeBuilder tBuilder = staticStorageRecipeBuilder(tRow);
			if (tBuilder == null) continue; // the row's material is driver-hidden — the hopper walk's skip semantics
			tBuilder.save(aOutput, staticStorageRecipeId(tRow));
		}
		for (PartFamilyRecipeRow tRow : partFamilyRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (PartFamilyRecipeRow tRow : tankValveRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		// task crafting-barrels-boilers — the barrel pair + the 26 boiler rows
		for (PartFamilyRecipeRow tRow : barrelRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		for (PartFamilyRecipeRow tRow : boilerRecipeBuilders()) {
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
		// task w5-t2-blade-six — the six blade-tool steel-route rows (the dig-tool row shape)
		// task blade-ladder — the three blade forms move to the per-material rows
		// (the OreProcessing_Tool rows Loader_Tools.java:321-323; the t1 steel-route
		// convergence placeholders for sword/knife/butchery retired with the ladder)
		bladeLadderRows(aOutput);
		axeBuilder().save(aOutput, AXE_ID);
		axeDoubleBuilder().save(aOutput, AXE_DOUBLE_ID);
		// task w5-t4-field-five — the five field-tool steel-route rows (the t1 shape)
		hoeBuilder().save(aOutput, HOE_ID);
		plowBuilder().save(aOutput, PLOW_ID);
		senseBuilder().save(aOutput, SENSE_ID);
		// task w5-t5-scene-six — the scene-tool rows (the same steel-route shape; the
		// universal spade/club/branch cutter/hand drill/scissors/scoop/plunger/
		// bending cylinders ride the arg-8 per-material band — the w5 anchors retired)
		flintAndTinderFromFlintAndSteelBuilder().save(aOutput, FLINT_AND_TINDER_ID);
		flintAndSteelBuilder().save(aOutput, FLINT_AND_STEEL_ID);
		rollingPinBuilder().save(aOutput, ROLLING_PIN_ID);
		// task w5-t6-electric-nineteen — the fifteen electric rows (the :356-377 convergence)
		for (ElectricToolRow tRow : electricToolRows()) {
			tRow.builder().save(aOutput, tRow.id());
		}
		pocketMultitoolBuilder().save(aOutput, POCKET_MULTITOOL_ID);
		// task w5-t8-armor-24 — the 24 hazmat rows (tail-append)
		for (GT6ArmorMaterials.SuitRow tSuit : GT6ArmorMaterials.SUITS) {
			for (int i = 0; i < GT6ArmorMaterials.PIECE_TYPES.length; i++) {
				ShapedRecipeBuilder tArmorBuilder = armorPieceBuilder(tSuit, i);
				if (tArmorBuilder == null) continue; // the suit's 'M' material is driver-hidden (HEAT = Al) — the skip semantics
				tArmorBuilder.save(aOutput, armorRecipeId(tSuit, i));
			}
		}
		// task dig-ladder — the per-material identity-stamped rows (the axis walk)
		digLadderRows(aOutput);
		machineLadderRows(aOutput);
		toolArg8Rows(aOutput);
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
			ShapedRecipeBuilder tRailBuilder = railRecipeBuilder(tRailRow);
			if (tRailBuilder == null) continue; // the row's rail material is driver-hidden — the JSON/wall skip semantics (mdh-clearout-batch2 sweep)
			tRailBuilder.save(aOutput, railRecipeId(tRailRow));
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
		// task toolhead-r11a-file-belt — the raw→finished tool-head file belt (Loader_Recipes_Handlers.java:420-434)
		for (FileBeltCraftFromRow tRow : fileBeltCraftFromDatagenRows()) {
			fileBeltCraftFromBuilder(tRow).save(aOutput, tRow.aId());
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
			ShapedRecipeBuilder tRocksBuilder = slabFromRocksBuilder(tStone);
			if (tRocksBuilder == null) continue; // the family's rock material is driver-hidden (EtFu prismarines) — the skip semantics (mdh-clearout-batch2 sweep)
			tRocksBuilder.save(aOutput, slabFromRocksId(tStone));
			slabFromCobbleBuilder(tStone).save(aOutput, slabFromCobbleId(tStone));
		}

		// task debt-emitter-sensor-generators — the 20 live self-crafting rows of the three
		// technological component families (the pour map in the band javadoc below)
		compactComponentRows(aOutput);
		// task robotics-chain — the 10 Autocrafter tip rows + the 37 live component-family
		// self-crafting rows (the pour map in the band doc below)
		robotTipRows(aOutput);
		roboticsComponentRows(aOutput);
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
		// task crafting-machines-steam-band — the steam-age bootstrap band (the forge-leg mirror)
		for (PartFamilyRecipeRow tRow : steamEngineRecipeBuilders()) tRow.builder().save(aOutput, tRow.id());
		for (PartFamilyRecipeRow tRow : burningBoxRecipeBuilders()) tRow.builder().save(aOutput, tRow.id());
		for (PartFamilyRecipeRow tRow : electricDynamoRecipeBuilders()) tRow.builder().save(aOutput, tRow.id());
		for (PartFamilyRecipeRow tRow : sifterRecipeBuilders()) tRow.builder().save(aOutput, tRow.id());
		for (PartFamilyRecipeRow tRow : compressorRecipeBuilders()) tRow.builder().save(aOutput, tRow.id());
		for (PartFamilyRecipeRow tRow : rollBenderRecipeBuilders()) tRow.builder().save(aOutput, tRow.id());
		for (PartFamilyRecipeRow tRow : machinePourBand()) tRow.builder().save(aOutput, tRow.id()); // task machines-crafting-pour — the non-steam machine bodies (the forge-leg mirror)
		for (PartFamilyRecipeRow tRow : forgeLadderBuilders()) tRow.builder().save(aOutput, tRow.id());
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
	// package-private: the GT6DatagenWalkLegTest seam (the guarded walk builders the leg pins)
	ShapedRecipeBuilder slabFromRocksBuilder(gregtech6.registry.GTStoneBlocks.StoneSpec aStone) {
		// null-drop guard: the prismarine families' rock material is EtFu PRIMARY (atlas :2085/:2086) —
		// a driver-hidden rock has no rockGt item; skip the family exactly like the hopper row face.
		if (GTMaterialItems.get(gregapi.data.OP.rockGt, aStone.material().get()) == null) return null;
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
	 * <li>Wooden Bookshelf (:181-183 "PPP","sfr","PPP"): 'P' = THE ROW'S plank item,
	 *     's' = the saw tag, 'f' = the file tag, 'r' = the soft hammer tag — the CR.java
	 *     letters verbatim ('s' = saw :356, 'f' = file :344, 'r' = softhammer :355; the
	 *     storage-static-batch landing had swapped 's'/'r' to hard hammer/screwdriver,
	 *     task storage-tool-char-decode);</li>
	 * <li>Wooden Bottlecrate (:184 "sfr","PGP","BPB"): the same 's'/'f'/'r' tool
	 *     letters, 'B' = the wood bolt, 'G' = a slime ball (the upstream itemGlue
	 *     column folds — no glue item in the port universe, the declared
	 *     deviation).</li>
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
	ShapedRecipeBuilder railRecipeBuilder(GT6Rails.RailRow aRow) {
		// null-drop guard: the rail ladder carries Al (TiC PRIMARY, atlas :2350) and TungstenCarbide
		// (ReC PRIMARY, :2412) — a hidden rail material has no railGt item; skip the row like the hopper face.
		if (GTMaterialItems.get(gregapi.data.OP.railGt, aRow.material()) == null) return null;
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
		// task material-mc-b-storage-mass-shelf — the metal shelf/crate ladders (Loader
		// :143 "PTP","sdh","PTP" plate+screw / :144 "CdC","TCT" casingSmall+screw, the
		// CR.java:342-365 tool chars s=saw d=screwdriver h=hammer), the null-drop guard
		// rides the hopperRecipeBuilder shape (a driver-hidden material has no registered
		// screw — the walk must never dereference an unregistered pair)
		if (aRow.material() != null && (aRow.kind() == gregtech6.registry.GT6StaticStorages.Kind.BOOKSHELF
				|| aRow.kind() == gregtech6.registry.GT6StaticStorages.Kind.BOTTLECRATE)) {
			if (GTMaterialItems.get(gregapi.data.OP.screw, aRow.material().mt()) == null) return null;
			if (aRow.kind() == gregtech6.registry.GT6StaticStorages.Kind.BOOKSHELF) {
				if (GTMaterialItems.get(gregapi.data.OP.plate, aRow.material().mt()) == null) return null;
				return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, resultOf(aRow))
						.pattern("PTP").pattern("sdh").pattern("PTP")
						.define('P', GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, aRow.material().mt()))
						.define('T', GTMaterialItems.get(gregapi.data.OP.screw, aRow.material().mt()).get())
						.define('s', GT6ItemTags.TOOLS_SAW)
						.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
						.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
						.unlockedBy("has_plate", has(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, aRow.material().slug())));
			}
			if (GTMaterialItems.get(gregapi.data.OP.casingSmall, aRow.material().mt()) == null) return null;
			return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, resultOf(aRow))
					.pattern("CdC").pattern("TCT")
					.define('C', GTMaterialItems.get(gregapi.data.OP.casingSmall, aRow.material().mt()).get())
					.define('T', GTMaterialItems.get(gregapi.data.OP.screw, aRow.material().mt()).get())
					.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
					.unlockedBy("has_casing_small", has(GTMaterialItems.get(gregapi.data.OP.casingSmall, aRow.material().mt()).get()));
		}
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
				.define('s', GT6ItemTags.TOOLS_SAW)
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('r', GT6ItemTags.TOOLS_SOFT_HAMMER);
		case BOTTLECRATE -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, resultOf(aRow))
				.pattern("sfr").pattern("PGP").pattern("BPB")
				.define('P', aRow.plank().item())
				.define('B', GTMaterialItems.get(gregapi.data.OP.bolt, gregapi.data.MT.Wood).get())
				.define('G', Items.SLIME_BALL)
				.define('s', GT6ItemTags.TOOLS_SAW)
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('r', GT6ItemTags.TOOLS_SOFT_HAMMER);
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
	 * The charging-locker crafting rows (task block-family-32xxx-port — the Loader :139
	 * recipe, "WCW"/"WMW"/"WCW"): 'M' = the SAME-material plain Locker (upstream
	 * {@code aRegistry.getItem(7300+aID)}), 'W' = {@code MT.DATA.CABLES_01[3]} → the
	 * 1x insulated tier-3 cable (the battery-recipe CABLES_01 column mapping), 'C' =
	 * {@code OD_CIRCUITS[3]} → the {@code #gt6:circuit3} tag. THE A RULING: the port
	 * plain Locker is the storage-static-batch two-anchor fold, so only the Bronze/Steel
	 * rows resolve 'M' — every other row returns null (the row stays UNGENERATED, the
	 * mc-A1 backfill owns the 58-row tail; no material-compensating substitute).
	 */
	// package-private: the GT6DatagenWalkLegTest seam (the hopperRecipeBuilder form)
	ShapedRecipeBuilder chargingLockerRecipeBuilder(gregtech6.registry.GT6ChargingLockers.ChargingLockerRow aRow) {
		RegistryObject<Item> tPlainLocker = plainLockerHandle(aRow.material().slug());
		//? if forge {
		if (tPlainLocker == null || !tPlainLocker.isPresent()) return null; // the A ruling (the class doc)
		//?} else {
		/*// 21.1: the Holder face — isBound() is the throw-free presence check
		if (tPlainLocker == null || !tPlainLocker.isBound()) return null; // the A ruling (the class doc)
		*///?}
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,
				gregtech6.registry.GT6ChargingLockers.ITEMS_BY_PATH.get(aRow.path()).get())
				.pattern("WCW")
				.pattern("WMW")
				.pattern("WCW")
				.define('M', tPlainLocker.get())
				.define('W', wireItem(3, 1, true)) // the CABLES_01[3] column (1x insulated cable, tier 3)
				.define('C', GT6ItemTags.gt6("circuit3")) // the OD_CIRCUITS[3] column
				.unlockedBy("has_locker", has(tPlainLocker.get()));
	}

	/**
	 * The plain-locker handle at a metalset material — the :139 'M' column. Bronze/Steel
	 * resolve through the storage-static-batch anchors (the two-anchor fold), every other
	 * metalset material has no port locker row (null — the A ruling face). The HANDLE
	 * (not the item) is the seam: the presence check null-drops before any registry
	 * dereference, so the walk stays throw-free on a cold JVM.
	 */
	// package-private: the A-ruling pin seam
	static RegistryObject<Item> plainLockerHandle(String aSlug) {
		return switch (aSlug) {
			case "bronze" -> gregtech6.registry.GT6StaticStorages.ITEMS_BY_PATH.get("locker_bronze");
			case "steel" -> gregtech6.registry.GT6StaticStorages.ITEMS_BY_PATH.get("locker_steel");
			default -> null;
		};
	}

	/**
	 * The Sap Bag crafting row (task block-family-32xxx-port — the Loader :2221 recipe,
	 * "SSS"/"LsL"/"LLL"): 'L' = {@code OD.craftingLeather} → {@code Items.LEATHER} (the
	 * static-storage recipe mapping), 'S' = {@code OD.stickAnyWood} → the vanilla stick
	 * (the rolling-pin 'S' precedent), 's' = the scissors tool letter → the
	 * {@code #gt6:tools/scissors} tag (the CS.java:1906 mapping).
	 */
	private ShapedRecipeBuilder sapBagBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,
				gregtech6.registry.GT6MiscToolBlocks.SAP_BAG_ITEM.get())
				.pattern("SSS")
				.pattern("LsL")
				.pattern("LLL")
				.define('L', Items.LEATHER)
				.define('S', Items.STICK)
				.define('s', GT6ItemTags.TOOLS_SCISSORS)
				.unlockedBy("has_leather", has(Items.LEATHER));
	}

	/**
	 * The Universal Plant Pot crafting row — NOT GENERATED (the :2229 recipe's 'U'
	 * column is {@code IL.Ceramic_Basin}, an item the port has not landed: the
	 * cup/jug/measuring-pot trio ported, the crafting basin did not). Recorded here as
	 * the reported deference seat; the builder lands with the basin item card.
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
	// package-private: the GT6DatagenWalkLegTest seam (the guarded walk builders the leg pins)
	ShapedRecipeBuilder hopperRecipeBuilder(GT6Hoppers.HopperRow aRow) {
		// null-drop guard: a driver-hidden material has no registered curved plate — skip the
		// row exactly like the JSON resolveItem bad-row face (the mdh-3 clear-out readiness,
		// review seat XVI; the walk must never dereference an unregistered pair).
		if (gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateCurved, aRow.material().mt()) == null) return null;
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GT6Hoppers.ITEMS_BY_PATH.get(aRow.path()).get())
				.pattern(aRow.queue() ? "PCP" : "PwP")
				.pattern("XCX")
				.pattern(aRow.queue() ? "wXh" : " Xh")
				// task hopper-matrix: the MATERIAL overload — the tag slug must be the
				// canonical snake of mNameInternal (GT6ItemTags.addFamilyFace), not the
				// display-snake row slug (HSLA -> plates/hslasteel, DuraniumAlloy ->
				// plates/duranium, SteelGalvanized -> plates/steel_galvanized): the display
				// slug would point at an empty tag. This is also the upstream-faithful face
				// ('P' = OP.plate.dat(aMat), Loader:145-146).
				.define('P', gregtech6.datagen.GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, aRow.material().mt()))
				.define('X', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateCurved, aRow.material().mt()).get())
				.define('C', Tags.Items.CHESTS)
				.define('w', GT6ItemTags.TOOLS_WRENCH)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_chest", has(Tags.Items.CHESTS));
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
	 * The metal-chest crafting row ids (task material-mc-a-storage-chests — the
	 * result-path convention, one per row, the HOPPER_RECIPE_IDS shape).
	 */
	public static final java.util.List<ResourceLocation> CHEST_RECIPE_IDS = gregtech6.registry.GT6Chests.ROWS.stream()
			.map(GT6CraftingRecipes::chestRecipeId)
			.collect(java.util.stream.Collectors.toList());

	/** The id of one chest row's recipe (the {@link #hopperRecipeId} join seam verbatim). */
	public static ResourceLocation chestRecipeId(gregtech6.registry.GT6Chests.ChestRow aRow) {
		String tPath = aRow.path();
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/**
	 * The metal-chest crafting rows (task material-mc-a-storage-chests — the metalset
	 * pair :132-133): the plain chest "sPw"/"RSR"/"PPP" over plate + ring + stick of the
	 * material (the 'P'/'R'/'S' columns of :132), the reinforced wooden chest
	 * "sSw"/"RCR"/"SSS" over stick + ring + any chest (the 'S'/'R'/'C' columns of :133 —
	 * OD.craftingChest = {@code Tags.Items.CHESTS}, the hopper 'C' column face). The
	 * lowercase 's'/'w' = the screwdriver/wrench in-grid tool letters. Null-drop guard:
	 * a driver-hidden material has no registered ring/stick/plate pair (the hopper
	 * mdh-3 guard verbatim, the walk must never dereference an unregistered item).
	 */
	private ShapedRecipeBuilder chestRecipeBuilder(gregtech6.registry.GT6Chests.ChestRow aRow) {
		var tRing = GTMaterialItems.get(gregapi.data.OP.ring, aRow.material().mt());
		var tStick = GTMaterialItems.get(gregapi.data.OP.stick, aRow.material().mt());
		if (tRing == null || tStick == null) return null;
		ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder
				.shaped(RecipeCategory.DECORATIONS, gregtech6.registry.GT6Chests.ITEMS_BY_PATH.get(aRow.path()).get())
				.unlockedBy("has_material_ring", has(tRing.get()));
		if (aRow.reinforced()) {
			return tBuilder
					.pattern("sSw")
					.pattern("RCR")
					.pattern("SSS")
					.define('S', tStick.get())
					.define('R', tRing.get())
					.define('C', Tags.Items.CHESTS)
					.define('s', GT6ItemTags.TOOLS_SCREWDRIVER)
					.define('w', GT6ItemTags.TOOLS_WRENCH);
		}
		return tBuilder
				.pattern("sPw")
				.pattern("RSR")
				.pattern("PPP")
				.define('P', gregtech6.datagen.GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, aRow.material().mt()))
				.define('R', tRing.get())
				.define('S', tStick.get())
				.define('s', GT6ItemTags.TOOLS_SCREWDRIVER)
				.define('w', GT6ItemTags.TOOLS_WRENCH);
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
			var tPlateHandle = GTMaterialItems.get(gregapi.data.OP.plate, USB_PLATE_MATS[i]);
			var tScrewHandle = GTMaterialItems.get(gregapi.data.OP.screw, USB_PLATE_MATS[i]);
			if (tPlateHandle == null || tScrewHandle == null) continue; // the tier's plate material (USB_PLATE_MATS[0] = Al, TiC PRIMARY :2350) is driver-hidden — the skip semantics
			Item tPlate = tPlateHandle.get();
			Item tScrew = tScrewHandle.get();
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
			var tPlateHandle = GTMaterialItems.get(gregapi.data.OP.plate, USB_PLATE_MATS[i]);
			var tScrewHandle = GTMaterialItems.get(gregapi.data.OP.screw, USB_PLATE_MATS[i]);
			if (tPlateHandle == null || tScrewHandle == null) continue; // the tier's plate material (USB_PLATE_MATS[0] = Al, TiC PRIMARY :2350) is driver-hidden — the skip semantics
			Item tPlate = tPlateHandle.get();
			Item tScrew = tScrewHandle.get();
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
	// package-private: the GT6DatagenWalkLegTest seam (the guarded walk builders the leg pins)
	java.util.List<BridgeCraftRow> usbStickRecipeRows() {
		java.util.List<BridgeCraftRow> rRows = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Item tWire = wireItemByPath(USB_WIRE_COL_PATHS[i]);
			var tPlateHandle = GTMaterialItems.get(gregapi.data.OP.plate, USB_PLATE_MATS[i]);
			var tScrewHandle = GTMaterialItems.get(gregapi.data.OP.screw, USB_PLATE_MATS[i]);
			if (tPlateHandle == null || tScrewHandle == null) continue; // the tier's plate material (USB_PLATE_MATS[0] = Al, TiC PRIMARY :2350) is driver-hidden — the skip semantics
			Item tPlate = tPlateHandle.get();
			Item tScrew = tScrewHandle.get();
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
	 * One extruder-mold crafting row — builder + save id (the FoodMoldRecipeRow form).
	 */
	private record MoldRecipeRow(ShapedRecipeBuilder builder, ResourceLocation id) {
	}

	/**
	 * One machining row of the upstream CR.shaped anchor chains (MultiItemTechnological
	 * .java:218-254 the Shape_Extruder family; the SimpleEx twins :294-330 carry the
	 * IDENTICAL stroke plan over their own family ids): the mold id suffix, the parent
	 * mold it machines down from, and the 3x3 stroke verbatim — 'x' is the CR.java:359
	 * wirecutter tool letter walking the six strike positions clockwise, 'P' the parent.
	 */
	private record MoldChainForm(String aMold, String aParent, String aRow0, String aRow1, String aRow2) {}

	/** The shared stroke plan, upstream row order verbatim (:218-254). */
	private static final List<MoldChainForm> MOLD_CHAIN_FORMS = List.of(
			new MoldChainForm("ingot", "empty", "x  ", " P ", "   "),        // :218
			new MoldChainForm("plate_tiny", "empty", " x ", " P ", "   "),   // :219
			new MoldChainForm("plate_curved", "empty", "  x", " P ", "   "), // :220
			new MoldChainForm("rod", "empty", "   ", " Px", "   "),          // :221
			new MoldChainForm("foil", "empty", "   ", " P ", "  x"),         // :222
			new MoldChainForm("ring", "empty", "   ", " P ", " x "),         // :223
			new MoldChainForm("bolt", "rod", "x  ", " P ", "   "),           // :225
			new MoldChainForm("wire", "rod", " x ", " P ", "   "),           // :226
			new MoldChainForm("rod_long", "rod", "  x", " P ", "   "),       // :227
			new MoldChainForm("wire_fine", "rod", "   ", " Px", "   "),      // :228
			new MoldChainForm("block", "ingot", "x  ", " P ", "   "),        // :230
			new MoldChainForm("pickaxe", "ingot", " x ", " P ", "   "),      // :231
			new MoldChainForm("hammer", "ingot", "  x", " P ", "   "),       // :232
			new MoldChainForm("hoe", "ingot", "   ", " Px", "   "),          // :233
			new MoldChainForm("gear", "ring", "x  ", " P ", "   "),          // :235
			new MoldChainForm("gear_small", "ring", " x ", " P ", "   "),    // :236
			new MoldChainForm("bottle", "ring", "  x", " P ", "   "),        // :237
			new MoldChainForm("cell", "ring", "   ", " Px", "   "),          // :238
			new MoldChainForm("ccc", "ring", "   ", " P ", "  x"),           // :239
			new MoldChainForm("axe", "plate_tiny", "x  ", " P ", "   "),     // :241
			new MoldChainForm("shovel", "plate_tiny", " x ", " P ", "   "),  // :242
			new MoldChainForm("file", "plate_tiny", "  x", " P ", "   "),    // :243
			new MoldChainForm("sword", "plate_tiny", "   ", " Px", "   "),   // :244
			new MoldChainForm("saw", "plate_tiny", "   ", " P ", "  x"),     // :245
			new MoldChainForm("plate", "foil", "x  ", " P ", "   "),         // :247
			new MoldChainForm("casing", "foil", " x ", " P ", "   "),        // :248
			new MoldChainForm("pipe_tiny", "plate_curved", "x  ", " P ", "   "),   // :250
			new MoldChainForm("pipe_small", "plate_curved", " x ", " P ", "   "),  // :251
			new MoldChainForm("pipe_medium", "plate_curved", "  x", " P ", "   "), // :252
			new MoldChainForm("pipe_large", "plate_curved", "   ", " Px", "   "),  // :253
			new MoldChainForm("pipe_huge", "plate_curved", "   ", " P ", "  x"));  // :254

	/**
	 * The FULL extruder-mold crafting chain (task mold-extruder-shapes) — 64 rows, both
	 * families. Supersedes the w1 flattened plate/rod rows: with the Empty molds now
	 * registered, every mold crafts from its TRUE upstream parent (the w1 javadoc's "the
	 * pooled Empty/Foil intermediates" no longer pooled). Row plan:
	 * <ul>
	 * <li>the two Empty folds (MultiItemTechnological.java:184/:260, the {@code "hf","xP"}
	 * 2x2 stroke over a double plate): 'h' = {@code #gt6:tools/hard_hammer} (the
	 * OreDictToolNames.hammer face, the foodmold row's letter mapping), 'f' =
	 * {@code #gt6:tools/file}, 'x' = {@code #gt6:tools/wire_cutter} (the CR.java:359
	 * letter — the w1 rows' file reading was a misread), and 'P' =
	 * {@code OP.plateDouble.dat(MT.TungstenCarbide)} for the Shape_Extruder Empty vs
	 * {@code OP.plateDouble.dat(ANY.Steel)} for the SimpleEx Empty — the ANY.Steel walk
	 * folds to its Steel representative (the Knightmetal/MeteoricSteel co-members are
	 * unported; the bathing-pot steel-plate precedent).</li>
	 * <li>the 31+31 machining rows from {@link #MOLD_CHAIN_FORMS} keyed on the EXACT
	 * parent mold item — the chain root is always the family's own Empty (multi-step:
	 * e.g. Plate ← Foil ← Empty, the upstream :247/:222 depth preserved).</li>
	 * </ul>
	 * The in-grid tools ride the vanilla container-item channel (the foodmold row's
	 * tool-mark form); CR.DEF_REV has no vanilla-datagen face (the reversed-grid freedom
	 * is inherent to vanilla shaped matching).
	 */
	private List<MoldRecipeRow> extruderMoldChainRows() {
		java.util.Map<String, net.minecraft.world.item.Item> tMolds = new java.util.LinkedHashMap<>();
		for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tMold : gregtech6.registry.GT6ExtruderMolds.MOLDS) {
			tMolds.put(tMold.getId().getPath(), tMold.get());
		}
		net.minecraft.world.item.Item tTcPlateDouble = GTMaterialItems.get(gregapi.data.OP.plateDouble, gregapi.data.MT.TungstenCarbide).get();
		net.minecraft.world.item.Item tSteelPlateDouble = GTMaterialItems.get(gregapi.data.OP.plateDouble, gregapi.data.MT.Steel).get();
		List<MoldRecipeRow> rRows = new ArrayList<>();
		for (String tFamily : new String[] {"shape_extruder", "shape_simple_ex"}) {
			net.minecraft.world.item.Item tPlateDouble = tFamily.equals("shape_extruder") ? tTcPlateDouble : tSteelPlateDouble;
			String tEmptyId = tFamily + "_empty";
			Item tEmpty = tMolds.get(tEmptyId);
			rRows.add(new MoldRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tEmpty)
					.pattern("hf")
					.pattern("xP")
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
					.define('f', GT6ItemTags.TOOLS_FILE)
					.define('x', GT6ItemTags.TOOLS_WIRE_CUTTER)
					.define('P', tPlateDouble)
					.unlockedBy("has_plate_double", has(tPlateDouble)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, tEmptyId)));
			for (MoldChainForm tForm : MOLD_CHAIN_FORMS) {
				String tMoldId = tFamily + "_" + tForm.aMold();
				Item tParent = tMolds.get(tFamily + "_" + tForm.aParent());
				rRows.add(new MoldRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tMolds.get(tMoldId))
						.pattern(tForm.aRow0())
						.pattern(tForm.aRow1())
						.pattern(tForm.aRow2())
						.define('x', GT6ItemTags.TOOLS_WIRE_CUTTER)
						.define('P', tParent)
						.unlockedBy("has_" + tForm.aParent(), has(tParent)),
						new ResourceLocation(GT6DataGenerators.MOD_ID, tMoldId)));
			}
		}
		return rRows;
	}

	/**
	 * The bullet-casing mold crafting rows (task explosives-chain) — the upstream stroke
	 * VERBATIM: {@code "TPT", "dyh", "SPS"} (MultiItemTechnological.java:356-358, Small/
	 * Medium/Large differing only in the 'P' plate tier). Letters per CR.java:342/:346/:360:
	 * 'd' = {@code #gt6:tools/screwdriver}, 'h' = {@code #gt6:tools/hard_hammer}, 'y' =
	 * chisel — the port has no {@code tools/chisel} tag at baseline, so 'y' rides the
	 * {@code gt6:chisel} item directly (the declared substitution face). 'T'/'S'/'P' = the
	 * MT.Steel representative of the upstream ANY.Steel group (screw/stick and the
	 * plateDouble/plateTriple/plateQuadruple tier) — the vanilla JSON cannot walk the
	 * material group, the w1 single-ingredient flattening convention. Result 1x mold —
	 * the RM.Press bullet-casing rows' shaping tool (Loader_Recipes_Other.java:657-668,
	 * the rows pooled with the bullet card).
	 */
	private ShapedRecipeBuilder shapePressBulletCasingSmallBuilder() {
		return shapePressBulletCasingBuilder(GT6PressMolds.SHAPE_PRESS_BULLET_CASING_SMALL.get(),
				gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateDouble, MT.Steel).get());
	}

	private ShapedRecipeBuilder shapePressBulletCasingMediumBuilder() {
		return shapePressBulletCasingBuilder(GT6PressMolds.SHAPE_PRESS_BULLET_CASING_MEDIUM.get(),
				gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateTriple, MT.Steel).get());
	}

	private ShapedRecipeBuilder shapePressBulletCasingLargeBuilder() {
		return shapePressBulletCasingBuilder(GT6PressMolds.SHAPE_PRESS_BULLET_CASING_LARGE.get(),
				gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plateQuadruple, MT.Steel).get());
	}

	/** The shared :356-358 stroke — the plate tier is the only per-mold leg. */
	private ShapedRecipeBuilder shapePressBulletCasingBuilder(net.minecraft.world.item.Item aMold, net.minecraft.world.item.Item aPlateTier) {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aMold)
				.pattern("TPT")
				.pattern("dyh")
				.pattern("SPS")
				.define('T', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.screw, MT.Steel).get())
				.define('P', aPlateTier)
				.define('S', gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.stick, MT.Steel).get())
				.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
				.define('y', GT6Tools.CHISEL.get())
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_steel_screw", has(gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.screw, MT.Steel).get()));
	}

	/**
	 * One food-mold crafting row — builder + save id (the CrucibleLadderRecipeRow form:
	 * one builder walk, one save line per leg).
	 */
	private record FoodMoldRecipeRow(ShapedRecipeBuilder builder, ResourceLocation id) {
	}

	/**
	 * The six food-mold crafting rows (task food-bake-items) — the upstream mold section
	 * VERBATIM (MultiItemTechnological.java:336 the empty mold + :344-348 the five named
	 * molds): the Empty Food Grade Mold folds from a StainlessSteel double plate under the
	 * {@code "hf"/"xP"} tool strokes ('h' = {@code #gt6:tools/hard_hammer},
	 * 'f' = {@code #gt6:tools/file}, 'x' = {@code #gt6:tools/wire_cutter} — the CR.java:359
	 * craftingToolWirecutter letter; {@code 'P'} = {@code OP.plateDouble.dat(MT.StainlessSteel)}
	 * → the GTMaterialItems plate_double item), and each named mold re-strikes the hammer
	 * mark over the EMPTY mold ({@code 'P'} = {@code IL.Shape_Foodmold_Empty} exact item,
	 * :344-348). The upstream MACHINE rows over these molds (the RM.Press/RollingMill/
	 * Mixer dough faces) stay the T3b card's surface. The in-grid tools ride the vanilla
	 * container-item channel (the pincers row's tool-mark form).
	 */
	private List<FoodMoldRecipeRow> foodMoldRecipeBuilders() {
		Item tEmptyMold = GT6BakeFoods.MOLDS.get(0).get();
		Item tSteelPlateDouble = GTMaterialItems.get(gregapi.data.OP.plateDouble, MT.StainlessSteel).get();
		List<FoodMoldRecipeRow> rRows = new ArrayList<>();
		rRows.add(new FoodMoldRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tEmptyMold)
				.pattern("hf")
				.pattern("xP")
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('x', GT6ItemTags.TOOLS_WIRE_CUTTER)
				.define('P', tSteelPlateDouble)
				.unlockedBy("has_steel_plate_double", has(tSteelPlateDouble)),
				new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_foodmold_empty")));
		// the five named molds — {save-id suffix, the 3x3 stroke verbatim}; MOLDS order is
		// empty, bun, bread, baguette, cylinder, toast so the strike walk maps index + 1
		String[][] tStrikes = {
				{"bun", "h  ", " P ", "   "},
				{"bread", " h ", " P ", "   "},
				{"baguette", "  h", " P ", "   "},
				{"cylinder", "   ", " Ph", "   "},
				{"toast", "   ", " P ", "  h"}};
		for (int i = 0; i < tStrikes.length; i++) {
			Item tMold = GT6BakeFoods.MOLDS.get(i + 1).get();
			rRows.add(new FoodMoldRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tMold)
					.pattern(tStrikes[i][1])
					.pattern(tStrikes[i][2])
					.pattern(tStrikes[i][3])
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
					.define('P', tEmptyMold)
					.unlockedBy("has_empty_food_mold", has(tEmptyMold)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_foodmold_" + tStrikes[i][0])));
		}
		return rRows;
	}

	// -------------------------------------------------------------------------
	// the food-bake band (task food-bake-recipes)
	// -------------------------------------------------------------------------

	/**
	 * One bake crafting row — builder + save id (the FoodMoldRecipeRow form). The builder
	 * is the SHAPED or the SHAPELESS flavor; both carry the same {@code save(consumer, id)}
	 * face, so the walk is one record with a nullable half per flavor.
	 */
	private record BakeCraftRow(ShapedRecipeBuilder shaped, ShapelessRecipeBuilder shapeless, ResourceLocation id) {
		static BakeCraftRow of(ShapedRecipeBuilder aBuilder, ResourceLocation aId) {return new BakeCraftRow(aBuilder, null, aId);}
		static BakeCraftRow of(ShapelessRecipeBuilder aBuilder, ResourceLocation aId) {return new BakeCraftRow(null, aBuilder, aId);}
	}

	/** One bake smelt row (the clayBowlSmeltingBuilder face: the RM.add_smelting family). */
	private record BakeSmeltRow(SimpleCookingRecipeBuilder builder, ResourceLocation id) {
	}

	/**
	 * The bake crafting rows (task food-bake-recipes + the food-crafting-tail tail) — the
	 * MultiItemFood.java:341-:785 inline CR band VERBATIM over the port universe (every
	 * number/count is the upstream literal; the javadoc line anchors carry the upstream
	 * line). The 'k' tool letter rides {@link GT6ItemTags#TOOLS_KNIFE} and the rolling pin
	 * {@link GT6ItemTags#TOOLS_ROLLING_PIN} (the clay-juicer "kCR" tag-define precedent —
	 * the gregOLantern fold predates the tags).
	 * DECLARED SKIPPED here (the GT6RecipesBake SKIPPED_UPSTREAM ledger carries them): the
	 * ketchup ladder :643-647 + the :636 heavy-cream cake + :637 delate (no removal channel)
	 * + the T5 veggie/ananas-slice legs (:664/:670/:697/:698/:733/:734/:763/:764). The :359
	 * fries-pack row is IN (the plateDouble-Paper pair resolves in the live flood — the
	 * Boxinator twin :360 pours in the runtime band).
	 *
	 * <p>The food-crafting-tail tail (task food-crafting-tail, 18 rows): the kX 补遗
	 * (:492 cheese, :504 the "foodBoiledegg" pair split one-row-per-boiled-egg :497/:498,
	 * :537/:538 the ham pairs), the crafting slice pairs (:685/:686 bun, :724/:725 bread,
	 * :754/:755 baguette — the T3b band poured only the :687/:726/:756 packunpack machine
	 * pairs) and the legs unlocked by the T4a items (:709/:710 the chum burgers,
	 * :737/:738/:767/:768 the bacon sandwiches, :799 the chum-on-stick).
	 */
	private List<BakeCraftRow> bakeCraftRows() {
		Item tCheeseSliced = foodItem(1); // Food_Cheese_Sliced :491
		Item tDough = bakeItem(GT6RecipesBakeIds.DOUGH);
		List<BakeCraftRow> rRows = new ArrayList<>();

		// the kX slicing family — :601/:609/:617/:628 (cookie doughs ×4), :683/:722/:752/:783 (the loaf splits)
		Object[][] tSlices = {
				{bakeItem(GT6RecipesBakeIds.COOKIE_RAW), 4, bakeItem(GT6RecipesBakeIds.DOUGH_CHOCOLATE), "slice_cookie_raw"},
				{bakeItem(GT6RecipesBakeIds.COOKIE_RAISINS_RAW), 4, bakeItem(GT6RecipesBakeIds.DOUGH_SUGAR_RAISINS), "slice_cookie_raisins_raw"},
				{bakeItem(GT6RecipesBakeIds.COOKIE_CHOCO_RAISINS_RAW), 4, bakeItem(GT6RecipesBakeIds.DOUGH_SUGAR_CHOCO_RAISINS), "slice_cookie_choco_raisins_raw"},
				{bakeItem(GT6RecipesBakeIds.COOKIE_ABYSSAL_RAW), 4, bakeItem(GT6RecipesBakeIds.DOUGH_ABYSSAL), "slice_cookie_abyssal_raw"},
				{bakeItem(GT6RecipesBakeIds.BUN_SLICED), 2, bakeItem(GT6RecipesBakeIds.BUN), "slice_bun"},
				{bakeItem(GT6RecipesBakeIds.BREAD_SLICED), 2, Items.BREAD, "slice_bread"},
				{bakeItem(GT6RecipesBakeIds.BAGUETTE_SLICED), 2, bakeItem(GT6RecipesBakeIds.BAGUETTE), "slice_baguette"},
				{bakeItem(GT6RecipesBakeIds.TOAST_SLICED), 8, bakeItem(GT6RecipesBakeIds.TOAST), "slice_toast"}};
		for (Object[] tSlice : tSlices) {
			rRows.add(BakeCraftRow.of(shapedBake((Item) tSlice[0], (int) tSlice[1], (Item) tSlice[2]), bakeId((String) tSlice[3])));
		}
		// the kX 补遗 (task food-crafting-tail) — :492 cheese (the "foodCheese" oredict member
		// is the lone GT6 cheese :490), :537/:538 the ham pairs ("foodHamraw"/"foodHamcooked",
		// :529/:530 → :534/:535); :504 slices the "foodBoiledegg" oredict (:497/:498 members)
		// whose port universe has exactly the two boiled eggs — one row each
		Object[][] tFoodSlices = {
				{foodItem(1), 4, foodItem(0), "slice_cheese"},
				{foodItem(7), 4, foodItem(3), "slice_egg_brown"},
				{foodItem(7), 4, foodItem(4), "slice_egg_white"},
				{foodItem(12), 4, foodItem(10), "slice_ham_raw"},
				{foodItem(13), 4, foodItem(11), "slice_ham_cooked"}};
		for (Object[] tSlice : tFoodSlices) {
			rRows.add(BakeCraftRow.of(shapedBake((Item) tSlice[0], (int) tSlice[1], (Item) tSlice[2]), bakeId((String) tSlice[3])));
		}
		// :358 — the fries knife row ("k"/"X" vertical, cropPotato = the vanilla potato)
		rRows.add(BakeCraftRow.of(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.FRIES_RAW))
						.pattern("k").pattern("X")
						.define('k', GT6ItemTags.TOOLS_KNIFE)
						.define('X', Items.POTATO)
						.unlockedBy("has_potato", has(Items.POTATO)),
				bakeId("fries_raw")));

		// :642 — the rolling-pin row (foodDough + rollingpin → Flattened Dough)
		rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.DOUGH_FLAT))
						.requires(tDough)
						.requires(GT6ItemTags.TOOLS_ROLLING_PIN)
						.unlockedBy("has_dough", has(tDough)),
				bakeId("dough_flat_rolling")));

		// :635 — the raw cake bottom (foodSugarDough ×4 — the oredict face is the three-member union)
		rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.CAKEBOTTOM_RAW))
						.requires(net.minecraft.world.item.crafting.Ingredient.of(
								bakeItem(GT6RecipesBakeIds.DOUGH_SUGAR), bakeItem(GT6RecipesBakeIds.DOUGH_SUGAR_RAISINS),
								bakeItem(GT6RecipesBakeIds.DOUGH_SUGAR_CHOCO_RAISINS)), 4)
						.unlockedBy("has_sugar_dough", has(bakeItem(GT6RecipesBakeIds.DOUGH_SUGAR))),
				bakeId("cakebottom_raw")));

		// :684/:723/:753/:784 — the dough → raw loaf shapeless ladder (×1/×2/×3/×4 foodDough)
		Object[][] tLoaves = {
				{GT6RecipesBakeIds.BUN_RAW, 1, "bun_raw"},
				{GT6RecipesBakeIds.BREAD_RAW, 2, "bread_raw"},
				{GT6RecipesBakeIds.BAGUETTE_RAW, 3, "baguette_raw"},
				{GT6RecipesBakeIds.TOAST_RAW, 4, "toast_raw"}};
		for (Object[] tLoaf : tLoaves) {
			rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem((int) tLoaf[0]))
							.requires(tDough, (int) tLoaf[1])
							.unlockedBy("has_dough", has(tDough)),
					bakeId((String) tLoaf[2])));
		}

		// :685/:686 + :724/:725 + :754/:755 — the crafting slice pairs (task
		// food-crafting-tail): the pack direction 2×sliced → pre-sliced and the unpack
		// direction back (the T3b band poured only the :687/:726/:756 packunpack machine
		// pairs — this is the CR.shapeless twin face, both directions)
		Object[][] tPairs = {
				{GT6RecipesBakeIds.BUN_SLICED, GT6RecipesBakeIds.BUNS_SLICED, "bun_pair", "bun_unpack"},
				{GT6RecipesBakeIds.BREAD_SLICED, GT6RecipesBakeIds.BREADS_SLICED, "bread_pair", "bread_unpack"},
				{GT6RecipesBakeIds.BAGUETTE_SLICED, GT6RecipesBakeIds.BAGUETTES_SLICED, "baguette_pair", "baguette_unpack"}};
		for (Object[] tPair : tPairs) {
			Item tSliced = bakeItem((int) tPair[0]), tPre = bakeItem((int) tPair[1]);
			rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tPre)
							.requires(tSliced, 2)
							.unlockedBy("has_input", has(tSliced)),
					bakeId((String) tPair[2])));
			rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tSliced, 2)
							.requires(tPre)
							.unlockedBy("has_input", has(tPre)),
					bakeId((String) tPair[3])));
		}

		// :359 — the fries pack (plateDouble Paper + Fries → Fries_Packaged; the pair resolves
		// in the live flood as gt6:plate_double_paper — the earlier dormancy call was a misread)
		rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.FRIES_PACKAGED))
						.requires(materialItemOrThrow(gregapi.data.OP.plateDouble, MT.Paper))
						.requires(bakeItem(GT6RecipesBakeIds.FRIES))
						.unlockedBy("has_fries", has(bakeItem(GT6RecipesBakeIds.FRIES))),
				bakeId("fries_packaged")));

		// :343/:349 — the potato-on-stick pair (ANY.Wood stick union + cropPotato / vanilla baked potato)
		rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.POTATO_ON_STICK))
						.requires(woodStickIngredient())
						.requires(Items.POTATO)
						.unlockedBy("has_potato", has(Items.POTATO)),
				bakeId("potato_on_stick")));
		rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.POTATO_ON_STICK_ROASTED))
						.requires(woodStickIngredient())
						.requires(Items.BAKED_POTATO)
						.unlockedBy("has_baked_potato", has(Items.BAKED_POTATO)),
				bakeId("potato_on_stick_roasted")));

		// :799 — the chum-on-a-stick (the ANY.Wood stick union + "foodChum" :594; the upstream
		// DEF_NCC face rides the same vanilla shapeless JSON — the loaf-split precedent)
		Item tChum = foodItem(31); // Food_Chum :594
		rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, foodItem(32)) // Food_Chum_On_Stick :798
						.requires(woodStickIngredient())
						.requires(tChum)
						.unlockedBy("has_chum", has(tChum)),
				bakeId("chum_on_stick")));

		// :652/:658 + the T5 legs — the leg-complete pizza rows PLUS the :664 veggie /
		// :670 ananas legs (task pool-drain-food-t5-tail; the four crop slices resolved
		// live off GT6CropFoods, the ham slice / cheese slice off GT6Foods)
		rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.PIZZA_CHEESE_RAW))
						.requires(bakeItem(GT6RecipesBakeIds.DOUGH_FLAT_KETCHUP))
						.requires(tCheeseSliced, 3)
						.unlockedBy("has_flat_ketchup", has(bakeItem(GT6RecipesBakeIds.DOUGH_FLAT_KETCHUP))),
				bakeId("pizza_cheese_raw")));
		rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.PIZZA_MEAT_RAW))
						.requires(bakeItem(GT6RecipesBakeIds.DOUGH_FLAT_KETCHUP))
						.requires(materialItemOrThrow(gregapi.data.OP.dust, MT.MeatCooked))
						.unlockedBy("has_flat_ketchup", has(bakeItem(GT6RecipesBakeIds.DOUGH_FLAT_KETCHUP))),
				bakeId("pizza_meat_raw")));
		// :664 — Ketchup Flat Dough + Cucumber/Tomato/Onion Sliced → Raw Veggie Pizza
		rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.PIZZA_VEGGIE_RAW))
						.requires(bakeItem(GT6RecipesBakeIds.DOUGH_FLAT_KETCHUP))
						.requires(cropFoodItem(8)) // GT6CropFoods index 8 = food_cucumber_sliced (:296)
						.requires(cropFoodItem(3)) // index 3 = food_tomato_sliced (:279)
						.requires(cropFoodItem(6)) // index 6 = food_onion_sliced (:290)
						.unlockedBy("has_flat_ketchup", has(bakeItem(GT6RecipesBakeIds.DOUGH_FLAT_KETCHUP))),
				bakeId("pizza_veggie_raw")));
		// :670 — Ketchup Flat Dough + 2 Ananas Sliced + Cooked Ham Slice + Cheese Slice → Raw Pizza Hawaii
		rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.PIZZA_ANANAS_RAW))
						.requires(bakeItem(GT6RecipesBakeIds.DOUGH_FLAT_KETCHUP))
						.requires(cropFoodItem(46), 2) // GT6CropFoods index 46 = food_ananas_sliced (:476)
						.requires(foodItem(13)) // GT6Foods index 13 = food_ham_slice_cooked (:535, meta 1103)
						.requires(tCheeseSliced)
						.unlockedBy("has_flat_ketchup", has(bakeItem(GT6RecipesBakeIds.DOUGH_FLAT_KETCHUP))),
				bakeId("pizza_ananas_raw")));

		// :699-:710 — the burger assembly; the leg-complete rows + the :709/:710 chum legs
		// (task food-crafting-tail) + the :697/:698 veggie legs (task pool-drain-food-t5-tail)
		burgerRows(rRows, tCheeseSliced);

		// :735-:770 — the sandwich assembly; the leg-complete rows + the :737/:738/:767/:768
		// bacon legs (task food-crafting-tail) + the :733/:734/:763/:764 veggie legs
		// (task pool-drain-food-t5-tail)
		sandwichRows(rRows, tCheeseSliced);
		return rRows;
	}

	/**
	 * The :699-:710 burger rows — the buns/bun-pair shapes over the material-ingot, cheese-slice and
	 * chum legs (the "foodChum" oredict member is the lone GT6 chum :594), plus the :697/:698 veggie
	 * legs over the GT6CropFoods slice trio (task pool-drain-food-t5-tail).
	 */
	private void burgerRows(List<BakeCraftRow> aRows, Item aCheeseSliced) {
		Object[][] tBurgers = {
				{GT6RecipesBakeIds.BURGER_CHEESE, null, aCheeseSliced, 3, "burger_cheese_buns", "burger_cheese_pair"},
				{GT6RecipesBakeIds.BURGER_CHUM, null, foodItem(31), 1, "burger_chum_buns", "burger_chum_pair"}, // :709/:710
				{GT6RecipesBakeIds.BURGER_MEAT, gregapi.data.OP.ingot, MT.MeatCooked, 1, "burger_meat_buns", "burger_meat_pair"},
				{GT6RecipesBakeIds.BURGER_TOFU, gregapi.data.OP.ingot, MT.Tofu, 1, "burger_tofu_buns", "burger_tofu_pair"},
				{GT6RecipesBakeIds.BURGER_SOYLENT, gregapi.data.OP.ingot, MT.SoylentGreen, 1, "burger_soylent_buns", "burger_soylent_pair"},
				{GT6RecipesBakeIds.BURGER_FISH, gregapi.data.OP.ingot, MT.FishCooked, 1, "burger_fish_buns", "burger_fish_pair"}};
		for (Object[] tBurger : tBurgers) {
			Item tFilling = tBurger[1] == null ? (Item) tBurger[2] : materialItemOrThrow((OreDictPrefix) tBurger[1], (OreDictMaterial) tBurger[2]);
			int tFillingCount = (int) tBurger[3];
			aRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem((int) tBurger[0]))
							.requires(bakeItem(GT6RecipesBakeIds.BUNS_SLICED))
							.requires(tFilling, tFillingCount)
							.unlockedBy("has_bun_sliced", has(bakeItem(GT6RecipesBakeIds.BUN_SLICED))),
					bakeId((String) tBurger[4])));
			aRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem((int) tBurger[0]))
							.requires(bakeItem(GT6RecipesBakeIds.BUN_SLICED), 2)
							.requires(tFilling, tFillingCount)
							.unlockedBy("has_bun_sliced", has(bakeItem(GT6RecipesBakeIds.BUN_SLICED))),
					bakeId((String) tBurger[5])));
		}
		// :697 — Buns_Sliced + Cucumber/Tomato/Onion Sliced → Veggie Burger
		Item tCucumber = cropFoodItem(8), tTomato = cropFoodItem(3), tOnion = cropFoodItem(6);
		aRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.BURGER_VEGGIE))
						.requires(bakeItem(GT6RecipesBakeIds.BUNS_SLICED))
						.requires(tCucumber)
						.requires(tTomato)
						.requires(tOnion)
						.unlockedBy("has_bun_sliced", has(bakeItem(GT6RecipesBakeIds.BUN_SLICED))),
				bakeId("burger_veggie_buns")));
		// :698 — Bun_Sliced ×2 + the slice trio → Veggie Burger
		aRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.BURGER_VEGGIE))
						.requires(bakeItem(GT6RecipesBakeIds.BUN_SLICED), 2)
						.requires(tCucumber)
						.requires(tTomato)
						.requires(tOnion)
						.unlockedBy("has_bun_sliced", has(bakeItem(GT6RecipesBakeIds.BUN_SLICED))),
				bakeId("burger_veggie_pair")));
	}

	/**
	 * The :733-:740/:763-:770 sandwich rows — the cheese, steak and bacon legs (the
	 * "foodBaconcooked" oredict member is the lone GT6 grilled bacon :542) plus the
	 * :733/:734 and :763/:764 veggie legs (task pool-drain-food-t5-tail; the cheese
	 * slice counts ride the upstream 5/7 literals — the pre-card 6/8 was the
	 * bake_sandwich_cheese_off_by_one bug, closed here).
	 */
	private void sandwichRows(List<BakeCraftRow> aRows, Item aCheeseSliced) {
		Object[][] tSandwiches = {
				{GT6RecipesBakeIds.SANDWICH_CHEESE, GT6RecipesBakeIds.BREADS_SLICED, GT6RecipesBakeIds.BREAD_SLICED, aCheeseSliced, 5, "sandwich_cheese_breads", "sandwich_cheese_pair"}, // :735/:736 — 5 slices
				{GT6RecipesBakeIds.SANDWICH_BACON, GT6RecipesBakeIds.BREADS_SLICED, GT6RecipesBakeIds.BREAD_SLICED, foodItem(15), 3, "sandwich_bacon_breads", "sandwich_bacon_pair"}, // :737/:738
				{GT6RecipesBakeIds.SANDWICH_STEAK, GT6RecipesBakeIds.BREADS_SLICED, GT6RecipesBakeIds.BREAD_SLICED, Items.COOKED_BEEF, 1, "sandwich_steak_breads", "sandwich_steak_pair"},
				{GT6RecipesBakeIds.LARGE_SANDWICH_CHEESE, GT6RecipesBakeIds.BAGUETTES_SLICED, GT6RecipesBakeIds.BAGUETTE_SLICED, aCheeseSliced, 7, "large_sandwich_cheese_baguettes", "large_sandwich_cheese_pair"}, // :765/:766 — 7 slices
				{GT6RecipesBakeIds.LARGE_SANDWICH_BACON, GT6RecipesBakeIds.BAGUETTES_SLICED, GT6RecipesBakeIds.BAGUETTE_SLICED, foodItem(15), 6, "large_sandwich_bacon_baguettes", "large_sandwich_bacon_pair"}, // :767/:768
				{GT6RecipesBakeIds.LARGE_SANDWICH_STEAK, GT6RecipesBakeIds.BAGUETTES_SLICED, GT6RecipesBakeIds.BAGUETTE_SLICED, Items.COOKED_BEEF, 2, "large_sandwich_steak_baguettes", "large_sandwich_steak_pair"}};
		for (Object[] tSandwich : tSandwiches) {
			aRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem((int) tSandwich[0]))
							.requires(bakeItem((int) tSandwich[1]))
							.requires((Item) tSandwich[3], (int) tSandwich[4])
							.unlockedBy("has_sliced_bread", has(bakeItem((int) tSandwich[2]))),
					bakeId((String) tSandwich[5])));
			aRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem((int) tSandwich[0]))
							.requires(bakeItem((int) tSandwich[2]), 2)
							.requires((Item) tSandwich[3], (int) tSandwich[4])
							.unlockedBy("has_sliced_bread", has(bakeItem((int) tSandwich[2]))),
					bakeId((String) tSandwich[6])));
		}
		// the veggie legs — Breads/Baguettes_Sliced (or the sliced pair ×2) + the Cucumber
		// ×2/×3 + Tomato ×2/×3 + Onion ×1 ladder (:733/:734 and :763/:764)
		Item tCucumber = cropFoodItem(8), tTomato = cropFoodItem(3), tOnion = cropFoodItem(6);
		aRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.SANDWICH_VEGGIE))
						.requires(bakeItem(GT6RecipesBakeIds.BREADS_SLICED))
						.requires(tCucumber, 2).requires(tTomato, 2).requires(tOnion)
						.unlockedBy("has_sliced_bread", has(bakeItem(GT6RecipesBakeIds.BREAD_SLICED))),
				bakeId("sandwich_veggie_breads")));
		aRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.SANDWICH_VEGGIE))
						.requires(bakeItem(GT6RecipesBakeIds.BREAD_SLICED), 2)
						.requires(tCucumber, 2).requires(tTomato, 2).requires(tOnion)
						.unlockedBy("has_sliced_bread", has(bakeItem(GT6RecipesBakeIds.BREAD_SLICED))),
				bakeId("sandwich_veggie_pair")));
		aRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.LARGE_SANDWICH_VEGGIE))
						.requires(bakeItem(GT6RecipesBakeIds.BAGUETTES_SLICED))
						.requires(tCucumber, 3).requires(tTomato, 3).requires(tOnion)
						.unlockedBy("has_sliced_bread", has(bakeItem(GT6RecipesBakeIds.BAGUETTE_SLICED))),
				bakeId("large_sandwich_veggie_baguettes")));
		aRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bakeItem(GT6RecipesBakeIds.LARGE_SANDWICH_VEGGIE))
						.requires(bakeItem(GT6RecipesBakeIds.BAGUETTE_SLICED), 2)
						.requires(tCucumber, 3).requires(tTomato, 3).requires(tOnion)
						.unlockedBy("has_sliced_bread", has(bakeItem(GT6RecipesBakeIds.BAGUETTE_SLICED))),
				bakeId("large_sandwich_veggie_pair")));
	}

	// -------------------------------------------------------------------------
	// the bottles band (task food-bottles-min) — an INDEPENDENT band, kept textually
	// separate from the food-crafting-tail card's bakeCraftRows() appends (the merge seam)
	// -------------------------------------------------------------------------

	/**
	 * The bottle-unlocked crafting tail (task food-bottles-min) — the MultiItemFood.java
	 * rows whose carrier items are the GT6-owned bottles (MultiItemBottles), VERBATIM over
	 * the port universe. The research.food-crafting-tail row-by-row list, each row with its
	 * upstream line anchor and its oredict → port-item mapping:
	 * <ul>
	 * <li>:643 — ketchup ladder rung 1: {@code foodKetchup} + 1 Food_Dough_Flat → 1
	 *     Food_Dough_Flat_Ketchup ({@code gt6:food_ketchup} + bake DOUGH_FLAT →
	 *     DOUGH_FLAT_KETCHUP)</li>
	 * <li>:644 — rung 2 (×2 outputs), :645 — rung 3 (×3), :646 — rung 4 (×4), :647 — rung
	 *     5 (×5): one ketchup bottle spreads over 1-5 flat doughs</li>
	 * <li>:636 — the vanilla cake re-craft: {@code CR.shaped(Items.cake, "C","Z", Z=
	 *     Food_CakeBottom, C="foodHeavycream")} — the foodHeavycream oredict name is the
	 *     RE-REGISTRATION of bottleCream (LoaderOreDictReRegistrations.java:870), so the
	 *     port item is {@code gt6:food_heavycream}; the cake bottom is the T3 bake item</li>
	 * <li>:550 — the BBQ ribs: {@code foodRibcooked} + {@code foodBarbecuesauce} →
	 *     Food_Rib_BBQ ({@code gt6:food_rib_cooked} + {@code gt6:food_barbecuesauce} →
	 *     {@code gt6:food_rib_bbq})</li>
	 * </ul>
	 *
	 * <p>SEVEN rows total: the research ledger's row-by-row list enumerates exactly these
	 * seven (its "8 行" header is a counting slip — the ledger's tiebreak clause rules the
	 * per-row list authoritative). The :637 cake delate row is the removal channel (the
	 * decision card, not this band). Landing these closes the pizza dead end: the ketchup
	 * ladder is the ONLY producer of food_dough_flat_ketchup, which the already-landed
	 * bake_pizza_cheese_raw/meat_raw rows consume. The machine filling rows (fluid + empty
	 * bottle → filled bottle) stay deferred with the capability-container decision card
	 * (the declared small-case-b deviation, GT6Bottles javadoc).
	 */
	private List<BakeCraftRow> bottleCraftRows() {
		Item tFlat = bakeItem(GT6RecipesBakeIds.DOUGH_FLAT);
		Item tFlatKetchup = bakeItem(GT6RecipesBakeIds.DOUGH_FLAT_KETCHUP);
		Item tKetchup = gregtech6.registry.GT6Bottles.FOOD_KETCHUP.get();
		List<BakeCraftRow> rRows = new ArrayList<>();

		// :643-:647 — the ketchup ladder (1 bottle + N flat doughs → N sauced flat doughs)
		for (int i = 1; i <= 5; i++) {
			rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tFlatKetchup, i)
							.requires(tKetchup)
							.requires(tFlat, i)
							.unlockedBy("has_flat_dough", has(tFlat)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, "bake_ketchup_flat_" + i)));
		}

		// :636 — the vanilla cake re-craft ("C"/"Z" over the cake bottom + the heavy-cream
		// bottle; CR.DEF_NCC. The :637 delate of the vanilla cake recipe is the removal
		// channel — the decision card, not here.)
		rRows.add(BakeCraftRow.of(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.CAKE)
						.pattern("C")
						.pattern("Z")
						.define('C', gregtech6.registry.GT6Bottles.FOOD_HEAVYCREAM.get())
						.define('Z', bakeItem(GT6RecipesBakeIds.CAKEBOTTOM))
						.unlockedBy("has_cake_bottom", has(bakeItem(GT6RecipesBakeIds.CAKEBOTTOM))),
				new ResourceLocation(GT6DataGenerators.MOD_ID, "bake_cake_heavycream")));

		// :550 — the BBQ ribs (CR.DEF_NCC)
		rRows.add(BakeCraftRow.of(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,
						gregtech6.registry.GT6Foods.FOODS.get(18).get()) // FOOD_ROWS index 18 = food_rib_bbq (MultiItemFood.java:548)
						.requires(gregtech6.registry.GT6Foods.FOODS.get(17).get()) // index 17 = food_rib_cooked (:547)
						.requires(gregtech6.registry.GT6Bottles.FOOD_BARBECUESAUCE.get())
						.unlockedBy("has_rib_cooked", has(gregtech6.registry.GT6Foods.FOODS.get(17).get())),
				new ResourceLocation(GT6DataGenerators.MOD_ID, "bake_rib_bbq")));
		return rRows;
	}

	/**
	 * The bake smelt rows (task food-bake-recipes) — the MultiItemFood.java oven family,
	 * the RM.add_smelting(in, out, F, T, F) lines VERBATIM (the vanilla smelt constants 0 xp
	 * / 200 ticks carry the unspecified upstream columns, the clayBowlSmeltingBuilder shape):
	 * :348 the roasted potato stick, :599 the cookie (→ vanilla Items.cookie), :607/:615 the
	 * raisin cookies, :626 the Abyssal cookie ELSE-leg (the :623 NeLi_Cookie leg is the
	 * foreign gate — the GT6-only face), :634 the cake bottom, :653/:659/:665/:671 the pizza
	 * quartet, :679 the bun, :718 the raw bread (→ vanilla Items.bread, the Food_Bread alias
	 * :715), :748 the baguette, :778/:779 the toast pair — plus the :317 egg row (task
	 * pool-drain-food-t5-tail, the Loader_Recipes_Food.java:317 smelt: the vanilla egg →
	 * Food_Egg_Fried; the listener-channel faces — the itemEggBig ×4 tAmount leg and the GT
	 * twin eggs — have no datagen carrier, declared at the row). The :590 Abyssal-Dough →
	 * NeLi_Bread row is the foreign-gated TRUE NEGATIVE (declared, no row).
	 */
	private List<BakeSmeltRow> bakeSmeltRows() {
		Object[][] tSmelts = {
				{GT6RecipesBakeIds.POTATO_ON_STICK, GT6RecipesBakeIds.POTATO_ON_STICK_ROASTED, "smelt_potato_on_stick"},
				{GT6RecipesBakeIds.COOKIE_RAW, null, "smelt_cookie"},
				{GT6RecipesBakeIds.COOKIE_RAISINS_RAW, GT6RecipesBakeIds.COOKIE_RAISINS, "smelt_cookie_raisins"},
				{GT6RecipesBakeIds.COOKIE_CHOCO_RAISINS_RAW, GT6RecipesBakeIds.COOKIE_CHOCO_RAISINS, "smelt_cookie_choco_raisins"},
				{GT6RecipesBakeIds.COOKIE_ABYSSAL_RAW, null, "smelt_cookie_abyssal"},
				{GT6RecipesBakeIds.CAKEBOTTOM_RAW, GT6RecipesBakeIds.CAKEBOTTOM, "smelt_cakebottom"},
				{GT6RecipesBakeIds.PIZZA_CHEESE_RAW, GT6RecipesBakeIds.PIZZA_CHEESE, "smelt_pizza_cheese"},
				{GT6RecipesBakeIds.PIZZA_MEAT_RAW, GT6RecipesBakeIds.PIZZA_MEAT, "smelt_pizza_meat"},
				{GT6RecipesBakeIds.PIZZA_VEGGIE_RAW, GT6RecipesBakeIds.PIZZA_VEGGIE, "smelt_pizza_veggie"},
				{GT6RecipesBakeIds.PIZZA_ANANAS_RAW, GT6RecipesBakeIds.PIZZA_ANANAS, "smelt_pizza_ananas"},
				{GT6RecipesBakeIds.BUN_RAW, GT6RecipesBakeIds.BUN, "smelt_bun"},
				{GT6RecipesBakeIds.BREAD_RAW, null, "smelt_bread_raw"},
				{GT6RecipesBakeIds.BAGUETTE_RAW, GT6RecipesBakeIds.BAGUETTE, "smelt_baguette"},
				{GT6RecipesBakeIds.TOAST_RAW, GT6RecipesBakeIds.TOAST, "smelt_toast"},
				{GT6RecipesBakeIds.TOAST_SLICED, GT6RecipesBakeIds.TOASTED_SLICED, "smelt_toast_sliced"}};
		List<BakeSmeltRow> rRows = new ArrayList<>();
		for (Object[] tSmelt : tSmelts) {
			Item tIn = bakeItem((int) tSmelt[0]);
			Item tOut = tSmelt[1] == null ? (tSmelt[2].equals("smelt_cookie") || tSmelt[2].equals("smelt_cookie_abyssal") ? Items.COOKIE : Items.BREAD) : bakeItem((int) tSmelt[1]);
			rRows.add(new BakeSmeltRow(bakeSmeltBuilder(tIn, tOut), bakeId((String) tSmelt[2])));
		}
		// :317 — RM.add_smelting(aEvent.mStack, IL.Food_Egg_Fried.get(tAmount)); the vanilla
		// egg rides the listener at tAmount=1 (the GT6MeatDatagen smelt JSON shape)
		rRows.add(new BakeSmeltRow(bakeSmeltBuilder(Items.EGG, foodItem(5)), bakeId("smelt_egg"))); // index 5 = food_egg_fried (:499, meta 1070)
		return rRows;
	}

	/** One bake smelt builder (the clayBowlSmeltingBuilder leg-split: 1.20.1 ItemLike result / 21.1 ItemStack result). */
	private SimpleCookingRecipeBuilder bakeSmeltBuilder(net.minecraft.world.level.ItemLike aIn, net.minecraft.world.level.ItemLike aOut) {
		//? if forge {
		return SimpleCookingRecipeBuilder.smelting(
						net.minecraft.world.item.crafting.Ingredient.of(aIn),
						RecipeCategory.MISC, aOut, 0.0F, 200)
				.unlockedBy("has_input", has(aIn));
		//?} else {
		/*return SimpleCookingRecipeBuilder.smelting(
						net.minecraft.world.item.crafting.Ingredient.of(aIn),
						RecipeCategory.MISC, new ItemStack(aOut.asItem()), 0.0F, 200)
				.unlockedBy("has_input", has(aIn));
		*///?}
	}

	/** The kX family row — "kX" over the knife tag + the input item, result × count (the upstream CR.DEF_NCC face). */
	private ShapedRecipeBuilder shapedBake(Item aResult, int aCount, Item aInput) {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aResult, aCount)
				.pattern("kX")
				.define('k', GT6ItemTags.TOOLS_KNIFE)
				.define('X', aInput)
				.unlockedBy("has_input", has(aInput));
	}

	/**
	 * The ANY.Wood stick union ingredient (the upstream OP.stick.dat(ANY.Wood) oredict face)
	 * — resolved live: every GT wood stick + the vanilla stick (the upstream "stickWood"
	 * oredict member; the port carries no item-data face for it, so the union is the carrier).
	 */
	private net.minecraft.world.item.crafting.Ingredient woodStickIngredient() {
		List<Item> tSticks = new ArrayList<>();
		tSticks.add(Items.STICK);
		for (OreDictMaterial tMat : gregapi.data.ANY.Wood.mToThis) {
			Item tStick = itemOrNull(gregapi.data.OP.stick, tMat);
			if (tStick != null) tSticks.add(tStick);
		}
		if (tSticks.size() < 2) throw new IllegalStateException("no wood sticks resolved for the bake potato-on-stick rows");
		return net.minecraft.world.item.crafting.Ingredient.of(tSticks.toArray(new Item[0]));
	}

	/** A material-pair item that MUST resolve (the loud face: a missing declared-present leg fails the datagen run). */
	private Item materialItemOrThrow(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		Item tItem = itemOrNull(aPrefix, aMaterial);
		if (tItem == null) throw new IllegalStateException("bake band leg missing: " + aPrefix.mNameInternal + aMaterial.mNameInternal);
		return tItem;
	}

	/** A bake food by {@link GT6BakeFoods#BAKE_ROWS} index (the GT6RecipesBake wiring constants, loud). */
	private Item bakeItem(int aIndex) {
		return GT6BakeFoods.FOODS.get(aIndex).get();
	}

	/**
	 * A food by the {@link gregtech6.registry.GT6Foods#FOOD_ROWS} index (the same table the
	 * registration walks — ascending upstream meta order, the upstream line anchors live at
	 * the call sites).
	 */
	private Item foodItem(int aIndex) {
		return gregtech6.registry.GT6Foods.FOODS.get(aIndex).get();
	}

	/** A crop food by the {@link gregtech6.registry.GT6CropFoods#FOOD_ROWS} index (the T5a table walk, the same id-pinning the GT6CropFoodsTest census rides). */
	private Item cropFoodItem(int aIndex) {
		return gregtech6.registry.GT6CropFoods.FOODS.get(aIndex).get();
	}

	/** The bake row save id — {@code bake_<name>} under the gt6 namespace. */
	private static ResourceLocation bakeId(String aName) {
		return new ResourceLocation(GT6DataGenerators.MOD_ID, "bake_" + aName);
	}

	/** The {@link gregtech6.recipes.GT6RecipesBake} wiring-constant mirror for the datagen walks (the single source is GT6RecipesBake; the local alias keeps the datagen file free of the recipes-package import). The non-constant indices (the rows GT6RecipesBake's runtime band does not touch) are the BAKE_ROWS order facts, pinned by the test walk. */
	private static final class GT6RecipesBakeIds {
		static final int COOKIE_RAW = gregtech6.recipes.GT6RecipesBake.BAKE_COOKIE_RAW;
		static final int COOKIE_RAISINS_RAW = gregtech6.recipes.GT6RecipesBake.BAKE_COOKIE_RAISINS_RAW;
		static final int COOKIE_RAISINS = 2;
		static final int COOKIE_CHOCO_RAISINS_RAW = gregtech6.recipes.GT6RecipesBake.BAKE_COOKIE_CHOCO_RAISINS_RAW;
		static final int COOKIE_CHOCO_RAISINS = 4;
		static final int COOKIE_ABYSSAL_RAW = gregtech6.recipes.GT6RecipesBake.BAKE_COOKIE_ABYSSAL_RAW;
		static final int CAKEBOTTOM_RAW = gregtech6.recipes.GT6RecipesBake.BAKE_CAKEBOTTOM_RAW;
		static final int CAKEBOTTOM = 7;
		static final int DOUGH_FLAT = gregtech6.recipes.GT6RecipesBake.BAKE_DOUGH_FLAT;
		static final int DOUGH_FLAT_KETCHUP = 9;
		static final int PIZZA_CHEESE_RAW = 10, PIZZA_CHEESE = 11, PIZZA_MEAT_RAW = 12, PIZZA_MEAT = 13,
				PIZZA_VEGGIE_RAW = 14, PIZZA_VEGGIE = 15, PIZZA_ANANAS_RAW = 16, PIZZA_ANANAS = 17;
		static final int BUN_RAW = gregtech6.recipes.GT6RecipesBake.BAKE_BUN_RAW;
		static final int BUN = gregtech6.recipes.GT6RecipesBake.BAKE_BUN;
		static final int BUN_SLICED = gregtech6.recipes.GT6RecipesBake.BAKE_BUN_SLICED;
		static final int BUNS_SLICED = gregtech6.recipes.GT6RecipesBake.BAKE_BUNS_SLICED;
		static final int BURGER_VEGGIE = 22, BURGER_CHEESE = 23, BURGER_MEAT = 24, BURGER_CHUM = 25, BURGER_TOFU = 26, BURGER_SOYLENT = 27, BURGER_FISH = 28;
		static final int BREAD_RAW = gregtech6.recipes.GT6RecipesBake.BAKE_BREAD_RAW;
		static final int BREAD_SLICED = gregtech6.recipes.GT6RecipesBake.BAKE_BREAD_SLICED;
		static final int BREADS_SLICED = gregtech6.recipes.GT6RecipesBake.BAKE_BREADS_SLICED;
		static final int SANDWICH_VEGGIE = 32, SANDWICH_CHEESE = 33, SANDWICH_BACON = 34, SANDWICH_STEAK = 35;
		static final int BAGUETTE_RAW = gregtech6.recipes.GT6RecipesBake.BAKE_BAGUETTE_RAW;
		static final int BAGUETTE = gregtech6.recipes.GT6RecipesBake.BAKE_BAGUETTE;
		static final int BAGUETTE_SLICED = gregtech6.recipes.GT6RecipesBake.BAKE_BAGUETTE_SLICED;
		static final int BAGUETTES_SLICED = gregtech6.recipes.GT6RecipesBake.BAKE_BAGUETTES_SLICED;
		static final int LARGE_SANDWICH_VEGGIE = 40, LARGE_SANDWICH_CHEESE = 41, LARGE_SANDWICH_BACON = 42, LARGE_SANDWICH_STEAK = 43;
		static final int FRIES_RAW = gregtech6.recipes.GT6RecipesBake.BAKE_FRIES_RAW;
		static final int FRIES = gregtech6.recipes.GT6RecipesBake.BAKE_FRIES;
		static final int FRIES_PACKAGED = gregtech6.recipes.GT6RecipesBake.BAKE_FRIES_PACKAGED;
		static final int TOAST_RAW = gregtech6.recipes.GT6RecipesBake.BAKE_TOAST_RAW;
		static final int TOAST = gregtech6.recipes.GT6RecipesBake.BAKE_TOAST;
		static final int TOAST_SLICED = gregtech6.recipes.GT6RecipesBake.BAKE_TOAST_SLICED;
		static final int TOASTED_SLICED = 50;
		static final int DOUGH = gregtech6.recipes.GT6RecipesBake.BAKE_DOUGH;
		static final int DOUGH_SUGAR = gregtech6.recipes.GT6RecipesBake.BAKE_DOUGH_SUGAR;
		static final int DOUGH_CHOCOLATE = gregtech6.recipes.GT6RecipesBake.BAKE_DOUGH_CHOCOLATE;
		static final int DOUGH_SUGAR_RAISINS = gregtech6.recipes.GT6RecipesBake.BAKE_DOUGH_SUGAR_RAISINS;
		static final int DOUGH_SUGAR_CHOCO_RAISINS = gregtech6.recipes.GT6RecipesBake.BAKE_DOUGH_SUGAR_CHOCO_RAISINS;
		static final int DOUGH_ABYSSAL = gregtech6.recipes.GT6RecipesBake.BAKE_DOUGH_ABYSSAL;
		static final int POTATO_ON_STICK = 58, POTATO_ON_STICK_ROASTED = 59;

		private GT6RecipesBakeIds() {}
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
	 * The mortar crafting rows (task mortar-family) — the upstream registration tails
	 * VERBATIM (Loader_MultiTileEntities.java:2179-2183, the {@code "P", "B", 'B',
	 * IL.Ceramic_Bowl, 'P', OP.<prefix>.dat(<material>)} shapeless pair): the base =
	 * the port Ceramic-bowl stand-in {@code GT6Kitchen.MIXING_BOWL_ITEM} (the declared
	 * mixing-bowl mapping, the research.manual-devices-port ruling — upstream
	 * IL.Ceramic_Bowl is the kitchen bowl's own 1.7.10 identity) + the row's {@code 'P'}
	 * ingredient ({@link GT6Mortars#pestleIngredient}: the DESIGN-0 STEEL-pestle row
	 * crafts from an ANY.Iron INGOT (:2179 — the upstream column, not the pestle
	 * column), Netherite from its ingot (:2180), the three gems from their gems
	 * (:2181-2183)). 1 + 1 → 1 mortar. The id is the result-path convention
	 * ({@link #mortarRecipeId}).
	 */
	private ShapelessRecipeBuilder mortarBuilder(gregtech6.registry.GT6Mortars.MortarRow aRow) {
		// the bare-local form (the :3567 rail precedent) — naming the holder type here races
		// the stonecutter RegistryObject/DeferredHolder swap inside this file's manual
		// `//? if forge` import guard (the swapped neo import lands inside the dead forge
		// comment and the 1.21.1 leg stops compiling); `var` never names it
		var tPestle = gregtech6.registry.GTMaterialItems.get(
				gregtech6.registry.GT6Mortars.pestlePrefix(aRow), gregtech6.registry.GT6Mortars.pestleIngredient(aRow));
		if (tPestle == null) return null; // the row's ingredient is driver-hidden — the skip semantics (the hopperRecipeBuilder form)
		net.minecraft.world.item.Item tPestleItem = tPestle.get();
		return ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, gregtech6.registry.GT6Mortars.ITEMS_BY_PATH.get(aRow.path()).get())
				.requires(GT6Kitchen.MIXING_BOWL_ITEM.get())
				.requires(tPestleItem)
				.unlockedBy("has_mixing_bowl", has(GT6Kitchen.MIXING_BOWL_ITEM.get()));
	}

	/** The id of one mortar row's recipe (the result-path convention, the hopperRecipeId bare-identifier form). */
	public static ResourceLocation mortarRecipeId(gregtech6.registry.GT6Mortars.MortarRow aRow) {
		String tPath = aRow.path();
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/**
	 * The Sifting Table crafting row (task sifting-table-family) — the upstream
	 * registration pattern VERBATIM (Loader_MultiTileEntities.java:2227
	 * {@code "TdT","WxW","SPS"}): 'P' = {@code OP.plateDouble.dat(ANY.Iron)} → the port
	 * iron plate-double item, 'S' = {@code OP.stickLong.dat(ANY.Iron)}, 'T' =
	 * {@code OP.screw.dat(ANY.Iron)}, 'W' = {@code OP.wireFine.dat(ANY.Iron)}, 'd' = the
	 * screwdriver tool tag and 'x' = the wirecutter tool tag (the tool letters, not
	 * consumed — the tools carry their own crafting-remaining face, the anvil 'h'
	 * precedent). Result 1x the table block. The vanilla shaped auto-mirror carries
	 * CR.DEF_MIR (the anvil note).
	 */
	private ShapedRecipeBuilder siftingTableBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, gregtech6.registry.GT6SiftingTables.SIFTING_TABLE.get())
				.pattern("TdT")
				.pattern("WxW")
				.pattern("SPS")
				.define('T', GTMaterialItems.get(gregapi.data.OP.screw, MT.Iron).get())
				.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
				.define('W', GTMaterialItems.get(gregapi.data.OP.wireFine, MT.Iron).get())
				.define('x', GT6ItemTags.TOOLS_WIRE_CUTTER)
				.define('S', GTMaterialItems.get(gregapi.data.OP.stickLong, MT.Iron).get())
				.define('P', GTMaterialItems.get(gregapi.data.OP.plateDouble, MT.Iron).get())
				.unlockedBy("has_plate_double", has(GTMaterialItems.get(gregapi.data.OP.plateDouble, MT.Iron).get()));
	}

	/**
	 * The Grindstone crafting row (task grindstone-family) — the upstream registration
	 * pattern VERBATIM (Loader_MultiTileEntities.java:2226 {@code "SAS","SwS","PPP"}):
	 * 'S' = {@code OP.stickLong.dat(ANY.Iron)} → the iron-member fold {@code gt6:stick_long_iron}
	 * (the bumbliary screw_iron precedent — stickLong has no tag family in the port), 'A' =
	 * {@code OP.stick.dat(ANY.Iron)} → {@code gt6:stick_iron}, 'P' = {@code OP.plateDouble
	 * .dat(ANY.Iron)} → the {@code #gt6:double_plates/iron} tag (the transformer-row form),
	 * 'w' = {@code #gt6:tools/wrench} (the tool letter, not consumed). Result 1x
	 * {@code gt6:grindstone}. The vanilla shaped auto-mirror carries CR.DEF_MIR (the
	 * bathing-pot note).
	 */
	private ShapedRecipeBuilder grindstoneBuilder() {
		TagKey<Item> tDoublePlates = GT6ItemTags.materialTag(GT6ItemTags.DOUBLE_PLATES_FAMILY, MT.Iron);
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, gregtech6.registry.GT6Grindstones.GRINDSTONE.get())
				.pattern("SAS")
				.pattern("SwS")
				.pattern("PPP")
				.define('S', GTMaterialItems.get(gregapi.data.OP.stickLong, MT.Iron).get())
				.define('A', GTMaterialItems.get(gregapi.data.OP.stick, MT.Iron).get())
				.define('P', tDoublePlates)
				.define('w', GT6ItemTags.TOOLS_WRENCH)
				.unlockedBy("has_stick_long_iron", has(GTMaterialItems.get(gregapi.data.OP.stickLong, MT.Iron).get()));
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
	 * The four colored-clay hardening smelts (task worldgen-diggables-pits) — the
	 * BlockDiggable.java:80-84 registration rows VERBATIM (the Diggables metas 1/4/5/6 →
	 * hardened_clay, 1.20.1 TERRACOTTA): xp 0 (the aEXP overload default), 200 ticks —
	 * the clayBowlSmeltingBuilder face. The 5-arg {@code add_smelting}'s EtFu
	 * smoker/blast flags are the foreign-mod leg (the mdh ruling, CUT). The ids are
	 * INPUT-named — the four rows share the terracotta output, so the output-path
	 * CLAY_BOWL_SMELT_ID convention cannot discriminate. The :82 red-clay row stays OUT —
	 * the meta 3 block is the GT6NetherOres nether_red_clay stand-in domain.
	 */
	private static List<BakeSmeltRow> clayPitSmeltRows() {
		String[] tColors = {"brown", "yellow", "blue", "white"};
		List<BakeSmeltRow> rRows = new ArrayList<>();
		for (int i = 0; i < GT6SurfaceBlocks.CLAY_BAND.size(); i++) {
			net.minecraft.world.level.ItemLike tIn = GT6SurfaceBlocks.CLAY_BAND.get(i).get();
			String tColor = tColors[i];
			//? if forge {
			rRows.add(new BakeSmeltRow(SimpleCookingRecipeBuilder.smelting(
							net.minecraft.world.item.crafting.Ingredient.of(tIn),
							RecipeCategory.MISC, net.minecraft.world.level.block.Blocks.TERRACOTTA, 0.0F, 200)
					.unlockedBy("has_" + tColor + "_clay", has(tIn)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, "smelt_" + tColor + "_clay")));
			//?} else {
			/*rRows.add(new BakeSmeltRow(SimpleCookingRecipeBuilder.smelting(
							net.minecraft.world.item.crafting.Ingredient.of(tIn),
							RecipeCategory.MISC, new ItemStack(net.minecraft.world.level.block.Blocks.TERRACOTTA), 0.0F, 200)
					.unlockedBy("has_" + tColor + "_clay", has(tIn)),
					new ResourceLocation(GT6DataGenerators.MOD_ID, "smelt_" + tColor + "_clay")));
			*///?}
		}
		return rRows;
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
	/**
	 * The circuit wiring crafting row (task circuit-chain-recipes) — the upstream
	 * {@code CR.shaped(IL.Circuit_Wire_Copper.get(1), CR.DEF, "WWW", "WxW", "WWW", 'W',
	 * OP.wireFine.dat(ANY.Cu))} VERBATIM (MultiItemTechnological.java:571): eight fine
	 * wires in a ring, the empty center (the CR 'x' empty-slot marker ports to the vanilla
	 * space — the builder rejects undefined symbols). 'W' = {@code OP.wireFine.dat(ANY.Cu)}
	 * → the {@code #forge:fine_wires/copper} material tag (the Progress Sensor 'W'
	 * precedent, TagPrefix.java:575) — the tag carries the whole ANY.Cu family (Copper +
	 * AnnealedCopper), so the single upstream oredict row ports as one tag row. Result
	 * 1x {@code gt6:circuit_wire_copper} (the GT6CircuitChain carrier, meta 30001).
	 */
	private ShapedRecipeBuilder circuitWireCopperBuilder() {
		TagKey<Item> tFineWires = GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY, MT.Copper);
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6CircuitChain.ITEMS_BY_PATH.get("circuit_wire_copper").get())
				.pattern("WWW")
				.pattern("W W")
				.pattern("WWW")
				.define('W', tFineWires)
				.unlockedBy("has_fine_wire", has(tFineWires));
	}

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
				var tCasing = GTMaterialItems.get(gregapi.data.OP.casingSmall,
						gregtech6.registry.GT6ElectricDynamos.ELECTRIC_T_LADDER.get(tRow.tier()).get());
				if (tCasing == null) continue; // a tier's housing can be driver-hidden (the dynamo ladder carries Al, TiC PRIMARY :2350) — the skip semantics
				tM = tCasing.get();
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

	/** The rung path of a family ladder index (the gregtech6.registry.GTMachines.bridgePath form mirrored locally). */
	private static String bridgePath(String aFamily, int aTier) {
		return aTier == 0 ? aFamily : aFamily + "_t" + (aTier + 1);
	}

	private static java.util.List<BridgeCraftRow> euBridgeCraftingRows() {
		java.util.List<BridgeCraftRow> rRows = new java.util.ArrayList<>();
		// --- the Heaters :817-821 ("TCT","CMC","TCd") — T5 CUT (SiC wires absent) ---
		gregapi.oredict.OreDictMaterial[] tHeaterWires = {gregapi.data.MT.Copper, gregapi.data.MT.Constantan, gregapi.data.MT.Kanthal, gregapi.data.MT.Nichrome};
		for (int i = 0; i < 4; i++) {
			gregapi.oredict.OreDictMaterial tMat = bridgeMat(i);
			if (GTMaterialItems.get(gregapi.data.OP.screw, tMat) == null) continue; // the rung's housing (bridgeMat(1) = Al, TiC PRIMARY :2350) is driver-hidden — the skip semantics
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
			if (GTMaterialItems.get(gregapi.data.OP.plateTriple, tMat) == null) continue; // the rung's housing (bridgeMat(1) = Al, TiC PRIMARY :2350) is driver-hidden — the skip semantics
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
			if (GTMaterialItems.get(gregapi.data.OP.screw, tMat) == null) continue; // the rung's housing (bridgeMat(1) = Al, TiC PRIMARY :2350) is driver-hidden — the skip semantics
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

	/** The Electric_T[1..5] rung material by ladder index (upstream MT.java:3691 members, the dynamo family's ladder face) — the single source lives in {@code gregtech6.registry.GTMachines.electricTierMat} (task c2-controller-tint: the bridge/laser tint rows ride the same ladder). */
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
    //     ladder T2 = upstream 10022 LV, gregtech6.registry.GTMachines.java:4711);
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

    // -------------------------------------------------------------------------
    // task crafting-barrels-boilers — the Fluid Containers barrel pair + the 26 Steam
    // Boiler rows (the recipe-bidirectional-census P1 card; the upstream registration
    // tails VERBATIM, Loader_MultiTileEntities.java):
    // <ul>
    // <li>:2140 Wooden Barrel — "rGs","PSP","PSP": 'r' = the softhammer tool letter
    //     (CR.java:355 auto-bind), 'G' = OD.itemGlue (no glue item in the port universe
    //     → the slime-ball fold, the static-storage bottlecrate precedent), 's' = the saw
    //     tool letter, 'P' = OP.plate.dat(MT.WoodTreated), 'S' = OP.stickLong.dat(ANY.Iron);</li>
    // <li>:2151 Bronze Drum — " h ","PSP","PSP": 'P' = OP.plateCurved.dat(MT.Bronze),
    //     'S' = OP.stickLong.dat(MT.Bronze), 'h' = the hard-hammer tool letter;</li>
    // <li>:553-565 Steam Boiler Tank (13) — " P ","PwP","PhP", 'P' = OP.plateDouble.dat(aMat),
    //     'w' = the wrench letter, 'h' = the hard-hammer letter (both CR auto-binds);</li>
    // <li>:567-579 Strong Steam Boiler Tank (13) — the same grid, 'P' =
    //     OP.plateDense.dat(aMat) (the :567 HBM-conditional .mat(1) is the same item, the
    //     GT6Boilers javadoc note).</li>
    // </ul>
    // DECLARED CUT: :2150 Plastic Canister — the upstream row carries NO recipe tail
    // (the IL.PlasticCan oredict bind is its whole tail) → the port ships no recipe by
    // upstream fidelity, NOT a census gap (the negative pin lives in the crafting-json
    // test).
    //
    // Dependency note (the census double-break): the plate_double/plate_dense CRAFTING
    // band is the parallel crafting-machines-steam-band card's deliverable (merge order:
    // steam-band first, then this card) — these rows reference the ITEM ids, which all
    // 26 boiler materials carry (the machine-channel welder rows are the other face of
    // the same inputs).
    // -------------------------------------------------------------------------
    private static final String BARREL_RECIPE_PREFIX = "barrel/";
    private static final String BOILER_RECIPE_PREFIX = "boiler/";

    /** One barrel row's recipe id: barrel/&lt;path&gt; (the tank_valve/&lt;path&gt; convention). */
    private ResourceLocation barrelRecipeId(String aPath) {
        return new ResourceLocation(GT6DataGenerators.MOD_ID, BARREL_RECIPE_PREFIX + aPath);
    }

    /** One boiler row's recipe id: boiler/&lt;path&gt; (the same convention). */
    private ResourceLocation boilerRecipeId(String aPath) {
        return new ResourceLocation(GT6DataGenerators.MOD_ID, BOILER_RECIPE_PREFIX + aPath);
    }

    /**
     * The two barrel crafting rows (:2140 wood + :2151 bronze drum). The plastic
     * canister (:2150) is the declared upstream CUT — no row here.
     */
    private java.util.List<PartFamilyRecipeRow> barrelRecipeBuilders() {
        java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
        // :2140 Wooden Barrel — 'G' = OD.itemGlue folds to the slime ball (no glue item
        // in the port universe, the bottlecrate builder's declared deviation).
        Item tPlate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.WoodTreated);
        // the ANY.Iron iron-member fold (the :2767 stick_long_iron house precedent —
        // the GTMaterialItems INDEX keys concrete members, never the ANY group object).
        Item tStick = itemOrNull(gregapi.data.OP.stickLong, gregapi.data.MT.Fe);
        if (tPlate != null && tStick != null) {
            rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTBarrels.BARREL_ITEM.get())
                    .pattern("rGs").pattern("PSP").pattern("PSP")
                    .define('r', GT6ItemTags.TOOLS_SOFT_HAMMER)
                    .define('G', Items.SLIME_BALL)
                    .define('s', GT6ItemTags.TOOLS_SAW)
                    .define('P', tPlate)
                    .define('S', tStick)
                    .unlockedBy("has_plate", has(tPlate)), barrelRecipeId("barrel_wood")));
        }
        // :2151 Bronze Drum
        Item tCurved = itemOrNull(gregapi.data.OP.plateCurved, gregapi.data.MT.Bronze);
        Item tStickB = itemOrNull(gregapi.data.OP.stickLong, gregapi.data.MT.Bronze);
        if (tCurved != null && tStickB != null) {
            rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTBarrels.BARREL_METAL_ITEM.get())
                    .pattern(" h ").pattern("PSP").pattern("PSP")
                    .define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
                    .define('P', tCurved)
                    .define('S', tStickB)
                    .unlockedBy("has_plate", has(tCurved)), barrelRecipeId("barrel_metal")));
        }
        return rRows;
    }

    /**
     * The 26 Steam Boiler rows (:553-565 standard plateDouble + :567-579 Strong
     * plateDense over the same " P ","PwP","PhP" grid, the upstream verbatim pair).
     */
    private java.util.List<PartFamilyRecipeRow> boilerRecipeBuilders() {
        java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
        for (gregtech6.registry.GT6Boilers.BoilerRow tRow : gregtech6.registry.GT6Boilers.allRows()) {
            // the ANY-group primary-member folds (the :4662 robot-tip house precedent): the
            // :559/:573 rows ride ANY.Steel → MT.Steel, the :563/:577 rows ANY.W → MT.W —
            // the port INDEX keys concrete members, never the ANY group object.
            gregapi.oredict.OreDictMaterial tMat = tRow.material().mat().get();
            if (tMat == gregapi.data.ANY.Steel) tMat = gregapi.data.MT.Steel;
            if (tMat == gregapi.data.ANY.W) tMat = gregapi.data.MT.W;
            var tPlate = gregtech6.registry.GTMaterialItems.get(
                    tRow.strong() ? gregapi.data.OP.plateDense : gregapi.data.OP.plateDouble,
                    tMat); // the bare-local form (the swap note above)
            if (tPlate == null) continue;
            rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Boilers.ITEMS_BY_PATH.get(tRow.path()).get())
                    .pattern(" P ").pattern("PwP").pattern("PhP")
                    .define('P', tPlate.get())
                    .define('w', GT6ItemTags.TOOLS_WRENCH)
                    .define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
                    .unlockedBy("has_plate", has(tPlate.get())), boilerRecipeId(tRow.path())));
        }
        return rRows;
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

    // ---------------------------------------------------------------------
    // The blade-tool steel-route rows (task w5-t2-blade-six). The upstream
    // AdvancedCraftingTool rows (Loader_Tools.java:337/:342-343 — SWORD toolHeadSword,
    // AXE toolHeadAxe, DOUBLE_AXE toolHeadAxeDouble; KNIFE/BUTCHERYKNIFE carry NO
    // AdvancedCraftingTool row — the knife's grid face is the cutting-board pool) over
    // the single steel tier: the plate counts keep the head-amount PROPORTIONS (the t1
    // mapping; the exact toolHead mAmount ladder is the standing pool cut). The worn
    // hammer/file letters ride the dig-tool builder (each pays one point through the
    // crafting-remaining face). The club's t1 placeholder retired with task
    // tool-arg8-nine-families — the club's upstream face is the :329 OreProcessing_Tool
    // direct rows, the arg-8 band's per-material face.
    // -----------------------------------------------------------------------
    // The field-tool steel-route rows (task w5-t4-field-five). The upstream
    // AdvancedCraftingTool rows for hoe/sense/plow (Loader_Tools.java:344-346 — the
    // :344 Birch and :346 Spruce suggestions converge steel per the t1 single-tier
    // ruling). Plate counts keep the upstream tool-head material ratios
    // (OP.java:244-246/:254): hoe 2 plates (toolHeadHoe U*2), sense 3 (toolHeadSense
    // U*3), plow 4 (toolHeadPlow U*4). The branch cutter's t1 placeholder retired with
    // task tool-arg8-nine-families (the upstream face is the :325 direct rows, the
    // arg-8 band) — the hand drill likewise (:330, the toolHeadArrow+2*bolt row).
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

    /** The sense row — the 3-plate scythe blade (the :345 row). */
    private ShapedRecipeBuilder senseBuilder() {
        return digToolBuilder(GT6Tools.SENSE.get(), new String[] {"PPP", " S ", "hSf"},
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

    // ------------------------------------------------------------------
    // task w5-t5-scene-six — the scene-tool rows. The OreProcessing_Tool
    // uppercase alphabet (Loader_Tools.java:308-318 comment): I=ingot P=plate
    // T=screw O=ring S=stick G=gem C=plateGem R=stone; the lowercase letters are
    // the CR.java:339-361 tool keys. Single-steel-tier convergence folds the
    // material loops onto the steel plates/screw/ring items (the t7 pocket
    // letter-by-letter precedent). The scissors/scoop/plunger t1 placeholders
    // retired with task tool-arg8-nine-families — their upstream faces are the
    // :326/:320/:315 direct rows, the arg-8 band's per-material face.

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
            ShapedRecipeBuilder tToolBuilder = electricToolBuilder(tSpec);
            if (tToolBuilder == null) continue; // the tier's ladder material is driver-hidden (the Al rung) — the skip semantics
            rRows.add(new ElectricToolRow(tToolBuilder, electricToolRecipeId(tSpec.aPath())));
        }
        return rRows;
    }

    /**
     * One row's builder — the pattern/key columns live in the two upstream tables
     * (the shape strings :357-377 and the OreProcessing_Tool ctor args per row).
     */
    // package-private: the GT6DatagenWalkLegTest seam (the guarded walk builders the leg pins)
    ShapedRecipeBuilder electricToolBuilder(gregtech6.items.tools.electric.GT6ElectricToolItem.Spec aSpec) {
        String tPath = aSpec.aPath();
        int tTier = aSpec.aTier();
        gregapi.oredict.OreDictMaterial tTierMat = electricMaterial(tTier);
        // null-drop guard: the Electric_T ladder rungs are foreign-visible too (rung Al, TiC PRIMARY
        // :2350) — a tier's housing-hidden tool skips, the hopper semantics (every pattern touches
        // the 'W' tTierMat column, so its pair is the representative anchor).
        gregapi.oredict.OreDictPrefix tW = tPath.equals("trimmer_lv") || tPath.equals("jackhammer_hv_normal")
                ? gregapi.data.OP.gearGtSmall : gregapi.data.OP.stick;
        if (GTMaterialItems.get(tW, tTierMat) == null) return null;
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
    // package-private: the GT6DatagenWalkLegTest seam (the guarded walk builders the leg pins)
    ShapedRecipeBuilder armorPieceBuilder(GT6ArmorMaterials.SuitRow aSuit, int aSlot) {
        if (aSuit.suit() != GT6ArmorMaterials.UNIVERSAL) {
            // the 'M'-column guard BEFORE the registry reads: a driver-hidden suit 'M' (HEAT = Al,
            // TiC PRIMARY :2350) skips the suit's pieces, the hopper semantics
            Item tEarlyMaterial = suitMaterial(aSuit);
            if (tEarlyMaterial == null) return null;
        }
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

    /** The 'M' column per base suit — the Loader_Tools.java:68-91 material rows; null when the
     * material is driver-hidden (HEAT rides MT.Al, the TiC PRIMARY row :2350 — the walk skips
     * the suit, the hopper semantics; Rubber/Asbestos/Pb are COMMON_SECONDARY and never hide). */
    private Item suitMaterial(GT6ArmorMaterials.SuitRow aSuit) {
        return switch (aSuit.suit()) {
            case INSECTS -> itemOrNull(gregapi.data.OP.foil, gregapi.data.MT.Rubber);
            case FROST -> itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Asbestos);
            case HEAT -> itemOrNull(gregapi.data.OP.foil, gregapi.data.MT.Al);
            case RADIATION -> itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Pb);
            case BIOCHEMGAS -> itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Rubber);
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

	// -------------------------------------------------------------------------
	// task robotics-chain — the Autocrafter tip rows (MultiItemRandomTools.java:481-490)
	// and the four compact component families' self-crafting rows
	// (MultiItemTechnological.java:404-421). Grids and columns VERBATIM:
	//   - tips: 'P' plateCurved SteelGalvanized, 'C' OD_CIRCUITS[3] → #gt6:circuit3 (the
	//     HV rung), 'M' MOTORS[3]/PISTONS[3]/CONVEYERS[3], 'X' the per-row tool-head
	//     column (robotTipXColumn), the saw 'D' dust ANY.Diamond→Diamond; the lowercase
	//     'w'/'h'/'f'/'d' = the CR.DEF tool letters → the #gt6:tools/* tags, defined per
	//     row-shape so no phantom keys (the craftfrom convention).
	//   - motors :404-415: 'I' the magnetic ladder (IronMagnetic row 1 + its :407
	//     SteelMagnetic DEF twin, SteelMagnetic rows 2-3, NeodymiumMagnetic rows 4-5,
	//     stickLong rows 6-9), 'P' plateCurved / 'R' stick over Electric_T[i]
	//     (MT.java:3691), 'W' wireGt01-09 (rows 1-3 walk ANY.Cu → AnnealedCopper — the
	//     only Cu member carrying port wire items, the WIRES-condition face; rows 4-9
	//     AnnealedCopper verbatim), 'C' CABLES_01[i] (MT.java:3631 = insulated 01 over
	//     Pb/Sn/Cu/Au/Al/Pt, then the bare graphene wire01 fold :3638 — the
	//     compactComponent band precedent). Row 0's pair (:404-405) is CUT —
	//     OP.wireFine carries no port item/tag face (the FIELD_GENERATORS ULV precedent);
	//     declared here and pinned in GT6RoboticsCensusTest.
	//   - conveyers/pistons/robot_arms :419-421 (the shared for-loop; the PUMPS row stays
	//     POOLED — the pumps are not this card's four prefixes): 'R' plate
	//     ANY.Rubber→Rubber (ANY.java:144 — the group's only member), 'T' screw / 'P'
	//     plate / 'S' stick / 'G' gearGtSmall over Electric_T[i], the arms 'E'
	//     OD_CIRCUITS[i] → #gt6:circuit<i> (rungs 7-9 CUT — no #gt6:circuit7..9 tags, the
	//     compactComponent band precedent) and 'P' column = the PISTONS[i] item.
	// LIVE: 10 tips + 10 motors (9 rungs + the SteelMagnetic twin) + 10 conveyers +
	// 10 pistons + 7 arms = 47; CUT: 2 motor rows (wireFine) + 3 arm rungs (circuit7-9);
	// POOLED: the 10 PUMPS loop rows.
	// -------------------------------------------------------------------------

	/** The tip rows, upstream order (:481-490): the kind leaf, the 'M' family index into GT6Robotics, the third-row pattern. */
	private static final String[][] ROBOT_TIP_ROWS = {
			{"wrench"     , "MOTORS"   , " X "},
			{"screwdriver", "MOTORS"   , " X "},
			{"saw"        , "MOTORS"   , "DXd"},
			{"hammer"     , "PISTONS"  , " X "},
			{"cutter"     , "MOTORS"   , "XfX"},
			{"chisel"     , "PISTONS"  , " X "},
			{"rubber"     , "PISTONS"  , " X "},
			{"blade"      , "PISTONS"  , " X "},
			{"drill"      , "MOTORS"   , "fX "},
			{"file"       , "CONVEYERS", " X "},
	};

	/** The kind leaf → the {@link gregtech6.registry.GT6Robotics#ROBOT_TIPS} index (the upstream :470-479 registration order). */
	private static final java.util.Map<String, Integer> ROBOT_TIP_ROW_INDEX = java.util.Map.ofEntries(
			java.util.Map.entry("wrench", 0), java.util.Map.entry("screwdriver", 1), java.util.Map.entry("saw", 2),
			java.util.Map.entry("hammer", 3), java.util.Map.entry("cutter", 4), java.util.Map.entry("chisel", 5),
			java.util.Map.entry("rubber", 6), java.util.Map.entry("blade", 7), java.util.Map.entry("drill", 8),
			java.util.Map.entry("file", 9));

	/**
	 * The tip rows' 'X' column (MultiItemRandomTools.java:481-490 verbatim pairing):
	 * the tool-head/plate/stick/dust faces. The ANY-group folds are declared inline
	 * (ANY.Rubber = MT.Rubber only; ANY.Steel/ANY.Diamond fold to the primary member —
	 * the secondary members carry no port items).
	 */
	private Item robotTipXColumn(String aKind) {
		return switch (aKind) {
			case "wrench"      -> itemOrNull(gregapi.data.OP.toolHeadWrench, gregapi.data.MT.Cr);
			case "screwdriver" -> itemOrNull(gregapi.data.OP.toolHeadScrewdriver, gregapi.data.MT.StainlessSteel);
			case "saw"         -> itemOrNull(gregapi.data.OP.toolHeadBuzzSaw, gregapi.data.MT.CobaltBrass);
			case "hammer"      -> itemOrNull(gregapi.data.OP.toolHeadHammer, gregapi.data.MT.TungstenCarbide);
			case "cutter"      -> itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.StainlessSteel);
			case "chisel"      -> itemOrNull(gregapi.data.OP.toolHeadChisel, gregapi.data.MT.TungstenSteel);
			case "rubber"      -> itemOrNull(gregapi.data.OP.toolHeadHammer, gregapi.data.MT.Rubber); // ANY.Rubber = MT.Rubber only (ANY.java:144)
			case "blade"       -> itemOrNull(gregapi.data.OP.toolHeadSword, gregapi.data.MT.Bronze);
			case "drill"       -> itemOrNull(gregapi.data.OP.stick, gregapi.data.MT.Steel); // ANY.Steel → the primary member
			case "file"        -> itemOrNull(gregapi.data.OP.dust, gregapi.data.MT.Diamond); // ANY.Diamond → the primary member
			default            -> null;
		};
	}

	/** The CABLES_01[i] wire-spec path (MT.java:3631, index = tier — the graphene tail folds to the bare wire01). */
	private static String roboticsCablePath(int aTier) {
		return aTier <= 5 ? componentWirePath(true, COMPONENT_WIRE_TOKENS[aTier], 1) : componentWirePath(false, "graphene", 1);
	}

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
	/** The forge leg of the robotics band (the tables/band doc above; the compactComponentRows shape). */
	private void robotTipRows(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		Item tPlateCurved = itemOrNull(gregapi.data.OP.plateCurved, gregapi.data.MT.SteelGalvanized);
		TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit3");
		for (String[] tSpec : ROBOT_TIP_ROWS) {
			String tKind = tSpec[0];
			Item tHead = robotTipXColumn(tKind);
			Item tRung = switch (tSpec[1]) {
				case "MOTORS" -> gregtech6.registry.GT6Robotics.MOTORS.get(3).get(); // the HV rung, :481-483/:485/:489
				case "PISTONS" -> gregtech6.registry.GT6Robotics.PISTONS.get(3).get(); // :484/:486-488
				default -> gregtech6.registry.GT6Robotics.CONVEYERS.get(3).get(); // :490
			};
			if (tPlateCurved == null || tHead == null || tRung == null) continue; // the silent-skip guard
			ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.ROBOT_TIPS.get(ROBOT_TIP_ROW_INDEX.get(tKind)).get())
					.pattern("wPh").pattern("CMC").pattern(tSpec[2])
					.define('P', tPlateCurved).define('C', tCircuit).define('M', tRung).define('X', tHead)
					.define('w', GT6ItemTags.TOOLS_WRENCH).define('h', GT6ItemTags.TOOLS_HARD_HAMMER);
			if (tSpec[2].indexOf('d') >= 0) tBuilder.define('d', GT6ItemTags.TOOLS_SCREWDRIVER); // the saw row, no phantom keys
			if (tSpec[2].indexOf('f') >= 0) tBuilder.define('f', GT6ItemTags.TOOLS_FILE); // the cutter/drill rows
			if (tSpec[2].indexOf('D') >= 0) tBuilder.define('D', itemOrNull(gregapi.data.OP.dust, gregapi.data.MT.Diamond)); // the saw row
			tBuilder.unlockedBy("has_circuit3", has(tCircuit))
					.save(aConsumer, new ResourceLocation(GT6DataGenerators.MOD_ID, "robot_tip/" + tKind));
		}
	}

	/** The forge leg of the component families' self-crafting rows (the band doc above). */
	private void roboticsComponentRows(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		gregapi.oredict.OreDictMaterial[] tElectricT = componentElectricT();
		// MOTORS :404-415 — row 0's pair is CUT (wireFine), the live rungs 1-9 + the :407 SteelMagnetic twin
		for (int i = 1; i <= 9; i++) {
			String tTier = gregtech6.items.GT6Emitters.TIER_TOKENS[i];
			Item tWire = wireItemByPath(componentWirePath(false, "annealed_copper", i)); // the wireGt multiplier face rides the GTWires block items (the upstream setTarget_ oredient isomorph, wire_<mat>_gtNN — task wiregt-prefix-item-retirement retired the prefix-item face; rows 1-3 ANY.Cu fold → the refined twin)
			Item tCable = wireItemByPath(roboticsCablePath(i));
			Item tPlate = itemOrNull(gregapi.data.OP.plateCurved, tElectricT[i]);
			Item tStick = itemOrNull(gregapi.data.OP.stick, tElectricT[i]);
			Item tMagnetic = switch (i) {
				case 1 -> itemOrNull(gregapi.data.OP.stick, gregapi.data.MT.IronMagnetic);
				case 2, 3 -> itemOrNull(gregapi.data.OP.stick, gregapi.data.MT.SteelMagnetic);
				case 4, 5 -> itemOrNull(gregapi.data.OP.stick, gregapi.data.MT.NeodymiumMagnetic);
				default -> itemOrNull(gregapi.data.OP.stickLong, gregapi.data.MT.NeodymiumMagnetic);
			};
			if (tWire == null || tCable == null || tPlate == null || tStick == null || tMagnetic == null) continue; // the silent-skip guard
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.MOTORS.get(i).get())
					.pattern("CWR").pattern("WIW").pattern("PWC")
					.define('C', tCable).define('W', tWire).define('R', tStick).define('I', tMagnetic).define('P', tPlate)
					.unlockedBy("has_wire", has(tWire))
					.save(aConsumer, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/motor_" + tTier));
			if (i == 1) { // the :407 DEF twin — the SteelMagnetic stick variant
				Item tSteelMag = itemOrNull(gregapi.data.OP.stick, gregapi.data.MT.SteelMagnetic);
				if (tSteelMag != null) {
					ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.MOTORS.get(1).get())
							.pattern("CWR").pattern("WIW").pattern("PWC")
							.define('C', tCable).define('W', tWire).define('R', tStick).define('I', tSteelMag).define('P', tPlate)
							.unlockedBy("has_wire", has(tWire))
							.save(aConsumer, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/motor_lv_steel_magnetic"));
				}
			}
		}
		for (int i = 0; i < 10; i++) { // the :418-421 loop — PUMPS stays pooled (not this card's prefixes)
			String tTier = gregtech6.items.GT6Emitters.TIER_TOKENS[i];
			Item tMotor = gregtech6.registry.GT6Robotics.MOTORS.get(i).get();
			Item tCable = wireItemByPath(roboticsCablePath(i));
			Item tPlate = itemOrNull(gregapi.data.OP.plate, tElectricT[i]);
			Item tStick = itemOrNull(gregapi.data.OP.stick, tElectricT[i]);
			if (tMotor == null || tCable == null || tPlate == null || tStick == null) continue; // the silent-skip guard
			// CONVEYERS :419 — "RRR","MCM","RRR"
			Item tRubberPlate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Rubber); // ANY.Rubber = MT.Rubber only (ANY.java:144)
			if (tRubberPlate != null) {
				ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.CONVEYERS.get(i).get())
						.pattern("RRR").pattern("MCM").pattern("RRR")
						.define('R', tRubberPlate).define('M', tMotor).define('C', tCable)
						.unlockedBy("has_motor", has(tMotor))
						.save(aConsumer, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/conveyor_" + tTier));
			}
			// PISTONS :420 — "TPP","dSS","TMG"
			Item tScrew = itemOrNull(gregapi.data.OP.screw, tElectricT[i]);
			Item tGear = itemOrNull(gregapi.data.OP.gearGtSmall, tElectricT[i]);
			if (tScrew != null && tGear != null) {
				ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.PISTONS.get(i).get())
						.pattern("TPP").pattern("dSS").pattern("TMG")
						.define('T', tScrew).define('P', tPlate).define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
						.define('S', tStick).define('M', tMotor).define('G', tGear)
						.unlockedBy("has_motor", has(tMotor))
						.save(aConsumer, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/piston_" + tTier));
			}
			// ROBOT_ARMS :421 — "CCC","MSM","PES", rungs 7-9 CUT (no #gt6:circuit7..9 tags)
			if (i <= 6) {
				TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit" + i);
				ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.ROBOT_ARMS.get(i).get())
						.pattern("CCC").pattern("MSM").pattern("PES")
						.define('C', tCable).define('M', tMotor).define('S', tStick)
						.define('P', gregtech6.registry.GT6Robotics.PISTONS.get(i).get()).define('E', tCircuit)
						.unlockedBy("has_circuit" + i, has(tCircuit))
						.save(aConsumer, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/robot_arm_" + tTier));
			}
		}
	}
	//?} else {
	/*private void robotTipRows(net.minecraft.data.recipes.RecipeOutput aOutput) {
		Item tPlateCurved = itemOrNull(gregapi.data.OP.plateCurved, gregapi.data.MT.SteelGalvanized);
		TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit3");
		for (String[] tSpec : ROBOT_TIP_ROWS) {
			String tKind = tSpec[0];
			Item tHead = robotTipXColumn(tKind);
			Item tRung = switch (tSpec[1]) {
				case "MOTORS" -> gregtech6.registry.GT6Robotics.MOTORS.get(3).get(); // the HV rung, :481-483/:485/:489
				case "PISTONS" -> gregtech6.registry.GT6Robotics.PISTONS.get(3).get(); // :484/:486-488
				default -> gregtech6.registry.GT6Robotics.CONVEYERS.get(3).get(); // :490
			};
			if (tPlateCurved == null || tHead == null || tRung == null) continue; // the silent-skip guard
			ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.ROBOT_TIPS.get(ROBOT_TIP_ROW_INDEX.get(tKind)).get())
					.pattern("wPh").pattern("CMC").pattern(tSpec[2])
					.define('P', tPlateCurved).define('C', tCircuit).define('M', tRung).define('X', tHead)
					.define('w', GT6ItemTags.TOOLS_WRENCH).define('h', GT6ItemTags.TOOLS_HARD_HAMMER);
			if (tSpec[2].indexOf('d') >= 0) tBuilder.define('d', GT6ItemTags.TOOLS_SCREWDRIVER); // the saw row, no phantom keys
			if (tSpec[2].indexOf('f') >= 0) tBuilder.define('f', GT6ItemTags.TOOLS_FILE); // the cutter/drill rows
			if (tSpec[2].indexOf('D') >= 0) tBuilder.define('D', itemOrNull(gregapi.data.OP.dust, gregapi.data.MT.Diamond)); // the saw row
			tBuilder.unlockedBy("has_circuit3", has(tCircuit))
					.save(aOutput, new ResourceLocation(GT6DataGenerators.MOD_ID, "robot_tip/" + tKind));
		}
	}

	private void roboticsComponentRows(net.minecraft.data.recipes.RecipeOutput aOutput) {
		gregapi.oredict.OreDictMaterial[] tElectricT = componentElectricT();
		for (int i = 1; i <= 9; i++) {
			String tTier = gregtech6.items.GT6Emitters.TIER_TOKENS[i];
			Item tWire = wireItemByPath(componentWirePath(false, "annealed_copper", i)); // the wireGt multiplier face rides the GTWires block items (the upstream setTarget_ oredient isomorph, wire_<mat>_gtNN — task wiregt-prefix-item-retirement retired the prefix-item face; rows 1-3 ANY.Cu fold → the refined twin)
			Item tCable = wireItemByPath(roboticsCablePath(i));
			Item tPlate = itemOrNull(gregapi.data.OP.plateCurved, tElectricT[i]);
			Item tStick = itemOrNull(gregapi.data.OP.stick, tElectricT[i]);
			Item tMagnetic = switch (i) {
				case 1 -> itemOrNull(gregapi.data.OP.stick, gregapi.data.MT.IronMagnetic);
				case 2, 3 -> itemOrNull(gregapi.data.OP.stick, gregapi.data.MT.SteelMagnetic);
				case 4, 5 -> itemOrNull(gregapi.data.OP.stick, gregapi.data.MT.NeodymiumMagnetic);
				default -> itemOrNull(gregapi.data.OP.stickLong, gregapi.data.MT.NeodymiumMagnetic);
			};
			if (tWire == null || tCable == null || tPlate == null || tStick == null || tMagnetic == null) continue; // the silent-skip guard
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.MOTORS.get(i).get())
					.pattern("CWR").pattern("WIW").pattern("PWC")
					.define('C', tCable).define('W', tWire).define('R', tStick).define('I', tMagnetic).define('P', tPlate)
					.unlockedBy("has_wire", has(tWire))
					.save(aOutput, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/motor_" + tTier));
			if (i == 1) { // the :407 DEF twin — the SteelMagnetic stick variant
				Item tSteelMag = itemOrNull(gregapi.data.OP.stick, gregapi.data.MT.SteelMagnetic);
				if (tSteelMag != null) {
					ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.MOTORS.get(1).get())
							.pattern("CWR").pattern("WIW").pattern("PWC")
							.define('C', tCable).define('W', tWire).define('R', tStick).define('I', tSteelMag).define('P', tPlate)
							.unlockedBy("has_wire", has(tWire))
							.save(aOutput, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/motor_lv_steel_magnetic"));
				}
			}
		}
		for (int i = 0; i < 10; i++) { // the :418-421 loop — PUMPS stays pooled (not this card's prefixes)
			String tTier = gregtech6.items.GT6Emitters.TIER_TOKENS[i];
			Item tMotor = gregtech6.registry.GT6Robotics.MOTORS.get(i).get();
			Item tCable = wireItemByPath(roboticsCablePath(i));
			Item tPlate = itemOrNull(gregapi.data.OP.plate, tElectricT[i]);
			Item tStick = itemOrNull(gregapi.data.OP.stick, tElectricT[i]);
			if (tMotor == null || tCable == null || tPlate == null || tStick == null) continue; // the silent-skip guard
			Item tRubberPlate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Rubber); // ANY.Rubber = MT.Rubber only (ANY.java:144)
			if (tRubberPlate != null) {
				ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.CONVEYERS.get(i).get())
						.pattern("RRR").pattern("MCM").pattern("RRR")
						.define('R', tRubberPlate).define('M', tMotor).define('C', tCable)
						.unlockedBy("has_motor", has(tMotor))
						.save(aOutput, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/conveyor_" + tTier));
			}
			Item tScrew = itemOrNull(gregapi.data.OP.screw, tElectricT[i]);
			Item tGear = itemOrNull(gregapi.data.OP.gearGtSmall, tElectricT[i]);
			if (tScrew != null && tGear != null) {
				ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.PISTONS.get(i).get())
						.pattern("TPP").pattern("dSS").pattern("TMG")
						.define('T', tScrew).define('P', tPlate).define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
						.define('S', tStick).define('M', tMotor).define('G', tGear)
						.unlockedBy("has_motor", has(tMotor))
						.save(aOutput, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/piston_" + tTier));
			}
			if (i <= 6) {
				TagKey<Item> tCircuit = GT6ItemTags.gt6("circuit" + i);
				ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GT6Robotics.ROBOT_ARMS.get(i).get())
						.pattern("CCC").pattern("MSM").pattern("PES")
						.define('C', tCable).define('M', tMotor).define('S', tStick)
						.define('P', gregtech6.registry.GT6Robotics.PISTONS.get(i).get()).define('E', tCircuit)
						.unlockedBy("has_circuit" + i, has(tCircuit))
						.save(aOutput, new ResourceLocation(GT6DataGenerators.MOD_ID, "component/robot_arm_" + tTier));
			}
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

	// ------------------------------------------------------------------------
	// The arg-8 direct-craft band (task tool-arg8-nine-families) — the nine tool
	// families whose upstream crafting face is the OreProcessing_Tool mToolRecipes
	// DIRECT rows (Loader_Tools.java:298 universal spade, :312-313 bending cylinders,
	// :315 plunger, :320 scoop, :325 branch cutter, :326 scissors, :329 club, :330 hand
	// drill) plus the rolling-pin metal/plastic ladder (:250-253, the non-listener
	// fixed-material loop). The w5-era single-steel anchors RETIRED — the stamped row
	// IS the steel row (the blade-family ruling), and every material crafts its own
	// tool (the audit's EMI dimension: one row per material, not one tag-folded
	// anchor). ONE gt6:material_tool row per (form x axis material), the upstream
	// shapes VERBATIM over the letter alphabet (Loader_Tools.java:455-472: I = ingot,
	// P = plate, G = gem, R = stone, S = stick, H = stick of the HANDLE material (the
	// useNormalHandle rows :329/:330 — the handle-material stick item when generated,
	// else the wooden-rod tag: the r7-39 assembly-band declared relaxation, the modern
	// material set carries no wood-family prefix items), T = screw, O = ring, A = the
	// listening prefix item (the universal spade's toolHeadUniversalSpade), X/Y = the
	// row specials (the hand drill's toolHeadArrow/bolt); the CR.java:193-217 lowercase
	// TOOL letters s/f/h/d/x ride the tool tags). The axis gates are the upstream And() rows
	// verbatim through the M5 identity form ({@link #woodExcluded}): the mToolTypes>0
	// listener gate (:426), typemin, the hand drill's qualmin(2), ANTIMATTER.NOT,
	// MT.Wood.NOT, the hand drill's WOOD.NOT tag gate, the scissors/branch-cutter/
	// hand-drill BOUNCY.NOT/STRETCHY.NOT. Fixed letters: the scoop 'V' = the wool tag
	// (the anchor precedent — the vanilla tag rides both legs, the 21.1 neoforge
	// Tags.Items has no WOOL field), the plunger 'V' = the rubber plate item (the
	// ONLY_IF_HAS face — no registered rubber plate, no plunger rows). DECLARED
	// zero-row variant: the club stone shape (:329 third pattern) rides 'R' =
	// OP.stone.dat — the port stone prefix carries no items (the rockGt card's
	// finding), so the walk emits nothing for it. The rolling-pin ladder walks the
	// upstream fixed list (Syrmorite/Au/Al/Cr/StainlessSteel/Netherite/
	// NetherizedDiamond) + the ANY.Plastic family; the WOOD-route rolling-pin anchor
	// (rollingPinBuilder, Loader_Recipes_Woods.java:237) is a DIFFERENT upstream loader
	// row and stays.
	// ------------------------------------------------------------------------

	/** One arg-8 form: the id, the axis gates, the shape, the fixed TAG letters (static-safe; the plunger's rubber-plate item resolves at emission). */
	record ToolArg8Form(String aId, int aTypeMin, int aQualMin, boolean aNoBouncy, boolean aNoStretchy,
			boolean aNoWoodTag, String[] aPattern, java.util.Map<Character, TagKey<Item>> aFixedTags) {}

	/** The :298-:330 direct rows + the club gem/stone variants — upstream order, shapes verbatim. (package-private: the offline census walk) */
	static final java.util.List<ToolArg8Form> TOOL_ARG8_FORMS = java.util.List.of(
			new ToolArg8Form("universal_spade", 2, 0, false, false, false, new String[] {"AT", "Sd"}, null), // :298 — 'A' = the toolHeadUniversalSpade item (the HEAD-consuming direct row)
			new ToolArg8Form("bending_cylinder", 2, 0, false, false, false, new String[] {"sfh", "III", "III"}, null), // :312
			new ToolArg8Form("bending_cylinder_small", 2, 0, false, false, false, new String[] {"sfh", "III"}, null), // :313
			new ToolArg8Form("plunger", 0, 0, false, false, false, new String[] {"xVV", " SV", "S f"}, null), // :315 — 'V' = the rubber plate (emission-resolved)
			new ToolArg8Form("scoop", 0, 0, false, false, false, new String[] {"SVS", "SSS", "xSh"}, java.util.Map.of('V', ItemTags.WOOL)), // :320 — 'V' = the wool tag
			new ToolArg8Form("branch_cutter", 2, 0, true, true, false, new String[] {"PfP", "PdP", "STS"}, null), // :325
			new ToolArg8Form("scissors", 2, 0, true, true, false, new String[] {"PfP", " T ", "OdO"}, null), // :326
			new ToolArg8Form("club", 0, 0, false, false, false, new String[] {" II", "III", "HI "}, null), // :329 ingot variant — 'H' = the handle stick
			new ToolArg8Form("club_gem", 0, 0, false, false, false, new String[] {" GG", "GGG", "HG "}, null), // :329 gem variant
			new ToolArg8Form("club_stone", 0, 0, false, false, false, new String[] {" RR", "RRR", "HR "}, null), // :329 stone variant — the DECLARED zero-row 'R' face
			new ToolArg8Form("hand_drill", 2, 2, true, true, true, new String[] {"  X", "HYH", "YH "}, null)); // :330 — 'X' = the arrow head, 'Y' = the bolt

	/** The :250-253 metal/plastic rolling-pin row — no listener axis, walked over {@link #rollingPinMaterials}. */
	static final ToolArg8Form ROLLING_PIN_FORM = new ToolArg8Form("rolling_pin", 0, 0, false, false, false,
			new String[] {"  S", " I ", "S f"}, null);

	/**
	 * The :250-253 fixed list + the ANY.Plastic family (the alias-merged dedup walk, the
	 * MATERIAL_ARRAY convention). (package-private: the offline census walk)
	 */
	static List<OreDictMaterial> rollingPinMaterials() {
		java.util.LinkedHashMap<String, OreDictMaterial> rRows = new java.util.LinkedHashMap<>();
		for (OreDictMaterial tMaterial : new OreDictMaterial[] {gregapi.data.MT.Syrmorite, gregapi.data.MT.Au, gregapi.data.MT.Al,
				gregapi.data.MT.Cr, gregapi.data.MT.StainlessSteel, gregapi.data.MT.Netherite, gregapi.data.MT.NetherizedDiamond}) {
			addRollingPinMaterial(rRows, tMaterial);
		}
		for (OreDictMaterial tMaterial : gregapi.data.ANY.Plastic.mToThis) {
			addRollingPinMaterial(rRows, tMaterial);
		}
		return new ArrayList<>(rRows.values());
	}

	private static void addRollingPinMaterial(java.util.LinkedHashMap<String, OreDictMaterial> aRows, OreDictMaterial aMaterial) {
		if (aMaterial == null || aMaterial.mID < 0) return;
		OreDictMaterial tMerged = MaterialRegistry.INSTANCE.get(aMaterial); // the alias merge
		if (tMerged == null || tMerged.mID < 0) return;
		aRows.putIfAbsent(tMerged.mNameInternal, tMerged);
	}

	/** The per-form axis gate — the upstream And() rows verbatim (see the band javadoc). (package-private: the offline census walk) */
	static boolean toolArg8Axis(OreDictMaterial aMaterial, ToolArg8Form aForm) {
		if (aMaterial.mToolTypes <= 0 || aMaterial.mToolTypes < aForm.aTypeMin()) return false; // the :426 listener gate + typemin
		if (aMaterial.mToolQuality < aForm.aQualMin()) return false; // qualmin
		if (aMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) return false; // ANTIMATTER.NOT
		if (woodExcluded(aMaterial)) return false; // MT.Wood.NOT — the M5 identity form
		if (aForm.aNoWoodTag() && aMaterial.contains(gregapi.data.TD.Properties.WOOD)) return false; // the :330 WOOD.NOT tag gate
		if (aForm.aNoBouncy() && aMaterial.contains(gregapi.data.TD.Properties.BOUNCY)) return false; // BOUNCY.NOT
		if (aForm.aNoStretchy() && aMaterial.contains(gregapi.data.TD.Properties.STRETCHY)) return false; // STRETCHY.NOT
		return true;
	}

	/** The result item of an arg-8 form (the GT6Tools registry face; datagen JVM only). */
	private static Item toolArg8Result(String aForm) {
		return switch (aForm) {
			case "universal_spade" -> GT6Tools.UNIVERSAL_SPADE.get();
			case "bending_cylinder" -> GT6Tools.BENDING_CYLINDER.get();
			case "bending_cylinder_small" -> GT6Tools.BENDING_CYLINDER_SMALL.get();
			case "plunger" -> GT6Tools.PLUNGER.get();
			case "scoop" -> GT6Tools.SCOOP.get();
			case "branch_cutter" -> GT6Tools.BRANCH_CUTTER.get();
			case "scissors" -> GT6Tools.SCISSORS.get();
			case "club", "club_gem", "club_stone" -> GT6Tools.CLUB.get();
			case "hand_drill" -> GT6Tools.HAND_DRILL.get();
			case "rolling_pin" -> GT6Tools.ROLLING_PIN.get();
			default -> throw new IllegalArgumentException("unknown arg-8 form: " + aForm);
		};
	}

	/** The prefix-carrying letters (S/T/O/A/X/Y — 'H' rides the HANDLE material column, resolved by the caller). Null = not a prefix letter. */
	private static gregapi.oredict.OreDictPrefix toolArg8Prefix(char aKey) {
		return switch (aKey) {
			case 'S' -> gregapi.data.OP.stick;
			case 'T' -> gregapi.data.OP.screw;
			case 'O' -> gregapi.data.OP.ring;
			case 'A' -> gregapi.data.OP.toolHeadUniversalSpade;
			case 'X' -> gregapi.data.OP.toolHeadArrow;
			case 'Y' -> gregapi.data.OP.bolt;
			default -> null;
		};
	}

	/**
	 * The row letters (the Loader_Tools.java:455-472 alphabet + the CR.java:193-217 TOOL
	 * letters). {@code null} = the item-truth miss — the row is skipped, never emitted
	 * with an unresolvable ingredient.
	 */
	private static net.minecraft.world.item.crafting.Ingredient toolArg8Ingredient(char aKey, ToolArg8Form aForm,
			OreDictMaterial aMaterial, Item aRubberPlate) {
		String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(aMaterial.mNameInternal);
		switch (aKey) {
			case 'P': return gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plate, aMaterial) == null ? null
					: net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake));
			case 'I': return gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.ingot, aMaterial) == null ? null
					: net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, tSnake));
			case 'G': return gregapi.data.OP.gem.isGeneratingItem(aMaterial)
					? net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, tSnake)) : null;
			case 'R': return null; // the OP.stone prefix carries no port items — the declared zero-row variant
			case 'H': {
				// the 'H' letter = stick.dat(mHandleMaterial) — the item when the port
				// generates the handle-material stick (the self-handle materials, upstream
				// verbatim), otherwise the wooden-rod tag (the r7-39 assembly-band declared
				// relaxation: wood-handled metals demand a wood stick the modern material set
				// carries no prefix item for)
				if (gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.stick, aMaterial.mHandleMaterial) != null) {
					return net.minecraft.world.item.crafting.Ingredient.of(gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.stick, aMaterial.mHandleMaterial).get());
				}
				return net.minecraft.world.item.crafting.Ingredient.of(Tags.Items.RODS_WOODEN);
			}
			case 'V': {
				TagKey<Item> tTag = aForm.aFixedTags() == null ? null : aForm.aFixedTags().get('V');
				if (tTag != null) return net.minecraft.world.item.crafting.Ingredient.of(tTag);
				return aRubberPlate == null ? null : net.minecraft.world.item.crafting.Ingredient.of(aRubberPlate); // the ONLY_IF_HAS face
			}
			case 's': return net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_SAW);
			case 'f': return net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_FILE);
			case 'h': return net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_HARD_HAMMER);
			case 'd': return net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_SCREWDRIVER);
			case 'x': return net.minecraft.world.item.crafting.Ingredient.of(GT6ItemTags.TOOLS_WIRE_CUTTER);
			default: {
				gregapi.oredict.OreDictPrefix tPrefix = toolArg8Prefix(aKey);
				if (tPrefix == null) throw new IllegalArgumentException("unknown arg-8 letter: " + aKey);
				if (gregtech6.registry.GTMaterialItems.get(tPrefix, aMaterial) == null) return null;
				return net.minecraft.world.item.crafting.Ingredient.of(gregtech6.registry.GTMaterialItems.get(tPrefix, aMaterial).get());
			}
		}
	}

	/** The advancement anchor — the plates/ingots/gems tag when carried, else the first prefix item (resolvability ran first). Leg-agnostic value. */
	private static Object toolArg8Criterion(ToolArg8Form aForm, OreDictMaterial aMaterial) {
		java.util.Set<Character> tLetters = new java.util.HashSet<>();
		for (String tRow : aForm.aPattern()) for (char tChar : tRow.toCharArray()) if (tChar != ' ') tLetters.add(tChar);
		String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(aMaterial.mNameInternal);
		if (tLetters.contains('P')) return GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, tSnake);
		if (tLetters.contains('I')) return GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, tSnake);
		if (tLetters.contains('G')) return GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, tSnake);
		for (char tKey : new char[] {'S', 'T', 'O', 'A', 'X', 'Y'}) {
			if (!tLetters.contains(tKey)) continue;
			gregapi.oredict.OreDictPrefix tPrefix = toolArg8Prefix(tKey);
			if (tPrefix != null && gregtech6.registry.GTMaterialItems.get(tPrefix, aMaterial) != null) {
				return gregtech6.registry.GTMaterialItems.get(tPrefix, aMaterial).get();
			}
		}
		throw new IllegalArgumentException("no material-carrying letter to anchor the advancement: " + aForm.aId());
	}

//? if forge {
	private void toolArg8Rows(Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		Item tRubberPlate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Rubber); // the :315 'V' special — the ONLY_IF_HAS face
		java.util.Set<String> tSeen = new java.util.HashSet<>();
		for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0) continue;
			tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // the alias merge
			if (tMaterial == null || tMaterial.mID < 0 || !tSeen.add(tMaterial.mNameInternal)) continue;
			for (ToolArg8Form tForm : TOOL_ARG8_FORMS) {
				if (!toolArg8Axis(tMaterial, tForm)) continue;
				toolArg8Row(aConsumer, tForm, tMaterial, tRubberPlate);
			}
		}
		for (OreDictMaterial tMaterial : rollingPinMaterials()) { // the :250-253 ladder — the fixed loop, no axis
			toolArg8Row(aConsumer, ROLLING_PIN_FORM, tMaterial, null);
		}
	}

	private void toolArg8Row(Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer, ToolArg8Form tForm,
			OreDictMaterial tMaterial, @javax.annotation.Nullable Item tRubberPlate) {
		java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
		java.util.List<String> tPattern = new ArrayList<>();
		for (String tRow : tForm.aPattern()) {
			tPattern.add(tRow);
			for (char tChar : tRow.toCharArray()) {
				if (tChar == ' ' || tKey.containsKey(tChar)) continue;
				net.minecraft.world.item.crafting.Ingredient tIngredient = toolArg8Ingredient(tChar, tForm, tMaterial, tRubberPlate);
				if (tIngredient == null) return; // the item-truth miss — no row (never an unresolvable ingredient)
				tKey.put(tChar, tIngredient);
			}
		}
		String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(tMaterial.mNameInternal);
		ResourceLocation tId = digLadderRowId(tForm.aId(), tSnake);
		Object tAnchor = toolArg8Criterion(tForm, tMaterial);
		net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
				.recipeAdvancement()
				.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
				.addCriterion("has_head_material", tAnchor instanceof TagKey ? has((TagKey<Item>) tAnchor) : has((Item) tAnchor))
				.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
				.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
				.requirements(net.minecraft.advancements.RequirementsStrategy.OR);
		aConsumer.accept(new MaterialToolRow(tId, tId.withPrefix("recipes/tools/"),
				net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT, tPattern, tKey,
				toolArg8Result(tForm.aId()), tSnake, tAdvancement));
	}

//?} else {
/*	private void toolArg8Rows(net.minecraft.data.recipes.RecipeOutput aOutput) {
		Item tRubberPlate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Rubber); // the :315 'V' special — the ONLY_IF_HAS face
		java.util.Set<String> tSeen = new java.util.HashSet<>();
		for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0) continue;
			tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // the alias merge
			if (tMaterial == null || tMaterial.mID < 0 || !tSeen.add(tMaterial.mNameInternal)) continue;
			for (ToolArg8Form tForm : TOOL_ARG8_FORMS) {
				if (!toolArg8Axis(tMaterial, tForm)) continue;
				toolArg8Row(aOutput, tForm, tMaterial, tRubberPlate);
			}
		}
		for (OreDictMaterial tMaterial : rollingPinMaterials()) { // the :250-253 ladder — the fixed loop, no axis
			toolArg8Row(aOutput, ROLLING_PIN_FORM, tMaterial, null);
		}
	}

	private void toolArg8Row(net.minecraft.data.recipes.RecipeOutput aOutput, ToolArg8Form tForm,
			OreDictMaterial tMaterial, @javax.annotation.Nullable Item tRubberPlate) {
		java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = new java.util.LinkedHashMap<>();
		java.util.List<String> tPattern = new ArrayList<>();
		for (String tRow : tForm.aPattern()) {
			tPattern.add(tRow);
			for (char tChar : tRow.toCharArray()) {
				if (tChar == ' ' || tKey.containsKey(tChar)) continue;
				net.minecraft.world.item.crafting.Ingredient tIngredient = toolArg8Ingredient(tChar, tForm, tMaterial, tRubberPlate);
				if (tIngredient == null) return; // the item-truth miss — no row
				tKey.put(tChar, tIngredient);
			}
		}
		String tSnake = gregtech6.registry.GTMaterialItems.snakeCase(tMaterial.mNameInternal);
		ResourceLocation tId = digLadderRowId(tForm.aId(), tSnake);
		Object tAnchor = toolArg8Criterion(tForm, tMaterial);
		net.minecraft.advancements.Advancement.Builder tAdvancement = net.minecraft.advancements.Advancement.Builder
				.recipeAdvancement()
				.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
				.addCriterion("has_head_material", tAnchor instanceof TagKey ? has((TagKey<Item>) tAnchor) : has((Item) tAnchor))
				.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
				.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
				.requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR);
		gregtech6.items.tools.GT6MaterialToolRecipe tRecipe = new gregtech6.items.tools.GT6MaterialToolRecipe("",
				net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
				net.minecraft.world.item.crafting.ShapedRecipePattern.of(tKey, tPattern),
				new ItemStack(toolArg8Result(tForm.aId())), true, tSnake);
		aOutput.accept(tId, tRecipe, tAdvancement.build(tId.withPrefix("recipes/tools/")));
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
	 * registrationOrder membership = the head item truth) minus the :333 soft-tag gate and
	 * the MT.Empty blank-head gate (AdvancedCraftingTool.java:68 — every ACT row rides the
	 * same listener, the blank heads are retip/recycle bases, never assembly heads; the
	 * toolhead-family-closeout :621 force made the pickaxeGem blank real, this gate keeps
	 * it out of the _from_head bands).
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
				if (tMaterial == gregapi.data.MT.Empty) continue; // AdvancedCraftingTool.java:68 — the blank head assembles nothing
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
	// wire-cutter tag (upstream CR.java:359). THE :169 ROWS POUR ZERO AGAIN — the
	// wireGt01 output face is the retired prefix-item face (task wiregt-prefix-item-retirement
	// reverted the wire-gt-registration item-path lift: upstream ships the multipliers ONLY
	// on the MTE block path, MultiTileEntityWireElectric.java:72-87, and the wireGtXX
	// oredient face rides the GTWires block items wire_<mat>_gtNN — the row is upstream's
	// own item-domain template and resolves to nothing port-side; the block-domain machine
	// face stays pooled, the GT6RecipesWiremill seam verbatim).
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
	// casingMachine+Double/Quadruple/Dense and cableGt01/02 carry no
	// MaterialPrefixItems (upstream Loader_Items.java:57-171 never built a
	// PrefixItem for them — the MTE-block domains), so their forms stay in
	// the tables and the rows unlock with those item families. plank LEFT
	// this zero-row band with task wood-planks-register (the WOOD-gated
	// item-path adaptation, the :151 cut pours).
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
	// live since task wood-planks-register — the plank WOOD domain minus the
	// identity wood rides the 2x2 cut).

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

	/**
	 * The material face of the gear band: the three-face intersection under the per-form
	 * condition tails (:149-151 verbatim); the stone face rides the family (OP.stone is
	 * block-path, outside the item walk — the rockGt seam). The plank face (task
	 * planks-blockification): OP.plank left the item path with the prefix-item retirement,
	 * so its truth leg is the wooddict plank rows ({@link GT6WoodDict} — the upstream
	 * walk is over the WoodDictionary planks, Loader_OreProcessing.java:151 rides the same
	 * oredict population).
	 */
	static List<GearGtCraftFromMaterialRow> gearGtCraftFromMaterialRows() {
		List<GearGtCraftFromMaterialRow> rRows = new ArrayList<>();
		for (GearGtCraftFromForm tForm : gearGtCraftFromForms()) {
			List<gregapi.oredict.OreDictPrefix> tFaces = new ArrayList<>();
			boolean tPlankFace = tForm.aInput() == gregapi.data.OP.plank; // the wooddict truth leg (task planks-blockification)
			if (!tPlankFace) {
				tFaces.add(tForm.aInput());
				if (tForm.aCompanion() != null && !tForm.aStoneFace()) tFaces.add(tForm.aCompanion()); // the plate third face; the stone face is block-path
			}
			java.util.Set<OreDictMaterial> tInputs = itemTruth(tFaces);
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput()) continue; // the output family
				// the input-truth intersection: the plank form keys on the wooddict face (the
				// mID-stable hasPlank — the test JVMs re-run MT.init across suites), the rest
				// on the item-truth set
				if (!(tPlankFace ? gregtech6.registry.GT6WoodDict.hasPlank(tMaterial) : tInputs.contains(tMaterial))) continue;
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
			// the plank input rides the wooddict face (task planks-blockification — OP.plank
			// left the item path, the plank truth leg is the GT6WoodDict rows)
			Item tInput = tForm.aInput() == gregapi.data.OP.plank
					? gregtech6.registry.GT6WoodDict.plankOrNull(tMaterialRow.aMaterial())
					: itemOrNull(tForm.aInput(), tMaterialRow.aMaterial());
			Item tCompanion = null;
			if (tForm.aStoneFace()) {
				gregtech6.registry.GTStoneBlocks.StoneSpec tStoneFamily = stoneFamilyOrNull(tMaterialRow.aMaterial());
				if (tStoneFamily != null) tCompanion = gregtech6.registry.GTStoneBlocks.item(tStoneFamily.snake(), gregtech6.block.stone.StoneVariant.STONE).get();
			} else if (tForm.aCompanion() != null) {
				tCompanion = itemOrNull(tForm.aCompanion(), tMaterialRow.aMaterial());
			}
			// the companion guard only binds when the form CARRIES a companion (the :151 plank cut is
			// companion-less — a null check here used to silently swallow the whole form while its
			// material face sat at zero rows, the wood-planks-register unlock flushed it out)
			boolean tCarriesCompanion = tForm.aStoneFace() || tForm.aCompanion() != null;
			if (tResult == null || tInput == null || (tCarriesCompanion && tCompanion == null)) continue; // the item-truth guard (belt and braces over the walk)
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
	// The raw→finished tool-head FILE BELT (task toolhead-r11a-file-belt) —
	// the C2 conversion route of the r11 toolhead census: the OreProcessing_
	// CraftFrom listener half living in Loader_Recipes_Handlers.java:420-434
	// (the p37 panel census circled Loader_OreProcessing.java only — the
	// file-boundary scope miss; the sibling C1 grindstone rows :403-415 are
	// the toolhead-r11b card, this band is the workbench face only). Fifteen
	// shaped rows over ONE grid, {"X ", " f"} = the raw head (X) + a file
	// tool (f) → the finished head:
	//  - :420 arrow ← gemChipped ×2, And(ANTIMATTER.NOT, COATED.NOT);
	//  - :421 arrow ← rockGt ×8, And(ANTIMATTER.NOT, COATED.NOT, STONE);
	//  - :422 arrow ← rawArrow ×1, ANTIMATTER.NOT;
	//  - :423-434 the twelve one-row families (saw/chisel/sword/pickaxe/
	//    shovel/spade/universalSpade/axe/axeDouble/hoe/sense/plow), all
	//    ANTIMATTER.NOT.
	// The same band translation as the stick family above (the listener walk,
	// the condition fold, the config-gate drop, the item-truth intersection)
	// — the listener rides the OUTPUT prefix's registration event (the
	// aEvent.mStack face), so the universe = the materials whose OUTPUT and
	// INPUT items both exist; the raw heads carry their finished head's
	// condition verbatim (OP.java:1303-1315 setCondition(toolHead<Family>)),
	// so the raw ∩ finished faces coincide for the twelve families. Tool
	// letter 'f' = the file tool tag (upstream CR.java:231).
	// -----------------------------------------------------------------------

	/** The condition kind of the :421 rockGt arrow row — the STONE positive leg joins the coated/antimatter pair. */
	static final int COND_COATED_ANTIMATTER_STONE = 4;

	/** One file-belt row form: the id key + the output prefix + count + the input prefix + the condition kind (Loader_Recipes_Handlers.java:420-434, the amounts and conditions verbatim). Package-private for the pin test. */
	record FileBeltCraftFromForm(String aKey, gregapi.oredict.OreDictPrefix aOutput, int aCount,
			gregapi.oredict.OreDictPrefix aInput, int aCondition) {}

	/** The fifteen row forms (upstream order :420-434). A method, not a field — the OP fields live only after OP.init. */
	static List<FileBeltCraftFromForm> fileBeltCraftFromForms() {
		return List.of(
				new FileBeltCraftFromForm("tool_head_arrow/from_gem_chipped", gregapi.data.OP.toolHeadArrow, 2, gregapi.data.OP.gemChipped, COND_COATED_ANTIMATTER), // :420
				new FileBeltCraftFromForm("tool_head_arrow/from_rock_gt", gregapi.data.OP.toolHeadArrow, 8, gregapi.data.OP.rockGt, COND_COATED_ANTIMATTER_STONE), // :421
				new FileBeltCraftFromForm("tool_head_arrow/from_raw_arrow", gregapi.data.OP.toolHeadArrow, 1, gregapi.data.OP.toolHeadRawArrow, COND_ANTIMATTER), // :422
				new FileBeltCraftFromForm("tool_head_saw/from_raw_saw", gregapi.data.OP.toolHeadSaw, 1, gregapi.data.OP.toolHeadRawSaw, COND_ANTIMATTER), // :423
				new FileBeltCraftFromForm("tool_head_chisel/from_raw_chisel", gregapi.data.OP.toolHeadChisel, 1, gregapi.data.OP.toolHeadRawChisel, COND_ANTIMATTER), // :424
				new FileBeltCraftFromForm("tool_head_sword/from_raw_sword", gregapi.data.OP.toolHeadSword, 1, gregapi.data.OP.toolHeadRawSword, COND_ANTIMATTER), // :425
				new FileBeltCraftFromForm("tool_head_pickaxe/from_raw_pickaxe", gregapi.data.OP.toolHeadPickaxe, 1, gregapi.data.OP.toolHeadRawPickaxe, COND_ANTIMATTER), // :426
				new FileBeltCraftFromForm("tool_head_shovel/from_raw_shovel", gregapi.data.OP.toolHeadShovel, 1, gregapi.data.OP.toolHeadRawShovel, COND_ANTIMATTER), // :427
				new FileBeltCraftFromForm("tool_head_spade/from_raw_spade", gregapi.data.OP.toolHeadSpade, 1, gregapi.data.OP.toolHeadRawSpade, COND_ANTIMATTER), // :428
				new FileBeltCraftFromForm("tool_head_universal_spade/from_raw_universal_spade", gregapi.data.OP.toolHeadUniversalSpade, 1, gregapi.data.OP.toolHeadRawUniversalSpade, COND_ANTIMATTER), // :429
				new FileBeltCraftFromForm("tool_head_axe/from_raw_axe", gregapi.data.OP.toolHeadAxe, 1, gregapi.data.OP.toolHeadRawAxe, COND_ANTIMATTER), // :430
				new FileBeltCraftFromForm("tool_head_axe_double/from_raw_axe_double", gregapi.data.OP.toolHeadAxeDouble, 1, gregapi.data.OP.toolHeadRawAxeDouble, COND_ANTIMATTER), // :431
				new FileBeltCraftFromForm("tool_head_hoe/from_raw_hoe", gregapi.data.OP.toolHeadHoe, 1, gregapi.data.OP.toolHeadRawHoe, COND_ANTIMATTER), // :432
				new FileBeltCraftFromForm("tool_head_sense/from_raw_sense", gregapi.data.OP.toolHeadSense, 1, gregapi.data.OP.toolHeadRawSense, COND_ANTIMATTER), // :433
				new FileBeltCraftFromForm("tool_head_plow/from_raw_plow", gregapi.data.OP.toolHeadPlow, 1, gregapi.data.OP.toolHeadRawPlow, COND_ANTIMATTER)); // :434
	}

	/** The material face of one file-belt row (the test-visible walk unit). */
	record FileBeltCraftFromMaterialRow(FileBeltCraftFromForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/** The material face of the file belt: per form, the materials whose INPUT and OUTPUT items both exist (the registrationOrder intersection) minus the per-form condition rows — the stick walk + the shapeless condition switch. */
	static List<FileBeltCraftFromMaterialRow> fileBeltCraftFromMaterialRows() {
		List<FileBeltCraftFromMaterialRow> rRows = new ArrayList<>();
		for (FileBeltCraftFromForm tForm : fileBeltCraftFromForms()) {
			java.util.Set<OreDictMaterial> tInputs = itemTruth(List.of(tForm.aOutput(), tForm.aInput()));
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tInputs.contains(tMaterial)) continue; // the item-truth intersection
				switch (tForm.aCondition()) {
					case COND_COATED_ANTIMATTER -> {
						if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
						if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
					}
					case COND_COATED_ANTIMATTER_STONE -> {
						if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
						if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
						if (!tMaterial.contains(gregapi.data.TD.Properties.STONE)) continue; // STONE — the positive rock face
					}
					default -> {
						if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT — the twelve families + the raw arrow
					}
				}
				rRows.add(new FileBeltCraftFromMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** The datagen row: the id + the output item + count + the input item. */
	private record FileBeltCraftFromRow(ResourceLocation aId, net.minecraft.world.item.Item aResult, int aCount, net.minecraft.world.item.Item aInput) {}

	/** The datagen face: the material walk resolved onto the live items (the silent-skip guard rides itemOrNull). */
	private List<FileBeltCraftFromRow> fileBeltCraftFromDatagenRows() {
		List<FileBeltCraftFromRow> rRows = new ArrayList<>();
		for (FileBeltCraftFromMaterialRow tMaterialRow : fileBeltCraftFromMaterialRows()) {
			String tSnake = GTMaterialItems.snakeCase(tMaterialRow.aMaterial().mNameInternal);
			net.minecraft.world.item.Item tResult = itemOrNull(tMaterialRow.aForm().aOutput(), tMaterialRow.aMaterial());
			net.minecraft.world.item.Item tInput = itemOrNull(tMaterialRow.aForm().aInput(), tMaterialRow.aMaterial());
			if (tResult == null || tInput == null) continue; // the item-truth guard (belt and braces over the walk)
			rRows.add(new FileBeltCraftFromRow(craftFromRowId(tMaterialRow.aForm().aKey(), tSnake), tResult, tMaterialRow.aForm().aCount(), tInput));
		}
		return rRows;
	}

	/** One row's builder — the upstream {"X ", " f"} grid, 'X' = the raw head item, 'f' = the file tag (upstream CR.java:231). */
	private ShapedRecipeBuilder fileBeltCraftFromBuilder(FileBeltCraftFromRow aRow) {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aRow.aResult(), aRow.aCount())
				.pattern("X ")
				.pattern(" f")
				.define('X', aRow.aInput())
				.define('f', GT6ItemTags.TOOLS_FILE)
				.unlockedBy("has_input", has(aRow.aInput()));
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

	// -----------------------------------------------------------------------
	// task crafting-machines-steam-band — the steam-age bootstrap crafting band
	// (the recipe-bidirectional-census P1 master gate: the GTMachines machine
	// family carried ZERO vanilla-crafting rows, the "cannot be made" root of
	// the whole production chain). The machine grids are the upstream
	// registration recipe strings VERBATIM (Loader_MultiTileEntities.java);
	// the lowercase letters ride the CR tool mapping the tank-valve/circuit
	// bands pinned (h = hard hammer, w = wrench, d = screwdriver, x = wire
	// cutter). Declared folds and segment cuts:
	//   - ANY.Cu plate-double column → Copper (the cracker 'C' fold, :1600);
	//   - the dynamo wire columns → the fine_wires material tags (the count
	//     differential folded, the transformer/EU-bridge fold precedent);
	//   - the ULV dynamo row (upstream ships no VN[0] line) rides the LV
	//     column set over Electric_T[0] TinAlloy (the transformer T0 lock
	//     precedent);
	//   - burning boxes: the Brick row (:519) + the 13 Solid rows (:522-534) —
	//     the steam-age segment only; the Dense/Liquid/Gas/FluidBed ladders
	//     and the sifter/compressor ULV rungs ride the P1' follow-up band;
	//   - rollbender t1-t4 (:1355-1358): the machine channel behind the metal
	//     ring spectrum (rollbender.json stick→ring), the chain bottom.
	// A missing input item skips the row silently (the itemOrNull guard, the
	// tank-valve band semantics).
	// -----------------------------------------------------------------------

	/** The result-path recipe id of one machine row (the hopper result-path convention). */
	private ResourceLocation steamBandId(String aPath) {
		String tPath = aPath; // the local so the two-arg RL ctor args stay bare identifiers (the swap-table regex note)
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/** The steam-engine/burning-box material slug → the loader material (the dieselMaterial switch shape). */
	private static gregapi.oredict.OreDictMaterial steamBandMaterial(String aSlug) {
		return switch (aSlug) {
			case "lead" -> MT.Pb;
			case "tin_alloy" -> MT.TinAlloy;
			case "bronze" -> MT.Bronze;
			case "arsenic_copper" -> MT.ArsenicCopper;
			case "arsenic_bronze" -> MT.ArsenicBronze;
			case "brass" -> MT.Brass;
			case "invar" -> MT.Invar;
			case "iron_wood" -> MT.IronWood;
			case "steel" -> MT.Steel;
			case "fiery_steel" -> MT.FierySteel;
			case "chromium" -> MT.Cr;
			case "titanium" -> MT.Ti;
			case "tungsten" -> MT.W;
			case "tungstensteel" -> MT.TungstenSteel;
			default -> throw new IllegalStateException("no loader material for steam-band slug " + aSlug);
		};
	}

	/**
	 * The 28 Steam Engine rows — the :584-597 grid "PhP"/"SIS"/"PwP" VERBATIM. The Steam
	 * ladder 'P' = plateDouble + 'I' = springSmall (the :584 keys); the Strong ladder
	 * (:599-612) keeps the same grid over 'P' = plateDense + 'I' = spring (the :599 keys).
	 * plateDense itself carries no crafting row (the rollingmill blockSolid channel stays
	 * its route — the upstream machine-channel face, faithful).
	 */
	private java.util.List<PartFamilyRecipeRow> steamEngineRecipeBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (gregtech6.registry.GT6Kinetics.SteamEngineRow tRow : gregtech6.registry.GT6Kinetics.STEAM_ENGINES) {
			gregapi.oredict.OreDictMaterial tMat = steamBandMaterial(tRow.matSlug());
			Item tPlate = itemOrNull(tRow.strong() ? gregapi.data.OP.plateDense : gregapi.data.OP.plateDouble, tMat);
			Item tSpring = itemOrNull(tRow.strong() ? gregapi.data.OP.spring : gregapi.data.OP.springSmall, tMat);
			Item tStick = itemOrNull(gregapi.data.OP.stick, tMat);
			if (tPlate == null || tSpring == null || tStick == null) continue; // the absent-input skip
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
					gregtech6.registry.GT6Kinetics.STEAM_ENGINE_ITEMS.get(tRow.path()).get())
					.pattern("PhP").pattern("SIS").pattern("PwP")
					.define('P', tPlate).define('S', tStick).define('I', tSpring)
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER).define('w', GT6ItemTags.TOOLS_WRENCH)
					.unlockedBy("has_plate", has(tPlate)), steamBandId(tRow.path())));
		}
		return rRows;
	}

	/**
	 * The Burning Box steam-age segment — the Brick row (:519, "BBB"/"BBB"/"BFB" over
	 * brick ingots + the firestarter tag) plus the 13 Solid rows (:522-534, "PCP"/"PwP"/
	 * "BBB" over the row plates, 'C' = plateDouble(ANY.Cu) → Copper, 'B' = the vanilla
	 * bricks block). The Dense ladder skips (the plateQuintuple/plateDense columns ride
	 * the P1' band). The brick row's 'B' rides the VANILLA brick item — upstream
	 * OP.ingot.dat(MT.Brick) resolves through the oredict ingotBrick face to
	 * minecraft:brick (the mold_ingot_raw_from_brick bridge precedent); the port
	 * registers no gt6 brick ingot.
	 */
	private java.util.List<PartFamilyRecipeRow> burningBoxRecipeBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		gregtech6.registry.GT6BurningBoxes.BurningBoxRow tBrick = gregtech6.registry.GT6BurningBoxes.BRICK_ROW;
		{
			Item tBrickIngot = net.minecraft.world.item.Items.BRICK;
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
					gregtech6.registry.GT6BurningBoxes.ITEMS_BY_PATH.get(tBrick.path()).get())
					.pattern("BBB").pattern("BBB").pattern("BFB")
					.define('B', tBrickIngot)
					.define('F', GT6ItemTags.TOOLS_FLINT_AND_TINDER)
					.unlockedBy("has_brick", has(tBrickIngot)), steamBandId(tBrick.path())));
		}
		Item tCuDouble = itemOrNull(gregapi.data.OP.plateDouble, gregapi.data.MT.Cu); // the ANY.Cu fold
		if (tCuDouble == null) return rRows;
		for (gregtech6.registry.GT6BurningBoxes.BurningBoxRow tRow : gregtech6.registry.GT6BurningBoxes.SOLID_ROWS) {
			if (tRow.path().startsWith("dense_")) continue; // the Dense ladder → the P1' band
			Item tPlate = itemOrNull(gregapi.data.OP.plate, tRow.material().mat().get());
			if (tPlate == null) continue;
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
					gregtech6.registry.GT6BurningBoxes.ITEMS_BY_PATH.get(tRow.path()).get())
					.pattern("PCP").pattern("PwP").pattern("BBB")
					.define('P', tPlate).define('C', tCuDouble)
					.define('B', net.minecraft.world.level.block.Blocks.BRICKS)
					.define('w', GT6ItemTags.TOOLS_WRENCH)
					.unlockedBy("has_plate", has(tPlate)), steamBandId(tRow.path())));
		}
		return rRows;
	}

	/**
	 * The 6 Electric Dynamo rows — the :946-950 grid "TGT"/"CMC"/"TId" VERBATIM
	 * (T = screw, G = gearGt, M = casingMachineDouble, all Electric_T[tier]; I = the
	 * magnetic stickLong column IronMagnetic/SteelMagnetic/NeodymiumMagnetic per tier;
	 * C = the wire column folded to the fine_wires tags). The ULV row (tier 0, the
	 * declared port extension) rides the LV column set over TinAlloy.
	 */
	private java.util.List<PartFamilyRecipeRow> electricDynamoRecipeBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		TagKey<Item> tCuWires = GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY, MT.Copper);
		TagKey<Item> tAcWires = GT6ItemTags.materialTag(GT6ItemTags.FINE_WIRES_FAMILY, MT.AnnealedCopper);
		for (gregtech6.registry.GT6ElectricDynamos.ElectricRow tRow : gregtech6.registry.GT6ElectricDynamos.ROWS) {
			gregapi.oredict.OreDictMaterial tMat = gregtech6.registry.GT6ElectricDynamos.ELECTRIC_T_LADDER.get(tRow.tier()).get();
			gregapi.oredict.OreDictMaterial tMagnetic = tRow.tier() <= 1 ? gregapi.data.MT.IronMagnetic
					: tRow.tier() <= 3 ? gregapi.data.MT.SteelMagnetic : gregapi.data.MT.NeodymiumMagnetic;
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachineDouble, tMat);
			Item tScrew = itemOrNull(gregapi.data.OP.screw, tMat);
			Item tGear = itemOrNull(gregapi.data.OP.gearGt, tMat);
			Item tStickLong = itemOrNull(gregapi.data.OP.stickLong, tMagnetic);
			if (tCasing == null || tScrew == null || tGear == null || tStickLong == null) continue; // the absent-input skip
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, electricDynamoItem(tRow.tier()))
					.pattern("TGT").pattern("CMC").pattern("TId")
					.define('T', tScrew).define('G', tGear).define('M', tCasing)
					.define('C', tRow.tier() <= 2 ? tCuWires : tAcWires)
					.define('I', tStickLong)
					.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
					.unlockedBy("has_casing", has(tCasing)), steamBandId(tRow.path())));
		}
		return rRows;
	}

	/** The dynamo tier → the row item (the constants are flat RegistryObjects, no BY_PATH map). */
	private static Item electricDynamoItem(int aTier) {
		return switch (aTier) {
			case 0 -> gregtech6.registry.GT6ElectricDynamos.ELECTRIC_DYNAMO_ULV_ITEM.get();
			case 1 -> gregtech6.registry.GT6ElectricDynamos.ELECTRIC_DYNAMO_ITEM.get();
			case 2 -> gregtech6.registry.GT6ElectricDynamos.ELECTRIC_DYNAMO_T2_ITEM.get();
			case 3 -> gregtech6.registry.GT6ElectricDynamos.ELECTRIC_DYNAMO_T3_ITEM.get();
			case 4 -> gregtech6.registry.GT6ElectricDynamos.ELECTRIC_DYNAMO_T4_ITEM.get();
			default -> gregtech6.registry.GT6ElectricDynamos.ELECTRIC_DYNAMO_T5_ITEM.get();
		};
	}

	/** The 4 Sifter rows — the :1313-1315 grid "WxW"/"RMR"/"SwS" VERBATIM (M = casingMachineDouble, S = spring, W = wireFine, R = stick, x = wire cutter, w = wrench — the SwS row's lowercase letter). */
	private java.util.List<PartFamilyRecipeRow> sifterRecipeBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.SIFTER_ROWS) {
			gregapi.oredict.OreDictMaterial tMat = tRow.material().get();
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachineDouble, tMat);
			Item tSpring = itemOrNull(gregapi.data.OP.spring, tMat);
			Item tWire = itemOrNull(gregapi.data.OP.wireFine, tMat);
			Item tStick = itemOrNull(gregapi.data.OP.stick, tMat);
			if (tCasing == null || tSpring == null || tWire == null || tStick == null) continue; // the absent-input skip
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
					gregtech6.registry.GTMachines.SIFTER_ITEMS_BY_PATH.get(tRow.path()).get())
					.pattern("WxW").pattern("RMR").pattern("SwS")
					.define('M', tCasing).define('S', tSpring).define('W', tWire).define('R', tStick)
					.define('x', GT6ItemTags.TOOLS_WIRE_CUTTER).define('w', GT6ItemTags.TOOLS_WRENCH)
					.unlockedBy("has_casing", has(tCasing)), steamBandId(tRow.path())));
		}
		return rRows;
	}

	/** The 4 Compressor rows — the :1343-1346 grid "PPR"/"wMS" VERBATIM (P = plateQuintuple, S = spring, R = stick, M = casingMachineDouble, w = wrench). */
	private java.util.List<PartFamilyRecipeRow> compressorRecipeBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.COMPRESSOR_ROWS) {
			gregapi.oredict.OreDictMaterial tMat = tRow.material().get();
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachineDouble, tMat);
			Item tQuintuple = itemOrNull(gregapi.data.OP.plateQuintuple, tMat);
			Item tSpring = itemOrNull(gregapi.data.OP.spring, tMat);
			Item tStick = itemOrNull(gregapi.data.OP.stick, tMat);
			if (tCasing == null || tQuintuple == null || tSpring == null || tStick == null) continue; // the absent-input skip
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
					gregtech6.registry.GTMachines.COMPRESSOR_ITEMS_BY_PATH.get(tRow.path()).get())
					.pattern("PPR").pattern("wMS")
					.define('P', tQuintuple).define('S', tSpring).define('R', tStick).define('M', tCasing)
					.define('w', GT6ItemTags.TOOLS_WRENCH)
					.unlockedBy("has_casing", has(tCasing)), steamBandId(tRow.path())));
		}
		return rRows;
	}

	/** The 4 Roll Bender rows — the :1355-1358 grid "wS "/"GMG"/" Sh" VERBATIM (G = gearGt, S = gearGtSmall, M = casingMachineDouble). The machine channel behind the metal ring spectrum. */
	private java.util.List<PartFamilyRecipeRow> rollBenderRecipeBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.ROLL_BENDER_ROWS) {
			gregapi.oredict.OreDictMaterial tMat = tRow.material().get();
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachineDouble, tMat);
			Item tGear = itemOrNull(gregapi.data.OP.gearGt, tMat);
			Item tGearSmall = itemOrNull(gregapi.data.OP.gearGtSmall, tMat);
			if (tCasing == null || tGear == null || tGearSmall == null) continue; // the absent-input skip
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
					gregtech6.registry.GTMachines.ROLLBENDER_ITEMS_BY_PATH.get(tRow.path()).get())
					.pattern("wS ").pattern("GMG").pattern(" Sh")
					.define('G', tGear).define('S', tGearSmall).define('M', tCasing)
					.define('w', GT6ItemTags.TOOLS_WRENCH).define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
					.unlockedBy("has_casing", has(tCasing)), steamBandId(tRow.path())));
		}
		return rRows;
	}

	// -----------------------------------------------------------------------
	// task machines-crafting-pour — the non-steam machine-body crafting band
	// (the machine-port-prereq-census P1' card: the P1 steam band poured the
	// 60 steam-age machines + sifter/compressor/rollbender, and the cracker/
	// burner-mixer/molecular-scanner bands shipped their slices; every OTHER
	// GTMachines family's crafting-table housing frame stayed unpoured — the
	// production chains behind them dead). The grids are the ACTIVE upstream
	// registration rows VERBATIM (Loader_MultiTileEntities.java machines1
	// :1288 - machines4 :1657; the Molecular Scanner T1/T2/T4/T5 rows are
	// commented out upstream — only the T3 rung exists and it already shipped
	// in the debt-scanner-t3 band). Lowercase letters ride the CR tool mapping
	// (h/w/d/x), auto-defined off the pattern text. Declared folds and cuts:
	//   - ANY.Cu plateDouble → Copper; ANY.Diamond gem/plateGem/plateGemTiny/
	//     dust → Diamond (the ANY-material single-representative precedent);
	//   - casing columns ride the REAL casing_machine*/casing_small items (the
	//     task casing-machine-register deviation items — the cracker band's
	//     casingSmall fold predates that registration, the faithful face wins
	//     here, the P1 sifter casingMachineDouble precedent);
	//   - Blocks.brick_block → vanilla bricks, OD.blockGlassColorless → glass,
	//     OD.sandstone → sandstone, OD.craftingHardenedClay → terracotta (the
	//     vanilla-bridge precedents);
	//   - OD_CIRCUITS[t] → the #gt6:circuit<t> tags (the battery/usb 'C'
	//     column convention); MT.DATA.CABLES_01[t] → the cable_* items over
	//     the GT6Batteries.WIRE_TOKENS tier ladder (the usb-peripherals path
	//     walk); the fixed wire columns ride their exact wire paths (the
	//     wiregt-prefix-item-retirement face — the wire family items, not the
	//     retired prefix items);
	//   - IL.MOTORS/CONVEYERS/PISTONS/ROBOT_ARMS[t] → the GT6Robotics ladders;
	//     IL.FIELD_GENERATORS/EMITTERS/SENSORS[t] → the GT6Emitters paths;
	//     IL.Processor_Crystal_* → the gem tags (the molecularScannerRow fold);
	//   - the pipe columns (Roasting/Fermenter/Autoclave/Melter) → the ONE wood
	//     fluid pipe item (the cracker band's declared representative fold —
	//     the pipe prefixes stay off the port item path);
	//   - the Melter 'U' crucible → the port smeltery_ceramic item (the
	//     ceramic rung the port registers). The Smelter/Crystallisation 'U'
	//     crucibles (tungsten/graphite/tantalum-hafnium-carbide/quartz/
	//     iridium small crucibles) have NO port items — those two families
	//     stay CUT, the gap ledger, no item fabricated;
	//   - the Canner rows (IL.PUMPS absent), the Laser Welder rows (the yellow
	//     lens family pooled, GTMachines:2032 card ruling), the Lightning
	//     Processor rows ('X' = wireGt01 over ANY.Iron — the wire family has
	//     no iron rung, the GTWireSpecs 30-row verbatim list starts Sn/Pb, so
	//     the upstream input itself resolves to nothing) and the Nanoscale
	//     Fabricator (no port machine registration at all) stay CUT — the gap
	//     ledger. The port-native ULV rows (*_ULV_ROWS / ROLLINGMILL_ROWS,
	//     zero upstream grid) have no pour object either.
	// A missing input item skips the row silently (the itemOrNull guard, the
	// P1 band semantics). Result ids ride the block path (the steam-band id
	// convention).
	// -----------------------------------------------------------------------

	/** The 'h'/'w'/'d'/'x' tool letters of one grid, auto-defined off the pattern text (the CR tool mapping, the P1 band's letter set). */
	private static void defineToolLetters(ShapedRecipeBuilder aBuilder, String[] aPatterns) {
		String tLetters = String.join("", aPatterns);
		if (tLetters.indexOf('h') >= 0) aBuilder.define('h', GT6ItemTags.TOOLS_HARD_HAMMER);
		if (tLetters.indexOf('w') >= 0) aBuilder.define('w', GT6ItemTags.TOOLS_WRENCH);
		if (tLetters.indexOf('d') >= 0) aBuilder.define('d', GT6ItemTags.TOOLS_SCREWDRIVER);
		if (tLetters.indexOf('x') >= 0) aBuilder.define('x', GT6ItemTags.TOOLS_WIRE_CUTTER);
	}

	/** One family's key resolver: defines the row's columns, {@code false} = a column item is absent (the row skips). */
	private interface PourKeys { boolean define(ShapedRecipeBuilder aBuilder, gregtech6.block.GTBasicMachineBlock.MachineRow aRow); }

	/**
	 * One poured machine family: the row list walked in order, the grid verbatim,
	 * the id off the row path (the steam-band convention). The family items map is
	 * the registration map the GTMachines static block filled from the same rows,
	 * so a null handle is a datagen-order bug, not a data state — the guard stays.
	 */
	private java.util.List<PartFamilyRecipeRow> machinePour(java.util.Map<String, ? extends java.util.function.Supplier<Item>> aItems,
			java.util.List<gregtech6.block.GTBasicMachineBlock.MachineRow> aRows, String[] aPatterns, PourKeys aKeys) {
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : aRows) {
			java.util.function.Supplier<Item> tResult = aItems.get(tRow.path());
			if (tResult == null) continue; // the unregistered-run guard
			ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tResult.get());
			for (String tPattern : aPatterns) tBuilder.pattern(tPattern);
			defineToolLetters(tBuilder, aPatterns);
			if (!aKeys.define(tBuilder, tRow)) continue; // the absent-column skip
			rRows.add(new PartFamilyRecipeRow(tBuilder, steamBandId(tRow.path())));
		}
		return rRows;
	}

	/** The Kinetic_T ladder materials in rung order (the row-list material suppliers, lifted once). */
	private static gregapi.oredict.OreDictMaterial[] kineticLadder() {
		return gregtech6.registry.GTMachines.KINETIC_T_LADDER.stream().map(java.util.function.Supplier::get).toArray(gregapi.oredict.OreDictMaterial[]::new);
	}

	/** The Heat_T ladder materials in rung order (the OVEN_ROWS material suppliers, lifted once). */
	private static gregapi.oredict.OreDictMaterial[] heatLadder() {
		return gregtech6.registry.GTMachines.HEAT_T_LADDER.stream().map(java.util.function.Supplier::get).toArray(gregapi.oredict.OreDictMaterial[]::new);
	}

	/** The insulated 01-cable item of a 1-based tier rung (the CABLES_01 column, the WIRE_TOKENS ladder — [1] Sn .. [5] Pt, MT.java:3631-3637). */
	private Item cable01(int aTier) {
		return wireItemByPath("cable_" + GT6Batteries.WIRE_TOKENS[aTier] + "_gt01");
	}

	/** The component item of a 1-based tier rung (the GT6Emitters family paths — the molecularScannerRow F/X/Y face). */
	private static Item component(String aFamily, int aTier) {
		return gregtech6.items.GT6Emitters.ITEMS_BY_PATH.get(aFamily + "_" + gregtech6.items.GT6Emitters.TIER_TOKENS[aTier]).get();
	}

	/** The plate ladder prefix of a tier rung (T1 plate .. T5 plateQuintuple — the cracker plate-column shape, the freezer/cryo 'S' column). */
	private static gregapi.oredict.OreDictPrefix plateLadder(int aTier) {
		return switch (aTier) {
			case 0 -> gregapi.data.OP.plate;
			case 1 -> gregapi.data.OP.plateDouble;
			case 2 -> gregapi.data.OP.plateTriple;
			case 3 -> gregapi.data.OP.plateQuadruple;
			default -> gregapi.data.OP.plateQuintuple;
		};
	}

	/** The whole band: every poured family, in the upstream machines1→machines4 file order (the pin test's walk order). */
	java.util.List<PartFamilyRecipeRow> machinePourBand() {
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		// -- machines1, the Kinetic_T housing frames (M = casingMachineDouble) ----
		rRows.addAll(shredderRecipeBuilders()); // :1294-1297
		rRows.addAll(crusherRecipeBuilders()); // :1300-1303
		rRows.addAll(latheRecipeBuilders()); // :1306-1309
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.BUZZSAW_ITEMS_BY_PATH, gregtech6.registry.GTMachines.BUZZSAW_ROWS, new String[] {"DGS", "wMS"}, (b, row) -> { // :1318-1321
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachineDouble, m), gearSmall = itemOrNull(gregapi.data.OP.gearGtSmall, m), dust = itemOrNull(gregapi.data.OP.dust, gregapi.data.MT.Diamond);
			Item head = row.tier() == 0 ? itemOrNull(gregapi.data.OP.toolHeadBuzzSaw, gregapi.data.MT.Steel) : itemOrNull(gregapi.data.OP.toolHeadBuzzSaw, gregapi.data.MT.CobaltBrass);
			if (casing == null || gearSmall == null || dust == null || head == null) return false;
			b.define('M', casing).define('S', gearSmall).define('D', dust).define('G', head).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.SQUEEZER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.SQUEEZER_ROWS, new String[] {"RS", "PM", "Pw"}, (b, row) -> { // :1324-1327
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachineDouble, m), plate = itemOrNull(gregapi.data.OP.plateTriple, m), spring = itemOrNull(gregapi.data.OP.spring, m), stick = itemOrNull(gregapi.data.OP.stick, m);
			if (casing == null || plate == null || spring == null || stick == null) return false;
			b.define('M', casing).define('P', plate).define('S', spring).define('R', stick).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.CENTRIFUGE_ITEMS_BY_PATH, gregtech6.registry.GTMachines.CENTRIFUGE_ROWS, new String[] {"Gw", "SM", "Gh"}, (b, row) -> { // :1330-1333
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachineDouble, m), gear = itemOrNull(gregapi.data.OP.gearGt, m), stickLong = itemOrNull(gregapi.data.OP.stickLong, m);
			if (casing == null || gear == null || stickLong == null) return false;
			b.define('M', casing).define('G', gear).define('S', stickLong).unlockedBy("has_casing", has(casing));
			return true;
		}));
		// -- machines1, the Electric rungs ---------------------------------------
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.ELECTROLYZER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.ELECTROLYZER_ROWS, new String[] {"SMS", "WwW"}, (b, row) -> { // :1336-1340
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get()), ptWire = wireItemByPath("wire_platinum_gt01");
			if (casing == null) return false;
			b.define('M', casing).define('S', ptWire).define('W', cable01(row.tier() + 1)).unlockedBy("has_casing", has(casing));
			return true;
		}));
		// -- machines1, the Kinetic_M runs (M = casingMachineDouble / single) -----
		rRows.addAll(rollingMillRecipeBuilders()); // :1349-1352 (the RU rows; the ULV rung is port-native, no upstream grid)
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.ROLLFORMER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.ROLL_FORMER_ROWS, new String[] {"wG ", "GMG", " Gh"}, (b, row) -> { // :1361-1364
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachineDouble, m), gear = itemOrNull(gregapi.data.OP.gearGt, m);
			if (casing == null || gear == null) return false;
			b.define('M', casing).define('G', gear).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.CLUSTERMILL_ITEMS_BY_PATH, gregtech6.registry.GTMachines.CLUSTER_MILL_ROWS, new String[] {"SSS", "wGh", "SMS"}, (b, row) -> { // :1367-1370
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachineQuadruple, m), gear = itemOrNull(gregapi.data.OP.gearGt, m), gearSmall = itemOrNull(gregapi.data.OP.gearGtSmall, m);
			if (casing == null || gear == null || gearSmall == null) return false;
			b.define('M', casing).define('G', gear).define('S', gearSmall).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.WIREMILL_ITEMS_BY_PATH, gregtech6.registry.GTMachines.WIREMILL_ROWS, new String[] {"SGS", "wMh"}, (b, row) -> { // :1373-1376 ('M' = casingMachine SINGLE)
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), gear = itemOrNull(gregapi.data.OP.gearGt, m), gearSmall = itemOrNull(gregapi.data.OP.gearGtSmall, m);
			if (casing == null || gear == null || gearSmall == null) return false;
			b.define('M', casing).define('G', gear).define('S', gearSmall).unlockedBy("has_casing", has(casing));
			return true;
		}));
		// -- machines1, the Canner CUT (IL.PUMPS absent) — the gap ledger --------
		rRows.addAll(ovenRecipeBuilders()); // :1288-1291 (the OvenRow walk — the OVEN family is the flat-constant shape, no MachineRow list)
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.ROASTING_ITEMS_BY_PATH, gregtech6.registry.GTMachines.ROASTING_ROWS, new String[] {"wPh", "PMP", "BCB"}, (b, row) -> { // :1386-1389 ('P' = pipeMedium → the wood-pipe fold)
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get()), cuDouble = itemOrNull(gregapi.data.OP.plateDouble, gregapi.data.MT.Cu);
			if (casing == null || cuDouble == null) return false;
			b.define('M', casing).define('P', gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM.get()).define('C', cuDouble)
					.define('B', net.minecraft.world.level.block.Blocks.BRICKS).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.MIXER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.MIXER_ROWS, new String[] {"PMP", "PRP", "hSw"}, (b, row) -> { // :1392-1395 ('P' = the FIXED plate(StainlessSteel) column)
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), stick = itemOrNull(gregapi.data.OP.stick, m), rotor = itemOrNull(gregapi.data.OP.rotor, gregapi.data.MT.StainlessSteel), plate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.StainlessSteel);
			if (casing == null || stick == null || rotor == null || plate == null) return false;
			b.define('P', plate).define('M', casing).define('R', rotor).define('S', stick).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(distilleryRecipeBuilders()); // :1398-1401 (the per-tier wire columns)
		rRows.addAll(extruderRecipeBuilders()); // :1406-1409 (the T1 Low-Heat rung's steel head)
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.LOOM_ITEMS_BY_PATH, gregtech6.registry.GTMachines.LOOM_ROWS, new String[] {"ShS", "GMG", "SwS"}, (b, row) -> { // :1412-1415 ('S' = stickLong, the raw :1412 key)
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), gear = itemOrNull(gregapi.data.OP.gearGt, m), stickLong = itemOrNull(gregapi.data.OP.stickLong, m);
			if (casing == null || gear == null || stickLong == null) return false;
			b.define('S', stickLong).define('G', gear).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		// -- machines2 ------------------------------------------------------------
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.POLARIZER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.POLARIZER_ROWS, new String[] {"TwT", "PMP", "TdT"}, (b, row) -> { // :1418-1422
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), plate = itemOrNull(gregapi.data.OP.plate, m), screw = itemOrNull(gregapi.data.OP.screw, m);
			if (casing == null || plate == null || screw == null) return false;
			b.define('T', screw).define('P', plate).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.PRESS_ITEMS_BY_PATH, gregtech6.registry.GTMachines.PRESS_ROWS, new String[] {"RS", "PM", "Pw"}, (b, row) -> { // :1425-1428
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), plate = itemOrNull(gregapi.data.OP.plateDouble, m), spring = itemOrNull(gregapi.data.OP.spring, m), stick = itemOrNull(gregapi.data.OP.stick, m);
			if (casing == null || plate == null || spring == null || stick == null) return false;
			b.define('R', stick).define('S', spring).define('P', plate).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		// -- the Smelter/Crystallisation CUT (the small crucible items absent) ----
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.INJECTOR_ITEMS_BY_PATH, gregtech6.registry.GTMachines.INJECTOR_ROWS, new String[] {"XPw", "CMW"}, (b, row) -> { // :1443-1447 ('P' = the pipeTiny→pipeHuge ladder → the wood-pipe fold)
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get());
			if (tCasing == null) return false;
			b.define('X', gregtech6.registry.GT6Robotics.PISTONS.get(row.tier() + 1).get()).define('C', GT6ItemTags.gt6("circuit" + (row.tier() + 1)))
					.define('W', cable01(row.tier() + 1)).define('M', tCasing)
					.define('P', gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM.get()).unlockedBy("has_casing", has(tCasing));
			return true;
		}));
		rRows.addAll(printerLikePour(gregtech6.registry.GTMachines.PRINTER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.PRINTER_ROWS,
				gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM.get())); // :1450-1454 ('P' = the pipeTiny ladder → the wood-pipe fold)
		rRows.addAll(printerLikePour(gregtech6.registry.GTMachines.SCANNER_VISUALS_ITEMS_BY_PATH, gregtech6.registry.GTMachines.SCANNER_VISUALS_ROWS,
				itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Lumium))); // :1457-1461 ('P' = the FIXED plate(Lumium), the raw :1457 key)
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.SLUICE_ITEMS_BY_PATH, gregtech6.registry.GTMachines.SLUICE_ROWS, new String[] {"PPP", "RGR", "GMG"}, (b, row) -> { // :1464-1467 ('P' = plateDouble(aMat), the raw keys)
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachineDouble, m), gear = itemOrNull(gregapi.data.OP.gearGt, m), rotor = itemOrNull(gregapi.data.OP.rotor, m), plate = itemOrNull(gregapi.data.OP.plateDouble, m);
			if (casing == null || gear == null || rotor == null || plate == null) return false;
			b.define('P', plate).define('R', rotor).define('G', gear).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.MAGNETIC_SEPARATOR_ITEMS_BY_PATH, gregtech6.registry.GTMachines.MAGNETIC_SEPARATOR_ROWS, new String[] {"TwT", "TdT", "PMP"}, (b, row) -> { // :1470-1474
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), plate = itemOrNull(gregapi.data.OP.plate, m), screw = itemOrNull(gregapi.data.OP.screw, m);
			if (casing == null || plate == null || screw == null) return false;
			b.define('T', screw).define('P', plate).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.DRYER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.DRYER_ROWS, new String[] {"wPh", "BMB", "BCB"}, (b, row) -> { // :1477-1480 ('P' = the pipeMedium(aMat) ladder → the wood-pipe fold)
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get()), cuDouble = itemOrNull(gregapi.data.OP.plateDouble, gregapi.data.MT.Cu);
			if (casing == null || cuDouble == null) return false;
			b.define('B', net.minecraft.world.level.block.Blocks.BRICKS).define('M', casing).define('C', cuDouble)
					.define('P', gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM.get()).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.LASER_ENGRAVER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.LASER_ENGRAVER_ROWS, new String[] {"TdT", "GPG", "CMC"}, (b, row) -> { // :1483-1487 ('P' = OD.craftingHardenedClay → terracotta, 'G' = gearGtSmall, the raw :1483 keys)
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), screw = itemOrNull(gregapi.data.OP.screw, m), gearSmall = itemOrNull(gregapi.data.OP.gearGtSmall, m);
			if (casing == null || screw == null || gearSmall == null) return false;
			b.define('T', screw).define('G', gearSmall).define('P', net.minecraft.world.level.block.Blocks.TERRACOTTA)
					.define('C', GT6ItemTags.gt6("circuit" + (row.tier() + 1))).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		// -- the Laser Welder CUT (the yellow lens family pooled) — the gap ledger -
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.AUTOCRAFTER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.AUTOCRAFTER_ROWS, new String[] {"WRW", "RwR", "CMC"}, (b, row) -> { // :1497-1501
			Item casing = itemOrNull(gregapi.data.OP.casingMachineDouble, row.material().get()), arm = gregtech6.registry.GT6Robotics.ROBOT_ARMS.get(row.tier() + 1).get(), cable = cable01(row.tier() + 1);
			b.define('W', cable).define('R', arm).define('C', GT6ItemTags.gt6("circuit" + (row.tier() + 1))).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.ELECTRIC_MIXER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.ELECTRIC_MIXER_ROWS, new String[] {"PMP", "PRP", "hSw"}, (b, row) -> { // :1504-1508 ('P' = the FIXED plate(StainlessSteel) column)
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), rotor = itemOrNull(gregapi.data.OP.rotor, gregapi.data.MT.StainlessSteel), plate = itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.StainlessSteel);
			if (casing == null || rotor == null || plate == null) return false;
			b.define('P', plate).define('M', casing).define('R', rotor).define('S', gregtech6.registry.GT6Robotics.MOTORS.get(row.tier() + 1).get()).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.ELECTRIC_LOOM_ITEMS_BY_PATH, gregtech6.registry.GTMachines.ELECTRIC_LOOM_ROWS, new String[] {"ShS", "GMG", "SwS"}, (b, row) -> { // :1511-1515 ('G' = the motor column, 'S' = stickLong, the raw :1511 keys)
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), stickLong = itemOrNull(gregapi.data.OP.stickLong, m);
			if (casing == null || stickLong == null) return false;
			b.define('S', stickLong).define('G', gregtech6.registry.GT6Robotics.MOTORS.get(row.tier() + 1).get()).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.ELECTRIC_SIFTER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.ELECTRIC_SIFTER_ROWS, new String[] {"WxW", "RMR", "SwS"}, (b, row) -> { // :1518-1522
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachineDouble, m), wire = itemOrNull(gregapi.data.OP.wireFine, m), stick = itemOrNull(gregapi.data.OP.stick, m);
			if (casing == null || wire == null || stick == null) return false;
			b.define('W', wire).define('R', stick).define('M', casing).define('S', gregtech6.registry.GT6Robotics.PISTONS.get(row.tier() + 1).get()).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.SLICER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.SLICER_ROWS, new String[] {"PRw", "YMC"}, (b, row) -> { // :1525-1529 ('R' = stick, the raw :1525 key)
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), stick = itemOrNull(gregapi.data.OP.stick, m);
			if (casing == null || stick == null) return false;
			b.define('P', gregtech6.registry.GT6Robotics.PISTONS.get(row.tier() + 1).get()).define('Y', gregtech6.registry.GT6Robotics.CONVEYERS.get(row.tier() + 1).get())
					.define('C', GT6ItemTags.gt6("circuit" + (row.tier() + 1))).define('R', stick).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.LAMINATOR_ITEMS_BY_PATH, gregtech6.registry.GTMachines.LAMINATOR_ROWS, new String[] {"SwS", "GMG", "SCS"}, (b, row) -> { // :1532-1535
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), gearSmall = itemOrNull(gregapi.data.OP.gearGtSmall, m), stick = itemOrNull(gregapi.data.OP.stick, m), cuDouble = itemOrNull(gregapi.data.OP.plateDouble, gregapi.data.MT.Cu);
			if (casing == null || gearSmall == null || stick == null || cuDouble == null) return false;
			b.define('S', stick).define('G', gearSmall).define('M', casing).define('C', cuDouble).unlockedBy("has_casing", has(casing));
			return true;
		}));
		// -- machines3, the exotic-energy/exotic rungs -----------------------------
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.MASSFAB_SMALL_ITEMS_BY_PATH, gregtech6.registry.GTMachines.MASSFAB_SMALL_ROWS, new String[] {"RFS", "FMF", "RFS"}, (b, row) -> { // :1542-1546
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get());
			if (casing == null) return false;
			b.define('R', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "ruby")).define('F', component("field_generator", 1))
					.define('S', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "sapphire")).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		// -- the Molecular Scanner family: only the T3 rung exists upstream and it shipped (debt-scanner-t3) --
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.REPLICATOR_ITEMS_BY_PATH, gregtech6.registry.GTMachines.REPLICATOR_ROWS, new String[] {"EXE", "FMF", "SXS"}, (b, row) -> { // :1556-1560
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get());
			if (casing == null) return false;
			b.define('E', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "emerald")).define('F', component("field_generator", 1))
					.define('S', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "sapphire")).define('X', component("signal_emitter", row.tier() + 1))
					.define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		// -- the Nanoscale Fabricator CUT (no port machine registration) -----------
		// -- the Lightning Processor CUT ('X' = wireGt01 over ANY.Iron, no iron wire rung) --
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.SANDING_ITEMS_BY_PATH, gregtech6.registry.GTMachines.SANDING_ROWS, new String[] {"SGS", "XXX", "wMh"}, (b, row) -> { // :1589-1592 ('X' = OD.sandstone → vanilla sandstone)
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachineDouble, m), gear = itemOrNull(gregapi.data.OP.gearGt, m), gearSmall = itemOrNull(gregapi.data.OP.gearGtSmall, m);
			if (casing == null || gear == null || gearSmall == null) return false;
			b.define('S', gearSmall).define('G', gear).define('X', net.minecraft.world.level.block.Blocks.SANDSTONE).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		// -- the Burner Mixer shipped (machines-burner-plantalyzer) ----------------
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.PLANTALYZER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.PLANTALYZER_ROWS, new String[] {"WXW", "ZMP", "CYC"}, (b, row) -> { // :1601-1605
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get());
			if (casing == null) return false;
			b.define('W', cable01(row.tier() + 1)).define('X', component("signal_emitter", row.tier() + 1)).define('Z', net.minecraft.tags.ItemTags.SAPLINGS)
					.define('M', casing).define('P', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "diamond")).define('C', GT6ItemTags.gt6("circuit" + (row.tier() + 1)))
					.define('Y', component("sensor", row.tier() + 1)).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.BUMBLELYZER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.BUMBLELYZER_ROWS, new String[] {"WXW", "ZMP", "CYC"}, (b, row) -> { // :1608-1612 (the plantalyzer grid; 'Z' = OD.container1000honey → the vanilla honey bottle, the bumbliary-band fold)
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get());
			if (casing == null) return false;
			b.define('W', cable01(row.tier() + 1)).define('X', component("signal_emitter", row.tier() + 1)).define('Z', net.minecraft.world.item.Items.HONEY_BOTTLE)
					.define('M', casing).define('P', GT6ItemTags.materialTag(GT6ItemTags.GEMS_FAMILY, "diamond")).define('C', GT6ItemTags.gt6("circuit" + (row.tier() + 1)))
					.define('Y', component("sensor", row.tier() + 1)).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.PRESSURE_WASHER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.PRESSURE_WASHER_ROWS, new String[] {"RPG", "wMG"}, (b, row) -> { // :1615-1618
			gregapi.oredict.OreDictMaterial m = row.material().get();
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, m), gearSmall = itemOrNull(gregapi.data.OP.gearGtSmall, m), rotor = itemOrNull(gregapi.data.OP.rotor, gregapi.data.MT.StainlessSteel);
			if (casing == null || gearSmall == null || rotor == null) return false;
			b.define('R', rotor).define('P', gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM.get()).define('G', gearSmall).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.FREEZER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.FREEZER_ROWS, new String[] {"hPw", "PMP", "PSP"}, (b, row) -> { // :1621-1625 ('S' = the Si plate ladder, 'P' = the FIXED plate(StainlessSteel) ladder capped at plateQuadruple — the raw :1625 T5 rung)
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get()), siPlate = itemOrNull(plateLadder(row.tier()), gregapi.data.MT.Si), ssPlate = itemOrNull(plateLadder(Math.min(row.tier(), 3)), gregapi.data.MT.StainlessSteel);
			if (casing == null || siPlate == null || ssPlate == null) return false;
			b.define('P', ssPlate).define('M', casing).define('S', siPlate).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.CRYO_MIXER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.CRYO_MIXER_ROWS, new String[] {"PMP", "PRP", "hSw"}, (b, row) -> { // :1628-1632 ('S' = the Si plate ladder, 'P' = the FIXED plate(StainlessSteel) ladder capped at plateQuadruple)
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get()), siPlate = itemOrNull(plateLadder(row.tier()), gregapi.data.MT.Si), rotor = itemOrNull(gregapi.data.OP.rotor, gregapi.data.MT.StainlessSteel), ssPlate = itemOrNull(plateLadder(Math.min(row.tier(), 3)), gregapi.data.MT.StainlessSteel);
			if (casing == null || siPlate == null || rotor == null || ssPlate == null) return false;
			b.define('P', ssPlate).define('M', casing).define('R', rotor).define('S', siPlate).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.BOXINATOR_ITEMS_BY_PATH, gregtech6.registry.GTMachines.BOXINATOR_ROWS, new String[] {"wP", "CY", "CM"}, (b, row) -> { // :1635-1639
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get());
			if (casing == null) return false;
			b.define('P', gregtech6.registry.GT6Robotics.PISTONS.get(row.tier() + 1).get()).define('Y', gregtech6.registry.GT6Robotics.CONVEYERS.get(row.tier() + 1).get())
					.define('C', GT6ItemTags.gt6("circuit" + (row.tier() + 1))).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.UNBOXINATOR_ITEMS_BY_PATH, gregtech6.registry.GTMachines.UNBOXINATOR_ROWS, new String[] {"Pw", "YC", "MC"}, (b, row) -> { // :1642-1646
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get());
			if (casing == null) return false;
			b.define('P', gregtech6.registry.GT6Robotics.PISTONS.get(row.tier() + 1).get()).define('Y', gregtech6.registry.GT6Robotics.CONVEYERS.get(row.tier() + 1).get())
					.define('C', GT6ItemTags.gt6("circuit" + (row.tier() + 1))).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		// -- machines4, the single-variant StainlessSteel rungs ---------------------
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.COAGULATOR_ITEMS_BY_PATH, gregtech6.registry.GTMachines.COAGULATOR_ROWS, new String[] {"T T", "hMw", "TdT"}, (b, row) -> { // :1651
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get()), screw = itemOrNull(gregapi.data.OP.screw, row.material().get());
			if (casing == null || screw == null) return false;
			b.define('T', screw).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.GENERIFIER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.GENERIFIER_ROWS, new String[] {"ChC", "CMC", "CwC"}, (b, row) -> { // :1652
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get()), casingSmall = itemOrNull(gregapi.data.OP.casingSmall, row.material().get());
			if (casing == null || casingSmall == null) return false;
			b.define('C', casingSmall).define('M', casing).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.BATH_ITEMS_BY_PATH, gregtech6.registry.GTMachines.BATH_ROWS, new String[] {"CwC", "PMP", "PPP"}, (b, row) -> { // :1653
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get()), casingSmall = itemOrNull(gregapi.data.OP.casingSmall, row.material().get()), plate = itemOrNull(gregapi.data.OP.plate, row.material().get());
			if (casing == null || casingSmall == null || plate == null) return false;
			b.define('C', casingSmall).define('M', casing).define('P', plate).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.FERMENTER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.FERMENTER_ROWS, new String[] {"wMh", "PPP", "BCB"}, (b, row) -> { // :1654 ('P' = pipeLarge → the wood-pipe fold)
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get()), cuDouble = itemOrNull(gregapi.data.OP.plateDouble, gregapi.data.MT.Cu);
			if (casing == null || cuDouble == null) return false;
			b.define('M', casing).define('P', gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM.get()).define('C', cuDouble)
					.define('B', net.minecraft.world.level.block.Blocks.BRICKS).unlockedBy("has_casing", has(casing));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.AUTOCLAVE_ITEMS_BY_PATH, gregtech6.registry.GTMachines.AUTOCLAVE_ROWS, new String[] {"CwC", "PMP", "GPG"}, (b, row) -> { // :1655 ('M' = casingMachineQuadruple, 'P' = pipeSmall → the wood-pipe fold)
			Item casingQuad = itemOrNull(gregapi.data.OP.casingMachineQuadruple, row.material().get()), casingSmall = itemOrNull(gregapi.data.OP.casingSmall, row.material().get()), gearSmall = itemOrNull(gregapi.data.OP.gearGtSmall, row.material().get());
			if (casingQuad == null || casingSmall == null || gearSmall == null) return false;
			b.define('C', casingSmall).define('M', casingQuad).define('G', gearSmall).define('P', gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM.get()).unlockedBy("has_casing", has(casingQuad));
			return true;
		}));
		rRows.addAll(machinePour(gregtech6.registry.GTMachines.MELTER_ITEMS_BY_PATH, gregtech6.registry.GTMachines.MELTER_ROWS, new String[] {"wUh", "PMP", "BCB"}, (b, row) -> { // :1657 ('U' = the ID-1005 Ceramic small crucible → the port smeltery_ceramic item; the housing rides the ANY.Iron → Iron fold — the port INDEX keys concrete materials)
			Item casing = itemOrNull(gregapi.data.OP.casingMachine, gregapi.data.MT.Iron), cuDouble = itemOrNull(gregapi.data.OP.plateDouble, gregapi.data.MT.Cu), crucible = gregtech6.registry.GT6Crucibles.ITEMS_BY_PATH.get("smeltery_ceramic").get();
			if (casing == null || cuDouble == null) return false;
			b.define('M', casing).define('P', gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM.get()).define('U', crucible).define('C', cuDouble)
					.define('B', net.minecraft.world.level.block.Blocks.BRICKS).unlockedBy("has_casing", has(casing));
			return true;
		}));
		return rRows;
	}

	/** The four Shredder rows — the :1294-1297 grid "GDG"/"hMw" VERBATIM ('D' = plateGem Diamond, the ANY.Diamond fold; the flat-constant walk, the steam-engine shape). */
	private java.util.List<PartFamilyRecipeRow> shredderRecipeBuilders() {
		gregapi.oredict.OreDictMaterial[] tMats = kineticLadder();
		String[] tPaths = {"shredder", "shredder_t2", "shredder_t3", "shredder_t4"};
		Item[] tItems = {gregtech6.registry.GTMachines.SHREDDER_ITEM.get(), gregtech6.registry.GTMachines.SHREDDER_T2_ITEM.get(), gregtech6.registry.GTMachines.SHREDDER_T3_ITEM.get(), gregtech6.registry.GTMachines.SHREDDER_T4_ITEM.get()};
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachineDouble, tMats[i]), tGear = itemOrNull(gregapi.data.OP.gearGt, tMats[i]), tPlateGem = itemOrNull(gregapi.data.OP.plateGem, gregapi.data.MT.Diamond);
			if (tCasing == null || tGear == null || tPlateGem == null) continue;
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tItems[i])
					.pattern("GDG").pattern("hMw")
					.define('G', tGear).define('D', tPlateGem).define('M', tCasing)
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER).define('w', GT6ItemTags.TOOLS_WRENCH)
					.unlockedBy("has_casing", has(tCasing)), steamBandId(tPaths[i])));
		}
		return rRows;
	}

	/** The four Crusher rows — the :1300-1303 grid "DMD"/"hSw" VERBATIM ('D' = gem Diamond). */
	private java.util.List<PartFamilyRecipeRow> crusherRecipeBuilders() {
		gregapi.oredict.OreDictMaterial[] tMats = kineticLadder();
		String[] tPaths = {"crusher", "crusher_t2", "crusher_t3", "crusher_t4"};
		Item[] tItems = {gregtech6.registry.GTMachines.CRUSHER_ITEM.get(), gregtech6.registry.GTMachines.CRUSHER_T2_ITEM.get(), gregtech6.registry.GTMachines.CRUSHER_T3_ITEM.get(), gregtech6.registry.GTMachines.CRUSHER_T4_ITEM.get()};
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachineDouble, tMats[i]), tSpring = itemOrNull(gregapi.data.OP.spring, tMats[i]), tGem = itemOrNull(gregapi.data.OP.gem, gregapi.data.MT.Diamond);
			if (tCasing == null || tSpring == null || tGem == null) continue;
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tItems[i])
					.pattern("DMD").pattern("hSw")
					.define('D', tGem).define('M', tCasing).define('S', tSpring)
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER).define('w', GT6ItemTags.TOOLS_WRENCH)
					.unlockedBy("has_casing", has(tCasing)), steamBandId(tPaths[i])));
		}
		return rRows;
	}

	/** The four Lathe rows — the :1306-1309 grid "TDS"/"dMG" VERBATIM ('D' = plateGemTiny Diamond, 'd' = the screwdriver). */
	private java.util.List<PartFamilyRecipeRow> latheRecipeBuilders() {
		gregapi.oredict.OreDictMaterial[] tMats = kineticLadder();
		String[] tPaths = {"lathe", "lathe_t2", "lathe_t3", "lathe_t4"};
		Item[] tItems = {gregtech6.registry.GTMachines.LATHE_ITEM.get(), gregtech6.registry.GTMachines.LATHE_T2_ITEM.get(), gregtech6.registry.GTMachines.LATHE_T3_ITEM.get(), gregtech6.registry.GTMachines.LATHE_T4_ITEM.get()};
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachineDouble, tMats[i]), tScrew = itemOrNull(gregapi.data.OP.screw, tMats[i]), tGear = itemOrNull(gregapi.data.OP.gearGt, tMats[i]), tGearSmall = itemOrNull(gregapi.data.OP.gearGtSmall, tMats[i]), tPlateGemTiny = itemOrNull(gregapi.data.OP.plateGemTiny, gregapi.data.MT.Diamond);
			if (tCasing == null || tScrew == null || tGear == null || tGearSmall == null || tPlateGemTiny == null) continue;
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tItems[i])
					.pattern("TDS").pattern("dMG")
					.define('T', tScrew).define('D', tPlateGemTiny).define('S', tGearSmall).define('M', tCasing).define('G', tGear)
					.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
					.unlockedBy("has_casing", has(tCasing)), steamBandId(tPaths[i])));
		}
		return rRows;
	}

	/** The four Oven rows — the :1288-1291 grid "wMh"/"BCB" VERBATIM (the flat OVEN_ITEMS walk over the OvenRow material suppliers). */
	private java.util.List<PartFamilyRecipeRow> ovenRecipeBuilders() {
		Item[] tItems = {gregtech6.registry.GTMachines.OVEN_ITEM.get(), gregtech6.registry.GTMachines.OVEN_T2_ITEM.get(), gregtech6.registry.GTMachines.OVEN_T3_ITEM.get(), gregtech6.registry.GTMachines.OVEN_T4_ITEM.get()};
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (int i = 0; i < gregtech6.registry.GTMachines.OVEN_ROWS.size(); i++) {
			gregtech6.registry.GTMachines.OvenRow tOven = gregtech6.registry.GTMachines.OVEN_ROWS.get(i);
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachine, tOven.material().get()), tCuDouble = itemOrNull(gregapi.data.OP.plateDouble, gregapi.data.MT.Cu);
			if (tCasing == null || tCuDouble == null) continue;
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tItems[i])
					.pattern("wMh").pattern("BCB")
					.define('M', tCasing).define('C', tCuDouble).define('B', net.minecraft.world.level.block.Blocks.BRICKS)
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER).define('w', GT6ItemTags.TOOLS_WRENCH)
					.unlockedBy("has_casing", has(tCasing)), steamBandId(tOven.path())));
		}
		return rRows;
	}

	/** The four Rolling Mill (RU) rows — the :1349-1352 grid "Gh"/"M "/"Gw" VERBATIM (the RU rows; the ULV rung is port-native, no upstream grid). */
	private java.util.List<PartFamilyRecipeRow> rollingMillRecipeBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.ROLLINGMILL_RU_ROWS) {
			gregapi.oredict.OreDictMaterial m = tRow.material().get();
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachineDouble, m), tGear = itemOrNull(gregapi.data.OP.gearGt, m);
			if (tCasing == null || tGear == null) continue;
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTMachines.ROLLINGMILL_ITEMS_BY_PATH.get(tRow.path()).get())
					.pattern("Gh").pattern("M ").pattern("Gw")
					.define('G', tGear).define('M', tCasing)
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER).define('w', GT6ItemTags.TOOLS_WRENCH)
					.unlockedBy("has_casing", has(tCasing)), steamBandId(tRow.path())));
		}
		return rRows;
	}

	/** The four Distillery rows — the :1398-1401 grid "GPG"/"WMW"/"hCw" VERBATIM ('W' = wireGt02 Constantan / wireGt04 Kanthal / wireGt08 Nichrome / wireGt16 SiC, the per-tier wire paths; 'G' = OD.blockGlassColorless → vanilla glass). */
	private java.util.List<PartFamilyRecipeRow> distilleryRecipeBuilders() {
		String[] tPaths = {"distillery", "distillery_t2", "distillery_t3", "distillery_t4"};
		String[] tWires = {"wire_constantan_gt02", "wire_kanthal_gt04", "wire_nichrome_gt08", "wire_carborundum_gt16"};
		gregapi.oredict.OreDictMaterial[] tMats = heatLadder();
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachine, tMats[i]), tCuDouble = itemOrNull(gregapi.data.OP.plateDouble, gregapi.data.MT.Cu), tWire = wireItemByPath(tWires[i]);
			if (tCasing == null || tCuDouble == null) continue;
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTMachines.DISTILLERY_ITEMS_BY_PATH.get(tPaths[i]).get())
					.pattern("GPG").pattern("WMW").pattern("hCw")
					.define('G', net.minecraft.world.item.Items.GLASS).define('W', tWire).define('C', tCuDouble).define('M', tCasing)
					.define('P', gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM.get()) // the pipeTiny→pipeLarge ladder → the wood-pipe fold
					.define('h', GT6ItemTags.TOOLS_HARD_HAMMER).define('w', GT6ItemTags.TOOLS_WRENCH)
					.unlockedBy("has_casing", has(tCasing)), steamBandId(tPaths[i])));
		}
		return rRows;
	}

	/** The four Extruder rows — the :1406-1409 grid "GPw"/"PMS"/"GPD" VERBATIM ('S' = toolHeadBuzzSaw over Steel on the T1 Low-Heat rung, TungstenCarbide beyond; 'P' = the plate ladder plate(Steel) / plateDouble..plateQuadruple(TungstenCarbide); 'D' = dust Diamond). */
	private java.util.List<PartFamilyRecipeRow> extruderRecipeBuilders() {
		gregapi.oredict.OreDictMaterial[] tMats = heatLadder();
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.EXTRUDER_ROWS) {
			gregapi.oredict.OreDictMaterial m = tRow.material().get();
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachineDouble, m), tGear = itemOrNull(gregapi.data.OP.gearGt, m), tDust = itemOrNull(gregapi.data.OP.dust, gregapi.data.MT.Diamond);
			Item tPlate = tRow.tier() == 0 ? itemOrNull(gregapi.data.OP.plate, gregapi.data.MT.Steel) : itemOrNull(plateLadder(tRow.tier()), gregapi.data.MT.TungstenCarbide);
			Item tHead = tRow.tier() == 0 ? itemOrNull(gregapi.data.OP.toolHeadBuzzSaw, gregapi.data.MT.Steel) : itemOrNull(gregapi.data.OP.toolHeadBuzzSaw, gregapi.data.MT.TungstenCarbide);
			if (tCasing == null || tGear == null || tDust == null || tHead == null || tPlate == null) continue;
			rRows.add(new PartFamilyRecipeRow(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, gregtech6.registry.GTMachines.EXTRUDER_ITEMS_BY_PATH.get(tRow.path()).get())
					.pattern("GPw").pattern("PMS").pattern("GPD")
					.define('G', tGear).define('P', tPlate).define('M', tCasing).define('S', tHead).define('D', tDust)
					.define('w', GT6ItemTags.TOOLS_WRENCH) // the GPw letter (NO hard hammer on the raw :1406 keys)
					.unlockedBy("has_casing", has(tCasing)), steamBandId(tRow.path())));
		}
		return rRows;
	}

	/** The Printer/Scanner(Visuals) shared pour — the :1450-1461 twin grids over the X/C/W column shape (X = conveyers, C = the circuit tag, W = the cable); the 'P' column rides the caller's item (the printer's pipeTiny ladder → the wood-pipe fold; the Scanner's FIXED plate(Lumium)). */
	private java.util.List<PartFamilyRecipeRow> printerLikePour(java.util.Map<String, ? extends java.util.function.Supplier<Item>> aItems,
			java.util.List<gregtech6.block.GTBasicMachineBlock.MachineRow> aRows, Item aPItem) {
		return machinePour(aItems, aRows, new String[] {"CPC", "wXh", "WMW"}, (b, row) -> {
			Item tCasing = itemOrNull(gregapi.data.OP.casingMachine, row.material().get());
			if (tCasing == null || aPItem == null) return false;
			b.define('C', GT6ItemTags.gt6("circuit" + (row.tier() + 1))).define('X', gregtech6.registry.GT6Robotics.CONVEYERS.get(row.tier() + 1).get())
					.define('W', cable01(row.tier() + 1)).define('M', tCasing).define('P', aPItem).unlockedBy("has_casing", has(tCasing));
			return true;
		});
	}

	// -----------------------------------------------------------------------
	// task crafting-machines-steam-band spec ② — the anvil forging ladder, the
	// chain bottom. Upstream gives the metal plate family NO vanilla-crafting
	// row — plate/plateDouble/.. live on the Anvil machine channel (Loader_
	// Recipes_Handlers.java:175-193) and the RollingMill channel (:264-269);
	// the census pinned the double break (boilers/steam engines consume
	// plate_double, the welder/rollingmill channels sit behind electric/RU
	// machines that were themselves uncraftable). This band translates the six
	// Anvil prefix rows onto the crafting grid — the input slots + the hard
	// hammer (the file-belt {"X ", " f"} frame shape):
	//   :175 ingot + ingot → ingotDouble      :193 ingotDouble ×1 → plate (the single slot)
	//   :181 plate + plate → plateDouble      :182 plate + plateDouble → plateTriple
	//   :185 plateDouble + plateDouble → plateQuadruple
	//   :186 plateDouble + plateTriple → plateQuintuple (the compressor 'P' column)
	// The condition fold = the :181 row condition verbatim minus the forge-
	// location gates (selfforge()/fullforge() — the hand grid IS the self-forge
	// face): SMITHABLE + FLAMMABLE.NOT + COATED.NOT + ANTIMATTER.NOT. Declared
	// translation of machine rows onto crafting (the FE_CONVERTER
	// declared-deviation face); ids ride the output prefix dirs (the
	// craftfrom-row-id convention).
	// -----------------------------------------------------------------------

	/** One forging-ladder row form: the id key (= the output prefix dir) + the output prefix + the two input prefixes (B == A = the doubled same-prefix pair :175/:181/:185; B null = the single-slot row :193). Package-private for the pin test. */
	record ForgeLadderForm(String aKey, gregapi.oredict.OreDictPrefix aOutput,
			gregapi.oredict.OreDictPrefix aInputA, gregapi.oredict.OreDictPrefix aInputB) {}

	/**
	 * The six Anvil-row forms (:175/:193/:181/:182/:185/:186, the amounts all 1→1). The
	 * doubled same-prefix rows encode BOTH slots (the upstream (ingot,1)+(ingot,1) shape);
	 * the :193 plate row is the single-slot one (ingotDouble×1 → plate×1). A method, not a
	 * field — the OP fields live only after OP.init (the stickCraftFromForms ruling).
	 */
	static java.util.List<ForgeLadderForm> forgeLadderForms() {
		return java.util.List.of(
				new ForgeLadderForm("ingot_double", gregapi.data.OP.ingotDouble, gregapi.data.OP.ingot, gregapi.data.OP.ingot),
				new ForgeLadderForm("plate", gregapi.data.OP.plate, gregapi.data.OP.ingotDouble, null),
				new ForgeLadderForm("plate_double", gregapi.data.OP.plateDouble, gregapi.data.OP.plate, gregapi.data.OP.plate),
				new ForgeLadderForm("plate_triple", gregapi.data.OP.plateTriple, gregapi.data.OP.plate, gregapi.data.OP.plateDouble),
				new ForgeLadderForm("plate_quadruple", gregapi.data.OP.plateQuadruple, gregapi.data.OP.plateDouble, gregapi.data.OP.plateDouble),
				new ForgeLadderForm("plate_quintuple", gregapi.data.OP.plateQuintuple, gregapi.data.OP.plateDouble, gregapi.data.OP.plateTriple));
	}

	/** The material face of one forging-ladder row (the test-visible walk unit, the StickCraftFromMaterialRow shape). */
	record ForgeLadderMaterialRow(ForgeLadderForm aForm, gregapi.oredict.OreDictMaterial aMaterial) {}

	/**
	 * The material face of the forging ladder: per form, the materials whose INPUT and
	 * OUTPUT items all exist (the registrationOrder intersection, in registration order)
	 * under the :181 condition fold (SMITHABLE + FLAMMABLE.NOT + COATED.NOT +
	 * ANTIMATTER.NOT).
	 */
	static java.util.List<ForgeLadderMaterialRow> forgeLadderMaterialRows() {
		java.util.List<ForgeLadderMaterialRow> rRows = new ArrayList<>();
		for (ForgeLadderForm tForm : forgeLadderForms()) {
			java.util.Set<OreDictMaterial> tFaceA = new java.util.HashSet<>();
			java.util.Set<OreDictMaterial> tFaceB = new java.util.HashSet<>();
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				if (tPair.prefix() == tForm.aInputA()) tFaceA.add(tPair.material());
				if (tForm.aInputB() != null && tPair.prefix() == tForm.aInputB()) tFaceB.add(tPair.material());
			}
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				OreDictMaterial tMaterial = tPair.material();
				if (tPair.prefix() != tForm.aOutput() || !tFaceA.contains(tMaterial)) continue; // the item-truth intersection
				if (tForm.aInputB() != null && !tFaceB.contains(tMaterial)) continue;
				if (!tMaterial.contains(gregapi.data.TD.Processing.SMITHABLE)) continue; // SMITHABLE
				if (tMaterial.contains(gregapi.data.TD.Properties.FLAMMABLE)) continue; // FLAMMABLE.NOT
				if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
				if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
				rRows.add(new ForgeLadderMaterialRow(tForm, tMaterial));
			}
		}
		return rRows;
	}

	/** One forging-ladder datagen row: the id + the builder parts (the StickCraftFromRow shape). */
	private record ForgeLadderRow(ResourceLocation aId, net.minecraft.world.item.Item aResult,
			net.minecraft.world.item.Item aInputA, net.minecraft.world.item.Item aInputB) {}

	/** The datagen face: the material walk resolved onto the live items (the silent-skip guard rides itemOrNull). */
	private java.util.List<PartFamilyRecipeRow> forgeLadderBuilders() {
		java.util.List<PartFamilyRecipeRow> rRows = new ArrayList<>();
		for (ForgeLadderMaterialRow tMaterialRow : forgeLadderMaterialRows()) {
			String tSnake = GTMaterialItems.snakeCase(tMaterialRow.aMaterial().mNameInternal);
			Item tResult = itemOrNull(tMaterialRow.aForm().aOutput(), tMaterialRow.aMaterial());
			Item tInputA = itemOrNull(tMaterialRow.aForm().aInputA(), tMaterialRow.aMaterial());
			Item tInputB = tMaterialRow.aForm().aInputB() == null ? null
					: itemOrNull(tMaterialRow.aForm().aInputB(), tMaterialRow.aMaterial());
			if (tResult == null || tInputA == null) continue; // the item-truth guard (belt and braces over the walk)
			String tKey = tMaterialRow.aForm().aKey() + "/" + tSnake;
			rRows.add(new PartFamilyRecipeRow(forgeLadderBuilder(tResult, tInputA, tInputB), forgeLadderId(tKey)));
		}
		return rRows;
	}

	/** The forging-ladder row id: &lt;output&gt;/&lt;material&gt; (the craftfrom-row-id convention). */
	private ResourceLocation forgeLadderId(String aKey) {
		String tKey = aKey; // the bare-identifier ctor-arg discipline (the swap-table regex note)
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tKey);
	}

	/** One row's builder — the inputs top (the doubled pair "AA" / the mixed pair "AB" / the single slot "A"), the hard hammer below-left (the anvil slots + hammer semantics). */
	private ShapedRecipeBuilder forgeLadderBuilder(Item aResult, Item aInputA, Item aInputB) {
		boolean tSingle = aInputB == null;
		boolean tDoubled = !tSingle && aInputB == aInputA;
		ShapedRecipeBuilder rBuilder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, aResult)
				.pattern(tSingle ? "A" : tDoubled ? "AA" : "AB")
				.pattern(tSingle ? "h" : "h ")
				.define('A', aInputA)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_input", has(aInputA));
		if (!tSingle && !tDoubled) rBuilder.define('B', aInputB);
		return rBuilder;
	}

}

