package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Jugs;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Offline census for task small-tank-jug — the single-row registration rendered through
 * the rim-over-body model family. Pins the card end to end (the GT6CupDatagenTest
 * split):
 * <ul>
 * <li>the registration row — the varying columns verbatim against
 *     Loader_MultiTileEntities.java:2095 (path, display, 2000 L, stack 16, magic F,
 *     temperature 2000) and the shared columns (hardness/resistance/aUtilStone ride the
 *     block carrier);</li>
 * <li>the 9-PNG borrow tree (block/jug/ 4+4 + item/clay_jug.png = the 996 icon) 1:1
 *     with the declared set and grounded in assets/README.md by name AND sha256;</li>
 * <li>the generated blockstate is the 9 gt_liquid_level variants pointing at the shared
 *     per-level jug family;</li>
 * <li>the jug model pins the upstream pass grammar (four 1px rim walls at y 10..14 +
 *     the body slab + the 0.01 overlay shells, cutout) and the per-level fluid box
 *     (levels 1..8, the flat eighth-fractions of the 3px interior above the body
 *     plane, the smeltery_content placeholder — the declared ceiling);</li>
 * <li>the block item model parents the jug; the raw item model rides the borrowed 996
 *     icon;</li>
 * <li>the lang faces: en = the :2095/:120 name columns verbatim, zh = the dump faces
 *     (陶杯/粘土杯);</li>
 * <li>the acquisition chain: the three recipes EXIST (shaped "kCR"/"C C"/"CCC" +
 *     reverse + smelt — the cup chain form).</li>
 * </ul>
 */
class GT6JugDatagenTest extends GTOfflineTestBase {

