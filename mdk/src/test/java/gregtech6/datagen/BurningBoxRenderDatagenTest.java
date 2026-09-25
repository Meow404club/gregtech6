package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Issue #11 texture half — the census for the burning-box render rewire: the five
 * upstream texture groups' 60 overlay sprites (+6 animation mcmeta + the one shared
 * body PNG) ship, every generated block model carries the p21 tint grammar (body
 * tintindex 0 + six tintindex-less decals), every blockstate pins the LIT variant
 * pair over the FACING rotations, every BlockItem model parents the UNLIT model,
 * and every new PNG's sha256 grounds in assets/README.md (the pin-f discipline,
 * scoped to this wave's files).
 */
class BurningBoxRenderDatagenTest {

	private static final List<String> GROUPS = List.of("solid", "liquid", "gas", "fluidbed", "brick");
	private static final List<String> FACES = List.of("front", "back", "left", "right", "top", "bottom");
	/** The vanilla model face keys (the body cube grammar) — the art-token faces above live in the TEXTURE names. */
	private static final List<String> BODY_FACES = List.of("down", "up", "north", "south", "west", "east");

	/** Location of the mdk project root, walking up from the (leg-dependent) test working dir. */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
				return p;
			}
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	private static Path staticResources() {
		return mdkRoot().resolve("src/main/resources");
	}

	private static Path generatedResources() {
		return mdkRoot().resolve("src/generated/resources");
	}

	/** Pin a: the 60 borrowed overlay sprites + the body + the 6 mcmeta all ship. */
	@Test
	void theFiveGroupsBorrowedSpritesAllShip() throws IOException {
		Path textures = staticResources().resolve("assets/gt6/textures/block");
		List<String> missing = new ArrayList<>();
		for (String tGroup : GROUPS) {
			for (String tFace : FACES) {
				if (!Files.isRegularFile(textures.resolve("burning_box_" + tGroup + "_overlay_" + tFace + ".png"))) {
					missing.add("burning_box_" + tGroup + "_overlay_" + tFace + ".png");
				}
				if (!Files.isRegularFile(textures.resolve("burning_box_" + tGroup + "_overlay_" + tFace + "_active.png"))) {
					missing.add("burning_box_" + tGroup + "_overlay_" + tFace + "_active.png");
				}
			}
			assertTrue(Files.isRegularFile(textures.resolve("burning_box_" + tGroup + "_overlay_front_active.png.mcmeta")),
					"the animated front strip needs its mcmeta: " + tGroup);
		}
		assertTrue(Files.isRegularFile(textures.resolve("burning_box_solid.png")), "the shared body PNG");
		assertTrue(Files.isRegularFile(textures.resolve("burning_box_brick_overlay_top_active.png.mcmeta")),
				"the Brick top strip is animated upstream too");
		assertTrue(missing.isEmpty(), "missing borrowed sprites: " + missing);
		// the three retired placeholder copies must NOT come back (their digest stays
		// grounded through the surviving body file)
		assertFalse(Files.isRegularFile(textures.resolve("burning_box_liquid.png")), "liquid placeholder retired");
		assertFalse(Files.isRegularFile(textures.resolve("burning_box_gas.png")), "gas placeholder retired");
		assertFalse(Files.isRegularFile(textures.resolve("burning_box_fluidbed.png")), "fluidbed placeholder retired");
	}

	/**
	 * Pin b: the model grammar — the body cube tintindex 0 on all six faces, seven
	 * elements (body + six decals), the decals WITHOUT a tintindex (the P22 uncoloured
	 * layer), and the lit model binding the _active decal set. One group per check arm
	 * (all five go through the same builder).
	 */
	@Test
	void theModelGrammarPinsBodyTintAndUntintedDecals() throws IOException {
		for (String tGroup : GROUPS) {
			JsonObject tModel = model("burning_box_" + tGroup);
			assertEquals(7, tModel.getAsJsonArray("elements").size(), tGroup + ": body + six decals");
			JsonObject tBody = tModel.getAsJsonArray("elements").get(0).getAsJsonObject();
			for (String tFace : BODY_FACES) {
				assertEquals(0, tBody.getAsJsonObject("faces").getAsJsonObject(tFace).get("tintindex").getAsInt(),
						tGroup + " body face " + tFace + " must carry tintindex 0 (the material seat)");
			}
			for (int i = 1; i <= 6; i++) {
				String tDecalJson = tModel.getAsJsonArray("elements").get(i).getAsJsonObject()
						.getAsJsonObject("faces").toString();
				assertFalse(tDecalJson.contains("tintindex"),
						tGroup + " decal element " + i + " must carry NO tintindex (the uncoloured layer)");
			}
			// the body texture is the ONE shared grayscale PNG
			assertEquals("gt6:block/burning_box_solid", tModel.getAsJsonObject("textures").get("down").getAsString());
			// the lit model swaps the decals to the _active set (front arm shown)
			JsonObject tLit = model("burning_box_" + tGroup + "_lit");
			assertEquals("gt6:block/burning_box_" + tGroup + "_overlay_front_active",
					tLit.getAsJsonObject("textures").get("overlay_front").getAsString(),
					tGroup + ": the lit model binds the overlay_active front");
		}
	}

	/**
	 * Pin c: the LIT variant pairs over the FACING rotations — one row per family plus
	 * the Brick row (the shared-SOLID ruling closure face).
	 */
	@Test
	void theBlockstatesPinTheLitVariantPairs() throws IOException {
		String[] tRows = {"burning_box_solid_lead", "burning_box_liquid_steel", "burning_box_gas_bronze",
				"burning_box_fluidbed_lead", "brick_burning_box"};
		for (String tRow : tRows) {
			JsonObject tVariants = blockstate(tRow).getAsJsonObject("variants");
			assertEquals(8, tVariants.entrySet().size(), tRow + ": 4 facings x 2 lit states");
			for (String tFacing : new String[] {"north", "south", "west", "east"}) {
				assertTrue(tVariants.has("facing=" + tFacing + ",lit=false"), tRow + " unlit variant");
				assertTrue(tVariants.has("facing=" + tFacing + ",lit=true"), tRow + " lit variant");
				JsonObject tLit = tVariants.getAsJsonObject("facing=" + tFacing + ",lit=true");
				assertTrue(tLit.get("model").getAsString().endsWith("_lit"),
						tRow + ": the lit variant mounts the _lit model");
			}
		}
	}

	/** Pin d: the BlockItem models parent their group's UNLIT model (the icon never burns). */
	@Test
	void theItemModelsParentTheUnlitModel() throws IOException {
		String[][] tRows = {{"burning_box_solid_lead", "burning_box_solid"},
				{"burning_box_liquid_steel", "burning_box_liquid"},
				{"burning_box_gas_bronze", "burning_box_gas"},
				{"burning_box_fluidbed_lead", "burning_box_fluidbed"},
				{"brick_burning_box", "burning_box_brick"}};
		for (String[] tRow : tRows) {
			JsonObject tItem = model("item", tRow[0]);
			assertEquals("gt6:block/" + tRow[1], tItem.get("parent").getAsString(),
					tRow[0] + ": the item parent is the UNLIT group model");
		}
	}

	/** Pin e: every new PNG's sha256 grounds in assets/README.md (the pin-f discipline, wave-scoped). */
	@Test
	void everyBorrowedSpriteDigestGroundsInTheLedger() throws Exception {
		String tReadme = Files.readString(staticResources().resolve("assets/README.md"), StandardCharsets.UTF_8);
		MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
		Path tTextures = staticResources().resolve("assets/gt6/textures/block");
		List<String> tNames = new ArrayList<>();
		for (String tGroup : GROUPS) {
			for (String tFace : FACES) {
				tNames.add("burning_box_" + tGroup + "_overlay_" + tFace + ".png");
				tNames.add("burning_box_" + tGroup + "_overlay_" + tFace + "_active.png");
			}
		}
		List<String> tUngrounded = new ArrayList<>();
		for (String tName : tNames) {
			String tHex = HexFormat.of().formatHex(
					tSha256.digest(Files.readAllBytes(tTextures.resolve(tName))));
			if (!tReadme.contains(tHex)) {
				tUngrounded.add(tName + " -> " + tHex);
			}
		}
		assertTrue(tUngrounded.isEmpty(), "borrowed sprites without a README digest row: " + tUngrounded);
		// and the mcmeta digests stay OUT of the ledger (the census grounds PNG bytes only)
		assertTrue(Files.readString(tTextures.resolve("burning_box_solid_overlay_front_active.png.mcmeta"),
				StandardCharsets.UTF_8).contains("frametime"), "the front strip is the 4-frame animation");
	}

	/** The generated block-model JSON reader. */
	private static JsonObject model(String aName) throws IOException {
		return model("block", aName);
	}

	private static JsonObject model(String aKind, String aName) throws IOException {
		return JsonParser.parseString(Files.readString(
				generatedResources().resolve("assets/gt6/models/" + aKind + "/" + aName + ".json"),
				StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static JsonObject blockstate(String aName) throws IOException {
		return JsonParser.parseString(Files.readString(
				generatedResources().resolve("assets/gt6/blockstates/" + aName + ".json"),
				StandardCharsets.UTF_8)).getAsJsonObject();
	}
}
