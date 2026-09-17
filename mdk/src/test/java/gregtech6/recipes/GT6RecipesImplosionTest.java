package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.item.GT6Circuits;

/**
 * The Implosion Compressor pour offline acceptance (task p31-implosion): the 4-tier
 * transcription of Loader_Recipes_Other.java:709-764 (the :711-714 template arms —
 * counts, selector configs, output prefixes, eUt 0 / duration 256), the group walk
 * (ANY.Diamond→DiamondIndustrial, the identity groups, the :758 singles), the
 * selector routing truth table (config 0..3 route the four tiers, a non-row config
 * matches nothing, a missing selector fails the 3-item minimum) and the never-consumed
 * selector arm (the Recipe.sNotConsumable identity-skip with the fixture stand-in —
 * the live binding is GT6Circuits::isSelector, proven by the RCON chain).
 *
 * <p>The fixture item seams follow the GT6RecipesDistilleryTest convention: the vanilla
 * registry is frozen offline, so prefix→item resolution answers vanilla stand-ins
 * (dust→CLAY_BALL, plateGem→BRICK, gem→IRON_INGOT, gemFlawless→GOLD_INGOT,
 * gemExquisite→DIAMOND) and the TNT leg answers the REAL vanilla TNT item.
 */
public class GT6RecipesImplosionTest extends GTRecipesOfflineTestBase {

	private static java.util.function.BiFunction<gregapi.oredict.OreDictPrefix, OreDictMaterial, net.minecraft.world.item.Item> sDefaultMaterialResolver;
	private static Supplier<net.minecraft.world.item.Item> sDefaultTntResolver;
	private static Function<Integer, ItemStack> sDefaultCircuitResolver;
	private static java.util.function.Predicate<ItemStack> sDefaultNotConsumable;

	/** The offline fixture resolver: vanilla stand-ins per prefix (the Distillery fixture convention). */
	private static final java.util.function.BiFunction<gregapi.oredict.OreDictPrefix, OreDictMaterial, net.minecraft.world.item.Item> FIXTURE_RESOLVER =
			(aPrefix, aMaterial) -> {
				if (aPrefix == OP.dust        ) return Items.CLAY_BALL;
				if (aPrefix == OP.plateGem    ) return Items.BRICK;
				if (aPrefix == OP.gem         ) return Items.IRON_INGOT;
				if (aPrefix == OP.gemFlawless ) return Items.GOLD_INGOT;
				if (aPrefix == OP.gemExquisite) return Items.DIAMOND;
				return null;
			};

	/** The fixture circuit stack: a tagged BRICKS stand-in (the Distillery fixtureCircuit form). */
	private static ItemStack circuit(int aConfig) {
		return GT6Circuits.selector(Items.BRICKS, aConfig);
	}

	/** The offline fixture predicate — the Distillery FIXTURE_NOT_CONSUMABLE form (tagged BRICKS). */
	public static final java.util.function.Predicate<ItemStack> FIXTURE_NOT_CONSUMABLE = aStack ->
			!aStack.isEmpty() && aStack.is(Items.BRICKS) && GT6Circuits.hasConfigurationTag(aStack);

