package gregtech6.covers.covers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;

/**
 * The redstone conductor (accept) marker — 1.20.1 port of
 * gregapi/cover/covers/CoverRedstoneConductorIN.java (36 lines, task
 * p10-cover-conductor-redstone). Upstream :30-36 is ZERO behaviour: the class exists
 * so the sibling {@link CoverRedstoneConductorOUT#onBlockUpdate} scan can
 * {@code instanceof}-test the face (the machine's world signal at THIS face is the
 * value the wire-through face re-emits on its own), plus the plate art.
 *
 * <p>The incoming read is NOT overridden — the marker stays transparent to redstone
 * (AbstractCoverDefault :78), which is exactly what makes the pair work: the OUT face
 * scans the marker faces through {@code ICoverableTE.getRedstoneIncoming}, and each
 * marker answers with the plain world read.
 *
 * <p>Attachment flag inlining (upstream AbstractCoverAttachment.java:35-40; the base
 * class is NOT ported — the emitter precedent, CoverRedstoneEmitter class doc): the
 * click intercepts stay {@code false} (a click on the marker falls through to the
 * host's own action) and the plate is non-opaque/non-sealable like every attachment
 * cover. The upstream {@code onToolClick2} host-relay arm (:42) is a declared
 * deviation — non-cutter tool clicks return 0 (no host face, the emitter note).
 *
 * <p>Texture (:31-33/:35): upstream stacks {@code BACKGROUND_COVER} under
 * {@code sTexture} for the attachment faces; the single-sprite plate renderer folds
 * attachment/holder into the same sprite like every cover (AbstractCoverDefault
 * :71-72 defaults), so only the surface sprite is pinned. Upstream
 * {@code "machines/covers/redstoneconductor/in"} is path-mapped to
 * {@code gt6:block/redstone_conductor/in} — the vanilla block atlas directory source
 * auto-stitches it (zero GT6Atlases wiring).
 */
public class CoverRedstoneConductorIN extends AbstractCoverDefault {

	/** The sprite path — upstream CoverRedstoneConductorIN.java:35, lowercased/underscored. */
	public static final String SPRITE_PATH = "block/redstone_conductor/in";

	/** The sprite id ({@code gt6:block/redstone_conductor/in}); static so the tables stay registry-free. */
	public static ResourceLocation sprite() {
		return new ResourceLocation("gt6", SPRITE_PATH);
	}

	/** Upstream AbstractCoverAttachment :37 — a marker click is never consumed. */
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

	/** Upstream :31 — the marker art. */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return sprite();
	}
}
