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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregapi.data.ANY;
import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The food-tail pour (task pool-drain-food-machine-tail) — the GT6RecipesMeatTest posture:
 * one distinct synthetic item per member id, the shared fixture resolvers, the census
 * ratchets per segment, the TRUE NEGATIVE / declared-null pins, the verbatim spot checks
 * and the id-universe walks (the b1/b2a id-universe pattern over GTMaterialItems +
 * GTMaterialBlocks registrationOrder, plus the GTFluids spec universe for every fluid leg).
 */
public class GT6RecipesFoodTailTest extends GTRecipesOfflineTestBase {

	/** The 24 gt6/vanilla food ids the band resolves — one distinct vanilla item each (identity-only mechanics). */
	private static final Map<String, Item> SYNTHETIC_FOODS = Map.ofEntries(
			Map.entry("gt6:food_cheese"                        , Items.LEATHER),
			Map.entry("gt6:food_cheese_sliced"                 , Items.RABBIT_HIDE),
			Map.entry("gt6:food_brown_egg_boiled"              , Items.FEATHER),
			Map.entry("gt6:food_white_egg_boiled"              , Items.STRING),
			Map.entry("minecraft:egg"                          , Items.EGG),
			Map.entry("gt6:food_white_egg"                     , Items.BONE),
			Map.entry("gt6:food_egg_sliced"                    , Items.INK_SAC),
			Map.entry("gt6:food_egg_white"                     , Items.GLOWSTONE_DUST),
			Map.entry("gt6:food_egg_yolk"                      , Items.GHAST_TEAR),
			Map.entry("gt6:food_egg_scrambled"                 , Items.POPPED_CHORUS_FRUIT),
			Map.entry("gt6:food_dough_egg"                     , Items.NETHER_WART),
			Map.entry("gt6:food_raisins_green"                 , Items.PRISMARINE_SHARD),
			Map.entry("gt6:food_raisins_white"                 , Items.PRISMARINE_CRYSTALS),
			Map.entry("gt6:food_raisins_red"                   , Items.COCOA_BEANS),
			Map.entry("gt6:food_raisins_purple"                , Items.BAMBOO),
			Map.entry("gt6:food_pomeraisins"                   , Items.KELP),
			Map.entry("gt6:food_raisins_chocolate"             , Items.DRIED_KELP),
			Map.entry("gt6:food_ice_cream"                     , Items.CHORUS_FRUIT),
			Map.entry("gt6:food_ice_cream_raisin"              , Items.SWEET_BERRIES),
			Map.entry("gt6:food_dough_sugar"                   , Items.HONEYCOMB),
			Map.entry("gt6:food_dough_sugar_raisins"           , Items.GOLD_NUGGET),
			Map.entry("gt6:food_dough_sugar_chocolate_raisins" , Items.IRON_NUGGET),
			Map.entry("gt6:food_dough_abyssal"                 , Items.QUARTZ),
			Map.entry("gt6:food_chum"                          , Items.AMETHYST_SHARD),
			Map.entry("minecraft:cod"                          , Items.CLAY_BALL),
			Map.entry("minecraft:salmon"                       , Items.BRICK),
			Map.entry("minecraft:tropical_fish"                , Items.FLINT),
			Map.entry("minecraft:pufferfish"                   , Items.COAL));

	/** The census constants of the band (the class-doc ratchet face). */
	private static final int CHEESE_ROWS = 3, EGG_SLICER_ROWS = 2, EGG_MACHINE_ROWS = 6, CHUM_ROWS = 4,
			FISH_OIL_ROWS = 8, RAISIN_ROWS = 10, TWO_NAME_ROWS = 6, SINGLE_ROWS = 7;

	/** The walk sizes (upstream ANY.java:111-112 verbatim wiring: Flour without Rice, Grains + Potato). */
	private static final int FLOUR_WALK = 7, FLOUR_GRAINS_WALK = 8;

