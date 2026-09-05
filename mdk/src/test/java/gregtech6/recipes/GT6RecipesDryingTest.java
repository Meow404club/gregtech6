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
 * The DRYING water-family pour offline tests (task p14-loop-closure-chain acceptance,
 * extended by task p16-drying-rows-backfill): the row transcription of
 * Loader_Recipes_Chem.java:525-532, the seven-row pour (the Water 10 L → DistW 8 L
 * foundation plus the six p16-aqua-fluids rows, EUt 16, duration 16, the buffered
 * addRecipe0 shape), the :530 water_hot absent-fluid pool reconciliation, the
 * machine-shape findRecipe lookup, and the isRecipeInputEqual consume semantics. The
 * live loop itself is the RCON chain (offline cannot touch the Forge registries).
 */
class GT6RecipesDryingTest extends GTRecipesOfflineTestBase {

	private static Function<String, Fluid> sDefaultFluidResolver;

	/**
	 * The offline fixture: every REGISTERED fluid id — water, distw and the six
	 * p16-aqua-fluids ids — resolves to the vanilla water fluid (the recipe mechanics
	 * only compare identities — the GTEngineFuelsTest WATER_FIXTURE convention); the
	 * deliberately unregistered water_hot alias (:530) alone stays null, mirroring the
	 * live resolver's absent-fluid verdict.
	 */
	private static final Function<String, Fluid> CENSUS_FIXTURE = aId ->
			GT6RecipesDrying.FLUID_HOT.equals(aId) ? null : Fluids.WATER;

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

	/**
	 * The end-to-end pour: EXACTLY the seven registered water rows land (:525-529/:531-532),
	 * the :530 water_hot alias skips.
	 */
	@Test
	void loadPoursExactlyTheSevenWaterRows() {
		GT6RecipesDrying.sFluidResolver = CENSUS_FIXTURE;
		GT6RecipesDrying.load();
		assertEquals(7, GT6RecipeMaps.DRYING.mRecipeList.size(), "the seven registered water rows resolve; :530 pools");

		// the :525 Water row, exactly as poured since P14: 10 L in, 8 L out, EUt 16, duration 16
		Recipe tRecipe = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length > 0 && r.mFluidInputs[0].getAmount() == 10
						&& r.mFluidInputs[0].getFluid() == Fluids.WATER).findFirst().orElse(null);
		assertNotNull(tRecipe, "the :525 Water row is among the poured");
		assertSame(Fluids.WATER, tRecipe.mFluidInputs[0].getFluid(), "the vanilla water input");

		assertEquals(0, tRecipe.mInputs.length, "fluid-only: no item inputs (upstream ZL_IS)");
		assertEquals(0, tRecipe.mOutputs.length, "fluid-only: no item outputs");
		assertEquals(1, tRecipe.mFluidInputs.length);
		assertEquals(8, tRecipe.mFluidOutputs[0].getAmount(), "the :525 FL.DistW.make(8)");
		assertEquals(16, tRecipe.mDuration, "the verbatim :525 duration");
		assertEquals(16, tRecipe.mEUt, "the verbatim :525 EUt");
		assertTrue(tRecipe.mCanBeBuffered, "the addRecipe0(T, ...) buffered shape");
		assertEquals(256, tRecipe.getAbsoluteTotalPower(), "|EUt x duration| (Recipe.java:723-725 semantics)");

