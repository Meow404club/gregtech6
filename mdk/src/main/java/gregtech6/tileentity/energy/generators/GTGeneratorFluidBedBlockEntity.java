package gregtech6.tileentity.energy.generators;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.fluids.FluidStack;

import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.fluid.FluidTankGT;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.attachment.GTTapBlockEntity;

/**
 * 1.20.1 counterpart of the GT6 Fluidized Bed Burning Box — task p13-burning-box-family
 * spec ④, ported from gregtech/tileentity/energy/generators/MultiTileEntityGeneratorFluidBed.java
 * (:65-305): the FM.FluidBed ("gt.recipe.fuels.fluidbed") dust+calcite HU source —
 * {@code mRecipes = FM.FluidBed} (:72, MIN=2: one ITEM and one FLUID input per row).
 *
 * <p><b>The fuel state (spec ⑥ archaeology verdict, the GT6RecipesBurnFuels class
 * doc)</b>: the upstream rows are the material-driven loop of
 * Loader_Fuels.java:37-43 (11 burning materials × 5 dust sizes, power
 * {@code mFurnaceBurnTime * 3 * EU_PER_FURNACE_TICK}, Calcite 648/72/18/8/1 L in, ash
 * dust out) — the port map is DECLARED-EMPTY (calcite/ash/burn-time primitives are
 * pool-card content), so the live box consumes an empty map and burns nothing until a
 * later card lands the data face. The BE machinery below is complete and the offline
 * tests drive it through an injected fixture map.</p>
 *
 * <p><b>The tick (upstream onTick2 :116-153, server branch — the Solid shape with the
 * FLUIDBED lookup)</b>: emit + fire spread (FLAME_RANGE 2, :66/:122-124); the refuel
 * gate (:126-139) searches with BOTH the tank AND the fuel slot as inputs
 * (:130 {@code findRecipe(..., mTank.AS_ARRAY, slot(0))}) and consumes both
 * (:131 {@code isRecipeInputEqual(T, F, mTank.AS_ARRAY, slot(0))}); the output item
 * (ash) lands in slot 1 (:133-134); the front auto-ignite pair at :144-146.</p>
 *
 * <p><b>The front click (:158-193)</b>: the Solid form MINUS the burning-time access —
 * the whole interaction refuses while burning (:160 {@code isServerSide() &&
 * !mBurning}; the click still consumes, :192).</p>
 *
 * <p><b>The Tap/Funnel faces (:238-248)</b>: tapDrain drains the fuel tank verbatim
 * (the Liquid-class ruling); funnelFill (:244-248) is the containsInput-gated tank
 * fill — both ported as pure methods ({@link #tapDrain}, {@link #funnelFill}), the
 * capability mounting stays the pool card.</p>
 *
 * <p><b>The tank capacity (:87)</b>: upstream {@code max(mRecipes.mMaxFluidInputSize,
 * mRate)} — the port RecipeMap carries no mMaxFluidInputSize field (the 15-param ctor
 * simplification), so the port folds the upstream default 1000 in:
 * {@code max(1000, mRate)} (declared).</p>
 *
 * <p><b>The port hierarchy ruling</b>: the Liquid-class collapse repeated — upstream
 * FluidBed is a SIBLING of Solid, the port extends
 * {@link GTGeneratorSolidBlockEntity} for the shared machinery and overrides the tick
 * (the dual-input lookup), the front click (the burning-time refusal) and the tank
 * carrier. The inherited 2-slot inventory IS the FluidBed inventory (fuel dust in
 * slot 0, ash out slot 1 — upstream :261).</p>
 */
public class GTGeneratorFluidBedBlockEntity extends GTGeneratorSolidBlockEntity implements GTTapBlockEntity.TapAccessible {

	/** The upstream :66 flame range (the Liquid-family value). */
	public static final int FLUIDBED_FLAME_RANGE = 2;

	/** The upstream :74 fluid tank — capacity max(1000, mRate), the :87 fold (class doc). */
	public final FluidTankGT mTank = new FluidTankGT(1000);

	/** The fuel map override — the injected fixture offline; the live (declared-empty) FM.FluidBed otherwise. */
	@Nullable
	protected RecipeMap mFluidBedMapOverride = null;

	public GTGeneratorFluidBedBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	/** The BET factory form (the diesel 2-arg shape — the shared type resolves at tick time). */
	public GTGeneratorFluidBedBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "burning_box.fluidbed"; // the family base name; the concrete BET rows carry their own registry paths
	}

	/** The fuel map accessor — the live FM.FluidBed map by default (empty until the calcite/ash/burn-time card). */
	@Nullable
	public RecipeMap fluidBedMap() {
		return mFluidBedMapOverride != null ? mFluidBedMapOverride : GT6RecipeMaps.FLUIDBED;
	}

	/** The capacity re-bind (:87, the mMaxFluidInputSize fold — class doc). */
	public void rebindTankCapacity() {
		mTank.setCapacity(Math.max(1000, mRate));
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		syncFacingFromState();
		if (mBurning) {
			// :119-125 — the emit + the fire spread (FLAME_RANGE 2)
			if (mEnergy >= mRate) {
				ITileEntityEnergy.Util.emitEnergyToNetwork(mEnergyTypeEmitted, 1, Math.min(mRate, mEnergy), this, adjacency());
				mEnergy -= mRate;
				if (shouldSpreadFire(mEfficiency, rng(mEfficiency))) trySpreadFire(FLUIDBED_FLAME_RANGE);
			}
			// :126-139 — the refuel gate (the dual-input lookup)
			if (mEnergy < mRate * 2) {
				// :127 — WD.burn front-face flammable ignition: POOLED
				// :128 — the mOutput1→slot-1 move folds into the direct slot-1 write (the Solid-class ruling)
				// :129 — the air/liquid/oxygen gate + a present fuel stack + a free output slot
				if (frontFaceOpen() && !mInventory.getStackInSlot(0).isEmpty() && canTakeContainerOutput()) {
					chargeFluidBedFuel();
				}
			}
			// :141 — out of fuel
			if (mEnergy < mRate) mBurning = false;
		} else {
			// :144-146 — something burning in front of it? Let's ignite!
			if (rng(200) == 0 && frontIsFlaming()) mBurning = true;
		}
		if (mEnergy < 0) mEnergy = 0; // :149
	}

	/**
	 * The :130-137 charge — the FLUIDBED row is consumed out of BOTH the tank and the
	 * fuel slot, the outputs (the ash) land in slot 1 (:133-134), and the buffer
	 * credits the efficiency translation (:135).
	 */
	protected void chargeFluidBedFuel() {
		RecipeMap tMap = fluidBedMap();
		if (tMap == null) return;
		FluidStack[] tFluids = tankFluids();
		ItemStack tFuel = mInventory.getStackInSlot(0);
		// the :130/:131 search+consume pair — probe both legs first, then take them
		Recipe tRecipe = findDualInputRecipe(tMap, tFluids, tFuel);
		if (tRecipe == null) return;
		if (!tRecipe.isRecipeInputEqual(false, false, tFluids, tFuel)) return; // :131 probe
		// the :131 consume — the tank leg through the ledger, the item leg through the slot
		// (the probe array holds live stacks, so the (T, ...) consume form is replaced by
		// the explicit takes — the diesel consumeFuel ledger ruling)
		long tConsume = 0;
		for (FluidStack tInput : tRecipe.mFluidInputs) if (tInput != null && !tInput.isEmpty()) tConsume += tInput.getAmount();
		if (tConsume > 0) mTank.remove(tConsume);
		int tItems = 0;
		for (ItemStack tInput : tRecipe.mInputs) if (tInput != null && !tInput.isEmpty()) tItems += tInput.getCount();
		if (tItems > 0) mInventory.extractItem(0, tItems, false);
		// :133-134 — the outputs to the output slot
		for (ItemStack tOut : tRecipe.mOutputs) {
			if (tOut == null || tOut.isEmpty()) continue;
			if (mInventory.getStackInSlot(1).isEmpty()) mInventory.setStackInSlot(1, tOut.copy());
			else if (ItemStack.isSameItemSameTags(mInventory.getStackInSlot(1), tOut)) mInventory.getStackInSlot(1).grow(tOut.getCount());
		}
		// :135 — the efficiency translation
		mEnergy += units(tRecipe.getAbsoluteTotalPower(), 10000, mEfficiency, false);
	}

	/** The fuel tank as the probe argument ({@code mTank.AS_ARRAY} upstream). */
	private FluidStack[] tankFluids() {
		FluidStack tFluid = mTank.getFluid();
		return tFluid != null && !tFluid.isEmpty() ? new FluidStack[] {tFluid} : new FluidStack[0];
	}

	/**
	 * The :130 search over the linear {@code mRecipeList} with BOTH input legs
	 * ({@code isRecipeInputEqual(false, true, fluids, items)}) — the port RecipeMap
	 * findRecipe shape does not cover the dual-leg scan against live slots, so the
	 * loop lives here (the Liquid-class seam, one more leg).
	 */
	@Nullable
	private Recipe findDualInputRecipe(RecipeMap aMap, FluidStack[] aFluids, ItemStack aFuel) {
		for (Recipe tRecipe : aMap.mRecipeList) {
			if (tRecipe.mFakeRecipe || !tRecipe.mEnabled) continue;
			if (tRecipe.isRecipeInputEqual(false, true, aFluids, aFuel)) return tRecipe;
		}
		return null;
	}

	// ---------------------------------------------------------------------------
	// the front click (:158-193 — the Solid form gated on !mBurning)
	// ---------------------------------------------------------------------------

	@Override
	public boolean useOnFront(Player aPlayer, InteractionHand aHand) {
		if (mBurning) return true; // :160 — refuses EVERY transfer while burning (the click still consumes, :192)
		return super.useOnFront(aPlayer, aHand);
	}

	// ---------------------------------------------------------------------------
	// the Tap/Funnel faces (:238-248 — the capability mounting stays the pool card)
	// ---------------------------------------------------------------------------

	/** The upstream tapDrain (:238-241) — the fuel tank, unconditionally (the Liquid-class ruling). */
	@Override
	public FluidStack tapDrain(byte aSide, int aMaxDrain, boolean aDoDrain) {
		return mTank.drain(aMaxDrain, aDoDrain
				? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
				: net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
	}

	/**
	 * The upstream funnelFill (:244-248) — the containsInput-gated tank fill (the
	 * calcite supply face; the diesel funnelFill form).
	 */
	public int funnelFill(@Nullable FluidStack aFluid, boolean aDoFill) {
		RecipeMap tMap = fluidBedMap();
		if (tMap == null || !containsFluidBedInput(aFluid)) return 0; // :245
		return mTank.fill(aFluid, aDoFill
				? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
				: net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE); // :247
	}

	/** The upstream containsInput(Fluid) (:224/:245 gate) for the FM.FluidBed map. */
	public boolean containsFluidBedInput(@Nullable FluidStack aFluid) {
		RecipeMap tMap = fluidBedMap();
		if (tMap == null || aFluid == null || aFluid.isEmpty()) return false;
		for (Recipe tRecipe : tMap.mRecipeList) {
			for (FluidStack tInput : tRecipe.mFluidInputs)
				if (tInput != null && !tInput.isEmpty() && tInput.isFluidEqual(aFluid)) return true;
		}
		return false;
	}

	/** The :123 spread with the FluidBed family's FLAME_RANGE 2 volume (the Liquid-class overload). */
	protected void trySpreadFire(int aFlameRange) {
		if (!hasLevel()) return;
		BlockPos tCenter = getBlockPos();
		int tX = tCenter.getX() - aFlameRange + rng(2 * aFlameRange + 1);
		int tY = tCenter.getY() - 1 + rng(2 + aFlameRange);
		int tZ = tCenter.getZ() - aFlameRange + rng(2 * aFlameRange + 1);
		placeFire(getLevel(), new BlockPos(tX, tY, tZ));
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :78-89 / writeToNBT2 :92-98)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		mTank.writeToNBT(aNBT, "gt.tank"); // :97
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		// :87 — the capacity re-bind AFTER the rate
		mTank.readFromNBT(aNBT, "gt.tank").setCapacity(Math.max(1000, mRate));
	}
}
