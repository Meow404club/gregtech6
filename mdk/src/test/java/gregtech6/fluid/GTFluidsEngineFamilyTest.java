package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Engine fuel fluid family offline tests (task p12-engine-fuel-fluids acceptance a — the
 * registration-row assertions against the DECLARED values): nine
 * {@link GTFluids.EngineFluidSpec} rows, id/temperature/density/viscosity/gas transcribed
 * from the upstream anchors (steam 373 K = FL.java:794 C+100, density −100/viscosity 200 =
 * FL.java:1105 STATE_GASEOUS and :1132 gas branch for MT.Steam's 0.0010 g/cm³; the liquids
 * 300 K = the createLiquid :1072 clamp over the fuel materials' .heat(100, 400), density
 * 1000 = the :1130 default-g/cm³ formula; tints = the material RGBa). The live registry
 * side is the runServer smoke evidence (the per-fluid GT6 fluid registered lines); offline
 * asserts the declaration table and the registration-shape ids.
 */
public class GTFluidsEngineFamilyTest extends GTOfflineTestBase {

	/** The nine ids in declaration order — the card list, steam first. */
	private static final List<String> IDS = List.of("steam", "distilled_water", "diesel", "kerosine",
			"petrol", "fuel", "nitrofuel", "jetfuel", "ethanol");

	@Test
	public void tableCarriesNineRowsInDeclarationOrder() {
		assertEquals(IDS, GTFluids.ENGINE_SPECS.stream().map(GTFluids.EngineFluidSpec::name).toList());
		assertEquals(IDS.size(), GTFluids.ENGINE_SPECS.size());
	}

	/** Acceptance a: the per-fluid declared values, one block per family. */
	@Test
	public void declaredValuesMatchTheUpstreamAnchors() {
		// steam — THE gaseous one: density < 0 (the FL.java:775 lighter rule), 373 K, gas
		GTFluids.EngineFluidSpec tSteam = GTFluids.engineSpec("steam");
		assertNotNull(tSteam);
		assertEquals(373, tSteam.temperature(), "FL.java:794 — the C+100 hardcode, C = 273 (CS.java:132)");
		assertEquals(-100, tSteam.density(), "FL.java:1105 STATE_GASEOUS carrier / :1132 -0.1/0.0010 for MT.Steam");
		assertEquals(200, tSteam.viscosity(), "FL.java:1105 — the gas-state viscosity");
		assertTrue(tSteam.gas(), "the gas declaration");
		assertEquals(0xFFC8C8C8, tSteam.tint(), "MT.Steam RGBa 200,200,200 (MT.java:1884)");

		// the liquids share the formula values (300 K / 1000 / 1000) and differ in tint
		for (String tId : List.of("distilled_water", "diesel", "kerosine", "petrol", "fuel", "nitrofuel", "jetfuel", "ethanol")) {
			GTFluids.EngineFluidSpec tSpec = GTFluids.engineSpec(tId);
			assertNotNull(tSpec, tId);
			assertEquals(300, tSpec.temperature(), tId + ": the createLiquid :1072 clamp");
			assertEquals(1000, tSpec.density(), tId + ": the :1130 liquid density carrier");
			assertEquals(1000, tSpec.viscosity(), tId + ": FL.java:1104 STATE_LIQUID viscosity");
			assertTrue(!tSpec.gas(), tId + ": a liquid");
		}
		assertEquals(0xFF6E6EFF, GTFluids.engineSpec("distilled_water").tint(), "MT.DistWater RGBa 110,110,255");
		assertEquals(0xFFFFFF00, GTFluids.engineSpec("diesel").tint(), "MT.Diesel RGBa 255,255,0");
		assertEquals(0xFF0000FF, GTFluids.engineSpec("kerosine").tint(), "MT.Kerosine RGBa 0,0,255");
		assertEquals(0xFFFF0000, GTFluids.engineSpec("petrol").tint(), "MT.Petrol RGBa 255,0,0");
		assertEquals(0xFFFFFF00, GTFluids.engineSpec("fuel").tint(), "MT.Fuel RGBa 255,255,0");
		assertEquals(0xFFC8FF00, GTFluids.engineSpec("nitrofuel").tint(), "MT.NitroFuel RGBa 200,255,0");
		assertEquals(0xFFFF8000, GTFluids.engineSpec("ethanol").tint(), "MT.Ethanol RGBa 255,128,0");
		assertEquals(0xFFD8C060, GTFluids.engineSpec("jetfuel").tint(), "no upstream material (FL.java:422) — the port-owned declared tint");
	}

	@Test
	public void unknownIdsResolveToNull() {
		assertNull(GTFluids.engineSpec("carbon_dioxide"), "not one of the nine — the CO2 mark stays data");
		assertNull(GTFluids.engineSpec(null));
		assertNull(GTFluids.engineSpec("creosote"), "the four pre-existing fluids are outside the engine table");
	}

	/** The registration shape: source = the id, flowing = id + "_flowing" (the four-DR template). */
	@Test
	public void registrationShapeCarriesSourceAndFlowingIds() {
		for (GTFluids.EngineFluid tFamily : GTFluids.engineFluids()) {
			//? if forge {
			assertEquals(new ResourceLocation("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(new ResourceLocation("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			//?} else {
			/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			*///?}
			assertEquals(tFamily.spec, tFamily.type.getId() == null ? null : GTFluids.engineSpec(tFamily.spec.name()), "spec identity");
		}
		// the offline walk keeps the spec list and the live handles aligned, same order
		assertEquals(GTFluids.ENGINE_SPECS, GTFluids.engineFluids().stream().map(f -> f.spec).toList());
	}

	/** The fluid-only declaration: the engine families expose NO block handle (zero blockstate JSON face). */
	@Test
	public void engineFamiliesDeclareNoBlockFace() {
		// the EngineFluid record shape itself: spec/type/source/flowing, no LiquidBlock slot
		assertEquals(4, GTFluids.EngineFluid.class.getDeclaredFields().length,
				"spec + type + source + flowing — adding a block field here would need a blockstate JSON face");
	}

		/** The engine-steam constants (card spec ④). 200 is the ENGINE-PRIVATE value (EngineSteam.java:58); the CS.java:242 global standard is 160 and stays unported — the boiler face is another card. */
	@Test
	public void steamConversionConstantsAreParkedHere() {
		assertEquals(200, GTFluids.STEAM_PER_WATER, "the engine-private MultiTileEntityEngineSteam.java:58 value — NOT the CS.java:242 global 160");
		assertEquals(2, GTFluids.STEAM_PER_EU, "CS.java:240 — 2 L steam per EU, global");
	}
}
