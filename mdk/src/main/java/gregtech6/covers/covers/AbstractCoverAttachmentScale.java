package gregtech6.covers.covers;

import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;
import gregtech6.util.UT6;

/**
 * The scale base — 1.20.1 port of gregapi/cover/covers/AbstractCoverAttachmentScale.java
 * (:36-78, task p35-covers-display-scale-6). A scale cover turns a machine quantity into
 * a redstone OUTPUT: the VALUE lane carries the 0..15 reading
 * ({@code needsVisualsSaved} :69 — the visual lane carries the two output-mode bits), the
 * emission pair :60-67 maps the lane onto the host's redstone exits.
 *
 * <p>The output modes (the visual lane bits, upstream CS.B[0]/B[1]):
 * <ul>
 * <li>bit 0 (the cutter toggle :39-43) — STRONG emission (comparator-grade) instead of
 *     weak;</li>
 * <li>bit 1 (the screwdriver toggle :44-48) — the scale INVERTS ({@code 15 - value}),
 *     "Redstone scales down" instead of up.</li>
 * </ul>
 *
 * <p>Declared deviations: the magnifyingglass read arm :49-55 keeps its upstream damage
 * (1) but has no chat channel on the ported ICover signature (the P10 controller
 * precedent); the {@code addToolTips} arm :71-77 is cut (no tooltip surface on the
 * ported ICover); the :56 host relay answers 0 (the attachment-base ruling).
 */
public abstract class AbstractCoverAttachmentScale extends AbstractCoverAttachment {

	/** Upstream :39-43 — the cutter toggles the strong-emission bit (visual bit 0). */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (CoverRedstoneEmitter.TOOL_CUTTER.equals(aToolId)) {
			aData.visual(aCoverSide, (short) (aData.mVisuals[aCoverSide] ^ 1), true); // B[0] == 1
			return 1000;
		}
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
			aData.visual(aCoverSide, (short) (aData.mVisuals[aCoverSide] ^ 2), true); // B[1] == 2
			return 1000;
		}
		if (TOOL_MAGNIFYINGGLASS.equals(aToolId)) {
			return 1; // :49-55 — the read arm; the chat lines have no ported channel
		}
		return 0; // the :56 host relay is not ported
	}

	/** Upstream :60-62 — strong emission only when the cutter bit is set, else nothing. */
	@Override
	public byte getRedstoneOutStrong(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {
		return (aData.mVisuals[aCoverSide] & 1) != 0 ? getRedstoneOutWeak(aCoverSide, aData, aDefaultRedstone) : 0;
	}

	/** Upstream :65-67 — the value lane on the weak exit, inverted when the screwdriver bit is set. */
	@Override
	public byte getRedstoneOutWeak(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {
		return (aData.mVisuals[aCoverSide] & 2) != 0 ? UT6.bind4(15 - aData.mValues[aCoverSide]) : UT6.bind4(aData.mValues[aCoverSide]);
	}

	/** Upstream :69 — the mode bits ride the visual lane, they save. */
	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {
		return true;
	}

	/** Upstream CS.TOOL_magnifyingglass (CS.java:1066) — the read-only inspector id. In-class on purpose: the ICover tool-id constants are a frozen surface (the CoverRedstoneEmitter ruling). */
	public static final String TOOL_MAGNIFYINGGLASS = "magnifyingglass";
}
