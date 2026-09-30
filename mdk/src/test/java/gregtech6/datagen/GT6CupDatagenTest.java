package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import gregtech6.registry.GT6Cups;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Offline census for task small-tank-cup — the single-row registration rendered through
 * the bowl model family. Pins the card end to end (the GT6CellDatagenTest split):
 * <ul>
 * <li>the registration row — the varying columns verbatim against
 *     Loader_MultiTileEntities.java:2094 (path, display, 250 L, stack 16, magic T,
 *     temperature 1800) and the shared columns (hardness/resistance/aUtilStone ride the
 *     block carrier);</li>
 * <li>the 9-PNG borrow tree (block/cup/ 4+4 + item/modeled_porcelain_cup.png = the
 *     899 icon) 1:1 with the declared set and grounded in assets/README.md by name AND
 *     sha256;</li>
 * <li>the generated blockstate is the 9 gt_liquid_level variants pointing at the shared
 *     per-level bowl family;</li>
 * <li>the bowl model pins the upstream pass grammar (four 1px walls + the bottom slab +
 *     the 0.01 overlay shells, cutout) and the per-level fluid box (levels 1..8, the flat
 *     eighth-fractions of the 4px interior, the smeltery_content placeholder — the
 *     declared ceiling);</li>
 * <li>the block item model parents the bowl; the raw item model rides the borrowed 899
 *     icon;</li>
 * <li>the lang faces: en = the :2094/:76 name columns verbatim, zh = the dump faces
 *     (瓷杯/已成型的瓷杯);</li>
 * <li>the acquisition chain: the three recipes EXIST (shaped "kPR" + reverse + smelt —
 *     unlike the cell family, the cup chain is complete on this card).</li>
 * </ul>
 */
class GT6CupDatagenTest extends GTOfflineTestBase {

