package gregtech6.client.render;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Vector3f;

import gregapi.oredict.OreDictMaterial;
import gregtech6.datagen.GT6CrucibleDatagen;
import gregtech6.datagen.GT6CrucibleDatagen.ContentFace;
import gregtech6.registry.GT6Crucibles;
import gregtech6.tileentity.multiblocks.TileEntityCrucible;

/**
 * The LARGE crucible cavity content BER (task crucible-large-ber) — the upstream
 * MultiTileEntityCrucible render passes the blockstate model cannot express, drawn over
 * the formed controller cell (MultiTileEntityCrucible.java:628-638 setBlockBounds2, gated
 * on mStructureOkay :629):
 * <ol>
 * <li><b>pass 4</b> — the cavity floor plate, its visible TOP face at
 *     {@code y = 1.125} controller-local (:634 {@code box(-0.999, 0, -0.999, 1.999, 1.125,
 *     1.999)} — the port draws the centre-column plane, the rest of the footprint is hidden
 *     behind the full-cube wall models); the tint is the controller body seat
 *     ({@link GTMachinePaintTint#tintARGB} — the boilerModel tintindex-0 colour), the
 *     sprite the band's {@code colored_top}.</li>
 * <li><b>pass 5</b> — the content box, its visible TOP face only (:648
 *     {@code mDisplayedHeight != 0 && SIDES_TOP}), at
 *     {@code y = 1.125 + mDisplayedHeight/150.0} (:635 verbatim). The face rides the
 *     {@link ContentFace} dispatch (the crucible-bowl-model seam): the MOLTEN arm
 *     (the {@code GT6CrucibleDatagen.moltenTexture} per-set molten grayscale +
 *     mRGBaLiquid, rendered fullbright — the upstream {@code getTextureMolten} shape,
 *     OreDictMaterial.java:996-999; task r11b-crucible-molten-art retired the flat
 *     smeltery_content placeholder here too) when the synced displayed fluid exists,
 *     the SOLID arm (bodyTexture + mRGBaSolid — the declared port face over upstream's
 *     gray-NULL placeholder :622) otherwise. The floor quad (pass 4) stays the machine
 *     body seat — upstream's :647 machine-texture arm, NOT molten.</li>
 * </ol>
 *
 * <p><b>Melt-down red-shift</b> (upstream :611-616 {@code getRenderPasses2}): with
 * {@code mMeltDown} latched both faces' tint takes {@code r*2+50, g*2+50, b/2+50} per
 * channel (bind8) — the upstream mRenderedRGBA shift stacked into the BER colours. The
 * WALL red tint defers: the wall blocks' tint is BAKED (GTMachineTintModel, the p32 route
 * — the live-BE read is the red line and the runtime BlockColor rendered achromatic), a
 * per-wall arm would need ModelData + GTRenderUpdates plumbing on 24 walls (the
 * TileEntityCrucible latch comment carries the same declaration).
 *
 * <p><b>Client data</b>: mStructureOkay/mMeltDown ride the base10/base03 sync (the
 * structure flip {@code updateClientData}, the latch likewise); mDisplayedHeight/
 * mDisplayedFluid ride the paint-key pattern (the saveAdditional keys in both channels,
 * TileEntityCrucible.NBT_DISPLAYED_*). Quads are baked per frame over FaceBakery (the
 * GTFluidSpringBakedModel.bakeQuad form — two quads, no cache worth it) and emitted
 * through {@code putBulkData}, so the tint multiplies at emit time and no
 * VertexConsumer setter name crosses a leg (forge 7-arg / neo 8-arg form, the only fork).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTCrucibleContentRenderer implements BlockEntityRenderer<TileEntityCrucible> {

	/** The band sprite the controller blockstate model tints its body with (GT6BlockStates boilerModel("large_crucible", "crucible", true) — the band name is "crucible"). */
	private static final String FLOOR_SPRITE = "gt6:block/crucible/colored_top";

	/** The upstream :634 floor plane — 1.125 block units above the controller bottom, in render px. */
	private static final float FLOOR_PX = 18.0F;

	/** The upstream :635 height divisor (the content top is 1.125 + h/150 controller-local). */
	private static final float HEIGHT_DIVISOR = 150.0F;

	private static final FaceBakery BAKERY = new FaceBakery();

	// ------------------------------------------------------------------------------------
	// registration (the mod-bus client seam — the GTOvenClientListener shape)
	// ------------------------------------------------------------------------------------

	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers aEvent) {
		aEvent.registerBlockEntityRenderer(GT6Crucibles.MULTIBLOCK_CRUCIBLE_BE.get(), GTCrucibleContentRenderer::new);
	}

	// ------------------------------------------------------------------------------------
	// the pure seams the tests drive (no client statics — the GTMachineTintModel.tintQuads form)
	// ------------------------------------------------------------------------------------

	/**
	 * The :635 content top plane in render px above the controller bottom:
	 * {@code (1.125 + h/150) * 16}. h = 0 → the floor plane, h = 255 → 45.2px (2.825
	 * blocks, inside the 3-high cavity).
	 */
	public static float contentTopPx(int aDisplayedHeight) {
		return (1.125F + aDisplayedHeight / HEIGHT_DIVISOR) * 16.0F;
	}

	/**
	 * The :611-616 melt-down red-shift over one packed ARGB: {@code r*2+50, g*2+50, b/2+50}
	 * per channel, bind8-clamped. The alpha byte passes through.
	 */
	public static int meltDownShift(int aARGB) {
		int r = (aARGB >>> 16) & 255, g = (aARGB >>> 8) & 255, b = aARGB & 255;
		return (aARGB & 0xFF000000) | bind8(r * 2 + 50) << 16 | bind8(g * 2 + 50) << 8 | bind8(b / 2 + 50);
	}

	/** The bind8 clamp (the upstream UT.Code.bind8 semantics, 0..255). */
	private static int bind8(int aValue) {
		return Math.max(0, Math.min(255, aValue));
	}

	// ------------------------------------------------------------------------------------
	// the render (upstream getRenderPasses2/setBlockBounds2/getTexture2 :610-651)
	// ------------------------------------------------------------------------------------

	public GTCrucibleContentRenderer(BlockEntityRendererProvider.Context aContext) {
		// the provider context is unused — the two quads need no camera-dependent state
	}

	@Override
	public void render(TileEntityCrucible aCrucible, float aPartialTick, PoseStack aPoseStack, MultiBufferSource aBuffer, int aPackedLight, int aPackedOverlay) {
		if (!aCrucible.mStructureOkay) return; // :629 — the mStructureOkay gate over every pass

		Block tBlock = aCrucible.getBlockState().getBlock();
		int tFloorTint = GTMachinePaintTint.tintARGB(aCrucible.getModelData(), GTMachinePaintTint.tintMaterialOf(tBlock), 0);
		if (aCrucible.mMeltDown) tFloorTint = meltDownShift(tFloorTint);
		int tContentTint = -1;
		TextureAtlasSprite tContentSprite = null;
		boolean tMolten = false;
		if (aCrucible.mDisplayedHeight != 0) { // the :648 SIDES_TOP + non-zero gate
			OreDictMaterial tDisplayed = GT6Crucibles.materialById(aCrucible.mDisplayedFluid);
			// the MOLTEN arm when the synced displayed fluid exists, the SOLID arm (the shell's
			// bodyTexture) over the solid charge — upstream rendered the gray-NULL :622 there
			tMolten = tDisplayed != null;
			ContentFace tFace = GT6CrucibleDatagen.contentFace(tMolten ? tDisplayed : GTMachinePaintTint.tintMaterialOf(tBlock), tMolten);
			tContentSprite = spriteOf(tFace.texture());
			tContentTint = aCrucible.mMeltDown ? meltDownShift(tFace.tintARGB()) : tFace.tintARGB();
		}

		aPoseStack.pushPose();
		// the quads bake in model px; the BER pose is block units — scale once
		aPoseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
		com.mojang.blaze3d.vertex.VertexConsumer tConsumer = aBuffer.getBuffer(RenderType.cutout());
		// pass 4 — the cavity floor top face at 1.125 (the blockstate top face beneath stays,
		// it is fully covered from above and hidden by the walls from the side)
		emitQuad(tConsumer, aPoseStack, spriteOf(FLOOR_SPRITE), FLOOR_PX, tFloorTint, aPackedLight, aPackedOverlay);
		// pass 5 — the content top face; fullbright when molten (the getTextureMolten glow)
		if (tContentSprite != null) {
			int tLight = tMolten ? LightTexture.FULL_BRIGHT : aPackedLight;
			emitQuad(tConsumer, aPoseStack, tContentSprite, contentTopPx(aCrucible.mDisplayedHeight), tContentTint, tLight, aPackedOverlay);
		}
		aPoseStack.popPose();
	}

	/** One UP-facing quad spanning the centre column at the given plane, emitted with the tint multiply. */
	private static void emitQuad(com.mojang.blaze3d.vertex.VertexConsumer aConsumer, PoseStack aPoseStack, TextureAtlasSprite aSprite, float aTopPx, int aTint, int aPackedLight, int aPackedOverlay) {
		// the FaceBakery form (GTFluidSpringBakedModel.bakeQuad): model space 0..16, the full-face
		// UV over the box bounds, tintIndex -1 (no runtime lookup — the colour multiplies here)
		BakedQuad tQuad = BAKERY.bakeQuad(new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, aTopPx, 16.0F),
				new BlockElementFace(null, -1, aSprite.contents().name().toString(), new BlockFaceUV(
						new float[] {0.0F, 0.0F, 16.0F, 16.0F}, 0)),
				aSprite, Direction.UP, BlockModelRotation.X0_Y0, null, true, aSprite.contents().name());
		float tR = ((aTint >>> 16) & 255) / 255.0F, tG = ((aTint >>> 8) & 255) / 255.0F, tB = (aTint & 255) / 255.0F;
		//? if forge {
		aConsumer.putBulkData(aPoseStack.last(), tQuad, tR, tG, tB, aPackedLight, aPackedOverlay);
		//?} else {
		/*aConsumer.putBulkData(aPoseStack.last(), tQuad, tR, tG, tB, 1.0F, aPackedLight, aPackedOverlay); // 21.1: the bulk emit takes the alpha (ItemRenderer.java.patch form)
		 *///?}
	}

	/** The blocks-atlas sprite lookup (the GTFluidSpringBakedModel materialOf face, the ModelManager-backed form). */
	private static TextureAtlasSprite spriteOf(String aQualified) {
		return net.minecraft.client.Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
				.apply(GT6CrucibleDatagen.loc(aQualified));
	}

	@Override
	public boolean shouldRenderOffScreen(TileEntityCrucible aCrucible) {
		return true; // the cavity faces rise ~2.8 blocks above the controller cell — never clip against its section's frustum cull
	}
}