	@BeforeAll
	static void bootMaterials() {
		// the vanilla boot comes from the base; the gregapi material registry populates on
		// the mod lifecycle — the BoilerWall bootOfflineThenMaterials shape
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

	/** The borrow faces — colored + overlay over {sides, insides, top, bottom} (the :58-66 icon set, the 4+4). */
	private static final List<String> FACES = List.of("sides", "insides", "top", "bottom");
	private static final List<String> LAYERS = List.of("colored", "overlay");
	/** 2 layers x 4 faces + the 996 raw icon — the walk upstream found exactly these files. */
	private static final int PINNED_PNG_TOTAL = 9;

	/** The mdk root (src/main/resources/assets/README.md), walked upward from the leg-dependent test working dir. */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("src/main/resources/assets/README.md"))) return p;
		}
		throw new AssertionError("mdk root (src/main/resources/assets/README.md) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	/** One committed generated-tree JSON as an object (the test classpath carries src/generated/resources). */
	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6JugDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	// ---------------------------------------------------------------------------
	// the registration row (the Loader :2095 columns)
	// ---------------------------------------------------------------------------

	@Test
	void theRowIsTheLoaderCeramicRow() {
		assertEquals(1, GT6Jugs.BLOCKS.getEntries().size(), "the single :2095 row");
		assertEquals(2000, gregtech6.tileentity.tank.GT6JugBlockEntity.CAPACITY, "NBT_TANK_CAPACITY 2000");
		assertEquals(16, gregtech6.tileentity.tank.GT6JugBlockEntity.STACK_SIZE, "the stack-16 registration column");
		assertEquals(2000, gregtech6.tileentity.tank.GT6JugBlockEntity.TEMPERATURE_MAX,
				"NBT_TEMPERATURE = aMat.mMeltingPoint — the MT.Ceramic heat(2000) row (MT.java:1279, recorded-only)");
		assertTrue(gregtech6.tileentity.tank.GT6JugBlockEntity.CAN_WATER_CROPS, "the MultiTileEntityJug.java:87 canWaterCrops flag");
		assertTrue(gregtech6.tileentity.tank.GT6JugBlockEntity.CAN_PICK_UP_FLUIDS, "the MultiTileEntityJug.java:88 canPickUpFluids flag");
		assertEquals("ceramic_jug", GT6Jugs.CERAMIC_JUG.getId().getPath(), "the registration path");
		assertEquals("clay_jug", GT6Jugs.CLAY_JUG_RAW.getId().getPath(),
				"the raw item path — the en name verbatim (MultiItemRandomTools.java:120)");
	}

	// ---------------------------------------------------------------------------
	// the PNG borrow census (positive, negative, attribution nail)
	// ---------------------------------------------------------------------------

	@Test
	void everyDeclaredFaceHasItsBorrowedPng() {
		for (String tLayer : LAYERS) {
			for (String tFace : FACES) {
				assertNotNull(GT6JugDatagenTest.class.getResource(
						"/assets/gt6/textures/block/jug/" + tLayer + "_" + tFace + ".png"),
						tLayer + "_" + tFace + ".png must be borrowed");
			}
		}
		assertNotNull(GT6JugDatagenTest.class.getResource("/assets/gt6/textures/item/clay_jug.png"),
				"the 996 raw icon must be borrowed");
	}

	@Test
	void borrowedPngTreeIsExactlyTheDeclaredSet() throws Exception {
		Set<String> tFound = new HashSet<>();
		try (var tWalk = Files.walk(Path.of(GT6JugDatagenTest.class.getResource("/assets/gt6/textures/block/jug").toURI()))) {
			tWalk.filter(p -> p.toString().endsWith(".png"))
					.forEach(p -> tFound.add("block/jug/" + p.getFileName().toString()));
		}
		tFound.add("item/clay_jug.png");
		assertEquals(PINNED_PNG_TOTAL, tFound.size(), "8 block tiles + the raw icon, walked");
		Set<String> tDeclared = new HashSet<>();
		for (String tLayer : LAYERS) {
			for (String tFace : FACES) tDeclared.add("block/jug/" + tLayer + "_" + tFace + ".png");
		}
		tDeclared.add("item/clay_jug.png");
		assertEquals(tDeclared, tFound, "the borrow is 1:1 with the declared tile set — zero strays, zero gaps");
	}

	/** The attribution nail: every borrowed PNG is named in assets/README.md AND hashes to a sha256 the ledger records. */
	@Test
	void borrowedPngsAreGroundedInTheAssetsLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
		List<String> tViolations = new ArrayList<>();
		List<Path> tPngs = new ArrayList<>();
		try (var tWalk = Files.walk(Path.of(GT6JugDatagenTest.class
				.getResource("/assets/gt6/textures/block/jug").toURI()))) {
			tPngs.addAll(tWalk.filter(p -> p.toString().endsWith(".png")).toList());
		}
		tPngs.add(Path.of(GT6JugDatagenTest.class.getResource("/assets/gt6/textures/item/clay_jug.png").toURI()));
		for (Path tPng : tPngs) {
			String tName = tPng.getFileName().toString();
			String tHex = HexFormat.of().formatHex(tSha256.digest(Files.readAllBytes(tPng)));
			if (!tReadme.contains(tName)) tViolations.add(tName + " — filename absent from assets/README.md");
			if (!tReadme.contains(tHex)) tViolations.add(tName + " — bytes hash to " + tHex + ", not grounded in the ledger");
		}
		assertTrue(tViolations.isEmpty(), "borrowed jug texture without attribution: " + tViolations);
	}

	// ---------------------------------------------------------------------------
	// the generated tree (blockstate, model geometry, item models, lang)
	// ---------------------------------------------------------------------------

	@Test
	void generatedBlockstateIsTheNineLevelVariants() throws Exception {
		JsonObject tState = generatedJson("assets/gt6/blockstates/ceramic_jug.json");
		assertTrue(tState.has("variants"), "the plain-variants form (no multipart)");
		var tVariants = tState.getAsJsonObject("variants");
		assertEquals(9, tVariants.size(), "the 9 gt_liquid_level variants (the cup form)");
		for (int tLevel = 0; tLevel <= 8; tLevel++) {
			JsonElement tVariant = tVariants.get("gt_liquid_level=" + tLevel);
			assertNotNull(tVariant, "the level " + tLevel + " variant exists");
			String tExpectedModel = tLevel == 0 ? "gt6:block/ceramic_jug_empty"
					: "gt6:block/ceramic_jug_filled_" + tLevel;
			assertEquals(tExpectedModel, tVariant.getAsJsonObject().get("model").getAsString(),
					"level " + tLevel + " points at the shared per-level family");
		}
	}

	/**
	 * The jug geometry pin: 10 elements = the four 1px rim walls at y 10..14 (the pass
	 * 0-3 boxes verbatim, corners open) + the body slab + their five 0.01 overlay
	 * shells, cutout. The wall face bands: outer=sides, inner=insides, rim=top; the
	 * body: horizontals=sides, down=bottom, up=top (the :76 pass-4/SIDE_Y_POS form).
	 */
	@Test
	void theJugModelPinsThePassGrammar() throws Exception {
		JsonObject tModel = generatedJson("assets/gt6/models/block/ceramic_jug_empty.json");
		assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), "the two-layer cutout form");
		JsonArray tElements = tModel.getAsJsonArray("elements");
		assertEquals(10, tElements.size(), "4 rim walls + the body + their 5 overlay shells");
		Set<String> tBoxes = new HashSet<>();
		int tOverlayShells = 0;
		for (var tElement : tElements) {
			JsonObject tElem = tElement.getAsJsonObject();
			String tBox = tElem.getAsJsonArray("from") + "" + tElem.getAsJsonArray("to"); // the Gson compact form
			tBoxes.add(tBox);
			if (tElem.getAsJsonObject("faces").getAsJsonObject("east").get("texture").getAsString().startsWith("#overlay_")) {
				tOverlayShells++;
				continue; // the overlay shells: band-mapped, not pinned per-face here
			}
			switch (tBox) {
				case "[5,10,6][6,14,10]" -> { // the west rim wall — outer west = sides, inner east = insides
					assertEquals("#sides", tElem.getAsJsonObject("faces").getAsJsonObject("west").get("texture").getAsString());
					assertEquals("#insides", tElem.getAsJsonObject("faces").getAsJsonObject("east").get("texture").getAsString());
					assertEquals("#top", tElem.getAsJsonObject("faces").getAsJsonObject("up").get("texture").getAsString());
				}
				case "[10,10,6][11,14,10]" -> { // the east rim wall — mirrored
					assertEquals("#sides", tElem.getAsJsonObject("faces").getAsJsonObject("east").get("texture").getAsString());
					assertEquals("#insides", tElem.getAsJsonObject("faces").getAsJsonObject("west").get("texture").getAsString());
				}
				case "[6,10,5][10,14,6]" -> { // the north rim wall
					assertEquals("#sides", tElem.getAsJsonObject("faces").getAsJsonObject("north").get("texture").getAsString());
					assertEquals("#insides", tElem.getAsJsonObject("faces").getAsJsonObject("south").get("texture").getAsString());
				}
				case "[6,10,10][10,14,11]" -> { // the south rim wall
					assertEquals("#sides", tElem.getAsJsonObject("faces").getAsJsonObject("south").get("texture").getAsString());
					assertEquals("#insides", tElem.getAsJsonObject("faces").getAsJsonObject("north").get("texture").getAsString());
				}
				case "[3,0,3][13,10,13]" -> { // the body — down=bottom, up=top (the :76 mapping), horizontals=sides
					assertEquals("#bottom", tElem.getAsJsonObject("faces").getAsJsonObject("down").get("texture").getAsString());
					assertEquals("#top", tElem.getAsJsonObject("faces").getAsJsonObject("up").get("texture").getAsString());
					assertEquals("#sides", tElem.getAsJsonObject("faces").getAsJsonObject("north").get("texture").getAsString());
				}
				default -> throw new AssertionError("unexpected jug box " + tBox);
			}
		}
		assertEquals(10, tBoxes.size(), "ten distinct boxes, no duplicates");
		assertEquals(5, tOverlayShells, "the five 0.01 overlay shells ride the overlay band");
	}

	/**
	 * The per-level fluid box: levels 1..8 keep the shell parent (the texture map) but carry
	 * the FULL element set in the child itself — the ten shell boxes + the one fluid box, the
	 * flat eighth-fractions of the 3px interior above the body plane. Vanilla
	 * {@code BlockModel.getElements} (1.20.1 BlockModel.java:104-105) never walks the parent
	 * chain once the child carries elements — the former parent + one-element "candle idiom"
	 * dropped the jug shell at render (the smeltery/cup filled family's same disease, the
	 * filled-shell-family-2 card). Cutout is self-carried too: render_type does not ride the
	 * parent chain either, and the shell's transparent overlay texels need the alpha-tested
	 * layer.
	 */
	@Test
	void theFilledModelsPinThePerLevelFluidBox() throws Exception {
		int tShellCount = generatedJson("assets/gt6/models/block/ceramic_jug_empty.json")
				.getAsJsonArray("elements").size();
		for (int tLevel = 1; tLevel <= 8; tLevel++) {
			JsonObject tModel = generatedJson("assets/gt6/models/block/ceramic_jug_filled_" + tLevel + ".json");
			assertEquals("gt6:block/ceramic_jug_empty", tModel.get("parent").getAsString(), "level " + tLevel + ": the parent stays (the texture map)");
			assertEquals("minecraft:cutout", tModel.get("render_type") == null ? null : tModel.get("render_type").getAsString(),
					"level " + tLevel + ": cutout self-carried — the transparent overlay shell must bake alpha-tested");
			assertEquals(tShellCount + 1, tModel.getAsJsonArray("elements").size(),
					"level " + tLevel + ": the FULL shell + the one fluid box — vanilla getElements never merges the parent");
			// the game's own accessor — the exact parent-fallback decision point — must see
			// the full set offline (ponytail: the stand-in for the quad bake, which needs
			// the atlas + ModelBakery; the layer-level view stays field_test)
			var tVanilla = net.minecraft.client.renderer.block.model.BlockModel.fromString(tModel.toString());
			assertEquals(tShellCount + 1, tVanilla.getElements().size(),
					"level " + tLevel + ": vanilla BlockModel.getElements sees the shell+fluid set");
			float tTop = 10.05F + tLevel * 2.9F / 8.0F;
			JsonObject tFluid = null;
			for (var tElement : tModel.getAsJsonArray("elements")) {
				for (var tFace : tElement.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
					if ("#content".equals(tFace.getValue().getAsJsonObject().get("texture").getAsString())) {
						tFluid = tElement.getAsJsonObject();
					}
				}
			}
			assertNotNull(tFluid, "level " + tLevel + ": the fluid box element");
			float tMinY = tFluid.getAsJsonArray("from").get(1).getAsFloat();
			assertEquals(10.05F, tMinY, 1e-4, "level " + tLevel + ": the 0.05px inset off the body plane (no z-fight)");
			assertEquals(tTop, tFluid.getAsJsonArray("to").get(1).getAsFloat(), 1e-4,
					"level " + tLevel + ": the flat eighth-fraction of the 3px interior, inset off the :53 fluid plane");
			for (var tFaceEntry : tFluid.getAsJsonObject("faces").entrySet()) {
				assertEquals("#content", tFaceEntry.getValue().getAsJsonObject().get("texture").getAsString(),
						"level " + tLevel + ": every face rides the smeltery_content placeholder (the declared ceiling)");
				assertFalse(tFaceEntry.getValue().getAsJsonObject().has("tintindex"),
						"level " + tLevel + " face " + tFaceEntry.getKey() + ": the fluid box never tints (the BlockTextureFluid form)");
			}
		}
	}

	@Test
	void generatedItemModelsRideTheFamilyForms() throws Exception {
		assertEquals("gt6:block/ceramic_jug_empty",
				generatedJson("assets/gt6/models/item/ceramic_jug.json").get("parent").getAsString(),
				"the block item parents the shared jug model (the cup form)");
		JsonObject tRaw = generatedJson("assets/gt6/models/item/clay_jug.json");
		assertEquals("minecraft:item/generated", tRaw.get("parent").getAsString(), "the raw rides item/generated (the pot form)");
		assertEquals("gt6:item/clay_jug",
				tRaw.getAsJsonObject("textures").get("layer0").getAsString(), "the raw icon rides the 996 borrow");
	}

	/** The lang faces: en = the :2095/:120 columns verbatim; zh = the dump faces 陶杯/粘土杯. */
	@Test
	void theLangFacesAreTheVerbatimColumns() throws Exception {
		JsonObject tEn = generatedJson("assets/gt6/lang/en_us.json");
		assertEquals("Ceramic Jug", tEn.get("block.gt6.ceramic_jug").getAsString(), "the Loader :2095 name column verbatim");
		assertEquals("Clay Jug", tEn.get("item.gt6.clay_jug").getAsString(), "the MultiItemRandomTools :120 row verbatim");
		JsonObject tZh = generatedJson("assets/gt6/lang/zh_cn.json");
		assertEquals("陶杯", tZh.get("block.gt6.ceramic_jug").getAsString(), "dump gt.multitileentity.32740 verbatim");
		assertEquals("粘土杯", tZh.get("item.gt6.clay_jug").getAsString(), "dump gt.multiitem.randomtools.996 verbatim");
	}

	// ---------------------------------------------------------------------------
	// the acquisition chain (the three rows — the cup chain form)
	// ---------------------------------------------------------------------------

	@Test
	void theAcquisitionChainGeneratesAllThreeRows() throws Exception {
		JsonObject tShaped = generatedJson("data/gt6/recipes/clay_jug.json");
		JsonArray tPattern = tShaped.getAsJsonArray("pattern");
		assertEquals(3, tPattern.size(), "the :133 row is the three-row \"kCR\"/\"C C\"/\"CCC\" pattern");
		assertEquals("kCR", tPattern.get(0).getAsString(), "the :133 top row verbatim (k knife mark + C clay + R rolling-pin mark)");
		assertEquals("C C", tPattern.get(1).getAsString(), "the :133 middle row verbatim (the hollow neck)");
		assertEquals("CCC", tPattern.get(2).getAsString(), "the :133 bottom row verbatim (the base)");
		JsonObject tKey = tShaped.getAsJsonObject("key");
		assertEquals("minecraft:clay_ball", tKey.getAsJsonObject("C").get("item").getAsString(),
				"C = the clay ball member (the U*6 OreDictItemData, the measuring-pot form)");
		assertEquals("gt6:tools/knife", tKey.getAsJsonObject("k").get("tag").getAsString(), "k = the knife tool tag (the clay-band law; vanilla serializes the tag unslashed)");
		assertEquals("gt6:tools/rolling_pin", tKey.getAsJsonObject("R").get("tag").getAsString(), "R = the rolling-pin tool tag (unslashed form)");

		JsonObject tReverse = generatedJson("data/gt6/recipes/clay_jug_reverse.json");
		// the 1.20.1 shapeless result shape: {"item": "...", "count": 6}
		assertEquals("minecraft:clay_ball", tReverse.getAsJsonObject("result").get("item").getAsString(),
				"the :120 tail — one raw → 6 clay balls");
		assertEquals(6, tReverse.getAsJsonObject("result").get("count").getAsInt(),
				"the :120 tail count column — the U*6 reverse");

		JsonObject tSmelt = generatedJson("data/gt6/recipes/smelt_clay_jug.json");
		// the 1.20.1 cooking result shape: the plain id string
		assertEquals("gt6:ceramic_jug", tSmelt.get("result").getAsString(),
				"the Loader :2095 smelting tail — raw → block item");
		assertEquals(200, tSmelt.get("cookingtime").getAsInt(), "the :2177-band standard 200t face");
		assertEquals(0.0F, tSmelt.get("experience").getAsFloat(), "the 0xp face");
	}
}
