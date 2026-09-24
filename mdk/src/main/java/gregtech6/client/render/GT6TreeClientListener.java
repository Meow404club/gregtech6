package gregtech6.client.render;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.block.tree.GT6TreeKind;
import gregtech6.registry.GT6TreeBlocks;

/**
 * The client wiring of the Rainbowood leaves tint (task p38-issue1-4, GitHub #4): the
 * {@code rainbowood_leaves} model carries {@code tintindex 0} on every face over its
 * GRAYSCALE {@code leaves_rainbowood.png} and this listener paints it with the upstream
 * RAINBOW table (CS.java:330-355, all 24 colors ported verbatim — the datagen leaves the
 * other 8 kinds' pre-coloured PNGs untinted). The {@link GTSurfaceRockClientListener}
 * card-local {@code @EventBusSubscriber(Dist.CLIENT)} form.
 *
 * <p>World face = the position hash (BlockTreeLeavesAB.java:138 colorMultiplier:
 * {@code (|x|+|y|+|z|) % length}); inventory face = the CLIENT_TIME animation (:132
 * getRenderColor: {@code (CLIENT_TIME/10) % length} — the upstream counter ticks once per
 * client tick, GT_API_Proxy_Client.java:619, so one color step every 10 ticks; the modern
 * stand-in is the wall clock at the same 500 ms cadence, no global counter ported).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6TreeClientListener {

	/** CS.java:330-355 RAINBOW_ARRAY verbatim, upstream 0xRRGGBB widened to the ARGB modern tint face. */
	public static final int[] RAINBOW = {
			0xFFFF0000, 0xFFFF4000, 0xFFFF8000, 0xFFFFC000,
			0xFFFFFF00, 0xFFC0FF00, 0xFF80FF00, 0xFF40FF00,
			0xFF00FF00, 0xFF00FF40, 0xFF00FF80, 0xFF00FFC0,
			0xFF00FFFF, 0xFF00C0FF, 0xFF0080FF, 0xFF0040FF,
			0xFF0000FF, 0xFF4000FF, 0xFF8000FF, 0xFFC000FF,
			0xFFFF00FF, 0xFFFF00C0, 0xFFFF0080, 0xFFFF0040
	};

	private GT6TreeClientListener() {
	}

	/** The world tint face: tint index 0 -> the (|x|+|y|+|z|) % length table slot (BlockTreeLeavesAB.java:138). */
	public static BlockColor rainbowLeavesColor() {
		return (BlockState aState, BlockAndTintGetter aLevel, BlockPos aPos, int aTintIndex) -> {
			if (aTintIndex != 0 || aLevel == null || aPos == null) return -1;
			return RAINBOW[(Math.abs(aPos.getX()) + Math.abs(aPos.getY()) + Math.abs(aPos.getZ())) % RAINBOW.length];
		};
	}

	/** The inventory tint face: the CLIENT_TIME/10 step (BlockTreeLeavesAB.java:132), 500 ms per color, tint index 0 only. */
	public static ItemColor rainbowLeavesItemColor() {
		return (ItemStack aStack, int aTintIndex) -> {
			if (aTintIndex != 0) return -1;
			return RAINBOW[(int) ((System.currentTimeMillis() / 500) % RAINBOW.length)];
		};
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		aEvent.getBlockColors().register(rainbowLeavesColor(),
				GT6TreeBlocks.LEAVES.get(GT6TreeKind.RAINBOWOOD.ordinal()).get());
	}

	@SubscribeEvent
	public static void onRegisterItemColors(RegisterColorHandlersEvent.Item aEvent) {
		aEvent.getItemColors().register(rainbowLeavesItemColor(),
				GT6TreeBlocks.LEAF_ITEMS.get(GT6TreeKind.RAINBOWOOD.ordinal()).get());
	}
}
