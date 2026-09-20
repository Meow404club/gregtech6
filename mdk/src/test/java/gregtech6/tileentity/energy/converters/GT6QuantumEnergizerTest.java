package gregtech6.tileentity.energy.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.registry.GT6Lasers;
import gregtech6.registry.GT6QuantumEnergizers;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.energy.converters.GT6LaserDomainTest.CountingSink;

/**
 * The Quantum Energizer offline tests (task p32-qu-energizer) — the THIRD type-pair
 * instance (LU→QU, back-in/front-out) of the {@link GT6LaserConverterBlockEntity} family,
 * the {@link GT6LaserDomainTest} harness form. The acceptance arms:
 * <ul>
 * <li>① the registration census: the five rows carry the upstream metaIds 10121-10125,
 *     the (T1..T5) display words and the SHARED laser ladder (Loader :961-966 verbatim);
 *     the FML live half asserts the five blocks + the BET in the BuiltInRegistries when
 *     the JVM booted through FML (the 21.1 unitTest leg — the id686 lesson: a
 *     registration card carries a live registry assertion, the bare forge JVM carries no
 *     gt6 registrations and assumption-skips);</li>
 * <li>② the five-tier ladder, EVERY rung: one in-sized LU packet enters the BACK, one
 *     out-sized QU packet leaves the FRONT (the units() half-rate), the accounting pair
 *     books the whole-packet masses and the WASTE vent clears the bucket;</li>
 * <li>③ the face predicates: BACK-only intake (MultiTileEntityQuantumEnergizerLaser :36),
 *     FRONT-only emission (:37) — byte-for-byte the absorber faces, asserted per rung
 *     tier; the emission type is QU everywhere;</li>
 * <li>④ the cross-domain reject (EU/RU refused on the LU intake — the reference
 *     equality) and the frequency door: getEnergySizeInputMin = in/2 EXACTLY per rung
 *     (the door the RCON chain walks live: the out-sized LU hop rides the next rung's
 *     door exactly);</li>
 * <li>the negative-sign conjunct folds (LU ∉ ALL_NEGATIVE_ALLOWED) and the accounting
 *     pair survives a save/load round trip.</li>
 * </ul>
 */
public class GT6QuantumEnergizerTest extends GTOfflineTestBase {

	static BlockEntityType<GT6LaserConverterBlockEntity> sType;
	static final BlockPos POS = new BlockPos(7, 4, 9);

