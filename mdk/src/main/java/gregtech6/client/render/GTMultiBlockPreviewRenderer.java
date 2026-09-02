package gregtech6.client.render;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import gregtech6.multiblock.GTMultiBlockPattern;

/**
 * The multiblock structure ghost preview (task p10-ghost-preview-poc POC; task
 * p12-ghost-pattern-api lifted to the declarative API; task p12-ghost-render-match
 * upgraded to per-cell translucent faces with green/red match colouring, ADR
 * 2026-09-02-p12-ghost-render-translucent): drawn per frame from the
 * {@code RenderHighlightEvent.Block} parameters while a wrench (hoe substitute) hovers
 * a controller, exactly like the wrench grid of {@link GTWrenchGridRenderer} — the same
 * transient input-feedback exception of ADR 2026-08-30-p5-wrench-ui, its three
 * constraints honored structurally: zero static BE/Level references (everything arrives
 * per frame by value), zero writes, no event cancellation (the vanilla selection box
 * stays).
 *
 * <p><b>The pattern arrives from the controller binding, never from
 * {@code checkStructure2}.</b> The listener hands in whatever
 * {@code getStructurePattern()} declares (null = nothing drawn). The pattern is the
 * declarative {@link GTMultiBlockPattern} — pure display data whose cells transcribe
 * the upstream check's loop without ever running it: the check writes the world (the
 * centre-cell {@code removeBlock}, TileEntityCokeOven.java:100-101) and embeds the
 * builder-wand auto-place, both deliberately outside the pattern API. The facing
 * dependence enters only through the structure-centre anchor {@code -OFF[mFacing]} —
 * the pure {@code getOffsetXN/YN/ZN} arithmetic (TileEntityBase01Root.java:174-176
 * tables, :194-206 accessors), generalized off this class into
 * {@link GTMultiBlockPattern#anchorOffset(byte)}. The upstream loop adds
 * {@code i, j, k} in WORLD axes (the Coke Oven shape is axis-aligned for every
 * facing), so no additional per-cell rotation exists to invent: the facing rotates the
 * centre, the cells stay world-axis — the offline four-facing full-table test pins
 * this constructively, and the formed-shell vertex-stream test still pins the shell
 * draw to the POC's literal draw, frame-equal.
 *
 * <p><b>Draw modes (ADR 2026-09-02-p12-ghost-render-translucent).</b> UNFORMED — one
 * ghost cell per pattern cell in declaration order, classified per frame by
 * {@link GTMultiBlockGhostMatcher} (pure function over the pattern + the listener's
 * Level): a matched part paints a translucent GREEN face cube, a missing-or-wrong one
 * a translucent RED face cube (the Litematica wrongBlock calibre, face alpha 0.3,
 * static colours — no pulse), each with an opaque edge wire of the same colour; a
 * matched hollow marker (the Coke Oven centre) paints nothing — a non-air centre
 * classifies RED, which is exactly the upstream check's failure rule
 * (TileEntityCokeOven.java:99-104). FORMED — unchanged from the POC: only the 12 outer
 * edges of the pattern's bounding shell in the wrench-grid pulse blue (the shell is
 * derived from the pattern bounds, {@code min - 0.5 .. max + 0.5} — for the 3x3x3 Coke
 * Oven shell that is the POC's centre ± 1.5 box, bit-equal; the offline shell test
 * still pins it vertex-for-vertex).
 *
 * <p><b>Render-type selection (pinned by the card, zero new RenderType).</b> The faces
 * ride vanilla {@link RenderType#debugQuads()} verbatim (RenderType.java:681-693:
 * POSITION_COLOR / QUADS / TRANSLUCENT transparency / NO_CULL / sort-on-upload) —
 * translucent first per the ruling, the self-overlap sort artefacts accepted (the P10
 * verdict); if a real-world case ever turns dirty, the written fallback is a cutout
 * downgrade. The edge wires ride {@link RenderType#lines()} (the GTWrenchGridRenderer
 * idiom). No {@code RenderSystem}/{@code Tesselator} direct issuance beyond the
 * line-width knob the POC already turns (the world-border direct-draw internals,
 * LevelRenderer.java:2265/:2328, are the anti-pattern, not the precedent). Coplanar
 * z-fighting is dodged by vertex inset ({@link #FACE_INSET}, the
 * WorldGenAttemptRenderer :31-51 centre-symmetric shrink idiom) — NOT by
 * polygonOffset: {@code debugQuads} is a frozen vanilla composite and attaching an
 * offset to it would mean a new RenderType, which this card forbids; the inset keeps
 * the face inside the block bounds (0.002 of 1.0, sub-pixel at any playable distance)
 * while clearing the coplanar band.
 *
 * <p><b>Frame plumbing (the P5 verified path):</b> the event pose stack is the
 * renderLevel frame pose (world-axis origin), so the draw translates by
 * {@code -cameraPos} first — the same idiom as vanilla {@code renderHitOutline}
 * (LevelRenderer.java:2346-2348, {@code blockPos - camera}) and
 * {@link GTWrenchGridRenderer} :85-86. The event's
 * {@link MultiBufferSource} IS the frame's {@code renderBuffers.bufferSource()}
 * (vanilla LevelRenderer.java:1187), so quads and lines emitted here ride the
 * guaranteed same-frame flush ({@code endLastBatch} :1322,
 * {@code endBatch(RenderType.lines())} + generic {@code endBatch()} :1359-1360) — no
 * cross-frame leak, zero caching, zero network, zero datagen. The event never fires
 * without a block hit and is dispatched only there (forge patch
 * LevelRenderer.java.patch:121-132, ForgeHooksClient.java:270-282). Fusion-grade
 * patterns (~600-1000 cells) would want a cached mesh on top of this per-frame
 * immediate emission — a future card, javadoc-noted per the task boundary.
 *
 * <p><b>Not on this card (the following ghost cards):</b> JEI/REI preview GUI. Still
 * cut pool: persistent holograms (a {@code RenderLevelStageEvent} category needing its
 * own ADR — P11+ pool), the property-level "wrong state" yellow grade (cut pool until
 * the first property-level structure lands), Sodium shader-pipeline compatibility
 * (this path never touches the chunk-baking pipeline, so the GTCEu-style SodiumCompat
 * surface does not exist here), ghost textures (pure colour quads suffice).
 *
 * <p>The facing byte is the vanilla {@code get3DDataValue()} of the BlockState
 * {@code HORIZONTAL_FACING} — the client display authority (the BE's own {@code mFacing}
 * can be stale on the client, the GTWrenchHighlightListener.java:73-76 precedent), and
 * its numbering matches the GT6 side byte ({@code DOWN UP NORTH SOUTH WEST EAST} =
 * TileEntityBase10MultiBlockBase.java:61 horizontal restriction 2..5).
 */
