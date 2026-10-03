package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import gregtech6.registry.GT6Cells;
import gregtech6.registry.GT6ExtruderMolds;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Offline census for task small-tank-cell — the 40-row registration rendered through
 * the shared per-level model family. Pins the card end to end (the
 * GT6GasCylinderDatagenTest split):
 * <ul>
 * <li>the 40-row registration table — every row's VARYING columns verbatim against
 *     Loader_MultiTileEntities.java:1770-1809 (path, display, the aUtilWood/aUtilMetal
 *     host band, acid/magic/plasma/temperature recorded columns), plus the shared
 *     columns once (1000 L / GASPROOF T / stack 64);</li>
 * <li>the 8-PNG borrow tree (block/cell/, the 4+4 colored+overlay set — cell HAS an
 *     insides tile, the id1055 correction) 1:1 with the declared set and grounded in
 *     assets/README.md by name AND sha256;</li>
 * <li>the 40 generated blockstates are the 9 gt_liquid_level variants pointing at the
 *     ONE shared per-level model family;</li>
 * <li>the shared model pins the upstream grammar (the 6x12x6 shell over the colored
 *     band, the insides box, the 0.01 overlay shells, cutout) and the per-level fluid
 *     box (levels 1..8, the flat eighth-fraction heights, the smeltery_content
 *     placeholder — the declared ceiling);</li>
 * <li>the 40 BlockItem models parent the shell;</li>
 * <li>the CCC-mold defer conclusion: the mold registry carries NO CCC shape and the
 *     generated tree carries NO cell recipe — the acquisition row
 *     (Loader_Recipes_Handlers.java:766/:799) stays deferred to the mold card;</li>
 * <li>task cell-family-closeout: the zh names are the %s单元 form (the user ruling), the
 *     family owns the dedicated {@code cells} creative tab and no longer rides the
 *     storage tab.</li>
 * </ul>
 */
class GT6CellDatagenTest extends GTOfflineTestBase {

