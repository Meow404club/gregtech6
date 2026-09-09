package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
}
