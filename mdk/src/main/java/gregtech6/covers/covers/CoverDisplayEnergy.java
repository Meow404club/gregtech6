package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;

/**
 * The energy display cover — 1.20.1 port of gregapi/cover/covers/CoverDisplayEnergy.java
 * (:36-68, task p35-covers-display-scale-6; upstream MultiItemTechnological.java:63
 * meta 1004 "Energy Display Cover", dump 能量显示面板). "Displays contained Energy": every
 * tickPost the 11-step gauge (:44) rides the visual lane — 0 empty, 10 full, the 9 steps
 * between map the remaining-fill fraction ({@code 9 - clamp((cap-stored)*9/cap, 0..8)}).
 *
 * <p>Host admission (:37): the capacitor face — the ported mapping reads the coverable
 * machine forms' energy buffer lane ({@code mEnergy}/{@code mInputMax}, the
 * CoverMachineLanes declared mapping; the {@code canTick()} half folds and the upstream
 * empty-capacitor-types guard folds with the always-present lane).
 *
 * <p>Texture (:48): upstream composes the underlay with the level overlay
 * (BlockTextureMulti); the port borrows the 11 pre-composited plates
 * ({@code gt6:block/energy_display/<level>}, the CoverSelectorTag underlay+digit
 * pre-composite precedent, assets/README.md). The {@code showsConnectorFront} :51 arm is
 * cut with the pooled connector hooks.
 */
public class CoverDisplayEnergy extends AbstractCoverAttachmentDisplay {

	/** The sprite path family — upstream :53-65, underlay+level pre-composited. */
	public static final String SPRITE_PATH = "block/energy_display/%d";

	/** The sprite id for a gauge level (0..10). */
	//? if forge {
	public static ResourceLocation spriteOf(int aLevel) {
		return new ResourceLocation("gt6", String.format(SPRITE_PATH, Math.max(0, Math.min(10, aLevel))));
	}
	//?} else {
	/*public static ResourceLocation spriteOf(int aLevel) {
		return ResourceLocation.fromNamespaceAndPath("gt6", String.format(SPRITE_PATH, Math.max(0, Math.min(10, aLevel))));
	}
	 *///?}

	/** Upstream :37 — the capacitor-face carrier (the ported machine-form mapping) only. */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return !CoverMachineLanes.isMachineForm(aData.mTileEntity);
	}

	/**
	 * Upstream :41-45 verbatim over the ported lane reads — the gauge level formula.
	 */
	@Override
	public void onTickPost(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (aIsServerSide) {
			long tStored = CoverMachineLanes.energyStored(aData.mTileEntity), tCapacity = CoverMachineLanes.energyCapacity(aData.mTileEntity);
			aData.visual(aCoverSide, (short) (tStored <= 0 || tCapacity <= 0 ? 0 : tStored >= tCapacity ? 10 : 9 - (int) Math.max(0, Math.min(8, ((tCapacity - tStored) * 9L) / tCapacity))));
		}
	}

	/** Upstream :48 — the pre-composited gauge plate for the current level. */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return spriteOf(aData.mVisuals[aCoverSide]);
	}
}
