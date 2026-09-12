package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import net.minecraft.core.BlockPos;
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
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.energy.GT6DynamoBlockEntityTestHarness.CountingFeSink;

/**
 * The Electric Dynamo core offline tests (task p28-c-dynamo-family-be) — the RU→EU
 * machine over the shared core: the 0.6875 ratio table per tier (Loader :946-950, the
 * upstream size-carrying Converter:85 emit — ONE packet whose SIZE is the whole converted
 * amount), the LIVE negative sign (RU ∧ EU both ∈ ALL_NEGATIVE_ALLOWED — the family that
 * keeps it, unlike the Flux fold), the input gate/overload family on the shared core, the
 * facing split and the EU band getters.
 */
public class GT6ElectricDynamoBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6ElectricDynamoBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	static final byte FRONT = 2, BACK = 3; // mFacing default NORTH = FRONT (output)

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GT6ElectricDynamoBlockEntity>[] tHolder = (BlockEntityType<GT6ElectricDynamoBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6ElectricDynamoBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** A T1 machine (32 RU in / 22 EU out). */
	private static GT6ElectricDynamoBlockEntity dynamo() {
		return sType.create(POS, Blocks.STONE.defaultBlockState());
	}

	/** A machine at an arbitrary tier column (the tierForOfflineTest seam). */
	private static GT6ElectricDynamoBlockEntity dynamo(long aInput, long aOutput) {
		GT6ElectricDynamoBlockEntity tDynamo = dynamo();
		tDynamo.tierForOfflineTest(aInput, aOutput);
		return tDynamo;
	}

	private static long inject(GT6DynamoBlockEntity aDynamo, long aSize, long aAmount) {
		return aDynamo.doEnergyInjection(TD.Energy.RU, BACK, aSize, aAmount, true);
	}

	// ---------------------------------------------------------------------------
	// the 0.6875 ratio table (the Converter:85 one-packet form)
	// ---------------------------------------------------------------------------

	@Test
	public void ratioTableExactZeroPointSixEightSevenFiveEveryTier() {
		for (long[] tRow : GT6DynamoBlockEntityTestHarness.ELECTRIC_ROWS) {
			GT6ElectricDynamoBlockEntity tDynamo = dynamo(tRow[0], tRow[1]);
			CountingEuSink tSink = new CountingEuSink(Long.MAX_VALUE);
			tDynamo.setAdjacencyOverride(GT6DynamoBlockEntityTestHarness.adjacencyAt(tSink, FRONT));
			assertEquals(1, inject(tDynamo, tRow[0], 1));
			tDynamo.doConversion(100);
			assertEquals(tRow[1], tSink.totalEu,
					tRow[0] + " RU -> one " + tRow[1] + " EU packet (the Loader :946-950 pair, exactly 0.6875)");
			assertEquals(tRow[1], tSink.lastSize, "the packet SIZE is the whole converted amount (Converter:85)");
			assertEquals(0, tDynamo.mStorage, "the vent emptied the bucket");
			assertTrue(tDynamo.mActive);
		}
	}

	@Test
	public void emitIsAlwaysOnePacketPerTick() {
		// two inRec packets in the bucket: tOutput = 44, still ONE packet of size 44 (the
		// amount stays mMultiplier = 1 — upstream floods size, EU carries it in the size)
		GT6ElectricDynamoBlockEntity tDynamo = dynamo(32, 22);
		CountingEuSink tSink = new CountingEuSink(Long.MAX_VALUE);
		tDynamo.setAdjacencyOverride(GT6DynamoBlockEntityTestHarness.adjacencyAt(tSink, FRONT));
		assertEquals(2, inject(tDynamo, 32, 2));
		tDynamo.doConversion(100);
		assertEquals(44, tSink.totalEu);
		assertEquals(1, tSink.lastAmount, "one packet — the mMultiplier=1 column");
	}

	// ---------------------------------------------------------------------------
	// the LIVE negative sign (RU ∧ EU both ∈ ALL_NEGATIVE_ALLOWED, TD.java:202)
	// ---------------------------------------------------------------------------

	@Test
	public void negativeSpinEmitsNegativeSizeEu() {
		GT6ElectricDynamoBlockEntity tDynamo = dynamo(32, 22);
		CountingEuSink tSink = new CountingEuSink(Long.MAX_VALUE);
		tDynamo.setAdjacencyOverride(GT6DynamoBlockEntityTestHarness.adjacencyAt(tSink, FRONT));
		assertEquals(1, inject(tDynamo, -32, 1));
		assertTrue(tDynamo.mNegativeInput);
		tDynamo.doConversion(100);
		assertEquals(-22, tSink.lastSize, "counterclockwise in → negative-size EU out (the Base10:121 conjunction LIVE)");
		assertEquals(22, tSink.totalEu, "the sink books |size × amount|");
		assertTrue(tDynamo.mActive);
	}

	// ---------------------------------------------------------------------------
	// the door and the vent on the EU side
	// ---------------------------------------------------------------------------

	@Test
	public void doorOpensAtExactlyTheHalfOutMin() {
		// storage 16: tOutput = floor(16*22/32) = 11 = outMin 11 — the door opens and the
		// size-carrying emit DELIVERS (unlike the Flux whole-packet floor): 11 EU out
		GT6ElectricDynamoBlockEntity tDynamo = dynamo(32, 22);
		CountingEuSink tSink = new CountingEuSink(Long.MAX_VALUE);
		tDynamo.setAdjacencyOverride(GT6DynamoBlockEntityTestHarness.adjacencyAt(tSink, FRONT));
		assertEquals(1, inject(tDynamo, 16, 1));
		tDynamo.doConversion(100);
		assertEquals(11, tSink.totalEu, "the odd 11-EU packet — the 0.6875 tail is a packet, not a remainder");
	}

	@Test
	public void refusedEmitStillVents() {
		// the funnel semantics on the EU family: the sink refuses, the conversion emitted
		// nothing, the vent burns the bucket — the axle's work is gone ("不通就漏光")
		GT6ElectricDynamoBlockEntity tDynamo = dynamo(32, 22);
		assertEquals(1, inject(tDynamo, 32, 1));
		tDynamo.doConversion(100); // no adjacency: isEnergyEmittingTo(front) but no neighbor
		assertFalse(tDynamo.mActive);
		assertEquals(0, tDynamo.mStorage);
	}

	// ---------------------------------------------------------------------------
	// the shared-core gate family (same assertions as the Flux face, EU side spot checks)
	// ---------------------------------------------------------------------------

	@Test
	public void oversizeAndFullGateOnTheSharedCore() {
		GT6ElectricDynamoBlockEntity tDynamo = dynamo(32, 22);
		assertEquals(3, inject(tDynamo, 65, 3), "65 > 64: consumed ALL, strike one");
		assertEquals(1, tDynamo.mExplosionPrevention);
		assertEquals(0, tDynamo.mStorage);
		assertEquals(2, inject(tDynamo, 32, 2), "the bucket refills");
		assertEquals(0, inject(tDynamo, 32, 1), "full → the 0 refund");
	}

	@Test
	public void euFacesSplitBySideAndType() {
		GT6ElectricDynamoBlockEntity tDynamo = dynamo(32, 22);
		assertTrue(tDynamo.isEnergyAcceptingFrom(TD.Energy.RU, BACK, false));
		assertFalse(tDynamo.isEnergyAcceptingFrom(TD.Energy.RU, FRONT, false));
		assertTrue(tDynamo.isEnergyEmittingTo(TD.Energy.EU, FRONT, false));
		assertFalse(tDynamo.isEnergyEmittingTo(TD.Energy.EU, BACK, false));
		assertFalse(tDynamo.isEnergyAcceptingFrom(TD.Energy.EU, BACK, false), "EU is the output type");
		assertEquals(16, tDynamo.getEnergySizeInputMin(TD.Energy.RU, BACK));
		assertEquals(11, tDynamo.getEnergySizeOutputMin(TD.Energy.EU, FRONT), "22/2 — the Converter:64 door");
		assertEquals(44, tDynamo.getEnergySizeOutputMax(TD.Energy.EU, FRONT));
		assertEquals(0, tDynamo.getEnergySizeOutputRecommended(TD.Energy.RF, FRONT), "RF has nothing to do with this machine");
		Collection<TagData> tTypes = tDynamo.getEnergyTypes(BACK);
		assertTrue(tTypes.contains(TD.Energy.RU) && tTypes.contains(TD.Energy.EU));
	}

	// ---------------------------------------------------------------------------
	// the EU emit walks ONLY the GT face — an adjacent FE cable is invisible to it
	// (the post-W0 posture: the util dispatch serves ITileEntityEnergy receivers alone)
	// ---------------------------------------------------------------------------

	@Test
	public void euEmitIgnoresNonGtNeighbors() {
		GT6ElectricDynamoBlockEntity tDynamo = dynamo(32, 22);
		CountingFeSink tCable = new CountingFeSink(Long.MAX_VALUE);
		// adjacency serves the FE cable on the FRONT — post-W0 the Util dispatch inserts
		// into ITileEntityEnergy receivers alone (ITileEntityEnergy.java:291-293), so the
		// packet is refused and the vent takes it
		tDynamo.setAdjacencyOverride(aVisited -> aVisited == FRONT
				? new gregapi.tileentity.energy.EnergyTarget(tCable, (byte)3) : null);
		assertEquals(1, inject(tDynamo, 32, 1));
		tDynamo.doConversion(100);
		assertFalse(tDynamo.mActive, "a non-GT front neighbor receives nothing");
		assertEquals(0, tCable.totalFe, "no FE ever leaves the EU family");
		assertEquals(0, tDynamo.mStorage, "the refused packet burned with the vent");
	}

	/** Counting EU sink — the LimitedSink shape of the FE-converter test (a real BE receiver). */
	public static class CountingEuSink extends BlockEntity implements ITileEntityEnergy {
		public long totalEu = 0, lastSize = -1, lastAmount = -1;
		private final long mAcceptCap;

		static final BlockEntityType<CountingEuSink> FAKE_TYPE =
				BlockEntityType.Builder.of((aPos, aState) -> new CountingEuSink(aPos, Long.MAX_VALUE), Blocks.STONE).build(null);

		public CountingEuSink(BlockPos aPos, long aAcceptCap) {
			super(FAKE_TYPE, aPos, Blocks.STONE.defaultBlockState());
			mAcceptCap = aAcceptCap;
		}

		/** The convenience form the tests use (the FE-converter LimitedSink POS shape). */
		public CountingEuSink(long aAcceptCap) {
			this(POS, aAcceptCap);
		}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				long tAccepted = Math.min(aAmount, mAcceptCap);
				if (aDoInject && tAccepted > 0) {
					totalEu += Math.abs(aSize) * tAccepted;
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
}
