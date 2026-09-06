/**
 * Tests for task p19-stoneblocks-registry: the GT6 stone universe census + the four
 * mapping-table transcriptions + the Loader_Rocks parameter rows + the oredict
 * equivalence face.
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

    /** The 272-pair census (17 x 16) — this card's and the render card's yardstick. */
    private static final int PINNED_TOTAL = 272;

    @BeforeAll
    static void initMaterialSystem() {
        // GTMaterialBlocksRegistrationTest shape: the material system must exist before
        // any MT.STONES/OP field is dereferenced (GTMaterialItems.initMaterials()).
        GTMaterialItems.initMaterials();
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

    /** The block: 16-state property, default STONE, the BlockMetaType.java:61-62 strength conversion. */
    @Test
    void blockCarriesThe16VariantPropertyAndUpstreamStrength() {
        assertEquals(16, GTStoneBlock.VARIANT.getPossibleValues().size());
        assertSame(StoneVariant.STONE, GTStoneBlock.VARIANT.getValue("stone").orElse(null),
                "the serialized name round-trips to the plain stone variant");
        GTStoneBlock tBlock = new GTStoneBlock("granite_black", MT.STONES.GraniteBlack, 3.00F, 6.00F, 3, true);
        assertSame(StoneVariant.STONE, tBlock.defaultBlockState().getValue(GTStoneBlock.VARIANT),
                "the default state is the plain stone variant");
        // the shared-instance discipline (ADR-P16-2) holds by construction — VARIANT is the
        // single static constant the whole family reads; every block defaults to the plain stone
        GTStoneBlock tShale = new GTStoneBlock("shale", MT.STONES.Shale, 0.50F, 0.75F, 0, false);
        assertSame(StoneVariant.STONE, tShale.defaultBlockState().getValue(GTStoneBlock.VARIANT));
        // the impl ignores both args (BlockBehaviour.java:566-568 returns the field) — nulls are the offline form
        assertEquals(4.5F, tBlock.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "hardness = 3.00 * 1.5 (BlockMetaType.java:61)");
        assertEquals(60.0F, tBlock.getExplosionResistance(), 1.0e-6F,
                "resistance = 6.00 * 10 (BlockMetaType.java:62, Block.java:331)");
        assertTrue(tBlock.witherProof && tBlock.harvestLevel == 3, "the granite row data rides the block");
        assertEquals("block.gt6.granite_black.stone", tBlock.getDescriptionId(),
                "the description id is the variant-0 lang key (offline-safe: no registry lookup)");
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

    /** The 272 registration pairs resolve one lang key each, and the compose() table is the upstream lang block. */
    @Test
    void variantKeysCoverTheLangTable() {
        Set<String> tKeys = new HashSet<>();
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            tKeys.add("block.gt6." + tKey.stone().snake() + "." + tKey.variant().snake);
        }
        assertEquals(PINNED_TOTAL, tKeys.size(), "272 distinct lang keys");
        assertTrue(tKeys.contains("block.gt6.granite_black.bricks_chiseled"));
        assertTrue(tKeys.contains("block.gt6.shale.square_bricks"));
        // BlockStones.java:117-132, the aDefaultLocalised = "Black Granite" column
        assertEquals("Black Granite", StoneVariant.STONE.compose("Black Granite"));
        assertEquals("Black Granite Cobblestone", StoneVariant.COBBL.compose("Black Granite"));
        assertEquals("Mossy Black Granite Cobblestone", StoneVariant.MCOBL.compose("Black Granite"));
        assertEquals("Black Granite Bricks", StoneVariant.BRICK.compose("Black Granite"));
        assertEquals("Cracked Black Granite Bricks", StoneVariant.CRACK.compose("Black Granite"));
        assertEquals("Mossy Black Granite Bricks", StoneVariant.MBRIK.compose("Black Granite"));
        assertEquals("Chiseled Black Granite", StoneVariant.CHISL.compose("Black Granite"));
        assertEquals("Smooth Black Granite", StoneVariant.SMOTH.compose("Black Granite"));
        assertEquals("Reinforced Black Granite Bricks", StoneVariant.RNFBR.compose("Black Granite"));
        assertEquals("Redstoned Black Granite Bricks", StoneVariant.RSTBR.compose("Black Granite"));
        assertEquals("Black Granite Tiles", StoneVariant.TILES.compose("Black Granite"));
        assertEquals("Small Black Granite Tiles", StoneVariant.STILE.compose("Black Granite"));
        assertEquals("Small Black Granite Bricks", StoneVariant.SBRIK.compose("Black Granite"));
        assertEquals("Black Granite Windmill Tiles A", StoneVariant.WINDA.compose("Black Granite"));
        assertEquals("Black Granite Windmill Tiles B", StoneVariant.WINDB.compose("Black Granite"));
        assertEquals("Black Granite Square Bricks", StoneVariant.QBRIK.compose("Black Granite"));
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
