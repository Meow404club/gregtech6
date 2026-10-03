/**
 * The wood-button-belt data-face test (task toolhead-r11d-wood-button-belt): the upstream
 * {@code Loader_Recipes_Woods.java:137-152} "Generic Wooden Stuff made mostly from Buttons"
 * band — 6 misc rows (:137-142 gear/gearSmall/casing/plateTiny/ring/round) + 10 tool-head
 * rows (:143-152 the finished Wood hammer + the nine raw heads) — ported as vanilla
 * crafting_shaped JSON (the r9-39 head-row shape: the head item IS the identity).
 *
 * <p>Letter faces (CR.java:339-360): {@code 's'} = tools/saw (the CR.java:356 letter),
 * {@code 'r'} = tools/soft_hammer (:355 softhammer), {@code 'v'} = the sawaxe union —
 * upstream the axe+saw re-registration alias (LoaderOreDictReRegistrations.java:645-646),
 * ported as the combined Ingredient over {@code #gt6:tools/axe} + {@code #gt6:tools/saw}
 * (the main-session ruling; both port tags in register, no new tag needed), {@code 'g'} =
 * tools/hand_drill (:345 handdrill), {@code 'k'} = tools/knife, {@code 'f'} = tools/file.
 * {@code 'P'} = {@code OD.buttonWood} → {@code #minecraft:wooden_buttons} (the 1.7.10
 * {@code minecraft:wooden_button} id does not exist on this generation — the vanilla tag
 * over the eleven wooden buttons is the faithful face) except the :137 gear row where 'P'
 * = {@code OD.plankAnyWood} → {@code #minecraft:planks} (the gt6 planks join it,
 * GT6ItemTags.addTreeTags) and the buttons ride 'B'.
 *
 * <p>The census rows are read verbatim off the classpath (the GT6StoneSlabCraftingJsonTest
 * convention); the walk truth is the offline-pure {@code OreDictPrefix.isGeneratingItem}
 * predicate (the r9-39 ruling — the headless JVM reproduces the registration gate).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6WoodButtonCraftingJsonTest extends GTOfflineTestBase {

	/** The 16-row band: id leaf, upstream line, output count, pattern verbatim (Woods:137-152). */
	static record BeltRow(String aId, int aLine, int aCount, String[] aPattern) {
	}

	/** The 6 misc rows (:137-142) then the 10 head rows (:143-152) — upstream order. */
	static final List<BeltRow> BELT_ROWS = List.of(
			new BeltRow("gear_gt", 137, 1, new String[] {"BPB", "PsP", "BPB"}),
			new BeltRow("gear_gt_small", 138, 1, new String[] {"P ", " s"}),
			new BeltRow("casing_small", 139, 2, new String[] {" P", "s "}),
			new BeltRow("plate_tiny", 140, 9, new String[] {"s ", " P"}),
			new BeltRow("ring", 141, 4, new String[] {"P ", " k"}),
			new BeltRow("round", 142, 9, new String[] {"P ", "fk"}),
			new BeltRow("tool_head_hammer", 143, 1, new String[] {"PP ", "PPg", "PPv"}),
			new BeltRow("tool_head_raw_arrow", 144, 4, new String[] {"  P", "r v"}),
			new BeltRow("tool_head_raw_sword", 145, 1, new String[] {" P ", "rPv"}),
			new BeltRow("tool_head_raw_pickaxe", 146, 1, new String[] {"PPP", "rgv"}),
			new BeltRow("tool_head_raw_shovel", 147, 1, new String[] {"rPv"}),
			new BeltRow("tool_head_raw_spade", 148, 1, new String[] {" P ", "r v"}),
			new BeltRow("tool_head_raw_axe", 149, 1, new String[] {" PP", "rPv"}),
			new BeltRow("tool_head_raw_hoe", 150, 1, new String[] {" PP", "r v"}),
			new BeltRow("tool_head_raw_sense", 151, 1, new String[] {"PPP", "   ", "r v"}),
			new BeltRow("tool_head_raw_plow", 152, 1, new String[] {"PPP", "PPP", "r v"}));

	/** The row's output item — the GTMaterialItems id composition (snake(prefix) + "_wood"). */
	static String resultItem(BeltRow aRow) {
		return "gt6:" + aRow.aId() + "_wood";
	}

	/** The recipe id — {@code <form>/wood} (the head-row <form>/<snake> convention, free leaves). */
	static String rowId(BeltRow aRow) {
		return aRow.aId() + "/wood";
	}

	@BeforeAll
	static void boot() {
		GTMaterialItems.initMaterials();
	}

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/" + aPath + ".json";
		try (InputStream tStream = GT6WoodButtonCraftingJsonTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	/** Location of the mdk project root (the GT6StoneSlabCraftingJsonTest.mdkRoot shape). */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) return p;
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	// ------------------------------------------------- the walk truth (offline-pure)

	/**
	 * Every belt row's output item is inside the registration gate — the offline-pure
	 * {@code isGeneratingItem} face (PrefixItem.java:104). The wooden button belt is
	 * upstream's ONLY wood route for these 16 outputs (Woods:137-152), so a miss here
	 * means the row cannot exist at all.
	 */
	@Test
	public void theBeltOutputsAreInsideTheRegistrationGate() {
		Map<String, OreDictPrefix> tPrefixes = Map.ofEntries(
				Map.entry("gear_gt", OP.gearGt),
				Map.entry("gear_gt_small", OP.gearGtSmall),
				Map.entry("casing_small", OP.casingSmall),
				Map.entry("plate_tiny", OP.plateTiny),
				Map.entry("ring", OP.ring),
				Map.entry("round", OP.round),
				Map.entry("tool_head_hammer", OP.toolHeadHammer),
				Map.entry("tool_head_raw_arrow", OP.toolHeadRawArrow),
				Map.entry("tool_head_raw_sword", OP.toolHeadRawSword),
				Map.entry("tool_head_raw_pickaxe", OP.toolHeadRawPickaxe),
				Map.entry("tool_head_raw_shovel", OP.toolHeadRawShovel),
				Map.entry("tool_head_raw_spade", OP.toolHeadRawSpade),
				Map.entry("tool_head_raw_axe", OP.toolHeadRawAxe),
				Map.entry("tool_head_raw_hoe", OP.toolHeadRawHoe),
				Map.entry("tool_head_raw_sense", OP.toolHeadRawSense),
				Map.entry("tool_head_raw_plow", OP.toolHeadRawPlow));
		for (BeltRow tRow : BELT_ROWS) {
			assertTrue(tPrefixes.get(tRow.aId()).isGeneratingItem(MT.Wood),
					"the " + tRow.aId() + " prefix generates for MT.Wood (the " + tRow.aLine() + " row's output face)");
		}
	}

	// ------------------------------------------------- the census (disk face)

	/**
	 * The 16-row census: every belt leaf {@code <form>/wood} exists on the plural face.
	 * (Other {@code /wood.json} leaves belong to sibling bands — the from_head rows, the
	 * arrows, bolt2screw, stick_long2stick — and are not the belt's business.)
	 */
	@Test
	public void theBeltCensusIsPinned() throws IOException {
		Path tRecipes = mdkRoot().resolve("src").resolve("generated").resolve("resources")
				.resolve("data").resolve("gt6").resolve("recipes");
		assertTrue(Files.isDirectory(tRecipes), "the generated recipes tree exists");
		Set<String> tSeen = new HashSet<>();
		try (Stream<Path> tWalk = Files.walk(tRecipes, 2)) {
			tWalk.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
				String tRel = tRecipes.relativize(p).toString().replace('\\', '/');
				if (tRel.endsWith("/wood.json")) tSeen.add(tRel.substring(0, tRel.length() - ".json".length()));
			});
		}
		for (BeltRow tRow : BELT_ROWS) {
			assertTrue(tSeen.contains(rowId(tRow)), "the belt row " + rowId(tRow) + " is on disk");
		}
		assertEquals(16, BELT_ROWS.size(), "the belt is the 16 upstream rows (10 heads + 6 misc)");
	}

	/** The 1.21 singular mirror face carries the same 16 rows (the GT6DualDirectoryFaces walk). */
	@Test
	public void theSingularMirrorFaceCarriesTheBelt() throws Exception {
		for (BeltRow tRow : BELT_ROWS) {
			assertNotNull(generated("recipe/" + rowId(tRow)), "the mirrored singular face of " + rowId(tRow));
		}
	}

	// ------------------------------------------------- the verbatim faces

	/** Every row: type, category, pattern, result item + count verbatim (Woods:137-152). */
	@Test
	public void everyRowIsTheUpstreamTranscription() throws Exception {
		for (BeltRow tRow : BELT_ROWS) {
			JsonObject tJson = generated("recipes/" + rowId(tRow));
			assertEquals("minecraft:crafting_shaped", tJson.get("type").getAsString(), rowId(tRow) + ": the vanilla shaped type");
			List<String> tPattern = tJson.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList();
			assertEquals(List.of(tRow.aPattern()), tPattern, rowId(tRow) + " (Woods:" + tRow.aLine() + ") pattern verbatim");
			JsonObject tResult = tJson.getAsJsonObject("result");
			assertEquals(resultItem(tRow), tResult.get("item").getAsString(), rowId(tRow) + ": the wood output item");
			assertEquals(tRow.aCount(), tResult.has("count") ? tResult.get("count").getAsInt() : 1,
					rowId(tRow) + ": the output count");
		}
	}

	/** The :137 gear row — 'P' = the plankAnyWood tag face, 'B' = the wooden button tag, 's' = the saw tag. */
	@Test
	public void theGearRowIsThe137Transcription() throws Exception {
		JsonObject tRow = generated("recipes/" + rowId(BELT_ROWS.get(0)));
		JsonObject tKey = tRow.getAsJsonObject("key");
		assertEquals("minecraft:planks", tKey.getAsJsonObject("P").get("tag").getAsString(),
				"'P' = OD.plankAnyWood → #minecraft:planks (the gt6 planks join it)");
		assertEquals("minecraft:wooden_buttons", tKey.getAsJsonObject("B").get("tag").getAsString(),
				"'B' = OD.buttonWood → #minecraft:wooden_buttons (the modern wooden-button face)");
		assertEquals("gt6:tools/saw", tKey.getAsJsonObject("s").get("tag").getAsString(),
				"'s' = the CR.java:356 SAW letter → the #gt6:tools/saw tag");
	}

	/** The button-key rows — every 'P' but the gear row is the wooden-buttons tag (the OD.buttonWood face). */
	@Test
	public void theButtonRowsKeyWoodenButtons() throws Exception {
		for (BeltRow tRow : BELT_ROWS.subList(1, BELT_ROWS.size())) {
			JsonObject tKey = generated("recipes/" + rowId(tRow)).getAsJsonObject("key");
			assertEquals("minecraft:wooden_buttons", tKey.getAsJsonObject("P").get("tag").getAsString(),
					rowId(tRow) + ": 'P' = OD.buttonWood → #minecraft:wooden_buttons");
		}
	}

	/**
	 * The 'v' sawaxe slot — the combined Ingredient over BOTH tags (the main-session ruling;
	 * upstream = the axe+saw re-registration alias, LoaderOreDictReRegistrations.java:645-646).
	 * The nine rows carrying 'v': the hammer (:143) + the eight raw heads :144-145/:147-152.
	 */
	@Test
	public void theSawaxeSlotIsTheAxePlusSawUnion() throws Exception {
		List<String> tSawaxeRows = List.of("tool_head_hammer", "tool_head_raw_arrow", "tool_head_raw_sword",
				"tool_head_raw_shovel", "tool_head_raw_spade", "tool_head_raw_axe", "tool_head_raw_hoe",
				"tool_head_raw_sense", "tool_head_raw_plow");
		for (String tForm : tSawaxeRows) {
			JsonElement tV = generated("recipes/" + tForm + "/wood").getAsJsonObject("key").get("v");
			assertNotNull(tV, tForm + ": the 'v' slot exists");
			JsonArray tUnion = tV.getAsJsonArray();
			assertEquals(2, tUnion.size(), tForm + ": the sawaxe union is the axe+saw pair");
			Set<String> tTags = new HashSet<>();
			for (JsonElement tFace : tUnion) tTags.add(tFace.getAsJsonObject().get("tag").getAsString());
			assertEquals(Set.of("gt6:tools/axe", "gt6:tools/saw"), tTags,
					tForm + ": BOTH the axe and the saw faces match (the union proof)");
		}
	}

	/** The 'g' handdrill slot — the hammer (:143) and pickaxe (:146) rows key the #gt6:tools/hand_drill tag. */
	@Test
	public void theHanddrillSlotIsTheHandDrillTag() throws Exception {
		for (String tForm : List.of("tool_head_hammer", "tool_head_raw_pickaxe")) {
			JsonObject tKey = generated("recipes/" + tForm + "/wood").getAsJsonObject("key");
			assertEquals("gt6:tools/hand_drill", tKey.getAsJsonObject("g").get("tag").getAsString(),
					tForm + ": 'g' = the CR.java:345 handdrill letter → the #gt6:tools/hand_drill tag");
		}
	}

	/** The 'r' softhammer slot — the nine raw-head rows key the #gt6:tools/soft_hammer tag. */
	@Test
	public void theSofthammerSlotIsTheSoftHammerTag() throws Exception {
		for (String tForm : List.of("tool_head_raw_arrow", "tool_head_raw_sword", "tool_head_raw_pickaxe",
				"tool_head_raw_shovel", "tool_head_raw_spade", "tool_head_raw_axe", "tool_head_raw_hoe",
				"tool_head_raw_sense", "tool_head_raw_plow")) {
			JsonObject tKey = generated("recipes/" + tForm + "/wood").getAsJsonObject("key");
			assertEquals("gt6:tools/soft_hammer", tKey.getAsJsonObject("r").get("tag").getAsString(),
					tForm + ": 'r' = the CR.java:355 softhammer letter → the #gt6:tools/soft_hammer tag");
		}
	}

	/** The knife/file slots — the ring row 'k' (:141) and the round row 'f'+'k' (:142). */
	@Test
	public void theKnifeAndFileSlotsAreTheirTags() throws Exception {
		JsonObject tRing = generated("recipes/ring/wood").getAsJsonObject("key");
		assertEquals("gt6:tools/knife", tRing.getAsJsonObject("k").get("tag").getAsString(),
				"ring :141: 'k' = the CR.java:349 knife letter");
		JsonObject tRound = generated("recipes/round/wood").getAsJsonObject("key");
		assertEquals("gt6:tools/file", tRound.getAsJsonObject("f").get("tag").getAsString(),
				"round :142: 'f' = the CR.java:344 file letter");
		assertEquals("gt6:tools/knife", tRound.getAsJsonObject("k").get("tag").getAsString(),
				"round :142: 'k' = the knife letter");
	}
}
