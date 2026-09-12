package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyBridge;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.energy.GT6DynamoBlockEntityTestHarness.CountingFeSink;

/**
 * The Flux Dynamo core offline tests (task p28-c-dynamo-family-be) — the
 * GT6FeConverterBlockEntityTest posture (hand-verified math tables over an offline BET)
 * on the RU→FE machine: the 2.75 ratio table per tier (the W0 whole-packet seam — the
 * packet sizes ARE the NBT_OUTPUT column, the math rides the root
 * {@code EnergyBridge.pushPacketTrain}), the input gate three states (oversize → the
 * 100-strike overload ladder / below-min → the Root white-burn swallow / full → the 0
 * refund), the WASTE_ENERGY funnel (idle vent, emit-without-deduction, the sub-packet
 * tail burn), the negative-sign fold (RF ∉ ALL_NEGATIVE_ALLOWED), the facing split and
 * the NBT round trip.
 *
 * <p>All expected numbers are hand-derived from the upstream anchors cited in the
 * assertions (TileEntityBase10EnergyConverter / TE_Behavior_Energy_Converter /
 * TE_Behavior_Energy_Stats / UT.Code.units UT.java:1682).
 */
public class GT6FluxDynamoBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6FluxDynamoBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	/** The GT6 side bytes: mFacing default 2 (NORTH) = FRONT (output), its opposite 3 = BACK (input). */
	static final byte FRONT = 2, BACK = 3;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GT6FluxDynamoBlockEntity>[] tHolder = (BlockEntityType<GT6FluxDynamoBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6FluxDynamoBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** A T1 machine (the STONE fixture state resolves tier 0: 32 RU in / 88 FE out). */
	private static GT6FluxDynamoBlockEntity dynamo() {
		return sType.create(POS, Blocks.STONE.defaultBlockState());
	}

	/** A machine at an arbitrary tier column (the tierForOfflineTest seam — a Block is unconstructible offline). */
	private static GT6FluxDynamoBlockEntity dynamo(long aInput, long aOutput) {
		GT6FluxDynamoBlockEntity tDynamo = dynamo();
		tDynamo.tierForOfflineTest(aInput, aOutput);
		return tDynamo;
	}

	/** Injects one RU packet through the Root gate (the real axle path: doEnergyInjection). */
	private static long inject(GT6DynamoBlockEntity aDynamo, long aSize, long aAmount) {
		return aDynamo.doEnergyInjection(TD.Energy.RU, BACK, aSize, aAmount, true);
	}

	/** The conversion tick, the transformer-test direct form (server-side semantics only). */
	private static void tick(GT6FluxDynamoBlockEntity aDynamo) {
		aDynamo.doConversion(100);
	}

	// ---------------------------------------------------------------------------
	// the 2.75 ratio table (the W0 seam: packet size = NBT_OUTPUT, count = whole packets)
	// ---------------------------------------------------------------------------

	@Test
	public void ratioTableExactTwoPointSevenFiveEveryTier() {
		long[][] tRows = GT6DynamoBlockEntityTestHarness.DYNAMO_ROWS;
		for (long[] tRow : tRows) {
			GT6FluxDynamoBlockEntity tDynamo = dynamo(tRow[0], tRow[1]);
			CountingFeSink tSink = new CountingFeSink(Long.MAX_VALUE);
			tDynamo.setPushTargetOverride(tSink::receiveEnergy);
			assertEquals(1, inject(tDynamo, tRow[0], 1), "one " + tRow[0] + " RU packet consumed");
			tick(tDynamo);
			assertEquals(tRow[1], tSink.totalFe,
					tRow[0] + " RU -> " + tRow[1] + " FE (the Loader :953-957 column pair, exactly 2.75)");
			assertEquals(0, tDynamo.mStorage, "the waste vent empties the bucket at the tick end");
			assertTrue(tDynamo.mActive, "the emit happened");
		}
	}

	@Test
	public void packetCountIsTheWholePacketIdentity() {
		// floor(floor(s*out/in)/out) == floor(s/in): two inRec packets in the bucket -> two out-packets
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		CountingFeSink tSink = new CountingFeSink(Long.MAX_VALUE);
		tDynamo.setPushTargetOverride(tSink::receiveEnergy);
		assertEquals(2, inject(tDynamo, 32, 2));
		tick(tDynamo);
		assertEquals(176, tSink.totalFe, "2 x 88 FE — two whole packets, the identity form");
	}

	@Test
	public void subPacketTailBurnsNeverAccumulates() {
		// storage 49 (one valid 49-RU packet): tOutput = floor(49*88/32) = 134, whole packets
		// = 1 -> 88 FE delivered; the 46-FE tail burns with the vent (the wave-card wall: no
		// accumulation — upstream would have flooded the amount tail, the W0 seam is whole packets)
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		CountingFeSink tSink = new CountingFeSink(Long.MAX_VALUE);
		tDynamo.setPushTargetOverride(tSink::receiveEnergy);
		assertEquals(1, inject(tDynamo, 49, 1), "49 <= 64 max: a valid packet");
		tick(tDynamo);
		assertEquals(88, tSink.totalFe, "one whole NBT_OUTPUT packet out of the 134-FE conversion");
		assertEquals(0, tDynamo.mStorage, "the tail burned with the funnel vent");
	}

	@Test
	public void belowTheDoorConvertsToNothingYetStillBurns() {
		// storage 16: tOutput = floor(16*88/32) = 44 >= outMin 44 — the DOOR opens but a whole
		// 88-FE packet never forms: zero packets push, the vent burns the bucket (the declared
		// whole-packet seam difference, class doc)
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		CountingFeSink tSink = new CountingFeSink(Long.MAX_VALUE);
		tDynamo.setPushTargetOverride(tSink::receiveEnergy);
		assertEquals(1, inject(tDynamo, 16, 1), "16 >= input min 16: accepted");
		tick(tDynamo);
		assertEquals(0, tSink.totalFe, "no whole packet to push");
		assertEquals(0, tDynamo.mStorage, "burned anyway — idle or loaded, the bucket vents");
		assertFalse(tDynamo.mActive);
		assertTrue(tDynamo.mCanEmitEnergy, "the door opened (tOutput 44 >= 44), the packet floor closed it");
	}

	// ---------------------------------------------------------------------------
	// the input gate three states (Stats.doInject :56-66 + the Root white-burn)
	// ---------------------------------------------------------------------------

	@Test
	public void oversizePacketConsumesAllAndStrikes() {
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		assertEquals(3, inject(tDynamo, 65, 3), "size 65 > 64 max: the WHOLE offer consumed (Stats :58-61)");
		assertEquals(1, tDynamo.mExplosionPrevention, "the first strike");
		assertEquals(0, tDynamo.mStorage, "the capacitor cleared (Base10:140-148 soft arm)");
	}

	@Test
	public void oversizeExplodesAfterTheHundredthStrike() {
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		for (int i = 0; i < 100; i++) inject(tDynamo, 65, 1); // the startup grace: 100 soft strikes
		assertEquals(100, tDynamo.mExplosionPrevention);
		assertEquals(0, tDynamo.mStorage);
		inject(tDynamo, 65, 1); // strike 101: the overcharge arm (offline: the log-only Root:330 path)
		assertEquals(100, tDynamo.mExplosionPrevention, "the counter stopped climbing — the ladder moved to overcharge");
	}

	@Test
	public void smallPacketWhiteBurnsAtTheRootGate() {
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		assertEquals(2, tDynamo.doEnergyInjection(TD.Energy.RU, BACK, 15, 2, true),
				"size 15 < input min 16: the offer counts as USED but doInject never runs (Root:717)");
		assertEquals(0, tDynamo.mStorage, "nothing stored — the rotation was swallowed");
	}

	@Test
	public void fullCapacitorRefundsTheOffer() {
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		assertEquals(2, inject(tDynamo, 32, 2), "64/32: the bucket fills exactly");
		assertEquals(0, inject(tDynamo, 32, 1), "full -> 0: the axle-side original-amount refund (Stats:62-63)");
		assertEquals(64, tDynamo.mStorage);
	}

	@Test
	public void partialPacketCountFitsTheSpace() {
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		assertEquals(1, inject(tDynamo, 32, 1));
		assertEquals(1, inject(tDynamo, 32, 10), "space for 32 more = exactly ONE packet of the ten offered (Stats:63)");
		assertEquals(64, tDynamo.mStorage);
	}

	// ---------------------------------------------------------------------------
	// the WASTE_ENERGY funnel (Converter:81 skip + :92 tail)
	// ---------------------------------------------------------------------------

	@Test
	public void idleVentBurnsWithoutAnyReceiver() {
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		tDynamo.setPushTargetOverride(null); // nothing attached
		assertEquals(1, inject(tDynamo, 32, 1));
		tick(tDynamo);
		assertFalse(tDynamo.mActive);
		assertEquals(0, tDynamo.mStorage, "空转也烧: the RU entered, converted nothing deliverable, vented");
	}

	@Test
	public void emitDeductsNothingTheVentTakesAll() {
		// the Converter:81 waste arm: the emit does NOT book its FE against the capacitor.
		// The receiver accepts only 100 of the 176-FE train — the seam's alignment floors
		// the real send to the whole-packet 88 (EnergyBridge:128, the FeCompat form), so
		// exactly one packet lands; the bucket is simply gone at the vent either way.
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		CountingFeSink tSink = new CountingFeSink(100); // accepts 100 of the 176 offered
		tDynamo.setPushTargetOverride(tSink::receiveEnergy);
		assertEquals(2, inject(tDynamo, 32, 2));
		tick(tDynamo);
		assertEquals(88, tSink.totalFe, "the train aligned DOWN to the whole packet the cable could take");
		assertEquals(0, tDynamo.mStorage);
		assertTrue(tDynamo.mActive);
	}

	// ---------------------------------------------------------------------------
	// the negative-sign fold (RF ∉ ALL_NEGATIVE_ALLOWED)
	// ---------------------------------------------------------------------------

	@Test
	public void negativeSpinFoldsToUnsignedFe() {
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		CountingFeSink tSink = new CountingFeSink(Long.MAX_VALUE);
		tDynamo.setPushTargetOverride(tSink::receiveEnergy);
		assertEquals(1, inject(tDynamo, -32, 1), "counterclockwise packet accepted");
		assertTrue(tDynamo.mNegativeInput);
		tick(tDynamo);
		assertEquals(88, tSink.totalFe, "unsigned FE out — the output conjunct killed the negative leg");
		assertEquals(88, tSink.lastSize, "pushPacketTrain bridges by the magnitude (EnergyBridge:124)");
	}

	// ---------------------------------------------------------------------------
	// the facing split and the size bands (DynamoFlux :36-39, Base10:76-77)
	// ---------------------------------------------------------------------------

	@Test
	public void backIsTheOnlyInputFrontTheOnlyOutput() {
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		assertTrue(tDynamo.isInput(BACK));
		assertFalse(tDynamo.isInput(FRONT));
		assertTrue(tDynamo.isOutput(FRONT));
		assertFalse(tDynamo.isOutput(BACK));
		assertTrue(tDynamo.isEnergyAcceptingFrom(TD.Energy.RU, BACK, false));
		assertFalse(tDynamo.isEnergyAcceptingFrom(TD.Energy.RU, FRONT, false));
		assertFalse(tDynamo.isEnergyAcceptingFrom(TD.Energy.RF, BACK, false), "RF is the OUTPUT type, not accepted");
		assertTrue(tDynamo.isEnergyEmittingTo(TD.Energy.RF, FRONT, false));
		assertFalse(tDynamo.isEnergyEmittingTo(TD.Energy.RF, BACK, false));
	}

	@Test
	public void sizeBandsFollowTheRowColumns() {
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		assertEquals(16, tDynamo.getEnergySizeInputMin(TD.Energy.RU, BACK), "in/2 (Base10:76, tInput > 16)");
		assertEquals(32, tDynamo.getEnergySizeInputRecommended(TD.Energy.RU, BACK));
		assertEquals(64, tDynamo.getEnergySizeInputMax(TD.Energy.RU, BACK), "2×in — the Stats overload line");
		assertEquals(44, tDynamo.getEnergySizeOutputMin(TD.Energy.RF, FRONT), "out/2 — the :64 door");
		assertEquals(88, tDynamo.getEnergySizeOutputRecommended(TD.Energy.RF, FRONT));
		assertEquals(176, tDynamo.getEnergySizeOutputMax(TD.Energy.RF, FRONT), "2×out — unreachable by construction");
		assertEquals(0, tDynamo.getEnergySizeInputMin(TD.Energy.RF, BACK), "the wrong type answers 0 (Stats :46-48)");
		assertEquals(0, tDynamo.getEnergySizeOutputRecommended(TD.Energy.RU, FRONT));
		Collection<TagData> tTypes = tDynamo.getEnergyTypes(BACK);
		assertTrue(tTypes.contains(TD.Energy.RU) && tTypes.contains(TD.Energy.RF), "both converter halves (Base10:159)");
	}

	@Test
	public void stoppedMachineRefusesInput() {
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		tDynamo.mStopped = true;
		assertFalse(tDynamo.isEnergyAcceptingFrom(TD.Energy.RU, BACK, false), "the :151 formula, waste=T fold");
		assertEquals(0, inject(tDynamo, 32, 1), "the gate refuses before doInject");
	}

	// ---------------------------------------------------------------------------
	// NBT round trip (the transformer carriers)
	// ---------------------------------------------------------------------------

	@Test
	public void capacitorRoundTripsThroughNbt() {
		GT6FluxDynamoBlockEntity tDynamo = dynamo(32, 88);
		assertEquals(2, inject(tDynamo, 32, 2));
		tDynamo.mStopped = true;
		CompoundTag tNbt = new CompoundTag();
		tDynamo.saveAdditional(tNbt);
		GT6FluxDynamoBlockEntity tRestored = dynamo();
		tRestored.load(tNbt);
		assertEquals(64, tRestored.mStorage, "the capacitor (gt.capacitor)");
		assertTrue(tRestored.mStopped, "the stop (gt.stopped)");
	}

	// ---------------------------------------------------------------------------
	// the units() reproduction (UT.java:1682 verbatim — the wall's own math)
	// ---------------------------------------------------------------------------

	@Test
	public void unitsMatchesTheUpstreamDirectionTable() {
		// hand-derived rows: no reduction (32∤88, 88∤32), floor vs round-up arms
		assertEquals(0, GT6DynamoBlockEntity.units(0, 32, 88, false));
		assertEquals(44, GT6DynamoBlockEntity.units(16, 32, 88, false), "floor(16*88/32) = 44");
		assertEquals(44, GT6DynamoBlockEntity.units(16, 32, 88, true), "16*88 divides by 32 exactly: no round-up even when asked");
		assertEquals(46, GT6DynamoBlockEntity.units(17, 32, 88, false), "floor(1496/32) = 46");
		assertEquals(47, GT6DynamoBlockEntity.units(17, 32, 88, true), "1496 % 32 = 24 > 0: the round-up arm fires");
		assertEquals(134, GT6DynamoBlockEntity.units(49, 32, 88, false), "floor(49*88/32) = 134.75 -> 134");
		assertEquals(176, GT6DynamoBlockEntity.units(64, 32, 88, false), "the full bucket");
		assertEquals(5632, GT6DynamoBlockEntity.units(8192, 8192, 5632, false), "the T5 row: identity units reduce");
		assertEquals(0, GT6DynamoBlockEntity.units(10, 32, 0, true), "the aTargetUnit == 0 guard");
	}
}
