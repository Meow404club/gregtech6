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
 * The rm-six-maps Mortar row pour test (the GT6CrystallisationRowsPourTest fixture
 * posture): the shipped {@code data/gt6/recipe_maps/mortar.json} (the
 * Loader_Recipes_Vanilla.java:674-684 + :705-706 static stock) pours through the real
 * {@link GT6RecipeMapJsonLoader} seam into {@code GT6RecipeMaps.MORTAR}. The census
 * (42 rows), the per-family content pins (16 EUt everywhere; the 16/32/64 t tiers) and
 * the item-id faces are the acceptance.
 */
public class GT6MortarRowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The item ids the loader asked for during the pour, "namespace:path" (the id-face assertion set). */
	private final Set<String> mRequestedItems = new HashSet<>();

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		mRequestedItems.clear();
		GT6RecipeMapJsonLoader.sItemResolver = aId -> {
			mRequestedItems.add(aId.getNamespace() + ":" + aId.getPath());
			// identity stand-ins per namespace — the mechanics compare shapes only
			return "gt6".equals(aId.getNamespace()) ? Items.IRON_INGOT : Items.BRICK;
		};
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> Fluids.WATER;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads the shipped true-row file verbatim and pours it under its map key. */
	private void pourShipped() throws Exception {
		String tPath = "/data/gt6/recipe_maps/mortar.json";
		try (InputStream tStream = GT6MortarRowsPourTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped true-row file " + tPath + " rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", "mortar"), JsonParser.parseString(tJson)));
		}
	}

	/**
	 * The census: :674-677 glass family 34 (1+16 nine-dust + 1+16 one-dust) + :678 bone
	 * + :680 flint + :681 gravel + :682 coal + :683 charcoal + :684 rotten flesh + the
	 * :705-706 blaze pair = 42.
	 */
	@Test
	public void theShippedStockPoursTheFullUpstreamCensus() throws Exception {
		pourShipped();
		assertEquals(42, GT6RecipeMaps.MORTAR.mRecipeList.size(),
				"34 glass + bone + flint + gravel + coal + charcoal + rotten flesh + 2 blaze");
	}

	/** The family forms: 16 EUt everywhere; the (duration, output-count) tally is the upstream column pair. */
	@Test
	public void theFamiliesCarryTheUpstreamTiers() throws Exception {
		pourShipped();
		Map<String, Integer> tTally = new java.util.TreeMap<>();
		for (Recipe tRow : GT6RecipeMaps.MORTAR.mRecipeList) {
			assertEquals(16L, tRow.mEUt, "every Mortar row is 16 EUt (the Loader_Recipes_Vanilla column)");
			assertEquals(1, tRow.mInputs.length, "single input slot (the addRecipe1 form)");
			assertEquals(1, tRow.mOutputs.length, "single output slot");
			tTally.merge(tRow.mDuration + "t x" + tRow.mOutputs[0].getCount(), 1, Integer::sum);
		}
		assertEquals(Map.of(
				"32t x9", 17,   // :674 + the :675 sixteen — glass family, 9 Glass dust
				"32t x1", 18,   // :676 + the :677 sixteen — pane family, 1 Glass dust; :681 — gravel -> 1 flint
				"32t x2", 1,    // :678 — bone -> 2 bonemeal
				"16t x1", 4,    // :680 flint / :682 coal / :683 charcoal / :684 rotten flesh
				"32t x3", 1,    // :705 — blaze stick -> 3 tiny Blaze dust
				"64t x6", 1),   // :706 — long blaze stick -> 6 tiny Blaze dust
				tTally, "the (duration, output count) census over the 42 rows");
	}

	/**
	 * The id faces: the gt6 material items (dust_glass/dust_flint/dust_coal/dust_charcoal/
	 * dust_meat_rotten/dust_tiny_blaze/stick_blaze/stick_long_blaze) and the vanilla
	 * inputs including the sixteen dye-order stained identities.
	 */
	@Test
	public void theIdFacesCoverTheMaterialAndVanillaSets() throws Exception {
		pourShipped();
		for (String tGt6 : new String[] {"gt6:dust_glass", "gt6:dust_flint", "gt6:dust_coal", "gt6:dust_charcoal",
				"gt6:dust_meat_rotten", "gt6:dust_tiny_blaze", "gt6:stick_blaze", "gt6:stick_long_blaze"}) {
			assertTrue(mRequestedItems.contains(tGt6), "the material face: " + tGt6);
		}
		for (String tVanilla : new String[] {"minecraft:glass", "minecraft:glass_pane", "minecraft:bone",
				"minecraft:bone_meal", "minecraft:flint", "minecraft:gravel", "minecraft:coal",
				"minecraft:charcoal", "minecraft:rotten_flesh"}) {
			assertTrue(mRequestedItems.contains(tVanilla), "the vanilla face: " + tVanilla);
		}
		String[] tColours = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
				"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
		for (String tColour : tColours) {
			assertTrue(mRequestedItems.contains("minecraft:" + tColour + "_glass"), "the stained-glass fan: " + tColour);
			assertTrue(mRequestedItems.contains("minecraft:" + tColour + "_glass_pane"), "the stained-pane fan: " + tColour);
		}
		assertEquals(8 + 9 + 32, mRequestedItems.size(), "no stray ids — exactly the 8 gt6 + 9 plain vanilla + 32 stained identities");
	}
}
