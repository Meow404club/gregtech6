package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
//?} else {
/*import net.minecraft.world.item.component.CustomData;
import gregtech6.registry.GT6DataComponents;
 *///?}
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import org.jetbrains.annotations.NotNull;

import gregtech6.fluid.FluidTankGT;
import gregtech6.fluid.GTFluidLists;

/**
 * The item face of the Capsule-Cell-Container family (task small-tank-cell) — the
 * GT6GasCylinderItemFluidHandler template minus the NBT_MODE re-bind (the cell
 * capacity is FIXED at the row 1000 L — upstream has no per-cell limit face), over
 * the one family door:
 * <ul>
 * <li>the LIQUIDPROOF F admission gate — every non-gas fluid refuses at fill
 *     ({@link GT6CellBlockEntity#allowsFluid}, the upstream isFluidAllowed :419-421
 *     row pair);</li>
 * <li>a fill/drain persists the tank back under the key the placed BE reads
 *     ({@link GT6CellBlockEntity#NBT_TANK}); the write gate keeps the key present
 *     iff content is, so an emptied cell drops the tag entirely.</li>
 * </ul>
 *
 * <p>Template shape ({@code FluidHandlerItemStack}, forge-1.20.1 templates/
 * FluidHandlerItemStack.java): the handler holds the container ItemStack by reference
 * (:37) and {@code fill}/{@code drain} refuse stacks with {@code getCount() != 1}
 * (:108) — note the emptied-then-tag-less cell STILL stacks to 64 filled or not (the
 * upstream MultiTileEntityCell.java:76 family override; the barrel/cylinder
 * content-kills-stacking face does not exist here).
 *
 * <p>{@link ICapabilitySerializable} is the persistence seam (the
 * GTBarrelItemFluidHandler form): serialize/deserialize ride the same key.
 */
//? if forge {
public class GT6CellItemFluidHandler implements IFluidHandlerItem, ICapabilitySerializable<CompoundTag> {
//?} else {
/*public class GT6CellItemFluidHandler implements IFluidHandlerItem {
 *///?}

	//? if forge {
	private final LazyOptional<IFluidHandlerItem> mHolder = LazyOptional.of(() -> this);
	//?}

	@NotNull
	private final ItemStack mContainer;

	private final FluidTankGT mTank = new FluidTankGT(GT6CellBlockEntity.CAPACITY);

	/**
	 * @param aContainer the live container stack — held by reference (the Forge template
	 *        {@code container} field); an existing tag re-hydrates the content
	 * @param aCapacityL the tank size — the fixed 1000 L row value (all 40 rows)
	 */
	public GT6CellItemFluidHandler(@NotNull ItemStack aContainer, long aCapacityL) {
		mContainer = aContainer;
		mTank.setCapacity(aCapacityL);
		//? if forge {
		if (aContainer.hasTag()) readFromContainerTag();
		//?} else {
		/*if (aContainer.get(GT6DataComponents.BARREL_CONTENT) != null) readFromContainerTag();
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// the container-tag persistence seam — the one key both sides read
	// ---------------------------------------------------------------------------

	//? if forge {
	private void readFromContainerTag() {
		CompoundTag tTag = mContainer.getTag();
		if (tTag == null) return;
		mTank.readFromNBT(tTag, GT6CellBlockEntity.NBT_TANK);
	}
	//?} else {
	/*private void readFromContainerTag() {
		CustomData tData = mContainer.get(GT6DataComponents.BARREL_CONTENT);
		if (tData == null) return;
		mTank.readFromNBT(tData.copyTag(), GT6CellBlockEntity.NBT_TANK);
	}
	 *///?}

	/** Writes the tank back; an emptied tank removes the key and a then-empty tag is dropped. */
	//? if forge {
	private void writeToContainerTag() {
		CompoundTag tTag = mContainer.getOrCreateTag();
		mTank.writeToNBT(tTag, GT6CellBlockEntity.NBT_TANK);
		if (tTag.isEmpty()) mContainer.setTag(null);
	}
	//?} else {
	/*private void writeToContainerTag() {
		CompoundTag tTag = new CompoundTag();
		mTank.writeToNBT(tTag, GT6CellBlockEntity.NBT_TANK);
		if (tTag.isEmpty()) mContainer.remove(GT6DataComponents.BARREL_CONTENT);
		else CustomData.set(GT6DataComponents.BARREL_CONTENT, mContainer, tTag);
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// the IFluidHandlerItem face (the template :108 count guard + the gas-only door)
	// ---------------------------------------------------------------------------

	@Override
	@NotNull
	public ItemStack getContainer() {
		return mContainer;
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public FluidStack getFluidInTank(int aTank) {
		FluidStack tFluid = mTank.fluid();
		return tFluid == null ? FluidStack.EMPTY : tFluid;
	}

	@Override
	public int getTankCapacity(int aTank) {
		return aTank == 0 ? (int)mTank.capacity() : 0;
	}

	@Override
	public boolean isFluidValid(int aTank, FluidStack aStack) {
		return aStack != null && !aStack.isEmpty() && GT6CellBlockEntity.allowsFluid(GTFluidLists.name(aStack));
	}

	@Override
	public int fill(FluidStack aResource, FluidAction aAction) {
		if (aResource == null || aResource.isEmpty() || mContainer.getCount() != 1) return 0;
		if (!GT6CellBlockEntity.allowsFluid(GTFluidLists.name(aResource))) return 0;
		int tFilled = mTank.fill(aResource, aAction);
		if (tFilled > 0 && aAction.execute()) writeToContainerTag();
		return tFilled;
	}

	@Override
	public FluidStack drain(FluidStack aResource, FluidAction aAction) {
		if (aResource == null || aResource.isEmpty() || mContainer.getCount() != 1) return FluidStack.EMPTY;
		FluidStack tDrained = mTank.drain(aResource, aAction);
		if (tDrained != null && !tDrained.isEmpty() && aAction.execute()) writeToContainerTag();
		return tDrained == null ? FluidStack.EMPTY : tDrained;
	}

	@Override
	public FluidStack drain(int aMaxDrain, FluidAction aAction) {
		if (mContainer.getCount() != 1) return FluidStack.EMPTY;
		FluidStack tDrained = mTank.drain(aMaxDrain, aAction);
		if (tDrained != null && !tDrained.isEmpty() && aAction.execute()) writeToContainerTag();
		return tDrained == null ? FluidStack.EMPTY : tDrained;
	}

	// ---------------------------------------------------------------------------
	// the persistence seam (the ICapabilitySerializable face, the barrel form)
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tTag = new CompoundTag();
		mTank.writeToNBT(tTag, GT6CellBlockEntity.NBT_TANK);
		return tTag;
	}

	@Override
	public void deserializeNBT(CompoundTag aNBT) {
		mTank.readFromNBT(aNBT, GT6CellBlockEntity.NBT_TANK);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable net.minecraft.core.Direction aSide) {
		return aCapability == ForgeCapabilities.FLUID_HANDLER_ITEM ? mHolder.cast() : LazyOptional.empty();
	}
	//?}
}
