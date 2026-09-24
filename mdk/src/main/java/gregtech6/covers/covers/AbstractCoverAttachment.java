package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;

/**
 * The attachment base — 1.20.1 port of gregapi/cover/covers/AbstractCoverAttachment.java
 * (:34-43, task p35-covers-display-scale-6). An ATTACHMENT plate interacts less than a
 * plain cover: clicks never consume (all four click hooks fall through — upstream the
 * interceptClick pair flips from the AbstractCoverDefault {@code true} to {@code false},
 * :37-38), the plate never blocks the host's light (:39) and never seals the host (:40).
 *
 * <p>Two declared deviations (the P11 controller-card rulings, now hoisted here because
 * this card finally ports the attachment family the P11 card inlined):
 * <ul>
 * <li>the {@code onWalkOver} arm (:41) does not answer — the hook exists since the asphalt
 *     cover restoration (task p37-covers-crafting-asphalt), the attachment default passes
 *     the AbstractCoverDefault consume-true through and the arm stays unimplemented;</li>
 * <li>the {@code onToolClick} host relay (:42, {@code aTileEntity.onToolClick2(...)})
 *     has no ported host face — non-answered tool ids return 0 (the CoverControllerAuto
 *     Redstone precedent).</li>
 * </ul>
 *
 * <p>The P11 card inlined this class' arms per-cover under the "second inline" research
 * ruling (only one consumer existed); with the display/scale family this card mounts
 * FOUR cover families on it, so the base lands as the shared superclass and
 * {@link CoverControllerAutoRedstone}/{@link CoverControllerRedstone} keep their
 * verified inlined copies (touching landed cards is not this card's business).
 */
public abstract class AbstractCoverAttachment extends AbstractCoverDefault {

	/** Upstream :35 — the left click is never consumed. */
	@Override
	public boolean onCoverClickedLeft(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		return false;
	}

	/** Upstream :36 — the right click is never consumed (the host GUI still opens). */
	@Override
	public boolean onCoverClickedRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		return false;
	}

	/** Upstream :37 — the left click falls through to the host. */
	@Override
	public boolean interceptClickLeft(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		return false;
	}

	/** Upstream :38 — the right click falls through to the host GUI. */
	@Override
	public boolean interceptClickRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		return false;
	}

	/** Upstream :39 — the plate never blocks the host's light. */
	@Override
	public boolean isOpaque(byte aCoverSide, CoverData aData) {
		return false;
	}

	/** Upstream :40 — the plate never seals the host. */
	@Override
	public boolean isSealable(byte aCoverSide, CoverData aData) {
		return false;
	}

	/**
	 * Upstream :42 — the tool relay. The upstream body relays to the host's
	 * {@code onToolClick2}; the ported composition model has no host tool face, so the
	 * base answers 0 and the consuming covers override the ids they know.
	 */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, @Nullable Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		return 0;
	}
}
