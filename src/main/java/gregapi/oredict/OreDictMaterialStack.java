/**
 * Ported from GregTech 6 (1.7.10), file gregapi/oredict/OreDictMaterialStack.java:20-103
 * (amount arithmetic, equality, list merging) plus equals/toString/hashCode (:80-96).
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

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.data.MT;
import gregapi.util.UT;

/**
 * @author Gregorius Techneticies
 *
 * Port deviations from upstream (MC 1.7.10):
 * - The Minecraft NBT compound type (upstream import at :29) is gone. save()/load() are expressed
 *   through the pure {@link MaterialStackSerializer} seam; the Default implementation keeps
 *   the upstream "a"/"i"/"m" key semantics (:105-127). The NBT/Codec binding is Phase 2.
 * - saveList/loadList (:129-160) are NOT ported: they are NBT-compound container helpers on
 *   top of the same coupling; they get re-added over this seam when their consumers
 *   (alloy component lists) are ported.
 * The arithmetic, equality and merging methods below are verbatim, including upstream quirks:
 * amounts may be negative, and a negative amount acts as an amount-wildcard in equals (:84).
 */
public final class OreDictMaterialStack implements Cloneable {
	public long mAmount;
	public OreDictMaterial mMaterial;

	public OreDictMaterialStack(OreDictMaterial aMaterial, long aAmount) {
		mMaterial = aMaterial==null?MT.NULL:aMaterial;
		mAmount = aAmount;
	}

	public OreDictMaterialStack copy(long aAmount) {
		return new OreDictMaterialStack(mMaterial, aAmount);
	}

	@Override
	public OreDictMaterialStack clone() {
		return new OreDictMaterialStack(mMaterial, mAmount);
	}

	public OreDictMaterialStack div(long aDivider) {
		return new OreDictMaterialStack(mMaterial, mAmount / aDivider);
	}

	public OreDictMaterialStack divup(long aDivider) {
		return new OreDictMaterialStack(mMaterial, UT.Code.divup(mAmount, aDivider));
	}

	public OreDictMaterialStack mul(long aMultiplier) {
		return new OreDictMaterialStack(mMaterial, aMultiplier * mAmount);
	}

	public OreDictMaterialStack div(long aMultiplier, long aDivider) {
		return new OreDictMaterialStack(mMaterial, (aMultiplier * mAmount) / aDivider);
	}

	public OreDictMaterialStack divup(long aMultiplier, long aDivider) {
		return new OreDictMaterialStack(mMaterial, UT.Code.divup(aMultiplier * mAmount, aDivider));
	}

	public double weight() {
		return mMaterial.getWeight(mAmount);
	}

	public boolean has(OreDictMaterial aMaterial) {
		return mMaterial == aMaterial && mAmount > 0;
	}

	@Override
	public boolean equals(Object aObject) {
		if (aObject == this || aObject == mMaterial) return T;
		if (aObject == null) return F;
		if (aObject instanceof OreDictMaterialStack) return ((OreDictMaterialStack)aObject).mMaterial == mMaterial && (mAmount < 0 || ((OreDictMaterialStack)aObject).mAmount < 0 || ((OreDictMaterialStack)aObject).mAmount == mAmount);
		return F;
	}

	@Override
	public String toString() {
		return mMaterial.toString() + " - " + mAmount;
	}

	@Override
	public int hashCode() {
		return mMaterial.hashCode();
	}

	public List<OreDictMaterialStack> addToList(List<OreDictMaterialStack> aList) {
		if (mAmount == 0) return aList;
		for (OreDictMaterialStack tMaterial : aList) if (tMaterial.mMaterial == mMaterial) {tMaterial.mAmount += mAmount; return aList;}
		aList.add(clone());
		return aList;
	}

	/** Delegates to {@link MaterialStackSerializer.Default}; pass another serializer for custom backends. */
	public void save(MaterialStackSerializer aSerializer, MaterialStackSerializer.Storage aStorage) {
		aSerializer.save(this, aStorage);
	}

	/** Delegates to {@link MaterialStackSerializer.Default}; pass another serializer for custom backends. */
	public static OreDictMaterialStack load(MaterialStackSerializer aSerializer, MaterialStackSerializer.Storage aStorage, MaterialStackSerializer.MaterialResolver aResolver) {
		return aSerializer.load(aStorage, aResolver);
	}
}
