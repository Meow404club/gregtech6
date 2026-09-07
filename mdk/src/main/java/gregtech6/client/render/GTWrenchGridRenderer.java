package gregtech6.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import gregtech6.client.render.GTWrenchGridTables.GTWrenchGridIcon;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;

/**
 * The wrench 3x3 grid overlay renderer (task p5-wrench-ui-gtceu, the transient
 * input-feedback exception of ADR 2026-08-30-p5-wrench-ui): drawn per frame from the
 * {@code RenderHighlightEvent.Block} parameters, never cached, never written back —
 * zero BE/Level static references (the BE is handed in fresh from the listener), no
 * event cancellation (the vanilla selection box stays), nothing reaches the chunk mesh.
 *
 * <p>Geometry is derived from {@link GTWrenchGridTables} and drawn straight in world
 * coordinates — the GTCEu moveToFace/rotateToFace chain (BlockHighlightRenderer:238-241)
 * is deliberately not ported, removing the RelativeDirection orientation-mismatch risk
 * class: the in-face axis tables here are the same world axes the click pick uses.
 *
 * <p>Visual language (GTCEu BlockHighlightRenderer verbatim where noted): the 4 grid
 * lines sit on the 0.25/0.75 pick thresholds — the actual pick regions of
 * {@code UT6.getSideWrenching}, not thirds — as {@link RenderType#lines} at width 3
 * with the blue-white pulse {@code r=g=0.2+sin(now/800)/2, b=1} (:163-164, ~2.5 s);
 * the 9 icons are 4x4 (of 1/16) quads with a 0.2 inset bound straight through
 * {@link RenderType#text} (vanilla RenderType.java:951 — direct texture, no atlas,
 * GT6Atlases untouched) at {@link LightTexture#FULL_BRIGHT}; the hovered cell draws
 * full white, every other cell 27% white (:247-272). The TEXT render type culls back
 * faces (CompositeState default CULL), so each quad's winding is emitted CCW as seen
 * from outside the face — the in-face axis tables give u x v = the outward normal on
 * faces 0/3/4 and the inward normal on 1/2/5, hence the flipped order there.
 */
@OnlyIn(Dist.CLIENT)
public final class GTWrenchGridRenderer {

	/** The overlay lift off the clicked face (GTCEu BlockHighlightRenderer:236 — 0.01). */
	private static final float FACE_OFFSET = 0.01F;

	/** The hovered cell renders full white, every other cell 27% white (GTCEu :247-272). */
	private static final int COLOR_HOVER = 0xFFFFFFFF;
	private static final int COLOR_DIM = 0x44FFFFFF;

	private GTWrenchGridRenderer() {
	}

	/**
	 * Draws the grid for one frame. All state comes in through the parameters — the BE
	 * was fetched from the level by the listener this very frame, the pose stack is the
	 * event's world-space frame pose (LevelRenderer.renderHitOutline receives the
	 * renderLevel pose, hence the camera translation GTCEu does at :219).
	 */
	public static void renderGrid(PoseStack aPoseStack, MultiBufferSource aBuffers, Camera aCamera,
			BlockHitResult aTarget, boolean aShift, GTFluidPipeBlockEntity aPipe) {
		BlockPos tPos = aTarget.getBlockPos();
		byte tFace = (byte)aTarget.getDirection().get3DDataValue();
		// the same 0..1 hit offsets the click path feeds UT6 (GTFluidPipeBlock.use:111-113)
		byte tHover = GTWrenchGridTables.hoverSide(tFace,
				(float)(aTarget.getLocation().x - tPos.getX()),
				(float)(aTarget.getLocation().y - tPos.getY()),
				(float)(aTarget.getLocation().z - tPos.getZ()));
		byte tConnections = aPipe.getConnections();
		byte tIoMask = aPipe.getIoMask();

		float[] tOrigin = faceOrigin(tPos, tFace);
		float[] tU = axisVector(GTWrenchGridTables.cellUAxis(tFace));
		float[] tV = axisVector(GTWrenchGridTables.cellVAxis(tFace));

		Vec3 tCamPos = aCamera.getPosition();
		aPoseStack.pushPose();
		aPoseStack.translate(-tCamPos.x, -tCamPos.y, -tCamPos.z);

		drawGridLines(aPoseStack, aBuffers, tOrigin, tU, tV);
		drawIcons(aPoseStack, aBuffers, tFace, tOrigin, tU, tV, tHover, tConnections, tIoMask, aShift);

		aPoseStack.popPose();
	}

