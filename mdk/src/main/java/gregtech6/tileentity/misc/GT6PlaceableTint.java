package gregtech6.tileentity.misc;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.GT6Mod;
import gregtech6.registry.GT6Placeables;

/**
 * The placed-pile tint (task p32-placeables) — the upstream material colour pass
 * ({@code mMaterial.fRGBaSolid} over the grayscale pile icons, MultiTileEntityIngot
 * .java:46-48 the family form) over the modern block-color seam: the tint reads the
 * pile BE's material live (the {@code getClientDataPacket} material-id sync upstream,
 * MultiTileEntityPlaceable.java:116-125 — the port syncs the whole stack NBT through the
 * base getUpdateTag face, the tint just reads the derived material).
 *
 * <p>Registered from its OWN client-gated subscriber class (the Dist.CLIENT annotation
 * keeps the dedicated server from ever loading the event type — the GTClientHandlers
 * init-guard rationale, inlined so the placeables card keeps its file scope). The stick
 * model carries no tintindex (the vanilla log borrow, the W6 stick form); the flint rock
 * reads null material → -1 = vanilla stone grey.
 */
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6PlaceableTint {

	private GT6PlaceableTint() {}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		aEvent.getBlockColors().register(GT6PlaceableTint::tint,
				GT6Placeables.PLACED_ROCK.get(), GT6Placeables.PLACED_INGOT.get(),
				GT6Placeables.PLACED_PLATE.get(), GT6Placeables.PLACED_GEM_PLATE.get(),
				GT6Placeables.PLACED_SCRAP.get());
		// the stick pile is un-tinted (the vanilla oak_log borrow, no tintindex in the model)
	}

	/** The material colour for tintindex 0 (the GT6SurfaceRockBlock.tintARGB arithmetic). */
	private static int tint(BlockState aState, @Nullable BlockAndTintGetter aLevel, @Nullable BlockPos aPos, int aTintIndex) {
		if (aTintIndex != 0 || aLevel == null || aPos == null) return -1;
		if (aLevel.getBlockEntity(aPos) instanceof GT6PlaceableBlockEntity tPile
				&& tPile.material() != null) {
			return 0xFF000000 | tPile.material().mRGBaSolid[0] << 16 | tPile.material().mRGBaSolid[1] << 8 | tPile.material().mRGBaSolid[2];
		}
		return -1;
	}
}
