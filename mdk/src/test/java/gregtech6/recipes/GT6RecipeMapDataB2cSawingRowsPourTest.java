package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.block.tree.GT6BeamKind;
import gregtech6.block.tree.GT6TreeKind;
import gregtech6.registry.GT6BeamBlocks;
import gregtech6.registry.GT6ConcreteBlocks;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GTStoneSlabBlocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The recipe-data-b2c-sawing row-stock pour test (the B2c-cut posture): the RM.sawing
 * SCATTER static replay — the seven card anchors (Vanilla:582-621, Woods:169/:192,
 * BlockStones:272-274, BlockMetaType:92, OreDict:171, RM.java:328/:351,
 * Temporary:105-107) — pours through the real {@link GT6RecipeMapJsonLoader} seam under
 * its own "sawing" key into the SAME CUTTER map (the size-split ruling: cutter.json sits
 * at 4966700 of the 5242880-byte commit cap). RECOMPUTABILITY: the rows were generated
 * from a one-shot live registration dump (an uncommitted JUnit over GTMaterialItems — the
 * weld/generify zero-transcription method), and THIS test is the durable half: the
 * live-walk cross-check recomputes ALL SEVEN anchors against the live registration and
 * fails the moment this frozen snapshot trails or outruns it.
 *
 * <p>Engine semantics under replay (RM.java:720-731): every RM.sawing(eut, dur, isFood,
 * lube, in...) call fans into five leg rows — water/spdew/mnwtr at duration x4 with lube
 * x4 mB, distilled_water at x3/x3, and (non-food only) one row per FluidsGT.LUBRICANT
 * member at x1/x1. The port lubricant family is {gt6:lubricant} only; the "rc
 * lubricant"/LubRoCant alias is a declared faithful absence.
 *
 * <p>Beam increment (task beam-consume-increment): the anchor-3 face (Woods:192) is
 * ACTIVATED for the 7 pourable BeamEntry calls of the port universe (the 6 vanilla
 * LoaderWoodDictionary:51-56 defaults + the :175 Rubber Wood face, whose upstream
 * PlankEntry is the vanilla jungle planks — WoodDictionary.PLANKS.get(Blocks.planks, 3))
 * — 35 rows tail-appended. The recompute below emits the beam face, so this frozen
 * snapshot cannot trail or outrun it.
 * <p>Task sawing-plank-concrete-increment flipped the four BLOCKED faces the two merged
 * registration cards unlocked: the 9 GT6 tree-log plank legs (Woods:169), the :66
 * DEFAULT_BEAM row (Woods:192 — superseding the blocked note above), the :604-617
 * IL.Plank-output statics and the BlockMetaType:92 BlockColored/concrete face — the
 * IL.Plank generic-plank output rides the DECLARED IDENTITY MAPPING to
 * minecraft:oak_planks (the file head carries the citation; census 3959 -> 4274 = the
 * 4239 increment + the 35 merged beam-consume rows).
 */
public class GT6RecipeMapDataB2cSawingRowsPourTest extends GTRecipesOfflineTestBase {

	private static final String FILE_KEY = "sawing";

	/** The frozen snapshot census: 855 calls x the five-leg fan (854 lubricant — :621 melon is food). */
	private static final int CENSUS = 4274; // 3959 b2c-sawing + 280 sawing-plank-concrete-increment + 35 beam-consume-increment
	/** The seven-anchor per-call census (calls; rows = the leg fan). */
	private static final int CALLS_WOOD_WALK = 248, CALLS_DYE_LOOP = 32, CALLS_VANILLA_STATICS = 15,
			CALLS_VANILLA_LOGS = 6, CALLS_STONE_SLABS = 210, CALLS_BLOCK_SLABS = 272, CALLS_SAPLINGS = 9,
			CALLS_BEAM_WALK = 7, // task beam-consume-increment: Woods:192, the 7 pourable BeamEntry faces
			CALLS_GT_LOGS = 9, CALLS_PLANK_STATICS = 14, CALLS_WOOD_BEAM = 1, CALLS_CONCRETE = 32; // task sawing-plank-concrete-increment: the 9 GT tree logs (Woods:169), the
	// :604-617 plank-output statics, the :66 DEFAULT_BEAM row (Woods:192), the BlockMetaType:92
	// BlockColored/concrete face (32 = 2 families x 16 colours)

	/** The five leg rows of one non-food call, verbatim RM.java:725-730 (fluid, duration mult, amount mult). */
	private static final Object[][] LEGS = {
			{"minecraft:water", 4L, 4L}, {"gt6:spdew", 4L, 4L}, {"gt6:mnwtr", 4L, 4L},
			{"gt6:distilled_water", 3L, 3L}, {"gt6:lubricant", 1L, 1L}};

	private static final String[] DYES = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
			"gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

	/** BlockStones.java:68 — the JUSTSTONE mask (RNFBR 8 / RSTBR 9 excluded from the :271 loop). */
	private static final int[] JUSTSTONE = {0, 1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15};
	private static final int RSTBR = 9;

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