	/**
	 * The oven grid entry (task p6-oven-rotation): the same 3x3 grid over the same
	 * in-face axis tables, but the icon layer is the machine-rotation table —
	 * {@link GTWrenchGridTables#ovenCellIcon} decides per cell and a null icon cell
	 * draws nothing. The front facing arrives from the BlockState, the client display
	 * authority: setBlock(state, 3) syncs the blockstate without re-sending the BE
	 * NBT, so the BE's own mFacing byte can be stale on the client.
	 */
	public static void renderOvenGrid(PoseStack aPoseStack, MultiBufferSource aBuffers, Camera aCamera,
			BlockHitResult aTarget, boolean aShift, byte aFrontFacing) {
		BlockPos tPos = aTarget.getBlockPos();
		byte tFace = (byte)aTarget.getDirection().get3DDataValue();
		// the same 0..1 hit offsets the click path feeds UT6 (GTOvenBlock.use rotation branch)
		byte tHover = GTWrenchGridTables.hoverSide(tFace,
				(float)(aTarget.getLocation().x - tPos.getX()),
				(float)(aTarget.getLocation().y - tPos.getY()),
				(float)(aTarget.getLocation().z - tPos.getZ()));

		float[] tOrigin = faceOrigin(tPos, tFace);
		float[] tU = axisVector(GTWrenchGridTables.cellUAxis(tFace));
		float[] tV = axisVector(GTWrenchGridTables.cellVAxis(tFace));

		Vec3 tCamPos = aCamera.getPosition();
		aPoseStack.pushPose();
		aPoseStack.translate(-tCamPos.x, -tCamPos.y, -tCamPos.z);

		drawGridLines(aPoseStack, aBuffers, tOrigin, tU, tV);
		for (int tRow = 0; tRow < 3; tRow++) {
			for (int tCol = 0; tCol < 3; tCol++) {
				byte tCellSide = GTWrenchGridTables.cellSide(tFace, tCol, tRow);
				GTWrenchGridIcon tIcon = GTWrenchGridTables.ovenCellIcon(aShift, tCellSide, aFrontFacing);
				if (tIcon == null) continue; // non-shift mode and dead cells draw no icon
				drawCellIcon(aPoseStack, aBuffers, tFace, tOrigin, tU, tV, tHover, tCol, tRow, tCellSide, tIcon);
			}
		}

		aPoseStack.popPose();
	}

	// ---------------------------------------------------------------------------
	// grid lines — the 0.25/0.75 pick-threshold cross (GTCEu :225-229 shape)
	// ---------------------------------------------------------------------------

	private static void drawGridLines(PoseStack aPoseStack, MultiBufferSource aBuffers, float[] aOrigin, float[] aU, float[] aV) {
		// the pulse is GTCEu :163-164 verbatim (period 800*pi ms ~= 2.5 s)
		float tRG = 0.2F + (float)Math.sin((System.currentTimeMillis() % (Mth.PI * 800)) / 800) / 2;
		float tB = 1.0F;
		VertexConsumer tBuffer = aBuffers.getBuffer(RenderType.lines());
		RenderSystem.lineWidth(3);
		PoseStack.Pose tPose = aPoseStack.last();
		// two lines along u at v=0.25/0.75 and two along v at u=0.25/0.75 — the full
		// cross that carves out the centre band, the edge bands and the corner cells
		drawLine(tPose, tBuffer, point(aOrigin, aU, 0.0F, aV, 0.25F), point(aOrigin, aU, 1.0F, aV, 0.25F), tRG, tB);
		drawLine(tPose, tBuffer, point(aOrigin, aU, 0.0F, aV, 0.75F), point(aOrigin, aU, 1.0F, aV, 0.75F), tRG, tB);
		drawLine(tPose, tBuffer, point(aOrigin, aU, 0.25F, aV, 0.0F), point(aOrigin, aU, 0.25F, aV, 1.0F), tRG, tB);
		drawLine(tPose, tBuffer, point(aOrigin, aU, 0.75F, aV, 0.0F), point(aOrigin, aU, 0.75F, aV, 1.0F), tRG, tB);
	}

