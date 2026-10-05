package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.data.MT;
import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GTMaterialItems;

/**
 * The twilight RockOres band pins (task twilight-adaptation-pilot — the mod-dimension
 * adaptation skeleton's first tenant): the 8-row census (Loader_Worldgen.java:666-673
 * verbatim, the meta mapping = BlockRockOres.ORE_MATERIALS, BlockRockOres.java:36), the
 * axis gate's 3/5 split, the feature/tag keys, and the twilight_ores biome-modifier JSON
 * in BOTH leg brands (the acceptance's 双腿各自家品牌 face — the GT6NetherWorldgenTest
 * posture, JSON reads off the CLASSPATH). The KJS face: everything here is datapack-domain
 * (the biome-modifier + feature JSONs are player-editable) and the mount condition is the
 * declared mod_loaded condition — no other surface.
 *
 * <p>THE TF-ABSENCE SEMANTICS (④): the modifier carries {@code [<brand>:mod_loaded
 * twilightforest]}; forge ICondition.java:24-30 {@code shouldRegisterEntry} gates the
 * entry BEFORE the codec parses (the RegistryDataLoader generic load loop, the
 * GT6BiomeModifierConditions javadoc face) — without TF the whole entry is skipped at
 * datapack load, the {@code #twilightforest:in_twilight_forest} tag never resolves, and
 * the game logs a debug-level skip instead of any error: zero mounts, zero errors, the
 * unconditioned rows untouched. Pinned here by the JSON shape itself (the conditions
 * array = exactly the positive mod_loaded twilightforest).
 */
