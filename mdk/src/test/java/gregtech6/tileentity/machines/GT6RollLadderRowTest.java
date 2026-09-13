package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GTMachines;

/**
 * The p29-w1-kinetic-roll-ladder row census (the OFFLINE half — the
 * {@link GT6UlvMachineLadderRowTest} shape): the four RU roll-ladder families pinned to
 * the upstream Loader_MultiTileEntities.java:1349-1370 rows — the :1349-1370 face all
 * four families share (item in left / auto left, out right / auto right, energy back),
 * the Kinetic_T[1..4] material rungs (Bronze/Steel/Titanium/Tungstensteel, MT.java:3690),
 * NBT_INPUT 32/128/512/2048 as the tier index into {@link GTMachines#TIER_INPUTS}, no
 * NBT_PARALLEL → 1, menu = null, and the 26-arg MachineRow constructor (efficiency =
 * null — kinetic machines carry no NBT_EFFICIENCY key). The RollingMill RU ladder shares
 * the p28 ULV rung's map AND BET (same-map co-existence, each running its own row), so
 * the map identity and the RU/EU energy split are pinned here.
 */
public class GT6RollLadderRowTest extends TileEntityBasicMachineOfflineTestBase {

	/** All sixteen roll-ladder rows in family-then-tier order. */
	private static java.util.List<GTBasicMachineBlock.MachineRow> allRows() {
		java.util.List<GTBasicMachineBlock.MachineRow> rRows = new java.util.ArrayList<>();
		rRows.addAll(GTMachines.ROLLINGMILL_RU_ROWS);
		rRows.addAll(GTMachines.ROLL_BENDER_ROWS);
		rRows.addAll(GTMachines.ROLL_FORMER_ROWS);
		rRows.addAll(GTMachines.CLUSTER_MILL_ROWS);
		return rRows;
	}

	@Test
	void rollLadderIsFourFamiliesOfFourRows() {
		assertEquals(4, GTMachines.ROLLINGMILL_RU_ROWS.size(), "the RU RollingMill ladder");
		assertEquals(4, GTMachines.ROLL_BENDER_ROWS.size(), "the RollBender ladder");
		assertEquals(4, GTMachines.ROLL_FORMER_ROWS.size(), "the RollFormer ladder");
		assertEquals(4, GTMachines.CLUSTER_MILL_ROWS.size(), "the ClusterMill ladder");
		// the metaIds, upstream line order :1349-1352 / :1355-1358 / :1361-1364 / :1367-1370
		int[] tExpected = {20111, 20112, 20113, 20114, 20121, 20122, 20123, 20124,
				20131, 20132, 20133, 20134, 20141, 20142, 20143, 20144};
		int i = 0;
		for (GTBasicMachineBlock.MachineRow tRow : allRows()) {
			assertEquals(tExpected[i++], tRow.metaId(), tRow.path() + ": the upstream metaId");
		}
	}

	@Test
	void rowsRideTheKineticMaterialLadder() {
		// Kinetic_T[1..4] = Bronze / ANY.Steel / Ti / Tungstensteel (upstream MT.java:3690),
		// hardness 7.0/6.0/9.0/12.5 (NBT_RESISTANCE == hardness, Loader:1349-1370)
		String[] tSlugs = {"bronze", "steel", "titanium", "tungstensteel"};
		String[] tDisplays = {"Bronze", "Steel", "Titanium", "Tungstensteel"};
		float[] tHardness = {7.0F, 6.0F, 9.0F, 12.5F};
		for (java.util.List<GTBasicMachineBlock.MachineRow> tRows : java.util.List.of(
				GTMachines.ROLLINGMILL_RU_ROWS, GTMachines.ROLL_BENDER_ROWS,
				GTMachines.ROLL_FORMER_ROWS, GTMachines.CLUSTER_MILL_ROWS)) {
			for (int t = 0; t < tRows.size(); t++) {
				GTBasicMachineBlock.MachineRow tRow = tRows.get(t);
				assertEquals(t, tRow.tier(), tRow.path() + ": the material rung index");
				assertSame(GTMachines.KINETIC_T_LADDER.get(t).get(), tRow.material().get(),
						tRow.path() + ": the Kinetic_T[" + (t + 1) + "] material column");
				assertEquals(tSlugs[t], tRow.matSlug(), tRow.path() + ": the gt6.row.mat slug");
				assertEquals(tDisplays[t], tRow.matDisplay(), tRow.path() + ": the material word");
				assertEquals(tHardness[t], tRow.hardness(), tRow.path() + ": the NBT_HARDNESS column");
			}
		}
	}

