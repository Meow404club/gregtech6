package gregtech6.tileentity;

/**
 * The paintable TileEntity face — the 1.20.1 counterpart of the upstream 1.7.10
 * {@code TileEntityBase07Paintable} paint API (gregapi/tileentity/base/
 * TileEntityBase07Paintable.java:83-86) plus the recolour routing of
 * TileEntityBase04MultiTileEntities.java:227-235.
 *
 * <p>Implemented by {@link TileEntityBase03TicksAndSync} — this repo's folded
 * 01-07 base chain keeps the upstream layering shape (07Paintable is the family-wide
 * inheritance stratum machines ride on), so every machine BE carries the face and the
 * future spray-can item (pool) can {@code instanceof IPaintableTE} route.
 *
 * <p>Storage semantics (ADR 2026-09-07-p21-paintable-rulings ruling 3): the paint colour
 * is stored directly as the final 0xRRGGBB int — the upstream route sprayed the
 * complemented dye index through {@code ~mColor&15} + the DYES_INT_INVERTED table, whose
 * composition is provably identical to {@code DYES_INT[mColor]} ("the colour you spray is
 * the colour you get"), so the port skips the complement detour (declared equivalence
 * simplification). Repainting an already-painted machine MIXES by channel average
 * ({@code UT.Code.mixRGBInt}, UT.java:1576-1578) — the GT6 semantics; the GTCEu
 * ColorSprayBehaviour overwrite-without-mixing deviation is not adopted.
 */
public interface IPaintableTE {

	/**
	 * Upstream Paintable:85 verbatim — stores the colour directly and marks the machine
	 * painted; a same-colour spray is the no-op.
	 *
	 * @return {@code true} when the paint actually changed (the caller-facing sync trigger)
	 */
	boolean paint(int aRGB);

	/**
	 * The recolour routing of upstream TileEntityBase04MultiTileEntities.java:227-235 with
	 * the dye-index complement folded away (ruling 3): an unpainted machine takes the
	 * colour as-is, a painted one takes the channel average mix
	 * ({@code (ch1+ch2)/2} per channel).
	 */
	boolean mixPaint(int aRGB);

	/**
	 * Upstream Paintable:83 shape — clears the painted flag and returns the colour to
	 * UNCOLORED white. Declared deviation: upstream restores the machine's material colour
	 * ({@code mMaterial.fRGBaSolid}); this port has no material reference on the machines
	 * (the trimmed field set), so unpaint returns white, which renders as "no tint".
	 */
	boolean unpaint();

	/** Upstream Paintable:84 server half — the client material-colour inference is cut with the material field. */
	boolean isPainted();

	/** Upstream Paintable:86 — the current paint colour (UNCOLORED white when unpainted). */
	int getPaint();
}
