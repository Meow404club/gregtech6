package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregtech6.fluid.FluidTankGT;
import gregtech6.fluid.GTDrinks;
import gregtech6.fluid.GTFluidLists;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The small fluid-container BE base (task small-tank-cup) — the port counterpart of the
 * upstream cup/jug base chain {@code TileEntityBase10FluidContainerSyncSmall} ←
 * {@code TileEntityBase09FluidContainerSync} ← {@code TileEntityBase08FluidContainer}
 * (tmp/gt6-1.7.10 gregapi/tileentity/tank/, the "Fluid Containers" carriers). The tank +
 * five-proof admission door + the drink seam + the rain-collect ticker + the item-NBT
 * projection, all reusable: the cup lands on it today, the jug card (2000 L,
 * canWaterCrops/canPickUpFluids) reuses it tomorrow. NOT a barrel supertype by design —
 * the barrel carries the cover machinery (TileEntityBase08Barrel:98-167) the upstream
 * small tanks refuse ({@code allowCovers → F}, TileEntityBase10:41), so this base takes
 * the small-tank subset only.
 *
 * <p>The five-proof door (upstream isFluidAllowed TileEntityBase08FluidContainer:419-421):
 * the power-conducting head refuses first, then the {@code (FL.gas ? mGasProof :
 * mLiquidProof)} branch admits. The acid/plasma branches and the NBT_TEMPERATURE ceiling
 * are the DECLARED POOL CUT — the port has no acid/plasma/magic fluid datasets and no
 * fluid-temperature face (the cell-card recorded-only ruling); the proof flags still ride
 * the NBT keys upstream wrote (NBT_LIQUIDPROOF/NBT_GASPROOF/... verbatim names) so the
 * recorded columns survive for a future dataset card. The magic flag is vacuous under the
 * cut (MAGICPROOF=T rows admit everything the cut admits).
 *
 * <p>The drink seam (upstream onBlockActivated3 :158-168 + isDrinkable :415-417): a tank
 * holding ≥ {@link GTDrinks#DRINK_MB} of a {@link GTDrinks#REGISTER}-keyed fluid drains
 * the 250 L, plays the drink sound and pays the {@link GTDrinks.DrinkStat} (hunger/sat
 * through {@link GTDrinks#drink}, the food-fluids-b2 mirror). {@link #consumeDrink()} is
 * the player-less core (the offline-test face), {@link #tryTankDrink(Player)} adds the
 * sound + stat faces. The canDrinkFromSide gate lives on the concrete block (cup = any
 * side, MultiTileEntityCup.java:85; jug = top only, :90).
 *
 * <p>The rain collector (upstream onTick2 TileEntityBase08FluidContainer:114-125 over the
 * {@code canFillWithRain()} :427 hook): every 600 ticks while raining, an open-sky cup
 * catches {@code max(1, downfall*100)} L of water — ×2 while thundering — with the
 * vanilla 1.20.1 faces standing in for the 1.7.10 ones ({@code biome.rainfall} →
 * {@code getModifiedClimateSettings().downfall()}, the forge/neoforge patch pair, and
 * {@code tBiome.rainfall > 0 && tBiome.temperature >= 0.2} →
 * {@code getPrecipitationAt == RAIN && getBaseTemperature() >= 0.2}); the tick phase rides
 * the per-BE {@code mTimer} instead of the global SERVER_TIME (same cadence, phase offset
 * by load tick — declared simplification). {@code getRainOffset} collapses to the vanilla
 * canSeeSky + a no-liquid-above check (the upstream :120 obstruction pair).
 */
public abstract class TileEntityBase10FluidContainerSmall extends TileEntityBase03TicksAndSync {

	/** Upstream CS NBT_TANK — the tank content (the in-repo plain "tank" form, the barrel/cell face). */
	public static final String NBT_TANK = "tank";

	/** Upstream CS NBT keys (CS.java:1201-1205 + :1272 verbatim) — the five-proof + temperature faces. */
	public static final String NBT_LIQUIDPROOF = "gt.liquidproof", NBT_GASPROOF = "gt.gasproof", NBT_ACIDPROOF = "gt.acidproof",
			NBT_PLASMAPROOF = "gt.plasmaproof", NBT_MAGICPROOF = "gt.magicproof";

	/** Upstream CS NBT_TEMPERATURE = "gt.temperature" — recorded-only under the temperature cut (the cell ruling). */
	public static final String NBT_TEMPERATURE = "gt.temperature";

	/** Upstream :68 {@code mTank = new FluidTankGT(1000)} — the capacity rides the concrete row. */
	public final FluidTankGT mTank;

	/** Upstream :69 defaults — LIQUIDPROOF T, GASPROOF/ACID/PLASMA/MAGIC F; the concrete row flips its pair. */
	protected boolean mLiquidProof = true, mGasProof = false, mAcidProof = false, mPlasmaProof = false, mMagicProof = false;

	/** Upstream :80 — the recorded temperature ceiling column (no port consumer, the cell recorded-only ruling). */
	protected long mTemperatureMax;

	/** BET factory for BlockEntityType.Builder.of (the cell form). */
	protected TileEntityBase10FluidContainerSmall(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		this(aType, aPos, aState, 1000);
	}

	/** The full constructor — the concrete row capacity + ticking=ON (the rain collector is the tick work). */
	protected TileEntityBase10FluidContainerSmall(BlockEntityType<?> aType, BlockPos aPos, BlockState aState, long aCapacityL) {
		super(true, aType, aPos, aState);
		mTank = new FluidTankGT(aCapacityL);
	}

	@Override
	public String getTileEntityName() {
		return "gt.multitileentity.small_fluid_container"; // the base placeholder — the concrete rows override verbatim
	}

	// ---------------------------------------------------------------------------
	// the rain collector (upstream onTick2 :114-125 over canFillWithRain :427)
	// ---------------------------------------------------------------------------

	/** The upstream :427 hook — default F, the cup flips to T (MultiTileEntityCup.java:84). */
	public boolean canFillWithRain() {
		return false;
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		if (!aIsServerSide || !canFillWithRain() || !hasLevel()) return;
		if (aTimer % 600 != 10 || !getLevel().isRaining() || !getLevel().canSeeSky(getBlockPos().above())) return;
		BlockPos tAbove = getBlockPos().above();
		if (!getLevel().getFluidState(tAbove).isEmpty()) return; // the :120 no-liquid-above half
		Biome tBiome = getLevel().getBiome(tAbove).value();
		if (tBiome.getPrecipitationAt(tAbove) != Biome.Precipitation.RAIN || tBiome.getBaseTemperature() < 0.2F) return;
		long tDrop = Math.max(1, (long)(tBiome.getModifiedClimateSettings().downfall() * 100)) * (getLevel().isThundering() ? 2 : 1);
		mTank.fill(new FluidStack(Fluids.WATER, (int)tDrop), FluidAction.EXECUTE); // upstream :121 FL.Water.make(...), T
		onTankChanged();
	}

	// ---------------------------------------------------------------------------
	// the drink seam (upstream onBlockActivated3 :158-168 + isDrinkable :415-417)
	// ---------------------------------------------------------------------------

	/** The upstream :415-417 gate over the port mirror: ≥ 250 L AND the fluid is REGISTER-keyed, else null. */
	@Nullable
	public GTDrinks.DrinkStat drinkableStat() {
		if (!mTank.has(GTDrinks.DRINK_MB) || mTank.getFluid() == null) return null;
		return GTDrinks.stat(GTFluidLists.name(mTank.getFluid()));
	}

	/**
	 * The player-less drink core: drains exactly the upstream 250 L ({@code mTank.remove(250)}
	 * :166) and flags the sync. False when the tank is not drinkable — the acceptance
	 * 扣量 face, the offline-test seam.
	 */
	public boolean consumeDrink() {
		if (drinkableStat() == null) return false;
		mTank.remove(GTDrinks.DRINK_MB);
		onTankChanged();
		return true;
	}

	/**
	 * The full block-side seam: the {@link GTDrinks#drink} stat face (hunger/sat, the
	 * 饱食度 carrier) + the drink sound (upstream SFX.MC_DRINK :164 — the EAT branch keys
	 * on the AppleCore food-action face the modern stat surface does not carry, declared
	 * simplification) + {@link #consumeDrink()}. Null/false-tolerant for the block call.
	 */
	public boolean tryTankDrink(Player aPlayer) {
		GTDrinks.DrinkStat tStat = drinkableStat();
		if (tStat == null || !GTDrinks.drink(aPlayer, tStat)) return false;
		if (hasLevel() && isServerSide()) {
			getLevel().playSound(null, getBlockPos(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		return consumeDrink();
	}

	// ---------------------------------------------------------------------------
	// the admission door (upstream isFluidAllowed :419-421, the cut form)
	// ---------------------------------------------------------------------------

	/** The shared gate body over the row's gas/liquid pair — the instance and item faces meet here. */
	protected static boolean admissionGate(@Nullable String aFluidName, boolean aGasProof, boolean aLiquidProof) {
		if (aFluidName == null) return false; // the :419 head
		if (GTFluidLists.isPowerConducting(aFluidName)) return false; // the :420 power-conducting head (the barrel :250 order)
		return GTFluidLists.isGas(aFluidName) ? aGasProof : aLiquidProof; // the :420 gas/liquid branch
		// the acid/plasma/magic branches and the FL.temperature ceiling: the declared pool cut (class doc)
	}

	/** The instance door (the jug card's override point — flip the row flags or override). */
	public boolean allowsFluid(@Nullable String aFluidName) {
		return admissionGate(aFluidName, mGasProof, mLiquidProof);
	}

	// ---------------------------------------------------------------------------
	// the content face (upstream getMaxStackSize :423 — content kills stacking)
	// ---------------------------------------------------------------------------

	/** The upstream :423 face — the item stack-gate reads this: a filled container stacks 1. */
	public boolean hasContent() {
		return mTank.has();
	}

	// ---------------------------------------------------------------------------
	// the tank change fan-out (the cell onTankChanged shape — the level sync is concrete)
	// ---------------------------------------------------------------------------

	/** The executed-change hook — setChanged + the client sync; concrete BEs override to add the level property. */
	public void onTankChanged() {
		setChanged();
		updateClientData();
	}

	// ---------------------------------------------------------------------------
	// the NBT pair (the Base08 :82/:88 read/write shape — cell form)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_LIQUIDPROOF)) mLiquidProof = aNBT.getBoolean(NBT_LIQUIDPROOF);
		if (aNBT.contains(NBT_GASPROOF)) mGasProof = aNBT.getBoolean(NBT_GASPROOF);
		if (aNBT.contains(NBT_ACIDPROOF)) mAcidProof = aNBT.getBoolean(NBT_ACIDPROOF);
		if (aNBT.contains(NBT_PLASMAPROOF)) mPlasmaProof = aNBT.getBoolean(NBT_PLASMAPROOF);
		if (aNBT.contains(NBT_MAGICPROOF)) mMagicProof = aNBT.getBoolean(NBT_MAGICPROOF);
		if (aNBT.contains(NBT_TEMPERATURE)) mTemperatureMax = aNBT.getLong(NBT_TEMPERATURE);
		mTank.readFromNBT(aNBT, NBT_TANK);
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		mTank.writeToNBT(aNBT, NBT_TANK);
	}

	/** The item-face write (the upstream writeItemNBT2 item face — the drop/placed stack carries the tank). */
	public CompoundTag writeItemNBT(CompoundTag aNBT) {
		mTank.writeToNBT(aNBT, NBT_TANK);
		return aNBT;
	}

	/** The item-face read (the placement construction — the fresh BE adopts the stack's tank). */
	public void readItemNBT(CompoundTag aNBT) {
		load(aNBT);
		setChanged();
	}

	// ---------------------------------------------------------------------------
	// the fluid capability (the fresh-wrapper-per-call form, the cell shape)
	// ---------------------------------------------------------------------------

	/** The single-tank all-sides handler over the row door (fill gated, drain always). */
	public IFluidHandler newFluidHandler() {
		return new SmallTankFluidHandler(this);
	}

	/** The one-tank IFluidHandler — the cell handler shape over the base door. */
	protected static final class SmallTankFluidHandler implements IFluidHandler {
		private final TileEntityBase10FluidContainerSmall mContainer;

		SmallTankFluidHandler(TileEntityBase10FluidContainerSmall aContainer) {
			mContainer = aContainer;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int aTank) {
			FluidStack tFluid = mContainer.mTank.fluid();
			return tFluid == null ? FluidStack.EMPTY : tFluid;
		}

		@Override
		public int getTankCapacity(int aTank) {
			return aTank == 0 ? (int)mContainer.mTank.capacity() : 0;
		}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {
			return aStack != null && !aStack.isEmpty() && mContainer.allowsFluid(GTFluidLists.name(aStack));
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty() || !mContainer.allowsFluid(GTFluidLists.name(aResource))) return 0;
			return mContainer.mTank.fill(aResource, aAction);
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			FluidStack tDrained = mContainer.mTank.drain(aResource, aAction);
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			FluidStack tDrained = mContainer.mTank.drain(aMaxDrain, aAction);
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}
	}

	//? if forge {
	/** The cached side-less fluid face (the cell mFluidCap form). */
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
	// by the GT6CapabilityWiring registerBlockEntity row exactly like the cell.)
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, net.minecraft.core.Direction> aCapability,
			@Nullable net.minecraft.core.Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
			return (T) newFluidHandler(); // the cell seam cast
		}
		return null;
	}
	 *///?}
}
