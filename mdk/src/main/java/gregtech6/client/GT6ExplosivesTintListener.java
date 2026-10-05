package gregtech6.client;

import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.registry.GT6Explosives;

/**
 * The per-item material tint of the dynamite family (task explosives-chain): the borrowed
 * greyscale "colored" block sprite (layer 0) renders multiplied with the row material's
 * {@code mRGBaSolid} — the upstream MTE material-tint pass over the same sprite
 * (Loader_MultiTileEntities.java:2236-2238, MT.Orange/Red/Purple). The overlay layer (the
 * "overlay" sprite, tint index 1) renders as-is. The
 * {@link GT6MoldTintListener#onRegisterItemColors} card-local subscriber form: no
 * GTClientHandlers touch, items only (plain items, no block half).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6ExplosivesTintListener {

	private GT6ExplosivesTintListener() {
	}

	/** The shared ItemColor over the whole family — the seam routes to {@link GT6Explosives#tintARGB}. */
	public static net.minecraft.client.color.item.ItemColor familyItemColor() {
		return (ItemStack aStack, int aTintIndex) -> GT6Explosives.tintARGB(aStack.getItem(), aTintIndex);
	}

	@SubscribeEvent
	public static void onRegisterItemColors(RegisterColorHandlersEvent.Item aEvent) {
		net.minecraft.world.item.Item[] tItems = new net.minecraft.world.item.Item[GT6Explosives.ITEMS_BY_PATH.size()];
		int i = 0;
		for (var tHandle : GT6Explosives.ITEMS_BY_PATH.values()) tItems[i++] = tHandle.get();
		aEvent.getItemColors().register(familyItemColor(), tItems);
	}
}
