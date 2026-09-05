package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.registry.GTMultiBlocks;

/**
 * 1.20.1 port of the Coke Oven multiblock controller — direct translation of
 * gregtech/tileentity/multiblocks/MultiTileEntityCokeOven.java:44-103 (structure face:
 * task p4-multiblock-framework W3; machine face: task p6-cokeoven-processing).
 *
 * <p>checkStructure2 (:46-60): the 3x3x3 loop around the cell behind the facing
 * (getOffsetXN/YN/ZN arithmetic — "Main Block centered on Side and facing outwards"), the
 * loaded-chunk guard on the four corners (:48, upstream worldObj.blockExists →
 * {@code isLoaded}), the centre cell forced to the upstream getAir/setBlockToAir pair (:52 —
 * a non-air centre is a failure, NOT a silent clear), and
 * {@code checkAndSetTarget} for the other 26 cells (:54) with the upstream mode
 * {@link MultiBlockPartBlockEntity#ONLY_ITEM_FLUID_ENERGY} and design 0. Since
 * p16-pattern-checker the WALK is the shared checker over the bound pattern (task ③, the
 * production pilot) — the upstream semantics above are what the pattern declares, not a
 * second hand-written copy.
 *
 * <p>Port substitutions:
 * <ul>
 * <li>the part identity: upstream MTE (registry 18000-style id, registry-id pair) is the
 *     {@link #getPartBlock()} Block — the "coke oven bricks" part type as a Block instance
 *     (card note: ids keep the gt6:multiblock_* prefix, the card-ruled form);</li>
 * <li>extends {@link TileEntityBase10MultiBlockMachine} (the upstream parent class of the
 *     same name) since p6-cokeoven-processing: the TU self-generation, doWork/checkRecipe/
 *     parallel/ignition business and the fluid push live there — this class carries only
 *     the Coke Oven shape and its fluid-output scan;</li>
 * <li>isInsideStructure (:76-79) verbatim bounding box;</li>
 * <li>{@link #getStructurePattern()} (task p12-ghost-pattern-api, enriched by
 *     p16-pattern-checker): the shape above, now carrying the forming expectation —
 *     since the ADR 2026-09-05-p16-formation-scoping the pattern is the check
 *     ({@link #checkStructure2} walks it through the shared checker);</li>
 * <li>{@link #getFluidOutputTarget(Fluid)} (:84-96 verbatim shape): the cache-then-rescan
 *     fluid target one layer BELOW the structure (tY-2 relative to the facing offsets,
 *     :87) scanned as a 3x3 (:88); the upstream {@code WD.te(..., SIDE_TOP)} +
 *     {@code canFill(SIDE_TOP, fluid)} pair (:89-90) becomes the target capability queried
 *     at Direction.UP (the ADR ruling ① — SIDE_TOP 1:1) with a 1 mB SIMULATE fill probe
 *     (the fill-acceptance equivalent of canFill). The cache is the upstream :81 field —
 *     invalidated when the cell no longer exposes a handler (:85 exists()), and a failed
 *     scan caches nothing (upstream :95 caches null → the next call rescans anyway);</li>
 * <li>the item/fluid input+output target trio (:98-100, all null) folds away: the port cut
 *     the auto-IO surface with it (the machine base has no doInputItems/doOutputItems);</li>
 * <li>the tooltip LH block (:62-73) needs the LH stack — the structure description lives on
 *     the lang/datagen side.</li>
 * </ul>
 */
public class TileEntityCokeOven extends TileEntityBase10MultiBlockMachine {

	/** The cached output-target cell (upstream :81 mFluidOutputTarget; a cell, re-resolved per push). */
	@Nullable
	private BlockPos mFluidOutputTargetPos = null;

	/** The declared structure pattern (lazy — {@link #getPartBlock()} is stable per instance). */
	@Nullable
	private GTMultiBlockPattern mStructurePattern = null;

	/** The registry-path constructor (the BlockEntityType.Builder.of factory form, the oven precedent). */
	public TileEntityCokeOven(BlockPos aPos, BlockState aState) {
		this(GTMultiBlocks.COKE_OVEN_BE.get(), aPos, aState);
	}

	/** The test seam: offline fixtures build their own BET (the frozen registry keeps .get() out of reach). */
	public TileEntityCokeOven(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_coke_oven";
	}

	/**
	 * The part block this structure is built from (upstream MTE id 18000, CokeOven :54 — the
	 * registry pair becomes one Block). A hook so the offline tests can bind a fixture block
	 * without the frozen-registry dance.
	 */
	protected Block getPartBlock() {
		return GTMultiBlocks.COKE_OVEN_BRICKS.get();
	}