	@Test
	void tierColumnIsTheNbtInputSemantics() {
		// NBT_INPUT 32/128/512/2048 (Loader:1349-1370) folds into the tier index —
		// TIER_INPUTS[t] = {in/2, in, in*2} through the :126 conversion, pinned verbatim
		// with the row tier as the selector (the RCON live window pin is the behavioural half)
		long[][] tExpected = {{16, 32, 64}, {64, 128, 256}, {256, 512, 1024}, {1024, 2048, 4096}};
		for (GTBasicMachineBlock.MachineRow tRow : allRows()) {
			assertArrayEquals(tExpected[tRow.tier()], GTMachines.TIER_INPUTS[tRow.tier()],
					tRow.path() + ": the NBT_INPUT window through the :126 conversion");
			// the 8 RU packet of the ULV-adjacent domain is dead below T1 (min 16) — the
			// same wall the p28 ULV window made on the electric side
			assertTrue(tExpected[tRow.tier()][0] > 8, tRow.path() + ": an 8 RU packet is below the ladder min (the ULV-wall semantics)");
		}
	}

	@Test
	void everyRowCarriesTheSharedFace() {
		// the :1349-1370 face ALL FOUR families share verbatim: item in left (SBIT_L,
		// auto SIDE_LEFT), out right (SBIT_R, auto SIDE_RIGHT), energy back (SBIT_B) —
		// the :137/:138/:151 reads OR SBIT_A onto the masks, the row bytes carry the
		// post-read values (the dryer-row convention)
		byte tLeft = (byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
		byte tRight = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A);
		byte tBack = (byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A);
		for (GTBasicMachineBlock.MachineRow tRow : allRows()) {
			assertEquals(tLeft, tRow.itemIn(), tRow.path() + ": NBT_INV_SIDE_IN SBIT_L");
			assertEquals(tRight, tRow.itemOut(), tRow.path() + ": NBT_INV_SIDE_OUT SBIT_R");
			assertEquals(tBack, tRow.energySides(), tRow.path() + ": NBT_ENERGY_ACCEPTED_SIDES SBIT_B");
			assertEquals((byte)2, tRow.itemAutoIn(), tRow.path() + ": NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals((byte)4, tRow.itemAutoOut(), tRow.path() + ": NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
			assertEquals((byte)127, tRow.fluidIn(), tRow.path() + ": no NBT_TANK_SIDE_IN key");
			assertEquals((byte)127, tRow.fluidOut(), tRow.path() + ": no NBT_TANK_SIDE_OUT key");
			assertEquals((byte)-1, tRow.fluidAutoIn(), tRow.path() + ": no NBT_TANK_SIDE_AUTO_IN key");
			assertEquals((byte)-1, tRow.fluidAutoOut(), tRow.path() + ": no NBT_TANK_SIDE_AUTO_OUT key");
		}
	}

	@Test
	void kineticColumnsNoParallelNoMenuNoEfficiency() {
		for (GTBasicMachineBlock.MachineRow tRow : allRows()) {
			assertEquals(1, tRow.parallel(), tRow.path() + ": NO NBT_PARALLEL key → 1 (the :97 ruling shape)");
			assertFalse(tRow.parallelDuration(), tRow.path() + ": no NBT_PARALLEL_DURATION key");
			assertNull(tRow.menu(), tRow.path() + ": zero new gt6:* MenuType (the p26 W1 GUI clause)");
			assertNull(tRow.efficiency(), tRow.path() + ": the 26-arg ctor — no NBT_EFFICIENCY key, the BE keeps the :96 default 10000");
			assertTrue(tRow.cheapOverclocking(), tRow.path() + ": the :773 loop runs unconditionally");
			assertNull(tRow.maxMeltingPointK(), tRow.path() + ": no melting gate on the legacy rows");
			assertFalse(tRow.ulvVoltage(), tRow.path() + ": the legacy TIER_INPUTS window (NOT the p28 ULV marker)");
			assertSame(TD.Energy.RU, tRow.energyType(), tRow.path() + ": RU on every roll-ladder row (Loader:1349-1370)");
		}
	}

	@Test
	void rollingmillRuSharesTheUlvMapAndRunsItsOwnRow() {
		// the same-map co-existence: the RU ladder and the p28 ULV electric rung share the
		// ROLLING_MILL map (and the family BET through the BY_PATH walk) — each side runs
		// its own row (ULV window EU vs TIER_INPUTS RU)
		assertSame(GT6RecipeMaps.ROLLING_MILL, GTMachines.ROLLINGMILL_RU_ROWS.get(0).recipes().get(),
				"the RU ladder rides the shared ROLLING_MILL map");
		assertSame(GTMachines.ROLLINGMILL_ROWS.get(0).recipes().get(), GTMachines.ROLLINGMILL_RU_ROWS.get(0).recipes().get(),
				"the p28 ULV rung and the RU ladder share ONE map");
		assertSame(TD.Energy.RU, GTMachines.ROLLINGMILL_RU_ROWS.get(0).energyType(), "the RU ladder: RU");
		assertSame(TD.Energy.EU, GTMachines.ROLLINGMILL_ROWS.get(0).energyType(), "the p28 ULV rung: EU (untouched)");
		assertTrue(GTMachines.ROLLINGMILL_ROWS.get(0).ulvVoltage(), "the p28 ULV rung keeps the ULV window marker");
		assertFalse(GTMachines.ROLLINGMILL_RU_ROWS.get(0).ulvVoltage(), "the RU ladder keeps the TIER_INPUTS window");
		// the map/texture per family
		assertSame(GT6RecipeMaps.ROLL_BENDER, GTMachines.ROLL_BENDER_ROWS.get(0).recipes().get(), "RM.ROLL_BENDER (card-A constant reused)");
		assertSame(GT6RecipeMaps.ROLL_FORMER, GTMachines.ROLL_FORMER_ROWS.get(0).recipes().get(), "RM.ROLL_FORMER");
		assertSame(GT6RecipeMaps.CLUSTER_MILL, GTMachines.CLUSTER_MILL_ROWS.get(0).recipes().get(), "RM.CLUSTER_MILL (the casingMachineQuadruple family)");
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLLINGMILL_RU_ROWS) {
			assertEquals("rollingmill", tRow.texture(), tRow.path() + ": the family NBT_TEXTURE");
		}
		assertEquals("rollbender", GTMachines.ROLL_BENDER_ROWS.get(0).texture(), "the :1355 NBT_TEXTURE");
		assertEquals("rollformer", GTMachines.ROLL_FORMER_ROWS.get(0).texture(), "the :1361 NBT_TEXTURE");
		assertEquals("clustermill", GTMachines.CLUSTER_MILL_ROWS.get(0).texture(), "the :1367 NBT_TEXTURE");
	}

