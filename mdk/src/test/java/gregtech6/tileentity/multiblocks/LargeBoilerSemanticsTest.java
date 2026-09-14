package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregtech6.fluid.GTFluids;

/**
 * Task p13-large-boiler — the offline acceptance fixture for the Large Boiler business
 * semantics (the W3 GTBoilerTankBlockEntityTest form). Covers the five-target
 * load-balanced steam output (the :203-256 five-Delegator algorithm, all four branches
 * pinned as a truth table), the conversion truth table (:180-190, the W3-style triple
 * min + the scale decrement with the 5000 floor + the distilled-water immunity), the
 * cooldown drain (:193-201), the explosion trigger table + the :326 strength formula
 * {@code 2 + max(1, sqrt(steam)/1000)} (the W3 cross-check — the LARGE-BOILER formula,
 * NOT the single-block sqrt/100), the pressurised dismantle + the chisel detonation,
 * and the five-variant row table (Loader:1248-1252 zero diff, the wallPath parameterisation
 * column).
 *
 * <p>The offline fluid stand-ins (the engine setFluidSeams pattern): steam → LAVA, water
 * → WATER, distilled → FLOWING_WATER. The load-balance cases call the package-private
 * {@code emitSteam} directly with the {@code setSteamTargetsOverride} seam — the same
 * seam shape the W3 fixture rides.
 */
public class LargeBoilerSemanticsTest extends GTMultiBlocksOfflineTestBase {

	static final BlockPos POS = new BlockPos(3, 4, 5);

	static BlockEntityType<FixtureBoiler> sType;

	/** The concrete test BE — the boiler class over a vanilla-block BET, fixture blocks bound. */
	public static final class FixtureBoiler extends TileEntityLargeBoiler {
		public FixtureBoiler(BlockPos aPos, BlockState aState) {
			super(sType, aPos, aState);
		}
		@Override
		protected net.minecraft.world.level.block.Block getWallBlock() {
			return Blocks.BRICKS;
		}
		@Override
		protected net.minecraft.world.level.block.Block getTransmitterBlock() {
			return Blocks.STONE;
		}
	}

	/**
	 * A capacity-bounded steam-counting sink (the W3 CountingSink over a capacity): the
	 * SIMULATE probe reports the free capacity, the EXECUTE fills accumulate — the truth
	 * table needs bounded targets to hit the :231/:234 branches.
	 */
	public static class BoundedSink implements IFluidHandler {
		public long pushed = 0;
		public final long capacity;

		public BoundedSink(long aCapacity) {
			capacity = aCapacity;
		}

		@Override public int getTanks() {return 1;}
		@Override public FluidStack getFluidInTank(int aTank) {return FluidStack.EMPTY;}
		@Override public int getTankCapacity(int aTank) {return Integer.MAX_VALUE;}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {return true;}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			long tFree = capacity - pushed;
			int tAccepted = (int)Math.min(tFree, aResource.getAmount());
			if (tAccepted <= 0) return 0;
			if (aAction.execute()) pushed += tAccepted;
			return tAccepted;
		}

