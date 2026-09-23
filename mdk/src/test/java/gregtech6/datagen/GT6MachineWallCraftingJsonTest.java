package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The shared machine-wall obtainability pins (task p36-recipes-obtainability — the
 * JSON-ship half, the GT6CrucibleLadderCensusTest.theEightWallBlocksAreObtainable form):
 * the :1143-1153 {@code "wPP","hPP"} four-plate crafting grid ships per
 * {@link gregtech6.registry.GTMultiBlocks#METAL_WALL_ROWS} block under
 * {@code data/gt6/recipes/machine_wall/<path>.json} with the wall block item as its
 * result — the crafting half of the crucible-wall card's sister gap (the WELDER face of
 * the same rows pins in GT6RecipesWelderRowTest, the resolution + table censuses). All
 * eleven ship: every wall material carries a port plate item (incl.
 * {@code plate_steel_galvanized} — the p36 datagen run disproved the stale
 * "Galvanized skips" welder note).
 */
public class GT6MachineWallCraftingJsonTest {

	@BeforeAll
	static void boot() {
		try { net.minecraft.SharedConstants.tryDetectVersion(); } catch (Throwable ignored) {}
		try { net.minecraft.server.Bootstrap.bootStrap(); } catch (Throwable ignored) {}
	}

	/**
	 * The eleven :1143-1153 rows in registration order — path → the
	 * {@code OP.plate.dat(aMat)} item id (the aMat column verbatim: Pb/Bronze/ANY.Steel/
	 * SteelGalvanized/StainlessSteel/Invar/Ti/TungstenSteel/ANY.W/Ta4HfC5/Ad).
	 */
	private static final String[][] WALLS = {
			{"machine_wall_lead"                    , "plate_lead"                    , ":1143"},
			{"machine_wall_bronze"                  , "plate_bronze"                  , ":1144"},
			{"machine_wall_steel"                   , "plate_steel"                   , ":1145"},
			{"machine_wall_galvanized_steel"        , "plate_steel_galvanized"        , ":1146"},
			{"machine_wall_stainless_steel"         , "plate_stainless_steel"         , ":1147"},
			{"machine_wall_invar"                   , "plate_invar"                   , ":1148"},
			{"machine_wall_titanium"                , "plate_titanium"                , ":1149"},
			{"machine_wall_tungstensteel"           , "plate_tungstensteel"           , ":1150"},
			{"machine_wall_tungsten"                , "plate_tungsten"                , ":1151"},
			{"machine_wall_tantalum_hafnium_carbide", "plate_tantalum_hafnium_carbide", ":1152"},
			{"machine_wall_adamantium"              , "plate_adamantium"              , ":1153"},
	};

	/** Acceptance: every wall ships the verbatim :1143-1153 crafting grid. */
	@Test
	public void theElevenWallsShipTheFourPlateGrid() throws Exception {
		for (String[] tWall : WALLS) {
			JsonObject tJson = generated("machine_wall/" + tWall[0]);
			assertEquals("wPP", tJson.getAsJsonArray("pattern").get(0).getAsString(), tWall[0] + " " + tWall[2] + " row 1");
			assertEquals("hPP", tJson.getAsJsonArray("pattern").get(1).getAsString(), tWall[0] + " " + tWall[2] + " row 2");
			JsonObject tKey = tJson.getAsJsonObject("key");
			assertEquals("gt6:" + tWall[1], tKey.getAsJsonObject("P").get("item").getAsString(), tWall[0] + ": the OP.plate.dat(aMat) column");
			assertEquals("gt6:tools/wrench", tKey.getAsJsonObject("w").get("tag").getAsString(), tWall[0] + ": the 'w' wrench key");
			assertEquals("gt6:tools/hard_hammer", tKey.getAsJsonObject("h").get("tag").getAsString(), tWall[0] + ": the 'h' hard-hammer key");
			assertEquals("gt6:" + tWall[0], tJson.getAsJsonObject("result").get("item").getAsString(), tWall[0] + ": the result is the wall itself");
		}
	}

	/** The datagen walk covers ALL eleven upstream rows (a dropped METAL_WALL_ROWS entry fails here, not silently). */
	@Test
	public void theBuilderCensusIsTheElevenUpstreamRows() {
		assertEquals(11, gregtech6.registry.GTMultiBlocks.METAL_WALL_ROWS.size(), "the :1143-1153 registration order");
	}

	private static JsonObject generated(String aId) throws Exception {
		byte[] tBytes = resourceOrNull("data/gt6/recipes/" + aId + ".json");
		assertNotNull(tBytes, "data/gt6/recipes/" + aId + ".json rides the generated-resources classpath");
		return JsonParser.parseString(new String(tBytes, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static byte[] resourceOrNull(String aPath) throws Exception {
		try (InputStream tStream = GT6MachineWallCraftingJsonTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : tStream.readAllBytes();
		}
	}
}
