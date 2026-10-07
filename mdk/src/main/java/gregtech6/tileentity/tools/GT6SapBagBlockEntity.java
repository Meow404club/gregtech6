package gregtech6.tileentity.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.fluid.FluidTankGT;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GT6MiscToolBlocks;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The GT6 Sap Bag — 1.20.1 counterpart of
 * {@code gregtech/tileentity/tools/MultiTileEntitySapBag.java} (200 lines), the :2221
 * row ("Resin/Sap Bag", meta 32736): the 1-slot / 8000 L trunk-mounted collector bag.
 *
 * <p>Semantics, each clause anchored:
 * <ul>
 * <li>the tank :57 — {@code FluidTankGT(8000)}, the NBT_TANK_CAPACITY column verbatim
 *     (the registration constant rides {@link GT6MiscToolBlocks#SAP_BAG_TANK_CAPACITY});</li>
 * <li>the FULL flag :56/:92-94 — tank or slot occupied; upstream flips it in onTick2,
 *     the port folds it EVENT-DRIVEN (the static-storage adjacency-wake fold: strictly
 *     not later, zero ticks) through {@link #recountFull()}, and the blockstate FULL
 *     property is the live truth (:195-196 visual data byte);</li>
 * <li>the drain view — the click arm's bag-fills-container face (:106 FL.fill(mTank,
 *     container)); the upstream tank is a bare FluidTankGT with NO automation
 *     capability face, so this view is handed out ONLY to the block's click arm
 *     (fill-face-closed, drain-face-open — nothing can push into the bag);</li>
 * <li><b>the collection tick CUT</b> (the class doc of the registry): the upstream
 *     :80-95 tree-hole walk has no port counterpart domain — the resin-hole MTE domain
 *     is a later card (the GT6TreeFeature spec-④ cut); the bag therefore never fills
 *     itself until that domain lands. NO TICK is mounted (the no-tick chain — every
 *     remaining upstream tick work was the collection arm + the FULL recount).</li>
 * </ul>
 */
public class GT6SapBagBlockEntity extends TileEntityBase03TicksAndSync {

	/** NBT key of the slot contents (the static-storage plain form). */
	public static final String NBT_INVENTORY = "inventory";

	/** NBT key of {@link #mFacing} (the static-storage plain form). */
	public static final String NBT_FACING = "facing";

	/** Upstream CS NBT_TANK ("tank") — the FluidTankGT key (the Base08 form). */
	public static final String NBT_TANK = "tank";

	/** The tank (:57). */
	public final FluidTankGT mTank = new FluidTankGT(GT6MiscToolBlocks.SAP_BAG_TANK_CAPACITY);

	/** The collected stack (upstream getDefaultInventory :173 = ItemStack[1]). */
	private GTItemStackHandler mSlots;

	/** Upstream 09FacingSingle mFacing default — the blockstate FACING is the live truth. */
	protected byte mFacing = 3;

	/** The visual FULL byte (:56). */
	private boolean mFull = false;

	/** BET factory for BlockEntityType.Builder.of. */
	public GT6SapBagBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.SAP_BAG_BE.get(), aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6SapBagBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType, aPos, aState); // the collection tick is cut — no tick mounted
		mSlots = new GTItemStackHandler(1, this::recountFull);
	}

	@Override
	public String getTileEntityName() {
		return "sap_bag"; // upstream :199 "gt.multitileentity.sapbag" — the BET path mirrors it
	}

	/** The collected-stack slot face (the GTEntityBlock drop walk reads it reflectively). */
	public GTItemStackHandler getInventory() {
		return mSlots;
	}

	/**
	 * The FULL recount (:92-94 mFull = mTank.has() || slotHas(0)), event-driven; the
	 * blockstate FULL property is the live truth (the dynamo syncActiveToState form).
	 */
	public void recountFull() {
		boolean tFull = mTank.has() || !mSlots.getStackInSlot(0).isEmpty();
		setChanged();
		if (tFull != mFull && hasLevel()) {
			BlockState tState = getBlockState();
			if (tState.hasProperty(GT6MiscToolBlocks.GT6SapBagBlock.FULL)
					&& tState.getValue(GT6MiscToolBlocks.GT6SapBagBlock.FULL) != tFull) {
				mFull = tFull;
				getLevel().setBlock(getBlockPos(), tState.setValue(GT6MiscToolBlocks.GT6SapBagBlock.FULL, tFull), 3);
				return;
			}
		}
		mFull = tFull;
	}

	/** The visual FULL flag (the :195 getter face). */
	public boolean isFull() {
		return mFull;
	}

	/** The facing byte — the blockstate FACING when present, the NBT byte otherwise. */
	public byte getFacing() {
		BlockState tState = getBlockState();
		if (tState != null && tState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return (byte) tState.getValue(BlockStateProperties.HORIZONTAL_FACING).get3DDataValue();
		}
		return mFacing;
	}

	/** The NBT-fallback byte write (the block's setPlacedBy keeps it in step — the storage form). */
	public void setFacingNbtFallback(byte aFacing) {
		mFacing = aFacing;
	}

	/**
	 * The drain-only tank view for the click arm (the class doc): the drain face is the
	 * upstream :106 FL.fill(mTank, container) pull; the fill face stays CLOSED (nothing
	 * pushes into the bag — the tank has no automation capability upstream).
	 */
	public IFluidHandler drainView() {
		return new DrainView(this);
	}

	private static final class DrainView implements IFluidHandler {
		private final GT6SapBagBlockEntity mBag;

		DrainView(GT6SapBagBlockEntity aBag) {
			mBag = aBag;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int aTank) {
			FluidStack tFluid = mBag.mTank.fluid();
			return tFluid == null ? FluidStack.EMPTY : tFluid;
		}

		@Override
		public int getTankCapacity(int aTank) {
			return aTank == 0 ? (int)mBag.mTank.capacity() : 0;
		}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {
			return true; // the bare FluidTankGT semantics — no admission door
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			return 0; // the closed fill face (the class doc)
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			FluidStack tDrained = mBag.mTank.drain(aResource, aAction);
			if (tDrained != null && !tDrained.isEmpty() && aAction.execute()) mBag.recountFull();
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			FluidStack tDrained = mBag.mTank.drain(aMaxDrain, aAction);
			if (tDrained != null && !tDrained.isEmpty() && aAction.execute()) mBag.recountFull();
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}
	}

	// ---------------------------------------------------------------------------
	// NBT (the static-storage plain form + the tank key)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_FACING, mFacing);
		//? if forge {
		aNBT.put(NBT_INVENTORY, mSlots.serializeNBT());
		//?} else {
		/*aNBT.put(NBT_INVENTORY, mSlots.serializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS)); // 21.1: provider-first
		 *///?}
		mTank.writeToNBT(aNBT, NBT_TANK);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_FACING, Tag.TAG_ANY_NUMERIC)) {
			mFacing = aNBT.getByte(NBT_FACING);
		}
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) {
			//? if forge {
			mSlots.deserializeNBT(aNBT.getCompound(NBT_INVENTORY));
			//?} else {
			/*mSlots.deserializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS, aNBT.getCompound(NBT_INVENTORY)); // 21.1: provider-first
			 *///?}
		}
		mTank.readFromNBT(aNBT, NBT_TANK);
		mFull = mTank.has() || !mSlots.getStackInSlot(0).isEmpty();
	}
}
