/**
 * Tests for task w6-rocks-sticks: the surface deco registration + worldgen band —
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
import gregtech6.block.surface.GT6SurfaceRockBlock;
import gregtech6.block.surface.GT6SurfaceStickBlock;
import gregtech6.block.surface.GT6SurfaceVariants;
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
        // +8 (task w6-t2-surface-blocks): the plant quartet + the 4 fallen-log woods.
        // the vein-indicator rocks, pickup-only like the first-batch rocks/sticks
        // (31 at w6-t3-large-veins; 40 since a-ore-axis-extension;
        // 57 since b-gem-pool-extension, the gem-pool axis chain;
        // 79 since worldgen-edge-ores-b1, the large-vein compensation axis chain).
        // +36 (task flower-blocks-indicator-family): the 18 indicator flowers + the 18
        // vanilla potted companions (GT6FlowerBlockTest pins the row set).
        // +7 (task worldgen-racks): the nether rack rocks.
        // 4 + 8 + 79 + 36 + 7 = 134; the ITEMS register holds the 26
        // obtainable block items (the rocks/sticks/indicator rocks/nether rocks/potted
        // stay zero-item).
        assertEquals(134, GT6SurfaceBlocks.BLOCKS.getEntries().size(),
                "the DeferredRegister holds the 4 rocks/sticks + the 8 obtainable rows + the 79 indicator rocks"
                        + " + the 18 flowers + the 18 potted companions + the 7 nether rack rocks");
        assertEquals(26, GT6SurfaceBlocks.ITEMS.getEntries().size(),
                "the obtainable band's block items (the rocks/sticks/indicator rocks/potted stay zero-item)");
    }

    /** The obtainable band paths + item pairing (task w6-t2-surface-blocks). */
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
     * The 57 vein-indicator rocks (task w6-t3-large-veins; 31 -> 40 with a-ore-axis-extension;
     * 40 -> 57 with b-gem-pool-extension): each literal id snake
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
        assertEquals(79, GT6SurfaceBlocks.INDICATOR_ROCKS.size(), "the spec \u2164 compensation set: 79 distinct valid vein slots");
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

    /**
     * Task debt-issue12-shape-follow-tilt (GitHub #12 residual): the selection box
     * FOLLOWS the render variant. The null-pos call (offline tests, shape caches)
     * falls back to the default centered bar through the six-facings — the C2 pins
     * carried NORTH/SOUTH TRANSPOSED against the emitted dispatch (the old test passed
     * tautologically against its own table; the vanilla end_rod anchor —
     * "facing=north" = x:90 tips the up-model to -Z — settles the maps, pinned per
     * facing in GT6SurfaceVariantsTest); a LIVE position replays the renderer's
     * position-seeded draw and shows that variant's box (the same-position wireframe
     * == the same-position model). Collision stays empty (the upstream :177 null —
     * noCollission). The rock null-pos stays the representative 8x3x8 tier (the
     * issue1-4 pin) and follows its tier live.
     */
    @Test
    void stickSelectionShapeIsBarExact() {
        // offline Block construction needs the block registry temporarily unfrozen (the
        // GTWireContactDamageTest.block / UseLockTest form)
        try {
            java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
            tUnfreeze.setAccessible(true);
            tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        } catch (Exception aE) {
            throw new IllegalStateException("could not unfreeze the offline block registry", aE);
        }
        GT6SurfaceStickBlock tStick = new GT6SurfaceStickBlock(
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollission()); // the surfaceProperties collision face (GT6SurfaceBlocks:220)
        // the null-pos fallback = the default centered bar (raw 2,0,7,14,2,9 through the
        // dispatch rotations) — pixel bounds / 16, VoxelShape#toAabbs speaks 0..1
        java.util.Map<net.minecraft.core.Direction, double[]> tExpected = java.util.Map.of(
                net.minecraft.core.Direction.DOWN, new double[] {2, 0, 7, 14, 2, 9},
                net.minecraft.core.Direction.UP, new double[] {2, 14, 7, 14, 16, 9},
                net.minecraft.core.Direction.NORTH, new double[] {2, 7, 0, 14, 9, 2},
                net.minecraft.core.Direction.SOUTH, new double[] {2, 7, 14, 14, 9, 16},
                net.minecraft.core.Direction.WEST, new double[] {7, 0, 2, 9, 2, 14},
                net.minecraft.core.Direction.EAST, new double[] {7, 0, 2, 9, 2, 14});
        for (var tEntry : tExpected.entrySet()) {
            net.minecraft.world.level.block.state.BlockState tState =
                    tStick.defaultBlockState().setValue(GT6SurfaceRockBlock.FACING, tEntry.getKey());
            var tBoxes = tStick.getShape(tState, null, null, null).toAabbs();
            assertEquals(1, tBoxes.size(), tEntry.getKey() + ": one bar box");
            double[] tE = tEntry.getValue();
            net.minecraft.world.phys.AABB tBox = tBoxes.get(0);
            assertEquals(tE[0] / 16, tBox.minX, 1e-9, tEntry.getKey() + " minX");
            assertEquals(tE[1] / 16, tBox.minY, 1e-9, tEntry.getKey() + " minY");
            assertEquals(tE[2] / 16, tBox.minZ, 1e-9, tEntry.getKey() + " minZ");
            assertEquals(tE[3] / 16, tBox.maxX, 1e-9, tEntry.getKey() + " maxX");
            assertEquals(tE[4] / 16, tBox.maxY, 1e-9, tEntry.getKey() + " maxY");
            assertEquals(tE[5] / 16, tBox.maxZ, 1e-9, tEntry.getKey() + " maxZ");
        }
        // the live follow: the hand-copied renderer draw (the independent java.util.Random
        // transcription, GT6SurfaceVariantsTest.handDraw's twin) picks the variant, the
        // block's getShape at that position IS that variant's box
        GT6SurfaceVariants.Stick[] tTable = GT6SurfaceVariants.Stick.values();
        GT6SurfaceVariants.Rock[] tRockTable = GT6SurfaceVariants.Rock.values();
        GT6SurfaceRockBlock tRock = new GT6SurfaceRockBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(), null);
        for (int tX = -2; tX <= 2; tX++) for (int tZ = -2; tZ <= 2; tZ++) {
            net.minecraft.core.BlockPos tPos = new net.minecraft.core.BlockPos(tX, 0, tZ);
            net.minecraft.world.level.block.state.BlockState tState =
                    tStick.defaultBlockState().setValue(GT6SurfaceRockBlock.FACING, net.minecraft.core.Direction.DOWN);
            java.util.Random tRandom = new java.util.Random(); // the LCG family, not Xoroshiro
            tRandom.setSeed(tState.getSeed(tPos));
            int tDraw = Math.abs((int) tRandom.nextLong()) % 8; // total stick weight = 8 uniform
            GT6SurfaceVariants.Stick tVariant = net.minecraft.util.random.WeightedRandom
                    .getWeightedItem(java.util.List.of(tTable), tDraw).orElse(tTable[0]);
            net.minecraft.world.phys.AABB tVariantBox =
                    GT6SurfaceVariants.shapeOf(tVariant, net.minecraft.core.Direction.DOWN).toAabbs().get(0);
            assertEquals(tVariantBox, tStick.getShape(tState, null, tPos, null).toAabbs().get(0),
                    "live stick shape at " + tPos + " is the drawn variant " + tVariant);
            // the rock tier follows the same chain (weights 3/2/1 -> total 6)
            tRandom = new java.util.Random();
            tRandom.setSeed(tRock.defaultBlockState().getSeed(tPos));
            tDraw = Math.abs((int) tRandom.nextLong()) % 6;
            GT6SurfaceVariants.Rock tTier = net.minecraft.util.random.WeightedRandom
                    .getWeightedItem(java.util.List.of(tRockTable), tDraw).orElse(tRockTable[0]);
            assertEquals(GT6SurfaceVariants.shapeOf(tTier, net.minecraft.core.Direction.DOWN).toAabbs().get(0),
                    tRock.getShape(tRock.defaultBlockState(), null, tPos, null).toAabbs().get(0),
                    "live rock shape at " + tPos + " is the drawn tier " + tTier);
        }
        // BlockState.getCollisionShape (the public BlockStateBase face; the BlockBehaviour
        // 4-arg form went protected on 21.1) — noCollission short-circuits to empty
        assertTrue(tStick.defaultBlockState().getCollisionShape(null, null).isEmpty(),
                "stick collision stays empty — MultiTileEntityStick.java:177 collision null");
        // the rock null-pos pin: the representative 8x3x8 tier (issue1-4 pin kept)
        var tRockBox = tRock.getShape(tRock.defaultBlockState(), null, null, null).toAabbs().get(0);
        assertEquals(new net.minecraft.world.phys.AABB(4 / 16.0, 0, 4 / 16.0, 12 / 16.0, 3 / 16.0, 12 / 16.0), tRockBox,
                "rock null-pos selection falls back to the 8x3x8 representative tier");
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
