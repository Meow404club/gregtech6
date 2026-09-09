package gregtech6.client.foam;

import javax.annotation.Nullable;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.block.foam.GT6CFoamFreshBlock;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GT6FoamBlocks;
import gregtech6.tileentity.foam.GT6CFoamBlockEntity;

/**
 * The client-side BlockColor wiring of the C-Foam block family (task
 * p26-c-foam-block-family, the spec_rulings.ruling_color_dim tint leg) — the P25
 * {@code GTPipeFoamClientListener} shape (card-local {@code @EventBusSubscriber},
 * Dist.CLIENT, no GTClientHandlers touch).
 *
 * <p>Two tint sources over ONE tint index (0, the tintedCubeAll models):
 * <ul>
 * <li>the plain foamable blocks (fresh/dried × full/slab) read the {@code color}
 *     blockstate property — the DYES_INT slot of the sprayed can (the upstream
 *     BlockColored meta → icon pass-through, rendered here as the grayscale multiply);</li>
 * <li>the owned block reads the BE paint ({@code gt.color}, the upstream :82 NBT bundle
 *     — the {@code GTPipeFoamClientListener.java:56} BE-read precedent). A not-yet-synced
 *     BE (or a broken state) is the {@code -1} no-tint sentinel — the grayscale foam shows
 *     untinted for one frame at worst (the p25 accepted sentinel).</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTCFoamTintListener {

	private GTCFoamTintListener() {
	}

	/** The pure seam the tests drive: the opaque ARGB for one tint index over one state/BE pair. */
	public static int cfoamTintARGB(@Nullable BlockState aState, @Nullable BlockAndTintGetter aLevel,
			@Nullable BlockPos aPos, int aTintIndex) {
		if (aTintIndex != 0) return -1;
		if (aState != null && aState.hasProperty(GT6CFoamFreshBlock.COLOR)) {
			return 0xFF000000 | GTSprayCanItem.DYES_INT[aState.getValue(GT6CFoamFreshBlock.COLOR) & 15];
		}
		if (aLevel != null && aPos != null && aLevel.getBlockEntity(aPos) instanceof GT6CFoamBlockEntity tOwned
				&& tOwned.isPainted()) {
			return 0xFF000000 | (tOwned.getPaint() & 0xFFFFFF);
		}
		return -1;
	}

	/** The world-side half: registered over the five family blocks. */
	public static BlockColor cfoamBlockColor() {
		return GTCFoamTintListener::cfoamTintARGB;
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		aEvent.getBlockColors().register(cfoamBlockColor(),
				GT6FoamBlocks.CFOAM_FRESH.get(),
				GT6FoamBlocks.CFOAM.get(),
				GT6FoamBlocks.CFOAM_FRESH_SLAB.get(),
				GT6FoamBlocks.CFOAM_SLAB.get(),
				GT6FoamBlocks.CFOAM_OWNED.get());
	}
}
