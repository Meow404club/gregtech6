package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The recipe-b6b-small-maps-cnc-assembler-nanofab row-stock pour test (the
 * GT6RecipeMapDataB2b1RowsPourTest posture, the hermetic reset-FIRST bracket): the
 * three small-map files — cnc (the Loader_Recipes_Woods.java:259-263 per-plank gear
 * walk), assembler (the Loader_Recipes_OreDict.java:234-238 plank-listener pair) and
 * nanofab (the Loader_Recipes_Other.java:818-894 Graphene stock + the
 * Loader_Recipes_Ores.java:341-344 Dolamide/Dilithium gems) — pour through the real
 * {@link GT6RecipeMapJsonLoader} seam with zero skips, the file-head declarations keep
 * naming the dropped selector legs (the press.json:2 form) and the declared-out faces
 * (the Tool_Matches walk, the P10 FR/BC rows, the wireGt01/pipe nanofab legs), the
 * verbatim spot checks read the shipped rows directly, and the walk faces pin their
 * port-universe membership counts.
 *
 * <p><b>The accounting</b> (re-cut by planks-retired-tail-hygiene against the
 * planks-blockification ship shape): CNC = 28 rows (the GT6WoodDict.ROWS walk — the ONE
 * plank authority that absorbed the retired 130-item OP.plank prefix family, every row's
 * gearGt item exists, times the one-member FluidsGT.LUBRICANT family). The card-spec
 * "5 站" assembler reading was the pre-dump estimate; the upstream实文 is two walk
 * statements, not static rows: assembler = 54 rows (27 plank rows x :237/:238 — the
 * shipped walk carries 27 of the 28 WoodDict rows, the treated-planks row is not a
 * :237/:238 listener member in the planks-blockification re-cut), the :345-351
 * match-stick walk rides NO row (IL.Tool_Matches is not
 * ported), the FR/BC compat rows are the P10 cuts. Nanofab = 45 rows (41 Other + 4
 * Ores); the :841-844 wireGt01 legs and the :876-894 pipe legs ride NO row (unregistered
 * carriers). The HeatMixer face needs NO map — upstream RM.java:75 HeatMixer IS Mixer,
 * the pure alias (the GT6RecipeMapDataB1ChemRowsPourTest flip).
 */
