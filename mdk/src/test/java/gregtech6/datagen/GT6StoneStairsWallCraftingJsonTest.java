package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The stone stairs/wall vanilla-degradation pins (task
 * debt-stairs-wall-vanilla-recipes — the JSON-ship half, the
 * GT6MachineWallCraftingJsonTest form): the three upstream BlockStones
 * {@code CR.shaped} lines ship per GT stone family under
 * {@code data/gt6/recipes/{stairs_rock,stairs_cobble,wall_cobble}/<snake>.json}
 * — 3× rockGt → 1 vanilla cobblestone stairs (:268), 3× family cobble → 4
 * (:328), 6× family cobble → 6 vanilla cobblestone wall (:329).
 *
 * <p>The output pin is {@code minecraft:cobblestone_stairs}, NOT
 * {@code minecraft:stone_stairs}: the upstream {@code Blocks.stone_stairs} is
 * 1.7.10 block id 67 = COBBLESTONE stairs (the MCP naming quirk; coder
 * correction #1, ruling decisions.2026-09-26-debt-stairs-walls-slab
 * coder_corrections) — the 1.20.1 {@code Items.STONE_STAIRS} is the 1.14
 * stone-textured block that never existed upstream. The input counts are the
 * upstream patterns verbatim — 3 X's in {@code " X","XX"} (coder correction
 * #2: the card's "2 inputs" misread the grid).
 *
 * <p>The 1.21 singular twins ({@code data/gt6/recipe/...}) are pinned for
 * EXISTENCE here; their 21-key forms ride the GT6DualDirectoryFaces adapter,
 * swept wholesale by GT6DualDirectoryFacesTest.everyRecipeAliasIsThe21KeyForm.
 */
public class GT6StoneStairsWallCraftingJsonTest {

	@BeforeAll
	static void boot() {
		try { net.minecraft.SharedConstants.tryDetectVersion(); } catch (Throwable ignored) {}
		try { net.minecraft.server.Bootstrap.bootStrap(); } catch (Throwable ignored) {}
	}

	/**
	 * The 17 stone families in GTStoneBlocks declaration order (= upstream CS.java:1668 /
	 * Loader_Rocks.java:56-136) — snake, the rockGt item id leaf, the Loader_Rocks line.
	 * The prismarine_light row's rock item is {@code rock_gt_prismarine}: MT.PrismarineLight's
	 * INTERNAL name is "Prismarine" (MT.java:2397), the slug the item id rides.
	 */
	private static final String[][] STONES = {
			{"granite_black",   "rock_gt_granite_black",   ":56"},
			{"granite_red",     "rock_gt_granite_red",     ":61"},
			{"basalt",          "rock_gt_basalt",          ":66"},
			{"marble",          "rock_gt_marble",          ":71"},
			{"limestone",       "rock_gt_limestone",       ":76"},
			{"granite",         "rock_gt_granite",         ":81"},
			{"diorite",         "rock_gt_diorite",         ":86"},
			{"andesite",        "rock_gt_andesite",        ":91"},
			{"komatiite",       "rock_gt_komatiite",       ":96"},
			{"greenschist",     "rock_gt_greenschist",     ":101"},
			{"blueschist",      "rock_gt_blueschist",      ":106"},
			{"kimberlite",      "rock_gt_kimberlite",      ":111"},
			{"quartzite",       "rock_gt_quartzite",       ":116"},
			{"prismarine_light","rock_gt_prismarine",     ":121"},
			{"prismarine_dark", "rock_gt_prismarine_dark",":126"},
			{"slate",           "rock_gt_slate",           ":131"},
			{"shale",           "rock_gt_shale",           ":136"},
	};

	/** Acceptance ①: every family ships the :268 rockGt row — pattern, input, vanilla stairs result. */
	@Test
	public void theSeventeenStairsFromRocksRows() throws Exception {
		for (String[] tStone : STONES) {
			JsonObject tJson = generated("stairs_rock/" + tStone[0]);
			assertEquals("minecraft:crafting_shaped", tJson.get("type").getAsString(), tStone[0] + " " + tStone[2] + " the shaped type");
			assertEquals(" X", tJson.getAsJsonArray("pattern").get(0).getAsString(), tStone[0] + " :268 row 1");
			assertEquals("XX", tJson.getAsJsonArray("pattern").get(1).getAsString(), tStone[0] + " :268 row 2");
			assertEquals("gt6:" + tStone[1], tJson.getAsJsonObject("key").getAsJsonObject("X").get("item").getAsString(),
					tStone[0] + ": the OP.rockGt.dat(mMaterial) column");
			JsonObject tResult = tJson.getAsJsonObject("result");
			assertEquals("minecraft:cobblestone_stairs", tResult.get("item").getAsString(),
					tStone[0] + ": the id-67 cobblestone stairs (coder correction #1)");
			assertFalse(tResult.has("count"), tStone[0] + ": count 1 rides the omitted default");
		}
	}

