package gregtech6.client;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.registry.GT6SurfaceBlocks;

/**
 * The tint of the wild berry bush (task bushesgt-tint-color — the food-crop-items
 * leftover ④). The upstream MTE 32759 "Berry Bush" renders the grayscale
 * {@code bush/colored/bush.png} body multiplied by the per-berry BushesGT colour
 * (MultiTileEntityBush.java:224/237 {@code BlockTextureDefault.get(sTextureBush, color)});
 * the port's berry carrier is CUT (the w6-t2 declared deviation — the static block stands
 * for the no-berry-carrier default), so this listener answers the upstream
 * <em>default</em> bush colour {@code 0x22cc22} — the {@code BushesGT.DEFAULT[0]} row
 * (CS.java:1588) that the {@code Items.string} cotton seed (:1589) and every
 * unregistered berry resolve to. When a future card lands the berry-state carrier, the
 * dispatch widens here (the {@code GTCFoamTintListener} per-state arm shape); the
 * tintindex-0 model seat and the grayscale borrow do not move.
 *
 * <p>The {@code GT6MoldTintListener} consumption shape, card-local subscriber (no
 * GTClientHandlers touch) over BOTH halves: the world face
 * ({@code RegisterColorHandlersEvent.Block}) and the inventory face
 * ({@code RegisterColorHandlersEvent.Item} — a BlockColor does NOT colour its BlockItem,
 * the ItemColors lesson the GTMachineTintModel kitchen card pinned).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6BushTintListener {

	/** The upstream default bush colour, 0xFFRRGGBB (CS.java:1588 BushesGT.DEFAULT[0] = 0x22cc22). */
	public static final int BUSH_TINT_ARGB = 0xFF22CC22;

	private GT6BushTintListener() {
	}

	/** The pure seam the tests drive: tintindex 0 = the default bush colour, anything else un-tinted. */
	public static int bushTintARGB(int aTintIndex) {
		return aTintIndex == 0 ? BUSH_TINT_ARGB : -1;
	}

	/** The world-side half. */
	public static BlockColor bushBlockColor() {
		return (aState, aLevel, aPos, aTintIndex) -> bushTintARGB(aTintIndex);
	}

	/** The inventory-side half. */
	public static ItemColor bushItemColor() {
		return (aStack, aTintIndex) -> bushTintARGB(aTintIndex);
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		aEvent.getBlockColors().register(bushBlockColor(), GT6SurfaceBlocks.BERRY_BUSH.get());
	}

	@SubscribeEvent
	public static void onRegisterItemColors(RegisterColorHandlersEvent.Item aEvent) {
		aEvent.getItemColors().register(bushItemColor(), GT6SurfaceBlocks.BERRY_BUSH.get().asItem());
	}
}
