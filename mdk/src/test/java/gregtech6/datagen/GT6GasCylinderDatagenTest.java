/**
 * Offline census for task small-tank-gas-cylinder — the gas cylinder family's render
 * + recipe wave. The four-row registration (Loader :2101-2104) rendered through the
 * shared static bell model; this census pins the card end to end (the
 * GT6KitchenRenderDatagenTest split):
 * <ul>
 * <li>the 4-row registration table (the paths, the en name columns verbatim, the
 *     resistances, the material columns);</li>
 * <li>the 8-PNG borrow tree (block/gas_cylinder/, the upstream colored+overlay tile
 *     set) is 1:1 with the declared set — zero strays, zero gaps — and every borrowed
 *     PNG is grounded in assets/README.md by basename AND by the actual sha256 of its
 *     bytes (the p31 attribution-nail pattern);</li>
 * <li>the four generated blockstates are property-free single-state rows pointing at
 *     the ONE shared model;</li>
 * <li>the shared model pins the upstream bell geometry (the 5 render passes :118-127
 *     verbatim; the two neck/barometer elements tile EVERY face with the barometer
 *     band — the :141 aRenderPass &gt; 2 branch) over the 0.01 overlay shells;</li>
 * <li>the four BlockItem models parent the shared block model;</li>
 * <li>the four shaped recipes carry the :2101-2104 inline "RCR"/"BCh"/"TPd" grid.</li>
 * </ul>
 */
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

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6GasCylinders;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Offline census for task small-tank-gas-cylinder — extends GTOfflineTestBase (the
 * GT6MeasuringPotDatagenTest form: the vanilla boot covers the ForgeRegistries clinit
 * the GT6GasCylinders row table rides).
 */
class GT6GasCylinderDatagenTest extends GTOfflineTestBase {

