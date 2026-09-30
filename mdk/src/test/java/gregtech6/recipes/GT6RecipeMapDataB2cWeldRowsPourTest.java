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

import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTItemPipes;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The recipe-data-b2c-weld row-stock pour test (the B2c-generify posture, r2): the
 * welder.json file — the 46 {@code RM.Welder} RecipeMapHandlerPrefix statements of
 * Loader_Recipes_Handlers.java:319-368 statically expanded over the port THREE-universe
 * item face (2971 rows) — pours through the real {@link GT6RecipeMapJsonLoader} seam with
 * zero skips. RECOMPUTABILITY: the rows were generated from a one-shot live registration
 * dump (an uncommitted JUnit walking the three universes + the condition gates, folded by a
 * /tmp generator — the B2c zero-transcription method), and THIS test is the durable half:
 * the live-walk cross-checks recompute ALL 46 statement walks against the live registration
 * and fail the moment the frozen snapshot trails or outruns it. THE THREE UNIVERSES (the
 * research.prefix-gap-survey reversal of the r1 single-universe claim): (a) the
 * GTMaterialItems pairs (every input prefix face), (b) the GTMaterialBlocks storage-block
 * pairs (the blockSolid outputs, the GTMaterialBlocks.get fallback — the
 * GT6RecipesCokeOven.java:263-267 precedent; both registries land in the Forge ITEM
 * registry, so the live GT6RecipeMapJsonLoader.resolveItem — a ForgeRegistries.ITEMS
 * lookup — resolves them unchanged), and (c) the pipe rows (GTFluidPipes TINY/SMALL +
 * GTItemPipes MEDIUM/LARGE/HUGE; for the double-carried medium/large/huge the ITEM side is
 * chosen = the upstream registration order, Loader_MultiTileEntities.java:1823 item before
 * :1846 fluid). Per the scale ruling the walk rows carry NO per-row comment (the
 * cross-check IS their provenance); the file head carries the DECLARED OVER-CAP statement,
 * the dropped ST.tag selector leg (the loom precedent) and the 16 BLOCKED statement faces
 * (the casingMachine family is unregistered port-side, so the engine's own both-side
 * existence gate drops every material).
 */
