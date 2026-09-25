/**
 * Tests for task p30-w6-t3-large-veins: the 40-row vein table, the draw/validity
 * semantics, the codec roundtrip and the per-chunk slice tiling — the acceptance's
 * offline audit unit (the GT6WorldgenDatagenTest posture).
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree
 * or a conscious decision is forced): Loader_Worldgen.java:886-925 (the 40 vein rows),
 * WorldgenOresLarge.java:88-131 (the generation algorithm + the :90 distance gate +
 * the :92 tMinY draw + the :111-128 shape rolls), GT6WorldGenerator.java:93-105 (the
 * weighted draw + the 3x3 origin grid), WD.java:547-560 (the stream seeding).
 *
 * <p>The tiling test IS the offline leg of the cross-chunk bit-consistency acceptance:
 * per-chunk slices over the whole neighborhood must be pairwise disjoint and must
 * concatenate to the full-vein clip — two chunks deriving the same vein therefore
 * never double-write and never leave a seam.
 *
 * <p>Offline-safe by construction: initMaterials + the vanilla bootstrap bracket only.
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import gregapi.oredict.OreDictMaterial;
import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.worldgen.GT6VeinGenerator.SliceSink;

class GT6LargeVeinTest {

    /** The snake ids of the 40 table rows, Loader_Worldgen.java:886-925 order. */
    private static final List<String> NAMES = List.of(
        "ore.large.lignite", "ore.large.coal", "ore.large.apatite", "ore.large.lapis", "ore.large.bauxite",
        "ore.large.iodinesalt", "ore.large.rocksalt", "ore.large.asbestos", "ore.large.sapphire", "ore.large.sapphire2",
        "ore.large.garnet", "ore.large.pitchblende", "ore.large.monazite", "ore.large.diamond", "ore.large.galena",
        "ore.large.quartz", "ore.large.peridot", "ore.large.gold", "ore.large.platinum", "ore.large.molybdenum",
        "ore.large.cassiterite", "ore.large.tungstate", "ore.large.manganese", "ore.large.beryllium", "ore.large.beryllium2",
        "ore.large.titanium", "ore.large.nickel", "ore.large.redstone", "ore.large.tetrahedrite", "ore.large.iron",
        "ore.large.copper", "ore.large.adamantium", "ore.large.naquadah", "ore.large.trinium", "ore.large.dolamide",
        "ore.large.moonmars", "ore.large.cheese", "ore.large.desh", "ore.large.syrmorite", "ore.large.octine");

    /** The rows drawable overworld under the registered-universe gate (the mInvalid gate: >= 1 valid slot). */
    private static final List<String> DRAWABLE = List.of(
        "ore.large.lignite", "ore.large.coal", "ore.large.lapis", "ore.large.iodinesalt", "ore.large.rocksalt",
        "ore.large.asbestos", "ore.large.diamond", "ore.large.galena", "ore.large.gold", "ore.large.platinum",
        "ore.large.cassiterite", "ore.large.tungstate", "ore.large.manganese", "ore.large.nickel", "ore.large.redstone",
        "ore.large.tetrahedrite", "ore.large.iron", "ore.large.copper");

    /** The valid slots' weights, DRAWABLE order — the draw mass the GT6WorldGenerator.java:93 sum produces. */
    private static final int DRAWABLE_WEIGHT_SUM = 1110;

    @BeforeAll
    static void boot() {
        // the material system before MT dereferences; the vanilla bootstrap bracket for the
        // codec/JsonOps classes (the GT6WorldgenDatagenTest posture, offline throwables ignored)
        GTMaterialItems.initMaterials();
        // the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** The 40-row census: names in Loader order, 31 overworld rows, ranges drawable, spot numbers verbatim. */
    @Test
    void veinTableCensusIsPinned() {
        List<GTVeinConfig> tTable = GT6WorldgenDatagen.LARGE_VEIN_TABLE;
        assertEquals(40, tTable.size(), "the card spec ② pin: 40 rows = Loader_Worldgen.java:886-925 verbatim");
        for (int i = 0; i < NAMES.size(); i++) {
            assertEquals(NAMES.get(i), tTable.get(i).name(), "row " + i + " must stay in Loader order");
        }
        assertEquals(31, tTable.stream().filter(GTVeinConfig::overworld).count(),
                "the :886-916 rows list ORE_OVERWORLD (31); the :917-925 Mars/End/Moon/BL rows do not (9)");
        for (GTVeinConfig tVein : tTable) {
            assertTrue(tVein.maxY() >= tVein.minY() + 6, tVein.name() + " must leave nextInt(maxY-minY-5) >= 1");
            assertTrue(tVein.indicator(), "every upstream row carries the indicator flag T");
            assertEquals(0, tVein.spawnDistance(), "no upstream row passes the DistanceFromSpawn arg");
        }
        GTVeinConfig tLignite = tTable.get(0);
        assertEquals(50, tLignite.minY());
        assertEquals(130, tLignite.maxY());
        assertEquals(160, tLignite.weight());
        assertEquals(8, tLignite.density());
        assertEquals(32, tLignite.size());
        assertEquals("Lignite", tLignite.oreTop().mNameInternal);
        assertEquals("Coal", tLignite.oreSpread().mNameInternal);
        GTVeinConfig tIron = tTable.get(29);
        assertEquals(120, tIron.weight());
        assertEquals("BrownLimonite", tIron.oreTop().mNameInternal);
        assertEquals("Hematite", tIron.oreBetween().mNameInternal, "MT.Fe2O3 serializes as the canonical Hematite");
        GTVeinConfig tCopper = tTable.get(30);
        assertEquals(80, tCopper.weight());
        assertEquals("Copper", tCopper.oreSpread().mNameInternal);
        GTVeinConfig tOctine = tTable.get(39);
        assertFalse(tOctine.overworld(), "octine is Betweenlands-only");
    }

    /** The draw gate + the indicator universe: 18 drawable rows, 1110 total weight, the 31 distinct valid slots. */
    @Test
    void drawableRowsAndIndicatorUniverseMatch() {
        List<GTVeinConfig> tTable = GT6WorldgenDatagen.LARGE_VEIN_TABLE;
        List<String> tDrawableNames = new ArrayList<>();
        int tWeightSum = 0;
        List<String> tValidSlotNames = new ArrayList<>(); // first-encounter order over the table walk
        for (GTVeinConfig tVein : tTable) {
            boolean tAnyValid = false;
            // only OVERWORLD rows feed the indicator universe — the naquadah/trinium/
            // dolamide slots are valid but their rows never draw overworld
            for (OreDictMaterial tSlot : (tVein.overworld()
                    ? List.of(tVein.oreTop(), tVein.oreBottom(), tVein.oreBetween(), tVein.oreSpread())
                    : List.<OreDictMaterial>of())) {
                if (!GT6VeinGenerator.valid(tSlot)) continue;
                tAnyValid = true;
                if (!tValidSlotNames.contains(tSlot.mNameInternal)) tValidSlotNames.add(tSlot.mNameInternal);
            }
            if (tVein.overworld() && tAnyValid) {
                tDrawableNames.add(tVein.name());
                tWeightSum += tVein.weight();
            }
        }
        assertEquals(DRAWABLE, tDrawableNames, "the draw list = overworld rows with >= 1 axis-valid slot (the mInvalid gate)");
        assertEquals(DRAWABLE_WEIGHT_SUM, tWeightSum, "the tMaxWeight mass of the drawable rows");
        assertEquals(31, tValidSlotNames.size(), "31 distinct valid slots across the OVERWORLD rows (the indicator universe)");
        assertEquals(tValidSlotNames, GT6SurfaceBlocks.INDICATOR_MATERIALS.stream().map(tSupply -> tSupply.get().mNameInternal).toList(),
                "the spec ⑤ indicator rock set = the table's distinct valid slots, first-encounter order");
    }

    /** The weighted draw is exactly-one with the upstream countdown semantics; the deterministic core replays identically. */
    @Test
    void drawAndSliceReplayIsDeterministic() {
        // the grid predicate: GT6WorldGenerator.java:97 verbatim (1 and -2 are cells, 0 is not)
        assertTrue(GT6VeinGenerator.isOriginCell(1));
        assertTrue(GT6VeinGenerator.isOriginCell(-2));
        assertFalse(GT6VeinGenerator.isOriginCell(0));

        long tSeed = 6131000569321125127L; // the card's RCON probe seed — the same one live
        RecordingSink tFirst = sliceForOrigin(tSeed, 1, 1);
        RecordingSink tSecond = sliceForOrigin(tSeed, 1, 1);
        assertEquals(tFirst.ores, tSecond.ores, "the same (seed, origin) must recompute the identical ore slice");
        assertEquals(tFirst.rocks, tSecond.rocks, "the indicator draws/placements must replay identically too");
        assertTrue(tFirst.ores.size() > 0, "an origin-cell slice at this seed actually places (the probe face)");
        assertTrue(GT6VeinGenerator.isOriginCell(4), "precondition: a second origin cell exists in range");
        assertTrue(sliceForOrigin(tSeed, 4, 1).ores.size() > 0, "a second origin cell also places");
    }

    /** THE cross-chunk proof: per-chunk slices over the neighborhood are disjoint and tile the full-vein clip. */
    @Test
    void perChunkSlicesTileTheFullVein() {
        long tSeed = 6131000569321125127L;
        int tOriginX = 1, tOriginZ = 1; // an origin cell
        RecordingSink tFull = new RecordingSink();
        generateAt(tSeed, tOriginX, tOriginZ, tFull, -1_000_000, 1_000_000, -1_000_000, 1_000_000);

        // every work chunk within reach recomputes the same vein and writes only its own clip
        List<Place> tUnion = new ArrayList<>();
        for (int tWX = tOriginX - 2; tWX <= tOriginX + 2; tWX++) {
            for (int tWZ = tOriginZ - 2; tWZ <= tOriginZ + 2; tWZ++) {
                RecordingSink tSlice = new RecordingSink();
                generateAt(tSeed, tOriginX, tOriginZ, tSlice, tWX << 4, (tWX << 4) + 15, tWZ << 4, (tWZ << 4) + 15);
                for (Place tPlace : tSlice.ores) {
                    assertFalse(tUnion.contains(tPlace), "a column block written by two work chunks = a double-write seam");
                    assertTrue(tPlace.x >= (tWX << 4) && tPlace.x <= ((tWX << 4) + 15), "the slice stayed inside its own chunk (x)");
                    assertTrue(tPlace.z >= (tWZ << 4) && tPlace.z <= ((tWZ << 4) + 15), "the slice stayed inside its own chunk (z)");
                    tUnion.add(tPlace);
                }
            }
        }
        assertEquals(tFull.ores.size(), tUnion.size(), "the chunk clips must cover every full-vein block exactly once");
        // and the union content equals the full clip (order-free comparison via the same stream replay)
        List<Place> tRemaining = new ArrayList<>(tFull.ores);
        for (Place tPlace : tUnion) tRemaining.remove(tPlace);
        assertTrue(tRemaining.isEmpty(), "union content != full clip");
    }

    /** The vertical layer semantics (WorldgenOresLarge.java:113-128) on a fully-materialized vein slice. */
    @Test
    void sliceCarriesTheUpstreamVerticalBands() {
        long tSeed = 6131000569321125127L;
        RecordingSink tSink = sliceForOrigin(tSeed, 1, 1);
        assertFalse(tSink.ores.isEmpty(), "precondition: a vein slice exists");
        int tMinY = tSink.ores.stream().mapToInt(tPlace -> tPlace.y).min().getAsInt() + 1; // the bottom band starts at tMinY-1
        for (Place tPlace : tSink.ores) {
            assertTrue(tPlace.y >= tMinY - 1 && tPlace.y <= tMinY + 5,
                    tPlace.mat + " at " + tPlace.y + " outside the upstream band [tMinY-1, tMinY+5] (tMinY=" + tMinY + ")");
        }
    }

    /** The spawn-distance gate: chunks at/below the distance write nothing, open gates are write-transparent (the :90 verbatim face). */
    @Test
    void spawnDistanceGateExcludesTheNearChunks() {
        // the SAME geometry run twice: once with the gate, once without — an open gate must
        // be write-transparent, a closed gate must suppress everything
        // the materials are read INLINE (post-initMaterials): a static capture would freeze
        // the pre-init MT slot at test-class-load time — the GTStoneBlocksRegistrationTest lesson
        GTVeinConfig tGated = new GTVeinConfig("test.vein", 20, 40, 10, 4, 24, 32, false, true, false,
                gregapi.data.MT.Cu, gregapi.data.MT.Cu, gregapi.data.MT.Cu, gregapi.data.MT.Cu);
        GTVeinConfig tUngated = new GTVeinConfig("test.vein", 20, 40, 10, 4, 24, 0, false, true, false,
                gregapi.data.MT.Cu, gregapi.data.MT.Cu, gregapi.data.MT.Cu, gregapi.data.MT.Cu);
        // the origin IS each work chunk (chunk min = the origin min block) — the rectangle
        // [min-rand .. min+16+rand] then covers its own chunk for every draw, so the only
        // variable left is the gate
        RecordingSink tNear = new RecordingSink();
        GT6VeinGenerator.generateSlice(tGated, new Random(42), -16, -16, -16, -1, -16, -1, -64, tNear);
        assertTrue(tNear.ores.isEmpty(), "a work chunk within DistanceFromSpawn writes nothing");
        RecordingSink tFarGated = new RecordingSink();
        GT6VeinGenerator.generateSlice(tGated, new Random(42), 64, 64, 64, 79, 64, 79, -64, tFarGated);
        RecordingSink tFarFree = new RecordingSink();
        GT6VeinGenerator.generateSlice(tUngated, new Random(42), 64, 64, 64, 79, 64, 79, -64, tFarFree);
        assertEquals(tFarFree.ores, tFarGated.ores, "the open gate is write-transparent");
        RecordingSink tNearFree = new RecordingSink();
        GT6VeinGenerator.generateSlice(tUngated, new Random(42), -16, -16, -16, -1, -16, -1, -64, tNearFree);
        assertTrue(tNearFree.ores.size() > 0, "without the gate the near chunk writes (the geometry covers it)");
    }

    /** The configured-feature config codec roundtrips through JSON (the datapack face the KJS surface edits). */
    @Test
    void veinTableCodecRoundtripsThroughJson() {
        GTVeinConfig.Table tTable = new GTVeinConfig.Table(GT6WorldgenDatagen.LARGE_VEIN_TABLE);
        var tJson = GTVeinConfig.Table.CODEC.encodeStart(JsonOps.INSTANCE, tTable).result().orElseThrow();
        GTVeinConfig.Table tBack = GTVeinConfig.Table.CODEC.parse(JsonOps.INSTANCE, tJson).result().orElseThrow();
        assertEquals(tTable, tBack, "the codec roundtrip must preserve the whole 40-row table");
        assertEquals(40, tBack.veins().size());
    }

    // ---------------------------------------------------------------- the harness

    private record Place(int x, int y, int z, String mat) {}

    /** The offline sink: records placements, answers reads like an idealized solid world. */
    private static final class RecordingSink implements SliceSink {
        final List<Place> ores = new ArrayList<>();
        final List<Place> rocks = new ArrayList<>();

        @Override public void ore(int aX, int aY, int aZ, OreDictMaterial aMaterial) {
            ores.add(new Place(aX, aY, aZ, aMaterial.mNameInternal));
        }

        @Override public void indicatorRock(int aX, int aY, int aZ, OreDictMaterial aMaterial) {
            rocks.add(new Place(aX, aY, aZ, aMaterial == null ? "Stone" : aMaterial.mNameInternal));
        }

        @Override public int surfaceHeight(int aX, int aZ) {
            return 200; // above every band: the scan starts at min(200, tMinY+25), the upstream cap shape
        }

        @Override public int probe(int aX, int aY, int aZ) {
            return PROBE_SUITABLE; // idealized solid ground — the scan lands at its band cap
        }

        @Override public boolean replaceable(int aX, int aY, int aZ) {
            return true;
        }
    }

    /** One full origin pipeline: the WD.random stream, the weighted draw, the slice into the given clip. */
    private static RecordingSink sliceForOrigin(long aSeed, int aOriginX, int aOriginZ) {
        RecordingSink tSink = new RecordingSink();
        generateAt(aSeed, aOriginX, aOriginZ, tSink, -1_000_000, 1_000_000, -1_000_000, 1_000_000);
        return tSink;
    }

    private static void generateAt(long aSeed, int aOriginX, int aOriginZ, RecordingSink aSink,
            int aClipMinX, int aClipMaxX, int aClipMinZ, int aClipMaxZ) {
        Random tRandom = GT6VeinGenerator.veinRandom(aSeed, 0, aOriginX, aOriginZ); // the overworld salt
        GTVeinConfig tVein = GT6VeinGenerator.drawVein(GT6WorldgenDatagen.LARGE_VEIN_TABLE, tRandom);
        if (tVein == null) return;
        GT6VeinGenerator.generateSlice(tVein, tRandom, aOriginX << 4, aOriginZ << 4,
                aClipMinX, aClipMaxX, aClipMinZ, aClipMaxZ, -64, aSink);
    }
}