	/** The borrow faces — colored + overlay over {sides, top, bottom, barometer} (the :129-137 icon set, NO insides). */
	private static final List<String> FACES = List.of("sides", "top", "bottom", "barometer");
	private static final List<String> LAYERS = List.of("colored", "overlay");
	/** 2 layers x 4 faces — the walk upstream found exactly these files (the id1055 correction: no insides tile). */
	private static final int PINNED_PNG_TOTAL = 8;

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
		try (InputStream tStream = GT6GasCylinderDatagenTest.class.getClassLoader()
				.getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	// ---------------------------------------------------------------------------
	// the 4-row registration census (the Loader :2101-2104 name/resistance columns)
	// ---------------------------------------------------------------------------

	@Test
	void theRowTableIsTheFourLoaderRows() {
		assertEquals(4, GT6GasCylinders.ROWS.size(), "the :2101-2104 quartet, in registration order");
		assertEquals("gas_cylinder_steel", GT6GasCylinders.ROWS.get(0).path());
		assertEquals("Steel Barometer Gas Cylinder", GT6GasCylinders.ROWS.get(0).displayName(), "the :2101 name column verbatim");
		assertEquals(6.0F, GT6GasCylinders.ROWS.get(0).resistanceF(), "the :2101 NBT_RESISTANCE");
		assertEquals("gas_cylinder_stainless_steel", GT6GasCylinders.ROWS.get(1).path());
		assertEquals("Stainless Barometer Gas Cylinder", GT6GasCylinders.ROWS.get(1).displayName(), "the :2102 name column verbatim");
		assertEquals(6.0F, GT6GasCylinders.ROWS.get(1).resistanceF(), "the :2102 NBT_RESISTANCE");
		assertEquals("gas_cylinder_tungsten", GT6GasCylinders.ROWS.get(2).path());
		assertEquals("Tungsten Barometer Gas Cylinder", GT6GasCylinders.ROWS.get(2).displayName(), "the :2103 name column verbatim");
		assertEquals(10.0F, GT6GasCylinders.ROWS.get(2).resistanceF(), "the :2103 NBT_RESISTANCE");
		assertEquals("gas_cylinder_tantalum_hafnium_carbide", GT6GasCylinders.ROWS.get(3).path());
		assertEquals("Tantalum Hafnium Carbide Barometer Gas Cylinder", GT6GasCylinders.ROWS.get(3).displayName(), "the :2104 name column verbatim");
		assertEquals(10.0F, GT6GasCylinders.ROWS.get(3).resistanceF(), "the :2104 NBT_RESISTANCE");
		assertEquals(GT6GasCylinders.BLOCKS_IN_ORDER.size(), GT6GasCylinders.ROWS.size(), "one block per row");
	}

	// ---------------------------------------------------------------------------
	// the PNG borrow census (positive, negative, attribution nail)
	// ---------------------------------------------------------------------------

	@Test
	void everyDeclaredFaceHasItsBorrowedPng() {
		for (String tLayer : LAYERS) {
			for (String tFace : FACES) {
				assertNotNull(GT6GasCylinderDatagenTest.class.getResource(
						"/assets/gt6/textures/block/gas_cylinder/" + tLayer + "_" + tFace + ".png"),
						tLayer + "_" + tFace + ".png must be borrowed");
			}
		}
	}

	@Test
	void borrowedPngTreeIsExactlyTheDeclaredSet() throws Exception {
		var tUrl = GT6GasCylinderDatagenTest.class.getResource("/assets/gt6/textures/block/gas_cylinder");
		assertNotNull(tUrl, "the gas_cylinder borrow root must exist");
		Set<String> tFound = new HashSet<>();
		try (var tWalk = Files.walk(Path.of(tUrl.toURI()))) {
			tWalk.filter(p -> p.toString().endsWith(".png"))
					.forEach(p -> tFound.add(p.getFileName().toString()));
		}
		assertEquals(PINNED_PNG_TOTAL, tFound.size(), "2 layers x 4 faces borrowed PNGs, walked");
		Set<String> tDeclared = new HashSet<>();
		for (String tLayer : LAYERS) {
			for (String tFace : FACES) tDeclared.add(tLayer + "_" + tFace + ".png");
		}
		assertEquals(tDeclared, tFound, "the borrow is 1:1 with the declared tile set — zero strays, zero gaps (no insides tile, the id1055 correction)");
	}

	/** The attribution nail (the p31 wave pattern): every borrowed PNG is named in assets/README.md AND its actual file bytes hash to a sha256 the ledger records. */
	@Test
	void borrowedPngsAreGroundedInTheAssetsLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
		List<String> tViolations = new ArrayList<>();
		try (var tWalk = Files.walk(Path.of(GT6GasCylinderDatagenTest.class
				.getResource("/assets/gt6/textures/block/gas_cylinder").toURI()))) {
			List<Path> tPngs = tWalk.filter(p -> p.toString().endsWith(".png")).toList();
			for (Path tPng : tPngs) {
				String tName = tPng.getFileName().toString();
				String tHex = HexFormat.of().formatHex(tSha256.digest(Files.readAllBytes(tPng)));
				if (!tReadme.contains(tName)) tViolations.add(tName + " — filename absent from assets/README.md");
				if (!tReadme.contains(tHex)) tViolations.add(tName + " — bytes hash to " + tHex + ", not grounded in the ledger");
			}
		}
		assertTrue(tViolations.isEmpty(), "borrowed gas cylinder texture without attribution: " + tViolations);
	}

	// ---------------------------------------------------------------------------
	// the generated tree (blockstates, model geometry, item models, recipes)
	// ---------------------------------------------------------------------------

	@Test
	void generatedBlockstatesAreSingleStateOnTheSharedModel() throws Exception {
		for (GT6GasCylinders.GasCylinderRow tRow : GT6GasCylinders.ROWS) {
			JsonObject tState = generatedJson("assets/gt6/blockstates/" + tRow.path() + ".json");
			assertTrue(tState.has("variants"), tRow.path() + ": the plain-variants form (no multipart)");
			var tVariants = tState.getAsJsonObject("variants");
			assertEquals(1, tVariants.size(), tRow.path() + ": exactly the default \"\" row (property-free block)");
			JsonObject tModel = tVariants.getAsJsonObject("");
			assertEquals("gt6:block/barometer_gas_cylinder", tModel.get("model").getAsString(),
					tRow.path() + ": the row points at the ONE shared bell model");
		}
	}

