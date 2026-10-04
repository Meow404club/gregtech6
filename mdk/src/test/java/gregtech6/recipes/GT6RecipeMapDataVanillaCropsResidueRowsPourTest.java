package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GT6FoodsideItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The smoke-juice-squeeze-vanilla-crops row-stock pour test (the b2 家法 over the
 * vanilla-crops residue the b4 card left declared). TRUE SOURCES:
 * <ol>
 * <li><b>Loader_Recipes_Vanilla.java :841-:856 + :874</b> — the JUICER residue band,
 * 16 rows: the 13 vanilla dye-flower rows (:841-:850 the nine red_flower metas + the
 * yellow_flower dandelion at chance 3000, :851-:853 the lilac/peony/rose_bush
 * double_plants at chance 6000; the dye-fluid face at FL.mul(DYE_FLUIDS_FLOWER, 1/2)
 * over the CS.L = 144 mB base, Loader_Fluids.java:122 — the gt6:dye_flower_ family, the
 * b4 carrier), the :855 cactus row (Juice_Cactus 75 + IL.Dye_Cactus.get(2) at chance
 * 9000; the LoaderItemList.java:757 alias = minecraft:green_dye), the :856 reed row
 * (Juice_Reed 75 + Remains_Plant 1 at chance 5000) and the :874 squid-ink row
 * (FL.make("squidink", 3*L/2) = 216 mB, NI; the LoaderItemList.java:756 IL.Dye_SquidInk
 * alias = minecraft:ink_sac). Every remains leg rides the vanilla-alias carrier
 * gt6:remains_plant (MultiItemFood.java:113, meta 12100).</li>
 * <li><b>The squeezer chanced-remains leg joins</b> — the b4 leftover "已座行的 chanced
 * remains 缺腿": 16 ALREADY-SEATED b2b2 rows gain their upstream item-output legs —
 * :802 melon + Remains_Fruit x9 (chance 6000, the gt6:remains_fruit carrier,
 * MultiItemFood.java:114), :803-:812 + Remains_Plant x1 (chance 2000), :813-:815 +
 * Remains_Plant x1 (chance 4000), :817 cactus + green_dye x2 (chance 7000), :818 reed +
 * Remains_Plant x1 (chance 4000). The seated fluid faces stay byte-untouched.</li>
 * <li><b>The SMOKE face CUT (the archaeology record)</b> — the upstream smoke domain is
 * NOT a GT6 recipe map: it is the Et Futurum Requiem SmokerRecipes injection/blacklist
 * shim (RM.java:805-821 the add_smelting aSmoker/aBlast legs, Loader_Recipes_Furnace.java
 * :47-51 the map walk + :107-128 the blacklist passes). The 1.20.1 port has no GT6
 * smoker map (the vanilla smoking recipe type is native datapack domain) and no rows of
 * that shim belong to a recipe-map JSON, so the domain is declared CUT — no new map, no
 * rows, per the card ruling (不越界新建图).</li>
 * </ol>
 *
 * <p>CENSUS (file rows = seated + this card): juicer 35+16=51, squeezer 54 (unchanged —
 * legs join seated rows, zero new rows). The upstream row order is preserved inside the
 * appended band (:841..:853, :855, :856, :874).
 *
 * <p>DECLARED OUT (pinned absent, the b2b2 declarations stand): the four unseated
 * squeezer Vanilla twins — :822/:823 (gemChipped/gemFlawed ice, unregistered), :835
 * pufferfish, :836/:837 the golden-apple potion-fluid faces, :839 the squid-ink twin
 * (its juicer sibling :874 is this card's band; the squeezer twin stays the b2b2
 * declared-out face, not a remains-band row). The two Remains siblings with no vanilla-
 * crops consumer (remains_veggie/remains_nut, MultiItemFood.java:115-116) stay absent.
 *
 * <p>Hermetic form (the hermetic-pour-tests law): the generation reset participates
 * before the init/pour and the live seams are restored after each test.
 */
public class GT6RecipeMapDataVanillaCropsResidueRowsPourTest extends GTRecipesOfflineTestBase {

	/** The per-file row census (the seated rows + this card's band). */
	private static final Map<String, Integer> CENSUS = Map.of(
			"juicer", 51, "squeezer", 54);

	/** The juicer seated census (everything before this card's 16-row band). */
	private static final int JUICER_SEATED = 35;

