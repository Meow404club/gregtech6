package gregtech6.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.inventories.GT6MassStorageBlockEntity;

/**
 * The mass storage content + digit BER (task storage-massstorage) — the port fold of the
 * upstream two render faces: the TESR content display
 * ({@code MultiTileEntityMassStorage.MultiTileEntityRendererMassStorage :706-758}) and the
 * seven-pass digit strip (the Standard/Logistics {@code getRenderPasses2/setBlockBounds2/
 * getTexture2/usesRenderPass2}, MassStorageStandard :38-141 — the architecture red line
 * routes both onto the BER, the bottle-crate content BER precedent).
 *
 * <p><b>The content display</b> (upstream :711-741 verbatim constants): the stored item
 * draws FLAT on the front face, half a block tall (the 1/256 × 8.0 scale dance), centred
 * 0.625 up, pushed 0.502 out of the face and 0.25 toward the viewer's LEFT (the
 * {@code -OFFZ[mFacing]*0.25} lateral for every horizontal facing — the invariant across
 * the four upstream arms), upright (the upstream 180°-Z flip compensated the GUI Y-down
 * convention; the vanilla FIXED display is already world-upright).
 *
 * <p><b>The digit strip</b> (upstream :44-107 geometry, face-local): six 2px cells in the
 * y 12..14px band across x 2..14px, most-significant LEFT (the upstream pass order reads
 * viewer-left-to-right on all four facings); a cell draws its decimal digit
 * (BI.decimalDigit(count, 6-pass), :125/:126) when {@code count > 99999/9999/999/99/9}
 * gates open (usesRenderPass2 :131-140, pass 6 = the units digit always); at
 * {@code count >= mMaxStorage} the strip collapses to the red "100%" pattern
 * (:117-124 — passes 2..5, CA_RED_255). The digit tint is the WHITE standard set (:125
 * CA_WHITE) or the CYAN logistics set (:126 CA_CYAN_255), vertex-coloured.
 *
 * <p><b>Client data</b>: the strip content rides the vanilla two-channel sync — the chunk
 * payload (the base {@code getUpdateTag}) and the mutation broadcasts (the
 * {@code updateInventory} face), the bottle-crate posture.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6MassStorageRenderer implements BlockEntityRenderer<GT6MassStorageBlockEntity> {

	/** The digit sprite sheet (the upstream gregapi overlays/characters band, byte-identical copies). */
	public static final Material CHAR_SHEET = new Material(
			net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS,
			ResourceLocation.fromNamespaceAndPath("gt6", "block/characters"));

	/** The digit cell horizontal band in face-local px (the upstream x 2..14 strip). */
	public static final float STRIP_LEFT_PX = 2.0F, STRIP_RIGHT_PX = 14.0F, STRIP_BOTTOM_PX = 12.0F, STRIP_TOP_PX = 14.0F;

	/** The visibility gate constants of the digit passes (upstream usesRenderPass2 :131-140). */
	public static final int[] DIGIT_GATE = {99999, 9999, 999, 99, 9};

	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers aEvent) {
		aEvent.registerBlockEntityRenderer(GTBlockEntities.MASS_STORAGE_BE.get(), GT6MassStorageRenderer::new);
		aEvent.registerBlockEntityRenderer(GTBlockEntities.MASS_STORAGE_LOGISTICS_BE.get(), GT6MassStorageRenderer::new);
	}

	public GT6MassStorageRenderer(BlockEntityRendererProvider.Context aContext) {
		// the provider context is unused — the display needs no camera-dependent state
	}

	@Override
	public void render(GT6MassStorageBlockEntity aStorage, float aPartialTick, PoseStack aPoseStack,
			MultiBufferSource aBuffer, int aPackedLight, int aPackedOverlay) {
		if (!aStorage.slotHas(GT6MassStorageBlockEntity.SLOT_MASS) || !aStorage.isFaceVisible()) return;
		Direction tFront = Direction.from3DDataValue(aStorage.getFacing());

		aPoseStack.pushPose();
		// the face-local frame: origin at the block centre, +Z pointing OUT of the front
		// face (the -toYRot rotation — see the class doc lateral invariant)
		aPoseStack.translate(0.5, 0, 0.5);
		aPoseStack.mulPose(Axis.YP.rotationDegrees(-tFront.toYRot()));

		renderContentItem(aStorage, aPoseStack, aBuffer, aPackedLight, aPackedOverlay);
		renderDigitStrip(aStorage, aPoseStack, aBuffer, aPackedLight);
		aPoseStack.popPose();
	}

	/** The stored item, flat on the face (upstream :722-741). */
	private void renderContentItem(GT6MassStorageBlockEntity aStorage, PoseStack aPoseStack,
			MultiBufferSource aBuffer, int aPackedLight, int aPackedOverlay) {
		ItemStack tContent = aStorage.slot(GT6MassStorageBlockEntity.SLOT_MASS);
		aPoseStack.pushPose();
		aPoseStack.translate(-0.25, 0.625, 0.502); // the centre + the viewer-left lateral + the face push
		aPoseStack.scale(0.5F, 0.5F, 0.5F);        // the 1/256 × 8.0 dance folded
		Minecraft.getInstance().getItemRenderer().renderStatic(tContent, ItemDisplayContext.FIXED,
				240, OverlayTexture.NO_OVERLAY, aPoseStack, aBuffer, aStorage.getLevel(), 0);
		aPoseStack.popPose();
	}

	/** The digit strip (upstream :44-141). */
	private void renderDigitStrip(GT6MassStorageBlockEntity aStorage, PoseStack aPoseStack,
			MultiBufferSource aBuffer, int aPackedLight) {
		int tCount = aStorage.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount();
		boolean tFull = tCount >= aStorage.mMaxStorage;
		int tTint = tFull ? GT6MassStorageBlockEntity.FULL_DIGIT_ARGB : aStorage.digitARGB();
		var tSpriteLookup = Minecraft.getInstance().getTextureAtlas(CHAR_SHEET.atlasLocation());

		float tCellWidth = (STRIP_RIGHT_PX - STRIP_LEFT_PX) / 6.0F;
		float tFrontZ = 0.5F + 0.002F;
		VertexConsumer tConsumer = CHAR_SHEET.buffer(aBuffer, aLoc -> net.minecraft.client.renderer.RenderType.cutout());
		for (int tCell = 0; tCell < 6; tCell++) {
			String tChar = digitCharacter(tCount, tCell, tFull);
			if (tChar == null) continue;
			// face-local: after the -toYRot rotation the viewer's LEFT is the local -X
			// (the north check: rot 180 sends local +X to world -X = the RIGHT hand of a
			// viewer gazing south), so the ascending walk below reads viewer-left →
			// right — the MSD cell first (the upstream pass order on all four facings)
			float tX0 = STRIP_LEFT_PX / 16.0F + tCell * (tCellWidth / 16.0F);
			float tX1 = tX0 + tCellWidth / 16.0F;
			float tY0 = STRIP_BOTTOM_PX / 16.0F;
			float tY1 = STRIP_TOP_PX / 16.0F;
			var tSprite = tSpriteLookup.apply(ResourceLocation.fromNamespaceAndPath("gt6", "block/characters/" + tChar));
			// the quad: outward normal (+Z local), UV upright
			int tR = (tTint >> 16) & 0xFF, tG = (tTint >> 8) & 0xFF, tB = tTint & 0xFF;
			quad(tConsumer, aPoseStack, tX0 - 0.5F, tY0 - 0.5F, tFrontZ, tX1 - 0.5F, tY1 - 0.5F, tSprite, tR, tG, tB, aPackedLight);
		}
	}

	/**
	 * The cell's character (the upstream getTexture2 :117-126 + usesRenderPass2 :131-140
	 * fusion). @return the sprite tail, or null for an empty cell
	 */
	public static String digitCharacter(int aCount, int aCell, boolean aFull) {
		if (aFull) {
			// the red "100%" pattern: cells 1..4, the rest null
			return switch (aCell) {
				case 1 -> "1";
				case 2 -> "0";
				case 3 -> "0";
				case 4 -> "percent";
				default -> null;
			};
		}
		// cell 0 = MSD (the 100000s place): the gate ladder; cell 5 = the units, always
		if (aCell < 5 && aCount <= DIGIT_GATE[aCell]) return null;
		int tPower = 1;
		for (int i = 0; i < 5 - aCell; i++) tPower *= 10;
		return Integer.toString((aCount / tPower) % 10);
	}

	/** One flat quad into the buffer (4 vertices, upright UV, the outward +Z normal). */
	private static void quad(VertexConsumer aConsumer, PoseStack aPoseStack, float aX0, float aY0, float aZ,
			float aX1, float aY1, net.minecraft.client.renderer.texture.TextureAtlasSprite aSprite, int aR, int aG, int aB, int aPackedLight) {
		org.joml.Matrix4f tPose = aPoseStack.last().pose();
		float tU0 = aSprite.getU(0), tV0 = aSprite.getV(0), tU1 = aSprite.getU(16), tV1 = aSprite.getV(16);
		// CCW from the +Z viewer: bottom-left, bottom-right, top-right, top-left
		vertex(aConsumer, tPose, aX0, aY0, aZ, tU0, tV1, aR, aG, aB, aPackedLight);
		vertex(aConsumer, tPose, aX1, aY0, aZ, tU1, tV1, aR, aG, aB, aPackedLight);
		vertex(aConsumer, tPose, aX1, aY1, aZ, tU1, tV0, aR, aG, aB, aPackedLight);
		vertex(aConsumer, tPose, aX0, aY1, aZ, tU0, tV0, aR, aG, aB, aPackedLight);
	}

	private static void vertex(VertexConsumer aConsumer, org.joml.Matrix4f aPose, float aX, float aY, float aZ,
			float aU, float aV, int aR, int aG, int aB, int aPackedLight) {
		aConsumer.vertex(aPose, aX, aY, aZ)
				.color(aR, aG, aB, 255)
				.uv(aU, aV)
				.uv2(aPackedLight)
				.endVertex();
	}
}