	/**
	 * The declared structure pattern ({@link GTMultiBlockPattern} binding, task
	 * p12-ghost-pattern-api + the p16-pattern-checker enrichment): the 26 brick cells in
	 * the upstream checkStructure2 loop order (:97-111, {@code i} outer / {@code j}
	 * middle / {@code k} inner — the loop order the checker no longer needs to rediscover)
	 * plus the hollow air centre appended (:52 — a non-air centre is a check failure, NOT
	 * a silent clear). Each brick cell carries the FULL forming expectation — part block
	 * {@link #getPartBlock()}, design 0, usage {@link MultiBlockPartBlockEntity#ONLY_ITEM_FLUID_ENERGY}
	 * — exactly the triple the hand-written loop passed checkAndSetTarget; since
	 * p16-pattern-checker the pattern IS the check ({@link #checkStructure2} walks it
	 * through the shared checker), ending the double bookkeeping. The binding is lazy,
	 * never derived by running the old hand-written loop.
	 */
	@Override
	@Nullable
	public GTMultiBlockPattern getStructurePattern() {
		if (mStructurePattern == null) {
			GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
			for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
				if (i == 0 && j == 0 && k == 0) continue; // the centre — declared hollow below
				tBuilder.formingPart(i, j, k, getPartBlock(), MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0);
			}
			tBuilder.hollow(0, 0, 0, GTMultiBlockPattern.AIR);
			mStructurePattern = tBuilder.build();
		}
		return mStructurePattern;
	}

	/**
	 * Upstream :46-60, now WALKED FROM THE PATTERN (task p16-pattern-checker ③ — the
	 * production pilot): the shared checker resolves each cell through
	 * {@link GTMultiBlockPattern#cellOffset} (the same getOffsetXN/YN/ZN anchor arithmetic
	 * this loop used to inline), drives the same checkAndSetTarget path for the 26 bricks
	 * and judges the centre fail-not-clear — one judgement source for the ghost preview
	 * and the server check. The unloaded guard keeps the upstream semantics: the checker's
	 * per-cell probe is the superset of the :48 four-corner form and an unloaded probe
	 * keeps the last verdict (:59 — {@code return mStructureOkay}), as does the no-level
	 * case (:133).
	 */
	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay; // :133
		GTMultiBlockStructureChecker.FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(
				this, mFacing, aCoordinates, aPlayer, aInventory);
		if (tVerdict.unloaded) return mStructureOkay; // :59 — unloaded cells keep the last verdict
		return tVerdict.formed;
	}

	/** Upstream :76-79 verbatim. */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		int tX = getOffsetXN(mFacing), tY = getOffsetYN(mFacing), tZ = getOffsetZN(mFacing);
		return aX >= tX - 1 && aY >= tY - 1 && aZ >= tZ - 1 && aX <= tX + 1 && aY <= tY + 1 && aZ <= tZ + 1;
	}

	// ---------------------------------------------------------------------------
	// the fluid output target (:81-96)
	// ---------------------------------------------------------------------------

	/** Upstream :84-96 — cache-then-scan, the layer BELOW the structure (tY-2), 3x3, UP-face acceptance. */
	@Override
	protected IFluidHandler getFluidOutputTarget(Fluid aOutput) {
		// :85 — the cache is valid while the cell still exposes a fluid handler
		if (mFluidOutputTargetPos != null) {
			IFluidHandler tCached = fluidHandlerAt(mFluidOutputTargetPos);
			if (tCached != null) return tCached;
			mFluidOutputTargetPos = null; // the target vanished → rescan (the upstream !exists() branch)
		}
		if (aOutput == null || !hasLevel() || isClientSide()) return null;
		int tX = getOffsetXN(mFacing), tY = getOffsetYN(mFacing) - 2, tZ = getOffsetZN(mFacing); // :87
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) { // :88
			BlockPos tPos = new BlockPos(tX + i, tY, tZ + j);
			IFluidHandler tHandler = fluidHandlerAt(tPos);
			if (tHandler != null && tHandler.fill(new FluidStack(aOutput, 1), FluidAction.SIMULATE) > 0) { // :90 canFill(SIDE_TOP)
				mFluidOutputTargetPos = tPos.immutable(); // :91
				return tHandler;
			}
		}
		return null; // :95 — nothing accepts; the failed scan caches nothing (upstream caches null, the next call rescans)
	}

	/** The capability resolve at the UP face (the upstream WD.te(..., SIDE_TOP) 1:1, ADR ruling ①). */
	@Nullable
	private IFluidHandler fluidHandlerAt(BlockPos aPos) {
		BlockEntity tNeighbor = getLevel().getBlockEntity(aPos);
		if (tNeighbor == null) return null;
		//? if forge {
		return tNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).resolve().orElse(null); // the CoverPump :103 idiom, the side pinned UP
		//?} else {
		/*// 21.1: BlockEntity carries no getCapability — the query goes through the level
		//(ILevelExtension.getCapability returns the handler directly, null when absent; the
		//TileEntityBase08Barrel:300 precedent).
		return getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, aPos, Direction.UP);
		*///?}
	}
}
