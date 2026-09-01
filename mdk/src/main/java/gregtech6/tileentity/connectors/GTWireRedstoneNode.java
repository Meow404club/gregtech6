package gregtech6.tileentity.connectors;

import javax.annotation.Nullable;

import gregapi.code.HashSetNoNulls;

/**
 * The BFS node face of the GT6 redstone-wire family (task p10-wire-redstone-family) — the
 * 1.20.1 counterpart of gregapi/tileentity/connectors/ITileEntityRedstoneWire.java, kept as
 * its own top-level type for the same reason upstream made it one: the push-BFS engine
 * ({@link #doRedstoneUpdate}, the Util.doRedstoneUpdate :43-56 verbatim port) walks this
 * interface, NOT the concrete BE, so offline truth tables (GTWireRedstoneLogicTest) can
 * drive it over fake nodes with no Level at all.
 *
 * <p>The two channel parameters ({@code aRedstoneID}) survive verbatim even though this
 * port mounts exactly one channel ({@link GTWireBlockEntity#REDSTONE_ID}) — bundled
 * redstone is a pool item and these signatures are the upstream shape (:34-40).
 */
public interface GTWireRedstoneNode {

	/** Upstream :34 — does this wire push a changed value out of aSide on this channel. */
	boolean canEmitRedstoneToWire(byte aSide, int aRedstoneID);

	/** Upstream :35 — does this wire accept a pushed value into aSide on this channel. */
	boolean canAcceptRedstoneFromWire(byte aSide, int aRedstoneID);

	/** Upstream :37 — recompute the value from the six sides; TRUE = it CHANGED (the BFS round propagates changes only). */
	boolean updateRedstone(int aRedstoneID);

	/** Upstream :38 — the per-segment loss of this wire on the channel (the range denominator). */
	long getRedstoneLoss(int aRedstoneID);

	/** Upstream :39 — the raw value of this wire on the channel (the :170 minus-loss source). */
	long getRedstoneValue(byte aSide, int aRedstoneID);

	/** Upstream :40 — what a wire NEIGHBOUR receives: this value minus this loss (the per-segment cost). */
	long getRedstoneMinusLoss(byte aSide, int aRedstoneID);

	/** The neighbour on aSide; {@code node() == null} = no redstone-wire neighbour there. */
	Adjacent adjacent(byte aSide);

	/**
	 * The pure-data form of the upstream DelegatorTileEntity pair — the neighbour node
	 * plus {@code mSideOfTileEntity} (the side of IT that faces us), the two values the
	 * upstream BFS round reads off one adjacency (:47-48).
	 */
	record Adjacent(@Nullable GTWireRedstoneNode node, byte side) {}

	/**
	 * Upstream ITileEntityRedstoneWire.Util.doRedstoneUpdate (:43-56) VERBATIM — the
	 * layer-order flood that PUSHES a changed value along the wire chain: each round walks
	 * the just-changed set, every emittable side whose neighbour is an accepting wire gets
	 * updateRedstone applied, and only nodes whose value CHANGED join the next round (the
	 * loop ends when a round changes nothing — the natural termination of a bounded loss
	 * chain). NOT the vanilla pull scheme (research card ②: vanilla RedStoneWireBlock
	 * :266/:305/:366 is the explicit counter-model, do not port it).
	 */
	static void doRedstoneUpdate(GTWireRedstoneNode aTileEntity, int aRedstoneID) {
		HashSetNoNulls<GTWireRedstoneNode> tSetUpdating = new HashSetNoNulls<>(false, aTileEntity), tSetNext = new HashSetNoNulls<>();
		while (!tSetUpdating.isEmpty()) {
			for (GTWireRedstoneNode tTileEntity : tSetUpdating) for (byte tSide = 0; tSide < 6; tSide++) if (tTileEntity.canEmitRedstoneToWire(tSide, aRedstoneID)) { // :46, ALL_SIDES_VALID
				Adjacent tDelegator = tTileEntity.adjacent(tSide); // :47, the (neighbour, side-facing-us) pair
				if (tDelegator.node() != null && tDelegator.node().canAcceptRedstoneFromWire(tDelegator.side(), aRedstoneID) && tDelegator.node().updateRedstone(aRedstoneID)) { // :48, the upstream instanceof narrows to this interface
					tSetNext.add(tDelegator.node()); // :49 — only CHANGED values propagate the round
				}
			}
			tSetUpdating.clear(); // :52-54
			tSetUpdating.addAll(tSetNext);
			tSetNext.clear();
		}
	}
}