	@BeforeAll
	static void captureDefaults() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		MT.init(); // the group walks need the material table (the MaterialStackNBTTest form)
		sDefaultMaterialResolver = GT6RecipesImplosion.sMaterialItemResolver;
		sDefaultTntResolver = GT6RecipesImplosion.sTntResolver;
		sDefaultCircuitResolver = GT6RecipesImplosion.sCircuitResolver;
		sDefaultNotConsumable = Recipe.sNotConsumable;
		Recipe.sNotConsumable = FIXTURE_NOT_CONSUMABLE;
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesImplosion.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipesImplosion.sTntResolver = sDefaultTntResolver;
		GT6RecipesImplosion.sCircuitResolver = sDefaultCircuitResolver;
		GT6RecipeMaps.reset();
		GT6RecipesImplosion.resetForTest();
	}

	@AfterAll
	static void restorePredicate() {
		Recipe.sNotConsumable = sDefaultNotConsumable;
	}

	// ------------------------------------------------------------------
	// the transcription — the :711-714 tier arms and the group walk
	// ------------------------------------------------------------------

	@Test
	void tiersTablePinsTheUpstreamArms() {
		assertEquals(4, GT6RecipesImplosion.TIERS.size());
		// :711 — dust1 + TNT x8 + tag(0) → plateGem
		assertTier(":711", 1, 8, 0, OP.plateGem);
		// :712 — dust1 + TNT x8 + tag(1) → gem
		assertTier(":712", 1, 8, 1, OP.gem);
		// :713 — dust2 + TNT x32 + tag(2) → gemFlawless (ST.mul(4, tTNT) on the count-8 stack)
		assertTier(":713", 2, 32, 2, OP.gemFlawless);
		// :714 — dust4 + TNT x64 + tag(3) → gemExquisite (ST.mul(8, tTNT))
		assertTier(":714", 4, 64, 3, OP.gemExquisite);
	}

	private void assertTier(String aNote, int aDust, int aTnt, int aConfig, gregapi.oredict.OreDictPrefix aPrefix) {
		GT6RecipesImplosion.ImplosionTier tTier = GT6RecipesImplosion.TIERS.stream()
				.filter(t -> t.note().equals(aNote)).findFirst().orElse(null);
		assertNotNull(tTier, "tier " + aNote + " transcribed");
		assertEquals(aDust, tTier.dustCount(), aNote + ": the dust count");
		assertEquals(aTnt, tTier.tntCount(), aNote + ": the vanilla TNT branch count (ST.make(Blocks.tnt, 8, W) base)");
		assertEquals(aConfig, tTier.selectorConfig(), aNote + ": the ST.tag selector configuration");
		assertEquals(aPrefix, tTier.outPrefix(), aNote + ": the output prefix");
	}

	@Test
	void tableWalksTheUpstreamGroups() {
		assertTrue(GT6RecipesImplosion.table().size() >= 9, "the nine group arms produce rows");
		// the fixed-output groups
		assertRowMember(":710", MT.Diamond, MT.DiamondIndustrial);
		assertRowMember(":716", MT.BlueSapphire, MT.Sapphire); // a Sapphire group member outputs Sapphire
		assertRowMember(":722", MT.Emerald, MT.Emerald);
		// the identity groups
		assertRowMember(":728", MT.Amethyst, MT.Amethyst);
		assertRowMember(":734", MT.Spessartine, MT.Spessartine); // a Garnet group member (no single MT.Garnet — the colored variants only)
		assertRowMember(":740", MT.Jasper, MT.Jasper);
		assertRowMember(":746", MT.TigerEyeYellow, MT.TigerEyeYellow);
		assertRowMember(":752", MT.AventurineGreen, MT.AventurineGreen);
		// the :758 explicit singles (all fourteen, identity output)
		OreDictMaterial[] tSingles = {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Zanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Peridot, MT.Dioptase, MT.Craponite};
		for (OreDictMaterial tMat : tSingles) assertRowMember(":758", tMat, tMat);
		// the group membership rides the root re-registration set — MT.Diamond is a Diamond group member
		assertTrue(ANY.Diamond.mToThis.contains(MT.Diamond), "put(ANY.Diamond) routed MT.Diamond into the group walk");
	}

	private void assertRowMember(String aNote, OreDictMaterial aMaterial, OreDictMaterial aOutput) {
		GT6RecipesImplosion.ImplosionRow tRow = GT6RecipesImplosion.table().stream()
				.filter(r -> r.note().equals(aNote) && r.material() == aMaterial).findFirst().orElse(null);
		assertNotNull(tRow, aNote + " walks " + aMaterial.mNameInternal + " (material from the group walk)");
		assertEquals(aOutput, tRow.output(), aNote + ": the output material");
	}

	@Test
	void skippedUpstreamAuditNamesTheFourTntBranches() {
		assertEquals(1, GT6RecipesImplosion.SKIPPED_UPSTREAM.size());
		assertTrue(GT6RecipesImplosion.SKIPPED_UPSTREAM.get(0).contains("IC2_ITNT")
				&& GT6RecipesImplosion.SKIPPED_UPSTREAM.get(0).contains("Boomstick")
				&& GT6RecipesImplosion.SKIPPED_UPSTREAM.get(0).contains("Dynamite")
				&& GT6RecipesImplosion.SKIPPED_UPSTREAM.get(0).contains("Dynamite_Strong"),
				"the four non-vanilla TNT branches are the declared cut");
	}

	// ------------------------------------------------------------------
	// the pour: every fixture-resolvable row x tier lands
	// ------------------------------------------------------------------

	@Test
	void loadPoursEveryResolvedRowTier() {
		GT6RecipesImplosion.sMaterialItemResolver = FIXTURE_RESOLVER;
		GT6RecipesImplosion.sTntResolver = () -> Items.TNT;
		GT6RecipesImplosion.sCircuitResolver = GT6RecipesImplosionTest::circuit;
		GT6RecipesImplosion.load();
		assertEquals(GT6RecipesImplosion.table().size() * 4, GT6RecipeMaps.IMPLOSION.mRecipeList.size(),
				"every table row x every tier resolves over the fixture seams — zero skips");
		for (Recipe tRecipe : GT6RecipeMaps.IMPLOSION.mRecipeList) {
			assertEquals(3, tRecipe.mInputs.length, "every row carries dust + TNT + selector");
			assertEquals(1, tRecipe.mOutputs.length, "every row carries one output");
			assertEquals(0, tRecipe.mEUt, "the :711-714 rows are eUt 0 (EUt0/256t 直译)");
			assertEquals(256, tRecipe.mDuration, "the :711-714 rows are duration 256");
		}
	}

	@Test
	void tntCountsAreTheVanillaBranchVerbatim() {
		GT6RecipesImplosion.sMaterialItemResolver = FIXTURE_RESOLVER;
		GT6RecipesImplosion.sTntResolver = () -> Items.TNT;
		GT6RecipesImplosion.sCircuitResolver = GT6RecipesImplosionTest::circuit;
		GT6RecipesImplosion.load();
		long tEights = GT6RecipeMaps.IMPLOSION.mRecipeList.stream().filter(r -> r.mInputs[1].getCount() == 8).count();
		long tThirtyTwos = GT6RecipeMaps.IMPLOSION.mRecipeList.stream().filter(r -> r.mInputs[1].getCount() == 32).count();
		long tSixtyFours = GT6RecipeMaps.IMPLOSION.mRecipeList.stream().filter(r -> r.mInputs[1].getCount() == 64).count();
		int tRowCount = GT6RecipesImplosion.table().size();
		assertEquals(tRowCount * 2, tEights, "tiers :711/:712 ride 8 TNT each");
		assertEquals(tRowCount, tThirtyTwos, "tier :713 rides 32 TNT (ST.mul(4, count-8))");
		assertEquals(tRowCount, tSixtyFours, "tier :714 rides 64 TNT (ST.mul(8, count-8))");
	}

	// ------------------------------------------------------------------
	// the machine-shape lookup: selector routing + never-consumed
	// ------------------------------------------------------------------

	/** Pours the fixture set once and hands the map. */
	private gregtech6.recipes.RecipeMap pour() {
		GT6RecipesImplosion.sMaterialItemResolver = FIXTURE_RESOLVER;
		GT6RecipesImplosion.sTntResolver = () -> Items.TNT;
		GT6RecipesImplosion.sCircuitResolver = GT6RecipesImplosionTest::circuit;
		GT6RecipesImplosion.load();
		return GT6RecipeMaps.IMPLOSION;
	}

	private static ItemStack dust(int aCount) { return new ItemStack(Items.CLAY_BALL, aCount); }
	private static ItemStack tnt(int aCount) { return new ItemStack(Items.TNT, aCount); }

	@Test
	void selectorRoutesTheFourTiers() {
		gregtech6.recipes.RecipeMap tMap = pour();
		// tag(0) — dust1 + TNT8 → the plateGem stand-in (BRICK)
		Recipe t0 = tMap.findRecipe(null, Long.MAX_VALUE, null, null, dust(1), tnt(8), circuit(0));
		assertNotNull(t0, "config 0 finds the :711 arm");
		assertEquals(Items.BRICK, t0.mOutputs[0].getItem(), "the plateGem arm outputs the plateGem stand-in");
		// tag(1) — dust1 + TNT8 → the gem stand-in (IRON_INGOT)
		Recipe t1 = tMap.findRecipe(null, Long.MAX_VALUE, null, null, dust(1), tnt(8), circuit(1));
		assertNotNull(t1, "config 1 finds the :712 arm");
		assertEquals(Items.IRON_INGOT, t1.mOutputs[0].getItem());
		// tag(2) — dust2 + TNT32 → the gemFlawless stand-in (GOLD_INGOT)
		Recipe t2 = tMap.findRecipe(null, Long.MAX_VALUE, null, null, dust(2), tnt(32), circuit(2));
		assertNotNull(t2, "config 2 finds the :713 arm");
		assertEquals(Items.GOLD_INGOT, t2.mOutputs[0].getItem());
		// tag(3) — dust4 + TNT64 → the gemExquisite stand-in (DIAMOND)
		Recipe t3 = tMap.findRecipe(null, Long.MAX_VALUE, null, null, dust(4), tnt(64), circuit(3));
		assertNotNull(t3, "config 3 finds the :714 arm");
		assertEquals(Items.DIAMOND, t3.mOutputs[0].getItem());
	}

	@Test
	void wrongConfigMatchesNothingAndMissingSelectorFailsTheMinimum() {
		gregtech6.recipes.RecipeMap tMap = pour();
		// a non-row configuration matches nothing (the Distillery config-routing arm)
		assertNull(tMap.findRecipe(null, Long.MAX_VALUE, null, null, dust(1), tnt(8), circuit(9)),
				"config 9 is no Implosion arm");
		// two inputs fail the map's mMinimalInputItems = 3 gate (the "dust + TNT but no selector" CUT arm)
		assertNull(tMap.findRecipe(null, Long.MAX_VALUE, null, null, dust(1), tnt(8)),
				"dust + TNT without the selector fails the 3-item minimum");
		// the TIER discipline rides the CONSUME stage, not the probe: the findRecipe probe
		// ignores stack sizes (the upstream :487 form — isRecipeInputEqual(F, T, ...)), so
		// the tier-2 stack with config 0 PROBES as the tier-0 arm, but the size-checking
		// consume (aDontCheckStackSizes = F) refuses the config-0 counts... by the tier-2
		// surplus. What the probe CANNOT do is route config 0 to the tier-2 recipe — the
		// selector Damage tag is exact-matched (isSameItemAndTag).
		Recipe tProbed = tMap.findRecipe(null, Long.MAX_VALUE, null, null, dust(2), tnt(32), circuit(0));
		assertNotNull(tProbed, "the probe ignores sizes — the tier-0 arm matches the bigger stack");
		assertEquals(Items.BRICK, tProbed.mOutputs[0].getItem(), "the probe routed to the CONFIG-0 arm (plateGem), not the tier-2 arm");
		// ...and the config-2 tag on the small stack routes to the tier-2 arm, whose consume then fails short
		Recipe tSmall = tMap.findRecipe(null, Long.MAX_VALUE, null, null, dust(1), tnt(8), circuit(2));
		assertNotNull(tSmall, "the config-2 tag routes to the :713 arm even on the smaller stack");
		assertFalse(tSmall.isRecipeInputEqual(true, false, null, dust(1), tnt(8), circuit(2)),
				"the tier-2 consume refuses a 1-dust/8-TNT stack (the counts enforce the tier)");
	}

	@Test
	void theSelectorIsNeverConsumed() {
		gregtech6.recipes.RecipeMap tMap = pour();
		ItemStack tDust = dust(1), tTnt = tnt(8), tSelector = circuit(0);
		Recipe tRecipe = tMap.findRecipe(null, Long.MAX_VALUE, null, null, tDust, tTnt, tSelector);
		assertNotNull(tRecipe);
		// the consume pass shrinks the dust and the TNT, never the selector
		assertTrue(tRecipe.isRecipeInputEqual(true, false, null, tDust, tTnt, tSelector), "the consume succeeds");
		assertEquals(0, tDust.getCount(), "the dust is consumed");
		assertEquals(0, tTnt.getCount(), "the TNT is consumed");
		assertEquals(1, tSelector.getCount(), "the selector survives (the ST.tag size-0 upstream semantic)");
	}

	@Test
	void theSecondConsumeFailsOnSpentInputs() {
		gregtech6.recipes.RecipeMap tMap = pour();
		ItemStack tDust = dust(1), tTnt = tnt(8), tSelector = circuit(0);
		Recipe tRecipe = tMap.findRecipe(null, Long.MAX_VALUE, null, null, tDust, tTnt, tSelector);
		assertNotNull(tRecipe);
		assertTrue(tRecipe.isRecipeInputEqual(true, false, null, tDust, tTnt, tSelector), "first consume succeeds");
		assertFalse(tRecipe.isRecipeInputEqual(true, false, null, tDust, tTnt, tSelector),
				"the spent dust/TNT refuse a second consume (the TNT is a REAL consumable)");
		assertEquals(1, tSelector.getCount(), "the selector is still unconsumed after both passes");
	}
}
