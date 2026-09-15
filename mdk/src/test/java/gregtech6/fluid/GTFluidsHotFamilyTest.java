package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Hot fluid family offline tests (task p29-w4-hot-lube acceptance ① — the registration-row
 * assertions against the DECLARED values): the twelve hot {@link GTFluids.ChemicalFluidSpec}
 * rows (Loader_Fluids.java:85-99, the STATE_LIQUID carriers — the six-arg create's 1000 is
 * the amount-per-unit NOT a density, FL.java:1089, so the non-pahoehoe rows ride the vanilla
 * 1000 density), the seven FM.Hot closure carriers (each anchored to the Loader_Fuels.java
 * reference that needs it — the createMolten melting-point rule FL.java:1077 / the
 * createLiquid 300 K rule :1072 / the :1128 density formula over the material g/cm³), and
 * the lubricant row (:617, the four-arg create → 300 K). The >340 K flag column is
 * acceptance ③'s census half — every row above the wood-barrel ceiling
 * {@code GTBarrelCommand} melt gate is declared here so the RCON steel-barrel arm has a
 * pinned counterpart.
 */
public class GTFluidsHotFamilyTest extends GTOfflineTestBase {

	/** The twelve hot ids in declaration order — the upstream Loader_Fluids.java block order. */
	private static final List<String> HOT_IDS = List.of(
			"ic2coolant", "ic2hotcoolant", "hotmoltensodium", "hotmoltentin", "hotmoltenlicl",
			"hotheavywater", "hotsemiheavywater", "hottritiatedwater",
			"hotcarbondioxide", "hothelium", "thoriumsalt", "ic2pahoehoelava");

	/** The seven closure carriers — blaze + the two molten walks + the three waters. */
	private static final List<String> CLOSURE_IDS = List.of(
			"blaze", "sodium_molten", "tin_molten", "lithium_chloride_molten",
			"heavywater", "semiheavywater", "tritiatedwater");

	@Test
	public void theTablesCarryNineteenPlusOneRowsInDeclarationOrder() {
		assertEquals(HOT_IDS, GTFluids.HOT_FLUID_SPECS.stream().map(GTFluids.ChemicalFluidSpec::name).toList());
		assertEquals(CLOSURE_IDS, GTFluids.CLOSURE_FLUID_SPECS.stream().map(GTFluids.ChemicalFluidSpec::name).toList());
		assertEquals(1, GTFluids.LUBRICANT_FLUID_SPECS.size());
		assertEquals("lubricant", GTFluids.LUBRICANT_FLUID_SPECS.get(0).name());
		assertEquals(HOT_IDS.size(), GTFluids.HOT_FLUIDS.size(), "the live registrations walk the same table");
		assertEquals(CLOSURE_IDS.size(), GTFluids.CLOSURE_FLUIDS.size(), "the live registrations walk the same table");
		assertEquals(1, GTFluids.LUBRICANT_FLUIDS.size(), "the live registrations walk the same table");
	}

