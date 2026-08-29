package gregtech6.client.render;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Base for all GT6 dynamic baked models (C-grade render foundation, ADR
 * 2026-08-30-p4-render-route-execution). Implements the Forge dispatch skeleton of
 * {@link IDynamicBakedModel#getQuads(BlockState, Direction, RandomSource, ModelData, RenderType)}
 * (IDynamicBakedModel.java:35 — the forced 5-arg overload):
 * <ul>
 * <li>{@link ModelData} carries a {@link GTModelProperties#RENDER_SNAPSHOT} (the BE
 *     participates in dynamic rendering this frame) →
 *     {@link #getDynamicQuads(BlockState, Direction, RandomSource, ModelData, RenderType)}
 *     assembles the quads from the snapshot;</li>
 * <li>no snapshot (item form, non-BE block, BE without dynamic data) → fall back to the
 *     blockstate-baked model the resource pack produced (the vanilla JSON path).</li>
 * </ul>
 *
 * <p>RED LINE (render-route ADR): {@code getQuads} (and everything it calls) must never
 * hold or reach a live {@code BlockEntity} — the model reads ONLY the immutable snapshot
 * inside the {@link ModelData}, which the render thread may touch from a worker thread
 * (IForgeBlockEntity.java:171) while the client tick thread mutates live state. The
 * snapshot flows in through the BE's {@code getModelData()} (IForgeBlockEntity.java:174)
 * and gets refreshed through the {@link GTRenderUpdates#scheduleRenderUpdate} pair. BEWLR
 * and TESR are forbidden project-wide by the same ADR (animation exception stays pooled).
 *
 * <p>GTCEu 对位：MachineModel.getQuads (MachineModel.java:238-258) 同型分发骨架（命中走机器
 * quads，未命中走 item/默认渲染），但其 LEVEL/POS 属性方案在 getQuads 里现场
 * MetaMachine.getMachine(level, pos)（MachineModel.java:215/:265）——正是我们的红线所禁，
 * 故本基座只认快照。
 */
public abstract class GTDynamicBakedModel implements IDynamicBakedModel {

	/** The blockstate-baked fallback model (replaced via the ModifyBakingResult hook). */
	private final BakedModel mFallbackModel;

	protected GTDynamicBakedModel(BakedModel aFallbackModel) {
		mFallbackModel = Objects.requireNonNull(aFallbackModel, "fallback model");
	}

	/** The blockstate-baked model this dynamic model replaces (also the item-form render path). */
	public final BakedModel getFallbackModel() {
		return mFallbackModel;
	}

	@Override
	public final List<BakedQuad> getQuads(@Nullable BlockState aState, @Nullable Direction aSide, RandomSource aRand,
			ModelData aModelData, @Nullable RenderType aRenderType) {
		if (aModelData != null && supportsDynamicQuads(aModelData)) {
			// ModelData hit → subclass hook assembles quads from the immutable snapshot.
			return getDynamicQuads(aState, aSide, aRand, aModelData, aRenderType);
		}
		// Miss → fall back to the blockstate-baked model (vanilla JSON path). The 3-arg
		// overload is the plain BakedModel contract the fallback was baked against.
		return mFallbackModel.getQuads(aState, aSide, aRand);
	}

	/**
	 * Dispatch gate. Default: the blockstate carries a {@link GTModelProperties#RENDER_SNAPSHOT}.
	 * Subclasses with stricter gating (e.g. side-specific snapshot properties) override this.
	 */
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return aModelData.has(GTModelProperties.RENDER_SNAPSHOT);
	}

	/**
	 * Subclass hook: assemble the quads for one render pass. Reads ONLY the immutable
	 * snapshot inside {@code aModelData} — RED LINE: no BlockEntity references may be
	 * held or dereferenced here (render-thread snapshot semantics, class doc above).
	 *
	 * @param aState      the blockstate being rendered (may be null for item rendering)
	 * @param aSide       the culling side (null = unculled pass, e.g. breaking overlay)
	 * @param aRand       the per-position random (same instance across the faces of one block)
	 * @param aModelData  the per-block snapshot carrier
	 * @param aRenderType the chunk layer being baked (null = all quads, IForgeBakedModel.java:41)
	 */
	protected abstract List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType);

	// ---------------------------------------------------------------------------
	// BakedModel face: delegate the static properties to the fallback model, so a
	// bare subclass only implements getDynamicQuads.
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
		return mFallbackModel.getParticleIcon();
	}
}
