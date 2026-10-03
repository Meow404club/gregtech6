package gregtech6.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregtech6.block.tools.GTAnvilBlock;
import gregtech6.gui.GTViewerJump;
import gregtech6.registry.GT6Anvils;

/**
 * The anvil NEI legs-band glyph (task manual-nei-four-family) — the
 * {@link GT6KitchenNeiModel} sibling: the upstream anvil drew {@code BI.nei()} on render
 * pass 5, a 2x2px tile at the FOOT of the two along-axis faces (MultiTileEntityAnvil
 * .java:343-352 the pass-5 box table — a 2px rail at Y 0..2 spanning the base depth,
 * inset PX_P[2] from the facing-side edge; :386 case 5 the {@code ALONG_AXIS[mFacing]}
 * gate), while its client arm opened {@code RM.Anvil} from the lower 4px band (:271-272
 * {@code aHitY < PX_P[4]}, any face — the jump arm lives on
 * {@code GT6AnvilBlockEntity.openNei}). The patch is a BAKE-TIME append over the anvil
 * rows' static models, gated by the ONE viewer predicate the jump route uses —
 * {@link GTViewerJump#preferredViewer()} — so the glyph is viewer-dynamic: EMI present →
 * the 「EMI」 tile, JEI only → 「JEI」, neither → NO patch at all (what you see is what
 * you jump to). LIKE the kitchen sibling, the patch is a bake artifact and NOT a datagen
 * product: the visibility gate is per-user (the client's ModList at resource load) and
 * the shipped blockstate JSON is fixed; the glyph tiles are the kitchen's own
 * viewer-dynamic pair ({@code kitchen_nei_jei/emi.png}) — zero new assets.
 *
 * <p>Layer seat: cutout joins the fallback exactly when the glyph exists (the transparent
 * glyph margins must discard, the r11-oven-solid-layer-fix lesson). No tint interplay:
 * the anvil body is the plain {@code cubeAll} placeholder (the upstream anvil silhouette
 * is the r8-tex-placeholder-audit render-pool defer), so the fallback quads pass through
 * verbatim and the glyph keeps the upstream px table — when the real silhouette swap
 * lands, the foot-band position stays put.
 *
 * <p>Geometry per FACING (the pass-5 box table verbatim, onto the full-cube carrier):
 * the negative facings (NORTH/WEST) ride the PX_P[2] min-edge arm, the positive ones
 * (SOUTH/EAST) the PX_P[12] max-edge arm; the glyph plane protrudes {@link #LIFT_PX} off
 * the carrier face (the upstream ±0.0001 expansion, folded to the kitchen lift scale).
 *
 * <p>CLIENT-only by registration ({@code Dist.CLIENT} listener — the dedicated server
 * never loads this class; the offline pins drive the test ctor directly, the
 * GT6KitchenNeiModelTest shape).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GT6AnvilNeiModel extends GTDynamicBakedModel {

	/** The glyph tile footprint — the upstream 2x2px box face (:343-352). */
	public static final float PATCH_PX = 2.0F;
	/** The foot band's top — the upstream box Y 0..2 (:344 {@code PX_P[0]}..{@code PX_P[2]}). */
	public static final float BAND_TOP_PX = 2.0F;
	/** The edge inset — upstream {@code PX_P[2]} = 2px (the min-edge arm; the max arm mirrors to 16-2-2). */
	public static final float EDGE_INSET_PX = 2.0F;
	/** The face-plane protrusion — the upstream ±0.0001 box expansion, the kitchen NEI_LIFT scale. */
	public static final float LIFT_PX = 0.001F;

	private static final FaceBakery BAKERY = new FaceBakery();

	private final Map<Direction, List<BakedQuad>> mGlyphs = new ConcurrentHashMap<>();
	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;
	/** The viewer probe — evaluated at bake time; the same predicate the jump route consumes. */
	private final Supplier<String> mViewerProbe;

	public GT6AnvilNeiModel(BakedModel aFallbackModel, Block aBlock) {
		this(aFallbackModel, aBlock, defaultSpriteLookup(), GTViewerJump::preferredViewer);
	}

	/** The offline-test arm (the GT6KitchenNeiModel shape): inject the sprite lookup + viewer probe. */
	GT6AnvilNeiModel(BakedModel aFallbackModel, Block aBlock,
			Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup, Supplier<String> aViewerProbe) {
		super(aFallbackModel);
		mSpriteLookup = aSpriteLookup;
		mViewerProbe = aViewerProbe;
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(aSpriteId);
	}

	/** Unconditional (the kitchen shape): the glyph must render with or without a snapshot. */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return true;
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		List<BakedQuad> rQuads = getFallbackModel().getQuads(aState, aSide, aRand);
		if (aRenderType == RenderType.cutout() && aState != null && aState.hasProperty(GTAnvilBlock.FACING)) {
			List<BakedQuad> tGlyphs = glyphQuads(aState.getValue(GTAnvilBlock.FACING));
			if (!tGlyphs.isEmpty()) {
				// the fallback may have handed back its input list — copy before appending
				rQuads = new ArrayList<>(rQuads);
				rQuads.addAll(tGlyphs);
			}
		}
		return rQuads;
	}

	/** Cutout joins the seat exactly when the glyph exists (no viewer → the model is unchanged). */
	@Override
	public ChunkRenderTypeSet getRenderTypes(@Nullable BlockState aState, RandomSource aRand, ModelData aData) {
		ChunkRenderTypeSet tBase = getFallbackModel().getRenderTypes(aState, aRand, aData);
		if (mViewerProbe.get() == null) return tBase;
		return ChunkRenderTypeSet.union(tBase, ChunkRenderTypeSet.of(RenderType.cutout()));
	}

	/** The per-facing glyph pair, baked lazily once (the sprite is stable per load). */
	private List<BakedQuad> glyphQuads(Direction aFacing) {
		return mGlyphs.computeIfAbsent(aFacing, aFace -> bakeGlyphs(aFace, mViewerProbe.get()));
	}

	/**
	 * The two along-axis face glyphs (the :343-352 table): both faces carry the tile at the
	 * same cross-axis offset — the negative facing rides the min-edge arm, the positive the
	 * max-edge arm.
	 */
	private List<BakedQuad> bakeGlyphs(Direction aFacing, @Nullable String aViewer) {
		if (aViewer == null) return List.of();
		ResourceLocation tSpriteId = ResourceLocation.fromNamespaceAndPath(GTRenderModelListener.MOD_ID,
				"emi".equals(aViewer) ? GT6KitchenNeiModel.SPRITE_EMI : GT6KitchenNeiModel.SPRITE_JEI);
		TextureAtlasSprite tSprite = mSpriteLookup.apply(tSpriteId);
		if (tSprite == null) return List.of(); // atlas gap: skip the quads instead of rendering garbage
		NeiPlan tPlan = new NeiPlan(tSprite, tSpriteId);
		boolean tMinSide = aFacing == Direction.NORTH || aFacing == Direction.WEST;
		float tCross0 = tMinSide ? EDGE_INSET_PX : 16.0F - EDGE_INSET_PX - PATCH_PX;
		List<BakedQuad> rQuads = new ArrayList<>(2);
		for (Direction tFace : aFacing.getAxis() == Direction.Axis.Z
				? new Direction[] {Direction.NORTH, Direction.SOUTH}
				: new Direction[] {Direction.WEST, Direction.EAST}) {
			rQuads.add(bakeGlyphQuad(tPlan, tFace, tCross0));
		}
		return List.copyOf(rQuads);
	}

	/** One planned glyph quad: the resolved sprite + its atlas id (the kitchen NeiPlan shape). */
	private record NeiPlan(TextureAtlasSprite baked, ResourceLocation sprite) {}

	/**
	 * The 2x2px foot-band quad on ONE carrier face — the canonical full-face UV [0,0,16,16]
	 * (the #27 ruling), tintIndex -1 (the P22 decal contract), the cullface rides the
	 * BlockElementFace (the glyph hides against an opaque neighbour — vanilla semantics).
	 */
	private BakedQuad bakeGlyphQuad(NeiPlan aPlan, Direction aFace, float aCross0) {
		// FaceBakery works in model space (0..16) — the box's face-plane sits LIFT_PX
		// outside the carrier face (the min face for NORTH/WEST, the max for SOUTH/EAST)
		Vector3f tFrom = switch (aFace) {
			case NORTH -> new Vector3f(aCross0, 0, -LIFT_PX);
			case SOUTH -> new Vector3f(aCross0, 0, 16.0F - LIFT_PX);
			case WEST -> new Vector3f(-LIFT_PX, 0, aCross0);
			default -> new Vector3f(16.0F - LIFT_PX, 0, aCross0);
		};
		Vector3f tTo = switch (aFace) {
			case NORTH -> new Vector3f(aCross0 + PATCH_PX, BAND_TOP_PX, LIFT_PX);
			case SOUTH -> new Vector3f(aCross0 + PATCH_PX, BAND_TOP_PX, 16.0F + LIFT_PX);
			case WEST -> new Vector3f(LIFT_PX, BAND_TOP_PX, aCross0 + PATCH_PX);
			default -> new Vector3f(16.0F + LIFT_PX, BAND_TOP_PX, aCross0 + PATCH_PX);
		};
		return BAKERY.bakeQuad(tFrom, tTo,
				new BlockElementFace(aFace, -1, aPlan.sprite().toString(), new BlockFaceUV(new float[] {0, 0, 16, 16}, 0)),
				aPlan.baked(), aFace, BlockModelRotation.X0_Y0, null, true, aPlan.sprite());
	}

	// ---------------------------------------------------------------------------
	// the bake-time wrap (the GT6KitchenNeiModel.wrapStates shape, anvil-scoped)
	// ---------------------------------------------------------------------------

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		for (var tHandle : GT6Anvils.BLOCKS_BY_PATH.values()) wrapStates(tHandle.get(), aEvent);
	}

	private static void wrapStates(Block tBlock, ModelEvent.ModifyBakingResult aEvent) {
		for (BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
			var tKey = BlockModelShaper.stateToModelLocation(tState);
			BakedModel tBaked = aEvent.getModels().get(tKey);
			// the idempotence guard (hot reload re-entry); no tint interplay — the anvil
			// rows carry no tintindex (the class doc)
			if (tBaked != null && !(tBaked instanceof GT6AnvilNeiModel)) {
				aEvent.getModels().put(tKey, new GT6AnvilNeiModel(tBaked, tBlock));
			}
		}
	}
}
