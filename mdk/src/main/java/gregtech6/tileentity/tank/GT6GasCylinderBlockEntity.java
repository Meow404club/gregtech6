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
import gregtech6.fluid.GTFluidLists;
import gregtech6.registry.GT6GasCylinders;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Barometer Gas Cylinder — the port counterpart of upstream
 * {@code MultiTileEntityBarometerGasCylinder} (tmp/gt6-1.7.10 .../tanks/
 * MultiTileEntityBarometerGasCylinder.java, the "Fluid Containers" metal rows,
 * Loader_MultiTileEntities.java:2101-2104: four materials, NBT_TANK_CAPACITY 8000,
 * NBT_LIQUIDPROOF F + NBT_GASPROOF T = the gas-only proof pair, temperature
 * mp-50). The upstream class chain TileEntityBase09FluidContainerSmall (no sync →
 * no fluid render pass) maps onto the in-repo {@link TileEntityBase03TicksAndSync}
 * (NOT ticking — the small container carries no tick work once the rain/drink faces
 * stay the declared pool cut, the GT6MeasuringPotBlockEntity form).
 *
 * <p><b>The hitY fill-limit face (upstream onBlockActivated3 :73-110, verbatim doors)</b>:
 * an EMPTY hand on a HORIZONTAL side re-binds the tank capacity — above the midline it
 * raises (+500/+100/+50/+10 by descending quarter-bands), below it lowers (the mirrored
 * negatives), and sneaking swaps every step for its ÷10 twin. The result clamps to
 * {@code [1, 8000]} (upstream {@code UT.Code.bind(1, mCapacity, ...)} against the class
 * default) and the override rides NBT_MODE (upstream readFromNBT2 :49-53 /
 * writeToNBT2 :55-59 — written only when the capacity left the class default, the item
 * NBT face :61-65 carries it too). The block routes this face through
 * {@link #limitChain} (the GT6MeasuringPotBlockEntity precedent — a null player means
 * the empty-hand arm with the non-sneaking coarse steps).
 *
 * <p><b>The gas-only fill gate (upstream isFluidAllowed :419-421, the row proof pair)</b>:
 * LIQUIDPROOF F rejects every non-gas fluid, GASPROOF T admits the gas band, and the
 * power-conducting list refuses first (the GTBarrelItemFluidHandler gate order, inverted
 * outcome). The acid/plasma/magic branches and the FL.temperature ceiling ride the
 * declared pool cut — the port has no flag data or fluid-temperature dataset for them
 * (the GTBarrels P4 quartet note).
 *
 * <p><b>Pool cuts (the small-tank-family tier-S card):</b> the fluid render pass
 * (Base09 has no sync — the block model is the static two-layer elements model), the
 * drink arm (gas cylinders hold no potable fluids — isDrinkable :415-417 stays false by
 * the gate above), rain collection (canFillWithRain :427 default F upstream — no cut at
 * all), the tap/funnel arms (ITileEntityTapFillable is a Base10-sync family face; the
 * upstream :407-409 top-gated tapFill has no port consumer on this family) and the
 * tooltips (the dormant gt6.tooltip.tank registry row — a tooltip-card face).
 */
public class GT6GasCylinderBlockEntity extends TileEntityBase03TicksAndSync {

	/** Upstream CS NBT_TANK — the tank content ("gt.tank", CS.java:1258; the in-repo plain "tank" form, the barrel face). */
	public static final String NBT_TANK = "tank";
	/** Upstream NBT_MODE — the adjusted capacity override, written only off the default (:57/:63). */
	public static final String NBT_MODE = "mode";

	/** Upstream :46 — the class default the limit clamps against (the Loader :2101-2104 NBT_TANK_CAPACITY 8000, all four rows). */
	public static final long DEFAULT_CAPACITY = 8000;

