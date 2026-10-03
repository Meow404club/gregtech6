package gregtech6.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

import gregtech6.block.GTOvenBlock;
import gregtech6.client.render.GTOvenRenderSnapshot.OvenOverlayGroup;

/**
 * The oven state-overlay dynamic model (task render-c-oven-overlay, ADR
 * 2026-09-01-render-c-oven-overlay ②) — the C-grade upgrade of the oven from pure
 * A-tier (static BlockState variants) to snapshot-driven overlay rendering. Third
 * consumer of the C-grade foundation, the {@link GTFluidPipeFlowModel} shape applied to
 * the oven.
 *
 * <p>Two-layer composition per render pass, both on ONE alpha-tested chunk layer:
 * <ul>
 * <li><b>material layer</b> — the fallback (A-tier blockstate) model's quads, TINTED.
 *     The A-tier model is the single-writer discipline's fallback anchor: the
 *     ACTIVE/RUNNING properties stay the unique correct source (driven by the BE fields
 *     through {@code applyVisualState} setBlock(state, 3)), so on a snapshot miss the
 *     block renders exactly as before (ADR ③: keeping the property = the single correct source of the A-tier fallback).
 *     The body quads are retinted with the machine colour (the {@code GTMachineTintModel}
 *     product) — see {@link #mTintedQuads}.</li>
 * <li><b>state overlay layer</b> — six full-face quads, textured from
 *     the upstream {@code overlay_active}/{@code overlay_running} groups
 *     (MultiTileEntityBasicMachine.java:174-203 texture-name form), picked by
 *     {@link GTOvenRenderSnapshot#overlayGroup()} — the :1014 pick with the
 *     {@code worldObj==null} branch dropped (declared non-port). The inactive state adds
 *     NO overlay quad: the upstream inactive {@code overlay} group's face detail is
 *     already carried by the A-tier front texture (declared trim, see assets/README.md).</li>
 * </ul>
 *
 * <p>Chunk-layer seat (r11-oven-solid-layer-fix): the whole model renders on the static
 * model's declared {@code render_type cutout} alone, riding the {@link
 * GTDynamicBakedModel#getRenderTypes} forward — no override. The old snapshot-present
 * solid+cutout split baked the fallback's six 0.01 alpha-texel decal shells into the
 * solid layer, which has NO alpha discard (GT6BlockStates machineModel :1979-1984): the
 * decals painted their RGB matte as opaque full-face plates OVER the tinted body — the
 * field "white oven" report. On cutout the transparent texels discard; this is exactly
 * how the 20+ {@code GTMachineTintModel}-wrapped machine families render the same
 * static-decal shape.
 *
 * <p>Overlay quads are the cube-faithful emission: cullface set (upstream
 * {@code aShouldSideBeRendered} gate, :1014), emitted only on the quad face's own pass.
 * The outer plane pokes {@value #OVERLAY_EPSILON} past the block boundary (the GTCEu
 * COVER_OVERLAY Z-fighting epsilon, the pipe-arrow precedent).
 *
 * <p>In-world dispatch: the oven's 16 per-state ModelResourceLocations
 * ({@code gt6:oven#active=…,facing=…,running=…}, vanilla ModelBakery loadTopLevel-per-state
 * keying) are registered by the Dist.CLIENT {@link GTOvenClientListener} — per-state keys,
 * because ModelBakery bakes blockstates per state, not per model file.
 *
 * <p>RED LINE: reads ONLY the immutable snapshot — no BlockEntity is reachable from here
 * (render-thread semantics, GTDynamicBakedModel class doc). CLIENT-ONLY class:
 * instantiated exclusively through the Dist.CLIENT listener.
 */
public class GTOvenOverlayModel extends GTDynamicBakedModel {

	/** The Z-fighting epsilon of GTCEu StaticFaceBakery.COVER_OVERLAY (BLOCK.inflate(0.002)). */
	public static final double OVERLAY_EPSILON = 0.002;