@OnlyIn(Dist.CLIENT)
public final class GTMultiBlockPreviewRenderer {

	/** The face alpha — the Litematica wrongBlock calibre (#4CFF3333 ≙ alpha 0.30). */
	private static final float FACE_ALPHA = 0.3F;
	/** The matched-part green (the Litematica-family green, static — no pulse). */
	private static final float GREEN_R = 0.2F, GREEN_G = 1.0F, GREEN_B = 0.2F;
	/** The missing-or-wrong red (the Litematica wrongBlock family, static — no pulse). */
	private static final float RED_R = 1.0F, RED_G = 0.2F, RED_B = 0.2F;
	/**
	 * The coplanar z-fight inset: ghost faces shrink from the block bounds by this much
	 * per side — the WorldGenAttemptRenderer :31-51 centre-symmetric shrink idiom, the
	 * card's primary ruling (polygonOffset would need a new RenderType, see the class
	 * javadoc). 0.002 of a 1.0 block is sub-pixel at any playable distance.
	 */
	private static final float FACE_INSET = 0.002F;

	private GTMultiBlockPreviewRenderer() {
	}

	/**
	 * Draws one frame of the ghost. All state arrives by value from the listener this
	 * very frame: the controller position (the hovered block), the declared pattern
	 * (null = nothing to draw), the facing byte and the formed bit read off the
	 * BlockState (the client display authority), the Level for the per-cell match
	 * reads (unformed only), the event's world-frame pose stack and its frame buffer
	 * source.
	 */
	public static void renderPreview(PoseStack aPoseStack, MultiBufferSource aBuffers, Camera aCamera, Level aLevel,
			BlockPos aControllerPos, GTMultiBlockPattern aPattern, byte aFacing, boolean aFormed) {
		if (aPattern == null) return;
		int[] tAnchor = GTMultiBlockPattern.anchorOffset(aFacing);
		int tCx = aControllerPos.getX() + tAnchor[0];
		int tCy = aControllerPos.getY() + tAnchor[1];
		int tCz = aControllerPos.getZ() + tAnchor[2];

		Vec3 tCamPos = aCamera.getPosition();
		aPoseStack.pushPose();
		aPoseStack.translate(-tCamPos.x, -tCamPos.y, -tCamPos.z);

		VertexConsumer tLines = aBuffers.getBuffer(RenderType.lines());
		RenderSystem.lineWidth(3);
		if (aFormed) {
			emitFormedShell(aPoseStack.last(), tLines, tCx, tCy, tCz, aPattern, pulseRG());
		} else {
			VertexConsumer tFaces = aBuffers.getBuffer(RenderType.debugQuads());
			emitUnformed(aPoseStack.last(), tFaces, tLines, aPattern,
					GTMultiBlockGhostMatcher.classify(aPattern, aLevel, aControllerPos, aFacing), tCx, tCy, tCz);
		}

		aPoseStack.popPose();
	}

