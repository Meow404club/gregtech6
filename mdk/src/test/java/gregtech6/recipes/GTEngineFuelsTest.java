package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * The FM.Engine fuel table offline tests (task p12-engine-fuel-fluids acceptance a): the
 * row values transcribed from Loader_Fuels.java:77-120 (JetFuel 1536 / Kerosine = Petrol =
 * Diesel 448 / Fuel 512 / nitrofuel 768 / ethanol 144, the |EUt × duration| semantics of
 * Recipe.java:723-725), the fluid-only row shape, the end-to-end pour + fluid probe lookup,
 * and the unregistered-fluid silent-skip semantics. The registry itself is live-verified by
 * the RCON smoke chain (offline cannot touch the Forge registries).
 */
class GTEngineFuelsTest extends GTRecipesOfflineTestBase {

	private static Function<String, Fluid> sDefaultFluidResolver;

	/** The offline fixture: every gt6 fuel id resolves to the vanilla water fluid — the recipe mechanics only compare identities. */
	private static final Function<String, Fluid> WATER_FIXTURE = aId -> Fluids.WATER;

	@BeforeAll
	static void captureDefaults() {
		sDefaultFluidResolver = GT6RecipesEngineFuels.sFluidResolver;
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesEngineFuels.sFluidResolver = sDefaultFluidResolver;
		GT6RecipeMaps.reset();
		GT6RecipesEngineFuels.resetForTest();
	}

	/** The transcription walk: seven rows, values per Loader_Fuels.java:77-120, every row marked with the CO2 byproduct. */
	@Test
	void tableTranscribesTheUpstreamValues() {
		assertEquals(7, GT6RecipesEngineFuels.table().size());
		assertRow(":79",  "jetfuel"  , -128, 12, 1536);
		assertRow(":83",  "kerosine" , - 64,  7,  448);
		assertRow(":87",  "petrol"   , - 64,  7,  448);
		assertRow(":91",  "diesel"   , - 64,  7,  448);
		assertRow(":95",  "fuel"     , - 64,  8,  512);
		assertRow(":98",  "nitrofuel", - 64, 12,  768);
		assertRow(":102", "ethanol"  , - 16,  9,  144);
		for (GT6RecipesEngineFuels.FuelRow tRow : GT6RecipesEngineFuels.table()) {
			assertTrue(tRow.co2(), "row " + tRow.note() + ": every upstream :77-120 row emits FL.CarbonDioxide.make(1)");
		}
	}

	private void assertRow(String aNote, String aFluid, long aEUt, long aDuration, long aPower) {
		GT6RecipesEngineFuels.FuelRow tRow = GT6RecipesEngineFuels.table().stream()
				.filter(r -> r.note().equals(aNote)).findFirst().orElse(null);
		assertNotNull(tRow, "row " + aNote + " transcribed");
		assertEquals(aFluid, tRow.fluid());
		assertEquals(aEUt, tRow.eUt(), "row " + aNote + ": the verbatim negative EUt (generator convention)");
		assertEquals(aDuration, tRow.duration(), "row " + aNote + ": the verbatim duration");
		assertEquals(aPower, tRow.absolutePowerPerUnit(), "row " + aNote + ": |EUt x duration| (Recipe.java:723-725)");
	}

	/** The fluid-only row shape: one 1-unit fluid input, no items, no fluid outputs. */
	@Test
	void buildRecipeCarriesTheFluidOnlyShape() {
		GT6RecipesEngineFuels.sFluidResolver = WATER_FIXTURE;
		GT6RecipesEngineFuels.FuelRow tRow = GT6RecipesEngineFuels.table().get(0); // JetFuel
		Recipe tRecipe = GT6RecipesEngineFuels.buildRecipe(tRow);
		assertNotNull(tRecipe);
		assertEquals(0, tRecipe.mInputs.length, "fluid-only");
		assertEquals(0, tRecipe.mOutputs.length, "the CO2 output is the row's data mark, not a FluidStack (declared deviation)");
		assertEquals(1, tRecipe.mFluidInputs.length);
		assertEquals(1, tRecipe.mFluidInputs[0].getAmount(), "the upstream FL.X.list(1) amount");
		assertEquals(0, tRecipe.mFluidOutputs.length);
		assertEquals(12, tRecipe.mDuration);
		assertEquals(-128, tRecipe.mEUt, "the verbatim negative EUt");
		assertEquals(1536, tRecipe.getAbsoluteTotalPower(), "Recipe.java:723-725 semantics");
		assertTrue(tRecipe.mCanBeBuffered, "the addRecipe0(T, ...) buffered shape");
	}

