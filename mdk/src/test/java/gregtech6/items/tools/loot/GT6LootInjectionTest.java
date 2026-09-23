/**
 * Offline pins for task p34-loot-injection — the dungeon-loot injection face
 * ({@code loaders/c/Loader_Loot.java} tail rows + the three bag weight tables).
 *
 * <p>Three layers, mirroring the card's double-insurance split:
 * <ol>
 * <li>the ROW tables ({@code GT6LootInjectionDatagen}) replayed headless through the
 *     registration-free material system — the weight/stack anchors pinned verbatim against
 *     the upstream {@code addLoot} rows, plus the declared POOL scan (bag/bottle/coin/... items
 *     must produce zero entries — the {@code addLoot :566-569} invalid-skip face);</li>
 * <li>the generated GLM JSONs pinned against the committed tree (the target table ids + the
 *     entry weights/stack ranges — ACCEPTANCE ①'s "逐参对 Loader_Loot 原锚");</li>
 * <li>the generated bag weight tables + the twin modifier index (the
 *     {@code GT6ToolLootModifiersDatagen} call-order contract, 19 entries).</li>
 * </ol>
 * The live face is the RCON p34_loot_inject chain (the /loot insert rolls through the Forge
 * {@code ForgeHooks.modifyLoot} patch); this file is the static half of the double insurance.
 */
package gregtech6.items.tools.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.datagen.GT6LootInjectionDatagen;
import gregtech6.registry.GTMaterialItems;

public class GT6LootInjectionTest {

    @BeforeAll
    public static void initMaterialSystem() {
        // the headless boot (the GT6WireLootLaserTest form) — the row tables are
        // registration-free faces over the material system
        GTMaterialItems.initMaterials();
    }

