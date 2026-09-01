package gregtech6.client.wire;

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
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;

import gregtech6.block.wire.GTWireBlock;

/**
 * The connection-aware wire baked model (task p9-wire-family-w2) — the modern direct
 * translation of the upstream 7-pass connector rendering
 * (TileEntityBase10ConnectorRendered): pass 0 = the core box of the wire diameter
 * ({@code setBlockBounds2 :113-116}), passes 1-6 = one connection arm per connected side
 * ({@code :120-133}, each arm spanning from the core to the block boundary with the same
 * cross-section — the port's connector length is always 0 because covers/ITileEntitySurface
 * are not ported, {@code getConnectorLength :241-249} rLength = 0 in every reachable case).
 *
 * <p>ADR ⑨ (2026-09-01-p9-wire-family): the connection mask LIVES IN THE BLOCKSTATE
 * ({@link GTWireBlock#CONNECTIONS}, 0..63), so this model is a pure state function — it
 * NEVER touches {@link ModelData} or any BlockEntity (the world-persistent visual red
 * line: BakedModel only; BEWLR/TESR/BER forbidden). 64 mask shapes are generated lazily
 * and cached (per (insulated, diameter, mask) for the offline geometry plans, per mask for
 * the sprite-baked quads of each instance).
 *
 * <p>Texture semantics (MultiTileEntityWireElectric.java:237-241): the grayscale
 * {@code materialicons/<set>/wire.png} carries the material colour through tint index 0
 * ({@link GTWireTint}, the {@code mRGBa} dye), while insulated cables add the gray-64
 * INSULATION layers as separate alpha-cutout quads — tint index 1 = the upstream
 * {@code UT.Code.getRGBInt(64, 64, 64)} jacket. Per upstream :138/:139 the layering is:
 * <ul>
 * <li>bare wire: every face = material wire texture (tint 0);</li>
 * <li>cable, mask == 0 (the standalone {@code getTextureConnected} form): core = wire
 *     texture + the diameter-tier insulation overlay;</li>
 * <li>cable, mask != 0: core faces = {@code INSULATION_FULL} gray only, each arm's outer
 *     cap = wire + tier overlay ({@code getTextureConnected}), the arm side walls =
 *     {@code INSULATION_FULL} ({@code getTextureSide}), the face buried against the core
 *     is skipped ({@code aSide == OPOS[pass-1] → null}).</li>
 * </ul>
 * The insulation tier follows the upstream :238 diameter ladder: &lt;0.37 TINY, &lt;0.49
 * SMALL, &lt;0.74 MEDIUM, &lt;0.99 LARGE, else HUGE (diameters = PX_P/16, CS.java:492).
 *
 * <p>Task p11-wire-fiber-texture — the LASER family branch (MultiTileEntityWireLaser.java
 * :121-122): upstream overrides BOTH texture picks with the SAME fixed pair
 * {@code BlockTextureMulti(BlockTextureDefault(FIBER_WIRE, mRGBa),
 * BlockTextureDefault(FIBER_WIRE_OVERLAY))} — every visible face (core, arm caps AND arm
 * side walls, the :138/:139 picks) carries the dyed fiber base plus the untinted overlay,
 * and there is NO glow layer (the {@code BlockTextureDefault} two-arg form has no
 * brightness argument). The port renders this as a parallel family form next to the
 * electric bare/cable pair: {@link Params} gains the {@code overlaySprite} carrier (null =
 * electric/redstone behaviour, byte-for-byte the pre-p11 plans) and the fiber planner
 * {@link #planShapesFiber} emits one {@link SpriteKind#FIBER_OVERLAY} twin (untinted,
 * {@link #INSULATION_EPSILON}-inflated, same face/cull) per material quad. The dye still
 * rides tint index 0 through {@link GTWireTint} — the row material is MT.NULL
 * (Loader_MultiTileEntities.java:1815), the same {@code mRGBa} source upstream dyes with.
 *
 * <p>Task p11-wire-brightness — the REDSTONE family joins the table (the last family off
 * the JSON fallback; the same swap the electric rows got, {@link GTWireClientListener}
 * feeds the six rows with the row's set sprite and the cable's insulation layers). The
 * per-family jacket colour rides {@link GTWireTint} (the upstream redstone fixed
 * {@code 96,64,64} jacket, MultiTileEntityWireRedstoneInsulated :184-185, against the
 * electric {@code 64,64,64} :237-238). DECLARED DEVIATION: upstream flips a dynamic
 * TEXTURE-fullbright flag on the bare redstone wire quads — {@code mState > 0}
 * (MultiTileEntityWireRedstone :81-82, the BlockTextureDefault brightness argument:
 * constant 240 light and AO off, BlockTextureDefault.java:150/:168/:194-196), and the
 * insulated class reuses the static material flag {@code mIsGlowing} the same way
 * (:185). A per-signal lightmap override from a BakedModel needs ModelData/BEWLR — the
 * P9 ADR red line (this model is a pure state function of {@link GTWireBlock#CONNECTIONS},
 * it never touches the BE) — so the flag is declared UNIMPLEMENTED here: with signal up,
 * RedAlloy/Signalum bare wires render at world light instead of constant full brightness.
 * The real light counterpart (the Lumium Wirelamp) is the world-light emission on
 * {@code GTWireBlock.getLightEmission}, which lights these same quads through the engine.
 *
 * <p>The tier/full overlays sit ON the material quads, so they are inflated by
 * {@value #INSULATION_EPSILON} (the GTCEu COVER_OVERLAY z-fight epsilon, the
 * GTFluidPipeFlowModel/oven precedent) and rendered on the cutout layer; the material
 * quads stay on the solid layer. Item form (state == null) renders the upstream
 * {@code worldObj == null} default mask {@code SBIT_S|SBIT_N = 12}
 * (TileEntityBase10ConnectorRendered.java:107 — a straight N-S segment).
 */
