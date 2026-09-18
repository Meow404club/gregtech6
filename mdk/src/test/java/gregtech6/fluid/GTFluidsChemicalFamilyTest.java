package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Chemical fluid family offline tests (task p29-w4-f1-chemicals acceptance ①⑤ — the
 * registration-row assertions against the DECLARED values): the thirty-four
 * {@link GTFluids.ChemicalFluidSpec} rows, id/temperature/density/viscosity/gas/luminosity
 * transcribed from the upstream anchors (Loader_Fluids.java:40-41/:45-48/:59-63/:68 the
 * FL.create literals; the gas closure the FL.java:1080 createGas walk with every element's
 * plasma = boiling × 100, OreDictMaterial.java:927, so the temperature rule lands on 300 K;
 * the densities the :1128-1136 formula per material g/cm³ — MT.java:380/:383/:395-398/
 * :406/:425/:443/:476/:1027-1037; the isotope batch the :658-662 tag-driven loop — task
 * p31-qu-b-materials). The live registry side is the runServer smoke evidence
 * (the per-fluid GT6 fluid registered lines); offline asserts the declaration table and the
 * registration-shape ids.
 */
public class GTFluidsChemicalFamilyTest extends GTOfflineTestBase {

	/** The thirty-four ids in declaration order — the upstream Loader_Fluids block order, the isotope loop appended (material-id order). */
	private static final List<String> IDS = List.of(
			"helium_plasma", "nitrogen_plasma",
			"propane", "butane", "propylene", "ethylene",
			"liquid_extra_heavy_oil", "liquid_heavy_oil", "liquid_medium_oil", "liquid_light_oil", "soulsandoil",
			"methane", "carbondioxide", "carbonmonoxide", "hydrogen",
			"nitrogen", "oxygen", "fluorine", "helium", "neon", "argon", "krypton", "xenon", "radon",
			"liquidoxygen",
			"deuterium", "tritium", "helium3",
			"lithium6_molten", "beryllium7_molten", "beryllium8_molten",
			"boron11_molten", "carbon13_molten", "ancientdebris_molten");

	@Test
	public void tableCarriesThirtyFourRowsInDeclarationOrder() {
		assertEquals(IDS, GTFluids.CHEMICAL_SPECS.stream().map(GTFluids.ChemicalFluidSpec::name).toList());
		assertEquals(34, GTFluids.CHEMICAL_SPECS.size());
		assertEquals(34, GTFluids.CHEMICALS.size(), "the live registrations walk the same table");
	}