	/** The expected pour census per map (fixtures resolve every non-chum leg). */
	private static final Map<GT6RecipesFoodTail.Target, Integer> POUR_CENSUS = Map.of(
			GT6RecipesFoodTail.Target.SHREDDER  , 1,  // :132 cheese
			GT6RecipesFoodTail.Target.MORTAR    , 1,  // :133 cheese
			GT6RecipesFoodTail.Target.SLICER    , 3,  // :130 cheese + :297 x 2 eggs
			GT6RecipesFoodTail.Target.AUTOCLAVE , 2,  // :313 x 2 eggs
			GT6RecipesFoodTail.Target.JUICER    , 6,  // :314 x 2 + :413 x 4 fish
			GT6RecipesFoodTail.Target.CENTRIFUGE, 2,  // :315 x 2 eggs
			GT6RecipesFoodTail.Target.MIXER     , 57, // 5 raisin + 6 two-name + 7 egg dough + 32 meat ingot + 1 mushroom + 1 ambrosia + 1 spoiled + 1 oat + 3 chocolate
			GT6RecipesFoodTail.Target.SQUEEZER  , 4,  // :412 x 4 fish
			GT6RecipesFoodTail.Target.BATH      , 5); // :155 x 5 raisins

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

	/** Arms the synthetic seams on a FRESH generation (the PhaseGate reset form — the shared maps must not carry sibling pours). */
	@BeforeEach
	void armFixtures() {
		GT6RecipeMaps.reset();
		GT6RecipesFoodTail.sFoodItemResolver = SYNTHETIC_FOODS::get;
		GT6RecipesFoodTail.sMaterialItemResolver = (aPrefix, aMaterial) -> Items.GUNPOWDER;
		GT6RecipesFoodTail.sSelectorResolver = aConfig -> new ItemStack(Items.GOLD_NUGGET);
		GT6RecipesFoodTail.sBladeResolver = () -> Items.IRON_SHOVEL;
		GT6RecipesFoodTail.sNamedFluidResolver = aName -> GT6RecipesFoodTail.SLUDGE_NULL.equals(aName) ? null : Fluids.WATER;
		GT6RecipesFoodTail.resetForTest();
	}

	/** Restores the live defaults verbatim (the Meat test form). */
	@AfterEach
	void restoreSeams() {
		GT6RecipesFoodTail.sFoodItemResolver = GT6RecipesFoodTail::resolveFoodItem;
		GT6RecipesFoodTail.sMaterialItemResolver = GT6RecipesFoodTail::resolveItem;
		GT6RecipesFoodTail.sSelectorResolver = GT6RecipesFoodTail::liveSelector;
		GT6RecipesFoodTail.sBladeResolver = () -> GT6RecipesFoodTail.bound(gregtech6.registry.GT6SlicerBlades.SHAPE_SLICER_FLAT);
		GT6RecipesFoodTail.sNamedFluidResolver = GT6RecipesFoodTail::resolveNamedFluid;
		GT6RecipeMaps.reset();
		GT6RecipesFoodTail.resetForTest();
	}

	// ------------------------------------------------------------------
	// the census ratchet
	// ------------------------------------------------------------------

	/** The transcribed-band census: the fixed segments + the two ANY walks, exact. */
	@Test
	void transcribedBandCensusIsExact() {
		List<GT6RecipesFoodTail.Row> tTable = GT6RecipesFoodTail.table();
		int tFixed = CHEESE_ROWS + EGG_SLICER_ROWS + EGG_MACHINE_ROWS + CHUM_ROWS + FISH_OIL_ROWS
				+ RAISIN_ROWS + TWO_NAME_ROWS + SINGLE_ROWS;
		assertEquals(tFixed + FLOUR_WALK + 4 * FLOUR_GRAINS_WALK, tTable.size(),
				"the band = " + tFixed + " fixed rows + the :322 ANY.Flour walk (" + FLOUR_WALK
				+ ") + the :720-:723 ANY.FlourGrains walk (" + FLOUR_GRAINS_WALK + " x 4)");
		// the walk faces live (the crops test posture) and match the pinned constants
		assertEquals(FLOUR_WALK, ANY.Flour.mToThis.size(), "ANY.Flour = 7 (upstream ANY.java:111, no Rice)");
		assertEquals(FLOUR_GRAINS_WALK, ANY.FlourGrains.mToThis.size(), "ANY.FlourGrains = 8 (7 grains + Potato)");
		// per-segment note counts (the notes carry the upstream file:line)
		assertEquals(CHEESE_ROWS, countNotes(tTable, ":13[023]"), "the cheese band :130/:132/:133");
		assertEquals(EGG_SLICER_ROWS, countNotes(tTable, ":297\\[.*"), "the foodBoiledegg band :297");
		assertEquals(EGG_MACHINE_ROWS, countNotes(tTable, ":31[345]\\[.*"), "the egg machine band :313/:314/:315");
		assertEquals(CHUM_ROWS, countNotes(tTable, ":39[4567]"), "the chum quartet :394-:397");
		assertEquals(FISH_OIL_ROWS, countNotes(tTable, ":41[23]\\[.*"), "the fish-oil band :412/:413");
		assertEquals(RAISIN_ROWS, countNotes(tTable, ":15[56]\\[.*"), "the raisin band :155/:156");
		assertEquals(TWO_NAME_ROWS, countNotes(tTable, ":25[59]"), "the two-name band :255/:259");
	}

