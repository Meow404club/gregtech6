/**
 * Tests for task p30-w6-t1-trees-nine commit 1: the 27-per-pair tree block universe —
 * the census/audit unit in the GTStoneBlocksRegistrationTest posture (offline-safe by
 * construction: only enum walks, string paths and ResourceKey interns; no RegisterEvent,
 * no bootstrapped registries, no BlockState construction).
 *
 * <p>Upstream anchors: Loader_Woods.java:67-70 (Saplings_AB/CD + Leaves_AB/CD registration
 * rows), BlockTreeSaplingAB.java:47-54 + BlockTreeSaplingCD.java:45 (the meta 0-7 + CD:0
 * display-name rows = the KINDS order), BlockTreeLeavesAB.java:47-54 (the leaves names),
 * Loader_Worldgen.java:608-616 (the 9 WorldgenTree rows).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.tree.GT6TreeKind;
import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.worldgen.GT6Worldgen;

class GT6TreeBlocksCensusTest {

    @BeforeAll
    static void boot() {
        // the vanilla bootstrap bracket for the ResourceKey/registry-key classes
        // (the GT6WorldgenDatagenTest posture; offline throwables ignored)
        // the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** The 9 species, upstream Saplings_AB meta 0-7 + Saplings_CD meta 0 order. */
    @Test
    void kindsOrderIsTheUpstreamMetaOrder() {
        assertEquals(9, GT6TreeBlocks.KINDS.size(), "9 worldgen trees (research.p29 census: Saplings_AB 0-7 + CD:0)");
        assertEquals("rubber", GT6TreeBlocks.KINDS.get(0).snake(), "meta 0 = Rubber (BlockTreeSaplingAB.java:47)");
        assertEquals("blue_mahoe", GT6TreeBlocks.KINDS.get(3).snake(), "meta 3 = Blue MahoE (:50)");
        assertEquals("coconut", GT6TreeBlocks.KINDS.get(6).snake(), "meta 6 = Coconut (:53) — the sand-grower");
        assertEquals("blue_spruce", GT6TreeBlocks.KINDS.get(8).snake(), "CD meta 0 = Blue Spruce (BlockTreeSaplingCD.java:45)");
        Set<String> tSnakes = new HashSet<>();
        for (GT6TreeKind tKind : GT6TreeBlocks.KINDS) {
            assertTrue(tSnakes.add(tKind.snake()), "snake ids must be distinct, dup at " + tKind.snake());
        }
    }

    /** The coconut sand-planting exception is the ONLY one (BlockTreeSaplingAB.java:74-76). */
    @Test
    void onlyCoconutGrowsOnSand() {
        for (GT6TreeKind tKind : GT6TreeBlocks.KINDS) {
            assertEquals(tKind == GT6TreeKind.COCONUT, tKind.canGrowOnSand(),
                    "canGrowOnSand must be coconut-exclusive, got " + tKind.snake());
        }
    }

    /** The 27-id scheme: {@code <snake>_sapling/_log/_leaves}, all distinct (GTGrassBlocks single-source rule). */
    @Test
    void pathsAre27DistinctIds() {
        Set<String> tPaths = new HashSet<>();
        for (GT6TreeKind tKind : GT6TreeBlocks.KINDS) {
            tPaths.add(GT6TreeBlocks.path(tKind, "_sapling"));
            tPaths.add(GT6TreeBlocks.path(tKind, "_log"));
            tPaths.add(GT6TreeBlocks.path(tKind, "_leaves"));
        }
        assertEquals(27, tPaths.size(), "27 distinct block ids = 9 saplings + 9 logs + 9 leaves");
    }

    /** The worldgen key band mirrors the KINDS order (the sapling grower reads treeConfiguredKey). */
    @Test
    void treeWorldgenKeysMirrorKinds() {
        assertEquals(9, GT6Worldgen.TREE_CONFIGURED_KEYS.size(), "9 configured keys");
        assertEquals(9, GT6Worldgen.TREE_PLACED_KEYS.size(), "9 placed keys");
        assertEquals("gt6:tree_rubber", GT6Worldgen.TREE_CONFIGURED_KEYS.get(0).location().toString(),
                "the Loader_Worldgen tree.rubber name, dot flattened");
        assertEquals("gt6:tree_blue_spruce", GT6Worldgen.TREE_PLACED_KEYS.get(8).location().toString(),
                "placed shares the configured path (the GTCEu blob form)");
        for (int i = 0; i < 9; i++) {
            assertEquals(GT6Worldgen.treeConfiguredKey(GT6TreeBlocks.KINDS.get(i).snake()),
                    GT6Worldgen.TREE_CONFIGURED_KEYS.get(i),
                    "configured key " + i + " must derive from the KINDS row");
        }
    }

    /** The en display words are the upstream LH rows verbatim (BlockTreeSaplingAB.java:47-54). */
    @Test
    void enNamesAreTheUpstreamLhRows() {
        assertEquals("Rubber", GT6TreeKind.RUBBER.enName(), ":47");
        assertEquals("Maple", GT6TreeKind.MAPLE.enName(), ":48");
        assertEquals("Willow", GT6TreeKind.WILLOW.enName(), ":49");
        assertEquals("Blue Mahoe", GT6TreeKind.BLUE_MAHOE.enName(), ":50");
        assertEquals("Hazel", GT6TreeKind.HAZEL.enName(), ":51");
        assertEquals("Cinnamon", GT6TreeKind.CINNAMON.enName(), ":52");
        assertEquals("Coconut", GT6TreeKind.COCONUT.enName(), ":53");
        assertEquals("Rainbowood", GT6TreeKind.RAINBOWOOD.enName(), ":54");
        assertEquals("Blue Spruce", GT6TreeKind.BLUE_SPRUCE.enName(), "BlockTreeSaplingCD.java:45");
    }

    /** The biome-modifier key band mirrors the KINDS order (the offline-safe ResourceKey face). */
    @Test
    void treeBiomeModifierKeysMirrorKinds() {
        assertEquals(GT6TreeBlocks.KINDS.size(), GT6WorldgenDatagen.TREE_BIOME_MODIFIER_KEYS.size(),
                "9 tree AddFeaturesBiomeModifier keys");
        assertEquals("gt6:tree_rubber", GT6WorldgenDatagen.TREE_BIOME_MODIFIER_KEYS.get(0).location().toString(),
                "the Loader tree.rubber row, dot flattened");
    }

    /** The upstream probability column, Loader_Worldgen.java:608-616 verbatim (KINDS order). */
    @Test
    void treeProbabilityColumnIsPinned() {
        assertEquals(List.of(5, 5, 4, 3, 32, 3, 1, 4, 32), GT6WorldgenDatagen.TREE_PROBABILITY,
                "rubber 1/5, maple 1/5, willow 1/4, bluemahoe 1/3, hazel 1/32, cinnamon 1/3, "
                        + "coconut 1/1, rainbowood 1/4, bluespruce 1/32");
    }
}
