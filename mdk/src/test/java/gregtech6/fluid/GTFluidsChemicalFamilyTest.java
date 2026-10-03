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
 * Chemical fluid family offline tests (task w4-f1-chemicals acceptance ①⑤ — the
 * registration-row assertions against the DECLARED values): the thirty-four
 * {@link GTFluids.ChemicalFluidSpec} rows, id/temperature/density/viscosity/gas/luminosity
 * transcribed from the upstream anchors (Loader_Fluids.java:40-41/:45-48/:59-63/:68 the
 * FL.create literals; the gas closure the FL.java:1080 createGas walk with every element's
 * plasma = boiling × 100, OreDictMaterial.java:927, so the temperature rule lands on 300 K;
 * the densities the :1128-1136 formula per material g/cm³ — MT.java:380/:383/:395-398/
 * :406/:425/:443/:476/:1027-1037; the isotope batch the :658-662 tag-driven loop — task
 * qu-b-materials). The live registry side is the runServer smoke evidence
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
				"heliumneon", // task debt-hene-fluid — the :660 createGas walk blend row the p29 batch left pooled
				"liquidoxygen",
			"deuterium", "tritium", "helium3",
			"lithium6_molten", "beryllium7_molten", "beryllium8_molten",
			"boron11_molten", "carbon13_molten", "ancientdebris_molten",
			// task fusion — the fusion-row closure quartet (the C/Li/W/Ad parent molten rows)
			"carbon_molten", "lithium_molten", "tungsten_molten", "adamantium_molten",
			// task qu-scanner-replicator — the :194 molten-redstone replicator carrier
			"redstone_molten",
			// task machines-bumblelyzer-crucible — the crystallisation molten quintet (the :683-706 row carriers)
			"silicon_molten", "germanium_molten", "redstonealloy_molten", "nikolinealloy_molten", "alumina_molten",
			// task machines-burner-plantalyzer — the three new Burner Mixer row
			// carriers (the fourth, tritiatedwater, is the w4-hot-lube closure row)
			"titaniumtetrachloride", "sodiumcarbonate_molten", "calcite_molten",
			// task chem-fluids-unlock — the B2 chemical-blocker batch, in table order
			"hydrogenperoxide", "hydrogenfluoride", "hydrochloricacid", "nitricacid",
				"nitrogenmonoxide", "nitrogendioxide", "sulfurdioxide", "sulfurtrioxide",
				"sulfuricacid", "disulfuricacid", "hexafluorosilicicacid", "aquaregia",
				"bromine", "saltwater", "saltedwater",
				"uraniumhexafluoride", "uranium238hexafluoride", "uranium235hexafluoride",
				"aluminiumfluoride_molten", "cryolite_molten",
				"bluevitriol", "redvitriol", "pinkvitriol", "cyanvitriol", "whitevitriol",
				"grayvitriol", "greenvitriol", "martianvitriol", "vitriolofclay",
				"chloroauricacid", "chloroplatinicacid", "stannicchloride");


	@Test
	public void tableCarriesTheChemicalRowsInDeclarationOrder() {
		assertEquals(IDS, GTFluids.CHEMICAL_SPECS.stream().map(GTFluids.ChemicalFluidSpec::name).toList());
		assertEquals(80, GTFluids.CHEMICAL_SPECS.size());
		assertEquals(80, GTFluids.CHEMICALS.size(), "the live registrations walk the same table");
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
			{"heliumneon"    ,   -92}, // 0.0001785 (He) + 0.0008999 (Ne) = 0.0010784 → −92.73 → −92 (:1024 uumMcfg, task debt-hene-fluid)
		};
		assertEquals(14, tFormulas.length, "the gas closure is fourteen");
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
		for (String tLight : List.of("hydrogen", "helium", "neon", "heliumneon")) {
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
	 * Task debt-hene-fluid — the blend row verbatim: the :1080 createGas walk over MT.HeNe
	 * (MT.java:1024 gaschemcent, the GASES tag → the Loader_Fluids.java:660 loop), temp
	 * 300 K over the uumMcfg-recomputed boiling 15 K (OreDictMaterial.java:482/:490 —
	 * melting (1+24)/2 = 12, boiling (4+27)/2 = 15, plasma 1550 → min(300, 1549)), density
	 * the :1128-1136 formula over the molecule g 0.0001785+0.0008999 = 0.0010784 → −92,
	 * the :1105 gas carrier, the material RGBa 255,0,128 tint, the mNameLocal display face.
	 */
	@Test
	public void heliumneonRowIsTheCreateGasWalkVerbatim() {
		GTFluids.ChemicalFluidSpec tSpec = GTFluids.chemicalSpec("heliumneon");
		assertNotNull(tSpec, "the blend row the p29 batch left pooled");
		assertEquals("Helium-Neon", tSpec.displayName(), "the createGas mNameLocal face (the createMaterial raw local string)");
		assertEquals(300, tSpec.temperature(), "the :1080 rule over the uumMcfg boiling 15 / plasma 1550");
		assertEquals(-92, tSpec.density(), "the :1128-1136 −0.1/g formula over g = 0.0010784");
		assertEquals(200, tSpec.viscosity(), "the FL.java:1105 gas viscosity");
		assertEquals(0xFF8000FF, tSpec.tint(), "the material RGBa 255,0,128 (MT.java:1024)");
		assertTrue(tSpec.gas(), "setGaseous (STATE_GASEOUS)");
		assertEquals(0, tSpec.luminosity(), "unlit");
		// the material binding seam: the port MT.HeNe internal name lowercases to the row id
		GTMaterialItems.initMaterials();
		assertSame(tSpec, GTFluids.specOf(gregapi.data.MT.HeNe, false), "specOf(MT.HeNe, gas) binds the blend row");
		assertSame(gregapi.data.MT.HeNe, GTFluids.materialOf(tSpec), "materialOf round-trips");
	}

	/**
	 * Task pool-gas-seeds-13 — the GAS-list reconciliation: upstream the
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
	 * Task qu-b-materials — the isotope batch census: the nine Loader_Fluids.java:
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
	 * Task qu-b-materials — the material↔spec binding seam: the two lookup legs
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

	/**
	 * Task chem-fluids-unlock — the B2 chemical-blocker batch (32 rows): the
	 * recipe-data-b2a/b2b2/b2b1 discard blockers plus the acid-chain family those rows
	 * consume/produce. Gases ride the FL.java:1080 createGas walk (temp = bp&lt;300 ?
	 * min(300, plasma−1) : bp — UF6's bp 329 lands verbatim, the acids clamp to 300),
	 * liquids the :1072 createLiquid walk (mp&lt;300 → min(300, bp−1) = 300 everywhere
	 * here), moltens the :1077 createMolten walk (+ luminosity 10). Densities are the
	 * :1128-1136 formula transcriptions (the setDensity literals, or the Σg·amt/U
	 * molecule sums, OreDictMaterial.java:393-409 over the element g/cm³ MT.java:943-1119).
	 */
	@Test
	public void b2BlockerBatchCarriesTheUpstreamWalkParameters() {
		// the nine createGas rows — {id, display, temp, density, tint}
		Object[][] tGases = {
			{"hydrogenfluoride"          , "Hydrogen Fluoride"        , 300,     1, 0xFF00F0F0}, // g = H+F = 0.00178588 → 1
			{"hydrochloricacid"          , "Hydrochloric Acid"        , 300,     3, 0xFF00FF80}, // g = H+Cl = 0.00330388 → 3
			{"nitrogenmonoxide"          , "Nitrogen Monoxide"        , 300,     2, 0xFF64AFFF}, // g = N+O = 0.0026796 → 2
			{"nitrogendioxide"           , "Nitrogen Dioxide"         , 300,     4, 0xFF78BEFF}, // g = N+2O = 0.0041086 → 4
			{"sulfurdioxide"             , "Sulfur Dioxide"           , 300,  2069, 0xFFFFC800}, // g = S+2O = 2.069858 → 2069
			{"sulfurtrioxide"            , "Sulfur Trioxide"          , 300,  2071, 0xFFFFDC00}, // g = S+3O = 2.071287 → 2071
			{"uraniumhexafluoride"       , "Uranium Hexafluoride"     , 329, 18960, 0xFF426255}, // bp 329 verbatim; g = U+6F = 18.960176 → 18960
			{"uranium238hexafluoride"    , "Uranium-238 Hexafluoride" , 329, 18960, 0xFF426255}, // the Chem:182 centrifuge split leg
			{"uranium235hexafluoride"    , "Uranium-235 Hexafluoride" , 329, 18960, 0xFF426255},
		};
		for (Object[] tRow : tGases) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.chemicalSpec((String)tRow[0]);
			assertNotNull(tSpec, (String)tRow[0]);
			assertEquals(tRow[1], tSpec.displayName(), (String)tRow[0] + ": the mNameLocal face");
			assertEquals(tRow[2], tSpec.temperature(), (String)tRow[0] + ": the :1080 rule over the material heat() points");
			assertEquals(tRow[3], tSpec.density(), (String)tRow[0] + ": the :1128-1136 1000·g formula");
			assertEquals(200, tSpec.viscosity(), (String)tRow[0] + ": the :1105 gas viscosity");
			assertEquals(tRow[4], tSpec.tint(), (String)tRow[0] + ": the material RGBa");
			assertTrue(tSpec.gas(), (String)tRow[0] + ": setGaseous");
			assertEquals(0, tSpec.luminosity(), (String)tRow[0] + ": unlit");
			assertTrue(GTFluidLists.isGas((String)tRow[0]), (String)tRow[0] + ": the :1105 GAS auto-add");
		}
		// the twenty-one createLiquid rows — {id, display, temp, density, tint}
		Object[][] tLiquids = {
			{"hydrogenperoxide"      , "Hydrogen Peroxide"       , 300,  1000, 0xFF1414FF}, // setDensity 1.0 (MT.java:1019); the Chem:88 Lightning row
			{"sulfuricacid"          , "Sulfuric Acid"           , 300,  1500, 0xFFFF8000}, // setDensity 1.5 (:1047)
			{"nitricacid"            , "Nitric Acid"             , 300,  1500, 0xFF80FF00}, // setDensity 1.5 (:1031)
			{"disulfuricacid"        , "Disulfuric Acid"         , 300,  1500, 0xFFFF9600}, // setDensity 1.5 (:1048)
			{"hexafluorosilicicacid" , "Hexafluorosilicic Acid"  , 300,  1500, 0xFFBEC8BE}, // setDensity 1.5 (:1054)
			{"aquaregia"             , "Aqua Regia"              , 300,  7526, 0xFF40FF40}, // g = 5×1.5+8×0.00330388 = 7.52643104 (:1188); the Chem:267 mixer row
			{"bromine"               , "Bromine"                 , 300,  3122, 0xFF500A0A}, // mp 265 → temp 300, g 3.122 (:424); the Chem:207 Freezer row
			{"saltwater"             , "Saltwater"               , 300,  1000, 0xFFFF00FF}, // mp 300 → temp 300, setDensity 1.0 (:1143)
			{"saltedwater"           , "Salted Water"            , 300,  1000, 0xFFFF00C8}, // same walk (:1160)
			{"bluevitriol"           , "Blue Vitriol"            , 300, 11032, 0xFF4242DE}, // g = Cu+S+4O = 11.032716 (:1169); the Chem:73 electrolyzer walk
			{"redvitriol"            , "Red Vitriol"             , 300, 10932, 0xFFDE4242}, // Co leg (:1171)
			{"pinkvitriol"           , "Pink Vitriol"            , 300,  3810, 0xFFDE6F6F}, // Mg leg (:1172)
			{"cyanvitriol"           , "Cyan Vitriol"            , 300, 10984, 0xFF6FDEDE}, // Ni leg (:1173)
			{"whitevitriol"          , "White Vitriol"           , 300,  9206, 0xFFDEDEDE}, // Zn leg (:1174)
			{"grayvitriol"           , "Gray Vitriol"            , 300,  9512, 0xFF6F6F6F}, // Mn leg (:1175); also the Chem:265 Eudialyte Bath row
			{"greenvitriol"          , "Green Vitriol"           , 300,  9946, 0xFF42DE42}, // Fe leg (:1170); also the Chem:261 Ilmenite Bath row
			{"martianvitriol"        , "Martian Vitriol"         , 300, 21966, 0xFFDE42DE}, // g = 2Fe+3S+12O = 21.966148 (:1176)
			{"vitriolofclay"         , "Vitriol Of Clay"         , 300, 33215, 0xFF42DEDE}, // g = 5×Al2O3+3S+9O = 33.215296 (:1177)
			{"chloroauricacid"       , "Chloroauric Acid"        , 300, 19294, 0xFFFFC846}, // g = Au+4Cl+H = 19.29494588 (:1163); the Chem:83 input
			{"chloroplatinicacid"    , "Chloroplatinic Acid"     , 300, 21479, 0xFFFF4646}, // g = Pt+6Cl+2H = 21.47946376 (:1164)
			{"stannicchloride"       , "Stannic Chloride"        , 300,  7299, 0xFFD2FAFA}, // g = Sn+4Cl = 7.299856 (:1165)
		};
		for (Object[] tRow : tLiquids) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.chemicalSpec((String)tRow[0]);
			assertNotNull(tSpec, (String)tRow[0]);
			assertEquals(tRow[1], tSpec.displayName(), (String)tRow[0] + ": the mNameLocal face");
			assertEquals(tRow[2], tSpec.temperature(), (String)tRow[0] + ": the :1072 rule over the material heat() points");
			assertEquals(tRow[3], tSpec.density(), (String)tRow[0] + ": the :1128-1136 1000·g formula");
			assertEquals(1000, tSpec.viscosity(), (String)tRow[0] + ": the :1104 STATE_LIQUID viscosity");
			assertEquals(tRow[4], tSpec.tint(), (String)tRow[0] + ": the material RGBa");
			assertTrue(!tSpec.gas(), (String)tRow[0] + ": a liquid");
		}
		// the two createMolten rows — the :1077 walk (+ luminosity 10); Na3AlF6/AlF3 carry
		// only MOLTEN tags upstream, so their .liquid() accessor resolves to the molten
		// carrier (createMolten binds mLiquid, FL.java:1130 — the Chem:291-298 aluminium
		// walk and the :103-106 mixer rows consume this face)
		Object[][] tMoltens = {
			{"aluminiumfluoride_molten", "Molten Aluminium Fluoride", 1560, 2703, 0xFFC8BEBE}, // mp 1560, g = Al+3F = 2.703088 (:1082)
			{"cryolite_molten"         , "Molten Cryolite"          , 1285, 5621, 0xFFC8BEBE}, // mp 1285, g = 3Na+Al+6F = 5.621176 (:1142)
		};
		for (Object[] tRow : tMoltens) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.chemicalSpec((String)tRow[0]);
			assertNotNull(tSpec, (String)tRow[0]);
			assertEquals(tRow[1], tSpec.displayName(), (String)tRow[0] + ": the \"Molten \" + mNameLocal face");
			assertEquals(tRow[2], tSpec.temperature(), (String)tRow[0] + ": the :1077 melting-point rule");
			assertEquals(tRow[3], tSpec.density(), (String)tRow[0] + ": the :1128-1136 1000·g formula");
			assertEquals(1000, tSpec.viscosity(), (String)tRow[0] + ": the STATE_LIQUID viscosity");
			assertEquals(tRow[4], tSpec.tint(), (String)tRow[0] + ": the material RGBa");
			assertTrue(!tSpec.gas(), (String)tRow[0] + ": a liquid");
			assertEquals(10, tSpec.luminosity(), (String)tRow[0] + ": the :1077 .setLuminosity(10) literal");
		}
	}

	/**
	 * Task chem-fluids-unlock — the difference-set assertions (the 交卡门禁 ①): the
	 * fluids the blocker lists named that ALREADY lived on the other tables are NOT
	 * re-registered (zero duplicates), and the declared absences stay absent. The
	 *Cream pair already maps to grcmilk_cream (FOOD_B1), pinkslime to the FOOD_B1
	 * aqua row; Heavy_Reiker ("rc heavy water", FL.java:121) stays unregistered — the
	 * RotaryCraft-compat alias behind the Chem:467 exists() gate, the same
	 * faithful-absence class the b2a Reikygen ruling declared; BlackVitriol
	 * (MT.java:1168) has zero consumers in the B2 loader files and stays pooled.
	 */
	@Test
	public void theDifferenceSetStaysClean() {
		// the already-mapped pair: zero re-registration
		assertNull(GTFluids.chemicalSpec("cream"), "Cream already lives as the grcmilk_cream FOOD_B1 row — no chemical-table duplicate");
		assertNotNull(GTFluids.foodB1Spec("grcmilk_cream"), "the Cream carrier stays the FOOD_B1 row (GTFluids.java:990 band)");
		assertNull(GTFluids.chemicalSpec("pinkslime"), "pinkslime already lives as the FOOD_B1 aqua row — no chemical-table duplicate");
		assertNull(GTFluids.simpleLiquidSpec("pinkslime"), "and not a simple-liquid row either");
		assertNotNull(GTFluids.foodB1Spec("pinkslime"), "the pinkslime carrier stays the FOOD_B1 row");
		// the declared faithful absences
		assertNull(GTFluids.chemicalSpec("rc heavy water"), "Heavy_Reiker: the RC-compat alias the upstream Chem:467 exists() gate hides — faithful absence (the b2a Reikygen ruling)");
		assertNull(GTFluids.simpleLiquidSpec("rc heavy water"), "no simple-liquid face either");
		assertNull(GTFluids.chemicalSpec("blackvitriol"), "BlackVitriol: zero B2 loader consumers — stays pooled (the YAGNI ruling)");
		// and the batch is 32 strong on the table (the census split above pins the legs)
		assertEquals(32, IDS.size() - 48, "the batch census: 9 gases + 21 liquids + 2 moltens");
	}

	/**
	 * Task chem-fluids-unlock — the material↔spec binding seam over the batch: the
	 * internal-name convention (specOf = mNameInternal lowercased, + "_molten" for the
	 * MOLTEN-tag-only pair) binds every material-walk row, round-tripping through
	 * {@link GTFluids#materialOf}.
	 */
	@Test
	public void b2BatchMaterialsRoundTripThroughTheSpecSeam() {
		GTMaterialItems.initMaterials();
		for (OreDictMaterial tGas : new OreDictMaterial[] {MT.H2O2, MT.HF, MT.HCl, MT.HNO3, MT.NO, MT.NO2, MT.SO2, MT.SO3, MT.UF6, MT.U235F6, MT.U238F6}) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.specOf(tGas, false);
			assertNotNull(tSpec, tGas.mNameInternal + ": the gas/liquid binding");
			assertSame(tGas, GTFluids.materialOf(tSpec), tGas.mNameInternal + ": the reverse leg");
		}
		assertEquals("sulfuricacid", GTFluids.specOf(MT.H2SO4, false).name());
		assertEquals("bromine", GTFluids.specOf(MT.Br, false).name());
		assertEquals("saltwater", GTFluids.specOf(MT.SaltWater, false).name());
		assertEquals("saltedwater", GTFluids.specOf(MT.SaltedWater, false).name());
		for (OreDictMaterial tMolten : new OreDictMaterial[] {MT.Na3AlF6, MT.AlF3}) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.specOf(tMolten, true);
			assertNotNull(tSpec, tMolten.mNameInternal + ": the molten binding");
			assertSame(tMolten, GTFluids.materialOf(tSpec), tMolten.mNameInternal + ": the reverse leg");
		}
		assertEquals("cryolite_molten", GTFluids.specOf(MT.Na3AlF6, true).name());
		assertEquals("aluminiumfluoride_molten", GTFluids.specOf(MT.AlF3, true).name());
	}

	@Test
	public void unknownIdsResolveToNull() {
		assertNull(GTFluids.chemicalSpec("oil"), "the p7 crude-oil row predates the family");
		assertNull(GTFluids.chemicalSpec("natural_gas"), "the p5 carrier is its own row");
		assertNull(GTFluids.chemicalSpec("kerosene"), "the upstream alias spelling is NOT a port id (the single-name ruling)");
		assertNull(GTFluids.chemicalSpec(null));
	}

	/**
	 * The client still/flow seam (task r11b-crucible-molten-art ③ — the "GTFluids 指向"
	 * assertion face): the molten-family rows (and only those — the
	 * {@link FluidBridge#isMoltenId} judge) point the fluid layers at the borrowed molten
	 * carrier sprite, everything else keeps the vanilla water layers. This is what makes
	 * the Jade tank bar draw the molten grayscale × mRGBaLiquid instead of tinted water.
	 */
	@Test
	public void theMoltenRowsPointAtTheBorrowedMoltenCarrier() {
		assertEquals("gt6:block/materialicons/rough/molten", GTFluids.MOLTEN_STILL.toString(),
				"the carrier = the borrowed shared standard molten art (assets/README.md sha a308af60…)");
		for (String tMolten : new String[] {"lithium6_molten", "redstone_molten", "silicon_molten", "calcite_molten"}) {
			assertNotNull(GTFluids.chemicalSpec(tMolten), tMolten + ": the row exists");
			assertEquals(GTFluids.MOLTEN_STILL, GTFluids.stillTextureOf(tMolten), tMolten + ": still rides the molten carrier");
			assertEquals(GTFluids.MOLTEN_STILL, GTFluids.flowTextureOf(tMolten), tMolten + ": flow shares the carrier (the dye-chemical still=flow form)");
		}
		for (String tWater : new String[] {"hydrogen", "saltwater", "liquidoxygen", "helium_plasma"}) {
			assertEquals("minecraft:block/water_still", GTFluids.stillTextureOf(tWater).toString(), tWater + ": non-molten keeps water");
			assertEquals("minecraft:block/water_flow", GTFluids.flowTextureOf(tWater).toString(), tWater + ": non-molten keeps water");
		}
	}
}
