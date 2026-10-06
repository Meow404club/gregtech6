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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.client.model.data.ModelData;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTBlockProperties;
import gregtech6.block.energy.GTAxleBlock;

/**
 * The connection-aware rod baked model (task rod-render-pool) — the non-full-block
 * connector families (fluid pipes / item pipes / the logistics wire / the 44 axles)
 * trade the full-cube placeholder for the upstream connector geometry: pass 0 = the
 * core box of the row diameter, passes 1-6 = one extension arm per connected side —
 * the {@link gregtech6.client.wire.GTWireBakedModel} plan (the ported
 * TileEntityBase10ConnectorRendered :113-133 forms, arm length 0) with the pipe
 * texture semantics instead of the wire's insulation ladder.
 *
 * <p>ADR ⑨ posture (the wire model's world-persistent visual red line): a pure state
 * function of the BlockState — the mask lives in {@link GTBlockProperties#CONNECTIONS}
 * (0..63, bit i = {@code Direction#get3DDataValue()} i connected, the shared GT6 side
 * order — upstream numbers the same six axes, CS.java:516-521) or, for the axles, in
 * {@link BlockStateProperties#AXIS} (the straight-line port-ism: both axis ends
 * connected — upstream {@code worldObj == null} defaults to the same N-S segment,
 * ConnectorStraight :37, {@link #ITEM_MASK}). NEVER a ModelData/BE read for geometry.
 *
 * <p>The dye rides {@link GTMachinePaintTint} — the pipe connector families already
 * resolve there (the tex-pipe-textures arms), so this model multiplies
 * {@code tintARGB(modelData, tintMaterialOf(block), 0)} into the tintindex-0 body quads
 * at query time (the {@link GTMachineTintModel} vertex-colour route, tintIndex flipped
 * to -1 on the retinted copies) and passes overlays through untinted. Task
 * r11a-pipe-tint-gate-fix — the pipe arm is UNCONDITIONAL (the axle arm below, and the
 * GTMachineTintModel :100-102 wrap form, are the precedents): an UNPAINTED plain pipe BE
 * hands EMPTY ModelData in (TileEntityBase03TicksAndSync.getModelData), so the former
 * ModelData-emptiness gate never opened and the raw grayscale art rendered white (the
 * user report); the row material inside tintARGB IS the unpainted identity, and the ITEM
 * render (state null) still passes raw — the registered {@link GTItemPaintTint} ItemColor
 * tints the inventory form exactly once. The material-less logistics wire keeps the -1
 * white identity (tintQuads returns the input unchanged).
 *
 * <p>Task axle-tint-arm — the AXLE rows join the same dye on the CONSUMER side
 * ({@link GTMachinePaintTint} zero-touch): the listener still seats them with
 * {@code block == null}, so the arm keys on the state's {@link GTAxleBlock} carrier and
 * resolves the row material through {@link #axleMaterialOf} (the bySlug table plus the
 * three-row wood/alloy tail) into the SAME {@code tintARGB} decision site. No ModelData
 * gate here — an UNPAINTED axle BE hands EMPTY in (TileEntityBase03TicksAndSync
 * .getModelData), and the row colour IS the unpainted identity (upstream renders
 * {@code BlockTextureDefault(colored, mRGBa)}); a spray-painted axle's PAINT snapshot
 * still wins inside tintARGB. The inventory form stays raw (state null → tintindex 0)
 * and tints exactly once through the registered {@link #axleRowTintARGB} ItemColor
 * (GTClientHandlers, the explicit-registration face — a BlockColor does not colour its
 * BlockItem).
 *
 * <p>Texture semantics (TextureSet.java:145-181 two-pass form): the grayscale
 * {@code materialicons/<set>/pipe_side} art carries the material colour through tint
 * index 0; the untinted {@code pipe_side_overlay} black outline (and the restrictive
 * rows' second {@code pipe_restrictor} decal band, the upstream third render pass
 * MultiTileEntityPipeItem.java:280) rides as inflated twins (tint -1) one epsilon step
 * above every base quad — the JSON {@code tintedPipeModel} band form carried into the
 * baked model. Quads split by chunk layer exactly like the wire model (everything
 * opaque-or-cutout; the JSON fallback already declares cutout for the pipe rows, so the
 * forwarded getRenderTypes keeps the whole set on the layer the blockstate picked).
 *
 * <p>Item form (state == null) renders the upstream {@code worldObj == null} default
 * mask {@code SBIT_S|SBIT_N = 12} (ConnectorStraight :37 — a straight N-S segment, the
 * {@link gregtech6.client.wire.GTWireBakedModel#ITEM_MASK} anchor).
 */
