package gregtech6.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.client.model.data.ModelData;

/**
 * The pipe flow-arrow dynamic model (task pipe-flow-control spec ④, form reworked by
 * task pipe-flow-indicator-rework): the {@link PipeFlowSnapshot} in the {@code ModelData}
 * says which faces carry output arrows, and the arrow quads are appended to the
 * fallback's own quads (the {@link CoverPlateModel} shape).
 *
 * <p>THE FORM (the user ruling verbatim: 「指定流向后，在该方向的管道末端靠内、沿环绕该轴向的
 * 4 个侧面各画一枚指向流向的箭头贴片，位置需计算——保证其余面全部连接时，这 4 枚箭头仍全部
 * 可见」): for every marked face F the planner emits FOUR arrow patches on the F arm's own
 * side faces (the four faces whose normal ⊥ the F axis), each a square decal lying on the
 * arm's side plane (one {@link #ARROW_EPSILON} off it, the cover-overlay Z-fight posture)
 * — never a slab floating outside the block boundary (the retired pipe-output-arrows
 * end-cap form). The colour ruling: the blue placeholder lost to the GT output orange,
 * repainted directly into {@code gt6:block/pipe_flow_arrow} (path and atlas registration
 * untouched).
 *
 * <p>THE POSITION DERIVATION (why the patches are where they are): all rod arms share the
 * row cross-section — {@code [8 - d/2, 8 + d/2] px = [h, 1 - h] block} on every tangent
 * axis ({@code h = (16 - d)/32}, {@link GTRodBakedModel#armBox}). A perpendicular arm (a
 * connected neighbour) occupies exactly {@code [h, 1 - h]} along the F axis too, so the
 * part of the F arm's side face it can hide is the band {@code F ∈ [h, 1 - h]}; the
 * guaranteed-visible remainder is the OUTER SEGMENT {@code F ∈ [1 - h, 1]} (positive F)
 * / {@code [0, h]} (negative F). The patch must therefore sit entirely inside that outer
 * segment — constraint: {@code patch F-span ∩ [h, 1 - h] = ∅}. The patch edge length is
 * {@link #patchSize}{@code = min(h, 1 - 2h)} block: never wider than the arm face itself
 * ({@code 1 - 2h = d px}), never deeper than the outer segment ({@code h}), centred on the
 * arm cross-section (0.5), flush to the block face (the arm end, inside the boundary).
 * Consequences: d=4px → 4px patch, d=6 → 5px, d=8 → 4px, d=12 → 2px (the 「粗管径收窄但
 * 不小于 2px 量级」 floor), and d=16 (full-block rows, {@code h = 0}) has an EMPTY outer
 * segment — no patch can satisfy the visibility constraint, so none is emitted (the row
 * renders a full cube; there is no exposed arm side to paint).
 *
 * <p>THE DIRECTION: the sprite head sits at V=0; the canonical (rotation 0) FaceInfo UV
 * walk points it up on side faces, north on UP, south on DOWN (FaceInfo.java:19-42, the
 * FaceBakePins walk). {@link #arrowRotation} is the per-(F, patch face) {@link BlockFaceUV}
 * rotation table that turns each patch toward F (BlockFaceUV.java:40-42: rotation k maps
 * vertex i to corner (i+k)%4, so the sprite-top edge migrates one corner per +90°). All
 * 24 combinations are baked and direction-pinned offline.
 *
 * <p>Emission rules (the pipe-output-arrows pass shape, re-keyed to the patch faces):
 * <ul>
 * <li>pass side == a patch face S (⊥ F) → exactly the patches facing S, NOT culled
 *     (a solid neighbour across S must not swallow the decal — it floats an epsilon in
 *     front of the arm's own face);</li>
 * <li>pass side == null (unculled pass) → every patch of every marked face;</li>
 * <li>any other pass → nothing.</li>
 * </ul>
 * Opaque quads: the layers the pipe family bakes — solid or cutout (the r8-tex pipe
 * models declare cutout), plus the null all-layers pass.
 *
 * <p>Key (task cover-narrowing-render-snapshot): the arrows ride the dedicated
 * {@link GTModelProperties#FLOW_SNAPSHOT} — the generic {@code RENDER_SNAPSHOT} stayed
 * with the cover plate chain (the single-valued-property coexistence ruling), so a
 * covered pipe keeps its arrows and the two snapshots never invalidate each other.
 *
 * <p>RED LINE: reads ONLY the immutable snapshot — no BlockEntity is reachable from
 * here (render-thread semantics, GTDynamicBakedModel class doc). CLIENT-ONLY class:
 * instantiated exclusively INSIDE the composed chain ({@link GTFluidPipeFoamModel#over},
 * seated by GTRodClientListener — task pipe-flow-arrow-render-fix; the row diameter
 * rides the seat since pipe-output-arrows, taken from the rod params). The rod body's
 * {@code effectiveMask} still renders the marked face as a connected arm (kept by the
 * same ruling that keeps the grid's shift mode a connection read-out).
 */
