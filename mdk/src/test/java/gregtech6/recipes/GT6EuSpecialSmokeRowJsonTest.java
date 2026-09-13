package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
 * The eu-special smoke-row datapack acceptance (task p29-w2-eu-special, the OFFLINE
 * half — the {@link GT6EuHuSmokeRowJsonTest} shape): the three committed
 * {@code data/gt6/recipe_maps/<map>.json} files — {@code autocrafter}, {@code lightning},
 * {@code laminator} — pour through the PUBLIC {@link GT6RecipeMapJsonLoader#pour} seam
 * into their card-① DECLARED-empty maps, with the item/fluid resolvers injected for the
 * vanilla ids the rows carry (the GT6RecipeMapJsonLoaderTest fixture convention). The
 * RM.java declaration columns of the three maps (the :63/:94/:90 transcriptions) are
 * pinned alongside — the smoke rows must FIT the maps they pour into.
 */
class GT6EuSpecialSmokeRowJsonTest extends GTRecipesOfflineTestBase {

	/** The ids the three smoke rows carry — every one resolves to a real vanilla entry. */
	private static final java.util.function.Function<ResourceLocation, Item> ITEM_FIXTURE = aId -> switch (aId.toString()) {
		case "minecraft:oak_planks" -> Items.OAK_PLANKS;
		case "minecraft:crafting_table" -> Items.CRAFTING_TABLE;
		case "minecraft:quartz" -> Items.QUARTZ;
		case "minecraft:glowstone_dust" -> Items.GLOWSTONE_DUST;
		case "minecraft:prismarine_crystals" -> Items.PRISMARINE_CRYSTALS;
		case "minecraft:piston" -> Items.PISTON;
		case "minecraft:slime_ball" -> Items.SLIME_BALL;
		case "minecraft:sticky_piston" -> Items.STICKY_PISTON;
		default -> null;
	};

	private static final java.util.function.Function<ResourceLocation, Fluid> FLUID_FIXTURE = aId -> Fluids.WATER;

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

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
		try (InputStream tStream = GT6EuSpecialSmokeRowJsonTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + aName)) {
			assertNotNull(tStream, "the committed smoke row must be on the classpath: " + aName);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	/** The three files pour under their map keys — the whitelist carries the card-③ datapack domain. */
	@Test
	void theThreeSmokeRowsPourThroughTheLoaderSeam() throws Exception {
		Map<ResourceLocation, JsonElement> tData = new HashMap<>();
		tData.put(new ResourceLocation("gt6", "autocrafter"), resource("autocrafter.json"));
		tData.put(new ResourceLocation("gt6", "lightning"), resource("lightning.json"));
		tData.put(new ResourceLocation("gt6", "laminator"), resource("laminator.json"));
		GT6RecipeMapJsonLoader.pour(tData);

		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("autocrafter"), "one autocrafter smoke row");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("lightning"), "one lightning smoke row");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("laminator"), "one laminator smoke row");

		// the rows are LIVE in the maps (the findRecipe stock grew by one each — the
		// DECLARED-empty card-① state is the whole pre-pour stock)
		assertEquals(1, GT6RecipeMaps.AUTOCRAFTER.mRecipeList.size(), "the AUTOCRAFTER map held ONLY the smoke row");
		assertEquals(1, GT6RecipeMaps.LIGHTNING.mRecipeList.size());
		assertEquals(1, GT6RecipeMaps.LAMINATOR.mRecipeList.size());
	}

	/** The RM.java declaration columns the smoke rows must fit (:63/:94/:90 — the card-① transcription). */
	@Test
	void theMapDeclarationsMatchTheUpstreamTranscription() {
		GT6RecipeMaps.init();
		// RM.java:63 — items 9/12/1, fluids 0/0/0, MIN 1
		assertEquals(9, GT6RecipeMaps.AUTOCRAFTER.mInputItemsCount, ":63 — 9 item inputs");
		assertEquals(12, GT6RecipeMaps.AUTOCRAFTER.mOutputItemsCount, ":63 — 12 item outputs");
		assertEquals(1, GT6RecipeMaps.AUTOCRAFTER.mMinimalInputItems, ":63 — MIN 1 (items)");
		assertEquals(0, GT6RecipeMaps.AUTOCRAFTER.mInputFluidCount, ":63 — 0/0/0 fluids");
		assertEquals(1, GT6RecipeMaps.AUTOCRAFTER.mMinimalInputs, ":63 — MIN 1");
		// RM.java:94 — items 6/6/0, fluids 6/6/0, MIN 2
		assertEquals(6, GT6RecipeMaps.LIGHTNING.mInputItemsCount, ":94 — 6/6 items");
		assertEquals(6, GT6RecipeMaps.LIGHTNING.mInputFluidCount, ":94 — 6/6 fluids");
		assertEquals(2, GT6RecipeMaps.LIGHTNING.mMinimalInputs, ":94 — MIN 2 (total)");
		// RM.java:90 — items 2/1/2, fluids 0/0/0, MIN 2
		assertEquals(2, GT6RecipeMaps.LAMINATOR.mInputItemsCount, ":90 — 2/1/2 items");
		assertEquals(0, GT6RecipeMaps.LAMINATOR.mInputFluidCount, ":90 — 0/0/0 fluids");
		assertEquals(2, GT6RecipeMaps.LAMINATOR.mMinimalInputItems, ":90 — MIN 2 (items)");
	}
}
