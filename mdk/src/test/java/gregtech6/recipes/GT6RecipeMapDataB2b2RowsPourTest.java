package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

import gregtech6.fluid.GTFluids;
import gregtech6.items.GT6ReactorRods;
import gregtech6.registry.GT6BeeCombs;
import gregtech6.registry.GT6TreeBlocks;

/**
 * The recipe-data-b2b2 row-stock pour test (the GT6RecipeMapDataB2aRowsPourTest posture):
 * the three shipped recipe-map files of this card — squeezer / electrolyzer / centrifuge,
 * the "large stock family walk" back half — pour through the real
 * {@link GT6RecipeMapJsonLoader} seam with zero skips (the "灌入行数 per 图钉" census), and
 * the verbatim spot checks read the shipped row objects directly (the file IS the
 * transcription artifact; the identity stand-in resolver cannot carry gt6 id faces, so the
 * JSON face is the honest assertion surface, with the pour proving every id resolves live
 * at the seam).
 *
 * <p>The id-universe test extends the b2a walk (GTMaterialItems + GTMaterialBlocks
 * registrationOrder) with the three extra families these files reference: the tree block
 * items (rubber leaves/sapling), the bee combs and the reactor rods — plus the FLUID id
 * face, proven against the offline-readable GTFluids spec tables (chemical/closure/aqua/
 * simple-liquid/food/engine) with the standalone chlorine row.
 */
