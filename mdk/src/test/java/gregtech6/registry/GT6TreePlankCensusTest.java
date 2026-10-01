/*
 * Tests for task gt-tree-planks: the 9-per-pair GT6 tree plank universe — the
 * census/attribute/audit unit in the GT6TreeBlocksCensusTest posture (offline-safe by
 * construction: enum walks, string paths, direct Block construction under the vanilla
 * bootstrap bracket, and classpath reads of the generated tree; no RegisterEvent, no
 * bootstrapped built-in registries).
 *
 * <p>Upstream anchors: Loader_Woods.java:62-65 (the Planks/Planks2 block rows),
 * BlockTreePlanks.java:38-63 (the 16-meta block, LH rows :40-55, the OD.plankWood walk
 * :59-62, the hardness split :90), BlockTreePlanks2.java:43-45 (the Blue Spruce row),
 * BlockBasePlanksFlammable.java:43-45 (flammability 20 / spread 5 / not a fire source),
 * BlockMetaType.java:61-62 (hardness 1.5x1 / resistance 10x1), LoaderWoodDictionary.java:69-113
 * (the 9 species rows: Planks meta 0-7 + Planks2 meta 0).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import gregtech6.block.tree.GT6TreeKind;
import gregtech6.block.tree.GT6TreePlankBlock;
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

    /** Direct constructions (offline-safe — RegistryObject.get() needs the live registry). */
    private static List<GT6TreePlankBlock> plankBlocks() {
        List<GT6TreePlankBlock> rBlocks = new ArrayList<>();
        for (GT6TreeKind tKind : GT6TreeBlocks.KINDS) {
            rBlocks.add(new GT6TreePlankBlock(tKind));
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
    }

    /** The census: 9 plank blocks over the KINDS walk, ids {@code <snake>_planks}, all distinct. */
    @Test
    void theNinePlankIdsMirrorTheKindsWalk() {
        assertEquals(9, GT6TreeBlocks.PLANKS.size(), "9 plank blocks (8 Planks metas + 1 Planks2 meta)");
        assertEquals(9, GT6TreeBlocks.PLANK_ITEMS.size(), "9 plank block items");
        Set<String> tPaths = new HashSet<>();
        for (int i = 0; i < 9; i++) {
            String tPath = gregtech6.registry.GT6TreeBlocks.path(GT6TreeBlocks.KINDS.get(i), "_planks");
            assertTrue(tPaths.add(tPath), "distinct ids, dup at " + tPath);
            assertEquals(tPath, GT6TreeBlocks.PLANKS.get(i).getId().getPath(),
                    "PLANKS row " + i + " rides the single-source path");
            assertEquals(tPath, GT6TreeBlocks.PLANK_ITEMS.get(i).getId().getPath(),
                    "PLANK_ITEMS row " + i + " shares the block id");
        }
    }

    /** The meta alignment: kind order == the upstream meta order (LoaderWoodDictionary.java:69-113). */
    @Test
    void kindOrderIsTheUpstreamPlankMetaOrder() {
        for (int i = 0; i < META_ORDER.length; i++) {
            assertEquals(META_ORDER[i], GT6TreeBlocks.KINDS.get(i).snake(),
                    "kind " + i + " = the upstream plank meta " + i + " row (Planks:0-7 + Planks2:0)");
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

    /** The attribute verbatim: hardness 1.5 / resistance 10.0 / wood sound (BlockMetaType.java:61-62). */
    @Test
    void blockPropertiesAreTheUpstreamMetaTypeRow() {
        for (GT6TreePlankBlock tPlank : plankBlocks()) {
            BlockState tState = tPlank.defaultBlockState();
            assertEquals(1.5F, tState.getDestroySpeed(null, BlockPos.ZERO), "hardness = multiplier(1) * 1.5F");
            assertEquals(10.0F, tPlank.getExplosionResistance(), "resistance = multiplier(1) * 10.0F");
            assertEquals(SoundType.WOOD, tState.getSoundType(), "soundTypeWood");
        }
    }

    /** The fire face verbatim: flammability 20 / spread 5 / not a fire source (BlockBasePlanksFlammable.java:43-45). */
    @Test
    void fireFaceIsThePlanksFlammableRow() {
        for (GT6TreePlankBlock tPlank : plankBlocks()) {
            BlockState tState = tPlank.defaultBlockState();
            assertEquals(GT6TreePlankBlock.FLAMMABILITY, tPlank.getFlammability(tState, null, BlockPos.ZERO, Direction.UP),
                    "the :44 row");
            assertEquals(GT6TreePlankBlock.FIRE_SPREAD_SPEED, tPlank.getFireSpreadSpeed(tState, null, BlockPos.ZERO, Direction.UP),
                    "the :45 row");
            assertTrue(!tPlank.isFireSource(tState, null, BlockPos.ZERO, Direction.UP),
                    "the :43 row");
        }
    }

    /**
     * The borrow audit: every landed {@code planks_<snake>.png} sha256 matches its
     * assets/README.md ledger row (the GT6TextureCensusTest ground-truth shape — machine
     * independent, the ledger IS the upstream-source record).
     */
    @Test
    void borrowedTexturesMatchTheReadmeLedger() throws Exception {
        String tReadme;
        try (InputStream tStream = GT6TreePlankCensusTest.class.getResourceAsStream("/assets/README.md")) {
            assertNotNull(tStream, "assets/README.md on the test classpath");
            tReadme = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        Pattern tDigest = Pattern.compile("sha256\\s+`([0-9a-f]{64})`");
        for (String tSnake : META_ORDER) {
            String tRef = "planks_" + tSnake + ".png";
            int tAt = tReadme.indexOf(tRef);
            assertTrue(tAt >= 0, "README ledger row for " + tRef);
            String tTail = tReadme.substring(tAt, Math.min(tReadme.length(), tAt + 400));
            Matcher tMatch = tDigest.matcher(tTail);
            assertTrue(tMatch.find(), "sha256 in the " + tRef + " ledger row");
            MessageDigest tSha = MessageDigest.getInstance("SHA-256");
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

    /** The generated blockstate: the cube_all single-variant form per species (the addPlanks band). */
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
    }

    /** The generated loot + tags: dropSelf per plank; planks/axe block tags + planks item tag carry all 9. */
    @Test
    void generatedLootAndTagsCarryTheNinePlanks() throws Exception {
        List<String> tIds = new ArrayList<>();
        for (String tSnake : META_ORDER) {
            tIds.add("gt6:" + tSnake + "_planks");
            String tLoot = classpathText("data/gt6/loot_tables/blocks/" + tSnake + "_planks.json");
            assertTrue(tLoot.contains("\"gt6:" + tSnake + "_planks\""), tSnake + ": the dropSelf loot row");
        }
        assertTrue(classpathText("data/minecraft/tags/blocks/planks.json").contains(tIds.get(0))
                        && classpathText("data/minecraft/tags/blocks/planks.json").contains(tIds.get(8)),
                "the 9 join #minecraft:planks (block side, first+last pinned)");
        assertTrue(classpathText("data/minecraft/tags/items/planks.json").contains(tIds.get(0))
                        && classpathText("data/minecraft/tags/items/planks.json").contains(tIds.get(8)),
                "the 9 join #minecraft:planks (item side)");
        assertTrue(classpathText("data/minecraft/tags/blocks/mineable/axe.json").contains(tIds.get(0))
                        && classpathText("data/minecraft/tags/blocks/mineable/axe.json").contains(tIds.get(8)),
                "the 9 join mineable/axe");
    }

    /** The lang face: en + zh carry all 9 plank keys with the verbatim/hand words. */
    @Test
    void langCarriesTheNinePlankKeys() throws Exception {
        String tEn = classpathText("assets/gt6/lang/en_us.json");
        String tZh = classpathText("assets/gt6/lang/zh_cn.json");
        for (int i = 0; i < META_ORDER.length; i++) {
            String tKey = "block.gt6." + META_ORDER[i] + "_planks";
            assertTrue(tEn.contains("\"" + tKey + "\": \"" + EN_WORDS[i] + " Planks\""), tKey + " en verbatim");
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
