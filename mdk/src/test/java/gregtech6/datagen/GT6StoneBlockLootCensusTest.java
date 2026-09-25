/**
 * Offline census for task p21-chisel-drop-conversion: the chisel mining-drop face of the
 * GT6 stone loot tables (the GT_Tool_Chisel.java:73-77 arm landed as a loot dispatch —
 * the ADR ruling, tmp/adr-drafts/2026-09-07-p21-chisel-drop-conversion-ruling.md).
 *
 * <p>Pins, all offline-safe (the decision statics are registry-free; no table is BUILT
 * here — the generated-JSON side is pinned by GT6StoneBlocksRenderDatagenTest against the
 * committed src/generated tree, and the live face by the chisel_drops.py RCON chain):
 * <ul>
 * <li>the 170/102 yardstick: 10 non-identity mappings x 17 stones = the rewritten
 *     dispatch tables, 6 identity mappings x 17 stones = the pass-through tables
 *     (272 total);</li>
 * <li>the mapping transcription per variant against the BlockStones.java:81 literal
 *     (0→7, 3→4, 4→1, 5→2, 7→6, 10→11, 12→11, 13↔14, 15→11; identity {1,2,6,8,9,11});</li>
 * <li>the :731 baseline co-rule (variant 0 yields the SAME STONE's COBBL item, the rest
 *     self) and the dispatch/baseline disagreement — a non-identity variant always
 *     converts to a DIFFERENT item than the baseline, which is exactly what makes its
 *     table need the match_tool branch.</li>
 * </ul>
 *
 * <p>Compile anchors: GTStoneBlocksRegistrationTest:163 pins the CHISEL_MAPPINGS byte
 * table itself (untouched here); the mapping tables are stone-blind, so one per-variant
 * pin covers all 17 stones.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;

class GT6StoneBlockLootCensusTest {

    /** The p21 yardstick: 10 non-identity chisel mappings x 17 stones = the rewritten tables. */
    private static final int PINNED_DISPATCH_TABLES = 170;
    /** The p21 yardstick: 6 identity mappings x 17 stones = the pass-through tables. */
    private static final int PINNED_PASSTHROUGH_TABLES = 102;

    /**
     * BlockStones.java:81 — CHISEL_MAPPINGS = {SMOTH, COBBL, MCOBL, CRACK, COBBL, MCOBL,
     * CHISL, CHISL, RNFBR, RSTBR, STILE, STILE, STILE, WINDB, WINDA, STILE} (BlockStones.java:71-77
     * meta constants) split into the non-identity rows, meta -> target meta.
     */
    private static final Map<Integer, Integer> NON_IDENTITY = Map.of(
        0, 7,   // STONE  -> SMOTH
        3, 4,   // BRICK  -> CRACK
        4, 1,   // CRACK  -> COBBL
        5, 2,   // MBRIK  -> MCOBL
        7, 6,   // SMOTH  -> CHISL
        10, 11, // TILES  -> STILE
        12, 11, // SBRIK  -> STILE
        13, 14, // WINDA  -> WINDB
        14, 13, // WINDB  -> WINDA
        15, 11);// QBRIK  -> STILE

    /** The identity rows — the chisel drop IS the :731 loot baseline, no dispatch needed. */
    private static final Set<Integer> IDENTITY = Set.of(1, 2, 6, 8, 9, 11); // COBBL MCOBL CHISL RNFBR RSTBR STILE

    @BeforeAll
    static void boot() {
        // the GTStoneBlocksRegistrationTest recipe: the material system before any MT/OP
        // dereference, bootstrap for the vanilla classes the provider chain class-loads
        GTMaterialItems.initMaterials();
        // the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** The 170/102 yardstick walked over the 272-pair registration census. */
    @Test
    void chiselDispatchCensusIsPinned() {
        int tDispatch = 0;
        int tPassThrough = 0;
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            if (GT6LootTables.GT6StoneBlockLoot.chiselTarget(tKey.variant()) == null) {
                tPassThrough++;
            } else {
                tDispatch++;
            }
        }
        assertEquals(PINNED_DISPATCH_TABLES, tDispatch, "10 non-identity mappings x 17 stones");
        assertEquals(PINNED_PASSTHROUGH_TABLES, tPassThrough, "6 identity mappings x 17 stones");
        assertEquals(272, tDispatch + tPassThrough, "the tables partition the 272-pair census");
    }

    /** Every variant's chisel target against the BlockStones.java:81 literal, per variant. */
    @Test
    void perVariantChiselTargetsMatchTheUpstreamLiteral() {
        assertEquals(16, NON_IDENTITY.size() + IDENTITY.size(), "every meta is accounted for exactly once");
        for (StoneVariant tVariant : StoneVariant.VALUES) {
            StoneVariant tTarget = GT6LootTables.GT6StoneBlockLoot.chiselTarget(tVariant);
            if (IDENTITY.contains((int) tVariant.meta())) {
                assertNull(tTarget, tVariant + " is identity — the table stays pass-through");
            } else {
                assertNotNull(tTarget, tVariant + " is non-identity — the table carries the dispatch");
                assertEquals(StoneVariant.VALUES[NON_IDENTITY.get((int) tVariant.meta())], tTarget,
                        tVariant + " -> the BlockStones.java:81 row");
            }
        }
    }

    /**
     * The dispatch is only correct if the two arms DISAGREE: on every non-identity pair the
     * chisel drop must differ from the :731 baseline drop (a converted item is observable),
     * and the baseline itself follows the :731 rule (variant 0 the SAME STONE's cobble id,
     * the rest the pair's own id). The tables are stone-blind, so the id faces are walked
     * for all 17 stones.
     */
    @Test
    void dispatchArmsDisagreeWithTheBaselineForEveryStone() {
        int tChecked = 0;
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            StoneVariant tTarget = GT6LootTables.GT6StoneBlockLoot.chiselTarget(tKey.variant());
            String tSnake = tKey.stone().snake();
            if (tTarget == null) {
                // pass-through: the chisel drop is the baseline drop (same pair id, variant 0 never self-maps)
                assertEquals(GTStoneBlocks.path(tSnake, tKey.variant()),
                        GTStoneBlocks.path(tSnake, tKey.variant()), tKey + ": identity, nothing to convert");
                continue;
            }
            String tChiselPath = GTStoneBlocks.path(tSnake, tTarget);
            String tBaselinePath = tKey.variant() == StoneVariant.STONE
                    ? GTStoneBlocks.path(tSnake, StoneVariant.COBBL)
                    : GTStoneBlocks.path(tSnake, tKey.variant());
            assertNotEquals(tBaselinePath, tChiselPath,
                    tKey + ": a dispatch table only exists where conversion is observable");
            if (tKey.variant() == StoneVariant.STONE) {
                assertEquals(tSnake + "_" + StoneVariant.COBBL.snake, tBaselinePath,
                        tKey + ": the :731 swap — variant 0's baseline is the SAME STONE's cobble");
            }
            tChecked++;
        }
        assertEquals(PINNED_DISPATCH_TABLES, tChecked, "the disagreement face covers exactly the dispatch tables");
    }

    /** The RCON chain's live arms, pinned as id faces (marble, the chain's stone). */
    @Test
    void theChainArmsArePinnedIdFaces() {
        // positive: chisel mines gt6:marble_bricks -> CRACK (mapping 3->4)
        assertEquals("marble_bricks_cracked", GTStoneBlocks.path("marble",
                StoneVariant.VALUES[NON_IDENTITY.get(3)]));
        // positive: chisel mines gt6:marble -> SMOTH (mapping 0->7)
        assertEquals("marble_smooth", GTStoneBlocks.path("marble",
                StoneVariant.VALUES[NON_IDENTITY.get(0)]));
        // negative: bare hand on gt6:marble -> the :731 cobble swap
        assertEquals("marble_cobble", GTStoneBlocks.path("marble", StoneVariant.COBBL));
    }
}
