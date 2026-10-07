package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import gregtech6.registry.GT6CircuitChain;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The recipe-b2-bath-potion-domain row-stock pour test (the b2b1/food-t2 posture): the
 * three card files — {@code bath.json} (578 = the w1 smoke row + the Ores acid-wash walks
 * + the Chem/Other/Vanilla pourable faces + the 244-row dye band the b3 card rode in), {@code distillery.json} (1499 = the 21-row
 * food face preserved + the Loader_Recipes_Potions :35-:323 replay) and the new second
 * MIXER file key {@code mixerpotions.json} (30 = the :325-:354 Dragon_Breath face) — pour
 * through the real {@link GT6RecipeMapJsonLoader} seam with zero skips (which also proves
 * every VANILLA id resolves offline — the gt6 legs ride the stand-in resolver, so their
 * truth is the separate id-universe pin below). The file heads keep naming the declared-out
 * bands, the verbatim spot checks read the shipped rows directly (fluid identity cannot
 * survive the stand-in seam), and the walk faces pin their expansion counts (the 7-material
 * Glowstone x 3-form x 52-row walk, the 53-row Redstone and 31-row Gunpowder walks, the
 * 53-row fermented transmutation, the 12-member MnO2-byproduct wash and the 13-material
 * Cryotheum gem walk). The PhaseGate SNAPSHOT stays untouched: the JSON smoke stock is
 * pinned per-file by this suite (the declared snapshot scope), and the wood-oil ladder the
 * Java side pours live (GT6RecipesBath) is NOT re-poured here — only-add-never-duplicate.
 */
