/**
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

package gregtech6.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.CruciblePhysics;
import gregtech6.components.OM;

/**
 * The shared crucible feed ladder — upstream MultiTileEntityCrucible.java:206-234, whose
 * small-crucible twin (MultiTileEntitySmeltery.java:159-183) the port had duplicated
 * byte-for-byte into both BEs (the r4-20b declared debt; the ponytail note at the old
 * TileEntityCrucible.feedStacks named exactly this consolidation). Both crucibles
 * ({@link gregtech6.tileentity.multiblocks.TileEntityCrucible} and
 * {@link gregtech6.tileentity.tools.TileEntitySmeltery}) delegate here.
 *
 * <p><b>The central-face seam (task component-crucible-feed-resolve, MS-3's first
 * domain-wide consumer):</b> upstream resolves the feed through {@code OM.anydata_}
 * (Crucible:209 + OM.java:177-179) — the item's component data decides what melts, never
 * the item class. The port's former ladder read {@code MaterialPrefixItem} fields
 * directly, which is why vanilla ingots needed hand bridges and any other data-carrying
 * stack could not feed. The ladder below consults
 * {@link OM#anydata} (the same read chain every other consumer routes through) and walks
 * the upstream arms verbatim over the READ DATA (prefix identity on
 * {@link OreDictItemData#mPrefix}, Crucible:213-232):
 * <ol>
 * <li>null data → null (the caller trashes the slot + SFX.MC_FIZZ, :210-212).</li>
 * <li>prefix-less data → every stack above zero, cloned (:213-216).</li>
 * <li>oreRaw / STANDARD_ORE → the direct-smelt projection ×1 (:217-218/:225-226).</li>
 * <li>blockRaw → ×9 (:219-220); DENSE_ORE → ×2 (:227-228).</li>
 * <li>any other prefix → the same generic emission (:229-232).</li>
 * </ol>
 * The upstream crateGtRaw/crateGt64Raw form factors (:221-224) stay cut — the port has no
 * raw-crate prefix items (the r4-20b declared shape, unchanged here).
 *
 * <p><b>DECLARED deviations</b> (inherited or pinned by the offline suite):
 * <ul>
 * <li><b>The whole-slot melt</b>: upstream melts ONE item per tick ({@code decrStackSize(0, 1)}
 *     :216/:232), the port clears the slot and melts everything in it — so every emitted
 *     amount scales by the stack count. The old ladder scaled only the generic arm (the ore
 *     arms silently destroyed the count of a hopper-pushed stack); the scaling is now
 *     uniform across every arm.</li>
 * <li><b>The vanilla-ore fallback AFTER the read chain</b>: the family-tag arm's reverse
 *     table has no ores/raw_materials family yet ({@code GT6ItemTags.itemTagFamily} covers
 *     the ingot/dust/gem/... item families only), so the nine-entry vanilla ore bridge
 *     survives here as the declared backup — the lazy call-time form verbatim (the eager
 *     static table froze pre-init nulls on the 21.1 class-init order lottery, GTWireSpecs:35).
 *     Extending itemTagFamily with the ores families is the upgrade path that deletes this
 *     bridge.</li>
 * <li><b>The vanilla ingot/nugget bridges are DELETED</b>: the read chain resolves them live
 *     through the family tags ({@code forge:ingots/iron = [minecraft:iron_ingot,
 *     gt6:ingot_iron]}, the VANILLA_INTERSECTION face — the modern oredict-registration
 *     stream upstream fed the same map from). The offline pins ride an
 *     {@link OM#sStackTags} membership stub, the OMComponentFaceTest posture.</li>
 * </ul>
 */
public final class CrucibleFeed {

	private CrucibleFeed() {}

