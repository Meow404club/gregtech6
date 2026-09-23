package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;
import gregtech6.util.UT6;

/**
 * The progress sensor cover — 1.20.1 port of gregapi/cover/covers/CoverScaleProgress.java
 * (:35-51, task p35-covers-display-scale-6; upstream MultiItemTechnological.java:77
 * meta 1018 "Progress Sensor", dump 进度传感器). "Emits depending on Progress": every
 * tickPost the 15-step scale (:42) rides the VALUE lane (the redstone output the Scale
 * base maps onto the host exits) — 0 idle, 15 done, the 14 steps between map the
 * remaining-progress fraction ({@code 14 - clamp((max-progress)*14/max, 0..13)}).
 *
 * <p>Host admission (:36): the progress face — the ported machine forms carry the
 * progress lane pair (:1018/:1019 normalisation, the CoverMachineLanes declared mapping;
 * the {@code canTick()} half folds).
 *
 * <p>Texture (:50): upstream {@code machines/covers/progressredstone/circuit} path-maps
 * to {@code gt6:block/progress_redstone/circuit} (the byte-identical borrow,
 * assets/README.md).
 */
public class CoverScaleProgress extends AbstractCoverAttachmentScale {

	/** The sprite path — upstream CoverScaleProgress.java:50, lowercased/underscored. */
	public static final String SPRITE_PATH = "block/progress_redstone/circuit";

	/** The sprite id; static so the tables stay registry-free. */
	public static ResourceLocation sprite() {
		return new ResourceLocation("gt6", SPRITE_PATH);
	}

	/** Upstream :36 — the progress-face carrier (the ported machine-form mapping) only. */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return !CoverMachineLanes.isMachineForm(aData.mTileEntity);
	}

	/**
	 * Upstream :41-42 verbatim over the ported lane reads — the progress scale, synced
	 * with the block update (the {@code T} third argument).
	 */
	@Override
	public void onTickPost(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (aIsServerSide) {
			long tProgress = CoverMachineLanes.progressValue(aData.mTileEntity), tMax = CoverMachineLanes.progressMax(aData.mTileEntity);
			aData.value(aCoverSide, UT6.bind4(tProgress <= 0 || tMax <= 0 ? 0 : tProgress >= tMax ? 15 : 14 - (int) Math.max(0, Math.min(13, ((tMax - tProgress) * 14L) / tMax))), true);
		}
	}

	/** Upstream :50 — the sensor circuit art. */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return sprite();
	}
}
