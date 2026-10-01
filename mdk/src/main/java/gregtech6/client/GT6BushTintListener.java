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
 * bush-growth-blockstate). The upstream MTE 32759 "Berry Bush" renders the grayscale
 * {@code bush/colored/bush.png} body multiplied by the per-berry BushesGT colour
 * (MultiTileEntityBush.java:224/237 {@code BlockTextureDefault.get(sTextureBush, color)}).
 * The berry carrier is the blockstate now (task bush-growth-blockstate), so the world
 * face answers the PER-STATE arm: the kind's upstream BUSH BODY colour —
 * {@code tBerryColor[0]} at every stage (MultiTileEntityBush.java:236-240; the stage
 * colours [1..3] ride the CUT berry overlay textures, so AGE does not move the tint) —
 * the blueberry 0x22ff22 / candleberry 0x44ff44 / cranberry 0x00dd00 rows
 * (MultiItemFood.java:397/:405/:409) and the cotton 0x22cc22 row (CS.java:1588/:1589).
 * The tintindex-0 model seat and the grayscale borrow do not move.
 *
 * <p>The {@code GT6MoldTintListener} consumption shape, card-local subscriber (no
 * GTClientHandlers touch) over BOTH halves: the world face
 * ({@code RegisterColorHandlersEvent.Block} — per-state) and the inventory face
 * ({@code RegisterColorHandlersEvent.Item} — a BlockColor does NOT colour its BlockItem,
 * the ItemColors lesson the GTMachineTintModel kitchen card pinned; the kind-less item
 * stack answers the DEFAULT (cotton) bush colour, the default-state continuity).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6BushTintListener {

	/** The upstream default bush colour, 0xFFRRGGBB (CS.java:1588 BushesGT.DEFAULT[0] = 0x22cc22 — the cotton kind). */
	public static final int BUSH_TINT_ARGB = GT6WildBushBlock.Kind.COTTON.bodyColorARGB();

	private GT6BushTintListener() {
	}

	/** The inventory-face seam: tintindex 0 = the default (cotton) bush colour, anything else un-tinted. */
	public static int bushTintARGB(int aTintIndex) {
		return aTintIndex == 0 ? BUSH_TINT_ARGB : -1;
	}

	/**
	 * The world-face seam, the per-state arm (the GTCFoamTintListener shape): tintindex 0 =
	 * the state's kind body colour, AGE-blind (the upstream body colour is stage-constant,
	 * MultiTileEntityBush.java:236-240), anything else un-tinted.
	 */
	public static int bushStateTintARGB(BlockState aState, int aTintIndex) {
		return aTintIndex == 0 ? aState.getValue(GT6WildBushBlock.KIND).bodyColorARGB() : -1;
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
