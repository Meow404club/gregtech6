/**
 * Minimal stand-in for GregTech 6 (1.7.10), file gregapi/data/MT.java (upstream 4118 lines).
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

package gregapi.data;

import gregapi.oredict.OreDictMaterial;

/**
 * @author Gregorius Techneticies
 *
 * CARD gt-material-foundation PLACEHOLDER - task card gt-material-dataset (卡3) owns the real
 * MT/AM/ANY material tables (registered in batches; no giant static initialization block).
 *
 * Only the null-object constant is carried here because OreDictMaterialStack's constructor
 * falls back to it (upstream OreDictMaterialStack.java:39). Upstream builds it via
 * MT.java:524: NULL = create(-1, "NULL").setStatsElement(0,0,0,0,0).put(INVALID_MATERIAL, ...);
 * the tag/stat side of that chain is card-2/3 scope, so this stand-in only preserves the
 * ID (-1) and internal name ("NULL").
 */
public class MT {
	/** The "not a real Material" Material: ID -1, internal name "NULL" (upstream MT.java:524). */
	public static final OreDictMaterial NULL = new OreDictMaterial((short)-1, "NULL", "NULL");
}
