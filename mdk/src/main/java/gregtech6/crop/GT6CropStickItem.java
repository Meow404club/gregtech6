package gregtech6.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6CropSticks;

/**
 * The crop stick item  --  the IC2 {@code ItemName.crop_stick} counterpart. Placing one on
 * farmland seeds the crop-stick block (the :414 arm's tile-side twin); the second-stick
 * crossing upgrade lives on the BLOCK's use face (TileEntityCrop.rightClick :414-422), so
 * this item only owns the place-first-stick arm.
 *
 * <p>Face census (cbc-6 汇核): KJS face — registration defer, the binding card declaration;
 * the recipe face is datapack-domain zero-adaptation. Viewer face — no machine diagram,
 * zero JEI/EMI surfaces. Jade face — another card, not this wave. RCON face — none.
 */
public class GT6CropStickItem extends Item {

	public GT6CropStickItem(Properties aProperties) {
		super(aProperties);
	}

	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		BlockState tTarget = tLevel.getBlockState(tPos);
		if (!tTarget.is(Blocks.FARMLAND)) return InteractionResult.PASS;
		BlockPos tAbove = tPos.above();
		if (!tLevel.getBlockState(tAbove).isAir()) return InteractionResult.PASS;
		if (!tLevel.isClientSide) {
			tLevel.setBlock(tAbove, GT6CropSticks.CROP_STICKS.get().defaultBlockState(), 3);
			ItemStack tStack = aContext.getItemInHand();
			if (!(aContext.getPlayer() != null && aContext.getPlayer().getAbilities().instabuild)) {
				tStack.shrink(1);
			}
		}
		return InteractionResult.sidedSuccess(tLevel.isClientSide);
	}
}
