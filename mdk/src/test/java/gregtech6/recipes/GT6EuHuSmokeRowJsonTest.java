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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The eu-hu smoke-row datapack acceptance (task w1-eu-hu-families, the OFFLINE half):
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
		// the gt6-domain stand-in (the GT6RecipeMapDataB2b1RowsPourTest posture): since
		// recipe-data-b2b1 the fermenter file carries the 195-row stock and the gt6-id
		// registration universe is guarded by ITS id-universe pin — this seam-acceptance
		// test resolves every gt6 id to a stand-in instead of whitelisting 28+ item ids.
		default -> "gt6".equals(aId.getNamespace()) ? Items.IRON_INGOT : null;
	};

	private static final Function<ResourceLocation, Fluid> FLUID_FIXTURE = aId -> Fluids.WATER;

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
		// the neo junit-fml leg NATIVELY pre-seeds GT6RecipeMaps at test-JVM boot
		// (boxinator/unboxinator +4 each, fermenter +1200: the FMLCommonSetup walk),
		// and init() is a no-op once maps exist — without this reset the declared
		// counts ride the boot rows (+4 fingerprint: expected 12330 was 12334).
		// reset() drops the boot generation so the fixture pour is the authority on
		// BOTH legs (the diggables id1470 posture); the forge leg is untouched (the
		// maps are fresh-null there and reset() is a no-op on them).
		GT6RecipeMaps.reset();
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
		tData.put(ResourceLocation.fromNamespaceAndPath("gt6", "loom"), resource("loom.json"));
		tData.put(ResourceLocation.fromNamespaceAndPath("gt6", "boxinator"), resource("boxinator.json"));
		tData.put(ResourceLocation.fromNamespaceAndPath("gt6", "unboxinator"), resource("unboxinator.json"));
		tData.put(ResourceLocation.fromNamespaceAndPath("gt6", "fermenter"), resource("fermenter.json"));
		GT6RecipeMapJsonLoader.pour(tData);

		// 2 since task recipe-b4-juicer-squeezer-flowerfruit: the seated smoke row + exactly one
		// of the b4 :736 dyed rows (white fiber -> white_wool — the ONE output the fixture above
		// resolves; the other 15 dyed outputs WARN-skip, the b2/b1 35-row stock rides its own
		// census pins in the *RowsPourTest family, not this seam-acceptance fixture)
		assertEquals(2, GT6RecipeMapJsonLoader.pouredCount("loom"), "the loom smoke row + the fixture-resolvable b4 dyed row");
		// 12330 = the seated smoke row (the only seated row whose vanilla legs sit in the fixture
		// whitelist — every other seated row walks minecraft ids the stand-in answers null,
		// the b2b1 posture) + task robotics-chain's 10 tip-packing rows (all-gt6 legs pour
		// the stand-in; their registration universe is guarded by the B1 id-universe pin)
		// + the recipe-b7 pack walk 12319 (review-seat rebase roll: the 28 vanilla-bearing b1
		// statics stay out of this narrow fixture's whitelist — the full file census lives in
		// the B1/B7 pour tests)
		assertEquals(12330, GT6RecipeMapJsonLoader.pouredCount("boxinator"), "the boxinator smoke row + the robotics-chain ten + the recipe-b7 pack walk 12319");
		assertEquals(11292, GT6RecipeMapJsonLoader.pouredCount("unboxinator"), "the unboxinator smoke row + the recipe-b7 unbox walk 11291 (the 20 vanilla-bearing seated statics stay out of this narrow fixture's whitelist — the full file census lives in the B2Residual/B7 pour tests)");
		assertEquals(195, GT6RecipeMapJsonLoader.pouredCount("fermenter"), "the fermenter file — the landed b2b1 census (7 smoke + 188 pour; the seat-IX reconciliation bump)");

		// the Ananas row content pin (review fix): the OFFLINE fixture collapses every
		// fluid id to Fluids.WATER (FLUID_FIXTURE above), so the poured Recipe objects
		// cannot carry fluid identity — the pin therefore reads the COMMITTED json
		// resource verbatim. Upstream Loader_Recipes_Food.java:608 ferments Juice_Ananas
		// ("binnie.juicepineapple" — the GTFluids juicepineapple carrier) into
		// Cider_Ananas ("binnie.winepineapple") — NOT winepineapple into itself (the
		// input-id transcription slip this pin guards against).
		// upstream carries TWO Juice_Pineapple input rows (:610 the Cider drink leg,
		// :649 the FRUIT_JUICE walk -> Wine_Fruit), so the selection goes by the row's
		// source citation — a bare input-id scan would last-match the :649 walk row.
		JsonObject tAnanasRow = null;
		for (JsonElement tElement : resource("fermenter.json").getAsJsonObject().getAsJsonArray("recipes")) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().startsWith("Loader_Recipes_Food.java:610")) {
				tAnanasRow = tRow;
			}
		}
		assertNotNull(tAnanasRow, "the fermenter file must carry the Ananas fermentation row");
		assertEquals("gt6:juicepineapple", tAnanasRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString(), "the Ananas input id");
		assertEquals(50, tAnanasRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsLong(), "the Ananas input amount");
		assertEquals(1, tAnanasRow.getAsJsonArray("fluidOutputs").size(), "the Ananas row is a single-fluid-output row");
		assertEquals("gt6:winepineapple", tAnanasRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString(), "the Ananas output id — Cider_Ananas (Loader_Recipes_Food.java:610)");
		assertEquals(25, tAnanasRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsLong(), "the Ananas output amount");

		// the rows are LIVE in the maps (the findRecipe stock grew by the walk each)
		assertEquals(2, GT6RecipeMaps.LOOM.mRecipeList.size(), "the LOOM map held the smoke row + the fixture-resolvable b4 dyed row (the card-A declared-empty era is long gone; the full stock is the *RowsPourTest census)");
		assertEquals(12330, GT6RecipeMaps.BOXINATOR.mRecipeList.size()); // the smoke row + the robotics-chain ten + the recipe-b7 pack walk 12319
		assertEquals(11292, GT6RecipeMaps.UNBOXINATOR.mRecipeList.size()); // the smoke row + the recipe-b7 unbox walk 11291
		assertEquals(195, GT6RecipeMaps.FERMENTER.mRecipeList.size());
	}

	/** A repeated pour REPLACES the same-file subset — the idempotence face of the seam. */
	@Test
	void repeatedPourReplacesTheSubsetIdempotently() throws Exception {
		Map<ResourceLocation, JsonElement> tData = new HashMap<>();
		tData.put(ResourceLocation.fromNamespaceAndPath("gt6", "fermenter"), resource("fermenter.json"));
		GT6RecipeMapJsonLoader.pour(tData);
		GT6RecipeMapJsonLoader.pour(tData);
		assertEquals(195, GT6RecipeMaps.FERMENTER.mRecipeList.size(), "the subset replace — never a duplicate");
	}

	/** MIXER and SIFTING are NOT in the card-D JSON face — the loader never touches them here. */
	@Test
	void theSharedMapsAreUntouchedByTheSmokeRows() {
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("mixer"), "MIXER reuses its static rows (GT6RecipesMixer)");
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("sifting"), "SIFTING reuses its static rows (GT6RecipesSifter)");
	}
}
