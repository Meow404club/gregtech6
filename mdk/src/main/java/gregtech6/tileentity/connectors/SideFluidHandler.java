package gregtech6.tileentity.connectors;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.fluid.FluidTankGT;

/**
 * The side-aware {@link IFluidHandler} wrapper over a {@link GTFluidPipeBlockEntity}
 * (task p4-fluid-pipes spec ⑤): the 1.20.1 IFluidHandler carries no Direction parameter
 * (IFluidHandler.java:85/:96 — the 1.7.10 fill(ForgeDirection, ...) side argument is
 * gone), so the side travels through the {@code getCapability(FLUID_HANDLER, Direction)}
 * wrapper, the GTCEu IOFluidHandlerList precedent (FluidPipeBlockEntity.java:130-146
 * returns a per-side handler from getCapability).
 *
 * <p>This is the verbatim home of the upstream per-side fill/drain semantics
 * (MultiTileEntityPipeFluid.java:480-490/:471-475):
 * <ul>
 * <li>{@link #fill} routes into the fillable tank for the side
 *     (getFluidTankFillable2 :463-468: containing tank first, then an empty one) and
 *     records the source direction into {@code mLastReceivedFrom |= SBIT[side]} on
 *     executed fills (:487) — the anti-backflow marker the distribute loop consumes
 *     (:378);</li>
 * <li>{@link #drain} routes into the drainable tank for the side (:471-475: a containing
 *     tank only, gated by canEmitFluidsTo).</li>
 * </ul>
 */
public class SideFluidHandler implements IFluidHandler {

	private final GTFluidPipeBlockEntity mTile;
	/** The GT6 side index (0..5, Direction.get3DDataValue order) this handler fronts; -1 for the side-less query. */
	private final byte mSide;

	public SideFluidHandler(GTFluidPipeBlockEntity aTile, byte aSide) {
		mTile = aTile;
		mSide = aSide;
	}

	public SideFluidHandler(GTFluidPipeBlockEntity aTile, Direction aSide) {
		this(aTile, (byte)(aSide == null ? -1 : aSide.get3DDataValue()));
	}

	@Override
	public int getTanks() {
		return mTile.mTanks.length;
	}

	@Override
	public FluidStack getFluidInTank(int aTank) {
		if (aTank < 0 || aTank >= mTile.mTanks.length) return FluidStack.EMPTY;
		FluidStack tFluid = mTile.mTanks[aTank].get();
		return tFluid == null ? FluidStack.EMPTY : tFluid;
	}

	@Override
	public int getTankCapacity(int aTank) {
		return aTank < 0 || aTank >= mTile.mTanks.length ? 0 : mTile.mTanks[aTank].getCapacity();
	}

	@Override
	public boolean isFluidValid(int aTank, FluidStack aStack) {
		return aTank >= 0 && aTank < mTile.mTanks.length && mTile.mTanks[aTank].isFluidValid(aStack);
	}

	/** Upstream :480-490 — the fillable-tank lookup plus the source-direction record. */
	@Override
	public int fill(FluidStack aResource, FluidAction aAction) {
		if (aResource == null || aResource.isEmpty()) return 0;
		FluidTankGT tTank = mTile.getFluidTankFillable(mSide, aResource);
		if (tTank == null) return 0;
		int rFilled = tTank.fill(aResource, aAction);
		if (aAction.execute() && rFilled > 0) {
			mTile.onFilledFrom(mSide, tTank);
		}
		return rFilled;
	}

	/** Upstream :471-475 via the drainable-tank lookup. */
	@Override
	public FluidStack drain(FluidStack aResource, FluidAction aAction) {
		if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
		FluidTankGT tTank = mTile.getFluidTankDrainable(mSide, aResource);
		if (tTank == null) return FluidStack.EMPTY;
		return tTank.drain(FluidTankGT.bindInt(Math.min(tTank.amount(), aResource.getAmount())), aAction);
	}

	@Override
	public FluidStack drain(int aMaxDrain, FluidAction aAction) {
		if (aMaxDrain <= 0) return FluidStack.EMPTY;
		for (FluidTankGT tTank : mTile.mTanks) {
			if (tTank.has()) {
				return tTank.drain(FluidTankGT.bindInt(Math.min(tTank.amount(), aMaxDrain)), aAction);
			}
		}
		return FluidStack.EMPTY;
	}
}
