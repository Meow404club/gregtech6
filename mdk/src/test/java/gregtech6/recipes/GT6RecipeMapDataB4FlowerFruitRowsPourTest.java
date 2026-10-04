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

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GT6SurfaceBlocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The recipe-b4-juicer-squeezer-flowerfruit row-stock pour test (the b2 家法 over the
 * flower/fruit band). TRUE SOURCES (the card v2 revision — the old ":803+ 34 stations"
 * vanilla reading is b2's consumed ground):
 * <ol>
 * <li><b>BlockFlowersA/B.run()</b> — the 26 rows INSIDE the block classes (A squeezer
 * :90-:97 + juicer :99-:106, 8 metas 0-7 each; B squeezer :94-:98 + juicer :100-:104,
 * metas 2/3/4/5/7) — 13 rows per map, the dye-fluid face at CS.L = 144 mB
 * (Loader_Fluids.java :121/:122 FL.make(..., L)) plus the Tungstus pair outputting
 * IL.Dye_Cactus = minecraft:green_dye (LoaderItemList.java:757, the vanilla-alias handle)
 * over FL.Juice_Cactus = gt6:cactuswater at the verbatim 100/75 asymmetry.</li>
 * <li><b>The dye-axis unlock faces</b> — Other:672-675 the Drying 32 (16 watermixed +
 * 16 flower, FL.mul 1/6 of 144 = 24 mB in, DistW 20/10 out, dustTiny dye out), a NEW
 * drying.json (the "drying" POURABLE key predates this card, w2-energy-types-5tier);
 * Vanilla:719 i=1..15 the dyed-wool Shredder legs (chance 9000, plantGtFiber x4) riding
 * the seated white row's carrier GT6RecipesCrops; Vanilla:736 the 16 Loom fiber→wool
 * rows (loom.json, the b2-residual seat; the ST.tag(0) selector slot dropped per the
 * loom.json declared deviation); the Crops :975-:990 fiber→string GENERIFIER axis is
 * ALREADY SEATED in generifier.json (the b2c-generify static replay, live since the
 * dye-item-axis items landed — pinned here as seated-elsewhere, not re-poured).</li>
 * </ol>
 *
 * <p>CENSUS (file rows = seated + this card): juicer 22+13=35, squeezer 41+13=54,
 * drying 0+32=32, loom 35+16=51. The upstream row order is preserved inside each band
 * (A block rows then B block rows; the i=0..15 loop order).
 *
 * <p>DECLARED OUT (pinned absent): the juicer/squeezer Vanilla residue (:841-:853 the
 * vanilla red/yellow/double-plant dye rows, :855 cactus, :856 reed, :874 squid ink and
 * their squeezer siblings — smoke-juice-squeeze-vanilla-crops territory), the B-block
 * metas without run() rows (sagebrush/four_wing_saltbush/pandanus; hexalily/vindicator),
 * the :743 Cu-fiber orange-wool joke row and the :720 CR.shaped crafting face.
 *
 * <p>Hermetic form (the hermetic-pour-tests law): the generation reset participates
 * before the init/pour and the live seams are restored after each test.
 */
public class GT6RecipeMapDataB4FlowerFruitRowsPourTest extends GTRecipesOfflineTestBase {

	/** The per-file row census (seated rows + the b4 band). */
	private static final Map<String, Integer> CENSUS = Map.of(
			"juicer", 35, "squeezer", 54, "drying", 32, "loom", 51);

	/** The seated-file censuses (the b2/b2b2/b2-residual stock this card tail-appends onto). */
	private static final Map<String, Integer> SEATED = Map.of(
			"juicer", 22, "squeezer", 41, "drying", 0, "loom", 35);

	/** The A-block run() inputs, metas 0-7 in upstream line order (A squeezer :90-:97). */
	private static final String[] FLOWERS_A = {"gt6:flower_altered_andesite_buckwheat", "gt6:flower_crosby_buckwheat",
			"gt6:flower_alpine_catchfly", "gt6:flower_viola_calaminaria", "gt6:flower_thlaspi_lereschianum",
			"gt6:flower_tufted_evening_primrose", "gt6:flower_narcissus_sheldonia", "gt6:flower_orechid"};

	/** The B-block run() inputs in upstream line order (B squeezer :94-:98, metas 2/3/4/5/7). */
	private static final String[] FLOWERS_B = {"gt6:flower_desert_trumpet", "gt6:flower_copper_plant",
			"gt6:flower_princes_plume", "gt6:flower_thompsons_locoweed", "gt6:flower_tungstus"};

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
		InputStream tStream = GT6RecipeMapDataB4FlowerFruitRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: every file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void theFourFilesPourTheirFullCensusesWithZeroSkips() throws Exception {
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

	/** The map-key seams: each file keys its own RM. */
	@Test
	public void theFourKeysResolveToTheirOwnMaps() {
		assertSame(GT6RecipeMaps.JUICER, GT6RecipeMapJsonLoader.mapFor("juicer"));
		assertSame(GT6RecipeMaps.SQUEEZER, GT6RecipeMapJsonLoader.mapFor("squeezer"));
		assertSame(GT6RecipeMaps.DRYING, GT6RecipeMapJsonLoader.mapFor("drying"));
		assertSame(GT6RecipeMaps.LOOM, GT6RecipeMapJsonLoader.mapFor("loom"));
	}

	private static void assertSame(Object aExpected, Object aActual) {
		org.junit.jupiter.api.Assertions.assertSame(aExpected, aActual);
	}

	/** The per-source breakdown: 13 flower rows per juice map (8 A + 5 B), 32 = 16+16 drying, 16 loom dye rows. */
	@Test
	public void theBandBreaksDownBySource() throws Exception {
		for (String tKey : new String[] {"juicer", "squeezer"}) {
			JsonArray tRows = pourShipped(tKey);
			assertEquals(SEATED.get(tKey) + 13, tRows.size(), tKey + ": seated + the 26-row half");
			assertEquals(13, countByFlowerInput(tRows), tKey + ": 13 flower rows");
			// walk the 13 in order: A block :90-:97/:99-:106 then B block :94-:98/:100-:104
			int tIndex = 0;
			for (JsonElement tElement : tRows) {
				JsonObject tRow = tElement.getAsJsonObject();
				if (!tRow.has("inputs")) continue;
				String tIn = tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
				if (!tIn.startsWith("gt6:flower_")) continue;
				String tExpected = tIndex < 8 ? FLOWERS_A[tIndex] : FLOWERS_B[tIndex - 8];
				assertEquals(tExpected, tIn, tKey + ": the flower band keeps the upstream run() order at band index " + tIndex);
				tIndex++;
			}
			assertEquals(13, tIndex, tKey + ": the walk covered the whole band");
		}
		JsonArray tDrying = pourShipped("drying");
		assertEquals(16, countByFluidFamily(tDrying, "gt6:dye_watermixed_"), "Other:673 — the 16 watermixed rows");
		assertEquals(16, countByFluidFamily(tDrying, "gt6:dye_flower_"), "Other:674 — the 16 flower rows");
		JsonArray tLoom = pourShipped("loom");
		assertEquals(16, countByFiberInput(tLoom), "Vanilla:736 — the 16 dyed fiber rows over the 35 seated");
	}

	/** The verbatim face: the A-band dye-fluid/dust pairs at 144 mB, eut 16 dur 16. */
	@Test
	public void theFlowerJuiceRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("juicer");
		// A meta 0/1: DYE_FLUIDS_FLOWER[DYE_INDEX_Yellow = 11] + OM.dust(MT.Wheat) (BlockFlowersA.java:99/:100)
		assertRow(findRow(tRows, "gt6:flower_altered_andesite_buckwheat"), "gt6:dye_flower_yellow:144>gt6:dust_wheat:1", 16, 16);
		assertRow(findRow(tRows, "gt6:flower_crosby_buckwheat"), "gt6:dye_flower_yellow:144>gt6:dust_wheat:1", 16, 16);
		// A meta 2: Magenta (13); meta 6: LightBlue (12); meta 7: Brown (3)
		assertRow(findRow(tRows, "gt6:flower_alpine_catchfly"), "gt6:dye_flower_magenta:144>gt6:dust_magenta:1", 16, 16);
		assertRow(findRow(tRows, "gt6:flower_narcissus_sheldonia"), "gt6:dye_flower_light_blue:144>gt6:dust_light_blue:1", 16, 16);
		assertRow(findRow(tRows, "gt6:flower_orechid"), "gt6:dye_flower_brown:144>gt6:dust_brown:1", 16, 16);
		// B meta 7 — the Tungstus pair: FL.Juice_Cactus.make(75) + IL.Dye_Cactus.get(2) (:104)
		assertRow(findRow(tRows, "gt6:flower_tungstus"), "gt6:cactuswater:75>minecraft:green_dye:2", 16, 16);
	}

	/** The squeezer face: the same band with the Tungstus 100 mB asymmetry (:98 vs the juicer :104 75). */
	@Test
	public void theFlowerSqueezerRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("squeezer");
		assertRow(findRow(tRows, "gt6:flower_altered_andesite_buckwheat"), "gt6:dye_flower_yellow:144>gt6:dust_wheat:1", 16, 16);
		assertRow(findRow(tRows, "gt6:flower_tufted_evening_primrose"), "gt6:dye_flower_white:144>gt6:dust_white:1", 16, 16);
		assertRow(findRow(tRows, "gt6:flower_thompsons_locoweed"), "gt6:dye_flower_purple:144>gt6:dust_purple:1", 16, 16);
		assertRow(findRow(tRows, "gt6:flower_tungstus"), "gt6:cactuswater:100>minecraft:green_dye:2", 16, 16);
	}

	/** The drying dye band: Other:673 (watermixed, 24 in / DistW 20 / dustTiny, dur 40) and :674 (flower, DistW 10, dur 20). */
	@Test
	public void theDryingDyeRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("drying");
		assertEquals(32, tRows.size(), "the 16-loop x the two family rows, no other stock");
		// i = 0 (Black) endpoints
		JsonObject tWater = findRowByFluidIn(tRows, "gt6:dye_watermixed_black");
		assertRow(tWater, "gt6:distilled_water:20>gt6:dust_tiny_black:1", 40, 16);
		assertEquals(24, tWater.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				"FL.mul(DYE_FLUIDS_WATER[i], 1, 6, T) = 144/6 = 24");
		JsonObject tFlower = findRowByFluidIn(tRows, "gt6:dye_flower_black");
		assertRow(tFlower, "gt6:distilled_water:10>gt6:dust_tiny_black:1", 20, 16);
		// i = 15 (White) endpoints — the Dye_Materials vanilla order 0=Black..15=White
		assertRow(findRowByFluidIn(tRows, "gt6:dye_watermixed_white"), "gt6:distilled_water:20>gt6:dust_tiny_white:1", 40, 16);
		assertRow(findRowByFluidIn(tRows, "gt6:dye_flower_white"), "gt6:distilled_water:10>gt6:dust_tiny_white:1", 20, 16);
		// no item inputs — the addRecipe0 fluid-only shape
		for (JsonElement tElement : tRows) {
			assertTrue(!tElement.getAsJsonObject().has("inputs")
					|| tElement.getAsJsonObject().getAsJsonArray("inputs").isEmpty(),
					"drying dye rows are addRecipe0 fluid-only");
		}
	}

	/** The loom dyed band: :736 fiber x4 -> wool, the Dye_Materials[15-i] index mapping over all 16 metas. */
	@Test
	public void theLoomDyedRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("loom");
		assertRow(findRow(tRows, "gt6:plant_gt_fiber_white", "minecraft:white_wool"), "minecraft:white_wool:1", 16, 16);
		assertRow(findRow(tRows, "gt6:plant_gt_fiber_orange", "minecraft:orange_wool"), "minecraft:orange_wool:1", 16, 16);
		assertRow(findRow(tRows, "gt6:plant_gt_fiber_light_gray", "minecraft:light_gray_wool"), "minecraft:light_gray_wool:1", 16, 16);
		assertRow(findRow(tRows, "gt6:plant_gt_fiber_black", "minecraft:black_wool"), "minecraft:black_wool:1", 16, 16);
		// every row carries exactly the fiber x4 input leg
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("inputs")) continue;
			String tIn = tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
			if (!tIn.startsWith("gt6:plant_gt_fiber_")) continue;
			assertEquals(4, tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("count").getAsInt(),
					tIn + ": plantGtFiber.mat(..., 4) verbatim");
		}
	}

	/** The crops dyed shred legs (:719 i=1..15): chance 9000, fiber x4, the white row stays the string special case. */
	@Test
	public void theCropsDyedShredLegsAreUpstreamVerbatim() {
		GTMaterialItems.initMaterials();
		assertNotNull(MT.DATA.Dye_Materials, "the dye-item-axis array is live");
		Set<String> tDyeNames = new HashSet<>();
		for (OreDictMaterial tMat : MT.DATA.Dye_Materials) tDyeNames.add(tMat.mNameInternal);
		assertEquals(16, tDyeNames.size(), "the 16 dye materials in the vanilla order");
		GT6RecipesCrops.Row tOrange = GT6RecipesCrops.table().stream()
				.filter(aRow -> aRow.note().contains(":719 dyed") && aRow.note().contains("i=1 ")).findFirst().get();
		assertEquals("minecraft:orange_wool", tOrange.inputs()[0].id(), "wool meta 1");
		assertEquals("gt6:plant_gt_fiber_orange", GTMaterialItems.itemIdOf(tOrange.outputs()[0].prefix(), tOrange.outputs()[0].material()),
				"Dye_Materials[15-1] = Orange");
		assertEquals(4, tOrange.outputs()[0].count(), "fiber x4");
		assertEquals(9000, tOrange.chances()[0], "the :719 9000 chance");
		assertEquals(16, GT6RecipesCrops.table().stream().filter(aRow -> aRow.note().contains(":719")).count(),
				"the seated white row + the 15 dyed legs = the full upstream loop");
		// the white special case stays: white wool -> 4 string, NOT a fiber row
		assertTrue(GT6RecipesCrops.table().stream().filter(aRow -> aRow.note().contains(":719") && !aRow.note().contains("dyed"))
				.allMatch(aRow -> aRow.outputs()[0].id() != null && aRow.outputs()[0].id().equals("minecraft:string")),
				"the i==0 row keeps the Items.string special case");
	}

	/** The declared absences: the smoke-card Vanilla residue and the out-of-run() metas stay out of these files. */
	@Test
	public void theDeclaredAbsencesStayAbsent() throws Exception {
		for (String tKey : new String[] {"juicer", "squeezer"}) {
			JsonArray tRows = pourShipped(tKey);
			for (String tAbsent : new String[] {"minecraft:poppy", "minecraft:dandelion", "minecraft:cactus",
					"minecraft:sugar_cane", "gt6:remains_plant", "gt6:flower_sagebrush", "gt6:flower_four_wing_saltbush",
					"gt6:flower_pandanus_candelabrum", "gt6:flower_hexalily", "gt6:flower_vindicator_flower"}) {
				for (JsonElement tElement : tRows) {
					JsonObject tRow = tElement.getAsJsonObject();
					for (String tLeg : new String[] {"inputs", "outputs"}) {
						if (!tRow.has(tLeg)) continue;
						for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
							assertTrue(!tAbsent.equals(tSlot.getAsJsonObject().get("item").getAsString()),
									tKey + ": " + tAbsent + " stays absent (the smoke-card/blocked face)");
						}
					}
				}
			}
		}
		// the :743 Cu-fiber joke row and the :720 crafting face stay declared out of loom.json
		JsonArray tLoom = pourShipped("loom");
		for (JsonElement tElement : tLoom) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("inputs")) continue;
			for (JsonElement tSlot : tRow.getAsJsonArray("inputs")) {
				assertTrue(!"gt6:plant_gt_fiber_copper".equals(tSlot.getAsJsonObject().get("item").getAsString()),
						"the :743 Cu joke row stays declared out");
			}
		}
	}

	/** The id faces: every gt6 ITEM id across the four files lives in GTMaterialItems ∪ GTMaterialBlocks ∪ the flower band ∪ the seated flats. */
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
		for (net.minecraftforge.registries.RegistryObject<Item> tFlower : GT6SurfaceBlocks.FLOWER_ITEMS) {
			tUniverse.add("gt6:" + tFlower.getId().getPath()); // the flower band items are plain BlockItems, not material-prefix ids
		}
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
						if (tDye.contains(tPath)) continue;
						if (!fluidRegistered(tPath)) tMissing.add(tFile + ": " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 fluid ids (these rows would WARN-skip live): " + tMissing);
	}

	// ------------------------------------------------------------------ helpers

	private static int countByFlowerInput(JsonArray aRows) {
		int rCount = 0;
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("inputs") && tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString().startsWith("gt6:flower_")) rCount++;
		}
		return rCount;
	}

	private static int countByFluidFamily(JsonArray aRows, String aPrefix) {
		int rCount = 0;
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("fluidInputs") && tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString().startsWith(aPrefix)) rCount++;
		}
		return rCount;
	}

	private static int countByFiberInput(JsonArray aRows) {
		int rCount = 0;
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("inputs") && tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString().startsWith("gt6:plant_gt_fiber_")) rCount++;
		}
		return rCount;
	}

	/** The offline fluid-universe lookup: the union of the GTFluids spec-table lookups (the b2-residual shape). */
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

	private static JsonObject findRow(JsonArray aRows, String aFirstItem, String aOutput) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!aFirstItem.equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())) continue;
			if (!tRow.has("outputs")) continue;
			for (JsonElement tOut : tRow.getAsJsonArray("outputs")) {
				if (aOutput.equals(tOut.getAsJsonObject().get("item").getAsString())) return tRow;
			}
		}
		throw new AssertionError("no row " + aFirstItem + " -> " + aOutput);
	}

	private static JsonObject findRowByFluidIn(JsonArray aRows, String aFluid) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("fluidInputs") && aFluid.equals(tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString())) return tRow;
		}
		throw new AssertionError("no row keyed on fluid " + aFluid);
	}

	/** Full-row pin: the output slot tail (">id:count" style segments), duration, eut. */
	private static void assertRow(JsonObject aRow, String aTail, long aDuration, long aEut) {
		StringBuilder tTail = new StringBuilder();
		if (aRow.has("fluidOutputs")) {
			for (JsonElement tOut : aRow.getAsJsonArray("fluidOutputs")) {
				JsonObject tSlot = tOut.getAsJsonObject();
				tTail.append(tSlot.get("fluid").getAsString()).append(':').append(tSlot.get("amount").getAsInt()).append('>');
			}
		}
		if (aRow.has("outputs")) {
			for (JsonElement tOut : aRow.getAsJsonArray("outputs")) {
				JsonObject tSlot = tOut.getAsJsonObject();
				tTail.append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsInt()).append('>');
			}
		}
		assertEquals(aTail + ">", tTail.toString(), "the output slot tail");
		assertEquals(aDuration, aRow.get("duration").getAsLong(), "the duration");
		assertEquals(aEut, aRow.get("eut").getAsLong(), "the eut");
	}

	/** The vanilla whitelist — exactly the minecraft: ids the four files reference. */
	private static Set<String> vanillaWhitelist() {
		Set<String> rSet = new HashSet<>();
		for (String tId : new String[] {
				// juicer/squeezer seated stock + the b4 Tungstus legs
				"green_dye", "sunflower", "blue_dye", "ice", "packed_ice", "snowball", "snow",
				"red_mushroom", "poisonous_potato", "spider_eye", "pufferfish",
				"slime_ball", "wheat_seeds", "melon_seeds", "beetroot_seeds", "pumpkin_seeds",
				// loom seated stock + the b4 dyed band
				"white_wool", "orange_wool", "magenta_wool", "light_blue_wool", "yellow_wool", "lime_wool",
				"pink_wool", "gray_wool", "light_gray_wool", "cyan_wool", "purple_wool", "blue_wool",
				"brown_wool", "green_wool", "red_wool", "black_wool",
				"leather", "iron_horse_armor", "saddle", "string",
				"chainmail_helmet", "chainmail_chestplate", "chainmail_leggings", "chainmail_boots",
				"bookshelf", "book", "cobweb", "sugar_cane",
				"golden_horse_armor", "diamond_horse_armor",
				"leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots",
				"quartz", "glowstone_dust", "water", "prismarine_crystals",
				"clay_ball", "snow_block", "map", "paper", "compass",
				"chest_minecart", "furnace_minecart", "hopper_minecart", "tnt_minecart", "minecart",
				"chest", "furnace", "hopper", "tnt", "stick",
				"iron_sword", "golden_sword", "diamond_sword",
				"iron_pickaxe", "golden_pickaxe", "diamond_pickaxe",
				"iron_shovel", "golden_shovel", "diamond_shovel",
				"iron_axe", "golden_axe", "diamond_axe",
				"iron_hoe", "golden_hoe", "diamond_hoe"}) {
			rSet.add("minecraft:" + tId);
		}
		return rSet;
	}
}