public class GTFluidPipeFlowModel extends GTDynamicBakedModel {

	/** The Z-fighting epsilon of GTCEu StaticFaceBakery.COVER_OVERLAY (BLOCK.inflate(0.002)). */
	public static final double ARROW_EPSILON = 0.002;

	/** The arrow sprite — the datagen lands it as a single-file atlas source (GT6Atlases); repainted GT-output orange by pipe-flow-indicator-rework (path unchanged). */
	public static final ResourceLocation ARROW_SPRITE = ResourceLocation.fromNamespaceAndPath("gt6", "block/pipe_flow_arrow");

	/**
	 * The BlockFaceUV rotation (deg) turning the canonical arrow direction toward the
	 * marked face — rows = marked face F, cols = patch face S (both get3DDataValue order
	 * DOWN UP N S W E; -1 = S ∥ F, never a patch face). Derived from the FaceInfo corner
	 * walk + the BlockFaceUV.getShiftedIndex rotation (see class doc); every entry is
	 * pinned against a real baked quad in GTFluidPipeFlowModelTest.
	 */
	private static final int[][] ARROW_UV_ROTATION = {
		{  -1,  -1, 180, 180, 180, 180 }, // DOWN marks: the four side patches flip (canonical up-arrow)
		{  -1,  -1,   0,   0,   0,   0 }, // UP marks: the four side patches are canonical
		{ 180,   0,  -1,  -1, 270,  90 }, // NORTH marks: down 180, up 0, west 270, east 90
		{   0, 180,  -1,  -1,  90, 270 }, // SOUTH marks: down 0, up 180, west 90, east 270
		{ 270, 270,  90, 270,  -1,  -1 }, // WEST marks: down/up 270, north 90, south 270
		{  90,  90, 270,  90,  -1,  -1 }, // EAST marks: down/up 90, north 270, south 90
	};

	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;

	/** The half-inset of the arm cross-section this row's patches sit on — {@code (16 - diameterPx) / 32} clamped (the rod planner's {@code readFromNBT2 :64} floor). */
	private final float mHalf;

	private static final FaceBakery BAKERY = new FaceBakery();

	public GTFluidPipeFlowModel(BakedModel aFallbackModel, int aDiameterPx) {
		this(aFallbackModel, aDiameterPx, defaultSpriteLookup());
	}

	public GTFluidPipeFlowModel(BakedModel aFallbackModel, int aDiameterPx, Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup) {
		super(aFallbackModel);
		mSpriteLookup = aSpriteLookup;
		mHalf = halfOf(aDiameterPx);
	}

	/** The half-inset for a row diameter — the same clamp as the rod planner (2px floor, 16px ceiling). */
	public static float halfOf(int aDiameterPx) {
		float tDiameter = Math.max(GTRodBakedModel.MIN_DIAMETER_PX, Math.min(16.0F, aDiameterPx));
		return (16.0F - tDiameter) / 32.0F;
	}

	/** The half-inset this model's patches sit on (the seated row diameter). */
	public float half() {
		return mHalf;
	}

