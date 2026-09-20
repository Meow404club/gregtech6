package gregtech6.tileentity.energy.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import gregtech6.registry.GT6Lasers;

/**
 * The laser domain offline tests (task p32-qu-laser-domain) — the CO2 Laser (EU→LU) and
 * Laser Absorber (LU→EU) converters over the shared {@link GT6DynamoBlockEntity} core with
 * BOTH arms re-typed (the GT6EuBridgeBlockEntityTest one-arm shape generalized). The
 * acceptance arms:
 * <ul>
 * <li>① the registration census (the offline half): the ten rows carry the upstream metaIds
 *     10101-10105 / 10151-10155 and the SHARED ladder {32,128,512,2048,8192} →
 *     {16,64,256,1024,4096} (Loader :930-934/:976-980 verbatim) — the live half is the
 *     RCON /give census;</li>
 * <li>② the five-tier ladder, BOTH directions: every rung converts one in-sized packet
 *     into exactly one out-sized packet (the units() half-rate), and the chain through the
 *     LU hop keeps the wire-lossless ledger (laser.out == absorber.in);</li>
 * <li>③ the face predicates: the laser takes ALL-BUT-FRONT (Base10 :176), the absorber
 *     takes the BACK only (MultiTileEntityLaserAbsorberElectric :35);</li>
 * <li>④ the cross-domain reject (EU vs LU reference equality) and the frequency gate: an
 *     8-sized packet is below the laser's 16 inMin → the Root white burn (Root:717), an
 *     oversize packet strikes the 100-grace ladder;</li>
 * <li>the WASTE_ENERGY = T arms: no consumer still pays the intake, the vent empties the
 *     bucket every tick (the :92 tail);</li>
 * <li>the negative-sign conjunct folds to positive packets on BOTH families (LU ∉
 *     ALL_NEGATIVE_ALLOWED, TD.java:202);</li>
 * <li>the accounting pair (gt.last_in / gt.last_out) survives a save/load round trip (the
 *     RCON live metering face).</li>
 * </ul>
 */
public class GT6LaserDomainTest {

	static BlockEntityType<GT6LaserConverterBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	static final byte FRONT = 2, BACK = 3; // mFacing default NORTH = FRONT (the emission face)

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		BlockEntityType<GT6LaserConverterBlockEntity>[] tHolder =
				(BlockEntityType<GT6LaserConverterBlockEntity>[]) new BlockEntityType<?>[1];
		// the laser-type fixture (EU→LU) — the absorber tests construct their own typed BEs
		// over the same holder (the offline type-capture form of the bridge test)
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6LaserConverterBlockEntity(tHolder[0], TD.Energy.EU, TD.Energy.LU, false, aPos, aState),
				Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** A CO2 Laser BE at the fixture (EU→LU, all-but-front intake). */
	private static GT6LaserConverterBlockEntity laser() {
		return new GT6LaserConverterBlockEntity(sType, TD.Energy.EU, TD.Energy.LU, false, POS, Blocks.STONE.defaultBlockState());
	}

	/** A Laser Absorber BE at the fixture (LU→EU, back-only intake). */
	private static GT6LaserConverterBlockEntity absorber() {
		return new GT6LaserConverterBlockEntity(sType, TD.Energy.LU, TD.Energy.EU, true, POS, Blocks.STONE.defaultBlockState());
	}

	/** A family BE at an arbitrary ladder row (the tierForOfflineTest core seam). */
	private static GT6LaserConverterBlockEntity laser(long aInput, long aOutput) {
		GT6LaserConverterBlockEntity tLaser = laser();
		tLaser.tierForOfflineTest(aInput, aOutput);
		return tLaser;
	}

	/** A Laser Absorber BE at an arbitrary ladder row. */
	private static GT6LaserConverterBlockEntity absorber(long aInput, long aOutput) {
		GT6LaserConverterBlockEntity tAbsorber = absorber();
		tAbsorber.tierForOfflineTest(aInput, aOutput);
		return tAbsorber;
	}

	// ---------------------------------------------------------------------------
	// ① the registration census (the offline half of the FML registration assertion)
	// ---------------------------------------------------------------------------

