/**
 * Ported from GregTech 6 (1.7.10), file gregapi/oredict/OreDictItemData.java:20-194
 * (the pure-logic model: flag group + component stacks :34-48, the convenience constructors
 * :50-95, the aggregation constructor :101-132, the validity predicate family :134-142,
 * the material-stack/weights views :145-164, getByProduct :167-169 and copy :175-187).
 * Ported by task component-data-model.
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

package gregapi.oredict;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Gregorius Techneticies
 *
 * Pure-logic port of the OreDictItemData model: zero MC-coupled members, offline testable.
 * Everything kept here is upstream verbatim (upstream OreDictItemData.java line references
 * in the member comments).
 *
 * Port deviations from upstream (MC 1.7.10), per the pure-logic policy of the sibling classes:
 * - mUnificationTarget (:39, ItemStack) and getStack(long) (:171-173, ItemStack creation) are
 *   cut per the MC coupling policy (same precedent as OreDictPrefix.mat(...), see the
 *   OreDictPrefix.java header). The ItemStack-keyed central storage face (component-central-face
 *   card) re-adds that seam on top of this model.
 * - OM.stack(...) (upstream gregapi/util/OM.java:485-487 = null-check + new OreDictMaterialStack)
 *   is inlined at every former call site; gregapi/util/OM.java itself is not ported.
 * - Upstream ArrayListNoNulls (gregapi/code) is not ported; plain ArrayList is used instead —
 *   every add site here is null-guarded or fed from non-null sources, so the observable behavior
 *   is identical (OreDictPrefix.java:112 precedent). CS.ZL_MS / CS.ZL_OREDICTITEMDATA become the
 *   local zero-length array constant below (CS is outside this card's FILES_SCOPE).
 * - The deprecated hasValid* trio (:191-193) is cut: the validity predicate family (:134-142)
 *   is their documented replacement and the port has no legacy callers.
 */
public class OreDictItemData {
	/** Upstream CS.ZL_MS, local because CS is outside this card's FILES_SCOPE. */
	private static final OreDictMaterialStack[] ZL_MS = new OreDictMaterialStack[0];

	public boolean mBlackListed = F; // :35
	public boolean mBlocked = F; // :36
	public boolean mUseVanillaDamage = F; // :37
	public boolean mFurnaceFuel = T; // :38

	/** The OreDictPrefix if there is one assigned to this. */ // :41-42
	public final OreDictPrefix mPrefix;
	/** The OreDictMaterialStack containing the Main Material of this Item. The Amount is in Material Units (U) */ // :43-44
	public final OreDictMaterialStack mMaterial;
	/** The OreDictMaterialStack containing the remaining Byproduct Materials of this Item. The Amount is in Material Units (U). */ // :45-46
	public final OreDictMaterialStack[] mByProducts;
	/** Caching the toString result. */ // :47-48
	public final String mOreDictName;

	/** Upstream :50-55, verbatim (OM.stack inlined). The Prefix byproduct stacks are stored by reference (:54), exactly like upstream. */
	public OreDictItemData(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		mPrefix = aPrefix;
		mMaterial = aMaterial==null?null:new OreDictMaterialStack(aMaterial, aPrefix.mAmount);
		mOreDictName = (aMaterial == null ? "" : aPrefix.mNameInternal + aMaterial.mNameInternal);
		mByProducts = aPrefix.mByProducts.toArray(new OreDictMaterialStack[0]); // :54 (the isEmpty()?ZL_MS: ternary is covered by toArray on an empty List)
	}

	/** Upstream :57-71, verbatim. Blacklisted data without a Prefix; the input stacks are cloned, null byproducts are dropped. */
	public OreDictItemData(OreDictMaterialStack aMaterial, OreDictMaterialStack... aByProducts) {
		mPrefix = null;
		mMaterial = aMaterial==null?null:aMaterial.clone();
		mOreDictName = "";
		mBlackListed = T;
		if (aByProducts == null) {
			mByProducts = ZL_MS;
		} else {
			OreDictMaterialStack[] tByProducts = aByProducts.length<1?ZL_MS:new OreDictMaterialStack[aByProducts.length];
			int j = 0;
			for (int i = 0; i < aByProducts.length; i++) if (aByProducts[i] != null) tByProducts[j++] = aByProducts[i].clone();
			mByProducts = j>0?new OreDictMaterialStack[j]:ZL_MS;
			for (int i = 0; i < mByProducts.length; i++) mByProducts[i] = tByProducts[i];
		}
	}

