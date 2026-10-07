package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregtech6.fluid.FluidTankGT;
import gregtech6.registry.GT6MeasuringPot;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.attachment.GTTapBlockEntity;

/**
 * The Measuring Pot — the port counterpart of upstream
 * {@code MultiTileEntityMeasuringPot} (tmp/gt6-1.7.10 .../tanks/
 * MultiTileEntityMeasuringPot.java, the "Fluid Containers" ceramic row,
 * Loader_MultiTileEntities.java:2096: hardness 0.5 / resistance 6.0 /
 * NBT_TANK_CAPACITY 1000 / aUtilStone). The upstream class chain
 * TileEntityBase10FluidContainerSyncSmall + ITileEntityTapFillable maps onto the
 * in-repo {@link TileEntityBase03TicksAndSync} + {@link GTTapBlockEntity.TapFillable}
 * pair (the GT6ManualKitchenBlockEntity mounting shape).
 *
 * <p><b>The hitY fill-limit face (upstream onBlockActivated3 :73-93, verbatim doors)</b>:
 * an EMPTY hand on a HORIZONTAL side re-binds the tank capacity — upper half (&gt; 4px)
 * raises it, lower half lowers it; the coarse/middle/fine bands are &gt;6px / 4-6px /
 * 2-4px against the click height, and sneaking swaps every step for its ÷10 twin
 * (+50/+10/+1 with sneak). The result clamps to {@code [1, 1000]} (upstream
 * {@code UT.Code.bind(1, mCapacity, ...)} against the ORIGINAL tank capacity) and the
 * override rides NBT_MODE (upstream readFromNBT2/writeToNBT2 :48-58 — written only when
 * the capacity left the class default, and the item NBT face :61-64 carries it too).
 * The block routes this face through {@link #limitChain} (the RCON-report channel, the
 * GTTapBlockEntity.activateChain precedent — a null player means the empty-hand arm with
 * the non-sneaking coarse steps).
 *
 * <p><b>The fluid faces:</b> the single {@link #mTank} (1000 L default) answers
 * {@link #tapFill} directly (the ITileEntityTapFillable :27-29 semantics — no fill doors,
 * the proof-flag family rides the declared barrel-base pool cut) and the block's
 * {@code FluidUtil.interactWithFluidHandler} fallthrough (the GTBarrelBlock.use shape)
 * covers the held-container fill/drain clicks through the FLUID_HANDLER capability.
 *
 * <p><b>Pool cuts (the issue-#45 C3 card):</b> the rain collection
 * (TileEntityBase08FluidContainer.onTick2 :116-125, {@code canFillWithRain} — the same
 * cut the kitchen family declared), the drink/eat arm (:158-169), the water-crops and
 * item-NBT faces (:203+), the plunger/magnifying-glass tool arms (:131-137) and the
 * fluid render pass (the mDisplay visual pool — the block model is the static sub-cube
 * tub, the kitchen-family form). NOT ticking: no per-tick behaviour survives the cuts.
 */
public class GT6MeasuringPotBlockEntity extends TileEntityBase03TicksAndSync implements GTTapBlockEntity.TapFillable {

	/** Upstream CS NBT_TANK — the tank content ("gt.tank", CS.java:1258). */
	public static final String NBT_TANK = "tank";
	/** Upstream NBT_MODE — the adjusted capacity override, written only off the default (:56/:62). */
	public static final String NBT_MODE = "mode";

	/** Upstream :45/:50 — the class default the limit clamps against (the Loader :2096 NBT_TANK_CAPACITY 1000). */
	public static final long DEFAULT_CAPACITY = 1000;

	/** The hitY band bounds — upstream PX_P[4] / PX_P[6] / PX_P[2] (the pixel-pair form CS.PX_P). */
	public static final float PX_BAND_LOWER = 4.0F / 16.0F;
	public static final float PX_BAND_MIDDLE = 6.0F / 16.0F;
	public static final float PX_BAND_UPPER = 2.0F / 16.0F;

	/** The coarse steps (non-sneaking) — upstream :78/:80/:84/:86 literals. */
	public static final long STEP_COARSE = 50;
	/** The sneaking steps — every coarse step's ÷10 twin. */
	public static final long STEP_FINE = 5;
	public static final long STEP_MIDDLE = 10;
	public static final long STEP_FINEST = 1;