	@Test
	public void theLaserDomainRowsCarryTheUpstreamColumns() {
		// the metaId ladders (Loader :930-934 laser / :976-980 absorber)
		assertEquals(10101, GT6Lasers.CO2_LASER_ROWS.get(0).metaId());
		assertEquals(10105, GT6Lasers.CO2_LASER_ROWS.get(4).metaId());
		assertEquals(10151, GT6Lasers.LASER_ABSORBER_ROWS.get(0).metaId());
		assertEquals(10155, GT6Lasers.LASER_ABSORBER_ROWS.get(4).metaId());
		assertEquals("LV", GT6Lasers.CO2_LASER_ROWS.get(0).voltageWord());
		assertEquals("IV", GT6Lasers.LASER_ABSORBER_ROWS.get(4).voltageWord());
		// the SHARED ladders: in = {32,128,512,2048,8192}, out = in/2 on every rung — the
		// absorber's NBT_INPUT column IS the laser's NBT_OUTPUT column (the Loader rows)
		for (int i = 0; i < 5; i++) {
			assertEquals(GT6Lasers.LASER_INPUTS[i], GT6Lasers.LASER_OUTPUTS[i] * 2, "rung " + i + " half-rate");
		}
	}

	// ---------------------------------------------------------------------------
	// ② the five-tier ladder, both directions (the acceptance ③ per-value walk)
	// ---------------------------------------------------------------------------

	@Test
	public void laserLadderEveryTierEmitsExactlyTheOutColumn() {
		for (int i = 0; i < GT6Lasers.LASER_INPUTS.length; i++) {
			long tIn = GT6Lasers.LASER_INPUTS[i], tOut = GT6Lasers.LASER_OUTPUTS[i];
			GT6LaserConverterBlockEntity tLaser = laser(tIn, tOut);
			CountingSink tSink = new CountingSink(TD.Energy.LU);
			tSink.recordSize = true;
			tLaser.setAdjacencyOverrideForTest(adjacencyAt(tSink, FRONT));
			// one in-sized EU packet enters ANY side but the front (side 3 here)
			assertEquals(1, tLaser.doEnergyInjection(TD.Energy.EU, BACK, tIn, 1, true),
					"laser rung " + tIn + ": the in-sized packet is above min " + (tIn / 2) + " and below max " + (tIn * 2));
			tLaser.onTick(100, true);
			assertEquals(1, tSink.packets, "laser rung " + tIn + ": one packet emitted");
			assertEquals(tOut, tSink.lastSize, "laser rung " + tIn + ": the LU packet SIZE is the out column");
			assertEquals(tOut, tSink.totalMass, "laser rung " + tIn + ": the emitted mass = the out column");
			assertEquals(tIn, tLaser.mLastIn, "laser rung " + tIn + ": the accounting in = the consumed EU");
			assertEquals(tOut, tLaser.mLastOut, "laser rung " + tIn + ": the accounting out = the emitted LU");
			assertEquals(0, tLaser.mStorage, "WASTE_ENERGY=T: the vent emptied the bucket");
			assertTrue(tLaser.mActive, "the emission fired");
		}
	}

	@Test
	public void absorberLadderEveryTierEmitsExactlyTheOutColumn() {
		for (int i = 0; i < GT6Lasers.LASER_INPUTS.length; i++) {
			long tIn = GT6Lasers.LASER_OUTPUTS[i], tOut = GT6Lasers.LASER_INPUTS[i] / 4;
			GT6LaserConverterBlockEntity tAbsorber = absorber(tIn * 2, tOut * 2);
			// the absorber rung mirrors the laser rung: NBT_INPUT = 2×out, NBT_OUTPUT = out —
			// T1 32→16: a 16 LU packet (the laser's T1 emission) → 8 EU out
			CountingSink tSink = new CountingSink(TD.Energy.EU);
			tSink.recordSize = true;
			tAbsorber.setAdjacencyOverrideForTest(adjacencyAt(tSink, FRONT));
			// the LU packet enters the BACK face only
			assertEquals(1, tAbsorber.doEnergyInjection(TD.Energy.LU, BACK, tIn, 1, true),
					"absorber rung " + (tIn * 2) + "→" + (tOut * 2) + ": the " + tIn + " LU packet rides the min door " + tIn);
			tAbsorber.onTick(100, true);
			assertEquals(1, tSink.packets, "absorber rung " + i + ": one packet emitted");
			assertEquals(tOut, tSink.lastSize, "absorber rung " + i + ": the EU packet SIZE is the converted out");
			assertEquals(tIn, tAbsorber.mLastIn, "absorber rung " + i + ": the accounting in = the consumed LU");
			assertEquals(tOut, tAbsorber.mLastOut, "absorber rung " + i + ": the accounting out = the emitted EU");
		}
	}

	// ---------------------------------------------------------------------------
	// ③ the face predicates (the ONE family split)
	// ---------------------------------------------------------------------------

