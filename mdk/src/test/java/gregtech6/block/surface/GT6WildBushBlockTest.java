/**
 * Offline pins for task bush-growth-blockstate — the wild bush growth domain in its
 * blockstate form: the AGE_3 ↔ upstream mStage mapping, the KIND nine (the 2026-10-01
 * coordinator ruling — the upstream BushesGT.MAP full set), the vanilla
 * growth roll (SweetBerryBushBlock.java:64) and the upstream harvest distribution
 * (MultiTileEntityBush.java:169 {@code ST.amount(1+rng(2), mBerry)}).
 *
 * <p>The seeded pins replay the vanilla arithmetic on an INDEPENDENT stream with the same
 * seed — the seam must agree draw-for-draw (an extra or reordered {@code nextInt} inside
 * the block diverges the replay). Seeds ride the cbc-2 lesson: LegacyRandomSource small
 * seeds (≲11200) draw a constant first value, so every seed here is ≥11258.
 *
 * <p>Boot: the GT6SurfaceVariantsTest recipe (version detect precedes bootStrap; the
 * offline BLOCK registry unfrozen for the bare construction).
 */
package gregtech6.block.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import gregtech6.registry.GT6CropFoods;

class GT6WildBushBlockTest {

	@BeforeAll
	static void boot() {
		// the version detect must precede bootStrap (the GT6SurfaceBlocksTest recipe — a
		// bare-JVM first boot poisons DataFixers for every later suite in this JVM)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
		// offline Block construction needs the block registry temporarily unfrozen (the
		// GTWireContactDamageTest.block / GT6SurfaceVariantsTest.boot form)
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		BUSH = new GT6WildBushBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
	}

	static GT6WildBushBlock BUSH;

	// ------------------------------------------------------------------
	// the AGE ↔ mStage four-state pin (the card acceptance 2)
	// ------------------------------------------------------------------

	/** The vanilla AGE_3 seat mirrors the upstream mStage 0-3 (MultiTileEntityBush.java:61/:131). */
	@Test
	void agePropertyMirrorsTheUpstreamStage() {
		assertSame(BlockStateProperties.AGE_3, GT6WildBushBlock.AGE, "the vanilla AGE_3 seat");
		assertEquals("age", GT6WildBushBlock.AGE.getName(), "the vanilla property name");
		assertEquals(List.of(0, 1, 2, 3), List.copyOf(GT6WildBushBlock.AGE.getPossibleValues()),
				"the four stages 0-3, the mStage range");
		assertEquals(0, BUSH.defaultBlockState().getValue(GT6WildBushBlock.AGE), "birth = stage 0");
		assertEquals(36, BUSH.getStateDefinition().getPossibleStates().size(),
				"the full grid: 4 ages x 9 kinds");
		for (int tAge = 0; tAge < GT6WildBushBlock.MAX_AGE; tAge++) {
			BlockState tState = BUSH.defaultBlockState().setValue(GT6WildBushBlock.AGE, tAge);
			assertTrue(BUSH.isRandomlyTicking(tState), "age " + tAge + " rides the tick roll");
		}
		assertFalse(BUSH.isRandomlyTicking(BUSH.defaultBlockState().setValue(GT6WildBushBlock.AGE, 3)),
				"the mature bush leaves the tick roll (SweetBerryBushBlock.java:57-59)");
	}

	// ------------------------------------------------------------------
	// the KIND nine pin (the 2026-10-01 coordinator ruling — the MAP full set,
	// the T5a carrier tie)
	// ------------------------------------------------------------------

	/** The 9-kind set: order, serialized names, the T5a food-row ids, the string cotton carrier. */
	@Test
	void kindNineIsTheUpstreamWorldgenSet() {
		GT6WildBushBlock.Kind[] tKinds = GT6WildBushBlock.Kind.values();
		assertEquals(9, tKinds.length, "the BushesGT.MAP full set: 8 berries + the string cotton");
		assertEquals(List.of("blueberry", "candleberry", "cranberry", "currants_black", "currants_white",
						"currants_red", "blackberry", "raspberry", "cotton"),
				java.util.Arrays.stream(tKinds).map(GT6WildBushBlock.Kind::getSerializedName).toList(),
				"the serialized state names, upstream put order + cotton last");
		List<String> tFoodRows = GT6CropFoods.FOOD_ROWS.stream().map(GT6CropFoods.CropFoodRow::id).toList();
		for (int i = 0; i < 8; i++) {
			assertEquals(List.of("food_blueberry", "food_candleberry", "food_cranberry", "food_currants_black",
					"food_currants_white", "food_currants_red", "food_blackberry", "food_raspberry").get(i),
					tKinds[i].foodPath(), "the T5a berry row " + tKinds[i]);
			assertTrue(tFoodRows.contains(tKinds[i].foodPath()),
					"the berry carrier lives in the T5a band (the berry() scan tie): " + tKinds[i]);
		}
		assertSame(Items.STRING, tKinds[8].berry(),
				"the cotton bush drops the vanilla string (WorldgenBushes.java:66)");
		assertSame(GT6WildBushBlock.Kind.COTTON, BUSH.defaultBlockState().getValue(GT6WildBushBlock.KIND),
				"the default kind = the BushesGT.DEFAULT row (CS.java:1588) — the item-face continuity");
	}