	/** Acceptance ①: the per-fluid declared census, one block per sub-family. */
	@Test
	public void declaredValuesMatchTheUpstreamAnchors() {
		// the plasmas (Loader_Fluids.java:40-41) — the STATE_PLASMA carriers, FL.java:1106
		for (String tId : List.of("helium_plasma", "nitrogen_plasma")) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.chemicalSpec(tId);
			assertNotNull(tSpec, tId);
			assertEquals(10000, tSpec.temperature(), tId + ": the FL.create 10000 K literal");
			assertEquals(-100000, tSpec.density(), tId + ": the FL.java:1106 plasma density");
			assertEquals(10, tSpec.viscosity(), tId + ": the FL.java:1106 plasma viscosity");
			assertEquals(15, tSpec.luminosity(), tId + ": the FL.java:1106 luminosity 15 (+ the .setLuminosity(15) literal)");
			assertTrue(tSpec.gas(), tId + ": setGaseous over states 2|3");
		}
		// the cracked hydrocarbons (:45-48)
		assertEquals(-1000, GTFluids.chemicalSpec("propane").density(), ":45 the density literal");
		assertEquals(-1000, GTFluids.chemicalSpec("butane").density(), ":46 the density literal");
		assertEquals(1000, GTFluids.chemicalSpec("propylene").density(), "the FL.java:1130 formula over the 1.0 g/cm³ default (OreDictMaterial.java:240 — no uumMcfg on :1208)");
		assertEquals(1000, GTFluids.chemicalSpec("ethylene").density(), "the FL.java:1130 formula over the 1.0 g/cm³ default (no uumMcfg on :1209)");
		for (String tId : List.of("propane", "butane", "propylene", "ethylene")) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.chemicalSpec(tId);
			assertEquals(300, tSpec.temperature(), tId + ": the four-arg create default");
			assertEquals(200, tSpec.viscosity(), tId + ": FL.java:1105 the gas viscosity");
			assertTrue(tSpec.gas(), tId + ": a gas");
			assertEquals(0, tSpec.luminosity(), tId + ": unlit");
		}
		// the oils (:59-63) — the setDensity literals over the STATE_LIQUID carriers
		assertEquals(900, GTFluids.chemicalSpec("liquid_extra_heavy_oil").density(), ":59");
		assertEquals(800, GTFluids.chemicalSpec("liquid_heavy_oil").density(), ":60");
		assertEquals(700, GTFluids.chemicalSpec("liquid_medium_oil").density(), ":61");
		assertEquals(600, GTFluids.chemicalSpec("liquid_light_oil").density(), ":62");
		assertEquals(650, GTFluids.chemicalSpec("soulsandoil").density(), ":63");
		for (String tId : List.of("liquid_extra_heavy_oil", "liquid_heavy_oil", "liquid_medium_oil", "liquid_light_oil", "soulsandoil")) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.chemicalSpec(tId);
			assertEquals(300, tSpec.temperature(), tId + ": the four-arg create default");
			assertEquals(1000, tSpec.viscosity(), tId + ": FL.java:1104 the liquid viscosity");
			assertTrue(!tSpec.gas(), tId + ": a liquid");
		}
		// liquid oxygen (:68)
		GTFluids.ChemicalFluidSpec tLOx = GTFluids.chemicalSpec("liquidoxygen");
		assertNotNull(tLOx);
		assertEquals(85, tLOx.temperature(), ":68 the 85 K literal");
		assertEquals(1, tLOx.density(), "the FL.java:1130 formula over MT.O's 0.001429 g/cm³");
		assertEquals(1000, tLOx.viscosity(), "the STATE_LIQUID viscosity");
		assertTrue(!tLOx.gas(), "a liquid");
	}

	/**
	 * Acceptance ⑤: the gas closure rides the FL.java:1080 createGas walk — every element's
	 * plasma = boiling × 100 (OreDictMaterial.java:927) so the temperature rule lands on
	 * min(300, plasma − 1) = 300 K, and the :1128-1136 density formula per material g/cm³:
	 * heavier than the 0.0012 air weight → (long)(1000·g) &gt; 0, lighter → (long)(−0.1/g)
	 * &lt; 0. The ELEMENT gases carry their measured g/cm³ (MT.java:380-476); the COMPOUNDS
	 * ride the molecule-configuration recomputation (OreDictMaterial.java:478-492 over the
	 * :240 default 1.0 — the uumMcfg rows sum the constituent g/cm³ over the recipe ratios,
	 * carbon 2.267, MT.java:392) so they all SINK.
	 */
	@Test
	public void gasClosureRidesTheCreateGasDensityAndTemperatureRules() {
		// {id, expected density} — the formula transcriptions
		Object[][] tFormulas = {
			{"hydrogen"      , -1112}, // −0.1/0.00008988 = −1112.59 → −1112 (MT.java:380)
			{"helium"        ,  -560}, // −0.1/0.0001785 = −560.22 → −560 (:383)
			{"neon"          ,  -111}, // −0.1/0.0008999 = −111.12 → −111 (:398)
			{"nitrogen"      ,     1}, // 1000×0.0012506 = 1.25 → 1 (:395)
			{"oxygen"        ,     1}, // 1000×0.001429 = 1.43 → 1 (:396)
			{"fluorine"      ,     1}, // 1000×0.001696 = 1.70 → 1 (:397)
			{"argon"         ,     1}, // 1000×0.0017837 = 1.78 → 1 (:406)
			{"krypton"       ,     3}, // 1000×0.003733 = 3.73 → 3 (:425)
			{"xenon"         ,     5}, // 1000×0.005887 = 5.89 → 5 (:443)
			{"radon"         ,     9}, // 1000×0.00973 = 9.73 → 9 (:476)
			{"methane"       ,  2267}, // molecule g/cm³ = 2.267 (C) + 4×0.00008988 (H) = 2.2674 → 1000×g (:1037 uumMcfg)
			{"carbondioxide" ,  2269}, // 2.267 + 2×0.001429 (O) = 2.2699 → 2269 (:1035 uumMcfg)
			{"carbonmonoxide",  2268}, // 2.267 + 0.001429 = 2.2684 → 2268 (:1034 uumMcfg)
		};
		assertEquals(13, tFormulas.length, "the gas closure is thirteen");
		for (Object[] tRow : tFormulas) {
			String tId = (String)tRow[0];
			int tDensity = (Integer)tRow[1];
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.chemicalSpec(tId);
			assertNotNull(tSpec, tId);
			assertEquals(300, tSpec.temperature(), tId + ": the FL.java:1080 rule over plasma = boiling×100");
			assertEquals(tDensity, tSpec.density(), tId + ": the FL.java:1128-1136 transcription");
			assertEquals(200, tSpec.viscosity(), tId + ": the FL.java:1105 gas viscosity");
			assertTrue(tSpec.gas(), tId + ": setGaseous");
			assertTrue(tSpec.density() < 0 || tSpec.density() > 0, tId + ": never zero — the FL sign rule carries the gravity consumers");
		}
		// the negative-density gases ARE lighter than air (the FL.java:775 gravity verdict face)
		for (String tLight : List.of("hydrogen", "helium", "neon")) {
			assertTrue(GTFluids.chemicalSpec(tLight).density() < 0, tLight + ": lighter than air");
		}
		// the positive ones: the heavy noble/diatomic set AND the carbon compounds (the
		// molecule recomputation sums carbon's 2.267 — they SINK despite being gases)
		for (String tHeavy : List.of("nitrogen", "oxygen", "fluorine", "argon", "krypton", "xenon", "radon",
				"methane", "carbondioxide", "carbonmonoxide")) {
			assertTrue(GTFluids.chemicalSpec(tHeavy).density() > 0, tHeavy + ": heavier than air");
		}
	}

	/**
	 * Task p30-pool-gas-seeds-13 — the GAS-list reconciliation: upstream the
	 * {@code FL.create} STATE_GASEOUS arm auto-adds the fluid name to {@code FluidsGT.GAS}
	 * (FL.java:1105) while STATE_PLASMA routes to {@code FluidsGT.PLASMA} (:1106) and the
	 * liquid rows never touch it — so every gaseous non-plasma row of
	 * {@link GTFluids#CHEMICAL_SPECS} must be a {@link GTFluidLists#GAS} member under its
	 * port registry path, the plasmas and liquids must not.
	 */
	@Test
	public void gaseousRowsReconcileWithTheGasList() {
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.CHEMICAL_SPECS) {
			boolean tPlasma = tSpec.name().endsWith("_plasma"); // Loader_Fluids.java:40-41, the :1106 PLASMA route
			assertEquals(tSpec.gas() && !tPlasma, GTFluidLists.isGas(tSpec.name()),
					tSpec.name() + ": the GAS-list reconciliation");
		}
	}

	/** The registration shape: source = the id, flowing = id + "_flowing" (the four-DR template), specs aligned with the live handles. */
	@Test
	public void registrationShapeCarriesSourceAndFlowingIds() {
		for (GTFluids.ChemicalFluid tFamily : GTFluids.CHEMICALS) {
			//? if forge {
			assertEquals(new ResourceLocation("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(new ResourceLocation("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			//?} else {
			/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			*///?}
			assertEquals(tFamily.spec, GTFluids.chemicalSpec(tFamily.spec.name()), "spec identity");
		}
		assertEquals(GTFluids.CHEMICAL_SPECS, GTFluids.CHEMICALS.stream().map(f -> f.spec).toList(),
				"the offline walk keeps the spec list and the live handles aligned, same order");
	}

	/** The fluid-only declaration: the chemical families expose NO block handle (zero blockstate JSON face). */
	@Test
	public void chemicalFamiliesDeclareNoBlockFace() {
		assertEquals(4, GTFluids.ChemicalFluid.class.getDeclaredFields().length,
				"spec + type + source + flowing — adding a block field here would need a blockstate JSON face");
	}

	/**
	 * Task p31-qu-b-materials — the isotope batch census: the nine Loader_Fluids.java:
	 * 658-662 tag-driven loop rows for the fusion isotope materials. Gases ride the :1080
	 * createGas walk (temp = min(300, plasma−1) = 300 over the default 10000 K plasma
	 * point; density = the :1128-1136 −0.1/g formula), the molten rows the :1077
	 * createMolten walk (temp = the melting point verbatim, density = 1000·g, luminosity
	 * 10). Material RGBa per MT.java:381/:382/:384/:386/:388/:389/:391/:393/:1832.
	 */
	@Test
	public void isotopeBatchCarriesTheUpstreamFluidParameters() {
		// the three createGas rows — D and T share hydrogen's 0.00008988 g/cm³, He-3 helium's 0.0001785
		Object[][] tGases = {
			{"deuterium", "Deuterium", 300, -1112, 200, 0xFFFFFF00, true, 0}, // MT.D 255,255,0 (:381)
			{"tritium"   , "Tritium"   , 300, -1112, 200, 0xFFFF0000, true, 0}, // MT.T 255,0,0 (:382)
			{"helium3"   , "Helium-3"  , 300,  -560, 200, 0xFFFF8C00, true, 0}, // MT.He_3 255,255,140 (:384)
		};
		for (Object[] tRow : tGases) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.chemicalSpec((String)tRow[0]);
			assertNotNull(tSpec, (String)tRow[0]);
			assertEquals(tRow[1], tSpec.displayName(), (String)tRow[0] + ": the createGas mNameLocal face");
			assertEquals(tRow[2], tSpec.temperature(), (String)tRow[0] + ": the :1080 rule over plasma = 10000");
			assertEquals(tRow[3], tSpec.density(), (String)tRow[0] + ": the :1128-1136 −0.1/g formula");
			assertEquals(tRow[4], tSpec.viscosity(), (String)tRow[0] + ": the gas viscosity");
			assertEquals(tRow[5], tSpec.tint(), (String)tRow[0] + ": the material RGBa");
			assertEquals(tRow[6], tSpec.gas(), (String)tRow[0] + ": setGaseous");
			assertEquals(tRow[7], tSpec.luminosity(), (String)tRow[0] + ": unlit");
		}
		// the six createMolten rows — temp = the melting point, density = 1000·g, the :1077 luminosity 10 literal
		Object[][] tMoltens = {
			{"lithium6_molten"     , "Molten Lithium-6"     , 453 , 534 , 0xFFE6E1FF}, // MT.Li_6 mp 453, g 0.534 (:386)
			{"beryllium7_molten"   , "Molten Beryllium-7"   , 1560, 1850, 0xFF6EBE6E}, // MT.Be_7 mp 1560, g 1.85 (:388)
			{"beryllium8_molten"   , "Molten Beryllium-8"   , 1560, 1850, 0xFF6EC86E}, // MT.Be_8 mp 1560, g 1.85 (:389)
			{"boron11_molten"      , "Molten Boron-11"      , 2349, 2340, 0xFFF0F0F0}, // MT.B_11 mp 2349, g 2.34 (:391)
			{"carbon13_molten"     , "Molten Carbon-13"     , 3800, 2267, 0xFF191919}, // MT.C_13 mp 3800, g 2.267 (:393)
			{"ancientdebris_molten", "Molten Ancient Debris", 2011, 1000, 0xFF6E505A}, // MT.AncientDebris heat(MeteoricIron) = Fe.mp+200 (:1832/:414); g default 1.0 → 1000
		};
		for (Object[] tRow : tMoltens) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.chemicalSpec((String)tRow[0]);
			assertNotNull(tSpec, (String)tRow[0]);
			assertEquals(tRow[1], tSpec.displayName(), (String)tRow[0] + ": the createMolten \"Molten \" + mNameLocal face");
			assertEquals(tRow[2], tSpec.temperature(), (String)tRow[0] + ": the :1077 melting-point rule");
			assertEquals(tRow[3], tSpec.density(), (String)tRow[0] + ": the :1128-1136 1000·g formula");
			assertEquals(1000, tSpec.viscosity(), (String)tRow[0] + ": the STATE_LIQUID viscosity");
			assertEquals(tRow[4], tSpec.tint(), (String)tRow[0] + ": the material RGBa");
			assertTrue(!tSpec.gas(), (String)tRow[0] + ": a liquid");
			assertEquals(10, tSpec.luminosity(), (String)tRow[0] + ": the :1077 .setLuminosity(10) literal");
		}
	}

	/**
	 * Task p31-qu-b-materials — the material↔spec binding seam: the two lookup legs
	 * ({@link GTFluids#specOf} / {@link GTFluids#materialOf}) round-trip over the live
	 * MT table, and MT.Dilithium — the one batch material the upstream tag loop never
	 * reached (the crystal helper, MT.java:198, carries no GASES/MOLTEN/LIQUID tag) —
	 * stays unbound in both directions.
	 */
	@Test
	public void materialSpecBindingRoundTrips() {
		// the mdk single-JVM probe convention (GTMaterialBlocksRegistrationTest @BeforeAll):
		// idempotent init only, NEVER a registry reset — on the 21.1 leg the test JVM boots
		// through FML itself, and a test-time reset races the boot thread's block
		// registration walk (the getSeamIsNullBeforeRegistration sentinel lives on it).
		GTMaterialItems.initMaterials();
		// the gas legs (the D/T/He_3 fusion feedstock, Loader_Recipes_Other.java:949-963 .gas() consumers)
		for (OreDictMaterial tGas : new OreDictMaterial[] {MT.D, MT.T, MT.He_3}) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.specOf(tGas, false);
			assertNotNull(tSpec, tGas.mNameInternal + ": the gas binding");
			assertSame(tSpec, GTFluids.chemicalSpec(tGas.mNameInternal.toLowerCase()), tGas.mNameInternal + ": the id convention");
			assertSame(tGas, GTFluids.materialOf(tSpec), tGas.mNameInternal + ": the reverse leg");
		}
		assertEquals("deuterium", GTFluids.specOf(MT.D, false).name());
		assertEquals("helium3", GTFluids.specOf(MT.He_3, false).name());
		// the molten legs (the fusion .liquid() consumers :948-968 — the :968 alliage row
		// itself is MT.Ad = Adamantium, NOT this batch; AncientDebris rides the same :659
		// createMolten walk via its explicit MOLTEN argument, upstream MT.java:1832)
		for (OreDictMaterial tMolten : new OreDictMaterial[] {MT.Li_6, MT.Be_7, MT.Be_8, MT.B_11, MT.C_13, MT.AncientDebris}) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.specOf(tMolten, true);
			assertNotNull(tSpec, tMolten.mNameInternal + ": the molten binding");
			assertSame(tMolten, GTFluids.materialOf(tSpec), tMolten.mNameInternal + ": the reverse leg");
		}
		assertEquals("lithium6_molten", GTFluids.specOf(MT.Li_6, true).name());
		assertEquals("ancientdebris_molten", GTFluids.specOf(MT.AncientDebris, true).name());
		// the gas form of a metal stays unbound (the loop never registers two carriers for one material)
		assertNull(GTFluids.specOf(MT.Li_6, false), "no lithium6 gas row");
		assertNull(GTFluids.specOf(MT.C_13, false), "no carbon-13 gas row");
		// MT.Dilithium: zero fluid binding, faithful to the upstream crystal helper (MT.java:198/:2302)
		assertNull(GTFluids.specOf(MT.Dilithium, false), "no dilithium gas row");
		assertNull(GTFluids.specOf(MT.Dilithium, true), "no dilithium molten row");
		// the non-binding spec rows resolve to no material (explicit-name rows like liquidoxygen)
		assertNull(GTFluids.materialOf(GTFluids.chemicalSpec("liquidoxygen")));
		assertNull(GTFluids.specOf(null, false), "null-safe");
		assertNull(GTFluids.materialOf(null), "null-safe");
	}

	@Test
	public void unknownIdsResolveToNull() {
		assertNull(GTFluids.chemicalSpec("oil"), "the p7 crude-oil row predates the family");
		assertNull(GTFluids.chemicalSpec("natural_gas"), "the p5 carrier is its own row");
		assertNull(GTFluids.chemicalSpec("kerosene"), "the upstream alias spelling is NOT a port id (the single-name ruling)");
		assertNull(GTFluids.chemicalSpec(null));
	}
}
