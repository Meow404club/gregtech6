/**
 * Minimal stand-in for GregTech 6 (1.7.10), file gregapi/oredict/OreDictMaterial.java
 * (upstream 1512 lines).
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

/**
 * @author Gregorius Techneticies
 *
 * CARD gt-material-foundation PLACEHOLDER - task card gt-material-model (卡2) owns the real
 * port of this file (registry, createMaterial, chemistry, tags, ...).
 *
 * This stand-in carries exactly the members OreDictMaterialStack needs, with upstream names,
 * types and semantics preserved (upstream line references):
 * - mID: short, negative = "not in the array / not unificatable" (:209)
 * - mNameInternal: OreDictionary name (:213)
 * - mGramPerCubicCentimeter, default 1.0 (:240)
 * - getWeight(long): kg formula (:1391-1402)
 * - hashCode() from a creation-order counter, independent of any ID (:210-211, :327, :1410-1412)
 * - toString() = internal name (:1404-1407)
 *
 * Deviations: the upstream constructor (:321, private, plus MATERIAL_MAP/MATERIAL_ARRAY
 * registration) is public here and performs NO registry interaction - registration is a
 * card-2 red line and must not be duplicated. Do not grow this class; port the real one in
 * card gt-material-model instead.
 */
public class OreDictMaterial {
	/** Creation-order hash counter; upstream mHashID is fully independent from any ID (:210-211, :327). */
	private static int sHashID = 0;

	/** The Index of this Material inside the Array. Negative for "Not in the Array" and therefore also for "Not Unificatable", 0 is the NULL Material so a > 0 check could be useful for you. */ // :208-209
	public final short mID;
	/** The HashCode for this Material. Fully independent from any ID this Material would be assigned to, UNLIKE ITEMS. */ // :210-211
	private final int mHashID;
	/** The OreDictionary Name of this Material */ // :212-213
	public final String mNameInternal;
	/** Density in g/cm^3, default 1.0 like upstream (:240). */
	public double mGramPerCubicCentimeter = 1.0;

	/** Upstream this is private (:321) and registers into MATERIAL_MAP/MATERIAL_ARRAY; registration is card-2 scope, so this stand-in constructor is public and registry-free. */
	public OreDictMaterial(short aID, String aNameInternal, String aNameLocal) {
		mID = aID;
		mNameInternal = aNameInternal;
		mHashID = sHashID++;
	}

	/** Gets the Weight of this Material in Kilogramme, depending on the Amount of Material passed. */ // :1390-1402 verbatim
	public double getWeight(long aAmount) {
		// Extended Math:
		// 9 Material-Units = 1 Cubic Meter.
		// 1000 g    = 1 kg
		// 1000 cm^3 = 1 dm^3
		// 1000 dm^3 = 1  m^3
		// ( g/cm^3 * aAmount * 1000 * 1000) / (Material-Unit * 9 * 1000)
		// ( g/ m^3 * aAmount              ) / (Material-Unit * 9 * 1000)
		// (kg/ m^3 * aAmount              ) / (Material-Unit * 9       )
		// (kg/ m^3 * aAmount * 0.111111111) /  Material-Unit
		return (mGramPerCubicCentimeter * 111.111111 * aAmount) / U;
	}

	@Override
	public String toString() {
		return mNameInternal;
	}

	@Override
	public int hashCode() {
		return mHashID;
	}
}
