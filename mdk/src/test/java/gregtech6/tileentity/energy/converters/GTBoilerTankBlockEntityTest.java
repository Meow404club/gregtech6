package gregtech6.tileentity.energy.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GT6Boilers;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task p13-boiler-tank — the offline acceptance fixture for the Steam Boiler Tank
 * semantics (the GTGeneratorSolidBlockEntityTest fixture form). Covers the conversion
 * truth table (the :116-126 triple min, the efficiency scaling, the scale decrement with
 * its 5000 floor and the distilled-water immunity), the cooldown drain (:129-137 — the
 * 128t gate, the drain rate, the steam trash), the push gate (:139-142 — the half gate,
 * the 3/4 double rate, the top-face single door), the explosion trigger table + the :215
 * strength formula, the energy face (:249-260), the water-in face (:236-262, the funnel
 * + the fluid identity half — the capability door face half is live-only, the
 * GTFluidPipeFlowControlTest ruling) and the 26-row table (Loader:553-579 zero diff).
 *
 * <p>The offline fluid stand-ins (the engine setFluidSeams pattern): steam → LAVA, water
 * → WATER, distilled → FLOWING_WATER. The gt6 fluids do not exist offline; the live
 * defaults resolve them through the registry.
 */
public class GTBoilerTankBlockEntityTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(3, 4, 5);
	static BlockEntityType<FixtureBoiler> sType;

	/** The concrete test BE — the boiler class over a vanilla-block BET. */
	public static final class FixtureBoiler extends GTBoilerTankBlockEntity {
		public FixtureBoiler(BlockPos aPos, BlockState aState) {
			super(sType, aPos, aState);
		}
	}

	/** A steam-counting sink (the push acceptance counter; records each executed fill). */
	public static class CountingSink implements IFluidHandler {
		public long pushed = 0;
		public int fills = 0;

		@Override public int getTanks() {return 1;}
		@Override public FluidStack getFluidInTank(int aTank) {return FluidStack.EMPTY;}
		@Override public int getTankCapacity(int aTank) {return 1000000;}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {return true;}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			if (aAction.execute()) {
				pushed += aResource.getAmount();
				fills++;
			}
			return aResource.getAmount();
		}

		@Override public FluidStack drain(FluidStack aResource, FluidAction aAction) {return FluidStack.EMPTY;}
		@Override public FluidStack drain(int aMaxDrain, FluidAction aAction) {return FluidStack.EMPTY;}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<FixtureBoiler>[] tHolder = (BlockEntityType<FixtureBoiler>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(FixtureBoiler::new, Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** A fixture with the offline fluid stand-ins wired (steam=LAVA, distw=FLOWING_WATER, water=WATER). */
	private static FixtureBoiler boiler() {
		FixtureBoiler tBoiler = new FixtureBoiler(POS, Blocks.STONE.defaultBlockState());
		tBoiler.setFluidSeams(
				aAmount -> new FluidStack(Fluids.LAVA, (int)Math.min(Integer.MAX_VALUE, Math.max(1, aAmount))),
				f -> f == Fluids.FLOWING_WATER,
				f -> f == Fluids.WATER);
		return tBoiler;
	}

	/** Fills the water tank with plain water. */
	private static void fillWater(FixtureBoiler aBoiler, long aAmount) {
		aBoiler.mTanks[0].setEmpty();
		aBoiler.mTanks[0].add(aAmount, new FluidStack(Fluids.WATER, (int)Math.min(Integer.MAX_VALUE, aAmount)));
	}

	/** Fills the water tank with the distilled stand-in. */
	private static void fillDistw(FixtureBoiler aBoiler, long aAmount) {
		aBoiler.mTanks[0].setEmpty();
		aBoiler.mTanks[0].add(aAmount, new FluidStack(Fluids.FLOWING_WATER, (int)Math.min(Integer.MAX_VALUE, aAmount)));
	}

	/** Fills the water tank with an arbitrary stand-in identity (the SpDew/MnWtr controls). */
	private static void fillStandin(FixtureBoiler aBoiler, Fluid aFluid, long aAmount) {
		aBoiler.mTanks[0].setEmpty();
		aBoiler.mTanks[0].add(aAmount, new FluidStack(aFluid, (int)Math.min(Integer.MAX_VALUE, aAmount)));
	}

	/**
	 * The sustained-run harness: {@code aTicks} CONVERSION ticks at 3 conversions/tick
	 * (mOutput 1 → the steam tank 10000 → the :116 lattice bound 10000/2560 = 3), venting
	 * the steam tank after every tick — the harness keeps the push gate (:139-142) and the
	 * isFull trigger (:148) out of the assertion, so the only variable left is the :119
	 * scaling verdict. The budget per run: water 4000 → 1333 ticks at 3/t ≥ the 1200
	 * asked; HU 300000 → 3750 whole ticks ≥ ditto. Returns the cumulative steam yield.
	 */
	private static long runConversionTicks(FixtureBoiler aBoiler, int aTicks) {
		long tYield = 0;
		for (int t = 0; t < aTicks; t++) {
			aBoiler.onTick(t + 1, true);
			tYield += aBoiler.mTanks[1].amount();
			aBoiler.mTanks[1].setEmpty();
		}
		return tYield;
	}

	/** The sustained-run budget: mOutput 1, full water tank, 300000 HU (see runConversionTicks). */
	private static void primeSustainedRun(FixtureBoiler aBoiler) {
		aBoiler.setOutput(1); // steam tank 10000 → the /2560 lattice = 3 conversions/tick
		aBoiler.mEnergy = 300000;
	}

	// ---------------------------------------------------------------------------
	// 1. the conversion truth table (:116-126)
	// ---------------------------------------------------------------------------

	@Test
	public void conversionFollowsTheTripleMin() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9); // rng(10) != 0 — never scales
		tBoiler.setOutput(64);
		fillWater(tBoiler, 5);
		tBoiler.mEnergy = 800; // 800 / 80 = 10 conversions affordable
		// water 5 < energy 10 < capacity/2560 250 → tConversions = 5
		tBoiler.onTick(1, true);
		assertEquals(0, tBoiler.mTanks[0].amount(), "all 5 water converted");
		assertEquals(800, tBoiler.mTanks[1].amount(), "5 conversions × 160 L at eff 10000");
		assertEquals(400, tBoiler.mEnergy, "5 × 80 HU consumed");
	}

	@Test
	public void conversionIsCapacityLimitedWhenEnergyAndWaterArePlentiful() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.setOutput(64); // capacity 640000 → the 2560-lattice bound is 250
		fillWater(tBoiler, 4000);
		tBoiler.mEnergy = 100000; // 1250 conversions affordable
		tBoiler.onTick(1, true);
		assertEquals(250, tBoiler.mTanks[1].amount() / GTFluids.STEAM_PER_WATER_GLOBAL,
				"250 conversions = the capacity/2560 bound");
		assertEquals(4000 - 250, tBoiler.mTanks[0].amount(), "only 250 water consumed");
		assertEquals(100000 - 250 * GTFluids.EU_PER_WATER, tBoiler.mEnergy);
	}

	@Test
	public void conversionIsEnergyLimitedBelowOneWholeBatch() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		fillWater(tBoiler, 4000);
		tBoiler.mEnergy = 79; // under one conversion's 80 HU
		tBoiler.onTick(1, true);
		assertEquals(0, tBoiler.mTanks[1].amount(), "no conversion without a whole 80 HU");
		assertEquals(4000, tBoiler.mTanks[0].amount(), "the water stays");
	}

	@Test
	public void halvedEfficiencyHalvesTheSteamYield() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.mEfficiency = 5000; // the calcified floor
		fillWater(tBoiler, 1);
		tBoiler.mEnergy = 80;
		tBoiler.onTick(1, true);
		// units(1, 10000, 5000*160=800000) = 80 L — half the 160
		assertEquals(80, tBoiler.mTanks[1].amount(), "1 water × 160 × 5000/10000 = 80 L");
	}

	@Test
	public void scalingDecrementsWithTheFloorAndDistilledWaterIsImmune() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 0); // every tick rolls a scale hit
		fillWater(tBoiler, 4000);
		tBoiler.mEnergy = 80000; // 1000 conversions affordable, cap bound 250 → 250/tick
		tBoiler.onTick(1, true);
		assertEquals(10000 - 250, tBoiler.mEfficiency, "one scale hit: eff -= tConversions");
		tBoiler.mEnergy = 80000;
		fillWater(tBoiler, 4000);
		tBoiler.onTick(2, true);
		assertEquals(10000 - 500, tBoiler.mEfficiency, "the second hit");
		// the distilled-water immunity: the same rng, no decrement
		fillDistw(tBoiler, 4000);
		tBoiler.mEnergy = 80000;
		tBoiler.onTick(3, true);
		assertEquals(9500, tBoiler.mEfficiency, "distilled water never scales");
		// the 5000 floor: burn the efficiency to the clamp
		tBoiler.mEfficiency = 5010;
		fillWater(tBoiler, 4000);
		tBoiler.mEnergy = 80000;
		tBoiler.onTick(4, true);
		assertEquals(5000, tBoiler.mEfficiency, "the :121 clamp — 5010 - 250 floors at 5000");
		assertTrue(tBoiler.mEfficiency >= 5000);
	}

	// ---------------------------------------------------------------------------
	// 2. the cooldown drain (:129-137)
	// ---------------------------------------------------------------------------

	@Test
	public void theCooldownGateHoldsForTheFull128TicksThenDrains() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.setOutput(64);
		tBoiler.mTanks[1].add(20000, new FluidStack(Fluids.LAVA, 20000));
		tBoiler.mEnergy = 100000;
		tBoiler.mCoolDownResetTimer = 1;
		tBoiler.onTick(1, true); // timer 1 → post-decrement 0? no: evaluates 1, becomes 0, no drain
		assertEquals(100000, tBoiler.mEnergy, "the gate holds at the last tick before zero");
		tBoiler.onTick(2, true); // evaluates 0 → the drain branch
		assertEquals(100000 - (64 * 64) / GTFluids.STEAM_PER_EU, tBoiler.mEnergy,
				"the :131 drain (mOutput * 64) / STEAM_PER_EU = 2048");
		assertEquals(20000 - 64 * 64, tBoiler.mTanks[1].amount(), "the :132 steam trash = mOutput * 64");
	}

	@Test
	public void theCooldownReArmsAt128WhenTheStoreEmpties() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.setOutput(64);
		tBoiler.mTanks[1].add(100, new FluidStack(Fluids.LAVA, 100)); // little steam, trashed whole
		tBoiler.mEnergy = 1000;
		tBoiler.mCoolDownResetTimer = 0;
		tBoiler.onTick(1, true);
		assertEquals(0, tBoiler.mEnergy, "the :131 drain sinks the store to ≤0 → the :134 clamp to 0");
		assertEquals(0, tBoiler.mTanks[1].amount(), "the 100 L trashed whole");
		assertEquals(128, tBoiler.mCoolDownResetTimer, "the :135 re-arm");
	}

	@Test
	public void aConversionRefreshesTheTimer() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		fillWater(tBoiler, 1);
		tBoiler.mEnergy = 80;
		tBoiler.mCoolDownResetTimer = 0;
		tBoiler.onTick(1, true);
		assertEquals(127, tBoiler.mCoolDownResetTimer,
				"the :125 refresh set 128 and the :129 post-decrement took it to 127 (the upstream order verbatim)");
	}

	// ---------------------------------------------------------------------------
	// 3. the push gate (:139-142)
	// ---------------------------------------------------------------------------

	@Test
	public void theHalfGateShutAndTheRateLadder() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.setOutput(32);
		CountingSink tSink = new CountingSink();
		tBoiler.setFluidAdjacencyOverride(aSide -> GTBoilerTankBlockEntity.SIDE_UP == aSide ? tSink : null);
		// exactly HALF: tAmount = cap/2 - cap/2 = 0 → nothing leaves
		tBoiler.mTanks[1].add(160000, new FluidStack(Fluids.LAVA, 160000));
		tBoiler.mEnergy = 0;
		tBoiler.onTick(1, true);
		assertEquals(0, tSink.pushed, "half-full pushes nothing (the strict >)");
		// above half but under 3/4: rate = mOutput — the push is capped by the SURPLUS
		// above half (min(rate, tAmount)), so a 33 L surplus pushes 32
		tBoiler.mTanks[1].add(33, new FluidStack(Fluids.LAVA, 33));
		tBoiler.onTick(2, true);
		assertEquals(32, tSink.pushed, "the low rate = mOutput, capped by the surplus");
		// above 3/4: rate = mOutput * 2
		tBoiler.mTanks[1].add(160000, new FluidStack(Fluids.LAVA, 160000)); // ~320001 → clamped at capacity
		tBoiler.onTick(3, true);
		assertEquals(32 + 64, tSink.pushed, "the 3/4 double rate = mOutput * 2");
	}

	@Test
	public void theTopFaceIsTheOnlyDoor() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.setOutput(32);
		tBoiler.mTanks[1].add(200000, new FluidStack(Fluids.LAVA, 200000));
		tBoiler.mEnergy = 0;
		final int[] tSideProbe = {0};
		CountingSink tSink = new CountingSink();
		tBoiler.setFluidAdjacencyOverride(aSide -> {
			tSideProbe[0] |= (1 << aSide);
			return GTBoilerTankBlockEntity.SIDE_UP == aSide ? tSink : null;
		});
		tBoiler.onTick(1, true);
		assertEquals(1 << GTBoilerTankBlockEntity.SIDE_UP, tSideProbe[0], "only the UP side is ever consulted");
		assertEquals(32, tSink.pushed);
	}

	@Test
	public void theBarometerFollowsTheTankFill() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.setOutput(32);
		tBoiler.setFluidAdjacencyOverride(aSide -> null); // no sink — the tank keeps its fill
		tBoiler.mTanks[1].add(200000, new FluidStack(Fluids.LAVA, 200000));
		tBoiler.mEnergy = 0;
		tBoiler.onTick(1, true);
		assertEquals(19, tBoiler.mBarometer, "scale(200000, 320000, 31, F) = 19");
		assertTrue(tBoiler.onTickCheck(1), "the changed barometer flags the sync");
		tBoiler.onTickResetChecks(1, true);
		assertFalse(tBoiler.onTickCheck(2), "the oBarometer pair resets the flag");
	}

	// ---------------------------------------------------------------------------
	// 4. the explosion trigger table + the :215 strength
	// ---------------------------------------------------------------------------

	@Test
	public void overheatAndTankFullArmTheDeferredExplosion() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 9);
		tBoiler.setOutput(32);
		tBoiler.setFluidAdjacencyOverride(aSide -> null);
		tBoiler.mTanks[1].add(1000, new FluidStack(Fluids.LAVA, 1000));
		tBoiler.mEnergy = tBoiler.mCapacity + 1; // the :148 mEnergy > mCapacity trigger
		tBoiler.onTick(1, true);
		assertEquals(1.0F, tBoiler.mExplosionStrength,
				"the deferred explode(F) armed with the :215 strength max(1, sqrt(1000)/100=0.316 → 1)");
		// the tank-full trigger on a fresh boiler
		FixtureBoiler tFull = boiler();
		tFull.setRngOverride(() -> 9);
		tFull.setOutput(32);
		tFull.setFluidAdjacencyOverride(aSide -> null);
		tFull.mTanks[1].add(320000, new FluidStack(Fluids.LAVA, 320000)); // isFull at capacity 320000
		tFull.mEnergy = 0;
		tFull.onTick(1, true);
		assertEquals(5.6568542F, tFull.mExplosionStrength, "sqrt(320000)/100 = 5.657… the :215 formula verbatim");
	}

	@Test
	public void theStrengthFormulaIsTheSteamVolumeSqrt() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setOutput(32);
		tBoiler.mTanks[1].add(250000, new FluidStack(Fluids.LAVA, 250000));
		tBoiler.mBarometer = 5;
		assertTrue(tBoiler.dismantle(null)); // the :202-205 pressurised dismantle (null player = the RCON door)
		assertEquals(5.0F, tBoiler.mExplosionStrength, "sqrt(250000)/100 = 5.0");
		// the empty tank keeps the floor of 1
		FixtureBoiler tEmpty = boiler();
		tEmpty.mBarometer = 5;
		assertTrue(tEmpty.dismantle(null));
		assertEquals(1.0F, tEmpty.mExplosionStrength, "max(1, 0) = 1");
		// sub-4 pressure never detonates
		FixtureBoiler tQuiet = boiler();
		tQuiet.mBarometer = 4;
		assertFalse(tQuiet.dismantle(null), "the strict > 4 gate");
		assertEquals(0.0F, tQuiet.mExplosionStrength);
	}

	@Test
	public void theCaughtInExplosionSecondBlastFollowsTheBarometer() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setOutput(32);
		tBoiler.mTanks[1].add(40000, new FluidStack(Fluids.LAVA, 40000));
		tBoiler.mBarometer = 5;
		tBoiler.onExploded(); // :208-211
		assertEquals(2.0F, tBoiler.mExplosionStrength, "sqrt(40000)/100 = 2.0");
		FixtureBoiler tQuiet = boiler();
		tQuiet.mBarometer = 3;
		tQuiet.onExploded();
		assertEquals(0.0F, tQuiet.mExplosionStrength, "barometer ≤ 4: no second blast");
	}

	@Test
	public void theChiselDetonatesAboveFifteenAndRepairsBelow() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setOutput(32);
		tBoiler.setRngOverride(() -> 9);
		tBoiler.setFluidAdjacencyOverride(aSide -> null);
		tBoiler.mEfficiency = 9000;
		tBoiler.mTanks[1].add(200000, new FluidStack(Fluids.LAVA, 200000));
		tBoiler.mEnergy = 500;
		tBoiler.mBarometer = 16; // above the :168 gate
		assertEquals(0, tBoiler.chisel(null), "the detonation branch returns 0");
		assertTrue(tBoiler.mExplosionStrength > 0, "explode(F) armed");
		assertEquals(9000, tBoiler.mEfficiency, "the tank is NOT cleared on the detonation branch");
		// the repair branch
		FixtureBoiler tScale = boiler();
		tScale.setOutput(32);
		tScale.setRngOverride(() -> 9);
		tScale.mEfficiency = 9000;
		tScale.mTanks[1].add(200000, new FluidStack(Fluids.LAVA, 200000));
		tScale.mEnergy = 500;
		tScale.mBarometer = 10;
		assertEquals(1000, tScale.chisel(null), "the :175 return = 10000 - mEfficiency");
		assertEquals(10000, tScale.mEfficiency, "the :173 reset");
		assertEquals(0, tScale.mEnergy, "the :174 reset");
		assertEquals(0, tScale.mTanks[1].amount(), "the :172 vent");
		assertEquals(0.0F, tScale.mExplosionStrength, "no explosion on the repair branch");
		// nothing to repair
		FixtureBoiler tClean = boiler();
		tClean.mEfficiency = 10000;
		tClean.mBarometer = 10;
		assertEquals(0, tClean.chisel(null), "rResult 0 = nothing to decalcify");
	}

	// ---------------------------------------------------------------------------
	// 5. the energy face (:249-260)
	// ---------------------------------------------------------------------------

	@Test
	public void theEnergyFaceIsHuOnlyReceiveOnly() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setOutput(32);
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertTrue(tBoiler.isEnergyAcceptingFrom(TD.Energy.HU, tSide, false), "HU accepted from side " + tSide);
			assertTrue(tBoiler.isEnergyAcceptingFrom(TD.Energy.HU, tSide, true), "the theoretical probe agrees on side " + tSide);
			assertFalse(tBoiler.isEnergyAcceptingFrom(TD.Energy.KU, tSide, false), "KU refused on side " + tSide);
			assertFalse(tBoiler.isEnergyEmittingTo(TD.Energy.HU, tSide, false), "never emitting on side " + tSide);
		}
		assertEquals(16, tBoiler.getEnergyDemanded(TD.Energy.HU, (byte)0, 1), "the :253 demand = mOutput / 2");
		assertEquals(16, tBoiler.getEnergySizeInputRecommended(TD.Energy.HU, (byte)0), "the :254 recommended");
		assertEquals(1, tBoiler.getEnergySizeInputMin(TD.Energy.HU, (byte)0), "the :255 min");
		assertEquals(Long.MAX_VALUE, tBoiler.getEnergySizeInputMax(TD.Energy.HU, (byte)0), "the :256 max");
		assertEquals(List.of(TD.Energy.HU), tBoiler.getEnergyTypes((byte)0), "the :259 type list");
	}

	@Test
	public void theInjectionDoorBooksAndReArmsTheCooldown() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setOutput(32);
		tBoiler.mCoolDownResetTimer = 10;
		long tBooked = tBoiler.doEnergyInjection(TD.Energy.HU, (byte)3, 1, 100, true);
		assertEquals(100, tBooked, "the whole packet booked");
		assertEquals(100, tBoiler.mEnergy, "mEnergy += abs(amount * size)");
		assertEquals(32, tBoiler.mCoolDownResetTimer, "the :252 re-arm max(,32)");
		tBoiler.mCoolDownResetTimer = 128;
		tBoiler.doEnergyInjection(TD.Energy.HU, (byte)3, 1, 100, true);
		assertEquals(128, tBoiler.mCoolDownResetTimer, "the max keeps 128");
		// the simulate half books nothing
		long tSimulated = tBoiler.doEnergyInjection(TD.Energy.HU, (byte)3, 1, 50, false);
		assertEquals(50, tSimulated, "the probe echoes the amount");
		assertEquals(200, tBoiler.mEnergy, "the simulate booked nothing extra");
		// the wrong type is refused by the Root gate
		assertEquals(0, tBoiler.doEnergyInjection(TD.Energy.KU, (byte)3, 1, 100, true), "KU refused");
		assertEquals(200, tBoiler.mEnergy);
	}

	// ---------------------------------------------------------------------------
	// 6. the tool faces
	// ---------------------------------------------------------------------------

	@Test
	public void thePlungerClearsWaterFirstThenSteam() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setOutput(32);
		fillWater(tBoiler, 1000);
		tBoiler.mTanks[1].add(2000, new FluidStack(Fluids.LAVA, 2000));
		assertEquals(1000, tBoiler.plunger(), "the water tank first");
		assertEquals(0, tBoiler.mTanks[0].amount());
		assertEquals(2000, tBoiler.plunger(), "then the steam tank");
		assertEquals(0, tBoiler.mTanks[1].amount());
	}

	@Test
	public void theThermometerAndMagnifierReadouts() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setOutput(32);
		tBoiler.mEnergy = 1234;
		assertEquals("Stored Heat Units: 1234 / " + tBoiler.mCapacity + " HU", tBoiler.thermometer());
		assertTrue(tBoiler.magnifyingglass().get(0).contains("No Calcification"), "the clean readout");
		assertTrue(tBoiler.magnifyingglass().get(1).contains("WARNING: NO WATER!!!"), "the dry warning");
		fillWater(tBoiler, 500);
		tBoiler.mEfficiency = 9850;
		assertTrue(tBoiler.magnifyingglass().get(0).contains("Calcification: 1%"), "10000-9850 = 150 → LH.percent 1%");
		assertFalse(tBoiler.magnifyingglass().get(1).contains("WARNING"), "water present");
	}

	// ---------------------------------------------------------------------------
	// 7. the water-in face (:236-238 the funnel half; the capability door face half is
	//    live-only — ForgeCapabilities does not resolve offline, the RCON fill arm covers it)
	// ---------------------------------------------------------------------------

	@Test
	public void theFunnelFaceTakesWaterOnly() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setOutput(32);
		assertEquals(1000, tBoiler.funnelFill((byte)0, new FluidStack(Fluids.WATER, 1000), true), "water pours in");
		assertEquals(1000, tBoiler.mTanks[0].amount());
		assertEquals(0, tBoiler.funnelFill((byte)0, new FluidStack(Fluids.LAVA, 100), true), "lava refused, NOT destroyed");
		assertEquals(1000, tBoiler.mTanks[0].amount(), "the refusal left the tank intact");
		// the probe half (aDoFill=false → SIMULATE)
		assertEquals(3000, tBoiler.funnelFill((byte)2, new FluidStack(Fluids.WATER, 3000), false), "the probe answers the space");
		assertEquals(1000, tBoiler.mTanks[0].amount(), "the probe booked nothing");
		// the funnel ignores the side (upstream :236 the aSide param is unused)
		assertEquals(3000, tBoiler.funnelFill(GTBoilerTankBlockEntity.SIDE_UP, new FluidStack(Fluids.WATER, 3000), true),
				"any side pours (3000 = the remaining space)");
	}

	@Test
	public void theRowCarrierDrivesTheOutputAndCapacity() {
		FixtureBoiler tBoiler = boiler();
		tBoiler.setOutput(32);
		assertEquals(320000, tBoiler.mTanks[1].capacity(), "the :78 capacity = mOutput * 10000");
		assertEquals(4000, tBoiler.mTanks[0].capacity(), "the water tank is the fixed 4000");
	}

	// ---------------------------------------------------------------------------
	// 8. the 26-row table (Loader:553-579 zero diff)
	// ---------------------------------------------------------------------------

	/** One expected row: path, meta id, display name, NBT_OUTPUT_SU, hardness (== resistance). */
	private record Row(String path, int metaId, String display, long output, float hardness) {}

	@Test
	public void the26RowTableMatchesTheLoaderVerbatim() {
		List<Row> tExpected = List.of(
				// :553-565 — "Steam Boiler Tank (X)", ids 1200-1212 in the upstream LINE order
				new Row("steam_boiler_tank_lead"            , 1200, "Steam Boiler Tank (Lead)"            ,  32,  4.0F),
				new Row("steam_boiler_tank_bismuth"         , 1201, "Steam Boiler Tank (Bismuth)"         ,  40,  4.0F),
				new Row("steam_boiler_tank_bronze"          , 1202, "Steam Boiler Tank (Bronze)"          ,  48,  7.0F),
				new Row("steam_boiler_tank_arsenic_copper"  , 1210, "Steam Boiler Tank (Arsenic Copper)"  ,  48,  7.0F),
				new Row("steam_boiler_tank_arsenic_bronze"  , 1211, "Steam Boiler Tank (Arsenic Bronze)"  ,  56,  7.0F),
				new Row("steam_boiler_tank_invar"           , 1203, "Steam Boiler Tank (Invar)"           ,  32,  4.0F),
				new Row("steam_boiler_tank_steel"           , 1204, "Steam Boiler Tank (Steel)"           ,  64,  6.0F),
				new Row("steam_boiler_tank_chromium"        , 1205, "Steam Boiler Tank (Chromium)"        , 192,  4.0F),
				new Row("steam_boiler_tank_titanium"        , 1206, "Steam Boiler Tank (Titanium)"        , 224,  9.0F),
				new Row("steam_boiler_tank_netherite"       , 1209, "Steam Boiler Tank (Netherite)"       , 224,  9.0F),
				new Row("steam_boiler_tank_tungsten"        , 1207, "Steam Boiler Tank (Tungsten)"        , 256, 10.0F),
				new Row("steam_boiler_tank_tungstensteel"   , 1208, "Steam Boiler Tank (Tungstensteel)"   , 256, 12.5F),
				new Row("steam_boiler_tank_ultimet"         , 1212, "Steam Boiler Tank (Ultimet)"         , 512, 12.5F),
				// :567-579 — "Strong Steam Boiler Tank (X)", ids 1250-1262 in the upstream LINE order
				new Row("strong_steam_boiler_tank_lead"            , 1250, "Strong Steam Boiler Tank (Lead)"            , 128,  4.0F),
				new Row("strong_steam_boiler_tank_bismuth"         , 1251, "Strong Steam Boiler Tank (Bismuth)"         , 160,  4.0F),
				new Row("strong_steam_boiler_tank_bronze"          , 1252, "Strong Steam Boiler Tank (Bronze)"          , 192,  7.0F),
				new Row("strong_steam_boiler_tank_arsenic_copper"  , 1260, "Strong Steam Boiler Tank (Arsenic Copper)"  , 192,  7.0F),
				new Row("strong_steam_boiler_tank_arsenic_bronze"  , 1261, "Strong Steam Boiler Tank (Arsenic Bronze)"  , 224,  7.0F),
				new Row("strong_steam_boiler_tank_invar"           , 1253, "Strong Steam Boiler Tank (Invar)"           , 128,  4.0F),
				new Row("strong_steam_boiler_tank_steel"           , 1254, "Strong Steam Boiler Tank (Steel)"           , 256,  6.0F),
				new Row("strong_steam_boiler_tank_chromium"        , 1255, "Strong Steam Boiler Tank (Chromium)"        , 768,  4.0F),
				new Row("strong_steam_boiler_tank_titanium"        , 1256, "Strong Steam Boiler Tank (Titanium)"        , 896,  9.0F),
				new Row("strong_steam_boiler_tank_netherite"       , 1259, "Strong Steam Boiler Tank (Netherite)"       , 896,  9.0F),
				new Row("strong_steam_boiler_tank_tungsten"        , 1257, "Strong Steam Boiler Tank (Tungsten)"        ,1024, 10.0F),
				new Row("strong_steam_boiler_tank_tungstensteel"   , 1258, "Strong Steam Boiler Tank (Tungstensteel)"   ,1024, 12.5F),
				new Row("strong_steam_boiler_tank_ultimet"         , 1262, "Strong Steam Boiler Tank (Ultimet)"         ,2048, 12.5F));

		List<GT6Boilers.BoilerRow> tRows = GT6Boilers.allRows();
		assertEquals(26, tRows.size(), "13 + 13 — the Loader :552-580 section is dense, no gaps, no commented rows");
		for (int i = 0; i < tExpected.size(); i++) {
			Row tWant = tExpected.get(i);
			GT6Boilers.BoilerRow tGot = tRows.get(i);
			assertEquals(tWant.path(), tGot.path(), "row " + i + " path");
			assertEquals(tWant.metaId(), tGot.metaId(), "row " + i + " meta id (the upstream line order kept)");
			// the composed face (task p20-i18n-compose-rows): the fixture display replays from
			// the template + the material word — a per-row expansion pin (all 26 rows)
			String tReplay = (tGot.strong() ? "Strong Steam Boiler Tank (%s)" : "Steam Boiler Tank (%s)")
					.replace("%s", tGot.material().display());
			assertEquals(tWant.display(), tReplay, "row " + i + " composed display replay");
			assertEquals(tWant.output(), tGot.outputSteamPerTick(), "row " + i + " NBT_OUTPUT_SU (= raw × STEAM_PER_EU 2)");
			assertEquals(tWant.hardness(), tGot.material().hardness(), "row " + i + " hardness");
			assertEquals(tWant.hardness(), tGot.material().hardness(), "row " + i + " resistance == hardness");
		}
		// the ids cover exactly the two upstream ranges
		for (GT6Boilers.BoilerRow tRow : tRows) {
			boolean tStandard = tRow.metaId() >= 1200 && tRow.metaId() <= 1212;
			boolean tStrong = tRow.metaId() >= 1250 && tRow.metaId() <= 1262;
			assertTrue(tStandard != tStrong, "each id belongs to exactly one ladder: " + tRow.metaId());
			assertEquals(tStrong, tRow.strong());
		}
		// every row carries its own path key in the registration map (the .get() resolution
		// is live-only — the DeferredRegister never fires offline)
		assertEquals(tRows.size(), GT6Boilers.BLOCKS_BY_PATH.size());
		for (GT6Boilers.BoilerRow tRow : tRows) {
			assertTrue(GT6Boilers.BLOCKS_BY_PATH.containsKey(tRow.path()), "the map key: " + tRow.path());
		}
	}

	// ---------------------------------------------------------------------------
	// 9. the distilled-water immunity canon (task p14-boiler-distw-immunity) — the
	//    :119 criterion `rng(10) == 0 && mEfficiency > 5000 && has() && !FL.distw(...)`
	//    short-circuits on the LAST conjunct: a distilled tank makes the whole scaling
	//    branch rng-INDEPENDENT (deterministic immunity), while the family mates
	//    SpDew/MnWtr (FL.java:113/:119 — both carry the WATER tag, so upstream :262
	//    FL.water admits them) are NOT immune: distw() = FL.DistW.is is the SINGULAR
	//    identity (FL.java:699-703). The canonical budget is 1200 conversion ticks with
	//    every rng arm — mRngOverride 0 (a hit every tick), 1 (never a hit) and a
	//    fixed-seed random (arbitrary rolls, deterministic CI).
	// ---------------------------------------------------------------------------

	/** The offline SpDew stand-in (any non-DistW identity; vanilla has five fluids). */
	private static final Fluid SPDEW = Fluids.FLOWING_LAVA;
	/** The offline MnWtr stand-in — disjoint from the DistW stand-in FLOWING_WATER. */
	private static final Fluid MNWTR = Fluids.LAVA;

	@Test
	public void theDistwImmuneArmsHold10000AcrossEveryRngArmOver1200ConversionTicks() {
		// arm 1 — mRngOverride → 0: every tick WOULD be a scale hit for a water tank
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 0);
		primeSustainedRun(tBoiler);
		fillDistw(tBoiler, 4000);
		long tYield = runConversionTicks(tBoiler, 1200);
		assertEquals(10000, tBoiler.mEfficiency, "rng 0 arm: the distilled criterion short-circuits, efficiency untouched");
		assertEquals(576000, tYield, "1200 ticks × 3 conversions × 160 L — the yield NEVER degraded");
		assertEquals(400, tBoiler.mTanks[0].amount(), "3600 of 4000 L converted (3/t × 1200t) — every tick converted");
		assertEquals(12000, tBoiler.mEnergy, "3600 × 80 HU consumed — the exact :124 accounting");

		// arm 2 — mRngOverride → 1: rng(10) never 0 — trivially no scaling either
		FixtureBoiler tNoHit = boiler();
		tNoHit.setRngOverride(() -> 1);
		primeSustainedRun(tNoHit);
		fillDistw(tNoHit, 4000);
		runConversionTicks(tNoHit, 1200);
		assertEquals(10000, tNoHit.mEfficiency, "rng 1 arm: efficiency untouched");

		// arm 3 — the random arm: a fixed-seed Random rolls arbitrary values (deterministic CI)
		java.util.Random tRandom = new java.util.Random(20260901L);
		FixtureBoiler tRolled = boiler();
		tRolled.setRngOverride(() -> tRandom.nextInt(10));
		primeSustainedRun(tRolled);
		fillDistw(tRolled, 4000);
		runConversionTicks(tRolled, 1200);
		assertEquals(10000, tRolled.mEfficiency, "random arm: whatever the rolls, distw is immune — the rng-INDEPENDENCE");
	}

	@Test
	public void plainWaterScalesOver1200ConversionTicks() {
		// the control: the identical budget with plain water and the every-tick-hit arm
		FixtureBoiler tBoiler = boiler();
		tBoiler.setRngOverride(() -> 0);
		primeSustainedRun(tBoiler);
		fillWater(tBoiler, 4000);
		runConversionTicks(tBoiler, 1200);
		assertEquals(6400, tBoiler.mEfficiency, "10000 - 3 conversions × 1200 ticks — the sustained :120 decrement");
		assertTrue(tBoiler.mEfficiency < 10000, "plain water is NOT immune");
		assertTrue(tBoiler.mEfficiency >= 5000, "the :121 floor holds");
	}

	@Test
	public void spDewAndMnWtrAreNotImmuneTheCriterionIsTheDistwSingularity() {
		// the seam truth table — FL.java:699-703: distw() is FL.DistW.is, the SINGULAR
		// identity; the family mates are NOT it (upstream they even pass the :262 FL.water
		// intake — the WATER tag at FL.java:113/:119 — so they reach the tank and scale)
		FixtureBoiler tSeamProbe = boiler();
		assertTrue(tSeamProbe.mDistwMatch.apply(Fluids.FLOWING_WATER), "the DistW stand-in matches");
		assertFalse(tSeamProbe.mDistwMatch.apply(SPDEW), "SpDew is NOT distw");
		assertFalse(tSeamProbe.mDistwMatch.apply(MNWTR), "MnWtr is NOT distw");

		// the SpDew control: same 1200-tick budget, same every-tick-hit arm — it scales
		FixtureBoiler tSpDew = boiler();
		tSpDew.setRngOverride(() -> 0);
		primeSustainedRun(tSpDew);
		fillStandin(tSpDew, SPDEW, 4000);
		runConversionTicks(tSpDew, 1200);
		assertEquals(6400, tSpDew.mEfficiency, "spectral_dew is water-family but NOT immune");
		assertTrue(tSpDew.mEfficiency < 10000);

		// the MnWtr control: ditto
		FixtureBoiler tMnWtr = boiler();
		tMnWtr.setRngOverride(() -> 0);
		primeSustainedRun(tMnWtr);
		fillStandin(tMnWtr, MNWTR, 4000);
		runConversionTicks(tMnWtr, 1200);
		assertEquals(6400, tMnWtr.mEfficiency, "mineral water is water-family but NOT immune");
		assertTrue(tMnWtr.mEfficiency < 10000);
	}
}
