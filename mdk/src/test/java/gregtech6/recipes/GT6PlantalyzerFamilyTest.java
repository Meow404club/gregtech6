package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;

/**
 * The p34-machines-burner-plantalyzer Plantalyzer acceptance test: the row-table pins
 * (the FIRST 5-tier ladder since the eu-core wave's shared {@code euFiveTierMachine}
 * carrier — Loader_MultiTileEntities.java:1601-1605, EU, Electric_T[1..5], parallel 64
 * with NO NBT_PARALLEL_DURATION key, hardness 4.0 on every row, the "plantalyzer"
 * texture) + the RM.java:109 map constants (items 2/2/0, fluids 1/0/0, MIN 1) + the
 * DECLARED-empty pour face (the upstream rows are the two Forestry/IC2 compat scans of
 * GT6_Main.java:321 plus the RecipeMapPlantalyzer dynamic findRecipe — the P10 compat
 * cut; the base-map form is the declared fold and the map stays at zero rows).
 */
public class GT6PlantalyzerFamilyTest extends GTRecipesOfflineTestBase {

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMaps.reset();
	}

	// ---------------------------------------------------------------------------
	// the row-table pins (Loader :1601-1605)
	// ---------------------------------------------------------------------------

	@Test
	void theFivePlantalyzerRowsCarryTheUpstreamColumns() {
		assertEquals(5, gregtech6.registry.GTMachines.PLANTALYZER_ROWS.size(), "the 5-ladder (the eu-core T5 shape)");
		String[] tPaths = {"plantalyzer", "plantalyzer_t2", "plantalyzer_t3", "plantalyzer_t4", "plantalyzer_t5"};
		int[] tIds = {20531, 20532, 20533, 20534, 20535};
		String[] tMats = {"lv", "mv", "hv", "ev", "iv"}; // the VN[1..5] ladder (the CS.java:154 array; the S9 "iv" ruling)
		int[] tWindows = {32, 128, 512, 2048, 8192}; // the NBT_INPUT column (TIER_INPUTS + the EV_TIER_INPUTS T5 arm)
		for (int i = 0; i < 5; i++) {
			GTBasicMachineBlockProxy tRow = new GTBasicMachineBlockProxy(gregtech6.registry.GTMachines.PLANTALYZER_ROWS.get(i));
			assertEquals(tPaths[i], tRow.path(), "the registry path of rung " + i);
			assertEquals(tIds[i], tRow.metaId(), "the meta id of rung " + i);
			assertEquals(4.0F, tRow.hardness(), "hardness 4.0 on EVERY row (:1601-1605)");
			assertEquals(64, tRow.parallel(), "NBT_PARALLEL 64 (:1601-1605)");
			assertFalse(tRow.parallelDuration(), "NO NBT_PARALLEL_DURATION key — the :770-771 speedup arm");
			assertEquals("plantalyzer", tRow.texture(), "the NBT_TEXTURE column");
			assertEquals(tMats[i], tRow.matSlug(), "the VN voltage word of rung " + i);
			assertTrue(tRow.energyType() == TD.Energy.EU, "the EU carrier (MultiTileEntityBasicMachineElectric)");
			assertEquals(i, tRow.tier(), "the tier IS the rung index");
			assertEquals(tWindows[i], gregtech6.registry.GTMachines.euFiveTierWindow(i)[1], "the NBT_INPUT column of rung " + i + " (the public euFiveTierWindow seam)");
			assertNotNull(tRow.recipes(), "the recipe-map supplier armed");
		}
	}

	// ---------------------------------------------------------------------------
	// the RM.java:109 constants + the declared-empty pour face
	// ---------------------------------------------------------------------------

	@Test
	void thePlantalyzerMapCarriesTheRm109Constants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.PLANTALYZER, "the PLANTALYZER map registered");
		assertEquals("gt.recipe.plantalyzer", GT6RecipeMaps.PLANTALYZER.mNameInternal, "the RM.java:109 internal name");
		assertEquals("Plantalyzer", GT6RecipeMaps.PLANTALYZER.mNameLocal, "the local name");
		assertEquals(2, GT6RecipeMaps.PLANTALYZER.mInputItemsCount, "items 2/2/0 — the input slots");
		assertEquals(2, GT6RecipeMaps.PLANTALYZER.mOutputItemsCount, "items 2/2/0 — the output slots");
		assertEquals(0, GT6RecipeMaps.PLANTALYZER.mMinimalInputItems, "items 2/2/0 — the item minimum");
		assertEquals(1, GT6RecipeMaps.PLANTALYZER.mInputFluidCount, "fluids 1/0/0 — the input slot");
		assertEquals(0, GT6RecipeMaps.PLANTALYZER.mOutputFluidCount, "fluids 1/0/0 — no output slot");
		assertEquals(0, GT6RecipeMaps.PLANTALYZER.mMinimalInputFluids, "fluids 1/0/0 — the fluid minimum");
		assertEquals(1, GT6RecipeMaps.PLANTALYZER.mMinimalInputs, "MIN 1 — the total-input gate");
	}

	@Test
	void theMapStaysDeclaredEmpty() {
		GT6RecipeMaps.init();
		// DECLARED-empty: the upstream row source is GT6_Main.java:321's two addFakeRecipe
		// scans (IL.FR_Tree_Sapling / IL.IC2_Crop_Seeds — Forestry/IC2 mod-compat) plus the
		// RecipeMapPlantalyzer dynamic findRecipe arm — BOTH are the P10 compat cut. The
		// PRESS declared-empty precedent pins the zero; the POURABLE key still ships so a
		// pack author can pour native rows without touching this loader.
		assertTrue(GT6RecipeMaps.PLANTALYZER.mRecipeList.isEmpty(), "zero static rows — the compat row source is the declared P10 cut");
		// the pourable-key face: the loader still RESOLVES the key (mapFor is the public
		// seam; POURABLE itself is the private table)
		assertNotNull(GT6RecipeMapJsonLoader.mapFor("plantalyzer"), "the plantalyzer key stays pourable");
		assertNotNull(GT6RecipeMapJsonLoader.mapFor("burnmixer"), "the burnmixer key stays pourable");
	}

	/** The narrow accessor over {@code GTBasicMachineBlock.MachineRow} (the record accessors, no package dance). */
	private record GTBasicMachineBlockProxy(gregtech6.block.GTBasicMachineBlock.MachineRow tRow) {
		String path() { return tRow.path(); }
		int metaId() { return tRow.metaId(); }
		float hardness() { return tRow.hardness(); }
		int parallel() { return tRow.parallel(); }
		boolean parallelDuration() { return tRow.parallelDuration(); }
		String texture() { return tRow.texture(); }
		String matSlug() { return tRow.matSlug(); }
		Object energyType() { return tRow.energyType(); }
		int tier() { return tRow.tier(); }
		Object recipes() { return tRow.recipes(); }
	}
}
