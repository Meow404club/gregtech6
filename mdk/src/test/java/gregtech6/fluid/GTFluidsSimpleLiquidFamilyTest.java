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
 * Simple-liquid fluid family offline tests (task p19-drying-rows-backfill-2 — the
 * registration-row assertions against the DECLARED values, the GTFluidsAquaFamilyTest
 * shape): two {@link GTFluids.AquaFluidSpec} rows on the SECOND table — seawater
 * (FL.java:125, the Ocean shorthand) and waterdirty (FL.java:127, the Dirty_Water
 * shorthand). Both carry ONLY the SIMPLE+LIQUID flags upstream — no FOOD/WATER/BATH —
 * which is exactly why the architect ruling keeps them OUT of {@link GTFluids#AQUA_SPECS}
 * (that family is exactly-order-pinned to its WATER-tagged six). Neither fluid has a GT6
 * {@code FL.create} upstream (external-mod fluid names; GT6 only attaches the
 * Loader_Fluids.java:365/:366 FoodStatDrink "Dirty"/"Salty" drink stats — drink stats,
 * not fluid declarations, not transcribed), so the declared values are the HONEST
 * FluidType defaults (FluidType.java:924-926, 300 K / 1000 / 1000 — the water_boiling
 * precedent) and the tints are port-owned declared values. The consumers are the two
 * unguarded Drying rows :548/:553 (GT6RecipesDrying.saltTable).
 */
public class GTFluidsSimpleLiquidFamilyTest extends GTOfflineTestBase {

	/** The two ids in declaration order — the card list, the GT6RecipesDrying pinned spellings. */
	private static final List<String> IDS = List.of("seawater", "waterdirty");

	@Test
	public void tableCarriesTwoRowsInDeclarationOrder() {
		assertEquals(IDS, GTFluids.SIMPLE_LIQUID_SPECS.stream().map(GTFluids.AquaFluidSpec::name).toList());
		assertEquals(IDS.size(), GTFluids.SIMPLE_LIQUID_SPECS.size());
	}

	/** The cross-family contract: the aqua table is UNCHANGED by the second table (the exact-order aqua assertion stays green). */
	@Test
	public void theAquaTableIsUntouched() {
		assertEquals(6, GTFluids.AQUA_SPECS.size(), "the p16-aqua-fluids six, never appended");
		assertEquals(List.of("spdew", "mnwtr", "water_geothermal", "water_boiling", "hot_water", "cold_water"),
				GTFluids.AQUA_SPECS.stream().map(GTFluids.AquaFluidSpec::name).toList());
	}

	/** The per-fluid declared values, one block per family (acceptance: 属性断言). */
	@Test
	public void declaredValuesMatchTheHonestDefaults() {
		GTFluids.AquaFluidSpec tSeawater = GTFluids.simpleLiquidSpec("seawater");
		assertNotNull(tSeawater);
		assertEquals(300, tSeawater.temperature(), "FluidType.java:925 default — the honest default, not fabricated");
		assertEquals("Seawater", tSeawater.displayName(), "the common-name spelling (the water_boiling \"Boiling Water\" precedent)");

		GTFluids.AquaFluidSpec tDirty = GTFluids.simpleLiquidSpec("waterdirty");
		assertNotNull(tDirty);
		assertEquals(300, tDirty.temperature(), "FluidType.java:925 default — the honest default, not fabricated");
		assertEquals("Dirty Water", tDirty.displayName(), "the common-name spelling");

		// both rows ride the STATE_LIQUID carriers (FL.java:1104 viscosity; density left at
		// the FluidType default — material-null skips the FL.java:1124-1130 formula)
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.SIMPLE_LIQUID_SPECS) {
			assertEquals(1000, tSpec.density(), tSpec.name() + ": FluidType.java:924 default, the STATE_LIQUID carrier");
			assertEquals(1000, tSpec.viscosity(), tSpec.name() + ": FL.java:1104 STATE_LIQUID viscosity");
			assertEquals("fluid.gt6." + tSpec.name(), tSpec.descriptionId(), tSpec.name() + ": the descriptionId shape");
			assertTrue(tSpec.temperature() < 340, tSpec.name() + ": wood-barrel safe (under the 340 K ceiling)");
		}
	}

	@Test
	public void unknownIdsResolveToNull() {
		assertNull(GTFluids.simpleLiquidSpec("water"), "vanilla water is not a table row");
		assertNull(GTFluids.simpleLiquidSpec("water_hot"), "the IC2 hot-water alias stays unregistered (the p19 ruling)");
		assertNull(GTFluids.simpleLiquidSpec("spdew"), "the aqua family is outside the simple-liquid table");
		assertNull(GTFluids.simpleLiquidSpec(null));
		// and the isolation is bidirectional: the new ids are NOT aqua rows either
		assertNull(GTFluids.aquaSpec("seawater"), "seawater lives on the SECOND table only (the architect ruling)");
		assertNull(GTFluids.aquaSpec("waterdirty"), "waterdirty lives on the SECOND table only");
	}

	/** The registration shape: source = the id, flowing = id + "_flowing" (the four-DR template). */
	@Test
	public void registrationShapeCarriesSourceAndFlowingIds() {
		for (GTFluids.AquaFluid tFamily : GTFluids.simpleLiquids()) {
			//? if forge {
			assertEquals(new ResourceLocation("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(new ResourceLocation("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			//?} else {
			/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			*///?}
			assertEquals(tFamily.spec, GTFluids.simpleLiquidSpec(tFamily.spec.name()), "spec identity");
		}
		// the offline walk keeps the spec list and the live handles aligned, same order
		assertEquals(GTFluids.SIMPLE_LIQUID_SPECS, GTFluids.simpleLiquids().stream().map(f -> f.spec).toList());
	}

	/**
	 * The W2 drying seam: the saltTable inputs are EXACTLY this table's two ids — every
	 * salt row's input resolves through the simple-liquid registrations (offline the
	 * ids/pinned constants are checked; the live RegistryObject resolution is the RCON
	 * chain's proof).
	 */
	@Test
	public void dryingSaltRowInputsAreCoveredByThisTable() {
		for (GT6RecipesDrying.SaltRow tRow : GT6RecipesDrying.saltTable()) {
			assertNotNull(GTFluids.simpleLiquidSpec(tRow.input()),
					"salt row " + tRow.note() + " input '" + tRow.input() + "' must be a registered simple liquid");
		}
		// and the pinned constants really are this table's spellings (the drying resolver contract)
		assertEquals("seawater", GT6RecipesDrying.FLUID_SEAWATER);
		assertEquals("waterdirty", GT6RecipesDrying.FLUID_WATERDIRTY);
	}
}
