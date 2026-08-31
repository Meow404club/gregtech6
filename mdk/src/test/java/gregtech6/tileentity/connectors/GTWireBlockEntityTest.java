package gregtech6.tileentity.connectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.HashSetNoNulls;
import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GTWireBlockEntity offline tests (task p7-d2-cable spec ⑧): the connection-mask surface,
 * the EU face family behind the connection gate, the size band, the NBT emptiness (the
 * upstream writeToNBT2 :121-123 empty body — only the base mConnections persists) and the
 * pure {@link GTWireBlockEntity#addToEnergyTransferred} overload logic (over-voltage /
 * over-current / the burn saturation cap). The full transfer recursion needs live
 * neighbours — the card prescribes it as the RCON command acceptance (/gt6wire inject
 * chains); offline only the two :171 gates of transferElectricity are reachable
 * (mTimer&lt;1 always offline, |v|&lt;=mLoss forced via the timer hook).
 */
public class GTWireBlockEntityTest extends GTOfflineTestBase {

	/** Test hook: mTimer lives protected in gregtech6.tileentity, this subclass lifts it for the :171 gate. */
	public static class TestWire extends GTWireBlockEntity {
		public TestWire(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		public void forceTimer(long aTimer) {
			mTimer = aTimer;
		}
	}

	/** Fake EU sink for the canConnect probe (upstream EnergyCompat.canConnectElectricity :201). */
	public static class FakeSink extends BlockEntity implements ITileEntityEnergy {
		public byte lastProbedSide = -1;

		public FakeSink(BlockPos aPos) {
			super(null, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			lastProbedSide = aSide;
			return aEnergyType == TD.Energy.EU;
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return aEnergyType == TD.Energy.EU;}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.EU.AS_LIST;}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {return 0;}

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

	/** Plain non-energy BE — must never connect. */
	public static class PlainBox extends BlockEntity {
		public PlainBox(BlockPos aPos) {
			super(null, aPos, Blocks.STONE.defaultBlockState());
		}
	}

	static BlockEntityType<TestWire> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<TestWire>[] tHolder = (BlockEntityType<TestWire>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TestWire(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	// ---------------------------------------------------------------------------
	// defaults and identity (upstream :64-65/:243/:205-206)
	// ---------------------------------------------------------------------------

	@Test
	public void defaultsFollowTheUpstreamFields() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(32, tWire.mVoltage, "upstream :64 default mVoltage = 32");
		assertEquals(1, tWire.mAmperage, "upstream :64 default mAmperage = 1");
		assertEquals(1, tWire.mLoss, "upstream :64 default mLoss = 1");
		assertEquals(0, tWire.mTransferredAmperes, "upstream :64 default mTransferredAmperes");
		assertEquals(0, tWire.mTransferredWattage, "upstream :64 default mTransferredWattage");
		assertEquals(0, tWire.mWattageLast, "upstream :64 default mWattageLast");
		assertEquals(0, tWire.mBurnCounter, "upstream :65 default mBurnCounter");
	}

	@Test
	public void connectorAndEnergyTypesAreWireElectric() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(TD.Connectors.WIRE_ELECTRIC.AS_LIST, tWire.getConnectorTypes((byte)0), "upstream :243");
		// the 09Connector intersection gate at wire level: wires shake hands, wires and pipes don't
		assertTrue(TileEntityBase09Connector.haveOneCommonElement(
				tWire.getConnectorTypes((byte)0), TD.Connectors.WIRE_ELECTRIC.AS_LIST), "same type connects (upstream :118)");
		assertFalse(TileEntityBase09Connector.haveOneCommonElement(
				tWire.getConnectorTypes((byte)0), TD.Connectors.PIPE_FLUID.AS_LIST), "wire never shakes hands with a pipe");

		assertTrue(tWire.isEnergyType(TD.Energy.EU, (byte)0, true), "upstream :205 — EU only");
		assertFalse(tWire.isEnergyType(TD.Energy.RU, (byte)0, true), "upstream :205 — non-EU rejected");
		assertEquals(TD.Energy.EU.AS_LIST, tWire.getEnergyTypes((byte)6), "upstream :206");
	}

	// ---------------------------------------------------------------------------
	// the EU faces ride the connection mask (upstream :208-230)
	// ---------------------------------------------------------------------------

	@Test
	public void energyFacesAreConnectionGated() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertFalse(tWire.isEnergyEmittingTo(TD.Energy.EU, (byte)2, false), "unconnected side never emits");
		assertFalse(tWire.isEnergyAcceptingFrom(TD.Energy.EU, (byte)2, false), "unconnected side never accepts");
		assertFalse(tWire.isEnergyAcceptingFrom(TD.Energy.RU, (byte)2, false), "non-EU never accepted");

