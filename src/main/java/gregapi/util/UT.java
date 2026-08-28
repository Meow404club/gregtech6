/**
 * Minimal port of GregTech 6 (1.7.10), file gregapi/util/UT.java (upstream 3476 lines).
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

package gregapi.util;

/**
 * @author Gregorius Techneticies
 *
 * Phase-1 policy (task gt-material-foundation): the upstream UT utility monolith is heavily
 * MC-coupled, so only the pure helpers needed by ported material code are carried here,
 * verbatim. Further pure helpers are added incrementally as their consumers get ported;
 * MC-facing sections (UT.NBT, UT.Entities, ...) are Phase 2.
 */
public class UT {

	public static class Code {
		/** Divides but rounds up. */ // UT.java:1696-1699 verbatim
		public static long divup(long aNumber, long aDivider) {
			return aNumber / aDivider + (aNumber % aDivider == 0 ? 0 : 1);
		}

		/** Binds a Value between two Boundaries. */ // UT.java:1544-1546 verbatim (added by task gt-material-model)
		public static long bind(long aMin, long aMax, long aBoundValue) {
			return aMin > aMax ? Math.max(aMax, Math.min(aMin, aBoundValue)) : Math.max(aMin, Math.min(aMax, aBoundValue));
		}

		public static byte bind2(long aBoundValue) {return (byte) Math.max(0, Math.min(          3, aBoundValue));} // UT.java:1554 verbatim

		public static byte bind4(long aBoundValue) {return (byte) Math.max(0, Math.min(         15, aBoundValue));} // UT.java:1556 verbatim

		/** Checks if the Object converts to a non-empty String. */ // UT.java:1512-1514 verbatim; the BiomeGenBase branch of upstream stringValidate is MC-coupled and stays behind
		public static boolean stringValid(Object aString) {
			return aString != null && !aString.toString().isEmpty();
		}

		/** toUpperCases the first Character of the String and returns it. */ // UT.java:1653-1656 verbatim
		public static String capitalise(String aString) {
			return aString == null ? "" : aString.length() <= 1 ? aString.toUpperCase() : aString.substring(0, 1).toUpperCase() + aString.substring(1);
		}
	}
}
