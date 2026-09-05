package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import gregtech6.item.GT6Circuits;

/**
 * The DISTILLERY water-family pour offline acceptance (task p16-distillery-family ③): the
 * row transcription of Loader_Recipes_Chem.java:534-541, the seven-row pour / one-skip
 * census reconciliation (the :539 water_hot IC2 alias has no port fluid — the upstream
 * {@code if (FL.Water_Hot.exists())} drop), the circuit-selector INPUT TRUTH TABLE (config
 * 0 matches, config 1 routes elsewhere, a tag-less circuit matches nothing, the circuit is
 * never consumed — the ① identity-skip), and the machine-shape findRecipe lookup. The live
 * loop is the RCON chain (offline cannot touch the Forge registries).
 */
public class GT6RecipesDistilleryTest extends GTRecipesOfflineTestBase {

	private static Function<String, Fluid> sDefaultFluidResolver;
	private static Function<Integer, ItemStack> sDefaultCircuitResolver;
	private static java.util.function.Predicate<ItemStack> sDefaultNotConsumable;

	/** The fixture circuit stack: a tagged BRICKS stack stands in for the circuit item — the
	 * vanilla item registry freezes at bootstrap (the intrusive holder makes {@code new Item}
	 * throw offline, the GT6RecipesShCLTest synthetic-item convention); the helpers are
	 * item-agnostic, the configuration rides the ItemStack NBT. Public — the row-test e2e in
	 * the machines package shares it. */
	public static ItemStack fixtureCircuit(int aConfig) {
		return GT6Circuits.selector(Items.BRICKS, aConfig);
	}

	private static ItemStack circuit(int aConfig) {
		return fixtureCircuit(aConfig);
	}

	/**
	 * The offline fixture predicate for {@link Recipe#sNotConsumable}: the fixture stacks are
	 * the tagged BRICKS stands-in, so the never-consumed arm keys on the tag (the production
	 * binding is {@code GT6Circuits::isSelector}, the live identity proven by the RCON chain).
	 * Public — the row-test e2e swaps it in for its own run.
	 */
	public static final java.util.function.Predicate<ItemStack> FIXTURE_NOT_CONSUMABLE = aStack ->
			!aStack.isEmpty() && aStack.is(Items.BRICKS) && GT6Circuits.hasConfigurationTag(aStack);

	/**
	 * The offline fixture: water, distw AND the five registered aqua ids resolve to the
	 * vanilla water fluid (the recipe mechanics only compare identities — the
	 * GTEngineFuelsTest WATER_FIXTURE convention); water_hot (the :539 IC2 alias) resolves
	 * to null — the one census skip.
	 */
	private static final Function<String, Fluid> CENSUS_FIXTURE = aId ->
			GT6RecipesDistillery.FLUID_HOT.equals(aId) ? null : Fluids.WATER;

