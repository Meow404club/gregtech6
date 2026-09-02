package gregtech6.tileentity.energy.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GT6RecipesBurnFuels;
import gregtech6.recipes.RecipeMapFurnaceFuel;
import gregtech6.registry.GT6BurningBoxes;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task p13-burning-box-family — the four registration ladders asserted ROW BY ROW
 * against the Loader_MultiTileEntities.java:517-704 anchors (行值零差), the row-count
 * erratum declaration, the FM.Burn pour values, the FluidBed declared-empty state and
 * the FM.Furnace bridge constants.
 *
 * <p><b>COUNT ERRATUM (declared)</b>: the task card's acceptance says "26+22+22+26",
 * but the Solid ladder is 1 (Brick, :519) + 13 (:522-534) + 13 (:536-548) = 27 — the
 * Brick row the card's own spec ① lists is the one the acceptance arithmetic lost
 * (the p12-engine-steam 28-vs-26 census precedent). The tests pin the upstream truth:
 * 27 + 22 + 22 + 26 = 97 rows.
 */
public class BurningBoxRowTableTest extends GTOfflineTestBase {

	/** One expected row: path tail, display, efficiency, output, hardness. */
	record Row(String path, String display, int eff, int rate, float hardness) {}

	/** The 13 burning-box materials (slug, display, hardness) — the Loader NBT_HARDNESS column. */
	static final Object[][] MATS = {
			{"lead", "Lead", 4.0F},
			{"bismuth", "Bismuth", 4.0F},
			{"bronze", "Bronze", 7.0F},
			{"arsenic_copper", "Arsenic Copper", 7.0F},
			{"arsenic_bronze", "Arsenic Bronze", 7.0F},
			{"invar", "Invar", 4.0F},
			{"steel", "Steel", 6.0F},
			{"chromium", "Chromium", 4.0F},
			{"titanium", "Titanium", 9.0F},
			{"netherite", "Netherite", 9.0F},
			{"tungsten", "Tungsten", 10.0F},
			{"tungstensteel", "Tungstensteel", 12.5F},
			{"tantalum_hafnium_carbide", "Ta4HfC5", 12.5F},
	};

	/** The eff/rate ladders, upstream file order (:522-534 normal then :536-548 dense). */
	static final int[][] SOLID_NORMAL = {{5000, 16}, {4500, 20}, {7500, 24}, {8000, 24}, {9000, 28}, {10000, 16}, {7000, 32}, {8500, 112}, {8500, 96}, {9000, 96}, {10000, 128}, {9000, 128}, {10000, 256}};
	static final int[][] SOLID_DENSE  = {{5000, 64}, {4500, 80}, {7500, 96}, {8000, 96}, {9000, 112}, {10000, 64}, {7000, 128}, {8500, 448}, {8500, 384}, {9000, 384}, {10000, 512}, {9000, 512}, {10000, 1024}};
	static final int[][] LIQUID_NORMAL = {{7500, 24}, {8000, 24}, {9000, 28}, {10000, 16}, {7000, 32}, {8500, 112}, {8500, 96}, {9000, 96}, {10000, 128}, {9000, 128}, {10000, 256}};
	static final int[][] LIQUID_DENSE  = {{7500, 96}, {8000, 96}, {9000, 112}, {10000, 64}, {7000, 128}, {8500, 448}, {8500, 384}, {9000, 384}, {10000, 512}, {9000, 512}, {10000, 1024}};
	// the GAS ladders are value-identical to the LIQUID ladders (:649-673 vs :619-643)
	static final int[][] FLUIDBED_NORMAL = {{5000, 64}, {4500, 80}, {7500, 96}, {8000, 96}, {9000, 112}, {10000, 64}, {7000, 128}, {8500, 448}, {8500, 384}, {9000, 384}, {10000, 512}, {9000, 512}, {10000, 1024}};
	static final int[][] FLUIDBED_DENSE  = {{5000, 256}, {4500, 320}, {7500, 384}, {8000, 384}, {9000, 448}, {10000, 256}, {7000, 512}, {8500, 1792}, {8500, 1536}, {9000, 1536}, {10000, 2048}, {9000, 2048}, {10000, 4096}};

	@BeforeAll
	static void initMaps() {
		GT6RecipeMaps.init();
	}

