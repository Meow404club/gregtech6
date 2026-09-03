package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * The DRYING water-family pour offline tests (task p14-loop-closure-chain acceptance):
 * the row transcription of Loader_Recipes_Chem.java:525-532, the one-row pour (Water 10 L
 * → DistW 8 L, EUt 16, duration 16, the buffered addRecipe0 shape), the census-pool
 * reconciliation of the seven absent-fluid rows, the machine-shape findRecipe lookup, and
 * the isRecipeInputEqual consume semantics. The live loop itself is the RCON chain
 * (offline cannot touch the Forge registries).
 */
class GT6RecipesDryingTest extends GTRecipesOfflineTestBase {

	private static Function<String, Fluid> sDefaultFluidResolver;

	/**
	 * The offline fixture: water AND distw resolve to the vanilla water fluid (the recipe
	 * mechanics only compare identities — the GTEngineFuelsTest WATER_FIXTURE convention),
	 * everything else to null — the null arms mirror the live census verdicts
	 * (SpDew/MnWtr/thermal family unregistered here).
	 */
	private static final Function<String, Fluid> CENSUS_FIXTURE = aId ->
			GT6RecipesDrying.FLUID_WATER.equals(aId) || GT6RecipesDrying.FLUID_DISTW.equals(aId)
					? Fluids.WATER : null;

	@BeforeAll
	static void captureDefaults() {
		sDefaultFluidResolver = GT6RecipesDrying.sFluidResolver;
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesDrying.sFluidResolver = sDefaultFluidResolver;
		GT6RecipeMaps.reset();
		GT6RecipesDrying.resetForTest();
	}

	/** The transcription walk: eight rows, values per Loader_Recipes_Chem.java:525-532. */
	@Test
	void tableTranscribesTheUpstreamWaterFamily() {
		assertEquals(8, GT6RecipesDrying.table().size());
		assertRow(":525", GT6RecipesDrying.FLUID_WATER     , 10,  8);
		assertRow(":526", GT6RecipesDrying.FLUID_SPDEW     , 10,  8);
		assertRow(":527", GT6RecipesDrying.FLUID_MNWTR     , 10,  8);
		assertRow(":528", GT6RecipesDrying.FLUID_GEOTHERMAL, 25, 20);
		assertRow(":529", GT6RecipesDrying.FLUID_BOILING   , 25, 20);
		assertRow(":530", GT6RecipesDrying.FLUID_HOT       , 25, 20);
		assertRow(":531", GT6RecipesDrying.FLUID_HOT_WATER , 25, 20);
		assertRow(":532", GT6RecipesDrying.FLUID_COLD      , 25, 20);
	}

	private void assertRow(String aNote, String aFluid, long aIn, long aOut) {
		GT6RecipesDrying.DryingRow tRow = GT6RecipesDrying.table().stream()
				.filter(r -> r.note().equals(aNote)).findFirst().orElse(null);
		assertNotNull(tRow, "row " + aNote + " transcribed");
		assertEquals(aFluid, tRow.input(), "row " + aNote + ": the input fluid id");
		assertEquals(aIn, tRow.inAmount(), "row " + aNote + ": the verbatim input litres");
		assertEquals(aOut, tRow.outAmount(), "row " + aNote + ": the verbatim distilled output litres");
	}

	/** The end-to-end pour: EXACTLY the :525 Water row lands, the seven census rows skip. */
	@Test
	void loadPoursExactlyTheWaterRow() {
		GT6RecipesDrying.sFluidResolver = CENSUS_FIXTURE;
		GT6RecipesDrying.load();
		assertEquals(1, GT6RecipeMaps.DRYING.mRecipeList.size(), "only the :525 Water row resolves against the port census");
		Recipe tRecipe = GT6RecipeMaps.DRYING.mRecipeList.iterator().next();

		assertEquals(0, tRecipe.mInputs.length, "fluid-only: no item inputs (upstream ZL_IS)");
		assertEquals(0, tRecipe.mOutputs.length, "fluid-only: no item outputs");
		assertEquals(1, tRecipe.mFluidInputs.length);
		assertEquals(10, tRecipe.mFluidInputs[0].getAmount(), "the :525 FL.Water.make(10)");
		assertSame(Fluids.WATER, tRecipe.mFluidInputs[0].getFluid(), "the vanilla water input");
		assertEquals(1, tRecipe.mFluidOutputs.length);
		assertEquals(8, tRecipe.mFluidOutputs[0].getAmount(), "the :525 FL.DistW.make(8)");
		assertEquals(16, tRecipe.mDuration, "the verbatim :525 duration");
		assertEquals(16, tRecipe.mEUt, "the verbatim :525 EUt");
		assertTrue(tRecipe.mCanBeBuffered, "the addRecipe0(T, ...) buffered shape");
		assertEquals(256, tRecipe.getAbsoluteTotalPower(), "|EUt x duration| (Recipe.java:723-725 semantics)");

		GT6RecipesDrying.load(); // idempotent: the second load is a no-op
		assertEquals(1, GT6RecipeMaps.DRYING.mRecipeList.size(), "load() is one pour per generation");
	}

