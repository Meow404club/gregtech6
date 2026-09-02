package gregtech6.tileentity.energy.generators;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.fluid.FluidTankGT;
import gregtech6.fluid.GTFluidLists;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.attachment.GTTapBlockEntity;

/**
 * 1.20.1 counterpart of the GT6 Liquid Burning Box — task p13-burning-box-family spec ②,
 * ported from gregtech/tileentity/energy/generators/MultiTileEntityGeneratorLiquid.java
 * (:63-285), the FM.Burn ("gt.recipe.fuels.burn") fluid-fuel HU source.
 *
 * <p><b>The tank shape</b>: upstream carries ONE tank — the fuel tank
 * ({@code mTank = new FluidTankGT(1000)}, :73, capacity re-bound to {@code mRate * 10}
 * at :85). The task card's "双罐（燃料+输出）" reading is a CARD-TEXT ERRATUM: the
 * upstream class has no output tank — the FM.Burn rows' CO2 fluid output is NEVER
 * stored (the burn loop probes and consumes inputs only, :135-145; the outputs go to
 * the floor), the same output-less shape as the Solid family's container arm. The port
 * follows the upstream verbatim: one fuel tank, outputs discarded.
 *
 * <p><b>The tick (upstream onTick2 :113-171, server branch verbatim)</b>:
 * <ol>
 * <li>the burning-or-cooldown arm (:116) — the emit (:118-125, the Solid form with
 *     FLAME_RANGE 2 :64) and the refuel gate (:127-154): under two packets the flag
 *     drops (:129), the front {@code WD.burn} arm (:131) is POOLED (declared, the
 *     Solid-class ruling), and on an open front face (:133 — the oxygen arm folds, the
 *     Solid-class WD.oxygen ruling) the fuel row is searched against the tank
 *     (:135-145): found → {@code mBurning = T}, {@code mCooldown = 100}, charge
 *     {@code units(absoluteTotalPower, 10000, mEfficiency, F)} (:140) and KEEP burning
 *     in the :142 while-loop until the head-room fills or the tank empties; the
 *     invalid-fuel clear (:146-149) empties the tank so the fuel type can swap; the
 *     no-air arm zeroes the cooldown (:150-153). Burn-out at :156.</li>
 * <li>the idle arm (:157-162) — the front auto-ignite pair, {@code rng(200) == 0 &&
 *     WD.flaming(front)} → burning (the Solid-class mechanic).</li>
 * <li>the cooldown decay (:166) — the auto-re-ignite window ticks down one per tick.</li>
 * </ol>
 *
 * <p><b>The Tap face (:63 class head {@code ITileEntityTapAccessible} + :228-231)</b>:
 * ported as {@link GTTapBlockEntity.TapAccessible} — the port's tap-interface carrier —
 * with {@link #tapDrain} draining the fuel tank UNCONDITIONALLY (the upstream tap face
 * bypasses the burning gate that governs the pipe face, :218-220; the card's "tapDrain
 * 只开放燃料罐" is exactly this: the one tank the box owns). The PIPE/capability face
 * (getFluidTankFillable2 :213-215 / getFluidTankDrainable2 :218-220 /
 * getFluidTanks2 :223-225) is the pool card — no FLUID_HANDLER capability is mounted
 * this card; the fill-side gates live on as the pure predicates
 * {@link #acceptsFuelFluid} / {@link #acceptsFuelFluidGas} for the tests and that card.</p>
 *
 * <p><b>The fluid-only lookup seam</b>: the diesel-engine closure repeated for the
 * FM.Burn map (the port {@code RecipeMap.findRecipe} hard-returns on empty item
 * inputs, RecipeMap.java:137-138 — the
 * {@code gregtech6.tileentity.energy.GTDieselEngineBlockEntity.findFuelRecipe} class
 * doc seam): {@link #findFuelRecipe} re-expresses the upstream :135 search
 * ({@code isRecipeInputEqual(false, true, fluids, EMPTY)} over the linear
 * {@code mRecipeList}) for this BE family only.</p>
 *
 * <p><b>The port hierarchy ruling</b>: upstream Liquid is a SIBLING of Solid (both
 * extend TileEntityBase09FacingSingle) and duplicates the energy face (:241-247);
 * the port collapses the pair into an inheritance chain (Liquid extends
 * {@link GTGeneratorSolidBlockEntity}) for the shared rng/fire/front-face/adjacency
 * machinery — the card's "类数以 BE 行为分族最小为准" ruling. Behavioural note: the
 * inherited 2-slot inventory stays unused (upstream Liquid has
 * {@code getDefaultInventory → ZL_IS}, :238 — an empty inventory), and the energy
 * face answers identically through the inheritance. The GAS family extends this
 * class exactly like upstream (:38).</p>
 *
 * <p><b>Cropped with declaration</b>: the plunger/magnifying-glass tool faces
 * (:183-188 — the readout rides {@code /gt6burner stat}); the client particles (:169);
 * the contact heat damage (:235), the ×16 burning hardness (:253), the 0.875
 * collision box (:236 — pool-card family, the Solid-class doc); and the texture
 * family (:233/:261-282 — one shared block texture per family, the crank ruling).
 */
