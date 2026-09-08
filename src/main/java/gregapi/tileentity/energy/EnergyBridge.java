/**
 * Ported from GregTech 6 (1.7.10), task p7-d1-energy-core (ADR 2026-08-31-p7-energy-network).
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

import java.util.function.Predicate;

import gregapi.code.TagData;
import gregapi.data.CS;
import gregapi.util.UT;

/**
 * Static seam for inserting energy into NON-GregTech receivers.
 *
 * Counterpart of the upstream EnergyCompat.insertEnergyInto dispatch (EnergyCompat.java:140):
 * upstream routed receivers that are not ITileEntityEnergy to the IC2 (electric) and RF
 * (RedstoneFlux) compat bridges. This port has no IC2, and ADR 2026-08-31-p7-energy-network
 * ruling 5 ruled the EU<->FE bridge out of the first wave (zero FE consumers in this
 * codebase), so the default behavior is "nothing accepts energy" (returns 0, same as upstream
 * EnergyCompat falling through its compat branches). A bridge implementation (the FE/EU
 * outbound handler living in the mdk) registers via {@link #register} without touching this
 * dispatch chain.
 *
 * The guard aAmount <= 0 || aSize == 0 || aReceiver == null -> 0 is upstream
 * EnergyCompat.java:141 and applies before any registered handler runs.
 *
 * <h2>The EU->FE outbound math (task p26-eu-bridge-outbound, design = research.p26-r-eu-bridge)</h2>
 *
 * The MC-free half of the bridge lives here so it is unit-testable without a platform: the
 * mdk per-leg handler resolves the receiver's platform FE storage (an {@code IEnergyStorage}
 * on both legs) and adapts it to {@link IFEReceiver}, then calls {@link #insertFe}.
 *
 * <ul>
 * <li><b>Ratio</b>: 1 EU = {@link CS#RF_PER_EU} FE (4, this repo CS.java:54 = upstream
 *     CS.java:207-208). One packet of aSize EU is therefore aSize*4 FE — the packet, not the
 *     bare ratio, is the accounting unit (upstream divides by aSize*RF_PER_EU,
 *     EnergyCompat.java:212-213).</li>
 * <li><b>Overflow</b>: aAmount*aSize*RF_PER_EU is computed in the long domain and clamped
 *     into int range by {@link #bind31} (upstream UT.Code.bind31, UT.java:1564, called at
 *     EnergyCompat.java:212) — EU packs reach VMAX 2^34 where naive int math overflows
 *     (the research card risk ②). GTCEu saturatedCast (GTMath.java:125) is the same guard
 *     family.</li>
 * <li><b>Rounding alignment</b>: the simulated acceptance is rounded DOWN to a whole packet
 *     before the real insert (GTCEu FeCompat.insertEu's {@code feSent - feSent % ratio}
 *     form, FeCompat.java:63-64, lifted from the bare ratio to the packet unit) — without it
 *     a partially-accepted packet would be billed as a full one via divup and the FE side
 *     would silently lose the remainder. The final bill stays upstream's
 *     {@code divup(sent, aSize*RF_PER_EU)} (EnergyCompat.java:212-213), which is exact when
 *     the send was packet-aligned.</li>
 * <li><b>Gate</b>: upstream EnergyCompat.java:210 {@code RF_ENERGY && (EMIT_EU_AS_RF ||
 *     isElectricRFReceiver(aReceiver))}. RF_ENERGY was a CoFH-API-on-classpath probe — always
 *     true in a modern platform where EnergyStorage ships with the loader. The class-name
 *     whitelist isElectricRFReceiver (:89-97, four hardcoded mod prefixes) has a strictly
 *     better modern equivalent: capability presence — a receiver IS an RF machine exactly
 *     when it exposes an FE EnergyStorage. {@link #gateFE} keeps the upstream structure with
 *     that substitution; {@link #EMIT_EU_AS_RF} keeps the upstream config's shipped default F
 *     (GT_API.java:505, CS.java:866), carried as a compile-time constant until the config
 *     system lands (research card pooling note).</li>
 * <li><b>DECLARED DEVIATION (deviation ledger)</b>: upstream's checkOverCharge
 *     (EnergyCompat.java:129-137) destroyed and exploded foreign receivers fed packets above
 *     VMAX[3] = 512. Cut, following the GTCEu native-outbound precedent (its EUToFEProvider
 *     path has no overcharge explosion either — modern FE has no voltage concept to violate;
 *     research card gtceu_reference.no_overcharge_foreign). GT-side machines keep their own
 *     overcharge path (Root :494-509) — only the FOREIGN side is affected.</li>
 * </ul>
 */
public final class EnergyBridge {
	private EnergyBridge() {}

	/**
	 * Upstream config "Emit_EU_as_RF_from_Blocks", shipped default F (GT_API.java:505,
	 * CS.java:866). F = only receivers exposing an FE capability are bridged (the modern
	 * isElectricRFReceiver whitelist); T = the gate is fully open and every dispatch attempts
	 * the query. Carried as a compile-time constant; moves into the config system when it
	 * lands (research.p26-r-eu-bridge phasing).
	 */
	public static final boolean EMIT_EU_AS_RF = CS.F;

	/** Handler for foreign energy systems, installed via {@link #register}. */
	public interface IEnergyBridgeHandler {
		/**
		 * @return the amount of used aAmount (same contract as
		 *         {@link ITileEntityEnergy#doEnergyInjection}).
		 */
		long insertEnergyInto(TagData aEnergyType, byte aSide, long aSize, long aAmount, Object aEmitter, Object aReceiver);
	}

