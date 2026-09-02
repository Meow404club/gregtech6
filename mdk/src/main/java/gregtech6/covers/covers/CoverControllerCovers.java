package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;
import gregtech6.util.UT6;

/**
 * The cover controller cover — 1.20.1 port of gregapi/cover/covers/
 * CoverControllerCovers.java (109 lines, task p11-cover-controllers, the
 * p11-research-cover-remainder card q3). Turns redstone into an ON/OFF state
 * for the OTHER covers on the block: its three arms drive
 * {@link CoverData#setStopped} — the block-wide cover stop flag — NOT the
 * machine's {@code setStateOnOff} (that boundary is what separates it from the
 * {@link CoverControllerRedstone} family; it extends the plain attachment base
 * {@link AbstractCoverDefault} and has NO switchable-host placement gate).
 *
 * <p>The arms (upstream :40-55):
 * <ul>
 * <li>{@link #onCoverRemove} :41-44 and {@link #onCoverPlaced} :47-50 — the
 *     stop flag re-derives from the face on both mount edges;</li>
 * <li>{@link #onTickPre} :53-55 — the server-side poll (the signal can change
 *     without any block update, the P10 controller's arm-5 shape, here with no
 *     host interface guard).</li>
 * </ul>
 *
 * <p>The cross-face relay (:58-91): a click or tool click on THIS cover's face
 * resolves the nine-grid hit region through {@link UT6#getSideWrenching} (the
 * verbatim {@code UT.Code.getSideWrenching} port) and forwards the interaction
 * to whatever cover sits on THAT face — the "operate the other covers through
 * this one" feature. The self-face/centre case runs the cover's own screwdriver
 * polarity toggle (:77-82), which re-derives the stop flag in the same breath.
 *
 * <p>The polarity formula (:106-108) is the EQUALITY —
 * {@code bind1(getRedstoneIncoming(side)) == (mValues & B[0])} — the exact
 * inverse of the machine-controller family's inequality: bit 0 clear (the
 * fresh mount) holds the covers RUNNING only while the face sees NO signal.
 *
 * <p>Declared deviations (the P10 controller's, carried over): the
 * {@code magnifyingglass} read arm (:83-86) is cut — no chat-return channel on
 * the ported ICover.onToolClick signature (the emitter precedent).
 *
 * <p>Attachment flag inlining (upstream AbstractCoverAttachment :35-40) and the
 * texture (:100-104): upstream {@code machines/covers/coverswitch/circuit}
 * path-maps to {@code gt6:block/cover_switch/circuit}; the attachment/holder
 * faces (:101-102 with the {@code coverswitch/base} background) fold into the
 * single-sprite plate like every cover (AbstractCoverDefault :71-72 defaults,
 * the BACKGROUND_COVER layer is not borrowed — the P10 controller precedent).
 */
public class CoverControllerCovers extends AbstractCoverDefault {

	/** The sprite path — upstream CoverControllerCovers.java:104 foreground, lowercased/underscored. */
	public static final String SPRITE_PATH = "block/cover_switch/circuit";

	/** The sprite id ({@code gt6:block/cover_switch/circuit}); static so the tables stay registry-free. */
	public static ResourceLocation sprite() {
		return new ResourceLocation("gt6", SPRITE_PATH);
	}

	/** Upstream UT.Code.bind1 (UT.java:1553) — the 0/1 logic-scale clamp (the P10 controller's private copy, inlined again per the ruling). */
	private static int bind1(int aValue) {
		return Math.max(0, Math.min(1, aValue));
	}

