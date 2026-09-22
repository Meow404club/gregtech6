package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.items.IItemHandler; // the leg-native handler type (the Fluidometer import swap form)

/**
 * The Stack-O-Meter Sensor (task p34-sensors-trivial-14 row ⑦) — the port of
 * MultiTileEntityStackometer.java:33-63: the OCCUPIED-STACK count over the neighbour
 * inventory (:34-50) with the slot count as max (:53-58) — the
 * {@link GT6ItemometerBlockEntity} probe at the stack grain (the same declared cuts:
 * no {@code IL.Display_Fluid} filter, the side-resolved capability view).
 */
public class GT6StackometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6StackometerBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6StackometerBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.STACKOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "stackometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		IItemHandler tHandler = probeHandler(aTarget);
		if (tHandler == null) return 0;
		long rStacks = 0;
		for (int i = 0; i < tHandler.getSlots(); i++) {
			ItemStack tStack = tHandler.getStackInSlot(i);
			if (!tStack.isEmpty() && tStack.getCount() > 0) rStacks++; // upstream :45/:49 the occupied-stack count
		}
		return rStacks;
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		IItemHandler tHandler = probeHandler(aTarget);
		return tHandler == null ? 0 : tHandler.getSlots(); // upstream :53-58 the slot count
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
