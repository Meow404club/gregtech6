/**
 * Offline guard tests for tasks debt-jei-emi-batch1 + debt-jei-emi-batch2: the shared
 * per-map category metadata table (the census pin, batch 2's full visible opening
 * included), the NEI layout-switch translation, the drawExtras cost-text arithmetic and
 * the chance/not-consumed tooltip seams — the exact faces the
 * JEI category (GT6RecipeMapJeiCategory) and the EMI twin (gregtech6.emi) both render
 * from, so pinning them here pins both legs' geometry at the one shared place. The JEI
 * category's own offline surface (uid/title/dims) rides the last test — RecipeType is a
 * pure value class, safe in a bare JVM.
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

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
		assertEquals(83, RecipeMap.RECIPE_MAPS.size(), "the merged-shape census (rm-six-maps' Microwave/Cooker/ToolHeads/Mortar/Hammer five maps landed by rebase — the pre-rebase branch pinned 75; +3 task recipe-b6b-small-maps-cnc-assembler-nanofab — the Nanofab/Assembler/CNC trio, RM.java:140/:159/:160)");

		// the ruled exclusion table (r-jei-emi-coverage id927) never enters a category
		// (crucible-viewer-page: the crucible pair LEFT the table — their page rides the
		// rowsOf synthesis off the material graph)
		for (String tExcluded : List.of("mc.recipe.furnace", "mc.recipe.furnacefuel",
				"gt.recipe.bumblelyzer", "gt.recipe.plantalyzer")) {
			assertFalse(GT6RecipeMapViewerMeta.eligible(RecipeMap.RECIPE_MAPS.get(tExcluded)), tExcluded + " is ruled out of every category");
		}
		// the crucible pair is ELIGIBLE now — the user-visible point of crucible-viewer-page
		for (String tCrucible : List.of("gt.recipe.cruciblesmelting", "gt.recipe.cruciblealloying")) {
			assertTrue(GT6RecipeMapViewerMeta.eligible(RecipeMap.RECIPE_MAPS.get(tCrucible)), tCrucible + " joins the category face");
		}
		// the upstream mNEIAllowed=F rows stay out the faithful way (the :159/:160 pair joined
		// the table with task recipe-b6b — upstream ships Assembler/CNC with aNEIAllowed=F)
		for (String tDisallowed : List.of("gt.recipe.chisel", "gt.recipe.autocrafting",
				"gt.recipe.assembler", "gt.recipe.cncmachine")) {
			assertFalse(GT6RecipeMapViewerMeta.eligible(RecipeMap.RECIPE_MAPS.get(tDisallowed)), tDisallowed + " carries upstream mNEIAllowed=F");
		}

		int tEligible = 0;
		Set<String> tVisible = new TreeSet<>();
		for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) {
			if (GT6RecipeMapViewerMeta.eligible(tMap)) tEligible++;
			if (GT6RecipeMapViewerMeta.visibleToViewers(tMap)) tVisible.add(tMap.mNameInternal);
		}
		assertEquals(75, tEligible, "83 census - 4 ruled-excluded - 5 upstream-disallowed + 1 overlap (furnacefuel is both) = 75 eligible (crucible-viewer-page lifted the crucible pair; the b6b trio joins with Nanofab eligible and the Assembler/CNC pair disallowed)");
		// batch 2 full opening (task debt-jei-emi-batch2): visibility IS eligibility — the
		// batch-1 canaries (cokeoven/shredder/crusher/lathe/distillery/drying + the
		// RM.java:153 bedrockorelist display map) ride along automatically; the closure is
		// exactly the exclusion table + the mNEIAllowed=F pair asserted above
		assertEquals(tEligible, tVisible.size(), "the visible set is the whole eligible set");
		assertTrue(tVisible.containsAll(List.of("gt.recipe.cokeoven", "gt.recipe.shredder", "gt.recipe.crusher",
				"gt.recipe.lathe", "gt.recipe.distillery", "gt.recipe.drying", "gt.recipe.bedrockorelist")),
				"the batch-1 canaries stay visible under the full opening");
		// deterministic registration order (name-sorted — the factory iteration contract)
		List<String> tVisibleMapNames = new ArrayList<>();
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps()) tVisibleMapNames.add(tMap.mNameInternal);
		assertEquals(new ArrayList<>(tVisible), tVisibleMapNames);
	}

	/**
	 * The rowsOf seam (crucible-viewer-page): the ONE row source both viewer legs register.
	 * Default arm = the live mRecipeList as a defensive copy; the crucible arms = the
	 * material-graph synthesis, NOT the (empty) live list — the PhaseGate double-zero pin
	 * stays untouched because the synthetic rows are display-only.
	 */
	@Test
	void rowsOfSeamFeedsBothViewerLegs() {
		GT6RecipeMaps.init();
		// the default arm: an equal copy of the live list, never the same instance
		List<Recipe> tLatheRows = GT6RecipeMapViewerMeta.rowsOf(GT6RecipeMaps.LATHE);
		assertEquals(new ArrayList<>(GT6RecipeMaps.LATHE.mRecipeList), tLatheRows,
				"the default arm is a defensive copy of the live list (List semantics — the live list is a Set)");
		// the crucible arms: synthesized (non-null), deterministic, and the live lists stay EMPTY
		List<Recipe> tSmeltingRows = GT6RecipeMapViewerMeta.rowsOf(GT6RecipeMaps.CRUCIBLE_SMELTING);
		List<Recipe> tAlloyingRows = GT6RecipeMapViewerMeta.rowsOf(GT6RecipeMaps.CRUCIBLE_ALLOYING);
		assertNotNull(tSmeltingRows);
		assertNotNull(tAlloyingRows);
		// determinism: same size + same input/output item fingerprint (Recipe carries no
		// content equals, so row-level equality is identity — useless across walks; the
		// fingerprint travels: the forge leg answers the live mat() resolver EMPTY, the neo
		// leg resolves real items, and both must walk the SAME universe twice)
		assertEquals(fingerprint(tSmeltingRows), fingerprint(GT6RecipeMapViewerMeta.rowsOf(GT6RecipeMaps.CRUCIBLE_SMELTING)),
				"the walk is deterministic; the CONTENT pins live in GT6RecipeMapCrucibleTest");
		assertEquals(fingerprint(tAlloyingRows), fingerprint(GT6RecipeMapViewerMeta.rowsOf(GT6RecipeMaps.CRUCIBLE_ALLOYING)));
		assertTrue(GT6RecipeMaps.CRUCIBLE_SMELTING.mRecipeList.isEmpty(), "synthetic rows never enter the live list (PhaseGate double-0)");
		assertTrue(GT6RecipeMaps.CRUCIBLE_ALLOYING.mRecipeList.isEmpty(), "synthetic rows never enter the live list (PhaseGate double-0)");
	}

	/** The leg-neutral walk fingerprint: every row's input/output item classes in walk order. */
	private static List<String> fingerprint(List<Recipe> aRows) {
		List<String> rPrint = new ArrayList<>();
		for (Recipe tRow : aRows) {
			StringBuilder tBuilder = new StringBuilder();
			for (ItemStack tInput : tRow.mInputs) tBuilder.append(tInput.getItem()).append('x').append(tInput.getCount()).append('|');
			tBuilder.append("->");
			for (ItemStack tOutput : tRow.mOutputs) tBuilder.append(tOutput.getItem()).append('x').append(tOutput.getCount()).append('|');
			rPrint.add(tBuilder.toString());
		}
		return rPrint;
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
		// furnacefuel: aShowVoltageAmperageInNEI is T on the FM.java:38 row tail (T,F,...)
		// — batch 1 had transcribed F; a dead value (the map is excluded and never renders)
		// but the table is a transcription, so the batch-2 review correction is pinned
		var tFurnaceFuel = GT6RecipeMapViewerMeta.metaOf(GT6RecipeMaps.FURNACE_FUEL);
		assertFalse(tFurnaceFuel.neiAllowed());
		assertTrue(tFurnaceFuel.showVoltageAmperage());
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

	// -------------------------------------------------------------------
	// The viewer backdrop (task 34-viewer-gui-bg, GitHub #34): the sOffset fold
	// table, the two-layer crop quadruples and the retired text band — the coordinates
	// the JEI draw()/setRecipe and the EMI addWidgets legs all consume through the
	// viewer* exits below (the raw inputPos/outputPos/fluid*Pos stay in machine-GUI
	// coordinates, the upstream switch transcription — pinned in the tests above).
	// -------------------------------------------------------------------

	@Test
	void viewerFoldTablePinnedAgainstTheNeiNumbers() {
		GT6RecipeMaps.init();
		// the fold is the re-anchored -(5,7) everywhere (task viewer-row-headroom): NEI
		// :66's sOffsetY 11 minus the 4px headroom the modern zero-headroom category
		// rects can't show (the S_OFFSET_Y doc carries the full mechanism)
		assertEquals(5, GT6RecipeMapViewerMeta.S_OFFSET_X);
		assertEquals(7, GT6RecipeMapViewerMeta.S_OFFSET_Y);
		// BATH (6 in / 1 in-fluid → the 16/34 band, 3 item outputs at row 0 y16): GUI
		// (17,16)/(35,34)/(107,16)/(125,16)/(143,16) → panel (12,9)/(30,27)/(102,9)/
		// (120,9)/(138,9) — the r9-34 pins (12,5)/(30,23)/(102,5)/(120,5)/(138,5)
		// translated by the re-anchor's pure +4; the fluid input GUI (53,63) → (48,56)
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(0, GT6RecipeMaps.BATH), 12, 9);
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(4, GT6RecipeMaps.BATH), 30, 27);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(0, GT6RecipeMaps.BATH), 102, 9);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(1, GT6RecipeMaps.BATH), 120, 9);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(2, GT6RecipeMaps.BATH), 138, 9);
		assertPos(GT6RecipeMapViewerMeta.viewerFluidInputPos(0), 48, 56);
		assertPos(GT6RecipeMapViewerMeta.viewerFluidOutputPos(1), 120, 56);
		// MIXER (6 in / 6 in-fluids → the y7 lift): the former negative-row case — GUI
		// (17,7)/(53,25) → panel (12,0)/(48,18). Pre-fix (12,-4) was upstream-faithful
		// (the NEI FixedPositionedStack carried it) but the modern category rect starts
		// AT its origin — the user-visible "首行被裁剪" (task viewer-row-headroom); the
		// +4 re-anchor lands the topmost slot row at exactly y0.
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(0, GT6RecipeMaps.MIXER), 12, 0);
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(1, GT6RecipeMaps.MIXER), 30, 0);
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(5, GT6RecipeMaps.MIXER), 48, 18);
		// STEAM_CRACKING (1 in / 3 out / 9 out-fluids → outputs lift to y7): the output
		// side's former negative row — GUI (53,25)/(107,7)/(143,7) → (48,18)/(102,0)/(138,0)
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(0, GT6RecipeMaps.STEAM_CRACKING), 48, 18);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(0, GT6RecipeMaps.STEAM_CRACKING), 102, 0);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(2, GT6RecipeMaps.STEAM_CRACKING), 138, 0);
		// FUSION (2 in / 6+6 out, fluids >3 both sides): the two-row 7/25 output band
		// folded — GUI (35,25)/(107,7)/(143,25) → (30,18)/(102,0)/(138,18)
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(0, GT6RecipeMaps.FUSION), 30, 18);
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(1, GT6RecipeMaps.FUSION), 48, 18);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(0, GT6RecipeMaps.FUSION), 102, 0);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(5, GT6RecipeMaps.FUSION), 138, 18);
		// the null contract passes through the fold untouched
		assertNull(GT6RecipeMapViewerMeta.viewerOutputPos(12, GT6RecipeMaps.SHREDDER), "past the 12th drawn slot: null in, null out");
	}

	/**
	 * The zero-negative census (task viewer-row-headroom acceptance ①): every visible
	 * map × every declared slot index, all four viewer exits return y ≥ 0 (null
	 * excepted) — nothing the modern JEI/EMI category rects can clip at the top any
	 * more. The pre-fix negative set (same loop over the pre-re-anchor fold, i.e. any
	 * current y < 4) is pinned as the exact 21-map list the live census produces: the
	 * 7+-item-slot maps plus the fluid-lift maps (the 4-6-item two-row band or the 1-3
	 * row lifting to y7 under >3/>6 fluids) — PRINTER has 4+ fluids but ≤3 items and
	 * ≤6 of them, so its row stays at y25 and it is NOT affected; any future map
	 * entering the set must re-run this acceptance deliberately.
	 */
	@Test
	void everyViewerExitStaysNonNegativeAcrossTheWholeVisibleCensus() {
		GT6RecipeMaps.init();
		Set<String> tPreFixNegative = new TreeSet<>();
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps()) {
			for (int i = 0; i < tMap.mInputItemsCount; i++) {
				int[] tPos = GT6RecipeMapViewerMeta.viewerInputPos(i, tMap);
				if (tPos == null) continue;
				assertTrue(tPos[1] >= 0, tMap.mNameInternal + " input " + i + " at y" + tPos[1] + " — clipped again");
				if (tPos[1] < 4) tPreFixNegative.add(tMap.mNameInternal); // pre-fix y = current − 4
			}
			for (int i = 0; i < tMap.mOutputItemsCount; i++) {
				int[] tPos = GT6RecipeMapViewerMeta.viewerOutputPos(i, tMap);
				if (tPos == null) continue;
				assertTrue(tPos[1] >= 0, tMap.mNameInternal + " output " + i + " at y" + tPos[1] + " — clipped again");
				if (tPos[1] < 4) tPreFixNegative.add(tMap.mNameInternal);
			}
			for (int i = 0; i < tMap.mInputFluidCount; i++) {
				int[] tPos = GT6RecipeMapViewerMeta.viewerFluidInputPos(i);
				assertTrue(tPos[1] >= 0, tMap.mNameInternal + " fluid input " + i + " at y" + tPos[1] + " — clipped again");
				if (tPos[1] < 4) tPreFixNegative.add(tMap.mNameInternal);
			}
			for (int i = 0; i < tMap.mOutputFluidCount; i++) {
				int[] tPos = GT6RecipeMapViewerMeta.viewerFluidOutputPos(i);
				assertTrue(tPos[1] >= 0, tMap.mNameInternal + " fluid output " + i + " at y" + tPos[1] + " — clipped again");
				if (tPos[1] < 4) tPreFixNegative.add(tMap.mNameInternal);
			}
		}
		// the affected list, in the record: 9 maps with a 7+-slot side + 12 fluid-lift
		// maps; canaries MIXER (6 in-items lifted by 6 in-fluids), STEAM_CRACKING (its
		// OUTPUTS lifted by 9 out-fluids) and FUSION (both) ride the r9-34 fold-table pins.
		// crucible-viewer-page deliberately re-ran the acceptance for the newly visible
		// CRUCIBLE_ALLOYING (12/12 — the 7+ band's y7 folds to viewer y0, the same shape
		// as SHREDDER); CRUCIBLE_SMELTING (6/6, the y16/34 two-row band) stays out.
		assertEquals(Set.of("gt.recipe.bedrockorelist", "gt.recipe.burnmixer", "gt.recipe.catalyticcracking",
				"gt.recipe.centrifuge", "gt.recipe.cokeoven", "gt.recipe.cooker", "gt.recipe.cruciblealloying",
				"gt.recipe.crusher", "gt.recipe.cryodistillationtower", "gt.recipe.cryomixer",
				"gt.recipe.distillationtower", "gt.recipe.electrolyzer", "gt.recipe.fusionreactor",
				"gt.recipe.lightning", "gt.recipe.magneticseparator", "gt.recipe.mixer",
				"gt.recipe.shredder", "gt.recipe.sifter", "gt.recipe.sluice", "gt.recipe.steamcracking",
				"gt.recipe.unboxinator", "gt.recipe.welder"),
				tPreFixNegative, "the pre-fix negative-row census drifted — re-run the headroom acceptance");
		assertTrue(tPreFixNegative.containsAll(List.of("gt.recipe.mixer", "gt.recipe.steamcracking", "gt.recipe.fusionreactor")),
				"the three named canaries must be among the affected");
	}

	/**
	 * The relative-geometry pin (task viewer-row-headroom acceptance ②): the re-anchor
	 * is a pure +4 translation — the element-to-element invariants that held pre-fix
	 * hold post-fix, each reading off the constants it relates. The ABSOLUTE pre/post
	 * values ride the fold-table and crop pins above (BATH 5→9, MIXER −4→0, fluid
	 * 52→56, text 73→77 — every diff exactly +4 in y; the gear-spot icon row was
	 * retired by task viewer-icon-retire-gu-pin and no longer rides the census).
	 */
	@Test
	void headroomReAnchorIsAPurePlusFourTranslation() {
		GT6RecipeMaps.init();
		// the 18px row pitch is fold-independent (the raw switch geometry)
		assertEquals(18, GT6RecipeMapViewerMeta.viewerInputPos(5, GT6RecipeMaps.MIXER)[1]
				- GT6RecipeMapViewerMeta.viewerInputPos(0, GT6RecipeMaps.MIXER)[1]);
		assertEquals(18, GT6RecipeMapViewerMeta.viewerFluidOutputPos(0)[1]
				- GT6RecipeMapViewerMeta.viewerFluidOutputPos(3)[1],
				"the fluid grid stacks upward at the same 18px pitch");
	}

	@Test
	void backdropCropsTextBandAndFurniturePinnedToTheUpstreamDraw() {
		// the plate crop: the NEI_RecipeMap.drawBackground :632 number carried to the
		// re-anchored panel system (task viewer-row-headroom) — (-5,-16,0,0,176,166)
		// → v 16−4 = 12, h 140 (the full category). Pre-fix pin: {5,16,166,140}.
		// The per-map machine-band crop is RETIRED (composed-ui-energy-slot-and-parts:
		// the whole-image band baked slot frames the code slots doubled) — the composed
		// page furniture (arrow cell, gear slot) rides the folded exits instead.
		assertArrayEquals(new int[] {5, 12, 166, 140}, GT6RecipeMapViewerMeta.PLATE_CROP);
		assertEquals("gt6:textures/gui/machines/nei.png", GT6RecipeMapViewerMeta.PLATE_TEXTURE.toString());
		// the furniture cells: the arrow cell at the skin print (78,24) → panel (73,17),
		// the gear slot at (77,60) → (72,53) — the machine-GUI constants fold -(5,7)
		assertArrayEquals(new int[] {73, 17}, GT6RecipeMapViewerMeta.viewerArrowPos());
		assertArrayEquals(new int[] {72, 53}, GT6RecipeMapViewerMeta.viewerGearPos());
		// the retired text band: the drawExtras lines sit at the FIXED panel y77 for every
		// map (the pre-fix 73 + the re-anchor's 4 — still 3px under the old band bottom edge
		// 74; the +10 fluid deviation stays dead; FUSION's 2 lines since the
		// r11-tu-costlines-slim slim-down end at 81 < 140)
		GT6RecipeMaps.init();
		for (RecipeMap tMap : List.of(GT6RecipeMaps.BATH, GT6RecipeMaps.MIXER, GT6RecipeMaps.FUSION,
				GT6RecipeMaps.LATHE, GT6RecipeMaps.STEAM_CRACKING))
			assertEquals(77, GT6RecipeMapViewerMeta.TEXT_BASE_Y, tMap.mNameInternal + " rides the fixed panel band");
	}

	// -------------------------------------------------------------------
	// The switch tail's dead branch (batch 2 review note): the upstream case 10/11
	// fourth-row anchoring and the 12-slots-drawn cap, transcribed faithfully and proven
	// dead on the live census
	// -------------------------------------------------------------------

	@Test
	void censusProvesTheTenElevenAndOverTwelveSlotShapesStayDead() {
		GT6RecipeMaps.init();
		// no live map declares 10/11 item slots on either side (the case 10/11 branch is
		// dead upstream too — the batch-1 review note) and none declares more than 12 (the
		// cap branch). If this ever fails, the faithful tail below went LIVE and the
		// anchoring must be re-verified against real rows, not just the synthetic pins.
		for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) {
			assertFalse(tMap.mInputItemsCount == 10 || tMap.mInputItemsCount == 11 || tMap.mInputItemsCount > 12,
					tMap.mNameInternal + " inputs hit the dead branch");
			assertFalse(tMap.mOutputItemsCount == 10 || tMap.mOutputItemsCount == 11 || tMap.mOutputItemsCount > 12,
					tMap.mNameInternal + " outputs hit the dead branch");
		}
		// the widest live shape is the 12-slot default (Shredder outputs) — the cap boundary
		assertEquals(12, GT6RecipeMaps.SHREDDER.mOutputItemsCount);
	}

	@Test
	void deadBranchFourthRowAnchoringPinnedAgainstTheUpstreamCases() {
		GT6RecipeMaps.init();
		// synthetic maps at the dead counts (reset in @AfterEach keeps them out of the
		// census scan); ctor counts are the raw switch keys
		RecipeMap tTen = deadMap("gt.recipe.batch2dead10", 10, 10);
		RecipeMap tEleven = deadMap("gt.recipe.batch2dead11", 11, 11);
		RecipeMap tThirteen = deadMap("gt.recipe.batch2dead13", 13, 13);
		// case 10 — the lone fourth-row slot hugs the grid's LAST column on BOTH sides
		// (inputs x53, NEI_RecipeMap.java:246; outputs x143, :358 — NOT the row's first
		// column the batch-1 arithmetic produced)
		assertPos(GT6RecipeMapViewerMeta.inputPos(9, tTen), 53, 61);
		assertPos(GT6RecipeMapViewerMeta.outputPos(9, tTen), 143, 61);
		// case 11 — the pair fills {last-1, last} (:258-259 / :370-371)
		assertPos(GT6RecipeMapViewerMeta.inputPos(9, tEleven), 35, 61);
		assertPos(GT6RecipeMapViewerMeta.inputPos(10, tEleven), 53, 61);
		assertPos(GT6RecipeMapViewerMeta.outputPos(9, tEleven), 125, 61);
		assertPos(GT6RecipeMapViewerMeta.outputPos(10, tEleven), 143, 61);
		// 12+ (the default cases) — full row from the FIRST column (:271-273 / :383-385),
		// and the 13th slot is never drawn (no case renders past the 12th)
		assertPos(GT6RecipeMapViewerMeta.inputPos(9, tThirteen), 17, 61);
		assertPos(GT6RecipeMapViewerMeta.outputPos(11, tThirteen), 143, 61);
		assertNull(GT6RecipeMapViewerMeta.inputPos(12, tThirteen));
		assertNull(GT6RecipeMapViewerMeta.outputPos(12, tThirteen));
		// the live 12-slot default (Shredder outputs) is untouched by the cap
		assertPos(GT6RecipeMapViewerMeta.outputPos(11, GT6RecipeMaps.SHREDDER), 143, 61);
	}

	/** A census-shaped synthetic map at the given raw slot counts (the 15-arg port ctor). */
	private static RecipeMap deadMap(String aName, int aInItems, int aOutItems) {
		return new RecipeMap(new ArrayList<>(), aName, "Batch2 Dead Branch", null, 0, 1,
				"gt6:textures/gui/machines/mixer", aInItems, aOutItems, 1, 0, 0, 0, 0, 1);
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

	/** The translatable contents of one cost line (the issues #29/#34a key+args face). */
	private static net.minecraft.network.chat.contents.TranslatableContents contents(net.minecraft.network.chat.Component aLine) {
		return (net.minecraft.network.chat.contents.TranslatableContents) aLine.getContents();
	}

	@Test
	void costLinesPositiveNegativeAndZeroEUt() {
		GT6RecipeMaps.init();
		// CokeOven: showVoltage=T, combinePower=F, power=1 — the full six-line face
		var tLines = GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.COKE_OVEN, row(32, 400, 0));
		assertEquals("gt6.jei.cost.costs", contents(tLines.get(0)).getKey());
		assertArrayEquals(new Object[]{12800L}, contents(tLines.get(0)).getArgs());
		assertEquals("gt6.jei.cost.usage", contents(tLines.get(1)).getKey());
		assertArrayEquals(new Object[]{32L}, contents(tLines.get(1)).getArgs());
		assertEquals("gt6.jei.cost.tier", contents(tLines.get(2)).getKey());
		assertEquals("gt6.jei.cost.power", contents(tLines.get(3)).getKey());
		assertEquals("gt6.jei.cost.time", contents(tLines.get(4)).getKey());
		// negative EUt = the generator face (Gain/Output, NEI :699-711)
		var tGainLines = GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.COKE_OVEN, row(-16, 600, 0));
		assertEquals("gt6.jei.cost.gain", contents(tGainLines.get(0)).getKey());
		assertArrayEquals(new Object[]{9600L}, contents(tGainLines.get(0)).getArgs());
		assertEquals("gt6.jei.cost.output", contents(tGainLines.get(1)).getKey());
		// EUt 0 + showVoltage → only the tier-unspecified line (NEI :683-686)
		var tZeroLines = GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.COKE_OVEN, row(0, 300, 0));
		assertEquals("gt6.jei.cost.tier_unspecified", contents(tZeroLines.get(0)).getKey());
		assertEquals("gt6.jei.cost.time", contents(tZeroLines.get(1)).getKey());
		// combinePower=T drops the Usage line (the fuel-map face, NEI :690-692/:696)
		var tFuelLines = GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.ENGINE_FUELS, row(8, 100, 0));
		assertEquals("gt6.jei.cost.costs", contents(tFuelLines.get(0)).getKey());
		assertEquals("gt6.jei.cost.tier", contents(tFuelLines.get(1)).getKey());
		assertEquals("gt6.jei.cost.power", contents(tFuelLines.get(2)).getKey());
		assertEquals("gt6.jei.cost.time", contents(tFuelLines.get(3)).getKey());
		assertEquals(4, tFuelLines.size(), "combinePower=T carries no Usage line");
		// the FUSION special-value triple rides the last line (mSpecialValue × multiplier,
		// the post unit folded into the second arg slot) — last of TWO lines since the
		// r11-tu-costlines-slim slim-down: FUSION is TU-pinned, time + Start only
		var tFusionLines = GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.FUSION, row(8, 200, 131072));
		var tSpecial = contents(tFusionLines.get(1));
		assertEquals("gt6.jei.cost.start", tSpecial.getKey());
		assertArrayEquals(new Object[]{131072L, " LU"}, tSpecial.getArgs());
	}

	@Test
	void timeLineThresholdsPinned() {
		pinTimeLine(GT6RecipeMapViewerMeta.timeLine(1199), 1199L, "gt6.jei.cost.unit_ticks");
		pinTimeLine(GT6RecipeMapViewerMeta.timeLine(1200), 60L, "gt6.jei.cost.unit_secs");
		pinTimeLine(GT6RecipeMapViewerMeta.timeLine(35980), 1799L, "gt6.jei.cost.unit_secs");
		pinTimeLine(GT6RecipeMapViewerMeta.timeLine(36000), 30L, "gt6.jei.cost.unit_mins");
	}

	/** The time line = the key, the folded number and the nested unit component. */
	private static void pinTimeLine(net.minecraft.network.chat.Component aLine, long aNumber, String aUnitKey) {
		var tContents = contents(aLine);
		assertEquals("gt6.jei.cost.time", tContents.getKey());
		assertEquals(aNumber, tContents.getArgs()[0]);
		assertEquals(aUnitKey, contents((net.minecraft.network.chat.Component) tContents.getArgs()[1]).getKey());
	}

	// -------------------------------------------------------------------
	// The tooltip seams — NEI :662-666 chance / :671 not-consumed
	// -------------------------------------------------------------------

	@Test
	void chanceLinePinned() {
		var tPlain = contents(GT6RecipeMapViewerMeta.chanceLine(5000, 1));
		assertEquals("gt6.jei.cost.chance", tPlain.getKey());
		assertArrayEquals(new Object[]{"50.00%"}, tPlain.getArgs());
		assertEquals("gt6.jei.cost.chance", contents(GT6RecipeMapViewerMeta.chanceLine(1234, 1)).getKey());
		var tEach = contents(GT6RecipeMapViewerMeta.chanceLine(5000, 3));
		assertEquals("gt6.jei.cost.chance_each", tEach.getKey(), "stackSize>1 → the ' each' suffix face");
		assertArrayEquals(new Object[]{"50.00%"}, tEach.getArgs());
		assertEquals("5.00%", GT6RecipeMapViewerMeta.chanceLine(500, 1).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getArgs()[0] : "",
				"the <10 fraction pad");
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

	// -------------------------------------------------------------------
	// The JEI category's offline surface (the uid/title/dims contract)
	// -------------------------------------------------------------------

	@Test
	void jeiCategoryOfflineSurface() {
		GT6RecipeMaps.init();
		GT6RecipeMapJeiCategory tCategory = new GT6RecipeMapJeiCategory(GT6RecipeMaps.COKE_OVEN, null /* the icon rides the plugin, not the offline surface */);
		assertEquals("gt6:recipe_map/gt.recipe.cokeoven", tCategory.getRecipeType().getUid().toString());
		assertEquals(Recipe.class, tCategory.getRecipeType().getRecipeClass());
		// the issues #29/#34a title face: translatable, keyed by the shared formula (the oregen
		// precedent — offline getString() rides Language.loadDefault back to the bare key,
		// so the pin is the TranslatableContents key, not a literal)
		var tTitle = tCategory.getTitle().getContents();
		assertTrue(tTitle instanceof net.minecraft.network.chat.contents.TranslatableContents, "getTitle is translatable, not literal");
		assertEquals("gt6.jei.recipe_map.cokeoven", ((net.minecraft.network.chat.contents.TranslatableContents) tTitle).getKey());
		assertEquals(GT6RecipeMapViewerMeta.CATEGORY_WIDTH, tCategory.getWidth());
		assertEquals(GT6RecipeMapViewerMeta.CATEGORY_HEIGHT, tCategory.getHeight());
	}

	// -------------------------------------------------------------------
	// Task debt-viewer-polish: the nojade decoupling + the gear-spot machine icon
	// -------------------------------------------------------------------

	/**
	 * The known_bugs r934_nojade_recipe_page_draw_ncdfe mechanism proof: on a no-Jade
	 * runtime the first recipe-page draw died in NoClassDefFoundError because THIS class's
	 * bytecode referenced the Jade integration class (the old energyUnit →
	 * GT6MachineProvider.energyTypeShortCode edge; Jade's API is compileOnly). A class can
	 * only trigger a load through a reference in its constant pool, so the decoupling is
	 * provable at the byte layer — the same offline-proof layer the GT6JeiPluginTest
	 * annotation guard reads. Absent strings: any snownee Jade API AND the gregtech6.jade
	 * integration package.
	 */
	@Test
	void viewerMetaBytecodeCarriesNoJadeReference() throws Exception {
		try (java.io.InputStream tIn = GT6RecipeMapViewerMeta.class.getResourceAsStream("GT6RecipeMapViewerMeta.class")) {
			assertNotNull(tIn, "meta class resource not found on the test classpath");
			String tBytes = new String(tIn.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			assertFalse(tBytes.contains("snownee"), "a Jade API class leaked into the viewer seam's constant pool — nojade runtimes NCDFE again");
			assertFalse(tBytes.contains("gregtech6/jade"), "the Jade integration class is referenced from the viewer seam — nojade runtimes NCDFE again");
		}
	}

	/**
	 * The moved p27 short-code face: the table now lives here (the provider delegates to
	 * it — its own pins stay green in GT6MachineProviderTest). Values verbatim, and the
	 * unknown-carrier fallback still folds the mName prefix (null carrier folds to "").
	 */
	@Test
	void energyTypeShortCodeLivesOnTheJadeFreeSeam() {
		assertEquals("EU", GT6RecipeMapViewerMeta.energyTypeShortCode(gregapi.data.TD.Energy.EU));
		assertEquals("RU", GT6RecipeMapViewerMeta.energyTypeShortCode(gregapi.data.TD.Energy.RU));
		assertEquals("Steam", GT6RecipeMapViewerMeta.energyTypeShortCode(gregapi.data.TD.Energy.STEAM));
		assertEquals("", GT6RecipeMapViewerMeta.energyTypeShortCode(null));
	}

	/**
	 * The retirement census (task viewer-icon-retire-gu-pin, the user ruling): the EMI
	 * workstation list (RecipeScreen.java:203-217) and the JEI catalyst column
	 * (RecipesGui.java:635-636 → RecipeCatalysts) both render the machine column from the
	 * already-registered data (GT6EmiPlugin:157 / GT6JeiPlugin:164-167), so the hand-drawn
	 * gear spot is gone for good — all three meta exits (GUI_MACHINE_ICON_POS /
	 * machineIconPos / machineIcon) and every production consumer must stay deleted.
	 * Source-level pin on purpose (the GT6JadeTooltipKeyPinTest posture): the consumer
	 * deletions are exactly what a behavioral test cannot see once the exits are gone.
	 * Comments are stripped so this retirement note and the leg notes citing the retired
	 * names don't trip the scan.
	 */
	@Test
	void machineIconExitsAreFullyRetiredZeroResidualCensus() throws IOException {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		try (Stream<Path> tSources = Files.walk(tMdk.resolve("src/main/java/gregtech6"))) {
			for (Path tFile : tSources.filter(p -> p.toString().endsWith(".java")).toList()) {
				String tCode = stripComments(Files.readString(tFile));
				assertFalse(tCode.contains("machineIcon") || tCode.contains("MACHINE_ICON"),
						tFile + " still references the retired gear-spot icon exits — EMI/JEI render "
								+ "the machine column themselves; re-introducing the hand-drawn draw "
								+ "(or any new exit) must widen this pin consciously");
			}
		}
	}

	/** Drops block then line comments (the GT6JadeTooltipKeyPinTest regex, verbatim). */
	private static String stripComments(String aSource) {
		return aSource.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
	}

	/** Climb from the working directory to the mdk root (the jade pin's posture). */
	private static Path locateMdkRoot() {
		Path tDir = Path.of("").toAbsolutePath();
		for (int i = 0; i < 8 && tDir != null; i++, tDir = tDir.getParent()) {
			if (Files.isRegularFile(tDir.resolve("src/main/java/gregtech6/jei/GT6RecipeMapViewerMeta.java"))) {
				return tDir;
			}
		}
		return null;
	}
}
