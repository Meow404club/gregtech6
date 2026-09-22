package gregtech6.covers.covers;

import gregtech6.covers.ICover;
import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;
import gregtech6.tileentity.machines.ITileEntitySwitchableMode;
import gregtech6.util.UT6;

/**
 * The button panel selector cover — 1.20.1 port of
 * gregapi/cover/covers/CoverSelectorButtonPanel.java:40-137 (task
 * p34-covers-gameplay-10; upstream item id 1027 "Button Panel Selector"). The plate is
 * a 4x4 button grid: a right-click hit maps the clicked cell straight onto the mode
 * ({@code column + row*4}, the :62 formula) and drives the host dial (:63). The value
 * lane runs the momentary window: with the mode armed (screwdriver toggle :76-80) a
 * click starts a 10-tick countdown (:64, :87-89) that resets the dial to 0 when it
 * lands, the doorbell behaviour; unarmed clicks latch (:66 always consumed).
 *
 * <p>The mode mirror: placement (:44-47), block updates (:53-56) and every server tick
 * (:91) fold the host dial into the LOW four visual bits ({@code (visual & ~15) |
 * bind4(mode)}) — the upper bits stay the chisel selector's own state.
 *
 * <p>DECLARED CUT — the chisel underlay cycle and the 8-underlay texture stack: the
 * chisel arm (:72-75) cycles visual bits 4-6 to pick one of eight {@code sTexturesBase}
 * underlays; the port's plate renderer is single-sprite (the CoverSelectorTag class-doc
 * cut), so there is no underlay layer to select — the chisel arm has nothing to drive
 * and is cut with the stack (the 16 shipped sprites are the pre-composited plates). The
 * {@code isOpaque=true} flag (:107) is KEPT (the port's own flag surface).
 */
public class CoverSelectorButtonPanel extends AbstractCoverAttachmentSelector {

	/** The momentary window length — upstream :64 {@code aData.value(aSide, (short)10)}. */
	public static final short MOMENTARY_TICKS = 10;

	/** The atlas sprite of the mode plates (upstream sTextures, :109-126). */
	//? if forge {
	public static ResourceLocation spriteOf(byte aMode) {
		return new ResourceLocation("gt6", "block/buttonselector/" + UT6.bind4(aMode));
	}
	//?} else {
	/*public static ResourceLocation spriteOf(byte aMode) {
		return ResourceLocation.fromNamespaceAndPath("gt6", "block/buttonselector/" + UT6.bind4(aMode));
	}
	 *///?}

	/** Upstream :44-47 — placement folds the host dial into the low visual bits. */
	@Override
	public void onCoverPlaced(byte aSide, CoverData aData, @Nullable Entity aPlayer, net.minecraft.world.item.ItemStack aCover) {
		super.onCoverPlaced(aSide, aData, aPlayer, aCover);
		if (aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable)
			aData.visual(aSide, (short) ((aData.mVisuals[aSide] & ~15) | UT6.bind4(tSwitchable.getStateMode())));
	}

	/** Upstream :49-52 — a chunk load writes the SAVED low bits back into the dial. */
	@Override
	public void onCoverLoaded(byte aSide, CoverData aData) {
		super.onCoverLoaded(aSide, aData);
		if (aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable)
			tSwitchable.setStateMode((byte) UT6.bind4(aData.mVisuals[aSide] & 15));
	}

	/** Upstream :54-57 — a block update folds the host dial in (blocked hosts do not mirror). */
	@Override
	public void onBlockUpdate(byte aSide, CoverData aData) {
		if (!aData.mStopped && aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable)
			aData.visual(aSide, (short) ((aData.mVisuals[aSide] & ~15) | UT6.bind4(tSwitchable.getStateMode())));
	}

	/**
	 * Upstream :59-68 verbatim — the 4x4 grid. The clicked cell maps
	 * {@code ((int)(x*4) % 4) + ((int)(y*4) % 4) * 4} onto the mode; a server-side hit
	 * drives the dial, folds the RETURN into the low visual bits (:63), arms the
	 * momentary window when the value lane is armed (:64) and always consumes the click
	 * (:66).
	 */
	@Override
	public boolean onCoverClickedRight(byte aSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (!aData.mStopped && aSide == aSideClicked && aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable) {
			float[] tCoords = facingCoordsClicked(aSideClicked, aHitX, aHitY, aHitZ);
			byte tMode = (byte) UT6.bind4(((int) (tCoords[0] * 4) % 4) + ((int) (tCoords[1] * 4) % 4) * 4);
			if (aData.mTileEntity.isServerSideTE())
				aData.visual(aSide, (short) ((aData.mVisuals[aSide] & ~15) | UT6.bind4(tSwitchable.setStateMode(tMode))));
			if (aData.mValues[aSide] > 0) aData.value(aSide, MOMENTARY_TICKS);
			return true;
		}
		return false;
	}

	/**
	 * Upstream :76-80 — the screwdriver toggles the momentary window arm (the value lane
	 * 0 ↔ 1, damage 10000). The :78 chat line has no channel on the ported ICover
	 * signature (the CoverShutter declared-deviation precedent).
	 */
	@Override
	public long onToolClick(byte aSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
			aData.value(aSide, (short) (aData.mValues[aSide] > 0 ? 0 : 1));
			return 10000;
		}
		return 0;
	}

	/**
	 * Upstream :85-93 verbatim — the momentary countdown: an armed window above 1 ticks
	 * down, landing on 1 fires the dial reset (:89), and every tick folds the host dial
	 * into the low visual bits (:91).
	 */
	@Override
	public void onTickPost(byte aSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (!aData.mStopped && aIsServerSide && aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable) {
			if (aData.mValues[aSide] > 1) {
				aData.value(aSide, (short) (aData.mValues[aSide] - 1));
				if (aData.mValues[aSide] == 1) tSwitchable.setStateMode((byte) 0);
			}
			aData.visual(aSide, (short) ((aData.mVisuals[aSide] & ~15) | UT6.bind4(tSwitchable.getStateMode())));
		}
	}

	/** Upstream :103 — the plate art follows the folded mode (the underlay stack is pre-composited, the class-doc cut). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aSide, CoverData aData) {
		return spriteOf((byte) aData.mVisuals[aSide]);
	}

	/** Upstream :106 — the folded mode (and the momentary value lane, the CoverData :86 unconditional write) survive the save. */
	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {
		return true;
	}

	/** Upstream :107 — the button panel is a full opaque plate. */
	@Override
	public boolean isOpaque(byte aCoverSide, CoverData aData) {
		return true;
	}
}