	/** Upstream :68 mTank — the single small-container tank. */
	public final FluidTankGT mTank = new FluidTankGT(DEFAULT_CAPACITY);

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime (the GT6BathingPotBlockEntity form). */
	public GT6MeasuringPotBlockEntity(BlockPos aPos, BlockState aState) {
		this(GT6MeasuringPot.MEASURING_POT_BE.get(), aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GTBarrelBlockEntity explicit-type form). */
	public GT6MeasuringPotBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType, aPos, aState); // NOT ticking — the rain/tool/render cuts leave no tick work
	}

	@Override
	public String getTileEntityName() {
		return "gt.multitileentity.measuring.pot"; // upstream :148 verbatim
	}

	// ---------------------------------------------------------------------------
	// the hitY fill-limit face (upstream onBlockActivated3 :73-93)
	// ---------------------------------------------------------------------------

	/**
	 * The empty-hand horizontal-side limit re-bind. {@code aPlayer == null} = the RCON arm
	 * (never sneaking). Returns the report string ("Limit: <n>L" — the upstream :89 chat
	 * face); the client side is a no-op report (upstream returned T client-first :75).
	 */
	public String limitChain(@Nullable Player aPlayer, float aHitY) {
		boolean tSneaking = aPlayer != null && aPlayer.isShiftKeyDown();
		long tCapacity = mTank.capacity();
		if (!isServerSide()) return "client side"; // upstream :75 — the client pass commits nothing
		long tDelta;
		if (aHitY > PX_BAND_LOWER) {
			tDelta = aHitY > PX_BAND_MIDDLE ? step(tSneaking, STEP_COARSE, STEP_FINE)
					: step(tSneaking, STEP_MIDDLE, STEP_FINEST); // :77-81
		} else {
			tDelta = aHitY > PX_BAND_UPPER ? -step(tSneaking, STEP_MIDDLE, STEP_FINEST)
					: -step(tSneaking, STEP_COARSE, STEP_FINE); // :83-87
		}
		long tNew = Math.max(1, Math.min(DEFAULT_CAPACITY, tCapacity + tDelta)); // UT.Code.bind(1, mCapacity, ...)
		mTank.setCapacity(tNew);
		onTankChanged();
		return "Limit: " + tNew + "L"; // upstream :89
	}

	/** The sneak swap — coarse 50/10, sneaking 5/1 (upstream :78/:80/:84/:86 ternary pair); package-visible for the offline band pin. */
	static long step(boolean aSneaking, long aCoarse, long aSneak) {
		return aSneaking ? aSneak : aCoarse;
	}

	/** The executed-change sync (the TileEntityBase08Barrel.onTankChanged shape). */
	public void onTankChanged() {
		setChanged();
		updateClientData();
	}

	// ---------------------------------------------------------------------------
	// the TapFillable face (upstream ITileEntityTapFillable — no fill doors, the proof
	// flags ride the declared pool cut; the kitchen tapFill shape over the single tank)
	// ---------------------------------------------------------------------------

	@Override
	public int tapFill(byte aSide, FluidStack aFluid, boolean aDoFill) {
		return mTank.fill(aFluid, aDoFill ? FluidAction.EXECUTE : FluidAction.SIMULATE);
	}

	// ---------------------------------------------------------------------------
	// NBT pair (upstream readFromNBT2 :48-52 / writeToNBT2 :55-58 — the mode rides only
	// off the default; the tank content always)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC)) mTank.setCapacity(aNBT.getLong(NBT_MODE)); // :51
		mTank.readFromNBT(aNBT, NBT_TANK); // :52 (the Base08 :82 read shape)
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (mTank.capacity() != DEFAULT_CAPACITY) aNBT.putLong(NBT_MODE, mTank.capacity()); // :56
		mTank.writeToNBT(aNBT, NBT_TANK); // :58 (the Base08 :88 write shape)
	}

	// ---------------------------------------------------------------------------
	// the fluid capability (the fresh-wrapper-per-call form, the kitchen newFluidHandler
	// shape over one tank)
	// ---------------------------------------------------------------------------

	/** The single-tank all-sides handler (fill/drain straight into {@link #mTank}). */
	public IFluidHandler newFluidHandler() {
		return new PotFluidHandler(this);
	}

	/** The one-tank IFluidHandler — the KitchenFluidHandler shape with the doors cut. */
	private static final class PotFluidHandler implements IFluidHandler {
		private final GT6MeasuringPotBlockEntity mPot;

		PotFluidHandler(GT6MeasuringPotBlockEntity aPot) {
			mPot = aPot;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int aTank) {
			FluidStack tFluid = mPot.mTank.fluid();
			return tFluid == null ? FluidStack.EMPTY : tFluid;
		}

		@Override
		public int getTankCapacity(int aTank) {
			return aTank == 0 ? (int)mPot.mTank.capacity() : 0;
		}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {
			return aStack != null && !aStack.isEmpty();
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			return mPot.mTank.fill(aResource, aAction);
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			FluidStack tDrained = mPot.mTank.drain(aResource, aAction);
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			FluidStack tDrained = mPot.mTank.drain(aMaxDrain, aAction);
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}
	}

	//? if forge {
	/** The cached side-less fluid face (the kitchen mItemCap form; the pot exposes fluid only). */
	private net.minecraftforge.common.util.LazyOptional<IFluidHandler> mFluidCap =
			net.minecraftforge.common.util.LazyOptional.of(this::newFluidHandler);

	@Override
	public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> aCapability,
			@Nullable net.minecraft.core.Direction aSide) {
		if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
			return mFluidCap.cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mFluidCap.invalidate();
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 deleted BlockEntity#getCapability — this member is the
	// provider seam (no @Override: the parent method does not exist on 21.1), delegated to
	// by the GT6CapabilityWiring registerBlockEntity row exactly like the kitchen family.)
	// the (T) ItemHandler cast: T pairs with aCapability by the caller contract; erasure = zero bytecode
	@SuppressWarnings("unchecked")
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, net.minecraft.core.Direction> aCapability,
			@Nullable net.minecraft.core.Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
			return (T) newFluidHandler();
		}
		return null;
	}
	 *///?}
}