public class GTRodBakedModel extends GTDynamicBakedModel {

	/** The upstream {@code worldObj == null} straight N-S segment (ConnectorStraight :37). */
	public static final int ITEM_MASK = 12;

	/** The z-fight epsilon (the wire model's INSULATION_EPSILON value, per-band stepped). */
	public static final double OVERLAY_EPSILON = 0.002;

	/** The upstream diameter clamp floor PX_P[2] (the wire model's readFromNBT2 :64 form). */
	public static final float MIN_DIAMETER_PX = 2.0F;

	/** Which sprite a planned face carries: the tinted core art, the tinted per-diameter
	 * arm art, or the j-th untinted overlay band. */
	public enum SpriteKind { BASE, ARM, OVERLAY }

	/**
	 * One planned face quad: the facing (also the per-side chunk dispatch key), the box
	 * in 0..1 block space, the tint index (0 = material colour, -1 = none), the sprite
	 * kind and the overlay band (0 when {@code kind == BASE}). Box copy guard (the
	 * FlowQuad discipline).
	 */
	public record Shape(Direction face, double[] box, int tintIndex, SpriteKind kind, int band,
			@Nullable Direction cull) {
		public Shape {
			box = box.clone();
		}
	}

	/**
	 * The immutable per-row render identity: sprite ids + the PX_P diameter of the row.
	 * Task pipe-render-closeout — the connected arms ride the per-diameter art
	 * ({@code arm}; upstream {@code getIconIndexConnected}, TileEntityBase10ConnectorRendered
	 * :265), the core the pipeSide art ({@code base}; :264). The 3-arg form keeps the
	 * single-art families (axles, the logistics wire, the flow-arrow model) on the old
	 * shape — arm = base.
	 */
	public record Params(ResourceLocation base, ResourceLocation arm, List<ResourceLocation> overlays, int diameterPx) {
		public Params(ResourceLocation aBase, List<ResourceLocation> aOverlays, int aDiameterPx) {
			this(aBase, aBase, aOverlays, aDiameterPx);
		}
	}

	private static final FaceBakery BAKERY = new FaceBakery();

	/** Offline geometry plan cache — key = (bands << 11) | (diameterPx << 6) | mask. */
	private static final Map<Long, List<Shape>> SHAPE_CACHE = new ConcurrentHashMap<>();

	private final Params mParams;
	/**
	 * The tint carrier (the pipe/wire blocks — the combined dispatch gate); null = the
	 * axle rows, whose row material resolves consumer-side off the state's
	 * {@link GTAxleBlock} carrier (task axle-tint-arm).
	 */
	@Nullable
	private final Block mBlock;
	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;
	/** Sprite-baked quad cache per mask (full lists; per-side filtering at query time). */
	private final ConcurrentHashMap<Integer, List<BakedQuad>> mBakedCache = new ConcurrentHashMap<>();
	/** The retinted-copy tables per tint colour (the GTMachineTintModel cache form). */
	private final Map<Integer, Map<BakedQuad, BakedQuad>> mTintedQuads = new ConcurrentHashMap<>();

	public GTRodBakedModel(BakedModel aFallbackModel, Params aParams, @Nullable Block aBlock) {
		this(aFallbackModel, aParams, aBlock, defaultSpriteLookup());
	}

