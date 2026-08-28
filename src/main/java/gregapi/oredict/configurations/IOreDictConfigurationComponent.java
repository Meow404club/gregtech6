/**
 * Ported from GregTech 6 (1.7.10), file gregapi/oredict/configurations/IOreDictConfigurationComponent.java
 * (upstream 53 lines), by task gt-material-model.
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

import java.util.ArrayList;
import java.util.List;

import gregapi.oredict.OreDictMaterialStack;

/**
 * @author Gregorius Techneticies
 *
 * The reason why this is an Interface is, that you can create more powerful implementations of this.
 * For example one could add another Interface that returns the Atomic binding Configuration of this Molecule.
 *
 * Port deviation: upstream returns gregapi.code.ArrayListNoNulls; that class lives in gregapi/code/**
 * which is outside this task card's FILES_SCOPE, so plain java.util lists are used instead. Null
 * elements simply must not be passed (same contract as upstream, just without the runtime guard).
 */
public interface IOreDictConfigurationComponent {
	/**
	 * Gets a List of all Molecules inside one Material Unit of this Material. The Material Stack Amounts are all in Material Units.
	 *
	 * For example the Alloy Electrum consists of 50% Gold and 50% Silver, and is created by half a Unit of Gold and half a Unit of Silver.
	 * This means it has to return a MaterialStack of half a Unit of Gold and a MaterialStack of half a Unit Silver inside a Non-Null-ArrayList.
	 * "return new ArrayList<OreDictMaterialStack>(false, MT.stack(MT.Au, CS.U / 2), MT.stack(MT.Ag, CS.U / 2))"
	 */
	public List<OreDictMaterialStack> getComponents();

	public List<OreDictMaterialStack> getUndividedComponents();

	/**
	 * @return a Number which can, when multiplied with the Material Amounts inside the List, give only whole Amounts of Material. Do not return zero or negative Values.
	 */
	public long getCommonDivider();
}
