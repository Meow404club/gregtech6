package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.jetbrains.annotations.NotNull;

import gregtech6.fluid.FluidTankGT;

/**
 * The item face of the barrel family (task p12-fluid-item-carrier spec ③) — the
 * 1.20.1 counterpart of the 1.7.10 {@code IFluidContainerItem} implementers
 * (forge-api 1.7.10 IFluidContainerItem.java:16-59, every method ItemStack-taking):
 * a barrel ItemStack holds fluid exactly like the placed BlockEntity does, through
 * the same {@link FluidTankGT} semantics (the long internal amount behind the int
 * surface, the bindInt boundary, preventDraining).
 *
 * <p>Template shape ({@code FluidHandlerItemStack}, forge-1.20.1 templates/
 * FluidHandlerItemStack.java): the handler holds the container ItemStack and
 * persists into its tag ({@code getContainer()} hands the used stack back,
 * :52-55); {@code fill} refuses stacks with {@code getCount() != 1} (:108 — a
 * merged pile has no single tank to write into) and so does {@code drain}.
 *
 * <p>Declared deviation (card spec ③): the payload key is the BE's own
 * {@link TileEntityBase08Barrel#NBT_TANK} ({@code "tank"}, the in-repo plain form
 * of upstream NBT_TANK "gt.tank"), NOT the Forge template key {@code "Fluid"}
 * (FluidHandlerItemStack.java:32) — the same key on both sides of the BE↔item
 * round trip is the whole point: what {@code writeToNBT} saved into the dropped
 * barrel reads straight back out of the placed one, the upstream single-NBT-space
 * shape where the MTE item tag IS the TE NBT.
 *
 * <p>{@link ICapabilitySerializable} is the persistence seam: {@code serializeNBT}
 * / {@code deserializeNBT} are the single read/write pair over the container tag
 * that {@link com.mojang.brigadier}-free tests exercise directly and that
 * {@link GTBarrelBlockItem} rides from {@code initCapabilities} (IForgeItem.java:678).
 */
public class GTBarrelItemFluidHandler implements IFluidHandlerItem, ICapabilitySerializable<CompoundTag> {

	private final LazyOptional<IFluidHandlerItem> mHolder = LazyOptional.of(() -> this);

	@NotNull
	private final ItemStack mContainer;

	private final FluidTankGT mTank = new FluidTankGT();

	/**
	 * @param aContainer the live container stack — held by reference like the Forge
	 *        template ({@code container} field, FluidHandlerItemStack.java:37), so a
	 *        write reaches the inventory slot's own stack; {@code getContainer()}
	 *        hands it back (:52-55)
	 * @param aCapacityL the tank size in litres — the block-carrier capacity
	 *        ({@code GTBarrelBlock.capacityL()}, the upstream NBT_TANK_CAPACITY row)
	 */
	public GTBarrelItemFluidHandler(@NotNull ItemStack aContainer, long aCapacityL) {
		mContainer = aContainer;
		mTank.setCapacity(aCapacityL);
		if (aContainer.hasTag()) readFromContainerTag();
	}

	/** The stickiness seam (the BE load :132 {@code setPreventDraining(keepsFilter())} item counterpart) — the future logistics barrel item flips this. */
	public GTBarrelItemFluidHandler setPreventDraining(boolean aPrevent) {
		mTank.setPreventDraining(aPrevent);
		return this;
	}

	// ---------------------------------------------------------------------------
	// the ICapabilitySerializable persistence seam — the single key both sides read
	// ---------------------------------------------------------------------------

	/** Reads {@link TileEntityBase08Barrel#NBT_TANK} out of the container tag (a fresh deserialized empty tank when the key is absent). */
	private void readFromContainerTag() {
		mTank.readFromNBT(mContainer.getTag(), TileEntityBase08Barrel.NBT_TANK);
	}

	/** Writes the tank under {@link TileEntityBase08Barrel#NBT_TANK} into the container tag; an emptied tank removes the key and a then-empty tag is dropped entirely, so a drained barrel item is byte-identical to a never-filled one (stacking restored). */
	private void writeToContainerTag() {
		CompoundTag tTag = mContainer.getOrCreateTag();
		mTank.writeToNBT(tTag, TileEntityBase08Barrel.NBT_TANK);
		if (tTag.isEmpty()) mContainer.setTag(null);
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tTag = new CompoundTag();
		mTank.writeToNBT(tTag, TileEntityBase08Barrel.NBT_TANK);
		return tTag;
	}

	@Override
	public void deserializeNBT(CompoundTag aNBT) {
		mTank.readFromNBT(aNBT, TileEntityBase08Barrel.NBT_TANK);
	}

	// ---------------------------------------------------------------------------
	// IFluidHandlerItem / IFluidHandler — the template shape over the long tank
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
	@NotNull
	public FluidStack getFluidInTank(int aTank) {
		FluidStack tFluid = mTank.getFluid();
		return tFluid == null || tFluid.isEmpty() ? FluidStack.EMPTY : tFluid;
	}

	@Override
	public int getTankCapacity(int aTank) {
		return mTank.getCapacity(); // the bindInt boundary over the long capacity
	}

	@Override
	public boolean isFluidValid(int aTank, @NotNull FluidStack aStack) {
		return mTank.isFluidValid(aStack);
	}

	/** Upstream item fill (TileEntityBase08Barrel :248-258, writeItemNBT back onto the stack) with the template count guard (:108). */
	@Override
	public int fill(@Nullable FluidStack aFluid, FluidAction aAction) {
		if (mContainer.getCount() != 1) return 0;
		int tFilled = mTank.fill(aFluid, aAction);
		if (tFilled > 0 && aAction.execute()) writeToContainerTag(); // upstream :256 UT.NBT.set(aStack, writeItemNBT(...))
		return tFilled;
	}

	@Override
	@NotNull
	public FluidStack drain(int aDrained, FluidAction aAction) {
		if (mContainer.getCount() != 1) return FluidStack.EMPTY;
		FluidStack tDrained = mTank.drain(aDrained, aAction);
		if (aAction.execute()) writeToContainerTag();
		return tDrained == null ? FluidStack.EMPTY : tDrained;
	}

	/** The identity-matched overload — the guard, then the tank's own {@code contains} gate (FluidTankGT.drain(FluidStack)). */
	@Override
	@NotNull
	public FluidStack drain(@Nullable FluidStack aFluid, FluidAction aAction) {
		if (aFluid == null || aFluid.isEmpty() || mContainer.getCount() != 1) return FluidStack.EMPTY;
		FluidStack tDrained = mTank.drain(aFluid, aAction);
		if (aAction.execute()) writeToContainerTag();
		return tDrained == null ? FluidStack.EMPTY : tDrained;
	}

	/** The capability this provider serves (the FLUID_HANDLER_ITEM slot, ForgeCapabilities.java:22). */
	@Override
	@NotNull
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> aCap, @Nullable Direction aSide) {
		return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(aCap, mHolder);
	}
}