	@BeforeAll
	static void bootMaterials() {
		// the vanilla boot comes from the base; the gregapi material registry populates on
		// the mod lifecycle — the BoilerWall bootOfflineThenMaterials shape
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

	/** The borrow faces — colored + overlay over {sides, insides, top, bottom} (the :58-66 icon set, the 4+4 WITH insides). */
	private static final List<String> FACES = List.of("sides", "insides", "top", "bottom");
	private static final List<String> LAYERS = List.of("colored", "overlay");
	/** 2 layers x 4 faces — the walk upstream found exactly these files (the id1055 correction: cell HAS insides). */
	private static final int PINNED_PNG_TOTAL = 8;

	/** The per-row VARYING columns verbatim (Loader :1770-1809): {path, local-name, woodHost, acid, magic, plasma, temperature}. Gas/capacity/stack/hardness/resistance are the shared columns — pinned once below. */
	private static final String[][] LOADER_ROWS = {
			{"cell_wax", "Wax", "wood", "F", "F", "F", "MP-10"}, // :1770
			{"cell_bees_wax", "Bees Wax", "wood", "F", "F", "F", "MP-10"}, // :1771
			{"cell_plant_wax", "Plant Wax", "wood", "F", "F", "F", "MP-10"}, // :1772
			{"cell_paraffin_wax", "Paraffin Wax", "wood", "F", "F", "F", "MP-10"}, // :1773
			{"cell_refractory_wax", "Refractory Wax", "wood", "T", "F", "F", "MP-10"}, // :1774
			{"cell_magic_wax", "Magic Wax", "wood", "T", "T", "F", "2700"}, // :1775
			{"cell_amnesic_wax", "Amnesic Wax", "wood", "T", "T", "F", "2700"}, // :1776 (id 32624)
			{"cell_soulful_wax", "Soulful Wax", "wood", "T", "T", "F", "MP-10"}, // :1777 (id 32625)
			{"cell_plastic", "Plastic", "wood", "F", "F", "F", "MP-10"}, // :1778
			{"cell_tin", "Tin", "metal", "F", "F", "F", "MP-50"}, // :1779
			{"cell_tin_alloy", "Tin Alloy", "metal", "F", "F", "F", "MP-50"}, // :1780
			{"cell_invar", "Invar", "metal", "F", "F", "F", "MP-50"}, // :1781
			{"cell_gold", "Gold", "metal", "T", "F", "F", "MP-50"}, // :1782 (id 32635)
			{"cell_aluminium", "Aluminium", "metal", "F", "F", "F", "MP-50"}, // :1783
			{"cell_stainless_steel", "Stainless Steel", "metal", "T", "F", "F", "MP-50"}, // :1784
			{"cell_tungsten_alloy", "Tungsten Alloy", "metal", "F", "T", "F", "MP-50"}, // :1785 (id 32636)
			{"cell_titanium", "Titanium", "metal", "F", "F", "F", "MP-50"}, // :1786
			{"cell_netherite", "Netherite", "metal", "T", "T", "T", "MP-50"}, // :1787 (id 32634)
			{"cell_tungstensteel", "Tungstensteel", "metal", "F", "T", "F", "MP-50"}, // :1788
			{"cell_tungsten_carbide", "Tungsten Carbide", "metal", "F", "T", "F", "MP-50"}, // :1789
			{"cell_tungsten", "Tungsten", "metal", "T", "T", "F", "MP-50"}, // :1790
			{"cell_palladium", "Palladium", "metal", "F", "T", "F", "MP-50"}, // :1791 (id 32639)
			{"cell_tantalum_hafnium_carbide", "Tantalum Hafnium Carbide", "metal", "F", "T", "F", "MP-50"}, // :1792 (id 32633)
			{"cell_desh", "Desh", "metal", "F", "T", "F", "MP-50"}, // :1793 (id 32632)
			{"cell_workers_alloy", "Workers Alloy", "metal", "F", "T", "F", "MP-50"}, // :1794 (id 32638)
			{"cell_trinium", "Trinium", "metal", "F", "T", "F", "MP-50"}, // :1795 (id 32616, the MT.Ke field)
			{"cell_trinitanium", "Trinitanium", "metal", "F", "T", "T", "MP-50"}, // :1796
			{"cell_adamantium", "Adamantium", "metal", "T", "T", "T", "MP-50"}, // :1797 (id 32618, the MT.Ad field)
			{"cell_syrmorite", "Syrmorite", "metal", "F", "T", "F", "MP-50"}, // :1798 (id 32626)
			{"cell_efrine", "Efrine", "metal", "F", "T", "T", "MP-50"}, // :1799 (id 32637)
			{"cell_thaumium", "Thaumium", "metal", "T", "T", "F", "MP-50"}, // :1800 (id 32619)
			{"cell_void", "Void", "metal", "T", "T", "T", "MP-50"}, // :1801 (id 32620, the VoidMetal local)
			{"cell_manasteel", "Manasteel", "metal", "T", "T", "F", "MP-50"}, // :1802 (id 32627)
			{"cell_terrasteel", "Terrasteel", "metal", "T", "T", "F", "MP-50"}, // :1803 (id 32628)
			{"cell_elementium", "Elementium", "metal", "T", "T", "F", "MP-50"}, // :1804 (id 32629, the ElvenElementium local)
			{"cell_gaia_spirit", "Gaia Spirit", "metal", "T", "T", "T", "MP-50"}, // :1805 (id 32630)
			{"cell_duranium_alloy", "Duranium Alloy", "metal", "T", "T", "T", "MP-50"}, // :1806 (id 32621)
			{"cell_draconium", "Draconium", "metal", "T", "T", "T", "MP-50"}, // :1807 (id 32622)
			{"cell_awakened_draconium", "Awakened Draconium", "metal", "T", "T", "T", "MP-50"}, // :1808 (id 32623)
			{"cell_infinity", "Infinity", "metal", "T", "T", "T", "MAX"}, // :1809 (id 32631)
	};

	/** The temperature column enum mapping (the LOADER_ROWS tail → the GT6Cells.TemperatureNote). */
	private static GT6Cells.TemperatureNote temperatureNote(String aTail) {
		return switch (aTail) {
			case "MP-10" -> GT6Cells.TemperatureNote.MELTING_POINT_MINUS_10;
			case "MP-50" -> GT6Cells.TemperatureNote.MELTING_POINT_MINUS_50;
			case "2700" -> GT6Cells.TemperatureNote.FIXED_2700;
			case "MAX" -> GT6Cells.TemperatureNote.INFINITE;
			default -> throw new AssertionError("unknown temperature tail " + aTail);
		};
	}

	/** The mdk root (src/main/resources/assets/README.md), walked upward from the leg-dependent test working dir. */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("src/main/resources/assets/README.md"))) return p;
		}
		throw new AssertionError("mdk root (src/main/resources/assets/README.md) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	/** One committed generated-tree JSON as an object (the test classpath carries src/generated/resources). */
	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6CellDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	// ---------------------------------------------------------------------------
	// the 40-row registration census (the Loader :1770-1809 columns, row by row)
	// ---------------------------------------------------------------------------

	@Test
	void theRowTableIsTheFortyLoaderRows() {
		assertEquals(40, GT6Cells.ROWS.size(), "the :1770-1809 band, in registration order (ids 32600-32639)");
		assertEquals(LOADER_ROWS.length, GT6Cells.ROWS.size(), "the census table covers every row");
		Set<String> tSeenPaths = new HashSet<>();
		for (int tIndex = 0; tIndex < LOADER_ROWS.length; tIndex++) {
			GT6Cells.CellRow tRow = GT6Cells.ROWS.get(tIndex);
			String[] tExpected = LOADER_ROWS[tIndex];
			assertEquals(tExpected[0], tRow.path(), "row " + tIndex + ": the registration path");
			assertTrue(tSeenPaths.add(tRow.path()), "row " + tIndex + ": paths are unique");
			assertEquals("Capsule-Cell-Container (" + tExpected[1] + ")", tRow.displayName(),
					"row " + tIndex + ": the \"Capsule-Cell-Container (\" + aMat.getLocal() + \")\" name construction verbatim");
			assertEquals("wood".equals(tExpected[2]), tRow.woodHost(),
					tRow.path() + ": the aUtilWood/aUtilMetal host column");
			assertEquals("T".equals(tExpected[3]), tRow.acidProof(), tRow.path() + ": NBT_ACIDPROOF (recorded)");
			assertEquals("T".equals(tExpected[4]), tRow.magicProof(), tRow.path() + ": NBT_MAGICPROOF (recorded)");
			assertEquals("T".equals(tExpected[5]), tRow.plasmaProof(), tRow.path() + ": NBT_PLASMAPROOF (recorded)");
			assertEquals(temperatureNote(tExpected[6]), tRow.temperature(), tRow.path() + ": NBT_TEMPERATURE (recorded)");
			assertNotNull(tRow.material().get(), tRow.path() + ": the material column resolves");
		}
		assertEquals(40, tSeenPaths.size(), "40 distinct blocks");
	}

	/** The shared columns — every row carries the same 1000 L tank, GASPROOF T, and the material set is 40 distinct. */
	@Test
	void theSharedColumnsRunAcrossAllRows() {
		assertEquals(1000, gregtech6.tileentity.tank.GT6CellBlockEntity.CAPACITY, "NBT_TANK_CAPACITY 1000 on every row (the shared column)");
		assertEquals(64, gregtech6.tileentity.tank.GT6CellBlockEntity.STACK_SIZE, "the stack-64 registration column");
		assertEquals(40, GT6Cells.BLOCKS_IN_ORDER.size(), "one block per row");
		long tDistinctMaterials = GT6Cells.ROWS.stream().map(tRow -> tRow.material().get()).distinct().count();
		assertEquals(40, tDistinctMaterials, "40 distinct materials — the Loader band has no repeat");
		// the material column spot anchors (the first wax, the first metal, the noble-metal oddity, the ceiling)
		assertEquals(gregapi.data.MT.Wax, GT6Cells.ROWS.get(0).material().get(), ":1770 MT.Wax");
		assertEquals(gregapi.data.MT.Sn, GT6Cells.ROWS.get(9).material().get(), ":1779 MT.Sn — the first aUtilMetal row");
		assertEquals(gregapi.data.MT.W, GT6Cells.ROWS.get(20).material().get(), ":1790 MT.W");
		assertEquals(gregapi.data.MT.Infinity, GT6Cells.ROWS.get(39).material().get(), ":1809 MT.Infinity");
	}

	// ---------------------------------------------------------------------------
	// the PNG borrow census (positive, negative, attribution nail)
	// ---------------------------------------------------------------------------

	@Test
	void everyDeclaredFaceHasItsBorrowedPng() {
		for (String tLayer : LAYERS) {
			for (String tFace : FACES) {
				assertNotNull(GT6CellDatagenTest.class.getResource(
						"/assets/gt6/textures/block/cell/" + tLayer + "_" + tFace + ".png"),
						tLayer + "_" + tFace + ".png must be borrowed");
			}
		}
	}

	@Test
	void borrowedPngTreeIsExactlyTheDeclaredSet() throws Exception {
		var tUrl = GT6CellDatagenTest.class.getResource("/assets/gt6/textures/block/cell");
		assertNotNull(tUrl, "the cell borrow root must exist");
		Set<String> tFound = new HashSet<>();
		try (var tWalk = Files.walk(Path.of(tUrl.toURI()))) {
			tWalk.filter(p -> p.toString().endsWith(".png"))
					.forEach(p -> tFound.add(p.getFileName().toString()));
		}
		assertEquals(PINNED_PNG_TOTAL, tFound.size(), "2 layers x 4 faces borrowed PNGs, walked");
		Set<String> tDeclared = new HashSet<>();
		for (String tLayer : LAYERS) {
			for (String tFace : FACES) tDeclared.add(tLayer + "_" + tFace + ".png");
		}
		assertEquals(tDeclared, tFound, "the borrow is 1:1 with the declared tile set — zero strays, zero gaps (the 4+4 set WITH insides, the id1055 correction)");
	}

	/** The attribution nail (the p31 wave pattern): every borrowed PNG is named in assets/README.md AND its actual file bytes hash to a sha256 the ledger records. */
	@Test
	void borrowedPngsAreGroundedInTheAssetsLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
		List<String> tViolations = new ArrayList<>();
		try (var tWalk = Files.walk(Path.of(GT6CellDatagenTest.class
				.getResource("/assets/gt6/textures/block/cell").toURI()))) {
			List<Path> tPngs = tWalk.filter(p -> p.toString().endsWith(".png")).toList();
			for (Path tPng : tPngs) {
				String tName = tPng.getFileName().toString();
				String tHex = HexFormat.of().formatHex(tSha256.digest(Files.readAllBytes(tPng)));
				if (!tReadme.contains(tName)) tViolations.add(tName + " — filename absent from assets/README.md");
				if (!tReadme.contains(tHex)) tViolations.add(tName + " — bytes hash to " + tHex + ", not grounded in the ledger");
			}
		}
		assertTrue(tViolations.isEmpty(), "borrowed cell texture without attribution: " + tViolations);
	}

	// ---------------------------------------------------------------------------
	// the generated tree (blockstates, model geometry, item models)
	// ---------------------------------------------------------------------------

	@Test
	void generatedBlockstatesAreTheNineLevelVariantsOnTheSharedFamily() throws Exception {
		for (GT6Cells.CellRow tRow : GT6Cells.ROWS) {
			JsonObject tState = generatedJson("assets/gt6/blockstates/" + tRow.path() + ".json");
			assertTrue(tState.has("variants"), tRow.path() + ": the plain-variants form (no multipart)");
			var tVariants = tState.getAsJsonObject("variants");
			assertEquals(9, tVariants.size(), tRow.path() + ": the 9 gt_liquid_level variants (the crucible bowl form)");
			for (int tLevel = 0; tLevel <= 8; tLevel++) {
				JsonElement tVariant = tVariants.get("gt_liquid_level=" + tLevel);
				assertNotNull(tVariant, tRow.path() + ": the level " + tLevel + " variant exists");
				String tExpectedModel = tLevel == 0 ? "gt6:block/cell_container_empty"
						: "gt6:block/cell_container_filled_" + tLevel;
				assertEquals(tExpectedModel, tVariant.getAsJsonObject().get("model").getAsString(),
						tRow.path() + ": level " + tLevel + " points at the shared per-level family");
			}
		}
	}

	/**
	 * The shell geometry pin: 4 elements = the 6x12x6 colored shell + the 0.01 overlay
	 * shell + the insides box + its overlay shell (the borrow-grammar parity), cutout.
	 * Face bands: the shell tiles world-side (horizontal=sides, up=top, down=bottom),
	 * the insides box tiles insides all around.
	 */
	@Test
	void theSharedModelPinsTheShellGrammar() throws Exception {
		JsonObject tModel = generatedJson("assets/gt6/models/block/cell_container_empty.json");
		assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), "the two-layer cutout form (the sides window is genuinely transparent)");
		JsonArray tElements = tModel.getAsJsonArray("elements");
		assertEquals(4, tElements.size(), "the colored shell + its overlay + the insides box + its overlay");
		List<String> tSeen = new ArrayList<>();
		for (var tElement : tElements) {
			JsonObject tElem = tElement.getAsJsonObject();
			String tBox = tElem.getAsJsonArray("from") + "" + tElem.getAsJsonArray("to"); // the Gson compact form
			tSeen.add(tBox);
			String tEast = tElem.getAsJsonObject("faces").getAsJsonObject("east").get("texture").getAsString();
			switch (tBox) {
				case "[5,0,5][11,12,11]" -> {
					assertEquals("#sides", tEast, "the shell tiles the sides band on horizontals");
					assertEquals("#top", tElem.getAsJsonObject("faces").getAsJsonObject("up").get("texture").getAsString(), "the shell tiles top");
					assertEquals("#bottom", tElem.getAsJsonObject("faces").getAsJsonObject("down").get("texture").getAsString(), "the shell tiles bottom");
				}
				case "[4.99,-0.01,4.99][11.01,12.01,11.01]" -> assertEquals("#overlay_sides", tEast, "the 0.01 overlay shell rides the overlay band");
				case "[6,0,6][10,12,10]" -> assertEquals("#insides", tEast, "the insides box rides the insides band");
				case "[5.99,-0.01,5.99][10.01,12.01,10.01]" -> assertEquals("#overlay_insides", tEast, "the insides overlay shell rides the overlay band");
				default -> throw new AssertionError("unexpected shell box " + tBox);
			}
		}
		assertEquals(4, tSeen.size(), "four distinct boxes, no duplicates");
	}

	/**
	 * The per-level fluid box: levels 1..8 keep the shell parent (the texture map) but carry
	 * the FULL element set in the child itself — the four shell boxes + the one fluid box, the
	 * flat eighth-fraction heights, vertically inset off the shell planes. Vanilla
	 * {@code BlockModel.getElements} (1.20.1 BlockModel.java:104-105) never walks the parent
	 * chain once the child carries elements — the former parent + one-element "candle idiom"
	 * dropped the cell shell at render (the smeltery/cup filled family's same disease, the
	 * filled-shell-family-2 card). Cutout is self-carried too: render_type does not ride the
	 * parent chain either, and the shell's transparent overlay texels need the alpha-tested
	 * layer.
	 */
	@Test
	void theFilledModelsPinThePerLevelFluidBox() throws Exception {
		int tShellCount = generatedJson("assets/gt6/models/block/cell_container_empty.json")
				.getAsJsonArray("elements").size();
		for (int tLevel = 1; tLevel <= 8; tLevel++) {
			JsonObject tModel = generatedJson("assets/gt6/models/block/cell_container_filled_" + tLevel + ".json");
			assertEquals("gt6:block/cell_container_empty", tModel.get("parent").getAsString(), "level " + tLevel + ": the parent stays (the texture map)");
			assertEquals("minecraft:cutout", tModel.get("render_type") == null ? null : tModel.get("render_type").getAsString(),
					"level " + tLevel + ": cutout self-carried — the transparent overlay shell must bake alpha-tested");
			assertEquals(tShellCount + 1, tModel.getAsJsonArray("elements").size(),
					"level " + tLevel + ": the FULL shell + the one fluid box — vanilla getElements never merges the parent");
			// the game's own accessor — the exact parent-fallback decision point — must see
			// the full set offline (ponytail: the stand-in for the quad bake, which needs
			// the atlas + ModelBakery; the layer-level view stays field_test)
			var tVanilla = net.minecraft.client.renderer.block.model.BlockModel.fromString(tModel.toString());
			assertEquals(tShellCount + 1, tVanilla.getElements().size(),
					"level " + tLevel + ": vanilla BlockModel.getElements sees the shell+fluid set");
			float tTop = 0.05F + tLevel * 11.5F / 8.0F;
			JsonObject tFluid = null;
			for (var tElement : tModel.getAsJsonArray("elements")) {
				for (var tFace : tElement.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
					if ("#content".equals(tFace.getValue().getAsJsonObject().get("texture").getAsString())) {
						tFluid = tElement.getAsJsonObject();
					}
				}
			}
			assertNotNull(tFluid, "level " + tLevel + ": the fluid box element");
			float tMinY = tFluid.getAsJsonArray("from").get(1).getAsFloat();
			assertEquals(0.05F, tMinY, 1e-4, "level " + tLevel + ": the 0.05px inset off the shell bottom plane (no z-fight)");
			assertEquals(tTop, tFluid.getAsJsonArray("to").get(1).getAsFloat(), 1e-4, "level " + tLevel + ": the flat eighth-fraction of the 12px interior, inset off the top plane");
			assertEquals(5.5F, tFluid.getAsJsonArray("from").get(0).getAsFloat(), 1e-4, "level " + tLevel + ": the fluid box sits between the shell wall and the insides box");
			for (var tFaceEntry : tFluid.getAsJsonObject("faces").entrySet()) {
				assertEquals("#content", tFaceEntry.getValue().getAsJsonObject().get("texture").getAsString(),
						"level " + tLevel + ": every face rides the smeltery_content placeholder (the declared ceiling)");
				assertFalse(tFaceEntry.getValue().getAsJsonObject().has("tintindex"),
						"level " + tLevel + " face " + tFaceEntry.getKey() + ": the fluid box never tints (the BlockTextureFluid form)");
			}
		}
	}

	@Test
	void generatedItemModelsParentTheShell() throws Exception {
		for (GT6Cells.CellRow tRow : GT6Cells.ROWS) {
			JsonObject tItem = generatedJson("assets/gt6/models/item/" + tRow.path() + ".json");
			assertEquals("gt6:block/cell_container_empty", tItem.get("parent").getAsString(),
					tRow.path() + ": the item parents the shared shell model");
		}
	}

	// ---------------------------------------------------------------------------
	// the CCC-mold defer conclusion (the card's 前置核查, pinned)
	// ---------------------------------------------------------------------------

	/**
	 * The 前置核查 verdict, updated by task mold-extruder-shapes: the CCC extruder mold
	 * NOW EXISTS (the full 64-mold family registered — Shape_Extruder_CCC meta 10028 /
	 * Shape_SimpleEx_CCC meta 10228) — but the acquisition row itself (upstream
	 * Loader_Recipes_Handlers.java:766 Shape_Extruder_CCC / :799 Shape_SimpleEx_CCC,
	 * output OP.capcellcon) stays DEFERRED to the extruder-row card (b5): the generated
	 * tree carries zero cell recipe/ files, no substitute recipe fabricated.
	 */
	@Test
	void theCccMoldExistsAndTheAcquisitionRowStaysDeferred() throws Exception {
		Set<String> tMoldPaths = new HashSet<>();
		for (var tMold : GT6ExtruderMolds.MOLDS) tMoldPaths.add(tMold.getId().getPath());
		assertTrue(tMoldPaths.containsAll(Set.of("shape_extruder_ccc", "shape_simple_ex_ccc")),
				"the CCC twin molds exist (task mold-extruder-shapes) — the recipe face below is the remaining defer");
		var tRecipesRoot = GT6CellDatagenTest.class.getResource("/data/gt6/recipes");
		assertNotNull(tRecipesRoot, "the generated recipe tree exists");
		try (var tWalk = Files.walk(Path.of(tRecipesRoot.toURI()))) {
			long tCellRecipes = tWalk.filter(p -> p.getFileName().toString().startsWith("cell_")).count();
			assertEquals(0, tCellRecipes, "no cell recipe is generated — the acquisition row is the declared defer, not a fabricated substitute");
		}
	}

	// ---------------------------------------------------------------------------
	// task cell-family-closeout — the zh 正名 pin + the dedicated-creative-tab pin
	// ---------------------------------------------------------------------------

	/**
	 * The zh display names carry the IC2-style "_material_ + 单元" form (the user ruling
	 * 2026-10-03: 蜡单元/金单元) — NOT the bare material the small-tank-cell backfill left
	 * behind ("block.gt6.cell_aluminium"="铝"). The upstream zh dump face is
	 * "单元式流体容器 (蜡)" (tmp/gregtech.lang:13481-13520); the port ruling replaces that
	 * wrapper with the suffix form — the declared deviation.
	 */
	@Test
	void zhNamesFollowTheUnitSuffixForm() throws Exception {
		JsonObject tZh = generatedJson("assets/gt6/lang/zh_cn.json");
		for (GT6Cells.CellRow tRow : GT6Cells.ROWS) {
			assertTrue(tZh.has("block.gt6." + tRow.path()), tRow.path() + ": the zh key exists");
			String tValue = tZh.get("block.gt6." + tRow.path()).getAsString();
			assertTrue(tValue.endsWith("单元") && tValue.length() > 2,
					tRow.path() + ": the zh name must be the %s单元 form, got \"" + tValue + "\"");
		}
		// the user's own examples — 蜡单元 / 金单元 / 铝单元
		assertEquals("蜡单元", tZh.get("block.gt6.cell_wax").getAsString());
		assertEquals("金单元", tZh.get("block.gt6.cell_gold").getAsString());
		assertEquals("铝单元", tZh.get("block.gt6.cell_aluminium").getAsString());
	}

	/**
	 * The storage-tab ride is gone (the user ruling 2026-10-03: the cells get their own
	 * creative page, the test-tube-prefix-tab treatment) — the family must not append
	 * itself into the GTBarrels "Fluid Containers" (zh 储罐) tab anymore. Structural pin:
	 * no GT6Cells method listens on BuildCreativeModeTabContentsEvent — that ride was the
	 * only pollution path (GTBarrels' own displayItems never listed the cells).
	 */
	@Test
	void theFamilyNoLongerRidesTheStorageTab() {
		for (var tMethod : GT6Cells.class.getDeclaredMethods()) {
			for (var tParam : tMethod.getParameterTypes()) {
				assertTrue(tParam != BuildCreativeModeTabContentsEvent.class,
						"GT6Cells." + tMethod.getName() + " still rides the storage tab via BuildCreativeModeTabContentsEvent");
			}
		}
	}

	/**
	 * The dedicated-tab positive face (the offline share of the GT6Tools tab posture — the
	 * live face rides the runServer registration-log gate): the family-owned tab exists
	 * under the {@code cells} id and the icon row is the FIRST registration (the upstream
	 * lazy-tab face, Wax :1770). The walk feeding it is {@code displayCells over
	 * BLOCKS_IN_ORDER}, whose full-40 registration-order content is the rowTable census.
	 */
	@Test
	void theFamilyOwnsTheDedicatedCellsTab() {
		assertEquals("cells", GT6Cells.CELLS_TAB.getId().getPath(), "the dedicated tab id");
		assertEquals("cell_wax", GT6Cells.BLOCKS_IN_ORDER.get(0).getId().getPath(),
				"the icon row is the first registration (the upstream lazy-tab icon face)");
	}
}
