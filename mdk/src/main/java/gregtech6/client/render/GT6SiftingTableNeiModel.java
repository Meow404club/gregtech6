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

import gregtech6.gui.GTViewerJump;
import gregtech6.registry.GT6SiftingTables;

/**
 * The sifting-table NEI corner glyph (task sifting-table-family) — the
 * {@link GT6AnvilNeiModel}/{@link GT6KitchenNeiModel} sibling: the upstream table drew
 * {@code BI.nei()} onto the SIDES_TOP faces of render pass 0 — the (0..2)x(0..2) leg-top
 * square at Y 13 (MultiTileEntitySiftingTable.java:420, the pass-0 box :393 top face),
 * while its client arm opened {@code RM.Sifting} from the 2px top-face corner (:292-295
 * {@code PX_P[2]}, the jump arm lives on {@code GT6SiftingTableBlockEntity.openNei}).
 * The patch is a BAKE-TIME append over the table's static model, gated by the ONE viewer
 * predicate the jump route uses — {@link GTViewerJump#preferredViewer()} — so the glyph
 * is viewer-dynamic: EMI present → the 「EMI」 tile, JEI only → 「JEI」, neither → NO
 * patch at all (what you see is what you jump to). The glyph tiles are the kitchen's own
 * viewer-dynamic pair ({@code kitchen_nei_jei/emi.png}) — zero new assets.
 *
 * <p>Layer seat: cutout joins the fallback exactly when the glyph exists (the transparent
 * glyph margins must discard, the r11-oven-solid-layer-fix lesson). Tint interplay is the
 * grindstone/kitchen order-safe pair (the body carries the datagen tintindex-0 seats —
 * the tint-wrap-inside / self-tint-outside arms): since task
 * tint-chain-hopper-grindstone-sifting the body resolves the ANY.Steel row colour through
 * {@link GTMachinePaintTint#tintMaterialOf} (the census L3 closure of the former
 * "render-pool defer"; the glyph quad itself stays untinted, the P22 decal contract).
 *
 * <p>CLIENT-only by registration ({@code Dist.CLIENT} listener — the dedicated server
 * never loads this class).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GT6SiftingTableNeiModel extends GTDynamicBakedModel {

	/** The derived glyph sprites (the README "Derived" ledger — the kitchen viewer-dynamic pair, reused). */
	public static final String SPRITE_JEI = GT6KitchenNeiModel.SPRITE_JEI;
	public static final String SPRITE_EMI = GT6KitchenNeiModel.SPRITE_EMI;

	/** The glyph tile footprint — the upstream 2x2px leg-top square (:393 the pass-0 box top). */
	public static final float PATCH_PX = 2.0F;
	/** The leg-top plane — upstream pass 0's Y top ({@code PX_P[13]}). */
	public static final float LEG_TOP_PX = 13.0F;
	/** The face-plane protrusion — the kitchen NEI_LIFT scale. */
	public static final float LIFT_PX = GT6KitchenNeiModel.NEI_LIFT;

	private static final FaceBakery BAKERY = new FaceBakery();

	private final Block mBlock;
	/** The per-wrapper retinted-copy table (the GTMachineTintModel cache form). */
	private final Map<Integer, Map<BakedQuad, BakedQuad>> mTintedQuads = new ConcurrentHashMap<>();
	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;
	/** The viewer probe — evaluated at bake time; the same predicate the jump route consumes. */
	private final Supplier<String> mViewerProbe;

	@Nullable
	private BakedQuad mGlyph;

	public GT6SiftingTableNeiModel(BakedModel aFallbackModel, Block aBlock) {
		this(aFallbackModel, aBlock, defaultSpriteLookup(), GTViewerJump::preferredViewer);
	}

	/** The offline-test arm (the anvil shape): inject the sprite lookup + viewer probe. */
	GT6SiftingTableNeiModel(BakedModel aFallbackModel, Block aBlock,
			Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup, Supplier<String> aViewerProbe) {
		super(aFallbackModel);
		mBlock = aBlock;
		mSpriteLookup = aSpriteLookup;
		mViewerProbe = aViewerProbe;
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(aSpriteId);
	}

	/** Unconditional (the anvil shape): the glyph must render with or without a snapshot. */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return true;
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		List<BakedQuad> rQuads = bodyQuads(aState, aSide, aRand, aModelData);
		if (aRenderType == RenderType.cutout()) { // the kitchen form: the glyph rides every cutout request
			BakedQuad tGlyph = glyphQuad();
			if (tGlyph != null) {
				// the tint arms may have handed back their input list — copy before appending
				rQuads = new ArrayList<>(rQuads);
				rQuads.add(tGlyph);
			}
		}
		return rQuads;
	}

	/**
	 * The body pass (the {@link GT6GrindstoneNeiModel#bodyQuads} order-safe pair verbatim):
	 * already tinted when the inner wrapper is the tint wrap, self-tinted off a raw
	 * fallback (the ANY.Steel row through the combined dispatch).
	 */
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

	/** The leg-top glyph quad, baked lazily once (the sprite is stable per load). */
	@Nullable
	private BakedQuad glyphQuad() {
		if (mGlyph == null) {
			String tViewer = mViewerProbe.get();
			if (tViewer == null) return null;
			ResourceLocation tSpriteId = ResourceLocation.fromNamespaceAndPath(GTRenderModelListener.MOD_ID,
					"emi".equals(tViewer) ? SPRITE_EMI : SPRITE_JEI);
			TextureAtlasSprite tSprite = mSpriteLookup.apply(tSpriteId);
			if (tSprite == null) return null; // atlas gap: skip the quad instead of rendering garbage
			GlyphPlan tPlan = new GlyphPlan(tSprite, tSpriteId);
			// the 2x2px UP quad at the leg top (0..2)x(0..2), lifted off the plane — the
			// canonical full-face UV [0,0,16,16] (the #27 ruling), tintIndex -1 (the P22
			// decal contract), cullface up (the glyph hides under a block placed on the
			// table — vanilla semantics); the tail arg rides the tPlan.sprite() census form
			// (the stonecutter bakeQuad eight-arg rewrite anchors it, the anvil shape)
			mGlyph = BAKERY.bakeQuad(
					new Vector3f(0.0F, LEG_TOP_PX + LIFT_PX, 0.0F),
					new Vector3f(PATCH_PX, LEG_TOP_PX + LIFT_PX, PATCH_PX),
					new BlockElementFace(Direction.UP, -1, tPlan.sprite().toString(), new BlockFaceUV(new float[] {0, 0, 16, 16}, 0)),
					tPlan.baked(), Direction.UP, BlockModelRotation.X0_Y0, null, true, tPlan.sprite());
		}
		return mGlyph;
	}

	/** One planned glyph quad: the resolved sprite + its atlas id (the anvil NeiPlan shape). */
	private record GlyphPlan(TextureAtlasSprite baked, ResourceLocation sprite) {}

	// ---------------------------------------------------------------------------
	// the bake-time wrap (the GT6AnvilNeiModel.wrapStates shape, sifting-scoped)
	// ---------------------------------------------------------------------------

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		wrapStates(GT6SiftingTables.SIFTING_TABLE.get(), aEvent);
	}

	private static void wrapStates(Block tBlock, ModelEvent.ModifyBakingResult aEvent) {
		for (BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
			var tKey = BlockModelShaper.stateToModelLocation(tState);
			BakedModel tBaked = aEvent.getModels().get(tKey);
			// the idempotence guard (hot reload re-entry); the tint interplay is the
			// order-safe pair documented on the class (the grindstone shape)
			if (tBaked != null && !(tBaked instanceof GT6SiftingTableNeiModel)) {
				aEvent.getModels().put(tKey, new GT6SiftingTableNeiModel(tBaked, tBlock));
			}
		}
	}
}
