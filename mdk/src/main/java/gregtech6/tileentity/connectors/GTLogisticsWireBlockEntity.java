package gregtech6.tileentity.connectors;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.logistics.ITileEntityLogistics;
import gregtech6.block.logistics.GTLogisticsWireBlock;
import gregtech6.registry.GT6Logistics;
import gregtech6.util.UT6;

/**
 * The logistics wire — 1.20.1 counterpart of gregapi/tileentity/connectors/
 * MultiTileEntityWireLogistics.java (60 lines, "Logistics Wire", upstream meta id 24901
 * / category "Logistics", Loader_MultiTileEntities.java:1819), task p32-logistics-lv2.
 * Upstream extended TileEntityBase10ConnectorRendered; this port mounts the same
 * {@link TileEntityBase09Connector} base the pipe and the wire family use (read-only
 * reuse, zero base-class changes) and implements the root {@link ITileEntityLogistics}
 * node face directly.
 *
 * <p>Verbatim semantics (the class is the connection MARKER of the wireless adjacency
 * network — no transport of its own; the Core walks {@code canLogistics} members):
 * <ul>
 * <li>{@link #canConnect(byte, BlockEntity)} (upstream :42-45) — the ONLY acceptance
 *     gate: a non-connector neighbour attaches iff it is an {@link ITileEntityLogistics}
 *     member whose back side answers {@code canLogistics}. Non-members never attach
 *     (the acceptance "非成员拒绝" arm).</li>
 * <li>{@link #canLogistics(byte)} (upstream :47) — {@code connected(aSide) ||
 *     SIDES_INVALID[aSide]}: an open end refuses, a connected side propagates the
 *     member answer along the wire chain (the "邻接扩散" observable), and the invalid
 *     side index (CS.java:698 {@code SIDES_INVALID = {F,F,F,F,F,F,T,T}} — SIDE_ANY = 6)
 *     answers unconditionally, which is exactly the
 *     AbstractCoverAttachmentLogistics.java:40 placement-gate query.</li>
 * <li>{@link #getConnectorTypes(byte)} (upstream :55) — {@link TD.Connectors#WIRE_LOGISTICS}:
 *     wire-to-wire adjacency goes through the base handshake's connector-type
 *     intersection (TileEntityBase09Connector :115-118), symmetric with the notify.</li>
 * </ul>
 *
 * <p>Trims (declared, against the 60-line upstream body): the render layer
 * (LOGISTICS_WIRE textures, :52-53 — the port's visuals ride the {@link GTLogisticsWireBlock}
 * CONNECTIONS BlockState, the pipe/wire-family form) and the {@code getFacingTool}
 * cutter face (:57 — no tool-type system on blocks in this port; connections form at
 * placement and through the base handshake, the pipe precedent). NO tick business —
 * the upstream class overrides no tick phase (pure marker), so this BE mounts
 * non-ticking (the base dispatcher never registers it with the level ticker).
 */
public class GTLogisticsWireBlockEntity extends TileEntityBase09Connector implements ITileEntityLogistics {

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTLogisticsWireBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to
	 * the registry type at runtime (the family-aware fallback is unnecessary here — the
	 * logistics wire is a single-type BET, unlike the GTWireBlockEntity three-family
	 * carrier), tests pass an offline-built BET.
	 */
	public GTLogisticsWireBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : GT6Logistics.LOGISTICS_WIRE_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "logistics_wire"; // BET registry path mirrors it (upstream :59 "gt.multitileentity.connector.wire.logistics")
	}

	// ---------------------------------------------------------------------------
	// the node face (upstream :42-45/:47)
	// ---------------------------------------------------------------------------

	@Override
	public boolean canLogistics(byte aSide) {
		return connected(aSide) || aSide < 0 || aSide >= 6; // upstream :47 — SIDES_INVALID[aSide] is T for the SIDE_ANY family queries
	}

	@Override
	public boolean canConnect(byte aSide, @Nullable BlockEntity aNeighbor) {
		// upstream :42-45 — the only non-connector acceptance: a logistics member whose
		// back side (the side of it that faces us) answers canLogistics
		if (!(aNeighbor instanceof ITileEntityLogistics tNode)) return false;
		byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
		return tNode.canLogistics(tOpposite);
	}

	@Override
	public Collection<TagData> getConnectorTypes(byte aSide) {
		return TD.Connectors.WIRE_LOGISTICS.AS_LIST; // upstream :55
	}

	// ---------------------------------------------------------------------------
	// visuals + placement (the pipe/wire-family form)
	// ---------------------------------------------------------------------------

	/**
	 * The mask becomes the CONNECTIONS BlockState — the visual counterpart of the
	 * connection data (the GTWireBlockEntity.onConnectionChange twin, :793-807 minus the
	 * redstone arms this family never mounts).
	 */
	@Override
	public void onConnectionChange(byte aPreviousConnections) {
		super.onConnectionChange(aPreviousConnections);
		if (hasLevel()) {
			BlockState tState = getBlockState();
			if (tState.hasProperty(GTLogisticsWireBlock.CONNECTIONS)
					&& tState.getValue(GTLogisticsWireBlock.CONNECTIONS) != (int)getConnections()) {
				getLevel().setBlock(getBlockPos(), tState.setValue(GTLogisticsWireBlock.CONNECTIONS, (int)getConnections()),
						Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
			}
		}
	}

	/**
	 * Upstream TileEntityBase09Connector.onPlaced (:82-96), server side, driven by the
	 * BlockItem place chain ({@link gregtech6.block.logistics.GTLogisticsWireBlockItem}
	 * — the hook that holds both the live BE and the BlockPlaceContext) and by
	 * /gt6logistics wire place. aSide is the CLICKED face (0..5): the wire side that
	 * touches the support is its opposite (the OPOS flip). After the support connect,
	 * the :90-94 loop back-connects every neighbour connector that already faces this
	 * wire. Duplicated from the pipe/wire BEs: the base file is frozen (zero-seam
	 * ruling) and owns no onPlaced in this port.
	 */
	public void onPlaced(byte aSide) {
		if (aSide < 0 || aSide >= 6 || !hasLevel() || !isServerSide()) return;
		connect(UT6.OPOS[aSide], true); // upstream :84/:88
		for (byte tSide = 0; tSide < 6; tSide++) { // upstream :90-94
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(tSide)));
			if (tNeighbor instanceof TileEntityBase09Connector tConnector) {
				byte tOpposite = (byte)Direction.from3DDataValue(tSide).getOpposite().get3DDataValue();
				if (tConnector.connected(tOpposite)
						&& haveOneCommonElement(tConnector.getConnectorTypes(tOpposite), getConnectorTypes(tSide))) {
					connect(tSide, true); // upstream :93
				}
			}
		}
	}
}
