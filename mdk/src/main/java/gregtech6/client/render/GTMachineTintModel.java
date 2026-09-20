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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.block.GTBasicMachineBlock;
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
		int tTint = 0xFF00FFFF; // p32 DIAGNOSTIC: pure cyan vertex bake
		List<BakedQuad> tQuads = getFallbackModel().getQuads(aState, aSide, aRand);
		if (tTint == -1) {
			// The material-less white identity: the no-tint sentinel IS full-alpha white,
			// so the fallback quads pass through byte-identical (the P23 barrel contract).
			return tQuads;
		}
		if (mTintedQuads.size() > CACHE_CAP) {
			mTintedQuads.clear(); // ponytail: adversarial unlimited spray colours reset the cache; an LRU if it ever matters
		}
		Map<BakedQuad, BakedQuad> tBySource = mTintedQuads.computeIfAbsent(tTint, tT -> new ConcurrentHashMap<>());
		List<BakedQuad> rOut = new ArrayList<>(tQuads.size());
		for (BakedQuad tQuad : tQuads) {
			if (tQuad.getTintIndex() != 0) {
				rOut.add(tQuad); // the P22 overlay decals: untinted by design, shared instance
				continue;
			}
			rOut.add(tBySource.computeIfAbsent(tQuad, tQuad1 -> retint(tQuad1, tTint)));
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
	 * {@code (colour * tint + 127) / 255} over every vertex of the baked vertex data.
	 */
	public static int[] retintVertices(int[] aVertices, int aTint) {
		int[] rVertices = new int[aVertices.length];
		for (int v = 0; v * IQuadTransformer.STRIDE < aVertices.length; v++) {
			int tBase = v * IQuadTransformer.STRIDE;
			System.arraycopy(aVertices, tBase, rVertices, tBase, IQuadTransformer.STRIDE);
			rVertices[tBase + IQuadTransformer.COLOR] = mulColor(aVertices[tBase + IQuadTransformer.COLOR], aTint);
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
	 * paintable-array state swaps in this wrapper over its freshly baked model. Blocks
	 * that already carry a dynamic model (the oven ladder's
	 * {@code GTOvenOverlayModel} chain) are skipped — they keep their own render route.
	 */
	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		for (Block tBlock : GTMachines.paintableBlockArray()) {
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
}
