/**
 * Ported from GregTech 6 (1.7.10), file gregapi/tileentity/logistics/
 * ITileEntityLogisticsSemiFilteredItem.java (upstream 31 lines, the :29-30 interface face),
 * by task p32-logistics-lv3.
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; and without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.tileentity.logistics;

import java.util.Collection;

import net.minecraft.world.item.ItemStack;

/**
 * The logistics semi-filtered item face — 1.20.1 port of the upstream single-method
 * interface. The Logistics Core folds every implementor's answer into the network-wide
 * protected-items set (MultiTileEntityLogisticsCore.java:280-282, the {@code tFilteredFor}
 * union the Dump face must not eject; MultiTileEntityMassStorage.java:439 is the upstream
 * implementor).
 *
 * <p>Port deviation (declared): upstream returns {@code ItemStackSet<ItemStackContainer>}
 * (the item+meta NBT-insensitive identity set); the port has no ItemStackSet on the pure
 * logic layer, so this is {@code Collection<ItemStack>} with ITEM-IDENTITY membership —
 * the same gate the port's {@code GTItemMover} filter clause uses. The upstream
 * {@code ITileEntityUnloadable} super-interface folds away (every 1.20.1 implementor is
 * a live BlockEntity).
 */
public interface ITileEntityLogisticsSemiFilteredItem {
	/**
	 * Upstream :30 — the items this node's side is semi-filtered for; null or empty = no
	 * filter. Membership is item-identity (see the class doc deviation note).
	 */
	Collection<ItemStack> getLogisticsFilter(byte aSide);
}
