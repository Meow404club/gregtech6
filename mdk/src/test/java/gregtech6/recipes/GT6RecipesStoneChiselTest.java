package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;

import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GTStoneBlocks;

/**
 * The Chisel book pour (task p19-chisel-recipes): the three upstream sources in upstream
 * order — the RM.java:470 stonetypes line (17 STONE→CHISL rows), the RM.java:507-518
 * bricks line (17 :508-shape BRICK→CRACK rows, the :514 else-branch zero-hit) and the
 * Loader_Recipes_Vanilla.java:772-773 vanilla pair — plus the variant-tag carrier
 * (withVariant/variantOf roundtrip), the exact-tag findRecipe semantics the
 * GTChiselItem gate consumes, the SKIPPED_UPSTREAM audit pins and the ADR-P18 bare-reset
 * canary.
 *
 * <p>Offline universe (the GT6RecipesShCL convention): the GT stone legs resolve through
 * the injectable {@code sStoneItemResolver} onto ONE existing vanilla item (new Items
 * cannot be created offline); the input/output legs of a stone row share that item and
 * differ ONLY in the variant tag — exactly the domain separation the live gate rides.
 * The item is kept distinct from every vanilla-pair item so a lookup can only match its
 * own source's rows.
 */
class GT6RecipesStoneChiselTest extends GTRecipesOfflineTestBase {

	/** The existing item standing in for every GT stone BlockItem (distinct from the vanilla pair's four). */
	private static final Item SYNTHETIC_STONE_ITEM = Blocks.BRICKS.asItem();

	private static Function<String, Item> sDefaultResolver;

	@BeforeAll
	static void captureDefault() {
		sDefaultResolver = GT6RecipesStoneChisel.sStoneItemResolver;
	}

	@AfterEach
	void restoreSeam() {
		GT6RecipesStoneChisel.sStoneItemResolver = sDefaultResolver;
		GT6RecipeMaps.reset();
		GT6RecipesStoneChisel.resetForTest();
	}

	private static void injectSyntheticResolver() {
		GT6RecipesStoneChisel.sStoneItemResolver = aSnake -> SYNTHETIC_STONE_ITEM;
	}

	// ------------------------------------------------------------------
	// the map declaration (RM.java:138)
	// ------------------------------------------------------------------

