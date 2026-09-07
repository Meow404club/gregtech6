package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import gregtech6.recipes.GT6RecipesDrying;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Food fluid family offline tests (task p21-drying-food-fluids — the registration-row
 * assertions against the DECLARED values, the GTFluidsSimpleLiquidFamilyTest shape): four
 * {@link GTFluids.AquaFluidSpec} rows on the THIRD table — sap, maplesap, reedwater,
 * cactuswater (the FL.java:250/:252/:233/:234 shorthands, upstream FOOD-tagged). Three of
 * the four carry the GT6 {@code FL.create} definition (Loader_Fluids.java:461-463,
 * {@code FL.create(..., null, 1, 1000, 300)} — 300 K, the texture parameter null means no
 * dedicated fluid texture exists to borrow, the vanilla-water+tint declaration rides);
 * {@code sap} alone is an external-name fluid (FL.java:250, no {@code FL.create} anywhere —
 * the seawater/waterdirty precedent), so its declared values are the HONEST FluidType
 * defaults. The consumers are the four food rows of the Drying backfill
 * (Loader_Recipes_Food.java:654-658, GT6RecipesDrying.foodTable). The live registry side
 * is the runServer smoke evidence and the RCON {@code /gt6tank fill} chain; offline asserts
 * the declaration table, the fluid-only registration shape and the drying-table seam.
 */
public class GTFluidsFoodFamilyTest extends GTOfflineTestBase {

	/** The four ids in declaration order — the card list, the GT6RecipesDrying pinned spellings. */
	private static final List<String> IDS = List.of("sap", "maplesap", "reedwater", "cactuswater");

	@Test
	public void tableCarriesFourRowsInDeclarationOrder() {
		assertEquals(IDS, GTFluids.FOOD_FLUID_SPECS.stream().map(GTFluids.AquaFluidSpec::name).toList());
		assertEquals(IDS.size(), GTFluids.FOOD_FLUID_SPECS.size());
	}

	/** The cross-family contract: the two earlier tables are UNCHANGED by the third (their exact-order assertions stay green). */
	@Test
	public void theAquaAndSimpleLiquidTablesAreUntouched() {
		assertEquals(6, GTFluids.AQUA_SPECS.size(), "the p16-aqua-fluids six, never appended");
		assertEquals(List.of("spdew", "mnwtr", "water_geothermal", "water_boiling", "hot_water", "cold_water"),
				GTFluids.AQUA_SPECS.stream().map(GTFluids.AquaFluidSpec::name).toList());
		assertEquals(2, GTFluids.SIMPLE_LIQUID_SPECS.size(), "the p19 simple-liquid pair, never appended");
		assertEquals(List.of("seawater", "waterdirty"),
				GTFluids.SIMPLE_LIQUID_SPECS.stream().map(GTFluids.AquaFluidSpec::name).toList());
	}