public class GTWireBakedModel implements IDynamicBakedModel {

	/** The z-fight epsilon of the GTCEu COVER_OVERLAY (StaticFaceBakery.java:31). */
	public static final double INSULATION_EPSILON = 0.002;

	/** The upstream {@code worldObj == null} default: {@code SBIT_S|SBIT_N = 8|4} (CS.java:612, :107). */
	public static final int ITEM_MASK = 12;

	/** The upstream diameter clamp floor PX_P[2] (readFromNBT2 :64) — legacy pair renders here. */
	public static final float MIN_DIAMETER_PX = 2.0F;

	/** Which sprite a planned face carries. */
	public enum SpriteKind { WIRE, FIBER_OVERLAY, INSULATION_FULL, INSULATION_TINY, INSULATION_SMALL, INSULATION_MEDIUM, INSULATION_LARGE, INSULATION_HUGE }

	/**
	 * One planned face quad: the facing (also the per-side chunk dispatch key), the box in
	 * 0..1 block space ({@code minX, minY, minZ, maxX, maxY, maxY}), the tint index
	 * (0 = material colour, 1 = insulation gray) and the sprite kind. Box copy guard
	 * (the FlowQuad discipline).
	 */
	public record Shape(Direction face, double[] box, int tintIndex, SpriteKind kind, @Nullable Direction cull) {
		public Shape {
			box = box.clone();
		}
	}

	/**
	 * The immutable per-block render identity (the W1 carrier fields, GTWireBlock). The
	 * p11 laser branch rides {@code overlaySprite}: null = the electric/redstone forms
	 * (plans unchanged), non-null = the FIBER_WIRE(+OVERLAY) pair of WireLaser :121-122.
	 */
	public record Params(@Nullable ResourceLocation wireSprite, @Nullable ResourceLocation overlaySprite,
			boolean insulated, int diameterPx) {

		/** The pre-p11 three-field form (every electric/redstone/legacy row). */
		public Params(@Nullable ResourceLocation aWireSprite, boolean aInsulated, int aDiameterPx) {
			this(aWireSprite, null, aInsulated, aDiameterPx);
		}
	}

	private static final FaceBakery BAKERY = new FaceBakery();

	/** Offline geometry plan cache — key = (insulated ? 1 : 0) | (diameterPx << 1) | (mask << 8). */
	private static final Map<Long, List<Shape>> SHAPE_CACHE = new ConcurrentHashMap<>();

	/** The blockstate-baked fallback (static properties delegate; replaced-key safety net). */
	private final BakedModel mFallbackModel;
	private final Params mParams;
	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;
	/** Sprite-baked quad cache per mask (full lists; per-side filtering at query time). */
	private final ConcurrentHashMap<Integer, List<BakedQuad>> mBakedCache = new ConcurrentHashMap<>();

	public GTWireBakedModel(BakedModel aFallbackModel, Params aParams) {
		this(aFallbackModel, aParams, defaultSpriteLookup());
	}

