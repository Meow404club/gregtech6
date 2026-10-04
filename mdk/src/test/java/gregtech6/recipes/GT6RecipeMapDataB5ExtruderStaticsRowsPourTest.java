package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Task recipe-b5-extruder-statics — the Loader_Recipes_Extruder static-station
 * transcription pour test. The card appends 175 rows to the r11c {@code extruder.json}:
 * (A) 138 module-domain literals (five upstream 30-row domains minus the 12 head rows
 * r11c shipped), (B) 9 prefix-walk representatives (the LRE.java:202 filter over 37
 * qualifying port prefixes; the walk call sites whose output items the port registers),
 * (C) 28 Handlers non-head forging representatives (14 statements x 2 families). The
 * census pins the per-source decomposition, the verbatim pins replay whole rows, the
 * discarded-rows ledger is pinned at the file head, and every referenced id is checked
 * against the live registration universe (the GT6RecipeMapDataB1RowsPourTest form).
 *
 * <p><b>HERMETIC (the r11e lesson)</b>: {@code reset()} retires the generation BEFORE
 * {@code init()} builds the fresh one, every test — the neo junit FML boot statically
 * pours the GT6Recipes* suite into the boot generation and a bare {@code init()} would
 * no-op onto it.
 */
public class GT6RecipeMapDataB5ExtruderStaticsRowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The item ids the loader asked for during the pour, "namespace:path" (the id-face assertion set). */
	private final Set<String> mRequestedItems = new HashSet<>();

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.reset(); // hermetic: retire the boot generation (and its residue) FIRST
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		mRequestedItems.clear();
		GT6RecipeMapJsonLoader.sItemResolver = aId -> {
			mRequestedItems.add(aId.getNamespace() + ":" + aId.getPath());
			// identity stand-ins per namespace — the mechanics compare shapes only
			return "gt6".equals(aId.getNamespace()) ? Items.IRON_INGOT : Items.BRICK;
		};
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> Fluids.WATER;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads the shipped true-row file verbatim and pours it under its map key. */
	private JsonObject pourShipped() throws Exception {
		String tPath = "/data/gt6/recipe_maps/extruder.json";
		try (InputStream tStream = GT6RecipeMapDataB5ExtruderStaticsRowsPourTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped true-row file " + tPath + " rides the test classpath");
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", "extruder"), tDoc));
			return tDoc;
		}
	}

	/**
	 * The census: 227 rows = the r11c 52 + the b5 175, bucketed by the row comments'
	 * upstream line anchors — the five 30-row module domains (:44-73 Blackstone = the b5
	 * 18 remainder + the r11c 12 heads; :75-104 Basalt; :106-135 Stone; :137-166 stone
	 * W-meta; :168-197 cobblestone), the 9 walk representatives (:200+), the 28 Handlers
	 * representatives ({@code Loader_Recipes_Handlers.java:} prefix), and the 40 remaining
	 * r11c rows (the 12 Blackstone heads + 16 metal forging + 12 stone/BlockStones).
	 */
	@Test
	public void theAppendDecomposesBySource() throws Exception {
		JsonObject tDoc = pourShipped();
		int tBlackstoneDomain = 0, tBasalt = 0, tStoneModule = 0, tStoneW = 0, tCobble = 0, tWalk = 0, tHandlers = 0, tR11cRest = 0;
		for (var tRow : tDoc.getAsJsonArray("recipes")) {
			String tComment = tRow.getAsJsonObject().get("comment").getAsString();
			if (tComment.startsWith("Loader_Recipes_Handlers.java:")) tHandlers++;
			else if (!tComment.contains("Loader_Recipes_Extruder.java:")) tR11cRest++;
			else {
				java.util.regex.Matcher tM = java.util.regex.Pattern.compile("Extruder\\.java:(\\d+)").matcher(tComment);
				assertTrue(tM.find(), "every LRE row cites its upstream anchor: " + tComment);
				int tLine = Integer.parseInt(tM.group(1));
				if (tLine <= 73) tBlackstoneDomain++;
				else if (tLine <= 104) tBasalt++;
				else if (tLine <= 135) tStoneModule++;
				else if (tLine <= 166) tStoneW++;
				else if (tLine <= 197) tCobble++;
				else tWalk++;
			}
		}
		assertEquals(30, tBlackstoneDomain, "the Blackstone domain (:44-73 = b5 remainder 18 + r11c heads 12)");
		assertEquals(30, tBasalt, "the Basalt module domain (:75-104)");
		assertEquals(30, tStoneModule, "the Stone module domain (:106-135)");
		assertEquals(30, tStoneW, "the stone W-meta domain (:137-166)");
		assertEquals(30, tCobble, "the cobblestone domain (:168-197)");
		assertEquals(9, tWalk, "the prefix-walk representatives");
		assertEquals(28, tHandlers, "the Handlers forging representatives (14 statements x 2)");
		assertEquals(40, tR11cRest, "the r11c metal + stone rows stay untouched");
		assertEquals(227, GT6RecipeMaps.EXTRUDER.mRecipeList.size(), "the full pour lands 227 rows");
		assertEquals(227, GT6RecipeMapJsonLoader.pouredCount("extruder"));
	}

	/**
	 * The mold-slot ledger: every row carries exactly one mold in its last input slot;
	 * the 227 rows split 114 Shape_Extruder / 113 Shape_SimpleEx (the Zr walk row has no
	 * SimpleEx twin upstream); the poured stock covers 24 usable mold types per family
	 * (the ledger's 48 poured molds) while the 7 deferred types (wire/pipes x5/ccc,
	 * port-absent output items) carry NO row.
	 */
	@Test
	public void theMoldLedgerCoversTheFullFamily() throws Exception {
		JsonObject tDoc = pourShipped();
		Set<String> tShapeMolds = new HashSet<>(), tSimpleMolds = new HashSet<>();
		int tShapeRows = 0, tSimpleRows = 0;
		for (var tRow : tDoc.getAsJsonArray("recipes")) {
			var tInputs = tRow.getAsJsonObject().getAsJsonArray("inputs");
			String tSecond = tInputs.get(tInputs.size() - 1).getAsJsonObject().get("item").getAsString();
			assertTrue(tSecond.startsWith("gt6:shape_extruder_") || tSecond.startsWith("gt6:shape_simple_ex_"),
					"the last input slot is always a mold: " + tSecond);
			if (tSecond.startsWith("gt6:shape_extruder_")) { tShapeRows++; tShapeMolds.add(tSecond); }
			else { tSimpleRows++; tSimpleMolds.add(tSecond); }
		}
		assertEquals(227, tShapeRows + tSimpleRows, "every row carries exactly one mold");
		assertEquals(114, tShapeRows, "the Shape_Extruder half");
		assertEquals(113, tSimpleRows, "the Shape_SimpleEx half (the Zr walk row has no twin)");
		// the deferred types: wire, the five pipes, ccc — port-absent output items, rows deferred
		for (String tMold : new String[] {"wire", "pipe_tiny", "pipe_small", "pipe_medium", "pipe_large", "pipe_huge", "ccc"}) {
			assertTrue(!tShapeMolds.contains("gt6:shape_extruder_" + tMold), "the deferred type carries no row: shape_extruder_" + tMold);
			assertTrue(!tSimpleMolds.contains("gt6:shape_simple_ex_" + tMold), "the deferred type carries no row: shape_simple_ex_" + tMold);
		}
		// the poured types per family: the 14 stone-domain r11c + saw/file r11c metal + the
		// six Handlers additions (ingot/ring/casing/plateTiny/foil/wireFine) + bottle/cell = 24
		assertEquals(24, tShapeMolds.size(), "the Shape_Extruder poured-type census");
		assertEquals(24, tSimpleMolds.size(), "the Shape_SimpleEx poured-type census");
	}

	/**
	 * The verbatim replay pins: whole-row structural equality against the shipped file
	 * for one row per source domain (the file is the row authority; comments carry the
	 * upstream line anchors). The expected bodies are parsed JSON — equals is structural,
	 * so only the field content matters.
	 */
	@Test
	public void theVerbatimRowsReplayTheUpstreamAnchors() throws Exception {
		JsonObject tDoc = pourShipped();
		List<String> tExpected = List.of(
			// :49 — the Blackstone Ingot-mold row, the basalt-bricks fallback output
			"{\"comment\":\"Loader_Recipes_Extruder.java:49 — Blackstone Shape_Extruder_Ingot module row (module input substituted: Module_Blackstone_Generator port-absent, the r11c declared deviation)\","
			+ "\"inputs\":[{\"item\":\"minecraft:blackstone\"},{\"item\":\"gt6:shape_extruder_ingot\"}],"
			+ "\"outputs\":[{\"item\":\"gt6:basalt_bricks\"}],\"duration\":64,\"eut\":16}",
			// :141 — the W-meta bolt row (the 8-t / x8-out ladder, the MT.Stone bolt items)
			"{\"comment\":\"Loader_Recipes_Extruder.java:141 — Stone Shape_Extruder_Bolt wildcard-meta row\","
			+ "\"inputs\":[{\"item\":\"minecraft:stone\"},{\"item\":\"gt6:shape_extruder_bolt\"}],"
			+ "\"outputs\":[{\"item\":\"gt6:bolt_stone\",\"count\":8}],\"duration\":8,\"eut\":16}",
			// :182 — the cobblestone hammer row (the 192-t / x6-in ladder tail)
			"{\"comment\":\"Loader_Recipes_Extruder.java:182 — Cobblestone Shape_Extruder_Hammer wildcard-meta row\","
			+ "\"inputs\":[{\"item\":\"minecraft:cobblestone\",\"count\":6},{\"item\":\"gt6:shape_extruder_hammer\"}],"
			+ "\"outputs\":[{\"item\":\"gt6:tool_head_hammer_stone\"}],\"duration\":192,\"eut\":16}",
			// :292 — the Zr walk row (the only 96-EUt walk column, no SimpleEx twin)
			"{\"comment\":\"Loader_Recipes_Extruder.java:292 — Zr Shape_Extruder_Cell walk row, ingot_zirconium representative (duration = max(64, (2128-293)*(1+getWeight(U))/6144) = 216, the only 96-EUt walk column; NO SimpleEx twin upstream)\","
			+ "\"inputs\":[{\"item\":\"gt6:ingot_zirconium\"},{\"item\":\"gt6:shape_extruder_cell\"}],"
			+ "\"outputs\":[{\"item\":\"gt6:empty_reactor_rod\"}],\"duration\":216,\"eut\":96}",
			// Handlers:765 — the block forging row (9 in, the 1661-t melting-point column)
			"{\"comment\":\"Loader_Recipes_Handlers.java:765 — Shape_Extruder_Block forging row, Iron representative (96 EUt, getCosts melting-point column 1661t at 9 ingots in)\","
			+ "\"inputs\":[{\"item\":\"gt6:ingot_iron\",\"count\":9},{\"item\":\"gt6:shape_extruder_block\"}],"
			+ "\"outputs\":[{\"item\":\"gt6:block_solid_iron\"}],\"duration\":1661,\"eut\":96}");
		Set<JsonObject> tRows = new HashSet<>();
		for (var tRow : tDoc.getAsJsonArray("recipes")) tRows.add(tRow.getAsJsonObject());
		for (String tExpectedRow : tExpected) assertTrue(tRows.contains(JsonParser.parseString(tExpectedRow).getAsJsonObject()),
				"the verbatim anchor row is shipped: " + tExpectedRow.substring(0, 80) + "...");
	}

	/**
	 * The discarded-rows ledger: the file head declares every upstream call site the card
	 * does NOT pour (16 LRE walk sites with port-absent outputs + the 7 x 2 Handlers
	 * statements with port-absent outputs), and no poured row references a discarded id
	 * or a deferred mold. The Shape_*_Empty leads carry no consumption row anywhere —
	 * the declared TRUE NEGATIVE.
	 */
	@Test
	public void theDiscardedRowsLedgerIsDeclaredAtTheFileHead() throws Exception {
		JsonObject tDoc = pourShipped();
		String tHead = tDoc.get("comment").getAsString();
		for (String tMarker : new String[] {
				"DISCARDED-ROWS LEDGER", ":210/:211", "Pill_Empty", "FR_*", "PlasticCan", "Cell_Empty",
				"IC2_Food_Can_Empty", ":748/:781-wire", "pipeTiny/Small/Medium/Large/Huge", ":766/:799 capcellcon",
				"THE 64-MOLD LEDGER", "48 + 14 + 2 = 64", "W-meta domains"}) {
			assertTrue(tHead.contains(tMarker), "the file head declares: " + tMarker);
		}
		String tAllRows = tDoc.getAsJsonArray("recipes").toString();
		for (String tDiscarded : new String[] {"pill", "fr_wax", "fr_refractory", "fr_magic", "plastic_can",
				"cell_empty", "ic2_", "shape_extruder_wire\"", "shape_simple_ex_wire\"", "shape_extruder_pipe",
				"shape_simple_ex_pipe", "shape_extruder_ccc", "shape_simple_ex_ccc", "shape_extruder_empty\"",
				"shape_simple_ex_empty\""}) {
			assertTrue(!tAllRows.contains(tDiscarded), "no row references the discarded face " + tDiscarded);
		}
	}

	/**
	 * The live-id face, offline: every id the file references must be a member of the
	 * registration universe (GTMaterialItems + GTMaterialBlocks registrationOrder, the
	 * GTStoneBlocks variant composition, the 64-mold registry, the three fixed walk
	 * outputs; the GT6RecipeMapDataB1RowsPourTest form) and every minecraft: id rides a
	 * four-entry vanilla whitelist.
	 */
	@Test
	public void everyIdTheFileReferencesIsRegistered() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials();
		Set<String> tUniverse = new HashSet<>();
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder())
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialBlocks.registrationOrder())
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		for (gregtech6.registry.GTStoneBlocks.StoneSpec tStone : gregtech6.registry.GTStoneBlocks.STONES)
			for (gregtech6.block.stone.StoneVariant tVariant : gregtech6.block.stone.StoneVariant.VALUES)
				tUniverse.add("gt6:" + tStone.snake() + (tVariant == gregtech6.block.stone.StoneVariant.STONE ? "" : "_" + tVariant.snake));
		for (var tMold : gregtech6.registry.GT6ExtruderMolds.MOLDS)
			tUniverse.add("gt6:" + tMold.getId().getPath());
		for (String tFixed : new String[] {"gt6:bottle_empty", "gt6:food_can_empty", "gt6:empty_reactor_rod"})
			tUniverse.add(tFixed);
		assertTrue(tUniverse.contains("gt6:dust_glass"), "the universe built (" + tUniverse.size() + " ids)");

		JsonObject tDoc2 = pourShipped();
		Set<String> tVanilla = Set.of("minecraft:stone", "minecraft:cobblestone", "minecraft:blackstone", "minecraft:stone_bricks");
		Set<String> tMissing = new java.util.TreeSet<>();
		for (var tRow : tDoc2.getAsJsonArray("recipes"))
			for (var tFace : new String[] {"inputs", "outputs"})
				for (var tStack : tRow.getAsJsonObject().getAsJsonArray(tFace)) {
					String tId = tStack.getAsJsonObject().get("item").getAsString();
					if (tId.startsWith("gt6:")) {
						if (!tUniverse.contains(tId)) tMissing.add(tId);
					} else if (!tVanilla.contains(tId)) tMissing.add(tId);
				}
		assertTrue(tMissing.isEmpty(), "every referenced id is registered (missing: " + tMissing + ")");
		// and the pour resolved the ids through the stand-ins without a skip
		assertEquals(227, GT6RecipeMaps.EXTRUDER.mRecipeList.size());
	}
}