	@BeforeAll
	static void bootMaterials() {
		// the vanilla boot comes from the base; the gregapi material registry populates on
		// the mod lifecycle — the BoilerWall bootOfflineThenMaterials shape
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

	/** The borrow faces — colored + overlay over {sides, insides, top, bottom} (the :55-63 icon set, the 4+4). */
	private static final List<String> FACES = List.of("sides", "insides", "top", "bottom");
	private static final List<String> LAYERS = List.of("colored", "overlay");
	/** 2 layers x 4 faces + the 899 raw icon — the walk upstream found exactly these files. */
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
		try (InputStream tStream = GT6CupDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	// ---------------------------------------------------------------------------
	// the registration row (the Loader :2094 columns)
	// ---------------------------------------------------------------------------

	@Test
	void theRowIsTheLoaderPorcelainRow() {
		assertEquals(1, GT6Cups.BLOCKS.getEntries().size(), "the single :2094 row");
		assertEquals(250, gregtech6.tileentity.tank.GT6CupBlockEntity.CAPACITY, "NBT_TANK_CAPACITY 250");
		assertEquals(16, gregtech6.tileentity.tank.GT6CupBlockEntity.STACK_SIZE, "the stack-16 registration column");
		assertEquals(1800, gregtech6.tileentity.tank.GT6CupBlockEntity.TEMPERATURE_MAX,
				"NBT_TEMPERATURE = aMat.mMeltingPoint — the MT.Porcelain heat(1800) row (recorded-only)");
		assertEquals("porcelain_cup", gregtech6.registry.GT6Cups.PORCELAIN_CUP.getId().getPath(), "the registration path");
		assertEquals("modeled_porcelain_cup", GT6Cups.MODELED_PORCELAIN_CUP_RAW.getId().getPath(),
				"the raw item path — the en name verbatim (MultiItemRandomTools.java:76)");
	}

	// ---------------------------------------------------------------------------
	// the PNG borrow census (positive, negative, attribution nail)
	// ---------------------------------------------------------------------------

	@Test
	void everyDeclaredFaceHasItsBorrowedPng() {
		for (String tLayer : LAYERS) {
			for (String tFace : FACES) {
				assertNotNull(GT6CupDatagenTest.class.getResource(
						"/assets/gt6/textures/block/cup/" + tLayer + "_" + tFace + ".png"),
						tLayer + "_" + tFace + ".png must be borrowed");
			}
		}
		assertNotNull(GT6CupDatagenTest.class.getResource("/assets/gt6/textures/item/modeled_porcelain_cup.png"),
				"the 899 raw icon must be borrowed");
	}

	@Test
	void borrowedPngTreeIsExactlyTheDeclaredSet() throws Exception {
		Set<String> tFound = new HashSet<>();
		try (var tWalk = Files.walk(Path.of(GT6CupDatagenTest.class.getResource("/assets/gt6/textures/block/cup").toURI()))) {
			tWalk.filter(p -> p.toString().endsWith(".png"))
					.forEach(p -> tFound.add("block/cup/" + p.getFileName().toString()));
		}
		tFound.add("item/modeled_porcelain_cup.png");
		assertEquals(PINNED_PNG_TOTAL, tFound.size(), "8 block tiles + the raw icon, walked");
		Set<String> tDeclared = new HashSet<>();
		for (String tLayer : LAYERS) {
			for (String tFace : FACES) tDeclared.add("block/cup/" + tLayer + "_" + tFace + ".png");
		}
		tDeclared.add("item/modeled_porcelain_cup.png");
		assertEquals(tDeclared, tFound, "the borrow is 1:1 with the declared tile set — zero strays, zero gaps");
	}

	/** The attribution nail: every borrowed PNG is named in assets/README.md AND hashes to a sha256 the ledger records. */
	@Test
	void borrowedPngsAreGroundedInTheAssetsLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
		List<String> tViolations = new ArrayList<>();
		List<Path> tPngs = new ArrayList<>();
		try (var tWalk = Files.walk(Path.of(GT6CupDatagenTest.class
				.getResource("/assets/gt6/textures/block/cup").toURI()))) {
			tPngs.addAll(tWalk.filter(p -> p.toString().endsWith(".png")).toList());
		}
		tPngs.add(Path.of(GT6CupDatagenTest.class.getResource("/assets/gt6/textures/item/modeled_porcelain_cup.png").toURI()));
		for (Path tPng : tPngs) {
			String tName = tPng.getFileName().toString();
			String tHex = HexFormat.of().formatHex(tSha256.digest(Files.readAllBytes(tPng)));
			if (!tReadme.contains(tName)) tViolations.add(tName + " — filename absent from assets/README.md");
			if (!tReadme.contains(tHex)) tViolations.add(tName + " — bytes hash to " + tHex + ", not grounded in the ledger");
		}
		assertTrue(tViolations.isEmpty(), "borrowed cup texture without attribution: " + tViolations);
	}

	// ---------------------------------------------------------------------------
	// the generated tree (blockstate, model geometry, item models, lang)
	// ---------------------------------------------------------------------------

	@Test
	void generatedBlockstateIsTheNineLevelVariants() throws Exception {
		JsonObject tState = generatedJson("assets/gt6/blockstates/porcelain_cup.json");
		assertTrue(tState.has("variants"), "the plain-variants form (no multipart)");
		var tVariants = tState.getAsJsonObject("variants");
		assertEquals(9, tVariants.size(), "the 9 gt_liquid_level variants (the crucible bowl form)");
		for (int tLevel = 0; tLevel <= 8; tLevel++) {
			JsonElement tVariant = tVariants.get("gt_liquid_level=" + tLevel);
			assertNotNull(tVariant, "the level " + tLevel + " variant exists");
			String tExpectedModel = tLevel == 0 ? "gt6:block/porcelain_cup_empty"
					: "gt6:block/porcelain_cup_filled_" + tLevel;
			assertEquals(tExpectedModel, tVariant.getAsJsonObject().get("model").getAsString(),
					"level " + tLevel + " points at the shared per-level family");
		}
	}

	/**
	 * The bowl geometry pin: 10 elements = the four 1px walls (the pass 0-3 boxes verbatim,
	 * corners open) + the bottom slab + their five 0.01 overlay shells, cutout. The wall
	 * face bands: outer=sides, inner=insides, rim=top; the slab: down=bottom, up=top (the
	 * :73 pass-4/SIDE_Y_POS mapping).
	 */
	@Test
	void theBowlModelPinsThePassGrammar() throws Exception {
		JsonObject tModel = generatedJson("assets/gt6/models/block/porcelain_cup_empty.json");
		assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), "the two-layer cutout form");
		JsonArray tElements = tModel.getAsJsonArray("elements");
		assertEquals(10, tElements.size(), "4 walls + the slab + their 5 overlay shells");
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
				case "[5,1,6][6,5,10]" -> { // the west wall — outer west = sides, inner east = insides
					assertEquals("#sides", tElem.getAsJsonObject("faces").getAsJsonObject("west").get("texture").getAsString());
					assertEquals("#insides", tElem.getAsJsonObject("faces").getAsJsonObject("east").get("texture").getAsString());
					assertEquals("#top", tElem.getAsJsonObject("faces").getAsJsonObject("up").get("texture").getAsString());
				}
				case "[10,1,6][11,5,10]" -> { // the east wall — mirrored
					assertEquals("#sides", tElem.getAsJsonObject("faces").getAsJsonObject("east").get("texture").getAsString());
					assertEquals("#insides", tElem.getAsJsonObject("faces").getAsJsonObject("west").get("texture").getAsString());
				}
				case "[6,1,5][10,5,6]" -> { // the north wall
					assertEquals("#sides", tElem.getAsJsonObject("faces").getAsJsonObject("north").get("texture").getAsString());
					assertEquals("#insides", tElem.getAsJsonObject("faces").getAsJsonObject("south").get("texture").getAsString());
				}
				case "[6,1,10][10,5,11]" -> { // the south wall
					assertEquals("#sides", tElem.getAsJsonObject("faces").getAsJsonObject("south").get("texture").getAsString());
					assertEquals("#insides", tElem.getAsJsonObject("faces").getAsJsonObject("north").get("texture").getAsString());
				}
				case "[6,0,6][10,1,10]" -> { // the slab — down=bottom, the interior floor=top (the :73 mapping)
					assertEquals("#bottom", tElem.getAsJsonObject("faces").getAsJsonObject("down").get("texture").getAsString());
					assertEquals("#top", tElem.getAsJsonObject("faces").getAsJsonObject("up").get("texture").getAsString());
				}
				default -> throw new AssertionError("unexpected bowl box " + tBox);
			}
		}
		assertEquals(10, tBoxes.size(), "ten distinct boxes, no duplicates");
		assertEquals(5, tOverlayShells, "the five 0.01 overlay shells ride the overlay band");
	}

	/** The per-level fluid box: levels 1..8 parent the shell and APPEND one content box, the flat eighth-fractions of the 4px interior. */
	@Test
	void theFilledModelsPinThePerLevelFluidBox() throws Exception {
		assertEquals("gt6:block/porcelain_cup_empty",
				generatedJson("assets/gt6/models/block/porcelain_cup_filled_1.json").get("parent").getAsString(),
				"the filled children parent the shell — elements APPEND (the vanilla candle idiom)");
		for (int tLevel = 1; tLevel <= 8; tLevel++) {
			JsonObject tModel = generatedJson("assets/gt6/models/block/porcelain_cup_filled_" + tLevel + ".json");
			assertEquals("gt6:block/porcelain_cup_empty", tModel.get("parent").getAsString(), "level " + tLevel + ": parents the shell");
			JsonArray tElements = tModel.getAsJsonArray("elements");
			assertEquals(1, tElements.size(), "level " + tLevel + ": exactly the appended fluid box");
			JsonObject tElem = tElements.get(0).getAsJsonObject();
			float tMinY = tElem.getAsJsonArray("from").get(1).getAsFloat();
			float tTop = tElem.getAsJsonArray("to").get(1).getAsFloat();
			assertEquals(1.05F, tMinY, 1e-4, "level " + tLevel + ": the 0.05px inset off the slab plane (no z-fight)");
			assertEquals(1.05F + tLevel * 3.5F / 8.0F, tTop, 1e-4, "level " + tLevel + ": the flat eighth-fraction of the 4px interior, inset off the rim plane");
			for (var tFaceEntry : tElem.getAsJsonObject("faces").entrySet()) {
				assertEquals("#content", tFaceEntry.getValue().getAsJsonObject().get("texture").getAsString(),
						"level " + tLevel + ": every face rides the smeltery_content placeholder (the declared ceiling)");
			}
		}
	}

	@Test
	void generatedItemModelsRideTheFamilyForms() throws Exception {
		assertEquals("gt6:block/porcelain_cup_empty",
				generatedJson("assets/gt6/models/item/porcelain_cup.json").get("parent").getAsString(),
				"the block item parents the shared bowl model (the cell form)");
		JsonObject tRaw = generatedJson("assets/gt6/models/item/modeled_porcelain_cup.json");
		assertEquals("minecraft:item/generated", tRaw.get("parent").getAsString(), "the raw rides item/generated (the pot form)");
		assertEquals("gt6:item/modeled_porcelain_cup",
				tRaw.getAsJsonObject("textures").get("layer0").getAsString(), "the raw icon rides the 899 borrow");
	}

	/** The lang faces: en = the :2094/:76 columns verbatim; zh = the dump faces 瓷杯/已成型的瓷杯. */
	@Test
	void theLangFacesAreTheVerbatimColumns() throws Exception {
		JsonObject tEn = generatedJson("assets/gt6/lang/en_us.json");
		assertEquals("Porcelain Cup", tEn.get("block.gt6.porcelain_cup").getAsString(), "the Loader :2094 name column verbatim");
		assertEquals("Modeled Porcelain Cup", tEn.get("item.gt6.modeled_porcelain_cup").getAsString(), "the MultiItemRandomTools :76 row verbatim");
		JsonObject tZh = generatedJson("assets/gt6/lang/zh_cn.json");
		assertEquals("瓷杯", tZh.get("block.gt6.porcelain_cup").getAsString(), "dump gt.multitileentity.32739 verbatim");
		assertEquals("已成型的瓷杯", tZh.get("item.gt6.modeled_porcelain_cup").getAsString(), "dump gt.multiitem.randomtools.899 verbatim");
	}

	// ---------------------------------------------------------------------------
	// the acquisition chain (the three rows EXIST — unlike the cell defer)
	// ---------------------------------------------------------------------------

	@Test
	void theAcquisitionChainGeneratesAllThreeRows() throws Exception {
		JsonObject tShaped = generatedJson("data/gt6/recipes/modeled_porcelain_cup.json");
		JsonArray tPattern = tShaped.getAsJsonArray("pattern");
		assertEquals(1, tPattern.size(), "the :78 row is the single-row \"kPR\" pattern");
		assertEquals("kPR", tPattern.get(0).getAsString(), "the :78 pattern verbatim (k knife mark + P dust + R rolling-pin mark)");
		JsonObject tKey = tShaped.getAsJsonObject("key");
		assertTrue(tKey.getAsJsonObject("P").get("tag") == null
				? tKey.getAsJsonObject("P").get("item").getAsString().contains("porcelain")
				: tKey.getAsJsonObject("P").get("tag").getAsString().contains("porcelain"),
				"P = the porcelain dust member");
		assertEquals("gt6:tools/knife", tKey.getAsJsonObject("k").get("tag").getAsString(), "k = the knife tool tag (the clay-band law; vanilla serializes the tag unslashed)");
		assertEquals("gt6:tools/rolling_pin", tKey.getAsJsonObject("R").get("tag").getAsString(), "R = the rolling-pin tool tag (unslashed form)");

		JsonObject tReverse = generatedJson("data/gt6/recipes/modeled_porcelain_cup_reverse.json");
		// the 1.20.1 shapeless result shape: {"item": "..."} — the count key is omitted at 1
		assertEquals("gt6:dust_porcelain", tReverse.getAsJsonObject("result").get("item").getAsString(),
				"the :76 tail — one raw → 1 porcelain dust (the dust_porcelain member)");

		JsonObject tSmelt = generatedJson("data/gt6/recipes/smelt_modeled_porcelain_cup.json");
		// the 1.20.1 cooking result shape: the plain id string
		assertEquals("gt6:porcelain_cup", tSmelt.get("result").getAsString(),
				"the Loader :2094 smelting tail — raw → block item");
		assertEquals(200, tSmelt.get("cookingtime").getAsInt(), "the :2177-band standard 200t face");
		assertEquals(0.0F, tSmelt.get("experience").getAsFloat(), "the 0xp face");
	}
}