	/** The per-fluid declared values, one block per family (acceptance: 属性断言). */
	@Test
	public void declaredValuesMatchTheUpstreamAnchors() {
		// the three GT6-defined fluids: the FL.create carriers + display names verbatim
		// (Loader_Fluids.java:461-463 — FL.create(..., null, 1, 1000, 300))
		GTFluids.AquaFluidSpec tMaple = GTFluids.foodSpec("maplesap");
		assertNotNull(tMaple);
		assertEquals(300, tMaple.temperature(), "Loader_Fluids.java:463 FL.create(..., 1, 1000, 300)");
		assertEquals("Maple Sap", tMaple.displayName(), "Loader_Fluids.java:463 verbatim");

		GTFluids.AquaFluidSpec tReed = GTFluids.foodSpec("reedwater");
		assertNotNull(tReed);
		assertEquals(300, tReed.temperature(), "Loader_Fluids.java:461 FL.create(..., 1, 1000, 300)");
		assertEquals("Reedwater", tReed.displayName(), "Loader_Fluids.java:461 verbatim");

		GTFluids.AquaFluidSpec tCactus = GTFluids.foodSpec("cactuswater");
		assertNotNull(tCactus);
		assertEquals(300, tCactus.temperature(), "Loader_Fluids.java:462 FL.create(..., 1, 1000, 300)");
		assertEquals("Cactuswater", tCactus.displayName(), "Loader_Fluids.java:462 verbatim");

		// sap — the HONEST DEFAULT row: no GT6 FL.create anywhere (FL.java:250 is an
		// external-name shorthand, the seawater/waterdirty precedent), so the declared
		// values are exactly the FluidType.Properties defaults (FluidType.java:924-926)
		GTFluids.AquaFluidSpec tSap = GTFluids.foodSpec("sap");
		assertNotNull(tSap);
		assertEquals(300, tSap.temperature(), "FluidType.java:925 default — the honest default, not fabricated");
		assertEquals("Sap", tSap.displayName(), "the FL.java:250 shorthand spelled out (the water_boiling \"Boiling Water\" precedent)");

		// every row rides the STATE_LIQUID carriers (FL.java:1104 viscosity; the three
		// FL.create rows carry the 1000 density literally, FL.java:1094 material-null)
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_FLUID_SPECS) {
			assertEquals(1000, tSpec.density(), tSpec.name() + ": the FL.create carrier / FluidType.java:924 default");
			assertEquals(1000, tSpec.viscosity(), tSpec.name() + ": FL.java:1104 STATE_LIQUID viscosity");
			assertEquals("fluid.gt6." + tSpec.name(), tSpec.descriptionId(), tSpec.name() + ": the descriptionId shape");
		}
	}

	/** All four sit at 300 K — under the 340 K wood-barrel ceiling (GTBarrelCommand.WOOD_MELTING_POINT) — the RCON chain carries them in wood barrels. */
	@Test
	public void everyFoodFluidIsWoodBarrelSafe() {
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_FLUID_SPECS) {
			assertTrue(tSpec.temperature() < 340, tSpec.name() + ": " + tSpec.temperature()
					+ " K must stay under the wood ceiling 340 K (GTBarrelCommand.WOOD_MELTING_POINT)");
		}
	}

	@Test
	public void unknownIdsResolveToNull() {
		assertNull(GTFluids.foodSpec("water"), "vanilla water is not a table row");
		assertNull(GTFluids.foodSpec("water_hot"), "the IC2 hot-water alias stays unregistered (the p19 ruling)");
		assertNull(GTFluids.foodSpec("rainbowsap"), "Sap_Rainbow (FL.java:251) is OUTSIDE the card's four");
		assertNull(GTFluids.foodSpec(null));
		// and the isolation is bidirectional: the new ids are NOT rows of the earlier tables either
		assertNull(GTFluids.aquaSpec("sap"), "sap lives on the THIRD table only");
		assertNull(GTFluids.simpleLiquidSpec("maplesap"), "maplesap lives on the THIRD table only");
	}

	/** The registration shape: source = the id, flowing = id + "_flowing" (the four-DR template). */
	@Test
	public void registrationShapeCarriesSourceAndFlowingIds() {
		for (GTFluids.AquaFluid tFamily : GTFluids.foodFluids()) {
			//? if forge {
			assertEquals(new ResourceLocation("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(new ResourceLocation("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			//?} else {
			/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			*///?}
			assertEquals(tFamily.spec, GTFluids.foodSpec(tFamily.spec.name()), "spec identity");
		}
		// the offline walk keeps the spec list and the live handles aligned, same order
		assertEquals(GTFluids.FOOD_FLUID_SPECS, GTFluids.foodFluids().stream().map(f -> f.spec).toList());
	}

	/** The fluid-only declaration: the food families ride the SHARED {@link GTFluids.AquaFluid} holder — the 4-field pin (no block face, no bucket face). */
	@Test
	public void foodFamiliesDeclareNoBlockOrBucketFace() {
		assertEquals(4, GTFluids.AquaFluid.class.getDeclaredFields().length,
				"spec + type + source + flowing — the shared fluid-only holder: adding a block/bucket field here would need a blockstate/JSON face");
	}

	/**
	 * The drying seam: the foodTable inputs are EXACTLY this table's four ids — every food
	 * row's input resolves through the food registrations (offline the ids/pinned
	 * constants are checked; the live RegistryObject resolution is the RCON chain's proof).
	 */
	@Test
	public void dryingFoodRowInputsAreCoveredByThisTable() {
		for (GT6RecipesDrying.FoodRow tRow : GT6RecipesDrying.foodTable()) {
			assertNotNull(GTFluids.foodSpec(tRow.input()),
					"food row " + tRow.note() + " input '" + tRow.input() + "' must be a registered food fluid");
		}
		// and the pinned constants really are this table's spellings (the drying resolver contract)
		assertEquals("sap", GT6RecipesDrying.FLUID_SAP);
		assertEquals("maplesap", GT6RecipesDrying.FLUID_MAPLESAP);
		assertEquals("reedwater", GT6RecipesDrying.FLUID_REEDWATER);
		assertEquals("cactuswater", GT6RecipesDrying.FLUID_CACTUSWATER);
	}
}