public class GTGeneratorLiquidBlockEntity extends GTGeneratorSolidBlockEntity implements GTTapBlockEntity.TapAccessible {

	/** The upstream :64 flame range — the Liquid family spreads fire one block less far than the Solid family. */
	public static final int LIQUID_FLAME_RANGE = 2;

	/** The upstream :66 cooldown — the auto-re-ignite window (100 on every successful burn, :138). */
	public byte mCooldown = 0;

	/** The upstream :73 fuel tank — capacity re-bound to {@code mRate * 10} (:85), the ONE tank of the box. */
	public final FluidTankGT mTank = new FluidTankGT(1000);

	/** BET factory — the concrete family (Liquid/GAS shared type) resolves its shared type. */
	public GTGeneratorLiquidBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "burning_box.liquid"; // the family base name; the concrete BET rows carry their own registry paths
	}

	/** The fuel map accessor — the live FM.Burn map by default, the injected fixture offline. */
	@Nullable
	protected RecipeMap mBurnMapOverride = null;

	public RecipeMap burnMap() {
		return mBurnMapOverride != null ? mBurnMapOverride : GT6RecipeMaps.BURN;
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		syncFacingFromState();
		if (mBurning || mCooldown > 0) { // :116
			// :118-125 — the emit + the fire spread (the Solid form, FLAME_RANGE 2)
			if (mEnergy >= mRate) {
				ITileEntityEnergy.Util.emitEnergyToNetwork(mEnergyTypeEmitted, 1, Math.min(mRate, mEnergy), this, adjacency());
				mEnergy -= mRate;
				if (shouldSpreadFire(mEfficiency, rng(mEfficiency))) trySpreadFire(LIQUID_FLAME_RANGE);
			}
			// :127-154 — the refuel gate
			if (mEnergy < mRate * 2) {
				mBurning = false; // :129 — set back to true if the Recipe finds enough Fuel
				// :131 — WD.burn front-face flammable ignition: POOLED (the Solid-class ruling)
				if (frontFaceOpen()) { // :133 (the oxygen arm folds — the Solid-class WD.oxygen ruling)
					Recipe tRecipe = findFuelRecipe(mLastRecipe, tankFluids());
					if (tRecipe != null && consumeFuel(tRecipe)) {
						mBurning = true; // :137
						mCooldown = 100; // :138
						mLastRecipe = tRecipe; // :139
						mEnergy += units(tRecipe.getAbsoluteTotalPower(), 10000, mEfficiency, false); // :140
						// :142-145 — burn as much as needed to keep up the Power per Tick
						while (mEnergy < mRate * 2 && consumeFuel(tRecipe)) {
							mEnergy += units(tRecipe.getAbsoluteTotalPower(), 10000, mEfficiency, false); // :143
							if (mTank.isEmpty()) break; // :144
						}
					} else {
						mTank.setEmpty(); // :148 — the fuel-type swap clear
					}
				} else {
					mCooldown = 0; // :152 — well, no air, no fire
				}
			}
			// :156 — out of fuel
			if (mEnergy < mRate) mBurning = false;
		} else {
			// :159-161 — something burning in front of it? Let's ignite!
			if (rng(200) == 0 && frontIsFlaming()) mBurning = true;
		}
		if (mEnergy < 0) mEnergy = 0; // :164
		// :166 — the auto-re-ignite window decay
		if (mCooldown > 0) mCooldown--;
	}

	/** The mLastRecipe fast path (upstream :72 field, the :135 search argument). */
	@Nullable
	public Recipe mLastRecipe = null;

	/** The fuel tank as the probe argument ({@code mTank.AS_ARRAY} upstream). */
	protected FluidStack[] tankFluids() {
		FluidStack tFluid = mTank.getFluid();
		return tFluid != null && !tFluid.isEmpty() ? new FluidStack[] {tFluid} : new FluidStack[0];
	}

	/**
	 * The upstream :135 findRecipe call re-expressed for the fluid-only map (the
	 * GTDieselEngineBlockEntity.findFuelRecipe closure, class doc seam): the
	 * {@code mLastRecipe} fast path then the linear {@code mRecipeList} scan, both
	 * probing {@code isRecipeInputEqual(false, true, fluids, EMPTY)} — search never
	 * checks amounts, the apply does.
	 */
	@Nullable
	public Recipe findFuelRecipe(@Nullable Recipe aLastRecipe, FluidStack[] aFluids) {
		RecipeMap tMap = burnMap();
		if (tMap == null) return null;
		// the :487 fast path
		if (aLastRecipe != null && !aLastRecipe.mFakeRecipe && aLastRecipe.mCanBeBuffered
				&& aLastRecipe.isRecipeInputEqual(false, true, aFluids, ZL_IS)) {
			return aLastRecipe.mEnabled && absGreaterEqual(Long.MAX_VALUE, aLastRecipe.mEUt) ? aLastRecipe : null;
		}
		// the :498-501 scan
		for (Recipe tRecipe : tMap.mRecipeList) {
			if (tRecipe.mFakeRecipe || !tRecipe.isRecipeInputEqual(false, true, aFluids, ZL_IS)) continue;
			return tRecipe.mEnabled && absGreaterEqual(Long.MAX_VALUE, tRecipe.mEUt) ? tRecipe : null;
		}
		return null;
	}

	/** The port's ZL_IS (empty item array) form, the diesel spelling. */
	public static final net.minecraft.world.item.ItemStack[] ZL_IS = new net.minecraft.world.item.ItemStack[0];

	/** Upstream UT.Code.abs_greater_equal (UT.java:1727), the RecipeMap.java:154 form. */
	private static boolean absGreaterEqual(long aAmount1, long aAmount2) {
		return Math.abs(aAmount1) >= Math.abs(aAmount2);
	}

	/**
	 * The :136/:142 apply — the probe/ledger consume of the diesel engine
	 * (GTDieselEngineBlockEntity.consumeFuel): the (F, F) form checks the amounts, the
	 * removal goes through the tank ledger (the burn-dry identity deviation, the diesel
	 * class doc).
	 */
	protected boolean consumeFuel(Recipe aRecipe) {
		FluidStack[] tFluids = tankFluids();
		if (!aRecipe.isRecipeInputEqual(false, false, tFluids, ZL_IS)) return false;
		long tConsume = 0;
		for (FluidStack tInput : aRecipe.mFluidInputs) if (tInput != null && !tInput.isEmpty()) tConsume += tInput.getAmount();
		if (tConsume > 0) mTank.remove(tConsume);
		return true;
	}

	/**
	 * The upstream containsInput(Fluid) fill gate (:214 {@code mRecipes.containsInput(...)
	 * && !FL.gas(...)}) — "is this fluid a valid FM.Burn input AND not a gas" (the
	 * Liquid family refuses gas fuel; the GAS family inverts the tail,
	 * MultiTileEntityGeneratorGas.java:41 {@code && FL.gas(...)}). The gas check rides
	 * the registry path over {@link GTFluidLists#isGas} (the p13-steam-proof-repay
	 * GTFluidLists carrier).
	 */
	public boolean acceptsFuelFluid(@Nullable FluidStack aFluid) {
		return containsBurnInput(aFluid) && !GTFluidLists.isGas(fluidPath(aFluid));
	}

	/** The upstream containsInput(Fluid) (:189/:204 gate) for the FM.Burn map — the diesel containsFuelInput closure. */
	public boolean containsBurnInput(@Nullable FluidStack aFluid) {
		RecipeMap tMap = burnMap();
		if (tMap == null || aFluid == null || aFluid.isEmpty()) return false;
		for (Recipe tRecipe : tMap.mRecipeList) {
			for (FluidStack tInput : tRecipe.mFluidInputs)
				if (tInput != null && !tInput.isEmpty() && tInput.isFluidEqual(aFluid)) return true;
		}
		return false;
	}

	/** The registry path of the fluid, or null (the GTFluidLists name key). */
	@Nullable
	public static String fluidPath(@Nullable FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return null;
		Fluid tFluid = aFluid.getFluid();
		var tKey = ForgeRegistries.FLUIDS.getKey(tFluid);
		return tKey == null ? null : tKey.getPath();
	}

	// ---------------------------------------------------------------------------
	// the Tap face (upstream :63 ITileEntityTapAccessible + :228-231 verbatim)
	// ---------------------------------------------------------------------------

	/**
	 * The upstream tapDrain (:228-231) — drains the fuel tank UNCONDITIONALLY (the tap
	 * face bypasses the burning gate of the pipe face, :218-220; the {@code
	 * updateInventory()} of :229 is a no-op here, no inventory). The card's "tapDrain
	 * 只开放燃料罐" — the one tank the box owns IS the fuel tank.
	 */
	@Override
	public FluidStack tapDrain(byte aSide, int aMaxDrain, boolean aDoDrain) {
		return mTank.drain(aMaxDrain, aDoDrain
				? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
				: net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
	}

	// ---------------------------------------------------------------------------
	// the fire spread override (FLAME_RANGE 2 — the :123 volume)
	// ---------------------------------------------------------------------------

	/** The :123 spread with the Liquid family's FLAME_RANGE 2 volume. */
	protected void trySpreadFire(int aFlameRange) {
		if (!hasLevel()) return;
		BlockPos tCenter = getBlockPos();
		int tX = tCenter.getX() - aFlameRange + rng(2 * aFlameRange + 1);
		int tY = tCenter.getY() - 1 + rng(2 + aFlameRange);
		int tZ = tCenter.getZ() - aFlameRange + rng(2 * aFlameRange + 1);
		placeFire(getLevel(), new BlockPos(tX, tY, tZ));
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :75-87 / writeToNBT2 :89-96)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte("gt.cooldown", mCooldown); // :92 NBT_COOLDOWN
		mTank.writeToNBT(aNBT, "gt.tank"); // :95 NBT_TANK
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains("gt.cooldown", Tag.TAG_ANY_NUMERIC)) mCooldown = aNBT.getByte("gt.cooldown"); // :80
		// :85-86 — the capacity re-bind AFTER the rate (the super load reads NBT_OUTPUT first)
		mTank.readFromNBT(aNBT, "gt.tank").setCapacity(mRate * 10);
	}
}
