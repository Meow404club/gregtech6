package gregtech6.client.ore;

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
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.block.ore.GTOreBlock;
import gregtech6.client.render.GTMachineTintModel;
import gregtech6.client.wire.GTWireBakedModel;
import gregtech6.client.wire.GTWireTextures;
import gregtech6.registry.GT6OreBlocks;

/**
 * The dual-sprite ore baked model (task p30-ore-3-datagen spec ①, the first-choice route):
 * the direct translation of the upstream ore render stack — a copied STONE base plus the
 * material's texture-set ORE overlay coloured with {@code fRGBa[prefix.mState]}
 * (PrefixBlock.java:294 {@code BlockTextureMulti(mTexture, BlockTextureDefault.get(aMaterial,
 * mPrefix))} — {@code mTexture} is the stone base, the overlay carries the colour; the
 * {@code getRenderColor :279-282} fRGBa encoding already lives in the port as the
 * GTMaterialPrefixBlock/GTMaterialPrefixBlockItem tint seams). Every visible face renders
 * as TWO stacked quads: the base stone sprite untinted WITH cullface (the copied-icon
 * semantics — vanilla cobblestone next door culls identically) and the overlay sprite
 * {@link GTWireBakedModel#INSULATION_EPSILON}-inflated WITHOUT cull on the cutout layer
 * (the SET grayscale PNGs carry alpha; the GTCEu COVER_OVERLAY z-fight epsilon, the
 * GTWireBakedModel insulation-layer form).
 *
 * <p>JSON discipline (the task-card anti-bloat pin): the blockstate/model JSON face stays
 * SHARED — one placeholder cube per distinct BASE texture ({@code GT6OreBlockStates}, 28
 * models over the 3922 blocks) — and this model does the dual-sprite work at bake time, so
 * the wave never emits a per-pair (red line) or even per-(base,SET) model JSON. The
 * placeholder models still reference the base texture (atlas stitching + a graceful
 * pre-wrap fallback); the OVERLAY sprites are referenced from NO JSON model, so they are
 * stitched explicitly via the atlas sources ({@code GTOreAtlasesSources} contribution in
 * GT6Atlases — the consumer-side wiring the GT6Atlases javadoc prescribes). The overlay
 * PNGs themselves are card ②'s borrow face: until that card lands, the overlay layer
 * renders the missingno checkerboard — the declared ADR ④ intermediate state.
 *
 * <p>Tint (task p38-issue2-ore-baked-tint, the p32 machine-domain migration applied to the
 * ore domain): the material colour rides {@link Params#tintARGB()} — {@code
 * fRGBa[prefix.mState]} (PrefixBlock.java:279-282), the exact value the retired runtime
 * {@code BlockColor} resolved — and is BAKED into the overlay quads' vertex colours (the
 * {@link GTMachineTintModel#retintVertices} product). The p32 live evidence applies here
 * verbatim: the runtime {@code BlockColor} route rendered achromatic in a live client, so
 * the colour rides the model where no chunk builder can drop it. The retinted overlay quads
 * carry {@code tintIndex -1}, so no runtime lookup can multiply a second time; the base
 * stone stays untinted exactly like upstream (the {@code mTexture} half of the
 * BlockTextureMulti has no colour argument; tinting the stone too would double-dye the
 * ore). Offline-testable: {@link #buildParams()} and the sprite derivations are
 * registry-free.
 */
public class GTOreBakedModel implements IDynamicBakedModel {

	/** The z-fight epsilon of the overlay layer (shared constant with the wire insulation twins). */
	public static final double EPSILON = GTWireBakedModel.INSULATION_EPSILON;

	/**
	 * The immutable per-block render identity: the copied stone base + the SET ore overlay
	 * + the material's ore colour ({@code fRGBa[prefix.mState]}, PrefixBlock.java:279-282)
	 * baked into the overlay vertices.
	 */
	public record Params(ResourceLocation baseSprite, ResourceLocation overlaySprite, int tintARGB) {}

	private static final FaceBakery BAKERY = new FaceBakery();

