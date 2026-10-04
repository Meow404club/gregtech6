package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The nether rack pins (task worldgen-racks): the WorldgenRacks port
 * (Loader_Worldgen.java:619 {@code "nether.rocks"}, the GEN_NETHER sister of the
 * overworld rocks band). The JSON reads go off the CLASSPATH
 * (src/generated/resources is a test resource dir, the GT6NetherWorldgenTest posture).
 *
 * <p>Compile anchors: WorldgenRacks.java:46-98 (the gate/scan/lottery), :57-59 (the
 * 16 attempts + the [47, 47+80) window + the 40-deep scan), :65-90 (the 24-case NBT
 * lottery), :68-71 (the debris 3:1 rockGt/oreRaw arm — the loot-table face).
 */
public class GT6RacksWorldgenTest {

    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() {
        // the ResourceKey/Blocks static-init face needs the vanilla bootstrap (the
        // GT6NetherWorldgenTest posture, offline throwables ignored), the material
        // system before MT dereferences (the GT6SurfaceBlocksTest posture)
        GTMaterialItems.initMaterials();
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- the window constants

    /** The Loader_Worldgen.java:619 row binds, transcribed into the Feature constants. */
    @Test
    public void windowConstantsAreTheUpstreamBinds() {
        assertEquals(47, GT6RacksFeature.RACKS_MIN_Y, "the start floor (:59, nextInt(80) + 47)");
        assertEquals(80, GT6RacksFeature.RACKS_SPAN, "the vanilla-height draw span (:59) — the flat-bedrock 200 arm is CUT, no port config face");
        assertEquals(40, GT6RacksFeature.RACKS_DEPTH, "the downward scan depth (:59, tY - 40)");
        assertEquals(16, GT6RacksFeature.RACKS_ATTEMPTS, "the per-chunk attempt count (:57)");
    }

    // ---------------------------------------------------------------- the 24-case lottery

    /**
     * The verbatim :65-90 draw table, every draw x the three contact probes
     * (nether bricks / soul sand-soil / gravel). Plain no-contact draw-count per
     * identity: NETHER_QUARTZ 2 (:0/:3), GLOWSTONE 2, OBSIDIAN 2, BASALT 3,
     * BLACKSTONE 8, FLINT 6 (the :6-11 plain arms), debris 1 + 3 brick arms,
     * GLOOMSTONE 0 (soul-only).
     */
    @Test
    public void drawTableIsTheVerbatimLottery() {
        Map<String, GT6RacksFeature.Rack> tExpected = Map.ofEntries(
                Map.entry("NETHER_QUARTZ", GT6RacksFeature.Rack.NETHER_QUARTZ),
                Map.entry("GLOWSTONE", GT6RacksFeature.Rack.GLOWSTONE),
                Map.entry("ANCIENT_DEBRIS", GT6RacksFeature.Rack.ANCIENT_DEBRIS),
                Map.entry("OBSIDIAN", GT6RacksFeature.Rack.OBSIDIAN),
                Map.entry("BASALT", GT6RacksFeature.Rack.BASALT),
                Map.entry("BLACKSTONE", GT6RacksFeature.Rack.BLACKSTONE),
                Map.entry("GLOOMSTONE", GT6RacksFeature.Rack.GLOOMSTONE),
                Map.entry("FLINT", GT6RacksFeature.Rack.FLINT));
        // the flat arms (:66-:68, :78-:81) — contact-blind
        assertEquals(tExpected.get("NETHER_QUARTZ"), GT6RacksFeature.pick(0, false, false, false), ":66");
        assertEquals(tExpected.get("GLOWSTONE"), GT6RacksFeature.pick(1, false, false, false), ":67");
        assertEquals(tExpected.get("ANCIENT_DEBRIS"), GT6RacksFeature.pick(2, false, false, false), ":68 — the 3:1 split rides the loot table");
        assertEquals(tExpected.get("OBSIDIAN"), GT6RacksFeature.pick(12, false, false, false), ":78");
        assertEquals(tExpected.get("BASALT"), GT6RacksFeature.pick(13, false, false, false), ":79");
        assertEquals(tExpected.get("BASALT"), GT6RacksFeature.pick(14, false, false, false), ":80");
        assertEquals(tExpected.get("BASALT"), GT6RacksFeature.pick(15, false, false, false), ":81");
        // the brick arms (:69-71) — debris on nether bricks, else the base arm
        assertEquals(tExpected.get("ANCIENT_DEBRIS"), GT6RacksFeature.pick(3, true, false, false), ":69 brick");
        assertEquals(tExpected.get("NETHER_QUARTZ"), GT6RacksFeature.pick(3, false, false, false), ":69 plain");
        assertEquals(tExpected.get("ANCIENT_DEBRIS"), GT6RacksFeature.pick(4, true, false, false), ":70 brick");
        assertEquals(tExpected.get("GLOWSTONE"), GT6RacksFeature.pick(4, false, false, false), ":70 plain");
        assertEquals(tExpected.get("ANCIENT_DEBRIS"), GT6RacksFeature.pick(5, true, false, false), ":71 brick");
        assertEquals(tExpected.get("OBSIDIAN"), GT6RacksFeature.pick(5, false, false, false), ":71 plain");
        // the soul arms (:72-77) — gloomstone pair then quartz quartet, flint otherwise
        assertEquals(tExpected.get("GLOOMSTONE"), GT6RacksFeature.pick(6, false, true, false), ":72 soul");
        assertEquals(tExpected.get("FLINT"), GT6RacksFeature.pick(6, false, false, false), ":72 plain flint");
        assertEquals(tExpected.get("GLOOMSTONE"), GT6RacksFeature.pick(7, false, true, false), ":73 soul");
        assertEquals(tExpected.get("FLINT"), GT6RacksFeature.pick(7, false, false, false), ":73 plain flint");
        for (int tDraw = 8; tDraw <= 11; tDraw++) {
            assertEquals(tExpected.get("NETHER_QUARTZ"), GT6RacksFeature.pick(tDraw, false, true, false), ":" + tDraw + " soul");
            assertEquals(tExpected.get("FLINT"), GT6RacksFeature.pick(tDraw, false, false, false), ":" + tDraw + " plain flint");
        }
        // the gravel arms (:82-89) — flint on gravel, blackstone otherwise
        for (int tDraw = 16; tDraw <= 23; tDraw++) {
            assertEquals(tExpected.get("FLINT"), GT6RacksFeature.pick(tDraw, false, false, true), ":" + tDraw + " gravel flint");
            assertEquals(tExpected.get("BLACKSTONE"), GT6RacksFeature.pick(tDraw, false, false, false), ":" + tDraw + " plain blackstone");
        }
        // the draw-count faces (the no-contact arm): quartz only on the bare :0/:3 arms
        // (the :8-11 quartz faces are SOUL-conditional), 2+8 flint, 8 blackstone
        java.util.EnumMap<GT6RacksFeature.Rack, Integer> tCounts = new java.util.EnumMap<>(GT6RacksFeature.Rack.class);
        for (int tDraw = 0; tDraw < 24; tDraw++) {
            tCounts.merge(GT6RacksFeature.pick(tDraw, false, false, false), 1, Integer::sum);
        }
        assertEquals(2, tCounts.get(GT6RacksFeature.Rack.NETHER_QUARTZ), "the quartz plain count (:0,:3 — the :8-11 quartz faces are soul-conditional)");
        assertEquals(2, tCounts.get(GT6RacksFeature.Rack.GLOWSTONE), "the glowstone plain count (:1,:4)");
        assertEquals(1, tCounts.get(GT6RacksFeature.Rack.ANCIENT_DEBRIS), "the debris plain count (:2; the brick arms pay 3 more)");
        assertEquals(2, tCounts.get(GT6RacksFeature.Rack.OBSIDIAN), "the obsidian count (:5,:12)");
        assertEquals(3, tCounts.get(GT6RacksFeature.Rack.BASALT), "the basalt count (:13-15)");
        assertEquals(8, tCounts.get(GT6RacksFeature.Rack.BLACKSTONE), "the blackstone count (:16-23)");
        assertEquals(0, tCounts.getOrDefault(GT6RacksFeature.Rack.GLOOMSTONE, 0), "gloomstone rides the soul arms only (:6-7)");
        assertEquals(6, tCounts.get(GT6RacksFeature.Rack.FLINT), "the flint count (:6-7 plain, :8-11 plain = 6; the 16-23 plain arms pay blackstone)");
    }

    // ---------------------------------------------------------------- the registration face

    /** The 7 nether rack rocks, Rack draw-table order, materials 1:1; the flint arm reuses the first-batch row. */
    @Test
    public void netherRocksPairTheirMaterialsInDrawOrder() {
        List<String> tPaths = List.of("surface_rock_nether_quartz", "surface_rock_glowstone",
                "surface_rock_ancient_debris", "surface_rock_obsidian", "surface_rock_basalt",
                "surface_rock_blackstone", "surface_rock_gloomstone");
        assertEquals(tPaths, GT6SurfaceBlocks.NETHER_ROCKS.stream().map(tRow -> tRow.getId().getPath()).toList(),
                "the 7 nether rack rock ids, draw-table order");
        assertEquals(7, GT6SurfaceBlocks.NETHER_MATERIALS.size(), "materials 1:1");
        assertEquals(GTMaterialItems.snakeCase(gregapi.data.MT.NetherQuartz.mNameInternal), "nether_quartz",
                "the id snake rides the material snake face (the indicator-rows convention)");
        // the flint arm is the SHARED first-batch block (WorldgenRacks.java:72 Items.flint
        // == the WorldgenRocks.java:63 flint arm, one identity both bands)
        assertTrue(GT6SurfaceBlocks.ALL.contains(GT6SurfaceBlocks.SURFACE_ROCK_FLINT),
                "the first-batch flint rock stays in the census walk unit");
    }

    /**
     * The wiring pin: the 7 nether rocks ride ALL (the blockstate/tint/prospector/loot
     * walk unit — a rock outside ALL has no blockstate JSON, no loot table, no tint).
     */
    @Test
    public void netherRocksRideTheCensusWalkUnit() {
        for (int i = 0; i < GT6SurfaceBlocks.NETHER_ROCKS.size(); i++) {
            assertTrue(GT6SurfaceBlocks.ALL.contains(GT6SurfaceBlocks.NETHER_ROCKS.get(i)),
                    GT6SurfaceBlocks.NETHER_ROCKS.get(i).getId().getPath() + " must ride ALL");
        }
        // the loot item faces exist in the headless enumeration (the GT6SurfaceBlocksTest
        // indicator posture — offline handles resolve only post-registration): gems to
        // OP.gem, stones to OP.rockGt, the debris to both rockGt and oreRaw
        java.util.Set<String> tItemIds = GTMaterialItems.registrationOrder().stream()
                .map(tPair -> GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()))
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(tItemIds.contains("gem_" + GTMaterialItems.snakeCase(gregapi.data.MT.NetherQuartz.mNameInternal)), "gem_nether_quartz");
        assertTrue(tItemIds.contains("gem_" + GTMaterialItems.snakeCase(gregapi.data.MT.Glowstone.mNameInternal)), "gem_glowstone");
        assertTrue(tItemIds.contains("gem_" + GTMaterialItems.snakeCase(gregapi.data.MT.Gloomstone.mNameInternal)), "gem_gloomstone");
        assertTrue(tItemIds.contains("rock_gt_" + GTMaterialItems.snakeCase(gregapi.data.MT.Obsidian.mNameInternal)), "rock_gt_obsidian");
        assertTrue(tItemIds.contains("rock_gt_" + GTMaterialItems.snakeCase(gregapi.data.MT.STONES.Basalt.mNameInternal)), "rock_gt_basalt");
        assertTrue(tItemIds.contains("rock_gt_" + GTMaterialItems.snakeCase(gregapi.data.MT.STONES.Blackstone.mNameInternal)), "rock_gt_blackstone");
        assertTrue(tItemIds.contains("rock_gt_" + GTMaterialItems.snakeCase(gregapi.data.MT.AncientDebris.mNameInternal)), "rock_gt_ancient_debris");
        assertTrue(tItemIds.contains("ore_raw_" + GTMaterialItems.snakeCase(gregapi.data.MT.AncientDebris.mNameInternal)), "ore_raw_ancient_debris");
    }

