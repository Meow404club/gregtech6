package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.data.TD;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.registry.GT6DynamoHousings;

/**
 * The Large Dynamo controller (task p29-w3-turbine-dynamo ④) — the 1.20.1 counterpart of
 * MultiTileEntityLargeDynamo (MultiTileEntityLargeDynamo.java:43-110) over the
 * {@link GTMultiBlockConverter} base: RU packets in at the FRONT, EU out at the far plate,
 * 4096→3072 / 8192→6144 / 16384→12288 / 131072→98304 (exactly 75%, Loader :1259-1262),
 * WASTE_ENERGY = T. The structure is the base's shell with the two middle layers as the
 * 18 Large Copper Coils (18040, LargeDynamo.java:68) and the far plate at design 2.
 */
public class GTLargeDynamoBlockEntity extends GTMultiBlockConverter {

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTLargeDynamoBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the dual-constructor precedent). */
	public GTLargeDynamoBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6DynamoHousings.DYNAMO_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GT6DynamoHousings.DynamoBlock tBlock) {
			GT6DynamoHousings.DynamoRow tRow = tBlock.row();
			applyRow(tRow.input(), tRow.output(), TD.Energy.RU, TD.Energy.EU, true, false);
		}
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_large_dynamo"; // BET registry path mirrors it (GT6DynamoHousings.DYNAMO_BE)
	}

	@Override
	protected net.minecraft.world.level.block.Block getWallBlock() {
		net.minecraft.world.level.block.state.BlockState tState = getBlockState();
		if (tState.getBlock() instanceof GT6DynamoHousings.DynamoBlock tBlock) {
			return tBlock.wallBlock();
		}
		return GTMultiBlocks.anyPartBlock("dense_wall_stainless_steel"); // the offline/defensive default (row 1's wall)
	}

	@Override
	protected boolean coilSegment() {
		return true; // LargeDynamo.java:68 — the two middle layers are the 18 Large Copper Coils
	}

	@Override
	protected int farPlateDesign() {
		return 2; // LargeDynamo.java:68 — the out cell's aDesign
	}
}
