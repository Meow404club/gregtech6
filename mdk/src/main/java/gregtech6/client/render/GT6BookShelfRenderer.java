package gregtech6.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.inventories.GT6BookShelfBlockEntity;

/**
 * The book-shelf content BER (task shelf-crate-geometry) — the upstream 28 book passes
 * ({@code MultiTileEntityBookShelf.usesRenderPass2 :286-288} + {@code setBlockBounds2
 * :314-322}, the {@code mDisplay[28]} sync :58/:162-168), which the static frame model
 * cannot express. Each occupied slot draws ITS stack through the vanilla FIXED item
 * display (the {@link GTBottleCrateRenderer} shape) — the upstream per-book-type spine
 * textures ({@code BOOK_TEXTURES_SIDE/BACK} :346) fold: the real item carries the
 * richer art, the crate precedent.
 *
 * <p><b>Niche mapping</b> (upstream verbatim, north default — front face = {@code -Z},
 * the mFacing side): slot {@code s} rides the back face from 14 ({@code aRenderPass>=21}
 * is {@code OPOS[mFacing]}), the top row while {@code s%14 < 7} (the :314/:317 bands
 * y 9..15), column {@code s%7}. Front columns centre at x {@code 2+2c}px (the :314 box
 * x 1+2c..3+2c), back columns mirror to {@code 14-2c}px (:316 x 13-2c..15-2c); depths
 * z 4.5px front (band 2..7) / 11.5px back (band 9..14); both rows fill a 6px band (the
 * top y 9..15, the bottom y 1..7 — {@code PX_P[1]..PX_N[9]} where {@code PX_N[9]} reads
 * 7px, the seat-25 correction). The spine scale re-expresses the upstream book boxes on
 * the square item sprite: 2px wide (the column width), the row-height fill (6px both
 * rows — the exact band heights).
 *
 * <p><b>Facing</b>: the blockstate yaw table (north 0 / east 90 / south 180 / west 270,
 * the {@code addStaticStorages} rotations) is CLOCKWISE from above, so the pose-stack
 * twin is the negated right-hand angle (0/270/180/90) — the BER books land exactly where
 * the blockstate-rotated frame opens its niches.
 *
 * <p><b>Client data</b>: the slots ride the vanilla two-channel sync — the chunk payload
 * ({@code getUpdateTag} = {@code saveWithoutMetadata}) and the mutation broadcast (the
 * base {@code updateInventory} arm; the shelf inherits the crate's event-driven shape
 * through the shared base BE).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6BookShelfRenderer implements BlockEntityRenderer<GT6BookShelfBlockEntity> {

	/** The spine width scale: 2px over the 16px sprite (the upstream book box width, :314). */
	public static final float SPINE_WIDTH = 2.0F / 16.0F;

	/** The z thinning: 1px (the sprite extrusion collapses to the niche band). */
	private static final float SPINE_DEPTH = 1.0F / 16.0F;

	// ------------------------------------------------------------------------------------
	// the niche mapping (the offline-tested statics, upstream :314-322 verbatim)
	// ------------------------------------------------------------------------------------

	/** Slots 14..27 ride the back face (the {@code aRenderPass >= 21} = OPOS split, :287). */
	public static boolean isBackFace(int aSlot) {
		return aSlot >= GT6BookShelfBlockEntity.SLOTS_PER_FACE;
	}

	/** The top row of each face: slots 0..6 and 14..20 (the y 9..15 bands, :314/:317). */
	public static boolean isTopRow(int aSlot) {
		return aSlot % GT6BookShelfBlockEntity.SLOTS_PER_FACE < 7;
	}

	/** The column centre x in px: front {@code 2+2c} (:314), back mirrored {@code 14-2c} (:316). */
	public static float columnCenterPx(int aSlot) {
		int tCol = aSlot % 7;
		return isBackFace(aSlot) ? 14.0F - 2.0F * tCol : 2.0F + 2.0F * tCol;
	}

	/** The row centre y in px: top 12 (the 9..15 band), bottom 4 (the 1..7 band, :315). */
	public static float rowCenterPx(int aSlot) {
		return isTopRow(aSlot) ? 12.0F : 4.0F;
	}

	/** The niche depth centre z in px: front 4.5 (band 2..7), back 11.5 (band 9..14). */
	public static float depthCenterPx(int aSlot) {
		return isBackFace(aSlot) ? 11.5F : 4.5F;
	}

	/** The top-row spine height: the 6px band fill (:314). */
	public static float topRowHeight() {
		return 6.0F / 16.0F;
	}

	/** The bottom-row spine height: the 6px band fill (the :315 band 1..7). */
	public static float bottomRowHeight() {
		return 6.0F / 16.0F;
	}

	// ------------------------------------------------------------------------------------
	// registration (the mod-bus client seam — the GTBottleCrateRenderer shape)
	// ------------------------------------------------------------------------------------

	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers aEvent) {
		aEvent.registerBlockEntityRenderer(GTBlockEntities.BOOKSHELF_BE.get(), GT6BookShelfRenderer::new);
	}

	public GT6BookShelfRenderer(BlockEntityRendererProvider.Context aContext) {
		// the provider context is unused — the display needs no camera-dependent state
	}

	@Override
	public void render(GT6BookShelfBlockEntity aShelf, float aPartialTick, PoseStack aPoseStack,
			MultiBufferSource aBuffer, int aPackedLight, int aPackedOverlay) {
		var tRenderer = net.minecraft.client.Minecraft.getInstance().getItemRenderer();
		var tLevel = aShelf.getLevel();
		Direction tFacing = aShelf.getBlockState().getValue(
				gregtech6.registry.GT6StaticStorages.GT6StorageBlock.FACING);
		aPoseStack.pushPose();
		aPoseStack.translate(0.5, 0.5, 0.5);
		// the negated blockstate yaw (the class doc) — north 0 / east 270 / south 180 / west 90
		aPoseStack.mulPose(Axis.YP.rotationDegrees(switch (tFacing) {
			case EAST -> 270.0F;
			case SOUTH -> 180.0F;
			case WEST -> 90.0F;
			default -> 0.0F;
		}));
		for (int i = 0, l = aShelf.getInventory().getSlots(); i < l; i++) {
			ItemStack tStack = aShelf.getInventory().getStackInSlot(i);
			if (tStack.isEmpty()) continue;
			aPoseStack.pushPose();
			aPoseStack.translate((columnCenterPx(i) - 8.0F) / 16.0F, (rowCenterPx(i) - 8.0F) / 16.0F,
					(depthCenterPx(i) - 8.0F) / 16.0F);
			aPoseStack.scale(SPINE_WIDTH, isTopRow(i) ? topRowHeight() : bottomRowHeight(), SPINE_DEPTH);
			tRenderer.renderStatic(tStack, ItemDisplayContext.FIXED, aPackedLight,
					OverlayTexture.NO_OVERLAY, aPoseStack, aBuffer, tLevel, 0);
			aPoseStack.popPose();
		}
		aPoseStack.popPose();
	}
}