	/** Upstream :73-78, verbatim (OM.stack inlined). */
	public OreDictItemData(OreDictMaterial aMaterial, OreDictPrefix aAmount, OreDictMaterialStack... aByProducts) {
		this(aMaterial==null||aAmount==null?null:new OreDictMaterialStack(aMaterial, aAmount.mAmount), aByProducts);
	}
	public OreDictItemData(OreDictMaterial aMaterial, long aAmount, OreDictMaterialStack... aByProducts) {
		this(aMaterial==null?null:new OreDictMaterialStack(aMaterial, aAmount), aByProducts);
	}

	/** Upstream :80-91, verbatim (OM.stack inlined). */
	public OreDictItemData(OreDictMaterial aMaterial, OreDictPrefix aAmount, OreDictMaterial aByProduct, OreDictPrefix aByProductAmount) {
		this(aMaterial==null||aAmount==null?null:new OreDictMaterialStack(aMaterial, aAmount.mAmount), aByProduct==null||aByProductAmount==null?null:new OreDictMaterialStack(aByProduct, aByProductAmount.mAmount));
	}
	public OreDictItemData(OreDictMaterial aMaterial, OreDictPrefix aAmount, OreDictMaterial aByProduct, long aByProductAmount) {
		this(aMaterial==null||aAmount==null?null:new OreDictMaterialStack(aMaterial, aAmount.mAmount), aByProduct==null?null:new OreDictMaterialStack(aByProduct, aByProductAmount));
	}
	public OreDictItemData(OreDictMaterial aMaterial, long aAmount, OreDictMaterial aByProduct, OreDictPrefix aByProductAmount) {
		this(aMaterial==null?null:new OreDictMaterialStack(aMaterial, aAmount), aByProduct==null||aByProductAmount==null?null:new OreDictMaterialStack(aByProduct, aByProductAmount.mAmount));
	}
	public OreDictItemData(OreDictMaterial aMaterial, long aAmount, OreDictMaterial aByProduct, long aByProductAmount) {
		this(aMaterial==null?null:new OreDictMaterialStack(aMaterial, aAmount), aByProduct==null?null:new OreDictMaterialStack(aByProduct, aByProductAmount));
	}

	/** Upstream :93-95, verbatim (OM.stack inlined). */
	public OreDictItemData(OreDictMaterial aMaterial, long aAmount, OreDictMaterial aByProduct, long aByProductAmount, OreDictMaterial aByProduct2, long aByProductAmount2) {
		this(aMaterial==null?null:new OreDictMaterialStack(aMaterial, aAmount), aByProduct==null?null:new OreDictMaterialStack(aByProduct, aByProductAmount), aByProduct2==null?null:new OreDictMaterialStack(aByProduct2, aByProductAmount2));
	}

	/** Upstream :97-99, verbatim. */
	public OreDictItemData(Collection<OreDictItemData> aData) {
		this(aData.toArray(new OreDictItemData[0]));
	}

	/**
	 * Upstream :101-132, verbatim. The aggregation constructor: every input Main Material and
	 * Byproduct is first re-targeted through {@link OreDictMaterial#mTargetReversing} (:109-110),
	 * then stacks of the same Material are summed (:113-120), everything is sorted by Amount
	 * descending (:122, stable sort so equal Amounts keep insertion order) and the largest stack
	 * becomes the Main Material while the rest are the Byproducts (:124-131). Null entries are
	 * skipped (:108) and non-positive Amounts are excluded (:109-110). The result is blacklisted
	 * data without a Prefix (:102-104).
	 */
	public OreDictItemData(OreDictItemData... aData) {
		mPrefix = null;
		mOreDictName = "";
		mBlackListed = T;

		ArrayList<OreDictMaterialStack> aList = new ArrayList<>(), rList = new ArrayList<>();

		for (OreDictItemData tData : aData) if (tData != null) {
			if (tData.validMaterial() && tData.mMaterial.mAmount > 0) aList.add(new OreDictMaterialStack(tData.mMaterial.mMaterial.mTargetReversing, tData.mMaterial.mAmount));
			for (OreDictMaterialStack tMaterial : tData.mByProducts) if (tMaterial.mAmount > 0) aList.add(new OreDictMaterialStack(tMaterial.mMaterial.mTargetReversing, tMaterial.mAmount));
		}

		for (OreDictMaterialStack aMaterial : aList) {
			boolean temp = T;
			for (OreDictMaterialStack tMaterial : rList) if (aMaterial.mMaterial == tMaterial.mMaterial) {
				tMaterial.mAmount += aMaterial.mAmount;
				temp = F; break;
			}
			if (temp) rList.add(aMaterial.clone());
		}

		Collections.sort(rList, (a, b) -> a.mAmount == b.mAmount ? 0 : a.mAmount > b.mAmount ? -1 : +1); // :122

		if (rList.isEmpty()) {
			mMaterial = null;
		} else {
			mMaterial = rList.get(0);
			rList.remove(0);
		}

		mByProducts = rList.toArray(ZL_MS);
	}