	/** The pour census with fixtures: every non-chum row resolves, per-map exact. */
	@Test
	void pourCensusIsExactPerMap() {
		GT6RecipesFoodTail.load();
		int tTotal = 0;
		for (Map.Entry<GT6RecipesFoodTail.Target, Integer> tEntry : POUR_CENSUS.entrySet()) {
			assertEquals(tEntry.getValue().intValue(), GT6RecipesFoodTail.recipeMap(tEntry.getKey()).mRecipeList.size(),
					tEntry.getKey() + " pour census");
			tTotal += tEntry.getValue();
		}
		assertEquals(85 - CHUM_ROWS, tTotal, "85 transcribed − 4 chum (both legs declared-null) = 81 poured");
		// the chum quartet skipped with a count (the scrapmeat input + the sludge output)
		assertEquals(CHUM_ROWS, GT6RecipesFoodTail.lastSkipped(GT6RecipesFoodTail.Target.MIXER), "the four chum rows skip");
		// idempotent per generation (the whole map set stays put)
		int tBefore = 0;
		for (GT6RecipesFoodTail.Target tTarget : GT6RecipesFoodTail.Target.values()) tBefore += GT6RecipesFoodTail.recipeMap(tTarget).mRecipeList.size();
		assertEquals(81, tBefore);
		GT6RecipesFoodTail.load();
		int tAfter = 0;
		for (GT6RecipesFoodTail.Target tTarget : GT6RecipesFoodTail.Target.values()) tAfter += GT6RecipesFoodTail.recipeMap(tTarget).mRecipeList.size();
		assertEquals(tBefore, tAfter, "the second load() is a no-op (the generation flag)");
	}

	// ------------------------------------------------------------------
	// the TRUE NEGATIVE / declared-null pins
	// ------------------------------------------------------------------

	/** The :316 Birb generify + :305 itemEggBig faces: no egg member carries amount 4 and no big-egg id exists. */
	@Test
	void theBirbFacesAreDeclaredAbsent() {
		for (GT6RecipesFoodTail.EggMember tEgg : GT6RecipesFoodTail.EGG_MEMBERS) {
			assertEquals(1, tEgg.amount(), tEgg.note() + ": the tAmount=4 itemEggBig face is TRUE NEGATIVE (Birb absent), literal 1");
		}
		assertTrue(GT6RecipesFoodTail.SKIPPED_UPSTREAM.stream().anyMatch(s -> s.contains(":316 Birb generify")),
				"the :316 declaration is in the ledger");
		assertTrue(GT6RecipesFoodTail.SKIPPED_UPSTREAM.stream().anyMatch(s -> s.contains("itemEggBig")),
				"the :305 declaration is in the ledger");
	}

	/** The :373 fish generify + :369-:387 first-listener faces: declared, not poured (no GENERIFIER rows here). */
	@Test
	void theFishGenerifyFaceIsDeclaredAbsent() {
		assertTrue(GT6RecipesFoodTail.SKIPPED_UPSTREAM.stream().anyMatch(s -> s.contains("the FIRST listAllfishraw listener") && s.contains(":373 generify TRUE NEGATIVE")),
				"the :373 declaration is in the ledger");
		// no row targets the (un-built) GENERIFIER map
		for (GT6RecipesFoodTail.Target tTarget : GT6RecipesFoodTail.Target.values()) {
			assertNotEquals("GENERIFIER", tTarget.name(), "no GENERIFIER target exists (the declaration is the closure)");
		}
	}