		tWire.setConnectionBit((byte)2); // SBIT[2] = 4
		assertTrue(tWire.isEnergyAcceptingFrom(TD.Energy.EU, (byte)2, false), "connected side accepts (upstream :209)");
		assertTrue(tWire.isEnergyEmittingTo(TD.Energy.EU, (byte)2, false), "connected side emits (upstream :208)");
		assertFalse(tWire.isEnergyAcceptingFrom(TD.Energy.EU, (byte)3, false), "neighbouring sides stay gated");
		assertTrue(tWire.canEmitEnergyTo((byte)2), "upstream :229 — canEmitEnergyTo == connected");
		assertTrue(tWire.canAcceptEnergyFrom((byte)2), "upstream :230 — canAcceptEnergyFrom == connected");
		assertEquals(4, tWire.getConnections());
	}

	@Test
	public void energySizesCarryTheVoltageBand() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(32, tWire.getEnergySizeOutputRecommended(TD.Energy.EU, (byte)0), "upstream :212");
		assertEquals(32, tWire.getEnergySizeOutputMax(TD.Energy.EU, (byte)0), "upstream :214");
		assertEquals(32, tWire.getEnergySizeInputRecommended(TD.Energy.EU, (byte)0), "upstream :215");
		assertEquals(32, tWire.getEnergySizeInputMax(TD.Energy.EU, (byte)0), "upstream :217");
		assertEquals(0, tWire.getEnergySizeOutputMin(TD.Energy.EU, (byte)0), "upstream :213");
		assertEquals(0, tWire.getEnergySizeInputMin(TD.Energy.EU, (byte)0), "upstream :216");
		assertEquals(0, tWire.getEnergyDemanded(TD.Energy.EU, (byte)0, 32), "the card — demanded 0");
		assertEquals(0, tWire.getEnergyOffered(TD.Energy.EU, (byte)0, 32), "the card — offered 0");
		assertEquals(0, tWire.doEnergyExtraction(TD.Energy.EU, (byte)0, 32, 1, true), "upstream :210 — no extraction");
	}

	// ---------------------------------------------------------------------------
	// canConnect (upstream :201 specialised per the card)
	// ---------------------------------------------------------------------------

