package gregtech6.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import gregtech6.multiblock.GTMultiBlockPattern;

/**
 * The multiblock structure ghost preview (task p10-ghost-preview-poc POC; task
 * p12-ghost-pattern-api lifted to the declarative API): drawn per frame from the
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
 * this constructively, and the vertex-stream test pins this renderer's emission to the
 * POC's literal draw, frame-equal.
 *
 * <p><b>Draw mode ruling (FORMED — unchanged from the POC):</b> formed structure →
 * only the 12 outer edges of the pattern's bounding shell; unformed → one cell
 * wireframe per pattern cell in declaration order (parts in the wrench-grid pulse
 * blue, hollow markers in flat grey — the "keep this hollow" marker). Rationale: part
 * wireframes over already-real blocks is pure occlusion noise, but skipping entirely
 * would waste the cheapest "this is a formed multiblock" affordance — the shell frame
 * marks the machine without hiding it. Both modes read nothing beyond the BlockState
 * the listener already holds ({@code FORMED}, TileEntityBase10MultiBlockBase.java:58)
 * plus the pattern. The shell is derived from the pattern bounds
 * ({@code min - 0.5 .. max + 0.5}) — for the 3x3x3 Coke Oven shell that is the POC's
 * centre ± 1.5 box, bit-equal.
 *
 * <p><b>Frame plumbing (the P5 verified path):</b> the event pose stack is the
 * renderLevel frame pose (world-axis origin), so the draw translates by
 * {@code -cameraPos} first — the same idiom as vanilla {@code renderHitOutline}
 * (LevelRenderer.java:2346-2348, {@code blockPos - camera}) and
 * {@link GTWrenchGridRenderer} :85-86. The event's
 * {@link MultiBufferSource} IS the frame's {@code renderBuffers.bufferSource()}
 * (vanilla LevelRenderer.java:1187), so lines emitted here ride the guaranteed
 * same-frame flush ({@code endLastBatch} :1322, {@code endBatch(RenderType.lines())}
 * + generic {@code endBatch()} :1359-1360) — no cross-frame leak, zero caching,
 * zero network, zero datagen. The event never fires without a block hit and is
 * dispatched only there (forge patch LevelRenderer.java.patch:121-132,
 * ForgeHooksClient.java:270-282).
 *
 * <p><b>Not on this card (the following ghost cards):</b> translucent pattern faces,
 * green/red per-cell match colouring (its per-cell lookup is
 * {@code Cell.matches(worldState)}), JEI/REI preview GUI. Still cut pool: persistent
 * holograms (a {@code RenderLevelStageEvent} category needing its own ADR — P11+
 * pool), Sodium shader-pipeline compatibility (this path never touches the
 * chunk-baking pipeline, so the GTCEu-style SodiumCompat surface does not exist here),
 * ghost textures (pure colour lines suffice).
 *
 * <p>The facing byte is the vanilla {@code get3DDataValue()} of the BlockState
 * {@code HORIZONTAL_FACING} — the client display authority (the BE's own {@code mFacing}
 * can be stale on the client, the GTWrenchHighlightListener.java:73-76 precedent), and
 * its numbering matches the GT6 side byte ({@code DOWN UP NORTH SOUTH WEST EAST} =
 * TileEntityBase10MultiBlockBase.java:61 horizontal restriction 2..5).
 */
@OnlyIn(Dist.CLIENT)
public final class GTMultiBlockPreviewRenderer {

	/** The flat grey of the hollow-marker cell (the "keep this hollow" centre, POC COLOR_AIR). */
	private static final float COLOR_AIR = 0.5F;

	private GTMultiBlockPreviewRenderer() {
	}

	/**
	 * Draws one frame of the ghost. All state arrives by value from the listener this
	 * very frame: the controller position (the hovered block), the declared pattern
	 * (null = nothing to draw), the facing byte and the formed bit read off the
	 * BlockState (the client display authority), the event's world-frame pose stack and
	 * its frame buffer source.
	 */
	public static void renderPreview(PoseStack aPoseStack, MultiBufferSource aBuffers, Camera aCamera,
			BlockPos aControllerPos, GTMultiBlockPattern aPattern, byte aFacing, boolean aFormed) {
		if (aPattern == null) return;
		int[] tAnchor = GTMultiBlockPattern.anchorOffset(aFacing);
		int tCx = aControllerPos.getX() + tAnchor[0];
		int tCy = aControllerPos.getY() + tAnchor[1];
		int tCz = aControllerPos.getZ() + tAnchor[2];

		Vec3 tCamPos = aCamera.getPosition();
		aPoseStack.pushPose();
		aPoseStack.translate(-tCamPos.x, -tCamPos.y, -tCamPos.z);

		VertexConsumer tBuffer = aBuffers.getBuffer(RenderType.lines());
		RenderSystem.lineWidth(3);
		emitPattern(aPoseStack.last(), tBuffer, tCx, tCy, tCz, aPattern, aFormed, pulseRG());

		aPoseStack.popPose();
	}

