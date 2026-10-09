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

import gregtech6.block.tools.GT6GrindstoneBlock;
import gregtech6.gui.GTViewerJump;
import gregtech6.registry.GT6Grindstones;

/**
 * The grindstone NEI corner glyph (task grindstone-family) — the
 * {@link GT6KitchenNeiModel} sibling: the upstream stone drew {@code BI.nei()} on the
 * pass-0 corner post's TOP face when mStone != 0 (MultiTileEntityGrindStone.java:244, the
 * 2x2px post at (6,3,2)-(8,15,4) Z-form / (2,3,6)-(4,15,8) X-form — :211/:216 the pass
 * boxes), while its client arm opened {@code RM.Sharpening} from the top-face corner
 * region (:168-173, the [hitX, hitZ] thresholds 8x4 / 4x8 px by facing axis) — the jump
 * arm lives on {@code GT6GrindstoneBlockEntity.openNei}. The patch is a BAKE-TIME append
 * over the grindstone's static models, GATED on the STONE blockstate (the empty stone
 * shows no glyph — the upstream {@code mStone!=0 &&} conjunction verbatim) and on the ONE
 * viewer predicate the jump route uses — {@link GTViewerJump#preferredViewer()} — so the
 * glyph is viewer-dynamic: EMI present → the 「EMI」 tile, JEI only → 「JEI」, neither →
 * NO patch at all. LIKE the kitchen sibling, the patch is a bake artifact and NOT a datagen
 * product: the visibility gate is per-user (the client's ModList at resource load) and the
 * shipped blockstate JSON is fixed; the glyph tiles are the kitchen's own viewer-dynamic
 * pair ({@code kitchen_nei_jei/emi.png}) — zero new assets.
 *
 * <p>Layer seat: cutout joins the fallback exactly when the glyph exists (the transparent
 * glyph margins must discard, the r11-oven-solid-layer-fix lesson). Tint interplay is the
 * kitchen order-safe pair documented there (the body carries tintindex-0 faces — the
 * tint-wrap-inside / self-tint-outside arms); since task
 * tint-chain-hopper-grindstone-sifting the dispatch resolves the ANY.Steel row colour
 * through {@link GTMachinePaintTint#tintMaterialOf} (the census L2 closure — the former
 * "no dispatch row, white no-op" README deviation retires), the glyph quad itself stays
 * untinted (the P22 decal contract).
 *
 * <p>Geometry per FACING axis (the post-top face verbatim): the Z-axis facings ride the
 * (6..8, 2..4) corner, the X-axis facings the (2..4, 6..8) corner — both lifted
 * {@link #NEI_LIFT} off the 15px plate top (the upstream {@code PX_N[1]+0.0001} post
 * shoulder). No cullface: the post top sits INSIDE the block space (the vanilla
 * per-block-face cull would hide it under any block above — the kitchen rim case does not
 * apply here).
 *
 * <p>CLIENT-only by registration ({@code Dist.CLIENT} listener — the dedicated server
 * never loads this class; the offline pins drive the test ctor directly, the
 * GT6AnvilNeiModelTest shape).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GT6GrindstoneNeiModel extends GTDynamicBakedModel {

	/** The derived glyph sprites (the kitchen pair — the README "Derived" ledger). */
	public static final String SPRITE_JEI = GT6KitchenNeiModel.SPRITE_JEI;
	public static final String SPRITE_EMI = GT6KitchenNeiModel.SPRITE_EMI;

	/** The plate-top lift — the upstream {@code PX_N[1]+0.0001} shoulder, the kitchen NEI_LIFT scale. */
	public static final float NEI_LIFT = 0.001F;

	/** The post-top footprint — the upstream 2x2px BI.nei() face (:211 the pass-0 box tail). */
	public static final float PATCH_PX = 2.0F;

	/** The plate top (the pass-0/pass-5 boxes' common shoulder, {@code PX_N[1]}). */
	public static final float PLATE_TOP_PX = 15.0F;

	private static final FaceBakery BAKERY = new FaceBakery();

	private final Block mBlock;
	private final Map<Integer, Map<BakedQuad, BakedQuad>> mTintedQuads = new ConcurrentHashMap<>();
	/** The per-axis baked glyph (0 = Z-axis facings, 1 = X-axis facings); lazily baked once. */
	private final List<BakedQuad> mNeiQuads = new ArrayList<>(java.util.Collections.nCopies(2, null));
	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;
	/** The viewer probe — evaluated at BAKE time; the same predicate the jump route consumes. */
	private final Supplier<String> mViewerProbe;

	public GT6GrindstoneNeiModel(BakedModel aFallbackModel, Block aBlock) {
		this(aFallbackModel, aBlock, defaultSpriteLookup(), GTViewerJump::preferredViewer);
	}

	/** The offline-test arm (the GT6KitchenNeiModel shape): inject the sprite lookup + viewer probe. */
	GT6GrindstoneNeiModel(BakedModel aFallbackModel, Block aBlock,
			Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup, Supplier<String> aViewerProbe) {
		super(aFallbackModel);
		mBlock = aBlock;
		mSpriteLookup = aSpriteLookup;
		mViewerProbe = aViewerProbe;
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(aSpriteId);
	}

	/** Unconditional (the kitchen shape): the glyph must render with or without a snapshot. */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return true;
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		List<BakedQuad> rQuads = bodyQuads(aState, aSide, aRand, aModelData);
		if (aRenderType == RenderType.cutout() && aState != null
				&& aState.hasProperty(GT6GrindstoneBlock.STONE) && aState.getValue(GT6GrindstoneBlock.STONE) > 0) {
			BakedQuad tNei = neiQuad(aState.getValue(GT6GrindstoneBlock.FACING).getAxis()
					== Direction.Axis.Z ? 0 : 1);
			if (tNei != null) {
				// the tint arms may have handed back their input list — copy before appending
				rQuads = new ArrayList<>(rQuads);
				rQuads.add(tNei);
			}
		}
		return rQuads;
	}

	/** The body pass: already tinted when the inner wrapper is the tint wrap, self-tinted off a raw fallback. */
	private List<BakedQuad> bodyQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData) {
		if (getFallbackModel() instanceof GTMachineTintModel) {
			// the tint listener wrapped this block first — its quads ARE the tinted body
			return getFallbackModel().getQuads(aState, aSide, aRand);
		}
		return GTMachineTintModel.tintQuads(getFallbackModel().getQuads(aState, aSide, aRand),
				GTMachinePaintTint.tintARGB(aModelData, GTMachinePaintTint.tintMaterialOf(mBlock), 0), mTintedQuads);
	}

	/** Cutout joins the seat exactly when the glyph exists (no viewer → the model is unchanged). */
	@Override
	public ChunkRenderTypeSet getRenderTypes(@Nullable BlockState aState, RandomSource aRand, ModelData aData) {
		ChunkRenderTypeSet tBase = getFallbackModel().getRenderTypes(aState, aRand, aData);
		if (mViewerProbe.get() == null) return tBase;
		return ChunkRenderTypeSet.union(tBase, ChunkRenderTypeSet.of(RenderType.cutout()));
	}

	/** The axis glyph, baked lazily once (the sprite is stable per load). */
	@Nullable
	private BakedQuad neiQuad(int aAxis) {
		BakedQuad rQuad = mNeiQuads.get(aAxis);
		if (rQuad == null) {
			String tViewer = mViewerProbe.get();
			if (tViewer == null) return null;
			ResourceLocation tSpriteId = ResourceLocation.fromNamespaceAndPath(
					GTRenderModelListener.MOD_ID, "emi".equals(tViewer) ? SPRITE_EMI : SPRITE_JEI);
			TextureAtlasSprite tSprite = mSpriteLookup.apply(tSpriteId);
			if (tSprite == null) return null; // atlas gap: skip the quad instead of rendering garbage
			rQuad = bakeNeiQuad(new NeiPlan(tSprite, tSpriteId), aAxis == 0);
			mNeiQuads.set(aAxis, rQuad);
		}
		return rQuad;
	}

	/** One planned glyph quad: the resolved sprite + its atlas id (the kitchen NeiPlan shape). */
	private record NeiPlan(TextureAtlasSprite baked, ResourceLocation sprite) {}

	/**
	 * The 2x2px post-top UP quad — the canonical full-face UV [0,0,16,16] (the #27 ruling),
	 * tintIndex -1 (the P22 decal contract), NO cullface (the post top is INSIDE the block
	 * space — the class doc), the Z-form at (6..8, 2..4), the X-form at (2..4, 6..8).
	 */
	private BakedQuad bakeNeiQuad(NeiPlan aPlan, boolean aAxisZ) {
		float tX0 = aAxisZ ? 6.0F : 2.0F, tZ0 = aAxisZ ? 2.0F : 6.0F;
		return BAKERY.bakeQuad(new Vector3f(tX0, PLATE_TOP_PX, tZ0),
				new Vector3f(tX0 + PATCH_PX, PLATE_TOP_PX + NEI_LIFT, tZ0 + PATCH_PX),
				new BlockElementFace(Direction.UP, -1, aPlan.sprite().toString(), new BlockFaceUV(new float[] {0, 0, 16, 16}, 0)),
				aPlan.baked(), Direction.UP, BlockModelRotation.X0_Y0, null, true, aPlan.sprite());
	}

	// ---------------------------------------------------------------------------
	// the bake-time wrap (the GT6KitchenNeiModel.wrapStates shape, grindstone-scoped)
	// ---------------------------------------------------------------------------

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		for (Block tBlock : GT6Grindstones.blockArray()) wrapStates(tBlock, aEvent);
	}

	private static void wrapStates(Block tBlock, ModelEvent.ModifyBakingResult aEvent) {
		for (BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
			var tKey = BlockModelShaper.stateToModelLocation(tState);
			BakedModel tBaked = aEvent.getModels().get(tKey);
			// the idempotence guard (hot reload re-entry); the tint interplay is the
			// order-safe pair documented on the class
			if (tBaked != null && !(tBaked instanceof GT6GrindstoneNeiModel)) {
				aEvent.getModels().put(tKey, new GT6GrindstoneNeiModel(tBaked, tBlock));
			}
		}
	}
}