	/**
	 * The FORMED emission — the POC draw verbatim: the 12 outer edges of the pattern's
	 * bounding shell in the wrench-grid pulse (class javadoc). Package-visible so the
	 * offline vertex-stream test can pin it against the POC's literal draw.
	 * {@code aCx/aCy/aCz} = the structure centre in world space (controller + anchor);
	 * {@code aRG} = the pulse colour.
	 */
	static void emitFormedShell(PoseStack.Pose aPose, VertexConsumer aBuffer, int aCx, int aCy, int aCz,
			GTMultiBlockPattern aPattern, float aRG) {
		// min - 0.5 .. max + 0.5 is bit-equal to the POC's centre ± 1.5 for the 3x3x3 shell
		drawBoxEdges(aPose, aBuffer,
				aCx + aPattern.minX() - 0.5F, aCy + aPattern.minY() - 0.5F, aCz + aPattern.minZ() - 0.5F,
				aCx + aPattern.maxX() + 0.5F, aCy + aPattern.maxY() + 0.5F, aCz + aPattern.maxZ() + 0.5F,
				aRG, aRG, 1.0F);
	}

	/**
	 * The UNFORMED emission — one ghost cell per pattern cell in declaration order,
	 * coloured by the verdict list (aligned with {@code pattern.cells()} by index, the
	 * classifier's output): GREEN/RED paint the translucent face cube plus the opaque
	 * same-colour edge wire, SKIP paints nothing (the matched hollow marker).
	 * Package-visible so the offline test can pin the stream. {@code aCx/aCy/aCz} = the
	 * structure centre in world space (controller + anchor).
	 */
	static void emitUnformed(PoseStack.Pose aPose, VertexConsumer aFaces, VertexConsumer aLines,
			GTMultiBlockPattern aPattern, List<GTMultiBlockGhostMatcher.Verdict> aVerdicts, int aCx, int aCy, int aCz) {
		List<GTMultiBlockPattern.Cell> tCells = aPattern.cells();
		for (int tIndex = 0; tIndex < tCells.size(); tIndex++) {
			GTMultiBlockGhostMatcher.Verdict tVerdict = aVerdicts.get(tIndex);
			if (tVerdict == GTMultiBlockGhostMatcher.Verdict.SKIP) continue;
			float tR = tVerdict == GTMultiBlockGhostMatcher.Verdict.GREEN ? GREEN_R : RED_R;
			float tG = tVerdict == GTMultiBlockGhostMatcher.Verdict.GREEN ? GREEN_G : RED_G;
			float tB = tVerdict == GTMultiBlockGhostMatcher.Verdict.GREEN ? GREEN_B : RED_B;
			GTMultiBlockPattern.Cell tCell = tCells.get(tIndex);
			emitGhostCell(aPose, aFaces, aLines, aCx + tCell.x, aCy + tCell.y, aCz + tCell.z, tR, tG, tB);
		}
	}

