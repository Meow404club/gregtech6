/**
 * Offline pin for task bushesgt-tint-color, widened by bush-growth-blockstate. The wild
 * berry bush renders the upstream grayscale {@code bush/colored/bush.png} verbatim through
 * the tintindex-0 model seat, multiplied per state by the kind's upstream BUSH BODY colour
 * (MultiTileEntityBush.java:236-240 {@code tBerryColor[0]}, stage-constant — the stage
 * colours [1..3] ride the CUT berry overlays) in {@code GT6BushTintListener}: the
 * blueberry 0x22ff22 / candleberry 0x44ff44 / cranberry 0x00dd00 rows
 * (MultiItemFood.java:397/:405/:409) and the cotton 0x22cc22 row (CS.java:1588/:1589,
 * the BushesGT.DEFAULT the kind-less item face keeps answering).
 *
 * <p>Reads the committed generated tree on the classpath (the
 * {@link GT6MoldTintDatagenTest} form — no datagen run). The per-state arm drives a bare
 * {@code GT6WildBushBlock} over the GT6SurfaceVariantsTest unfreeze recipe.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.block.surface.GT6WildBushBlock;
import gregtech6.client.GT6BushTintListener;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6BushTintDatagenTest extends GTOfflineTestBase {

	/** The upstream default bush colour, pinned literally (CS.java:1588 BushesGT.DEFAULT[0]). */
	private static final int BUSH_TINT = 0xFF22CC22;

	/** The upstream per-kind body colours, pinned literally (MultiItemFood.java:397/:405/:409 + :1588). */
	private static final int BLUEBERRY_TINT = 0xFF22FF22;
	private static final int CANDLEBERRY_TINT = 0xFF44FF44;
	private static final int CRANBERRY_TINT = 0xFF00DD00;

	private static GT6WildBushBlock BUSH;

	@BeforeAll
	static void bootBareBush() {
		// offline Block construction needs the block registry temporarily unfrozen (the
		// GTWireContactDamageTest.block / GT6SurfaceVariantsTest.boot form; the base
		// @BeforeAll already ran the version-detect + bootStrap)
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		BUSH = new GT6WildBushBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
	}

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

	/**
	 * The per-state arm (task bush-growth-blockstate — the tint-card leftover ①): each
	 * kind answers its upstream body colour at tintindex 0, AGE-blind across all four
	 * stages (MultiTileEntityBush.java:236-240 — the body colour is tBerryColor[0] at
	 * every stage), foreign tint indexes stay un-tinted.
	 */
	@Test
	void thePerStateArmAnswersTheUpstreamBodyColours() {
		for (GT6WildBushBlock.Kind tKind : GT6WildBushBlock.Kind.values()) {
			int tExpected = switch (tKind) {
				case BLUEBERRY -> BLUEBERRY_TINT;
				case CANDLEBERRY -> CANDLEBERRY_TINT;
				case CRANBERRY -> CRANBERRY_TINT;
				case COTTON -> BUSH_TINT;
			};
			for (int tAge = 0; tAge <= 3; tAge++) {
				var tState = BUSH.defaultBlockState()
						.setValue(GT6WildBushBlock.KIND, tKind)
						.setValue(GT6WildBushBlock.AGE, tAge);
				assertEquals(tExpected, GT6BushTintListener.bushStateTintARGB(tState, 0),
						tKind + " at age " + tAge + ": the upstream body colour, stage-constant");
				assertEquals(-1, GT6BushTintListener.bushStateTintARGB(tState, 1),
						tKind + " at age " + tAge + ": a foreign tint index answers no-tint");
			}
		}
		assertEquals(16, BUSH.getStateDefinition().getPossibleStates().size(),
				"the arm walks the full 16-state grid the blockstate emits");
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
