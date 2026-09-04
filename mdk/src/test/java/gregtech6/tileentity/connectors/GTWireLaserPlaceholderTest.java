/**
 * The offline assertions of the laser-wire placeholder (task p10-wire-laser-placeholder).
 * Three pins: the {@link GTWireBlockEntity#transferLaser} SHELL is pure and returns 0 (the
 * upstream :66-86 flood is documentation-only — no LU carrier exists on this port), the
 * laser family is INERT three ways (accepts NO energy carrier — the "no EU" half of the
 * triad, the no-burn counters, the no-shock carrier matrix) and the family wiring resolves
 * the laser identities (isLaser/isRedstone split, WIRE_LASER connector type, the wire_laser
 * BE name). The live placement chain is the RCON channel.
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.HashSetNoNulls;
import gregapi.data.MT;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWireSpecs.Row.Family;

public class GTWireLaserPlaceholderTest {

	static BlockEntityType<TestWire> sType;
	static final BlockPos POS = new BlockPos(1, 2, 3);

	/** Offline BE fixture: a non-null BET keeps the constructor off the registry-backed fallback. */
	public static class TestWire extends GTWireBlockEntity {
		public TestWire(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}
	}

	@BeforeAll
	public static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GTMaterialItems.initMaterials();
		@SuppressWarnings("unchecked")
		BlockEntityType<TestWire>[] tHolder = (BlockEntityType<TestWire>[]) new BlockEntityType<?>[1];
		// 21.1 validates the BE type/state pair at the ctor: the valid set carries the
		// cached GT6 wire blocks (task p15-m4-test-infra-2).
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TestWire(tHolder[0], aPos, aState),
				Blocks.STONE, block(Family.LASER, MT.NULL, 0, 0), block(Family.ELECTRIC, MT.Sn, 32, 2)).build(null);
		sType = tHolder[0];
	}

	/**
	 * Offline Block construction needs the block registry temporarily unfrozen (the
	 * UseLockTest form). Instances are memoized per carrier row — the 21.1 BE ctor
	 * validates the state against the BET's valid set, so the fixture must hand out ONE
	 * stable block identity per row (task p15-m4-test-infra-2).
	 */
	private record WireKey(Family aFamily, OreDictMaterial aMaterial, long aVoltage, long aLoss) {}

	private static final Map<WireKey, GTWireBlock> sBlockCache = new HashMap<>();

	private static GTWireBlock block(Family aFamily, OreDictMaterial aMaterial, long aVoltage, long aLoss) {
		WireKey tKey = new WireKey(aFamily, aMaterial, aVoltage, aLoss);
		GTWireBlock tCached = sBlockCache.get(tKey);
		if (tCached != null) return tCached;
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		GTWireBlock tBlock = new GTWireBlock(aVoltage, 1, aLoss, aMaterial, 1, false, 6, aFamily, BlockBehaviour.Properties.of());
		sBlockCache.put(tKey, tBlock);
		return tBlock;
	}

	private static GTWireBlockEntity laserWire() {
		return sType.create(POS, block(Family.LASER, MT.NULL, 0, 0).defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the shell (upstream :66-86 stands as documentation only)
	// ---------------------------------------------------------------------------

	@Test
	public void transferLaserShellIsPureZero() {
		GTWireBlockEntity tLaser = laserWire();
		assertTrue(tLaser.isLaser(), "the fixture is a laser-family BE");
		assertEquals(0, tLaser.transferLaser((byte)0, 4, 100, -1, new HashSetNoNulls<>(false, tLaser)),
				"the shell floods NOTHING — the upstream :66-86 observable when no LU consumer exists");
		// purity: the call leaves no state behind (no transfer bookkeeping, no strikes)
		assertEquals(0, tLaser.mTransferredWattage);
		assertEquals(0, tLaser.mTransferredAmperes);
		assertEquals(0, tLaser.mBurnCounter);
		assertEquals(0, tLaser.mWattageLast);
	}

	@Test
	public void shellStaysZeroOnEveryFamily() {
		// the shell is the shared-class placeholder — an electric row answers the same 0
		GTWireBlockEntity tElectric = sType.create(POS, block(Family.ELECTRIC, MT.Sn, 32, 2).defaultBlockState());
		assertFalse(tElectric.isLaser());
		assertEquals(0, tElectric.transferLaser((byte)0, 32, 2, -1, new HashSetNoNulls<>(false, tElectric)),
				"the placeholder answers 0 regardless of family — the flood is not mounted");
	}

	// ---------------------------------------------------------------------------
	// the inert triad: NO EU carrier, NO burn, NO shock
	// ---------------------------------------------------------------------------

	@Test
	public void laserFamilyAcceptsNoEnergyType() {
		GTWireBlockEntity tLaser = laserWire();
		// the "no EU" half: upstream the laser wire conducts LU ONLY (:94/:111) and this port
		// mounts no LU carrier, so NO type at all is mounted
		assertFalse(tLaser.isEnergyType(TD.Energy.EU, (byte)0, false), "isEnergyType refuses EU (upstream it would refuse everything but LU :94)");
		assertFalse(tLaser.isEnergyType(TD.Energy.LU, (byte)0, false), "no LU carrier is mounted on this port either");
		assertTrue(tLaser.getEnergyTypes((byte)0).isEmpty(), "the :206 type list is empty");
		assertFalse(tLaser.isEnergyAcceptingFrom(TD.Energy.EU, (byte)0, false));
		assertFalse(tLaser.isEnergyEmittingTo(TD.Energy.EU, (byte)0, true));
		// both doEnergyInjection arms refuse — nothing can be pushed into the laser wire
		assertEquals(0, tLaser.doEnergyInjection(TD.Energy.EU, (byte)0, 32, 2, true), "the real push is refused");
		assertEquals(0, tLaser.doEnergyInjection(TD.Energy.EU, (byte)0, 32, 2, false), "the simulate arm reports nothing taken");
		// and the canConnect LU probe (:89-92) is permanently false — an EU neighbour attaches nothing
		assertFalse(tLaser.canConnect((byte)0, sType.create(new BlockPos(1, 2, 4), block(Family.ELECTRIC, MT.Sn, 32, 2).defaultBlockState())),
				"the neighbour's EU face must NOT attach the laser wire (upstream probes LU, not EU)");
	}

	@Test
	public void laserFamilyNeverBurns() {
		GTWireBlockEntity tLaser = laserWire();
		// the tick is a structural no-op on laser rows (upstream :57-64 trims to nothing here)
		tLaser.onTick(1, true);
		tLaser.onTick(513, true);
		assertEquals(0, tLaser.mBurnCounter, "no overload strikes can accumulate — 16 would set the wire on fire");
		assertEquals(0, tLaser.mWattageLast, "no wattage window ever opens (nothing can flow)");
		// the ratings ride the block carrier: loss 0 (the LOSSLESS wire :114), no EU voltage
		assertEquals(0, tLaser.mLoss);
		assertEquals(0, tLaser.mVoltage);
	}

	@Test
	public void laserFamilyNeverShocks() {
		// the carrier matrix: NBT_CONTACTDAMAGE F on the Loader:1815 registration
		GTWireBlock tLaserBlock = block(Family.LASER, MT.NULL, 0, 0);
		assertFalse(tLaserBlock.contactDamage(), "NBT_CONTACTDAMAGE F (Loader:1815) — no entityInside hook, no 2px inset");
		// the full-cube collision box (an inert wire keeps the plain block shape, upstream :219 else-branch)
		VoxelShape tShape = tLaserBlock.getCollisionShape(
				tLaserBlock.defaultBlockState(), net.minecraft.world.level.EmptyBlockGetter.INSTANCE, POS, CollisionContext.empty());
		assertEquals(Shapes.block().min(Direction.Axis.Y), tShape.min(Direction.Axis.Y), 1.0e-9);
		assertEquals(Shapes.block().max(Direction.Axis.Y), tShape.max(Direction.Axis.Y), 1.0e-9);
		// and the reachable shock chain is dead: nothing flows in, so mWattageLast stays 0
		GTWireBlockEntity tLaser = laserWire();
		assertEquals(0, tLaser.pendingContactDamage(), "tierMax(0) * 4 = 0 — the :203 body is a no-op on the laser family");
	}

	// ---------------------------------------------------------------------------
	// the family wiring (the laser identities resolve through the shared carrier)
	// ---------------------------------------------------------------------------

	@Test
	public void laserFamilyWiringResolves() {
		GTWireBlockEntity tLaser = laserWire();
		assertTrue(tLaser.isLaser());
		assertFalse(tLaser.isRedstone(), "the laser family is NOT the redstone family — the BFS machinery stays off");
		assertTrue(tLaser.getConnectorTypes((byte)0).contains(TD.Connectors.WIRE_LASER),
				"MultiTileEntityWireLaser :124 — laser-to-laser chains form, electric chains do not");
		assertFalse(tLaser.getConnectorTypes((byte)0).contains(TD.Connectors.WIRE_ELECTRIC),
				"the WIRE_LASER set never intersects WIRE_ELECTRIC (upstream :124 vs :243)");
		assertEquals("wire_laser", tLaser.getTileEntityName(), "the BET registry path twin (GTWires.WIRE_LASER_BE)");
	}
}
