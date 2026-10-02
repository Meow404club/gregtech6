/**
 * Tests for task small-gem-1pct: the RANDOM_SMALL_GEM_ORE 1% stone-layer fallback core —
 * the pool census/order (the upstream MATERIAL_ARRAY declaration order), the verbatim
 * 1/100 roll sequence (miss → one nextInt(100), hit → one nextInt(100) + one
 * nextInt(61)), the same-seed replay, and the switch-off zero-change face.
 *
 * <p>The upstream three-branch order (layer ore → 1% gem → plain stone,
 * WorldgenStoneLayers.java:112-116/:165-169) maps onto this port as: the layer-ore arm is
 * the ore_lens companion JSON chain riding AFTER the lens feature (the GT6LensOreTest
 * FeatureSorter chain pin, mutual exclusion via the stone-ore-replaceables host gate),
 * while the gem/plain-stone arms are {@link GT6SmallGemFallback#roll} — pinned here.
 *
 * <p>Offline-safe by construction: initMaterials + the vanilla bootstrap bracket only
 * (the core touches no vanilla class).
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTMaterialItems;

class GT6SmallGemFallbackTest {

    @BeforeAll
    static void boot() {
        GTMaterialItems.initMaterials();
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** A Random that records its bounded draws (the sequence-contract witness). */
    private static final class CountingRandom extends Random {
        final List<Integer> bounds = new ArrayList<>();
        @Override public int nextInt(int aBound) {
            bounds.add(aBound);
            return super.nextInt(aBound);
        }
    }

    /** The supplier resolve with the pool's own alias merge (the pool() walk shape). */
    private static OreDictMaterial resolve(Supplier<OreDictMaterial> aSupplier) {
        OreDictMaterial tMaterial = aSupplier.get();
        if (tMaterial == null || tMaterial.mID < 0) return null;
        return MaterialRegistry.INSTANCE.get(tMaterial);
    }

    // ---------------------------------------------------------------- the pool

    /**
     * THE pool pin: exactly 61 members (the r7-b census pool_exact_61), ascending mID =
     * the upstream MATERIAL_ARRAY declaration order (the pick order), and the set equals
     * the port tables' union — GEM_POOL_ORES (56) + the STONE_LAYER_ORES members carrying
     * the flag (5: Peridot/Uvarovite/Grossular/Spinel/BalasRuby) — the card's cross-check
     * of the pool against the port axis tables. Every member is axis-placed (the small
     * block faces exist).
     */
    @Test
    void poolCensusOrderAndTableUnionIsPinned() {
        List<OreDictMaterial> tPool = GT6SmallGemFallback.pool();
        assertEquals(61, tPool.size(), "the r7-b pool_exact_61 census");
        for (int i = 1; i < tPool.size(); i++) {
            assertTrue(tPool.get(i - 1).mID < tPool.get(i).mID,
                    "the pick order is the ascending-mID upstream declaration order, violation at " + i);
        }
        // the port-tables union
        Set<OreDictMaterial> tUnion = new HashSet<>();
        int tStoneLayerOverlaps = 0;
        for (Supplier<OreDictMaterial> tSupply : GT6OreBlocks.GEM_POOL_ORES) {
            OreDictMaterial tMaterial = resolve(tSupply);
            assertNotNull(tMaterial, "every GEM_POOL_ORES supplier resolves");
            assertTrue(tMaterial.contains(TD.Properties.RANDOM_SMALL_GEM_ORE), "GEM_POOL_ORES member carries the flag");
            tUnion.add(tMaterial);
        }
        for (Supplier<OreDictMaterial> tSupply : GT6OreBlocks.STONE_LAYER_ORES) {
            OreDictMaterial tMaterial = resolve(tSupply);
            assertNotNull(tMaterial, "every STONE_LAYER_ORES supplier resolves");
            if (tMaterial.contains(TD.Properties.RANDOM_SMALL_GEM_ORE)) {
                tStoneLayerOverlaps++;
                tUnion.add(tMaterial);
            }
        }
        assertEquals(5, tStoneLayerOverlaps, "the 5 stone-layer companions inside the pool (Peridot/Uvarovite/Grossular/Spinel/BalasRuby)");
        assertEquals(new HashSet<>(tPool), tUnion, "pool == the port tables' union, no member lost or invented");
        // every pick is placeable: the member sits on the block axis
        List<OreDictMaterial> tAxis = GT6OreBlocks.materialAxis();
        for (OreDictMaterial tMember : tPool) {
            assertTrue(tAxis.contains(tMember), "pool member on the 122 axis: " + tMember.mNameInternal);
        }
    }

    // ---------------------------------------------------------------- the roll sequence

    /**
     * THE verbatim sequence pin: a miss consumes exactly one draw and it is nextInt(100);
     * a hit consumes exactly two — nextInt(100) then nextInt(pool.size()); the pick is the
     * pool member at that second draw's index (recomputed independently from the same
     * seed). Same seed ⇒ same pick (the seeded replay face).
     */
    @Test
    void rollSequenceIsTheUpstreamForm() {
        List<OreDictMaterial> tPool = GT6SmallGemFallback.pool();
        int tMissSeed = -1, tHitSeed = -1;
        for (int tSeed = 0; tSeed < 100_000 && (tMissSeed < 0 || tHitSeed < 0); tSeed++) {
            boolean tHit = new Random(tSeed).nextInt(100) == 0;
            if (tHit && tHitSeed < 0) tHitSeed = tSeed;
            if (!tHit && tMissSeed < 0) tMissSeed = tSeed;
        }
        assertTrue(tMissSeed >= 0 && tHitSeed >= 0, "both branches exist within the scan window");

        // the miss arm: one draw, bound 100, null result (plain stone)
        CountingRandom tMiss = new CountingRandom();
        tMiss.setSeed(tMissSeed);
        assertNull(GT6SmallGemFallback.roll(tMiss), "a miss returns null = the plain-stone arm");
        assertEquals(List.of(100), tMiss.bounds, "a miss spends exactly one nextInt(100)");

        // the hit arm: two draws, 100 then 61, and the pick index is the second draw
        CountingRandom tHit = new CountingRandom();
        tHit.setSeed(tHitSeed);
        OreDictMaterial tPick = GT6SmallGemFallback.roll(tHit);
        assertNotNull(tPick, "a hit returns the pool pick");
        assertEquals(List.of(100, tPool.size()), tHit.bounds, "a hit spends nextInt(100) then nextInt(pool.size())");
        Random tProbe = new Random(tHitSeed);
        assertEquals(0, tProbe.nextInt(100), "the hit seed's first draw is the 1/100 face");
        assertSame(tPool.get(tProbe.nextInt(tPool.size())), tPick, "the pick is pool[second draw]");

        // the replay face: the same seed re-derives the identical pick
        assertSame(tPick, GT6SmallGemFallback.roll(new Random(tHitSeed)), "same seed replays the same pick");
    }

    /**
     * The switch-off zero-change pin: no draw consumed at all, null result — the stone
     * write stays byte-identical (the upstream short-circuit order, the empty-pool face
     * included by construction since the gate precedes the pool read's roll).
     */
    @Test
    void switchOffConsumesNothing() {
        GT6SmallGemFallback.RANDOM_SMALL_GEM_ORES = false;
        try {
            CountingRandom tRandom = new CountingRandom();
            assertNull(GT6SmallGemFallback.roll(tRandom), "off = the plain-stone arm, unconditionally");
            assertTrue(tRandom.bounds.isEmpty(), "off spends zero draws");
            // the pool itself is untouched by the switch (the config-fill ↔ roll-gate split:
            // upstream fills the pool at load :879 and gates the roll on !isEmpty() at :112)
            assertEquals(61, GT6SmallGemFallback.pool().size());
        } finally {
            GT6SmallGemFallback.RANDOM_SMALL_GEM_ORES = true;
        }
    }
}
