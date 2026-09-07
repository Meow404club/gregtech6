/**
 * Offline tests for task p21-stoneblocks-16item-registry-split (the p19-stoneblocks-render
 * pins re-keyed to the per-pair registry): the 272-PNG borrow census, the generated
 * blockstate/item-model/loot census (272 per-pair JSONs each), and the loot FORM pin
 * (variant-0 table drops the same stone's COBBL variant item, the other 271 dropSelf) —
 * the {@link GT6PrefixBlockRenderDatagenTest} split verbatim (the enumeration side walks
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

    /** The generated blockstate JSON of one composite path (committed tree, test classpath). */
    private static JsonObject blockstate(String aPath) throws Exception {
        try (InputStream tStream = GT6StoneBlocksRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream("assets/gt6/blockstates/" + aPath + ".json")) {
            assertNotNull(tStream, "the generated blockstate must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * The generated BLOCKSTATE census (272, the per-pair split): every (stone, variant) pair
     * has its OWN property-free single-state blockstate at the composite id path (variant 0
     * keeps the bare snake, the other 15 suffix the variant segment — {@code path()}), whose
     * lone {@code ""} row points at the pair's dedicated
     * {@code gt6:block/stones/<stone>/<variant>} model. The P19 16-row
     * {@code variant=<snake>} form is retired with the EnumProperty.
     */
    @Test
    void generatedBlockstatesArePerPairSingleState() throws Exception {
        assertEquals(GTStoneBlocks.STONES.size() * VARIANTS_PER_STONE, PINNED_PNG_TOTAL,
                "the blockstate census equals the PNG census (one state per borrowed texture)");
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            String tPath = GTStoneBlocks.path(tKey.stone().snake(), tKey.variant());
            JsonObject tState = blockstate(tPath);
            assertTrue(tState.has("variants"), tPath + ": the plain-variants form (no multipart)");
            var tVariants = tState.getAsJsonObject("variants");
            assertEquals(1, tVariants.size(), tPath + ": exactly the default \"\" row (property-free block)");
            assertTrue(tVariants.has(""), tPath + ": the row key is the default state");
            JsonObject tModel = tVariants.getAsJsonObject(""); // single-model rows serialize as an object
            assertEquals("gt6:block/stones/" + tKey.stone().snake() + "/" + tKey.variant().snake,
                    tModel.get("model").getAsString(), tPath + " row \"\" model key");
            assertTrue(!tModel.has("x") && !tModel.has("y") && !tModel.has("uvlock"),
                    tPath + ": a plain cube_all, no rotation");
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
     * The 272 generated item models each parent their OWN block model — the per-pair face
     * (the P19 "inventory shows the STONE look for every state" single-parent deviation is
     * RETIRED: one item id per variant shows its own variant's look, the upstream 1.7.10
     * ItemBlock per-meta icon face).
     */
    @Test
    void generatedItemModelsParentTheirOwnPairModel() throws Exception {
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            String tPath = "assets/gt6/models/item/" + GTStoneBlocks.path(tKey.stone().snake(), tKey.variant()) + ".json";
            try (InputStream tStream = GT6StoneBlocksRenderDatagenTest.class.getClassLoader().getResourceAsStream(tPath)) {
                assertNotNull(tStream, "the generated item model must be on the classpath: " + tPath);
                JsonObject tModel = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals("gt6:block/stones/" + tKey.stone().snake() + "/" + tKey.variant().snake,
                        tModel.get("parent").getAsString(), tKey + ": item parents its OWN pair block model");
            }
        }
    }

    /**
     * The 272 generated loot tables exist at the vanilla default per-block location and pin
     * the BlockStones.java:731 FORM (the p21 loot ruling): the variant-0 table's single pool
     * entry is the SAME STONE's COBBL variant item ({@code gt6:<snake>_cobble} — the :731
     * {@code aMeta == STONE ? COBBL : aMeta} swap, direct-translated now that the variant
     * item ids exist), and every other table drops itself. The P19 declared collapse
     * ("stone-yields-cobble unrecoverable without splitting 16 items per stone") closes here.
     */
    @Test
    void generatedLootTablesPinTheCobbleSwapAndSelfDrops() throws Exception {
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            String tPath = GTStoneBlocks.path(tKey.stone().snake(), tKey.variant());
            try (InputStream tStream = GT6StoneBlocksRenderDatagenTest.class.getClassLoader()
                    .getResourceAsStream("data/gt6/loot_tables/blocks/" + tPath + ".json")) {
                assertNotNull(tStream, "the generated loot table must be on the classpath: " + tPath);
                JsonObject tLoot = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals("minecraft:block", tLoot.get("type").getAsString(), tPath + ": the BLOCK param set");
                assertEquals("gt6:blocks/" + tPath, tLoot.get("random_sequence").getAsString(),
                        tPath + ": the vanilla default table location, zero block code");
                String tDropped = tLoot.getAsJsonArray("pools").get(0).getAsJsonObject()
                        .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString();
                if (tKey.variant() == StoneVariant.STONE) {
                    assertEquals("gt6:" + tKey.stone().snake() + "_" + StoneVariant.COBBL.snake, tDropped,
                            tPath + ": the :731 swap — variant 0 yields the SAME STONE's COBBL variant item");
                } else {
                    assertEquals("gt6:" + tPath, tDropped, tPath + ": drops itself (the :731 self arm)");
                }
            }
        }
    }

    /**
     * The offline side of the loot 1:1 rule (the p8 form): {@link GT6LootTables#stoneLootBlocks()}
     * snapshots {@link GTStoneBlocks#blockArray()} — both empty in this headless JVM, both
     * 272 in the datagen JVM.
     */
    @Test
    void stoneLootBlocksMirrorTheBlockArray() {
        assertEquals(GTStoneBlocks.STONES.size(), 17, "the 17-stone CS.java:1668 census");
        assertEquals(GTStoneBlocks.blockArray().size(), GT6LootTables.stoneLootBlocks().size(),
                "stoneLootBlocks is a snapshot of the block array (both empty offline, both 272 in the datagen JVM)");
    }
}
