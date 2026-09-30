package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregtech6.block.tank.GT6CellBlock;
import gregtech6.fluid.FluidTankGT;
import gregtech6.fluid.GTFluidLists;
import gregtech6.registry.GT6Cells;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Capsule-Cell-Container — the port counterpart of upstream
 * {@code MultiTileEntityCell} (tmp/gt6-1.7.10 .../tanks/MultiTileEntityCell.java,
 * the "Fluid Containers" 40-row band Loader_MultiTileEntities.java:1770-1809: every
 * row NBT_TANK_CAPACITY 1000, NBT_GASPROOF T, stack 64). The upstream class chain
 * TileEntityBase10FluidContainerSyncSmall maps onto the in-repo
 * {@link TileEntityBase03TicksAndSync} (NOT ticking — once the rain/drink faces stay
 * the declared pool cut the family carries no tick work; the fill-level display
 * syncs event-driven from {@link #onTankChanged}, the crucible LIQUID_LEVEL face
 * TileEntitySmeltery:263-266 minus the ticker).
 *
 * <p><b>The gas-only fill gate (upstream isFluidAllowed TileEntityBase08FluidContainer
 * :419-421, the row pair NBT_GASPROOF T + NBT_LIQUIDPROOF default F)</b>: the
 * power-conducting list refuses first, LIQUIDPROOF F rejects every non-gas, GASPROOF T
 * admits the gas band. The acid/plasma/magic branches and the FL.temperature ceiling
 * (per-row NBT_TEMPERATURE mp-10/mp-50/2700/MAX) ride the declared pool cut — the port
 * has no fluid-temperature dataset for them (the GTBarrels P4 quartet note); the
 * per-row proof/temperature columns are recorded verbatim on the
 * {@link GT6Cells.CellRow} table and pinned by the offline census.
 *
 * <p><b>The stack-64 family override</b>: upstream {@code MultiTileEntityCell
 * .getMaxStackSize :76} returns {@code aDefault} — a FILLED cell still stacks to 64
 * (the one small-tank family that does; the base :423 content-kills-stacking form is
 * overridden away). The port rides the plain BlockItem 64 default — the BlockItem
 * deliberately carries NO getMaxStackSize override.
 *
 * <p><b>The fluid display</b>: upstream pass 2 stacks insides + BlockTextureFluid +
 * sides on the side faces (MultiTileEntityCell.java:40-46). The port renders the fill
 * LEVEL through the static per-level {@code LIQUID_LEVEL} 0..8 blockstate (the
 * crucible-bowl-card declared simplification — the static model cannot know the BE's
 * actual fluid; the placeholder sprite + no-tint ceilings are declared on the model).
 *
 * <p><b>Pool cuts (the small-tank-family tier-SM card)</b>: the drink arm (gas-only
 * admission keeps isDrinkable :415-417 false), rain collection (canFillWithRain :427
 * default F upstream — no cut at all), the tap/funnel arms (the ITileEntityTapFillable
 * top-gated face has no port consumer on this family), the upstream
 * {@code IMTE_IgnorePlayerCollisionWhenPlacing :78} flag (no BlockBehaviour seam —
 * declared deviation, sub-cube collision makes it near-invisible) and the tooltips
 * (the dormant gt6.tooltip.tank registry row — a tooltip-card face).
 */
public class GT6CellBlockEntity extends TileEntityBase03TicksAndSync {

	/** Upstream CS NBT_TANK — the tank content (the in-repo plain "tank" form, the barrel/cylinder face). */
	public static final String NBT_TANK = "tank";

	/** Upstream Loader :1770-1809 NBT_TANK_CAPACITY — 1000 on every one of the 40 rows. */
	public static final long CAPACITY = 1000;

	/** Upstream MultiTileEntityCell.java:76 — the family keeps stacking at 64 filled or not. */
	public static final int STACK_SIZE = 64;

	/** Upstream :46 mTank — the single small-container tank (1000 L, all 40 rows). */
	public final FluidTankGT mTank = new FluidTankGT(CAPACITY);

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime (the gas-cylinder form). */
	public GT6CellBlockEntity(BlockPos aPos, BlockState aState) {
		this(GT6Cells.CELL_BE.get(), aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GTBarrelBlockEntity explicit-type form). */
	public GT6CellBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType, aPos, aState); // NOT ticking — the rain/drink cuts leave no tick work
	}

	@Override
	public String getTileEntityName() {
		return "gt.multitileentity.cell"; // upstream MultiTileEntityCell.java:80 verbatim
	}

	// ---------------------------------------------------------------------------
	// the fill-level display face (the crucible LIQUID_LEVEL bucket, event-driven)
	// ---------------------------------------------------------------------------

	/** The executed-change sync (the TileEntityBase08Barrel.onTankChanged shape) + the level bucket ride. */
	public void onTankChanged() {
		setChanged();
		updateClientData();
		syncLevelProperty();
	}

	/** The 0..8 fill bucket — 1 + (amount-1)*8/capacity keeps bucket 1 the first non-empty liter step. */
	public int levelBucket() {
		long tAmount = mTank.amount();
		return tAmount <= 0 ? 0 : (int)(1 + (tAmount - 1) * 8 / mTank.capacity());
	}

	/** The property ride (the TileEntitySmeltery:263-266 shape, UPDATE_CLIENTS only — no neighbor work). */
	private void syncLevelProperty() {
		if (!hasLevel()) return;
		BlockState tState = getBlockState();
		if (tState.hasProperty(GT6CellBlock.LIQUID_LEVEL) && tState.getValue(GT6CellBlock.LIQUID_LEVEL) != levelBucket()) {
			getLevel().setBlock(getBlockPos(), tState.setValue(GT6CellBlock.LIQUID_LEVEL, levelBucket()), Block.UPDATE_CLIENTS);
		}
	}

	// ---------------------------------------------------------------------------
	// the gas-only fill gate (upstream isFluidAllowed :419-421, the row proof pair)
	// ---------------------------------------------------------------------------

	/**
	 * The admission gate over the fluid registry-path name: the power-conducting list
	 * refuses first (the barrel :250 order), then LIQUIDPROOF F rejects every non-gas —
	 * GASPROOF T is what lets the gas band through. Package-visible for the offline pin.
	 */
	static boolean allowsFluid(@Nullable String aFluidName) {
		return !GTFluidLists.isPowerConducting(aFluidName) && GTFluidLists.isGas(aFluidName);
	}

	// ---------------------------------------------------------------------------
	// NBT pair (the Base08 :82/:88 read/write shape — capacity is fixed, no MODE key)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		mTank.readFromNBT(aNBT, NBT_TANK);
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		mTank.writeToNBT(aNBT, NBT_TANK);
	}

	/**
	 * The item-face write (the upstream writeItemNBT2 item face): the drop/placed stack
	 * carries the tank — the {@link gregtech6.block.tank.GT6CellBlock#getDrops} drop face
	 * and the {@link gregtech6.item.GT6CellBlockItem} placement readback ride this one key.
	 */
	public CompoundTag writeItemNBT(CompoundTag aNBT) {
		mTank.writeToNBT(aNBT, NBT_TANK);
		return aNBT;
	}

	/** The item-face read (the upstream placement construction — the fresh BE adopts the stack's tank). */
	public void readItemNBT(CompoundTag aNBT) {
		load(aNBT);
		setChanged();
	}

	// ---------------------------------------------------------------------------
	// the fluid capability (the fresh-wrapper-per-call form, the gas-cylinder shape)
	// ---------------------------------------------------------------------------

	/** The single-tank all-sides handler (fill gated to gas, drain always — the cylinder drain face). */
	public IFluidHandler newFluidHandler() {
		return new CellFluidHandler(this);
	}

	/** The one-tank IFluidHandler — the cylinder handler shape with the same LIQUIDPROOF door. */
	private static final class CellFluidHandler implements IFluidHandler {
		private final GT6CellBlockEntity mCell;

		CellFluidHandler(GT6CellBlockEntity aCell) {
			mCell = aCell;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int aTank) {
			FluidStack tFluid = mCell.mTank.fluid();
			return tFluid == null ? FluidStack.EMPTY : tFluid;
		}

		@Override
		public int getTankCapacity(int aTank) {
			return aTank == 0 ? (int)mCell.mTank.capacity() : 0;
		}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {
			return aStack != null && !aStack.isEmpty() && allowsFluid(GTFluidLists.name(aStack));
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty() || !allowsFluid(GTFluidLists.name(aResource))) return 0;
			return mCell.mTank.fill(aResource, aAction);
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			FluidStack tDrained = mCell.mTank.drain(aResource, aAction);
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			FluidStack tDrained = mCell.mTank.drain(aMaxDrain, aAction);
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}
	}

	//? if forge {
	/** The cached side-less fluid face (the gas-cylinder mFluidCap form). */
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
	// by the GT6CapabilityWiring registerBlockEntity row exactly like the gas cylinder.)
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, net.minecraft.core.Direction> aCapability,
			@Nullable net.minecraft.core.Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
			return (T) newFluidHandler(); // the gas-cylinder seam cast
		}
		return null;
	}
	 *///?}
}
