package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GTMaterialItems;

/**
 * The findRecipe hash-index restoration (task p32-perf-recipe-hash-index): the upstream
 * Recipe.java:469-551 lookup structure (item/tag/fluid buckets + the map-level
 * {@code oRecipe} buffer) replacing the linear-scan port, with the per-value equivalence
 * contract as the hard acceptance.
 *
 * <ul>
 * <li><b>Equivalence (acceptance ①)</b>: for the fully-poured real census (every
 *     RecipeMap × representative query shapes derived from its own rows) AND a strict
 *     synthetic map, the indexed {@code findRecipe} and the retired linear scan agree on
 *     the SAME instance for every unambiguous query. A disagreement is legal only under
 *     ambiguity (two+ rows matching the same inputs — the HashSet identity-order
 *     coin-flip the FusionTest incident documented); the classifier arm pins the weaker
 *     honest contract there: the indexed result is a matching stored row (or a
 *     first-match gate-fail null).</li>
 * <li><b>Buffer (acceptance ②)</b>: a repeat lookup with the same inputs returns the same
 *     recipe WITHOUT re-entering the index — proven by removing the row from the list
 *     behind the buffer's back and observing the buffer still serve it.</li>
 * <li><b>Self-heal</b>: the two declared direct-write seams (the CokeOven tag listener
 *     adds, the JSON reload removeAll) are covered — a bypassed add is picked up by the
 *     size-drift rebuild (the upstream :491-494 reInit shape) and a removed row is never
 *     served from a stale bucket (the membership re-check).</li>
 * <li><b>Order of magnitude (discipline ④)</b>: a nanoTime microbenchmark on a 512-row
 *     map — indexed lookups vs the linear baseline.</li>
 * </ul>
 */
class GT6RecipeMapHashIndexTest extends GTRecipesOfflineTestBase {

	/** The probe (ingot, Iron) item — the recipe-input side of the fallback arm. */
	private static MaterialPrefixItem INGOT_IRON;
	/** The probe (plate, Iron) item — the direction-rule arm. */
	private static MaterialPrefixItem PLATE_IRON;

	// ------------------------------------------------------------------
	// the retired linear scan — the equivalence baseline (the old RecipeMap.java:165-170 body)
	// ------------------------------------------------------------------

	private static Recipe linearScanBaseline(RecipeMap aMap, long aSize, FluidStack[] aFluids, ItemStack... aInputs) {
		for (Recipe tRecipe : aMap.mRecipeList) {
			if (tRecipe.mFakeRecipe || !tRecipe.isRecipeInputEqual(false, true, aFluids, aInputs)) continue;
			return tRecipe.mEnabled && RecipeMap.absGreaterEqual(aSize * aMap.mPower, tRecipe.mEUt) ? tRecipe : null;
		}
		return null;
	}

	/**
	 * The per-query equivalence assertion: SAME instance is the hard pass; a disagreement
	 * must be ambiguity — the classifier re-scans and pins the indexed result to the
	 * matching set (or the first-match gate-fail null).
	 */
	private static void assertEquivalent(RecipeMap aMap, String aLabel, long aSize, FluidStack[] aFluids, ItemStack... aInputs) {
		Recipe tBaseline = linearScanBaseline(aMap, aSize, aFluids, aInputs);
		Recipe tIndexed = aMap.findRecipe(null, aSize, ItemStack.EMPTY, aFluids, aInputs);
		if (tBaseline == tIndexed) return;
		List<Recipe> tMatching = new ArrayList<>();
		for (Recipe tRecipe : aMap.mRecipeList) {
			if (!tRecipe.mFakeRecipe && tRecipe.isRecipeInputEqual(false, true, aFluids, aInputs)) tMatching.add(tRecipe);
		}
		assertFalse(tMatching.isEmpty(), aLabel + ": the indexed lookup drifted from the linear baseline on an UNAMBIGUOUS query — baseline="
				+ tBaseline + " indexed=" + tIndexed + " matching=" + tMatching.size());
		assertTrue(tIndexed == null || (tMatching.contains(tIndexed) && tIndexed.mEnabled
				&& RecipeMap.absGreaterEqual(aSize * aMap.mPower, tIndexed.mEUt)),
				aLabel + ": the indexed result under ambiguity must be a matching stored row with a passing verdict (or the gate-fail null), got " + tIndexed);
	}

	// ------------------------------------------------------------------
	// acceptance ①: the poured real census, every map × representative queries
	// ------------------------------------------------------------------

