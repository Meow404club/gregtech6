package gregtech6.worldgen.dungeon;

import net.minecraft.util.RandomSource;

/**
 * The shelter-dungeon grid layout (task p38-dungeon-framework) — the upstream
 * {@code WorldgenDungeonGT.generate} layout math (tmp/gt6-1.7.10
 * WorldgenDungeonGT.java:159-248) as a PURE function over a RandomSource: no world
 * access, so the offline tests can pin the maze arithmetic byte-for-byte against the
 * upstream constants. The cell vocabulary:
 * <ul>
 * <li>{@code 0} — solid rock (border ring + uncarved cells);</li>
 * <li>{@code CORRIDOR (-128)} — carved corridor cell;</li>
 * <li>{@code ENTRANCE (-2)} — the surface-shaft cell (one per dungeon);</li>
 * <li>{@code BARRACKS (-1)} — the second important-room cell (room piece deferred to a
 *     later card — the dispatch maps it to the empty room, the upstream
 *     {@code ROOM_EMPTY} fallback face);</li>
 * <li>{@code ROOM_ID (1)} — a rolled room cell (upstream {@code ROOM_ID_COUNT = 1}).</li>
 * </ul>
 *
 * <p>Constants are the Loader_Worldgen.java:652 registration row: probability 100,
 * size 3..7, Y 20, roomChance 6. The upstream config-file knobs ride the datapack
 * structure/structure_set JSONs instead (a GT6.cfg re-port is not generated).
 *
 * <p>Determinism posture (the p31 strata-lens precedent): decision-level — the layout
 * consumes the chunk-seeded {@code WorldgenRandom}, so the same world seed yields the
 * same layout for the same dungeon chunk; the upstream java.util.Random draw ORDER is
 * not replicated (RNG sequence identity is out of scope, the research-card ruling).
 */
public final class GT6DungeonLayout {

    /** The cell codes (upstream WorldgenDungeonGT.java:159-187 vocabulary). */
    public static final byte CORRIDOR = -128, ENTRANCE = -2, BARRACKS = -1, ROOM_ID = 1;

    /** The registration-row constants, Loader_Worldgen.java:652 (prob=100 size 3-7 Y20 roomChance=6). */
    public static final int PROBABILITY = 100, MIN_SIZE = 3, MAX_SIZE = 7, DUNGEON_Y = 20, ROOM_CHANCE = 6;

    /** Upstream {@code IMPORTANT_ROOM_COUNT = 2} (barracks + entrance, :137). */
    public static final int IMPORTANT_ROOM_COUNT = 2;

    /**
     * The spawn exclusion: upstream {@code Math.abs(aMinX) < 256+mMaxSize*16} in blocks
     * (:142) = {@code |chunk| < 16 + MAX_SIZE} chunks.
     */
    public static final int SPAWN_EXCLUDE_CHUNKS = 16 + MAX_SIZE;

    /** The grid side: {@code 2 + MIN_SIZE + rand(1 + MAX_SIZE - MIN_SIZE)} = 5..9 (:159). */
    public static int gridSize(RandomSource aRandom) {
        return 2 + MIN_SIZE + aRandom.nextInt(1 + MAX_SIZE - MIN_SIZE);
    }

    /** The chunk-grid placement period: upstream {@code mMaxSize + 4} = 11 (:144). */
    public static int gridPeriod() {
        return MAX_SIZE + 4;
    }

