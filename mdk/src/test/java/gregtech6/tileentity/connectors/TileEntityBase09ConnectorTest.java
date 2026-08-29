package gregtech6.tileentity.connectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregapi.code.TagData;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * TileEntityBase09Connector offline tests (task p4-fluid-pipes spec ②): the 6-bit mask
 * arithmetic, the NBT round trip with the &amp;63 clamp, and the connector-type
 * intersection gate. The world-level connect() handshake needs live neighbours — it is
 * exercised by the runServer command path (acceptance ②), like the chest open chain was.
 */
public class TileEntityBase09ConnectorTest extends GTOfflineTestBase {

	/** Minimal concrete connector for offline fixture use. */
	public static class TestConnector extends TileEntityBase09Connector {
		public TestConnector(BlockEntityType<?> aType, BlockPos aPos) {
			super(false, aType, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public Collection<TagData> getConnectorTypes(byte aSide) {
			return TD.Connectors.PIPE_FLUID.AS_LIST;
		}

		@Override
		public String getTileEntityName() {
			return "test_connector";
		}
	}

	static BlockEntityType<TestConnector> sType;
	static final BlockPos POS = new BlockPos(1, 2, 3);

	@BeforeAll
	static void buildOfflineFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TestConnector>[] tHolder = (BlockEntityType<TestConnector>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new TestConnector(tHolder[0], aPos), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	@Test
	public void connectionBitsFollowTheGT6SideOrder() {
		TestConnector tConnector = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertFalse(tConnector.connected((byte)0), "starts unconnected (upstream :50)");
		assertFalse(tConnector.connected((byte)6), "out-of-range sides are never connected");
		assertFalse(tConnector.connected((byte)-1), "negative sides are never connected");

		tConnector.setConnectionBit((byte)3);
		assertTrue(tConnector.connected((byte)3), "SBIT[3] = 8 (CS.java:612)");
		assertEquals(8, tConnector.getConnections());
		assertFalse(tConnector.connected((byte)2), "neighbouring sides stay clear");
	}

	@Test
	public void nbtRoundTripClampsToSixBits() {
		TestConnector tConnector = sType.create(POS, Blocks.STONE.defaultBlockState());
		tConnector.mConnections = 63; // every side
		CompoundTag tSaved = tConnector.saveWithoutMetadata();
		assertEquals(63, tSaved.getByte("connections"));

		tConnector.mConnections = -1; // upstream :55 clamps with & 63 on read
		tConnector.load(tSaved);
		assertEquals(63, tConnector.getConnections());

		CompoundTag tOverflow = new CompoundTag();
		tOverflow.putByte("connections", (byte)-1); // all bits set as a signed byte
		tConnector.load(tOverflow);
		assertEquals(63, tConnector.getConnections(), "upstream :55 — (byte)(getByte & 63)");

		tOverflow.putByte("connections", (byte)0b11111100); // bits beyond the six sides
		tConnector.load(tOverflow);
		assertEquals(0b111100, tConnector.getConnections(), "bits 6/7 are stripped by the same clamp");
	}

	@Test
	public void connectorTypesIntersectionGate() {
		Collection<TagData> tPipe = TD.Connectors.PIPE_FLUID.AS_LIST;
		Collection<TagData> tOther = TD.Connectors.WIRE_ELECTRIC.AS_LIST;
		Collection<TagData> tEmpty = List.of();

		assertTrue(TileEntityBase09Connector.haveOneCommonElement(tPipe, tPipe), "same type connects (upstream :118)");
		assertFalse(TileEntityBase09Connector.haveOneCommonElement(tPipe, tOther), "pipe does not shake hands with wire");
		assertFalse(TileEntityBase09Connector.haveOneCommonElement(tPipe, tEmpty), "empty type set never connects");
		assertFalse(TileEntityBase09Connector.haveOneCommonElement(null, tPipe), "null-safe");
	}

	@Test
	public void syncGateTracksConnectionChanges() {
		TestConnector tConnector = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertFalse(tConnector.onTickCheck(1), "no change, no sync (chest mUsingPlayers idiom)");
		tConnector.setConnectionBit((byte)0);
		assertTrue(tConnector.onTickCheck(2));
		tConnector.onTickResetChecks(2, true);
		assertFalse(tConnector.onTickCheck(3), "reset clears the change flag");
	}
}
