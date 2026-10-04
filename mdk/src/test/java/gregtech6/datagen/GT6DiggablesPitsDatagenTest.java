/*
 * Tests for task worldgen-diggables-pits: the four colored-clay pit datagen rows, read
 * verbatim off the generated-resources classpath (the GT6ClayBandDatagenTest convention).
 *
 * <p>Compile anchors: Loader_Worldgen.java:593-597 — the four Diggables pit rows (meta
 * 1 brown / 4 yellow / 5 blue / 6 white, chance 1 divider 320; the :594 red row stays
 * F = nether-only, the GT6NetherOres nether_red_clay card's domain), WorldgenPit.java:58
 * (the 1/320 chunk gate), BlockDiggable.java:47 (IS_CLAY metas 1/3/4/5/6). The row face
 * is the pit_clay_vanilla clone family: the vanilla DISK face over the shared soil target
 * (WorldgenPit.java:67-69 replaceable set) at the codec caps (radius 7 / half_height 4),
 * the shared plains|savanna biome tag (WorldgenPit.java:58) and the LOCAL_MODIFICATIONS
 * step (the vanilla disk pass).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GT6DiggablesPitsDatagenTest {

	/** The four colored-clay pit ids, upstream row order (Loader_Worldgen.java:593/:595-597). */
	private static final List<String> COLORS = List.of("brown", "yellow", "blue", "white");

	/** The shared soil target, the WorldgenPit.java:67-69 replaceable set (the tSoil walk). */
	private static final JsonArray SOIL_TARGET = new JsonArray();
	static {
		SOIL_TARGET.add("minecraft:dirt");
		SOIL_TARGET.add("minecraft:sand");
		SOIL_TARGET.add("minecraft:red_sand");
		SOIL_TARGET.add("minecraft:gravel");
		SOIL_TARGET.add("minecraft:clay");
	}

	private static JsonObject generated(String aPath) throws IOException {
		String tPath = "/" + aPath;
		try (InputStream tStream = GT6DiggablesPitsDatagenTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	/** The four configured DISK faces: the pit_clay_vanilla clone over the per-color block state. */
	@Test
	void configuredFacesAreTheVanillaPitClones() throws IOException {
		for (String tColor : COLORS) {
			JsonObject tJson = generated("data/gt6/worldgen/configured_feature/pit_clay_" + tColor + ".json");
			assertEquals("minecraft:disk", tJson.get("type").getAsString(), tColor + ": the disk family face");
			JsonObject tConfig = tJson.getAsJsonObject("config");
			assertEquals(4, tConfig.get("half_height").getAsInt(), tColor + ": the half_height cap (DiskConfiguration.java:16)");
			assertEquals(7, tConfig.get("radius").getAsInt(), tColor + ": the radius constant (the areal deviation, codec cap 8)");
			assertEquals("gt6:" + tColor + "_clay",
					tConfig.getAsJsonObject("state_provider").getAsJsonObject("fallback")
							.getAsJsonObject("state").get("Name").getAsString(),
					tColor + ": the pit fills its own colored-clay block (the Diggables meta stand-in)");
			JsonObject tTarget = tConfig.getAsJsonObject("target");
			assertEquals("minecraft:matching_blocks", tTarget.get("type").getAsString(), tColor + ": the matching-blocks target");
			assertEquals(SOIL_TARGET, tTarget.get("blocks"), tColor + ": the WorldgenPit.java:67-69 replaceable set");
		}
	}

	/** The four placed faces: the 1/320 chunk gate (WorldgenPit.java:58) over the vanilla disk anchor chain. */
	@Test
	void placedFacesCarryTheChunkGate() throws IOException {
		for (String tColor : COLORS) {
			JsonObject tJson = generated("data/gt6/worldgen/placed_feature/pit_clay_" + tColor + ".json");
			assertEquals("gt6:pit_clay_" + tColor, tJson.get("feature").getAsString(), tColor + ": the path-direct feature key");
			JsonArray tPlacement = tJson.getAsJsonArray("placement");
			assertEquals(4, tPlacement.size(), tColor + ": rarity + square + heightmap + biome (the pit_clay_vanilla chain)");
			JsonObject tRarity = tPlacement.get(0).getAsJsonObject();
			assertEquals("minecraft:rarity_filter", tRarity.get("type").getAsString(), tColor + ": the chunk gate rides the rarity filter");
			assertEquals(320, tRarity.get("chance").getAsInt(), tColor + ": WorldgenPit.java:58 nextInt(320) > 0 — the 1/320 gate");
			assertEquals("minecraft:heightmap", tPlacement.get(2).getAsJsonObject().get("type").getAsString(), tColor + ": the heightmap anchor");
			assertEquals("MOTION_BLOCKING", tPlacement.get(2).getAsJsonObject().get("heightmap").getAsString(), tColor + ": the MOTION_BLOCKING face");
		}
	}

	/** The eight brand modifier faces: one per brand per row, the shared tag, the disk step. */
	@Test
	void biomeModifiersHangOffTheSharedTag() throws IOException {
		for (String tBrand : List.of("forge", "neoforge")) {
			for (String tColor : COLORS) {
				JsonObject tJson = generated("data/gt6/" + tBrand + "/biome_modifier/pit_clay_" + tColor + ".json");
				assertEquals(tBrand + ":add_features", tJson.get("type").getAsString(), tColor + "/" + tBrand + ": the add_features face");
				assertEquals("#gt6:surface_pit_clay", tJson.get("biomes").getAsString(),
						tColor + "/" + tBrand + ": the shared plains|savanna tag (WorldgenPit.java:58, one tag for the pit family)");
				assertEquals("gt6:pit_clay_" + tColor, tJson.get("features").getAsString(), tColor + "/" + tBrand + ": the row's own placed feature");
				assertEquals("local_modifications", tJson.get("step").getAsString(), tColor + "/" + tBrand + ": the vanilla disk pass");
			}
		}
	}
}
