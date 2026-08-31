package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Recipe.mChances semantics (task p8-recipe-chances-orechain ①): the 10000-based chance
 * array against upstream gregapi/recipes/Recipe.java:749-771.
 * <ul>
 * <li>null chances / all-10000 = the deterministic pre-chances behaviour, byte-identical
 *     on every existing loader row (the "zero change" ruling);</li>
 * <li>the deterministic overloads {@code getOutputs()}/{@code getOutputs(int)} keep their
 *     contract on null-chances rows and resolve probabilistic slots at full certainty;</li>
 * <li>the sampling overload {@code getOutputs(Random, int)}: chance &gt;= 10000 whole stack,
 *     0 &lt; chance &lt; 10000 per-unit Bernoulli with unit accumulation (:761-763),
 *     chance == 0 → NO output (the declared deviation against upstream :765-767 + the
 *     :906 ctor rewrite that is not replicated);</li>
 * <li>array alignment: chances trimmed to the trailing-null-trimmed outputs; reads past a
 *     shorter array default to 10000 (upstream :686/:687 with mMaxChances folded).</li>
 * </ul>
 */
class RecipeChancesTest {

	private static final ItemStack SAND = new ItemStack(Items.SAND, 2);

	@Test
	void nullChancesRowsKeepThePreChancesBehaviour() {
		Recipe tRecipe = new Recipe(true, null, new ItemStack[] {SAND}, null, null, 16, 16, 0);
		assertNull(tRecipe.mChances, "the 8-param ctor implies null chances");
		ItemStack[] tOutputs = tRecipe.getOutputs(3);
		assertEquals(1, tOutputs.length);
		assertEquals(6, tOutputs[0].getCount(), "2 x 3 = the deterministic whole-stack x processCount shape");
		// and the 9-param ctor with explicit null behaves identically
		Recipe tSame = new Recipe(true, null, new ItemStack[] {SAND}, null, null, 16, 16, 0, null);
		assertNull(tSame.mChances);
		assertEquals(6, tSame.getOutputs(3)[0].getCount());
	}

	@Test
	void allTenThousandChancesAreBehaviourallyNull() {
		Recipe tRecipe = new Recipe(true, null, new ItemStack[] {SAND, new ItemStack(Items.GLASS, 1)}, null, null, 16, 16, 0, new long[] {10000, 10000});
		ItemStack[] tOutputs = tRecipe.getOutputs(new Random(1), 3);
		assertEquals(6, tOutputs[0].getCount(), "chance 10000 = deterministic whole stack x processCount (upstream :758-759)");
		assertEquals(3, tOutputs[1].getCount());
	}

	@Test
	void chanceZeroYieldsNoOutputOnEveryPath() {
		// the upstream slot-0 sentinel shape: slot 0 chance 0, slot 1 chance 10000
		Recipe tRecipe = new Recipe(true, null, new ItemStack[] {SAND, new ItemStack(Items.GLASS, 1)}, null, null, 16, 16, 0, new long[] {0, 10000});
		ItemStack[] tSampled = tRecipe.getOutputs(new Random(1), 5);
		assertNull(tSampled[0], "chance 0 must yield nothing — declared deviation against upstream :765-767 passthrough");
		assertEquals(5, tSampled[1].getCount(), "chance 10000: 1 x processCount 5");
		// the deterministic convenience overload honours the same ruling (this is what keeps
		// the ore-chain rows from double-yielding through the existing machine call sites)
		ItemStack[] tDeterministic = tRecipe.getOutputs(5);
		assertNull(tDeterministic[0], "the deterministic overload must not resurrect the chance-0 slot");
		assertEquals(5, tDeterministic[1].getCount());
		// the ctor must NOT rewrite 0 to 10000 (upstream :906 not replicated)
		assertEquals(0, tRecipe.mChances[0], "the port keeps the raw 0 (upstream Recipe.java:906 rewrite not ported)");
	}

