/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregtech6.tileentity.machines;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.item.energy.IItemEnergy;

/**
 * The Charging Crafting Table (task act-matrix) — the matrix's second machine kind,
 * port of upstream gregapi/tileentity/tools/MultiTileEntityChargingCraftingTable.java:42
 * (81 lines): an {@link TileEntityAdvancedCraftingTable} subclass that ADDS the
 * tool-slot charger — the injected energy packet charges ONE packet into each occupied
 * tool slot (16-20, the {@code SLOTS_TOOLS} walk :48-51; the BatteryBox exchange shape,
 * GT6BatteryBoxBlockEntity.exchange — {@code IItemEnergy} dispatch, the upstream
 * {@code IItemEnergy.Utility} bridge being cut with the IC2 compat pool, IItemEnergy.java:45-48).
 *
 * <p>The energy acceptor face is verbatim upstream :54-58: accepts every energy type
 * ({@code TD.Energy.ALL}), input band {@code min 1 / max recommended MAX_VALUE} — the
 * band overrides are LOAD-BEARING (the Root defaults derive Min = Rec/2 and Max = Rec*2,
 * TileEntityBase01Root.java:405-409, and MAX_VALUE/2 / MAX_VALUE*2 both overflow).
 *
 * <p><b>Cut with declaration</b>: the {@code getTexture2} charging texture family
 * (:61-80, the craftingtables/charging colored/overlay groups) rides the ⑩B render/GUI
 * card — the transitional shared ACT model covers both kinds (the GTAdvancedCraftingTableBlock
 * class doc). The {@code NBT_GUI} registration column rides the same card. KJS surface:
 * the registration face defers to the kjs-binding card.
 */
public class TileEntityChargingCraftingTable extends TileEntityAdvancedCraftingTable {

	public TileEntityChargingCraftingTable(BlockPos aPos, BlockState aState) {
		this(gregtech6.registry.GTMachines.CHARGING_CRAFTING_TABLE_BE.get(), aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the ACT twin). */
	public TileEntityChargingCraftingTable(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "charging_crafting_table"; // upstream "gt.multitileentity.crafting.charging" :43, the BET registry path
	}

	/**
	 * Upstream doInject :46-52 verbatim: one packet per occupied tool slot, while the
	 * remaining amount lasts ({@code aAmount > rReturn}). The injection mutates the
	 * stack's energy component in place (the BatteryBox exchange contract).
	 */
	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		long rReturn = 0;
		for (int i : SLOTS_TOOLS) if (aAmount > rReturn && !mInv.getStackInSlot(i).isEmpty()) {
			ItemStack tStack = mInv.getStackInSlot(i);
			if (tStack.getItem() instanceof IItemEnergy tEnergy) {
				rReturn += tEnergy.doEnergyInjection(aEnergyType, tStack, aSize, 1, aDoInject);
			}
		}
		return rReturn;
	}

	@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return !aEmitting;} // :54
	@Override public long getEnergySizeInputMin        (TagData aEnergyType, byte aSide) {return 1;} // :55
	@Override public long getEnergySizeInputMax        (TagData aEnergyType, byte aSide) {return Long.MAX_VALUE;} // :56
	@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return Long.MAX_VALUE;} // :57
	@Override public Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.ALL;} // :58
}
