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
import gregtech6.registry.GTStoneBlocks;

/**
 * The twilight band pins (task twilight-adaptation-pilot — the mod-dimension adaptation
 * skeleton's first tenant — extended by task twilight-vanilla-ores-deadrock): the 8-row
 * RockOres census (Loader_Worldgen.java:666-673 verbatim, the meta mapping =
 * BlockRockOres.ORE_MATERIALS, BlockRockOres.java:36), the 16-row VanillaOresA census
 * (:677-692, BlockVanillaOresA.java:48) + the netherite row (:712), the axis gates'
 * 3/5 and 12/4 splits, the feature/tag keys, and the twilight_ores biome-modifier JSON
 * in BOTH leg brands (the acceptance's 双腿各自家品牌 face — the GT6NetherWorldgenTest
 * posture, JSON reads off the CLASSPATH). The KJS face: everything here is datapack-domain
 * (the biome-modifier + feature + tag JSONs are player-editable) and the mount condition is
 * the declared mod_loaded condition — no other surface.
 *
 * <p>THE TF-ABSENCE SEMANTICS (④): the modifier carries {@code [<brand>:mod_loaded
 * twilightforest]}; forge ICondition.java:24-30 {@code shouldRegisterEntry} gates the
 * entry BEFORE the codec parses (the RegistryDataLoader generic load loop, the
 * GT6BiomeModifierConditions javadoc face) — without TF the whole entry is skipped at
 * datapack load, the {@code #twilightforest:in_twilight_forest} tag never resolves, and
 * the game logs a debug-level skip instead of any error: zero mounts, zero errors, the
 * unconditioned rows untouched. Pinned here by the JSON shape itself (the conditions
 * array = exactly the positive mod_loaded twilightforest).
 *
 * <p>Extended by task twilight-stone-rows: the 17 WorldgenStone twilight rows
 * (Loader_Worldgen.java:654 loop, :657 row) as placed variants over the 12-blob-reuse +
 * 5-lens-own configured split, the ONE twilight_stones modifier row (CONDITION_ROWS row
 * 3), and the erratum columns (amount=1, size=100, probability=200, Y0-40 — the card
 * face's 200/100 were the overworld :655 numbers transposed).
 */
public class GT6TwilightWorldgenTest {

    /** The 17 stone snakes, GTStoneBlocks.STONES order (the acceptance 17-row audit unit). */
    private static final List<String> TWILIGHT_STONE_SNAKES = List.of(
            "granite_black", "granite_red", "basalt", "marble", "limestone", "granite", "diorite",
            "andesite", "komatiite", "greenschist", "blueschist", "kimberlite", "quartzite",
            "prismarine_light", "prismarine_dark", "slate", "shale");

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
        // twilightOnAxisRows() is the WHOLE twilight universe gate (RockOres table + the
        // VanillaOresA band + netherite): 8 at the pilot card, 23 since the deadrock band
        // joined (8 + 14 + 1 — the B1 axis carried rutile/bastnasite)
        List<String> tOnAxis = GTOreWorldgen.twilightOnAxisRows().stream().map(GTOreWorldgen.TwilightOreRow::tail).toList();
        assertEquals(List.of(
                "anthracite", "lignite", "salt", "rocksalt", "bauxite", "oilshale", "gypsum", "milkyquartz",
                "sulfur", "ruby", "amber", "amethyst", "galena", "cassiterite", "cooperite", "pentlandite",
                "scheelite", "rutile", "bastnasite", "graphite", "pitchblende", "borax", "netherite"),
                tOnAxis, "the full twilight universe emits, band order (the live axis gate)");
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

    // ---------------------------------------------------------------- the bumble-hive mount (task twilight-hives-springs)

    /**
     * The FOURTH hive modifier (task twilight-hives-springs): key path + the leg's
     * biome_modifier registry (the TWILIGHT_ORES key pin's twin).
     */
    @Test
    public void twilightHiveModifierKeyIsPinned() {
        assertEquals("gt6:twilight_bumble_hives",
                GT6WorldgenDatagen.TWILIGHT_HIVES_MODIFIER_KEY.location().toString(),
                "the twilight_bumble_hives modifier id");
        assertEquals(GT6WorldgenDatagen.biomeModifierRegistryKey().location(),
                GT6WorldgenDatagen.TWILIGHT_HIVES_MODIFIER_KEY.registry(),
                "the modifier lives in the leg's biome_modifier registry");
    }