    private static String raw(String aPath) throws IOException {
        try (InputStream tStream = GT6LootInjectionTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated resource must be on the classpath: " + aPath);
            return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static JsonObject tree(String aPath) throws IOException {
        return JsonParser.parseString(raw(aPath)).getAsJsonObject();
    }

    private static Map<String, int[]> byItem(List<GT6LootInjectionDatagen.EntryRow> aRows) {
        return aRows.stream().collect(Collectors.toMap(GT6LootInjectionDatagen.EntryRow::item,
                aRow -> new int[] {aRow.weight(), aRow.min(), aRow.max()}, (a, b) -> a));
    }

    // ------------------------------------------------- 1: the structure injections (row face)

    /**
     * The verified category → table-id mapping (ACCEPTANCE ④, both jars listed) — the seven
     * landed rows; BONUS_CHEST/STRONGHOLD_LIBRARY/STRONGHOLD_CROSSING keep zero rows (the
     * declared pool) and get no row at all.
     */
    @Test
    public void injectionTargetsAreTheVerifiedVanillaTableIds() {
        List<GT6LootInjectionDatagen.InjectionRow> tRows = GT6LootInjectionDatagen.injections();
        assertEquals(7, tRows.size(), "seven landed structure injections (the pool categories fall out)");
        assertEquals(List.of(
                "dungeon_inject_simple_dungeon",
                "dungeon_inject_desert_pyramid",
                "dungeon_inject_jungle_temple",
                "dungeon_inject_jungle_temple_dispenser",
                "dungeon_inject_abandoned_mineshaft",
                "dungeon_inject_village_weaponsmith",
                "dungeon_inject_stronghold_corridor"),
                tRows.stream().map(GT6LootInjectionDatagen.InjectionRow::name).collect(Collectors.toList()),
                "the upstream Loader_Loot category order");
        assertEquals("minecraft:chests/simple_dungeon", tRows.get(0).table());
        assertEquals("minecraft:chests/desert_pyramid", tRows.get(1).table());
        assertEquals("minecraft:chests/jungle_temple", tRows.get(2).table(), "the 1.7.10 jungle chest table");
        assertEquals("minecraft:chests/jungle_temple_dispenser", tRows.get(3).table());
        assertEquals("minecraft:chests/abandoned_mineshaft", tRows.get(4).table());
        assertEquals("minecraft:chests/village/village_weaponsmith", tRows.get(5).table(),
                "the 1.7.10 blacksmith chest IS the modern weaponsmith shop");
        assertEquals("minecraft:chests/stronghold_corridor", tRows.get(6).table());
        for (GT6LootInjectionDatagen.InjectionRow tRow : tRows) {
            assertEquals(1, tRow.rollMin(), "the declared share-approximation roll floor");
            assertEquals(3, tRow.rollMax(), "the declared share-approximation roll ceiling");
            assertTrue(tRow.entries().size() > 0, tRow.name() + " carries rows");
        }
    }

    /** The dungeon metal ladder (:418-433) + the task-p36 ZPM artifact row: 17 entries. */
    @Test
    public void simpleDungeonCarriesTheMetalLadderVerbatim() {
        Map<String, int[]> tMap = byItem(GT6LootInjectionDatagen.injections().get(0).entries());
        assertEquals(17, tMap.size(), "the four 4-metal ladders (ingot/plate/stick/toolHeadArrow) + the p36 ZPM artifact row");
        for (String tMetal : new String[] {"steel", "bronze", "brass"}) {
            assertEquals(12, tMap.get("gt6:ingot_" + tMetal)[0], ":418-420 weight");
            assertEquals(12, tMap.get("gt6:plate_" + tMetal)[0], ":422-424 weight");
            assertEquals(12, tMap.get("gt6:stick_" + tMetal)[0], ":426-428 weight");
            assertTrue(tMap.get("gt6:stick_" + tMetal)[1] == 2 && tMap.get("gt6:stick_" + tMetal)[2] == 12,
                    ":426-428 stack [2,12]");
            assertEquals(12, tMap.get("gt6:tool_head_arrow_" + tMetal)[0], ":430-432 weight");
            assertTrue(tMap.get("gt6:tool_head_arrow_" + tMetal)[1] == 4
                    && tMap.get("gt6:tool_head_arrow_" + tMetal)[2] == 24, ":430-432 stack [4,24]");
        }
        assertEquals(2, tMap.get("gt6:ingot_damascus_steel")[0], ":421 the damascus weight-2 arm");
        // task p36 — the ZPM artifact row: the full-NBT lane (the DungeonData.zpm active face)
        GT6LootInjectionDatagen.EntryRow tZpm = GT6LootInjectionDatagen.injections().get(0).entries()
                .stream().filter(aRow -> aRow.item().equals("gt6:zpm")).findFirst().orElseThrow();
        assertEquals(2, tZpm.weight(), "the rare-roll posture");
        assertTrue(tZpm.min() == 1 && tZpm.max() == 1, "the [1,1] artifact stack");
        org.junit.jupiter.api.Assertions.assertNotNull(tZpm.tag(), "the full-NBT lane rides the row");
        assertTrue(tZpm.tag().getBoolean(gregtech6.item.energy.GT6BatteryItem.NBT_ACTIVE_ENERGY),
                "gt.active.energy = the store-as-full key (the 2/3 dungeon dice collapsed to always-full)");
    }

    /** The mineshaft rows (:468-482): the seven vanilla ore blocks + the six dig heads. */
    @Test
    public void mineshaftCarriesTheOreBlocksAndDigHeads() {
        Map<String, int[]> tMap = byItem(GT6LootInjectionDatagen.injections().get(4).entries());
        assertEquals(13, tMap.size());
        assertEquals(4, tMap.get("minecraft:coal_ore")[0], ":470");
        assertEquals(1, tMap.get("minecraft:diamond_ore")[0], ":475");
        assertTrue(tMap.get("minecraft:diamond_ore")[1] == 4 && tMap.get("minecraft:diamond_ore")[2] == 16,
                ":475 stack [4,16]");
        assertEquals(5, tMap.get("gt6:tool_head_shovel_arsenic_bronze")[0], ":477");
        assertEquals(3, tMap.get("gt6:tool_head_raw_pickaxe_steel")[0], ":481");
    }

    /** The village/corridor/pyramid/jungle/dispenser anchor rows. */
    @Test
    public void theRemainingTablesCarryTheirAnchors() {
        Map<String, int[]> tVillage = byItem(GT6LootInjectionDatagen.injections().get(5).entries());
        assertEquals(16, tVillage.size(), ":491-506 the smith ladder (the small-gear bronze/brass arms "
                + "fall to the registration universe — the gearGtSmall condition gates on the big-gear "
                + "prefix, the modern universe has no gear_gt for them; the declared skip)");
        assertEquals(2, tVillage.get("gt6:gear_gt_small_steel")[0], ":494");
        assertEquals(1, tVillage.get("gt6:ingot_damascus_steel")[0], ":506");
        Map<String, int[]> tCorridor = byItem(GT6LootInjectionDatagen.injections().get(6).entries());
        assertEquals(6, tCorridor.size(), ":546-551");
        assertEquals(6, tCorridor.get("gt6:arrow_gt_wood_sterling_silver")[0], ":551");
        Map<String, int[]> tDesert = byItem(GT6LootInjectionDatagen.injections().get(1).entries());
        assertEquals(1, tDesert.get("gt6:tool_head_arrow_naquadah")[0], ":446 the Nq arrow head");
        Map<String, int[]> tJungle = byItem(GT6LootInjectionDatagen.injections().get(2).entries());
        assertEquals(3, tJungle.get("gt6:ingot_arsenic_copper")[0], ":452");
        assertEquals(30, tMap2(GT6LootInjectionDatagen.injections().get(3).entries(), "minecraft:fire_charge"),
                ":463 the dispenser fire charges");
    }

    private static int tMap2(List<GT6LootInjectionDatagen.EntryRow> aRows, String aItem) {
        for (GT6LootInjectionDatagen.EntryRow tRow : aRows) if (tRow.item().equals(aItem)) return tRow.weight();
        return -1;
    }

    // ------------------------------------------------- 2: the declared pool (zero appearances)

    /** The unported item families must produce NO entry (the upstream addLoot invalid-skip face). */
    @Test
    public void theDeclaredPoolProducesZeroEntries() {
        List<String> tPooled = List.of("bag", "bottle", "coin", "food_can", "book_loot", "paper_magic",
                "dynamite", "matchbox", "lighter", "porcelain", "pill", "crate");
        for (GT6LootInjectionDatagen.InjectionRow tRow : GT6LootInjectionDatagen.injections()) {
            for (GT6LootInjectionDatagen.EntryRow tEntry : tRow.entries()) {
                for (String tFrag : tPooled) {
                    assertTrue(!tEntry.item().contains(tFrag),
                            tRow.name() + " pooled item leaked: " + tEntry.item());
                }
            }
        }
        for (GT6LootInjectionDatagen.WeightRow tRow : GT6LootInjectionDatagen.weightTables()) {
            for (GT6LootInjectionDatagen.EntryRow tEntry : tRow.entries()) {
                for (String tFrag : tPooled) {
                    assertTrue(!tEntry.item().contains(tFrag),
                            tRow.name() + " pooled item leaked: " + tEntry.item());
                }
            }
        }
    }

    // ------------------------------------------------- 3: the bag weight tables (row face)

    /** gt.flawless (:84-103): every row [1,1], the flagship weights pinned verbatim. */
    @Test
    public void flawlessTableCarriesTheUpstreamWeights() {
        Map<String, int[]> tMap = byItem(GT6LootInjectionDatagen.weightTables().get(0).entries());
        assertEquals(20, tMap.size(), ":84-103 the twenty gem rows");
        assertEquals(2160, tMap.get("gt6:gem_flawless_diamond")[0], ":84");
        assertEquals(1152, tMap.get("gt6:gem_flawless_emerald")[0], ":86");
        assertEquals(720, tMap.get("gt6:gem_flawless_ruby")[0], ":93");
        assertEquals(432, tMap.get("gt6:gem_flawless_redstone")[0], ":103");
        for (int[] tSpec : tMap.values()) {
            assertEquals(1, tSpec[1], "every flawless row is [1,1] upstream");
            assertEquals(1, tSpec[2]);
        }
    }

    /** gt.gems (:109-126): the fixed families + the RANDOM_SMALL_GEM_ORE loop in tier triplets. */
    @Test
    public void gemsTableCarriesTheFamiliesAndTheLoop() {
        List<GT6LootInjectionDatagen.EntryRow> tRows = GT6LootInjectionDatagen.weightTables().get(1).entries();
        Map<String, int[]> tMap = byItem(tRows);
        assertEquals(9216, tMap.get("gt6:gem_emerald")[0], ":109 the top weight");
        assertTrue(tMap.get("gt6:gem_emerald")[1] == 1 && tMap.get("gt6:gem_emerald")[2] == 4, ":109 [1,4]");
        assertEquals(2160, tMap.get("gt6:gem_chipped_diamond")[0], ":112");
        assertTrue(tMap.get("gt6:gem_chipped_diamond")[1] == 4 && tMap.get("gt6:gem_chipped_diamond")[2] == 16,
                ":112 [4,16]");
        int tFixed = 13; // :109-121 (emerald + diamond family + pink/craponite/amber triplets)
        assertTrue(tRows.size() > tFixed, "the loop rows landed");
        // the loop is per-prefix gated (the gemFlawed red_fluorite lesson: a material can carry
        // the gem item without the flawed tier), so tiers may be partial — the anchor is the
        // weight: every loop row rides 144 (upstream :123-125)
        for (GT6LootInjectionDatagen.EntryRow tRow : tRows.subList(tFixed, tRows.size())) {
            assertEquals(144, tRow.weight(), "the :122-126 loop weight");
        }
    }

    /** gt.misc (:132-177): the vanilla rows, the billet ladder and the blaze sticks. */
    @Test
    public void miscTableCarriesTheVanillaAndBilletRows() {
        Map<String, int[]> tMap = byItem(GT6LootInjectionDatagen.weightTables().get(2).entries());
        assertEquals(144, tMap.get("minecraft:name_tag")[0], ":132");
        assertEquals(13, tMap.get("minecraft:music_disc_13")[0], ":135 the disc weight");
        assertEquals(144, tMap.get("gt6:billet_neodymium")[0], ":147 (MT.Nd = Neodymium)");
        assertEquals(144, tMap.get("gt6:billet_lead")[0], ":158");
        assertEquals(72, tMap.get("gt6:ore_raw_meteoric_iron")[0], ":163");
        assertEquals(36, tMap.get("gt6:stick_blizz")[0], ":164");
        assertEquals(144, tMap.get("gt6:rock_gt_sky_stone")[0],
                ":160 the SkyStone rock row — the resolver never skips it");
    }

    /** The declared roll ranges (the semantic-shift constants). */
    @Test
    public void theRollRangesAreTheDeclaredConstants() {
        assertEquals(1, GT6LootInjectionDatagen.ROLL_MIN);
        assertEquals(3, GT6LootInjectionDatagen.ROLL_MAX);
        assertEquals(8, GT6LootInjectionDatagen.BAG_ROLL_MIN, "the upstream setMin(8)");
        assertEquals(24, GT6LootInjectionDatagen.BAG_ROLL_MAX, "the upstream setMax(24)");
    }

    // ------------------------------------------------- 4: the generated GLM JSONs (ACCEPTANCE ①)

    /** The generated modifier JSON: the target table id + the entry anchors, per-param. */
    @Test
    public void generatedDungeonModifierJsonPinsTheAnchors() throws IOException {
        JsonObject tJson = tree("data/gt6/loot_modifiers/dungeon_inject_simple_dungeon.json");
        assertEquals("gt6:gt6_dungeon_inject", tJson.get("type").getAsString(), "the serializer row id");
        assertEquals("minecraft:chests/simple_dungeon", tJson.get("table").getAsString(), "the in-codec target");
        JsonArray tEntries = tJson.getAsJsonArray("entries");
        assertEquals(17, tEntries.size(), "the metal ladder + the p36 ZPM artifact row");
        JsonObject tFirst = tEntries.get(0).getAsJsonObject();
        assertEquals("gt6:ingot_steel", tFirst.get("item").getAsString());
        assertEquals(12, tFirst.get("weight").getAsInt(), ":418 weight");
        assertEquals(1, tFirst.get("min").getAsInt());
        assertEquals(6, tFirst.get("max").getAsInt());
        // task p36 — the artifact tail entry carries the full-NBT lane
        JsonObject tZpm = tEntries.get(16).getAsJsonObject();
        assertEquals("gt6:zpm", tZpm.get("item").getAsString(), "the artifact row rides last");
        // the SNBT 1b byte round-trips through the JSON as the number 1 (gson int form)
        assertEquals(1, tZpm.get("tag").getAsJsonObject().get("gt.active.energy").getAsInt(),
            "gt.active.energy = the store-as-full key (the DungeonData.zpm active face)");
        JsonObject tRolls = tJson.getAsJsonObject("rolls");
        assertNotNull(tRolls, "the uniform roll range rides the codec");
        assertEquals(1, tRolls.get("min_inclusive").getAsInt(), "the declared [1,3] floor");
        assertEquals(3, tRolls.get("max_inclusive").getAsInt(), "the declared [1,3] ceiling");
    }

    /** Every landed injection has a generated modifier JSON with its table id. */
    @Test
    public void everyLandedInjectionHasItsModifierJson() throws IOException {
        for (GT6LootInjectionDatagen.InjectionRow tRow : GT6LootInjectionDatagen.injections()) {
            JsonObject tJson = tree("data/gt6/loot_modifiers/" + tRow.name() + ".json");
            assertEquals("gt6:gt6_dungeon_inject", tJson.get("type").getAsString());
            assertEquals(tRow.table(), tJson.get("table").getAsString());
            assertEquals(tRow.entries().size(), tJson.getAsJsonArray("entries").size());
        }
    }

    /** The pooled categories stay OUT of the modifier set (no JSON, no index entry). */
    @Test
    public void thePooledCategoriesHaveNoModifierJson() {
        for (String tPooled : new String[] {"bonus_chest", "stronghold_library", "stronghold_crossing"}) {
            try (InputStream tStream = GT6LootInjectionTest.class.getClassLoader()
                    .getResourceAsStream("data/gt6/loot_modifiers/dungeon_inject_" + tPooled + ".json")) {
                assertEquals(null, tStream, "the pooled category must not ship a modifier JSON: " + tPooled);
            } catch (IOException aE) {
                throw new RuntimeException(aE);
            }
        }
    }

    // ------------------------------------------------- 5: the generated bag tables + the index

    /** The gt_flawless generated table: the CHEST param set, the 8..24 pool, the weighted entries. */
    @Test
    public void generatedFlawlessTableIsTheChestPoolShape() throws IOException {
        JsonObject tJson = tree("data/gt6/loot_tables/chests/gt_flawless.json");
        assertEquals("minecraft:chest", tJson.get("type").getAsString(), "the CHEST param set");
        JsonArray tPools = tJson.getAsJsonArray("pools");
        assertEquals(1, tPools.size(), "one pool — the bag-open roll count");
        JsonObject tPool = tPools.get(0).getAsJsonObject();
        JsonObject tRolls = tPool.getAsJsonObject("rolls");
        assertEquals("minecraft:uniform", tRolls.get("type").getAsString());
        assertEquals(8.0, tRolls.get("min").getAsDouble(), "setMin(8)");
        assertEquals(24.0, tRolls.get("max").getAsDouble(), "setMax(24)");
        JsonArray tEntries = tPool.getAsJsonArray("entries");
        assertEquals(GT6LootInjectionDatagen.weightTables().get(0).entries().size(), tEntries.size());
        JsonObject tFirst = tEntries.get(0).getAsJsonObject();
        assertEquals("minecraft:item", tFirst.get("type").getAsString());
        assertEquals("gt6:gem_flawless_diamond", tFirst.get("name").getAsString(), ":84 order preserved");
        assertEquals(2160, tFirst.get("weight").getAsInt());
        JsonArray tFunctions = tFirst.getAsJsonArray("functions");
        assertEquals("minecraft:set_count", tFunctions.get(0).getAsJsonObject().get("function").getAsString());
        JsonObject tCount = tFunctions.get(0).getAsJsonObject().getAsJsonObject("count");
        assertEquals("minecraft:uniform", tCount.get("type").getAsString());
        assertEquals(1.0, tCount.get("min").getAsDouble(), "the flawless [1,1] stack floor");
        assertEquals(1.0, tCount.get("max").getAsDouble(), "the flawless [1,1] stack ceiling");
    }

    /** The twin modifier index: the 19 entries in the MODIFIER_NAMES call order. */
    @Test
    public void theTwinIndexCarriesAllNineteenEntriesInCallOrder() throws IOException {
        JsonArray tTwin = tree("data/neoforge/loot_modifiers/global_loot_modifiers.json").getAsJsonArray("entries");
        assertEquals(19, tTwin.size(), "the 12 tool rows + the 7 dungeon injections");
        assertEquals("gt6:dungeon_inject_simple_dungeon", tTwin.get(12).getAsString(), "the tail-append order");
        assertEquals("gt6:dungeon_inject_stronghold_corridor", tTwin.get(18).getAsString());
        JsonArray tForge = tree("data/forge/loot_modifiers/global_loot_modifiers.json").getAsJsonArray("entries");
        assertEquals(tTwin.size(), tForge.size(), "same entry set on both indices");
    }
}
