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
 * The item face of the Porcelain Cup (task small-tank-cup) — the
 * {@link GT6CellItemFluidHandler} template over the cup row door (the :2094 pair
 * LIQUIDPROOF T / GASPROOF F — every gas refuses at fill, the liquid band admits):
 * <ul>
 * <li>the admission gate rides {@link GT6CupBlockEntity#admits} — a water bucket
 *     fills, a hydrogen cell refuses;</li>
 * <li>a fill/drain persists the tank back under {@link GT6CupBlockEntity#NBT_TANK}; the
 *     write gate keeps the key present iff content is, so an emptied cup drops the tag
 *     entirely (the cell/barrel face).</li>
 * </ul>
 *
 * <p>Template shape ({@code FluidHandlerItemStack}, forge-1.20.1 templates/
 * FluidHandlerItemStack.java): the container stack held by reference (:37), the
 * {@code getCount() != 1} guard (:108) — a FILLED cup stacks 1 anyway (the upstream
 * TileEntityBase08FluidContainer:423 content gate on the item, the registration column
 * 16 for the empty stack).
 */
//? if forge {
public class GT6CupItemFluidHandler implements IFluidHandlerItem, ICapabilitySerializable<CompoundTag> {
//?} else {
/*public class GT6CupItemFluidHandler implements IFluidHandlerItem {
 *///?}

	//? if forge {
	private final LazyOptional<IFluidHandlerItem> mHolder = LazyOptional.of(() -> this);
	//?}

	@NotNull
	private final ItemStack mContainer;

	private final FluidTankGT mTank = new FluidTankGT(GT6CupBlockEntity.CAPACITY);

	/** @param aContainer the live container stack — held by reference; an existing tag re-hydrates the content */
	public GT6CupItemFluidHandler(@NotNull ItemStack aContainer) {
		mContainer = aContainer;
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
		mTank.readFromNBT(tTag, GT6CupBlockEntity.NBT_TANK);
	}
	//?} else {
	/*private void readFromContainerTag() {
		CustomData tData = mContainer.get(GT6DataComponents.BARREL_CONTENT);
		if (tData == null) return;
		mTank.readFromNBT(tData.copyTag(), GT6CupBlockEntity.NBT_TANK);
	}
	 *///?}

	/** Writes the tank back; an emptied tank removes the key and a then-empty tag is dropped. */
	//? if forge {
	private void writeToContainerTag() {
		CompoundTag tTag = mContainer.getOrCreateTag();
		mTank.writeToNBT(tTag, GT6CupBlockEntity.NBT_TANK);
		if (tTag.isEmpty()) mContainer.setTag(null);
	}
	//?} else {
	/*private void writeToContainerTag() {
		CompoundTag tTag = new CompoundTag();
		mTank.writeToNBT(tTag, GT6CupBlockEntity.NBT_TANK);
		if (tTag.isEmpty()) mContainer.remove(GT6DataComponents.BARREL_CONTENT);
		else CustomData.set(GT6DataComponents.BARREL_CONTENT, mContainer, tTag);
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// the IFluidHandlerItem face (the template :108 count guard + the liquid door)
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
		return aStack != null && !aStack.isEmpty() && GT6CupBlockEntity.admits(GTFluidLists.name(aStack));
	}

	@Override
	public int fill(FluidStack aResource, FluidAction aAction) {
		if (aResource == null || aResource.isEmpty() || mContainer.getCount() != 1) return 0;
		if (!GT6CupBlockEntity.admits(GTFluidLists.name(aResource))) return 0;
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
		mTank.writeToNBT(tTag, GT6CupBlockEntity.NBT_TANK);
		return tTag;
	}

	@Override
	public void deserializeNBT(CompoundTag aNBT) {
		mTank.readFromNBT(aNBT, GT6CupBlockEntity.NBT_TANK);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable net.minecraft.core.Direction aSide) {
		return aCapability == ForgeCapabilities.FLUID_HANDLER_ITEM ? mHolder.cast() : LazyOptional.empty();
	}
	//?}
}
