package gregtech6.tileentity.foam;

import java.util.UUID;

import javax.annotation.Nullable;

/**
 * The TE-side C-Foam face — the 1.20.1 counterpart of upstream
 * {@code gregapi/tileentity/ITileEntityFoamable.java:27-45} (six methods verbatim), with
 * the recorded folds: {@code Entity aPlayer} → {@code UUID} (the offline-test discipline —
 * no Player is ever constructed; the GTFluidPipeBlockEntity foam-face precedent) and the
 * {@code short[] aCFoamRGB + byte aVanillaColor} pair → one {@code int aRGB} (the
 * DYES_INT table value, the pipe applyFoam fold).
 *
 * <p>The upstream family has exactly TWO implementers (research.p26-r-foam-family): the
 * rendered pipe connector (upstream TileEntityBase10ConnectorRendered:55 — the port's
 * {@code GTFluidPipeBlockEntity} carries the SAME six method faces directly, frozen by the
 * P25 no-rewrite boundary and therefore NOT a Java implementer of this interface) and the
 * owned C-Foam TE ({@code MultiTileEntityCFoam.java:52} — this port's
 * {@link GT6CFoamBlockEntity}, the interface's implementer). The Remover card (card C)
 * routes its TE arm over BOTH: {@code instanceof ITileEntityFoamable} (the owned foam)
 * plus the pipe's concrete type.
 */
public interface ITileEntityFoamable {

	/** @return if it got applied successfully (upstream :29). */
	boolean applyFoam(byte aSide, @Nullable UUID aPlayer, int aRGB, boolean aOwnedFoam);

	/** @return if it got dried successfully (upstream :32). */
	boolean dryFoam(byte aSide, @Nullable UUID aPlayer);

	/** @return if it got removed successfully (upstream :35). */
	boolean removeFoam(byte aSide, @Nullable UUID aPlayer);

	/** @return if it is foamed (upstream :38). */
	boolean hasFoam(byte aSide);

	/** @return if it is dried (upstream :41). */
	boolean driedFoam(byte aSide);

	/** @return if it is owned (upstream :44). */
	boolean ownedFoam(byte aSide);
}