	/** The juicer band: input -> the upstream face, in :841..:874 line order. */
	private static final String[][] JUICER_BAND = {
			// :841-:850 — red_flower metas 0-8 + yellow_flower, chance 3000, FL.mul(x1) = 144
			{"minecraft:poppy"        , "gt6:dye_flower_red"        , "144", "gt6:remains_plant", "1", "3000"},
			{"minecraft:blue_orchid"  , "gt6:dye_flower_light_blue" , "144", "gt6:remains_plant", "1", "3000"},
			{"minecraft:allium"       , "gt6:dye_flower_magenta"    , "144", "gt6:remains_plant", "1", "3000"},
			{"minecraft:azure_bluet"  , "gt6:dye_flower_light_gray" , "144", "gt6:remains_plant", "1", "3000"},
			{"minecraft:red_tulip"    , "gt6:dye_flower_red"        , "144", "gt6:remains_plant", "1", "3000"},
			{"minecraft:orange_tulip" , "gt6:dye_flower_orange"     , "144", "gt6:remains_plant", "1", "3000"},
			{"minecraft:white_tulip"  , "gt6:dye_flower_light_gray" , "144", "gt6:remains_plant", "1", "3000"},
			{"minecraft:pink_tulip"   , "gt6:dye_flower_pink"       , "144", "gt6:remains_plant", "1", "3000"},
			{"minecraft:oxeye_daisy"  , "gt6:dye_flower_light_gray" , "144", "gt6:remains_plant", "1", "3000"},
			{"minecraft:dandelion"    , "gt6:dye_flower_yellow"     , "144", "gt6:remains_plant", "1", "3000"},
			// :851-:853 — the double_plants, chance 6000, FL.mul(x2) = 288
			{"minecraft:lilac"        , "gt6:dye_flower_magenta"    , "288", "gt6:remains_plant", "1", "6000"},
			{"minecraft:peony"        , "gt6:dye_flower_red"        , "288", "gt6:remains_plant", "1", "6000"},
			{"minecraft:rose_bush"    , "gt6:dye_flower_pink"       , "288", "gt6:remains_plant", "1", "6000"},
			// :855 cactus (chance 9000) + :856 reed (chance 5000) + :874 squid ink (NI)
			{"minecraft:cactus"       , "gt6:cactuswater"           , "75" , "minecraft:green_dye", "2", "9000"},
			{"minecraft:sugar_cane"   , "gt6:reedwater"             , "75" , "gt6:remains_plant", "1", "5000"},
			{"minecraft:ink_sac"      , "gt6:squidink"              , "216", null, null, null}};

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.reset(); // hermetic: the reset participates BEFORE the init (the pour window reopens)
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
		InputStream tStream = GT6RecipeMapDataVanillaCropsResidueRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: both files pour their full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void theTwoFilesPourTheirFullCensusesWithZeroSkips() throws Exception {
		for (Map.Entry<String, Integer> tFile : CENSUS.entrySet()) {
			pourShipped(tFile.getKey());
			RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(tFile.getKey());
			assertNotNull(tMap, tFile.getKey() + " resolves");
			assertEquals(tFile.getValue().intValue(), GT6RecipeMapJsonLoader.pouredCount(tFile.getKey()),
					tFile.getKey() + ": the tracker mirrors the file census (a smaller number = WARN-skipped rows)");
			assertEquals(tFile.getValue().intValue(), tMap.mRecipeList.size(),
					tFile.getKey() + ": the map holds the census");
		}
	}

	/** The map-key seams: juicer/squeezer key their own RMs. */
	@Test
	public void theTwoKeysResolveToTheirOwnMaps() {
		assertSame(GT6RecipeMaps.JUICER, GT6RecipeMapJsonLoader.mapFor("juicer"));
		assertSame(GT6RecipeMaps.SQUEEZER, GT6RecipeMapJsonLoader.mapFor("squeezer"));
	}

	private static void assertSame(Object aExpected, Object aActual) {
		org.junit.jupiter.api.Assertions.assertSame(aExpected, aActual);
	}

