package gregtech6.covers;

import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import gregtech6.client.render.GTRenderSnapshot;

/**
 * The immutable cover render snapshot — the {@link GTRenderSnapshot} record the host BE
 * hands to the render thread (task p4-cover-core ⑥). Carries the per-face cover sprite
 * id ({@code getCoverTextureSurface}, the ICover :194 hook); the plate GEOMETRY (the
 * 2px-thickness slab of the upstream BOXES_COVERS) is constant and stays model-side in
 * {@code CoverPlateModel}.
 *
 * <p>Thread safety per the snapshot contract: {@link Direction} keys are JVM
 * singletons and {@link ResourceLocation} values are immutable; the map is frozen by
 * the {@code GTModelProperties} builder ({@code Map.copyOf}) before it enters
 * {@code ModelData} (the ModelData.java:28 iron law).
 *
 * <p>GTCEu 对位：ICoverableRenderer.renderCovers (ICoverableRenderer.java:43-91) 从
 * COVER_MODEL_DATA 拿每面数据；我们把同形数据提前冻进快照，getQuads 只读不查世界。
 */
public record GTCoverRenderSnapshot(Map<Direction, ResourceLocation> coverSprites) implements GTRenderSnapshot {

	public GTCoverRenderSnapshot {
		coverSprites = Map.copyOf(coverSprites); // second freeze line of defense beside the builder
	}

	/** Whether the given face carries a cover sprite. */
	public boolean hasCover(Direction aFace) {
		return coverSprites.containsKey(aFace);
	}

	/** The GT6 side byte ({@code Direction.get3DDataValue()}) order mask of the covered faces. */
	public byte mask() {
		byte rMask = 0;
		for (Direction tFace : coverSprites.keySet()) rMask |= (byte) (1 << tFace.get3DDataValue());
		return rMask;
	}

	/** @return the cover sprite on that face or null. */
	public ResourceLocation sprite(Direction aFace) {
		return coverSprites.get(aFace);
	}
}
