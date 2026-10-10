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
 * The pipe flow-arrow dynamic model (task pipe-flow-control spec ④) — the second
 * consumer of the C-grade render foundation, the {@link CoverPlateModel} shape applied
 * to the fluid pipe: the {@link PipeFlowSnapshot} in the {@code ModelData} says which
 * faces carry output arrows, and the arrow quads are appended to the fallback's own
 * quads.
 *
 * <p>Task pipe-output-arrows (the user field report, port-native feature — upstream has
 * NO output-direction marking at all: the 1.7.10 "arrow" is the held-tool
 * {@code RenderHelper.drawWrenchOverlay} line cross, DrawBlockHighlight
 * TileEntityBase01Root.java:995-1005 + RenderHelper.java:98-123, a tool-state UI, and
 * the pipe {@code getTextureSide/getTextureConnected} render only the pipe art +
 * the optional PIPE_RESTRICTOR): the marker is a STATIC direction badge for the faces
 * the user shift-marked — never a flow animation. Two corrections rode the report:
 * <ul>
 * <li>SIZED TO THE ARM — the old marker was a FULL-FACE slab: on a thin pipe it floated
 *     as a large icon over/around the core art, at a per-face orientation that read as
 *     a random "rotating icon covering the connection texture". Now the box is the
 *     connected-arm cross-section (the seated row diameter) centred on the face, so the
 *     pipe body art stays visible around the badge;</li>
 * <li>THE BADGE SITS ON THE MOUTH — the slab is pushed fully OUTSIDE the block boundary
 *     (the outer plane {@link #ARROW_EPSILON} past the face, {@link #ARROW_THICKNESS}
 *     deep), reading as attached to the arm end cap; the arm form itself is the rod
 *     model's job ({@code GTRodBakedModel.effectiveMask} — a marked face renders the
 *     connected form).</li>
 * </ul>
 * Emission rules:
 * <ul>
 * <li>pass side == the marked face F → the arrow quad, NOT culled (a solid neighbour
 *     at the marked face must not swallow the arrow — it points INTO that neighbour);</li>
 * <li>pass side == null (unculled pass) → the same quad;</li>
 * <li>any other pass side → nothing (tangent passes never see the face plane).</li>
 * </ul>
 * Opaque quads: the layers the pipe family bakes — solid or cutout (the r8-tex pipe
 * models declare cutout), plus the null all-layers pass. The sprite is the static
 * light-blue {@code gt6:block/pipe_flow_arrow} (untinted); its head sits at the sprite
 * top (V=0), which the canonical UV walk plants at the face top on the four sides
 * (north on UP, south on DOWN) — deterministic per face, the face itself carries the
 * out-of-the-pipe direction.
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
 * rides the seat since pipe-output-arrows, taken from the rod params).
 */
public class GTFluidPipeFlowModel extends GTDynamicBakedModel {

	/** The Z-fighting epsilon of GTCEu StaticFaceBakery.COVER_OVERLAY (BLOCK.inflate(0.002)). */
	public static final double ARROW_EPSILON = 0.002;

	/** The arrow slab thickness (1px — the whole slab sits outside the block boundary). */
	public static final double ARROW_THICKNESS = 1.0 / 16.0;

	/** The arrow sprite — the datagen lands it as a single-file atlas source (GT6Atlases). */
	public static final ResourceLocation ARROW_SPRITE = ResourceLocation.fromNamespaceAndPath("gt6", "block/pipe_flow_arrow");

	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;

	/** The half-inset of the arm cross-section this row's badge is sized to — {@code (16 - diameterPx) / 32} clamped (the rod planner's {@code readFromNBT2 :64} floor). */
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

	/** The badge half-inset for a row diameter — the same clamp as the rod planner (2px floor, 16px ceiling). */
	public static float halfOf(int aDiameterPx) {
		float tDiameter = Math.max(GTRodBakedModel.MIN_DIAMETER_PX, Math.min(16.0F, aDiameterPx));
		return (16.0F - tDiameter) / 32.0F;
	}

