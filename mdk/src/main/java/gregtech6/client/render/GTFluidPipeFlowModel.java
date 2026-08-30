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
 * The pipe flow-arrow dynamic model (task p4-pipe-flow-control spec ④) — the second
 * consumer of the C-grade render foundation, the {@link CoverPlateModel} shape applied
 * to the fluid pipe: the {@link PipeFlowSnapshot} in the {@code ModelData} says which
 * faces carry output arrows, and the arrow quads are appended to the fallback's own
 * quads.
 *
 * <p>Geometry: on every marked face one thin arrow slab — outer plane poked
 * {@value #ARROW_EPSILON} past the block boundary (the GTCEu COVER_OVERLAY
 * StaticFaceBakery.java:31 Z-fighting epsilon, the CoverPlateModel precedent) and
 * {@value #ARROW_THICKNESS} px thick. Emission rules:
 * <ul>
 * <li>pass side == the marked face F → the arrow quad, NOT culled (a solid neighbour
 *     at the marked face must not swallow the arrow — it points INTO that neighbour);</li>
 * <li>pass side == null (unculled pass) → the same quad;</li>
 * <li>any other pass side → nothing (tangent passes never see the face plane).</li>
 * </ul>
 * Opaque quads: solid layer only (plus the null all-layers pass).
 *
 * <p>RED LINE: reads ONLY the immutable snapshot — no BlockEntity is reachable from
 * here (render-thread semantics, GTDynamicBakedModel class doc). CLIENT-ONLY class:
 * instantiated exclusively through the Dist.CLIENT {@code GTPipeFlowClientListener}.
 */
public class GTFluidPipeFlowModel extends GTDynamicBakedModel {

	/** The Z-fighting epsilon of GTCEu StaticFaceBakery.COVER_OVERLAY (BLOCK.inflate(0.002)). */
	public static final double ARROW_EPSILON = 0.002;

	/** The arrow slab thickness (1px — the inner plane hides inside the pipe block). */
	public static final double ARROW_THICKNESS = 1.0 / 16.0;

	/** The arrow sprite — the datagen lands it as a single-file atlas source (GT6Atlases). */
	public static final ResourceLocation ARROW_SPRITE = new ResourceLocation("gt6", "block/pipe_flow_arrow");

	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;

	private static final FaceBakery BAKERY = new FaceBakery();

	public GTFluidPipeFlowModel(BakedModel aFallbackModel) {
		this(aFallbackModel, defaultSpriteLookup());
	}

	public GTFluidPipeFlowModel(BakedModel aFallbackModel, Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup) {
		super(aFallbackModel);
		mSpriteLookup = aSpriteLookup;
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(aSpriteId);
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		List<BakedQuad> rQuads = new ArrayList<>(getFallbackModel().getQuads(aState, aSide, aRand));
		// opaque arrows: solid layer (+ the null all-layers pass)
		if (aRenderType != null && !aRenderType.equals(RenderType.solid())) return rQuads;
		if (!(aModelData.get(GTModelProperties.RENDER_SNAPSHOT) instanceof PipeFlowSnapshot tSnapshot)) return rQuads;

		for (FlowQuad tPlan : planQuads(tSnapshot, aSide)) {
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
	 * the slab box in 0..1 block space, the sprite id.
	 */
	public record FlowQuad(Direction quadFace, double[] box, ResourceLocation sprite) {

		/** Box copy guard — the planner builds fresh arrays per quad. */
		public FlowQuad {
			box = box.clone();
		}
	}

	/** The emission rules of the class doc, as pure geometry. */
	public static List<FlowQuad> planQuads(PipeFlowSnapshot aSnapshot, @Nullable Direction aSide) {
		List<FlowQuad> rPlans = new ArrayList<>();
		for (Direction tFace : Direction.values()) {
			if (!aSnapshot.hasArrow(tFace)) continue;
			if (aSide != null && aSide != tFace) continue;
			rPlans.add(new FlowQuad(tFace, slabOf(tFace), ARROW_SPRITE));
		}
		return rPlans;
	}

	/**
	 * The arrow slab box on the given face in 0..1 space — the CoverPlateModel slab shape
	 * at 1px thickness: the outer plane inflates {@link #ARROW_EPSILON} past the face,
	 * the inner plane hides {@link #ARROW_THICKNESS} inside the block.
	 */
	public static double[] slabOf(Direction aFace) {
		double e = ARROW_EPSILON, t = ARROW_THICKNESS;
		return switch (aFace) {
			case DOWN  -> new double[] {-e, -e,    -e,    1 + e, t,    1 + e};
			case UP    -> new double[] {-e, 1 - t, -e,    1 + e, 1 + e, 1 + e};
			case NORTH -> new double[] {-e, -e,    -e,    1 + e, 1 + e, t};
			case SOUTH -> new double[] {-e, -e,    1 - t, 1 + e, 1 + e, 1 + e};
			case WEST  -> new double[] {-e, -e,    -e,    t,     1 + e, 1 + e};
			case EAST  -> new double[] {1 - t, -e, -e,    1 + e, 1 + e, 1 + e};
		};
	}

	// ---------------------------------------------------------------------------
	// baker (client-only; the CoverPlateModel recipe over FaceBakery)
	// ---------------------------------------------------------------------------

	private BakedQuad bakeArrowQuad(FlowQuad aPlan, TextureAtlasSprite aSprite) {
		double[] tBox = aPlan.box();
		// FaceBakery works in model space (0..16); full-face UV across the slab.
		Vector3f tFrom = new Vector3f((float) tBox[0] * 16, (float) tBox[1] * 16, (float) tBox[2] * 16);
		Vector3f tTo = new Vector3f((float) tBox[3] * 16, (float) tBox[4] * 16, (float) tBox[5] * 16);
		float[] tUv = uvOf(aPlan.quadFace(), tBox[0] * 16, tBox[1] * 16, tBox[2] * 16, tBox[3] * 16, tBox[4] * 16, tBox[5] * 16);
		// cull = null — the arrow never culls (spec ④: 箭头面不 cull)
		return BAKERY.bakeQuad(tFrom, tTo,
				new BlockElementFace(null, 0, aPlan.sprite().toString(), new BlockFaceUV(tUv, 0)),
				aSprite, aPlan.quadFace(), BlockModelRotation.X0_Y0, null, true, aPlan.sprite());
	}

	/** The GTCEu StaticFaceBakery.bakeFace cubeUV switch (StaticFaceBakery.java:54-66). */
	private static float[] uvOf(Direction aFace, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return switch (aFace) {
			case UP    -> new float[] {(float) minX, (float) minZ, (float) maxX, (float) maxZ};
			case DOWN  -> new float[] {(float) minX, (float) maxZ, (float) maxX, (float) minZ};
			case NORTH -> new float[] {(float) maxX, (float) maxY, (float) minX, (float) minY};
			case SOUTH -> new float[] {(float) minX, (float) maxY, (float) maxX, (float) minY};
			case WEST  -> new float[] {(float) minZ, (float) maxY, (float) maxZ, (float) minY};
			case EAST  -> new float[] {(float) maxZ, (float) maxY, (float) minZ, (float) minY};
		};
	}
}
