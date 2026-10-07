package gregtech6.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;

import net.minecraftforge.registries.RegistryObject;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Boilers;
import gregtech6.registry.GT6BurningBoxes;
import gregtech6.registry.GT6DynamoHousings;
import gregtech6.registry.GT6ElectricDynamos;
import gregtech6.registry.GT6FluxDynamos;
import gregtech6.registry.GT6HeatExchangers;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6MagicAbsorbers;
import gregtech6.registry.GT6Turbines;
import gregtech6.registry.GTMultiBlocks;

/**
 * The per-carrier energy census (task e2-energy-census, the E2 data layer of the
 * energy-source-page wave) — one derived index keyed by energy carrier, splitting every
 * machine face into three families so the E3 (JEI) / E4 (EMI) category pages query
 * instead of re-walking:
 *
 * <ul>
 * <li><b>produce</b> — machines EMITTING the carrier (the BE declaration face:
 * {@code mEnergyTypeEmitted} / {@code EMITTED_TYPE} constants and the
 * {@code GTMultiBlockConverter.applyRow} output columns). Converters appear here too,
 * keyed by their OUTPUT carrier — the approved page IA puts the steam turbine on the RU
 * produce row exactly like the diesel engine.</li>
 * <li><b>consume</b> — the recipe maps ACCEPTING the carrier, each with its workstation
 * icons. Fully DERIVED: {@link GT6RecipeMapViewerMeta#energyOf} (the ENERGY_BY_MAP
 * transcription, 51 maps over 9 carriers) reversed over
 * {@link GT6RecipeMapViewerMeta#visibleMaps()}, crossed with
 * {@link GT6RecipeMapIcons#workstationsOf} (the r11-emi-workstation reverse index) — a
 * future registration row lands here with zero census edits (the KJS declared seam).</li>
 * <li><b>convert</b> — machines whose accepted carrier differs from their emitted one
 * (the {@code accepted != emitted} ruling), keyed by their INPUT carrier. The pair
 * travels on the record ({@code from}/{@code to}) for the page's color-code rendering.</li>
 * </ul>
 *
 * <p><b>Carriers</b>: the nine ENERGY_BY_MAP carriers (the pinned census) plus
 * {@link TD.Energy#STEAM} as the tenth page carrier. STEAM has no ENERGY_BY_MAP row (no
 * EU denomination) and no packet-consuming recipe maps; its families come entirely off
 * the BE declaration face (producers = the boilers, converters = turbine/engine). RF is
 * tabled as a produce/convert OUTPUT only (the flux dynamos) — no page queries it in
 * v1, the record simply stays honest.
 *
 * <p><b>Empty families are legal states</b> (CU/LU/MU produce nothing — unported; KU
 * produces nothing; STEAM consumes nothing): every accessor returns an empty list, never
 * null, never throws.
 *
 * <p>The produce/convert tables are the machine-level transcription of the declaration
 * face, each entry carrying its BE-class evidence in the table comment; the consume face
 * derives live. The lazy-build idiom is the {@link GT6RecipeMapIcons} one (client-main
 * thread query face, registry classes init without the mod bus; the item suppliers stay
 * lazy so an offline JVM resolves paths but never calls {@code RegistryObject.get()}).
 */
public final class GT6EnergyCensus {

	/** One producing machine: the registration path (the census face) + the lazy item. */
	public record Machine(String path, Supplier<Item> item) {}

	/** One converter: accepts {@code from}, emits {@code to} (always {@code from != to}). */
	public record Converter(Machine machine, TagData from, TagData to) {}

	/** One consuming recipe map with its workstation icon sources (registration order). */
	public record Consumer(RecipeMap map, List<GT6RecipeMapIcons.Workstation> workstations) {}

	/** The three families of one carrier; any family may be empty (the legal state). */
	public record Families(List<Machine> produce, List<Consumer> consume, List<Converter> convert) {}

	/** The produce table, keyed by the EMITTED carrier (built once, see ensureBuilt). */
	private static volatile Map<TagData, List<Machine>> sProduce;

	/** The convert table (flat; keyed queries filter by {@code from}). */
	private static volatile List<Converter> sConverters;

	private GT6EnergyCensus() {}

