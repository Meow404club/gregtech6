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
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTBlockEntities;

/**
 * The fluid-spring's per-fluid skin baked INTO the quads (task p38-spring-texture-tint) —
 * the direct translation of the upstream spring render stack, MultiTileEntityFluidSpring
 * .java:150 {@code getTexture = BlockTextureMulti(BlockTextureFluid.get(mFluid),
 * BlockTextureDefault.get(Textures.BlockIcons.FLUID_SPRING))}: every visible face renders
 * as TWO stacked quads —
 * <ul>
 * <li>the <b>fluid base</b>: the spring fluid's still texture multiplied by its tint
 *     (upstream BlockTextureFluid.java:52-53 — the fluid icon x the render colour), the
 *     colour BAKED into the vertex data ({@link GTMachineTintModel#retintVertices}, the
 *     p32 route — the runtime BlockColor route rendered achromatic live, so the colour
 *     rides the model);</li>
 * <li>the <b>FLUID_SPRING dither overlay</b> (the borrowed
 *     {@code gt6:block/fluid_spring}, the assets/README.md ledger row): un-tinted in both
 *     editions (the BlockTextureDefault half is white), FULLY COPLANAR with the fluid
 *     base ({@code ε=0}, cullface-synced — the #16 seam fix, the GTOreBakedModel base+
 *     overlay composition) — the PNG's transparent holes are what the tinted fluid body
 *     shows through.</li>
 * </ul>
 *
 * <p>WHY per-{@link ModelData}: all sixteen spring rows share the ONE blockstate
 * ({@code GTFluidSpringBlock}, property-free) — the fluid identity rides the BE
 * ({@code springFluid} column), so the skin key flows in through
 * {@link GTModelProperties#SPRING_FLUID} (the paint arm's snapshot shape; the red line —
 * this method reads ONLY the immutable ModelData, never a live BlockEntity). The skin
 * resolution is the offline-readable seam {@link #skinOf}: the gt6 fluids' tints are the
 * SAME spec-table rows the registrations read ({@code GTFluids.CHEMICAL_SPECS}/
 * {@code AQUA_SPECS} — "the offline-readable half; the live FluidType carries the same
 * numbers at registration time", the GTFluids declaration pattern), the vanilla lava row
 * is the white {@code -1} identity over its own already-coloured still, and the still
 * sprites are the registrations' shared vanilla carriers ({@code water_still}/
 * {@code lava_still} — every GTFluids {@code initializeClient} points STILL there).
 * {@code ponytail:} the natural_gas tint is a transcribed literal (GTFluids
 * .NATURAL_GAS_TYPE initializeClient {@code 0x66FFF2B0}) because that registration is
 * inline, not table-driven — if a fluid's tint or still carrier ever changes, THIS
 * resolver is the one-file follow-up.
 *
 * <p>Quad emission + cache shape: the {@link GTOreBakedModel} lazy-bake form, keyed by the
 * immutable skin record instead of the static per-block params (one model instance serves
 * every spring position; the chunk build may hit it from any worker thread — concurrent
 * map, immutable values). The chunk passes are PARTITIONED like the ore model (issue #2
 * same-type fix): solid receives the tinted fluid body, cutout the dither shell (its
 * 116 transparent holes are alpha-discarded on cutout but would paint an opaque gray
 * plate over the fluid on the alpha-less solid shader), the null pass (item render,
 * breaking overlays — IForgeBakedModel: return all their quads) both; the no-skin miss
 * falls back to the blockstate JSON (the water-cube base carrier).
 *
 * <p>CLIENT-ONLY ({@code @OnlyIn(Dist.CLIENT)} — the {@link GTMachineTintModel}
 * annotation-registration shape; the skin seam is driven offline by the tests).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTFluidSpringBakedModel extends GTDynamicBakedModel {

	/** The borrowed FLUID_SPRING dither (the overlay layer, stitched via the GT6Atlases source — referenced by NO model JSON). */
	public static final ResourceLocation OVERLAY_SPRITE = ResourceLocation.fromNamespaceAndPath("gt6", "block/fluid_spring");

	/** The still carrier of every gt6 spring fluid (the registrations' shared initializeClient STILL). */
	private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");

	/** The vanilla lava row's own still (already coloured — the white tint identity renders it as-is). */
	private static final ResourceLocation LAVA_STILL = ResourceLocation.withDefaultNamespace("block/lava_still");

	/**
	 * One spring position's render identity: the base still sprite + the opaque-basis tint
	 * ({@code -1} = the no-tint identity, the lava row). Immutable — the cache key AND the
	 * ModelData-adjacent payload contract.
	 *
	 * @param stillSprite the fluid base texture (null = the unresolvable-id face: overlay
	 *                    only, the dither IS the block identity)
	 * @param tintARGB    the fluid's tint as declared (the spec rows keep their own alpha —
	 *                    the gas {@code 0x66…} rides along; the solid pass does not blend,
	 *                    which is exactly how the fluid lakes themselves render opaque)
	 */
	public record SpringSkin(@Nullable ResourceLocation stillSprite, int tintARGB) {}

	/** The unresolvable-id sentinel: no base layer, the overlay shell only. */
	private static final SpringSkin OVERLAY_ONLY = new SpringSkin(null, -1);

	private static final FaceBakery BAKERY = new FaceBakery();

	/** The sprite resolver — the GTOreBakedModel seam shape (tests: the unit-sprite stub). */
	private final Function<Material, TextureAtlasSprite> mSpriteLookup;

	/** The per-skin baked quad lists split by chunk layer (the {@link gregtech6.client.ore.GTOreBakedModel} Layers form: fluid body / dither shell / the combined null-pass view). */
	private record Layers(List<BakedQuad> base, List<BakedQuad> overlay, List<BakedQuad> all) {}

	/** The per-skin baked layers (6 base + 6 overlay), baked lazily at first getQuads. */
	private final Map<SpringSkin, Layers> mQuadsBySkin = new ConcurrentHashMap<>();

	public GTFluidSpringBakedModel(BakedModel aFallbackModel, Function<Material, TextureAtlasSprite> aSpriteLookup) {
		super(aFallbackModel);
		mSpriteLookup = aSpriteLookup;
	}

	// ---------------------------------------------------------------------------
	// the offline-readable skin seam (the colour source, spec tables verbatim)
	// ---------------------------------------------------------------------------

	/**
	 * The skin of one spring block id, or {@code null} when unresolvable. The sixteen
	 * datagen rows collapse onto SEVEN distinct fluids (the offworld rows duplicate the OW
	 * blocks): the four oils + geothermal water ride their spec-table tints, natural gas
	 * its inline p5 tint, lava the white identity. Returns {@code null} (→ overlay only)
	 * for an id that resolves to nothing — the loud-refusal face of the BE's own
	 * {@code sourceState}, never a silent water fallback.
	 */
	public static SpringSkin skinOf(@Nullable String aSpringBlockId) {
		if (aSpringBlockId == null) return null;
		if ("minecraft:lava".equals(aSpringBlockId)) return new SpringSkin(LAVA_STILL, -1);
		int tColon = aSpringBlockId.indexOf(':');
		if (tColon <= 0 || tColon == aSpringBlockId.length() - 1) return null;
		if (!"gt6".equals(aSpringBlockId.substring(0, tColon))) return null;
		String tPath = aSpringBlockId.substring(tColon + 1);
		if (!tPath.endsWith("_block")) return null;
		String tFluid = tPath.substring(0, tPath.length() - "_block".length());
		if ("natural_gas".equals(tFluid)) {
			return new SpringSkin(WATER_STILL, 0x66FFF2B0); // GTFluids.NATURAL_GAS_TYPE initializeClient, the p5 inline tint
		}
		GTFluids.ChemicalFluidSpec tChemical = GTFluids.chemicalSpec(tFluid);
		if (tChemical != null) return new SpringSkin(WATER_STILL, tChemical.tint());
		GTFluids.AquaFluidSpec tAqua = GTFluids.aquaSpec(tFluid);
		if (tAqua != null) return new SpringSkin(WATER_STILL, tAqua.tint());
		return null;
	}

	// ---------------------------------------------------------------------------
	// quad emission: the fluid base (solid, culled, tinted) + the dither shell (cutout)
	// ---------------------------------------------------------------------------

	/** The dispatch gate: exactly while the BE hands in a spring identity (the miss = the JSON fallback). */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return aModelData.has(GTModelProperties.SPRING_FLUID);
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		// the issue #2 same-type partition: solid = the tinted fluid body, cutout = the
		// dither shell (the solid shader has no alpha discard, so the shell's transparent
		// holes would paint opaque gray over the fluid there); the null pass (breaking
		// overlays — IForgeBakedModel: return all their quads) gets both
		if (aRenderType != null && !aRenderType.equals(RenderType.solid()) && !aRenderType.equals(RenderType.cutout())) {
			return List.of();
		}
		String tId = aModelData.get(GTModelProperties.SPRING_FLUID);
		if (tId == null) return List.of();
		Layers tLayers = quadsOf(skinOf(tId));
		List<BakedQuad> tQuads = aRenderType == null ? tLayers.all()
				: aRenderType.equals(RenderType.solid()) ? tLayers.base() : tLayers.overlay();
		if (aSide == null) {
			// the #16 cull sync (the GTOreBakedModel ruling): the chunk builder's null-SIDE
			// pass renders unconditionally (ModelBlockRenderer.java:81-85/:106-110) — the
			// cullface-synced quads flow through the per-direction passes only; the null
			// RenderTYPE pass (items, breaking overlays) still carries everything
			return aRenderType == null ? tQuads : List.of();
		}
		List<BakedQuad> rOut = new ArrayList<>(2);
		for (BakedQuad tQuad : tQuads) if (tQuad.getDirection() == aSide) rOut.add(tQuad);
		return rOut;
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState aState, RandomSource aRand, ModelData aData) {
		if (aData != null && aData.has(GTModelProperties.SPRING_FLUID)) {
			return ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout());
		}
		return getFallbackModel().getRenderTypes(aState, aRand, aData);
	}

	/** The fluid base rides the particle too (the upstream fluid-layer semantics, the particle-layer mapping). */
	@Override
	public TextureAtlasSprite getParticleIcon(ModelData aData) {
		if (aData != null && aData.has(GTModelProperties.SPRING_FLUID)) {
			SpringSkin tSkin = skinOf(aData.get(GTModelProperties.SPRING_FLUID));
			if (tSkin != null && tSkin.stillSprite() != null) {
				TextureAtlasSprite tSprite = mSpriteLookup.apply(materialOf(tSkin.stillSprite()));
				if (tSprite != null) return tSprite;
			}
		}
		return getFallbackModel().getParticleIcon(aData);
	}

	/** The lazy per-skin bake (the GTOreBakedModel double-checked form, keyed by the immutable skin). */
	private Layers quadsOf(@Nullable SpringSkin aSkin) {
		SpringSkin tKey = aSkin == null ? OVERLAY_ONLY : aSkin;
		Layers tLayers = mQuadsBySkin.get(tKey);
		if (tLayers == null) {
			synchronized (this) {
				tLayers = mQuadsBySkin.computeIfAbsent(tKey, this::bakeQuads);
			}
		}
		return tLayers;
	}

	/** The full 0..1 fluid cube (solid set, culled, tinted) + its COPLANAR dither twin (cutout set, the #16 fix). */
	private Layers bakeQuads(SpringSkin aSkin) {
		List<BakedQuad> rBase = new ArrayList<>(6), rOverlay = new ArrayList<>(6);
		// the shared full 0..1 cube — base and overlay bake the SAME box (#16: coplanar,
		// the grass-block precedent; the UVs ride the same 0..16 box bounds)
		double[] tCube = {0, 0, 0, 1, 1, 1};
		if (aSkin.stillSprite() != null) { // the atlas-gap skip rides the wire form (never render garbage)
			TextureAtlasSprite tBase = mSpriteLookup.apply(materialOf(aSkin.stillSprite()));
			if (tBase != null) {
				for (Direction tFace : Direction.values()) {
					// the p32 form: tintIndex -1 (no runtime lookup can double-dye), the colour
					// multiplied into the vertex data at bake time; the -1 identity = the raw quad
					BakedQuad tQuad = bakeQuad(tFace, tCube, tBase, -1, tFace);
					rBase.add(aSkin.tintARGB() == -1 ? tQuad : retinted(tQuad, aSkin.tintARGB()));
				}
			}
		}
		TextureAtlasSprite tOverlay = mSpriteLookup.apply(materialOf(OVERLAY_SPRITE));
		if (tOverlay != null) { // the transparent holes are the tinted fluid body showing through
			// cull synced with the base face (the dispatch routes it through the per-direction pass)
			for (Direction tFace : Direction.values()) rOverlay.add(bakeQuad(tFace, tCube, tOverlay, -1, tFace));
		}
		List<BakedQuad> rAll = new ArrayList<>(rBase.size() + rOverlay.size());
		rAll.addAll(rBase);
		rAll.addAll(rOverlay);
		return new Layers(List.copyOf(rBase), List.copyOf(rOverlay), List.copyOf(rAll));
	}

	// ---------------------------------------------------------------------------
	// the bake recipe (the GTOreBakedModel face-bakery form, uvOf verbatim)
	// ---------------------------------------------------------------------------

	private static Material materialOf(ResourceLocation aSpriteId) {
		return new Material(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS, aSpriteId);
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

	/** The bake-time tint: the colour multiplied into the vertex data (the {@link GTMachineTintModel#retintVertices} product). */
	private static BakedQuad retinted(BakedQuad aQuad, int aTint) {
		return new BakedQuad(GTMachineTintModel.retintVertices(aQuad.getVertices(), aTint),
				aQuad.getTintIndex(), aQuad.getDirection(), aQuad.getSprite(), aQuad.isShade());
	}

	/** One box quad over FaceBakery (model space 0..16, box-bounds UVs — the GTOreBakedModel.bakeQuad form). */
	private BakedQuad bakeQuad(Direction aFace, double[] aBox, TextureAtlasSprite aSprite, int aTintIndex,
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
	// static BakedModel face: the GTDynamicBakedModel fallback delegation covers it —
	// only the ModelData-aware particle arm needs the override above.
	// ---------------------------------------------------------------------------

	/**
	 * The baked-model replacement (the {@link GTMachineTintModel} ModifyBakingResult hook
	 * shape) over the ONE spring block: every state's freshly baked model swaps for this
	 * wrapper (the dynamic-model guard is order-safe against GTRenderModelListener's own
	 * hook — whichever runs first, the dynamic model ends up the map value).
	 */
	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		Block tSpring = GTBlockEntities.FLUID_SPRING.get();
		for (BlockState tState : tSpring.getStateDefinition().getPossibleStates()) {
			var tKey = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(tState);
			BakedModel tBaked = aEvent.getModels().get(tKey);
			if (tBaked != null && !(tBaked instanceof GTDynamicBakedModel)) {
				aEvent.getModels().put(tKey, new GTFluidSpringBakedModel(tBaked,
						aMaterial -> net.minecraft.client.Minecraft.getInstance().getTextureAtlas(
								aMaterial.atlasLocation()).apply(aMaterial.texture())));
			}
		}
	}
}
