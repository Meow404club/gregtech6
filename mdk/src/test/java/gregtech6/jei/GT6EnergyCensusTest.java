/**
 * Offline guard suite for task e2-energy-census: the per-carrier energy census
 * ({@link GT6EnergyCensus}) that the E3 (JEI) / E4 (EMI) energy-source pages query.
 * Three nails per the card: ① the nine-carrier census pin (the per-carrier consume
 * counts over the live ENERGY_BY_MAP transcription — future registration drift goes
 * red here), ② the BE declaration-face full-collection pin (every machine class that
 * declares {@code mEnergyTypeEmitted} / {@code EMITTED_TYPE} / an
 * {@code applyRow(accepted, emitted)} pair is tabled — exact counts, additions are a
 * conscious pin update), ③ the STEAM adjudication (the tenth carrier: no ENERGY_BY_MAP
 * row, its families come off the declaration face alone). The empty-family semantics
 * (CU/LU/MU/KU produce nothing — unported) are pinned as legal states, never errors.
 *
 * <p>Offline posture = the GT6EnergyJumpTest form: the minimal vanilla bootstrap, the
 * recipe-map generation per test ({@code init()}/{@code reset()}), and NO item
 * resolution — the census is path-level data, the item suppliers stay lazy (a bare JVM
 * has no Forge registry).
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GTMaterialItems;

class GT6EnergyCensusTest {

	/** The minimal vanilla offline bootstrap (the GT6EnergyJumpTest form). */
	@BeforeAll
	static void bootVanillaOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GTMaterialItems.initMaterials();
	}

	@AfterEach
	void cleanUp() {
		GT6RecipeMaps.reset();
	}

	private static List<String> producePaths(TagData aCarrier) {
		return GT6EnergyCensus.producersOf(aCarrier).stream().map(GT6EnergyCensus.Machine::path).toList();
	}

	private static List<String> converterPaths(TagData aCarrier) {
		return GT6EnergyCensus.convertersOf(aCarrier).stream().map(t -> t.machine().path()).toList();
	}

	// -------------------------------------------------------------------
	// The carrier census: nine pinned + STEAM, deterministic order
	// -------------------------------------------------------------------

	@Test
	void carriersAreTheNinePinnedPlusSteamInDeterministicOrder() {
		List<TagData> tCarriers = GT6EnergyCensus.carriers();
		assertEquals(10, tCarriers.size(), "the nine ENERGY_BY_MAP carriers + STEAM, the tenth page carrier");
		assertEquals(List.of("CU", "EU", "HU", "KU", "LU", "MU", "QU", "RU", "TU"),
				tCarriers.subList(0, 9).stream().map(GT6RecipeMapViewerMeta::energyTypeShortCode).toList(),
				"the nine ride the pinnedEnergyCarriers short-code-sorted order");
		assertEquals(TD.Energy.STEAM, tCarriers.get(9), "STEAM appends last (no EU denomination, no sorted slot)");
	}

	// -------------------------------------------------------------------
	// Nail ①: the per-carrier consume census (the ENERGY_BY_MAP reverse)
	// -------------------------------------------------------------------

	@Test
	void consumeFacePinsTheFiftyOneMapCensusPerCarrier() {
		GT6RecipeMaps.init();
		assertEquals(9, GT6EnergyCensus.consumersOf(TD.Energy.EU).size(), "EU: the Electric_T maps");
		assertEquals(13, GT6EnergyCensus.consumersOf(TD.Energy.RU).size(), "RU: the Kinetic_T + buzzsaw/debarker maps");
		assertEquals(2, GT6EnergyCensus.consumersOf(TD.Energy.KU).size(), "KU: compressor + press");
		assertEquals(12, GT6EnergyCensus.consumersOf(TD.Energy.HU).size(), "HU: the Heat_T maps + distillation tower");
		assertEquals(3, GT6EnergyCensus.consumersOf(TD.Energy.CU).size(), "CU: the cryo maps");
		assertEquals(2, GT6EnergyCensus.consumersOf(TD.Energy.LU).size(), "LU: laserengraver + welder");
		assertEquals(2, GT6EnergyCensus.consumersOf(TD.Energy.MU).size(), "MU: polarizer + magneticseparator");
		assertEquals(3, GT6EnergyCensus.consumersOf(TD.Energy.QU).size(), "QU: massfab/replicator/scannermolecular");
		assertEquals(5, GT6EnergyCensus.consumersOf(TD.Energy.TU).size(), "TU: autoclave/bath/coagulator/fusionreactor/generifier");
		int tTotal = 0;
		for (TagData tCarrier : GT6EnergyCensus.carriers()) tTotal += GT6EnergyCensus.consumersOf(tCarrier).size();
		assertEquals(51, tTotal, "the #30a carrier-map census, split per carrier (51 = 9+13+2+12+3+2+2+3+5)");
		// the derived seam: every consumer exposes the workstation icon sources
		for (TagData tCarrier : GT6EnergyCensus.carriers())
			for (GT6EnergyCensus.Consumer tConsumer : GT6EnergyCensus.consumersOf(tCarrier))
				assertNotNull(tConsumer.map(), tCarrier + " consumer carries its recipe map");
	}

	// -------------------------------------------------------------------
	// Nail ②: the BE declaration face, fully collected (exact counts)
	// -------------------------------------------------------------------

	@Test
	void produceFacePinsTheDeclarationFaceExactly() {
		// HU 98 = 97 burning boxes (solid 27 + liquid 22 + gas 22 + fluidbed 26, the four
		// generator BEs over the GTGeneratorSolid:131 HU declaration) + the heat exchanger
		assertEquals(98, GT6EnergyCensus.producersOf(TD.Energy.HU).size(), "the burning box families + the heat exchanger");
		assertTrue(producePaths(TD.Energy.HU).contains("large_heat_exchanger"),
				"GT6HeatExchangerBlockEntity:133 mEnergyTypeEmitted = HU is collected");
		// RU 17 = 8 diesel (GTDieselEngineBlockEntity:152) + 4 steam turbines + 4 gas
		// turbines (both applyRow emit RU) + the water wheel (pure RU source)
		assertEquals(17, GT6EnergyCensus.producersOf(TD.Energy.RU).size());
		List<String> tRu = producePaths(TD.Energy.RU);
		assertTrue(tRu.contains("diesel_engine_steel") && tRu.contains("diesel_engine_iridium"),
				"the diesel walk (DIESEL_SPECS × dieselName) is collected");
		assertTrue(tRu.contains("steam_turbine_magnalium") && tRu.contains("steam_turbine_vibramantium"),
				"the steam turbine rows emit RU (GTSteamTurbineBlockEntity:37)");
		assertTrue(tRu.contains("gas_turbine_magnalium"), "the gas turbine rows emit RU (GTGasTurbineBlockEntity:64)");
		assertTrue(tRu.contains("water_wheel"), "the water wheel is the pure RU source (GT6WaterWheelBlockEntity:275)");
		// EU 12 = fusion reactor + lightning rod + 6 electric dynamos + 4 large dynamos
		assertEquals(12, GT6EnergyCensus.producersOf(TD.Energy.EU).size());
		List<String> tEu = producePaths(TD.Energy.EU);
		assertTrue(tEu.contains("fusion_reactor"), "TileEntityFusionReactor:123 final EU is collected");
		assertTrue(tEu.contains("multiblock_lightning_rod"), "TileEntityLightningRod:147 final EU is collected");
		assertTrue(tEu.contains("electric_dynamo_ulv") && tEu.contains("electric_dynamo_t5"),
				"GT6ElectricDynamoBlockEntity:84 outputType EU — the six-tier ladder");
		assertTrue(tEu.contains("large_dynamo_stainless_steel") && tEu.contains("large_dynamo_adamantium"),
				"GTLargeDynamoBlockEntity:33 applyRow(RU, EU) — the four housings");
		// QU/TU 1+1: the magic absorber's two declaration rows on one block
		assertEquals(1, GT6EnergyCensus.producersOf(TD.Energy.QU).size());
		assertEquals(1, GT6EnergyCensus.producersOf(TD.Energy.TU).size());
		assertEquals(producePaths(TD.Energy.QU), producePaths(TD.Energy.TU), "both rows are the same magic_absorber item");
		assertEquals(List.of("magic_absorber"), producePaths(TD.Energy.QU),
				"GT6MagicAbsorberBlockEntity:164 egg QU / :166 skull TU / :98 default TU");
		// ST 31 = 26 boiler tanks (13 + 13 strong) + 5 large boilers
		assertEquals(31, GT6EnergyCensus.producersOf(TD.Energy.STEAM).size());
		List<String> tSt = producePaths(TD.Energy.STEAM);
		assertTrue(tSt.contains("steam_boiler_tank_lead") && tSt.contains("strong_steam_boiler_tank_lead"),
				"the boiler tanks boil steam (GTBoilerTankBlockEntity:159 HU-fed)");
		assertTrue(tSt.contains("large_boiler_stainless_steel") && tSt.contains("large_boiler_adamantium"),
				"the large boilers (TileEntityLargeBoiler:184, solid fuel or HU heat)");
		// RF 5: the flux dynamo ladder (honest output, no page queries RF in v1)
		assertEquals(5, GT6EnergyCensus.producersOf(TD.Energy.RF).size());
		assertTrue(producePaths(TD.Energy.RF).contains("flux_dynamo"));
	}

	@Test
	void converterFacePinsTheAcceptedNotEmittedPairsExactly() {
		// ST→RU 4 turbines + ST→KU 28 steam engines = 32 converters accept STEAM
		List<GT6EnergyCensus.Converter> tSteam = GT6EnergyCensus.convertersOf(TD.Energy.STEAM);
		assertEquals(32, tSteam.size(), "the steam packet consumers per the declaration face");
		assertEquals(4, tSteam.stream().filter(t -> t.to() == TD.Energy.RU).count(), "the ST→RU turbine rows");
		assertEquals(28, tSteam.stream().filter(t -> t.to() == TD.Energy.KU).count(),
				"the ST→KU engine ladder (GTSteamEngineBlockEntity:126 EMITTED_TYPE = KU)");
		// HU→ST 26 tanks + HU→RU 4 gas turbines = 30 converters accept HU
		List<GT6EnergyCensus.Converter> tHu = GT6EnergyCensus.convertersOf(TD.Energy.HU);
		assertEquals(30, tHu.size());
		assertEquals(26, tHu.stream().filter(t -> t.to() == TD.Energy.STEAM).count(),
				"the HU→ST boiler tanks (GTBoilerTankBlockEntity:159 mEnergyTypeAccepted = HU)");
		assertEquals(4, tHu.stream().filter(t -> t.to() == TD.Energy.RU).count(), "the HU→RU gas turbine rows");
		// RU→EU 10 (6 electric + 4 large) + RU→RF 5 flux = 15 converters accept RU
		List<GT6EnergyCensus.Converter> tRu = GT6EnergyCensus.convertersOf(TD.Energy.RU);
		assertEquals(15, tRu.size());
		assertEquals(10, tRu.stream().filter(t -> t.to() == TD.Energy.EU).count(),
				"the RU→EU dynamos (GT6DynamoBlockEntity:253 accepted-RU/outputType pair + the large housings)");
		assertEquals(5, tRu.stream().filter(t -> t.to() == TD.Energy.RF).count(), "the RU→RF flux dynamos");
		// no converter accepts EU/KU/CU/LU/MU/QU/TU (transformers are transmission, not conversion)
		for (TagData tCarrier : List.of(TD.Energy.EU, TD.Energy.KU, TD.Energy.CU, TD.Energy.LU,
				TD.Energy.MU, TD.Energy.QU, TD.Energy.TU))
			assertTrue(GT6EnergyCensus.convertersOf(tCarrier).isEmpty(), tCarrier + " accepts no converter");
		// the ruling itself: every pair is accepted != emitted
		for (TagData tCarrier : GT6EnergyCensus.carriers())
			for (GT6EnergyCensus.Converter tConverter : GT6EnergyCensus.convertersOf(tCarrier))
				assertFalse(tConverter.from() == tConverter.to(), "a converter with from == to is a transmitter, not a converter");
	}

	// -------------------------------------------------------------------
	// Nail ③: the STEAM adjudication — the tenth page carrier
	// -------------------------------------------------------------------

	@Test
	void steamCarrierAdjudicationPinsTheDeclarationFaceFamilies() {
		GT6RecipeMaps.init();
		// no ENERGY_BY_MAP row ever carries STEAM — the consume face stays empty
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps())
			assertFalse(TD.Energy.STEAM.equals(GT6RecipeMapViewerMeta.energyOf(tMap)),
					tMap.mNameInternal + " must not transcribe a STEAM row (the GU face is its home)");
		assertTrue(GT6EnergyCensus.consumersOf(TD.Energy.STEAM).isEmpty(),
				"STEAM consumes no recipe map — its families are the declaration face alone");
		// produce: 31 boilers; convert: the turbine/engine pair face
		assertEquals(31, GT6EnergyCensus.producersOf(TD.Energy.STEAM).size(), "the steam producers (tanks + large boilers)");
		assertEquals(32, GT6EnergyCensus.convertersOf(TD.Energy.STEAM).size(),
				"the steam packet consumers are converters (ST→RU, ST→KU), not recipe maps");
	}

	// -------------------------------------------------------------------
	// The empty-family semantics + determinism
	// -------------------------------------------------------------------

	@Test
	void emptyFamiliesAreLegalStatesNotErrors() {
		GT6RecipeMaps.init();
		// KU/CU/LU/MU produce nothing — no ported machine declares their emitted constant
		for (TagData tCarrier : List.of(TD.Energy.KU, TD.Energy.CU, TD.Energy.LU, TD.Energy.MU))
			assertTrue(GT6EnergyCensus.producersOf(tCarrier).isEmpty(),
					tCarrier + " produce is the legal unported empty (the design's 空态 rows)");
		// every accessor answers total: never null, never throws, any carrier
		for (TagData tCarrier : GT6EnergyCensus.carriers()) {
			GT6EnergyCensus.Families tFamilies = GT6EnergyCensus.familiesOf(tCarrier);
			assertNotNull(tFamilies.produce(), tCarrier + " produce face");
			assertNotNull(tFamilies.consume(), tCarrier + " consume face");
			assertNotNull(tFamilies.convert(), tCarrier + " convert face");
		}
	}

	@Test
	void censusQueriesAreDeterministicAcrossRepeats() {
		GT6RecipeMaps.init();
		assertEquals(GT6EnergyCensus.carriers(), GT6EnergyCensus.carriers(), "the carrier order is stable");
		for (TagData tCarrier : GT6EnergyCensus.carriers()) {
			assertEquals(GT6EnergyCensus.familiesOf(tCarrier), GT6EnergyCensus.familiesOf(tCarrier),
					tCarrier + " families are stable across queries");
		}
	}
}