		// the :528 thermal row keeps its verbatim 25/20 split
		Recipe tGeo = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length > 0 && r.mFluidInputs[0].getAmount() == 25).findFirst().orElse(null);
		assertNotNull(tGeo, "a 25 L thermal row (:528/:529/:531) is among the poured");
		assertEquals(20, tGeo.mFluidOutputs[0].getAmount(), "the 25 → 20 thermal split");

		GT6RecipesDrying.load(); // idempotent: the second load is a no-op
		assertEquals(7, GT6RecipeMaps.DRYING.mRecipeList.size(), "load() is one pour per generation");
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

		// mRecipeList is a HashSet (RecipeMap.java:74) — with seven water-family rows the
		// linear scan's first match is hash-ordered, so the assertion is "a water-family
		// row answers the water tank", the amounts being exactly the registered {10, 25}
		Recipe tFound = GT6RecipeMaps.DRYING.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots);
		assertNotNull(tFound, "the Dryer T1 voltage (64) covers the rows' EUt 16");
		assertSame(Fluids.WATER, tFound.mFluidInputs[0].getFluid(), "the answering row consumes water");
		assertTrue(tFound.mFluidInputs[0].getAmount() == 10 || tFound.mFluidInputs[0].getAmount() == 25,
				"the answering row is one of the registered water-family splits (10 or 25 L)");

		assertNull(GT6RecipeMaps.DRYING.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.LAVA, 1000)}, tSlots), "lava in the tank finds nothing");
		assertNull(GT6RecipeMaps.DRYING.findRecipe(null, 0, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots), "size 0 fails the absGreaterEqual voltage gate");
		assertNull(GT6RecipeMaps.DRYING.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[0], tSlots), "an empty tank census finds nothing (Recipe.java:800 early false)");
	}

	/**
	 * The consume semantics (the :738/:744 two-stage contract): the probe leaves the tank
	 * snapshot untouched, the applied consume drains exactly the row's litre amount, and a
	 * short tank fails the amount check UNCHANGED (aDontCheckStackSizes=false on the apply
	 * path). Pinned on both the 10 L (:525) and the 25 L thermal split.
	 */
	@Test
	void isRecipeInputEqualConsumesExactlyTheRowLitres() {
		GT6RecipesDrying.sFluidResolver = CENSUS_FIXTURE;
		GT6RecipesDrying.load();
		ItemStack[] tSlots = new ItemStack[1];
		Recipe tTen = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length > 0 && r.mFluidInputs[0].getAmount() == 10).findFirst().orElse(null);
		assertNotNull(tTen, "a 10 L row (:525/:526/:527) is among the poured");

		FluidStack[] tProbe = {new FluidStack(Fluids.WATER, 1000)};
		assertTrue(tTen.isRecipeInputEqual(false, true, tProbe, tSlots), "the findRecipe probe shape matches");
		assertEquals(1000, tProbe[0].getAmount(), "the probe never consumes");

		FluidStack[] tConsume = {new FluidStack(Fluids.WATER, 1000)};
		assertTrue(tTen.isRecipeInputEqual(true, false, tConsume, tSlots), "the applied consume succeeds");
		assertEquals(990, tConsume[0].getAmount(), "exactly the row's 10 L are drained");

		FluidStack[] tShort = {new FluidStack(Fluids.WATER, 9)};
		assertFalse(tTen.isRecipeInputEqual(true, false, tShort, tSlots), "9 L cannot feed a 10 L row");
		assertEquals(9, tShort[0].getAmount(), "a failing consume leaves the tank untouched");

		Recipe tTwentyFive = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length > 0 && r.mFluidInputs[0].getAmount() == 25).findFirst().orElse(null);
		assertNotNull(tTwentyFive, "a 25 L thermal row (:528/:529/:531) is among the poured");
		FluidStack[] tThermal = {new FluidStack(Fluids.WATER, 1000)};
		assertTrue(tTwentyFive.isRecipeInputEqual(true, false, tThermal, tSlots), "the thermal consume succeeds");
		assertEquals(975, tThermal[0].getAmount(), "exactly the row's 25 L are drained");
	}

	/**
	 * The pool reconciliation after the p16 backfill: the :530 water_hot row is the ONLY
	 * water-family row the live resolver still answers null for (the IC2 alias
	 * "ic2hotwater", FL.java:116, deliberately unregistered by p16-aqua-fluids) — the
	 * upstream {@code if (FL.Water_Hot.exists())} guard shape, so load() skips it. The
	 * other six non-water ids resolve through live RegistryObjects, which only exist under
	 * a real registry event — their live resolution is the RCON chain's proof, offline the
	 * pour tests carry them through fixtures.
	 */
	@Test
	void theWaterHotRowAlonePools() {
		assertEquals(7, GT6RecipesDrying.table().stream()
				.filter(r -> !GT6RecipesDrying.FLUID_WATER.equals(r.input())).count(), "five 25/20 rows + two 10/8 rows beyond the Water row");
		GT6RecipesDrying.DryingRow tHot = GT6RecipesDrying.table().stream()
				.filter(r -> r.note().equals(":530")).findFirst().orElse(null);
		assertNotNull(tHot, "the :530 row is transcribed");
		assertEquals(GT6RecipesDrying.FLUID_HOT, tHot.input(), "the :530 input is the water_hot alias");
		assertNull(GT6RecipesDrying.resolveFluid(GT6RecipesDrying.FLUID_HOT),
				"the live census: water_hot has no port fluid — the :530 row pools");
		assertNull(GT6RecipesDrying.buildRecipe(new GT6RecipesDrying.DryingRow(":530", GT6RecipesDrying.FLUID_HOT, 25, 20)),
				"a pooled row builds nothing (the silent-skip shape)");
	}
}
