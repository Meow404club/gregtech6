package gregtech6.client.decor;

import javax.annotation.Nullable;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.decor.GT6AsphaltBlock;
import gregtech6.block.decor.GT6BarsBlock;
import gregtech6.block.decor.GT6GlassBlock;
import gregtech6.block.decor.GT6SpikeBlock;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GT6DecorBlocks;
import gregtech6.registry.GT6Spikes;

/**
 * The client-side tint wiring of the decor-misc families (task material-mc-g2-decor-misc,
 * the GT6ConcreteTintListener shape — card-local {@code @EventBusSubscriber}, Dist.CLIENT,
 * no GTClientHandlers touch). Two colour faces, both the upstream multiply over grayscale:
 * <ul>
 * <li>the DYE face (Asphalt/Glass/GlowGlass — BlockColored.java:63-73 getRenderColor):
 *     ONE grayscale PNG ({@code UT.Code.fill(...)}, Textures.java:700-701) x
 *     {@code DYES_INT[fixed dye index]} (the {@code GTSprayCanItem.DYES_INT} table);</li>
 * <li>the MATERIAL face (Spikes/Bars — BlockBaseSpike.java:191 / BlockBaseBars.java:220
 *     {@code getTextureSmooth()}): the grayscale blockSolid PNG x the row material's
 *     {@code fRGBaSolid} (the GTBasicMachineBlock.materialColor composition, the
 *     UNCOLORED white fallback stays the {@code -1} no-tint sentinel).</li>
 * </ul>
 * The Path (vanilla-look textures) and the Bales (per-variant art, no tint upstream)
 * register no tint.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6DecorTintListener {

	private GT6DecorTintListener() {
	}

	/** The pure seam the tests drive: the opaque ARGB tint of one tint index over one block. */
	public static int tintARGB(@Nullable Block aBlock, int aTintIndex) {
		if (aTintIndex != 0) return -1;
		if (aBlock instanceof GT6AsphaltBlock tAsphalt) {
			return 0xFF000000 | GTSprayCanItem.DYES_INT[tAsphalt.dyeIndex & 15];
		}
		if (aBlock instanceof GT6GlassBlock tGlass) {
			return 0xFF000000 | GTSprayCanItem.DYES_INT[tGlass.dyeIndex & 15];
		}
		if (aBlock instanceof GT6BarsBlock tBars) {
			return materialTint(tBars.material);
		}
		if (aBlock instanceof GT6SpikeBlock tSpike) {
			return materialTint(tSpike.material);
		}
		return -1;
	}

	/** The fRGBaSolid composition (the getRGBInt face, GTBasicMachineBlock.materialColor). */
	private static int materialTint(gregapi.oredict.OreDictMaterial aMaterial) {
		return 0xFF000000 | gregtech6.block.GTBasicMachineBlock.materialColor(aMaterial);
	}

	/** The world-side half: registered over the dye families + the material families. */
	public static BlockColor blockColor() {
		return (aState, aLevel, aPos, aTintIndex) -> tintARGB(aState.getBlock(), aTintIndex);
	}

	/** The item-side half: the held stack's block answers the same seam. */
	public static ItemColor itemColor() {
		return (aStack, aTintIndex) -> tintARGB(aStack.getItem() instanceof BlockItem tItem
				? tItem.getBlock() : null, aTintIndex);
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		BlockColor tColor = blockColor();
		for (var tHandle : GT6DecorBlocks.ASPHALT_BLOCKS) aEvent.getBlockColors().register(tColor, tHandle.get());
		for (var tHandle : GT6DecorBlocks.GLASS_BLOCKS) aEvent.getBlockColors().register(tColor, tHandle.get());
		for (var tHandle : GT6DecorBlocks.GLOW_GLASS_BLOCKS) aEvent.getBlockColors().register(tColor, tHandle.get());
		for (var tHandle : GT6DecorBlocks.BARS_BLOCKS) aEvent.getBlockColors().register(tColor, tHandle.get());
		for (var tHandle : GT6Spikes.BLOCKS) aEvent.getBlockColors().register(tColor, tHandle.get());
	}

	@SubscribeEvent
	public static void onRegisterItemColors(RegisterColorHandlersEvent.Item aEvent) {
		ItemColor tColor = itemColor();
		for (RegistryObject<Item> tItem : GT6DecorBlocks.ITEMS) aEvent.getItemColors().register(tColor, tItem.get());
		for (var tItem : GT6Spikes.ITEMS) aEvent.getItemColors().register(tColor, tItem.get());
	}
}
