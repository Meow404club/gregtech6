package gregtech6.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.registry.GTMachines;

/**
 * The machine paint tint baked INTO the quads (task p32-render-embeddium-tint): the
 * baked models of the {@link GTMachines#paintableBlockArray()} machine domain are
 * wrapped here and {@link GTMachinePaintTint#tintARGB} is multiplied into the body
 * quads' VERTEX COLOURS at {@code getQuads} time — the colour rides the model instead
 * of the runtime {@code BlockColor} route.
 *
 * <p>WHY the seam moved (the p32 live evidence): the colour VALUES of the old
 * {@code BlockColor} half are correct — the pinned offline suite proves it — but the
 * rendered terrain vertices came out achromatic in a live client with BOTH the vanilla
 * chunk builder and Embeddium 0.3.31 (probe: the handler ran with the right state/BE and
 * returned {@code 0xFFD2823C}, while the visible machine pixels measured
 * {@code (178,178,178)} — the exact untinted {@code texture x AO}). The known_bugs
 * embeddium_tint_no_shader report rides the same flaw: with a shader pack the terrain
 * pipeline takes the legacy colour format and the tint shows, without it the
 * field-deployed look is the untinted grayscale. Baking the colour into the quads
 * removes the runtime lookup from the equation entirely — the tint cannot be dropped by
 * any chunk builder, threaded rebuild, snapshot cache or vertex writer downstream.
 *
 * <p>Semantics are byte-for-byte the old seam's: {@link GTMachinePaintTint#tintARGB} is
 * still the single colour source (painted PAINT snapshot wins, unpainted falls back to
 * the row material, {@code -1} = the no-tint identity for the material-less
 * registrations — the P23 barrel contract). The decal overlay elements (no tintindex,
 * the P22 split) pass through untouched. The retinted copies carry {@code tintIndex -1},
 * so a runtime colour lookup can never multiply a second time.
 *
 * <p>RED LINE (render-route ADR, unchanged): {@code getDynamicQuads} reads ONLY the
 * immutable {@link ModelData} the chunk build hands in — no live {@code BlockEntity}.
 *
 * <p>CLIENT-ONLY ({@code @OnlyIn(Dist.CLIENT)} — registered from the mod-construct event
 * under the dist guard, the {@link GTOvenClientListener} shape).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTMachineTintModel extends GTDynamicBakedModel {

	/** Retinted-copy cache guard: more distinct spray colours than this clears the table. */
	private static final int CACHE_CAP = 256;

	private final Block mBlock;

	private final Map<Integer, Map<BakedQuad, BakedQuad>> mTintedQuads = new ConcurrentHashMap<>();

	private GTMachineTintModel(BakedModel aFallbackModel, Block aBlock) {
		super(aFallbackModel);
		mBlock = aBlock;
	}

	/**
	 * Every machine is tinted (the tint resolves from the snapshot's PAINT or the row
	 * material — never from a RENDER_SNAPSHOT-style property), so the dispatch gate is
	 * unconditional.
	 */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return true;
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		// the tint source IS the ModelData the chunk build hands in: PAINT while painted
		// (the 03 base getModelData), the row material while unpainted, -1 for the
		// material-less registrations — and every paint change re-enters here through the
		// block-update rebuild (the GTRenderUpdates pair), so the retint follows live.
		// The part-family blocks (task p38-issue8-multipart-tint) carry NO paint BE — their
		// ModelData is always empty, so the material fallback IS the upstream mRGBa, fixed
		// per block.
		return tintQuads(getFallbackModel().getQuads(aState, aSide, aRand),
				GTMachinePaintTint.tintARGB(aModelData, GTMachinePaintTint.tintMaterialOf(mBlock), 0),
				mTintedQuads);
	}

	/**
	 * The pure retint the tests drive (and {@link #getDynamicQuads} consumes): the tinted
	 * body quads ({@code tintIndex == 0}) swap for the cached retinted copies, the P22
	 * decal overlays ({@code tintIndex != 0}) pass through as the shared instances, and
	 * the {@code -1} tint identity returns the input list unchanged (the P23 barrel
	 * contract). {@code aCache} is the per-wrapper retinted-copy table.
	 */
	static List<BakedQuad> tintQuads(List<BakedQuad> aQuads, int aTint,
			Map<Integer, Map<BakedQuad, BakedQuad>> aCache) {
		if (aTint == -1) {
			return aQuads;
		}
		if (aCache.size() > CACHE_CAP) {
			aCache.clear(); // ponytail: adversarial unlimited spray colours reset the cache; an LRU if it ever matters
		}
		Map<BakedQuad, BakedQuad> tBySource = aCache.computeIfAbsent(aTint, tT -> new ConcurrentHashMap<>());
		List<BakedQuad> rOut = new ArrayList<>(aQuads.size());
		for (BakedQuad tQuad : aQuads) {
			if (tQuad.getTintIndex() != 0) {
				rOut.add(tQuad); // the P22 overlay decals: untinted by design, shared instance
				continue;
			}
			rOut.add(tBySource.computeIfAbsent(tQuad, tQuad1 -> retint(tQuad1, aTint)));
		}
		return rOut;
	}

	/** The tint multiplied into each vertex colour slot (IQuadTransformer COLOR = 3, stride 8). */
	private static BakedQuad retint(BakedQuad aQuad, int aTint) {
		return new BakedQuad(retintVertices(aQuad.getVertices(), aTint), -1, aQuad.getDirection(),
				aQuad.getSprite(), aQuad.isShade());
	}

	/**
	 * The pure recolour the tests drive (and {@link #retint} consumes): per-channel
	 * {@code (colour * tint + 255) >> 8} over every vertex of the baked vertex data.
	 *
	 * <p>ENCODING (issue #14 fix, the single point): the baked COLOR slot int stores its
	 * channels {@code A<<24|B<<16|G<<8|R} — byte order R,G,B,A, the layout vanilla's
	 * putBulkData consumes (VertexConsumer.java:90-92 reads bytes 12/13/14 = R/G/B) and
	 * Forge's own {@link QuadTransformers#toABGR} defines for {@code applyingColor} — while
	 * the tint arrives ARGB (the BlockColors ecosystem encoding every colour seam here
	 * resolves). Multiplying an ARGB tint in by byte position swapped R and B, so every
	 * warm colour rendered cool (copper blue, gold cyan; the R==B rows like tungsten
	 * 50,50,50 hid it — the #8 burning-box side observation). The tint is converted ONCE
	 * here, so every consumer domain (machines, kitchen, controllers, ores, the fluid
	 * spring) inherits the fix through the shared product.
	 */
	public static int[] retintVertices(int[] aVertices, int aTint) {
		int tTintABGR = QuadTransformers.toABGR(aTint);
		int[] rVertices = new int[aVertices.length];
		for (int v = 0; v * IQuadTransformer.STRIDE < aVertices.length; v++) {
			int tBase = v * IQuadTransformer.STRIDE;
			System.arraycopy(aVertices, tBase, rVertices, tBase, IQuadTransformer.STRIDE);
			rVertices[tBase + IQuadTransformer.COLOR] = mulColor(aVertices[tBase + IQuadTransformer.COLOR], tTintABGR);
		}
		return rVertices;
	}

	/** Per-channel {@code (c * t + 255) / 256} over the packed colour (alpha included) — full-value exact. */
	private static int mulColor(int aColour, int aTint) {
		int rResult = 0;
		for (int tShift = 0; tShift < 32; tShift += 8) {
			int tChannel = (((aColour >> tShift) & 255) * ((aTint >> tShift) & 255) + 255) >> 8;
			rResult |= (tChannel & 255) << tShift;
		}
		return rResult;
	}

	/**
	 * The baked-model replacement (the ModifyBakingResult hook, the
	 * {@link GTRenderModelListener} shape read directly off the event map): every
	 * paintable-array state swaps in this wrapper over its freshly baked model — the
	 * machine domain ({@code GTMachines.paintableBlockArray}), the part family
	 * ({@code GTMultiBlocks.partPaintableBlockArray}), since task
	 * p38-c3-kitchen-tint-shape the kitchen family
	 * ({@code GT6Kitchen.paintableBlockArray} — the tintindex-0 faces resolve the carrier
	 * material, the #7 reservation closing) and, since task
	 * p38-c2-controller-tint, the controller/energy domain (the 12 multiblock mains, the
	 * 15 EU-bridge rungs, the 10 laser rungs, the magic absorber); since task
	 * issue8-residual the #8 stragglers (the 25 tank valve controllers, the 8 dedicated
	 * crucible walls). Blocks that already
	 * carry a dynamic model (the oven ladder's {@code GTOvenOverlayModel} chain) are
	 * skipped — they keep their own render route.
	 */
	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		for (Block tBlock : GTMachines.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GTMultiBlocks.partPaintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6Kitchen.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6Turbines.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6DynamoHousings.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : GTMachines.bridgePaintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6Lasers.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6MagicAbsorbers.paintableBlockArray()) wrapStates(tBlock, aEvent);
		// task issue8-residual — the #8 stragglers: the 25 tank valve controllers (the row
		// material rides the controller gate through GTTankValveBlock) and the 8 crucible
		// walls (the dedicated GTCrucibleWallBlock part carriers)
		for (Block tBlock : gregtech6.registry.GT6Tanks.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6Crucibles.paintableWallBlockArray()) wrapStates(tBlock, aEvent);
		// issue #11 — the burning boxes join the baked-tint domain: every row carries
		// NBT_MATERIAL upstream (Loader :519-548/:619-704), the body cube is the
		// tintindex-0 seat, the colour comes off GTBasicMachineBlock.materialOf like
		// every other machine domain (the lit decals carry NO tintindex, so the
		// burning glow passes through untinted — the P22 decal contract)
		for (Block tBlock : gregtech6.registry.GT6BurningBoxes.paintableBlockArray()) wrapStates(tBlock, aEvent);
		// task r3-world-tint-render-type — the C5 boiler clean-up: the 26 steam boiler
		// tanks join (every upstream row carries NBT_MATERIAL, Loader :553-579; the
		// shared model's body cube is the tintindex-0 seat since this card)
		for (Block tBlock : gregtech6.registry.GT6Boilers.paintableBlockArray()) wrapStates(tBlock, aEvent);
	}

	/**
	 * The kitchen paint tint, the INVENTORY half (task p38-c3-kitchen-tint-shape): the
	 * family's BlockItems registered over the shared {@link GTItemPaintTint} lambda — the
	 * unpainted stacks resolve the carrier material through the combined
	 * {@code GTMachinePaintTint.tintMaterialOf} dispatch (the #8 part-registration mirror
	 * shape). It lives on this subscriber rather than GTClientHandlers because the kitchen
	 * card's FILES_SCOPE draws the client seam at client/render/; explicit registration is
	 * still mandatory — a BlockColor does NOT colour its BlockItem and vanilla
	 * {@code ItemColors.createDefault} has no BlockItem delegation (ItemColors.java:25-93).
	 */
	@SubscribeEvent
	public static void onRegisterKitchenPaintItemColors(RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<net.minecraft.world.item.Item> tItems = new java.util.ArrayList<>();
		for (Block tBlock : gregtech6.registry.GT6Kitchen.paintableBlockArray()) tItems.add(tBlock.asItem());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tItems.toArray(net.minecraft.world.item.Item[]::new));
	}

	/**
	 * The controller/energy-domain paint tint, the INVENTORY half (task
	 * p38-c2-controller-tint): the 38 new-domain blocks' BlockItems ride the shared
	 * {@link GTItemPaintTint} lambda through the combined
	 * {@code GTMachinePaintTint.tintMaterialOf} dispatch — the creative-tab face (a
	 * BlockItem is NOT coloured by any baked world tint, ItemColors.java:25-93). The
	 * per-domain-listener registration is the {@code GT6TreeClientListener} shape (this
	 * class IS the domain's client seam; GTClientHandlers keeps the machine/part/barrel
	 * faces it already owns). The coke-oven bricks join the existing part registration
	 * automatically through {@code partPaintableBlockArray}.
	 */
	@SubscribeEvent
	public static void onRegisterControllerPaintItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<Item> tPaintItems = new ArrayList<>();
		for (Block tBlock : gregtech6.registry.GT6Turbines.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6DynamoHousings.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : GTMachines.bridgePaintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6Lasers.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6MagicAbsorbers.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tPaintItems.toArray(Item[]::new));
	}

	/**
	 * The tank-valve + crucible-wall paint tint, the INVENTORY half (task issue8-residual):
	 * the #8 stragglers' BlockItems ride the shared {@link GTItemPaintTint} lambda through
	 * the combined {@code GTMachinePaintTint.tintMaterialOf} dispatch — the creative-tab
	 * face (a BlockItem is NOT coloured by any baked world tint, ItemColors.java:25-93).
	 * The registration mirrors {@link #onRegisterKitchenPaintItemColors} (this class is the
	 * shared client tint seam; GTClientHandlers keeps the faces it already owns).
	 */
	@SubscribeEvent
	public static void onRegisterValveWallPaintItemColors(RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<net.minecraft.world.item.Item> tItems = new java.util.ArrayList<>();
		for (Block tBlock : gregtech6.registry.GT6Tanks.paintableBlockArray()) tItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6Crucibles.paintableWallBlockArray()) tItems.add(tBlock.asItem());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tItems.toArray(net.minecraft.world.item.Item[]::new));
	}

	/**
	 * The burning-box paint tint, the INVENTORY half (issue #11 texture half): the 97
	 * BlockItems ride the shared {@link GTItemPaintTint} lambda through the combined
	 * {@code GTMachinePaintTint.tintMaterialOf} dispatch (which resolves the row
	 * material via the common {@code GTBasicMachineBlock.materialOf} gate, task p27) —
	 * the creative-tab face the world half cannot colour (a BlockItem is NOT coloured
	 * by any baked world tint, ItemColors.java:25-93). The per-domain-listener
	 * registration is the {@link #onRegisterControllerPaintItemColors} shape.
	 */
	@SubscribeEvent
	public static void onRegisterBurningBoxPaintItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<Item> tPaintItems = new ArrayList<>();
		for (Block tBlock : gregtech6.registry.GT6BurningBoxes.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tPaintItems.toArray(Item[]::new));
	}

	/**
	 * The boiler paint tint, the INVENTORY half (task r3-world-tint-render-type, the C5
	 * clean-up): the 26 boiler BlockItems ride the shared {@link GTItemPaintTint} lambda
	 * through the combined {@code GTMachinePaintTint.tintMaterialOf} dispatch (the row
	 * material resolves through the common {@code GTBasicMachineBlock.materialOf} gate —
	 * the burning-box form, 43f48149b).
	 */
	@SubscribeEvent
	public static void onRegisterBoilerPaintItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<Item> tPaintItems = new ArrayList<>();
		for (Block tBlock : gregtech6.registry.GT6Boilers.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tPaintItems.toArray(Item[]::new));
	}

	/** One block's state walk (the shared swap body; the dynamic-model guard is order-safe against GTRenderModelListener's own hook). */
	private static void wrapStates(Block tBlock, ModelEvent.ModifyBakingResult aEvent) {
		for (BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
			var tKey = BlockModelShaper.stateToModelLocation(tState);
			BakedModel tBaked = aEvent.getModels().get(tKey);
			// the dynamic-model guard is order-safe against GTRenderModelListener's own
			// hook: whichever runs first, the dynamic model ends up the map value
			if (tBaked != null && !(tBaked instanceof GTDynamicBakedModel)) {
				aEvent.getModels().put(tKey, new GTMachineTintModel(tBaked, tBlock));
			}
		}
	}
}