	/** Acceptance ①: the per-fluid hot census — id / temperature / density / viscosity / luminosity / gas flag. */
	@Test
	public void hotDeclaredValuesMatchTheUpstreamAnchors() {
		// {id, tempK, density, viscosity, lum} — the FL.create 6-arg literals (:85-:99)
		Object[][] tCensus = {
			{"ic2coolant"       ,  300,  1000,  1000, 0}, // :85 the four-arg create default
			{"ic2hotcoolant"    , 1200,  1000,  1000, 0}, // :86
			{"hotmoltensodium"  , 1100,  1000,  1000, 0}, // :87
			{"hotmoltentin"     , 2800,  1000,  1000, 0}, // :88
			{"hotmoltenlicl"    , 1600,  1000,  1000, 0}, // :89
			{"hotheavywater"    ,  600,  1000,  1000, 0}, // :91
			{"hotsemiheavywater",  550,  1000,  1000, 0}, // :92
			{"hottritiatedwater",  650,  1000,  1000, 0}, // :93
			{"hotcarbondioxide" ,  950,  1000,  1000, 0}, // :95
			{"hothelium"        , 1150,  1000,  1000, 0}, // :96
			{"thoriumsalt"      ,  600,  1000,  1000, 0}, // :97
			{"ic2pahoehoelava"  , 1200, 50000, 250000, 10}, // :99 the chained setLuminosity/setDensity/setViscosity verbatim
		};
		for (Object[] tRow : tCensus) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.hotSpec((String) tRow[0]);
			assertNotNull(tSpec, (String) tRow[0]);
			assertEquals(tRow[1], tSpec.temperature(), tSpec.name() + ": the FL.create temperature literal");
			assertEquals(tRow[2], tSpec.density(), tSpec.name() + ": the material-null row rides the vanilla density (the 1000 in :85-:99 is the amount-per-unit, FL.java:1089)");
			assertEquals(tRow[3], tSpec.viscosity(), tSpec.name() + ": the STATE_LIQUID viscosity");
			assertEquals(tRow[4], tSpec.luminosity(), tSpec.name() + ": the luminosity literal");
			assertTrue(!tSpec.gas(), tSpec.name() + ": every :85-:99 row is a STATE_LIQUID carrier");
		}
	}

	/** Acceptance ①+②: the closure carriers — every FM.Hot :191-211 referenced fluid is a LIVE registration with its walk-rule values (the dead-row check). */
	@Test
	public void closureCarriersMatchTheWalkRulesAndCoverEveryHexReference() {
		// {id, tempK, density, viscosity, lum} — the walk-rule transcriptions
		Object[][] tCensus = {
			{"blaze"      , 4000, 1000, 1000, 15}, // :195 MT.Blaze, gcm default 1.0 → 1000, lum 15 verbatim
			{"sodium_molten"   , 370,  971, 1000, 10}, // createMolten MT.Na — melting 370, gcm 0.971 (MT.java:399)
			{"tin_molten"      , 505, 7287, 1000, 10}, // MT.Sn melting 505, gcm 7.287 (:439)
			{"lithium_chloride_molten", 880, 537, 1000, 10}, // MT.LiCl heat(880), uumMcfg 0.534+0.003214 (:1121)
			{"heavywater"     , 300, 1105, 1000, 0}, // MT.D2O setDensity 1.1056 → (long)1105.6 (:1009)
			{"semiheavywater" , 300, 1054, 1000, 0}, // MT.HDO 1.0540 → 1054 (:1008)
			{"tritiatedwater" , 300, 1211, 1000, 0}, // MT.T2O 1.2112 → 1211 (:1010)
		};
		for (Object[] tRow : tCensus) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.closureSpec((String) tRow[0]);
			assertNotNull(tSpec, (String) tRow[0]);
			assertEquals(tRow[1], tSpec.temperature(), tSpec.name() + ": the walk temperature rule");
			assertEquals(tRow[2], tSpec.density(), tSpec.name() + ": the FL.java:1128 formula over the material g/cm³");
			assertEquals(tRow[3], tSpec.viscosity(), tSpec.name() + ": the STATE_LIQUID viscosity");
			assertEquals(tRow[4], tSpec.luminosity(), tSpec.name() + ": the lum literal (createMolten 10 / :195 15)");
			assertTrue(!tSpec.gas(), tSpec.name() + ": a liquid");
		}
		// the F-2 row: the four-arg create default (Loader_Fluids.java:617)
		GTFluids.ChemicalFluidSpec tLube = GTFluids.lubricantSpec("lubricant");
		assertNotNull(tLube, ":617");
		assertEquals(300, tLube.temperature(), ":617 the four-arg create → 300 K");
		assertEquals(1000, tLube.density(), "the :1128 formula over MT.Lubricant's default 1.0 g/cm³ (MT.java:2081)");
		assertNull(GTFluids.hotSpec("nonexistent"), "the lookup misses clean");
	}

	/** Acceptance ③ census half: the >340 K flag column — every row above the wood ceiling plus the FL.java:90+:95-102 POWER_CONDUCTING membership (nine rows — the cold coolant/thoriumsalt/pahoehoe deliberately NOT). */
	@Test
	public void theOver340KCarriersAndThePowerConductingSeedsArePinned() {
		java.util.Set<String> tOver340 = new java.util.HashSet<>();
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.HOT_FLUID_SPECS) {
			if (tSpec.temperature() > 340) tOver340.add(tSpec.name());
		}
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.CLOSURE_FLUID_SPECS) {
			if (tSpec.temperature() > 340) tOver340.add(tSpec.name());
		}
		// ic2coolant (300 K) and the 300 K carriers stay below the wood ceiling
		assertEquals(java.util.Set.of(
				"ic2hotcoolant", "hotmoltensodium", "hotmoltentin", "hotmoltenlicl",
				"hotheavywater", "hotsemiheavywater", "hottritiatedwater",
				"hotcarbondioxide", "hothelium", "thoriumsalt", "ic2pahoehoelava",
				"blaze", "sodium_molten", "tin_molten", "lithium_chloride_molten"), tOver340,
				"the >340 K flag column — the RCON wood-barrel arm melts exactly this set");
		// FL.java:90 + :95-102 — the NINE POWER_CONDUCTING rows (the review-round correction:
		// the enum tail :97-102 carries the three hot waters + the two hot GAS rows); the
		// pipe :184 void gate consumers
		for (String tPc : new String[] {"ic2hotcoolant", "hotmoltensodium", "hotmoltentin",
				"hotheavywater", "hotsemiheavywater", "hottritiatedwater", "hotmoltenlicl",
				"hotcarbondioxide", "hothelium"}) {
			assertTrue(GTFluidLists.POWER_CONDUCTING.contains(tPc), tPc + ": the POWER_CONDUCTING flag row");
		}
		// the unseeded members of the same :89-105 block
		assertTrue(!GTFluidLists.POWER_CONDUCTING.contains("ic2coolant"), ":89 — the COLD coolant is deliberately not power conducting");
		assertTrue(!GTFluidLists.POWER_CONDUCTING.contains("thoriumsalt"), ":93 — LIQUID only");
		assertTrue(!GTFluidLists.POWER_CONDUCTING.contains("ic2pahoehoelava"), ":105 — SIMPLE, LIQUID only");
	}
}