    // ---------------------------------------------------------------- the worldgen rows

    /** The configured row: gt6:nether_racks over the bare codec (the nether-form shape). */
    @Test
    public void configuredRowShipsTheRacksFeature() throws Exception {
        JsonObject tRow = resourceJson("data/gt6/worldgen/configured_feature/nether_racks.json");
        assertEquals("gt6:nether_racks", tRow.get("type").getAsString(), "the registered GT6Features.NETHER_RACKS id");
        assertEquals(0, tRow.get("config").getAsJsonObject().size(), "NoneFeatureConfiguration — the upstream constants live in the class");
    }

    /** The placed row: the :54 nextBoolean chunk gate as rarity 2 + one attempt per gated chunk. */
    @Test
    public void placedRowCarriesTheHalfChunkGate() throws Exception {
        JsonObject tRow = resourceJson("data/gt6/worldgen/placed_feature/nether_racks.json");
        assertEquals("gt6:nether_racks", tRow.get("feature").getAsString());
        var tPlacement = tRow.getAsJsonArray("placement");
        assertEquals(4, tPlacement.size(), "rarity + count + square + biome");
        JsonObject tRarity = tPlacement.get(0).getAsJsonObject();
        assertEquals("minecraft:rarity_filter", tRarity.get("type").getAsString(), "the :54 nextBoolean gate");
        assertEquals(2, tRarity.get("chance").getAsInt(), "onAverageOnceEvery(2) = the 50% chunk draw");
        assertEquals("minecraft:count", tPlacement.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals(1, tPlacement.get(1).getAsJsonObject().get("count").getAsInt(), "one attempt bundle per gated chunk (the 16 columns live in the Feature)");
        assertEquals("minecraft:in_square", tPlacement.get(2).getAsJsonObject().get("type").getAsString());
        assertEquals("minecraft:biome", tPlacement.get(3).getAsJsonObject().get("type").getAsString());
    }

    /** The biome modifier, both brands: every nether biome (GEN_NETHER), the surface-deco step. */
    @Test
    public void biomeModifierShipsBothBrandsOverTheNetherTag() throws Exception {
        for (String tBrand : new String[] {"forge", "neoforge"}) {
            JsonObject tRow = resourceJson("data/gt6/" + tBrand + "/biome_modifier/nether_racks.json");
            assertEquals(tBrand + ":add_features", tRow.get("type").getAsString(), tBrand + " brand type");
            assertEquals("#minecraft:is_nether", tRow.get("biomes").getAsString(),
                    "GEN_NETHER = the biome-tag face (the nether-forms convention)");
            assertEquals("gt6:nether_racks", tRow.get("features").getAsString());
            assertEquals("vegetal_decoration", tRow.get("step").getAsString(),
                    "the surface-deco step (the overworld rocks band convention — the racks are its nether sister)");
        }
    }

    // ---------------------------------------------------------------- the loot faces

    /** The 7 rack loot tables: the collected 1..2 face, the debris 3:1 rockGt/oreRaw arm (:68). */
    @Test
    public void rackLootShipsTheNetherArms() throws Exception {
        for (String[] tRow : new String[][] {
                {"surface_rock_nether_quartz", "gt6:gem_nether_quartz"},
                {"surface_rock_glowstone", "gt6:gem_glowstone"},
                {"surface_rock_gloomstone", "gt6:gem_gloomstone"},
                {"surface_rock_obsidian", "gt6:rock_gt_obsidian"},
                {"surface_rock_basalt", "gt6:rock_gt_basalt"},
                {"surface_rock_blackstone", "gt6:rock_gt_blackstone"}}) {
            JsonObject tTable = resourceJson("data/gt6/loot_tables/blocks/" + tRow[0] + ".json");
            JsonObject tEntry = tTable.getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals(1, tTable.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").size(),
                    tRow[0] + ": the single-identity collected face");
            assertEquals(tRow[1], tEntry.get("name").getAsString(), tRow[0] + " carries its material item");
        }
        // the debris rock: the :68 nextInt(4)==0 ? oreRaw : rockGt arm as the 3:1 weights
        // (the meteorite loot precedent, surface_rock_meteorite)
        JsonObject tDebris = resourceJson("data/gt6/loot_tables/blocks/surface_rock_ancient_debris.json");
        var tEntries = tDebris.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        assertEquals(2, tEntries.size(), "the two debris arms");
        assertEquals("gt6:rock_gt_ancient_debris", tEntries.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals(3, tEntries.get(0).getAsJsonObject().get("weight").getAsInt(), "rockGt weight 3");
        assertEquals("gt6:ore_raw_ancient_debris", tEntries.get(1).getAsJsonObject().get("name").getAsString());
        assertEquals(1, tEntries.get(1).getAsJsonObject().get("weight").getAsInt(), "oreRaw weight 1");
    }

    private static JsonObject resourceJson(String aPath) throws Exception {
        try (InputStream tStream = GT6RacksWorldgenTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, aPath + " must ship on the classpath");
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