	/** Overlay slab thickness (1px inward — the outer plane carries the texture). */
	public static final double OVERLAY_THICKNESS = 1.0 / 16.0;

	/** Sprite id prefix: {@code block/oven_overlay_<group>_<face>} (lowercase, 1.20.1 charset). */
	public static final String SPRITE_PREFIX = "block/oven_overlay_";

	/**
	 * The per-instance retinted-copy table for the body pass (the {@link
	 * GTMachineTintModel} shape: tint → (source quad → retinted copy)). The oven ladder
	 * is SKIPPED by the {@link GTMachineTintModel} wrap (its states already carry this
	 * dynamic model, the {@code instanceof GTDynamicBakedModel} guard), so the body tint
	 * rides HERE — the fallback quads are retinted with the same
	 * {@link GTMachinePaintTint#tintARGB} colour every other machine gets (upstream
	 * unpainted = the row NBT_MATERIAL colour, TileEntityBase07Paintable.java:83-84;
	 * painted = the PAINT snapshot, which TileEntityOven.getModelData co-hosts on the
	 * same ModelData as OVEN_SNAPSHOT). Without it the tintindex-0 body cube renders the
	 * raw grayscale plate untinted — the "pure-white oven" field report root cause
	 * (task oven-texture-borrow).
	 */
	private final Map<Integer, Map<BakedQuad, BakedQuad>> mTintedQuads = new ConcurrentHashMap<>();

	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;

	private static final FaceBakery BAKERY = new FaceBakery();

	public GTOvenOverlayModel(BakedModel aFallbackModel) {
		this(aFallbackModel, defaultSpriteLookup());
	}

	public GTOvenOverlayModel(BakedModel aFallbackModel, Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup) {
		super(aFallbackModel);
		mSpriteLookup = aSpriteLookup;
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(aSpriteId);
	}