	/**
	 * The bell geometry pin: 10 elements = the 5 upstream passes + their 5 overlay shells
	 * (the 0.01-plate form), each box in its texture band — the bell body (passes 0-2)
	 * tiles sides, the neck column (pass 3) and barometer arm (pass 4) tile the barometer
	 * band on every face (the :141 {@code aRenderPass > 2} branch).
	 */
	@Test
	void theSharedModelPinsTheBellGeometry() throws Exception {
		JsonObject tModel = generatedJson("assets/gt6/models/block/barometer_gas_cylinder.json");
		assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), "the two-layer cutout form");
		JsonArray tElements = tModel.getAsJsonArray("elements");
		assertEquals(10, tElements.size(), "5 body boxes + 5 overlay shells (the potElement grammar)");
		List<String> tSidesBoxes = List.of(
				"[4,0,5][12,8,11]",                    // :120 pass 0 — the crossed slab pair (Gson writes integral floats bare)
				"[5,0,4][11,8,12]",                    // :121 pass 1
				"[5,8,5][11,9,11]",                    // :122 pass 2 — the shoulder plate
				"[3.99,-0.01,4.99][12.01,8.01,11.01]", // the shells
				"[4.99,-0.01,3.99][11.01,8.01,12.01]",
				"[4.99,7.99,4.99][11.01,9.01,11.01]");
		List<String> tBarometerBoxes = List.of(
				"[7,9,7][9,16,9]",                     // :123 pass 3 — the neck column, full height
				"[6,10,6][10,14,10]",                  // :124 pass 4 — the barometer arm
				"[6.99,8.99,6.99][9.01,16.01,9.01]",
				"[5.99,9.99,5.99][10.01,14.01,10.01]");
		Set<String> tSeen = new HashSet<>();
		for (var tElement : tElements) {
			JsonObject tElem = tElement.getAsJsonObject();
			String tBox = tElem.getAsJsonArray("from").toString() + tElem.getAsJsonArray("to").toString(); // the Gson compact form
			String tEast = tElem.getAsJsonObject("faces").getAsJsonObject("east").get("texture").getAsString();
			if (tEast.contains("barometer")) {
				assertTrue(tBarometerBoxes.contains(tBox), "the barometer band box " + tBox + " is an upstream pass 3/4 element (+shell)");
			} else {
				assertTrue(tEast.contains("sides"), "the bell body tiles the sides band (the :141 horizontal branch)");
				assertTrue(tSidesBoxes.contains(tBox), "the sides band box " + tBox + " is an upstream pass 0/1/2 element (+shell)");
			}
			tSeen.add(tBox);
		}
		assertEquals(10, tSeen.size(), "ten distinct boxes, no duplicates");
		for (String tBox : tSidesBoxes) assertTrue(tSeen.contains(tBox), "the sides box " + tBox + " generated");
		for (String tBox : tBarometerBoxes) assertTrue(tSeen.contains(tBox), "the barometer box " + tBox + " generated");
	}

	@Test
	void generatedItemModelsParentTheSharedModel() throws Exception {
		for (GT6GasCylinders.GasCylinderRow tRow : GT6GasCylinders.ROWS) {
			JsonObject tItem = generatedJson("assets/gt6/models/item/" + tRow.path() + ".json");
			assertEquals("gt6:block/barometer_gas_cylinder", tItem.get("parent").getAsString(),
					tRow.path() + ": the item parents the shared block model");
		}
	}

	/** The four :2101-2104 inline grids — "RCR"/"BCh"/"TPd" with the h/d tool marks. */
	@Test
	void theRecipesCarryTheInlineGrid() throws Exception {
		for (GT6GasCylinders.GasCylinderRow tRow : GT6GasCylinders.ROWS) {
			JsonObject tRecipe = generatedJson("data/gt6/recipes/" + tRow.path() + ".json");
			assertEquals("minecraft:crafting_shaped", tRecipe.get("type").getAsString(), tRow.path() + ": the shaped form");
			JsonArray tPattern = tRecipe.getAsJsonArray("pattern");
			assertEquals("RCR", tPattern.get(0).getAsString(), tRow.path() + ": row 1 (ring/plateCurved/ring)");
			assertEquals("BCh", tPattern.get(1).getAsString(), tRow.path() + ": row 2 (round/plateCurved/hammer)");
			assertEquals("TPd", tPattern.get(2).getAsString(), tRow.path() + ": row 3 (screw/plate/screwdriver)");
			var tKey = tRecipe.getAsJsonObject("key");
			assertEquals(7, tKey.size(), tRow.path() + ": R C B T P h d — the :2101 letter set");
			assertTrue(tKey.getAsJsonObject("h").toString().contains("tools/hard_hammer"), tRow.path() + ": the hammer tool mark");
			assertTrue(tKey.getAsJsonObject("d").toString().contains("tools/screwdriver"), tRow.path() + ": the screwdriver tool mark");
			JsonObject tResult = tRecipe.getAsJsonObject("result");
			assertTrue(tResult.toString().contains("gt6:" + tRow.path()), tRow.path() + ": the result is the row's own item");
		}
	}
}
