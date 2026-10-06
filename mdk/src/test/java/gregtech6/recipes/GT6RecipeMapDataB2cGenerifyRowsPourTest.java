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

import gregapi.data.ANY;
import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.UT;
import gregtech6.block.tree.GT6BeamKind;
import gregtech6.registry.GT6BeamBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The recipe-data-b2c-generify row-stock pour test (the B2c-roll posture, scaled + the
 * weld-casing-increment re-increment + the beam-consume-increment beam face): the two
 * handler-expansion map files of this card — generifier (9123 rows: 8818 walk + 238 stone
 * family + 39 static items + 15 fluids + the 128 weld-casing-increment casing rows + the
 * 7 beam rows of task beam-consume-increment, Loader_Recipes_Woods.java:190 — the port beam
 * universe generifying into the IL.Beam target gt6:wood_beam; the :66 IL.Beam self-row is a
 * declared degenerate skip, the handler mTargetGenerifying != self gate canon) and polarizer
 * (887) —
 * pour through the real {@link GT6RecipeMapJsonLoader} seam with zero skips. RECOMPUTABILITY:
 * the rows were generated from a one-shot live registration dump (an uncommitted JUnit walking
 * GTMaterialItems.registrationOrder + the condition gates, emitted as TSV and folded by a
 * /tmp generator — the B2c-roll zero-transcription method), and THIS test is the durable
 * half of that method: the live-walk cross-checks recompute the handler walks against the
 * live registration and fail the moment the frozen snapshot trails or outruns it. Per the
 * scale ruling, the walk rows carry NO per-row comment (the cross-check IS their provenance);
 * the static direct-call rows keep their per-row citations; both file heads carry the
 * DECLARED OVER-CAP statement (the upstream Generifier/Polarizer configs are inherently
 * ten-thousand-scale — the handler expands every qualifying prefix of every material).
 */
