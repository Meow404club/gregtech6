package gregtech6.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GT6BurningBoxes;
import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6Distillation;
import gregtech6.registry.GT6HeatExchangers;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Kitchen;
import gregtech6.registry.GT6LargeMachines;
import gregtech6.registry.GT6Mortars;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GT6Turbines;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GTMultiBlocks;

/**
 * The per-map machine reverse index (task r11-emi-workstation-full, the user-reported
 * "EMI does not list every machine able to process a recipe" fix) — the batch-2
 * adjudication's deferred reverse seam, now DERIVED instead of hand-curated: the port's
 * folded 15-arg RecipeMap carries no upstream {@code mRecipeMachineList} and the machine
 * registries are forward-keyed, so the reverse mapping map→machines is walked out of the
 * registration tables themselves (the {@link GTBasicMachineBlock.MachineRow#recipes()}
 * Supplier<RecipeMap> seam + the family BY_PATH item tables, the GTCEu
 * GTRecipeEMICategory.registerWorkStations whole-registry walk). One entry per registered
 * machine, in registration order (the BY_PATH LinkedHashMap order — the research card's
 * "注册序天然升阶" tier reading; the family T1-T4 ladders ascend, ULV rungs tail the list
 * exactly as the registration loops append them). Both viewer legs consume this one index:
 * the EMI leg registers every entry as a workstation (EmiRecipes' per-category list, the
 * EmiRecipes.java:107-109 append) and the JEI leg as recipe catalysts (the JEMI red-line
 * twin); {@link #iconOf} hands back the FIRST entry — the same representative machine the
 * retired hand table tabled — so the category-icon face is byte-compatible with every
 * existing consumer.
 *
 * <p>The index builds LAZILY on first query: the walk resolves {@code row.recipes().get()}
 * for the map keys, and the RecipeMap fields are null until {@code GT6RecipeMaps.init()}
 * (the mod-construct face in-game — the viewer plugins register long after; the census
 * tests init explicitly). The map keys are stable name strings, so the cached index
 * survives the test generations' {@code reset()}/{@code init()} cycles unchanged.
 * ponytail: unsynchronized lazy build — the query face is the client main thread
 * (plugin registration), add a holder idiom only if an off-thread caller ever appears.
 *
	 * <p>The upstream fallback face stays FAITHFUL for the maps with no port machine (the
	 * three DECLARED-empty microwave/cooker/toolhead — GT6RecipeMapJsonLoader's
	 * zero-row-stock set — plus the nanofab, recipe-b6b's rows-before-machine card; the
	 * mortar joined the tabled machines in the mortar-family card): {@code #iconOf} hands
	 * back the lit-furnace default the
 * upstream NEI_RecipeMap.init() drew whenever a map's mRecipeMachineList was empty
 * (NEI_RecipeMap.java:82 {@code Blocks.lit_furnace}). That whitelist is the ONLY
 * fallback path — every other visible map must walk at least one machine (the guard test
 * pins this against the live census).
 *
 * <p>Deviations from the upstream manual adds (declared, nothing fabricated): the wooden
 * pot (GT6_Main.java:408-409 ByProductList) has no port item; the furnace face
 * (RM.java:174) is the EXCLUDED vanilla-mirror map — the vanilla viewer category is its
 * face; the oven/large-oven machines ride that same excluded furnace map; the chisel map
 * is NEI-disallowed upstream and here alike. The tool faces that DO exist in the port
 * (hammer, bending cylinder) and the anvil tiers (MultiTileEntityAnvil.java:424-426's
 * self-adds) walk in below.
 */
public final class GT6RecipeMapIcons {

