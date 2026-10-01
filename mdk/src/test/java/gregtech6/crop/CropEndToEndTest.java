package gregtech6.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The cbc-6 end-to-end offline pin suite — the ADR-CB5 acceptance face: the WHOLE chain
 * 种→长→杂→变→收→种 driven under ONE seeded {@link RandomSource} (seeds 11258+, the
 * LegacyRandomSource small-seed constant-first-draw law from the CropMathTest header),
 * N generations of stat evolution pinned, the clamp 0-31 boundary and the aux&gt;100 death
 * branch included.
 *
 * <p><b>The BE offline driver</b> — the pure-function tick engine driven level-free: the
 * {@link GT6CropBlockEntity} is a plain {@link CropTileView} here (level null, or the map
 * rig where a level double only serves {@code getBlockEntity} for the neighbor walk), every
 * cycle is one {@link CropMath#tickCrop} call (one 256t engine cycle), and no world/registry
 * service is touched. The registry-bound face rides the payload branch of the lineage test —
 * the FML test JVM binds gt6:crop_seed live (the research.fml-test-memory 家法), the bare
 * forge JVM answers the base-seed fallback; the RNG walk is leg-identical, only the seed
 * stack carrier differs.
 *
 * <p><b>Candidate-list discipline</b>: the lineage passes an EXPLICIT candidate list (the
 * two fixture cards), never {@link GT6Crops#crops()} — the static registry is JVM-shared
 * across test classes, and the weighted pick must not ride that order dependence.
 *
 * <p><b>Donor size law</b>: donors plant at size 4 — the canCross gate is
 * {@code size + 2 > maxSize} (GT_BaseCrop.java:113-115), so size-3 donors against maxSize 5
 * never qualify and the rig would just Weed-EX-drain into the 1% self-weed (observed, the
 * probe that shaped this suite).
 */
public class CropEndToEndTest extends GTOfflineTestBase {

	/** The lineage seed — >= 11258 per the small-seed constant-first-draw law. */
	static final long SEED = 11258L;
	static final long DEATH_SEED = 11259L;

	/**
	 * The two donor cards — tier 1/2, mx 5, PLAIN GT6CropCards over vanilla stacks: the drop
	 * and the base seed are real (offline-constructible) stacks so the seed route stays the
	 * production one — {@code seedStack} → {@link GT6CropSeeds#tryGenerate} live (the FML-leg
	 * payload), the base-seed copy offline — with ZERO test overrides in the chain. The
	 * stats/attributes/tier match the probe walk, so the pinned lineage is unaffected.
	 */
	static final GT6CropCard A = new GT6CropCard("Alpha", "Tester", new ItemStack(Items.WHEAT), null,
			new ItemStack(Items.WHEAT_SEEDS), 1, 5, 2, 2, 5, 6, 7, 8, 9, new String[] {"Foo", "Bar"});
	static final GT6CropCard B = new GT6CropCard("Beta", "Tester", new ItemStack(Items.CARROT), null,
			new ItemStack(Items.WHEAT_SEEDS), 2, 5, 2, 2, 9, 8, 7, 6, 5, new String[] {"Bar", "Baz"});

	/** The kill card — tier 10 → minimum 36 vs provided 0 → aux 144 > 100 vs the rand(32) > Re death roll. */
	static final GT6CropCard DEADLY = new GT6CropCard("Deadly", "Tester", new ItemStack(Items.WHEAT), null,
			new ItemStack(Items.WHEAT_SEEDS), 10, 5, 2, 2, 0, 0, 0, 0, 0, new String[0]);

	/** The lineage candidate list — EXPLICIT, the static-registry order dependence stays out. */
	static final List<GT6CropCard> CANDIDATES = List.of(A, B);

	static final BlockPos P_CENTER = new BlockPos(2, 3, 4);
	static final BlockPos P_NORTH = P_CENTER.north();
	static final BlockPos P_SOUTH = P_CENTER.south();

	// ---------------------------------------------------------------- fixtures

	@BeforeAll
	static void bootOfflineFixtures() {
		// the cbc-1 fixture walk verbatim: the fluid-type seam + the unfrozen BET registry +
		// the offline block/BE-type pair (its WHEATISH/DEADLY crop registrations ride along,
		// harmless — the lineage never reads GT6Crops.crops()).
		CropBlockEntityTest.buildOfflineFixtures();
	}

	/** The CropBlockEntityTest.CropLevel subclass whose getBlockEntity serves the rig tiles. */
	static final class LineageLevel extends CropBlockEntityTest.CropLevel {
		final Map<BlockPos, BlockEntity> mTiles = new HashMap<>();

		@Override
		public BlockEntity getBlockEntity(BlockPos aPos) {
			BlockEntity tTile = mTiles.get(aPos);
			return tTile != null ? tTile : super.getBlockEntity(aPos);
		}
	}

	/** The 3-tile crossing rig: the center (crossing base) + the two donor slots. */
	static final class Rig {
		final LineageLevel mLevel = new LineageLevel();
		final GT6CropBlockEntity mCenter, mNorth, mSouth;

		Rig() {
			BlockState tState = CropBlockEntityTest.sBlock.defaultBlockState();
			mCenter = tile(P_CENTER, tState);
			mNorth = tile(P_NORTH, tState);
			mSouth = tile(P_SOUTH, tState);
			terrainAll();
		}

		private GT6CropBlockEntity tile(BlockPos aPos, BlockState aState) {
			GT6CropBlockEntity tTile = new GT6CropBlockEntity(CropBlockEntityTest.sBeType, aPos, aState);
			tTile.setLevel(mLevel);
			mLevel.mTiles.put(aPos, tTile);
			return tTile;
		}

		/** The ideal-terrain rig on all three tiles + the Weed-EX shield against the 1% self-weed. */
		void terrainAll() {
			for (GT6CropBlockEntity tTile : new GT6CropBlockEntity[] {mCenter, mNorth, mSouth}) {
				tTile.setTerrainHumidity(12);
				tTile.setTerrainNutrients(12);
				tTile.setTerrainAirQuality(12);
			}
			mCenter.setStorages(0, 0, 100); // the :241 shield — no self-weed inside the lineage
		}

		/** The empty-crossing-base state the next generation starts from. */
		void reseatCenter() {
			mCenter.clear();
			mCenter.setCrossingBase(true);
			terrainAll();
		}
	}

	/**
	 * One lineage record — {generation, card(A=0/B=1), G, Ga, Re, dropCount, seedCount, cycles}.
	 */
	private static List<int[]> runLineage(Rig aRig, RandomSource aRNG, int aGenerations, List<int[]> aSink) {
		int[] tDonorA = {5, 5, 5};
		int[] tDonorB = {10, 10, 10};
		for (int tGen = 1; tGen <= aGenerations; tGen++) {
			// 种 — the picked seeds' stats plant the donors (generation 1 rides the base seeds)
			assertTrue(aRig.mNorth.tryPlantIn(A, 4, tDonorA[0], tDonorA[1], tDonorA[2], 0), "donor A planted");
			assertTrue(aRig.mSouth.tryPlantIn(B, 4, tDonorB[0], tDonorB[1], tDonorB[2], 0), "donor B planted");
			aRig.reseatCenter();

			// 杂 + 变 — the 1/3 gate, then the weighted card pick and the stat perturbation
			// (neighbor average ± rand(1+2n)-n, clamp 0-31) under the SAME seed
			int tCycles = 0;
			while (aRig.mCenter.crop() == null) {
				CropMath.tickCrop(aRig.mCenter, CANDIDATES, aRNG);
				if (++tCycles > 10000) throw new AssertionError("no crossing in 10000 cycles");
			}
			GT6CropCard tChild = aRig.mCenter.crop();
			int[] tChildStats = {aRig.mCenter.statGrowth(), aRig.mCenter.statGain(), aRig.mCenter.statResistance()};

			// 长 — to the harvest threshold
			while (!tChild.canBeHarvested(aRig.mCenter)) {
				CropMath.tickCrop(aRig.mCenter, CANDIDATES, aRNG);
				if (++tCycles > 10000) throw new AssertionError("no maturity in 10000 cycles");
			}

			// 收 — the produce roll, the size reset
			List<ItemStack> tDrops = CropMath.performHarvest(tChild, aRig.mCenter, aRNG);
			// 种 — the left-click seed pull carries the child stats off the tile
			List<ItemStack> tSeeds = CropMath.pickSeed(tChild, aRig.mCenter, aRNG);

			aSink.add(new int[] {tGen, tChild == A ? 0 : 1, tChildStats[0], tChildStats[1], tChildStats[2],
					tDrops.size(), tSeeds.size(), tCycles});

			// the picked seeds feed the next generation (the loop's stat evolution carrier)
			tDonorA = tDonorB = tChildStats;
			//? if forge {
			if (GT6CropSeeds.CROP_SEED.isPresent()) { // the FML test JVM binds the registry live (research.fml-test-memory)
			//?} else {
			/*if (GT6CropSeeds.CROP_SEED.isBound()) { // 21.1: the DeferredHolder face (Holder.isBound)
			 *///?}
				for (ItemStack tSeed : tSeeds) { // the live face: the seeds CARRY the child stats
					GT6CropSeeds.SeedData tData = GT6CropSeeds.readSeed(tSeed);
					assertNotNull(tData, "the live seed item writes the payload");
					assertEquals("gt6", tData.cropOwner(), "the owner stamp");
					assertEquals(tChild.name(), tData.cropId(), "the payload names the child card");
					assertEquals(tChildStats[0], tData.growth(), "the seed payload G = the child stat");
					assertEquals(tChildStats[1], tData.gain(), "the seed payload Ga");
					assertEquals(tChildStats[2], tData.resistance(), "the seed payload Re");
				}
			} else {
				for (ItemStack tSeed : tSeeds) { // the offline face: the base-seed copy fallback
					assertNull(GT6CropSeeds.readSeed(tSeed), "the unbound seed item answers the plain fallback");
				}
			}
		}
		return aSink;
	}


	// ---------------------------------------------------------------- the full chain

	/**
	 * The 种→长→杂→变→收→种 loop, 6 generations under seed 11258. The lineage table is the
	 * pinned contract (card sequence, child stats, drop/seed counts, cycle counts — all
	 * deterministic under the seed); every row also asserts the clamp law (0 ≤ stat ≤ 31)
	 * and the harvest-reset/pick-clear state machine.
	 */
	@Test
	public void seedToSeedLineageEvolvesUnderOneSeededRng() {
		Rig tRig = new Rig();
		List<int[]> tLineage = runLineage(tRig, RandomSource.create(SEED), 6, new ArrayList<>());

		// the pinned lineage — the whole-chain determinism contract under seed 11258
		int[][] tPinned = {
				{1, 0, 8, 8, 5, 0, 1, 16},
				{2, 1, 7, 8, 3, 1, 1, 15},
				{3, 1, 9, 7, 3, 1, 1, 17},
				{4, 1, 9, 8, 3, 3, 2, 40},
				{5, 0, 8, 7, 1, 0, 1, 32},
				{6, 0, 7, 6, 0, 1, 2, 24}};
		assertEquals(tPinned.length, tLineage.size(), "6 generations");
		for (int i = 0; i < tPinned.length; i++) {
			int[] tRow = tLineage.get(i);
			for (int j = 0; j < 8; j++) {
				assertEquals(tPinned[i][j], tRow[j], "lineage row " + (i + 1) + " column " + j);
			}
			// the clamp law rides every generation — the stats never leave 0-31
			assertTrue(tRow[2] >= 0 && tRow[2] <= 31, "G in 0-31 at gen " + tRow[0]);
			assertTrue(tRow[3] >= 0 && tRow[3] <= 31, "Ga in 0-31 at gen " + tRow[0]);
			assertTrue(tRow[4] >= 0 && tRow[4] <= 31, "Re in 0-31 at gen " + tRow[0]);
		}

		// the state machine: after 收 the size rode the after-harvest reset, after 种 the tile cleared
		assertNull(tRig.mCenter.crop(), "pickSeed cleared the tile (the :764 face)");
		assertEquals(1, tRig.mCenter.size(), "the cleared tile sits at size 1");
	}

	/**
	 * The clamp 0-31 boundary under saturation — both donors at 31/31/31 (resp. 0/0/0) for
	 * 100 generations: the perturbation is rand(1+2n)-n ∈ [-2,+2] around the 31 (resp. 0)
	 * average, so the raw walk crosses 32/33 (resp. -2/-1) regularly and every observed
	 * stat is the CLAMPED value. Any clamp regression turns this red within a generation.
	 */
	@Test
	public void clampBoundaryHoldsAcrossAHundredGenerations() {
		// the 31 ceiling — pinned first child under seed 11260, then the 100-gen bound walk
		Rig tCeiling = new Rig();
		tCeiling.mNorth.tryPlantIn(A, 4, 31, 31, 31, 0);
		tCeiling.mSouth.tryPlantIn(B, 4, 31, 31, 31, 0);
		tCeiling.reseatCenter();
		RandomSource tRng = RandomSource.create(11260L);
		int tCycles = 0;
		while (tCeiling.mCenter.crop() == null) {
			CropMath.tickCrop(tCeiling.mCenter, CANDIDATES, tRng);
			if (++tCycles > 10000) throw new AssertionError("no crossing");
		}
		assertEquals(30, tCeiling.mCenter.statGrowth(), "the pinned ceiling child G (avg 31, perturbation)");
		assertEquals(29, tCeiling.mCenter.statGain(), "the pinned ceiling child Ga (31-2)");
		assertEquals(31, tCeiling.mCenter.statResistance(), "the pinned ceiling child Re (the +2 arm clamped)");

		// the 100-generation bound walk over both fences
		for (int tGen = 0; tGen < 100; tGen++) {
			for (int tFence = 0; tFence < 2; tFence++) {
				Rig tRig = new Rig();
				int[] tStats = tFence == 0 ? new int[] {31, 31, 31} : new int[] {0, 0, 0};
				tRig.mNorth.tryPlantIn(A, 4, tStats[0], tStats[1], tStats[2], 0);
				tRig.mSouth.tryPlantIn(B, 4, tStats[0], tStats[1], tStats[2], 0);
				tRig.reseatCenter();
				while (tRig.mCenter.crop() == null) {
					CropMath.tickCrop(tRig.mCenter, CANDIDATES, tRng);
				}
				int[] tChild = {tRig.mCenter.statGrowth(), tRig.mCenter.statGain(), tRig.mCenter.statResistance()};
				for (int tStat : tChild) {
					assertTrue(tStat >= 0 && tStat <= 31, "clamp law: " + tStat + " at gen " + tGen + " fence " + tFence);
				}
				if (tFence == 0) assertTrue(tChild[0] >= 29 && tChild[1] >= 29 && tChild[2] >= 29,
						"the ceiling band keeps the child at 29-31 (avg 31)");
				else assertTrue(tChild[0] <= 2 && tChild[1] <= 2 && tChild[2] <= 2,
						"the floor band keeps the child at 0-2 (avg 0)");
			}
		}
	}

	// ---------------------------------------------------------------- the death branch

	/**
	 * The aux&gt;100 death branch — DEADLY (tier 10, Re 0) on barren soil dies under seed
	 * 11259 on the FIRST cycle (rand(32) > 0 drew nonzero), the reset face rides it; the
	 * Re-31 twin is immune (rand(32) > 31 impossible) and the (100-144)/100 floor pins
	 * growthPoints at 0 while the plant survives.
	 */
	@Test
	public void deathBranchFiresAndReThirtyOneIsImmune() {
		GT6CropBlockEntity tTile = new GT6CropBlockEntity(CropBlockEntityTest.sBeType, P_CENTER,
				CropBlockEntityTest.sBlock.defaultBlockState());
		assertTrue(tTile.tryPlantIn(DEADLY, 2, 0, 0, 0, 0), "planted");
		tileBarren(tTile);
		CropMath.tickCrop(tTile, CANDIDATES, RandomSource.create(DEATH_SEED));
		assertNull(tTile.crop(), "the first deficit cycle kills (rand(32) > Re 0)");
		assertEquals(1, tTile.size(), "clear() rode the death branch — size back to 1");
		assertEquals(0, tTile.statGrowth(), "stats wiped by the death reset (TileEntityCrop.java:301)");
		assertEquals(-1, tTile.terrainHumidity(), "the terrain staleness of the reset");

		// the immunity twin — same seed, Re 31
		GT6CropBlockEntity tHardy = new GT6CropBlockEntity(CropBlockEntityTest.sBeType, P_CENTER,
				CropBlockEntityTest.sBlock.defaultBlockState());
		assertTrue(tHardy.tryPlantIn(DEADLY, 2, 0, 0, 31, 0), "planted");
		tileBarren(tHardy);
		RandomSource tRng = RandomSource.create(DEATH_SEED);
		for (int tCycle = 1; tCycle <= 20; tCycle++) {
			CropMath.tickCrop(tHardy, CANDIDATES, tRng);
			assertNotNull(tHardy.crop(), "Re 31 is immune at cycle " + tCycle);
			assertEquals(0, tHardy.growthPoints(), "aux 144 > 100 → the (100-144)/100 floor at cycle " + tCycle);
			tileBarren(tHardy); // the terrain stays barren (the engine never writes terrain)
		}
		assertEquals(DEADLY, tHardy.crop(), "the plant survives all 20 cycles");
	}

	private static void tileBarren(GT6CropBlockEntity aTile) {
		aTile.setTerrainHumidity(0);
		aTile.setTerrainNutrients(0);
		aTile.setTerrainAirQuality(0);
	}

	// ---------------------------------------------------------------- the level-free BE driver

	/**
	 * The BE offline driver face — a {@link GT6CropBlockEntity} with {@code level == null}
	 * drives the FULL planted chain through the pure engine: plant → 256t cycles to
	 * maturity → harvest → pick, no world service anywhere (the neighbor walk answers null,
	 * the seed face answers the offline fallback, the storage shield suppresses the weed
	 * arms the empty tiles would roll).
	 */
	@Test
	public void blockEntityDrivesLevelFree() {
		GT6CropBlockEntity tTile = new GT6CropBlockEntity(CropBlockEntityTest.sBeType, P_CENTER,
				CropBlockEntityTest.sBlock.defaultBlockState());
		assertNull(tTile.getLevel(), "the level-free precondition");
		assertTrue(tTile.tryPlantIn(A, 1, 3, 3, 3, 0), "planted");
		tileBarren2Ideal(tTile);
		tTile.setStorages(0, 0, 100); // the shield — the planted tile still rolls the 1% self-weed gate? no:
		// the self-weed arm only runs on crop()==null, but the shield keeps this tile's weed work honest

		RandomSource tRng = RandomSource.create(SEED);
		int tCycles = 0;
		while (!A.canBeHarvested(tTile)) {
			CropMath.tickCrop(tTile, CANDIDATES, tRng);
			if (++tCycles > 10000) throw new AssertionError("no maturity level-free");
		}
		assertEquals(2, tTile.size(), "one size-up to the harvest threshold (harvestSize 2)");
		assertTrue(tCycles < 60, "the growth ran at the ideal-terrain rate, got " + tCycles + " cycles");

		List<ItemStack> tDrops = CropMath.performHarvest(A, tTile, tRng);
		assertNotNull(tDrops, "mature harvests level-free");
		assertEquals(2, tTile.size(), "the after-harvest reset");
		List<ItemStack> tSeeds = CropMath.pickSeed(A, tTile, tRng);
		assertNull(tTile.crop(), "the pick cleared the tile");
		assertTrue(tSeeds.size() <= 2, "the seed roll stays in the two-roll band");
		assertTrue(tTile.neighbor(0) == null && tTile.neighbor(3) == null, "the neighbor walk is null-safe level-free");
		assertTrue(tTile.readout().startsWith("empty"), "the readout face answers the cleared tile");
	}

	private static void tileBarren2Ideal(GT6CropBlockEntity aTile) {
		aTile.setTerrainHumidity(12);
		aTile.setTerrainNutrients(12);
		aTile.setTerrainAirQuality(12);
	}
}