	/** One ghost cell: the inset translucent face cube plus the opaque same-colour edge wire, both at cell centre ± (0.5 - inset). */
	private static void emitGhostCell(PoseStack.Pose aPose, VertexConsumer aFaces, VertexConsumer aLines,
			float aCx, float aCy, float aCz, float aR, float aG, float aB) {
		float tX0 = aCx - 0.5F + FACE_INSET, tY0 = aCy - 0.5F + FACE_INSET, tZ0 = aCz - 0.5F + FACE_INSET;
		float tX1 = aCx + 0.5F - FACE_INSET, tY1 = aCy + 0.5F - FACE_INSET, tZ1 = aCz + 0.5F - FACE_INSET;
		drawCellFaces(aPose, aFaces, tX0, tY0, tZ0, tX1, tY1, tZ1, aR, aG, aB);
		drawBoxEdges(aPose, aLines, tX0, tY0, tZ0, tX1, tY1, tZ1, aR, aG, aB);
	}

	/** The wrench-grid pulse (GTWrenchGridRenderer:139 — GTCEu :163-164 verbatim). */
	private static float pulseRG() {
		return 0.2F + (float)Math.sin((System.currentTimeMillis() % (Mth.PI * 800)) / 800) / 2;
	}

	/**
	 * Emits the 6 faces of the axis-aligned box spanning (aMinX..aMaxX, aMinY..aMaxY,
	 * aMinZ..aMaxZ) onto {@link RenderType#debugQuads()} (POSITION_COLOR — vertex +
	 * colour, no normal/uv): one quad per face, NO_CULL so the winding is presentation
	 * only. The TRANSLUCENT transparency state takes the face alpha straight.
	 */
	private static void drawCellFaces(PoseStack.Pose aPose, VertexConsumer aBuffer,
			float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ, float aR, float aG, float aB) {
		// bottom / top
		drawQuad(aPose, aBuffer, aMinX, aMinY, aMinZ, aMaxX, aMinY, aMinZ, aMaxX, aMinY, aMaxZ, aMinX, aMinY, aMaxZ, aR, aG, aB);
		drawQuad(aPose, aBuffer, aMinX, aMaxY, aMinZ, aMaxX, aMaxY, aMinZ, aMaxX, aMaxY, aMaxZ, aMinX, aMaxY, aMaxZ, aR, aG, aB);
		// north / south
		drawQuad(aPose, aBuffer, aMinX, aMinY, aMinZ, aMaxX, aMinY, aMinZ, aMaxX, aMaxY, aMinZ, aMinX, aMaxY, aMinZ, aR, aG, aB);
		drawQuad(aPose, aBuffer, aMinX, aMinY, aMaxZ, aMaxX, aMinY, aMaxZ, aMaxX, aMaxY, aMaxZ, aMinX, aMaxY, aMaxZ, aR, aG, aB);
		// west / east
		drawQuad(aPose, aBuffer, aMinX, aMinY, aMinZ, aMinX, aMinY, aMaxZ, aMinX, aMaxY, aMaxZ, aMinX, aMaxY, aMinZ, aR, aG, aB);
		drawQuad(aPose, aBuffer, aMaxX, aMinY, aMinZ, aMaxX, aMinY, aMaxZ, aMaxX, aMaxY, aMaxZ, aMaxX, aMaxY, aMinZ, aR, aG, aB);
	}