	@Test
	void pouredCensusEquivalenceOverEveryMap() throws Exception {
		pourCensus();
		int tQueriedMaps = 0;
		for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) {
			if (tMap.mRecipeList.isEmpty()) continue;
			// the subclass maps (Canner/Press/Crucible/ScannerMolecular) carry DYNAMIC findRecipe
			// arms on top of super — their offline resolvers explode on the capability loader —
			// and their dynamics are outside this card's contract; the shared index underneath is
			// the same code the base maps exercise here.
			if (tMap.getClass() != RecipeMap.class) continue;
			tQueriedMaps++;
			int tBudget = tMap.mRecipeList.size() > 10000 ? 8 : 48; // MIXER's 56k identity-stand-in rows: sample
			long tSize = Long.MAX_VALUE / 4096; // covers every row mEUt without the mPower>1 overflow
			int tSeen = 0;
			for (Recipe tRow : tMap.mRecipeList) {
				if (tSeen >= tBudget) break;
				tSeen++;
				// the exact query: copies of the row's own legs (count + NBT preserved)
				List<ItemStack> tQuery = new ArrayList<>();
				for (ItemStack tInput : tRow.mInputs) if (tInput != null && !tInput.isEmpty()) tQuery.add(tInput.copy());
				FluidStack[] tFluids = fluidSnapshot(tRow);
				if (!tQuery.isEmpty()) {
					assertEquivalent(tMap, tMap.mNameInternal + " exact#" + tSeen, tSize, tFluids, tQuery.toArray(new ItemStack[0]));
					// count-starved variant: halve the first leg (the count gate arm)
					ItemStack[] tStarved = tQuery.toArray(new ItemStack[0]);
					tStarved[0] = tStarved[0].copy();
					tStarved[0].setCount(Math.max(1, tStarved[0].getCount() / 2));
					assertEquivalent(tMap, tMap.mNameInternal + " starved#" + tSeen, tSize, tFluids, tStarved);
					// extra-unrelated-input variant: matching is a cover, extras are noise
					ItemStack[] tExtra = new ItemStack[tQuery.size() + 1];
					for (int i = 0; i < tQuery.size(); i++) tExtra[i] = tQuery.get(i);
					tExtra[tQuery.size()] = new ItemStack(Items.DIAMOND, 1);
					assertEquivalent(tMap, tMap.mNameInternal + " extra#" + tSeen, tSize, tFluids, tExtra);
				} else if (tFluids != null) {
					// the fluid-only row shape (ENGINE_FUELS/BURN form): empty item array
					assertEquivalent(tMap, tMap.mNameInternal + " fluid#" + tSeen, tSize, tFluids);
				}
			}
			// foreign-item and foreign-fluid probes: both must agree (usually null)
			assertEquivalent(tMap, tMap.mNameInternal + " foreignItem", tSize, null, new ItemStack(Items.DIAMOND, 1));
			assertEquivalent(tMap, tMap.mNameInternal + " foreignFluid", tSize, new FluidStack[] {new FluidStack(Fluids.LAVA, 100)});
		}
		assertTrue(tQueriedMaps >= 20, "the census pour must stock the real maps, saw " + tQueriedMaps);
	}

	/** Non-empty fluids of a row, snapshot-copied; null when the row has none. */
	private static FluidStack[] fluidSnapshot(Recipe aRow) {
		List<FluidStack> tFluids = new ArrayList<>();
		for (FluidStack tFluid : aRow.mFluidInputs) if (tFluid != null && !tFluid.isEmpty()) {
			//? if forge {
			tFluids.add(new FluidStack(tFluid, tFluid.getAmount()));
			//?} else {
			/*tFluids.add(tFluid.copyWithAmount(tFluid.getAmount())); // 21.1: no copy ctor (Recipe.getFluidOutputs shape)
			*///?}
		}
		return tFluids.isEmpty() ? null : tFluids.toArray(new FluidStack[0]);
	}

	// ------------------------------------------------------------------
	// acceptance ① strict arm: the synthetic map, unambiguous by construction
	// ------------------------------------------------------------------

	@Test
	void syntheticMapStrictEquivalenceAndRouting() {
		Recipe.sTagTest = membershipStub();
		RecipeMap tMap = new RecipeMap(new LinkedHashSet<>(), "gt.recipe.hashindex.strict", "HashIndex Strict", null, 0, 1, "gt6:textures/gui/hashindex", 2, 2, 0, 2, 2, 0, 0, 1);
		Recipe tSand = tMap.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND, 4)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 32, 16, 0));
		Recipe tIngot = tMap.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(INGOT_IRON, 2)}, new ItemStack[] {new ItemStack(Items.IRON_NUGGET)}, null, null, 32, 16, 0));
		Recipe tPlate = tMap.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(PLATE_IRON, 1)}, new ItemStack[] {new ItemStack(Items.IRON_NUGGET)}, null, null, 64, 16, 0));
		Recipe tWater = tMap.addRecipe(new Recipe(true, null, new ItemStack[] {new ItemStack(Items.PAPER)}, new FluidStack[] {new FluidStack(Fluids.WATER, 100)}, null, 32, 16, 0));
		Recipe tKeyless = tMap.addRecipe(new Recipe(true, new ItemStack[0], new ItemStack[] {new ItemStack(Items.SLIME_BALL)}, new FluidStack[] {null}, null, 32, 16, 0)); // the all-null-fluid-array residue leg
		long tSize = Long.MAX_VALUE / 4096;

		// exact hits: same instance, both implementations
		assertSame(linearScanBaseline(tMap, tSize, null, new ItemStack(Items.SAND, 4)), tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)));
		assertSame(tSand, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)));
		assertSame(tIngot, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(INGOT_IRON, 2)), "the exact branch routes through the Item bucket");
		assertSame(tPlate, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(PLATE_IRON, 1)));

		// the direction rule at the map level: the ingot query must NOT reach the plate row,
		// and an unrelated caller buffer must not be served over the real match
		assertSame(tIngot, linearScanBaseline(tMap, tSize, null, new ItemStack(INGOT_IRON, 2)));
		assertSame(tIngot, tMap.findRecipe(tPlate, tSize, ItemStack.EMPTY, null, new ItemStack(INGOT_IRON, 2)), "an unrelated aLastRecipe declines, the index still answers");
		assertSame(tIngot, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(INGOT_IRON, 2)));

		// the p25 fallback: the VANILLA iron ingot reaches the GT ingot row through the tag sweep (the upstream unified-stack tStack2 leg)
		assertSame(tIngot, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.IRON_INGOT, 2)),
				"the vanilla iron ingot (stub: ingots/iron member) must reach the GT ingot row — the sweep leg");
		assertNull(tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.GOLD_INGOT, 2)), "gold ∉ ingots/iron — the sweep declines too");

		// fluid routing: the lava query reaches only the keyless residue row (its legs are vacuous)
		assertSame(tKeyless, tMap.findRecipe(null, tSize, ItemStack.EMPTY, new FluidStack[] {new FluidStack(Fluids.LAVA, 100)}));
		// the water query is structurally ambiguous (tWater + tKeyless both match) — the classifier arm
		assertEquivalent(tMap, "water ambiguous", tSize, new FluidStack[] {new FluidStack(Fluids.WATER, 100)});
		// a water query with the required paper: unambiguous? tWater needs water+paper, tKeyless matches any fluid array too — still ambiguous, classifier
		assertEquivalent(tMap, "water+paper", tSize, new FluidStack[] {new FluidStack(Fluids.WATER, 100)}, new ItemStack(Items.PAPER, 1));

		// lookup-probe stack sizes: the findRecipe probe runs aDontCheckStackSizes=T (upstream
		// :487/:501 — the count gate is the machine's consume-time job), so a 2x query still
		// matches the 4x row in BOTH implementations
		assertSame(tSand, linearScanBaseline(tMap, tSize, null, new ItemStack(Items.SAND, 2)));
		assertSame(tSand, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 2)));

		// power gate: 8 < 16 — the row matches but the voltage gate nulls the lookup (upstream :501)
		assertNull(linearScanBaseline(tMap, 8, null, new ItemStack(Items.SAND, 4)));
		assertNull(tMap.findRecipe(null, 8, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)));
		assertEquals(tSand, tMap.findRecipe(null, 16, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)));

		// runtime mutation parity: disabled and fake rows are skipped by BOTH (probe-time checks)
		tSand.mEnabled = false;
		assertNull(tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)), "disabled rows gate to null");
		tSand.mEnabled = true;
		tSand.mFakeRecipe = true;
		assertNull(tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)), "fake rows are skipped");
		tSand.mFakeRecipe = false;

		// the self-heal: a bypassed add (the CokeOven tag-listener shape) is picked up by the size-drift rebuild
		Recipe tBone = new Recipe(true, new ItemStack[] {new ItemStack(Items.BONE, 1)}, new ItemStack[] {new ItemStack(Items.BONE_MEAL)}, null, null, 32, 16, 0);
		tMap.mRecipeList.add(tBone);
		assertSame(tBone, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.BONE, 1)), "the size-drift rebuild admits the bypassed row");

		// the stale-bucket guard: a row removed behind the index is never served
		tMap.mRecipeList.remove(tSand);
		assertNull(tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)), "a removed row must not be served from its bucket");
	}

	/**
	 * The P1 pin (review round 1): the CokeOven tag listener's subset replace is a runtime
	 * remove+add seam that can be SIZE-NEUTRAL — a /reload re-firing TagsUpdatedEvent with an
	 * unchanged #minecraft:logs removes M rows and adds M fresh instances (Δ=0), so the
	 * size-drift rebuild never fires and the fresh instances would sit in no bucket (the
	 * contains guard only blocks the inverse) — every COKE_OVEN log lookup would silently
	 * null until restart. The same-size swap through the REAL seam
	 * ({@code GT6CokeOvenTagListener.replaceLogRecipes}) must leave the fresh instance findable.
	 */
	@Test
	void cokeOvenSameSizeReplaceStaysFindable() {
		long tSize = Long.MAX_VALUE / 4096;
		ItemStack[] tLog = {new ItemStack(Items.OAK_LOG, 16)};
		Recipe tOld = new Recipe(true, new ItemStack[] {new ItemStack(Items.OAK_LOG, 16)}, new ItemStack[] {new ItemStack(Items.CHARCOAL)}, new FluidStack[0], null, 16, 1, 0);
		Recipe tNew = new Recipe(true, new ItemStack[] {new ItemStack(Items.OAK_LOG, 16)}, new ItemStack[] {new ItemStack(Items.COAL)}, new FluidStack[0], null, 16, 1, 0);

		GT6CokeOvenTagListener.replaceLogRecipes(List.of(tOld)); // first pour: Δ≠0 → the size-drift self-heal answers
		assertSame(tOld, GT6RecipeMaps.COKE_OVEN.findRecipe(null, tSize, ItemStack.EMPTY, null, tLog));

		GT6CokeOvenTagListener.replaceLogRecipes(List.of(tNew)); // SAME-SIZE swap (1→1, Δ=0) — the runtime /reload face
		assertSame(tNew, GT6RecipeMaps.COKE_OVEN.findRecipe(null, tSize, ItemStack.EMPTY, null, tLog),
				"after a size-neutral subset replace the fresh instance must be found (invalidateIndex), not nulled");
		// the buffer holds the retired tOld: the membership guard skips it, the rebuild answers tNew
		assertNull(GT6RecipeMaps.COKE_OVEN.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SPRUCE_LOG, 16)),
				"rows outside the subset stay unreachable");
		// leave the listener's tracked subset EMPTY — sLogRecipes is listener-static and the
		// sibling CokeOven tests pin its clean-slate count (the seam-restores discipline)
		GT6CokeOvenTagListener.replaceLogRecipes(List.of());
	}

	// ------------------------------------------------------------------
	// acceptance ②: the oRecipe map-level buffer
	// ------------------------------------------------------------------

	@Test
	void bufferServesTheSameInputsTwiceWithoutRescanning() {
		RecipeMap tMap = strictMap();
		long tSize = Long.MAX_VALUE / 4096;
		// two rows matching the SAME query; rowB was poured FIRST, so the index's bucket order
		// answers B — the buffer answering A is the no-rescan witness
		Recipe tRowB = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND, 4)}, new ItemStack[] {new ItemStack(Items.APPLE)}, null, null, 32, 16, 0);
		Recipe tRowA = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND, 4)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 32, 16, 0);
		assertSame(tRowB, tMap.addRecipe(tRowB));
		assertSame(tRowA, tMap.addRecipe(tRowA));

		// lookup 1 via the caller-buffer arm: A is served and lands in the map buffer (upstream :487)
		assertSame(tRowA, tMap.findRecipe(tRowA, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)));
		assertSame(tMap.oRecipe, tRowA);
		// lookup 2 with a null caller: the buffer answers A — the INDEX would have said B first
		assertSame(tRowA, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)),
				"the second same-input lookup is served from the map buffer, not the (B-first) index");

		// the membership guard: a row removed behind the map is never served from the buffer —
		// A is skipped (not contained), and with B removed too the lookup is empty for both paths
		tMap.mRecipeList.remove(tRowA);
		assertSame(tRowB, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)),
				"the removed buffer row is skipped; the stored sibling answers (the retired-scan behavior)");
		tMap.mRecipeList.remove(tRowB);
		assertNull(tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)),
				"with no stored row left, neither buffer nor index may answer");
	}

	@Test
	void bufferIsNotPollutedByNonBufferableRowsAndRidesTheCallerBufferArm() {
		RecipeMap tMap = strictMap();
		Recipe tNoBuffer = new Recipe(false, new ItemStack[] {new ItemStack(Items.GRAVEL, 2)}, new ItemStack[] {new ItemStack(Items.FLINT)}, null, null, 32, 16, 0);
		assertSame(tNoBuffer, tMap.addRecipe(tNoBuffer));
		long tSize = Long.MAX_VALUE / 4096;

		assertSame(tNoBuffer, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.GRAVEL, 2)));
		assertSame(tNoBuffer, tMap.oRecipe, "upstream :501 sets the buffer on every indexed hit — the mCanBeBuffered gate lives on the :488 PROBE, not the set");

		// and the probe-side gate: the buffered non-bufferable row is skipped, the index is re-entered
		Recipe tSand = strictSandRow(tMap);
		assertSame(tSand, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)));
		assertSame(tMap.oRecipe, tSand);

		// the caller buffer arm populates the map buffer too (upstream :487 oRecipe=aRecipe)
		assertSame(tSand, tMap.findRecipe(tSand, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)), "the caller buffer arm");
		assertSame(tMap.oRecipe, tSand, "the :487 arm sets oRecipe");
		assertSame(tSand, tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)));

		// the power gate on the buffer arm: 8 < 16 → null, and the buffer is NOT updated (upstream ternary shape)
		assertNull(tMap.findRecipe(null, 8, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 4)));
		assertSame(tMap.oRecipe, tSand, "a gate-failed buffer probe leaves the buffer untouched");
	}

	// ------------------------------------------------------------------
	// discipline ④: the order-of-magnitude microbenchmark
	// ------------------------------------------------------------------

	@Test
	void hashIndexBeatsTheLinearScanByOrdersOfMagnitude() {
		// 512 distinct vanilla items, one 4x row each — rotating distinct queries (no buffer hits)
		List<Item> tItems = new ArrayList<>();
		for (Item tItem : BuiltInRegistries.ITEM) {
			if (tItems.size() >= 512) break;
			if (tItem == null || new ItemStack(tItem).isEmpty() || tItem == Items.AIR) continue;
			tItems.add(tItem);
		}
		assertTrue(tItems.size() >= 256, "the vanilla universe must spare enough distinct items, saw " + tItems.size());
		RecipeMap tMap = new RecipeMap(new LinkedHashSet<>(), "gt.recipe.hashindex.bench", "HashIndex Bench", null, 0, 1, "gt6:textures/gui/hashindex", 1, 1, 0, 0, 0, 0, 0, 1);
		for (Item tItem : tItems) {
			tMap.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(tItem, 4)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 32, 16, 0));
		}
		long tSize = Long.MAX_VALUE / 4096;
		int tRounds = 300;
		ItemStack[][] tQueries = new ItemStack[tRounds][];
		for (int i = 0; i < tRounds; i++) tQueries[i] = new ItemStack[] {new ItemStack(tItems.get(i % tItems.size()), 4)};

		// warmup both paths (JIT), then measure
		for (ItemStack[] tQ : tQueries) linearScanBaseline(tMap, tSize, null, tQ);
		for (ItemStack[] tQ : tQueries) tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, tQ);
		long tLinearStart = System.nanoTime();
		for (int r = 0; r < 3; r++) for (ItemStack[] tQ : tQueries) linearScanBaseline(tMap, tSize, null, tQ);
		long tLinear = System.nanoTime() - tLinearStart;
		long tIndexStart = System.nanoTime();
		for (int r = 0; r < 3; r++) for (ItemStack[] tQ : tQueries) tMap.findRecipe(null, tSize, ItemStack.EMPTY, null, tQ);
		long tIndexed = System.nanoTime() - tIndexStart;

		System.out.println("p32-perf-recipe-hash-index microbench: linear=" + tLinear / 1_000_000 + "ms indexed=" + tIndexed / 1_000_000 + "ms ratio=" + (tLinear / Math.max(1, tIndexed)) + "x");
		assertTrue(tIndexed * 10 < tLinear, "the indexed lookup must beat the linear scan by an order of magnitude: linear=" + tLinear + "ns indexed=" + tIndexed + "ns");
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	private static RecipeMap strictMap() {
		return new RecipeMap(new LinkedHashSet<>(), "gt.recipe.hashindex.buf", "HashIndex Buf", null, 0, 1, "gt6:textures/gui/hashindex", 2, 2, 0, 2, 2, 0, 0, 1);
	}

	private static Recipe strictSandRow(RecipeMap aMap) {
		Recipe tSand = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND, 4)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 32, 16, 0);
		assertSame(tSand, aMap.addRecipe(tSand));
		return tSand;
	}

	/** The same-value membership stub (the TagFallbackTest shape): vanilla iron ingot + the GT probe faces. */
	private static java.util.function.BiPredicate<ItemStack, TagKey<Item>> membershipStub() {
		return (aInput, aTag) -> {
			String tPath = aTag.location().getPath();
			Item tItem = aInput.getItem();
			if ("ingots/iron".equals(tPath)) return tItem == Items.IRON_INGOT || tItem == INGOT_IRON;
			if ("plates/iron".equals(tPath)) return tItem == PLATE_IRON;
			return false;
		};
	}

	private static <I extends Item> I probeItem(String aProbeId, java.util.function.Supplier<I> aCreator) {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			// three locks must open (the FileSawTest walk): the vanilla frozen flag, the
			// delegate ForgeRegistry.isFrozen, the NamespacedWrapper.locked register gate
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
			Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
			tDelegate.setAccessible(true);
			Object tForgeRegistry = tDelegate.get(tRegistry);
			java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
			Field tLocked = inheritedField(tRegistry.getClass(), "locked");
			tLocked.setBoolean(tRegistry, false);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		//?} else {
		/*try {
			// 21.1: the plain vanilla DefaultedMappedRegistry — a single frozen flag
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		*///?}
		I rItem = aCreator.get();
		net.minecraft.core.Registry.register(tRegistry, aProbeId, rItem);
		return rItem;
	}

	/** getDeclaredField along the superclass chain (the FileSawTest helper, mirrored). */
	private static Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> c = aClass; c != null; c = c.getSuperclass()) {
			try {
				Field rField = c.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException ignored) {
				// walk up
			}
		}
		throw new NoSuchFieldException(aName);
	}

	// ------------------------------------------------------------------
	// lifecycle: the offline material universe + the probe items + the census fixtures
	// ------------------------------------------------------------------

	@BeforeAll
	static void bootAndBuildProbes() {
		GTMaterialItems.initMaterials(); // the offline material universe (the ShCL convention)
		INGOT_IRON = probeItem("hashindex_probe_ingot_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.ingot, MT.Iron));
		PLATE_IRON = probeItem("hashindex_probe_plate_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.plate, MT.Iron));
		// capture the resolver seams BEFORE the first @BeforeEach arms them (the phase-gate
		// capture/restore discipline — the census fixtures must not leak into sibling classes)
		capture(() -> GT6RecipesEngineFuels.sFluidResolver, aV -> GT6RecipesEngineFuels.sFluidResolver = aV);
		capture(() -> GT6RecipesBurnFuels.sFluidResolver, aV -> GT6RecipesBurnFuels.sFluidResolver = aV);
		capture(() -> GT6RecipesOreChain.sMaterialItemResolver, aV -> GT6RecipesOreChain.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesDistillery.sFluidResolver, aV -> GT6RecipesDistillery.sFluidResolver = aV);
		capture(() -> GT6RecipesDistillery.sCircuitResolver, aV -> GT6RecipesDistillery.sCircuitResolver = aV);
		capture(() -> GT6RecipesDrying.sFluidResolver, aV -> GT6RecipesDrying.sFluidResolver = aV);
		capture(() -> GT6RecipesDrying.sMaterialItemResolver, aV -> GT6RecipesDrying.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesExtruder.sMaterialItemResolver, aV -> GT6RecipesExtruder.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesExtruder.sBlockResolver, aV -> GT6RecipesExtruder.sBlockResolver = aV);
		capture(() -> GT6RecipesExtruder.sPlateMoldResolver, aV -> GT6RecipesExtruder.sPlateMoldResolver = aV);
		capture(() -> GT6RecipesExtruder.sRodMoldResolver, aV -> GT6RecipesExtruder.sRodMoldResolver = aV);
		capture(() -> GT6RecipesAnvil.sMaterialItemResolver, aV -> GT6RecipesAnvil.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesMixer.sMaterialItemResolver, aV -> GT6RecipesMixer.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesMixer.sWaterResolver, aV -> GT6RecipesMixer.sWaterResolver = aV);
		capture(() -> GT6RecipesMixer.sCfoamResolver, aV -> GT6RecipesMixer.sCfoamResolver = aV);
		capture(() -> GT6RecipesMixer.sBaseCfoamResolver, aV -> GT6RecipesMixer.sBaseCfoamResolver = aV);
		capture(() -> GT6RecipesBath.sPlankItemResolver, aV -> GT6RecipesBath.sPlankItemResolver = aV);
		capture(() -> GT6RecipesBath.sOilFluidResolver, aV -> GT6RecipesBath.sOilFluidResolver = aV);
		capture(() -> GT6RecipesSifter.sMaterialItemResolver, aV -> GT6RecipesSifter.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesCompressor.sMaterialItemResolver, aV -> GT6RecipesCompressor.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesWiremill.sMaterialItemResolver, aV -> GT6RecipesWiremill.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesBees.sCombResolver, aV -> GT6RecipesBees.sCombResolver = aV);
		capture(() -> GT6RecipesBees.sFluidResolver, aV -> GT6RecipesBees.sFluidResolver = aV);
		capture(() -> GT6RecipesBees.sMaterialItemResolver, aV -> GT6RecipesBees.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesImplosion.sMaterialItemResolver, aV -> GT6RecipesImplosion.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesImplosion.sTntResolver, aV -> GT6RecipesImplosion.sTntResolver = aV);
		capture(() -> GT6RecipesImplosion.sCircuitResolver, aV -> GT6RecipesImplosion.sCircuitResolver = aV);
		capture(() -> GT6RecipesStoneChisel.sStoneItemResolver, aV -> GT6RecipesStoneChisel.sStoneItemResolver = aV);
		capture(() -> GT6RecipesShCL.sMaterialItemResolver, aV -> GT6RecipesShCL.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesCokeOven.sOutputItemResolver, aV -> GT6RecipesCokeOven.sOutputItemResolver = aV);
		capture(() -> GT6RecipesCokeOven.sInputItemResolver, aV -> GT6RecipesCokeOven.sInputItemResolver = aV);
		capture(() -> GT6RecipesCokeOven.sFluidResolver, aV -> GT6RecipesCokeOven.sFluidResolver = aV);
		capture(() -> GT6RecipesWelder.sPlateResolver, aV -> GT6RecipesWelder.sPlateResolver = aV);
		capture(() -> GT6RecipesWelder.sOutputResolver, aV -> GT6RecipesWelder.sOutputResolver = aV);
		capture(() -> GT6RecipesWelder.sSelectorResolver, aV -> GT6RecipesWelder.sSelectorResolver = aV);
		capture(() -> GT6RecipesMassfab.sMaterialItemResolver, aV -> GT6RecipesMassfab.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesMassfab.sMatterFluidResolver, aV -> GT6RecipesMassfab.sMatterFluidResolver = aV);
		capture(() -> GT6RecipesFusion.sCircuitResolver, aV -> GT6RecipesFusion.sCircuitResolver = aV);
		capture(() -> GT6RecipesFusion.sFluidResolver, aV -> GT6RecipesFusion.sFluidResolver = aV);
		capture(() -> GT6RecipesFusion.sMaterialItemResolver, aV -> GT6RecipesFusion.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesCanner.sDyeFluidResolver, aV -> GT6RecipesCanner.sDyeFluidResolver = aV);
		capture(() -> GT6RecipesCanner.sChlorineResolver, aV -> GT6RecipesCanner.sChlorineResolver = aV);
		capture(() -> GT6RecipesCanner.sEmptyCanResolver, aV -> GT6RecipesCanner.sEmptyCanResolver = aV);
		capture(() -> GT6RecipesCanner.sSprayPaintResolver, aV -> GT6RecipesCanner.sSprayPaintResolver = aV);
		capture(() -> GT6RecipesCanner.sRemoverResolver, aV -> GT6RecipesCanner.sRemoverResolver = aV);
		capture(() -> GT6RecipesCanner.sFoodCanEmptyResolver, aV -> GT6RecipesCanner.sFoodCanEmptyResolver = aV);
		capture(() -> GT6RecipesCanner.sRottenCansResolver, aV -> GT6RecipesCanner.sRottenCansResolver = aV);
		capture(() -> GT6RecipesCanner.sCookiesCanResolver, aV -> GT6RecipesCanner.sCookiesCanResolver = aV);
		capture(() -> GT6RecipesCanner.sCfoamFluidResolver, aV -> GT6RecipesCanner.sCfoamFluidResolver = aV);
		capture(() -> GT6RecipesCanner.sCfoamOwnedFluidResolver, aV -> GT6RecipesCanner.sCfoamOwnedFluidResolver = aV);
		capture(() -> GT6RecipesCanner.sFoamSprayResolver, aV -> GT6RecipesCanner.sFoamSprayResolver = aV);
		capture(() -> GT6RecipesCanner.sFoamSprayOwnedResolver, aV -> GT6RecipesCanner.sFoamSprayOwnedResolver = aV);
		capture(() -> GT6RecipesCanner.sCarbonDioxideResolver, aV -> GT6RecipesCanner.sCarbonDioxideResolver = aV);
		capture(() -> GT6RecipesCanner.sLaserGasEmptyResolver, aV -> GT6RecipesCanner.sLaserGasEmptyResolver = aV);
		capture(() -> GT6RecipesCanner.sLaserGasCo2Resolver, aV -> GT6RecipesCanner.sLaserGasCo2Resolver = aV);
	}

	/** The captured loader seams, restored after each test so sibling classes see defaults (the phase-gate convention). */
	private static final List<Runnable> sSeamRestores = new ArrayList<>();

	private static <T> void capture(java.util.function.Supplier<T> aGetter, java.util.function.Consumer<T> aSetter) {
		T tDefault = aGetter.get();
		sSeamRestores.add(() -> aSetter.accept(tDefault));
	}

	@AfterEach
	void restoreSeamsAndDropGeneration() {
		Recipe.sTagTest = Recipe.VANILLA_TAG_TEST;
		for (Runnable tRestore : sSeamRestores) tRestore.run();
		GT6RecipeMaps.reset();
	}

	// ------------------------------------------------------------------
	// the census pour (the phase-gate fixture, verbatim stand-ins)
	// ------------------------------------------------------------------

	@BeforeEach
	void armFixturesAndFreshGeneration() {
		GT6RecipesEngineFuels.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesBurnFuels.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesOreChain.sMaterialItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesDistillery.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesDistillery.sCircuitResolver = aConfig -> new ItemStack(Items.PAPER);
		GT6RecipesDrying.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesDrying.sMaterialItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesExtruder.sMaterialItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesExtruder.sBlockResolver = aPair -> new ItemStack(Items.BRICK);
		GT6RecipesExtruder.sPlateMoldResolver = () -> new ItemStack(Items.BRICK);
		GT6RecipesExtruder.sRodMoldResolver = () -> new ItemStack(Items.BRICK);
		GT6RecipesAnvil.sMaterialItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesMixer.sMaterialItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesMixer.sWaterResolver = aIndex -> Fluids.WATER;
		GT6RecipesMixer.sCfoamResolver = (aIndex, aOwned) -> Fluids.WATER;
		GT6RecipesMixer.sBaseCfoamResolver = () -> Fluids.WATER;
		GT6RecipesBath.sPlankItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesBath.sOilFluidResolver = aLeg -> Fluids.WATER;
		GT6RecipesSifter.sMaterialItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesCompressor.sMaterialItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesWiremill.sMaterialItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesBees.sCombResolver = aComb -> Items.BRICK;
		GT6RecipesBees.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesBees.sMaterialItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesImplosion.sMaterialItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesImplosion.sTntResolver = () -> Items.TNT;
		GT6RecipesImplosion.sCircuitResolver = aConfig -> new ItemStack(Items.PAPER);
		GT6RecipesStoneChisel.sStoneItemResolver = aStone -> Items.BRICK;
		GT6RecipesShCL.sMaterialItemResolver = (aP, aM) -> aP == null || aM == null ? null : Items.BRICK;
		GT6RecipesCokeOven.sOutputItemResolver = aOutput -> Items.BRICK;
		GT6RecipesCokeOven.sInputItemResolver = aRow -> Items.BRICK;
		GT6RecipesCokeOven.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesWelder.sPlateResolver = (aP, aM) -> new ItemStack(Items.BRICK);
		GT6RecipesWelder.sOutputResolver = aId -> Items.BRICK;
		GT6RecipesWelder.sSelectorResolver = aConfig -> new ItemStack(Items.PAPER);
		GT6RecipesMassfab.sMaterialItemResolver = (aP, aM) -> Items.IRON_INGOT;
		GT6RecipesMassfab.sMatterFluidResolver = aHalf -> aHalf.equals("charged") ? Fluids.LAVA : Fluids.WATER;
		GT6RecipesFusion.sCircuitResolver = aConfig -> new ItemStack(Items.PAPER);
		GT6RecipesFusion.sFluidResolver = (aMaterial, aMolten) -> aMolten ? Fluids.LAVA : Fluids.WATER;
		GT6RecipesFusion.sMaterialItemResolver = (aP, aM) -> Items.BRICK;
		GT6RecipesCanner.sDyeFluidResolver = aIndex -> Fluids.WATER;
		GT6RecipesCanner.sChlorineResolver = () -> Fluids.LAVA;
		GT6RecipesCanner.sEmptyCanResolver = () -> new ItemStack(Items.PAPER);
		GT6RecipesCanner.sSprayPaintResolver = aIndex -> new ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sRemoverResolver = () -> new ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sFoodCanEmptyResolver = () -> new ItemStack(Items.PAPER);
		GT6RecipesCanner.sRottenCansResolver = aTier -> new ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sCookiesCanResolver = () -> new ItemStack(Items.BRICK);
		GT6RecipesCanner.sCfoamFluidResolver = aIndex -> Fluids.FLOWING_LAVA;
		GT6RecipesCanner.sCfoamOwnedFluidResolver = aIndex -> Fluids.FLOWING_WATER;
		GT6RecipesCanner.sFoamSprayResolver = aIndex -> new ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sFoamSprayOwnedResolver = aIndex -> new ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sCarbonDioxideResolver = () -> Fluids.FLOWING_LAVA;
		GT6RecipesCanner.sLaserGasEmptyResolver = () -> new ItemStack(Items.PAPER);
		GT6RecipesCanner.sLaserGasCo2Resolver = () -> new ItemStack(Items.CLAY_BALL);
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
	}

	/** The full census pour, arm by arm (the phase-gate ledger order). */
	private static void pourCensus() throws Exception {
		GT6RecipesDistillery.load();
		GT6RecipesDrying.load();
		GT6RecipesBurnFuels.load();
		GT6RecipesEngineFuels.load();
		GT6RecipesCokeOven.load();
		GT6RecipesOreChain.load();
		GT6RecipesShCL.load();
		GT6RecipesStoneChisel.load();
		GT6RecipesCanner.load();
		GT6RecipeMapJsonLoader.pour(java.util.Map.of()); // the ledger arm — the no-op pour
		GT6RecipesMixer.load();
		GT6RecipesSifter.load();
		GT6RecipesCompressor.load();
		GT6RecipesWiremill.load();
		GT6RecipesExtruder.load();
		GT6RecipesPress.load();
		GT6RecipesBath.load();
		GT6RecipesAnvil.load();
		GT6RecipesWelder.load();
		GT6RecipesImplosion.load();
		GT6RecipesBees.load();
		GT6RecipesMassfab.load();
		GT6RecipesFusion.load();
	}
}
