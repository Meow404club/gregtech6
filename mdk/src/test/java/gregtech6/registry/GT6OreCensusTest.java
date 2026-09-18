/**
 * The p30 ore wave close-out full-census ratchet (task p30-ore-5-census): the four
 * ledgers of the landed ore universe pinned against EACH OTHER per key, not just per
 * card — the registration walk (ledger 1, ore-1: 3922), the generated blockstates +
 * item models + shared base models + atlas seam (ledger 2, ore-3), the generated loot
 * tables in BOTH directory bands (ledger 3, ore-4: 3922 x 2 = 7844), and the borrowed
 * materialicon ore textures (ledger 4, ore-2: 60 PNGs) — plus the creative tab content
 * face (the material axis minus the mHidden filter, the GT6OreBlocks registerCreativeTab
 * walk).
 *
 * <p>Per-key reconciliation (the "四源一致" acceptance): every (family, form, material)
 * path of {@link GT6OreBlocks#registrationOrder()} must own exactly one blockstate JSON,
 * one item-model JSON and two loot JSONs (forge loot_tables + 21.1 loot_table), with NO
 * ore-prefixed orphans in any ledger — a missing file is a merge drift, an extra file a
 * dead entry; both fail. The texture side walks the CONSUMER face ({@link
 * gregtech6.client.ore.GTOreBakedModel#buildParams()}): every gt6-namespaced sprite any
 * ore references must resolve to a PNG on disk — the missingno lattice is killed by
 * construction (the ore-2 merge made the 30 atlas sources real; this pin keeps it so).
 *
 * <p>Offline-safe: the walk is registry-free (the census-walk posture) — the file faces
 * resolve from the mdk root (the GT6TextureCensusTest mdkRoot walk), the tab face reads
 * only {@link GT6OreBlocks#materialAxis()} + mHidden.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.oredict.OreDictMaterial;
import gregtech6.client.ore.GTOreBakedModel;
import gregtech6.registry.GT6BedrockOreBlocks;
import gregtech6.registry.GT6OreBlocks.FormKind;
import gregtech6.registry.GT6OreBlocks.OreKey;

class GT6OreCensusTest {

    /** Ledger 1 — the registration walk (74 form-rows x M=53, the ore-1 pin). */
    private static final int PINNED_BLOCKS = 3922;
    /** Ledger 2 — the ore-3 faces: per-pair blockstates + item models, shared base models.
     *  29 since task p31-bedrock-ore-worldgen: the ONE shared bedrock cube
     *  32 since task p31-nether-lens-end-yield: the THREE nether stand-in cubes
     *  (nether_quartz_ore / amethyst_block / packed_mud, GT6NetherOres.KEYS)
     *  (gt6:block/ore/bedrock, minecraft:block/bedrock) joins the 28. */
    private static final int PINNED_BASE_MODELS = 32;
    /** Ledger 3 — the ore-4 loot trees: one table per block, BOTH directory bands. */
    private static final int PINNED_LOOT_TOTAL = 2 * PINNED_BLOCKS;
    /** Ledger 4 — the ore-2 texture batch: 15 SETs x {ore, ore_small, + the two overlays}. */
    private static final int PINNED_SETS = 15;
    private static final int PINNED_TEXTURES = PINNED_SETS * 4;
    /** The atlas seam: every distinct overlay sprite stitched (15 SETs x normal+small). */
    private static final int PINNED_ATLAS_SOURCES = PINNED_SETS * 2;
    /** The tab content face: every axis material is visible today (53 = M, the mHidden filter empty). */
    private static final int PINNED_TAB_ITEMS = 53;

    private static final Path STATIC_TREE = Path.of("src", "main", "resources");
    private static final Path GENERATED_TREE = Path.of("src", "generated", "resources");

    @BeforeAll
    static void initMaterialSystem() {
        GTMaterialItems.initMaterials(); // MT/OP must exist before any field dereference
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** Location of the mdk project root, walking up from the (leg-dependent) test working dir. */
    private static Path mdkRoot() {
        for (Path tP = Path.of("").toAbsolutePath(); tP != null; tP = tP.getParent()) {
            if (Files.isRegularFile(tP.resolve("tools").resolve("gen_textures.py"))) {
                return tP;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    /** The walk paths (registration order) — ledger 1's face, shared by every check. */
    private static List<String> walkPaths() {
        List<String> rPaths = new ArrayList<>();
        for (OreKey tKey : GT6OreBlocks.registrationOrder()) rPaths.add(GT6OreBlocks.path(tKey));
        return rPaths;
    }

    /**
     * The bedrock band's blockstate/item-model paths (task p31-bedrock-ore-worldgen, 2 x
     * 45 = 90) — they share the ore_-prefixed generated directories with ledger 2, so the
     * no-orphans censuses must count them. They carry NO loot files (noLootTable = the
     * upstream Drops_None), so the ledger-3 loot faces stay the 3922-only walks.
     */
    private static List<String> bedrockPaths() {
        List<String> rPaths = new ArrayList<>();
        for (GT6BedrockOreBlocks.BedrockKey tKey : GT6BedrockOreBlocks.registrationOrder()) {
            rPaths.add(GT6BedrockOreBlocks.path(tKey.small(), tKey.material()));
        }
        return rPaths;
    }

    /** Every regular file directly in one directory whose name starts with the prefix. */
    private static Set<String> fileStems(Path tDir, String aPrefix) throws IOException {
        if (!Files.isDirectory(tDir)) throw new AssertionError("missing ledger dir: " + tDir);
        Set<String> rStems = new HashSet<>();
        try (Stream<Path> tWalk = Files.list(tDir)) {
            tWalk.filter(Files::isRegularFile)
                    .map(tFile -> tFile.getFileName().toString())
                    .filter(tName -> tName.startsWith(aPrefix))
                    .forEach(tName -> rStems.add(tName.substring(0, tName.length() - ".json".length())));
        }
        return rStems;
    }

    /**
     * The OP.oreRaw ITEM models (ore_raw_*, 618) live in the same item-models directory —
     * the p8 item-walk face, NOT this wave's ledger; the orphan censuses exclude them.
     */
    private static Set<String> fileStemsMinusRaw(Path tDir, String aPrefix) throws IOException {
        Set<String> rStems = fileStems(tDir, aPrefix);
        rStems.removeAll(fileStems(tDir, "ore_raw_"));
        return rStems;
    }

    /** Ledger 1 re-pin + the per-key existence face of ledgers 2 and 3. */
    @Test
    void perKeyAllFourLedgersAgree() throws IOException {
        List<String> tPaths = walkPaths();
        assertEquals(PINNED_BLOCKS, tPaths.size(), "ledger 1: the registration walk (74 x 53)");
        Path tAssets = mdkRoot().resolve(GENERATED_TREE).resolve("assets").resolve("gt6");
        Path tData = mdkRoot().resolve(GENERATED_TREE).resolve("data").resolve("gt6");
        for (String tPath : tPaths) {
            assertTrue(Files.isRegularFile(tAssets.resolve("blockstates").resolve(tPath + ".json")),
                    "ledger 2: blockstate JSON of " + tPath);
            assertTrue(Files.isRegularFile(tAssets.resolve("models").resolve("item").resolve(tPath + ".json")),
                    "ledger 2: item model JSON of " + tPath);
            assertTrue(Files.isRegularFile(tData.resolve("loot_tables").resolve("blocks").resolve(tPath + ".json")),
                    "ledger 3: loot_tables band JSON of " + tPath);
            assertTrue(Files.isRegularFile(tData.resolve("loot_table").resolve("blocks").resolve(tPath + ".json")),
                    "ledger 3: loot_table band JSON of " + tPath);
        }
    }

    /** No orphans: the ore-prefixed file census of ledgers 2 and 3 equals the walk exactly. */
    @Test
    void oreFileCensusHasNoOrphans() throws IOException {
        Set<String> tWalk = new HashSet<>(walkPaths());
        tWalk.addAll(bedrockPaths()); // ledger 2's generated dirs carry the bedrock band too (p31-bedrock-ore)
        Set<String> tLootWalk = new HashSet<>(walkPaths()); // the loot bands stay 3922-only: the bedrock band is noLootTable
        Path tAssets = mdkRoot().resolve(GENERATED_TREE).resolve("assets").resolve("gt6");
        Path tData = mdkRoot().resolve(GENERATED_TREE).resolve("data").resolve("gt6");
        assertEquals(tWalk, fileStems(tAssets.resolve("blockstates"), "ore_"),
                "ledger 2: the blockstate ore census is the walk, no drift either way");
        assertEquals(tWalk, fileStemsMinusRaw(tAssets.resolve("models").resolve("item"), "ore_"),
                "ledger 2: the item-model ore census is the walk, no drift either way (ore_raw_* excluded)");
        assertEquals(tLootWalk, fileStems(tData.resolve("loot_tables").resolve("blocks"), "ore_"),
                "ledger 3: the loot_tables band ore census is the walk, no drift either way");
        assertEquals(tLootWalk, fileStems(tData.resolve("loot_table").resolve("blocks"), "ore_"),
                "ledger 3: the loot_table band ore census is the walk, no drift either way");
    }

    /** The ledger-3 band total (3922 x 2 = 7844) and the ledger-2 shared base models (28). */
    @Test
    void ledgerTotalsArePinned() throws IOException {
        Path tAssets = mdkRoot().resolve(GENERATED_TREE).resolve("assets").resolve("gt6");
        Path tData = mdkRoot().resolve(GENERATED_TREE).resolve("data").resolve("gt6");
        int tLoot = 0;
        for (String tBand : new String[] {"loot_tables", "loot_table"}) {
            try (Stream<Path> tWalk = Files.list(tData.resolve(tBand).resolve("blocks"))) {
                tLoot += tWalk.filter(Files::isRegularFile)
                        .filter(tFile -> tFile.getFileName().toString().startsWith("ore_")).count();
            }
        }
        assertEquals(PINNED_LOOT_TOTAL, tLoot, "ledger 3: both directory bands carry the walk");
        // the shared base models: 11 vanilla-anchor JSONs + the 17 GT-stone models
        // under ore/stones/ + the p31-bedrock-ore bedrock cube + the 3 p31-nether stand-ins = 32
        int tBase = 0;
        try (Stream<Path> tWalk = Files.walk(tAssets.resolve("models").resolve("block").resolve("ore"))) {
            tBase = (int) tWalk.filter(Files::isRegularFile)
                    .filter(tFile -> tFile.getFileName().toString().endsWith(".json")).count();
        }
        assertEquals(PINNED_BASE_MODELS, tBase,
                "ledger 2: the shared ore base models (9 vanilla + 2 cobble broken + 17 GT stones)");
    }

    /** The atlas seam: every distinct overlay sprite is stitched into the blocks atlas. */
    @Test
    void atlasStitchesEveryOverlaySprite() throws IOException {
        Path tAtlas = mdkRoot().resolve(GENERATED_TREE).resolve("assets").resolve("minecraft")
                .resolve("atlases").resolve("blocks.json");
        String tBody = Files.readString(tAtlas);
        List<net.minecraft.resources.ResourceLocation> tSprites = GTOreBakedModel.overlaySprites();
        assertEquals(PINNED_ATLAS_SOURCES, tSprites.size(),
                "ledger 2/4 seam: 15 SETs x {ore, ore_small} distinct overlay sprites");
        for (net.minecraft.resources.ResourceLocation tSprite : tSprites) {
            assertTrue(tBody.contains("\"" + tSprite + "\""),
                    "the atlas stitches " + tSprite);
        }
    }

    /**
     * Ledger 4 + the missingno kill: every gt6-namespaced sprite of every param
     * (the GT17 stone bases + every overlay) and the vanilla-anchored base sprites of
     * the walk resolve to a PNG on disk (or to the vanilla namespace for the 9 anchors);
     * the 15 SETs carry the full four-file ore form (60 PNGs, ore-2's batch).
     */
    @Test
    void everyOreSpriteResolvesToAPng() throws IOException {
        Path tTextures = mdkRoot().resolve(STATIC_TREE).resolve("assets").resolve("gt6")
                .resolve("textures");
        Set<String> tSets = new HashSet<>();
        for (OreKey tKey : GT6OreBlocks.registrationOrder()) {
            GTOreBakedModel.Params tParams = GTOreBakedModel.paramsOf(tKey);
            for (net.minecraft.resources.ResourceLocation tSprite
                    : new net.minecraft.resources.ResourceLocation[] {
                            tParams.baseSprite(), tParams.overlaySprite()}) {
                if (tSprite.getNamespace().equals("gt6")) {
                    // the sprite path already carries its block/ or item/ prefix
                    assertTrue(Files.isRegularFile(tTextures.resolve(tSprite.getPath() + ".png")),
                            "every gt6 ore sprite resolves: " + tSprite);
                } else {
                    assertEquals("minecraft", tSprite.getNamespace(),
                            "ore sprites are gt6 or vanilla-anchored: " + tSprite);
                }
            }
            tSets.add(GTOreBakedModel.setOf(tKey.material()));
        }
        assertEquals(PINNED_SETS, tSets.size(), "the SET axis over the material axis");
        for (String tSet : tSets) {
            for (String tForm : new String[] {"ore", "ore_small", "ore_overlay", "ore_small_overlay"}) {
                assertTrue(Files.isRegularFile(tTextures.resolve("block").resolve("materialicons")
                                .resolve(tSet).resolve(tForm + ".png")),
                        "ledger 4: materialicons/" + tSet + "/" + tForm + ".png");
            }
        }
    }

    /**
     * The creative tab content face (the registerCreativeTab walk): the stone normal
     * family, axis order, the mHidden filter off (PrefixBlockItem.java:114 semantics,
     * SHOW_HIDDEN_MATERIALS=false) — the pinned visible count and the membership.
     */
    @Test
    void tabContentIsTheAxisMinusHidden() {
        List<OreDictMaterial> tVisible = new ArrayList<>();
        for (OreDictMaterial tMaterial : GT6OreBlocks.materialAxis()) {
            if (tMaterial.mHidden) continue; // GT6OreBlocks.registerCreativeTab's filter line
            tVisible.add(tMaterial);
        }
        assertEquals(PINNED_TAB_ITEMS, tVisible.size(),
                "the ore tab shows the axis minus hidden — bump only with an axis/hidden delta");
        Set<String> tWalk = new HashSet<>(walkPaths());
        for (OreDictMaterial tMaterial : tVisible) {
            String tPath = GT6OreBlocks.path(new OreKey(GT6OreBlocks.TAB_FAMILY, FormKind.NORMAL, tMaterial));
            assertTrue(tWalk.contains(tPath), "the tab face is a walk subset: " + tPath);
            assertTrue(tPath.startsWith("ore_stone_"), "the tab family is the stone family: " + tPath);
        }
    }
}
