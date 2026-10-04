package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import gregapi.data.OP;
import gregtech6.block.tree.GT6BeamKind;
import gregtech6.registry.GT6BeamBlocks;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * The beam-consume-increment row-stock test: the RM walk bodies of
 * Loader_Recipes_Woods.java:188-206 over the port beam universe (the 8 GT6BeamKind
 * registrations), replayed across the three JSON faces and the coke-oven table.
 *
 * <p><b>Upstream constants</b> (verbatim replay): the BeamEntry default chain
 * (BeamEntry.java:48-61 — charcoal 1, creosote 200, plank hand/saw/buzz 3/5/7,
 * stickLong of the plank material, stick saw/lathe 3/5), the Rubber Wood face
 * (LoaderWoodDictionary.java:175 — charcoal 1, creosote 300, buzz 5, stickLathe 2,
 * plank = WoodDictionary.PLANKS.get(Blocks.planks, 3) = the vanilla jungle planks),
 * the DEFAULT_BEAM face (:66 = IL.Beam, PlankEntry(IL.Plank) 3-arg = MT.Wood,
 * PlankEntry.java:49/:61-68). Faces: RM.generify (:181-184 — Generifier, eut 0,
 * duration 1), RM.sawing (:720-732 — the five legs, eut 16, dur 128 base),
 * RM.lathing (:734-738 — one row, eut 16, dur 80), RM.CokeOven (:197-201 — 3600 t,
 * FL.Oil_Creosote.make(mCreosoteAmount) = mB direct, the WoodEntry 250 mB log face
 * RCON-verified precedent).
 *
 * <p><b>Declared zero-row faces of the walk</b>: the pulverize statement (:191) —
 * RM.pulverizing feeds ONLY the IC2 macerator / TE sawmill+pulverizer / RailCraft rock
 * crusher compat machines (RM.java:977-1017), never a GT6 RecipeMap, and the port has
 * no such compat tier; the crafting statements (:195 CLUB, :203-205 stick/plank) — the
 * vanilla-crafting domain has no port band (the log walk's :183-185 crafting rows share
 * that unported domain); the IL.Beam generify self-row (:190 for the :66 DEFAULT_BEAM) —
 * input == output, the handler mTargetGenerifying != self gate is the port canon; the
 * IL.Beam sawing row (:192 for :66) — the plank-output leg is the GT tree plank, the
 * gt-tree-planks card's face.
 *
 * <p>Task plank-mapping-sweep (2026-10-01) absorbed the sawing-card identity mapping: the
 * :66 sawing rows' plank output pours gt6:plank_wood (the wood-planks-register carrier,
 * OP.plank x MT.Wood); the six vanilla BeamEntry faces keep their upstream
 * minecraft:*_planks ids (the BEAMS table carries both faces verbatim).
 */
public class GT6RecipeMapDataBeamConsumeRowsPourTest extends GTRecipesOfflineTestBase {

	/** The per-file total census (task beam-consume-increment deltas marked). */
	private static final int SAWING_CENSUS = 4304, GENERIFIER_CENSUS = 9187, LATHE_CENSUS = 8; // 9187 = 9123 + the +64 task wire-gt-registration generifier walk re-pour (the 6 stripped rows and the 30 sawing twins ride the other maps, untouched); 4304 = 4274 + the 30 stripped-log-qol twin rows (review-seat rebase seam syncs: the pins were cut against the pre-casing / pre-plank-concrete baselines)
	/** The beam-segment censuses: 7 pourable sawing calls x 5 legs, 7 generify rows, 8 lathe rows. */
	private static final int SAWING_BEAM_ROWS = 40, GENERIFIER_BEAM_ROWS = 7, LATHE_BEAM_ROWS = 8; // 40 = the 35 BeamEntry faces + the 5 :66 DEFAULT_BEAM legs (activated by sawing-plank-concrete-increment)

