package gregtech6.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.inventories.GT6BottleCrateBlockEntity;

/**
 * The bottle-crate content BER (task r11-geometry-batch) — the upstream 27 dynamic
 * content passes (MultiTileEntityBottleCrate :161-196 getRenderPasses2/setBlockBounds2,
 * the 9-slot mDisplay[] sync :55/:202-210), which the static plank-frame blockstate
 * model cannot express. Each occupied slot draws ITS stack through the vanilla FIXED
 * item display — the upstream semantic is "show the contents" and the potion tints ride
 * the item models, richer than the upstream gray fluid column (the 3-box mini-bottle
 * pass folded: the BOTTLECRATE_BOTTLE_* textures and per-pass fluid resolution are the
 * declared omission).
 *
 * <p><b>Slot placement</b> (upstream verbatim, WORLD-FIXED — the upstream bottle boxes
 * :170-196 do not consume mFacing, nor does the :105 click picker): slot i sits at
 * column {@code i % 3}, row {@code i / 3}, the cell centre at px {@code 3 + 5*col} /
 * {@code 3 + 5*row} (cells 1..5 / 6..10 / 11..15). The pose squash
 * (x/z 0.4 = 6.4px wide, y 0.875 = 14px tall over the 1..15px band) re-expresses the
 * upstream tall-thin bottle proportion on the square item sprite.
 *
 * <p><b>Client data</b>: the slots ride the vanilla two-channel sync — the chunk
 * payload ({@code getUpdateTag} = {@code saveWithoutMetadata}, the base pair) and the
 * mutation broadcast ({@code GT6BottleCrateBlockEntity.updateInventory → sendClientData},
 * the event-driven form of the upstream :80-94 tick arm).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTBottleCrateRenderer implements BlockEntityRenderer<GT6BottleCrateBlockEntity> {

	/** The cell-centre X/Z in render px (the upstream bottle boxes, WORLD-FIXED). */
	public static float slotCenterPx(int aSlot) {
		return 3.0F + 5.0F * (aSlot % 3);
	}

	/** The cell-centre Z in render px (the row half of the same grid). */
	public static float slotRowPx(int aSlot) {
		return 3.0F + 5.0F * (aSlot / 3);
	}

	// ------------------------------------------------------------------------------------
	// registration (the mod-bus client seam — the GTCrucibleContentRenderer shape)
	// ------------------------------------------------------------------------------------

	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers aEvent) {
		aEvent.registerBlockEntityRenderer(GTBlockEntities.BOTTLECRATE_BE.get(), GTBottleCrateRenderer::new);
	}

	public GTBottleCrateRenderer(BlockEntityRendererProvider.Context aContext) {
		// the provider context is unused — the display needs no camera-dependent state
	}

	@Override
	public void render(GT6BottleCrateBlockEntity aCrate, float aPartialTick, PoseStack aPoseStack,
			MultiBufferSource aBuffer, int aPackedLight, int aPackedOverlay) {
		var tRenderer = net.minecraft.client.Minecraft.getInstance().getItemRenderer();
		var tLevel = aCrate.getLevel();
		for (int i = 0, l = aCrate.getInventory().getSlots(); i < l; i++) {
			ItemStack tStack = aCrate.getInventory().getStackInSlot(i);
			if (tStack.isEmpty()) continue;
			aPoseStack.pushPose();
			aPoseStack.translate(slotCenterPx(i) / 16.0F, 0.5F, slotRowPx(i) / 16.0F);
			aPoseStack.scale(0.4F, 0.875F, 0.4F);
			tRenderer.renderStatic(tStack, ItemDisplayContext.FIXED, aPackedLight,
					OverlayTexture.NO_OVERLAY, aPoseStack, aBuffer, tLevel, 0);
			aPoseStack.popPose();
		}
	}
}
