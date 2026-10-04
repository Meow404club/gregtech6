/**
 * Offline pin for task flower-blocks-indicator-family — the indicator-flower assets: the
 * 18 cross blockstates/models + the 18 flat item models + the 18 potted companions
 * (parented on the vanilla {@code block/flower_pot_cross} template with the plant
 * texture) + the two loot faces (the flower self-drop; the vanilla pot + flower
 * two-pool table, the {@code potted_dandelion.json} shape) + the texture borrow
 * byte-identity (the {@link GT6BushTintDatagenTest} sha256 pin form).
 *
 * <p>Reads the committed generated tree on the classpath (the
 * {@link GT6BushTintDatagenTest} form — no datagen run). The row universe derives from
 * the registration walks ({@code GT6SurfaceBlocks.FLOWERS/POTTED_FLOWERS}), so a new
 * flower row without assets breaks HERE instead of a player's screen (the C5
 * asset-coverage posture).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6FlowerDatagenTest extends GTOfflineTestBase {

	/** The Orechid borrow sha256 (the FLOWER_ORECHID.png byte-identity pin). */
	private static final String ORECHID_SHA =
			"9eaeb3b654f27880e906c282910c065e4bc1ad263aa9df4d11876f014b6cf33b";

	private static List<String> SNAKES;

	@BeforeAll
	static void boot() {
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
		SNAKES = new ArrayList<>();
		for (int i = 0; i < GT6SurfaceBlocks.FLOWERS.size(); i++) {
			SNAKES.add(GT6SurfaceBlocks.FLOWERS.get(i).getId().getPath());
		}
	}

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6FlowerDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
	}

	private static String sha256(String aPath) throws Exception {
		try (InputStream tStream = GT6FlowerDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the texture must be on the classpath: " + aPath);
			byte[] tBytes = tStream.readAllBytes();
			StringBuilder tOut = new StringBuilder();
			for (byte tB : MessageDigest.getInstance("SHA-256").digest(tBytes)) {
				tOut.append(String.format("%02x", tB));
			}
			return tOut.toString();
		}
	}

	/** The 36 blockstates exist (18 flowers + 18 pots), each dispatching to its model. */
	@Test
	void allThirtySixBlockstatesExist() throws Exception {
		assertEquals(18, SNAKES.size());
		for (String tSnake : SNAKES) {
			JsonObject tFlower = generatedJson("assets/gt6/blockstates/" + tSnake + ".json");
			assertTrue(tFlower.getAsJsonObject("variants").entrySet().size() >= 1,
					tSnake + " blockstate carries the cross variant");
			JsonObject tPot = generatedJson("assets/gt6/blockstates/potted_" + tSnake + ".json");
			assertTrue(tPot.getAsJsonObject("variants").entrySet().size() >= 1,
					"potted_" + tSnake + " blockstate carries the model variant");
		}
	}

	/** The cross model pair: the flower parents the vanilla cross (cutout render type, the
	 * borrowed sprite), the pot parents the vanilla flower_pot_cross template (the plant
	 * texture key), the item the flat generated face. */
	@Test
	void modelsRenderTheVanillaCrossAndPotForms() throws Exception {
		JsonObject tModel = generatedJson("assets/gt6/models/block/flower_orechid.json");
		assertEquals("minecraft:block/cross", tModel.get("parent").getAsString());
		assertEquals("minecraft:cutout", tModel.get("render_type").getAsString());
		assertEquals("gt6:block/flower_orechid", tModel.getAsJsonObject("textures").get("cross").getAsString());
		JsonObject tPot = generatedJson("assets/gt6/models/block/potted_flower_orechid.json");
		assertEquals("minecraft:block/flower_pot_cross", tPot.get("parent").getAsString());
		assertEquals("gt6:block/flower_orechid", tPot.getAsJsonObject("textures").get("plant").getAsString());
		JsonObject tItem = generatedJson("assets/gt6/models/item/flower_orechid.json");
		assertEquals("minecraft:item/generated", tItem.get("parent").getAsString());
		assertEquals("gt6:block/flower_orechid", tItem.getAsJsonObject("textures").get("layer0").getAsString());
		// the pots carry NO item models (the vanilla potted parity — no BlockItem exists)
		for (String tSnake : SNAKES) {
			assertNotNull(GT6FlowerDatagenTest.class.getClassLoader()
					.getResource("assets/gt6/models/item/" + tSnake + ".json"), tSnake + " item model");
			assertEquals(null, GT6FlowerDatagenTest.class.getClassLoader()
					.getResource("assets/gt6/models/item/potted_" + tSnake + ".json"),
					"potted_" + tSnake + " has no item (the vanilla potted parity)");
		}
	}

	/** The two loot faces: the flower self-drop (one pool, the flower item) and the pot
	 * two-pool face (flower_pot + the flower, the vanilla potted_dandelion.json shape). */
	@Test
	void lootFacesMatchTheVanillaShapes() throws Exception {
		for (String tSnake : SNAKES) {
			JsonObject tFlower = generatedJson("data/gt6/loot_tables/blocks/" + tSnake + ".json");
			assertEquals(1, tFlower.getAsJsonArray("pools").size(), tSnake + " self-drop = one pool");
			assertEquals("gt6:" + tSnake,
					tFlower.getAsJsonArray("pools").get(0).getAsJsonObject()
							.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
			JsonObject tPot = generatedJson("data/gt6/loot_tables/blocks/potted_" + tSnake + ".json");
			assertEquals(2, tPot.getAsJsonArray("pools").size(), "potted_" + tSnake + " = the pot + flower pools");
			assertEquals("minecraft:flower_pot",
					tPot.getAsJsonArray("pools").get(0).getAsJsonObject()
							.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
			assertEquals("gt6:" + tSnake,
					tPot.getAsJsonArray("pools").get(1).getAsJsonObject()
							.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
		}
	}

	/** The texture borrow byte-identity: the representative Orechid sprite is the upstream
	 * FLOWER_ORECHID.png verbatim (assets/README.md ledger row), and all 18 sprites exist. */
	@Test
	void spritesAreTheUpstreamBorrows() throws Exception {
		for (String tSnake : SNAKES) {
			assertNotNull(GT6FlowerDatagenTest.class.getClassLoader()
					.getResource("assets/gt6/textures/block/" + tSnake + ".png"), tSnake + " sprite");
		}
		assertEquals(ORECHID_SHA, sha256("assets/gt6/textures/block/flower_orechid.png"),
				"the Orechid borrow is the upstream FLOWER_ORECHID.png byte-identical (README ledger)");
	}
}