		@Override public FluidStack drain(FluidStack aResource, FluidAction aAction) {return FluidStack.EMPTY;}
		@Override public FluidStack drain(int aMaxDrain, FluidAction aAction) {return FluidStack.EMPTY;}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<FixtureBoiler>[] tHolder = (BlockEntityType<FixtureBoiler>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(FixtureBoiler::new, Blocks.BRICKS, Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** A fixture with the offline fluid stand-ins wired (steam=LAVA, distw=FLOWING_WATER, water=WATER). */
	private static FixtureBoiler boiler() {
		FixtureBoiler tBoiler = new FixtureBoiler(POS, Blocks.BRICKS.defaultBlockState());
		tBoiler.setFluidSeams(
				aAmount -> new FluidStack(Fluids.LAVA, (int)Math.min(Integer.MAX_VALUE, Math.max(1, aAmount))),
				f -> f == Fluids.FLOWING_WATER,
				f -> f == Fluids.WATER);
		return tBoiler;
	}

	// ---------------------------------------------------------------------------
	// 1. the five-target load balance (:203-256, the four branches)
	// ---------------------------------------------------------------------------

	/** The uniform load-balance rig: capacity 12000, 11000 L preloaded (tAmount 5000, above the 3/4 gate → offer = mOutput * 2). */
	private static FixtureBoiler balanced(int aOutput) {
		FixtureBoiler tBoiler = boiler();
		tBoiler.mOutput = aOutput;
		tBoiler.mTanks[1].setCapacity(12000);
		preload(tBoiler, 11000); // tAmount = 11000 - 6000 = 5000
		return tBoiler;
	}

	private static void preload(FixtureBoiler aBoiler, long aAmount) {
		aBoiler.mTanks[1].setEmpty();
		aBoiler.mTanks[1].add(aAmount, new FluidStack(Fluids.LAVA, (int)Math.min(Integer.MAX_VALUE, aAmount)));
	}

	private static void emit(FixtureBoiler aBoiler, IFluidHandler a0, IFluidHandler a1, IFluidHandler a2) {
		aBoiler.setSteamTargetsOverride(new IFluidHandler[] {a0, a1, a2, null, null});
		aBoiler.emitSteam(aBoiler.mTanks[1].amount() - aBoiler.mTanks[1].capacity() / 2);
	}

	@Test
	public void singleTargetTakesTheWholeOffer() {
		FixtureBoiler tBoiler = balanced(100); // the offer = min(5000 > 3000 ? 200 : 100, 5000) = 200 (the 3/4 double rate)
		BoundedSink tOnly = new BoundedSink(10000);
		tBoiler.setSteamTargetsOverride(new IFluidHandler[] {tOnly, null, null, null, null});
		tBoiler.emitSteam(5000);
		assertEquals(200, tOnly.pushed, "the single target takes the whole offer (:225-229)");
		assertEquals(10800, tBoiler.mTanks[1].amount(), "what moved left the tank; the rest stays");
	}

	@Test
	public void offerBelowHalfGateUsesTheSingleRate() {
		FixtureBoiler tBoiler = balanced(100);
		preload(tBoiler, 6500); // tAmount = 500 — NOT above capacity/4 (3000) → the single rate
		BoundedSink tOnly = new BoundedSink(10000);
		tBoiler.setSteamTargetsOverride(new IFluidHandler[] {tOnly, null, null, null, null});
		tBoiler.emitSteam(500); // offer = min(100, 500) = 100
		assertEquals(100, tOnly.pushed, "the half-gate offer is the single rate (:207)");
	}

	@Test
	public void totalAcceptanceBelowTheOfferDrainsEveryoneToTheirMax() {
		FixtureBoiler tBoiler = balanced(100); // offer 200
		BoundedSink tSmall = new BoundedSink(30);
		BoundedSink tMid = new BoundedSink(40);
		tBoiler.setSteamTargetsOverride(new IFluidHandler[] {tSmall, tMid, null, null, null});
		tBoiler.emitSteam(5000); // sum(30+40)=70 <= 200 → the :251-253 everyone branch
		assertEquals(30, tSmall.pushed, "the small target drains to its maximum (:252)");
		assertEquals(40, tMid.pushed, "the mid target drains to its maximum (:252)");
		assertEquals(11000 - 70, tBoiler.mTanks[1].amount(), "only the accepted litres left the tank");
	}

	@Test
	public void quotaSweepSingleSurvivorAndEvenSplitBranches() {
		FixtureBoiler tBoiler = balanced(100); // offer 200
		// targets accept 30 / 120 / 120 → sum 270 > 200 → the :231-250 fair split
		BoundedSink tSmall = new BoundedSink(30);   // 30 <= 100 → swept, move(100) delivers 30, tMoveable 170, targets 2
		BoundedSink tFirst = new BoundedSink(120);  // 120 > 100 → NOT swept
		BoundedSink tSecond = new BoundedSink(120); // 120 > 100 → NOT swept
		tBoiler.setSteamTargetsOverride(new IFluidHandler[] {tSmall, tFirst, tSecond, null, null});
		tBoiler.emitSteam(5000);
		// :233-239 — the sweep: only the small target is <= offer/originalTargets (100);
		// :245-249 — the even split with the upstream decrement ORDER: the divisor drops
		// AFTER each move (:248 --tTargets), so the second move is 85/1 = 85 — the offer
		// drains fully across the two survivors
		assertEquals(30, tSmall.pushed, "the swept target got the quota attempt, delivered what it had");
		assertEquals(85, tFirst.pushed, "the first survivor got 170/2 (:247)");
		assertEquals(85, tSecond.pushed, "the second survivor got 85/1 — the upstream decrement order (:247 after :248)");
		assertEquals(10800, tBoiler.mTanks[1].amount(), "the offer drained fully");
	}

	@Test
	public void singleSurvivorTakesTheWholeRemainder() {
		FixtureBoiler tBoiler = balanced(100); // offer 200
		// targets accept 50 / 1000 → sum 1050 > 200 → :231; 50 <= 100 → swept;
		// ONE survivor remains → :240-244 the survivor takes the whole tMoveable
		BoundedSink tSmall = new BoundedSink(50);
		BoundedSink tSurvivor = new BoundedSink(1000);
		tBoiler.setSteamTargetsOverride(new IFluidHandler[] {tSmall, tSurvivor, null, null, null});
		tBoiler.emitSteam(5000);
		assertEquals(50, tSmall.pushed, "the swept target (:234-236)");
		assertEquals(150, tSurvivor.pushed, "the single survivor takes the whole remainder (:240-244)");
		assertEquals(10800, tBoiler.mTanks[1].amount(), "the offer drained fully");
	}

	@Test
	public void offerBelowTargetCountPushesNothing() {
		FixtureBoiler tBoiler = balanced(1); // the offer = 2 < the 3 accepting targets → the :230 gate blocks
		BoundedSink tA = new BoundedSink(1000);
		BoundedSink tB = new BoundedSink(1000);
		BoundedSink tC = new BoundedSink(1000);
		tBoiler.setSteamTargetsOverride(new IFluidHandler[] {tA, tB, tC, null, null});
		tBoiler.emitSteam(5000);
		assertEquals(0, tA.pushed, "the :230 tTargets gate keeps the offer in the tank");
		assertEquals(0, tB.pushed);
		assertEquals(0, tC.pushed);
		assertEquals(11000, tBoiler.mTanks[1].amount());
	}

	@Test
	public void noAcceptingTargetLeavesEverything() {
		FixtureBoiler tBoiler = balanced(100);
		BoundedSink tFull = new BoundedSink(0); // accepts nothing → the :223 probe drops it
		tBoiler.setSteamTargetsOverride(new IFluidHandler[] {tFull, null, null, null, null});
		tBoiler.emitSteam(5000);
		assertEquals(0, tFull.pushed);
		assertEquals(11000, tBoiler.mTanks[1].amount(), "the :223 else-arm dropped the only target");
	}

	// ---------------------------------------------------------------------------
	// 2. the conversion truth table (:180-190, the W3 cross-check)
	// ---------------------------------------------------------------------------

	@Test
	public void conversionFollowsTheTripleMinAtTheLargeBoilerScale() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9); // rng(10) != 0 — never scales
		preload(tBoiler, 0);
		tBoiler.mTanks[1].setCapacity(2000000); // mOutput 200 — the :80 pair (steamTankCapacity)
		tBoiler.mOutput = 200;
		tBoiler.mCapacity = 2000000;
		tBoiler.mTanks[0].setEmpty();
		tBoiler.mTanks[0].add(50, new FluidStack(Fluids.WATER, 50));
		tBoiler.mEnergy = 8000; // 8000 / 80 = 100 conversions affordable
		// water 50 < energy 100 < capacity/2560 781 → tConversions = 50
		tBoiler.onTick(1, true);
		assertEquals(0, tBoiler.mTanks[0].amount(), "all 50 water converted");
		assertEquals(8000, tBoiler.mTanks[1].amount(), "50 conversions × 160 L at eff 10000");
		assertEquals(4000, tBoiler.mEnergy, "50 × 80 HU consumed");
	}

