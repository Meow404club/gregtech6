/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3, or (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see http://www.gnu.org/licenses/lgpl-3.0.txt
 */

package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.server.Bootstrap;

import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.OP;
import gregtech6.registry.GT6Foods;
import gregtech6.registry.GTMaterialItems;

/**
 * The meat/fish listener-walk pour (task food-meat-recipes) — the GT6RecipesBeesTest
 * posture with a DISTINCT synthetic item per member id (the census reads input/output
 * identities, so one shared stand-in cannot do). The can families and the empty can ride
 * the shared GT6RecipesCanner seams (the pour reuses {@code foodCanRow}, so the test arms
 * the same legs the Canner test does).
 */
public class GT6RecipesMeatTest extends GTRecipesOfflineTestBase {

	/** The 28 canned member ids + the bath output — one distinct vanilla item each (identity-only mechanics). */
	private static final Map<String, Item> SYNTHETIC_FOODS = Map.ofEntries(
			Map.entry("minecraft:cod"            , Items.LEATHER),
			Map.entry("minecraft:salmon"         , Items.RABBIT_HIDE),
			Map.entry("minecraft:tropical_fish"  , Items.FEATHER),
			Map.entry("minecraft:pufferfish"     , Items.STRING),
			Map.entry("minecraft:cooked_cod"     , Items.BONE),
			Map.entry("minecraft:cooked_salmon"  , Items.INK_SAC),
			Map.entry("gt6:food_rib_raw"         , Items.BLAZE_POWDER),
			Map.entry("gt6:food_rib_cooked"      , Items.SUGAR),
			Map.entry("gt6:food_ribeyesteak_raw" , Items.OBSIDIAN),
			Map.entry("gt6:food_ribeyesteak_cooked", Items.CRYING_OBSIDIAN),
			Map.entry("minecraft:beef"           , Items.EGG),
			Map.entry("minecraft:cooked_beef"    , Items.SLIME_BALL),
			Map.entry("minecraft:chicken"        , Items.CLAY_BALL),
			Map.entry("minecraft:cooked_chicken" , Items.BRICK),
			Map.entry("gt6:food_mutton_raw"      , Items.FLINT),
			Map.entry("gt6:food_mutton_cooked"   , Items.COAL),
			Map.entry("minecraft:porkchop"       , Items.CHARCOAL),
			Map.entry("minecraft:cooked_porkchop", Items.REDSTONE),
			Map.entry("gt6:food_ham_raw"         , Items.LAPIS_LAZULI),
			Map.entry("gt6:food_ham_cooked"      , Items.QUARTZ),
			Map.entry("gt6:food_horse_raw"       , Items.AMETHYST_SHARD),
			Map.entry("gt6:food_horse_cooked"    , Items.EMERALD),
			Map.entry("gt6:food_mule_raw"        , Items.DIAMOND),
			Map.entry("gt6:food_mule_cooked"     , Items.GOLD_NUGGET),
			Map.entry("gt6:food_donkey_raw"      , Items.IRON_NUGGET),
			Map.entry("gt6:food_donkey_cooked"   , Items.HONEYCOMB),
			Map.entry("gt6:food_dogmeat_raw"     , Items.WHEAT_SEEDS),
			Map.entry("gt6:food_dogmeat_cooked"  , Items.PUMPKIN_SEEDS),
			Map.entry("gt6:food_bacon_raw"       , Items.MELON_SEEDS),
			Map.entry("gt6:food_bacon_cooked"    , Items.SWEET_BERRIES),
			Map.entry("gt6:food_rib_bbq"         , Items.GLOW_BERRIES));

	private static final Item[] SYNTHETIC_MEAT_CANS = {
			Items.GLOWSTONE_DUST, Items.GHAST_TEAR, Items.FERMENTED_SPIDER_EYE, Items.POPPED_CHORUS_FRUIT, Items.NETHER_WART, Items.PRISMARINE_SHARD};
	private static final Item[] SYNTHETIC_FISH_CANS = {
			Items.COCOA_BEANS, Items.BAMBOO, Items.KELP, Items.CACTUS, Items.DRIED_KELP, Items.CHORUS_FRUIT};

	/** The mortar output counts per member (the dustLadder transcription of the amount column). */
	private static final Map<String, Integer> EXPECTED_DUST_COUNTS = Map.ofEntries(
			Map.entry("gt6:food_ham_raw"        , 2),
			Map.entry("gt6:food_bacon_raw"      , 2), // U2 → dustSmall x2
			Map.entry("gt6:food_rib_raw"        , 2),
			Map.entry("gt6:food_ribeyesteak_raw", 3),
			Map.entry("gt6:food_dogmeat_raw"    , 2),
			Map.entry("gt6:food_mutton_raw"     , 2),
			Map.entry("gt6:food_horse_raw"      , 2),
			Map.entry("gt6:food_mule_raw"       , 10), // 5*U2 → dustSmall x10 (the OM.java:464 ladder arm)
			Map.entry("gt6:food_donkey_raw"     , 10),
			Map.entry("minecraft:porkchop"      , 2),
			Map.entry("minecraft:beef"          , 2),
			Map.entry("minecraft:chicken"       , 2),
			Map.entry("minecraft:cod"           , 2),
			Map.entry("minecraft:salmon"        , 2),
			Map.entry("minecraft:tropical_fish" , 2),
			Map.entry("minecraft:pufferfish"    , 1));

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

	/** Arms the synthetic seams (every test drives the same fixture posture). */
	@BeforeEach
	void armFixtures() {
		GT6RecipesMeat.sFoodItemResolver = SYNTHETIC_FOODS::get;
		GT6RecipesMeat.sDustItemResolver = (aPrefix, aMaterial) -> Items.GUNPOWDER;
		GT6RecipesMeat.sBbqSauceResolver = () -> Fluids.WATER;
		GT6RecipesCanner.sFoodCanEmptyResolver = () -> new ItemStack(Items.PAPER, 1);
		GT6RecipesCanner.sMeatCansResolver = aTier -> new ItemStack(SYNTHETIC_MEAT_CANS[aTier], 1);
		GT6RecipesCanner.sFishCansResolver = aTier -> new ItemStack(SYNTHETIC_FISH_CANS[aTier], 1);
		GT6RecipesMeat.resetForTest();
	}

	/** Restores the live defaults verbatim (the Canner test form). */
	@AfterEach
	void restoreSeams() {
		GT6RecipesMeat.sFoodItemResolver = GT6RecipesMeat::resolveFoodItem;
		GT6RecipesMeat.sDustItemResolver = GT6RecipesMixer::resolveItem;
		GT6RecipesMeat.sBbqSauceResolver = () -> gregtech6.fluid.GTFluids.BBQSAUCE.source.get();
		GT6RecipesCanner.sFoodCanEmptyResolver = () -> new ItemStack(gregtech6.registry.GT6FoodCans.FOOD_CAN_EMPTY.get());
		GT6RecipesCanner.sMeatCansResolver = aTier -> GT6RecipesCanner.liveFamilyCan(() -> new ItemStack(gregtech6.registry.GT6FoodCans.FOOD_CAN_MEAT.get(aTier).get()));
		GT6RecipesCanner.sFishCansResolver = aTier -> GT6RecipesCanner.liveFamilyCan(() -> new ItemStack(gregtech6.registry.GT6FoodCans.FOOD_CAN_FISH.get(aTier).get()));
		GT6RecipeMaps.reset();
		GT6RecipesMeat.resetForTest();
	}

	// ------------------------------------------------------------------
	// the canned face (:404-:514, 28 rows into CANNER)
	// ------------------------------------------------------------------

	/** The canned census: 30 rows, 6 fish + 24 meat (the bidirectional-exact declared universe). */
	@Test
	void cannedFacePoursThirtyRows() {
		GT6RecipesMeat.load();
		assertEquals(30, GT6RecipeMaps.CANNER.mRecipeList.size(),
				"the :404-:514 family walk — 12 family listeners over the declared port members");
		long tFish = GT6RecipeMaps.CANNER.mRecipeList.stream()
				.filter(r -> r.mOutputs.length == 1 && isOneOf(r.mOutputs[0].getItem(), SYNTHETIC_FISH_CANS)).count();
		long tMeat = GT6RecipeMaps.CANNER.mRecipeList.stream()
				.filter(r -> r.mOutputs.length == 1 && isOneOf(r.mOutputs[0].getItem(), SYNTHETIC_MEAT_CANS)).count();
		assertEquals(6, tFish, "the :404/:417 fish families — 4 raw + 2 cooked");
		assertEquals(24, tMeat, "the meat families :423-:509 (the beef family carries the ribeye pair)");
	}

	/** The nutrition columns match GT6Foods.FOOD_ROWS for every gt6 member (the coherence face). */
	@Test
	void nutritionColumnsMatchTheFoodRows() {
		for (GT6RecipesMeat.CannedMember tMember : GT6RecipesMeat.CANNED_MEMBERS) {
			if (!tMember.itemId().startsWith("gt6:")) continue;
			String tId = tMember.itemId().substring(4);
			GT6Foods.FoodRow tRow = GT6Foods.FOOD_ROWS.stream().filter(r -> r.id().equals(tId)).findFirst().orElse(null);
			assertNotNull(tRow, tMember.note() + ": the member rides a registered FoodRow");
			assertEquals(tRow.nutrition(), tMember.nutrition(), tMember.note() + ": the ST.food transcription matches the FoodRow");
		}
		// the reverse direction: the oredict-less FOOD_ROWS meats stay OUT of the walk
		List<String> tExpectedAbsent = List.of("food_ham_slice_raw", "food_ham_slice_cooked", "food_rib_bbq", "food_chum", "food_chum_on_stick");
		for (GT6Foods.FoodRow tRow : GT6Foods.FOOD_ROWS) {
			boolean tIsWalkMember = GT6RecipesMeat.CANNED_MEMBERS.stream().anyMatch(m -> m.itemId().equals("gt6:" + tRow.id()));
			if (tRow.id().startsWith("food_ham") || tRow.id().startsWith("food_rib") || tRow.id().startsWith("food_bacon")
					|| tRow.id().startsWith("food_ribeyesteak") || tRow.id().startsWith("food_dogmeat") || tRow.id().startsWith("food_mutton")
					|| tRow.id().startsWith("food_horse") || tRow.id().startsWith("food_mule") || tRow.id().startsWith("food_donkey")
					|| tRow.id().startsWith("food_chum")) {
				if (tExpectedAbsent.contains(tRow.id())) assertFalse(tIsWalkMember, tRow.id() + ": the oredict-less row stays out");
				else assertTrue(tIsWalkMember, tRow.id() + ": the listed meat row must walk");
			}
		}
	}

	/** The canned row shape verbatim (:404 — buffered T, EUt 16, duration 16, one empty can, the tier dispatch). */
	@Test
	void cannedRowShapesFollowTheTierDispatch() {
		GT6RecipesMeat.load();
		// cod: nutrition 2 → switch(1) → {1, 0} tiny fish can
		Recipe tCod = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, null,
				new ItemStack(SYNTHETIC_FOODS.get("minecraft:cod"), 1), new ItemStack(Items.PAPER, 1));
		assertNotNull(tCod, "the cod row resolves for (fish, empty can)");
		assertTrue(tCod.mCanBeBuffered, "RM.food_can → addRecipe2(T, ...) — buffered");
		assertEquals(16, tCod.mEUt, "EUt 16 — CONSTANT");
		assertEquals(16, tCod.mDuration, "duration 16 — CONSTANT");
		assertEquals(1, tCod.mInputs[1].getCount(), "foodValue 2 → ONE empty can");
		assertSame(SYNTHETIC_FISH_CANS[0], tCod.mOutputs[0].getItem(), "the tiny fish can");
		// cooked beef: nutrition 8 → switch(4) → {1, 3} medium-tier meat can
		Recipe tBeef = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, null,
				new ItemStack(SYNTHETIC_FOODS.get("minecraft:cooked_beef"), 1), new ItemStack(Items.PAPER, 1));
		assertNotNull(tBeef, "the cooked beef row resolves");
		assertSame(SYNTHETIC_MEAT_CANS[3], tBeef.mOutputs[0].getItem(), "nutrition 8 → tier 3");
		// bacon raw: nutrition 1 → Math.max(1, 1) → switch(0) → {1, 0} tiny
		Recipe tBacon = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, null,
				new ItemStack(SYNTHETIC_FOODS.get("gt6:food_bacon_raw"), 1), new ItemStack(Items.PAPER, 1));
		assertNotNull(tBacon, "the bacon row resolves");
		assertSame(SYNTHETIC_MEAT_CANS[0], tBacon.mOutputs[0].getItem(), "nutrition 1 → the tiny tier");
	}

	// ------------------------------------------------------------------
	// the Fermenter face (:354/:357, 12 rows) and the Mortar face (:364/:384, 16 rows)
	// ------------------------------------------------------------------

	@Test
	void fermenterFacePoursTwelveRottenFleshRows() {
		GT6RecipesMeat.load();
		assertEquals(12, GT6RecipeMaps.FERMENTER.mRecipeList.size(), "the :350 meatraw face — fish excluded by the :351 guard");
		for (Recipe tRow : GT6RecipeMaps.FERMENTER.mRecipeList) {
			assertTrue(tRow.mCanBeBuffered, "addRecipe1(T, ...) — buffered");
			assertEquals(16, tRow.mEUt, "EUt 16 verbatim");
			assertEquals(288, tRow.mDuration, "duration 288 verbatim");
			assertEquals(0, tRow.mFluidInputs.length + tRow.mFluidOutputs.length, "the item-only spoilage row");
			assertEquals(Items.ROTTEN_FLESH, tRow.mOutputs[0].getItem(), "the rotten flesh output");
		}
	}

	@Test
	void mortarFacePoursSixteenLadderRows() {
		GT6RecipesMeat.load();
		assertEquals(16, GT6RecipeMaps.MORTAR.mRecipeList.size(), "12 raw meats (:364) + 4 raw fish (:384)");
		for (Recipe tRow : GT6RecipeMaps.MORTAR.mRecipeList) {
			assertTrue(tRow.mCanBeBuffered, "addRecipe1(T, ...) — buffered");
			assertEquals(16, tRow.mEUt, "EUt 16 verbatim");
			assertEquals(16, tRow.mDuration, "duration 16 verbatim");
			Item tInput = tRow.mInputs[0].getItem();
			Integer tExpected = null;
			for (Map.Entry<String, Item> tEntry : SYNTHETIC_FOODS.entrySet())
				if (tEntry.getValue() == tInput) tExpected = EXPECTED_DUST_COUNTS.get(tEntry.getKey());
			assertNotNull(tExpected, "the input is a declared raw member");
			assertEquals(tExpected.intValue(), tRow.mOutputs[0].getCount(), "the dustLadder count for the member's meat amount");
		}
	}

	/** The ladder itself — the OM.java:460-468 transcription spot checks. */
	@Test
	void dustLadderIsTheOmTranscription() {
		assertEquals(new GT6RecipesMeat.DustOut(OP.blockDust, 8), GT6RecipesMeat.dustLadder(72 * CS.U), "72U → blockDust x8");
		assertEquals(new GT6RecipesMeat.DustOut(OP.dust, 2), GT6RecipesMeat.dustLadder(CS.U * 2), "U*2 → dust x2");
		assertEquals(new GT6RecipesMeat.DustOut(OP.dust, 3), GT6RecipesMeat.dustLadder(CS.U * 3), "U*3 → dust x3");
		assertEquals(new GT6RecipesMeat.DustOut(OP.dust, 1), GT6RecipesMeat.dustLadder(CS.U), "U → dust x1");
		assertEquals(new GT6RecipesMeat.DustOut(OP.dustSmall, 2), GT6RecipesMeat.dustLadder(CS.U2), "U2 → dustSmall x2");
		assertEquals(new GT6RecipesMeat.DustOut(OP.dustSmall, 10), GT6RecipesMeat.dustLadder(5 * CS.U2), "5*U2 → dustSmall x10");
		assertEquals(new GT6RecipesMeat.DustOut(OP.dustDiv72, 1), GT6RecipesMeat.dustLadder(CS.U72), "U72 → dustDiv72 x1");
		assertNull(GT6RecipesMeat.dustLadder(0), "sub-U72 → null (the upstream mat() null face)");
	}

	// ------------------------------------------------------------------
	// the Bath face (:430, 1 row)
	// ------------------------------------------------------------------

	@Test
	void bathRowIsTheUpstreamRibBbqLine() {
		GT6RecipesMeat.load();
		assertEquals(1, GT6RecipeMaps.BATH.mRecipeList.size(), "the single ribcooked member");
		Recipe tRow = GT6RecipeMaps.BATH.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 250)},
				new ItemStack(SYNTHETIC_FOODS.get("gt6:food_rib_cooked"), 1));
		assertNotNull(tRow, "the :430 row resolves for (rib cooked, 250 mB sauce)");
		assertTrue(tRow.mCanBeBuffered, "addRecipe1(T, ...) — buffered");
		assertEquals(0, tRow.mEUt, "EUt 0 — the Bath is energy-free");
		assertEquals(16, tRow.mDuration, "duration 16 verbatim");
		assertEquals(250, tRow.mFluidInputs[0].getAmount(), "FL.Sauce_BBQ.make(250)");
		assertEquals(0, tRow.mFluidOutputs.length, "NF — no fluid output");
		assertSame(Items.GLOW_BERRIES, tRow.mOutputs[0].getItem(), "the Barbecue Ribs output");
	}

	// ------------------------------------------------------------------
	// the generation discipline
	// ------------------------------------------------------------------

	@Test
	void pourIsIdempotentPerGeneration() {
		GT6RecipesMeat.load();
		int tAfterFirst = GT6RecipeMaps.CANNER.mRecipeList.size()
				+ GT6RecipeMaps.FERMENTER.mRecipeList.size()
				+ GT6RecipeMaps.MORTAR.mRecipeList.size()
				+ GT6RecipeMaps.BATH.mRecipeList.size();
		assertEquals(59, tAfterFirst, "30 canned + 12 fermenter + 16 mortar + 1 bath");
		GT6RecipesMeat.load();
		assertEquals(tAfterFirst, GT6RecipeMaps.CANNER.mRecipeList.size()
				+ GT6RecipeMaps.FERMENTER.mRecipeList.size()
				+ GT6RecipeMaps.MORTAR.mRecipeList.size()
				+ GT6RecipeMaps.BATH.mRecipeList.size(), "the second load() is a no-op (the generation flag)");
	}

	private static boolean isOneOf(Item aItem, Item[] aItems) {
		for (Item tItem : aItems) if (tItem == aItem) return true;
		return false;
	}
}