	private static void drawLine(PoseStack.Pose aPose, VertexConsumer aBuffer, float[] aFrom, float[] aTo, float aRG, float aB) {
		float tNx = aTo[0] - aFrom[0], tNy = aTo[1] - aFrom[1], tNz = aTo[2] - aFrom[2];
		aBuffer.vertex(aPose.pose(), aFrom[0], aFrom[1], aFrom[2])
				.color(aRG, aRG, aB, 1.0F)
				.normal(aPose.normal(), tNx, tNy, tNz)
				.endVertex();
		aBuffer.vertex(aPose.pose(), aTo[0], aTo[1], aTo[2])
				.color(aRG, aRG, aB, 1.0F)
				.normal(aPose.normal(), tNx, tNy, tNz)
				.endVertex();
	}

	// ---------------------------------------------------------------------------
	// the 9 icon quads (GTCEu :246-272 layout over the in-face axes)
	// ---------------------------------------------------------------------------

	private static void drawIcons(PoseStack aPoseStack, MultiBufferSource aBuffers, byte aFace, float[] aOrigin,
			float[] aU, float[] aV, byte aHover, byte aConnections, byte aIoMask, boolean aShift) {
		for (int tRow = 0; tRow < 3; tRow++) {
			for (int tCol = 0; tCol < 3; tCol++) {
				byte tCellSide = GTWrenchGridTables.cellSide(aFace, tCol, tRow);
				GTWrenchGridIcon tIcon = GTWrenchGridTables.iconFor(aShift, tCellSide, aConnections, aIoMask);
				drawCellIcon(aPoseStack, aBuffers, aFace, aOrigin, aU, aV, aHover, tCol, tRow, tCellSide, tIcon);
			}
		}
	}

