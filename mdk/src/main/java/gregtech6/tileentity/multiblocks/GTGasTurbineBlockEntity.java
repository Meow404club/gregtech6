package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.fluid.FluidTankGT;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.registry.GT6Turbines;

/**
 * The Gas Turbine controller (task p29-w3-turbine-dynamo ③) — the 1.20.1 counterpart of
 * MultiTileEntityLargeTurbineGas (MultiTileEntityLargeTurbineGas.java:44-155) over the
 * {@link GTMultiBlockConverter} base: HU packets accepted on the FRONT (NBT_ENERGY_ACCEPTED
 * HU, Loader :1264-1267) AND the FM.Gas fluid rows burned into the same capacitor, RU out
 * at the far plate. NBT_LIMIT_CONSUMPTION = T, NBT_WASTE_ENERGY = F (the emit deducts the
 * capacitor — waste only through the no-fuel idle drain below).
 *
 * <h2>The fuel half (LargeTurbineGas.java:107-139, verbatim shape)</h2>
 * <ul>
 * <li>the clamp leg (:108-115, the "hacking my own code" comment upstream): a stockpiled
 *     capacitor ≥ 2×NBT_INPUT converts one full window per tick, banking the excess;</li>
 * <li>the burn leg (:116-135): while stopped-clean, the input tank holds fuel and all
 *     three exhaust tanks are under half — find the FM.Gas row, parallel = min(the
 *     energy-deficit ceiling, the tank supply), consume, credit
 *     {@code parallel × |EUt| × duration}, fill the exhaust rows (a failed fill zeroes the
 *     capacitor, :127);</li>
 * <li>the idle drain (:136-138): no fuel found → the capacitor vents up to 2×NBT_INPUT
 *     before the base conversion runs (the "Exhaust Gas has to be removed!" penalty
 *     semantics — declared upstream behaviour, kept).</li>
 * </ul>
 *
 * <p>The fluid face (the :141 fill gate) re-forms as the fresh per-call IFluidHandler
 * wrapper (the LargeBoilerFluidHandler shape): fill = the gas-map containment, drain =
 * the three exhaust tanks in server-time rotation (:147).
 */
public class GTGasTurbineBlockEntity extends GTMultiBlockConverter {