	// ------------------------------------------------------------------
	// the growth roll pin (the card acceptance 1 — the light gate verbatim semantics)
	// ------------------------------------------------------------------

	/**
	 * The SweetBerryBushBlock.java:64 line replayed: {@code age < 3 && nextInt(5) == 0 &&
	 * rawBrightness >= 9}. Two pins: (a) draw-for-draw agreement with the hand transcription
	 * over 20k seeded rolls; (b) the operand order — the light read happens AFTER the roll
	 * (9 passes / 8 fails on a rolled draw) and a mature bush consumes NO draw.
	 */
	@Test
	void growthRollPinsTheVanillaFifth() {
		for (long tSeed : new long[] {11258L, 11259L, 20261001L}) {
			RandomSource tSeam = RandomSource.create(tSeed);
			RandomSource tHand = RandomSource.create(tSeed);
			int tHits = 0;
			for (int i = 0; i < 20000; i++) {
				// the transcription reduces at age 2 / brightness 15 to the bare vanilla roll
				boolean tExpected = tHand.nextInt(5) == 0;
				boolean tActual = GT6WildBushBlock.growthRoll(2, 15, tSeam);
				assertEquals(tExpected, tActual,
						"seed " + tSeed + " draw " + i + ": the seam must replay the vanilla arithmetic");
				if (tActual) tHits++;
			}
			assertTrue(tHits > 3000 && tHits < 5000, "seed " + tSeed + ": the 1/5 roll lands ~4000 of 20k, got "
					+ tHits);
		}
		// a stream whose FIRST draw rolls 0 — found, not assumed (the small-seed lesson)
		long tRolledSeed = 11258L;
		while (RandomSource.create(tRolledSeed).nextInt(5) != 0) tRolledSeed++;
		assertTrue(GT6WildBushBlock.growthRoll(2, 9, RandomSource.create(tRolledSeed)),
				"brightness 9 passes the gate (the >= boundary)");
		assertFalse(GT6WildBushBlock.growthRoll(2, 8, RandomSource.create(tRolledSeed)),
				"brightness 8 fails the gate");
		// the operand order: the mature bush consumes no rng draw
		RandomSource tMature = RandomSource.create(tRolledSeed);
		RandomSource tFresh = RandomSource.create(tRolledSeed);
		assertFalse(GT6WildBushBlock.growthRoll(3, 15, tMature), "the mature bush never grows");
		assertEquals(tFresh.nextInt(5), tMature.nextInt(5), "the mature check consumed no draw");
	}

	// ------------------------------------------------------------------
	// the harvest distribution pin (the card acceptance 1 — seeded, both faces)
	// ------------------------------------------------------------------

	/**
	 * The upstream harvest form (MultiTileEntityBush.java:169 {@code 1+rng(2)}): the seam
	 * replays the transcription draw-for-draw on three seeds, and both payout faces occur —
	 * each near-half over 20k draws (a degenerate one-value stream would mean the rng fed
	 * the wrong operand).
	 */
	@Test
	void harvestDistributionPinsTheUpstreamForm() {
		for (long tSeed : new long[] {11258L, 11259L, 20261001L}) {
			RandomSource tSeam = RandomSource.create(tSeed);
			RandomSource tHand = RandomSource.create(tSeed);
			int tOnes = 0, tTwos = 0;
			for (int i = 0; i < 20000; i++) {
				int tExpected = 1 + tHand.nextInt(2); // the transcription
				int tActual = GT6WildBushBlock.harvestAmount(tSeam);
				assertEquals(tExpected, tActual, "seed " + tSeed + " draw " + i + ": the payout replay");
				if (tActual == 1) tOnes++;
				if (tActual == 2) tTwos++;
			}
			assertTrue(tOnes > 8000 && tTwos > 8000, "seed " + tSeed + ": both faces land ~10k of 20k (ones="
					+ tOnes + " twos=" + tTwos + ")");
		}
	}
}
