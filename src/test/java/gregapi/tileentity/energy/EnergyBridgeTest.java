/**
 * Tasks p28-cut-eu-fe-bridge (the generalized outbound face) + p28-a-fe-inbound-math (the
 * FE->EU inbound face): the MC-free FE packet math of {@link EnergyBridge}. The EU->FE
 * outbound bridge itself was cut (decision decisions.p28-cut-eu-fe-bridge); what remains is
 * the ratio-agnostic {@link EnergyBridge#pushPacketTrain} (the former outbound math family,
 * the x4 factored out — the ratio lives in the callers' registration constants) and the
 * inbound {@link EnergyBridge#extractFe} dual.
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregapi.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Evidence anchors: the math family inherited from the former outbound face — upstream
 * EnergyCompat.java:212 UT.Code.bind31, :212-213 divup bill, :147 aSize abs; GTCEu
 * FeCompat.java:61-65 (two-call rounding alignment). The packet unit is FE-native on the
 * push face (ratio-agnostic); the pull face keeps the FE->EU reconstruction ratio
 * (CS.RF_PER_EU = 4, CS.java:54). pushPacketTrain has no static state — nothing to restore.
 */
public class EnergyBridgeTest {

	/** A recording FE sink: accepts up to aCapacity, remembers every (amount, simulate) call. */
	private static final class RecordingSink implements EnergyBridge.IFEReceiver {
		final int aCapacity;
		int mStored = 0;
		final List<long[]> mCalls = new ArrayList<>(); // {amount, simulate}

		RecordingSink(int aCapacity) {this.aCapacity = aCapacity;}

		@Override
		public int receiveEnergy(int aAmount, boolean aSimulate) {
			mCalls.add(new long[] {aAmount, aSimulate ? 1 : 0});
			if (aAmount <= 0) return 0;
			int tAccepted = Math.min(aAmount, aCapacity - mStored);
			if (!aSimulate && tAccepted > 0) mStored += tAccepted;
			return tAccepted;
		}
	}

	/** A recording FE source: holds aStored FE, remembers every (amount, simulate) call. */
	private static final class RecordingSource implements EnergyBridge.IFESource {
		int mStored;
		final List<long[]> mCalls = new ArrayList<>(); // {amount, simulate}

		RecordingSource(int aStored) {this.mStored = aStored;}

		@Override
		public int extractEnergy(int aAmount, boolean aSimulate) {
			mCalls.add(new long[] {aAmount, aSimulate ? 1 : 0});
			if (aAmount <= 0) return 0;
			int tGiven = Math.min(aAmount, mStored);
			if (!aSimulate && tGiven > 0) mStored -= tGiven;
			return tGiven;
		}
	}

	// ---------------------------------------------------------------------------
	// bind31 (upstream UT.Code.bind31 verbatim)
	// ---------------------------------------------------------------------------

	@Test
	public void bind31ClampsLongDomainIntoNonNegativeInt() {
		assertEquals(0, EnergyBridge.bind31(-1));
		assertEquals(0, EnergyBridge.bind31(Long.MIN_VALUE));
		assertEquals(0, EnergyBridge.bind31(0));
		assertEquals(42, EnergyBridge.bind31(42));
		assertEquals(Integer.MAX_VALUE, EnergyBridge.bind31(Integer.MAX_VALUE));
		assertEquals(Integer.MAX_VALUE, EnergyBridge.bind31((long)Integer.MAX_VALUE + 1));
		assertEquals(Integer.MAX_VALUE, EnergyBridge.bind31(Long.MAX_VALUE));
		// the overflow shape that made the clamp mandatory: a top-tier ladder's packet count
		// times packet size far exceeds int math
		assertEquals(Integer.MAX_VALUE, EnergyBridge.bind31((1L << 34) * 4));
	}

	// ---------------------------------------------------------------------------
	// the pushPacketTrain family (ratio-agnostic, FE-native packet unit)
	// ---------------------------------------------------------------------------