	/**
	 * The upstream lit-furnace fallback's whitelist — the maps with ZERO port machines:
	 * the three DECLARED-empty faces (zero static rows, no machine ever) plus the nanofab
	 * (task recipe-b6b poured its 45-row stock but the machine block is not ported yet —
	 * upstream runs the same lit-furnace default whenever mRecipeMachineList is empty,
	 * NEI_RecipeMap.java:82, rows or not; the row retires when the machine card lands).
	 * Any wider fallback is a regression (the guard test pins this set against the visible
	 * census).
	 */
	public static final Set<String> FURNACE_FALLBACK = Set.of(
			"gt.recipe.microwave",  // the p34 easter-egg surface, machine not ported
			"gt.recipe.cooker",     // declared-empty, never had a consumer
			"gt.recipe.toolhead",   // declared-empty, the per-material listener walk is the W5 cut
			"gt.recipe.nanofab");   // recipe-b6b: rows poured, machine block still pending

	/** One machine entry: the registration path (the census/reconciliation face) + the lazy item. */
	public record Workstation(String path, Supplier<Item> item) {}

	/** The reverse index, built once on first query (see the class doc's lazy contract). */
	private static Map<String, List<Workstation>> sIndex;

	private GT6RecipeMapIcons() {}

	/**
	 * The item seam: live {@link RegistryObject} resolution by default, fixtures injected
	 * offline (the {@code GT6RecipeMapJsonLoader.sItemResolver} convention — the Forge
	 * item registry does not exist in a bare JVM, so the offline guard tests stub this to
	 * a vanilla stand-in instead of booting a registry). Public so the cross-package EMI
	 * leg's guard test can stub it.
	 */
	public static java.util.function.Function<Supplier<Item>, Item> sResolver = Supplier::get;

	/** Every registered machine able to process this map, registration order. Never null; empty for the whitelist maps. */
	public static List<Workstation> workstationsOf(RecipeMap aMap) {
		if (sIndex == null) sIndex = build();
		List<Workstation> tList = sIndex.get(aMap.mNameInternal);
		return tList == null ? List.of() : tList;
	}

	/** The resolved stack of one entry (the sResolver seam's workstation face). */
	public static ItemStack stackOf(Workstation aWorkstation) {
		return new ItemStack(sResolver.apply(aWorkstation.item()));
	}

	/** The tabled machine item for this map (the FIRST entry), or the whitelist's furnace fallback. Never empty. */
	public static ItemStack iconOf(RecipeMap aMap) {
		List<Workstation> tList = workstationsOf(aMap);
		return tList.isEmpty() ? new ItemStack(Items.FURNACE) : stackOf(tList.get(0));
	}

	/** True exactly when the map carries at least one tabled machine (the guard test's seam). */
	public static boolean has(RecipeMap aMap) {
		return !workstationsOf(aMap).isEmpty();
	}

	// -------------------------------------------------------------------------
	// the reverse-index walk (registration-table order throughout)
	// -------------------------------------------------------------------------

	private static void add(Map<String, List<Workstation>> aIndex, String aMap, Workstation aWorkstation) {
		aIndex.computeIfAbsent(aMap, k -> new ArrayList<>()).add(aWorkstation);
	}

	/** A single-machine face: the path is the registration id (the census face for free). */
	private static void single(Map<String, List<Workstation>> aIndex, String aMap, RegistryObject<Item> aItem) {
		add(aIndex, aMap, new Workstation(aItem.getId().getPath(), aItem::get));
	}

	/**
	 * A row-carried family: the rows' own {@code recipes()} supplier names the map key,
	 * the family's BY_PATH item table supplies the machine by path — the exact pairing the
	 * registration loops run ({@code ITEMS_BY_PATH.put(tRow.path(), ...)}), so the walk
	 * cannot drift from the registration.
	 */
	private static void walk(Map<String, List<Workstation>> aIndex, List<GTBasicMachineBlock.MachineRow> aRows,
			Map<String, RegistryObject<Item>> aItems) {
		for (GTBasicMachineBlock.MachineRow tRow : aRows)
			add(aIndex, tRow.recipes().get().mNameInternal,
					new Workstation(tRow.path(), () -> aItems.get(tRow.path()).get()));
	}