	/** The per-source breakdown: the juicer band = 13 vanilla flowers + cactus + reed + squid ink over the 35 seated. */
	@Test
	public void theJuicerBandBreaksDownBySource() throws Exception {
		JsonArray tRows = pourShipped("juicer");
		assertEquals(JUICER_SEATED + 16, tRows.size(), "juicer: seated + the 16-row residue band");
		Set<String> tBandInputs = new HashSet<>();
		for (String[] tFace : JUICER_BAND) tBandInputs.add(tFace[0]);
		int tBand = 0, tFlowers = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("inputs")) continue;
			String tIn = tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
			if (!tBandInputs.contains(tIn)) continue;
			// the array is ordered and the band rides the file tail, so the walk pins the upstream :841-:874 order
			assertEquals(JUICER_BAND[tBand][0], tIn, "the band keeps the upstream :841-:874 order at index " + tBand);
			if (tBand < 13) tFlowers++;
			tBand++;
		}
		assertEquals(16, tBand, "16 rows keyed on the band inputs (:841-:853 + :855/:856/:874)");
		assertEquals(13, tFlowers, ":841-:853 — the 13 dye-flower rows");
	}

	/** The verbatim face: every juicer band row pins its fluid/item/chance/duration/eut stack. */
	@Test
	public void theJuicerBandRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("juicer");
		for (String[] tFace : JUICER_BAND) {
			JsonObject tRow = findRow(tRows, tFace[0]);
			StringBuilder tTail = new StringBuilder();
			if (tRow.has("fluidOutputs")) {
				for (JsonElement tOut : tRow.getAsJsonArray("fluidOutputs")) {
					JsonObject tSlot = tOut.getAsJsonObject();
					tTail.append(tSlot.get("fluid").getAsString()).append(':').append(tSlot.get("amount").getAsInt()).append('>');
				}
			}
			if (tFace[3] != null) {
				for (JsonElement tOut : tRow.getAsJsonArray("outputs")) {
					JsonObject tSlot = tOut.getAsJsonObject();
					tTail.append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsInt()).append('>');
				}
			} else {
				assertFalse(tRow.has("outputs"), tFace[0] + ": the NI row stays output-less");
			}
			assertEquals(tFace[1] + ':' + tFace[2] + (tFace[3] == null ? ">" : ">" + tFace[3] + ':' + tFace[4] + ">"),
					tTail.toString(), tFace[0] + ": the output slot tail");
			assertEquals(16, tRow.get("duration").getAsLong(), tFace[0] + ": the upstream 16t");
			assertEquals(16, tRow.get("eut").getAsLong(), tFace[0] + ": the upstream 16 EUt");
			if (tFace[5] != null) {
				assertEquals(Integer.parseInt(tFace[5]),
						tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("chance").getAsInt(),
						tFace[0] + ": the upstream chanced item leg");
			}
		}
	}

	/** The squeezer leg joins: 16 seated rows carry their upstream chanced item legs, the fluid faces byte-untouched. */
	@Test
	public void theSqueezerRemainsLegsJoinTheirSeatedRows() throws Exception {
		JsonArray tRows = pourShipped("squeezer");
		assertEquals(54, tRows.size(), "squeezer: zero new rows — legs join the seated stock");
		// :802 melon — Remains_Fruit x9 at chance 6000
		JsonObject tMelon = findRowByAnchor(tRows, "Loader_Recipes_Vanilla.java:802");
		assertEquals("gt6:remains_fruit", tMelon.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(9, tMelon.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals(6000, tMelon.getAsJsonArray("outputs").get(0).getAsJsonObject().get("chance").getAsInt());
		assertEquals(2250, tMelon.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				"the seated juice leg stays untouched");
		// the flower walk :803-:815 — Remains_Plant x1, chance 2000 (red/yellow rows) / 4000 (double plants)
		for (int tLine = 803; tLine <= 815; tLine++) {
			JsonObject tRow = findRowByAnchor(tRows, "Loader_Recipes_Vanilla.java:" + tLine);
			JsonObject tSlot = tRow.getAsJsonArray("outputs").get(0).getAsJsonObject();
			assertEquals("gt6:remains_plant", tSlot.get("item").getAsString(), ":" + tLine + " remains leg");
			assertEquals(1, tSlot.get("count").getAsInt(), ":" + tLine + " remains count");
			assertEquals(tLine <= 812 ? 2000 : 4000, tSlot.get("chance").getAsInt(), ":" + tLine + " the upstream chance");
		}
		// the seated b2b2 dye carrier stays byte-untouched (the poppy spot)
		JsonObject tPoppy = findRowByAnchor(tRows, "Loader_Recipes_Vanilla.java:803");
		assertEquals("gt6:dye_chemical_red", tPoppy.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString(),
				"the b2b2-era dye_chemical carrier is NOT this card's face to re-key");
		assertEquals(288, tPoppy.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt());
		// :817 cactus — green_dye x2 at chance 7000 over the untouched 100 mB juice
		JsonObject tCactus = findRowByAnchor(tRows, "Loader_Recipes_Vanilla.java:817");
		assertEquals("minecraft:green_dye", tCactus.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(2, tCactus.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals(7000, tCactus.getAsJsonArray("outputs").get(0).getAsJsonObject().get("chance").getAsInt());
		assertEquals(100, tCactus.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt());
		// :818 reed — Remains_Plant x1 at chance 4000 over the untouched 100 mB juice
		JsonObject tReed = findRowByAnchor(tRows, "Loader_Recipes_Vanilla.java:818");
		assertEquals("gt6:remains_plant", tReed.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(4000, tReed.getAsJsonArray("outputs").get(0).getAsJsonObject().get("chance").getAsInt());
		assertEquals(100, tReed.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt());
		// :816 sunflower keeps exactly its one deterministic dye output (upstream has no remains leg there)
		JsonObject tSunflower = findRowByAnchor(tRows, "Loader_Recipes_Vanilla.java:816");
		assertEquals(1, tSunflower.getAsJsonArray("outputs").size(), ":816 — no remains leg upstream");
		// the ice/poison NI rows gained nothing
		for (String tAnchor : new String[] {"Loader_Recipes_Vanilla.java:821", "Loader_Recipes_Vanilla.java:832"}) {
			assertFalse(findRowByAnchor(tRows, tAnchor).has("outputs"), tAnchor + " stays NI");
		}
	}

	/** The declared absences: the out twins stay out and the unconsumed Remains siblings stay absent. */
	@Test
	public void theDeclaredAbsencesStayAbsent() throws Exception {
		JsonArray tJuicer = pourShipped("juicer");
		JsonArray tSqueezer = pourShipped("squeezer");
		// the two Remains siblings with no vanilla-crops consumer
		for (JsonArray tRows : new JsonArray[] {tJuicer, tSqueezer}) {
			for (String tAbsent : new String[] {"gt6:remains_veggie", "gt6:remains_nut"}) {
				for (JsonElement tElement : tRows) {
					JsonObject tRow = tElement.getAsJsonObject();
					for (String tLeg : new String[] {"inputs", "outputs"}) {
						if (!tRow.has(tLeg)) continue;
						for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
							assertFalse(tAbsent.equals(tSlot.getAsJsonObject().get("item").getAsString()),
									tAbsent + " stays absent (no upstream vanilla-crops consumer)");
						}
					}
				}
			}
		}
		// the four unseated squeezer Vanilla twins stay out (the b2b2 declarations stand)
		Set<String> tSqueezerInputs = new HashSet<>();
		for (JsonElement tElement : tSqueezer) {
			tSqueezerInputs.add(tElement.getAsJsonObject().getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString());
		}
		assertFalse(tSqueezerInputs.contains("minecraft:pufferfish"), ":835 pufferfish stays declared out");
		assertFalse(tSqueezerInputs.contains("minecraft:golden_apple"), ":836/:837 golden apples stay declared out");
		assertFalse(tSqueezerInputs.contains("minecraft:ink_sac"), ":839 squid-ink twin stays declared out (the juicer :874 is this card's band)");
	}

	/** The id faces: every gt6 ITEM id across the two files lives in the universe (materials + flowers + trees + foodside + the seated flats). */
	@Test
	public void everyItemIdTheFilesReferenceIsRegistered() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialBlocks.registrationOrder()) {
			tUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (var tFlower : gregtech6.registry.GT6SurfaceBlocks.FLOWER_ITEMS) {
			tUniverse.add("gt6:" + tFlower.getId().getPath()); // the flower band items are plain BlockItems
		}
		for (var tTree : gregtech6.registry.GT6TreeBlocks.ITEMS) {
			tUniverse.add("gt6:" + tTree.getId().getPath()); // the seated b2b2 Woods rows
		}
		for (GT6FoodsideItems.SideRow tRow : GT6FoodsideItems.ROWS) {
			tUniverse.add("gt6:" + tRow.id()); // the vanilla-alias card's remains/drop family
		}
		tUniverse.add("gt6:comb_honey"); // the seated juicer Food:266 flat item
		tUniverse.addAll(vanillaWhitelist());
		Set<String> tMissing = new HashSet<>();
		for (String tFile : CENSUS.keySet()) {
			for (JsonElement tElement : pourShipped(tFile)) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"inputs", "outputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("item").getAsString();
						if (!tUniverse.contains(tId)) tMissing.add(tFile + ": " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered item ids (these rows would WARN-skip live): " + tMissing);
	}

	/** The fluid faces: every gt6 FLUID id resolves in the GTFluids spec lookups ∪ the three dye families. */
	@Test
	public void everyFluidIdTheFilesReferenceIsRegistered() throws Exception {
		Set<String> tDye = new HashSet<>();
		for (gregtech6.fluid.GTFluids.DyeFluid tRow : gregtech6.fluid.GTFluids.DYE_WATERMIXED) tDye.add(tRow.name());
		for (gregtech6.fluid.GTFluids.DyeFluid tRow : gregtech6.fluid.GTFluids.DYE_FLOWER) tDye.add(tRow.name());
		assertEquals(32, tDye.size(), "the two compose families, 16+16");
		Set<String> tDyeChemical = new HashSet<>();
		for (int i = 0; i < 16; i++) tDyeChemical.add(gregtech6.fluid.GTFluids.dyeChemicalName(i)); // the seated b2b2 rows' family
		Set<String> tMissing = new HashSet<>();
		for (String tFile : CENSUS.keySet()) {
			for (JsonElement tElement : pourShipped(tFile)) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"fluidInputs", "fluidOutputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("fluid").getAsString();
						if (tId.startsWith("minecraft:")) continue; // the vanilla carriers resolve live
						String tPath = tId.substring("gt6:".length());
						if (tDye.contains(tPath) || tDyeChemical.contains(tPath)) continue;
						if (!fluidRegistered(tPath)) tMissing.add(tFile + ": " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 fluid ids (these rows would WARN-skip live): " + tMissing);
	}

	// ------------------------------------------------------------------ helpers

	/** The offline fluid-universe lookup: the union of the GTFluids spec-table lookups (the b4 shape). */
	private static boolean fluidRegistered(String aPath) {
		return gregtech6.fluid.GTFluids.aquaSpec(aPath) != null || gregtech6.fluid.GTFluids.engineSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.chemicalSpec(aPath) != null || gregtech6.fluid.GTFluids.simpleLiquidSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.lubricantSpec(aPath) != null || gregtech6.fluid.GTFluids.foodSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.foodB1Spec(aPath) != null || gregtech6.fluid.GTFluids.foodB2Spec(aPath) != null
				|| gregtech6.fluid.GTFluids.foodTailSpec(aPath) != null || gregtech6.fluid.GTFluids.hotSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.closureSpec(aPath) != null || gregtech6.fluid.GTFluids.honeySpec(aPath) != null
				|| gregtech6.fluid.GTFluids.beeRowSpec(aPath) != null || gregtech6.fluid.GTFluids.quSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.namingSpec(aPath) != null;
	}

	private static JsonObject findRow(JsonArray aRows, String aFirstItem) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (aFirstItem.equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())) return tRow;
		}
		throw new AssertionError("no row keyed on " + aFirstItem);
	}

	private static JsonObject findRowByAnchor(JsonArray aRows, String aAnchor) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().contains(aAnchor)) return tRow;
		}
		throw new AssertionError("no row anchored on " + aAnchor);
	}

	/** The vanilla whitelist — exactly the minecraft: ids the two files reference (the b4 list plus the band's ink sac). */
	private static Set<String> vanillaWhitelist() {
		Set<String> rSet = new HashSet<>();
		for (String tId : new String[] {
				// juicer/squeezer seated stock + the b4 Tungstus legs
				"green_dye", "yellow_dye", "sunflower", "blue_dye", "ice", "packed_ice", "snowball", "snow",
				// the seated b2b2 vanilla-flower identity band (:803-:817 the 1.20.1 mapping)
				"poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip",
				"pink_tulip", "oxeye_daisy", "dandelion", "lilac", "peony", "rose_bush", "cactus",
				"melon", "melon_slice", "golden_carrot", "golden_apple",
				"red_mushroom", "poisonous_potato", "spider_eye", "pufferfish",
				"slime_ball", "wheat_seeds", "melon_seeds", "beetroot_seeds", "pumpkin_seeds",
				// this card's band
				"sugar_cane", "ink_sac",
				// the loom-file seated stock this file family shares the walk with
				"string", "wheat_seeds"}) {
			rSet.add("minecraft:" + tId);
		}
		return rSet;
	}
}
