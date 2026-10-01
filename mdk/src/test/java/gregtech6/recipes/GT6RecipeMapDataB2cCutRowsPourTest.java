package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The recipe-data-b2c-cut row-stock pour test (the B2c-generify posture): the Cutter
 * handler-walk static replay — the 20 statements of Loader_Recipes_Handlers.java:633-652
 * over the FOUR fluid legs of :628-632 — pours through the real {@link GT6RecipeMapJsonLoader}
 * seam with zero skips. RECOMPUTABILITY: the rows were generated from a one-shot live
 * registration dump (an uncommitted JUnit walking the GTMaterialItems ∪ GTMaterialBlocks
 * registration + the condition gates, emitted as TSV and folded by a /tmp generator — the
 * B2c-roll/generify zero-transcription method), and THIS test is the durable half of that
 * method: the live-walk cross-check recomputes the walk against the live registration and
 * fails the moment the frozen snapshot trails or outruns it. Per the scale ruling the walk
 * rows carry NO per-row comment (the cross-check IS their provenance); the file head carries
 * the DECLARED OVER-CAP statement (the upstream Cutter handler is inherently ten-thousand-scale:
 * 20 statements x 4 legs x every qualifying material). The LubRoCant fluid leg is a verbatim
 * faithful absence — the upstream loop bound is i<4 (:632), the fifth array slot (:628) never
 * walks. The RM.sawing scatter call sites are a separate card's scope and are asserted absent.
 */
public class GT6RecipeMapDataB2cCutRowsPourTest extends GTRecipesOfflineTestBase {

	private static final String FILE_KEY = "cutter";

	/** The frozen snapshot census: 20 statements x 4 legs = 5899 materials per leg. */
	private static final int CENSUS = 23596;
	/** Per-leg split of the census (every statement lands every leg). */
	private static final int PER_LEG = 5899;
	/** The four walked legs, verbatim :628-632 port ids + tMultiplier (:629). */
	private static final Set<String> LEG_FLUIDS = Set.of("minecraft:water", "gt6:spdew", "gt6:distilled_water", "gt6:lubricant");

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	// the walk tables — OP fields resolve lazily (OP.init runs with the material universe, so no
	// static captures: the GT6TmpCutWalkDump lesson)
	private static final String[][] LEGS = {
			{"minecraft:water", "4"},
			{"gt6:spdew", "4"},
			{"gt6:distilled_water", "3"},
			{"gt6:lubricant", "1"},
	};

	/** The walk prefixes: the 20 input faces + the 4 output-only faces, OP fields resolved lazily. */
	private static OreDictPrefix[] prefixes() {
		return new OreDictPrefix[] {OP.blockSolid, OP.stickLong, OP.stick, OP.plate, OP.plateGem, OP.gemChipped,
				OP.gemFlawed, OP.gem, OP.gemFlawless, OP.gemExquisite, OP.gemLegendary, OP.bouleGt,
				OP.ingotDouble, OP.ingotTriple, OP.ingotQuadruple, OP.ingotQuintuple,
				OP.plateDouble, OP.plateTriple, OP.plateQuadruple, OP.plateQuintuple,
				OP.plateTiny, OP.plateGemTiny, OP.ingot, OP.bolt};
	}