	/** The RM.java:138 Chisel map constants: items 1/1/1, fluids 0/0/0, MIN 0, AMP 1. */
	@Test
	void chiselMapDeclaredWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.CHISEL);
		assertEquals("gt.recipe.chisel", GT6RecipeMaps.CHISEL.mNameInternal);
		assertSame(GT6RecipeMaps.CHISEL, RecipeMap.RECIPE_MAPS.get("gt.recipe.chisel"));
		assertEquals("Chisel", GT6RecipeMaps.CHISEL.mNameLocal);
		assertEquals("gt.recipe.chisel", GT6RecipeMaps.CHISEL.mNameNEI, "RM.java:138 passes null → the internal name");
		assertEquals(1, GT6RecipeMaps.CHISEL.mInputItemsCount);
		assertEquals(1, GT6RecipeMaps.CHISEL.mOutputItemsCount);
		assertEquals(1, GT6RecipeMaps.CHISEL.mMinimalInputItems);
		assertEquals(0, GT6RecipeMaps.CHISEL.mInputFluidCount);
		assertEquals(0, GT6RecipeMaps.CHISEL.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.CHISEL.mMinimalInputFluids);
		assertEquals(0, GT6RecipeMaps.CHISEL.mMinimalInputs);
		assertEquals(1, GT6RecipeMaps.CHISEL.mPower);
		assertEquals("gt6:textures/gui/machines/chisel.png", GT6RecipeMaps.CHISEL.mGUIPath,
				"the RM.java:138 machines/Chisel row, lowercased (the 1.20.1 ResourceLocation charset convention)");
		assertTrue(GT6RecipeMaps.CHISEL.getClass() == RecipeMap.class,
				"the base RecipeMap — the RecipeMapChisel oredict ring-composition synthesizer is the OM runtime pool (SKIPPED_UPSTREAM)");
	}

	// ------------------------------------------------------------------
	// the transcription walks
	// ------------------------------------------------------------------

	/** The stonetypes line: 17 rows, one per GT stone, STONE → CHISL, eUt 16 duration 16 (RM.java:470). */
	@Test
	void stoneTypesTableCensusAndRowShape() {
		var tRows = GT6RecipesStoneChisel.stoneTypesTable();
		assertEquals(17, tRows.size(), "one row per CS.java:1668 stone");
		for (int i = 0; i < tRows.size(); i++) {
			GT6RecipesStoneChisel.ChiselRow tRow = tRows.get(i);
			assertEquals(GTStoneBlocks.STONES.get(i).snake(), tRow.input().stoneSnake(), "declaration order");
			assertTrue(tRow.note().startsWith("RM.java:470 "), "the upstream line anchor: " + tRow.note());
			assertEquals(StoneVariant.STONE, tRow.input().variant());
			assertEquals(StoneVariant.CHISL, tRow.output().variant(), "the stoneChiseled carrier is CHISL (OP.stoneChiseled)");
			Recipe tRecipe = GT6RecipesStoneChisel.buildRecipe(tRow);
			assertNotNull(tRecipe);
			assertEquals(16, tRecipe.mEUt, "RM.java:470 eUt 16");
			assertEquals(16, tRecipe.mDuration, "RM.java:470 duration 16");
			assertEquals(1, tRecipe.mInputs.length);
			assertEquals(1, tRecipe.mOutputs.length);
			assertTrue(tRecipe.mInputs[0].getItem() == tRecipe.mOutputs[0].getItem(),
					"one family block, both legs — the variant tag separates them");
			assertEquals(StoneVariant.STONE, GT6RecipesStoneChisel.variantOf(tRecipe.mInputs[0]));
			assertEquals(StoneVariant.CHISL, GT6RecipesStoneChisel.variantOf(tRecipe.mOutputs[0]));
		}
	}

	/** The bricks line: 17 rows, all the :508 shape BRICK → CRACK; the :514 cobble else-branch is zero-hit. */
	@Test
	void bricksTablePoursTheCrackedBranch() {
		var tRows = GT6RecipesStoneChisel.bricksTable();
		assertEquals(17, tRows.size(), "one row per stone");
		for (int i = 0; i < tRows.size(); i++) {
			GT6RecipesStoneChisel.ChiselRow tRow = tRows.get(i);
			assertEquals("RM.java:508 " + GTStoneBlocks.STONES.get(i).snake(), tRow.note(),
					"every stone carries a cracked variant (the full 16-variant family), so the :507 gate is TRUE everywhere and :514 never fires");
			assertEquals(StoneVariant.BRICK, tRow.input().variant());
			assertEquals(StoneVariant.CRACK, tRow.output().variant());
			Recipe tRecipe = GT6RecipesStoneChisel.buildRecipe(tRow);
			assertNotNull(tRecipe);
			assertEquals(16, tRecipe.mEUt, "RM.java:508 eUt 16");
			assertEquals(16, tRecipe.mDuration, "RM.java:508 duration 16");
		}
	}

	/** The vanilla pair verbatim (Loader_Recipes_Vanilla.java:772-773): stone→chiseled, stone bricks→cracked. */
	@Test
	void vanillaPairRowsVerbatim() {
		var tRows = GT6RecipesStoneChisel.vanillaTable();
		assertEquals(2, tRows.size());
		assertEquals(Blocks.STONE.asItem(), tRows.get(0).input().vanilla().get());
		assertEquals(Blocks.CHISELED_STONE_BRICKS.asItem(), tRows.get(0).output().vanilla().get(),
				":772 stone → stonebrick:3 = chiseled stone bricks (the 1.20.1 flat identity)");
		assertEquals(Blocks.STONE_BRICKS.asItem(), tRows.get(1).input().vanilla().get());
		assertEquals(Blocks.CRACKED_STONE_BRICKS.asItem(), tRows.get(1).output().vanilla().get(),
				":773 stonebrick:0 → stonebrick:2 = cracked stone bricks (the 1.20.1 flat identity)");
		for (GT6RecipesStoneChisel.ChiselRow tRow : tRows) {
			Recipe tRecipe = GT6RecipesStoneChisel.buildRecipe(tRow);
			assertNotNull(tRecipe);
			assertEquals(16, tRecipe.mEUt, "eUt 16");
			assertEquals(16, tRecipe.mDuration, "duration 16");
			assertNull(GT6RecipesStoneChisel.variantOf(tRecipe.mInputs[0]), "the vanilla legs carry no variant tag");
			assertNull(GT6RecipesStoneChisel.variantOf(tRecipe.mOutputs[0]));
		}
	}

	// ------------------------------------------------------------------
	// the pour census
	// ------------------------------------------------------------------

	/** 17 (stonetypes) + 17 (bricks :508 shape) + 2 (vanilla) = 36 rows pour into the CHISEL map. */
	@Test
	void pourCensusIsSeventeenPlusSeventeenPlusTwo() {
		injectSyntheticResolver();
		GT6RecipesStoneChisel.load();
		assertNotNull(GT6RecipeMaps.CHISEL);
		assertEquals(36, GT6RecipeMaps.CHISEL.mRecipeList.size(),
				"17 stonetypes (RM.java:470) + 17 bricks (:508 shape) + 2 vanilla (:772-773)");
	}

	/** load() is idempotent within a generation (the second call must not double-pour). */
	@Test
	void loadIsIdempotent() {
		injectSyntheticResolver();
		GT6RecipesStoneChisel.load();
		int tPoured = GT6RecipeMaps.CHISEL.mRecipeList.size();
		GT6RecipesStoneChisel.load();
		assertEquals(tPoured, GT6RecipeMaps.CHISEL.mRecipeList.size(), "one generation, one pour");
	}

	/** A dead stone resolver drops every stone row; the all-vanilla pair survives. */
	@Test
	void unresolvableStoneRowsSkipButVanillaRowsSurvive() {
		GT6RecipesStoneChisel.sStoneItemResolver = aSnake -> null;
		GT6RecipesStoneChisel.load();
		assertEquals(2, GT6RecipeMaps.CHISEL.mRecipeList.size(), "the Loader_Recipes_Vanilla.java:772-773 pair");
	}

	/** The ADR-P18 canary: bare reset() → load() must re-pour the 36 rows (the pour-flag retires WITH the generation). */
	@Test
	void loadRepoursAfterABareMapReset() {
		injectSyntheticResolver();
		GT6RecipeMaps.reset();
		GT6RecipesStoneChisel.resetForTest(); // self-grounding: boot state and prior classes never feed this test
		GT6RecipesStoneChisel.load();
		assertEquals(36, GT6RecipeMaps.CHISEL.mRecipeList.size());

		GT6RecipeMaps.reset(); // the BARE reset — no resetForTest: the shared-static poison constructor
		assertNull(GT6RecipeMaps.CHISEL, "the bare reset dropped the map generation");
		GT6RecipesStoneChisel.load();
		assertEquals(36, GT6RecipeMaps.CHISEL.mRecipeList.size(),
				"load() must truly re-pour after a bare reset() — the pour-flag must retire WITH the generation (ADR-P18)");
	}

	// ------------------------------------------------------------------
	// the variant-tag carrier + the exact-tag lookup the gate rides
	// ------------------------------------------------------------------

	/** The variant tag roundtrip, the unknown-name tolerance and the byName decode. */
	@Test
	void variantTagRoundtripAndTolerance() {
		ItemStack tStack = new ItemStack(SYNTHETIC_STONE_ITEM, 1);
		assertNull(GT6RecipesStoneChisel.variantOf(tStack), "an untagged stack decodes to no variant");
		GT6RecipesStoneChisel.withVariant(tStack, StoneVariant.CHISL);
		assertEquals(StoneVariant.CHISL, GT6RecipesStoneChisel.variantOf(tStack));
		assertEquals(StoneVariant.STONE, GT6RecipesStoneChisel.variantByName("stone"));
		assertNull(GT6RecipesStoneChisel.variantByName("nonsense"), "an unknown name decodes to no variant");
		assertNull(GT6RecipesStoneChisel.variantByName(null));
	}

	/**
	 * The findRecipe semantics the gate rides: the probe matches its own variant-tagged row
	 * EXACTLY — a differently-varianted probe and an untagged probe both miss (the recipe
	 * input carries a tag, so Recipe.checkStacksEqual compares tags too).
	 */
	@Test
	void findRecipeMatchesTheExactVariantTagOnly() {
		injectSyntheticResolver();
		GT6RecipesStoneChisel.load();

		ItemStack tStoneProbe = new ItemStack(SYNTHETIC_STONE_ITEM, 1);
		GT6RecipesStoneChisel.withVariant(tStoneProbe, StoneVariant.STONE);
		Recipe tHit = GT6RecipeMaps.CHISEL.findRecipe(null, Long.MAX_VALUE, null, new FluidStack[0], tStoneProbe);
		assertNotNull(tHit, "the :470 STONE-tag row matches the STONE-tag probe");
		assertEquals(StoneVariant.CHISL, GT6RecipesStoneChisel.variantOf(tHit.mOutputs[0]));

		ItemStack tChislProbe = new ItemStack(SYNTHETIC_STONE_ITEM, 1);
		GT6RecipesStoneChisel.withVariant(tChislProbe, StoneVariant.CHISL);
		assertNull(GT6RecipeMaps.CHISEL.findRecipe(null, Long.MAX_VALUE, null, new FluidStack[0], tChislProbe),
				"the CHISL probe misses the STONE-tag row (no self-mapping row exists — the upstream CHISEL_MAPPINGS self-map is the TE face, pooled)");

		ItemStack tPlainProbe = new ItemStack(SYNTHETIC_STONE_ITEM, 1);
		assertNull(GT6RecipeMaps.CHISEL.findRecipe(null, Long.MAX_VALUE, null, new FluidStack[0], tPlainProbe),
				"an untagged probe misses the tagged row — a vanilla-item lookup can only match its own untagged row");

		ItemStack tBrickProbe = new ItemStack(SYNTHETIC_STONE_ITEM, 1);
		GT6RecipesStoneChisel.withVariant(tBrickProbe, StoneVariant.BRICK);
		Recipe tBrickHit = GT6RecipeMaps.CHISEL.findRecipe(null, Long.MAX_VALUE, null, new FluidStack[0], tBrickProbe);
		assertNotNull(tBrickHit, "the :508 BRICK-tag row matches the BRICK-tag probe");
		assertEquals(StoneVariant.CRACK, GT6RecipesStoneChisel.variantOf(tBrickHit.mOutputs[0]));
	}

	// ------------------------------------------------------------------
	// the audit pins
	// ------------------------------------------------------------------

	/** The SKIPPED_UPSTREAM audit census: the CR.shaped hand rows, the sibling tool rows, the TE faces, the OM synthesizer. */
	@Test
	void skippedUpstreamAuditPins() {
		var tSkipped = GT6RecipesStoneChisel.SKIPPED_UPSTREAM;
		assertEquals(12, tSkipped.size(), "the dead-row audit pins: the full pool census");
		String tAll = String.join("\n", tSkipped);
		assertTrue(tAll.contains("RM.java:469"), "the TE Mana Bath row (FL.Mana_TE gate)");
		assertTrue(tAll.contains("RM.java:471"), "the CR.shaped chiseled hand row ('y' key) — the crafting bridge pool");
		assertTrue(tAll.contains("RM.java:509-510") && tAll.contains("RM.java:515-516"), "the Hammer/Crusher sibling rows — the tool-family pool");
		assertTrue(tAll.contains("RM.java:511-512") && tAll.contains("RM.java:517-518"), "the CR.shaped cracked/cobble hand rows — the crafting bridge pool");
		assertTrue(tAll.contains("RecipeMapChisel.java:47-64"), "the oredict ring-composition synthesizer — the OM runtime pool");
		assertTrue(tAll.contains("BlockStones.java:573-576"), "the BlockStones TE-face chisel arm (CHISEL_MAPPINGS direct-meta, 1250/octant) — the TE-face pool");
		assertTrue(tAll.contains("GT_Tool_Chisel.java:57-79") && tAll.contains("HarvestDropsEvent"), "the mining-drop conversion — 1.20.1 has no HarvestDropsEvent");
		assertTrue(tAll.contains("BlockStones.java:495-498"), "the LaserEngraver white-lens rows");
		assertTrue(tAll.contains("BlockStones.java:405-411"), "the CHISL equal-set machine family — the tool/smelting pools");
		assertTrue(tAll.contains("BlockStones.java:419-424"), "the SMOTH equal-set CR.shaped hand rows — the crafting bridge pool");
		assertTrue(tSkipped.stream().noneMatch(a -> a.startsWith("RM.java:470")),
				"the poured :470 line is not a skip");
	}
}
