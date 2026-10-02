package gregtech6.client;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.block.surface.GT6WildBushBlock;
import gregtech6.registry.GT6SurfaceBlocks;

/**
 * The tint of the wild berry bush (task bushesgt-tint-color, widened by
 * bush-growth-blockstate, completed by berry-overlay). The upstream MTE 32759 "Berry Bush"
 * renders the grayscale {@code bush/colored/bush.png} body multiplied by the per-berry
 * BushesGT colour (MultiTileEntityBush.java:224/237
 * {@code BlockTextureDefault.get(sTextureBush, color)}) with the berry layer stacked on top
 * (:230-242). The berry carrier is the blockstate (task bush-growth-blockstate), so the
 * world face answers the PER-STATE arm over BOTH tint seats:
 * <ul>
 * <li>tintindex 0 — the kind's upstream BUSH BODY colour, {@code tBerryColor[0]} at every
 * stage (MultiTileEntityBush.java:236-240) — the nine-kind body table: blueberry
 * 0x22ff22 / candleberry 0x44ff44 / cranberry 0x00dd00 / the three currants 0x33ff33 /
 * blackberry + raspberry 0x11ff11 (MultiItemFood.java:397-429) and the cotton 0x22cc22
 * row (CS.java:1588/:1589);</li>
 * <li>tintindex 1 — the kind's STAGE colour at the state's AGE ({@code tBerryColor[1..3]},
 * :230-242, task berry-overlay) on the {@code bush_parts} berry sprites; the age-0 model
 * has no index-1 faces, the seat never queries it.</li>
 * </ul>
 * The tintindex-0 model seat and the grayscale borrow do not move.
 *
 * <p>The {@code GT6MoldTintListener} consumption shape, card-local subscriber (no
 * GTClientHandlers touch) over BOTH halves: the world face
 * ({@code RegisterColorHandlersEvent.Block} — per-state) and the inventory face
 * ({@code RegisterColorHandlersEvent.Item} — a BlockColor does NOT colour its BlockItem,
 * the ItemColors lesson the GTMachineTintModel kitchen card pinned; the kind-less item
 * stack answers the DEFAULT (cotton) row — body 0x22cc22 at index 0, and at index 1 the
 * berry layer the upstream item face ALWAYS shows at the stage-2 colour
 * (MultiTileEntityBush.java:233-234 {@code SIDES_ITEM_RENDER}, CS.java:1589
 * {@code DEFAULT[2]} = 0x44cc44)).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6BushTintListener {

	/** The upstream default bush colour, 0xFFRRGGBB (CS.java:1588 BushesGT.DEFAULT[0] = 0x22cc22 — the cotton kind). */
	public static final int BUSH_TINT_ARGB = GT6WildBushBlock.Kind.COTTON.bodyColorARGB();

	/**
	 * The item-face berry colour, 0xFFRRGGBB — the cotton stage-2 row (CS.java:1589
	 * {@code DEFAULT[2]} = 0x44cc44): the upstream item face always renders the berry layer
	 * at the stage-2 colour (MultiTileEntityBush.java:233-234 {@code SIDES_ITEM_RENDER}),
	 * so the inventory model (parented to {@code berry_bush_stage2}) tints its index-1
	 * berry faces with it.
	 */
	public static final int BERRY_ITEM_TINT_ARGB = GT6WildBushBlock.Kind.COTTON.stageColorARGB(2);

	private GT6BushTintListener() {
	}

	/**
	 * The inventory-face seam: tintindex 0 = the default (cotton) bush body, tintindex 1 =
	 * the always-on stage-2 berry layer of the upstream item face (MultiTileEntityBush.java:
	 * 233-234), anything else un-tinted.
	 */
	public static int bushTintARGB(int aTintIndex) {
		return switch (aTintIndex) {
			case 0 -> BUSH_TINT_ARGB;
			case 1 -> BERRY_ITEM_TINT_ARGB;
			default -> -1;
		};
	}

	/**
	 * The world-face seam, the per-state arm (the GTCFoamTintListener shape): tintindex 0 =
	 * the state's kind body colour (stage-constant, MultiTileEntityBush.java:236-240);
	 * tintindex 1 = the kind's stage colour at the state's AGE (:230-242); anything else
	 * un-tinted.
	 */
	public static int bushStateTintARGB(BlockState aState, int aTintIndex) {
		if (aTintIndex == 0) return aState.getValue(GT6WildBushBlock.KIND).bodyColorARGB();
		if (aTintIndex == 1) return aState.getValue(GT6WildBushBlock.KIND)
				.stageColorARGB(aState.getValue(GT6WildBushBlock.AGE));
		return -1;
	}

	/** The world-side half. */
	public static BlockColor bushBlockColor() {
		return (aState, aLevel, aPos, aTintIndex) -> bushStateTintARGB(aState, aTintIndex);
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
