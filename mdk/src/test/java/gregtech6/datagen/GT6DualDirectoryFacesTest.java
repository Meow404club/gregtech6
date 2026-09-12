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
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

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
			String tExpected = tPluralText.replace("\"minecraft:copy_nbt\"", "\"minecraft:copy_custom_data\"");
			assertEquals(tExpected, tSingularText,
					"gt6/loot_table/" + tSingularRoot.relativize(tSingular) + " differs beyond the codec-verified rename");
			assertFalse(tSingularText.contains("minecraft:copy_nbt"),
					"gt6/loot_table/" + tSingularRoot.relativize(tSingular) + " a survivor is a boot-time LootDataType parse death");
			if (!tExpected.equals(tPluralText)) tAdapted++;
		}
		assertTrue(tAdapted >= 51, "the 51-table paint/foam carry band rides the adapter (got " + tAdapted + ")");
	}
}
