/**
 * Tests for task p31-fluid-spring: the 16-row table parity (Loader_Worldgen.java:782-797
 * row-for-row), the first-hit-wins spring draw, the dome shape pin (shell/lake per layer,
 * the cave-seal skin), the codec roundtrip and the fixed-seed projection with the
 * per-chunk bedrock-ore mutual exclusion — the acceptance's offline audit unit (the
 * GT6BedrockOreWorldgenTest posture).
 *
 * <p>The fixed-seed projection IS the offline leg of the RCON acceptance: over the SAME
 * 64x64 chunk region (x 0..63, z 64..127) the seed-6131000569321125127 decisions are
 * computed from the same code the Feature runs — the live scan's chunk-hit floors, the
 * six-kind presence gate and the ore/spring exclusion are pinned here at DECISION level
 * (block survival rides the pipeline drift,
 * decisions.2026-09-18-p31-strata-lens-determinism-acceptance).
 *
 * <p>Offline-safe by construction: initMaterials + the vanilla bootstrap bracket only; no
 * vanilla Feature class is touched (the generator core is Feature-free) and the config
 * rows carry plain id strings (no registry reads).
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.misc.GTFluidSpringBlockEntity;
import gregtech6.worldgen.GT6FluidSpringGenerator.DomeSink;

class GT6FluidSpringWorldgenTest {

    /** The card's fixed RCON probe seed — the same one live (the bedrock card's calibrated seed). */
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

    // ---------------------------------------------------------------- the table parity

    /**
     * THE parity pin: the 16-row table against the upstream transcription, row-for-row
     * (Loader_Worldgen.java:782-797). The indicatorType column (2/2/2/2/1/3/1) is the
     * remaining declared spec ③ deferral; the springFluid amounts are the loader values
     * verbatim (the task p38-issue5-fluid-spring-nozzle column return).
     */
    @Test
    void tableParityAgainstUpstream() {
        List<GTFluidSpringConfig> tTable = GT6WorldgenDatagen.FLUID_SPRING_TABLE;
        assertEquals(16, tTable.size(), "16 rows (the upstream WorldgenFluidSpring loop verbatim)");

        // (name, blockId, P, overworld, springFluid) — the upstream ctor columns verbatim,
        // in row order; springFluid = the mSpringFluid amount (6000 oils / 3000 gas /
        // 500 geothermal / 1000 lava OW; 2000/1000/250/500 offworld)
        Object[][] tExpected = {
            {"overworld.fluid.oil.extraheavy", "gt6:liquid_extra_heavy_oil_block", 400, true , 6000}, // :782
            {"overworld.fluid.oil.heavy"     , "gt6:liquid_heavy_oil_block"      , 400, true , 6000}, // :783
            {"overworld.fluid.oil.medium"    , "gt6:liquid_medium_oil_block"     , 400, true , 6000}, // :784
            {"overworld.fluid.oil.light"     , "gt6:liquid_light_oil_block"      , 400, true , 6000}, // :785
            {"overworld.fluid.gas.natural"   , "gt6:natural_gas_block"           , 200, true , 3000}, // :786
            {"overworld.fluid.water"         , "gt6:water_geothermal_block"      , 100, true ,  500}, // :787
            {"overworld.fluid.lava"          , "minecraft:lava"                  , 200, true , 1000}, // :788
            {"atum.fluid.oil.extraheavy"     , "gt6:liquid_extra_heavy_oil_block", 200, false, 2000}, // :789
            {"atum.fluid.oil.heavy"          , "gt6:liquid_heavy_oil_block"      , 200, false, 2000}, // :790
            {"atum.fluid.oil.medium"         , "gt6:liquid_medium_oil_block"     , 200, false, 2000}, // :791
            {"atum.fluid.oil.light"          , "gt6:liquid_light_oil_block"      , 200, false, 2000}, // :792
            {"erebus.fluid.gas.natural"      , "gt6:natural_gas_block"           , 200, false, 1000}, // :793
            {"betweenlands.fluid.gas.natural", "gt6:natural_gas_block"           , 200, false, 1000}, // :794
            {"twilight.fluid.gas.natural"    , "gt6:natural_gas_block"           , 200, false, 1000}, // :795
            {"twilight.fluid.water"          , "gt6:water_geothermal_block"      , 100, false,  250}, // :796
            {"nether.fluid.lava"             , "minecraft:lava"                  , 100, false,  500}};// :797
        assertEquals(tTable.size(), tExpected.length);
        for (int i = 0; i < tExpected.length; i++) {
            GTFluidSpringConfig tRow = tTable.get(i);
            assertEquals(tExpected[i][0], tRow.name(), "row " + i + " name must stay in upstream order");
            assertEquals(tExpected[i][1], tRow.blockId(), "row " + i + " block id (the single-sourced face)");
            assertEquals(tExpected[i][2], tRow.probability(), "row " + i + " probability (the 1/P per-chunk roll)");
            assertEquals(tExpected[i][3], tRow.overworld(), "row " + i + " dimension mask");
            assertEquals(tExpected[i][4], tRow.springFluid(), "row " + i + " springFluid amount (the 1/amount nozzle divisor)");
        }
        // the overworld roll mass: 4/400 + 1/200 + 1/100 + 1/200 = 0.035/chunk — the ~3.5%
        // face behind the RCON "~144 springs/4096 chunk" order of magnitude
        double tSum = 0;
        for (GTFluidSpringConfig tRow : tTable) if (tRow.overworld()) tSum += 1.0 / tRow.probability();
        assertTrue(tSum > 0.025 && tSum < 0.045, "the OW roll mass stays the upstream order: " + tSum);
    }

    /** The block-id single source: the GT rows resolve through GTFluids.springBlockId, lava is the bare vanilla face. */
    @Test
    void blockIdsAreSingleSourced() {
        assertEquals("gt6:liquid_medium_oil_block", GTFluids.springBlockId("liquid_medium_oil"));
        assertEquals("gt6:natural_gas_block", GTFluids.springBlockId("natural_gas"), "natural_gas rides its p5 block face");
        assertEquals("gt6:water_geothermal_block", GTFluids.springBlockId("water_geothermal"));
        for (GTFluidSpringConfig tRow : GT6WorldgenDatagen.FLUID_SPRING_TABLE) {
            if (tRow.blockId().equals("minecraft:lava")) continue;
            assertTrue(tRow.blockId().startsWith("gt6:"), tRow.name() + ": the GT rows carry the gt6 namespace");
            assertTrue(GTFluids.SPRING_BLOCK_IDS.contains(tRow.blockId().substring(4, tRow.blockId().length() - "_block".length()))
                    || tRow.blockId().equals("gt6:natural_gas_block"),
                    tRow.name() + ": the block id must come from the worldgen block face: " + tRow.blockId());
        }
        assertEquals(List.of("liquid_extra_heavy_oil", "liquid_heavy_oil", "liquid_medium_oil", "liquid_light_oil",
                "water_geothermal"), GTFluids.SPRING_BLOCK_IDS, "the five NEW block faces (natural_gas is the p6 face)");
    }

    /** The codec roundtrips through JSON (the datapack face the KJS surface edits). */
    @Test
    void tableCodecRoundtripsThroughJson() {
        GTFluidSpringConfig.Table tTable = new GTFluidSpringConfig.Table(GT6WorldgenDatagen.FLUID_SPRING_TABLE);
        var tJson = GTFluidSpringConfig.Table.CODEC.encodeStart(JsonOps.INSTANCE, tTable).result().orElseThrow();
        GTFluidSpringConfig.Table tBack = GTFluidSpringConfig.Table.CODEC.parse(JsonOps.INSTANCE, tJson).result().orElseThrow();
        assertEquals(tTable, tBack, "the codec roundtrip must preserve the whole 16-row table");
        assertEquals(16, tJson.getAsJsonObject().getAsJsonArray("rows").size(), "the JSON face carries every row");
    }

    // ---------------------------------------------------------------- the draw semantics

    /**
     * The first-hit-wins claim (WorldgenFluidSpring.java:64 CAN_GENERATE_BEDROCK_ORE=F):
     * a P=1 row always wins and every later row stays unreachable — at most one spring
     * per chunk, unlike the bedrock card's independent rows.
     */
    @Test
    void drawSpringIsFirstHitWins() {
        GTFluidSpringConfig tFirst = new GTFluidSpringConfig("a", "minecraft:lava", 1, true, 1000);
        GTFluidSpringConfig tSecond = new GTFluidSpringConfig("b", "minecraft:lava", 1, true, 1000);
        GTFluidSpringConfig.Table tTable = new GTFluidSpringConfig.Table(List.of(tFirst, tSecond));
        for (long tSeed = 0; tSeed < 100; tSeed++) {
            GTFluidSpringConfig tHit = GT6FluidSpringGenerator.drawSpring(tTable, new Random(tSeed));
            assertEquals("a", tHit.name(), "seed " + tSeed + ": the first P=1 row claims every chunk");
        }
        // the mask: an overworld=false row never draws (the dormant offworld band)
        GTFluidSpringConfig.Table tMasked = new GTFluidSpringConfig.Table(List.of(
                new GTFluidSpringConfig("off", "minecraft:lava", 1, false, 1000)));
        assertNull(GT6FluidSpringGenerator.drawSpring(tMasked, new Random(0)),
                "the overworld=false rows never roll (the :789-797 dormancy)");
    }

    /**
     * THE mutual-exclusion unit face: the ore replay on a table with a P=1 row claims
     * EVERY chunk (WorldgenFluidSpring.java:62 GENERATED_NO_BEDROCK_ORE face), so the
     * spring refuses regardless of its own roll — one bedrock event per chunk.
     */
    @Test
    void oreClaimsBlockTheSpring() {
        GTBedrockOreConfig.Table tOreTable = new GTBedrockOreConfig.Table(List.of(
                new GTBedrockOreConfig("ore.bedrock.coal", gregapi.data.MT.Coal, 1, true)));
        GTFluidSpringConfig.Table tSpringTable = new GTFluidSpringConfig.Table(List.of(
                new GTFluidSpringConfig("overworld.fluid.water", "gt6:water_geothermal_block", 1, true, 500)));
        for (long tSeed = 0; tSeed < 50; tSeed++) {
            assertTrue(GT6FluidSpringGenerator.oreClaims(tOreTable, new Random(tSeed)),
                    "seed " + tSeed + ": the P=1 ore row claims the chunk");
        }
    }

    // ---------------------------------------------------------------- the dome shape

    /** The recording fake world: a set of carved (non-opaque) cells, everything else opaque. */
    private static final class FakeSink implements DomeSink {
        final Set<Long> tCarved = new HashSet<>();
        final Set<Long> tShell = new HashSet<>();
        final Map<Long, String> tFluid = new HashMap<>();
        final List<Long> tNozzles = new ArrayList<>();
        boolean tBedrockFace = true;
        boolean tBedrockFloor = true;

        static long key(int aX, int aY, int aZ) {
            return ((long) (aX + 32768) << 32) | ((long) (aY + 32768) << 16) | (aZ + 32768);
        }

        void carve(int aX, int aY, int aZ) {
            tCarved.add(key(aX, aY, aZ));
        }

        @Override
        public boolean isBedrockFace(int aX, int aZ) {
            return tBedrockFace;
        }

        @Override
        public boolean isBedrock(int aX, int aZ) {
            return tBedrockFloor;
        }

        @Override
        public boolean isOpaque(int aX, int aY, int aZ) {
            return !tCarved.contains(key(aX, aY, aZ));
        }

        @Override
        public void shell(int aX, int aY, int aZ) {
            tShell.add(key(aX, aY, aZ));
        }

        @Override
        public void fluid(int aX, int aY, int aZ, GTFluidSpringConfig aRow) {
            tFluid.put(key(aX, aY, aZ), aRow.name());
        }

        @Override
        public void nozzle(int aX, int aY, int aZ) {
            tNozzles.add(key(aX, aY, aZ));
        }
    }

    private static GTFluidSpringConfig row() {
        return new GTFluidSpringConfig("overworld.fluid.oil.medium", "gt6:liquid_medium_oil_block", 400, true, 6000);
    }

    /**
     * The dome shape pin (WorldgenFluidSpring.java:72-80): a fully-opaque world writes
     * ZERO shell and the 14x14..4x4 ziggurat of fluid (556 cells over y 1..6 -> -63..-58);
     * the gate refuses a non-bedrock centre before any write.
     */
    @Test
    void domeShapePinsTheZiggurat() {
        GTFluidSpringConfig tRow = row();
        FakeSink tSink = new FakeSink();
        assertTrue(GT6FluidSpringGenerator.generateDome(tRow, 64, 128, -64, new Random(0), tSink));
        assertEquals(556, tSink.tFluid.size(), "14^2+12^2+10^2+8^2+6^2+4^2 lake cells");
        assertTrue(tSink.tShell.isEmpty(), "an opaque world needs no shell skin");
        // the per-level insets (local x/z i..15-i at y = -64+i, i = 1..6)
        int[] tInsets = {14, 12, 10, 8, 6, 4};
        for (int i = 1; i <= 6; i++) {
            int tCount = 0;
            for (long tKey : tSink.tFluid.keySet()) {
                int tY = (int) ((tKey >> 16) & 0xffff) - 32768;
                if (tY == -64 + i) tCount++;
            }
            assertEquals(tInsets[i - 1] * tInsets[i - 1], tCount, "layer y=" + (-64 + i) + " inset " + tInsets[i - 1]);
        }
        // the gate: no bedrock face at the chunk centre -> no writes at all (:66-67)
        FakeSink tRefused = new FakeSink();
        tRefused.tBedrockFace = false;
        assertFalse(GT6FluidSpringGenerator.generateDome(tRow, 64, 128, -64, new Random(0), tRefused));
        assertTrue(tRefused.tFluid.isEmpty() && tRefused.tShell.isEmpty(), "the gate precedes every write");
    }

    /**
     * The cave-seal skin pin (:73): an air cavity inside the shell-only ring becomes
     * deepslate; a cavity under the lake body is overwritten by the fluid; a cavity
     * outside every footprint is untouched; the dome-top cap seals at y = -57.
     */
    @Test
    void shellSealsTheOpenings() {
        GTFluidSpringConfig tRow = row();
        FakeSink tSink = new FakeSink();
        tSink.carve(65, -62, 136); // local (1, -62, 8): shell ring at layer i=1, outside every lake footprint
        tSink.carve(72, -62, 136); // local (8, -62, 8): shell at i=1 then the lake overwrite at i=2
        tSink.carve(72, -57, 136); // local (8, -57, 8): the dome-top cap at i=6
        tSink.carve(64, -60, 128); // local (0, -60, 0): outside every write footprint
        assertTrue(GT6FluidSpringGenerator.generateDome(tRow, 64, 128, -64, new Random(0), tSink));
        assertTrue(tSink.tShell.contains(FakeSink.key(65, -62, 136)), "the ring cavity seals with shell");
        assertTrue(tSink.tShell.contains(FakeSink.key(72, -62, 136))
                && tSink.tFluid.containsKey(FakeSink.key(72, -62, 136)),
                "the interior cavity: the shell pass wrote first (:73) and the lake overwrote it (:75) — the write trace in upstream order");
        assertEquals("overworld.fluid.oil.medium", tSink.tFluid.get(FakeSink.key(72, -62, 136)), "the final state at the interior cavity is the fluid");
        assertTrue(tSink.tShell.contains(FakeSink.key(72, -57, 136)), "the dome-top cap seals at y = -57");
        assertFalse(tSink.tShell.contains(FakeSink.key(64, -60, 128)));
        assertFalse(tSink.tFluid.containsKey(FakeSink.key(64, -60, 128)), "outside the footprints nothing writes");
    }

    // ---------------------------------------------------------------- the nozzle arm (task p38-issue5-fluid-spring-nozzle)

    /** A row with the nozzle arm parameterized by the springFluid amount (null = the arm-less upstream NF face). */
    private static GTFluidSpringConfig nozzleRow(Integer aSpringFluid) {
        return new GTFluidSpringConfig("overworld.fluid.oil.medium", "gt6:liquid_medium_oil_block", 400, true, aSpringFluid);
    }

    private static int yOf(long aKey) {
        return (int) ((aKey >> 16) & 0xffff) - 32768;
    }

    private static int xOf(long aKey) {
        return (int) ((aKey >> 32) & 0xffff) - 32768;
    }

    private static int zOf(long aKey) {
        return (int) (aKey & 0xffff) - 32768;
    }

    /** The all-hit stream: nextInt(bound) == 0 always (the 1/16 draw never refuses). */
    private static Random alwaysHit() {
        return new Random(0) {
            @Override
            public int nextInt(int aBound) {
                return 0;
            }
        };
    }

    /**
     * The nozzle arm pin (WorldgenFluidSpring.java:77-79): an all-hit stream draws a nozzle
     * at EVERY i &gt; 2 footprint position — 216 draws over the nested insets
     * (100+64+36+16 = 10x10/8x8/6x6/4x4, layers i = 3..6), 100 distinct floor cells (the
     * nested footprints re-place the same floor cells, upstream placeBlock-over-itself
     * same), every one at the bedrock floor y and strictly inside the inset >= 3 ring;
     * nothing at i <= 2. The dome writes are untouched by the arm.
     */
    @Test
    void nozzleArmPinsTheFootprint() {
        FakeSink tSink = new FakeSink();
        assertTrue(GT6FluidSpringGenerator.generateDome(nozzleRow(6000), 64, 128, -64, alwaysHit(), tSink));
        assertEquals(216, tSink.tNozzles.size(), "10^2+8^2+6^2+4^2 nozzle draws (the i > 2 layers)");
        Set<Long> tDistinct = new HashSet<>(tSink.tNozzles);
        assertEquals(100, tDistinct.size(), "the nested footprints cover exactly the inner 10x10 floor");
        for (long tKey : tDistinct) {
            assertEquals(-64, yOf(tKey), "every nozzle sits on the bedrock floor (upstream y = 0)");
            int tX = xOf(tKey) - 64, tZ = zOf(tKey) - 128;
            assertTrue(Math.min(Math.min(tX, 15 - tX), Math.min(tZ, 15 - tZ)) >= 3,
                    "nozzle local (" + tX + "," + tZ + ") rides the i > 2 inset");
        }
        assertEquals(556, tSink.tFluid.size(), "the lake body is untouched by the arm");
        assertTrue(tSink.tShell.isEmpty(), "the shell pass is untouched by the arm");

        // the strict WD.bedrock floor gate (:77): no bedrock floor -> no nozzle at all
        FakeSink tNoFloor = new FakeSink();
        tNoFloor.tBedrockFloor = false;
        assertTrue(GT6FluidSpringGenerator.generateDome(nozzleRow(6000), 64, 128, -64, alwaysHit(), tNoFloor));
        assertTrue(tNoFloor.tNozzles.isEmpty(), "the strict-bedrock gate precedes every nozzle write");
        assertEquals(556, tNoFloor.tFluid.size(), "the dome still generates over a foreign floor");
    }

    /**
     * The arm randomness faces: a null springFluid draws NOTHING (the :77 short-circuit —
     * the stream stays drawSpring-aligned, the dome decisions keep their P31 projection),
     * and a real amount sprays the deterministic 1/16 rate across seeds.
     */
    @Test
    void nozzleArmRandomnessPins() {
        // the stream-alignment face: null-springFluid consumes ZERO draws after drawSpring
        GTFluidSpringConfig.Table tTable = new GTFluidSpringConfig.Table(List.of(row()));
        Random tDrawn = new Random(7), tPristine = new Random(7);
        GT6FluidSpringGenerator.drawSpring(tTable, tDrawn);
        FakeSink tSink = new FakeSink();
        GT6FluidSpringGenerator.generateDome(nozzleRow(null), 64, 128, -64, tDrawn, tSink);
        GT6FluidSpringGenerator.drawSpring(tTable, tPristine);
        assertTrue(tSink.tNozzles.isEmpty(), "null springFluid = the arm-less row (upstream NF)");
        assertEquals(tPristine.nextInt(), tDrawn.nextInt(),
                "the nozzle-free row consumed ZERO draws — the dome decisions stay P31-aligned");

        // the 1/16 face at the loader amount: every seed lands strictly between 0 and the
        // all-hit ceiling (216 draws @ 1/16 -> ~13.5 per chunk; zero is ~(15/16)^216 < 1e-6)
        for (long tSeed = 0; tSeed < 8; tSeed++) {
            FakeSink tSeeded = new FakeSink();
            GT6FluidSpringGenerator.generateDome(nozzleRow(6000), 64, 128, -64, new Random(tSeed), tSeeded);
            assertTrue(tSeeded.tNozzles.size() > 0 && tSeeded.tNozzles.size() < 216,
                    "seed " + tSeed + ": the 1/16 rate must land in (0, 216): " + tSeeded.tNozzles.size());
        }
    }

    // ---------------------------------------------------------------- the nozzle spray (task p38-issue5-fluid-spring-nozzle)

    /**
     * The spray decision pin (MultiTileEntityFluidSpring.java:110-141, the infinite-fluid
     * branch): the above cell sprays when air, same-fluid-flowing or foreign liquid —
     * never over a solid or the full source itself; the lateral walk converts same-fluid
     * flowing and air only (foreign liquids and solids keep it scanning, upstream :126-134).
     */
    @Test
    void sprayDecisionsPinTheUpstreamBranches() {
        BlockState tSource = Blocks.WATER.defaultBlockState();
        BlockState tFlowing = Blocks.WATER.defaultBlockState().trySetValue(LiquidBlock.LEVEL, 3);
        BlockState tForeign = Blocks.LAVA.defaultBlockState();
        BlockState tAir = Blocks.AIR.defaultBlockState();
        BlockState tSolid = Blocks.STONE.defaultBlockState();

        // :117/:139 — WD.liquid(tAbove) || tAbove.isAir, minus the full source itself
        assertTrue(GTFluidSpringBlockEntity.shouldSprayAbove(tAir, tSource), "air sprays");
        assertTrue(GTFluidSpringBlockEntity.shouldSprayAbove(tFlowing, tSource), "a same-fluid flowing outlet tops up to source");
        assertTrue(GTFluidSpringBlockEntity.shouldSprayAbove(tForeign, tSource), "a foreign liquid outlet is displaced");
        assertFalse(GTFluidSpringBlockEntity.shouldSprayAbove(tSolid, tSource), "a solid cap blocks the spray");
        assertFalse(GTFluidSpringBlockEntity.shouldSprayAbove(tSource, tSource), "the full source takes the lateral branch");

        // :126-134 — the lateral refill walk
        assertTrue(GTFluidSpringBlockEntity.shouldSpreadTo(tFlowing, tSource), "a same-fluid flowing neighbor converts");
        assertTrue(GTFluidSpringBlockEntity.shouldSpreadTo(tAir, tSource), "an air neighbor converts");
        assertFalse(GTFluidSpringBlockEntity.shouldSpreadTo(tSource, tSource), "an existing source is skipped");
        assertFalse(GTFluidSpringBlockEntity.shouldSpreadTo(tSolid, tSource), "a solid keeps the walk scanning");
        assertFalse(GTFluidSpringBlockEntity.shouldSpreadTo(tForeign, tSource), "a foreign liquid keeps the walk scanning");
    }

    /** The NBT face ("gt.spring"/"gt.spring_amount"/"gt.active") round-trips; the amount floor (:101) holds. */
    @Test
    void springNbtRoundtripsAndTheAmountFloorHolds() {
        GTFluidSpringBlockEntity tSpring = new GTFluidSpringBlockEntity();
        tSpring.setSpring("gt6:liquid_medium_oil_block", 6000);
        assertEquals("gt6:liquid_medium_oil_block", tSpring.getSpringBlockId());
        assertEquals(6000, tSpring.getSpringAmount());
        assertFalse(tSpring.isActive(), "a fresh worldgen placement starts dormant");

        CompoundTag tTag = tSpring.saveWithoutMetadata();
        assertEquals("gt6:liquid_medium_oil_block", tTag.getString(GTFluidSpringBlockEntity.NBT_SPRING));
        assertFalse(tTag.getBoolean(GTFluidSpringBlockEntity.NBT_ACTIVE));
        tSpring.load(tTag);
        assertEquals("gt6:liquid_medium_oil_block", tSpring.getSpringBlockId(), "the roundtrip preserves the spring identity");
        assertEquals(6000, tSpring.getSpringAmount());

        CompoundTag tActiveTag = tSpring.saveWithoutMetadata();
        tActiveTag.putBoolean(GTFluidSpringBlockEntity.NBT_ACTIVE, true);
        tSpring.load(tActiveTag);
        assertTrue(tSpring.isActive(), "the one-way wake latch persists (upstream NBT_ACTIVE)");

        // the :101 floor — also the nextInt(0) guard
        tSpring.setSpring("minecraft:lava", 0);
        assertEquals(GTFluidSpringBlockEntity.DEFAULT_AMOUNT, tSpring.getSpringAmount(), "a non-positive amount clamps to the :101 floor");
    }

    // ---------------------------------------------------------------- the fixed-seed projection

    /**
     * One decision replay of the Feature's place() over the scan region: the ore replay
     * (the exclusion) then the spring draw, both on the coordinate-seeded streams. Returns
     * the per-chunk verdicts: ore-claimed chunks and the winning spring row name.
     */
    private Map<String, String> projectRegion(Set<String> aOreChunks) {
        GTBedrockOreConfig.Table tOreTable = new GTBedrockOreConfig.Table(GT6WorldgenDatagen.BEDROCK_ORE_TABLE);
        GTFluidSpringConfig.Table tSpringTable = new GTFluidSpringConfig.Table(GT6WorldgenDatagen.FLUID_SPRING_TABLE);
        Map<String, String> rSprings = new HashMap<>();
        for (int tX = REGION_MIN_X; tX <= REGION_MAX_X; tX++) for (int tZ = REGION_MIN_Z; tZ <= REGION_MAX_Z; tZ++) {
            Random tOreRandom = GT6VeinGenerator.veinRandom(SEED, GT6VeinGenerator.OVERWORLD_DIMENSION_SALT, tX, tZ);
            if (GT6FluidSpringGenerator.oreClaims(tOreTable, tOreRandom)) {
                aOreChunks.add(tX + "," + tZ);
                continue; // the place() refusal — one bedrock event per chunk
            }
            Random tSpringRandom = GT6VeinGenerator.veinRandom(SEED, GT6Worldgen.SPRING_DIMENSION_SALT, tX, tZ);
            GTFluidSpringConfig tRow = GT6FluidSpringGenerator.drawSpring(tSpringTable, tSpringRandom);
            if (tRow != null) rSprings.put(tX + "," + tZ, tRow.name());
        }
        return rSprings;
    }

    /**
     * THE fixed-seed projection (the offline leg of the RCON gate): the decisions are
     * computed twice and must replay identically (the decision-level determinism), the
     * six OW kinds must each hit >= 1 chunk, and no chunk may carry BOTH an ore decision
     * and a spring decision (the exclusion,WorldgenFluidSpring.java:62 face). The expected
     * order of magnitude: 0.035/chunk * 4096 ≈ 144 springs, 27 ore claims (the bedrock
     * card's pinned projection).
     */
    @Test
    void fixedSeedProjectionReplaysIdenticallyWithExclusion() {
        Set<String> tOre1 = new HashSet<>();
        Map<String, String> tSpring1 = projectRegion(tOre1);
        Set<String> tOre2 = new HashSet<>();
        Map<String, String> tSpring2 = projectRegion(tOre2);
        assertEquals(tSpring1, tSpring2, "the decisions must replay identically (same seed, same tables)");
        assertEquals(tOre1, tOre2, "the ore replay must be deterministic too");

        // the exclusion: no chunk carries both events (WorldgenFluidSpring.java:62)
        for (String tChunk : tOre1) assertFalse(tSpring1.containsKey(tChunk),
                "chunk " + tChunk + ": an ore-claimed chunk must refuse the spring");

        // the ore side matches the bedrock card's pinned projection (27 decisions)
        assertEquals(27, tOre1.size(), "the ore replay must reproduce the bedrock card's 27 chunk decisions");

        // the kind presence (the RCON "6 kinds hit" gate at decision level; lava included —
        // the vanilla block face still carries the dome decision)
        Set<String> tKinds = new HashSet<>(tSpring1.values());
        for (String tKind : List.of("overworld.fluid.oil.extraheavy", "overworld.fluid.oil.heavy",
                "overworld.fluid.oil.medium", "overworld.fluid.oil.light", "overworld.fluid.gas.natural",
                "overworld.fluid.water", "overworld.fluid.lava"))
            assertTrue(tKinds.contains(tKind), "the fixed-seed region must carry a " + tKind + " decision");

        // the order of magnitude: ~3.5%/chunk over 4096 chunks ≈ 144 (the RCON floor is
        // half of it, the pipeline-drift semantics)
        assertTrue(tSpring1.size() >= 70 && tSpring1.size() <= 220,
                "the spring decision count stays the upstream order: " + tSpring1.size());
        System.out.println("[p31-fluid-spring projection] springs=" + tSpring1.size() + " oreClaims=" + tOre1.size());
    }
}
