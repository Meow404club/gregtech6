package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6SlicerBlades;

/**
 * The slicer obtainability pins (task p36-recipes-obtainability, coordinator ruling B —
 * the obtainability domain = items + recipes inseparable): the census-completion item
 * face (EIGHT registered slicer items, the upstream :362-:372 meta order; the RM row0
 * {@code BLADES} subset stays the {@code sBladeTest} face) and the JSON-ship half of the
 * eight crafting rows — the MultiItemTechnological.java:364 frame row + :374-380 blade
 * rows VERBATIM ({@code 'f'} file / {@code 's'} saw per CR.java:344/:356, the ring on the
 * hollow pair only).
 */
public class GT6SlicerCraftingJsonTest {

	@BeforeAll
	static void boot() {
		try { net.minecraft.SharedConstants.tryDetectVersion(); } catch (Throwable ignored) {}
		try { net.minecraft.server.Bootstrap.bootStrap(); } catch (Throwable ignored) {}
	}

	/** The eight census ids in upstream meta order (:362 frame, :366-:372 the blades). */
	private static final String[] ALL_IDS = {
			"shape_slicer_empty", "shape_slicer_flat", "shape_slicer_grid", "shape_slicer_eigths",
			"shape_slicer_eigths_hollow", "shape_slicer_split", "shape_slicer_quarters", "shape_slicer_quarters_hollow"};

	@Test
	public void theCensusIsTheEightUpstreamItems() {
		assertEquals(8, GT6SlicerBlades.ALL.size(), "the :362-:372 census — frame + seven blades");
		for (int i = 0; i < ALL_IDS.length; i++) {
			assertEquals(ALL_IDS[i], GT6SlicerBlades.ALL.get(i).getId().getPath(), "upstream meta order position " + i);
		}
		// the RM row0 subset stays the sBladeTest identity face — the p36 census does NOT join it
		assertEquals(2, GT6SlicerBlades.BLADES.size(), "the sBladeTest face: grid + split only (the never pool holds)");
	}

	@Test
	public void theEightCraftingRowsShipVerbatimGrids() throws Exception {
		// the frame :364 — " R ","RhR"," R "
		JsonObject tFrame = generated("shape_slicer_empty");
		assertPattern(tFrame, "shape_slicer_empty", " R ", "RhR", " R ");
		JsonObject tFrameKey = tFrame.getAsJsonObject("key");
		assertEquals("gt6:stick_stainless_steel", tFrameKey.getAsJsonObject("R").get("item").getAsString(), ":364 the StainlessSteel stick column");
		assertEquals("gt6:tools/hard_hammer", tFrameKey.getAsJsonObject("h").get("tag").getAsString(), ":364 the hammer key");
		assertFalse(tFrameKey.has("O"), "the frame row carries no blade-frame key");
		// the blades :374-380 — id, row 1, row 2, row 3
		String[][] tBlades = {
				{"shape_slicer_flat",           "B f", "BO ", "B s"},
				{"shape_slicer_grid",           " Bf", "BOB", " Bs"},
				{"shape_slicer_eigths",         "B B", "s f", "BOB"},
				{"shape_slicer_eigths_hollow",  "B B", "sRf", "BOB"},
				{"shape_slicer_split",          " Of", "BBB", "  s"},
				{"shape_slicer_quarters",       "fB ", "B s", " O "},
				{"shape_slicer_quarters_hollow","fB ", "BRs", " O "}};
		for (String[] tBlade : tBlades) {
			JsonObject tJson = generated(tBlade[0]);
			assertPattern(tJson, tBlade[0], tBlade[1], tBlade[2], tBlade[3]);
			JsonObject tKey = tJson.getAsJsonObject("key");
			assertEquals("gt6:shape_slicer_empty", tKey.getAsJsonObject("O").get("item").getAsString(), tBlade[0] + ": the 'O' frame ingredient");
			assertEquals("gt6:plate_tiny_stainless_steel", tKey.getAsJsonObject("B").get("item").getAsString(), tBlade[0] + ": the 'B' plateTiny column");
			assertEquals("gt6:tools/file", tKey.getAsJsonObject("f").get("tag").getAsString(), tBlade[0] + ": the 'f' FILE key (CR.java:344)");
			assertEquals("gt6:tools/saw", tKey.getAsJsonObject("s").get("tag").getAsString(), tBlade[0] + ": the 's' SAW key (CR.java:356 — NOT the screwdriver)");
			assertEquals("gt6:" + tBlade[0], tJson.getAsJsonObject("result").get("item").getAsString(), tBlade[0] + ": the result is the blade itself");
			boolean tHollow = tBlade[0].contains("hollow");
			assertEquals(tHollow, tKey.has("R"), tBlade[0] + ": the ring key rides the hollow pair only");
			if (tHollow) {
				assertEquals("gt6:ring_stainless_steel", tKey.getAsJsonObject("R").get("item").getAsString(), tBlade[0] + ": the 'R' StainlessSteel ring");
			}
		}
	}

	private static void assertPattern(JsonObject aJson, String aId, String aL1, String aL2, String aL3) {
		JsonArray tPattern = aJson.getAsJsonArray("pattern");
		assertEquals(3, tPattern.size(), aId + ": a 3x3 grid");
		assertEquals(aL1, tPattern.get(0).getAsString(), aId + ": row 1");
		assertEquals(aL2, tPattern.get(1).getAsString(), aId + ": row 2");
		assertEquals(aL3, tPattern.get(2).getAsString(), aId + ": row 3");
	}

	private static JsonObject generated(String aId) throws Exception {
		byte[] tBytes = resourceOrNull("data/gt6/recipes/" + aId + ".json");
		assertNotNull(tBytes, "data/gt6/recipes/" + aId + ".json rides the generated-resources classpath");
		return JsonParser.parseString(new String(tBytes, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static byte[] resourceOrNull(String aPath) throws Exception {
		try (InputStream tStream = GT6SlicerCraftingJsonTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : tStream.readAllBytes();
		}
	}
}
