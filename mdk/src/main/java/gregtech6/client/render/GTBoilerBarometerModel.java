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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraftforge.client.model.data.ModelData;

/**
 * The boiler front barometer dynamic model (task boiler-barometer) — the
 * {@link GTOvenOverlayModel} shape applied to the boiler families' pressure gauge, the
 * materialisation of the p13 render-pool deferral the r8-tex-large-boilers card booked.
 *
 * <p>Upstream (both boilers, FRONT side only) stacks TWO more texture layers over the
 * front art: the {@code BI.BAROMETER} dial and {@code BI.BAROMETER_SCALE[mBarometer]} red
 * needle (BI.java:165-166; the single tank MultiTileEntityBoilerTank.java:240, the large
 * boiler :357-361 — there on the Base10 front pair). The gauge byte is the synced 5-bit
 * {@code scale(steam, steamCapacity, 31)} visual (BoilerTank :145/:219-233, LargeBoiler
 * :259/:331-343), riding the vanilla two-channel sync into the client BE's
 * {@code mBarometer} — the model reads it through
 * {@link GTModelProperties#BAROMETER} (the snapshot at {@code getModelData()} time).
 *
 * <p>Layer composition per render pass: the static two-layer blockstate model (the
 * r8-tex-large-boilers {@code boilerModel} grammar — body tint + overlay decals, its
 * front decal plate sitting {@code 0.01} outside the face) carries the boiler art, and
 * the gauge adds the dial plate ({@link #DIAL_OFFSET}, {@code 0.01 + epsilon} — one
 * epsilon clear of the static decal) plus the needle plate ({@link #NEEDLE_OFFSET}, one
 * more epsilon) as cutout quads on the FACING face only. The fallback model is declared
 * {@code cutout}, so no render-type override is needed — the gauge quads join the same
 * cutout pass the fallback already draws on.
 *
 * <p>The needle red is upstream's render-time colour modulation
 * {@code CA_RED_64 = {64,0,0,255}} (CS.java:367) over the white needle texels — baked in
 * as the vertex retint {@link #NEEDLE_ARGB} (the {@link GTMachineTintModel#retintVertices}
 * product, tintIndex stays {@code -1} = untinted by the BlockColors chain).
 *
 * <p>In-world dispatch: the per-state ModelResourceLocations (26 tank rows x 4 facing =
 * 104, 5 large rows x 4 facing x 2 formed = 40) are registered by the Dist.CLIENT
 * {@link GTBoilerClientListener} (per-state keys, the ModelBakery
 * loadTopLevel-per-state ruling, GTOvenClientListener class doc).
 *
 * <p>RED LINE: reads ONLY the immutable ModelData snapshot — no BlockEntity is reachable
 * from here (render-thread semantics, GTDynamicBakedModel class doc). CLIENT-ONLY class:
 * instantiated exclusively through the Dist.CLIENT listener.
 */
public class GTBoilerBarometerModel extends GTDynamicBakedModel {

	/** The static boilerModel front decal plate depth (the 0.01 elements, GT6BlockStates). */
	public static final double DECAL_DEPTH = 0.01;

	/** The Z-fighting epsilon of GTCEu StaticFaceBakery.COVER_OVERLAY (BLOCK.inflate(0.002)). */
	public static final double OVERLAY_EPSILON = 0.002;

	/** Gauge slab thickness (1px inward — the outer plane carries the texture). */
	public static final double GAUGE_THICKNESS = 1.0 / 16.0;

	/** The dial plate plane: one epsilon clear of the static decal plate. */
	public static final double DIAL_OFFSET = DECAL_DEPTH + OVERLAY_EPSILON;

	/** The needle plate plane: one epsilon above the dial. */
	public static final double NEEDLE_OFFSET = DIAL_OFFSET + OVERLAY_EPSILON;

	/** Sprite id prefix: {@code block/barometer/} (the ledger's verbatim upstream names). */
	public static final String SPRITE_PREFIX = "block/barometer/";

	/** The dial sprite ({@code gt6:block/barometer/base}). */
	public static final ResourceLocation DIAL_SPRITE =
			ResourceLocation.fromNamespaceAndPath(GTRenderModelListener.MOD_ID, SPRITE_PREFIX + "base");

