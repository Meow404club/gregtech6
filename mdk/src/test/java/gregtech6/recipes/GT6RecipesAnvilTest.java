package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import gregapi.data.CS;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The Anvil recipe pour (task p28-c-anvil, acceptance ⑤ "两 RM 图行匹配"):
 * <ul>
 * <li>the two maps register under their upstream internal names with the RM.java:118-120
 *     constants (items 2/2/2, fluids 0/0/0, MIN 0, AMP 1) and lowercase GUI paths;</li>
 * <li>the transcription census: 41 RM.Anvil templates + 5 bend templates, the zero-
 *     expansion set named;</li>
 * <li>the poured reconciliation: {@link GT6RecipesAnvil#load()} over the synthetic offline
 *     universe, with the expected count recomputed INDEPENDENTLY here (the cokeoven test
 *     template, the GT6RecipesShCLTest ruling);</li>
 * <li>row-shape spot checks (the :157 shredding hop, the :175 dual-input weld, the
 *     :191/:209 chance literals, the :213 foil counts);</li>
 * <li>the getCosts duration identity over the summed dual-slot unit columns.</li>
 * </ul>
 */
class GT6RecipesAnvilTest extends GTRecipesOfflineTestBase {

	/** The synthetic offline universe (the GT6RecipesShCLTest form — vanilla items only). */
	private static final Map<PrefixMaterial, Item> SYNTHETIC_ITEMS = new HashMap<>();

	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultMaterialResolver;

	@BeforeAll
	static void buildSyntheticUniverse() {
		GTMaterialItems.initMaterials();
		List<Item> tPool = BuiltInRegistries.ITEM.stream()
				.filter(t -> "minecraft".equals(BuiltInRegistries.ITEM.getKey(t).getNamespace()))
				.filter(t -> new ItemStack(t, 1).getMaxStackSize() == 64)
				.toList();
		int tNext = 0;
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			Item tItem;
			do {tItem = tPool.get(tNext++ % tPool.size());} while (new ItemStack(tItem, 1).isEmpty());
			SYNTHETIC_ITEMS.put(tPair, tItem);
		}
		sDefaultMaterialResolver = GT6RecipesAnvil.sMaterialItemResolver;
		GT6RecipesAnvil.sMaterialItemResolver = (aPrefix, aMaterial) -> {
			Item tItem = SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
			return tItem == null ? null : tItem; // no port item → the mat() → null convention
		};
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesAnvil.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipeMaps.reset();
		GT6RecipesAnvil.resetForTest();
	}

	// ------------------------------------------------------------------
	// map constants (RM.java:118-120)
	// ------------------------------------------------------------------

	@Test
	void twoMapsRegisterWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertSame(GT6RecipeMaps.ANVIL, RecipeMap.RECIPE_MAPS.get("gt.recipe.anvil"));
		assertSame(GT6RecipeMaps.ANVIL_BEND, RecipeMap.RECIPE_MAPS.get("gt.recipe.anvil.bend"));

		for (RecipeMap tMap : new RecipeMap[] {GT6RecipeMaps.ANVIL, GT6RecipeMaps.ANVIL_BEND}) {
			assertEquals(tMap.mGUIPath, tMap.mGUIPath.toLowerCase(java.util.Locale.ROOT));
			assertTrue(tMap.mGUIPath.startsWith("gt6:textures/gui/machines/"));
			// the RM.java:118-120 constants row — items 2/2/2, fluids 0/0/0, MIN 0, AMP 1
			assertEquals(2, tMap.mInputItemsCount, "the two working halves");
			assertEquals(2, tMap.mOutputItemsCount);
			assertEquals(2, tMap.mMinimalInputItems);
			assertEquals(0, tMap.mInputFluidCount);
			assertEquals(0, tMap.mOutputFluidCount);
			assertEquals(0, tMap.mMinimalInputFluids);
			assertEquals(0, tMap.mMinimalInputs, "MIN 0");
			assertEquals(1, tMap.mPower, "AMP 1 folds onto the port mPower");
		}
		assertEquals("gt6:textures/gui/machines/anvil.png", GT6RecipeMaps.ANVIL.mGUIPath);
		assertEquals("gt6:textures/gui/machines/anvilbend.png", GT6RecipeMaps.ANVIL_BEND.mGUIPath);
	}

	// ------------------------------------------------------------------
	// the transcription census
	// ------------------------------------------------------------------

	@Test
	void templateCensus() {
		List<GT6RecipesAnvil.AnvilTemplate> tAnvil = GT6RecipesAnvil.anvilTable();
		List<GT6RecipesAnvil.AnvilTemplate> tBend = GT6RecipesAnvil.bendTable();
		assertEquals(45, tAnvil.size(), ":157-165 grinding (9) + :166 oreRaw + :167-172 crushed (6) + :175-187 welding (13) + :188 ring + :191-199 chances (9) + :200-205 gems (6) = 45, the file order census");
		assertEquals(5, tBend.size(), ":208-210 Big + :213-214 Small — the union pour");

		// the zero-expansion set: the input prefixes OUTSIDE the port item path (the class doc)
		Set<String> tZero = new HashSet<>();
		for (GT6RecipesAnvil.AnvilTemplate tTemplate : tAnvil) {
			if (GT6RecipesShCL.expandCrusherMaterials(tTemplate.in1()).isEmpty()) tZero.add(tTemplate.note());
		}
		assertEquals(Set.of(":158", ":159", ":160", ":161", ":162", ":163", ":164", ":165"), tZero,
				"chunk/rubble/pebbles/clump/reduced/crystalline/cleanGravel/cluster have no port items");

		// the :192 plateSteamcraft row stays transcribed (DATA), its OUTPUT is unresolvable
		GT6RecipesAnvil.AnvilTemplate tSteamcraft = findAnvilRow(tAnvil, ":192");
		assertNotNull(tSteamcraft);
		assertSame(OP.plateSteamcraft, tSteamcraft.outPrefixes()[0]);
		assertArrayEquals(new long[] {10000, 9000}, tSteamcraft.chances());
	}

	// ------------------------------------------------------------------
	// the poured reconciliation
	// ------------------------------------------------------------------

	@Test
	void pourReconciliation() {
		// the resolver rides the TEST (the GT6RecipesShCLTest form) — the @AfterEach restore
		// would otherwise hand load() a dead resolver when another method ran first
		GT6RecipesAnvil.sMaterialItemResolver = (aPrefix, aMaterial) ->
				SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		GT6RecipesAnvil.load();
		assertTrue(GT6RecipeMaps.ANVIL.mRecipeList.size() > 0, "the pour is live over the synthetic universe");

		int tAnvilExpected = expectedPours(GT6RecipesAnvil.anvilTable(), new HashSet<>());
		int tBendExpected = expectedPours(GT6RecipesAnvil.bendTable(), new HashSet<>());
		assertEquals(tAnvilExpected, GT6RecipeMaps.ANVIL.mRecipeList.size(), "ANVIL rows = recomputed walk");
		assertEquals(tBendExpected, GT6RecipeMaps.ANVIL_BEND.mRecipeList.size(), "ANVIL_BEND rows = recomputed walk");

		// load() is idempotent within a generation
		GT6RecipesAnvil.load();
		assertEquals(tAnvilExpected, GT6RecipeMaps.ANVIL.mRecipeList.size(), "no double-pour");
	}

	/** The independent walk (condition + resolution + the exact-row dedup), the test's own shape. */
	private int expectedPours(List<GT6RecipesAnvil.AnvilTemplate> aTemplates, Set<String> aSeen) {
		int tExpected = 0;
		for (GT6RecipesAnvil.AnvilTemplate tTemplate : aTemplates) {
			for (OreDictMaterial tMaterial : GT6RecipesShCL.expandCrusherMaterials(tTemplate.in1())) {
				Recipe tRecipe = GT6RecipesAnvil.buildRecipe(tTemplate, tMaterial);
				if (tRecipe == null) continue;
				if (!aSeen.add(GT6RecipesShCL.rowKey(tRecipe))) continue;
				tExpected++;
			}
		}
		return tExpected;
	}

	// ------------------------------------------------------------------
	// row-shape spot checks
	// ------------------------------------------------------------------

	@Test
	void grindingRowShapes() {
		List<GT6RecipesAnvil.AnvilTemplate> tTable = GT6RecipesAnvil.anvilTable();

		// :157 — rockGt → dustSmall x9, the sole non-MORTAR grinding row, eUt 16 mult 16
		GT6RecipesAnvil.AnvilTemplate tRock = findAnvilRow(tTable, ":157");
		assertNotNull(tRock);
		assertSame(OP.rockGt, tRock.in1());
		assertSame(OP.dustSmall, tRock.outPrefixes()[0]);
		assertEquals(9, tRock.outCounts()[0]);
		assertEquals(16, tRock.eUt());
		assertTrue(tRock.shredding(), "the RecipeMapHandlerPrefixShredding hop flag");
		assertFalse(tRock.useMortar(), ":157 carries ANTIMATTER.NOT only");

		// :158 — chunk → dust x2 + dustTiny x1, MORTAR-gated, dual output
		GT6RecipesAnvil.AnvilTemplate tChunk = findAnvilRow(tTable, ":158");
		assertNotNull(tChunk);
		assertTrue(tChunk.useMortar());
		assertEquals(2, tChunk.outPrefixes().length);
		assertSame(OP.dust, tChunk.outPrefixes()[0]);
		assertSame(OP.dustTiny, tChunk.outPrefixes()[1]);
		assertEquals(1, tChunk.outCounts()[1]);

		// :166 — oreRaw → crushed + crushedTiny x6, MORTAR + selfcrush()
		GT6RecipesAnvil.AnvilTemplate tOreRaw = findAnvilRow(tTable, ":166");
		assertNotNull(tOreRaw);
		assertTrue(tOreRaw.useSelfcrush(), "selfcrush() = mTargetSmashing.mMaterial == mat");
		assertSame(OP.crushedTiny, tOreRaw.outPrefixes()[1]);
		assertEquals(6, tOreRaw.outCounts()[1]);

		// :175 — ingot + ingot → ingotDouble (the dual-input welding row)
		GT6RecipesAnvil.AnvilTemplate tWeld = findAnvilRow(tTable, ":175");
		assertNotNull(tWeld);
		assertSame(OP.ingot, tWeld.in1());
		assertSame(OP.ingot, tWeld.in2());
		assertSame(OP.ingotDouble, tWeld.outPrefixes()[0]);
		assertTrue(tWeld.forgePurity(), "the full forge-purity conjunct set");
		assertTrue(tWeld.smithable());
		assertTrue(tWeld.flammableNot());
		assertTrue(tWeld.coatedNot());

		// :188 — ring x2 → chain, bare ANTIMATTER.NOT
		GT6RecipesAnvil.AnvilTemplate tChain = findAnvilRow(tTable, ":188");
		assertNotNull(tChain);
		assertEquals(2, tChain.inCount1());
		assertFalse(tChain.smithable(), ":188 has no SMITHABLE conjunct");

		// :191 — chunkGt → plateTiny + scrap x1 @9000
		GT6RecipesAnvil.AnvilTemplate tChunkGt = findAnvilRow(tTable, ":191");
		assertNotNull(tChunkGt);
		assertArrayEquals(new long[] {10000, 9000}, tChunkGt.chances(), "the .chances(10000, 9000) literal");
		assertSame(OP.scrapGt, tChunkGt.outPrefixes()[1]);

		// :200-205 — the gem chain, eUt 64
		GT6RecipesAnvil.AnvilTemplate tGem = findAnvilRow(tTable, ":200");
		assertNotNull(tGem);
		assertSame(OP.gemLegendary, tGem.in1());
		assertSame(OP.gemExquisite, tGem.outPrefixes()[0]);
		assertEquals(2, tGem.outCounts()[0]);
		assertEquals(64, tGem.eUt());
		assertNull(tGem.chances(), "the gem rows are deterministic");
	}

	@Test
	void bendRowShapes() {
		List<GT6RecipesAnvil.AnvilTemplate> tTable = GT6RecipesAnvil.bendTable();

		// :208 — plate → plateCurved (Big), SMITHABLE only
		GT6RecipesAnvil.AnvilTemplate tCurved = findBendRow(tTable, ":208");
		assertNotNull(tCurved);
		assertSame(OP.plate, tCurved.in1());
		assertSame(OP.plateCurved, tCurved.outPrefixes()[0]);
		assertTrue(tCurved.smithable());
		assertFalse(tCurved.forgePurity(), "the Big row carries no forge-purity conjuncts");

		// :209 — stick → springSmall + scrap x2 @9000 (Big)
		GT6RecipesAnvil.AnvilTemplate tSpring = findBendRow(tTable, ":209");
		assertNotNull(tSpring);
		assertSame(OP.springSmall, tSpring.outPrefixes()[0]);
		assertSame(OP.scrapGt, tSpring.outPrefixes()[1]);
		assertEquals(2, tSpring.outCounts()[1]);
		assertArrayEquals(new long[] {10000, 9000}, tSpring.chances());

		// :210 — stickLong → spring (Big, deterministic)
		GT6RecipesAnvil.AnvilTemplate tSpringBig = findBendRow(tTable, ":210");
		assertNotNull(tSpringBig);
		assertSame(OP.spring, tSpringBig.outPrefixes()[0]);
		assertNull(tSpringBig.chances());

		// :213 — plate → foil x2 + scrap x4 @9000, WITH the forge-purity conjuncts
		GT6RecipesAnvil.AnvilTemplate tFoil = findBendRow(tTable, ":213");
		assertNotNull(tFoil);
		assertSame(OP.foil, tFoil.outPrefixes()[0]);
		assertEquals(2, tFoil.outCounts()[0]);
		assertEquals(4, tFoil.outCounts()[1]);
		assertTrue(tFoil.forgePurity(), "the Small plate row's selfforge+fullforge+COATED.NOT");

		// :214 — stick → ring + scrap x2 @9000 (Small, SMITHABLE only)
		GT6RecipesAnvil.AnvilTemplate tRing = findBendRow(tTable, ":214");
		assertNotNull(tRing);
		assertSame(OP.ring, tRing.outPrefixes()[0]);
		assertFalse(tRing.forgePurity());
		assertTrue(tRing.smithable());
	}

	/**
	 * The getCosts identity over the summed dual-slot columns (:73-78/:225-227): the :175
	 * weld row's unitsIn = 2U (ingot + ingot), unitsOut = 2U (ingotDouble) → duration =
	 * units(2U, U, 64+64q, T) = 2*(64+64q). A fixture material with mToolQuality 2 gives
	 * 384.
	 */
	@Test
	void weldDurationSumsBothSlots() {
		GT6RecipesAnvil.AnvilTemplate tWeld = findAnvilRow(GT6RecipesAnvil.anvilTable(), ":175");
		assertNotNull(tWeld);
		long tUnitsIn = tWeld.in1().mAmount * tWeld.inCount1() + tWeld.in2().mAmount * tWeld.inCount2();
		long tUnitsOut = tWeld.outPrefixes()[0].mAmount * tWeld.outCounts()[0];
		assertEquals(2 * CS.U, tUnitsIn, "ingot + ingot");
		assertEquals(2 * CS.U, tUnitsOut, "ingotDouble");
		// the units() arithmetic with a multiplier of 64 and tool quality q:
		// duration = ceil(2U * 64(1+q) / U) = 128(1+q); q=2 → 384
		assertEquals(384, GT6RecipesShCL.handlerCosts(tUnitsIn, tUnitsOut, 64, fixtureQualityTwo()));
	}

	/** A material fixture with mToolQuality 2 (the live universe walk). */
	private static OreDictMaterial fixtureQualityTwo() {
		for (OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial != null && tMaterial.mToolQuality == 2) return tMaterial;
		}
		throw new AssertionError("no tool-quality-2 material in the offline universe");
	}

	/** The condition gate: the ANTIMATTER/INVALID_MATERIAL head (the ShCL gate form). */
	@Test
	void conditionHeadFilters() {
		GT6RecipesAnvil.AnvilTemplate tRock = findAnvilRow(GT6RecipesAnvil.anvilTable(), ":157");
		assertNotNull(tRock);
		for (OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null) continue;
			if (tMaterial.contains(TD.Atomic.ANTIMATTER) || tMaterial.contains(TD.Properties.INVALID_MATERIAL)) {
				assertFalse(GT6RecipesAnvil.condition(tMaterial, tRock), ":205 filters " + tMaterial.mNameInternal);
			}
		}
	}

	private static GT6RecipesAnvil.AnvilTemplate findAnvilRow(List<GT6RecipesAnvil.AnvilTemplate> aTable, String aNote) {
		for (GT6RecipesAnvil.AnvilTemplate tRow : aTable) {
			if (tRow.note().equals(aNote)) return tRow;
		}
		return null;
	}

	private static GT6RecipesAnvil.AnvilTemplate findBendRow(List<GT6RecipesAnvil.AnvilTemplate> aTable, String aNote) {
		return findAnvilRow(aTable, aNote);
	}
}