	/**
	 * The ten page carriers: the nine ENERGY_BY_MAP carriers (short-code-sorted, the
	 * pinned census order) plus STEAM appended (no EU denomination — no short-code slot
	 * in the sorted run).
	 */
	public static List<TagData> carriers() {
		List<TagData> rCarriers = new ArrayList<>(GT6RecipeMapViewerMeta.pinnedEnergyCarriers());
		rCarriers.add(TD.Energy.STEAM);
		return List.copyOf(rCarriers);
	}

	/** Every machine emitting this carrier, registration order. Empty = the legal unported state. */
	public static List<Machine> producersOf(TagData aCarrier) {
		ensureBuilt();
		return sProduce.getOrDefault(aCarrier, List.of());
	}

	/**
	 * The recipe maps accepting this carrier with their workstations — the live
	 * ENERGY_BY_MAP⁻¹ × workstationsOf walk (never cached: the map objects regenerate
	 * per {@code GT6RecipeMaps} generation, the walk is a 75-map filter).
	 */
	public static List<Consumer> consumersOf(TagData aCarrier) {
		List<Consumer> rConsumers = new ArrayList<>();
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps())
			if (aCarrier.equals(GT6RecipeMapViewerMeta.energyOf(tMap)))
				rConsumers.add(new Consumer(tMap, List.copyOf(GT6RecipeMapIcons.workstationsOf(tMap))));
		return List.copyOf(rConsumers);
	}

	/** Converters whose ACCEPTED (input) carrier is this one — the page's converter row. */
	public static List<Converter> convertersOf(TagData aCarrier) {
		ensureBuilt();
		List<Converter> rConverters = new ArrayList<>();
		for (Converter tConverter : sConverters)
			if (tConverter.from() == aCarrier) rConverters.add(tConverter);
		return List.copyOf(rConverters);
	}

	/** The three families of one carrier in one record (the E3/E4 page-build face). */
	public static Families familiesOf(TagData aCarrier) {
		return new Families(producersOf(aCarrier), consumersOf(aCarrier), convertersOf(aCarrier));
	}

	// -------------------------------------------------------------------------
	// The produce/convert transcription (the BE declaration face)
	// -------------------------------------------------------------------------

	/** A family-walked machine: the row path pairs with the family's BY_PATH item table. */
	private static Machine of(Map<String, ? extends Supplier<Item>> aItems, String aPath) {
		return new Machine(aPath, () -> aItems.get(aPath).get());
	}

	/** A named-constant machine: the RegistryObject's own id is the path (no literal duplication). */
	private static Machine of(RegistryObject<Item> aItem) {
		return new Machine(aItem.getId().getPath(), aItem::get);
	}

	private static void add(Map<TagData, List<Machine>> aProduce, TagData aCarrier, Machine aMachine) {
		aProduce.computeIfAbsent(aCarrier, k -> new ArrayList<>()).add(aMachine);
	}

	private static void ensureBuilt() {
		if (sProduce != null) return; // ponytail: unsynchronized lazy build — the query face is the client main thread (plugin registration); a holder idiom only if an off-thread caller appears
		Map<TagData, List<Machine>> rProduce = new HashMap<>();
		List<Converter> rConverters = new ArrayList<>();

		// ---- HU produce: the four Burning Box generator families (solid/liquid/gas/
		// fluidbed — GTGeneratorSolidBlockEntity:131 mEnergyTypeEmitted = TD.Energy.HU,
		// the Liquid/FluidBed/Gas BEs inherit the Solid declaration) + the Heat Exchanger
		// (GT6HeatExchangerBlockEntity:133 mEnergyTypeEmitted = TD.Energy.HU).
		for (GT6BurningBoxes.BurningBoxRow tRow : GT6BurningBoxes.allRows())
			add(rProduce, TD.Energy.HU, of(GT6BurningBoxes.ITEMS_BY_PATH, tRow.path()));
		add(rProduce, TD.Energy.HU,
				new Machine(GT6HeatExchangers.HEAT_EXCHANGER_ROW.path(), GT6HeatExchangers.HEAT_EXCHANGER_ITEM::get));

		// ---- RU produce: the diesel engines (GTDieselEngineBlockEntity:152
		// mEnergyTypeEmitted = TD.Energy.RU; the DIESEL_ITEMS map fills at the mod-bus
		// event, so the walk mirrors GT6RecipeMapIcons' DIESEL_SPECS form), the steam
		// turbines (GTSteamTurbineBlockEntity:37 applyRow emits RU), the gas turbines
		// (GTGasTurbineBlockEntity:64 applyRow emits RU) and the water wheel
		// (GT6WaterWheelBlockEntity:275 — the pure RU source).
		for (GT6Kinetics.DieselSpec tSpec : GT6Kinetics.DIESEL_SPECS)
			add(rProduce, TD.Energy.RU, of(GT6Kinetics.DIESEL_ITEMS, GT6Kinetics.dieselName(tSpec.material())));
		for (GT6Turbines.SteamTurbineRow tRow : GT6Turbines.STEAM_ROWS)
			add(rProduce, TD.Energy.RU, of(GT6Turbines.ITEMS_BY_PATH, tRow.path()));
		for (GT6Turbines.GasTurbineRow tRow : GT6Turbines.GAS_ROWS)
			add(rProduce, TD.Energy.RU, of(GT6Turbines.ITEMS_BY_PATH, tRow.path()));
		add(rProduce, TD.Energy.RU, of(GT6Kinetics.WATER_WHEEL_ITEM));

		// ---- EU produce: the fusion reactor (TileEntityFusionReactor:123 final
		// mEnergyTypeEmitted = TD.Energy.EU), the lightning rod
		// (TileEntityLightningRod:147 same), the electric dynamos
		// (GT6ElectricDynamoBlockEntity:84 outputType = EU) and the large dynamo
		// multiblocks (GTLargeDynamoBlockEntity:33 applyRow(RU, EU)).
		add(rProduce, TD.Energy.EU, of(GTMultiBlocks.FUSION_REACTOR_ITEM));
		add(rProduce, TD.Energy.EU, of(GTMultiBlocks.LIGHTNING_ROD_ITEM));
		for (GT6ElectricDynamos.ElectricRow tRow : GT6ElectricDynamos.ROWS)
			add(rProduce, TD.Energy.EU, electricDynamo(tRow));
		for (GT6DynamoHousings.DynamoRow tRow : GT6DynamoHousings.DYNAMO_ROWS)
			add(rProduce, TD.Energy.EU, of(GT6DynamoHousings.ITEMS_BY_PATH, tRow.path()));

		// ---- QU/TU produce: the magic absorber — ONE block, TWO declaration rows
		// (GT6MagicAbsorberBlockEntity:98 default TU; :164 the egg → QU; :166 the skull
		// → TU). The same item sits in both families, exactly as the BE declares.
		for (String tPath : GT6MagicAbsorbers.MAGIC_ABSORBER_ITEMS_BY_PATH.keySet()) {
			add(rProduce, TD.Energy.TU, of(GT6MagicAbsorbers.MAGIC_ABSORBER_ITEMS_BY_PATH, tPath));
			add(rProduce, TD.Energy.QU, of(GT6MagicAbsorbers.MAGIC_ABSORBER_ITEMS_BY_PATH, tPath));
		}

		// ---- ST produce: the boiler tanks (GTBoilerTankBlockEntity:159 accepts HU,
		// boils steam into the tank network) + the large boilers
		// (TileEntityLargeBoiler:184 — solid fuel or HU heat in, steam out).
		for (GT6Boilers.BoilerRow tRow : GT6Boilers.allRows())
			add(rProduce, TD.Energy.STEAM, of(GT6Boilers.ITEMS_BY_PATH, tRow.path()));
		for (GTMultiBlocks.LargeBoilerRow tRow : GTMultiBlocks.LARGE_BOILER_ROWS)
			add(rProduce, TD.Energy.STEAM, of(GTMultiBlocks.LARGE_BOILER_ITEMS_BY_PATH, tRow.path()));

		// ---- RF produce: the flux dynamos (GT6FluxDynamoBlockEntity:94 outputType =
		// RF) — tabled for honesty, no page queries RF in v1.
		for (GT6FluxDynamos.FluxRow tRow : GT6FluxDynamos.ROWS)
			add(rProduce, TD.Energy.RF, fluxDynamo(tRow));

		// KU / CU / LU / MU produce: NO ported machine declares an emitted constant —
		// the legal empty families (the design's 空态 rows).

		// ---- Converters (accepted != emitted), each with its applyRow/field evidence:
		// steam turbines ST→RU (GTSteamTurbineBlockEntity:37), steam engines ST→KU
		// (GTSteamEngineBlockEntity:126 EMITTED_TYPE = KU, steam tank in), boiler tanks
		// HU→ST (GTBoilerTankBlockEntity:159 mEnergyTypeAccepted = HU), gas turbines
		// HU→RU (GTGasTurbineBlockEntity:64), electric dynamos RU→EU
		// (GT6DynamoBlockEntity:253 the accepted-RU/outputType pair), large dynamos
		// RU→EU (GTLargeDynamoBlockEntity:33), flux dynamos RU→RF (:253 + :94).
		for (GT6Turbines.SteamTurbineRow tRow : GT6Turbines.STEAM_ROWS)
			rConverters.add(new Converter(of(GT6Turbines.ITEMS_BY_PATH, tRow.path()), TD.Energy.STEAM, TD.Energy.RU));
		for (GT6Kinetics.SteamEngineRow tRow : GT6Kinetics.STEAM_ENGINES)
			rConverters.add(new Converter(of(GT6Kinetics.STEAM_ENGINE_ITEMS, tRow.path()), TD.Energy.STEAM, TD.Energy.KU));
		for (GT6Boilers.BoilerRow tRow : GT6Boilers.allRows())
			rConverters.add(new Converter(of(GT6Boilers.ITEMS_BY_PATH, tRow.path()), TD.Energy.HU, TD.Energy.STEAM));
		for (GT6Turbines.GasTurbineRow tRow : GT6Turbines.GAS_ROWS)
			rConverters.add(new Converter(of(GT6Turbines.ITEMS_BY_PATH, tRow.path()), TD.Energy.HU, TD.Energy.RU));
		for (GT6ElectricDynamos.ElectricRow tRow : GT6ElectricDynamos.ROWS)
			rConverters.add(new Converter(electricDynamo(tRow), TD.Energy.RU, TD.Energy.EU));
		for (GT6DynamoHousings.DynamoRow tRow : GT6DynamoHousings.DYNAMO_ROWS)
			rConverters.add(new Converter(of(GT6DynamoHousings.ITEMS_BY_PATH, tRow.path()), TD.Energy.RU, TD.Energy.EU));
		for (GT6FluxDynamos.FluxRow tRow : GT6FluxDynamos.ROWS)
			rConverters.add(new Converter(fluxDynamo(tRow), TD.Energy.RU, TD.Energy.RF));

		sConverters = List.copyOf(rConverters);
		sProduce = Map.copyOf(rProduce); // the flag LAST — a reader past the null check must see both tables
	}

	/**
	 * The electric dynamo item seam — the registry keeps six named
	 * {@code RegistryObject} constants (no BY_PATH table), so the row pairs by tier.
	 */
	private static Machine electricDynamo(GT6ElectricDynamos.ElectricRow aRow) {
		return switch (aRow.tier()) {
			case 0 -> new Machine(aRow.path(), GT6ElectricDynamos.ELECTRIC_DYNAMO_ULV_ITEM::get);
			case 1 -> new Machine(aRow.path(), GT6ElectricDynamos.ELECTRIC_DYNAMO_ITEM::get);
			case 2 -> new Machine(aRow.path(), GT6ElectricDynamos.ELECTRIC_DYNAMO_T2_ITEM::get);
			case 3 -> new Machine(aRow.path(), GT6ElectricDynamos.ELECTRIC_DYNAMO_T3_ITEM::get);
			case 4 -> new Machine(aRow.path(), GT6ElectricDynamos.ELECTRIC_DYNAMO_T4_ITEM::get);
			default -> new Machine(aRow.path(), GT6ElectricDynamos.ELECTRIC_DYNAMO_T5_ITEM::get);
		};
	}

	/** The flux dynamo twin (five named constants, {@link #electricDynamo} shape). */
	private static Machine fluxDynamo(GT6FluxDynamos.FluxRow aRow) {
		return switch (aRow.tier()) {
			case 0 -> new Machine(aRow.path(), GT6FluxDynamos.FLUX_DYNAMO_ITEM::get);
			case 1 -> new Machine(aRow.path(), GT6FluxDynamos.FLUX_DYNAMO_T2_ITEM::get);
			case 2 -> new Machine(aRow.path(), GT6FluxDynamos.FLUX_DYNAMO_T3_ITEM::get);
			case 3 -> new Machine(aRow.path(), GT6FluxDynamos.FLUX_DYNAMO_T4_ITEM::get);
			default -> new Machine(aRow.path(), GT6FluxDynamos.FLUX_DYNAMO_T5_ITEM::get);
		};
	}
}