	/**
	 * The machine-shape lookup: the Dryer's checkRecipe probes with the REAL input-tank
	 * snapshot and the length-1 slot array (TileEntityBasicMachine.java:512/:531 — the
	 * RecipeMap's empty-array hard return needs the length, the row needs no item), and
	 * the row answers for water but not for another fluid.
	 */
	@Test
	void machineShapeFindRecipeHitsTheWaterRow() {
		GT6RecipesDrying.sFluidResolver = CENSUS_FIXTURE;
		GT6RecipesDrying.load();
		ItemStack[] tSlots = new ItemStack[1]; // the empty input slot, the live :512 shape

		Recipe tFound = GT6RecipeMaps.DRYING.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots);
		assertNotNull(tFound, "the Dryer T1 voltage (64) covers the row's EUt 16");
		assertEquals(10, tFound.mFluidInputs[0].getAmount(), "the found row IS the Water row");

		assertNull(GT6RecipeMaps.DRYING.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.LAVA, 1000)}, tSlots), "lava in the tank finds nothing");
		assertNull(GT6RecipeMaps.DRYING.findRecipe(null, 0, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots), "size 0 fails the absGreaterEqual voltage gate");
		assertNull(GT6RecipeMaps.DRYING.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[0], tSlots), "an empty tank census finds nothing (Recipe.java:800 early false)");
	}

	/**
	 * The consume semantics (the :738/:744 two-stage contract): the probe leaves the tank
	 * snapshot untouched, the applied consume drains exactly the row's 10 L, and a 9 L
	 * tank fails the amount check UNCHANGED (aDontCheckStackSizes=false on the apply path).
	 */
	@Test
	void isRecipeInputEqualConsumesExactlyTenLitres() {
		GT6RecipesDrying.sFluidResolver = CENSUS_FIXTURE;
		GT6RecipesDrying.load();
		Recipe tRecipe = GT6RecipeMaps.DRYING.mRecipeList.iterator().next();
		ItemStack[] tSlots = new ItemStack[1];

		FluidStack[] tProbe = {new FluidStack(Fluids.WATER, 1000)};
		assertTrue(tRecipe.isRecipeInputEqual(false, true, tProbe, tSlots), "the findRecipe probe shape matches");
		assertEquals(1000, tProbe[0].getAmount(), "the probe never consumes");

		FluidStack[] tConsume = {new FluidStack(Fluids.WATER, 1000)};
		assertTrue(tRecipe.isRecipeInputEqual(true, false, tConsume, tSlots), "the applied consume succeeds");
		assertEquals(990, tConsume[0].getAmount(), "exactly the row's 10 L are drained");

		FluidStack[] tShort = {new FluidStack(Fluids.WATER, 9)};
		assertFalse(tRecipe.isRecipeInputEqual(true, false, tShort, tSlots), "9 L cannot feed the 10 L row");
		assertEquals(9, tShort[0].getAmount(), "a failing consume leaves the tank untouched");
	}

	/**
	 * The pooled-census reconciliation: the seven non-water rows (:526-532) carry no
	 * port fluid — resolveFluid answers null for each (the full GTFluids census: no
	 * SpDew/MnWtr/geothermal/boiling/hot/cold registration), so load() skips them with
	 * the upstream absent-fluid semantics. The backfill pool card pours them by deleting
	 * the null arm, not by touching the table.
	 */
	@Test
	void theSevenCensusRowsPoolLoad() {
		GT6RecipesDrying.sFluidResolver = sDefaultFluidResolver; // the LIVE resolver, offline: distw alone would bounce, the census rows null out
		assertEquals(7, GT6RecipesDrying.table().stream()
				.filter(r -> !GT6RecipesDrying.FLUID_WATER.equals(r.input())).count(), "five 25/20 rows + two 10/8 rows beyond the Water row");
		for (GT6RecipesDrying.DryingRow tRow : GT6RecipesDrying.table()) {
			if (GT6RecipesDrying.FLUID_WATER.equals(tRow.input())) continue;
			assertNull(GT6RecipesDrying.resolveFluid(tRow.input()),
					"the census: " + tRow.input() + " (" + tRow.note() + ") has no port fluid — the row pools");
		}
		assertNull(GT6RecipesDrying.buildRecipe(new GT6RecipesDrying.DryingRow(":526", GT6RecipesDrying.FLUID_SPDEW, 10, 8)),
				"a pooled row builds nothing (the silent-skip shape)");
	}
}