public class GT6TwilightWorldgenTest {

    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() {
        // the ResourceKey/TagKey static-init face needs the vanilla bootstrap + the material
        // system (the residual_rule: any test touching the worldgen table classes boots
        // materials FIRST — the forge-test-hygiene 42c9ad4e2 discipline)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline; registries are ready by now.
        }
        GTMaterialItems.initMaterials();
    }

    // ---------------------------------------------------------------- the 8-row census

    /** The upstream row names in :666-673 order; meta i; the ORE_MATERIALS mapping. */
    @Test
    public void twilightRockOreCensusIsPinned() {
        assertEquals(8, GTOreWorldgen.TWILIGHT_ORE_ROWS.size(), "the 8 RockOres rows, :666-673");
        List<String> tNames = List.of("twilight.ore.anthracite", "twilight.ore.lignite", "twilight.ore.salt",
                "twilight.ore.rocksalt", "twilight.ore.bauxite", "twilight.ore.oilshale",
                "twilight.ore.gypsum", "twilight.ore.milkyquartz");
        for (int i = 0; i < tNames.size(); i++) {
            GTOreWorldgen.TwilightOreRow tRow = GTOreWorldgen.TWILIGHT_ORE_ROWS.get(i);
            assertEquals(tNames.get(i), tRow.name(), "row " + i + " name, :666-673 order");
            assertEquals(i, tRow.meta(), "row " + i + " RockOres meta (BlockRockOres.ORE_MATERIALS slot)");
        }
        // the BlockRockOres.java:36 material mapping, identity-pinned (the ORE_MATERIALS slots)
        assertSame(MT.Coal, GTOreWorldgen.TWILIGHT_ORE_ROWS.get(0).material().get(), "meta 0 anthracite");
        assertSame(MT.Lignite, GTOreWorldgen.TWILIGHT_ORE_ROWS.get(1).material().get(), "meta 1 lignite");
        assertSame(MT.NaCl, GTOreWorldgen.TWILIGHT_ORE_ROWS.get(2).material().get(), "meta 2 salt");
        assertSame(MT.KCl, GTOreWorldgen.TWILIGHT_ORE_ROWS.get(3).material().get(), "meta 3 rocksalt");
        assertSame(MT.OREMATS.Bauxite, GTOreWorldgen.TWILIGHT_ORE_ROWS.get(4).material().get(), "meta 4 bauxite");
        assertSame(MT.Oilshale, GTOreWorldgen.TWILIGHT_ORE_ROWS.get(5).material().get(), "meta 5 oilshale");
        assertSame(MT.Gypsum, GTOreWorldgen.TWILIGHT_ORE_ROWS.get(6).material().get(), "meta 6 gypsum");
        assertSame(MT.MilkyQuartz, GTOreWorldgen.TWILIGHT_ORE_ROWS.get(7).material().get(), "meta 7 milkyquartz");
    }

    /** The shared WorldgenBlob columns (WorldgenBlob.java:53-57 binds — every row identical). */
    @Test
    public void twilightBlobColumnsArePinned() {
        assertEquals(1, GTOreWorldgen.TWILIGHT_ORE_AMOUNT, "Amount=1 (the ctor arg, bind 1..16)");
        assertEquals(50, GTOreWorldgen.TWILIGHT_ORE_SIZE, "Size=50 (the ctor arg, bind 4..250 — under the 64 codec cap)");
        assertEquals(100, GTOreWorldgen.TWILIGHT_ORE_PROBABILITY, "Probability=100 -> 1/100 chunk attempts");
        assertEquals(16, GTOreWorldgen.TWILIGHT_ORE_MIN_Y, "MinHeight 16");
        assertEquals(32, GTOreWorldgen.TWILIGHT_ORE_MAX_Y, "MaxHeight 32");
    }

    // ---------------------------------------------------------------- the axis gate

    /**
     * The axis gate reads the LIVE material axis: at the card's base Coal/NaCl/KCl were
     * axis members (WORLDGEN_ORES :832/:820/:821) and the other five sat off-axis as table
     * data (the molybdenum large-vein precedent); the axis-extension batch 2 opened
     * Lignite/Bauxite/Oilshale/Gypsum/MilkyQuartz (EDGE_ORES :481-571 call anchors), so
     * the SAME gate now emits the FULL table, zero band edits (the designed revival —
     * the review-seat re-measure, task worldgen-axis-batch2 union).
     */
    @Test
    public void twilightAxisGateFollowsTheLiveAxis() {
        List<String> tOnAxis = GTOreWorldgen.twilightOnAxisRows().stream().map(GTOreWorldgen.TwilightOreRow::tail).toList();
        assertEquals(List.of("anthracite", "lignite", "salt", "rocksalt", "bauxite", "oilshale", "gypsum", "milkyquartz"),
                tOnAxis, "the full table emits, table order (the batch2 axis opening)");
        List<String> tOffAxis = GTOreWorldgen.TWILIGHT_ORE_ROWS.stream()
                .filter(tRow -> !GTOreWorldgen.twilightOnAxis(tRow))
                .map(GTOreWorldgen.TwilightOreRow::tail).toList();
        assertEquals(List.of(), tOffAxis, "no off-axis remainder");
    }

    // ---------------------------------------------------------------- the keys

    @Test
    public void twilightKeysAndTagArePinned() {
        for (GTOreWorldgen.TwilightOreRow tRow : GTOreWorldgen.TWILIGHT_ORE_ROWS) {
            assertEquals("gt6:twilight_ore/" + tRow.tail(),
                    GTOreWorldgen.twilightConfiguredKey(tRow).location().toString(),
                    tRow.tail() + " configured key");
            assertEquals("gt6:twilight_ore/" + tRow.tail(),
                    GTOreWorldgen.twilightPlacedKey(tRow).location().toString(),
                    tRow.tail() + " placed key shares the path");
            assertEquals("minecraft:worldgen/configured_feature",
                    GTOreWorldgen.twilightConfiguredKey(tRow).registry().toString());
            assertEquals("minecraft:worldgen/placed_feature",
                    GTOreWorldgen.twilightPlacedKey(tRow).registry().toString());
        }
        assertEquals("gt6:twilight_ores",
                GT6WorldgenDatagen.TWILIGHT_ORES_MODIFIER_KEY.location().toString(),
                "the twilight_ores modifier id");
            assertEquals(GT6WorldgenDatagen.biomeModifierRegistryKey().location(),
                GT6WorldgenDatagen.TWILIGHT_ORES_MODIFIER_KEY.registry(),
                "the modifier lives in the leg's biome_modifier registry");
        assertEquals("twilightforest:in_twilight_forest", GTOreWorldgen.twilightBiomeTag().location().toString(),
                "TF's own tag — never emitted here, the datagen resolves it empty (EmptyTagLookup)");
    }

    // ---------------------------------------------------------------- the JSON snapshots

    private static JsonObject resourceJson(String aPath) throws Exception {
        try (InputStream tStream = GT6TwilightWorldgenTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, aPath + " must ship on the classpath");
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * The 3-row modifier in EACH leg's brand: the tag gate + the TF-absence conditions
     * (the acceptance's 3 行 JSON 双腿 + TF 缺席钉). Features order = the table order of the
     * on-axis rows; the conditions = exactly ONE positive {@code mod_loaded twilightforest}
     * (the type-first member order the rebrand walk treats identically,
     * KNOWN_CONDITION_TYPES mod_loaded).
     */
    @Test
    public void twilightModifierShipsTheTfGateInBothBrands() throws Exception {
        for (String tBrand : new String[] {"forge", "neoforge"}) {
            JsonObject tRow = resourceJson("data/gt6/" + tBrand + "/biome_modifier/twilight_ores.json");
            assertEquals(tBrand + ":add_features", tRow.get("type").getAsString(), tBrand + " type brand");
            assertEquals("#twilightforest:in_twilight_forest", tRow.get("biomes").getAsString(),
                    "the TF tag gate (④: never resolved when TF is absent — the condition skips the entry first)");
            JsonArray tFeatures = tRow.getAsJsonArray("features");
            assertNotNull(tFeatures, "the 8-row feature list");
            assertEquals(8, tFeatures.size(), "the 8 axis-valid rows (the batch2 axis opening)");
            assertEquals(List.of("gt6:twilight_ore/anthracite", "gt6:twilight_ore/lignite", "gt6:twilight_ore/salt",
                            "gt6:twilight_ore/rocksalt", "gt6:twilight_ore/bauxite", "gt6:twilight_ore/oilshale",
                            "gt6:twilight_ore/gypsum", "gt6:twilight_ore/milkyquartz"),
                    tFeatures.asList().stream().map(JsonElement::getAsString).toList(), "table order");
            assertEquals("underground_ores", tRow.get("step").getAsString(), "the ore step");
            JsonArray tConditions = tRow.getAsJsonArray(tBrand + ":conditions");
            assertNotNull(tConditions, "the conditions key in the " + tBrand + " brand");
            assertEquals(1, tConditions.size(), "exactly the positive mod_loaded");
            JsonObject tModLoaded = tConditions.get(0).getAsJsonObject();
            assertEquals(tBrand + ":mod_loaded", tModLoaded.get("type").getAsString());
            assertEquals("twilightforest", tModLoaded.get("modid").getAsString(),
                    "THE TF-ABSENCE PIN: without TF the entry never registers (zero mounts, zero errors)");
        }
    }

    /**
     * The 8 configured/placed pairs ship the upstream blob columns: size=50 over the
     * verbatim stone host (WorldgenOresVanilla replaceBlock=null = the vanilla
     * {@code target == Blocks.stone} default), placed = 1/100 rarity + NO count modifier
     * (Amount=1 = the default count) + the 16..32 uniform band. The block = the material's
     * NORMAL ore form on the stone family (the dense-block translation, the declared
     * deviation — no OP.oreDense form on this port).
     */
    @Test
    public void twilightOreFeatureJsonsShipTheUpstreamColumns() throws Exception {
        List<String> tTails = List.of("anthracite", "lignite", "salt", "rocksalt", "bauxite", "oilshale", "gypsum", "milkyquartz");
        // the material snakes = GTMaterialItems.snakeCase(mNameInternal): Coal->coal,
        // NaCl->"Salt"->salt, KCl->"Sylvite"->sylvite (the upstream meta-3 display name,
        // BlockRockOres.java:50 "Sylvite" — the ROW tail says rocksalt, the MATERIAL says sylvite);
        // Lignite->lignite, Bauxite->bauxite, Gypsum->gypsum ride their own names; the
        // camel-cased pairs split: Oilshale->oil_shale, MilkyQuartz->milky_quartz
        List<String> tSnakes = List.of("coal", "lignite", "salt", "sylvite", "bauxite", "oil_shale", "gypsum", "milky_quartz");
        for (int i = 0; i < tTails.size(); i++) {
            String tTail = tTails.get(i);
            JsonObject tConfigured = resourceJson("data/gt6/worldgen/configured_feature/twilight_ore/" + tTail + ".json");
            assertEquals("minecraft:ore", tConfigured.get("type").getAsString(), tTail);
            JsonObject tConfig = tConfigured.getAsJsonObject("config");
            assertEquals(50, tConfig.get("size").getAsInt(), tTail + " size=50 (:666-673, under the 64 cap)");
            assertEquals(0.0, tConfig.get("discard_chance_on_air_exposure").getAsDouble(), tTail + " no air discard");
            JsonArray tTargets = tConfig.getAsJsonArray("targets");
            assertEquals(1, tTargets.size(), tTail + " the single verbatim stone host");
            JsonObject tTarget = tTargets.get(0).getAsJsonObject();
            assertEquals("minecraft:block_match", tTarget.getAsJsonObject("target").get("predicate_type").getAsString());
            assertEquals("minecraft:stone", tTarget.getAsJsonObject("target").get("block").getAsString(),
                    tTail + " the vanilla-stone host (replaceBlock=null semantics)");
            assertEquals("gt6:ore_stone_" + tSnakes.get(i), tTarget.getAsJsonObject("state").get("Name").getAsString(),
                    tTail + " the NORMAL ore form on the stone family");

            JsonObject tPlaced = resourceJson("data/gt6/worldgen/placed_feature/twilight_ore/" + tTail + ".json");
            assertEquals("gt6:twilight_ore/" + tTail, tPlaced.get("feature").getAsString());
            JsonArray tPlacement = tPlaced.getAsJsonArray("placement");
            assertEquals(4, tPlacement.size(), tTail + " rarity + square + height + biome (NO count: Amount=1 default)");
            JsonObject tRarity = tPlacement.get(0).getAsJsonObject();
            assertEquals("minecraft:rarity_filter", tRarity.get("type").getAsString());
            assertEquals(100, tRarity.get("chance").getAsInt(), tTail + " 1/100 chunk attempts");
            assertEquals("minecraft:in_square", tPlacement.get(1).getAsJsonObject().get("type").getAsString());
            JsonObject tHeight = tPlacement.get(2).getAsJsonObject().getAsJsonObject("height");
            assertEquals("minecraft:uniform", tHeight.get("type").getAsString());
            assertEquals(16, tHeight.getAsJsonObject("min_inclusive").get("absolute").getAsInt(), tTail + " MinHeight");
            assertEquals(32, tHeight.getAsJsonObject("max_inclusive").get("absolute").getAsInt(), tTail + " MaxHeight");
            assertEquals("minecraft:biome", tPlacement.get(3).getAsJsonObject().get("type").getAsString());
        }
    }
}