	/**
	 * The needle red: upstream {@code CA_RED_64 = {64,0,0,255}} (CS.java:367) as ARGB —
	 * the {@code (colour * tint + 255) >> 8} per-channel retint over the white needle
	 * texels reproduces the upstream modulation exactly (255 * 64 + 255 >> 8 = 64).
	 */
	public static final int NEEDLE_ARGB = 0xFF400000;

	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;

	private static final FaceBakery BAKERY = new FaceBakery();

	public GTBoilerBarometerModel(BakedModel aFallbackModel) {
		this(aFallbackModel, defaultSpriteLookup());
	}

	public GTBoilerBarometerModel(BakedModel aFallbackModel, Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup) {
		super(aFallbackModel);
		mSpriteLookup = aSpriteLookup;
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(aSpriteId);
	}

	/** This model keys on the barometer property, not the cover chain's RENDER_SNAPSHOT. */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return aModelData.has(GTModelProperties.BAROMETER);
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		// the fallback boilerModel is declared cutout — the gauge quads ride the same
		// cutout pass, no ChunkRenderTypeSet extension (class doc)
		boolean tCutout = aRenderType == null || aRenderType.equals(RenderType.cutout());
		if (!tCutout) return getFallbackModel().getQuads(aState, aSide, aRand);

		List<BakedQuad> rQuads = new ArrayList<>(getFallbackModel().getQuads(aState, aSide, aRand));
		Integer tGauge = aModelData.get(GTModelProperties.BAROMETER);
		if (tGauge == null || aState == null) return rQuads; // defensive — supportsDynamicQuads gates this
		for (GaugePlan tPlan : planGaugeQuads(frontOf(aState), aSide, tGauge)) {
			TextureAtlasSprite tSprite = mSpriteLookup.apply(tPlan.sprite());
			if (tSprite == null) continue; // atlas gap: skip the quad instead of rendering garbage
			rQuads.add(bakeGaugeQuad(tPlan, tSprite));
		}
		return rQuads;
	}

	// ---------------------------------------------------------------------------
	// planner (offline-testable: pure geometry over the gauge, no sprite needed)
	// ---------------------------------------------------------------------------

	/**
	 * One planned gauge quad: the world face it sits on (always the FACING face), the
	 * sprite id (dial or the zero-padded needle state), the slab box in 0..1 block space,
	 * and whether the quad carries the red vertex retint.
	 */
	public record GaugePlan(Direction quadFace, ResourceLocation sprite, double[] box, boolean needle) {

		/** Box copy guard — the planner builds fresh arrays per quad. */
		public GaugePlan {
			box = box.clone();
		}
	}

	/**
	 * The upstream two-quad front stack as pure geometry: emitted ONLY on the front face's
	 * own culling pass ({@code aSide == null} is the unculled pass and sees none — the
	 * vanilla cube convention, the quads carry a cullface; the oven planOverlayQuads form).
	 * The needle index binds {@code BAROMETER_SCALE[gauge]} — the zero-padded upstream
	 * file names, clamped 0..31 (the :232 {@code &31} mask).
	 */
	public static List<GaugePlan> planGaugeQuads(Direction aFront, @Nullable Direction aSide, int aGauge) {
		if (aSide == null || aSide != aFront) return List.of();
		return List.of(
				new GaugePlan(aSide, DIAL_SPRITE, slabOf(aSide, DIAL_OFFSET), false),
				new GaugePlan(aSide, needleSprite(aGauge), slabOf(aSide, NEEDLE_OFFSET), true));
	}

	/** The needle sprite for one gauge reading ({@code gt6:block/barometer/00..31}). */
	public static ResourceLocation needleSprite(int aGauge) {
		return ResourceLocation.fromNamespaceAndPath(GTRenderModelListener.MOD_ID,
				SPRITE_PREFIX + String.format("%02d", Math.max(0, Math.min(31, aGauge))));
	}

	/**
	 * The front face of a boiler state — the horizontal FACING (both carriers share the
	 * {@code BlockStateProperties.HORIZONTAL_FACING} instance: BoilerTankBlock.FACING and
	 * TileEntityBase10MultiBlockBase.FACING are the same constant). The FRONT = the
	 * barometer face, the :240/:357 {@code aSide == mFacing} pick.
	 */
	public static Direction frontOf(BlockState aState) {
		return aState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
				? aState.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
	}

	/**
	 * The full-face slab box on the given face in 0..1 space, the outer plane pushed
	 * {@code aOffset} past the face (the GTOvenOverlayModel.slabOf form with the offset
	 * parameter stacking the gauge plates over the static 0.01 decal): the outer plane
	 * carries the texture, the inner plane hides one pixel inside the block.
	 */
	public static double[] slabOf(Direction aFace, double aOffset) {
		double t = GAUGE_THICKNESS - aOffset;
		return switch (aFace) {
			case DOWN  -> new double[] {-OVERLAY_EPSILON, -aOffset,    -OVERLAY_EPSILON, 1 + OVERLAY_EPSILON, t, 1 + OVERLAY_EPSILON};
			case UP    -> new double[] {-OVERLAY_EPSILON, 1 - GAUGE_THICKNESS + aOffset, -OVERLAY_EPSILON, 1 + OVERLAY_EPSILON, 1 + aOffset, 1 + OVERLAY_EPSILON};
			case NORTH -> new double[] {-OVERLAY_EPSILON, -OVERLAY_EPSILON, -aOffset,        1 + OVERLAY_EPSILON, 1 + OVERLAY_EPSILON, t};
			case SOUTH -> new double[] {-OVERLAY_EPSILON, -OVERLAY_EPSILON, 1 - GAUGE_THICKNESS + aOffset, 1 + OVERLAY_EPSILON, 1 + OVERLAY_EPSILON, 1 + aOffset};
			case WEST  -> new double[] {-aOffset,         -OVERLAY_EPSILON, -OVERLAY_EPSILON, t,                   1 + OVERLAY_EPSILON, 1 + OVERLAY_EPSILON};
			case EAST  -> new double[] {1 - GAUGE_THICKNESS + aOffset, -OVERLAY_EPSILON, -OVERLAY_EPSILON, 1 + aOffset, 1 + OVERLAY_EPSILON, 1 + OVERLAY_EPSILON};
		};
	}

	// ---------------------------------------------------------------------------
	// baker (client-only; the oven recipe over FaceBakery)
	// ---------------------------------------------------------------------------

	private BakedQuad bakeGaugeQuad(GaugePlan aPlan, TextureAtlasSprite aSprite) {
		double[] tBox = aPlan.box();
		// model space (0..16); the UV is the canonical full-face form [0,0,16,16] rotation 0
		// (the r8-uvof-private-copies #27 ruling — the borrowed upright dial/needle art is
		// drawn for the 1.7.10 ITexture orientation the canonical form reproduces); the
		// bakeQuad rotation slot stays null. tintIndex -1: the needle red is baked into the
		// vertex colours below, the BlockColors chain stays out.
		Vector3f tFrom = new Vector3f((float) tBox[0] * 16, (float) tBox[1] * 16, (float) tBox[2] * 16);
		Vector3f tTo = new Vector3f((float) tBox[3] * 16, (float) tBox[4] * 16, (float) tBox[5] * 16);
		BakedQuad tQuad = BAKERY.bakeQuad(tFrom, tTo,
				new BlockElementFace(aPlan.quadFace(), -1, aPlan.sprite().toString(), new BlockFaceUV(new float[] {0, 0, 16, 16}, 0)),
				aSprite, aPlan.quadFace(), BlockModelRotation.X0_Y0, null, true, aPlan.sprite());
		if (!aPlan.needle()) return tQuad;
		// the CA_RED_64 modulation (class doc): the white needle texels retinted at bake
		// time, tintIndex stays -1
		return new BakedQuad(GTMachineTintModel.retintVertices(tQuad.getVertices(), NEEDLE_ARGB),
				-1, tQuad.getDirection(), tQuad.getSprite(), tQuad.isShade());
	}
}