	@Test
	void chancesArrayIsAlignedToTheTrimmedOutputs() {
		// 12-slot chances over a row whose trailing outputs are null: aligned down to 2
		long[] tChances = new long[12];
		tChances[0] = 10000; tChances[1] = 2500;
		Recipe tRecipe = new Recipe(true, null, new ItemStack[] {SAND, new ItemStack(Items.GLASS, 1), null, null}, null, null, 16, 16, 0, tChances);
		assertEquals(2, tRecipe.mOutputs.length);
		assertEquals(2, tRecipe.mChances.length, "chances are trimmed to the trailing-null-trimmed outputs");
		// a shorter chances array keeps reading 10000 past its end (upstream :686 default)
		Recipe tShort = new Recipe(true, null, new ItemStack[] {SAND, new ItemStack(Items.GLASS, 1)}, null, null, 16, 16, 0, new long[] {10000});
		assertEquals(1, tShort.mChances.length);
		ItemStack[] tOutputs = tShort.getOutputs(new Random(1), 2);
		assertEquals(4, tOutputs[0].getCount());
		assertEquals(2, tOutputs[1].getCount(), "index 1 reads the out-of-bounds default 10000");
	}

	@Test
	void bernoulliSamplingAccumulatesPerUnit() {
		// stackSize 2 x processCount 3 = 6 independent trials at 50% (fixed seed → a stable interval)
		Recipe tHalf = new Recipe(true, null, new ItemStack[] {SAND}, null, null, 16, 16, 0, new long[] {5000});
		ItemStack[] tOutputs = tHalf.getOutputs(new Random(42), 3);
		assertNotNull(tOutputs[0], "50% over 6 trials with seed 42 must land at least one success");
		int tCount = tOutputs[0].getCount();
		assertTrue(tCount >= 2 && tCount <= 5, "6 trials @50% (seed 42) must stay in the plausible band, got " + tCount);
		assertNotEquals(6, tCount, "a strict mid-chance must not deterministically fill the whole stack (unit Bernoulli, not whole-stack)");

		// the unit-accumulation shape: the result is a stack of the SAME item sized by successes
		assertEquals(Items.SAND, tOutputs[0].getItem());
	}

	@Test
	void bernoulliEdgeChances() {
		// chance 1 (0.01%) over 40 trials: almost surely zero, never the full stack
		Recipe tTiny = new Recipe(true, null, new ItemStack[] {new ItemStack(Items.SAND, 4)}, null, null, 16, 16, 0, new long[] {1});
		ItemStack[] tOutputs = tTiny.getOutputs(new Random(7), 10);
		if (tOutputs[0] != null) {
			assertTrue(tOutputs[0].getCount() <= 4, "unit accumulation is capped by the trial count");
			assertTrue(tOutputs[0].getCount() < 4, "0.01% over 40 trials (seed 7) must not fill the stack");
		}
		// chance 9999 over 40 trials: almost surely nonzero and less than the full 40
		Recipe tNear = new Recipe(true, null, new ItemStack[] {new ItemStack(Items.SAND, 4)}, null, null, 16, 16, 0, new long[] {9999});
		ItemStack[] tNearOutputs = tNear.getOutputs(new Random(7), 10);
		assertNotNull(tNearOutputs[0]);
		assertTrue(tNearOutputs[0].getCount() >= 1, "99.99% over 40 trials (seed 7) must succeed at least once");
		assertTrue(tNearOutputs[0].getCount() <= 40, "unit accumulation cannot exceed stackSize x processCount");
	}

	@Test
	void deterministicOverloadResolvesProbabilisticSlotsAtFullCertainty() {
		Recipe tRecipe = new Recipe(true, null, new ItemStack[] {SAND, new ItemStack(Items.GLASS, 1)}, null, null, 16, 16, 0, new long[] {10000, 2500});
		ItemStack[] tOutputs = tRecipe.getOutputs(4);
		assertEquals(8, tOutputs[0].getCount());
		assertEquals(4, tOutputs[1].getCount(), "the deterministic convenience overload reads every live chance as certain");
		assertNull(new Recipe(true, null, new ItemStack[] {SAND}, null, null, 16, 16, 0, new long[] {0}).getOutputs(4)[0]);
	}

	@Test
	void nullRandomSpawnsAFreshGenerator() {
		// upstream :751 — null Random → new Random(); two calls may differ, both stay in band
		Recipe tHalf = new Recipe(true, null, new ItemStack[] {new ItemStack(Items.SAND, 8)}, null, null, 16, 16, 0, new long[] {5000});
		ItemStack[] tFirst = tHalf.getOutputs(null, 10);
		ItemStack[] tSecond = tHalf.getOutputs(null, 10);
		if (tFirst[0] != null) assertTrue(tFirst[0].getCount() <= 80);
		if (tSecond[0] != null) assertTrue(tSecond[0].getCount() <= 80);
	}
}
