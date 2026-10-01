/**
 * Offline pin for task bushesgt-tint-color — the food-crop-items leftover ④. The wild
 * berry bush renders the upstream grayscale {@code bush/colored/bush.png} verbatim through
 * the tintindex-0 model seat, multiplied by the upstream default bush colour 0x22cc22
 * (MultiTileEntityBush.java:224/237 {@code BlockTextureDefault.get(sTextureBush, color)},
 * CS.java:1588 {@code BushesGT.DEFAULT[0]}) in {@code GT6BushTintListener}. The berry
 * carrier itself stays CUT (the w6-t2 declared deviation — the growth/berries/harvest
 * faces are the upstream MTE domain, booked to the T5b/breeding cards, NOT this one).
 *
 * <p>Reads the committed generated tree on the classpath (the
 * {@link GT6MoldTintDatagenTest} form — no datagen run).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.client.GT6BushTintListener;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6BushTintDatagenTest extends GTOfflineTestBase {

	/** The upstream default bush colour, pinned literally (CS.java:1588 BushesGT.DEFAULT[0]). */
	private static final int BUSH_TINT = 0xFF22CC22;

	/** The verbatim grayscale borrow (assets/README.md row this card). */
	private static final String BUSH_PNG_SHA256 =
			"2b6afbbd9c6f12d28908185e9ef5615da8600e82925a165bb8736fe45c48b683";

	private static final String[] FACES = {"down", "up", "north", "south", "west", "east"};

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6BushTintDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
	}

	/** The block model is the tinted cube_all grammar: one full element, every face tintindex 0. */
	@Test
	void theBushBlockModelCarriesTheTintindexZeroSeat() throws Exception {
		JsonObject tModel = generatedJson("assets/gt6/models/block/berry_bush.json");
		assertTrue(tModel.toString().contains("gt6:block/berry_bush"), "the bush texture stays the verbatim borrow");
		assertEquals(1, tModel.getAsJsonArray("elements").size(), "the tinted cube_all form: one full element");
		JsonObject tFaces = tModel.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
		for (String tFace : FACES) {
			assertTrue(tFaces.has(tFace), "the " + tFace + " face exists");
			assertEquals(0, tFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
					"the " + tFace + " face carries tintindex 0");
		}
	}

	/** The item model parents the block model (the tintindex seat rides into the inventory face). */
	@Test
	void theBushItemModelRidesTheBlockSeat() throws Exception {
		JsonObject tItem = generatedJson("assets/gt6/models/item/berry_bush.json");
		assertEquals("gt6:block/berry_bush", tItem.get("parent").getAsString(), "the item parents the tinted block model");
	}

	/** The handler colours = the upstream default bush colour (the CS.java:1588 anchor, not derived from production code). */
	@Test
	void theTintPinsMatchTheUpstreamDefaultBushColour() {
		assertEquals(BUSH_TINT, GT6BushTintListener.bushTintARGB(0), "index 0 = the default bush colour");
		assertEquals(-1, GT6BushTintListener.bushTintARGB(1), "a foreign tint index answers no-tint");
		assertEquals(-1, GT6BushTintListener.bushTintARGB(-1), "the un-tinted sentinel answers no-tint");
	}

	/** The borrow stays byte-identical to the upstream grayscale (a re-coloured PNG would double-multiply). */
	@Test
	void theBorrowStaysTheVerbatimUpstreamGrayscale() throws Exception {
		MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
		try (InputStream tStream = GT6BushTintDatagenTest.class.getClassLoader()
				.getResourceAsStream("assets/gt6/textures/block/berry_bush.png")) {
			assertNotNull(tStream, "the bush PNG must be on the classpath");
			byte[] tDigest = tSha256.digest(tStream.readAllBytes());
			StringBuilder tHex = new StringBuilder();
			for (byte tByte : tDigest) tHex.append(String.format("%02x", tByte));
			assertEquals(BUSH_PNG_SHA256, tHex.toString(), "the grayscale borrow is byte-identical to upstream");
		}
	}
}
