package gregtech6.block.wire;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.item.GT6MachineBlockItem;
import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * The electric-wire BlockItem — the onPlaced face-direction carrier, the GTFluidPipeBlockItem
 * twin (task pipe-flow-control spec ② ruling): {@code placeBlock} is the only vanilla
 * placement hook where the BE already exists (super placed the block) while the
 * {@link BlockPlaceContext} is still in hand, so the clicked face survives to
 * {@link GTWireBlockEntity#onPlaced(byte)} (the upstream TileEntityBase09Connector.onPlaced
 * :82-96 support-face connect + symmetric back-connect). Deliberately NOT a BE.onLoad hook —
 * onLoad replays on every chunk load, which would resurrect connections the user tore down.
 *
 * <p>Task tooltip-wire-pipe-sensor moves the parent to the machine carrier
 * {@link GT6MachineBlockItem} (itself a {@code GTComposedNameItem}) so the family row tables
 * replay off the registration-site family key: {@code wire} / the {@code wire_contact}
 * sibling (the bare shock-flagged rows), {@code wire_redstone} / {@code wire_laser}
 * (unregistered keys — the zero-row T1 contract — the upstream WireElectric stat rows must
 * not leak onto the redstone/laser families). The stack name keeps the B-wave p20 posture:
 * the inherited {@code GTComposedNameItem.getName} delegates to
 * {@link GTWireBlock#getName()}, the same compose the retired local override made (both
 * hooks land on the block face, the p20 arch card's "single compose point").
 */
public class GTWireBlockItem extends GT6MachineBlockItem {

	public GTWireBlockItem(Block aBlock, Item.Properties aProperties, String aFamily, Object... aLineArgs) {
		super(aBlock, aProperties, aFamily, aLineArgs);
	}

	@Override
	public boolean placeBlock(BlockPlaceContext aContext, BlockState aState) {
		if (!super.placeBlock(aContext, aState)) return false;
		Level tLevel = aContext.getLevel();
		BlockEntity tBE = tLevel.getBlockEntity(aContext.getClickedPos());
		if (tBE instanceof GTWireBlockEntity tWire && !tLevel.isClientSide) {
			tWire.onPlaced((byte)aContext.getClickedFace().get3DDataValue());
		}
		return true;
	}
}