	/** Reads the shipped file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped() throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + FILE_KEY + ".json";
		InputStream tStream = GT6RecipeMapDataB2cSawingRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", FILE_KEY), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: the file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void theSawingFilePoursItsFullCensusWithZeroSkips() throws Exception {
		pourShipped();
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(FILE_KEY);
		assertNotNull(tMap, FILE_KEY + " resolves");
		assertEquals(GT6RecipeMaps.CUTTER, tMap, FILE_KEY + ": the second key pours into the SAME CUTTER map");
		assertEquals(CENSUS, tMap.mRecipeList.size(), FILE_KEY + ": the map holds the census");
		assertEquals(CENSUS, GT6RecipeMapJsonLoader.pouredCount(FILE_KEY),
				FILE_KEY + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
	}

	/** The file-head declaration pins: the seven anchors, the split ruling, deviations, negatives, provenance. */
	@Test
	public void theFileHeadKeepsItsDeclarations() throws Exception {
		InputStream tStream = GT6RecipeMapDataB2cSawingRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + FILE_KEY + ".json");
		assertNotNull(tStream);
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		assertTrue(tDoc.has("comment"), FILE_KEY + ": the file head carries a comment member");
		String tComment = tDoc.get("comment").getAsString();
		String tLower = tComment.toLowerCase(java.util.Locale.ROOT);
		// the seven card anchors, each cited
		assertTrue(tComment.contains("Loader_Recipes_Vanilla.java:583-587"), "anchor 1 (the vanilla run :582-621)");
		assertTrue(tComment.contains("Loader_Recipes_Woods.java:169"), "anchor 2 (the log walk)");
		assertTrue(tComment.contains("Woods:192"), "anchor 3 (the beam walk)");
		assertTrue(tComment.contains("BlockStones.java:272-274"), "anchor 4 (the stone slab sawing)");
		assertTrue(tComment.contains("BlockMetaType.java:92"), "anchor 5 (the block->2-slab ctor loop)");
		assertTrue(tComment.contains("OreDict.java:171"), "anchor 6 (the treeSapling listener)");
		assertTrue(tComment.contains("RM.java:328") && tComment.contains(":351"), "anchor 7 (the stoneshapes arms)");
		assertTrue(tComment.contains("Temporary:105-107"), "the Temporary HEX anchor");
		// the method declarations
		assertTrue(tLower.contains("declared deviation"), "the deviation class is declared");
		assertTrue(tLower.contains("frozen"), "the declaration names the frozen-universe semantics");
		assertTrue(tLower.contains("4966700"), "the file-split ruling names the cutter.json byte census");
		assertTrue(tLower.contains("lubrocant"), "the LubRoCant faithful absence is declared");
		assertTrue(tLower.contains("true negative"), "the zero-row faces (stoneshapes compat callers, HEX) are declared");
		assertTrue(tLower.contains("identity mapping"), "the 1.7.10 -> 1.20.1 identity mappings are declared");
		assertTrue(tLower.contains("un-named scatter faces"), "the un-named faces found by the line-by-line read are declared");
		assertTrue(tLower.contains("walk row provenance"), "the declaration names the walk-row provenance ruling");
		// the sawing-plank-concrete-increment activations and the declared identity mapping
		assertTrue(tComment.contains("sawing-plank-concrete-increment"), "the increment card is named");
		assertTrue(tLower.contains("declared identity mapping"), "the IL.Plank mapping class is declared");
		assertTrue(tComment.contains("IL.Plank = BlocksGT.Planks meta 9"), "the upstream IL.Plank face is cited");
		assertTrue(tComment.contains("Loader_Woods.java:74") && tComment.contains("BlockTreePlanks.java:49"),
				"the mapping's upstream citations ride the head");
		assertTrue(tComment.contains("-> minecraft:oak_planks"), "the mapping target is declared");
		assertTrue(tComment.contains("gt-tree-planks") && tComment.contains("concrete-blocks-register"),
				"the two registration cards that unlocked the faces are cited");
		assertTrue(tComment.contains("beam-consume-increment"), "the pending beam-branch merge-order note is declared");
	}

	/** The seven-anchor census: each anchor's recomputed call count pins; the sum x legs is the file census. */
	@Test
	public void theSevenAnchorsSplitTheCensus() throws Exception {
		Map<String, Integer> tByAnchor = anchorCallCensus();
		assertEquals(CALLS_WOOD_WALK, tByAnchor.get("wood_walk").intValue(), ":582-585 ANY.Wood walk calls");
		assertEquals(CALLS_DYE_LOOP, tByAnchor.get("dye_loop").intValue(), ":586-589 16-dye loop calls");
		assertEquals(CALLS_VANILLA_STATICS + CALLS_PLANK_STATICS, tByAnchor.get("vanilla_statics").intValue(),
				":590-621 pourable static calls (the :604-617 plank-output rows joined at sawing-plank-concrete-increment)");
		assertEquals(CALLS_VANILLA_LOGS + CALLS_GT_LOGS, tByAnchor.get("vanilla_logs").intValue(),
				"Woods:169 log calls (the 9 GT6 tree rows joined at sawing-plank-concrete-increment)");
		assertEquals(CALLS_STONE_SLABS, tByAnchor.get("stone_slabs").intValue(), "BlockStones:272-274 calls (plate-gated)");
		assertEquals(CALLS_BLOCK_SLABS + CALLS_CONCRETE, tByAnchor.get("block_slabs").intValue(),
				"BlockMetaType:92 calls (the 32-pair BlockColored/concrete face joined at sawing-plank-concrete-increment)");
		assertEquals(CALLS_SAPLINGS, tByAnchor.get("saplings").intValue(), "OreDict:171 calls");
		assertEquals(CALLS_BEAM_WALK, tByAnchor.get("beam_walk").intValue(), "Woods:192 beam-walk calls (the pourable BeamEntry faces)");
		assertEquals(CALLS_WOOD_BEAM, tByAnchor.get("wood_beam").intValue(), "Woods:192 :66 DEFAULT_BEAM call");
		int tSum = tByAnchor.values().stream().mapToInt(Integer::intValue).sum();
		assertEquals(CENSUS, tSum * 5 - 1, "calls x the five-leg fan minus the melon food leg = the file census");
	}

	/** The five-leg accounting: exactly one fluid input per row; the legs split 792/792/792/792/791. */
	@Test
	public void theFiveFluidLegsSplitTheCensus() throws Exception {
		Map<String, Integer> tByFluid = new java.util.HashMap<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			assertTrue(tRow.has("fluidInputs") && tRow.getAsJsonArray("fluidInputs").size() == 1,
					"every sawing row carries exactly one fluid input");
			String tFluid = tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString();
			tByFluid.merge(tFluid, 1, Integer::sum);
		}
		Set<String> tExpected = Set.of("minecraft:water", "gt6:spdew", "gt6:mnwtr", "gt6:distilled_water", "gt6:lubricant");
		assertEquals(tExpected, tByFluid.keySet(), "exactly the five legs (the LubRoCant alias is a declared absence)");
		int tCalls = CALLS_WOOD_WALK + CALLS_DYE_LOOP + CALLS_VANILLA_STATICS + CALLS_VANILLA_LOGS + CALLS_GT_LOGS
				+ CALLS_PLANK_STATICS + CALLS_WOOD_BEAM + CALLS_STONE_SLABS + CALLS_BLOCK_SLABS + CALLS_CONCRETE
				+ CALLS_SAPLINGS + CALLS_BEAM_WALK;
		for (String tFluid : new String[] {"minecraft:water", "gt6:spdew", "gt6:mnwtr", "gt6:distilled_water"}) {
			assertEquals(tCalls, tByFluid.get(tFluid).intValue(), tFluid + " carries its leg census");
		}
		assertEquals(tCalls - 1, tByFluid.get("gt6:lubricant").intValue(),
				"the lubricant leg skips the single aIsFoodItem=T call (:621 melon) — RM.java:729");
	}

	/**
	 * Verbatim spot checks — twelve full-row pins across the anchors: the walk pair on their
	 * extreme legs, the dye loop, the double-stone-slab identity row on the lubricant leg, the
	 * ladder row with its OM.dust dustDiv72 arm (stick U2/3 -> dust_div72 x12), the vanilla
	 * log row (plankBuzz 6 + bark), the stone :272/:274 pair, the BlockMetaType:92 arm on the
	 * mnwtr leg, the sapling row, and the melon food row.
	 */
	@Test
	public void anchorRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped();

		// :583 stick -> bolt x4, Water leg (dur 16x4 = 64, lube 3x4 = 12 mB)
		assertRow(findRow(tRows, "gt6:stick_wood", "minecraft:water"),
				">gt6:bolt_wood:4", 64, 16, 12);
		// :584 stickLong -> stick x2, DistW leg (dur 16x3 = 48, lube 1x3 = 3 mB)
		assertRow(findRow(tRows, "gt6:stick_long_wood", "gt6:distilled_water"),
				">gt6:stick_wood:2", 48, 16, 3);
		// :587 stained glass -> pane x9, Water leg (dur 32x4 = 128, lube 50x4 = 200 mB)
		assertRow(findRow(tRows, "minecraft:white_stained_glass", "minecraft:water"),
				">minecraft:white_stained_glass_pane:9", 128, 16, 200);
		// :588 wool x2 -> carpet x3, SpDew leg
		assertRow(findRow(tRows, "minecraft:white_wool", "gt6:spdew"),
				">minecraft:white_carpet:3", 128, 16, 200);
		// :591 double_stone_slab:0 -> smooth_stone identity, Lubricant leg (dur 16, lube 100 mB)
		assertRow(findRow(tRows, "minecraft:smooth_stone", "gt6:lubricant"),
				">gt6:plate_stone:8>gt6:dust_stone:1", 16, 16, 100);
		// :620 ladder -> stick x2 + OM.dust(Wood, U2/3) = dustDiv72 x12 (OM.java:463-468 ladder walk), DistW leg
		assertRow(findRow(tRows, "minecraft:ladder", "gt6:distilled_water"),
				">minecraft:stick:2>gt6:dust_div72_wood:12", 120, 16, 300);
		// Woods:169 oak log -> planks x6 (WoodEntry 2-arg defaults, WoodEntry.java:76-89) + bark dust, Water leg (128x4)
		assertRow(findRow(tRows, "minecraft:oak_log", "minecraft:water"),
				">minecraft:oak_planks:6>gt6:dust_bark:1", 512, 16, 16);
		// Woods:169 GT tree leg (sawing-plank-concrete-increment): the rubber row on the water leg
		assertRow(findRow(tRows, "gt6:rubber_log", "minecraft:water"),
				">gt6:rubber_planks:6>gt6:dust_bark:1", 512, 16, 16);
		// the Cinnamon row carries the IL.HaC_Cinnamon.get(1, IL.Food_Cinnamon.get(1, OM.dust(MT.Cinnamon))) bark chain
		// (LoaderWoodDictionary.java:93, the compat items are absent -> the dust_cinnamon tail), Lubricant leg
		assertRow(findRow(tRows, "gt6:cinnamon_log", "gt6:lubricant"),
				">gt6:cinnamon_planks:6>gt6:dust_cinnamon:1", 128, 16, 4);
		// Woods:192 :66 DEFAULT_BEAM row (sawing-plank-concrete-increment): BeamEntry 2-arg defaults buzz 7
		// (BeamEntry.java:48-50), the plank output under the declared IL.Plank -> oak_planks identity mapping, Water leg
		assertRow(findRow(tRows, "gt6:wood_beam", "minecraft:water"),
				">minecraft:oak_planks:7>gt6:dust_wood:1", 512, 16, 16);
		// :610 bed row (sawing-plank-concrete-increment): Items.bed -> red_bed, wool x3 tail, DistW leg
		assertRow(findRow(tRows, "minecraft:red_bed", "gt6:distilled_water"),
				">minecraft:oak_planks:3>minecraft:white_wool:3", 144, 16, 300);
		// :617 jukebox row: plank x8 + the diamond gem tail, Water leg (lube 100 x4 = 400 mB)
		assertRow(findRow(tRows, "minecraft:jukebox", "minecraft:water"),
				">minecraft:oak_planks:8>gt6:gem_diamond:1", 512, 16, 400);
		// BlockStones:272 granite_black smooth slab -> plate x4 + dustSmall x2, Lubricant leg
		assertRow(findRow(tRows, "gt6:granite_black_smooth_slab", "gt6:lubricant"),
				">gt6:plate_granite_black:4>gt6:dust_small_granite_black:2", 16, 16, 50);
		// BlockStones:274 the RSTBR row carries the redstone dust tail
		assertRow(findRow(tRows, "gt6:granite_black_bricks_redstone_slab", "minecraft:water"),
				">gt6:plate_granite_black:4>gt6:dust_small_granite_black:2>gt6:dust_redstone:2", 64, 16, 200);
		// BlockMetaType:92 block -> 2 slabs, MnWtr leg (lube 5 x4 = 20 mB)
		assertRow(findRow(tRows, "gt6:marble", "gt6:mnwtr"),
				">gt6:marble_slab:2", 64, 16, 20);
		// BlockMetaType:92 BlockColored/concrete face (sawing-plank-concrete-increment): first and last colour,
		// both families (family-major registrationOrder, colour-major DYE_IDS 0=black..15=white)
		assertRow(findRow(tRows, "gt6:concrete_black", "gt6:mnwtr"),
				">gt6:concrete_black_slab:2", 64, 16, 20);
		assertRow(findRow(tRows, "gt6:concrete_reinforced_white", "minecraft:water"),
				">gt6:concrete_reinforced_white_slab:2", 64, 16, 20);
		// OreDict:171 sapling -> stick_wood, Water leg (dur 64, lube 1x4 = 4 mB)
		assertRow(findRow(tRows, "gt6:rubber_sapling", "minecraft:water"),
				">gt6:stick_wood:1", 64, 16, 4);
		// :621 melon — the aIsFoodItem=T call: its water row exists, and NO lubricant row does
		assertRow(findRow(tRows, "minecraft:melon", "minecraft:water"),
				">minecraft:melon_slice:8>minecraft:melon_seeds:1", 64, 16, 200);
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if ("minecraft:melon".equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())) {
				assertTrue(!"gt6:lubricant".equals(tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString()),
						"the melon food call never grows a lubricant leg (RM.java:729 isFood gate)");
			}
		}
	}

	/**
	 * The live-walk cross-check — the heart of the frozen-snapshot deviation: recompute ALL
	 * SEVEN anchors against the LIVE registration (the ANY.Wood walk over GTMaterialItems, the
	 * 272-pair GTStoneBlocks universe, the GT6TreeKind saplings, the static vanilla tables) and
	 * require the shipped rows to cover the recomputation EXACTLY, both directions.
	 */
	@Test
	public void theShippedRowsMatchTheLiveRecompute() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tExpected = liveRecompute();
		Set<String> tShipped = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			tShipped.add(keyOf(tElement.getAsJsonObject()));
		}
		Set<String> tMissing = new HashSet<>(tExpected);
		tMissing.removeAll(tShipped);
		Set<String> tStale = new HashSet<>(tShipped);
		tStale.removeAll(tExpected);
		assertTrue(tMissing.isEmpty(), "the frozen snapshot trails the live recompute (re-pour needed): " + tMissing);
		assertTrue(tStale.isEmpty(), "the frozen snapshot holds rows the live recompute no longer generates: " + tStale);
		assertEquals(tExpected.size(), tShipped.size(), "the snapshot size equals the live recompute");
	}

	/** The fluid-universe face: the gt6 leg ids resolve in the GTFluids spec tables (the cut-test shape). */
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

	/** The offline fluid-universe lookup: the union of the GTFluids spec-table lookups (the cut-test shape). */
	private static boolean fluidRegistered(String aPath) {
		return gregtech6.fluid.GTFluids.aquaSpec(aPath) != null || gregtech6.fluid.GTFluids.engineSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.chemicalSpec(aPath) != null || gregtech6.fluid.GTFluids.simpleLiquidSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.lubricantSpec(aPath) != null;
	}

	/**
	 * The id faces: every gt6 ITEM id lives in GTMaterialItems ∪ GTMaterialBlocks ∪
	 * GTStoneBlocks(272) ∪ GTStoneSlabBlocks(272) ∪ GT6TreeBlocks(saplings/logs); every
	 * minecraft: id sits in the static recompute tables (a typo would be a row the recompute
	 * never emits — test 6 catches it — this test pins the vanilla whitelist explicitly).
	 */
	@Test
	public void everyItemIdTheFileReferencesIsRegistered() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tItemUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tItemUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			tItemUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
			tItemUniverse.add("gt6:" + GTStoneBlocks.path(tKey.stone().snake(), tKey.variant()));
			tItemUniverse.add("gt6:" + GTStoneSlabBlocks.slabPath(tKey.stone().snake(), tKey.variant()));
		}
		for (GT6TreeKind tKind : GT6TreeKind.values()) {
			tItemUniverse.add("gt6:" + tKind.snake() + "_sapling");
			tItemUniverse.add("gt6:" + tKind.snake() + "_log");
			tItemUniverse.add("gt6:" + tKind.snake() + "_planks"); // sawing-plank-concrete-increment
		}
		tItemUniverse.add("gt6:" + GT6BeamKind.WOOD.snake() + "_beam"); // the :66 input face
		for (GT6ConcreteBlocks.ConcreteRow tPair : GT6ConcreteBlocks.registrationOrder()) {
			tItemUniverse.add("gt6:" + GT6ConcreteBlocks.path(tPair.family(), tPair.dyeIndex()));
			tItemUniverse.add("gt6:" + GT6ConcreteBlocks.slabPath(tPair.family(), tPair.dyeIndex()));
		}
		for (GT6BeamKind tBeam : GT6BeamKind.values()) { // task beam-consume-increment: the beam universe
			tItemUniverse.add("gt6:" + GT6BeamBlocks.path(tBeam));
		}
		tItemUniverse.addAll(vanillaWhitelist());
		Set<String> tMissing = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			for (String tLeg : new String[] {"inputs", "outputs"}) {
				for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
					String tId = tSlot.getAsJsonObject().get("item").getAsString();
					if (!tItemUniverse.contains(tId)) tMissing.add(tId);
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered item ids (these rows would WARN-skip live): " + tMissing);
	}

	// ------------------------------------------------------------- the live recompute (all seven anchors)

	/** The full expected key set over the seven anchors, computed from the LIVE registration. */
	private static Set<String> liveRecompute() {
		Set<String> tReg = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tReg.add(tPair.prefix().mNameInternal + "|" + tPair.material().mNameInternal);
		}
		anchorCallCensus(); // the per-anchor ratchet rides the same walk (asserted in test 3)
		Set<String> rKeys = new HashSet<>();

		// anchor 1 — Loader_Recipes_Vanilla.java:582-585
		for (OreDictMaterial tMat : ANY.Wood.mToThis) {
			if (!tReg.contains("stick|" + tMat.mNameInternal)) continue;
			if (tReg.contains("bolt|" + tMat.mNameInternal)) { // :583
				emit(rKeys, 16, 16, 3, false, "gt6:" + idOf(OP.stick, tMat), 1, "gt6:" + idOf(OP.bolt, tMat) + ":4");
			}
			if (tReg.contains("stickLong|" + tMat.mNameInternal)) { // :584
				emit(rKeys, 16, 16, 1, false, "gt6:" + idOf(OP.stickLong, tMat), 1, "gt6:" + idOf(OP.stick, tMat) + ":2");
			}
		}

		// anchor 1 — :586-589 the dye loop
		for (String tDye : DYES) {
			emit(rKeys, 16, 32, 50, false, "minecraft:" + tDye + "_stained_glass", 1, "minecraft:" + tDye + "_stained_glass_pane:9");
			emit(rKeys, 16, 32, 50, false, "minecraft:" + tDye + "_wool", 2, "minecraft:" + tDye + "_carpet:3");
		}

		// anchor 1 — :590-621 the statics (pourable faces only; blocked rows cited in the file head)
		emit(rKeys, 16, 32, 50, false, "minecraft:glass", 1, "minecraft:glass_pane:9"); // :590
		emit(rKeys, 16, 16, 100, false, "minecraft:smooth_stone", 1, "gt6:plate_stone:8>gt6:dust_stone:1"); // :591
		emit(rKeys, 16, 16, 100, false, "minecraft:smooth_sandstone", 1, "gt6:plate_stone:8>gt6:dust_stone:1"); // :592
		emit(rKeys, 16, 16, 50, false, "minecraft:smooth_stone_slab", 1, "gt6:plate_stone:4>gt6:dust_small_stone:2"); // :593
		emit(rKeys, 16, 16, 100, false, "minecraft:stone", 1, "minecraft:smooth_stone_slab:2"); // :594
		emit(rKeys, 16, 16, 100, false, "minecraft:sandstone", 1, "minecraft:sandstone_slab:2"); // :598
		emit(rKeys, 16, 16, 100, false, "minecraft:cobblestone", 1, "minecraft:cobblestone_slab:2"); // :599
		emit(rKeys, 16, 16, 100, false, "minecraft:bricks", 1, "minecraft:brick_slab:2"); // :600
		emit(rKeys, 16, 16, 100, false, "minecraft:stone_bricks", 1, "minecraft:stone_brick_slab:2"); // :601
		emit(rKeys, 16, 16, 100, false, "minecraft:nether_bricks", 1, "minecraft:nether_brick_slab:2"); // :602
		emit(rKeys, 16, 16, 100, false, "minecraft:quartz_block", 1, "minecraft:quartz_slab:2"); // :603
		// :604-617 — the plank-output statics (ACTIVATED at sawing-plank-concrete-increment under the declared
		// IL.Plank -> minecraft:oak_planks identity mapping: IL.Plank = BlocksGT.Planks meta 9 "Wood Planks",
		// Loader_Woods.java:74 / BlockTreePlanks.java:49; the port registers no generic plank face)
		emit(rKeys, 16, 16, 100, false, "minecraft:oak_button", 1, "minecraft:oak_planks:1"); // :604
		emit(rKeys, 16, 32, 100, false, "minecraft:oak_pressure_plate", 1, "minecraft:oak_planks:2"); // :605
		emit(rKeys, 16, 32, 100, false, "minecraft:oak_sign", 1, "minecraft:oak_planks:2>gt6:dust_div72_wood:12"); // :606
		emit(rKeys, 16, 32, 100, false, "minecraft:oak_door", 1, "minecraft:oak_planks:2"); // :607
		// :608 OM.dust(Wood, stick.mAmount x4) = 2U -> the :463 dust arm x2
		emit(rKeys, 16, 32, 100, false, "minecraft:oak_fence_gate", 1, "minecraft:oak_planks:2>gt6:dust_wood:2"); // :608
		emit(rKeys, 16, 48, 100, false, "minecraft:oak_trapdoor", 1, "minecraft:oak_planks:3"); // :609
		emit(rKeys, 16, 48, 100, false, "minecraft:red_bed", 1, "minecraft:oak_planks:3>minecraft:white_wool:3"); // :610
		emit(rKeys, 16, 64, 100, false, "minecraft:crafting_table", 1, "minecraft:oak_planks:4"); // :611
		emit(rKeys, 16, 80, 100, false, "minecraft:oak_boat", 1, "minecraft:oak_planks:5"); // :612
		emit(rKeys, 16, 96, 100, false, "minecraft:bookshelf", 1, "minecraft:oak_planks:6>minecraft:book:3"); // :613
		emit(rKeys, 16, 128, 100, false, "minecraft:chest", 1, "minecraft:oak_planks:8"); // :614
		emit(rKeys, 16, 128, 100, false, "minecraft:trapped_chest", 1, "minecraft:oak_planks:8>minecraft:tripwire_hook:1"); // :615
		emit(rKeys, 16, 128, 100, false, "minecraft:note_block", 1, "minecraft:oak_planks:8>gt6:dust_redstone:1"); // :616
		emit(rKeys, 16, 128, 100, false, "minecraft:jukebox", 1, "minecraft:oak_planks:8>gt6:gem_diamond:1"); // :617
		emit(rKeys, 16, 64, 100, false, "minecraft:painting", 1, "minecraft:stick:8"); // :618
		emit(rKeys, 16, 64, 100, false, "minecraft:item_frame", 1, "minecraft:stick:8"); // :619
		// :620 OM.dust(Wood, stick.mAmount/3): U2/3 fails the U4 arm, the U9 arm conditions fail
		// (amt%U9 > amt%U72), landing on the dustDiv72 tail arm (OM.java:463-468) = x12
		long tDustAmt = OP.stick.mAmount / 3;
		assertEquals((tDustAmt * 72) / CS.U, 12, "the :620 dust arm resolves to dustDiv72 x12 (the OM.dust ladder walk)");
		emit(rKeys, 16, 40, 100, false, "minecraft:ladder", 1, "minecraft:stick:2>gt6:dust_div72_wood:12"); // :620
		emit(rKeys, 16, 16, 50, true, "minecraft:melon", 1, "minecraft:melon_slice:8>minecraft:melon_seeds:1"); // :621

		// anchor 2 — Woods:169, the vanilla-log entries of LIST_WOODS (WoodEntry 2-arg defaults)
		String[][] tLogs = {{"oak_log", "oak_planks"}, {"spruce_log", "spruce_planks"}, {"birch_log", "birch_planks"},
				{"jungle_log", "jungle_planks"}, {"acacia_log", "acacia_planks"}, {"dark_oak_log", "dark_oak_planks"}};
		for (String[] tLog : tLogs) {
			emit(rKeys, 16, 128, 4, false, "minecraft:" + tLog[0], 1, "minecraft:" + tLog[1] + ":6>gt6:dust_bark:1");
		}
		// anchor 2 — Woods:169, the 9 GT6 tree-log entries (ACTIVATED at sawing-plank-concrete-increment:
		// the gt6:<snake>_planks items live since gt-tree-planks; mPlankCountBuzz 6 + bark dust, EXCEPT the
		// Cinnamon row whose mBark is the IL.HaC_Cinnamon.get(1, IL.Food_Cinnamon.get(1, OM.dust(MT.Cinnamon)))
		// fallback chain landing gt6:dust_cinnamon — LoaderWoodDictionary.java:93)
		for (GT6TreeKind tKind : GT6TreeKind.values()) {
			String tBark = tKind == GT6TreeKind.CINNAMON ? "gt6:dust_cinnamon" : "gt6:dust_bark";
			emit(rKeys, 16, 128, 4, false, "gt6:" + tKind.snake() + "_log", 1,
					"gt6:" + tKind.snake() + "_planks:6>" + tBark + ":1");
		}
		// anchor 3 — Woods:192, the beam-walk sawing. The 7 pourable BeamEntry faces (task beam-consume-increment,
		// merged): the 6 vanilla beams ride the BeamEntry defaults (LoaderWoodDictionary:51-56: mPlankCountBuzz 7,
		// dust of the plank material) and the :175 Rubber Wood face (mPlankCountBuzz 5, material WoodRubber,
		// plank = the vanilla jungle planks per WoodDictionary.PLANKS.get(Blocks.planks, 3)). Plus the :66
		// DEFAULT_BEAM row (task sawing-plank-concrete-increment: the input gt6:wood_beam lives since
		// beam-blocks-register; the BeamEntry 2-arg defaults BeamEntry.java:48-50 give mPlankCountBuzz 7 +
		// mMaterialBeam MT.Wood; the plank output rides the IL.Plank identity mapping).
		String[][] tBeams = {
				{"oak_beam", "oak_planks", "7", "dust_oak"},
				{"spruce_beam", "spruce_planks", "7", "dust_spruce"},
				{"birch_beam", "birch_planks", "7", "dust_birch"},
				{"jungle_beam", "jungle_planks", "7", "dust_junglewood"},
				{"acacia_beam", "acacia_planks", "7", "dust_acacia"},
				{"dark_oak_beam", "dark_oak_planks", "7", "dust_dark_oak"},
				{"rubber_wood_beam", "jungle_planks", "5", "dust_wood_rubber"}};
		for (String[] tBeam : tBeams) {
			emit(rKeys, 16, 128, 4, false, "gt6:" + tBeam[0], 1,
					"minecraft:" + tBeam[1] + ":" + tBeam[2] + ">gt6:" + tBeam[3] + ":1");
		}
		emit(rKeys, 16, 128, 4, false, "gt6:wood_beam", 1, "minecraft:oak_planks:7>gt6:dust_wood:1");

		// anchors 4+5 — BlockStones:272-274 + BlockMetaType:92 over the 272-pair universe
		for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) {
			OreDictMaterial tMat = tStone.material().get();
			// the RM.sawing first-output gate (RM.java:723 ST.invalid(aOutputs[0])): prismarine
			// light/dark + quartzite carry NO plate item -> :272/:274 never fire for them
			// (declared absence); a trailing dustSmall absence would hit the Recipe.java:895 trim
			if (tReg.contains("plate|" + tMat.mNameInternal)) {
				for (int tMeta : JUSTSTONE) { // :271-273
					emit(rKeys, 16, 16, 50, false, "gt6:" + GTStoneSlabBlocks.slabPath(tStone.snake(), variant(tMeta)), 1,
							"gt6:" + GTMaterialItems.itemIdOf(OP.plate, tMat) + ":4>gt6:" + GTMaterialItems.itemIdOf(OP.dustSmall, tMat) + ":2");
				}
				// :274 — the RSTBR row with the redstone dust tail
				emit(rKeys, 16, 16, 50, false, "gt6:" + GTStoneSlabBlocks.slabPath(tStone.snake(), variant(RSTBR)), 1,
						"gt6:" + GTMaterialItems.itemIdOf(OP.plate, tMat) + ":4>gt6:" + GTMaterialItems.itemIdOf(OP.dustSmall, tMat)
								+ ":2>gt6:" + GTMaterialItems.itemIdOf(OP.dust, gregapi.data.MT.Redstone) + ":2");
			}
			for (int tMeta = 0; tMeta < 16; tMeta++) { // BlockMetaType:92
				emit(rKeys, 16, 16, 5, false, "gt6:" + GTStoneBlocks.path(tStone.snake(), variant(tMeta)), 1,
						"gt6:" + GTStoneSlabBlocks.slabPath(tStone.snake(), variant(tMeta)) + ":2");
			}
		}
		// anchor 5 — BlockMetaType:92, the BlockColored/concrete face (ACTIVATED at sawing-plank-concrete-increment:
		// the 64 concrete ids live since concrete-blocks-register; family-major x colour-major DYE_IDS 0=black..15=white)
		for (GT6ConcreteBlocks.ConcreteRow tPair : GT6ConcreteBlocks.registrationOrder()) {
			emit(rKeys, 16, 16, 5, false, "gt6:" + GT6ConcreteBlocks.path(tPair.family(), tPair.dyeIndex()), 1,
					"gt6:" + GT6ConcreteBlocks.slabPath(tPair.family(), tPair.dyeIndex()) + ":2");
		}

		// anchor 6 — OreDict:171, the treeSapling listener expanded to the 9 GT6TreeKind saplings
		for (GT6TreeKind tKind : GT6TreeKind.values()) {
			emit(rKeys, 16, 16, 1, false, "gt6:" + tKind.snake() + "_sapling", 1, "gt6:" + idOf(OP.stick, gregapi.data.MT.Wood) + ":1");
		}
		// (anchor 7 — RM.java:328/:351 stoneshapes: TRUE NEGATIVE, foreign callers only — declared, emits nothing)
		return rKeys;
	}

	/** The per-anchor live call census (the ratchet face of test 3). */
	private static Map<String, Integer> anchorCallCensus() {
		GTMaterialItems.initMaterials();
		Set<String> tReg = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tReg.add(tPair.prefix().mNameInternal + "|" + tPair.material().mNameInternal);
		}
		Map<String, Integer> rMap = new LinkedHashMap<>();
		int tWalk = 0;
		for (OreDictMaterial tMat : ANY.Wood.mToThis) {
			if (!tReg.contains("stick|" + tMat.mNameInternal)) continue;
			if (tReg.contains("bolt|" + tMat.mNameInternal)) tWalk++;
			if (tReg.contains("stickLong|" + tMat.mNameInternal)) tWalk++;
		}
		rMap.put("wood_walk", tWalk);
		rMap.put("dye_loop", DYES.length * 2);
		rMap.put("vanilla_statics", 15 + 14); // :590-621 pourable + the :604-617 plank-output rows (the increment)
		rMap.put("vanilla_logs", 6 + GT6TreeKind.values().length); // the vanilla six + the 9 GT6 tree rows (the increment)
		rMap.put("wood_beam", 1); // the Woods:192 :66 DEFAULT_BEAM row (the increment)
		int tStoneCalls = 0;
		for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) {
			if (tReg.contains("plate|" + tStone.material().get().mNameInternal)) tStoneCalls += JUSTSTONE.length + 1;
		}
		rMap.put("stone_slabs", tStoneCalls);
		rMap.put("block_slabs", GTStoneBlocks.STONES.size() * 16 + GT6ConcreteBlocks.registrationOrder().size()); // + the concrete face (the increment)
		rMap.put("saplings", GT6TreeKind.values().length);
		rMap.put("beam_walk", 7); // Woods:192, the 7 pourable BeamEntry faces (beam-consume-increment)
		return rMap;
	}

	/** Fans one RM.sawing call into its leg rows (RM.java:725-730) under the row key. */
	private static void emit(Set<String> aKeys, long aEut, long aDur, long aLube, boolean aFood,
			String aIn, long aInCount, String aOuts) {
		for (Object[] tLeg : LEGS) {
			if (aFood && "gt6:lubricant".equals(tLeg[0])) continue; // the isFood gate drops the lubricant family
			aKeys.add(aIn + ":" + aInCount + ">" + tLeg[0] + "@" + (aLube * (Long) tLeg[2])
					+ ">" + (aDur * (Long) tLeg[1]) + ">" + aEut + ">" + aOuts);
		}
	}

	/** The shipped-row key, the same face the recompute emits. */
	private static String keyOf(JsonObject aRow) {
		StringBuilder tOutputs = new StringBuilder();
		for (JsonElement tOut : aRow.getAsJsonArray("outputs")) {
			JsonObject tSlot = tOut.getAsJsonObject();
			tOutputs.append('>').append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsInt());
		}
		JsonObject tFluid = aRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject();
		return aRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString()
				+ ":" + aRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("count").getAsInt()
				+ ">" + tFluid.get("fluid").getAsString() + "@" + tFluid.get("amount").getAsInt()
				+ ">" + aRow.get("duration").getAsLong() + ">" + aRow.get("eut").getAsLong() + tOutputs;
	}

	private static String idOf(OreDictPrefix aPrefix, OreDictMaterial aMat) {
		return GTMaterialItems.itemIdOf(aPrefix, aMat);
	}

	/** The StoneVariant of an upstream meta (the declaration order IS the meta order). */
	private static gregtech6.block.stone.StoneVariant variant(int aMeta) {
		return gregtech6.block.stone.StoneVariant.VALUES[aMeta];
	}

	/** The vanilla whitelist — exactly the minecraft: ids the static tables reference. */
	private static Set<String> vanillaWhitelist() {
		Set<String> rSet = new HashSet<>();
		for (String tDye : DYES) {
			rSet.add("minecraft:" + tDye + "_stained_glass");
			rSet.add("minecraft:" + tDye + "_stained_glass_pane");
			rSet.add("minecraft:" + tDye + "_wool");
			rSet.add("minecraft:" + tDye + "_carpet");
		}
		for (String tId : new String[] {"glass", "glass_pane", "smooth_stone", "smooth_sandstone", "smooth_stone_slab",
				"sandstone_slab", "cobblestone_slab", "brick_slab", "stone_brick_slab", "nether_brick_slab", "quartz_slab",
				"stone", "sandstone", "cobblestone", "bricks", "stone_bricks", "nether_bricks", "quartz_block",
				"painting", "item_frame", "stick", "ladder", "melon", "melon_slice", "melon_seeds",
				"oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log",
				"oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks", "dark_oak_planks",
				// the :604-617 input faces (sawing-plank-concrete-increment)
				"oak_button", "oak_pressure_plate", "oak_sign", "oak_door", "oak_fence_gate", "oak_trapdoor",
				"red_bed", "crafting_table", "oak_boat", "bookshelf", "chest", "trapped_chest", "tripwire_hook",
				"note_block", "jukebox", "book"}) {
			rSet.add("minecraft:" + tId);
		}
		return rSet;
	}

	// ------------------------------------------------------------- the verbatim pins (row lookups)

	private static JsonObject findRow(JsonArray aRows, String aInputItem, String aFluid) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (aInputItem.equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())
					&& aFluid.equals(tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString())) return tRow;
		}
		throw new AssertionError("no row " + aInputItem + " on " + aFluid);
	}

	/** Full-row pin: outputs tail (">id:count" segments), duration, eut, fluid amount. */
	private static void assertRow(JsonObject aRow, String aOutputsTail, long aDuration, long aEut, long aFluidAmount) {
		StringBuilder tOutputs = new StringBuilder();
		for (JsonElement tOut : aRow.getAsJsonArray("outputs")) {
			JsonObject tSlot = tOut.getAsJsonObject();
			tOutputs.append('>').append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsInt());
		}
		assertEquals(aOutputsTail, tOutputs.toString(), "the output slot tail");
		assertEquals(aDuration, aRow.get("duration").getAsLong(), "the leg duration (aDuration x the leg multiplier)");
		assertEquals(aEut, aRow.get("eut").getAsLong(), "the call eut");
		assertEquals(aFluidAmount, aRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				"the leg fluid amount (lube x the leg multiplier)");
	}
}