	/** The 20 statements: upstream line, input prefix idx, K, eut, output prefix idx, outCount — :633-652 in order. */
	private static long[][] stmts() {
		return new long[][] {
				{633, 0, 7, 32, 3, 8}, {634, 1, 1, 32, 2, 2}, {635, 2, 3, 32, 23, 4}, {636, 3, 4, 32, 20, 8},
				{637, 4, 4, 32, 21, 8}, {638, 5, 1, 32, 21, 2}, {639, 6, 2, 32, 21, 4}, {640, 7, 1, 96, 4, 1},
				{641, 8, 1, 96, 4, 2}, {642, 9, 3, 96, 4, 4}, {643, 10, 7, 96, 4, 8}, {644, 11, 3, 32, 4, 4},
				{645, 12, 1, 32, 22, 2}, {646, 13, 2, 32, 22, 3}, {647, 14, 3, 32, 22, 4}, {648, 15, 4, 32, 22, 5},
				{649, 16, 1, 32, 3, 2}, {650, 17, 2, 32, 3, 3}, {651, 18, 3, 32, 3, 4}, {652, 19, 4, 32, 3, 5}};
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
		String tPath = "/data/gt6/recipe_maps/" + FILE_KEY + ".json";
		InputStream tStream = GT6RecipeMapDataB2cCutRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", FILE_KEY), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: the file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void theCutterFilePoursItsFullCensusWithZeroSkips() throws Exception {
		pourShipped();
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(FILE_KEY);
		assertNotNull(tMap, FILE_KEY + " resolves");
		assertEquals(CENSUS, tMap.mRecipeList.size(), FILE_KEY + ": the map holds the census");
		assertEquals(CENSUS, GT6RecipeMapJsonLoader.pouredCount(FILE_KEY),
				FILE_KEY + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
	}

	/** The file-head declaration pins: deviation + frozen + walk provenance + over-cap + smoke row + the LubRoCant absence. */
	@Test
	public void theFileHeadKeepsItsDeclarations() throws Exception {
		InputStream tStream = GT6RecipeMapDataB2cCutRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + FILE_KEY + ".json");
		assertNotNull(tStream);
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		assertTrue(tDoc.has("comment"), FILE_KEY + ": the file head carries a comment member");
		String tComment = tDoc.get("comment").getAsString();
		String tLower = tComment.toLowerCase(java.util.Locale.ROOT);
		assertTrue(tLower.contains("declared deviation"), "the deviation class is declared");
		assertTrue(tLower.contains("frozen"), "the declaration names the frozen-universe semantics");
		assertTrue(tComment.contains("Loader_Recipes_Handlers.java:633-652"), "the declaration cites the 20 handler statements");
		assertTrue(tComment.contains(":628-632"), "the declaration cites the fluid-leg loop");
		assertTrue(tLower.contains("lubrocant") && tComment.contains("i<4"), "the LubRoCant verbatim absence (loop bound i<4) is declared");
		assertTrue(tLower.contains("smoke row"), "the declaration names the replaced smoke row");
		assertTrue(tLower.contains("declared over-cap"), "the declaration names the over-cap ruling");
		assertTrue(tLower.contains("walk row provenance"), "the declaration names the walk-row provenance ruling");
		assertTrue(tLower.contains("sawing"), "the declaration disclaims the sawing scatter sites (another card's scope)");
	}

	/** The walk/static split: every row is an uncommented walk row (the generify scale ruling). */
	@Test
	public void theWalkRowsCarryNoCommentAndMatchTheirSnapshotCensus() throws Exception {
		int tUncommented = 0, tCommented = 0;
		for (JsonElement tElement : pourShipped()) {
			if (tElement.getAsJsonObject().has("comment")) tCommented++;
			else tUncommented++;
		}
		assertEquals(CENSUS, tUncommented, FILE_KEY + ": the walk-row snapshot census");
		assertEquals(0, tCommented, FILE_KEY + ": no per-row comments at the ten-thousand-row scale");
	}

