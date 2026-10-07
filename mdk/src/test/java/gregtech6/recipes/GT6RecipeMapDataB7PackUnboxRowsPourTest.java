package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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
 * The recipe-b7 pack/unbox/roll walk pour test (the B2c-cut canonical form): the
 * Loader_Recipes_Handlers.java handler-walk static replay over the FOUR maps of this card —
 * the RollingMill 22 statements (:262-284), the RollBender 8 statements (:298-306), the
 * Unboxinator 34 statements (:451-489) and the Boxinator 55 statements (:492-550) — the
 * 89 static statements of the :451-550 Box/Unbox face plus the roll twins, all pouring
 * through the real {@link GT6RecipeMapJsonLoader} seam with the seated rows preserved.
 * RECOMPUTABILITY: the rows were generated from a one-shot live registration dump (the
 * uncommitted JUnit walking GTMaterialItems ∪ GTMaterialBlocks — the B2c-cut zero-transcription
 * method) and THIS test is the durable half: the live-walk cross-check recomputes every walk
 * against the live registration and fails the moment the frozen snapshot trails or outruns
 * it, so walk rows carry no per-row comment.
 *
 * <p>DECLARED ZERO-POUR faces (all with live pair census 0, pinned by
 * {@link #theZeroPourFacesStayTrue}): plateSteamcraft (:263/:275), compressed (:270/:282),
 * sheetGt (:272/:284), the pipeMedium/pipeSmall/pipeQuadruple/pipeNonuple faces
 * (:458/:459/:492/:493 — pipes ride the MultiTileEntity domain), the twelve crateGt/crateGt64
 * faces of :461-473 and the thirty crate faces of :495-525 (IL.Crate/IL.Crate_Fireproof
 * unported). The :619-626 wire face is a DECLARED ZERO-ROW WALK: :622 the RM.Unboxinator leg
 * walks wireGt01-16 whose live pair census is 0 (the wiremill zero-pour pool precedent; 34
 * upstream divisor pairs), :621 the RM.Loom leg rides the same absent face inside loom.json,
 * outside this card's file scope. The ST.tag(n) selector legs (:492/:493/:527-550) drop per
 * the loom.json selector precedent. The MT.Empty additional-output slots (:451-456) resolve
 * null BOTH sides — verbatim identity, single-output rows.
 */
public class GT6RecipeMapDataB7PackUnboxRowsPourTest extends GTRecipesOfflineTestBase {

	private static final Map<String, Integer> CENSUS = Map.of(
			"rollingmill", 2434,  // 2 seated smoke + 2432 walk (22 statements :262-284)
			"rollbender",   935,  // 3 seated smoke +  932 walk ( 8 statements :298-306)
			"boxinator",  12358,  // 39 seated (smoke + b1 28 + robotics-chain 10, the review-seat rebase roll) + 12319 walk (55 statements :492-550)
			"unboxinator", 11312); // 21 seated (smoke + bookshelf + b1) + 11291 walk (34 statements :451-489)

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

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

	/** Pours all four shipped files under their map keys; returns the parsed row arrays. */
	private Map<String, JsonArray> pourShipped() throws Exception {
		Map<ResourceLocation, JsonElement> tData = new HashMap<>();
		Map<String, JsonArray> rRows = new HashMap<>();
		for (String tKey : CENSUS.keySet()) {
			String tPath = "/data/gt6/recipe_maps/" + tKey + ".json";
			InputStream tStream = GT6RecipeMapDataB7PackUnboxRowsPourTest.class.getResourceAsStream(tPath);
			assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			tData.put(new ResourceLocation("gt6", tKey), tDoc.deepCopy());
			rRows.put(tKey, tDoc.getAsJsonArray("recipes"));
		}
		GT6RecipeMapJsonLoader.pour(tData);
		return rRows;
	}

	/** The census: each file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void theFourFilesPourTheirFullCensusWithZeroSkips() throws Exception {
		pourShipped();
		for (Map.Entry<String, Integer> tPin : CENSUS.entrySet()) {
			RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(tPin.getKey());
			assertNotNull(tMap, tPin.getKey() + " resolves");
			assertEquals(tPin.getValue().intValue(), tMap.mRecipeList.size(), tPin.getKey() + ": the map holds the census");
			assertEquals(tPin.getValue().intValue(), GT6RecipeMapJsonLoader.pouredCount(tPin.getKey()),
					tPin.getKey() + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
		}
	}

	/** The file-head declaration pins: anchors + zero-pour faces + walk provenance + frozen + seated rows. */
	@Test
	public void theFileHeadsKeepTheirDeclarations() throws Exception {
		Map<String, String[]> tPins = Map.of(
				"rollingmill", new String[] {"Loader_Recipes_Handlers.java:262-284", ":263/:275", ":270/:282", ":272/:284", "platesteamcraft", "compressed", "sheetgt", "getcosts", "teasyworkable", "smoke rows stay", "frozen", "live-walk cross-check"},
				"rollbender", new String[] {"Loader_Recipes_Handlers.java:298-306", "no coated leg", "16/4=4", "smoke rows stay", "frozen", "live-walk cross-check"},
				"unboxinator", new String[] {"Loader_Recipes_Handlers.java:451-489", ":619-626", "wiregt01-16", ":622", ":621", "loom.json", "crategt", ":458/:459", "mt.empty", "frozen", "live-walk cross-check"},
				"boxinator", new String[] {"Loader_Recipes_Handlers.java:492-550", "st.tag", "selector", "loom.json", ":495-509", ":511-525", ":492/:493", "crategt", "il.crate_fireproof", "frozen", "live-walk cross-check"});
		for (Map.Entry<String, String[]> tPin : tPins.entrySet()) {
			InputStream tStream = GT6RecipeMapDataB7PackUnboxRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tPin.getKey() + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			assertTrue(tDoc.has("comment"), tPin.getKey() + ": the file head carries a comment member");
			String tLower = tDoc.get("comment").getAsString().toLowerCase(java.util.Locale.ROOT);
			for (String tNeedle : tPin.getValue()) assertTrue(tLower.contains(tNeedle.toLowerCase(java.util.Locale.ROOT)),
					tPin.getKey() + ": head declares \"" + tNeedle + "\"");
		}
	}

	/**
	 * The seated/walk split: a walk row = no comment member AND a gt6: input slot (the material
	 * prefix rows; the only uncommented seated rows are the two map smoke rows with vanilla
	 * inputs). Walk rows carry no comment (the cut scale ruling); the seated counts hold.
	 */
	@Test
	public void theWalkRowsCarryNoCommentAndTheSeatedRowsStay() throws Exception {
		Map<String, JsonArray> tRows = pourShipped();
		Map<String, Integer> tWalk = Map.of("rollingmill", 2432, "rollbender", 932, "boxinator", 12319, "unboxinator", 11291);
		for (Map.Entry<String, Integer> tPin : tWalk.entrySet()) {
			int tWalkRows = 0;
			for (JsonElement tElement : tRows.get(tPin.getKey())) {
				JsonObject tRow = tElement.getAsJsonObject();
				boolean tGt6Input = tRow.has("inputs") && tRow.getAsJsonArray("inputs").size() > 0
						&& tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString().startsWith("gt6:");
				if (!tRow.has("comment") && tGt6Input) {
					tWalkRows++;
				} else {
					// the seated stock: commented (smoke + prior-card rows) or the vanilla-input map smoke rows
					assertTrue(tRow.has("comment") || !tGt6Input, tPin.getKey() + ": an uncommented gt6-input row outside the walk census");
				}
			}
			assertEquals(tPin.getValue().intValue(), tWalkRows, tPin.getKey() + ": the walk-row snapshot census");
		}
	}

	/**
	 * Verbatim spot checks — thirteen rows across the four domains, field-level: the hard-arm
	 * getCosts durations over the mToolQuality ladder (iron q=2: 256x3, the U/8 fine-wire 192,
	 * the U/2 stick 384, the 9U dense-plate 6912, the U9 nugget ceil(768/9)=86, the 5U quintuple
	 * 3840), the easy-arm fixed durations (gold 16, stick 16/4=4), and the Box/Unbox 16/16 rows
	 * over their verbatim counts (dustSmall>dustDiv72x18, dustTiny>dustDiv72x8, ingotx2>billetx3,
	 * plateTinyx9>casingSmallx2, the bullet dustTiny out-counts, the MT.Empty-absent single slot).
	 */
	@Test
	public void statementRowsAreUpstreamVerbatim() throws Exception {
		Map<String, JsonArray> tRows = pourShipped();

		// :264 hard arm — ingot 1U -> plate 1U, eut 16, ceil(U*256*3/U) = 768 (iron q=2)
		assertRow(tRows, "rollingmill", "gt6:ingot_iron", 1, "gt6:plate_iron", 1, 768, 16);
		// :262 hard arm — nugget U/9 -> plateTiny U/9, ceil(256*3/9) = 86
		assertRow(tRows, "rollingmill", "gt6:nugget_iron", 1, "gt6:plate_tiny_iron", 1, 86, 16);
		// :268 hard arm — ingotQuintuple 5U -> plateQuintuple 5U, 5*768 = 3840
		assertRow(tRows, "rollingmill", "gt6:ingot_quintuple_iron", 1, "gt6:plate_quintuple_iron", 1, 3840, 16);
		// :269 hard arm — blockSolid 9U -> plateDense 9U, 9*768 = 6912
		assertRow(tRows, "rollingmill", "gt6:block_solid_iron", 1, "gt6:plate_dense_iron", 1, 6912, 16);
		// :276 easy arm — gold rides FURNACE/SOFT, the fixed 16
		assertRow(tRows, "rollingmill", "gt6:ingot_gold", 1, "gt6:plate_gold", 1, 16, 16);
		// :283 easy arm — plateCurved -> plate, fixed 16
		assertRow(tRows, "rollingmill", "gt6:plate_curved_gold", 1, "gt6:plate_gold", 1, 16, 16);
		// :298 hard arm — plate -> plateCurved, 768
		assertRow(tRows, "rollbender", "gt6:plate_iron", 1, "gt6:plate_curved_iron", 1, 768, 16);
		// :299 hard arm — stick U/2 -> ring x2 (2 x U/2 = U, max stays U/2), 768/2 = 384
		assertRow(tRows, "rollbender", "gt6:stick_iron", 1, "gt6:ring_iron", 2, 384, 16);
		// :300 hard arm — stickLong U -> spring U, 768
		assertRow(tRows, "rollbender", "gt6:stick_long_iron", 1, "gt6:spring_iron", 1, 768, 16);
		// :301 hard arm — wireFine x2 (2 x U/8 = U/4) -> springSmall x1, 768/4 = 192
		assertRow(tRows, "rollbender", "gt6:wire_fine_iron", 2, "gt6:spring_small_iron", 1, 192, 16);
		// :304 easy arm — stick -> ring x2, the fixed 16/4 = 4
		assertRow(tRows, "rollbender", "gt6:stick_gold", 1, "gt6:ring_gold", 2, 4, 16);
		// :485 — ingot -> nugget x9, eut 16 dur 16
		assertRow(tRows, "unboxinator", "gt6:ingot_iron", 1, "gt6:nugget_iron", 9, 16, 16);
		// :488 — dustSmall -> dustDiv72 x18 (the OM ladder pair, verbatim out-count)
		assertRow(tRows, "unboxinator", "gt6:dust_small_iron", 1, "gt6:dust_div72_iron", 18, 16, 16);
		// :489 — dustTiny -> dustDiv72 x8
		assertRow(tRows, "unboxinator", "gt6:dust_tiny_iron", 1, "gt6:dust_div72_iron", 8, 16, 16);
		// :454 — bulletGtSmall -> dustTiny x1, the MT.Empty additional-output slot absent both sides
		assertRow(tRows, "unboxinator", "gt6:bullet_gt_small_lead", 1, "gt6:dust_tiny_lead", 1, 16, 16);
		// :545 — ingot x2 -> billet x3 (the ST.tag(6) selector dropped)
		assertRow(tRows, "boxinator", "gt6:ingot_iron", 2, "gt6:billet_iron", 3, 16, 16);
		// :549 — plateTiny x9 -> casingSmall x2 (the ST.tag(9) selector dropped)
		assertRow(tRows, "boxinator", "gt6:plate_tiny_iron", 9, "gt6:casing_small_iron", 2, 16, 16);
		// :527 — dustSmall x36 -> blockDust x1
		assertRow(tRows, "boxinator", "gt6:dust_small_iron", 36, "gt6:block_dust_iron", 1, 16, 16);
	}

	/** Full-row pin: single input slot (item+count) -> single output slot (item+count), duration, eut. */
	private static void assertRow(Map<String, JsonArray> aRows, String aKey, String aIn, int aInCount, String aOut, int aOutCount, int aDuration, int aEut) {
		for (JsonElement tElement : aRows.get(aKey)) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("inputs") || tRow.getAsJsonArray("inputs").size() != 1) continue;
			JsonObject tIn = tRow.getAsJsonArray("inputs").get(0).getAsJsonObject();
			if (!aIn.equals(tIn.get("item").getAsString()) || aInCount != tIn.get("count").getAsInt()) continue;
			JsonArray tOuts = tRow.getAsJsonArray("outputs");
			if (tOuts.size() != 1 || !aOut.equals(tOuts.get(0).getAsJsonObject().get("item").getAsString())
					|| aOutCount != tOuts.get(0).getAsJsonObject().get("count").getAsInt()) continue;
			// the full six-field signature: the seated smoke row shares the in/out pair (rollingmill) — the duration disambiguates
			if (aDuration != tRow.get("duration").getAsLong() || aEut != tRow.get("eut").getAsLong()) continue;
			assertEquals(aDuration, tRow.get("duration").getAsLong(), aKey + " " + aIn + ": the duration");
			assertEquals(aEut, tRow.get("eut").getAsLong(), aKey + " " + aIn + ": the eut");
			return;
		}
		throw new AssertionError("no row " + aKey + " " + aIn + "x" + aInCount + " -> " + aOut + "@" + aDuration);
	}

	/**
	 * The live-walk cross-check — the heart of the frozen-snapshot deviation: recompute all
	 * four walks (22 + 8 + 34 + 55 statements over the LIVE GTMaterialItems ∪ GTMaterialBlocks
	 * registration, the RecipeMapHandlerPrefix gates: the per-statement conditions + INVALID_MATERIAL
	 * :205, both-side item existence :209/:214, the hard-arm getCosts :225-227 at multiplier 256,
	 * the easy-arm fixed durations) and require the shipped walk rows to cover the recomputation
	 * EXACTLY, both directions. The seated rows ride outside this check (their census is pinned
	 * separately), and the zero-pour faces never enter tExpected (input/output gates fail).
	 */
	@Test
	public void theShippedWalkMatchesTheLiveRegistrationWalk() throws Exception {
		GTMaterialItems.initMaterials();
		Map<String, JsonArray> tShippedByFile = pourShipped();
		List<GTMaterialItems.PrefixMaterial> tOrder = new ArrayList<>(GTMaterialItems.registrationOrder());
		tOrder.addAll(GTMaterialBlocks.registrationOrder());
		Set<String> tRegKeys = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : tOrder) tRegKeys.add(tPair.prefix().mNameInternal + "|" + tPair.material().mNameInternal);

		Map<String, Set<String>> tExpected = new HashMap<>();
		tExpected.put("rollingmill", walkKeys(rollingMillStatements(), tOrder, tRegKeys));
		tExpected.put("rollbender", walkKeys(rollBenderStatements(), tOrder, tRegKeys));
		tExpected.put("unboxinator", walkKeys(null, unboxinatorStatements(), tOrder, tRegKeys));
		tExpected.put("boxinator", walkKeys(null, boxinatorStatements(), tOrder, tRegKeys));

		Map<String, Set<String>> tShipped = new HashMap<>();
		for (Map.Entry<String, JsonArray> tFile : tShippedByFile.entrySet()) {
			Set<String> tKeys = new HashSet<>();
			int tSeated = 0;
			for (JsonElement tElement : tFile.getValue()) {
				JsonObject tRow = tElement.getAsJsonObject();
				boolean tGt6Input = tRow.has("inputs") && tRow.getAsJsonArray("inputs").size() > 0
						&& tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString().startsWith("gt6:");
				if (tRow.has("comment") || !tGt6Input) {tSeated++; continue;} // the seated stock rides its own pins
				tKeys.add(rowKey(tRow));
			}
			assertEquals(CENSUS.get(tFile.getKey()).intValue() - walkCount(tFile.getKey()), tSeated, tFile.getKey() + ": the seated census");
			tShipped.put(tFile.getKey(), tKeys);
		}

		for (String tKey : CENSUS.keySet()) {
			Set<String> tMissing = new HashSet<>(tExpected.get(tKey));
			tMissing.removeAll(tShipped.get(tKey));
			Set<String> tStale = new HashSet<>(tShipped.get(tKey));
			tStale.removeAll(tExpected.get(tKey));
			assertTrue(tMissing.isEmpty(), tKey + ": the frozen snapshot trails the live walk (re-pour needed): " + tMissing);
			assertTrue(tStale.isEmpty(), tKey + ": the frozen snapshot holds walk rows the live walk no longer generates: " + tStale);
			assertEquals(tExpected.get(tKey).size(), tShipped.get(tKey).size(), tKey + ": the snapshot size equals the live walk");
		}
	}

	/** The id faces: every gt6 ITEM id lives in GTMaterialItems ∪ GTMaterialBlocks; zero fluid members. */
	@Test
	public void everyItemIdTheFileReferencesIsRegistered() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tItemUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) tItemUniverse.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) tItemUniverse.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		// task planks-blockification — the plank prefix items retired; the plank face is the
		// GT6WoodDict BlockItem rows (the ONE plank authority, the GT6TreeBlocks cube family),
		// so the 17 gt6 family ids join the universe (the unboxinator plank rows pour onto it;
		// this universe carries BARE paths — the gt6: prefix is stripped at the check below)
		for (gregtech6.registry.GT6WoodDict.PlankEntry tPlank : gregtech6.registry.GT6WoodDict.GT6_ROWS) {
			tItemUniverse.add(tPlank.id());
		}
		Set<String> tMissing = new HashSet<>();
		int tFluidSlots = 0;
		// the seat14 rebase roll: boxinator now also carries main's seated robotics-chain rows
			 // (task robotics-chain landed after this card's cut) — their tip-template/token ids are
			 // IL items OUTSIDE the material universe, guarded by GT6RoboticsRowsPourTest; the walk
			 // rows stay fully pinned against the material universe
		java.util.Set<String> tSeatedIlIds = new HashSet<>();
		for (String tKind : new String[] {"wrench", "screwdriver", "saw", "hammer", "cutter", "chisel", "rubber", "blade", "drill", "file"}) {
			tSeatedIlIds.add("gt6:robot_arm_" + tKind + "_tip");
			tSeatedIlIds.add("gt6:single_use_" + tKind);
		}
		for (JsonElement tElement : pourShipped().values()) {
			for (String tLeg : new String[] {"inputs", "outputs"}) for (JsonElement tSlot : tElement.getAsJsonArray()) {
				JsonObject tRow = tSlot.getAsJsonObject();
				if (tRow.has("fluidInputs")) tFluidSlots += tRow.getAsJsonArray("fluidInputs").size();
				if (tRow.has("fluidOutputs")) tFluidSlots += tRow.getAsJsonArray("fluidOutputs").size();
				for (JsonElement tSlot2 : tRow.getAsJsonArray(tLeg)) {
					String tId = tSlot2.getAsJsonObject().get("item").getAsString();
					if (tSeatedIlIds.contains(tId)) continue;
					if (tId.startsWith("gt6:") && !tItemUniverse.contains(tId.substring(4))) tMissing.add(tId);
				}
			}
		}
		assertEquals(0, tFluidSlots, "the four walk faces are item-only rows");
		assertTrue(tMissing.isEmpty(), "unregistered gt6 item ids (these rows would WARN-skip live): " + tMissing);
	}

	/**
	 * The zero-pour ratchet: the absent faces stay absent — recomputed LIVE so a future port of
	 * any of them turns this pin red and demands the follow-up pour (the pool gate). Covers the
	 * wire face :619-626 (both legs), the crate family, the pipe family, and the three
	 * RollingMill output faces.
	 */
	@Test
	public void theZeroPourFacesStayTrue() throws Exception {
		GTMaterialItems.initMaterials();
		List<GTMaterialItems.PrefixMaterial> tOrder = new ArrayList<>(GTMaterialItems.registrationOrder());
		tOrder.addAll(GTMaterialBlocks.registrationOrder());
		Map<String, Integer> tLive = new HashMap<>();
		// the wireGt01/02/16 faces LEFT the zero-pin (the seat14 rebase roll): task wire-family's
			// 4943f338e registered the OP.wireGt01-16 item path after this card's cut, so the :619-626
			// wire pour is the wire-family card's pool-due face, not a zero face — the still-absent
			// carriers (pipes, crates, the press RollingMill outputs) keep the zero-pin
			for (String tName : new String[] {"plateSteamcraft", "sheetGt", "compressed", "crateGtRaw", "crateGt64Raw",
					"pipeSmall", "pipeMedium", "pipeQuadruple", "pipeNonuple"}) {
			OreDictPrefix tPrefix = prefix(tName);
			int tCount = 0;
			for (GTMaterialItems.PrefixMaterial tPair : tOrder) if (tPair.prefix() == tPrefix) tCount++;
			tLive.put(tName, tCount);
		}
		for (Map.Entry<String, Integer> tFace : tLive.entrySet()) assertEquals(0, tFace.getValue().intValue(),
				tFace.getKey() + ": the zero-pour face gained live registrations — the follow-up pour (pool) is due");
		// the :619-626 walk census: 34 divisor pairs per leg (tBig 1..16 x tSmall < tBig, tBig % tSmall == 0) stay absent on both legs
		int tDivisorPairs = 0;
		for (int tBig = 1; tBig <= 16; tBig++) for (int tSmall = 1; tSmall < tBig; tSmall++) if (tBig % tSmall == 0) tDivisorPairs++;
		assertEquals(34, tDivisorPairs, "the upstream :619-626 divisor-pair census (per leg — Loom :621 + Unboxinator :622)");
		// and the shipped files hold no wireGt/pipe/crate row at all
		for (JsonArray tFile : pourShipped().values()) for (JsonElement tRowElement : tFile) {
			String tRow = tRowElement.getAsJsonObject().toString();
			assertTrue(!tRow.contains("wire_gt0") && !tRow.contains("\"gt6:pipe_") && !tRow.contains("crate_gt"),
					"no zero-pour-face row shipped");
		}
	}

	// ------------------------------------------------------------- the walk engine (RecipeMapHandlerPrefix semantics)

	/** cond flags: 1=ANTIMATTER.NOT, 2=COATED.NOT, 4=SMITHABLE, 8=MT.Empty.NOT, 16=EXPLODES_NOT, 32=easyArm, 64=hardArm */
	private static final int ANTIMATTER_NOT = 1, COATED_NOT = 2, SMITHABLE = 4, EXPLODES_NOT = 16, EASY = 32, HARD = 64;

	private static final String[] IDX = {"nugget", "billet", "ingot", "ingotDouble", "ingotTriple", "ingotQuadruple", "ingotQuintuple",
			"blockSolid", "compressed", "plateCurved", "plate", "plateTiny", "plateSteamcraft", "plateDouble", "plateTriple",
			"plateQuadruple", "plateQuintuple", "plateDense", "sheetGt", "stick", "stickLong", "ring", "spring", "springSmall", "wireFine"};

	/** {line, inIdx, inCount, outIdx, outCount} — the hard arm first (:262-272), then the easy arm (:274-284). */
	private static List<long[]> rollingMillStatements() {
		int[][] tPairs = {{262, 0, 11}, {263, 1, 12}, {264, 2, 10}, {265, 3, 13}, {266, 4, 14}, {267, 5, 15}, {268, 6, 16},
				{269, 7, 17}, {270, 8, 10}, {271, 9, 10}, {272, 10, 18}};
		int[] tEasyDur = {16 / 9, 32 / 3, 16, 16 * 2, 16 * 3, 16 * 4, 16 * 5, 16 * 9, 16, 16, 16};
		List<long[]> r = new ArrayList<>();
		for (int[] tPair : tPairs) r.add(new long[] {tPair[0], tPair[1], 1, tPair[2], 1, 16, 0, ANTIMATTER_NOT | COATED_NOT | SMITHABLE | HARD});
		for (int i = 0; i < tPairs.length; i++) r.add(new long[] {274 + i, tPairs[i][1], 1, tPairs[i][2], 1, 16, tEasyDur[i], ANTIMATTER_NOT | COATED_NOT | SMITHABLE | EASY});
		return r;
	}

	/** {line, inIdx, inCount, outIdx, outCount} — :298-301 hard, :303-306 easy (NO COATED leg). */
	private static List<long[]> rollBenderStatements() {
		int[][] tPairs = {{298, 10, 9, 1, 1}, {299, 19, 21, 1, 2}, {300, 20, 22, 1, 1}, {301, 24, 23, 2, 1}};
		int[] tEasyDur = {16, 16 / 4, 16, 16 / 4};
		List<long[]> r = new ArrayList<>();
		for (int[] tPair : tPairs) r.add(new long[] {tPair[0], tPair[1], tPair[3], tPair[2], tPair[4], 16, 0, ANTIMATTER_NOT | SMITHABLE | HARD});
		for (int i = 0; i < tPairs.length; i++) r.add(new long[] {303 + i, tPairs[i][1], tPairs[i][3], tPairs[i][2], tPairs[i][4], 16, tEasyDur[i], ANTIMATTER_NOT | SMITHABLE | EASY});
		return r;
	}

	/** {line, inName, inCount, outName, outCount, cond} — eut 16 / dur 16 fixed. */
	private static List<Object[]> boxUnboxStatements() {
		List<Object[]> r = new ArrayList<>();
		r.add(new Object[] {451, "toolHeadPickaxeGem", 1, "gemFlawed", 2, ANTIMATTER_NOT | 8});
		r.add(new Object[] {452, "arrowGtWood", 1, "toolHeadArrow", 1, ANTIMATTER_NOT | 8});
		r.add(new Object[] {453, "arrowGtPlastic", 1, "toolHeadArrow", 1, ANTIMATTER_NOT | 8});
		r.add(new Object[] {454, "bulletGtSmall", 1, "dustTiny", 1, ANTIMATTER_NOT | 8});
		r.add(new Object[] {455, "bulletGtMedium", 1, "dustTiny", 2, ANTIMATTER_NOT | 8});
		r.add(new Object[] {456, "bulletGtLarge", 1, "dustTiny", 3, ANTIMATTER_NOT | 8});
		r.add(new Object[] {458, "pipeQuadruple", 1, "pipeMedium", 4, ANTIMATTER_NOT});
		r.add(new Object[] {459, "pipeNonuple", 1, "pipeSmall", 9, ANTIMATTER_NOT});
		String[] tCrates = {"crateGtRaw>oreRaw", "crateGtDust>dust", "crateGtGem>gem", "crateGtIngot>ingot", "crateGtPlate>plate", "crateGtPlateGem>plateGem"};
		for (String tC : tCrates) r.add(new Object[] {461, face(tC, 0), 1, face(tC, 1), 16, ANTIMATTER_NOT});
		String[] tCrates64 = {"crateGt64Raw>oreRaw", "crateGt64Dust>dust", "crateGt64Gem>gem", "crateGt64Ingot>ingot", "crateGt64Plate>plate", "crateGt64PlateGem>plateGem"};
		for (String tC : tCrates64) r.add(new Object[] {468, face(tC, 0), 1, face(tC, 1), 64, ANTIMATTER_NOT});
		String[] tBlocks = {"blockRaw>oreRaw", "blockDust>dust", "blockGem>gem", "blockIngot>ingot", "blockPlate>plate", "blockPlateGem>plateGem"};
		int tLine = 475;
		for (String tB : tBlocks) r.add(new Object[] {tLine++, face(tB, 0), 1, face(tB, 1), 9, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {481, "crushed", 1, "crushedTiny", 9, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {482, "crushedPurified", 1, "crushedPurifiedTiny", 9, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {483, "crushedCentrifuged", 1, "crushedCentrifugedTiny", 9, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {485, "ingot", 1, "nugget", 9, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {486, "billet", 1, "nugget", 6, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {487, "dust", 1, "dustTiny", 9, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {488, "dustSmall", 1, "dustDiv72", 18, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {489, "dustTiny", 1, "dustDiv72", 8, ANTIMATTER_NOT | EXPLODES_NOT});
		// ---- the Boxinator half (:492-550, negative lines mark the file) ----
		r.add(new Object[] {-492, "pipeMedium", 4, "pipeQuadruple", 1, ANTIMATTER_NOT});
		r.add(new Object[] {-493, "pipeSmall", 9, "pipeNonuple", 1, ANTIMATTER_NOT});
		r.add(new Object[] {-495, "dustSmall", 64, "crateGtDust", 1, ANTIMATTER_NOT});
		r.add(new Object[] {-496, "chunkGt", 64, "crateGtIngot", 1, ANTIMATTER_NOT});
		r.add(new Object[] {-497, "billet", 24, "crateGtIngot", 1, ANTIMATTER_NOT});
		String[] tCrate64Out = {"oreRaw>crateGt64Raw", "dust>crateGt64Dust", "gem>crateGt64Gem", "ingot>crateGt64Ingot", "plate>crateGt64Plate", "plateGem>crateGt64PlateGem"};
		int tLine2 = 498;
		for (String tC : tCrate64Out) r.add(new Object[] {-tLine2, face(tC, 0), 64, face(tC, 1), 1, ANTIMATTER_NOT});
		int tLine3 = 504;
		for (String tB : tBlocks) r.add(new Object[] {-tLine3, face(tB, 0), 64, tCrate64Out[List.of("blockRaw", "blockDust", "blockGem", "blockIngot", "blockPlate", "blockPlateGem").indexOf(face(tB, 0))].split(">")[1], 9, ANTIMATTER_NOT});
		r.add(new Object[] {-511, "dustSmall", 64, "crateGtDust", 1, ANTIMATTER_NOT});
		r.add(new Object[] {-512, "chunkGt", 64, "crateGtIngot", 1, ANTIMATTER_NOT});
		r.add(new Object[] {-513, "billet", 24, "crateGtIngot", 1, ANTIMATTER_NOT});
		tLine2 = 514;
		for (String tC : tCrate64Out) r.add(new Object[] {-tLine2, face(tC, 0), 64, face(tC, 1), 1, ANTIMATTER_NOT});
		tLine3 = 520;
		for (String tB : tBlocks) r.add(new Object[] {-tLine3, face(tB, 0), 64, tCrate64Out[List.of("blockRaw", "blockDust", "blockGem", "blockIngot", "blockPlate", "blockPlateGem").indexOf(face(tB, 0))].split(">")[1], 9, ANTIMATTER_NOT});
		r.add(new Object[] {-527, "dustSmall", 36, "blockDust", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-528, "oreRaw", 9, "blockRaw", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-529, "dust", 9, "blockDust", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-530, "gem", 9, "blockGem", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-531, "chunkGt", 36, "blockIngot", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-532, "billet", 27, "blockIngot", 2, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-533, "ingot", 9, "blockIngot", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-534, "plate", 9, "blockPlate", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-535, "plateGem", 9, "blockPlateGem", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-536, "crushedTiny", 9, "crushed", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-537, "crushedPurifiedTiny", 9, "crushedPurified", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-538, "crushedCentrifugedTiny", 9, "crushedCentrifuged", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-540, "dustDiv72", 8, "dustTiny", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-541, "dustDiv72", 18, "dustSmall", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-542, "dustTiny", 9, "dust", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-543, "dustSmall", 4, "dust", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-544, "nugget", 6, "billet", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-545, "ingot", 2, "billet", 3, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-546, "nugget", 9, "ingot", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-547, "chunkGt", 4, "ingot", 1, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-548, "billet", 3, "ingot", 2, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-549, "plateTiny", 9, "casingSmall", 2, ANTIMATTER_NOT | EXPLODES_NOT});
		r.add(new Object[] {-550, "plateGemTiny", 9, "casingSmall", 2, ANTIMATTER_NOT | EXPLODES_NOT});
		return r;
	}

	private static String face(String aPair, int aIdx) {return aPair.split(">")[aIdx];}

	private static int walkCount(String aKey) {
		return switch (aKey) {case "rollingmill" -> 2432; case "rollbender" -> 932; case "boxinator" -> 12319; default -> 11291;};
	}

	private static List<Object[]> unboxinatorStatements() {
		List<Object[]> r = new ArrayList<>();
		for (Object[] tStmt : boxUnboxStatements()) if (((int) tStmt[0]) > 0) r.add(tStmt);
		return r;
	}

	private static List<Object[]> boxinatorStatements() {
		List<Object[]> r = new ArrayList<>();
		for (Object[] tStmt : boxUnboxStatements()) if (((int) tStmt[0]) < 0) r.add(tStmt);
		return r;
	}

	private static OreDictPrefix prefix(String aName) {
		try {return (OreDictPrefix) OP.class.getField(aName).get(null);}
		catch (Exception e) {throw new IllegalArgumentException(aName, e);}
	}

	/** Recomputes one statement table over the live registration; returns the row-key set. */
	private static Set<String> walkKeys(List<long[]> aRolling, List<Object[]> aBoxUnbox, List<GTMaterialItems.PrefixMaterial> aOrder, Set<String> aRegKeys) {
		Set<String> rKeys = new HashSet<>();
		List<Object[]> tStmts = new ArrayList<>();
		if (aRolling != null) for (long[] tStmt : aRolling) tStmts.add(new Object[] {tStmt[0], IDX[(int) tStmt[1]], (int) tStmt[2], IDX[(int) tStmt[3]], (int) tStmt[4], (int) tStmt[5], (int) tStmt[6], (int) tStmt[7]});
		if (aBoxUnbox != null) for (Object[] tStmt : aBoxUnbox) tStmts.add(new Object[] {tStmt[0], tStmt[1], tStmt[2], tStmt[3], tStmt[4], 16, 16, tStmt[5]});
		for (Object[] tStmt : tStmts) {
			OreDictPrefix tIn = prefix((String) tStmt[1]), tOut = prefix((String) tStmt[3]);
			int tInCount = (Integer) tStmt[2], tOutCount = (Integer) tStmt[4], tEut = (Integer) tStmt[5], tDurFixed = (Integer) tStmt[6], tCond = (Integer) tStmt[7];
			Set<String> tSeen = new HashSet<>();
			for (GTMaterialItems.PrefixMaterial tPair : aOrder) {
				if (tPair.prefix() != tIn) continue;
				OreDictMaterial tMat = tPair.material();
				if (!tSeen.add(tMat.mNameInternal)) continue;
				if (!condition(tCond, tMat)) continue; // :205
				if (tMat.contains(TD.Properties.INVALID_MATERIAL)) continue; // :205
				if (!aRegKeys.contains(tIn.mNameInternal + "|" + tMat.mNameInternal)) continue; // :209
				if (!aRegKeys.contains(tOut.mNameInternal + "|" + tMat.mNameInternal)) continue; // :214
				long tDur = tDurFixed > 0 ? tDurFixed : costs(tIn, tInCount, tOut, tOutCount, 256, tMat);
				rKeys.add("gt6:" + GTMaterialItems.itemIdOf(tIn, tMat) + "x" + tInCount + ">gt6:" + GTMaterialItems.itemIdOf(tOut, tMat) + "x" + tOutCount + "@" + tDur + "@" + tEut);
			}
		}
		return rKeys;
	}

	private static Set<String> walkKeys(List<long[]> aRolling, List<GTMaterialItems.PrefixMaterial> aOrder, Set<String> aRegKeys) {
		return walkKeys(aRolling, null, aOrder, aRegKeys);
	}

	/** Serializes one shipped walk row into the same key form (no comment members on walk rows). */
	private static String rowKey(JsonObject aRow) {
		JsonObject tIn = aRow.getAsJsonArray("inputs").get(0).getAsJsonObject();
		StringBuilder rKey = new StringBuilder(tIn.get("item").getAsString() + "x" + tIn.get("count").getAsInt());
		JsonArray tOuts = aRow.getAsJsonArray("outputs");
		rKey.append('>'); // the single-output shape (the MT.Empty slots are null both sides)
		for (JsonElement tOut : tOuts) {
			JsonObject tSlot = tOut.getAsJsonObject();
			rKey.append(tSlot.get("item").getAsString()).append('x').append(tSlot.get("count").getAsInt());
		}
		rKey.append('@').append(aRow.get("duration").getAsLong()).append('@').append(aRow.get("eut").getAsLong());
		return rKey.toString();
	}

	/** The per-statement condition face (:205 aCondition + the tEasyWorkable arm split). */
	private static boolean condition(int aCond, OreDictMaterial aMat) {
		if ((aCond & ANTIMATTER_NOT) != 0 && aMat.contains(TD.Atomic.ANTIMATTER)) return false;
		if ((aCond & COATED_NOT) != 0 && aMat.contains(TD.Compounds.COATED)) return false;
		if ((aCond & SMITHABLE) != 0 && !aMat.contains(TD.Processing.SMITHABLE)) return false;
		if ((aCond & 8) != 0 && aMat == gregapi.data.MT.Empty) return false;
		if ((aCond & EXPLODES_NOT) != 0 && aMat.contains(TD.Properties.EXPLODES_IN_NONVANILLA_CRAFTING_GRID)) return false;
		boolean tEasy = aMat.contains(TD.Processing.FURNACE) || aMat.contains(TD.Properties.SOFT);
		if ((aCond & EASY) != 0 && !tEasy) return false;
		if ((aCond & HARD) != 0 && tEasy) return false;
		return true;
	}

	/** RecipeMapHandlerPrefix.getCosts :225-227 — the Wiremill/ShCL transcription, round-up. */
	private static long costs(OreDictPrefix aIn, int aInCount, OreDictPrefix aOut, int aOutCount, long aMultiplier, OreDictMaterial aMat) {
		long tUnitsIn = aIn.mAmount * aInCount, tUnitsOut = aOut.mAmount * aOutCount;
		long tAmount = Math.max(tUnitsIn, tUnitsOut);
		long tTarget = aMultiplier + aMultiplier * aMat.mToolQuality;
		if (tTarget == 0) return 0;
		return Math.max(0, tAmount * tTarget / CS.U + ((tAmount * tTarget) % CS.U > 0 ? 1 : 0));
	}
}
