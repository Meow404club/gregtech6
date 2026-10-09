package gregtech6.tileentity.energy;

import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
//? if forge {
import net.minecraftforge.common.util.LazyOptional;
//?}
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregapi.code.TagData;
import gregtech6.fluid.GTFluids;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.fluid.FluidTankGT;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GT6Kinetics;

/**
 * 1.20.1 counterpart of the GT6 Small Steam Turbine (task kinetics-be-function-family —
 * the mc-D 遗留 B 案面, the SST 蒸汽机反形) — ported from
 * gregtech/tileentity/energy/converters/MultiTileEntityTurbineSteam.java (:48-132) over
 * {@link GTMotorConverterBlockEntity} (the TileEntityBase11Motor port): the steam
 * engine's INVERSE — instead of burning steam into EU it burns steam into RU (rotation),
 * exhausting DISTILLED WATER (the 蒸馏水回吐, upstream :98-106 — every
 * {@code STEAM_PER_WATER = 200} SU of consumed steam yields 1 L of dist-w pushed into
 * the four perpendicular sides, the remainder voided).
 *
 * <p>The conversion (upstream doConversion :88-109): the two-tick storage cycle — one
 * tick consumes the WHOLE steam tank ({@code tSteam/STEAM_PER_EU} EU into the capacitor,
 * the same amount banked into {@code mEnergyProducedNextTick}), the next tick banks the
 * second half — then the plain converter law converts the capacitor into ONE RU burst
 * out the FRONT face ({@code tOutput = units(storage, inRec, outRec)}, the drain
 * booking {@code packets × tOutput} back out); the funnel waste leg
 * ({@code NBT_WASTE_ENERGY = T}, Loader :794-:811) vents the input max every tick.
 *
 * <p>Faces (upstream isInput/isOutput :129-130): the steam fill door is the BACK face
 * ({@code OPOS[mFacing]}), the RU emission the FRONT. The tank capacity is the input
 * max × 4 (readFromNBT2 :59 — {@code mEnergyIN.mMax*4}).
 *
 * <p>The 15 rows ride the mc-D data layer ({@link GT6Kinetics#STEAM_TURBINES}): Bronze
 * 48/16 (:794) through Graphene 6144/2048 (:811), the Kinetic_T body tiers.
 *
 * <p>Cropped with declaration: the texture half (the sColoreds/sOverlays icon bank +
 * the 4-way activity visual), the onWalkOver2 spin-kick (upstream :120 — the visual
 * pool), the plunger tool face (the tool-click pool), and the monkey-wrench direction
 * toggle (the Motor base pool declaration).
 */
public class GT6SteamTurbineBlockEntity extends GTMotorConverterBlockEntity {

	/** The upstream steam-per-distilled-water ratio (MultiTileEntityTurbineSteam :51 verbatim). */
	public static final int STEAM_PER_WATER = 200;

	/** The steam tank (upstream :49; capacity re-derived at :59 as the input max × 4 — the ctor rides the row). */
	public final FluidTankGT mTank = new FluidTankGT(GT6Kinetics.STEAM_TURBINES.get(0).inputEU() * 2 * 4);

	/** The banked second half (upstream {@code mEnergyProducedNextTick}, :50; NBT_OUTPUT_SU persisted). */
	public long mEnergyProducedNextTick = 0;

	/** The consumed-steam counter (upstream {@code mSteamCounter}, :50; NBT_ENERGY_SU persisted). */
	public long mSteamCounter = 0;

	/** The pending dist-water litres (the port-side carry — the upstream pushes inside the same tick). */
	public long mDistWaterPending = 0;

	// ---------------------------------------------------------------------------
	// the offline test seams (the engine BE's mSteamMatch/mByproductMake/mFluidAdjacency form)
	// ---------------------------------------------------------------------------

	/** The steam matcher (the engine BE seam; the plunger face is pool). */
	public Function<net.minecraft.world.level.material.Fluid, Boolean> mSteamMatch = f -> f == GTFluids.STEAM.source.get();

