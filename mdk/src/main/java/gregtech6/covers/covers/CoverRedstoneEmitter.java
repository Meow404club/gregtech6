package gregtech6.covers.covers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;

/**
 * The redstone emitter cover — 1.20.1 port of gregapi/cover/covers/CoverRedstoneEmitter.java
 * (136 lines, task p9-redstone-cover-emitter, ADR 2026-09-01-p9-redstone-cover-emitter).
 * The first REAL redstone cover: the visual lane holds the emitted tier (0..15, weak
 * signal), the value lane holds only the strong gate (bit 0), and bare-hand right
 * clicks tune the tier through the 16-zone keypad on the plate (:72-109).
 *
 * <p>Emission (upstream :55-62 verbatim — the overrides RETURN, they never merge the
 * machine default):
 * <ul>
 * <li>{@link #getRedstoneOutWeak} = {@code bind4(mVisuals[side])} — the tier lane;</li>
 * <li>{@link #getRedstoneOutStrong} = {@code mValues[side] != 0 ? weak : 0} — the
 *     cutter-toggled strong gate re-reads the weak hook.</li>
 * </ul>
 *
 * <p>Attachment flag inlining (upstream AbstractCoverAttachment.java:35-40; the base
 * class is NOT ported, single-consumer YAGNI — ADR alternatives ②): the click
 * intercepts stay {@code false} (a click that misses a keypad zone falls through to
 * the host's own action), and the plate is non-opaque/non-sealable like every
 * attachment cover. The incoming read is NOT overridden (upstream has no
 * {@code getRedstoneIn} on this class nor on the attachment chain — the plain-cover
 * world pass-through of AbstractCoverDefault :78 is the whole incoming story).
 *
 * <p>The {@code magnifyingglass} read hook (:47-50) and the {@code onToolClick2}
 * host-relay arm (:51) are declared deviations: no chat channel exists on the ported
 * ICover.onToolClick signature and the host has no onToolClick2 face — non-cutter
 * tool clicks return 0 (the ADR deviations list).
 *
 * <p>Textures (:111/:116-135): upstream stacks {@code BlockTextureMulti(underlay,
 * tier)}; the single-sprite plate renderer carries the offline source-over
 * composition instead (assets/README.md, the borrow/composition record). The sprite
 * ids are {@code gt6:block/redstone_emitter/<tier>} — the vanilla block atlas
 * directory source auto-stitches them (zero GT6Atlases wiring).
 */
public class CoverRedstoneEmitter extends AbstractCoverDefault {

	/** Upstream CS.TOOL_cutter (CS.java:1064) — the strong-gate toggle. In-class on purpose: the ICover tool-id constants are a frozen surface (ADR alternatives ③). */
	public static final String TOOL_CUTTER = "cutter";

	/** The sprite family root — tier sprites {@code gt6:block/redstone_emitter/<0..15>}. */
	public static final String SPRITE_PATH = "block/redstone_emitter/";

	/** Upstream PX_P (CS.java:492-503) — pixel n at n/16. */
	public static final float[] PX_P = {0.0000F, 0.0625F, 0.1250F, 0.1875F, 0.2500F, 0.3125F, 0.3750F, 0.4375F, 0.5000F, 0.5625F, 0.6250F, 0.6875F, 0.7500F, 0.8125F, 0.8750F, 0.9375F, 1.0000F};

	/** Upstream PX_N (CS.java:504-514) — pixel n at 1 - n/16. */
	public static final float[] PX_N = {1.0000F, 0.9375F, 0.8750F, 0.8125F, 0.7500F, 0.6875F, 0.6250F, 0.5625F, 0.5000F, 0.4375F, 0.3750F, 0.3125F, 0.2500F, 0.1875F, 0.1250F, 0.0625F, 0.0000F};

	/**
	 * Upstream UT.Code.bind4 — the 0..15 redstone scale clamp. (ICoverableTE.bind4 is
	 * package-private over in gregtech6.covers; the emitter is a different package.)
	 */
	public static byte bind4(int aValue) {
		return (byte) Math.max(0, Math.min(15, aValue));
	}

