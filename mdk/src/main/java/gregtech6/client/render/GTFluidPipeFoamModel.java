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
 * The pipe C-Foam dynamic model (task p25-c-foam-pipe-spray spec ⑤) — the third consumer
 * of the C-grade render foundation, the {@link GTFluidPipeFlowModel} shape over the
 * {@link PipeFoamSnapshot}: fresh foam OVERLAYS the full-block fresh texture on the pipe
 * body (the upstream pass-7 overlay, TileEntityBase10ConnectorRendered.java:108/:137) and
 * dried foam REPLACES the whole body with the hardened cube (the upstream pass-0 swap,
 * :114/:138 — the pipe model quads are suppressed entirely). The owned variant picks the
 * owned texture pair (upstream :261-262); the TINT does not ride this model — the foam
 * quads carry {@link #FOAM_TINT_INDEX} and the registered pipe BlockColor resolves the
 * PAINT colour (applyFoam paints the pipe the foam colour, upstream :161/:163).
 *
 * <p>CHAIN: registered as the outer wrapper of the flow model ({@link #chain()} — one
 * registration per per-state key, GTRenderModelListener last-wins), so an arrowed+foamed
 * pipe renders arrows AND foam; a foam-only pipe hits {@link #supportsDynamicQuads} on the
 * FOAM key (the flow model alone would fall back on its RENDER_SNAPSHOT gate). Geometry:
 * full-cube quads inflated {@value #FOAM_EPSILON} past the block boundary (the GTCEu
 * StaticFaceBakery epsilon, the arrow precedent) — never culled, solid layer only.
 *
 * <p>RED LINE: reads ONLY the immutable snapshot — no BlockEntity is reachable from here
 * (render-thread semantics, GTDynamicBakedModel class doc). CLIENT-ONLY class:
 * instantiated exclusively through the Dist.CLIENT chain registration (and the offline
 * tests, which exercise the pure planner over its {@link FoamQuad} records).
 */
public class GTFluidPipeFoamModel extends GTDynamicBakedModel {

	/** The Z-fighting epsilon of GTCEu StaticFaceBakery.COVER_OVERLAY (the arrow slab's value). */
	public static final double FOAM_EPSILON = 0.002;

	/**
	 * The foam tint index — index 0 is TAKEN by the arrow quads' BlockElementFace default,
	 * so the foam quads answer on index 1 and the pipe BlockColor gates on exactly this
	 * value (the body blockstate model carries no tintindex, the arrows stay untinted).
	 */
	public static final int FOAM_TINT_INDEX = 1;

	/** The four grayscale foam sprites — FRESH/HARDENED x normal/owned (upstream :261-262 pairs). */
	public static final ResourceLocation FRESH_SPRITE = new ResourceLocation("gt6", "block/cfoam_fresh");
	public static final ResourceLocation FRESH_OWNED_SPRITE = new ResourceLocation("gt6", "block/cfoam_fresh_owned");
	public static final ResourceLocation HARDENED_SPRITE = new ResourceLocation("gt6", "block/cfoam_hardened");
	public static final ResourceLocation HARDENED_OWNED_SPRITE = new ResourceLocation("gt6", "block/cfoam_hardened_owned");

	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;

	private static final FaceBakery BAKERY = new FaceBakery();

	public GTFluidPipeFoamModel(BakedModel aFallbackModel) {
		this(aFallbackModel, defaultSpriteLookup());
	}

	public GTFluidPipeFoamModel(BakedModel aFallbackModel, Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup) {
		super(aFallbackModel);
		mSpriteLookup = aSpriteLookup;
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(aSpriteId);
	}

	/**
	 * The runtime chain factory: the foam model wraps the flow model, the flow model wraps
	 * the baked blockstate model. ONE registration site (GTPipeFlowClientListener) — the
	 * per-state keys stay single-registered, no ordering hazard.
	 */
	public static Function<BakedModel, BakedModel> chain() {
		return aBaked -> new GTFluidPipeFoamModel(new GTFluidPipeFlowModel(aBaked));
	}

	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		// the flow model alone keys on RENDER_SNAPSHOT — a foam-only pipe must dispatch too
		return aModelData.has(GTModelProperties.RENDER_SNAPSHOT) || aModelData.has(GTModelProperties.FOAM_SNAPSHOT);
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		PipeFoamSnapshot tSnapshot = aModelData.get(GTModelProperties.FOAM_SNAPSHOT);
		if (tSnapshot != null && tSnapshot.dried()) {
			// the dried pass-0 swap (upstream :108/:114/:138): the foam cube IS the body —
			// the pipe/arrow quads are suppressed entirely
			return foamQuads(tSnapshot, aSide, aRenderType);
		}
		// fresh (or absent) — the fallback chain renders the pipe body + arrows (the flow
		// model's forced 5-arg face), the fresh foam rides ON TOP as the pass-7 overlay
		List<BakedQuad> rQuads = new ArrayList<>(getFallbackModel().getQuads(aState, aSide, aRand, aModelData, aRenderType));
		if (tSnapshot == null) return rQuads;
		rQuads.addAll(foamQuads(tSnapshot, aSide, aRenderType));
		return rQuads;
	}

	/** The solid-layer gate + the six planned quads, resolved against the atlas. */
	private List<BakedQuad> foamQuads(PipeFoamSnapshot aSnapshot, @Nullable Direction aSide, @Nullable RenderType aRenderType) {
		List<BakedQuad> rQuads = new ArrayList<>();
		// opaque foam: solid layer only (+ the null all-layers pass)
		if (aRenderType != null && !aRenderType.equals(RenderType.solid())) return rQuads;
		for (FoamQuad tPlan : planQuads(aSnapshot, aSide)) {
			TextureAtlasSprite tSprite = mSpriteLookup.apply(tPlan.sprite());
			if (tSprite == null) continue; // atlas gap: skip the quad instead of rendering garbage
			rQuads.add(bakeFoamQuad(tPlan, tSprite));
		}
		return rQuads;
	}

	// ---------------------------------------------------------------------------
	// planner (offline-testable: pure geometry over the snapshot, no sprite needed)
	// ---------------------------------------------------------------------------

	/**
	 * One planned foam quad: the quad's facing, the full-cube box in 0..1 block space, the
	 * sprite id (the dried/owned pair) and the {@link #FOAM_TINT_INDEX} tint.
	 */
	public record FoamQuad(Direction quadFace, double[] box, ResourceLocation sprite, int tintIndex) {

		/** Box copy guard — the planner builds fresh arrays per quad. */
		public FoamQuad {
			box = box.clone();
		}
	}

	/** The snapshot → sprite pair (upstream :261-262 mOwnable ternaries, dried/fresh split). */
	public static ResourceLocation spriteOf(PipeFoamSnapshot aSnapshot) {
		return aSnapshot.dried()
				? (aSnapshot.owned() ? HARDENED_OWNED_SPRITE : HARDENED_SPRITE)
				: (aSnapshot.owned() ? FRESH_OWNED_SPRITE : FRESH_SPRITE);
	}

	/** The full-cube emission rules: one quad per face (the box covers the whole block). */
	public static List<FoamQuad> planQuads(PipeFoamSnapshot aSnapshot, @Nullable Direction aSide) {
		List<FoamQuad> rPlans = new ArrayList<>();
		ResourceLocation tSprite = spriteOf(aSnapshot);
		for (Direction tFace : Direction.values()) {
			if (aSide != null && aSide != tFace) continue;
			rPlans.add(new FoamQuad(tFace, cubeOf(), tSprite, FOAM_TINT_INDEX));
		}
		return rPlans;
	}

	/** The epsilon-inflated full cube in 0..1 space (the arrow slab's boundary poke). */
	public static double[] cubeOf() {
		double e = FOAM_EPSILON;
		return new double[] {-e, -e, -e, 1 + e, 1 + e, 1 + e};
	}

	// ---------------------------------------------------------------------------
	// baker (client-only; the arrow-baker recipe over FaceBakery)
	// ---------------------------------------------------------------------------

	private BakedQuad bakeFoamQuad(FoamQuad aPlan, TextureAtlasSprite aSprite) {
		double[] tBox = aPlan.box();
		// FaceBakery works in model space (0..16); full-face UV across the cube.
		Vector3f tFrom = new Vector3f((float) tBox[0] * 16, (float) tBox[1] * 16, (float) tBox[2] * 16);
		Vector3f tTo = new Vector3f((float) tBox[3] * 16, (float) tBox[4] * 16, (float) tBox[5] * 16);
		float[] tUv = uvOf(aPlan.quadFace(), tBox[0] * 16, tBox[1] * 16, tBox[2] * 16, tBox[3] * 16, tBox[4] * 16, tBox[5] * 16);
		// cull = null — the foam never culls (the arrow precedent; the epsilon inflation
		// keeps it off the block boundary)
		return BAKERY.bakeQuad(tFrom, tTo,
				new BlockElementFace(null, aPlan.tintIndex(), aPlan.sprite().toString(), new BlockFaceUV(tUv, 0)),
				aSprite, aPlan.quadFace(), BlockModelRotation.X0_Y0, null, true, aPlan.sprite());
	}

	/** The GTCEu StaticFaceBakery.bakeFace cubeUV switch (StaticFaceBakery.java:54-66, the arrow baker's table). */
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
