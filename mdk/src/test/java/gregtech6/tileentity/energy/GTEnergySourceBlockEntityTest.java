package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * GTEnergySourceBlockEntity offline tests (task p8-d4-energy-source spec ④): the emitter
 * face gate (isEnergyType = the EU lock, accepting = false — the pure source), the size
 * band, the NBT round trip over the card's three keys, and the joint test with the D1
 * fixture adjacency — the emit call books into a counting fake sink through the real
 * {@code ITileEntityEnergy.Util.emitEnergyToNetwork} single-call semantics (the upstream
 * MultiTileEntitySolarPanelElectric :160-162 evidence line). Also hosts the
 * GTWireBlockEntity.canConnect double-probe assertions (the p8 ruling 1 backfill:
 * upstream EnergyCompat.canConnectElectricity :102 accepting || emitting).
 *
 * <p>Offline-harness note on the mEmitting default (task p11-infra-hygiene-bundle,
 * the in-case record): {@code mEmitting} starts {@code false} (the p8-d4 card) and
 * NOTHING in the offline harness flips it or fires the tick emit on its own — there is
 * no server ticker, {@code onTick} only runs when a test drives {@code updateEntity()}
 * by hand. Note the side resolution: a level-less fixture takes the SERVER branch of
 * the tick chain ({@code TileEntityBase01Root.isClientSide()} :164-166 needs a non-null
 * level, so {@code isServerSide()} :159-161 is {@code true}) — the gate that keeps a
 * fresh fixture silent is {@code mEmitting = false} itself; once armed, the level-less
 * {@code adjacency()} resolves every side to {@code null} and the emit books 0 (that is
 * why {@code onTickOfflineIsANoCrashNoOp} is a no-op without a crash). Emission-asserting
 * tests must therefore {@code setEmitting(true)} AND wire the emit seam explicitly
 * ({@code emitOnce()} or an {@code mAdjacencyOverride}) — a forgotten flip or a forgotten
 * adjacency wiring reads as zero packets booked, never as an error.
 */
