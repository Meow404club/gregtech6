package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.block.tree.GT6BeamKind;
import gregtech6.registry.GT6BeamBlocks;
import gregtech6.registry.GTMaterialItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The beam-fireproof-closeout row-stock pour test: the laminator.json beam segment — the
 * fire-proofing walk of Loader_Recipes_Woods.java:46-72, BEAM segment only (:53-58 the
 * plate x6 leg + :66-71 the foil x24 leg, the card's line-range erratum: the card face
 * cited ":56-72", the actual beam segment starts at :53), 21 woods x 2 legs = 42 rows.
 * Each row: {@code RM.Laminator.addRecipe2(T, 16, 192, wax, beam, beamFireproof)} — eut
 * 16, duration 192, the wax plate x6 / foil x24 additive leading (the addRecipe2 argument
 * order). Pours through the real {@link GT6RecipeMapJsonLoader} seam with zero skips,
 * on top of the w2-eu-special sticky-piston smoke row which stays row 0 (the tail-append
 * seam; the b6 card appends AFTER this segment — the laminator serial note).
 *
 * <p>RECOMPUTABILITY: the live half walks the port beam registration itself
 * (GT6BeamKind.values() x GT6BeamBlocks.path + the {@code _fireproof} twin suffix), and
 * the shipped rows must cover it EXACTLY, both directions, so a registration change (a
 * kind added) turns the walk red until the laminator rows follow.
 *
 * <p>The UNPOURED faces (the file head carries them, this test pins them): the log
 * segment (:48-51/:61-64 — BlocksGT.Log1/A/B/C FireProof blocks are the plank/log
 * FireProof defer, NOT this card), and the Bath fire-resistance walk (:74+ — the potion
 * fluid face is another card's domain).
 */
public class GT6RecipeMapDataLaminatorBeamRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	private static final String LAMINATOR = "laminator";

	/** The frozen census: 42 beam rows + the w2-eu-special smoke row (the conscious-bump ratchet; FILE census bumped 43 -> 329 by task recipe-b6-small-maps-batch, the serial-note tail-append — the b6 wire segment's 286 rows ride AFTER this segment and its own ratchet is GT6RecipeMapDataB6SmallMapsRowsPourTest). */
	private static final int BEAM_CENSUS = 42;
	private static final int FILE_CENSUS = 329;

	/** The two wax legs (the additive item ids + counts, Loader_Recipes_Woods.java:53/:66 shapes). */
	private static final String PLATE = "gt6:plate_wax_refractory";
	private static final String FOIL = "gt6:foil_wax_refractory";
	private static final long PLATE_COUNT = 6;
	private static final long FOIL_COUNT = 24;

	/** The per-kind upstream citation lines: {family line :53-58 plate / :66-71 foil} over the 21 kinds, kind order. */
	private static final Map<String, String> PLATE_LINES = new LinkedHashMap<>();
	private static final Map<String, String> FOIL_LINES = new LinkedHashMap<>();
	static {
		String[] plateFamilies = {"53", "54", "55", "56", "57", "58"};
		String[] foilFamilies = {"66", "67", "68", "69", "70", "71"};
		GT6BeamKind[] tKinds = GT6BeamKind.values();
		for (int i = 0; i < tKinds.length; i++) {
			// Beam1 x4 -> :53/:66, Beam2 x4 -> :54/:67, Beam3 x4 -> :55/:68, BeamA x4 -> :56/:69,
			// BeamB x4 -> :57/:70, BeamC x1 -> :58/:71 (the i/4 family index; BeamC = family 5, 1 wood)
			int tFamily = Math.min(i / 4, 5);
			PLATE_LINES.put(tKinds[i].snake(), plateFamilies[tFamily]);
			FOIL_LINES.put(tKinds[i].snake(), foilFamilies[tFamily]);
		}
	}

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = aId ->
				"gt6".equals(aId.getNamespace()) ? Items.IRON_INGOT : Items.BRICK; // identity stand-ins
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> Fluids.WATER;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads the shipped file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped() throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + LAMINATOR + ".json";
		InputStream tStream = GT6RecipeMapDataLaminatorBeamRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", LAMINATOR), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	private String head() throws Exception {
		InputStream tStream = GT6RecipeMapDataLaminatorBeamRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + LAMINATOR + ".json");
		assertNotNull(tStream);
		return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject().get("comment").getAsString();
	}

	/** The census: the file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void theFilePoursItsFullCensusWithZeroSkips() throws Exception {
		pourShipped();
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(LAMINATOR);
		assertNotNull(tMap, LAMINATOR + " resolves");
		assertEquals(FILE_CENSUS, tMap.mRecipeList.size(), LAMINATOR + ": the map holds the census");
		assertEquals(FILE_CENSUS, GT6RecipeMapJsonLoader.pouredCount(LAMINATOR),
				LAMINATOR + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
	}

	/** The declaration shape pins: the fire-proofing walk anchors, the beam-segment erratum, the unpoured faces, the b6 serial note. */
	@Test
	public void theFileHeadKeepsItsDeclarations() throws Exception {
		String tHead = head(), tLower = tHead.toLowerCase(Locale.ROOT);
		assertTrue(tHead.contains("Loader_Recipes_Woods.java:46-72"), "the fire-proofing walk is cited");
		assertTrue(tHead.contains(":53-58") && tHead.contains(":66-71"), "the beam segment's plate and foil families are cited");
		assertTrue(tLower.contains("erratum") && tHead.contains(":56-72"), "the card's line-range erratum is declared");
		assertTrue(tHead.contains("eut") && tHead.contains("16") && tHead.contains("192"), "the wrapper constants (eut 16 / duration 192) are cited");
		assertTrue(tLower.contains("log fireproof segment") && tHead.contains(":48-51") && tHead.contains(":61-64"),
				"the log FireProof segment is declared OUT (the plank/log FireProof defer)");
		assertTrue(tLower.contains("bath") && tHead.contains(":74"), "the Bath fire-resistance walk is declared OUT (the potion fluid face)");
		assertTrue(tLower.contains("b6"), "the laminator serial note vs the b6 plastic-sheet card is declared");
		assertTrue(tLower.contains("smoke row") && tHead.contains("sticky-piston"), "the w2-eu-special smoke row stays row 0");
		assertTrue(tHead.contains("LAMINATOR") && tLower.contains("pinned 0"), "the PhaseGate zero-bump posture is declared");
		assertTrue(tHead.contains("Row census: 42 beam rows"), "the row census statement is present");
	}

	/** The smoke row stays row 0 (the eu-special stock) and every beam row carries its line citation. */
	@Test
	public void theSmokeRowStaysPutAndBeamRowsCarryCitations() throws Exception {
		JsonArray tRows = pourShipped();
		JsonObject tSmoke = tRows.get(0).getAsJsonObject();
		assertEquals("minecraft:sticky_piston", tSmoke.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString(),
				"the w2-eu-special sticky-piston row stays row 0");
		int tBeamRows = 0;
		for (int i = 1; i < tRows.size(); i++) {
			JsonObject tRow = tRows.get(i).getAsJsonObject();
			assertTrue(tRow.has("comment"), LAMINATOR + ": every row carries its citation");
			String tCite = tRow.get("comment").getAsString();
			if (!tCite.startsWith("Loader_Recipes_Woods.java:")) continue; // the b6 wire segment (task recipe-b6-small-maps-batch) rides its own citations
			assertTrue(tCite.startsWith("Loader_Recipes_Woods.java:"), LAMINATOR + ": the citation names the upstream line");
			assertTrue(tCite.contains("; plate x6)") || tCite.contains("; foil x24)"), LAMINATOR + ": the citation names the wax leg");
			tBeamRows++;
		}
		assertEquals(BEAM_CENSUS, tBeamRows, LAMINATOR + ": the cited beam stock is the census");
	}

	/**
	 * The per-source ratchet: the frozen accounting (21 woods x {plate, foil}, the family
	 * line citations) must equal the shipped row stock partitioned by its citation AND the
	 * live kind recomputation. A registration change turns the live check red.
	 */
	@Test
	public void theSourceAccountingMatchesTheFrozenPartition() throws Exception {
		Map<String, Integer> tActual = new LinkedHashMap<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment")) continue; // the smoke row
			String tCite = tRow.get("comment").getAsString();
			if (!tCite.startsWith("Loader_Recipes_Woods.java:")) continue; // the b6 wire segment (task recipe-b6-small-maps-batch) rides its own ratchet
			tActual.merge(tCite, 1, Integer::sum);
		}
		assertEquals(PLATE_LINES.size() + FOIL_LINES.size(), tActual.size(),
				LAMINATOR + ": one citation per wood x leg (21 x 2), no duplicates");
		for (Map.Entry<String, String> tLine : PLATE_LINES.entrySet()) {
			assertEquals(1, tActual.getOrDefault("Loader_Recipes_Woods.java:" + tLine.getValue() + " (" + tLine.getKey() + "; plate x6)", 0).intValue(),
					tLine.getKey() + ": exactly one plate row");
			assertEquals(1, tActual.getOrDefault("Loader_Recipes_Woods.java:" + FOIL_LINES.get(tLine.getKey()) + " (" + tLine.getKey() + "; foil x24)", 0).intValue(),
					tLine.getKey() + ": exactly one foil row");
		}
	}

	/**
	 * The live cross-check — recompute the walk over the port beam registration (the
	 * GT6BeamKind universe x the fireproof twin suffix) and require the shipped rows to
	 * cover it EXACTLY, both directions.
	 */
	@Test
	public void theRowsMatchTheLiveBeamRegistration() throws Exception {
		Set<String> tExpected = liveWalk();
		Set<String> tShipped = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment")) continue; // the smoke row
			if (!tRow.get("comment").getAsString().startsWith("Loader_Recipes_Woods.java:")) continue; // the b6 wire segment rides GT6RecipeMapDataB6SmallMapsRowsPourTest
			tShipped.add(rowKey(tRow));
		}
		Set<String> tMissing = new HashSet<>(tExpected);
		tMissing.removeAll(tShipped);
		Set<String> tStale = new HashSet<>(tShipped);
		tStale.removeAll(tExpected);
		assertTrue(tMissing.isEmpty(), LAMINATOR + ": the frozen snapshot trails the live walk (re-pour needed): " + tMissing);
		assertTrue(tStale.isEmpty(), LAMINATOR + ": the frozen snapshot holds rows the live walk no longer generates: " + tStale);
		assertEquals(tExpected.size(), tShipped.size(), LAMINATOR + ": the walk snapshot size equals the live walk");
	}

	/** Verbatim pins — the plate and foil leg shapes over one flammable kind and the single-meta BeamC kind. */
	@Test
	public void rowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped();
		assertVerbatim(tRows, PLATE, PLATE_COUNT, "gt6:oak_beam", "gt6:oak_beam_fireproof", 192, 16);
		assertVerbatim(tRows, FOIL, FOIL_COUNT, "gt6:oak_beam", "gt6:oak_beam_fireproof", 192, 16);
		assertVerbatim(tRows, PLATE, PLATE_COUNT, "gt6:blue_spruce_beam", "gt6:blue_spruce_beam_fireproof", 192, 16);
		assertVerbatim(tRows, FOIL, FOIL_COUNT, "gt6:rubber_beam", "gt6:rubber_beam_fireproof", 192, 16);
	}

	/**
	 * The id/energy face: every gt6 id is registered (the material universe for the wax
	 * legs, the live beam registry for the beams AND their fireproof twins), every beam row
	 * is 2 inputs / 1 output / no fluids / eut 16 / duration 192, and the output is always
	 * the input beam's fireproof twin.
	 */
	@Test
	public void theStockIsRegisteredTypedAndTwinShaped() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tItemUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tItemUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		Set<String> tBeamUniverse = new HashSet<>();
		for (GT6BeamKind tKind : GT6BeamKind.values()) {
			tBeamUniverse.add("gt6:" + GT6BeamBlocks.path(tKind));
			tBeamUniverse.add("gt6:" + GT6BeamBlocks.path(tKind) + "_fireproof");
		}
		int tBeamRows = 0;
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			// the b6 wire segment (task recipe-b6-small-maps-batch) rides its own ratchet — this
			// test's typed/twin-shaped stock face is the BEAM segment only (the serial-note seam)
			if (tRow.has("comment") && !tRow.get("comment").getAsString().startsWith("Loader_Recipes_Woods.java:")) continue;
			for (String tLeg : new String[] {"inputs", "outputs"}) {
				for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
					String tId = tSlot.getAsJsonObject().get("item").getAsString();
					assertTrue(tItemUniverse.contains(tId) || tBeamUniverse.contains(tId)
							|| tId.startsWith("minecraft:"), "unregistered item id (the row would WARN-skip live): " + tId);
				}
			}
			if (!tRow.has("comment")) continue; // the smoke row rides its own card's pins
			tBeamRows++;
			assertEquals(2, tRow.getAsJsonArray("inputs").size(), "the addRecipe2 two-input face");
			assertEquals(1, tRow.getAsJsonArray("outputs").size(), "the single fireproof output");
			assertTrue(!tRow.has("fluidInputs"), "RM.Laminator :90 carries 0/0/0 fluids");
			assertEquals(16, tRow.get("eut").getAsLong(), "eut 16 on every row");
			assertEquals(192, tRow.get("duration").getAsLong(), "duration 192 on every row");
			String tBeam = tRow.getAsJsonArray("inputs").get(1).getAsJsonObject().get("item").getAsString();
			String tOut = tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString();
			assertEquals(tBeam + "_fireproof", tOut, "the output is the input beam's fireproof twin: " + tBeam);
		}
		assertEquals(BEAM_CENSUS, tBeamRows, LAMINATOR + ": the typed beam stock is the census");
	}

	/** The unpoured faces stay out: no log/plank input, no beam->beam (non-twin) output. */
	@Test
	public void theUnpouredFacesStayOut() throws Exception {
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			for (JsonElement tSlot : tRow.getAsJsonArray("inputs")) {
				String tId = tSlot.getAsJsonObject().get("item").getAsString();
				assertTrue(!tId.contains("_log") && !tId.contains("plank"),
						"no log/plank input (the :48-51/:61-64 log segment + the plank FireProof defer stay unpoured): " + tId);
			}
		}
	}

	// ---------------------------------------------------------------- live walk

	/**
	 * The live recomputation: every kind x {plate x6, foil x24} (the :53-58 x :66-71
	 * shape), the fireproof twin output, the wrapper constants.
	 */
	private static Set<String> liveWalk() {
		Set<String> rRows = new HashSet<>();
		for (GT6BeamKind tKind : GT6BeamKind.values()) {
			String tBeam = "gt6:" + GT6BeamBlocks.path(tKind);
			rRows.add(rowKey(PLATE, PLATE_COUNT, tBeam, tBeam + "_fireproof"));
			rRows.add(rowKey(FOIL, FOIL_COUNT, tBeam, tBeam + "_fireproof"));
		}
		return rRows;
	}

	// ---------------------------------------------------------------- helpers

	/** The row identity key: inputs > outputs > duration > eut (the wash rowKey shape, two inputs). */
	private static String rowKey(String aWax, long aWaxCount, String aBeam, String aOut) {
		return "in:" + aWax + ":" + aWaxCount + "+" + aBeam + ":1>out:" + aOut + ":1>dur:192>16";
	}

	private static String rowKey(JsonObject aRow) {
		StringBuilder tKey = new StringBuilder("in:");
		JsonArray tIn = aRow.getAsJsonArray("inputs");
		for (int i = 0; i < tIn.size(); i++) {
			if (i > 0) tKey.append('+');
			JsonObject tSlot = tIn.get(i).getAsJsonObject();
			tKey.append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsLong());
		}
		tKey.append(">out:");
		JsonArray tOut = aRow.getAsJsonArray("outputs");
		for (int i = 0; i < tOut.size(); i++) {
			if (i > 0) tKey.append('+');
			JsonObject tSlot = tOut.get(i).getAsJsonObject();
			tKey.append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsLong());
		}
		tKey.append(">dur:").append(aRow.get("duration").getAsLong()).append('>').append(aRow.get("eut").getAsLong());
		return tKey.toString();
	}

	/** The verbatim pin: the exact row exists with the wrapper constants and the twin output. */
	private static void assertVerbatim(JsonArray aRows, String aWax, long aWaxCount, String aBeam, String aOut,
			long aDuration, long aEut) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			JsonArray tIn = tRow.getAsJsonArray("inputs");
			if (tIn.size() == 2 && aWax.equals(tIn.get(0).getAsJsonObject().get("item").getAsString())
					&& aBeam.equals(tIn.get(1).getAsJsonObject().get("item").getAsString())) {
				assertEquals(aWaxCount, tIn.get(0).getAsJsonObject().get("count").getAsLong(),
						aBeam + ": the wax leg count");
				JsonArray tOut = tRow.getAsJsonArray("outputs");
				assertEquals(1, tOut.size(), aBeam + ": the single fireproof output");
				assertEquals(aOut, tOut.get(0).getAsJsonObject().get("item").getAsString(), aBeam + ": the twin output");
				assertEquals(1, tOut.get(0).getAsJsonObject().get("count").getAsLong(), aBeam + ": twin x1");
				assertEquals(aDuration, tRow.get("duration").getAsLong(), aBeam + ": duration 192");
				assertEquals(aEut, tRow.get("eut").getAsLong(), aBeam + ": eut 16");
				return;
			}
		}
		throw new AssertionError("no verbatim row " + aWax + " + " + aBeam);
	}
}