	@Test
	public void theTwoCallShapeIsSimulateThenAlignedRealInsert() {
		RecordingSink tSink = new RecordingSink(100000);
		EnergyBridge.pushPacketTrain(tSink, 128, 2); // wants 256 FE, gets it whole
		assertEquals(2, tSink.mCalls.size(), "exactly one simulate call and one real call");
		assertEquals(256, tSink.mCalls.get(0)[0], "the simulate call asks for the whole clamped train");
		assertEquals(1, tSink.mCalls.get(0)[1], "first call simulates");
		assertEquals(256, tSink.mCalls.get(1)[0], "the real call sends the packet-aligned amount");
		assertEquals(0, tSink.mCalls.get(1)[1], "second call is real");
	}

	// ---------------------------------------------------------------------------
	// rounding alignment (GTCEu FeCompat.java:63-64 semantics at packet unit)
	// ---------------------------------------------------------------------------

	@Test
	public void partialAcceptanceRoundsDownToWholePacketsBeforeSending() {
		// 128 FE packets; only 200 FE fit -> aligned send is 128 FE (1 packet),
		// NOT 200 FE billed as 2 packets (the divup over-bill the alignment removes).
		RecordingSink tSink = new RecordingSink(200);
		assertEquals(1, EnergyBridge.pushPacketTrain(tSink, 128, 4));
		assertEquals(128, tSink.mStored, "the partial packet's 72 FE stay with the sender, not gifted to FE");
	}

	@Test
	public void tinySinkTakingLessThanOnePacketBridgesNothing() {
		// 100 FE room < one 128 FE packet: aligned send is 0, the real call never fires (wait —
		// it does fire with 0; the contract return 0 bills no packets).
		RecordingSink tSink = new RecordingSink(100);
		assertEquals(0, EnergyBridge.pushPacketTrain(tSink, 128, 4));
		assertEquals(0, tSink.mStored);
	}

	@Test
	public void aFullSinkRejectsTheTrainWithZeroCallsAfterSimulate() {
		RecordingSink tSink = new RecordingSink(5000);
		tSink.mStored = 5000; // pre-fill
		assertEquals(0, EnergyBridge.pushPacketTrain(tSink, 128, 4));
		assertEquals(1, tSink.mCalls.size(), "only the simulate call happens; no zero-amount real call");
	}

	// ---------------------------------------------------------------------------
	// overflow protection (the long-domain product + bind31 clamp)
	// ---------------------------------------------------------------------------

	@Test
	public void overflowingTrainIsClampedNotWrapped() {
		// packetCount * packetSize far beyond int range: the request clamps to
		// Integer.MAX_VALUE, the bill stays within what the sink accepted (never a wrapped
		// negative). 2^20 packets of 2^22 FE = 4194304 FE each; the sink takes 96468992 FE
		// = exactly 23 packets.
		RecordingSink tSink = new RecordingSink(100000000);
		long tUsed = EnergyBridge.pushPacketTrain(tSink, 1L << 22, 1L << 20);
		assertTrue(tUsed > 0, "the clamped train still pushes");
		assertEquals(96468992, tSink.mStored, "the send stops on the last whole packet");
		assertEquals(23, tUsed);
	}

	@Test
	public void aPacketBiggerThanTheWholeSinkBridgesNothing() {
		// the alignment extreme: one packet (4M FE) does not fit a 1M FE sink at all, so the
		// aligned send is zero and NOTHING is sent — the bare-ratio form would have gifted
		// the 1M FE while billing a full packet (the loss the alignment removes).
		RecordingSink tSink = new RecordingSink(1000000);
		assertEquals(0, EnergyBridge.pushPacketTrain(tSink, 1L << 22, 1L << 20));
		assertEquals(0, tSink.mStored);
	}

	@Test
	public void divupBillRoundsUpWhenTheSinkMisbehaves() {
		// a hostile sink that accepts a non-aligned amount on the real call (contract-breaking
		// but legal interface): the divup bill still rounds up, never under-bills.
		EnergyBridge.IFEReceiver tWeird = new EnergyBridge.IFEReceiver() {
			@Override
			public int receiveEnergy(int aAmount, boolean aSimulate) {
				return aSimulate ? aAmount : aAmount - 3; // returns a non-multiple of the packet
			}
		};
		// 2 packets of 128 FE = 256 FE; real call returns 253 -> divup(253, 128) = 2
		assertEquals(2, EnergyBridge.pushPacketTrain(tWeird, 128, 2));
	}

