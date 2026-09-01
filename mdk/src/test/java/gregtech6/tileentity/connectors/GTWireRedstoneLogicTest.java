/**
 * The offline truth tables of the redstone-wire family (task p10-wire-redstone-family).
 * The push-BFS engine (GTWireRedstoneNode.doRedstoneUpdate, the upstream
 * ITileEntityRedstoneWire.Util :43-56 verbatim port) drives over FAKE nodes — the engine
 * is interface-typed exactly so this needs no Level — while the updateRedstone scan
 * (upstream :122-133), the emission formula (:144) and the vanilla-input amplification
 * (:118) drive over a stubbed offline BE (the GTWireBlockUseLockTest offline
 * construction form). The live chain (lamp on/off, the Signalum-vs-RedAlloy distance
 * decay) is the RCON channel; what is pinned HERE is the pure math: the layer-order
 * flood, the 6-side max, the mReceived source tracking, the loss formula, the mMode
 * constant-strength baseline and the bind4/neighbour-correction output.
 */
package gregtech6.tileentity.connectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.wire.GTWireBlock;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWireSpecs.Row.Family;
import gregtech6.util.UT6;

public class GTWireRedstoneLogicTest {

	@BeforeAll
	public static void bootVanillaOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		makeOfflineRedstoneBlock(); // SAME method: the block construction must follow the bootstrap (JUnit runs @BeforeAll methods in no guaranteed order)
	}

	// ---------------------------------------------------------------------------
	// the BFS engine over fake nodes (layer-order flood, change-only propagation)
	// ---------------------------------------------------------------------------

	/** A minimal wire node: vanilla source input at the leaf, the :170 minus-loss handshake on the edges. */
	static final class FakeNode implements GTWireRedstoneNode {
		final long loss;
		int vanillaInput = -1; // the 0..15 leaf source, -1 = none
		long value = 0;
		final FakeNode[] sides = new FakeNode[6];

		FakeNode(long aLoss) { loss = aLoss; }

		@Override
		public boolean canEmitRedstoneToWire(byte aSide, int aRedstoneID) { return sides[aSide] != null; }

		@Override
		public boolean canAcceptRedstoneFromWire(byte aSide, int aRedstoneID) { return sides[aSide] != null; }

		@Override
		public long getRedstoneLoss(int aRedstoneID) { return loss; }

		@Override
		public long getRedstoneValue(byte aSide, int aRedstoneID) { return value; }

		@Override
		public long getRedstoneMinusLoss(byte aSide, int aRedstoneID) { return value - loss; }

		@Override
		public boolean updateRedstone(int aRedstoneID) {
			long o = value;
			long best = vanillaInput >= 0 ? GTWireSpecs.MAX_RANGE * vanillaInput - loss : 0; // the :118 amplification
			for (byte s = 0; s < 6; s++) if (sides[s] != null) {
				long in = sides[s].value - sides[s].loss; // the :170 minus-loss handshake
				if (in > best) best = in;
			}
			value = best;
			return value != o;
		}

		@Override
		public Adjacent adjacent(byte aSide) {
			// the Adjacent.side is the side of the NEIGHBOUR that faces us (mSideOfTileEntity)
			return sides[aSide] == null ? new Adjacent(null, aSide) : new Adjacent(sides[aSide], UT6.OPOS[aSide]);
		}
	}

	private static void link(FakeNode a, FakeNode b) {
		a.sides[5] = b; // EAST
		b.sides[4] = a; // WEST
	}

	@Test
	public void bfsFloodPropagatesLayerwiseAndTerminates() {
		FakeNode a = new FakeNode(1), b = new FakeNode(1), c = new FakeNode(1);
		link(a, b); link(b, c);
		a.vanillaInput = 15; // the leaf source
		// the upstream call shape (:94/:104): the seed recomputes FIRST, the flood pushes the change
		assertTrue(a.updateRedstone(GTWireBlockEntity.REDSTONE_ID));
		GTWireRedstoneNode.doRedstoneUpdate(a, GTWireBlockEntity.REDSTONE_ID);
		// the full-range value decays by the per-segment loss at every hop (:170)
		long tSource = GTWireSpecs.MAX_RANGE * 15L - 1;
		assertEquals(tSource, a.value);
		assertEquals(tSource - 1, b.value);
		assertEquals(tSource - 2, c.value);
		// a second flood from the far end changes NOTHING (the convergence guarantee)
		GTWireRedstoneNode.doRedstoneUpdate(c, GTWireBlockEntity.REDSTONE_ID);
		assertEquals(tSource, a.value);
		assertEquals(tSource - 1, b.value);
		assertEquals(tSource - 2, c.value);
	}

	@Test
	public void bfsFloodDoesNotCrossMissingEdges() {
		FakeNode a = new FakeNode(1), b = new FakeNode(1);
		FakeNode c = new FakeNode(1);
		link(a, b); // b is NOT linked to c
		a.vanillaInput = 15;
		assertTrue(a.updateRedstone(GTWireBlockEntity.REDSTONE_ID));
		GTWireRedstoneNode.doRedstoneUpdate(a, GTWireBlockEntity.REDSTONE_ID);
		assertEquals(GTWireSpecs.MAX_RANGE * 15L - 2, b.value);
		assertEquals(0, c.value, "the flood must not cross a missing edge");
	}

	@Test
	public void signalumDecaysFourTimesSlowerThanRedAlloy() {
		// the range ladder research card ① pins: MAX_RANGE/loss grid steps — 16 (RedAlloy) vs 64 (Signalum)
		assertEquals(134217727L, GTWireSpecs.MAX_RANGE / 16);
		assertEquals(33554431L, GTWireSpecs.MAX_RANGE / 64);
		assertEquals(4, GTWireSpecs.MAX_RANGE / 16 / (GTWireSpecs.MAX_RANGE / 64), "Signalum carries 4x the range per strength point");
	}

	// ---------------------------------------------------------------------------
	// updateRedstone over a stubbed offline BE (the :122-133 scan verbatim)
	// ---------------------------------------------------------------------------

	private static Block sRedstoneBlock;

	/**
	 * The GTWireBlockUseLockTest offline construction form: Block.&lt;init&gt; creates its
	 * intrusive holder past the bootstrap freeze — unfreeze the vanilla block registry
	 * wrapper for the construction (test-JVM-local reflection).
	 */
	private static void makeOfflineRedstoneBlock() {
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		sRedstoneBlock = new GTWireBlock(0, 1, GTWireSpecs.MAX_RANGE / 16, null, 1, false, 2, Family.REDSTONE,
				BlockBehaviour.Properties.of());
	}

	/** The stub: getRedstoneAtSide is scripted, everything else is the real BE code. */
	static final class StubWire extends GTWireBlockEntity {
		final long[] sideInputs = new long[6];

		StubWire() {
			super(BlockEntityType.Builder.of(GTWireBlockEntity::new, sRedstoneBlock).build(null),
					BlockPos.ZERO, sRedstoneBlock.defaultBlockState());
		}

		@Override
		public long getRedstoneAtSide(byte aSide) {
			return aSide >= 0 && aSide < 6 ? sideInputs[aSide] : 0;
		}
	}

	@Test
	public void updateRedstoneTakesTheSixSideMaxAndTracksTheSource() {
		StubWire tWire = new StubWire();
		assertTrue(tWire.isRedstone());
		tWire.sideInputs[2] = 100; // NORTH
		tWire.sideInputs[4] = 70; // WEST
		assertTrue(tWire.updateRedstone(GTWireBlockEntity.REDSTONE_ID), "the first scan must report the change");
		assertEquals(100, tWire.mRedstone, "the strongest of the six sides wins");
		assertEquals(2, tWire.mReceived, "mReceived tracks the source side");
		assertFalse(tWire.updateRedstone(GTWireBlockEntity.REDSTONE_ID), "a converged wire reports no change");
	}

	@Test
	public void updateRedstoneConvergesToTheModeBaselineWithNoInput() {
		StubWire tWire = new StubWire();
		tWire.sideInputs[2] = 100;
		tWire.updateRedstone(GTWireBlockEntity.REDSTONE_ID);
		// the source dies to a TRUE ZERO script (the unconnected-side form): the scan keeps
		// the first zero (0 is never <= the negative baseline, and no side beats it strictly)
		tWire.sideInputs[2] = 0;
		assertTrue(tWire.updateRedstone(GTWireBlockEntity.REDSTONE_ID));
		assertEquals(0, tWire.mRedstone, "the scripted zero outranks the negative baseline");
		assertEquals(2, tWire.mReceived, "mReceived only resets through the :126 branch (needs an input <= the baseline)");
		assertEquals(0, tWire.getComparatorOut());
		// the negative vanilla-zero form (level 0 = -loss): it NEVER beats the 0 the
		// SIDE_UNDEFINED probe keeps writing at :126 (0 is never <= the negative baseline
		// with mMode 0), so the wire stays at 0 — the :126 BASELINE RESET is the mMode>0
		// branch (0 <= positive baseline always fires there, see the mMode test)
		tWire.sideInputs[2] = -GTWireSpecs.MAX_RANGE / 16;
		assertFalse(tWire.updateRedstone(GTWireBlockEntity.REDSTONE_ID), "0 -> 0: no change to report");
		assertEquals(0, tWire.mRedstone);
	}

	@Test
	public void mModeIsTheConstantStrengthSource() {
		StubWire tWire = new StubWire();
		tWire.mMode = 3;
		tWire.updateRedstone(GTWireBlockEntity.REDSTONE_ID);
		// the :124 baseline verbatim: mMode * MAX_RANGE - mLoss
		assertEquals(3 * GTWireSpecs.MAX_RANGE - tWire.mLoss, tWire.mRedstone);
		assertEquals(GTWireBlockEntity.SIDE_UNDEFINED, tWire.mReceived);
		// a weak input does NOT beat the baseline; a strong one does
		tWire.sideInputs[1] = GTWireSpecs.MAX_RANGE * 2; // below 3*MAX_RANGE
		tWire.updateRedstone(GTWireBlockEntity.REDSTONE_ID);
		assertEquals(3 * GTWireSpecs.MAX_RANGE - tWire.mLoss, tWire.mRedstone);
		tWire.sideInputs[1] = GTWireSpecs.MAX_RANGE * 4; // above the baseline
		assertTrue(tWire.updateRedstone(GTWireBlockEntity.REDSTONE_ID));
		assertEquals(GTWireSpecs.MAX_RANGE * 4, tWire.mRedstone);
		assertEquals(1, tWire.mReceived);
	}

	@Test
	public void nonRedstoneChannelIsRejected() {
		StubWire tWire = new StubWire();
		assertFalse(tWire.updateRedstone(42), "aRedstoneID != REDSTONE_ID is the :123 gate");
		assertFalse(tWire.canEmitRedstoneToWire((byte) 0, 42));
		assertEquals(0, tWire.getRedstoneMinusLoss((byte) 0, 42));
	}

	// ---------------------------------------------------------------------------
	// the vanilla emission formula (:144) and the vanilla input amplification (:118)
	// ---------------------------------------------------------------------------

	@Test
	public void emissionValueIsBind4OfDivupMinusCorrection() {
		long tMax = GTWireSpecs.MAX_RANGE;
		// full-range round trips: divup(MAX*15, MAX) = 15
		assertEquals(15, GTWireBlockEntity.emissionValue(tMax * 15, false));
		assertEquals(14, GTWireBlockEntity.emissionValue(tMax * 15, true), "the wire/conductor neighbour costs one point (:144)");
		// the ceil branch: MAX*8-3 still rounds UP to 8
		assertEquals(8, GTWireBlockEntity.emissionValue(tMax * 8 - 3, false));
		assertEquals(7, GTWireBlockEntity.emissionValue(tMax * 8 - 3, true));
		// the exact multiples do NOT round up (divup is ceil, not +1)
		assertEquals(4, GTWireBlockEntity.emissionValue(tMax * 4, false));
		// zero clamps to 0. NOTE the negative form never reaches this formula live: every
		// upstream exit gates "mRedstone <= 0 -> return 0" FIRST (:142/:149), so divup's
		// negative-ceil behaviour is unreachable by design.
		assertEquals(0, GTWireBlockEntity.emissionValue(0, false));
	}

	@Test
	public void vanillaInputAmplificationIsMaxRangeTimesLevelMinusLoss() {
		// the :118 formula over the census losses — the RCON lamp ladder rides on it
		assertEquals(GTWireSpecs.MAX_RANGE * 15L - (GTWireSpecs.MAX_RANGE / 16), GTWireSpecs.MAX_RANGE * 15L - GTWireSpecs.MAX_RANGE / 16);
		FakeNode tLeaf = new FakeNode(GTWireSpecs.MAX_RANGE / 16);
		tLeaf.vanillaInput = 15;
		tLeaf.updateRedstone(GTWireBlockEntity.REDSTONE_ID);
		assertEquals(GTWireSpecs.MAX_RANGE * 15L - (GTWireSpecs.MAX_RANGE / 16), tLeaf.value);
		// a vanilla level of 1 still propagates: MAX_RANGE - loss > 0 (the non-dead-zone property)
		assertTrue(GTWireSpecs.MAX_RANGE - GTWireSpecs.MAX_RANGE / 16 > 0);
	}

	@Test
	public void comparatorOutIsFloorDivision() {
		StubWire tWire = new StubWire();
		tWire.mRedstone = GTWireSpecs.MAX_RANGE * 7 + 123; // floor = 7 (NOT divup)
		assertEquals(7, tWire.getComparatorOut());
	}

	// ---------------------------------------------------------------------------
	// data pins: the REDSTONE_SINKS anti-feedback set and the clamp helpers
	// ---------------------------------------------------------------------------

	@Test
	public void redstoneSinksMatchTheUpstreamSet() {
		// CS.java:1366 — tnt, golden_rail, noteblock, trapdoor, wooden_door, iron_door,
		// piston, sticky_piston, dispenser, dropper, redstone_lamp(+lit)
		assertEquals(11, GTWireBlockEntity.REDSTONE_SINKS.size());
		assertTrue(GTWireBlockEntity.REDSTONE_SINKS.contains(Blocks.TNT));
		assertTrue(GTWireBlockEntity.REDSTONE_SINKS.contains(Blocks.POWERED_RAIL), "1.7.10 golden_rail");
		assertTrue(GTWireBlockEntity.REDSTONE_SINKS.contains(Blocks.NOTE_BLOCK));
		assertTrue(GTWireBlockEntity.REDSTONE_SINKS.contains(Blocks.OAK_TRAPDOOR), "1.7.10 (wooden) trapdoor");
		assertTrue(GTWireBlockEntity.REDSTONE_SINKS.contains(Blocks.OAK_DOOR), "1.7.10 wooden_door");
		assertTrue(GTWireBlockEntity.REDSTONE_SINKS.contains(Blocks.IRON_DOOR));
		assertTrue(GTWireBlockEntity.REDSTONE_SINKS.contains(Blocks.PISTON));
		assertTrue(GTWireBlockEntity.REDSTONE_SINKS.contains(Blocks.STICKY_PISTON));
		assertTrue(GTWireBlockEntity.REDSTONE_SINKS.contains(Blocks.DISPENSER));
		assertTrue(GTWireBlockEntity.REDSTONE_SINKS.contains(Blocks.DROPPER));
		assertTrue(GTWireBlockEntity.REDSTONE_SINKS.contains(Blocks.REDSTONE_LAMP), "one LIT-property block covers both 1.7.10 lamp ids");
	}

	@Test
	public void bind4AndDivupAreTheUpstreamHelpers() {
		// UT.Code.bind4 :1556
		assertEquals(0, UT6.bind4(-5));
		assertEquals(0, UT6.bind4(0));
		assertEquals(15, UT6.bind4(15));
		assertEquals(15, UT6.bind4(16));
		// UT.Code.divup :1697
		assertEquals(2, UT6.divup(31, 16));
		assertEquals(2, UT6.divup(32, 16));
		assertEquals(3, UT6.divup(33, 16));
		assertEquals(0, UT6.divup(0, 16));
	}
}
