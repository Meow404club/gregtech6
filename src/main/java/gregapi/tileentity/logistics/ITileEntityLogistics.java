/**
 * Ported from GregTech 6 (1.7.10), file gregapi/tileentity/logistics/ITileEntityLogistics.java
 * (upstream 29 lines, the :27-28 interface face), by task p32-logistics-lv2.
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

package gregapi.tileentity.logistics;

/**
 * The logistics-network node face — 1.20.1 port of the upstream single-method interface.
 * Upstream implementors: MultiTileEntityLogisticsCore (:689), MultiTileEntityWireLogistics
 * (MultiTileEntityWireLogistics.java:40, "Logistics Wire" id 24901), MultiBlockPart (:679),
 * the MassStorage base (:501) and BarrelLogistics (:41) — the wireless adjacency network
 * seeds and BFS-walks exactly through these members (research.p31-logistics node_interface).
 *
 * <p>Port deviation (declared, the ITileEntityTemperature precedent): upstream extends
 * ITileEntityCoverable; the root pure-logic layer carries no such interface (the cover
 * lifecycle rides the mdk ICoverableTE), so this is a plain interface. The method
 * signature is upstream-verbatim.
 */
public interface ITileEntityLogistics {
	/**
	 * Upstream :28 — whether this node participates in the logistics network on aSide.
	 * Connectors answer {@code connected(aSide) || SIDES_INVALID[aSide]} (the
	 * MultiTileEntityWireLogistics :47 form): an open end refuses, an invalid side index
	 * (the SIDE_ANY = 6 family query, e.g. the AbstractCoverAttachmentLogistics :40
	 * placement gate) answers unconditionally.
	 */
	boolean canLogistics(byte aSide);
}
