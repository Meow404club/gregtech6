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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The circuit synthesis-chain rows pour test (task circuit-chain-recipes) — the
 * MultiItemTechnological.java:546-770 Press band via the SECOND press file key, the
 * Bath soldering band :720-752 and the LaserEngraver crystal/wiring lens bands
 * (Loader_Recipes_Other.java:147-167), poured through the real
 * {@link GT6RecipeMapJsonLoader} seam with verbatim spot checks reading the shipped
 * rows directly (the GT6ElectrodePressRowsPourTest posture).
 *
 * <p><b>Row accounting (acceptance ① — the upstream-call-site ledger)</b>:
 * <ul>
 * <li><b>press2.json, 205 rows</b> = the Press statement blocks of MIT:546-770 expanded
 *     over the PORT-universe walks (the 2026-10-04 live dump): Plate Empty walk
 *     :548-553 = ANY.SiO2 {22 dust-true} x ANY.Plastic {5 plate-true} = 110; the six
 *     Plate+Wire rows :573-579 + the HSLA row :581 = 7; the Cu parts walk :597-610 =
 *     ANY.Cu {AnnealedCopper, Copper} x 12 statements = 24; :611-616 = 6; the Magic
 *     parts :618-629 = 4 wireFine-true materials x 3 = 12; the Enderium/Signalum parts
 *     :630-636 = 7; the EnderPearl/EnderEye parts :638-649 = 12; the boards :674-698 =
 *     18 + 3 + 2 = 23; the crystal processors :767-770 = 4.</li>
 * <li><b>bath.json, +7 rows</b> = the Sn legs of the 31-row soldering band :720-752
 *     (:721/:724/:727/:730/:733/:736/:742, gt6:tin_molten 288 mB = upstream
 *     MT.Sn.liquid(U2) at the 144 L/unit molten convention, 0 EUt / 64 t). The 24 CUT
 *     rows carry the head declaration: 7 Pb legs + 9 SolderingAlloy legs (gt6:lead_molten
 *     and gt6:soldering_alloy_molten are UNREGISTERED — the MOLTEN material flags exist,
 *     the fluid specs never ported; the b2a lightning H2O2/NO precedent) + the 8 BC rows
 *     (:745-752) whose output items are the MD.BC_SILICON-gated BuildCraft circuits —
 *     CUT per the card ruling, which orphans the card-1 circuit_board_bc_* eight.</li>
 * <li><b>laserengraver.json, +447 rows</b> = the two listener bands per PORT lens item
 *     with a VERIFIED upstream DYE assignment (the MT.java .lens(DYE_INDEX_*) archaeology
 *     intersected with the 114 port lens items): GREEN 11 lenses x 27 rows (Diamond 12 +
 *     Emerald 8 + Sapphire-minus-Ruby 6 + Ruby 1, :149-:154) + RED 15 lenses x 10 rows
 *     (ANY.Cu foil 2 + the eight singles :159-:166). The lens leg rides
 *     {@link Recipe#sNotConsumable}'s lens disjunct — the port face for the upstream
 *     {@code ST.amount(0)} non-consumable idiom (the replicator-USB/slicer-blade
 *     posture), because the JSON v1 count floor cannot carry a zero-count slot.</li>
 * </ul>
 *
 * <p><b>The 11-item disposition (acceptance ②)</b>: circuit_magic 30311 /
 * circuit_enderium 30313 / circuit_signalum 30315 REGISTERED (GT6CircuitChain siblings —
 * MT.Magic/MT.Enderium/MT.Signalum all live in the port material tree); the eight
 * circuit_bc_* 30380-30387 CUT (BuildCraft-gated upstream, no BC in the port).
 * The :765 Crystal Socket crafting row is the MTE crafting 'C' column — the
 * machines-domain card (declared out of this card).
 *
 * <p>KJS surface (the card declaration): datapack-domain JSON rows + the RM map-key
 * seam defer — JSON is naturally mutable, the loader key join is the kjs binding card's
 * face (the b-series template line).
 */
public class GT6CircuitChainRecipesRowsPourTest extends GTRecipesOfflineTestBase {

	private static final int PRESS2_ROWS = 205;
	private static final int BATH_CARD_ROWS = 7;
	private static final int LASER_CARD_ROWS = 447;
	private static final int GREEN_LENS = 11, RED_LENS = 15;

	/** The verified upstream green-lens materials' port lens ids (MT.java DYE_INDEX_Green ∩ port lens items). */
	private static final Set<String> GREEN_LENS_IDS = Set.of("lens_green_diamond", "lens_emerald", "lens_spinel",
			"lens_amazonite", "lens_hexorium_green", "lens_green_jasper", "lens_cats_eye", "lens_infused_earth",
			"lens_green_aventurine", "lens_green_sapphire", "lens_jade");
	/** The verified upstream red-lens materials' port lens ids (MT.java DYE_INDEX_Red ∩ port lens items). */
	private static final Set<String> RED_LENS_IDS = Set.of("lens_red_diamond", "lens_bixbite", "lens_almandine",
			"lens_spessartine", "lens_hexorium_red", "lens_red_jasper", "lens_dragon_eye", "lens_infused_fire",
			"lens_red_aventurine", "lens_ruby", "lens_balas_ruby", "lens_onyx_red", "lens_zircon",
			"lens_realgar", "lens_redstone");

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
		// the hermetic bracket (the press-electrodes posture): reset FIRST — the neoforge
		// junit FML boot runs the whole static pour suite at modloading
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

	/** Reads a shipped recipe_maps file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6CircuitChainRecipesRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	private static java.util.List<JsonObject> cardRows(JsonArray aRows, String aCommentPrefix) {
		java.util.List<JsonObject> rRows = new java.util.ArrayList<>();
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().startsWith(aCommentPrefix)) rRows.add(tRow);
		}
		return rRows;
	}

	private static String slotId(JsonElement aSlot) {
		return aSlot.getAsJsonObject().get("item").getAsString();
	}

	// ---------------------------------------------------------------- press2

	/** The census: the 205 chain Press rows, zero skips, tracked under the press2 key. */
	@Test
	public void press2FilePoursTheChainCensusWithZeroSkips() throws Exception {
		JsonArray tRows = pourShipped("press2");
		assertEquals(PRESS2_ROWS, tRows.size(), "the shipped press2 array");
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor("press2");
		assertNotNull(tMap, "the press2 dual-file key resolves to the SAME PRESS map (the sawing ruling)");
		assertTrue(tMap == GT6RecipeMapJsonLoader.mapFor("press"), "press2 pours into the press map");
		assertEquals(PRESS2_ROWS, GT6RecipeMapJsonLoader.pouredCount("press2"),
				"press2: the tracker mirrors the pour (a smaller number = WARN-skipped rows)");
	}

	/** The per-block walk accounting, each block pinned against the shipped file. */
	@Test
	public void press2BlockAccountingMatchesTheDeclaredPortUniverse() throws Exception {
		JsonArray tRows = pourShipped("press2");
		java.util.Map<String, Integer> tPerLine = new java.util.TreeMap<>();
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment")) continue;
			String tComment = tRow.get("comment").getAsString();
			assertTrue(tComment.startsWith("MultiItemTechnological.java:"), "every card row cites its anchor: " + tComment);
			String tLine = tComment.substring("MultiItemTechnological.java:".length()).split("[ :—]")[0];
			tPerLine.merge(tLine, 1, Integer::sum);
		}
		// the block ledger: walk x port-universe (the class doc table)
		assertEquals(110, tPerLine.get("551"), ":548-553 the Plate Empty walk = 22 SiO2 x 5 Plastic");
		for (String tLine : new String[] {"573", "574", "575", "577", "578", "579", "581"}) {
			assertEquals(1, tPerLine.get(tLine), ":" + tLine + " the single Plate+Wire/HSLA row");
		}
		int tCuParts = 0;
		for (String tLine : new String[] {"598", "599", "600", "601", "602", "603", "604", "605", "606", "607", "608", "609"}) {
			tCuParts += tPerLine.get(tLine);
		}
		assertEquals(24, tCuParts, ":597-610 the Cu walk = 2 members x 12 statements");
		for (String tLine : new String[] {"611", "612", "613", "614", "615", "616", "630", "767", "768", "769", "770"}) {
			assertEquals(1, tPerLine.get(tLine), ":" + tLine + " the single-statement row");
		}
		for (String tLine : new String[] {"618", "619", "620", "621", "622", "623", "624", "625", "626", "627", "628", "629"}) {
			assertEquals(1, tPerLine.get(tLine), ":" + tLine + " one Magic-part row per material leg");
		}
		int tSignalum = 0, tEnder = 0, tBoards = 0;
		for (String tLine : new String[] {"631", "632", "633", "634", "635", "636"}) tSignalum += tPerLine.get(tLine);
		for (String tLine : new String[] {"638", "639", "640", "641", "642", "643", "644", "645", "646", "647", "648", "649"}) tEnder += tPerLine.get(tLine);
		for (String tLine : new String[] {"674", "675", "676", "677", "678", "679", "680", "681", "682", "683", "684", "685",
				"686", "687", "688", "689", "690", "691", "693", "694", "695", "697", "698"}) tBoards += tPerLine.get(tLine);
		assertEquals(6, tSignalum, ":631-636 the Signalum part pair");
		assertEquals(12, tEnder, ":638-649 the EnderPearl/EnderEye parts");
		assertEquals(23, tBoards, ":674-698 the boards = 18 + 3 + 2");
		assertEquals(PRESS2_ROWS, tPerLine.values().stream().mapToInt(Integer::intValue).sum(), "the blocks sum to the census");
	}

	/** Verbatim spot checks: the Plate Empty walk head, the HSLA row, a board row, a processor row. */
	@Test
	public void press2VerbatimSpotChecks() throws Exception {
		JsonArray tRows = pourShipped("press2");
		JsonObject tFirst = tRows.get(0).getAsJsonObject();
		assertEquals("gt6:plate_plastic", slotId(tFirst.getAsJsonArray("inputs").get(0)), ":551 the Plastic plate leg");
		assertEquals("gt6:dust_silicon_dioxide", slotId(tFirst.getAsJsonArray("inputs").get(1)), ":551 the SiO2 dust leg");
		assertEquals("gt6:circuit_plate_empty", slotId(tFirst.getAsJsonArray("outputs").get(0)), ":551 the output");
		assertEquals(64, tFirst.get("duration").getAsInt(), ":551 64 t");
		assertEquals(16, tFirst.get("eut").getAsInt(), ":551 16 EUt");
		boolean tHsla = false, tProcessor = false, tBoard = false;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			String tComment = tRow.get("comment").getAsString();
			if (tComment.startsWith("MultiItemTechnological.java:581")) {
				assertEquals("gt6:plate_hslasteel", slotId(tRow.getAsJsonArray("inputs").get(0)), ":581 the HSLA plate");
				assertEquals("gt6:circuit_wire_gold", slotId(tRow.getAsJsonArray("inputs").get(1)), ":581 the gold wiring");
				assertEquals("gt6:circuit_plate_hsla", slotId(tRow.getAsJsonArray("outputs").get(0)), ":581 the output");
				tHsla = true;
			} else if (tComment.startsWith("MultiItemTechnological.java:674")) {
				assertEquals(4, tRow.getAsJsonArray("inputs").get(1).getAsJsonObject().get("count").getAsInt(),
						":674 four parts per board");
				assertEquals("gt6:circuit_board_basic", slotId(tRow.getAsJsonArray("outputs").get(0)), ":674 the board");
				tBoard = true;
			} else if (tComment.startsWith("MultiItemTechnological.java:767")) {
				assertEquals("gt6:processor_crystal_empty", slotId(tRow.getAsJsonArray("inputs").get(0)), ":767 the socket");
				assertEquals("gt6:processor_crystal_diamond", slotId(tRow.getAsJsonArray("outputs").get(0)), ":767 the processor");
				assertEquals(16, tRow.get("duration").getAsInt(), ":767 16 t");
				tProcessor = true;
			}
		}
		assertTrue(tHsla && tBoard && tProcessor, "the spot-check rows all shipped");
	}

	// ---------------------------------------------------------------- bath

	/** The census: 1 baseline + the 7 Sn soldering rows, zero skips. */
	@Test
	public void bathFilePoursTheSolderingCensusWithZeroSkips() throws Exception {
		JsonArray tRows = pourShipped("bath");
		assertEquals(1 + BATH_CARD_ROWS, tRows.size(), "the baseline smoke row + the seven Sn legs");
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor("bath");
		assertNotNull(tMap, "bath resolves");
		assertEquals(1 + BATH_CARD_ROWS, GT6RecipeMapJsonLoader.pouredCount("bath"),
				"bath: the tracker mirrors the pour (a smaller number = WARN-skipped rows)");
	}

	/** The seven Sn rows verbatim: board + tin_molten 288 mB -> circuit, 0 EUt / 64 t. */
	@Test
	public void bathSolderRowsAreTheSnLegsVerbatim() throws Exception {
		JsonArray tRows = pourShipped("bath");
		java.util.List<JsonObject> tCard = cardRows(tRows, "MultiItemTechnological.java:");
		assertEquals(BATH_CARD_ROWS, tCard.size(), "exactly the seven Sn legs cite their anchors");
		String[][] tExpected = { // {anchor, board, circuit}
				{"721", "circuit_board_basic", "circuit_basic"}, {"724", "circuit_board_good", "circuit_good"},
				{"727", "circuit_board_advanced", "circuit_good"}, {"730", "circuit_board_elite", "circuit_advanced"},
				{"733", "circuit_board_master", "circuit_elite"}, {"736", "circuit_board_ultimate", "circuit_master"},
				{"742", "circuit_board_signalum", "circuit_signalum"}};
		Set<String> tSeen = new HashSet<>();
		for (JsonObject tRow : tCard) {
			String tComment = tRow.get("comment").getAsString();
			String tAnchor = tComment.substring("MultiItemTechnological.java:".length()).split("[ ]")[0];
			tSeen.add(tAnchor);
			assertEquals(0, tRow.get("eut").getAsInt(), tAnchor + " rides the TU no-constant-power face");
			assertEquals(64, tRow.get("duration").getAsInt(), tAnchor + " 64 t");
			assertEquals(1, tRow.getAsJsonArray("fluidInputs").size(), tAnchor + " one fluid leg");
			JsonObject tFluid = tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject();
			assertEquals("gt6:tin_molten", tFluid.get("fluid").getAsString(), tAnchor + " the Sn leg");
			assertEquals(288, tFluid.get("amount").getAsInt(), tAnchor + " U2 at the 144 L/unit molten convention");
		}
		for (String[] tRow : tExpected) {
			assertTrue(tSeen.contains(tRow[0]), ":" + tRow[0] + " ships");
		}
		// the :742 leg verbatim (the M/E/S band's sole pour row)
		for (JsonObject tRow : tCard) {
			if (tRow.get("comment").getAsString().startsWith("MultiItemTechnological.java:742")) {
				assertEquals("gt6:circuit_board_signalum", slotId(tRow.getAsJsonArray("inputs").get(0)), ":742 the board");
				assertEquals("gt6:circuit_signalum", slotId(tRow.getAsJsonArray("outputs").get(0)), ":742 the circuit");
			}
		}
	}

	/** The CUT declaration (acceptance ②): the head names all 24 abandoned rows + the 11-item disposition. */
	@Test
	public void bathHeadDeclaresEveryAbandonedRow() throws Exception {
		String tPath = "/data/gt6/recipe_maps/bath.json";
		InputStream tStream = GT6CircuitChainRecipesRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream);
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		assertTrue(tDoc.has("comment"), "the CUT declaration rides the head comment");
		String tHead = tDoc.get("comment").getAsString();
		for (String tLine : new String[] {":720", ":723", ":726", ":729", ":732", ":735", ":741"}) {
			assertTrue(tHead.contains(tLine), "the Pb leg " + tLine + " is declared CUT");
		}
		for (String tLine : new String[] {":722", ":725", ":728", ":731", ":734", ":737", ":739", ":740", ":743"}) {
			assertTrue(tHead.contains(tLine), "the SolderingAlloy leg " + tLine + " is declared CUT");
		}
		assertTrue(tHead.contains(":745"), "the BC band opener :745 is declared CUT");
		assertTrue(tHead.contains("752"), "the BC band tail :752 is declared CUT");
		assertTrue(tHead.contains("lead_molten") && tHead.contains("soldering_alloy_molten"),
				"the two unregistered molten fluids are named");
		for (String tItem : new String[] {"circuit_magic", "circuit_enderium", "circuit_signalum"}) {
			assertTrue(tHead.contains(tItem), "the registered gap sibling " + tItem + " is dispositioned");
		}
		assertTrue(tHead.contains("circuit_bc_"), "the eight CUT BC circuits are dispositioned");
	}

	// ---------------------------------------------------------------- laserengraver

	/** The census: 1 baseline + the 447 lens-band rows, zero skips. */
	@Test
	public void laserFilePoursTheLensBandCensusWithZeroSkips() throws Exception {
		JsonArray tRows = pourShipped("laserengraver");
		assertEquals(1 + LASER_CARD_ROWS, tRows.size(), "the baseline smoke row + the 447 lens rows");
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor("laserengraver");
		assertNotNull(tMap, "laserengraver resolves");
		assertEquals(1 + LASER_CARD_ROWS, GT6RecipeMapJsonLoader.pouredCount("laserengraver"),
				"laserengraver: the tracker mirrors the pour");
	}

	/** The band accounting: 11 green lenses x 27 + 15 red lenses x 10, the exact lens-id sets pinned. */
	@Test
	public void laserBandsExpandOverTheVerifiedLensUniverse() throws Exception {
		JsonArray tRows = pourShipped("laserengraver");
		java.util.List<JsonObject> tCard = cardRows(tRows, "Loader_Recipes_Other.java:");
		assertEquals(LASER_CARD_ROWS, tCard.size(), "exactly the lens rows cite their anchors");
		Set<String> tGreenSeen = new HashSet<>(), tRedSeen = new HashSet<>();
		int tGreen = 0, tRed = 0;
		for (JsonObject tRow : tCard) {
			String tComment = tRow.get("comment").getAsString();
			boolean tIsGreen = tComment.contains("green lens band");
			String tLens = tComment.substring(tComment.indexOf("(lens_") + 1, tComment.indexOf(')', tComment.indexOf("(lens_")));
			if (tIsGreen) { tGreen++; tGreenSeen.add(tLens); }
			else { tRed++; tRedSeen.add(tLens); }
		}
		assertEquals(GREEN_LENS * 27, tGreen, "the green band = 11 lenses x 27 rows");
		assertEquals(RED_LENS * 10, tRed, "the red band = 15 lenses x 10 rows");
		assertEquals(GREEN_LENS_IDS, tGreenSeen, "the green lens universe is exactly the verified DYE_INDEX_Green set");
		assertEquals(RED_LENS_IDS, tRedSeen, "the red lens universe is exactly the verified DYE_INDEX_Red set");
	}

	/** Verbatim spot checks: a green crystal row and a red wiring row, columns and legs. */
	@Test
	public void laserVerbatimSpotChecks() throws Exception {
		JsonArray tRows = pourShipped("laserengraver");
		boolean tGreen = false, tRed = false;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			String tComment = tRow.has("comment") ? tRow.get("comment").getAsString() : "";
			if (tComment.startsWith("Loader_Recipes_Other.java:149") && tComment.contains("lens_emerald")
					&& tComment.contains("plateGem diamond ")) {
				assertEquals("gt6:plate_gem_diamond", slotId(tRow.getAsJsonArray("inputs").get(0)), ":149 the gem leg");
				assertEquals("gt6:lens_emerald", slotId(tRow.getAsJsonArray("inputs").get(1)), ":149 the lens leg");
				assertEquals("gt6:circuit_crystal_diamond", slotId(tRow.getAsJsonArray("outputs").get(0)), ":149 the crystal");
				assertEquals(64, tRow.get("duration").getAsInt(), ":149 64 t");
				assertEquals(256, tRow.get("eut").getAsInt(), ":149 256 EUt");
				tGreen = true;
			} else if (tComment.startsWith("Loader_Recipes_Other.java:158") && tComment.contains("lens_redstone")
					&& tComment.contains("foil annealed_copper ")) {
				assertEquals("gt6:foil_annealed_copper", slotId(tRow.getAsJsonArray("inputs").get(0)), ":158 the foil leg");
				assertEquals(4, tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("count").getAsInt(), ":158 foil x4");
				assertEquals("gt6:lens_redstone", slotId(tRow.getAsJsonArray("inputs").get(1)), ":158 the lens leg");
				assertEquals("gt6:circuit_wire_copper", slotId(tRow.getAsJsonArray("outputs").get(0)), ":158 the wiring");
				assertEquals(16, tRow.get("eut").getAsInt(), ":158 16 EUt");
				tRed = true;
			}
		}
		assertTrue(tGreen && tRed, "the spot-check rows all shipped");
	}

	// ---------------------------------------------------------------- cross-file

	/**
	 * The live-id face (the B1 pattern): every gt6 id the three files reference is a member
	 * of the registration walks' universe (GTMaterialItems + GTMaterialBlocks + the plain-item
	 * homes GT6CircuitChain/GT6Batteries), and every fluid is the one registered molten carrier.
	 */
	@Test
	public void everyGt6IdTheChainFilesReferenceIsRegistered() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials();
		Set<String> tUniverse = new HashSet<>();
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialBlocks.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (var tRow : gregtech6.registry.GT6CircuitChain.ROWS) { // var: the RegistryObject stays unnamed (the neo leg)
			tUniverse.add("gt6:" + tRow.path());
		}
		for (var tRow : gregtech6.registry.GT6Batteries.CIRCUIT_ROWS) {
			tUniverse.add("gt6:" + tRow.path());
		}
		assertFalse(tUniverse.isEmpty(), "the id universe built");

		Set<String> tFluids = Set.of("gt6:tin_molten", "minecraft:water");
		Set<String> tMissing = new java.util.TreeSet<>();
		for (String tFile : new String[] {"press2", "bath", "laserengraver"}) {
			InputStream tStream = GT6CircuitChainRecipesRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tFile + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement tElement : tDoc.getAsJsonArray("recipes")) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"inputs", "outputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("item").getAsString();
						if (tId.startsWith("gt6:") && !tUniverse.contains(tId)) tMissing.add(tFile + " " + tId);
					}
				}
				if (tRow.has("fluidInputs")) {
					for (JsonElement tSlot : tRow.getAsJsonArray("fluidInputs")) {
						String tId = tSlot.getAsJsonObject().get("fluid").getAsString();
						if (!tFluids.contains(tId)) tMissing.add(tFile + " fluid " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered ids leaked into the chain rows: " + tMissing);
	}

	/** The 11-item disposition (acceptance ②): the three siblings registered, the eight BC circuits absent. */
	@Test
	public void theElevenGapItemsAreDispositioned() {
		Set<String> tPaths = gregtech6.registry.GT6CircuitChain.ITEMS_BY_PATH.keySet();
		assertTrue(tPaths.contains("circuit_magic"), "30311 Circuit (Magic) — MT.Magic is a port material: REGISTERED");
		assertTrue(tPaths.contains("circuit_enderium"), "30313 Circuit (Enderium) — MT.Enderium is a port material: REGISTERED");
		assertTrue(tPaths.contains("circuit_signalum"), "30315 Circuit (Signalum) — MT.Signalum is a port material: REGISTERED");
		for (String tBc : new String[] {"circuit_bc_redstone", "circuit_bc_iron", "circuit_bc_gold", "circuit_bc_diamond",
				"circuit_bc_ender", "circuit_bc_quartz", "circuit_bc_comparator", "circuit_bc_emerald"}) {
			assertFalse(tPaths.contains(tBc), tBc + " — MD.BC_SILICON content, no BuildCraft in the port: CUT");
		}
	}

	/**
	 * The lens non-consumable face: the upstream {@code ST.amount(0, lens)} idiom rides the
	 * {@link Recipe#sNotConsumable} lens disjunct — a lens stack is never consumed by a
	 * machine row, while the row's material legs stay consumable. Offline discipline: a
	 * mod-Item is not constructible in this JVM (the forge intrusive-holder wall), so the
	 * fixture rides the {@code GTMaterialItems.sLensTest} seam (the
	 * {@code GT6SlicerBlades.sBladeTest} shape) — the production default is the two-line
	 * prefix identity, and the RCON-free card proves the WIRING here.
	 */
	@Test
	public void theLensLegIsNeverConsumed() {
		java.util.function.Predicate<ItemStack> tDefault = gregtech6.registry.GTMaterialItems.sLensTest;
		try {
			gregtech6.registry.GTMaterialItems.sLensTest = aStack -> aStack != null && aStack.is(Items.EMERALD);
			assertTrue(Recipe.sNotConsumable.test(new ItemStack(Items.EMERALD)), "a lens rides the never-consumed face");
			assertFalse(Recipe.sNotConsumable.test(new ItemStack(Items.IRON_INGOT)), "non-lens stacks stay consumable");
			assertFalse(Recipe.sNotConsumable.test(ItemStack.EMPTY), "the empty stack is not a catalyst");
		} finally {
			gregtech6.registry.GTMaterialItems.sLensTest = tDefault; // restore (JUL test order is arbitrary)
		}
	}
}