	@Test
	public void theRowCountsMatchTheLoaderLines() {
		assertEquals(27, 1 + GT6BurningBoxes.SOLID_ROWS.size(), "Brick(:519) + 13(:522-534) + 13(:536-548) — the card's '26' loses the Brick row (declared erratum)");
		assertEquals(22, GT6BurningBoxes.LIQUID_ROWS.size(), ":619-629 + :633-643, the Pb/Bi comment rows :617-618/:631-632 excluded");
		assertEquals(22, GT6BurningBoxes.GAS_ROWS.size(), ":649-659 + :663-673");
		assertEquals(26, GT6BurningBoxes.FLUIDBED_ROWS.size(), ":678-690 + :692-704");
		assertEquals(97, GT6BurningBoxes.allRows().size(), "97 blocks/items — the whole Burning Boxes category");
	}

	/** The Brick row anchor (:519). */
	@Test
	public void theBrickRowMatchesLoader519() {
		GT6BurningBoxes.BurningBoxRow tRow = GT6BurningBoxes.BRICK_ROW;
		assertEquals("brick_burning_box", tRow.path());
		assertEquals("Brick Burning Box (Solid)", tRow.displayName());
		assertEquals(2500, tRow.efficiency(), ":519 NBT_EFFICIENCY");
		assertEquals(16, tRow.rate(), ":519 NBT_OUTPUT");
		assertEquals(6.0F, tRow.material().hardness(), ":519 NBT_HARDNESS");
		assertEquals(GT6BurningBoxes.Family.SOLID, tRow.family());
		assertTrue(tRow.stone(), ":519 the aStone block carrier");
	}

	/** The Solid ladders (:522-548), every row anchored. */
	@Test
	public void theSolidLaddersMatchLoader522to548() {
		List<Row> tExpected = ladder("burning_box_solid_", "Burning Box (Solid, ", "dense_burning_box_solid_", "Dense Burning Box (Solid, ", SOLID_NORMAL, SOLID_DENSE);
		assertLadder(GT6BurningBoxes.SOLID_ROWS, tExpected);
	}

	/** The Liquid ladders (:619-643). */
	@Test
	public void theLiquidLaddersMatchLoader619to643() {
		List<Row> tExpected = ladder("burning_box_liquid_", "Burning Box (Liquid, ", "dense_burning_box_liquid_", "Dense Burning Box (Liquid, ", LIQUID_NORMAL, LIQUID_DENSE);
		assertLadder(GT6BurningBoxes.LIQUID_ROWS, tExpected);
	}

	/** The Gas ladders (:649-673) — every row FM.Burn (the family stamps it). */
	@Test
	public void theGasLaddersMatchLoader649to673AllBurnMap() {
		List<Row> tExpected = ladder("burning_box_gas_", "Burning Box (Gas, ", "dense_burning_box_gas_", "Dense Burning Box (Gas, ", LIQUID_NORMAL, LIQUID_DENSE);
		assertLadder(GT6BurningBoxes.GAS_ROWS, tExpected);
		for (GT6BurningBoxes.BurningBoxRow tRow : GT6BurningBoxes.GAS_ROWS) {
			assertEquals(GT6BurningBoxes.Family.GAS, tRow.family(), tRow.path() + " rides the GAS BE (the FM.Burn fill gate, NOT FM.Gas — Loader :645-673 NBT_FUELMAP)");
		}
	}

	/** The FluidBed ladders (:678-704) — the Dense ×4 output. */
	@Test
	public void theFluidBedLaddersMatchLoader678to704() {
		List<Row> tExpected = ladder("burning_box_fluidbed_", "Fluidized Bed Burning Box (", "dense_burning_box_fluidbed_", "Dense Fluidized Bed Burning Box (", FLUIDBED_NORMAL, FLUIDBED_DENSE);
		assertLadder(GT6BurningBoxes.FLUIDBED_ROWS, tExpected);
		for (GT6BurningBoxes.BurningBoxRow tRow : GT6BurningBoxes.FLUIDBED_ROWS) {
			assertEquals(GT6BurningBoxes.Family.FLUIDBED, tRow.family(), tRow.path());
		}
	}

	// ---------------------------------------------------------------------------
	// the fuel tables
	// ---------------------------------------------------------------------------