public class GT6RecipeMapDataB6bRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The per-map census of this card: file key -> expected poured rows (zero skips). */
	private static final Map<String, Integer> CENSUS = Map.of(
			"cnc", 28,         // the 28 GT6WoodDict.ROWS plank rows x gear x the 1-member LUBRICANT family
			"assembler", 54,   // 27 plank rows x the :237/:238 listener pair (28 WoodDict rows minus the treated row)
			"nanofab", 45);    // 41 Other:818-894 (the port-registered shapes) + 4 Ores:341-344

	/** The file-head declarations: every file must keep naming its declared-out blockers. */
	private static final Map<String, String> DECLARATIONS = Map.of(
			"cnc", "press.json:2",
			"assembler", "IL.Tool_Matches is not ported",
			"nanofab", "wireGt01");

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.reset(); // the hermetic reset-FIRST bracket (the r11e house rule)
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

	/** Reads one shipped file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6RecipeMapDataB6bRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	private static long countRows(JsonArray aRows, String aCommentFragment) {
		long rCount = 0;
		for (JsonElement tElement : aRows) {
			if (tElement.getAsJsonObject().get("comment").getAsString().contains(aCommentFragment)) rCount++;
		}
		return rCount;
	}

	private static JsonObject findRow(JsonArray aRows, String aCommentFragment) {
		for (JsonElement tElement : aRows) {
			if (tElement.getAsJsonObject().get("comment").getAsString().contains(aCommentFragment)) {
				return tElement.getAsJsonObject();
			}
		}
		throw new AssertionError("no row names " + aCommentFragment);
	}

	/** The census: every file pours its full expected row count onto ITS OWN map — no WARN-skipped rows, no truncation. */
	@Test
	public void everyB6bFilePoursItsFullCensusWithZeroSkips() throws Exception {
		assertEquals(GT6RecipeMaps.CNC, GT6RecipeMapJsonLoader.mapFor("cnc"), "the CNC map instance");
		assertEquals(GT6RecipeMaps.ASSEMBLER, GT6RecipeMapJsonLoader.mapFor("assembler"), "the ASSEMBLER map instance");
		assertEquals(GT6RecipeMaps.NANOFAB, GT6RecipeMapJsonLoader.mapFor("nanofab"), "the NANOFAB map instance");
		for (String tKey : CENSUS.keySet()) {
			pourShipped(tKey);
			RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(tKey);
			assertNotNull(tMap, tKey + " resolves");
			assertEquals(CENSUS.get(tKey).intValue(), tMap.mRecipeList.size(), tKey + ": the map holds the census");
			assertEquals(CENSUS.get(tKey).intValue(), GT6RecipeMapJsonLoader.pouredCount(tKey),
					tKey + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
		}
	}

	/** The declaration shape pin: the file-head comments keep naming the dropped legs and the declared-out faces. */
	@Test
	public void theFileHeadsKeepTheirDeclarationComments() throws Exception {
		for (String tKey : DECLARATIONS.keySet()) {
			InputStream tStream = GT6RecipeMapDataB6bRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			assertTrue(tDoc.has("comment") && tDoc.get("comment").getAsString().contains(DECLARATIONS.get(tKey)),
					tKey + ": the declaration comment names " + DECLARATIONS.get(tKey));
		}
	}

	/** Spot check 1 — the CNC gear walk (Loader_Recipes_Woods.java:262): 4 planks + 1 mB lubricant -> 1 wood gear, 64 t / 16 EUt. */
	@Test
	public void cncGearWalkLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("cnc");
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Woods.java:262 — the Wood plank gear");
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals("gt6:plank_wood", tInputs.get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(4, tInputs.get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals("gt6:lubricant", tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString());
		assertEquals(1, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(), "FL.make(name, 1) — the symbolic 1 mB leg");
		assertEquals("gt6:gear_gt_wood", tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(64, tRow.get("duration").getAsLong());
		assertEquals(16, tRow.get("eut").getAsLong());
		// the walk membership: one row per plank-x-gear material — the GT6WoodDict.ROWS
		// walk (the ONE plank authority since planks-blockification retired the 130-item
		// OP.plank prefix family; every one of the 28 rows carries a gearGt item)
		assertEquals(1, gregtech6.fluid.GTFluids.LUBRICANT_FLUID_SPECS.size(), "the port LUBRICANT family is the single lubricant row");
		assertEquals(28, countRows(tRows, "Loader_Recipes_Woods.java:262"), "the LIST_PLANKS x LUBRICANT walk = the 28 WoodDict rows");
	}

	/**
	 * Spot check 2 — the assembler listener pair (Loader_Recipes_OreDict.java:237/:238):
	 * 8 planks + redstone dust -> note block at 32 t, 8 planks + diamond -> jukebox at
	 * 64 t, both 16 EUt, one row per plankWood listener member (27 x 2 — the shipped walk
	 * carries 27 of the 28 WoodDict rows, the treated-planks row is not a :237/:238
	 * listener member in the planks-blockification re-cut of task
	 * recipe-b6b-small-maps-cnc-assembler-nanofab).
	 */
	@Test
	public void assemblerListenerPairIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("assembler");
		JsonObject tNoteblock = findRow(tRows, "Loader_Recipes_OreDict.java:237 — Wood planks -> note block");
		assertEquals("gt6:plank_wood", tNoteblock.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(8, tNoteblock.getAsJsonArray("inputs").get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals("gt6:dust_redstone", tNoteblock.getAsJsonArray("inputs").get(1).getAsJsonObject().get("item").getAsString());
		assertEquals("minecraft:note_block", tNoteblock.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(32, tNoteblock.get("duration").getAsLong());
		JsonObject tJukebox = findRow(tRows, "Loader_Recipes_OreDict.java:238 — Wood planks -> jukebox");
		assertEquals("gt6:gem_diamond", tJukebox.getAsJsonArray("inputs").get(1).getAsJsonObject().get("item").getAsString());
		assertEquals("minecraft:jukebox", tJukebox.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(64, tJukebox.get("duration").getAsLong());
		assertEquals(27, countRows(tRows, "Loader_Recipes_OreDict.java:237"), "the noteblock walk = one row per plankWood listener member");
		assertEquals(27, countRows(tRows, "Loader_Recipes_OreDict.java:238"), "the jukebox walk = one row per plankWood listener member");
	}

	/**
	 * Spot check 3 — the nanofab stocks: the :818 foil head, the :873/:874 rotor tail and
	 * the Ores:343/:344 Dolamide/Dilithium gem rows with their verbatim EUt ladders.
	 */
	@Test
	public void nanofabStocksAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("nanofab");
		JsonObject tFoil = findRow(tRows, "Loader_Recipes_Other.java:818");
		assertEquals("gt6:dust_div72_carbon", tFoil.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(18, tFoil.getAsJsonArray("inputs").get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals("gt6:foil_graphene", tFoil.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(64, tFoil.get("duration").getAsLong());
		assertEquals(16, tFoil.get("eut").getAsLong());
		JsonObject tRotor = findRow(tRows, "Loader_Recipes_Other.java:874");
		assertEquals(17, tRotor.getAsJsonArray("inputs").get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals("gt6:rotor_graphene", tRotor.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(4, tRotor.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals(4352, tRotor.get("duration").getAsLong());
		JsonObject tDolamide = findRow(tRows, "Loader_Recipes_Ores.java:343");
		assertEquals("gt6:dust_dolamide", tDolamide.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals("gt6:gem_dilithium", tDolamide.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(512, tDolamide.get("eut").getAsLong());
		assertEquals(2048, tDolamide.get("duration").getAsLong());
		JsonObject tDilithium = findRow(tRows, "Loader_Recipes_Ores.java:344");
		assertEquals(4096, tDilithium.get("duration").getAsLong());
		// the shape-group accounting (the port-registered subsets; the wireGt01/pipe legs ride no row)
		assertEquals(41, countRows(tRows, "Loader_Recipes_Other.java:"), "the Other stock = 41 of the 60 upstream rows");
		assertEquals(4, countRows(tRows, "Loader_Recipes_Ores.java:"), "the Ores stock = 4 (the :341-344 quartet)");
	}

	/**
	 * The live-id face, offline: every gt6 item id the three files reference must be a member
	 * of the registration walks' id universe (GTMaterialItems + GTMaterialBlocks
	 * registrationOrder over the initialized material system — the B1 test pattern).
	 */
	@Test
	public void everyGt6IdTheSmallMapFilesReferenceIsRegistered() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials();
		Set<String> tUniverse = new HashSet<>();
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialBlocks.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		// task planks-blockification — the plank prefix items retired; the plank face is the
		// GT6WoodDict BlockItem rows (the ONE plank authority, the GT6TreeBlocks cube family),
		// so the 17 gt6 family ids join the universe (the vanilla rows are not gt6-namespace)
		for (gregtech6.registry.GT6WoodDict.PlankEntry tPlank : gregtech6.registry.GT6WoodDict.GT6_ROWS) {
			tUniverse.add("gt6:" + tPlank.id());
		}
		// the fluid face: the lubricant family is the CNC walk's only fluid carrier (its
		// registered spec names are the fluid ids, GTFluids.java:2804)
		for (gregtech6.fluid.GTFluids.ChemicalFluidSpec tSpec : gregtech6.fluid.GTFluids.LUBRICANT_FLUID_SPECS) {
			tUniverse.add("gt6:" + tSpec.name());
		}
		assertTrue(tUniverse.contains("gt6:dust_coal"), "the id universe built (" + tUniverse.size() + " ids)");

		Set<String> tMissing = new TreeSet<>();
		for (String tKey : CENSUS.keySet()) {
			InputStream tStream = GT6RecipeMapDataB6bRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement tElement : tDoc.getAsJsonArray("recipes")) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"inputs", "outputs"}) {
					if (tRow.get(tLeg) == null) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("item").getAsString();
						if (tId.startsWith("gt6:") && !tUniverse.contains(tId)) tMissing.add(tKey + ": " + tId);
					}
				}
				if (tRow.get("fluidInputs") != null) {
					for (JsonElement tSlot : tRow.getAsJsonArray("fluidInputs")) {
						String tId = tSlot.getAsJsonObject().get("fluid").getAsString();
						if (tId.startsWith("gt6:") && !tUniverse.contains(tId)) tMissing.add(tKey + ": " + tId + " (fluid)");
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "every gt6 id resolves against the registration walks; missing: " + tMissing);
	}

	/** The alias seam this card rides: the three keys are one-map-one-key (no sawing-style second key). */
	@Test
	public void theThreeKeysAreTheirOwnMaps() {
		assertSame(GT6RecipeMaps.NANOFAB, GT6RecipeMapJsonLoader.mapFor("nanofab"), "nanofab is its own map");
		assertSame(GT6RecipeMaps.ASSEMBLER, GT6RecipeMapJsonLoader.mapFor("assembler"), "assembler is its own map");
		assertSame(GT6RecipeMaps.CNC, GT6RecipeMapJsonLoader.mapFor("cnc"), "cnc is its own map");
	}
}
