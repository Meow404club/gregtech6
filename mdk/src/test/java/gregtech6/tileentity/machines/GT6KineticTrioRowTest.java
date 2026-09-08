package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GTMachines;

/**
 * The W1 Kinetic trio row acceptance (task p26-w1-sifter-compressor-wiremill, the OFFLINE
 * half — the {@link GTCannerFamilyRowTest} shape): the twelve row records pinned to the
 * upstream columns (Sifter :1312-1315 ids 20051-20054 / Compressor :1343-1346 ids
 * 20101-20104 / Wiremill :1373-1376 ids 20151-20154 — Kinetic_T tier-material name column
 * / NBT_HARDNESS 7.0/6.0/9.0/12.5 / NBT_INPUT 32/128/512/2048 through the TIER_INPUTS
 * carrier / NBT_ENERGY_ACCEPTED KU/KU/RU / NBT_RECIPEMAP RM.Sifting|RM.Compressor|
 * RM.Wiremill / NBT_TEXTURE per family / NBT_PARALLEL {4,8,16,32} on sifter+compressor via
 * the PARALLEL_4_32 shared constant, NO key on wiremill → 1 / NBT_PARALLEL_DURATION T on
 * sifter+compressor, F on wiremill / the connectivity mask columns / menu = null on every
 * row — the zero-new-MenuType GUI clause). The PARALLEL_4_32/CRUSHER_PARALLEL merge is
 * pinned as one array identity (the W1 ruling: merged, never re-defined).
 */
public class GT6KineticTrioRowTest extends TileEntityBasicMachineOfflineTestBase {