	@Test
	public void canConnectTargetsEnergyAcceptorsOnTheBackSide() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		FakeSink tSink = new FakeSink(POS.offset(0, 0, 1));
		// side 2 = NORTH, the neighbour side facing the wire is 3 (SOUTH) — the "opposite side"
		assertTrue(tWire.canConnect((byte)2, tSink), "an EU acceptor connects (upstream :201)");
		assertEquals(3, tSink.lastProbedSide, "the probe uses the neighbour's back side (the opposite side)");
		assertFalse(tWire.canConnect((byte)2, new PlainBox(POS.offset(0, 0, 1))), "non-energy BEs never connect");
		assertFalse(tWire.canConnect((byte)2, null), "null neighbours never connect");
		// a wire BE IS an ITileEntityEnergy: acceptance rides its own connection mask
		assertFalse(tWire.canConnect((byte)2, sType.create(POS.offset(0, 0, 1), Blocks.STONE.defaultBlockState())),
				"an unconnected wire accepts nothing (mask 0)");
	}

	// ---------------------------------------------------------------------------
	// transferElectricity gates offline + doEnergyInjection (upstream :171/:211)
	// ---------------------------------------------------------------------------

	@Test
	public void transferElectricityDiesOnTheUpstreamGates() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		HashSetNoNulls<BlockEntity> tPassed = new HashSetNoNulls<>(false, tWire);
		// mTimer < 1 — an offline BE never ticked, so every packet dies at the :171 first gate
		assertEquals(0, tWire.transferElectricity((byte)2, 32, 1, -1, tPassed), "upstream :171 — unticked wires carry nothing");
		tWire.forceTimer(1);
		assertEquals(0, tWire.transferElectricity((byte)2, 0, 1, -1, tPassed), "upstream :171 — |0| <= mLoss");
		assertEquals(0, tWire.transferElectricity((byte)2, 1, 1, -1, tPassed), "upstream :171 — |1| <= mLoss = 1");
		assertEquals(0, tWire.transferElectricity((byte)2, -1, 1, -1, tPassed), "upstream :171 — the negative form");
		assertEquals(0, tWire.mTransferredWattage + tWire.mTransferredAmperes, "no packet got past the gates, nothing booked");
	}

	@Test
	public void doEnergyInjectionReturnsZeroOfflineEvenWhenConnected() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		tWire.mConnections = 63;
		assertEquals(0, tWire.doEnergyInjection(TD.Energy.EU, (byte)0, 32, 1, true), "offline mTimer=0 — the :171 gate ends the recursion");
		assertEquals(1, tWire.doEnergyInjection(TD.Energy.EU, (byte)0, 32, 1, false), "the simulate form reports the full amount (:211)");
		assertEquals(0, tWire.doEnergyInjection(TD.Energy.EU, (byte)0, 0, 1, false), "aSize == 0 rejects (:211 first clause)");
	}

	// ---------------------------------------------------------------------------
	// addToEnergyTransferred pure logic (upstream :191-199)
	// ---------------------------------------------------------------------------

	@Test
	public void addToEnergyTransferredBooksNormalPackets() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertTrue(tWire.addToEnergyTransferred(30, 1), "30 EU x 1 A fits the 32/1 rating");
		assertEquals(1, tWire.mTransferredAmperes);
		assertEquals(30, tWire.mTransferredWattage, "upstream :193 — |v * a|");
		assertEquals(0, tWire.mBurnCounter, "within rating, no strike");
		assertTrue(tWire.addToEnergyTransferred(-32, 0), "the voltage boundary |32| == mVoltage passes (zero amperes adds no wattage)");
		assertEquals(30, tWire.mTransferredWattage);
	}

	@Test
	public void overVoltageStrikesTheBurnCounter() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertFalse(tWire.addToEnergyTransferred(64, 1), "|64| > 32 — the packet reports fully consumed (upstream :196)");
		assertEquals(64, tWire.mTransferredWattage, "upstream :193 — the wattage books even on the strike");
		assertEquals(1, tWire.mBurnCounter, "upstream :195 — one strike");
		// the accumulated amperes persist inside the tick window (upstream :194 reads the
		// accumulator): a second packet in the same window strikes again via over-current
		assertFalse(tWire.addToEnergyTransferred(30, 1), "ampere 2 in the same window still exceeds mAmperage = 1");
		assertEquals(2, tWire.mBurnCounter, "one over-voltage strike plus one over-current strike");
		// the onTick window end (:153-155) clears the accumulators — then the wire is fine again
		tWire.mTransferredAmperes = 0;
		tWire.mTransferredWattage = 0;
		assertTrue(tWire.addToEnergyTransferred(30, 1), "a fresh window without strikes");
		assertEquals(2, tWire.mBurnCounter, "no healing outside the %512==2 window (upstream :152)");
	}

	@Test
	public void overCurrentStrikesOnAccumulation() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertTrue(tWire.addToEnergyTransferred(30, 1), "ampere 1 of 1 — fine");
		assertFalse(tWire.addToEnergyTransferred(30, 1), "ampere 2 exceeds mAmperage = 1");
		assertEquals(2, tWire.mTransferredAmperes);
		assertEquals(1, tWire.mBurnCounter, "upstream :194 — the accumulated amperes trigger, not the single packet");
	}

	@Test
	public void burnCounterSaturatesAtSixteen() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		for (int i = 0; i < 16; i++) {
			assertFalse(tWire.addToEnergyTransferred(64, 1), "over-voltage strike " + i);
			assertEquals(i + 1, tWire.mBurnCounter, "upstream :195 — the cap guard mBurnCounter < 16");
		}
		assertFalse(tWire.addToEnergyTransferred(64, 1), "still failing past the cap");
		assertEquals(16, tWire.mBurnCounter, "upstream :195 — saturated at exactly 16, the setToFire threshold");
	}

	// ---------------------------------------------------------------------------
	// NBT: the upstream writeToNBT2 :121-123 empty body — only the base connections
	// ---------------------------------------------------------------------------

	@Test
	public void nbtPersistsConnectionsOnly() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		tWire.setConnectionBit((byte)3);
		tWire.mTransferredWattage = 999;
		tWire.mTransferredAmperes = 42;
		tWire.mBurnCounter = 9;
		tWire.mWattageLast = 555;

		CompoundTag tSaved = tWire.saveWithoutMetadata();
		assertEquals(8, tWire.getConnections(), "SBIT[3]");
		assertTrue(tSaved.contains("connections"), "the base 09Connector persistence");
		assertEquals(8, tSaved.getByte("connections"));
		// the zero-new-NBT contract: no wire-specific key may appear (the transfer state is
		// transient, the ratings travel on the block carrier)
		for (String tKey : tSaved.getAllKeys()) {
			assertTrue(tKey.equals("connections") || tKey.equals("id") || tKey.equals("x") || tKey.equals("y")
					|| tKey.equals("z") || tKey.equals("te_name"),
					"unexpected persisted key '" + tKey + "' — the wire BE must add no NBT beyond the base");
		}

		// round trip: the connections survive, the transient transfer state resets
		TestWire tBack = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertEquals(8, tBack.getConnections());
		assertEquals(0, tBack.mBurnCounter, "the strike counter is transient (upstream :121-123 empty body)");
		assertEquals(0, tBack.mTransferredWattage, "the transfer state is transient");
		assertEquals(32, tBack.mVoltage, "the ratings come from the carrier, not NBT");
	}

	// ---------------------------------------------------------------------------
	// sync gate regression over the connection changes (the 09Connector pair)
	// ---------------------------------------------------------------------------

	@Test
	public void connectionChangesArmTheClientSync() {
		TestWire tWire = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertFalse(tWire.onTickCheck(1));
		tWire.setConnectionBit((byte)5);
		assertTrue(tWire.onTickCheck(2), "the 09Connector change-detection gate drives the mask sync");
		tWire.onTickResetChecks(2, true);
		assertFalse(tWire.onTickCheck(3));
	}
}
