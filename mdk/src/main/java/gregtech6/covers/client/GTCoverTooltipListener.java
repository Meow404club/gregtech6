package gregtech6.covers.client;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.covers.CoverRegistry;
import gregtech6.covers.ICover;

/**
 * The cover hover dispatch (task tooltip-cover-face) — 1:1 port of the upstream anchor:
 * {@code ICover tCover = CoverRegistry.get(aEvent.itemStack); if (tCover != null)
 * tCover.addToolTips(aEvent.toolTip, aEvent.itemStack, aEvent.showAdvancedItemTooltips);}
 * (GT_API_Proxy_Client.onItemTooltip :285-286, the ItemTooltipEvent HIGHEST body).
 *
 * <p>Bus: the GAME bus — {@code ItemTooltipEvent} fires from
 * {@code ItemStack#getTooltipLines} (forge-1.20.1 ItemTooltipEvent.java:26; the 21.1
 * swap table lands the event in net.neoforged.neoforge.event.entity.player and drops the
 * bus clause — the NeoForge bus auto-detects by event type). Dist.CLIENT: hover is a
 * client-only face, and the dedicated server never loads this class (the
 * {@code GTCoverClientListener} card-local-listener convention).
 *
 * <p>This resolves the registered-cover plate too: {@code gt6:plate_iron} mounts
 * {@code CoverTextureSimple} (GT6Covers.init :516), so the plate's material rows
 * (MaterialPrefixItem.appendHoverText) are followed by the cover base row — upstream the
 * cover block (:285-286) runs BEFORE the material rows (:320-507), so the plate shows
 * the two row families in swapped order; single DGRAY row, cosmetic, the declared
 * deviation.
 */
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GTCoverTooltipListener {

	private GTCoverTooltipListener() {
	}

	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent aEvent) {
		appendCoverTooltips(aEvent.getItemStack(), aEvent.getToolTip(), aEvent.getFlags().isAdvanced());
	}

	/**
	 * The pure seam the event handler and the tests drive (the GT6MoldTintListener
	 * public-static-seam posture): the registry lookup + the behaviour rows.
	 * {@code aF3_H} is the F3+H advanced flag (TooltipFlag.isAdvanced =
	 * showAdvancedItemTooltips, the material-face pin).
	 */
	public static void appendCoverTooltips(ItemStack aStack, List<Component> aTooltip, boolean aF3_H) {
		ICover tCover = CoverRegistry.get(aStack); // upstream :285
		if (tCover != null) tCover.addToolTips(aTooltip, aStack, aF3_H); // upstream :286
	}
}
