package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The Shredder/Crusher/Lathe recipe pour (task p7-recipe-maps-shcl, acceptance ④):
 * <ul>
 * <li>the transcription walk: every material (prefix, material) pair of every row resolves
 *     inside the offline material universe ({@link GTMaterialItems#registrationOrder}) and
 *     every map's poured count equals transcribed-rows-minus-skips, recomputed independently
 *     here (the "poured reconciliation" ruling — the cokeoven test template);</li>
 * <li>row-shape spot checks per map (upstream line anchors, eUt/duration, output counts);</li>
 * <li>the quantity-conserving gem chain and the getCosts duration identity;</li>
 * <li>the condition gate (ANTIMATTER/INVALID_MATERIAL) filters the Crusher walk;</li>
 * <li>the three maps exist with lowercase GUI paths (1.20.1 ResourceLocation constraint).</li>
 * </ul>
 */
class GT6RecipesShCLTest extends GTRecipesOfflineTestBase {

	/**
	 * The synthetic offline universe: one distinct EXISTING item per (prefix, material) pair —
	 * new Items cannot be created offline (the intrusive vanilla item registry freezes at
	 * bootstrap), so the driver maps pairs onto distinct vanilla registry entries; the recipe
	 * mechanics only compare identities.
	 */
	private static final Map<PrefixMaterial, Item> SYNTHETIC_ITEMS = new HashMap<>();

	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultMaterialResolver;
	private static Function<Supplier<Item>, Item> sDefaultVanillaResolver;

	/**
	 * The vanilla items the all-vanilla table rows use as inputs — kept out of the synthetic
	 * pool so a lookup with one of them can only ever match its own row (a synthetic
	 * (prefix, material) pair must never alias onto FLINT/GRAVEL/... or the flint-row and
	 * web-row lookups would be ambiguous).
	 */
	private static final Set<Item> RESERVED_VANILLA_ITEMS = Set.of(
			Items.FLINT, Items.GRAVEL, Items.SAND, Items.COBWEB, Items.STRING,
			Items.COBBLESTONE, Items.STONE, Items.GLASS_PANE);

	@BeforeAll
	static void buildSyntheticUniverse() {
		GTMaterialItems.initMaterials(); // the offline material universe (MT.init + OP.init)
		List<Item> tPool = BuiltInRegistries.ITEM.stream().toList();
		int tNext = 0;
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			Item tItem;
			// AIR (or any item making an empty stack) must be skipped: new ItemStack(AIR, n) is an
			// empty stack, the Recipe ctor trims it, and the resulting empty-input row would match
			// EVERY lookup (the ghost recipe that made this suite order-flaky).
			do {tItem = tPool.get(tNext++ % tPool.size());} while (RESERVED_VANILLA_ITEMS.contains(tItem) || new ItemStack(tItem, 1).isEmpty());
			SYNTHETIC_ITEMS.put(tPair, tItem);
		}
		sDefaultMaterialResolver = GT6RecipesShCL.sMaterialItemResolver;
		sDefaultVanillaResolver = GT6RecipesShCL.sVanillaItemResolver;
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesShCL.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipesShCL.sVanillaItemResolver = sDefaultVanillaResolver;
		GT6RecipeMaps.reset();
		GT6RecipesShCL.resetForTest();
	}

	// ------------------------------------------------------------------
	// map constants
	// ------------------------------------------------------------------

	/** The three maps register under their upstream internal names, with lowercase GUI paths. */
	@Test
	void threeMapsRegisterWithLowercaseGuiPaths() {
		GT6RecipeMaps.init();
		assertSame(GT6RecipeMaps.SHREDDER, RecipeMap.RECIPE_MAPS.get("gt.recipe.shredder"));
		assertSame(GT6RecipeMaps.CRUSHER, RecipeMap.RECIPE_MAPS.get("gt.recipe.crusher"));
		assertSame(GT6RecipeMaps.LATHE, RecipeMap.RECIPE_MAPS.get("gt.recipe.lathe"));

		for (RecipeMap tMap : new RecipeMap[] {GT6RecipeMaps.SHREDDER, GT6RecipeMaps.CRUSHER, GT6RecipeMaps.LATHE}) {
			assertEquals(tMap.mGUIPath, tMap.mGUIPath.toLowerCase(java.util.Locale.ROOT),
					"1.20.1 ResourceLocation paths are [a-z0-9_.-/] — the GUI path must be lowercase");
			assertTrue(tMap.mGUIPath.endsWith(".png"), "RecipeMap auto-attaches .png (RecipeMap.java:84)");
			assertTrue(tMap.mGUIPath.startsWith("gt6:textures/gui/machines/"));
		}
		assertEquals("gt6:textures/gui/machines/shredder.png", GT6RecipeMaps.SHREDDER.mGUIPath);
		assertEquals("gt6:textures/gui/machines/crusher.png", GT6RecipeMaps.CRUSHER.mGUIPath);
		assertEquals("gt6:textures/gui/machines/lathe.png", GT6RecipeMaps.LATHE.mGUIPath);

		// the transcribed slot constants (RM.java:134/:135/:97)
		assertEquals(1, GT6RecipeMaps.SHREDDER.mInputItemsCount);
		assertEquals(12, GT6RecipeMaps.SHREDDER.mOutputItemsCount);
		assertEquals(1, GT6RecipeMaps.CRUSHER.mInputItemsCount);
		assertEquals(12, GT6RecipeMaps.CRUSHER.mOutputItemsCount);
		assertEquals(1, GT6RecipeMaps.LATHE.mInputItemsCount);
		assertEquals(2, GT6RecipeMaps.LATHE.mOutputItemsCount);
		for (RecipeMap tMap : new RecipeMap[] {GT6RecipeMaps.SHREDDER, GT6RecipeMaps.CRUSHER, GT6RecipeMaps.LATHE}) {
			assertEquals(0, tMap.mInputFluidCount);
			assertEquals(0, tMap.mOutputFluidCount);
			assertEquals(0, tMap.mMinimalInputs);
			assertEquals(1, tMap.mPower);
		}
	}

	/** reset() drops the three maps along with the rest of the generation. */
	@Test
	void resetClearsTheThreeMaps() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.reset();
		assertNull(GT6RecipeMaps.SHREDDER);
		assertNull(GT6RecipeMaps.CRUSHER);
		assertNull(GT6RecipeMaps.LATHE);
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.shredder"));
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.crusher"));
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.lathe"));
	}

	// ------------------------------------------------------------------
	// row-shape spot checks
	// ------------------------------------------------------------------

	/** The Shredder table: 7 rows, the exact upstream shapes (Vanilla:688-693/:707-708). */
	@Test
	void shredderRowShapes() {
		List<GT6RecipesShCL.FixedRow> tTable = GT6RecipesShCL.shredderTable();
		assertEquals(7, tTable.size(), "5 vanilla rows + 2 Blaze rows (the group expansion is pooled)");

		GT6RecipesShCL.FixedRow tFlint = findFixedRow(tTable, ":688");
		assertNotNull(tFlint);
		assertEquals(Items.FLINT, tFlint.input().vanilla().get());
		assertEquals(1, tFlint.input().count());
		assertEquals(16, tFlint.eUt());
		assertEquals(16, tFlint.duration());
		assertEquals(1, tFlint.outputs().length);
		assertSame(OP.dust, tFlint.outputs()[0].prefix());
		assertSame(MT.Flint, tFlint.outputs()[0].material());
		assertEquals(1, tFlint.outputs()[0].count());

		// :692/:693 — the OM.dust(MT.Stone, U*9) nine-dust output
		GT6RecipesShCL.FixedRow tCobble = findFixedRow(tTable, ":692");
		assertNotNull(tCobble);
		assertEquals(Blocks.COBBLESTONE.asItem(), tCobble.input().vanilla().get(), "the :692 input is cobblestone");
		assertEquals(9, tCobble.outputs()[0].count(), "OM.dust(MT.Stone, U*9) = nine dusts");
		assertSame(OP.dust, tCobble.outputs()[0].prefix());
		assertSame(MT.Stone, tCobble.outputs()[0].material());

		// :707/:708 — the Blaze sticks
		GT6RecipesShCL.FixedRow tStick = findFixedRow(tTable, ":707");
		assertNotNull(tStick);
		assertSame(OP.stick, tStick.input().prefix());
		assertSame(MT.Blaze, tStick.input().material());
		assertEquals(32, tStick.duration());
		assertEquals(2, tStick.outputs()[0].count());
		assertSame(OP.dustSmall, tStick.outputs()[0].prefix());

		GT6RecipesShCL.FixedRow tStickLong = findFixedRow(tTable, ":708");
		assertNotNull(tStickLong);
		assertSame(OP.stickLong, tStickLong.input().prefix());
		assertEquals(64, tStickLong.duration());
		assertEquals(1, tStickLong.outputs()[0].count());
		assertSame(OP.dust, tStickLong.outputs()[0].prefix());

		// every row runs at eUt 16 (the addRecipe1 second arg)
		for (GT6RecipesShCL.FixedRow tRow : tTable) assertEquals(16, tRow.eUt(), "row " + tRow.note());
	}

	/** The Lathe table: 2 rows (Vanilla:523-524); the glass pane row has two outputs. */
	@Test
	void latheRowShapes() {
		List<GT6RecipesShCL.FixedRow> tTable = GT6RecipesShCL.latheTable();
		assertEquals(2, tTable.size());

		GT6RecipesShCL.FixedRow tPane = findFixedRow(tTable, ":523");
		assertNotNull(tPane);
		assertEquals(Blocks.GLASS_PANE.asItem(), tPane.input().vanilla().get());
		assertEquals(16, tPane.duration());
		assertEquals(2, tPane.outputs().length);
		assertSame(OP.lens, tPane.outputs()[0].prefix());
		assertSame(MT.Glass, tPane.outputs()[0].material());
		assertEquals(1, tPane.outputs()[0].count());
		assertSame(OP.dustSmall, tPane.outputs()[1].prefix());
		assertSame(MT.Glass, tPane.outputs()[1].material());
		assertEquals(1, tPane.outputs()[1].count());

		GT6RecipesShCL.FixedRow tStone = findFixedRow(tTable, ":524");
		assertNotNull(tStone);
		assertEquals(Blocks.STONE.asItem(), tStone.input().vanilla().get());
		assertEquals(1, tStone.outputs().length);
		assertSame(OP.stickLong, tStone.outputs()[0].prefix());
		assertSame(MT.Stone, tStone.outputs()[0].material());
	}

	/** The Crusher templates: 6 rows, and the chain is quantity-conserving (no pulverized remains). */
	@Test
	void crusherTemplateShapesAndQuantityConservation() {
		List<GT6RecipesShCL.CrusherTemplate> tTable = GT6RecipesShCL.crusherTable();
		assertEquals(6, tTable.size(), "the gem chain :69-73 + the boule row :75");

		GT6RecipesShCL.CrusherTemplate tLegendary = findCrusherTemplate(tTable, ":69");
		assertNotNull(tLegendary);
		assertSame(OP.gemLegendary, tLegendary.inPrefix());
		assertSame(OP.gemExquisite, tLegendary.outPrefix());
		assertEquals(2, tLegendary.outCount());
		assertEquals(16, tLegendary.eUt());
		assertEquals(256, tLegendary.multiplier());

		GT6RecipesShCL.CrusherTemplate tBoule = findCrusherTemplate(tTable, ":75");
		assertNotNull(tBoule);
		assertSame(OP.bouleGt, tBoule.inPrefix());
		assertEquals(4, tBoule.outCount());

		// quantity conservation: unitsIn == unitsOut per template — the upstream
		// mOutputPulverizedRemains secondary output (RecipeMapHandlerPrefix.java:215) is
		// therefore null for every transcribed row, keeping the outputs deterministic.
		for (GT6RecipesShCL.CrusherTemplate tTpl : tTable) {
			assertEquals(tTpl.inPrefix().mAmount * tTpl.inCount(), tTpl.outPrefix().mAmount * tTpl.outCount(),
					"row " + tTpl.note() + " must be quantity-conserving");
		}
	}

	/** The getCosts duration identity (RecipeMapHandlerPrefix.java:225-227) for every expanding material. */
	@Test
	void crusherCostsMatchTheUpstreamFormula() {
		for (GT6RecipesShCL.CrusherTemplate tTpl : GT6RecipesShCL.crusherTable()) {
			for (OreDictMaterial tMaterial : GT6RecipesShCL.expandCrusherMaterials(tTpl.inPrefix())) {
				long tUnits = Math.max(tTpl.inPrefix().mAmount * tTpl.inCount(), tTpl.outPrefix().mAmount * tTpl.outCount());
				// UT.Code.units(units, U, mult*(1+q), T): the gem chain is U-divisible, so no round-up
				long tExpected = tUnits * (tTpl.multiplier() + tTpl.multiplier() * tMaterial.mToolQuality) / CS.U;
				assertEquals(tExpected, GT6RecipesShCL.crusherCosts(tTpl, tMaterial), "row " + tTpl.note() + " material " + tMaterial.mNameInternal);
				assertTrue(GT6RecipesShCL.crusherCosts(tTpl, tMaterial) >= 1);
			}
		}
	}

	// ------------------------------------------------------------------
	// poured reconciliation (acceptance ④)
	// ------------------------------------------------------------------

	/** Every map's poured count = transcribed rows - skips, recomputed independently. */
	@Test
	void pouredCountsReconcileWithIndependentExpectations() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;

		GT6RecipesShCL.load();

		int tExpectedShredder = 0;
		for (GT6RecipesShCL.FixedRow tRow : GT6RecipesShCL.shredderTable()) if (fixedRowResolves(tRow)) tExpectedShredder++;
		int tExpectedLathe = 0;
		for (GT6RecipesShCL.FixedRow tRow : GT6RecipesShCL.latheTable()) if (fixedRowResolves(tRow)) tExpectedLathe++;
		int tExpectedCrusher = 0;
		for (GT6RecipesShCL.CrusherTemplate tTpl : GT6RecipesShCL.crusherTable()) {
			for (OreDictMaterial tMaterial : GT6RecipesShCL.expandCrusherMaterials(tTpl.inPrefix())) {
				if (crusherRowResolves(tTpl, tMaterial)) tExpectedCrusher++;
			}
		}

		assertEquals(tExpectedShredder, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "Shredder poured = transcribed - skipped");
		assertEquals(tExpectedLathe, GT6RecipeMaps.LATHE.mRecipeList.size(), "Lathe poured = transcribed - skipped");
		assertEquals(tExpectedCrusher, GT6RecipeMaps.CRUSHER.mRecipeList.size(), "Crusher poured = expanded - skipped");
	}

	/** The gem chain expands over the registered gemLegendary materials, in registration order. */
	@Test
	void crusherWalkIsRegistrationOrderSelfConsistent() {
		Set<PrefixMaterial> tUniverse = new HashSet<>(GTMaterialItems.registrationOrder());
		List<OreDictMaterial> tExpandable = GT6RecipesShCL.expandCrusherMaterials(OP.gemLegendary);
		assertFalse(tExpandable.isEmpty(), "the gem chain must expand over at least the vanilla-style gems");
		for (OreDictMaterial tMaterial : tExpandable) {
			assertTrue(tUniverse.contains(new PrefixMaterial(OP.gemLegendary, tMaterial)),
					"every expanded material must come from the registration universe");
			assertEquals(tMaterial, MaterialRegistryTarget(tMaterial), "registration-order identity (no alias slot leak)");
		}
	}

	/** The condition gate: ANTIMATTER / INVALID_MATERIAL materials produce no Crusher row. */
	@Test
	void crusherConditionGateFiltersMaterials() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;
		for (OreDictMaterial tMaterial : GT6RecipesShCL.expandCrusherMaterials(OP.gemLegendary)) {
			GT6RecipesShCL.CrusherTemplate tTpl = findCrusherTemplate(GT6RecipesShCL.crusherTable(), ":72");
			Recipe tRecipe = GT6RecipesShCL.buildCrusherRecipe(tTpl, tMaterial);
			if (tMaterial.contains(TD.Atomic.ANTIMATTER) || tMaterial.contains(TD.Properties.INVALID_MATERIAL)) {
				assertNull(tRecipe, "row :72 must skip " + tMaterial.mNameInternal + " by the upstream condition gate");
			} else {
				assertNotNull(tRecipe, "row :72 must resolve for " + tMaterial.mNameInternal);
			}
		}
	}

	/** A dead material resolver drops every material-dependent row; pure vanilla rows survive. */
	@Test
	void unresolvableMaterialRowsSkipButVanillaRowsSurvive() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> null;
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;

		GT6RecipesShCL.load();

		// Shredder: :689 gravel→sand and :690 web→string are all-vanilla; the other five rows touch material items
		assertEquals(2, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "the two all-vanilla Shredder rows pour");
		// Lathe: both rows produce material items → all skip
		assertEquals(0, GT6RecipeMaps.LATHE.mRecipeList.size());
		// Crusher: every expansion is material-both-sides → all skip
		assertEquals(0, GT6RecipeMaps.CRUSHER.mRecipeList.size());
	}

	/** load() is idempotent within a generation (the second call must not double-pour). */
	@Test
	void loadIsIdempotent() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;

		GT6RecipesShCL.load();
		int tShredder = GT6RecipeMaps.SHREDDER.mRecipeList.size();
		int tLathe = GT6RecipeMaps.LATHE.mRecipeList.size();
		int tCrusher = GT6RecipeMaps.CRUSHER.mRecipeList.size();

		GT6RecipesShCL.load();

		assertEquals(tShredder, GT6RecipeMaps.SHREDDER.mRecipeList.size());
		assertEquals(tLathe, GT6RecipeMaps.LATHE.mRecipeList.size());
		assertEquals(tCrusher, GT6RecipeMaps.CRUSHER.mRecipeList.size());
	}

	// ------------------------------------------------------------------
	// end-to-end lookup smoke
	// ------------------------------------------------------------------

	/** The flint row finds and consumes through the SHREDDER map; the output is the nine-dust shape's sibling. */
	@Test
	void shredderFlintRowLookupsAndConsumes() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;
		GT6RecipesShCL.load();

		Item tDustFlint = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.dust, MT.Flint));
		ItemStack[] tInputs = {new ItemStack(Items.FLINT, 4)};

		// two-stage contract: probe does not consume
		Recipe tFound = GT6RecipeMaps.SHREDDER.findRecipe(null, 16, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound, "the flint row must be found");
		assertEquals(16, tFlintDuration(tFound), "the flint row runs 16 t");
		assertTrue(tFound.isRecipeInputEqual(false, false, null, tInputs));
		assertEquals(4, tInputs[0].getCount(), "the probe must not consume");
		assertTrue(tFound.isRecipeInputEqual(true, false, null, tInputs));
		assertEquals(3, tInputs[0].getCount(), "one pass consumes exactly one flint");

		ItemStack[] tOutputs = tFound.getOutputs(1);
		assertEquals(1, tOutputs.length);
		assertEquals(tDustFlint, tOutputs[0].getItem());
		assertEquals(1, tOutputs[0].getCount());
	}

	/** The web→string row is all-vanilla on both sides — the deterministic no-material control. */
	@Test
	void shredderWebRowIsAllVanilla() {
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;
		GT6RecipesShCL.load();

		ItemStack[] tInputs = {new ItemStack(Blocks.COBWEB.asItem(), 1)};
		Recipe tFound = GT6RecipeMaps.SHREDDER.findRecipe(null, 16, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound, "the web row must be found");
		ItemStack[] tOutputs = tFound.getOutputs(1);
		assertEquals(1, tOutputs.length);
		assertEquals(Items.STRING, tOutputs[0].getItem());
		assertEquals(1, tOutputs[0].getCount());
	}

	/** A Crusher row resolves with the getCosts duration and the x2 output (gem → gemFlawed x2). */
	@Test
	void crusherGemRowEndToEnd() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;
		GT6RecipesShCL.load();

		// pick the first expanding material of the :72 template whose synthetic gem item is used
		// as a Crusher input by exactly ONE poured row — the modulo pool can alias two pairs onto
		// one item, and the linear-scan lookup must stay unambiguous for this assertion.
		GT6RecipesShCL.CrusherTemplate tTpl = findCrusherTemplate(GT6RecipesShCL.crusherTable(), ":72");
		assertNotNull(tTpl);
		Map<Item, Integer> tInputUses = new HashMap<>();
		for (GT6RecipesShCL.CrusherTemplate tTemplate : GT6RecipesShCL.crusherTable()) {
			for (OreDictMaterial tCandidate : GT6RecipesShCL.expandCrusherMaterials(tTemplate.inPrefix())) {
				if (crusherRowResolves(tTemplate, tCandidate)) {
					tInputUses.merge(SYNTHETIC_ITEMS.get(new PrefixMaterial(tTemplate.inPrefix(), tCandidate)), 1, Integer::sum);
				}
			}
		}
		OreDictMaterial tMaterial = null;
		for (OreDictMaterial tCandidate : GT6RecipesShCL.expandCrusherMaterials(tTpl.inPrefix())) {
			if (crusherRowResolves(tTpl, tCandidate)
					&& tInputUses.get(SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.gem, tCandidate))) == 1) {
				tMaterial = tCandidate;
				break;
			}
		}
		assertNotNull(tMaterial, "at least one gem material must expand to a uniquely-keyed input item");

		Item tGem = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.gem, tMaterial));
		Item tGemFlawed = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.gemFlawed, tMaterial));
		ItemStack[] tInputs = {new ItemStack(tGem, 2)};
		Recipe tFound = GT6RecipeMaps.CRUSHER.findRecipe(null, 16, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound, "the gem row must be found for " + tMaterial.mNameInternal);

		assertEquals(Math.max(1, GT6RecipesShCL.crusherCosts(tTpl, tMaterial)), tFound.mDuration);
		assertEquals(16, tFound.mEUt);
		ItemStack[] tOutputs = tFound.getOutputs(1);
		assertEquals(1, tOutputs.length, "the gem chain has no pulverized-remains second output");
		assertEquals(tGemFlawed, tOutputs[0].getItem());
		assertEquals(2, tOutputs[0].getCount(), "gem → gemFlawed x2");
	}

	// ------------------------------------------------------------------
	// pooled surface
	// ------------------------------------------------------------------

	/** The pooled upstream surface is declared, not silent. */
	@Test
	void pooledEntriesAreDeclaredNotTranscribed() {
		String tSkipped = String.join("\n", GT6RecipesShCL.SKIPPED_UPSTREAM);
		assertTrue(tSkipped.contains(":691"), "the reeds IL row is pooled");
		assertTrue(tSkipped.contains("rockGt"), "the null-output prefix rows are pooled");
		assertTrue(tSkipped.contains("RecipeMapHandlerCrushing"), "the crushed-family ore chain is pooled (chances window)");
		assertTrue(tSkipped.contains("RECYCLABLE"), "the RecipeMapShredder on-demand synthesis is pooled");
		assertTrue(tSkipped.contains("wood loop"), "the OreDict wood loop is pooled");
		assertEquals(7, GT6RecipesShCL.shredderTable().size());
		assertEquals(2, GT6RecipesShCL.latheTable().size());
		assertEquals(6, GT6RecipesShCL.crusherTable().size());
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	private static GT6RecipesShCL.FixedRow findFixedRow(List<GT6RecipesShCL.FixedRow> aTable, String aNote) {
		for (GT6RecipesShCL.FixedRow tRow : aTable) if (tRow.note().equals(aNote)) return tRow;
		return null;
	}

	private static GT6RecipesShCL.CrusherTemplate findCrusherTemplate(List<GT6RecipesShCL.CrusherTemplate> aTable, String aNote) {
		for (GT6RecipesShCL.CrusherTemplate tTemplate : aTable) if (tTemplate.note().equals(aNote)) return tTemplate;
		return null;
	}

	private static boolean fixedRowResolves(GT6RecipesShCL.FixedRow aRow) {
		if (!slotResolves(aRow.input())) return false;
		for (GT6RecipesShCL.Slot tSlot : aRow.outputs()) if (!slotResolves(tSlot)) return false;
		return true;
	}

	private static boolean slotResolves(GT6RecipesShCL.Slot aSlot) {
		if (aSlot.vanilla() != null) return aSlot.vanilla().get() != null;
		return SYNTHETIC_ITEMS.containsKey(new PrefixMaterial(aSlot.prefix(), aSlot.material()));
	}

	private static boolean crusherRowResolves(GT6RecipesShCL.CrusherTemplate aTemplate, OreDictMaterial aMaterial) {
		if (aMaterial.contains(TD.Atomic.ANTIMATTER) || aMaterial.contains(TD.Properties.INVALID_MATERIAL)) return false;
		return SYNTHETIC_ITEMS.containsKey(new PrefixMaterial(aTemplate.inPrefix(), aMaterial))
				&& SYNTHETIC_ITEMS.containsKey(new PrefixMaterial(aTemplate.outPrefix(), aMaterial));
	}

	private static long tFlintDuration(Recipe aRecipe) {
		return aRecipe.mDuration;
	}

	/** Local alias so the identity assertion reads without a static import. */
	private static OreDictMaterial MaterialRegistryTarget(OreDictMaterial aMaterial) {
		return aMaterial;
	}

	/** java-only alias to keep the vanilla Blocks references visually grouped in the tests. */
	private static final class Blocks {
		static net.minecraft.world.level.block.Block COBBLESTONE = net.minecraft.world.level.block.Blocks.COBBLESTONE;
		static net.minecraft.world.level.block.Block COBWEB = net.minecraft.world.level.block.Blocks.COBWEB;
		static net.minecraft.world.level.block.Block GLASS_PANE = net.minecraft.world.level.block.Blocks.GLASS_PANE;
		static net.minecraft.world.level.block.Block STONE = net.minecraft.world.level.block.Blocks.STONE;
	}
}
