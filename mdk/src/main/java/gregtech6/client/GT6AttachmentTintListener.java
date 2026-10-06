package gregtech6.client;

import javax.annotation.Nullable;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.attachment.GTAttachmentSmallBlock;
import gregtech6.registry.GT6Attachments;

/**
 * The material tint of the 12 tap/funnel attachments (task tap-funnel-model-audit).
 * The family texture is the borrowed upstream GRAYSCALE {@code machines/tools/<tap|
 * funnel>/colored/side.png} — upstream renders it multiplied with the row's
 * {@code mRGBa} (MultiTileEntityFluidTap.java:204 {@code BlockTextureDefault.get(
 * sColoreds[..], mRGBa)}), which is what makes the six material rows visually
 * distinct. The models seat tintindex 0 on the colored elements (the overlay twins
 * stay untinted, the upstream multi layers only the colored pass); this listener
 * answers the colour over BOTH consumption halves (the {@link GT6MoldTintListener}
 * card-local subscriber shape, no GTClientHandlers touch):
 * <ul>
 * <li>the world face — {@code RegisterColorHandlersEvent.Block} over the 12
 *     attachment blocks;</li>
 * <li>the inventory face — {@code RegisterColorHandlersEvent.Item} over the same
 *     blocks' items (a BlockColor does NOT colour its BlockItem — the
 *     ItemColors.java:25-93 lesson).</li>
 * </ul>
 *
 * <p>The material rides {@link GT6Attachments#materialOf} (the upstream NBT_MATERIAL
 * column); unlike the mold family there is NO bodyTinted gate — the attachment art is
 * grayscale on every row. The faucet's synthetic rows are NOT registered here (the
 * {@link GT6MoldTintListener} FaucetBlock arm owns them).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6AttachmentTintListener {

	private GT6AttachmentTintListener() {
	}

	/**
	 * The pure seam the tests drive: the opaque ARGB tint for one row over one tint
	 * index (the {@code GT6MoldTintListener.materialTintARGB} mRGBaSolid pack form);
	 * {@code -1} = no tint.
	 */
	public static int rowTintARGB(@Nullable GT6Attachments.AttachmentRow aRow, int aTintIndex) {
		if (aTintIndex != 0 || aRow == null) return -1;
		OreDictMaterial tMaterial = GT6Attachments.materialOf(aRow);
		if (tMaterial == null) return -1;
		return 0xFF000000 | (tMaterial.mRGBaSolid[0] << 16) | (tMaterial.mRGBaSolid[1] << 8) | tMaterial.mRGBaSolid[2];
	}

	/** The block carrier resolution — unknown blocks stay un-tinted. */
	public static int blockTintARGB(@Nullable Block aBlock, int aTintIndex) {
		return aBlock instanceof GTAttachmentSmallBlock tAttachment ? rowTintARGB(tAttachment.row(), aTintIndex) : -1;
	}

	/** The family walk: exactly the 12 registered attachment rows. */
	private static Block[] familyBlocks() {
		Block[] rBlocks = new Block[GT6Attachments.BLOCKS_BY_PATH.size()];
		int i = 0;
		for (var tHandle : GT6Attachments.BLOCKS_BY_PATH.values()) rBlocks[i++] = tHandle.get();
		return rBlocks;
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		aEvent.getBlockColors().register((aState, aLevel, aPos, aTintIndex) ->
				blockTintARGB(aState.getBlock(), aTintIndex), familyBlocks());
	}

	@SubscribeEvent
	public static void onRegisterItemColors(RegisterColorHandlersEvent.Item aEvent) {
		ItemColor tColor = (aStack, aTintIndex) -> aStack.getItem() instanceof BlockItem tBlockItem
				? blockTintARGB(tBlockItem.getBlock(), aTintIndex) : -1;
		aEvent.getItemColors().register(tColor, familyBlocks());
	}
}
