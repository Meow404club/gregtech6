package gregtech6.block.multiblock;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6Crucibles;

/**
 * The LARGE-crucible wall part block (upstream "Steel Wall", Loader_MultiTileEntities.java
 * :1145 — part id 18009, the metalwall texture family, hardness == resistance 6.0; the
 * crucible's NBT_DESIGN :1270 wall reference becomes a Block identity, the p13 wall-variant
 * shape). The {@link GTMultiBlockPartBlock} body with the one difference that matters: the
 * block-entity factory mounts the {@link gregtech6.tileentity.multiblocks.CrucibleWallBlockEntity}
 * (the energy + crucible relaying part) over its OWN BET — the shared part BET cannot mount
 * it without a GTMultiBlocks.java touch this card does not own (the GTHeatTransmitterBlock
 * precedent verbatim). Everything else (onPlace/playerWillDestroy propagation, the
 * no-onRemove red line, RenderShape.MODEL) is inherited untouched.
 */
public class GTCrucibleWallBlock extends GTMultiBlockPartBlock {

	public GTCrucibleWallBlock(Properties aProperties) {
		super(aProperties);
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		return GT6Crucibles.CRUCIBLE_WALL_BE.get().create(aPos, aState);
	}
}
