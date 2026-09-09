package gregtech6.tileentity.inventories;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The wake handshake of the storage-hopper family — 1.20.1 counterpart of
 * gregapi/tileentity/ITileEntityAdjacentInventoryUpdatable.java:28-35 (the interface minus
 * its ITileEntityUnloadable parent, whose mShouldRefresh lifecycle already lives on
 * {@code TileEntityBase01Root}).
 *
 * <p>Upstream javadoc, verbatim intent: "Gets called by some GT things like Pipes to notify
 * about changes inside the Inventory of the caller. This is only for important Inventory
 * Updates, like when a Pipe has more free space than before." The hopper family consumers
 * (MultiTileEntityHopper.java:226-231 / MultiTileEntityQueueHopper.java:209-214): after a
 * tick in which anything moved, every horizontal neighbour (all sides but the top and the
 * own output face) implementing this interface gets
 * {@code adjacentInventoryUpdated(sideOfNeighborFacingMe, this)} — which re-arms an idle
 * ({@code mCheck == -1}) neighbour hopper to {@code mCheck == 0} so hopper-to-hopper chains
 * flow every tick instead of waiting for the 20-tick sync arm. The parameter is the SIDE OF
 * THE RECEIVER that faces the caller (the upstream DelegatorTileEntity.mSideOfTileEntity of
 * the walk from the caller outward).
 */
public interface GT6AdjacentInventoryUpdatable {

	/** The upstream single method — the receiver re-plans its transfer timing off it. */
	void adjacentInventoryUpdated(byte aSide, BlockEntity aSource);
}
