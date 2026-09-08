package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The Crusher ore chain (task p8-recipe-chances-orechain ④, yields reformed by
 * p9-recipe-yield-reform): the pure planner math against the upstream handler branches
 * (RecipeMapHandlerCrushing.java:50-137), the pour reconciliation over the offline material
 * universe (the cokeoven/ShCL template), and the row shapes end-to-end (Fe → the double
 * 3x Hematite base at 1152 t — two 10000 main slots, upstream sentinel+dup parity;
 * blockRaw 9 slots at x9/2 duration; the Cinnabar gem row at its summed chance).
 */
class GT6RecipesOreChainTest extends GTRecipesOfflineTestBase {

	/** The pinned oreRaw registration universe (the walk is registration-order, offline = live). */
	private static final int ORE_RAW_MATERIALS = 618;

	/** The pinned live pour: oreRaw plans that resolve in the registered universe (crushed/dust/gem of the target). */
	private static final int POURED_ORE_RAW_ROWS = 493;

	/**
	 * The synthetic offline universe: one distinct EXISTING item per registered (prefix,
	 * material) pair (the ShCL template). The blockRaw pairs are NOT in the universe — they
	 * mirror the LIVE shape (the block universe is the prefixblock card's), and the blockRaw
	 * row shape is exercised on a stub resolver instead.
	 */
	private static final Map<PrefixMaterial, Item> SYNTHETIC_ITEMS = new HashMap<>();

	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultMaterialResolver;