	/**
	 * Upstream UT.Code.getFacingCoordsClicked (UT.java:1734-1744 verbatim) — the
	 * texture-sheet [u, v] of the clicked point, top-left origin, clamped to 0..0.99.
	 */
	public static float[] facingCoordsClicked(byte aSide, float aHitX, float aHitY, float aHitZ) {
		return switch (aSide) {
			case 0  -> new float[] {Math.min(0.99F, Math.max(0, aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitZ))};
			case 1  -> new float[] {Math.min(0.99F, Math.max(0, aHitX)), Math.min(0.99F, Math.max(0, aHitZ))};
			case 2  -> new float[] {Math.min(0.99F, Math.max(0, 1 - aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			case 3  -> new float[] {Math.min(0.99F, Math.max(0, aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			case 4  -> new float[] {Math.min(0.99F, Math.max(0, aHitZ)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			case 5  -> new float[] {Math.min(0.99F, Math.max(0, 1 - aHitZ)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			default -> new float[] {0.5F, 0.5F};
		};
	}

	/** The tier sprite id ({@code gt6:block/redstone_emitter/<aTier>}); pure for the offline tables. */
	public static ResourceLocation spriteForTier(int aTier) {
		return new ResourceLocation("gt6", SPRITE_PATH + aTier);
	}

	// ---------------------------------------------------------------------------
	// the emission pair (:55-62 verbatim — RETURN, never merge the machine default)
	// ---------------------------------------------------------------------------

	@Override
	public byte getRedstoneOutStrong(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {
		return aData.mValues[aCoverSide] != 0 ? getRedstoneOutWeak(aCoverSide, aData, aDefaultRedstone) : 0; // :55-57
	}

	@Override
	public byte getRedstoneOutWeak(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {
		return bind4(aData.mVisuals[aCoverSide]); // :60-62 — the tier lane 0..15
	}

	// ---------------------------------------------------------------------------
	// the cutter strong-gate toggle (:42-46; magnifyingglass :47-50 and the :51
	// host-relay arm are declared deviations — see the class doc)
	// ---------------------------------------------------------------------------

	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (TOOL_CUTTER.equals(aToolId)) {
			aData.value(aCoverSide, (short) (aData.mValues[aCoverSide] ^ 1), true); // :43 — B[0] == 1, block update on
			return 1000; // :45 (the :44 chat line has no channel on the ported ICover signature)
		}
		return 0; // the :51 onToolClick2 host-relay arm is not ported (no host face)
	}

	// ---------------------------------------------------------------------------
	// the bare-hand 16-zone keypad (:72-109 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean onCoverClickedRight(byte aSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (aSide == aSideClicked) {
			boolean rReturn = false;
			byte tMode = bind4(aData.mVisuals[aSide]); // :75
			float[] tCoords = facingCoordsClicked(aSideClicked, aHitX, aHitY, aHitZ); // :76
			if (tCoords[1] >= PX_P[1] && tCoords[1] <= PX_P[4]) { // :77 — the upper row
				if (tCoords[0] >= PX_P[1] && tCoords[0] <= PX_P[4]) { // :78 — upper-left: -1 with 0→15 wrap
					tMode--;
					if (tMode < 0) tMode = 15;
					rReturn = true;
				} else //
				if (tCoords[0] >= PX_N[4] && tCoords[0] <= PX_N[1]) { // :83 — upper-right: +1 with 15→0 wrap
					tMode++;
					if (tMode > 15) tMode = 0;
					rReturn = true;
				}
			} else //
			if (tCoords[1] >= PX_N[7] && tCoords[1] <= PX_N[4]) { // :89 — the lower row
				if (tCoords[0] >= PX_P[2] && tCoords[0] <= PX_N[2]) { // :90 — the four bit cells
					if (tCoords[0] <= PX_P[5]) {
						tMode ^= 8;
					} else //
					if (tCoords[0] <= PX_P[8]) {
						tMode ^= 4;
					} else //
					if (tCoords[0] <= PX_N[5]) {
						tMode ^= 2;
					} else {
						tMode ^= 1;
					}
					rReturn = true;
				}
			}
			if (aData.mTileEntity.isServerSideTE()) aData.visual(aSide, tMode, true); // :105 — aBlockUpdate = T
			return rReturn;
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// the attachment flag inlining (AbstractCoverAttachment :35-40) + visuals + texture
	// ---------------------------------------------------------------------------

	@Override public boolean interceptClickLeft(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {return false;} // :37
	@Override public boolean interceptClickRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {return false;} // :38
	@Override public boolean isOpaque(byte aCoverSide, CoverData aData) {return false;} // :39
	@Override public boolean isSealable(byte aCoverSide, CoverData aData) {return false;} // :40

	/** Upstream :114 — the tier survives the save (the CoverData writeToNBT :87 gate). */
	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {return true;}

	/**
	 * Upstream :111 — the tier art. The upstream {@code BlockTextureMulti(underlay,
	 * tier)} two-pass stack lands as the offline-composed single sprite
	 * (assets/README.md); the attachment/holder faces (:112-113) fold into the same
	 * sprite like every cover on the single-sprite plate.
	 */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return spriteForTier(bind4(aData.mVisuals[aCoverSide]));
	}
}
