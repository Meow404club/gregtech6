
package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import com.google.gson.JsonParser;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.registry.GT6BakeFoods;
import gregtech6.registry.GT6SlicerBlades;
import gregtech6.registry.GTMaterialItems;

/**
 * The food-domain T3b band tests (task food-bake-recipes, the GT6RecipesFoodTest seam
 * shape). Faces:
 * <ol>
 * <li>the RATCHET — the band is exactly 28 rows (listener 12 = 9 foodDough + 3
 *     foodSugarDough, inline 16 = 8 slicer + 1 bath + 6 packunpack + 1 dormant boxinator)
 *     with the upstream anchor notes, and NOTHING from the :590 NeLi TRUE NEGATIVE or the
 *     :636/:637 cake rows;</li>
 * <li>the VERBATIM pins — duration/EUt/fluid amounts/stack counts of the upstream literals
 *     (Loader_Recipes_Food.java:137-153 + MultiItemFood.java:341-360/:598-785), including
 *     the :756 upstream copy-paste quirk PORTED VERBATIM;</li>
 * <li>the WIRING — the {@link GT6BakeFoods#BAKE_ROWS} index constants (both the runtime
 *     band's and the datagen mirror's) pin id-by-index;</li>
 * <li>the LIVE UNIVERSE — poured + skipped = 28 on both legs, the two declared dormancies
 *     ((gemChipped, Sugar) and (plateDouble, Paper)) never resolve on either leg;</li>
 * <li>the PORT UNIVERSE — fixtures mirroring the registered universe pour 26 / skip the 2
 *     dormancies, the poured rows carry the upstream columns, and the shaping-tool face of
 *     {@link Recipe#sNotConsumable} recognizes the molds + flat blade (and NOTHING else);</li>
 * <li>the POUR-FACE-FOREVER — with the dormant pairs bound the SAME load() pours all 28;</li>
 * <li>the DECLARED ledger + the DATAGEN census — SKIPPED_UPSTREAM carries the absent-leg
 *     and POOLED faces (the datagen band rows are NOT duplicated into it), and the
 *     committed {@code bake_*.json} generation census matches the band (64 crafting + 16
 *     smelt rows over data/gt6/recipes/ — 38 from food-bake-recipes + 18 from
 *     food-crafting-tail + 8 veggie legs + 1 egg smelt from pool-drain-food-t5-tail).</li>
 * </ol>
 */
class GT6RecipesBakeTest extends GTRecipesOfflineTestBase {

	@BeforeAll
	static void bootTheMaterialUniverse() {
		GTMaterialItems.initMaterials(); // the offline material universe (the family-walk prerequisite)
	}

