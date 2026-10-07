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
		// the reset FIRST (task toolhead-r11e-press-mortar, the neo leg empirics): the neoforge
		// junit FML boot runs the whole static pour suite at modloading (build/minecraft-junit/
		// logs/latest.log 19:07:37 — Meat mortar 16 + the FoodTail mortar subset = 23 rows land
		// in the boot generation), so a bare init() is a NO-OP there (FURNACE != null) and the
		// census would count boot residue. reset() retires the boot generation; init() then
		// creates virgin maps — the exact state the @AfterEach reset leaves for methods 2..n.
		GT6RecipeMaps.reset();
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
			GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", "mortar"), JsonParser.parseString(tJson)));
		}
	}

	/**
	 * The census: :674-677 glass family 34 (1+16 nine-dust + 1+16 one-dust) + :678 bone
	 * + :680 flint + :681 gravel + :682 coal + :683 charcoal + :684 rotten flesh + the
	 * :705-706 blaze pair = 42, +2 the HandlerPrefix arrow-head walk representatives
	 * (Loader_Recipes_Handlers.java:100-101, task toolhead-r11e-press-mortar) = 44.
	 */
	@Test
	public void theShippedStockPoursTheFullUpstreamCensus() throws Exception {
		pourShipped();
		assertEquals(44, GT6RecipeMaps.MORTAR.mRecipeList.size(),
				"34 glass + bone + flint + gravel + coal + charcoal + rotten flesh + 2 blaze + 2 arrow-head handler representatives");
	}

	/** The family forms: 16 EUt on the vanilla stock (the Loader_Recipes_Vanilla column); the (duration, output-count) tally is the upstream column pair. */
	@Test
	public void theFamiliesCarryTheUpstreamTiers() throws Exception {
		pourShipped();
		Map<String, Integer> tTally = new java.util.TreeMap<>();
		int tVanillaRows = 0;
		for (Recipe tRow : GT6RecipeMaps.MORTAR.mRecipeList) {
			if (tRow.mEUt == 16L) {
				tVanillaRows++;
				assertEquals(1, tRow.mInputs.length, "single input slot (the addRecipe1 form)");
				assertEquals(1, tRow.mOutputs.length, "single output slot");
				tTally.merge(tRow.mDuration + "t x" + tRow.mOutputs[0].getCount(), 1, Integer::sum);
			}
		}
		assertEquals(42, tVanillaRows, "the Loader_Recipes_Vanilla stock keeps its 16 EUt column");
		assertEquals(Map.of(
				"32t x9", 17,   // :674 + the :675 sixteen — glass family, 9 Glass dust
				"32t x1", 18,   // :676 + the :677 sixteen — pane family, 1 Glass dust; :681 — gravel -> 1 flint
				"32t x2", 1,    // :678 — bone -> 2 bonemeal
				"16t x1", 4,    // :680 flint / :682 coal / :683 charcoal / :684 rotten flesh
				"32t x3", 1,    // :705 — blaze stick -> 3 tiny Blaze dust
				"64t x6", 1),   // :706 — long blaze stick -> 6 tiny Blaze dust
				tTally, "the (duration, output count) census over the 42 vanilla rows");
	}

	/**
	 * The HandlerPrefix arrow-head walk representatives (task toolhead-r11e-press-mortar,
	 * Loader_Recipes_Handlers.java:100-101 — the P8 pooled reclaim of the two tool-head
	 * rows): hand-crank eut 0 / 16 t, toolHeadArrow (U9) and toolHeadRawArrow (U8) each
	 * pulverize to ONE dustTiny of the walked material (OM.pulverize:370-372 = the
	 * mTargetPulver map through the OM.dust ladder :460-467; both U9 and U8 bindStack to
	 * a single tiny), the MORTAR-tagged representative is Flint (MT.java:1058) — the full
	 * material walk stays the declared pooled face (the :79-112 band is one handler per
	 * crushed prefix; only the two tool-head statements are this card's scope).
	 */
	@Test
	public void theArrowHeadHandlerRowsPourUpstreamVerbatim() throws Exception {
		pourShipped();
		Map<String, Integer> tTally = new java.util.TreeMap<>();
		int tHandlerRows = 0;
		for (Recipe tRow : GT6RecipeMaps.MORTAR.mRecipeList) {
			if (tRow.mEUt != 0L) continue;
			tHandlerRows++;
			assertEquals(16L, tRow.mDuration, "the handler duration column (the mDuration=16 arm, RecipeMapHandlerPrefix:218)");
			assertEquals(1, tRow.mInputs.length, "the walked prefix slot alone (no additional input on :100-101)");
			assertEquals(1, tRow.mOutputs.length, "the pulverized-remains slot alone");
			tTally.merge(tRow.mInputs[0].getCount() + " -> " + tRow.mOutputs[0].getCount(), 1, Integer::sum);
		}
		assertEquals(2, tHandlerRows, "the :100 toolHeadArrow + :101 toolHeadRawArrow representatives");
		assertEquals(Map.of("1 -> 1", 2), tTally, "one arrow head in, one dustTiny out (the U9/U8 bindStack face)");
	}

	/**
	 * The id faces: the gt6 material items (dust_glass/dust_flint/dust_coal/dust_charcoal/
	 * dust_meat_rotten/dust_tiny_blaze/stick_blaze/stick_long_blaze + the two arrow-head
	 * representatives and their dust) and the vanilla inputs including the sixteen
	 * dye-order stained identities.
	 */
	@Test
	public void theIdFacesCoverTheMaterialAndVanillaSets() throws Exception {
		pourShipped();
		for (String tGt6 : new String[] {"gt6:dust_glass", "gt6:dust_flint", "gt6:dust_coal", "gt6:dust_charcoal",
				"gt6:dust_meat_rotten", "gt6:dust_tiny_blaze", "gt6:stick_blaze", "gt6:stick_long_blaze",
				"gt6:tool_head_arrow_flint", "gt6:tool_head_raw_arrow_flint", "gt6:dust_tiny_flint"}) {
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
		assertEquals(11 + 9 + 32, mRequestedItems.size(), "no stray ids — exactly the 11 gt6 + 9 plain vanilla + 32 stained identities");
	}
}
