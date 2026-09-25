/**
 * Tests for task p38-dungeon-framework: the shelter-dungeon layout arithmetic + the
 * piece vocabulary constants + the dungeon-chest loot carrier — the acceptance's
 * offline audit unit.
 *
 * <p>Compile anchors (transcribed independently, production and test must agree or a
 * conscious decision is forced):
 * <ul>
 * <li>WorldgenDungeonGT.java:141-144 — the trigger row (1/100 + the 11-chunk grid +
 *     the spawn exclusion + the bedrock check, the last one dropped by ruling);</li>
 * <li>WorldgenDungeonGT.java:159-248 — the grid layout (sizes, important rooms,
 *     roomChance roll, corridor carving, the two pruning passes) — the production
 *     {@code GT6DungeonLayout.generate} is the verbatim transcription, so these tests
 *     pin the INVARIANTS (border, counts, connectivity) rather than RNG sequences (the
 *     p31 decision-level determinism precedent: java.util.Random draw order is not
 *     replicated);</li>
 * <li>Loader_Worldgen.java:652 — prob=100 size 3-7 Y20 roomChance=6 overworld-only;</li>
 * <li>RandomSpreadStructurePlacement (both legs) — offset = nextInt(spacing-separation)
 *     per salt-seeded cell, the documented fixed-offset-5 → uniform deviation;</li>
 * <li>GT6LootInjectionTest — the headless material-system boot the loot pins replay.</li>
 * </ul>
 *
 * <p>Offline-safe by construction: ResourceKey interns, RandomSource.create, the
 * registration-free loot row tables — no registry boot, no BlockState construction.
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import gregtech6.datagen.GT6LootInjectionDatagen;
import gregtech6.datagen.GT6LootTables;
import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GTMaterialItems;
import gregtech6.worldgen.dungeon.GT6DungeonLayout;
import gregtech6.worldgen.dungeon.GT6DungeonPiece;

class GT6DungeonStructureTest {

    @BeforeAll
    static void boot() {
        // the material system for the loot-row resolver faces (GT6LootInjectionTest posture);
        // vanilla bootstrap bracket for the ResourceKey/Registries/Direction classes (the
        // GT6WorldgenDatagenTest posture — offline throwables ignored).
        GTMaterialItems.initMaterials();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- constants

    /** The Loader_Worldgen.java:652 registration row, transcribed into the layout constants. */
    @Test
    void layoutConstantsArePinned() {
        assertEquals(100, GT6DungeonLayout.PROBABILITY, "probability=100 -> the 1/100 chunk roll (:141)");
        assertEquals(3, GT6DungeonLayout.MIN_SIZE, "MinSize=3 (:135)");
        assertEquals(7, GT6DungeonLayout.MAX_SIZE, "MaxSize=7 (:135)");
        assertEquals(20, GT6DungeonLayout.DUNGEON_Y, "Y=20, min=max so the room band sits at 20..27 (:135)");
        assertEquals(6, GT6DungeonLayout.ROOM_CHANCE, "roomChance=6 (:135)");
        assertEquals(2, GT6DungeonLayout.IMPORTANT_ROOM_COUNT, "the barracks + entrance pair (:137)");
        assertEquals(11, GT6DungeonLayout.gridPeriod(), "the chunk-grid period = maxSize + 4 (:144)");
        assertEquals(23, GT6DungeonLayout.SPAWN_EXCLUDE_CHUNKS,
                "the spawn exclusion 256+maxSize*16 blocks = 16+maxSize chunks (:142)");
        assertEquals(-128, GT6DungeonLayout.CORRIDOR, "the corridor cell code (:200)");
        assertEquals(-2, GT6DungeonLayout.ENTRANCE, "the entrance cell code (:183)");
        assertEquals(-1, GT6DungeonLayout.BARRACKS, "the barracks cell code (:183)");
        assertEquals(1, GT6DungeonLayout.ROOM_ID, "ROOM_ID_COUNT = 1 (:137)");
    }

    /** The structure-set placement constants (the 11-chunk grid JSON band). */
    @Test
    void placementConstantsArePinned() {
        assertEquals(11, GT6WorldgenDatagen.DUNGEON_SPACING, "spacing = the upstream grid period");
        assertEquals(5, GT6WorldgenDatagen.DUNGEON_SEPARATION,
                "separation = the upstream fixed mid-cell offset (the uniform deviation floor)");
        assertEquals(371266954, GT6WorldgenDatagen.DUNGEON_SALT, "the arbitrary salt, pinned");
        assertEquals("minecraft:worldgen/structure_set", GT6WorldgenDatagen.DUNGEON_STRUCTURE_SET.registry().toString(),
                "the set key lives in the structure_set registry");
        assertEquals("gt6:dungeon", GT6WorldgenDatagen.DUNGEON_STRUCTURE.location().toString(),
                "the structure id = the JSON face the structure_set references");
        assertTrue(GT6WorldgenDatagen.BUILDER_KEYS.contains(Registries.STRUCTURE)
                && GT6WorldgenDatagen.BUILDER_KEYS.contains(Registries.STRUCTURE_SET),
                "the builder carries the two dungeon bands");
    }

    // ---------------------------------------------------------------- layout

    /** The grid side roll: {@code 2 + 3 + rand(1 + 7 - 3)} = 5..9 (:159). */
    @Test
    void gridSizeBounds() {
        for (long tSeed = 0; tSeed < 200; tSeed++) {
            int tSide = GT6DungeonLayout.gridSize(RandomSource.create(tSeed));
            assertTrue(tSide >= 5 && tSide <= 9, "grid side " + tSide + " out of 5..9 (seed " + tSeed + ")");
        }
    }

    /**
     * The layout invariants over 200 seeds: the border ring stays rock, exactly one
     * entrance, at least one barracks, at least two rooms, and — the structural
     * acceptance face — every non-zero cell is corridor-connected to the grid center.
     */
    @Test
    void layoutInvariantsHold() {
        for (long tSeed = 0; tSeed < 200; tSeed++) {
            byte[][] tLayout = GT6DungeonLayout.generate(RandomSource.create(tSeed));
            int tSide = tLayout.length;
            int tEntrances = 0, tBarracks = 0, tRooms = 0, tCorridors = 0;
            Set<Long> tOccupied = new HashSet<>();
            for (int i = 0; i < tSide; i++) for (int j = 0; j < tSide; j++) {
                byte tCell = tLayout[i][j];
                boolean tBorder = i == 0 || j == 0 || i == tSide - 1 || j == tSide - 1;
                if (tBorder) {
                    assertEquals(0, tCell, "border cell (" + i + "," + j + ") must stay rock (seed " + tSeed + ")");
                    continue;
                }
                switch (tCell) {
                    case GT6DungeonLayout.ENTRANCE -> tEntrances++;
                    case GT6DungeonLayout.BARRACKS -> tBarracks++;
                    case GT6DungeonLayout.ROOM_ID -> tRooms++;
                    case GT6DungeonLayout.CORRIDOR -> tCorridors++;
                    case 0 -> {
                    }
                    default -> assertFalse(true, "unknown cell code " + tCell + " at (" + i + "," + j + ")");
                }
                if (tCell != 0) tOccupied.add(key(i, j));
            }
            assertEquals(1, tEntrances, "exactly one entrance per dungeon (seed " + tSeed + ")");
            assertTrue(tBarracks >= 1, "the barracks important room placed (seed " + tSeed + ")");
            assertTrue(tBarracks <= 1, "at most one barracks (seed " + tSeed + ")");
            assertTrue(tRooms >= 2, "the roomChance loop guarantees 2 rooms (seed " + tSeed + ")");
            // corridors may be ZERO (adjacent rooms carve nothing — seed 0 is the live case);
            // connectivity is the invariant, pinned by the flood-fill below.

            // connectivity: flood-fill from the first occupied cell (the center itself may
            // stay rock when every carve-walk stops early — the upstream walk shape) —
            // every occupied cell must be reachable from any other.
            assertTrue(!tOccupied.isEmpty(), "the layout places rooms (seed " + tSeed + ")");
            ArrayDeque<Long> tQueue = new ArrayDeque<>(List.of(tOccupied.iterator().next()));
            Set<Long> tSeen = new HashSet<>(tQueue);
            while (!tQueue.isEmpty()) {
                long tKey = tQueue.poll();
                int ti = (int) (tKey >> 16), tj = (int) (tKey & 0xFFFF);
                for (int[] tOff : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int tni = ti + tOff[0], tnj = tj + tOff[1];
                    if (tni < 0 || tnj < 0 || tni >= tSide || tnj >= tSide) continue;
                    long tN = key(tni, tnj);
                    if (tOccupied.contains(tN) && tSeen.add(tN)) tQueue.add(tN);
                }
            }
            assertEquals(tOccupied, tSeen, "every room/corridor cell must be center-connected (seed " + tSeed + ")");
        }
    }

    private static long key(int aI, int aJ) {
        return ((long) aI << 16) | aJ;
    }

    /**
     * The dispatch rule, pinned through the layout face: a ROOM cell with exactly one
     * occupied neighbor is a DEAD_END (draws the storage vault); everything else falls
     * back to the empty room (the ROOMS pool ships empty this card). The door bitfield:
     * the 2D data order S=0/W=1/N=2/E=3 (Direction.get2DDataValue), self-consistent with
     * {@link GT6DungeonPiece#doorsOf}.
     */
    @Test
    void deadEndDispatchAndDoorBits() {
        assertEquals(8, 1 << Direction.EAST.get2DDataValue(), "EAST bit = 8 (2D order S,W,N,E)");
        assertEquals(1, 1 << Direction.SOUTH.get2DDataValue(), "SOUTH bit = 1");
        int tSide0 = 0;
        boolean tSawAnyDeadEnd = false;
        for (long tSeed = 0; tSeed < 100; tSeed++) {
            byte[][] tLayout = GT6DungeonLayout.generate(RandomSource.create(tSeed));
            int tSide = tLayout.length;
            tSide0 = tSide;
            for (int i = 1; i < tSide - 1; i++) for (int j = 1; j < tSide - 1; j++) {
                if (tLayout[i][j] != GT6DungeonLayout.ROOM_ID) continue;
                byte tDoors = GT6DungeonPiece.doorsOf(tLayout, i, j);
                int tConnections = GT6DungeonLayout.connectionCount(tLayout, i, j);
                // every occupied neighbor contributes a bit
                int tBitCount = Integer.bitCount(tDoors & 0xF);
                assertEquals(tConnections, tBitCount, "door bits == connection count (seed " + tSeed + ")");
                if (tConnections == 1) tSawAnyDeadEnd = true; // storage-vault dispatch
            }
        }
        assertTrue(tSide0 > 0, "the sweep ran");
        assertTrue(tSawAnyDeadEnd, "at least one dead-end room across the seed sweep");
    }

    // ---------------------------------------------------------------- loot carrier

    /**
     * The dungeon-chest carrier rows: the DUNGEON_CHEST category minus the ZPM artifact
     * (the boundary — see the accessor javadoc). 16 metal-ladder rows + the Guide row.
     */
    @Test
    void dungeonChestRowsArePinned() {
        List<GT6LootInjectionDatagen.EntryRow> tRows = GT6LootInjectionDatagen.dungeonChestEntries();
        assertEquals(17, tRows.size(), "16 ladder rows + the Guide row (:418-442, the artifact cut)");
        assertEquals("gt6:book_loot_guide", tRows.get(tRows.size() - 1).item(), "the Guide row is the tail (:442)");
        assertEquals(50, tRows.get(tRows.size() - 1).weight(), "the Guide weight 50 (:442)");
        assertEquals(2, tRows.get(tRows.size() - 1).min(), "the Guide stack floor 2 (:442)");
        assertEquals(8, tRows.get(tRows.size() - 1).max(), "the Guide stack cap 8 (:442)");
        assertTrue(tRows.stream().noneMatch(aRow -> aRow.item().equals("gt6:zpm")),
                "the ZPM artifact stays on the vanilla injection (the p34/p38 boundary)");
        // the vanilla injection still carries the artifact (the p34 face untouched)
        GT6LootInjectionDatagen.InjectionRow tVanilla = GT6LootInjectionDatagen.injections().stream()
                .filter(aRow -> aRow.name().equals("dungeon_inject_simple_dungeon")).findFirst().orElseThrow();
        assertTrue(tVanilla.entries().stream().anyMatch(aRow -> aRow.item().equals("gt6:zpm")),
                "the vanilla simple_dungeon injection keeps the ZPM artifact");
        assertEquals(GT6LootInjectionDatagen.ROLL_MIN, 1, "the shared roll floor");
        assertEquals(GT6LootInjectionDatagen.ROLL_MAX, 3, "the shared roll cap");
    }

    /** The table id the piece binds (createChest) == the id datagen emits — one string, two faces. */
    @Test
    void dungeonChestTableIdIsPinned() {
        assertEquals("chests/dungeon_chest", GT6DungeonPiece.DUNGEON_CHEST_TABLE,
                "the piece-side relative id (the ResourceLocation/ResourceKey arg face)");
        assertEquals(new ResourceLocation("gt6", "chests/dungeon_chest"),
                GT6LootTables.GT6DungeonChestLoot.tableId(),
                "the datagen-side full id — same table, both faces");
        assertEquals(Registries.STRUCTURE_SET.location().toString(), "minecraft:worldgen/structure_set",
                "the StructureSet key registry (the datagen band rides the vanilla set registry)");
    }
}
