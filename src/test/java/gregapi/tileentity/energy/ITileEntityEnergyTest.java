/**
 * Task p7-d1-energy-core: offline tests for the ported energy core (plain JUnit, no Minecraft
 * classpath). Covers:
 * (a) ITileEntityEnergy.Util.emitEnergyToNetwork six-side loop - dispatch order 0..5, decreasing
 *     aAmount, early break on exhausted amount, zero when nothing emits, null-adjacency skip;
 * (b) Util.insertEnergyInto dispatch - GT receivers go to doEnergyInjection, everything else to
 *     the EnergyBridge seam (register/forward/unregister restore, upstream EnergyCompat.java:141
 *     guards);
 * (c) EnergyGate math - the pure part of upstream TileEntityBase01Root.java:716-717
 *     (ALL_SIZE_IRRELEVANT branch + |size| >= Min, swallowed aAmount on too-small injection);
 * (d) token/constant locks - TD.Energy.EU exists, is NOT in ALL_SIZE_IRRELEVANT (EU is
 *     size-sensitive, TD.java:218), IS in ALL_EXPLODING (TD.java:214), CS.RF_PER_EU = 4
 *     (upstream CS.java:208) and the overcharge card decisions.
 */
package gregapi.tileentity.energy;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.CS;
import gregapi.data.TD;

public class ITileEntityEnergyTest {

	@AfterEach
	public void restoreDefaultEnergyBridge() {
		EnergyBridge.register(null);
	}

	// ---------------------------------------------------------------------------
	// Fixtures
	// ---------------------------------------------------------------------------