public class GT6RecipeMapDataB2b2RowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/**
	 * The per-map census of this card: file key -> expected total rows. REBASE SEAM
	 * (seat XIII): the squeezer-seed-legs card landed first on main (+4 Crops seed rows),
	 * so the squeezer file = those 4 + this card's 37 = 41 — and the two fake smoke
	 * canaries (apple/melon-slice -> stick + water, no upstream counterpart) retire with
	 * the true :802 melon row landed (the card's REPLACE posture, now fully realized).
	 * The electrolyzer/centrifuge seated smoke rows have no true-row supersedes — they
	 * stay seated, so those censuses carry +2 each. The recipe-b4 flower band tail-appends
	 * 13 more squeezer rows (BlockFlowersA :90-:97 + BlockFlowersB :94-:98).
	 */
	private static final Map<String, Integer> CENSUS = Map.of(
			"squeezer", 54,     // seeds 4 (landed cbc-5 leftover) + Vanilla :802-:839 minus :835-:837/:839 (32) + Woods (5) + the b4 BlockFlowers band (13)
			"electrolyzer", 43, // seated clay/snow smoke rows (2) + Chem :66-:71 x4 waters (24) + :300-:304 (5) + :312-:319 pour legs (10) + Vanilla :899-:900 (2)
			"centrifuge", 96);  // seated snow smoke rows (2) + Ores :43-:93/:361-:380 (36) + MTE rods (20) + Chem waters (10) + Vanilla slime (3) + Other oils (2) + combs (20)

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

	/** Reads one shipped file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6RecipeMapDataB2b2RowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: every file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void everyB2b2FilePoursItsFullCensusWithZeroSkips() throws Exception {
		for (String tKey : CENSUS.keySet()) {
			pourShipped(tKey);
			RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(tKey);
			assertNotNull(tMap, tKey + " resolves");
			assertEquals(CENSUS.get(tKey).intValue(), tMap.mRecipeList.size(), tKey + ": the map holds the census");
			assertEquals(CENSUS.get(tKey).intValue(), GT6RecipeMapJsonLoader.pouredCount(tKey),
					tKey + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
		}
	}

	/**
	 * Spot check 1 — the salt hydrolysis walk leg (Loader_Recipes_Chem.java:66): small salt
	 * dust + water x3/4 = 750 -> chlorine 125 + hydrogen 375 + oxygen 125 + small sodium
	 * hydroxide dust, 1280 t / 16 EUt; the FL.waters(1000) walk lands FOUR legs per line.
	 */
	@Test
	public void saltHydrolysisWalkLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("electrolyzer");
		assertEquals(4, countRows(tRows, "Loader_Recipes_Chem.java:66"), "the :66 small-salt line lands one row per FL.waters member (FL.java:689)");
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Chem.java:66");
		assertEquals(1280, tRow.get("duration").getAsLong());
		assertEquals(16, tRow.get("eut").getAsLong());
		assertEquals(1, tRow.getAsJsonArray("inputs").size());
		assertEquals("gt6:dust_small_salt", slotId(tRow.getAsJsonArray("inputs").get(0)), "MT.NaCl internal Salt -> dust_small_salt");
		assertEquals(750, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(), "FL.mul(tWater, 3, 4, T)");
		JsonArray tFluids = tRow.getAsJsonArray("fluidOutputs");
		assertEquals(3, tFluids.size());
		assertEquals("gt6:chlorine", slotId(tFluids.get(0)));
		assertEquals(125, tFluids.get(0).getAsJsonObject().get("amount").getAsInt(), "MT.Cl.gas(U8)");
		assertEquals("gt6:hydrogen", slotId(tFluids.get(1)));
		assertEquals(375, tFluids.get(1).getAsJsonObject().get("amount").getAsInt(), "MT.H.gas(3*U8)");
		assertEquals("gt6:oxygen", slotId(tFluids.get(2)));
		assertEquals(125, tFluids.get(2).getAsJsonObject().get("amount").getAsInt());
		assertEquals("gt6:dust_small_sodium_hydroxide", slotId(tRow.getAsJsonArray("outputs").get(0)), "OM.dust(NaOH, 3*U8) rides the U4-class ladder rung");
	}

	/**
	 * Spot check 2 — the ice squeeze middle rung (Loader_Recipes_Vanilla.java:821) and the
	 * flower dye walk size: OM.dust(MT.Ice) -> FL.Ice 1000, and the thirteen 1.7.10
	 * red/yellow/double flower metas land as thirteen modern identity-mapped rows.
	 */
	@Test
	public void iceSqueezeAndFlowerWalkAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("squeezer");
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Vanilla.java:821");
		assertEquals("gt6:dust_ice", slotId(tRow.getAsJsonArray("inputs").get(0)));
		assertEquals(1, tRow.getAsJsonArray("fluidOutputs").size());
		assertEquals("gt6:ice", slotId(tRow.getAsJsonArray("fluidOutputs").get(0)));
		assertEquals(1000, tRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt());
		int tFlowers = 0;
		for (int tLine = 803; tLine <= 815; tLine++) tFlowers += countRows(tRows, "Loader_Recipes_Vanilla.java:" + tLine);
		assertEquals(13, tFlowers, "the :803-:815 flower walk = 13 identity-mapped rows");
		JsonObject tLilac = findRow(tRows, "Loader_Recipes_Vanilla.java:813");
		assertEquals("gt6:dye_chemical_magenta", slotId(tLilac.getAsJsonArray("fluidOutputs").get(0)), "DYE_INDEX_Magenta -> the dye_chemical family");
		assertEquals(432, tLilac.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt(), "FL.mul(dye stack, 3) over the L=144 mB base");
	}

	/**
	 * Spot check 3 — the rare-earth walk (Loader_Recipes_Ores.java:72) with its chance
	 * table, and the comb row whose foreign leg takes its declared fallback
	 * (MultiItemFood.java:257, FR_Propolis_Silky -> vanilla string).
	 */
	@Test
	public void rareEarthWalkAndCombFallbackLegsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("centrifuge");
		assertEquals(1, countRows(tRows, "Loader_Recipes_Ores.java:70"), "the rare-earth walk rides once per rung");
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Ores.java:72");
		assertEquals(144, tRow.get("duration").getAsLong());
		assertEquals(6, tRow.getAsJsonArray("outputs").size());
		assertEquals("gt6:dust_tiny_neodymium", slotId(tRow.getAsJsonArray("outputs").get(0)));
		assertEquals(8, tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals(648, tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("chance").getAsInt(), "new long[] {648,...}");
		JsonObject tComb = findRow(tRows, "MultiItemFood.java:257");
		assertEquals("minecraft:string", slotId(tComb.getAsJsonArray("outputs").get(1)), "the FR_Propolis_Silky declared fallback leg");
		assertEquals(1000, tComb.getAsJsonArray("outputs").get(1).getAsJsonObject().get("chance").getAsInt());
	}

	/** The live-id face, offline: every gt6 id the three files reference must be a member of
	 * the id universe (GTMaterialItems + GTMaterialBlocks registrationOrder + the tree block
	 * items + the combs + the reactor rods; the b2a pattern with this card's extra families). */
	@Test
	public void everyGt6ItemIdTheB2b2FilesReferenceIsRegistered() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials();
		Set<String> tUniverse = new HashSet<>();
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialBlocks.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (var tItem : GT6TreeBlocks.ITEMS) tUniverse.add("gt6:" + tItem.getId().getPath());
		// the flower band items are plain BlockItems, not material-prefix ids (the b4 BlockFlowers rows)
		for (var tFlower : gregtech6.registry.GT6SurfaceBlocks.FLOWER_ITEMS) tUniverse.add("gt6:" + tFlower.getId().getPath());
		for (var tComb : GT6BeeCombs.COMBS) tUniverse.add("gt6:" + tComb.getId().getPath());
		for (GT6ReactorRods.RodRow tRod : GT6ReactorRods.ROWS) tUniverse.add("gt6:" + tRod.path());
		// the vanilla-alias foodside family (the smoke-card remains legs on the squeezer rows)
		for (gregtech6.registry.GT6FoodsideItems.SideRow tRow : gregtech6.registry.GT6FoodsideItems.ROWS) {
			tUniverse.add("gt6:" + tRow.id());
		}
		assertTrue(tUniverse.size() > 50000, "the id universe built (" + tUniverse.size() + " ids)");

		Set<String> tMissing = new java.util.TreeSet<>();
		for (String tKey : CENSUS.keySet()) {
			InputStream tStream = GT6RecipeMapDataB2b2RowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement tElement : tDoc.getAsJsonArray("recipes")) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"inputs", "outputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("item").getAsString();
						if (tId.startsWith("gt6:") && !tUniverse.contains(tId)) tMissing.add(tKey + ": " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 item ids (these rows would WARN-skip live): " + tMissing);
	}

	/** The FLUID id face, offline: every gt6 fluid id must live in one of the GTFluids spec
	 * tables (chemical/closure/aqua/simple-liquid/food/engine) or the standalone chlorine
	 * row — the fluid half of the id universe (vanilla ids skip, they resolve live). */
	@Test
	public void everyGt6FluidIdTheB2b2FilesReferenceIsRegistered() throws Exception {
		Set<String> tMissing = new java.util.TreeSet<>();
		for (String tKey : CENSUS.keySet()) {
			InputStream tStream = GT6RecipeMapDataB2b2RowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement tElement : tDoc.getAsJsonArray("recipes")) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"fluidInputs", "fluidOutputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("fluid").getAsString();
						if (!tId.startsWith("gt6:")) continue;
						String tPath = tId.substring(4);
						if (fluidRegistered(tPath)) continue;
						tMissing.add(tKey + ": " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 fluid ids (these rows would WARN-skip live): " + tMissing);
	}

	/** The offline fluid-universe lookup: the union of ALL the GTFluids spec-table lookups
	 * (chemical/closure/hot/lubricant/honey/bee/qu/naming/aqua/simple-liquid/food families)
	 * plus the standalone chlorine row (R3 ruling). */
	private static boolean fluidRegistered(String aPath) {
		if (aPath.startsWith("dye_watermixed_") || aPath.startsWith("dye_flower_")) return true; // the GTFluids DyeFluid compose walk (the b4 juice rows)
		return GTFluids.chemicalSpec(aPath) != null || GTFluids.closureSpec(aPath) != null || GTFluids.hotSpec(aPath) != null
				|| GTFluids.lubricantSpec(aPath) != null || GTFluids.honeySpec(aPath) != null || GTFluids.beeRowSpec(aPath) != null
				|| GTFluids.quSpec(aPath) != null || GTFluids.namingSpec(aPath) != null
				|| GTFluids.aquaSpec(aPath) != null || GTFluids.simpleLiquidSpec(aPath) != null
				|| GTFluids.foodSpec(aPath) != null || GTFluids.foodB1Spec(aPath) != null || GTFluids.foodB2Spec(aPath) != null
				|| GTFluids.foodTailSpec(aPath) != null || GTFluids.engineSpec(aPath) != null
				|| GTFluids.dyeIndexOf(aPath) >= 0 || aPath.equals("chlorine");
	}

	// ------------------------------------------------------------------ helpers

	private static int countRows(JsonArray aRows, String aCommentPrefix) {
		int rCount = 0;
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().contains(aCommentPrefix)) rCount++;
		}
		return rCount;
	}

	/** Finds the first row whose comment cites the given source line. */
	private static JsonObject findRow(JsonArray aRows, String aCommentPrefix) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().contains(aCommentPrefix)) return tRow;
		}
		throw new AssertionError("no row cited " + aCommentPrefix);
	}

	private static String slotId(JsonElement aSlot) {
		JsonObject tSlot = aSlot.getAsJsonObject();
		return tSlot.has("item") ? tSlot.get("item").getAsString() : tSlot.get("fluid").getAsString();
	}
}