	/** RM.java:723-730 leg fan (fluid, duration mult, lube mult) — the sawing-test LEGS face. */
	private static final Object[][] LEGS = {
			{"minecraft:water", 4L, 4L}, {"gt6:spdew", 4L, 4L}, {"gt6:mnwtr", 4L, 4L},
			{"gt6:distilled_water", 3L, 3L}, {"gt6:lubricant", 1L, 1L}};

	/**
	 * The per-kind replay face: (kind, plank output id or null for the blocked GT-plank leg,
	 * plank buzz count, dust id, stickLong id, stick lathe count) — the 1.7.10 Blocks.planks
	 * metas map to the six minecraft:*_planks ids; the upstream WoodEntry/BeamEntry materials
	 * map to the port dust ids through snakeCase(mNameInternal) (Oak -> dust_oak, Junglewood
	 * -> dust_junglewood, DarkOak -> dust_dark_oak, WoodRubber -> dust_wood_rubber).
	 */
	private static final Object[][] BEAMS = {
			{GT6BeamKind.OAK, "minecraft:oak_planks", 7, "gt6:dust_oak", "gt6:stick_long_oak", 5},
			{GT6BeamKind.SPRUCE, "minecraft:spruce_planks", 7, "gt6:dust_spruce", "gt6:stick_long_spruce", 5},
			{GT6BeamKind.BIRCH, "minecraft:birch_planks", 7, "gt6:dust_birch", "gt6:stick_long_birch", 5},
			{GT6BeamKind.JUNGLE, "minecraft:jungle_planks", 7, "gt6:dust_junglewood", "gt6:stick_long_junglewood", 5},
			{GT6BeamKind.ACACIA, "minecraft:acacia_planks", 7, "gt6:dust_acacia", "gt6:stick_long_acacia", 5},
			{GT6BeamKind.DARK_OAK, "minecraft:dark_oak_planks", 7, "gt6:dust_dark_oak", "gt6:stick_long_dark_oak", 5},
			{GT6BeamKind.RUBBER_WOOD, "minecraft:jungle_planks", 5, "gt6:dust_wood_rubber", "gt6:stick_long_wood_rubber", 2},
			{GT6BeamKind.WOOD, "gt6:plank_wood", 7, "gt6:dust_wood", "gt6:stick_long_wood", 5}}; // ACTIVATED by sawing-plank-concrete-increment; the plank output re-poured onto gt6:plank_wood by task plank-mapping-sweep (the absorbed IL.Plank identity mapping)

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

	/** Reads a shipped recipe_maps file verbatim and pours it under its map key. */
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6RecipeMapDataBeamConsumeRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: every touched map pours its full total, zero WARN-skips. */
	@Test
	public void theFilesPourTheirFullCensusesWithZeroSkips() throws Exception {
		JsonArray tSawing = pourShipped("sawing");
		assertEquals(SAWING_CENSUS, GT6RecipeMaps.CUTTER.mRecipeList.size(), "sawing: the map holds the census");
		assertEquals(SAWING_CENSUS, GT6RecipeMapJsonLoader.pouredCount("sawing"), "sawing: zero skips");
		assertEquals(SAWING_BEAM_ROWS, countBeamRows(tSawing), "sawing: the beam segment");
		JsonArray tGenerifier = pourShipped("generifier");
		assertEquals(GENERIFIER_CENSUS, GT6RecipeMaps.GENERIFIER.mRecipeList.size(), "generifier: the map holds the census");
		assertEquals(GENERIFIER_CENSUS, GT6RecipeMapJsonLoader.pouredCount("generifier"), "generifier: zero skips");
		assertEquals(GENERIFIER_BEAM_ROWS, countBeamRows(tGenerifier), "generifier: the beam segment");
		JsonArray tLathe = pourShipped("lathe");
		assertEquals(LATHE_CENSUS, GT6RecipeMaps.LATHE.mRecipeList.size(), "lathe: the map holds the census");
		assertEquals(LATHE_CENSUS, GT6RecipeMapJsonLoader.pouredCount("lathe"), "lathe: zero skips");
		assertEquals(LATHE_BEAM_ROWS, countBeamRows(tLathe), "lathe: every row is a beam row");
	}