	/** The dist-w stack maker (the engine BE seam). */
	public Function<Integer, FluidStack> mByproductMake = amount -> new FluidStack(GTFluids.DISTILLED_WATER.source.get(), amount);

	/** The fluid adjacency (the engine BE seam form). */
	public Function<Byte, IFluidHandler> mFluidAdjacency = aSide -> {
		//? if forge {
		if (!hasLevel()) return null;
		BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
		if (tNeighbor == null || tNeighbor.isRemoved()) return null;
		return tNeighbor.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
				Direction.from3DDataValue(aSide).getOpposite()).resolve().orElse(null);
		//?} else {
		/*if (!hasLevel()) return null;
		return getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
				getBlockPos().relative(Direction.from3DDataValue(aSide)),
				Direction.from3DDataValue(aSide).getOpposite());*/
		//?}
	};

	/** BET factory (the BlockEntityType.Builder.of shape). */
	public GT6SteamTurbineBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** The offline (test) entry point — a null type falls back to the shared registry type. */
	public GT6SteamTurbineBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GTBlockEntities.STEAM_TURBINE_BE.get(), aPos, aState);
		mTank.setCapacity(inputSU() * 2 * 4); // upstream :59 — mEnergyIN.mMax * 4 (the row band, fresh blocks too)
	}

	@Override
	public String getTileEntityName() {
		return "small_steam_turbine"; // BET registry path mirrors it (GTBlockEntities.STEAM_TURBINE_BE)
	}

	// ---------------------------------------------------------------------------
	// the row adoption (the mc-D data layer — the mounted PowertrainBlock's row)
	// ---------------------------------------------------------------------------

	/** The row's NBT_INPUT in SU (the :794-:811 column — 48..6144, always a STEAM_PER_EU multiple). */
	public long inputSU() {
		if (getBlockState().getBlock() instanceof GT6Kinetics.PowertrainBlock tBlock
				&& tBlock.row() instanceof GT6Kinetics.SteamTurbineRow tRow) {
			return tRow.inputEU();
		}
		return GT6Kinetics.STEAM_TURBINES.get(0).inputEU(); // the Bronze stand-in
	}

	/** The row's NBT_OUTPUT in RU (the :794-:811 column — 16..2048). */
	public long outputRU() {
		if (getBlockState().getBlock() instanceof GT6Kinetics.PowertrainBlock tBlock
				&& tBlock.row() instanceof GT6Kinetics.SteamTurbineRow tRow) {
			return tRow.output();
		}
		return GT6Kinetics.STEAM_TURBINES.get(0).output();
	}

	// ---------------------------------------------------------------------------
	// the faces (MultiTileEntityTurbineSteam :129-130)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isInput(byte aSide) {
		return aSide == opposite(mFacing); // the steam door = the BACK
	}

	@Override
	public boolean isOutput(byte aSide) {
		return aSide == mFacing; // the RU emission = the FRONT
	}

	// ---------------------------------------------------------------------------
	// the bands (readEnergyBehavior :73-78 over the row — the STEAM/RU pair)
	// ---------------------------------------------------------------------------

	@Override
	public long inputSizeMin() {
		return inputSU() / 2; // 48 > 16 — the tInput/2 arm
	}

	@Override
	public long inputSizeRec() {
		return inputSU();
	}

	@Override
	public long inputSizeMax() {
		return inputSU() * 2;
	}

	@Override
	public long outputSizeMin() {
		return outputRU() / 2;
	}

	@Override
	public long outputSizeRec() {
		return outputRU();
	}

	@Override
	public long outputSizeMax() {
		return outputRU() * 2;
	}

	@Override
	public long storageCapacity() {
		return inputSU() * 2; // readEnergyBehavior :75
	}

	@Override
	public boolean wasteEnergy() {
		return true; // the row column NBT_WASTE_ENERGY = T, Loader :794-:811
	}

	@Override
	public TagData energyTypeIn() {
		return TD.Energy.STEAM; // NBT_ENERGY_ACCEPTED, Loader :794
	}

	@Override
	public TagData energyTypeOut() {
		return TD.Energy.RU; // NBT_ENERGY_EMITTED, Loader :794
	}

	// ---------------------------------------------------------------------------
	// the conversion (MultiTileEntityTurbineSteam.doConversion :88-109 + the plain
	// converter law the super chain runs — the transformer BE's size-carrying branch)
	// ---------------------------------------------------------------------------

	@Override
	protected void doConversion(long aTimer) {
		if (mEnergyProducedNextTick > 0) {
			// upstream :89-91 — the banked second half lands
			mStorage += mEnergyProducedNextTick;
			mEnergyProducedNextTick = 0;
		} else if (mTank.amount() >= inputSizeMin() * 2) {
			// upstream :92-97 — the whole-tank burn, split across this and the next tick
			long tSteam = mTank.amount();
			mSteamCounter += tSteam;
			long tEnergy = tSteam / GTFluids.STEAM_PER_EU;
			mStorage += tEnergy;
			mEnergyProducedNextTick = tEnergy;
			mTank.setEmpty();
			// upstream :98-106 — the 蒸馏水回吐: 1 L per STEAM_PER_WATER consumed
			mDistWaterPending += tSteam / STEAM_PER_WATER;
			mSteamCounter %= STEAM_PER_WATER;
		}
		if (mDistWaterPending > 0) pushByproduct();
		// the plain converter law (the doConversion :63-89 size-relevant branch, mult = 1):
		// ONE RU burst out the front, the drain booking packets × tOutput
		mActive = false;
		long tOutput = mStorage * outputSizeRec() / inputSizeRec(); // units(storage, inRec, outRec, F)
		if (tOutput >= outputSizeMin()) { // the converter :65 gate
			long tEmitted = ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.RU, tOutput, 1, this, adjacency());
			if (tEmitted > 0) {
				mStorage -= tEmitted * tOutput; // the converter :90-92 drain
				if (mStorage < 0) mStorage = 0;
				mActive = true;
				mLastOutSize = tOutput;
				mLastOutAmount = tEmitted;
			}
		}
		if (wasteEnergy()) mStorage = Math.max(0, mStorage - inputSizeMax()); // the converter :92 tail
	}

	/**
	 * The upstream :98-105 byproduct push — the pending dist-w litres into the four
	 * perpendicular sides (the FACING_SIDES ring), first handler with room wins per
	 * batch; the remainder voids (the :104 GarbageGT call).
	 */
	private void pushByproduct() {
		long tLeft = mDistWaterPending;
		byte[] tRing = facingSideRing(mFacing);
		for (byte tDir : tRing) {
			if (tLeft <= 0) break;
			IFluidHandler tTarget = mFluidAdjacency.apply(tDir);
			if (tTarget == null) continue;
			FluidStack tPush = mByproductMake.apply((int) Math.min(tLeft, Integer.MAX_VALUE));
			if (tPush == null || tPush.isEmpty()) {
				mDistWaterPending = 0;
				return;
			}
			int tFilled = tTarget.fill(tPush, FluidAction.EXECUTE);
			tLeft -= tFilled;
		}
		mDistWaterPending = Math.max(0, tLeft); // the remainder voids (upstream :104)
	}

	/** CS.java:555 FACING_SIDES — the four sides perpendicular to the facing axis (the GTSteamEngineBlockEntity form). */
	public static byte[] facingSideRing(byte aFacing) {
		return switch (aFacing) {
			case 0, 1 -> new byte[] {2, 3, 4, 5};
			case 2, 3 -> new byte[] {0, 1, 4, 5};
			case 4, 5 -> new byte[] {0, 1, 2, 3};
			default -> new byte[] {};
		};
	}

	// ---------------------------------------------------------------------------
	// the fluid half (upstream getFluidTankFillable2 :116-118 + the capability :239 gate)
	// ---------------------------------------------------------------------------

	/** The tank fill door — steam only, only while not stopped (upstream :116). */
	private boolean acceptsSteam(FluidStack aStack) {
		return !mStopped && aStack != null && !aStack.isEmpty() && mSteamMatch.apply(aStack.getFluid());
	}

	/** The fill-door gate (the getCapability side half; the offline-test seam). */
	boolean fluidDoorOpen(byte aSide) {
		return aSide == opposite(mFacing);
	}

	/**
	 * The /gt6engine fill channel (task kinetics-be-function-family — the direct-intake
	 * face the engine-steam chain canonized, ADR 2026-09-02-steam-proof-deviation): fills
	 * the steam tank through the back door, the readout naming the tank state. The
	 * REJECTED echo is the chain's poll-to-expect key.
	 */
	public String rconFill(int aAmount) {
		IFluidHandler tDoor = mFluidHandler;
		int tFilled = tDoor.fill(new FluidStack(GTFluids.STEAM.source.get(), aAmount), FluidAction.EXECUTE);
		return String.format("GT6 steam turbine fill at %s: filled %d/%d L of gt6:steam%s, tank holds %d L",
				(getBlockPos() == null ? "?" : getBlockPos().toShortString()), tFilled, aAmount,
				tFilled == 0 ? " (REJECTED)" : " (ACCEPTED)", mTank.amount());
	}

	/** The fluid handler (the GTSteamEngineBlockEntity.EngineFluidHandler form — fill-only). */
	final IFluidHandler mFluidHandler = new IFluidHandler() {
		@Override public int getTanks() {return mTank.AS_ARRAY.length;}
		@Override public FluidStack getFluidInTank(int aTank) {return aTank == 0 && mTank.get() != null ? mTank.get() : FluidStack.EMPTY;}
		@Override public int getTankCapacity(int aTank) {return aTank == 0 ? mTank.getCapacity() : 0;}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {return aTank == 0 && mTank.isFluidValid(aStack);}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (!acceptsSteam(aResource)) return 0;
			return mTank.fill(aResource, aAction);
		}

		@Override public FluidStack drain(FluidStack aResource, FluidAction aAction) {return FluidStack.EMPTY;} // upstream :117
		@Override public FluidStack drain(int aMaxDrain, FluidAction aAction) {return FluidStack.EMPTY;} // upstream :117
	};

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
			// the :239 side gate lives HERE — only the back face exposes the fill door
			if (aSide != null && !fluidDoorOpen((byte) aSide.get3DDataValue())) {
				return LazyOptional.empty();
			}
			return LazyOptional.of(() -> mFluidHandler).cast();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 removed BlockEntity#getCapability/LazyOptional — W4's
	// RegisterCapabilitiesEvent.registerBlockEntity delegates to this member; no @Override.)
	// (T) is the BlockCapability dispatch the caller types — T is not expressible
	// here (no Class token on BlockCapability); the platform-documented shape.
	@SuppressWarnings("unchecked")
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
			// the :239 side gate lives HERE — only the back face exposes the fill door
			if (aSide != null && !fluidDoorOpen((byte) aSide.get3DDataValue())) {
				return null; // the LazyOptional.empty() counterpart
			}
			return (T) mFluidHandler;
		}
		return null;
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :54-60 / writeToNBT2 :63-68)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong("gt.energy_su", mSteamCounter); // upstream NBT_ENERGY_SU :65
		aNBT.putLong("gt.output_su", mEnergyProducedNextTick); // upstream NBT_OUTPUT_SU :66
		aNBT.putLong("gt.dist_water", mDistWaterPending); // the port-side carry
		mTank.writeToNBT(aNBT, "gt.tank.0"); // upstream :67 (NBT_TANK.0)
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains("gt.energy_su", Tag.TAG_ANY_NUMERIC)) mSteamCounter = aNBT.getLong("gt.energy_su");
		if (aNBT.contains("gt.output_su", Tag.TAG_ANY_NUMERIC)) mEnergyProducedNextTick = aNBT.getLong("gt.output_su");
		if (aNBT.contains("gt.dist_water", Tag.TAG_ANY_NUMERIC)) mDistWaterPending = aNBT.getLong("gt.dist_water");
		mTank.readFromNBT(aNBT, "gt.tank.0"); // upstream :58
		mTank.setCapacity(inputSU() * 2 * 4); // upstream :59 — mEnergyIN.mMax * 4
	}
}