	// ---------------------------------------------------------------------------
	// arm 1 — the removal re-derive (:41-44 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public void onCoverRemove(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		super.onCoverRemove(aCoverSide, aData, aPlayer);
		aData.setStopped(getStateOnOff(aCoverSide, aData));
	}

	// ---------------------------------------------------------------------------
	// arm 2 — the mount re-derive (:47-50 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public void onCoverPlaced(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer, ItemStack aCover) {
		super.onCoverPlaced(aCoverSide, aData, aPlayer, aCover);
		aData.setStopped(getStateOnOff(aCoverSide, aData));
	}

	// ---------------------------------------------------------------------------
	// arm 3 — the server tick poll (:53-55 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (aIsServerSide) aData.setStopped(getStateOnOff(aCoverSide, aData));
	}

	// ---------------------------------------------------------------------------
	// the right-click relay (:58-63 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean onCoverClickedRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		byte tSide = UT6.getSideWrenching(aSideClicked, aHitX, aHitY, aHitZ);
		if (tSide == aCoverSide || aSideClicked != aCoverSide) return false;
		if (aData.mBehaviours[tSide] != null) return aData.mBehaviours[tSide].onCoverClickedRight(tSide, aData, aPlayer, aSideClicked, aHitX, aHitY, aHitZ);
		return false;
	}

	// ---------------------------------------------------------------------------
	// the left-click relay (:66-71 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean onCoverClickedLeft(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		byte tSide = UT6.getSideWrenching(aSideClicked, aHitX, aHitY, aHitZ);
		if (tSide == aCoverSide || aSideClicked != aCoverSide) return false;
		if (aData.mBehaviours[tSide] != null) return aData.mBehaviours[tSide].onCoverClickedLeft(tSide, aData, aPlayer, aSideClicked, aHitX, aHitY, aHitZ);
		return false;
	}

	// ---------------------------------------------------------------------------
	// the tool-click relay + the self-face screwdriver toggle (:74-91)
	// ---------------------------------------------------------------------------

	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		byte tSide = UT6.getSideWrenching(aSideClicked, aHitX, aHitY, aHitZ);
		if (tSide == aCoverSide || aSideClicked != aCoverSide) {
			if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
				aData.value(aCoverSide, (short) (aData.mValues[aCoverSide] ^ 1)); // :78 — B[0] == 1
				// :79 — the chat line has no channel on the ported ICover signature
				aData.setStopped(getStateOnOff(aCoverSide, aData)); // :80 — the toggle re-derives the stop flag in the same breath
				return 1000; // :81
			}
			// :83-86 — the magnifyingglass arm is cut (no chat-return channel)
			return 0; // :87
		}
		if (aData.mBehaviours[tSide] != null) return aData.mBehaviours[tSide].onToolClick(tSide, aData, aToolId, aRemainingDurability, aPlayer, aSneaking, aSideClicked, aHitX, aHitY, aHitZ); // :89
		return 0; // :90
	}

	// ---------------------------------------------------------------------------
	// the polarity formula (:106-108 verbatim — the EQUALITY)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :106-108 verbatim: {@code bind1(getRedstoneIncoming(side)) ==
	 * (mValues[side] & B[0])}. Bit 0 clear (the fresh mount) = the covers on this
	 * block RUN while the face sees NO signal; set (the screwdriver toggle) = they
	 * run while it DOES — the exact inverse of the machine-controller inequality
	 * (CoverControllerRedstone :68-70), pinned as a cross-claim in both tests.
	 */
	public boolean getStateOnOff(byte aCoverSide, CoverData aData) {
		return bind1(aData.mTileEntity.getRedstoneIncoming(aCoverSide)) == (aData.mValues[aCoverSide] & 1);
	}

	// ---------------------------------------------------------------------------
	// the inlined attachment flags (AbstractCoverAttachment :35-40) + texture
	// ---------------------------------------------------------------------------

	/** Upstream AbstractCoverAttachment :37 — a controller click is never consumed. */
	@Override
	public boolean interceptClickLeft(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		return false;
	}

	/** Upstream AbstractCoverAttachment :38 — same shape as the left click. */
	@Override
	public boolean interceptClickRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		return false;
	}

	/** Upstream AbstractCoverAttachment :39 — the plate never blocks the host's light. */
	@Override
	public boolean isOpaque(byte aCoverSide, CoverData aData) {
		return false;
	}

	/** Upstream AbstractCoverAttachment :40 — the plate never seals the host. */
	@Override
	public boolean isSealable(byte aCoverSide, CoverData aData) {
		return false;
	}

	/** Upstream :100/:104 — the switch art (the coverswitch/base faces fold in, :101-102). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return sprite();
	}
}
