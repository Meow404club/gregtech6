package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregtech6.fluid.FluidTankGT;

/**
 * The side-aware drain-only {@link IFluidHandler} wrapper over a
 * {@link TileEntityBase10MultiBlockMachine}'s output tank (task p8-cokeoven-fluid-capability
 * spec ①, the {@link gregtech6.tileentity.tank.BarrelFluidHandler} shape: a fresh wrapper per
 * {@code getCapability(FLUID_HANDLER, Direction)} call — LazyOptional memoizes its supplier,
 * so a stored field would freeze the first-queried side into the stateless wrapper).
 *
 * <p>Upstream semantics (the 1.7.10 IFluidTank face of MultiTileEntityBasicMachine):
 * <ul>
 * <li><b>drain</b> routes through {@code getFluidTankDrainable2} (:574-582), gated by the
 *     output-face mask — the Coke Oven registers {@code NBT_TANK_SIDE_OUT = 61}
 *     (Loader_MultiTileEntities.java:1193), i.e. {@code 0b111101}: every machine-relative
 *     face except the relative top (CS.java:612 {@code SBIT_U = 2} = bit 1) may draw from
 *     the output tank;</li>
 * <li><b>fill</b> is refused for every face and fluid: the Coke Oven registers no
 *     {@code NBT_TANK_SIDE_IN} (Loader :1193), so the upstream
 *     {@code getFluidTankFillable2} input-mask leg (:566, {@code mFluidInputs = 0}) never
 *     connects — the machine has no input tank (output-only), both overloads return 0;</li>
 * <li>an executed drain marks the machine dirty exactly like the upstream
 *     {@code tapDrain} {@code updateInventory()} beat (:919) — {@code setChanged()} plus
 *     {@code mInventoryChanged = true}, which feeds the {@code doActive} recipe re-check
 *     window (:273 in the port, upstream :800).</li>
 * </ul>
 *
 * <p>Side rules (spec ②): the upstream verdict is the relative-coordinate mask
 * {@code FACE_CONNECTED[FACING_ROTATIONS[mFacing][aSide]][mask]} (CS.java:528-537 rotation,
 * CS.java:612 bit weights) — so the gate here is the same pure rotation + mask arithmetic,
 * never a bare world-face compare. The ported base class carries
 * {@code FACING = HORIZONTAL_FACING} (TileEntityBase10MultiBlockBase:61, no pitch), so the
 * relative top ≡ the world UP face for every horizontal facing and the constant verdict is
 * "all five non-UP faces drain, UP refuses" — but that verdict is DERIVED by the truth
 * table over {@link #drainAllowedBySide} (GT6MultiBlockFluidTest), not hardcoded. The
 * side-less query ({@code aSide == null}) is the all-open path for drain and refused for
 * fill (the P5 barrel precedent, GTBarrelCommand.java:106/:165).
 */
public class MultiBlockFluidHandler implements IFluidHandler {

	/** The Coke Oven output-face mask (Loader_MultiTileEntities.java:1193 {@code NBT_TANK_SIDE_OUT}): 0b111101 — every relative face but the top. */
	public static final int FLUID_TANK_SIDE_OUT = 61;

	/** No {@code NBT_TANK_SIDE_IN} registration (Loader :1193) → the input-face mask is 0 → the upstream :566 fill gate never connects. */
	public static final int FLUID_TANK_SIDE_IN = 0;

	/**
	 * CS.java:528-537 verbatim — {@code [Facing, Side] -> Side} mappings for blocks that
	 * do not face up/down; the row is the world facing (the GT6 side order, == the
	 * Direction 3D data order, TileEntityBase10MultiBlockBase:45) and the value the
	 * machine-relative side (0 bottom, 1 top, 2 left, 3 front, 4 right, 5 back). Rows
	 * 0/1 (a vertical facing, unreachable for this horizontal-facing base) keep the
	 * identity, every row maps bottom→bottom and top→top.
	 */
	private static final byte[][] FACING_ROTATIONS = {
		{0,1,2,3,4,5,6,6},
		{0,1,2,3,4,5,6,6},
		{0,1,3,5,4,2,6,6},
		{0,1,5,3,2,4,6,6},
		{0,1,2,4,3,5,6,6},
		{0,1,4,2,5,3,6,6},
		{0,1,2,3,4,5,6,6},
		{0,1,2,3,4,5,6,6}
	};

	private final TileEntityBase10MultiBlockMachine mMachine;
	/** The face this handler fronts (the {@code getCapability} query direction); null = the side-less query. */
	@Nullable
	private final Direction mSide;
	/** The machine facing captured at wrapper creation (the rotation anchor, spec ②). */
	private final byte mFacing;

