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
 * Aqua fluid family offline tests (task p16-aqua-fluids — the registration-row
 * assertions against the DECLARED values, the GTFluidsEngineFamilyTest shape): six
 * {@link GTFluids.AquaFluidSpec} rows with every value census-anchored —
 * spdew/mnwtr 300 K (Loader_Fluids.java:362/:371 {@code FL.create(..., 1, 1000, 300)}),
 * water_geothermal 320 K (:373), water_boiling the honest FluidType defaults
 * (FluidType.java:924-926 — no upstream GT6 definition exists, FL.java:117 is an
 * external-mod fluid), hot_water 333 K / cold_water 288 K (the FL.java:115/:114
 * {@code // 60°C} / {@code // 15°C} annotations over C = 273, CS.java:132). The live
 * registry side is the runServer smoke evidence (the per-fluid "GT6 fluid registered"
 * lines) and the RCON {@code /gt6tank fill} chain; offline asserts the declaration
 * table, the registration-shape ids and the drying-table seam.
 */
public class GTFluidsAquaFamilyTest extends GTOfflineTestBase {

	/** The six ids in declaration order — the card list, the GT6RecipesDrying pinned spellings. */
	private static final List<String> IDS = List.of("spdew", "mnwtr", "water_geothermal",
			"water_boiling", "hot_water", "cold_water");

	@Test
	public void tableCarriesSixRowsInDeclarationOrder() {
		assertEquals(IDS, GTFluids.AQUA_SPECS.stream().map(GTFluids.AquaFluidSpec::name).toList());
		assertEquals(IDS.size(), GTFluids.AQUA_SPECS.size());
	}

	/** The per-fluid declared values, one block per family (acceptance: 属性断言). */
	@Test
	public void declaredValuesMatchTheUpstreamAnchors() {
		// the three GT6-defined fluids: the FL.create temperature literals + display names verbatim
		GTFluids.AquaFluidSpec tSpDew = GTFluids.aquaSpec("spdew");
		assertNotNull(tSpDew);
		assertEquals(300, tSpDew.temperature(), "Loader_Fluids.java:362 FL.create(..., 1, 1000, 300)");
		assertEquals("Spectral Dew", tSpDew.displayName(), "Loader_Fluids.java:362 verbatim");

		GTFluids.AquaFluidSpec tMnWtr = GTFluids.aquaSpec("mnwtr");
		assertNotNull(tMnWtr);
		assertEquals(300, tMnWtr.temperature(), "Loader_Fluids.java:371 FL.create(..., 1, 1000, 300)");
		assertEquals("Mineral Water", tMnWtr.displayName(), "Loader_Fluids.java:371 verbatim");

		GTFluids.AquaFluidSpec tGeo = GTFluids.aquaSpec("water_geothermal");
		assertNotNull(tGeo);
		assertEquals(320, tGeo.temperature(), "Loader_Fluids.java:373 FL.create(..., 1, 1000, 320)");
		assertEquals("Hot Spring Water", tGeo.displayName(), "Loader_Fluids.java:373 verbatim");

		// water_boiling — the HONEST DEFAULT row: no upstream GT6 definition anywhere
		// (FL.java:117 is an external-mod fluid name; the recipe rows guard it with
		// FL.Water_Boiling.exists(), Loader_Recipes_Chem.java:529), so the declared
		// values are exactly the FluidType.Properties defaults (FluidType.java:924-926)
		GTFluids.AquaFluidSpec tBoiling = GTFluids.aquaSpec("water_boiling");
		assertNotNull(tBoiling);
		assertEquals(300, tBoiling.temperature(), "FluidType.java:925 default — the honest default, not fabricated");
		assertEquals("Boiling Water", tBoiling.displayName(), "the FL.java:117 shorthand spelled out");

		// the two external fluids the FL.java annotations temperature
		assertEquals(333, GTFluids.aquaSpec("hot_water").temperature(), "FL.java:115 // 60°C = C+60 = 333 (C = 273, CS.java:132)");
		assertEquals(288, GTFluids.aquaSpec("cold_water").temperature(), "FL.java:114 // 15°C = C+15 = 288");
		assertEquals("Hot Water", GTFluids.aquaSpec("hot_water").displayName());
		assertEquals("Cold Water", GTFluids.aquaSpec("cold_water").displayName());

		// every row rides the STATE_LIQUID carriers (FL.java:1104 viscosity; density left
		// at the FluidType default — material-null skips the FL.java:1124-1130 formula)
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.AQUA_SPECS) {
			assertEquals(1000, tSpec.density(), tSpec.name() + ": FluidType.java:924 default, the STATE_LIQUID carrier");
			assertEquals(1000, tSpec.viscosity(), tSpec.name() + ": FL.java:1104 STATE_LIQUID viscosity");
			assertEquals("fluid.gt6." + tSpec.name(), tSpec.descriptionId(), tSpec.name() + ": the descriptionId shape");
		}
	}

	/** All six sit under the 340 K wood-barrel ceiling (GTBarrelCommand.WOOD_MELTING_POINT) — the RCON chain carries them in wood barrels. */
	@Test
	public void everyAquaFluidIsWoodBarrelSafe() {
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.AQUA_SPECS) {
			assertTrue(tSpec.temperature() < 340, tSpec.name() + ": " + tSpec.temperature()
					+ " K must stay under the wood ceiling 340 K (GTBarrelCommand.WOOD_MELTING_POINT)");
		}
	}

	@Test
	public void unknownIdsResolveToNull() {
		assertNull(GTFluids.aquaSpec("water"), "vanilla water is not a table row");
		assertNull(GTFluids.aquaSpec("water_hot"), "the IC2 hot-water alias (FL.java:116) is OUTSIDE the card's six");
		assertNull(GTFluids.aquaSpec("steam"), "the engine family is outside the aqua table");
		assertNull(GTFluids.aquaSpec(null));
	}

	/** The registration shape: source = the id, flowing = id + "_flowing" (the four-DR template). */
	@Test
	public void registrationShapeCarriesSourceAndFlowingIds() {
		for (GTFluids.AquaFluid tFamily : GTFluids.aquaFluids()) {
			//? if forge {
			assertEquals(new ResourceLocation("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(new ResourceLocation("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			//?} else {
			/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			*///?}
			assertEquals(tFamily.spec, GTFluids.aquaSpec(tFamily.spec.name()), "spec identity");
		}
		// the offline walk keeps the spec list and the live handles aligned, same order
		assertEquals(GTFluids.AQUA_SPECS, GTFluids.aquaFluids().stream().map(f -> f.spec).toList());
	}

	/** The fluid-only declaration: the aqua families expose NO block handle (zero blockstate JSON face). */
	@Test
	public void aquaFamiliesDeclareNoBlockFace() {
		assertEquals(4, GTFluids.AquaFluid.class.getDeclaredFields().length,
				"spec + type + source + flowing — adding a block field here would need a blockstate JSON face");
	}

	/**
	 * The W2 drying seam: every Drying-table input id the drying-rows-backfill card will
	 * pour either is vanilla water (always resolves), or is one of THIS card's six rows,
	 * or is the declared-outside water_hot (the IC2 alias) whose row keeps the
	 * absent-fluid skip (GT6RecipesDrying.buildRecipe null arm).
	 */
	@Test
	public void dryingTableInputsAreCoveredExceptTheIc2Alias() {
		for (GT6RecipesDrying.DryingRow tRow : GT6RecipesDrying.table()) {
			if (GT6RecipesDrying.FLUID_WATER.equals(tRow.input())) continue;    // vanilla water, no registration involved
			if (GT6RecipesDrying.FLUID_HOT.equals(tRow.input())) continue;      // water_hot — outside the card's six, stays pooled
			assertNotNull(GTFluids.aquaSpec(tRow.input()),
					"drying row " + tRow.note() + " input '" + tRow.input() + "' must be a registered aqua fluid");
		}
		// and the pinned constants really are this table's spellings (the W2 contract)
		assertEquals("spdew", GT6RecipesDrying.FLUID_SPDEW);
		assertEquals("mnwtr", GT6RecipesDrying.FLUID_MNWTR);
		assertEquals("water_geothermal", GT6RecipesDrying.FLUID_GEOTHERMAL);
		assertEquals("water_boiling", GT6RecipesDrying.FLUID_BOILING);
		assertEquals("hot_water", GT6RecipesDrying.FLUID_HOT_WATER);
		assertEquals("cold_water", GT6RecipesDrying.FLUID_COLD);
	}
}
