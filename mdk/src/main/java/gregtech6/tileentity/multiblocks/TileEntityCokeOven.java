package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GTMultiBlocks;

/**
 * 1.20.1 port of the Coke Oven multiblock controller — direct translation of
 * gregtech/tileentity/multiblocks/MultiTileEntityCokeOven.java:44-103 (task
 * p4-multiblock-framework W3, spec ④): the STRUCTURE face only.
 *
 * <p>checkStructure2 (:46-60) verbatim: the 3x3x3 loop around the cell behind the facing
 * (getOffsetXN/YN/ZN arithmetic — "Main Block centered on Side and facing outwards"), the
 * loaded-chunk guard on the four corners (:48, upstream worldObj.blockExists →
 * {@code isLoaded}), the centre cell forced to the upstream getAir/setBlockToAir pair (:52 —
 * a non-air centre is a failure, NOT a silent clear), and
 * {@code checkAndSetTarget} for the other 26 cells (:54) with the upstream mode
 * {@link MultiBlockPartBlockEntity#ONLY_ITEM_FLUID_ENERGY} and design 0.
 *
 * <p>Port substitutions:
 * <ul>
 * <li>the part identity: upstream MTE (registry 18000-style id, registry-id pair) is the
 *     {@link #getPartBlock()} Block — the "coke oven bricks" part type as a Block instance
 *     (card note: ids keep the gt6:multiblock_* prefix, the card-ruled form);</li>
 * <li>extends {@link TileEntityBase10MultiBlockBase} instead of TileEntityBase10MultiBlockMachine
 *     — the machine business face (RM.CokeOven recipes, tanks, energy) is a later card
 *     (spec ⑦: 加工业务不入卡, the recipe side is unsurveyed pool);</li>
 * <li>isInsideStructure (:76-79) verbatim bounding box;</li>
 * <li>the mFluidOutputTarget fluid-output scan (:81-96) and the delegate targets (:98-100)
 *     are processing business — out with the machine face;</li>
 * <li>the tooltip LH block (:62-73) needs the LH stack — the structure description lives on
 *     the lang/datagen side.</li>
 * </ul>
 */
public class TileEntityCokeOven extends TileEntityBase10MultiBlockBase {

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

	/** Upstream :46-60 verbatim. */
	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		int tX = getOffsetXN(mFacing), tY = getOffsetYN(mFacing), tZ = getOffsetZN(mFacing);
		if (!hasLevel()) return mStructureOkay;
		if (getLevel().isLoaded(new BlockPos(tX - 1, tY, tZ - 1)) && getLevel().isLoaded(new BlockPos(tX + 1, tY, tZ - 1))
				&& getLevel().isLoaded(new BlockPos(tX - 1, tY, tZ + 1)) && getLevel().isLoaded(new BlockPos(tX + 1, tY, tZ + 1))) {
			boolean tSuccess = true;
			for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
				if (i == 0 && j == 0 && k == 0) {
					// upstream :52 — the centre must ALREADY be air; a non-air centre fails the check
					if (getLevel().getBlockState(new BlockPos(tX + i, tY + j, tZ + k)).isAir()) {
						getLevel().removeBlock(new BlockPos(tX + i, tY + j, tZ + k), false); // setBlockToAir
					} else {
						tSuccess = false;
					}
				} else {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX + i, tY + j, tZ + k,
							getPartBlock(), 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) {
						tSuccess = false;
					}
				}
			}
			return tSuccess;
		}
		return mStructureOkay; // :59 — unloaded corners keep the last verdict
	}

	/** Upstream :76-79 verbatim. */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		int tX = getOffsetXN(mFacing), tY = getOffsetYN(mFacing), tZ = getOffsetZN(mFacing);
		return aX >= tX - 1 && aY >= tY - 1 && aZ >= tZ - 1 && aX <= tX + 1 && aY <= tY + 1 && aZ <= tZ + 1;
	}
}
