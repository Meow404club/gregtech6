package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.CS;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyBridge;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The FE→EU converter core offline tests (task p28-b-fe-converter-machine) — the
 * EnergyBridgeTest posture (hand-verified math tables) over the ULV machine: the push
 * intake (ratio floor + capacity clamp), the pull intake (the root
 * {@code EnergyBridge.extractFe} whole-packet math driven through the seam, the hostile
 * source remainder staying in the source), the emit arm (one packet per tick = the 8 EU/t
 * ceiling, only whole packets, only the USED packets deducted), the upstream overload
 * ladder (the 2-tick chunkload grace, then the suspended explosion) and the NBT round
 * trip. The GT face gate rides the pure-source shape (the GTEnergySourceBlockEntityTest
 * family).
 *
 * <p>The FE platform capability itself cannot class-init offline (the
 * ForgeCapabilities:457 record) — the wrapper delegates to {@code pushFe}, so the tests
 * drive the same math the capability serves; the wrapper's canExtract=false and the level
 * capability wiring are RCON-covered.
 */
public class GT6FeConverterBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6FeConverterBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GT6FeConverterBlockEntity>[] tHolder = (BlockEntityType<GT6FeConverterBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6FeConverterBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/**
	 * A synthetic FE source with a countdown store — the {@code storage::extractEnergy}
	 * lambda shape the BE adapts on the live legs (the pull seam doubles as the platform
	 * face reference).
	 */
	private static EnergyBridge.IFESource countdownSource(int aInitial) {
		int[] tStore = {aInitial};
		return (aAmount, aSimulate) -> {
			int tTake = Math.min(aAmount, tStore[0]);
			if (!aSimulate) tStore[0] -= tTake;
			return tTake;
		};
	}

	/** The hostile source: the simulate answer lies about the extract answer (the p28-a 敌意源). */
	private static EnergyBridge.IFESource hostileSource() {
		return (aAmount, aSimulate) -> aSimulate ? Math.min(aAmount, 30) : Math.min(aAmount, 1);
	}

	/** Counting EU sink with a per-call acceptance cap — the fake consumer (the emit-face receiver). */
	public static class LimitedSink extends BlockEntity implements ITileEntityEnergy {
		public long packetsBooked = 0, wattage = 0, lastSize = -1, lastAmount = -1;
		private final long mAcceptCap;

		static final BlockEntityType<LimitedSink> FAKE_TYPE =
				BlockEntityType.Builder.of((aPos, aState) -> new LimitedSink(aPos, Long.MAX_VALUE), Blocks.STONE).build(null);

		public LimitedSink(BlockPos aPos, long aAcceptCap) {
			super(FAKE_TYPE, aPos, Blocks.STONE.defaultBlockState());
			mAcceptCap = aAcceptCap;
		}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				long tAccepted = Math.min(aAmount, mAcceptCap);
				if (aDoInject && tAccepted > 0) {
					packetsBooked += tAccepted;
					wattage += Math.abs(aSize * tAccepted);
					lastSize = aSize;
					lastAmount = tAccepted;
				}
				return tAccepted;
			}
			return 0;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {return aEnergyType == TD.Energy.EU;}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return aEnergyType == TD.Energy.EU;}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.EU.AS_LIST;}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}
		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}
		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}
		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}
		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}
		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}
		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}
		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return 0;}
		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	/** The one-sided adjacency: only side 3 resolves, to the sink's back (the GTEnergySource fixture shape). */
	private static IEnergyAdjacency southOf(BlockEntity aSink) {
		return aSide -> aSide == 3 ? new EnergyTarget(aSink, (byte)2) : null;
	}

	// ---------------------------------------------------------------------------
	// sizing (the ULV balance ruling: 8 EU x 1 A, buffer 512 FE)
	// ---------------------------------------------------------------------------

	@Test
	public void sizingFollowsTheUlvRuling() {
		GT6FeConverterBlockEntity tConverter = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(8, tConverter.mVoltage, "the ULV ruling: V[0] = 8 EU, the only tier");
		assertEquals(1, GT6FeConverterBlockEntity.AMPS, "one amp — more would sum back to LV throughput");
		assertEquals(32, tConverter.packetFe(), "one packet = 8 EU x 4 FE");
		assertEquals(512, tConverter.capacityFe(), "the GTCEu capacitor: 8 x 1 x 16 ticks x 4 = 512 FE");
		assertEquals("fe_converter", tConverter.getTileEntityName(), "the BET registry path mirrors the name");
	}

	// ---------------------------------------------------------------------------
	// the push intake (the ratio floor + the capacity clamp; ConverterTrait :108 form)
	// ---------------------------------------------------------------------------

	@Test
	public void pushFloorsAtTheRatioAndClampsAtCapacity() {
		GT6FeConverterBlockEntity tConverter = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(0, tConverter.pushFe(0, false), "the zero guard");
		assertEquals(0, tConverter.pushFe(-5, false), "the negative guard");
		assertEquals(0, tConverter.pushFe(3, false), "3 FE is below the 4:1 ratio — nothing is accepted");
		assertEquals(0, tConverter.mBufferFe, "the rejected tail stayed in the cable");
		assertEquals(28, tConverter.pushFe(30, false), "30 FE floors to 28 (whole EU units)");
		assertEquals(28, tConverter.mBufferFe, "the buffer holds exactly the accepted FE");
		assertEquals(0, tConverter.pushFe(1, false), "a 1 FE tail is refused (1 % 4 alignment)");
		assertEquals(28, tConverter.mBufferFe);
		// simulate must not move the buffer
		assertEquals(100, tConverter.pushFe(100, true), "the simulated answer mirrors the real acceptance (100 is ratio-aligned)");
		assertEquals(28, tConverter.mBufferFe, "simulate never charges the buffer");
		// the capacity clamp: 28 + 500 = 528 > 512 → only 484 fits (484 % 4 == 0)
		assertEquals(484, tConverter.pushFe(500, false), "the clamp takes only the capacity headroom, ratio-aligned");
		assertEquals(512, tConverter.mBufferFe, "the buffer is exactly full");
		assertEquals(0, tConverter.pushFe(4, false), "a full capacitor accepts nothing");
	}

	// ---------------------------------------------------------------------------
	// the pull intake (the root EnergyBridge.extractFe math through the seam)
	// ---------------------------------------------------------------------------

	@Test
	public void pullTakesWholePacketTrainsThroughTheBridgeMath() {
		GT6FeConverterBlockEntity tConverter = sType.create(POS, Blocks.STONE.defaultBlockState());
		tConverter.setPullSourceOverride(countdownSource(130)); // 4 packets + a 2 FE tail
		tConverter.pullOnce();
		assertEquals(32, tConverter.mBufferFe, "one packet (1 A cap) landed as 32 FE");
		// the tail case: drain 4 ticks off one source
		GT6FeConverterBlockEntity tDrainer = sType.create(POS, Blocks.STONE.defaultBlockState());
		EnergyBridge.IFESource tSource = countdownSource(130);
		tDrainer.setPullSourceOverride(tSource);
		for (int i = 0; i < 4; i++) tDrainer.pullOnce();
		assertEquals(128, tDrainer.mBufferFe, "four packets pulled (4 x 32 FE)");
		// the countdown store is closed over per lambda — re-read it through a fresh pull
		GT6FeConverterBlockEntity tProbe = sType.create(POS, Blocks.STONE.defaultBlockState());
		tProbe.setPullSourceOverride(tSource);
		tProbe.pullOnce();
		assertEquals(0, tProbe.mBufferFe, "the 2 FE tail is below one packet — nothing more is pulled (the floor stays in the source)");
	}

	@Test
	public void pullRefusesEmptyHostileAndTightSources() {
		GT6FeConverterBlockEntity tConverter = sType.create(POS, Blocks.STONE.defaultBlockState());
		// the empty source
		tConverter.setPullSourceOverride(countdownSource(0));
		tConverter.pullOnce();
		assertEquals(0, tConverter.mBufferFe, "an empty source yields nothing");
		// the hostile source: the simulate answer (30) floors to ZERO whole packets — no real call at all
		tConverter.setPullSourceOverride(hostileSource());
		tConverter.pullOnce();
		assertEquals(0, tConverter.mBufferFe, "a source answering 30 FE to a 32 FE request yields 0 packets (floor: 30 - 30 % 32 = 0)");
		// the space guard: room for less than one packet → no pull (never strand a packet against the capacity)
		tConverter.mBufferFe = tConverter.capacityFe() - 16;
		tConverter.setPullSourceOverride(countdownSource(1000));
		tConverter.pullOnce();
		assertEquals(tConverter.capacityFe() - 16, tConverter.mBufferFe, "16 FE of room is below one packet — the pull waits");
	}

	// ---------------------------------------------------------------------------
	// the emit arm (the min gate + the 只扣实收 deduction + the 1-packet/tick ceiling)
	// ---------------------------------------------------------------------------

	@Test
	public void emitBooksOnePacketPerTickAndDeductsOnlyUsed() {
		GT6FeConverterBlockEntity tConverter = sType.create(POS, Blocks.STONE.defaultBlockState());
		LimitedSink tSink = new LimitedSink(POS.offset(0, 0, 1), Long.MAX_VALUE);
		tConverter.setAdjacencyOverride(southOf(tSink));
		tConverter.mBufferFe = 512; // full: 16 packets

		tConverter.emitOnce();
		assertEquals(1, tSink.packetsBooked, "the 1 A cap: exactly ONE packet per tick, even from a full buffer");
		assertEquals(8, tSink.lastSize, "the packet size is the ULV voltage");
		assertEquals(480, tConverter.mBufferFe, "only the used packet is deducted (32 FE)");

		for (int i = 0; i < 15; i++) tConverter.emitOnce();
		assertEquals(0, tConverter.mBufferFe, "16 emits drain the full capacitor exactly");
		assertEquals(16, tSink.packetsBooked, "the sink booked all 16 packets = 128 EU");
		assertEquals(128, tSink.wattage, "the wattage ledger = 16 x 8 EU");

		// the min gate: a partial packet never emits
		tConverter.mBufferFe = 31;
		tConverter.emitOnce();
		assertEquals(31, tConverter.mBufferFe, "31 FE is below one whole packet — the emit gate stays shut");
		assertEquals(16, tSink.packetsBooked, "nothing was booked");

		// the 只扣实收 arm: a refusing receiver books and deducts nothing
		LimitedSink tDead = new LimitedSink(POS.offset(1, 0, 1), 0) {
			@Override
			public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}
		};
		tConverter.mBufferFe = 64;
		tConverter.setAdjacencyOverride(southOf(tDead));
		tConverter.emitOnce();
		assertEquals(64, tConverter.mBufferFe, "an unreachable network deducts nothing (upstream :87)");
	}

	@Test
	public void onTickRunsTheWholeCoreAndTheCeilingHolds() {
		GT6FeConverterBlockEntity tConverter = sType.create(POS, Blocks.STONE.defaultBlockState());
		LimitedSink tSink = new LimitedSink(POS.offset(0, 0, 1), Long.MAX_VALUE);
		tConverter.setAdjacencyOverride(southOf(tSink));
		tConverter.setPullSourceOverride(countdownSource(10000)); // an ample source
		// a level-less fixture takes the SERVER branch (the GTEnergySource harness note)
		for (int i = 0; i < 30; i++) tConverter.updateEntity();
		// the pull refill is capped at 1 packet/tick and the emit at 1 packet/tick, so the
		// buffer can never exceed one packet above the intake: the throughput ceiling holds
		assertTrue(tConverter.mBufferFe <= GT6FeConverterBlockEntity.AMPS * tConverter.packetFe(),
				"the buffer never grows beyond one packet of headroom (the 8 EU/t ceiling)");
		assertEquals(30, tSink.packetsBooked, "30 ticks moved exactly 30 packets = 240 EU (8 EU/t live)");
		assertFalse(tConverter.isDead(), "a healthy core never dies");
	}

	// ---------------------------------------------------------------------------
	// the overload ladder (the upstream doConversion :68-77 shape)
	// ---------------------------------------------------------------------------

	@Test
	public void overloadGraceDumpsThenTheExplosionArms() {
		GT6FeConverterBlockEntity tConverter = sType.create(POS, Blocks.STONE.defaultBlockState());
		// dispatcher order note: updateEntityBase increments mTimer BEFORE onTick, so the
		// passes see onTick(1) / onTick(2) / onTick(3) — the grace covers the first TWO.
		tConverter.mBufferFe = 100000; // only reachable through a foreign writer (the chunkload scenario)
		tConverter.updateEntity(); // onTick(1) — the grace
		assertEquals(0, tConverter.mBufferFe, "the grace window dumps the buffer (upstream :72-75)");
		assertEquals(0, tConverter.mExplosionStrength, "no explosion during the grace");
		tConverter.mBufferFe = 100000;
		tConverter.updateEntity(); // onTick(2) — the last grace tick
		assertEquals(0, tConverter.mBufferFe);
		assertEquals(0, tConverter.mExplosionStrength);
		tConverter.mBufferFe = 100000;
		tConverter.updateEntity(); // onTick(3) — past the grace: the overcharge arms
		assertTrue(tConverter.mExplosionStrength > 0, "past the grace the suspended explosion is armed (tierMax of the buffer)");
		assertEquals(0, tConverter.mBufferFe, "the buffer clears with the overcharge");
		tConverter.updateEntity(); // the next tick's core consumes the suspended explosion
		assertTrue(tConverter.isDead(), "the machine is dead after the explosion consumes");
	}

	// ---------------------------------------------------------------------------
	// the NBT round trip + the GT face gate
	// ---------------------------------------------------------------------------

	@Test
	public void nbtRoundTripsTheBuffer() {
		GT6FeConverterBlockEntity tConverter = sType.create(POS, Blocks.STONE.defaultBlockState());
		tConverter.mBufferFe = 96;
		CompoundTag tSaved = tConverter.saveWithoutMetadata();
		assertTrue(tSaved.contains("fe"), "the card key fe");
		assertEquals(96, tSaved.getLong("fe"));
		assertEquals("fe_converter", tSaved.getString("te_name"), "the base te_name key rides along");

		GT6FeConverterBlockEntity tBack = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertEquals(96, tBack.mBufferFe, "the buffer survives the round trip");

		// a negative persisted value clamps to zero (the hostile-writer guard)
		CompoundTag tHostile = tConverter.saveWithoutMetadata();
		tHostile.putLong("fe", -50);
		tBack.load(tHostile);
		assertEquals(0, tBack.mBufferFe, "a negative persisted buffer clamps to 0");
	}

	@Test
	public void theGtFaceIsAPureEuSource() {
		GT6FeConverterBlockEntity tConverter = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertTrue(tConverter.isEnergyType(TD.Energy.EU, (byte)0, true), "the emitting probe locks EU");
		assertFalse(tConverter.isEnergyType(TD.Energy.EU, (byte)0, false), "a pure source accepts nothing on the GT face");
		assertFalse(tConverter.isEnergyType(TD.Energy.RU, (byte)0, true), "non-EU rejected");
		assertFalse(tConverter.isEnergyAcceptingFrom(TD.Energy.EU, (byte)3, false), "the accepting probe is constant false");
		assertTrue(tConverter.isEnergyEmittingTo(TD.Energy.EU, (byte)3, false), "the real emit probe is open (no mode gate)");
		assertTrue(tConverter.isEnergyEmittingTo(TD.Energy.EU, (byte)3, true), "the theoretical probe is open (the wire stays connected)");
		assertEquals(TD.Energy.EU.AS_LIST, tConverter.getEnergyTypes((byte)6));
		assertEquals(8, tConverter.getEnergySizeOutputRecommended(TD.Energy.EU, (byte)0), "the emitted packet size");
		// the Root default band = the upstream converter :76-77 form (min = rec/2, max = rec*2)
		assertEquals(4, tConverter.getEnergySizeOutputMin(TD.Energy.EU, (byte)0));
		assertEquals(16, tConverter.getEnergySizeOutputMax(TD.Energy.EU, (byte)0));
		assertEquals(0, tConverter.doEnergyInjection(TD.Energy.EU, (byte)0, 32, 1, true), "the GT face never injects");
		assertEquals(0, tConverter.doEnergyExtraction(TD.Energy.EU, (byte)0, 32, 1, true), "the GT face never extracts");
		// the ratio constant is the bridge's own
		assertEquals(4, CS.RF_PER_EU, "the lossless 4:1");
	}
}
