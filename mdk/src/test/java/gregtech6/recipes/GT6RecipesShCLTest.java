package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
			Items.COBBLESTONE, Items.STONE, Items.GLASS_PANE,
			// p10-compat-vanilla-rows vanilla identities (row inputs and the all-vanilla outputs)
			Items.BONE, Items.BONE_MEAL, Items.NETHER_BRICK,
			Blocks.NETHER_BRICKS.asItem(), Blocks.NETHERRACK.asItem(), Blocks.END_STONE.asItem(), Blocks.OBSIDIAN.asItem());

	@BeforeAll
	static void buildSyntheticUniverse() {
		GTMaterialItems.initMaterials(); // the offline material universe (MT.init + OP.init)
		// VANILLA-NAMESPACE ITEMS ONLY (task p25-tool-hammer-wrench stabilization): the
		// wrap-around aliasing (tNext % tPool.size()) makes every pair assignment sensitive
		// to the POOL SIZE — a probe-registering test class (the FileSawTest reflection
		// bracket, p24; the HammerWrenchTest pair, p25) shifts the size and silently
		// re-aliases the whole universe. Filtering to the minecraft namespace pins the pool
		// to the frozen vanilla item set, so any number of registered test probes is inert.
		List<Item> tPool = BuiltInRegistries.ITEM.stream()
				.filter(t -> "minecraft".equals(BuiltInRegistries.ITEM.getKey(t).getNamespace()))
				.filter(t -> new ItemStack(t, 1).getMaxStackSize() == 64) // 64-stack only: new ItemStack(item, N>max) silently clamps and breaks the parallel/count math (the ItemStack no-arg form is leg-agnostic; Item.getMaxStackSize takes a stack on 21.1)
				.toList();
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

	/** The Shredder table: 8 rows, the exact upstream shapes (Vanilla:688-693/:697/:707-708). */
	@Test
	void shredderRowShapes() {
		List<GT6RecipesShCL.FixedRow> tTable = GT6RecipesShCL.shredderTable();
		assertEquals(8, tTable.size(), "5 vanilla rows + the :697 bone backfill + 2 Blaze rows (the group expansion is pooled)");

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

		// :697 — the p10 backfill: bone → 4 bonemeal (upstream IL.Dye_Bonemeal.get(4) = the
		// 1.7.10 white-dye meta, whose 1.20.1 identity is Items.BONE_MEAL); deterministic, 32 t
		GT6RecipesShCL.FixedRow tBone = findFixedRow(tTable, ":697");
		assertNotNull(tBone);
		assertEquals(Items.BONE, tBone.input().vanilla().get());
		assertEquals(1, tBone.input().count());
		assertEquals(16, tBone.eUt());
		assertEquals(32, tBone.duration());
		assertNull(tBone.chances(), "the bone row is deterministic (upstream addRecipe1 has no chances literal)");
		assertEquals(1, tBone.outputs().length);
		assertEquals(Items.BONE_MEAL, tBone.outputs()[0].vanilla().get());
		assertEquals(4, tBone.outputs()[0].count());

		// every row runs at eUt 16 (the addRecipe1 second arg)
		for (GT6RecipesShCL.FixedRow tRow : tTable) assertEquals(16, tRow.eUt(), "row " + tRow.note());
	}

	/** The p10 Crusher vanilla rows (OreDict:82/:88/:92/:96): inputs, outputs, and the chance literals. */
	@Test
	void crusherVanillaRowShapes() {
		List<GT6RecipesShCL.FixedRow> tTable = GT6RecipesShCL.crusherVanillaTable();
		assertEquals(4, tTable.size(), "obsidian + netherbrick + netherrack + endstone (upstream file order)");

		// :82 — obsidian: dust x8 @10000 + dust x1 @2500, duration 600; the input is the vanilla
		// obsidian block (the port block universe generates no blockSolid Obsidian — deviation declared)
		GT6RecipesShCL.FixedRow tObsidian = findFixedRow(tTable, ":82");
		assertNotNull(tObsidian);
		assertEquals(Blocks.OBSIDIAN.asItem(), tObsidian.input().vanilla().get(), "the :82 input is the vanilla obsidian block (declared deviation)");
		assertEquals(16, tObsidian.eUt());
		assertEquals(600, tObsidian.duration());
		assertArrayEquals(new long[] {10000, 2500}, tObsidian.chances(), "the upstream new long[] {10000, 2500} literal");
		assertEquals(2, tObsidian.outputs().length);
		assertSame(OP.dust, tObsidian.outputs()[0].prefix());
		assertSame(MT.Obsidian, tObsidian.outputs()[0].material());
		assertEquals(8, tObsidian.outputs()[0].count(), "the dust tail of the IL.RC/HBM fallback chain = x8");
		assertSame(OP.dust, tObsidian.outputs()[1].prefix());
		assertEquals(1, tObsidian.outputs()[1].count(), "the 2500-chance second slot");

		// :88 — nether bricks: four independent single-brick slots at descending certainty
		GT6RecipesShCL.FixedRow tBricks = findFixedRow(tTable, ":88");
		assertNotNull(tBricks);
		assertEquals(Blocks.NETHER_BRICKS.asItem(), tBricks.input().vanilla().get());
		assertEquals(16, tBricks.eUt());
		assertEquals(16, tBricks.duration());
		assertArrayEquals(new long[] {10000, 9000, 8000, 7000}, tBricks.chances(), "the upstream four-tier chance literal");
		assertEquals(4, tBricks.outputs().length);
		for (GT6RecipesShCL.Slot tSlot : tBricks.outputs()) {
			assertEquals(Items.NETHER_BRICK, tSlot.vanilla().get());
			assertEquals(1, tSlot.count());
		}

		// :92/:96 — the rockGt rows are deterministic (no chances literal upstream)
		GT6RecipesShCL.FixedRow tNetherrack = findFixedRow(tTable, ":92");
		assertNotNull(tNetherrack);
		assertEquals(Blocks.NETHERRACK.asItem(), tNetherrack.input().vanilla().get());
		assertNull(tNetherrack.chances());
		assertEquals(16, tNetherrack.duration());
		assertEquals(1, tNetherrack.outputs().length);
		assertSame(OP.rockGt, tNetherrack.outputs()[0].prefix());
		assertSame(MT.Netherrack, tNetherrack.outputs()[0].material());
		assertEquals(4, tNetherrack.outputs()[0].count());

		GT6RecipesShCL.FixedRow tEndstone = findFixedRow(tTable, ":96");
		assertNotNull(tEndstone);
		assertEquals(Blocks.END_STONE.asItem(), tEndstone.input().vanilla().get());
		assertNull(tEndstone.chances());
		assertEquals(1, tEndstone.outputs().length);
		assertSame(OP.rockGt, tEndstone.outputs()[0].prefix());
		assertSame(MT.Endstone, tEndstone.outputs()[0].material());
		assertEquals(4, tEndstone.outputs()[0].count());

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
		assertEquals(7, tTable.size(), "the gem chain :69-73 + the boule row :75 + the :65 rawOreChunk row");

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
		// p26: the reconciliation now mirrors the FULL pour order (fixed rows → shred templates →
		// RECYCLABLE ring) including the exact-row dedup, via the package-private pour primitives.
		Set<String> tShredderKeys = new HashSet<>();
		for (GT6RecipesShCL.FixedRow tRow : GT6RecipesShCL.shredderTable()) {
			Recipe tRecipe = fixedRowResolves(tRow) ? GT6RecipesShCL.buildFixedRecipe(tRow) : null;
			if (tRecipe != null && tShredderKeys.add(GT6RecipesShCL.rowKey(tRecipe))) tExpectedShredder++;
		}
		for (GT6RecipesShCL.ShredTemplate tTpl : GT6RecipesShCL.shredTemplateTable()) {
			for (OreDictMaterial tMaterial : GT6RecipesShCL.expandCrusherMaterials(tTpl.inPrefix())) {
				Recipe tRecipe = GT6RecipesShCL.buildShredRecipe(tTpl, tMaterial);
				if (tRecipe != null && tShredderKeys.add(GT6RecipesShCL.rowKey(tRecipe))) tExpectedShredder++;
			}
		}
		for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
			if (!tPrefix.mByProducts.isEmpty() || GT6RecipesShCL.UPSTREAM_BYPRODUCT_PREFIXES.contains(tPrefix.mNameInternal)) continue;
			if (!tPrefix.contains(TD.Prefix.RECYCLABLE)) continue;
			if (tPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED, TD.Prefix.DUST_BASED, TD.Prefix.IS_CONTAINER)) continue;
			if (tPrefix.mNameInternal.startsWith("cableGt") || tPrefix.mNameInternal.startsWith("wireGt") || tPrefix.mNameInternal.startsWith("pipe")) continue;
			for (OreDictMaterial tMaterial : GT6RecipesShCL.expandCrusherMaterials(tPrefix)) {
				for (int tArm = 0; tArm < 2; tArm++) {
					Recipe tRecipe = GT6RecipesShCL.buildRingRecipe(tPrefix, tMaterial, tArm == 0 ? 256 : 16, tArm == 0);
					if (tRecipe != null && tShredderKeys.add(GT6RecipesShCL.rowKey(tRecipe))) tExpectedShredder++;
				}
			}
		}
		int tExpectedLathe = 0;
		Set<String> tLatheKeys = new HashSet<>();
		for (GT6RecipesShCL.FixedRow tRow : GT6RecipesShCL.latheTable()) {
			Recipe tRecipe = fixedRowResolves(tRow) ? GT6RecipesShCL.buildFixedRecipe(tRow) : null;
			if (tRecipe != null && tLatheKeys.add(GT6RecipesShCL.rowKey(tRecipe))) tExpectedLathe++;
		}
		// p26-rm-row-backfill: the handler-template expansions join the reconciliation
		for (GT6RecipesShCL.LatheTemplate tTpl : GT6RecipesShCL.latheTemplateTable()) {
			for (OreDictMaterial tMaterial : GT6RecipesShCL.expandCrusherMaterials(tTpl.inPrefix())) {
				Recipe tRecipe = latheRowResolves(tTpl, tMaterial) ? GT6RecipesShCL.buildLatheRecipe(tTpl, tMaterial) : null;
				if (tRecipe != null && tLatheKeys.add(GT6RecipesShCL.rowKey(tRecipe))) tExpectedLathe++;
			}
		}
		int tExpectedCrusher = 0;
		for (GT6RecipesShCL.CrusherTemplate tTpl : GT6RecipesShCL.crusherTable()) {
			for (OreDictMaterial tMaterial : GT6RecipesShCL.expandCrusherMaterials(tTpl.inPrefix())) {
				if (crusherRowResolves(tTpl, tMaterial)) tExpectedCrusher++;
			}
		}
		int tExpectedCrusherVanilla = 0;
		for (GT6RecipesShCL.FixedRow tRow : GT6RecipesShCL.crusherVanillaTable()) if (fixedRowResolves(tRow)) tExpectedCrusherVanilla++;

		assertEquals(tExpectedShredder, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "Shredder poured = transcribed - skipped");
		assertEquals(tExpectedLathe, GT6RecipeMaps.LATHE.mRecipeList.size(), "Lathe poured = transcribed - skipped");
		assertEquals(tExpectedCrusher + tExpectedCrusherVanilla, GT6RecipeMaps.CRUSHER.mRecipeList.size(),
				"Crusher poured = expanded templates + vanilla rows - skipped");
		// all four p10 vanilla rows must resolve inside the offline synthetic universe
		// ((rockGt, Netherrack/Endstone) and (dust, Obsidian) are in the registration walk)
		assertEquals(4, tExpectedCrusherVanilla, "the p10 Crusher vanilla rows all resolve");
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

		// Shredder: :689 gravel→sand, :690 web→string and :697 bone→bonemeal are all-vanilla; the other five rows touch material items
		assertEquals(3, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "the three all-vanilla Shredder rows pour");
		// Lathe: both rows produce material items → all skip
		assertEquals(0, GT6RecipeMaps.LATHE.mRecipeList.size());
		// Crusher: :88 netherbrick→bricks is all-vanilla; the gem expansion + :82/:92/:96 touch material items
		assertEquals(1, GT6RecipeMaps.CRUSHER.mRecipeList.size(), "the all-vanilla netherbrick row pours");
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
	// p10-compat-vanilla-rows: end-to-end lookups + the chances passthrough proof
	// ------------------------------------------------------------------

	/**
	 * THE offline live proof of the four-tier chance literal (task acceptance): the :88 row's
	 * {@code new long[] {10000, 9000, 8000, 7000}} survives the pour into {@link Recipe#mChances}
	 * and each tier behaves per the {@code getOutputs(Random, int)} threshold semantics.
	 */
	@Test
	void crusherNetherBrickRowPassesTheFourTierChancesThrough() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;
		GT6RecipesShCL.load();

		ItemStack[] tInputs = {new ItemStack(Blocks.NETHER_BRICKS.asItem(), 1)};
		Recipe tFound = GT6RecipeMaps.CRUSHER.findRecipe(null, 16, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound, "the :88 netherbrick row must be found");
		assertEquals(16, tFound.mDuration);
		assertEquals(16, tFound.mEUt);
		assertArrayEquals(new long[] {10000, 9000, 8000, 7000}, tFound.mChances,
				"the upstream four-tier literal must pass through the pour untouched");

		// an always-succeeding RNG → every tier yields its single brick; the 10000 slot takes the
		// deterministic whole-stack branch (it never even samples)
		ItemStack[] tAll = tFound.getOutputs(new AlwaysYesRandom(), 1);
		for (int i = 0; i < 4; i++) {
			assertNotNull(tAll[i], "slot " + i + " must yield under an always-succeeding RNG");
			assertEquals(Items.NETHER_BRICK, tAll[i].getItem());
			assertEquals(1, tAll[i].getCount());
		}
		// an always-failing RNG (9999) → only the 10000 slot survives, the three sub-10000 tiers stay empty
		ItemStack[] tTop = tFound.getOutputs(new AlwaysNoRandom(), 1);
		assertNotNull(tTop[0], "the 10000 slot is the deterministic whole-stack branch");
		assertEquals(Items.NETHER_BRICK, tTop[0].getItem());
		assertEquals(1, tTop[0].getCount());
		for (int i = 1; i < 4; i++) assertNull(tTop[i], "sub-10000 tier " + i + " must yield nothing under an always-failing RNG");
	}

	/** The :82 obsidian row: duration 600, the {10000, 2500} literal, and both slots' chance semantics. */
	@Test
	void crusherObsidianRowChancesEndToEnd() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;
		GT6RecipesShCL.load();

		Item tDustObsidian = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.dust, MT.Obsidian));
		ItemStack[] tInputs = {new ItemStack(Blocks.OBSIDIAN.asItem(), 1)};
		Recipe tFound = GT6RecipeMaps.CRUSHER.findRecipe(null, 16, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound, "the :82 obsidian row must be found");
		assertEquals(600, tFound.mDuration);
		assertArrayEquals(new long[] {10000, 2500}, tFound.mChances);

		// always-failing RNG: the main 8-dust output is full certainty, the 25% bonus stays empty
		ItemStack[] tMain = tFound.getOutputs(new AlwaysNoRandom(), 1);
		assertEquals(tDustObsidian, tMain[0].getItem());
		assertEquals(8, tMain[0].getCount(), "the dust tail of the IL.RC/HBM fallback chain");
		assertNull(tMain[1], "9999 !< 2500 — the bonus slot must stay empty");
		// always-succeeding RNG: the bonus slot adds exactly its 1 dust
		ItemStack[] tBoth = tFound.getOutputs(new AlwaysYesRandom(), 1);
		assertEquals(8, tBoth[0].getCount());
		assertNotNull(tBoth[1]);
		assertEquals(tDustObsidian, tBoth[1].getItem());
		assertEquals(1, tBoth[1].getCount());
	}

	/** The :92/:96 rockGt rows: deterministic x4 outputs through the SHREDDER-sibling lookup path. */
	@Test
	void crusherRockGtRowsEndToEnd() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;
		GT6RecipesShCL.load();

		Item tRockNetherrack = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.rockGt, MT.Netherrack));
		ItemStack[] tInputs = {new ItemStack(Blocks.NETHERRACK.asItem(), 1)};
		Recipe tFound = GT6RecipeMaps.CRUSHER.findRecipe(null, 16, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound, "the :92 netherrack row must be found");
		assertNull(tFound.mChances, "the rockGt rows are deterministic");
		assertEquals(16, tFound.mDuration);
		ItemStack[] tOutputs = tFound.getOutputs(1);
		assertEquals(1, tOutputs.length);
		assertEquals(tRockNetherrack, tOutputs[0].getItem());
		assertEquals(4, tOutputs[0].getCount());

		Item tRockEndstone = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.rockGt, MT.Endstone));
		ItemStack[] tEndInputs = {new ItemStack(Blocks.END_STONE.asItem(), 1)};
		Recipe tEndFound = GT6RecipeMaps.CRUSHER.findRecipe(null, 16, ItemStack.EMPTY, null, tEndInputs);
		assertNotNull(tEndFound, "the :96 endstone row must be found");
		ItemStack[] tEndOutputs = tEndFound.getOutputs(1);
		assertEquals(tRockEndstone, tEndOutputs[0].getItem());
		assertEquals(4, tEndOutputs[0].getCount());
	}

	/** The :697 bone row through the SHREDDER map: find, consume, 4 bonemeal out. */
	@Test
	void shredderBoneRowEndToEnd() {
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;
		GT6RecipesShCL.load();

		ItemStack[] tInputs = {new ItemStack(Items.BONE, 1)};
		Recipe tFound = GT6RecipeMaps.SHREDDER.findRecipe(null, 16, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound, "the :697 bone row must be found");
		assertEquals(32, tFound.mDuration);
		assertNull(tFound.mChances);
		assertTrue(tFound.isRecipeInputEqual(true, false, null, tInputs));
		assertEquals(0, tInputs[0].getCount(), "one pass consumes exactly one bone");

		ItemStack[] tOutputs = tFound.getOutputs(1);
		assertEquals(1, tOutputs.length);
		assertEquals(Items.BONE_MEAL, tOutputs[0].getItem());
		assertEquals(4, tOutputs[0].getCount());
	}

	// ------------------------------------------------------------------
	// p26-rm-row-backfill — the Lathe handler-template expansion (Handlers:371-393)
	// ------------------------------------------------------------------

	/** The :371-393 census: 22 templates, twin arms, and the conjunct flags land on the right rows. */
	@Test
	void latheTemplateCensus() {
		List<GT6RecipesShCL.LatheTemplate> tTable = GT6RecipesShCL.latheTemplateTable();
		assertEquals(22, tTable.size(), "the tEasyWorkable twin arms :371-381 + :383-393");
		for (GT6RecipesShCL.LatheTemplate tTpl : tTable) {
			assertEquals(16, tTpl.eUt(), "row " + tTpl.note() + " eUt");
			assertTrue(tTpl.outCount() > 0, "row " + tTpl.note() + " well-formed");
		}
		// arm A: duration 0 (getCosts) with multiplier 64; arm B: fixed duration with multiplier 0
		GT6RecipesShCL.LatheTemplate tIngotHard = findLatheTemplate(tTable, ":377");
		assertNotNull(tIngotHard);
		assertFalse(tIngotHard.easyArm());
		assertEquals(0, tIngotHard.duration());
		assertEquals(64, tIngotHard.multiplier());
		assertSame(OP.ingot, tIngotHard.inPrefix());
		assertSame(OP.stick, tIngotHard.outPrefix());
		assertTrue(tIngotHard.layeredNot(), ":377 carries LAYERED.NOT");
		GT6RecipesShCL.LatheTemplate tIngotEasy = findLatheTemplate(tTable, ":389");
		assertNotNull(tIngotEasy);
		assertTrue(tIngotEasy.easyArm());
		assertEquals(16, tIngotEasy.duration(), "the :389 fixed literal 16");
		assertEquals(0, tIngotEasy.multiplier());
		// the lens.NOT conjunct rides :374/:386 only
		assertTrue(findLatheTemplate(tTable, ":374").lensNot());
		assertTrue(findLatheTemplate(tTable, ":386").lensNot());
		assertFalse(findLatheTemplate(tTable, ":373").lensNot());
		// the output-count rows
		assertEquals(3, findLatheTemplate(tTable, ":379").outCount(), "bouleGt → stickLong x3");
		assertEquals(3, findLatheTemplate(tTable, ":393").outCount(), "gemFlawed → bolt x3");
	}

	/**
	 * The twin-arm split is the material's FURNACE/SOFT tag pair (tEasyWorkable, Handlers:58):
	 * Iron is NEVER_FURNACE and not SOFT → arm A only; Tin carries SOFT → arm B only.
	 */
	@Test
	void latheArmSplitFollowsTheMaterialTags() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		assertFalse(MT.Iron.contains(TD.Processing.FURNACE) || MT.Iron.contains(TD.Properties.SOFT),
				"iron must be a hard (arm A) material for this test to mean anything");
		assertTrue(MT.Sn.contains(TD.Processing.FURNACE) || MT.Sn.contains(TD.Properties.SOFT),
				"tin must be an easy (arm B) material for this test to mean anything");
		GT6RecipesShCL.LatheTemplate tHard = findLatheTemplate(GT6RecipesShCL.latheTemplateTable(), ":377");
		GT6RecipesShCL.LatheTemplate tEasy = findLatheTemplate(GT6RecipesShCL.latheTemplateTable(), ":389");
		assertNotNull(GT6RecipesShCL.buildLatheRecipe(tHard, MT.Iron), "iron takes the hard-arm ingot→stick row");
		assertNull(GT6RecipesShCL.buildLatheRecipe(tEasy, MT.Iron), "iron never matches the easy arm");
		assertNotNull(GT6RecipesShCL.buildLatheRecipe(tEasy, MT.Sn), "tin takes the easy-arm ingot→stick row");
		assertNull(GT6RecipesShCL.buildLatheRecipe(tHard, MT.Sn), "tin never matches the hard arm");
		// arm A duration = the getCosts arithmetic (mult 64); arm B = the fixed 16
		Recipe tHardRow = GT6RecipesShCL.buildLatheRecipe(tHard, MT.Iron);
		assertEquals(Math.max(1, tHardRowDuration(MT.Iron)), tHardRow.mDuration, "arm A :377 duration = getCosts");
		Recipe tEasyRow = GT6RecipesShCL.buildLatheRecipe(tEasy, MT.Sn);
		assertEquals(16, tEasyRow.mDuration, "arm B :389 duration = the fixed literal");
	}

	/** The getCosts mirror for the :377 row (ingot x1 → stick x1, mult 64). */
	private static long tHardRowDuration(OreDictMaterial aMaterial) {
		long tUnits = Math.max(OP.ingot.mAmount, OP.stick.mAmount);
		long tTarget = 64 + 64 * aMaterial.mToolQuality;
		return tUnits * tTarget / CS.U + (tUnits * tTarget % CS.U > 0 ? 1 : 0);
	}

	/**
	 * The mOutputPulverizedRemains transcription follows the unit surplus (the ctor :79 gate +
	 * :215 append): the unit-flat nugget→round twin stays single-output (U9 − U9 = 0), the
	 * bolt→screw row sits EXACTLY on the gate boundary (U8 − U9 = U72 → 1x dustDiv72 — the
	 * gate is >=, not >), and the ingot→stick surplus U/2 → 2x dustSmall.
	 */
	@Test
	void latheRemainsFollowTheUnitSurplus() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		Item tDiv72Iron = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.dustDiv72, MT.Iron.mTargetPulver.mMaterial));
		Item tDustSmallIron = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.dustSmall, MT.Iron.mTargetPulver.mMaterial));
		assertNotNull(tDiv72Iron);
		assertNotNull(tDustSmallIron);
		Recipe tFlat = GT6RecipesShCL.buildLatheRecipe(findLatheTemplate(GT6RecipesShCL.latheTemplateTable(), ":372"), MT.Iron);
		assertNotNull(tFlat, "iron must take the :372 hard-arm nugget→round row");
		assertEquals(1, tFlat.mOutputs.length, "U9 − U9 = 0: the nugget→round row stays single-output");
		Recipe tBoundary = GT6RecipesShCL.buildLatheRecipe(findLatheTemplate(GT6RecipesShCL.latheTemplateTable(), ":371"), MT.Iron);
		assertNotNull(tBoundary, "iron must take the :371 hard-arm bolt→screw row");
		assertEquals(2, tBoundary.mOutputs.length, "U8 − U9 = U72 exactly: the >= gate opens, the remains rides");
		assertEquals(tDiv72Iron, tBoundary.mOutputs[1].getItem());
		assertEquals(1, tBoundary.mOutputs[1].getCount());
		Recipe tIngotRow = GT6RecipesShCL.buildLatheRecipe(findLatheTemplate(GT6RecipesShCL.latheTemplateTable(), ":377"), MT.Iron);
		assertNotNull(tIngotRow);
		assertEquals(2, tIngotRow.mOutputs.length, "U − U/2 = U/2 surplus: [stick, 2x dustSmall]");
		assertEquals(tDustSmallIron, tIngotRow.mOutputs[1].getItem());
		assertEquals(2, tIngotRow.mOutputs[1].getCount());
	}

	/**
	 * The (ingot, Iron) → (stick, Iron) row poured with the getCosts shape and consumes. The
	 * offline synthetic universe ALIASES pairs onto shared vanilla items, so the row is located
	 * by identity in the map instead of an ambiguous findRecipe probe (the reserved-vanilla
	 * findRecipe path is the flint/web/bone tests).
	 */
	@Test
	void latheIngotRowRoundTripsThroughFindRecipe() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		GT6RecipesShCL.sVanillaItemResolver = Supplier::get;
		GT6RecipesShCL.load();

		Item tIngotIron = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.ingot, MT.Iron));
		Item tStickIron = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.stick, MT.Iron));
		assertNotNull(tIngotIron);
		assertNotNull(tStickIron, "iron must generate sticks for this round-trip");
		// the :377 row carries the mOutputPulverizedRemains transcription (ctor :79 + :215):
		// ingot U − stick U/2 = U/2 surplus → OM.pulverize = 2x dustSmall of the pulver target
		Item tDustSmallIron = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.dustSmall, MT.Iron.mTargetPulver.mMaterial));
		assertNotNull(tDustSmallIron, "iron's pulver target must generate small dusts for this round-trip");
		Recipe tIronRow = null;
		for (Recipe tRecipe : GT6RecipeMaps.LATHE.mRecipeList) {
			if (tRecipe.mInputs.length == 1 && tRecipe.mInputs[0].getItem() == tIngotIron && tRecipe.mInputs[0].getCount() == 1
					&& tRecipe.mOutputs.length == 2 && tRecipe.mOutputs[0].getItem() == tStickIron && tRecipe.mOutputs[0].getCount() == 1
					&& tRecipe.mOutputs[1].getItem() == tDustSmallIron && tRecipe.mOutputs[1].getCount() == 2) {
				tIronRow = tRecipe;
				break;
			}
		}
		assertNotNull(tIronRow, "the :377 iron ingot→stick+remains row must be poured");
		assertEquals(Math.max(1, tHardRowDuration(MT.Iron)), tIronRow.mDuration, "arm A duration = the getCosts arithmetic");
		assertEquals(16, tIronRow.mEUt);
		ItemStack[] tInputs = {new ItemStack(tIngotIron, 2)};
		assertTrue(tIronRow.isRecipeInputEqual(false, false, null, tInputs));
		assertEquals(2, tInputs[0].getCount(), "the probe must not consume");
		assertTrue(tIronRow.isRecipeInputEqual(true, false, null, tInputs));
		assertEquals(1, tInputs[0].getCount(), "one pass consumes exactly one ingot");
	}

	// ------------------------------------------------------------------
	// p26-rm-row-backfill — the Shredder handler templates + the RECYCLABLE ring (:114-155)
	// ------------------------------------------------------------------

	/** The :114-150 census: 34 templates, twin MORTAR arms, the crushed-array rows carry 2-4 outputs. */
	@Test
	void shredTemplateCensus() {
		List<GT6RecipesShCL.ShredTemplate> tTable = GT6RecipesShCL.shredTemplateTable();
		assertEquals(34, tTable.size(), ":114-124 (11) + :126-136 (11) + :138-143 (6) + :145-150 (6)");
		for (GT6RecipesShCL.ShredTemplate tTpl : tTable) {
			assertEquals(16, tTpl.eUt(), "row " + tTpl.note() + " eUt");
			assertTrue(tTpl.outCounts().length == tTpl.outPrefixes().length, "row " + tTpl.note() + " shape");
		}
		GT6RecipesShCL.ShredTemplate tImpure = findShredTemplate(tTable, ":114");
		assertNotNull(tImpure);
		assertSame(OP.dustImpure, tImpure.inPrefix());
		assertEquals(256, tImpure.multiplier());
		assertFalse(tImpure.mortar(), ":114 is the MORTAR.NOT arm");
		assertTrue(tImpure.bedrockNot(), ":114 carries MT.Bedrock.NOT");
		assertEquals(2, tImpure.outPrefixes().length);
		GT6RecipesShCL.ShredTemplate tCluster = findShredTemplate(tTable, ":124");
		assertEquals(1, tCluster.outPrefixes().length, ":124 cluster → dust x3 single-output");
		assertEquals(3, tCluster.outCounts()[0]);
		GT6RecipesShCL.ShredTemplate tCentrifuged = findShredTemplate(tTable, ":140");
		assertEquals(4, tCentrifuged.outPrefixes().length, ":140 carries the 4-output crushed array");
		assertEquals(16, findShredTemplate(tTable, ":147").multiplier(), "the :145-150 arm runs multiplier 16");
		assertTrue(findShredTemplate(tTable, ":147").mortar());
	}

	/**
	 * The crushed-array rows pour with the mTargetPulver output-material hop
	 * (RecipeMapHandlerPrefixShredding.java:47) and a positive summed-units duration.
	 */
	@Test
	void shredCrushedRowPoursWithPulverHop() {
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		List<OreDictMaterial> tMaterials = GT6RecipesShCL.expandCrusherMaterials(OP.crushed);
		assertFalse(tMaterials.isEmpty(), "the crushed prefix must have registered materials (it is an item-path prefix)");
		GT6RecipesShCL.ShredTemplate tRow = findShredTemplate(GT6RecipesShCL.shredTemplateTable(), ":138");
		OreDictMaterial tWithRow = null;
		Recipe tPoured = null;
		for (OreDictMaterial tMaterial : tMaterials) {
			Recipe tRecipe = GT6RecipesShCL.buildShredRecipe(tRow, tMaterial);
			if (tRecipe != null) {tWithRow = tMaterial; tPoured = tRecipe; break;}
		}
		assertNotNull(tPoured, "at least one :138 row must resolve in the synthetic universe");
		OreDictMaterial tPulverTarget = tWithRow.mTargetPulver.mMaterial;
		assertEquals(3, tPoured.mOutputs.length, ":138 outputs dust + dustTiny + dustDiv72");
		assertEquals(SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.dust, tPulverTarget)), tPoured.mOutputs[0].getItem());
		assertEquals(SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.dustTiny, tPulverTarget)), tPoured.mOutputs[1].getItem());
		assertEquals(SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.dustDiv72, tPulverTarget)), tPoured.mOutputs[2].getItem());
		assertTrue(tPoured.mDuration >= 1);
		// the MORTAR twin arms are mutually exclusive: whatever :138 accepted, :145 must reject (and vice versa)
		GT6RecipesShCL.ShredTemplate tRowMortar = findShredTemplate(GT6RecipesShCL.shredTemplateTable(), ":145");
		if (tWithRow.contains(TD.Processing.MORTAR)) {
			assertNull(GT6RecipesShCL.buildShredRecipe(tRow, tWithRow));
			assertNotNull(GT6RecipesShCL.buildShredRecipe(tRowMortar, tWithRow));
		} else {
			assertNotNull(GT6RecipesShCL.buildShredRecipe(tRow, tWithRow));
			assertNull(GT6RecipesShCL.buildShredRecipe(tRowMortar, tWithRow));
		}
	}

	/**
	 * The :152 filter: the upstream byproduct carriers are excluded (toolHeadDrill), stick
	 * qualifies (RECYCLABLE, no exclusion tag, no byproduct upstream), and the ring row output
	 * is the OM.pulverize transcription — for the U2 stick the dustSmall x2 shape.
	 */
	@Test
	void recyclableRingFilterAndOutputShape() {
		assertTrue(GT6RecipesShCL.UPSTREAM_BYPRODUCT_PREFIXES.contains("toolHeadDrill"),
				"toolHeadDrill carries an upstream byproduct (OP.java:717) and must be excluded");
		assertFalse(GT6RecipesShCL.UPSTREAM_BYPRODUCT_PREFIXES.contains("stick"));
		assertTrue(OP.stick.contains(TD.Prefix.RECYCLABLE), "stick must carry RECYCLABLE for the ring");
		assertFalse(OP.stick.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED, TD.Prefix.DUST_BASED, TD.Prefix.IS_CONTAINER),
				"stick must pass the :152 exclusion tags");
		GT6RecipesShCL.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		OreDictMaterial tStickMaterial = null;
		for (OreDictMaterial tMaterial : GT6RecipesShCL.expandCrusherMaterials(OP.stick)) {
			if (!tMaterial.contains(TD.Atomic.ANTIMATTER) && !tMaterial.contains(TD.Properties.INVALID_MATERIAL) && !tMaterial.contains(TD.Processing.MORTAR)) {
				tStickMaterial = tMaterial; break;
			}
		}
		assertNotNull(tStickMaterial, "at least one non-mortar stick material must exist");
		Recipe tRingRow = GT6RecipesShCL.buildRingRecipe(OP.stick, tStickMaterial, 256, true);
		assertNotNull(tRingRow, "the :153 arm must resolve for a non-mortar stick material");
		ItemStack tExpectedOutput = GT6RecipesShCL.pulverizeOutput(tStickMaterial, OP.stick.mAmount);
		assertNotNull(tExpectedOutput);
		assertEquals(1, tRingRow.mOutputs.length, "the ring rows carry the remains output alone");
		assertEquals(tExpectedOutput.getItem(), tRingRow.mOutputs[0].getItem());
		assertEquals(tExpectedOutput.getCount(), tRingRow.mOutputs[0].getCount());
		assertEquals(SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.stick, tStickMaterial)), tRingRow.mInputs[0].getItem());
		// the twin arm is the MORTAR side — rejected for this material
		assertNull(GT6RecipesShCL.buildRingRecipe(OP.stick, tStickMaterial, 16, false));
		// dustCascade identity for the U2 stick (default pulver target): dustSmall x2
		assertEquals(OP.dustSmall, dustCascadePrefixOf(OP.stick.mAmount), "stick (U2) pulverizes into the dustSmall branch");
	}

	/** The OM.dust cascade branch probe for a unit amount (the prefix the cascade resolves). */
	private static OreDictPrefix dustCascadePrefixOf(long aUnits) {
		if (aUnits < CS.U72) return null;
		if (aUnits >= CS.U) return OP.dust;
		if (aUnits >= CS.U4) return OP.dustSmall;
		if (aUnits >= CS.U9) return OP.dustTiny;
		return OP.dustDiv72;
	}

	// ------------------------------------------------------------------
	// pooled surface
	// ------------------------------------------------------------------

	/** The pooled upstream surface is declared, not silent. */
	@Test
	void pooledEntriesAreDeclaredNotTranscribed() {
		String tSkipped = String.join("\n", GT6RecipesShCL.SKIPPED_UPSTREAM);
		assertTrue(tSkipped.contains(":691"), "the reeds IL row is pooled");
		assertTrue(tSkipped.contains(":699"), "the melon 6000-chance row is CUT (Remains_Fruit has no port item)");
		assertTrue(tSkipped.contains("Remains_Plant") && tSkipped.contains("Remains_Fruit"),
				"the GT Remains_* output items are declared absent from this port");
		assertTrue(tSkipped.contains(":697"), "the bone row's backfill is declared where it was once pooled");
		assertTrue(tSkipped.contains("rockGt"), "the null-output prefix rows are pooled");
		assertTrue(tSkipped.contains("RecipeMapHandlerCrushing"), "the crushed-family ore chain is pooled (chances window)");
		assertTrue(tSkipped.contains("RECYCLABLE"), "the RecipeMapShredder on-demand synthesis is pooled");
		assertTrue(tSkipped.contains("wood loop"), "the OreDict wood loop is pooled");
		assertTrue(tSkipped.contains("blockSolid Obsidian"), "the :82 input deviation (vanilla obsidian) is declared");
		assertEquals(8, GT6RecipesShCL.shredderTable().size());
		assertEquals(2, GT6RecipesShCL.latheTable().size());
		assertEquals(7, GT6RecipesShCL.crusherTable().size());
		assertEquals(4, GT6RecipesShCL.crusherVanillaTable().size());
		assertEquals(34, GT6RecipesShCL.shredTemplateTable().size());
		assertEquals(22, GT6RecipesShCL.latheTemplateTable().size());
	}

	/**
	 * The CRUSHER :65 rawOreChunk row: transcribed DATA (quantity-conserving 27*U72 → 3×9*U72)
	 * whose walk expands to ZERO rows — OP.rawOreChunk exists as prefix data but has no port
	 * item registrations (it is not in GTMaterialItems.itemPathPrefixes).
	 */
	@Test
	void crusherRawOreChunkRowIsZeroExpansionData() {
		GT6RecipesShCL.CrusherTemplate tRaw = findCrusherTemplate(GT6RecipesShCL.crusherTable(), ":65");
		assertNotNull(tRaw);
		assertSame(OP.rawOreChunk, tRaw.inPrefix());
		assertSame(OP.crushedTiny, tRaw.outPrefix());
		assertEquals(3, tRaw.outCount());
		assertEquals(OP.rawOreChunk.mAmount * 1, OP.crushedTiny.mAmount * 3, "the :65 row is quantity-conserving");
		assertTrue(GT6RecipesShCL.expandCrusherMaterials(OP.rawOreChunk).isEmpty(),
				"rawOreChunk has no registered port items — the :65 walk must expand to zero rows");
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

	private static GT6RecipesShCL.LatheTemplate findLatheTemplate(List<GT6RecipesShCL.LatheTemplate> aTable, String aNote) {
		for (GT6RecipesShCL.LatheTemplate tTemplate : aTable) if (tTemplate.note().equals(aNote)) return tTemplate;
		return null;
	}

	private static GT6RecipesShCL.ShredTemplate findShredTemplate(List<GT6RecipesShCL.ShredTemplate> aTable, String aNote) {
		for (GT6RecipesShCL.ShredTemplate tTemplate : aTable) if (tTemplate.note().equals(aNote)) return tTemplate;
		return null;
	}

	/** The independent recomputation of the p26 Lathe backfill (condition gate + both-side resolution). */
	private static boolean latheRowResolves(GT6RecipesShCL.LatheTemplate aTemplate, OreDictMaterial aMaterial) {
		if (!GT6RecipesShCL.latheCondition(aMaterial, aTemplate)) return false;
		return SYNTHETIC_ITEMS.containsKey(new PrefixMaterial(aTemplate.inPrefix(), aMaterial))
				&& SYNTHETIC_ITEMS.containsKey(new PrefixMaterial(aTemplate.outPrefix(), aMaterial));
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
		// p10-compat-vanilla-rows identities
		static net.minecraft.world.level.block.Block NETHER_BRICKS = net.minecraft.world.level.block.Blocks.NETHER_BRICKS;
		static net.minecraft.world.level.block.Block NETHERRACK = net.minecraft.world.level.block.Blocks.NETHERRACK;
		static net.minecraft.world.level.block.Block END_STONE = net.minecraft.world.level.block.Blocks.END_STONE;
		static net.minecraft.world.level.block.Block OBSIDIAN = net.minecraft.world.level.block.Blocks.OBSIDIAN;
	}

	/**
	 * RNG stubs for the chance-tier semantics — {@code java.util.Random.nextInt(int)} is
	 * overridable, so the tiers are exercised EXACTLY instead of sampling: an always-0
	 * generator succeeds every live chance; an always-(bound-1) generator fails every
	 * sub-10000 chance (9999 &lt; 10000 only, and the 10000 slots take the deterministic
	 * whole-stack branch without ever sampling).
	 */
	private static final class AlwaysYesRandom extends Random {
		@Override public int nextInt(int aBound) { return 0; }
	}

	private static final class AlwaysNoRandom extends Random {
		@Override public int nextInt(int aBound) { return aBound - 1; }
	}
}
