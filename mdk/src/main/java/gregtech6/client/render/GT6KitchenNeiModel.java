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
import gregtech6.registry.GT6Kitchen;

/**
 * The kitchen NEI corner glyph (task kitchen-nei-corner-jump): the rim-corner quad the
 * upstream pot/bowl/juicer drew as render pass 6/7 ({@code BI.nei()} on the walls' corner
 * column top, MixingBowl.java:375 case 6 / Juicer :239 case 7). The patch is a BAKE-TIME
 * append over the family's static models, gated by the ONE viewer predicate the jump
 * route uses — {@link GTViewerJump#preferredViewer()} — so the glyph is viewer-dynamic:
 * EMI present → the 「EMI」 tile, JEI only → 「JEI」, neither → NO patch at all (the
 * known_bugs.r11-nei ruling: two missing viewers must not show a dead button; the
 * viewer_priority_emi ruling: a dual install renders and jumps to EMI — what you see is
 * what you jump to).
 *
 * <p>WHY an appended quad and not a datagen element: the visibility gate is per-user
 * (the client's ModList at resource load), and the shipped blockstate JSON is fixed — a
 * static element cannot un-render for the viewer-less install. The glyph textures are
 * stitched by vanilla's directory-source block atlas (any {@code textures/block/**} PNG),
 * so the append needs zero JSON on either side.
 *
 * <p>Layer seat: the glyph PNG carries transparent margins, so the quad rides
 * {@code cutout} (the r11-oven-solid-layer-fix lesson — transparent texels on the solid
 * layer paint their RGB matte opaque) while the body keeps the fallback's own layers;
 * {@link #getRenderTypes} adds cutout exactly when the glyph exists.
 *
 * <p>WRAP ORDER (order-safe against the {@link GTMachineTintModel} listener, same event):
 * whichever listener runs first, the family ends up tinted exactly once and carrying the
 * glyph — if the tint wrapper is INSIDE (tint listener first), its quads are already
 * tinted and this model only appends; if this model lands first, the tint wrap skips the
 * family ({@code instanceof GTDynamicBakedModel} guard) and the tint rides HERE (the
 * {@link GTOvenOverlayModel} shape — the oven ladder self-tints for the same reason).
 *
 * <p>Upstream geometry verbatim: a 2x2px UP quad on the walls' corner column top, the
 * whole 128x128 sheet squeezed onto it (the 1.7.10 IIcon full-texture mapping), lifted
 * +0.001 off the wall rim — {@code PX_N[8]+0.001F} on the tub pair (walls y 2..8, corner
 * (0..2)x(0..2)), {@code PX_N[12]+0.001F} on the Juicer (walls y 0..4, corner (2..4)x(2..4)
 * — the 4px family variant's rim). TintIndex -1: the yellow is baked into the PNG, never
 * retinted (the P22 uncoloured-decal contract).
 *
 * <p>CLIENT-only by registration ({@code Dist.CLIENT} listener — the dedicated server
 * never loads this class; the offline pins drive the test ctor directly, the
 * GTOvenOverlayModelTest shape).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GT6KitchenNeiModel extends GTDynamicBakedModel {

	/** The derived glyph sprites (the README "Derived" ledger — the viewer-dynamic corner tiles). */
	public static final String SPRITE_JEI = "block/tools/kitchen_nei_jei";
	public static final String SPRITE_EMI = "block/tools/kitchen_nei_emi";

	/** The rim lift — upstream {@code PX_N[8]+0.001F} / {@code PX_N[12]+0.001F}. */
	public static final float NEI_LIFT = 0.001F;

	/** The corner patch footprint in px — the upstream 2x2px glyph quad (MixingBowl.java:375 case 6 box width). */
	public static final float PATCH_PX = 2.0F;

	private static final FaceBakery BAKERY = new FaceBakery();

	private final Block mBlock;
	private final net.minecraft.world.phys.shapes.VoxelShape mShape;
	private final Map<Integer, Map<BakedQuad, BakedQuad>> mTintedQuads = new ConcurrentHashMap<>();
	/** Sprite resolver — runtime: the block atlas (Minecraft.java:2386); tests: a stub. */
	private final Function<ResourceLocation, TextureAtlasSprite> mSpriteLookup;
	/** The viewer probe — evaluated at BAKE time; the same predicate the jump route consumes. */
	private final Supplier<String> mViewerProbe;

	@Nullable
	private BakedQuad mNeiQuad;

	public GT6KitchenNeiModel(BakedModel aFallbackModel, Block aBlock) {
		this(aFallbackModel, aBlock, shapeOf(aBlock), defaultSpriteLookup(), GTViewerJump::preferredViewer);
	}

	/** The offline-test arm (the GTOvenOverlayModel shape): inject the shape, sprite lookup + viewer probe. */
	GT6KitchenNeiModel(BakedModel aFallbackModel, Block aBlock, net.minecraft.world.phys.shapes.VoxelShape aShape,
			Function<ResourceLocation, TextureAtlasSprite> aSpriteLookup, Supplier<String> aViewerProbe) {
		super(aFallbackModel);
		mBlock = aBlock;
		mShape = aShape;
		mSpriteLookup = aSpriteLookup;
		mViewerProbe = aViewerProbe;
	}

	/**
	 * The family collision-pool shape — the port's single source of the rim geometry (the
	 * GTKitchenBlock carrier answers the query state-free, its {@code getShape} ignores
	 * the arguments).
	 */
	private static net.minecraft.world.phys.shapes.VoxelShape shapeOf(Block aBlock) {
		return aBlock instanceof gregtech6.block.tools.GTKitchenBlock tKitchen
				? tKitchen.getShape(null, null, null, null)
				: net.minecraft.world.phys.shapes.Shapes.block();
	}

	private static Function<ResourceLocation, TextureAtlasSprite> defaultSpriteLookup() {
		return aSpriteId -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(aSpriteId);
	}

	/** Unconditional (the tint-wrapper shape): the glyph must render with or without a snapshot. */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return true;
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		List<BakedQuad> rQuads = bodyQuads(aState, aSide, aRand, aModelData);
		if (aRenderType == RenderType.cutout()) {
			BakedQuad tNei = neiQuad();
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

	/** The corner quad for THIS block's family variant, baked lazily once (the sprite is stable per load). */
	@Nullable
	private BakedQuad neiQuad() {
		if (mNeiQuad == null) {
			String tViewer = mViewerProbe.get();
			if (tViewer == null) return null;
			ResourceLocation tSpriteId = ResourceLocation.fromNamespaceAndPath(
					GTRenderModelListener.MOD_ID, "emi".equals(tViewer) ? SPRITE_EMI : SPRITE_JEI);
			TextureAtlasSprite tSprite = mSpriteLookup.apply(tSpriteId);
			if (tSprite == null) return null; // atlas gap: skip the quad instead of rendering garbage
			mNeiQuad = bakeNeiQuad(new NeiPlan(tSprite, tSpriteId));
		}
		return mNeiQuad;
	}

	/** One planned glyph quad: the resolved sprite + its atlas id (the GTOvenOverlayModel.OverlayPlan shape — the `.sprite()` accessor the stonecutter 21.1 eight-arg rewrite anchors on). */
	private record NeiPlan(TextureAtlasSprite baked, ResourceLocation sprite) {}

	/**
	 * The 2x2px rim-corner UP quad — the upstream pass-6/7 box, derived from the family
	 * collision-pool shape: the corner = the shape's min footprint corner, {@value #PATCH_PX}
	 * px wide, the rim = the shape height (8px tub pair / 4px Juicer — exactly the upstream
	 * {@code PX_N[8]+0.001F} / {@code PX_N[12]+0.001F} boxes; the Juicer's 2px inset min
	 * reproduces its (2..4)x(2..4) corner).
	 */
	private BakedQuad bakeNeiQuad(NeiPlan aPlan) {
		// toAabbs() is the 0..1 normalized space (the r3-stick-shape lesson)
		net.minecraft.world.phys.AABB tBox = mShape.toAabbs().get(0);
		double tX0 = tBox.minX * 16, tZ0 = tBox.minZ * 16;
		double tRim = tBox.maxY * 16;
		// the bakeQuad call is the GTOvenOverlayModel.bakeOverlayQuad form: canonical
		// full-face UV [0,0,16,16] (the #27 ruling), tintIndex -1 (the P22 decal contract),
		// cullface up (the glyph hides under a block placed on the rim — vanilla semantics)
		return BAKERY.bakeQuad(new Vector3f((float) tX0, (float) tRim, (float) tZ0),
				new Vector3f((float) (tX0 + PATCH_PX), (float) (tRim + NEI_LIFT), (float) (tZ0 + PATCH_PX)),
				new BlockElementFace(Direction.UP, -1, aPlan.sprite().toString(), new BlockFaceUV(new float[] {0, 0, 16, 16}, 0)),
				aPlan.baked(), Direction.UP, BlockModelRotation.X0_Y0, null, true, aPlan.sprite());
	}

	// ---------------------------------------------------------------------------
	// the bake-time wrap (the GTMachineTintModel.wrapStates shape, kitchen-scoped)
	// ---------------------------------------------------------------------------

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		for (Block tBlock : GT6Kitchen.paintableBlockArray()) wrapStates(tBlock, aEvent);
	}

	private static void wrapStates(Block tBlock, ModelEvent.ModifyBakingResult aEvent) {
		for (BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
			var tKey = BlockModelShaper.stateToModelLocation(tState);
			BakedModel tBaked = aEvent.getModels().get(tKey);
			// the idempotence guard (hot reload re-entry); the tint interplay is the
			// order-safe pair documented on the class
			if (tBaked != null && !(tBaked instanceof GT6KitchenNeiModel)) {
				aEvent.getModels().put(tKey, new GT6KitchenNeiModel(tBaked, tBlock));
			}
		}
	}
}
