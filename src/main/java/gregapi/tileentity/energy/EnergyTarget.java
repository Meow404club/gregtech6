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

/**
 * Pure-data carrier for one adjacent energy receiver: the receiver object plus the side to
 * insert energy into. Root-level counterpart of the data half of the upstream
 * DelegatorTileEntity&lt;TileEntity&gt; (mTileEntity + mSideOfTileEntity), without the World
 * coupling, so {@link ITileEntityEnergy.Util} can dispatch without a Minecraft import.
 *
 * receiver is typed Object on purpose: non-GregTech receivers are legal targets and get routed
 * through {@link EnergyBridge} by the dispatch chain.
 *
 * Port deviation (behavior-preserving): record instead of the upstream DelegatorTileEntity
 * class; the upstream wrapper also carried World/coords access, which is what the
 * {@link IEnergyAdjacency} seam now provides from the outside.
 */
public record EnergyTarget(Object receiver, byte side) {
}
