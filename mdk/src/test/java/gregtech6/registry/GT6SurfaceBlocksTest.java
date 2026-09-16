/**
 * Tests for task p30-w6-rocks-sticks: the surface deco registration + worldgen band —
 * the acceptance's offline audit unit (the GT6WorldgenDatagenTest posture).
 *
 * <p>Compile anchors: WorldgenRocks.java:63 (the NBT lottery), WorldgenSticks.java:53-55
 * (the three ray-count groups), WorldgenOnSurface.java:49-76 (Amount/Probability ->
 * Count/RarityFilter), Loader_Worldgen.java:618/:630 (the amount/probability binds),
 * MultiTileEntityRock.java:81 (the 1+rng(1+fortune) drop count).
 *
 * <p>Offline-safe by construction: DeferredRegister ENTRIES (no supplier runs) + the
 * headless material enumeration — no RegisterEvent, no bootstrapped registries.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.worldgen.GT6Worldgen;

class GT6SurfaceBlocksTest {

    @BeforeAll
    static void boot() {
        // the material system before MT dereferences (the GTStoneBlocksRegistrationTest posture)
        GTMaterialItems.initMaterials();
    }

    /** The four registration paths, registration order — the Jade/lang/census walk unit. */
    @Test
    void registrationPathsArePinned() {
        assertEquals(List.of("surface_rock_stone", "surface_rock_flint", "surface_rock_meteorite", "surface_stick"),
                GT6SurfaceBlocks.ALL.stream().map(tRow -> tRow.getId().getPath()).toList(),
                "the four surface deco blocks, rocks first (the WorldgenRocks first-batch set + the stick)");
        // the zero-BlockItem ruling: the class registers ONLY blocks (no ITEMS register)
        assertEquals(4, GT6SurfaceBlocks.BLOCKS.getEntries().size(),
                "the DeferredRegister holds exactly the four rows");
    }

    /** The WorldgenOnSurface binds, transcribed into the constant table. */
    @Test
    void surfaceConstantsArePinned() {
        assertEquals(2, GT6Worldgen.SURFACE_ROCKS_AMOUNT, "Loader_Worldgen.java:618 overworld.rocks amount=2");
        assertEquals(3, GT6Worldgen.SURFACE_ROCKS_PROBABILITY, "Loader_Worldgen.java:618 overworld.rocks probability=3");
        assertEquals(2, GT6Worldgen.STICKS_PROBABILITY, "Loader_Worldgen.java:630 sticks probability=2");
        assertEquals(6, GT6Worldgen.STICKS_DENSE_COUNT, "WorldgenSticks.java:53 woods/swamp = mAmount*3");
        assertEquals(4, GT6Worldgen.STICKS_MODERATE_COUNT, "WorldgenSticks.java:54 river/plains/savanna = mAmount*2");
        assertEquals(2, GT6Worldgen.STICKS_SPARSE_COUNT, "WorldgenSticks.java:55 taiga/mesa/wasteland = mAmount*1");
        assertEquals(0.5F, GT6Worldgen.SURFACE_ROCK_CHANCE_STONE, 1e-6F,
                "WorldgenRocks.java:63 nextInt(2)==0 — the NBT-less half is the stone default rock");
        assertEquals(11.0F / 12.0F, GT6Worldgen.SURFACE_ROCK_CHANCE_FLINT, 1e-6F,
                "of the NBT half, 11/12 flint (the joint distribution 12:11:1 = the SPEC weights)");
    }

    /** The configured/placed/biome-tag key paths, the datagen-JSON filename set. */
    @Test
    void surfaceKeyPathsArePinned() {
        assertEquals("gt6:overworld_surface_rocks", GT6Worldgen.SURFACE_ROCKS_CONFIGURED.location().toString());
        assertEquals("minecraft:worldgen/configured_feature", GT6Worldgen.SURFACE_ROCKS_CONFIGURED.registry().toString());
        assertEquals("gt6:overworld_surface_rocks", GT6Worldgen.SURFACE_ROCKS_PLACED.location().toString());
        assertEquals("gt6:overworld_surface_stick", GT6Worldgen.SURFACE_STICK_CONFIGURED.location().toString());
        assertEquals(List.of("overworld_surface_sticks_dense", "overworld_surface_sticks_moderate", "overworld_surface_sticks_sparse"),
                GT6Worldgen.STICKS_GROUP_PATHS, "WorldgenSticks.java:53-55 dense/moderate/sparse order");
        assertEquals("gt6:surface_rocks", GT6Worldgen.SURFACE_ROCKS_BIOMES.location().toString());
        assertEquals("gt6:sticks_dense", GT6Worldgen.STICKS_DENSE_BIOMES.location().toString());
        assertEquals("gt6:sticks_moderate", GT6Worldgen.STICKS_MODERATE_BIOMES.location().toString());
        assertEquals("gt6:sticks_sparse", GT6Worldgen.STICKS_SPARSE_BIOMES.location().toString());
    }

    /**
     * The loot item existence pin: the four item ids the loot datagen dereferences MUST be
     * in the headless enumeration (GTMaterialItems.registrationOrder) — a condition change
     * (rockGt's Or(ORES, STONE) gate, oreRaw's STANDARD_ORE) fails HERE instead of
     * crashing runData with a null handle.
     */
    @Test
    void lootItemIdsExistInEnumeration() {
        Set<String> tItemIds = GTMaterialItems.registrationOrder().stream()
                .map(tPair -> GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()))
                .collect(Collectors.toSet());
        for (String tId : List.of("rock_gt_stone", "rock_gt_meteoric_iron", "ore_raw_meteoric_iron")) {
            assertTrue(tItemIds.contains(tId),
                    "the loot item gt6:" + tId + " must be a registered material item (the meteorite/stone loot faces)");
        }
        // and the material identities the blocks tint with must exist
        assertTrue(MT.Stone != null && MT.Flint != null && MT.MeteoricIron != null, "the three tint materials exist");
    }
}