	/** Inert baseline for the 14-method surface; tests override what they exercise. */
	private static abstract class FakeEnergy implements ITileEntityEnergy {
		@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) { return false; }
		@Override public Collection<TagData> getEnergyTypes(byte aSide) { return Collections.emptyList(); }
		@Override public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) { return false; }
		@Override public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) { return false; }
		@Override public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) { return 0; }
		@Override public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) { return 0; }
		@Override public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) { return 0; }
		@Override public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) { return 0; }
		@Override public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) { return 0; }
		@Override public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) { return 0; }
		@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) { return 0; }
		@Override public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) { return 0; }
		@Override public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) { return 0; }
		@Override public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) { return 0; }
	}

	/** Emitter with per-side programmable isEnergyEmittingTo and a gate-query log. */
	private static final class EmitterFixture extends FakeEnergy {
		final boolean[] emitting = new boolean[6];
		final List<Byte> gateQueries = new ArrayList<>();

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			gateQueries.add(aSide);
			return emitting[aSide];
		}
	}

	/** Bucket adjacency: side -> EnergyTarget, records the query order. */
	private static final class AdjacencyFixture implements IEnergyAdjacency {
		final Map<Byte, EnergyTarget> targets = new HashMap<>();
		final List<Byte> queries = new ArrayList<>();

		@Override
		public EnergyTarget adjacent(byte aSide) {
			queries.add(aSide);
			return targets.get(aSide);
		}
	}

	/** Fake sink accounting doEnergyInjection: accepts up to acceptPerCall packets per call. */
	private static final class SinkFixture extends FakeEnergy {
		final List<long[]> injections = new ArrayList<>(); // {side, size, amount, doInject?1:0}
		final long acceptPerCall;
		long acceptedTotal = 0;

		SinkFixture(long aAcceptPerCall) { acceptPerCall = aAcceptPerCall; }

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			injections.add(new long[] {aSide, aSize, aAmount, aDoInject ? 1 : 0});
			long used = Math.min(aAmount, acceptPerCall);
			if (aDoInject) acceptedTotal += used;
			return used;
		}
	}

	private static AdjacencyFixture bucketOf(SinkFixture[] aSinks) {
		AdjacencyFixture rAdjacency = new AdjacencyFixture();
		for (byte side = 0; side < 6; side++) rAdjacency.targets.put(side, new EnergyTarget(aSinks[side], side));
		return rAdjacency;
	}

	// ---------------------------------------------------------------------------
	// (a) emitEnergyToNetwork: the six-side loop, upstream ITileEntityEnergy.java:229-236
	// ---------------------------------------------------------------------------

	@Test
	public void emitEnergyToNetworkDispatchesToEmittingSidesInOrderWithDecreasingAmountAndBreaks() {
		EmitterFixture emitter = new EmitterFixture();
		Arrays.fill(emitter.emitting, true);
		SinkFixture[] sinks = new SinkFixture[6];
		for (byte side = 0; side < 6; side++) sinks[side] = new SinkFixture(1); // each sink takes exactly 1 packet
		AdjacencyFixture adjacency = bucketOf(sinks);

		long used = ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, 32, 5, emitter, adjacency);

		// side 0 gets the full 5, each following side gets aAmount-rUsedAmount: 5,4,3,2,1
		assertEquals(5, used);
		for (byte side = 0; side < 5; side++) {
			assertEquals(1, sinks[side].injections.size(), "side " + side + " must be dispatched once");
			assertEquals(5 - side, sinks[side].injections.get(0)[2], "side " + side + " must see the decreased amount");
			assertEquals(32, sinks[side].injections.get(0)[1], "packet size must pass through unchanged");
			assertEquals(1, sinks[side].injections.get(0)[3], "insertion must be a real injection, not a simulation");
		}
		// early break after side 4 (aAmount <= rUsedAmount): side 5 is never gated nor dispatched
		assertEquals(0, sinks[5].injections.size());
		assertEquals(Arrays.asList((byte)0, (byte)1, (byte)2, (byte)3, (byte)4), emitter.gateQueries);
		assertEquals(Arrays.asList((byte)0, (byte)1, (byte)2, (byte)3, (byte)4), adjacency.queries);
	}

	@Test
	public void emitEnergyToNetworkReturnsZeroWhenNoSideEmits() {
		EmitterFixture emitter = new EmitterFixture(); // all sides refuse to emit
		AdjacencyFixture adjacency = new AdjacencyFixture();

		assertEquals(0, ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, 32, 5, emitter, adjacency));
		assertEquals(Arrays.asList((byte)0, (byte)1, (byte)2, (byte)3, (byte)4, (byte)5), emitter.gateQueries);
		assertTrue(adjacency.queries.isEmpty(), "no side may be dispatched when the gate refuses all of them");
	}

	@Test
	public void emitEnergyToNetworkSkipsNonEmittingSidesAndForwardsTheReceiverSide() {
		EmitterFixture emitter = new EmitterFixture();
		emitter.emitting[1] = true;
		emitter.emitting[4] = true;
		SinkFixture[] sinks = new SinkFixture[6];
		for (byte side = 0; side < 6; side++) sinks[side] = new SinkFixture(1);
		// adjacency reports the RECEIVER-facing side OPOS[side] = 5-side (upstream DelegatorTileEntity.mSideOfTileEntity)
		AdjacencyFixture adjacency = new AdjacencyFixture();
		for (byte side = 0; side < 6; side++) adjacency.targets.put(side, new EnergyTarget(sinks[side], (byte)(5 - side)));

		long used = ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, 32, 2, emitter, adjacency);

		assertEquals(2, used);
		assertEquals(1, sinks[1].injections.size());
		assertEquals(1, sinks[4].injections.size());
		assertEquals(0, sinks[0].injections.size());
		assertEquals(0, sinks[5].injections.size());
		// Util.insertEnergyInto must receive EnergyTarget.side(), not the sideOutOf value
		assertEquals(4, sinks[1].injections.get(0)[0]);
		assertEquals(1, sinks[4].injections.get(0)[0]);
		// only the two dispatched sides ever reach the adjacency (gated sides are skipped before it), break after side 4
		assertEquals(Arrays.asList((byte)1, (byte)4), adjacency.queries);
	}

	@Test
	public void emitEnergyToNetworkSkipsSidesWithoutAnAdjacentReceiver() {
		EmitterFixture emitter = new EmitterFixture();
		Arrays.fill(emitter.emitting, true);
		SinkFixture sink = new SinkFixture(10); // takes everything offered
		AdjacencyFixture adjacency = new AdjacencyFixture();
		adjacency.targets.put((byte)2, new EnergyTarget(sink, (byte)3));

		long used = ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, 32, 3, emitter, adjacency);

		// sides 0 and 1 have no neighbor (null target -> 0), side 2 consumes all -> early break
		assertEquals(3, used);
		assertEquals(1, sink.injections.size());
		assertEquals(3, sink.injections.get(0)[2]);
		assertEquals(Arrays.asList((byte)0, (byte)1, (byte)2), adjacency.queries);
	}

	// ---------------------------------------------------------------------------
	// (b) insertEnergyInto: instanceof dispatch, upstream ITileEntityEnergy.java:264-266
	// ---------------------------------------------------------------------------

	@Test
	public void insertEnergyIntoDispatchesGTReceiversToDoEnergyInjectionAndNeverToTheBridge() {
		SinkFixture sink = new SinkFixture(3);
		EnergyBridge.register((aEnergyType, aSide, aSize, aAmount, aEmitter, aReceiver) -> {
			throw new AssertionError("a GT receiver must be dispatched before the bridge is ever consulted");
		});

		long used = ITileEntityEnergy.Util.insertEnergyInto(TD.Energy.EU, (byte)2, 32, 5, "emitter", sink);

		assertEquals(3, used);
		assertEquals(1, sink.injections.size());
		assertArrayEquals(new long[] {2, 32, 5, 1}, sink.injections.get(0));
	}

	@Test
	public void insertEnergyIntoDispatchesNonGTReceiversToARegisteredBridge() {
		Object receiver = new Object();
		Object emitter = new Object();
		List<long[]> bridgeCalls = new ArrayList<>(); // {side, size, amount}

		EnergyBridge.register((aEnergyType, aSide, aSize, aAmount, aE, aR) -> {
			assertSame(TD.Energy.EU, aEnergyType);
			assertSame(emitter, aE);
			assertSame(receiver, aR);
			bridgeCalls.add(new long[] {aSide, aSize, aAmount});
			return 42;
		});

		assertEquals(42, ITileEntityEnergy.Util.insertEnergyInto(TD.Energy.EU, (byte)1, 32, 5, emitter, receiver));
		assertEquals(1, bridgeCalls.size());
		assertArrayEquals(new long[] {1, 32, 5}, bridgeCalls.get(0));
	}

	@Test
	public void insertEnergyIntoWithoutABridgeReturnsZero() {
		assertEquals(0, ITileEntityEnergy.Util.insertEnergyInto(TD.Energy.EU, (byte)1, 32, 5, null, new Object()));
		assertEquals(0, EnergyBridge.insertEnergyInto(TD.Energy.EU, (byte)1, 32, 5, null, new Object()));
	}

	@Test
	public void energyBridgeGuardsMirrorUpstreamEnergyCompat() {
		EnergyBridge.register((aEnergyType, aSide, aSize, aAmount, aEmitter, aReceiver) -> {
			throw new AssertionError("upstream EnergyCompat.java:141 guards must reject degenerate input before any handler runs");
		});
		assertEquals(0, EnergyBridge.insertEnergyInto(TD.Energy.EU, (byte)1, 32, 0, null, new Object()));
		assertEquals(0, EnergyBridge.insertEnergyInto(TD.Energy.EU, (byte)1, 32, -1, null, new Object()));
		assertEquals(0, EnergyBridge.insertEnergyInto(TD.Energy.EU, (byte)1, 0, 5, null, new Object()));
		assertEquals(0, EnergyBridge.insertEnergyInto(TD.Energy.EU, (byte)1, 32, 5, null, null));
	}

	@Test
	public void energyBridgeRegistrationRestoresDefaultOnTeardown() {
		assertEquals(0, EnergyBridge.insertEnergyInto(TD.Energy.EU, (byte)1, 32, 5, null, new Object()));
		EnergyBridge.register((aEnergyType, aSide, aSize, aAmount, aEmitter, aReceiver) -> 99);
		assertEquals(99, EnergyBridge.insertEnergyInto(TD.Energy.EU, (byte)1, 32, 5, null, new Object()));
		EnergyBridge.register(null);
		assertEquals(0, EnergyBridge.insertEnergyInto(TD.Energy.EU, (byte)1, 32, 5, null, new Object()));
	}

	// ---------------------------------------------------------------------------
	// (c) EnergyGate: the pure part of upstream TileEntityBase01Root.java:716-717
	// ---------------------------------------------------------------------------

	@Test
	public void gateInjectionLetsSizeIrrelevantTypesSkipTheMinimumCheck() {
		assertTrue(TD.Energy.ALL_SIZE_IRRELEVANT.contains(TD.Energy.RF)); // TD.java:218 carries RF
		boolean[] called = {false};
		// size 1 far below InputMin 100, but RF packets have an irrelevant size
		assertEquals(7, EnergyGate.gateInjection(TD.Energy.RF, true, 1, 100, 7, () -> { called[0] = true; return 7; }));
		assertTrue(called[0]);
	}

	@Test
	public void gateInjectionAcceptsPacketsAtOrAboveInputMin() {
		// EU is size-sensitive (NOT in ALL_SIZE_IRRELEVANT), the gate uses |size| >= InputMin
		assertFalse(TD.Energy.ALL_SIZE_IRRELEVANT.contains(TD.Energy.EU));
		assertEquals(2, EnergyGate.gateInjection(TD.Energy.EU, true, 32, 32, 2, () -> 2)); // exactly at the minimum
		assertEquals(2, EnergyGate.gateInjection(TD.Energy.EU, true, 33, 32, 2, () -> 2)); // above it
		assertEquals(2, EnergyGate.gateInjection(TD.Energy.EU, true, -32, 32, 2, () -> 2)); // Math.abs(aSize), negative direction
	}

	@Test
	public void gateInjectionSwallowsTheAmountWhenThePacketIsTooSmall() {
		// Root:717 returns aAmount (counted as used) WITHOUT calling doInject
		boolean[] called = {false};
		assertEquals(3, EnergyGate.gateInjection(TD.Energy.EU, true, 16, 32, 3, () -> { called[0] = true; return 0; }));
		assertFalse(called[0]);
	}

	@Test
	public void gateInjectionRejectsZeroSizeAndNonAcceptingSides() {
		boolean[] called = {false};
		assertEquals(0, EnergyGate.gateInjection(TD.Energy.EU, true, 0, 0, 5, () -> { called[0] = true; return 1; }));
		assertEquals(0, EnergyGate.gateInjection(TD.Energy.EU, false, 32, 0, 5, () -> { called[0] = true; return 1; }));
		assertFalse(called[0]);
	}

	@Test
	public void gateExtractionMirrorsTheInjectionGateButRefusesTooSmallPackets() {
		// Root:716: ok -> doExtract; too small -> 0 (NOT aAmount like on the injection side)
		assertEquals(4, EnergyGate.gateExtraction(TD.Energy.EU, true, 32, 16, 4, () -> 4));
		assertEquals(0, EnergyGate.gateExtraction(TD.Energy.EU, true, 8, 16, 4, () -> 1));
		assertEquals(4, EnergyGate.gateExtraction(TD.Energy.RF, true, 1, 100, 4, () -> 4)); // size-irrelevant skips
		assertEquals(0, EnergyGate.gateExtraction(TD.Energy.EU, false, 32, 0, 4, () -> 1));
		assertEquals(0, EnergyGate.gateExtraction(TD.Energy.EU, true, 0, 0, 4, () -> 1));
	}

	// ---------------------------------------------------------------------------
	// (d) token and constant locks
	// ---------------------------------------------------------------------------

	@Test
	public void interfaceSurfaceIsExactlyTheFourteenUpstreamMethods() {
		Set<String> expected = new HashSet<>(Arrays.asList(
			"isEnergyType", "getEnergyTypes",
			"isEnergyAcceptingFrom", "isEnergyEmittingTo",
			"doEnergyInjection", "getEnergyDemanded",
			"doEnergyExtraction", "getEnergyOffered",
			"getEnergySizeInputMin", "getEnergySizeOutputMin",
			"getEnergySizeInputRecommended", "getEnergySizeOutputRecommended",
			"getEnergySizeInputMax", "getEnergySizeOutputMax"));
		List<String> actual = new ArrayList<>();
		for (java.lang.reflect.Method m : ITileEntityEnergy.class.getMethods())
			if (m.getDeclaringClass() == ITileEntityEnergy.class && !Modifier.isStatic(m.getModifiers())) actual.add(m.getName());
		assertEquals(14, actual.size(), "the upstream :54-216 surface is 14 methods - additions are new ADR territory");
		assertEquals(expected, new HashSet<>(actual));
	}

	@Test
	public void energyTokensLockEUSemantics() {
		assertSame(TD.Energy.EU, TD.Energy.ELECTRICITY);
		assertEquals("ENERGY.ELECTRICITY", TD.Energy.EU.mName);
		// EU machines reject too-small packets via the gate, so EU must never land in ALL_SIZE_IRRELEVANT (TD.java:218)
		assertFalse(TD.Energy.ALL_SIZE_IRRELEVANT.contains(TD.Energy.EU));
		// overcharged EU machines explode (Root:496-498 consumes ALL_EXPLODING, TD.java:214)
		assertTrue(TD.Energy.ALL_EXPLODING.contains(TD.Energy.EU));
		assertTrue(TD.Energy.ALL_ELECTRIC.contains(TD.Energy.EU));
	}

	@Test
	public void energyConstantsLockExchangeRateAndOverchargeCardDecisions() {
		assertEquals(4, CS.RF_PER_EU); // upstream CS.java:208: 4 RF = 1 EU
		assertTrue(CS.OVERCHARGE_EXPLOSIONS); // card decision, observable overcharge (upstream config default is false)
		assertFalse(CS.OVERCHARGE_BREAKING); // upstream config default ("break_by_overload", GT_API.java:587)
	}
}
