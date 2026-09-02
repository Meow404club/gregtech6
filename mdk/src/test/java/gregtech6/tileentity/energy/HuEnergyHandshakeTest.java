package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.fluid.GTFluids;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task p13-hu-steam-foundation — the offline acceptance fixture for the HU/steam
 * foundation (decision 2026-09-03-p13-hu-energy-face): the full HU handshake over the
 * EXISTING faces, zero production code. Three sections:
 *
 * <ol>
 * <li>the boiler-constant scope pin (decision 2026-09-03-p13-boiler-family-split ②);</li>
 * <li>the HU token + the source→sink handshake: {@code resolveEnergyType("HU")} reference
 *     equality, then a real {@code ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.HU, ...)}
 *     booking into an HU sink's double doEnergyInjection (the F probe + the T inject), with
 *     the EU-gate refusal as the negative control (the fixture precedent
 *     GTEnergySourceBlockEntityTest:251);</li>
 * <li>the SIDES_TOP single-gate truth table (upstream MultiTileEntityGeneratorSolid.java:270
 *     {@code SIDES_TOP[aSide] && super.isEnergyEmittingTo(...)}) — the imaginary form W2's
 *     burning box must reproduce, driven through the real Util to prove only the top face
 *     leaves packets.</li>
 * </ol>
 *
 * <p>Side bytes are the vanilla 3D data order the port's adjacency uses
 * (GTEnergySourceBlockEntity.adjacency → {@code Direction.from3DDataValue}): 0=down,
 * 1=up, 2..5 = n/s/w/e — so the upstream {@code SIDES_TOP} gate is {@code aSide == 1}.
 */
public class HuEnergyHandshakeTest extends GTOfflineTestBase {

	/** The vanilla UP ordinal — the port's byte for the upstream CS SIDES_TOP slot. */
	static final byte SIDE_TOP = 1;

