package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The c4-copper-bridge pin: GT copper is the ONLY copper source (#32 suppressed the
 * vanilla copper ore blobs), so the four 1.20.1 vanilla rows that consume
 * {@code minecraft:copper_ingot} (brush / copper_block / lightning_rod / spyglass — the
 * complete crafting-INPUT census; 1.20.1 has no copper armor, doors or trapdoors) are
 * overridden onto the platform ingot tag {@code forge:ingots/copper}. The tag face needs
 * no new binding: the Forge jar's own default tag already carries
 * {@code minecraft:copper_ingot} and the port datagen adds {@code gt6:ingot_copper} —
 * vanilla (e.g. the drowned drop) and GT copper stay interchangeable inputs. The
 * copper_block override still outputs the VANILLA block, so the oxidation/wax chain
 * stays self-consistent. Reads off the CLASSPATH (src/generated/resources is a test
 * resource dir — the GT6DualDirectoryFacesTest form, working-directory independent).
 */
public class GT6VanillaCopperBridgeJsonTest {

	private static final String FORGE_TAG = "forge:ingots/copper";
	private static final String COMMON_TAG = "c:ingots/copper";

	private static String resource(String aPath) throws IOException {
		try (InputStream tStream = GT6VanillaCopperBridgeJsonTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, aPath + " must ship on the classpath");
			return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static JsonObject json(String aPath) throws IOException {
		return JsonParser.parseString(resource(aPath)).getAsJsonObject();
	}

	/** The tag face: both namespace twins carry the GT member (the loader defaults add the vanilla item at runtime). */
	@Test
	public void theCopperIngotTagCarriesThePortMember() throws Exception {
		assertEquals("gt6:ingot_copper", json("data/forge/tags/items/ingots/copper.json").getAsJsonArray("values").get(0).getAsString(),
				"the 1.20.1 material tag face carries the GT ingot (the Forge jar adds minecraft:copper_ingot)");
		assertEquals("gt6:ingot_copper", json("data/c/tags/items/ingots/copper.json").getAsJsonArray("values").get(0).getAsString(),
				"the 21.1 c: twin carries the GT ingot");
	}

	/** The override face: each vanilla row re-keyed onto the tag, pattern/category/result verbatim, zero bare copper_ingot inputs. */
	@Test
	public void everyVanillaCopperRowRidesTheIngotTag() throws Exception {
		pinShapedRow("data/minecraft/recipes/brush.json", "minecraft:brush", "equipment",
				new String[] {"X", "#", "I"},
				'#', FORGE_TAG, 'X', "minecraft:feather", 'I', "minecraft:stick");
		pinShapedRow("data/minecraft/recipes/copper_block.json", "minecraft:copper_block", "building",
				new String[] {"###", "###", "###"},
				'#', FORGE_TAG);
		pinShapedRow("data/minecraft/recipes/lightning_rod.json", "minecraft:lightning_rod", "redstone",
				new String[] {"#", "#", "#"},
				'#', FORGE_TAG);
		pinShapedRow("data/minecraft/recipes/spyglass.json", "minecraft:spyglass", "equipment",
				new String[] {" # ", " X ", " X "},
				'#', "minecraft:amethyst_shard", 'X', FORGE_TAG);
	}

	/** One override row: shaped type, book category, verbatim pattern, the given key mapping (tag path or item id), tag result. */
	private static void pinShapedRow(String aPath, String aResult, String aCategory, String[] aPattern,
			Object... aKeyPairs) throws IOException {
		JsonObject tRow = json(aPath);
		assertTrue(aPath.contains("minecraft/"), "the override face is the minecraft namespace: " + aPath);
		assertEquals("minecraft:crafting_shaped", tRow.get("type").getAsString(), aPath + " type");
		assertEquals(aCategory, tRow.get("category").getAsString(), aPath + " book category");
		assertEquals(aPattern.length, tRow.getAsJsonArray("pattern").size(), aPath + " pattern rows");
		for (int i = 0; i < aPattern.length; i++) {
			assertEquals(aPattern[i], tRow.getAsJsonArray("pattern").get(i).getAsString(), aPath + " pattern " + i);
		}
		JsonObject tKeys = tRow.getAsJsonObject("key");
		for (int i = 0; i < aKeyPairs.length; i += 2) {
			char tLetter = (Character) aKeyPairs[i];
			JsonObject tIngredient = tKeys.getAsJsonObject(String.valueOf(tLetter));
			String tExpected = (String) aKeyPairs[i + 1];
			if (FORGE_TAG.equals(tExpected)) {
				assertEquals(FORGE_TAG, tIngredient.get("tag").getAsString(), aPath + " key " + tLetter);
			} else {
				assertEquals(tExpected, tIngredient.get("item").getAsString(), aPath + " key " + tLetter);
			}
		}
		assertEquals(aResult, tRow.getAsJsonObject("result").get("item").getAsString(), aPath + " result");
		assertFalse(resource(aPath).contains("\"item\": \"minecraft:copper_ingot\""),
				aPath + " a bare copper_ingot input is the un-bridged vanilla face");
	}

	/**
	 * The 21.1 mirror face: every override row ships the adapted singular twin (c: tag,
	 * id-form result, no default-true tail). The singular CLASSPATH read is forge-leg-only:
	 * on 21.1 {@code data/minecraft/recipe/} is a union with vanilla's OWN singular band
	 * (the 1.21 client-extra ships {@code minecraft/recipe/brush.json} itself) and the
	 * classloader may resolve the vanilla face — the runtime pack stack still prefers the
	 * mod face, and the 21.1 twin is proven by the datagen_tree_check node comparison
	 * instead (the GT6DualDirectoryFacesTest minecraft-union precedent).
	 */
	@Test
	public void everyOverrideShipsTheAdapted21Face() throws Exception {
		String[] tRows = {"brush", "copper_block", "lightning_rod", "spyglass"};
		for (String tRow : tRows) {
			String tPlural = resource("data/minecraft/recipes/" + tRow + ".json");
			assertTrue(tPlural.contains(FORGE_TAG), tRow + " plural keeps the forge namespace");
			//? if forge {
			String tSingular = resource("data/minecraft/recipe/" + tRow + ".json");
			assertFalse(tSingular.contains(FORGE_TAG), tRow + " singular must not carry a forge: value");
			assertTrue(tSingular.contains(COMMON_TAG), tRow + " singular carries the c: carrier");
			assertFalse(tSingular.contains("show_notification"), tRow + " singular drops the codec-omitted default");
			JsonObject tRoot = JsonParser.parseString(tSingular).getAsJsonObject();
			JsonObject tResult = tRoot.getAsJsonObject("result");
			assertTrue(tResult.has("id") && tResult.has("count") && !tResult.has("item"),
					tRow + " singular result is the {count,id} form");
			assertEquals("minecraft:" + tRow, tResult.get("id").getAsString(), tRow + " singular result id");
			// the adapted mirror differs from the plural face ONLY through the codec-verified deltas
			String tNormalized = tPlural
					.replace("\"result\": {\n    \"item\": \"minecraft:" + tRow + "\"\n  }",
							"\"result\": {\n    \"count\": 1,\n    \"id\": \"minecraft:" + tRow + "\"\n  }")
					.replace(FORGE_TAG, COMMON_TAG)
					.replace(",\n  \"show_notification\": true", "");
			assertEquals(tNormalized, tSingular, tRow + " the mirror is the key-form delta alone");
			//?}
		}
	}

	/** The unlock-advancement overrides: the vanilla ids carry the tag criterion (the toast fires on either copper face). */
	@Test
	public void everyUnlockAdvancementCarriesTheTagCriterion() throws Exception {
		pinAdvancement("recipes/building_blocks/copper_block", "minecraft:copper_block");
		pinAdvancement("recipes/redstone/lightning_rod", "minecraft:lightning_rod");
		pinAdvancement("recipes/tools/brush", "minecraft:brush");
		pinAdvancement("recipes/tools/spyglass", "minecraft:spyglass");
	}

	private static void pinAdvancement(String aPath, String aRecipe) throws IOException {
		JsonObject tAdvancement = json("data/minecraft/advancements/" + aPath + ".json");
		assertEquals("minecraft:recipes/root", tAdvancement.get("parent").getAsString(), aPath + " parent");
		JsonObject tCriterion = tAdvancement.getAsJsonObject("criteria").getAsJsonObject("has_copper_ingot");
		JsonElement tFirst = tCriterion.getAsJsonObject("conditions").getAsJsonArray("items").get(0);
		assertEquals(FORGE_TAG, tFirst.getAsJsonObject().get("tag").getAsString(), aPath + " the tag criterion");
		assertEquals(aRecipe, tAdvancement.getAsJsonObject("criteria").getAsJsonObject("has_the_recipe")
				.getAsJsonObject("conditions").get("recipe").getAsString(), aPath + " the recipe unlock");
	}
}