public class GT6RecipeMapDataB2RowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The per-file census: file key -> expected total rows (smoke + pour, zero skips). */
	private static final Map<String, Integer> CENSUS = Map.of(
			"bath", 585,         // 1 w1 smoke + 225 Ores + 25 Chem + 1 Other + 326 Vanilla (82 b2 + the 244 b3 dye band) + the 7 Sn soldering legs (task circuit-chain-recipes — the review-seat ratchet)
			"distillery", 1499,  // 21 food (preserved, task food-recipes-t2) + 1478 potion replay
			"mixerpotions", 30); // the :325-:354 Dragon_Breath face, 13+7+10 rows

	/** The per-source composition the census must decompose into (the pour receipt). */
	private static final Pattern SRC = Pattern.compile("(?:Loader_Recipes_)?([A-Za-z]+)\\.java:(\\d+)");

	@BeforeAll
	static void bootMaterials() {
		GTMaterialItems.initMaterials(); // the id-universe face (the b1 dump convention)
	}

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = aId ->
				"gt6".equals(aId.getNamespace()) ? Items.IRON_INGOT : sDefaultItems.apply(aId); // vanilla ids live-resolve
		GT6RecipeMapJsonLoader.sFluidResolver = aId ->
				"gt6".equals(aId.getNamespace()) ? Fluids.WATER : sDefaultFluids.apply(aId);
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads one shipped file, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6RecipeMapDataB2RowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: every file pours its full row count — no WARN-skipped rows, no truncation. */
	@Test
	public void everyB2FilePoursItsFullCensusWithZeroSkips() throws Exception {
		for (String tKey : CENSUS.keySet()) {
			pourShipped(tKey);
			assertEquals(CENSUS.get(tKey).intValue(), GT6RecipeMapJsonLoader.pouredCount(tKey),
					tKey + ": the tracker mirrors the census (a smaller number = WARN-skipped rows)");
		}
		assertEquals(CENSUS.get("bath").intValue(), GT6RecipeMaps.BATH.mRecipeList.size(),
				"bath: the map holds exactly the card rows (the Java wood-oil ladder pours only on a registry-bound JVM or under fixtures)");
		assertEquals(CENSUS.get("mixerpotions").intValue(), GT6RecipeMapJsonLoader.pouredCount("mixerpotions"),
				"mixerpotions: the second MIXER file key pours");
	}

	/** The per-source decomposition of both big files (the ratchet receipt, source file -> rows). */
	@Test
	public void theFilesDecomposeIntoTheirUpstreamSources() throws Exception {
		Map<String, Integer> tBath = countSources(pourShipped("bath"));
		assertEquals(225, tBath.get("Ores"), "bath: the Ores acid-walk face");
		assertEquals(25, tBath.get("Chem"), "bath: the Chem mineral faces");
		assertEquals(1, tBath.get("Other"), "bath: the :231 coal hydration");
		assertEquals(326, tBath.get("Vanilla"), "bath: the 30 bleach + 48 W-expanded stain rows + 4 reeds legs (b2) + the 244 :727-:735 dye band (b3)");
		assertEquals(1, tBath.get("(smoke)"), "bath: the preserved w1 smoke row (only-add-never-duplicate)");

		Map<String, Integer> tDist = countSources(pourShipped("distillery"));
		assertEquals(21, tDist.get("Food"), "distillery: the food face preserved untouched");
		assertEquals(1478, tDist.get("Potions"), "distillery: the :35-:323 potion replay");

		Map<String, Integer> tMix = countSources(pourShipped("mixerpotions"));
		assertEquals(30, tMix.get("Potions"), "mixerpotions: the :325-:354 face");
	}

	private static Map<String, Integer> countSources(JsonArray aRows) {
		Map<String, Integer> rMap = new java.util.HashMap<>();
		for (JsonElement tElement : aRows) {
			JsonElement tCommentElement = tElement.getAsJsonObject().get("comment");
			String tComment = tCommentElement == null ? "" : tCommentElement.getAsString();
			java.util.regex.Matcher tMatcher = SRC.matcher(tComment);
			String tKey = tMatcher.find() ? tMatcher.group(1) : "(smoke)";
			rMap.merge(tKey, 1, Integer::sum);
		}
		return rMap;
	}

	/** The walk expansion pins: the loop bodies of Loader_Recipes_Potions expanded over the port universe. */
	@Test
	public void thePotionWalksExpandToTheirUniverseCounts() throws Exception {
		JsonArray tRows = pourShipped("distillery");
		assertEquals(1092, countLineRange(tRows, 183, 234), "the Glowstone walk: 7 materials x 3 dust forms x 52 rows");
		assertEquals(159, countLineRange(tRows, 237, 289), "the Redstone walk: 3 forms x 53 rows");
		assertEquals(93, countLineRange(tRows, 292, 322), "the Gunpowder walk: 3 forms x 31 rows");
		assertEquals(53, countLineRange(tRows, 110, 162), "the fermented_spider_eye transmutation: :110-:162");
	}

	/** The bath walk pins: the MnO2-byproduct wash and the Cryotheum gem walk over the live families. */
	@Test
	public void theBathWalksExpandToTheirUniverseCounts() throws Exception {
		JsonArray tRows = pourShipped("bath");
		int tMnO2 = 0, tCryo = 0, tVitriolTiny = 0;
		for (JsonElement tElement : tRows) {
			JsonElement tCommentElement = tElement.getAsJsonObject().get("comment");
			String tComment = tCommentElement == null ? "" : tCommentElement.getAsString();
			if (tComment.contains("[MnO2-walk ")) tMnO2++;
			if (tComment.contains("[Cryotheum ")) tCryo++;
			if (tComment.contains(" tiny]") && tComment.contains("Vitriol")) tVitriolTiny++;
		}
		assertEquals(24, tMnO2, "the MnO2 walk: 12 live byproduct materials x 2 forms");
		assertEquals(39, tCryo, "the Cryotheum walk: 2 Amber + 7 Glowstone + 4 fixed x 3 rows");
		assertEquals(53, tVitriolTiny, "the vitriol tiny legs: (4+5+6+3+16+9+3+5+2) vitriol materials x 1 tiny row each");
	}

	private static int countLineRange(JsonArray aRows, int aFrom, int aTo) {
		int rCount = 0;
		for (JsonElement tElement : aRows) {
			java.util.regex.Matcher tMatcher = Pattern.compile("Potions\\.java:(\\d+)").matcher(tElement.getAsJsonObject().get("comment").getAsString());
			if (tMatcher.find()) {
				int tLine = Integer.parseInt(tMatcher.group(1));
				if (aFrom <= tLine && tLine <= aTo) rCount++;
			}
		}
		return rCount;
	}

	/** The verbatim spot checks (the acceptance's field-level抽验), reading the SHIPPED rows. */
	@Test
	public void distilleryPotionRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("distillery");
		// :35 — the nether wart opener (DistW 750 -> Awkward 750, dur 48, eut 16)
		JsonObject tRow = byLine(tRows, 35);
		assertEquals("minecraft:nether_wart", item(tRow, 0));
		assertEquals("gt6:distilled_water", fluidIn(tRow, 0));
		assertEquals(750, fluidInAmount(tRow, 0));
		assertEquals("gt6:potion.awkward", fluidOut(tRow, 0));
		assertEquals(750, fluidOutAmount(tRow, 0));
		assertEquals(48, tRow.get("duration").getAsInt());
		assertEquals(16, tRow.get("eut").getAsInt());
		// :159 — the FL.Poison.exists() arm: poison 750 -> Harming 750
		tRow = byLine(tRows, 159);
		assertEquals("gt6:poison", fluidIn(tRow, 0));
		assertEquals("gt6:potion.damage.strong", fluidOut(tRow, 0));
		// :192 — the Glowstone walk face: Heal_1S -> Heal_2S over dust_glowstone
		tRow = byLine(tRows, 192);
		assertEquals("gt6:dust_glowstone", item(tRow, 0));
		assertEquals("gt6:potion.health.splash", fluidIn(tRow, 0));
		assertEquals("gt6:potion.health.strong.splash", fluidOut(tRow, 0));
		// :292 — the Gunpowder splash face: Harm_1 -> Harm_1S over dust_gunpowder
		tRow = byLine(tRows, 292);
		assertEquals("gt6:dust_gunpowder", item(tRow, 0));
		assertEquals("gt6:potion.damage", fluidIn(tRow, 0));
		assertEquals("gt6:potion.damage.splash", fluidOut(tRow, 0));
	}

	@Test
	public void mixerPotionRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("mixerpotions");
		// :325 — Dragon_Breath 1 mB + Harm_1S 3 mB -> Harm_1D 3 mB, duration 1
		JsonObject tRow = byLine(tRows, 325);
		assertEquals(2, tRow.getAsJsonArray("fluidInputs").size(), "the dragon breath blend is a two-fluid row");
		assertEquals("gt6:dragon_breath", fluidIn(tRow, 0));
		assertEquals(1, fluidInAmount(tRow, 0));
		assertEquals("gt6:potion.damage.splash", fluidIn(tRow, 1));
		assertEquals(3, fluidInAmount(tRow, 1));
		assertEquals("gt6:potion.damage.lingering", fluidOut(tRow, 0));
		assertEquals(3, fluidOutAmount(tRow, 0));
		assertEquals(1, tRow.get("duration").getAsInt());
		assertEquals(16, tRow.get("eut").getAsInt());
		assertFalse(tRow.has("inputs"), "addRecipe0 rows carry no items");
		// :354 — the tail: Invisibility_1LS -> Invisibility_1LD
		tRow = byLine(tRows, 354);
		assertEquals("gt6:potion.invisibility.long.splash", fluidIn(tRow, 1));
		assertEquals("gt6:potion.invisibility.long.lingering", fluidOut(tRow, 0));
	}

	@Test
	public void bathRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("bath");
		// Ores:109 — the PinkVitriol wash template over Magnesium (full form)
		JsonObject tRow = bySourceLine(tRows, "Ores\\.java:109\\[PinkVitriol Magnesium full\\]");
		assertEquals("gt6:crushed_purified_magnesium", item(tRow, 0));
		assertEquals("gt6:sulfuricacid", fluidIn(tRow, 0));
		assertEquals(3500, fluidInAmount(tRow, 0));
		assertEquals("gt6:pinkvitriol", fluidOut(tRow, 0));
		assertEquals(3000, fluidOutAmount(tRow, 0));
		assertEquals("gt6:hydrogen", fluidOut(tRow, 1));
		assertEquals(1000, fluidOutAmount(tRow, 1));
		assertEquals(6, tRow.getAsJsonArray("outputs").size(), "centrifuged + 5 tiny chances");
		assertEquals("gt6:crushed_centrifuged_magnesium", item(tRow, 0, true));
		assertEquals(5000, tRow.getAsJsonArray("outputs").get(1).getAsJsonObject().get("chance").getAsInt());
		// Ores:145 — the Pt-group AquaRegia face over Platinum (the sludge tiny x8 outputs)
		tRow = bySourceLine(tRows, "Ores\\.java:145\\[AquaRegia Platinum full\\]");
		assertEquals("gt6:aquaregia", fluidIn(tRow, 0));
		assertEquals(9750, fluidInAmount(tRow, 0));
		assertEquals("gt6:chloroplatinicacid", fluidOut(tRow, 0));
		assertEquals(4500, fluidOutAmount(tRow, 0));
		assertEquals("gt6:nitrogenmonoxide", fluidOut(tRow, 1));
		assertEquals(1500, fluidOutAmount(tRow, 1));
		assertEquals(4125, fluidOutAmount(tRow, 2));
		assertEquals("gt6:crushed_centrifuged_tiny_platinum_group_sludge", item(tRow, 1, true));
		assertEquals(8, tRow.getAsJsonArray("outputs").get(1).getAsJsonObject().get("count").getAsInt(), "the sludge tiny x8 arm");
		assertEquals(5, countOf(tRow, "gt6:crushed_centrifuged_tiny_platinum_group_sludge"), "five sludge slots");
		// Ores:157 — the MnO2-walk face over Hematite (HCl gas 4000, the MnCl2 dustSmall x6 ladder arm)
		tRow = bySourceLine(tRows, "Ores\\.java:157\\[MnO2-walk Hematite full\\]");
		assertEquals("gt6:hydrochloricacid", fluidIn(tRow, 0));
		assertEquals(4000, fluidInAmount(tRow, 0));
		assertEquals(3000, fluidOutAmount(tRow, 0));
		assertEquals("gt6:chlorine", fluidOut(tRow, 1));
		assertEquals("gt6:dust_small_manganese_chloride", item(tRow, 1, true));
		assertEquals(6, tRow.getAsJsonArray("outputs").get(1).getAsJsonObject().get("count").getAsInt(), "the OM.dust ladder arm: U2*3 -> dustSmall x6");
		// Chem:263 — the Eudialyte face (the OM.dust ladder arms: zircon U4*9 -> dustTiny x2)
		tRow = bySourceLine(tRows, "Chem\\.java:263");
		assertEquals("gt6:dust_eudialyte", item(tRow, 0));
		assertEquals(3500, fluidInAmount(tRow, 0));
		// the OM.dust ladder arms (OM.java:460-468): U4*9 zircon fires the dustSmall arm (%U4=0 <= %U9), NOT dustTiny
		assertEquals("gt6:dust_small_zircon", item(tRow, 0, true));
		assertEquals(9, tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals("gt6:dust_silicon_dioxide", item(tRow, 1, true));
		assertEquals("gt6:dust_small_sodium", item(tRow, 2, true));
		assertEquals("gt6:dust_small_calcium", item(tRow, 3, true));
		// Vanilla:723 — the chlorine bleach face (the 1.7.10 meta 15-i -> modern red wool)
		tRow = bySourceLine(tRows, "Vanilla\\.java:723\\[red wool\\]");
		assertEquals("minecraft:red_wool", item(tRow, 0));
		assertEquals("gt6:chlorine", fluidIn(tRow, 0));
		assertEquals(50, fluidInAmount(tRow, 0));
		assertEquals("minecraft:white_wool", item(tRow, 0, true));
		// Vanilla:748 — the reeds -> paper FL.waters(125, 100) legs
		int tReeds = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tCandidate = tElement.getAsJsonObject();
			JsonElement tCommentElement = tCandidate.get("comment");
			if (tCommentElement != null && tCommentElement.getAsString().startsWith("Loader_Recipes_Vanilla.java:748")) {
				tReeds++;
				assertEquals("minecraft:sugar_cane", item(tCandidate, 0));
				assertEquals("minecraft:paper", item(tCandidate, 0, true));
			}
		}
		assertEquals(4, tReeds, "the FL.waters legs: water/mnwtr/distw/spdew");
	}

	/** The w1 smoke row stays the FIRST bath row untouched (only-add-never-duplicate). */
	@Test
	public void theW1SmokeRowSurvivesUntouchedAtTheHead() throws Exception {
		JsonArray tRows = pourShipped("bath");
		JsonObject tSmoke = tRows.get(0).getAsJsonObject();
		assertTrue(tSmoke.get("comment") == null, "the smoke row predates the comment convention");
		assertEquals("minecraft:white_wool", tSmoke.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString());
	}

	/** The file heads keep naming the declared-out bands (the honest-default form). */
	@Test
	public void theFileHeadsNameTheirDeclaredOutBands() throws Exception {
		String tBath = head("bath");
		for (String tNeedle : new String[] {"Pyrotheum chunk band", "Osmium member", "DYE_FLUIDS dye bath band",
				"gold-molten", "fireproof", "Mana_TE", "wood-oil ladder", "b8 residual"}) {
			assertTrue(tBath.contains(tNeedle), "bath header names: " + tNeedle);
		}
		String tDist = head("distillery");
		for (String tNeedle : new String[] {"Food_Potato_Poisonous", "gem_chipped_sugar", "rabbit_foot", "fermenter.json"}) {
			assertTrue(tDist.contains(tNeedle), "distillery header names: " + tNeedle);
		}
		assertTrue(head("mixerpotions").contains("Dragon_Breath"), "mixerpotions header names its face");
	}

	/**
	 * The id universe: every gt6 id the three files carry must be a registered item
	 * (GTMaterialItems + GTMaterialBlocks registrationOrder — the b1/b2a/b2b1 pattern)
	 * or a spec-registered fluid (the GTFluids spec tables — the offline truth). A miss
	 * here is the dead-row class the GTFluids ruling cuts.
	 */
	@Test
	public void everyGt6IdRidesTheRegistrationUniverse() throws Exception {
		Set<String> tItems = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tItems.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			tItems.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		// task circuit-chain-recipes — the circuit chain's registered siblings (the bath soldering
		// legs' board/circuit outputs ride this walk, the card's own id-universe pin shape)
		for (var tRow : GT6CircuitChain.ROWS) tItems.add("gt6:" + tRow.path());
		for (var tRow : gregtech6.registry.GT6Batteries.CIRCUIT_ROWS) tItems.add("gt6:" + tRow.path());
		Set<String> tFluids = new HashSet<>();
		for (GTFluids.EngineFluidSpec tSpec : GTFluids.ENGINE_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.AQUA_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.SIMPLE_LIQUID_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_FLUID_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_B1_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_B2_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_TAIL_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.CHEMICAL_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.CLOSURE_FLUID_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.BEE_ROW_FLUID_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.HONEY_FLUID_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.HOT_FLUID_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.LUBRICANT_FLUID_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.QU_FLUID_SPECS) tFluids.add("gt6:" + tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.NAMING_FLUID_SPECS) tFluids.add("gt6:" + tSpec.name());
		// the dye bath band families (the b3 pour rides them — the GT6Bottles/GTFluids registration face;
		// gt6:squidink/indigo ride NAMING_FLUID_SPECS above, the Loader_Fluids :111/:112 seeds)
		for (GTFluids.DyeFluid tFamily : GTFluids.DYE_WATERMIXED) tFluids.add("gt6:" + tFamily.name());
		for (GTFluids.DyeFluid tFamily : GTFluids.DYE_FLOWER) tFluids.add("gt6:" + tFamily.name());
		for (GTFluids.DyeChemicalFluid tFamily : GTFluids.DYE_CHEMICALS) tFluids.add("gt6:" + tFamily.name());
		// the individually-registered faces (the iron_molten/chlorine shape — DR handles, no spec row)
		tFluids.add("gt6:chlorine");
		assertTrue(tFluids.contains("gt6:dragon_breath"), "the closure face is enumerated");
		assertTrue(tItems.size() > 50000, "the item universe built (" + tItems.size() + " ids)");

		Set<String> tMisses = new LinkedHashSet<>();
		for (String tKey : CENSUS.keySet()) {
			for (JsonElement tElement : pourShipped(tKey)) {
				JsonObject tRow = tElement.getAsJsonObject();
				collectMisses(tRow, "inputs", "item", tItems, tMisses);
				collectMisses(tRow, "outputs", "item", tItems, tMisses);
				collectMisses(tRow, "fluidInputs", "fluid", tFluids, tMisses);
				collectMisses(tRow, "fluidOutputs", "fluid", tFluids, tMisses);
			}
		}
		assertEquals(Set.of(), tMisses, "every gt6 id rides the registration universe");
	}

	private static void collectMisses(JsonObject aRow, String aArrayKey, String aIdKey, Set<String> aUniverse, Set<String> aMisses) {
		if (!aRow.has(aArrayKey)) return;
		for (JsonElement tElement : aRow.getAsJsonArray(aArrayKey)) {
			String tId = tElement.getAsJsonObject().get(aIdKey).getAsString();
			if (tId.startsWith("gt6:") && !aUniverse.contains(tId)) aMisses.add(tId);
		}
	}

	// ---- the shipped-file readers (the Ananas-pin posture: fluid identity lives in the json, not the stand-in seam)

	private static String head(String aKey) throws Exception {
		try (InputStream tStream = GT6RecipeMapDataB2RowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + aKey + ".json")) {
			return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static JsonObject byLine(JsonArray aRows, int aLine) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.get("comment").getAsString().endsWith(":" + aLine)) return tRow;
		}
		throw new AssertionError("no row for source line :" + aLine);
	}

	private static JsonObject bySourceLine(JsonArray aRows, String aRegex) {
		Pattern tPattern = Pattern.compile(aRegex);
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tPattern.matcher(tRow.get("comment").getAsString()).find()) return tRow;
		}
		throw new AssertionError("no row matching " + aRegex);
	}

	private static String item(JsonObject aRow, int aIndex) {
		return item(aRow, aIndex, false);
	}

	private static String item(JsonObject aRow, int aIndex, boolean aFromOutputs) {
		return slot(aRow, aFromOutputs ? "outputs" : "inputs", aIndex).get("item").getAsString();
	}

	private static int countOf(JsonObject aRow, String aItemId) {
		int rCount = 0;
		for (JsonElement tElement : aRow.getAsJsonArray("outputs")) {
			if (aItemId.equals(tElement.getAsJsonObject().get("item").getAsString())) rCount++;
		}
		return rCount;
	}

	private static JsonObject slot(JsonObject aRow, String aKey, int aIndex) {
		return aRow.getAsJsonArray(aKey).get(aIndex).getAsJsonObject();
	}

	private static String fluidIn(JsonObject aRow, int aIndex) { return slot(aRow, "fluidInputs", aIndex).get("fluid").getAsString(); }
	private static int fluidInAmount(JsonObject aRow, int aIndex) { return slot(aRow, "fluidInputs", aIndex).get("amount").getAsInt(); }
	private static String fluidOut(JsonObject aRow, int aIndex) { return slot(aRow, "fluidOutputs", aIndex).get("fluid").getAsString(); }
	private static int fluidOutAmount(JsonObject aRow, int aIndex) { return slot(aRow, "fluidOutputs", aIndex).get("amount").getAsInt(); }
}