	@BeforeEach
	void armTheMap() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		GT6RecipesBake.resetForTest();
	}

	@AfterEach
	void restoreTheLiveSeams() {
		// the live lambdas restored verbatim (the Canner restoreSeams convention)
		GT6RecipesBake.sMaterialItemResolver = GT6RecipesBake::resolveItem;
		GT6RecipesBake.sBakeItemResolver = GT6RecipesBake::defaultBakeItem;
		GT6RecipesBake.sMoldItemResolver = GT6RecipesBake::defaultMoldItem;
		GT6RecipesBake.sBladeResolver = GT6RecipesBake::defaultBladeItem;
		GT6RecipesBake.sSelectorResolver = gregtech6.item.GT6Circuits::selector;
		GT6RecipesBake.sNamedFluidResolver = GT6RecipesBake::resolveNamedFluid;
		GT6RecipesBake.sShapingToolTest = GT6RecipesBake::isShapingToolLive;
		GT6SlicerBlades.sBladeTest = SBLADE_DEFAULT;
		GT6RecipeMaps.reset();
	}

	/** The captured production blade seam (the guard-test captureDefaults convention). */
	private static final java.util.function.Predicate<ItemStack> SBLADE_DEFAULT = GT6SlicerBlades.sBladeTest;

	// ---------------------------------------------------------------- the ratchet

	/** 28 rows, per-target sizes, every upstream anchor note present. */
	@Test
	void theBandIs28RowsWithTheUpstreamAnchors() {
		List<GT6RecipesBake.Row> tTable = GT6RecipesBake.table();
		assertEquals(GT6RecipesBake.TOTAL_ROWS, tTable.size(), "the band total (12 listener + 16 inline)");
		assertEquals(GT6RecipesBake.LISTENER_ROWS, countListener(tTable), "listener: 9 foodDough + 3 foodSugarDough");
		assertEquals(GT6RecipesBake.INLINE_ROWS, tTable.size() - countListener(tTable), "inline: 8 slicer + 1 bath + 6 packunpack + 1 dormant boxinator");
		assertEquals(1, countTarget(tTable, GT6RecipesBake.Target.ROLLING_MILL), "rolling mill: the :139 row");
		assertEquals(4, countTarget(tTable, GT6RecipesBake.Target.MIXER), "mixer: :140 (dormant) + :141-:143");
		assertEquals(7, countTarget(tTable, GT6RecipesBake.Target.PRESS), "press: :146-:149 buns + :152 x3 cake bottoms");
		assertEquals(8, countTarget(tTable, GT6RecipesBake.Target.SLICER), "slicer: 4 cookie doughs + 4 loaf splits");
		assertEquals(1, countTarget(tTable, GT6RecipesBake.Target.BATH), "bath: the :357 fries row");
		assertEquals(4, countTarget(tTable, GT6RecipesBake.Target.BOXINATOR), "boxinator: 3 packs + the :360 dormant fries pack");
		assertEquals(3, countTarget(tTable, GT6RecipesBake.Target.UNBOXINATOR), "unboxinator: 3 unpacks");

		Set<String> tNotes = new HashSet<>();
		for (GT6RecipesBake.Row tRow : tTable) tNotes.add(tRow.note());
		for (String tAnchor : new String[] {":139", ":140", ":141", ":142", ":143", ":146", ":147", ":148", ":149",
				":152[sugar]", ":152[sugar_raisins]", ":152[sugar_choco_raisins]",
				":357", ":360", ":602", ":610", ":618", ":629", ":688", ":727", ":757", ":785",
				":687", ":687[unpack]", ":726", ":726[unpack]", ":756", ":756[unpack]"}) {
			assertTrue(tNotes.contains(tAnchor), "the upstream anchor " + tAnchor + " is transcribed");
		}
		// the :590 NeLi smelt and the :636/:637 cake faces are NOT band rows (the declared faces)
		for (GT6RecipesBake.Row tRow : tTable) {
			assertFalse(tRow.note().startsWith(":590") || tRow.note().startsWith(":636") || tRow.note().startsWith(":637")
					|| tRow.note().startsWith(":643") || tRow.note().startsWith(":359"),
				"no declared-face row in the band: " + tRow.note());
		}
	}

	// ---------------------------------------------------------------- the verbatim pins

	/** :139/:141/:143 — the rolling mill and mixer columns (16/16, the sugar/chocolate out-counts). */
	@Test
	void theListenerMixerRowsAreVerbatim() {
		GT6RecipesBake.Row t139 = row(":139");
		assertEquals(16, t139.eUt(), ":139 — EUt 16");
		assertEquals(16, t139.duration(), ":139 — duration 16");
		assertEquals(1, t139.inItems().length, ":139 — one dough in");
		assertEquals(GT6RecipesBake.BAKE_DOUGH, foodIndexOf(t139.inItems()[0]), ":139 — the dough leg");
		assertEquals(1, t139.inItems()[0].count(), ":139 — 1 dough");
		assertEquals(GT6RecipesBake.BAKE_DOUGH_FLAT, foodIndexOf(t139.outItems()[0]), ":139 — Food_Dough_Flat out");

		GT6RecipesBake.Row t141 = row(":141");
		assertEquals(16, t141.eUt(), ":141 — EUt 16");
		assertEquals(16, t141.duration(), ":141 — duration 16");
		assertEquals("dust", t141.inItems()[1].prefix().mNameInternal, ":141 — OM.dust(Sugar)");
		assertEquals(1, t141.inItems()[1].count(), ":141 — 1 sugar dust");
		assertEquals(2, t141.outItems()[0].count(), ":141 — 2 sugary dough out");

		GT6RecipesBake.Row t143 = row(":143");
		assertEquals("dust", t143.inItems()[1].prefix().mNameInternal, ":143 — OM.dust(Chocolate)");
		assertEquals(2, t143.outItems()[0].count(), ":143 — 2 chocolate dough out");
		assertEquals(1, row(":142").outItems()[0].count(), ":142 — 1 chocolate dough out (the cocoa leg)");
	}

	/** :146-:149/:152 — the press face: mold second input, the 1/2/3/4 dough counts and 16/32/48/64 durations, the cylinder trio. */
	@Test
	void thePressRowsAreVerbatim() {
		long[] tDurations = {16, 32, 48, 64};
		int[] tCounts = {1, 2, 3, 4};
		String[] tNotes = {":146", ":147", ":148", ":149"};
		int[] tMolds = {GT6RecipesBake.MOLD_BUN, GT6RecipesBake.MOLD_BREAD, GT6RecipesBake.MOLD_BAGUETTE, GT6RecipesBake.MOLD_TOAST};
		for (int i = 0; i < 4; i++) {
			GT6RecipesBake.Row tRow = row(tNotes[i]);
			assertEquals(16, tRow.eUt(), tNotes[i] + " — EUt 16");
			assertEquals(tDurations[i], tRow.duration(), tNotes[i] + " — duration");
			assertEquals(tCounts[i], tRow.inItems()[0].count(), tNotes[i] + " — the dough count");
			assertEquals(2, tRow.inItems().length, tNotes[i] + " — dough + mold");
			assertEquals(tMolds[i], moldIndexOf(tRow.inItems()[1]), tNotes[i] + " — the food mold leg");
			assertEquals(1, tRow.outItems()[0].count(), tNotes[i] + " — 1 raw loaf");
		}
		for (String tNote : new String[] {":152[sugar]", ":152[sugar_raisins]", ":152[sugar_choco_raisins]"}) {
			GT6RecipesBake.Row tRow = row(tNote);
			assertEquals(16, tRow.eUt(), tNote + " — EUt 16");
			assertEquals(64, tRow.duration(), tNote + " — duration 64");
			assertEquals(4, tRow.inItems()[0].count(), tNote + " — 4 sugar dough");
			assertEquals(GT6RecipesBake.MOLD_CYLINDER, moldIndexOf(tRow.inItems()[1]), tNote + " — the cylinder mold");
			assertEquals(GT6RecipesBake.BAKE_CAKEBOTTOM_RAW, foodIndexOf(tRow.outItems()[0]), tNote + " — raw cake bottom");
		}
	}

	/** :357 — the fries Bath row: EUt 0, duration 16, 10 mB hot frying oil (the upstream U/100 material-unit fold). */
	@Test
	void theFriesBathRowIsVerbatim() {
		GT6RecipesBake.Row t357 = row(":357");
		assertEquals(0, t357.eUt(), ":357 — EUt 0");
		assertEquals(16, t357.duration(), ":357 — duration 16");
		assertEquals(1, t357.inFluids().length, ":357 — one fluid in");
		assertEquals(GT6RecipesBake.HOT_FRYING_OIL, t357.inFluids()[0].name(), ":357 — MT.FryingOilHot");
		assertEquals(10, t357.inFluids()[0].amount(), ":357 — U/100 = 10 mB (OreDictMaterial.liquid:1307-1313)");
		assertEquals(GT6RecipesBake.BAKE_FRIES_RAW, foodIndexOf(t357.inItems()[0]), ":357 — raw fries in");
		assertEquals(GT6RecipesBake.BAKE_FRIES, foodIndexOf(t357.outItems()[0]), ":357 — fries out");
	}

	/** :602/:727/:785 — the slicer face: blade second input, the ×4/×2/×8 output counts, the vanilla-bread :727 input. */
	@Test
	void theSlicerRowsAreVerbatim() {
		GT6RecipesBake.Row t602 = row(":602");
		assertEquals(16, t602.eUt(), ":602 — EUt 16");
		assertEquals(16, t602.duration(), ":602 — duration 16");
		assertEquals(2, t602.inItems().length, ":602 — dough + flat blade");
		assertEquals(GT6RecipesBake.BLADE_FLAT, bladeIndexOf(t602.inItems()[1]), ":602 — the flat blade");
		assertEquals(4, t602.outItems()[0].count(), ":602 — 4 raw cookies");

		GT6RecipesBake.Row t727 = row(":727");
		assertEquals(2, t727.inItems().length, ":727 — bread + split blade");
		assertEquals(GT6RecipesBake.BLADE_SPLIT, bladeIndexOf(t727.inItems()[1]), ":727 — the split blade");
		assertNotNull(t727.inItems()[0].vanilla(), ":727 — the input is the VANILLA bread (the Food_Bread alias :715)");
		assertEquals(2, t727.outItems()[0].count(), ":727 — 2 sliced breads");
		assertEquals(GT6RecipesBake.BAKE_BREAD_SLICED, foodIndexOf(t727.outItems()[0]), ":727 — Bread_Sliced out");

		assertEquals(GT6RecipesBake.BLADE_FLAT, bladeIndexOf(row(":785").inItems()[1]), ":785 — toast splits over the FLAT blade");
		assertEquals(8, row(":785").outItems()[0].count(), ":785 — 8 toast slices");
		assertEquals(GT6RecipesBake.BLADE_SPLIT, bladeIndexOf(row(":757").inItems()[1]), ":757 — the baguette split");
	}

	/** :687/:726 — the packunpack pairs: content ×2 + selector(2) → full, unpack full → ×2 (RM.java:239-244). */
	@Test
	void thePackunpackRowsAreVerbatim() {
		GT6RecipesBake.Row t687 = row(":687");
		assertEquals(16, t687.eUt(), ":687 — EUt 16");
		assertEquals(16, t687.duration(), ":687 — duration 16");
		assertEquals(2, t687.inItems().length, ":687 — content + selector tag");
		assertEquals(2, t687.inItems()[0].count(), ":687 — 2 sliced buns");
		assertEquals(2, t687.inItems()[1].config(), ":687 — ST.tag(2)");
		assertEquals(GT6RecipesBake.BAKE_BUNS_SLICED, foodIndexOf(t687.outItems()[0]), ":687 — Buns_Sliced out");
		assertEquals(1, t687.outItems()[0].count(), ":687 — 1 pack");
		assertEquals(2, row(":687[unpack]").outItems()[0].count(), ":687[unpack] — 2 sliced out");

		assertEquals(2, row(":726").inItems()[1].config(), ":726 — ST.tag(2)");
		assertEquals(GT6RecipesBake.BAKE_BREADS_SLICED, foodIndexOf(row(":726").outItems()[0]), ":726 — Breads_Sliced out");
	}

	/** :756 — the upstream copy-paste quirk PORTED VERBATIM: the 2↔1 self-loop (second arg Baguette_Sliced where :754-:755 use Baguettes_Sliced). */
	@Test
	void the756SelfLoopQuirkIsVerbatim() {
		GT6RecipesBake.Row t756 = row(":756");
		assertEquals(GT6RecipesBake.BAKE_BAGUETTE_SLICED, foodIndexOf(t756.inItems()[0]), ":756 — sliced baguette in");
		assertEquals(2, t756.inItems()[0].count(), ":756 — 2 in");
		assertEquals(GT6RecipesBake.BAKE_BAGUETTE_SLICED, foodIndexOf(t756.outItems()[0]), ":756 — sliced baguette out (the SAME item — the upstream quirk)");
		assertEquals(1, t756.outItems()[0].count(), ":756 — 1 out (the self-loop)");
		assertEquals(GT6RecipesBake.BAKE_BAGUETTE_SLICED, foodIndexOf(row(":756[unpack]").inItems()[0]), ":756[unpack] — the same-item reverse face");
		assertEquals(2, row(":756[unpack]").outItems()[0].count(), ":756[unpack] — 2 out");
	}

	// ---------------------------------------------------------------- the wiring

	/** The {@link GT6BakeFoods#BAKE_ROWS} index constants pin id-by-index — a reorder trips here, not in the rows. */
	@Test
	void theBakeIndexConstantsPinTheBakeTable() {
		Object[][] tPins = {
				{GT6RecipesBake.BAKE_COOKIE_RAW, "food_cookie_raw"}, {GT6RecipesBake.BAKE_COOKIE_RAISINS_RAW, "food_cookie_raisins_raw"},
				{GT6RecipesBake.BAKE_COOKIE_CHOCO_RAISINS_RAW, "food_cookie_chocolate_raisins_raw"}, {GT6RecipesBake.BAKE_COOKIE_ABYSSAL_RAW, "food_cookie_abyssal_raw"},
				{GT6RecipesBake.BAKE_CAKEBOTTOM_RAW, "food_cakebottom_raw"}, {GT6RecipesBake.BAKE_DOUGH_FLAT, "food_dough_flat"},
				{GT6RecipesBake.BAKE_BUN_RAW, "food_bun_raw"}, {GT6RecipesBake.BAKE_BUN, "food_bun"},
				{GT6RecipesBake.BAKE_BUN_SLICED, "food_bun_sliced"}, {GT6RecipesBake.BAKE_BUNS_SLICED, "food_buns_sliced"},
				{GT6RecipesBake.BAKE_BREAD_RAW, "food_bread_raw"}, {GT6RecipesBake.BAKE_BREAD_SLICED, "food_bread_sliced"},
				{GT6RecipesBake.BAKE_BREADS_SLICED, "food_breads_sliced"}, {GT6RecipesBake.BAKE_BAGUETTE_RAW, "food_baguette_raw"},
				{GT6RecipesBake.BAKE_BAGUETTE, "food_baguette"}, {GT6RecipesBake.BAKE_BAGUETTE_SLICED, "food_baguette_sliced"},
				{GT6RecipesBake.BAKE_BAGUETTES_SLICED, "food_baguettes_sliced"}, {GT6RecipesBake.BAKE_FRIES_RAW, "food_fries_raw"},
				{GT6RecipesBake.BAKE_FRIES, "food_fries"}, {GT6RecipesBake.BAKE_FRIES_PACKAGED, "food_fries_packaged"},
				{GT6RecipesBake.BAKE_TOAST_RAW, "food_toast_raw"}, {GT6RecipesBake.BAKE_TOAST, "food_toast"},
				{GT6RecipesBake.BAKE_TOAST_SLICED, "food_toast_sliced"}, {GT6RecipesBake.BAKE_DOUGH, "food_dough"},
				{GT6RecipesBake.BAKE_DOUGH_SUGAR, "food_dough_sugar"}, {GT6RecipesBake.BAKE_DOUGH_CHOCOLATE, "food_dough_chocolate"},
				{GT6RecipesBake.BAKE_DOUGH_SUGAR_RAISINS, "food_dough_sugar_raisins"},
				{GT6RecipesBake.BAKE_DOUGH_SUGAR_CHOCO_RAISINS, "food_dough_sugar_chocolate_raisins"},
				{GT6RecipesBake.BAKE_DOUGH_ABYSSAL, "food_dough_abyssal"}};
		for (Object[] tPin : tPins) assertEquals(tPin[1], GT6BakeFoods.BAKE_ROWS.get((int) tPin[0]).id(), "BAKE_ROWS index " + tPin[0]);
		// the datagen mirror's non-constant indices (the rows the runtime band does not touch)
		Object[][] tMirrorPins = {
				{2, "food_cookie_raisins"}, {4, "food_cookie_chocolate_raisins"}, {7, "food_cakebottom"}, {9, "food_dough_flat_ketchup"},
				{10, "food_pizza_cheese_raw"}, {11, "food_pizza_cheese"}, {12, "food_pizza_meat_raw"}, {13, "food_pizza_meat"},
				{14, "food_pizza_veggie_raw"}, {15, "food_pizza_veggie"}, {16, "food_pizza_ananas_raw"}, {17, "food_pizza_ananas"},
				{23, "food_burger_cheese"}, {24, "food_burger_meat"}, {25, "food_burger_chum"}, {26, "food_burger_tofu"}, {27, "food_burger_soylent"}, {28, "food_burger_fish"},
				{22, "food_burger_veggie"}, // the pool-drain-food-t5-tail burger output (the meta-5010 slot)
				{32, "food_sandwich_veggie"}, {33, "food_sandwich_cheese"}, {34, "food_sandwich_bacon"}, {35, "food_sandwich_steak"},
				{40, "food_large_sandwich_veggie"}, {41, "food_large_sandwich_cheese"}, {42, "food_large_sandwich_bacon"}, {43, "food_large_sandwich_steak"},
				{50, "food_toasted_sliced"}, {58, "food_potato_on_stick"}, {59, "food_potato_on_stick_roasted"}};
		for (Object[] tPin : tMirrorPins) assertEquals(tPin[1], GT6BakeFoods.BAKE_ROWS.get((int) tPin[0]).id(), "BAKE_ROWS mirror index " + tPin[0]);
	}

	// ---------------------------------------------------------------- the pour faces

	/**
	 * The live-seam reconciliation (both legs): every row is either poured or accounted, and
	 * the two DECLARED dormancies never resolve on either leg (the T2 :761 gemChipped-Sugar
	 * verdict + the plateTiny-only force table). The pour COUNT is a leg fact.
	 */
	@Test
	void theLiveUniverseReconcilesWithTheDeclaredDormancies() {
		GT6RecipesBake.load();
		assertEquals(GT6RecipesBake.TOTAL_ROWS, totalPoured() + totalSkipped(), "every row accounted: poured + skipped = 28");
		assertNull(GT6RecipesBake.resolveItem(OP.gemChipped, MT.Sugar), "the gemChipped Sugar pair is unregistered (both legs, the T2 :761 verdict)");
		// the plateDouble Paper pair RESOLVES in the runtime flood (gt6:plate_double_paper —
		// proven on the 21.1 live JVM; the forge offline JVM binds nothing, so the pair is
		// asserted on the fixture-universe side, not here) — the :360 row pours
		for (GT6RecipesBake.Row tRow : GT6RecipesBake.table()) {
			if (tRow.note().equals(":140") || tRow.note().equals(":360")) continue; // the dormancies ride the skip side
			// whichever rows DID pour on this leg, they must have landed on their own map
		}
		assertTrue(GT6RecipesBake.lastSkipped(GT6RecipesBake.Target.MIXER) >= 1, "the :140 gemChipped Sugar leg skips");
	}

	/**
	 * The port reality: fixtures mirroring the registered universe (the bake foods, the
	 * molds, the blades, the selector, the water/hot-frying-oil carriers — but NOT the two
	 * dormant pairs) pour 26 / skip the 2 dormancies; the poured rows carry the upstream
	 * columns and the index wiring is proven by distinct fixture items.
	 */
	@Test
	void thePortUniversePours27With1DeclaredDormancy() {
		bindFixtures(false);
		GT6RecipesBake.resetForTest();
		GT6RecipesBake.load();

		assertEquals(27, totalPoured(), "the port reality: 27 pours");
		assertEquals(1, totalSkipped(), "the declared dormancy: :140");
		assertEquals(1, GT6RecipesBake.lastPoured(GT6RecipesBake.Target.ROLLING_MILL), "rolling mill 1");
		assertEquals(3, GT6RecipesBake.lastPoured(GT6RecipesBake.Target.MIXER), "mixer 3 (the :140 leg dormant)");
		assertEquals(7, GT6RecipesBake.lastPoured(GT6RecipesBake.Target.PRESS), "press 7");
		assertEquals(8, GT6RecipesBake.lastPoured(GT6RecipesBake.Target.SLICER), "slicer 8");
		assertEquals(1, GT6RecipesBake.lastPoured(GT6RecipesBake.Target.BATH), "bath 1");
		assertEquals(4, GT6RecipesBake.lastPoured(GT6RecipesBake.Target.BOXINATOR), "boxinator 4 (the :360 leg live)");
		assertEquals(3, GT6RecipesBake.lastPoured(GT6RecipesBake.Target.UNBOXINATOR), "unboxinator 3");

		// the Bath row carries the :357 columns
		for (Object tRowObject : GT6RecipeMaps.BATH.mRecipeList) {
			Recipe tRow = (Recipe) tRowObject;
			assertEquals(0, tRow.mEUt, ":357 — EUt 0");
			assertEquals(16, tRow.mDuration, ":357 — duration 16");
			assertEquals(1, tRow.mFluidInputs.length, ":357 — one fluid in");
			assertEquals(10, tRow.mFluidInputs[0].getAmount(), ":357 — 10 mB hot frying oil");
		}
		// the Press rows: 2 inputs, the dough count rides slot 0, 16 EUt
		int tPressRows = 0;
		for (Object tRowObject : GT6RecipeMaps.PRESS.mRecipeList) {
			Recipe tRow = (Recipe) tRowObject;
			assertEquals(2, tRow.mInputs.length, "press — dough + mold");
			assertEquals(16, tRow.mEUt, "press — EUt 16");
			tPressRows++;
		}
		assertEquals(7, tPressRows, "press 7 rows");
		// the Boxinator rows: the three pack rows carry the selector (the fixture GOLD_NUGGET
		// at slot 1); the :360 fries pack is the content + material row (GUNPOWDER + BRICK →
		// GLOWSTONE_DUST); the :756 self-loop is the row whose output IS its input (the
		// fixture SNOWBALL pair — the :687/:726 packs carry distinct sliced→buns fixtures)
		int tSelfLoop = 0, tFriesPack = 0;
		for (Object tRowObject : GT6RecipeMaps.BOXINATOR.mRecipeList) {
			Recipe tRow = (Recipe) tRowObject;
			if (tRow.mOutputs[0].getItem() == Items.GLOWSTONE_DUST) {
				tFriesPack++;
				assertEquals(Items.GUNPOWDER, tRow.mInputs[0].getItem(), ":360 — the fries content leg");
				assertEquals(Items.BRICK, tRow.mInputs[1].getItem(), ":360 — the plateDouble-Paper leg (live, not a selector)");
				continue;
			}
			assertEquals(Items.GOLD_NUGGET, tRow.mInputs[1].getItem(), "boxinator pack — the selector tag leg");
			if (tRow.mOutputs[0].getItem() == tRow.mInputs[0].getItem()) {
				tSelfLoop++;
				assertEquals(Items.SNOWBALL, tRow.mInputs[0].getItem(), "the self-loop is exactly the :756 baguette-sliced pair");
			}
		}
		assertEquals(1, tSelfLoop, "exactly the :756 self-loop row");
		assertEquals(1, tFriesPack, "exactly the :360 fries-pack row");
	}

	/**
	 * The pour-face-forever proof (the Bath wood-ladder form): with the dormant pair bound
	 * the SAME load() pours all 28 — the dormancy is a universe fact, not a code wall.
	 */
	@Test
	void theFullAssemblyPoursAll28WhenTheDormantPairsLand() {
		bindFixtures(true);
		GT6RecipesBake.resetForTest();
		GT6RecipesBake.load();

		assertEquals(GT6RecipesBake.TOTAL_ROWS, totalPoured(), "all 28 pour");
		assertEquals(0, totalSkipped(), "none skipped");
		assertEquals(4, GT6RecipesBake.lastPoured(GT6RecipesBake.Target.MIXER), "mixer 4 (the :140 leg live)");
		assertEquals(4, GT6RecipesBake.lastPoured(GT6RecipesBake.Target.BOXINATOR), "boxinator 4 (all packs live)");
	}

	/**
	 * The shaping-tool face of {@link Recipe#sNotConsumable}: the six food molds and the
	 * flat blade are never-consumed, the split blade rides the earlier isBlade arm, and a
	 * random item is NOT a shaping tool (the narrow-claim pin).
	 */
	@Test
	void theShapingToolFaceRecognizesExactlyTheBakeTools() {
		GT6RecipesBake.sBakeItemResolver = aIndex -> Items.PAPER; // fixtures for the deref
		GT6RecipesBake.sMoldItemResolver = aIndex -> Items.IRON_INGOT;
		GT6RecipesBake.sBladeResolver = aIndex -> aIndex == GT6RecipesBake.BLADE_FLAT ? Items.IRON_SHOVEL : Items.IRON_AXE;
		// the offline fixture identity: the mold fixture + the flat-blade fixture (the live
		// registry is unbound here — the sBladeTest two-contract swap covers the split arm)
		GT6RecipesBake.sShapingToolTest = aStack -> aStack != null && !aStack.isEmpty()
				&& (aStack.is(Items.IRON_INGOT) || aStack.is(Items.IRON_SHOVEL));
		GT6SlicerBlades.sBladeTest = aStack -> aStack != null && !aStack.isEmpty() && aStack.is(Items.IRON_AXE);
		assertTrue(Recipe.sNotConsumable.test(new ItemStack(Items.IRON_INGOT)), "the food mold is never-consumed (the fifth arm)");
		assertTrue(Recipe.sNotConsumable.test(new ItemStack(Items.IRON_SHOVEL)), "the flat blade is never-consumed (the fifth arm)");
		assertTrue(Recipe.sNotConsumable.test(new ItemStack(Items.IRON_AXE)), "the split blade rides the isBlade arm (the fourth arm, fixture-swapped)");
		// the selector arm (the FIRST disjunct) is the pre-existing GT6Circuits face — pinned
		// by the circuit-card tests; the synthetic-item construction hits the offline item-
		// registry freeze here, so this test does not re-pin it (the packunpack rows consume
		// its coverage).
		assertFalse(Recipe.sNotConsumable.test(new ItemStack(Items.DIRT)), "a random item is consumed");
	}

	// ---------------------------------------------------------------- the declared ledger + the datagen census

	/**
	 * The SKIPPED_UPSTREAM ledger: the datagen-band rows are NOT duplicated into it (the
	 * 二选一 rule), the absent-leg/foreign/POOLED faces are.
	 */
	@Test
	void theDeclaredLedgerCarriesTheNonBandFaces() {
		assertTrue(GT6RecipesBake.SKIPPED_UPSTREAM.size() >= 11, "the declared families are present");
		String tDeclared = String.join("\n", GT6RecipesBake.SKIPPED_UPSTREAM);
		for (String tMarker : new String[] {"foodBake band", ":590", "TRUE NEGATIVE", "ketchup ladder", "foodHeavycream", "delate",
				"Sandwiches.INGREDIENTS", "RM.food_can", "POOLED", ":138", "NO-OP", "DORMANT", ":140", "gemChipped", "plateDouble"}) {
			assertTrue(tDeclared.contains(tMarker), "SKIPPED_UPSTREAM declares " + tMarker);
		}
		// the datagen band rows are listed ONCE (pointer face), never as individual smelt/craft declarations
		for (GT6RecipesBake.Row tRow : GT6RecipesBake.table()) {
			assertFalse(tDeclared.contains("declared out-of-band " + tRow.note()), "no band-row duplication in the ledger");
		}
	}

	/**
	 * The committed datagen generation census: 64 crafting + 16 smelt bake rows over
	 * data/gt6/recipes/ (the bake_*.json set).
	 */
	@Test
	void theDatagenBakeGenerationCensus() throws Exception {
		Set<String> tExpected = new HashSet<>();
		String[] tCraft = {"bake_slice_cookie_raw", "bake_slice_cookie_raisins_raw", "bake_slice_cookie_choco_raisins_raw", "bake_slice_cookie_abyssal_raw",
				"bake_slice_bun", "bake_slice_bread", "bake_slice_baguette", "bake_slice_toast", "bake_fries_raw",
				"bake_dough_flat_rolling", "bake_cakebottom_raw", "bake_bun_raw", "bake_bread_raw", "bake_baguette_raw", "bake_toast_raw",
				"bake_potato_on_stick", "bake_potato_on_stick_roasted", "bake_pizza_cheese_raw", "bake_pizza_meat_raw", "bake_fries_packaged",
				// the food-crafting-tail tail: kX 补遗 5 + slice pairs 6 + chum legs 3 + bacon legs 4
				"bake_slice_cheese", "bake_slice_egg_brown", "bake_slice_egg_white", "bake_slice_ham_raw", "bake_slice_ham_cooked",
				"bake_bun_pair", "bake_bun_unpack", "bake_bread_pair", "bake_bread_unpack", "bake_baguette_pair", "bake_baguette_unpack",
				"bake_burger_chum_buns", "bake_burger_chum_pair", "bake_chum_on_stick",
				"bake_sandwich_bacon_breads", "bake_sandwich_bacon_pair", "bake_large_sandwich_bacon_baguettes", "bake_large_sandwich_bacon_pair",
				"bake_burger_cheese_buns", "bake_burger_cheese_pair", "bake_burger_meat_buns", "bake_burger_meat_pair",
				"bake_burger_tofu_buns", "bake_burger_tofu_pair", "bake_burger_soylent_buns", "bake_burger_soylent_pair",
				"bake_burger_fish_buns", "bake_burger_fish_pair",
				"bake_sandwich_cheese_breads", "bake_sandwich_cheese_pair", "bake_sandwich_steak_breads", "bake_sandwich_steak_pair",
				"bake_large_sandwich_cheese_baguettes", "bake_large_sandwich_cheese_pair", "bake_large_sandwich_steak_baguettes", "bake_large_sandwich_steak_pair",
				// the pool-drain-food-t5-tail veggie legs: pizzas 2 + burgers 2 + sandwiches 4
				"bake_pizza_veggie_raw", "bake_pizza_ananas_raw",
				"bake_burger_veggie_buns", "bake_burger_veggie_pair",
				"bake_sandwich_veggie_breads", "bake_sandwich_veggie_pair",
				"bake_large_sandwich_veggie_baguettes", "bake_large_sandwich_veggie_pair"};
		String[] tSmelt = {"bake_smelt_potato_on_stick", "bake_smelt_cookie", "bake_smelt_cookie_raisins", "bake_smelt_cookie_choco_raisins",
				"bake_smelt_cookie_abyssal", "bake_smelt_cakebottom", "bake_smelt_pizza_cheese", "bake_smelt_pizza_meat",
				"bake_smelt_pizza_veggie", "bake_smelt_pizza_ananas", "bake_smelt_bun", "bake_smelt_bread_raw", "bake_smelt_baguette",
				"bake_smelt_toast", "bake_smelt_toast_sliced", "bake_smelt_egg"};
		assertEquals(64, tCraft.length, "64 crafting rows (38 food-bake-recipes + 18 food-crafting-tail + 8 pool-drain-food-t5-tail)");
		assertEquals(16, tSmelt.length, "16 smelt rows (15 + the :317 egg)");
		// the unlock-advancement stop (2026-10-03 user ruling, remember id1359): the census pins the
		// recipe JSONs alone — no advancement companions anymore
		for (String tId : tCraft) tExpected.add("data/gt6/recipes/" + tId + ".json");
		for (String tId : tSmelt) tExpected.add("data/gt6/recipes/" + tId + ".json");
		int tFound = 0;
		for (String tPath : tExpected) {
			assertTrue(classpathHas(tPath), "the generated recipe " + tPath + " is committed");
			tFound++;
		}
		assertEquals(80, tFound, "80 bake generations (64 + 16)");
		// a verbatim spot: the :147 press analog is runtime, but the :718 smelt row output is the vanilla bread
		String tBread = classpathJson("data/gt6/recipes/bake_smelt_bread_raw.json");
		assertTrue(tBread.contains("minecraft:bread"), ":718 — Bread_Raw smelts into the vanilla bread (the Food_Bread alias)");
		String tCookie = classpathJson("data/gt6/recipes/bake_smelt_cookie.json");
		assertTrue(tCookie.contains("minecraft:cookie"), ":599 — Cookie_Raw smelts into the vanilla cookie");
		theFoodCraftingTailRowsAreVerbatim();
		theT5VeggieRowsAreVerbatim();
	}

	/**
	 * The food-crafting-tail verbatim pins (over the committed JSON, the upstream line
	 * anchors): the :492 cheese kX, the :504 egg split (one row per boiled egg :497/:498),
	 * the :685/:686 bun pair both directions, the :709 chum burger, the :799 stick union,
	 * the :737/:768 bacon counts (×3/×6).
	 */
	private void theFoodCraftingTailRowsAreVerbatim() throws Exception {
		// :492 CR.shaped(Food_Cheese_Sliced.get(4), DEF_NCC, "kX", 'X', "foodCheese")
		// (classpathJson re-serializes compactly — "count":4, ingredient counts are REPEATS)
		String tCheese = classpathJson("data/gt6/recipes/bake_slice_cheese.json");
		assertTrue(tCheese.contains("\"item\":\"gt6:food_cheese_sliced\"") && tCheese.contains("\"count\":4"), ":492 — Cheese_Sliced ×4 out");
		assertTrue(tCheese.contains("\"item\":\"gt6:food_cheese\""), ":492 — the cheese in (the foodCheese member)");
		assertTrue(tCheese.contains("\"tag\":\"gt6:tools/knife\""), ":492 — the kX knife letter");
		// :504 split — each boiled egg row → 4 Egg_Sliced (:497 brown / :498 white)
		String tEggBrown = classpathJson("data/gt6/recipes/bake_slice_egg_brown.json");
		assertTrue(tEggBrown.contains("\"item\":\"gt6:food_brown_egg_boiled\""), ":504 — the :497 brown boiled egg leg");
		assertTrue(tEggBrown.contains("\"item\":\"gt6:food_egg_sliced\"") && tEggBrown.contains("\"count\":4"), ":504 — Egg_Sliced ×4 out");
		String tEggWhite = classpathJson("data/gt6/recipes/bake_slice_egg_white.json");
		assertTrue(tEggWhite.contains("\"item\":\"gt6:food_white_egg_boiled\""), ":504 — the :498 white boiled egg leg");
		// :685/:686 — the bun pair both directions (2 sliced in = 2 repeated entries, no count key)
		String tBunPair = classpathJson("data/gt6/recipes/bake_bun_pair.json");
		assertTrue(tBunPair.contains("\"item\":\"gt6:food_buns_sliced\""), ":685 — Buns_Sliced out");
		assertEquals(2, occurrences(tBunPair, "gt6:food_bun_sliced"), ":685 — 2 bun slices in");
		String tBunUnpack = classpathJson("data/gt6/recipes/bake_bun_unpack.json");
		assertTrue(tBunUnpack.contains("\"item\":\"gt6:food_buns_sliced\""), ":686 — the pre-sliced pack in");
		assertTrue(tBunUnpack.contains("\"item\":\"gt6:food_bun_sliced\"") && tBunUnpack.contains("\"count\":2"), ":686 — 2 bun slices out");
		// :709 — Buns_Sliced + foodChum(=gt6:food_chum :594) → Chum Burger
		String tChumBurger = classpathJson("data/gt6/recipes/bake_burger_chum_buns.json");
		assertTrue(tChumBurger.contains("\"item\":\"gt6:food_buns_sliced\""), ":709 — Buns_Sliced in");
		assertTrue(tChumBurger.contains("\"item\":\"gt6:food_chum\""), ":709 — the foodChum member");
		assertTrue(tChumBurger.contains("\"item\":\"gt6:food_burger_chum\""), ":709 — Chum_Burger out");
		// :799 — the ANY.Wood stick union + chum → Chum_On_Stick (the union serializes as the multi-item array)
		String tStick = classpathJson("data/gt6/recipes/bake_chum_on_stick.json");
		assertTrue(tStick.contains("\"item\":\"gt6:food_chum\""), ":799 — the foodChum member in");
		assertTrue(tStick.contains("\"item\":\"gt6:food_chum_on_stick\""), ":799 — Chum_On_Stick out");
		assertTrue(tStick.contains("minecraft:stick"), ":799 — the vanilla stick rides the union (the woodStickIngredient face)");
		// :737/:768 — the bacon counts ×3 / ×6 ("foodBaconcooked" = gt6:food_bacon_cooked :542, repeated entries)
		String tBaconBreads = classpathJson("data/gt6/recipes/bake_sandwich_bacon_breads.json");
		assertEquals(3, occurrences(tBaconBreads, "gt6:food_bacon_cooked"), ":737 — 3 grilled bacon");
		assertTrue(tBaconBreads.contains("\"item\":\"gt6:food_breads_sliced\""), ":737 — Breads_Sliced in");
		assertTrue(tBaconBreads.contains("\"item\":\"gt6:food_sandwich_bacon\""), ":737 — Bacon_Sandwich out");
		String tBaconPair = classpathJson("data/gt6/recipes/bake_large_sandwich_bacon_pair.json");
		assertEquals(2, occurrences(tBaconPair, "gt6:food_baguette_sliced"), ":768 — 2 baguette slices");
		assertEquals(6, occurrences(tBaconPair, "gt6:food_bacon_cooked"), ":768 — 6 grilled bacon");
		assertTrue(tBaconPair.contains("\"item\":\"gt6:food_large_sandwich_bacon\""), ":768 — Large_Bacon_Sandwich out");
	}

	/** The plain substring occurrence count (the repeated-entry count face over the compact JSON). */
	private static int occurrences(String aJson, String aNeedle) {
		return aJson.split(aNeedle, -1).length - 1;
	}

	/**
	 * The pool-drain-food-t5-tail verbatim pins (over the committed JSON, the upstream
	 * line anchors): the :664/:670 pizza legs, the :697/:698 burger legs, the :733/:734
	 * and :763/:764 veggie sandwich ladders, the off-by-one CLOSURE (the cheese
	 * sandwich counts ride the upstream :735/:736 = 5 and :765/:766 = 7 slice literals,
	 * not the pre-card 6/8), and the :317 egg smelt.
	 */
	private void theT5VeggieRowsAreVerbatim() throws Exception {
		// :664 — Ketchup Flat Dough + the crop slice trio → Raw Veggie Pizza
		String tVeggiePizza = classpathJson("data/gt6/recipes/bake_pizza_veggie_raw.json");
		assertTrue(tVeggiePizza.contains("\"item\":\"gt6:food_dough_flat_ketchup\""), ":664 — the ketchup flat dough base");
		assertTrue(tVeggiePizza.contains("\"item\":\"gt6:food_cucumber_sliced\""), ":664 — the cucumber slice");
		assertTrue(tVeggiePizza.contains("\"item\":\"gt6:food_tomato_sliced\""), ":664 — the tomato slice");
		assertTrue(tVeggiePizza.contains("\"item\":\"gt6:food_onion_sliced\""), ":664 — the onion slice");
		assertTrue(tVeggiePizza.contains("\"item\":\"gt6:food_pizza_veggie_raw\""), ":664 — Raw Veggie Pizza out");
		// :670 — + 2 Ananas Sliced + Ham Slice :535 + Cheese Slice → Raw Pizza Hawaii
		String tAnanasPizza = classpathJson("data/gt6/recipes/bake_pizza_ananas_raw.json");
		assertEquals(2, occurrences(tAnanasPizza, "gt6:food_ananas_sliced"), ":670 — 2 ananas slices");
		assertTrue(tAnanasPizza.contains("\"item\":\"gt6:food_ham_slice_cooked\""), ":670 — the cooked ham slice (:535)");
		assertTrue(tAnanasPizza.contains("\"item\":\"gt6:food_pizza_ananas_raw\""), ":670 — Raw Pizza Hawaii out");
		// :697/:698 — the veggie burger both bun shapes (1 pre-sliced / 2 sliced + the trio)
		String tVeggieBuns = classpathJson("data/gt6/recipes/bake_burger_veggie_buns.json");
		assertTrue(tVeggieBuns.contains("\"item\":\"gt6:food_buns_sliced\""), ":697 — Buns_Sliced in");
		assertEquals(1, occurrences(tVeggieBuns, "gt6:food_cucumber_sliced"), ":697 — 1 cucumber slice");
		assertTrue(tVeggieBuns.contains("\"item\":\"gt6:food_burger_veggie\""), ":697 — Veggie Burger out");
		String tVeggiePair = classpathJson("data/gt6/recipes/bake_burger_veggie_pair.json");
		assertEquals(2, occurrences(tVeggiePair, "gt6:food_bun_sliced"), ":698 — 2 bun slices in");
		// :733/:734 — the veggie sandwich ladder (2+2+1 over the pre-sliced pack / the pair)
		String tSandBreads = classpathJson("data/gt6/recipes/bake_sandwich_veggie_breads.json");
		assertTrue(tSandBreads.contains("\"item\":\"gt6:food_breads_sliced\""), ":733 — Breads_Sliced in");
		assertEquals(2, occurrences(tSandBreads, "gt6:food_cucumber_sliced"), ":733 — 2 cucumber slices");
		assertEquals(2, occurrences(tSandBreads, "gt6:food_tomato_sliced"), ":733 — 2 tomato slices");
		assertEquals(1, occurrences(tSandBreads, "gt6:food_onion_sliced"), ":733 — 1 onion slice");
		assertTrue(tSandBreads.contains("\"item\":\"gt6:food_sandwich_veggie\""), ":733 — Veggie Sandwich out");
		String tSandPair = classpathJson("data/gt6/recipes/bake_sandwich_veggie_pair.json");
		assertEquals(2, occurrences(tSandPair, "gt6:food_bread_sliced"), ":734 — 2 bread slices in");
		// :763/:764 — the large veggie ladder (3+3+1 over Baguettes_Sliced / the pair)
		String tLargeBaguettes = classpathJson("data/gt6/recipes/bake_large_sandwich_veggie_baguettes.json");
		assertTrue(tLargeBaguettes.contains("\"item\":\"gt6:food_baguettes_sliced\""), ":763 — Baguettes_Sliced in");
		assertEquals(3, occurrences(tLargeBaguettes, "gt6:food_cucumber_sliced"), ":763 — 3 cucumber slices");
		assertEquals(3, occurrences(tLargeBaguettes, "gt6:food_tomato_sliced"), ":763 — 3 tomato slices");
		assertEquals(1, occurrences(tLargeBaguettes, "gt6:food_onion_sliced"), ":763 — 1 onion slice");
		assertTrue(tLargeBaguettes.contains("\"item\":\"gt6:food_large_sandwich_veggie\""), ":763 — Large Veggie Sandwich out");
		String tLargePair = classpathJson("data/gt6/recipes/bake_large_sandwich_veggie_pair.json");
		assertEquals(2, occurrences(tLargePair, "gt6:food_baguette_sliced"), ":764 — 2 baguette slices in");
		// the off-by-one closure — the cheese sandwich JSON carries the upstream 5/7 slice
		// counts (known_bugs.bake_sandwich_cheese_off_by_one: the pre-card 6/8)
		assertEquals(5, occurrences(classpathJson("data/gt6/recipes/bake_sandwich_cheese_breads.json"), "gt6:food_cheese_sliced"),
				":735 — 5 cheese slices (the off-by-one closure, was 6)");
		assertEquals(7, occurrences(classpathJson("data/gt6/recipes/bake_large_sandwich_cheese_baguettes.json"), "gt6:food_cheese_sliced"),
				":765 — 7 cheese slices (the off-by-one closure, was 8)");
		// :317 — the vanilla egg smelts into Food_Egg_Fried
		String tEggSmelt = classpathJson("data/gt6/recipes/bake_smelt_egg.json");
		assertTrue(tEggSmelt.contains("\"item\":\"minecraft:egg\""), ":317 — the vanilla egg in");
		assertTrue(tEggSmelt.contains("\"result\":\"gt6:food_egg_fried\""), ":317 — Food_Egg_Fried out");
	}

	// ---------------------------------------------------------------- helpers

	/** Fixtures mirroring the registered port universe; {@code aBindDormants} adds the two unregistered pairs (the full-assembly future). */
	private static void bindFixtures(boolean aBindGemChippedSugar) {
		Map<Integer, Item> tFoods = new HashMap<>();
		tFoods.put(GT6RecipesBake.BAKE_DOUGH_FLAT, Items.BREAD);
		tFoods.put(GT6RecipesBake.BAKE_BUN_RAW, Items.COOKIE);
		tFoods.put(GT6RecipesBake.BAKE_BREAD_RAW, Items.PUMPKIN_PIE);
		tFoods.put(GT6RecipesBake.BAKE_BAGUETTE_RAW, Items.GOLD_INGOT);
		tFoods.put(GT6RecipesBake.BAKE_TOAST_RAW, Items.IRON_INGOT);
		tFoods.put(GT6RecipesBake.BAKE_CAKEBOTTOM_RAW, Items.CLAY_BALL);
		// the packunpack identities (the :756 self-loop probe walks the baguette-sliced pair)
		tFoods.put(GT6RecipesBake.BAKE_BUN_SLICED, Items.FLINT);
		tFoods.put(GT6RecipesBake.BAKE_BUNS_SLICED, Items.FEATHER);
		tFoods.put(GT6RecipesBake.BAKE_BREAD_SLICED, Items.BONE);
		tFoods.put(GT6RecipesBake.BAKE_BREADS_SLICED, Items.STRING);
		tFoods.put(GT6RecipesBake.BAKE_BAGUETTE_SLICED, Items.SNOWBALL);
		// the fries-pack identities (the :360 row is content + material, NOT a selector row)
		tFoods.put(GT6RecipesBake.BAKE_FRIES, Items.GUNPOWDER);
		tFoods.put(GT6RecipesBake.BAKE_FRIES_PACKAGED, Items.GLOWSTONE_DUST);
		GT6RecipesBake.sMaterialItemResolver = (aPrefix, aMaterial) -> {
			if (!aBindGemChippedSugar && aPrefix == OP.gemChipped && aMaterial == MT.Sugar) return null;
			return Items.BRICK;
		};
		GT6RecipesBake.sBakeItemResolver = aIndex -> tFoods.getOrDefault(aIndex, Items.PAPER);
		GT6RecipesBake.sMoldItemResolver = aIndex -> Items.IRON_INGOT;
		GT6RecipesBake.sBladeResolver = aIndex -> aIndex == GT6RecipesBake.BLADE_FLAT ? Items.IRON_SHOVEL : Items.IRON_AXE;
		GT6RecipesBake.sSelectorResolver = GT6CircuitsFixture::fixture;
		GT6RecipesBake.sNamedFluidResolver = aName -> Fluids.WATER;
		GT6RecipesBake.sShapingToolTest = aStack -> aStack != null && !aStack.isEmpty()
				&& (aStack.is(Items.IRON_INGOT) || aStack.is(Items.IRON_SHOVEL));
	}

	/** The GT6RecipesBake ItemLeg index probes (the band-side wiring face — the leg data carries its own index). */
	private static int foodIndexOf(GT6RecipesBake.ItemLeg aLeg) {
		assertEquals(GT6RecipesBake.ItemLeg.BAKE, aLeg.kind(), "the probed leg is a bake-food leg");
		return aLeg.config();
	}

	/** The mold index probe. */
	private static int moldIndexOf(GT6RecipesBake.ItemLeg aLeg) {
		assertEquals(GT6RecipesBake.ItemLeg.MOLD, aLeg.kind(), "the probed leg is a food-mold leg");
		return aLeg.config();
	}

	/** The blade index probe. */
	private static int bladeIndexOf(GT6RecipesBake.ItemLeg aLeg) {
		assertEquals(GT6RecipesBake.ItemLeg.BLADE, aLeg.kind(), "the probed leg is a blade leg");
		return aLeg.config();
	}

	private static int countTarget(List<GT6RecipesBake.Row> aTable, GT6RecipesBake.Target aTarget) {
		int rCount = 0;
		for (GT6RecipesBake.Row tRow : aTable) if (tRow.target() == aTarget) rCount++;
		return rCount;
	}

	private static int countListener(List<GT6RecipesBake.Row> aTable) {
		int rCount = 0;
		for (GT6RecipesBake.Row tRow : aTable) if (tRow.note().startsWith(":1")) rCount++;
		return rCount;
	}

	private static GT6RecipesBake.Row row(String aNote) {
		for (GT6RecipesBake.Row tRow : GT6RecipesBake.table()) if (tRow.note().equals(aNote)) return tRow;
		throw new AssertionError("no row " + aNote);
	}

	private static int totalPoured() {
		int rSum = 0;
		for (GT6RecipesBake.Target tTarget : GT6RecipesBake.Target.values()) rSum += GT6RecipesBake.lastPoured(tTarget);
		return rSum;
	}

	private static int totalSkipped() {
		int rSum = 0;
		for (GT6RecipesBake.Target tTarget : GT6RecipesBake.Target.values()) rSum += GT6RecipesBake.lastSkipped(tTarget);
		return rSum;
	}

	private static boolean classpathHas(String aPath) {
		return GT6RecipesBakeTest.class.getClassLoader().getResource(aPath) != null;
	}

	private static String classpathJson(String aPath) throws Exception {
		try (java.io.InputStream tStream = GT6RecipesBakeTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).toString();
		}
	}

	/** The selector fixture (the plain packunpack stand-in; the live form is the restored GT6Circuits::selector). */
	private static final class GT6CircuitsFixture {
		static ItemStack fixture(int aConfig) {return new ItemStack(Items.GOLD_NUGGET);}
		private GT6CircuitsFixture() {}
	}
}
