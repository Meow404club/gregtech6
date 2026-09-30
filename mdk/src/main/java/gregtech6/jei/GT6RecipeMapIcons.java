package gregtech6.jei;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.registries.RegistryObject;

import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GT6BurningBoxes;
import gregtech6.registry.GT6Distillation;
import gregtech6.registry.GT6HeatExchangers;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Kitchen;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GT6Turbines;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GTMultiBlocks;

/**
 * The per-map category icon table (task issues #29/#34a, GitHub #29a) — the reverse seam the
 * batch-2 adjudication (task debt-jei-emi-batch2) deferred: the port's folded 15-arg
 * RecipeMap carries no upstream mRecipeMachineList and the machine registries are
 * forward-keyed (machine row → RecipeMapSupplier, many machines share one map), so the
 * reverse mapping lives HERE as one hand-curated row per visible map, keyed by
 * {@link RecipeMap#mNameInternal}. The icon stock is the map's representative machine
 * BlockItem (the lowest registered tier of its family) — zero new PNGs, the item-model
 * icons every machine already has. Both viewer legs consume this one table: the JEI leg
 * draws it through {@code IGuiHelper.createDrawableItemStack} (GT6RecipeMapJeiCategory)
 * and registers the same stack as the recipe catalyst; the EMI leg passes it as the
 * {@link dev.emi.emi.api.recipe.EmiRecipeCategory} icon and the workstation (the JEMI
 * red-line twin).
 *
 * <p>The upstream fallback face stays FAITHFUL for the four DECLARED-empty maps
 * (microwave/cooker/toolhead/mortar — GT6RecipeMapJsonLoader's zero-row-stock set, no
 * machine exists in the port): {@code #iconOf} hands back the lit-furnace default the
 * upstream NEI_RecipeMap.init() drew whenever a map's mRecipeMachineList was empty
 * (NEI_RecipeMap.java:82 {@code Blocks.lit_furnace}). That whitelist is the ONLY
 * fallback path — every other visible map must table a machine (the guard test pins
 * this against the live census).
 */
public final class GT6RecipeMapIcons {

	/**
	 * The upstream lit-furnace fallback's whitelist — exactly the DECLARED-empty maps
	 * (zero static rows, zero machines in the port). Any wider fallback is a regression
	 * (the guard test pins this set against the visible census).
	 */
	public static final Set<String> FURNACE_FALLBACK = Set.of(
			"gt.recipe.microwave",  // the p34 easter-egg surface, machine not ported
			"gt.recipe.cooker",     // declared-empty, never had a consumer
			"gt.recipe.toolhead",   // declared-empty, the per-material listener walk is the W5 cut
			"gt.recipe.mortar");    // hand-tool face, no mortar item exists in the port

	/** The per-map machine items, keyed by {@link RecipeMap#mNameInternal}. */
	private static final Map<String, Supplier<Item>> ICONS = new HashMap<>();

	private static void icon(String aMap, Supplier<Item> aItem) {
		ICONS.put(aMap, aItem);
	}

	/** The lowest registered tier of a family (BY_PATH maps insert in registration order). */
	private static Supplier<Item> first(Map<String, RegistryObject<Item>> aItems) {
		return () -> aItems.values().iterator().next().get();
	}

	/** The lowest registered tier of one sub-family inside a mixed BY_PATH map. */
	private static Supplier<Item> firstByPath(Map<String, RegistryObject<Item>> aItems, String aPrefix) {
		return () -> {
			for (Map.Entry<String, RegistryObject<Item>> tEntry : aItems.entrySet())
				if (tEntry.getKey().startsWith(aPrefix)) return tEntry.getValue().get();
			throw new IllegalStateException("no registered item under path prefix " + aPrefix);
		};
	}

