/**
 * Offline pin for task berry-overlay (built on bushesgt-tint-color +
 * bush-growth-blockstate). The wild berry bush renders the upstream BlockTextureMulti
 * layer stack (MultiTileEntityBush.java:230-242): the grayscale
 * {@code bush/colored/bush.png} body through the tintindex-0 model seat, multiplied per
 * state by the kind's BUSH BODY colour (MultiTileEntityBush.java:236-240
 * {@code tBerryColor[0]}, stage-constant), with the {@code bush_parts} berry layer on top
 * at tintindex 1 — the kind's STAGE colour at the state's AGE ({@code tBerryColor[1..3]},
 * the CS.java:1584 put javadoc "Bush Color, Stage 1, Stage 2, Stage 3" rows,
 * MultiItemFood.java:397-429 + CS.java:1588/:1589), in {@code GT6BushTintListener}.
 * The three cutout models (berry_bush / _stage1 / _stage2) carry 7 / 19 / 19 elements;
 * the item model parents the stage-2 model — the upstream item face always shows the
 * berry layer at the stage-2 colour (MultiTileEntityBush.java:233-234 SIDES_ITEM_RENDER).
 *
 * <p>Reads the committed generated tree on the classpath (the
 * {@link GT6MoldTintDatagenTest} form — no datagen run). The per-state arm drives a bare
 * {@code GT6WildBushBlock} over the GT6SurfaceVariantsTest unfreeze recipe.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

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

	/** The upstream per-kind body colours, pinned literally (MultiItemFood.java:397-429 + :1588). */
	private static final int BLUEBERRY_TINT = 0xFF22FF22;
	private static final int CANDLEBERRY_TINT = 0xFF44FF44;
	private static final int CRANBERRY_TINT = 0xFF00DD00;
	private static final int CURRANTS_TINT = 0xFF33FF33;
	private static final int BRAMBLE_TINT = 0xFF11FF11;

	/**
	 * The upstream STAGE colours [1..3] per kind, pinned literally (MultiItemFood.java:397-429
	 * + CS.java:1589 — the berry-layer tints of task berry-overlay, 0xFF-prefixed the
	 * bodyColorARGB way).
	 */
	private static final Map<GT6WildBushBlock.Kind, int[]> STAGE_COLORS = Map.of(
			GT6WildBushBlock.Kind.BLUEBERRY, new int[] {0xFFFFCCCC, 0xFF6666DD, 0xFF0000FF},
			GT6WildBushBlock.Kind.CANDLEBERRY, new int[] {0xFFCCFFCC, 0xFFAAFFAA, 0xFFCCFFCC},
			GT6WildBushBlock.Kind.CRANBERRY, new int[] {0xFFFFCCCC, 0xFF66FF66, 0xFFFF0000},
			GT6WildBushBlock.Kind.CURRANTS_BLACK, new int[] {0xFFAAAAAA, 0xFF66FF66, 0xFF111111},
			GT6WildBushBlock.Kind.CURRANTS_WHITE, new int[] {0xFFAAAAAA, 0xFF66FF66, 0xFFEEEEDD},
			GT6WildBushBlock.Kind.CURRANTS_RED, new int[] {0xFFAAAAAA, 0xFF66FF66, 0xFFEE0000},
			GT6WildBushBlock.Kind.BLACKBERRY, new int[] {0xFFFFCCCC, 0xFF663333, 0xFF331111},
			GT6WildBushBlock.Kind.RASPBERRY, new int[] {0xFFFFCCCC, 0xFF664444, 0xFFFFAAAA},
			GT6WildBushBlock.Kind.COTTON, new int[] {0xFF33CC33, 0xFF44CC44, 0xFFEEEEEE});

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

	/**
	 * The five bush_parts borrows, byte-identical to the upstream
	 * {@code machines/plants/bush/} sprites (assets/README.md berry-overlay rows).
	 * {@code overlay_berries.png} and {@code overlay_berries_immature.png} ARE the same
	 * file upstream (both 595331db… — the barrel same-hash precedent, each path borrowed).
	 */
	private static final Map<String, String> BUSH_PARTS_SHA256 = Map.of(
			"berries", "f5f544e6e7cc6bba3bfb59e885496a03021ac31318cfd1c7e820774073d58e1b",
			"berries_immature", "3fb0dfa7a665bf0051fbcf38386c9ef4c7850bb1caf803112868398c80822703",
			"overlay_bush", "57ee6c0dfeba5a32c738a26e1364c6f20c9097ec2676e31b35e7848581490e6c",
			"overlay_berries", "595331dbb5390e5fb40bc1fc80eac6082c8f58dc52a2ac02418ae72e336092e4",
			"overlay_berries_immature", "595331dbb5390e5fb40bc1fc80eac6082c8f58dc52a2ac02418ae72e336092e4");

	private static final String[] FACES = {"down", "up", "north", "south", "west", "east"};

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6BushTintDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
	}

	/** The faces of the first (body) element — the tintindex-0 seat. */
	private static JsonObject bodyFaces(JsonObject aModel) {
		return aModel.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
	}

	/** How many faces of the model sit on {@code aTextureKey} carrying tintindex {@code aTintIndex} (-1 = no key). */
	private static int tintedFaceCount(JsonObject aModel, String aTextureKey, int aTintIndex) {
		int tCount = 0;
		for (JsonElement tElement : aModel.getAsJsonArray("elements")) {
			for (var tEntry : tElement.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
				JsonObject tFace = tEntry.getValue().getAsJsonObject();
				boolean tTintMatch = aTintIndex == -1 ? !tFace.has("tintindex")
						: tFace.has("tintindex") && tFace.get("tintindex").getAsInt() == aTintIndex;
				if (tTintMatch && aTextureKey.equals(tFace.get("texture").getAsString())) tCount++;
			}
		}
		return tCount;
	}

	/**
	 * The age-0 block model: the tinted cube body (tintindex 0) + the six overlay_bush
	 * detail plates — 7 elements, cutout, and NO tintindex-1 face (the age-0 state never
	 * queries the stage-colour seat).
	 */
	@Test
	void theBushBlockModelCarriesTheTintindexZeroSeat() throws Exception {
		JsonObject tModel = generatedJson("assets/gt6/models/block/berry_bush.json");
		assertTrue(tModel.toString().contains("gt6:block/berry_bush"), "the bush texture stays the verbatim borrow");
		assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(),
				"the decal shells need the alpha discard (the C7' lesson)");
		assertEquals(7, tModel.getAsJsonArray("elements").size(),
				"the two-layer form: one body element + the six overlay_bush plates");
		JsonObject tFaces = bodyFaces(tModel);
		for (String tFace : FACES) {
			assertTrue(tFaces.has(tFace), "the " + tFace + " face exists");
			assertEquals(0, tFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
					"the " + tFace + " face carries tintindex 0");
		}
		assertEquals("gt6:block/bush_parts/overlay_bush", tModel.getAsJsonObject("textures").get("bush_overlay").getAsString(),
				"the detail shell rides the borrowed overlay sprite");
		assertEquals(0, tintedFaceCount(tModel, "#bush_overlay", 1), "the age-0 model has no tintindex-1 face");
	}

	/**
	 * The stage models (task berry-overlay): body + overlay_bush plates + the berries
	 * plates at tintindex 1 + their detail plates = 19 elements, all cutout, the berries
	 * texture per stage on the borrowed bush_parts sprites.
	 */
	@Test
	void theStageModelsCarryTheTintindexOneBerrySeat() throws Exception {
		String[][] tStages = {
				{"berry_bush_stage1", "berries_immature", "overlay_berries_immature"},
				{"berry_bush_stage2", "berries", "overlay_berries"}};
		for (String[] tStage : tStages) {
			JsonObject tModel = generatedJson("assets/gt6/models/block/" + tStage[0] + ".json");
			assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(),
					tStage[0] + " renders cutout (the C7' lesson)");
			assertEquals(19, tModel.getAsJsonArray("elements").size(),
					tStage[0] + ": body + 3 plate groups (6 each)");
			assertEquals(6, tintedFaceCount(tModel, "#berries", 1),
					tStage[0] + ": exactly the six berries faces carry the stage-colour seat");
			assertEquals(0, tintedFaceCount(tModel, "#berries", -1),
					tStage[0] + ": no untinted berries face (the plates are ALL tinted)");
			JsonObject tTextures = tModel.getAsJsonObject("textures");
			assertEquals("gt6:block/bush_parts/" + tStage[1], tTextures.get("berries").getAsString(),
					tStage[0] + ": the berries sprite");
			assertEquals("gt6:block/bush_parts/" + tStage[2], tTextures.get("berries_overlay").getAsString(),
					tStage[0] + ": the berries detail sprite");
			assertEquals("gt6:block/bush_parts/overlay_bush", tTextures.get("bush_overlay").getAsString(),
					tStage[0] + ": the shared bush detail sprite");
		}
		// the explicit upstream anchor: blueberry stage 3 = the pure blue row
		assertEquals(0xFF0000FF, BUSH.defaultBlockState()
				.setValue(GT6WildBushBlock.KIND, GT6WildBushBlock.Kind.BLUEBERRY)
				.setValue(GT6WildBushBlock.AGE, 3)
				.getValue(GT6WildBushBlock.KIND).stageColorARGB(3),
				"blueberry stage 3 = 0x0000ff (MultiItemFood.java:397) 0xFF-prefixed");
	}

	/** The item model parents the stage-2 model (the upstream :233-234 always-berry item face). */
	@Test
	void theBushItemModelRidesTheBlockSeat() throws Exception {
		JsonObject tItem = generatedJson("assets/gt6/models/item/berry_bush.json");
		assertEquals("gt6:block/berry_bush_stage2", tItem.get("parent").getAsString(),
				"the item parents the stage-2 model — the berry layer always shows");
	}

	/** The handler colours = the upstream default bush colour + the stage-2 berry of the item face. */
	@Test
	void theTintPinsMatchTheUpstreamDefaultBushColour() {
		assertEquals(BUSH_TINT, GT6BushTintListener.bushTintARGB(0), "index 0 = the default bush colour");
		assertEquals(0xFF44CC44, GT6BushTintListener.bushTintARGB(1),
				"index 1 = the always-shown berry layer at the cotton stage-2 colour "
						+ "(MultiTileEntityBush.java:233-234, CS.java:1589 DEFAULT[2])");
		assertEquals(-1, GT6BushTintListener.bushTintARGB(2), "a foreign tint index answers no-tint");
		assertEquals(-1, GT6BushTintListener.bushTintARGB(-1), "the un-tinted sentinel answers no-tint");
	}

	/**
	 * The per-state arm, index 0 (the bushesgt-tint-color pin kept): each kind answers its
	 * upstream body colour, AGE-blind across all four stages (MultiTileEntityBush.java:
	 * 236-240 — the body colour is tBerryColor[0] at every stage).
	 */
	@Test
	void thePerStateArmAnswersTheUpstreamBodyColours() {
		for (GT6WildBushBlock.Kind tKind : GT6WildBushBlock.Kind.values()) {
			int tExpected = switch (tKind) {
				case BLUEBERRY -> BLUEBERRY_TINT;
				case CANDLEBERRY -> CANDLEBERRY_TINT;
				case CRANBERRY -> CRANBERRY_TINT;
				case CURRANTS_BLACK, CURRANTS_WHITE, CURRANTS_RED -> CURRANTS_TINT;
				case BLACKBERRY, RASPBERRY -> BRAMBLE_TINT;
				case COTTON -> BUSH_TINT;
			};
			for (int tAge = 0; tAge <= 3; tAge++) {
				var tState = BUSH.defaultBlockState()
						.setValue(GT6WildBushBlock.KIND, tKind)
						.setValue(GT6WildBushBlock.AGE, tAge);
				assertEquals(tExpected, GT6BushTintListener.bushStateTintARGB(tState, 0),
						tKind + " at age " + tAge + ": the upstream body colour, stage-constant");
			}
		}
	}

	/**
	 * The per-state arm, index 1 (task berry-overlay): each kind answers its upstream
	 * STAGE colour at the state's AGE — the full 9x3 literal scan — and the age-0 seat
	 * (never queried by the model) falls back to the body colour; foreign indexes stay
	 * un-tinted.
	 */
	@Test
	void thePerStateArmAnswersTheUpstreamStageColours() {
		for (GT6WildBushBlock.Kind tKind : GT6WildBushBlock.Kind.values()) {
			int[] tStages = STAGE_COLORS.get(tKind);
			for (int tAge = 0; tAge <= 3; tAge++) {
				var tState = BUSH.defaultBlockState()
						.setValue(GT6WildBushBlock.KIND, tKind)
						.setValue(GT6WildBushBlock.AGE, tAge);
				if (tAge == 0) {
					assertEquals(tKind.bodyColorARGB(), GT6BushTintListener.bushStateTintARGB(tState, 1),
							tKind + " at age 0: the un-queried seat answers the body colour");
				} else {
					assertEquals(tStages[tAge - 1], GT6BushTintListener.bushStateTintARGB(tState, 1),
							tKind + " at age " + tAge + ": the upstream stage colour");
				}
			}
			assertEquals(-1, GT6BushTintListener.bushStateTintARGB(BUSH.defaultBlockState()
					.setValue(GT6WildBushBlock.KIND, tKind), 2),
					tKind + ": a foreign tint index answers no-tint");
		}
	}

	/** The grayscale borrow stays byte-identical to the upstream (a re-coloured PNG would double-multiply). */
	@Test
	void theBorrowStaysTheVerbatimUpstreamGrayscale() throws Exception {
		assertBorrowSha("assets/gt6/textures/block/berry_bush.png", BUSH_PNG_SHA256);
	}

	/** The five bush_parts borrows stay byte-identical to the upstream berry sprites. */
	@Test
	void theBushPartBorrowsStayTheVerbatimUpstreamSprites() throws Exception {
		for (var tRow : BUSH_PARTS_SHA256.entrySet()) {
			assertBorrowSha("assets/gt6/textures/block/bush_parts/" + tRow.getKey() + ".png", tRow.getValue());
		}
		assertFalse(BUSH_PARTS_SHA256.get("overlay_berries")
				.equals(BUSH_PARTS_SHA256.get("berries")), "the berries COLOUR sprite differs from its overlay");
	}

	private static void assertBorrowSha(String aPath, String aExpected) throws Exception {
		MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
		try (InputStream tStream = GT6BushTintDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the borrowed PNG must be on the classpath: " + aPath);
			byte[] tDigest = tSha256.digest(tStream.readAllBytes());
			StringBuilder tHex = new StringBuilder();
			for (byte tByte : tDigest) tHex.append(String.format("%02x", tByte));
			assertEquals(aExpected, tHex.toString(), "the borrow is byte-identical to upstream: " + aPath);
		}
	}
}
