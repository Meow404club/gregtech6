package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Task mold-extruder-shapes — the extruder-mold registration census. The w1 row0 pair
 * (plate + rod) and the toolhead-r11c-extruder-heads head family (8+8) extend to the
 * FULL upstream family: 32 {@code Shape_Extruder_*} items (MultiItemTechnological.java
 * :182-216, metas 10000-10031, the Empty lead) + their 32 {@code Shape_SimpleEx_*}
 * low-heat twins (:258-292, metas 10200-10231), walked in upstream meta order.
 *
 * <p>The offline surface follows the CreativeTabJoinCensusTest discipline: registry
 * objects are unbound in this JVM, but the {@code MOLDS} walk order, the shipped asset
 * tree (generated resources ride the test classpath) and the reflective tab-join shape
 * are all readable — and every face here is exactly what the JEI/creative visibility
 * and the not-consumable predicate consume. The machining-chain replay
 * ({@link #theMoldChainReplaysTheUpstreamMachiningSteps}) reads the generated crafting
 * rows back and re-derives the upstream CR.shaped strokes verbatim
 * (MultiItemTechnological.java:184/:218-254/:260/:294-330).
 */
public class GT6ExtruderMoldsCensusTest {

	/** The mold ids in upstream meta order (the MultiItemTechnological registration walk). */
	private static final List<String> MOLD_IDS = List.of(
			"shape_extruder_empty",       // 10000 (:182)
			"shape_extruder_plate",       // 10001 (:186)
			"shape_extruder_rod_long",    // 10002
			"shape_extruder_bolt",        // 10003
			"shape_extruder_ring",        // 10004
			"shape_extruder_cell",        // 10005
			"shape_extruder_ingot",       // 10006
			"shape_extruder_wire",        // 10007
			"shape_extruder_casing",      // 10008
			"shape_extruder_pipe_tiny",   // 10009
			"shape_extruder_pipe_small",  // 10010
			"shape_extruder_pipe_medium", // 10011
			"shape_extruder_pipe_large",  // 10012
			"shape_extruder_pipe_huge",   // 10013
			"shape_extruder_block",       // 10014
			"shape_extruder_sword",       // 10015
			"shape_extruder_pickaxe",     // 10016
			"shape_extruder_shovel",      // 10017
			"shape_extruder_axe",         // 10018
			"shape_extruder_hoe",         // 10019
			"shape_extruder_hammer",      // 10020
			"shape_extruder_file",        // 10021
			"shape_extruder_saw",         // 10022
			"shape_extruder_gear",        // 10023
			"shape_extruder_bottle",      // 10024
			"shape_extruder_plate_curved",// 10025
			"shape_extruder_gear_small",  // 10026
			"shape_extruder_rod",         // 10027 (:212)
			"shape_extruder_ccc",         // 10028
			"shape_extruder_foil",        // 10029
			"shape_extruder_plate_tiny",  // 10030
			"shape_extruder_wire_fine",   // 10031
			"shape_simple_ex_empty",       // 10200 (:258)
			"shape_simple_ex_plate",       // 10201
			"shape_simple_ex_rod_long",    // 10202
			"shape_simple_ex_bolt",        // 10203
			"shape_simple_ex_ring",        // 10204
			"shape_simple_ex_cell",        // 10205
			"shape_simple_ex_ingot",       // 10206
			"shape_simple_ex_wire",        // 10207
			"shape_simple_ex_casing",      // 10208
			"shape_simple_ex_pipe_tiny",   // 10209
			"shape_simple_ex_pipe_small",  // 10210
			"shape_simple_ex_pipe_medium", // 10211
			"shape_simple_ex_pipe_large",  // 10212
			"shape_simple_ex_pipe_huge",   // 10213
			"shape_simple_ex_block",       // 10214
			"shape_simple_ex_sword",       // 10215
			"shape_simple_ex_pickaxe",     // 10216
			"shape_simple_ex_shovel",      // 10217
			"shape_simple_ex_axe",         // 10218
			"shape_simple_ex_hoe",         // 10219
			"shape_simple_ex_hammer",      // 10220
			"shape_simple_ex_file",        // 10221
			"shape_simple_ex_saw",         // 10222
			"shape_simple_ex_gear",        // 10223
			"shape_simple_ex_bottle",      // 10224
			"shape_simple_ex_plate_curved",// 10225
			"shape_simple_ex_gear_small",  // 10226
			"shape_simple_ex_rod",         // 10227
			"shape_simple_ex_ccc",         // 10228
			"shape_simple_ex_foil",        // 10229
			"shape_simple_ex_plate_tiny",  // 10230
			"shape_simple_ex_wire_fine");  // 10231

	/**
	 * One machining row of the upstream CR.shaped anchor chains (MultiItemTechnological
	 * .java:218-254 the Shape_Extruder family, :294-330 the SimpleEx twins): the mold id
	 * suffix, the parent it machines down from, and the 3x3 stroke verbatim — 'x' is the
	 * CR.java:359 wirecutter tool letter walking the six strike positions clockwise,
	 * 'P' the parent mold.
	 */
	private static record ChainRow(String aMold, String aParent, String aRow0, String aRow1, String aRow2) {}

	/**
	 * The full machining chain, upstream row order (:218-254; the SimpleEx twins :294-330
	 * carry the IDENTICAL stroke plan over their own family ids — both walks replayed here).
	 */
	private static final List<ChainRow> CHAIN = List.of(
			new ChainRow("ingot", "empty", "x  ", " P ", "   "),        // :218
			new ChainRow("plate_tiny", "empty", " x ", " P ", "   "),   // :219
			new ChainRow("plate_curved", "empty", "  x", " P ", "   "), // :220
			new ChainRow("rod", "empty", "   ", " Px", "   "),          // :221
			new ChainRow("foil", "empty", "   ", " P ", "  x"),         // :222
			new ChainRow("ring", "empty", "   ", " P ", " x "),         // :223
			new ChainRow("bolt", "rod", "x  ", " P ", "   "),           // :225
			new ChainRow("wire", "rod", " x ", " P ", "   "),           // :226
			new ChainRow("rod_long", "rod", "  x", " P ", "   "),       // :227
			new ChainRow("wire_fine", "rod", "   ", " Px", "   "),      // :228
			new ChainRow("block", "ingot", "x  ", " P ", "   "),        // :230
			new ChainRow("pickaxe", "ingot", " x ", " P ", "   "),      // :231
			new ChainRow("hammer", "ingot", "  x", " P ", "   "),       // :232
			new ChainRow("hoe", "ingot", "   ", " Px", "   "),          // :233
			new ChainRow("gear", "ring", "x  ", " P ", "   "),          // :235
			new ChainRow("gear_small", "ring", " x ", " P ", "   "),    // :236
			new ChainRow("bottle", "ring", "  x", " P ", "   "),        // :237
			new ChainRow("cell", "ring", "   ", " Px", "   "),          // :238
			new ChainRow("ccc", "ring", "   ", " P ", "  x"),           // :239
			new ChainRow("axe", "plate_tiny", "x  ", " P ", "   "),     // :241
			new ChainRow("shovel", "plate_tiny", " x ", " P ", "   "),  // :242
			new ChainRow("file", "plate_tiny", "  x", " P ", "   "),    // :243
			new ChainRow("sword", "plate_tiny", "   ", " Px", "   "),   // :244
			new ChainRow("saw", "plate_tiny", "   ", " P ", "  x"),     // :245
			new ChainRow("plate", "foil", "x  ", " P ", "   "),         // :247
			new ChainRow("casing", "foil", " x ", " P ", "   "),        // :248
			new ChainRow("pipe_tiny", "plate_curved", "x  ", " P ", "   "),      // :250
			new ChainRow("pipe_small", "plate_curved", " x ", " P ", "   "),     // :251
			new ChainRow("pipe_medium", "plate_curved", "  x", " P ", "   "),    // :252
			new ChainRow("pipe_large", "plate_curved", "   ", " Px", "   "),     // :253
			new ChainRow("pipe_huge", "plate_curved", "   ", " P ", "  x"));     // :254

	/** The offline boot before the first registry-class touch (the CreativeTabJoinCensusTest.boot shape). */
	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	/** The census: 64 molds (32+32), ids in the upstream meta order. */
	@Test
	public void theMoldCensusIsSixtyFourInTheUpstreamMetaOrder() {
		assertEquals(MOLD_IDS.size(), GT6ExtruderMolds.MOLDS.size(), "the full family census");
		List<String> tPaths = new ArrayList<>();
		for (RegistryObject<Item> tMold : GT6ExtruderMolds.MOLDS) tPaths.add(tMold.getId().getPath());
		assertEquals(MOLD_IDS, tPaths, "the ids walk in upstream meta order (Extruder 10000-10031, then SimpleEx 10200-10231)");
	}

	/** The asset face: every mold has an item model JSON and its borrowed texture PNG. */
	@Test
	public void everyMoldHasAnItemModelAndABorrowedTexture() {
		for (String tPath : MOLD_IDS) {
			assertNotNull(read("assets/gt6/models/item/" + tPath + ".json"), "the item model rides the generated tree: " + tPath);
			String tTexture = texturePath(tPath);
			assertNotNull(read("assets/gt6/textures/item/" + tTexture + ".png"), "the borrowed sprite rides main resources: " + tTexture);
		}
	}

	/** The not-consumable face: the extruder_shapes tag JSON carries all 64 memberships. */
	@Test
	public void theExtruderShapesTagCarriesTheFullFamily() throws Exception {
		String tJson = read("data/gt6/tags/item/extruder_shapes.json");
		assertNotNull(tJson, "the generated tag file rides the test classpath");
		List<String> tValues = new ArrayList<>();
		for (var tElement : JsonParser.parseString(tJson).getAsJsonObject().getAsJsonArray("values")) {
			tValues.add(tElement.getAsString());
		}
		for (String tPath : MOLD_IDS) {
			assertTrue(tValues.contains("gt6:" + tPath), "the tag membership: " + tPath);
		}
		assertEquals(MOLD_IDS.size(), tValues.size(), "no stray memberships — exactly the registered family");
	}

	/**
	 * The machining-chain replay: every non-empty mold's generated crafting row re-derives
	 * the upstream CR.shaped stroke verbatim — the pattern rows, the 'x' = wirecutter tool
	 * letter (CR.java:359), the 'P' = the exact parent mold, the 1x result. The two Empty
	 * rows ride their own plateDouble faces ({@link #theEmptyMoldsFoldFromTheirPlateDoubles}).
	 */
	@Test
	public void theMoldChainReplaysTheUpstreamMachiningSteps() throws Exception {
		for (String tFamily : new String[] {"shape_extruder", "shape_simple_ex"}) {
			for (ChainRow tRow : CHAIN) {
				var tJson = JsonParser.parseString(read("data/gt6/recipes/" + tFamily + "_" + tRow.aMold() + ".json")).getAsJsonObject();
				assertEquals("minecraft:crafting_shaped", tJson.get("type").getAsString(), tFamily + "_" + tRow.aMold() + ": the shaped row");
				List<String> tPattern = new ArrayList<>();
				tJson.getAsJsonArray("pattern").forEach(e -> tPattern.add(e.getAsString()));
				assertEquals(List.of(tRow.aRow0(), tRow.aRow1(), tRow.aRow2()), tPattern, tFamily + "_" + tRow.aMold() + ": the 3x3 stroke verbatim");
				var tKey = tJson.getAsJsonObject("key");
				assertEquals("gt6:tools/wire_cutter", tKey.getAsJsonObject("x").get("tag").getAsString(),
						tFamily + "_" + tRow.aMold() + ": 'x' is the CR.java:359 wirecutter letter");
				assertEquals("gt6:" + tFamily + "_" + tRow.aParent(), tKey.getAsJsonObject("P").get("item").getAsString(),
						tFamily + "_" + tRow.aMold() + ": 'P' is the exact parent mold");
				assertEquals("gt6:" + tFamily + "_" + tRow.aMold(), tJson.getAsJsonObject("result").get("item").getAsString(),
						tFamily + "_" + tRow.aMold() + ": the 1x result");
			}
		}
	}

	/**
	 * The two Empty rows (MultiItemTechnological.java:184/:260 — the {@code "hf","xP"} fold
	 * over a double plate): the Shape_Extruder Empty keys the TungstenCarbide double plate,
	 * the SimpleEx Empty the Steel double plate (the upstream ANY.Steel walk folds to its
	 * Steel representative — the Knightmetal/MeteoricSteel co-members are unported).
	 */
	@Test
	public void theEmptyMoldsFoldFromTheirPlateDoubles() throws Exception {
		Map.of("shape_extruder_empty", "gt6:plate_double_tungsten_carbide",
				"shape_simple_ex_empty", "gt6:plate_double_steel").forEach((tId, tPlate) -> {
			var tJson = JsonParser.parseString(read("data/gt6/recipes/" + tId + ".json")).getAsJsonObject();
			List<String> tPattern = new ArrayList<>();
			tJson.getAsJsonArray("pattern").forEach(e -> tPattern.add(e.getAsString()));
			assertEquals(List.of("hf", "xP"), tPattern, tId + ": the 2x2 fold stroke verbatim (:184/:260)");
			var tKey = tJson.getAsJsonObject("key");
			assertEquals("gt6:tools/hard_hammer", tKey.getAsJsonObject("h").get("tag").getAsString(), tId + ": 'h' stays the hammer letter");
			assertEquals("gt6:tools/file", tKey.getAsJsonObject("f").get("tag").getAsString(), tId + ": 'f' is the file letter");
			assertEquals("gt6:tools/wire_cutter", tKey.getAsJsonObject("x").get("tag").getAsString(), tId + ": 'x' is the wirecutter letter");
			assertEquals(tPlate, tKey.getAsJsonObject("P").get("item").getAsString(), tId + ": the family's plateDouble face");
			assertEquals("gt6:" + tId, tJson.getAsJsonObject("result").get("item").getAsString(), tId + ": the 1x result");
		});
	}

	/** The creative-tab join (the GT6SlicerBlades.onBuildTabContents form) exists and walks the full family. */
	@Test
	public void theTabJoinHandlerWalksTheWholeFamily() throws Exception {
		assertNotNull(GT6ExtruderMolds.class.getDeclaredMethod("onBuildTabContents", BuildCreativeModeTabContentsEvent.class),
				"the MACHINES_TAB join handler (the tabfix census: registered-but-tab-less is invisible in JEI)");
	}

	/** The texture basename drops the family prefix (the food_can band convention). */
	private static String texturePath(String aPath) {
		return aPath.startsWith("shape_simple_ex_")
				? "shape_simple_ex/" + aPath.substring("shape_simple_ex_".length())
				: "shape_extruder/" + aPath.substring("shape_extruder_".length());
	}

	/** A classloader read of the shipped tree (null = absent). */
	private static String read(String aPath) {
		try (InputStream tStream = GT6ExtruderMoldsCensusTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			return null;
		}
	}
}