	/** The file heads name the increment and its declared faces. */
	@Test
	public void theFileHeadsCarryTheBeamIncrementDeclarations() throws Exception {
		String tSawingHead = headOf("sawing");
		assertTrue(tSawingHead.contains("ACTIVATED by task beam-consume-increment"), "sawing: the Woods:192 activation is declared");
		assertTrue(tSawingHead.contains("the :66 DEFAULT_BEAM row is ACTIVATED"), "sawing: the WOOD-face activation is declared (sawing-plank-concrete-increment)");
		assertTrue(tSawingHead.contains("WoodDictionary.PLANKS.get(Blocks.planks, 3)"), "sawing: the rubber jungle-plank mapping is cited");
		String tGenerifierHead = headOf("generifier");
		assertTrue(tGenerifierHead.contains("BEAM INCREMENT (task beam-consume-increment)"), "generifier: the Woods:190 group is declared");
		assertTrue(tGenerifierHead.contains("mTargetGenerifying != self"), "generifier: the IL.Beam self-row skip cites the handler canon");
		String tLatheHead = headOf("lathe");
		assertTrue(tLatheHead.contains("task beam-consume-increment"), "lathe: the owning card is declared");
		assertTrue(tLatheHead.contains("RM.java:734-738"), "lathe: the engine citation");
		assertTrue(tLatheHead.contains("RM.java:977-1017"), "lathe: the pulverize TRUE-NEGATIVE citation");
		assertTrue(tLatheHead.contains("vanilla-crafting domain has no port band"), "lathe: the crafting-face declaration");
	}

