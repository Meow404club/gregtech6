package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;
import gregtech6.tileentity.machines.ITileEntitySwitchableMode;

/**
 * The selector-cover base — 1.20.1 port of
 * gregapi/cover/covers/AbstractCoverAttachmentSelector.java:29-35 (task
 * p34-covers-gameplay-10). The one inherited behaviour: removing a selector cover
 * resets the host's mode dial to 0 (:31-34) — a selectorless machine must not keep the
 * last selector's command. The four concrete selectors (Tag/Manual/Redstone/
 * ButtonPanel) narrow the placement gate to {@link ITileEntitySwitchableMode} hosts
 * and drive the dial from their own channels.
 *
 * <p>The shared click-geometry helper ({@link #facingCoordsClicked}) is the upstream
 * UT.Code.getFacingCoordsClicked (UT.java:1734-1744) verbatim — the Manual selector's
 * arrow/bit zones and the ButtonPanel's 4x4 grid both key on it. It lives here because
 * both consumers sit in this package (the UT6 shared file is outside the card's file
 * scope).
 */
public abstract class AbstractCoverAttachmentSelector extends AbstractCoverDefault {

	/** Upstream :31-34 verbatim — the removal resets the host dial to 0. */
	@Override
	public void onCoverRemove(byte aSide, CoverData aData, @Nullable Entity aPlayer) {
		super.onCoverRemove(aSide, aData, aPlayer);
		if (aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable) tSwitchable.setStateMode((byte) 0);
	}

	/**
	 * The placement gate the four selectors share — upstream CoverSelectorTag :41 /
	 * CoverSelectorManual :37 / CoverSelectorRedstone :37 / CoverSelectorButtonPanel :41
	 * verbatim: the selector only mounts on switchable-mode hosts.
	 */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return !(aData.mTileEntity instanceof ITileEntitySwitchableMode);
	}

	/**
	 * Upstream UT.Code.getFacingCoordsClicked (UT.java:1734-1744) verbatim — the X/Y of
	 * the clicked point on the face, texture-sheet origin at the top left, clamped into
	 * [0, 0.99]; the invalid side folds to the centre.
	 */
	public static float[] facingCoordsClicked(byte aSide, float aHitX, float aHitY, float aHitZ) {
		return switch (aSide) {
		case 0 -> new float[] {Math.min(0.99F, Math.max(0, aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitZ))};
		case 1 -> new float[] {Math.min(0.99F, Math.max(0, aHitX)), Math.min(0.99F, Math.max(0, aHitZ))};
		case 2 -> new float[] {Math.min(0.99F, Math.max(0, 1 - aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
		case 3 -> new float[] {Math.min(0.99F, Math.max(0, aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
		case 4 -> new float[] {Math.min(0.99F, Math.max(0, aHitZ)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
		case 5 -> new float[] {Math.min(0.99F, Math.max(0, 1 - aHitZ)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
		default -> new float[] {0.5F, 0.5F};
		};
	}
}
