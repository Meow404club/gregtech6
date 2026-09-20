package gregtech6.tileentity.energy.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;

/**
 * The Magic Field Absorber offline tests (task p32-magic-absorber) — the acceptance
 * arms over the {@link GT6MagicAbsorberBlockEntity} probe/emit split:
 * <ul>
 * <li>① the registration census (the offline half): the BET path, the block path and the
 *     tile-entity name ride "magic_absorber" (the Loader :1005 id 10180 twin) — the live
 *     half is the RCON setblock + the onCommonSetup boot log;</li>
 * <li>② the three-branch probe (:85-88 minus the :89-97 TF cut): the Dragon Egg → QU 64,
 *     the skull family (standing AND wall forms) → TU 1, anything else → idle with the
 *     :82-83 reset re-arming the 64 default;</li>
 * <li>③ the emission metering (:101-108 + the gt.last_out book): the egg emits 64 packets
 *     of size 1 per tick into an accepting neighbor, the skull 1; a rejecting neighbor
 *     books NOTHING (the no-consumer posture — the open-wire lesson);</li>
 * <li>④ the faces (:116-121): the emitter answer is type-blind, the FACING face is the
 *     only emission face, the size band min=max=rec=mOutput, the type family ALL_GT;</li>
 * <li>⑤ the stop arm (mStopped) freezes the whole tick body (the :78 gate);</li>
 * <li>⑥ the NBT round trip (gt.active/gt.stopped/gt.last_out, :54-63).</li>
 * </ul>
 * The TF trophy arm (:89-97) is the task-card CUT: no offline or live path can arm KU/
 * HU/LU/CU emission — arm ② pins the else-idle leg over a non-trophy block.
 */
public class GT6MagicAbsorberTest {

