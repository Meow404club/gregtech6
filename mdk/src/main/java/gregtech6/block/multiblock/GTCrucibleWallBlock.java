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

	/**
	 * The composed-name ctor (task p29-w3-distill-crucible ③ — the 8-material ladder): the
	 * "{@code <mat> Wall}" template over the EXISTING gt6.row.mat unit words (the card ①
	 * metal-wall composition — zero new lang unit keys; the composed carrier is the
	 * GTMultiBlockPartBlock :134 form, DESIGNS 0 → no DESIGN property).
	 */
	public GTCrucibleWallBlock(Properties aProperties, String aTemplateKey, String aUnitKey) {
		super(aProperties, 0, net.minecraft.network.chat.Component.translatable(aTemplateKey,
				net.minecraft.network.chat.Component.translatable(aUnitKey)));
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		return GT6Crucibles.CRUCIBLE_WALL_BE.get().create(aPos, aState);
	}
}