	/** The FoodsGT.put arms (:135/:391) and the foodVanilla band (:123-128) are declared skips with zero rows. */
	@Test
	void theFoodsGtAndFoodVanillaFacesDeclareZeroRows() {
		assertTrue(GT6RecipesFoodTail.SKIPPED_UPSTREAM.stream().anyMatch(s -> s.contains(":135 FoodsGT.put")), ":135 in the ledger");
		assertTrue(GT6RecipesFoodTail.SKIPPED_UPSTREAM.stream().anyMatch(s -> s.contains(":391 FoodsGT.put")), ":391 in the ledger");
		assertTrue(GT6RecipesFoodTail.SKIPPED_UPSTREAM.stream().anyMatch(s -> s.contains(":123-128 foodVanilla") && s.contains("TRUE NEGATIVE")),
				"the foodVanilla band is a declared TRUE NEGATIVE");
		for (GT6RecipesFoodTail.Row tRow : GT6RecipesFoodTail.table()) {
			assertFalse(tRow.note().startsWith(":125") || tRow.note().startsWith(":126"), "no foodVanilla row exists");
		}
	}

	/** The chum sludge output leg is the declared null: the name resolves null by contract and the rows carry it. */
	@Test
	void theChumSludgeLegIsDeclaredNull() {
		assertTrue(GT6RecipesFoodTail.SKIPPED_UPSTREAM.stream().anyMatch(s -> s.contains("FL.Sludge output leg") && s.contains("DECLARED NULL")),
				"the sludge declaration is in the ledger");
		// the live default contract: sludge resolves null (before any GTFluids walk)
		assertNull(GT6RecipesFoodTail.resolveNamedFluid(GT6RecipesFoodTail.SLUDGE_NULL), "sludge = the declared-null carrier");
		// all four chum rows carry the sludge output leg + the declared-absent scrapmeat input
		int tChum = 0;
		for (GT6RecipesFoodTail.Row tRow : GT6RecipesFoodTail.table()) {
			if (!tRow.note().startsWith(":39")) continue;
			tChum++;
			assertEquals(1, tRow.outFluids().length, tRow.note() + ": exactly the sludge output leg");
			assertEquals(GT6RecipesFoodTail.SLUDGE_NULL, tRow.outFluids()[0].name(), tRow.note() + ": the sludge carrier");
			assertEquals(GT6RecipesFoodTail.SCRAP_MEAT_INPUT, tRow.inItems()[0].foodId(), tRow.note() + ": the declared-absent input");
			// the one valid loop leg: the vanilla poisonous potato (MultiItemFood.java:336 = the vanilla alias)
			assertEquals(Items.POISONOUS_POTATO, tRow.inItems()[4].vanilla().get(), tRow.note() + ": the potato leg is the vanilla native");
		}
		assertEquals(CHUM_ROWS, tChum, "the quartet is transcribed verbatim");
	}

	/** The :734/:736 gemChipped Sugar rows are DROPPED: no row carries the note and the ledger declares it. */
	@Test
	void theGemChippedSugarRowsAreDropped() {
		assertTrue(GT6RecipesFoodTail.SKIPPED_UPSTREAM.stream().anyMatch(s -> s.contains(":734/:736") && s.contains("DROPPED")),
				"the drop declaration is in the ledger");
		for (GT6RecipesFoodTail.Row tRow : GT6RecipesFoodTail.table()) {
			assertFalse(tRow.note().startsWith(":734") || tRow.note().startsWith(":736"), "no gemChipped row exists");
		}
		// the chocolate ladder = the three surviving rows
		assertEquals(3, countNotes(GT6RecipesFoodTail.table(), ":73[357]"), "the :733/:735/:737 trio");
	}

	// ------------------------------------------------------------------
	// the verbatim spot checks (x4+)
	// ------------------------------------------------------------------

