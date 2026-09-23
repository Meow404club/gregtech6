package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GT6LongDistanceTransformers;
import gregtech6.registry.GT6LongDistWires;
import gregtech6.registry.GTWireSpecs;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GT6LongDistanceTransformerBlockEntity offline tests (task p35-energy-tail-machines):
 * the row-table pins (the :909-:913 declared subset — FIVE rows, meta ids 10064-10068,
 * NBT_INPUT = NBT_OUTPUT = V[4..8]; the 16 wire metas with the Loader_Blocks:160 tier
 * bytes and the VMAX throughput), the DELEGATE MATH (the doInject :232-259 shape over
 * the offline rig: the throughput burn arm, the distance loss
 * {@code aSize - max(64, mDistance/8)}, the target band gates, the
 * emit-through-the-target's-BACK push :254), and the face sets (FRONT in, BACK out).
 *
 * <p>Offline harness (the transformer test form): the {@code setTargetOverride} seam
 * replaces the wire BFS (no level), the {@code setAdjacencyOverride} seam wires the
 * counting sink behind the TARGET's BACK face (the :254 emitter walk), the
 * {@code mThroughput}/{@code mDistance} fields are set directly (the scan columns).
 */
public class GT6LongDistanceTransformerBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6LongDistanceTransformerBlockEntity> sType;
	static final BlockPos POS = new BlockPos(10, 4, 5);
	static final BlockPos SINK_POS = new BlockPos(10, 4, 3);

	/** The GT6 byte side of NORTH (the default facing; FRONT = input). */
	static final byte FRONT = 2;

	/** The BACK byte (SOUTH = 3 — the output face). */
	static final byte BACK = 3;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GT6LongDistanceTransformerBlockEntity>[] tHolder = (BlockEntityType<GT6LongDistanceTransformerBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6LongDistanceTransformerBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** A fresh tier-4 (EV) endpoint fixture. */
	private static GT6LongDistanceTransformerBlockEntity endpoint() {
		return new GT6LongDistanceTransformerBlockEntity(sType, POS, Blocks.STONE.defaultBlockState(), 4);
	}

	/**
	 * Counting EU sink — the transformer test form: accepts EU from every side, records
	 * every packet (size, amount).
	 */
	public static class EuSink extends BlockEntity implements ITileEntityEnergy {
		public final List<long[]> packets = new ArrayList<>();

		static final BlockEntityType<EuSink> FAKE_TYPE =
				BlockEntityType.Builder.of((aPos, aState) -> new EuSink(aPos), Blocks.STONE).build(null);

		public EuSink(BlockPos aPos) {
			super(FAKE_TYPE, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == TD.Energy.EU;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == TD.Energy.EU;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.EU.AS_LIST;}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) packets.add(new long[] {aSize, aAmount});
				return aAmount;
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

	/** The rig: sender → target → sink-behind-target (the :254 emit walk captured). */
	private static EuSink wire(GT6LongDistanceTransformerBlockEntity aSender, GT6LongDistanceTransformerBlockEntity aTarget) {
		EuSink tSink = new EuSink(SINK_POS);
		aSender.setTargetOverride(aTarget);
		aTarget.setAdjacencyOverride(aSide -> new EnergyTarget(tSink, (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue()));
		return tSink;
	}

	// ---------------------------------------------------------------------------
	// 1. the row-table pins (the :909-:913 declared subset + the wire metas)
	// ---------------------------------------------------------------------------

	@Test
	public void ladderPinsTheUpstreamDeclaredSubset() {
		assertEquals(5, GT6LongDistanceTransformers.ROWS.size(), "the :909-:913 declared row count");
		for (int i = 0; i < 5; i++) {
			GT6LongDistanceTransformers.LDRow tRow = GT6LongDistanceTransformers.ROWS.get(i);
			assertEquals(10064 + i, tRow.metaId(), "row " + i + ": the upstream meta id");
			assertEquals(4 + i, tRow.tier(), "row " + i + ": the ladder index");
			assertEquals(GTWireSpecs.VN[4 + i], tRow.voltageWord(), "row " + i + ": the VN word");
			GT6LongDistanceTransformerBlockEntity tTrans = new GT6LongDistanceTransformerBlockEntity(sType, POS, Blocks.STONE.defaultBlockState(), tRow.tier());
			assertEquals(GTWireSpecs.V[4 + i], tTrans.mInput, "row " + i + ": NBT_INPUT = V[i]");
			assertEquals(GTWireSpecs.V[4 + i], tTrans.mOutput, "row " + i + ": NBT_OUTPUT = V[i] (same-voltage pass-through)");
		}
	}

	@Test
	public void wireTablePinsThe160LoaderLine() {
		assertEquals(16, GT6LongDistWires.ROWS.size(), "the 16 metas");
		int[] tExpectedTiers = {4, 4, 5, 6, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8};
		for (int i = 0; i < 16; i++) {
			assertEquals(i, GT6LongDistWires.ROWS.get(i).meta());
			assertEquals(tExpectedTiers[i], GT6LongDistWires.ROWS.get(i).tier(), "meta " + i + ": the tier byte");
			// the throughput seat = VMAX[tier] (the block's throughput() is a one-line read
			// of this table — the block instance itself is registry-bound, unprobeable offline)
			assertEquals(GTWireSpecs.VMAX[tExpectedTiers[i]], GTWireSpecs.VMAX[GT6LongDistWires.ROWS.get(i).tier()],
					"meta " + i + ": the throughput = VMAX[tier]");
		}
	}

	// ---------------------------------------------------------------------------
	// 2. the delegate math (the doInject :232-259 shape over the offline rig)
	// ---------------------------------------------------------------------------

	@Test
	public void delegatePushesThroughTheTargetWithDistanceLoss() {
		GT6LongDistanceTransformerBlockEntity tSender = endpoint();
		GT6LongDistanceTransformerBlockEntity tTarget = new GT6LongDistanceTransformerBlockEntity(sType, new BlockPos(10, 4, 40), Blocks.STONE.defaultBlockState(), 4);
		EuSink tSink = wire(tSender, tTarget);

		tSender.mThroughput = 4096; // the scan column (the EV wire blob)
		tSender.mDistance = 800; // the hop count: the loss = max(64, 800/8) = 100

		assertEquals(1, tSender.doEnergyInjection(TD.Energy.EU, FRONT, 2048, 1, true), "the V[4] packet accepted");
		assertEquals(1, tSink.packets.size(), "one dispatch behind the target's BACK");
		assertEquals(1948, tSink.packets.get(0)[0], "2048 - the 100 EU distance loss");
		assertEquals(1, tSink.packets.get(0)[1], "one packet");
		assertTrue(tSender.mActive, "the sender reports activity (:255)");
		assertTrue(tTarget.mActive, "the target reports activity too (:255)");
	}

	@Test
	public void oversizeThroughputPacketBurnsTheWiresAndConsumesAll() {
		GT6LongDistanceTransformerBlockEntity tSender = endpoint();
		GT6LongDistanceTransformerBlockEntity tTarget = new GT6LongDistanceTransformerBlockEntity(sType, new BlockPos(10, 4, 40), Blocks.STONE.defaultBlockState(), 4);
		EuSink tSink = wire(tSender, tTarget);
		tSender.mThroughput = 1024; // the wire blob is rated BELOW the packet

		// the V[4] packet 2048 > the throughput 1024 → the burn-rescan (the offline scan
		// short-circuits) + consumed-all (:242-243)
		assertEquals(2048, tSender.doEnergyInjection(TD.Energy.EU, FRONT, 2048, 2048, true), "the oversize-throughput offer counts as used");
		assertTrue(tSink.packets.isEmpty(), "nothing reaches the sink");
	}

	@Test
	public void shortLineLossFloorsAt64() {
		GT6LongDistanceTransformerBlockEntity tSender = endpoint();
		GT6LongDistanceTransformerBlockEntity tTarget = new GT6LongDistanceTransformerBlockEntity(sType, new BlockPos(10, 4, 40), Blocks.STONE.defaultBlockState(), 4);
		EuSink tSink = wire(tSender, tTarget);
		tSender.mThroughput = 4096;
		tSender.mDistance = 8; // max(64, 8/8) = 64 — the floor

		assertEquals(1, tSender.doEnergyInjection(TD.Energy.EU, FRONT, 2048, 1, true));
		assertEquals(1984, tSink.packets.get(0)[0], "2048 - 64");
	}

	@Test
	public void noTargetRefusesThePacket() {
		GT6LongDistanceTransformerBlockEntity tSender = endpoint();
		EuSink tSink = new EuSink(SINK_POS);
		tSender.setAdjacencyOverride(aSide -> new EnergyTarget(tSink, aSide));
		tSender.mThroughput = 4096;

		// checkTarget without a target override: the offline scan short-circuits with the
		// self-seed → no target → 0 (:240)
		assertEquals(0, tSender.doEnergyInjection(TD.Energy.EU, FRONT, 2048, 1, true), "no link, no transfer");
		assertTrue(tSink.packets.isEmpty());
	}

	// ---------------------------------------------------------------------------
	// 3. the faces + the stop (the :261-262/:272-273 shape)
	// ---------------------------------------------------------------------------

	@Test
	public void frontIsInputAndBackIsOutput() {
		GT6LongDistanceTransformerBlockEntity tTrans = endpoint();
		assertTrue(tTrans.isInput(FRONT));
		assertTrue(tTrans.isOutput(BACK));
		assertFalse(tTrans.isInput(BACK));
		assertFalse(tTrans.isOutput(FRONT));
		assertTrue(tTrans.isEnergyAcceptingFrom(TD.Energy.EU, FRONT, true), "the theoretical input face");
		assertFalse(tTrans.isEnergyAcceptingFrom(TD.Energy.EU, BACK, true), "the BACK is not an input");
		assertTrue(tTrans.isEnergyEmittingTo(TD.Energy.EU, BACK, true), "the output face");
	}

	@Test
	public void stoppedEndpointAcceptsNothing() {
		GT6LongDistanceTransformerBlockEntity tTrans = endpoint();
		tTrans.mStopped = true;
		assertEquals(0, tTrans.doEnergyInjection(TD.Energy.EU, FRONT, 2048, 1, true), ":233 — the stopped guard");
	}
}