	// ---------------------------------------------------------------------------
	// the FE->EU inbound face (task p28-a-fe-inbound-math): the dual of the push family,
	// same packet unit / same bind31 clamp, floor-exact bill, no overcharge
	// ---------------------------------------------------------------------------

	@Test
	public void conversionTableFourFePerEuPulled() {
		// 1 packet of v EU pulled as 1; the FE source parts with v*4.
		RecordingSource tSrc = new RecordingSource(100000);
		assertEquals(1, EnergyBridge.extractFe(tSrc, 32, 1));
		assertEquals(99872, tSrc.mStored, "32 EU x 4 = 128 FE pulled");

		// a multi-packet pull: amps x volts x 4
		RecordingSource tSrc2 = new RecordingSource(100000);
		assertEquals(5, EnergyBridge.extractFe(tSrc2, 32, 5));
		assertEquals(99360, tSrc2.mStored, "5 amps x 32 EU x 4 = 640 FE pulled");

		// upstream :147: a negative (directional) size pulls by its magnitude
		RecordingSource tSrc3 = new RecordingSource(100000);
		assertEquals(1, EnergyBridge.extractFe(tSrc3, -32, 1));
		assertEquals(99872, tSrc3.mStored);

		// the zero/no-op guards (the :141 family, dual)
		assertEquals(0, EnergyBridge.extractFe(new RecordingSource(100), 32, 0));
		assertEquals(0, EnergyBridge.extractFe(new RecordingSource(100), 0, 5));
		assertEquals(0, EnergyBridge.extractFe(null, 32, 5));
	}

	@Test
	public void thePullShapeIsSimulateThenAlignedRealExtract() {
		RecordingSource tSrc = new RecordingSource(100000);
		EnergyBridge.extractFe(tSrc, 32, 2); // wants 256 FE, gets it whole
		assertEquals(2, tSrc.mCalls.size(), "exactly one simulate call and one real call");
		assertEquals(256, tSrc.mCalls.get(0)[0], "the simulate call asks for the whole clamped train");
		assertEquals(1, tSrc.mCalls.get(0)[1], "first call simulates");
		assertEquals(256, tSrc.mCalls.get(1)[0], "the real call takes the packet-aligned amount");
		assertEquals(0, tSrc.mCalls.get(1)[1], "second call is real");
	}

	@Test
	public void partialSourceRoundsDownToWholePacketsBeforePulling() {
		// 32 EU packets (128 FE each); only 200 FE available -> aligned pull is 128 FE (1 packet),
		// NOT 200 FE counted as 2 packets — the partial packet's 72 FE stay IN the source.
		RecordingSource tSrc = new RecordingSource(200);
		assertEquals(1, EnergyBridge.extractFe(tSrc, 32, 4));
		assertEquals(72, tSrc.mStored, "the partial packet's 72 FE are never taken from the source");
	}

	@Test
	public void sourceWithLessThanOnePacketPullsNothing() {
		// 100 FE < one 128 FE packet: aligned pull is 0, the source keeps everything (the real
		// call still fires with 0, exactly as the push family's tiny-sink case does).
		RecordingSource tSrc = new RecordingSource(100);
		assertEquals(0, EnergyBridge.extractFe(tSrc, 32, 4));
		assertEquals(2, tSrc.mCalls.size(), "the zero-aligned real call fires, mirroring the push face");
		assertEquals(100, tSrc.mStored);
	}

	@Test
	public void anEmptySourceRejectsThePullWithZeroCallsAfterSimulate() {
		RecordingSource tSrc = new RecordingSource(0);
		assertEquals(0, EnergyBridge.extractFe(tSrc, 32, 4));
		assertEquals(1, tSrc.mCalls.size(), "only the simulate call happens; no zero-amount real call");
	}

