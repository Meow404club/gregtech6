package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.energy.GT6DynamoBlockEntity;

/**
 * The EU-bridge converter offline tests (task p29-w4-eu-bridge) — the EU→{HU,KU,RU}
 * machines over the shared {@link GT6DynamoBlockEntity} core with the input arm re-typed
 * to EU. The acceptance arms:
 * <ul>
 * <li>① the half-rate ladder: EVERY family × EVERY tier converts one in-sized EU packet
 *     into exactly one out-sized packet where out = in/2 (the units(stored, in, out)
 *     mechanism — the row NBT_INPUT/NBT_OUTPUT pairs verbatim, Loader :817-821/:833-837/
 *     :849-853);</li>
 * <li>the WASTE_ENERGY = T arm: with NO accepting neighbor the intake continues and the
 *     vent empties the capacitor every tick (nothing emitted, nothing banked);</li>
 * <li>② the cross-domain reject: the bridges accept EU ONLY — a KU/RU/HU input offer is
 *     refused outright (the isEnergyType reference-equality gate);</li>
 * <li>the packet bands (in/2 .. 2·in in, out/2 .. 2·out out, the Base10:76/:77 arms) and
 *     the oversize strike;</li>
 * <li>the emission signs: HU ∉ ALL_NEGATIVE_ALLOWED (positive packets only), KU/RU ride
 *     the live ±sign (TD.java:202);</li>
 * <li>the accounting pair round-trip (gt.last_in / gt.last_out, the RCON live face).</li>
 * </ul>
 * Driven through {@link GT6DynamoBlockEntity#onTick} (public) — the package-private
 * doConversion is the dynamo-home-package tests' handle, not this registry-package one.
 */
