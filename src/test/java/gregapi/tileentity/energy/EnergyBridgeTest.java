/**
 * Task p26-eu-bridge-outbound: the MC-free EU->FE outbound bridge math of
 * {@link EnergyBridge} (the root half of the bridge; the mdk per-leg handlers are
 * platform-wired and live in the mdk tree).
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Evidence anchors: upstream EnergyCompat.java:210-216 (the RF branch), :212 UT.Code.bind31,
 * :212-213 divup(aSize*RF_PER_EU), :147 aSize abs; GTCEu FeCompat.java:61-65 (insertEu two-call
 * rounding alignment); this repo CS.java:54 RF_PER_EU = 4; the register/restore contract is
 * already pinned by ITileEntityEnergyTest — this class covers the NEW math only.
 */
public class EnergyBridgeTest {

	@AfterEach
	public void restoreDefaultSeam() {
		EnergyBridge.register(null); // the ITileEntityEnergyTest.restoreDefaultEnergyBridge discipline
	}

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
		// the research-card risk ② shape: VMAX-top EU times amps times 4 overflows int math
		assertEquals(Integer.MAX_VALUE, EnergyBridge.bind31((1L << 34) * 4));
	}

	// ---------------------------------------------------------------------------
	// the 4:1 conversion table (RF_PER_EU = 4, CS.java:54)
	// ---------------------------------------------------------------------------

	@Test
	public void conversionTableFourFePerEu() {
		// 1 packet of v EU billed as 1 when fully accepted; the FE sink receives v*4.
		RecordingSink tSink = new RecordingSink(100000);
		assertEquals(1, EnergyBridge.insertFe(tSink, 32, 1));
		assertEquals(128, tSink.mStored, "32 EU x 4 = 128 FE");

		// a multi-packet train: amps x volts x 4
		RecordingSink tSink2 = new RecordingSink(100000);
		assertEquals(5, EnergyBridge.insertFe(tSink2, 32, 5));
		assertEquals(640, tSink2.mStored, "5 amps x 32 EU x 4 = 640 FE");

		// upstream :147: a negative (directional) size is bridged by its magnitude
		RecordingSink tSink3 = new RecordingSink(100000);
		assertEquals(1, EnergyBridge.insertFe(tSink3, -32, 1));
		assertEquals(128, tSink3.mStored);

		// the zero/no-op guards (the :141 family)
		assertEquals(0, EnergyBridge.insertFe(new RecordingSink(100), 32, 0));
		assertEquals(0, EnergyBridge.insertFe(new RecordingSink(100), 0, 5));
		assertEquals(0, EnergyBridge.insertFe(null, 32, 5));
	}

	@Test
	public void theTwoCallShapeIsSimulateThenAlignedRealInsert() {
		RecordingSink tSink = new RecordingSink(100000);
		EnergyBridge.insertFe(tSink, 32, 2); // wants 256 FE, gets it whole
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
		// 32 EU packets (128 FE each); only 200 FE fit -> aligned send is 128 FE (1 packet),
		// NOT 200 FE billed as 2 packets (the divup over-bill the alignment removes).
		RecordingSink tSink = new RecordingSink(200);
		assertEquals(1, EnergyBridge.insertFe(tSink, 32, 4));
		assertEquals(128, tSink.mStored, "the partial packet's 72 FE stay with GT, not gifted to FE");
	}

	@Test
	public void tinySinkTakingLessThanOnePacketBridgesNothing() {
		// 100 FE room < one 128 FE packet: aligned send is 0, the real call never fires (wait —
		// it does fire with 0; the contract return 0 bills no packets).
		RecordingSink tSink = new RecordingSink(100);
		assertEquals(0, EnergyBridge.insertFe(tSink, 32, 4));
		assertEquals(0, tSink.mStored);
	}

	@Test
	public void aFullSinkRejectsTheTrainWithZeroCallsAfterSimulate() {
		RecordingSink tSink = new RecordingSink(5000);
		tSink.mStored = 5000; // pre-fill
		assertEquals(0, EnergyBridge.insertFe(tSink, 32, 4));
		assertEquals(1, tSink.mCalls.size(), "only the simulate call happens; no zero-amount real call");
	}

	// ---------------------------------------------------------------------------
	// overflow protection (the long-domain product + bind31 clamp)
	// ---------------------------------------------------------------------------

	@Test
	public void overflowingTrainIsClampedNotWrapped() {
		// amps * volts * 4 far beyond int range: the request clamps to Integer.MAX_VALUE,
		// the bill stays within what the sink accepted (never a wrapped negative).
		// 2^20 EU packets = 4194304 FE each; the sink takes 96468992 FE = exactly 23 packets.
		RecordingSink tSink = new RecordingSink(100000000);
		long tUsed = EnergyBridge.insertFe(tSink, 1L << 20, 1L << 20);
		assertTrue(tUsed > 0, "the clamped train still bridges");
		assertEquals(96468992, tSink.mStored, "the send stops on the last whole packet");
		assertEquals(23, tUsed);
	}

	@Test
	public void aPacketBiggerThanTheWholeSinkBridgesNothing() {
		// the alignment extreme: one packet (4M FE) does not fit a 1M FE sink at all, so the
		// aligned send is zero and NOTHING is sent — the upstream one-liner would have gifted
		// the 1M FE while billing a full packet (the loss the alignment removes).
		RecordingSink tSink = new RecordingSink(1000000);
		assertEquals(0, EnergyBridge.insertFe(tSink, 1L << 20, 1L << 20));
		assertEquals(0, tSink.mStored);
	}

	@Test
	public void divupBillRoundsUpWhenTheSinkMisbehaves() {
		// a hostile sink that accepts a non-aligned amount on the real call (contract-breaking
		// but legal interface): the upstream divup bill still rounds up, never under-bills.
		EnergyBridge.IFEReceiver tWeird = new EnergyBridge.IFEReceiver() {
			@Override
			public int receiveEnergy(int aAmount, boolean aSimulate) {
				return aSimulate ? aAmount : aAmount - 3; // returns a non-multiple of the packet
			}
		};
		// 2 packets of 32 EU = 256 FE; real call returns 253 -> divup(253, 128) = 2
		assertEquals(2, EnergyBridge.insertFe(tWeird, 32, 2));
	}

	// ---------------------------------------------------------------------------
	// the gate (upstream :210 with isElectricRFReceiver -> capability presence)
	// ---------------------------------------------------------------------------

	@Test
	public void gateKeepsTheUpstreamStructureWithTheModernWhitelist() {
		// shipped default EMIT_EU_AS_RF = F (GT_API.java:505): capability presence decides
		assertFalse(EnergyBridge.EMIT_EU_AS_RF, "the shipped default keeps the upstream config F");
		assertTrue(EnergyBridge.gateFE(true), "a receiver exposing FE (the modern whitelist) is bridged");
		assertFalse(EnergyBridge.gateFE(false), "a receiver without FE is not");
	}

	// ---------------------------------------------------------------------------
	// the registered-handler path (only the EU type is bridged; others fall through)
	// ---------------------------------------------------------------------------

	@Test
	public void theMdkHandlerContractBridgesEuAndBillsPackets() {
		// the exact shape the mdk handlers install: resolve FE first, gate, then insertFe.
		List<String> tLog = new ArrayList<>();
		EnergyBridge.register((aType, aSide, aSize, aAmount, aEmitter, aReceiver) -> {
			tLog.add(aType.mName + "@" + aSide);
			if (aType != gregapi.data.TD.Energy.EU) return 0;
			boolean tHasFe = (aReceiver instanceof String) && !((String)aReceiver).isEmpty(); // the capability-presence stand-in
			if (!EnergyBridge.gateFE(tHasFe)) return 0;
			return EnergyBridge.insertFe((aAmount2, aSimulate2) -> (int)Math.min(aAmount2, 700), aSize, aAmount);
		});
		// a 32 EU x 5 train into a sink capping at 700 FE: aligned send = 640 (5 packets), bill 5
		assertEquals(5, EnergyBridge.insertEnergyInto(gregapi.data.TD.Energy.EU, (byte)2, 32, 5, null, "fe-machine"));
		// a non-EU type never reaches the math
		assertEquals(0, EnergyBridge.insertEnergyInto(gregapi.data.TD.Energy.RF, (byte)2, 32, 5, null, "fe-machine"));
		// a gate-blind receiver bills nothing
		assertEquals(0, EnergyBridge.insertEnergyInto(gregapi.data.TD.Energy.EU, (byte)2, 32, 5, null, ""));
		assertTrue(tLog.contains("ENERGY.ELECTRICITY@2"), "the dispatch carries the EU TagData and the side");
		EnergyBridge.register(null);
	}
}