	/** Acceptance ①: every family ships the :328 cobble-stairs row — 3 in, 4 vanilla stairs out. */
	@Test
	public void theSeventeenStairsFromCobbleRows() throws Exception {
		for (String[] tStone : STONES) {
			JsonObject tJson = generated("stairs_cobble/" + tStone[0]);
			assertEquals(" X", tJson.getAsJsonArray("pattern").get(0).getAsString(), tStone[0] + " :328 row 1");
			assertEquals("XX", tJson.getAsJsonArray("pattern").get(1).getAsString(), tStone[0] + " :328 row 2");
			assertEquals("gt6:" + tStone[0] + "_cobble", tJson.getAsJsonObject("key").getAsJsonObject("X").get("item").getAsString(),
					tStone[0] + ": the mEqualBlocks[COBBL] self-add column (BlockStones.java:254)");
			JsonObject tResult = tJson.getAsJsonObject("result");
			assertEquals("minecraft:cobblestone_stairs", tResult.get("item").getAsString(), tStone[0] + ": the vanilla stairs output");
			assertEquals(4, tResult.get("count").getAsInt(), tStone[0] + ": the :328 count column");
		}
	}

	/** Acceptance ①: every family ships the :329 wall row — 6 in, 6 vanilla cobblestone walls out. */
	@Test
	public void theSeventeenWallsFromCobbleRows() throws Exception {
		for (String[] tStone : STONES) {
			JsonObject tJson = generated("wall_cobble/" + tStone[0]);
			assertEquals("XXX", tJson.getAsJsonArray("pattern").get(0).getAsString(), tStone[0] + " :329 row 1");
			assertEquals("XXX", tJson.getAsJsonArray("pattern").get(1).getAsString(), tStone[0] + " :329 row 2");
			assertEquals("gt6:" + tStone[0] + "_cobble", tJson.getAsJsonObject("key").getAsJsonObject("X").get("item").getAsString(),
					tStone[0] + ": the same COBBL column");
			JsonObject tResult = tJson.getAsJsonObject("result");
			assertEquals("minecraft:cobblestone_wall", tResult.get("item").getAsString(), tStone[0] + ": the vanilla wall output");
			assertEquals(6, tResult.get("count").getAsInt(), tStone[0] + ": the :329 count column");
		}
	}

	/**
	 * Acceptance ②③: every one of the 51 rows rides the 1.21 singular mirror too (the
	 * GT6DualDirectoryFaces whole-band mirror) — the plural↔singular pairing the global
	 * sweep only checks one direction of.
	 */
	@Test
	public void everyRowShipsTheSingularTwin() throws Exception {
		for (String[] tStone : STONES) {
			for (String tGroup : new String[] {"stairs_rock", "stairs_cobble", "wall_cobble"}) {
				assertNotNull(resourceOrNull("data/gt6/recipe/" + tGroup + "/" + tStone[0] + ".json"),
						"data/gt6/recipe/" + tGroup + "/" + tStone[0] + ".json rides the 21 mirror");
			}
		}
	}

	/** The datagen walk covers ALL seventeen families (a dropped GTStoneBlocks entry fails here, not silently). */
	@Test
	public void theWalkCensusIsTheSeventeenUpstreamStones() {
		assertEquals(17, gregtech6.registry.GTStoneBlocks.STONES.size(), "the CS.java:1668 declaration order");
		assertEquals(STONES.length, gregtech6.registry.GTStoneBlocks.STONES.size(), "the pin table tracks the walk");
	}

	private static JsonObject generated(String aId) throws Exception {
		byte[] tBytes = resourceOrNull("data/gt6/recipes/" + aId + ".json");
		assertNotNull(tBytes, "data/gt6/recipes/" + aId + ".json rides the generated-resources classpath");
		return JsonParser.parseString(new String(tBytes, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static byte[] resourceOrNull(String aPath) throws Exception {
		try (InputStream tStream = GT6StoneStairsWallCraftingJsonTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : tStream.readAllBytes();
		}
	}
}
