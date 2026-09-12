/**
 * Ported from GregTech 6 (1.7.10), file gregapi/tileentity/energy/ITileEntityEnergy.java
 * (upstream 283 lines), by task p7-d1-energy-core (ADR 2026-08-31-p7-energy-network).
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

import static gregapi.data.CS.*;

import java.util.Collection;

import gregapi.code.TagData;

/**
 * @author Gregorius Techneticies
 *
 * Interface for getting connected to any Energy Network.
 *
 * Note:
 * A correct implementation of this is only required Server Side. In 1.20.1 all BlockEntity
 * ticking happens on the single server thread, so unlike upstream 1.7.10 (where the IC2 energy
 * net could reach these methods from other threads) these functions are only invoked from the
 * server-side BE tick path of this port. Implementations should still defer destructive
 * reactions: do not set Blocks or explode things directly, just set a flag to do it in your own
 * next tick. The default implementations carried by the mdk TileEntityBase01Root counterpart
 * keep the upstream "synchronized" modifier on doEnergyInjection/doEnergyExtraction out of that
 * same habit.
 *
 * This is the moved Version of the Interface (the deprecated shell
 * gregapi.tileentity.ITileEntityEnergy, upstream ITileEntityEnergy.java:33, only existed to
 * catch 1.7.10 legacy callers and is NOT ported - there are no such callers in 1.20.1).
 *
 * Port deviations (behavior-preserving on the pure path):
 * - Zero Minecraft imports: the neighbor lookup of Util.emitEnergyToSide (upstream
 *   :249 DelegatorTileEntity/WD.te) is externalized into the {@link IEnergyAdjacency} seam,
 *   with {@link EnergyTarget} as the pure-data carrier, and the non-GregTech fallback of
 *   Util.insertEnergyInto (upstream :265 EnergyCompat) lands on the {@link EnergyBridge}
 *   static seam. The DelegatorTileEntity overload of insertEnergyInto (upstream :279-281)
 *   is replaced by passing the {@link EnergyTarget} fields directly.
 * - The marker interfaces of the upstream energy family (FluxHandler/ElectricityAcceptor/
 *   Emitter/DataConductor/DataCapacitor) and the capacitor methods of the Root default block
 *   are not ported (ADR ruling 1: IC2-era API surface, no counterpart, pool).
 */