	static final byte FRONT = 2, BACK = 3; // mFacing default NORTH = FRONT (the emission face)

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		BlockEntityType<GT6LaserConverterBlockEntity>[] tHolder =
				(BlockEntityType<GT6LaserConverterBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6LaserConverterBlockEntity(tHolder[0], TD.Energy.LU, TD.Energy.QU, true, aPos, aState),
				Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** A Quantum Energizer BE at the fixture (LU→QU, back-only intake). */
	private static GT6LaserConverterBlockEntity energizer() {
		return new GT6LaserConverterBlockEntity(sType, TD.Energy.LU, TD.Energy.QU, true, POS, Blocks.STONE.defaultBlockState());
	}

	/** A family BE at an arbitrary ladder row (the tierForOfflineTest core seam). */
	private static GT6LaserConverterBlockEntity energizer(long aInput, long aOutput) {
		GT6LaserConverterBlockEntity tEnergizer = energizer();
		tEnergizer.tierForOfflineTest(aInput, aOutput);
		return tEnergizer;
	}

	// ---------------------------------------------------------------------------
	// ① the registration census + the FML live half (the id686 lesson)
	// ---------------------------------------------------------------------------

	@Test
	public void theEnergizerRowsCarryTheUpstreamColumns() {
		// the metaId ladder (Loader :961-966) and the (T1..T5) display words — NOT voltage
		// words: the upstream name column is "Quantum Energizer (T)"
		assertEquals(10121, GT6QuantumEnergizers.QUANTUM_ENERGIZER_ROWS.get(0).metaId());
		assertEquals(10125, GT6QuantumEnergizers.QUANTUM_ENERGIZER_ROWS.get(4).metaId());
		List<String> tWords = new ArrayList<>();
		for (GT6Lasers.LaserRow tRow : GT6QuantumEnergizers.QUANTUM_ENERGIZER_ROWS) tWords.add(tRow.voltageWord());
		assertEquals(List.of("T1", "T2", "T3", "T4", "T5"), tWords, "the quantum display words ride the row word slot");
		// the ladder IS the shared laser ladder (Loader :962-966 columns verbatim)
		for (int i = 0; i < 5; i++) {
			assertEquals(GT6Lasers.LASER_OUTPUTS[i] * 2, GT6Lasers.LASER_INPUTS[i], "rung " + i + " half-rate");
		}
		// the paths: the family name + the _t2.._t5 tails (the laser family shape)
		assertEquals("quantum_energizer", GT6QuantumEnergizers.QUANTUM_ENERGIZER_ROWS.get(0).path());
		assertEquals("quantum_energizer_t5", GT6QuantumEnergizers.QUANTUM_ENERGIZER_ROWS.get(4).path());
	}

	@Test
	public void theFiveRungsAreLiveRegisteredWhenTheJvmBootedThroughFml() {
		// the FML-boot probe: the bare forge-leg JVM carries ZERO gt6 blocks (the offline
		// boot registers vanilla only), the 21.1 unitTest leg boots through FML and
		// carries the full registration — probe on the p32 laser domain's ground-floor
		// block, then assert the energizer family against the SAME live registry.
		Set<String> tPaths = new HashSet<>();
		for (net.minecraft.resources.ResourceLocation tId : BuiltInRegistries.BLOCK.keySet()) {
			if ("gt6".equals(tId.getNamespace())) tPaths.add(tId.getPath());
		}
		org.junit.jupiter.api.Assumptions.assumeTrue(tPaths.contains("co2_laser"),
				"the JVM booted through FML (the 21.1 unitTest leg) — the bare forge JVM carries no gt6 registrations");
		for (GT6Lasers.LaserRow tRow : GT6QuantumEnergizers.QUANTUM_ENERGIZER_ROWS) {
			assertTrue(tPaths.contains(tRow.path()), "the live BLOCK registry holds gt6:" + tRow.path());
		}
		Set<String> tBetPaths = new HashSet<>();
		for (net.minecraft.resources.ResourceLocation tId : BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet()) {
			if ("gt6".equals(tId.getNamespace())) tBetPaths.add(tId.getPath());
		}
		assertTrue(tBetPaths.contains("quantum_energizer"), "the live BET registry holds gt6:quantum_energizer");
		// the item face rides the same five paths (the BlockItem registration)
		Set<String> tItemPaths = new HashSet<>();
		for (net.minecraft.resources.ResourceLocation tId : BuiltInRegistries.ITEM.keySet()) {
			if ("gt6".equals(tId.getNamespace())) tItemPaths.add(tId.getPath());
		}
		for (GT6Lasers.LaserRow tRow : GT6QuantumEnergizers.QUANTUM_ENERGIZER_ROWS) {
			assertTrue(tItemPaths.contains(tRow.path()), "the live ITEM registry holds gt6:" + tRow.path());
		}
	}

	// ---------------------------------------------------------------------------
	// ② the five-tier ladder (the acceptance ② offline half)
	// ---------------------------------------------------------------------------

	@Test
	public void energizerLadderEveryTierEmitsExactlyTheOutColumn() {
		for (int i = 0; i < GT6Lasers.LASER_INPUTS.length; i++) {
			long tIn = GT6Lasers.LASER_INPUTS[i], tOut = GT6Lasers.LASER_OUTPUTS[i];
			GT6LaserConverterBlockEntity tEnergizer = energizer(tIn, tOut);
			CountingSink tSink = new CountingSink(TD.Energy.QU);
			tSink.recordSize = true;
			tEnergizer.setAdjacencyOverrideForTest(GT6LaserDomainTest.adjacencyAt(tSink, FRONT));
			// one in-sized LU packet enters the BACK face only (the beam face)
			assertEquals(1, tEnergizer.doEnergyInjection(TD.Energy.LU, BACK, tIn, 1, true),
					"energizer rung " + tIn + ": the LU packet is above min " + (tIn / 2) + " and below max " + (tIn * 2));
			tEnergizer.onTick(100, true);
			assertEquals(1, tSink.packets, "energizer rung " + tIn + ": one packet emitted");
			assertEquals(tOut, tSink.lastSize, "energizer rung " + tIn + ": the QU packet SIZE is the out column");
			assertEquals(tOut, tSink.totalMass, "energizer rung " + tIn + ": the emitted mass = the out column");
			assertEquals(tIn, tEnergizer.mLastIn, "energizer rung " + tIn + ": the accounting in = the consumed LU");
			assertEquals(tOut, tEnergizer.mLastOut, "energizer rung " + tIn + ": the accounting out = the emitted QU");
			assertEquals(0, tEnergizer.mStorage, "WASTE_ENERGY=T: the vent emptied the bucket");
			assertTrue(tEnergizer.mActive, "the emission fired");
		}
	}

	// ---------------------------------------------------------------------------
	// ③ the face predicates (the absorber faces, per the :36-:37 pair)
	// ---------------------------------------------------------------------------

	@Test
	public void energizerTakesBackOnlyAndEmitsFrontOnlyQu() {
		GT6LaserConverterBlockEntity tEnergizer = energizer(32, 16);
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertEquals(tSide == BACK, tEnergizer.isInput(tSide),
					"energizer side " + tSide + ": back only (MultiTileEntityQuantumEnergizerLaser :36)");
			assertEquals(tSide == FRONT, tEnergizer.isEnergyEmittingTo(TD.Energy.QU, tSide, true),
					"energizer side " + tSide + ": the QU emission face is the FRONT (:37)");
		}
		assertTrue(tEnergizer.isEnergyAcceptingFrom(TD.Energy.LU, BACK, false), "the BACK takes the beam");
		assertFalse(tEnergizer.isEnergyAcceptingFrom(TD.Energy.LU, FRONT, false), "the FRONT refuses intake");
		assertFalse(tEnergizer.isEnergyEmittingTo(TD.Energy.LU, FRONT, true), "the FRONT never emits LU");
		assertTrue(tEnergizer.isEnergyEmittingTo(TD.Energy.QU, FRONT, true), "the FRONT emits QU");
		// the family identity: the name twin resolves the energizer BET
		assertEquals("quantum_energizer", tEnergizer.getTileEntityName());
	}