public class GT6RecipeMapDataB2cWeldRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The map key and the frozen census (the file-head Row census statement). */
	private static final String KEY = "welder";
	private static final int CENSUS = 2971; // 2320 cold + 651 hot

	/**
	 * One upstream statement (Loader_Recipes_Handlers.java:319-368; the single-input ctor
	 * :57-80 for :319-347, the two-input ctor :82-105 for :350-368): the input prefix/count
	 * pairs, the OUTPUT FACE ("item:" = the GTMaterialItems universe, "block:" = the
	 * GTMaterialBlocks universe, "fpipe:"/"ipipe:" = the pipe-row families), the output
	 * count, the fixed duration (0 = computed, getCosts :225-227 over mult + mult*
	 * mToolQuality), the COATED.NOT gate leg and the heat leg (FURNACE positive = the hot
	 * statements, tEasyHeatable = Or(FURNACE), Loader_Recipes_Handlers.java:58). The
	 * ST.tag(n) selector leg of :319-347 is DROPPED (declared deviation).
	 */
	private record WeldStmt(int line, String[] ins, long[] inCounts, String outKind, String out, long outCount,
			long fixed, long mult, boolean coated, boolean hot) {}

	/** The 46 statements, upstream order. eut 16 on every row (:218 mEUt). */
	private static final List<WeldStmt> STMTS = List.of(
			// cold leg A :319-332 — And(ANTIMATTER.NOT, FLAMMABLE.NOT, SMITHABLE, FURNACE.NOT[, COATED.NOT]), dur 0, mult 64
			new WeldStmt(319, new String[] {"ingot"}, new long[] {2}, "item", "ingotDouble", 1, 0, 64, true, false),
			new WeldStmt(320, new String[] {"ingot"}, new long[] {3}, "item", "ingotTriple", 1, 0, 64, true, false),
			new WeldStmt(321, new String[] {"ingot"}, new long[] {4}, "item", "ingotQuadruple", 1, 0, 64, true, false),
			new WeldStmt(322, new String[] {"ingot"}, new long[] {5}, "item", "ingotQuintuple", 1, 0, 64, true, false),
			new WeldStmt(323, new String[] {"ingot"}, new long[] {9}, "block", "blockSolid", 1, 0, 64, true, false),
			new WeldStmt(324, new String[] {"plateCurved"}, new long[] {1}, "fpipe", "TINY", 2, 0, 64, true, false),
			new WeldStmt(325, new String[] {"plateCurved"}, new long[] {1}, "fpipe", "SMALL", 1, 0, 64, true, false),
			new WeldStmt(326, new String[] {"plateCurved"}, new long[] {3}, "ipipe", "MEDIUM", 1, 0, 64, true, false),
			new WeldStmt(327, new String[] {"plateCurved"}, new long[] {6}, "ipipe", "LARGE", 1, 0, 64, true, false),
			new WeldStmt(328, new String[] {"plateCurved"}, new long[] {12}, "ipipe", "HUGE", 1, 0, 64, true, false),
			new WeldStmt(329, new String[] {"casingSmall"}, new long[] {2}, "item", "plate", 1, 0, 64, true, false),
			new WeldStmt(330, new String[] {"bolt"}, new long[] {4}, "item", "stick", 1, 0, 64, false, false),
			new WeldStmt(331, new String[] {"bolt"}, new long[] {8}, "item", "stickLong", 1, 0, 64, false, false),
			new WeldStmt(332, new String[] {"stick"}, new long[] {2}, "item", "stickLong", 1, 0, 64, false, false),
			// hot leg A :334-347 — And(..., SMITHABLE, FURNACE[, COATED.NOT]), duration 16*n fixed, mult 0
			new WeldStmt(334, new String[] {"ingot"}, new long[] {2}, "item", "ingotDouble", 1, 16 * 2, 0, true, true),
			new WeldStmt(335, new String[] {"ingot"}, new long[] {3}, "item", "ingotTriple", 1, 16 * 3, 0, true, true),
			new WeldStmt(336, new String[] {"ingot"}, new long[] {4}, "item", "ingotQuadruple", 1, 16 * 4, 0, true, true),
			new WeldStmt(337, new String[] {"ingot"}, new long[] {5}, "item", "ingotQuintuple", 1, 16 * 5, 0, true, true),
			new WeldStmt(338, new String[] {"ingot"}, new long[] {9}, "block", "blockSolid", 1, 16 * 9, 0, true, true),
			new WeldStmt(339, new String[] {"plateCurved"}, new long[] {1}, "fpipe", "TINY", 2, 16 * 1, 0, true, true),
			new WeldStmt(340, new String[] {"plateCurved"}, new long[] {1}, "fpipe", "SMALL", 1, 16 * 1, 0, true, true),
			new WeldStmt(341, new String[] {"plateCurved"}, new long[] {3}, "ipipe", "MEDIUM", 1, 16 * 3, 0, true, true),
			new WeldStmt(342, new String[] {"plateCurved"}, new long[] {6}, "ipipe", "LARGE", 1, 16 * 6, 0, true, true),
			new WeldStmt(343, new String[] {"plateCurved"}, new long[] {12}, "ipipe", "HUGE", 1, 16 * 12, 0, true, true),
			new WeldStmt(344, new String[] {"casingSmall"}, new long[] {2}, "item", "plate", 1, 16 * 1, 0, true, true),
			new WeldStmt(345, new String[] {"bolt"}, new long[] {4}, "item", "stick", 1, 16 / 2, 0, false, true),
			new WeldStmt(346, new String[] {"bolt"}, new long[] {8}, "item", "stickLong", 1, 16, 0, false, true),
			new WeldStmt(347, new String[] {"stick"}, new long[] {2}, "item", "stickLong", 1, 16, 0, false, true),
			// cold leg B :350-358 — two-input, no selector, And(..., SMITHABLE, FURNACE.NOT), dur 0, mult 64
			new WeldStmt(350, new String[] {"plateCurved", "ring"}, new long[] {4, 1}, "item", "rotor", 1, 0, 64, false, false),
			new WeldStmt(351, new String[] {"plate", "stickLong"}, new long[] {6, 2}, "item", "casingMachine", 1, 0, 64, false, false),
			new WeldStmt(352, new String[] {"plateDouble", "stickLong"}, new long[] {6, 2}, "item", "casingMachineDouble", 1, 0, 64, false, false),
			new WeldStmt(353, new String[] {"plateQuadruple", "stickLong"}, new long[] {6, 2}, "item", "casingMachineQuadruple", 1, 0, 64, false, false),
			new WeldStmt(354, new String[] {"plateDense", "stickLong"}, new long[] {6, 2}, "item", "casingMachineDense", 1, 0, 64, false, false),
			new WeldStmt(355, new String[] {"plate", "stick"}, new long[] {6, 4}, "item", "casingMachine", 1, 0, 64, false, false),
			new WeldStmt(356, new String[] {"plateDouble", "stick"}, new long[] {6, 4}, "item", "casingMachineDouble", 1, 0, 64, false, false),
			new WeldStmt(357, new String[] {"plateQuadruple", "stick"}, new long[] {6, 4}, "item", "casingMachineQuadruple", 1, 0, 64, false, false),
			new WeldStmt(358, new String[] {"plateDense", "stick"}, new long[] {6, 4}, "item", "casingMachineDense", 1, 0, 64, false, false),
			// hot leg B :360-368 — two-input, no selector, And(..., SMITHABLE, FURNACE), duration 16*n fixed
			new WeldStmt(360, new String[] {"plateCurved", "ring"}, new long[] {4, 1}, "item", "rotor", 1, 16 * 4, 0, false, true),
			new WeldStmt(361, new String[] {"plate", "stickLong"}, new long[] {6, 2}, "item", "casingMachine", 1, 16 * 8, 0, false, true),
			new WeldStmt(362, new String[] {"plateDouble", "stickLong"}, new long[] {6, 2}, "item", "casingMachineDouble", 1, 16 * 14, 0, false, true),
			new WeldStmt(363, new String[] {"plateQuadruple", "stickLong"}, new long[] {6, 2}, "item", "casingMachineQuadruple", 1, 16 * 26, 0, false, true),
			new WeldStmt(364, new String[] {"plateDense", "stickLong"}, new long[] {6, 2}, "item", "casingMachineDense", 1, 16 * 56, 0, false, true),
			new WeldStmt(365, new String[] {"plate", "stick"}, new long[] {6, 4}, "item", "casingMachine", 1, 16 * 8, 0, false, true),
			new WeldStmt(366, new String[] {"plateDouble", "stick"}, new long[] {6, 4}, "item", "casingMachineDouble", 1, 16 * 14, 0, false, true),
			new WeldStmt(367, new String[] {"plateQuadruple", "stick"}, new long[] {6, 4}, "item", "casingMachineQuadruple", 1, 16 * 26, 0, false, true),
			new WeldStmt(368, new String[] {"plateDense", "stick"}, new long[] {6, 4}, "item", "casingMachineDense", 1, 16 * 56, 0, false, true));

	/**
	 * The frozen per-statement expansion table (the generation-time accounting face; the
	 * 16 zero rows are the BLOCKED statement faces — the casingMachine family is
	 * unregistered port-side). A registration change turns the live recomputation red and
	 * this table is the conscious-bump ratchet.
	 */
	private static final Map<Integer, Integer> STMT_CENSUS = new LinkedHashMap<>();
	static {
		int[][] tPairs = {{319, 231}, {320, 231}, {321, 231}, {322, 231}, {323, 231}, {324, 29}, {325, 29},
				{326, 14}, {327, 14}, {328, 14}, {329, 180}, {330, 234}, {331, 234}, {332, 234},
				{334, 71}, {335, 71}, {336, 71}, {337, 71}, {338, 71}, {339, 5}, {340, 5},
				{341, 7}, {342, 7}, {343, 7}, {344, 26}, {345, 71}, {346, 71}, {347, 71},
				{350, 183}, {351, 0}, {352, 0}, {353, 0}, {354, 0}, {355, 0}, {356, 0}, {357, 0}, {358, 0},
				{360, 26}, {361, 0}, {362, 0}, {363, 0}, {364, 0}, {365, 0}, {366, 0}, {367, 0}, {368, 0}};
		for (int[] tPair : tPairs) STMT_CENSUS.put(tPair[0], tPair[1]);
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
		String tPath = "/data/gt6/recipe_maps/" + KEY + ".json";
		InputStream tStream = GT6RecipeMapDataB2cWeldRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", KEY), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: the file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void theWelderFilePoursItsFullCensusWithZeroSkips() throws Exception {
		pourShipped();
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(KEY);
		assertNotNull(tMap, KEY + " resolves");
		assertEquals(CENSUS, tMap.mRecipeList.size(), KEY + ": the map holds the census");
		assertEquals(CENSUS, GT6RecipeMapJsonLoader.pouredCount(KEY),
				KEY + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
	}

	/** The declaration shape pin: deviation + frozen + handler cite + selector + walk rows + over-cap + smoke row + output faces + blocked faces. */
	@Test
	public void theFileHeadKeepsItsDeclarationComments() throws Exception {
		InputStream tStream = GT6RecipeMapDataB2cWeldRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + KEY + ".json");
		assertNotNull(tStream);
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		assertTrue(tDoc.has("comment"), KEY + ": the file head carries a comment member");
		String tComment = tDoc.get("comment").getAsString();
		String tLower = tComment.toLowerCase(java.util.Locale.ROOT);
		assertTrue(tLower.contains("declared deviation"), KEY + ": the deviation class is declared");
		assertTrue(tLower.contains("frozen"), KEY + ": the declaration names the frozen-universe semantics");
		assertTrue(tComment.contains("Loader_Recipes_Handlers.java:319-368"), KEY + ": the declaration cites the 46 handler statements");
		assertTrue(tLower.contains("st.tag(2..9)"), KEY + ": the dropped selector leg is declared (the loom precedent)");
		assertTrue(tLower.contains("walk row provenance"), KEY + ": the declaration names the walk-row provenance ruling");
		assertTrue(tLower.contains("declared over-cap"), KEY + ": the declaration names the over-cap ruling");
		assertTrue(tLower.contains("smoke row"), KEY + ": the declaration names the replaced smoke row");
		assertTrue(tLower.contains("gtmaterialblocks"), KEY + ": the blockSolid output face names the block universe + fallback");
		assertTrue(tLower.contains("fluid-pipe family") && tLower.contains("item-pipe family"),
				KEY + ": the two pipe families are named with their upstream carrier faces");
		assertTrue(tComment.contains(":1823 item before :1846 fluid"),
				KEY + ": the double-carried medium/large/huge faces declare the item-side choice");
		assertTrue(tLower.contains("blocked statement faces"), KEY + ": the 16 zero-row statements are accounted");
		assertTrue(tComment.contains("casingMachine") && tComment.contains("casingMachineDense"),
				KEY + ": the blocked casingMachine family is named");
	}

	/** All 2971 rows are handler-walk expansions — none carries a per-row comment (the scale ruling). */
	@Test
	public void everyRowIsAnUncommentedWalkRow() throws Exception {
		int tCommented = 0;
		JsonArray tRows = pourShipped();
		for (JsonElement tElement : tRows) {
			if (tElement.getAsJsonObject().has("comment")) tCommented++;
		}
		assertEquals(CENSUS, tRows.size(), KEY + ": the row stock");
		assertEquals(0, tCommented, KEY + ": no per-row comments (the provenance IS the live-walk cross-check)");
	}

	/**
	 * The per-statement ratchet: the frozen accounting table (STMT_CENSUS, the generation-time
	 * expansion face with the 16 zero-row BLOCKED statements) must equal the live recomputation.
	 * A registration change turns this red — re-pour and bump the table consciously.
	 */
	@Test
	public void theStatementCensusMatchesTheFrozenExpansion() throws Exception {
		Walk tWalk = liveWalk();
		List<String> tDrift = new ArrayList<>();
		int tLiveTotal = 0;
		for (Map.Entry<Integer, Integer> tPin : STMT_CENSUS.entrySet()) {
			int tLive = tWalk.census().get(tPin.getKey());
			tLiveTotal += tLive;
			if (tLive != tPin.getValue()) tDrift.add(":" + tPin.getKey() + " frozen " + tPin.getValue() + " vs live " + tLive);
		}
		assertTrue(tDrift.isEmpty(), KEY + ": the per-statement expansion drifted — re-pour and bump STMT_CENSUS: " + tDrift);
		assertEquals(CENSUS, tLiveTotal, KEY + ": the live total equals the census");
	}

	/**
	 * The live-walk cross-check — the heart of the frozen-snapshot deviation: recompute ALL
	 * 46 statement walks against the LIVE three-universe registration (the
	 * RecipeMapHandlerPrefix engine: the condition gate :205, the INVALID_MATERIAL gate, the
	 * both-side item existence gates :209/:214, the walk over mInputPrefixes[0].
	 * mRegisteredMaterials :173, duration mDuration>0 ? mDuration : max(1, units(max(
	 * mUnitsIn, mUnitsOut), U, mult + mult*q, roundUp)) :218/:225-227) and require the
	 * shipped rows to cover the recomputation EXACTLY, both directions.
	 */
	@Test
	public void theShippedWalkMatchesTheLiveRegistrationWalk() throws Exception {
		Set<String> tExpected = liveWalk().rows();

		// the exact set match, both directions
		Set<String> tShipped = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			StringBuilder tKey = new StringBuilder();
			JsonArray tIn = tRow.getAsJsonArray("inputs");
			for (int i = 0; i < tIn.size(); i++) {
				if (i > 0) tKey.append('+');
				JsonObject tSlot = tIn.get(i).getAsJsonObject();
				tKey.append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsLong());
			}
			JsonArray tOut = tRow.getAsJsonArray("outputs");
			JsonObject tOutSlot = tOut.get(0).getAsJsonObject();
			tKey.append('>').append(tOutSlot.get("item").getAsString()).append(':').append(tOutSlot.get("count").getAsLong())
					.append('>').append(tRow.get("duration").getAsLong()).append(">16");
			tShipped.add(tKey.toString());
		}
		Set<String> tMissing = new HashSet<>(tExpected);
		tMissing.removeAll(tShipped);
		Set<String> tStale = new HashSet<>(tShipped);
		tStale.removeAll(tExpected);
		assertTrue(tMissing.isEmpty(), KEY + ": the frozen snapshot trails the live walk (re-pour needed): " + tMissing);
		assertTrue(tStale.isEmpty(), KEY + ": the frozen snapshot holds walk rows the live walk no longer generates: " + tStale);
		assertEquals(tExpected.size(), tShipped.size(), KEY + ": the snapshot size equals the live walk");
	}

	/** The live recomputation of all 46 statement walks over the THREE universes (the RecipeMapHandlerPrefix engine semantics). */
	private Walk liveWalk() {
		GTMaterialItems.initMaterials();
		Map<String, Pair> tPairs = new HashMap<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tPairs.put(tPair.prefix().mNameInternal + "|" + tPair.material().mNameInternal,
					new Pair(tPair.prefix(), tPair.material()));
		}
		// universe (b): the GTMaterialBlocks storage-block pairs (prefix|material keys)
		Set<String> tBlockPairs = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			tBlockPairs.add(tPair.prefix().mNameInternal + "|" + tPair.material().mNameInternal);
		}
		// universe (c): the pipe rows, material internal name | variant -> the shipped path
		Map<String, String> tFluidPipes = new HashMap<>();
		for (GTFluidPipes.FluidPipeRow tRow : GTFluidPipes.ROWS) {
			tFluidPipes.put(tRow.material().oreDictMaterial().mNameInternal + "|" + tRow.variant().name(), tRow.path());
		}
		Map<String, String> tItemPipes = new HashMap<>();
		for (GTItemPipes.ItemPipeRow tRow : GTItemPipes.ROWS) {
			tItemPipes.put(tRow.material().oreDictMaterial().mNameInternal + "|" + tRow.variant().name(), tRow.path());
		}

		Set<String> tExpected = new HashSet<>();
		Map<Integer, Integer> tLiveCensus = new LinkedHashMap<>();
		for (WeldStmt tStmt : STMTS) {
			int tCount = 0;
			for (Map.Entry<String, Pair> tEntry : tPairs.entrySet()) {
				Pair tFirst = tEntry.getValue();
				if (!tFirst.prefix().mNameInternal.equals(tStmt.ins()[0])) continue; // the walk iterates mInputPrefixes[0].mRegisteredMaterials
				OreDictMaterial tMat = tFirst.material();
				if (tMat.contains(TD.Atomic.ANTIMATTER) || tMat.contains(TD.Properties.FLAMMABLE)
						|| !tMat.contains(TD.Processing.SMITHABLE) || tMat.contains(TD.Processing.FURNACE) != tStmt.hot()
						|| (tStmt.coated() && tMat.contains(TD.Compounds.COATED))
						|| tMat.contains(TD.Properties.INVALID_MATERIAL)) continue;
				boolean tBothSides = true;
				long tUnitsIn = 0;
				for (int i = 0; i < tStmt.ins().length; i++) {
					Pair tInput = tPairs.get(tStmt.ins()[i] + "|" + tMat.mNameInternal);
					if (tInput == null) {tBothSides = false; break;}
					tUnitsIn += tInput.prefix().mAmount * tStmt.inCounts()[i];
				}
				if (!tBothSides) continue;
				// the output face: the :214 gate over the matching universe
				String tOutId;
				long tOutAmount;
				switch (tStmt.outKind()) {
					case "item" -> {
						Pair tOutput = tPairs.get(tStmt.out() + "|" + tMat.mNameInternal);
						if (tOutput == null) continue;
						tOutId = "gt6:" + GTMaterialItems.itemIdOf(tOutput.prefix(), tMat);
						tOutAmount = tOutput.prefix().mAmount;
					}
					case "block" -> {
						if (!tBlockPairs.contains(tStmt.out() + "|" + tMat.mNameInternal)) continue;
						tOutId = "gt6:" + GTMaterialItems.itemIdOf(blockPrefix(tStmt.out()), tMat);
						tOutAmount = blockPrefix(tStmt.out()).mAmount;
					}
					case "fpipe" -> {
						String tPath = tFluidPipes.get(tMat.mNameInternal + "|" + tStmt.out());
						if (tPath == null) continue;
						tOutId = "gt6:" + tPath;
						tOutAmount = pipeAmount(tStmt.out());
					}
					default -> {
						String tPath = tItemPipes.get(tMat.mNameInternal + "|" + tStmt.out());
						if (tPath == null) continue;
						tOutId = "gt6:" + tPath;
						tOutAmount = pipeAmount(tStmt.out());
					}
				}
				long tDur = tStmt.fixed() > 0 ? tStmt.fixed()
						: Math.max(1, UT.Code.units(Math.max(tUnitsIn, tOutAmount * tStmt.outCount()), CS.U,
								tStmt.mult() + tStmt.mult() * tMat.mToolQuality, true));
				StringBuilder tKey = new StringBuilder();
				for (int i = 0; i < tStmt.ins().length; i++) {
					if (i > 0) tKey.append('+');
					tKey.append("gt6:").append(GTMaterialItems.itemIdOf(tPairs.get(tStmt.ins()[i] + "|" + tMat.mNameInternal).prefix(), tMat))
							.append(':').append(tStmt.inCounts()[i]);
				}
				tKey.append('>').append(tOutId).append(':').append(tStmt.outCount())
						.append('>').append(tDur).append(">16");
				tExpected.add(tKey.toString());
				tCount++;
			}
			tLiveCensus.put(tStmt.line(), tCount);
		}
		return new Walk(tExpected, tLiveCensus);
	}

	/** The block-universe output prefix (only blockSolid is a welder output face today). */
	private static OreDictPrefix blockPrefix(String aName) {
		assertEquals("blockSolid", aName, "the only block-universe welder output face");
		return OP.blockSolid;
	}

	/** The pipe variant -> the OP prefix amount (TINY -> OP.pipeTiny.mAmount etc). */
	private static long pipeAmount(String aVariant) {
		return switch (aVariant) {
			case "TINY" -> OP.pipeTiny.mAmount;
			case "SMALL" -> OP.pipeSmall.mAmount;
			case "MEDIUM" -> OP.pipeMedium.mAmount;
			case "LARGE" -> OP.pipeLarge.mAmount;
			case "HUGE" -> OP.pipeHuge.mAmount;
			default -> throw new IllegalArgumentException("no welder pipe face " + aVariant);
		};
	}

	/** Spot checks — 18 rows across all four legs, the quality scaling, the fixed hot durations, the block + pipe faces. */
	@Test
	public void welderRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped();
		// cold leg A (:319-323) — duration = units(max, U, 64+64q, T); Iron q=2, Tungstensteel q=4
		assertEquals(384, findRow(tRows, "gt6:ingot_iron", "gt6:ingot_double_iron", 384).get("duration").getAsLong(),
				":319 cold Iron ingot x2 -> double, q2");
		assertEquals(640, findRow(tRows, "gt6:ingot_tungstensteel", "gt6:ingot_double_tungstensteel", 640).get("duration").getAsLong(),
				":319 cold Tungstensteel q4");
		assertEquals(1600, findRow(tRows, "gt6:ingot_tungstensteel", "gt6:ingot_quintuple_tungstensteel", 1600).get("duration").getAsLong(),
				":322 cold ingot x5, 5*64*(4+1)");
		assertEquals(1728, findRow(tRows, "gt6:ingot_iron", "gt6:block_solid_iron", 1728).get("duration").getAsLong(),
				":323 cold ingot x9 -> the GTMaterialBlocks storage block, 9*64*3");
		assertEquals(2880, findRow(tRows, "gt6:ingot_tungstensteel", "gt6:block_solid_tungstensteel", 2880).get("duration").getAsLong(),
				":323 cold blockSolid, q4");
		assertEquals(192, findRow(tRows, "gt6:casing_small_iron", "gt6:plate_iron", 192).get("duration").getAsLong(),
				":329 cold casingSmall x2 -> plate");
		assertEquals(96, findRow(tRows, "gt6:bolt_iron", "gt6:stick_iron", 96).get("duration").getAsLong(),
				":330 cold bolt x4 (U8 each) -> stick (U2)");
		assertEquals(320, findRow(tRows, "gt6:bolt_tungstensteel", "gt6:stick_long_tungstensteel", 320).get("duration").getAsLong(),
				":331 cold bolt x8 -> stickLong, q4");
		assertEquals(192, findRow(tRows, "gt6:stick_iron", "gt6:stick_long_iron", 192).get("duration").getAsLong(),
				":332 cold stick x2 -> stickLong");
		// hot leg A (:334-347) — the fixed durations, FURNACE-positive materials only
		assertEquals(32, findRow(tRows, "gt6:ingot_copper", "gt6:ingot_double_copper", 32).get("duration").getAsLong(),
				":334 hot ingot x2, fixed 32");
		assertEquals(16, findRow(tRows, "gt6:plate_curved_copper", "gt6:copper_fluid_pipe_tiny", 16).get("duration").getAsLong(),
				":339 hot plateCurved -> the fluid-pipe TINY face, fixed 16");
		assertEquals(16, findRow(tRows, "gt6:casing_small_copper", "gt6:plate_copper", 16).get("duration").getAsLong(),
				":344 hot casingSmall x2, fixed 16");
		assertEquals(8, findRow(tRows, "gt6:bolt_tin", "gt6:stick_tin", 8).get("duration").getAsLong(),
				":345 hot bolt x4, fixed 16/2");
		assertEquals(16, findRow(tRows, "gt6:stick_bronze", "gt6:stick_long_bronze", 16).get("duration").getAsLong(),
				":347 hot stick x2, fixed 16");
		// leg B — the two-input rotor rows, cold computed vs hot fixed
		JsonObject tRotorCold = findRow(tRows, "gt6:plate_curved_iron", "gt6:rotor_iron", 816);
		assertEquals(816, tRotorCold.get("duration").getAsLong(), ":350 cold rotor = 4.25U * 64*3 / U");
		assertEquals(2, tRotorCold.getAsJsonArray("inputs").size(), ":350 carries the ring second input");
		assertEquals(1, tRotorCold.getAsJsonArray("inputs").get(1).getAsJsonObject().get("count").getAsInt(), ":350 ring x1");
		assertEquals(64, findRow(tRows, "gt6:plate_curved_copper", "gt6:rotor_copper", 64).get("duration").getAsLong(),
				":360 hot rotor, fixed 64");
		// the pipe faces — the item-pipe family carries MEDIUM/LARGE/HUGE, the slug id form
		assertEquals(960, findRow(tRows, "gt6:plate_curved_osmium_elemental", "gt6:osmium_item_pipe_medium", 960).get("duration").getAsLong(),
				":326 cold item-pipe MEDIUM, 3U * 64*(4+1) / U — the slug id form, not the snakeCase");
		assertEquals(48, findRow(tRows, "gt6:plate_curved_brass", "gt6:brass_item_pipe_medium", 48).get("duration").getAsLong(),
				":341 hot item-pipe MEDIUM, fixed 48");
	}

	/**
	 * The id face over the THREE universes: every gt6 ITEM id lives in GTMaterialItems ∪ the
	 * GTMaterialBlocks storage blocks ∪ the pipe rows (GTFluidPipes/GTItemPipes).
	 */
	@Test
	public void everyIdTheFilesReferenceIsRegistered() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tItemUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tItemUniverse.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			tItemUniverse.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTFluidPipes.FluidPipeRow tRow : GTFluidPipes.ROWS) {
			tItemUniverse.add(tRow.path());
		}
		for (GTItemPipes.ItemPipeRow tRow : GTItemPipes.ROWS) {
			tItemUniverse.add(tRow.path());
		}
		Set<String> tMissing = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			for (String tLeg : new String[] {"inputs", "outputs"}) {
				for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
					String tId = tSlot.getAsJsonObject().get("item").getAsString();
					if (tId.startsWith("gt6:") && !tItemUniverse.contains(tId.substring(4))) {
						tMissing.add(tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 item ids (these rows would WARN-skip live): " + tMissing);
	}

	/** The energy face: every row carries the handler's mEUt=16 and a positive duration. */
	@Test
	public void everyRowCarriesTheWelderEutAndAPositiveDuration() throws Exception {
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			assertEquals(16, tRow.get("eut").getAsLong(), ":218 mEUt = 16 on every statement");
			assertTrue(tRow.get("duration").getAsLong() > 0, "the JSON loader rejects duration <= 0");
		}
	}

	/** The replaced smoke row stays gone: the stock is 100% gt6 ids (the vanilla iron+gold->chain row is no more). */
	@Test
	public void theVanillaSmokeRowStaysGone() throws Exception {
		int tForeign = 0;
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			for (String tLeg : new String[] {"inputs", "outputs"}) {
				for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
					if (!tSlot.getAsJsonObject().get("item").getAsString().startsWith("gt6:")) tForeign++;
				}
			}
		}
		assertEquals(0, tForeign, "no vanilla-namespace ids (the p29-w1 smoke row was replaced by the handler replay)");
	}

	// ------------------------------------------------------------------ helpers

	/** Finds the row with the exact first-input/output item pair AND duration (the cold/hot legs share pairs). */
	private static JsonObject findRow(JsonArray aRows, String aInputItem, String aOutputItem, long aDuration) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			JsonArray tIn = tRow.getAsJsonArray("inputs"), tOut = tRow.getAsJsonArray("outputs");
			if (aInputItem.equals(tIn.get(0).getAsJsonObject().get("item").getAsString())
					&& aOutputItem.equals(tOut.get(0).getAsJsonObject().get("item").getAsString())
					&& aDuration == tRow.get("duration").getAsLong()) return tRow;
		}
		throw new AssertionError("no row " + aInputItem + " -> " + aOutputItem + " @ " + aDuration);
	}

	/** The live-registration pair face (prefix/material + the evaluated condition tags). */
	private record Pair(OreDictPrefix prefix, OreDictMaterial material) {}

	/** The live-walk result: the recomputed row keys + the per-statement expansion census. */
	private record Walk(Set<String> rows, Map<Integer, Integer> census) {}
}