	/** The hitY band bounds — upstream PX_P[2]/PX_P[4]/PX_P[6]/PX_P[8]/PX_P[10]/PX_P[12]/PX_P[14] (the pixel-pair form CS.PX_P). */
	public static final float PX_BAND_2 = 2.0F / 16.0F;
	public static final float PX_BAND_4 = 4.0F / 16.0F;
	public static final float PX_BAND_6 = 6.0F / 16.0F;
	public static final float PX_BAND_8 = 8.0F / 16.0F;
	public static final float PX_BAND_10 = 10.0F / 16.0F;
	public static final float PX_BAND_12 = 12.0F / 16.0F;
	public static final float PX_BAND_14 = 14.0F / 16.0F;

	/** The coarse steps (non-sneaking) — upstream :80/:82/:86/:88/:94/:96/:100/:102 literals. */
	public static final long STEP_500 = 500;
	public static final long STEP_100 = 100;
	public static final long STEP_50 = 50;
	public static final long STEP_10 = 10;
	/** The sneaking steps — every coarse step's ÷10 twin (upstream :80/:82/:86/:88/:94/:96/:100/:102 sneak ternaries). */
	public static final long STEP_FINE_50 = 50;
	public static final long STEP_FINE_10 = 10;
	public static final long STEP_FINE_5 = 5;
	public static final long STEP_FINE_1 = 1;

