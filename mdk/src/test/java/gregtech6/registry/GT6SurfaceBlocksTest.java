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
        // the material system before MT dereferences (the GTStoneBlocksRegistrationTest posture);
        // the vanilla bootstrap bracket for the ForgeRegistries/ResourceKey classes the
        // GT6SurfaceBlocks clinit touches (the GT6WorldgenDatagenTest posture, offline throwables ignored)
        GTMaterialItems.initMaterials();
        // the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** The first-batch registration paths, registration order — the Jade/lang/census walk unit. */
    @Test
    void registrationPathsArePinned() {
        assertEquals(List.of("surface_rock_stone", "surface_rock_flint", "surface_rock_meteorite", "surface_stick"),
                GT6SurfaceBlocks.ALL.stream().map(tRow -> tRow.getId().getPath()).limit(4).toList(),
                "the first-batch surface deco blocks, rocks first (the WorldgenRocks first-batch set + the stick)");
        // +8 (task p30-w6-t2-surface-blocks): the plant quartet + the 4 fallen-log woods.
        // +31 (task p30-w6-t3-large-veins): the vein-indicator rocks, pickup-only like
        // the first-batch rocks/sticks. 4 + 8 + 31 = 43; the ITEMS register holds the 8
        // obtainable block items (the rocks/sticks/indicator rocks stay zero-item).
        assertEquals(43, GT6SurfaceBlocks.BLOCKS.getEntries().size(),
                "the DeferredRegister holds the 4 rocks/sticks + the 8 obtainable rows + the 31 indicator rocks");
        assertEquals(8, GT6SurfaceBlocks.ITEMS.getEntries().size(),
                "the obtainable band's block items (the rocks/sticks/indicator rocks stay zero-item)");
    }

    /** The obtainable band paths + item pairing (task p30-w6-t2-surface-blocks). */
    @Test
    void surfacePlantPathsArePinned() {
        assertEquals(List.of("glowtus", "berry_bush", "black_sand", "turf"),
                GT6SurfaceBlocks.PLANT_BAND.stream().map(tRow -> tRow.getId().getPath()).toList(),
                "the plant quartet, registration order");
        assertEquals(List.of("dead_log", "rotten_log", "mossy_log", "frozen_log"),
                GT6SurfaceBlocks.FALLEN_LOGS.stream().map(tRow -> tRow.getId().getPath()).toList(),
                "the fallen-log woods, Log1 meta order (BlockTreeLog1.java:46-62)");
        for (int i = 0; i < GT6SurfaceBlocks.PLANT_BAND.size(); i++) {
            assertEquals(GT6SurfaceBlocks.PLANT_TAB_ITEMS.get(i).getId().getPath(),
                    GT6SurfaceBlocks.PLANT_BAND.get(i).getId().getPath(),
                    "plant block item i pairs plant block i");
        }
        for (int i = 0; i < GT6SurfaceBlocks.FALLEN_LOGS.size(); i++) {
            assertEquals(GT6SurfaceBlocks.LOG_TAB_ITEMS.get(i).getId().getPath(),
                    GT6SurfaceBlocks.FALLEN_LOGS.get(i).getId().getPath(),
                    "log block item i pairs fallen-log block i");
        }
    }

    /**
     * The 31 vein-indicator rocks (task p30-w6-t3-large-veins): each literal id snake
     * matches the material's composed path (GTMaterialItems.snakeCase over mNameInternal —
     * the literal was required because the class loads before MT.init), the rockGt loot
     * item exists in the headless enumeration, and the {@code indicatorRock} lookup
     * resolves by material identity with the default-rock fallback (the WorldgenOresLarge
     * .java:104 NBT-less arm).
     */
    @Test
    void indicatorRocksMatchTheirMaterials() {
        Set<String> tItemIds = GTMaterialItems.registrationOrder().stream()
                .map(tPair -> GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()))
                .collect(Collectors.toSet());
        assertEquals(31, GT6SurfaceBlocks.INDICATOR_ROCKS.size(), "the spec \u2164 compensation set: 31 distinct valid vein slots");
        for (int i = 0; i < GT6SurfaceBlocks.INDICATOR_ROCKS.size(); i++) {
            gregapi.oredict.OreDictMaterial tMaterial = GT6SurfaceBlocks.INDICATOR_MATERIALS.get(i).get();
            assertEquals("surface_rock_" + GTMaterialItems.snakeCase(tMaterial.mNameInternal),
                    GT6SurfaceBlocks.INDICATOR_ROCKS.get(i).getId().getPath(),
                    "the literal snake must equal the composed material path (row " + i + ")");
            assertTrue(tItemIds.contains("rock_gt_" + GTMaterialItems.snakeCase(tMaterial.mNameInternal)),
                    "the loot item rock_gt_" + GTMaterialItems.snakeCase(tMaterial.mNameInternal) + " must exist");
        }
        // the block-instance faces (the indicatorRock identity lookup + the default-rock
        // fallback for a null/unregistered pick) need the live RegisterEvent — the RCON
        // live face covers them (offline DeferredRegister handles resolve only post-registration)
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
