package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GT6BeeCombs;
import gregtech6.registry.GTMaterialItems;

/**
 * The bee-comb pour offline acceptance (task p31-bees-lv1): the 20-row centrifuge
 * transcription (MultiItemFood.java:251-270 — eUt 16, duration 64, item chances verbatim,
 * the FR-propolis tails CUT with their chances), the 20-row squeezer generalization
 * (Loader_Recipes_Food.java:264 — the materialHoneycomb listener body per comb, the
 * declared port deviation), and the pour census 20+20 loaded / 0 skipped (every output
 * fluid/item resolves — the seven dependency fluids are this card's registration face).
 * The live loop is the RCON chain (offline cannot touch the Forge registries).
 */
public class GT6RecipesBeesTest extends GTRecipesOfflineTestBase {

	/** The upstream :251-270 row order — the comb snake fragments. */
	private static final List<String> CENTRIFUGE_COMBS = List.of(
			"honey", "water", "magic", "nether", "end", "rock", "jungle", "frozen", "shroom", "sandy",
			"clay", "sticky", "royal", "soul", "amnesic", "military", "pyro", "cryo", "aero", "tera");

	/**
	 * The offline fixture universe: the vanilla HONEYCOMB item stands in for every comb and
	 * the CLAY_BALL for every material item (new Items cannot be created offline — the
	 * GT6RecipesShCLTest synthetic-item convention); the vanilla outputs dereference their
	 * REAL items (the vanilla registry is bootstrapped); every fluid id resolves to the
	 * vanilla water (the GTEngineFuelsTest WATER_FIXTURE convention — the mechanics only
	 * compare identities).
	 */
	private static final java.util.function.Function<String, Fluid> FIXTURE_FLUIDS = aId -> Fluids.WATER;

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GTMaterialItems.initMaterials(); // the offline material refill (the registration-test convention)
	}

	@AfterEach
	void resetGeneration() {
		GT6RecipeMaps.reset();
		GT6RecipesBees.resetForTest();
	}

	// ------------------------------------------------------------------
	// the transcription walk — the upstream :251-270 columns
	// ------------------------------------------------------------------

	@Test
	void tableTranscribesTheTwentyUpstreamRows() {
		assertEquals(CENTRIFUGE_COMBS, GT6RecipesBees.centrifugeTable().stream().map(GT6RecipesBees.BeeRow::comb).toList(),
				"the 20 centrifuge rows in the upstream :251-270 order");
		for (GT6RecipesBees.BeeRow tRow : GT6RecipesBees.centrifugeTable()) {
			assertTrue(!tRow.fluidOuts().isEmpty(), tRow.note() + ": every comb row carries a fluid output");
			for (GT6RecipesBees.FluidLeg tLeg : tRow.fluidOuts()) assertTrue(tLeg.amount() > 0, tRow.note() + ": positive fluid amount");
			assertNotNull(tRow.chances(), tRow.note() + ": the centrifuge form carries the chances array");
			assertTrue(tRow.chances().length >= tRow.outputs().length,
					tRow.note() + ": the item-aligned chances cover every item output");
		}
	}

	@Test
	void theSpotChecksMatchTheUpstreamCells() {
		// :251 honey — 100 L, wax-only after the propolis cut
		GT6RecipesBees.BeeRow tHoney = row("honey");
		assertEquals(100, tHoney.fluidOuts().get(0).amount(), ":251 the verbatim honey litres");
		assertEquals(1, tHoney.outputs().length, ":251 the FR_Propolis tail CUT → one item output");
		assertEquals(1, tHoney.chances().length, ":251 the chances prefix-trim");

		// :255 end — 125 L dragon breath, endstone dust + chorus (the pulsating propolis CUT)
		GT6RecipesBees.BeeRow tEnd = row("end");
		assertEquals("dragon_breath", tEnd.fluidOuts().get(0).fluid(), ":255 the dragon breath output");
		assertEquals(125, tEnd.fluidOuts().get(0).amount(), ":255 the verbatim litres");
		assertEquals(2, tEnd.outputs().length, ":255 the endstone + chorus outputs");

		// :261 clay — six 20% dust outputs (upstream {2000 x6})
		GT6RecipesBees.BeeRow tClay = row("clay");
		assertEquals(6, tClay.outputs().length, ":261 the six dust outputs");
		assertEquals(6, tClay.chances().length, ":261 the six chances");
		for (long tChance : tClay.chances()) assertEquals(2000, tChance, ":261 the 20% chance");

		// :263 royal — the TWO fluid legs: honey 50 AND royal jelly 10 (the :263 FL.array form)
		GT6RecipesBees.BeeRow tRoyal = row("royal");
		assertEquals(2, tRoyal.fluidOuts().size(), ":263 the honey + royal jelly pair");
		assertEquals("honey", tRoyal.fluidOuts().get(0).fluid(), ":263 the honey leg");
		assertEquals(50, tRoyal.fluidOuts().get(0).amount(), ":263 the verbatim 50 L");
		assertEquals("royal_jelly", tRoyal.fluidOuts().get(1).fluid(), ":263 the royal jelly leg");
		assertEquals(10, tRoyal.fluidOuts().get(1).amount(), ":263 the verbatim 10 L");
		assertEquals(1, tRoyal.outputs().length, ":263 the wax dust item leg");

		// :266 military — four chances over four item outputs
		GT6RecipesBees.BeeRow tMilitary = row("military");
		assertEquals(4, tMilitary.outputs().length, ":266 bone dust/bone/rotten flesh/spider eye");
		assertEquals(4, tMilitary.chances().length, ":266 {10000, 500, 500, 250}");
		assertEquals(10000, tMilitary.chances()[0], ":266 the dust at 100%");
		assertEquals(250, tMilitary.chances()[3], ":266 the spider eye at 2.5%");

		// :269 aero — three item outputs (the Breeze stick kept — the port MT has Breeze)
		assertEquals(3, row("aero").outputs().length, ":269 the blitz dust + blitz stick + breeze stick");
	}

	/** The :263 RoyalJelly second fluid leg lands in the recipe as a two-slot fluid output array. */
	@Test
	void theRoyalRowPoursTheSecondFluidLeg() {
		GT6RecipesBees.sCombResolver = aName -> Items.HONEYCOMB;
		GT6RecipesBees.sFluidResolver = FIXTURE_FLUIDS;
		GT6RecipesBees.sMaterialItemResolver = (aPrefix, aMaterial) -> Items.CLAY_BALL;
		GT6RecipesBees.load();
		Recipe tRoyal = GT6RecipeMaps.CENTRIFUGE.mRecipeList.stream()
				.filter(r -> r.mFluidOutputs.length == 2).findFirst().orElse(null);
		assertNotNull(tRoyal, ":263 the royal row carries the two fluid outputs (honey 50 + royal jelly 10)");
		assertEquals(50, tRoyal.mFluidOutputs[0].getAmount(), ":263 the honey leg");
		assertEquals(10, tRoyal.mFluidOutputs[1].getAmount(), ":263 the royal jelly leg");
	}

	// ------------------------------------------------------------------
	// the pour: 20 + 20 rows land, zero skips
	// ------------------------------------------------------------------

	@Test
	void loadPoursTwentyRowsIntoEachMap() {
		GT6RecipesBees.sCombResolver = aName -> Items.HONEYCOMB;
		GT6RecipesBees.sFluidResolver = FIXTURE_FLUIDS;
		GT6RecipesBees.sMaterialItemResolver = (aPrefix, aMaterial) -> Items.CLAY_BALL;
		GT6RecipesBees.load();
		assertEquals(20, GT6RecipeMaps.CENTRIFUGE.mRecipeList.size(), "the 20 comb rows pour into CENTRIFUGE");
		assertEquals(20, GT6RecipeMaps.SQUEEZER.mRecipeList.size(), "the 20 generalization rows pour into SQUEEZER");

		for (Recipe tRecipe : GT6RecipeMaps.CENTRIFUGE.mRecipeList) {
			assertEquals(1, tRecipe.mInputs.length, "one comb input");
			assertEquals(1, tRecipe.mInputs[0].getCount(), "count 1");
			assertTrue(tRecipe.mFluidOutputs.length >= 1, "at least one fluid output leg");
			assertEquals(16, tRecipe.mEUt, "the verbatim eUt 16");
			assertEquals(64, tRecipe.mDuration, "the verbatim duration 64");
			assertTrue(tRecipe.mCanBeBuffered, "the addRecipe1(T, ...) buffered shape");
		}
		for (Recipe tRecipe : GT6RecipeMaps.SQUEEZER.mRecipeList) {
			// the Listener:264 body — 90 L honey + wax dust, NO chances array (every output 100%)
			assertEquals(1, tRecipe.mInputs.length);
			assertEquals(1, tRecipe.mFluidOutputs.length);
			assertEquals(1, tRecipe.mOutputs.length, "the wax dust");
			assertEquals(90, tRecipe.mFluidOutputs[0].getAmount(), "the verbatim 90 L");
			assertNull(tRecipe.mChances, "the listener form has no chances array");
			assertEquals(16, tRecipe.mEUt, "the verbatim eUt 16");
			assertEquals(64, tRecipe.mDuration, "the verbatim duration 64");
		}

		GT6RecipesBees.load(); // idempotent: the second load is a no-op
		assertEquals(20, GT6RecipeMaps.CENTRIFUGE.mRecipeList.size(), "load() is one pour per generation");
		assertEquals(20, GT6RecipeMaps.SQUEEZER.mRecipeList.size(), "load() is one pour per generation");
	}

	/** The squeezer generalization covers ALL 20 combs (the declared listener-expansion deviation). */
	@Test
	void theSqueezerTableCoversEveryComb() {
		assertEquals(GT6BeeCombs.COMB_SPECS.size(), GT6RecipesBees.squeezerTable().size());
		for (int i = 0; i < GT6BeeCombs.COMB_SPECS.size(); i++) {
			assertEquals(GT6BeeCombs.COMB_SPECS.get(i).name(), GT6RecipesBees.squeezerTable().get(i).comb(),
					"row " + i + " mirrors the comb declaration order");
			assertEquals("honey", GT6RecipesBees.squeezerTable().get(i).fluidOuts().get(0).fluid(), "every row squeezes honey");
			assertEquals(90, GT6RecipesBees.squeezerTable().get(i).fluidOuts().get(0).amount(), "the verbatim 90 L");
		}
	}

	/** The live fluid resolver's null arm: an unknown id returns null (the silent-skip semantics). */
	@Test
	void theLiveFluidResolverNullArms() {
		assertNull(GT6RecipesBees.resolveFluid("definitely_not_a_fluid"), "unknown ids resolve null");
		assertSame(Fluids.WATER, GT6RecipesBees.resolveFluid("water"), "the vanilla water identity");
	}

	/** The comb registration seam mirrors the upstream meta range order (the GT6BeeCombs walk). */
	@Test
	void theCombSpecsCarryTheUpstreamMetas() {
		assertEquals(30000, GT6BeeCombs.COMB_SPECS.get(0).meta(), ":226 the honey comb meta");
		assertEquals(30100, GT6BeeCombs.COMB_SPECS.get(10).meta(), ":234 the clay comb meta");
		assertEquals(30203, GT6BeeCombs.COMB_SPECS.get(19).meta(), ":247 the tera comb meta");
		assertEquals(20, GT6BeeCombs.COMBS.size(), "20 handles registered off the same table");
	}

	/**
	 * The material-item existence gate for the LIVE pour: every (prefix, material) output
	 * pair of the transcription must resolve in the offline-safe registration walk — the
	 * wax/clay-family dusts, the dustTiny element dusts and the elemental sticks (the
	 * zero-skip acceptance rides this gate; a missing pair would skip its row live).
	 */
	@Test
	void everyMaterialOutputPairGeneratesAnItem() {
		java.util.Set<GTMaterialItems.PrefixMaterial> tOrder =
				new java.util.HashSet<>(GTMaterialItems.registrationOrder());
		Object[][] tPairs = {
			{OP.dust, MT.WaxBee}, {OP.dust, MT.WaxMagic}, {OP.dust, MT.WaxRefractory},
			{OP.dust, MT.WaxSoulful}, {OP.dust, MT.WaxAmnesic},
			{OP.dust, MT.Endstone}, {OP.dust, MT.Stone}, {OP.dust, MT.Clay},
			{OP.dust, MT.ClayBrown}, {OP.dust, MT.ClayRed}, {OP.dust, MT.Bentonite},
			{OP.dust, MT.Palygorskite}, {OP.dust, MT.Kaolinite}, {OP.dust, MT.Cocoa},
			{OP.dust, MT.Ice}, {OP.dust, MT.Bone}, {OP.dust, MT.SoulSand},
			{OP.dustTiny, MT.Blaze}, {OP.dustTiny, MT.Blizz}, {OP.dustTiny, MT.Blitz}, {OP.dustTiny, MT.Basalz},
			{OP.stick, MT.Blaze}, {OP.stick, MT.Blizz}, {OP.stick, MT.Blitz},
			{OP.stick, MT.Breeze}, {OP.stick, MT.Basalz},
		};
		for (Object[] tPair : tPairs) {
			assertTrue(tOrder.contains(new GTMaterialItems.PrefixMaterial((OreDictPrefix)tPair[0], (OreDictMaterial)tPair[1])),
					"the item for " + tPair[1] + " " + tPair[0] + " must generate (the live pour's zero-skip gate)");
		}
	}

	private GT6RecipesBees.BeeRow row(String aComb) {
		return GT6RecipesBees.centrifugeTable().stream().filter(r -> r.comb().equals(aComb)).findFirst().orElseThrow();
	}
}