	public boolean fullData        () {return validPrefix() && fullMaterial();} // :134
	public boolean listedData      () {return validPrefix() && listedMaterial();} // :135
	public boolean nonemptyData    () {return validPrefix() && nonemptyMaterial();} // :136
	public boolean validData       () {return validPrefix() && validMaterial();} // :137
	public boolean fullMaterial    () {return nonemptyMaterial() && mMaterial.mAmount > 0;} // :138
	public boolean listedMaterial  () {return validMaterial() && mMaterial.mMaterial.mID >= 0;} // :139 If it has a positive ID then it is in the List.
	public boolean nonemptyMaterial() {return validMaterial() && mMaterial.mMaterial.mID >  0;} // :140 0 happens to be the "Empty" Material
	public boolean validMaterial   () {return mMaterial != null;} // :141
	public boolean validPrefix     () {return mPrefix   != null;} // :142

	/** Utility Function for getting a List containing both, the Main Material and all the Byproduct Materials. The Amount is in Material Units (U). */ // :144-150, verbatim
	public List<OreDictMaterialStack> getAllMaterialStacks() {
		ArrayList<OreDictMaterialStack> rList = new ArrayList<>(mByProducts.length + 1);
		if (validMaterial()) rList.add(mMaterial);
		rList.addAll(Arrays.asList(mByProducts));
		return rList;
	}

	/**
	 * Utility Function for getting a List containing both, the Main Material and all the Byproduct
	 * Materials. The Amount is in Material Units (U). Upstream :152-164, verbatim: with a Prefix
	 * the Main Material is reported at the Prefix Weight ({@link OreDictPrefix#mWeight}, the
	 * per-Prefix statistic) instead of the stored Amount (:156-157); without a Prefix the stored
	 * Main Material stack is passed through by reference (:159). The Byproducts are always
	 * appended by reference (:162).
	 */ // :152-164
	public List<OreDictMaterialStack> getAllMaterialWeights() {
		ArrayList<OreDictMaterialStack> rList = new ArrayList<>(mByProducts.length + 1);
		if (validMaterial()) {
			if (validPrefix()) {
				rList.add(new OreDictMaterialStack(mMaterial.mMaterial, mPrefix.mWeight));
			} else {
				rList.add(mMaterial);
			}
		}
		rList.addAll(Arrays.asList(mByProducts));
		return rList;
	}

	/** Utility Function for getting a Byproduct Material at a certain Index, if it exists. The Amount is in Material Units (U). */ // :166-169, verbatim
	public OreDictMaterialStack getByProduct(int aIndex) {
		return aIndex>=0&&aIndex<mByProducts.length?mByProducts[aIndex]:null;
	}

	/**
	 * Upstream :175-182, verbatim minus the mUnificationTarget line (:177, ItemStack field cut).
	 * A Prefix-less copy clones the Main Material and every Byproduct (via the stack constructor);
	 * a Prefix-backed copy re-derives both from the Prefix (so its Byproduct stacks are the
	 * Prefix's own references). Quirk kept verbatim: mFurnaceFuel is NOT copied in either path,
	 * so a copy always starts at the default T (:178-180 copy only the other three flags).
	 */
	public OreDictItemData copy() {
		OreDictItemData rData = mPrefix == null ? new OreDictItemData(mMaterial, mByProducts) : new OreDictItemData(mPrefix, mMaterial.mMaterial);
		rData.mUseVanillaDamage = mUseVanillaDamage;
		rData.mBlackListed = mBlackListed;
		rData.mBlocked = mBlocked;
		return rData;
	}

	/** Upstream :184, verbatim. */
	public static OreDictItemData copy(OreDictItemData aData) {return aData == null ? null : aData.copy();}

	public OreDictItemData setUseVanillaDamage() {mUseVanillaDamage = T; return this;} // :186
	public OreDictItemData setNotFurnaceFuel() {mFurnaceFuel = F; return this;} // :187

	@Override public String toString() {return mOreDictName;} // :189
}
