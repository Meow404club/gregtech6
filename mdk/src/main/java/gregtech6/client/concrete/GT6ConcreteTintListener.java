package gregtech6.client.concrete;

import javax.annotation.Nullable;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.block.concrete.GT6ConcreteBlock;
import gregtech6.block.concrete.GT6ConcreteSlabBlock;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GT6ConcreteBlocks;

/**
 * The client-side BlockColor wiring of the concrete family (task concrete-blocks-register,
 * the GTCFoamTintListener shape — card-local {@code @EventBusSubscriber}, Dist.CLIENT, no
 * GTClientHandlers touch). The upstream render face verbatim: ONE grayscale texture
 * (Textures.BlockIcons.CONCRETES = {@code UT.Code.fill(CONCRETE, ..)}, Textures.java:704)
 * coloured by {@code DYES_INT[meta]} (BlockColored.java:63-73 getRenderColor) — the port
 * keeps the grayscale PNG + tintindex-0 models and resolves the tint per BLOCK: every
 * per-pair block carries its FIXED dye index, so the BlockColor is a constant lookup
 * ({@code GTSprayCanItem.DYES_INT[block.dyeIndex]}), tint index 0 only.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6ConcreteTintListener {

	private GT6ConcreteTintListener() {
	}

	/** The pure seam the tests drive: the opaque ARGB tint of one tint index over one block. */
	public static int concreteTintARGB(@Nullable Block aBlock, int aTintIndex) {
		if (aTintIndex != 0) return -1;
		if (aBlock instanceof GT6ConcreteBlock tFull) {
			return 0xFF000000 | GTSprayCanItem.DYES_INT[tFull.dyeIndex & 15];
		}
		if (aBlock instanceof GT6ConcreteSlabBlock tSlab) {
			return 0xFF000000 | GTSprayCanItem.DYES_INT[tSlab.dyeIndex & 15];
		}
		return -1;
	}

	/** The world-side half: registered over the 64 family blocks. */
	public static BlockColor concreteBlockColor() {
		return (aState, aLevel, aPos, aTintIndex) -> concreteTintARGB(aState.getBlock(), aTintIndex);
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		BlockColor tColor = concreteBlockColor();
		for (var tHandle : GT6ConcreteBlocks.FULL_BLOCKS) {
			aEvent.getBlockColors().register(tColor, tHandle.get());
		}
		for (var tHandle : GT6ConcreteBlocks.SLAB_BLOCKS) {
			aEvent.getBlockColors().register(tColor, tHandle.get());
		}
	}
}