	/** The :155 Bath raisin row verbatim: EUt 0, duration 16, chocolate molten 250 mB. */
	@Test
	void theRaisinBathRowIsUpstreamVerbatim() {
		GT6RecipesFoodTail.load();
		Recipe tRow = GT6RecipeMaps.BATH.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 250)},
				new ItemStack(SYNTHETIC_FOODS.get("gt6:food_raisins_green"), 1));
		assertNotNull(tRow, "the :155 row resolves for (raisin, 250 mB molten chocolate)");
		assertEquals(0, tRow.mEUt, "EUt 0 — the Bath is energy-free");
		assertEquals(16, tRow.mDuration, "duration 16 verbatim");
		assertEquals(250, tRow.mFluidInputs[0].getAmount(), "MT.Chocolate.liquid(U4) = 250 mB");
		assertEquals(0, tRow.mFluidOutputs.length, "NF — no fluid output");
		assertSame(Items.DRIED_KELP, tRow.mOutputs[0].getItem(), "food_raisins_chocolate x1");
		assertEquals(1, tRow.mOutputs[0].getCount(), ".get(1)");
	}

	/** The :313 Autoclave egg row verbatim: steam 800 → DistW 5, EUt 0, duration 128, the never-consumed selector. */
	@Test
	void theEggAutoclaveRowIsUpstreamVerbatim() {
		GT6RecipesFoodTail.load();
		Recipe tRow = GT6RecipeMaps.AUTOCLAVE.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 800)},
				new ItemStack(SYNTHETIC_FOODS.get("minecraft:egg"), 1), new ItemStack(Items.GOLD_NUGGET, 1));
		assertNotNull(tRow, "the :313 row resolves for (egg, selector, 800 mB steam)");
		assertEquals(0, tRow.mEUt, "EUt 0 verbatim");
		assertEquals(128, tRow.mDuration, "duration 128 x tAmount verbatim");
		assertEquals(800, tRow.mFluidInputs[0].getAmount(), "FL.Steam.make(800 x tAmount)");
		assertEquals(5, tRow.mFluidOutputs[0].getAmount(), "FL.DistW.make(5 x tAmount)");
		assertSame(Items.FEATHER, tRow.mOutputs[0].getItem(), "vanilla egg → the BROWN boiled egg (ST.equal face)");
		assertSame(Items.BONE_MEAL, tRow.mOutputs[1].getItem(), "the Birb_Egg_Shell fallback = Dye_Bonemeal (bonemeal x1)");
		// the ST.tag(0) face rides the table: the second input is the config-0 selector leg
		GT6RecipesFoodTail.Row tSource = GT6RecipesFoodTail.table().stream()
				.filter(r -> r.note().startsWith(":313[egg]")).findFirst().orElse(null);
		assertNotNull(tSource, "the vanilla-egg autoclave row is in the table");
		assertEquals(GT6RecipesFoodTail.ItemLeg.SELECTOR, tSource.inItems()[1].kind(), "ST.tag(0) = the selector leg");
		assertEquals(0, tSource.inItems()[1].config(), "the config-0 selector");
	}

	/** The :412/:413 fish-oil rows verbatim: the oil columns and the halved byproduct ladder. */
	@Test
	void theFishOilRowsAreUpstreamVerbatim() {
		GT6RecipesFoodTail.load();
		// cod: oil 2U → Squeezer 2000 mB / Juicer 1000 mB; byproduct FishRaw U → dust x1
		Recipe tSqueezer = GT6RecipeMaps.SQUEEZER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, null,
				new ItemStack(SYNTHETIC_FOODS.get("minecraft:cod"), 1));
		assertNotNull(tSqueezer, "the :412 cod row resolves");
		assertEquals(2000, tSqueezer.mFluidOutputs[0].getAmount(), "MT.FishOil.liquid(2U) = 2000 mB");
		assertEquals(1, tSqueezer.mOutputs[0].getCount(), "the FishRaw U byproduct → dust x1 (the OM ladder)");
		assertEquals(32, tSqueezer.mDuration, "duration 32 verbatim");
		// pufferfish: oil 1U → Juicer 500 mB; byproduct FishRaw U2 → dustSmall x2
		Recipe tJuicer = GT6RecipeMaps.JUICER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, null,
				new ItemStack(SYNTHETIC_FOODS.get("minecraft:pufferfish"), 1));
		assertNotNull(tJuicer, "the :413 pufferfish row resolves");
		assertEquals(500, tJuicer.mFluidOutputs[0].getAmount(), "MT.FishOil.liquid(1U/2) = 500 mB");
		assertEquals(2, tJuicer.mOutputs[0].getCount(), "the FishRaw U2 byproduct → dustSmall x2");
		// salmon: oil 4U → Squeezer 4000 mB
		Recipe tSalmon = GT6RecipeMaps.SQUEEZER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, null,
				new ItemStack(SYNTHETIC_FOODS.get("minecraft:salmon"), 1));
		assertNotNull(tSalmon, "the :412 salmon row resolves");
		assertEquals(4000, tSalmon.mFluidOutputs[0].getAmount(), "MT.FishOil.liquid(4U) = 4000 mB");
	}

	/** The :737/:725/:519 singles verbatim: the tiny chocolate ladder, the abyssal dough, the mushroom soup. */
	@Test
	void theSingleRowsAreUpstreamVerbatim() {
		GT6RecipesFoodTail.load();
		// :733/:735/:737 — the dust/dustSmall/dustTiny chocolate ladder, verbatim prefixes and counts (table-side:
		// the material fixture collapses every material leg to one stand-in, the map lookup cannot pick one row)
		GT6RecipesFoodTail.Row t733 = rowByNote(":733"), t735 = rowByNote(":735"), t737 = rowByNote(":737");
		assertEquals(OP.dust, t733.inItems()[0].prefix(), ":733 = the OM.dust face");
		assertEquals(2, t733.outItems()[0].count(), ":733 → dustChocolate U*2 = x2");
		assertEquals(OP.dustSmall, t735.inItems()[0].prefix(), ":735 = the OM.dust(U4) face");
		assertEquals(OP.dustSmall, t735.outItems()[0].prefix(), ":735 → dustChocolate U2 = dustSmall");
		assertEquals(2, t735.outItems()[0].count(), ":735 → x2");
		assertEquals(OP.dustTiny, t737.inItems()[0].prefix(), ":737 = the OM.dust(U9) face");
		assertEquals(OP.dustTiny, t737.outItems()[0].prefix(), ":737 → dustChocolate 2*U9 = dustTiny");
		assertEquals(2, t737.outItems()[0].count(), ":737 → x2");
		assertEquals(MT.Cocoa, t733.inItems()[1].material(), "the cocoa leg");
		assertEquals(MT.Chocolate, t733.outItems()[0].material(), "the chocolate output");
		// :519 — red + brown mushroom → soup 1000 mB
		Recipe tSoup = GT6RecipeMaps.MIXER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, new FluidStack[0],
				new ItemStack(Items.RED_MUSHROOM, 1), new ItemStack(Items.BROWN_MUSHROOM, 1));
		assertNotNull(tSoup, "the :519 row resolves for the mushroom pair");
		assertEquals(1000, tSoup.mFluidOutputs[0].getAmount(), "FL.Soup_Mushroom.make(1000)");
		assertEquals(0, tSoup.mOutputs.length, "ZL_IS — no item output");
		// :725 — dust_oat_abyssal + hellderberryjuice 100 → the abyssal dough (table-side: the MIXER
		// map is too crowded under the all-GUNPOWDER fixture for an unambiguous findRecipe probe)
		GT6RecipesFoodTail.Row tOat = rowByNote(":725");
		assertNotNull(tOat, "the :725 row is in the table");
		assertEquals(MT.OatAbyssal, tOat.inItems()[0].material(), "the abyssal oat dust leg");
		assertEquals(100, tOat.inFluids()[0].amount(), "FL.Juice_Hellderberry.make(100)");
		assertEquals(0, tOat.inFluids().length + tOat.outFluids().length - tOat.inFluids().length, "NF — no fluid output");
		assertEquals("gt6:food_dough_abyssal", tOat.outItems()[0].foodId(), "the abyssal dough output");
	}

	/** The row with the exact note (the note = the upstream file:line anchor). */
	private static GT6RecipesFoodTail.Row rowByNote(String aNote) {
		return GT6RecipesFoodTail.table().stream().filter(r -> r.note().equals(aNote)).findFirst().orElse(null);
	}

	// ------------------------------------------------------------------
	// the id universes
	// ------------------------------------------------------------------

	/** Every MATERIAL leg pair must sit in the GTMaterialItems ∪ GTMaterialBlocks registration universe. */
	@Test
	void everyMaterialLegPairIsRegistered() {
		Set<String> tUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tUniverse.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			tUniverse.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		assertFalse(tUniverse.isEmpty(), "the universe built");
		Set<String> tMissing = new HashSet<>();
		for (GT6RecipesFoodTail.Row tRow : GT6RecipesFoodTail.table()) {
			for (GT6RecipesFoodTail.ItemLeg[] tSides : new GT6RecipesFoodTail.ItemLeg[][] {tRow.inItems(), tRow.outItems()}) {
				for (GT6RecipesFoodTail.ItemLeg tLeg : tSides) {
					if (tLeg.kind() != GT6RecipesFoodTail.ItemLeg.MATERIAL) continue;
					String tId = GTMaterialItems.itemIdOf(tLeg.prefix(), tLeg.material());
					if (!tUniverse.contains(tId)) tMissing.add(tRow.note() + ": " + tId);
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 material ids (these rows would skip live): " + tMissing);
	}

	/** Every fluid-leg name except the declared sludge must sit in the GTFluids spec universe (offline-readable data). */
	@Test
	void everyFluidLegNameIsARegisteredSpec() {
		Set<String> tSpecs = new HashSet<>();
		for (GTFluids.EngineFluidSpec tSpec : GTFluids.ENGINE_SPECS) tSpecs.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.AQUA_SPECS) tSpecs.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.SIMPLE_LIQUID_SPECS) tSpecs.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_FLUID_SPECS) tSpecs.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_B1_SPECS) tSpecs.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_B2_SPECS) tSpecs.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_TAIL_SPECS) tSpecs.add(tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.HONEY_FLUID_SPECS) tSpecs.add(tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.BEE_ROW_FLUID_SPECS) tSpecs.add(tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.HOT_FLUID_SPECS) tSpecs.add(tSpec.name());
		for (GTFluids.ChemicalFluid tFamily : GTFluids.CHEMICALS) tSpecs.add(tFamily.spec.name());
		assertFalse(tSpecs.isEmpty(), "the spec universe built (" + tSpecs.size() + " names)");
		Set<String> tMissing = new HashSet<>();
		for (GT6RecipesFoodTail.Row tRow : GT6RecipesFoodTail.table()) {
			for (GT6RecipesFoodTail.FluidLeg[] tSides : new GT6RecipesFoodTail.FluidLeg[][] {tRow.inFluids(), tRow.outFluids()}) {
				for (GT6RecipesFoodTail.FluidLeg tLeg : tSides) {
					if (GT6RecipesFoodTail.SLUDGE_NULL.equals(tLeg.name())) continue; // the declared-null leg
					if (!tSpecs.contains(tLeg.name())) tMissing.add(tRow.note() + ": " + tLeg.name());
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered fluid names (these rows would skip live): " + tMissing);
	}

	/** The food ids the band resolves must be registered rows of the three food tables (bidirectional-exact heads). */
	@Test
	void everyFoodIdIsARegisteredFoodRow() {
		Set<String> tRows = new HashSet<>();
		for (gregtech6.registry.GT6Foods.FoodRow tRow : gregtech6.registry.GT6Foods.FOOD_ROWS) tRows.add(tRow.id());
		for (gregtech6.registry.GT6BakeFoods.BakeRow tRow : gregtech6.registry.GT6BakeFoods.BAKE_ROWS) tRows.add(tRow.id());
		for (gregtech6.registry.GT6CropFoods.CropFoodRow tRow : gregtech6.registry.GT6CropFoods.FOOD_ROWS) tRows.add(tRow.id());
		tRows.add("minecraft:egg"); // the vanilla itemEgg member
		tRows.add("minecraft:cod"); tRows.add("minecraft:salmon"); tRows.add("minecraft:tropical_fish"); tRows.add("minecraft:pufferfish");
		Set<String> tMissing = new HashSet<>();
		for (GT6RecipesFoodTail.Row tRow : GT6RecipesFoodTail.table()) {
			for (GT6RecipesFoodTail.ItemLeg[] tSides : new GT6RecipesFoodTail.ItemLeg[][] {tRow.inItems(), tRow.outItems()}) {
				for (GT6RecipesFoodTail.ItemLeg tLeg : tSides) {
					if (tLeg.kind() != GT6RecipesFoodTail.ItemLeg.FOOD) continue;
					String tId = tLeg.foodId();
					if (GT6RecipesFoodTail.SCRAP_MEAT_INPUT.equals(tId)) continue; // the declared-absent chum input (pooled T3)
					if (tId.startsWith("gt6:")) tId = tId.substring(4); // the tables carry bare snake ids
					if (!tRows.contains(tId)) tMissing.add(tRow.note() + ": " + tLeg.foodId());
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered food ids: " + tMissing);
		// the declared-absent scrapmeat input must NOT be a registered row (the pooled T3 item)
		assertTrue(gregtech6.registry.GT6Foods.FOOD_ROWS.stream().noneMatch(r -> r.id().equals("food_scrap_meat")),
				"food_scrap_meat stays pooled-absent (the chum rows' declared-null input)");
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	private static int countNotes(List<GT6RecipesFoodTail.Row> aTable, String aRegex) {
		int rCount = 0;
		for (GT6RecipesFoodTail.Row tRow : aTable) if (tRow.note().matches(aRegex + ".*") || tRow.note().matches(aRegex)) rCount++;
		return rCount;
	}
}
