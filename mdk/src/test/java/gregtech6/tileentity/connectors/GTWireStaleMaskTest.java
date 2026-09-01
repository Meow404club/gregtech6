/**
 * The offline test of the p11-connector-stale-mask prune: the connection mask maintenance
 * path the handshake never had. The P10 E1 RCON finding — swap the wire's neighbour via
 * {@code /setblock} (flags=2: no {@code neighborChanged} anywhere upstream or in vanilla)
 * and the old mask bit survives forever — pinned here against
 * {@link GTWireBlockEntity#validateConnections}: a bit survives ONLY while the current
 * neighbour is one the family's own {@code connect} decision would still accept
 * ({@link GTWireBlockEntity#canStayConnected}); foreign-family/solid replacements prune,
 * air stays a legitimate open end (the NOT-a-bug half of the repro matrix), redstone rows
 * never prune (upstream :172/:130-140/:141 accept everything) and the
 * {@link GTWireBlock#updateShape} seam — the only channel {@code /setblock} reaches — runs
 * the prune synchronously with no tick involved. The handshake needs live neighbours, so
 * this test drives a minimal Level double (the GTWireConnectBranchTest.WireLevel form with
 * map-backed BE/state lookups and an {@code isLoaded} stub for the chunk-border guard).
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
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
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
import gregtech6.tileentity.energy.GTEnergySourceBlockEntity;

public class GTWireStaleMaskTest extends GTOfflineTestBase {

	static BlockEntityType<TestWire> sType;
	static BlockEntityType<GTEnergySourceBlockEntity> sSourceType;
	static final BlockPos POS = new BlockPos(5, 64, 5);  // the wire under test
	static final BlockPos NPOS = new BlockPos(5, 64, 4); // the neighbour (side 2 = NORTH of the wire, SBIT = 4)

	public static class TestWire extends GTWireBlockEntity {
		public TestWire(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}
	}

	/** Map-backed Level double: BE lookup, block lookup, setBlock, plus the isLoaded stub the prune's chunk-border guard reads. */
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
			// vanilla LevelChunk.setBlockState keeps the BE's own state field in sync on every
			// setBlock (:248-250) — onConnectionChange's diff gate reads exactly that field
			BlockEntity tBE = mBlockEntities.get(aPos);
			if (tBE != null) {
				try {
					Method tSet = BlockEntity.class.getDeclaredMethod("setBlockState", BlockState.class);
					tSet.setAccessible(true);
					tSet.invoke(tBE, aState);
				} catch (Exception aE) {
					throw new IllegalStateException("could not sync the BE state field", aE);
				}
			}
			return true;
		}

		@Override
		public boolean isLoaded(BlockPos aPos) {
			return true; // MinimalLevel.getChunkSource() is null — the offline stand-in for "the neighbour chunk is here"
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
		@SuppressWarnings("unchecked")
		BlockEntityType<GTEnergySourceBlockEntity>[] tSourceHolder = (BlockEntityType<GTEnergySourceBlockEntity>[]) new BlockEntityType<?>[1];
		tSourceHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTEnergySourceBlockEntity(tSourceHolder[0], aPos, aState), Blocks.STONE).build(null);
		sSourceType = tSourceHolder[0];
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
		return switch (aFamily) {
		case REDSTONE -> new GTWireBlock(0, 1, GTWireSpecs.MAX_RANGE / 16, MT.RedAlloy, 1, false, 2, aFamily, BlockBehaviour.Properties.of());
		case LASER -> new GTWireBlock(0, 1, 0, null, 1, false, 2, aFamily, BlockBehaviour.Properties.of());
		default -> new GTWireBlock(32, 1, 2, MT.Sn, 1, false, 2, aFamily, BlockBehaviour.Properties.of());
		};
	}

	/** The connected electric pair: wire A at POS (side 2, SBIT 4), wire B at NPOS (side 3, SBIT 8). */
	private static Object[] electricPair(WireLevel aLevel) {
		GTWireBlockEntity tWireA = sType.create(POS, block(Family.ELECTRIC).defaultBlockState());
		GTWireBlockEntity tWireB = sType.create(NPOS, block(Family.ELECTRIC).defaultBlockState());
		tWireA.setLevel(aLevel);
		tWireB.setLevel(aLevel);
		aLevel.mBlockEntities.put(POS, tWireA);
		aLevel.mBlockEntities.put(NPOS, tWireB);
		assertTrue(tWireA.connect((byte)2, true), "precondition: the symmetric handshake connects the pair");
		assertEquals(4, tWireA.getConnections());
		assertEquals(8, tWireB.getConnections());
		return new Object[] {tWireA, tWireB};
	}

	/** Swaps the NPOS neighbour the /setblock way: block state always, BE only when one is given. */
	private static void setNeighbor(WireLevel aLevel, BlockEntity aNeighbor, Block aBlock) {
		BlockState tState = aBlock.defaultBlockState();
		if (aNeighbor != null) {
			aNeighbor.setLevel(aLevel);
			aLevel.mBlockEntities.put(NPOS, aNeighbor);
		} else {
			aLevel.mBlockEntities.remove(NPOS);
		}
		aLevel.mStates.put(NPOS, tState);
	}

	@Test
	public void familySwapPrunesTheStaleElectricBit() {
		WireLevel tLevel = new WireLevel();
		GTWireBlockEntity tWireA = (GTWireBlockEntity)electricPair(tLevel)[0];
		// the P10 E1 scenario: the electric partner is replaced by a redstone-family wire
		setNeighbor(tLevel, sType.create(NPOS, block(Family.REDSTONE).defaultBlockState()), block(Family.REDSTONE));
		assertTrue(tWireA.connected((byte)2), "precondition: the stale bit is on the mask before the rescan");
		tWireA.validateConnections();
		assertEquals(0, tWireA.getConnections(), "the electric bit toward a redstone-family wire is unattainable through every legitimate connect path — pruned");
		assertFalse(tWireA.connected((byte)2));
		// the mask is the CONNECTIONS BlockState — the prune must propagate (onConnectionChange)
		assertEquals(0, (int)tLevel.mStates.get(POS).getValue(GTWireBlock.CONNECTIONS), "the BlockState mirror follows the pruned mask");
	}

	@Test
	public void solidSwapAlsoPrunes() {
		WireLevel tLevel = new WireLevel();
		GTWireBlockEntity tWireA = (GTWireBlockEntity)electricPair(tLevel)[0];
		setNeighbor(tLevel, null, Blocks.STONE); // the electric family can never connect a solid block
		tWireA.validateConnections();
		assertEquals(0, tWireA.getConnections(), "the bit toward a plain solid block is residue too");
	}

	@Test
	public void removalToAirKeepsTheOpenEnd() {
		WireLevel tLevel = new WireLevel();
		GTWireBlockEntity tWireA = (GTWireBlockEntity)electricPair(tLevel)[0];
		setNeighbor(tLevel, null, Blocks.AIR);
		tWireA.validateConnections();
		assertEquals(4, tWireA.getConnections(), "air is a legitimate connect target (the base open-end arm) — the bit stays, upstream-faithful");
	}

	@Test
	public void laserSwapPrunesAndOpenEndsStay() {
		WireLevel tLevel = new WireLevel();
		// a laser pair (the WIRE_LASER intersection) then the partner swapped to an electric wire
		GTWireBlockEntity tLaser = sType.create(POS, block(Family.LASER).defaultBlockState());
		GTWireBlockEntity tPartner = sType.create(NPOS, block(Family.LASER).defaultBlockState());
		tLaser.setLevel(tLevel);
		tPartner.setLevel(tLevel);
		tLevel.mBlockEntities.put(POS, tLaser);
		tLevel.mBlockEntities.put(NPOS, tPartner);
		assertTrue(tLaser.connect((byte)2, true));
		assertEquals(4, tLaser.getConnections());
		setNeighbor(tLevel, sType.create(NPOS, block(Family.ELECTRIC).defaultBlockState()), block(Family.ELECTRIC));
		tLaser.validateConnections();
		assertEquals(0, tLaser.getConnections(), "laser bits toward a foreign-family wire prune (the LU probe is permanently false on this port)");
		// the air swap half: an open end stays
		setNeighbor(tLevel, null, Blocks.AIR);
		tLaser.connect((byte)2, true); // re-form the open end through the front door (the base air arm)
		tLaser.validateConnections();
		assertEquals(4, tLaser.getConnections(), "the laser open end survives the rescan");
	}

	@Test
	public void redstoneBitsAreNeverPruned() {
		WireLevel tLevel = new WireLevel();
		// the one-sided :130-140 bit: a redstone wire pointing at an electric wire
		GTWireBlockEntity tRedstone = sType.create(POS, block(Family.REDSTONE).defaultBlockState());
		GTWireBlockEntity tElectric = sType.create(NPOS, block(Family.ELECTRIC).defaultBlockState());
		tRedstone.setLevel(tLevel);
		tElectric.setLevel(tLevel);
		tLevel.mBlockEntities.put(POS, tRedstone);
		tLevel.mBlockEntities.put(NPOS, tElectric);
		assertTrue(tRedstone.connect((byte)2, true), "the divergent connector arm connects one-sidedly");
		assertEquals(4, tRedstone.getConnections());
		// the electric wire is removed: the redstone family accepts air (:141) — no prune...
		setNeighbor(tLevel, null, Blocks.AIR);
		tRedstone.validateConnections();
		assertEquals(4, tRedstone.getConnections(), "redstone rows accept every neighbour (upstream :172) — no bit can be stale");
		// ...and a solid replacement changes nothing either
		setNeighbor(tLevel, null, Blocks.STONE);
		tRedstone.validateConnections();
		assertEquals(4, tRedstone.getConnections());
	}

	@Test
	public void unrelatedNeighbourChangeKeepsValidBits() {
		WireLevel tLevel = new WireLevel();
		Object[] tPair = electricPair(tLevel);
		// the partner is replaced by ANOTHER electric wire — the bit stays valid
		setNeighbor(tLevel, sType.create(NPOS, block(Family.ELECTRIC).defaultBlockState()), block(Family.ELECTRIC));
		GTWireBlockEntity tWireA = (GTWireBlockEntity)tPair[0];
		GTWireBlockEntity tWireB = (GTWireBlockEntity)tLevel.mBlockEntities.get(NPOS);
		tWireA.validateConnections();
		assertEquals(4, tWireA.getConnections(), "an electric-to-electric bit survives the rescan (the handshake decision re-derived, not reset)");
		assertEquals(0, tWireB.getConnections(), "the prune never auto-connects — the fresh partner (mask 0) gains no bit");
	}

	@Test
	public void energyEmitterNeighbourBitStays() {
		WireLevel tLevel = new WireLevel();
		GTWireBlockEntity tWireA = (GTWireBlockEntity)electricPair(tLevel)[0];
		// the p8 backfill probe: the theoretical emitting check accepts a pure emitter BE (mEmitting=false is fine — aTheoretical=true)
		setNeighbor(tLevel, sSourceType.create(NPOS, Blocks.STONE.defaultBlockState()), Blocks.STONE);
		tWireA.validateConnections();
		assertEquals(4, tWireA.getConnections(), "the canConnect EU double probe (accepting || emitting, theoretical) keeps the machine bit");
	}

	@Test
	public void updateShapeRunsThePruneSynchronously() {
		WireLevel tLevel = new WireLevel();
		GTWireBlockEntity tWireA = (GTWireBlockEntity)electricPair(tLevel)[0];
		setNeighbor(tLevel, sType.create(NPOS, block(Family.REDSTONE).defaultBlockState()), block(Family.REDSTONE));
		// the /setblock seam: flags=2 delivers updateShape with NO tick and NO neighborChanged
		GTWireBlock tBlock = block(Family.ELECTRIC);
		tBlock.updateShape(tBlock.defaultBlockState(), Direction.NORTH, tLevel.mStates.get(NPOS), tLevel, POS, NPOS);
		assertEquals(0, tWireA.getConnections(), "the shape-update channel prunes synchronously — no BE tick involved");
		assertTrue(tWireA.mBlockUpdated, "the deferred per-tick re-check is armed as well (the upstream :103 consumption)");
	}
}
