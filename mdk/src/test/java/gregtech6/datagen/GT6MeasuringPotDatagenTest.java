/**
 * The Measuring Pot datagen pin (task issue45-c3, issue #45) — reads the committed
 * generated tree on the classpath (the GT6MoldAssetDatagenTest form — no datagen run) and
 * pins the whole chain: the three recipe rows (the upstream :134 shaped + the :121 reverse
 * + the Loader :2096 smelt), the two-layer block model over the 8 borrowed PNGs, the raw
 * item model over the 997 borrow, the en/zh lang faces, and the registration-id face
 * (the RegistryObject.getId() path read — the C2 lesson: never .get() an unregistered
 * holder offline).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6MeasuringPot;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6MeasuringPotDatagenTest extends GTOfflineTestBase {

	/** The block model texture keys — the colored band + the overlay shell band (8 refs). */
	private static final String[] TEXTURE_KEYS = {
			"sides", "insides", "top", "bottom",
			"overlay_sides", "overlay_insides", "overlay_top", "overlay_bottom"};

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6MeasuringPotDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
	}

	private static void assertTextureExists(String aPath) {
		assertNotNull(GT6MeasuringPotDatagenTest.class.getClassLoader().getResource("assets/gt6/textures/" + aPath),
				"the borrowed asset must be on the classpath: " + aPath);
	}

	/** The registration face: block + BET + both items ride their ids (the getId() read, no .get()). */
	@Test
	void theRegistrationFaceCarriesTheIds() {
		assertEquals("measuring_pot", GT6MeasuringPot.MEASURING_POT.getId().getPath(), "the block id");
		assertEquals("measuring_pot", GT6MeasuringPot.MEASURING_POT_BE.getId().getPath(), "the BET id");
		assertEquals("measuring_pot", GT6MeasuringPot.MEASURING_POT_ITEM.getId().getPath(), "the BlockItem id");
		assertEquals("clay_measuring_pot", GT6MeasuringPot.CLAY_MEASURING_POT_RAW.getId().getPath(), "the raw id (the 997 clay band, the clay_juicer naming law)");
	}

	/** The :134 shaped row — "CkC"/"CCR" over 4 clay balls + the knife + the rolling-pin tag marks. */
	@Test
	void theShapedRowCarriesTheToolMarks() throws Exception {
		JsonObject tRecipe = generatedJson("data/gt6/recipes/clay_measuring_pot.json");
		assertEquals("minecraft:crafting_shaped", tRecipe.get("type").getAsString());
		JsonArray tPattern = tRecipe.getAsJsonArray("pattern");
		assertEquals(2, tPattern.size(), "the upstream :134 row is a 2-row form");
		assertEquals("CkC", tPattern.get(0).getAsString(), "the top row verbatim");
		assertEquals("CCR", tPattern.get(1).getAsString(), "the bottom row verbatim");
		Map<String, String> tKeys = new java.util.HashMap<>();
		for (Map.Entry<String, com.google.gson.JsonElement> tEntry : tRecipe.getAsJsonObject("key").entrySet()) {
			JsonObject tIngredient = tEntry.getValue().getAsJsonObject();
			String tFace = tIngredient.has("tag") ? tIngredient.get("tag").getAsString() : tIngredient.get("item").getAsString();
			tKeys.put(tEntry.getKey(), tFace);
		}
		assertEquals("minecraft:clay_ball", tKeys.get("C"), "C = the clay ball (4 of them = the U*4 band)");
		assertEquals("gt6:tools/knife", tKeys.get("k"), "k = the knife tool mark (the C1 tag law)");
		assertEquals("gt6:tools/rolling_pin", tKeys.get("R"), "R = the rolling-pin tool mark");
		assertEquals("gt6:clay_measuring_pot", tRecipe.getAsJsonObject("result").get("item").getAsString());
	}

	/** The :121 reverse shapeless — one raw → 4 clay balls. */
	@Test
	void theReverseRowReturnsFourClayBalls() throws Exception {
		JsonObject tRecipe = generatedJson("data/gt6/recipes/clay_measuring_pot_reverse.json");
		assertEquals("minecraft:crafting_shapeless", tRecipe.get("type").getAsString());
		JsonArray tIngredients = tRecipe.getAsJsonArray("ingredients");
		assertEquals(1, tIngredients.size());
		assertEquals("gt6:clay_measuring_pot", tIngredients.get(0).getAsJsonObject().get("item").getAsString(),
				"the raw pays the row");
		assertEquals(4, tRecipe.getAsJsonObject("result").get("count").getAsInt(), "4 clay balls back (U*4)");
		assertEquals("minecraft:clay_ball", tRecipe.getAsJsonObject("result").get("item").getAsString());
	}

	/** The Loader :2096 smelting tail — raw → block item, the 200t/0xp band face. */
	@Test
	void theSmeltTailHardensTheRaw() throws Exception {
		JsonObject tRecipe = generatedJson("data/gt6/recipes/smelt_clay_measuring_pot.json");
		assertEquals("minecraft:smelting", tRecipe.get("type").getAsString());
		assertEquals("gt6:clay_measuring_pot", tRecipe.getAsJsonObject("ingredient").get("item").getAsString());
		assertEquals("gt6:measuring_pot", tRecipe.get("result").getAsString());
		assertEquals(200, tRecipe.get("cookingtime").getAsInt(), "the standard 200t band");
	}

	/** The block model: the two-layer grammar — 10 elements (5 body + 5 overlay shells), all 8 texture refs live PNGs. */
	@Test
	void theBlockModelCarriesTheTwoLayerTub() throws Exception {
		JsonObject tModel = generatedJson("assets/gt6/models/block/measuring_pot.json");
		JsonObject tTextures = tModel.getAsJsonObject("textures");
		for (String tKey : TEXTURE_KEYS) {
			String tRef = tTextures.get(tKey).getAsString();
			assertTrue(tRef.startsWith("gt6:block/measuring_pot_"), tKey + " rides the borrowed band: " + tRef);
			assertTextureExists(tRef.substring("gt6:".length()) + ".png");
		}
		assertEquals(10, tModel.getAsJsonArray("elements").size(), "5 body boxes + 5 overlay shells");
		// the renderType: the overlay shells ride the cutout pass (the bumbliary form; the
		// forge generator serializes it as "render_type" with the minecraft: prefix)
		assertEquals("minecraft:cutout", tModel.get("render_type").getAsString());
	}

	/** The item face: the raw model over the 997 borrow + the BlockItem parent + the blockstate. */
	@Test
	void theItemFacesAndBlockstateLand() throws Exception {
		JsonObject tRaw = generatedJson("assets/gt6/models/item/clay_measuring_pot.json");
		assertEquals("minecraft:item/generated", tRaw.get("parent").getAsString());
		assertEquals("gt6:item/clay_measuring_pot", tRaw.getAsJsonObject("textures").get("layer0").getAsString());
		assertTextureExists("item/clay_measuring_pot.png");

		JsonObject tBlockItem = generatedJson("assets/gt6/models/item/measuring_pot.json");
		assertEquals("gt6:block/measuring_pot", tBlockItem.get("parent").getAsString(),
				"the BlockItem parents its block model (the addKitchenBlock form)");

		JsonObject tBlockstate = generatedJson("assets/gt6/blockstates/measuring_pot.json");
		assertNotNull(tBlockstate.getAsJsonObject("variants").get(""), "the property-free single state");
	}

	/** The lang faces: en = the :2096/:121 name columns verbatim, zh = the dump faces verbatim. */
	@Test
	void theLangFacesLandVerbatim() throws Exception {
		JsonObject tEn = generatedJson("assets/gt6/lang/en_us.json");
		assertEquals("Ceramic Measuring Pot", tEn.get("block.gt6.measuring_pot").getAsString());
		assertEquals("Clay Measuring Pot", tEn.get("item.gt6.clay_measuring_pot").getAsString());
		JsonObject tZh = generatedJson("assets/gt6/lang/zh_cn.json");
		assertEquals("陶瓷量杯", tZh.get("block.gt6.measuring_pot").getAsString(), "the dump mte 32738 face verbatim");
		assertEquals("粘土量杯", tZh.get("item.gt6.clay_measuring_pot").getAsString(), "the dump meta-997 face verbatim");
	}

	/** The nine borrowed PNGs are on the classpath (the texture-reference walk, 8 block + 1 item). */
	@Test
	void allNineBorrowsExist() {
		for (String tLayer : new String[] {"colored", "overlay"}) {
			for (String tFace : new String[] {"sides", "insides", "top", "bottom"}) {
				assertTextureExists("block/measuring_pot_" + tLayer + "_" + tFace + ".png");
			}
		}
		assertTextureExists("item/clay_measuring_pot.png");
	}
}