	/**
	 * The MC-free FE face of the bridge: the only platform method the math needs. Both legs'
	 * {@code IEnergyStorage} (forge-1.20.1 IEnergyStorage.java:31-64 and neoforge 21.1.249
	 * IEnergyStorage, javap-verified six-method twins) adapt with a two-argument lambda.
	 */
	public interface IFEReceiver {
		/** Same contract as the platform method: energy accepted (or would be, simulated). */
		int receiveEnergy(int aAmount, boolean aSimulate);
	}

	/** volatile so a bridge registered from mod init is safely visible to the server tick thread. */
	private static volatile IEnergyBridgeHandler mHandler = null;

	/**
	 * The theoretical connect probe (see {@link #bridgesForeign}): volatile for the same
	 * reason as the handler. Both arms are installed together by the mod init; a null
	 * probe (the shipped state) means "no foreign connections are bridgeable".
	 */
	private static volatile Predicate<Object> mForeignConnectProbe = null;

	/**
	 * Installs the bridge implementation. Pass null to restore the default
	 * "nothing accepts energy" behavior (used by tests and mod shutdown).
	 */
	public static void register(IEnergyBridgeHandler aHandler) {
		mHandler = aHandler;
	}

	/**
	 * Installs the foreign-connect probe the conductor handshakes consult (the wire
	 * {@code canConnect} of the p7-d2 port): a platform-side test of whether a NON-GT
	 * receiver exposes a bridgeable FE face. Runs with no packet in flight — capability
	 * presence only, no energy moved.
	 */
	public static void registerForeignConnectProbe(Predicate<Object> aProbe) {
		mForeignConnectProbe = aProbe;
	}

	public static long insertEnergyInto(TagData aEnergyType, byte aSide, long aSize, long aAmount, Object aEmitter, Object aReceiver) {
		if (aAmount <= 0 || aSize == 0 || aReceiver == null) return 0; // upstream EnergyCompat.java:141
		IEnergyBridgeHandler tHandler = mHandler;
		return tHandler == null ? 0 : tHandler.insertEnergyInto(aEnergyType, aSide, aSize, aAmount, aEmitter, aReceiver);
	}

	/**
	 * The theoretical half of the RF connection, upstream EnergyCompat.canConnectElectricity
	 * :124 — {@code (EMIT_EU_AS_RF || isElectricRFReceiver(aTarget)) && IEnergyHandler/Receiver}.
	 * The class-name whitelist became capability presence (the handler's gate), which only the
	 * platform can answer, so the probe rides the seam next to the handler and this wrapper
	 * applies the same {@link #gateFE}: a foreign receiver IS a connection target exactly when
	 * the bridge would attempt it. No probe installed (the shipped pre-bridge state) → false,
	 * the conductor behaviour the port shipped with before this card.
	 */
	public static boolean bridgesForeign(Object aReceiver) {
		if (aReceiver == null) return false;
		Predicate<Object> tProbe = mForeignConnectProbe;
		return tProbe != null && gateFE(tProbe.test(aReceiver));
	}

	// ---------------------------------------------------------------------------
	// the EU->FE outbound math (MC-free, unit-tested in EnergyBridgeTest)
	// ---------------------------------------------------------------------------

	/** Upstream UT.Code.bind31 (UT.java:1564 verbatim): clamp [0, Integer.MAX_VALUE] and narrow. */
	public static int bind31(long aBoundValue) {
		return (int) Math.max(0, Math.min(2147483647, aBoundValue));
	}

	/** The modern whitelist gate: upstream EnergyCompat.java:210 with isElectricRFReceiver replaced by capability presence. */
	public static boolean gateFE(boolean aCapabilityPresent) {
		return EMIT_EU_AS_RF || aCapabilityPresent;
	}

	/**
	 * Bridges one EU packet train into an FE storage: the upstream RF branch
	 * (EnergyCompat.java:210-216, checkOverCharge arm cut per the ledger note) fused with the
	 * GTCEu insertEu rounding alignment (FeCompat.java:61-65), the accounting unit being the
	 * whole packet (aSize*RF_PER_EU FE), not the bare ratio.
	 *
	 * @param aStorage the receiver's FE storage (platform-adapted)
	 * @param aSize the EU packet size (voltage); may be negative (signed upstream too, :147)
	 * @param aAmount the packet count (amperage)
	 * @return the amount of used EU packets (the upstream divup bill)
	 */
	public static long insertFe(IFEReceiver aStorage, long aSize, long aAmount) {
		if (aStorage == null || aAmount <= 0 || aSize == 0) return 0; // the :141 guard family, handler-side
		long tSize = Math.abs(aSize); // upstream EnergyCompat.java:147
		long tPacketFe = tSize * CS.RF_PER_EU; // one EU packet in FE (upstream's divup denominator, :212-213)
		int tFeWanted = bind31(aAmount * tPacketFe); // upstream :212 (long-domain product, clamped)
		int tFeSimulated = aStorage.receiveEnergy(tFeWanted, true);
		if (tFeSimulated <= 0) return 0; // nothing accepts: the whole train is unused
		long tAligned = tFeSimulated - tFeSimulated % tPacketFe; // GTCEu FeCompat.java:64, packet-unit form
		int tFeSent = aStorage.receiveEnergy(bind31(tAligned), false);
		return UT.Code.divup(tFeSent, tPacketFe); // upstream :212-213, exact for an aligned send
	}
}
