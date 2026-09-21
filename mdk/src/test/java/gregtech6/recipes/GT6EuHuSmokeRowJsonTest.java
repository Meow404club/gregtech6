package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The eu-hu smoke-row datapack acceptance (task p29-w1-eu-hu-families, the OFFLINE half):
 * the four committed {@code data/gt6/recipe_maps/<map>.json} files — {@code loom},
 * {@code boxinator}, {@code unboxinator}, {@code fermenter} — pour through the PUBLIC
 * {@link GT6RecipeMapJsonLoader#pour} seam into their maps (the card-D datapack domain),
 * with the item/fluid resolvers injected for the vanilla ids the rows carry (the
 * GT6RecipeMapJsonLoaderTest fixture convention). MIXER and SIFTING are asserted
 * UNTOUCHED by the JSON face (the card ruling: those two maps reuse their existing
 * static rows — GT6RecipesMixer / GT6RecipesSifter).
 */
class GT6EuHuSmokeRowJsonTest extends GTRecipesOfflineTestBase {

	/** The ids the four smoke rows carry — every one resolves to a real vanilla entry. */
	private static final Function<ResourceLocation, Item> ITEM_FIXTURE = aId -> switch (aId.toString()) {
		case "minecraft:string" -> Items.STRING;
		case "minecraft:white_wool" -> Items.WHITE_WOOL;
		case "minecraft:paper" -> Items.PAPER;
		case "minecraft:compass" -> Items.COMPASS;
		case "minecraft:map" -> Items.MAP;
		case "minecraft:wheat" -> Items.WHEAT;
		case "minecraft:sugar" -> Items.SUGAR;
		default -> null;
	};

	private static final Function<ResourceLocation, Fluid> FLUID_FIXTURE = aId -> Fluids.WATER;

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = ITEM_FIXTURE;
		GT6RecipeMapJsonLoader.sFluidResolver = FLUID_FIXTURE;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems;
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset();
	}

	/** Reads one committed smoke-row file off the test classpath (src/main/resources rides it). */
	private static JsonElement resource(String aName) throws Exception {
		try (InputStream tStream = GT6EuHuSmokeRowJsonTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + aName)) {
			assertNotNull(tStream, "the committed smoke row must be on the classpath: " + aName);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	/** The four files pour under their map keys — the whitelist carries the card-D datapack domain. */
	@Test
	void theFourSmokeRowsPourThroughTheLoaderSeam() throws Exception {
		Map<ResourceLocation, JsonElement> tData = new HashMap<>();
		tData.put(new ResourceLocation("gt6", "loom"), resource("loom.json"));
		tData.put(new ResourceLocation("gt6", "boxinator"), resource("boxinator.json"));
		tData.put(new ResourceLocation("gt6", "unboxinator"), resource("unboxinator.json"));
		tData.put(new ResourceLocation("gt6", "fermenter"), resource("fermenter.json"));
		GT6RecipeMapJsonLoader.pour(tData);

		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("loom"), "one loom smoke row");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("boxinator"), "one boxinator smoke row");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("unboxinator"), "one unboxinator smoke row");
		assertEquals(7, GT6RecipeMapJsonLoader.pouredCount("fermenter"), "the fermenter file — 1 smoke row + the 6 p33-food-fluids-b1 core rows");

		// the rows are LIVE in the maps (the findRecipe stock grew by one each)
		assertEquals(1, GT6RecipeMaps.LOOM.mRecipeList.size(), "the LOOM map held ONLY the smoke row (the DECLARED-empty card-A state)");
		assertEquals(1, GT6RecipeMaps.BOXINATOR.mRecipeList.size());
		assertEquals(1, GT6RecipeMaps.UNBOXINATOR.mRecipeList.size());
		assertEquals(7, GT6RecipeMaps.FERMENTER.mRecipeList.size());
	}

	/** A repeated pour REPLACES the same-file subset — the idempotence face of the seam. */
	@Test
	void repeatedPourReplacesTheSubsetIdempotently() throws Exception {
		Map<ResourceLocation, JsonElement> tData = new HashMap<>();
		tData.put(new ResourceLocation("gt6", "fermenter"), resource("fermenter.json"));
		GT6RecipeMapJsonLoader.pour(tData);
		GT6RecipeMapJsonLoader.pour(tData);
		assertEquals(7, GT6RecipeMaps.FERMENTER.mRecipeList.size(), "the subset replace — never a duplicate");
	}

	/** MIXER and SIFTING are NOT in the card-D JSON face — the loader never touches them here. */
	@Test
	void theSharedMapsAreUntouchedByTheSmokeRows() {
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("mixer"), "MIXER reuses its static rows (GT6RecipesMixer)");
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("sifting"), "SIFTING reuses its static rows (GT6RecipesSifter)");
	}
}