	@BeforeAll
	static void captureDefaults() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		sDefaultFluidResolver = GT6RecipesDistillery.sFluidResolver;
		sDefaultCircuitResolver = GT6RecipesDistillery.sCircuitResolver;
		sDefaultNotConsumable = Recipe.sNotConsumable;
		Recipe.sNotConsumable = FIXTURE_NOT_CONSUMABLE;
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesDistillery.sFluidResolver = sDefaultFluidResolver;
		GT6RecipesDistillery.sCircuitResolver = sDefaultCircuitResolver;
		GT6RecipeMaps.reset();
		GT6RecipesDistillery.resetForTest();
	}

	@AfterAll
	static void restorePredicate() {
		Recipe.sNotConsumable = sDefaultNotConsumable;
	}

	// ------------------------------------------------------------------
	// the transcription walk — the upstream :534-541 columns
	// ------------------------------------------------------------------

	@Test
	void tableTranscribesTheUpstreamWaterFamily() {
		assertEquals(8, GT6RecipesDistillery.table().size());
		assertRow(":534", GT6RecipesDistillery.FLUID_WATER     , 10,  8);
		assertRow(":535", GT6RecipesDistillery.FLUID_SPDEW     , 10,  8);
		assertRow(":536", GT6RecipesDistillery.FLUID_MNWTR     , 10,  8);
		assertRow(":537", GT6RecipesDistillery.FLUID_GEOTHERMAL, 25, 20);
		assertRow(":538", GT6RecipesDistillery.FLUID_BOILING   , 25, 20);
		assertRow(":539", GT6RecipesDistillery.FLUID_HOT       , 25, 20);
		assertRow(":540", GT6RecipesDistillery.FLUID_HOT_WATER , 25, 20);
		assertRow(":541", GT6RecipesDistillery.FLUID_COLD      , 25, 20);
	}

	private void assertRow(String aNote, String aFluid, long aIn, long aOut) {
		GT6RecipesDistillery.DistilleryRow tRow = GT6RecipesDistillery.table().stream()
				.filter(r -> r.note().equals(aNote)).findFirst().orElse(null);
		assertNotNull(tRow, "row " + aNote + " transcribed");
		assertEquals(aFluid, tRow.input(), "row " + aNote + ": the input fluid id");
		assertEquals(aIn, tRow.inAmount(), "row " + aNote + ": the verbatim input litres");
		assertEquals(aOut, tRow.outAmount(), "row " + aNote + ": the verbatim DistW litres");
	}

	// ------------------------------------------------------------------
	// the pour: exactly seven rows land, the :539 alias skips
	// ------------------------------------------------------------------

	@Test
	void loadPoursExactlyTheSevenResolvableRows() {
		GT6RecipesDistillery.sFluidResolver = CENSUS_FIXTURE;
		GT6RecipesDistillery.sCircuitResolver = aConfig -> circuit(aConfig);
		GT6RecipesDistillery.load();
		assertEquals(7, GT6RecipeMaps.DISTILLERY.mRecipeList.size(),
				"the :534-538/:540/:541 seven rows resolve; the :539 water_hot alias skips");

		for (Recipe tRecipe : GT6RecipeMaps.DISTILLERY.mRecipeList) {
			assertEquals(1, tRecipe.mInputs.length, "every row carries the one circuit input");
			assertTrue(GT6Circuits.hasConfigurationTag(tRecipe.mInputs[0]), "the input carries the selector tag (the fixture circuit stand-in)");
			assertEquals(0, GT6Circuits.configurationOf(tRecipe.mInputs[0]), "every :534-541 row carries ST.tag(0)");
			assertEquals(1, tRecipe.mInputs[0].getCount(), "count 1 — the portable size-0 stand-in");
			assertEquals(1, tRecipe.mFluidInputs.length);
			assertSame(Fluids.WATER, tRecipe.mFluidInputs[0].getFluid(), "the fixture identity");
			assertEquals(1, tRecipe.mFluidOutputs.length);
			assertEquals(16, tRecipe.mDuration, "the verbatim :534-541 duration (the port ctor order is (duration, EUt))");
			assertEquals(16, tRecipe.mEUt, "the verbatim :534-541 EUt");
			assertTrue(tRecipe.mCanBeBuffered, "the addRecipe1(T, ...) buffered shape");
			assertEquals(0, tRecipe.mOutputs.length, "no item outputs (upstream ZL_IS)");
		}
		// the two litre pairs: three 10→8 rows + five 25→20 rows (wait — the census: three
		// 10/8 rows :534-536, four 25/20 rows :537-538/:540-541, and :539 skips)
		long tTen = GT6RecipeMaps.DISTILLERY.mRecipeList.stream().filter(r -> r.mFluidInputs[0].getAmount() == 10).count();
		long tTwentyFive = GT6RecipeMaps.DISTILLERY.mRecipeList.stream().filter(r -> r.mFluidInputs[0].getAmount() == 25).count();
		assertEquals(3, tTen, "the Water/SpDew/MnWtr 10→8 trio");
		assertEquals(4, tTwentyFive, "the Geothermal/Boiling/HotWater/Cold 25→20 quartet");

		GT6RecipesDistillery.load(); // idempotent: the second load is a no-op
		assertEquals(7, GT6RecipeMaps.DISTILLERY.mRecipeList.size(), "load() is one pour per generation");
	}

	/** The :539 IC2 alias row builds nothing against the LIVE resolver — the census null arm (no registry touch offline). */
	@Test
	void theWaterHotAliasRowPoolsOn() {
		assertNull(GT6RecipesDistillery.resolveFluid(GT6RecipesDistillery.FLUID_HOT),
				"water_hot has no port registration (the aqua card's outside-the-six ruling) — the row pools");
		assertNotNull(GT6RecipesDistillery.resolveFluid(GT6RecipesDistillery.FLUID_WATER), "water stays the vanilla fluid");
	}

	// ------------------------------------------------------------------
	// the circuit INPUT TRUTH TABLE
	// ------------------------------------------------------------------

	/** The water row under test (fixture-poured, the :534 transcription). */
	private static Recipe waterRow() {
		GT6RecipesDistillery.sFluidResolver = CENSUS_FIXTURE;
		GT6RecipesDistillery.sCircuitResolver = aConfig -> circuit(aConfig);
		GT6RecipesDistillery.resetForTest();
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		GT6RecipesDistillery.load();
		return GT6RecipeMaps.DISTILLERY.mRecipeList.stream()
				.filter(r -> r.mFluidInputs[0].getAmount() == 10).findFirst().orElseThrow();
	}

	@Test
	void truthTableConfigZeroMatchesAndRuns() {
		Recipe tRow = waterRow();
		ItemStack[] tSlots = {circuit(0)};

		assertTrue(tRow.isRecipeInputEqual(false, true, new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots),
				"[circuit@0 + water] matches — the upstream ST.tag(0) bucket");
		// the applied consume: the WATER drains 10 L, the CIRCUIT survives untouched
		FluidStack[] tConsume = {new FluidStack(Fluids.WATER, 1000)};
		ItemStack tCircuit = circuit(0);
		assertTrue(tRow.isRecipeInputEqual(true, false, tConsume, new ItemStack[] {tCircuit}),
				"the applied consume succeeds with the circuit in the slot");
		assertEquals(990, tConsume[0].getAmount(), "exactly the row's 10 L of water are drained");
		assertEquals(1, tCircuit.getCount(), "the circuit is NEVER consumed — the upstream size-0 net effect (the ① identity-skip)");
		assertEquals(0, GT6Circuits.configurationOf(tCircuit), "the circuit's configuration tag survives the consume");
	}

	@Test
	void truthTableConfigOneRoutesAway() {
		Recipe tRow = waterRow();
		// the Chem.java:333-vs-:346 premise: the SAME input fluid behind a different tag(1)
		// selector is a DIFFERENT row — the water row must not answer a config-1 circuit
		ItemStack[] tSlots = {circuit(1)};
		assertFalse(tRow.isRecipeInputEqual(false, true, new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots),
				"[circuit@1 + water] does NOT match the tag(0) water row — the selector routing");
		assertNull(GT6RecipeMaps.DISTILLERY.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots),
				"no poured row answers a config-1 circuit (all seven carry tag(0))");
	}

	@Test
	void truthTableMissingAndWrongInputsRefuse() {
		Recipe tRow = waterRow();

		assertNull(GT6RecipeMaps.DISTILLERY.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, new ItemStack[1]),
				"[no circuit + water] finds nothing — the RM.java:70 mMinimalInputItems=1 item leg is real");
		assertFalse(tRow.isRecipeInputEqual(false, true, new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, new ItemStack[] {Items.BRICKS.getDefaultInstance()}),
				"[a plain item + water] does not match — the circuit item identity is required");
		assertFalse(tRow.isRecipeInputEqual(false, true, new FluidStack[] {new FluidStack(Fluids.LAVA, 1000)}, new ItemStack[] {circuit(0)}),
				"[circuit@0 + wrong fluid] does not match — the fluid leg is real");
		assertFalse(tRow.isRecipeInputEqual(false, true, new FluidStack[0], new ItemStack[] {circuit(0)}),
				"[circuit@0 + no fluid] does not match (the Recipe.java:800 early false)");
		assertFalse(tRow.isRecipeInputEqual(true, false, new FluidStack[] {new FluidStack(Fluids.WATER, 9)}, new ItemStack[] {circuit(0)}),
				"9 L cannot feed the 10 L row (the amount check)");
	}

	/** A tag-less circuit (a /give'd stack, no Damage key) matches NO poured row — the exact-tag matching, declared. */
	@Test
	void truthTableTagLessCircuitMatchesNothing() {
		Recipe tRow = waterRow();
		ItemStack[] tSlots = {new ItemStack(Items.BRICKS, 1)};
		assertFalse(tRow.isRecipeInputEqual(false, true, new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots),
				"the tagged recipe input does not match a tag-less stack (ItemStack.isSameItemSameTags)");
	}

	/** The selector routing at the Recipe layer: same fluid, tag(0) vs tag(1) rows answer their own circuit only (the Chem:333/:346 pair). */
	@Test
	void truthTableSelectorRoutesSameFluidRows() {
		Recipe tRow0 = waterRow();
		// the synthetic :346-shaped sibling: identical fluid legs, the tag(1) selector, a
		// different output amount (the Glycerol branch's port stand-in)
		Recipe tRow1 = new Recipe(true,
				new ItemStack[] {circuit(1)}, new ItemStack[0],
				new FluidStack[] {new FluidStack(Fluids.WATER, 10)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 5)},
				16, 16, 0);

		ItemStack[] tSlots0 = {circuit(0)};
		ItemStack[] tSlots1 = {circuit(1)};
		assertTrue(tRow0.isRecipeInputEqual(false, true, new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots0), "tag(0) row answers circuit@0");
		assertFalse(tRow0.isRecipeInputEqual(false, true, new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots1), "tag(0) row refuses circuit@1");
		assertTrue(tRow1.isRecipeInputEqual(false, true, new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots1), "tag(1) row answers circuit@1");
		assertFalse(tRow1.isRecipeInputEqual(false, true, new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots0), "tag(1) row refuses circuit@0");
	}

	// ------------------------------------------------------------------
	// the machine-shape lookup + the parallel count helper (the consume loop)
	// ------------------------------------------------------------------

	@Test
	void machineShapeFindRecipeAndCountConsume() {
		Recipe tRow = waterRow();
		ItemStack[] tSlots = {circuit(0)};

		Recipe tFound = GT6RecipeMaps.DISTILLERY.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 40)}, tSlots);
		assertNotNull(tFound, "the T1 voltage (64) covers the row's EUt 16");
		// the map is a HashSet and the fixture rows are identity-identical — ANY poured row
		// may answer first; the found row carries the family shape (the tag(0) input, the
		// 16/16 constants, the DistW-style fluid output)
		assertEquals(0, GT6Circuits.configurationOf(tFound.mInputs[0]), "the tag(0) selector input");
		assertEquals(16, tFound.mEUt, "the verbatim :534-541 EUt");
		assertEquals(16, tFound.mDuration, "the verbatim :534-541 duration");
		assertEquals(1, tFound.mFluidOutputs.length, "the single DistW output leg");

		// the TileEntityBasicMachine :744-751 count loop: consume one recipe-worth at a time
		// — the water drains each round, the circuit NEVER does
		FluidStack[] tTanks = {new FluidStack(Fluids.WATER, 40)};
		ItemStack tCircuit = circuit(0);
		ItemStack[] tLive = {tCircuit};
		assertTrue(tRow.isRecipeInputEqual(true, false, tTanks, tLive), "consume 1");
		assertTrue(tRow.isRecipeInputEqual(true, false, tTanks, tLive), "consume 2");
		assertTrue(tRow.isRecipeInputEqual(true, false, tTanks, tLive), "consume 3");
		assertTrue(tRow.isRecipeInputEqual(true, false, tTanks, tLive), "consume 4");
		assertEquals(0, tTanks[0].getAmount(), "4 x 10 L drained");
		assertEquals(1, tCircuit.getCount(), "the circuit outlives all four consumes");
		assertFalse(tRow.isRecipeInputEqual(true, false, tTanks, tLive), "the fifth consume starves on water");
	}
}