public class GT6RecipeMapDataB2cGenerifyRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The per-map census of this card: file key -> expected total rows (zero skips). */
	private static final Map<String, Integer> CENSUS = Map.of(
			"generifier", 9123, // 8818 walk + 238 stone family + 39 static items + 15 fluids + 7 beam + 6 qol stripped (+128: the weld-casing-increment re-increment, +7: the beam-consume-increment beam face, +6: the stripped-log-qol stripped-beam rows; −64: task wiregt-prefix-item-retirement — the sixteen wireGt multipliers x the 4 polymer generifying faces left with the retired wire items, the live-walk re-pour)
			"polarizer", 887);  // 76 Nd + 583 Fe-walk + 228 Steel-walk (+44: the weld-casing-increment re-increment)

	/** The uncommented walk rows per map — the frozen walk snapshot sizes. */
	private static final Map<String, Integer> WALK_CENSUS = Map.of(
			"generifier", 8818,
			"polarizer", 887);

	/** The static direct-call rows keep their per-row upstream citation; these are their counts. */
	private static final Map<String, Integer> STATIC_CITES = Map.ofEntries(
			Map.entry("Loader_Recipes_Vanilla.java:344", 1), Map.entry("Loader_Recipes_Vanilla.java:345", 1),
			Map.entry("Loader_Recipes_Vanilla.java:351", 1), Map.entry("Loader_Recipes_Vanilla.java:352", 1),
			Map.entry("Loader_Recipes_Vanilla.java:357", 7), Map.entry("Loader_Recipes_Vanilla.java:358", 7),
			Map.entry("Loader_Recipes_Vanilla.java:365", 1), Map.entry("Loader_Recipes_Vanilla.java:366", 1),
			Map.entry("Loader_Recipes_Vanilla.java:576", 1), Map.entry("Loader_Recipes_Vanilla.java:975", 17),
			Map.entry("BlockGrass.java:72", 1),
			Map.entry("BlockStones.java:282", 17), Map.entry("BlockStones.java:324", 17), Map.entry("BlockStones.java:367", 17),
			Map.entry("BlockStones.java:378", 17), Map.entry("BlockStones.java:391", 17), Map.entry("BlockStones.java:400", 17),
			Map.entry("BlockStones.java:409", 17), Map.entry("BlockStones.java:417", 17), Map.entry("BlockStones.java:443", 17),
			Map.entry("BlockStones.java:452", 17), Map.entry("BlockStones.java:460", 17), Map.entry("BlockStones.java:468", 17),
			Map.entry("BlockStones.java:477", 17), Map.entry("BlockStones.java:486", 17),
			Map.entry("Loader_Recipes_Woods.java:190", 7), // task beam-consume-increment: the 7 port beams -> gt6:wood_beam
			Map.entry("task stripped-log-qol", 6), // the stripped-log beam-ification QoL rows (task stripped-log-qol)
			Map.entry("Loader_Recipes_Temporary.java:674", 1), Map.entry("Loader_Recipes_Temporary.java:675", 1),
			Map.entry("Loader_Recipes_Temporary.java:676", 1), Map.entry("Loader_Recipes_Temporary.java:677", 1),
			Map.entry("Loader_Recipes_Temporary.java:678", 1), Map.entry("Loader_Recipes_Temporary.java:680", 1),
			Map.entry("Loader_Recipes_Temporary.java:681", 1), Map.entry("Loader_Recipes_Temporary.java:682", 1),
			Map.entry("Loader_Recipes_Temporary.java:683", 1), Map.entry("Loader_Recipes_Temporary.java:684", 1),
			Map.entry("Loader_Recipes_Temporary.java:704", 1), Map.entry("Loader_Recipes_Temporary.java:705", 1),
			Map.entry("Loader_Recipes_Temporary.java:706", 1), Map.entry("Loader_Recipes_Temporary.java:707", 1),
			Map.entry("Loader_Recipes_Temporary.java:708", 1));

	/** The file-head declarations: the deviation class + the over-cap ruling + the replaced smoke rows. */
	private static final Map<String, String> DECLARATIONS = Map.of(
			"generifier", "DECLARED DEVIATION",
			"polarizer", "DECLARED DEVIATION");

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
		InputStream tStream = GT6RecipeMapDataB2cGenerifyRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: every file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void everyB2cGenerifyFilePoursItsFullCensusWithZeroSkips() throws Exception {
		for (String tKey : CENSUS.keySet()) {
			pourShipped(tKey);
			RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(tKey);
			assertNotNull(tMap, tKey + " resolves");
			assertEquals(CENSUS.get(tKey).intValue(), tMap.mRecipeList.size(), tKey + ": the map holds the census");
			assertEquals(CENSUS.get(tKey).intValue(), GT6RecipeMapJsonLoader.pouredCount(tKey),
					tKey + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
		}
	}

	/** The declaration shape pin: deviation + frozen-universe + walk rows + over-cap + smoke row. */
	@Test
	public void theFileHeadsKeepTheirDeclarationComments() throws Exception {
		for (String tKey : DECLARATIONS.keySet()) {
			InputStream tStream = GT6RecipeMapDataB2cGenerifyRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			assertTrue(tDoc.has("comment"), tKey + ": the file head carries a comment member");
			String tComment = tDoc.get("comment").getAsString();
			String tLower = tComment.toLowerCase(java.util.Locale.ROOT);
			assertTrue(tLower.contains(DECLARATIONS.get(tKey).toLowerCase(java.util.Locale.ROOT)),
					tKey + ": the declaration names " + DECLARATIONS.get(tKey));
			assertTrue(tLower.contains("frozen"), tKey + ": the declaration names the frozen-universe semantics");
			assertTrue(tComment.contains("Loader_Recipes_Handlers.java:"), tKey + ": the declaration cites the handler rows");
			assertTrue(tLower.contains("smoke row"), tKey + ": the declaration names the replaced smoke row");
			assertTrue(tLower.contains("declared over-cap"), tKey + ": the declaration names the over-cap ruling");
			assertTrue(tLower.contains("walk row provenance"), tKey + ": the declaration names the walk-row provenance ruling");
		}
	}

	/** The walk/static split: the uncommented rows match the walk snapshot size exactly. */
	@Test
	public void theWalkRowsCarryNoCommentAndMatchTheirSnapshotCensus() throws Exception {
		for (String tKey : WALK_CENSUS.keySet()) {
			int tUncommented = 0, tCommented = 0;
			for (JsonElement tElement : pourShipped(tKey)) {
				if (tElement.getAsJsonObject().has("comment")) tCommented++;
				else tUncommented++;
			}
			assertEquals(WALK_CENSUS.get(tKey).intValue(), tUncommented, tKey + ": the walk-row snapshot census");
			assertEquals(CENSUS.get(tKey).intValue() - WALK_CENSUS.get(tKey).intValue(), tCommented,
					tKey + ": the static-row census (every static row cited)");
		}
	}

	/** The per-citation census: each upstream direct-call statement lands its expected row count. */
	@Test
	public void theStaticCitationsCarryTheirCensus() throws Exception {
		JsonArray tRows = pourShipped("generifier");
		for (Map.Entry<String, Integer> tCite : STATIC_CITES.entrySet()) {
			int tCount = 0;
			for (JsonElement tElement : tRows) {
				JsonObject tRow = tElement.getAsJsonObject();
				if (tRow.has("comment") && tRow.get("comment").getAsString().startsWith(tCite.getKey() + " ")) tCount++;
			}
			assertEquals(tCite.getValue().intValue(), tCount, "the " + tCite.getKey() + " citation census");
		}
	}

	/**
	 * Spot checks — the walk: the magnetic pair BOTH ways (IronMagnetic -> Fe on the generifier,
	 * Fe -> IronMagnetic on the polarizer) plus the duration scaling proof. The generifier walk
	 * duration is max(1, units(prefix.mAmount, U, 1, roundUp)) — 1 for U-prefixes, 2 for the
	 * double ingot; the polarizer scales mAmount*144/U — 144 for the plain ingot.
	 */
	@Test
	public void magneticPairsAreUpstreamVerbatimOnBothMaps() throws Exception {
		JsonObject tGen = findRow(pourShipped("generifier"), "gt6:ingot_iron_magnetic", "gt6:ingot_iron");
		assertEquals(1, tGen.get("duration").getAsLong(), "generifier: U-prefix duration = 1");
		assertEquals(0, tGen.get("eut").getAsLong(), "generifier walk eut = 0");
		JsonObject tGenDouble = findRow(pourShipped("generifier"), "gt6:ingot_double_iron_magnetic", "gt6:ingot_double_iron");
		assertEquals(2, tGenDouble.get("duration").getAsLong(), "generifier: ingotDouble 2U -> duration 2");
		JsonObject tPol = findRow(pourShipped("polarizer"), "gt6:ingot_iron", "gt6:ingot_iron_magnetic");
		assertEquals(144, tPol.get("duration").getAsLong(), "polarizer: ingot U*144/U = 144");
		assertEquals(16, tPol.get("eut").getAsLong(), "polarizer Fe-walk eut = 16");
		JsonObject tNd = findRow(pourShipped("polarizer"), "gt6:ingot_neodymium", "gt6:ingot_neodymium_magnetic");
		assertEquals(144, tNd.get("duration").getAsLong());
		assertEquals(128, tNd.get("eut").getAsLong(), "polarizer Nd statement eut = 128");
		JsonObject tSteel = findRow(pourShipped("polarizer"), "gt6:ingot_steel", "gt6:ingot_steel_magnetic");
		assertEquals(16, tSteel.get("eut").getAsLong(), "polarizer Steel-walk eut = 16");
		// the casing re-increment — the casingMachine* items joined BOTH walks (casing-machine-register unlock)
		JsonObject tGenCasing = findRow(pourShipped("generifier"), "gt6:casing_machine_quadruple_iron_compressed", "gt6:casing_machine_quadruple_iron");
		assertEquals(26, tGenCasing.get("duration").getAsLong(), "generifier: casingMachineQuadruple 26U -> duration 26, eut 0");
		assertEquals(0, tGenCasing.get("eut").getAsLong(), "generifier walk eut = 0 on the casing face too");
		JsonObject tPolCasing = findRow(pourShipped("polarizer"), "gt6:casing_machine_steel", "gt6:casing_machine_steel_magnetic");
		assertEquals(1152, tPolCasing.get("duration").getAsLong(), "polarizer: casingMachine 8U * 144/U = 1152");
		assertEquals(16, tPolCasing.get("eut").getAsLong(), "polarizer casing face rides the ANY.Steel walk eut = 16");
	}

	/** Spot checks — the static face: vanilla charcoal, a fiber, a stone family row, a fluid row. */
	@Test
	public void staticDirectRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("generifier");
		JsonObject tCharcoal = findRow(tRows, "gt6:ingot_charcoal", "minecraft:charcoal");
		assertTrue(tCharcoal.get("comment").getAsString().startsWith("Loader_Recipes_Vanilla.java:344"),
				"the charcoal row cites its direct-call statement");
		JsonObject tFiber = findRow(tRows, "gt6:plant_gt_fiber_black", "minecraft:string");
		assertTrue(tFiber.get("comment").getAsString().startsWith("Loader_Recipes_Vanilla.java:975"));
		JsonObject tStone = findRow(tRows, "gt6:basalt", "minecraft:stone");
		assertTrue(tStone.get("comment").getAsString().startsWith("BlockStones.java:282"),
				"the stone family rows cite their BlockStones form statements");
		JsonObject tHoney = findFluidRow(tRows, "gt6:royal_jelly");
		assertEquals(10, tHoney.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				"royal jelly -> honey x10 verbatim");
		assertTrue(tHoney.get("comment").getAsString().startsWith("Loader_Recipes_Temporary.java:707"));
		JsonObject tWater = findFluidRow(tRows, "gt6:distilled_water");
		assertEquals("minecraft:water", slotId(tWater.getAsJsonArray("fluidOutputs").get(0)),
				"the water family drains into the vanilla carrier");
	}

	/**
	 * The live-walk cross-check — the heart of the frozen-snapshot deviation: recompute BOTH
	 * handler walks against the LIVE registration (the RecipeMapHandlerMaterial.addRecipeForPrefix
	 * gates: the prefix condition :128, mAmount > 0, input/output item existence; the walk
	 * universe = the registration order grouped per material) and require the shipped walk
	 * rows (the uncommented ones) to cover the recomputation EXACTLY, both directions.
	 */
	@Test
	public void theShippedWalkMatchesTheLiveRegistrationWalk() throws Exception {
		GTMaterialItems.initMaterials();
		Map<OreDictMaterial, Set<OreDictPrefix>> tByMat = new java.util.HashMap<>();
		Set<String> tRegKeys = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tByMat.computeIfAbsent(tPair.material(), aM -> new java.util.TreeSet<>((a, b) -> a.mNameInternal.compareTo(b.mNameInternal)))
					.add(tPair.prefix());
			tRegKeys.add(tPair.prefix().mNameInternal + "|" + tPair.material().mNameInternal);
		}

		// the generifier walk: mTargetGenerifying != self, And(SIMPLIFIABLE, ingotHot.NOT), eut 0
		Set<String> tExpectedGen = new HashSet<>();
		for (OreDictMaterial tMat : tByMat.keySet()) {
			OreDictMaterial tTarget = tMat.mTargetGenerifying.mMaterial;
			if (tTarget == tMat) continue;
			for (OreDictPrefix tPrefix : tByMat.get(tMat)) {
				if (!tPrefix.contains(TD.Prefix.SIMPLIFIABLE) || tPrefix == OP.ingotHot || tPrefix.mAmount <= 0) continue;
				if (!tRegKeys.contains(tPrefix.mNameInternal + "|" + tTarget.mNameInternal)) continue;
				long tDur = Math.max(1, UT.Code.units(tPrefix.mAmount, CS.U, 1, true));
				tExpectedGen.add(key(tPrefix, tMat, tTarget, tDur, 0));
			}
		}
		checkExact("generifier", tExpectedGen);

		// the polarizer walk: three statements over the Nor condition :655
		Set<OreDictPrefix> tAllPrefixes = new java.util.TreeSet<>((a, b) -> a.mNameInternal.compareTo(b.mNameInternal));
		for (Set<OreDictPrefix> tPrefixes : tByMat.values()) tAllPrefixes.addAll(tPrefixes);
		Set<String> tExpectedPol = new HashSet<>();
		assertEquals(8, ANY.Fe.mToThis.size(), "the ANY.Fe port membership snapshot");
		assertEquals(3, ANY.Steel.mToThis.size(), "the ANY.Steel port membership snapshot");
		for (OreDictPrefix tPrefix : tAllPrefixes) {
			if (tPrefix.contains(TD.Prefix.PREFIX_UNUSED) || tPrefix.contains(TD.Prefix.PLANT_DROP) || tPrefix.contains(TD.Prefix.IS_CONTAINER)
					|| tPrefix.contains(TD.Prefix.DUST_BASED) || tPrefix.contains(TD.Prefix.ORE) || tPrefix.contains(TD.Prefix.ORE_PROCESSING_BASED)
					|| tPrefix == OP.scrapGt || tPrefix == OP.ingotHot) continue;
			if (tRegKeys.contains(tPrefix.mNameInternal + "|" + MT.Nd.mNameInternal)
					&& tRegKeys.contains(tPrefix.mNameInternal + "|" + MT.NeodymiumMagnetic.mNameInternal)) {
				tExpectedPol.add(key(tPrefix, MT.Nd, MT.NeodymiumMagnetic, Math.max(1, UT.Code.units(tPrefix.mAmount, CS.U, 144, true)), 128));
			}
			for (OreDictMaterial tMat : sorted(ANY.Fe.mToThis)) {
				if (!tRegKeys.contains(tPrefix.mNameInternal + "|" + tMat.mNameInternal)
						|| !tRegKeys.contains(tPrefix.mNameInternal + "|" + MT.IronMagnetic.mNameInternal)) continue;
				tExpectedPol.add(key(tPrefix, tMat, MT.IronMagnetic, Math.max(1, UT.Code.units(tPrefix.mAmount, CS.U, 144, true)), 16));
			}
			for (OreDictMaterial tMat : sorted(ANY.Steel.mToThis)) {
				if (!tRegKeys.contains(tPrefix.mNameInternal + "|" + tMat.mNameInternal)
						|| !tRegKeys.contains(tPrefix.mNameInternal + "|" + MT.SteelMagnetic.mNameInternal)) continue;
				tExpectedPol.add(key(tPrefix, tMat, MT.SteelMagnetic, Math.max(1, UT.Code.units(tPrefix.mAmount, CS.U, 144, true)), 16));
			}
		}
		checkExact("polarizer", tExpectedPol);
	}

	/** One map's uncommented rows vs the recomputed walk, exact both ways. */
	private void checkExact(String aKey, Set<String> aExpected) throws Exception {
		Set<String> tShipped = new HashSet<>();
		for (JsonElement tElement : pourShipped(aKey)) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment")) continue; // the static face pins itself by citation
			String tIn = tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
			String tOut = tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString();
			tShipped.add(tIn + ">" + tOut + ">" + tRow.get("duration").getAsLong() + ">" + tRow.get("eut").getAsLong());
		}
		Set<String> tMissing = new HashSet<>(aExpected);
		tMissing.removeAll(tShipped);
		Set<String> tStale = new HashSet<>(tShipped);
		tStale.removeAll(aExpected);
		assertTrue(tMissing.isEmpty(), aKey + ": the frozen snapshot trails the live walk (re-pour needed): " + tMissing);
		assertTrue(tStale.isEmpty(), aKey + ": the frozen snapshot holds walk rows the live walk no longer generates: " + tStale);
		assertEquals(aExpected.size(), tShipped.size(), aKey + ": the snapshot size equals the live walk");
	}

	private static String key(OreDictPrefix aPrefix, OreDictMaterial aIn, OreDictMaterial aOut, long aDur, long aEut) {
		return "gt6:" + GTMaterialItems.itemIdOf(aPrefix, aIn) + ">gt6:" + GTMaterialItems.itemIdOf(aPrefix, aOut) + ">" + aDur + ">" + aEut;
	}

	/**
	 * The id faces: every gt6 ITEM id lives in GTMaterialItems ∪ the GTStoneBlocks paths ∪ the
	 * grass block item; every gt6 FLUID id lives in the GTFluids spec tables (the b2b2 lookup)
	 * or is the vanilla water carrier.
	 */
	@Test
	public void everyIdTheFilesReferenceIsRegistered() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tItemUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tItemUniverse.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
			tItemUniverse.add(GTStoneBlocks.path(tKey.stone().snake(), tKey.variant()));
		}
		for (GT6BeamKind tBeam : GT6BeamKind.values()) { // task beam-consume-increment: the beam universe
			tItemUniverse.add(GT6BeamBlocks.path(tBeam));
		}
		tItemUniverse.add("grass"); // the GTGrassBlocks green base — a BLOCK item, outside GTMaterialItems
		Set<String> tMissing = new HashSet<>();
		for (String tKey : CENSUS.keySet()) {
			for (JsonElement tElement : pourShipped(tKey)) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"inputs", "outputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						collectMissing(tSlot.getAsJsonObject().get("item").getAsString(), tKey, tItemUniverse, tMissing);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 item ids (these rows would WARN-skip live): " + tMissing);
	}

	private static void collectMissing(String aId, String aKey, Set<String> aUniverse, Set<String> aMissing) {
		if (!aId.startsWith("gt6:")) return;
		if (!aUniverse.contains(aId.substring(4))) aMissing.add(aKey + ": " + aId);
	}

	/** The fluid-universe face: every gt6 fluid id the files reference resolves in a spec table. */
	@Test
	public void everyFluidIdTheFilesReferenceIsRegistered() throws Exception {
		Set<String> tMissing = new HashSet<>();
		for (String tKey : CENSUS.keySet()) {
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

	/** The offline fluid-universe lookup: the union of ALL the GTFluids spec-table lookups (the b2b2 shape). */
	private static boolean fluidRegistered(String aPath) {
		if (aPath.equals("chlorine")) return true;
		return gregtech6.fluid.GTFluids.chemicalSpec(aPath) != null || gregtech6.fluid.GTFluids.closureSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.hotSpec(aPath) != null || gregtech6.fluid.GTFluids.lubricantSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.honeySpec(aPath) != null || gregtech6.fluid.GTFluids.beeRowSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.quSpec(aPath) != null || gregtech6.fluid.GTFluids.namingSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.aquaSpec(aPath) != null || gregtech6.fluid.GTFluids.simpleLiquidSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.foodSpec(aPath) != null || gregtech6.fluid.GTFluids.foodB1Spec(aPath) != null
				|| gregtech6.fluid.GTFluids.foodB2Spec(aPath) != null || gregtech6.fluid.GTFluids.foodTailSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.engineSpec(aPath) != null || gregtech6.fluid.GTFluids.dyeIndexOf(aPath) >= 0;
	}

	// ------------------------------------------------------------------ helpers

	/** Finds the row with the exact input/output item pair. */
	private static JsonObject findRow(JsonArray aRows, String aInputItem, String aOutputItem) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("inputs") || !tRow.has("outputs")) continue;
			JsonArray tIn = tRow.getAsJsonArray("inputs"), tOut = tRow.getAsJsonArray("outputs");
			if (tIn.isEmpty() || tOut.isEmpty()) continue; // the fluid rows ride empty item legs
			if (aInputItem.equals(tIn.get(0).getAsJsonObject().get("item").getAsString())
					&& aOutputItem.equals(tOut.get(0).getAsJsonObject().get("item").getAsString())) return tRow;
		}
		throw new AssertionError("no row " + aInputItem + " -> " + aOutputItem);
	}

	/** Finds the first fluid row whose first fluidInput carries the given fluid id. */
	private static JsonObject findFluidRow(JsonArray aRows, String aFluid) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("fluidInputs")) continue;
			if (aFluid.equals(tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString())) return tRow;
		}
		throw new AssertionError("no fluid row for " + aFluid);
	}

	private static String slotId(JsonElement aSlot) {
		return aSlot.getAsJsonObject().get("item") != null ? aSlot.getAsJsonObject().get("item").getAsString()
				: aSlot.getAsJsonObject().get("fluid").getAsString();
	}

	private static List<OreDictMaterial> sorted(Iterable<OreDictMaterial> aMats) {
		List<OreDictMaterial> tList = new ArrayList<>();
		for (OreDictMaterial tMat : aMats) tList.add(tMat);
		tList.sort((a, b) -> a.mNameInternal.compareTo(b.mNameInternal));
		return tList;
	}
}
