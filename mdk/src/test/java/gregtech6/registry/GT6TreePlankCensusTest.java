/*
 * Tests for task gt-tree-planks + planks-blockification: the 17-per-pair GT6 plank universe —
 * the census/attribute/audit unit in the GT6TreeBlocksCensusTest posture (offline-safe by
 * construction: enum walks, string paths, direct Block construction under the vanilla
 * bootstrap bracket, and classpath reads of the generated tree; no RegisterEvent, no
 * bootstrapped built-in registries).
 *
 * <p>Upstream anchors: Loader_Woods.java:62-65 (the Planks/Planks2 block rows),
 * BlockTreePlanks.java:38-63 (the 16-meta block, LH rows :40-55, the OD.plankWood walk
 * :59-62 skipping the treated row, the hardness split :90), BlockTreePlanks2.java:43-45
 * (the Blue Spruce row), BlockBasePlanksFlammable.java:43-45 (flammability 20 / spread 5 /
 * not a fire source), BlockMetaType.java:61-62 (hardness 1.5x1 / resistance 10x1),
 * LoaderWoodDictionary.java:69-113 (the 9 species rows) + :157-172 (the standalone generic
 * rows: Compressed/Wood/Treated/Crate/Dead/Rotten/Mossy/Frozen).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.tree.GT6PlankBlock;
import gregtech6.block.tree.GT6TreeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

class GT6TreePlankCensusTest {

    /** The KINDS order = the upstream plank meta order (rubber = Planks:0 .. rainbowood = Planks:7, blue_spruce = Planks2:0). */
    private static final String[] META_ORDER = {
            "rubber", "maple", "willow", "blue_mahoe", "hazel", "cinnamon", "coconut", "rainbowood", "blue_spruce"};

    /** The upstream plank LH words (BlockTreePlanks.java:40-47 + BlockTreePlanks2.java:45) minus the " Planks" suffix. */
    private static final String[] EN_WORDS = {
            "Rubberwood", "Maple", "Willow", "Blue Mahoe", "Hazel", "Cinnamon", "Coconut", "Rainbowood", "Blue Spruce"};

    /** The 8 generic rows in upstream meta order (BlockTreePlanks.java:48-55, GT6TreeBlocks.GENERIC_PLANK_ROWS): path = upstream word. */
    private static final String[][] GENERIC_ROWS = {
            {"plank_wood_compressed", "Compressed Wood Planks"}, // meta 8
            {"plank_wood",            "Wood Planks"},            // meta 9 = IL.Plank
            {"plank_wood_treated",    "Treated Planks"},         // meta 10
            {"crate",                 "Crate"},                  // meta 11
            {"plank_wood_dead",       "Dead Planks"},            // meta 12
            {"plank_wood_rotten",     "Rotten Planks"},          // meta 13
            {"plank_wood_mossy",      "Mossy Planks"},           // meta 14
            {"plank_wood_frozen",     "Frozen Planks"}};         // meta 15

    /** Direct constructions (offline-safe — RegistryObject.get() needs the live registry). */
    private static List<GT6PlankBlock> plankBlocks() {
        List<GT6PlankBlock> rBlocks = new ArrayList<>();
        for (int i = 0; i < 9; i++) rBlocks.add(new GT6PlankBlock(GT6PlankBlock.HARDNESS));
        for (gregtech6.registry.GT6TreeBlocks.GenericPlank tRow : gregtech6.registry.GT6TreeBlocks.GENERIC_PLANK_ROWS) {
            rBlocks.add(new GT6PlankBlock(tRow.hardness()));
        }
        return rBlocks;
    }

    @BeforeAll
    static void boot() {
        // the vanilla bootstrap bracket (the GT6TreeBlocksCensusTest recipe: version detect
        // must precede bootStrap — a bare-JVM first boot poisons DataFixers for later suites)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
        // the plankBlocks() fixture builds real Blocks and a Block CONSTRUCTION folds the
        // state into the built-in block registry (NamespacedWrapper.createIntrusiveHolder),
        // so the frozen post-bootStrap registry must be reopened — the GT6PortalBlockProperties
        // Test/GT6JuicerRegistrationTest bracket form (the r8-hotfix b679b2345 precedent);
        // without it both attribute walks die "Registry is already frozen" even single-class
        try {
            java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getClass().getMethod("unfreeze");
            tUnfreeze.setAccessible(true);
            tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        } catch (Exception aE) {
            throw new IllegalStateException("could not unfreeze the offline block registry", aE);
        }
    }

    /** The census: 17 plank blocks — 9 species ids {@code <snake>_planks} + 8 generic row paths, all distinct. */
    @Test
    void theSeventeenPlankIdsMirrorTheUpstreamRows() {
        assertEquals(17, GT6TreeBlocks.PLANKS.size() + GT6TreeBlocks.GENERIC_PLANKS.size(),
                "17 plank blocks (the BlockTreePlanks 16 metas + the BlockTreePlanks2 meta 0)");
        assertEquals(9, GT6TreeBlocks.PLANK_ITEMS.size(), "9 species plank block items");
        assertEquals(8, GT6TreeBlocks.GENERIC_PLANK_ITEMS.size(), "8 generic plank block items");
        Set<String> tPaths = new HashSet<>();
        for (int i = 0; i < 9; i++) {
            String tPath = gregtech6.registry.GT6TreeBlocks.path(GT6TreeBlocks.KINDS.get(i), "_planks");
            assertTrue(tPaths.add(tPath), "distinct ids, dup at " + tPath);
            assertEquals(tPath, GT6TreeBlocks.PLANKS.get(i).getId().getPath(),
                    "PLANKS row " + i + " rides the single-source path");
            assertEquals(tPath, GT6TreeBlocks.PLANK_ITEMS.get(i).getId().getPath(),
                    "PLANK_ITEMS row " + i + " shares the block id");
        }
        for (int i = 0; i < 8; i++) {
            String tPath = GENERIC_ROWS[i][0];
            assertTrue(tPaths.add(tPath), "distinct ids, dup at " + tPath);
            assertEquals(tPath, GT6TreeBlocks.GENERIC_PLANKS.get(i).getId().getPath(),
                    "GENERIC_PLANKS row " + i + " rides the upstream meta order");
            assertEquals(tPath, GT6TreeBlocks.GENERIC_PLANK_ITEMS.get(i).getId().getPath(),
                    "GENERIC_PLANK_ITEMS row " + i + " shares the block id");
        }
    }

    /** The meta alignment: kind order == the upstream meta order (LoaderWoodDictionary.java:69-113) and the generic rows ride metas 8-15 (BlockTreePlanks.java:48-55). */
    @Test
    void rowOrderIsTheUpstreamPlankMetaOrder() {
        for (int i = 0; i < META_ORDER.length; i++) {
            assertEquals(META_ORDER[i], GT6TreeBlocks.KINDS.get(i).snake(),
                    "kind " + i + " = the upstream plank meta " + i + " row (Planks:0-7 + Planks2:0)");
        }
        for (int i = 0; i < GENERIC_ROWS.length; i++) {
            assertEquals(GENERIC_ROWS[i][0], GT6TreeBlocks.GENERIC_PLANK_ROWS.get(i).path(),
                    "generic row " + i + " = the BlockTreePlanks meta " + (i + 8) + " row");
            assertEquals(GENERIC_ROWS[i][1], GT6TreeBlocks.GENERIC_PLANK_ROWS.get(i).enName(),
                    "generic row " + i + " carries the upstream LH word verbatim (BlockTreePlanks.java:" + (48 + i) + ")");
        }
    }

    /** The en display words are the upstream LH rows verbatim (BlockTreePlanks.java:40-47 / BlockTreePlanks2.java:45). */
    @Test
    void plankEnNamesAreTheUpstreamLhRows() {
        for (int i = 0; i < EN_WORDS.length; i++) {
            assertEquals(EN_WORDS[i], GT6TreeBlocks.KINDS.get(i).plankEnName(),
                    "plank word " + i + " (the rubber row is Rubberwood, not Rubber)");
        }
    }

    /** The attribute verbatim: hardness 1.5 (metas 0-11) / 0.75 (the :90 soft split) / resistance 10.0 / wood sound (BlockMetaType.java:61-62). */
    @Test
    void blockPropertiesAreTheUpstreamMetaTypeRow() {
        List<GT6PlankBlock> tBlocks = plankBlocks();
        for (int i = 0; i < tBlocks.size(); i++) {
            GT6PlankBlock tPlank = tBlocks.get(i);
            float tExpected = i < 13 ? GT6PlankBlock.HARDNESS : GT6PlankBlock.HARDNESS_SOFT; // metas 12-15 = list rows 13-16
            BlockState tState = tPlank.defaultBlockState();
            assertEquals(tExpected, tState.getDestroySpeed(null, BlockPos.ZERO),
                    "plank row " + i + " hardness = the getBlockHardness split (BlockTreePlanks.java:90)");
            assertEquals(10.0F, tPlank.getExplosionResistance(), "resistance = multiplier(1) * 10.0F");
            assertEquals(SoundType.WOOD, tState.getSoundType(), "soundTypeWood");
        }
    }

    /** The fire face verbatim: flammability 20 / spread 5 / not a fire source (BlockBasePlanksFlammable.java:43-45, every meta including treated). */
    @Test
    void fireFaceIsThePlanksFlammableRow() {
        for (GT6PlankBlock tPlank : plankBlocks()) {
            BlockState tState = tPlank.defaultBlockState();
            assertEquals(GT6PlankBlock.FLAMMABILITY, tPlank.getFlammability(tState, null, BlockPos.ZERO, Direction.UP),
                    "the :44 row");
            assertEquals(GT6PlankBlock.FIRE_SPREAD_SPEED, tPlank.getFireSpreadSpeed(tState, null, BlockPos.ZERO, Direction.UP),
                    "the :45 row");
            assertTrue(!tPlank.isFireSource(tState, null, BlockPos.ZERO, Direction.UP),
                    "the :43 row");
        }
    }

    /**
     * The borrow audit: every landed plank texture sha256 matches its
     * assets/README.md ledger row (the GT6TextureCensusTest ground-truth shape — machine
     * independent, the ledger IS the upstream-source record). The 9 species files ride the
     * {@code planks_<snake>} names, the 8 generic files the block-path names.
     */
    @Test
    void borrowedTexturesMatchTheReadmeLedger() throws Exception {
        String tReadme;
        try (InputStream tStream = GT6TreePlankCensusTest.class.getResourceAsStream("/assets/README.md")) {
            assertNotNull(tStream, "assets/README.md on the test classpath");
            tReadme = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        Pattern tDigest = Pattern.compile("sha256\\s+`([0-9a-f]{64})`");
        List<String> tRefs = new ArrayList<>();
        for (String tSnake : META_ORDER) tRefs.add("planks_" + tSnake + ".png");
        for (String[] tRow : GENERIC_ROWS) tRefs.add(tRow[0] + ".png");
        MessageDigest tSha = MessageDigest.getInstance("SHA-256");
        for (String tRef : tRefs) {
            int tAt = tReadme.indexOf(tRef);
            assertTrue(tAt >= 0, "README ledger row for " + tRef);
            String tTail = tReadme.substring(tAt, Math.min(tReadme.length(), tAt + 400));
            Matcher tMatch = tDigest.matcher(tTail);
            assertTrue(tMatch.find(), "sha256 in the " + tRef + " ledger row");
            byte[] tBytes;
            try (InputStream tStream = GT6TreePlankCensusTest.class.getResourceAsStream("/assets/gt6/textures/block/tree/" + tRef)) {
                assertNotNull(tStream, tRef + " on the classpath");
                tBytes = tStream.readAllBytes();
            }
            StringBuilder tHex = new StringBuilder();
            for (byte b : tSha.digest(tBytes)) tHex.append(String.format("%02x", b));
            assertEquals(tMatch.group(1), tHex.toString(), tRef + " byte-identical to the ledger");
        }
    }

    /** The generated blockstate: the cube_all single-variant form per row (the addPlanks band, 17 rows). */
    @Test
    void generatedBlockstatesAreCubeAll() throws Exception {
        for (String tSnake : META_ORDER) {
            String tJson = classpathText("assets/gt6/blockstates/" + tSnake + "_planks.json");
            assertTrue(tJson.contains("\"gt6:block/" + tSnake + "_planks\""),
                    tSnake + ": the blockstate points at the cube_all model");
            String tModel = classpathText("assets/gt6/models/block/" + tSnake + "_planks.json");
            assertTrue(tModel.contains("minecraft:block/cube_all"), tSnake + ": the cube_all parent");
            assertTrue(tModel.contains("gt6:block/tree/planks_" + tSnake), tSnake + ": the borrowed texture face");
            String tItem = classpathText("assets/gt6/models/item/" + tSnake + "_planks.json");
            assertTrue(tItem.contains("\"gt6:block/" + tSnake + "_planks\""), tSnake + ": the item model parent");
        }
        for (String[] tRow : GENERIC_ROWS) {
            String tPath = tRow[0];
            String tJson = classpathText("assets/gt6/blockstates/" + tPath + ".json");
            assertTrue(tJson.contains("\"gt6:block/" + tPath + "\""), tPath + ": the blockstate points at the cube_all model");
            String tModel = classpathText("assets/gt6/models/block/" + tPath + ".json");
            assertTrue(tModel.contains("minecraft:block/cube_all"), tPath + ": the cube_all parent");
            assertTrue(tModel.contains("gt6:block/tree/" + tPath), tPath + ": the borrowed texture face");
            String tItem = classpathText("assets/gt6/models/item/" + tPath + ".json");
            assertTrue(tItem.contains("\"gt6:block/" + tPath + "\""), tPath + ": the item model parent");
        }
    }

    /** The generated loot + tags: dropSelf per plank; #minecraft:planks carries all but the treated row (the OD.plankWood skip verbatim), mineable/axe carries all 17. */
    @Test
    void generatedLootAndTagsCarryTheSeventeenPlanks() throws Exception {
        List<String> tIds = new ArrayList<>();
        for (String tSnake : META_ORDER) tIds.add("gt6:" + tSnake + "_planks");
        for (String[] tRow : GENERIC_ROWS) tIds.add("gt6:" + tRow[0]);
        for (String tId : tIds) {
            String tPath = tId.substring("gt6:".length());
            String tLoot = classpathText("data/gt6/loot_tables/blocks/" + tPath + ".json");
            assertTrue(tLoot.contains("\"" + tId + "\""), tId + ": the dropSelf loot row");
        }
        String tBlockPlanks = classpathText("data/minecraft/tags/blocks/planks.json");
        String tItemPlanks = classpathText("data/minecraft/tags/items/planks.json");
        for (String tId : tIds) {
            if (tId.equals("gt6:plank_wood_treated")) continue; // the `if (i != 10)` skip, BlockTreePlanks.java:60
            assertTrue(tBlockPlanks.contains(tId), tId + " joins #minecraft:planks (block side)");
            assertTrue(tItemPlanks.contains(tId), tId + " joins #minecraft:planks (item side)");
        }
        assertFalse(tBlockPlanks.contains("gt6:plank_wood_treated"), "the treated row stays OFF the planks tag (the :60 verbatim)");
        String tAxe = classpathText("data/minecraft/tags/blocks/mineable/axe.json");
        for (String tId : tIds) assertTrue(tAxe.contains(tId), tId + " joins mineable/axe (the aWooden face)");
    }

    /** The lang face: en + zh carry all 17 plank keys — en the LH rows verbatim, zh the dump-verbatim hand rows (gt.block.planks.0-15). */
    @Test
    void langCarriesTheSeventeenPlankKeys() throws Exception {
        String tEn = classpathText("assets/gt6/lang/en_us.json");
        String tZh = classpathText("assets/gt6/lang/zh_cn.json");
        for (int i = 0; i < META_ORDER.length; i++) {
            String tKey = "block.gt6." + META_ORDER[i] + "_planks";
            assertTrue(tEn.contains("\"" + tKey + "\": \"" + EN_WORDS[i] + " Planks\""), tKey + " en verbatim");
        }
        String[][] tZhWords = {
                {"plank_wood_compressed", "压缩木木板"}, {"plank_wood", "木木板"}, {"plank_wood_treated", "防腐木木板"},
                {"crate", "板条箱木板"}, {"plank_wood_dead", "枯死的木木板"}, {"plank_wood_rotten", "腐败的木木板"},
                {"plank_wood_mossy", "苔痕木木板"}, {"plank_wood_frozen", "结冰的木木板"}};
        for (String[] tRow : tZhWords) {
            String tKey = "block.gt6." + tRow[0];
            assertTrue(tZh.contains("\"" + tKey + "\": \"" + tRow[1] + "\""), tKey + " zh dump-verbatim row");
            assertTrue(tEn.contains("\"" + tKey + "\": \"" + GENERIC_ROWS[java.util.Arrays.asList(tZhWords).indexOf(tRow)][1] + "\""),
                    tKey + " en LH verbatim");
        }
        assertTrue(tZh.contains("\"block.gt6.rubber_planks\": \"橡胶树木板\""), "the zh hand row sample");
        assertTrue(tZh.contains("\"block.gt6.blue_spruce_planks\": \"蓝云杉木板\""), "the zh hand row tail");
    }

    private static String classpathText(String aPath) throws Exception {
        try (InputStream tStream = GT6TreePlankCensusTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, aPath + " on the test classpath (the generated tree)");
            return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
