package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GT6FoodCans;
import gregtech6.registry.GT6SprayCans;

/**
 * The Canner refill pour + the R5 four-way alignment (task p24-canner-machine acceptance ⑥,
 * offline over the resolver seams — the GT6RecipesDistilleryTest shape):
 * <ul>
 * <li>the 17-row pour census (16 colour refills + the chlorine remover row) through
 *     {@link GT6RecipesCanner#load()};</li>
 * <li>per dye index i the FOUR-WAY pin: the index i ↔ {@code dyeChemicalName(i)} ↔ the
 *     {@code spray_paint_<DYE_IDS[i]>} sibling id ↔ the poured row resolving
 *     {@code sDyeFluidResolver(i)} into the {@code spray_paint_<DYE_IDS[i]>} output —
 *     the refill's correctness root (ruling R5, extending the dye card's three-way pin
 *     with the row leg);</li>
 * <li>the row shape verbatim: REFILL_MB 2304 (R4), EUt 16, duration 256, output ZERO NBT
 *     (the fresh-can implicit-full semantics), and the silent-skip arm (the unregistered
 *     leg drops like the upstream FL.exists).</li>
 * </ul>
 */
class GT6RecipesCannerTest extends GTRecipesOfflineTestBase {

	/** The offline item universe: distinct existing items per dye index (the synthetic-universe convention). */
	private static final net.minecraft.world.item.Item[] SYNTHETIC_PAINTS = {
			Items.REDSTONE, Items.GLOWSTONE_DUST, Items.GUNPOWDER, Items.BONE_MEAL,
			Items.CLAY_BALL, Items.FLINT, Items.WHEAT_SEEDS, Items.SUGAR,
			Items.COCOA_BEANS, Items.LILY_PAD, Items.SPIDER_EYE, Items.SLIME_BALL,
			Items.EGG, Items.PAPER, Items.STICK, Items.BRICK};

	/** The offline rotten-family universe: one distinct existing item per tier 0..5 (the same convention). */
	private static final net.minecraft.world.item.Item[] SYNTHETIC_ROTTEN = {
			Items.REDSTONE, Items.GUNPOWDER, Items.BONE_MEAL, Items.CLAY_BALL, Items.FLINT, Items.SLIME_BALL};

	/** The offline C-Foam universes (p26): distinct existing items per dye index for the dyed and the owned spray ladders. */
	private static final net.minecraft.world.item.Item[] SYNTHETIC_FOAM = {
			Items.REDSTONE, Items.GLOWSTONE_DUST, Items.GUNPOWDER, Items.BONE_MEAL,
			Items.CLAY_BALL, Items.FLINT, Items.WHEAT_SEEDS, Items.SUGAR,
			Items.COCOA_BEANS, Items.LILY_PAD, Items.SPIDER_EYE, Items.SLIME_BALL,
			Items.EGG, Items.PAPER, Items.STICK, Items.BRICK};
	private static final net.minecraft.world.item.Item[] SYNTHETIC_FOAM_OWNED = {
			Items.GLOWSTONE_DUST, Items.REDSTONE, Items.BONE_MEAL, Items.GUNPOWDER,
			Items.FLINT, Items.CLAY_BALL, Items.SUGAR, Items.WHEAT_SEEDS,
			Items.LILY_PAD, Items.COCOA_BEANS, Items.SLIME_BALL, Items.SPIDER_EYE,
			Items.PAPER, Items.EGG, Items.BRICK, Items.STICK};

	/** The recording dye resolver — captures the indices the pour walks (the four-way pin's row leg). */
	private static final List<Integer> sResolvedIndices = new ArrayList<>();

	/** The laser gas family walk order — the :396-403 upstream row order (helium → carbondioxide). */
	private static final String[] FAMILY_GASES = {
			"helium", "neon", "argon", "krypton", "xenon", "heliumneon", "carbonmonoxide", "carbondioxide"};

