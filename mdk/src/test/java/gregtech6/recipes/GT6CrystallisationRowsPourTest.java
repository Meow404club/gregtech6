/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The p34-machines-bumblelyzer-crucible true-row pour test (the GT6ChemicalRowsPourTest
 * fixture posture): the shipped {@code data/gt6/recipe_maps/crystallisationcrucible.json}
 * (the Loader_Recipes_Other.java:683-706 verbatim stock — the six noble gases × the
 * Si/Ge/RedstoneAlloy/NikolineAlloy boule quartet + the Al2O3 sapphire pair + the
 * coloured-sapphire dopant walk) pours through the real {@link GT6RecipeMapJsonLoader} seam
 * into {@code GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE}. The census (132 rows), the
 * representative-row content pins, and the id faces (the 6 gases + 5 molten carriers +
 * the dust item set) are the acceptance ② face.
 */
public class GT6CrystallisationRowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The fluid ids the loader asked for during the pour (the id-face assertion set). */
	private final Set<String> mRequestedFluidPaths = new HashSet<>();
	/** The item ids the loader asked for during the pour. */
	private final Set<String> mRequestedItemPaths = new HashSet<>();

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		mRequestedFluidPaths.clear();
		mRequestedItemPaths.clear();
		GT6RecipeMapJsonLoader.sItemResolver = aId -> {
			if (!"gt6".equals(aId.getNamespace())) return Items.AIR;
			mRequestedItemPaths.add(aId.getPath());
			return Items.IRON_INGOT; // the identity stand-in — the mechanics compare shapes only
		};
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> {
			if ("minecraft".equals(aId.getNamespace())) return Fluids.WATER;
			mRequestedFluidPaths.add(aId.getPath());
			return Fluids.WATER;
		};
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads the shipped true-row file verbatim and pours it under its map key. */
	private void pourShipped() throws Exception {
		String tPath = "/data/gt6/recipe_maps/crystallisationcrucible.json";
		try (InputStream tStream = GT6CrystallisationRowsPourTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped true-row file " + tPath + " rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", "crystallisationcrucible"), JsonParser.parseString(tJson)));
		}
	}

	/** The census: 6 gases × (4 quartet + 4 quartet-×9 + 2 sapphire + 6 + 6 coloured) = 132 rows. */
	@Test
	public void theShippedStockPoursTheFullUpstreamCensus() throws Exception {
		pourShipped();
		assertEquals(132, GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE.mRecipeList.size(),
				"6 gases x 22 rows = the :683-706 stock (4 quartet-U9 + 4 quartet-full + 2 sapphire + 12 coloured)");
	}

	/** The Si boule rows carry the :683/:687 forms (16 EUt, 72000/648000 t, the gas+molten legs, the ×1/×9 outputs). */
	@Test
	public void theSiliconRowsCarryTheUpstreamContent() throws Exception {
		pourShipped();
		int tSingle = 0, tNine = 0;
		for (Recipe tRow : GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE.mRecipeList) {
			if (tRow.mDuration == 72000L) {
				tSingle++;
				assertEquals(16L, tRow.mEUt, "the :683 EUt");
				assertEquals(1, tRow.mInputs.length, "the dust input slot");
				assertEquals(2, tRow.mFluidInputs.length, "gas + molten");
				assertEquals(1000, tRow.mFluidInputs[0].getAmount(), "the gas(U) leg — the createGas 1000 mB/unit default");
				assertEquals(560, tRow.mFluidInputs[1].getAmount(), "the 35*U9 molten leg = 35/9 x 144 L");
				assertEquals(1, tRow.mOutputs[0].getCount(), "one boule");
			} else if (tRow.mDuration == 648000L) {
				tNine++;
				assertEquals(16L, tRow.mEUt, "the :687 EUt");
				assertEquals(9000, tRow.mFluidInputs[0].getAmount(), "the gas 9U leg");
				assertEquals(5040, tRow.mFluidInputs[1].getAmount(), "the 35*U molten leg = 35 x 144 L");
				assertEquals(9, tRow.mOutputs[0].getCount(), "nine boules");
			}
		}
		assertEquals(24, tSingle, "the :683 forms — 4 quartet materials x 6 gases");
		assertEquals(24, tNine, "the :687 forms — 4 quartet materials x 6 gases");
	}

	/** The sapphire walks carry the :692-693/:695-706 forms (256 EUt, the alumina legs, the dopant dusts). */
	@Test
	public void theSapphireRowsCarryTheUpstreamContent() throws Exception {
		pourShipped();
		int tBase = 0, tBaseNine = 0, tColour = 0, tColourThree = 0;
		Set<Integer> tOutputCounts = new HashSet<>();
		Set<Integer> tAluminaLegs = new HashSet<>();
		for (Recipe tRow : GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE.mRecipeList) {
			if (tRow.mEUt != 256L) continue;
			int tMolten = tRow.mFluidInputs[1].getAmount();
			if (tRow.mDuration == 18000L && tMolten == 464) { // the :692 U9 form — 29/9 x 144
				tBase++;
			} else if (tRow.mDuration == 156000L && tMolten == 4176) { // the :693 full form — 29 x 144
				tBaseNine++;
			} else if (tRow.mDuration == 18000L && tMolten == 480) { // the :695-700 2*U3 forms — 10/3 x 144
				tColour++;
				tOutputCounts.add(tRow.mOutputs[0].getCount());
				tAluminaLegs.add(tMolten);
			} else if (tRow.mDuration == 52000L && tMolten == 1440) { // the :701-706 2*U x3 forms — 10 x 144
				tColourThree++;
				tOutputCounts.add(tRow.mOutputs[0].getCount());
				tAluminaLegs.add(tMolten);
			}
		}
		assertEquals(6, tBase, "the :692 forms — 6 gases");
		assertEquals(6, tBaseNine, "the :693 forms — 6 gases");
		assertEquals(36, tColour, "the :695-700 forms — 6 dopants x 6 gases");
		assertEquals(36, tColourThree, "the :701-706 forms — 6 dopants x 6 gases");
		assertEquals(Set.of(1, 3), tOutputCounts, "the boule output counts (x1 and the :701 x3)");
		assertEquals(Set.of(480, 1440), tAluminaLegs, "the coloured-form alumina legs");
	}

	/** The id faces: the six noble gases, the five molten carriers, and the dust item set. */
	@Test
	public void theIdFacesCoverGasesMoltenAndDusts() throws Exception {
		pourShipped();
		for (String tGas : new String[] {"helium", "neon", "argon", "krypton", "xenon", "radon"}) {
			assertTrue(mRequestedFluidPaths.contains(tGas), "the noble gas face: " + tGas);
		}
		for (String tMolten : new String[] {"silicon_molten", "germanium_molten", "redstonealloy_molten", "nikolinealloy_molten", "alumina_molten"}) {
			assertTrue(mRequestedFluidPaths.contains(tMolten), "the molten carrier face: " + tMolten);
		}
		for (String tDust : new String[] {"dust_silicon", "dust_germanium", "dust_redstone_alloy", "dust_nikoline_alloy", "dust_alumina",
				"dust_magnesium", "dust_titanium", "dust_copper", "dust_iron", "dust_vanadium", "dust_chromium"}) {
			assertTrue(mRequestedItemPaths.contains(tDust), "the dust face: " + tDust);
		}
		assertEquals(22, mRequestedItemPaths.size(), "the 11 input dusts + the 11 output boule ids");
		assertTrue(mRequestedItemPaths.containsAll(Set.of("boule_gt_silicon", "boule_gt_germanium", "boule_gt_redstone_alloy",
				"boule_gt_nikoline_alloy", "boule_gt_sapphire", "boule_gt_blue_sapphire", "boule_gt_green_sapphire",
				"boule_gt_yellow_sapphire", "boule_gt_orange_sapphire", "boule_gt_purple_sapphire", "boule_gt_ruby")),
				"the boule output id face: " + mRequestedItemPaths);
		assertEquals(11, mRequestedFluidPaths.size(), "exactly the six gases + the five molten carriers");
	}
}
