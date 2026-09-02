package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GT6Kinetics;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GTGearBoxBlockEntity + GTTransformerRotationBlockEntity offline tests (task
 * p12-gearbox-transformer acceptance a): the checkGears topology truth table (the
 * corner-pair/opposite-pair/triangle/D-shape/5-6-gear rules over the mask space), the
 * multi-input lowest-speed + power-accumulation, the overspeed gear explosion (>VMAX →
 * the mask zeroes + the block-level state change; the physical drops are the RCON
 * chain's assertion), the free-axle passthrough, the direction-conflict jam (two
 * opposite sources → zero output, zero explosion), the round-robin per-face
 * {@code max(1, power/3)} package cap, and the transformer ÷4 ×4 pair with sign
 * preservation.
 *
 * <p>Offline harness (the axle test form): level-less fixtures on a vanilla STONE
 * state, the {@code mAdjacencyOverride} seam wires the chain by hand, the gearbox
 * masks are written directly (the {@code /gt6engine gearbox} channel semantics), and
 * the drop/destroy legs degrade to no-ops without a level.
 */
public class GearBoxTest extends GTOfflineTestBase {

	static BlockEntityType<GTGearBoxBlockEntity> sBoxType;
	static BlockEntityType<GTTransformerRotationBlockEntity> sTransType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GTGearBoxBlockEntity>[] tBox = (BlockEntityType<GTGearBoxBlockEntity>[]) new BlockEntityType<?>[1];
		tBox[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTGearBoxBlockEntity(tBox[0], aPos, aState), Blocks.STONE).build(null);
		sBoxType = tBox[0];
		BlockEntityType<GTTransformerRotationBlockEntity>[] tTrans = (BlockEntityType<GTTransformerRotationBlockEntity>[]) new BlockEntityType<?>[1];
		tTrans[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTTransformerRotationBlockEntity(tTrans[0], aPos, aState), Blocks.STONE).build(null);
		sTransType = tTrans[0];
	}

	/** A fresh level-less gearbox fixture with the wood row rating (mMaxThroughPut = VMAX[0] = 16). */
	private static GTGearBoxBlockEntity gearbox() {
		return new GTGearBoxBlockEntity(sBoxType, POS, Blocks.STONE.defaultBlockState());
	}

	/** A fresh level-less transformer fixture (the wood row: 8 → 2, multiplier 4). */
	private static GTTransformerRotationBlockEntity transformer() {
		return new GTTransformerRotationBlockEntity(sTransType, POS, Blocks.STONE.defaultBlockState());
	}

	/**
	 * A capped RU sink — records EVERY injection call's amount, accepts at most
	 * {@code cap} packets per call: the instrument for the per-face max(1, power/3)
	 * pass cap (an unlimited sink would drain everything per tick and hide the cap).
	 */
	public static class CappedSink extends BlockEntity implements ITileEntityEnergy {
		public final long cap;
		public final List<Long> calls = new ArrayList<>();
		public long lastSize = 1;

		public CappedSink(BlockPos aPos, long aCap) {
			super(null, aPos, Blocks.STONE.defaultBlockState());
			cap = aCap;
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == TD.Energy.RU;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == TD.Energy.RU;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.RU.AS_LIST;}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) {
					long tTaken = Math.min(cap, aAmount);
					calls.add(tTaken);
					lastSize = aSize;
					return tTaken;
				}
			}
			return 0;
		}

		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	// ---------------------------------------------------------------------------
	// checkGears — the topology truth table (upstream :271-310)
	// ---------------------------------------------------------------------------

	/** The gear-face bits of a side set. */
	private static int gears(int... aSides) {
		int rMask = 0;
		for (int tSide : aSides) rMask |= (1 << tSide);
		return rMask;
	}

	@Test
	public void checkGearsTopologyTruthTable() {
		GTGearBoxBlockEntity tBox = gearbox();

		// 0 gears and 1 gear always work (upstream :278-283)
		assertTrue(tBox.checkGears()); // the fresh box carries no mask
		for (int tSide = 0; tSide < 6; tSide++) {
			tBox.mAxleGear = gears(tSide);
			assertTrue(tBox.checkGears(), "single gear @" + tSide + " must always work");
		}

		// 2 CORNER gears (no opposite pair on any axis) always work (upstream :284-286):
		// all 15 non-degenerate 2-subsets minus the 3 opposite pairs
		int tCornerPairs = 0;
		for (int tA = 0; tA < 6; tA++) for (int tB = tA + 1; tB < 6; tB++) {
			boolean tOpposite = GTGearBoxBlockEntity.opposite((byte) tA) == tB;
			tBox.mAxleGear = gears(tA, tB);
			if (!tOpposite) {
				tCornerPairs++;
				assertTrue(tBox.checkGears(), "corner pair " + tA + "+" + tB + " must always work");
			} else {
				// an opposite pair works ONLY on its own through-axle (upstream :287-293)
				int tAxisMask = gears(tA, tB);
				tBox.mAxleGear = tAxisMask; // no axle → dead
				assertFalse(tBox.checkGears(), "opposite pair " + tA + "+" + tB + " without an axle must not work");
				for (int tAxis = 1; tAxis <= 3; tAxis++) {
					tBox.mAxleGear = tAxisMask | (tAxis << 6);
					assertEquals(GTGearBoxBlockEntity.axisXYZ(tAxis, (byte) tA), tBox.checkGears(),
							"opposite pair " + tA + "+" + tB + " with axle axis " + tAxis);
				}
			}
		}
		assertEquals(12, tCornerPairs, "C(6,2) - 3 opposite pairs");

		// 3 gears: the TRIANGLE (one gear on each of the three axes) never works;
		// everything else does — unless the through-axle forms a D shape (upstream :294-306)
		for (int tA = 0; tA < 6; tA++) for (int tB = tA + 1; tB < 6; tB++) for (int tC = tB + 1; tC < 6; tC++) {
			int tMask = gears(tA, tB, tC);
			boolean tX = (tMask & 48) != 0, tY = (tMask & 3) != 0, tZ = (tMask & 12) != 0;
			boolean tTriangle = tX && tY && tZ;
			for (int tAxis = 0; tAxis <= 3; tAxis++) {
				tBox.mAxleGear = tMask | (tAxis << 6);
				// axis 1 ↔ mask 48 (sides 4,5), axis 2 ↔ mask 3 (sides 0,1), axis 3 ↔ mask 12 (sides 2,3)
				boolean tDShape = tAxis == 1 && (tMask & 48) == 48 || tAxis == 2 && (tMask & 3) == 3 || tAxis == 3 && (tMask & 12) == 12;
				boolean tExpected = !tTriangle && !tDShape;
				assertEquals(tExpected, tBox.checkGears(),
						"triangle=" + tTriangle + " dshape=" + tDShape + " mask=" + tMask + " axis=" + tAxis);
			}
		}

		// 4 gears: same triangle/D rules as 3 (upstream :294 case 4)
		tBox.mAxleGear = gears(0, 2, 4, 5); // X pair + Y bit + Z bit = all three axes → triangle
		assertFalse(tBox.checkGears(), "the 0/2/4/5 spread uses all three axes = triangle");
		tBox.mAxleGear = gears(0, 1, 2) | (2 << 6); // the Y pair on the Y axle = the D shape
		assertFalse(tBox.checkGears(), "the Y-pair D shape must refuse");
		tBox.mAxleGear = gears(2, 3, 4) | (1 << 6); // the Z pair + W corner on the X axle: no D (X not full), no triangle
		assertTrue(tBox.checkGears(), "Z pair + W corner on the X axle is a legal 3-gear box");

		// 5 and 6 gears NEVER work (upstream :308-309), axle or not
		for (int tAxis = 0; tAxis <= 3; tAxis++) {
			tBox.mAxleGear = 63 & ~(1 << 0) | (tAxis << 6); // 5 gears
			assertFalse(tBox.checkGears(), "5 gears never work (axis " + tAxis + ")");
			tBox.mAxleGear = 63 | (tAxis << 6); // 6 gears
			assertFalse(tBox.checkGears(), "6 gears never work (axis " + tAxis + ")");
		}

		// the upstream side effect: a re-check resets the accumulators (:273-275)
		tBox.mAxleGear = 0;
		tBox.mCurrentSpeed = 7;
		tBox.mCurrentPower = 9;
		tBox.mIgnorePower = 3;
		assertTrue(tBox.checkGears());
		assertEquals(0, tBox.mCurrentSpeed);
		assertEquals(0, tBox.mCurrentPower);
		assertEquals(0, tBox.mIgnorePower);
	}

	// ---------------------------------------------------------------------------
	// the input semantics (upstream doInject :347-410)
	// ---------------------------------------------------------------------------

	@Test
	public void multiInputTakesLowestSpeedAndAccumulatesPower() {
		GTGearBoxBlockEntity tBox = gearbox();
		// gears on N (input 1) + S (output 3): a 2-corner... no — N/S is the Z pair, no axle →
		// dead. Use W (input 4) + N (output 2): a corner pair.
		tBox.mAxleGear = gears(4, 2);
		assertTrue(tBox.mGearsWork = tBox.checkGears());
		CappedSink tSink = new CappedSink(POS.north(), 64);
		tBox.setAdjacencyOverride(aSide -> aSide == 2 ? new EnergyTarget(tSink, (byte) 2) : null);

		// input 1 on the W gear face: speed 8, power 2
		assertEquals(2, tBox.doInject(TD.Energy.RU, (byte) 4, -8, 2, true));
		// input 2 on the same face, SAME sign, speed 4, power 3 — the lowest speed wins,
		// power adds (:393-394); an opposite sign here would jam instead (the conflict test)
		assertEquals(3, tBox.doInject(TD.Energy.RU, (byte) 4, -4, 3, true));
		assertEquals(4, tBox.mCurrentSpeed);
		assertEquals(5, tBox.mCurrentPower);

		// the tick drains into the sink at ±mCurrentSpeed (the sign = the gear physics bit)
		tBox.onTick(11, true);
		assertEquals(5, tSink.calls.stream().mapToLong(Long::longValue).sum(), "all five packets leave");
		assertEquals(4, Math.abs(tSink.lastSize), "every packet rides the LOWEST speed");
		// the tachometer bookkeeping saw the movement (:186/:217)
		assertEquals(5 * 4, tBox.mTransferredLast);
	}

	@Test
	public void overspeedExplodesTheGears() {
		GTGearBoxBlockEntity tBox = gearbox();
		tBox.mAxleGear = gears(4, 2, 3); // the RCON dual-output topology
		assertTrue(tBox.mGearsWork = tBox.checkGears());
		// push the box through the load-grace window (upstream :358 mTimer < 10)
		for (int i = 0; i < 11; i++) tBox.updateEntity();
		assertTrue(tBox.getTimer() >= 10);

		// 64 > VMAX[0] = 16: the gears explode — the mask zeroes, the box SURVIVES re-checked (:366-368)
		assertEquals(4, tBox.doInject(TD.Energy.RU, (byte) 4, -64, 4, true));
		assertEquals(0, tBox.mAxleGear);
		assertTrue(tBox.mGearsWork, "0 gears re-check = true — the block lives on (the RCON chain asserts the drops)");
		// the exploded box accepts nothing on gear faces anymore (the mask is gone)
		assertEquals(0, tBox.doInject(TD.Energy.RU, (byte) 4, -8, 1, true));

		// the grace window: a fresh box (mTimer < 10) eats the packet WITHOUT exploding (:358)
		GTGearBoxBlockEntity tFresh = gearbox();
		tFresh.mAxleGear = gears(4);
		assertEquals(4, tFresh.doInject(TD.Energy.RU, (byte) 4, -64, 4, true));
		assertEquals(gears(4), tFresh.mAxleGear, "the first 10 ticks are the load grace");
	}

	@Test
	public void freeAxleIsAPassthrough() {
		GTGearBoxBlockEntity tBox = gearbox();
		tBox.mAxleGear = 1 << 6; // the through-axle on X, NO gears at all (the monkey-wrench :145 form)
		assertTrue(tBox.checkGears());
		CappedSink tSink = new CappedSink(POS.east(), 64);
		// the packet arrives on the W face (side 4, on the axle axis) and crosses to E
		tBox.setAdjacencyOverride(aSide -> aSide == 5 ? new EnergyTarget(tSink, (byte) 4) : null);
		assertEquals(1, tBox.doInject(TD.Energy.RU, (byte) 4, -16, 1, true));
		assertEquals(1, tSink.calls.size());
		assertEquals(-16, tSink.lastSize, "the passthrough preserves the speed sign untouched (:374)");
	}

	@Test
	public void directionConflictJamsZeroOutputZeroExplosion() {
		GTGearBoxBlockEntity tBox = gearbox();
		tBox.mAxleGear = gears(4, 2, 3); // W input + N/S outputs (the corner topology)
		assertTrue(tBox.mGearsWork = tBox.checkGears());
		CappedSink tNorth = new CappedSink(POS.north(), 64), tSouth = new CappedSink(POS.south(), 64);
		tBox.setAdjacencyOverride(aSide -> aSide == 2 ? new EnergyTarget(tNorth, (byte) 2)
				: aSide == 3 ? new EnergyTarget(tSouth, (byte) 2) : null);

		// source 1: counterclockwise on W → the rotation data settles
		assertEquals(1, tBox.doInject(TD.Energy.RU, (byte) 4, -8, 1, true));
		// source 2: ALSO counterclockwise on the adjacent N gear within the same tick —
		// adjacent gears must COUNTER-rotate, so two same-sign inputs disagree (:382-389)
		assertEquals(1, tBox.doInject(TD.Energy.RU, (byte) 2, -8, 1, true));
		assertTrue(tBox.mJammed, "the conflict must jam the box");
		assertEquals(0, tBox.mRotationData & 64, "the running bit clears");

		// the tick: the jam zeroes the power — zero output, zero explosion (:184)
		tBox.onTick(11, true);
		assertEquals(0, tNorth.calls.size());
		assertEquals(0, tSouth.calls.size());
		assertTrue(tBox.mAxleGear != 0, "no gear explosion on a jam");

		// the jam gate closes the faces (:413) until the masks change
		assertFalse(tBox.isEnergyAcceptingFrom(TD.Energy.RU, (byte) 4, false));
		assertTrue(tBox.isEnergyAcceptingFrom(TD.Energy.RU, (byte) 4, true), "the theoretical probe stays open");
		// the mask write (the /gt6engine gearbox channel, the tool :148-149 form) un-jams
		tBox.setMasks(gears(4, 2, 3), 0);
		assertFalse(tBox.mJammed);
		assertTrue(tBox.isEnergyAcceptingFrom(TD.Energy.RU, (byte) 4, false));
	}

	@Test
	public void leftoverPowerIgnoresFreshInput() {
		GTGearBoxBlockEntity tBox = gearbox();
		tBox.mAxleGear = gears(4, 2);
		tBox.mGearsWork = tBox.checkGears();
		// no adjacency: the tick cannot deliver — the power stays queued
		tBox.setAdjacencyOverride(aSide -> null);
		assertEquals(2, tBox.doInject(TD.Energy.RU, (byte) 4, -8, 2, true));
		tBox.onTick(11, true);
		assertEquals(2, tBox.mCurrentPower, "the undelivered power carries over (:217)");

		// the next input hits the mIgnorePower ladder (:398-403): consumed by the box,
		// REJECTED for the queue — the packet is not wasted into a double-queue
		assertEquals(0, tBox.doInject(TD.Energy.RU, (byte) 4, -8, 3, true));
		assertEquals(2, tBox.mCurrentPower);
	}

	// ---------------------------------------------------------------------------
	// the output round-robin (upstream onTick2 :188-216)
	// ---------------------------------------------------------------------------

	@Test
	public void outputRespectsMaxOnePowerThirdPerFacePerPass() {
		GTGearBoxBlockEntity tBox = gearbox();
		tBox.mAxleGear = gears(4, 2); // W input, N output
		tBox.mGearsWork = tBox.checkGears();
		// the sink could take 4 per call — the CAP is what limits each pass
		CappedSink tSink = new CappedSink(POS.north(), 4);
		tBox.setAdjacencyOverride(aSide -> aSide == 2 ? new EnergyTarget(tSink, (byte) 2) : null);

		// power 7: pass caps are max(1, 7/3)=2, max(1, 5/3)=1, 1, 1, 1, 1
		assertEquals(7, tBox.doInject(TD.Energy.RU, (byte) 4, -8, 7, true));
		tBox.onTick(11, true);
		assertEquals(List.of(2L, 1L, 1L, 1L, 1L, 1L), tSink.calls,
				"each pass inserts at most max(1, remainingPower/3) packets (:193)");
		assertEquals(0, tBox.mCurrentPower);
	}

	@Test
	public void roundRobinAlternatesTheFaces() {
		GTGearBoxBlockEntity tBox = gearbox();
		tBox.mAxleGear = gears(4, 2, 3); // W input + N/S outputs
		tBox.mGearsWork = tBox.checkGears();
		CappedSink tNorth = new CappedSink(POS.north(), 1), tSouth = new CappedSink(POS.south(), 1);
		tBox.setAdjacencyOverride(aSide -> aSide == 2 ? new EnergyTarget(tNorth, (byte) 2)
				: aSide == 3 ? new EnergyTarget(tSouth, (byte) 2) : null);

		// four packets, one per pass: N first (order 0), then S, then N, then S
		assertEquals(4, tBox.doInject(TD.Energy.RU, (byte) 4, -8, 4, true));
		tBox.onTick(11, true);
		assertEquals(2, tNorth.calls.size());
		assertEquals(2, tSouth.calls.size());
		assertEquals(1L, tNorth.calls.get(0));
		assertEquals(1L, tSouth.calls.get(0));
		// the order advanced past both faces (the mOrder rotation :214)
		assertTrue(tBox.mOrder != 0, "the round-robin cursor must have moved");
	}

	@Test
	public void outputSignFollowsTheGearPhysics() {
		GTGearBoxBlockEntity tBox = gearbox();
		tBox.mAxleGear = gears(4, 2, 3);
		tBox.mGearsWork = tBox.checkGears();
		CappedSink tNorth = new CappedSink(POS.north(), 64), tSouth = new CappedSink(POS.south(), 64);
		tBox.setAdjacencyOverride(aSide -> aSide == 2 ? new EnergyTarget(tNorth, (byte) 2)
				: aSide == 3 ? new EnergyTarget(tSouth, (byte) 2) : null);

		// the counterclockwise input (the crank's constant negative sign) on W:
		// getRotations sets B[4] only (the adjacent N/S gears counter-rotate) → both
		// output faces carry the CLEARED bit → -mCurrentSpeed (the negative-sign =
		// counterclockwise direction semantics, verbatim). Power 2 = one packet per face.
		assertEquals(2, tBox.doInject(TD.Energy.RU, (byte) 4, -8, 2, true));
		tBox.onTick(11, true);
		assertEquals(1, tNorth.calls.size());
		assertEquals(1, tSouth.calls.size());
		assertEquals(-8, tNorth.lastSize, "the RU negative sign IS the direction semantic");
		assertEquals(-8, tSouth.lastSize);

		// the clockwise input flips the adjacent-gear bits → +mCurrentSpeed out
		GTGearBoxBlockEntity tCw = gearbox();
		tCw.mAxleGear = gears(4, 2, 3);
		tCw.mGearsWork = tCw.checkGears();
		CappedSink tCwSink = new CappedSink(POS.north(), 64);
		tCw.setAdjacencyOverride(aSide -> aSide == 2 ? new EnergyTarget(tCwSink, (byte) 2) : null);
		assertEquals(1, tCw.doInject(TD.Energy.RU, (byte) 4, 8, 1, true));
		tCw.onTick(11, true);
		assertEquals(8, tCwSink.lastSize);
	}

	// ---------------------------------------------------------------------------
	// the energy face family + the row rating (upstream :412-421, Loader :1669)
	// ---------------------------------------------------------------------------

	@Test
	public void faceFamilyAndRowRating() {
		GTGearBoxBlockEntity tBox = gearbox();
		assertEquals(16, tBox.mMaxThroughPut, "the Custom Wooden Gearbox row NBT_INPUT = VMAX[0]");
		assertEquals(16, GT6Kinetics.GEARBOX_MAX_THROUGHPUT);
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertTrue(tBox.isEnergyType(TD.Energy.RU, tSide, false));
			assertFalse(tBox.isEnergyType(TD.Energy.EU, tSide, false));
			assertFalse(tBox.isEnergyType(TD.Energy.KU, tSide, false));
			assertEquals(0, tBox.getEnergySizeInputMin(TD.Energy.RU, tSide));
			assertEquals(8, tBox.getEnergySizeInputRecommended(TD.Energy.RU, tSide));
			assertEquals(16, tBox.getEnergySizeInputMax(TD.Energy.RU, tSide));
			assertEquals(0, tBox.getEnergySizeOutputMin(TD.Energy.RU, tSide));
			assertEquals(8, tBox.getEnergySizeOutputRecommended(TD.Energy.RU, tSide));
			assertEquals(16, tBox.getEnergySizeOutputMax(TD.Energy.RU, tSide));
			assertEquals(1, tBox.getEnergyTypes(tSide).size());
		}
		// the face gate (:349): no mask → nothing accepts anywhere
		for (byte tSide = 0; tSide < 6; tSide++) assertEquals(0, tBox.doInject(TD.Energy.RU, tSide, -8, 1, true));
		// the theoretical probe of doInject (:350) answers while no input ran this tick
		tBox.mAxleGear = gears(4);
		assertEquals(1, tBox.doInject(TD.Energy.RU, (byte) 4, -8, 1, false));
		assertEquals(0, tBox.doInject(TD.Energy.RU, (byte) 2, -8, 1, false), "the unmasked face refuses even the probe");
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2/writeToNBT2 :64-78)
	// ---------------------------------------------------------------------------

	@Test
	public void gearboxNbtRoundTrip() {
		GTGearBoxBlockEntity tBox = gearbox();
		tBox.mAxleGear = gears(2, 3) | (1 << 6);
		tBox.mJammed = true;
		CompoundTag tTag = tBox.saveWithoutMetadata();
		assertEquals("gearbox", tTag.getString("te_name"));
		assertTrue(tTag.contains(GTGearBoxBlockEntity.NBT_CONNECTION, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(GTGearBoxBlockEntity.NBT_STOPPED, Tag.TAG_ANY_NUMERIC));

		GTGearBoxBlockEntity tLoaded = gearbox();
		tLoaded.load(tTag);
		assertEquals(gears(2, 3) | (1 << 6), tLoaded.mAxleGear, "the unsignB chain round-trips the axis bits");
		assertTrue(tLoaded.mJammed);
		// load re-checks the gears (upstream :70): the N/S pair + X axle = dead → mGearsWork false
		assertFalse(tLoaded.mGearsWork);
	}

	// ---------------------------------------------------------------------------
	// the transformer (task spec 2 — the ÷4 ×4 pair)
	// ---------------------------------------------------------------------------

	@Test
	public void transformerDividesSpeedAndMultipliesPower() {
		GTTransformerRotationBlockEntity tTrans = transformer();
		CappedSink tSink = new CappedSink(POS.east(), 64);
		// FRONT = north (side 2) is the input, BACK = south (side 3)... the opposite of
		// NORTH is SOUTH (side 3): the sink sits on the BACK face
		tTrans.setAdjacencyOverride(aSide -> aSide == 3 ? new EnergyTarget(tSink, (byte) 2) : null);

		// one 16x1 packet in on the front (the crank's counterclockwise packet)
		assertEquals(1, tTrans.doInject(TD.Energy.RU, (byte) 2, -16, 1, true));
		assertEquals(16, tTrans.mStorage);
		tTrans.onTick(11, true);
		// ONE burst of MULTIPLIER packets at speed/4 — ÷4 speed ×4 power, sign preserved
		assertEquals(1, tSink.calls.size());
		assertEquals(4, tSink.calls.get(0), "the multiplier = 4 packets per burst");
		assertEquals(-4, tSink.lastSize, "16 → 4 speed ÷4, NEGATIVE preserved (direction kept)");
		assertEquals(0, tTrans.mStorage, "the drain books the whole burst");
		assertTrue(tTrans.mActive);
		assertEquals(-16, tTrans.mLastInSize);
		assertEquals(-4, tTrans.mLastOutSize);
		assertEquals(4, tTrans.mLastOutAmount);
		// |in| == |out| : the power conservation of the ÷4×4 pair
		assertEquals(Math.abs(tTrans.mLastInSize * tTrans.mLastInAmount), Math.abs(tTrans.mLastOutSize * tTrans.mLastOutAmount));

		// the wood row numbers themselves (8 → 2): an 8-speed input converts to 2
		GTTransformerRotationBlockEntity tWood = transformer();
		CappedSink tWoodSink = new CappedSink(POS.east(), 64);
		tWood.setAdjacencyOverride(aSide -> aSide == 3 ? new EnergyTarget(tWoodSink, (byte) 2) : null);
		assertEquals(1, tWood.doInject(TD.Energy.RU, (byte) 2, -8, 1, true));
		tWood.onTick(11, true);
		assertEquals(-2, tWoodSink.lastSize, "the wood row: 8 → 2 (Loader :1668)");
		assertEquals(4, tWoodSink.calls.get(0));
	}

	@Test
	public void transformerFacesAndBands() {
		GTTransformerRotationBlockEntity tTrans = transformer();
		assertEquals(8, GT6Kinetics.TRANSFORMER_INPUT_SPEED);
		assertEquals(2, GT6Kinetics.TRANSFORMER_OUTPUT_SPEED);
		assertEquals(4, GT6Kinetics.TRANSFORMER_MULTIPLIER);
		// the FRONT face only accepts, the BACK face only emits (:42-43)
		assertTrue(tTrans.isInput((byte) 2));
		assertTrue(tTrans.isOutput((byte) 3));
		assertFalse(tTrans.isInput((byte) 3));
		assertFalse(tTrans.isOutput((byte) 2));
		assertTrue(tTrans.isEnergyAcceptingFrom(TD.Energy.RU, (byte) 2, false));
		assertFalse(tTrans.isEnergyAcceptingFrom(TD.Energy.RU, (byte) 3, false));
		assertTrue(tTrans.isEnergyEmittingTo(TD.Energy.RU, (byte) 3, false));
		assertFalse(tTrans.isEnergyEmittingTo(TD.Energy.RU, (byte) 2, false));
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertEquals(1, tTrans.getEnergySizeInputMin(TD.Energy.RU, tSide));
			assertEquals(8, tTrans.getEnergySizeInputRecommended(TD.Energy.RU, tSide));
			assertEquals(16, tTrans.getEnergySizeInputMax(TD.Energy.RU, tSide));
			assertEquals(1, tTrans.getEnergySizeOutputMin(TD.Energy.RU, tSide));
			assertEquals(2, tTrans.getEnergySizeOutputRecommended(TD.Energy.RU, tSide));
			assertEquals(4, tTrans.getEnergySizeOutputMax(TD.Energy.RU, tSide));
		}
	}

	@Test
	public void transformerOverloadAndFullCapacitor() {
		GTTransformerRotationBlockEntity tTrans = transformer();
		// a packet above the input max (32 > 16): consumed fully, the capacitor clears,
		// one prevention strike (the Stats.doInject :57-62 + overload :140-148 form)
		assertEquals(2, tTrans.doInject(TD.Energy.RU, (byte) 2, -32, 2, true));
		assertEquals(0, tTrans.mStorage);
		assertEquals(1, tTrans.mExplosionPrevention);

		// fill the capacitor to the cap (16 = 2×8): the next packet is REFUSED (:63)
		assertEquals(1, tTrans.doInject(TD.Energy.RU, (byte) 2, -8, 1, true));
		assertEquals(1, tTrans.doInject(TD.Energy.RU, (byte) 2, -8, 1, true));
		assertEquals(16, tTrans.mStorage);
		assertEquals(0, tTrans.doInject(TD.Energy.RU, (byte) 2, -8, 1, true), "a full capacitor refuses — the packet stays at the source");
	}

	@Test
	public void wasteVentDrainsRefusedBurstsAndDoesNotReemit() {
		// the waste leg (the converter :92 tail — the row NBT_WASTE_ENERGY = T, Loader
		// :1668; aMode = 0 → units(16, 16, 16) = 16 = the whole capacity): a burst the
		// consumer refuses is VENTED the same tick — upstream is a funnel that vents
		// what did not flow ("不通就漏光"), not a buffer
		GTTransformerRotationBlockEntity tTrans = transformer();
		tTrans.setAdjacencyOverride(aSide -> null); // the 全拒 tick: no consumer anywhere

		assertEquals(1, tTrans.doInject(TD.Energy.RU, (byte) 2, -16, 1, true));
		assertEquals(16, tTrans.mStorage);
		tTrans.onTick(11, true);
		assertEquals(0, tTrans.mStorage, "the refused burst vents the whole capacitor");
		assertFalse(tTrans.mActive);
		assertEquals(0, tTrans.mLastOutSize, "nothing was emitted — no record");

		// 下拍不重发: the vent left nothing stored, so the next tick emits nothing even
		// with a hungry sink attached
		CappedSink tSink = new CappedSink(POS.east(), 64);
		tTrans.setAdjacencyOverride(aSide -> aSide == 3 ? new EnergyTarget(tSink, (byte) 2) : null);
		tTrans.onTick(12, true);
		assertTrue(tSink.calls.isEmpty(), "the vented burst must not re-emit on the next tick");
		assertEquals(0, tTrans.mStorage);
	}

	@Test
	public void transformerNbtRoundTrip() {
		GTTransformerRotationBlockEntity tTrans = transformer();
		tTrans.mStorage = 12;
		tTrans.mStopped = true;
		CompoundTag tTag = tTrans.saveWithoutMetadata();
		assertEquals("transformer_rotation", tTag.getString("te_name"));
		assertTrue(tTag.contains(GTTransformerRotationBlockEntity.NBT_CAPACITOR, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(GTTransformerRotationBlockEntity.NBT_STOPPED, Tag.TAG_ANY_NUMERIC));

		GTTransformerRotationBlockEntity tLoaded = transformer();
		tLoaded.load(tTag);
		assertEquals(12, tLoaded.mStorage);
		assertTrue(tLoaded.mStopped);
		assertFalse(tLoaded.isEnergyAcceptingFrom(TD.Energy.RU, (byte) 2, false), "the stopped box refuses (the :151 gate)");
		assertTrue(tLoaded.isEnergyAcceptingFrom(TD.Energy.RU, (byte) 2, true), "the theoretical probe stays open");
	}
}
