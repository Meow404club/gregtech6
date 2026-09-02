package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GT6Kinetics;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GTAxleBlockEntity offline tests (task p12-axle-family acceptance a): the adjacency
 * recursion with zero loss (the fake BE chain source→axle→axle→sink, the packet arriving
 * UNTOUCHED), the idle gate (oRotationDir == 0 books the spin-up tick and forwards
 * nothing, upstream :112), the overspeed/bandwidth pop-off truth table (the break flag +
 * the ORIGINAL power returned to the caller, upstream :125-129; a dead face returns 0 =
 * the caller's packet unconsumed, upstream :114), the canConnect predicate (the neighbor
 * must accept OR emit RU — EU/KU/plain BEs refuse, upstream :134-137), the AXIS face truth
 * table (forward along the axis, perpendicular faces refuse), the loop kill, the size
 * band, the NBT round trip, and the Loader kinetic-section spec table (GT6Kinetics).
 *
 * <p>Offline harness (the crank/in-case records carry over): level-less fixtures on a
 * vanilla STONE state, the {@code mAdjacencyOverride} seam wires the chain by hand, the
 * AXIS mirror is driven through a vanilla OAK_LOG state (the SAME
 * {@code BlockStateProperties.AXIS} instance the axle block registers), and the deferred
 * pop-off is asserted at the flag level ({@code level.destroyBlock} needs a live level —
 * the physical break is the RCON chain's assertion).
 */
public class GTAxleBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GTAxleBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GTAxleBlockEntity>[] tHolder = (BlockEntityType<GTAxleBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTAxleBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** A fresh level-less axle fixture with the default ratings (upstream :56: speed 32, power 1). */
	private static GTAxleBlockEntity axle() {
		return new GTAxleBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	/**
	 * Counting RU sink — the shredder-shaped fake consumer (the GTCrankBlockEntityTest
	 * form): accepts the parameterised carrier from every side, refuses everything else.
	 */
	public static class CountingSink extends BlockEntity implements ITileEntityEnergy {
		public long calls = 0, lastSize = 1, lastAmount = 0;
		public byte lastSide = -1;
		public final TagData acceptedType;

		public CountingSink(BlockPos aPos, TagData aAcceptedType) {
			super(null, aPos, Blocks.STONE.defaultBlockState());
			acceptedType = aAcceptedType;
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == acceptedType;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == acceptedType;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {return acceptedType.AS_LIST;}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) {
					calls++;
					lastSize = aSize;
					lastAmount = aAmount;
					lastSide = aSide;
				}
				return aAmount;
			}
			return 0;
		}

		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	/** A pure RU/KU/EU emitter fake — the canConnect "or emits" arm (upstream :135). */
	public static class EmittingSource extends BlockEntity implements ITileEntityEnergy {
		public final TagData emittedType;

		public EmittingSource(BlockPos aPos, TagData aEmittedType) {
			super(null, aPos, Blocks.STONE.defaultBlockState());
			emittedType = aEmittedType;
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return aEmitting && aEnergyType == emittedType;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == emittedType;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {return emittedType.AS_LIST;}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {return 0;}

		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	/**
	 * The source→axleA→axleB→sink chain, all on the Z axis (the packet enters on side 2,
	 * NORTH, and walks SOUTH): A forwards to B on its side 3, B forwards to the sink on
	 * its side 3. Returns the sink for assertions.
	 */
	private static CountingSink wireChain(GTAxleBlockEntity aA, GTAxleBlockEntity aB) {
		aA.mAxis = Direction.Axis.Z;
		aB.mAxis = Direction.Axis.Z;
		CountingSink tSink = new CountingSink(POS.south().south(), TD.Energy.RU);
		aA.setAdjacencyOverride(aSide -> aSide == 3 ? new EnergyTarget(aB, (byte) 2) : null);
		aB.setAdjacencyOverride(aSide -> aSide == 3 ? new EnergyTarget(tSink, (byte) 2) : null);
		return tSink;
	}

	@Test
	public void recursiveChainZeroLoss() {
		GTAxleBlockEntity tA = axle(), tB = axle();
		CountingSink tSink = wireChain(tA, tB);
		// both axles through their first tick (the upstream :106 mTimer < 1 guard) —
		// oRotationDir is still 0, the idle gate is armed
		tA.updateEntity();
		tB.updateEntity();

		// Tick 1 — BOTH axles idle-gate (oRotationDir == 0): the spin-up books the packet,
		// nothing leaves, the caller is told "consumed" (upstream :112 returns aPower)
		long tUsed = tA.doEnergyInjection(TD.Energy.RU, (byte) 2, -16, 1, true);
		assertEquals(1, tUsed);
		assertEquals(0, tSink.calls, "the idle gate must not forward on the spin-up tick");
		// advance both axles one tick (the :101 oRotationDir = mRotationDir sync)
		tA.updateEntity();
		tB.updateEntity();

		// Tick 2 — A forwards, B still idle-gates (its first packet): the packet dies at B
		tUsed = tA.doEnergyInjection(TD.Energy.RU, (byte) 2, -16, 1, true);
		assertEquals(1, tUsed);
		assertEquals(0, tSink.calls, "B's own spin-up tick must not forward");
		tA.updateEntity(); // clear A's tick bookkeeping too (the :96 zeroing)
		tB.updateEntity();

		// Tick 3 — the full chain: the packet arrives UNTOUCHED (zero loss, upstream :159)
		tUsed = tA.doEnergyInjection(TD.Energy.RU, (byte) 2, -16, 1, true);
		assertEquals(1, tUsed);
		assertEquals(1, tSink.calls);
		assertEquals(-16, tSink.lastSize, "zero loss: the size passes through untouched");
		assertEquals(1, tSink.lastAmount, "zero loss: the amount passes through untouched");
		assertEquals(2, tSink.lastSide, "the sink is hit on its own north face");

		// and the readout bookkeeping saw the walk (the tachometer channel)
		assertEquals(16, tA.mTransferredEnergy);
		assertEquals(16, tB.mTransferredEnergy);
	}

	@Test
	public void overspeedPopOffTruthTable() {
		GTAxleBlockEntity tAxle = axle(); // the default rating: mSpeed 32 (upstream :56)

		// |aSpeed| <= mSpeed: no break, the downstream consumption is what comes back
		assertEquals(1, tAxle.addToEnergyTransferred(-16, 1, 1));
		assertFalse(tAxle.mBreakPending);

		// |aSpeed| > mSpeed: the break flag AND the ORIGINAL power returned to the caller
		// (upstream :125-128 — the "original amount" form; the physical destroyBlock is the
		// RCON chain's assertion, the flag level is the offline maximum)
		assertEquals(4, tAxle.addToEnergyTransferred(64, 4, 4));
		assertTrue(tAxle.mBreakPending, "64 > 32 must trip the break");

		// the deferred break consumes the flag on the axle's own next tick (no level
		// offline: popOffNow degrades to the flag flip — the live destroy is the RCON arm)
		tAxle.updateEntity();
		assertFalse(tAxle.mBreakPending, "the deferred pop-off must consume its flag");

		// idle rotation can spin at ludicrous speeds WITHOUT breaking (upstream :123-124):
		// the check lives in addToEnergyTransferred, so ONLY a packet booking can trip it —
		// the spin-up booking (upstream :112) runs the same check (the idle axle with no
		// packets NEVER enters the method, which is the upstream comment's point)
		GTAxleBlockEntity tIdle = axle();
		tIdle.mAxis = Direction.Axis.Z; // the injection side (2 = NORTH) rides the Z faces
		tIdle.updateEntity(); // the :106 mTimer guard
		assertEquals(1, tIdle.doEnergyInjection(TD.Energy.RU, (byte) 2, 1048576, 1, true)); // the :112 aPower form
		assertTrue(tIdle.mBreakPending, "even the spin-up booking checks |aSpeed| > mSpeed");
	}

	@Test
	public void bandwidthExcessBreaksAndRefuses() {
		GTAxleBlockEntity tAxle = axle(); // mPower = 1 (upstream :56)
		CountingSink tSink = new CountingSink(POS.south(), TD.Energy.RU);
		tAxle.mAxis = Direction.Axis.Z;
		tAxle.setAdjacencyOverride(aSide -> aSide == 3 ? new EnergyTarget(tSink, (byte) 2) : null);
		tAxle.updateEntity(); // the :106 mTimer guard
		tAxle.oRotationDir = 1; // past the idle gate — the real transfer path

		// packet 1 of amount 1: within the bandwidth — the sink consumes it
		assertEquals(1, tAxle.doEnergyInjection(TD.Energy.RU, (byte) 2, -16, 1, true));
		assertEquals(1, tSink.calls);
		assertFalse(tAxle.mBreakPending);

		// packet 2 in the SAME tick: mTransferredPower 1+1 = 2 > mPower 1 → break + the
		// original amount returned (upstream :125's mTransferredPower > mPower arm)
		assertEquals(1, tAxle.doEnergyInjection(TD.Energy.RU, (byte) 2, -16, 1, true));
		assertTrue(tAxle.mBreakPending, "the second packet must trip the bandwidth break");
	}

	@Test
	public void deadFaceReturnsUnconsumed() {
		GTAxleBlockEntity tAxle = axle();
		tAxle.mAxis = Direction.Axis.Z;
		tAxle.updateEntity(); // the :106 mTimer guard
		tAxle.oRotationDir = 1; // past the idle gate — the real transfer path

		// NO neighbor reachable on the exit face (no adjacency override): the face books
		// zero (upstream :114) and the caller's packet comes back UNCONSUMED (0)
		assertEquals(0, tAxle.doEnergyInjection(TD.Energy.RU, (byte) 2, -16, 1, true));
		assertFalse(tAxle.mBreakPending);

		// a non-axle NON-energy neighbor (a plain BE): Util hands it to the bridge seam,
		// nobody consumes → 0 back to the caller (the same unconsumed semantics)
		BlockEntity tPlain = new BlockEntity(null, POS.south(), Blocks.STONE.defaultBlockState()) {};
		tAxle.setAdjacencyOverride(aSide -> aSide == 3 ? new EnergyTarget(tPlain, (byte) 2) : null);
		assertEquals(0, tAxle.doEnergyInjection(TD.Energy.RU, (byte) 2, -16, 1, true));
	}

	@Test
	public void canConnectPredicate() {
		GTAxleBlockEntity tAxle = axle();
		// an RU receiver OR an RU emitter both connect (upstream :135, the theoretical probe)
		assertTrue(tAxle.canConnect((byte) 2, new CountingSink(POS.south(), TD.Energy.RU)));
		assertTrue(tAxle.canConnect((byte) 2, new EmittingSource(POS.south(), TD.Energy.RU)));
		// EU/KU receivers refuse — RU has no cross-carrier connectivity
		assertFalse(tAxle.canConnect((byte) 2, new CountingSink(POS.south(), TD.Energy.EU)));
		assertFalse(tAxle.canConnect((byte) 2, new CountingSink(POS.south(), TD.Energy.KU)));
		// a plain (non-energy) BE and null refuse (upstream :136)
		assertFalse(tAxle.canConnect((byte) 2, new BlockEntity(null, POS.south(), Blocks.STONE.defaultBlockState()) {}));
		assertFalse(tAxle.canConnect((byte) 2, null));

		// and the axle's own face family only ever speaks RU (upstream :139/:140)
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertFalse(tAxle.isEnergyType(TD.Energy.EU, tSide, true));
			assertFalse(tAxle.isEnergyType(TD.Energy.KU, tSide, false));
			assertTrue(tAxle.isEnergyType(TD.Energy.RU, tSide, true));
			assertEquals(1, tAxle.getEnergyTypes(tSide).size());
		}
	}

	@Test
	public void axisFaceTruthTable() {
		GTAxleBlockEntity tAxle = axle();
		tAxle.updateEntity(); // the :106 mTimer guard (the on-axis injection arms return 1)
		tAxle.mAxis = Direction.Axis.X;
		// X faces (WEST 4 / EAST 5) carry, everything perpendicular refuses — both probes
		for (byte tSide = 0; tSide < 6; tSide++) {
			boolean tOnAxis = tSide >= 4;
			assertEquals(tOnAxis, tAxle.connected(tSide), "connected @" + tSide);
			assertEquals(tOnAxis, tAxle.isEnergyAcceptingFrom(TD.Energy.RU, tSide, false), "accepting @" + tSide);
			assertEquals(tOnAxis, tAxle.isEnergyEmittingTo(TD.Energy.RU, tSide, false), "emitting @" + tSide);
			// the injection door: perpendicular faces refuse BEFORE any transfer
			assertEquals(tOnAxis ? 1 : 0, tAxle.doEnergyInjection(TD.Energy.RU, tSide, -16, 1, true));
		}
		// Y faces too (the vanilla-log three-axis symmetry)
		tAxle.mAxis = Direction.Axis.Y;
		assertTrue(tAxle.connected((byte) 0));
		assertTrue(tAxle.connected((byte) 1));
		assertFalse(tAxle.connected((byte) 2));
		assertFalse(tAxle.connected((byte) 5));

		// the mirror re-syncs from the state at the tick head (the OAK_LOG state carries the
		// SAME BlockStateProperties.AXIS instance — no port block offline)
		BlockState tLog = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		GTAxleBlockEntity tMirrored = new GTAxleBlockEntity(sType, POS, tLog);
		assertEquals(Direction.Axis.X, tMirrored.mAxis); // the default before the first tick
		tMirrored.updateEntity();
		assertEquals(Direction.Axis.Y, tMirrored.mAxis);
		assertTrue(tMirrored.connected((byte) 0));
		assertFalse(tMirrored.connected((byte) 4));
		// a state WITHOUT the property (the STONE fixture) leaves the mirror alone
		GTAxleBlockEntity tPlain = axle();
		tPlain.mAxis = Direction.Axis.Z;
		tPlain.updateEntity();
		assertEquals(Direction.Axis.Z, tPlain.mAxis);
	}

	@Test
	public void loopKillTerminates() {
		GTAxleBlockEntity tA = axle(), tB = axle();
		tA.mAxis = Direction.Axis.Z;
		tB.mAxis = Direction.Axis.Z;
		// a two-axle ring: A's exit hands to B, B's exit hands BACK to A
		tA.setAdjacencyOverride(aSide -> aSide == 3 ? new EnergyTarget(tB, (byte) 2) : null);
		tB.setAdjacencyOverride(aSide -> aSide == 3 ? new EnergyTarget(tA, (byte) 2) : null);
		tA.updateEntity();
		tB.updateEntity();
		tA.oRotationDir = 1; // past the idle gate — the real transfer path
		tB.oRotationDir = 1;

		// the walk terminates (the aAlreadyPassed set kills the loop, upstream :116) and
		// the ring consumes nothing — 0 back to the caller
		long tUsed = tA.doEnergyInjection(TD.Energy.RU, (byte) 2, -16, 1, true);
		assertEquals(0, tUsed, "the ring must terminate unconsumed");
	}

	@Test
	public void sizeBandMirrorsRating() {
		GTAxleBlockEntity tAxle = axle(); // mSpeed 32
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertEquals(0, tAxle.getEnergySizeInputMin(TD.Energy.RU, tSide));
			assertEquals(32, tAxle.getEnergySizeInputRecommended(TD.Energy.RU, tSide));
			assertEquals(32, tAxle.getEnergySizeInputMax(TD.Energy.RU, tSide));
			assertEquals(0, tAxle.getEnergySizeOutputMin(TD.Energy.RU, tSide));
			assertEquals(32, tAxle.getEnergySizeOutputRecommended(TD.Energy.RU, tSide));
			assertEquals(32, tAxle.getEnergySizeOutputMax(TD.Energy.RU, tSide));
			// extraction is a dead door (upstream :144)
			assertEquals(0, tAxle.doEnergyExtraction(TD.Energy.RU, tSide, 32, 1, true));
		}
	}

	@Test
	public void nbtRoundTrip() {
		GTAxleBlockEntity tAxle = axle();
		tAxle.mSpeed = 64;
		tAxle.mPower = 4;
		tAxle.mRotationDir = 1;
		CompoundTag tTag = tAxle.saveWithoutMetadata();

		GTAxleBlockEntity tLoaded = axle();
		tLoaded.load(tTag);
		assertEquals(64, tLoaded.mSpeed);
		assertEquals(4, tLoaded.mPower);
		assertEquals(1, tLoaded.mRotationDir);
		assertEquals("axle", tTag.getString("te_name"));
		assertTrue(tTag.contains(GTAxleBlockEntity.NBT_ACTIVE_DATA, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(GTAxleBlockEntity.NBT_PIPESIZE, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(GTAxleBlockEntity.NBT_PIPEBANDWIDTH, Tag.TAG_ANY_NUMERIC));
		// the :63-64 max(1, ...) clamp
		tLoaded.mSpeed = 0;
		CompoundTag tZero = tLoaded.saveWithoutMetadata();
		GTAxleBlockEntity tClamped = axle();
		tClamped.load(tZero);
		assertTrue(tClamped.mSpeed >= 1);
		assertTrue(tClamped.mPower >= 1);
	}

	@Test
	public void specTableMatchesLoader() {
		// 11 materials x 4 diameters = 44 rows (the registration loop itself runs at MOD
		// construct, which the offline JVM never fires — AXLE_BLOCKS stays empty here)
		assertEquals(11, GT6Kinetics.AXLE_SPECS.size());
		assertEquals(4, GT6Kinetics.AXLE_DIAMETERS.length);
		assertEquals(44, GT6Kinetics.AXLE_SPECS.size() * GT6Kinetics.AXLE_DIAMETERS.length);
		// PX_P (CS.java:492): 6/9/12/16
		assertEquals(6, GT6Kinetics.AXLE_DIAMETERS[0]);
		assertEquals(9, GT6Kinetics.AXLE_DIAMETERS[1]);
		assertEquals(12, GT6Kinetics.AXLE_DIAMETERS[2]);
		assertEquals(16, GT6Kinetics.AXLE_DIAMETERS[3]);

		// the speed rating per row = VMAX[tier] (Loader NBT_PIPESIZE, CS.java:150 VMAX)
		assertEquals(16, GT6Kinetics.axleSpeed(GT6Kinetics.AXLE_SPECS.get(0))); // WoodTreated :1663
		assertEquals(64, GT6Kinetics.axleSpeed(GT6Kinetics.AXLE_SPECS.get(1))); // Bronze :1672
		assertEquals(256, GT6Kinetics.axleSpeed(GT6Kinetics.AXLE_SPECS.get(5))); // Steel
		assertEquals(262144, GT6Kinetics.axleSpeed(GT6Kinetics.AXLE_SPECS.get(10))); // Trinitanium

		// the bandwidth ladders verbatim (Loader NBT_PIPEBANDWIDTH)
		assertEquals(16, GT6Kinetics.axleSpeed(GT6Kinetics.AXLE_SPECS.get(0)));
		assertEquals(64, GT6Kinetics.axleSpeed(GT6Kinetics.AXLE_SPECS.get(4))); // the bronze band shares VMAX[1]
		assertArrayEquals(new int[] {1, 2, 4, 8}, GT6Kinetics.AXLE_SPECS.get(0).bandwidth()); // Wood :1663-1666
		assertArrayEquals(new int[] {2, 4, 8, 16}, GT6Kinetics.AXLE_SPECS.get(1).bandwidth()); // Bronze :1672-1675
		assertArrayEquals(new int[] {2, 4, 8, 16}, GT6Kinetics.AXLE_SPECS.get(2).bandwidth()); // Brass
		assertArrayEquals(new int[] {2, 4, 8, 16}, GT6Kinetics.AXLE_SPECS.get(3).bandwidth()); // ArsenicCopper
		assertArrayEquals(new int[] {3, 6, 12, 24}, GT6Kinetics.AXLE_SPECS.get(4).bandwidth()); // ArsenicBronze :1696-1699
		assertArrayEquals(new int[] {4, 8, 16, 32}, GT6Kinetics.AXLE_SPECS.get(5).bandwidth()); // Steel
		assertArrayEquals(new int[] {8, 16, 32, 64}, GT6Kinetics.AXLE_SPECS.get(6).bandwidth()); // Ti
		assertArrayEquals(new int[] {16, 32, 64, 128}, GT6Kinetics.AXLE_SPECS.get(7).bandwidth()); // TungstenSteel
		assertArrayEquals(new int[] {32, 64, 128, 256}, GT6Kinetics.AXLE_SPECS.get(8).bandwidth()); // Ir
		assertArrayEquals(new int[] {64, 128, 256, 512}, GT6Kinetics.AXLE_SPECS.get(9).bandwidth()); // Iritanium
		assertArrayEquals(new int[] {128, 256, 512, 1024}, GT6Kinetics.AXLE_SPECS.get(10).bandwidth()); // Trinitanium

		// the tiers
		assertEquals(0, GT6Kinetics.AXLE_SPECS.get(0).tier());
		assertEquals(1, GT6Kinetics.AXLE_SPECS.get(1).tier());
		assertEquals(7, GT6Kinetics.AXLE_SPECS.get(10).tier());

		// the registry-name and display-name forms
		assertEquals("axle_wood_treated_small", GT6Kinetics.axleName("wood_treated", 0));
		assertEquals("axle_trinitanium_huge", GT6Kinetics.axleName("trinitanium", 3));
		assertEquals("Small Wooden Axle", GT6Kinetics.axleDisplay(GT6Kinetics.AXLE_SPECS.get(0), 0)); // the :1663 wording
		assertEquals("Huge Trinitanium Axle", GT6Kinetics.axleDisplay(GT6Kinetics.AXLE_SPECS.get(10), 3));

		// VMAX itself is the CS table verbatim (the first eight tiers the rows use)
		assertEquals(16, GT6Kinetics.VMAX[0]);
		assertEquals(64, GT6Kinetics.VMAX[1]);
		assertEquals(256, GT6Kinetics.VMAX[2]);
		assertEquals(1024, GT6Kinetics.VMAX[3]);
		assertEquals(4096, GT6Kinetics.VMAX[4]);
		assertEquals(16384, GT6Kinetics.VMAX[5]);
		assertEquals(65536, GT6Kinetics.VMAX[6]);
		assertEquals(262144, GT6Kinetics.VMAX[7]);
	}

	private static void assertArrayEquals(int[] aExpected, int[] aActual) {
		org.junit.jupiter.api.Assertions.assertArrayEquals(aExpected, aActual);
	}
}