    /**
     * The full layout roll (:159-248): grid side roll, the two important rooms, the
     * roomChance room roll, the corridor carving toward the center, then the two
     * pruning passes — every line the upstream {@code generate} body, world access
     * stripped. The border ring stays 0 and every non-zero cell lives in {@code [1, n-2]}.
     */
    public static byte[][] generate(RandomSource aRandom) {
        int n = gridSize(aRandom);
        byte[][] tLayout = new byte[n][n];

        // :180-184 — the two important rooms, -1 (barracks) then -2 (entrance), random
        // free interior cells, 10000-try guard verbatim.
        for (int i = 0, j = 0, k = -1, l = 0; k >= -IMPORTANT_ROOM_COUNT && l < 10000; l++) {
            i = 1 + aRandom.nextInt(n - 2);
            j = 1 + aRandom.nextInt(n - 2);
            if (tLayout[i][j] == 0) {
                tLayout[i][j] = (byte) k--;
            }
        }

        // :187 — rooms until at least 2 exist: each pass rolls roomChance on every free
        // interior cell (the pass may overshoot — upstream identical).
        int tRoomCount = 0;
        while (tRoomCount < 2) {
            for (int i = 1; i < n - 1; i++) for (int j = 1; j < n - 1; j++) if (tLayout[i][j] == 0) {
                if (aRandom.nextInt(ROOM_CHANCE) == 0) {
                    tLayout[i][j] = ROOM_ID;
                    tRoomCount++;
                }
            }
        }

        // :189-193 — carve corridors: every non-zero cell walks toward the grid center,
        // first on i then on j, marking free cells as corridors.
        for (int i = 1; i < n - 1; i++) for (int j = 1; j < n - 1; j++) if (tLayout[i][j] != 0) {
            int a = i, b = j;
            while (a != n / 2) {
                a += a > n / 2 ? -1 : 1;
                if (tLayout[a][b] == 0) tLayout[a][b] = CORRIDOR; else break;
            }
            while (b != n / 2) {
                b += b > n / 2 ? -1 : 1;
                if (tLayout[a][b] == 0) tLayout[a][b] = CORRIDOR; else break;
            }
        }

        // :200-216 — pruning pass 1: dead-end corridors and the four diagonal-corner
        // cases drop out until stable.
        boolean temp = true;
        while (temp) {
            temp = false;
            for (int i = 1; i < n - 1; i++) for (int j = 1; j < n - 1; j++) if (tLayout[i][j] == CORRIDOR) {
                if (tLayout[i + 1][j] != 0 && tLayout[i - 1][j] != 0 && tLayout[i][j - 1] == 0 && tLayout[i][j + 1] == 0) continue;
                if (tLayout[i + 1][j] == 0 && tLayout[i - 1][j] == 0 && tLayout[i][j - 1] != 0 && tLayout[i][j + 1] != 0) continue;

                int tConnectionCount = 0;
                for (byte tSide : SIDES) if (tLayout[i + OFF_X[tSide]][j + OFF_Z[tSide]] != 0) tConnectionCount++;

                if (tConnectionCount <= 1) {
                    tLayout[i][j] = 0;
                    temp = true;
                    continue;
                }

                if (tLayout[i + 1][j] != 0 && tLayout[i + 1][j + 1] != 0 && tLayout[i][j + 1] != 0 && tLayout[i - 1][j] == 0 && tLayout[i][j - 1] == 0) {
                    tLayout[i][j] = 0;
                    temp = true;
                    continue;
                }
                if (tLayout[i + 1][j] != 0 && tLayout[i + 1][j - 1] != 0 && tLayout[i][j - 1] != 0 && tLayout[i - 1][j] == 0 && tLayout[i][j + 1] == 0) {
                    tLayout[i][j] = 0;
                    temp = true;
                    continue;
                }
                if (tLayout[i - 1][j] != 0 && tLayout[i - 1][j + 1] != 0 && tLayout[i][j + 1] != 0 && tLayout[i + 1][j] == 0 && tLayout[i][j - 1] == 0) {
                    tLayout[i][j] = 0;
                    temp = true;
                    continue;
                }
                if (tLayout[i - 1][j] != 0 && tLayout[i - 1][j - 1] != 0 && tLayout[i][j - 1] != 0 && tLayout[i + 1][j] == 0 && tLayout[i][j + 1] == 0) {
                    tLayout[i][j] = 0;
                    temp = true;
                    continue;
                }
            }
        }

        // :217-248 — pruning pass 2: adds the diagonal-adjacency count (>= 7 and the
        // == 5 three-in-a-row cases).
        temp = true;
        while (temp) {
            temp = false;
            for (int i = 1; i < n - 1; i++) for (int j = 1; j < n - 1; j++) if (tLayout[i][j] == CORRIDOR) {
                if (tLayout[i + 1][j] != 0 && tLayout[i - 1][j] != 0 && tLayout[i][j - 1] == 0 && tLayout[i][j + 1] == 0) continue;
                if (tLayout[i + 1][j] == 0 && tLayout[i - 1][j] == 0 && tLayout[i][j - 1] != 0 && tLayout[i][j + 1] != 0) continue;

                int tConnectionCount = 0;
                for (byte tSide : SIDES) if (tLayout[i + OFF_X[tSide]][j + OFF_Z[tSide]] != 0) tConnectionCount++;

                if (tConnectionCount <= 1) {
                    tLayout[i][j] = 0;
                    temp = true;
                    continue;
                }

                if (tLayout[i + 1][j + 1] != 0) tConnectionCount++;
                if (tLayout[i + 1][j - 1] != 0) tConnectionCount++;
                if (tLayout[i - 1][j + 1] != 0) tConnectionCount++;
                if (tLayout[i - 1][j - 1] != 0) tConnectionCount++;

                if (tConnectionCount >= 7) {
                    tLayout[i][j] = 0;
                    temp = true;
                    continue;
                }

                if (tConnectionCount == 5) {
                    if (tLayout[i + 1][j - 1] == 0 && tLayout[i + 1][j] == 0 && tLayout[i + 1][j + 1] == 0) {
                        tLayout[i][j] = 0;
                        temp = true;
                        continue;
                    }
                    if (tLayout[i - 1][j - 1] == 0 && tLayout[i - 1][j] == 0 && tLayout[i - 1][j + 1] == 0) {
                        tLayout[i][j] = 0;
                        temp = true;
                        continue;
                    }
                    if (tLayout[i - 1][j + 1] == 0 && tLayout[i][j + 1] == 0 && tLayout[i + 1][j + 1] == 0) {
                        tLayout[i][j] = 0;
                        temp = true;
                        continue;
                    }
                    if (tLayout[i - 1][j - 1] == 0 && tLayout[i][j - 1] == 0 && tLayout[i + 1][j - 1] == 0) {
                        tLayout[i][j] = 0;
                        temp = true;
                        continue;
                    }
                }

                if (tLayout[i + 1][j] != 0 && tLayout[i + 1][j + 1] != 0 && tLayout[i][j + 1] != 0 && tLayout[i - 1][j] == 0 && tLayout[i][j - 1] == 0) {
                    tLayout[i][j] = 0;
                    temp = true;
                    continue;
                }
                if (tLayout[i + 1][j] != 0 && tLayout[i + 1][j - 1] != 0 && tLayout[i][j - 1] != 0 && tLayout[i - 1][j] == 0 && tLayout[i][j + 1] == 0) {
                    tLayout[i][j] = 0;
                    temp = true;
                    continue;
                }
                if (tLayout[i - 1][j] != 0 && tLayout[i - 1][j + 1] != 0 && tLayout[i][j + 1] != 0 && tLayout[i + 1][j] == 0 && tLayout[i][j - 1] == 0) {
                    tLayout[i][j] = 0;
                    temp = true;
                    continue;
                }
                if (tLayout[i - 1][j] != 0 && tLayout[i - 1][j - 1] != 0 && tLayout[i][j - 1] != 0 && tLayout[i + 1][j] == 0 && tLayout[i][j + 1] == 0) {
                    tLayout[i][j] = 0;
                    temp = true;
                    continue;
                }
            }
        }

        return tLayout;
    }