	/** The baked fallback (the shared placeholder cube) — static-property delegate + particle ancestry. */
	private final BakedModel mFallbackModel;
	private final Params mParams;
	/**
	 * Sprite resolver — task p33-ore-overlay-impl 21.1 seam: MATERIAL-keyed on both legs.
	 * Runtime 1.20.1 Forge: the block atlas over the material's atlas location ({@link
	 * #defaultSpriteLookup()}); runtime 1.21.1 NeoForge: the bake-time {@code
	 * ModelEvent.ModifyBakingResult#getTextureGetter()} handed through the listener (the
	 * event-owned lookup — the research.p32-r-ore-overlay-render delta-4 — no static
	 * Minecraft dereference on the bake worker threads); tests: a stub.
	 */
	private final Function<Material, TextureAtlasSprite> mSpriteLookup;
	/**
	 * The 12 quads (6 base + 6 overlay), baked LAZILY at first {@link #getQuads} — task
	 * p33-fix-forge-ore-invisible. Eager constructor baking was the forge-leg whole-ore
	 * invisibility root cause: ModifyBakingResult fires BEFORE the sprite upload
	 * (ModelManager.java.patch — onModifyBakingResult precedes the dispatch/registry set)
	 * and its javadoc forbids touching ModelManager (ModelEvent.java:40-43), so the static
	 * atlas lookup resolved against the EMPTY atlas and baked 0 quads (baked models still
	 * had collision/tooltip/outline). The GTWireBakedModel mBakedCache lazy form is the
	 * in-repo precedent — all five sibling dynamic families resolve at render time and
	 * were never affected. Double-checked-locking volatile: getQuads runs concurrently on
	 * the chunk-build worker pool; resolution is idempotent and the result immutable.
	 */
	private volatile List<BakedQuad> mQuads;

	public GTOreBakedModel(BakedModel aFallbackModel, Params aParams) {
		this(aFallbackModel, aParams, defaultSpriteLookup());
	}

	public GTOreBakedModel(BakedModel aFallbackModel, Params aParams,
			Function<Material, TextureAtlasSprite> aSpriteLookup) {
		mFallbackModel = aFallbackModel;
		mParams = aParams;
		mSpriteLookup = aSpriteLookup;
	}

	/** The material of a sprite id: the blocks atlas + the id (the 1.20.1 lookup's payload split out). */
	public static Material materialOf(ResourceLocation aSpriteId) {
		return new Material(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS, aSpriteId);
	}

	private static Function<Material, TextureAtlasSprite> defaultSpriteLookup() {
		return aMaterial -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(
				aMaterial.atlasLocation()).apply(aMaterial.texture());
	}

	public Params params() {
		return mParams;
	}

