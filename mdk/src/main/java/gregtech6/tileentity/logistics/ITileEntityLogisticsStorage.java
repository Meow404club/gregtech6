/**
 * Ported from GregTech 6 (1.7.10), file gregapi/tileentity/logistics/ITileEntityLogisticsStorage.java
 * (upstream 36 lines, the :28-35 interface face), by task p32-logistics-lv3.
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; and without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.tileentity.logistics;

import gregapi.tileentity.logistics.ITileEntityLogistics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * The logistics storage-endpoint face — 1.20.1 port of the upstream interface, verbatim
 * four-method shape. The Logistics Core (MultiTileEntityLogisticsCore.java:277-294) walks
 * its BFS members and registers every implementor into one of the three priority tiers,
 * separately for fluids and items:
 * <ul>
 * <li>{@code 0} — Disabled: the endpoint is not a routing target for that medium;</li>
 * <li>{@code 1} — Generic: accepts anything;</li>
 * <li>{@code 2} — Semi-Filtered: the {@code getLogisticsFilter*} answer is advisory;</li>
 * <li>{@code 3} — Filtered: only the {@code getLogisticsFilter*} answer moves in
 *     (the defragmentation arms route generic content here first).</li>
 * </ul>
 *
 * <p>Upstream implementors: the MassStorage base (MultiTileEntityMassStorage.java:502-505)
 * and the barrel base (TileEntityBase08Barrel.java:285-288 — the content-derived pair).
 * The port's first implementor is the Logistics Tank ({@code GTBarrelLogisticsBlockEntity},
 * the TileEntityBase08Barrel.java:285-288 face verbatim).
 */
public interface ITileEntityLogisticsStorage extends ITileEntityLogistics {
	/** Upstream :29-30. @return 0 = Disabled, 1 = Generic, 2 = Semi-Filtered, 3 = Filtered */
	int getLogisticsPriorityFluid();
	/** Upstream :31-32. @return 0 = Disabled, 1 = Generic, 2 = Semi-Filtered, 3 = Filtered */
	int getLogisticsPriorityItem();

	/** Upstream :34 — the fluid the endpoint is (semi-)filtered for; null = no filter. */
	Fluid getLogisticsFilterFluid();
	/** Upstream :35 — the item the endpoint is (semi-)filtered for; null = no filter. */
	ItemStack getLogisticsFilterItem();
}