    /** The horizontal side walk order — east, west, north, south (upstream ALL_SIDES_HORIZONTAL heads). */
    static final byte[] SIDES = {0, 1, 2, 3};
    /** The per-side x offsets for {@link #SIDES} (+1, -1, 0, 0). */
    static final int[] OFF_X = {1, -1, 0, 0};
    /** The per-side z offsets for {@link #SIDES} (0, 0, +1, -1). */
    static final int[] OFF_Z = {0, 0, 1, -1};

    /** The 4-neighbor occupied count (upstream :253-254) — the dead-end test for the pool dispatch. */
    public static int connectionCount(byte[][] aLayout, int aI, int aJ) {
        int rCount = 0;
        for (byte tSide : SIDES) if (aLayout[aI + OFF_X[tSide]][aJ + OFF_Z[tSide]] != 0) rCount++;
        return rCount;
    }

    /** Is the neighbor cell toward {@link net.minecraft.core.Direction} {@code aSide} occupied (the door face)? */
    public static boolean connected(byte[][] aLayout, int aI, int aJ, net.minecraft.core.Direction aSide) {
        return switch (aSide) {
            case EAST -> aLayout[aI + 1][aJ] != 0;
            case WEST -> aLayout[aI - 1][aJ] != 0;
            case SOUTH -> aLayout[aI][aJ + 1] != 0;
            case NORTH -> aLayout[aI][aJ - 1] != 0;
            default -> false;
        };
    }

    private GT6DungeonLayout() {
    }
}
