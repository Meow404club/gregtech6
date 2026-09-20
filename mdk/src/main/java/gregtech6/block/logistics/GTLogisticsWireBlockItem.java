package gregtech6.block.logistics;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.tileentity.connectors.GTLogisticsWireBlockEntity;

/**
 * The logistics-wire BlockItem — the onPlaced face-direction carrier (the
 * {@link gregtech6.block.pipe.GTItemPipeBlockItem} shape over a plain BlockItem: the
 * single-row family needs no composed-name posture — the vanilla stack name resolves
 * the block translation key "block.gt6.logistics_wire").
 *
 * <p>{@code placeBlock} is the only vanilla placement hook where the BE already exists
 * while the {@link BlockPlaceContext} is still in hand, so the clicked face survives to
 * {@link GTLogisticsWireBlockEntity#onPlaced(byte)}. NOT a BE.onLoad hook — onLoad
 * replays on every chunk load (the p4 ruling).
 */
public class GTLogisticsWireBlockItem extends BlockItem {

	public GTLogisticsWireBlockItem(GTLogisticsWireBlock aBlock, Properties aProperties) {
		super(aBlock, aProperties);
	}

	@Override
	public boolean placeBlock(BlockPlaceContext aContext, BlockState aState) {
		if (!super.placeBlock(aContext, aState)) return false;
		Level tLevel = aContext.getLevel();
		BlockEntity tBE = tLevel.getBlockEntity(aContext.getClickedPos());
		if (tBE instanceof GTLogisticsWireBlockEntity tWire && !tLevel.isClientSide) {
			// upstream TileEntityBase09Connector.onPlaced :82-96 — the clicked-face support
			// connect (OPOS flip) + the symmetric back-connect loop (the connector family)
			tWire.onPlaced((byte)aContext.getClickedFace().get3DDataValue());
		}
		return true;
	}
}
