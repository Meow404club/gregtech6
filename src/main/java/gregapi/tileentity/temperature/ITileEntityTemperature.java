/**
 * Ported from GregTech 6 (1.7.10), file gregapi/tileentity/data/ITileEntityTemperature.java
 * (upstream 32 lines, the :27-31 interface face), by task p26-crucible-physics-smeltery.
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

package gregapi.tileentity.temperature;

/**
 * @author Gregorius Techneticies
 *
 * The temperature read-out face of the crucible family (Smeltery, Mold, large Crucible) —
 * the thermometer/tool/tooltip consumers read these two methods.
 *
 * Port deviations (both declared):
 * <ul>
 * <li>upstream extends ITileEntityUnloadable; the root pure-logic layer carries no such
 *     interface (the unloadable lifecycle checks ride the mdk TileEntityBase01Root), so
 *     this is a plain interface. The method signatures are upstream-verbatim.</li>
 * <li>upstream package is gregapi.tileentity.data; this port puts the temperature family
 *     into its own gregapi.tileentity.temperature package (the task card's files_scope
 *     path), mirroring how the energy family got its own package in p7-d1-energy-core.</li>
 * </ul>
 */
public interface ITileEntityTemperature {
	/** Upstream :29 — The Temperature this Object has right now. Measured in Kelvin. */
	long getTemperatureValue(byte aSide);

	/** Upstream :31 — The Temperature this Object can have at most before breaking. Measured in Kelvin. */
	long getTemperatureMax(byte aSide);

	/**
	 * The MC-free environment-temperature seam, the IEnergyAdjacency form applied to
	 * temperature (the arch ruling tasks.p26-arch-crucible-chain ②: "envTemp adjacency
	 * seam, 仿 root IEnergyAdjacency 剥 World 形").
	 *
	 * <p>Upstream resolved the ambient temperature inside WD.envTemp(World, x, y, z)
	 * (WD.java:404-406: biome lookup + {@code max(1, C-3+biomeTemp*20)}), which requires a
	 * World object. This port pulls the lookup out into this seam so the crucible physics
	 * consumers stay world-free and offline-testable; the mdk side implements it on top of
	 * the Level/biome climate at the BE position. The fallback answer for a seam-less
	 * context is the upstream DEF_ENV_TEMP = C + 20 = 293 (CS.java:135, 293.15 IRL).
	 */
	interface EnvTemp {
		/**
		 * @return the regular Environment Temperature at this Location in Kelvin
		 *         (upstream WD.envTemp(World, x, y, z), WD.java:404-406).
		 */
		long envTemp(int aX, int aY, int aZ);
	}
}
