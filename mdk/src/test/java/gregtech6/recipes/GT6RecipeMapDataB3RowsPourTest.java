package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
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
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The recipe-b3-dye-bath-band row-stock pour test: the {@code bath.json} dye bath band —
 * Loader_Recipes_Vanilla.java:727-:735, the {@code DYE_FLUIDS[i]} inner loop of the
 * :718 16-colour walk, replayed as 244 rows (wool 47 + carpet 47 + terracotta 50 +
 * stained glass 50 + stained glass pane 50). The upstream facts the replay pins:
 *
 * <ul>
 * <li>the :728 guard skips the wool/carpet legs for {@code DYE_INDEX_White = 15}
 * (CS.java:457) — the White legs are FAITHFULLY ABSENT (upstream has no white dye-bath
 * leg; the preserved w1 smoke row stays the only white-wool row), while the three
 * stained faces take all 16 colours;</li>
 * <li>the per-colour fluid legs ride the upstream {@code DYE_FLUIDS[i]} contents: the
 * :111/:112 seeds (gt6:squidink → Black, gt6:indigo → Blue) add a 4th leg to exactly
 * those two colours, every other colour carries the :121-:123 three-family walk
 * (watermixed/flower/chemical) — 50 distinct fluids, all live in the port
 * ({@code GT6Bottles} dye walk + {@code GTFluids.DYE_CHEMICALS/DYE_WATERMIXED/DYE_FLOWER});</li>
 * <li>the amounts resolve {@code FL.mul(tDye, a, b, T)} = {@code UT.Code.units}
 * (UT.java:1677-1683, roundUp) over the per-fluid base {@code CS.L = 144} mB
 * (CS.java:129 — NOT 1000; the research-note 62/31/23 guess was the L=1000 trap):
 * 144/16 = 9 (wool/terracotta/glass), 144/32 → 5 (carpet), 144*3/128 → 4 (pane);</li>
 * <li>the output meta {@code 15-i} maps the dye index onto the vanilla BLOCK colour
 * order (block meta 0=White..15=Black), the port snake ids via
 * {@link GTSprayCanItem#DYE_IDS} (the light_gray declared deviation);</li>
 * <li>eut 0 / duration 16 verbatim (the {@code addRecipe1(T, 0, 16, ...)} literals).</li>
 * </ul>
 *
 * <p>The 334 pre-existing rows (the w1 smoke + the b2 Ores/Chem/Other/Vanilla faces) are
 * b2 sovereignty — only counted here, never rewritten. The PhaseGate SNAPSHOT stays
 * untouched (the r11e JSON-seam convention). KJS surface: datapack domain, naturally
 * editable, zero adapter.
 */
public class GT6RecipeMapDataB3RowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The band census: bath.json = the 334 pre-b3 rows + the 244 dye-band rows + the 7 Sn soldering legs (task circuit-chain-recipes — the review-seat ratchet). */
	private static final int CENSUS = 585;

	/** The band comment lines (the five upstream statements) and the vanilla-id block face each dyes. */
	private static final int[] BAND_LINES = {729, 730, 732, 733, 734};
	private static final Pattern BAND_COMMENT = Pattern.compile("^Loader_Recipes_Vanilla\\.java:(\\d+)\\[(.+)]$");
	private static final String[] BLOCK_TAILS = {"_wool", "_carpet", "_terracotta", "_stained_glass", "_stained_glass_pane"};

	/** The vanilla BLOCK colour order (block meta 0=White..15=Black) — the {@code 15-i} output target names. */
	private static final String[] BLOCK_COLOR = {"white", "orange", "magenta", "light_blue", "yellow", "lime",
			"pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

	/** The dye index of a band fluid id (the live port family dump fills this). */
	private static Map<String, Integer> DYE_FLUID_INDEX;

	/** The live port dye-fluid universe (the btl registration face, offline-string form). */
	private static Set<String> DYE_FLUID_UNIVERSE;

	@BeforeAll
	static void bootMaterials() {
		GTMaterialItems.initMaterials(); // the id-universe face (the b1 dump convention)
		DYE_FLUID_UNIVERSE = new HashSet<>();
		DYE_FLUID_INDEX = new HashMap<>();
		for (GTFluids.DyeFluid tFamily : GTFluids.DYE_WATERMIXED) register(tFamily.name(), tFamily.dyeIndex);
		for (GTFluids.DyeFluid tFamily : GTFluids.DYE_FLOWER) register(tFamily.name(), tFamily.dyeIndex);
		for (GTFluids.DyeChemicalFluid tFamily : GTFluids.DYE_CHEMICALS) register(tFamily.name(), tFamily.dyeIndex);
		register("squidink", 0);  // the Loader_Fluids.java:111 seed → DYE_FLUIDS[0]
		register("indigo", 4);    // the Loader_Fluids.java:112 seed → DYE_FLUIDS[4]
	}

	private static void register(String aName, int aIndex) {
		DYE_FLUID_UNIVERSE.add("gt6:" + aName);
		DYE_FLUID_INDEX.put("gt6:" + aName, aIndex);
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

	/** Reads the shipped file, pours it under the bath map key, returns its parsed row array. */
	private JsonArray pourShipped() throws Exception {
		String tPath = "/data/gt6/recipe_maps/bath.json";
		InputStream tStream = GT6RecipeMapDataB3RowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", "bath"), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: the file pours its full row count — no WARN-skipped rows, no truncation. */
	@Test
	public void bathPoursItsFullCensusWithZeroSkips() throws Exception {
		pourShipped();
		assertEquals(CENSUS, GT6RecipeMapJsonLoader.pouredCount("bath"),
				"bath: the tracker mirrors the census (a smaller number = WARN-skipped rows)");
		assertEquals(CENSUS, GT6RecipeMaps.BATH.mRecipeList.size(),
				"bath: the map holds exactly the card rows (the Java wood-oil ladder pours only on a registry-bound JVM)");
	}

	/** The band decomposes into the five upstream statements (:729 wool, :730 carpet, :732-:734 the stained trio). */
	@Test
	public void theDyeBandDecomposesIntoTheFiveUpstreamStatements() throws Exception {
		JsonArray tRows = pourShipped();
		assertEquals(47, countBandLine(tRows, 729), "wool: 47 legs = 15 colours x 3 + the squidink/indigo 4th legs");
		assertEquals(47, countBandLine(tRows, 730), "carpet: 47 legs, the same shape");
		assertEquals(50, countBandLine(tRows, 732), "terracotta: 16 colours x 3 + 2 seeds");
		assertEquals(50, countBandLine(tRows, 733), "stained glass: 16 colours x 3 + 2 seeds");
		assertEquals(50, countBandLine(tRows, 734), "stained glass pane: 16 colours x 3 + 2 seeds");
	}

	private static int countBandLine(JsonArray aRows, int aLine) {
		int rCount = 0;
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			JsonElement tComment = tRow.get("comment");
			if (tComment != null && tComment.getAsString().startsWith("Loader_Recipes_Vanilla.java:" + aLine + "[")) rCount++;
		}
		return rCount;
	}

	/** The White legs are faithfully absent: no white dye fluid ever dyes wool/carpet, while the stained trio takes it. */
	@Test
	public void theWhiteLegsAreFaithfullyAbsent() throws Exception {
		JsonArray tRows = pourShipped();
		int tWhiteStained = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (bandLineOf(tRow) < 0) continue;
			String tFluid = tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString();
			String tOut = tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString();
			if (tFluid.endsWith("_white")) {
				assertFalse(tOut.endsWith("_wool") || tOut.endsWith("_carpet"),
						"the :728 guard: no white leg dyes wool/carpet (DYE_INDEX_White=15), saw " + tFluid + " -> " + tOut);
				tWhiteStained++;
			}
		}
		assertEquals(9, tWhiteStained, "the White colour still takes the :732/:733/:734 stained trio (3 fluids x 3 faces)");
		String tHead = head();
		assertTrue(tHead.contains("DYE_FLUIDS dye bath band"), "the header names the band (the b2 head-pin needle stays)");
		assertTrue(tHead.contains("DYE_INDEX_White=15"), "the header declares the White absence");
		assertTrue(tHead.contains("CS.L=144"), "the header names the amount base (the L=1000 research-note trap)");
	}

	/** The per-colour legs: 4 fluids for Black (squidink) and Blue (indigo), 3 for every other colour. */
	@Test
	public void everyColourCarriesThreeFluidLegsExceptBlackAndBlueWithFour() throws Exception {
		JsonArray tRows = pourShipped();
		Map<Integer, Set<String>> tFluidsPerColour = new HashMap<>();
		Map<Integer, Integer> tRowsPerColour = new HashMap<>();
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			int tLine = bandLineOf(tRow);
			if (tLine < 0) continue;
			int tFace = faceOf(tLine);
			String tFluid = tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString();
			Integer tColour = DYE_FLUID_INDEX.get(tFluid);
			assertNotNull(tColour, "the band fluid rides the live port family: " + tFluid);
			tFluidsPerColour.computeIfAbsent(tColour, k -> new HashSet<>()).add(tFluid);
			tRowsPerColour.merge(tColour, 1, Integer::sum);
			// the output meta 15-i: dye index i lands on the vanilla BLOCK colour 15-i of this face
			String tOut = tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString();
			String tExpected = "minecraft:" + BLOCK_COLOR[15 - tColour] + BLOCK_TAILS[tFace];
			assertEquals(tExpected, tOut, "the 15-i output meta for dye index " + tColour + " on :" + tLine);
		}
		assertEquals(16, tFluidsPerColour.size(), "all 16 colours pour");
		for (Map.Entry<Integer, Set<String>> tEntry : tFluidsPerColour.entrySet()) {
			int tExpected = (tEntry.getKey() == 0 || tEntry.getKey() == 4) ? 4 : 3;
			assertEquals(tExpected, tEntry.getValue().size(),
					"colour " + tEntry.getKey() + " (" + GTSprayCanItem.DYE_IDS[tEntry.getKey()] + ") carries " + tExpected + " fluid legs");
		}
		assertEquals(20, tRowsPerColour.get(0), "Black: 4 fluids x 5 faces");
		assertEquals(20, tRowsPerColour.get(4), "Blue: 4 fluids x 5 faces");
		assertEquals(9, tRowsPerColour.get(15), "White: 3 fluids x the 3 stained faces only");
	}

	/** The face index of a band statement line (the BLOCK_TAILS slot), or -1. */
	private static int faceOf(int aLine) {
		for (int i = 0; i < BAND_LINES.length; i++) if (BAND_LINES[i] == aLine) return i;
		return -1;
	}

	/** The band statement line a row replays, or -1 (non-band rows: smoke/b2 faces/:739-:741/:748). */
	private static int bandLineOf(JsonObject aRow) {
		JsonElement tComment = aRow.get("comment");
		if (tComment == null) return -1;
		Matcher tMatcher = BAND_COMMENT.matcher(tComment.getAsString());
		if (!tMatcher.matches()) return -1;
		int tLine = Integer.parseInt(tMatcher.group(1));
		return faceOf(tLine) >= 0 ? tLine : -1;
	}

	/** The verbatim spot checks (field-level, one per band statement), reading the SHIPPED rows. */
	@Test
	public void theBandRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped();
		// :729 — the wool face over the watermixed Black fluid (144/16 = 9 mB)
		JsonObject tRow = byComment(tRows, "Loader_Recipes_Vanilla.java:729[dye_watermixed_black]");
		assertEquals("minecraft:white_wool", item(tRow, 0));
		assertEquals("gt6:dye_watermixed_black", fluidIn(tRow, 0));
		assertEquals(9, fluidInAmount(tRow, 0));
		assertEquals("minecraft:black_wool", item(tRow, 0, true));
		assertEquals(16, tRow.get("duration").getAsInt());
		assertEquals(0, tRow.get("eut").getAsInt());
		// :730 — the carpet face over the squidink seed (144/32 -> 5 mB, the roundUp arm)
		tRow = byComment(tRows, "Loader_Recipes_Vanilla.java:730[squidink]");
		assertEquals("minecraft:white_carpet", item(tRow, 0));
		assertEquals("gt6:squidink", fluidIn(tRow, 0));
		assertEquals(5, fluidInAmount(tRow, 0));
		assertEquals("minecraft:black_carpet", item(tRow, 0, true));
		// :732 — the terracotta face over the indigo seed (Blue, the 4th leg)
		tRow = byComment(tRows, "Loader_Recipes_Vanilla.java:732[indigo]");
		assertEquals("minecraft:terracotta", item(tRow, 0));
		assertEquals("gt6:indigo", fluidIn(tRow, 0));
		assertEquals(9, fluidInAmount(tRow, 0));
		assertEquals("minecraft:blue_terracotta", item(tRow, 0, true));
		// :733 — the glass face over the watermixed White fluid (the White colour still dyes glass)
		tRow = byComment(tRows, "Loader_Recipes_Vanilla.java:733[dye_watermixed_white]");
		assertEquals("minecraft:glass", item(tRow, 0));
		assertEquals("gt6:dye_watermixed_white", fluidIn(tRow, 0));
		assertEquals(9, fluidInAmount(tRow, 0));
		assertEquals("minecraft:white_stained_glass", item(tRow, 0, true));
		// :734 — the pane face over the chemical Light Gray fluid (144*3/128 -> 4 mB, the snake deviation colour)
		tRow = byComment(tRows, "Loader_Recipes_Vanilla.java:734[dye_chemical_light_gray]");
		assertEquals("minecraft:glass_pane", item(tRow, 0));
		assertEquals("gt6:dye_chemical_light_gray", fluidIn(tRow, 0));
		assertEquals(4, fluidInAmount(tRow, 0));
		assertEquals("minecraft:light_gray_stained_glass_pane", item(tRow, 0, true));
	}

	/**
	 * The amounts resolve through the upstream ladder: a verbatim transcription of
	 * UT.Code.units (UT.java:1677-1683) over CS.L=144 reproduces every band amount —
	 * and the hard anchors catch a transcription drift (the 62/31/23 research-note
	 * hand-calc forgot the roundUp arm and the L base; this pin is the interlock).
	 */
	@Test
	public void theBandAmountsResolveThroughTheUpstreamUnitsLadder() throws Exception {
		// the UT.java:1677-1683 shape verbatim (aOriginalUnit -> aTargetUnit, roundUp)
		assertEquals(9, units(144, 16, 1, true), "FL.mul(tDye, 1, 16, T): 144/16 exact");
		assertEquals(5, units(144, 32, 1, true), "FL.mul(tDye, 1, 32, T): 4.5 rounds up");
		assertEquals(4, units(144, 128, 3, true), "FL.mul(tDye, 3, 128, T): 3.375 rounds up");
		JsonArray tRows = pourShipped();
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			int tLine = bandLineOf(tRow);
			if (tLine < 0) continue;
			int tMul, tDiv;
			switch (tLine) {
				case 730: tMul = 1; tDiv = 32; break;
				case 734: tMul = 3; tDiv = 128; break;
				default: tMul = 1; tDiv = 16; break; // :729/:732/:733
			}
			assertEquals(units(144, tDiv, tMul, true), fluidInAmount(tRow, 0),
					tRow.get("comment").getAsString() + ": FL.mul(tDye, " + tMul + ", " + tDiv + ", T) over CS.L=144");
		}
	}

	/** UT.Code.units verbatim (UT.java:1677-1683) — the offline transcription the amount pin rides. */
	private static long units(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
		if (aTargetUnit == 0) return 0;
		if (aOriginalUnit == aTargetUnit || aOriginalUnit == 0) return aAmount;
		if (aOriginalUnit %   aTargetUnit == 0) {aOriginalUnit /=   aTargetUnit;   aTargetUnit = 1;} else
		if (aTargetUnit   % aOriginalUnit == 0) {  aTargetUnit /= aOriginalUnit; aOriginalUnit = 1;}
		return Math.max(0, ((aAmount * aTargetUnit) / aOriginalUnit) + (aRoundUp && (aAmount * aTargetUnit) % aOriginalUnit > 0 ? 1 : 0));
	}

	/** The band fluids: exactly the 50-entry live port universe, every entry used, nothing foreign. */
	@Test
	public void everyBandFluidRidesTheLivePortDyeFamilies() throws Exception {
		JsonArray tRows = pourShipped();
		Set<String> tUsed = new LinkedHashSet<>();
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (bandLineOf(tRow) < 0) continue;
			tUsed.add(tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString());
		}
		assertEquals(50, tUsed.size(), "the DYE_FLUIDS content: 16 x 3 families + the squidink/indigo seeds");
		assertEquals(DYE_FLUID_UNIVERSE, tUsed, "the band fluids == the live port registration face (all used, none foreign)");
	}

	/** The id universe: every gt6 id the file carries must be a registered item or a spec-registered fluid. */
	@Test
	public void everyGt6IdRidesTheRegistrationUniverse() throws Exception {
		Set<String> tItems = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tItems.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		// task circuit-chain-recipes — the circuit chain's registered siblings (the bath soldering
		// legs' board/circuit outputs ride these walks, the card's own id-universe pin shape)
		for (var tRow : gregtech6.registry.GT6CircuitChain.ROWS) tItems.add("gt6:" + tRow.path());
		for (var tRow : gregtech6.registry.GT6Batteries.CIRCUIT_ROWS) tItems.add("gt6:" + tRow.path());
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			tItems.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		Set<String> tFluids = new HashSet<>(DYE_FLUID_UNIVERSE);
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
		tFluids.add("gt6:chlorine"); // the individually-registered face (the DR handle, no spec row)
		assertTrue(tItems.size() > 50000, "the item universe built (" + tItems.size() + " ids)");

		Set<String> tMisses = new LinkedHashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			collectMisses(tRow, "inputs", "item", tItems, tMisses);
			collectMisses(tRow, "outputs", "item", tItems, tMisses);
			collectMisses(tRow, "fluidInputs", "fluid", tFluids, tMisses);
			collectMisses(tRow, "fluidOutputs", "fluid", tFluids, tMisses);
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

	// ---- the shipped-file readers (the b2 posture: fluid identity lives in the json, not the stand-in seam)

	private static String head() throws Exception {
		try (InputStream tStream = GT6RecipeMapDataB3RowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/bath.json")) {
			return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static JsonObject byComment(JsonArray aRows, String aComment) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (aComment.equals(tRow.get("comment") == null ? "" : tRow.get("comment").getAsString())) return tRow;
		}
		throw new AssertionError("no row commented " + aComment);
	}

	private static String item(JsonObject aRow, int aIndex) {
		return item(aRow, aIndex, false);
	}

	private static String item(JsonObject aRow, int aIndex, boolean aFromOutputs) {
		return aRow.getAsJsonArray(aFromOutputs ? "outputs" : "inputs").get(aIndex).getAsJsonObject().get("item").getAsString();
	}

	private static String fluidIn(JsonObject aRow, int aIndex) { return aRow.getAsJsonArray("fluidInputs").get(aIndex).getAsJsonObject().get("fluid").getAsString(); }
	private static int fluidInAmount(JsonObject aRow, int aIndex) { return aRow.getAsJsonArray("fluidInputs").get(aIndex).getAsJsonObject().get("amount").getAsInt(); }
}
