package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;

/**
 * The redstone conductor (emit) face — the wire-through cover, 1.20.1 port of
 * gregapi/cover/covers/CoverRedstoneConductorOUT.java (64 lines, task
 * p10-cover-conductor-redstone). The pair turns one machine into a redstone bridge:
 * the {@link CoverRedstoneConductorIN} marker faces read the world (plain-cover
 * pass-through), and THIS face re-emits the strongest marker reading — signal
 * travels through the machine body without vanilla wires touching it.
 *
 * <p>Emission (upstream :37-40 verbatim): {@link #getRedstoneOutWeak} = {@code
 * bind4(mValues[side])} — the value lane, RETURN, never merging the machine default.
 * Only the weak hook is overridden (:36-57 is the whole behavioural surface): the
 * strong exit passes the machine default through (AbstractCoverDefault :80), exactly
 * like upstream (:59-60 is already texture).
 *
 * <p>The value lane (upstream :48-57): every block update the face receives, the
 * cover scans ALL six faces for IN markers and caches
 * {@code max(getRedstoneIncoming(marker face))} bind4-clamped into
 * {@code mValues[aCoverSide]}. {@code onCoverPlaced} self-refreshes once (:42-46) so
 * the plate is live from the moment it is mounted.
 *
 * <p>Declared deviations (both recorded):
 * <ul>
 * <li>the upstream {@code GT_API_Proxy.DELAYED_BLOCK_UPDATES} batch queue (:55) is
 *     not ported — the new value lands through {@link CoverData#value(byte, short,
 *     boolean)} with {@code aBlockUpdate = true}, whose
 *     {@code sendBlockUpdateFromCover} fires the neighbour refresh immediately (the
 *     emitter's direct-write precedent, CoverRedstoneEmitter.java:107; CoverData's
 *     own {@code mValues != aValue} guard IS the upstream :53 guard);</li>
 * <li>the value write is server-gated with {@code isServerSideTE()} (the emitter
 *     :151 shape — the value lane is server-authoritative, the client reads it from
 *     the 03 sync channels); upstream's queue-add was implicitly server-only.</li>
 * </ul>
 *
 * <p>Attachment flag inlining follows the marker sibling (upstream
 * AbstractCoverAttachment :35-40, the emitter precedent). Texture (:59-63): the
 * upstream {@code "machines/covers/redstoneconductor/out"} sprite path-maps to
 * {@code gt6:block/redstone_conductor/out} (vanilla atlas directory source).
 */
public class CoverRedstoneConductorOUT extends AbstractCoverDefault {

	/** The sprite path — upstream CoverRedstoneConductorOUT.java:63, lowercased/underscored. */
	public static final String SPRITE_PATH = "block/redstone_conductor/out";

	/** The sprite id ({@code gt6:block/redstone_conductor/out}); static so the tables stay registry-free. */
	public static ResourceLocation sprite() {
		return new ResourceLocation("gt6", SPRITE_PATH);
	}

	/**
	 * Upstream UT.Code.bind4 — the 0..15 redstone scale clamp. (ICoverableTE.bind4 is
	 * package-private over in gregtech6.covers; the emitter inlines the same helper.)
	 */
	private static byte bind4(int aValue) {
		return (byte) Math.max(0, Math.min(15, aValue));
	}

	/** Upstream :37-40 verbatim — the cached value lane IS the emission, never the machine default. */
	@Override
	public byte getRedstoneOutWeak(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {
		return bind4(aData.mValues[aCoverSide]);
	}

	/** Upstream :42-46 — a fresh OUT cover refreshes itself once (the value lane starts at 0). */
	@Override
	public void onCoverPlaced(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer, ItemStack aCover) {
		super.onCoverPlaced(aCoverSide, aData, aPlayer, aCover);
		onBlockUpdate(aCoverSide, aData);
	}

	/**
	 * Upstream :48-57 — the scan. Every face carrying an IN marker contributes its
	 * {@link gregtech6.covers.ICoverableTE#getRedstoneIncoming} reading (the marker
	 * never overrides the read, so each answer is the plain world signal at that face);
	 * the maximum wins, bind4-clamped (:52). The scan itself is deliberately ungated —
	 * only the WRITE carries the server gate (the deviation note above).
	 */
	@Override
	public void onBlockUpdate(byte aCoverSide, CoverData aData) {
		byte tEmitted = 0; // :50
		for (byte tSide = 0; tSide < 6; tSide++) // :51 — upstream ALL_SIDES_VALID
			if (aData.mBehaviours[tSide] instanceof CoverRedstoneConductorIN)
				tEmitted = (byte) Math.max(tEmitted, aData.mTileEntity.getRedstoneIncoming(tSide));
		tEmitted = bind4(tEmitted); // :52
		if (aData.mTileEntity.isServerSideTE()) aData.value(aCoverSide, tEmitted, true); // :53-55 — the guard + queue → CoverData guard + direct dispatch
	}

	/** Upstream AbstractCoverAttachment :37 — a conductor click is never consumed. */
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

	/** Upstream :59 — the conductor art. */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return sprite();
	}
}