	// ---------------------------------------------------------------------------
	// ④ the cross-domain reject + the exact frequency door per rung
	// ---------------------------------------------------------------------------

	@Test
	public void crossDomainOffersAreRefusedAndTheDoorIsExactlyHalfTheInputColumn() {
		GT6LaserConverterBlockEntity tEnergizer = energizer(32, 16);
		// the energizer is an LU machine: an EU/RU offer is refused outright
		assertEquals(0, tEnergizer.doEnergyInjection(TD.Energy.EU, BACK, 32, 1, true), "EU refused on the LU intake");
		assertEquals(0, tEnergizer.doEnergyInjection(TD.Energy.RU, BACK, 32, 1, true), "RU refused");
		assertEquals(0, tEnergizer.mLastIn, "nothing consumed");
		// the exact door per rung: min = in/2 (the column the RCON chain walks live)
		for (int i = 0; i < GT6Lasers.LASER_INPUTS.length; i++) {
			GT6LaserConverterBlockEntity tRung = energizer(GT6Lasers.LASER_INPUTS[i], GT6Lasers.LASER_OUTPUTS[i]);
			assertEquals(GT6Lasers.LASER_OUTPUTS[i],
					tRung.getEnergySizeInputMin(TD.Energy.LU, BACK),
					"rung " + i + ": the door is out = in/2 EXACTLY (Base10 :76, the frequency-tuning face)");
			assertEquals(GT6Lasers.LASER_INPUTS[i],
					tRung.getEnergySizeInputRecommended(TD.Energy.LU, BACK), "rung " + i + ": rec = in");
			assertEquals(GT6Lasers.LASER_INPUTS[i] * 2,
					tRung.getEnergySizeInputMax(TD.Energy.LU, BACK), "rung " + i + ": max = 2in");
		}
		// the type collection names both converter halves
		var tTypes = tEnergizer.getEnergyTypes(BACK);
		assertTrue(tTypes.contains(TD.Energy.LU) && tTypes.contains(TD.Energy.QU), "LU in + QU out");
	}

