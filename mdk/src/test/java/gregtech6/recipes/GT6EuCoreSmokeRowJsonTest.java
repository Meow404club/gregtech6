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
 * The eu-core smoke-row datapack acceptance (task p29-w2-eu-core-5tier, the OFFLINE half —
 * the {@link GT6EuHuSmokeRowJsonTest} shape): the five committed
 * {@code data/gt6/recipe_maps/<map>.json} files — {@code electrolyzer}, {@code injector},
 * {@code printer}, {@code scannervisuals}, {@code slicer} — pour through the PUBLIC
 * {@link GT6RecipeMapJsonLoader#pour} seam into their DECLARED-EMPTY card-① maps (the
 * card's datapack domain; the live drive of the same rows is the RCON chains' face, the
 * sweep eu_core group).
 */
class GT6EuCoreSmokeRowJsonTest extends GTRecipesOfflineTestBase {

	/** The ids the five smoke rows carry — every one resolves to a real vanilla entry. */
	private static final Function<ResourceLocation, Item> ITEM_FIXTURE = aId -> switch (aId.toString()) {
		case "minecraft:clay_ball" -> Items.CLAY_BALL;
		case "minecraft:brick" -> Items.BRICK;
		case "minecraft:stick" -> Items.STICK;
		case "minecraft:flint" -> Items.FLINT;
		case "minecraft:arrow" -> Items.ARROW;
		case "minecraft:paper" -> Items.PAPER;
		case "minecraft:compass" -> Items.COMPASS;
		case "minecraft:map" -> Items.MAP;
		case "minecraft:stone" -> Items.STONE;
		case "minecraft:stone_slab" -> Items.STONE_SLAB;
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
		try (InputStream tStream = GT6EuCoreSmokeRowJsonTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + aName)) {
			assertNotNull(tStream, "the committed smoke row must be on the classpath: " + aName);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	/** The four files pour under their map keys - the whitelist carried them since card 1. The slicer file retired with task p35-slicer-row-domain (the production rows replaced the smoke debt). */
	@Test
	void theFourSmokeRowsPourThroughTheLoaderSeam() throws Exception {
		Map<ResourceLocation, JsonElement> tData = new HashMap<>();
		tData.put(new ResourceLocation("gt6", "electrolyzer"), resource("electrolyzer.json"));
		tData.put(new ResourceLocation("gt6", "injector"), resource("injector.json"));
		tData.put(new ResourceLocation("gt6", "printer"), resource("printer.json"));
		tData.put(new ResourceLocation("gt6", "scannervisuals"), resource("scannervisuals.json"));
		GT6RecipeMapJsonLoader.pour(tData);

		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("electrolyzer"), "one electrolyzer smoke row");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("injector"), "one injector smoke row");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("printer"), "one printer smoke row");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("scannervisuals"), "one scannervisuals smoke row");

		// the rows are LIVE in the maps (the findRecipe stock grew by one each - the maps
		// were the DECLARED-EMPTY card-1 state, the consumer card pours the first content)
		assertEquals(1, GT6RecipeMaps.ELECTROLYZER.mRecipeList.size(), "the ELECTROLYZER map held ONLY the smoke row");
		assertEquals(1, GT6RecipeMaps.INJECTOR.mRecipeList.size());
		assertEquals(1, GT6RecipeMaps.PRINTER.mRecipeList.size());
		assertEquals(1, GT6RecipeMaps.SCANNER_VISUALS.mRecipeList.size());
		// the SLICER smoke face retired with task p35-slicer-row-domain: the map is a
		// production map now (the Java pourer), the smoke seam file is gone
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("slicer"), "no slicer smoke row anymore");
	}

	/** A repeated pour REPLACES the same-file subset — the idempotence face of the seam. */
	@Test
	void repeatedPourReplacesTheSubsetIdempotently() throws Exception {
		Map<ResourceLocation, JsonElement> tData = new HashMap<>();
		tData.put(new ResourceLocation("gt6", "electrolyzer"), resource("electrolyzer.json"));
		GT6RecipeMapJsonLoader.pour(tData);
		GT6RecipeMapJsonLoader.pour(tData);
		assertEquals(1, GT6RecipeMaps.ELECTROLYZER.mRecipeList.size(), "the subset replace — never a duplicate");
	}

	/** The OTHER fourteen W2 card-① keys stay empty here — the consumer cards own their content. */
	@Test
	void theSiblingCardKeysStayUntouchedByThisFace() {
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("autocrafter"), "card ③ owns the autocrafter content");
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("cryomixer"), "card ④ owns the cryomixer content");
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("steamcracking"), "card ⑤ owns the steam-cracking content");
	}
}