	/**
	 * The patch edge length in block units for an arm half-inset: {@code min(h, 1 - 2h)} —
	 * bounded by the outer-segment depth ({@code h}) and by the arm face width
	 * ({@code 1 - 2h = d px}); 0 on full-block rows (no exposed outer segment, see the
	 * class doc derivation).
	 */
	public static float patchSize(float aHalf) {
		return Math.min(aHalf, 1 - 2 * aHalf);
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(aSpriteId);
	}

	/**
	 * The chain pass-through gate (task pipe-flow-arrow-render-fix): the flow model sits
	 * INSIDE the composed Foam(Flow(rod)) chain, so its gate must admit everything the
	 * outer foam gate admits — a closed gate would route the base-class miss down the 3-arg
	 * fallback path and DROP the ModelData, leaving the rod body painting from EMPTY
	 * (a painted pipe spraying back to its material colour). The arrows themselves still
	 * key on FLOW_SNAPSHOT only (checked in getDynamicQuads).
	 */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return aModelData.has(GTModelProperties.FLOW_SNAPSHOT)
				|| aModelData.has(GTModelProperties.RENDER_SNAPSHOT)
				|| aModelData.has(GTModelProperties.FOAM_SNAPSHOT)
				|| aModelData.has(GTModelProperties.PAINT);
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		// the 5-ARG forward — the body is the rod model, whose paint tint reads the PAINT
		// property off this ModelData; the old 3-arg form (the pre-chain standalone
		// fallback) dropped it and arrowed pipes lost their spray (the 掉漆 bug)
		List<BakedQuad> rQuads = new ArrayList<>(getFallbackModel().getQuads(aState, aSide, aRand, aModelData, aRenderType));
		// opaque arrows: the layers the pipe family actually bakes. The r8-tex shared pipe
		// models declare render_type:cutout, so the chunk bake runs ONLY the cutout pass —
		// the solid-only gate of the pre-cutout era never baked an arrow at all (the
		// GTRodBakedModel:183 layer posture, solid+cutout)
		if (aRenderType != null && !aRenderType.equals(RenderType.solid()) && !aRenderType.equals(RenderType.cutout())) return rQuads;
		// the typed property hands the snapshot back directly — absent = null (the FoamModel form)
		PipeFlowSnapshot tSnapshot = aModelData.get(GTModelProperties.FLOW_SNAPSHOT);
		if (tSnapshot == null) return rQuads;