	/**
	 * The pure per-frame emission — the POC draw loop with the hardcoded table swapped
	 * for the pattern's cells, in declaration order (the Coke Oven declares its 26
	 * bricks in the upstream loop order, hollow centre appended last, so the emission
	 * order is the POC's). Package-visible so the offline vertex-stream test can pin it
	 * against the POC's literal draw. {@code aCx/aCy/aCz} = the structure centre in
	 * world space (controller + anchor); {@code aRG} = the pulse colour.
	 */
	static void emitPattern(PoseStack.Pose aPose, VertexConsumer aBuffer, int aCx, int aCy, int aCz,
			GTMultiBlockPattern aPattern, boolean aFormed, float aRG) {
		if (aFormed) {
			// the 12 outer edges of the pattern's bounding shell — the formed ruling (class javadoc);
			// min - 0.5 .. max + 0.5 is bit-equal to the POC's centre ± 1.5 for the 3x3x3 shell
			drawBoxEdges(aPose, aBuffer,
					aCx + aPattern.minX() - 0.5F, aCy + aPattern.minY() - 0.5F, aCz + aPattern.minZ() - 0.5F,
					aCx + aPattern.maxX() + 0.5F, aCy + aPattern.maxY() + 0.5F, aCz + aPattern.maxZ() + 0.5F,
					aRG, 1.0F);
		} else {
			for (GTMultiBlockPattern.Cell tCell : aPattern.cells()) {
				float tRG = tCell.isHollow() ? COLOR_AIR : aRG;
				float tB = tCell.isHollow() ? COLOR_AIR : 1.0F;
				drawBoxEdges(aPose, aBuffer,
						aCx + tCell.x - 0.5F, aCy + tCell.y - 0.5F, aCz + tCell.z - 0.5F,
						aCx + tCell.x + 0.5F, aCy + tCell.y + 0.5F, aCz + tCell.z + 0.5F,
						tRG, tB);
			}
		}
	}

	/** The wrench-grid pulse (GTWrenchGridRenderer:139 — GTCEu :163-164 verbatim). */
	private static float pulseRG() {
		return 0.2F + (float)Math.sin((System.currentTimeMillis() % (Mth.PI * 800)) / 800) / 2;
	}

	/**
	 * Emits the 12 edges of the axis-aligned box spanning (aMinX..aMaxX, aMinY..aMaxY,
	 * aMinZ..aMaxZ) — the drawLine emission is the GTWrenchGridRenderer:152-162 idiom
	 * verbatim (vertex/normal per endpoint, opaque alpha; LINES takes the colour
	 * straight), the edge order the POC's bottom-4/top-4/vertical-4.
	 */
	private static void drawBoxEdges(PoseStack.Pose aPose, VertexConsumer aBuffer,
			float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ, float aRG, float aB) {
		// the 4 bottom edges
		drawLine(aPose, aBuffer, aMinX, aMinY, aMinZ, aMaxX, aMinY, aMinZ, aRG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMinY, aMinZ, aMaxX, aMinY, aMaxZ, aRG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMinY, aMaxZ, aMinX, aMinY, aMaxZ, aRG, aB);
		drawLine(aPose, aBuffer, aMinX, aMinY, aMaxZ, aMinX, aMinY, aMinZ, aRG, aB);
		// the 4 top edges
		drawLine(aPose, aBuffer, aMinX, aMaxY, aMinZ, aMaxX, aMaxY, aMinZ, aRG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMaxY, aMinZ, aMaxX, aMaxY, aMaxZ, aRG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMaxY, aMaxZ, aMinX, aMaxY, aMaxZ, aRG, aB);
		drawLine(aPose, aBuffer, aMinX, aMaxY, aMaxZ, aMinX, aMaxY, aMinZ, aRG, aB);
		// the 4 vertical edges
		drawLine(aPose, aBuffer, aMinX, aMinY, aMinZ, aMinX, aMaxY, aMinZ, aRG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMinY, aMinZ, aMaxX, aMaxY, aMinZ, aRG, aB);
		drawLine(aPose, aBuffer, aMaxX, aMinY, aMaxZ, aMaxX, aMaxY, aMaxZ, aRG, aB);
		drawLine(aPose, aBuffer, aMinX, aMinY, aMaxZ, aMinX, aMaxY, aMaxZ, aRG, aB);
	}

	/** One world-space edge — GTWrenchGridRenderer:152-162 verbatim (decomposed coordinates). */
	private static void drawLine(PoseStack.Pose aPose, VertexConsumer aBuffer,
			float aX0, float aY0, float aZ0, float aX1, float aY1, float aZ1, float aRG, float aB) {
		float tNx = aX1 - aX0, tNy = aY1 - aY0, tNz = aZ1 - aZ0;
		aBuffer.vertex(aPose.pose(), aX0, aY0, aZ0)
				.color(aRG, aRG, aB, 1.0F)
				.normal(aPose.normal(), tNx, tNy, tNz)
				.endVertex();
		aBuffer.vertex(aPose.pose(), aX1, aY1, aZ1)
				.color(aRG, aRG, aB, 1.0F)
				.normal(aPose.normal(), tNx, tNy, tNz)
				.endVertex();
	}
}
