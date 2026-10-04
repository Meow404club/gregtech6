/**
 * Tests for task worldgen-water-replace: the 3-row table parity (Loader_Worldgen.java
 * :576-578 row-for-row, the OCEAN→RIVER→SWAMP hard order), the chunk-gate matrix (the
 * WorldgenRiver.java:54 compound verbatim), the codec roundtrip with the scanTop default,
 * the block-face id pin (the task's WATER_REPLACE_BLOCK_IDS face), and the generated
 * JSON chain (configured rows + the TOP_LAYER_MODIFICATION biome modifier in both
 * brands) — the card's offline audit unit (the GT6FluidSpringWorldgenTest posture).
 *
 * <p>Offline-safe by construction: the vanilla bootstrap bracket only; the config rows
 * carry plain id strings (no registry reads) and the gate matrix is a pure function.
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.world.level.block.Blocks;

import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.fluid.GTFluids;

class GT6WaterReplaceWorldgenTest extends gregtech6.tileentity.GTOfflineTestBase {

    // ---------------------------------------------------------------- the table parity

    /**
     * THE parity pin: the 3-row table against the upstream transcription, row-for-row
     * (Loader_Worldgen.java:576-578, the upstream WorldgenObject names verbatim). The ROW
     * ORDER IS the OCEAN→RIVER→SWAMP hard constraint (Loader_Worldgen.java:575-578 source
     * order — the swamp arm converts the ocean/river faces the earlier rows placed), so
     * the order-sensitive assert IS the acceptance; scanTop rides the 62 default (the
     * upstream "Height" config default over WD.waterLevel()).
     */
    @Test
    void tableParityAgainstUpstream() {
        List<GT6WaterReplaceConfig> tTable = GT6WorldgenDatagen.WATER_REPLACE_TABLE;
        assertEquals(3, tTable.size(), "3 rows (the upstream ocean/river/swamp triple verbatim)");
        Object[][] tExpected = {
            {"ocean.seawater"  , "gt6:seawater_block"  , GT6WaterReplaceConfig.Gate.OCEAN, 62}, // :576
            {"river.riverwater", "gt6:riverwater_block", GT6WaterReplaceConfig.Gate.RIVER, 62}, // :577
            {"swamp.dirtywater", "gt6:waterdirty_block", GT6WaterReplaceConfig.Gate.SWAMP, 62}};// :578
        assertEquals(tTable.size(), tExpected.length);
        for (int i = 0; i < tExpected.length; i++) {
            GT6WaterReplaceConfig tRow = tTable.get(i);
            assertEquals(tExpected[i][0], tRow.name(), "row " + i + " name must stay in upstream order (the :575-578 hard constraint)");
            assertEquals(tExpected[i][1], tRow.blockId(), "row " + i + " block id (the single-sourced face)");
            assertEquals(tExpected[i][2], tRow.gate(), "row " + i + " gate");
            assertEquals(tExpected[i][3], tRow.scanTop(), "row " + i + " scanTop (the WD.waterLevel default)");
        }
    }

    /** The block ids single-source through {@code GTFluids.springBlockId} and the fluids sit on the task's WATER_REPLACE_BLOCK_IDS face. */
    @Test
    void blockIdsSingleSourceThroughTheBlockFace() {
        assertEquals(List.of("seawater", "riverwater", "waterdirty"), GTFluids.WATER_REPLACE_BLOCK_IDS,
                "the three water-body block faces (the card's GTFluids revision, the fluid-spring precedent shape)");
        // the spring faces stay untouched (the ratchet)
        assertEquals(List.of("liquid_extra_heavy_oil", "liquid_heavy_oil", "liquid_medium_oil",
                "liquid_light_oil", "water_geothermal"), GTFluids.SPRING_BLOCK_IDS, "the five spring block faces unchanged");
        for (GT6WaterReplaceConfig tRow : GT6WorldgenDatagen.WATER_REPLACE_TABLE) {
            String tFluid = tRow.blockId().substring("gt6:".length(), tRow.blockId().length() - "_block".length());
            assertTrue(GTFluids.WATER_REPLACE_BLOCK_IDS.contains(tFluid),
                    "row " + tRow.name() + " fluid id must be a WATER_REPLACE_BLOCK_IDS member: " + tFluid);
            assertEquals(tRow.blockId(), GTFluids.springBlockId(tFluid), "row " + tRow.name() + " single-source face");
        }
    }

    // ---------------------------------------------------------------- the gate matrix

    /**
     * The chunk-gate matrix, all 8 combos — OCEAN runs iff the chunk carries an ocean
     * biome (WorldgenOcean.java:54); RIVER iff a river biome AND NO ocean biome
     * (WorldgenRiver.java:54 verbatim compound — the ocean-adjacent river chunk skips);
     * SWAMP iff a swamp biome (WorldgenSwamp.java:55).
     */
    @Test
    void gateMatrixIsTheUpstreamCompound() {
        for (boolean tOcean : new boolean[] {false, true}) {
            for (boolean tRiver : new boolean[] {false, true}) {
                for (boolean tSwamp : new boolean[] {false, true}) {
                    assertEquals(tOcean, GT6WaterReplaceFeature.rowRuns(
                            GT6WaterReplaceConfig.Gate.OCEAN, tOcean, tRiver, tSwamp),
                            "OCEAN gate (" + tOcean + "," + tRiver + "," + tSwamp + ")");
                    assertEquals(tRiver && !tOcean, GT6WaterReplaceFeature.rowRuns(
                            GT6WaterReplaceConfig.Gate.RIVER, tOcean, tRiver, tSwamp),
                            "RIVER gate (" + tOcean + "," + tRiver + "," + tSwamp + ") — the :54 compound");
                    assertEquals(tSwamp, GT6WaterReplaceFeature.rowRuns(
                            GT6WaterReplaceConfig.Gate.SWAMP, tOcean, tRiver, tSwamp),
                            "SWAMP gate (" + tOcean + "," + tRiver + "," + tSwamp + ")");
                }
            }
        }
    }

    /** The :66 waterlike arm matches the port's three water bodies and stays null-safe offline (the unbound-registry miss face). */
    @Test
    void waterlikeArmCoversThePortWaterBodies() {
        // the offline face: unresolvable ids resolve to null, the arm never matches
        assertFalse(GT6WaterReplaceFeature.isWaterlike(Blocks.WATER.defaultBlockState(), java.util.Arrays.asList(null, null)),
                "null members (unbound registry) never match — the offline miss face");
        // the hit face over a synthetic member set (the state identity, the :66 instanceof face)
        assertTrue(GT6WaterReplaceFeature.isWaterlike(Blocks.WATER.defaultBlockState(),
                        java.util.Arrays.asList(Blocks.WATER.defaultBlockState(), null)),
                "a live member matches by block identity");
        assertFalse(GT6WaterReplaceFeature.isWaterlike(Blocks.STONE.defaultBlockState(),
                        List.of(Blocks.WATER.defaultBlockState())),
                "non-waterlike states never match");
    }

    // ---------------------------------------------------------------- the codec

    /** The JSON face: lowercase gate spelling, scanTop optional defaulting 62, unknown gate trips loud. */
    @Test
    void codecRoundtripWithScanTopDefault() {
        String tJson = "{\"rows\":["
                + "{\"name\":\"ocean.seawater\",\"block\":\"gt6:seawater_block\",\"gate\":\"ocean\"},"
                + "{\"name\":\"river.riverwater\",\"block\":\"gt6:riverwater_block\",\"gate\":\"river\",\"scanTop\":50}]}";
        GT6WaterReplaceConfig.Table tTable = GT6WaterReplaceConfig.Table.CODEC.parse(
                JsonOps.INSTANCE, JsonParser.parseString(tJson)).result().orElseThrow();
        assertEquals(2, tTable.rows().size());
        assertEquals(62, tTable.rows().get(0).scanTop(), "the omitted scanTop rides the 62 default (the upstream Height default)");
        assertEquals(50, tTable.rows().get(1).scanTop(), "an explicit scanTop overrides (the upstream Height knob)");

        // the roundtrip face: serialize back to the same JSON (key order aside, the value face)
        JsonObject tBack = (JsonObject) GT6WaterReplaceConfig.Table.CODEC.encodeStart(JsonOps.INSTANCE, tTable).result().orElseThrow();
        assertEquals(2, tBack.getAsJsonArray("rows").size(), "the encode face keeps both rows");
        assertEquals("ocean", tBack.getAsJsonArray("rows").get(0).getAsJsonObject().get("gate").getAsString(),
                "the lowercase gate spelling roundtrips");

        // the loud-refusal face: an unknown gate trips the codec (no silent fallthrough)
        assertThrows(Exception.class, () -> GT6WaterReplaceConfig.Table.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"rows\":[{\"name\":\"x\",\"block\":\"gt6:x_block\",\"gate\":\"lake\"}]}")).result().orElseThrow(),
                "an unknown gate string must trip the codec");
    }

    // ---------------------------------------------------------------- the generated chain

    private static JsonObject resourceJson(String aPath) throws Exception {
        try (InputStream tStream = GT6WaterReplaceWorldgenTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, aPath + " must ship on the classpath");
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * The configured-feature JSON carries the 3-row table inline, in loader order (the
     * tier-a datapack face — a datapack edit reorders/replaces rows without touching
     * Java, the spring-table posture).
     */
    @Test
    void configuredJsonShipsTheTableInLoaderOrder() throws Exception {
        JsonObject tConfigured = resourceJson("data/gt6/worldgen/configured_feature/water_replace.json");
        assertEquals("gt6:water_replace", tConfigured.get("type").getAsString(), "the registered feature id");
        var tRows = tConfigured.getAsJsonObject("config").getAsJsonArray("rows");
        assertEquals(3, tRows.size(), "the 3-row table");
        assertEquals("ocean.seawater", tRows.get(0).getAsJsonObject().get("name").getAsString(), ":576 first");
        assertEquals("river.riverwater", tRows.get(1).getAsJsonObject().get("name").getAsString(), ":577 second");
        assertEquals("swamp.dirtywater", tRows.get(2).getAsJsonObject().get("name").getAsString(), ":578 third");
        assertEquals("gt6:waterdirty_block", tRows.get(2).getAsJsonObject().get("block").getAsString(),
                "the swamp row targets dirty water");
        assertFalse(tRows.get(0).getAsJsonObject().has("scanTop"), "the 62 default rides the codec, not the JSON");
    }

    /**
     * The biome-modifier JSON in BOTH brands: every overworld biome (the row gates live
     * in-feature), at the TOP_LAYER_MODIFICATION step (the selection report: after kelp/
     * seagrass and freeze_top_layer — the strict-water-gate dependencies).
     */
    @Test
    void biomeModifierShipsTopLayerInBothBrands() throws Exception {
        for (String tBrand : new String[] {"forge", "neoforge"}) {
            JsonObject tRow = resourceJson("data/gt6/" + tBrand + "/biome_modifier/water_replace.json");
            assertEquals(tBrand + ":add_features", tRow.get("type").getAsString(), tBrand + " type brand");
            assertEquals("#minecraft:is_overworld", tRow.get("biomes").getAsString(), tBrand + " biome set");
            assertEquals("gt6:water_replace", tRow.get("features").getAsString(), tBrand + " placed feature");
            assertEquals("top_layer_modification", tRow.get("step").getAsString(),
                    tBrand + " step — the last decoration pass (the selection report's two water-gate dependencies)");
        }
        // the placed chain: one attempt per chunk, no rarity (the row gates are not probabilistic)
        JsonObject tPlaced = resourceJson("data/gt6/worldgen/placed_feature/water_replace.json");
        var tModifiers = tPlaced.getAsJsonArray("placement");
        assertEquals(3, tModifiers.size(), "Count + InSquare + BiomeFilter (the bedrock-ore chain shape)");
    }
}
