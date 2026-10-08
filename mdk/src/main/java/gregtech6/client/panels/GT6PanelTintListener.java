package gregtech6.client.panels;

import javax.annotation.Nullable;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.block.panels.GT6PanelBlock;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GT6Panels;

/**
 * The client-side tint wiring of the dyed Cover Panel family (task
 * material-mc-g1-panels-dyed, the {@code GT6ConcreteTintListener} shape plus the Item
 * half — a BlockColor does NOT colour its BlockItem, the ItemColors.java:25-93 lesson the
 * GT6AttachmentTintListener javadoc records; card-local {@code @EventBusSubscriber},
 * Dist.CLIENT, no GTClientHandlers touch). The upstream render face verbatim: ONE
 * grayscale texture per family (Textures.BlockIcons CONCRETE/CFOAM_HARDENED/ASPHALT)
 * coloured by {@code DYES[mColor]} (MultiTileEntityPanelColored.java:33-36) — the port
 * keeps the grayscale PNG + tintindex-0 models and resolves the tint per BLOCK: every
 * per-row block carries its FIXED dye index, so the BlockColor is a constant lookup
 * ({@code GTSprayCanItem.DYES_INT[block.dyeIndex]}), tint index 0 only.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6PanelTintListener {

	private GT6PanelTintListener() {
	}

	/** The pure seam the tests drive: the opaque ARGB tint of one tint index over one block. */
	public static int panelTintARGB(@Nullable Block aBlock, int aTintIndex) {
		if (aTintIndex != 0) return -1;
		if (aBlock instanceof GT6PanelBlock tPanel) {
			return 0xFF000000 | GTSprayCanItem.DYES_INT[tPanel.dyeIndex & 15];
		}
		return -1;
	}

	/** The world-side half: registered over the 48 family blocks. */
	public static BlockColor panelBlockColor() {
		return (aState, aLevel, aPos, aTintIndex) -> panelTintARGB(aState.getBlock(), aTintIndex);
	}

	/** The inventory-side half: the same constant lookup over the items (the attachment listener form). */
	public static ItemColor panelItemColor() {
		return (aStack, aTintIndex) -> aStack.getItem() instanceof BlockItem tBlockItem
				? panelTintARGB(tBlockItem.getBlock(), aTintIndex) : -1;
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		BlockColor tColor = panelBlockColor();
		for (var tHandle : GT6Panels.BLOCKS) {
			aEvent.getBlockColors().register(tColor, tHandle.get());
		}
	}

	@SubscribeEvent
	public static void onRegisterItemColors(RegisterColorHandlersEvent.Item aEvent) {
		ItemColor tColor = panelItemColor();
		for (var tHandle : GT6Panels.ITEMS) {
			aEvent.getItemColors().register(tColor, tHandle.get());
		}
	}
}
