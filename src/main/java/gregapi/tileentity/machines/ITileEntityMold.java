/**
 * Ported from GregTech 6 (1.7.10), file gregapi/tileentity/machines/ITileEntityMold.java
 * (upstream 36 lines, the :28-35 four-method face), by task p26-crucible-physics-smeltery.
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

import gregapi.oredict.OreDictMaterialStack;

/**
 * @author Gregorius Techneticies
 *
 * The mold side of the crucible-to-mold pour seam, the pair of
 * {@link ITileEntityCrucible}. Consumers: the Mold BE implements it; the Smeltery BE
 * (this card), the large Crucible (card C) and the Faucet (card B) call it. The
 * four-method signature is pinned HERE on purpose - cards B and C develop against this
 * seam without a compile dependency on this card's BlockEntities (arch ruling
 * tasks.p26-arch-crucible-chain ③).
 *
 * Port deviation (declared): upstream extends ITileEntityUnloadable; the root pure-logic
 * layer carries no such interface (the unloadable lifecycle checks ride the mdk
 * TileEntityBase01Root), so this is a plain interface. The method signatures themselves
 * are upstream-verbatim.
 */
public interface ITileEntityMold {
	/** Upstream :29 verbatim — the side(s) the Mold accepts poured material from. */
	boolean isMoldInputSide(byte aSide);

	/** Upstream :31 — The Maximum Temperature this Mold can accept without melting itself. */
	long getMoldMaxTemperature();

	/** Upstream :33 — The Amount of Material required to fill the Mold completely. */
	long getMoldRequiredMaterialUnits();

	/**
	 * Upstream :35 — Pour material into the Mold. The first Parameter can also have a
	 * Material Amount higher or lower than the actually required Amount, usually only in
	 * case it is done manually.
	 *
	 * @return the amount of Material subtracted from the passed MaterialStack
	 */
	long fillMold(OreDictMaterialStack aMaterial, long aTemperature, byte aSide);
}
