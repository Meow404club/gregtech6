package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

/**
 * The recipe-data-b1 row-stock pour test (the GT6HammerRowsPourTest fixture posture): the
 * ten shipped recipe-map files of this card — the two vacuum-map replays (fluidbed,
 * press) plus the eight smoke-map expansions — pour through the real
 * {@link GT6RecipeMapJsonLoader} seam with zero skips (the acceptance "灌入行数 per 图钉"
 * census), and the verbatim spot checks read the shipped row objects directly (the
 * acceptance "抽验 3 图行参数对上游" — the file IS the transcription artifact; the
 * identity stand-in resolver cannot carry gt6 id faces, so the JSON face is the honest
 * assertion surface, with the pour proving every id resolves live at the seam).
 */
public class GT6RecipeMapDataB1RowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/**
	 * The per-map census of this card: file key -> expected poured rows (zero skips).
	 * REBASE SEAM (task recipe-data-b1, seat XIII re-adjudication): four of the eight
	 * smoke-map files and the unboxinator file are SHARED with main's landed cards —
	 * the seated smoke rows and the b2-residual walks stay in the files (the branch's
	 * wholesale-file form dropped them; the union ruling keeps both), so the census
	 * counts the whole file: seated + landed + this card's rows. The lightning and
	 * cryomixer files absorb this card's candidates entirely (identical rows / corrected
	 * salt legs), so their census is the landed file's.
	 */
	private static final Map<String, Integer> CENSUS = Map.of(
			"fluidbed", 55,    // Loader_Fuels.java:37-43 — 11 burning materials x 5 dust forms
			"press", 89,       // Loader_Recipes_Vanilla.java:776-799 — 2x10 lamp + 4x10 TNT +4 toolhead-r11e-press-mortar (the HandlerPrefix gem-pickaxe/arrow walk representatives, Handlers:247-250/:252-253) +1 toolhead-family-closeout (:251 Empty-head retip; :254-259 stay pooled) +24 explosives-chain (Loader_Recipes_Other.java:641-655, the dynamite/boomstick rows; :657-668 casings stay pooled)
			"loom", 51,        // seated :744 (16t corrected) + b2 walks 26 + card :745/:746/:752/:753/:757-:760 (8) + the b4 :736 dyed band 16 (task recipe-b4-juicer-squeezer-flowerfruit)
			"boxinator", 12358,   // seated GT6_Main:350 map row + this card's 28 + task robotics-chain's 10 tip-packing rows (MultiItemRandomTools.java:503-512) + the recipe-b7 pack walk 12319
			"unboxinator", 11312, // seated map row + b2 bookshelf row + this card's 19 + the recipe-b7 unbox walk 11291
			"lightning", 8,    // the b2 eight (certus identical; the salt legs corrected in place)
			"freezer", 10,     // seated smoke row + this card's 9
			"cryomixer", 62,   // the b2 census 1+37+24 (this card's six water rows ride it, anchor comments kept)
			"coagulator", 6,   // seated fluid row + this card's 5
			"sharpening", 7574);  // seated smoke row + the B1 Vanilla pair + the r11b walk replay 7571 (task toolhead-r11b-sharpening-rows: the Handlers :396-415 statement-groups expanded per material)

	@BeforeEach
	void freshGeneration() {
		// the reset FIRST (task toolhead-r11e-press-mortar): the neoforge junit FML boot runs
		// the whole static pour suite at modloading, so a bare init() no-ops there and the
		// census would count boot residue (the MortarRowsPourTest empirics; PRESS/MORTAR boot
		// pours are 0 today only by the declared-empty accident). Same hermetic form as the
		// @AfterEach reset below.
		GT6RecipeMaps.reset();
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
		InputStream tStream = GT6RecipeMapDataB1RowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: every file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void everyB1FilePoursItsFullCensusWithZeroSkips() throws Exception {
		for (String tKey : CENSUS.keySet()) {
			pourShipped(tKey);
			RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(tKey);
			assertNotNull(tMap, tKey + " resolves");
			assertEquals(CENSUS.get(tKey).intValue(), tMap.mRecipeList.size(), tKey + ": the map holds the census");
			assertEquals(CENSUS.get(tKey).intValue(), GT6RecipeMapJsonLoader.pouredCount(tKey),
					tKey + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
		}
	}

	/** The press key joins the pourable set with this card; the crucible pair stays keyless. */
	@Test
	public void thePressKeyIsPourableAndTheCruciblePairStaysKeyless() {
		assertNotNull(GT6RecipeMapJsonLoader.mapFor("press"), "press resolves to GT6RecipeMaps.PRESS");
		assertNull(GT6RecipeMapJsonLoader.mapFor("cruciblesmelting"), "zero upstream static rows — the dynamic arm lives in GT6RecipeMapCrucible");
		assertNull(GT6RecipeMapJsonLoader.mapFor("cruciblealloying"), "zero upstream static rows — the dynamic arm lives in GT6RecipeMapCrucible");
	}

	/**
	 * Spot check 1 — the fluidbed coal dust leg (Loader_Fuels.java:39, the Coal iteration):
	 * duration 1600x3x25 = 120000, eut -1, Calcite 72 mB, output = OM.dust(DarkAsh, U8) =
	 * 9x dust_div72_dark_ash (the OM.java:460-468 ladder at the dust-leg amount).
	 */
	@Test
	public void fluidbedCoalDustLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("fluidbed");
		JsonObject tRow = findRow(tRows, "Loader_Fuels.java:39 — Coal dust leg");
		assertEquals(120000, tRow.get("duration").getAsLong(), "1600 x 3 x 25 (CS.java:210-212)");
		assertEquals(-1, tRow.get("eut").getAsLong(), "the fuel-map burn semantics");
		assertEquals("gt6:calcite_molten", slotId(tRow.getAsJsonArray("fluidInputs").get(0)), "FL.Calcite (molten.calcite, FL.java:458)");
		assertEquals(72, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(), "FL.Calcite.make(72)");
		assertEquals(1, tRow.getAsJsonArray("inputs").size());
		assertEquals("gt6:dust_coal", slotId(tRow.getAsJsonArray("inputs").get(0)));
		assertEquals(1, tRow.getAsJsonArray("outputs").size());
		assertEquals("gt6:dust_div72_dark_ashes", slotId(tRow.getAsJsonArray("outputs").get(0)), "the OM.dust ladder at U8");
		assertEquals(9, tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
	}

	/**
	 * Spot check 2 — the press glowstone blockDust lamp leg (Loader_Recipes_Vanilla.java:777):
	 * 4x block dust Redstone + 4x block dust Glowstone -> 9 lamps at 144 t / 16 EUt.
	 */
	@Test
	public void pressLampBlockDustLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("press");
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Vanilla.java:777 — redstone lamp, blockDust leg xGlowstone");
		assertEquals(144, tRow.get("duration").getAsLong());
		assertEquals(16, tRow.get("eut").getAsLong(), "the addRecipeX eut column");
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals(2, tInputs.size());
		assertEquals("gt6:block_dust_redstone", slotId(tInputs.get(0)));
		assertEquals(4, slotCount(tInputs.get(0)));
		assertEquals("gt6:block_dust_glowstone", slotId(tInputs.get(1)));
		assertEquals(4, slotCount(tInputs.get(1)));
		JsonArray tOutputs = tRow.getAsJsonArray("outputs");
		assertEquals(1, tOutputs.size());
		assertEquals("minecraft:redstone_lamp", slotId(tOutputs.get(0)), "ST.make(Blocks.redstone_lamp, 9, 0)");
		assertEquals(9, slotCount(tOutputs.get(0)));
	}

	/**
	 * Spot check 3 — the cryomixer NaNO3 base row (Loader_Recipes_Other.java:233): the
	 * OM.dust ladder faces (1 dust redstone + 1 tiny blizz + 1 nano3) + water 250 mB ->
	 * 2x cryotheum at 32 t / 16 EUt.
	 */
	@Test
	public void cryomixerBaseRowIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("cryomixer");
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Other.java:233 —");
		assertEquals(32, tRow.get("duration").getAsLong());
		assertEquals(16, tRow.get("eut").getAsLong());
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals(3, tInputs.size());
		assertEquals("gt6:dust_redstone", slotId(tInputs.get(0)), "OM.dust(Redstone, U)");
		assertEquals("gt6:dust_tiny_blizz", slotId(tInputs.get(1)), "OM.dust(Blizz, U9) = 1 tiny");
		assertEquals("gt6:dust_sodium_nitrate", slotId(tInputs.get(2)), "snakeCase(NaNO3)");
		JsonArray tFluids = tRow.getAsJsonArray("fluidInputs");
		assertEquals(1, tFluids.size());
		assertEquals("minecraft:water", slotId(tFluids.get(0)));
		assertEquals(250, tFluids.get(0).getAsJsonObject().get("amount").getAsInt(), "FL.mul(tWater, 1, 4, T)");
		assertEquals(1, tRow.getAsJsonArray("outputs").size());
		assertEquals("gt6:dust_cryotheum", slotId(tRow.getAsJsonArray("outputs").get(0)));
		assertEquals(2, slotCount(tRow.getAsJsonArray("outputs").get(0)), "OM.dust(Cryotheum, 2U)");
	}

	/**
	 * The coagulator TU face: eut 0 rows (NBT_NO_CONSTANT_POWER) at the upstream durations.
	 * The seated fluid smoke row (water -> snowball, eut 1) is not the TU face — the card's
	 * five commented rows are (the rebase-seam census scoping).
	 */
	@Test
	public void coagulatorRowsCarryTheZeroEutTuFace() throws Exception {
		JsonArray tRows = pourShipped("coagulator");
		assertEquals(6, tRows.size(), "the seated fluid smoke row + the five card rows");
		int tCardRows = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment")) continue; // the seated fluid smoke row (eut 1)
			tCardRows++;
			assertEquals(0, tRow.get("eut").getAsInt(), "every Coagulator card row is the TU no-constant-power face");
		}
		assertEquals(5, tCardRows, "the card's five rows");
	}

	/**
	 * The live-id face, offline: every gt6 id the ten files reference must be a member of
	 * the registration walks' id universe (GTMaterialItems + GTMaterialBlocks
	 * registrationOrder over the initialized material system — the GT6AssetCoverageGuardTest
	 * :380-386 pattern). The stand-in pour above proves the rows PARSE; this proves the ids
	 * EXIST, closing the WARN-skip gap the stand-ins cannot see.
	 */
	@Test
	public void everyGt6IdTheB1FilesReferenceIsRegistered() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials();
		java.util.Set<String> tUniverse = new java.util.HashSet<>();
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialBlocks.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (gregtech6.registry.GT6Robotics.RobotRow tRow : gregtech6.registry.GT6Robotics.ROWS) {
			tUniverse.add("gt6:" + tRow.id()); // task robotics-chain — the 60 plain-item faces the tip rows walk (the vanilla-alias items convention: the universe = every registered carrier the shipped rows reference)
		}
		// task explosives-chain — the plain-item registration homes are part of the id
		// universe too (the prefix walks cannot see them): the DR entries resolve to their
		// declared ids offline (the holder getId, the GT6ExtruderMoldsTest face). The
		// element types stay UNNAMED (var / receiver inference) — naming RegistryObject
		// here breaks the neoforge leg, the stonecutter import swap rewrites the import
		// line only, not the named usages
		for (gregtech6.registry.GT6Explosives.DynamiteRow tRow : gregtech6.registry.GT6Explosives.ROWS) {
			tUniverse.add("gt6:" + gregtech6.registry.GT6Explosives.ITEMS_BY_PATH.get(tRow.path()).getId().getPath());
		}
		for (var tMold : gregtech6.registry.GT6PressMolds.MOLDS) {
			tUniverse.add("gt6:" + tMold.getId().getPath());
		}
		assertTrue(tUniverse.contains("gt6:dust_coal"), "the id universe built (" + tUniverse.size() + " ids)");

		java.util.Set<String> tMissing = new java.util.TreeSet<>();
		for (String tKey : CENSUS.keySet()) {
			InputStream tStream = GT6RecipeMapDataB1RowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
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
		assertTrue(tMissing.isEmpty(), "unregistered gt6 ids (these rows would WARN-skip live): " + tMissing);
	}

	/**
	 * Spot check 4 (task toolhead-r11e-press-mortar, extended by toolhead-family-closeout) —
	 * the Press HandlerPrefix walk representatives (Loader_Recipes_Handlers.java:247-253,
	 * the P8 pooled reclaim): 16 EUt / 16 t everywhere, the walk input first and the
	 * additional input trailing (RecipeMapHandlerPrefix.addRecipeForMaterial:207-208),
	 * single output, no chance key. The walked material/base axes are the representative
	 * faces (Diamond gem, Iron base, Flint arrow head) — the full cross-product is the
	 * declared pooled face, one row per upstream STATEMENT, matching the mortar.json
	 * :705-706 Blaze-representative convention. The :251 Empty-head retip joined with
	 * toolhead-family-closeout: the OP.java:621 force row landed the blank pickaxeGem head
	 * (GTMaterialItemsForceTest), so the retip base exists and the drop ring (the broken
	 * gem pick returns the blank) closes the loop.
	 */
	@Test
	public void pressHandlerWalkRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("press");
		JsonObject tRaw = findRow(tRows, "Loader_Recipes_Handlers.java:247-248 —");
		assertEquals(16, tRaw.get("duration").getAsLong(), "the handler duration column");
		assertEquals(16, tRaw.get("eut").getAsLong(), "the handler eut column");
		JsonArray tRawInputs = tRaw.getAsJsonArray("inputs");
		assertEquals(2, tRawInputs.size(), "gemFlawed x2 + the raw any-iron head");
		assertEquals("gt6:gem_flawed_diamond", slotId(tRawInputs.get(0)));
		assertEquals(2, slotCount(tRawInputs.get(0)), "the gemFlawed amount column");
		assertEquals("gt6:tool_head_raw_pickaxe_iron", slotId(tRawInputs.get(1)), "the ANY.Iron raw-head representative, trailing");
		assertEquals(1, slotCount(tRawInputs.get(1)));
		assertEquals("gt6:tool_head_pickaxe_gem_diamond", slotId(tRaw.getAsJsonArray("outputs").get(0)));
		assertEquals(1, slotCount(tRaw.getAsJsonArray("outputs").get(0)));

		JsonObject tFin = findRow(tRows, "Loader_Recipes_Handlers.java:249-250 —");
		assertEquals("gt6:tool_head_pickaxe_iron", slotId(tFin.getAsJsonArray("inputs").get(1)), "the ANY.Iron finished-head representative");
		assertEquals("gt6:tool_head_pickaxe_gem_diamond", slotId(tFin.getAsJsonArray("outputs").get(0)));

		JsonObject tEmpty = findRow(tRows, "Loader_Recipes_Handlers.java:251 —");
		assertEquals(16, tEmpty.get("duration").getAsLong(), "the handler duration column");
		assertEquals(16, tEmpty.get("eut").getAsLong(), "the handler eut column");
		JsonArray tEmptyInputs = tEmpty.getAsJsonArray("inputs");
		assertEquals(2, tEmptyInputs.size(), "gemFlawed x2 + the blank Empty pickaxeGem head");
		assertEquals("gt6:gem_flawed_diamond", slotId(tEmptyInputs.get(0)), "the gemFlawed walk slot");
		assertEquals(2, slotCount(tEmptyInputs.get(0)));
		assertEquals("gt6:tool_head_pickaxe_gem_empty", slotId(tEmptyInputs.get(1)), "toolHeadPickaxeGem.mat(MT.Empty, 1) — the OP.java:621 forced blank, trailing");
		assertEquals(1, slotCount(tEmptyInputs.get(1)));
		assertEquals("gt6:tool_head_pickaxe_gem_diamond", slotId(tEmpty.getAsJsonArray("outputs").get(0)), "the walked gem's gem head");

		JsonObject tWood = findRow(tRows, "Loader_Recipes_Handlers.java:252 —");
		JsonArray tWoodInputs = tWood.getAsJsonArray("inputs");
		assertEquals(2, tWoodInputs.size(), "the arrow head + the plain Empty wooden arrow");
		assertEquals("gt6:tool_head_arrow_flint", slotId(tWoodInputs.get(0)));
		assertEquals("gt6:arrow_gt_wood_empty", slotId(tWoodInputs.get(1)), "arrowGtWood.mat(MT.Empty, 1) — the Or(toolHeadArrow, EMPTY) plain arrow");
		assertEquals("gt6:arrow_gt_wood_flint", slotId(tWood.getAsJsonArray("outputs").get(0)));

		JsonObject tPlastic = findRow(tRows, "Loader_Recipes_Handlers.java:253 —");
		assertEquals("gt6:arrow_gt_plastic_empty", slotId(tPlastic.getAsJsonArray("inputs").get(1)));
		assertEquals("gt6:arrow_gt_plastic_flint", slotId(tPlastic.getAsJsonArray("outputs").get(0)));

		// the :254-259 bullet retips stay POOLED (the card-scope boundary): the blank
		// bulletGt carriers exist (the Or(PROJECTILES, EMPTY) face) but the pool card owns
		// those statements — no bullet row ships here
		for (int tLine = 254; tLine <= 259; tLine++) {
			for (JsonElement tElement : tRows) {
				String tComment = tElement.getAsJsonObject().has("comment") ? tElement.getAsJsonObject().get("comment").getAsString() : "";
				assertTrue(!tComment.startsWith("Loader_Recipes_Handlers.java:" + tLine + " —"),
						"the :" + tLine + " bullet retip stays pooled (the card-scope boundary; the pool card reclaims it)");
			}
		}
	}

	// ------------------------------------------------------------------ helpers

	/** Finds the one row whose comment starts with the given source citation. */
	private static JsonObject findRow(JsonArray aRows, String aCommentPrefix) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().startsWith(aCommentPrefix)) return tRow;
		}
		throw new AssertionError("no row cited " + aCommentPrefix);
	}

	private static String slotId(JsonElement aSlot) {
		JsonObject tSlot = aSlot.getAsJsonObject();
		return tSlot.has("item") ? tSlot.get("item").getAsString() : tSlot.get("fluid").getAsString();
	}

	private static int slotCount(JsonElement aSlot) {
		return aSlot.getAsJsonObject().has("count") ? aSlot.getAsJsonObject().get("count").getAsInt() : 1;
	}
}