	public GTRodBakedModel(BakedModel aFallbackModel, Params aParams, @Nullable Block aBlock,
			Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup) {
		super(aFallbackModel);
		mParams = aParams;
		mBlock = aBlock;
		mSpriteLookup = aSpriteLookup;
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(
				net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(aSpriteId);
	}

	public Params params() {
		return mParams;
	}

	// ---------------------------------------------------------------------------
	// quad dispatch
	// ---------------------------------------------------------------------------

	/** The rod geometry is unconditional — the item form renders the N-S segment too. */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return true;
	}

	@Override
	public List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		// the wire model's layer posture: everything rides the layers the fallback
		// declares (cutout for the pipe rows, solid for the axles); the null pass (item
		// render, breaking overlays) receives everything
		if (aRenderType != null && !aRenderType.equals(RenderType.solid()) && !aRenderType.equals(RenderType.cutout())) {
			return List.of();
		}
		int tMask = maskOf(aState);
		List<BakedQuad> tAll = mBakedCache.computeIfAbsent(tMask,
				tM -> bakeShapes(planShapes(mParams.diameterPx(), tM, mParams.overlays().size())));
		// task axle-tint-arm — the axle arm: the state's GTAxleBlock carrier resolves the
		// row material (the consumer-side slug dispatch, axleMaterialOf) into the same
		// tintARGB decision site. Deliberately NO ModelData gate: an UNPAINTED axle BE
		// hands EMPTY in (TileEntityBase03TicksAndSync.getModelData) and the row colour is
		// the unpainted identity; a painted axle's PAINT snapshot wins inside tintARGB
		// (the spray override). The item form (state null) falls through raw — the
		// axleRowTintARGB ItemColor tints the inventory form exactly once.
		if (aState != null && mBlock == null && aState.getBlock() instanceof GTAxleBlock tAxle) {
			return GTMachineTintModel.tintQuads(tAll,
					GTMachinePaintTint.tintARGB(aModelData, axleMaterialOf(tAxle), 0), mTintedQuads);
		}
		// the pipe tint arm — UNCONDITIONAL (task r11a-pipe-tint-gate-fix, the :192 axle
		// precedent): an UNPAINTED plain pipe BE hands EMPTY in (TileEntityBase03TicksAndSync
		// .getModelData), so the former ModelData-emptiness gate never opened and the raw
		// grayscale pipe_side rendered WHITE (the user report). The material fallback inside
		// tintARGB IS the unpainted identity (upstream renders BlockTextureDefault(colored,
		// mRGBa)); a painted pipe's PAINT snapshot still wins there (the spray override).
		// The item render (state null) falls through raw — tintindex-0 quads, the
		// GTItemPaintTint ItemColor tints the inventory form exactly once; the logistics
		// wire resolves materialOf = null → tint -1 → tintQuads returns the list unchanged
		// (the white identity, byte-identical).
		if (aState != null && mBlock != null) {
			return GTMachineTintModel.tintQuads(tAll,
					GTMachinePaintTint.tintARGB(aModelData, GTMachinePaintTint.tintMaterialOf(mBlock), 0), mTintedQuads);
		}
		return tAll;
	}

	/**
	 * The axle row material (task axle-tint-arm, the CONSUMER-side slug dispatch —
	 * {@link GTMachinePaintTint} zero-touch): 8 of the 11 row slugs already live in the
	 * {@link GTMachinePaintTint#bySlug} table (the shared GT6Boilers slug conventions —
	 * ANY.Steel → {@code "steel"}, MT.Ti → {@code "titanium"}, MT.Ir → {@code "iridium"}),
	 * the wood/alloy tail is the axle-local arm: the wooden rows Loader :1662-1666
	 * (MT.WoodTreated), the Iritanium alloy row {@code titanium_iridium} (MT.Iritanium,
	 * GT6Kinetics.AXLE_SPECS), Trinitanium :1748-1752.
	 */
	public static OreDictMaterial axleMaterialOf(GTAxleBlock aAxle) {
		OreDictMaterial tMaterial = GTMachinePaintTint.bySlug(aAxle.spec.material());
		if (tMaterial != null) return tMaterial;
		return switch (aAxle.spec.material()) {
			case "wood_treated" -> MT.WoodTreated;
			case "titanium_iridium" -> MT.Iritanium;
			case "trinitanium" -> MT.Trinitanium;
			default -> null;
		};
	}

	/**
	 * The axle row tint, the INVENTORY half (the ItemColor face GTClientHandlers registers
	 * over {@link gregtech6.registry.GT6Kinetics#AXLE_ITEMS}): index 0 = the row material's
	 * fRGBaSolid through the single {@link GTMachinePaintTint#tintARGB} decision site (the
	 * axle drops carry no paint NBT — the unpainted row colour is the whole item face);
	 * every other index, and any off-family stack, the -1 sentinel. Explicit registration
	 * is mandatory — a BlockColor does not colour its BlockItem AND the baked world arm
	 * cannot colour the creative-tab face (the GTItemPaintTint doc face).
	 */
	public static int axleRowTintARGB(ItemStack aStack, int aTintIndex) {
		if (aTintIndex != 0 || !(aStack.getItem() instanceof BlockItem tItem)
				|| !(tItem.getBlock() instanceof GTAxleBlock tAxle)) return -1;
		return GTMachinePaintTint.tintARGB(null, axleMaterialOf(tAxle), 0);
	}

	/** The state → 6-bit mask map: CONNECTIONS verbatim, AXIS as the straight line, null = N-S. */
	public static int maskOf(@Nullable BlockState aState) {
		if (aState == null) return ITEM_MASK;
		if (aState.hasProperty(GTBlockProperties.CONNECTIONS)) return aState.getValue(GTBlockProperties.CONNECTIONS);
		if (aState.hasProperty(BlockStateProperties.AXIS)) {
			return switch (aState.getValue(BlockStateProperties.AXIS)) {
				case X -> (1 << Direction.WEST.get3DDataValue()) | (1 << Direction.EAST.get3DDataValue()); // 48
				case Y -> (1 << Direction.DOWN.get3DDataValue()) | (1 << Direction.UP.get3DDataValue()); // 3
				case Z -> (1 << Direction.NORTH.get3DDataValue()) | (1 << Direction.SOUTH.get3DDataValue()); // 12
			};
		}
		return ITEM_MASK;
	}

	// ---------------------------------------------------------------------------
	// baking
	// ---------------------------------------------------------------------------

	/** Bakes the planned shapes into atlas-sprite quads (the wire model's bakeShapes). */
	private List<BakedQuad> bakeShapes(List<Shape> aShapes) {
		List<BakedQuad> rQuads = new ArrayList<>(aShapes.size());
		for (Shape tShape : aShapes) {
			TextureAtlasSprite tSprite = mSpriteLookup.apply(spriteOf(tShape.kind(), tShape.band()));
			if (tSprite == null) continue; // atlas gap: skip the quad instead of rendering garbage
			rQuads.add(bakeQuad(tShape, tSprite));
		}
		return rQuads;
	}

	/** The sprite id for a kind/band — the borrowed grayscale PNGs (lowercased paths). */
	public ResourceLocation spriteOf(SpriteKind aKind, int aBand) {
		if (aKind == SpriteKind.ARM) return mParams.arm();
		if (aKind == SpriteKind.BASE || aBand >= mParams.overlays().size()) {
			return mParams.base();
		}
		return mParams.overlays().get(aBand);
	}

	/** The CoverPlateModel/wire recipe over FaceBakery (model space 0..16). */
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

	/** The GTCEu StaticFaceBakery.bakeFace cubeUV switch (the wire model's uvOf verbatim). */
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
	// planner (offline-testable: pure geometry over (diameter, mask, bands))
	// ---------------------------------------------------------------------------

	/**
	 * The mask → box plans. {@code aMask} bit i = {@link Direction#get3DDataValue()} == i
	 * connected. Pass 0 = the core diameter box (TileEntityBase10ConnectorRendered
	 * :113-116); passes 1-6 = one arm per connected side (each arm spans from the core
	 * plane flush to the block boundary with the same cross-section — the port arm length
	 * 0, the wire model's armBox form); every base quad then gets one inflated untinted
	 * twin per overlay band (the two-pass texture stack, TextureSet.java:145-181). Task
	 * pipe-render-closeout — the arm quads carry {@link SpriteKind#ARM} (the per-diameter
	 * connected art, :265), the core {@link SpriteKind#BASE} (:264).
	 */
	public static List<Shape> planShapes(int aDiameterPx, int aMask, int aBands) {
		long tKey = ((long) (aBands & 0x1F) << 11) | ((long) (aDiameterPx & 0x1F) << 6) | (aMask & 0x3FL);
		return SHAPE_CACHE.computeIfAbsent(tKey, tK -> {
			List<Shape> tBase = planBaseShapes(aDiameterPx, aMask);
			if (aBands <= 0) return tBase;
			// the untinted overlay twins — band j inflates by (j+1) * OVERLAY_EPSILON (the
			// JSON tintedPipeModel's stepped offsets carried into the baked form); built
			// into a fresh list (iterating tBase while appending would CME)
			List<Shape> rShapes = new ArrayList<>(tBase.size() * (aBands + 1));
			rShapes.addAll(tBase);
			for (int tBand = 0; tBand < aBands; tBand++) {
				double tE = OVERLAY_EPSILON * (tBand + 1);
				for (Shape tShape : tBase) {
					double[] tBox = tShape.box();
					rShapes.add(new Shape(tShape.face(),
							new double[] {tBox[0] - tE, tBox[1] - tE, tBox[2] - tE, tBox[3] + tE, tBox[4] + tE, tBox[5] + tE},
							-1, SpriteKind.OVERLAY, tBand, tShape.cull()));
				}
			}
			return rShapes;
		});
	}

	/** The bare core + arms plan (no overlay twins). */
	private static List<Shape> planBaseShapes(int aDiameterPx, int aMask) {
		float tDiameter = Math.max(MIN_DIAMETER_PX / 16.0F, Math.min(1.0F, aDiameterPx / 16.0F)); // readFromNBT2 :64 clamp
		float tHalf = (1.0F - tDiameter) / 2.0F;
		List<Shape> rShapes = new ArrayList<>(64);

		// pass 0 — the core (:113-116)
		addBox(rShapes, new double[] {tHalf, tHalf, tHalf, 1 - tHalf, 1 - tHalf, 1 - tHalf});

		// passes 1-6 — the connection arms (:120-133 boxes, tLength = 0)
		for (int tBit = 0; tBit < 6; tBit++) {
			if ((aMask & (1 << tBit)) == 0) continue;
			Direction tDir = Direction.from3DDataValue(tBit);
			double[] tArm = armBox(tDir, tHalf);
			for (Direction tFace : Direction.values()) {
				if (tFace == tDir.getOpposite()) continue; // :139 — the buried face renders null
				rShapes.add(new Shape(tFace, tArm, 0, SpriteKind.ARM, 0, tFace == tDir ? tDir : null));
			}
		}
		return rShapes;
	}

	/**
	 * One arm box in 0..1 space — the :124-129 switch with tDiameter = the row diameter
	 * and tLength = 0 (the arm spans from the core plane flush to the block boundary).
	 * Port side order = Direction 3D data values (DOWN, UP, NORTH, SOUTH, WEST, EAST).
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
	private static void addBox(List<Shape> aOut, double[] aBox) {
		for (Direction tFace : Direction.values()) aOut.add(new Shape(tFace, aBox, 0, SpriteKind.BASE, 0, null));
	}

	// ---------------------------------------------------------------------------
	// static BakedModel face: delegated by GTDynamicBakedModel to the fallback
	// ---------------------------------------------------------------------------
}
