/**
 * Offline guard tests for task debt-jei-emi-batch1: the shared per-map category
 * metadata table (the census pin), the NEI layout-switch translation, the drawExtras
 * cost-text arithmetic and the chance/not-consumed tooltip seams — the exact faces the
 * JEI category (GT6RecipeMapJeiCategory) and the EMI twin (gregtech6.emi) both render
 * from, so pinning them here pins both legs' geometry at the one shared place. The JEI
 * category's own offline surface (uid/title/dims) rides the last test — RecipeType is a
 * pure value class, safe in a bare JVM.
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

class GT6RecipeMapViewerMetaTest extends GTRecipesOfflineTestBase {

	@BeforeAll
	static void bootMaterials() {
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

	@AfterEach
	void cleanUp() {
		GT6RecipeMaps.reset();
	}

	// -------------------------------------------------------------------
	// The census pin: eligibility, exclusion and the batch-1 canary set
	// -------------------------------------------------------------------

	@Test
	void censusEligibleVisibleAndExcluded() {
		GT6RecipeMaps.init();
		assertEquals(75, RecipeMap.RECIPE_MAPS.size(), "the main-baseline census (rm-six-maps' Mortar/Hammer tail lands by rebase, the card's 80-pin is that merged shape)");

		// the ruled exclusion table (r-jei-emi-coverage id927) never enters a category
		for (String tExcluded : List.of("mc.recipe.furnace", "mc.recipe.furnacefuel", "gt.recipe.cruciblesmelting",
				"gt.recipe.cruciblealloying", "gt.recipe.bumblelyzer", "gt.recipe.plantalyzer")) {
			assertFalse(GT6RecipeMapViewerMeta.eligible(RecipeMap.RECIPE_MAPS.get(tExcluded)), tExcluded + " is ruled out of every category");
		}
		// the upstream mNEIAllowed=F rows stay out the faithful way
		for (String tDisallowed : List.of("gt.recipe.chisel", "gt.recipe.autocrafting")) {
			assertFalse(GT6RecipeMapViewerMeta.eligible(RecipeMap.RECIPE_MAPS.get(tDisallowed)), tDisallowed + " carries upstream mNEIAllowed=F");
		}

		int tEligible = 0;
		Set<String> tVisible = new TreeSet<>();
		for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) {
			if (GT6RecipeMapViewerMeta.eligible(tMap)) tEligible++;
			if (GT6RecipeMapViewerMeta.visibleToViewers(tMap)) tVisible.add(tMap.mNameInternal);
		}
		assertEquals(67, tEligible, "75 census - 6 ruled-excluded - 3 upstream-disallowed + 1 overlap (furnacefuel is both) = 67 eligible");
		// the batch-1 face: the six canaries of the 2026-09-26 ruling + BEDROCK_ORE_LIST (RM.java:153)
		assertEquals(Set.of("gt.recipe.cokeoven", "gt.recipe.shredder", "gt.recipe.crusher",
				"gt.recipe.lathe", "gt.recipe.distillery", "gt.recipe.drying", "gt.recipe.bedrockorelist"), tVisible);
		// deterministic registration order (name-sorted — the factory iteration contract)
		List<RecipeMap> tVisibleMaps = GT6RecipeMapViewerMeta.visibleMaps();
		assertEquals(7, tVisibleMaps.size());
		assertEquals("gt.recipe.bedrockorelist", tVisibleMaps.get(0).mNameInternal);
	}

	// -------------------------------------------------------------------
	// The per-map metadata table pin (the upstream trailing-NEI-arg transcription)
	// -------------------------------------------------------------------

	@Test
	void metadataTableDeviationsPinned() {
		GT6RecipeMaps.init();
		// the dominant census shape: the RM.java T,T,T,T,F row (CokeOven :78 form)
		var tStandard = GT6RecipeMapViewerMeta.metaOf(GT6RecipeMaps.COKE_OVEN);
		assertTrue(tStandard.neiAllowed());
		assertTrue(tStandard.showVoltageAmperage());
		assertFalse(tStandard.combinePower());
		assertEquals("", tStandard.specialValuePre());
		assertEquals(1, tStandard.specialValueMultiplier());
		assertEquals("", tStandard.specialValuePost());

		// the fuel maps carry combinePower=T (FM.java:40-/:45 — the T,T,T,F,T rows)
		for (RecipeMap tFuel : List.of(GT6RecipeMaps.FLUIDBED, GT6RecipeMaps.BURN, GT6RecipeMaps.GAS_FUELS,
				GT6RecipeMaps.FUELS_HOT, GT6RecipeMaps.ENGINE_FUELS)) {
			assertTrue(GT6RecipeMapViewerMeta.metaOf(tFuel).combinePower(), tFuel.mNameInternal + " is a fuel map (combinePower=T)");
		}
		// FUSION carries the special-value triple (RM.java:146 "Start: %s LU")
		var tFusion = GT6RecipeMapViewerMeta.metaOf(GT6RecipeMaps.FUSION);
		assertEquals("Start: ", tFusion.specialValuePre());
		assertEquals(" LU", tFusion.specialValuePost());
		// the crucible pair carries the Temperature triple (RM.java:128/:129 — tabled though excluded)
		assertEquals("Temperature: ", GT6RecipeMapViewerMeta.metaOf(GT6RecipeMaps.CRUCIBLE_SMELTING).specialValuePre());
		assertEquals(" K", GT6RecipeMapViewerMeta.metaOf(GT6RecipeMaps.CRUCIBLE_ALLOYING).specialValuePost());
		// the mNEIAllowed=F rows read back through the same table
		assertFalse(GT6RecipeMapViewerMeta.metaOf(GT6RecipeMaps.CHISEL).neiAllowed());
		assertFalse(GT6RecipeMapViewerMeta.metaOf(GT6RecipeMaps.AUTOCRAFTER).neiAllowed());
	}

	// -------------------------------------------------------------------
	// The layout math — the NEI_RecipeMap ctor switch translation, spot-pinned per band
	// -------------------------------------------------------------------

	@Test
	void layoutItemSlotsPinnedAgainstTheNeiSwitch() {
		GT6RecipeMaps.init();
		// the ≤3 single right-anchored row, no fluids → y25 (Lathe 1/2, RM.java:97 shape)
		assertPos(GT6RecipeMapViewerMeta.inputPos(0, GT6RecipeMaps.LATHE), 53, 25);
		assertPos(GT6RecipeMapViewerMeta.outputPos(0, GT6RecipeMaps.LATHE), 107, 25);
		assertPos(GT6RecipeMapViewerMeta.outputPos(1, GT6RecipeMaps.LATHE), 125, 25);

		// the 7+ fixed 3x3 + 61-row (Shredder 1/12 out, 0 fluids — the default case rows)
		assertPos(GT6RecipeMapViewerMeta.inputPos(0, GT6RecipeMaps.SHREDDER), 53, 25); // 1 in
		assertPos(GT6RecipeMapViewerMeta.outputPos(0, GT6RecipeMaps.SHREDDER), 107, 7);
		assertPos(GT6RecipeMapViewerMeta.outputPos(2, GT6RecipeMaps.SHREDDER), 143, 7);
		assertPos(GT6RecipeMapViewerMeta.outputPos(3, GT6RecipeMaps.SHREDDER), 107, 25);
		assertPos(GT6RecipeMapViewerMeta.outputPos(8, GT6RecipeMaps.SHREDDER), 143, 43);
		assertPos(GT6RecipeMapViewerMeta.outputPos(9, GT6RecipeMaps.SHREDDER), 107, 61);
		assertPos(GT6RecipeMapViewerMeta.outputPos(11, GT6RecipeMaps.SHREDDER), 143, 61);
		assertNull(GT6RecipeMapViewerMeta.outputPos(12, GT6RecipeMaps.SHREDDER), "past the declared count = no slot");

		// the 4-6 two-row band lifts to y7/25 when in-fluids > 3 (Mixer 6 in / 6 in-fluids)
		assertPos(GT6RecipeMapViewerMeta.inputPos(0, GT6RecipeMaps.MIXER), 17, 7);
		assertPos(GT6RecipeMapViewerMeta.inputPos(5, GT6RecipeMaps.MIXER), 53, 25);
		// the same 6-in shape without the fluid lift sits at the 16/34 band (Bath 6 in / 1
		// in-fluid: 1 ≤ 3 → the 16/34 band)
		assertPos(GT6RecipeMapViewerMeta.inputPos(0, GT6RecipeMaps.BATH), 17, 16);
		assertPos(GT6RecipeMapViewerMeta.inputPos(3, GT6RecipeMaps.BATH), 17, 34);
		assertPos(GT6RecipeMapViewerMeta.inputPos(4, GT6RecipeMaps.BATH), 35, 34);
		// Distillery's raw 2-in column collapses to ONE input slot (the ctor's
		// max(in, minItem) — IN-OUT-MIN-ITEM 1,2,1 → mInputItemsCount 1, the same formula
		// upstream keys its NEI switch on) — the single-slot row at 53
		assertPos(GT6RecipeMapViewerMeta.inputPos(0, GT6RecipeMaps.DISTILLERY), 53, 25);
		assertNull(GT6RecipeMapViewerMeta.inputPos(1, GT6RecipeMaps.DISTILLERY));
		assertPos(GT6RecipeMapViewerMeta.outputPos(0, GT6RecipeMaps.DISTILLERY), 107, 25);
	}

	@Test
	void layoutFluidRowsPinned() {
		// NEI :389-390: inputs 53-(i%3)*18 / outputs 107+(i%3)*18, both at 63-(i/3)*18
		assertPos(GT6RecipeMapViewerMeta.fluidInputPos(0), 53, 63);
		assertPos(GT6RecipeMapViewerMeta.fluidInputPos(2), 17, 63);
		assertPos(GT6RecipeMapViewerMeta.fluidOutputPos(0), 107, 63);
		assertPos(GT6RecipeMapViewerMeta.fluidOutputPos(1), 125, 63);
	}

	private static void assertPos(int[] aPos, int aX, int aY) {
		assertNotNull(aPos);
		assertEquals(aX, aPos[0]);
		assertEquals(aY, aPos[1]);
	}

	// -------------------------------------------------------------------
	// The cost text — drawExtras :680-717 arithmetic pins
	// -------------------------------------------------------------------

	private static Recipe row(long aEUt, long aDuration, long aSpecialValue) {
		return new Recipe(true, new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
				new ItemStack[]{new ItemStack(Items.IRON_NUGGET)}, null, null, aDuration, aEUt, aSpecialValue);
	}

	@Test
	void costLinesPositiveNegativeAndZeroEUt() {
		GT6RecipeMaps.init();
		// CokeOven: showVoltage=T, combinePower=F, power=1 — the full six-line face
		assertEquals(List.of("Costs: 12800 GU", "Usage: 32 GU/t", "Tier: 32 GU", "Power: 1", "Time: 400 ticks"),
				GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.COKE_OVEN, row(32, 400, 0)));
		// negative EUt = the generator face (Gain/Output, NEI :699-711)
		assertEquals(List.of("Gain: 9600 GU", "Output: 16 GU/t", "Tier: 16 GU", "Power: 1", "Time: 600 ticks"),
				GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.COKE_OVEN, row(-16, 600, 0)));
		// EUt 0 + showVoltage → only the tier line (NEI :683-686)
		assertEquals(List.of("Tier: unspecified", "Time: 300 ticks"),
				GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.COKE_OVEN, row(0, 300, 0)));
		// combinePower=T drops the Usage line (the fuel-map face, NEI :690-692/:696)
		assertEquals(List.of("Costs: 800 GU", "Tier: 8 GU", "Power: 1", "Time: 100 ticks"),
				GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.ENGINE_FUELS, row(8, 100, 0)));
		// the FUSION special-value triple rides the last line (mSpecialValue × multiplier)
		assertEquals(List.of("Costs: 1600 GU", "Usage: 8 GU/t", "Tier: 8 GU", "Power: 1", "Time: 200 ticks", "Start: 131072 LU"),
				GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.FUSION, row(8, 200, 131072)));
	}

	@Test
	void timeLineThresholdsPinned() {
		assertEquals("Time: 1199 ticks", GT6RecipeMapViewerMeta.timeLine(1199));
		assertEquals("Time: 60 secs", GT6RecipeMapViewerMeta.timeLine(1200));
		assertEquals("Time: 1799 secs", GT6RecipeMapViewerMeta.timeLine(35980));
		assertEquals("Time: 30 mins", GT6RecipeMapViewerMeta.timeLine(36000));
	}

	// -------------------------------------------------------------------
	// The tooltip seams — NEI :662-666 chance / :671 not-consumed
	// -------------------------------------------------------------------

	@Test
	void chanceLinePinned() {
		assertEquals("Chance: 50.00%", GT6RecipeMapViewerMeta.chanceLine(5000, 1));
		assertEquals("Chance: 12.34%", GT6RecipeMapViewerMeta.chanceLine(1234, 1));
		assertEquals("Chance: 50.00% each", GT6RecipeMapViewerMeta.chanceLine(5000, 3), "stackSize>1 → the ' each' suffix");
		assertEquals("Chance: 5.00%", GT6RecipeMapViewerMeta.chanceLine(500, 1), "the <10 fraction pad");
		assertNull(GT6RecipeMapViewerMeta.chanceLine(10000, 1), "the folded max reads as deterministic — no tooltip");
		assertNull(GT6RecipeMapViewerMeta.chanceLine(0, 1));
	}

	@Test
	void outputChanceReadsTheFoldedMaxSemantics() {
		// null chances → 10000; a short array reads 10000 past its end (Recipe.mChances doc)
		Recipe tRow = new Recipe(true, new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
				new ItemStack[]{new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.GOLD_NUGGET)},
				null, null, 100, 8, 0, new long[]{2500});
		assertEquals(2500, GT6RecipeMapViewerMeta.outputChance(tRow, 0));
		assertEquals(10000, GT6RecipeMapViewerMeta.outputChance(tRow, 1));
		assertEquals(10000, GT6RecipeMapViewerMeta.outputChance(tRow, 9));
	}

	@Test
	void notConsumedTextIsTheUpstreamLiteral() {
		assertEquals("Does not get consumed in the process", GT6RecipeMapViewerMeta.NOT_CONSUMED_TEXT);
	}

	// -------------------------------------------------------------------
	// The JEI category's offline surface (the uid/title/dims contract)
	// -------------------------------------------------------------------

	@Test
	void jeiCategoryOfflineSurface() {
		GT6RecipeMaps.init();
		GT6RecipeMapJeiCategory tCategory = new GT6RecipeMapJeiCategory(GT6RecipeMaps.COKE_OVEN);
		assertEquals("gt6:recipe_map/gt.recipe.cokeoven", tCategory.getRecipeType().getUid().toString());
		assertEquals(Recipe.class, tCategory.getRecipeType().getRecipeClass());
		assertEquals("Coke Oven", tCategory.getTitle().getString());
		assertEquals(GT6RecipeMapViewerMeta.CATEGORY_WIDTH, tCategory.getWidth());
		assertEquals(GT6RecipeMapViewerMeta.CATEGORY_HEIGHT, tCategory.getHeight());
	}
}