	/** The end-to-end pour + the fluid probe lookup (the exact predicate findRecipe scans with, RecipeMap.java:147). */
	@Test
	void loadPoursSevenRowsAndTheFluidProbeFindsThem() {
		GT6RecipesEngineFuels.sFluidResolver = WATER_FIXTURE;
		GT6RecipesEngineFuels.load();
		assertEquals(7, GT6RecipeMaps.ENGINE_FUELS.mRecipeList.size(), "every row resolves against the fixture");

		// every row answers the 1-unit probe — the powers come back as the exact seven-row multiset
		// (the mRecipeList is a HashSet: the scan order is not the transcription order)
		FluidStack[] tTank = {new FluidStack(Fluids.WATER, 10)};
		List<Long> tPowers = new ArrayList<>();
		for (Recipe tRecipe : GT6RecipeMaps.ENGINE_FUELS.mRecipeList) {
			// the RecipeMap.findRecipe scan predicate (aDontCheckStackSizes=true probe)
			if (tRecipe.isRecipeInputEqual(false, true, tTank, new ItemStack[0])) tPowers.add(tRecipe.getAbsoluteTotalPower());
		}
		assertEquals(List.of(1536L, 768L, 512L, 448L, 448L, 448L, 144L), tPowers.stream().sorted(Comparator.reverseOrder()).toList(),
				"the probe answers for all seven rows (upstream :519-523 fluid-only lookup semantics at the predicate level)");

		// the apply path consumes exactly the row amount
		Recipe tAny = GT6RecipeMaps.ENGINE_FUELS.mRecipeList.iterator().next();
		FluidStack[] tBurn = {new FluidStack(Fluids.WATER, 10)};
		assertTrue(tAny.isRecipeInputEqual(true, false, tBurn, new ItemStack[0]));
		assertEquals(9, tBurn[0].getAmount(), "one fluid unit consumed");
		assertFalse(tAny.isRecipeInputEqual(false, false, new FluidStack[] {new FluidStack(Fluids.WATER, 0)}, new ItemStack[0]),
				"a zero tank cannot feed the row");
	}

	/** The unregistered-fluid semantics: the row skips SILENTLY (counted), the others pour. */
	@Test
	void unregisteredFluidRowsSkipSilently() {
		Function<String, Fluid> tDieselLess = aId -> "diesel".equals(aId) ? null : Fluids.WATER;
		// full fixture baseline
		GT6RecipesEngineFuels.sFluidResolver = WATER_FIXTURE;
		GT6RecipesEngineFuels.load();
		assertEquals(7, GT6RecipeMaps.ENGINE_FUELS.mRecipeList.size());
		long tFullPower = GT6RecipeMaps.ENGINE_FUELS.mRecipeList.stream().mapToLong(Recipe::getAbsoluteTotalPower).sum();

		// diesel lost its registration → exactly that row drops, the others pour untouched
		GT6RecipeMaps.reset();
		GT6RecipesEngineFuels.resetForTest();
		GT6RecipesEngineFuels.sFluidResolver = tDieselLess;
		GT6RecipesEngineFuels.load();
		assertEquals(6, GT6RecipeMaps.ENGINE_FUELS.mRecipeList.size(), "the six surviving rows pour");
		long tDieselLessPower = GT6RecipeMaps.ENGINE_FUELS.mRecipeList.stream().mapToLong(Recipe::getAbsoluteTotalPower).sum();
		assertEquals(448, tFullPower - tDieselLessPower, "exactly the Diesel 448 power/L left the table");

		// an id outside the table never pours either (the resolver seam is the only fluid source)
		GT6RecipeMaps.reset();
		GT6RecipesEngineFuels.resetForTest();
		GT6RecipesEngineFuels.sFluidResolver = aId -> null;
		GT6RecipesEngineFuels.load();
		assertEquals(0, GT6RecipeMaps.ENGINE_FUELS.mRecipeList.size(), "all rows skipped — no exception, no ghost rows");
	}

	/** load() is idempotent per generation; a reset re-pours cleanly. */
	@Test
	void loadIsIdempotentPerGeneration() {
		// self-grounding (ADR-P18): boot leftovers (21.1 junit-fml) or a prior class's unpaired
		// reset() never feed this test — the first load() below pours THIS method's generation.
		GT6RecipeMaps.reset();
		GT6RecipesEngineFuels.resetForTest();
		GT6RecipesEngineFuels.sFluidResolver = WATER_FIXTURE;
		GT6RecipesEngineFuels.load();
		GT6RecipesEngineFuels.load();
		assertEquals(7, GT6RecipeMaps.ENGINE_FUELS.mRecipeList.size(), "the second load() is a no-op");

		GT6RecipeMaps.reset();
		GT6RecipesEngineFuels.resetForTest();
		assertNull(GT6RecipeMaps.ENGINE_FUELS);
		GT6RecipesEngineFuels.load();
		assertEquals(7, GT6RecipeMaps.ENGINE_FUELS.mRecipeList.size(), "a fresh generation re-pours");
	}
}