	/** The 4-leg accounting: the census splits evenly over exactly the four :628-632 legs — LubRoCant never walks. */
	@Test
	public void theFourFluidLegsSplitTheCensusAndLubRoCantNeverWalks() throws Exception {
		Map<String, Integer> tByFluid = new java.util.HashMap<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			assertTrue(tRow.has("fluidInputs") && tRow.getAsJsonArray("fluidInputs").size() == 1,
					"every cutter walk row carries exactly one fluid input");
			String tFluid = tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString();
			tByFluid.merge(tFluid, 1, Integer::sum);
		}
		assertEquals(LEG_FLUIDS, tByFluid.keySet(), "exactly the four walked legs (the LubRoCant slot-5 fluid is a verbatim faithful absence)");
		for (String tFluid : LEG_FLUIDS) {
			assertEquals(PER_LEG, tByFluid.get(tFluid).intValue(), tFluid + " carries its leg census");
		}
	}

	/**
	 * Verbatim spot checks — twelve rows across the statement/leg matrix, pinned to the full row:
	 * the five remains-carrying statements (dust x1 / dustTiny x1 / dustDiv72 x2/x4) with the live
	 * mTargetPulver remap (wrought-iron-family and compressed-iron cuts yield iron dust), the
	 * gem-family 96 EU tier, the bouleGt force-table face, and the per-leg tMultiplier lattice
	 * (4/4/3/1) x the mToolQuality+1 fluid scaling of RecipeMapHandlerPrefix.java:218.
	 */
	@Test
	public void statementLegsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped();

		// :633 blockSolid -> plate x8 + remains dust x1, Water leg (tMult 4: 4*7*16 = 448), lithium q=0
		JsonObject tRow = findRow(tRows, "gt6:block_solid_lithium", "minecraft:water");
		assertRow(tRow, "gt6:plate_lithium", 8, "gt6:dust_lithium", 1, 448, 448, 32);
		// :633 WroughtIron DistW leg (tMult 3: 336) x quality 2 -> 1008 mB; the remains remap: Fe dust
		tRow = findRow(tRows, "gt6:block_solid_wrought_iron", "gt6:distilled_water");
		assertRow(tRow, "gt6:plate_wrought_iron", 8, "gt6:dust_iron", 1, 1008, 336, 32);
		// :634 stickLong -> stick x2, Water leg, iron q=2 (64*3 = 192 mB), no remains (2U -> 2U)
		tRow = findRow(tRows, "gt6:stick_long_iron", "minecraft:water");
		assertRow(tRow, "gt6:stick_iron", 2, null, 0, 192, 64, 32);
		// :636 plate -> plateTiny x8 + remains dustTiny x1, SpDew leg (tMult 4: 256), beryllium q=2
		tRow = findRow(tRows, "gt6:plate_beryllium", "gt6:spdew");
		assertRow(tRow, "gt6:plate_tiny_beryllium", 8, "gt6:dust_tiny_beryllium", 1, 768, 256, 32);
		// :636 IronCompressed (the setPulver(Fe) remap face), Water leg, q=2
		tRow = findRow(tRows, "gt6:plate_iron_compressed", "minecraft:water");
		assertRow(tRow, "gt6:plate_tiny_iron_compressed", 8, "gt6:dust_tiny_iron", 1, 768, 256, 32);
		// :637 plateGem -> plateGemTiny x8 + remains dustTiny x1, Water leg (256), diamond q=3 -> 1024
		tRow = findRow(tRows, "gt6:plate_gem_diamond", "minecraft:water");
		assertRow(tRow, "gt6:plate_gem_tiny_diamond", 8, "gt6:dust_tiny_diamond", 1, 1024, 256, 32);
		// :638 gemChipped -> plateGemTiny x2 + remains dustDiv72 x2 (U4 - 2xU9 = U36), glass q=0
		tRow = findRow(tRows, "gt6:gem_chipped_glass", "minecraft:water");
		assertRow(tRow, "gt6:plate_gem_tiny_glass", 2, "gt6:dust_div72_glass", 2, 64, 64, 32);
		// :639 gemFlawed -> plateGemTiny x4 + remains dustDiv72 x4 (U2 - 4xU9 = U18), ruby q=3
		tRow = findRow(tRows, "gt6:gem_flawed_ruby", "minecraft:water");
		assertRow(tRow, "gt6:plate_gem_tiny_ruby", 4, "gt6:dust_div72_ruby", 4, 512, 128, 32);
		// :640 gem -> plateGem, the 96 EU tier (:640-643), Water leg, diamond q=3
		tRow = findRow(tRows, "gt6:gem_diamond", "minecraft:water");
		assertRow(tRow, "gt6:plate_gem_diamond", 1, null, 0, 256, 64, 96);
		// :644 bouleGt -> plateGem x4 (the port force-table face), silicon q=0, Water leg (3*16=192)
		tRow = findRow(tRows, "gt6:boule_gt_silicon", "minecraft:water");
		assertRow(tRow, "gt6:plate_gem_silicon", 4, null, 0, 192, 192, 32);
		// :645 ingotDouble -> ingot x2, SpDew leg, wrought iron q=2, no remains (2U -> 2U)
		tRow = findRow(tRows, "gt6:ingot_double_wrought_iron", "gt6:spdew");
		assertRow(tRow, "gt6:ingot_wrought_iron", 2, null, 0, 192, 64, 32);
		// :652 plateQuintuple -> plate x5, Lubricant leg (tMult 1: 64), steel q=2
		tRow = findRow(tRows, "gt6:plate_quintuple_steel", "gt6:lubricant");
		assertRow(tRow, "gt6:plate_steel", 5, null, 0, 192, 64, 32);
	}

	/** Full-row pin: main output (count), optional remains output, duration, eut, fluid amount. */
	private static void assertRow(JsonObject aRow, String aOut, int aOutCount, String aRemains, int aRemainsCount,
			int aFluidAmount, int aDuration, int aEut) {
		JsonArray tOuts = aRow.getAsJsonArray("outputs");
		assertEquals(aOut, tOuts.get(0).getAsJsonObject().get("item").getAsString(), "the main output id");
		assertEquals(aOutCount, tOuts.get(0).getAsJsonObject().get("count").getAsInt(), "the main output count");
		assertEquals(aRemains != null ? 2 : 1, tOuts.size(), "the output slot census (main [+ pulverized remains])");
		if (aRemains != null) {
			assertEquals(aRemains, tOuts.get(1).getAsJsonObject().get("item").getAsString(), "the pulverized-remains id");
			assertEquals(aRemainsCount, tOuts.get(1).getAsJsonObject().get("count").getAsInt(), "the pulverized-remains count");
		}
		assertEquals(aFluidAmount, aRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				"the fluid amount (tMultiplier*K*16 x (mToolQuality+1), HandlerPrefix.java:218)");
		assertEquals(aDuration, aRow.get("duration").getAsLong(), "the fixed mDuration (the mDuration>0 arm — no quality scaling)");
		assertEquals(aEut, aRow.get("eut").getAsLong(), "the fixed statement eut");
	}

	/**
	 * The live-walk cross-check — the heart of the frozen-snapshot deviation: recompute the whole
	 * walk (20 statements x 4 legs over the LIVE GTMaterialItems ∪ GTMaterialBlocks registration,
	 * the RecipeMapHandlerPrefix gates: tConditionM :630 + INVALID_MATERIAL :205, item existence
	 * :209/:214, fluid x(mToolQuality+1) :218, the remains arm :79/:215 + OM.pulverize :370-372 +
	 * the OM.dust ladder :460-467) and require the shipped rows to cover the recomputation
	 * EXACTLY, both directions.
	 */
	@Test
	public void theShippedWalkMatchesTheLiveRegistrationWalk() throws Exception {
		GTMaterialItems.initMaterials();
		OreDictPrefix[] tPrefixes = prefixes();
		Set<String> tRegKeys = new HashSet<>();
		List<GTMaterialItems.PrefixMaterial> tOrder = new ArrayList<>(GTMaterialItems.registrationOrder());
		tOrder.addAll(GTMaterialBlocks.registrationOrder());
		for (GTMaterialItems.PrefixMaterial tPair : tOrder) {
			tRegKeys.add(tPair.prefix().mNameInternal + "|" + tPair.material().mNameInternal);
		}
		Set<String> tExpected = new HashSet<>();
		for (long[] tStmt : stmts()) {
			OreDictPrefix tIn = tPrefixes[(int) tStmt[1]], tOut = tPrefixes[(int) tStmt[4]];
			long tK = tStmt[2], tEut = tStmt[3], tOutCount = tStmt[5];
			long tUnitsIn = tIn.mAmount, tUnitsOut = tOut.mAmount * tOutCount;
			boolean tRemains = tUnitsIn - tUnitsOut >= OP.dustDiv72.mAmount; // HandlerPrefix :79
			for (String[] tLeg : LEGS) {
				long tBase = Long.parseLong(tLeg[1]) * tK * 16; // duration + base fluid mB
				for (OreDictMaterial tMat : materialsOf(tIn, tOrder)) {
					if (!tRegKeys.contains(tIn.mNameInternal + "|" + tMat.mNameInternal)) continue; // input gate :209
					if (!tRegKeys.contains(tOut.mNameInternal + "|" + tMat.mNameInternal)) continue; // output gate :214
					if (tMat.contains(TD.Atomic.ANTIMATTER) || tMat.contains(TD.Compounds.COATED)) continue; // tConditionM :630
					if (tMat.contains(TD.Properties.INVALID_MATERIAL)) continue; // :205
					String tRem = "";
					if (tRemains) {
						String tSlot = remainsKey(tMat, tUnitsIn - tUnitsOut, tRegKeys);
						if (!tSlot.isEmpty()) tRem = ">" + tSlot; // an empty slot = the :895 trailing-null trim (no key face)
					}
					tExpected.add(key(GTMaterialItems.itemIdOf(tIn, tMat), tLeg[0], tBase * (tMat.mToolQuality + 1),
							tBase, tEut, GTMaterialItems.itemIdOf(tOut, tMat), tOutCount, tRem));
				}
			}
		}
		Set<String> tShipped = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			StringBuilder tOutputs = new StringBuilder();
			for (JsonElement tOut : tRow.getAsJsonArray("outputs")) {
				JsonObject tSlot = tOut.getAsJsonObject();
				tOutputs.append('>').append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsInt());
			}
			JsonObject tFluid = tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject();
			tShipped.add(key(itemId(tRow.getAsJsonArray("inputs").get(0)), tFluid.get("fluid").getAsString(),
					tFluid.get("amount").getAsInt(), tRow.get("duration").getAsLong(), tRow.get("eut").getAsLong(), tOutputs.toString()));
		}
		Set<String> tMissing = new HashSet<>(tExpected);
		tMissing.removeAll(tShipped);
		Set<String> tStale = new HashSet<>(tShipped);
		tStale.removeAll(tExpected);
		assertTrue(tMissing.isEmpty(), "the frozen snapshot trails the live walk (re-pour needed): " + tMissing);
		assertTrue(tStale.isEmpty(), "the frozen snapshot holds walk rows the live walk no longer generates: " + tStale);
		assertEquals(tExpected.size(), tShipped.size(), "the snapshot size equals the live walk");
	}

	private static String key(String aInPath, String aFluid, long aFluidAmount, long aDuration, long aEut, String aOutputsTail) {
		return "gt6:" + aInPath + ">" + aFluid + "@" + aFluidAmount + ">" + aDuration + ">" + aEut + aOutputsTail;
	}

	private static String key(String aInPath, String aFluid, long aFluidAmount, long aDuration, long aEut,
			String aOutPath, long aOutCount, String aRemainsTail) {
		return key(aInPath, aFluid, aFluidAmount, aDuration, aEut, ">gt6:" + aOutPath + ":" + aOutCount + aRemainsTail);
	}

	private static String itemId(JsonElement aSlot) {
		return aSlot.getAsJsonObject().get("item").getAsString().substring("gt6:".length());
	}

	/** The fluid-universe face: the gt6 leg ids resolve in the GTFluids spec tables (the b2b2 shape). */
	@Test
	public void everyFluidIdTheFileReferencesIsRegistered() throws Exception {
		Set<String> tMissing = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			for (JsonElement tSlot : tRow.getAsJsonArray("fluidInputs")) {
				String tId = tSlot.getAsJsonObject().get("fluid").getAsString();
				if (tId.equals("minecraft:water")) continue; // the vanilla carrier resolves live (the generify precedent)
				if (!fluidRegistered(tId.substring("gt6:".length()))) tMissing.add(tId);
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 fluid ids (these rows would WARN-skip live): " + tMissing);
	}

	/** The offline fluid-universe lookup: the union of the GTFluids spec-table lookups (the b2b2 shape). */
	private static boolean fluidRegistered(String aPath) {
		return gregtech6.fluid.GTFluids.aquaSpec(aPath) != null || gregtech6.fluid.GTFluids.engineSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.chemicalSpec(aPath) != null || gregtech6.fluid.GTFluids.simpleLiquidSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.lubricantSpec(aPath) != null;
	}

	/** The id faces: every gt6 ITEM id lives in GTMaterialItems ∪ GTMaterialBlocks (the blockSolid input face). */
	@Test
	public void everyItemIdTheFileReferencesIsRegistered() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tItemUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tItemUniverse.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			tItemUniverse.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		Set<String> tMissing = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			for (String tLeg : new String[] {"inputs", "outputs"}) {
				for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
					String tId = tSlot.getAsJsonObject().get("item").getAsString();
					if (tId.startsWith("gt6:") && !tItemUniverse.contains(tId.substring(4))) tMissing.add(tId);
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 item ids (these rows would WARN-skip live): " + tMissing);
	}

	// ------------------------------------------------------------- the walk engine (RecipeMapHandlerPrefix + OM semantics)

	private static List<OreDictMaterial> materialsOf(OreDictPrefix aPrefix, List<GTMaterialItems.PrefixMaterial> aOrder) {
		Set<String> tSeen = new HashSet<>();
		List<OreDictMaterial> tMats = new ArrayList<>();
		for (GTMaterialItems.PrefixMaterial tPair : aOrder) {
			if (tPair.prefix() != aPrefix) continue;
			if (tSeen.add(tPair.material().mNameInternal)) tMats.add(tPair.material());
		}
		return tMats;
	}

	/** OM.pulverize :370-372 + the OM.dust ladder :460-467, serialized as the key face ">gt6:id:count";
	 * an unregistered final-arm slot = the Recipe.java:895 trailing-null trim (the row keeps, the slot goes). */
	private static String remainsKey(OreDictMaterial aMat, long aRemainsAmt, Set<String> aRegKeys) {
		OreDictMaterial tTarget = aMat.mTargetPulver.mMaterial;
		long tDustAmt = aRemainsAmt * aMat.mTargetPulver.mAmount / CS.U; // UT.Code.units(..., F)
		if (tDustAmt < CS.U72) return "";
		if (tDustAmt >= CS.U * 72 && has(OP.blockDust, tTarget, aRegKeys)) return remKey(OP.blockDust, tTarget, tDustAmt / (CS.U * 9));
		if (tDustAmt >= CS.U && (tDustAmt >= CS.U * 16 || tDustAmt % CS.U == 0) && has(OP.dust, tTarget, aRegKeys)) return remKey(OP.dust, tTarget, tDustAmt / CS.U);
		if (tDustAmt >= CS.U4 && (tDustAmt >= CS.U * 8 || tDustAmt % CS.U4 <= tDustAmt % CS.U9) && has(OP.dustSmall, tTarget, aRegKeys)) return remKey(OP.dustSmall, tTarget, (tDustAmt * 4) / CS.U);
		if (tDustAmt >= CS.U9 && (tDustAmt >= CS.U || tDustAmt % CS.U9 <= tDustAmt % CS.U72) && has(OP.dustTiny, tTarget, aRegKeys)) return remKey(OP.dustTiny, tTarget, (tDustAmt * 9) / CS.U);
		if (has(OP.dustDiv72, tTarget, aRegKeys)) return remKey(OP.dustDiv72, tTarget, (tDustAmt * 72) / CS.U);
		return "";
	}

	private static boolean has(OreDictPrefix aPrefix, OreDictMaterial aMat, Set<String> aRegKeys) {
		return aRegKeys.contains(aPrefix.mNameInternal + "|" + aMat.mNameInternal);
	}

	private static String remKey(OreDictPrefix aPrefix, OreDictMaterial aMat, long aCount) {
		long tCount = Math.max(1, Math.min(64, aCount)); // UT.Code.bindStack
		return "gt6:" + GTMaterialItems.itemIdOf(aPrefix, aMat) + ":" + tCount;
	}

	/** Finds the row with the exact input item + fluid pair. */
	private static JsonObject findRow(JsonArray aRows, String aInputItem, String aFluid) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("inputs") || !tRow.has("fluidInputs")) continue;
			if (aInputItem.equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())
					&& aFluid.equals(tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString())) return tRow;
		}
		throw new AssertionError("no row " + aInputItem + " on " + aFluid);
	}
}
