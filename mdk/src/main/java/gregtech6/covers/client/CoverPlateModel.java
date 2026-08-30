package gregtech6.covers.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.joml.Vector3f;

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

import gregtech6.client.render.GTDynamicBakedModel;
import gregtech6.client.render.GTModelProperties;
import gregtech6.covers.GTCoverRenderSnapshot;

/**
 * The cover-plate dynamic model — the first consumer of the C-grade render foundation
 * (task p4-cover-core ⑥, ADR 2026-08-30-p4-cover-route option ①). A
 * {@link GTDynamicBakedModel} over the oven's blockstate-baked models: the
 * {@link GTCoverRenderSnapshot} in the {@code ModelData} says which faces carry covers,
 * and the plate quads are appended to the fallback's own quads.
 *
 * <p>Geometry follows the GTCEu ICoverableRenderer.renderCovers template
 * (ICoverableRenderer.java:43-91): a slab of the upstream cover-plate thickness
 * (2px = 2/16, the AbstractCoverDefault BOXES_COVERS shape) on each covered face,
 * inflated by the COVER_OVERLAY epsilon 0.002 (StaticFaceBakery.java:31 — "slightly
 * under a full block's size ... to combat Z-fighting": the plate pokes just past the
 * block boundary so it never coplanar-fights the block's own face). Emission rules
 * mirror the ICoverableRenderer mask logic:
 * <ul>
 * <li>pass side == covered face F → the plate's outer face (culled);</li>
 * <li>pass side == a face P perpendicular to F with NO cover of its own → the plate's
 *     rim quad facing P (culled) — a covered neighbour suppresses it;</li>
 * <li>pass side == null (unculled pass) → the plate's inner back face (unculled) —
 *     ICoverableRenderer.java:76 "render back".</li>
 * </ul>
 * Plates are opaque, so quads emit on the solid layer only (the chunk renderer gates
 * the layer calls through the model's ChunkRenderTypeSet; null = the all-layers pass).
 *
 * <p>RED LINE: reads ONLY the immutable snapshot — no BlockEntity is reachable from
 * here (render-thread semantics, GTDynamicBakedModel class doc). CLIENT-ONLY class:
 * it is instantiated exclusively through the Dist.CLIENT
 * {@code GTCoverClientListener} registration.
 */
public class CoverPlateModel extends GTDynamicBakedModel {

	/** The plate epsilon of GTCEu StaticFaceBakery.COVER_OVERLAY (BLOCK.inflate(0.002)). */
	public static final double PLATE_EPSILON = 0.002;

	/** The plate slab depth = the upstream cover thickness (AbstractCoverDefault BOXES_COVERS: 2px). */
	public static final double PLATE_THICKNESS = 2.0 / 16.0;

	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;

	private static final FaceBakery BAKERY = new FaceBakery();

	public CoverPlateModel(BakedModel aFallbackModel) {
		this(aFallbackModel, defaultSpriteLookup());
	}

	public CoverPlateModel(BakedModel aFallbackModel, Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup) {
		super(aFallbackModel);
		mSpriteLookup = aSpriteLookup;
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(aSpriteId);
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable net.minecraft.client.renderer.RenderType aRenderType) {
		List<BakedQuad> rQuads = new ArrayList<>(getFallbackModel().getQuads(aState, aSide, aRand));
		// opaque plates: solid layer (+ the null all-layers pass)
		if (aRenderType != null && !aRenderType.equals(net.minecraft.client.renderer.RenderType.solid())) return rQuads;
		if (!(aModelData.get(GTModelProperties.RENDER_SNAPSHOT) instanceof GTCoverRenderSnapshot tSnapshot)) return rQuads;

		for (PlateQuad tPlan : planQuads(tSnapshot, aSide)) {
			TextureAtlasSprite tSprite = mSpriteLookup.apply(tPlan.sprite());
			if (tSprite == null) continue; // atlas gap: skip the quad instead of rendering garbage
			rQuads.add(bakePlateQuad(tPlan, tSprite));
		}
		return rQuads;
	}

