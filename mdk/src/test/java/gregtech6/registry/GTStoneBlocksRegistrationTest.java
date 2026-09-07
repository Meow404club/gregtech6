/**
 * Tests for task p21-stoneblocks-16item-registry-split (superseding the p19 registration
 * pins it keeps): the GT6 stone universe census (272 per-pair ids) + the four
 * mapping-table transcriptions + the Loader_Rocks parameter rows + the oredict
 * equivalence face + the id-scheme ruling.
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree
 * or a conscious decision is forced):
 * <ul>
 * <li>CS.java:1668 — the {@code stones} array literal: 17 elements,
 *     GraniteBlack..Shale, the exact stone set (NOT all of MT.STONES).</li>
 * <li>BlockStones.java:71-77 — the 16 meta constants and their declaration order.</li>
 * <li>BlockStones.java:81-84 — CHISEL/FILE/HAMMER/MOSS_MAPPINGS, meta-indexed byte
 *     arrays; CHISEL_MAPPINGS[6]==[7]==CHISL is the self-mapping pin.</li>
 * <li>Loader_Rocks.java:56-139 — the 17 ctor rows: (resistance, hardness, harvestLevel,
 *     witherProof) with the upstream ctor's leading-two-floats order
 *     (BlockStonesGT.java:37).</li>
 * <li>BlockStones.java:117-132 — the 16 lang suffix patterns.</li>
 * <li>BlockStones.java:135-194 — the OM.reg_ face captured by oreDictMappings().</li>
 * <li>BlockStones.java:731 — the getDrops per-meta item face the per-pair split
 *     restores ({@code ST.make(this, 1, aMeta == STONE ? COBBL : aMeta)}).</li>
 * </ul>
 *
 * <p>Offline-safe by construction (the GTMaterialBlocksRegistrationTest lesson): the
 * walks touch only the spec records, the enum and the byte tables — no RegisterEvent,
 * no block construction (the block ctor is exercised on the live legs only).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictPrefix;
import gregtech6.block.stone.GTStoneBlock;
import gregtech6.block.stone.StoneVariant;

class GTStoneBlocksRegistrationTest {

    /** CS.java:1668 — the array literal, declaration order (internal names). */
    private static final List<String> UPSTREAM_STONES = List.of(
        "GraniteBlack", "GraniteRed", "Basalt", "Marble", "Limestone", "Granite", "Diorite",
        "Andesite", "Komatiite", "SchistGreen", "SchistBlue", "Kimberlite", "Quartzite",
        "PrismarineLight", "PrismarineDark", "Slate", "Shale");

    /** The snake ids of the 17 stones, CS.java:1668 order (the card's pinned id list). */
    private static final List<String> SNAKES = List.of(
        "granite_black", "granite_red", "basalt", "marble", "limestone", "granite", "diorite",
        "andesite", "komatiite", "greenschist", "blueschist", "kimberlite", "quartzite",
        "prismarine_light", "prismarine_dark", "slate", "shale");

    /** BlockStones.java:71-77 — the meta constants in declaration (= meta) order. */
    private static final List<String> UPSTREAM_VARIANTS = List.of(
        "STONE", "COBBL", "MCOBL", "BRICK", "CRACK", "MBRIK", "CHISL", "SMOTH",
        "RNFBR", "RSTBR", "TILES", "STILE", "SBRIK", "WINDA", "WINDB", "QBRIK");

    /** BlockStones.java:81 — CHISEL_MAPPINGS literal (SMOTH..STILE by meta index). */
    private static final byte[] UPSTREAM_CHISEL = {7, 1, 2, 4, 1, 2, 6, 6, 8, 9, 11, 11, 11, 14, 13, 11};
    /** BlockStones.java:82 — FILE_MAPPINGS literal. */
    private static final byte[] UPSTREAM_FILE = {7, 1, 2, 12, 4, 5, 6, 7, 8, 9, 11, 11, 11, 14, 13, 11};
    /** BlockStones.java:83 — HAMMER_MAPPINGS literal (stone maps to itself). */
    private static final byte[] UPSTREAM_HAMMER = {0, 1, 2, 4, 1, 4, 4, 1, 8, 9, 4, 4, 4, 4, 4, 4};
    /** BlockStones.java:84 — MOSS_MAPPINGS literal. */
    private static final byte[] UPSTREAM_MOSS = {0, 2, 2, 5, 5, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    /** The 272-pair census (17 x 16) — the registration walk AND the 272-item id yardstick. */
    private static final int PINNED_TOTAL = 272;

    @BeforeAll
    static void initMaterialSystem() {
        // GTMaterialBlocksRegistrationTest shape: the material system must exist before
        // any MT.STONES/OP field is dereferenced (GTMaterialItems.initMaterials()).
        GTMaterialItems.initMaterials();
        // the composed-name face builds Component.translatable contents (task
        // p20-i18n-compose-rows), which initializes vanilla registry classes — bootstrap
        // first, the GT6LangParityTest.boot posture (offline-expected throwables ignored)
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** The census: 17 stones, CS.java:1668 order, each carrying exactly the 16 variants in meta order. */
    @Test
    void stoneCensusIsPinned() {
        assertEquals(17, GTStoneBlocks.STONES.size(), "CS.java:1668 pins exactly 17 stones");
        assertEquals(SNAKES, GTStoneBlocks.STONES.stream().map(GTStoneBlocks.StoneSpec::snake).toList(),
                "stone order must be the CS.java:1668 declaration order");
        assertEquals(PINNED_TOTAL, GTStoneBlocks.registrationOrder().size(), "17 x 16 registration pairs");
        Set<String> tSeen = new HashSet<>();
        for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) {
            assertTrue(tSeen.add(tStone.snake()), "stone ids must be unique: " + tStone.snake());
        }
    }

    /**
     * The id scheme (the p21 ADR ruling ②): variant 0 keeps the bare {@code gt6:<snake>} id
     * (the P19 reference face diffs minimally), the other 15 variants suffix the variant's
     * serialized name — exactly the model/texture path segment the render card landed — and
     * the 272 composite paths are pairwise distinct.
     */
    @Test
    void perPairIdSchemeIsPinned() {
        assertEquals("marble", GTStoneBlocks.path("marble", StoneVariant.STONE), "variant 0 keeps the bare P19 id");
        assertEquals("marble_bricks_chiseled", GTStoneBlocks.path("marble", StoneVariant.CHISL));
        assertEquals("granite_black_small_bricks", GTStoneBlocks.path("granite_black", StoneVariant.SBRIK));
        assertEquals("prismarine_light_windmill_tiles_a", GTStoneBlocks.path("prismarine_light", StoneVariant.WINDA));
        Set<String> tPaths = new HashSet<>();
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            assertTrue(tPaths.add(GTStoneBlocks.path(tKey.stone().snake(), tKey.variant())),
                    "composite ids must be unique: " + tKey);
        }
        assertEquals(PINNED_TOTAL, tPaths.size(), "272 distinct composite ids");
        // the model-path correspondence: every composite suffix IS the texture segment
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            if (tKey.variant() == StoneVariant.STONE) continue;
            assertTrue(GTStoneBlocks.path(tKey.stone().snake(), tKey.variant())
                            .endsWith("_" + tKey.variant().snake),
                    "the id suffix is the stones/<stone>/<variant> path segment: " + tKey);
        }
    }

    /** The materials are the MT fields, load-bearing by identity (compile-anchored to CS.java:1668's MT references). */
    @Test
    void stoneMaterialsAreTheCSArrayMaterials() {
        List<Object> tExpected = List.of(
            MT.STONES.GraniteBlack, MT.STONES.GraniteRed, MT.STONES.Basalt, MT.STONES.Marble,
            MT.STONES.Limestone, MT.STONES.Granite, MT.STONES.Diorite, MT.STONES.Andesite,
            MT.STONES.Komatiite, MT.STONES.Greenschist, MT.STONES.Blueschist, MT.STONES.Kimberlite,
            MT.STONES.Quartzite, MT.PrismarineLight, MT.PrismarineDark, MT.STONES.Slate, MT.STONES.Shale);
        assertEquals(17, tExpected.size());
        for (int i = 0; i < tExpected.size(); i++) {
            assertSame(tExpected.get(i), GTStoneBlocks.STONES.get(i).material().get(),
                    "stone " + SNAKES.get(i) + " must carry its CS.java:1668 material");
        }
    }

    /** The variant enum is the upstream 16-meta set, declaration order = meta value. */
    @Test
    void variantsAreTheUpstreamMetaSet() {
        assertEquals(16, StoneVariant.VALUES.length, "BlockStones.java:71-77 pins 16 variants");
        for (int i = 0; i < 16; i++) {
            assertEquals(i, StoneVariant.VALUES[i].meta(), "declaration order IS the meta order");
            assertEquals(UPSTREAM_VARIANTS.get(i), StoneVariant.VALUES[i].name(),
                    "enum constant names are the upstream meta constant names (compile anchors)");
        }
        assertEquals(6, StoneVariant.CHISL.meta(), "CHISL=6, the stoneChiseled carrier (BlockStones.java:75)");
    }

    /** The four mapping tables are the BlockStones.java:81-84 literals, byte for byte. */
    @Test
    void mappingTablesAreUpstreamLiterals() {
        assertTrue(java.util.Arrays.equals(UPSTREAM_CHISEL, GTStoneBlocks.CHISEL_MAPPINGS), "CHISEL_MAPPINGS (BlockStones.java:81)");
        assertTrue(java.util.Arrays.equals(UPSTREAM_FILE, GTStoneBlocks.FILE_MAPPINGS), "FILE_MAPPINGS (BlockStones.java:82)");
        assertTrue(java.util.Arrays.equals(UPSTREAM_HAMMER, GTStoneBlocks.HAMMER_MAPPINGS), "HAMMER_MAPPINGS (BlockStones.java:83)");
        assertTrue(java.util.Arrays.equals(UPSTREAM_MOSS, GTStoneBlocks.MOSS_MAPPINGS), "MOSS_MAPPINGS (BlockStones.java:84)");
        for (byte[] tTable : List.of(GTStoneBlocks.CHISEL_MAPPINGS, GTStoneBlocks.FILE_MAPPINGS,
                GTStoneBlocks.HAMMER_MAPPINGS, GTStoneBlocks.MOSS_MAPPINGS)) {
            assertEquals(16, tTable.length);
            for (byte tValue : tTable) {
                assertTrue(tValue >= 0 && tValue < 16, "mapping targets must be meta values");
            }
        }
    }

    /** The chisel self-mapping pin: CHISEL_MAPPINGS[6]==[7]==CHISL — chiseling a chiseled variant is a no-op. */
    @Test
    void chiselMappingsSelfMapChiseled() {
        assertEquals(StoneVariant.CHISL.meta(), GTStoneBlocks.CHISEL_MAPPINGS[StoneVariant.CHISL.meta()],
                "CHISEL_MAPPINGS[6]==CHISL (BlockStones.java:81)");
        assertEquals(GTStoneBlocks.CHISEL_MAPPINGS[6], GTStoneBlocks.CHISEL_MAPPINGS[7],
                "the :573 gate (CHISEL_MAPPINGS[aMeta]!=aMeta) must see no chisel work on 6/7");
        // the reachable chisel targets are exactly what SMOTH/BRICK-ish variants turn into
        assertEquals(StoneVariant.CHISL.meta(), GTStoneBlocks.CHISEL_MAPPINGS[StoneVariant.SMOTH.meta()],
                "SMOTH(7) chisels to CHISL — the RM.generify stonebrick-mossy counterpart (BlockStones.java:419)");
    }

    /** The Loader_Rocks.java:56-139 ctor rows, verbatim (hardness/resistance multipliers, level, witherProof). */
    @Test
    void loaderRocksRowsAreVerbatim() {
        // {snake, hardnessMultiplier, resistanceMultiplier, harvestLevel, witherProof}
        record Row(String snake, float hardness, float resistance, int level, boolean witherProof) {}
        List<Row> tRows = List.of(
            new Row("granite_black",    3.00F, 6.00F, 3, true),   // :56
            new Row("granite_red",      3.00F, 6.00F, 3, true),   // :61
            new Row("basalt",           2.00F, 3.00F, 2, false),  // :66
            new Row("marble",           0.50F, 0.75F, 0, false),  // :71
            new Row("limestone",        0.50F, 0.75F, 0, false),  // :76
            new Row("granite",          1.00F, 2.00F, 1, false),  // :81
            new Row("diorite",          0.50F, 0.75F, 0, false),  // :86
            new Row("andesite",         0.50F, 0.75F, 0, false),  // :91
            new Row("komatiite",        2.00F, 3.00F, 2, false),  // :96
            new Row("greenschist",      0.50F, 0.75F, 0, false),  // :101
            new Row("blueschist",       0.50F, 0.75F, 0, false),  // :106
            new Row("kimberlite",       2.00F, 3.00F, 2, false),  // :111
            new Row("quartzite",        0.50F, 0.75F, 0, false),  // :116
            new Row("prismarine_light", 0.50F, 0.75F, 0, false),  // :121
            new Row("prismarine_dark",  0.50F, 0.75F, 1, false),  // :126
            new Row("slate",            0.50F, 0.75F, 1, false),  // :131
            new Row("shale",            0.50F, 0.75F, 0, false)); // :136
        assertEquals(17, tRows.size());
        for (int i = 0; i < tRows.size(); i++) {
            Row tRow = tRows.get(i);
            GTStoneBlocks.StoneSpec tSpec = GTStoneBlocks.STONES.get(i);
            assertEquals(tRow.snake(), tSpec.snake(), "row alignment at " + i);
            assertEquals(tRow.hardness(), tSpec.hardnessMultiplier(), tRow.snake() + " hardnessMultiplier");
            assertEquals(tRow.resistance(), tSpec.resistanceMultiplier(), tRow.snake() + " resistanceMultiplier");
            assertEquals(tRow.level(), tSpec.harvestLevel(), tRow.snake() + " harvestLevel");
            assertEquals(tRow.witherProof(), tSpec.witherProof(), tRow.snake() + " witherProof");
        }
    }

    /**
     * The block: a degenerate pure block carrying its FIXED variant (the p21 per-pair
     * shape), NO blockstate property any more (the P19 EnumProperty retired — the declared
     * placement-state migration loss), and the BlockMetaType.java:61-62 strength conversion.
     */
    @Test
    void blockCarriesItsFixedVariantAndUpstreamStrength() {
        GTStoneBlock tBlock = new GTStoneBlock("granite_black", StoneVariant.SMOTH, MT.STONES.GraniteBlack,
                3.00F, 6.00F, 3, true);
        assertSame(StoneVariant.SMOTH, tBlock.variant, "the block's variant is fixed at construction");
        assertTrue(tBlock.defaultBlockState().getProperties().isEmpty(),
                "the p21 block is a pure block — the P19 variant property is retired");
        assertSame(tBlock, tBlock.defaultBlockState().getBlock());
        // the impl ignores both args (BlockBehaviour.java:566-568 returns the field) — nulls are the offline form
        assertEquals(4.5F, tBlock.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "hardness = 3.00 * 1.5 (BlockMetaType.java:61)");
        assertEquals(60.0F, tBlock.getExplosionResistance(), 1.0e-6F,
                "resistance = 6.00 * 10 (BlockMetaType.java:62, Block.java:331)");
        assertTrue(tBlock.witherProof && tBlock.harvestLevel == 3, "the granite row data rides the block");
        // every pair of the walk yields an independent block face
        GTStoneBlock tCobble = new GTStoneBlock("granite_black", StoneVariant.COBBL, MT.STONES.GraniteBlack,
                3.00F, 6.00F, 3, true);
        assertTrue(tBlock != tCobble && tCobble.variant == StoneVariant.COBBL,
                "two variants of one stone are two distinct block instances");
    }

    /** The oredict equivalence face: the merged OM.reg_ sets (BlockStones.java:135-194), OP-order keys. */
    @Test
    void oreDictFaceIsTheMergedRegisterOreSurface() {
        Map<OreDictPrefix, List<StoneVariant>> tFace = GTStoneBlocks.oreDictMappings();
        // keys in OP.java:456-465 declaration order
        List<OreDictPrefix> tKeys = List.of(OP.stoneCobble, OP.stoneSmooth, OP.stoneMossyBricks, OP.stoneMossy,
                OP.stoneBricks, OP.stoneCracked, OP.stoneChiseled, OP.stonePolished, OP.stone, OP.cobblestone);
        assertEquals(tKeys, List.copyOf(tFace.keySet()), "oreDictMappings keys follow the OP.java:456-465 order");
        assertEquals(List.of(StoneVariant.STONE, StoneVariant.COBBL, StoneVariant.MCOBL, StoneVariant.BRICK,
                StoneVariant.CRACK, StoneVariant.MBRIK, StoneVariant.CHISL, StoneVariant.SMOTH,
                StoneVariant.TILES, StoneVariant.STILE, StoneVariant.SBRIK, StoneVariant.WINDA,
                StoneVariant.WINDB, StoneVariant.QBRIK), tFace.get(OP.stone),
                "OP.stone takes 14 variants — RNFBR/RSTBR excluded upstream (:142-155)");
        assertEquals(List.of(StoneVariant.CHISL), tFace.get(OP.stoneChiseled),
                "OP.stoneChiseled -> CHISL exactly (:163/:183) — the p19-chisel-recipes anchor");
        assertEquals(List.of(StoneVariant.MCOBL, StoneVariant.MBRIK), tFace.get(OP.stoneMossy),
                "OP.stoneMossy union (:159/:176/:181)");
        assertEquals(List.of(StoneVariant.BRICK, StoneVariant.CRACK, StoneVariant.MBRIK, StoneVariant.CHISL,
                StoneVariant.TILES, StoneVariant.STILE, StoneVariant.SBRIK, StoneVariant.WINDA,
                StoneVariant.WINDB, StoneVariant.QBRIK), tFace.get(OP.stoneBricks),
                "OP.stoneBricks union (:160/:162/:165-170/:177-191)");
    }

    /**
     * The composed-name face (task p20-i18n-compose-rows, unchanged by p21): the 16 variant
     * template keys (one per StoneVariant, the stone name riding the %s slot as the
     * gt6.material small unit), and each block composes ITS OWN variant's template —
     * variant-0 blocks compose the exact same template the P19 single-block form composed
     * (zero lang keys added or retired).
     */
    @Test
    void variantTemplatesCoverTheComposedNameFace() {
        Set<String> tKeys = new HashSet<>();
        for (StoneVariant tVariant : StoneVariant.VALUES) tKeys.add(tVariant.key());
        assertEquals(16, tKeys.size(), "16 distinct variant template keys");
        assertTrue(tKeys.contains("gt6.stone.variant.bricks_chiseled"));
        assertTrue(tKeys.contains("gt6.stone.variant.square_bricks"));
        for (StoneVariant tVariant : StoneVariant.VALUES) {
            assertTrue(tVariant.key().startsWith("gt6.stone.variant."),
                    "the template-key namespace is uniform: " + tVariant.key());
        }
        // the block face composes its OWN variant template over the gt6.material small unit;
        // the compose CONTRACT is pinned on the contents (a bare JVM has no lang tables —
        // the full-expansion pin lives in GT6LangParityTest over the recorded faces)
        GTStoneBlock tGranite = new GTStoneBlock("granite_black", StoneVariant.STONE, MT.STONES.GraniteBlack,
                3.00F, 6.00F, 3, true);
        net.minecraft.network.chat.contents.TranslatableContents tContents =
                (net.minecraft.network.chat.contents.TranslatableContents) tGranite.getName().getContents();
        assertEquals(StoneVariant.STONE.key(), tContents.getKey(),
                "the variant-0 block composes the variant-0 template (the P19 name face, unchanged)");
        assertEquals(1, tContents.getArgs().length, "one slot: the gt6.material small unit");
        net.minecraft.network.chat.contents.TranslatableContents tStoneSlot =
                (net.minecraft.network.chat.contents.TranslatableContents)
                        ((net.minecraft.network.chat.Component) tContents.getArgs()[0]).getContents();
        assertEquals("gt6.material.granite_black", tStoneSlot.getKey(),
                "the stone-name slot is the material small-unit component");
        // a non-zero variant block composes its own template — the per-pair name identity
        GTStoneBlock tChiseled = new GTStoneBlock("granite_black", StoneVariant.CHISL, MT.STONES.GraniteBlack,
                3.00F, 6.00F, 3, true);
        assertEquals(StoneVariant.CHISL.key(),
                ((net.minecraft.network.chat.contents.TranslatableContents) tChiseled.getName().getContents()).getKey(),
                "the chiseled block composes the chiseled template — same 16 keys, no new lang face");
    }

    /** The texture segments recover the upstream icon names (the render card's PNG borrow paths). */
    @Test
    void iconSegmentsAreTheUpstreamIconNames() {
        assertEquals("STONE", StoneVariant.STONE.iconSegment());
        assertEquals("BRICKS_CHISELED", StoneVariant.CHISL.iconSegment());
        assertEquals("WINDMILL_TILES_A", StoneVariant.WINDA.iconSegment());
        assertEquals("SQUARE_BRICKS", StoneVariant.QBRIK.iconSegment());
    }
}
