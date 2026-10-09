package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import gregapi.data.ANY;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The b6 small-maps row-stock pour test (task recipe-b6-small-maps-batch, the b2 family
 * method): the three in-domain JSON faces over the beam-fireproof-closeout chain tip.
 *
 * <p>LAMINATOR — the wire-insulation segment, tail-append AFTER the beam segment: the
 * MultiTileEntityWireElectric.java:95-107 ANY.Rubber x 10-statement insulation walk
 * (plate x1/x1/x2/x3/x4 at dur 16/16/32/48/64 over wireGt01/02/04/08/12 -> cableGt01/02/04/08/12,
 * plus the foil x4/x4/x8/x12/x16 twin legs) over the 28 cable-bearing GTWireSpecs electric
 * rows = 280 rows, plus the Loader_MultiTileEntities.java:1904-1911 redstone insulation face
 * over the 3 GTWireSpecs REDSTONE_ROWS = 6 rows. Card erratum declared in the file head: the
 * task's "49 stations" counted Woods 36 + pistons 3 + WireElectric 10 and excluded the
 * redstone face; the redstone rows pour because the port carriers exist. The Woods
 * log/plank/FR fire-proofing faces stay OUT (no log/plank FireProof twins — the family landed
 * for beams only; Forestry out of domain); the three sticky-piston listener stations are the
 * row-0 smoke row (slime leg verbatim, tar/resin legs unported).
 *
 * <p>LASERENGRAVER — ZERO pour, the 36 upstream stations are accounted in the file head
 * (13 unported circuit outputs + 3 MD.MO + 3 unported generator modules + 8 stoneshapes
 * TRUE NEGATIVE + 9 lens-colour-axis stations); the smoke row keeps the file alive.
 *
 * <p>INJECTOR — the Food/Other/Temp/Vanilla static faces: Temporary:64-75 coolant walk = 12
 * rows, Food:596-603 the FL.waters x4 walk = 21 rows (:598 Ambrosium U9 mass face ships count
 * 1 = the v1 no-mass deviation), Other:121-127 = 20 rows over the live ANY.Diamond/ANY.Glowstone
 * walks, Vanilla:904-905 the FluidsGT.SLIME walk = 3 rows (HashSet pinned to the FL.java:279-281
 * declaration order). Declared skips: Other:489 dyed-CFoam AIR walk (no per-colour fresh-foam
 * items) and :602-605 GlowGlass (unported blocks).
 *
 * <p>RECOMPUTABILITY: the wire rows recompute live from the GTWireSpecs table (a wire/cable
 * registration change turns the walk red until the JSON follows); the injector ANY walks
 * recompute live from the material registry; the id universes are the registration walks.
 */
