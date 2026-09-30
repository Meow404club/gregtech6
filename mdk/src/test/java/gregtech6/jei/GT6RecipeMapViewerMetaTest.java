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

import java.util.ArrayList;
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
		assertEquals(80, RecipeMap.RECIPE_MAPS.size(), "the merged-shape census (rm-six-maps' Microwave/Cooker/ToolHeads/Mortar/Hammer five maps landed by rebase — the pre-rebase branch pinned 75)");

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
		assertEquals(72, tEligible, "80 census - 6 ruled-excluded - 3 upstream-disallowed + 1 overlap (furnacefuel is both) = 72 eligible (rm-six-maps' five maps are all upstream mNEIAllowed=T standard rows)");
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
	// The viewer backdrop (task r9-34-viewer-gui-bg, GitHub #34): the sOffset fold
	// table, the two-layer crop quadruples and the retired text band — the coordinates
	// the JEI draw()/setRecipe and the EMI addWidgets legs all consume through the
	// viewer* exits below (the raw inputPos/outputPos/fluid*Pos stay in machine-GUI
	// coordinates, the upstream switch transcription — pinned in the tests above).
	// -------------------------------------------------------------------

	@Test
	void viewerFoldTablePinnedAgainstTheNeiNumbers() {
		GT6RecipeMaps.init();
		// the fold is exactly -(5,11) everywhere (NEI_RecipeMap.java:66/:112)
		assertEquals(5, GT6RecipeMapViewerMeta.S_OFFSET_X);
		assertEquals(11, GT6RecipeMapViewerMeta.S_OFFSET_Y);
		// BATH (6 in / 1 in-fluid → the 16/34 band, 3 item outputs at row 0 y16): GUI
		// (17,16)/(35,34)/(107,16)/(125,16)/(143,16) → panel (12,5)/(30,23)/(102,5)/
		// (120,5)/(138,5); the fluid input GUI (53,63) → panel (48,52)
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(0, GT6RecipeMaps.BATH), 12, 5);
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(4, GT6RecipeMaps.BATH), 30, 23);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(0, GT6RecipeMaps.BATH), 102, 5);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(1, GT6RecipeMaps.BATH), 120, 5);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(2, GT6RecipeMaps.BATH), 138, 5);
		assertPos(GT6RecipeMapViewerMeta.viewerFluidInputPos(0), 48, 52);
		assertPos(GT6RecipeMapViewerMeta.viewerFluidOutputPos(1), 120, 52);
		// MIXER (6 in / 6 in-fluids → the y7 lift): the negative-row case — GUI (17,7)/
		// (53,25) → panel (12,-4)/(48,14). y=-4 is the upstream-faithful shape (the NEI
		// FixedPositionedStack carried the same), JEI/EMI don't clip widgets — no clamping.
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(0, GT6RecipeMaps.MIXER), 12, -4);
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(1, GT6RecipeMaps.MIXER), 30, -4);
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(5, GT6RecipeMaps.MIXER), 48, 14);
		// STEAM_CRACKING (1 in / 3 out / 9 out-fluids → outputs lift to y7): the output
		// side's negative row — GUI (53,25)/(107,7)/(143,7) → panel (48,14)/(102,-4)/(138,-4)
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(0, GT6RecipeMaps.STEAM_CRACKING), 48, 14);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(0, GT6RecipeMaps.STEAM_CRACKING), 102, -4);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(2, GT6RecipeMaps.STEAM_CRACKING), 138, -4);
		// FUSION (2 in / 6+6 out, fluids >3 both sides): the two-row 7/25 output band
		// folded — GUI (35,25)/(107,7)/(143,25) → panel (30,14)/(102,-4)/(138,14)
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(0, GT6RecipeMaps.FUSION), 30, 14);
		assertPos(GT6RecipeMapViewerMeta.viewerInputPos(1, GT6RecipeMaps.FUSION), 48, 14);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(0, GT6RecipeMaps.FUSION), 102, -4);
		assertPos(GT6RecipeMapViewerMeta.viewerOutputPos(5, GT6RecipeMaps.FUSION), 138, 14);
		// the null contract passes through the fold untouched
		assertNull(GT6RecipeMapViewerMeta.viewerOutputPos(12, GT6RecipeMaps.SHREDDER), "past the 12th drawn slot: null in, null out");
	}

	@Test
	void backdropCropsAndTextBandPinnedToTheUpstreamDraw() {
		// the two crop quadruples, the NEI_RecipeMap.drawBackground numbers folded to the
		// panel system: plate (-5,-16,0,0,176,166) → (u,v,w,h)=(5,16,166,140) :632; band
		// (-5,-8,0,3,176,79) → (5,11,166,71) :634
		assertArrayEquals(new int[] {5, 16, 166, 140}, GT6RecipeMapViewerMeta.PLATE_CROP);
		assertArrayEquals(new int[] {5, 11, 166, 71}, GT6RecipeMapViewerMeta.BAND_CROP);
		assertEquals("gt6:textures/gui/machines/nei.png", GT6RecipeMapViewerMeta.PLATE_TEXTURE.toString());
		// the retired text band: the drawExtras lines sit at the FIXED panel y73 for every
		// map (the +10 fluid deviation died with the backdrop landing — the fluid row ends
		// at panel y70, 3px clear)
		GT6RecipeMaps.init();
		for (RecipeMap tMap : List.of(GT6RecipeMaps.BATH, GT6RecipeMaps.MIXER, GT6RecipeMaps.FUSION,
				GT6RecipeMaps.LATHE, GT6RecipeMaps.STEAM_CRACKING))
			assertEquals(73, GT6RecipeMapViewerMeta.TEXT_BASE_Y, tMap.mNameInternal + " rides the fixed panel band");
		// the per-map machine texture is the live mGUIPath (the mapping table needs no
		// second copy): anvilbend folds AnvilBendingBig, the five fuel maps share default
		assertEquals("gt6:textures/gui/machines/anvilbend.png",
				GT6RecipeMapViewerMeta.guiTexture(GT6RecipeMaps.ANVIL_BEND).toString());
		assertEquals("gt6:textures/gui/machines/default.png",
				GT6RecipeMapViewerMeta.guiTexture(GT6RecipeMaps.ENGINE_FUELS).toString());
		assertEquals("gt6:textures/gui/machines/steamcracking.png",
				GT6RecipeMapViewerMeta.guiTexture(GT6RecipeMaps.STEAM_CRACKING).toString());
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

	/** The translatable contents of one cost line (the r6-29-34a key+args face). */
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
		// the post unit folded into the second arg slot)
		var tFusionLines = GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.FUSION, row(8, 200, 131072));
		var tSpecial = contents(tFusionLines.get(5));
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
		// the r6-29-34a title face: translatable, keyed by the shared formula (the oregen
		// precedent — offline getString() rides Language.loadDefault back to the bare key,
		// so the pin is the TranslatableContents key, not a literal)
		var tTitle = tCategory.getTitle().getContents();
		assertTrue(tTitle instanceof net.minecraft.network.chat.contents.TranslatableContents, "getTitle is translatable, not literal");
		assertEquals("gt6.jei.recipe_map.cokeoven", ((net.minecraft.network.chat.contents.TranslatableContents) tTitle).getKey());
		assertEquals(GT6RecipeMapViewerMeta.CATEGORY_WIDTH, tCategory.getWidth());
		assertEquals(GT6RecipeMapViewerMeta.CATEGORY_HEIGHT, tCategory.getHeight());
	}

	// -------------------------------------------------------------------
	// Task r10-debt-viewer-polish: the nojade decoupling + the gear-spot machine icon
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
}