	/** The half-inset this model's badges are sized to (the seated row diameter). */
	public float half() {
		return mHalf;
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
	 * One planned arrow quad: the quad's facing (NOT a cullface — arrows never cull),
	 * the box in 0..1 block space, the sprite id.
	 */
	public record FlowQuad(Direction quadFace, double[] box, ResourceLocation sprite) {

		/** Box copy guard — the planner builds fresh arrays per quad. */
		public FlowQuad {
			box = box.clone();
		}
	}

	/** The emission rules of the class doc, as pure geometry. */
	public static List<FlowQuad> planQuads(PipeFlowSnapshot aSnapshot, @Nullable Direction aSide, float aHalf) {
		List<FlowQuad> rPlans = new ArrayList<>();
		for (Direction tFace : Direction.values()) {
			if (!aSnapshot.hasArrow(tFace)) continue;
			if (aSide != null && aSide != tFace) continue;
			rPlans.add(new FlowQuad(tFace, markerOf(tFace, aHalf), ARROW_SPRITE));
		}
		return rPlans;
	}

	/**
	 * The arrow badge box on the given face in 0..1 space (task pipe-output-arrows): the
	 * connected-arm cross-section {@code [aHalf, 1 - aHalf]} on the two tangent axes,
	 * pushed fully OUTSIDE the block boundary along the normal — the outer plane pokes
	 * {@link #ARROW_EPSILON} past the face, the inner plane sits {@link #ARROW_THICKNESS}
	 * out (the whole badge floats one epsilon layer off the arm end cap, z-fight-free and
	 * covering none of the pipe body art beyond its own footprint).
	 */
	public static double[] markerOf(Direction aFace, float aHalf) {
		double e = ARROW_EPSILON, t = ARROW_THICKNESS;
		float h = aHalf;
		return switch (aFace) {
			case DOWN  -> new double[] {h, -e,         h,        1 - h, -e + t,   1 - h};
			case UP    -> new double[] {h, 1 - e - t,  h,        1 - h, 1 + e,    1 - h};
			case NORTH -> new double[] {h, h,         -e,        1 - h, 1 - h,   -e + t};
			case SOUTH -> new double[] {h, h,          1 - e - t, 1 - h, 1 + e,    1 - h};
			case WEST  -> new double[] {-e, h,         h,       -e + t,   1 - h,   1 - h};
			case EAST  -> new double[] {1 - e - t, h,  h,        1 + e,  1 - h,    1 - h};
		};
	}

	// ---------------------------------------------------------------------------
	// baker (client-only; the CoverPlateModel recipe over FaceBakery)
	// ---------------------------------------------------------------------------

	private BakedQuad bakeArrowQuad(FlowQuad aPlan, TextureAtlasSprite aSprite) {
		double[] tBox = aPlan.box();
		// FaceBakery works in model space (0..16). The UV is the canonical full-face form
		// [u0,v0,u1,v1] = [0,0,16,16] (the uvof-private-copies fix, the issue #27
		// GTOreBakedModel ruling): the arrow is a DIRECTIONAL sprite (drawn pointing at V=0),
		// and the old GTCEu StaticFaceBakery cubeUV table put the box maxY in the slot that
		// BlockFaceUV reads as the sprite-top V on every side face (FaceInfo.java:19-42 —
		// the top corners are vertices 0/3; BlockFaceUV.java:31-38 — they read uvs[1]),
		// so the arrow hung upside down on all four sides, with an extra U mirror (180°)
		// on NORTH/EAST. The canonical form is the orientation every vanilla cube JSON
		// renders with, and it decouples the UV from the ±0.002 inflated slab box (the #16
		// atlas-edge rule — TextureAtlasSprite.getU linearly extrapolates past the sprite).
		// cull = null — the arrow never culls (spec ④: the arrow face never culls)
		Vector3f tFrom = new Vector3f((float) tBox[0] * 16, (float) tBox[1] * 16, (float) tBox[2] * 16);
		Vector3f tTo = new Vector3f((float) tBox[3] * 16, (float) tBox[4] * 16, (float) tBox[5] * 16);
		return BAKERY.bakeQuad(tFrom, tTo,
				new BlockElementFace(null, 0, aPlan.sprite().toString(), new BlockFaceUV(new float[] {0, 0, 16, 16}, 0)),
				aSprite, aPlan.quadFace(), BlockModelRotation.X0_Y0, null, true, aPlan.sprite());
	}
}
