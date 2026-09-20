/**
 * The offline assertions of the laser-wire LU revival (task p32-qu-laser-domain — the
 * p10-wire-laser-placeholder revival condition, now fulfilled): the
 * {@link GTWireBlockEntity#transferLaser} flood is LIVE (MultiTileEntityWireLaser :66-86),
 * the laser family answers LU on the energy face family (:94-:100 — EU stays refused),
 * the canConnect LU probe (:89-92) is live, and the flood books the lossless ledger
 * ({@code mTransferred += |frequency * used|}, :83). The no-burn/no-shock posture and the
 * family wiring (isLaser/isRedstone split, WIRE_LASER connector type, the wire_laser BE
 * name) carry over from the p10 card unchanged. The live placement chain is the RCON
 * channel.
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

import gregapi.code.HashSetNoNulls;
import gregapi.code.TagData;
import gregapi.data.MT;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.tileentity.energy.EnergyTarget;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.registry.GT6Lasers;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWireSpecs.Row.Family;
import gregtech6.tileentity.energy.converters.GT6LaserConverterBlockEntity;

public class GTWireLaserRevivalTest {

	static BlockEntityType<TestWire> sType;
	static BlockEntityType<GT6LaserConverterBlockEntity> sLaserType;
	static final BlockPos POS = new BlockPos(1, 2, 3);

	/** The three-wire flood geometry: WIRE1 (entry) at z=6, WIRE2 at z=5, the SINK (absorber) at z=4 — all north-linked. */
	static final BlockPos WIRE1 = new BlockPos(5, 64, 6);
	static final BlockPos WIRE2 = new BlockPos(5, 64, 5);
	static final BlockPos SINK = new BlockPos(5, 64, 4);

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
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6LaserConverterBlockEntity>[] tLaserHolder =
				(BlockEntityType<GT6LaserConverterBlockEntity>[]) new BlockEntityType<?>[1];
		tLaserHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6LaserConverterBlockEntity(tLaserHolder[0], TD.Energy.LU, TD.Energy.EU, true, aPos, aState),
				Blocks.STONE).build(null);
		sLaserType = tLaserHolder[0];
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

	private static GTWireBlockEntity laserWire(BlockPos aPos) {
		return sType.create(aPos, block(Family.LASER, MT.NULL, 0, 0).defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the LU carrier (the revived :94-:100 face family)
	// ---------------------------------------------------------------------------

	@Test
	public void laserFamilyCarriesTheLuFace() {
		GTWireBlockEntity tLaser = laserWire(POS);
		assertTrue(tLaser.isLaser(), "the fixture is a laser-family BE");
		// the :94 arm — LU ONLY
		assertTrue(tLaser.isEnergyType(TD.Energy.LU, (byte)0, false), "the laser wire conducts LU (MultiTileEntityWireLaser :94)");
		assertTrue(tLaser.isEnergyType(TD.Energy.LU, (byte)0, true), "the emission face is LU too");
		assertFalse(tLaser.isEnergyType(TD.Energy.EU, (byte)0, false), "the EU exclusion stays exact (the :94 LU-only ruling)");
		assertTrue(tLaser.getEnergyTypes((byte)0).contains(TD.Energy.LU), "the :95 type list is LU");
		assertEquals(1, tLaser.getEnergyTypes((byte)0).size(), "LU only — no second carrier");
		// the :101-106 ratings: the MAX_VALUE carrier over the [0, MAX] band, both directions
		assertEquals(0, tLaser.getEnergySizeInputMin(TD.Energy.LU, (byte)0));
		assertEquals(Long.MAX_VALUE, tLaser.getEnergySizeInputRecommended(TD.Energy.LU, (byte)0));
		assertEquals(Long.MAX_VALUE, tLaser.getEnergySizeInputMax(TD.Energy.LU, (byte)0));
		assertEquals(0, tLaser.getEnergySizeOutputMin(TD.Energy.LU, (byte)0));
		assertEquals(Long.MAX_VALUE, tLaser.getEnergySizeOutputRecommended(TD.Energy.LU, (byte)0));
		assertEquals(Long.MAX_VALUE, tLaser.getEnergySizeOutputMax(TD.Energy.LU, (byte)0));
		// the EU band stays dead on the laser rows
		assertEquals(0, tLaser.getEnergySizeInputMin(TD.Energy.EU, (byte)0));
	}

	@Test
	public void doEnergyInjectionLuArmFloodsAndEuArmStaysDead() {
		GTWireBlockEntity tLaser = laserWire(POS);
		// no connections offline: the wire accepts from CONNECTED sides only (:98
		// canAcceptEnergyFrom = the connected-mask passthrough) — both arms answer 0
		assertEquals(0, tLaser.doEnergyInjection(TD.Energy.LU, (byte)0, 16, 100, true),
				"the real push floods nothing (no connected sides offline)");
		// the EU push stays refused (the :94 gate in front of both arms)
		assertEquals(0, tLaser.doEnergyInjection(TD.Energy.EU, (byte)0, 32, 2, true), "the EU push is refused");
		assertEquals(0, tLaser.doEnergyInjection(TD.Energy.EU, (byte)0, 32, 2, false), "the EU simulate reports nothing taken");
	}

	@Test
	public void canConnectLuProbeIsLive() {
		GTWireBlockEntity tLaser = laserWire(POS);
		// the :89-92 LU probe — the absorber sits NORTH of the wire (side 2), so the probe
		// asks its SOUTH face = the BACK (the beam face, mFacing 2 → back 3)
		GT6LaserConverterBlockEntity tAbsorber = new GT6LaserConverterBlockEntity(
				sLaserType, TD.Energy.LU, TD.Energy.EU, true, new BlockPos(1, 2, 4), Blocks.STONE.defaultBlockState());
		assertTrue(tLaser.canConnect((byte)2, tAbsorber), "the LU acceptor attaches the laser wire (the revived probe)");
		// an ELECTRIC wire neighbour carries no LU face — no attachment (the probe answers
		// the LU question, not the EU one, exactly upstream :89-92)
		GTWireBlockEntity tElectric = sType.create(new BlockPos(1, 2, 4), block(Family.ELECTRIC, MT.Sn, 32, 2).defaultBlockState());
		assertFalse(tLaser.canConnect((byte)2, tElectric), "the EU neighbour must NOT attach the laser wire");
	}

	// ---------------------------------------------------------------------------
	// the flood (upstream :66-86) over a two-segment chain into a real absorber
	// ---------------------------------------------------------------------------

	@Test
	public void theFloodCarriesLuLosslesslyThroughTwoSegmentsIntoTheAbsorber() {
		RevivalLevel tLevel = new RevivalLevel();
		GTWireBlockEntity tWire1 = laserWire(WIRE1);
		GTWireBlockEntity tWire2 = laserWire(WIRE2);
		GT6LaserConverterBlockEntity tAbsorber = new GT6LaserConverterBlockEntity(
				sLaserType, TD.Energy.LU, TD.Energy.EU, true, SINK, Blocks.STONE.defaultBlockState());
		for (BlockEntity tBE : new BlockEntity[] {tWire1, tWire2, tAbsorber}) tBE.setLevel(tLevel);
		tLevel.mBlockEntities.put(WIRE1, tWire1);
		tLevel.mBlockEntities.put(WIRE2, tWire2);
		tLevel.mBlockEntities.put(SINK, tAbsorber);

		// the geometry: WIRE1 --north--> WIRE2 --north--> SINK; the wire-wire joins ride the
		// connector-type intersection (WIRE_LASER on both ends), the wire-absorber join the
		// revived :89-92 LU probe (the absorber's BACK is the face toward the wire)
		assertTrue(tWire1.connect((byte)2, true), "wire1 -> wire2 joins (WIRE_LASER intersection)");
		assertTrue(tWire1.connected((byte)2) && tWire2.connected((byte)3), "the :126 symmetric join");
		assertTrue(tWire2.connect((byte)2, true), "wire2 -> absorber joins (the live LU probe)");

		// the packet: frequency 16 (the T1 laser emission size), one packet, entering from
		// the SOUTH (the laser face — skipped by the :69 loop)
		long tUsed = tWire1.transferLaser((byte)3, 16, 1, -1, new HashSetNoNulls<>(false, tWire1));
		assertEquals(1, tUsed, "the absorber took the packet — one strength unit used");
		// LOSSLESS: the 16 LU crossed two segments undiminished (the mLoss carrier is 0 on
		// the laser family) and landed whole in the absorber's capacitor
		assertEquals(16, tAbsorber.mStorage, "the capacitor holds the full 16 LU (no wire loss)");
		// the :83 ledger: |frequency × used| booked on BOTH wires
		assertEquals(16, tWire1.mTransferred, "wire1 books |16 × 1|");
		assertEquals(16, tWire2.mTransferred, "wire2 books |16 × 1|");

		// the absorber converts its 16 LU → 8 EU on the next tick (the offline seam)
		AssertingSink tEuSink = new AssertingSink();
		tAbsorber.setAdjacencyOverrideForTest(aSide -> {
			if (aSide != 2) return null;
			return new EnergyTarget(tEuSink, (byte)3);
		});
		tAbsorber.onTick(100, true);
		assertEquals(8, tEuSink.totalMass, "8 EU × 1 packet emitted — the half-rate conversion of the chain tail (units(16, 32, 16))");
		assertEquals(0, tAbsorber.mStorage, "WASTE_ENERGY=T: the absorber vented its bucket");
	}

	/** A minimal EU counting sink for the chain tail (the typed sink's one-arm form). */
	static class AssertingSink implements gregapi.tileentity.energy.ITileEntityEnergy {
		long totalMass = 0;

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == TD.Energy.EU;
		}

		@Override
		public java.util.Collection<TagData> getEnergyTypes(byte aSide) {
			return java.util.List.of(TD.Energy.EU);
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return isEnergyType(aEnergyType, aSide, false);
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return false;
		}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (!isEnergyAcceptingFrom(aEnergyType, aSide, false)) return 0;
			if (aDoInject) totalMass += aAmount * Math.abs(aSize);
			return aAmount;
		}

		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
			return 0;
		}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
			return 0;
		}

		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {
			return 0;
		}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
			return 1;
		}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
			return 0;
		}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
			return Long.MAX_VALUE;
		}

		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
			return 0;
		}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
			return 0;
		}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
			return 0;
		}
	}

	/** Map-backed Level double (the GTWireStaleMaskTest.WireLevel form). */
	public static class RevivalLevel extends gregtech6.recipes.GTRecipesOfflineTestBase.MinimalLevel {
		final Map<BlockPos, BlockEntity> mBlockEntities = new HashMap<>();
		final Map<BlockPos, BlockState> mStates = new HashMap<>();

		public RevivalLevel() {
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

		@Override
		public boolean isLoaded(BlockPos aPos) {
			return true;
		}
	}

	// ---------------------------------------------------------------------------
	// the carried-over p10 posture: no burn, no shock
	// ---------------------------------------------------------------------------

	@Test
	public void laserFamilyNeverBurns() {
		GTWireBlockEntity tLaser = laserWire(POS);
		// the tick lags the transfer window (the revived :57-64) and never strikes
		tLaser.mTransferred = 40;
		tLaser.onTick(1, true);
		assertEquals(40, tLaser.mTransferredLast, "the revived window lag books the previous window");
		assertEquals(0, tLaser.mTransferred, "and clears the current window");
		tLaser.onTick(513, true);
		assertEquals(0, tLaser.mBurnCounter, "no overload strikes can accumulate — 16 would set the wire on fire");
		assertEquals(0, tLaser.mWattageLast, "no wattage window ever opens (the laser family mounts no burn)");
	}

	@Test
	public void laserFamilyNeverShocks() {
		GTWireBlock tLaserBlock = block(Family.LASER, MT.NULL, 0, 0);
		assertFalse(tLaserBlock.contactDamage(), "NBT_CONTACTDAMAGE F (Loader:1815) — no entityInside hook, no 2px inset");
		GTWireBlockEntity tLaser = laserWire(POS);
		assertEquals(0, tLaser.pendingContactDamage(), "tierMax(0) * 4 = 0 — the :203 body is a no-op on the laser family");
	}

	// ---------------------------------------------------------------------------
	// the family wiring (the laser identities resolve through the shared carrier)
	// ---------------------------------------------------------------------------

	@Test
	public void laserFamilyWiringResolves() {
		GTWireBlockEntity tLaser = laserWire(POS);
		assertTrue(tLaser.isLaser());
		assertFalse(tLaser.isRedstone(), "the laser family is NOT the redstone family — the BFS machinery stays off");
		assertTrue(tLaser.getConnectorTypes((byte)0).contains(TD.Connectors.WIRE_LASER),
				"MultiTileEntityWireLaser :124 — laser-to-laser chains form, electric chains do not");
		assertFalse(tLaser.getConnectorTypes((byte)0).contains(TD.Connectors.WIRE_ELECTRIC),
				"the WIRE_LASER set never intersects WIRE_ELECTRIC (upstream :124 vs :243)");
		assertEquals("wire_laser", tLaser.getTileEntityName(), "the BET registry path twin (GTWires.WIRE_LASER_BE)");
	}
}