	/** The BURN column pour (Loader_Fuels.java:77-120, the BURN values vs the ENGINE column). */
	@Test
	public void theBurnPourCarriesTheBurnColumnValues() {
		Object[][] tExpected = {
				{"jetfuel", -128, 9}, {"kerosine", -64, 5}, {"petrol", -64, 5},
				{"diesel", -64, 5}, {"fuel", -64, 6}, {"nitrofuel", -64, 9}, {"ethanol", -16, 6}};
		List<GT6RecipesBurnFuels.BurnRow> tRows = GT6RecipesBurnFuels.table();
		assertEquals(tExpected.length, tRows.size(), "the :77-120 pinned range, the port's seven registered fluids");
		for (int i = 0; i < tExpected.length; i++) {
			GT6RecipesBurnFuels.BurnRow tRow = tRows.get(i);
			assertEquals(tExpected[i][0], tRow.fluid(), "row " + i);
			assertEquals((long)(Integer) tExpected[i][1], tRow.eUt(), tRow.fluid() + " EUt (the negative generator convention, verbatim)");
			assertEquals((long)(Integer) tExpected[i][2], tRow.duration(), tRow.fluid() + " duration");
			// :78=1152, :82/:86/:90=320, :94=384, :97=576, :101=96 — the BURN column (vs the ENGINE 1536/448/512/768/144)
			assertEquals((long)Math.abs((Integer) tExpected[i][1] * (Integer) tExpected[i][2]), tRow.absolutePowerPerUnit(),
					tRow.fluid() + " heat per fluid unit = |EUt × duration| (Recipe.java:723-725)");
		}
		assertTrue(tRows.get(0).absolutePowerPerUnit() != 1536, "the BURN column is NOT the ENGINE column (jetfuel 1152 ≠ 1536)");
	}

	/** The FluidBed declared-empty state (the spec ⑥ archaeology verdict, GT6RecipesBurnFuels class doc). */
	@Test
	public void theFluidBedMapStaysDeclaredEmpty() {
		assertTrue(GT6RecipeMaps.FLUIDBED != null, "the map exists (W1 skeleton)");
		assertTrue(GT6RecipeMaps.FLUIDBED.mRecipeList.isEmpty(), "no rows — calcite/ash/burn-time primitives are pool-card content (Loader_Fuels.java:37-43 evidence in the pour class doc)");
	}

	/** The FM.Furnace bridge constants (the RecipeMapFurnaceFuel class doc). */
	@Test
	public void theFurnaceFuelBridgePinsTheConstants() {
		assertEquals(25, RecipeMapFurnaceFuel.EU_PER_FURNACE_TICK, "CS.java:212 — 1 Smelt = 200 ticks = 5000 GU");
		assertTrue(GT6RecipeMaps.FURNACE_FUEL != null, "the map instance constructs in init()");
		assertTrue(GT6RecipeMaps.FURNACE_FUEL.mRecipeList.isEmpty(), "the on-demand synthesizer never carries static rows (upstream RecipeMapFurnaceFuel.java:48-92)");
	}

	// ---------------------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------------------

	private static List<Row> ladder(String aNormalPrefix, String aNormalDisplay, String aDensePrefix, String aDenseDisplay, int[][] aNormal, int[][] aDense) {
		List<Row> rRows = new ArrayList<>();
		int tMatCount = MATS.length;
		// the upstream ladders order the materials identically (Pb, Bi, Bronze, AsCu, AsBronze, Invar, Steel, Cr, Ti, Netherite, W, TS, Ta4HfC5)
		for (int i = 0; i < aNormal.length; i++) {
			int tMat = (i + tMatShift(aNormalPrefix)) % tMatCount;
			rRows.add(new Row(aNormalPrefix + MATS[tMat][0], aNormalDisplay + MATS[tMat][1] + ")", aNormal[i][0], aNormal[i][1], (Float)MATS[tMat][2]));
		}
		for (int i = 0; i < aDense.length; i++) {
			int tMat = (i + tMatShift(aNormalPrefix)) % tMatCount;
			rRows.add(new Row(aDensePrefix + MATS[tMat][0], aDenseDisplay + MATS[tMat][1] + ")", aDense[i][0], aDense[i][1], (Float)MATS[tMat][2]));
		}
		return rRows;
	}

	/** The Liquid/Gas ladders start at Bronze (the Pb/Bi rows are commented out upstream) — an offset of 2 into MATS. */
	private static int tMatShift(String aPrefix) {
		return aPrefix.contains("liquid") || aPrefix.contains("gas") ? 2 : 0;
	}

	private static void assertLadder(List<GT6BurningBoxes.BurningBoxRow> aRows, List<Row> aExpected) {
		assertEquals(aExpected.size(), aRows.size());
		for (int i = 0; i < aExpected.size(); i++) {
			GT6BurningBoxes.BurningBoxRow tRow = aRows.get(i);
			Row tExp = aExpected.get(i);
			assertEquals(tExp.path(), tRow.path(), "row " + i + " path");
			assertEquals(tExp.display(), tRow.displayName(), tRow.path() + " display");
			assertEquals(tExp.eff(), tRow.efficiency(), tRow.path() + " NBT_EFFICIENCY");
			assertEquals(tExp.rate(), tRow.rate(), tRow.path() + " NBT_OUTPUT");
			assertEquals(tExp.hardness(), tRow.material().hardness(), tRow.path() + " NBT_HARDNESS");
		}
	}
}