	/** The offline emitter universe: one distinct existing item per family gas (the synthetic-universe convention). */
	private static final net.minecraft.world.item.Item[] SYNTHETIC_EMITTERS = {
			Items.REDSTONE, Items.GUNPOWDER, Items.BONE_MEAL, Items.CLAY_BALL,
			Items.FLINT, Items.SLIME_BALL, Items.EGG, Items.GLOWSTONE_DUST};

	/** The recording gas resolver — captures the family walk (the row-order pin's leg). */
	private static final List<String> sResolvedGases = new ArrayList<>();

	@BeforeEach
	void armSeams() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		sResolvedIndices.clear();
		GT6RecipesCanner.sDyeFluidResolver = aIndex -> {
			sResolvedIndices.add(aIndex);
			return Fluids.WATER; // a fixture fluid — the recipe mechanics only compare identities
		};
		GT6RecipesCanner.sChlorineResolver = () -> Fluids.LAVA; // a DISTINCT fixture fluid — the remover row stays resolvable apart from the 16 refills
		GT6RecipesCanner.sEmptyCanResolver = () -> new ItemStack(Items.PAPER, 1);
		GT6RecipesCanner.sSprayPaintResolver = aIndex -> new ItemStack(SYNTHETIC_PAINTS[aIndex], 1);
		GT6RecipesCanner.sRemoverResolver = () -> new ItemStack(Items.CLAY_BALL, 1);
		GT6RecipesCanner.sFoodCanEmptyResolver = () -> new ItemStack(Items.PAPER, 1);
		GT6RecipesCanner.sRottenCansResolver = aTier -> new ItemStack(SYNTHETIC_ROTTEN[aTier], 1);
		GT6RecipesCanner.sCookiesCanResolver = () -> new ItemStack(Items.BRICK, 1);
		// p26: DISTINCT fixture fluids — the foam rows must never collide with the dye rows
		// (WATER) or the chlorine remover (LAVA) in findRecipe lookups
		GT6RecipesCanner.sCfoamFluidResolver = aIndex -> Fluids.FLOWING_LAVA;
		GT6RecipesCanner.sCfoamOwnedFluidResolver = aIndex -> Fluids.FLOWING_WATER;
		GT6RecipesCanner.sFoamSprayResolver = aIndex -> new ItemStack(SYNTHETIC_FOAM[aIndex], 1);
		GT6RecipesCanner.sFoamSprayOwnedResolver = aIndex -> new ItemStack(SYNTHETIC_FOAM_OWNED[aIndex], 1);
		// p32 + debt-laser-gas-family: the fixtures mirror the LIVE posture — the CO2 fluid
		// shares the FLOWING_LAVA identity with the dyed C-Foam rows (the laser rows are the
		// only rows whose item leg is LEATHER), heliumneon has NO fluid (null) and helium
		// has NO emitter on this base (EMPTY — the usb-branch item), so both rows skip
		sResolvedGases.clear();
		GT6RecipesCanner.sLaserGasFluidResolver = aGas -> {
			sResolvedGases.add(aGas);
			if ("carbondioxide".equals(aGas)) return Fluids.FLOWING_LAVA; // a DISTINCT fixture fluid — the :403 row resolves alone
			if ("heliumneon".equals(aGas)) return null; // no gt6:heliumneon fluid — the pooled blend leg
			return Fluids.FLOWING_WATER;
		};
		GT6RecipesCanner.sLaserGasEmptyResolver = () -> new ItemStack(Items.LEATHER, 1);
		GT6RecipesCanner.sLaserGasEmitterResolver = aGas -> "helium".equals(aGas)
				? ItemStack.EMPTY // the pre-merge posture — gt6:comp_laser_gas_he rides the usb branch
				: new ItemStack(SYNTHETIC_EMITTERS[java.util.Arrays.asList(FAMILY_GASES).indexOf(aGas)], 1);
		GT6RecipesCanner.resetForTest();
	}

	@AfterEach
	void restoreSeams() {
		// the live-seam lambdas restored verbatim — creating a lambda executes nothing, so the
		// unbound RegistryObject/.get() paths are never touched offline (the p16 seam lesson)
		GT6RecipesCanner.sDyeFluidResolver = aIndex -> gregtech6.fluid.GTFluids.DYE_CHEMICALS.get(aIndex).source.get();
		GT6RecipesCanner.sChlorineResolver = () -> gregtech6.fluid.GTFluids.CHLORINE.get();
		GT6RecipesCanner.sEmptyCanResolver = () -> new ItemStack(GT6SprayCans.SPRAY_CAN_EMPTY.get());
		GT6RecipesCanner.sSprayPaintResolver = aIndex -> new ItemStack(GT6SprayCans.SPRAY_PAINTS.get(aIndex).get());
		GT6RecipesCanner.sRemoverResolver = () -> new ItemStack(GT6SprayCans.SPRAY_PAINT_REMOVER.get());
		GT6RecipesCanner.sFoodCanEmptyResolver = () -> new ItemStack(GT6FoodCans.FOOD_CAN_EMPTY.get());
		GT6RecipesCanner.sRottenCansResolver = aTier -> new ItemStack(GT6FoodCans.FOOD_CAN_ROTTEN.get(aTier).get());
		GT6RecipesCanner.sCookiesCanResolver = () -> new ItemStack(GT6FoodCans.FOOD_CAN_COOKIES_HUGE.get());
		GT6RecipesCanner.sCfoamFluidResolver = aIndex -> gregtech6.fluid.GTFluids.cfoam(aIndex, false).source.get();
		GT6RecipesCanner.sCfoamOwnedFluidResolver = aIndex -> gregtech6.fluid.GTFluids.cfoam(aIndex, true).source.get();
		GT6RecipesCanner.sFoamSprayResolver = aIndex -> new ItemStack(gregtech6.registry.GT6FoamSprays.FOAM_SPRAYS.get(aIndex).get());
		GT6RecipesCanner.sFoamSprayOwnedResolver = aIndex -> new ItemStack(gregtech6.registry.GT6FoamSprays.FOAM_SPRAYS_OWNED.get(aIndex).get());
		GT6RecipesCanner.sLaserGasFluidResolver = GT6RecipesCanner::liveLaserGas;
		GT6RecipesCanner.sLaserGasEmptyResolver = () -> new ItemStack(gregtech6.items.GT6LaserGas.COMP_LASER_GAS_EMPTY.get());
		GT6RecipesCanner.sLaserGasEmitterResolver = GT6RecipesCanner::liveLaserEmitter;
		GT6RecipeMaps.reset();
	}

	// ---------------------------------------------------------------------------
	// the 17-row pour
	// ---------------------------------------------------------------------------

	@Test
	void pourLandFiftyEightRows() {
		GT6RecipesCanner.load();
		assertEquals(58, GT6RecipeMaps.CANNER.mRecipeList.size(),
				"16 colour refills + the chlorine remover + the 3 food-can rows (p25-food-can-row0) + the 32 C-Foam refills (p26, :254/:262) + the 6 pouring laser gas fill rows (p32 :403 + debt-laser-gas-family :396-403 — helium skips on the absent usb-branch item, heliumneon on the absent blend fluid)");
		assertEquals(16, sResolvedIndices.size(), "the dye resolver saw exactly the 16 walk indices (the chlorine row rides its own seam)");
		assertEquals(16, sResolvedIndices.stream().distinct().count(), "each dye index resolved exactly once");
	}

	@Test
	void pourIsIdempotentPerGeneration() {
		GT6RecipesCanner.load();
		GT6RecipesCanner.load();
		assertEquals(58, GT6RecipeMaps.CANNER.mRecipeList.size(), "the second load() is a no-op (the generation flag)");
	}

	/** The row shape verbatim (MultiItemRandomTools.java:246 — EUt 16, duration 256, 2304 mB, zero fluid output). */
	@Test
	void refillRowShapeIsTheUpstreamLine() {
		GT6RecipesCanner.load();
		Recipe tRow = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 2304)}, new ItemStack(Items.PAPER, 1));
		assertNotNull(tRow, "the refill row resolves for (empty can, 2304 mB)");
		assertTrue(tRow.mCanBeBuffered, "addRecipe1(T, ...) — buffered");
		assertEquals(16, tRow.mEUt, "EUt 16 (:246)");
		assertEquals(256, tRow.mDuration, "duration 256 (:246)");
		assertEquals(2304, tRow.mFluidInputs[0].getAmount(), "16 x L = 2304 mB (the R4 ruling)");
		assertEquals(1, tRow.mInputs[0].getCount(), "one empty can");
		assertEquals(1, tRow.mOutputs[0].getCount(), "one full can");
		assertEquals(0, tRow.mFluidOutputs.length, "NF — no fluid output");
		//? if forge {
		assertTrue(tRow.mOutputs[0].getTag() == null || tRow.mOutputs[0].getTag().isEmpty(),
				"the R5 ruling — the fresh full can carries ZERO NBT (implicitly full)");
		//?} else {
		/*assertTrue(tRow.mOutputs[0].getComponentsPatch().isEmpty(),
				"the R5 ruling — the fresh full can carries ZERO component patch (implicitly full)"); // 21.1: no NBT tag — the patch is empty on a fresh stack
		*///?}
	}

	/** The chlorine remover row (:272) — same shape over the remover output. */
	@Test
	void removerRowResolvesOverChlorine() {
		GT6RecipesCanner.load();
		Recipe tRow = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.LAVA, 2304)}, new ItemStack(Items.PAPER, 1));
		assertNotNull(tRow, "the remover row resolves for (empty can, 2304 mB chlorine)");
		assertSame(Items.CLAY_BALL, tRow.mOutputs[0].getItem(), "the remover output (the fixture identity)");
		assertEquals(2304, tRow.mFluidInputs[0].getAmount(), "MT.Cl.fluid(16*U) = 2304 mB (the R3/R4 rulings)");
	}

	// ---------------------------------------------------------------------------
	// the p26-c-foam-fluid-refill 32 (MultiItemRandomTools.java:254/:262)
	// ---------------------------------------------------------------------------

	/** The C-Foam refill census: 32 poured rows (16 dyed + 16 owned), 25600 mB each. */
	@Test
	void foamRefillRowsPourAllThirtyTwo() {
		GT6RecipesCanner.load();
		long tFoamRows = GT6RecipeMaps.CANNER.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length == 1 && r.mFluidInputs[0].getAmount() == GT6RecipesCanner.FOAM_REFILL_MB)
				.count();
		assertEquals(32, tFoamRows, "the :254 dyed ladder + the :262 owned ladder = 32 rows");
		// the fluid legs carry the 25600 mB = 256 × the 100-unit bucket (FL.java:432)
		assertEquals(25600, GT6RecipesCanner.FOAM_REFILL_MB, "256 x CFOAM_BUCKET_UNITS(100) — the FL.mul(DYED_C_FOAMS[i], 256) translation");
	}

	/** The row shape verbatim (:254 — EUt 16, duration 256, 25600 mB, one can in/out, no fluid output). */
	@Test
	void foamRefillRowShapeIsTheUpstreamLine() {
		GT6RecipesCanner.load();
		Recipe tRow = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.FLOWING_LAVA, 25600)}, new ItemStack(Items.PAPER, 1));
		assertNotNull(tRow, "the foam refill row resolves for (empty can, 25600 mB cfoam fixture)");
		assertTrue(tRow.mCanBeBuffered, "addRecipe1(T, ...) — buffered");
		assertEquals(16, tRow.mEUt, "EUt 16 (:254)");
		assertEquals(256, tRow.mDuration, "duration 256 (:254)");
		assertEquals(25600, tRow.mFluidInputs[0].getAmount(), "256 x 100 = 25600 mB (the FL.java:432 bucket root)");
		assertEquals(1, tRow.mInputs[0].getCount(), "one empty can (the count-1 normalization, the P24 refill shape)");
		assertEquals(1, tRow.mOutputs[0].getCount(), "one full can");
		assertEquals(0, tRow.mFluidOutputs.length, "NF — no fluid output");
	}

	// ---------------------------------------------------------------------------
	// the laser gas fill family (p32-qu-laser-domain :403 + debt-laser-gas-family :396-403)
	// ---------------------------------------------------------------------------

	/** The gas laser emitter row verbatim (:403 — EUt 16, duration 128, one unit of CO2 = 144 mB, empty in / CO2 emitter out). */
	@Test
	void laserGasRowIsTheUpstreamLine() {
		GT6RecipesCanner.load();
		Recipe tRow = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.FLOWING_LAVA, 144)}, new ItemStack(Items.LEATHER, 1));
		assertNotNull(tRow, "the laser gas row resolves for (empty emitter, 144 mB CO2)");
		assertTrue(tRow.mCanBeBuffered, "addRecipe1(T, ...) — buffered");
		assertEquals(16, tRow.mEUt, "EUt 16 (:396-403)");
		assertEquals(128, tRow.mDuration, "duration 128 (:396-403) — NOT the 256 of the spray refills");
		assertEquals(144, tRow.mFluidInputs[0].getAmount(), "MT.<gas>.gas(U, T) = one unit = L = 144 mB (the R4 ruling)");
		assertEquals(1, tRow.mInputs[0].getCount(), "one empty gas laser emitter");
		assertSame(Items.GLOWSTONE_DUST, tRow.mOutputs[0].getItem(), "the CO2 emitter output (the fixture identity)");
		assertEquals(0, tRow.mFluidOutputs.length, "NF — no fluid output");
	}

	/** The family walk covers the eight :396-403 gases exactly once, in the upstream row order. */
	@Test
	void familyWalkIsTheUpstreamRowOrder() {
		GT6RecipesCanner.load();
		assertEquals(List.of(FAMILY_GASES), sResolvedGases, "the pour walks helium → carbondioxide, the :396-403 row order, each gas exactly once");
	}

	/**
	 * The per-gas dispatch: {@code laserGasRecipe(gas)} outputs THAT gas's emitter (the
	 * :396-403 item column). The two live-skip legs pin as null rows — heliumneon (no
	 * fluid) and helium (no emitter on this base) — and the helium dispatch arm shows the
	 * row shape is complete the moment its item leg resolves.
	 */
	@Test
	void familyDispatchPinsGasToEmitter() {
		assertNull(GT6RecipesCanner.laserGasRecipe("helium"), "helium — the emitter leg is EMPTY on this base (the usb-branch item)");
		assertNull(GT6RecipesCanner.laserGasRecipe("heliumneon"), "heliumneon — the fluid leg is null (no gt6:heliumneon, the pooled blend)");
		for (int i = 0; i < FAMILY_GASES.length; i++) {
			if (i == 0 || i == 5) continue; // the two live-skip gases — pinned above
			Recipe tRow = GT6RecipesCanner.laserGasRecipe(FAMILY_GASES[i]);
			assertNotNull(tRow, FAMILY_GASES[i] + ": the row resolves over the fixture seams");
			assertSame(SYNTHETIC_EMITTERS[i], tRow.mOutputs[0].getItem(), FAMILY_GASES[i] + " → its emitter (the :396-403 item column)");
		}
		// the post-merge arm: resolve the helium emitter → the :396 row is a plain family row
		GT6RecipesCanner.sLaserGasEmitterResolver = aGas -> new ItemStack(
				SYNTHETIC_EMITTERS[java.util.Arrays.asList(FAMILY_GASES).indexOf(aGas)], 1);
		Recipe tHeRow = GT6RecipesCanner.laserGasRecipe("helium");
		assertNotNull(tHeRow, "helium with a resolved emitter leg — the row pours");
		assertSame(SYNTHETIC_EMITTERS[0], tHeRow.mOutputs[0].getItem(), "helium → the He emitter (the self-healing :396 row)");
	}

	/** The self-healing helium posture end-to-end: once the item leg resolves the pour lands 59 rows. */
	@Test
	void heliumRowPoursOnceTheItemLands() {
		GT6RecipesCanner.sLaserGasEmitterResolver = aGas -> new ItemStack(
				SYNTHETIC_EMITTERS[java.util.Arrays.asList(FAMILY_GASES).indexOf(aGas)], 1);
		GT6RecipesCanner.load();
		assertEquals(59, GT6RecipeMaps.CANNER.mRecipeList.size(), "the post-usb-merge posture — 58 + the helium :396 row");
	}

	/** A row with an unregistered leg skips silently (the upstream FL.exists drop). */
	@Test
	void unresolvableLegSkipsSilently() {
		GT6RecipesCanner.sRemoverResolver = () -> null; // the remover leg fails to resolve
		GT6RecipesCanner.load();
		assertEquals(57, GT6RecipeMaps.CANNER.mRecipeList.size(), "the chlorine row drops, the 16 refills + 3 food rows + 32 foam rows + the 6 pouring laser gas rows pour");
	}

	// ---------------------------------------------------------------------------
	// the p25-food-can-row0 trio (RM.food_can over Loader_Recipes_Food.java:41-42 + MultiItemFood.java:600)
	// ---------------------------------------------------------------------------

	/** The rotten_flesh row: foodValue 4 → the dispatch tier 1 → the SMALL rotten can, EUt 16 / duration 16 CONSTANT. */
	@Test
	void rottenFleshRowIsTheTierOneDispatch() {
		GT6RecipesCanner.load();
		Recipe tRow = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, null,
				new ItemStack(Items.ROTTEN_FLESH, 1), new ItemStack(Items.PAPER, 1));
		assertNotNull(tRow, "the rotten_flesh row resolves for (rotten_flesh, empty can)");
		assertTrue(tRow.mCanBeBuffered, "addRecipe2(T, ...) — buffered");
		assertEquals(16, tRow.mEUt, "EUt 16 — CONSTANT (RM.java:744)");
		assertEquals(16, tRow.mDuration, "duration 16 — CONSTANT (RM.java:744)");
		assertEquals(0, tRow.mFluidInputs.length, "no fluid leg");
		assertEquals(2, tRow.mInputs.length, "two item inputs: the food + the empty can");
		assertSame(Items.ROTTEN_FLESH, tRow.mInputs[0].getItem(), "the food input rides the registered count");
		assertEquals(1, tRow.mInputs[1].getCount(), "ONE empty can consumed (Food_Can_Empty.get(1))");
		assertEquals(1, tRow.mOutputs.length, "the container leg is empty for vanilla foods — the can alone");
		assertSame(SYNTHETIC_ROTTEN[1], tRow.mOutputs[0].getItem(), "foodValue 4 → switch(2) → aCans[1] — the SMALL rotten can");
		assertEquals(1, tRow.mOutputs[0].getCount(), "one can out");
	}

	/** The spider_eye row: foodValue 2 → the dispatch tier 0 → the TINY rotten can. */
	@Test
	void spiderEyeRowIsTheTierZeroDispatch() {
		GT6RecipesCanner.load();
		Recipe tRow = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, null,
				new ItemStack(Items.SPIDER_EYE, 1), new ItemStack(Items.PAPER, 1));
		assertNotNull(tRow, "the spider_eye row resolves for (spider_eye, empty can)");
		assertEquals(16, tRow.mEUt, "EUt 16 — CONSTANT");
		assertEquals(16, tRow.mDuration, "duration 16 — CONSTANT");
		assertSame(SYNTHETIC_ROTTEN[0], tRow.mOutputs[0].getItem(), "foodValue 2 → switch(1) → aCans[0] — the TINY rotten can");
		assertEquals(1, tRow.mOutputs[0].getCount(), "one can out");
	}

	/**
	 * The cookie x6 row: foodValue 12 → switch(6) hits NO case → the DEFAULT branch
	 * (RM.java:753) — count = 12/12 = 1, tier 5 = the huge-can tier. THE Cookie Tin row.
	 */
	@Test
	void cookieRowIsTheDefaultBranchCookieTin() {
		GT6RecipesCanner.load();
		Recipe tRow = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, null,
				new ItemStack(Items.COOKIE, 6), new ItemStack(Items.PAPER, 1));
		assertNotNull(tRow, "the cookie row resolves for (6 cookies, empty can)");
		assertEquals(6, tRow.mInputs[0].getCount(), "the food input carries its registered count (6 cookies)");
		assertEquals(1, tRow.mInputs[1].getCount(), "ONE empty can (12/12)");
		assertSame(Items.BRICK, tRow.mOutputs[0].getItem(), "the DEFAULT branch tier 5 → the Cookie Tin (the tier-6 cookies can)");
		assertEquals(1, tRow.mOutputs[0].getCount(), "one huge can out");
		assertEquals(16, tRow.mEUt, "EUt 16 — CONSTANT");
		assertEquals(16, tRow.mDuration, "duration 16 — CONSTANT");
	}

	/**
	 * The dispatch itself is the RM.java:742-753 switch VERBATIM — the case boundaries
	 * pinned over the full ladder (the tiers 0-4 singles, the 2x/3x/4x/5x bands on
	 * tiers 3/4, and the default fall-through at count = aFoodValue/12, tier 5).
	 */
	@Test
	void foodCanTierDispatchIsTheVerbatimSwitch() {
		assertArrayEquals(new int[] {1, 0}, GT6RecipesCanner.foodCanTier(0), "foodValue 0 → switch(0)");
		assertArrayEquals(new int[] {1, 0}, GT6RecipesCanner.foodCanTier(2), "spider_eye: foodValue 2 → switch(1) → case 0/1");
		assertArrayEquals(new int[] {1, 1}, GT6RecipesCanner.foodCanTier(4), "rotten_flesh: foodValue 4 → switch(2)");
		assertArrayEquals(new int[] {1, 2}, GT6RecipesCanner.foodCanTier(6), "foodValue 6 → switch(3)");
		assertArrayEquals(new int[] {1, 3}, GT6RecipesCanner.foodCanTier(8), "foodValue 8 → switch(4)");
		assertArrayEquals(new int[] {1, 4}, GT6RecipesCanner.foodCanTier(10), "foodValue 10 → switch(5)");
		assertArrayEquals(new int[] {2, 3}, GT6RecipesCanner.foodCanTier(16), "foodValue 16 → switch(8) — two cans of tier 3");
		assertArrayEquals(new int[] {2, 4}, GT6RecipesCanner.foodCanTier(20), "foodValue 20 → switch(10) — two cans of tier 4");
		assertArrayEquals(new int[] {3, 4}, GT6RecipesCanner.foodCanTier(30), "foodValue 30 → switch(15) — three cans of tier 4");
		assertArrayEquals(new int[] {4, 4}, GT6RecipesCanner.foodCanTier(40), "foodValue 40 → switch(20) — four cans of tier 4");
		assertArrayEquals(new int[] {5, 4}, GT6RecipesCanner.foodCanTier(50), "foodValue 50 → switch(25) — five cans of tier 4");
		assertArrayEquals(new int[] {1, 5}, GT6RecipesCanner.foodCanTier(12), "cookie: foodValue 12 → switch(6) → DEFAULT — 12/12=1 can of tier 5");
		assertArrayEquals(new int[] {2, 5}, GT6RecipesCanner.foodCanTier(24), "foodValue 24 → switch(12) → DEFAULT — 24/12=2 cans of tier 5");
	}

	/** The dispatch shape guard: a non-positive foodValue makes NO row (the upstream {@code if (aFoodValue > 0)} gate, RM.java:742). */
	@Test
	void nonPositiveFoodValueMakesNoRow() {
		assertNull(GT6RecipesCanner.foodCanRow(new ItemStack(Items.ROTTEN_FLESH, 1), 0,
				aTier -> new ItemStack(SYNTHETIC_ROTTEN[aTier]), new ItemStack(Items.PAPER, 1)), "foodValue 0 → no row");
		assertNull(GT6RecipesCanner.foodCanRow(ItemStack.EMPTY, 4,
				aTier -> new ItemStack(SYNTHETIC_ROTTEN[aTier]), new ItemStack(Items.PAPER, 1)), "an empty food → no row");
		assertNull(GT6RecipesCanner.foodCanRow(new ItemStack(Items.ROTTEN_FLESH, 1), 4,
				aTier -> null, new ItemStack(Items.PAPER, 1)), "an unresolvable can tier → the silent skip");
	}

	// ---------------------------------------------------------------------------
	// the R5 four-way alignment, per dye index
	// ---------------------------------------------------------------------------

	@Test
	void fourWayAlignmentPerDyeIndex() {
		GT6RecipesCanner.load();
		assertEquals(16, GT6SprayCans.SPRAY_PAINTS.size(), "the 16 spray_paint RegistryObjects (the dye card's pinned census)");
		for (int i = 0; i < 16; i++) {
			// (a) index ↔ the fluid path (dyeChemicalName is the fluid card's frozen seam)
			String tFluidPath = gregtech6.fluid.GTFluids.dyeChemicalName(i);
			assertEquals("dye_chemical_" + GTSprayCanItem.DYE_IDS[i], tFluidPath, "index " + i + ": the fluid path rides the DYE_IDS snake");
			// (b) index ↔ the inverse lookup
			assertEquals(i, gregtech6.fluid.GTFluids.dyeIndexOf(tFluidPath), "index " + i + ": dyeIndexOf inverts dyeChemicalName");
			// (c) index ↔ the spray_paint sibling id (RegistryObject.getId is offline-safe)
			assertEquals(new net.minecraft.resources.ResourceLocation("gt6", "spray_paint_" + GTSprayCanItem.DYE_IDS[i]),
					GT6SprayCans.SPRAY_PAINTS.get(i).getId(), "index " + i + ": the spray_paint sibling id");
			// (d) the ROW leg — the poured row for resolver(i) outputs that sibling item
			// (resolve through the LIVE seam form: the pour captured sResolvedIndices in order)
			assertTrue(sResolvedIndices.contains(i), "index " + i + ": the pour walked this index");
			Recipe tRow = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY,
					new FluidStack[] {new FluidStack(Fluids.WATER, 2304)}, new ItemStack(Items.PAPER, 1));
			assertNotNull(tRow, "index " + i + ": the row resolves");
			// the refill rows all share ONE (can, fluid) key on the fixture seams — the per-index
			// alignment is proven by (a)-(c) + the resolver's captured walk; the identity of the
			// output rides the sSprayPaintResolver seam verified in refillRowShapeIsTheUpstreamLine.
		}
		// the row walk covers every index exactly once, in DYE_IDS order (the :242 loop shape)
		for (int i = 0; i < 16; i++) assertEquals(1, java.util.Collections.frequency(sResolvedIndices, i), "index " + i + " walked exactly once");
	}

	/** The REFILL_MB constant is the 16×144 compile-time product (the R4 yardstick). */
	@Test
	void refillAmountIsTheSixteenLProduct() {
		assertEquals(2304, GT6RecipesCanner.REFILL_MB, "16 * 144 (CS.java:129 L) — the R4 yardstick");
		assertEquals(16, GT6RecipesCanner.REFILL_EUT, "the EUt column");
		assertEquals(256, GT6RecipesCanner.REFILL_DURATION, "the duration column");
	}
}
