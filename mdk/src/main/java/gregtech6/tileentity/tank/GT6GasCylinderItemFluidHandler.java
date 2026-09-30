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
 * The item face of the gas-cylinder family (task small-tank-gas-cylinder) — the
 * GTBarrelItemFluidHandler template over the cylinder's two family doors:
 * <ul>
 * <li>the LIQUIDPROOF F admission gate — every non-gas fluid refuses at fill
 *     ({@link GT6GasCylinderBlockEntity#allowsFluid}, the upstream isFluidAllowed :419-421
 *     row pair inverted from the barrel's gas gate);</li>
 * <li>the NBT_MODE adjustable capacity — the container tag's {@code mode} long re-binds
 *     the tank on construction (upstream readFromNBT2 :49-53; a stack whose limit never
 *     left the default carries no mode key and rides the 8000 L class default), and a
 *     fill/drain persists the pair back under the same keys the placed BE reads
 *     ({@link GT6GasCylinderBlockEntity#NBT_MODE} + {@link GT6GasCylinderBlockEntity
 *     #NBT_TANK}).</li>
 * </ul>
 *
 * <p>Template shape ({@code FluidHandlerItemStack}, forge-1.20.1 templates/
 * FluidHandlerItemStack.java): the handler holds the container ItemStack by reference
 * (:37), {@code fill}/{@code drain} refuse stacks with {@code getCount() != 1} (:108),
 * and the write-back drops an emptied, default-limit tag entirely so a drained cylinder
 * stacks again (the barrel write gate).
 *
 * <p>{@link ICapabilitySerializable} is the persistence seam (the
 * GTBarrelItemFluidHandler form): serialize/deserialize ride the same pair of keys.
 */
//? if forge {
public class GT6GasCylinderItemFluidHandler implements IFluidHandlerItem, ICapabilitySerializable<CompoundTag> {
//?} else {
/*public class GT6GasCylinderItemFluidHandler implements IFluidHandlerItem {
 *///?}

	//? if forge {
	private final LazyOptional<IFluidHandlerItem> mHolder = LazyOptional.of(() -> this);
	//?}

	@NotNull
	private final ItemStack mContainer;

	private final FluidTankGT mTank = new FluidTankGT(GT6GasCylinderBlockEntity.DEFAULT_CAPACITY);

	/**
	 * @param aContainer the live container stack — held by reference (the Forge template
	 *        {@code container} field); an existing tag re-hydrates the mode + content
	 * @param aCapacityL the tank size — the block-carrier 8000 L row value, the ceiling
	 *        the NBT_MODE limit clamps against
	 */
	public GT6GasCylinderItemFluidHandler(@NotNull ItemStack aContainer, long aCapacityL) {
		mContainer = aContainer;
		mTank.setCapacity(aCapacityL);
		//? if forge {
		if (aContainer.hasTag()) readFromContainerTag();
		//?} else {
		/*if (aContainer.get(GT6DataComponents.BARREL_CONTENT) != null) readFromContainerTag();
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// the container-tag persistence seam — the one key pair both sides read
	// ---------------------------------------------------------------------------

	//? if forge {
	private void readFromContainerTag() {
		CompoundTag tTag = mContainer.getTag();
		if (tTag == null) return;
		if (tTag.contains(GT6GasCylinderBlockEntity.NBT_MODE, CompoundTag.TAG_ANY_NUMERIC)) {
			mTank.setCapacity(Math.max(1, Math.min(GT6GasCylinderBlockEntity.DEFAULT_CAPACITY, tTag.getLong(GT6GasCylinderBlockEntity.NBT_MODE))));
		}
		mTank.readFromNBT(tTag, GT6GasCylinderBlockEntity.NBT_TANK);
	}
	//?} else {
	/*private void readFromContainerTag() {
		CustomData tData = mContainer.get(GT6DataComponents.BARREL_CONTENT);
		if (tData == null) return;
		CompoundTag tTag = tData.copyTag();
		if (tTag.contains(GT6GasCylinderBlockEntity.NBT_MODE, CompoundTag.TAG_ANY_NUMERIC)) {
			mTank.setCapacity(Math.max(1, Math.min(GT6GasCylinderBlockEntity.DEFAULT_CAPACITY, tTag.getLong(GT6GasCylinderBlockEntity.NBT_MODE))));
		}
		mTank.readFromNBT(tTag, GT6GasCylinderBlockEntity.NBT_TANK);
	}
	 *///?}

	/** Writes the pair back; an emptied tank at the default limit removes the keys and a then-empty tag is dropped (stacking restored). */
	//? if forge {
	private void writeToContainerTag() {
		CompoundTag tTag = mContainer.getOrCreateTag();
		mTank.writeToNBT(tTag, GT6GasCylinderBlockEntity.NBT_TANK);
		if (mTank.capacity() != GT6GasCylinderBlockEntity.DEFAULT_CAPACITY) tTag.putLong(GT6GasCylinderBlockEntity.NBT_MODE, mTank.capacity());
		if (tTag.isEmpty()) mContainer.setTag(null);
	}
	//?} else {
	/*private void writeToContainerTag() {
		CompoundTag tTag = new CompoundTag();
		mTank.writeToNBT(tTag, GT6GasCylinderBlockEntity.NBT_TANK);
		if (mTank.capacity() != GT6GasCylinderBlockEntity.DEFAULT_CAPACITY) tTag.putLong(GT6GasCylinderBlockEntity.NBT_MODE, mTank.capacity());
		if (tTag.isEmpty()) mContainer.remove(GT6DataComponents.BARREL_CONTENT);
		else CustomData.set(GT6DataComponents.BARREL_CONTENT, mContainer, tTag);
	}
	 *///?}

	//? if forge {
	@Override
	//?}
	public CompoundTag serializeNBT() {
		CompoundTag tTag = new CompoundTag();
		mTank.writeToNBT(tTag, GT6GasCylinderBlockEntity.NBT_TANK);
		if (mTank.capacity() != GT6GasCylinderBlockEntity.DEFAULT_CAPACITY) tTag.putLong(GT6GasCylinderBlockEntity.NBT_MODE, mTank.capacity());
		return tTag;
	}

	//? if forge {
	@Override
	//?}
	public void deserializeNBT(CompoundTag aNBT) {
		if (aNBT.contains(GT6GasCylinderBlockEntity.NBT_MODE, CompoundTag.TAG_ANY_NUMERIC)) {
			mTank.setCapacity(Math.max(1, Math.min(GT6GasCylinderBlockEntity.DEFAULT_CAPACITY, aNBT.getLong(GT6GasCylinderBlockEntity.NBT_MODE))));
		}
		mTank.readFromNBT(aNBT, GT6GasCylinderBlockEntity.NBT_TANK);
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
		FluidStack tFluid = mTank.fluid();
		return tFluid == null || tFluid.isEmpty() ? FluidStack.EMPTY : tFluid;
	}

	@Override
	public int getTankCapacity(int aTank) {
		return mTank.getCapacity(); // the bindInt boundary over the long capacity
	}

	@Override
	public boolean isFluidValid(int aTank, @NotNull FluidStack aStack) {
		return !aStack.isEmpty() && GT6GasCylinderBlockEntity.allowsFluid(GTFluidLists.name(aStack));
	}

	/**
	 * The template count guard (:108) + the LIQUIDPROOF F gate first (upstream :188
	 * isFluidAllowed refuses before the tank math), then the tank fill; the committed
	 * write-back rides the upstream :256 item-NBT face (writeItemNBT2 :63 mode included).
	 */
	@Override
	public int fill(@Nullable FluidStack aFluid, FluidAction aAction) {
		if (mContainer.getCount() != 1) return 0;
		if (aFluid == null || aFluid.isEmpty() || !GT6GasCylinderBlockEntity.allowsFluid(GTFluidLists.name(aFluid))) return 0;
		int tFilled = mTank.fill(aFluid, aAction);
		if (tFilled > 0 && aAction.execute()) writeToContainerTag();
		return tFilled;
	}

	/** Drain carries no fluid gate — a cylinder still holding its fill always drains back (the pre-card stock clean-up path, the barrel face). */
	@Override
	@NotNull
	public FluidStack drain(int aDrained, FluidAction aAction) {
		if (mContainer.getCount() != 1) return FluidStack.EMPTY;
		FluidStack tDrained = mTank.drain(aDrained, aAction);
		if (aAction.execute()) writeToContainerTag();
		return tDrained == null ? FluidStack.EMPTY : tDrained;
	}

	/** The identity-matched overload — the guard, then the tank's own gate (FluidTankGT.drain(FluidStack)). */
	@Override
	@NotNull
	public FluidStack drain(@Nullable FluidStack aFluid, FluidAction aAction) {
		if (aFluid == null || aFluid.isEmpty() || mContainer.getCount() != 1) return FluidStack.EMPTY;
		FluidStack tDrained = mTank.drain(aFluid, aAction);
		if (aAction.execute()) writeToContainerTag();
		return tDrained == null ? FluidStack.EMPTY : tDrained;
	}

	//? if forge {
	/** The capability this provider serves (the FLUID_HANDLER_ITEM slot, ForgeCapabilities.java:22). */
	@Override
	@NotNull
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> aCap, @Nullable net.minecraft.core.Direction aSide) {
		return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(aCap, mHolder);
	}
	//?} else {
	/*// (1.21.1: no item-capability provider face here — IForgeItem.initCapabilities does not
	// exist; the W4 registration wave exposes this handler through
	// RegisterCapabilitiesEvent.registerItem(FLUID_HANDLER_ITEM, item, provider), the
	// GTBarrelItemFluidHandler declared defer.)
	 *///?}
}