	/** A row-less tier ladder (the BET hardwires the map; each tier is one explicit item). */
	@SafeVarargs
	private static void ladder(Map<String, List<Workstation>> aIndex, String aMap, RegistryObject<Item>... aTiers) {
		for (RegistryObject<Item> tTier : aTiers) single(aIndex, aMap, tTier);
	}

	/**
	 * The whole walk, in registration-table order. The family order inside a shared map is
	 * what pins today's representative as the FIRST entry (mixer's manual ladder before the
	 * electric one, loom likewise, massfab's large controller before the small ladder,
	 * basic families before the twelve W3 large machines).
	 */
	private static Map<String, List<Workstation>> build() {
		Map<String, List<Workstation>> rIndex = new HashMap<>();

		// the multiblock controllers (GTMultiBlocks explicit items)
		single(rIndex, "gt.recipe.cokeoven", GTMultiBlocks.COKE_OVEN_ITEM);
		single(rIndex, "gt.recipe.implosioncompressor", GTMultiBlocks.IMPLOSION_COMPRESSOR_ITEM);
		single(rIndex, "gt.recipe.massfab", GTMultiBlocks.MASSFAB_ITEM);
		single(rIndex, "gt.recipe.fusionreactor", GTMultiBlocks.FUSION_REACTOR_ITEM);
		single(rIndex, "gt.recipe.bedrockorelist", GTMultiBlocks.BEDROCK_DRILL_ITEM);

		// the row-less tier ladders (shredder/crusher/lathe — the ULV blocks join the quads)
		ladder(rIndex, "gt.recipe.shredder", GTMachines.SHREDDER_ITEM, GTMachines.SHREDDER_T2_ITEM,
				GTMachines.SHREDDER_T3_ITEM, GTMachines.SHREDDER_T4_ITEM, GTMachines.SHREDDER_ULV_ITEM);
		ladder(rIndex, "gt.recipe.crusher", GTMachines.CRUSHER_ITEM, GTMachines.CRUSHER_T2_ITEM,
				GTMachines.CRUSHER_T3_ITEM, GTMachines.CRUSHER_T4_ITEM, GTMachines.CRUSHER_ULV_ITEM);
		ladder(rIndex, "gt.recipe.lathe", GTMachines.LATHE_ITEM, GTMachines.LATHE_T2_ITEM,
				GTMachines.LATHE_T3_ITEM, GTMachines.LATHE_T4_ITEM);

		// the row-carried basic-machine families (GTMachines registration-loop order)
		walk(rIndex, GTMachines.DRYER_ROWS, GTMachines.DRYER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.CANNER_ROWS, GTMachines.CANNER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.CANNER_ULV_ROWS, GTMachines.CANNER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.PRESS_ROWS, GTMachines.PRESS_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.EXTRUDER_ROWS, GTMachines.EXTRUDER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.SIFTER_ROWS, GTMachines.SIFTER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.SIFTER_ULV_ROWS, GTMachines.SIFTER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.COMPRESSOR_ROWS, GTMachines.COMPRESSOR_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.WIREMILL_ROWS, GTMachines.WIREMILL_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.WIREMILL_ULV_ROWS, GTMachines.WIREMILL_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.ROLLINGMILL_ROWS, GTMachines.ROLLINGMILL_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.ROLLINGMILL_RU_ROWS, GTMachines.ROLLINGMILL_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.ROLL_BENDER_ROWS, GTMachines.ROLLBENDER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.ROLL_FORMER_ROWS, GTMachines.ROLLFORMER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.CLUSTER_MILL_ROWS, GTMachines.CLUSTERMILL_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.MIXER_ROWS, GTMachines.MIXER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.ELECTRIC_MIXER_ROWS, GTMachines.ELECTRIC_MIXER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.LOOM_ROWS, GTMachines.LOOM_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.ELECTRIC_LOOM_ROWS, GTMachines.ELECTRIC_LOOM_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.ELECTRIC_SIFTER_ROWS, GTMachines.ELECTRIC_SIFTER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.BOXINATOR_ROWS, GTMachines.BOXINATOR_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.UNBOXINATOR_ROWS, GTMachines.UNBOXINATOR_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.FERMENTER_ROWS, GTMachines.FERMENTER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.POLARIZER_ROWS, GTMachines.POLARIZER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.MAGNETIC_SEPARATOR_ROWS, GTMachines.MAGNETIC_SEPARATOR_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.LASER_ENGRAVER_ROWS, GTMachines.LASER_ENGRAVER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.LASER_WELDER_ROWS, GTMachines.LASER_WELDER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.FREEZER_ROWS, GTMachines.FREEZER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.CRYO_MIXER_ROWS, GTMachines.CRYO_MIXER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.MASSFAB_SMALL_ROWS, GTMachines.MASSFAB_SMALL_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.MOLECULAR_SCANNER_ROWS, GTMachines.MOLECULAR_SCANNER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.REPLICATOR_ROWS, GTMachines.REPLICATOR_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.DISTILLERY_ROWS, GTMachines.DISTILLERY_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.BUZZSAW_ROWS, GTMachines.BUZZSAW_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.SQUEEZER_ROWS, GTMachines.SQUEEZER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.CENTRIFUGE_ROWS, GTMachines.CENTRIFUGE_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.SLUICE_ROWS, GTMachines.SLUICE_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.SANDING_ROWS, GTMachines.SANDING_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.PRESSURE_WASHER_ROWS, GTMachines.PRESSURE_WASHER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.AUTOCRAFTER_ROWS, GTMachines.AUTOCRAFTER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.LIGHTNING_ROWS, GTMachines.LIGHTNING_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.LAMINATOR_ROWS, GTMachines.LAMINATOR_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.ELECTROLYZER_ROWS, GTMachines.ELECTROLYZER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.INJECTOR_ROWS, GTMachines.INJECTOR_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.PRINTER_ROWS, GTMachines.PRINTER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.SCANNER_VISUALS_ROWS, GTMachines.SCANNER_VISUALS_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.SLICER_ROWS, GTMachines.SLICER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.STEAM_CRACKER_ROWS, GTMachines.STEAM_CRACKER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.CATALYTIC_CRACKER_ROWS, GTMachines.CATALYTIC_CRACKER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.COAGULATOR_ROWS, GTMachines.COAGULATOR_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.GENERIFIER_ROWS, GTMachines.GENERIFIER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.BATH_ROWS, GTMachines.BATH_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.AUTOCLAVE_ROWS, GTMachines.AUTOCLAVE_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.SMELTER_ROWS, GTMachines.SMELTER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.MELTER_ROWS, GTMachines.MELTER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.ROASTING_ROWS, GTMachines.ROASTING_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.BUMBLELYZER_ROWS, GTMachines.BUMBLELYZER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.CRYSTALLISATION_ROWS, GTMachines.CRYSTALLISATION_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.BURNER_MIXER_ROWS, GTMachines.BURNER_MIXER_ITEMS_BY_PATH);
		walk(rIndex, GTMachines.PLANTALYZER_ROWS, GTMachines.PLANTALYZER_ITEMS_BY_PATH);

		// the twelve W3 large machines (GT6LargeMachines rows, same recipes() seam)
		for (GT6LargeMachines.LargeMachineRow tRow : GT6LargeMachines.ROWS)
			add(rIndex, tRow.recipes().get().mNameInternal,
					new Workstation(tRow.path(), () -> GT6LargeMachines.ITEMS_BY_PATH.get(tRow.path()).get()));

		// the towers (the TowerRow cryo column names the map, the BE's own derivation)
		for (GT6Distillation.TowerRow tRow : GT6Distillation.ROWS)
			add(rIndex, tRow.cryo() ? "gt.recipe.cryodistillationtower" : "gt.recipe.distillationtower",
					new Workstation(tRow.path(), () -> GT6Distillation.TOWER_ITEMS_BY_PATH.get(tRow.path()).get()));

		// the anvil tiers (the MultiTileEntityAnvil :424-426 self-adds) + the tool/kitchen faces
		for (GT6Anvils.AnvilRow tRow : GT6Anvils.ROWS)
			add(rIndex, "gt.recipe.anvil", new Workstation(tRow.path(), () -> GT6Anvils.ITEMS_BY_PATH.get(tRow.path()).get()));
		single(rIndex, "gt.recipe.juicer", GT6Kitchen.JUICER_ITEM);
		// the mortar family (task mortar-family — the hand-tool face leaves the furnace
		// fallback whitelist: the five tier blocks walk in, registration order, the steel
		// head is the representative face)
		for (Map.Entry<String, RegistryObject<Item>> tEntry : GT6Mortars.ITEMS_BY_PATH.entrySet())
			add(rIndex, "gt.recipe.mortar", new Workstation(tEntry.getKey(), () -> tEntry.getValue().get()));
		single(rIndex, "gt.recipe.anvil.bend", GT6Tools.BENDING_CYLINDER);
		single(rIndex, "gt.recipe.hammer", GT6Tools.HAMMER);

		// the fuel faces: diesel engines (walked from the DIESEL_SPECS static table — the
		// DIESEL_ITEMS map fills at the mod-bus registration event, AFTER class-init, so the
		// spec-driven name derivation is the only offline-stable face; in-game the map is
		// live and the supplier resolves the same RegistryObject the event registered),
		// gas turbines, the FM.Burn burning boxes (LIQUID + GAS — the GAS BE extends the
		// LIQUID one, GTGeneratorGasBlockEntity), the fluidized bed, the heat exchanger.
		// SOLID/brick boxes burn on the vanilla furnace-fuel face — no recipe map, not
		// tabled (the declared deviation).
		for (GT6Kinetics.DieselSpec tSpec : GT6Kinetics.DIESEL_SPECS) {
			String tName = GT6Kinetics.dieselName(tSpec.material());
			add(rIndex, "gt.recipe.fuels.engine", new Workstation(tName, () -> GT6Kinetics.DIESEL_ITEMS.get(tName).get()));
		}
		for (GT6Turbines.GasTurbineRow tRow : GT6Turbines.GAS_ROWS)
			add(rIndex, "gt.recipe.fuels.gas", new Workstation(tRow.path(), () -> GT6Turbines.ITEMS_BY_PATH.get(tRow.path()).get()));
		for (GT6BurningBoxes.BurningBoxRow tRow : GT6BurningBoxes.allRows()) {
			String tMap = tRow.family() == GT6BurningBoxes.Family.LIQUID || tRow.family() == GT6BurningBoxes.Family.GAS
					? "gt.recipe.fuels.burn"
					: tRow.family() == GT6BurningBoxes.Family.FLUIDBED ? "gt.recipe.fuels.fluidbed" : null;
			if (tMap != null) add(rIndex, tMap, new Workstation(tRow.path(), () -> GT6BurningBoxes.ITEMS_BY_PATH.get(tRow.path()).get()));
		}
		single(rIndex, "gt.recipe.fuels.hot", GT6HeatExchangers.HEAT_EXCHANGER_ITEM);

		// the crucible pair (crucible-viewer-page): the Smeltery family (TileEntitySmeltery is
		// the crucible physics machine) — lowest registered rung, both maps share the
		// representative (the ITEMS_BY_PATH LinkedHashMap order)
		single(rIndex, "gt.recipe.cruciblesmelting", GT6Crucibles.ITEMS_BY_PATH.values().iterator().next());
		single(rIndex, "gt.recipe.cruciblealloying", GT6Crucibles.ITEMS_BY_PATH.values().iterator().next());

		return rIndex;
	}
}