public class GT6RecipeMapDataB6SmallMapsRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	private static final int LAM_FILE_CENSUS = 329; // 1 smoke + 42 beam + 286 b6 wire rows
	private static final int LAM_WIRE_CENSUS = 286; // 280 electric + 6 redstone
	private static final int INJ_FILE_CENSUS = 57;  // 1 smoke + 56 poured rows
	private static final int LAS_FILE_CENSUS = 448; // was 1 (the smoke row only); +447 task circuit-chain-recipes — the 447 lens-band rows (11 green x 27 + 15 red x 10), the review-seat ratchet; the 36-station head accounting stays (13 of the 36 now POURED via the two listener bands)

	/** The wire-insulation leg table, MultiTileEntityWireElectric.java:96-106 verbatim: size -> {wire tail, dur, plate count, foil count, plate line, foil line}. */
	private static final long[][] WIRE_LEGS = {{1, 16, 1, 4, 96, 102}, {2, 16, 1, 4, 97, 103}, {4, 32, 2, 8, 98, 104}, {8, 48, 3, 12, 99, 105}, {12, 64, 4, 16, 100, 106}};

	@BeforeEach
	void freshGeneration() {
		GTMaterialItems.initMaterials();
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
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6RecipeMapDataB6SmallMapsRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	private String head(String aKey) throws Exception {
		InputStream tStream = GT6RecipeMapDataB6SmallMapsRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + aKey + ".json");
		assertNotNull(tStream);
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		return tDoc.has("comment") ? tDoc.get("comment").getAsString() : tDoc.get("_header").getAsString();
	}

	// ------------------------------------------------------------------ census

	/** The census: the three files pour their full expected row counts — no WARN-skipped rows, no truncation. */
	@Test
	public void theFilesPourTheirFullCensusWithZeroSkips() throws Exception {
		pourShipped("laminator");
		assertEquals(LAM_FILE_CENSUS, GT6RecipeMapJsonLoader.pouredCount("laminator"),
				"laminator: the map holds the full stock (the b6 tail-append over the beam segment)");
		pourShipped("injector");
		assertEquals(INJ_FILE_CENSUS, GT6RecipeMapJsonLoader.pouredCount("injector"), "injector: the four static faces");
		pourShipped("laserengraver");
		assertEquals(LAS_FILE_CENSUS, GT6RecipeMapJsonLoader.pouredCount("laserengraver"), "laserengraver: the smoke row only");
	}

	/** The smoke rows stay row 0 in all three files (the w2-eu-special stock, the tail-append seam). */
	@Test
	public void theSmokeRowsStayPut() throws Exception {
		assertEquals("minecraft:sticky_piston", firstOutput(pourShipped("laminator").get(0).getAsJsonObject()),
				"laminator: the w2-eu-special sticky-piston row stays row 0");
		assertEquals("minecraft:arrow", firstOutput(pourShipped("injector").get(0).getAsJsonObject()),
				"injector: the w2-eu-core stick+flint arrow row stays row 0");
		assertEquals("minecraft:name_tag", firstOutput(pourShipped("laserengraver").get(0).getAsJsonObject()),
				"laserengraver: the paper+ink name-tag row stays row 0");
	}

	private static String firstOutput(JsonObject aRow) {
		return aRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString();
	}

	// ------------------------------------------------------------------ declarations

	/** The file-head declaration pins: the b6 wire segment, the card erratum, the dropped faces, the beam header intact. */
	@Test
	public void theLaminatorHeadKeepsItsDeclarations() throws Exception {
		String tHead = head("laminator"), tLower = tHead.toLowerCase(Locale.ROOT);
		// the beam card's own declarations survive the append (the chain seam)
		assertTrue(tHead.contains("Loader_Recipes_Woods.java:46-72"), "the beam walk citation is intact");
		assertTrue(tHead.contains("Row census: 42 beam rows"), "the beam segment's own census statement is intact");
		assertTrue(tLower.contains("b6"), "the beam header's b6 serial note is intact");
		// the b6 segment
		assertTrue(tHead.contains("MultiTileEntityWireElectric.java:95-107"), "the wire-insulation walk is cited");
		assertTrue(tHead.contains("Loader_MultiTileEntities.java:1904-1911"), "the redstone insulation face is cited");
		assertTrue(tLower.contains("erratum"), "the 49-station card erratum is declared");
		assertTrue(tHead.contains("ANY.Rubber = {Rubber}"), "the single-member rubber walk is pinned");
		assertTrue(tLower.contains("fireproof twins have no port blocks"), "the Woods log/plank/FR drop is declared");
		assertTrue(tHead.contains("itemTar") && tHead.contains("itemResin"), "the piston listener stations are accounted");
		assertTrue(tHead.contains("286 b6 wire rows + 1 smoke row = 329"), "the census statement is present");
	}

	/** The injector head: the four faces, the waters order, the U9 deviation, the SLIME pin, the two declared skips. */
	@Test
	public void theInjectorHeadKeepsItsDeclarations() throws Exception {
		String tHead = head("injector"), tLower = tHead.toLowerCase(Locale.ROOT);
		assertTrue(tHead.contains("Loader_Recipes_Temporary.java:64-75"), "the coolant walk is cited");
		assertTrue(tHead.contains("Loader_Recipes_Food.java:596-603") && tHead.contains("FL.java:689"), "the Food waters walk is cited with its order anchor");
		assertTrue(tHead.contains("U9") && tLower.contains("no-mass deviation"), "the Ambrosium U9 mass deviation is declared");
		assertTrue(tHead.contains("Loader_Recipes_Other.java:121-127"), "the RefinedObsidian/Glowstone face is cited");
		assertTrue(tHead.contains("FluidsGT.SLIME") && tHead.contains("slime_blue/pinkslime/slime"), "the SLIME HashSet pin order is declared");
		assertTrue(tHead.contains(":489") && tLower.contains("cfoam"), "the dyed-CFoam AIR walk is declared OUT");
		assertTrue(tHead.contains(":602-605") && tLower.contains("glowglass"), "the GlowGlass walk is declared OUT");
		assertTrue(tHead.contains("56 poured rows + 1 smoke row = 57"), "the census statement is present");
	}

	/** The laserengraver head: the 36-station accounting, zero pour, the lens-colour unlock. */
	@Test
	public void theLaserengraverHeadKeepsItsDeclarations() throws Exception {
		String tHead = head("laserengraver"), tLower = tHead.toLowerCase(Locale.ROOT);
		assertTrue(tHead.contains("36 upstream stations"), "the 36-station accounting is declared");
		assertTrue(tHead.contains("Loader_Recipes_Other.java:147-167"), "the 13 circuit-output stations are cited");
		assertTrue(tHead.contains("Loader_Recipes_Temporary.java:215-221"), "the MD.MO stations are cited");
		assertTrue(tHead.contains("Loader_Recipes_OreDict.java:76-78"), "the generator-module stations are cited");
		assertTrue(tLower.contains("stoneshapes") && tLower.contains("true negative"), "the stoneshapes TRUE NEGATIVE is declared (the b2c-sawing RM.java ruling)");
		assertTrue(tLower.contains("lens-colour axis is unported") || tHead.contains("craftingLens"), "the lens-colour axis defer is declared");
		assertTrue(tHead.contains("GT6CraftingRecipes.java:1642"), "the port's own crystal-circuit absence declaration is cited");
	}

	// ------------------------------------------------------------------ live wire walk

	/**
	 * The live cross-check — recompute the wire-insulation face from the GTWireSpecs table (the
	 * 28 cable-bearing electric rows + the 3 redstone rows) and require the shipped rows to
	 * cover it EXACTLY, both directions.
	 */
	@Test
	public void theWireRowsMatchTheLiveGTWireSpecs() throws Exception {
		Set<String> tExpected = new HashSet<>();
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) {
			if (!tVariant.insulated()) continue;
			String tCable = "gt6:" + GTWireSpecs.registryName(tVariant);
			int tSize = tVariant.size(), tLine = 0;
			for (long[] tLeg : WIRE_LEGS) if (tLeg[0] == tSize) tLine = (int) tLeg[4];
			assertTrue(tLine > 0, "the leg table covers every insulated size: " + tSize);
			tExpected.add(tCable + "|plate");
			tExpected.add(tCable + "|foil");
		}
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) {
			if (!tVariant.insulated()) continue; // the walk covers the cable forms only (the bare wires are the signal face, not the insulation stock)
			String tCable = "gt6:" + GTWireSpecs.registryName(tVariant);
			tExpected.add(tCable + "|plate");
			tExpected.add(tCable + "|foil");
		}
		Set<String> tShipped = new HashSet<>();
		int tWireRows = 0;
		for (JsonElement tElement : pourShipped("laminator")) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment")) continue; // the smoke row
			String tCite = tRow.get("comment").getAsString();
			if (!tCite.startsWith("MultiTileEntityWireElectric.java:") && !tCite.startsWith("Loader_MultiTileEntities.java:")) continue; // the beam rows
			tWireRows++;
			JsonArray tIn = tRow.getAsJsonArray("inputs"), tOut = tRow.getAsJsonArray("outputs");
			String tWire = tIn.get(1).getAsJsonObject().get("item").getAsString();
			String tCable = tOut.get(0).getAsJsonObject().get("item").getAsString();
			// the wire id equals the cable id with the wire_/cable_ family prefix swapped (the registryName composition)
			assertEquals(tCable.replaceFirst("cable_", "wire_"), tWire, "the pair shape: " + tCite);
			tShipped.add(tCable + "|" + (tIn.get(0).getAsJsonObject().get("item").getAsString().endsWith("plate_rubber") ? "plate" : "foil"));
		}
		assertEquals(LAM_WIRE_CENSUS, tWireRows, "laminator: the cited wire stock is the census");
		Set<String> tMissing = new HashSet<>(tExpected);
		tMissing.removeAll(tShipped);
		Set<String> tStale = new HashSet<>(tShipped);
		tStale.removeAll(tExpected);
		assertTrue(tMissing.isEmpty(), "laminator: the frozen snapshot trails the live GTWireSpecs walk (re-pour needed): " + tMissing);
		assertTrue(tStale.isEmpty(), "laminator: the snapshot holds rows the live walk no longer generates: " + tStale);
	}

	/** The wax-beam segment is untouched by the tail-append: the 42 beam citations survive (the chain seam). */
	@Test
	public void theBeamSegmentSurvivesTheAppend() throws Exception {
		int tBeamRows = 0;
		for (JsonElement tElement : pourShipped("laminator")) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().startsWith("Loader_Recipes_Woods.java:")) tBeamRows++;
		}
		assertEquals(42, tBeamRows, "the beam stock is intact after the b6 append");
	}

	// ------------------------------------------------------------------ live injector walks

	/**
	 * The injector ANY-family walks recompute live over the material registry: the :123
	 * RefinedObsidian rows must cover the ANY.Diamond members' dust items exactly, and the
	 * :126 RefinedGlowstone rows the ANY.Glowstone members', both directions.
	 */
	@Test
	public void theInjectorAnyWalksMatchTheLiveRegistry() throws Exception {
		Set<String> tDiamond = new HashSet<>(), tGlowstone = new HashSet<>();
		for (OreDictMaterial tMat : ANY.Diamond.mToThis) tDiamond.add("gt6:" + GTMaterialItems.itemIdOf(OP.dust, tMat));
		for (OreDictMaterial tMat : ANY.Glowstone.mToThis) tGlowstone.add("gt6:" + GTMaterialItems.itemIdOf(OP.dust, tMat));
		Set<String> tShippedDiamond = new HashSet<>(), tShippedGlowstone = new HashSet<>();
		for (JsonElement tElement : pourShipped("injector")) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment")) continue;
			String tCite = tRow.get("comment").getAsString();
			JsonArray tIn = tRow.has("inputs") ? tRow.getAsJsonArray("inputs") : new JsonArray();
			if (tCite.startsWith("Loader_Recipes_Other.java:123")) {
				assertEquals("gt6:dust_obsidian", tIn.get(0).getAsJsonObject().get("item").getAsString(), ":123 leads with the obsidian dust");
				tShippedDiamond.add(tIn.get(1).getAsJsonObject().get("item").getAsString());
			} else if (tCite.startsWith("Loader_Recipes_Other.java:126")) {
				tShippedGlowstone.add(tIn.get(0).getAsJsonObject().get("item").getAsString());
			}
		}
		assertEquals(tDiamond, tShippedDiamond, ":123 the ANY.Diamond walk must match the live registry both directions");
		assertEquals(tGlowstone, tShippedGlowstone, ":126 the ANY.Glowstone walk must match the live registry both directions");
	}

	/** The water-leg walks: the Temp coolant rows and the Food rows each carry exactly the FL.java:689 four-leg family. */
	@Test
	public void theWaterLegsAreTheFlWatersFamily() throws Exception {
		List<String> tWaters = List.of("minecraft:water", "gt6:mnwtr", "gt6:distilled_water", "gt6:spdew");
		Map<String, Set<String>> tByItem = new LinkedHashMap<>();
		for (JsonElement tElement : pourShipped("injector")) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment") || !tRow.has("inputs") || tRow.getAsJsonArray("inputs").size() != 1) continue;
			String tCite = tRow.get("comment").getAsString();
			if (!tCite.startsWith("Loader_Recipes_Temporary.java:") && !tCite.startsWith("Loader_Recipes_Food.java:")) continue;
			String tItem = tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
			tByItem.computeIfAbsent(tItem, k -> new HashSet<>())
					.add(tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString());
		}
		// the three coolant dusts + Mg/Ca/Ambrosium/Holystone dusts each walk the full four-leg family
		for (String tItem : new String[] {"gt6:dust_lapis", "gt6:dust_lazurite", "gt6:dust_sodalite",
				"gt6:dust_magnesium", "gt6:dust_calcium", "gt6:dust_ambrosium", "gt6:dust_holystone"}) {
			assertEquals(new HashSet<>(tWaters), tByItem.get(tItem), tItem + " walks the FL.waters family exactly");
		}
	}

	/** The FluidsGT.SLIME walk: exactly three bawls legs, pinned to the FL.java declaration order slime_blue/pinkslime/slime. */
	@Test
	public void theSlimeWalkIsThePinnedThreeLegFamily() throws Exception {
		List<String> tOrder = List.of("slime_blue", "pinkslime", "slime");
		int tIndex = 0;
		for (JsonElement tElement : pourShipped("injector")) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment") || !tRow.get("comment").getAsString().startsWith("Loader_Recipes_Vanilla.java:905")) continue;
			assertTrue(tIndex < tOrder.size(), "no more than three bawls rows");
			assertEquals("gt6:" + tOrder.get(tIndex), tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString(),
					"the bawls walk follows the FL.java:279-281 declaration order");
			tIndex++;
		}
		assertEquals(tOrder.size(), tIndex, "exactly three slime legs");
	}

	// ------------------------------------------------------------------ verbatim pins

	/** Verbatim pins — the wire leg shapes over the first material and the ramp tail, plus the redstone face. */
	@Test
	public void wireRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("laminator");
		// MultiTileEntityWireElectric.java:96 — tin 1x, rubber plate x1, dur 16
		assertWireRow(tRows, "gt6:plate_rubber", 1, "gt6:wire_tin_gt01", "gt6:cable_tin_gt01", 16, "MultiTileEntityWireElectric.java:96");
		// :98 — tin 4x, plate x2, dur 32
		assertWireRow(tRows, "gt6:plate_rubber", 2, "gt6:wire_tin_gt04", "gt6:cable_tin_gt04", 32, "MultiTileEntityWireElectric.java:98");
		// :106 — tin 12x, foil x16, dur 64
		assertWireRow(tRows, "gt6:foil_rubber", 16, "gt6:wire_tin_gt12", "gt6:cable_tin_gt12", 64, "MultiTileEntityWireElectric.java:106");
		// Loader_MultiTileEntities.java:1905 — the redstone face, plate x1, dur 16
		assertWireRow(tRows, "gt6:plate_rubber", 1, "gt6:wire_red_alloy", "gt6:cable_red_alloy", 16, "Loader_MultiTileEntities.java:1905");
		// :1910 — the lumium foil tail, foil x4, dur 16
		assertWireRow(tRows, "gt6:foil_rubber", 4, "gt6:wire_lumium", "gt6:cable_lumium", 16, "Loader_MultiTileEntities.java:1910");
	}

	private static void assertWireRow(JsonArray aRows, String aAdditive, long aCount, String aWire, String aCable, long aDur, String aCite) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("inputs") || !tRow.has("outputs")) continue;
			JsonArray tIn = tRow.getAsJsonArray("inputs"), tOut = tRow.getAsJsonArray("outputs");
			if (tIn.size() != 2 || tOut.size() != 1) continue;
			if (aAdditive.equals(tIn.get(0).getAsJsonObject().get("item").getAsString())
					&& aCount == tIn.get(0).getAsJsonObject().get("count").getAsLong()
					&& aWire.equals(tIn.get(1).getAsJsonObject().get("item").getAsString())
					&& aCable.equals(tOut.get(0).getAsJsonObject().get("item").getAsString())) {
				assertEquals(aDur, tRow.get("duration").getAsLong(), aCite + " duration");
				assertEquals(16, tRow.get("eut").getAsLong(), aCite + " eut");
				assertTrue(tRow.get("comment").getAsString().startsWith(aCite), aCite + " citation rides the row");
				return;
			}
		}
		throw new AssertionError("no wire row " + aAdditive + " x" + aCount + " + " + aWire + " -> " + aCable);
	}

	/** Verbatim pins — the injector faces: coolant, holywater, soda, the diamond walk, gloomstone, bawls. */
	@Test
	public void injectorRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("injector");
		// Temporary:64 — lapis dust x1 + distilled water 1000 -> ic2coolant 1000, dur/eut 16
		assertFluidRow(tRows, "Loader_Recipes_Temporary.java:64", "gt6:dust_lapis", 1, "gt6:distilled_water", 1000, "gt6:ic2coolant", 1000, 16);
		// Temporary:75 — sodalite dust x2 + water 1000
		assertFluidRow(tRows, "Loader_Recipes_Temporary.java:75", "gt6:dust_sodalite", 2, "minecraft:water", 1000, "gt6:ic2coolant", 1000, 16);
		// Food:598 — the Ambrosium U9 mass face ships count 1 (the declared deviation), spdew leg
		assertFluidRow(tRows, "Loader_Recipes_Food.java:598", "gt6:dust_ambrosium", 1, "gt6:spdew", 1000, "gt6:holywater", 1000, 16);
		// Food:601 — the soda leg: water 250 (FL.mul 1/4) + CO2 36 (MT.CO2.gas(U4))
		assertFluidRow(tRows, "Loader_Recipes_Food.java:601", null, 0, "minecraft:water", 250, "gt6:soda", 250, 16);
		// Other:123 — the diamond-walk anchor row (lead member order-free: find by output+germanium)
		assertGermaniumRow(tRows, "gt6:dust_diamond", "gt6:ingot_obsidian_refined", 256);
		// Other:126 — gloomstone
		assertGermaniumRow(tRows, "gt6:dust_gloomstone", "gt6:ingot_glowstone_refined", 256);
		// Other:127 — the re-melt
		assertGermaniumRow(tRows, "gt6:dust_obsidian_refined", "gt6:ingot_obsidian_refined", 256);
		// Vanilla:905 — bawls over the slime family, CO2 36
		boolean tBawls = false;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment") || !tRow.get("comment").getAsString().startsWith("Loader_Recipes_Vanilla.java:905")) continue;
			assertEquals(36, tRow.getAsJsonArray("fluidInputs").get(1).getAsJsonObject().get("amount").getAsLong(), "the CO2 U4 leg is 36");
			assertEquals("gt6:bawls", tRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString());
			tBawls = true;
		}
		assertTrue(tBawls, "the bawls face poured");
	}

	private static void assertFluidRow(JsonArray aRows, String aCite, String aItem, long aItemCount, String aFin, long aFinAmount, String aFout, long aFoutAmount, long aDur) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment") || !tRow.get("comment").getAsString().startsWith(aCite)) continue;
			// multi-leg rows repeat the citation per water leg — the fluid leg disambiguates
			if (!aFin.equals(tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString())) continue;
			if (aItem != null) {
				assertEquals(aItem, tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString(), aCite + " input");
				assertEquals(aItemCount, tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("count").getAsLong(), aCite + " count");
			}
			assertEquals(aFin, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString(), aCite + " fluid in");
			assertEquals(aFinAmount, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsLong(), aCite + " amount in");
			assertEquals(aFout, tRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString(), aCite + " fluid out");
			assertEquals(aFoutAmount, tRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsLong(), aCite + " amount out");
			assertEquals(aDur, tRow.get("duration").getAsLong(), aCite + " duration");
			assertEquals(16, tRow.get("eut").getAsLong(), aCite + " eut");
			return;
		}
		throw new AssertionError("no row cited " + aCite);
	}

	private static void assertGermaniumRow(JsonArray aRows, String aDust, String aIngot, long aDur) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("inputs")) continue;
			JsonArray tIn = tRow.getAsJsonArray("inputs");
			boolean tHasDust = false;
			for (JsonElement tSlot : tIn) if (aDust.equals(tSlot.getAsJsonObject().get("item").getAsString())) tHasDust = true;
			if (!tHasDust) continue;
			assertEquals("gt6:germanium_molten", tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString(), aDust + " Ge leg");
			assertEquals(144, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsLong(), aDust + " Ge = U = 144");
			assertEquals(aIngot, tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString(), aDust + " output");
			assertEquals(aDur, tRow.get("duration").getAsLong(), aDust + " duration");
			return;
		}
		throw new AssertionError("no germanium row for " + aDust);
	}

	// ------------------------------------------------------------------ id universes

	/** The item-universe face: every gt6 item id the three files reference resolves in the registration walks — the GTMaterialItems universe for prefix items, the GTWireSpecs table for the wire/cable family. */
	@Test
	public void everyItemIdTheFilesReferenceIsRegistered() throws Exception {
		Set<String> tMaterialItems = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tMaterialItems.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		// task circuit-chain-recipes — the crystal/wiring circuit outputs (the lens bands'
		// outputs ride this walk, the card's own id-universe pin shape)
		for (var tRow : gregtech6.registry.GT6CircuitChain.ROWS) tMaterialItems.add("gt6:" + tRow.path());
		Set<String> tWireItems = new HashSet<>();
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) {
			tWireItems.add("gt6:" + GTWireSpecs.registryName(tVariant));
		}
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) {
			tWireItems.add("gt6:" + GTWireSpecs.registryName(tVariant));
		}
		Set<String> tMissing = new HashSet<>();
		for (String tKey : new String[] {"laminator", "injector", "laserengraver"}) {
			for (JsonElement tElement : pourShipped(tKey)) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"inputs", "outputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("item").getAsString();
						if (!tId.startsWith("gt6:")) continue; // minecraft ids resolve live
						if (tId.endsWith("_beam") || tId.endsWith("_fireproof")) continue; // the beam-card segment, its own test's universe
						if (!tMaterialItems.contains(tId) && !tWireItems.contains(tId)) tMissing.add(tKey + ": " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 item ids (these rows would WARN-skip live): " + tMissing);
	}

	/** The fluid-universe face: every gt6 fluid id the files reference resolves in a GTFluids spec table (the b2c shape). */
	@Test
	public void everyFluidIdTheFilesReferenceIsRegistered() throws Exception {
		Set<String> tMissing = new HashSet<>();
		for (String tKey : new String[] {"laminator", "injector"}) {
			for (JsonElement tElement : pourShipped(tKey)) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"fluidInputs", "fluidOutputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("fluid").getAsString();
						if (!tId.startsWith("gt6:")) continue; // minecraft:water resolves live
						String tPath = tId.substring(4);
						if (!fluidRegistered(tPath)) tMissing.add(tKey + ": " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 fluid ids (these rows would WARN-skip live): " + tMissing);
	}

	/** The offline fluid-universe lookup: the union of ALL the GTFluids spec-table lookups (the b2c shape). */
	private static boolean fluidRegistered(String aPath) {
		return gregtech6.fluid.GTFluids.chemicalSpec(aPath) != null || gregtech6.fluid.GTFluids.closureSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.hotSpec(aPath) != null || gregtech6.fluid.GTFluids.lubricantSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.honeySpec(aPath) != null || gregtech6.fluid.GTFluids.beeRowSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.quSpec(aPath) != null || gregtech6.fluid.GTFluids.namingSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.aquaSpec(aPath) != null || gregtech6.fluid.GTFluids.simpleLiquidSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.foodSpec(aPath) != null || gregtech6.fluid.GTFluids.foodB1Spec(aPath) != null
				|| gregtech6.fluid.GTFluids.foodB2Spec(aPath) != null || gregtech6.fluid.GTFluids.foodTailSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.engineSpec(aPath) != null || gregtech6.fluid.GTFluids.dyeIndexOf(aPath) >= 0;
	}

	// ------------------------------------------------------------------ the retired mixerchem note

	/** The mixerchem HeatMixer pool note is retired (the b6b transcription carries the face; this card retires the stale claim, header-only). */
	@Test
	public void theMixerchemPoolNoteIsRetired() throws Exception {
		String tHead = head("mixerchem");
		assertFalse(tHead.contains("has NO port map instance"), "the stale pool note is gone");
		assertTrue(tHead.contains("recipe-b6b-small-maps-cnc-assembler-nanofab"), "the b6b transcription is cited");
		assertTrue(tHead.contains("recipe-b6-small-maps-batch"), "the retirement citation is present");
	}
}