	/** The sawing beam face: set-exact against the upstream recompute (the 8 faces incl. the :66 DEFAULT_BEAM), verbatim pins. */
	@Test
	public void theSawingBeamFaceIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("sawing");
		// set-exact two-way walk over the 7 pourable faces
		Set<String> tShipped = new HashSet<>(), tExpected = new HashSet<>();
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (inputItem(tRow).endsWith("_beam")) {
				tShipped.add(keyOf(tRow));
			}
		}
		for (Object[] tBeam : BEAMS) {
			if (tBeam[1] == null) continue; // defensive: the WOOD row carries the gt6:plank_wood sweep re-pour
			String tPlank = (String) tBeam[1]; // the vanilla faces are "minecraft:*_planks", the WOOD face is the gt6 carrier
			for (Object[] tLeg : LEGS) {
				tExpected.add("gt6:" + snake((GT6BeamKind) tBeam[0]) + "_beam:1>" + tLeg[0] + "@" + (4L * (Long) tLeg[2])
						+ ">" + (128L * (Long) tLeg[1]) + ">16>" + tPlank + ":" + tBeam[2] + ">" + tBeam[3] + ":1");
			}
		}
		Set<String> tMissing = new HashSet<>(tExpected);
		tMissing.removeAll(tShipped);
		Set<String> tStale = new HashSet<>(tShipped);
		tStale.removeAll(tExpected);
		assertTrue(tMissing.isEmpty(), "the beam segment trails the recompute: " + tMissing);
		assertTrue(tStale.isEmpty(), "the beam segment holds rows the recompute never emits: " + tStale);
		// verbatim pins: the oak water leg and the rubber distilled-water leg
		assertRow(findRow(tRows, "gt6:oak_beam", "minecraft:water"),
				">minecraft:oak_planks:7>gt6:dust_oak:1", 512, 16, 16);
		assertRow(findRow(tRows, "gt6:rubber_wood_beam", "gt6:distilled_water"),
				">minecraft:jungle_planks:5>gt6:dust_wood_rubber:1", 384, 16, 12);
		// the :66 DEFAULT_BEAM face is active since sawing-plank-concrete-increment — its 5 legs
		// ride the set-exact walk above (the BEAMS WOOD row emits them)
	}

	/** The lathe face: 8 rows, eut 16 / dur 80, the stick counts 5/5/5/5/5/5/2/5, dust tail. */
	@Test
	public void theLatheFileIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("lathe");
		assertEquals(LATHE_BEAM_ROWS, tRows.size(), "lathe: exactly the 8 beam faces");
		Set<String> tInputs = new HashSet<>();
		for (int i = 0; i < tRows.size(); i++) {
			JsonObject tRow = tRows.get(i).getAsJsonObject();
			String tIn = tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
			tInputs.add(tIn);
			Object[] tBeam = byKind(GT6BeamKind.values()[i]); // GT6BeamKind order, one row per face
			assertEquals("gt6:" + snake((GT6BeamKind) tBeam[0]) + "_beam", tIn, "lathe: the row order rides the enum");
			assertEquals(16, tRow.get("eut").getAsLong(), "RM.lathing(16, 80) eut verbatim");
			assertEquals(80, tRow.get("duration").getAsLong(), "RM.lathing(16, 80) duration verbatim");
			assertFalse(tRow.has("fluidInputs"), "the lathe rows carry no fluids");
			// the upstream call: stick x mStickCountLathe + OM.dust(mMaterialBeam) x1
			assertEquals(">" + tBeam[4] + ":" + tBeam[5] + ">" + tBeam[3] + ":1", outputsTail(tRow),
					"lathe: the " + tIn + " output tail (stickLong x mStickCountLathe + dust)");
			assertTrue(tRow.has("comment") && tRow.get("comment").getAsString().startsWith("Loader_Recipes_Woods.java:193"),
					"lathe: every row cites the walk line");
		}
		assertEquals(8, tInputs.size(), "lathe: one row per beam kind, no duplicates");
	}

	/** The generifier beam face: 7 rows into the IL.Beam target, no self-row. */
	@Test
	public void theGenerifierBeamFaceIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("generifier");
		Set<String> tShipped = new HashSet<>();
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			String tIn = inputItem(tRow);
			if (!tIn.endsWith("_beam")) continue;
			assertTrue(tRow.get("comment").getAsString().startsWith("Loader_Recipes_Woods.java:190"),
					"the generify row cites the direct call");
			assertEquals(0, tRow.get("eut").getAsLong(), "RM.generify eut verbatim (RM.java:183)");
			assertEquals(1, tRow.get("duration").getAsLong(), "RM.generify duration verbatim");
			assertFalse(tRow.has("fluidInputs"), "the generify rows carry no fluids");
			assertEquals(">gt6:wood_beam:1", outputsTail(tRow), "the IL.Beam generify target = gt6:wood_beam");
			tShipped.add(tIn);
		}
		Set<String> tExpected = new HashSet<>();
		// task beam-fireproof-closeout: the consume walk (Loader_Recipes_Woods.java:190) for the
		// 13 residual kinds (Beam3/A/B/C, enum slots 8+) is the declared UNPOURED face — those
		// rows land with the recipe-backfill wave (the consume walk is that wave's domain), the
		// pin stays the original 8-kind slice (Beam1x4 + Beam2x4 minus the WOOD self-row).
		for (int i = 0; i < 8; i++) {
			GT6BeamKind tKind = GT6BeamKind.values()[i];
			if (tKind == GT6BeamKind.WOOD) continue; // the self-row: declared degenerate skip
			tExpected.add("gt6:" + tKind.snake() + "_beam");
		}
		assertEquals(tExpected, tShipped, "the 7 non-degenerate generify faces, exactly");
	}

	/** The three-universe id face: the beam segments' gt6 ids live in items x blocks x beams; the minecraft ids in the vanilla whitelist. */
	@Test
	public void everyItemIdTheBeamSegmentsReferenceIsRegistered() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			tUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GT6BeamKind tKind : GT6BeamKind.values()) tUniverse.add("gt6:" + GT6BeamBlocks.path(tKind));
		for (String tPlank : new String[] {"oak_planks", "spruce_planks", "birch_planks", "jungle_planks",
				"acacia_planks", "dark_oak_planks"}) tUniverse.add("minecraft:" + tPlank);
		Set<String> tMissing = new HashSet<>();
		// only the beam segments of each file walk this universe — the sawing/generifier
		// remainder carries the stone universe, jurisdiction of their owning cards' tests
		for (JsonElement tRow : pourShipped("sawing")) {
			if (inputItem(tRow.getAsJsonObject()).endsWith("_beam")) collectMissing(tRow, tUniverse, tMissing);
		}
		for (JsonElement tRow : pourShipped("lathe")) collectMissing(tRow, tUniverse, tMissing);
		for (JsonElement tRow : pourShipped("generifier")) {
			if (inputItem(tRow.getAsJsonObject()).endsWith("_beam")) collectMissing(tRow, tUniverse, tMissing);
		}
		assertTrue(tMissing.isEmpty(), "unregistered item ids (these rows would WARN-skip live): " + tMissing);
	}

	/** The coke-oven table face: 8 rows in enum order, 3600 t, 200/300 mB creosote, charcoal x1. */
	@Test
	public void theCokeOvenBeamRowsCarryTheUpstreamShapes() {
		GT6RecipesCokeOven.StaticRow[] tBeams = GT6RecipesCokeOven.table().stream()
				.filter(tRow -> tRow.inBeam() != null).toArray(GT6RecipesCokeOven.StaticRow[]::new);
		assertEquals(8, tBeams.length, "the 8 wood-beam rows (Loader_Recipes_Woods.java:197-201)");
		for (int i = 0; i < tBeams.length; i++) {
			GT6RecipesCokeOven.StaticRow tRow = tBeams[i];
			assertEquals(GT6BeamKind.values()[i], tRow.inBeam(), "the row order rides the enum");
			assertEquals(1, tRow.inCount(), "one beam per row");
			assertEquals(3600, tRow.duration(), ":200 carries the 3600 t duration");
			assertEquals(GT6RecipesCokeOven.FLUID_CREOSOTE, tRow.fluid());
			assertEquals(tRow.inBeam() == GT6BeamKind.RUBBER_WOOD ? 300 : 200, tRow.fluidMB(),
					"the BeamEntry creosote (200 default, :175 rubber = 300)");
			assertEquals(1, tRow.outputs().length, "the charcoal array is a single slot (mCharcoalCount 1)");
			assertEquals(OP.gem, tRow.outputs()[0].prefix(), "gem.mat(MT.Charcoal, 1)");
			assertSame(MT.Charcoal, tRow.outputs()[0].material());
			assertEquals(1, tRow.outputs()[0].count());
		}
	}

	/** The coke-oven end-to-end pour: the beam input seam resolves, the lookup returns charcoal + creosote. */
	@Test
	public void theCokeOvenBeamRowsPourEndToEnd() {
		// the offline beam stand-ins: the vanilla planks family (distinct, registered, thematic)
		Item[] tStandIns = {Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS,
				Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS};
		GT6RecipesCokeOven.sInputItemResolver = tRow -> tRow.inBeam() != null
				? tStandIns[tRow.inBeam().ordinal()]
				: Items.COAL;
		GT6RecipesCokeOven.sOutputItemResolver = tOutput -> Items.COAL; // the charcoal stand-in
		GT6RecipesCokeOven.sFluidResolver = tFluidId -> Fluids.WATER; // the creosote carrier

		GT6RecipesCokeOven.load();
		assertEquals(GT6RecipesCokeOven.table().size(), GT6RecipeMaps.COKE_OVEN.mRecipeList.size(),
				"every row resolves — the beam input seam included — zero skips");

		// the oak-beam lookup: 3600 t, charcoal x1, 200 mB
		ItemStack[] tInputs = {new ItemStack(Items.OAK_PLANKS, 4)};
		Recipe tFound = GT6RecipeMaps.COKE_OVEN.findRecipe(null, 16, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound, "the oak-beam row must be findable after the pour");
		assertEquals(3600, tFound.mDuration);
		ItemStack[] tOutputs = tFound.getOutputs(1);
		assertEquals(1, tOutputs.length);
		assertEquals(Items.COAL, tOutputs[0].getItem(), "the charcoal output");
		FluidStack[] tFluids = tFound.getFluidOutputs(1);
		assertEquals(1, tFluids.length);
		assertEquals(200, tFluids[0].getAmount(), "the BeamEntry default creosote = 200 mB");

		// the rubber-beam lookup: the :175 face carries 300 mB
		ItemStack[] tRubber = {new ItemStack(Items.CRIMSON_PLANKS, 4)};
		Recipe tRubberFound = GT6RecipeMaps.COKE_OVEN.findRecipe(null, 16, ItemStack.EMPTY, null, tRubber);
		assertNotNull(tRubberFound, "the rubber-beam row must be findable after the pour");
		assertEquals(300, tRubberFound.getFluidOutputs(1)[0].getAmount(), "the :175 rubber face = 300 mB");
	}

	/** load() stays idempotent with the beam rows in the table. */
	@Test
	public void theCokeOvenPourStaysIdempotentWithBeams() {
		GT6RecipesCokeOven.sInputItemResolver = tRow -> tRow.inBeam() != null ? Items.OAK_PLANKS : Items.COAL;
		GT6RecipesCokeOven.sOutputItemResolver = tOutput -> Items.COAL;
		GT6RecipesCokeOven.sFluidResolver = tFluidId -> Fluids.WATER;
		GT6RecipesCokeOven.load();
		int tFirst = GT6RecipeMaps.COKE_OVEN.mRecipeList.size();
		assertEquals(47, tFirst);
		GT6RecipesCokeOven.load();
		assertEquals(tFirst, GT6RecipeMaps.COKE_OVEN.mRecipeList.size(), "the second load must not stack");
	}

	// ------------------------------------------------------------------ helpers

	private static void assertSame(Object aExpected, Object aActual) {
		org.junit.jupiter.api.Assertions.assertSame(aExpected, aActual);
	}

	/** The input item of a row, or "" for the fluid-only rows (empty inputs array). */
	private static String inputItem(JsonObject aRow) {
		JsonArray tInputs = aRow.getAsJsonArray("inputs");
		return tInputs.size() == 0 ? "" : tInputs.get(0).getAsJsonObject().get("item").getAsString();
	}

	/** The beam rows of one file (input id ends with _beam). */
	private static int countBeamRows(JsonArray aRows) {
		int tCount = 0;
		for (JsonElement tElement : aRows) {
			if (inputItem(tElement.getAsJsonObject()).endsWith("_beam")) tCount++;
		}
		return tCount;
	}

	private static String headOf(String aKey) throws Exception {
		InputStream tStream = GT6RecipeMapDataBeamConsumeRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + aKey + ".json");
		assertNotNull(tStream);
		return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject().get("comment").getAsString();
	}

	private static Object[] byKind(GT6BeamKind aKind) {
		for (Object[] tBeam : BEAMS) if (tBeam[0] == aKind) return tBeam;
		throw new AssertionError(aKind + " missing from BEAMS");
	}

	private static String snake(GT6BeamKind aKind) {
		return aKind.snake();
	}

	private static void collectMissing(JsonElement aElement, Set<String> aUniverse, Set<String> aMissing) {
		JsonObject tRow = aElement.getAsJsonObject();
		for (String tLeg : new String[] {"inputs", "outputs"}) {
			for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
				String tId = tSlot.getAsJsonObject().get("item").getAsString();
				if (!aUniverse.contains(tId)) aMissing.add(tId);
			}
		}
	}

	private static String outputsTail(JsonObject aRow) {
		StringBuilder tOutputs = new StringBuilder();
		for (JsonElement tOut : aRow.getAsJsonArray("outputs")) {
			JsonObject tSlot = tOut.getAsJsonObject();
			tOutputs.append('>').append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsInt());
		}
		return tOutputs.toString();
	}

	/** The shipped-row key, the same face the sawing-test recompute emits. */
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
		assertEquals(aOutputsTail, outputsTail(aRow), "the output slot tail");
		assertEquals(aDuration, aRow.get("duration").getAsLong(), "the leg duration (aDuration x the leg multiplier)");
		assertEquals(aEut, aRow.get("eut").getAsLong(), "the call eut");
		assertEquals(aFluidAmount, aRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				"the leg fluid amount (lube x the leg multiplier)");
	}
}
