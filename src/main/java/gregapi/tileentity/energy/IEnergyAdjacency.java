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
 * MC-free adjacency seam for {@link ITileEntityEnergy.Util}.
 *
 * Upstream 1.7.10 resolved the neighbor of an emitting TileEntity inside the Util itself via
 * IHasWorldAndCoords.getAdjacentTileEntity(side) or WD.te(...) (upstream
 * ITileEntityEnergy.java:249), which requires a World object. This port pulls that lookup out
 * into this seam so the emit/insert dispatch chain in Util stays pure Java and offline-testable.
 * The mdk side (wire blocks, energy sources) implements this on top of Level#getBlockEntity.
 *
 * Port deviation (behavior-preserving): this interface did not exist upstream; it replaces the
 * DelegatorTileEntity acquisition half of upstream ITileEntityEnergy.java:248-251.
 */
public interface IEnergyAdjacency {
	/**
	 * Resolves the receiver adjacent to the caller on the given side.
	 *
	 * @param aSide the side of the CALLER (the emitting block) to look through, 0 - 5.
	 * @return the adjacent receiver together with the side of the RECEIVER that faces the caller
	 *         (upstream DelegatorTileEntity.mSideOfTileEntity, typically OPOS[aSide] for a plain
	 *         neighbor lookup), or null if there is nothing adjacent to push energy into.
	 */
	EnergyTarget adjacent(byte aSide);
}
