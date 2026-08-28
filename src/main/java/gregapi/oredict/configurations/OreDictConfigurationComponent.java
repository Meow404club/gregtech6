/**
 * Ported from GregTech 6 (1.7.10), file gregapi/oredict/configurations/OreDictConfigurationComponent.java
 * (upstream 54 lines), by task gt-material-model.
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

package gregapi.oredict.configurations;

import static gregapi.data.CS.F;

import java.util.ArrayList;
import java.util.List;

import gregapi.oredict.OreDictMaterialStack;

/**
 * @author Gregorius Techneticies
 *
 * Port deviations: upstream builds gregapi.code.ArrayListNoNulls (gregapi/code is outside this
 * card's FILES_SCOPE, plain ArrayList used with the NoNulls semantics inlined: null elements are
 * silently dropped on add) and creates stacks via the OM.stack(material, amount) equivalent
 * {@link gregapi.oredict.OreDictMaterial#stack} (null material yields a null stack, which the
 * NoNulls list then drops, exactly like upstream) - OM itself is not ported yet.
 */
public class OreDictConfigurationComponent implements IOreDictConfigurationComponent {
	private final List<OreDictMaterialStack> mList;
	private final List<OreDictMaterialStack> mDividedList;
	public final long mCommonDivider;

	public OreDictConfigurationComponent(long aCommonDivider, OreDictMaterialStack... aComponents) {
		mCommonDivider = aCommonDivider;
		mList = new ArrayList<>();
		if (aComponents != null) for (OreDictMaterialStack tMaterial : aComponents) if (tMaterial != null) mList.add(tMaterial); // ArrayListNoNulls drops null elements silently
		mDividedList = new ArrayList<>();
		for (OreDictMaterialStack tMaterial : mList) mDividedList.add(new OreDictMaterialStack(tMaterial.mMaterial, tMaterial.mAmount / mCommonDivider));
	}

	@Override
	public List<OreDictMaterialStack> getComponents() {
		return mDividedList;
	}

	@Override
	public List<OreDictMaterialStack> getUndividedComponents() {
		return mList;
	}

	@Override
	public long getCommonDivider() {
		return mCommonDivider;
	}
}