	// ---------------------------------------------------------------------------
	// planner (offline-testable: pure geometry over the snapshot, no sprite needed)
	// ---------------------------------------------------------------------------

	/**
	 * One planned plate quad: the quad's facing (and cullface), the slab box in
	 * 0..1 block space, the sprite id and the cover face it belongs to.
	 */
	public record PlateQuad(Direction quadFace, boolean cull, Direction coverFace, double[] box, ResourceLocation sprite) {

		/** Box copy guard — the planner builds fresh arrays per quad. */
		public PlateQuad {
			box = box.clone();
		}
	}

	/** The emission rules of the class doc, as pure geometry. */
	public static List<PlateQuad> planQuads(GTCoverRenderSnapshot aSnapshot, @Nullable Direction aSide) {
		List<PlateQuad> rPlans = new ArrayList<>();
		for (Direction tFace : Direction.values()) {
			ResourceLocation tSprite = aSnapshot.sprite(tFace);
			if (tSprite == null) continue;
			double[] tSlab = slabOf(tFace);
			if (aSide == null) {
				// ICoverableRenderer.java:76 — the inner back face (unculled), null pass only
				rPlans.add(new PlateQuad(tFace.getOpposite(), false, tFace, tSlab, tSprite));
				continue;
			}
			if (aSide == tFace) {
				// the plate's outer face, culled by the neighbour at the cover face
				rPlans.add(new PlateQuad(tFace, true, tFace, tSlab, tSprite));
				continue;
			}
			if (aSide.getAxis() != tFace.getAxis() && !aSnapshot.hasCover(aSide)) {
				// the plate's rim on the perpendicular pass, suppressed when that face has its own cover
				rPlans.add(new PlateQuad(aSide, true, tFace, tSlab, tSprite));
			}
		}
		return rPlans;
	}

	/**
	 * The slab box on the given face in 0..1 space — the covered face's outer 2px,
	 * inflated by {@link #PLATE_EPSILON} on all axes (GTCEu COVER_OVERLAY shape).
	 */
	public static double[] slabOf(Direction aFace) {
		double e = PLATE_EPSILON, t = PLATE_THICKNESS;
		return switch (aFace) {
			case DOWN  -> new double[] {-e, -e,       -e, 1 + e, t,        1 + e};
			case UP    -> new double[] {-e, 1 - t,    -e, 1 + e, 1 + e,    1 + e};
			case NORTH -> new double[] {-e, -e,       -e, 1 + e, 1 + e,    t};
			case SOUTH -> new double[] {-e, -e,       1 - t, 1 + e, 1 + e, 1 + e};
			case WEST  -> new double[] {-e, -e,       -e, t,     1 + e,    1 + e};
			case EAST  -> new double[] {1 - t, -e,    -e, 1 + e, 1 + e,    1 + e};
		};
	}

	// ---------------------------------------------------------------------------
	// baker (client-only; the GTCEu StaticFaceBakery.bakeFace recipe over FaceBakery)
	// ---------------------------------------------------------------------------

	private BakedQuad bakePlateQuad(PlateQuad aPlan, TextureAtlasSprite aSprite) {
		double[] tBox = aPlan.box();
		// FaceBakery works in model space (0..16); the GTCEu bakeFace cubeUV switch maps
		// the box extents per face — full-face UV across the slab.
		Vector3f tFrom = new Vector3f((float) tBox[0] * 16, (float) tBox[1] * 16, (float) tBox[2] * 16);
		Vector3f tTo = new Vector3f((float) tBox[3] * 16, (float) tBox[4] * 16, (float) tBox[5] * 16);
		float[] tUv = uvOf(aPlan.quadFace(), tBox[0] * 16, tBox[1] * 16, tBox[2] * 16, tBox[3] * 16, tBox[4] * 16, tBox[5] * 16);
		return BAKERY.bakeQuad(tFrom, tTo,
				new BlockElementFace(aPlan.cull() ? aPlan.quadFace() : null, 0, aPlan.sprite().toString(), new BlockFaceUV(tUv, 0)),
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