	/** Upstream :46 mTank — the single small-container tank (8000 L class default). */
	public final FluidTankGT mTank = new FluidTankGT(DEFAULT_CAPACITY);

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime (the GT6MeasuringPotBlockEntity form). */
	public GT6GasCylinderBlockEntity(BlockPos aPos, BlockState aState) {
		this(GT6GasCylinders.GAS_CYLINDER_BE.get(), aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GTBarrelBlockEntity explicit-type form). */
	public GT6GasCylinderBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType, aPos, aState); // NOT ticking — the rain/drink/render cuts leave no tick work
	}

	@Override
	public String getTileEntityName() {
		return "gt.multitileentity.barometer.gas.cylinder"; // upstream :150 verbatim
	}

	// ---------------------------------------------------------------------------
	// the hitY fill-limit face (upstream onBlockActivated3 :73-110)
	// ---------------------------------------------------------------------------

	/**
	 * The empty-hand horizontal-side limit re-bind. {@code aPlayer == null} = the RCON arm
	 * (never sneaking). Returns the report string ("Limit: <n>L" — the upstream :106 chat
	 * face); the client side is a no-op report (upstream returned T client-first :76).
	 */
	public String limitChain(@Nullable Player aPlayer, float aHitY) {
		boolean tSneaking = aPlayer != null && aPlayer.isShiftKeyDown();
		if (!isServerSide()) return "client side"; // upstream :76 — the client pass commits nothing
		long tCapacity = mTank.capacity();
		long tDelta;
		if (aHitY > PX_BAND_8) {
			if (aHitY > PX_BAND_12) {
				tDelta = aHitY > PX_BAND_14 ? band(tSneaking, STEP_500, STEP_FINE_50)
						: band(tSneaking, STEP_100, STEP_FINE_10); // :79-83
			} else {
				tDelta = aHitY > PX_BAND_10 ? band(tSneaking, STEP_50, STEP_FINE_5)
						: band(tSneaking, STEP_10, STEP_FINE_1); // :85-89
			}
		} else {
			if (aHitY > PX_BAND_4) {
				tDelta = aHitY > PX_BAND_6 ? -band(tSneaking, STEP_10, STEP_FINE_1)
						: -band(tSneaking, STEP_50, STEP_FINE_5); // :92-96
			} else {
				tDelta = aHitY > PX_BAND_2 ? -band(tSneaking, STEP_100, STEP_FINE_10)
						: -band(tSneaking, STEP_500, STEP_FINE_50); // :98-102
			}
		}
		long tNew = Math.max(1, Math.min(DEFAULT_CAPACITY, tCapacity + tDelta)); // UT.Code.bind(1, mCapacity, ...)
		mTank.setCapacity(tNew);
		onTankChanged();
		return "Limit: " + tNew + "L"; // upstream :106
	}

	/** The sneak swap — the upstream ternary pair per band; package-visible for the offline band pin (the pot step face). */
	static long band(boolean aSneaking, long aCoarse, long aSneak) {
		return aSneaking ? aSneak : aCoarse;
	}

	/** The executed-change sync (the TileEntityBase08Barrel.onTankChanged shape). */
	public void onTankChanged() {
		setChanged();
		updateClientData();
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
	// NBT pair (upstream readFromNBT2 :49-53 / writeToNBT2 :55-59 — the mode rides only
	// off the default; the tank content always)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC)) mTank.setCapacity(aNBT.getLong(NBT_MODE)); // :52
		mTank.readFromNBT(aNBT, NBT_TANK); // :51 (the Base08 :82 read shape)
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (mTank.capacity() != DEFAULT_CAPACITY) aNBT.putLong(NBT_MODE, mTank.capacity()); // :57
		mTank.writeToNBT(aNBT, NBT_TANK); // :58 (the Base08 :88 write shape)
	}

	/**
	 * The item-face write (upstream writeItemNBT2 :61-65): the drop/placed stack carries
	 * the mode override (only off the default) beside the tank — the GT6GasCylinderBlock
	 * .getDrops drop face and the {@link gregtech6.item.GT6GasCylinderBlockItem}
	 * placement readback ride this one pair.
	 */
	public CompoundTag writeItemNBT(CompoundTag aNBT) {
		if (mTank.capacity() != DEFAULT_CAPACITY) aNBT.putLong(NBT_MODE, mTank.capacity()); // :63
		mTank.writeToNBT(aNBT, NBT_TANK);
		return aNBT;
	}

	/** The item-face read (the upstream placement construction — the fresh BE adopts the stack's pair). */
	public void readItemNBT(CompoundTag aNBT) {
		load(aNBT);
		setChanged();
	}

	// ---------------------------------------------------------------------------
	// the fluid capability (the fresh-wrapper-per-call form, the pot newFluidHandler
	// shape over one tank — with the gas-only admission gate)
	// ---------------------------------------------------------------------------

	/** The single-tank all-sides handler (fill gated to gas, drain always — the barrel drain face). */
	public IFluidHandler newFluidHandler() {
		return new CylinderFluidHandler(this);
	}

	/** The one-tank IFluidHandler — the PotFluidHandler shape with the LIQUIDPROOF door. */
	private static final class CylinderFluidHandler implements IFluidHandler {
		private final GT6GasCylinderBlockEntity mCylinder;

		CylinderFluidHandler(GT6GasCylinderBlockEntity aCylinder) {
			mCylinder = aCylinder;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int aTank) {
			FluidStack tFluid = mCylinder.mTank.fluid();
			return tFluid == null ? FluidStack.EMPTY : tFluid;
		}

		@Override
		public int getTankCapacity(int aTank) {
			return aTank == 0 ? (int)mCylinder.mTank.capacity() : 0;
		}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {
			return aStack != null && !aStack.isEmpty() && allowsFluid(GTFluidLists.name(aStack));
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty() || !allowsFluid(GTFluidLists.name(aResource))) return 0;
			return mCylinder.mTank.fill(aResource, aAction);
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			FluidStack tDrained = mCylinder.mTank.drain(aResource, aAction);
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			FluidStack tDrained = mCylinder.mTank.drain(aMaxDrain, aAction);
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}
	}

	//? if forge {
	/** The cached side-less fluid face (the pot mFluidCap form; the cylinder exposes fluid only). */
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
	// by the GT6CapabilityWiring registerBlockEntity row exactly like the measuring pot.)
	// the (T) ItemHandler cast: T pairs with aCapability by the caller contract; erasure = zero bytecode
	@SuppressWarnings("unchecked")
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, net.minecraft.core.Direction> aCapability,
			@Nullable net.minecraft.core.Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
			return (T) newFluidHandler(); // the pot seam cast (GT6MeasuringPotBlockEntity :246)
		}
		return null;
	}
	 *///?}
}
