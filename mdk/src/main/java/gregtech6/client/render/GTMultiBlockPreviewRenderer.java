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

/**
 * The multiblock structure ghost preview (task p10-ghost-preview-poc — the Coke Oven
 * minimal closed loop): drawn per frame from the {@code RenderHighlightEvent.Block}
 * parameters while a wrench (hoe substitute) hovers a controller, exactly like the
 * wrench grid of {@link GTWrenchGridRenderer} — the same transient input-feedback
 * exception of ADR 2026-08-30-p5-wrench-ui, its three constraints honored structurally:
 * zero static BE/Level references (everything arrives per frame by value), zero writes,
 * no event cancellation (the vanilla selection box stays).
 *
 * <p><b>The pattern is a hardcoded pure table, never {@code checkStructure2}.</b> The
 * upstream/port structure check carries a world write in its centre cell ({@code
 * getLevel().removeBlock(...)} — TileEntityCokeOven.java:100-101, the upstream
 * setBlockToAir pair), so running it on the client to "ask for the shape" is forbidden.
 * The table below is the 26 canonical loop cells of that same check
 * (TileEntityCokeOven.java:97-111: {@code for i, j, k in -1..1} minus the air centre),
 * written out literally; the facing dependence enters only through the structure-centre
 * offset {@code -OFF[mFacing]} — the pure {@code getOffsetXN/YN/ZN} arithmetic
 * (TileEntityBase01Root.java:174-176 tables, :194-206 accessors) mirrored in
 * {@link #cellOffset}. The upstream loop adds {@code i, j, k} in WORLD axes (the Coke
 * Oven shape is axis-aligned for every facing), so no additional per-cell rotation
 * exists to invent: the facing rotates the centre, the cells stay world-axis — the
 * offline four-facing full-table test pins this constructively.
 *
 * <p><b>Draw mode ruling (FORMED):</b> formed structure → only the 12 outer edges of
 * the 3x3x3 shell; unformed → all 27 cell wireframes (26 bricks in the wrench-grid
 * pulse blue + the centre air cell in flat grey, the "keep this hollow" marker).
 * Rationale: 26 brick wireframes over already-real blocks is pure occlusion noise, but
 * skipping entirely would waste the cheapest "this is a formed multiblock" affordance —
 * the shell frame marks the machine without hiding it. Both modes read nothing beyond
 * the BlockState the listener already holds ({@code FORMED},
 * TileEntityBase10MultiBlockBase.java:58).
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
 * <p><b>Cut pool (deliberately not implemented here):</b> persistent holograms (a
 * {@code RenderLevelStageEvent} category needing its own ADR — P11+ pool), partial-match
 * green/red cell colouring, JEI/REI preview GUI, Sodium shader-pipeline compatibility
 * (this path never touches the chunk-baking pipeline, so the GTCEu-style SodiumCompat
 * surface does not exist here), ghost textures (pure colour lines suffice for the POC).
 *
 * <p>The facing byte is the vanilla {@code get3DDataValue()} of the BlockState
 * {@code HORIZONTAL_FACING} — the client display authority (the BE's own {@code mFacing}
 * can be stale on the client, the GTWrenchHighlightListener.java:73-76 precedent), and
 * its numbering matches the GT6 side byte ({@code DOWN UP NORTH SOUTH WEST EAST} =
 * TileEntityBase10MultiBlockBase.java:61 horizontal restriction 2..5).
 */
@OnlyIn(Dist.CLIENT)
public final class GTMultiBlockPreviewRenderer {

	/**
	 * The 26 canonical pattern cells — the literal {@code (i, j, k)} loop body of
	 * TileEntityCokeOven.java:97-111 minus the {@code (0, 0, 0)} air centre, in the
	 * upstream loop order (i outer, j middle, k inner). World-axis coordinates relative
	 * to the structure centre; the facing rotation happens in {@link #cellOffset}.
	 * NEVER derive this table by running {@code checkStructure2} — it writes the world
	 * (the centre {@code removeBlock}, TileEntityCokeOven.java:100-101).
	 */
	public static final int[][] PATTERN_CELLS = {
			// i = -1
			{ -1, -1, -1 }, { -1, -1, 0 }, { -1, -1, 1 },
			{ -1, 0, -1 }, { -1, 0, 0 }, { -1, 0, 1 },
			{ -1, 1, -1 }, { -1, 1, 0 }, { -1, 1, 1 },
			// i = 0 — the (0, 0, 0) air centre is NOT a brick cell
			{ 0, -1, -1 }, { 0, -1, 0 }, { 0, -1, 1 },
			{ 0, 0, -1 }, { 0, 0, 1 },
			{ 0, 1, -1 }, { 0, 1, 0 }, { 0, 1, 1 },
			// i = 1
			{ 1, -1, -1 }, { 1, -1, 0 }, { 1, -1, 1 },
			{ 1, 0, -1 }, { 1, 0, 0 }, { 1, 0, 1 },
			{ 1, 1, -1 }, { 1, 1, 0 }, { 1, 1, 1 }
	};

	/**
	 * The GT6 side-offset tables mirrored from TileEntityBase01Root.java:174-176 (the
	 * {@code OFFX/OFFY/OFFZ} of {@code getOffsetXN/YN/ZN}, :194-206) — the specification
	 * the offline test asserts against. Index = the side byte / {@code get3DDataValue()}.
	 */
	private static final int[] OFF_X = { 0, 0, 0, 0, -1, 1 };
	private static final int[] OFF_Y = { 0, 1, 0, 0, 0, 0 };
	private static final int[] OFF_Z = { 0, 0, -1, 1, 0, 0 };

