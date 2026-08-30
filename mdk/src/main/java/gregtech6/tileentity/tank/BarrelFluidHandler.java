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
 *
 * <p>Side rules (task p5-barrel-side-rules spec ①, the user feature — upstream :292-293
 * is face-blind): fill is open through every face (only the cover intercept may refuse,
 * {@link #interceptFluidFill}); {@link #drain} is the single choke point — the cover
 * intercept first, then the admission rule {@link #drainAllowedBySide(byte, FluidStack)}:
 * the side-less query (-1) stays the all-open path (the /gt6tank accept chain depends on
 * it), the bottom face (0) drains only fluids heavier than air, the top face (1) only
 * fluids lighter than air, and the four sides (2-5) take in but never give. The verdict
 * is the strict GT6 density sign (FL.java:775/:780 — {@code < 0} lighter, {@code > 0}
 * heavier; the Forge javadoc's "negative or zero" reading is NOT taken, density 0
 * passes neither face).
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

	/** Upstream getFluidTankFillable2 :292 → mTank.fill — fill is face-open, only a cover on the face may refuse (p5 spec ①). */
	@Override
	public int fill(FluidStack aResource, FluidAction aAction) {
		if (aResource == null || aResource.isEmpty()) return 0;
		if (mBarrel.interceptFluidFill(mSide, aResource)) return 0; // the cover one-way gate (CoverPump out-face), upstream 04Covers :370
		int rFilled = mBarrel.mTank.fill(aResource, aAction);
		if (aAction.execute() && rFilled > 0) mBarrel.onTankChanged();
		return rFilled;
	}

	/** Upstream getFluidTankDrainable2 :293 → mTank.drain — THE drain choke point (drain(FluidStack) delegates here, p5 spec ①). */
	@Override
	public FluidStack drain(int aMaxDrain, FluidAction aAction) {
		if (aMaxDrain <= 0) return FluidStack.EMPTY;
		FluidStack tContent = mBarrel.mTank.getFluid();
		if (mBarrel.interceptFluidDrain(mSide, tContent)) return FluidStack.EMPTY; // cover gate FIRST (spec A), upstream 04Covers :374+
		if (!drainAllowedBySide(mSide, tContent)) return FluidStack.EMPTY; // then the side rule
		FluidStack rDrained = mBarrel.mTank.drain(aMaxDrain, aAction);
		if (aAction.execute() && !rDrained.isEmpty()) mBarrel.onTankChanged();
		return rDrained;
	}

	@Override
	public FluidStack drain(FluidStack aResource, FluidAction aAction) {
		if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
		return drain(FluidTankGT.bindInt(Math.min(mBarrel.mTank.amount(), aResource.getAmount())), aAction);
	}

	// ---------------------------------------------------------------------------
	// the p5 side-rule verdicts (spec ①, offline-testable sign seams)
	// ---------------------------------------------------------------------------

	/**
	 * The drain admission rule (p5 spec ①): {@code -1} all-open (the side-less query —
	 * GTBarrelCommand.java:106/:165 null-side capability, the /gt6tank accept chain);
	 * bottom (0) drains only heavier fluids; top (1) drains only lighter fluids; the four
	 * sides (2-5) refuse every drain. An empty content refuses on the gated faces too.
	 */
	public static boolean drainAllowedBySide(byte aSide, @Nullable FluidStack aFluid) {
		if (aSide < 0) return true; // ruling ⑥: the side-less path never consults the content
		return drainAllowedBySide(aSide, fluidDensitySign(aFluid));
	}

	/** The raw sign form of the rule — the offline seam over the FluidType density lookup. */
	public static boolean drainAllowedBySide(byte aSide, int aDensitySign) {
		if (aSide < 0) return true; // ruling ⑥: the null-side path is all-open
		if (aSide >= 2) return false; // the sides take in but never give
		// strict GT6 verdict (FL.java:775/:780): > 0 heavier drains the bottom, < 0 lighter
		// drains the top, 0 passes neither (the user's "heavier than air" is a strict >)
		return aSide == 0 ? aDensitySign > 0 : aDensitySign < 0;
	}

	/**
	 * The GT6 density verdict (FL.java:775/:780): {@code -1} lighter than air (density
	 * {@code < 0}), {@code +1} heavier (density {@code > 0}), {@code 0} neither (density
	 * exactly 0 or no content). The 1.20.1 density home is the FluidType
	 * (FluidType.java:176-179) — the live-registry lookup that offline tests route around
	 * through the {@code (byte, int)} overload.
	 */
	public static int fluidDensitySign(@Nullable FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return 0;
		return aFluid.getFluid().getFluidType().getDensity() < 0 ? -1 : aFluid.getFluid().getFluidType().getDensity() > 0 ? +1 : 0;
	}
}
