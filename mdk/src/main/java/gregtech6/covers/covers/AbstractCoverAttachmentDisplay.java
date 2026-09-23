package gregtech6.covers.covers;

import gregtech6.covers.CoverData;

/**
 * The display base — 1.20.1 port of gregapi/cover/covers/AbstractCoverAttachmentDisplay.java
 * (:29-31, task p35-covers-display-scale-6). A display cover paints its state into the
 * VISUAL lane every tick, so the lane persists with the cover
 * ({@code needsVisualsSaved} :30 — the CoverData writeToNBT :75 gate).
 */
public abstract class AbstractCoverAttachmentDisplay extends AbstractCoverAttachment {

	/** Upstream :30 — the visual lane IS the display state, it saves. */
	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {
		return true;
	}
}