	// ---------------------------------------------------------------------------
	// quad emission: the base cube (solid, culled) + the overlay shell (cutout)
	// ---------------------------------------------------------------------------

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState aState, @Nullable Direction aSide, RandomSource aRand,
			ModelData aModelData, @Nullable RenderType aRenderType) {
		// two chunk layers: base on solid, overlay on cutout; the null pass (item render,
		// breaking overlays) receives everything — the GTWireBakedModel dispatch form
		if (aRenderType != null && !aRenderType.equals(RenderType.solid()) && !aRenderType.equals(RenderType.cutout())) {
			return List.of();
		}
		List<BakedQuad> tQuads = quads();
		if (aSide == null) return tQuads;
		List<BakedQuad> rOut = new ArrayList<>(2);
		for (BakedQuad tQuad : tQuads) if (tQuad.getDirection() == aSide) rOut.add(tQuad);
		return rOut;
	}

	/** The lazy first-render bake (the double-checked-lock form of the field doc). */
	private List<BakedQuad> quads() {
		List<BakedQuad> tQuads = mQuads;
		if (tQuads == null) {
			synchronized (this) {
				tQuads = mQuads;
				if (tQuads == null) mQuads = tQuads = bakeQuads();
			}
		}
		return tQuads;
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState aState, RandomSource aRand, ModelData aData) {
		return ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout());
	}

	/** The full 0..1 cube and its epsilon-inflated overlay twin (the planShapesFiber twin form). */
	private List<BakedQuad> bakeQuads() {
		TextureAtlasSprite tBase = mSpriteLookup.apply(materialOf(mParams.baseSprite()));
		TextureAtlasSprite tOverlay = mSpriteLookup.apply(materialOf(mParams.overlaySprite()));
		List<BakedQuad> rQuads = new ArrayList<>(12);
		if (tBase != null) {
			double[] tCore = {0, 0, 0, 1, 1, 1};
			for (Direction tFace : Direction.values()) rQuads.add(bakeQuad(tFace, tCore, tBase, -1, tFace));
		}
		if (tOverlay != null) { // atlas gap: skip the layer instead of rendering garbage (the wire form)
			double[] tShell = {0 - EPSILON, 0 - EPSILON, 0 - EPSILON, 1 + EPSILON, 1 + EPSILON, 1 + EPSILON};
			int tTint = mParams.tintARGB();
			for (Direction tFace : Direction.values()) {
				// the p32 form: tintIndex -1 (no runtime lookup can double-dye), the colour
				// multiplied into the vertex data at bake time
				BakedQuad tQuad = bakeQuad(tFace, tShell, tOverlay, -1, null);
				rQuads.add(tTint == -1 ? tQuad : retinted(tQuad, tTint));
			}
		}
		return rQuads;
	}

	/** The bake-time material tint: the colour multiplied into the vertex data (the {@link GTMachineTintModel#retintVertices} product). */
	private static BakedQuad retinted(BakedQuad aQuad, int aTint) {
		return new BakedQuad(GTMachineTintModel.retintVertices(aQuad.getVertices(), aTint),
				aQuad.getTintIndex(), aQuad.getDirection(), aQuad.getSprite(), aQuad.isShade());
	}

	/** The CoverPlateModel/wire recipe over FaceBakery (model space 0..16, box-bounds UVs). */
	private static BakedQuad bakeQuad(Direction aFace, double[] aBox, TextureAtlasSprite aSprite, int aTintIndex,
			@Nullable Direction aCull) {
		Vector3f tFrom = new Vector3f((float) aBox[0] * 16, (float) aBox[1] * 16, (float) aBox[2] * 16);
		Vector3f tTo = new Vector3f((float) aBox[3] * 16, (float) aBox[4] * 16, (float) aBox[5] * 16);
		float[] tUv = uvOf(aFace, aBox[0] * 16, aBox[1] * 16, aBox[2] * 16, aBox[3] * 16, aBox[4] * 16, aBox[5] * 16);
		return BAKERY.bakeQuad(tFrom, tTo,
				new BlockElementFace(aCull, aTintIndex, aSprite.contents().name().toString(),
						new BlockFaceUV(tUv, 0)),
				aSprite, aFace, BlockModelRotation.X0_Y0, null, true, aSprite.contents().name());
	}

	// ---------------------------------------------------------------------------
	// static pure seams (sprite derivations + the param table) — registry-free
	// ---------------------------------------------------------------------------

	/** The family's stone base sprite (the copied rock texture); broken rides the broken anchor's rock. */
	public static ResourceLocation baseSpriteOf(GT6OreBlocks.OreFamily aFamily, GT6OreBlocks.FormKind aKind) {
		// the vanilla anchors: the rock texture = the vanilla block texture of the family's
		// anchor (the IconContainerCopied(Blocks.<rock>) upstream form) — stone/deepslate
		// broken = the cobble forms (GT6OreBlocks.OreFamily brokenAnchor rows)
		return switch (aFamily.snake()) {
			case "stone"      -> mcBlock(aKind == GT6OreBlocks.FormKind.BROKEN ? "cobblestone" : "stone");
			case "deepslate"  -> mcBlock(aKind == GT6OreBlocks.FormKind.BROKEN ? "cobbled_deepslate" : "deepslate");
			case "netherrack" -> mcBlock("netherrack");
			case "endstone"   -> mcBlock("end_stone");
			case "sandstone"  -> mcBlock("sandstone");
			case "gravel"     -> mcBlock("gravel");
			case "sand"       -> mcBlock("sand");
			case "redsand"    -> mcBlock("red_sand");
			case "mud"        -> mcBlock("mud");
			// the 17 GT stones: the GTStoneBlocks STONE-variant texture (the addStoneBlocks
			// model/texture path shape, GT6BlockStates.java:1824 — block id ≠ texture path,
			// the granite/prismarine splits mirror GT6OreBlocks.stoneBlockSnake)
			// (fromNamespaceAndPath: the concatenated arg escapes the 21.1 swap regex — the
			// GTWireBakedModel.spriteOf form, Forge 1.20.1 backported, both legs javap-proven)
			default -> ResourceLocation.fromNamespaceAndPath("gt6", "block/stones/" + stoneTextureSnake(aFamily.snake()) + "/stone");
		};
	}

	/** The family snake -> the GTStoneBlocks texture snake (the granite/prismarine naming splits). */
	public static String stoneTextureSnake(String aSnake) {
		return switch (aSnake) {
			case "blackgranite" -> "granite_black";
			case "redgranite" -> "granite_red";
			case "lightprismarine" -> "prismarine_light";
			case "darkprismarine" -> "prismarine_dark";
			default -> aSnake;
		};
	}

	/** The SET's ore overlay sprite; small ores ride {@code ore_small} (OreDictPrefix.java:399/408 naming, snaked). */
	public static ResourceLocation overlaySpriteOf(String aSetSnake, GT6OreBlocks.FormKind aKind) {
		// fromNamespaceAndPath: the concatenated arg escapes the 21.1 swap regex (the spriteOf form)
		return ResourceLocation.fromNamespaceAndPath("gt6", "block/materialicons/" + aSetSnake
				+ (aKind == GT6OreBlocks.FormKind.SMALL ? "/ore_small" : "/ore"));
	}

	/** The material's block texture-set name (the GTWireTextures single source, the addPrefixBlocks expression). */
	public static String setOf(OreDictMaterial aMaterial) {
		return GTWireTextures.blockSetOf(aMaterial);
	}

	/** The params of one registration key (the per-path dispatch fuel, mirror of the wire PARAMS table). */
	public static Params paramsOf(GT6OreBlocks.OreKey aKey) {
		return new Params(baseSpriteOf(aKey.family(), aKey.kind()),
				overlaySpriteOf(setOf(aKey.material()), aKey.kind()),
				tintARGBOf(aKey.material(), aKey.family().prefix(aKey.kind())));
	}

	/**
	 * The material's ore colour as opaque ARGB: {@code fRGBa[prefix.mState]} (PrefixBlock.java:279-282
	 * {@code getRenderColor}, the UT.Code.getRGBInt encoding — the same value the retired
	 * runtime BlockColor and the {@link GTOreClientListener#oreTintARGB} pure seam resolve).
	 */
	public static int tintARGBOf(OreDictMaterial aMaterial, OreDictPrefix aPrefix) {
		short[] tRGBa = aMaterial.fRGBa[aPrefix.mState];
		return 0xFF000000 | (bind8(tRGBa[0]) << 16) | (bind8(tRGBa[1]) << 8) | bind8(tRGBa[2]);
	}

	/** Upstream UT.Code.bind8 semantics: clamp to 0-255. */
	private static int bind8(long aValue) {
		return (int) Math.max(0, Math.min(255, aValue));
	}

	/** The path -> params table over the whole registration walk (offline-safe, test-driven). */
	public static Map<String, Params> buildParams() {
		Map<String, Params> rTable = new ConcurrentHashMap<>();
		for (GT6OreBlocks.OreKey tKey : GT6OreBlocks.registrationOrder()) {
			rTable.put(GT6OreBlocks.path(tKey), paramsOf(tKey));
		}
		return rTable;
	}

	/**
	 * Every overlay sprite the baked models can look up, ordered distinct — the atlas-source
	 * list GT6Atlases consumes (the consumer-side stitching wiring; missing until card ②
	 * lands the PNGs, the declared intermediate state).
	 */
	public static List<ResourceLocation> overlaySprites() {
		List<ResourceLocation> rList = new ArrayList<>();
		java.util.Set<String> tSeen = new java.util.HashSet<>();
		for (GT6OreBlocks.OreKey tKey : GT6OreBlocks.registrationOrder()) {
			String tSet = setOf(tKey.material());
			if (tSeen.add(tSet)) {
				rList.add(overlaySpriteOf(tSet, GT6OreBlocks.FormKind.NORMAL));
				rList.add(overlaySpriteOf(tSet, GT6OreBlocks.FormKind.SMALL));
			}
		}
		return rList;
	}

	// ---------------------------------------------------------------------------
	// static BakedModel face — delegate to the fallback (the GTWireBakedModel form)
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
		TextureAtlasSprite tSprite = mSpriteLookup.apply(materialOf(mParams.baseSprite()));
		return tSprite != null ? tSprite : mFallbackModel.getParticleIcon();
	}

	/** The vanilla-namespaced block texture id. */
	private static ResourceLocation mcBlock(String aPath) {
		return new ResourceLocation("minecraft", "block/" + aPath);
	}

	/** The GTCEu StaticFaceBakery.bakeFace cubeUV switch (GTWireBakedModel.uvOf verbatim — private there). */
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
