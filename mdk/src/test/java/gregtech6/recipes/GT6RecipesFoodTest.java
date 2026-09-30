
package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import gregtech6.registry.GT6Foods;
import gregtech6.registry.GTMaterialItems;

/**
 * The food-domain T1b band tests (task food-recipes-t1b, the GT6RecipesBathTest seam
 * shape). Five faces:
 * <ol>
 * <li>the RATCHET — the band is exactly 29 rows (coagulator 2 / cryo 8 / centrifuge 4 /
 *     mixer 7 / bath 8) with the upstream anchor notes, and NOTHING from the :673-:686
 *     coagulator TRUE NEGATIVE band (dropHoney/dropHoneydew/RoyalJelly/Glue/Blood — the
 *     B2a verdict: upstream registers nothing in a GT6-only env, no GT6 substitute);</li>
 * <li>the VERBATIM pins — duration/EUt/fluid amounts/stack counts of the upstream
 *     literals on every band (Loader_Recipes_Food.java:694-695/:660-670/:729-731/:771 +
 *     :303-:312, MultiItemFood.java:847-850);</li>
 * <li>the WIRING — the {@link GT6Foods#FOOD_ROWS} index constants pin id-by-index, so an
 *     items-core reorder trips here instead of silently rewiring the rows;</li>
 * <li>the LIVE UNIVERSE — the reconciliation is exact on both legs (poured + skipped =
 *     29): the forge offline JVM binds nothing (0/29), the 21.1 FML test JVM binds the
 *     registries (24/5); the two declared dormancies never pour on either leg;</li>
 * <li>the POUR-FACE-FOREVER — with the dormant legs bound the SAME load() pours all 29
 *     (the Bath wood-ladder form: the rows are live code, not dead stubs).</li>
 * </ol>
 */
class GT6RecipesFoodTest extends GTRecipesOfflineTestBase {

	@BeforeAll
	static void bootTheMaterialUniverse() {
		GTMaterialItems.initMaterials(); // the offline material universe (the family-walk prerequisite)
	}