public interface ITileEntityEnergy {
	/**
	 * You do not have to check for this Function, this is only for things like Energy Network optimisation and similar.
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aEmitting if it is asked to emit this Energy Type, otherwise it is asked to accept this Energy Type.
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @return if this TileEntity has anything to do with this Type of Energy, depending on insert or extract request. The returning Value must be constant for this TileEntity.
	 */
	boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting);

	/**
	 * Gets all the Types of Energy, which are relevant to this TileEntity.
	 *
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (should return all related Types of Energy)
	 * @return any Type of Energy that is related to this TileEntity. This is especially useful for Data Displays and Redstone Conditions, where people can select the Energy Type via GUI or something.
	 */
	Collection<TagData> getEnergyTypes(byte aSide);

	// Connectivity

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @param aTheoretical true if this is only checking for Conductor Connections. Use it in case things like Redstone or Covers for example can toggle the connectivity of this Side for this Energy Type, but you still want to visually connect to it, even if it temporarily wouldn't accept Energy Interactions from that Side right now. Basically, so that the Conductor doesn't visually toggle on/off all the time causing Sync Lag.
	 */
	boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical);

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 * As in do not make a direct call to set Blocks or explode things, just set a flag to do it in the next tick.
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @param aTheoretical true if this is only checking for Conductor Connections. Use it in case things like Redstone or Covers for example can toggle the connectivity of this Side for this Energy Type, but you still want to visually connect to it, even if it temporarily wouldn't accept Energy Interactions from that Side right now. Basically, so that the Conductor doesn't visually toggle on/off all the time causing Sync Lag.
	 */
	boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical);


	// Push based Energy.

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 * As in do not make a direct call to set Blocks or explode things, just set a flag to do it in the next tick.
	 *
	 * Inject Energy Call for Electricity. Gets called by EnergyEmitters to inject Energy into your Block
	 *
	 * Note: The IMPLEMENTOR of this Function has to check for isEnergyAcceptingFrom, when implementing this Function, NOT the one injecting the Energy, because the Network won't check for that by itself.
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aDoInject if this is supposed to increase the internal Energy. true = Yes, this is a normal Operation. false = No, this is just a simulation.
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @return amount of used aAmount. 0 if not accepted anything.
	 */
	long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject);

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 *
	 * Some Energy Networks use this Value to pre-calculate the Energy Flow.
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @param aSize the Energy Packet Size to be demanded. getEnergySizeInputRecommended is the recommended Power Level.
	 * @return The Amount of Energy Packets of aSize Size this TileEntity needs
	 */
	long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize);


	// Pull based Energy.

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 * As in do not make a direct call to set Blocks or explode things, just set a flag to do it in the next tick.
	 *
	 * Extract Energy Call for Electricity. Gets called by Energy Networks to extract Energy from your Block
	 *
	 * Note: The IMPLEMENTOR of this Function has to check for isEnergyEmittingTo, when implementing this Function, NOT the one extracting the Energy, because the Network won't check for that by itself.
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aDoExtract if this is supposed to decrease the internal Energy. true = Yes, this is a normal Operation. false = No, this is just a simulation.
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @return amount of taken aAmount. 0 if not accepted anything.
	 */
	long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract);

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 *
	 * Some Energy Networks use this Value to pre-calculate the Energy Flow.
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @param aSize the Energy Packet Size to be taken. getEnergySizeOutputRecommended is the recommended Power Level.
	 * @return The Amount of Energy Packets of aSize Size this TileEntity offers
	 */
	long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize);


	// Parameters for the minimum Packet Sizes in order for things to actually work.

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @return The minimum Energy Packet Size for the INPUT of this Object.
	 */
	long getEnergySizeInputMin(TagData aEnergyType, byte aSide);

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @return The minimum Energy Packet Size for the OUTPUT of this Object.
	 */
	long getEnergySizeOutputMin(TagData aEnergyType, byte aSide);


	// Parameters for the recommended Packet Sizes.

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @return The recommended Energy Packet Size for the INPUT of this Object.
	 */
	long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide);

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @return The recommended Energy Packet Size for the OUTPUT of this Object.
	 */
	long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide);


	// Parameters for the maximum Packet Sizes before bad shit happens. Anything greater than this Value would cause an explosion in a GregTech Machine. Just saying.

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @return The maximum Energy Packet Size for the INPUT of this Object.
	 */
	long getEnergySizeInputMax(TagData aEnergyType, byte aSide);

	/**
	 * Single-threaded in this port (server-side BE tick path only, see the interface note).
	 *
	 * @param aEnergyType The Type of Energy
	 * @param aSide 0 - 5 = Vanilla Directions of the Implementors Block. 6 = No specific Side (don't do Side checks for this Side)
	 * @return The maximum Energy Packet Size for the OUTPUT of this Object.
	 */
	long getEnergySizeOutputMax(TagData aEnergyType, byte aSide);

	/** Utility for the Energy Networks */
	public static class Util {
		/**
		 * The six vanilla sides, upstream CS.java:671 ALL_SIDES_VALID = {0,1,2,3,4,5}. Carried
		 * locally because this card's CS increment policy only allows three named constants;
		 * the side set itself has no consumer outside this loop so far (pool).
		 */
		private static final byte[] ALL_SIDES_VALID = {0, 1, 2, 3, 4, 5};

		/**
		 * Emits Energy to all the Blocks adjacent to an Output Side.
		 * Receivers that are not ITileEntityEnergy accept NOTHING: the EU->FE outbound
		 * bridge was cut (task p28-cut-eu-fe-bridge, decision
		 * decisions.p28-cut-eu-fe-bridge), so the dispatch falls through the way upstream
		 * EnergyCompat.insertEnergyInto did when no compat branch matched.
		 *
		 * @param aEnergyType The Type of Energy to be emitted
		 * @param aSize The Minimum Transfer Rate of Energy (like Voltage for example). This can be negative too in case it has a direction for example (clockwise/counterclockwise)
		 * @param aAmount The Amount of Packets in size of aSize to be emitted (like Amperage for example)
		 * @param aEmitter The Emitter of the Energy.
		 * @param aAdjacency resolves the neighbors of aEmitter; with no emitting adjacency nothing is used.
		 * @return the amount of used Energy Packets.
		 */
		public static final long emitEnergyToNetwork(TagData aEnergyType, long aSize, long aAmount, ITileEntityEnergy aEmitter, IEnergyAdjacency aAdjacency) {
			long rUsedAmount = 0;
			for (byte tSide : ALL_SIDES_VALID) if (aEmitter.isEnergyEmittingTo(aEnergyType, tSide, F)) {
				rUsedAmount += emitEnergyToSide(aEnergyType, tSide, aSize, aAmount-rUsedAmount, aEmitter, aAdjacency);
				if (aAmount <= rUsedAmount) break;
			}
			return rUsedAmount;
		}

		/**
		 * Emits Energy to the adjacent Block.
		 * Receivers that are not ITileEntityEnergy accept NOTHING: the EU->FE outbound
		 * bridge was cut (task p28-cut-eu-fe-bridge, decision
		 * decisions.p28-cut-eu-fe-bridge), so the dispatch falls through the way upstream
		 * EnergyCompat.insertEnergyInto did when no compat branch matched.
		 *
		 * @param aEnergyType The Type of Energy to be emitted
		 * @param aSideOutOf The Side of the TileEntity to output Energy out of.
		 * @param aSize The Minimum Transfer Rate of Energy (like Voltage for example). This can be negative too in case it has a direction for example (clockwise/counterclockwise)
		 * @param aAmount The Amount of Packets in size of aSize to be emitted (like Amperage for example)
		 * @param aEmitter The Emitter of the Energy. May be null!
		 * @param aAdjacency resolves the neighbor of aEmitter; null means nothing is adjacent (upstream: a null DelegatorTileEntity target reaching the EnergyCompat null guard, EnergyCompat.java:141).
		 * @return the amount of used Energy Packets.
		 */
		public static final long emitEnergyToSide(TagData aEnergyType, byte aSideOutOf, long aSize, long aAmount, Object aEmitter, IEnergyAdjacency aAdjacency) {
			if (aAdjacency == null) return 0;
			EnergyTarget tTarget = aAdjacency.adjacent(aSideOutOf); // upstream ITileEntityEnergy.java:249 (DelegatorTileEntity acquisition)
			return tTarget == null ? 0 : insertEnergyInto(aEnergyType, tTarget.side(), aSize, aAmount, aEmitter, tTarget.receiver());
		}

		/**
		 * Inserts Energy into the receiver.
		 * GregTech receivers get doEnergyInjection directly, everything else accepts nothing
		 * (upstream: EnergyCompat.insertEnergyInto falling through its compat branches; the
		 * EnergyBridge seam that used to catch foreign receivers here was cut with the EU->FE
		 * outbound bridge, task p28-cut-eu-fe-bridge — foreign pushes go through the
		 * ratio-agnostic EnergyBridge.pushPacketTrain faces of their own machines instead).
		 *
		 * @param aEnergyType The Type of Energy to be emitted
		 * @param aSideInto The Side of the receiving TileEntity to insert the Energy into.
		 * @param aSize The Minimum Transfer Rate of Energy (like Voltage for example). This can be negative too in case it has a direction for example (clockwise/counterclockwise)
		 * @param aAmount The Amount of Packets in size of aSize to be emitted (like Amperage for example)
		 * @param aEmitter The Emitter of the Energy. May be null!
		 * @param aReceiver The Receiver of the Energy. May be null!
		 * @return the amount of used Energy Packets.
		 */
		public static final long insertEnergyInto(TagData aEnergyType, byte aSideInto, long aSize, long aAmount, Object aEmitter, Object aReceiver) {
			return aReceiver instanceof ITileEntityEnergy ? ((ITileEntityEnergy)aReceiver).doEnergyInjection(aEnergyType, aSideInto, aSize, aAmount, T) : 0;
		}
	}
}