	static BlockEntityType<GTEnergySourceBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GTEnergySourceBlockEntity>[] tHolder = (BlockEntityType<GTEnergySourceBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTEnergySourceBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	// ---------------------------------------------------------------------------
	// 1. the boiler-constant scope pin (decision 2026-09-03-p13-boiler-family-split ②)
	// ---------------------------------------------------------------------------

	/**
	 * The four-way machine check of the STEAM_PER_WATER scope discipline: the new
	 * boiler-side globals carry the CS.java values while the pre-existing engine-private
	 * pair stays byte-identical (the P12 zero-diff red line) — the _GLOBAL suffix exists
	 * precisely so both can live side by side in {@link GTFluids}.
	 */
	@Test
	public void boilerSteamConstantsPinTheScopeDiscipline() {
		assertEquals(80, GTFluids.EU_PER_WATER, "CS.java:238 — the boiler-side global heat price of 1 L water");
		assertEquals(160, GTFluids.STEAM_PER_WATER_GLOBAL, "CS.java:242 — the boiler-side global standard 160 steam = 1 water");
		assertEquals(200, GTFluids.STEAM_PER_WATER, "the engine-private MultiTileEntityEngineSteam.java:58 recycle ratio — NOT touched by the boiler append (the P12 consumers' zero-diff red line)");
		assertEquals(2, GTFluids.STEAM_PER_EU, "CS.java:240 — the shared steam-per-EU divisor, reused not re-declared");
		assertEquals(GTFluids.EU_PER_WATER * GTFluids.STEAM_PER_EU, GTFluids.STEAM_PER_WATER_GLOBAL,
				"the CS.java:238-242 self-consistency: 80 EU × 2 steam/EU = 160 steam");
	}

	// ---------------------------------------------------------------------------
	// 2. the HU token + the full source→sink handshake
	// ---------------------------------------------------------------------------

	/** HU-counting sink — the receiving end of the handshake fixture. */
	public static class HuSink extends BlockEntity implements ITileEntityEnergy {
		public long injectionCalls = 0, acceptedAmount = 0, injectedWattage = 0, lastSize = -1, lastAmount = -1;
		public byte lastSide = -1;

		public HuSink(BlockPos aPos) {
			super(null, aPos, Blocks.STONE.defaultBlockState());
		}

		/**
		 * The double doEnergyInjection receive path: the {@code aDoInject=false} probe arm
		 * answers capacity WITHOUT booking (the simulate slot), the {@code aDoInject=true}
		 * arm books — the IMPLEMENTOR checks its own acceptance first (the interface
		 * contract note), so a foreign type returns 0 on both arms.
		 */
		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) {
					injectionCalls++;
					acceptedAmount += aAmount;
					injectedWattage += Math.abs(aSize * aAmount);
					lastSize = aSize;
					lastAmount = aAmount;
					lastSide = aSide;
				}
				return aAmount; // the full bundle fits — the probe and the inject answer alike
			}
			return 0;
		}

		/** The decision's receiving face: ANY side (the upstream BoilerTank FACE_ANY :98 shape), HU only. */
		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == TD.Energy.HU;
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return aEnergyType == TD.Energy.HU;}

		@Override
		public java.util.Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.HU.AS_LIST;}

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
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return Long.MAX_VALUE;}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return Long.MAX_VALUE;}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	@Test
	public void resolveEnergyTypeHuIsTheSharedHeatInstance() {
		// the reference-equality contract: "HU" resolves to the SHARED TD.Energy.HEAT
		// instance (GTEnergySourceBlockEntity.java:290-309, the :501 gate vocabulary) —
		// never a rogue mint (TagData.createTagData would register a type nobody accepts)
		assertSame(TD.Energy.HU, GTEnergySourceBlockEntity.resolveEnergyType("HU"));
		assertSame(TD.Energy.HU, GTEnergySourceBlockEntity.resolveEnergyType("hu"), "case-insensitive, still the shared instance");
		assertSame(TD.Energy.HEAT, TD.Energy.HU, "HU is the HEAT alias — one and the same TagData object");
		assertNull(GTEnergySourceBlockEntity.resolveEnergyType("NOT_A_REAL_TYPE"), "an unknown name resolves to null — no mint");

		// the dial arms the HU face on the rig
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		tSource.setEnergyType(TD.Energy.HU);
		assertTrue(tSource.isEnergyType(TD.Energy.HU, (byte)0, true), "the emitting probe follows the HU dial");
		assertFalse(tSource.isEnergyType(TD.Energy.HU, (byte)0, false), "the accepting probe stays pure-source");
		assertFalse(tSource.isEnergyType(TD.Energy.EU, (byte)0, true), "the old EU default is gone");
		assertSame(TD.Energy.HU, tSource.mEnergyType, "the field holds the shared instance");
	}

	@Test
	public void huEmitBooksIntoTheHuSinkThroughTheRealUtil() {
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		tSource.setEnergyType(TD.Energy.HU);
		tSource.setVoltage(1); // the HU packet shape: size is unused (TD.Energy.HU "Size = unused (always 1)"), amount carries the heat
		HuSink tSink = new HuSink(POS.offset(0, 0, 1)); // the sink to the source's south (side 3)
		IEnergyAdjacency tAdjacency = aSide -> aSide == 3 ? new EnergyTarget(tSink, (byte)2) : null;

		// the probe arm books nothing (the F half of the double doEnergyInjection)
		assertEquals(5, tSink.doEnergyInjection(TD.Energy.HU, (byte)2, 1, 5, false),
				"the F probe answers capacity (the whole 5-packet bundle fits)");
		assertEquals(0, tSink.injectionCalls, "the probe books nothing");

		// mode off — the Util loop never opens (the real-probe gate is closed)
		tSource.setEmitting(false);
		assertEquals(0, ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.HU, tSource.mVoltage, tSource.mAmperage, tSource, tAdjacency),
				"mode off — nothing is emitted");
		assertEquals(0, tSink.injectionCalls, "no packets booked while off");

		// mode on — the handshake: the HU packet books exactly once, on the sink's north face (side 2)
		tSource.setEmitting(true);
		assertEquals(1, ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.HU, tSource.mVoltage, tSource.mAmperage, tSource, tAdjacency),
				"mode on — the sink accepts the full packet (the Util return = used packets)");
		assertEquals(1, tSink.injectionCalls, "one side call — the Util hands the sink the whole bundle at once");
		assertEquals(1, tSink.lastSize, "the packet size is 1 (the HU shape)");
		assertEquals(1, tSink.lastAmount, "the packet count is mAmperage");
		assertEquals(2, tSink.lastSide, "the injection lands on the sink's north face (the EnergyTarget side)");
		assertEquals(1, tSink.injectedWattage, "the sink's wattage ledger = |size x amount|");

		// the amperage dial scales the packet count (the /gt6energy amp path)
		tSource.setAmperage(4);
		assertEquals(4, ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.HU, tSource.mVoltage, tSource.mAmperage, tSource, tAdjacency),
				"4 A — four packets used");
		assertEquals(5, tSink.acceptedAmount, "the amount ledger accumulates 1 + 4");
	}

	@Test
	public void huEmitIsRefusedByTheEuGate() {
		// the negative control: an EU machine's :501/:508 reference gate refuses HU on the
		// real emit path — the W2-relevant semantic the RCON chain re-proves live
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		tSource.setEnergyType(TD.Energy.HU);
		tSource.setEmitting(true);
		GTEnergySourceBlockEntityTest.CountingSink tEuSink = new GTEnergySourceBlockEntityTest.CountingSink(POS.offset(0, 0, 1));
		IEnergyAdjacency tAdjacency = aSide -> aSide == 3 ? new EnergyTarget(tEuSink, (byte)2) : null;
		assertEquals(0, ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.HU, tSource.mVoltage, tSource.mAmperage, tSource, tAdjacency),
				"HU into an EU-only sink: zero packets used");
		assertEquals(0, tEuSink.injectionCalls, "nothing booked — the reference gate refused");
	}

	// ---------------------------------------------------------------------------
	// 3. the SIDES_TOP single-gate truth table (the W2 firebox imaginary form,
	//    upstream MultiTileEntityGeneratorSolid.java:270)
	// ---------------------------------------------------------------------------

	/** The W2 firebox gate, imagined over the existing rig: {@code SIDES_TOP[aSide] && super} — the GeneratorSolid:270 form verbatim. */
	static final class TopGateSource extends GTEnergySourceBlockEntity {
		TopGateSource(BlockEntityType<GTEnergySourceBlockEntity> aType, BlockPos aPos) {
			super(aType, aPos, Blocks.STONE.defaultBlockState()); // the offline BET (the null fallback would hit the live registry)
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aSide == SIDE_TOP && super.isEnergyEmittingTo(aEnergyType, aSide, aTheoretical);
		}
	}

	@Test
	public void theTopFaceGateTruthTablePinsTheW2FireboxShape() {
		TopGateSource tSource = new TopGateSource(sType, POS);
		tSource.setEnergyType(TD.Energy.HU);

		// the truth table over all six sides, BOTH probes: only the top face (byte 1)
		// carries the gate. Theoretical probe = the static capability, mode-independent
		// (a conductor visually stays connected while off); real probe = additionally
		// mode-gated by the super arm, so mode off closes even the top face.
		tSource.setEmitting(false);
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertEquals(tSide == SIDE_TOP, tSource.isEnergyEmittingTo(TD.Energy.HU, tSide, true),
					"theoretical probe, side " + tSide + (tSide == SIDE_TOP ? " — the top face alone stays connected" : " — a non-top face is dead"));
			assertFalse(tSource.isEnergyEmittingTo(TD.Energy.HU, tSide, false),
					"real probe (mode off), side " + tSide + " — the super's mode arm keeps everything closed");
		}

		// mode on: the top face opens, the five others stay dead
		tSource.setEmitting(true);
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertEquals(tSide == SIDE_TOP, tSource.isEnergyEmittingTo(TD.Energy.HU, tSide, false),
					"real probe (mode on), side " + tSide);
		}
		// a foreign type is locked out everywhere, top face included (the super's isEnergyType arm)
		assertFalse(tSource.isEnergyEmittingTo(TD.Energy.EU, SIDE_TOP, true), "the type lock holds on the open face");

		// the real Util drive with an all-sides adjacency: the six-side loop opens on the
		// single gate, one bundle call consumes all three packets, and they land on the
		// sink's bottom (side 0)
		HuSink tSink = new HuSink(POS.offset(0, 1, 0)); // the sink directly above the source
		IEnergyAdjacency tAdjacency = aSide -> new EnergyTarget(tSink, (byte)(aSide ^ 1)); // ^1 = the vanilla opposite table
		assertEquals(3, ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.HU, 1, 3, tSource, tAdjacency),
				"one side open → the whole bundle is used (the Util six-side loop hits the single gate)");
		assertEquals(1, tSink.injectionCalls, "exactly one side call");
		assertEquals(3, tSink.acceptedAmount, "the three packets booked");
		assertEquals(0, tSink.lastSide, "the source's top (side 1) feeds the sink's bottom face (side 0)");
	}
}