	/** The input tank (upstream mInputTank, capacity mEnergyIN.mMax × 4 = 8×NBT_INPUT, :56). */
	public final FluidTankGT mInputTank = new FluidTankGT(1);
	/** The three exhaust tanks (upstream mTanksOutput, capacity mEnergyIN.mMax × 16 = 32×NBT_INPUT, :55). */
	public final FluidTankGT[] mTanksOutput = new FluidTankGT[] {new FluidTankGT(1), new FluidTankGT(1), new FluidTankGT(1)};
	/** The fuel map (upstream mRecipes = FM.Gas via NBT_FUELMAP, :47/:53 — the port pins the one map). */
	public Recipe mLastRecipe = null;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTGasTurbineBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the dual-constructor precedent). */
	public GTGasTurbineBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6Turbines.GAS_TURBINE_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GT6Turbines.GasTurbineBlock tBlock) {
			GT6Turbines.GasTurbineRow tRow = tBlock.row();
			applyRow(tRow.input(), tRow.output(), gregapi.data.TD.Energy.HU, gregapi.data.TD.Energy.RU, false, true);
		}
		rederiveTankCapacities();
	}

	/** The :55-/:56 capacity derivation — the input 4×, the exhausts 16× over mEnergyIN.mMax = 2×NBT_INPUT. */
	protected void rederiveTankCapacities() {
		mInputTank.setCapacity(mInput * 2 * 4);
		for (FluidTankGT tTank : mTanksOutput) tTank.setCapacity(mInput * 2 * 16);
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_gas_turbine"; // BET registry path mirrors it (GT6Turbines.GAS_TURBINE_BE)
	}

	/** The upstream NBT_TANK key family (:50-65): the input tank at NBT_TANK, the exhausts at NBT_TANK.i. */
	public static final String NBT_TANK = "gt.tank";

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_TANK, Tag.TAG_COMPOUND)) mInputTank.readFromNBT(aNBT, NBT_TANK);
		for (int i = 0; i < mTanksOutput.length; i++) {
			mTanksOutput[i].readFromNBT(aNBT, NBT_TANK + "." + i).setCapacity(mInput * 2 * 16); // :55
		}
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		mInputTank.writeToNBT(aNBT, NBT_TANK);
		for (int i = 0; i < mTanksOutput.length; i++) mTanksOutput[i].writeToNBT(aNBT, NBT_TANK + "." + i);
	}

	@Override
	protected net.minecraft.world.level.block.Block getWallBlock() {
		net.minecraft.world.level.block.state.BlockState tState = getBlockState();
		if (tState.getBlock() instanceof GT6Turbines.GasTurbineBlock tBlock) {
			return tBlock.wallBlock();
		}
		return GTMultiBlocks.anyPartBlock("dense_wall_stainless_steel"); // the offline/defensive default (row 1's wall)
	}

	@Override
	protected void doConversion(long aTimer) {
		if (mStorage >= mInput * 2) {
			// the clamp leg (:108-115): one full window converts this tick, the rest banks
			long tEnergy = mStorage;
			mStorage = mInput * 2;
			super.doConversion(aTimer);
			mStorage = tEnergy - mInput * 2;
			return;
		}
		if (!mStopped && mInputTank.has() && underHalfAll()) {
			Recipe tRecipe = findFuelRecipe();
			if (tRecipe != null && tRecipe.mEUt < 0 && tRecipe.mDuration > 0) {
				mLastRecipe = tRecipe;
				// the parallel ceiling (:121): the energy deficit over |EUt × duration|, ceil
				long tPower = -tRecipe.mEUt * tRecipe.mDuration;
				long tMax = (mInput * 2 - mStorage + tPower - 1) / tPower; // UT.Code.divup
				if (tMax > Integer.MAX_VALUE) tMax = Integer.MAX_VALUE; // UT.Code.bindInt
				long tPerUnit = tRecipe.mFluidInputs[0].getAmount();
				long tParallel = Math.min(tMax, mInputTank.amount() / tPerUnit);
				if (tParallel < tMax) mInputTank.setEmpty(); // :122 — the leftover scrap burns too
				else mInputTank.remove(tParallel * tPerUnit); // :121 — exactly tMax units
				if (tParallel > 0) {
					mStorage -= tParallel * tRecipe.mEUt * tRecipe.mDuration; // :124 (negative EUt credits)
					for (int i = 0; i < tRecipe.mFluidOutputs.length && i < mTanksOutput.length; i++) {
						FluidStack tOut = tRecipe.mFluidOutputs[i];
						long tWant = tOut.getAmount() * tParallel;
						if (mTanksOutput[i].add(tWant, tOut) < tWant) mStorage = 0; // :126-128 — no room, no credit
					}
					super.doConversion(aTimer); // :130
					return;
				}
			}
		}
		mStorage -= mInput * 2; // :136 — the idle drain (the exhaust-penalty semantics)
		if (mStorage < 0) mStorage = 0;
		super.doConversion(aTimer); // :138
	}

	/** The :116 under-half gate — all three exhaust tanks must be under half to keep burning. */
	private boolean underHalfAll() {
		for (FluidTankGT tTank : mTanksOutput) {
			if (tTank.amount() * 2 >= tTank.capacity()) return false;
		}
		return true;
	}

	/** The :117 lookup — the FM.Gas map over the input tank content (fluid-only rows, empty item leg). */
	@Nullable
	private Recipe findFuelRecipe() {
		if (GT6RecipeMaps.GAS_FUELS == null || !mInputTank.has()) return null;
		FluidStack tContent = mInputTank.get();
		if (tContent == null || tContent.isEmpty()) return null;
		return GT6RecipeMaps.GAS_FUELS.findRecipe(mLastRecipe, mInput * 2, null, new FluidStack[] {tContent});
	}

	// ---------------------------------------------------------------------------
	// the fluid face (:141-:152 — the fill gate + the rotating exhaust drain, the
	// LargeBoilerFluidHandler fresh-per-call wrapper shape)
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> aCapability, @Nullable net.minecraft.core.Direction aSide) {
		if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
			return net.minecraftforge.common.util.LazyOptional.of(GasFluidHandler::new).cast();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 removed BlockEntity#getCapability — the
	//GT6CapabilityWiring provider row delegates to this member; no @Override.
	//The FRESH per-call side wrapper form is kept — the TileEntityLargeBoiler
	//getCapability seam shape, the side accepted unused.)
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, net.minecraft.core.Direction> aCapability, @Nullable net.minecraft.core.Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
			return (T) new GasFluidHandler();
		}
		return null;
	}
	*///?}

	/** The :141-:152 tank view — fill = the gas-map containment gate + the stop gate; drain = the exhaust rotation (:147). */
	public class GasFluidHandler implements IFluidHandler {

		private FluidTankGT drainTank(@Nullable FluidStack aResource) {
			if (aResource == null) {
				// the rotation (:147): server-time/20 picks the starting tank
				int tStart = (int) ((getTimer() / 20) % mTanksOutput.length);
				for (int i = 0; i < mTanksOutput.length; i++) {
					FluidTankGT tTank = mTanksOutput[(tStart + i) % mTanksOutput.length];
					if (tTank.has()) return tTank;
				}
				return null;
			}
			for (FluidTankGT tTank : mTanksOutput) if (tTank.contains(aResource)) return tTank; // :149
			return null;
		}

		@Override
		public int getTanks() {
			return 1 + mTanksOutput.length;
		}

		@Override
		public FluidStack getFluidInTank(int aTank) {
			if (aTank == 0) return mInputTank.get();
			return mTanksOutput[aTank - 1].get();
		}

		@Override
		public int getTankCapacity(int aTank) {
			return FluidTankGT.bindInt(aTank == 0 ? mInputTank.capacity() : mTanksOutput[aTank - 1].capacity());
		}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {
			return aTank == 0 && !mStopped && findFuelRecipeFor(aStack) != null;
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty() || mStopped || findFuelRecipeFor(aResource) == null) return 0;
			if (mInputTank.has() && !mInputTank.contains(aResource)) return 0;
			if (aAction.simulate()) {
				return FluidTankGT.bindInt(mInputTank.has() ? mInputTank.capacity() - mInputTank.amount() : Math.min(mInputTank.capacity(), aResource.getAmount()));
			}
			return FluidTankGT.bindInt(mInputTank.add(aResource.getAmount(), aResource));
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			return aResource == null ? null : drain(aResource.getAmount(), aAction, aResource);
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			return drain(aMaxDrain, aAction, null);
		}

		private FluidStack drain(int aMaxDrain, FluidAction aAction, @Nullable FluidStack aResource) {
			FluidTankGT tTank = drainTank(aResource);
			if (tTank == null || !tTank.has() || aMaxDrain <= 0) return null;
			FluidStack tDrain = tTank.get(aMaxDrain);
			if (tDrain == null) return null;
			if (aAction.execute()) tTank.remove(tDrain.getAmount());
			return tDrain;
		}
	}

	/** The :141 containment probe — the fluid matches SOME gas row's input (the upstream mRecipes.containsInput re-form over the public mRecipeList). */
	@Nullable
	public static Recipe findFuelRecipeFor(@Nullable FluidStack aFluid) {
		if (GT6RecipeMaps.GAS_FUELS == null || aFluid == null || aFluid.isEmpty()) return null;
		for (Recipe tRecipe : GT6RecipeMaps.GAS_FUELS.mRecipeList) {
			for (FluidStack tInput : tRecipe.mFluidInputs) {
				if (tInput != null && !tInput.isEmpty() && tInput.isFluidEqual(aFluid)) return tRecipe;
			}
		}
		return null;
	}
}
