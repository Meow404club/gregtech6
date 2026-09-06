package gregtech6.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * A BlockItem whose stack name DELEGATES to the block's (composed) name — the B-wave
 * composed-name face for the rows/stone families (task p20-i18n-compose-rows, the
 * GTWireBlockItem B1 posture): vanilla BlockItem has NO getName override — it delegates
 * only the descriptionId (BlockItem.java:186-189, the same-file evidence) — so a stack of
 * a block whose lang keys retired into compose templates would resolve the raw default id.
 * Delegating to {@code Block#getName()} lands every family on its single compose point
 * (the family block's own override); blocks that keep an atomic key (the coke-oven bricks,
 * the heat transmitter) resolve the same string through the vanilla default, so the item
 * swap is safe for them too.
 */
public class GTComposedNameItem extends BlockItem {

	public GTComposedNameItem(Block aBlock, Properties aProperties) {
		super(aBlock, aProperties);
	}

	@Override
	public Component getName(ItemStack aStack) {
		return this.getBlock().getName();
	}
}
