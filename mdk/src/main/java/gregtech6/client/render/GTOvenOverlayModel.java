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

import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

import gregtech6.block.GTOvenBlock;
import gregtech6.client.render.GTOvenRenderSnapshot.OvenOverlayGroup;

/**
 * The oven state-overlay dynamic model (task p9-render-c-oven-overlay, ADR
 * 2026-09-01-p9-render-c-oven-overlay ②) — the C-grade upgrade of the oven from pure
 * A-tier (static BlockState variants) to snapshot-driven overlay rendering. Third
 * consumer of the C-grade foundation, the {@link GTFluidPipeFlowModel} shape applied to
 * the oven.
 *
 * <p>Two-layer composition per render pass:
 * <ul>
 * <li><b>material layer</b> — the fallback (A-tier blockstate) model's quads, verbatim.
 *     The A-tier model is the single-writer discipline's fallback anchor: the
 *     ACTIVE/RUNNING properties stay the unique correct source (driven by the BE fields
 *     through {@code applyVisualState} setBlock(state, 3)), so on a snapshot miss the
 *     block renders exactly as before (ADR ③: keeping the property = the single correct source of the A-tier fallback).
 *     It renders on the solid layer (plus the null all-layers pass).</li>
 * <li><b>state overlay layer</b> — six full-face quads on the cutout layer, textured from
 *     the upstream {@code overlay_active}/{@code overlay_running} groups
 *     (MultiTileEntityBasicMachine.java:174-203 texture-name form), picked by
 *     {@link GTOvenRenderSnapshot#overlayGroup()} — the :1014 pick with the
 *     {@code worldObj==null} branch dropped (declared non-port). The inactive state adds
 *     NO overlay quad: the upstream inactive {@code overlay} group's face detail is
 *     already carried by the A-tier front texture (declared trim, see assets/README.md).</li>
 * </ul>
 *
 * <p>Chunk-layer wiring (the research card's cutout checkpoint): the chunk renderer only
 * queries the layers of {@link #getRenderTypes} (IForgeBakedModel.java:85
 * ChunkRenderTypeSet), so the override extends the set with cutout while the snapshot is
 * present; {@code getDynamicQuads} then filters quads per layer — solid keeps the
 * fallback, cutout carries the overlays (upstream BlockTextureMulti's second layer,
 * :1014).
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

	/** This model keys on the oven snapshot property, not the cover chain's RENDER_SNAPSHOT. */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return aModelData.has(GTModelProperties.OVEN_SNAPSHOT);
	}

	/**
	 * While the snapshot is present the oven renders on solid (fallback) + cutout
	 * (overlays); without it the block keeps the blockstate-declared set.
	 */
	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState aState, RandomSource aRand, ModelData aData) {
		if (aData.has(GTModelProperties.OVEN_SNAPSHOT)) {
			return ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout());
		}
		return super.getRenderTypes(aState, aRand, aData);
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		GTOvenRenderSnapshot tSnapshot = aModelData.get(GTModelProperties.OVEN_SNAPSHOT);
		if (tSnapshot == null) {
			// no oven snapshot (defensive — supportsDynamicQuads gates this) → pure fallback
			return getFallbackModel().getQuads(aState, aSide, aRand);
		}
		boolean tSolid = aRenderType == null || aRenderType.equals(RenderType.solid());
		boolean tCutout = aRenderType == null || aRenderType.equals(RenderType.cutout());
		if (!tSolid && !tCutout) return List.of(); // the machine draws on no other chunk layer

		List<BakedQuad> rQuads = new ArrayList<>();
		if (tSolid) rQuads.addAll(getFallbackModel().getQuads(aState, aSide, aRand));
		if (!tCutout) return rQuads;

		Direction tFacing = aState != null && aState.hasProperty(GTOvenBlock.FACING)
				? aState.getValue(GTOvenBlock.FACING) : Direction.NORTH;
		for (OverlayPlan tPlan : planOverlayQuads(tSnapshot, tFacing, aSide)) {
			TextureAtlasSprite tSprite = mSpriteLookup.apply(tPlan.sprite());
			if (tSprite == null) continue; // atlas gap: skip the quad instead of rendering garbage
			rQuads.add(bakeOverlayQuad(tPlan, tSprite));
		}
		return rQuads;
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
		return new ResourceLocation(GTRenderModelListener.MOD_ID,
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
		// FaceBakery works in model space (0..16); full-face UV across the slab.
		Vector3f tFrom = new Vector3f((float) tBox[0] * 16, (float) tBox[1] * 16, (float) tBox[2] * 16);
		Vector3f tTo = new Vector3f((float) tBox[3] * 16, (float) tBox[4] * 16, (float) tBox[5] * 16);
		float[] tUv = uvOf(aPlan.quadFace(), tBox[0] * 16, tBox[1] * 16, tBox[2] * 16, tBox[3] * 16, tBox[4] * 16, tBox[5] * 16);
		// cullface rides the BlockElementFace (the upstream aShouldSideBeRendered gate, :1014);
		// the bakeQuad rotation slot stays null (no element rotation)
		return BAKERY.bakeQuad(tFrom, tTo,
				new BlockElementFace(aPlan.quadFace(), 0, aPlan.sprite().toString(), new BlockFaceUV(tUv, 0)),
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