public class GTEnergySourceBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GTEnergySourceBlockEntity> sType;
	static BlockEntityType<GTWireBlockEntity> sWireType;
	static final BlockPos POS = new BlockPos(3, 4, 5);
	static final BlockPos GEN_POS = new BlockPos(3, 4, 6);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GTEnergySourceBlockEntity>[] tHolder = (BlockEntityType<GTEnergySourceBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTEnergySourceBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
		BlockEntityType<GTWireBlockEntity>[] tWireHolder = (BlockEntityType<GTWireBlockEntity>[]) new BlockEntityType<?>[1];
		tWireHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTWireBlockEntity(tWireHolder[0], aPos, aState), Blocks.STONE).build(null);
		sWireType = tWireHolder[0];
	}

	/** Counting EU sink — the fake consumer of the D1 fixture joint test. */
	public static class CountingSink extends BlockEntity implements ITileEntityEnergy {
		public long injectionCalls = 0, acceptedAmount = 0, injectedWattage = 0, lastSize = -1, lastAmount = -1;
		public byte lastSide = -1;

		public CountingSink(BlockPos aPos) {
			super(null, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			// the IMPLEMENTOR checks its own acceptance (the interface contract note)
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) {
					injectionCalls++; // one call per side per emit — the Util passes the whole packet bundle at once
					acceptedAmount += aAmount;
					injectedWattage += Math.abs(aSize * aAmount);
					lastSize = aSize;
					lastAmount = aAmount;
					lastSide = aSide;
				}
				return aAmount;
			}
			return 0;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == TD.Energy.EU;
		}

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

	/** Plain non-energy BE — must never connect. */
	public static class PlainBox extends BlockEntity {
		public PlainBox(BlockPos aPos) {
			super(null, aPos, Blocks.STONE.defaultBlockState());
		}
	}

	// ---------------------------------------------------------------------------
	// defaults (the card: mVoltage=32 / mAmperage=1 / mEmitting=false)
	// ---------------------------------------------------------------------------

	@Test
	public void defaultsFollowTheCard() {
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(32, tSource.mVoltage, "the card default mVoltage = 32");
		assertEquals(1, tSource.mAmperage, "the card default mAmperage = 1");
		assertFalse(tSource.mEmitting, "the card default mEmitting = false");
		assertEquals("energy_source", tSource.getTileEntityName(), "the BET registry path mirrors the name");
	}

	// ---------------------------------------------------------------------------
	// the emitter face gate (spec ④: the isEnergyEmittingTo gate = isEnergyType / the EU lock / accepting = F)
	// ---------------------------------------------------------------------------

	@Test
	public void emitterFacesCarryTheUpstreamGate() {
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		// upstream :104 — isEnergyType = aEmitting && EU
		assertTrue(tSource.isEnergyType(TD.Energy.EU, (byte)0, true), "upstream :104 — the emitting probe locks EU");
		assertFalse(tSource.isEnergyType(TD.Energy.EU, (byte)0, false), "upstream :104 — the accepting probe is constant false (pure source)");
		assertFalse(tSource.isEnergyType(TD.Energy.RU, (byte)0, true), "upstream :104 — non-EU rejected");
		// the theoretical conductor probe: the static capability on every side (all-sides, no facing)
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertTrue(tSource.isEnergyEmittingTo(TD.Energy.EU, tSide, true),
					"side " + tSide + " — the theoretical probe reports the static EU capability (all six sides)");
			assertFalse(tSource.isEnergyAcceptingFrom(TD.Energy.EU, tSide, true),
					"side " + tSide + " — a pure source accepts nothing");
		}
		// non-EU is locked out on both probes
		assertFalse(tSource.isEnergyEmittingTo(TD.Energy.RU, (byte)3, true), "the EU lock holds on the theoretical probe too");
		assertFalse(tSource.isEnergyEmittingTo(TD.Energy.RU, (byte)3, false), "the EU lock holds on the real probe too");
		assertEquals(TD.Energy.EU.AS_LIST, tSource.getEnergyTypes((byte)6), "upstream :110");
	}

	@Test
	public void theRealProbeCarriesTheModeGate() {
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertFalse(tSource.mEmitting, "starts off");
		assertFalse(tSource.isEnergyEmittingTo(TD.Energy.EU, (byte)3, false),
				"mode off — the real emit probe (Util :246 form) is closed");
		tSource.setEmitting(true);
		assertTrue(tSource.isEnergyEmittingTo(TD.Energy.EU, (byte)3, false),
				"mode on — the real emit probe opens (the onTick gate rides the face too)");
		assertTrue(tSource.isEnergyEmittingTo(TD.Energy.EU, (byte)3, true),
				"the theoretical probe is mode-independent (the conductor stays connected while off)");
	}

	// ---------------------------------------------------------------------------
	// the size band and the dead directions (upstream :108-109 + the card)
	// ---------------------------------------------------------------------------

	@Test
	public void sizeBandCarriesTheVoltage() {
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(32, tSource.getEnergySizeOutputRecommended(TD.Energy.EU, (byte)0), "upstream :109");
		assertEquals(32, tSource.getEnergySizeOutputMax(TD.Energy.EU, (byte)0), "upstream :108");
		assertEquals(32, tSource.getEnergySizeInputRecommended(TD.Energy.EU, (byte)0), "the card — input rec = mVoltage");
		assertEquals(32, tSource.getEnergySizeInputMax(TD.Energy.EU, (byte)0), "the card — input max = mVoltage");
		assertEquals(0, tSource.getEnergySizeOutputMin(TD.Energy.EU, (byte)0), "upstream :107 flattened to 0 per the card");
		assertEquals(0, tSource.getEnergySizeInputMin(TD.Energy.EU, (byte)0), "the card — input min = 0");
		assertEquals(0, tSource.getEnergyDemanded(TD.Energy.EU, (byte)0, 32), "a pure source demands nothing");
		assertEquals(0, tSource.getEnergyOffered(TD.Energy.EU, (byte)0, 32), "the card — offered 0");
		assertEquals(0, tSource.doEnergyInjection(TD.Energy.EU, (byte)0, 32, 1, true), "a pure source never injects");
		assertEquals(0, tSource.doEnergyExtraction(TD.Energy.EU, (byte)0, 32, 1, true), "a pure source never extracts");
	}

	// ---------------------------------------------------------------------------
	// NBT round trip (the card's three keys)
	// ---------------------------------------------------------------------------

	@Test
	public void nbtRoundTripsTheThreeKeys() {
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		tSource.setEmitting(true);
		tSource.setVoltage(128);
		tSource.setAmperage(3);

		CompoundTag tSaved = tSource.saveWithoutMetadata();
		assertTrue(tSaved.contains("emitting"), "the card key emitting");
		assertTrue(tSaved.getBoolean("emitting"));
		assertEquals(128, tSaved.getLong("voltage"), "the card key voltage");
		assertEquals(3, tSaved.getLong("amperage"), "the card key amperage");
		assertEquals("energy_source", tSaved.getString("te_name"), "the base te_name key rides along");

		GTEnergySourceBlockEntity tBack = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertTrue(tBack.mEmitting, "the mode survives the round trip");
		assertEquals(128, tBack.mVoltage, "the voltage survives the round trip");
		assertEquals(3, tBack.mAmperage, "the amperage survives the round trip");
	}

	// ---------------------------------------------------------------------------
	// the D1 fixture joint test — the Util emit books into the fake sink
	// (the upstream :160-162 single-call semantics, spec ④)
	// ---------------------------------------------------------------------------

	@Test
	public void emitBooksIntoTheFixtureSinkThroughTheRealUtil() {
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		CountingSink tSink = new CountingSink(POS.offset(0, 0, 1)); // the sink sits to the source's south (side 3)
		// the D1 fixture adjacency: only side 3 resolves, to the sink's back side 2 (NORTH)
		IEnergyAdjacency tAdjacency = aSide -> aSide == 3 ? new EnergyTarget(tSink, (byte)2) : null;

		// mode off — the Util loop probes isEnergyEmittingTo(F), the gate is closed, nothing flows
		tSource.setEmitting(false);
		assertEquals(0, ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, tSource.mVoltage, tSource.mAmperage, tSource, tAdjacency),
				"mode off — nothing is emitted (the upstream mStopped slot, :84)");
		assertEquals(0, tSink.injectionCalls, "no packets booked while off");

		// mode on — the evidence-line call: size=32 EU x amount=1 A books exactly one packet
		tSource.setEmitting(true);
		assertEquals(1, ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, tSource.mVoltage, tSource.mAmperage, tSource, tAdjacency),
				"mode on — the sink accepts the full packet (the Util return = used packets)");
		assertEquals(1, tSink.injectionCalls, "one side call — the Util hands the sink the whole bundle at once (Util :246-248)");
		assertEquals(32, tSink.lastSize, "the packet size is mVoltage");
		assertEquals(1, tSink.lastAmount, "the packet count is mAmperage");
		assertEquals(2, tSink.lastSide, "the injection lands on the sink's back side (the EnergyTarget side)");
		assertEquals(32, tSink.injectedWattage, "the sink's wattage ledger = |size x amount|");

		// the amperage dial scales the packet count (the /gt6energy amp path)
		tSource.setAmperage(4);
		assertEquals(4, ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, tSource.mVoltage, tSource.mAmperage, tSource, tAdjacency),
				"4 A — four packets used");
		assertEquals(2, tSink.injectionCalls, "the second emit is the second side call");
		assertEquals(5, tSink.acceptedAmount, "the amount ledger accumulates 1 + 4");
		assertEquals(4, tSink.lastAmount);
	}

	@Test
	public void onTickOfflineIsANoCrashNoOp() {
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		tSource.setEmitting(true);
		tSource.updateEntity(); // no level — the adjacency resolves to nothing, the emit is 0
		assertFalse(tSource.isDead(), "the tick chain survives the level-less emit");
	}

	// ---------------------------------------------------------------------------
	// the canConnect double probe (p8 ruling 1 — the GTWireBlockEntity backfill,
	// upstream EnergyCompat.canConnectElectricity :102 accepting || emitting)
	// ---------------------------------------------------------------------------

	// ---------------------------------------------------------------------------
	// the p11 dials (task p11-rotor-source-flip): the emitted type + the ±alternating mode
	// ---------------------------------------------------------------------------

	@Test
	public void typeDialCarriesTheEmittedTypeFace() {
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		// the reference-equality contract: the dial sets the SHARED TD.Energy instance
		tSource.setEnergyType(TD.Energy.RU);
		assertTrue(tSource.isEnergyType(TD.Energy.RU, (byte)0, true), "the emitting probe follows the dial (RU)");
		assertFalse(tSource.isEnergyType(TD.Energy.EU, (byte)0, true), "the old EU pin is gone");
		assertFalse(tSource.isEnergyType(TD.Energy.RU, (byte)0, false), "the accepting probe stays pure-source");
		assertTrue(tSource.isEnergyEmittingTo(TD.Energy.RU, (byte)3, true), "the theoretical face follows the dial");
		assertFalse(tSource.isEnergyEmittingTo(TD.Energy.KU, (byte)3, true), "a foreign type stays locked");
		assertEquals(TD.Energy.RU.AS_LIST, tSource.getEnergyTypes((byte)6), "the type list follows the dial");

		tSource.setEnergyType(TD.Energy.KU);
		assertTrue(tSource.isEnergyType(TD.Energy.KU, (byte)0, true), "the dial moves to KU");
		assertFalse(tSource.isEnergyType(TD.Energy.RU, (byte)0, true), "RU is locked out again");

		// the name lookup returns the REGISTERED instance (never a rogue mint)
		assertEquals(TD.Energy.RU, GTEnergySourceBlockEntity.resolveEnergyType("ru"), "case-insensitive, shared instance");
		assertNull(GTEnergySourceBlockEntity.resolveEnergyType("NOT_A_REAL_TYPE"), "an unknown name resolves to null — no mint");
	}

	@Test
	public void alternatingModeSquaresThePacketSign() {
		// the upstream EngineSteam :146 form: mPiston += 1; mPiston &= 3 (:113-114), then
		// size = mPiston > 1 ? -size : size — from phase 0 the emit sequence is +,-,-,+
		// (each period holds exactly one positive→non-positive crossing, the machine :815
		// delivery edge). The plain (non-alternating) emit stays all-positive. Driven via
		// emitOnce directly: the bare seam under test — no server ticker exists offline
		// (see the class-javadoc harness note), and updateEntity would only re-wrap this
		// same emitOnce in timer bookkeeping.
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		CountingSink tSink = new CountingSink(POS.offset(0, 0, 1));
		IEnergyAdjacency tAdjacency = aSide -> aSide == 3 ? new EnergyTarget(tSink, (byte)2) : null;
		tSource.setAdjacencyOverride(tAdjacency); // the offline emit seam (no level on the fixture)
		tSource.setEmitting(true);
		tSource.setAmperage(1);

		// alternating OFF: every packet positive (the p8-d4 card form)
		for (int i = 0; i < 3; i++) tSource.emitOnce();
		assertEquals(32, tSink.lastSize, "non-alternating emit stays positive");

		// alternating ON: the 2-bit piston square wave +,-,-,+
		tSource.setAlternating(true);
		long[] tExpected = {32, -32, -32, 32};
		for (int i = 0; i < 4; i++) {
			tSource.emitOnce();
			assertEquals(tExpected[i], tSink.lastSize, "alternating emit " + i + " follows the piston phase (mPiston=" + tSource.mPiston + ")");
		}
		// the negative packets flowed at full |size| (the Util layer does not abs — the
		// :503 machine math owns the direction-agnostic booking)
		assertEquals(32, Math.abs(tSink.lastSize), "the magnitude rides mVoltage");
	}

	@Test
	public void energyTypeAndAlternatingNbtRoundTrip() {
		GTEnergySourceBlockEntity tSource = sType.create(POS, Blocks.STONE.defaultBlockState());
		tSource.setEnergyType(TD.Energy.RU);
		tSource.setAlternating(true);

		CompoundTag tSaved = tSource.saveWithoutMetadata();
		assertEquals(TD.Energy.RU.mName, tSaved.getString("energytype"), "the p11 energytype key rides the TagData mName");
		assertTrue(tSaved.getBoolean("alternating"), "the p11 alternating key");

		GTEnergySourceBlockEntity tBack = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertEquals(TD.Energy.RU, tBack.mEnergyType, "the type survives as the SHARED instance (the :501 gate is reference equality)");
		assertTrue(tBack.mAlternating, "the alternating mode survives");

		// an unknown persisted name keeps the LOADING BE's current type (no rogue mint on
		// load, no silent EU clobber of a configured type)
		CompoundTag tGarbage = tSource.saveWithoutMetadata();
		tGarbage.putString("energytype", "NOT_A_REAL_TYPE");
		GTEnergySourceBlockEntity tKept = sType.create(POS, Blocks.STONE.defaultBlockState());
		tKept.setEnergyType(TD.Energy.RU); // the operator had configured RU before the broken save replays
		tKept.load(tGarbage);
		assertEquals(TD.Energy.RU, tKept.mEnergyType, "an unknown name does not clobber the type on load");
	}

	@Test
	public void wireConnectsToThePureEmitterThroughTheEmittingBranch() {
		GTWireBlockEntity tWire = sWireType.create(POS, Blocks.STONE.defaultBlockState());
		GTEnergySourceBlockEntity tGen = sType.create(GEN_POS, Blocks.STONE.defaultBlockState());
		// side 2 (NORTH) of the wire faces the gen; the probed neighbour side is 3 (SOUTH)
		assertTrue(tWire.canConnect((byte)2, tGen),
				"the emitting branch reaches the pure source (the accepting branch is permanently false there)");
		tGen.setEmitting(false);
		assertTrue(tWire.canConnect((byte)2, tGen),
				"the theoretical probe is mode-independent — the wire stays connectable while the gen is off");
		tGen.setEmitting(true);
		assertTrue(tWire.canConnect((byte)2, tGen), "and of course while on");
	}

	@Test
	public void theAcceptingBranchAndTheRejectionsSurviveTheBackfill() {
		GTWireBlockEntity tWire = sWireType.create(POS, Blocks.STONE.defaultBlockState());
		CountingSink tSink = new CountingSink(POS.offset(0, 0, 1));
		assertTrue(tWire.canConnect((byte)2, tSink), "the accepting branch still connects pure acceptors");
		assertFalse(tWire.canConnect((byte)2, new PlainBox(POS.offset(0, 0, 1))), "non-energy BEs never connect");
		assertFalse(tWire.canConnect((byte)2, null), "null neighbours never connect");
		// an unconnected wire accepts nothing (mask 0) AND emits towards nothing — both probes dead
		assertFalse(tWire.canConnect((byte)2, sWireType.create(POS.offset(0, 0, 1), Blocks.STONE.defaultBlockState())),
				"an unconnected wire neighbour stays unconnectable through either branch");
	}
}
