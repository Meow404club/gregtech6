/**
 * Ported from GregTech 6 (1.7.10), file gregapi/tileentity/machines/ITileEntityCrucible.java
 * (upstream 29 lines, the :27-29 interface face), by task p26-crucible-physics-smeltery.
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

package gregapi.tileentity.machines;

/**
 * @author Gregorius Techneticies
 *
 * The crucible side of the crucible-to-mold pour seam. Consumers: the Smeltery BE and (card
 * p26-crucible-multiblock) the large Crucible controller implement it; the Mold BE and the
 * Faucet attachment (card p26-crucible-mold-faucet) call it. The single-method signature is
 * pinned HERE on purpose - cards B and C develop against this seam without a compile
 * dependency on this card's BlockEntities (arch ruling tasks.p26-arch-crucible-chain ③).
 *
 * Port deviation (declared): upstream extends ITileEntityUnloadable; the root pure-logic
 * layer carries no such interface (the unloadable lifecycle checks ride the mdk
 * TileEntityBase01Root), so this is a plain interface. The method signature itself is
 * upstream-verbatim.
 */
public interface ITileEntityCrucible {
	/**
	 * Upstream :28 verbatim. Fills the given adjacent Mold from this crucible's molten
	 * content.
	 *
	 * @param aMold       the Mold to pour into
	 * @param aSide       the side of THIS crucible that faces the Mold
	 * @param aSideOfMold the side of the Mold that faces this crucible
	 * @return if any Material got transferred
	 */
	boolean fillMoldAtSide(ITileEntityMold aMold, byte aSide, byte aSideOfMold);
}
