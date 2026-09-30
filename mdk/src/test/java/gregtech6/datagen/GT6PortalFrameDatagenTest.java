/**
 * Offline pin for task 23a-portal-frame (issue #23) — the miniature portals carry the
 * faithful transcription of the upstream 13-pass render
 * (MultiTileEntityMiniPortal.java:273-323) instead of the old frame/portal cube swap:
 * inactive = the twelve 2x2px edge beams ONLY (the hollow see-through cage — the
 * inactive pass-0 arm renders nothing, :318/:323), active = the same cage + the 1px
 * inset portal face cube (:283). Also pins the End frame texture correction
 * (end_portal_frame_top, End.java:128 — the old end_stone stand-in was a mis-borrow)
 * and the nether-portal translucent layer (ItemBlockRenderTypes.java:269-271). Reads
 * the committed generated tree (the {@link GT6FallenLogBlockstateTest} classpath form —
 * no registry, no datagen run).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6PortalFrameDatagenTest {

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6PortalFrameDatagenTest.class.getClassLoader()
				.getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
	}

	private static void assertCoords(JsonObject aElement, String aKey, double... aExpected) {
		JsonElement tArray = aElement.get(aKey);
		assertNotNull(tArray, "the element must carry " + aKey);
		assertEquals(aExpected.length, tArray.getAsJsonArray().size());
		for (int i = 0; i < aExpected.length; i++) {
			assertEquals(aExpected[i], tArray.getAsJsonArray().get(i).getAsDouble(),
					aKey + "[" + i + "]");
		}
	}

	/** The two ACTIVE blockstate variants point at the frame cage / the inset-face model. */
	@Test
	void blockstatesSwapBetweenCageAndInsetFaceModels() throws Exception {
		for (String tSlug : new String[] {"mini_portal_nether", "mini_portal_end"}) {
			JsonObject tVariants = generatedJson("assets/gt6/blockstates/" + tSlug + ".json")
					.getAsJsonObject("variants");
			assertEquals(2, tVariants.size(), tSlug + ": exactly the two ACTIVE variants");
			assertEquals("gt6:block/" + tSlug + "_frame",
					tVariants.getAsJsonObject("active=false").get("model").getAsString());
			assertEquals("gt6:block/" + tSlug + "_portal",
					tVariants.getAsJsonObject("active=true").get("model").getAsString());
		}
	}

	/** The frame texture pins: obsidian for the Nether, the end_portal_frame TOP face for End (the mis-borrow fix). */
	@Test
	void frameTexturesAreObsidianAndEndPortalFrameTop() throws Exception {
		assertEquals("minecraft:block/obsidian", generatedJson("assets/gt6/models/block/mini_portal_nether_frame.json")
				.getAsJsonObject("textures").get("frame").getAsString(), "Nether.java:133 — the obsidian frame");
		assertEquals("minecraft:block/end_portal_frame_top", generatedJson("assets/gt6/models/block/mini_portal_end_frame.json")
				.getAsJsonObject("textures").get("frame").getAsString(), "End.java:128 — the end_portal_frame top face");
	}

	/** The inactive models are the 12-beam cage with NO portal face (the hollow form). */
	@Test
	void inactiveModelsAreTwelveBeamsWithoutTheFace() throws Exception {
		for (String tSlug : new String[] {"mini_portal_nether", "mini_portal_end"}) {
			JsonObject tModel = generatedJson("assets/gt6/models/block/" + tSlug + "_frame.json");
			assertEquals(12, tModel.getAsJsonArray("elements").size(),
					tSlug + ": the twelve edge beams (upstream passes 1-12)");
			assertFalse(tModel.getAsJsonObject("textures").has("portal"),
					tSlug + ": the inactive arm renders no face (upstream :318/:323)");
			// the hull cullface spot (the X edge at y0/z0): down and north sit on the hull,
			// up and south are interior cage faces and must always draw
			JsonObject tBeam = tModel.getAsJsonArray("elements").get(0).getAsJsonObject();
			assertCoords(tBeam, "from", 0, 0, 0);
			assertCoords(tBeam, "to", 16, 2, 2);
			JsonObject tFaces = tBeam.getAsJsonObject("faces");
			assertEquals("down", tFaces.getAsJsonObject("down").get("cullface").getAsString());
			assertEquals("north", tFaces.getAsJsonObject("north").get("cullface").getAsString());
			assertFalse(tFaces.getAsJsonObject("up").has("cullface"), "the interior up face never culls");
			assertFalse(tFaces.getAsJsonObject("south").has("cullface"), "the interior south face never culls");
			assertEquals("#frame", tFaces.getAsJsonObject("down").get("texture").getAsString());
		}
	}

	/** The active models are the 13 elements: cage + the 1px-inset face cube (upstream pass 0). */
	@Test
	void activeModelsAddTheInsetPortalFace() throws Exception {
		for (String tSlug : new String[] {"mini_portal_nether", "mini_portal_end"}) {
			JsonObject tModel = generatedJson("assets/gt6/models/block/" + tSlug + "_portal.json");
			assertEquals(13, tModel.getAsJsonArray("elements").size(),
					tSlug + ": twelve beams + the inset face (the 13-pass count)");
			// exactly one element is the PX_P[1]..PX_N[1] face cube (:283), all-#portal, no cullface
			int tFaceCubes = 0;
			for (JsonElement tEntry : tModel.getAsJsonArray("elements")) {
				JsonObject tElement = tEntry.getAsJsonObject();
				if (tElement.get("from").getAsJsonArray().get(0).getAsDouble() == 1) {
					tFaceCubes++;
					assertCoords(tElement, "from", 1, 1, 1);
					assertCoords(tElement, "to", 15, 15, 15);
					JsonObject tFaces = tElement.getAsJsonObject("faces");
					assertEquals(6, tFaces.size(), "the inset face cube renders all six sides");
					for (JsonElement tFace : tFaces.getAsJsonObject().entrySet().stream()
							.map(java.util.Map.Entry::getValue).toList()) {
						assertEquals("#portal", tFace.getAsJsonObject().get("texture").getAsString());
						assertFalse(tFace.getAsJsonObject().has("cullface"),
								"the inset face sits inside the block — cullface would punch holes");
					}
				}
			}
			assertEquals(1, tFaceCubes, tSlug + ": exactly the one inset face cube");
			assertTrue(tModel.getAsJsonObject("textures").has("portal"), tSlug + ": the face texture is present");
		}
	}

	/** The face texture pins: the animated vanilla nether_portal / the owned near-black mini_portal_end.png. */
	@Test
	void portalFaceTexturesCarryTheExistingArt() throws Exception {
		assertEquals("minecraft:block/nether_portal", generatedJson("assets/gt6/models/block/mini_portal_nether_portal.json")
				.getAsJsonObject("textures").get("portal").getAsString());
		assertEquals("gt6:block/mini_portal_end", generatedJson("assets/gt6/models/block/mini_portal_end_portal.json")
				.getAsJsonObject("textures").get("portal").getAsString());
	}

	/** Only the nether ACTIVE model leaves the default layer — the vanilla nether-portal translucent row. */
	@Test
	void renderTypeIsTranslucentOnTheNetherFaceOnly() throws Exception {
		assertEquals("minecraft:translucent", generatedJson("assets/gt6/models/block/mini_portal_nether_portal.json")
				.get("render_type").getAsString(), "ItemBlockRenderTypes.java:271 — the vanilla nether-portal layer");
		assertFalse(generatedJson("assets/gt6/models/block/mini_portal_end_portal.json").has("render_type"),
				"the End face art is opaque — the default layer stands");
		assertFalse(generatedJson("assets/gt6/models/block/mini_portal_nether_frame.json").has("render_type"));
		assertFalse(generatedJson("assets/gt6/models/block/mini_portal_end_frame.json").has("render_type"));
	}

	/** The standalone element models parent block/block — the display transforms the BlockItem rendering inherits. */
	@Test
	void elementModelsParentBlockBlockForTheItemTransforms() throws Exception {
		for (String tModel : new String[] {"mini_portal_nether_frame", "mini_portal_nether_portal",
				"mini_portal_end_frame", "mini_portal_end_portal"}) {
			assertEquals("minecraft:block/block", generatedJson("assets/gt6/models/block/" + tModel + ".json")
					.get("parent").getAsString(), tModel + ": the display-transform root");
		}
	}
}