    /**
     * The hive-mount modifier in EACH leg's brand: the TF tag gate + the shared
     * {@code gt6:bumble_hives} placed feature (the nether_bumble_hives same-placed-key
     * convention) + the ore step + the TF-absence conditions = exactly ONE positive
     * {@code mod_loaded twilightforest}.
     */
    @Test
    public void twilightHiveModifierShipsTheTfGateInBothBrands() throws Exception {
        for (String tBrand : new String[] {"forge", "neoforge"}) {
            JsonObject tRow = resourceJson("data/gt6/" + tBrand + "/biome_modifier/twilight_bumble_hives.json");
            assertEquals(tBrand + ":add_features", tRow.get("type").getAsString(), tBrand + " type brand");
            assertEquals("#twilightforest:in_twilight_forest", tRow.get("biomes").getAsString(),
                    "the TF tag gate (never resolved when TF is absent — the condition skips the entry first)");
            assertEquals("gt6:bumble_hives", tRow.get("features").getAsString(),
                    "the shared placed feature (the FOURTH modifier over the SAME placed key)");
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
     * The magical family tag ships the TF slice: exactly ONE optional entry (required:false
     * — the deadrock red-line), the vanilla-zero discipline holds (TF absent = empty tag).
     */
    @Test
    public void magicalHiveTagShipsTheTfSliceRequiredFalse() throws Exception {
        JsonObject tTag = resourceJson("data/gt6/tags/worldgen/biome/bumble_hives/magical.json");
        assertTrue(!tTag.has("replace") || !tTag.get("replace").getAsBoolean(), "join semantics");
        JsonArray tValues = tTag.getAsJsonArray("values");
        assertEquals(1, tValues.size(), "exactly the enchanted_forest slice");
        JsonObject tEntry = tValues.get(0).getAsJsonObject();
        assertEquals("twilightforest:enchanted_forest", tEntry.get("id").getAsString());
        assertEquals(false, tEntry.get("required").getAsBoolean(),
                "required:false — TF absent = empty tag = no crash, no hang");
    }

    /**
     * The twilight FLUID-SPRING mount (task twilight-hives-springs): the modifier key twin
     * of the hive mount.
     */
    @Test
    public void twilightSpringModifierKeyIsPinned() {
        assertEquals("gt6:twilight_fluid_springs",
                GT6WorldgenDatagen.TWILIGHT_FLUID_SPRINGS_MODIFIER_KEY.location().toString(),
                "the twilight_fluid_springs modifier id");
        assertEquals(GT6WorldgenDatagen.biomeModifierRegistryKey().location(),
                GT6WorldgenDatagen.TWILIGHT_FLUID_SPRINGS_MODIFIER_KEY.registry(),
                "the modifier lives in the leg's biome_modifier registry");
    }

    /**
     * The spring-mount modifier in EACH leg's brand: the TF tag gate + the shared
     * {@code gt6:fluid_springs} placed feature (the nether_fluid_springs same-placed-key
     * convention) + the ore step + the TF-absence conditions = exactly ONE positive
     * {@code mod_loaded twilightforest}. THE ROW MASKS do the band selection (the twilight
     * column, the :795-796 rows) — the Feature's three-state routing never walks the OW
     * oil/gas band in TF (the hasCeiling-binary trap).
     */
    @Test
    public void twilightSpringModifierShipsTheTfGateInBothBrands() throws Exception {
        for (String tBrand : new String[] {"forge", "neoforge"}) {
            JsonObject tRow = resourceJson("data/gt6/" + tBrand + "/biome_modifier/twilight_fluid_springs.json");
            assertEquals(tBrand + ":add_features", tRow.get("type").getAsString(), tBrand + " type brand");
            assertEquals("#twilightforest:in_twilight_forest", tRow.get("biomes").getAsString(),
                    "the TF tag gate (never resolved when TF is absent — the condition skips the entry first)");
            assertEquals("gt6:fluid_springs", tRow.get("features").getAsString(),
                    "the shared placed feature (the nether_fluid_springs same-placed-key convention)");
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

    // ---------------------------------------------------------------- the JSON snapshots

    private static JsonObject resourceJson(String aPath) throws Exception {
        try (InputStream tStream = GT6TwilightWorldgenTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, aPath + " must ship on the classpath");
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * The 3-row modifier in EACH leg's brand: the tag gate + the TF-absence conditions
     * (the acceptance's 3 行 JSON 双腿 + TF 缺席钉). Features order = the band order of the
     * on-axis rows (RockOres band, then the VanillaOresA band, then netherite — 23 rows
     * since the twilight-vanilla-ores-deadrock band + the axis openings); the conditions = exactly ONE positive
     * {@code mod_loaded twilightforest} (the type-first member order the rebrand walk
     * treats identically, KNOWN_CONDITION_TYPES mod_loaded).
     */
    @Test
    public void twilightModifierShipsTheTfGateInBothBrands() throws Exception {
        // the band order = the 8 RockOres rows (table order — the batch2 axis opening) +
        // the 14 on-axis VanillaOresA rows (rutile/bastnasite rode the B1 large-vein axis,
        // the review-seat re-measure) + netherite = 23 (the card's base measured 16:
        // 3 RockOres + 12 A + netherite)
        List<String> tExpected = List.of(
                "gt6:twilight_ore/anthracite", "gt6:twilight_ore/lignite", "gt6:twilight_ore/salt",
                "gt6:twilight_ore/rocksalt", "gt6:twilight_ore/bauxite", "gt6:twilight_ore/oilshale",
                "gt6:twilight_ore/gypsum", "gt6:twilight_ore/milkyquartz",
                "gt6:twilight_ore/sulfur", "gt6:twilight_ore/ruby", "gt6:twilight_ore/amber",
                "gt6:twilight_ore/amethyst", "gt6:twilight_ore/galena", "gt6:twilight_ore/cassiterite",
                "gt6:twilight_ore/cooperite", "gt6:twilight_ore/pentlandite", "gt6:twilight_ore/scheelite",
                "gt6:twilight_ore/rutile", "gt6:twilight_ore/bastnasite",
                "gt6:twilight_ore/graphite", "gt6:twilight_ore/pitchblende", "gt6:twilight_ore/borax",
                "gt6:twilight_ore/netherite");
        for (String tBrand : new String[] {"forge", "neoforge"}) {
            JsonObject tRow = resourceJson("data/gt6/" + tBrand + "/biome_modifier/twilight_ores.json");
            assertEquals(tBrand + ":add_features", tRow.get("type").getAsString(), tBrand + " type brand");
            assertEquals("#twilightforest:in_twilight_forest", tRow.get("biomes").getAsString(),
                    "the TF tag gate (④: never resolved when TF is absent — the condition skips the entry first)");
            JsonArray tFeatures = tRow.getAsJsonArray("features");
            assertNotNull(tFeatures, "the 23-row feature list");
            assertEquals(23, tFeatures.size(), "the 23 axis-valid rows (8 RockOres + 14 VanillaOresA + netherite)");
            assertEquals(tExpected, tFeatures.asList().stream().map(JsonElement::getAsString).toList(), "band order");
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

    // ---------------------------------------------------------------- the 16 VanillaOresA rows (task twilight-vanilla-ores-deadrock)

    /** The upstream row names in :677-692 order; meta i; the ORE_MATERIALS mapping (BlockVanillaOresA.java:48). */
    @Test
    public void twilightVanillaOreACensusIsPinned() {
        assertEquals(16, GTOreWorldgen.TWILIGHT_ORE_ROWS_A.size(), "the 16 VanillaOresA rows, :677-692");
        List<String> tNames = List.of("twilight.ore.sulfur", "twilight.ore.apatite", "twilight.ore.ruby",
                "twilight.ore.amber", "twilight.ore.amethyst", "twilight.ore.galena", "twilight.ore.tetrahedrite",
                "twilight.ore.cassiterite", "twilight.ore.cooperite", "twilight.ore.pentlandite",
                "twilight.ore.scheelite", "twilight.ore.rutile", "twilight.ore.bastnasite", "twilight.ore.graphite",
                "twilight.ore.pitchblende", "twilight.ore.borax");
        for (int i = 0; i < tNames.size(); i++) {
            GTOreWorldgen.TwilightOreRow tRow = GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(i);
            assertEquals(tNames.get(i), tRow.name(), "row " + i + " name, :677-692 order");
            assertEquals(i, tRow.meta(), "row " + i + " VanillaOresA meta (BlockVanillaOresA.ORE_MATERIALS slot)");
            assertNull(tRow.hostTag(), "row " + i + " is stone-hosted (replaceBlock=null)");
        }
        // the BlockVanillaOresA.java:48 material mapping, identity-pinned (the ORE_MATERIALS slots)
        assertSame(MT.S                   , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(0).material().get(), "meta 0 sulfur");
        assertSame(MT.Apatite             , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(1).material().get(), "meta 1 apatite");
        assertSame(MT.Ruby                , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(2).material().get(), "meta 2 ruby");
        assertSame(MT.Amber               , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(3).material().get(), "meta 3 amber");
        assertSame(MT.Amethyst            , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(4).material().get(), "meta 4 amethyst");
        assertSame(MT.OREMATS.Galena      , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(5).material().get(), "meta 5 galena");
        assertSame(MT.OREMATS.Tetrahedrite, GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(6).material().get(), "meta 6 tetrahedrite");
        assertSame(MT.OREMATS.Cassiterite , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(7).material().get(), "meta 7 cassiterite");
        assertSame(MT.OREMATS.Cooperite   , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(8).material().get(), "meta 8 cooperite");
        assertSame(MT.OREMATS.Pentlandite , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(9).material().get(), "meta 9 pentlandite");
        assertSame(MT.OREMATS.Scheelite   , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(10).material().get(), "meta 10 scheelite");
        assertSame(MT.TiO2                , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(11).material().get(),
                "meta 11: the \"Rutile Ore\" slot carries MT.TiO2 (BlockVanillaOresA.java:48/:63 — the KCl-sylvite mismatch kin)");
        assertSame(MT.OREMATS.Bastnasite  , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(12).material().get(), "meta 12 bastnasite");
        assertSame(MT.Graphite            , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(13).material().get(), "meta 13 graphite");
        assertSame(MT.OREMATS.Pitchblende , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(14).material().get(), "meta 14 pitchblende");
        assertSame(MT.OREMATS.Borax       , GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(15).material().get(), "meta 15 borax");
    }

    /** The per-row WorldgenBlob columns, :677-692 order, verbatim (amount/size/probability/Y band + the BIOMES_* marker). */
    @Test
    public void twilightVanillaOreAColumnsAreVerbatim() {
        // {amount, size, probability, minY, maxY} per row, Loader_Worldgen.java:677-692 ctor args 5-9
        int[][] tCols = {
                {1, 16, 1,  0,  8},  // :677 sulfur
                {1, 16, 2, 24, 32},  // :678 apatite
                {1, 12, 1, 40, 52},  // :679 ruby
                {1, 12, 1, 40, 52},  // :680 amber
                {1, 12, 1, 40, 52},  // :681 amethyst
                {1, 24, 4,  8, 32},  // :682 galena
                {1, 24, 4,  8, 32},  // :683 tetrahedrite
                {1, 24, 4,  8, 32},  // :684 cassiterite
                {1,  6, 1, 40, 52},  // :685 cooperite
                {1, 16, 4,  8, 24},  // :686 pentlandite
                {1, 12, 4,  8, 24},  // :687 scheelite
                {1,  6, 1,  8, 24},  // :688 rutile
                {1, 16, 1, 40, 52},  // :689 bastnasite
                {1,  6, 2,  0,  8},  // :690 graphite
                {1, 16, 1,  8, 16},  // :691 pitchblende
                {2,  6, 1,  0, 16}}; // :692 borax
        // the BIOMES_* group markers (CS.java:263/250/281) — DATA ONLY: no per-group emission this card
        String[] tBiomes = {null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                "swamp", "lake"};
        for (int i = 0; i < tCols.length; i++) {
            GTOreWorldgen.TwilightOreRow tRow = GTOreWorldgen.TWILIGHT_ORE_ROWS_A.get(i);
            assertEquals(tCols[i][0], tRow.amount(), tRow.tail() + " Amount");
            assertEquals(tCols[i][1], tRow.size(), tRow.tail() + " Size");
            assertEquals(tCols[i][2], tRow.probability(), tRow.tail() + " Probability");
            assertEquals(tCols[i][3], tRow.minY(), tRow.tail() + " MinHeight");
            assertEquals(tCols[i][4], tRow.maxY(), tRow.tail() + " MaxHeight");
            assertEquals(tBiomes[i], tRow.biomes(), tRow.tail() + " the BIOMES_* group marker");
        }
    }

    /**
     * The netherite row (:712) — the identifying columns AND the deadrock meta2 host
     * archaeology: GT6's own LoaderItemList.java:979-981 binds meta 0 weathered /
     * meta 1 cracked / meta 2 plain {@code IL.TF_Deadrock}; modern TF is the 1:1 block
     * split (TFBlocks.java:163-165 weathered_deadrock/cracked_deadrock/deadrock), so the
     * meta-2 host is {@code twilightforest:deadrock}, carried ONLY inside the
     * {@code gt6:tf_deadrock} tag (required:false), never in the feature JSON.
     */
    @Test
    public void twilightNetheriteRowIsPinned() {
        GTOreWorldgen.TwilightOreRow tRow = GTOreWorldgen.TWILIGHT_NETHERITE_ROW;
        assertNotNull(tRow, "the :712 netherite row");
        assertEquals("twilight.ore.netherite", tRow.name());
        assertEquals(0, tRow.meta(), "the ancient-debris block meta");
        assertSame(MT.AncientDebris, tRow.material().get(), "the placed material");
        assertEquals(8, tRow.amount(), "Amount=8 (:712)");
        assertEquals(8, tRow.size(), "Size=8 (:712)");
        assertEquals(1, tRow.probability(), "Probability=1 (:712 — every chunk)");
        assertEquals(8, tRow.minY(), "MinHeight 8");
        assertEquals(80, tRow.maxY(), "MaxHeight 80");
        assertEquals("mountains", tRow.biomes(), "the BIOMES_MOUNTAINS marker (data only — no per-group emission this card)");
        assertEquals("tf_deadrock", tRow.hostTag(), "the tag-host face");
        assertEquals("gt6:tf_deadrock", GTOreWorldgen.twilightHostTag(tRow).location().toString(),
                "the block-tag key");
        assertEquals("gt6:tf_deadrock", GTOreWorldgen.twilightDeadrockTag().location().toString(),
                "the tag-file census key");
    }

    /**
     * The A-band axis gate reads the LIVE axis: at the card's base the split was 12/4
     * (apatite/tetrahedrite/rutile(TiO2)/bastnasite off-axis, table data); the B1
     * large-vein axis (ce6acae9a) carried rutile(TiO2)/bastnasite onto the axis, so the
     * SAME gate now emits 14 (the review-seat re-measure). The netherite row rides the
     * axis via its :852 slot — the combined emission set = 8 + 14 + 1 = 23 over the
     * batch2-opened RockOres table.
     */
    @Test
    public void twilightVanillaOreAAxisGateFollowsTheLiveAxis() {
        List<String> tOnAxis = GTOreWorldgen.TWILIGHT_ORE_ROWS_A.stream()
                .filter(GTOreWorldgen::twilightOnAxis).map(GTOreWorldgen.TwilightOreRow::tail).toList();
        assertEquals(List.of("sulfur", "ruby", "amber", "amethyst", "galena", "cassiterite",
                "cooperite", "pentlandite", "scheelite", "rutile", "bastnasite", "graphite",
                "pitchblende", "borax"), tOnAxis,
                "the fourteen axis members, :677-692 order (rutile/bastnasite rode the B1 axis)");
        List<String> tOffAxis = GTOreWorldgen.TWILIGHT_ORE_ROWS_A.stream()
                .filter(tRow -> !GTOreWorldgen.twilightOnAxis(tRow)).map(GTOreWorldgen.TwilightOreRow::tail).toList();
        assertEquals(List.of("apatite", "tetrahedrite"), tOffAxis,
                "exactly the two off-axis rows, :677-692 order");
        assertTrue(GTOreWorldgen.twilightOnAxis(GTOreWorldgen.TWILIGHT_NETHERITE_ROW),
                "the netherite row's axis slot = the :852 always-on ancientdebris row");
        assertEquals(23, GTOreWorldgen.twilightOnAxisRows().size(), "8 RockOres + 14 A-band + 1 netherite");
    }

    /**
     * THE AXIS-GATE WITNESS MODE (task datagen-axis-lottery-fix — the
     * known_bugs.datagen_axis_emission_jvm_lottery root fix): the gate reads
     * {@link gregtech6.registry.GT6OreBlocks#axisWitness}, whose boot-witness mode
     * (BLOCKS non-empty — a RegisterEvent ran) answers from the immutable boot
     * registration instead of the ambient MT/OP-derived axis, so the emission cannot
     * drift with provider order or JVM warmth. WHICH mode this test JVM takes is
     * leg-dependent and deliberately NOT pinned: the forge test JVM stays offline
     * (BLOCKS empty — the ambient fallback), while the neo test JVM boots far enough to
     * fire RegisterEvent (BLOCKS populated — the FML test transformer, the same boot
     * shape the neo datagen JVM takes), so the pin asserts the MODE-INVARIANT semantics:
     * the anchors (on-axis members true, the declared off-axis apatite false, null
     * false) and the gate == witness equivalence over every census row hold in either
     * mode. The boot-witness emission stability itself is exercised by the runData
     * double-run byte-stability gate (the acceptance's neo runData ×2 23 行实证).
     */
    @Test
    public void twilightAxisGateRidesTheRegistrationWitness() {
        // mode-reporting only: ambient (BLOCKS empty, the forge test JVM) vs boot-witness
        // (BLOCKS non-empty, the neo test JVM) — the anchors below hold in BOTH, never pin the mode
        boolean tAmbientFallback = gregtech6.registry.GT6OreBlocks.blocks().isEmpty();
        assertTrue(gregtech6.registry.GT6OreBlocks.axisWitness(MT.Coal),
                (tAmbientFallback ? "ambient" : "boot-witness") + " mode: an axis member (the :832 always-on row) witnesses true");
        assertTrue(gregtech6.registry.GT6OreBlocks.axisWitness(MT.S), "the reg0001 sulfur axis member witnesses true");
        assertFalse(gregtech6.registry.GT6OreBlocks.axisWitness(MT.Apatite),
                "the declared off-axis apatite (the :678 row stays table data) witnesses false");
        assertFalse(gregtech6.registry.GT6OreBlocks.axisWitness(null), "null resolves false");
        // the gate == witness equivalence over the whole census (the same resolution the gate performs)
        for (GTOreWorldgen.TwilightOreRow tRow : GTOreWorldgen.TWILIGHT_ORE_ROWS) {
            assertEquals(gregtech6.registry.GT6OreBlocks.axisWitness(tRow.material().get()), GTOreWorldgen.twilightOnAxis(tRow),
                    tRow.tail() + ": the gate rides the witness (RockOres band)");
        }
        for (GTOreWorldgen.TwilightOreRow tRow : GTOreWorldgen.TWILIGHT_ORE_ROWS_A) {
            assertEquals(gregtech6.registry.GT6OreBlocks.axisWitness(tRow.material().get()), GTOreWorldgen.twilightOnAxis(tRow),
                    tRow.tail() + ": the gate rides the witness (VanillaOresA band)");
        }
        assertEquals(gregtech6.registry.GT6OreBlocks.axisWitness(MT.AncientDebris),
                GTOreWorldgen.twilightOnAxis(GTOreWorldgen.TWILIGHT_NETHERITE_ROW), "netherite: the gate rides the witness");
    }

    /** The A-band + netherite keys share the twilight_ore directory (the band rule). */
    @Test
    public void twilightVanillaOreAKeysArePinned() {
        for (GTOreWorldgen.TwilightOreRow tRow : GTOreWorldgen.TWILIGHT_ORE_ROWS_A) {
            assertEquals("gt6:twilight_ore/" + tRow.tail(),
                    GTOreWorldgen.twilightConfiguredKey(tRow).location().toString(),
                    tRow.tail() + " configured key");
            assertEquals("gt6:twilight_ore/" + tRow.tail(),
                    GTOreWorldgen.twilightPlacedKey(tRow).location().toString(),
                    tRow.tail() + " placed key shares the path");
        }
        assertEquals("gt6:twilight_ore/netherite",
                GTOreWorldgen.twilightConfiguredKey(GTOreWorldgen.TWILIGHT_NETHERITE_ROW).location().toString(),
                "the netherite key");
    }

    /**
     * The 14 axis-valid A-band configured/placed pairs ship their verbatim columns:
     * stone host + the NORMAL ore-stone form; the placed chain = [rarity?][count?]
     * square + height + biome — rarity only when probability &gt; 1, count only when
     * amount &gt; 1 (amount=1 IS the default count, the pilot band convention).
     */
    @Test
    public void twilightOreAFeatureJsonsShipTheUpstreamColumns() throws Exception {
        // {tail, size, count(0=absent), rarity(0=absent), minY, maxY} — the on-axis twelve, band order
        Object[][] tRows = {
                {"sulfur"     , 16, 0, 0,  0,  8, "sulfur"},
                {"ruby"       , 12, 0, 0, 40, 52, "ruby"},
                {"amber"      , 12, 0, 0, 40, 52, "amber"},
                {"amethyst"   , 12, 0, 0, 40, 52, "amethyst"},
                {"galena"     , 24, 0, 4,  8, 32, "galena"},
                {"cassiterite", 24, 0, 4,  8, 32, "cassiterite"},
                {"cooperite"  ,  6, 0, 0, 40, 52, "cooperite"},
                {"pentlandite", 16, 0, 4,  8, 24, "pentlandite"},
                {"scheelite"  , 12, 0, 4,  8, 24, "scheelite"},
                {"rutile"     ,  6, 0, 0,  8, 24, "rutile"},
                {"bastnasite" , 16, 0, 0, 40, 52, "bastnasite"},
                {"graphite"   ,  6, 0, 2,  0,  8, "graphite"},
                {"pitchblende", 16, 0, 0,  8, 16, "pitchblende"},
                {"borax"      ,  6, 2, 0,  0, 16, "borax"}};
        for (Object[] tSpec : tRows) {
            String tTail = (String) tSpec[0];
            int tSize = (int) tSpec[1], tCount = (int) tSpec[2], tRarity = (int) tSpec[3];
            int tMinY = (int) tSpec[4], tMaxY = (int) tSpec[5];
            String tSnake = (String) tSpec[6];

            JsonObject tConfigured = resourceJson("data/gt6/worldgen/configured_feature/twilight_ore/" + tTail + ".json");
            assertEquals("minecraft:ore", tConfigured.get("type").getAsString(), tTail);
            JsonObject tConfig = tConfigured.getAsJsonObject("config");
            assertEquals(tSize, tConfig.get("size").getAsInt(), tTail + " size verbatim (:677-692)");
            JsonArray tTargets = tConfig.getAsJsonArray("targets");
            assertEquals(1, tTargets.size(), tTail + " the single verbatim stone host");
            JsonObject tTarget = tTargets.get(0).getAsJsonObject();
            assertEquals("minecraft:block_match", tTarget.getAsJsonObject("target").get("predicate_type").getAsString(),
                    tTail + " stone host (replaceBlock=null semantics)");
            assertEquals("minecraft:stone", tTarget.getAsJsonObject("target").get("block").getAsString(), tTail);
            assertEquals("gt6:ore_stone_" + tSnake, tTarget.getAsJsonObject("state").get("Name").getAsString(),
                    tTail + " the NORMAL ore form on the stone family");

            JsonObject tPlaced = resourceJson("data/gt6/worldgen/placed_feature/twilight_ore/" + tTail + ".json");
            assertEquals("gt6:twilight_ore/" + tTail, tPlaced.get("feature").getAsString(), tTail);
            JsonArray tPlacement = tPlaced.getAsJsonArray("placement");
            assertEquals((tRarity > 0 ? 1 : 0) + (tCount > 0 ? 1 : 0) + 3, tPlacement.size(),
                    tTail + " [rarity?][count?] square + height + biome");
            int tIndex = 0;
            if (tRarity > 0) {
                JsonObject tRarityStep = tPlacement.get(tIndex++).getAsJsonObject();
                assertEquals("minecraft:rarity_filter", tRarityStep.get("type").getAsString(), tTail);
                assertEquals(tRarity, tRarityStep.get("chance").getAsInt(), tTail + " 1/" + tRarity + " chunk attempts");
            }
            if (tCount > 0) {
                JsonObject tCountStep = tPlacement.get(tIndex++).getAsJsonObject();
                assertEquals("minecraft:count", tCountStep.get("type").getAsString(), tTail + " the Amount column");
                assertEquals(tCount, tCountStep.get("count").getAsInt(),
                        tTail + " count=" + tCount + " blob attempts");
            }
            assertEquals("minecraft:in_square", tPlacement.get(tIndex++).getAsJsonObject().get("type").getAsString(), tTail);
            JsonObject tHeight = tPlacement.get(tIndex).getAsJsonObject().getAsJsonObject("height");
            assertEquals("minecraft:uniform", tHeight.get("type").getAsString(), tTail);
            assertEquals(tMinY, tHeight.getAsJsonObject("min_inclusive").get("absolute").getAsInt(), tTail + " MinHeight");
            assertEquals(tMaxY, tHeight.getAsJsonObject("max_inclusive").get("absolute").getAsInt(), tTail + " MaxHeight");
            assertEquals("minecraft:biome", tPlacement.get(tIndex + 1).getAsJsonObject().get("type").getAsString(), tTail);
        }
    }

    // ---------------------------------------------------------------- the 17 stone rows (task twilight-stone-rows)

    /**
     * The 17 stone-row placed keys, GTStoneBlocks.STONES order (the acceptance 17-row
     * audit unit): the upstream WorldgenStone twilight row per stone (the :654 loop,
     * :657 row) — INCLUDING the two prismarines upstream excludes at :654 (the declared
     * 17+ deviation, the overworld band's "17 verbatim" face carried into the twilight
     * dim). The configured split: the 12 blob stones REUSE their overworld configured
     * (the same Feature.ORE blob — the effective size is the identical clamp), the 5
     * strata-lens marker stones own a twilight-path blob configured (their overworld
     * blob rows are retired with the lens band, the strata ruling is an overworld face).
     */
    @Test
    public void twilightStoneBandCensusIsPinned() {
        assertEquals(TWILIGHT_STONE_SNAKES, GTStoneBlocks.STONES.stream().map(GTStoneBlocks.StoneSpec::snake).toList(),
                "STONES order is the audit order (GTStoneBlocksRegistrationTest.java:108 face)");
        assertEquals(17, GT6Worldgen.TWILIGHT_STONE_PLACED_KEYS.size(),
                "the 17 placed variants (the 15 upstream :654 rows + the 2 prismarines — the declared deviation)");
        assertEquals(5, GT6Worldgen.LENS_STONE_SNAKES.size(), "12 blob + 5 lens = 17");
        for (int i = 0; i < TWILIGHT_STONE_SNAKES.size(); i++) {
            String tSnake = TWILIGHT_STONE_SNAKES.get(i);
            assertEquals("gt6:twilight_stone_" + tSnake,
                    GT6Worldgen.TWILIGHT_STONE_PLACED_KEYS.get(i).location().toString(),
                    tSnake + " placed key (STONES order)");
            assertEquals("minecraft:worldgen/placed_feature",
                    GT6Worldgen.TWILIGHT_STONE_PLACED_KEYS.get(i).registry().toString(), tSnake + " registry");
            if (GT6Worldgen.LENS_STONE_SNAKES.contains(tSnake)) {
                assertEquals("gt6:twilight_stone_" + tSnake,
                        GT6Worldgen.twilightStoneConfiguredKey(tSnake).location().toString(),
                        tSnake + " the lens stone owns its twilight-path configured");
            } else {
                assertEquals("gt6:overworld_stone_" + tSnake,
                        GT6Worldgen.twilightStoneConfiguredKey(tSnake).location().toString(),
                        tSnake + " the blob stone reuses the overworld configured");
            }
        }
    }

    /**
     * The shared twilight stone-row columns (Loader_Worldgen.java:657 verbatim — the
     * ERRATUM numbers: the task-card face carried "size 200 / probability 100", the
     * overworld row's :655 numbers transposed in the second-hand transcription;
     * WorldgenBlob.java:48 ctor order + the :657 source text bind amount=1, size=100,
     * probability=200, Y0-40 — the 按上游原文落 ruling, 2026-10-07).
     */
    @Test
    public void twilightStoneColumnsArePinned() {
        assertEquals(200, GT6Worldgen.TWILIGHT_STONE_PROBABILITY,
                "Probability=200 -> the 1/200 chunk gate (NOT the transposed 100)");
        assertEquals(0, GT6Worldgen.TWILIGHT_STONE_MIN_Y, "MinHeight 0");
        assertEquals(40, GT6Worldgen.TWILIGHT_STONE_MAX_Y, "MaxHeight 40");
    }

    /**
     * The 17 placed JSONs ship the upstream columns: the 4-step chain = rarity 1/200 +
     * square + the uniform Y 0..40 band + biome — NO count modifier (Amount=1 IS the
     * default count, the band convention). The 12 blob stones hang their placed variant
     * off the REUSED overworld configured; the 5 lens stones off their own blob
     * configured (Feature.ORE, the single stone_ore_replaceables target on the STONE
     * block — byte-shape-identical to the 12-blob band, the upstream twilight size 100
     * clamping to the same codec-cap 64 the overworld 200 clamps to).
     */
    @Test
    public void twilightStoneFeatureJsonsShipTheUpstreamColumns() throws Exception {
        for (String tSnake : TWILIGHT_STONE_SNAKES) {
            JsonObject tPlaced = resourceJson("data/gt6/worldgen/placed_feature/twilight_stone_" + tSnake + ".json");
            assertEquals("gt6:" + GT6Worldgen.twilightStoneConfiguredKey(tSnake).location().getPath(),
                    tPlaced.get("feature").getAsString(), tSnake + " the reuse-or-own configured face");
            JsonArray tPlacement = tPlaced.getAsJsonArray("placement");
            assertEquals(4, tPlacement.size(), tSnake + " rarity + square + height + biome (NO count: Amount=1 default)");
            JsonObject tRarity = tPlacement.get(0).getAsJsonObject();
            assertEquals("minecraft:rarity_filter", tRarity.get("type").getAsString(), tSnake);
            assertEquals(200, tRarity.get("chance").getAsInt(), tSnake + " the 1/200 chunk gate (the erratum number)");
            assertEquals("minecraft:in_square", tPlacement.get(1).getAsJsonObject().get("type").getAsString(), tSnake);
            assertEquals("minecraft:biome", tPlacement.get(2).getAsJsonObject().get("type").getAsString(),
                    tSnake + " (the overworld stone-blob band chain shape: rarity + square + biome + height)");
            JsonObject tHeight = tPlacement.get(3).getAsJsonObject().getAsJsonObject("height");
            assertEquals("minecraft:height_range", tPlacement.get(3).getAsJsonObject().get("type").getAsString(), tSnake);
            assertEquals("minecraft:uniform", tHeight.get("type").getAsString(), tSnake);
            assertEquals(0, tHeight.getAsJsonObject("min_inclusive").get("absolute").getAsInt(), tSnake + " MinHeight 0");
            assertEquals(40, tHeight.getAsJsonObject("max_inclusive").get("absolute").getAsInt(), tSnake + " MaxHeight 40");
        }
        // the 5 lens configured JSONs — the head-loop blob shape at the twilight stone
        for (String tSnake : GT6Worldgen.LENS_STONE_SNAKES) {
            JsonObject tConfigured = resourceJson("data/gt6/worldgen/configured_feature/twilight_stone_" + tSnake + ".json");
            assertEquals("minecraft:ore", tConfigured.get("type").getAsString(), tSnake);
            JsonObject tConfig = tConfigured.getAsJsonObject("config");
            assertEquals(64, tConfig.get("size").getAsInt(),
                    tSnake + " size=64 (the upstream 100 under the 64 codec cap — the :56 clamp face)");
            assertEquals(0.0, tConfig.get("discard_chance_on_air_exposure").getAsDouble(), tSnake + " no air discard");
            JsonArray tTargets = tConfig.getAsJsonArray("targets");
            assertEquals(1, tTargets.size(), tSnake + " the single stone host");
            JsonObject tTarget = tTargets.get(0).getAsJsonObject();
            assertEquals("minecraft:tag_match", tTarget.getAsJsonObject("target").get("predicate_type").getAsString(), tSnake);
            assertEquals("minecraft:stone_ore_replaceables", tTarget.getAsJsonObject("target").get("tag").getAsString(),
                    tSnake + " the stone_ore_replaceables host");
            assertEquals("gt6:" + tSnake, tTarget.getAsJsonObject("state").get("Name").getAsString(),
                    tSnake + " the STONE-variant block (GTStoneBlocks.path: the plain snake)");
        }
    }

    /**
     * The SECOND mod-dimension modifier row (the twilight_ores shape): the 17 stone
     * placed variants over TF's own tag at the ore step, in EACH leg's brand — the SAME
     * TF-absence semantics (exactly ONE positive mod_loaded twilightforest; the
     * conditions ride the emission registry, CONDITION_ROWS row 3).
     */
    @Test
    public void twilightStonesModifierShipsTheTfGateInBothBrands() throws Exception {
        List<String> tExpected = TWILIGHT_STONE_SNAKES.stream().map(tSnake -> "gt6:twilight_stone_" + tSnake).toList();
        assertEquals("gt6:twilight_stones", GT6WorldgenDatagen.TWILIGHT_STONES_MODIFIER_KEY.location().toString(),
                "the twilight_stones modifier id");
        assertEquals(GT6WorldgenDatagen.biomeModifierRegistryKey().location(),
                GT6WorldgenDatagen.TWILIGHT_STONES_MODIFIER_KEY.registry(),
                "the modifier lives in the leg's biome_modifier registry");
        for (String tBrand : new String[] {"forge", "neoforge"}) {
            JsonObject tRow = resourceJson("data/gt6/" + tBrand + "/biome_modifier/twilight_stones.json");
            assertEquals(tBrand + ":add_features", tRow.get("type").getAsString(), tBrand + " type brand");
            assertEquals("#twilightforest:in_twilight_forest", tRow.get("biomes").getAsString(),
                    tBrand + " the TF tag gate (the ④ face: never resolved when TF is absent)");
            JsonArray tFeatures = tRow.getAsJsonArray("features");
            assertNotNull(tFeatures, tBrand + " the 17-row feature list");
            assertEquals(17, tFeatures.size(), tBrand + " the 17 placed variants, ALL mounted by the ONE row");
            assertEquals(tExpected, tFeatures.asList().stream().map(JsonElement::getAsString).toList(),
                    tBrand + " STONES order");
            assertEquals("underground_ores", tRow.get("step").getAsString(), tBrand + " the ore step (the blob rows' pass)");
            JsonArray tConditions = tRow.getAsJsonArray(tBrand + ":conditions");
            assertNotNull(tConditions, tBrand + " the conditions key");
            assertEquals(1, tConditions.size(), tBrand + " exactly the positive mod_loaded");
            JsonObject tModLoaded = tConditions.get(0).getAsJsonObject();
            assertEquals(tBrand + ":mod_loaded", tModLoaded.get("type").getAsString(), tBrand);
            assertEquals("twilightforest", tModLoaded.get("modid").getAsString(),
                    tBrand + " THE TF-ABSENCE PIN: without TF the entry never registers");
        }
    }

    /**
     * The netherite pair ships the deadrock TAG host (the red-line indirection — the
     * foreign id never appears here) over the vanilla ancient-debris state, count=8
     * (the Amount column), NO rarity filter (Probability=1), the 8..80 band.
     */
    @Test
    public void twilightNetheriteFeatureJsonShipsTheDeadrockTagHost() throws Exception {
        JsonObject tConfigured = resourceJson("data/gt6/worldgen/configured_feature/twilight_ore/netherite.json");
        assertEquals("minecraft:ore", tConfigured.get("type").getAsString());
        JsonObject tConfig = tConfigured.getAsJsonObject("config");
        assertEquals(8, tConfig.get("size").getAsInt(), "size=8 (:712)");
        JsonArray tTargets = tConfig.getAsJsonArray("targets");
        assertEquals(1, tTargets.size());
        JsonObject tTarget = tTargets.get(0).getAsJsonObject();
        assertEquals("minecraft:tag_match", tTarget.getAsJsonObject("target").get("predicate_type").getAsString(),
                "the TagMatchTest host");
        assertEquals("gt6:tf_deadrock", tTarget.getAsJsonObject("target").get("tag").getAsString(),
                "OUR tag — the twilightforest id lives only in the tag file");
        assertEquals("minecraft:ancient_debris", tTarget.getAsJsonObject("state").get("Name").getAsString(),
                "the vanilla ancient-debris state");

        JsonObject tPlaced = resourceJson("data/gt6/worldgen/placed_feature/twilight_ore/netherite.json");
        assertEquals("gt6:twilight_ore/netherite", tPlaced.get("feature").getAsString());
        JsonArray tPlacement = tPlaced.getAsJsonArray("placement");
        assertEquals(4, tPlacement.size(), "count + square + height + biome (Probability=1 = no rarity filter)");
        assertEquals("minecraft:count", tPlacement.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals(8, tPlacement.get(0).getAsJsonObject().get("count").getAsInt(),
                "Amount=8 blob attempts per chunk");
        assertEquals("minecraft:in_square", tPlacement.get(1).getAsJsonObject().get("type").getAsString());
        JsonObject tHeight = tPlacement.get(2).getAsJsonObject().getAsJsonObject("height");
        assertEquals("minecraft:uniform", tHeight.get("type").getAsString());
        assertEquals(8, tHeight.getAsJsonObject("min_inclusive").get("absolute").getAsInt(), "MinHeight 8");
        assertEquals(80, tHeight.getAsJsonObject("max_inclusive").get("absolute").getAsInt(), "MaxHeight 80");
        assertEquals("minecraft:biome", tPlacement.get(3).getAsJsonObject().get("type").getAsString());
    }

    /**
     * THE TF-ABSENCE TAG PIN (the deadrock red-line): {@code gt6:tf_deadrock} ships in
     * BOTH leg dialects (1.20.1 {@code tags/blocks}, 1.21.1 {@code tags/block}) with the
     * single {@code required:false} entry — TF absent = the tag resolves empty = the
     * netherite feature targets nothing (and the modifier is unmounted by its mod_loaded
     * condition anyway): the row neither crashes nor hangs.
     */
    @Test
    public void twilightDeadrockTagShipsRequiredFalseEntries() throws Exception {
        for (String tDir : new String[] {"tags/blocks", "tags/block"}) {
            JsonObject tTag = resourceJson("data/gt6/" + tDir + "/tf_deadrock.json");
            // "replace" is carried IMPLICITLY false (TagsProvider.java:96-97 new TagFile(entries, false)
            // omits the key — the mineable/pickaxe.json precedent)
            assertTrue(!tTag.has("replace") || !tTag.get("replace").getAsBoolean(), tDir + " join semantics");
            JsonArray tValues = tTag.getAsJsonArray("values");
            assertEquals(1, tValues.size(), tDir + " the meta-2 host only (deadrock; NOT weathered/cracked — the 1:1 meta split)");
            JsonObject tEntry = tValues.get(0).getAsJsonObject();
            assertEquals("twilightforest:deadrock", tEntry.get("id").getAsString(), tDir);
            assertEquals(false, tEntry.get("required").getAsBoolean(),
                    tDir + " required:false — TF absent = empty tag = no crash, no hang");
        }
    }
}