	@Test
	void pathsAndUnitKeysMatchTheCardContract() {
		// the p28 ULV rung owns the bare "rollingmill" path — the RU ladder is tier-suffixed
		assertEquals("rollingmill", GTMachines.ROLLINGMILL_ROWS.get(0).path(), "the p28 ULV rung keeps the bare path");
		String[] tRuPaths = {"rollingmill_t1", "rollingmill_t2", "rollingmill_t3", "rollingmill_t4"};
		for (int t = 0; t < 4; t++) {
			assertEquals(tRuPaths[t], GTMachines.ROLLINGMILL_RU_ROWS.get(t).path(), "the RU ladder path " + t);
			assertEquals(GTMachines.MACHINE_ROLLING_MILL_UNIT_KEY, GTMachines.ROLLINGMILL_RU_ROWS.get(t).displayKey(),
					"the RU ladder shares the Rolling Mill family template");
		}
		assertEquals("gt6.row.machine.roll_bender", GTMachines.MACHINE_ROLL_BENDER_UNIT_KEY, "the Roll Bender unit key");
		assertEquals("gt6.row.machine.roll_former", GTMachines.MACHINE_ROLL_FORMER_UNIT_KEY, "the Roll Former unit key");
		assertEquals("gt6.row.machine.cluster_mill", GTMachines.MACHINE_CLUSTER_MILL_UNIT_KEY, "the Cluster Mill unit key");
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLL_BENDER_ROWS) {
			assertEquals(GTMachines.MACHINE_ROLL_BENDER_UNIT_KEY, tRow.displayKey(), tRow.path() + ": the family template");
		}
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLL_FORMER_ROWS) {
			assertEquals(GTMachines.MACHINE_ROLL_FORMER_UNIT_KEY, tRow.displayKey(), tRow.path() + ": the family template");
		}
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.CLUSTER_MILL_ROWS) {
			assertEquals(GTMachines.MACHINE_CLUSTER_MILL_UNIT_KEY, tRow.displayKey(), tRow.path() + ": the family template");
		}
	}
}
