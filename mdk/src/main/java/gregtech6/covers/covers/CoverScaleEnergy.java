package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;
import gregtech6.util.UT6;

/**
 * The energy sensor cover — 1.20.1 port of gregapi/cover/covers/CoverScaleEnergy.java
 * (:36-53, task p35-covers-display-scale-6; upstream MultiItemTechnological.java:73
 * meta 1014 "Energy Sensor", dump 能量传感器). "Emits depending on Energy stored": every
 * tickPost the 15-step scale (:44) rides the VALUE lane — 0 empty, 15 full, the 14 steps
 * between map the remaining-fill fraction, the same formula shape as the progress sensor
 * over the energy buffer lane (the CoverMachineLanes declared capacitor mapping).
 *
 * <p>Texture (:52): upstream {@code machines/covers/energyredstone/circuit} path-maps to
 * {@code gt6:block/energy_redstone/circuit} (the byte-identical borrow,
 * assets/README.md).
 */
public class CoverScaleEnergy extends AbstractCoverAttachmentScale {

	/** The sprite path — upstream CoverScaleEnergy.java:52, lowercased/underscored. */
	public static final String SPRITE_PATH = "block/energy_redstone/circuit";

	/** The sprite id; static so the tables stay registry-free. */
	public static ResourceLocation sprite() {
		return new ResourceLocation("gt6", SPRITE_PATH);
	}

	/** Upstream :37 — the capacitor-face carrier (the ported machine-form mapping) only. */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return !CoverMachineLanes.isMachineForm(aData.mTileEntity);
	}

	/**
	 * Upstream :41-45 verbatim over the ported lane reads — the energy scale, synced with
	 * the block update.
	 */
	@Override
	public void onTickPost(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (aIsServerSide) {
			long tStored = CoverMachineLanes.energyStored(aData.mTileEntity), tCapacity = CoverMachineLanes.energyCapacity(aData.mTileEntity);
			aData.value(aCoverSide, UT6.bind4(tStored <= 0 || tCapacity <= 0 ? 0 : tStored >= tCapacity ? 15 : 14 - (int) Math.max(0, Math.min(13, ((tCapacity - tStored) * 14L) / tCapacity))), true);
		}
	}

	/** Upstream :52 — the sensor circuit art. */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return sprite();
	}
}
