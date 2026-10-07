/**
 * Ported from GregTech 6 (1.7.10), file gregapi/oredict/IOreDictItemDataOverrideItem.java
 * (the whole interface, verbatim shape), by task component-central-face. It is arm 2 of the
 * central component face's read chain (upstream OreDictManager.getItemData_ :694 consults it
 * on every allow-override read).
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

package gregtech6.components;

import net.minecraft.world.item.ItemStack;

import gregapi.oredict.OreDictItemData;

/**
 * An Item that describes its own OreDictItemData at read time (upstream :694:
 * {@code aStack.getItem() instanceof IOreDictItemDataOverrideItem}). The port's
 * {@link gregtech6.item.MaterialPrefixItem} and {@link gregtech6.item.GTMaterialPrefixBlockItem}
 * are the self-describing items: their (prefix, material) pair IS the data, so they answer a
 * fresh {@code new OreDictItemData(prefix, material)} per consult — the upstream
 * MultiTileEntityItemInternal:262-280 self-description shape.
 */
public interface IOreDictItemDataOverrideItem {
	/** Gets an overridden Value for the OreDict Item Data. */
	OreDictItemData getOreDictItemData(ItemStack aStack);
}
