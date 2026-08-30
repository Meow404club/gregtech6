package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.fluid.FluidTankGT;

/**
 * The side-aware {@link IFluidHandler} wrapper over a {@link TileEntityBase08Barrel}
 * (task p4-fluid-barrel spec ①, "side 包装，W1 同型"): the 1.20.1 IFluidHandler carries
 * no Direction parameter, so the side travels through the
 * {@code getCapability(FLUID_HANDLER, Direction)} wrapper — the W1
 * SideFluidHandler shape (GTFluidPipeBlockEntity.java:297-303), typed to the barrel.
 *
 * <p>Upstream semantics (the 1.7.10 IFluidHandler face of TileEntityBase08Barrel):
 * fill/drain route into the single tank through the getFluidTankFillable2/
 * getFluidTankDrainable2 lookups (:292-294, mode-gated there — the sealed bit is a cut
 * pool item, so the tank is returned unconditionally). There is deliberately NO
 * fill-time fluid gate: upstream fills first and the tick judgment (:162) melts the
 * barrel down on the next pass. Executed changes mark the BE dirty and queue the
 * client sync (onTankChanged, the pipe onFilledFrom shape).
 */
public class BarrelFluidHandler implements IFluidHandler {

	private final TileEntityBase08Barrel mBarrel;
	/** The GT6 side index (0..5, Direction.get3DDataValue order) this handler fronts; -1 for the side-less query. */
	private final byte mSide;

	public BarrelFluidHandler(TileEntityBase08Barrel aBarrel, @Nullable Direction aSide) {
		mBarrel = aBarrel;
		mSide = (byte)(aSide == null ? -1 : aSide.get3DDataValue());
	}

	/** The GT6 side index this handler fronts (-1 = side-less). */
	public byte side() {
		return mSide;
	}

	@Override
	public int getTanks() {
		return mBarrel.mTank.AS_ARRAY.length;
	}

	@Override
	public FluidStack getFluidInTank(int aTank) {
		if (aTank != 0) return FluidStack.EMPTY;
		FluidStack tFluid = mBarrel.mTank.get();
		return tFluid == null ? FluidStack.EMPTY : tFluid;
	}

	@Override
	public int getTankCapacity(int aTank) {
		return aTank == 0 ? mBarrel.mTank.getCapacity() : 0;
	}

	@Override
	public boolean isFluidValid(int aTank, FluidStack aStack) {
		return aTank == 0 && mBarrel.mTank.isFluidValid(aStack);
	}

	/** Upstream getFluidTankFillable2 :292 → mTank.fill (the world-fill path, no fluid gate — see class doc). */
	@Override
	public int fill(FluidStack aResource, FluidAction aAction) {
		if (aResource == null || aResource.isEmpty()) return 0;
		int rFilled = mBarrel.mTank.fill(aResource, aAction);
		if (aAction.execute() && rFilled > 0) mBarrel.onTankChanged();
		return rFilled;
	}

	/** Upstream getFluidTankDrainable2 :293 → mTank.drain. */
	@Override
	public FluidStack drain(int aMaxDrain, FluidAction aAction) {
		if (aMaxDrain <= 0) return FluidStack.EMPTY;
		FluidStack rDrained = mBarrel.mTank.drain(aMaxDrain, aAction);
		if (aAction.execute() && !rDrained.isEmpty()) mBarrel.onTankChanged();
		return rDrained;
	}

	@Override
	public FluidStack drain(FluidStack aResource, FluidAction aAction) {
		if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
		return drain(FluidTankGT.bindInt(Math.min(mBarrel.mTank.amount(), aResource.getAmount())), aAction);
	}
}