	@BeforeAll
	static void buildSyntheticUniverse() {
		GTMaterialItems.initMaterials(); // the offline material universe (MT.init + OP.init)
		// VANILLA-NAMESPACE ITEMS ONLY — the wrap-around aliasing is sensitive to the pool
		// SIZE, so a probe-registering test class (FileSawTest p24 / HammerWrenchTest p25)
		// would silently re-alias the universe; the minecraft-namespace filter pins the
		// pool to the frozen vanilla item set (the ShCL stabilization comment).
		List<Item> tPool = BuiltInRegistries.ITEM.stream()
				.filter(t -> "minecraft".equals(BuiltInRegistries.ITEM.getKey(t).getNamespace()))
				.filter(t -> new ItemStack(t, 1).getMaxStackSize() == 64) // 64-stack only: new ItemStack(item, N>max) silently clamps and breaks the parallel/count math (the ItemStack no-arg form is leg-agnostic; Item.getMaxStackSize takes a stack on 21.1)
				.toList();
		int tNext = 0;
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			Item tItem;
			do {tItem = tPool.get(tNext++ % tPool.size());} while (new ItemStack(tItem, 1).isEmpty()); // AIR never enters the pool (the ghost-recipe lesson)
			SYNTHETIC_ITEMS.put(tPair, tItem);
		}
		sDefaultMaterialResolver = GT6RecipesOreChain.sMaterialItemResolver;
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesOreChain.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipeMaps.reset();
		GT6RecipesOreChain.resetForTest();
	}

	// ------------------------------------------------------------------
	// table + walk shape
	// ------------------------------------------------------------------

	/** The two upstream driver prefixes (:153 oreRaw before :154 blockRaw), and the pinned walk. */
	@Test
	void templatesAndPinnedWalk() {
		List<GT6RecipesOreChain.OreChainTemplate> tTable = GT6RecipesOreChain.table();
		assertEquals(2, tTable.size());
		assertSame(OP.oreRaw, tTable.get(0).inPrefix());
		assertEquals(":153", tTable.get(0).note());
		assertSame(OP.blockRaw, tTable.get(1).inPrefix());
		assertEquals(":154", tTable.get(1).note());

		List<OreDictMaterial> tWalk = GT6RecipesOreChain.expandOreMaterials();
		assertEquals(ORE_RAW_MATERIALS, tWalk.size(), "the oreRaw registration universe (pinned — registration-order, offline == live)");
		Set<PrefixMaterial> tUniverse = new HashSet<>(GTMaterialItems.registrationOrder());
		for (OreDictMaterial tMaterial : tWalk) {
			assertTrue(tUniverse.contains(new PrefixMaterial(OP.oreRaw, tMaterial)), tMaterial.mNameInternal + " must be a registered oreRaw pair");
		}
	}

	// ------------------------------------------------------------------
	// pure planner math (upstream handler branches)
	// ------------------------------------------------------------------

	/** The multiplier branches (:53-67): plain x mOreMultiplier, blockRaw x2, Hexorium netherrack tuning. */
	@Test
	void multiplierBranches() {
		assertEquals(3, GT6RecipesOreChain.crushingMultiplier(OP.oreRaw, MT.Fe), "Fe: mOreProcessingMultiplier 1 x mOreMultiplier 3");
		assertEquals(6, GT6RecipesOreChain.crushingMultiplier(OP.blockRaw, MT.Fe), "blockRaw doubles (:64)");
		assertEquals(2, GT6RecipesOreChain.crushingMultiplier(OP.oreRaw, MT.Al));
		assertEquals(4, GT6RecipesOreChain.crushingMultiplier(OP.blockRaw, MT.Al));
		// the netherrack family (:55-62): HexoriumBlack +1, HexoriumRed -1, plain x mOreMultiplier
		assertEquals(4, GT6RecipesOreChain.crushingMultiplier(OP.oreNetherrack, MT.HexoriumBlack), "1 x (3+1)");
		assertEquals(3, GT6RecipesOreChain.crushingMultiplier(OP.oreNetherrack, MT.HexoriumRed), "1 x (4-1)");
		assertEquals(3, GT6RecipesOreChain.crushingMultiplier(OP.oreNetherrack, MT.Fe));
	}

	/** The skip states: the TODO branch (:73-76) and the ANTIMATTER gate (:51). */
	@Test
	void skipStates() {
		assertNull(GT6RecipesOreChain.planRow(OP.oreSmall, MT.Fe, ":xx"));
		assertNull(GT6RecipesOreChain.planRow(OP.oreRich, MT.Fe, ":xx"));
		assertNull(GT6RecipesOreChain.planRow(OP.oreNormal, MT.Fe, ":xx"));
		assertNull(GT6RecipesOreChain.planRow(OP.oreRaw, MT.UNUSED.Antimatter, ":xx"), "upstream :51 ANTIMATTER arm");
	}

	/** The orePoor tiny branch (:68-72): 3x the plain multiplier, poorTinyBranch flagged. */
	@Test
	void poorTinyBranchPlan() {
		GT6RecipesOreChain.OreChainPlan tPlan = GT6RecipesOreChain.planRow(OP.orePoor, MT.Fe, ":68");
		assertNotNull(tPlan);
		assertTrue(tPlan.poorTinyBranch());
		assertEquals(9, tPlan.multiplier(), "3 x the plain 3 (the :53-67 block runs before the :69 3x)");
		assertSame(MT.Fe2O3, tPlan.outMaterial());
	}

	/** The main-output count identity (:78-80) and the duration identity (:83/:101-102/:109). */
	@Test
	void mainCountAndDurationIdentities() {
		assertEquals(3, GT6RecipesOreChain.mainOutputCount(MT.Fe, 3), "units(U, U, 3) = 3 Hematite per Iron oreRaw");
		assertEquals(6, GT6RecipesOreChain.mainOutputCount(MT.Fe, 6));

		GT6RecipesOreChain.OreChainPlan tOreRaw = GT6RecipesOreChain.planRow(OP.oreRaw, MT.Fe, ":153");
		assertNotNull(tOreRaw);
		assertEquals(0, tOreRaw.extraCopies());
		assertFalse(tOreRaw.dense());
		assertEquals(1152, GT6RecipesOreChain.crushingDuration(tOreRaw, 3), "128 x 3 x max(1, mToolQuality 2 + 1)");

		GT6RecipesOreChain.OreChainPlan tBlockRaw = GT6RecipesOreChain.planRow(OP.blockRaw, MT.Fe, ":154");
		assertNotNull(tBlockRaw);
		assertEquals(7, tBlockRaw.extraCopies(), "blockRaw fills 7 more slots (:86-103) — the sentinel+dup pair is the double-slot buildRecipe base (p9 reform)");
		assertEquals(10368, GT6RecipesOreChain.crushingDuration(tBlockRaw, 6), "128 x 6 x 3 x 9 / 2");

		// the DENSE_ORE shape is unreachable in the first wave — exercised on a hand-built plan
		GT6RecipesOreChain.OreChainPlan tDense = new GT6RecipesOreChain.OreChainPlan(":xx", OP.oreRaw, MT.Fe, MT.Fe2O3, false, 3, 2, true, 0, 2, tOreRaw.byproducts());
		assertEquals(2304, GT6RecipesOreChain.crushingDuration(tDense, 3), "1152 x 2 (:109)");
	}

	/** The Cinnabar chance sums (:111-122): Redstone 2500+500, Cinnabar 2500+500, plain ores 0. */
	@Test
	void cinnabarBonusSums() {
		assertEquals(3000, GT6RecipesOreChain.cinnabarBonus(MT.Redstone), "PULVERIZING_CINNABAR 2500 + OREMATS.Cinnabar byproduct 500");
		assertEquals(3000, GT6RecipesOreChain.cinnabarBonus(MT.OREMATS.Cinnabar), "PULVERIZING_CINNABAR 2500 + Redstone byproduct 500");
		assertEquals(0, GT6RecipesOreChain.cinnabarBonus(MT.Fe));
		assertEquals(9, GT6RecipesOreChain.cinnabarGemCount(OP.blockRaw, false), ":124 blockRaw → 9");
		assertEquals(2, GT6RecipesOreChain.cinnabarGemCount(OP.oreRaw, true), ":124 DENSE_ORE → 2");
		assertEquals(1, GT6RecipesOreChain.cinnabarGemCount(OP.oreRaw, false), ":124 plain → 1");
	}

	// ------------------------------------------------------------------
	// buildRecipe: resolution + fallback + chances assembly
	// ------------------------------------------------------------------

	/** The three-level fallback (:77-81): crushed preferred, dust second, gem last, all-null skips. */
	@Test
	void mainOutputThreeLevelFallback() {
		GT6RecipesOreChain.OreChainPlan tPlan = GT6RecipesOreChain.planRow(OP.oreRaw, MT.Fe, ":153");
		assertNotNull(tPlan);
		GT6RecipesOreChain.sMaterialItemResolver = (aPrefix, aMaterial) -> Items.BRICK; // everything resolves

		Recipe tCrushed = GT6RecipesOreChain.buildRecipe(tPlan, 12);
		assertNotNull(tCrushed);
		assertEquals(Items.BRICK, tCrushed.mOutputs[0].getItem());
		assertEquals(3, tCrushed.mOutputs[0].getCount());

		GT6RecipesOreChain.sMaterialItemResolver = (aPrefix, aMaterial) -> aPrefix == OP.crushed ? null : Items.BRICK;
		Recipe tDust = GT6RecipesOreChain.buildRecipe(tPlan, 12);
		assertNotNull(tDust, "the dust fallback must catch the missing crushed tier");
		assertEquals(Items.BRICK, tDust.mOutputs[0].getItem());
		assertEquals(3, tDust.mOutputs[0].getCount());

		GT6RecipesOreChain.sMaterialItemResolver = (aPrefix, aMaterial) -> (aPrefix == OP.crushed || aPrefix == OP.dust) ? null : Items.BRICK;
		Recipe tGem = GT6RecipesOreChain.buildRecipe(tPlan, 12);
		assertNotNull(tGem, "the gem fallback must catch missing crushed+dust tiers");

		GT6RecipesOreChain.sMaterialItemResolver = (aPrefix, aMaterial) -> aMaterial == MT.Fe2O3 ? null : Items.BRICK;
		assertNull(GT6RecipesOreChain.buildRecipe(tPlan, 12), "all three tiers missing = the upstream :81 return F");
	}

	/** The chances assembly: Fe row = the double deterministic base; the Cinnabar row = +probabilistic gem. */
	@Test
	void chancesAssemblyFeAndCinnabar() {
		GT6RecipesOreChain.OreChainPlan tFe = GT6RecipesOreChain.planRow(OP.oreRaw, MT.Fe, ":153");
		GT6RecipesOreChain.sMaterialItemResolver = (aPrefix, aMaterial) -> Items.BRICK;
		Recipe tFeRow = GT6RecipesOreChain.buildRecipe(tFe, 12);
		assertNotNull(tFeRow);
		assertEquals(2, tFeRow.mOutputs.length, "Fe has no Cinnabar affinity and no prefix byproducts — the double main-output base after the trim (p9 reform)");
		assertArrayEquals(new long[] {10000, 10000}, tFeRow.mChances, "both main slots at 10000 — the sentinel at its upstream ctor-rewritten value (:906)");
		assertEquals(1152, tFeRow.mDuration);
		assertEquals(16, tFeRow.mEUt, "the handler rows run at eUt 16 (upstream :137 trailing args)");
		ItemStack[] tFeSampled = tFeRow.getOutputs(new Random(1), 1);
		assertEquals(3, tFeSampled[0].getCount());
		assertEquals(3, tFeSampled[1].getCount(), "the second main slot emits the same 3-count stack (upstream two-main-output parity)");

		GT6RecipesOreChain.OreChainPlan tCinnabar = GT6RecipesOreChain.planRow(OP.oreRaw, MT.OREMATS.Cinnabar, ":153");
		assertNotNull(tCinnabar);
		assertEquals(3000, tCinnabar.cinnabarChance());
		Recipe tCinnabarRow = GT6RecipesOreChain.buildRecipe(tCinnabar, 12);
		assertNotNull(tCinnabarRow);
		assertEquals(3, tCinnabarRow.mOutputs.length, "double main + gem Cinnabar");
		assertEquals(3000, tCinnabarRow.mChances[2], "the gem slot carries the summed chance (:111-125)");
		ItemStack[] tSampled = tCinnabarRow.getOutputs(new Random(3), 1);
		assertNotNull(tSampled[0], "the first 10000 main slot always emits");
		assertNotNull(tSampled[1], "the second 10000 main slot always emits (p9 double yield)");
		assertTrue(tSampled[2] == null || tSampled[2].getCount() == 1, "the 30% gem is unit Bernoulli — 0 or 1 at processCount 1");
	}

	/** The blockRaw row shape (:86-103): 9 main slots all-10000, duration x9/2, gem count 9 (:124). */
	@Test
	void blockRawRowShapeEmulatingThePrefixblockFuture() {
		GT6RecipesOreChain.OreChainPlan tPlan = GT6RecipesOreChain.planRow(OP.blockRaw, MT.Fe, ":154");
		assertNotNull(tPlan);
		GT6RecipesOreChain.sMaterialItemResolver = (aPrefix, aMaterial) -> Items.BRICK; // stub: the blockRaw items the prefixblock card will register
		Recipe tRow = GT6RecipesOreChain.buildRecipe(tPlan, 12);
		assertNotNull(tRow, "blockRaw rows pour once the prefixblock card registers the items (stub stand-ins here)");
		assertEquals(9, tRow.mOutputs.length, "the double base + 7 block copies = the upstream 9 main slots (:77-103)");
		assertEquals(6, tRow.mOutputs[0].getCount(), "multiplier 6 → 6 Hematite per block");
		for (int i = 0; i < 9; i++) assertEquals(10000, tRow.mChances[i], "every main slot is deterministic");
		assertEquals(10368, tRow.mDuration);
	}

	// ------------------------------------------------------------------
	// poured reconciliation
	// ------------------------------------------------------------------

	/**
	 * The live shape: the blockRaw legs have no items yet (the prefixblock card's universe),
	 * so only the oreRaw plans pour. The pour count reconciles against an independently
	 * recomputed expectation and pins the expansion shape.
	 */
	@Test
	void pourReconcilesWithThePinnedExpansion() {
		GT6RecipesOreChain.sMaterialItemResolver = (aPrefix, aMaterial) -> {
			if (aPrefix == OP.blockRaw) return null; // the block universe is the prefixblock card's
			return SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		};
		GT6RecipesOreChain.load();

		int tExpected = 0;
		for (GT6RecipesOreChain.OreChainTemplate tTemplate : GT6RecipesOreChain.table()) {
			for (OreDictMaterial tMaterial : GT6RecipesOreChain.expandOreMaterials()) {
				GT6RecipesOreChain.OreChainPlan tPlan = GT6RecipesOreChain.planRow(tTemplate.inPrefix(), tMaterial, tTemplate.note());
				if (tPlan == null) continue;
				if (GT6RecipesOreChain.buildRecipe(tPlan, 12) != null) tExpected++;
			}
		}
		assertEquals(tExpected, GT6RecipeMaps.CRUSHER.mRecipeList.size(),
				"poured == resolving plans over the 2-prefix x " + ORE_RAW_MATERIALS + "-material walk");
		assertEquals(POURED_ORE_RAW_ROWS, tExpected,
				"the pinned live pour: the oreRaw legs of the " + (2 * ORE_RAW_MATERIALS) + " transcribed data rows (blockRaw all skip until the prefixblock card)");

		// the guard backstop: no stored row may have an empty input leg
		for (Recipe tRecipe : GT6RecipeMaps.CRUSHER.mRecipeList) {
			assertTrue(tRecipe.mInputs.length > 0, "every stored ore-chain row has an item input leg");
			assertEquals(1, tRecipe.mInputs.length, "ore-chain rows are single-input");
			assertEquals(1, tRecipe.mInputs[0].getCount(), "upstream ST.amount(1, aInput)");
		}
	}

	/** load() is idempotent within a generation; the second call must not double-pour. */
	@Test
	void loadIsIdempotent() {
		GT6RecipesOreChain.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		GT6RecipesOreChain.load();
		int tFirst = GT6RecipeMaps.CRUSHER.mRecipeList.size();
		assertTrue(tFirst > 0);
		GT6RecipesOreChain.load();
		assertEquals(tFirst, GT6RecipeMaps.CRUSHER.mRecipeList.size());
	}

	/** An empty synthetic universe (no blockRaw items live yet is the REAL shape): oreRaw rows pour, blockRaw rows skip. */
	@Test
	void liveUniverseShapeOreRawOnlyPours() {
		// the LIVE universe has no blockRaw items: serve only genuinely registered pairs
		GT6RecipesOreChain.sMaterialItemResolver = (aPrefix, aMaterial) -> {
			if (aPrefix == OP.blockRaw) return null; // the block universe is the prefixblock card's
			return SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		};
		GT6RecipesOreChain.load();
		int tOreRawOnly = 0;
		for (OreDictMaterial tMaterial : GT6RecipesOreChain.expandOreMaterials()) {
			GT6RecipesOreChain.OreChainPlan tPlan = GT6RecipesOreChain.planRow(OP.oreRaw, tMaterial, ":153");
			if (tPlan == null) continue; // the in-walk ANTIMATTER materials are declared non-rows
			if (GT6RecipesOreChain.buildRecipe(tPlan, 12) != null) tOreRawOnly++;
		}
		assertEquals(tOreRawOnly, GT6RecipeMaps.CRUSHER.mRecipeList.size(),
				"the live shape pours only the oreRaw legs (" + tOreRawOnly + " of " + (2 * ORE_RAW_MATERIALS) + " transcribed data rows)");
	}

	/**
	 * The findRecipe positive control end-to-end through the two-stage consume contract, on
	 * the first walk material whose synthetic input item keys exactly ONE poured row (the
	 * modulo pool aliases pairs, the ShCL inputUses discipline).
	 */
	@Test
	void positiveControlThroughFindRecipe() {
		GT6RecipesOreChain.sMaterialItemResolver = (aPrefix, aMaterial) -> {
			if (aPrefix == OP.blockRaw) return null;
			return SYNTHETIC_ITEMS.get(new PrefixMaterial(aPrefix, aMaterial));
		};
		// self-grounding (ADR-P18): boot leftovers (21.1 junit-fml pours the live registry rows)
		// or a prior class's unpaired reset() never feed this test — the pour below must be THIS
		// resolver's generation, and the generation reset retires the pour-flag with the maps.
		GT6RecipeMaps.reset();
		GT6RecipesOreChain.load();

		Map<Item, Integer> tInputUses = new HashMap<>();
		for (OreDictMaterial tMaterial : GT6RecipesOreChain.expandOreMaterials()) {
			GT6RecipesOreChain.OreChainPlan tPlan = GT6RecipesOreChain.planRow(OP.oreRaw, tMaterial, ":153");
			if (tPlan != null && GT6RecipesOreChain.buildRecipe(tPlan, 12) != null) {
				tInputUses.merge(SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.oreRaw, tMaterial)), 1, Integer::sum);
			}
		}
		OreDictMaterial tControl = null;
		for (OreDictMaterial tCandidate : GT6RecipesOreChain.expandOreMaterials()) {
			GT6RecipesOreChain.OreChainPlan tPlan = GT6RecipesOreChain.planRow(OP.oreRaw, tCandidate, ":153");
			if (tPlan == null || GT6RecipesOreChain.buildRecipe(tPlan, 12) == null) continue;
			if (tInputUses.get(SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.oreRaw, tCandidate))) == 1) {tControl = tCandidate; break;}
		}
		assertNotNull(tControl, "at least one walk material must expand to a uniquely-keyed input item");

		Item tInput = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.oreRaw, tControl));
		GT6RecipesOreChain.OreChainPlan tPlan = GT6RecipesOreChain.planRow(OP.oreRaw, tControl, ":153");
		ItemStack[] tInputs = {new ItemStack(tInput, 16)};
		Recipe tFound = GT6RecipeMaps.CRUSHER.findRecipe(null, 16, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound, "the oreRaw row of " + tControl.mNameInternal + " must be findable after the pour");
		assertTrue(tFound.isRecipeInputEqual(false, false, null, tInputs));
		assertEquals(16, tInputs[0].getCount(), "the probe must not consume");
		assertTrue(tFound.isRecipeInputEqual(true, false, null, tInputs));
		assertEquals(15, tInputs[0].getCount(), "one pass consumes exactly one oreRaw");
		ItemStack[] tOutputs = tFound.getOutputs(1);
		long tExpectedCount = GT6RecipesOreChain.mainOutputCount(tControl, tPlan.multiplier());
		assertEquals(2, tOutputs.length, "every plain row yields the double main-output base (p9 reform)");
		assertEquals(tExpectedCount, tOutputs[0].getCount(), "the main output carries the multiplier-scaled count");
		assertEquals(tExpectedCount, tOutputs[1].getCount(), "the duplicate main slot carries the same count (upstream parity)");
		assertEquals(GT6RecipesOreChain.crushingDuration(tPlan, tOutputs[0].getCount()), tFound.mDuration);
	}
}