	/**
	 * The feed projection of one stack: the materials that melt out of it (upstream
	 * :206-234), {@code null} = the trash+fizz arm. The amounts are whole-slot (the
	 * count-scaled whole-slot melt deviation). The callers guard a non-empty stack.
	 */
	@Nullable
	public static List<OreDictMaterialStack> feedStacks(ItemStack aStack) {
		OreDictItemData tData = OM.anydata(aStack); // Crucible:209 (OM.anydata_ + the ST.invalid gate)
		if (tData == null) {
			// the declared vanilla-ore fallback (the read chain has no ores family yet —
			// see the class doc): a vanilla ore block melts as ONE standard ore of its material
			OreDictMaterial tOre = vanillaOres().get(aStack.getItem());
			if (tOre == null) return null;
			OreDictMaterialStack tDirect = CruciblePhysics.oreDirect(tOre, 1);
			tDirect.mAmount *= aStack.getCount();
			return List.of(tDirect);
		}
		List<OreDictMaterialStack> rList = new ArrayList<>();
		if (tData.mPrefix == null) { // :213-216
			genericArm(tData, aStack.getCount(), rList);
		} else if (tData.mPrefix == OP.oreRaw || tData.mPrefix.contains(TD.Prefix.STANDARD_ORE)) { // :217-218/:225-226
			rList.add(scaledOreDirect(tData, 1, aStack.getCount()));
		} else if (tData.mPrefix == OP.blockRaw) { // :219-220
			rList.add(scaledOreDirect(tData, 9, aStack.getCount()));
		} else if (tData.mPrefix.contains(TD.Prefix.DENSE_ORE)) { // :227-228
			rList.add(scaledOreDirect(tData, 2, aStack.getCount()));
		} else { // :229-232
			genericArm(tData, aStack.getCount(), rList);
		}
		rList.removeIf(tMat -> tMat.mAmount <= 0);
		return rList.isEmpty() ? null : rList;
	}

	/** The ore arm over the read data: the MAIN material's direct-smelt projection (CruciblePhysics.oreDirect = the :218 shape), count-scaled. */
	private static OreDictMaterialStack scaledOreDirect(OreDictItemData aData, long aFormFactor, long aCount) {
		OreDictMaterialStack rDirect = CruciblePhysics.oreDirect(aData.mMaterial.mMaterial, aFormFactor);
		rDirect.mAmount *= aCount;
		return rDirect;
	}

	/**
	 * The :214-215/:230-231 generic emission: every stack (main + byproducts) above zero,
	 * cloned — the stacks are the data's OWN instances (the prefix byproducts ride by
	 * reference, the central-face handed-over constraint ①), so the clone before the count
	 * scale is load-bearing.
	 */
	private static void genericArm(OreDictItemData aData, long aCount, List<OreDictMaterialStack> rList) {
		for (OreDictMaterialStack tMaterial : aData.getAllMaterialStacks()) if (tMaterial.mAmount > 0) {
			OreDictMaterialStack tClone = tMaterial.clone();
			tClone.mAmount *= aCount;
			rList.add(tClone);
		}
	}

	/**
	 * The vanilla-ore fallback bridge (the TileEntitySmeltery vanillaOres lazy form verbatim,
	 * moved here once for both crucibles): nine declared entries, call-time MT reads (the
	 * GTWireSpecs:35 rule). The read-chain-vanilla-arm upgrade path that deletes it is in the
	 * class doc.
	 */
	private static volatile Map<ItemLike, OreDictMaterial> sVanillaOres = null;

	/** The vanilla-ore bridge, built on first use (one material generation — the lazy form). */
	private static Map<ItemLike, OreDictMaterial> vanillaOres() {
		Map<ItemLike, OreDictMaterial> tTable = sVanillaOres;
		if (tTable == null) sVanillaOres = tTable = Map.of(
				net.minecraft.world.item.Items.IRON_ORE, MT.Fe,
				net.minecraft.world.item.Items.DEEPSLATE_IRON_ORE, MT.Fe,
				net.minecraft.world.item.Items.RAW_IRON, MT.Fe,
				net.minecraft.world.item.Items.GOLD_ORE, MT.Au,
				net.minecraft.world.item.Items.DEEPSLATE_GOLD_ORE, MT.Au,
				net.minecraft.world.item.Items.RAW_GOLD, MT.Au,
				net.minecraft.world.item.Items.COPPER_ORE, MT.Cu,
				net.minecraft.world.item.Items.DEEPSLATE_COPPER_ORE, MT.Cu,
				net.minecraft.world.item.Items.RAW_COPPER, MT.Cu);
		return tTable;
	}
}