	@Test
	public void overflowingPullIsClampedNotWrapped() {
		// amps * volts * 4 far beyond int range: the request clamps to Integer.MAX_VALUE,
		// the pull takes every WHOLE packet the source holds (never a wrapped negative).
		// 2^20 EU packets = 4194304 FE each; the source holds 100000000 FE = 23 packets + 3531008.
		RecordingSource tSrc = new RecordingSource(100000000);
		long tGot = EnergyBridge.extractFe(tSrc, 1L << 20, 1L << 20);
		assertTrue(tGot > 0, "the clamped pull still bridges");
		assertEquals(3531008, tSrc.mStored, "the pull stops on the last whole packet");
		assertEquals(23, tGot);
	}

	@Test
	public void aPacketBiggerThanTheWholeSourcePullsNothing() {
		// the alignment extreme: one packet (4M FE) exceeds the whole 1M FE source, so the
		// aligned pull is zero and NOTHING is taken — all 1M FE stay with the source.
		RecordingSource tSrc = new RecordingSource(1000000);
		assertEquals(0, EnergyBridge.extractFe(tSrc, 1L << 20, 1L << 20));
		assertEquals(1000000, tSrc.mStored);
	}

	@Test
	public void exactDivisionFloorsWhenTheSourceMisbehaves() {
		// a hostile source that returns a non-aligned amount on the real call (contract-breaking
		// but legal interface): the exact-division count rounds DOWN to whole packets, the
		// phantom remainder never becomes a packet (the floor mirror of the push divup bill).
		EnergyBridge.IFESource tWeird = new EnergyBridge.IFESource() {
			@Override
			public int extractEnergy(int aAmount, boolean aSimulate) {
				return aSimulate ? aAmount : aAmount - 3; // returns a non-multiple of the packet
			}
		};
		// 2 packets of 32 EU = 256 FE; real call returns 253 -> 253 / 128 = 1
		assertEquals(1, EnergyBridge.extractFe(tWeird, 32, 2));
	}

	// ---------------------------------------------------------------------------
	// the round trip (pushPacketTrain o extractFe): conservation through the 128-FE packet
	// ---------------------------------------------------------------------------

	/** A battery: sink and source in one object, the round-trip conservation fixture. */
	private static final class Battery implements EnergyBridge.IFEReceiver, EnergyBridge.IFESource {
		int mStored = 0;

		@Override
		public int receiveEnergy(int aAmount, boolean aSimulate) {
			if (aAmount <= 0) return 0;
			int tAccepted = Math.min(aAmount, Integer.MAX_VALUE - mStored);
			if (!aSimulate) mStored += tAccepted;
			return tAccepted;
		}

		@Override
		public int extractEnergy(int aAmount, boolean aSimulate) {
			if (aAmount <= 0) return 0;
			int tGiven = Math.min(aAmount, mStored);
			if (!aSimulate) mStored -= tGiven;
			return tGiven;
		}
	}

	@Test
	public void extractThenInsertConservesFeAcrossTheRatio() {
		// source holds 1000 FE (7 whole 128-FE packets + 104 remainder): the pull takes the 7
		// whole packets, the sink receives exactly the FE the source lost, nothing is created
		// or destroyed — the lossless round trip the converter machine's accounting relies on.
		Battery tSink = new Battery();
		RecordingSource tSrc = new RecordingSource(1000);
		long tPackets = EnergyBridge.extractFe(tSrc, 32, 8);
		assertEquals(7, tPackets, "8 requested, only 7 whole packets exist in 1000 FE");
		assertEquals(7, EnergyBridge.pushPacketTrain(tSink, 128, tPackets));
		assertEquals(896, tSink.mStored, "the sink gained exactly packets x packet size");
		assertEquals(104, tSrc.mStored, "the source lost exactly what the sink gained");
	}

	@Test
	public void insertThenExtractIsLosslessOnWholePackets() {
		// the exact chain the converter machine performs: push FE packets in, pull EU packets
		// back out — whole packets round-trip with zero loss under the 128-FE packet unit.
		Battery tBattery = new Battery();
		assertEquals(3, EnergyBridge.pushPacketTrain(tBattery, 128, 3)); // 384 FE in
		assertEquals(384, tBattery.mStored);
		assertEquals(3, EnergyBridge.extractFe(tBattery, 32, 10)); // all 3 packets back out
		assertEquals(0, tBattery.mStored, "whole packets round-trip with zero loss");
	}
}