	/**
	 * Emits one cell's icon quad — the mechanical per-cell extraction of the p5
	 * drawIcons body shared by both grid entries (the pipe emission order and math are
	 * byte-identical); the hover tint is the only caller-supplied-state difference,
	 * folded in via {@code aCellSide == aHover}.
	 */
	private static void drawCellIcon(PoseStack aPoseStack, MultiBufferSource aBuffers, byte aFace,
			float[] aOrigin, float[] aU, float[] aV, byte aHover, int aCol, int aRow, byte aCellSide,
			GTWrenchGridIcon aIcon) {
		float tInset = GTWrenchGridTables.MARGIN_16 / 16.0F;
		// u x v is the outward normal on faces 0/3/4 and the inward normal on 1/2/5 —
		// the TEXT render type culls back faces, so flip the winding there
		boolean tFlip = aFace == 1 || aFace == 2 || aFace == 5;
		PoseStack.Pose tPose = aPoseStack.last();
		int tColor = aCellSide == aHover ? COLOR_HOVER : COLOR_DIM;
		float tU0 = GTWrenchGridTables.CELL_ORIGIN_16[aCol] / 16.0F + tInset;
		float tU1 = (GTWrenchGridTables.CELL_ORIGIN_16[aCol] + GTWrenchGridTables.CELL_16) / 16.0F - tInset;
		float tV0 = GTWrenchGridTables.CELL_ORIGIN_16[aRow] / 16.0F + tInset;
		float tV1 = (GTWrenchGridTables.CELL_ORIGIN_16[aRow] + GTWrenchGridTables.CELL_16) / 16.0F - tInset;
		// PNG u runs with the table u axis; PNG v runs against the table v axis —
		// PNG top is the v-FAR end (the non-flipped table maps the v-near corner to
		// uv(0,1), the PNG bottom); the pairing is fixed, only the emission order
		// flips to keep the quad CCW from outside
		float[][] tQuads = tFlip
				? new float[][] { point(aOrigin, aU, tU0, aV, tV0), point(aOrigin, aU, tU0, aV, tV1),
						point(aOrigin, aU, tU1, aV, tV1), point(aOrigin, aU, tU1, aV, tV0) }
				: new float[][] { point(aOrigin, aU, tU0, aV, tV0), point(aOrigin, aU, tU1, aV, tV0),
						point(aOrigin, aU, tU1, aV, tV1), point(aOrigin, aU, tU0, aV, tV1) };
		float[][] tUVs = tFlip
				? new float[][] { { 0, 1 }, { 0, 0 }, { 1, 0 }, { 1, 1 } }
				: new float[][] { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 0, 0 } };
		VertexConsumer tBuffer = aBuffers.getBuffer(
				RenderType.text(new ResourceLocation(GTRenderModelListener.MOD_ID, aIcon.texturePath)));
		for (int tCorner = 0; tCorner < 4; tCorner++) {
			float[] tP = tQuads[tCorner];
			// single-arg .color(int) is NOT in the swap table (p22 narrowing): regex cannot
			// tell the VertexConsumer packed-colour form from Jade's ProgressStyle.color(int),
			// so this one true positive forks inline — canonical shape GT6CircuitsTest:57
			tBuffer.vertex(tPose.pose(), tP[0], tP[1], tP[2])
					//? if forge {
					.color(tColor)
					//?} else {
					/*.setColor(tColor)
					*///?}
					.uv(tUVs[tCorner][0], tUVs[tCorner][1])
					.uv2(LightTexture.FULL_BRIGHT)
					.endVertex();
		}
	}

	// ---------------------------------------------------------------------------
	// face-plane geometry (world axes straight from the tables — no rotation chain)
	// ---------------------------------------------------------------------------

	/**
	 * The corner of the face plane on the clicked side, lifted FACE_OFFSET outward:
	 * the normal component carries the plane coordinate (1 on positive faces, 0 on
	 * negative ones) plus the lift along the face normal; the tangent components stay 0.
	 */
	private static float[] faceOrigin(BlockPos aPos, byte aFace) {
		Vec3i tNormal = Direction.from3DDataValue(aFace).getNormal();
		float tPlane = Direction.from3DDataValue(aFace).getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F : 0.0F;
		return new float[] {
				aPos.getX() + tNormal.getX() * (tPlane + FACE_OFFSET),
				aPos.getY() + tNormal.getY() * (tPlane + FACE_OFFSET),
				aPos.getZ() + tNormal.getZ() * (tPlane + FACE_OFFSET)
		};
	}

	/** The unit vector of an in-face axis id ({@code 0:X, 1:Y, 2:Z}). */
	private static float[] axisVector(byte aAxis) {
		switch (aAxis) {
		case 0: return new float[] { 1, 0, 0 };
		case 1: return new float[] { 0, 1, 0 };
		case 2: return new float[] { 0, 0, 1 };
		}
		throw new IllegalArgumentException("axis " + aAxis + " is not 0..2");
	}

	/** origin + u*uc + v*vc — the grid point on the face plane. */
	private static float[] point(float[] aOrigin, float[] aU, float aUc, float[] aV, float aVc) {
		return new float[] {
				aOrigin[0] + aU[0] * aUc + aV[0] * aVc,
				aOrigin[1] + aU[1] * aUc + aV[1] * aVc,
				aOrigin[2] + aU[2] * aUc + aV[2] * aVc
		};
	}
}
