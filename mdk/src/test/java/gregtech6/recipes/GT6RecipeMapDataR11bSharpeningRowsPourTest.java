package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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

import gregapi.data.CS;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.UT;
import gregtech6.registry.GTMaterialItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The toolhead-r11b sharpening walk replay (the GT6RecipeMapDataB1RowsPourTest fixture
 * posture): the shipped {@code sharpening.json} carries the THREE seated rows (the smoke
 * sandstone row + the two B1 Vanilla rows) plus the C1 conversion-path walk — the 20
 * handler statements of Loader_Recipes_Handlers.java:396-415 expanded per material over
 * the port item universe, the polarizer/B2c-cut 家法 (query-time lazy per-material
 * generation frozen to the port universe snapshot — the P8 ruling cut the handler
 * framework, so the JSON static row is the only datapack face).
 *
 * <p><b>Walk engine semantics</b> (RecipeMapHandlerPrefix.java:204-218, the
 * GT6RecipesShCL lathe-transcription twin — same handler class, same arms):
 * <ul>
 * <li>condition gate :205 + the And() heads — the seven misc rows :396-402 carry
 *     {@code And(ANTIMATTER.NOT, COATED.NOT)} (the :402 rockGt row adds STONE), the
 *     thirteen raw rows :403-415 carry {@code ANTIMATTER.NOT}, and every row excludes
 *     {@code INVALID_MATERIAL}.</li>
 * <li>both-side item existence :209/:214 — the input AND output (prefix, material) pair
 *     must be members of the port registration walk (GTMaterialItems.registrationOrder).</li>
 * <li>duration :218 mDuration=0 → {@code max(1, getCosts)} = UT.Code.units(max(unitsIn,
 *     unitsOut), U, mult + mult*mToolQuality, T) with mult 256 on the misc rows and 16 on
 *     the raw rows (GT6RecipesShCL.handlerCosts, the compressor transcription).</li>
 * <li>the pulverized-remains face :79/:215 — EVERY sharpening row requests the remains
 *     (the 15-arg ctor 13th arg T): wherever {@code unitsIn − unitsOut >= dustDiv72.mAmount}
 *     the row appends {@code OM.pulverize(mat, surplus)} = the GT6RecipesShCL.dustCascade
 *     over {@code mTargetPulver} as a SECOND output. The misc rows' {@code .chances(10000,
 *     7500)} aligns to that pair: main output 100%, remains 75% — the JSON carries the
 *     7500 on the second slot only (a 10000 slot is the loader default).</li>
 * <li>DECLARED DEVIATION (the S11' review class, GT6RecipesShCL.buildLatheRecipe :407):
 *     an unresolvable cascade (no dust-family item for the target material) drops the
 *     REMAINS SLOT ONLY, never the row — the upstream trailing-null truncation face
 *     (Recipe.java:895), which keeps the raw→finished conversion path alive for every
 *     material whose head items exist. The JSON cannot express a null slot, so the
 *     truncated row is the faithful form.</li>
 * </ul>
 *
 * <p>The census pins are LITERALS (the ratchet); the replay test re-walks the live
 * registration and compares the shipped rows bidirectionally, so a regeneration drift or
 * a material-system change turns both red.
 */
public class GT6RecipeMapDataR11bSharpeningRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The seated rows that predate this card — keyed by their vanilla input id (all three). */
	private static final Set<String> SEATED_VANILLA_INPUTS = Set.of("minecraft:sandstone", "minecraft:flint", "minecraft:glass_pane");

	// ---------------------------------------------------------------------------
	// the census ratchet — literals, filled from the walk dump (statement order)
	// ---------------------------------------------------------------------------

	/** Per-statement expanded row counts, :396-:415 in upstream order. */
	private static final Map<String, Integer> STATEMENT_CENSUS = new LinkedHashMap<>();
	static {
		STATEMENT_CENSUS.put(":396", 339); // nugget -> round
		STATEMENT_CENSUS.put(":397", 113); // plateGem -> lens
		STATEMENT_CENSUS.put(":398", 201); // gem -> stick
		STATEMENT_CENSUS.put(":399", 331); // ingot -> stick
		STATEMENT_CENSUS.put(":400", 331); // billet -> stick
		STATEMENT_CENSUS.put(":401", 95);  // gemChipped -> arrow x2
		STATEMENT_CENSUS.put(":402", 79);  // rockGt -> arrow x8 (STONE)
		STATEMENT_CENSUS.put(":403", 534); // raw arrow
		STATEMENT_CENSUS.put(":404", 309); // raw saw
		STATEMENT_CENSUS.put(":405", 309); // raw chisel
		STATEMENT_CENSUS.put(":406", 539); // raw sword
		STATEMENT_CENSUS.put(":407", 539); // raw pickaxe
		STATEMENT_CENSUS.put(":408", 539); // raw shovel
		STATEMENT_CENSUS.put(":409", 539); // raw spade
		STATEMENT_CENSUS.put(":410", 309); // raw universal spade
		STATEMENT_CENSUS.put(":411", 539); // raw axe
		STATEMENT_CENSUS.put(":412", 309); // raw double axe
		STATEMENT_CENSUS.put(":413", 539); // raw hoe
		STATEMENT_CENSUS.put(":414", 539); // raw sense
		STATEMENT_CENSUS.put(":415", 539); // raw plow
	}

	/** The walk total (sum of the statement census above). */
	private static int walkCensus() {
		return STATEMENT_CENSUS.values().stream().mapToInt(Integer::intValue).sum();
	}

	// ---------------------------------------------------------------------------
	// the walk engine (the ShCL transcription, JSON-face projection)
	// ---------------------------------------------------------------------------

	/** One upstream handler statement (Loader_Recipes_Handlers.java:396-415). */
	record Statement(String cite, OreDictPrefix inPrefix, int inCount, OreDictPrefix outPrefix, int outCount,
			long multiplier, boolean stoneGate, boolean chancesFace) {}

	/** The 20 statements, upstream file order. eUt 16 throughout; duration 0 (the getCosts arm).
	 * Lazy: resolved on first CALL (always post-boot, inside a test method) — a static initializer
	 * reading OP.* class-loads ahead of OP.init() and freezes the null prefixes JVM-wide (the
	 * GT6RegistryStaticInitGuardTest rule; the early OP class-init also poisoned the neo
	 * registry+recipes batch order — the seam/surface-rock reds). */
	private static List<Statement> sStatements;

	private static List<Statement> statements() {
		if (sStatements != null) return sStatements;
		return sStatements = List.of(
			new Statement(":396", OP.nugget    , 1, OP.round        , 1, 256, false, true ),
			new Statement(":397", OP.plateGem  , 1, OP.lens         , 1, 256, false, true ),
			new Statement(":398", OP.gem       , 1, OP.stick        , 1, 256, false, true ),
			new Statement(":399", OP.ingot     , 1, OP.stick        , 1, 256, false, true ),
			new Statement(":400", OP.billet    , 1, OP.stick        , 1, 256, false, true ),
			new Statement(":401", OP.gemChipped, 1, OP.toolHeadArrow, 2, 256, false, true ),
			new Statement(":402", OP.rockGt    , 1, OP.toolHeadArrow, 8, 256, true , true ),
			new Statement(":403", OP.toolHeadRawArrow        , 1, OP.toolHeadArrow        , 1, 16, false, false),
			new Statement(":404", OP.toolHeadRawSaw          , 1, OP.toolHeadSaw          , 1, 16, false, false),
			new Statement(":405", OP.toolHeadRawChisel       , 1, OP.toolHeadChisel       , 1, 16, false, false),
			new Statement(":406", OP.toolHeadRawSword        , 1, OP.toolHeadSword        , 1, 16, false, false),
			new Statement(":407", OP.toolHeadRawPickaxe      , 1, OP.toolHeadPickaxe      , 1, 16, false, false),
			new Statement(":408", OP.toolHeadRawShovel       , 1, OP.toolHeadShovel       , 1, 16, false, false),
			new Statement(":409", OP.toolHeadRawSpade        , 1, OP.toolHeadSpade        , 1, 16, false, false),
			new Statement(":410", OP.toolHeadRawUniversalSpade, 1, OP.toolHeadUniversalSpade, 1, 16, false, false),
			new Statement(":411", OP.toolHeadRawAxe          , 1, OP.toolHeadAxe          , 1, 16, false, false),
			new Statement(":412", OP.toolHeadRawAxeDouble    , 1, OP.toolHeadAxeDouble    , 1, 16, false, false),
			new Statement(":413", OP.toolHeadRawHoe          , 1, OP.toolHeadHoe          , 1, 16, false, false),
			new Statement(":414", OP.toolHeadRawSense        , 1, OP.toolHeadSense        , 1, 16, false, false),
			new Statement(":415", OP.toolHeadRawPlow         , 1, OP.toolHeadPlow         , 1, 16, false, false));
	}

	/** One walked row, JSON-face: input id+count, output slots (id, count, chance-or-0), duration. */
	record Row(Statement st, String inId, int inCount, List<Out> outs, long duration) {
		record Out(String id, int count, long chance) {}
		/** The normalized replay key: the exact JSON projection, order-sensitive. */
		String key() {
			StringBuilder rKey = new StringBuilder(inId).append('x').append(inCount).append("->");
			for (Out tOut : outs) rKey.append(tOut.id).append(':').append(tOut.count)
					.append(tOut.chance != 0 ? "@" + tOut.chance : "").append(',');
			return rKey.append('d').append(duration).append('e').append(16).toString();
		}
	}

	/** The universe map: prefix -> its registered materials (the both-side existence set). */
	private static Map<OreDictPrefix, Set<OreDictMaterial>> universe() {
		Map<OreDictPrefix, Set<OreDictMaterial>> rUniverse = new HashMap<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			rUniverse.computeIfAbsent(tPair.prefix(), k -> new HashSet<>()).add(tPair.material());
		}
		return rUniverse;
	}

	/** The condition gate: :205 INVALID_MATERIAL + the And() heads (ANTIMATTER/COATED [+STONE]). */
	private static boolean passesCondition(Statement aSt, OreDictMaterial aMaterial) {
		if (aMaterial.contains(TD.Properties.INVALID_MATERIAL)) return false;
		if (aMaterial.contains(TD.Atomic.ANTIMATTER)) return false;
		if (aMaterial.contains(TD.Compounds.COATED)) return false;
		if (aSt.stoneGate() && !aMaterial.contains(TD.Properties.STONE)) return false;
		return true;
	}

	/** Upstream getCosts (:225-227) — the GT6RecipesShCL.handlerCosts transcription. */
	static long handlerCosts(long aUnitsIn, long aUnitsOut, long aMultiplier, OreDictMaterial aMaterial) {
		long tAmount = Math.max(aUnitsIn, aUnitsOut);
		long tTarget = aMultiplier + aMultiplier * aMaterial.mToolQuality;
		if (tTarget == 0) return 0;
		return Math.max(0, tAmount * tTarget / CS.U + ((tAmount * tTarget) % CS.U > 0 ? 1 : 0));
	}

	/** The :79 remains gate + the :215 OM.pulverize face — the GT6RecipesShCL.pulverizeOutput + dustCascade transcription over id strings. */
	static Row.Out remainsOutput(OreDictMaterial aMaterial, long aSurplus, Map<OreDictPrefix, Set<OreDictMaterial>> aUniverse) {
		if (aSurplus < OP.dustDiv72.mAmount) return null; // the ctor gate :79
		OreDictMaterial tTarget = aMaterial.mTargetPulver.mMaterial;
		long tAmount = UT.Code.units(aSurplus, CS.U, aMaterial.mTargetPulver.mAmount, false);
		return dustCascade(tTarget, tAmount, aUniverse);
	}

	/** Upstream UT.Code.bindStack (UT.java:1568) — the GT6RecipesShCL.bindStack transcription. */
	static int bindStack(long aBoundValue) {
		return (int)Math.max(1, Math.min(64, aBoundValue));
	}

	/** The GT6RecipesShCL.dustCascade ladder over the id universe (first branch with an existing item). */
	static Row.Out dustCascade(OreDictMaterial aMaterial, long aAmount, Map<OreDictPrefix, Set<OreDictMaterial>> aUniverse) {
		if (aAmount < CS.U72 || aMaterial == null) return null;
		Set<OreDictMaterial> tMats = aUniverse.get(OP.blockDust);
		if (aAmount >= CS.U * 72 && tMats != null && tMats.contains(aMaterial)) return new Row.Out(id(OP.blockDust, aMaterial), bindStack(aAmount / (CS.U * 9)), 0);
		tMats = aUniverse.get(OP.dust);
		if (aAmount >= CS.U && (aAmount >= CS.U * 16 || aAmount % CS.U == 0) && tMats != null && tMats.contains(aMaterial)) return new Row.Out(id(OP.dust, aMaterial), bindStack(aAmount / CS.U), 0);
		tMats = aUniverse.get(OP.dustSmall);
		if (aAmount >= CS.U4 && (aAmount >= CS.U * 8 || aAmount % CS.U4 <= aAmount % CS.U9) && tMats != null && tMats.contains(aMaterial)) return new Row.Out(id(OP.dustSmall, aMaterial), bindStack(aAmount * 4 / CS.U), 0);
		tMats = aUniverse.get(OP.dustTiny);
		if (aAmount >= CS.U9 && (aAmount >= CS.U || aAmount % CS.U9 <= aAmount % CS.U72) && tMats != null && tMats.contains(aMaterial)) return new Row.Out(id(OP.dustTiny, aMaterial), bindStack(aAmount * 9 / CS.U), 0);
		tMats = aUniverse.get(OP.dustDiv72);
		if (tMats != null && tMats.contains(aMaterial)) return new Row.Out(id(OP.dustDiv72, aMaterial), bindStack(aAmount * 72 / CS.U), 0);
		return null;
	}

	private static String id(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		return "gt6:" + GTMaterialItems.itemIdOf(aPrefix, aMaterial);
	}

	/** The full walk: 20 statements x their registered materials, upstream statement order x registration order. */
	public static List<Row> walkAll() {
		Map<OreDictPrefix, Set<OreDictMaterial>> tUniverse = universe();
		List<GTMaterialItems.PrefixMaterial> tOrder = GTMaterialItems.registrationOrder();
		List<Row> rRows = new ArrayList<>();
		for (Statement tSt : statements()) {
			Set<OreDictMaterial> tOutputs = tUniverse.get(tSt.outPrefix());
			for (GTMaterialItems.PrefixMaterial tPair : tOrder) {
				if (tPair.prefix() != tSt.inPrefix()) continue;
				OreDictMaterial tMat = tPair.material();
				if (!passesCondition(tSt, tMat)) continue;
				if (tOutputs == null || !tOutputs.contains(tMat)) continue; // :214 both-side
				long tUnitsIn = tSt.inPrefix().mAmount * tSt.inCount();
				long tUnitsOut = tSt.outPrefix().mAmount * tSt.outCount();
				long tDuration = Math.max(1, handlerCosts(tUnitsIn, tUnitsOut, tSt.multiplier(), tMat));
				List<Row.Out> tOuts = new ArrayList<>(2);
				tOuts.add(new Row.Out(id(tSt.outPrefix(), tMat), tSt.outCount(), 0));
				Row.Out tRemains = remainsOutput(tMat, tUnitsIn - tUnitsOut, tUniverse);
				if (tRemains != null) tOuts.add(tSt.chancesFace() ? new Row.Out(tRemains.id(), tRemains.count(), 7500) : tRemains);
				rRows.add(new Row(tSt, id(tSt.inPrefix(), tMat), tSt.inCount(), tOuts, tDuration));
			}
		}
		return rRows;
	}

	// ---------------------------------------------------------------------------
	// the fixtures
	// ---------------------------------------------------------------------------

	@BeforeEach
	void freshGeneration() {
		gregtech6.registry.GTMaterialItems.initMaterials();
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
		String tPath = "/data/gt6/recipe_maps/sharpening.json";
		InputStream tStream = GT6RecipeMapDataR11bSharpeningRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", "sharpening"), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The shipped rows split into the seated band (vanilla inputs) and the walk band. */
	private record Bands(List<JsonObject> seated, List<JsonObject> walkRows) {}
	private Bands bands(JsonArray aRows) {
		List<JsonObject> tSeated = new ArrayList<>(), tWalk = new ArrayList<>();
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			String tInput = tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
			if (SEATED_VANILLA_INPUTS.contains(tInput)) tSeated.add(tRow); else tWalk.add(tRow);
		}
		return new Bands(tSeated, tWalk);
	}

	// ---------------------------------------------------------------------------
	// the pins
	// ---------------------------------------------------------------------------

	/** The census: 3 seated + the walk ratchet pour with zero skips. */
	@Test
	public void theShippedFilePoursItsFullCensusWithZeroSkips() throws Exception {
		pourShipped();
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor("sharpening");
		assertNotNull(tMap);
		int tExpected = 3 + walkCensus();
		assertEquals(tExpected, tMap.mRecipeList.size(), "the map holds 3 seated + the walk census");
		assertEquals(tExpected, GT6RecipeMapJsonLoader.pouredCount("sharpening"),
				"the tracker mirrors the map (a smaller number = WARN-skipped rows)");
	}

	/** The three seated rows survive untouched ahead of the walk band. */
	@Test
	public void theThreeSeatedRowsSurvive() throws Exception {
		Bands tBands = bands(pourShipped());
		assertEquals(3, tBands.seated().size(), "the smoke sandstone row + the two B1 Vanilla rows");
	}

	/** The 20 statement groups all expand (every conversion family is alive), per-statement census ratchet. */
	@Test
	public void everyStatementGroupExpandsToItsCensus() throws Exception {
		Bands tBands = bands(pourShipped());
		Map<String, Integer> tCounts = new LinkedHashMap<>();
		for (JsonObject tRow : tBands.walkRows()) tCounts.merge(rowCite(tBands.walkRows(), tRow), 1, Integer::sum);
		for (Map.Entry<String, Integer> tPin : STATEMENT_CENSUS.entrySet()) {
			assertTrue(tPin.getValue() > 0, tPin.getKey() + " expands (the conversion family is alive)");
			assertEquals(tPin.getValue(), tCounts.getOrDefault(tPin.getKey(), 0).intValue(),
					tPin.getKey() + " row count ratchet");
		}
	}

	/** Recovers a row's statement cite by its input prefix family (the walk key order = the JSON order). */
	private String rowCite(List<JsonObject> aWalkRows, JsonObject aRow) {
		int tIndex = aWalkRows.indexOf(aRow);
		int tAcc = 0;
		for (Statement tSt : statements()) {
			tAcc += STATEMENT_CENSUS.get(tSt.cite());
			if (tIndex < tAcc) return tSt.cite();
		}
		throw new AssertionError("row beyond the census: " + aRow);
	}

	/** The live-walk replay: the shipped walk band equals the recomputed walk, row for row, in order. */
	@Test
	public void theWalkBandReplaysTheLiveRegistrationExactly() throws Exception {
		Bands tBands = bands(pourShipped());
		List<Row> tExpected = walkAll();
		assertEquals(tExpected.size(), tBands.walkRows().size(), "the walk band size");
		for (int i = 0; i < tExpected.size(); i++) {
			assertEquals(tExpected.get(i).key(), jsonRowKey(tBands.walkRows().get(i)),
					"walk row " + i + " (" + tExpected.get(i).st().cite() + " " + tExpected.get(i).inId() + ")");
		}
	}

	private static String jsonRowKey(JsonObject aRow) {
		JsonArray tOuts = aRow.getAsJsonArray("outputs");
		List<Row.Out> tOutList = new ArrayList<>(tOuts.size());
		for (JsonElement tElement : tOuts) {
			JsonObject tSlot = tElement.getAsJsonObject();
			tOutList.add(new Row.Out(tSlot.get("item").getAsString(), tSlot.has("count") ? tSlot.get("count").getAsInt() : 1,
					tSlot.has("chance") ? tSlot.get("chance").getAsLong() : 0));
		}
		JsonObject tIn = aRow.getAsJsonArray("inputs").get(0).getAsJsonObject();
		return new Row(null, tIn.get("item").getAsString(), tIn.has("count") ? tIn.get("count").getAsInt() : 1,
				tOutList, aRow.get("duration").getAsLong()).key();
	}

	/**
	 * Hand-computed formula anchors (independent of the walk code): the iron rows.
	 * iron mToolQuality 2, self-pulver — target 16x3 = 48: raw sword max(2U, 17U/9) →
	 * duration 2U*48/U = 96, surplus U/9 → 1x dust_tiny; raw arrow U8 → U9 duration 6,
	 * surplus U/72 → 1x dust_div72; raw double axe max(5U, 43U/9) → duration 240,
	 * surplus 2U/9 → 2x dust_tiny. (The card's "16EU/16t" shorthand is the EUt column
	 * plus the multiplier-16 getCosts arm — the per-material duration scales with
	 * mToolQuality, the :218 semantics.)
	 */
	@Test
	public void ironAnchorsAreHandComputed() throws Exception {
		Map<String, JsonObject> tByInput = new HashMap<>();
		for (JsonObject tRow : bands(pourShipped()).walkRows()) {
			tByInput.put(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString(), tRow);
		}
		JsonObject tSword = tByInput.get("gt6:tool_head_raw_sword_iron");
		assertNotNull(tSword, "the iron raw sword row exists");
		assertEquals(96, tSword.get("duration").getAsLong(), "max(2U, 17U/9) x 48 / U (iron q=2)");
		assertEquals(2, tSword.getAsJsonArray("outputs").size());
		assertEquals("gt6:tool_head_sword_iron", tSword.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals("gt6:dust_tiny_iron", tSword.getAsJsonArray("outputs").get(1).getAsJsonObject().get("item").getAsString(), "surplus U/9 -> 1 tiny");

		JsonObject tArrow = tByInput.get("gt6:tool_head_raw_arrow_iron");
		assertNotNull(tArrow, "the iron raw arrow row exists");
		assertEquals(6, tArrow.get("duration").getAsLong(), "max(U8, U9) x 48 / U = 6");
		assertEquals("gt6:dust_div72_iron", tArrow.getAsJsonArray("outputs").get(1).getAsJsonObject().get("item").getAsString(), "surplus U/72 -> 1 div72");

		JsonObject tAxeDouble = tByInput.get("gt6:tool_head_raw_axe_double_iron");
		assertNotNull(tAxeDouble, "the iron raw double axe row exists");
		assertEquals(240, tAxeDouble.get("duration").getAsLong(), "max(5U, 43U/9) x 48 / U = 240");
		assertEquals(2, tAxeDouble.getAsJsonArray("outputs").get(1).getAsJsonObject().get("count").getAsInt(), "surplus 2U/9 -> 2 tiny");
	}

	/** The chances face: misc rows chance their remains 7500; raw rows are deterministic; the first slot never carries a chance key. */
	@Test
	public void theChancesFaceMatchesTheHandlerArray() throws Exception {
		Bands tBands = bands(pourShipped());
		int tMiscRows = 0, tRemainsRows = 0, tRawRows = 0;
		for (int i = 0; i < tBands.walkRows().size(); i++) {
			JsonObject tRow = tBands.walkRows().get(i);
			String tCite = rowCite(tBands.walkRows(), tRow);
			JsonArray tOuts = tRow.getAsJsonArray("outputs");
			for (int s = 0; s < tOuts.size(); s++) {
				JsonObject tSlot = tOuts.get(s).getAsJsonObject();
				if (s == 0) assertTrue(!tSlot.has("chance"), tCite + " first output is 10000 = the no-key default");
			}
			boolean tRaw = tCite.compareTo(":403") >= 0;
			if (tRaw) {
				tRawRows++;
				for (int s = 0; s < tOuts.size(); s++) assertTrue(!tOuts.get(s).getAsJsonObject().has("chance"), tCite + " raw rows are chance-free");
			} else {
				tMiscRows++;
				if (tOuts.size() == 2) {
					assertEquals(7500, tOuts.get(1).getAsJsonObject().get("chance").getAsLong(), tCite + " remains chance");
					tRemainsRows++;
				} else {
					assertEquals(1, tOuts.size(), tCite + " unit-flat misc rows stay single-output");
				}
			}
		}
		assertTrue(tMiscRows > 0 && tRawRows > 0, "both bands walked");
		assertTrue(tRemainsRows > 0, "the remains face walked");
	}

	/** The live-id face: every gt6 id the file references is a member of the registration walk (the B1 pattern). */
	@Test
	public void everyGt6IdTheFileReferencesIsRegistered() throws Exception {
		Set<String> tUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		assertTrue(tUniverse.contains("gt6:tool_head_raw_sword_iron"), "the id universe built (" + tUniverse.size() + " ids)");
		Set<String> tMissing = new TreeSet<>();
		InputStream tStream = GT6RecipeMapDataR11bSharpeningRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/sharpening.json");
		assertNotNull(tStream);
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		for (JsonElement tElement : tDoc.getAsJsonArray("recipes")) {
			JsonObject tRow = tElement.getAsJsonObject();
			for (String tLeg : new String[] {"inputs", "outputs"}) {
				for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
					String tId = tSlot.getAsJsonObject().get("item").getAsString();
					if (tId.startsWith("gt6:") && !tUniverse.contains(tId)) tMissing.add(tId);
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 ids (these rows would WARN-skip live): " + tMissing);
	}
}