	/**
	 * Every oven render assembles here (the {@link GTMachineTintModel} :85-88 form): the
	 * tint resolves from the state + ModelData alone, so the no-snapshot transient (the
	 * first frame after placement, before the BE's first {@code getModelData} round-trip)
	 * renders on the same tinted path — the old strict gate let it slip out to the RAW
	 * fallback, an untinted white body for a frame (the r11-oven-solid-layer-fix
	 * breakpoint 3). The model only ever sits on the oven's 64 per-state keys
	 * (GTOvenClientListener), so the unconditional gate reaches nothing else.
	 */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return true;
	}

	/**
	 * One alpha-tested list (r11-oven-solid-layer-fix): the tinted body + the static
	 * decal shells + the snapshot-driven overlays, all on the blockstate-declared cutout
	 * layer (the base class forwards the chunk-layer query to the fallback's JSON
	 * {@code render_type}) plus the null all-quads pass. The overlay quads keep
	 * tintIndex -1, so {@code tintQuads} passes them through as the shared instances and
	 * only the tintindex-0 body copies multiply.
	 */
	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		if (aRenderType != null && !aRenderType.equals(RenderType.cutout())) return List.of(); // cutout-only seat
		List<BakedQuad> rQuads = new ArrayList<>(GTMachineTintModel.tintQuads(getFallbackModel().getQuads(aState, aSide, aRand),
				bodyTint(aState, aModelData), mTintedQuads));
		GTOvenRenderSnapshot tSnapshot = aModelData.get(GTModelProperties.OVEN_SNAPSHOT);
		if (tSnapshot == null) return rQuads; // the no-snapshot transient: the tinted fallback IS the whole render

		Direction tFacing = aState != null && aState.hasProperty(GTOvenBlock.FACING)
				? aState.getValue(GTOvenBlock.FACING) : Direction.NORTH;
		for (OverlayPlan tPlan : planOverlayQuads(tSnapshot, tFacing, aSide)) {
			TextureAtlasSprite tSprite = mSpriteLookup.apply(tPlan.sprite());
			if (tSprite == null) continue; // atlas gap: skip the quad instead of rendering garbage
			rQuads.add(bakeOverlayQuad(tPlan, tSprite));
		}
		return rQuads;
	}

	/**
	 * The body-pass tint (the {@link GTMachineTintModel#getDynamicQuads} colour line over
	 * the same dispatch): index 0 resolves PAINT-wins-else-row-material through the
	 * combined {@link GTMachinePaintTint#tintMaterialOf} gate (the oven ladder rides the
	 * {@code GTBasicMachineBlock.materialOf} arm). Reads ONLY the state + ModelData —
	 * the render-route red line.
	 */
	private static int bodyTint(@Nullable BlockState aState, ModelData aModelData) {
		return GTMachinePaintTint.tintARGB(aModelData,
				aState == null ? null : GTMachinePaintTint.tintMaterialOf(aState.getBlock()), 0);
	}

	// ---------------------------------------------------------------------------
	// planner (offline-testable: pure geometry over the snapshot, no sprite needed)
	// ---------------------------------------------------------------------------

	/** The upstream per-face texture names, in the IIconContainer[6] order (:176-203). */
	public enum OvenTextureFace {
		BOTTOM, TOP, LEFT, FRONT, RIGHT, BACK;

		/** The texture-path token (upstream directory name, already lowercase). */
		public String textureKey() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/**
	 * One planned overlay quad: the world face it sits on, the upstream texture face drawn
	 * there, the sprite id, and the slab box in 0..1 block space.
	 */
	public record OverlayPlan(Direction quadFace, OvenTextureFace textureFace, ResourceLocation sprite, double[] box) {

		/** Box copy guard — the planner builds fresh arrays per quad. */
		public OverlayPlan {
			box = box.clone();
		}
	}

	/**
	 * The upstream {@code FACING_ROTATIONS[facing][side]} face pick (CS.java:528-537
	 * verbatim, horizontal machine — 0=bottom, 1=top, 2=left, 3=front, 4=right, 5=back),
	 * the emission rules as pure geometry: a state overlay on every face of the block,
	 * each quad emitted only on its own culling pass ({@code aSide == null} is the
	 * unculled pass and sees none — vanilla cube convention, the quads carry a cullface).
	 * The inactive group plans nothing.
	 */
	public static List<OverlayPlan> planOverlayQuads(GTOvenRenderSnapshot aSnapshot, Direction aFacing, @Nullable Direction aSide) {
		OvenOverlayGroup tGroup = aSnapshot.overlayGroup();
		if (tGroup == OvenOverlayGroup.NONE) return List.of();
		if (aSide == null) return List.of();
		OvenTextureFace tTextureFace = textureFaceOf(aFacing, aSide);
		return List.of(new OverlayPlan(aSide, tTextureFace,
				spriteOf(tGroup, tTextureFace), slabOf(aSide)));
	}

	/**
	 * CS.java:528-537 verbatim ({@code [facing][side] -> 0=bottom,1=top,2=left,3=front,
	 * 4=right,5=back}); rows for the vertical facings kept for table fidelity although
	 * HORIZONTAL_FACING never produces them.
	 */
	public static OvenTextureFace textureFaceOf(Direction aFacing, Direction aSide) {
		OvenTextureFace[][] tTable = {
				//                                                      DOWN                UP                NORTH              SOUTH              WEST               EAST
				/* DOWN  (upstream row 0) */ {OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.LEFT, OvenTextureFace.FRONT, OvenTextureFace.RIGHT, OvenTextureFace.BACK},
				/* UP    (upstream row 1) */ {OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.LEFT, OvenTextureFace.FRONT, OvenTextureFace.RIGHT, OvenTextureFace.BACK},
				/* NORTH (upstream row 2) */ {OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.FRONT, OvenTextureFace.BACK, OvenTextureFace.RIGHT, OvenTextureFace.LEFT},
				/* SOUTH (upstream row 3) */ {OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.BACK, OvenTextureFace.FRONT, OvenTextureFace.LEFT, OvenTextureFace.RIGHT},
				/* WEST  (upstream row 4) */ {OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.LEFT, OvenTextureFace.RIGHT, OvenTextureFace.FRONT, OvenTextureFace.BACK},
				/* EAST  (upstream row 5) */ {OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.RIGHT, OvenTextureFace.LEFT, OvenTextureFace.BACK, OvenTextureFace.FRONT},
		};
		return tTable[aFacing.get3DDataValue()][aSide.get3DDataValue()];
	}

	/** The sprite id for one overlay group and texture face ({@code gt6:block/oven_overlay_<group>_<face>}). */
	public static ResourceLocation spriteOf(OvenOverlayGroup aGroup, OvenTextureFace aFace) {
		// fromNamespaceAndPath, not the two-arg ctor: private in 1.21.1, and Forge 1.20.1
		// backported the same factory (both legs javap-proven, adapt-registry-core) —
		// the swap table's conservative regex skips this call (concat expression argument).
		return ResourceLocation.fromNamespaceAndPath(GTRenderModelListener.MOD_ID,
				SPRITE_PREFIX + aGroup.textureKey() + "_" + aFace.textureKey());
	}

	/**
	 * The full-face slab box on the given face in 0..1 space — the pipe-arrow slab shape:
	 * the outer plane inflates {@link #OVERLAY_EPSILON} past the face, the inner plane
	 * hides {@link #OVERLAY_THICKNESS} inside the block.
	 */
	public static double[] slabOf(Direction aFace) {
		double e = OVERLAY_EPSILON, t = OVERLAY_THICKNESS;
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
	// baker (client-only; the pipe-arrow recipe over FaceBakery)
	// ---------------------------------------------------------------------------

	private BakedQuad bakeOverlayQuad(OverlayPlan aPlan, TextureAtlasSprite aSprite) {
		double[] tBox = aPlan.box();
		// FaceBakery works in model space (0..16). The UV is the canonical full-face form
		// [u0,v0,u1,v1] = [0,0,16,16] (the uvof-private-copies fix, the issue #27
		// GTOreBakedModel ruling): the borrowed upstream face PNGs are UPRIGHT art (the
		// running front's glow window sits in the sprite's bottom half), drawn for the
		// orientation 1.7.10's renderFixed* ITexture family bakes — which the #27 ruling
		// corner-mapped to exactly this canonical form. The old GTCEu StaticFaceBakery
		// cubeUV table put the box maxY in the slot that BlockFaceUV reads as the
		// sprite-top V on every side face (FaceInfo.java:19-42 — the top corners are
		// vertices 0/3; BlockFaceUV.java:31-38 — they read uvs[1]), so every side face
		// baked upside down (the glow window at the door's top), with an extra U mirror
		// (180°) on NORTH/EAST. The canonical form also decouples the UV from the ±0.002
		// inflated slab box (the #16 atlas-edge rule — TextureAtlasSprite.getU linearly
		// extrapolates past the sprite).
		// cullface rides the BlockElementFace (the upstream aShouldSideBeRendered gate, :1014);
		// the bakeQuad rotation slot stays null (no element rotation)
		Vector3f tFrom = new Vector3f((float) tBox[0] * 16, (float) tBox[1] * 16, (float) tBox[2] * 16);
		Vector3f tTo = new Vector3f((float) tBox[3] * 16, (float) tBox[4] * 16, (float) tBox[5] * 16);
		// tintIndex -1 (NO_TINT): the state decal is UNCOLOURED upstream
		// (BlockTextureDefault.java:179-180, the P22 split) — never multiplied by the
		// paint/material colour, and the static JSON overlays deserialize to the same
		// -1 default (BlockElementFace.Deserializer DEFAULT_TINT_INDEX). The old
		// explicit 0 diverged from both and would have let any tint route wash the
		// door art (the oven-texture-borrow follow-up).
		return BAKERY.bakeQuad(tFrom, tTo,
				new BlockElementFace(aPlan.quadFace(), -1, aPlan.sprite().toString(), new BlockFaceUV(new float[] {0, 0, 16, 16}, 0)),
				aSprite, aPlan.quadFace(), BlockModelRotation.X0_Y0, null, true, aPlan.sprite());
	}
}