		for (FlowQuad tPlan : planQuads(tSnapshot, aSide, mHalf)) {
			TextureAtlasSprite tSprite = mSpriteLookup.apply(tPlan.sprite());
			if (tSprite == null) continue; // atlas gap: skip the quad instead of rendering garbage
			rQuads.add(bakeArrowQuad(tPlan, tSprite));
		}
		return rQuads;
	}

	// ---------------------------------------------------------------------------
	// planner (offline-testable: pure geometry over the snapshot, no sprite needed)
	// ---------------------------------------------------------------------------

	/**
	 * One planned arrow patch: the quad's facing (NOT a cullface — patches never cull),
	 * the carrier box in 0..1 block space (only the {@code quadFace} side of it is baked),
	 * the sprite id and the {@link BlockFaceUV} rotation pointing the sprite head at the
	 * marked face.
	 */
	public record FlowQuad(Direction quadFace, double[] box, ResourceLocation sprite, int uvRotation) {

		/** Box copy guard — the planner builds fresh arrays per quad. */
		public FlowQuad {
			box = box.clone();
		}
	}

	/** The emission rules of the class doc, as pure geometry. */
	public static List<FlowQuad> planQuads(PipeFlowSnapshot aSnapshot, @Nullable Direction aSide, float aHalf) {
		List<FlowQuad> rPlans = new ArrayList<>();
		float tSize = patchSize(aHalf);
		if (tSize <= 0) return rPlans; // full-block rows: the outer segment is empty, nothing satisfies the visibility ruling
		for (Direction tFace : Direction.values()) {
			if (!aSnapshot.hasArrow(tFace)) continue;
			for (Direction tPatchFace : Direction.values()) {
				if (tPatchFace.getAxis() == tFace.getAxis()) continue; // the four faces around the F axis
				if (aSide != null && aSide != tPatchFace) continue; // the pass bucketing
				rPlans.add(new FlowQuad(tPatchFace, patchOf(tFace, tPatchFace, aHalf, tSize), ARROW_SPRITE, arrowRotation(tFace, tPatchFace)));
			}
		}
		return rPlans;
	}

	/**
	 * The {@link #arrowRotation} lookup (deg) — see the table for the derivation; pinned
	 * per entry against a real baked quad in GTFluidPipeFlowModelTest.
	 */
	public static int arrowRotation(Direction aMarkedFace, Direction aPatchFace) {
		return ARROW_UV_ROTATION[aMarkedFace.get3DDataValue()][aPatchFace.get3DDataValue()];
	}

	/**
	 * The patch carrier box for a patch of the F arm's {@code aPatchFace} side, in 0..1
	 * space, the {@code markerOf} INTERLEAVED layout {@code [x0, y0, z0, x1, y1, z1]} the
	 * baker reads as from/to (the class doc derivation): the F axis carries the
	 * outer-segment span flush to the block face ({@code [1 - s, 1]} positive /
	 * {@code [0, s]} negative), the patch normal axis carries the arm face ±
	 * {@link #ARROW_EPSILON} slab (the decal plane), and the remaining tangent axis is
	 * centred on the arm cross-section ({@code [0.5 - s/2, 0.5 + s/2]}). Only the
	 * {@code aPatchFace} side of the box is baked — the slab is a carrier, the quad sits
	 * ON the arm's side plane.
	 */
	public static double[] patchOf(Direction aFace, Direction aPatchFace, float aHalf, float aSize) {
		double e = ARROW_EPSILON, s = aSize;
		double[] rBox = new double[6];
		for (Direction.Axis tAxis : Direction.Axis.values()) {
			int o = tAxis.ordinal();
			boolean tIsPatchAxis = tAxis == aPatchFace.getAxis();
			boolean tIsFaceAxis = tAxis == aFace.getAxis();
			Direction tSpanFace = tIsPatchAxis ? aPatchFace : aFace;
			boolean tPositive = tSpanFace.getAxisDirection() == Direction.AxisDirection.POSITIVE;
			if (tIsPatchAxis) {
				rBox[o] = tPositive ? 1 - aHalf : aHalf - e;
				rBox[o + 3] = tPositive ? 1 - aHalf + e : aHalf;
			} else if (tIsFaceAxis) {
				rBox[o] = tPositive ? 1 - s : 0;
				rBox[o + 3] = tPositive ? 1 : s;
			} else {
				rBox[o] = 0.5 - s / 2;
				rBox[o + 3] = 0.5 + s / 2;
			}
		}
		return rBox;
	}

	// ---------------------------------------------------------------------------
	// baker (client-only; the CoverPlateModel recipe over FaceBakery)
	// ---------------------------------------------------------------------------

	private BakedQuad bakeArrowQuad(FlowQuad aPlan, TextureAtlasSprite aSprite) {
		double[] tBox = aPlan.box();
		// FaceBakery works in model space (0..16). The UV is the canonical full-face form
		// [u0,v0,u1,v1] = [0,0,16,16] with the plan's rotation (the uvof-private-copies
		// fix, the issue #27 GTOreBakedModel ruling — see FaceBakePins for the vanilla
		// walk the rotation composes with). cull = null — the patch never culls (spec ④).
		Vector3f tFrom = new Vector3f((float) tBox[0] * 16, (float) tBox[1] * 16, (float) tBox[2] * 16);
		Vector3f tTo = new Vector3f((float) tBox[3] * 16, (float) tBox[4] * 16, (float) tBox[5] * 16);
		return BAKERY.bakeQuad(tFrom, tTo,
				new BlockElementFace(null, 0, aPlan.sprite().toString(), new BlockFaceUV(new float[] {0, 0, 16, 16}, aPlan.uvRotation())),
				aSprite, aPlan.quadFace(), BlockModelRotation.X0_Y0, null, true, aPlan.sprite());
	}
}