	/** One POSITION_COLOR quad — debugQuads takes the colour straight, four vertices, no normal/uv/lightmap. */
	private static void drawQuad(PoseStack.Pose aPose, VertexConsumer aBuffer,
			float aX0, float aY0, float aZ0, float aX1, float aY1, float aZ1,
			float aX2, float aY2, float aZ2, float aX3, float aY3, float aZ3, float aR, float aG, float aB) {
		aBuffer.vertex(aPose.pose(), aX0, aY0, aZ0).color(aR, aG, aB, FACE_ALPHA).endVertex();
		aBuffer.vertex(aPose.pose(), aX1, aY1, aZ1).color(aR, aG, aB, FACE_ALPHA).endVertex();
		aBuffer.vertex(aPose.pose(), aX2, aY2, aZ2).color(aR, aG, aB, FACE_ALPHA).endVertex();
		aBuffer.vertex(aPose.pose(), aX3, aY3, aZ3).color(aR, aG, aB, FACE_ALPHA).endVertex();
	}

	/**
	 * Emits the 12 edges of the axis-aligned box spanning (aMinX..aMaxX, aMinY..aMaxY,
	 * aMinZ..aMaxZ) — the drawLine emission is the GTWrenchGridRenderer:152-162 idiom
	 * verbatim (vertex/normal per endpoint, opaque alpha; LINES takes the colour
	 * straight), the edge order the POC's bottom-4/top-4/vertical-4.
	 */
	private static void drawBoxEdges(PoseStack.Pose aPose, VertexConsumer aBuffer,
			float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ, float aR, float aG, float aB) {
		// the 4 bottom edges
		drawLine(aPose, aBuffer, aMinX, aMinY, aMinZ, aMaxX, aMinY, aMinZ, aR, aG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMinY, aMinZ, aMaxX, aMinY, aMaxZ, aR, aG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMinY, aMaxZ, aMinX, aMinY, aMaxZ, aR, aG, aB);
		drawLine(aPose, aBuffer, aMinX, aMinY, aMaxZ, aMinX, aMinY, aMinZ, aR, aG, aB);
		// the 4 top edges
		drawLine(aPose, aBuffer, aMinX, aMaxY, aMinZ, aMaxX, aMaxY, aMinZ, aR, aG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMaxY, aMinZ, aMaxX, aMaxY, aMaxZ, aR, aG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMaxY, aMaxZ, aMinX, aMaxY, aMaxZ, aR, aG, aB);
		drawLine(aPose, aBuffer, aMinX, aMaxY, aMaxZ, aMinX, aMaxY, aMinZ, aR, aG, aB);
		// the 4 vertical edges
		drawLine(aPose, aBuffer, aMinX, aMinY, aMinZ, aMinX, aMaxY, aMinZ, aR, aG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMinY, aMinZ, aMaxX, aMaxY, aMinZ, aR, aG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMinY, aMaxZ, aMaxX, aMaxY, aMaxZ, aR, aG, aB);
		drawLine(aPose, aBuffer, aMinX, aMinY, aMaxZ, aMinX, aMaxY, aMaxZ, aR, aG, aB);
	}

	/** One world-space edge — GTWrenchGridRenderer:152-162 verbatim (decomposed coordinates). */
	private static void drawLine(PoseStack.Pose aPose, VertexConsumer aBuffer,
			float aX0, float aY0, float aZ0, float aX1, float aY1, float aZ1, float aR, float aG, float aB) {
		float tNx = aX1 - aX0, tNy = aY1 - aY0, tNz = aZ1 - aZ0;
		aBuffer.vertex(aPose.pose(), aX0, aY0, aZ0)
				.color(aR, aG, aB, 1.0F)
				.normal(aPose.normal(), tNx, tNy, tNz)
				.endVertex();
		aBuffer.vertex(aPose.pose(), aX1, aY1, aZ1)
				.color(aR, aG, aB, 1.0F)
				.normal(aPose.normal(), tNx, tNy, tNz)
				.endVertex();
	}
}
