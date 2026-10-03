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

import gregtech6.block.tools.GT6MortarBlock;
import gregtech6.gui.GTViewerJump;
import gregtech6.registry.GT6Mortars;

/**
 * The mortar NEI rim-corner glyph (task mortar-family) — the {@link GT6KitchenNeiModel}
 * sibling via the GT6AnvilNeiModel bake-time form: the upstream mortar drew
 * {@code BI.nei()} on render pass 6, the 12x12px rim-top plate at
 * {@code PX_N[10]+0.001} (MultiTileEntityMortar.java:122 the pass-6 box, :150 case 6 the
 * top-face-only {@code BI.nei()}), while its client arm opened {@code RM.Mortar} from the
 * top-face 4px corner (:97-103 — the clickable envelope footprint 2..14px makes the
 * corner region the (2..4)x(2..4)px patch). The patch is a BAKE-TIME append over the five
 * rows' static models, gated by the ONE viewer predicate the jump route uses —
 * {@link GTViewerJump#preferredViewer()} — so the glyph is viewer-dynamic: EMI present →
 * the 「EMI」 tile, JEI only → 「JEI」, neither → NO patch at all (what you see is what
 * you jump to). The glyph tiles are the family viewer-dynamic pair
 * ({@code kitchen_nei_jei/emi.png}) — zero new assets. Like the kitchen/anvil siblings,
 * the patch is a bake artifact and NOT a datagen product: the visibility gate is
 * per-user and the shipped blockstate JSON is fixed.
 *
 * <p>NO tint interplay (the anvil form, not the kitchen order-safe form): the mortar rows
 * are NOT machine-domain paintables — the tint rides the runtime {@code GT6MortarTint}
 * BlockColor over the fallback quads' tintindex 0/1 seats, so the wrap passes the
 * fallback quads through verbatim and only the glyph (tintIndex -1, the P22 decal
 * contract) joins on cutout — the layer the model JSON already declares (the overlay
 * shells), so {@link #getRenderTypes} degrades to the fallback seat unchanged.
 *
 * <p>Geometry (the kitchen shape-derivation form): the collision-pool envelope's min
 * footprint corner (2, 2px) at the rim height (6px) + {@link #NEI_LIFT} — exactly the
 * (2..4)x(2..4)px patch the corner click region (hitX/hitZ &le; {@code PX_P[4]},
 * MultiTileEntityMortar.java:81/:99) names, lifted off the {@code PX_N[10]} rim plane the
 * way the upstream pass-6 plate lifted off the walls.
 *
 * <p>CLIENT-only by registration ({@code Dist.CLIENT} listener — the dedicated server
 * never loads this class; the offline pins drive the test ctor directly, the
 * GT6AnvilNeiModel shape).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GT6MortarNeiModel extends GTDynamicBakedModel {

	/** The glyph tile footprint — the upstream corner region's 2x2px clickable remainder (PX_P[4] minus the 2px envelope inset). */
	public static final float PATCH_PX = 2.0F;
	/** The rim lift — the upstream pass-6 {@code PX_N[10]+0.001F} plate (MultiTileEntityMortar.java:122). */
	public static final float NEI_LIFT = 0.001F;

	private static final FaceBakery BAKERY = new FaceBakery();

	/** The per-block glyph quad, baked lazily once (the sprite is stable per load). */
	private final Map<Block, BakedQuad> mGlyphs = new ConcurrentHashMap<>();
	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;
	/** The viewer probe — evaluated at bake time; the same predicate the jump route consumes. */
	private final Supplier<String> mViewerProbe;

	public GT6MortarNeiModel(BakedModel aFallbackModel) {
		this(aFallbackModel, defaultSpriteLookup(), GTViewerJump::preferredViewer);
	}

	/** The offline-test arm (the GT6AnvilNeiModel shape): inject the sprite lookup + viewer probe. */
	GT6MortarNeiModel(BakedModel aFallbackModel,
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
		if (aRenderType == RenderType.cutout() && aState != null && aState.getBlock() instanceof GT6MortarBlock) {
			BakedQuad tGlyph = glyphQuad(aState.getBlock());
			if (tGlyph != null) {
				// the fallback may have handed back its input list — copy before appending
				rQuads = new ArrayList<>(rQuads);
				rQuads.add(tGlyph);
			}
		}
		return rQuads;
	}

	/** Cutout joins the seat exactly when the glyph exists (the model JSON already declares cutout — the union is inert). */
	@Override
	public ChunkRenderTypeSet getRenderTypes(@Nullable BlockState aState, RandomSource aRand, ModelData aData) {
		ChunkRenderTypeSet tBase = getFallbackModel().getRenderTypes(aState, aRand, aData);
		if (mViewerProbe.get() == null || tBase.contains(RenderType.cutout())) return tBase;
		return ChunkRenderTypeSet.union(tBase, ChunkRenderTypeSet.of(RenderType.cutout()));
	}

	/** The rim-corner glyph for THIS row, baked lazily once (the sprite is stable per load). */
	@Nullable
	private BakedQuad glyphQuad(Block aBlock) {
		return mGlyphs.computeIfAbsent(aBlock, aKey -> bakeGlyph(mViewerProbe.get()));
	}

	private record NeiPlan(TextureAtlasSprite baked, ResourceLocation sprite) {}

	/**
	 * The 2x2px rim-corner UP quad — the canonical full-face UV [0,0,16,16] (the #27
	 * ruling), tintIndex -1 (the P22 decal contract), cullface up (the glyph hides under a
	 * block placed on the rim — vanilla semantics).
	 */
	@Nullable
	private BakedQuad bakeGlyph(@Nullable String aViewer) {
		if (aViewer == null) return null;
		ResourceLocation tSpriteId = ResourceLocation.fromNamespaceAndPath(GTRenderModelListener.MOD_ID,
				"emi".equals(aViewer) ? GT6KitchenNeiModel.SPRITE_EMI : GT6KitchenNeiModel.SPRITE_JEI);
		TextureAtlasSprite tSprite = mSpriteLookup.apply(tSpriteId);
		if (tSprite == null) return null; // atlas gap: skip the quad instead of rendering garbage
		NeiPlan tPlan = new NeiPlan(tSprite, tSpriteId);
		// the envelope's min footprint corner + rim height, in px (GT6MortarBlock.SHAPE —
		// the 2..14px footprint x 6px rim; the corner click region names the 2px remainder
		// up to PX_P[4])
		float tX0 = 2.0F, tZ0 = 2.0F, tRim = 6.0F;
		return BAKERY.bakeQuad(new Vector3f(tX0, tRim, tZ0),
				new Vector3f(tX0 + PATCH_PX, tRim + NEI_LIFT, tZ0 + PATCH_PX),
				new BlockElementFace(Direction.UP, -1, tPlan.sprite().toString(), new BlockFaceUV(new float[] {0, 0, 16, 16}, 0)),
				tPlan.baked(), Direction.UP, BlockModelRotation.X0_Y0, null, true, tPlan.sprite());
	}

	// ---------------------------------------------------------------------------
	// the bake-time wrap (the GT6AnvilNeiModel.wrapStates shape, mortar-scoped)
	// ---------------------------------------------------------------------------

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		for (var tHandle : GT6Mortars.BLOCKS_BY_PATH.values()) wrapStates(tHandle.get(), aEvent);
	}

	private static void wrapStates(Block tBlock, ModelEvent.ModifyBakingResult aEvent) {
		for (BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
			var tKey = BlockModelShaper.stateToModelLocation(tState);
			BakedModel tBaked = aEvent.getModels().get(tKey);
			// the idempotence guard (hot reload re-entry); no baked-tint interplay — the
			// mortar tint rides the runtime BlockColor (the class doc)
			if (tBaked != null && !(tBaked instanceof GT6MortarNeiModel)) {
				aEvent.getModels().put(tKey, new GT6MortarNeiModel(tBaked));
			}
		}
	}
}
