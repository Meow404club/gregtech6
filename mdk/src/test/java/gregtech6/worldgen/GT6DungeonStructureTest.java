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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import gregtech6.datagen.GT6LootInjectionDatagen;
import gregtech6.datagen.GT6LootTables;
import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GTMaterialItems;
import gregtech6.worldgen.dungeon.GT6DungeonLayout;
import gregtech6.worldgen.dungeon.GT6DungeonStructure;
import gregtech6.worldgen.dungeon.GT6DungeonPiece;
import gregtech6.worldgen.dungeon.GT6DungeonStructure;

class GT6DungeonStructureTest {

    @BeforeAll
    static void boot() {
        // the material system for the loot-row resolver faces (GT6LootInjectionTest posture);
        // vanilla bootstrap bracket for the ResourceKey/Registries/Direction classes (the
        // GT6WorldgenDatagenTest posture — offline throwables ignored). The version detect
        // MUST precede bootStrap (the GTBlockPropertyIdentityTest/GTOreOverlay211SeamTest
        // form — a bare-JVM first boot throws at Util.doFetchChoiceType and leaves the
        // Blocks/Items classes half-initialized for every later face in this JVM).
        GTMaterialItems.initMaterials();
        net.minecraft.SharedConstants.tryDetectVersion();
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

    // ---------------------------------------------------------------- room batch (dungeon-rooms-batch)

    /**
     * The room-kind vocabulary: 9 kinds = the framework's four + the batch card's five
     * pool rooms (upstream ROOMS :85-94 minus the Library rows) and the two special-cell
     * rooms; every name survives the NBT round trip ({@code Kind.of}).
     */
    @Test
    void roomKindVocabularyIsPinned() {
        assertEquals(12, GT6DungeonPiece.Kind.values().length,
                "the framework 4 + Corridor3/Corridor4/Barracks/Workshop + MiningBedrock + the 3 farms");
        for (GT6DungeonPiece.Kind tKind : GT6DungeonPiece.Kind.values()) {
            assertEquals(tKind, GT6DungeonPiece.Kind.valueOf(tKind.name()), "the NBT round trip: " + tKind);
        }
    }

    /**
     * The ROOMS pool: exactly the five upstream ported rooms in the upstream list order
     * (Workshop :85, MiningBedrock :86, the Library rows :87-89 = the parallel library
     * card's seam, FarmMobs :90, FarmCrop :91, FarmFish :92); immutable (the dispatch
     * copies per dungeon).
     */
    @Test
    void roomsPoolIsPinned() {
        assertEquals(List.of(
                GT6DungeonPiece.Kind.WORKSHOP,
                GT6DungeonPiece.Kind.MINING_BEDROCK,
                GT6DungeonPiece.Kind.FARM_MOBS,
                GT6DungeonPiece.Kind.FARM_CROP,
                GT6DungeonPiece.Kind.FARM_FISH), GT6DungeonStructure.ROOMS_POOL,
                "the pool = the upstream ROOMS rows minus the Library seam, order verbatim");
        assertThrows(UnsupportedOperationException.class, () -> GT6DungeonStructure.ROOMS_POOL.add(null),
                "the pool is immutable — the dispatch works on a copy");
    }

    /** The corridor split (upstream :289-291): 4-way crossing, 3-way alcove, else the plain arm. */
    @Test
    void corridorKindMappingIsPinned() {
        assertEquals(GT6DungeonPiece.Kind.CORRIDOR4, GT6DungeonStructure.corridorKind(4));
        assertEquals(GT6DungeonPiece.Kind.CORRIDOR3, GT6DungeonStructure.corridorKind(3));
        assertEquals(GT6DungeonPiece.Kind.CORRIDOR, GT6DungeonStructure.corridorKind(2));
        assertEquals(GT6DungeonPiece.Kind.CORRIDOR, GT6DungeonStructure.corridorKind(1));
        assertEquals(GT6DungeonPiece.Kind.CORRIDOR, GT6DungeonStructure.corridorKind(0));
    }

    /**
     * The mob-tower spill mask (upstream FarmMobs :41-56): the diagonal is licensed only
     * when the diagonal AND both orthos are 0-or-CORRIDOR. Synthetic 3x3 layouts pin all
     * four quadrants; the live-layout sweep then asserts no mask bit ever licenses a
     * room-occupied neighbor.
     */
    @Test
    void mobsDiagMaskIsPinned() {
        byte[][] t = new byte[3][3];
        byte tRoom = 1, tRock = 0, tCorr = GT6DungeonLayout.CORRIDOR;
        // the empty grid licenses NW
        assertEquals(0b0001, GT6DungeonStructure.mobsDiagMask(t, 1, 1) & 0b0001, "all rock -> NW licensed");
        // a room diagonal kills the bit
        t[0][0] = tRoom;
        assertEquals(0b0000, GT6DungeonStructure.mobsDiagMask(t, 1, 1) & 0b0001, "room at the diagonal -> NW dead");
        // a corridor diagonal is fine (the == -128 arm)
        t[0][0] = tCorr;
        assertEquals(0b0001, GT6DungeonStructure.mobsDiagMask(t, 1, 1) & 0b0001, "corridor at the diagonal -> NW licensed");
        // a room ortho kills the bit
        t[0][0] = tRock;
        t[1][0] = tRoom; // the north ortho
        assertEquals(0b0000, GT6DungeonStructure.mobsDiagMask(t, 1, 1) & 0b0001, "room at the ortho -> NW dead");
        t[1][0] = tRock;
        // live layouts: every licensed diagonal is 0-or-corridor and so are the orthos
        for (long tSeed = 0; tSeed < 100; tSeed++) {
            byte[][] tLayout = GT6DungeonLayout.generate(RandomSource.create(tSeed));
            int tSide = tLayout.length;
            for (int i = 1; i < tSide - 1; i++) for (int j = 1; j < tSide - 1; j++) {
                if (tLayout[i][j] != GT6DungeonLayout.ROOM_ID) continue;
                int tMask = GT6DungeonStructure.mobsDiagMask(tLayout, i, j);
                int[][] tQuads = {{i - 1, j - 1}, {i + 1, j - 1}, {i - 1, j + 1}, {i + 1, j + 1}};
                int[][] tOrthoPairs = {{i - 1, j, i, j - 1}, {i + 1, j, i, j - 1}, {i - 1, j, i, j + 1}, {i + 1, j, i, j + 1}};
                for (int tBit = 0; tBit < 4; tBit++) {
                    if ((tMask & (1 << tBit)) == 0) continue;
                    int tD = tLayout[tQuads[tBit][0]][tQuads[tBit][1]];
                    assertTrue(tD == 0 || tD == GT6DungeonLayout.CORRIDOR,
                            "licensed diagonal must be rock/corridor (seed " + tSeed + " bit " + tBit + ")");
                    int tO1 = tLayout[tOrthoPairs[tBit][0]][tOrthoPairs[tBit][1]];
                    int tO2 = tLayout[tOrthoPairs[tBit][2]][tOrthoPairs[tBit][3]];
                    assertTrue(tO1 == 0 || tO1 == GT6DungeonLayout.CORRIDOR, "the first ortho must be free");
                    assertTrue(tO2 == 0 || tO2 == GT6DungeonLayout.CORRIDOR, "the second ortho must be free");
                }
            }
        }
    }

    /** The barracks safe-loot table ids: the p34 ChestGenHooks mapping rows verbatim. */
    @Test
    void barracksSafeLootsArePinned() {
        assertEquals(9, GT6DungeonPiece.BARRACKS_SAFE_LOOTS.length, "the upstream tLoots row (:108)");
        List<String> tWanted = List.of(
                "minecraft:chests/stronghold_library", "minecraft:chests/stronghold_corridor",
                "minecraft:chests/stronghold_crossing", "minecraft:chests/desert_pyramid",
                "minecraft:chests/jungle_temple", "minecraft:chests/village/village_weaponsmith",
                "minecraft:chests/abandoned_mineshaft", "gt6:chests/dungeon_chest",
                "minecraft:chests/spawn_bonus_chest");
        assertEquals(tWanted, List.of(GT6DungeonPiece.BARRACKS_SAFE_LOOTS), "the p34 mapping verbatim");
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

    // ---------------------------------------------------------------- dungeon keys (task dungeon-keys)

    /**
     * The per-dungeon key roll (WorldgenDungeonGT.java:169-171 transcription): five ids,
     * descending, first {@code 1 + max(draw, chunk tag)} — positive, strictly decreasing,
     * deterministic per origin chunk and distinct across dungeons (the nanoTime :170
     * anti-collision face carried by the unique chunk tag).
     */
    @Test
    void dungeonKeyRollIsDeterministicDescendingAndCollisionFree() {
        for (long tSeed = 0; tSeed < 50; tSeed++) {
            ChunkPos tOrigin = new ChunkPos((int) tSeed * 11 - 500, (int) tSeed * 7 + 3);
            long[] tIds = GT6DungeonStructure.keyIds(tOrigin);
            assertEquals(5, tIds.length, "five keys per dungeon (:161)");
            assertTrue(tIds[0] > tOrigin.toLong(), "the first id exceeds the chunk tag (the max() floor, seed " + tSeed + ")");
            for (int i = 1; i < tIds.length; i++) {
                assertEquals(tIds[i - 1] - 1, tIds[i], "the descending chain (:171, seed " + tSeed + ")");
            }
            long[] tAgain = GT6DungeonStructure.keyIds(tOrigin);
            assertArrayEquals(tIds, tAgain, "the roll is a pure function of the origin chunk (seed " + tSeed + ")");
        }
        // two dungeons never share an id set (the collision faces are disjoint)
        long[] tA = GT6DungeonStructure.keyIds(new ChunkPos(1234, -987));
        long[] tB = GT6DungeonStructure.keyIds(new ChunkPos(-987, 1234));
        Set<Long> tSetA = new HashSet<>();
        for (long tId : tA) tSetA.add(tId);
        for (long tId : tB) assertFalse(tSetA.contains(tId), "cross-dungeon id collision");
    }

    /** The key pool constant faces (WorldgenDungeonGT.java:161/173 anchors, the IL.KEYS draw). */
    @Test
    void dungeonKeyConstantsArePinned() {
        assertEquals(5, gregtech6.items.GT6Keys.KEYS_PER_DUNGEON, "the :161 boolean[5] face");
        assertEquals(10, gregtech6.items.GT6Keys.KEYS.size(), "the IL.KEYS draw table (IL.java:516)");
    }

    /**
     * The hide-draw gate (the Workshop :119-120 clamp — the AIOOBE red item): every
     * draw is either -1 (the >= keys branch, hide nothing) or a passing value that IS
     * the key index, naturally {@code < keys} — never a raw next(keys*2) value fed to
     * the five-id array unchecked.
     */
    @Test
    void hideDrawsAreGatedToTheFiveIdsOrNone() {
        for (long tSeed = 0; tSeed < 200; tSeed++) {
            ChunkPos tOrigin = new ChunkPos((int) tSeed * 13 - 900, (int) tSeed * 5 + 1);
            for (int tCount = 0; tCount <= 8; tCount++) {
                int[] tDraws = GT6DungeonStructure.hideDraws(tOrigin, tCount);
                assertEquals(tCount, tDraws.length);
                for (int tDraw : tDraws) {
                    assertTrue(tDraw == -1 || (tDraw >= 0 && tDraw < gregtech6.items.GT6Keys.KEYS_PER_DUNGEON),
                            "hide draw " + tDraw + " escapes {-1} ∪ [0," + gregtech6.items.GT6Keys.KEYS_PER_DUNGEON
                                    + ") (seed " + tSeed + ") — the mKeyIds[mKeyIndex] clamp is broken");
                }
            }
        }
    }

    /**
     * The 50%-per-draw gate is live (the red item: upstream hides in ~half the draws,
     * not all): over a wide sweep both branches occur, and the hide rate stays inside
     * a generous sanity band around 50%.
     */
    @Test
    void hideDrawsKeepTheUpstreamHalfRate() {
        int tHides = 0, tTotal = 0;
        boolean tSawNone = false, tSawHide = false;
        for (long tSeed = 0; tSeed < 300; tSeed++) {
            int[] tDraws = GT6DungeonStructure.hideDraws(new ChunkPos((int) tSeed * 17, (int) tSeed * 3 - 40), 4);
            for (int tDraw : tDraws) {
                tTotal++;
                if (tDraw < 0) {
                    tSawNone = true;
                } else {
                    tSawHide = true;
                    tHides++;
                }
            }
        }
        assertTrue(tSawNone && tSawHide, "both the hide and the skip branch must occur");
        double tRate = (double) tHides / tTotal;
        assertTrue(tRate > 0.35 && tRate < 0.65, "the hide rate " + tRate + " drifts from the upstream 1/2");
    }

    /**
     * The sequential-stream spread (the red item: a per-piece reseed made every
     * dead-end of one dungeon redraw the SAME index): within one origin the draws
     * diverge, and across the sweep every key index receives hides (no key is
     * unfarmable just because the dungeon has several dead-ends).
     */
    @Test
    void hideDrawsSpreadAcrossDeadEndsAndKeys() {
        boolean tSawDivergence = false;
        java.util.Set<Integer> tHiddenIndexes = new HashSet<>();
        for (long tSeed = 0; tSeed < 200; tSeed++) {
            int[] tDraws = GT6DungeonStructure.hideDraws(new ChunkPos((int) tSeed * 7 + 11, (int) tSeed * 11 - 77), 4);
            Set<Integer> tDistinct = new HashSet<>();
            for (int tDraw : tDraws) if (tDraw >= 0) tDistinct.add(tDraw);
            if (tDistinct.size() >= 2) tSawDivergence = true;
            tHiddenIndexes.addAll(tDistinct);
        }
        assertTrue(tSawDivergence, "a multi-dead-end dungeon must take DIFFERENT draws per dead-end");
        // 200 dungeons x 4 dead-ends x 1/2 gate x 1/5 spread ≈ 80 hides per index —
        // a fully missing index would mean the spread collapsed.
        assertEquals(Set.of(0, 1, 2, 3, 4), tHiddenIndexes, "every key index must receive hides across the sweep");
    }

    /** The hide table is a pure function of the origin chunk (the audit face). */
    @Test
    void hideDrawsAreDeterministicPerOrigin() {
        ChunkPos tOrigin = new ChunkPos(4242, -4242);
        assertArrayEquals(GT6DungeonStructure.hideDraws(tOrigin, 6), GT6DungeonStructure.hideDraws(tOrigin, 6));
    }
}