	@Test
	public void scaleDecrementWithTheFloorAndTheDistilledImmunity() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 0); // rng(10) == 0 — scales every tick
		tBoiler.mTanks[1].setCapacity(2000000);
		tBoiler.mOutput = 200;
		tBoiler.mCapacity = 2000000;
		// NOTE the :182/:183 order — the scale probe runs AFTER the water remove, so the
		// water must SURVIVE the conversion (10 conversions, 20 L tanked) to calcify at all
		tBoiler.mTanks[0].setEmpty();
		tBoiler.mTanks[0].add(20, new FluidStack(Fluids.WATER, 20));
		tBoiler.mEnergy = 800;
		tBoiler.onTick(1, true);
		assertEquals(9990, tBoiler.mEfficiency, "10 conversions × the :184 decrement (water left, :183 has() holds)");
		// the distilled immunity: the FLOWING_WATER stand-in
		tBoiler.mTanks[0].setEmpty();
		tBoiler.mTanks[0].add(20, new FluidStack(Fluids.FLOWING_WATER, 20));
		tBoiler.mEnergy = 800;
		tBoiler.onTick(2, true);
		assertEquals(9990, tBoiler.mEfficiency, "the distilled water is immune (:183)");
		// the floor: drive to 4999 → clamps at 5000
		tBoiler.mEfficiency = 5000 + 4;
		tBoiler.mTanks[0].setEmpty();
		tBoiler.mTanks[0].add(20, new FluidStack(Fluids.WATER, 20));
		tBoiler.mEnergy = 800;
		tBoiler.onTick(3, true);
		assertEquals(5000, tBoiler.mEfficiency, "the :185 floor");
	}

	// ---------------------------------------------------------------------------
	// 3. the cooldown drain (:193-201)
	// ---------------------------------------------------------------------------

	@Test
	public void cooldownBleedsHeatAndTrashesSteam() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.mTanks[1].setCapacity(200000);
		tBoiler.mOutput = 100;
		tBoiler.mCapacity = 200000;
		preload(tBoiler, 100000);
		tBoiler.mCoolDownResetTimer = 0; // expired — the drain arms
		tBoiler.mEnergy = 10000;
		tBoiler.mStructureOkay = true; // the intact-structure cheap path (barometer 14 > 4 otherwise arms the probe)
		tBoiler.onTick(1, true);
		assertEquals(10000 - (100 * 64) / GTFluids.STEAM_PER_EU, tBoiler.mEnergy, "the :195 heat bleed");
		assertEquals(100000 - 100 * 64, tBoiler.mTanks[1].amount(), "the :196 steam trash");
		// the re-arm only when the store empties (:197-199)
		assertEquals(0, tBoiler.mCoolDownResetTimer, "the timer stays 0 while heat remains");
	}

	@Test
	public void cooldownRearmsWhenTheStoreEmpties() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.mTanks[1].setCapacity(200000);
		tBoiler.mOutput = 100;
		tBoiler.mCapacity = 200000;
		preload(tBoiler, 100000);
		tBoiler.mCoolDownResetTimer = 0;
		tBoiler.mEnergy = 100; // under one tick's bleed (3200) — drained to 0
		tBoiler.mStructureOkay = true;
		tBoiler.onTick(1, true);
		assertEquals(0, tBoiler.mEnergy, "the :198 clamp");
		assertEquals(128, tBoiler.mCoolDownResetTimer, "the :199 re-arm");
	}

	// ---------------------------------------------------------------------------
	// 4. the explosion trigger table + the :326 strength formula
	// ---------------------------------------------------------------------------

	@Test
	public void explosionStrengthIsTwoPlusMaxOneSqrtOverThousand() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.mTanks[1].setCapacity(100000000); // the default 2048000 tank would clamp the big volumes
		preload(tBoiler, 0);
		tBoiler.explode(false);
		assertEquals(3.0F, tBoiler.mExplosionStrength, 0.0001F, "an empty tank: 2 + max(1, 0) = 3");
		preload(tBoiler, 1000000); // sqrt = 1000 → /1000 = 1 → max(1, 1) = 1
		tBoiler.explode(false);
		assertEquals(3.0F, tBoiler.mExplosionStrength, 0.0001F, "the max floor holds at exactly 1");
		preload(tBoiler, 4000000); // sqrt = 2000 → /1000 = 2
		tBoiler.explode(false);
		assertEquals(4.0F, tBoiler.mExplosionStrength, 0.0001F, "2 + 2 = 4");
	}

	@Test
	public void overheatTriggersTheBufferedExplode() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		preload(tBoiler, 0);
		tBoiler.mCoolDownResetTimer = 128; // no cooldown this tick
		tBoiler.mEnergy = tBoiler.mCapacity + 1; // the :262 overheat arm
		tBoiler.onTick(1, true);
		assertTrue(tBoiler.mExplosionStrength > 0, "the overheated boiler explodes (buffered F form)");
		assertEquals(3.0F, tBoiler.mExplosionStrength, 0.0001F, "the empty-tank strength floor");
	}

	@Test
	public void fullSteamTankTriggersTheBufferedExplode() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.mTanks[1].setCapacity(200000);
		tBoiler.mOutput = 200;
		tBoiler.mCapacity = 200000;
		preload(tBoiler, 200000); // FULL
		tBoiler.mCoolDownResetTimer = 128;
		tBoiler.onTick(1, true);
		assertTrue(tBoiler.mExplosionStrength > 0, "the :262 isFull arm");
	}

	@Test
	public void brokenStructureAtPressureTriggersTheBufferedExplode() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		FixtureBoiler tBoiler = sType.create(new BlockPos(100, 64, 100), Blocks.BRICKS.defaultBlockState());
		tBoiler.setLevel(tLevel);
		tBoiler.mFacing = 2;
		tLevel.mStates.put(tBoiler.getBlockPos(), Blocks.BRICKS.defaultBlockState());
		tLevel.mBlockEntities.put(tBoiler.getBlockPos(), tBoiler);
		tBoiler.setFluidSeams(
				aAmount -> new FluidStack(Fluids.LAVA, (int)Math.min(Integer.MAX_VALUE, Math.max(1, aAmount))),
				f -> f == Fluids.FLOWING_WATER,
				f -> f == Fluids.WATER);
		tBoiler.setRngOverride(() -> 9);
		tBoiler.mTanks[1].setCapacity(20000);
		tBoiler.mOutput = 200;
		tBoiler.mCapacity = 20000;
		preload(tBoiler, 5000); // 5000/20000 → barometer = 7 > 4 — pressurised
		tBoiler.mEnergy = 0;
		tBoiler.mCoolDownResetTimer = 128; // no cooldown, no conversion (no water)
		tBoiler.mStructureOkay = true;     // the structure was fine...
		tBoiler.mStructureChanged = true;  // ...until a wall was broken (the part-block hook)
		tBoiler.onTick(1, true);           // the :262 tick probe — the empty world fails the check
		assertTrue(tBoiler.mExplosionStrength > 0, "breaking any structure wall at pressure detonates (the tick probe, NOT onBlockUpdate)");
	}

	@Test
	public void intactStructureAtPressureDoesNotExplode() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.mTanks[1].setCapacity(20000);
		tBoiler.mOutput = 200;
		tBoiler.mCapacity = 20000;
		preload(tBoiler, 5000); // barometer 7 > 4
		tBoiler.mBarometer = 7;
		tBoiler.mEnergy = 0;
		tBoiler.mCoolDownResetTimer = 128;
		tBoiler.mStructureOkay = true;
		tBoiler.mStructureChanged = false; // the cheap path keeps the formed verdict
		tBoiler.onTick(1, true);
		assertEquals(0, tBoiler.mExplosionStrength, "an intact structure at pressure holds");
	}

	// ---------------------------------------------------------------------------
	// 5. the dismantle + chisel faces
	// ---------------------------------------------------------------------------

	@Test
	public void dismantleDetonatesAbovePressureFour() {
		// NO level (the W3 dismantle form): isServerSide() is the !isClientSide negation, so
		// the level-less fixture still runs the server arm, and the instant explode only
		// BUFFERS (hasLevel() false keeps Level.explode out of the offline stub)
		FixtureBoiler tBoiler = boiler();
		tBoiler.mBarometer = 5;
		assertTrue(tBoiler.dismantle(null), "the null player IS the non-creative counterfactual (:314)");
		assertTrue(tBoiler.mExplosionStrength >= 2.0, "the instant T form buffered the blast");
		tBoiler.mBarometer = 4;
		tBoiler.mExplosionStrength = 0;
		assertFalse(tBoiler.dismantle(null), "sub-4 pressure dismantles quietly");
		assertEquals(0, tBoiler.mExplosionStrength);
	}

	@Test
	public void chiselDetonatesAboveFifteen() {
		FixtureBoiler tBoiler = boiler();
		preload(tBoiler, 0);
		tBoiler.mEfficiency = 9000;
		tBoiler.mBarometer = 16;
		tBoiler.chisel(null);
		assertTrue(tBoiler.mExplosionStrength > 0, "the :282-283 detonation branch");
		tBoiler.mExplosionStrength = 0;
		tBoiler.mEfficiency = 9000;
		tBoiler.mBarometer = 15;
		int tResult = tBoiler.chisel(null);
		assertEquals(1000, tResult, "the repair branch returns 10000 - efficiency");
		assertEquals(10000, tBoiler.mEfficiency, "the descale");
		assertEquals(0, tBoiler.mTanks[1].amount(), "the vent");
		assertEquals(0, tBoiler.mEnergy, "the heat reset");
	}

	// ---------------------------------------------------------------------------
	// 6. the five-variant row table (Loader :1248-1252 zero diff) + the wall parameterisation
	// ---------------------------------------------------------------------------

	@Test
	public void theFiveVariantRowsAreTheLoaderLinesVerbatim() {
		var tRows = gregtech6.registry.GTMultiBlocks.LARGE_BOILER_ROWS;
		assertEquals(5, tRows.size(), "five Loader rows (:1248-1252)");
		// the line order = the registration order (NOT id-ordered: 17201/17205/17202/17203/17204)
		String[][] tExpected = {
			// path, display, material, metaId, output(raw*STEAM_PER_EU), hardness, wallPath
			{"large_boiler_stainless_steel", "Stainless Steel Boiler Main Barometer", "Stainless Steel", "17201", "8192",   "6.0", "dense_wall_stainless_steel"},
			{"large_boiler_invar"          , "Invar Boiler Main Barometer"           , "Invar"          , "17205", "8192",   "6.0", "dense_wall_invar"},
			{"large_boiler_titanium"       , "Titanium Boiler Main Barometer"        , "Titanium"       , "17202", "16384",  "9.0", "dense_wall_titanium"},
			{"large_boiler_tungstensteel"  , "Tungstensteel Boiler Main Barometer"   , "Tungstensteel"  , "17203", "32768",  "12.5", "dense_wall_tungstensteel"},
			{"large_boiler_adamantium"     , "Adamantium Boiler Main Barometer"      , "Adamantium"     , "17204", "262144", "100.0", "dense_wall_adamantium"}
		};
		for (int i = 0; i < 5; i++) {
			var tRow = tRows.get(i);
			assertEquals(tExpected[i][0], tRow.path(), "row " + i + " path");
			// the composed face (task p20-i18n-compose-rows): the fixture display replays from
			// the template + the material word — a per-row expansion pin
			assertEquals(tExpected[i][1], "%s Boiler Main Barometer".replace("%s", tRow.material()),
					"row " + i + " composed display replay (the Loader line verbatim)");
			assertEquals("gt6.row.mat." + tRow.path().substring("large_boiler_".length()),
					gregtech6.registry.GTMultiBlocks.boilerMatUnitKeyOf(tRow), "row " + i + " mat unit key");
			assertEquals(tExpected[i][2], tRow.material(), "row " + i + " material");
			assertEquals(Integer.parseInt(tExpected[i][3]), tRow.metaId(), "row " + i + " meta id");
			assertEquals(Long.parseLong(tExpected[i][4]), tRow.outputSteamPerTick(), "row " + i + " output = raw * STEAM_PER_EU");
			assertEquals(Float.parseFloat(tExpected[i][5]), tRow.hardness(), "row " + i + " hardness == resistance");
			assertEquals(tExpected[i][6], tRow.wallPath(), "row " + i + " wall (the NBT_DESIGN parameterisation)");
		}
	}

	@Test
	public void theWallRowsAndTheTransmitterRowAreTheLoaderLinesVerbatim() {
		var tWalls = gregtech6.registry.GTMultiBlocks.WALL_ROWS;
		assertEquals(11, tWalls.size(), "eleven Dense Wall rows (5 at :1159-1165 + the 6 additions"
			+ " at :1155-1165, task p29-w3-nbtdesign-parts — appended, the p13 EDIT-ruling shape)");
		String[][] tExpected = {
			{"dense_wall_stainless_steel", "Dense Stainless Steel Wall", "18022", "6.0"},
			{"dense_wall_invar"          , "Dense Invar Wall"           , "18027", "6.0"},
			{"dense_wall_titanium"       , "Dense Titanium Wall"        , "18026", "9.0"},
			{"dense_wall_tungstensteel"  , "Dense Tungstensteel Wall"   , "18023", "12.5"},
			{"dense_wall_adamantium"     , "Dense Adamantium Wall"      , "18025", "100.0"}
		};
		for (int i = 0; i < 5; i++) {
			assertEquals(tExpected[i][0], tWalls.get(i).path(), "wall " + i + " path");
			assertEquals(tExpected[i][1], "Dense %s Wall".replace("%s", tWalls.get(i).matDisplay()),
					"wall " + i + " composed display replay");
			assertEquals(Integer.parseInt(tExpected[i][2]), tWalls.get(i).metaId(), "wall " + i + " part id");
			assertEquals(Float.parseFloat(tExpected[i][3]), tWalls.get(i).hardness(), "wall " + i + " hardness");
		}
		var tTx = gregtech6.registry.GTMultiBlocks.TRANSMITTER_ROW;
		assertEquals("heat_transmitter", tTx.path());
		// the transmitter stays ATOMIC (task p20-i18n-compose-rows) — its word rides the row's
		// matDisplay column as the whole-string lang VALUE (the bare-noun form, nothing to compose)
		assertEquals("Heat Transmitter", tTx.matDisplay(), "the :1176 row verbatim");
		assertEquals(18101, tTx.metaId());
		assertEquals(10.0F, tTx.hardness());
	}

	@Test
	public void everyVariantWallPathResolvesInsideTheWallRowTable() {
		// the wall PARAMETERISATION truth table: each variant's NBT_DESIGN wall is a row of
		// the wall table (the live getWallBlock hook resolves the same path)
		for (var tRow : gregtech6.registry.GTMultiBlocks.LARGE_BOILER_ROWS) {
			boolean tResolved = false;
			for (var tWall : gregtech6.registry.GTMultiBlocks.WALL_ROWS) {
				if (tWall.path().equals(tRow.wallPath())) tResolved = true;
			}
			assertTrue(tResolved, "variant " + tRow.path() + " wall '" + tRow.wallPath() + "' resolves");
		}
		// the :80 double assignment — the steam tank AND the overheat ceiling pair with mOutput
		FixtureBoiler tBoiler = boiler();
		tBoiler.setOutput(16384);
		assertEquals(16384, tBoiler.mOutput);
		assertEquals(163840000, tBoiler.mCapacity, "mCapacity = mOutput * 10000 (the :80 double assignment, lesson 227②)");
		assertEquals(163840000, tBoiler.mTanks[1].getCapacity(), "the steam tank rides the same value");
		assertEquals(128000, tBoiler.mTanks[0].getCapacity(), "the water tank is fixed at 128000 L (:71)");
	}
}
