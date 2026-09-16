package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The 18103 Bedrock Mining Drill Head acceptance census (task p30-pool-drillhead-18103):
 * the row registration (the census test's row face, here re-anchored over
 * {@code NEW_PART_ROWS} — the membership that drives the creative-tab walk, the BET
 * valid list and every datagen walk), the generated blockstate/model/loot artifacts
 * (committed src/generated tree, the GT6StoneBlocksRenderDatagenTest classpath shape)
 * and the lang keys both locales. DESIGNS 0 = the ventilation/processor singleton form
 * (the property-free blockstate, the item model parents the bare block model).
 *
 * <p>Offline constraint: the DeferredRegister-backed handles stay unresolvable in a
 * headless JVM (the GTMultiBlocksOfflineTestBase frozen-registry note), so "registered +
 * in the tab" is pinned structurally — the row rides NEW_PART_ROWS, and
 * GTMultiBlocks.MULTIBLOCKS_TAB's displayItems walks that same map with zero per-row
 * code (the W3 card-① tab seam), while the artifacts pin the registry side via runData.
 */
public class GT6BedrockDrillHeadPartTest extends gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase {

	private static final String PATH = "bedrock_drill_head";
	private static final String KEY = "block.gt6." + PATH;

	/** One generated JSON off the committed tree (the stone-render classpath recipe). */
	private static JsonObject generated(String aPath) throws Exception {
		try (InputStream tStream = GT6BedrockDrillHeadPartTest.class.getClassLoader()
				.getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated artifact must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	@Test
	void theRowRidesEverySharedWalk() {
		// the tab walk + BET valid list + datagen walks all iterate NEW_PART_ROWS /
		// NEW_PART_BLOCKS_BY_PATH — membership here IS the creative-tab presence face
		assertTrue(GTMultiBlocks.NEW_PART_ROWS.stream().anyMatch(r -> r.path().equals(PATH)),
				"bedrock_drill_head rides NEW_PART_ROWS (the MULTIBLOCKS_TAB displayItems walk)");
		assertTrue(GTMultiBlocks.PART_ROWS.stream().anyMatch(r -> r.metaId() == 18103),
				"the upstream meta id 18103 (Loader :1178)");
	}

	@Test
	void generatedBlockstateIsTheDesignsZeroSingleton() throws Exception {
		JsonObject tState = generated("assets/gt6/blockstates/" + PATH + ".json");
		JsonObject tVariants = tState.getAsJsonObject("variants");
		assertEquals(1, tVariants.size(), "DESIGNS 0 — the property-free singleton variant");
		assertTrue(tVariants.has(""), "the empty variant key (the ventilation/processor form)");
		assertEquals("gt6:block/" + PATH, tVariants.get("").getAsJsonObject().get("model").getAsString());
	}

	@Test
	void generatedModelsReferenceTheBorrowedBedrockdrillTextures() throws Exception {
		JsonObject tModel = generated("assets/gt6/models/block/" + PATH + ".json");
		JsonObject tTextures = tModel.getAsJsonObject("textures");
		assertEquals("gt6:block/parts/bedrockdrill/0/colored/top", tTextures.get("up").getAsString(),
				"the borrowed :1178 NBT_TEXTURE \"bedrockdrill\" design-0 layer");
		assertNotNull(tTextures.get("overlay_side"), "the two-layer overlay decal form");
		JsonObject tItem = generated("assets/gt6/models/item/" + PATH + ".json");
		assertEquals("gt6:block/" + PATH, tItem.get("parent").getAsString(), "the item shows the placed look");
	}

	@Test
	void generatedLootTableIsTheSelfDrop() throws Exception {
		JsonObject tLoot = generated("data/gt6/loot_tables/blocks/" + PATH + ".json");
		assertEquals("gt6:" + PATH, tLoot.getAsJsonArray("pools").get(0).getAsJsonObject()
				.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString(),
				"the part MTE self-drop (the GT6PartBlockLoot dropSelf walk)");
	}

	@Test
	void langKeysLandBothLocales() throws Exception {
		JsonObject tEn = generated("assets/gt6/lang/en_us.json");
		assertEquals("Bedrock Mining Drill Head", tEn.get(KEY).getAsString(), "the :1178 name column verbatim");
		JsonObject tZh = generated("assets/gt6/lang/zh_cn.json");
		assertEquals("基岩钻头", tZh.get(KEY).getAsString(), "the dump mte-18103 word (zh_cn_ref.tsv:4327)");
	}
}