	public MultiBlockFluidHandler(TileEntityBase10MultiBlockMachine aMachine, @Nullable Direction aSide) {
		mMachine = aMachine;
		mSide = aSide;
		mFacing = aMachine.mFacing;
	}

	// ---------------------------------------------------------------------------
	// the pure side-rule seams (spec ② — the offline truth table drives THESE)
	// ---------------------------------------------------------------------------

	/** The machine-relative side of a world side under {@code aFacing} (the FACING_ROTATIONS lookup, out-of-range sides fold to the 6/7 undefined row entries). */
	public static byte relativeSide(byte aFacing, byte aWorldSide) {
		return FACING_ROTATIONS[aFacing & 7][aWorldSide & 7];
	}

	/**
	 * The drain admission rule (spec ①/②): the side-less query is all-open (the P5 barrel
	 * ruling); otherwise the rotated output mask 61 — relative top refuses, the other five
	 * relative faces admit, for every horizontal facing.
	 */
	public static boolean drainAllowedBySide(byte aFacing, @Nullable Direction aSide) {
		if (aSide == null) return true;
		return ((FLUID_TANK_SIDE_OUT >> relativeSide(aFacing, (byte)aSide.get3DDataValue())) & 1) != 0;
	}

	/**
	 * The fill admission rule (spec ①): the rotated input mask 0 — the Coke Oven has no
	 * input tank, so no face (and the side-less probe, which has no upstream form and
	 * follows the machine's take-nothing contract) ever admits.
	 */
	public static boolean fillAllowedBySide(byte aFacing, @Nullable Direction aSide) {
		return aSide != null && ((FLUID_TANK_SIDE_IN >> relativeSide(aFacing, (byte)aSide.get3DDataValue())) & 1) != 0;
	}

	// ---------------------------------------------------------------------------
	// the IFluidHandler face (one tank: mTanksOutput[0])
	// ---------------------------------------------------------------------------

	@Override
	public int getTanks() {
		return 1; // the single output tank (mTanksOutput length, spec ①)
	}

	@Override
	public FluidStack getFluidInTank(int aTank) {
		if (aTank != 0) return FluidStack.EMPTY;
		FluidStack tFluid = mMachine.mTanksOutput[0].fluid(); // the content snapshot (null = empty)
		return tFluid == null ? FluidStack.EMPTY : tFluid;
	}

	/**
	 * The 1.20.1 tank face is an int contract — the upstream default tank capacity is
	 * Long.MAX_VALUE (FluidTankGT.java:49/:55), so the unbounded output tank reads as
	 * Integer.MAX_VALUE here ({@code UT.Code.bindInt}, the declared truncation, spec ①).
	 */
	@Override
	public int getTankCapacity(int aTank) {
		return aTank == 0 ? mMachine.mTanksOutput[0].getCapacity() : 0;
	}

	/** The take-nothing face: {@code fill} refuses every fluid, so no stack is ever valid to insert (the output-only contract). */
	@Override
	public boolean isFluidValid(int aTank, FluidStack aStack) {
		return false;
	}

	/** Upstream getFluidTankFillable2 :564-571 — {@code mFluidInputs = 0} refuses every face before any content check (spec ①). */
	@Override
	public int fill(FluidStack aResource, FluidAction aAction) {
		return 0;
	}

	/** The drain choke point (the {@code getFluidTankDrainable2} :574-582 face): side gate first, then the empty-tank refusal. */
	@Override
	public FluidStack drain(int aMaxDrain, FluidAction aAction) {
		if (aMaxDrain <= 0) return FluidStack.EMPTY;
		if (!drainAllowedBySide(mFacing, mSide)) return FluidStack.EMPTY; // the rotated 61 mask — the relative top refuses
		FluidTankGT tTank = mMachine.mTanksOutput[0];
		if (tTank.isEmpty()) return FluidStack.EMPTY;
		FluidStack rDrained = tTank.drain(aMaxDrain, aAction);
		if (aAction.execute() && !rDrained.isEmpty()) {
			mMachine.setChanged(); // the upstream tapDrain :919 updateInventory() beat
			mMachine.mInventoryChanged = true; // feeds the doActive re-check window (the ported :273 condition)
		}
		return rDrained;
	}

	/** The typed drain — same choke point, only landing when the tank holds exactly this fluid (the :579 {@code contains} leg). */
	@Override
	public FluidStack drain(FluidStack aResource, FluidAction aAction) {
		if (aResource == null || aResource.isEmpty() || !mMachine.mTanksOutput[0].contains(aResource)) return FluidStack.EMPTY;
		return drain(FluidTankGT.bindInt(Math.min(mMachine.mTanksOutput[0].amount(), aResource.getAmount())), aAction);
	}
}
