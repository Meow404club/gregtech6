package gregtech6.block.pipe;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;

/**
 * The fluid-pipe BlockItem — the onPlaced face-direction carrier (task
 * p4-pipe-flow-control spec ②, the architect ruling): {@code placeBlock} is the only
 * vanilla placement hook where the BE already exists (super placed the block) while the
 * {@link BlockPlaceContext} is still in hand, so the clicked face survives to
 * {@link GTFluidPipeBlockEntity#onPlaced(byte, UUID)}. Deliberately NOT a BE.onLoad hook —
 * onLoad replays on every chunk load, which would resurrect connections the user tore
 * down by hand.
 *
 * <p>Task p24-pipe-owner adds the owner carrier: the placing player's UUID rides into
 * {@code onPlaced(byte, UUID)} (null when the context has no player), where the ownable
 * pipe records it (upstream TileEntityBase10ConnectorRendered:148-150) and the locked
 * support-side neighbour can deny the first connect (upstream 09Connector:86).
 */
public class GTFluidPipeBlockItem extends BlockItem {

	public GTFluidPipeBlockItem(Block aBlock, Properties aProperties) {
		super(aBlock, aProperties);
	}

	@Override
	public boolean placeBlock(BlockPlaceContext aContext, BlockState aState) {
		if (!super.placeBlock(aContext, aState)) return false;
		Level tLevel = aContext.getLevel();
		BlockEntity tBE = tLevel.getBlockEntity(aContext.getClickedPos());
		if (tBE instanceof GTFluidPipeBlockEntity tPipe && !tLevel.isClientSide) {
			tPipe.onPlaced((byte)aContext.getClickedFace().get3DDataValue(),
					aContext.getPlayer() != null ? aContext.getPlayer().getUUID() : null);
		}
		return true;
	}
}