	static {
		// the multiblock controllers (GTMultiBlocks explicit items)
		icon("gt.recipe.cokeoven", () -> GTMultiBlocks.COKE_OVEN_ITEM.get());
		icon("gt.recipe.implosioncompressor", () -> GTMultiBlocks.IMPLOSION_COMPRESSOR_ITEM.get());
		icon("gt.recipe.massfab", () -> GTMultiBlocks.MASSFAB_ITEM.get());
		icon("gt.recipe.fusionreactor", () -> GTMultiBlocks.FUSION_REACTOR_ITEM.get());
		icon("gt.recipe.bedrockorelist", () -> GTMultiBlocks.BEDROCK_DRILL_ITEM.get());
		// the basic-machine families with explicit base items
		icon("gt.recipe.shredder", () -> GTMachines.SHREDDER_ITEM.get());
		icon("gt.recipe.crusher", () -> GTMachines.CRUSHER_ITEM.get());
		icon("gt.recipe.lathe", () -> GTMachines.LATHE_ITEM.get());
		// the basic-machine families walked through their BY_PATH maps (lowest tier)
		icon("gt.recipe.distillery", first(GTMachines.DISTILLERY_ITEMS_BY_PATH));
		icon("gt.recipe.drying", first(GTMachines.DRYER_ITEMS_BY_PATH));
		icon("gt.recipe.mixer", first(GTMachines.MIXER_ITEMS_BY_PATH));
		icon("gt.recipe.burnmixer", first(GTMachines.BURNER_MIXER_ITEMS_BY_PATH));
		icon("gt.recipe.sifter", first(GTMachines.SIFTER_ITEMS_BY_PATH));
		icon("gt.recipe.compressor", first(GTMachines.COMPRESSOR_ITEMS_BY_PATH));
		icon("gt.recipe.wiremill", first(GTMachines.WIREMILL_ITEMS_BY_PATH));
		icon("gt.recipe.rollingmill", first(GTMachines.ROLLINGMILL_ITEMS_BY_PATH));
		icon("gt.recipe.extruder", first(GTMachines.EXTRUDER_ITEMS_BY_PATH));
		icon("gt.recipe.bath", first(GTMachines.BATH_ITEMS_BY_PATH));
		icon("gt.recipe.fermenter", first(GTMachines.FERMENTER_ITEMS_BY_PATH));
		icon("gt.recipe.loom", first(GTMachines.LOOM_ITEMS_BY_PATH));
		icon("gt.recipe.pressurewasher", first(GTMachines.PRESSURE_WASHER_ITEMS_BY_PATH));
		icon("gt.recipe.squeezer", first(GTMachines.SQUEEZER_ITEMS_BY_PATH));
		icon("gt.recipe.clustermill", first(GTMachines.CLUSTERMILL_ITEMS_BY_PATH));
		icon("gt.recipe.rollbender", first(GTMachines.ROLLBENDER_ITEMS_BY_PATH));
		icon("gt.recipe.rollformer", first(GTMachines.ROLLFORMER_ITEMS_BY_PATH));
		icon("gt.recipe.centrifuge", first(GTMachines.CENTRIFUGE_ITEMS_BY_PATH));
		icon("gt.recipe.sharpener", first(GTMachines.SANDING_ITEMS_BY_PATH));
		icon("gt.recipe.cutter", first(GTMachines.BUZZSAW_ITEMS_BY_PATH));
		icon("gt.recipe.boxinator", first(GTMachines.BOXINATOR_ITEMS_BY_PATH));
		icon("gt.recipe.unboxinator", first(GTMachines.UNBOXINATOR_ITEMS_BY_PATH));
		icon("gt.recipe.sluice", first(GTMachines.SLUICE_ITEMS_BY_PATH));
		icon("gt.recipe.steamcracking", first(GTMachines.STEAM_CRACKER_ITEMS_BY_PATH));
		icon("gt.recipe.catalyticcracking", first(GTMachines.CATALYTIC_CRACKER_ITEMS_BY_PATH));
		icon("gt.recipe.coagulator", first(GTMachines.COAGULATOR_ITEMS_BY_PATH));
		icon("gt.recipe.cryomixer", first(GTMachines.CRYO_MIXER_ITEMS_BY_PATH));
		icon("gt.recipe.magneticseparator", first(GTMachines.MAGNETIC_SEPARATOR_ITEMS_BY_PATH));
		icon("gt.recipe.injector", first(GTMachines.INJECTOR_ITEMS_BY_PATH));
		icon("gt.recipe.laminator", first(GTMachines.LAMINATOR_ITEMS_BY_PATH));
		icon("gt.recipe.autoclave", first(GTMachines.AUTOCLAVE_ITEMS_BY_PATH));
		icon("gt.recipe.freezer", first(GTMachines.FREEZER_ITEMS_BY_PATH));
		icon("gt.recipe.polarizer", first(GTMachines.POLARIZER_ITEMS_BY_PATH));
		icon("gt.recipe.lightning", first(GTMachines.LIGHTNING_ITEMS_BY_PATH));
		icon("gt.recipe.slicer", first(GTMachines.SLICER_ITEMS_BY_PATH));
		icon("gt.recipe.laserengraver", first(GTMachines.LASER_ENGRAVER_ITEMS_BY_PATH));
		icon("gt.recipe.welder", first(GTMachines.LASER_WELDER_ITEMS_BY_PATH));
		icon("gt.recipe.electrolyzer", first(GTMachines.ELECTROLYZER_ITEMS_BY_PATH));
		icon("gt.recipe.printer", first(GTMachines.PRINTER_ITEMS_BY_PATH));
		icon("gt.recipe.scannervisuals", first(GTMachines.SCANNER_VISUALS_ITEMS_BY_PATH));
		icon("gt.recipe.scannermolecular", first(GTMachines.MOLECULAR_SCANNER_ITEMS_BY_PATH));
		icon("gt.recipe.generifier", first(GTMachines.GENERIFIER_ITEMS_BY_PATH));
		icon("gt.recipe.melter", first(GTMachines.MELTER_ITEMS_BY_PATH));
		icon("gt.recipe.smelter", first(GTMachines.SMELTER_ITEMS_BY_PATH));
		icon("gt.recipe.roaster", first(GTMachines.ROASTING_ITEMS_BY_PATH));
		icon("gt.recipe.crystallisationcrucible", first(GTMachines.CRYSTALLISATION_ITEMS_BY_PATH));
		icon("gt.recipe.press", first(GTMachines.PRESS_ITEMS_BY_PATH));
		icon("gt.recipe.canner", first(GTMachines.CANNER_ITEMS_BY_PATH));
		icon("gt.recipe.replicator", first(GTMachines.REPLICATOR_ITEMS_BY_PATH));
		// the towers, kitchen, tools, fuels and anvil faces (their own registry classes)
		icon("gt.recipe.distillationtower", firstByPath(GT6Distillation.TOWER_ITEMS_BY_PATH, "distillation_tower"));
		icon("gt.recipe.cryodistillationtower", firstByPath(GT6Distillation.TOWER_ITEMS_BY_PATH, "cryo_distillation_tower"));
		icon("gt.recipe.juicer", () -> GT6Kitchen.JUICER_ITEM.get());
		icon("gt.recipe.anvil", first(GT6Anvils.ITEMS_BY_PATH));
		icon("gt.recipe.anvil.bend", () -> GT6Tools.BENDING_CYLINDER.get());
		icon("gt.recipe.hammer", () -> GT6Tools.HAMMER.get());
		icon("gt.recipe.fuels.engine", first(GT6Kinetics.DIESEL_ITEMS));
		icon("gt.recipe.fuels.burn", firstByPath(GT6BurningBoxes.ITEMS_BY_PATH, "burning_box_liquid"));
		icon("gt.recipe.fuels.fluidbed", firstByPath(GT6BurningBoxes.ITEMS_BY_PATH, "burning_box_fluidbed"));
		icon("gt.recipe.fuels.gas", first(GT6Turbines.ITEMS_BY_PATH));
		icon("gt.recipe.fuels.hot", () -> GT6HeatExchangers.HEAT_EXCHANGER_ITEM.get());
	}

	private GT6RecipeMapIcons() {}

	/**
	 * The item seam: live {@link RegistryObject} resolution by default, fixtures injected
	 * offline (the {@code GT6RecipeMapJsonLoader.sItemResolver} convention — the Forge
	 * item registry does not exist in a bare JVM, so the offline guard tests stub this to
	 * a vanilla stand-in instead of booting a registry). Public so the cross-package EMI
	 * leg's guard test can stub it.
	 */
	public static java.util.function.Function<Supplier<Item>, Item> sResolver = Supplier::get;

	/** The tabled machine item for this map, or the whitelist's furnace fallback. Never empty. */
	public static ItemStack iconOf(RecipeMap aMap) {
		Supplier<Item> tItem = ICONS.get(aMap.mNameInternal);
		return new ItemStack(tItem == null ? Items.FURNACE : sResolver.apply(tItem));
	}

	/** True exactly when the map carries a tabled machine (the guard test's seam). */
	public static boolean has(RecipeMap aMap) {
		return ICONS.containsKey(aMap.mNameInternal);
	}
}
