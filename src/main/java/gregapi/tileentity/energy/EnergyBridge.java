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

import gregapi.code.TagData;

/**
 * Static seam for inserting energy into NON-GregTech receivers.
 *
 * Counterpart of the upstream EnergyCompat.insertEnergyInto dispatch (EnergyCompat.java:140):
 * upstream routed receivers that are not ITileEntityEnergy to the IC2 (electric) and RF
 * (RedstoneFlux) compat bridges. This port has no IC2, and ADR 2026-08-31-p7-energy-network
 * ruling 5 ruled the EU<->FE bridge out of the first wave (zero FE consumers in this
 * codebase), so the default behavior is "nothing accepts energy" (returns 0, same as upstream
 * EnergyCompat falling through all its compat branches). A bridge implementation (e.g. an
 * FE/EnergyStorage handler living in the mdk) can be registered later without touching this
 * dispatch chain.
 *
 * The guard aAmount <= 0 || aSize == 0 || aReceiver == null -> 0 is upstream
 * EnergyCompat.java:141 and applies before any registered handler runs.
 */
public final class EnergyBridge {
	private EnergyBridge() {}

	/** Handler for foreign energy systems, installed via {@link #register}. */
	public interface IEnergyBridgeHandler {
		/**
		 * @return the amount of used aAmount (same contract as
		 *         {@link ITileEntityEnergy#doEnergyInjection}).
		 */
		long insertEnergyInto(TagData aEnergyType, byte aSide, long aSize, long aAmount, Object aEmitter, Object aReceiver);
	}

	/** volatile so a bridge registered from mod init is safely visible to the server tick thread. */
	private static volatile IEnergyBridgeHandler mHandler = null;

	/**
	 * Installs the bridge implementation. Pass null to restore the default
	 * "nothing accepts energy" behavior (used by tests and mod shutdown).
	 */
	public static void register(IEnergyBridgeHandler aHandler) {
		mHandler = aHandler;
	}

	public static long insertEnergyInto(TagData aEnergyType, byte aSide, long aSize, long aAmount, Object aEmitter, Object aReceiver) {
		if (aAmount <= 0 || aSize == 0 || aReceiver == null) return 0; // upstream EnergyCompat.java:141
		IEnergyBridgeHandler tHandler = mHandler;
		return tHandler == null ? 0 : tHandler.insertEnergyInto(aEnergyType, aSide, aSize, aAmount, aEmitter, aReceiver);
	}
}
