package gregtech6.block.pipe;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.GTComposedNameItem;
import gregtech6.tileentity.connectors.GTItemPipeBlockEntity;

/**
 * The item-pipe BlockItem — the onPlaced face-direction carrier (the
 * {@link GTFluidPipeBlockItem} shape) over the composed-name posture
 * ({@link GTComposedNameItem}: vanilla BlockItem has NO getName override, so the stack
 * name delegates to the block's composed row name — the boiler precedent, task
 * p20-i18n-compose-rows).
 *
 * <p>{@code placeBlock} is the only vanilla placement hook where the BE already exists
 * while the {@link BlockPlaceContext} is still in hand, so the clicked face survives to
 * {@link GTItemPipeBlockEntity#onPlaced(byte)}. NOT a BE.onLoad hook — onLoad replays on
 * every chunk load (the p4 ruling).
 */
public class GTItemPipeBlockItem extends GTComposedNameItem {

	public GTItemPipeBlockItem(gregtech6.block.pipe.GTItemPipeBlock aBlock, Properties aProperties) {
		super(aBlock, aProperties);
	}

	@Override
	public boolean placeBlock(BlockPlaceContext aContext, BlockState aState) {
		if (!super.placeBlock(aContext, aState)) return false;
		Level tLevel = aContext.getLevel();
		BlockEntity tBE = tLevel.getBlockEntity(aContext.getClickedPos());
		if (tBE instanceof GTItemPipeBlockEntity tPipe && !tLevel.isClientSide) {
			// upstream TileEntityBase09Connector.onPlaced :82-96 — the clicked-face support
			// connect (OPOS flip) + the symmetric back-connect loop (the pipe-connection family)
			tPipe.onPlaced((byte)aContext.getClickedFace().get3DDataValue());
		}
		return true;
	}
}
