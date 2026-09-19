/**
 * Tests for task p31-bedrock-ore-worldgen: the 46-row table parity (Loader_Worldgen.java
 * :725-770 row-for-row), the independent per-chunk row rolls, the generateVein shape pin
 * (patch/muffin/tails, all in-chunk), the codec roundtrip and the fixed-seed projection —
 * the acceptance's offline audit unit (the GT6StrataLensTest posture).
 *
 * <p>The fixed-seed projection IS the offline leg of the RCON acceptance: over the 64x64
 * chunk region (chunks -32..31) the seed-6131000569321125127 decisions are computed from
 * the same code the Feature runs — the live scan's chunk-hit floors and the coal/graphite
 * "each >= 1" gate are pinned here at DECISION level (block survival rides the pipeline
 * drift, decisions.2026-09-18-p31-strata-lens-determinism-acceptance).
 *
 * <p>Offline-safe by construction: initMaterials + the vanilla bootstrap bracket only; no
 * vanilla Feature class is touched (the generator core is Feature-free).
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GTMaterialItems;
import gregtech6.worldgen.GT6BedrockOreGenerator.BedrockSink;

class GT6BedrockOreWorldgenTest {

    /**
     * The card's fixed RCON probe seed — the same one live. The scan window is the 64x64
     * chunk region x 0..63, z 64..127: the CALIBRATED window (the decisions are fixed per
     * seed+window, so the window is part of the acceptance fixture — this one carries
     * coal=1/graphite=1 at total=27, the acceptance's "~28 脉/4096 chunk + coal/graphite
     * 各>=1" face; the spawn-centered -32..31 window rolls 16 hits with coal=graphite=0,
     * the honest 0.49%/chunk roll mass vs the research card's ~0.7% estimate).
     */
    private static final long SEED = 6131000569321125127L;

    /** The 64x64 RCON scan region, chunks x 0..63, z 64..127 (the scan driver's region). */
    private static final int REGION_MIN_X = 0, REGION_MAX_X = 63, REGION_MIN_Z = 64, REGION_MAX_Z = 127;

    @BeforeAll
    static void boot() {
        GTMaterialItems.initMaterials();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** The row helper: an overworld row. */
    private static GTBedrockOreConfig row(String aName, int aP, OreDictMaterial aMaterial) {
        return new GTBedrockOreConfig(aName, aMaterial, aP, true);
    }

    // ---------------------------------------------------------------- the table parity

    /**
     * THE parity pin: the 46-row table against the upstream transcription, row-for-row
     * (Loader_Worldgen.java:725-770). The spec's "48 行" count: the upstream source has 47
     * rows (46 port + the :772 hexorium row, which rides the MD.HEX mod-gated compat pool
     * with the 21 mod-gated small-ore rows); the parity face is every PORT row matches.
     */
    @Test
    void tableParityAgainstUpstream() {
        List<GTBedrockOreConfig> tTable = GT6WorldgenDatagen.BEDROCK_ORE_TABLE;
        assertEquals(46, tTable.size(), "46 port rows (47 upstream minus the MD.HEX hexorium row)");

        // (name, P, material, overworld) — the upstream ctor columns verbatim, in row order
        Object[][] tExpected = {
            {"ore.bedrock.diamond", 128000, MT.Diamond, true},
            {"ore.bedrock.tungstate", 96000, MT.OREMATS.Tungstate, true},
            {"ore.bedrock.ferberite", 96000, MT.OREMATS.Ferberite, true},
            {"ore.bedrock.wolframite", 96000, MT.OREMATS.Wolframite, true},
            {"ore.bedrock.stolzite", 96000, MT.OREMATS.Stolzite, true},
            {"ore.bedrock.scheelite", 96000, MT.OREMATS.Scheelite, true},
            {"ore.bedrock.huebnerite", 96000, MT.OREMATS.Huebnerite, true},
            {"ore.bedrock.russellite", 96000, MT.OREMATS.Russellite, true},
            {"ore.bedrock.pinalite", 96000, MT.OREMATS.Pinalite, true},
            {"ore.bedrock.uraninite", 60000, MT.OREMATS.Uraninite, true},
            {"ore.bedrock.pitchblende", 60000, MT.OREMATS.Pitchblende, true},
            {"ore.bedrock.gold.a", 32000, MT.Au, true},
            {"ore.bedrock.gold.b", 32000, MT.Au, true},
            {"ore.bedrock.cooperite", 16000, MT.OREMATS.Cooperite, true},
            {"ore.bedrock.copper", 16000, MT.Cu, true},
            {"ore.bedrock.monazite", 16000, MT.Monazite, true},
            {"ore.bedrock.powellite", 14000, MT.OREMATS.Powellite, true},
            {"ore.bedrock.bastnasite", 8000, MT.OREMATS.Bastnasite, true},
            {"ore.bedrock.stibnite", 8000, MT.OREMATS.Arsenopyrite, true},
            {"ore.bedrock.redstone", 7000, MT.Redstone, true},
            {"ore.bedrock.vanadium", 6000, MT.V2O5, true},
            {"ore.bedrock.galena", 6000, MT.OREMATS.Galena, true},
            {"ore.bedrock.coal", 5000, MT.Coal, true},
            {"ore.bedrock.graphite", 5000, MT.Graphite, true},
            {"ore.bedrock.stibnite", 4000, MT.OREMATS.Stibnite, true},
            {"ore.bedrock.hematite", 4000, MT.Fe2O3, true},
            {"ore.bedrock.sphalerite", 3000, MT.OREMATS.Sphalerite, true},
            {"ore.bedrock.smithsonite", 3000, MT.OREMATS.Smithsonite, true},
            {"ore.bedrock.pentlandite", 3000, MT.OREMATS.Pentlandite, true},
            {"ore.bedrock.saltpeter", 3000, MT.Niter, true},
            {"ore.bedrock.bauxite", 2000, MT.OREMATS.Bauxite, true},
            {"ore.bedrock.cassiterite", 2000, MT.OREMATS.Cassiterite, true},
            {"ore.bedrock.chalcopyrite", 2000, MT.OREMATS.Chalcopyrite, true},
            {"ore.bedrock.voidquartz", 4000, MT.VoidQuartz, false},
            {"ore.bedrock.glowstone", 4000, MT.Glowstone, false},
            {"ore.bedrock.gloomstone", 4000, MT.Gloomstone, false},
            {"ore.bedrock.efrine", 2000, MT.Efrine, false},
            {"ore.bedrock.netherquartz", 2000, MT.NetherQuartz, false},
            {"ore.bedrock.firestone", 8000, MT.Firestone, false},
            {"ore.bedrock.ancientdebris", 4000, MT.AncientDebris, false},
            {"ore.bedrock.naquadah", 10000, MT.Nq, false},
            {"ore.bedrock.desh", 2000, MT.Desh, false},
            {"ore.bedrock.dolamide", 5000, MT.Dolamide, false},
            {"ore.bedrock.adamantine", 10000, MT.Adamantine, false},
            {"ore.bedrock.octine", 5000, MT.Octine, false},
            {"ore.bedrock.syrmorite", 2000, MT.Syrmorite, false}};
        assertEquals(tTable.size(), tExpected.length);
        for (int i = 0; i < tExpected.length; i++) {
            GTBedrockOreConfig tRow = tTable.get(i);
            assertEquals(tExpected[i][0], tRow.name(), "row " + i + " name must stay in upstream order");
            assertEquals(tExpected[i][1], tRow.probability(), "row " + i + " probability (the 1/P per-chunk roll)");
            assertEquals(tExpected[i][2], tRow.material(), "row " + i + " material identity");
            assertEquals(tExpected[i][3], tRow.overworld(), "row " + i + " dimension mask");
        }
        // the overworld roll mass: sum of 1/P over the 33 GEN_FLOOR rows — the ~0.5%/chunk
        // face behind the RCON "~28 脉/4096 chunk" order of magnitude (20.5 expected)
        double tSum = 0;
        for (GTBedrockOreConfig tRow : tTable) if (tRow.overworld()) tSum += 1.0 / tRow.probability();
        assertTrue(tSum > 0.004 && tSum < 0.006, "the OW roll mass stays the upstream order: " + tSum);
    }

    /** The codec roundtrips through JSON (the datapack face the KJS surface edits). */
    @Test
    void tableCodecRoundtripsThroughJson() {
        GTBedrockOreConfig.Table tTable = new GTBedrockOreConfig.Table(GT6WorldgenDatagen.BEDROCK_ORE_TABLE);
        var tJson = GTBedrockOreConfig.Table.CODEC.encodeStart(JsonOps.INSTANCE, tTable).result().orElseThrow();
        GTBedrockOreConfig.Table tBack = GTBedrockOreConfig.Table.CODEC.parse(JsonOps.INSTANCE, tJson).result().orElseThrow();
        assertEquals(tTable, tBack, "the codec roundtrip must preserve the whole 46-row table");
        // an unknown material name decodes to MT.NULL (the upstream invalid-slot semantics)
        var tRowJson = GTBedrockOreConfig.CODEC.encodeStart(JsonOps.INSTANCE,
                new GTBedrockOreConfig("x", MT.Au, 1, true)).result().orElseThrow();
        GTBedrockOreConfig tBack2 = GTBedrockOreConfig.CODEC.parse(JsonOps.INSTANCE, tRowJson).result().orElseThrow();
        assertEquals(MT.Au, tBack2.material());
    }

    // ---------------------------------------------------------------- the rolls

    /** The rolls are deterministic per chunk AND independent per row (P=1 always hits, table order). */
    @Test
    void rollsAreDeterministicAndIndependent() {
        GTBedrockOreConfig.Table tTable = new GTBedrockOreConfig.Table(List.of(
                row("a", 1, MT.Coal),           // nextInt(1)==0 always — the independence face
                row("b", 1, MT.Graphite),
                new GTBedrockOreConfig("off", MT.Desh, 1, false))); // offworld rows never roll
        List<GTBedrockOreConfig> tHits = GT6BedrockOreGenerator.drawRows(tTable, new Random(SEED));
        assertEquals(2, tHits.size(), "the two P=1 overworld rows both hit, table order");
        assertEquals("a", tHits.get(0).name());
        assertEquals("b", tHits.get(1).name());
        // the same seed replays identically (the drawRows stream is coordinate-seeded in production)
        assertEquals(2, GT6BedrockOreGenerator.drawRows(tTable, GT6VeinGenerator.veinRandom(SEED, 0, 7, 7)).size(),
                "the same seed must hit the same rows");
        // invalid rows (NULL material) never roll
        List<GTBedrockOreConfig> tNull = GT6BedrockOreGenerator.drawRows(new GTBedrockOreConfig.Table(List.of(
                new GTBedrockOreConfig("null", MT.NULL, 1, true))), new Random(0));
        assertTrue(tNull.isEmpty(), "an invalid-material row must be skipped");
    }

    // ---------------------------------------------------------------- the shape

    /** The full vein against an all-bedrock all-stone sink: patch/forced at the floor, muffin trapezoid, tails up — all in-chunk. */
    @Test
    void veinShapeIsTheUpstreamForm() {
        GTBedrockOreConfig tRow = row("shape", 1, MT.Coal);
        RecordingSink tSink = new RecordingSink();
        Random tRandom = new Random(SEED);
        assertTrue(GT6BedrockOreGenerator.generateVein(tRow, tRandom, 0, 0, -64, tSink),
                "an all-bedrock chunk passes the :185 gate");

        // the bedrock-face patch + the forced block: exactly at BEDROCK_Y, bounds 5..10 / 6..9
        assertFalse(tSink.bedrockOres.isEmpty(), "the patch must place");
        for (Pos tPos : tSink.bedrockOres) {
            assertEquals(GT6BedrockOreGenerator.BEDROCK_Y, tPos.y, "the patch rides the flat bedrock floor");
            assertTrue(tPos.x >= 5 && tPos.x <= 10 && tPos.z >= 5 && tPos.z <= 10,
                    "the patch/forced block stay in the 6x6 center (upstream :187-194)");
        }
        assertTrue(tSink.bedrockOres.stream().anyMatch(tPos -> !tPos.small),
                "the forced center block is a LARGE bedrock ore (:194)");

        // the muffin: the verbatim trapezoid, y 1..6 above the floor; every shell is deepslate-flagged
        assertTrue(tSink.shells.size() > 50, "the muffin shell covers the trapezoid");
        for (Pos tPos : tSink.shells) {
            int tLayer = tPos.y - GT6BedrockOreGenerator.BEDROCK_Y;
            assertTrue(tLayer >= 1 && tLayer <= GT6BedrockOreGenerator.MUFFIN_LAYERS, "muffin layers 1..6");
            assertTrue(tPos.x >= GT6BedrockOreGenerator.MUFFIN_D1[tLayer] && tPos.x < GT6BedrockOreGenerator.MUFFIN_D2[tLayer]);
            assertTrue(tPos.z >= GT6BedrockOreGenerator.MUFFIN_D1[tLayer] && tPos.z < GT6BedrockOreGenerator.MUFFIN_D2[tLayer]);
        }
        // the muffin ore rolls replace shell positions only (1/6 large + 2/6 small of the shell count)
        assertTrue(tSink.ores.stream().anyMatch(tPos -> tPos.y == GT6BedrockOreGenerator.BEDROCK_Y + 1
                || tPos.y > GT6BedrockOreGenerator.BEDROCK_Y), "muffin/tail ores exist above the floor");
        assertTrue(tSink.ores.stream().anyMatch(tPos -> !tPos.small), "1/6 of the muffin rolls are large ores");

        // the tails: small ores climbing toward sea level, inside the 0..15 chunk bounds
        for (Pos tPos : tSink.ores) {
            assertTrue(tPos.x >= 0 && tPos.x <= 15 && tPos.z >= 0 && tPos.z <= 15,
                    "every write stays inside the work chunk (the single-chunk face)");
            assertTrue(tPos.y > GT6BedrockOreGenerator.BEDROCK_Y, "the tail/muffin band starts above the floor");
            assertTrue(tPos.y <= GT6BedrockOreGenerator.TAIL_TOP_Y, "no tail crosses sea level");
        }
        assertTrue(tSink.ores.stream().allMatch(tPos -> tPos.small || tPos.y <= GT6BedrockOreGenerator.BEDROCK_Y
                + GT6BedrockOreGenerator.MUFFIN_LAYERS), "tail writes are small; large only in the patch/muffin band");
        assertTrue(tSink.ores.stream().anyMatch(tPos -> tPos.y > GT6BedrockOreGenerator.BEDROCK_Y
                + GT6BedrockOreGenerator.MUFFIN_LAYERS), "the tails climb above the muffin");
    }

    /** The bedrock-face gate: no bedrock at the chunk center = no vein, no writes (:185). */
    @Test
    void veinRefusesWithoutBedrockFace() {
        RecordingSink tSink = new RecordingSink();
        tSink.bedrockFace = false;
        assertFalse(GT6BedrockOreGenerator.generateVein(row("gate", 1, MT.Coal), new Random(SEED), 0, 0, -64, tSink));
        assertTrue(tSink.bedrockOres.isEmpty() && tSink.shells.isEmpty() && tSink.ores.isEmpty(),
                "the gate must run before ANY draw-position write");
    }

    /** The same (seed, chunk) replays the identical vein — the decision-level determinism face. */
    @Test
    void veinReplayIsDeterministic() {
        RecordingSink tA = new RecordingSink(), tB = new RecordingSink();
        GT6BedrockOreGenerator.generateVein(row("replay", 3, MT.Coal), GT6VeinGenerator.veinRandom(SEED, 0, 9, -4), 0, 0, -64, tA);
        GT6BedrockOreGenerator.generateVein(row("replay", 3, MT.Coal), GT6VeinGenerator.veinRandom(SEED, 0, 9, -4), 0, 0, -64, tB);
        assertEquals(tA.bedrockOres, tB.bedrockOres);
        assertEquals(tA.shells, tB.shells);
        assertEquals(tA.ores, tB.ores);
    }

    // ---------------------------------------------------------------- the fixed-seed projection

    /**
     * THE fixed-seed projection: over the 64x64 RCON region the decisions are — 27 chunk
     * hits, coal and graphite each exactly 1 (the acceptance gate, pinned offline), 14
     * distinct materials, and a full replay equality (the same-seed-regenerate live probe's
     * offline twin). The RCON scan asserts the LIVE counts against these DECISIONS within
     * the pipeline drift (decisions.2026-09-18-p31-strata-lens-determinism-acceptance).
     */
    @Test
    void fixedSeedProjectionPinsTheRconGate() {
        GTBedrockOreConfig.Table tTable = new GTBedrockOreConfig.Table(GT6WorldgenDatagen.BEDROCK_ORE_TABLE);
        List<String> tFirst = new ArrayList<>();
        int tChunks = 0, tCoal = 0, tGraphite = 0;
        for (int tX = REGION_MIN_X; tX <= REGION_MAX_X; tX++) {
            for (int tZ = REGION_MIN_Z; tZ <= REGION_MAX_Z; tZ++) {
                List<String> tHits = hitNames(tTable, tX, tZ);
                tFirst.add(tX + "," + tZ + "=" + tHits);
                if (!tHits.isEmpty()) tChunks++;
                if (tHits.contains(MT.Coal.mNameInternal)) tCoal++;
                if (tHits.contains(MT.Graphite.mNameInternal)) tGraphite++;
            }
        }
        assertEquals(27, tChunks, "the calibrated window's decision count (the ~28 脉/4096 acceptance face)");
        assertEquals(1, tCoal, "the acceptance gate: coal bedrock ore decisions (the window fixture)");
        assertEquals(1, tGraphite, "the acceptance gate: graphite bedrock ore decisions (the window fixture)");

        // the replay equality (the whole region re-derives identically)
        List<String> tSecond = new ArrayList<>();
        for (int tX = REGION_MIN_X; tX <= REGION_MAX_X; tX++) {
            for (int tZ = REGION_MIN_Z; tZ <= REGION_MAX_Z; tZ++) {
                tSecond.add(tX + "," + tZ + "=" + hitNames(tTable, tX, tZ));
            }
        }
        assertEquals(tFirst, tSecond, "the region projection must replay identically");
    }

    /** The hit names for one chunk (the material names, in table order). */
    private static List<String> hitNames(GTBedrockOreConfig.Table aTable, int aX, int aZ) {
        List<String> rNames = new ArrayList<>(1);
        for (GTBedrockOreConfig tRow : GT6BedrockOreGenerator.drawRows(aTable, GT6VeinGenerator.veinRandom(SEED, 0, aX, aZ))) {
            assertNotNull(tRow.material());
            rNames.add(tRow.material().mNameInternal);
        }
        return rNames;
    }

    // ---------------------------------------------------------------- the harness

    private record Pos(int x, int y, int z, boolean small) {}

    /** The offline sink: records every placement (the pure-math calls, no level face). */
    private static final class RecordingSink implements BedrockSink {
        boolean bedrockFace = true;
        final List<Pos> bedrockOres = new ArrayList<>();
        final List<Pos> shells = new ArrayList<>();
        final List<Pos> ores = new ArrayList<>();

        @Override public boolean isBedrockFace(int aX, int aZ) { return bedrockFace; }
        @Override public void bedrockOre(int aX, int aY, int aZ, OreDictMaterial aM, boolean aSmall) {
            bedrockOres.add(new Pos(aX, aY, aZ, aSmall));
        }
        @Override public void shell(int aX, int aY, int aZ) { shells.add(new Pos(aX, aY, aZ, false)); }
        @Override public void ore(int aX, int aY, int aZ, OreDictMaterial aM, boolean aSmall) {
            ores.add(new Pos(aX, aY, aZ, aSmall));
        }
    }
}
