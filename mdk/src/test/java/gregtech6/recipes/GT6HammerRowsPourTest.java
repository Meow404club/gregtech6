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
 * The rm-six-maps Hammer row pour test (the GT6CrystallisationRowsPourTest fixture
 * posture): the shipped {@code data/gt6/recipe_maps/hammer.json} (the RM.smash
 * Loader_Recipes_Vanilla.java:545-554 + :556-569 brick families + the OreDict stone
 * listener trio :81/:91/:95 static stock) pours through the real
 * {@link GT6RecipeMapJsonLoader} seam into {@code GT6RecipeMaps.HAMMER}. The census
 * (67 rows), the per-family (duration, output-count) tally and the item-id faces are
 * the acceptance.
 */
public class GT6HammerRowsPourTest extends GTRecipesOfflineTestBase {

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
		String tPath = "/data/gt6/recipe_maps/hammer.json";
		try (InputStream tStream = GT6HammerRowsPourTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped true-row file " + tPath + " rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", "hammer"), JsonParser.parseString(tJson)));
		}
	}

	/**
	 * The census: the RM.smash families 57 (1 cobblestone + 3 sandstone + 2 ice + 17
	 * terracotta + 17 glass + 17 pane) + the brick trio (:556/:558/:559) + the nether
	 * quartet (:565/:567/:568/:569) + the listener trio (:81/:91/:95) = 67.
	 */
	@Test
	public void theShippedStockPoursTheFullUpstreamCensus() throws Exception {
		pourShipped();
		assertEquals(67, GT6RecipeMaps.HAMMER.mRecipeList.size(),
				"57 smash + 3 brick + 4 nether brick + 3 listeners");
	}

	/**
	 * The family forms: 16 EUt everywhere; every row 16 t except the :81 obsidian
	 * listener at 64 t; the (duration, output-count) tally is the upstream column pair.
	 */
	@Test
	public void theFamiliesCarryTheUpstreamTiers() throws Exception {
		pourShipped();
		Map<String, Integer> tTally = new java.util.TreeMap<>();
		for (Recipe tRow : GT6RecipeMaps.HAMMER.mRecipeList) {
			assertEquals(16L, tRow.mEUt, "every Hammer row is 16 EUt (the smash/addRecipe1 column)");
			assertEquals(1, tRow.mInputs.length, "single input slot (the addRecipe1 form)");
			assertEquals(1, tRow.mOutputs.length, "single output slot");
			tTally.merge(tRow.mDuration + "t x" + tRow.mOutputs[0].getCount(), 1, Integer::sum);
		}
		assertEquals(Map.of(
				"16t x1", 24,  // cobblestone(:545) + 3 sandstone(:546) + ice(:547) + glass-pane family 17 (:553/:554) + :559 brick_slab + :569 nether_brick_slab
				"16t x2", 21,  // packed ice(:548) + the 17 terracotta (:549/:550) + :558 brick_stairs + :567 nether_brick_stairs + :568 nether_brick_fence
				"16t x3", 2,   // :556 bricks + :565 nether_bricks
				"16t x4", 2,   // :91 netherrack + :95 end_stone (the rock listeners)
				"16t x9", 17,  // the glass family (:551/:552)
				"64t x8", 1),  // :81 — obsidian -> 8 Obsidian dust
				tTally, "the (duration, output count) census over the 67 rows");
	}

	/**
	 * The id faces: the gt6 material items (dust_glass/dust_ice/dust_clay/dust_obsidian/
	 * rock_gt_netherrack/rock_gt_endstone) and the vanilla inputs including the sixteen
	 * dye-order stained identities and the 1.20.1 flattened slabs.
	 */
	@Test
	public void theIdFacesCoverTheMaterialAndVanillaSets() throws Exception {
		pourShipped();
		for (String tGt6 : new String[] {"gt6:dust_glass", "gt6:dust_ice", "gt6:dust_clay", "gt6:dust_obsidian",
				"gt6:rock_gt_netherrack", "gt6:rock_gt_endstone"}) {
			assertTrue(mRequestedItems.contains(tGt6), "the material face: " + tGt6);
		}
		for (String tVanilla : new String[] {"minecraft:cobblestone", "minecraft:gravel", "minecraft:sand",
				"minecraft:sandstone", "minecraft:chiseled_sandstone", "minecraft:smooth_sandstone",
				"minecraft:ice", "minecraft:packed_ice", "minecraft:terracotta",
				"minecraft:glass", "minecraft:glass_pane", "minecraft:bricks", "minecraft:brick",
				"minecraft:brick_stairs", "minecraft:brick_slab", "minecraft:nether_bricks", "minecraft:nether_brick",
				"minecraft:nether_brick_stairs", "minecraft:nether_brick_fence", "minecraft:nether_brick_slab",
				"minecraft:obsidian", "minecraft:netherrack", "minecraft:end_stone"}) {
			assertTrue(mRequestedItems.contains(tVanilla), "the vanilla face: " + tVanilla);
		}
		String[] tColours = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
				"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
		for (String tColour : tColours) {
			assertTrue(mRequestedItems.contains("minecraft:" + tColour + "_glass"), "the stained-glass fan: " + tColour);
			assertTrue(mRequestedItems.contains("minecraft:" + tColour + "_glass_pane"), "the stained-pane fan: " + tColour);
			assertTrue(mRequestedItems.contains("minecraft:" + tColour + "_terracotta"), "the terracotta fan: " + tColour);
		}
		// 6 gt6 outputs + 23 plain vanilla + 48 stained = 77 ids, no strays
		assertEquals(6 + 23 + 48, mRequestedItems.size(), "no stray ids — exactly the 6 gt6 + 23 plain vanilla + 48 stained identities");
	}
}