public class GT6EuBridgeBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GTMachines.ElectricBridgeBlockEntity> sHeaterType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	static final byte FRONT = 2, BACK = 3; // mFacing default NORTH = FRONT (the emission face)

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		// the type-capturing factory form of the real registrations, over STONE (offline:
		// GT6DynamoBlock.tier(STONE) = 0 → the T1 rung 32/16)
		BlockEntityType<GTMachines.ElectricBridgeBlockEntity>[] tHolder =
				(BlockEntityType<GTMachines.ElectricBridgeBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTMachines.ElectricBridgeBlockEntity(tHolder[0], TD.Energy.HU, aPos, aState),
				Blocks.STONE).build(null);
		sHeaterType = tHolder[0];
	}

	/** A family BE at the fixture (the type-capture stands in for the three real BETs). */
	private static GTMachines.ElectricBridgeBlockEntity bridge(TagData aOutType) {
		return new GTMachines.ElectricBridgeBlockEntity(sHeaterType, aOutType, POS, Blocks.STONE.defaultBlockState());
	}

	/** A family BE at an arbitrary ladder row (the tierForOfflineTest seam). */
	private static GTMachines.ElectricBridgeBlockEntity bridge(TagData aOutType, long aInput, long aOutput) {
		GTMachines.ElectricBridgeBlockEntity tBridge = bridge(aOutType);
		tBridge.tierForOfflineTest(aInput, aOutput);
		return tBridge;
	}

	/** The row ladders of the three families, straight off the registration tables. */
	static long[][] familyRows() {
		return new long[][] {
				GTMachines.BRIDGE_INPUTS, GTMachines.BRIDGE_OUTPUTS };
	}

	// ---------------------------------------------------------------------------
	// ① the half-rate ladder — every family × every tier
	// ---------------------------------------------------------------------------

	@Test
	public void halfRateLadderEveryTierEveryFamily() {
		TagData[] tTypes = {TD.Energy.HU, TD.Energy.KU, TD.Energy.RU};
		for (TagData tOut : tTypes) {
			for (int i = 0; i < GTMachines.BRIDGE_INPUTS.length; i++) {
				long tIn = GTMachines.BRIDGE_INPUTS[i], tOutAmount = GTMachines.BRIDGE_OUTPUTS[i];
				assertEquals(tIn / 2, tOutAmount, "the ladder row " + tIn + " carries out = in/2 exactly");
				GTMachines.ElectricBridgeBlockEntity tBridge = bridge(tOut, tIn, tOutAmount);
				CountingSink tSink = new CountingSink(tOut);
				tSink.recordSize = true;
				tBridge.setAdjacencyOverrideForTest(adjacencyAt(tSink, FRONT));
				// one in-sized EU packet enters the BACK face
				assertEquals(1, tBridge.doEnergyInjection(TD.Energy.EU, BACK, tIn, 1, true),
						"the in-sized packet is above min " + (tIn / 2) + " and below max " + (tIn * 2));
				tBridge.onTick(100, true);
				assertEquals(1, tSink.packets, "family " + tOut + " tier " + tIn + ": one packet emitted");
				assertEquals(tOutAmount, tSink.lastSize, "the packet SIZE is the converted out = in/2 (units()");
				assertEquals(tOutAmount, tSink.totalMass, "the emitted mass = the out column");
				assertEquals(0, tBridge.mStorage, "WASTE_ENERGY=T: the vent emptied the bucket");
				assertTrue(tBridge.mActive, "the emission fired");
				assertEquals(tIn, tBridge.mLastIn, "the accounting in = the consumed EU");
				assertEquals(tOutAmount, tBridge.mLastOut, "the accounting out = the emitted " + tOut);
			}
		}
	}

	@Test
	public void theVentPairingKeepsPerTickInEqualsTwoOutUnderLoad() {
		// two ticks with a standing offer: each tick contributes exactly in in / out out —
		// the RCON half=true relation's offline shape
		GTMachines.ElectricBridgeBlockEntity tBridge = bridge(TD.Energy.HU, 32, 16);
		CountingSink tSink = new CountingSink(TD.Energy.HU);
		tBridge.setAdjacencyOverrideForTest(adjacencyAt(tSink, FRONT));
		for (int t = 0; t < 2; t++) {
			assertEquals(1, tBridge.doEnergyInjection(TD.Energy.EU, BACK, 32, 1, true));
			tBridge.onTick(100 + t, true);
		}
		assertEquals(2, tSink.packets);
		assertEquals(64, tBridge.mLastIn);
		assertEquals(32, tBridge.mLastOut);
		assertEquals(tBridge.mLastIn, tBridge.mLastOut * 2, "the half=true pairing (the /gt6bridge stat arm)");
	}

	// ---------------------------------------------------------------------------
	// the WASTE_ENERGY = T arm — the intake is never gated by the consumer side
	// ---------------------------------------------------------------------------

	@Test
	public void wasteArmConsumesIntoTheVoidWithoutAnAcceptingNeighbor() {
		GTMachines.ElectricBridgeBlockEntity tBridge = bridge(TD.Energy.KU, 32, 16);
		CountingSink tSink = new CountingSink(TD.Energy.KU);
		tSink.rejecting = true; // nothing out there takes KU
		tBridge.setAdjacencyOverrideForTest(adjacencyAt(tSink, FRONT));
		assertEquals(1, tBridge.doEnergyInjection(TD.Energy.EU, BACK, 32, 1, true), "the EU intake rides regardless");
		tBridge.onTick(100, true);
		assertEquals(0, tSink.packets, "nothing emitted — no consumer");
		assertEquals(0, tBridge.mLastOut, "the accounting out stays 0");
		assertEquals(32, tBridge.mLastIn, "the intake PAID (waste=T: the input side is not refunded)");
		assertEquals(0, tBridge.mStorage, "the vent cleared the bucket");
		assertFalse(tBridge.mActive, "no emission, no activity");
	}

	// ---------------------------------------------------------------------------
	// ② the cross-domain reject — EU ONLY on the input arm
	// ---------------------------------------------------------------------------

	@Test
	public void crossDomainOffersAreRefusedOutright() {
		GTMachines.ElectricBridgeBlockEntity tBridge = bridge(TD.Energy.HU, 32, 16);
		// a KU offer (the axle's rotation packet) — the bridge is NOT an EU machine
		assertEquals(0, tBridge.doEnergyInjection(TD.Energy.KU, BACK, 32, 1, true), "KU refused");
		// an RU offer and an HU offer likewise
		assertEquals(0, tBridge.doEnergyInjection(TD.Energy.RU, BACK, 32, 1, true), "RU refused");
		assertEquals(0, tBridge.doEnergyInjection(TD.Energy.HU, BACK, 32, 1, true), "HU refused (the bridge accepts EU only)");
		assertEquals(0, tBridge.mLastIn, "nothing consumed");
		assertEquals(0, tBridge.mStorage, "nothing banked");
		// the face getters answer the type gate too (the connectivity probes)
		assertFalse(tBridge.isEnergyAcceptingFrom(TD.Energy.KU, BACK, false), "the KU probe is dead");
		assertTrue(tBridge.isEnergyAcceptingFrom(TD.Energy.EU, BACK, false), "the EU probe is live on the BACK face");
		assertFalse(tBridge.isEnergyAcceptingFrom(TD.Energy.EU, FRONT, false), "the FRONT face is the emission face, not intake");
		assertTrue(tBridge.isEnergyEmittingTo(TD.Energy.HU, FRONT, false), "the HU emission face is the FRONT");
		// and the type collections name both converter halves
		Collection<TagData> tTypes = tBridge.getEnergyTypes(BACK);
		assertTrue(tTypes.contains(TD.Energy.EU) && tTypes.contains(TD.Energy.HU), "EU in + HU out");
	}

	// ---------------------------------------------------------------------------
	// the packet bands (Base10:76/:77) and the oversize strike
	// ---------------------------------------------------------------------------

	@Test
	public void inputBandIsHalfToDoubleTheRecommendedColumn() {
		GTMachines.ElectricBridgeBlockEntity tBridge = bridge(TD.Energy.HU, 512, 256);
		assertEquals(256, tBridge.getEnergySizeInputMin(TD.Energy.EU, BACK), "in > 16: min = in/2 (the :76 arm)");
		assertEquals(512, tBridge.getEnergySizeInputRecommended(TD.Energy.EU, BACK));
		assertEquals(1024, tBridge.getEnergySizeInputMax(TD.Energy.EU, BACK), "max = 2×in");
		// wrong type answers 0 across the band
		assertEquals(0, tBridge.getEnergySizeInputMin(TD.Energy.RU, BACK), "the RU band is dead on an EU intake");
		// the output band rides the out column
		assertEquals(128, tBridge.getEnergySizeOutputMin(TD.Energy.HU, FRONT), "outMin = out/2 (the :64 door)");
		assertEquals(256, tBridge.getEnergySizeOutputRecommended(TD.Energy.HU, FRONT));
		assertEquals(512, tBridge.getEnergySizeOutputMax(TD.Energy.HU, FRONT));
		assertEquals(0, tBridge.getEnergySizeOutputMin(TD.Energy.KU, FRONT), "the KU band is dead on an HU emission");
	}

	@Test
	public void belowMinOffersAreWhiteBurnedAndOversizeOffersStrikeTheLadder() {
		GTMachines.ElectricBridgeBlockEntity tBridge = bridge(TD.Energy.HU, 32, 16);
		// an 8 EU packet (below min 16): the ROOT gate swallows the offer (the white burn)
		assertEquals(1, tBridge.doEnergyInjection(TD.Energy.EU, BACK, 8, 1, true), "the offer is consumed");
		assertEquals(0, tBridge.mStorage, "for nothing (the Root white burn, Root:717)");
		assertEquals(0, tBridge.mLastIn, "the white burn is not intake");
		// a 65 EU packet (above max 64): consumed ALL, the overload ladder strikes once
		assertEquals(3, tBridge.doEnergyInjection(TD.Energy.EU, BACK, 65, 3, true), "oversize: the whole offer consumed");
		assertEquals(1, tBridge.mExplosionPrevention, "strike one of the 100-grace ladder");
	}

	// ---------------------------------------------------------------------------
	// the emission signs (TD.java:202) — HU positive-only, KU/RU live ±
	// ---------------------------------------------------------------------------

	@Test
	public void huEmissionFoldsTheSignAndKuKeepsIt() {
		// the heater fed from a NEGATIVE EU source emits a POSITIVE HU packet (HU ∉ ALL_NEGATIVE_ALLOWED)
		GTMachines.ElectricBridgeBlockEntity tHeater = bridge(TD.Energy.HU, 32, 16);
		CountingSink tHeatSink = new CountingSink(TD.Energy.HU);
		tHeatSink.recordSize = true;
		tHeater.setAdjacencyOverrideForTest(adjacencyAt(tHeatSink, FRONT));
		assertEquals(1, tHeater.doEnergyInjection(TD.Energy.EU, BACK, -32, 1, true), "the negative EU packet enters");
		tHeater.onTick(100, true);
		assertEquals(16, tHeatSink.lastSize, "the HU packet size is positive (the sign folded, Base10:121 second conjunct fails)");
		// the engine keeps the sign (KU ∈ ALL_NEGATIVE_ALLOWED ∧ EU ∈ ALL_NEGATIVE_ALLOWED)
		GTMachines.ElectricBridgeBlockEntity tEngine = bridge(TD.Energy.KU, 32, 16);
		CountingSink tKuSink = new CountingSink(TD.Energy.KU);
		tKuSink.recordSize = true;
		tEngine.setAdjacencyOverrideForTest(adjacencyAt(tKuSink, FRONT));
		assertEquals(1, tEngine.doEnergyInjection(TD.Energy.EU, BACK, -32, 1, true));
		tEngine.onTick(100, true);
		assertEquals(-16, tKuSink.lastSize, "the KU packet rides the live negative sign");
	}

	// ---------------------------------------------------------------------------
	// the accounting pair round-trip (the RCON live face)
	// ---------------------------------------------------------------------------

	@Test
	public void accountingPairSurvivesASaveLoadRoundTrip() {
		GTMachines.ElectricBridgeBlockEntity tBridge = bridge(TD.Energy.RU, 32, 16);
		CountingSink tSink = new CountingSink(TD.Energy.RU);
		tBridge.setAdjacencyOverrideForTest(adjacencyAt(tSink, FRONT));
		tBridge.doEnergyInjection(TD.Energy.EU, BACK, 32, 1, true);
		tBridge.onTick(100, true);
		assertEquals(32, tBridge.mLastIn);
		assertEquals(16, tBridge.mLastOut);
		CompoundTag tNBT = new CompoundTag();
		tBridge.saveAdditional(tNBT);
		assertTrue(tNBT.contains(GTMachines.ElectricBridgeBlockEntity.NBT_LAST_IN), "gt.last_in persisted");
		assertTrue(tNBT.contains(GTMachines.ElectricBridgeBlockEntity.NBT_LAST_OUT), "gt.last_out persisted");
		GTMachines.ElectricBridgeBlockEntity tRestored = bridge(TD.Energy.RU, 32, 16);
		tRestored.load(tNBT);
		assertEquals(32, tRestored.mLastIn, "the intake survived");
		assertEquals(16, tRestored.mLastOut, "the emission survived");
		tRestored.resetAccounting();
		assertEquals(0, tRestored.mLastIn + tRestored.mLastOut, "the /gt6bridge reset arm zeroes the pair");
	}

	@Test
	public void theBridgeFamilyRowsCarryTheUpstreamColumns() {
		// the three families' metaId ladders + voltage words (the parity columns)
		assertEquals(10001, GTMachines.ELECTRIC_HEATER_ROWS.get(0).metaId());
		assertEquals(10005, GTMachines.ELECTRIC_HEATER_ROWS.get(4).metaId());
		assertEquals(10011, GTMachines.ELECTRIC_ENGINE_ROWS.get(0).metaId());
		assertEquals(10025, GTMachines.ELECTRIC_MOTOR_ROWS.get(4).metaId());
		assertEquals("LV", GTMachines.ELECTRIC_HEATER_ROWS.get(0).voltageWord());
		assertEquals("IV", GTMachines.ELECTRIC_MOTOR_ROWS.get(4).voltageWord());
		assertSame(TD.Energy.HU, GTMachines.ELECTRIC_HEATER_ROWS.get(0).outType());
		assertSame(TD.Energy.KU, GTMachines.ELECTRIC_ENGINE_ROWS.get(0).outType());
		assertSame(TD.Energy.RU, GTMachines.ELECTRIC_MOTOR_ROWS.get(0).outType());
		// the shared ladders: out = in/2 on every rung
		for (int i = 0; i < 5; i++) {
			assertEquals(GTMachines.BRIDGE_INPUTS[i] / 2, GTMachines.BRIDGE_OUTPUTS[i], "rung " + i + " half-rate");
		}
	}

	// ---------------------------------------------------------------------------
	// the counting sink (the harness's CountingEuSink generalized to the typed packet)
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
			// ANY side: the emitter's FRONT lands on the sink's own opposite face (the
			// EnergyTarget side is the receiver's), and the sink is type-agnostic geometry
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
