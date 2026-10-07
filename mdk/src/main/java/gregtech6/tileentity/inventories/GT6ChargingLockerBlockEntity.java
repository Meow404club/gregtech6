package gregtech6.tileentity.inventories;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.item.energy.IItemEnergy;
import gregtech6.registry.GTBlockEntities;

/**
 * The GT6 Charging Locker — 1.20.1 counterpart of
 * {@code gregtech/tileentity/inventories/MultiTileEntityLockerCharging.java} (85 lines),
 * the metalset row :139 ("Charging Locker (Mat)", id 7500+aID): the plain locker's four
 * armor slots double as the charge bay.
 *
 * <p>Semantics, each clause anchored:
 * <ul>
 * <li>the inventory/facing/armor-swap faces inherit {@link GT6LockerBlockEntity}
 *     verbatim — the upstream charging subclass overrides none of them;</li>
 * <li>the injection arm :44-48 verbatim: a received energy push walks slots 0-3 and
 *     re-emits each packet train into the {@link IItemEnergy} item sitting there (the
 *     unused remainder flows to the next slot via the {@code aAmount-rReturn} column);</li>
 * <li>the type faces :50-54: EVERY energy type accepted while receiving
 *     ({@code TD.Energy.ALL}), the packet band 1 .. Long.MAX_VALUE (:51-53 — the
 *     Root defaults fold, the Recommended column drives them so all three are pinned
 *     here, the MAX*2 overflow face included);</li>
 * <li>the stateRunning quad (:56-59) folds — it feeds only the 1.7.10 comparator /
 *     client-data surface, the port charging texture group ships no active variant
 *     (the registry-class doc). No ticking: the locker is a pure push receiver (the
 *     upstream class ticks only for the state push, the port no-tick chain holds).</li>
 * </ul>
 */
public class GT6ChargingLockerBlockEntity extends GT6LockerBlockEntity {

	/** BET factory for BlockEntityType.Builder.of (the locker form). */
	public GT6ChargingLockerBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.CHARGING_LOCKER_BE.get(), aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6ChargingLockerBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "locker_charging"; // upstream :84 "gt.multitileentity.locker.charging" — the BET path mirrors it
	}

	// ---------------------------------------------------------------------------
	// the energy receiver (upstream :44-54)
	// ---------------------------------------------------------------------------

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		long rReturn = 0;
		for (int i = 0; i < INVENTORY_SIZE; i++) {
			if (!slotHas(i)) continue;
			ItemStack tStack = slot(i);
			if (tStack.getItem() instanceof IItemEnergy tEnergy) {
				// the upstream Utility.inject(aAmount-rReturn) remainder column verbatim
				rReturn += tEnergy.doEnergyInjection(aEnergyType, tStack, aSize, aAmount - rReturn, aDoInject);
			}
		}
		return rReturn;
	}

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return !aEmitting; // :50 — receiving side only, every type
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return TD.Energy.ALL; // :54
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return 1; // :51
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return Long.MAX_VALUE; // :52
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return Long.MAX_VALUE; // :53 — the Root band folds (min/max pinned over it)
	}
}