	static BlockEntityType<GT6MagicAbsorberBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);
	static BlockState STONE; // assigned in boot() — Blocks.* needs the vanilla bootstrap first

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		// vanilla bootstrap first (the GT6LangParityTest.boot shape; offline-expected throwables)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline-expected
		}
		STONE = Blocks.STONE.defaultBlockState();
		BlockEntityType<GT6MagicAbsorberBlockEntity>[] tHolder =
				(BlockEntityType<GT6MagicAbsorberBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6MagicAbsorberBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** A fresh absorber BE at the fixture (the probe seam feeds the trophy directly). */
	private static GT6MagicAbsorberBlockEntity absorber() {
		return new GT6MagicAbsorberBlockEntity(sType, POS, STONE);
	}

	// ---------------------------------------------------------------------------
	// ① the registration census (the offline half)
	// ---------------------------------------------------------------------------

	@Test
	public void theAbsorberIdentityRidesTheUpstreamRow() {
		// the BET path / tile-entity name twin (the /gt6machine report-name face)
		assertEquals("magic_absorber", absorber().getTileEntityName());
	}

	// ---------------------------------------------------------------------------
	// ② the three-branch probe (upstream :82-97, the TF arm cut)
	// ---------------------------------------------------------------------------

	@Test
	public void theDragonEggArmsQuantumSixtyFour() {
		GT6MagicAbsorberBlockEntity tAbsorber = absorber();
		tAbsorber.probe(Blocks.DRAGON_EGG); // :85-86
		assertTrue(tAbsorber.mActive, "the egg arms the absorber");
		assertEquals(64, tAbsorber.mOutput, "the egg output is 64");
		assertEquals(TD.Energy.QU, tAbsorber.mEnergyTypeEmitted, "the egg emits QU");
	}

	@Test
	public void theSkullFamilyArmsTractionOne() {
		// the standing AND the wall forms (the single 1.7.10 Blocks.skull block split)
		for (Block tSkull : new Block[] {
				Blocks.SKELETON_SKULL, Blocks.SKELETON_WALL_SKULL,
				Blocks.WITHER_SKELETON_SKULL, Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD,
				Blocks.ZOMBIE_HEAD, Blocks.CREEPER_HEAD, Blocks.DRAGON_HEAD, Blocks.PIGLIN_HEAD}) {
			GT6MagicAbsorberBlockEntity tAbsorber = absorber();
			tAbsorber.probe(tSkull); // :87-88
			assertTrue(tAbsorber.mActive, tSkull + " arms the absorber");
			assertEquals(1, tAbsorber.mOutput, tSkull + " output is 1");
			assertEquals(TD.Energy.TU, tAbsorber.mEnergyTypeEmitted, tSkull + " emits TU");
		}
	}

	@Test
	public void anythingElseLeavesTheAbsorberIdleAtThe64Default() {
		GT6MagicAbsorberBlockEntity tAbsorber = absorber();
		// a live trophy first (the re-check may retract it), then the idle legs
		tAbsorber.probe(Blocks.DRAGON_EGG);
		assertTrue(tAbsorber.mActive);
		tAbsorber.probe(Blocks.STONE); // :82-83 — the reset re-arms the defaults
		assertFalse(tAbsorber.mActive, "a non-trophy leaves the absorber idle");
		assertEquals(64, tAbsorber.mOutput, "the output default re-arms at 64");
		tAbsorber.probe(null); // the no-level posture
		assertFalse(tAbsorber.mActive, "no trophy, no activity");
	}

	// ---------------------------------------------------------------------------
	// ③ the emission metering (the :101-108 arm over the size-irrelevant branch pair)
	// ---------------------------------------------------------------------------

	@Test
	public void theEggEmitsSixtyFourSizeOnePacketsIntoTheNetwork() {
		GT6MagicAbsorberBlockEntity tAbsorber = absorber();
		tAbsorber.probe(Blocks.DRAGON_EGG);
		CountingSink tSink = new CountingSink();
		tSink.recordSize = true;
		tAbsorber.setAdjacencyOverrideForTest(adjacencyAt(tSink, (byte) 0 /* DOWN — the default facing */));
		tAbsorber.emitOnce();
		assertEquals(64, tSink.packets, "the egg budget is 64 packets per tick");
		assertEquals(1, tSink.lastSize, "the QU packet SIZE is 1 (the :104-105 size-irrelevant arm)");
		assertEquals(64, tSink.totalMass, "the per-tick mass is 64");
		assertEquals(64, tAbsorber.mLastOut, "the meter books the accepted mass");
		assertTrue(tAbsorber.mActive);
	}

	@Test
	public void theSkullEmitsOneSizeOnePacketIntoTheNetwork() {
		GT6MagicAbsorberBlockEntity tAbsorber = absorber();
		tAbsorber.probe(Blocks.SKELETON_SKULL);
		CountingSink tSink = new CountingSink();
		tAbsorber.setAdjacencyOverrideForTest(adjacencyAt(tSink, (byte) 0));
		tAbsorber.emitOnce();
		assertEquals(1, tSink.packets, "the skull budget is 1 packet per tick");
		assertEquals(1, tSink.totalMass, "the per-tick mass is 1");
		assertEquals(1, tAbsorber.mLastOut, "the meter books the accepted mass");
	}

	@Test
	public void noConsumerBooksNothing() {
		GT6MagicAbsorberBlockEntity tAbsorber = absorber();
		tAbsorber.probe(Blocks.DRAGON_EGG);
		CountingSink tSink = new CountingSink();
		tSink.rejecting = true; // nothing out there takes QU
		tAbsorber.setAdjacencyOverrideForTest(adjacencyAt(tSink, (byte) 0));
		tAbsorber.emitOnce();
		assertEquals(0, tSink.packets, "nothing landed");
		assertEquals(0, tAbsorber.mLastOut, "the open-wire posture: no acceptance, no book");
		assertTrue(tAbsorber.mActive, "the probe still reports the trophy (the emission is the consumer's verdict)");
	}

	// ---------------------------------------------------------------------------
	// ④ the faces (:116-121)
	// ---------------------------------------------------------------------------

	@Test
	public void theFacingFaceIsTheOnlyEmissionFace() {
		GT6MagicAbsorberBlockEntity tAbsorber = absorber();
		tAbsorber.probe(Blocks.DRAGON_EGG);
		tAbsorber.mFacing = 0; // DOWN
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertEquals(tSide == 0, tAbsorber.isEnergyEmittingTo(TD.Energy.QU, tSide, true),
					"side " + tSide + ": the FACING face only (:117)");
			assertFalse(tAbsorber.isEnergyAcceptingFrom(TD.Energy.QU, tSide, true),
					"side " + tSide + ": a pure source accepts nothing");
		}
		// the live gate folds the activity: an IDLE absorber answers the theoretical probe
		// only (the conductor must not visually toggle; the live emission waits for a trophy)
		GT6MagicAbsorberBlockEntity tIdle = absorber(); // no trophy probed, mActive = false
		assertFalse(tIdle.isEnergyEmittingTo(TD.Energy.QU, (byte) 0, false), "an idle absorber is dead on the live probe");
		assertTrue(tIdle.isEnergyEmittingTo(TD.Energy.QU, (byte) 0, true), "…but connectable on the theoretical probe (:117 super form)");
		assertTrue(tAbsorber.isEnergyEmittingTo(TD.Energy.QU, (byte) 0, false), "the armed absorber emits on the live probe");
		assertTrue(tAbsorber.isEnergyType(TD.Energy.QU, (byte) 0, true), ":116 — the emitter answer is type-blind");
		assertTrue(tAbsorber.isEnergyType(TD.Energy.KU, (byte) 0, true), ":116 — ANY type on the emitting probe");
		assertFalse(tAbsorber.isEnergyType(TD.Energy.QU, (byte) 0, false), ":116 — nothing on the accepting probe");
		// the size band min=max=rec=mOutput (:118-120)
		assertEquals(64, tAbsorber.getEnergySizeOutputMin(TD.Energy.QU, (byte) 0));
		assertEquals(64, tAbsorber.getEnergySizeOutputMax(TD.Energy.QU, (byte) 0));
		assertEquals(64, tAbsorber.getEnergySizeOutputRecommended(TD.Energy.QU, (byte) 0));
		// the display family (:121)
		Collection<TagData> tTypes = tAbsorber.getEnergyTypes((byte) 0);
		assertTrue(tTypes.contains(TD.Energy.QU) && tTypes.contains(TD.Energy.TU), "ALL_GT names both live types");
	}

	// ---------------------------------------------------------------------------
	// ⑤ the stop arm
	// ---------------------------------------------------------------------------

	@Test
	public void theStopArmFreezesTheTickBody() {
		GT6MagicAbsorberBlockEntity tAbsorber = absorber();
		tAbsorber.mStopped = true;
		CountingSink tSink = new CountingSink();
		tAbsorber.setAdjacencyOverrideForTest(adjacencyAt(tSink, (byte) 0));
		tAbsorber.onTick(600, true); // the :78 gate
		assertEquals(0, tSink.packets, "no emission while stopped");
		assertFalse(tAbsorber.mActive, "no probe while stopped (the mCheck arm never ran)");
	}

	// ---------------------------------------------------------------------------
	// ⑥ the NBT round trip
	// ---------------------------------------------------------------------------

	@Test
	public void theStateSurvivesASaveLoadRoundTrip() {
		GT6MagicAbsorberBlockEntity tAbsorber = absorber();
		tAbsorber.probe(Blocks.DRAGON_EGG);
		CountingSink tSink = new CountingSink();
		tAbsorber.setAdjacencyOverrideForTest(adjacencyAt(tSink, (byte) 0));
		tAbsorber.emitOnce();
		tAbsorber.mStopped = true;
		CompoundTag tNBT = new CompoundTag();
		tAbsorber.saveAdditional(tNBT);
		assertTrue(tNBT.contains(GT6MagicAbsorberBlockEntity.NBT_ACTIVE), "gt.active persisted");
		assertTrue(tNBT.contains(GT6MagicAbsorberBlockEntity.NBT_STOPPED), "gt.stopped persisted");
		assertTrue(tNBT.contains(GT6MagicAbsorberBlockEntity.NBT_LAST_OUT), "gt.last_out persisted");

		GT6MagicAbsorberBlockEntity tRestored = absorber();
		tRestored.load(tNBT);
		assertTrue(tRestored.mActive, "the activity survived");
		assertTrue(tRestored.mStopped, "the stop survived");
		assertEquals(64, tRestored.mLastOut, "the meter survived");
		tRestored.resetAccounting();
		assertEquals(0, tRestored.mLastOut, "the reset arm zeroes the meter");
	}

	// ---------------------------------------------------------------------------
	// the harness (the laser-domain sink form, accepting ANY type on ONE side)
	// ---------------------------------------------------------------------------

	/** A sink that accepts every type on ONE side, counting the whole packets. */
	static class CountingSink implements ITileEntityEnergy {
		long packets = 0, totalMass = 0, lastSize = 0;
		boolean rejecting = false;
		boolean recordSize = false;

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting;
		}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {
			return TD.Energy.ALL_GT;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return !rejecting;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return false;
		}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (!isEnergyAcceptingFrom(aEnergyType, aSide, false)) return 0;
			if (aDoInject) {
				packets += aAmount;
				totalMass += aAmount * Math.abs(aSize);
				if (recordSize) lastSize = aSize;
			}
			return aAmount;
		}

		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
			return 0;
		}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
			return 0;
		}

		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {
			return 0;
		}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
			return 1;
		}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
			return 0;
		}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
			return Long.MAX_VALUE;
		}

		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
			return 0;
		}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
			return 0;
		}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
			return 0;
		}
	}

	/** The adjacency seam pinning the neighbor at ONE side (the laser-domain form). */
	static IEnergyAdjacency adjacencyAt(ITileEntityEnergy aReceiver, byte aSideOfEmitter) {
		return aEmitterSide -> {
			if (aEmitterSide != aSideOfEmitter) return null;
			Direction tDir = Direction.from3DDataValue(aSideOfEmitter);
			byte tOpposite = (byte) tDir.getOpposite().get3DDataValue();
			return new EnergyTarget(aReceiver, tOpposite);
		};
	}
}
