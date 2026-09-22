package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;
import gregtech6.tileentity.machines.ITileEntitySwitchableMode;
import gregtech6.util.UT6;

/**
 * The manual selector cover — 1.20.1 port of gregapi/cover/covers/CoverSelectorManual
 * .java:36-118 (task p34-covers-gameplay-10; upstream item id 1008 "Manual Selector").
 * The plate IS the GUI: right-click zones drive the host dial — an up-arrow decrement
 * zone and a down-arrow increment zone in the upper band (:60-70), four bit-toggle
 * buttons in the lower band (:71-87, the 8/4/2/1 split of the 4-bit mode). The visual
 * lane mirrors the host mode for the per-mode plate art (:94).
 *
 * <p>The mode mirror: placement (:40-43) and block updates (:50-52) read the host dial
 * into the visual lane; a chunk load (:45-48) writes the SAVED visual lane BACK into
 * the dial (the manual selector is the dial's persistence — the upstream literal). The
 * blocked host (:51 {@code !aData.mStopped}) does not mirror.
 *
 * <p>DECLARED CUT — none of substance: the {@code BACKGROUND_COVER} attachment/holder
 * stack (:95-96) folds into the single per-mode sprite (the CoverSelectorTag class-doc
 * cut).
 */
public class CoverSelectorManual extends AbstractCoverAttachmentSelector {

	/** The atlas sprite of the mode plates — the per-mode art (upstream sTextures, :99-116). */
	//? if forge {
	public static ResourceLocation spriteOf(byte aMode) {
		return new ResourceLocation("gt6", "block/manualselector/" + UT6.bind4(aMode));
	}
	//?} else {
	/*public static ResourceLocation spriteOf(byte aMode) {
		return ResourceLocation.fromNamespaceAndPath("gt6", "block/manualselector/" + UT6.bind4(aMode));
	}
	 *///?}

	/** Upstream :40-43 — placement mirrors the host dial into the visual lane. */
	@Override
	public void onCoverPlaced(byte aSide, CoverData aData, @Nullable Entity aPlayer, net.minecraft.world.item.ItemStack aCover) {
		super.onCoverPlaced(aSide, aData, aPlayer, aCover);
		if (aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable)
			aData.visual(aSide, UT6.bind4(tSwitchable.getStateMode()));
	}

	/** Upstream :45-48 — a chunk load writes the SAVED mode back into the dial. */
	@Override
	public void onCoverLoaded(byte aSide, CoverData aData) {
		super.onCoverLoaded(aSide, aData);
		if (aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable)
			tSwitchable.setStateMode((byte) UT6.bind4(aData.mVisuals[aSide] & 15));
	}

	/** Upstream :50-52 — a block update mirrors the host dial (blocked hosts do not mirror). */
	@Override
	public void onBlockUpdate(byte aSide, CoverData aData) {
		if (!aData.mStopped && aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable)
			aData.visual(aSide, UT6.bind4(tSwitchable.getStateMode()));
	}

	/**
	 * Upstream :55-92 verbatim — the zone map (CS.java:492-513: PX_P[i] = i/16,
	 * PX_N[i] = 1 - i/16). The upper band ({@code y} in [PX_P[1], PX_P[4]] =
	 * [1/16, 4/16]): left third decrements (0 wraps to 15), right third increments
	 * (15 wraps to 0). The lower band ({@code y} in [PX_N[7], PX_N[4]] =
	 * [7/16, 12/16]): four toggle buttons splitting the row at PX_P[5]/PX_P[8]/PX_N[5],
	 * flipping bits 8/4/2/1. A server-side zone hit writes the new mode through
	 * {@code setStateMode} and mirrors its RETURN into the visual lane (:88); the hit
	 * consumes the click with the click animation, a miss falls through to the host (:89-91).
	 */
	@Override
	public boolean onCoverClickedRight(byte aSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (!aData.mStopped && aSide == aSideClicked && aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable) {
			boolean rReturn = false;
			byte tMode = UT6.bind4(aData.mVisuals[aSide]);
			float[] tCoords = facingCoordsClicked(aSideClicked, aHitX, aHitY, aHitZ);
			if (tCoords[1] >= 0.0625F && tCoords[1] <= 0.25F) { // PX_P[1] .. PX_P[4] — the upper band
				if (tCoords[0] >= 0.0625F && tCoords[0] <= 0.25F) { // PX_P[1] .. PX_P[4] — the decrement zone
					tMode--;
					if (tMode < 0) tMode = 15;
					rReturn = true;
				} else if (tCoords[0] >= 0.75F && tCoords[0] <= 0.9375F) { // PX_N[4] .. PX_N[1] — the increment zone
					tMode++;
					if (tMode > 15) tMode = 0;
					rReturn = true;
				}
			} else if (tCoords[1] >= 0.5625F && tCoords[1] <= 0.75F) { // PX_N[7] .. PX_N[4] — the lower band
				if (tCoords[0] >= 0.125F && tCoords[0] <= 0.875F) { // PX_P[2] .. PX_N[2]
					if (tCoords[0] <= 0.3125F) { // PX_P[5]
						tMode ^= 8;
					} else if (tCoords[0] <= 0.5F) { // PX_P[8]
						tMode ^= 4;
					} else if (tCoords[0] <= 0.6875F) { // PX_N[5]
						tMode ^= 2;
					} else {
						tMode ^= 1;
					}
					rReturn = true;
				}
			}
			if (aData.mTileEntity.isServerSideTE()) aData.visual(aSide, tSwitchable.setStateMode(tMode));
			return rReturn;
		}
		return false;
	}

	/** Upstream :94 — the plate art follows the mirrored mode. */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aSide, CoverData aData) {
		return spriteOf((byte) aData.mVisuals[aSide]);
	}

	/** Upstream :97 — the mirrored mode survives the save (the CoverData writeToNBT :87 gate). */
	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {
		return true;
	}
}
