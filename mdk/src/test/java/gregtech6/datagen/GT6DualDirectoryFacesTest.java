package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The dual-directory faces pin (task p26-w1-press-extruder-molds): the generated tree ships
 * the data faces at BOTH directory spellings — the 1.20.1 plural form AND the 1.21
 * singular-registry aliases (GT6DualDirectoryFaces, the mirror provider). The r2 live
 * finding: the plural-only tree was structurally dead on the 1.21.1 loader (the 1.21
 * client-extra jar carries data/minecraft/tags/item/*.json with ZERO files under
 * tags/items/), the mold tag resolved empty on the neo server and the crown mold was
 * consumed. The reads go off the CLASSPATH (the GT6ExtruderMoldsTest form —
 * src/generated/resources is a test resource dir, working-directory independent).
 */
public class GT6DualDirectoryFacesTest {

	private static String resource(String aPath) throws IOException {
		try (InputStream tStream = GT6DualDirectoryFacesTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, aPath + " must ship on the classpath");
			return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** The null-reading twin (the graft-absence arm reads through the same classpath face). */
	private static String resourceOrNull(String aPath) throws IOException {
		try (InputStream tStream = GT6DualDirectoryFacesTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** Both faces exist for the four renamed families, spot-pinned on this card's own files. */
	@Test
	public void singularAliasesShipBesideThePluralFaces() throws Exception {
		// the crown face itself: the mold family tag, plural (1.20.1) and singular (1.21+)
		assertNotNull(resource("data/gt6/tags/items/extruder_shapes.json"));
		assertNotNull(resource("data/gt6/tags/item/extruder_shapes.json"));
		// the crafting face: the plate-mold row, plural and singular
		assertNotNull(resource("data/gt6/recipes/shape_extruder_plate.json"));
		assertNotNull(resource("data/gt6/recipe/shape_extruder_plate.json"));
		// the loot face: a machine block self-drop, plural and singular
		assertNotNull(resource("data/gt6/loot_tables/blocks/extruder.json"));
		assertNotNull(resource("data/gt6/loot_table/blocks/extruder.json"));
		// the vanilla-join face: the mineable band override, plural and singular
		assertNotNull(resource("data/minecraft/tags/blocks/mineable/pickaxe.json"));
		assertNotNull(resource("data/minecraft/tags/block/mineable/pickaxe.json"));
	}

	/** The alias content is the provider-produced identity: the mold tag mirrors byte-for-byte. */
	@Test
	public void theMoldTagAliasMirrorsThePluralContent() throws Exception {
		String tPlural = resource("data/gt6/tags/items/extruder_shapes.json");
		String tSingular = resource("data/gt6/tags/item/extruder_shapes.json");
		assertEquals(tPlural, tSingular, "the alias is the same provider-produced JSON");
		assertTrue(tSingular.contains("gt6:shape_extruder_plate"), "the plate mold stays a member");
		assertTrue(tSingular.contains("gt6:shape_extruder_rod"), "the rod mold stays a member");
	}

	/**
	 * The per-leg c: face contract (task p28-neo-tag-wiring, the ADR-P17-1 re-ruling): the
	 * neoforgeTagFaces Copy task grafts the platform material band onto the singular datapack
	 * face — data/forge/tags/items/** and the p27 forward twin data/c/tags/items/** re-land as
	 * data/c/tags/item/** (namespace forge→c, directory items→item, family/file names STAY
	 * plural — 24w21a singularized only the directory segment; content byte-identical, the
	 * build-side closure of the forge→c remap the provider above deliberately left out of
	 * datagen scope). The forge leg ships NOTHING there: its runtime reads the plural
	 * tags/items face and the canonical tracked tree stays untouched (the tree_check face).
	 */
	@Test
	public void theCMaterialGraftFaceHoldsPerLeg() throws Exception {
		//? if forge {
		// the graft is the NEO leg's build face — the forge leg must NOT ship data/c/tags/item/**
		assertNull(resourceOrNull("data/c/tags/item/ingots/iron.json"),
				"the singular graft face must not ship on the forge leg");
		assertNull(resourceOrNull("data/c/tags/item/gems/quartz.json"),
				"the singular graft face must not ship on the forge leg");
		// the canonical tree is zero-disturbed: both plural faces stay exactly as tracked
		assertNotNull(resource("data/forge/tags/items/ingots/iron.json"));
		assertNotNull(resource("data/c/tags/items/ingots/iron.json"));
		//?} else {
		/*// the canonical plural faces stay on the classpath (the canonical tree is zero-disturbed)
		String tForgeIngots = resource("data/forge/tags/items/ingots/iron.json");
		assertNotNull(resource("data/c/tags/items/ingots/iron.json"), "the p27 forward twin band stays");
		// the graft lands at the mapped path — namespace forge→c, directory items→item, names plural
		String tGraftIngots = resource("data/c/tags/item/ingots/iron.json");
		assertEquals(tForgeIngots, tGraftIngots, "the grafted face is byte-source identical to the forge band");
		assertTrue(tGraftIngots.contains("gt6:ingot_iron"), "the port member stays");
		// nested relative paths map 1:1 and the twin origin band dedups to the same bytes
		assertEquals(resource("data/forge/tags/items/storage_blocks/iron.json"),
				resource("data/c/tags/item/storage_blocks/iron.json"), "nested storage_blocks maps 1:1");
		assertEquals(resource("data/forge/tags/items/gems/quartz.json"),
				resource("data/c/tags/item/gems/quartz.json"), "the quartz canonical-name face maps 1:1");
		*///?}
	}

	/**
	 * The loot face 1.21.1 adapter spot pin (task p28-neo-loot-copy-custom-data): the mirrored
	 * singular loot JSON carries {@code minecraft:copy_custom_data} (the 1.21.1 registration,
	 * LootItemFunctions.java:49) while the plural face stays the canonical {@code copy_nbt}
	 * form, and the adapted mirror is the FUNCTION-NAME DELTA ALONE — the codec-verified full
	 * form (source/ops/path identical across the legs; the carrier moves from the tag NBT to
	 * the {@code minecraft:custom_data} component invisibly to the JSON). The tables sampled:
	 * the two foam pipes (the RCON E-arm subjects, the 5-op form) and painted machine faces
	 * (the 2-op form over every tier incl. T4 + the ACT row).
	 */
	@Test
	public void theLootFaceAliasCarriesThe21FunctionForm() throws Exception {
		String[] tTables = {"wood_fluid_pipe_small", "wood_fluid_pipe_medium",
				"lathe", "lathe_t2", "lathe_t3", "lathe_t4", "canner", "press", "advanced_crafting_table"};
		for (String tTable : tTables) {
			String tPlural = resource("data/gt6/loot_tables/blocks/" + tTable + ".json");
			String tSingular = resource("data/gt6/loot_table/blocks/" + tTable + ".json");
			assertTrue(tPlural.contains("minecraft:copy_nbt"), tTable + " plural stays the canonical 1.20.1 form");
			assertFalse(tPlural.contains("minecraft:copy_custom_data"), tTable + " plural must not carry the 21 form");
			assertTrue(tSingular.contains("minecraft:copy_custom_data"), tTable + " mirror carries the 1.21.1 form");
			assertFalse(tSingular.contains("minecraft:copy_nbt"), tTable + " mirror must not carry the dead 1.20 name");
			assertEquals(tPlural.replace("\"minecraft:copy_nbt\"", "\"minecraft:copy_custom_data\""), tSingular,
					tTable + " the adapted mirror is the function-name delta alone");
			assertTrue(tSingular.contains("\"BlockEntityTag."), tTable + " the op target keeps the relative path");
		}
	}

	/**
	 * The full loot band sweep (task p28-neo-loot-copy-custom-data): EVERY mirrored singular
	 * loot JSON equals its plural twin with EXACTLY the codec-verified function rename —
	 * files without {@code copy_nbt} stay byte-identical (the pre-existing identity
	 * contract), files with it differ in the one string. Zero {@code copy_nbt} may survive
	 * anywhere in the singular band: one survivor is one boot-time {@code LootDataType}
	 * parse death = zero drops for that block (the 51-table outage this card closes).
	 *
	 * <p>The sweep scopes to the {@code gt6} namespace — the provider's own produced band
	 * and the only namespace the adapter contract governs. The {@code minecraft} namespace
	 * is NOT walkable as a twin-mirror domain on the 21.1 leg: its classpath face is a
	 * union that spans vanilla's own 1.21 data pack (natively singular, e.g.
	 * {@code minecraft/loot_table/spawners/trial_chamber/key.json}), which never had a
	 * plural twin and never rode this provider.
	 */
	@Test
	public void everyLootAliasIsTheAdaptedPluralFace() throws Exception {
		ClassLoader tLoader = GT6DualDirectoryFacesTest.class.getClassLoader();
		// both band roots resolve DIRECTLY — on the 21.1 leg the classpath face is a union
		// filesystem, and only a top-level getResource lands on a concrete member entry
		// (a manual sub-resolve off the data root throws NoSuchFile there).
		URL tSingularBandUrl = tLoader.getResource("data/gt6/loot_table");
		URL tPluralBandUrl = tLoader.getResource("data/gt6/loot_tables");
		assertNotNull(tSingularBandUrl, "the singular loot band ships on the classpath");
		assertNotNull(tPluralBandUrl, "the plural loot band ships on the classpath");
		Path tSingularRoot = Paths.get(tSingularBandUrl.toURI());
		Path tPluralRoot = Paths.get(tPluralBandUrl.toURI());
		List<Path> tTables = new ArrayList<>(0);
		try (Stream<Path> tWalk = Files.walk(tSingularRoot)) {
			tWalk.filter(tPath -> tPath.toString().endsWith(".json")).forEach(tTables::add);
		}
		assertFalse(tTables.isEmpty(), "the singular loot band ships");
		int tAdapted = 0;
		for (Path tSingular : tTables) {
			String tSingularText = Files.readString(tSingular);
			String tPluralText = Files.readString(tPluralRoot.resolve(tSingularRoot.relativize(tSingular)));
			// task p30-ore-5-census dialect tolerance: the singular band carries the 21.1
			// predicate dialect — the census LIVE finding was that the old byte-identity
			// contract shipped 1.20.1 match_tool shapes into the 1.21.1 loader, where they
			// parse as an EMPTY ItemPredicate (the RecordCodecBuilder silently drops the
			// unknown "enchantments" key) = an always-true tool match = the fortune arm
			// firing on bare hands. The identity contract is therefore judged on the
			// DIALECT-NORMALIZED trees (the tools/datagen_tree_check.py match_tool
			// normalizer, test-side), not on bytes.
			assertEquals(dialectNormalize(JsonParser.parseString(tPluralText)),
					dialectNormalize(JsonParser.parseString(tSingularText)),
					"gt6/loot_table/" + tSingularRoot.relativize(tSingular) + " differs beyond the codec-verified rename");
			assertFalse(tSingularText.contains("minecraft:copy_nbt"),
					"gt6/loot_table/" + tSingularRoot.relativize(tSingular) + " a survivor is a boot-time LootDataType parse death");
			if (tPluralText.contains("minecraft:copy_nbt")) tAdapted++;
		}
		assertTrue(tAdapted >= 51, "the 51-table paint/foam carry band rides the adapter (got " + tAdapted + ")");
	}

	/**
	 * The dual-dialect fold (task p30-ore-5-census): one comparable tree from either
	 * loader dialect — ① the value rename {@code minecraft:copy_nbt} → {@code
	 * minecraft:copy_custom_data} (the p28 adapter), ② the match_tool predicate
	 * reshape: 1.20.1 (plural) {@code "enchantments": [{"enchantment": X, ...}]} vs
	 * 1.21.1 (singular) {@code "predicates": {"minecraft:enchantments": [{"enchantments":
	 * X, ...}]}} (ItemPredicate.java:29 optionalFieldOf("predicates") — the p28-era
	 * byte contract predates the first match_tool tables in the singular band), ③ the
	 * item-id scalar/array tolerance ({@code "items": "gt6:x"} vs {@code ["gt6:x"]},
	 * the 1.21 single-element HolderSet face).
	 */
	private static JsonElement dialectNormalize(JsonElement aElement) {
		if (aElement.isJsonObject()) {
			JsonObject tObject = aElement.getAsJsonObject();
			JsonObject rObject = new JsonObject();
			for (Map.Entry<String, JsonElement> tEntry : tObject.entrySet()) {
				String tKey = tEntry.getKey();
				JsonElement tValue = dialectNormalize(tEntry.getValue());
				// ② 1.21.1 predicate map -> the 1.20.1 enchantment list; the entry
				// key folds too ("enchantments": "minecraft:fortune" -> "enchantment": ...)
				if (tKey.equals("predicates") && tValue.isJsonObject()
						&& tValue.getAsJsonObject().has("minecraft:enchantments")) {
					JsonElement tList = tValue.getAsJsonObject().get("minecraft:enchantments");
					if (tList.isJsonArray()) {
						com.google.gson.JsonArray rList = new com.google.gson.JsonArray();
						for (JsonElement tCond : tList.getAsJsonArray()) {
							if (tCond.isJsonObject() && tCond.getAsJsonObject().has("enchantments")
									&& !tCond.getAsJsonObject().has("enchantment")) {
								JsonObject tFolded = new JsonObject();
								for (Map.Entry<String, JsonElement> tRest : tCond.getAsJsonObject().entrySet()) {
									tFolded.add(tRest.getKey().equals("enchantments") ? "enchantment" : tRest.getKey(),
											tRest.getValue());
								}
								rList.add(tFolded);
							} else {
								rList.add(tCond);
							}
						}
						rObject.add("enchantments", rList);
						continue;
					}
					rObject.add("enchantments", tList);
					continue;
				}
				// ③ the 1.21 scalar item id -> the 1.20.1 single-element array
				if (tKey.equals("items") && tValue.isJsonPrimitive()) {
					com.google.gson.JsonArray tItems = new com.google.gson.JsonArray();
					tItems.add(tValue);
					rObject.add(tKey, tItems);
					continue;
				}
				rObject.add(tKey, tValue);
			}
			return rObject;
		}
		if (aElement.isJsonArray()) {
			com.google.gson.JsonArray rArray = new com.google.gson.JsonArray();
			for (JsonElement tItem : aElement.getAsJsonArray()) rArray.add(dialectNormalize(tItem));
			return rArray;
		}
		if (aElement.isJsonPrimitive() && aElement.getAsJsonPrimitive().isString()
				&& aElement.getAsString().equals("minecraft:copy_nbt")) {
			return new com.google.gson.JsonPrimitive("minecraft:copy_custom_data"); // ① the p28 adapter rename
		}
		return aElement;
	}

	/**
	 * The biome-modifier dual-brand faces (task p30-ops-biome-modifier-dual-dir, decisions
	 * .p26-worldgen-biome-modifier-dual-dir plan a): the band's directory follows the
	 * REGISTRY-KEY namespace (forge:biome_modifier → data/gt6/forge/biome_modifier/,
	 * ForgeRegistries.java:195; neoforge:biome_modifier → data/gt6/neoforge/biome_modifier/,
	 * NeoForgeRegistries.java:61-66, the directory derivation Registries.java:251-253), and
	 * the r2 live finding was the neoforge face MISSING from the shared tree — 1444
	 * forceloaded chunks, zero GT6 stones, zero log errors (a foreign-brand directory is
	 * simply not scanned by a loader). The mirror face (GT6DualDirectoryFaces
	 * .mirrorBiomeModifiers) ships BOTH brands from the ONE canonical producer.
	 */
	@Test
	public void biomeModifierDualFacesShipPerBrand() throws Exception {
		ClassLoader tLoader = GT6DualDirectoryFacesTest.class.getClassLoader();
		URL tForgeBand = tLoader.getResource("data/gt6/forge/biome_modifier");
		URL tNeoforgeBand = tLoader.getResource("data/gt6/neoforge/biome_modifier");
		assertNotNull(tForgeBand, "the forge brand face ships on the classpath");
		assertNotNull(tNeoforgeBand, "the neoforge brand face ships on the classpath (the r2 structural fix)");
		assertEquals(countJson(tForgeBand), countJson(tNeoforgeBand),
				"the brand faces are same-source twins: equal member counts");
		assertTrue(countJson(tForgeBand) >= 17, "the 17 stone-blob modifiers ship (got " + countJson(tForgeBand) + ")");
	}

	/**
	 * The full biome-modifier sweep: EVERY neoforge-brand face equals its forge-brand twin
	 * with EXACTLY the registry-brand type swap ({@code forge:add_features} ↔
	 * {@code neoforge:add_features} — the census-proven single-key delta over
	 * GT6WorldgenDatagen's AddFeaturesBiomeModifier legs) — biomes/features/step identical.
	 * Zero foreign-brand survivors on either side: one survivor is one boot-time parse
	 * failure on the loader that owns that directory (the silent-zero bug class).
	 */
	@Test
	public void everyBiomeModifierBrandFaceIsTheTypeDeltaAlone() throws Exception {
		ClassLoader tLoader = GT6DualDirectoryFacesTest.class.getClassLoader();
		URL tForgeBand = tLoader.getResource("data/gt6/forge/biome_modifier");
		URL tNeoforgeBand = tLoader.getResource("data/gt6/neoforge/biome_modifier");
		assertNotNull(tForgeBand, "the forge brand face ships on the classpath");
		assertNotNull(tNeoforgeBand, "the neoforge brand face ships on the classpath");
		Path tForgeRoot = Paths.get(tForgeBand.toURI());
		Path tNeoforgeRoot = Paths.get(tNeoforgeBand.toURI());
		List<Path> tForgeFiles = new ArrayList<>(0);
		try (Stream<Path> tWalk = Files.walk(tForgeRoot)) {
			tWalk.filter(tPath -> tPath.toString().endsWith(".json")).forEach(tForgeFiles::add);
		}
		assertFalse(tForgeFiles.isEmpty(), "the forge brand face ships members");
		for (Path tForge : tForgeFiles) {
			String tForgeText = Files.readString(tForge);
			String tNeoforgeText = Files.readString(tNeoforgeRoot.resolve(tForgeRoot.relativize(tForge)));
			assertTrue(tForgeText.contains("\"forge:add_features\""),
					tForge.getFileName() + " the forge face carries the forge brand");
			assertFalse(tForgeText.contains("neoforge:add_features"),
					tForge.getFileName() + " the forge face carries no foreign brand");
			assertTrue(tNeoforgeText.contains("\"neoforge:add_features\""),
					tForge.getFileName() + " the neoforge face carries the neoforge brand");
			// the negative arm pins the QUOTED token: the bare string is a substring of
			// the neoforge brand itself ("neoforge:add_features".contains("forge:..."))
			assertFalse(tNeoforgeText.contains("\"forge:add_features\""),
					tForge.getFileName() + " the neoforge face carries no foreign brand");
			assertEquals(tForgeText.replace("\"forge:add_features\"", "\"neoforge:add_features\""), tNeoforgeText,
					tForge.getFileName() + " the brand mirror is the type-key delta alone");
		}
	}

	/** The .json member count under a band classpath root (the union-FS-safe top-level form). */
	private static int countJson(URL aBandUrl) throws Exception {
		try (Stream<Path> tWalk = Files.walk(Paths.get(aBandUrl.toURI()))) {
			return (int) tWalk.filter(tPath -> tPath.toString().endsWith(".json")).count();
		}
	}

	/**
	 * The recipe face 1.21.1 key-form adapter spot pin (task p30-pool-recipe-key-form): the
	 * mirrored singular recipe JSON carries the ItemStack id-form result (1.21.1
	 * ItemStack.java:103-126 — {@code id} fieldOf + {@code count} optionalFieldOf(1), riding
	 * ShapedRecipe.java:96 / ShapelessRecipe.java:86 / SimpleCookingSerializer.java:23), the
	 * codec-omitted {@code show_notification} default is gone, and the platform tags read
	 * {@code c:} (the 21.1 runtime tag carrier). The plural face stays the 1.20.1 form —
	 * the forge runtime never scans the singular directory, so its face is untouched.
	 * The before-fix live run: ALL 225 rows died with "Parsing error loading recipe".
	 */
	@Test
	public void theRecipeFaceAliasCarriesThe21KeyForms() throws Exception {
		// the shaped row: result id-form (count first), the default-true tail dropped,
		// the gt6 ingredient tags ride UNTOUCHED (the 1.21.1 ingredient codec keeps item/tag)
		String tPlural = resource("data/gt6/recipes/spray_can_empty.json");
		String tSingular = resource("data/gt6/recipe/spray_can_empty.json");
		assertTrue(tPlural.contains("\"result\": {\n    \"item\": \"gt6:spray_can_empty\"\n  }")
				&& tPlural.contains("\"show_notification\": true"), "plural stays the 1.20.1 form");
		assertTrue(tSingular.contains("\"result\": {\n    \"count\": 1,\n    \"id\": \"gt6:spray_can_empty\"\n  }"),
				"the mirror carries the id-form result (count first)");
		assertFalse(tSingular.contains("show_notification"), "the codec-omitted default true rides nowhere");
		assertTrue(tSingular.contains("\"tag\": \"gt6:plate_curved_tin\""), "the gt6 ingredient tags ride untouched");
		// the count>1 shapeless row: the count value survives
		assertTrue(resource("data/gt6/recipe/grass.json")
				.contains("\"result\": {\n    \"count\": 8,\n    \"id\": \"gt6:grass\"\n  }"), "count>1 survives");
		// the smelting row: the 1.20.1 bare-id string becomes the object form, ingredient untouched
		String tSmeltPlural = resource("data/gt6/recipes/smelt_mold_ceramic_sense.json");
		String tSmeltSingular = resource("data/gt6/recipe/smelt_mold_ceramic_sense.json");
		assertTrue(tSmeltPlural.contains("\"result\": \"gt6:mold_ceramic_sense\""), "plural keeps the bare-id form");
		assertTrue(tSmeltSingular.contains("\"result\": {\n    \"count\": 1,\n    \"id\": \"gt6:mold_ceramic_sense\"\n  }"),
				"the cooking row mirror carries the object id-form");
		assertFalse(tSmeltSingular.contains("\"item\": \"gt6:mold_ceramic_sense\""),
				"no id-form leak into a bare string; the _raw ingredient is the only item");
		// the platform-tag row: forge: → c: (a forge: survivor would resolve EMPTY on 21.1)
		tPlural = resource("data/gt6/recipes/axe.json");
		tSingular = resource("data/gt6/recipe/axe.json");
		assertTrue(tPlural.contains("\"tag\": \"forge:plates/steel\""), "plural keeps the forge namespace");
		assertTrue(tSingular.contains("\"tag\": \"c:plates/steel\""), "the mirror reads the c: carrier");
	}

	/**
	 * The full recipe band sweep (task p30-pool-recipe-key-form): EVERY mirrored singular
	 * recipe JSON is the 1.21.1 key form — zero {@code forge:} tag values, zero
	 * {@code show_notification} keys, every result the {@code {count,id}} object. One
	 * survivor of any of the three is one boot-time RecipeManager parse death (the
	 * 225-row outage this adapter closes). Pairs 1:1 with the plural band by name, and
	 * ingredient shapes ride untouched (item/tag keys stay, only VALUES never rename).
	 */
	@Test
	public void everyRecipeAliasIsThe21KeyForm() throws Exception {
		ClassLoader tLoader = GT6DualDirectoryFacesTest.class.getClassLoader();
		URL tSingularBand = tLoader.getResource("data/gt6/recipe");
		URL tPluralBand = tLoader.getResource("data/gt6/recipes");
		assertNotNull(tSingularBand, "the singular recipe band ships on the classpath");
		assertNotNull(tPluralBand, "the plural recipe band ships on the classpath");
		Path tSingularRoot = Paths.get(tSingularBand.toURI());
		Path tPluralRoot = Paths.get(tPluralBand.toURI());
		List<Path> tRows = new ArrayList<>(0);
		try (Stream<Path> tWalk = Files.walk(tSingularRoot)) {
			tWalk.filter(tPath -> tPath.toString().endsWith(".json")).forEach(tRows::add);
		}
		assertTrue(tRows.size() >= 225, "the full gt6 crafting universe ships (got " + tRows.size() + ")");
		int tSmelting = 0;
		for (Path tRow : tRows) {
			String tName = "gt6/recipe/" + tSingularRoot.relativize(tRow);
			assertTrue(Files.exists(tPluralRoot.resolve(tSingularRoot.relativize(tRow))),
					tName + " pairs with a plural twin");
			JsonObject tRoot = JsonParser.parseString(Files.readString(tRow)).getAsJsonObject();
			assertFalse(tRoot.has("show_notification"), tName + " carries the codec-omitted default");
			if ("minecraft:smelting".equals(tRoot.get("type").getAsString())) tSmelting++;
			// the result member: the object id-form only
			JsonObject tResult = tRoot.getAsJsonObject("result");
			assertNotNull(tResult, tName + " result is the object form (no bare-id string survivor)");
			assertTrue(tResult.get("id") != null && tResult.get("id").isJsonPrimitive()
					&& tResult.get("count") != null && tResult.get("count").isJsonPrimitive()
					&& tResult.get("item") == null, tName + " result is {count,id}, never item");
			// the whole tree: no forge: tag value anywhere
			assertNoForgeTagValue(tRoot, tName);
		}
		assertTrue(tSmelting >= 33, "the ceramic-mold cooking band rides the bare-id adapter (got " + tSmelting + ")");
	}

	/** The recursive arm: every {@code tag} VALUE must be off the forge: namespace. */
	private static void assertNoForgeTagValue(JsonElement aJson, String aName) {
		if (aJson.isJsonObject()) {
			for (Map.Entry<String, JsonElement> tMember : aJson.getAsJsonObject().entrySet()) {
				if ("tag".equals(tMember.getKey()) && tMember.getValue().isJsonPrimitive()) {
					assertFalse(tMember.getValue().getAsString().startsWith("forge:"),
							aName + " a forge: survivor resolves EMPTY on the 21.1 loader");
				}
				assertNoForgeTagValue(tMember.getValue(), aName);
			}
		} else if (aJson.isJsonArray()) {
			for (JsonElement tElement : aJson.getAsJsonArray()) {
				assertNoForgeTagValue(tElement, aName);
			}
		}
	}
}