	@BeforeEach
	void armTheMap() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		GT6RecipesFood.resetForTest();
	}

	@AfterEach
	void restoreTheLiveSeams() {
		// the live lambdas restored verbatim (the Canner restoreSeams convention)
		GT6RecipesFood.sMaterialItemResolver = GT6RecipesFood::resolveItem;
		GT6RecipesFood.sFoodItemResolver = GT6RecipesFood::defaultFoodItem;
		GT6RecipesFood.sWhiteEggResolver = () -> null;
		GT6RecipesFood.sNamedFluidResolver = GT6RecipesFood::resolveNamedFluid;
		GT6RecipeMaps.reset();
	}

	// ---------------------------------------------------------------- the ratchet

	/** 29 rows, per-band sizes, every upstream anchor note present. */
	@Test
	void theBandIs29RowsWithTheUpstreamAnchors() {
		List<GT6RecipesFood.Row> tTable = GT6RecipesFood.table();
		assertEquals(GT6RecipesFood.TOTAL_ROWS, tTable.size(), "the band total (2+8+4+7+8)");
		assertEquals(GT6RecipesFood.COAGULATOR_ROWS, countTarget(tTable, GT6RecipesFood.Target.COAGULATOR), "coagulator: the :694-:695 pair");
		assertEquals(GT6RecipesFood.CRYO_MIXER_ROWS, countTarget(tTable, GT6RecipesFood.Target.CRYO_MIXER), "cryo: MultiItemFood:847-850 = 4 waters x 2 shapes");
		assertEquals(GT6RecipesFood.CENTRIFUGE_ROWS, countTarget(tTable, GT6RecipesFood.Target.CENTRIFUGE), "centrifuge: the :660-:664 dairy band");
		assertEquals(GT6RecipesFood.MIXER_ROWS, countTarget(tTable, GT6RecipesFood.Target.MIXER), "mixer: :670 + :729-:731 + :771 x3");
		assertEquals(GT6RecipesFood.BATH_ROWS, countTarget(tTable, GT6RecipesFood.Target.BATH), "bath: 2 egg identities x 4 hot-fluid legs");

		Set<String> tNotes = new HashSet<>();
		for (GT6RecipesFood.Row tRow : tTable) tNotes.add(tRow.note());
		for (String tAnchor : new String[] {":694", ":695", ":660", ":661", ":662", ":664", ":670", ":729", ":730", ":731", ":771",
				":848[water]", ":848[mnwtr]", ":848[distilled_water]", ":848[spdew]",
				":849[water]", ":849[mnwtr]", ":849[distilled_water]", ":849[spdew]",
				":307[brown]", ":309[brown]", ":311[brown]", ":312[brown]",
				":307[white]", ":309[white]", ":311[white]", ":312[white]"}) {
			assertTrue(tNotes.contains(tAnchor), "the upstream anchor " + tAnchor + " is transcribed");
		}
	}

	/**
	 * The TRUE NEGATIVE pin (the B2a coagulator verdict): the :673-:686 band registers
	 * NOTHING here — every row the upstream guards behind a foreign-mod ore-dict/item
	 * probe (dropHoney x3 OD.java:159-160, dropHoneydew, RoyalJelly FR/HaC :680, Glue/Blood
	 * TiC :683-:686) is absent without a GT6 substitute, and the declaration lives in
	 * {@link GT6RecipesFood#SKIPPED_UPSTREAM}.
	 */
	@Test
	void theCoagulatorForeignBandRegistersNothing() {
		for (GT6RecipesFood.Row tRow : GT6RecipesFood.table()) {
			int tLine = upstreamLine(tRow.note());
			assertFalse(tLine >= 673 && tLine <= 686, "no :673-:686 (foreign dropHoney/dropHoneydew/RoyalJelly/Glue/Blood) row: " + tRow.note());
		}
		// the coagulator band is EXACTLY the :694-:695 pair (the :696 MilkSoy→Tofu row is already live — not re-added)
		for (GT6RecipesFood.Row tRow : GT6RecipesFood.table()) {
			if (tRow.target() == GT6RecipesFood.Target.COAGULATOR) {
				assertTrue(tRow.note().equals(":694") || tRow.note().equals(":695"), "coagulator band = the cheese pair only: " + tRow.note());
			}
		}
		assertTrue(GT6RecipesFood.SKIPPED_UPSTREAM.size() >= 4, "the foreign families are declared");
		String tDeclared = String.join("\n", GT6RecipesFood.SKIPPED_UPSTREAM);
		for (String tFamily : new String[] {"dropHoney", "dropHoneydew", "RoyalJelly", "Glue", "Blood", "OD.java:159-160", "TRUE NEGATIVE"}) {
			assertTrue(tDeclared.contains(tFamily), "SKIPPED_UPSTREAM declares " + tFamily);
		}
	}

	// ---------------------------------------------------------------- the verbatim pins

	/** :694-:695 — EUt 0, duration 1024, milk 1000 in, cheese x1 out (the :696 tofu row is NOT here). */
	@Test
	void theCheeseRowsAreVerbatim() {
		GT6RecipesFood.Row t694 = row(":694");
		assertEquals(0, t694.eUt(), ":694 — EUt 0");
		assertEquals(1024, t694.duration(), ":694 — duration 1024");
		assertEquals(1, t694.inFluids().length, ":694 — one fluid in");
		assertEquals("milk", t694.inFluids()[0].name(), ":694 — FL.Milk");
		assertEquals(1000, t694.inFluids()[0].amount(), ":694 — 1000 mB");
		assertEquals(1, t694.outItems().length, ":694 — one item out");
		assertEquals(GT6RecipesFood.FOOD_CHEESE, t694.outItems()[0].foodIndex(), ":694 — Food_Cheese");
		assertEquals(1, t694.outItems()[0].count(), ":694 — cheese x1");

		GT6RecipesFood.Row t695 = row(":695");
		assertEquals("grcmilk.milk", t695.inFluids()[0].name(), ":695 — FL.MilkGrC (the grcmilk carrier)");
		assertEquals(1024, t695.duration(), ":695 — duration 1024");
	}

	/** MultiItemFood:847-850 — the two shapes over the four-carrier waters walk, salt/cream/water/out literals verbatim. */
	@Test
	void theIceCreamBaseRowsAreVerbatim() {
		GT6RecipesFood.Row tSmall = row(":848[water]");
		assertEquals(16, tSmall.eUt(), ":848 — EUt 16");
		assertEquals(16, tSmall.duration(), ":848 — duration 16");
		assertEquals(1, tSmall.inItems().length, ":848 — one item input");
		assertEquals("dustSmall", tSmall.inItems()[0].prefix().mNameInternal, ":848 — OM.dust(NaCl, U4) lands on dustSmall");
		assertEquals("Salt", tSmall.inItems()[0].material().mNameInternal, ":848 — MT.NaCl (the internal name)");
		assertEquals(1, tSmall.inItems()[0].count(), ":848 — 1 salt item");
		assertEquals(2, tSmall.inFluids().length, ":848 — FL.array(tWater, FL.Cream.make(250))");
		assertEquals(250, tSmall.inFluids()[0].amount(), ":848 — tWater at 250");
		assertEquals(250, tSmall.inFluids()[1].amount(), ":848 — cream 250");
		assertEquals("grcmilk.cream", tSmall.inFluids()[1].name(), ":848 — FL.Cream");
		assertEquals(1, tSmall.outItems()[0].count(), ":848 — 1 ice cream");

		GT6RecipesFood.Row tBig = row(":849[spdew]");
		assertEquals(64, tBig.duration(), ":849 — duration 64");
		assertEquals("dust", tBig.inItems()[0].prefix().mNameInternal, ":849 — OM.dust(NaCl, U)");
		assertEquals(1000, tBig.inFluids()[0].amount(), ":849 — FL.mul(tWater, 4)");
		assertEquals(1000, tBig.inFluids()[1].amount(), ":849 — cream 1000");
		assertEquals(4, tBig.outItems()[0].count(), ":849 — 4 ice cream");
		assertEquals("spdew", tBig.inFluids()[0].name(), ":849 — the walk reaches SpDew");

		// the walk width: FL.waters(250) = 4 members, each with both shapes
		assertEquals(GT6RecipesFood.WATER_WALK,
				GT6RecipesFood.table().stream().filter(aRow -> aRow.note().startsWith(":848")).count(),
				":847 — FL.waters(250) = 4 members");
	}

	/** :660-:670 — the dairy butter band: milk family 50→50 at 16t, the two butter rows 250 in at 64t. */
	@Test
	void theButterBandIsVerbatim() {
		GT6RecipesFood.Row t660 = row(":660");
		assertEquals(16, t660.eUt(), ":660 — EUt 16");
		assertEquals(16, t660.duration(), ":660 — duration 16");
		assertEquals(0, t660.inItems().length, ":660 — addRecipe0, no item");
		assertEquals("milk", t660.inFluids()[0].name(), ":660 — FL.Milk");
		assertEquals(50, t660.inFluids()[0].amount(), ":660 — 50 mB");
		assertEquals("grcmilk.cream", t660.outFluids()[0].name(), ":660 — FL.Cream out");
		assertEquals(50, t660.outFluids()[0].amount(), ":660 — cream 50");
		assertEquals("grcmilk.milk", row(":661").inFluids()[0].name(), ":661 — FL.MilkGrC");
		assertEquals("soymilk", row(":662").inFluids()[0].name(), ":662 — FL.MilkSoy");

		GT6RecipesFood.Row t664 = row(":664");
		assertEquals(16, t664.eUt(), ":664 — EUt 16");
		assertEquals(64, t664.duration(), ":664 — duration 64");
		assertEquals(250, t664.inFluids()[0].amount(), ":664 — cream 250");
		assertEquals(0, t664.outFluids().length, ":664 — NF, no fluid out");
		assertEquals(GT6RecipesFood.FOOD_BUTTER, t664.outItems()[0].foodIndex(), ":664 — Food_Butter");
		assertEquals(1, t664.outItems()[0].count(), ":664 — butter x1");

		GT6RecipesFood.Row t670 = row(":670");
		assertEquals(64, t670.duration(), ":670 — duration 64");
		assertEquals(1, t670.inItems().length, ":670 — one item input");
		assertEquals("stick", t670.inItems()[0].prefix().mNameInternal, ":670 — OP.stick");
		assertEquals("WoodTreated", t670.inItems()[0].material().mNameInternal, ":670 — MT.WoodTreated catalyst");
		assertEquals(250, t670.inFluids()[0].amount(), ":670 — cream 250");
	}

	/** :729-:731 + :771 — the salt ladder 144/32/16t (1+9→9, 1+2→2, 1+1→1) and the chili walk 1/4/9. */
	@Test
	void theSaltedButterAndChiliChipsRowsAreVerbatim() {
		long[] tSaltDurations = {144, 32, 16};
		long[] tSaltCounts = {9, 2, 1};
		String[] tSaltNotes = {":729", ":730", ":731"};
		String[] tSaltForms = {"dust", "dustSmall", "dustTiny"};
		for (int i = 0; i < 3; i++) {
			GT6RecipesFood.Row tRow = row(tSaltNotes[i]);
			assertEquals(16, tRow.eUt(), tSaltNotes[i] + " — EUt 16");
			assertEquals(tSaltDurations[i], tRow.duration(), tSaltNotes[i] + " — duration");
			assertEquals(tSaltForms[i], tRow.inItems()[0].prefix().mNameInternal, tSaltNotes[i] + " — the NaCl form");
			assertEquals("Salt", tRow.inItems()[0].material().mNameInternal, tSaltNotes[i] + " — MT.NaCl");
			assertEquals(1, tRow.inItems()[0].count(), tSaltNotes[i] + " — 1 salt item");
			assertEquals(GT6RecipesFood.FOOD_BUTTER, tRow.inItems()[1].foodIndex(), tSaltNotes[i] + " — butter in");
			assertEquals(tSaltCounts[i], tRow.inItems()[1].count(), tSaltNotes[i] + " — the butter count");
			assertEquals(tSaltCounts[i], tRow.outItems()[0].count(), tSaltNotes[i] + " — the salted count");
		}

		long[] tChiliCounts = {1, 4, 9};
		int tSeen = 0;
		for (GT6RecipesFood.Row tRow : GT6RecipesFood.table()) {
			if (!tRow.note().equals(":771")) continue;
			final int tIndex = tSeen++;
			assertEquals(16, tRow.eUt(), ":771 — EUt 16");
			assertEquals(16, tRow.duration(), ":771 — duration 16");
			assertEquals(GT6RecipesFood.FOOD_POTATO_CHIPS, tRow.inItems()[1].foodIndex(), ":771 — Food_PotatoChips in");
			assertEquals(1, tRow.inItems()[1].count(), ":771 — 1 chips");
			assertEquals(GT6RecipesFood.FOOD_CHILI_CHIPS, tRow.outItems()[0].foodIndex(), ":771 — Food_ChiliChips out");
			assertEquals(tChiliCounts[tIndex], tRow.inItems()[0].count(), ":771 — the chili form count " + tIndex);
		}
		assertEquals(3, tSeen, ":770 — ST.array(dust, dustSmall, dustTiny) = 3 forms");
	}

	/** :303-:312 — the Bath arm: 128t / EUt 0 / 100→100, brown keyed on the vanilla egg, the shell on the Dye_Bonemeal fallback. */
	@Test
	void theBathEggArmIsVerbatim() {
		GT6RecipesFood.Row tBrown = row(":309[brown]"); // the FL.Hot_Water leg
		assertEquals(0, tBrown.eUt(), ":309 — EUt 0");
		assertEquals(128, tBrown.duration(), ":309 — duration 128 x tAmount(1)");
		assertEquals(100, tBrown.inFluids()[0].amount(), ":309 — the hot fluid 100");
		assertEquals(100, tBrown.outFluids()[0].amount(), ":309 — water 100 out");
		assertEquals("water", tBrown.outFluids()[0].name(), ":309 — FL.Water out");
		assertEquals(1, tBrown.inItems().length, ":309 — one egg in");
		assertNotNull(tBrown.inItems()[0].vanilla(), ":309 — the brown input is the vanilla egg identity");
		assertEquals(GT6RecipesFood.FOOD_BROWN_EGG_BOILED, tBrown.outItems()[0].foodIndex(), ":309 — ST.equal(Items.egg) → Food_Brown_Egg_Boiled");
		assertEquals(1, tBrown.outItems()[0].count(), ":309 — tAmount 1");
		assertEquals(2, tBrown.outItems().length, ":309 — the egg shell rides with it");

		GT6RecipesFood.Row tWhite = row(":311[white]");
		assertEquals(GT6RecipesFood.FOOD_WHITE_EGG_BOILED, tWhite.outItems()[0].foodIndex(), ":311 — the GT6 egg identity → Food_White_Egg_Boiled");

		// the four legs in upstream order, the ic2hotwater one included (it is the deliberate null at resolve time)
		assertEquals("ic2hotwater", row(":307[brown]").inFluids()[0].name(), ":306-:307 — FL.Water_Hot (the IC2 alias)");
		assertEquals("hot_water", row(":309[brown]").inFluids()[0].name(), ":308-:309 — FL.Hot_Water");
		assertEquals("water_boiling", row(":311[brown]").inFluids()[0].name(), ":310-:311 — FL.Water_Boiling");
		assertEquals("water_geothermal", row(":312[brown]").inFluids()[0].name(), ":312 — FL.Water_Geothermal");
		assertNull(GT6RecipesFood.resolveNamedFluid(GT6RecipesFood.WATER_HOT_IC2),
				"the ic2hotwater carrier is the DELIBERATE null (the Drying :530 verdict — no IC2 alias fabricated)");
	}

	// ---------------------------------------------------------------- the wiring

	/** The {@link GT6Foods#FOOD_ROWS} index constants pin id-by-index — an items-core reorder trips here, not in the rows. */
	@Test
	void theFoodIndexConstantsPinTheItemsCoreTable() {
		assertEquals("food_cheese", GT6Foods.FOOD_ROWS.get(GT6RecipesFood.FOOD_CHEESE).id());
		assertEquals("food_brown_egg_boiled", GT6Foods.FOOD_ROWS.get(GT6RecipesFood.FOOD_BROWN_EGG_BOILED).id());
		assertEquals("food_white_egg_boiled", GT6Foods.FOOD_ROWS.get(GT6RecipesFood.FOOD_WHITE_EGG_BOILED).id());
		assertEquals("food_potato_chips", GT6Foods.FOOD_ROWS.get(GT6RecipesFood.FOOD_POTATO_CHIPS).id());
		assertEquals("food_chili_chips", GT6Foods.FOOD_ROWS.get(GT6RecipesFood.FOOD_CHILI_CHIPS).id());
		assertEquals("food_ice_cream", GT6Foods.FOOD_ROWS.get(GT6RecipesFood.FOOD_ICE_CREAM).id());
		assertEquals("food_butter", GT6Foods.FOOD_ROWS.get(GT6RecipesFood.FOOD_BUTTER).id());
		assertEquals("food_butter_salted", GT6Foods.FOOD_ROWS.get(GT6RecipesFood.FOOD_BUTTER_SALTED).id());
	}

	// ---------------------------------------------------------------- the pour faces

	/**
	 * The live-seam reconciliation (both legs): every row is either poured or accounted on
	 * the skip side, and the two DECLARED dormancies never pour — the raw GT6 white egg is
	 * port-absent and the ic2hotwater carrier is the deliberate null on BOTH legs. The
	 * pour COUNT is a leg fact: the forge offline JVM binds nothing (0 pour / 29 skip),
	 * the 21.1 FML test JVM binds the registries (24 pour / 5 skip — the port reality).
	 */
	@Test
	void theLiveUniverseReconcilesWithTheDeclaredDormancies() {
		GT6RecipesFood.load();
		assertEquals(GT6RecipesFood.TOTAL_ROWS, totalPoured() + totalSkipped(), "every row accounted: poured + skipped = 29");
		assertNull(GT6RecipesFood.sWhiteEggResolver.get(), "the raw GT6 white egg is port-absent (the declared dormancy, both legs)");
		assertNull(GT6RecipesFood.resolveNamedFluid(GT6RecipesFood.WATER_HOT_IC2), "the ic2hotwater alias is the deliberate null (both legs)");
		// whichever rows DID pour, the Bath arm carries the :306-:312 columns
		for (Object tRowObject : GT6RecipeMaps.BATH.mRecipeList) {
			Recipe tRow = (Recipe) tRowObject;
			assertEquals(128, tRow.mDuration, ":306-:312 — duration 128");
			assertEquals(0, tRow.mEUt, ":306-:312 — EUt 0");
			assertEquals(2, tRow.mOutputs.length, ":306-:312 — the boiled egg + the shell by-product");
		}
		assertTrue(GT6RecipesFood.lastPoured(GT6RecipesFood.Target.BATH) <= 3, "at most the 3 vanilla-egg hot-fluid legs pour (white egg + ic2hotwater stay dormant)");
	}

	/**
	 * The port reality: fixtures mirroring the registered universe (the material items, the
	 * nine foods, the milk/cream/waters carriers — but NOT the raw white egg and NOT the
	 * ic2hotwater alias) pour 24 / skip the 5 declared dormancies (bath 3: the vanilla-egg
	 * rows minus the :307 leg).
	 */
	@Test
	void thePortUniversePours24With5DeclaredDormancies() {
		bindSeams(null, false); // the white egg stays absent, the ic2hotwater alias stays null
		GT6RecipesFood.resetForTest();
		GT6RecipesFood.load();

		assertEquals(24, totalPoured(), "the port reality: 24 pours");
		assertEquals(5, totalSkipped(), "the declared dormancies: 4 white-egg rows + 1 ic2hotwater leg");
		assertEquals(2, GT6RecipesFood.lastPoured(GT6RecipesFood.Target.COAGULATOR), "coagulator 2");
		assertEquals(8, GT6RecipesFood.lastPoured(GT6RecipesFood.Target.CRYO_MIXER), "cryo 8");
		assertEquals(4, GT6RecipesFood.lastPoured(GT6RecipesFood.Target.CENTRIFUGE), "centrifuge 4");
		assertEquals(7, GT6RecipesFood.lastPoured(GT6RecipesFood.Target.MIXER), "mixer 7");
		assertEquals(3, GT6RecipesFood.lastPoured(GT6RecipesFood.Target.BATH), "bath 3 (the :307 ic2hotwater leg dormant)");

		// the poured Bath rows carry the :306-:312 columns
		for (Object tRowObject : GT6RecipeMaps.BATH.mRecipeList) {
			Recipe tRow = (Recipe) tRowObject;
			assertEquals(128, tRow.mDuration, ":306-:312 — duration 128");
			assertEquals(0, tRow.mEUt, ":306-:312 — EUt 0");
			assertEquals(1, tRow.mFluidInputs.length, ":306-:312 — one hot fluid in");
			assertEquals(100, tRow.mFluidInputs[0].getAmount(), ":306-:312 — 100 mB in");
			assertEquals(100, tRow.mFluidOutputs[0].getAmount(), ":306-:312 — 100 mB water out");
			assertEquals(2, tRow.mOutputs.length, ":306-:312 — the boiled egg + the shell by-product");
		}
		// the index wiring rides through build: the :664 output is the Food_Butter fixture
		Item tButter = null;
		for (Object tRowObject : GT6RecipeMaps.CENTRIFUGE.mRecipeList) {
			Recipe tRow = (Recipe) tRowObject;
			if (tRow.mOutputs.length > 0 && tRow.mOutputs[0].getItem() == Items.GOLD_INGOT) tButter = Items.GOLD_INGOT;
		}
		assertNotNull(tButter, "the :664 row poured with Food_Butter wired to index " + GT6RecipesFood.FOOD_BUTTER);
	}

	/**
	 * The pour-face-forever proof (the Bath wood-ladder form): with the raw white egg AND
	 * the ic2hotwater alias bound, the SAME load() pours all 29 — the 5 dormancies are
	 * universe facts, not code walls.
	 */
	@Test
	void theFullAssemblyPoursAll29WhenTheDormantLegsLand() {
		bindSeams(Items.FEATHER, true); // a distinct stand-in for the future raw white egg, the alias bound
		GT6RecipesFood.resetForTest();
		GT6RecipesFood.load();

		assertEquals(GT6RecipesFood.TOTAL_ROWS, totalPoured(), "all 29 pour");
		assertEquals(0, totalSkipped(), "none skipped");
		assertEquals(8, GT6RecipesFood.lastPoured(GT6RecipesFood.Target.BATH), "bath 8 (both egg identities, all four legs)");

		// the white face: a poured row carries the FEATHER stand-in as its input
		boolean tWhiteFace = false;
		for (Object tRowObject : GT6RecipeMaps.BATH.mRecipeList) {
			Recipe tRow = (Recipe) tRowObject;
			if (tRow.mInputs[0].getItem() == Items.FEATHER) tWhiteFace = true;
		}
		assertTrue(tWhiteFace, "the raw-white-egg rows are live code (they pour the moment the item lands)");
	}

	// ---------------------------------------------------------------- helpers

	/**
	 * Fixtures mirroring the registered port universe: the material pairs → brick, the
	 * foods → distinct items, the named carriers → water. {@code aBindIc2HotWater} picks
	 * between the port reality (the alias null) and the full-assembly future (bound).
	 */
	private static void bindSeams(Item aWhiteEgg, boolean aBindIc2HotWater) {
		GT6RecipesFood.sMaterialItemResolver = (aPrefix, aMaterial) -> Items.BRICK;
		GT6RecipesFood.sFoodItemResolver = GT6RecipesFoodTest::fixtureFood;
		GT6RecipesFood.sWhiteEggResolver = () -> aWhiteEgg;
		GT6RecipesFood.sNamedFluidResolver = aName -> !GT6RecipesFood.WATER_HOT_IC2.equals(aName) || aBindIc2HotWater ? Fluids.WATER : null;
	}

	/** Distinct per-index items so the poured outputs prove the index wiring (a vacuous all-brick map would not). */
	static Item fixtureFood(int aIndex) {
		return switch (aIndex) {
			case GT6RecipesFood.FOOD_CHEESE -> Items.BREAD;
			case GT6RecipesFood.FOOD_BROWN_EGG_BOILED -> Items.COOKIE;
			case GT6RecipesFood.FOOD_WHITE_EGG_BOILED -> Items.PAPER;
			case GT6RecipesFood.FOOD_POTATO_CHIPS -> Items.PUMPKIN_PIE;
			case GT6RecipesFood.FOOD_CHILI_CHIPS -> Items.COOKED_CHICKEN;
			case GT6RecipesFood.FOOD_ICE_CREAM -> Items.MILK_BUCKET;
			case GT6RecipesFood.FOOD_BUTTER -> Items.GOLD_INGOT;
			case GT6RecipesFood.FOOD_BUTTER_SALTED -> Items.IRON_INGOT;
			default -> throw new IllegalArgumentException("fixture food index " + aIndex);
		};
	}

	private static int countTarget(List<GT6RecipesFood.Row> aTable, GT6RecipesFood.Target aTarget) {
		int rCount = 0;
		for (GT6RecipesFood.Row tRow : aTable) if (tRow.target() == aTarget) rCount++;
		return rCount;
	}

	private static GT6RecipesFood.Row row(String aNote) {
		for (GT6RecipesFood.Row tRow : GT6RecipesFood.table()) if (tRow.note().equals(aNote)) return tRow;
		throw new AssertionError("no row " + aNote);
	}

	/** The upstream line out of a note (":848[water]" → 848). */
	private static int upstreamLine(String aNote) {
		return Integer.parseInt(aNote.substring(1, aNote.indexOf('[') < 0 ? aNote.length() : aNote.indexOf('[')));
	}

	private static int totalPoured() {
		int rSum = 0;
		for (GT6RecipesFood.Target tTarget : GT6RecipesFood.Target.values()) rSum += GT6RecipesFood.lastPoured(tTarget);
		return rSum;
	}

	private static int totalSkipped() {
		int rSum = 0;
		for (GT6RecipesFood.Target tTarget : GT6RecipesFood.Target.values()) rSum += GT6RecipesFood.lastSkipped(tTarget);
		return rSum;
	}
}