	public GTWireBakedModel(BakedModel aFallbackModel, Params aParams,
			Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup) {
		mFallbackModel = aFallbackModel;
		mParams = aParams;
		mSpriteLookup = aSpriteLookup;
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(
				net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(aSpriteId);
	}

	public Params params() {
		return mParams;
	}

	public BakedModel getFallbackModel() {
		return mFallbackModel;
	}

	// ---------------------------------------------------------------------------
	// quad dispatch
	// ---------------------------------------------------------------------------

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState aState, @Nullable Direction aSide, RandomSource aRand,
			ModelData aModelData, @Nullable RenderType aRenderType) {
		// two chunk layers: material quads on solid, insulation overlays on cutout; the
		// null pass (item render, breaking overlays) receives everything
		if (aRenderType != null && !aRenderType.equals(RenderType.solid()) && !aRenderType.equals(RenderType.cutout())) {
			return List.of();
		}
		int tMask = aState != null && aState.hasProperty(GTWireBlock.CONNECTIONS)
				? aState.getValue(GTWireBlock.CONNECTIONS) : ITEM_MASK;
		boolean tFiber = mParams.overlaySprite() != null; // the p11 laser branch (WireLaser :121-122)
		List<BakedQuad> tAll = mBakedCache.computeIfAbsent(tMask, tM -> tFiber
				? bakeShapes(planShapesFiber(mParams.diameterPx(), tM))
				: bakeShapes(planShapes(mParams.insulated(), mParams.diameterPx(), tM)));
		if (aSide == null) return tAll;
		List<BakedQuad> rOut = new ArrayList<>(tAll.size());
		for (BakedQuad tQuad : tAll) if (tQuad.getDirection() == aSide) rOut.add(tQuad);
		return rOut;
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState aState, RandomSource aRand, ModelData aData) {
		return ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout());
	}

	/** Bakes the planned shapes into atlas-sprite quads, split by chunk layer. */
	private List<BakedQuad> bakeShapes(List<Shape> aShapes) {
		List<BakedQuad> rQuads = new ArrayList<>(aShapes.size());
		for (Shape tShape : aShapes) {
			TextureAtlasSprite tSprite = mSpriteLookup.apply(spriteOf(tShape.kind()));
			if (tSprite == null) continue; // atlas gap: skip the quad instead of rendering garbage
			rQuads.add(bakeQuad(tShape, tSprite));
		}
		return rQuads;
	}

	/** The sprite id for a kind — the borrowed grayscale PNGs (lowercased paths). */
	public ResourceLocation spriteOf(SpriteKind aKind) {
		if (aKind == SpriteKind.WIRE) {
			return mParams.wireSprite() != null ? mParams.wireSprite()
					: new ResourceLocation("gt6", "block/wire_electric"); // legacy pair: the p7 placeholder texture
		}
		if (aKind == SpriteKind.FIBER_OVERLAY) {
			// the p11 laser overlay layer (WireLaser :121-122, untinted)
			return mParams.overlaySprite() != null ? mParams.overlaySprite()
					: new ResourceLocation("gt6", "block/iconsets/fiber_wire_overlay");
		}
		return new ResourceLocation("gt6", "block/iconsets/insulation_" + kindTail(aKind));
	}

	private static String kindTail(SpriteKind aKind) {
		return switch (aKind) {
			case INSULATION_FULL -> "full";
			case INSULATION_TINY -> "tiny";
			case INSULATION_SMALL -> "small";
			case INSULATION_MEDIUM -> "medium";
			case INSULATION_LARGE -> "large";
			default -> "huge";
		};
	}

	/** The CoverPlateModel/pipe recipe over FaceBakery (model space 0..16). */
	private static BakedQuad bakeQuad(Shape aShape, TextureAtlasSprite aSprite) {
		double[] tBox = aShape.box();
		Vector3f tFrom = new Vector3f((float) tBox[0] * 16, (float) tBox[1] * 16, (float) tBox[2] * 16);
		Vector3f tTo = new Vector3f((float) tBox[3] * 16, (float) tBox[4] * 16, (float) tBox[5] * 16);
		float[] tUv = uvOf(aShape.face(), tBox[0] * 16, tBox[1] * 16, tBox[2] * 16, tBox[3] * 16, tBox[4] * 16, tBox[5] * 16);
		return BAKERY.bakeQuad(tFrom, tTo,
				new BlockElementFace(aShape.cull(), aShape.tintIndex(), aSprite.contents().name().toString(),
						new BlockFaceUV(tUv, 0)),
				aSprite, aShape.face(), BlockModelRotation.X0_Y0, null, true, aSprite.contents().name());
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

	// ---------------------------------------------------------------------------
	// planner (offline-testable: pure geometry over (insulated, diameter, mask))
	// ---------------------------------------------------------------------------

	/**
	 * The mask → box plans. {@code aMask} bit i = {@link Direction#get3DDataValue()} == i
	 * connected (the port's side order — GTWireBlockEntity writes Direction indices; upstream
	 * numbers sides X-/Y-/Z-/X+/Y+/Z+, the same six axes in a different arithmetic, and every
	 * bit flows through the same Direction mapping at the write site).
	 */
	public static List<Shape> planShapes(boolean aInsulated, int aDiameterPx, int aMask) {
		long tKey = (aInsulated ? 1L : 0L) | ((long) (aDiameterPx & 0x1F) << 1) | ((long) (aMask & 0x3F) << 8);
		return SHAPE_CACHE.computeIfAbsent(tKey, tK -> planShapesUncached(aInsulated, aDiameterPx, aMask));
	}

	/**
	 * The p11 LASER family form (MultiTileEntityWireLaser :121-122): the BARE geometry —
	 * upstream registers exactly one fiber wire, no cable form (Loader:1814-1815) — with
	 * one untinted {@link SpriteKind#FIBER_OVERLAY} twin per material quad. Upstream picks
	 * the SAME pair for {@code getTextureSide} AND {@code getTextureConnected} (:121/:122),
	 * so core, arm caps and arm side walls all carry the overlay — the twins mirror the
	 * base quads one-to-one (same face, same cull, {@link #INSULATION_EPSILON}-inflated
	 * box, tint index -1 = no dye, the {@code BlockTextureDefault} no-colour argument).
	 * Builds on the uncached bare plan (the SHAPE_CACHE lists are shared and must stay
	 * unmutated); the fiber result caches under its own bit-7 key.
	 */
	public static List<Shape> planShapesFiber(int aDiameterPx, int aMask) {
		long tKey = 1L << 7 | ((long) (aDiameterPx & 0x1F) << 1) | ((long) (aMask & 0x3F) << 8); // bit 7 = the fiber namespace
		return SHAPE_CACHE.computeIfAbsent(tKey, tK -> {
			List<Shape> rShapes = planShapesUncached(false, aDiameterPx, aMask);
			List<Shape> tTwins = new ArrayList<>(rShapes.size());
			for (Shape tShape : rShapes) {
				if (tShape.kind() != SpriteKind.WIRE) continue; // a bare plan is all-WIRE; the filter is belt and suspenders
				tTwins.add(new Shape(tShape.face(), inflate(tShape.box()), -1, SpriteKind.FIBER_OVERLAY, tShape.cull()));
			}
			rShapes.addAll(tTwins);
			return rShapes;
		});
	}

	private static List<Shape> planShapesUncached(boolean aInsulated, int aDiameterPx, int aMask) {
		float tDiameter = Math.max(MIN_DIAMETER_PX / 16.0F, Math.min(1.0F, aDiameterPx / 16.0F)); // readFromNBT2 :64 clamp
		float tHalf = (1.0F - tDiameter) / 2.0F;
		List<Shape> rShapes = new ArrayList<>(64);
		SpriteKind tTier = insulationTier(tDiameter);

		// pass 0 — the core (TileEntityBase10ConnectorRendered :113-116, texture pick :138)
		double[] tCore = {tHalf, tHalf, tHalf, 1 - tHalf, 1 - tHalf, 1 - tHalf};
		if (!aInsulated) {
			addBox(rShapes, tCore, SpriteKind.WIRE, 0, null, false);
		} else if (aMask == 0) {
			// the standalone getTextureConnected form: material + tier overlay
			addBox(rShapes, tCore, SpriteKind.WIRE, 0, null, false);
			addBox(rShapes, tCore, tTier, 1, null, true);
		} else {
			addBox(rShapes, tCore, SpriteKind.INSULATION_FULL, 1, null, false);
		}

		// passes 1-6 — the connection arms (:120-133 boxes, :139 texture pick, tLength = 0)
		for (int tBit = 0; tBit < 6; tBit++) {
			if ((aMask & (1 << tBit)) == 0) continue;
			Direction tDir = Direction.from3DDataValue(tBit);
			double[] tArm = armBox(tDir, tHalf);
			for (Direction tFace : Direction.values()) {
				if (tFace == tDir.getOpposite()) continue; // :139 — the buried face renders null
				if (tFace == tDir) {
					// the outer cap: getTextureConnected = material [+ tier overlay for cables]
					rShapes.add(new Shape(tFace, tArm, 0, SpriteKind.WIRE, tDir));
					if (aInsulated) rShapes.add(new Shape(tFace, inflate(tArm), 1, tTier, tDir));
				} else {
					// the side walls: getTextureSide = material (bare) / INSULATION_FULL (cable)
					rShapes.add(new Shape(tFace, tArm, aInsulated ? 1 : 0,
							aInsulated ? SpriteKind.INSULATION_FULL : SpriteKind.WIRE, null));
				}
			}
		}
		return rShapes;
	}

	/** The upstream :238 insulation ladder (diameters are PX_P/16 fractions). */
	public static SpriteKind insulationTier(float aDiameter) {
		if (aDiameter < 0.37F) return SpriteKind.INSULATION_TINY;
		if (aDiameter < 0.49F) return SpriteKind.INSULATION_SMALL;
		if (aDiameter < 0.74F) return SpriteKind.INSULATION_MEDIUM;
		if (aDiameter < 0.99F) return SpriteKind.INSULATION_LARGE;
		return SpriteKind.INSULATION_HUGE;
	}

	/**
	 * One arm box in 0..1 space — the :124-129 switch with tDiameter = the wire diameter and
	 * tLength = 0 (the arm spans from the core plane flush to the block boundary). Port side
	 * order = Direction 3D data values (DOWN, UP, NORTH, SOUTH, WEST, EAST).
	 */
	public static double[] armBox(Direction aDir, float aHalf) {
		return switch (aDir) {
			case DOWN  -> new double[] {aHalf, 0, aHalf, 1 - aHalf, aHalf, 1 - aHalf}; // SIDE_Y_NEG :125
			case UP    -> new double[] {aHalf, 1 - aHalf, aHalf, 1 - aHalf, 1, 1 - aHalf}; // SIDE_Y_POS :128
			case NORTH -> new double[] {aHalf, aHalf, 0, 1 - aHalf, 1 - aHalf, 1 - aHalf}; // SIDE_Z_NEG :126
			case SOUTH -> new double[] {aHalf, aHalf, 1 - aHalf, 1 - aHalf, 1 - aHalf, 1}; // SIDE_Z_POS :129
			case WEST  -> new double[] {0, aHalf, aHalf, aHalf, 1 - aHalf, 1 - aHalf}; // SIDE_X_NEG :124
			case EAST  -> new double[] {1 - aHalf, aHalf, aHalf, 1, 1 - aHalf, 1 - aHalf}; // SIDE_X_POS :127
		};
	}

	/** Six faces of one box, UVs over the box bounds (the RenderHelper box mapping form). */
	private static void addBox(List<Shape> aOut, double[] aBox, SpriteKind aKind, int aTintIndex, Direction aCull,
			boolean aInflate) {
		double[] tBox = aInflate ? inflate(aBox) : aBox;
		for (Direction tFace : Direction.values()) aOut.add(new Shape(tFace, tBox, aTintIndex, aKind, aCull));
	}

	/** The z-fight inflation for the overlay quads (all axes, the CoverPlateModel epsilon form). */
	private static double[] inflate(double[] aBox) {
		double tE = INSULATION_EPSILON;
		return new double[] {aBox[0] - tE, aBox[1] - tE, aBox[2] - tE, aBox[3] + tE, aBox[4] + tE, aBox[5] + tE};
	}

	// ---------------------------------------------------------------------------
	// static BakedModel face — delegate to the fallback (the GTDynamicBakedModel form)
	// ---------------------------------------------------------------------------

	@Override
	public boolean useAmbientOcclusion() {
		return mFallbackModel.useAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return mFallbackModel.isGui3d();
	}

	@Override
	public boolean usesBlockLight() {
		return mFallbackModel.usesBlockLight();
	}

	@Override
	public boolean isCustomRenderer() {
		return mFallbackModel.isCustomRenderer();
	}

	@Override
	public ItemTransforms getTransforms() {
		return mFallbackModel.getTransforms();
	}

	@Override
	public ItemOverrides getOverrides() {
		return mFallbackModel.getOverrides();
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		TextureAtlasSprite tSprite = mSpriteLookup.apply(spriteOf(SpriteKind.WIRE));
		return tSprite != null ? tSprite : mFallbackModel.getParticleIcon();
	}
}
