package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.items.IItemHandler; // the leg-native handler type (the Fluidometer import swap form)

/**
 * The Item-O-Meter Sensor (task p34-sensors-trivial-14 row ⑥) — the port of
 * MultiTileEntityItemometer.java:33-70. Upstream summed the stack sizes over the
 * neighbour {@code IInventory} (:34-52) with the slot-count × stack-limit max (:57-62);
 * the modern equivalent is the ITEM_HANDLER capability at the probe position — the
 * {@link GT6FluidometerBlockEntity} probe shape (one capability query, both legs).
 * Declared cuts: the {@code IL.Display_Fluid} stack filter (:40/:46 — no port face) and
 * the sided {@code SIDE_ANY} slot caching (:36-38 — the capability is already the
 * side-resolved view). The max side sums the per-slot limits (upstream multiplied the
 * uniform {@code getInventoryStackLimit()} by the slot count — the modern handler
 * carries per-slot limits instead).
 */
public class GT6ItemometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6ItemometerBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6ItemometerBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.ITEMOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "itemometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		IItemHandler tHandler = probeHandler(aTarget);
		if (tHandler == null) return 0;
		long rAmount = 0;
		for (int i = 0; i < tHandler.getSlots(); i++) {
			ItemStack tStack = tHandler.getStackInSlot(i);
			if (!tStack.isEmpty()) rAmount += tStack.getCount(); // upstream :40/:46 the stackSize sum
		}
		return rAmount;
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		IItemHandler tHandler = probeHandler(aTarget);
		if (tHandler == null) return 0;
		long rLimit = 0;
		for (int i = 0; i < tHandler.getSlots(); i++) rLimit += tHandler.getSlotLimit(i); // upstream :57-62 in the per-slot form
		return rLimit;
	}

	//? if forge {
	private net.minecraftforge.items.IItemHandler probeHandler(@Nullable BlockEntity aTarget) {
		if (aTarget == null || aTarget.isRemoved()) return null;
		return aTarget.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,
				Direction.from3DDataValue(mSecondFacing).getOpposite()).orElse(null); // the Fluidometer probe shape
	}
	//?} else {
	/*private net.neoforged.neoforge.items.IItemHandler probeHandler(@Nullable BlockEntity aTarget) {
		if (aTarget == null || aTarget.isRemoved()) return null;
		return aTarget.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
				aTarget.getBlockPos(), Direction.from3DDataValue(mSecondFacing).getOpposite()); // the Fluidometer probe shape
	}
	*///?}
}
