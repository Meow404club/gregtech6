package gregtech6.block.wire;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * The electric-wire BlockItem — the onPlaced face-direction carrier, the GTFluidPipeBlockItem
 * twin (task p4-pipe-flow-control spec ② ruling): {@code placeBlock} is the only vanilla
 * placement hook where the BE already exists (super placed the block) while the
 * {@link BlockPlaceContext} is still in hand, so the clicked face survives to
 * {@link GTWireBlockEntity#onPlaced(byte)} (the upstream TileEntityBase09Connector.onPlaced
 * :82-96 support-face connect + symmetric back-connect). Deliberately NOT a BE.onLoad hook —
 * onLoad replays on every chunk load, which would resurrect connections the user tore down.
 */
public class GTWireBlockItem extends BlockItem {

	public GTWireBlockItem(Block aBlock, Properties aProperties) {
		super(aBlock, aProperties);
	}

	/**
	 * The item face of the composed wire name (task p20-i18n-compose-wires): vanilla
	 * BlockItem has NO getName override — it delegates only the descriptionId
	 * (BlockItem.java:186-189) — so the stack display would resolve the raw per-variant
	 * key the B-wave retired. Delegate to the block compose instead: the arch card's
	 * "single compose point" splits across the two vanilla name hooks (Block.getName for
	 * the block face, Item.getName(ItemStack) for the stack face), both landing on
	 * {@link GTWireBlock#displayNameOf}.
	 */
	@Override
	public Component getName(ItemStack aStack) {
		return getBlock() instanceof GTWireBlock tWire ? tWire.getName() : super.getName(aStack);
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