	/** The pulse is the wrench-grid one (GTCEu :163-164 verbatim via GTWrenchGridRenderer:139-140, ~2.5 s). */
	private static final float COLOR_AIR = 0.5F; // the flat grey of the centre air cell

	private GTMultiBlockPreviewRenderer() {
	}

	/**
	 * The pure facing rotation — the world offset of one canonical pattern cell
	 * {@code (i, j, k)} relative to the CONTROLLER position, for a controller facing
	 * {@code aFacing} (a {@code get3DDataValue()} byte): the upstream
	 * {@code tX + i, tY + j, tZ + k} arithmetic (TileEntityCokeOven.java:92/:97-111)
	 * with {@code tX/tY/tZ = controller - OFF[facing]} (the getOffsetXN/YN/ZN pure
	 * mirror, TileEntityBase01Root.java:194-206). MC-free — the offline test drives it
	 * directly.
	 */
	public static int[] cellOffset(byte aFacing, int aI, int aJ, int aK) {
		return new int[] { aI - OFF_X[aFacing], aJ - OFF_Y[aFacing], aK - OFF_Z[aFacing] };
	}

	/**
	 * Draws one frame of the ghost. All state arrives by value from the listener this
	 * very frame: the controller position (the hovered block), the facing byte and the
	 * formed bit read off the BlockState (the client display authority), the event's
	 * world-frame pose stack and its frame buffer source.
	 */
	public static void renderPreview(PoseStack aPoseStack, MultiBufferSource aBuffers, Camera aCamera,
			BlockPos aControllerPos, byte aFacing, boolean aFormed) {
		int tCx = aControllerPos.getX() - OFF_X[aFacing];
		int tCy = aControllerPos.getY() - OFF_Y[aFacing];
		int tCz = aControllerPos.getZ() - OFF_Z[aFacing];

		Vec3 tCamPos = aCamera.getPosition();
		aPoseStack.pushPose();
		aPoseStack.translate(-tCamPos.x, -tCamPos.y, -tCamPos.z);

		VertexConsumer tBuffer = aBuffers.getBuffer(RenderType.lines());
		RenderSystem.lineWidth(3);
		PoseStack.Pose tPose = aPoseStack.last();
		float tRG = pulseRG();
		if (aFormed) {
			// the 12 outer edges of the 3x3x3 shell — the formed ruling (class javadoc)
			drawBoxEdges(tPose, tBuffer, tCx, tCy, tCz, 1.5F, tRG, 1.0F);
		} else {
			for (int tCell = 0; tCell < PATTERN_CELLS.length; tCell++) {
				drawBoxEdges(tPose, tBuffer, tCx + PATTERN_CELLS[tCell][0], tCy + PATTERN_CELLS[tCell][1],
						tCz + PATTERN_CELLS[tCell][2], 0.5F, tRG, 1.0F);
			}
			// the centre air cell — the "keep this hollow" marker in flat grey
			drawBoxEdges(tPose, tBuffer, tCx, tCy, tCz, 0.5F, COLOR_AIR, COLOR_AIR);
		}

		aPoseStack.popPose();
	}

	/** The wrench-grid pulse (GTWrenchGridRenderer:139 — GTCEu :163-164 verbatim). */
	private static float pulseRG() {
		return 0.2F + (float)Math.sin((System.currentTimeMillis() % (Mth.PI * 800)) / 800) / 2;
	}

	/**
	 * Emits the 12 edges of the axis-aligned box centred at (aX, aY, aZ) with half-size
	 * aHalf — the drawLine emission is the GTWrenchGridRenderer:152-162 idiom verbatim
	 * (vertex/normal per endpoint, opaque alpha; LINES takes the colour straight).
	 */
	private static void drawBoxEdges(PoseStack.Pose aPose, VertexConsumer aBuffer,
			float aX, float aY, float aZ, float aHalf, float aRG, float aB) {
		float tX0 = aX - aHalf, tY0 = aY - aHalf, tZ0 = aZ - aHalf;
		float tX1 = aX + aHalf, tY1 = aY + aHalf, tZ1 = aZ + aHalf;
		// the 4 bottom edges
		drawLine(aPose, aBuffer, tX0, tY0, tZ0, tX1, tY0, tZ0, aRG, aB);
		drawLine(aPose, aBuffer, tX1, tY0, tZ0, tX1, tY0, tZ1, aRG, aB);
		drawLine(aPose, aBuffer, tX1, tY0, tZ1, tX0, tY0, tZ1, aRG, aB);
		drawLine(aPose, aBuffer, tX0, tY0, tZ1, tX0, tY0, tZ0, aRG, aB);
		// the 4 top edges
		drawLine(aPose, aBuffer, tX0, tY1, tZ0, tX1, tY1, tZ0, aRG, aB);
		drawLine(aPose, aBuffer, tX1, tY1, tZ0, tX1, tY1, tZ1, aRG, aB);
		drawLine(aPose, aBuffer, tX1, tY1, tZ1, tX0, tY1, tZ1, aRG, aB);
		drawLine(aPose, aBuffer, tX0, tY1, tZ1, tX0, tY1, tZ0, aRG, aB);
		// the 4 vertical edges
		drawLine(aPose, aBuffer, tX0, tY0, tZ0, tX0, tY1, tZ0, aRG, aB);
		drawLine(aPose, aBuffer, tX1, tY0, tZ0, tX1, tY1, tZ0, aRG, aB);
		drawLine(aPose, aBuffer, tX1, tY0, tZ1, tX1, tY1, tZ1, aRG, aB);
		drawLine(aPose, aBuffer, tX0, tY0, tZ1, tX0, tY1, tZ1, aRG, aB);
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
