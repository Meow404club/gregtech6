package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GT6Kinetics;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GTSteamEngineBlockEntity offline tests (task p12-engine-steam acceptance a): the
 * whole-tank steam-to-KU conversion truth table (STEAM_PER_WATER 200 / STEAM_PER_EU 2 /
 * the ten-thousandths efficiency), the AC square wave (2-bit piston phase, the +/+/-/-
 * sign sequence, the hotter-engine period), the heat-state ramp (scale table, the :138
 * output ratio, the :141 activation gate), the overheat stop + tank vent (:152-161), the
 * no-receiver dissipation (:147), the distilled-water byproduct rate and side ring
 * (:125-130), the back-face steam feed gates (the :239 projection), the NBT round trip
 * WITH the persisted piston phase (the card's fidelity point over the P11 rig), and the
 * 28-row family census.
 *
 * <p>Offline-harness notes (the crank test record carries over): the level-less fixture
 * runs the SERVER branch; {@code updateEntity()} is driven by hand (aTimer counts
 * 0,1,2,...); the SYNC_SECOND fold recomputes {@code mState} at {@code aTimer % 20 == 0}
 * — so the phase/wave tests pin {@code mState} BETWEEN ticks and stay inside the
 * 1..19 window; the gt6 steam/distilled-water fluids do not exist in the offline registry,
 * so the fluid identity seams take vanilla WATER as the stand-in
 * ({@code setFluidSeams}, the declared test seam — the production lambdas read
 * {@link GTFluids#STEAM}/{@link GTFluids#DISTILLED_WATER}).
 */
public class GTSteamEngineBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GTSteamEngineBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GTSteamEngineBlockEntity>[] tHolder = (BlockEntityType<GTSteamEngineBlockEntity>[]) new BlockEntityType<?>[1];
		// OAK_STAIRS joins the valid set for the facingSyncFromState fixture (the vanilla
		// stairs state carries the SAME HORIZONTAL_FACING property instance); 21.1 validates
		// the type/state pair at the BE ctor (task p15-m4-test-infra-2).
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTSteamEngineBlockEntity(tHolder[0], aPos, aState), Blocks.STONE, Blocks.OAK_STAIRS).build(null);
		sType = tHolder[0];
	}

	/** A fresh level-less engine fixture (the STONE state leaves the class defaults). */
	private static GTSteamEngineBlockEntity engine() {
		return new GTSteamEngineBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	/** A fresh fixture pinned to the Lead Steam Engine row (3000 / 16000 / 8 KU). */
	private static GTSteamEngineBlockEntity leadEngine() {
		GTSteamEngineBlockEntity tEngine = engine();
		tEngine.mEfficiency = 3000;
		tEngine.mCapacity = 16000;
		tEngine.setOutput(8);
		return tEngine;
	}

	/** The WATER-for-steam fluid seam swap (the offline stand-in). */
	private static void waterSeams(GTSteamEngineBlockEntity aEngine) {
		aEngine.setFluidSeams(f -> f == Fluids.WATER, amount -> new FluidStack(Fluids.WATER, amount));
	}

	/** Fills the engine tank through the long-add primitive. */
	private static void fillTank(GTSteamEngineBlockEntity aEngine, long aAmount) {
		assertTrue(aEngine.mTank.add(aAmount, new FluidStack(Fluids.WATER, (int)Math.min(aAmount, Integer.MAX_VALUE))) > 0);
	}

	/**
	 * Counting KU sink — the crusher-shaped fake consumer (the crank test's CountingSink
	 * with the type parameterised to KU): accepts KU from every side, refuses the rest.
	 */
	public static class CountingSink extends BlockEntity implements ITileEntityEnergy {
		public long calls = 0, lastSize = 1, lastAmount = 0;
		public byte lastSide = -1;

		// 21.1 ctor validation: the fake binds a real BET over the vanilla stone state —
		// the supplier is stored, never invoked (task p15-m4-test-infra-2).
		static final BlockEntityType<CountingSink> FAKE_TYPE =
				BlockEntityType.Builder.of((aPos, aState) -> new CountingSink(aPos), Blocks.STONE).build(null);

		public CountingSink(BlockPos aPos) {
			super(FAKE_TYPE, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == TD.Energy.KU;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == TD.Energy.KU;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.KU.AS_LIST;}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) {
					calls++;
					lastSize = aSize;
					lastAmount = aAmount;
					lastSide = aSide;
				}
				return aAmount;
			}
			return 0;
		}

		@Override public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}
		@Override public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}
		@Override public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}
		@Override public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	/** Wires the sink in as the engine's (offline) energy adjacency. */
	private static CountingSink wire(GTSteamEngineBlockEntity aEngine) {
		CountingSink tSink = new CountingSink(aEngine.getBlockPos().relative(Direction.from3DDataValue(aEngine.getFacing())));
		aEngine.setAdjacencyOverride(aSide -> {
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tSink, tOpposite);
		});
		return tSink;
	}

	// ---------------------------------------------------------------------------
	// acceptance a — the conversion truth table
	// ---------------------------------------------------------------------------

	@Test
	public void conversionTruthTable() {
		// the pure UT.Code.units form (:123): one whole 200 L batch = 100 EU-units, scaled
		// by the row efficiency in ten-thousandths → per-batch KU = efficiency / 100 (floor)
		for (GT6Kinetics.SteamEngineRow tRow : GT6Kinetics.STEAM_ENGINES) {
			long tPerBatch = GTSteamEngineBlockEntity.units(
					GTFluids.STEAM_PER_WATER / GTFluids.STEAM_PER_EU, 10000, tRow.efficiency(), false);
			assertEquals(tRow.efficiency() / 100, tPerBatch, tRow.path() + ": one 200 L batch at eff " + tRow.efficiency());
		}
		// the spot rows of the acceptance table (units floors: (100*eff)/10000)
		assertEquals(30, GTSteamEngineBlockEntity.units(100, 10000, 3000, false), "Lead 3000 → 30 KU per batch");
		assertEquals(64, GTSteamEngineBlockEntity.units(100, 10000, 6450, false), "IronWood 6450 → the 64.5 floor");
		assertEquals(0, GTSteamEngineBlockEntity.units(1, 10000, 3000, false), "a fraction of a batch converts nothing");
		// the live tick: a full Lead tank (STEAM_PER_WATER * 8 * 2 = 3200 L = 16 batches)
		// converts to 16 * 30 = 480 KU in ONE tick, the tank drains to empty
		GTSteamEngineBlockEntity tEngine = leadEngine();
		assertEquals(3200, tEngine.steamTankCapacity(), "the :80 tank formula 200 * mOutput * 2");
		fillTank(tEngine, 3200);
		tEngine.updateEntity();
		assertEquals(480, tEngine.mEnergy, "16 whole batches x 30 KU");
		assertEquals(0, tEngine.mTank.amount(), "only WHOLE batches leave the tank — 16x200 exactly");
		assertFalse(tEngine.mActive, "480 KU: tOutput=0 at state 0 fails the :141 gate — no emission yet");
		// a partial batch stays: 199 L is below one STEAM_PER_WATER unit
		GTSteamEngineBlockEntity tPartial = leadEngine();
		fillTank(tPartial, 199);
		tPartial.updateEntity();
		assertEquals(0, tPartial.mEnergy);
		assertEquals(199, tPartial.mTank.amount(), "the 199 L remainder waits for the next whole batch");
	}

	// ---------------------------------------------------------------------------
	// acceptance a — the AC square wave (2-bit piston phase)
	// ---------------------------------------------------------------------------

	@Test
	public void squareWavePhaseSigns() {
		GTSteamEngineBlockEntity tEngine = engine();
		tEngine.mOutput = 16;
		tEngine.mCapacity = 31000;
		tEngine.mEnergy = 1000;
		CountingSink tSink = wire(tEngine);

		tEngine.updateEntity(); // aTimer 1: state still 0 → the gate fails, nothing emits
		assertEquals(0, tSink.calls);

		// pin the heat state at 28 (the piston period 32-28 = 4 ticks) inside the %20 window.
		// The 03 dispatcher INCREMENTS mTimer before onTick, so aTimer runs 1,2,3,... — the
		// next recompute pulse lands at aTimer 20, and the pin tick's mActive was false (the
		// state-0 gate), so the first piston advance fires at aTimer 4.
		tEngine.mState = 28;
		StringBuilder tWave = new StringBuilder();
		for (int t = 0; t < 18; t++) {
			tEngine.updateEntity(); // aTimer 2..19 — no recompute, the pin survives
			tWave.append(tSink.lastSize > 0 ? '+' : '-');
		}
		assertEquals(18, tSink.calls, "one packet per active tick");
		// the +/+/-/- square wave: sign flips every 2 piston-steps = 2 x (32-mState) ticks.
		// piston advances at aTimer 4/8/12/16: ticks 2..7 ride phase 0 → '+', tick 4 lifts
		// phase 1 (still '+'), ticks 8..15 phases 2/3 → '-', ticks 16..19 wrapped to 0 → '+'
		assertEquals("++++++--------++++", tWave.toString(), "the 2-bit piston-phase square wave at mState 28");
		// the :146 sign form against the LIVE phase, pair-verified per tick: the advance
		// happens BEFORE the emit inside one tick, so the post-tick mPiston IS the emit phase
		tEngine.updateEntity(); // aTimer 20: the recompute pulse — burns the pin, emits nothing
		tEngine.mEnergy = 1000; // refill against the 18 x 29 the window spent
		tEngine.mState = 28; // re-pin inside the fresh %20 window
		for (int t = 0; t < 8; t++) {
			tEngine.updateEntity(); // aTimer 21..28 — no recompute
			long tExpected = tEngine.mPiston > 1 ? -29 : 29;
			assertEquals(tExpected, tSink.lastSize, "phase " + tEngine.mPiston + " → the :146 signed packet");
		}
		// the pinned-phase spot checks (each tick re-pins the phase; ticks at aTimer % 4 != 0
		// do not advance it): phase 2 → negative, phase 1 (no advance) → positive
		GTSteamEngineBlockEntity tAmp = engine();
		tAmp.mOutput = 16;
		tAmp.mCapacity = 31000;
		tAmp.mEnergy = 1000;
		CountingSink tAmpSink = wire(tAmp);
		tAmp.updateEntity(); // tick 0
		tAmp.mState = 28;
		tAmp.mPiston = 2;
		tAmp.updateEntity(); // aTimer 1 — no advance
		assertEquals(-29, tAmpSink.lastSize, "phase 2 emits -tOutput");
		assertEquals(1, tAmpSink.lastAmount, "amount 1 — one packet per tick (:146 amount)");
		tAmp.mPiston = 0;
		tAmp.updateEntity(); // aTimer 2 — 2 % 4 == 0 advances 0→1, still positive
		assertEquals(29, tAmpSink.lastSize, "phase 0/1 emits +tOutput");
	}

	@Test
	public void pistonPeriodHotterFaster() {
		// the period formula: the advance fires at aTimer % (32 - mState) == 0 — mState 30
		// flips the phase every 2 ticks (a 4-tick sign half-period), mState 0 every 32
		GTSteamEngineBlockEntity tHot = engine();
		tHot.mOutput = 16;
		tHot.mCapacity = 31000;
		tHot.mEnergy = 1000;
		CountingSink tSink = wire(tHot);
		tHot.updateEntity(); // aTimer 1: state 0, the gate fails, no emit — the pin tick's mActive stays false
		tHot.mState = 30; // pin: the advance now fires every 2 ticks (first at aTimer 4)
		StringBuilder tWave = new StringBuilder();
		for (int t = 0; t < 8; t++) {
			tHot.updateEntity(); // aTimer 2..9
			tWave.append(tSink.lastSize > 0 ? '+' : '-');
		}
		// a2/a3 ride phase 0 '+', a4 lifts phase 1 '+', a6 lifts phase 2 '-', a8 lifts
		// phase 3 — sign halves of 2 x (32-30) = 4 ticks
		assertEquals("++++----", tWave.toString(), "the hot engine's fast piston period");
	}

	// ---------------------------------------------------------------------------
	// acceptance a — the heat-state ramp
	// ---------------------------------------------------------------------------

	@Test
	public void heatRampActivationAndOutputRatio() {
		// the :138 output ratio over the full state ladder (mOutput 16)
		for (int tState = 0; tState <= 31; tState++) {
			assertEquals(16L * (tState + 1) / 16, GTSteamEngineBlockEntity.outputAt(16, (byte) tState), "state " + tState);
		}
		// the :141 activation gate — the formula verbatim (bites at mState >= 8 for whole ratios)
		for (int tState = 0; tState <= 31; tState++) {
			boolean tExpected = tState >= 8;
			assertEquals(tExpected, GTSteamEngineBlockEntity.activationGate(false, 1000, 16, (byte) tState), "state " + tState);
			assertFalse(GTSteamEngineBlockEntity.activationGate(true, 1000, 16, (byte) tState), "stopped never runs, state " + tState);
		}
		// the :135 scale table: 0 → 0, capacity → 32, a quarter tank → state 8
		assertEquals(0, GTSteamEngineBlockEntity.scale(0, 16000, 32, false));
		assertEquals(32, GTSteamEngineBlockEntity.scale(16000, 16000, 32, false));
		assertEquals(8, GTSteamEngineBlockEntity.scale(4000, 16000, 32, false), "1 + (4000*31)/16000");
		// the live fold: a quarter-full store lands on state 8 at the FIRST %20 pulse
		// (aTimer runs 1,2,3,... — the pulse fires on the 20th call), gate opens, tOutput = 9
		GTSteamEngineBlockEntity tEngine = engine();
		tEngine.mOutput = 16;
		tEngine.mCapacity = 16000;
		tEngine.mEnergy = 4000;
		CountingSink tSink = wire(tEngine);
		for (int t = 0; t < 20; t++) tEngine.updateEntity();
		assertEquals(8, tEngine.mState);
		assertTrue(tEngine.mActive, "the gate opens at state 8");
		assertEquals(1, tSink.calls, "nothing emitted before the pulse — the state-0 gate held");
		assertEquals(9, tSink.lastSize, "tOutput = 16 * (8+1) / 16");
		assertEquals(4000 - 9, tEngine.mEnergy, "the :147 deduction");
	}

	// ---------------------------------------------------------------------------
	// acceptance a — the overheat stop + the no-receiver dissipation
	// ---------------------------------------------------------------------------

	@Test
	public void overheatStopClearsTank() {
		GTSteamEngineBlockEntity tEngine = leadEngine();
		tEngine.mEnergy = 15600;
		tEngine.mState = 31; // pinned inside the %20 window
		fillTank(tEngine, 3200);
		tEngine.updateEntity();
		// 15600 + 480 (conversion) - 16 (emit at state 31) = 16064 >= 16000 → the :152 clamp
		assertTrue(tEngine.mStopped, "the :155 overheat stop");
		assertEquals(0, tEngine.mTank.amount(), "the :156 tank vent");
		assertEquals(15999 - 250, tEngine.mEnergy, "the :153 clamp AND the same-tick :164 bleed (stopped bleeds max(1, cap/64) immediately)");
		// the stopped engine refuses further steam (the :239 gate)
		fillTank(tEngine, 400);
		assertEquals(400, tEngine.mTank.amount(), "manual NBT-level fill still lands — the GATE lives in the feed/door, not the tank");
		assertTrue(tEngine.mStopped);
		// the sub-30 branch: state < 31 pins to 31 WITHOUT stopping (:158-160)
		GTSteamEngineBlockEntity tCool = leadEngine();
		tCool.mEnergy = 17000;
		tCool.mState = 15;
		tCool.updateEntity();
		assertFalse(tCool.mStopped, "no stop below state 31");
		assertEquals(31, tCool.mState, "the :159 state pin");
		// the stopped engine bleeds its store (:164) — start BELOW capacity so the :152
		// clamp does not bite first
		GTSteamEngineBlockEntity tBleed = leadEngine();
		tBleed.mStopped = true;
		tBleed.mEnergy = 15999;
		tBleed.updateEntity();
		assertEquals(15999 - 250, tBleed.mEnergy, "max(1, mCapacity/64) per tick");
	}

	@Test
	public void idleDissipation() {
		// the :147 unconditional deduction — pushing into NOTHING still spends the store
		GTSteamEngineBlockEntity tEngine = engine();
		tEngine.mOutput = 16;
		tEngine.mCapacity = 31000;
		tEngine.mEnergy = 1000;
		tEngine.mState = 28;
		tEngine.setAdjacencyOverride(aSide -> null); // no receiver at all
		tEngine.updateEntity();
		assertTrue(tEngine.mActive, "the :141 gate is receiver-blind");
		assertFalse(tEngine.mEmitsEnergy, "the emit booked nowhere");
		assertEquals(1000 - 29, tEngine.mEnergy, "tOutput leaves the store whether or not it was accepted");
		// the same through a REFUSING receiver (a full sink that declines the packet)
		GTSteamEngineBlockEntity tRefused = engine();
		tRefused.mOutput = 16;
		tRefused.mCapacity = 31000;
		tRefused.mEnergy = 1000;
		tRefused.mState = 28;
		CountingSink tSink = wire(tRefused);
		tSink.calls = 0;
		// the sink accepts — the control: accepted emits DO report mEmitsEnergy
		tRefused.updateEntity();
		assertTrue(tRefused.mEmitsEnergy, "an accepted emit latches mEmitsEnergy (:146)");
	}

	// ---------------------------------------------------------------------------
	// acceptance a — the distilled-water byproduct
	// ---------------------------------------------------------------------------

	/** A recording fluid handler for the side-ring push (the fake barrel). */
	public static class RecordingTank implements IFluidHandler {
		public long filled = 0;
		public final FluidStack offer;
		public long drained = 0;

		public RecordingTank(FluidStack aOffer) {offer = aOffer;}

		@Override public int getTanks() {return 1;}
		@Override public FluidStack getFluidInTank(int aTank) {return offer == null ? FluidStack.EMPTY : offer;}
		@Override public int getTankCapacity(int aTank) {return aTank == 0 ? 64000 : 0;}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {return true;}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			if (aAction.execute()) filled += aResource.getAmount();
			return aResource.getAmount();
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {return drain(aResource == null ? 0 : aResource.getAmount(), aAction);}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			if (offer == null || aMaxDrain <= 0) return FluidStack.EMPTY;
			int tAmount = (int)Math.min(aMaxDrain, offer.getAmount() - drained);
			if (tAmount <= 0) return FluidStack.EMPTY;
			if (aAction.execute()) drained += tAmount;
			return new FluidStack(offer.getFluid(), tAmount);
		}
	}

	@Test
	public void distilledWaterByproductRateAndSideRing() {
		GTSteamEngineBlockEntity tEngine = leadEngine();
		waterSeams(tEngine);
		// the side ring for facing NORTH (2): the CS.java:555 FACING_SIDES row {0,1,4,5};
		// front (2) and back (3) sit OUTSIDE the ring
		Map<Byte, RecordingTank> tSides = new HashMap<>();
		for (byte tSide : GTSteamEngineBlockEntity.facingSideRing(tEngine.mFacing)) {
			tSides.put(Byte.valueOf(tSide), new RecordingTank(null));
		}
		RecordingTank tFront = new RecordingTank(null);
		tSides.put((byte) 2, tFront); // the FRONT face — outside the ring, must stay dry
		tEngine.mFluidAdjacency = tSides::get;
		fillTank(tEngine, 400); // 2 whole batches → 2 L of byproduct
		tEngine.updateEntity();
		// the acceptance rate: every 200 L of steam → exactly 1 byproduct unit (:125)
		long tRingTotal = 0;
		for (Map.Entry<Byte, RecordingTank> tEntry : tSides.entrySet()) {
			if (tEntry.getKey() != 2) tRingTotal += tEntry.getValue().filled;
			else assertEquals(0, tEntry.getValue().filled, "the front face never takes byproduct");
		}
		assertEquals(2, tRingTotal, "400 L steam = 2 batches = 2 byproduct units to the SIDE ring");
		assertEquals(0, tEngine.mTank.amount(), "exactly 400 L left with the batches");
		// the ring geometry itself (CS.java:555)
		assertEquals("[0, 1, 4, 5]", ringString((byte) 2));
		assertEquals("[0, 1, 4, 5]", ringString((byte) 3));
		assertEquals("[2, 3, 4, 5]", ringString((byte) 0));
		assertEquals("[0, 1, 2, 3]", ringString((byte) 4));
	}

	private static String ringString(byte aFacing) {
		StringBuilder r = new StringBuilder("[");
		for (byte tSide : GTSteamEngineBlockEntity.facingSideRing(aFacing)) r.append((int) tSide).append(", ");
		return r.substring(0, r.length() - 2) + "]";
	}

	// ---------------------------------------------------------------------------
	// acceptance a — the back-face steam feed gates
	// ---------------------------------------------------------------------------

	@Test
	public void steamFeedBackFaceGates() {
		// the feed gate: steam only, back face only, not stopped, only what fits
		GTSteamEngineBlockEntity tEngine = leadEngine();
		waterSeams(tEngine);
		RecordingTank tBack = new RecordingTank(new FluidStack(Fluids.WATER, 16000));
		Map<Byte, RecordingTank> tFront = new HashMap<>();
		tFront.put((byte) 2, new RecordingTank(new FluidStack(Fluids.WATER, 16000))); // the FRONT impostor
		tEngine.mFluidAdjacency = aSide -> aSide == tEngine.backSide() ? tBack : tFront.get(aSide);
		tEngine.updateEntity();
		assertEquals(3200, tBack.drained, "one tick pulls the whole free tank space (200*8*2)");
		assertEquals(0, tEngine.mTank.amount(), "the same tick converts the pull: 16 whole batches");
		assertEquals(480, tEngine.mEnergy, "16 batches x 30 KU (the Lead row)");
		// the stopped engine refuses the feed (the :239 mode half)
		GTSteamEngineBlockEntity tStopped = leadEngine();
		waterSeams(tStopped);
		RecordingTank tStoppedBack = new RecordingTank(new FluidStack(Fluids.WATER, 16000));
		tStopped.mFluidAdjacency = aSide -> aSide == tStopped.backSide() ? tStoppedBack : null;
		tStopped.mStopped = true;
		tStopped.updateEntity();
		assertEquals(0, tStoppedBack.drained, "the :239 !mStopped half");
		// a non-steam offer is refused (the :239 FL.Steam.is half)
		GTSteamEngineBlockEntity tWrong = leadEngine();
		RecordingTank tWrongBack = new RecordingTank(new FluidStack(Fluids.LAVA, 16000));
		tWrong.mFluidAdjacency = aSide -> aSide == tWrong.backSide() ? tWrongBack : null;
		tWrong.updateEntity();
		assertEquals(0, tWrongBack.drained, "lava is not steam — the pull refuses");
		assertEquals(0, tWrong.mTank.amount());
	}

	// ---------------------------------------------------------------------------
	// acceptance a — the NBT round trip with the PERSISTED piston phase
	// ---------------------------------------------------------------------------

	@Test
	public void pistonPhaseNbtRoundTrip() {
		GTSteamEngineBlockEntity tEngine = leadEngine();
		tEngine.mPiston = 3; // the fidelity point — the P11 rig dropped exactly this
		tEngine.mState = 17;
		tEngine.mEnergy = 1234;
		tEngine.mStopped = true;
		tEngine.mActive = true;
		tEngine.mEmitsEnergy = true;
		tEngine.mFacing = 4;
		fillTank(tEngine, 1000);
		CompoundTag tTag = tEngine.saveWithoutMetadata();

		GTSteamEngineBlockEntity tLoaded = leadEngine();
		tLoaded.load(tTag);
		assertEquals(3, tLoaded.mPiston, "the persisted piston phase (upstream :88 NBT_PISTON)");
		assertEquals(17, tLoaded.mState, "the :87 visual/heat byte");
		assertEquals(1234, tLoaded.mEnergy, "the :86 store");
		assertTrue(tLoaded.mStopped, "the :90 stopped latch");
		assertTrue(tLoaded.mActive, "the :89 active latch");
		assertTrue(tLoaded.mEmitsEnergy, "the :91 emitting latch");
		assertEquals(1000, tLoaded.mTank.amount(), "the :93 tank compound");
		assertEquals(3200, tLoaded.steamTankCapacity(), "the :80 capacity re-derivation");
		assertEquals("steam_engine", tTag.getString("te_name"));
		assertTrue(tTag.contains(GTSteamEngineBlockEntity.NBT_PISTON, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(GTSteamEngineBlockEntity.NBT_VISUAL, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(GTSteamEngineBlockEntity.NBT_ENERGY, Tag.TAG_ANY_NUMERIC));
		// a rogue piston byte wraps into the 2-bit domain (:72 upstream masks &3)
		CompoundTag tWide = new CompoundTag();
		tWide.putByte(GTSteamEngineBlockEntity.NBT_PISTON, (byte) 7);
		GTSteamEngineBlockEntity tMasked = leadEngine();
		tMasked.load(tWide);
		assertEquals(3, tMasked.mPiston, "7 & 3");
	}

	// ---------------------------------------------------------------------------
	// the face family + the facing mirror
	// ---------------------------------------------------------------------------

	@Test
	public void faceTruthTable() {
		GTSteamEngineBlockEntity tEngine = engine();
		tEngine.mFacing = 2;
		for (byte tSide = 0; tSide < 6; tSide++) {
			boolean tExpect = tSide == 2;
			assertEquals(tExpect, tEngine.isEnergyEmittingTo(TD.Energy.KU, tSide, true), "theoretical KU @" + tSide);
			assertEquals(tExpect, tEngine.isEnergyEmittingTo(TD.Energy.KU, tSide, false), "real KU @" + tSide);
			assertFalse(tEngine.isEnergyAcceptingFrom(TD.Energy.KU, tSide, false), "a pure source");
		}
		assertFalse(tEngine.isEnergyType(TD.Energy.EU, (byte) 2, true), "EU never emits");
		assertFalse(tEngine.isEnergyType(TD.Energy.RU, (byte) 2, true), "RU never emits");
		assertEquals(0, tEngine.doEnergyInjection(TD.Energy.KU, (byte) 2, 8, 1, true), "the injection door is shut (Root default)");
		Collection<TagData> tTypes = tEngine.getEnergyTypes((byte) 2);
		assertEquals(1, tTypes.size());
		assertTrue(tTypes.contains(TD.Energy.KU), "the :236 AS_LIST");
		// the :233 recommended band over a row
		GTSteamEngineBlockEntity tLead = leadEngine();
		assertEquals(8, tLead.getEnergySizeOutputRecommended(TD.Energy.KU, (byte) 2), "the row output");
		assertEquals(4, tLead.getEnergySizeOutputMin(TD.Energy.KU, (byte) 2), "the Root :720 Min = Rec/2");
		assertEquals(16, tLead.getEnergySizeOutputMax(TD.Energy.KU, (byte) 2), "the Root :721 Max = Rec*2");
	}

	@Test
	public void facingSyncFromState() {
		// the /setblock gt6:steam_engine_lead[facing=west] path — the vanilla stairs state
		// carries the SAME HORIZONTAL_FACING property instance (the crank test form)
		BlockState tState = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
		GTSteamEngineBlockEntity tEngine = new GTSteamEngineBlockEntity(sType, POS, tState);
		assertEquals(2, tEngine.getFacing());
		tEngine.updateEntity();
		assertEquals(4, tEngine.getFacing(), "WEST == 4");
		assertTrue(tEngine.isEnergyEmittingTo(TD.Energy.KU, (byte) 4, false));
		assertEquals(5, tEngine.backSide(), "the steam face is the opposite (EAST == 5)");
		// a state WITHOUT the property leaves the mirror alone
		GTSteamEngineBlockEntity tPlain = engine();
		tPlain.mFacing = 0;
		tPlain.updateEntity();
		assertEquals(0, tPlain.getFacing());
	}

	// ---------------------------------------------------------------------------
	// the family census (the 28-row erratum)
	// ---------------------------------------------------------------------------

	@Test
	public void familyTableCensus() {
		// COUNT ERRATUM: the card says 26 (13+13) but metas 1300..1313 / 1350..1363 are
		// FOURTEEN each — the upstream loop :584-597 + :599-612 is 28 rows and the
		// wire-W1 no-subset ruling keeps every one of them
		assertEquals(28, GT6Kinetics.STEAM_ENGINES.size());
		Set<String> tPaths = new HashSet<>();
		for (GT6Kinetics.SteamEngineRow tRow : GT6Kinetics.STEAM_ENGINES) {
			assertTrue(tPaths.add(tRow.path()), "unique path: " + tRow.path());
			assertTrue(tRow.efficiency() >= 3000 && tRow.efficiency() <= 6450, tRow.path() + " efficiency band");
			assertTrue(tRow.outputKU() >= 8 && tRow.outputKU() <= 256, tRow.path() + " output band");
		}
		// the verbatim spot rows (Loader :584 / :591 / :597 / :599 / :612)
		GT6Kinetics.SteamEngineRow tLead = GT6Kinetics.STEAM_ENGINES.get(0);
		assertEquals("steam_engine_lead", tLead.path());
		assertEquals("Steam Engine (Lead)", tLead.displayName());
		assertEquals(3000, tLead.efficiency());
		assertEquals(16000, tLead.energyCapacity());
		assertEquals(8, tLead.outputKU());
		assertEquals(4.0F, tLead.hardness());
		GT6Kinetics.SteamEngineRow tIronWood = GT6Kinetics.STEAM_ENGINES.get(7);
		assertEquals("steam_engine_iron_wood", tIronWood.path());
		assertTrue(tIronWood.wooden(), "the aWooden sound tier");
		assertEquals(6450, tIronWood.efficiency(), "the family efficiency ceiling");
		GT6Kinetics.SteamEngineRow tLast = GT6Kinetics.STEAM_ENGINES.get(27);
		assertEquals("strong_steam_engine_tungstensteel", tLast.path());
		assertEquals("Strong Steam Engine (Tungstensteel)", tLast.displayName());
		assertEquals(6000, tLast.efficiency());
		assertEquals(512000, tLast.energyCapacity());
		assertEquals(256, tLast.outputKU(), "the family output ceiling");
		assertEquals(12.5F, tLast.hardness());
		assertEquals(14, GT6Kinetics.STEAM_ENGINES.stream().filter(r -> r.path().startsWith("steam_engine")).count());
		assertEquals(14, GT6Kinetics.STEAM_ENGINES.stream().filter(r -> r.path().startsWith("strong_steam_engine")).count());
	}
}
