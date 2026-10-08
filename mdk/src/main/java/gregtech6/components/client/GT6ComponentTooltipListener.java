package gregtech6.components.client;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregapi.data.TD;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterialStack;
import gregtech6.components.OM;
import gregtech6.item.GTMaterialPrefixBlockItem;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.tooltip.GT6TooltipStyle;

/**
 * The F3+H contained-materials face for every NON-self-describing item (task
 * component-tooltip-f3h-rows, MS-3 consumption face #2): the upstream anchor is the
 * F3+H arm of the global hover hook, {@code GT_API_Proxy_Client.onItemTooltip}
 * (ItemTooltipEvent HIGHEST, :452-479) — advanced mode prints one
 * {@code gt6.tooltip.material.contained_materials} header (DCYAN, :456) followed by one
 * {@link MaterialPrefixItem#containedMaterialRow component row per material} (the
 * :459-475 verbatim shape), non-advanced mode prints the DGRAY hint (:477-479). The whole
 * arm sits inside the {@code tData != null} (:323) and {@code tData.validMaterial()}
 * (:336) guards, so an item the central face cannot answer gains NOTHING — the empty
 * negative of the card.
 *
 * <p>Query: {@link OM#anydata} — the upstream hover body reads {@code OM.anydata_}
 * (:238) behind the {@code ST.invalid} early-out (:216), which is exactly the guarded
 * {@code OM.anydata} (OM.java:174-176). One data source, many consumption faces: the
 * central face map arms / family-tag arm / damage arm feed this display verbatim, so
 * machines, tanks and derived JSON products all grow the rows without per-item code.
 *
 * <p>Double-print guard (the port's declared seam): upstream prefix items do NOT
 * self-describe — the global hook prints every row for them. In the port,
 * {@link MaterialPrefixItem} and {@link GTMaterialPrefixBlockItem} already replay their
 * own material rows INCLUDING this F3+H face
 * ({@code MaterialPrefixItem.appendMaterialTooltip :272-283}, material-tooltip-face
 * 4f02ead8e), so the listener skips those two classes; every other item goes through.
 *
 * <p>Bus: the GAME bus, the {@code GTCoverTooltipListener} convention —
 * {@code ItemTooltipEvent} fires from {@code ItemStack#getTooltipLines} (forge-1.20.1
 * ItemTooltipEvent.java:26), Dist.CLIENT: hover is a client-only face and the dedicated
 * server never loads this class.
 */
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GT6ComponentTooltipListener {

	private GT6ComponentTooltipListener() {
	}

	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent aEvent) {
		appendComponentTooltips(aEvent.getItemStack(), aEvent.getToolTip(), aEvent.getFlags().isAdvanced());
	}

	/**
	 * The pure seam the event handler and the tests drive (the GTCoverTooltipListener
	 * public-static-seam posture). {@code aF3_H} is the F3+H advanced flag
	 * ({@code aEvent.showAdvancedItemTooltips}, :452).
	 */
	public static void appendComponentTooltips(ItemStack aStack, List<Component> aTooltip, boolean aF3_H) {
		// the double-print guard: the two self-describing material items own their rows.
		if (aStack.getItem() instanceof MaterialPrefixItem || aStack.getItem() instanceof GTMaterialPrefixBlockItem) return;
		OreDictItemData tData = OM.anydata(aStack); // upstream :238 (+ the :216 invalid early-out)
		if (tData == null || !tData.validMaterial()) return; // :323 + :336
		if (aF3_H) {
			boolean temp = true; // the lazy header — only before the FIRST qualifying material (:453-458)
			for (OreDictMaterialStack tMaterial : tData.getAllMaterialWeights()) {
				// :454 — zero-amount and DONT_SHOW_THIS_COMPONENT materials print nothing
				if (tMaterial.mAmount != 0 && !tMaterial.mMaterial.contains(TD.Properties.DONT_SHOW_THIS_COMPONENT)) {
					if (temp) {
						aTooltip.add(Component.translatable("gt6.tooltip.material.contained_materials").withStyle(GT6TooltipStyle.DCYAN)); // :456
						temp = false;
					}
					aTooltip.add(MaterialPrefixItem.containedMaterialRow(tMaterial)); // :459-475, the shared row builder
				}
			}
		} else {
			// :477-479 — the DGRAY hint is the non-advanced face of the SAME data gate.
			aTooltip.add(Component.translatable("gt6.tooltip.material.f3h_hint").withStyle(GT6TooltipStyle.DGRAY));
		}
	}
}