	/** CS.java:612 — SBIT_B (Sifter/Wiremill) / SBIT_L (Compressor), the :151 read form + the ORed SBIT_A. */
	static final byte ENERGY_B = (byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A);
	static final byte ENERGY_L = (byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	/** The no-NBT-key tank face: the upstream field default 127, all relative sides. */
	static final byte TANK_DEFAULT = 127;
	/** SIDE_UNDEFINED — no NBT_TANK_SIDE_AUTO_* key on the trio rows. */
	static final byte AUTO_UNDEFINED = (byte)-1;

	@Test
	void kineticRowsMatchTheUpstreamColumns() {
		String[][] tFamilies = {
				{"sifter", "gt6.row.machine.sifter", "KU"},
				{"compressor", "gt6.row.machine.compressor", "KU"},
				{"wiremill", "gt6.row.machine.wiremill", "RU"}};
		int[][] tMetaIds = {{20051, 20052, 20053, 20054}, {20101, 20102, 20103, 20104}, {20151, 20152, 20153, 20154}};
		String[] tMatSlugs = {"bronze", "steel", "titanium", "tungstensteel"};
		String[] tMatWords = {"Bronze", "Steel", "Titanium", "Tungstensteel"};
		float[] tHardness = {7.0F, 6.0F, 9.0F, 12.5F};
		for (int f = 0; f < 3; f++) {
			java.util.List<GTBasicMachineBlock.MachineRow> tRows = f == 0 ? GTMachines.SIFTER_ROWS
					: f == 1 ? GTMachines.COMPRESSOR_ROWS : GTMachines.WIREMILL_ROWS;
			assertEquals(4, tRows.size(), "the four " + tFamilies[f][0] + " rows");
			for (int i = 0; i < 4; i++) {
				GTBasicMachineBlock.MachineRow tRow = tRows.get(i);
				int tTier = i + 1;
				String tPath = tFamilies[f][0] + (i == 0 ? "" : "_t" + tTier);
				assertEquals(tPath, tRow.path(), "the registry path ladder");
				assertEquals(tMetaIds[f][i], tRow.metaId(), "the MultiTile id column of " + tPath);
				assertEquals(tTier, tRow.tier() + 1, "the tier index");
				assertEquals(tHardness[i], tRow.hardness(), "NBT_HARDNESS of " + tPath);
				assertSame(f == 2 ? TD.Energy.RU : TD.Energy.KU, tRow.energyType(), "NBT_ENERGY_ACCEPTED of " + tPath);
				assertSame(f == 0 ? GT6RecipeMaps.SIFTING : f == 1 ? GT6RecipeMaps.COMPRESSOR : GT6RecipeMaps.WIREMILL,
						tRow.recipes().get(), "NBT_RECIPEMAP through the supplier (" + tPath + ")");
				assertEquals(tFamilies[f][0], tRow.texture(), "NBT_TEXTURE (" + tPath + ")");
				assertEquals(tFamilies[f][1], tRow.displayKey(), "the family template key (the unit-key carrier)");
				assertEquals(tMatWords[i], tRow.matDisplay(), "the Kinetic_T material word of tier " + tTier);
				assertEquals(tMatSlugs[i], tRow.matSlug(), "the Kinetic_T slug (the gt6.row.mat key tail)");
				assertTrue(tRow.cheapOverclocking(), "the port :773 loop runs unconditionally");
				assertNull(tRow.menu(), "menu = null on " + tPath + " — ZERO new gt6:* MenuType (the card GUI clause)");
				// the parallel columns: {4,8,16,32} + duration T on sifter/compressor, no keys on wiremill
				if (f == 2) {
					assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL on the :1373 wiremill rows → 1");
					assertFalse(tRow.parallelDuration(), "NO NBT_PARALLEL_DURATION on the :1373 wiremill rows → F");
				} else {
					assertEquals(4 * (i + 1), tRow.parallel(), "NBT_PARALLEL of " + tPath + " (:1312/:1343)");
					assertTrue(tRow.parallelDuration(), "NBT_PARALLEL_DURATION T on " + tPath);
				}
				// the connectivity columns: top-in/bottom-out on sifter+compressor, left-in/right-out on wiremill;
				// tanks ride the 127 all-sides upstream field defaults (zero fluid recipes ≠ zero fluid face)
				if (f == 2) {
					assertEquals(ENERGY_B, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_B (:1373) + the ORed SBIT_A");
					assertEquals((byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A), tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_L (:1373)");
					assertEquals((byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A), tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R (:1373)");
					assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT (:1373)");
					assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT (:1373)");
				} else {
					byte tEnergy = f == 0 ? ENERGY_B : ENERGY_L;
					assertEquals(tEnergy, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES (:1312 SBIT_B / :1343 SBIT_L) + the ORed SBIT_A");
					assertEquals((byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A), tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_U (" + tPath + ")");
					assertEquals((byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A), tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_D (" + tPath + ")");
					assertEquals(1, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_TOP (" + tPath + ")");
					assertEquals(0, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_BOTTOM (" + tPath + ")");
				}
				assertEquals(TANK_DEFAULT, tRow.fluidIn(), "no NBT_TANK_SIDE_IN key → the upstream 127 default (" + tPath + ")");
				assertEquals(TANK_DEFAULT, tRow.fluidOut(), "no NBT_TANK_SIDE_OUT key → the upstream 127 default (" + tPath + ")");
				assertEquals(AUTO_UNDEFINED, tRow.fluidAutoIn(), "no NBT_TANK_SIDE_AUTO_IN key → SIDE_UNDEFINED (" + tPath + ")");
				assertEquals(AUTO_UNDEFINED, tRow.fluidAutoOut(), "no NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED (" + tPath + ")");
			}
		}
	}

	/** The W1 parallel-table merge ruling: PARALLEL_4_32 and CRUSHER_PARALLEL are ONE array. */
	@Test
	void parallelTableMergesWithTheCrusherRow() {
		assertSame(GTMachines.PARALLEL_4_32, GTMachines.CRUSHER_PARALLEL, "the W1 merge ruling — cross-referenced, never re-defined");
		assertNotNull(GTMachines.PARALLEL_4_32);
		assertEquals(4, GTMachines.PARALLEL_4_32.length);
		int[] tUpstream = {4, 8, 16, 32}; // :1300-1303/:1312-1315/:1343-1346
		for (int i = 0; i < 4; i++) assertEquals(tUpstream[i], GTMachines.PARALLEL_4_32[i], "the NBT_PARALLEL column of tier " + (i + 1));
		for (int i = 0; i < 4; i++) {
			assertEquals(GTMachines.PARALLEL_4_32[i], GTMachines.SIFTER_ROWS.get(i).parallel(), "the sifter rows consume the shared table");
			assertEquals(GTMachines.PARALLEL_4_32[i], GTMachines.COMPRESSOR_ROWS.get(i).parallel(), "the compressor rows consume the shared table");
		}
	}

	/**
	 * The {@code kineticMachine} factory seam (the applyRow public-test-seam form): the
	 * row masks land on a fixture machine per family, and the TIER_INPUTS half rides the
	 * shared {@code machine} helper — the tier index selects {mInputMin, mInput, mInputMax}.
	 */
	@Test
	void applyFactoryColumnsOnAFixture() {
		GTBasicMachineBlock.MachineRow tRow = GTMachines.SIFTER_ROWS.get(1); // T2
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SIFTING, tRow.parallel(), tRow.parallelDuration(), TD.Energy.KU);
		GTMachines.applyRow(tMachine, tRow);
		assertEquals(ENERGY_B, tMachine.mEnergyInputs, "the energy mask via applyRow");
		assertEquals(TANK_DEFAULT, tMachine.mFluidInputs, "the 127 tank-in default via applyRow");
		assertEquals(TANK_DEFAULT, tMachine.mFluidOutputs, "the 127 tank-out default via applyRow");
		// the TIER_INPUTS half (the shared machine() helper form): T2 = {64, 128, 256}
		assertEquals(64, tMachine.mInputMin, "TIER_INPUTS[1][0]");
		assertEquals(128, tMachine.mInput, "TIER_INPUTS[1][1]");
		assertEquals(256, tMachine.mInputMax, "TIER_INPUTS[1][2]");
		// the SIFTING slot shape (RM.java:83: items 1,12,1) — the Shredder 1+12 topology
		assertEquals(13, tMachine.getInventory().getSlots(), "the 1+12 slot shape (the Shredder outputGridPos topology)");
		assertEquals(1, tMachine.getInputSlotCount(), "the data-driven input count");
	}

	/** The RM.java:83/:87/:111 map declaration columns over the three maps. */
	@Test
	void kineticMapsCarryTheUpstreamDeclarations() {
		GT6RecipeMaps.init();
		try {
			assertSame(GT6RecipeMaps.SIFTING, gregtech6.recipes.RecipeMap.RECIPE_MAPS.get("gt.recipe.sifter"));
			assertEquals("Sifter", GT6RecipeMaps.SIFTING.mNameLocal);
			assertEquals(1, GT6RecipeMaps.SIFTING.mInputItemsCount);
			assertEquals(12, GT6RecipeMaps.SIFTING.mOutputItemsCount, "the RM.java:83 1/12 row — the Shredder topology");
			assertEquals(2, GT6RecipeMaps.SIFTING.mProgressBarDirection, "the RM.java:83 row is the one non-0 direction in the RM.java:60-115 block");
			assertEquals("gt6:textures/gui/machines/sifter.png", GT6RecipeMaps.SIFTING.mGUIPath, "lowercased");
			assertSame(GT6RecipeMaps.COMPRESSOR, gregtech6.recipes.RecipeMap.RECIPE_MAPS.get("gt.recipe.compressor"));
			assertEquals("Compressor", GT6RecipeMaps.COMPRESSOR.mNameLocal);
			assertEquals(1, GT6RecipeMaps.COMPRESSOR.mOutputItemsCount, "the RM.java:87 1/1/1 row");
			assertEquals("gt6:textures/gui/machines/compressor.png", GT6RecipeMaps.COMPRESSOR.mGUIPath, "lowercased");
			assertSame(GT6RecipeMaps.WIREMILL, gregtech6.recipes.RecipeMap.RECIPE_MAPS.get("gt.recipe.wiremill"));
			assertEquals("Wiremill", GT6RecipeMaps.WIREMILL.mNameLocal);
			assertEquals(1, GT6RecipeMaps.WIREMILL.mOutputItemsCount, "the RM.java:111 1/1/1 row");
			assertEquals("gt6:textures/gui/machines/wiremill.png", GT6RecipeMaps.WIREMILL.mGUIPath, "lowercased");
		} finally {
			GT6RecipeMaps.reset();
		}
	}
}
