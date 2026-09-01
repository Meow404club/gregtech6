/**
 * The offline test of the :130-140 connect branch (task p10-wire-contact-damage
 * ride-along): a REDSTONE wire connecting to a connector neighbour whose connector-type
 * set shares NO element with its own — the live case being a redstone wire touching an
 * ELECTRIC wire (WIRE_REDSTONE vs WIRE_ELECTRIC never intersect). Upstream sets the own
 * mask bit and fires the change chain WITHOUT the :126 reciprocal (one-sided, purely
 * visual); intersecting-type neighbours still take the base symmetric handshake and the
 * electric rows keep the plain intersection gate. The handshake needs live neighbours, so
 * this test drives a minimal Level double (the GTRecipesOfflineTestBase.MinimalLevel form
 * with map-backed BE/state lookups).
 */
package gregtech6.tileentity.connectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWireSpecs.Row.Family;
import gregtech6.recipes.GTRecipesOfflineTestBase.MinimalLevel;
import gregtech6.tileentity.GTOfflineTestBase;

public class GTWireConnectBranchTest extends GTOfflineTestBase {

	static BlockEntityType<TestWire> sType;
	static final BlockPos POS = new BlockPos(5, 64, 5); // the redstone wire
	static final BlockPos NPOS = new BlockPos(5, 64, 4); // the neighbour (side 2 = NORTH of the wire)

	public static class TestWire extends GTWireBlockEntity {
		public TestWire(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}
	}

	/** Map-backed Level double: only what the connect handshake touches (BE lookup, block lookup, setBlock). */
	public static class WireLevel extends MinimalLevel {
		final Map<BlockPos, BlockEntity> mBlockEntities = new HashMap<>();
		final Map<BlockPos, BlockState> mStates = new HashMap<>();

		public WireLevel() {
			super(null);
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos aPos) {
			return mBlockEntities.get(aPos);
		}

		@Override
		public BlockState getBlockState(BlockPos aPos) {
			return mStates.getOrDefault(aPos, Blocks.AIR.defaultBlockState());
		}

		@Override
		public boolean setBlock(BlockPos aPos, BlockState aState, int aFlags) {
			mStates.put(aPos, aState);
			return true;
		}
	}

	@BeforeAll
	public static void buildOfflineFixture() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GTMaterialItems.initMaterials();
		@SuppressWarnings("unchecked")
		BlockEntityType<TestWire>[] tHolder = (BlockEntityType<TestWire>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TestWire(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** Offline Block construction needs the block registry temporarily unfrozen (the UseLockTest form). */
	private static GTWireBlock block(Family aFamily) {
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		return aFamily == Family.REDSTONE
				? new GTWireBlock(0, 1, GTWireSpecs.MAX_RANGE / 16, MT.RedAlloy, 1, false, 2, aFamily, BlockBehaviour.Properties.of())
				: new GTWireBlock(32, 1, 2, MT.Sn, 1, false, 2, aFamily, BlockBehaviour.Properties.of());
	}

	@Test
	public void redstoneWireVisuallyConnectsToAnElectricWireOneSided() {
		WireLevel tLevel = new WireLevel();
		GTWireBlockEntity tRedstone = sType.create(POS, block(Family.REDSTONE).defaultBlockState());
		GTWireBlockEntity tElectric = sType.create(NPOS, block(Family.ELECTRIC).defaultBlockState());
		tRedstone.setLevel(tLevel);
		tElectric.setLevel(tLevel);
		tLevel.mBlockEntities.put(POS, tRedstone);
		tLevel.mBlockEntities.put(NPOS, tElectric);

		// the types diverge (WIRE_REDSTONE vs WIRE_ELECTRIC), so the :130-140 arm fires
		assertTrue(TileEntityBase09Connector.haveOneCommonElement(
				tElectric.getConnectorTypes((byte)3), tRedstone.getConnectorTypes((byte)2)) == false,
				"precondition: the two connector-type sets share no element");
		assertTrue(tRedstone.connect((byte)2, true), "upstream :130-140 — the divergent connector neighbour still connects");
		assertTrue(tRedstone.connected((byte)2), "the redstone mask carries the bit (SBIT[2] = 4)");
		assertEquals(4, tRedstone.getConnections());
		assertEquals(0, tElectric.getConnections(), "ONE-SIDED: the upstream :130-140 body has no :126 reciprocal — the partner mask stays clean");
	}

	@Test
	public void intersectingTypesStillTakeTheSymmetricHandshake() {
		WireLevel tLevel = new WireLevel();
		GTWireBlockEntity tRedstone = sType.create(POS, block(Family.REDSTONE).defaultBlockState());
		GTWireBlockEntity tPartner = sType.create(NPOS, block(Family.REDSTONE).defaultBlockState());
		tRedstone.setLevel(tLevel);
		tPartner.setLevel(tLevel);
		tLevel.mBlockEntities.put(POS, tRedstone);
		tLevel.mBlockEntities.put(NPOS, tPartner);

		assertTrue(tRedstone.connect((byte)2, true), "same-family wires intersect (upstream :118)");
		assertTrue(tRedstone.connected((byte)2));
		assertTrue(tPartner.connected((byte)3), "the base handshake keeps its :126 reciprocal for intersecting types");
	}

	@Test
	public void electricRowsKeepThePlainIntersectionGate() {
		WireLevel tLevel = new WireLevel();
		GTWireBlockEntity tElectric = sType.create(POS, block(Family.ELECTRIC).defaultBlockState());
		GTWireBlockEntity tRedstone = sType.create(NPOS, block(Family.REDSTONE).defaultBlockState());
		tElectric.setLevel(tLevel);
		tRedstone.setLevel(tLevel);
		tLevel.mBlockEntities.put(POS, tElectric);
		tLevel.mBlockEntities.put(NPOS, tRedstone);

		// the :130-140 arm is gated on `this instanceof ITileEntityRedstoneWire` — an electric
		// wire touching a redstone wire does NOT connect its own mask (upstream behaviour too)
		assertFalse(tElectric.connect((byte)2, true), "electric rows stay behind the :118 intersection gate");
		assertEquals(0, tElectric.getConnections());
		assertEquals(0, tRedstone.getConnections());
	}
}