	@Test
	public void laserTakesAllButFrontAndAbsorberTakesBackOnly() {
		GT6LaserConverterBlockEntity tLaser = laser(32, 16);
		GT6LaserConverterBlockEntity tAbsorber = absorber(32, 16);
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertEquals(tSide != FRONT, tLaser.isInput(tSide),
					"laser side " + tSide + ": all-but-front (Base10 :176)");
			assertEquals(tSide == BACK, tAbsorber.isInput(tSide),
					"absorber side " + tSide + ": back only (MultiTileEntityLaserAbsorberElectric :35)");
			// both emit FRONT only
			assertEquals(tSide == FRONT, tAbsorber.isEnergyEmittingTo(tAbsorber.outputType(), tSide, true),
					"absorber side " + tSide + ": the EU emission face is the FRONT");
		}
		// the face probes name the split through the acceptance face (aTheoretical=false)
		assertTrue(tLaser.isEnergyAcceptingFrom(TD.Energy.EU, BACK, false), "any-but-front intake is live");
		assertFalse(tLaser.isEnergyAcceptingFrom(TD.Energy.EU, FRONT, false), "the FRONT is emission, not intake");
		assertFalse(tAbsorber.isEnergyAcceptingFrom(TD.Energy.LU, FRONT, false), "the absorber refuses the FRONT");
		assertTrue(tAbsorber.isEnergyAcceptingFrom(TD.Energy.LU, BACK, false), "the absorber takes the BACK (the beam face)");
		// and the laser emission face is the FRONT through the live gate too
		assertTrue(tLaser.isEnergyEmittingTo(TD.Energy.LU, FRONT, true), "the laser's LU emission face is the FRONT");
		assertFalse(tLaser.isEnergyEmittingTo(TD.Energy.LU, BACK, true), "no emission towards the intake sides");
	}

	// ---------------------------------------------------------------------------
	// ④ the cross-domain reject + the frequency gate
	// ---------------------------------------------------------------------------

	@Test
	public void crossDomainOffersAreRefusedOutright() {
		GT6LaserConverterBlockEntity tLaser = laser(32, 16);
		// the laser is an EU machine: an LU/RU/KU offer is refused outright
		assertEquals(0, tLaser.doEnergyInjection(TD.Energy.LU, BACK, 32, 1, true), "LU refused on the EU intake");
		assertEquals(0, tLaser.doEnergyInjection(TD.Energy.RU, BACK, 32, 1, true), "RU refused");
		assertEquals(0, tLaser.doEnergyInjection(TD.Energy.KU, BACK, 32, 1, true), "KU refused");
		assertEquals(0, tLaser.mLastIn, "nothing consumed");
		// the absorber is an LU machine: an EU offer is refused outright
		GT6LaserConverterBlockEntity tAbsorber = absorber(32, 16);
		assertEquals(0, tAbsorber.doEnergyInjection(TD.Energy.EU, BACK, 32, 1, true), "EU refused on the LU intake");
		assertEquals(0, tAbsorber.mLastIn, "nothing consumed");
		// the type collections name both converter halves
		Collection<TagData> tTypes = tLaser.getEnergyTypes(BACK);
		assertTrue(tTypes.contains(TD.Energy.EU) && tTypes.contains(TD.Energy.LU), "EU in + LU out");
		Collection<TagData> tAbsorberTypes = tAbsorber.getEnergyTypes(BACK);
		assertTrue(tAbsorberTypes.contains(TD.Energy.LU) && tAbsorberTypes.contains(TD.Energy.EU), "LU in + EU out");
	}

	@Test
	public void belowMinOffersAreWhiteBurnedAndOversizeOffersStrikeTheLadder() {
		GT6LaserConverterBlockEntity tLaser = laser(32, 16);
		// an 8 EU packet (below min 16): the ROOT gate swallows the offer — the white burn
		// (Root:717). THE FREQUENCY GATE: a detuned source tunes to nothing.
		assertEquals(1, tLaser.doEnergyInjection(TD.Energy.EU, BACK, 8, 1, true), "the offer is consumed");
		assertEquals(0, tLaser.mStorage, "for nothing (the white burn)");
		assertEquals(0, tLaser.mLastIn, "the white burn is not intake");
		// a 65 EU packet (above max 64): consumed ALL, the overload ladder strikes once
		assertEquals(3, tLaser.doEnergyInjection(TD.Energy.EU, BACK, 65, 3, true), "oversize: the whole offer consumed");
		assertEquals(1, tLaser.mExplosionPrevention, "strike one of the 100-grace ladder");
	}

	// ---------------------------------------------------------------------------
	// the WASTE_ENERGY = T arm — the intake is never gated by the consumer side
	// ---------------------------------------------------------------------------

	@Test
	public void wasteArmConsumesIntoTheVoidWithoutAnAcceptingNeighbor() {
		GT6LaserConverterBlockEntity tLaser = laser(32, 16);
		CountingSink tSink = new CountingSink(TD.Energy.LU);
		tSink.rejecting = true; // nothing out there takes LU
		tLaser.setAdjacencyOverrideForTest(adjacencyAt(tSink, FRONT));
		assertEquals(1, tLaser.doEnergyInjection(TD.Energy.EU, BACK, 32, 1, true), "the EU intake rides regardless");
		tLaser.onTick(100, true);
		assertEquals(0, tSink.packets, "nothing emitted — no consumer");
		assertEquals(0, tLaser.mLastOut, "the accounting out stays 0");
		assertEquals(32, tLaser.mLastIn, "the intake PAID (waste=T: the input side is not refunded)");
		assertEquals(0, tLaser.mStorage, "the vent cleared the bucket");
		assertFalse(tLaser.mActive, "no emission, no activity");
	}

	// ---------------------------------------------------------------------------
	// the negative-sign conjunct folds on BOTH families (LU ∉ ALL_NEGATIVE_ALLOWED)
	// ---------------------------------------------------------------------------

	@Test
	public void negativeInputFoldsToPositivePacketsOnBothFamilies() {
		GT6LaserConverterBlockEntity tLaser = laser(32, 16);
		CountingSink tSink = new CountingSink(TD.Energy.LU);
		tSink.recordSize = true;
		tLaser.setAdjacencyOverrideForTest(adjacencyAt(tSink, FRONT));
		assertEquals(1, tLaser.doEnergyInjection(TD.Energy.EU, BACK, -32, 1, true), "the negative EU packet enters");
		tLaser.onTick(100, true);
		assertEquals(16, tSink.lastSize, "the LU packet size is positive (Base10 :121 second conjunct fails)");

		GT6LaserConverterBlockEntity tAbsorber = absorber(32, 16);
		CountingSink tEuSink = new CountingSink(TD.Energy.EU);
		tEuSink.recordSize = true;
		tAbsorber.setAdjacencyOverrideForTest(adjacencyAt(tEuSink, FRONT));
		assertEquals(1, tAbsorber.doEnergyInjection(TD.Energy.LU, BACK, -32, 1, true), "the negative LU packet enters the absorber");
		tAbsorber.onTick(100, true);
		assertEquals(16, tEuSink.lastSize, "the EU packet size is positive likewise");
	}

	// ---------------------------------------------------------------------------
	// the accounting pair round-trip (the RCON live metering face)
	// ---------------------------------------------------------------------------

	@Test
	public void accountingPairSurvivesASaveLoadRoundTrip() {
		GT6LaserConverterBlockEntity tAbsorber = absorber(32, 16);
		CountingSink tSink = new CountingSink(TD.Energy.EU);
		tAbsorber.setAdjacencyOverrideForTest(adjacencyAt(tSink, FRONT));
		tAbsorber.doEnergyInjection(TD.Energy.LU, BACK, 32, 1, true);
		tAbsorber.onTick(100, true);
		assertEquals(32, tAbsorber.mLastIn);
		assertEquals(16, tAbsorber.mLastOut);
		CompoundTag tNBT = new CompoundTag();
		tAbsorber.saveAdditional(tNBT);
		assertTrue(tNBT.contains(GT6LaserConverterBlockEntity.NBT_LAST_IN), "gt.last_in persisted");
		assertTrue(tNBT.contains(GT6LaserConverterBlockEntity.NBT_LAST_OUT), "gt.last_out persisted");
		GT6LaserConverterBlockEntity tRestored = absorber(32, 16);
		tRestored.load(tNBT);
		assertEquals(32, tRestored.mLastIn, "the intake survived");
		assertEquals(16, tRestored.mLastOut, "the emission survived");
		tRestored.resetAccounting();
		assertEquals(0, tRestored.mLastIn + tRestored.mLastOut, "the reset arm zeroes the pair");
	}

	// ---------------------------------------------------------------------------
	// the counting sink (the bridge harness's typed form)
	// ---------------------------------------------------------------------------

	/** A sink that accepts exactly one energy type on ONE side, counting the whole packets. */
	static class CountingSink implements ITileEntityEnergy {
		final TagData mType;
		long packets = 0, totalMass = 0, lastSize = 0;
		boolean rejecting = false;
		boolean recordSize = false;

		CountingSink(TagData aType) {
			mType = aType;
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == mType;
		}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {
			return java.util.List.of(mType);
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return !rejecting && isEnergyType(aEnergyType, aSide, false);
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

	/** The adjacency seam pinning the neighbor at ONE side (the harness form). */
	static IEnergyAdjacency adjacencyAt(ITileEntityEnergy aReceiver, byte aSideOfEmitter) {
		return aEmitterSide -> {
			if (aEmitterSide != aSideOfEmitter) return null;
			Direction tDir = Direction.from3DDataValue(aSideOfEmitter);
			byte tOpposite = (byte) tDir.getOpposite().get3DDataValue();
			return new EnergyTarget(aReceiver, tOpposite);
		};
	}
}
