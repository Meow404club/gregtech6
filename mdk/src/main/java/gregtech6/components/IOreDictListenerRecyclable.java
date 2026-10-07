/**
 * Ported from GregTech 6 (1.7.10), file gregapi/oredict/event/IOreDictListenerRecyclable.java
 * (the interface + the nested OreDictRecyclingContainer), by task component-central-face.
 * The notification gate that fires it lives on the central face's write path (upstream
 * OreDictManager.setItemData_ :666-670).
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
 * A Listener that gets notified when an Item receives recyclable component data. Upstream's
 * only implementation is RecyclingProcessing (Loader_OreProcessing.java:238-294 — the port's
 * component-recycling-recipes card); the central face additionally replays past registrations
 * from {@link OM#recyclingRegistrations()} so late listeners catch up (upstream
 * mRecyclableRegistrations :70).
 */
public interface IOreDictListenerRecyclable {
	void onRecycleableRegistration(OreDictRecyclingContainer aEvent);

	/** The (stack, data) pair a registration carries. Upstream nested class, verbatim minus the
	 * re-read copy constructor (no port caller; the recycling card re-reads through {@link OM} directly). */
	class OreDictRecyclingContainer {
		public final OreDictItemData mItemData;
		public final ItemStack mStack;

		public OreDictRecyclingContainer(ItemStack aStack, OreDictItemData aItemData) {
			mItemData = aItemData;
			mStack = aStack.copy();
		}
	}
}
