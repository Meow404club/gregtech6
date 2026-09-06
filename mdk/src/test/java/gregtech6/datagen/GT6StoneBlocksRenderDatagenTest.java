/**
 * Offline tests for task p19-stoneblocks-render: the 272-PNG borrow census, the generated
 * model-key census (17x16), and the generated loot-table existence — the
 * {@link GT6PrefixBlockRenderDatagenTest} split verbatim (the enumeration side walks
 * {@link gregtech6.registry.GTStoneBlocks#registrationOrder()}, the generated-JSON side is
 * asserted against the committed src/generated tree; the write side is gated by runData:
 * first run written&gt;0, second run written:0).
 *
 * <p>Census ground truth (walked 2026-09-06 over tmp/gt6-1.7.10, the research card's
 * unwalked risk item closed): upstream ships
 * {@code textures/blocks/stones/gt.stone.<name>/<VARIANT>.png} as 17 folders x 16 PNGs =
 * 272 files, zero gaps, no {@code .mcmeta} animations — the borrow is complete, so the
 * expected gap list here is EMPTY and the walk pins 272.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GTStoneBlocks;

class GT6StoneBlocksRenderDatagenTest {

    /** The pinned borrow census (17 stones x 16 variants, walked upstream, zero gaps). */
    private static final int PINNED_PNG_TOTAL = 272;

    /** The per-stone variant count (the upstream BlockStones.java:93-108 icon table length). */
    private static final int VARIANTS_PER_STONE = 16;

    @BeforeAll
    static void initMaterialSystem() {
        // The GT6PrefixBlockRenderDatagenTest recipe: StoneVariant implements StringRepresentable
        // and the datagen-facing constants ride vanilla classes — bootStrap keeps the headless
        // JVM safe if the provider chain gets class-loaded through the walk.
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** The (stone, variant) walk the provider, the borrow and this census all derive from. */
    private static List<String> referencedPngs() {
        List<String> rList = new ArrayList<>();
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            rList.add(tKey.stone().snake() + "/" + tKey.variant().snake + ".png");
        }
        return rList;
    }

    /**
     * PNG census, POSITIVE side: the borrow is exactly the referenced set — every
     * (stone, variant) of the 272-pair walk has its lower-snake PNG under
     * {@code textures/block/stones/}, the path charset is 1.20.1-legal, and the expected
     * upstream gap list is EMPTY (census 2026-09-06: 272/272 existed upstream).
     */
    @Test
    void everyPairReferencesABorrowedLowercasePng() {
        List<String> tMissing = new ArrayList<>();
        for (String tRef : referencedPngs()) {
            assertTrue(tRef.matches("[a-z0-9_]+/[a-z0-9_]+\\.png"), "lowercase ResourceLocation charset: " + tRef);
            if (GT6StoneBlocksRenderDatagenTest.class.getResource("/assets/gt6/textures/block/stones/" + tRef) == null) {
                tMissing.add(tRef); // the declared gap list — pinned EMPTY by the census
            }
        }
        assertEquals(272, referencedPngs().size(), "the registration walk is the 17x16 census");
        assertEquals(List.of(), tMissing, "upstream gap list — the census found zero missing source PNGs");
    }

    /**
     * PNG census, NEGATIVE side: the borrow tree hosts exactly the 272 referenced files in
     * exactly the 17 stone folders — no strays, no gaps (the whole directory is this card's
     * borrow, unlike the shared materialicons root the p8 test had to filter).
     */
    @Test
    void borrowedPngTreeIsExactlyTheReferencedSet() throws Exception {
        var tUrl = GT6StoneBlocksRenderDatagenTest.class.getResource("/assets/gt6/textures/block/stones");
        assertNotNull(tUrl, "the stones borrow root must exist");
        Set<String> tReferenced = new HashSet<>(referencedPngs());
        Set<String> tFound = new HashSet<>();
        try (var tWalk = java.nio.file.Files.walk(java.nio.file.Path.of(tUrl.toURI()))) {
            tWalk.filter(p -> p.toString().endsWith(".png"))
                    .forEach(p -> tFound.add(p.getParent().getFileName() + "/" + p.getFileName()));
        }
        assertEquals(PINNED_PNG_TOTAL, tFound.size(), "17 folders x 16 PNGs, walked");
        assertEquals(tReferenced, tFound, "the borrow is 1:1 with the walk — zero strays, zero gaps");
    }

    /** The generated blockstate JSON of one stone (committed tree, test classpath). */
    private static JsonObject blockstate(String aSnake) throws Exception {
        try (InputStream tStream = GT6StoneBlocksRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream("assets/gt6/blockstates/" + aSnake + ".json")) {
            assertNotNull(tStream, "the generated blockstate must be on the classpath: " + aSnake);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * The generated MODEL-KEY census (17x16): every stone's blockstate carries exactly the 16
     * {@code variant=<snake>} rows in meta order, and every row's model key is the dedicated
     * {@code gt6:block/stones/<stone>/<variant>} (one model per texture — the merge rule is
     * degenerate here, the textures are per-pair dedicated color PNGs, NO tintindex).
     */
    @Test
    void generatedBlockstatesCarryAllSixteenVariantModelKeys() throws Exception {
        assertEquals(GTStoneBlocks.STONES.size() * VARIANTS_PER_STONE, PINNED_PNG_TOTAL,
                "the model-key census equals the PNG census (one texture per key)");
        for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) {
            JsonObject tState = blockstate(tStone.snake());
            assertTrue(tState.has("variants"), tStone.snake() + ": the plain-variants form (no multipart)");
            var tVariants = tState.getAsJsonObject("variants");
            assertEquals(VARIANTS_PER_STONE, tVariants.size(), tStone.snake() + ": 16 variant rows");
            int tIndex = 0;
            for (StoneVariant tVariant : StoneVariant.VALUES) {
                String tKey = "variant=" + tVariant.snake;
                assertTrue(tVariants.has(tKey), tStone.snake() + " missing row " + tKey);
                JsonObject tModel = tVariants.getAsJsonObject(tKey); // single-model rows serialize as an object (the vanilla VariantSelector form)
                assertEquals("gt6:block/stones/" + tStone.snake() + "/" + tVariant.snake,
                        tModel.get("model").getAsString(), tStone.snake() + " row " + tKey + " model key");
                assertTrue(!tModel.has("x") && !tModel.has("y") && !tModel.has("uvlock"),
                        tStone.snake() + " row " + tKey + ": a plain cube_all, no rotation");
                assertEquals(tIndex, tVariant.meta(), "walk order == meta order");
                tIndex++;
            }
        }
    }

    /**
     * The generated block models exist 1:1 with the 272 keys and reference the borrowed PNGs;
     * the cube_all form carries NO tintindex (the dedicated color-PNG route — the addPrefixBlocks
     * contrast pinned structurally).
     */
    @Test
    void generatedBlockModelsAreUntintedDedicatedCubeAlls() throws Exception {
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            String tPath = "assets/gt6/models/block/stones/" + tKey.stone().snake() + "/" + tKey.variant().snake + ".json";
            try (InputStream tStream = GT6StoneBlocksRenderDatagenTest.class.getClassLoader().getResourceAsStream(tPath)) {
                assertNotNull(tStream, "the generated block model must be on the classpath: " + tPath);
                JsonObject tModel = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals("minecraft:block/cube_all", tModel.get("parent").getAsString(),
                        tKey + ": the cube_all parent");
                assertEquals("gt6:block/stones/" + tKey.stone().snake() + "/" + tKey.variant().snake,
                        tModel.getAsJsonObject("textures").get("all").getAsString(),
                        tKey + ": its own dedicated borrowed PNG");
                String tRaw = tModel.toString();
                assertTrue(!tRaw.contains("tintindex"), tKey + ": the color-PNG route carries no tint");
            }
        }
    }

    /**
     * The 17 generated item models parent the variant-0 (STONE) block model — the spec ①
     * single-parent form; per-state item looks stay the declared deviation.
     */
    @Test
    void generatedItemModelsParentTheStoneVariantModel() throws Exception {
        for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) {
            String tPath = "assets/gt6/models/item/" + tStone.snake() + ".json";
            try (InputStream tStream = GT6StoneBlocksRenderDatagenTest.class.getClassLoader().getResourceAsStream(tPath)) {
                assertNotNull(tStream, "the generated item model must be on the classpath: " + tPath);
                JsonObject tModel = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals("gt6:block/stones/" + tStone.snake() + "/" + StoneVariant.STONE.snake,
                        tModel.get("parent").getAsString(), tStone.snake() + ": item parents the STONE block model");
            }
        }
    }

    /**
     * The 17 generated loot tables exist at the vanilla default location and are the dropSelf
     * shape (the GT6WireLootLaserTest yardstick). Declared collapse pinned here: the table is
     * per BLOCK and self-dropping — upstream BlockStones.java:731 swaps variant STONE's drop
     * to COBBL, but this port has ONE item id per stone, so both outcomes are the same
     * ItemStack and dropSelf is the collapsed equivalent (GT6LootTables.stoneLootBlocks doc).
     */
    @Test
    void generatedLootTablesAreSelfDropPerStone() throws Exception {
        for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) {
            String tPath = "data/gt6/loot_tables/blocks/" + tStone.snake() + ".json";
            try (InputStream tStream = GT6StoneBlocksRenderDatagenTest.class.getClassLoader().getResourceAsStream(tPath)) {
                assertNotNull(tStream, "the generated loot table must be on the classpath: " + tPath);
                JsonObject tLoot = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals("minecraft:block", tLoot.get("type").getAsString(), tStone.snake() + ": the BLOCK param set");
                assertEquals("gt6:blocks/" + tStone.snake(), tLoot.get("random_sequence").getAsString(),
                        tStone.snake() + ": the vanilla default table location, zero block code");
                var tPool = tLoot.getAsJsonArray("pools").get(0).getAsJsonObject();
                assertEquals("gt6:" + tStone.snake(),
                        tPool.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString(),
                        tStone.snake() + ": drops itself (the :731 meta swap collapses to the same item id)");
            }
        }
    }

    /**
     * The offline side of the loot 1:1 rule (the p8 form): {@link GT6LootTables#stoneLootBlocks()}
     * snapshots {@link GTStoneBlocks#blockArray()} — both empty in this headless JVM, both 17
     * in the datagen JVM.
     */
    @Test
    void stoneLootBlocksMirrorTheBlockArray() {
        assertEquals(GTStoneBlocks.STONES.size(), 17, "the 17-stone CS.java:1668 census");
        assertEquals(GTStoneBlocks.blockArray().size(), GT6LootTables.stoneLootBlocks().size(),
                "stoneLootBlocks is a snapshot of the block array (both empty offline, both 17 in the datagen JVM)");
    }
}
