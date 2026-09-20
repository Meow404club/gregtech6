package gregtech6.tileentity.connectors;

import java.util.Collection;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.logistics.ITileEntityLogistics;
import gregtech6.tileentity.GTOfflineTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offline truth tables for the logistics wire BE (task p32-logistics-lv2, the
 * GTItemPipeBlockEntityTest shape — the RCON live chain carries the adjacency spread,
 * these tables pin the decision functions the spread is made of).
 *
 * <p>Verbatim anchors: canConnect upstream MultiTileEntityWireLogistics.java:42-45, the
 * canLogistics truth table upstream :47 (SIDES_INVALID = CS.java:698), the connector
 * types upstream :55.
 */
public class GTLogisticsWireBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GTLogisticsWireBlockEntity> sType;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	/** The non-connector interface member stub — the Lv3 endpoint shape (Core/Storage) the wire attaches to. */
	static class MemberBlockEntity extends BlockEntity implements ITileEntityLogistics {
		final boolean mAnswer;
		MemberBlockEntity(boolean aAnswer) {
			super(sType, POS, Blocks.STONE.defaultBlockState());
			mAnswer = aAnswer;
		}
		@Override
		public boolean canLogistics(byte aSide) {
			return mAnswer;
		}
	}

	@BeforeAll
	static void buildOfflineFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GTLogisticsWireBlockEntity>[] tHolder = (BlockEntityType<GTLogisticsWireBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTLogisticsWireBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sType = tHolder[0];
	}

	private static GTLogisticsWireBlockEntity wire() {
		return sType.create(POS, Blocks.STONE.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// canLogistics — the node face truth table (upstream :47)
	// ---------------------------------------------------------------------------

	@Test
	public void canLogisticsTruthTable() {
		GTLogisticsWireBlockEntity tWire = wire();
		// an open end refuses — every valid side of a fresh wire
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertFalse(tWire.canLogistics(tSide), "side " + tSide + " of an unconnected wire");
		}
		// the SIDES_INVALID family (CS.java:698 {F,F,F,F,F,F,T,T}): SIDE_ANY = 6 answers
		// unconditionally — this is exactly the AbstractCoverAttachmentLogistics.java:40 gate query
		assertTrue(tWire.canLogistics((byte)6));
		assertTrue(tWire.canLogistics((byte)7));
		assertTrue(tWire.canLogistics((byte)-1));
		// a connected side propagates the member answer — the adjacency spread observable
		tWire.mConnections = TileEntityBase09Connector.SBIT[3];
		assertTrue(tWire.canLogistics((byte)3), "a connected side answers true (upstream :47)");
		assertFalse(tWire.canLogistics((byte)4), "an unconnected side still refuses");
	}

	// ---------------------------------------------------------------------------
	// canConnect — the member-only acceptance gate (upstream :42-45)
	// ---------------------------------------------------------------------------

	@Test
	public void canConnectAcceptsLogisticsMembersOnly() {
		GTLogisticsWireBlockEntity tWire = wire();
		byte tSide = 2; // the queried side of the wire; the neighbour faces back on side 3
		byte tOpposite = (byte)Direction.from3DDataValue(tSide).getOpposite().get3DDataValue();
		assertEquals(3, tOpposite);

		// a member whose back side answers — attach
		assertTrue(tWire.canConnect(tSide, new MemberBlockEntity(true)), "upstream :43 — the member with canLogistics(back)=T");
		// a member whose back side refuses — the :43 instanceof arm holds but the answer gates
		assertFalse(tWire.canConnect(tSide, new MemberBlockEntity(false)), "upstream :43 — the member with canLogistics(back)=F");
		// a NON-member — refused outright (the acceptance "非成员拒绝" arm; upstream :44 F)
		BlockEntity tForeign = new BlockEntity(sType, POS, Blocks.STONE.defaultBlockState()) {/**/};
		assertFalse(tWire.canConnect(tSide, tForeign), "upstream :44 — a non-member never attaches");
		assertFalse(tWire.canConnect(tSide, null));
	}

	// ---------------------------------------------------------------------------
	// connector types — the wire-chain identity (upstream :55)
	// ---------------------------------------------------------------------------

	@Test
	public void connectorTypesAreLogisticsOnly() {
		GTLogisticsWireBlockEntity tWire = wire();
		Collection<TagData> tTypes = tWire.getConnectorTypes((byte)0);
		assertEquals(TD.Connectors.WIRE_LOGISTICS.AS_LIST, tTypes, "upstream :55 — the single logistics type");
		// wire↔wire: the base handshake intersection (TileEntityBase09Connector :115-118) holds
		assertTrue(TileEntityBase09Connector.haveOneCommonElement(tTypes, wire().getConnectorTypes((byte)2)),
				"two logistics wires share the connector type");
		// the wire family must NOT interconnect with the pipe families
		assertFalse(TileEntityBase09Connector.haveOneCommonElement(tTypes, new GTItemPipeBlockEntity(sType, POS, Blocks.STONE.defaultBlockState()).getConnectorTypes((byte)0)),
				"a logistics wire never type-intersects an item pipe");
	}

	// ---------------------------------------------------------------------------
	// persistence — the base connection-mask contract (TileEntityBase09Connector NBT)
	// ---------------------------------------------------------------------------

	@Test
	public void connectionMaskRoundTripsThroughNbt() {
		GTLogisticsWireBlockEntity tWire = wire();
		tWire.mConnections = 63; // all six sides
		CompoundTag tNBT = new CompoundTag();
		tWire.saveAdditional(tNBT);
		assertEquals((byte)63, tNBT.getByte(TileEntityBase09Connector.NBT_CONNECTION), "the base NBT key");
		GTLogisticsWireBlockEntity tLoaded = wire();
		tLoaded.load(tNBT);
		assertEquals(63, tLoaded.getConnections(), "the &63 clamp round trip (upstream :55/:61)");
		// no logistics-specific state exists — the pure marker (the upstream writeToNBT2 absence):
		// a fresh wire persists only the base-chain keys (te_name + the zero mask)
		CompoundTag tClean = new CompoundTag();
		wire().saveAdditional(tClean);
		assertEquals((byte)0, tClean.getByte(TileEntityBase09Connector.NBT_CONNECTION), "a fresh wire persists the zero mask");
	}
}