	// ---------------------------------------------------------------------------
	// the waste arm + the negative fold + the accounting round trip
	// ---------------------------------------------------------------------------

	@Test
	public void wasteArmConsumesIntoTheVoidAndNegativeFoldsPositive() {
		GT6LaserConverterBlockEntity tEnergizer = energizer(32, 16);
		CountingSink tSink = new CountingSink(TD.Energy.QU);
		tSink.rejecting = true; // nothing out there takes QU
		tEnergizer.setAdjacencyOverrideForTest(GT6LaserDomainTest.adjacencyAt(tSink, FRONT));
		assertEquals(1, tEnergizer.doEnergyInjection(TD.Energy.LU, BACK, 32, 1, true), "the LU intake rides regardless");
		tEnergizer.onTick(100, true);
		assertEquals(0, tSink.packets, "nothing emitted — no consumer");
		assertEquals(0, tEnergizer.mLastOut, "the accounting out stays 0");
		assertEquals(32, tEnergizer.mLastIn, "the intake PAID (waste=T: the input side is not refunded)");
		assertEquals(0, tEnergizer.mStorage, "the vent cleared the bucket");
		assertFalse(tEnergizer.mActive, "no emission, no activity");

		// the negative LU packet folds to a positive QU emission (LU ∉ ALL_NEGATIVE_ALLOWED)
		GT6LaserConverterBlockEntity tNegative = energizer(32, 16);
		CountingSink tQuSink = new CountingSink(TD.Energy.QU);
		tQuSink.recordSize = true;
		tNegative.setAdjacencyOverrideForTest(GT6LaserDomainTest.adjacencyAt(tQuSink, FRONT));
		assertEquals(1, tNegative.doEnergyInjection(TD.Energy.LU, BACK, -32, 1, true), "the negative LU packet enters");
		tNegative.onTick(100, true);
		assertEquals(16, tQuSink.lastSize, "the QU packet size is positive (Base10 :121 second conjunct fails)");
	}

	@Test
	public void accountingPairSurvivesASaveLoadRoundTrip() {
		GT6LaserConverterBlockEntity tEnergizer = energizer(32, 16);
		CountingSink tSink = new CountingSink(TD.Energy.QU);
		tEnergizer.setAdjacencyOverrideForTest(GT6LaserDomainTest.adjacencyAt(tSink, FRONT));
		tEnergizer.doEnergyInjection(TD.Energy.LU, BACK, 32, 1, true);
		tEnergizer.onTick(100, true);
		assertEquals(32, tEnergizer.mLastIn);
		assertEquals(16, tEnergizer.mLastOut);
		CompoundTag tNBT = new CompoundTag();
		tEnergizer.saveAdditional(tNBT);
		assertTrue(tNBT.contains(GT6LaserConverterBlockEntity.NBT_LAST_IN), "gt.last_in persisted");
		assertTrue(tNBT.contains(GT6LaserConverterBlockEntity.NBT_LAST_OUT), "gt.last_out persisted");
		GT6LaserConverterBlockEntity tRestored = energizer(32, 16);
		tRestored.load(tNBT);
		assertEquals(32, tRestored.mLastIn, "the intake survived");
		assertEquals(16, tRestored.mLastOut, "the emission survived");
		tRestored.resetAccounting();
		assertEquals(0, tRestored.mLastIn + tRestored.mLastOut, "the reset arm zeroes the pair");
	}
}
