/**
 * Minimal port of GregTech 6 (1.7.10), file gregapi/data/CS.java (upstream 2345 lines).
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

/**
 * Constant Storage.
 *
 * Phase-1 policy (task gt-material-foundation): carry ONLY the constant sections the pure
 * material foundation needs, with names and values matching upstream exactly
 * (CS.java:96 T/F, CS.java:108-122 U family + UD). The remaining sections of the upstream
 * file are ported incrementally by later task cards - do NOT bulk-copy the whole file
 * (upstream fields include MC-coupled types like Abstract_Mod).
 *
 * All constants below are compile-time constants: no static-initialization-order hazards.
 */
public class CS {
	/** Because "true" and "false" are too long. Some Programmers might wanna kill me for that, but this looks much better than true and false, and also it is better to have something that is not 4 and 5 Characters long, because of symmetry */ // CS.java:95
	public static final boolean T = true, F = false;

	/**
	 * Renamed from "MATERIAL_UNIT" to just "U"
	 *
	 * This is worth exactly one normal Item.
	 * This Constant can be divided by many commonly used Numbers such as
	 * 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 20, 21, 22, 24, 25, ... 64, 81, 96, 144 or 1000
	 * without loosing precision and is for that reason used as Unit of Amount.
	 * But it is also small enough to be multiplied with larger Numbers.
	 *
	 * This is used to determine the amount of Material contained inside a prefixed Ore.
	 * For example Nugget = U / 9 as it contains out of 1/9th of an Ingot.
	 */ // CS.java:108-119
	public static final long U = 648648000, U2 = U/2, U3 = U/3, U4 = U/4, U5 = U/5, U6 = U/6, U7 = U/7, U8 = U/8, U9 = U/9, U10 = U/10, U11 = U/11, U12 = U/12, U13 = U/13, U14 = U/14, U15 = U/15, U16 = U/16, U17 = U/17, U18 = U/18, U20 = U/20, U24 = U/24, U25 = U/25, U32 = U/32, U36 = U/36, U40 = U/40, U48 = U/48, U50 = U/50, U64 = U/64, U72 = U/72, U80 = U/80, U96 = U/96, U100 = U/100, U128 = U/128, U144 = U/144, U192 = U/192, U200 = U/200, U240 = U/240, U256 = U/256, U288 = U/288, U480 = U/480, U500 = U/500, U512 = U/512, U1000 = U/1000, U1440 = U/1440;
	/** The Double Version of the Material Unit "U" */ // CS.java:121-122
	public static final double UD = U;

	/** The value of how many RF are worth an EU. */ // CS.java:207-208
	public static final int RF_PER_EU = 4;
	/**
	 * If Machines explode when they get overloaded with too big Energy Packets (upstream
	 * Root:496 overcharge branch). CARD DECISION (ADR 2026-08-31-p7-energy-network 1e): pinned
	 * to true so the overcharge path is observable in the port; the upstream CONFIG default is
	 * actually false ("explode_by_overload", GT_API.java:582), and upstream force-couples
	 * BREAKING to EXPLOSIONS at GT_API.java:593. Upstream config system is pooled, so this is
	 * carried as a compile-time constant.
	 */
	public static final boolean OVERCHARGE_EXPLOSIONS = T;
	/**
	 * If overloaded Machines break instead of exploding (upstream Root:502 branch, only reached
	 * when OVERCHARGE_EXPLOSIONS is false). false matches the upstream config default
	 * ("break_by_overload", GT_API.java:587). Upstream config system is pooled, so this is
	 * carried as a compile-time constant.
	 */
	public static final boolean OVERCHARGE_BREAKING = F;
}
