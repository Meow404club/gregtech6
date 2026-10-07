package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;

import gregtech6.datagen.GT6BiomeModifierConditions;
import gregtech6.datagen.GT6WorldgenDatagen;

/**
 * The atum band pins (task atum-dim-adaptation — the ONE mod dimension with a 1.20.1
 * carrier): the :789-792 oil-spring mask (1/200 · 2000mB), the :886-916 ORE_ATUM large-
 * vein block (31 rows sharing the ORE_OVERWORLD list), the 35-row GEN_ATUM small-ore
 * projection (:800-829 + :832-835 + :848; the :854-870 mod-gated rows stay out — the
 * compat-pool ruling), the stone/rocks/coconut/hive band keys, the constants (the 11
 * biome ids, the base-stone pair, the synthetic salt), and the six biome-modifier JSONs
 * in BOTH leg brands with the mod_loaded atum condition (the GT6TwilightWorldgenTest
 * posture, JSON reads off the CLASSPATH). KJS face: everything here is datapack-domain;
 * the Feature/codec registration is the registry face.
 *
 * <p>THE JAR FACTS the card pinned (the atum2 master harvest — 1.20.1's 2.2.x line
 * predates none of them): dimension_type/atum.json {@code has_ceiling: false} (the
 * spring routing's ceiling arm can never misroute atum), AtumBiomes.java:10-20 = the 11
 * biome ids, {@code #atum:base_stone_atum} = limestone + karst (AtumAPI.java:46), and
 * Atum's own {@code #forge:is_atum} fill covers only 2 of the 11 — hence the OUR-tag
 * {@code #gt6:atum_biomes} mount.
 */
public class GT6AtumWorldgenTest {

    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() {
        // the ResourceKey/TagKey static-init face needs the vanilla bootstrap + the material
        // system (the forge-test-hygiene discipline, the GT6TwilightWorldgenTest posture)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // offline bootstrap quirks are expected; registries are ready by now.
        }
        gregtech6.registry.GTMaterialItems.initMaterials();
    }

    // ---------------------------------------------------------------- the spring band

    /** The :789-792 oil rows verbatim, the atum mask on, everything else census-dark. */
    @Test
    public void springAtumRowsArePinned() {
        List<String> tNames = List.of("atum.fluid.oil.extraheavy", "atum.fluid.oil.heavy",
                "atum.fluid.oil.medium", "atum.fluid.oil.light");
        List<String> tBlocks = List.of("gt6:liquid_extra_heavy_oil_block", "gt6:liquid_heavy_oil_block",
                "gt6:liquid_medium_oil_block", "gt6:liquid_light_oil_block");
        for (int i = 0; i < tNames.size(); i++) {
            GTFluidSpringConfig tRow = GT6WorldgenDatagen.FLUID_SPRING_TABLE.get(7 + i);
            assertEquals(tNames.get(i), tRow.name(), "row " + i + " name, :789-792 order");
            assertEquals(tBlocks.get(i), tRow.blockId(), "row " + i + " block id");
            assertEquals(200, tRow.probability(), "1/200 — half the OW band's 1/400");
            assertEquals(2000, tRow.springFluid(), "2000mB — half the OW band's 6000");
            assertTrue(tRow.atum(), "the atum mask lights the row");
            assertFalse(tRow.overworld(), "never drawn overworld");
            assertFalse(tRow.nether(), "never drawn nether");
        }
        assertEquals(16, GT6WorldgenDatagen.FLUID_SPRING_TABLE.size(), "the 16-row table size holds");
        for (int i = 0; i < GT6WorldgenDatagen.FLUID_SPRING_TABLE.size(); i++) {
            if (i >= 7 && i <= 10) continue;
            assertFalse(GT6WorldgenDatagen.FLUID_SPRING_TABLE.get(i).atum(),
                    "every non-atum row keeps the mask dark: " + GT6WorldgenDatagen.FLUID_SPRING_TABLE.get(i).name());
        }
    }

    /** The three-state draw: each Dim rolls ONLY its own mask; the atum replay arm is ore-free. */
    @Test
    public void springDimRoutingIsThreeState() {
        GTFluidSpringConfig tOw = new GTFluidSpringConfig("ow", "minecraft:lava", 1, true, 100);
        GTFluidSpringConfig tAtum = new GTFluidSpringConfig("atum", "x", 1, false, false, true, 100);
        GTFluidSpringConfig tNether = new GTFluidSpringConfig("nether", "minecraft:lava", 1, false, true, 100);
        GTFluidSpringConfig.Table tTable = new GTFluidSpringConfig.Table(List.of(tOw, tAtum, tNether));
        for (int tSeed = 0; tSeed < 200; tSeed++) {
            assertSame(tOw, GT6FluidSpringGenerator.drawSpring(tTable, new Random(tSeed), GT6FluidSpringGenerator.Dim.OVERWORLD));
            assertSame(tAtum, GT6FluidSpringGenerator.drawSpring(tTable, new Random(tSeed), GT6FluidSpringGenerator.Dim.ATUM));
            assertSame(tNether, GT6FluidSpringGenerator.drawSpring(tTable, new Random(tSeed), GT6FluidSpringGenerator.Dim.NETHER));
        }
        // the ore-replay arm: atum carries NO GT bedrock-ore rows, the replay never refuses
        assertFalse(GT6FluidSpringGenerator.oreClaims(
                new gregtech6.worldgen.GTBedrockOreConfig.Table(GT6WorldgenDatagen.BEDROCK_ORE_TABLE), new Random(7),
                GT6FluidSpringGenerator.Dim.ATUM), "atum: no bedrock-ore counterpart, no refusal");
        // the legacy boolean faces survive (the twilight-branch delegation shape)
        assertSame(tOw, GT6FluidSpringGenerator.drawSpring(tTable, new Random(3), false));
        assertSame(tNether, GT6FluidSpringGenerator.drawSpring(tTable, new Random(3), true));
    }

    // ---------------------------------------------------------------- the vein band

    /** The :886-916 ORE_ATUM block: exactly 31 rows, name order verbatim, the flags per row kind. */
    @Test
    public void veinAtumRowsArePinned() {
        List<String> tNames = List.of(
                "ore.large.lignite", "ore.large.coal", "ore.large.apatite", "ore.large.lapis", "ore.large.bauxite",
                "ore.large.iodinesalt", "ore.large.rocksalt", "ore.large.asbestos", "ore.large.sapphire",
                "ore.large.sapphire2", "ore.large.garnet", "ore.large.pitchblende", "ore.large.monazite",
                "ore.large.diamond", "ore.large.galena", "ore.large.quartz", "ore.large.peridot", "ore.large.gold",
                "ore.large.platinum", "ore.large.molybdenum", "ore.large.cassiterite", "ore.large.tungstate",
                "ore.large.manganese", "ore.large.beryllium", "ore.large.beryllium2", "ore.large.titanium",
                "ore.large.nickel", "ore.large.redstone", "ore.large.tetrahedrite", "ore.large.iron", "ore.large.copper");
        List<GTVeinConfig> tAtum = GT6WorldgenDatagen.LARGE_VEIN_TABLE.stream()
                .filter(GTVeinConfig::atum).toList();
        assertEquals(tNames, tAtum.stream().map(GTVeinConfig::name).toList(), "the 31 atum rows, :886-916 order");
        // spot columns: the bookends + the ORE_END intersection
        GTVeinConfig tLignite = tAtum.get(0);
        assertEquals(List.of(50, 130, 160, 8, 32),
                List.of(tLignite.minY(), tLignite.maxY(), tLignite.weight(), tLignite.density(), tLignite.size()),
                "lignite :886 verbatim");
        assertTrue(tLignite.overworld() && !tLignite.end(), "lignite rides OW, not END");
        GTVeinConfig tCopper = tAtum.get(30);
        assertEquals(List.of(10, 30, 80, 4, 24),
                List.of(tCopper.minY(), tCopper.maxY(), tCopper.weight(), tCopper.density(), tCopper.size()),
                "copper :916 verbatim");
        for (GTVeinConfig tRow : tAtum) {
            assertTrue(tRow.overworld(), tRow.name() + " shares the ORE_OVERWORLD list");
        }
        for (String tName : List.of("ore.large.platinum", "ore.large.molybdenum", "ore.large.cassiterite")) {
            assertTrue(tAtum.stream().filter(tRow -> tRow.name().equals(tName)).allMatch(GTVeinConfig::end),
                    tName + " is the ORE_END intersection");
        }
        // the :917-925 rows stay dark
        for (GTVeinConfig tRow : GT6WorldgenDatagen.LARGE_VEIN_TABLE) {
            if (!tNames.contains(tRow.name())) {
                assertFalse(tRow.atum(), tRow.name() + " (:917-925) never listed ORE_ATUM");
            }
        }
        assertTrue(GT6WorldgenDatagen.LARGE_VEIN_TABLE.stream().filter(tRow -> tRow.name().equals("ore.large.naquadah"))
                .allMatch(tRow -> !tRow.atum() && tRow.end()), "naquadah stays END-only");
    }

    /** The atum draw arm: only atum rows, bit-identical replays, the OW/End arms untouched. */
    @Test
    public void veinAtumDrawIsThreeState() {
        List<GTVeinConfig> tTable = GT6WorldgenDatagen.LARGE_VEIN_TABLE;
        for (int tSeed = 0; tSeed < 300; tSeed++) {
            GTVeinConfig tVein = GT6VeinGenerator.drawVein(tTable, new Random(tSeed), false, true);
            assertNotNull(tVein, "the atum band always has a drawable row");
            assertTrue(tVein.atum(), "the atum arm draws only atum rows: " + tVein.name());
            assertSame(tVein, GT6VeinGenerator.drawVein(tTable, new Random(tSeed), false, true), "replay identical");
        }
    }

    // ---------------------------------------------------------------- the small-ore band

    /** The 35-row GEN_ATUM projection: exact tail set, the mod-gated rows stay out. */
    @Test
    public void smallOreAtumProjectionIsPinned() {
        List<String> tTails = List.of(
                "copper", "chalcopyrite", "malachite", "tin", "cassiterite", "zinc", "sphalerite", "smithsonite",
                "stibnite", "bismuth", "lead", "galena", "silver", "gold", "pyrite", "hematite", "pyrolusite",
                "garnierite", "pentlandite", "scheelite", "salt", "rocksalt", "borax", "asbestos", "diamond",
                "amber", "craponite", "redstone", "redcinnabar", "lapis", "coal", "graphite", "pollucite",
                "zeolite", "sulfur");
        List<String> tAtumTails = GTOreWorldgen.ROWS.stream()
                .filter(tRow -> tRow.dims().contains(GTOreWorldgen.Dim.ATUM))
                .map(GTOreWorldgen.SmallOreRow::tail).toList();
        assertEquals(tTails, tAtumTails, "the 35 in-table GEN_ATUM rows (:800-829 + :832-835 + :848)");
        // the compat pool stays out (blackquartz/certus/vinteum/chimerite/hexorium/infused/IHL rows)
        for (String tGated : List.of("blackquartz", "certus", "vinteum", "chimerite", "hexoriumred", "bischofite", "datolite")) {
            assertTrue(GTOreWorldgen.ROWS.stream().filter(tRow -> tRow.tail().equals(tGated))
                    .noneMatch(tRow -> tRow.dims().contains(GTOreWorldgen.Dim.ATUM)),
                    "the mod-gated row " + tGated + " carries no atum face (the compat-pool ruling)");
        }
        assertEquals(202, GTOreWorldgen.placementPairs().size(), "114 OW + 20 NETHER + 33 END + 35 ATUM");
        // the atum host face: the single #gt6:atum_base_stone arm onto the stone family
        gregapi.oredict.OreDictMaterial tCopperMaterial = GTOreWorldgen.resolve(GTOreWorldgen.ROWS.stream()
                .filter(tRow -> tRow.tail().equals("copper")).findFirst().orElseThrow());
        assertEquals(1, GTOreWorldgen.hostPaths(tCopperMaterial, GTOreWorldgen.Dim.ATUM).size(),
                "the atum host face is ONE target (24/1/1/1 acceptance counts)");
        assertEquals(List.of("ore_small_stone_copper"),
                GTOreWorldgen.hostPaths(tCopperMaterial, GTOreWorldgen.Dim.ATUM),
                "the atum small-ore host = the stone-family small ore (the port-level declaration)");
    }

    // ---------------------------------------------------------------- the constants

    /** The jar facts: 11 biomes, the base-stone pair, the modid, the synthetic salt, the rocks 3,3. */
    @Test
    public void atumConstantsArePinned() {
        assertEquals("atum", GT6Worldgen.ATUM_MODID);
        assertEquals("gt6:atum_biomes", GT6Worldgen.ATUM_BIOMES.location().toString());
        assertEquals(List.of("atum:dead_oasis", "atum:dense_woods", "atum:sparse_woods", "atum:dried_river",
                "atum:limestone_crags", "atum:limestone_mountains", "atum:oasis", "atum:sand_dunes",
                "atum:sand_hills", "atum:sand_plains", "atum:karst_caves"),
                GT6Worldgen.ATUM_BIOME_IDS.stream().map(ResourceLocation::toString).toList(),
                "AtumBiomes.java:10-20 verbatim (the 2.3.0 master harvest)");
        assertEquals("gt6:atum_base_stone", GT6Worldgen.ATUM_BASE_STONE.location().toString());
        assertEquals(List.of("atum:limestone", "atum:karst"),
                GT6Worldgen.ATUM_BASE_STONE_IDS.stream().map(ResourceLocation::toString).toList(),
                "the #atum:base_stone_atum fill verbatim (base_stone_atum.json)");
        assertEquals(4, GT6Worldgen.ATUM_DIMENSION_SALT, "the synthetic vein salt");
        assertNotEquals(GT6Worldgen.ATUM_DIMENSION_SALT, 0);
        assertNotEquals(GT6Worldgen.ATUM_DIMENSION_SALT, GT6VeinGenerator.NETHER_DIMENSION_SALT);
        assertNotEquals(GT6Worldgen.ATUM_DIMENSION_SALT, GT6VeinGenerator.END_DIMENSION_SALT);
        assertNotEquals(GT6Worldgen.ATUM_DIMENSION_SALT, GT6Worldgen.SPRING_DIMENSION_SALT);
        assertNotEquals(GT6Worldgen.ATUM_DIMENSION_SALT, GT6Worldgen.COLTAN_DIMENSION_SALT);
        assertEquals(3, GT6Worldgen.ATUM_SURFACE_ROCKS_AMOUNT, "atum.rocks amount 3, :625");
        assertEquals(3, GT6Worldgen.ATUM_SURFACE_ROCKS_PROBABILITY, "atum.rocks probability 3, :625");
        assertEquals("gt6:atum_surface_rocks", GT6Worldgen.ATUM_SURFACE_ROCKS_PLACED.location().toString());
    }

    // ---------------------------------------------------------------- the conditions registry

    /** Six atum tenants, all positive mod_loaded atum, both brand faces byte-shape-equal modulo brand. */
    @Test
    public void conditionsRegistryShipsTheAtumTenants() {
        List<String> tAtumPaths = List.of("atum_fluid_springs", "atum_large_veins", "atum_ores",
                "atum_stones", "atum_surface_rocks", "atum_bumble_hives");
        assertEquals(8, GT6BiomeModifierConditions.CONDITION_ROWS.size(),
                "the End yield + twilight_ores predecessors + the SIX atum rows");
        for (String tPath : tAtumPaths) {
            GT6BiomeModifierConditions.ConditionRow tRow = GT6BiomeModifierConditions.CONDITION_ROWS.stream()
                    .filter(tCandidate -> tCandidate.rowPath().equals(tPath)).findFirst().orElseThrow();
            for (String tBrand : List.of("forge", "neoforge")) {
                JsonArray tConditions = tRow.conditions().apply(tBrand);
                assertEquals(1, tConditions.size(), tPath + " = exactly one condition");
                JsonObject tCondition = tConditions.get(0).getAsJsonObject();
                assertEquals(tBrand + ":mod_loaded", tCondition.get("type").getAsString(),
                        tPath + " type-first member order");
                assertEquals("atum", tCondition.get("modid").getAsString(), tPath + " the atum trigger");
            }
        }
    }

    // ---------------------------------------------------------------- the generated JSONs

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6AtumWorldgenTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, aPath + " must ship on the classpath");
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** The six atum modifiers in EACH brand: the #gt6:atum_biomes gate + the mod_loaded atum conditions. */
    @Test
    public void atumModifiersShipTheGateAndConditionsInBothBrands() throws Exception {
        List<String> tRows = List.of("atum_fluid_springs", "atum_large_veins", "atum_ores",
                "atum_stones", "atum_surface_rocks", "atum_bumble_hives");
        for (String tBrand : List.of("forge", "neoforge")) {
            for (String tRow : tRows) {
                JsonObject tJson = json("data/gt6/" + tBrand + "/biome_modifier/" + tRow + ".json");
                assertEquals(tBrand + ":add_features", tJson.get("type").getAsString(), tRow + " add_features type");
                assertEquals("#gt6:atum_biomes", tJson.get("biomes").getAsString(), tRow + " the mount tag");
                assertTrue(tJson.get("features").getAsJsonArray().size() > 0, tRow + " carries features");
                JsonArray tConditions = tJson.getAsJsonArray(tBrand + ":conditions");
                assertEquals(1, tConditions.size(), tRow + " one condition");
                JsonObject tCondition = tConditions.get(0).getAsJsonObject();
                assertEquals(tBrand + ":mod_loaded", tCondition.get("type").getAsString(), tRow + " condition type");
                assertEquals("atum", tCondition.get("modid").getAsString(), tRow + " condition modid");
            }
            // the atum small-ore modifier carries exactly the 35 atum pairs
            JsonArray tOreFeatures = json("data/gt6/" + tBrand + "/biome_modifier/atum_ores.json").getAsJsonArray("features");
            assertEquals(35, tOreFeatures.size(), "the 35 ore_small_atum placed features");
            // the atum stone modifier = the 12 reused blob placed + the 5 atum lens placed
            JsonArray tStoneFeatures = json("data/gt6/" + tBrand + "/biome_modifier/atum_stones.json").getAsJsonArray("features");
            assertEquals(17, tStoneFeatures.size(), "12 reused blob placed + 5 atum lens placed");
        }
    }

    /** The atum tag files: 11 optional biomes, the coconut oasis member, the base-stone pair. */
    @Test
    public void atumTagJsonsArePinned() throws Exception {
        JsonArray tBiomes = json("data/gt6/tags/worldgen/biome/atum_biomes.json").getAsJsonArray("values");
        assertEquals(11, tBiomes.size(), "the 11 members, all required:false");
        for (JsonElement tEntry : tBiomes) {
            JsonObject tMember = tEntry.getAsJsonObject();
            assertTrue(tMember.get("id").getAsString().startsWith("atum:"), "foreign ids only: " + tMember);
            assertFalse(tMember.get("required").getAsBoolean(), "required:false — the addOptional form");
        }
        JsonArray tCoconut = json("data/gt6/tags/worldgen/biome/trees/coconut.json").getAsJsonArray("values");
        assertTrue(tCoconut.toString().contains("minecraft:beach"), "the vanilla member survives");
        boolean tOasis = false;
        for (JsonElement tEntry : tCoconut) {
            JsonObject tMember = tEntry.getAsJsonObject();
            if ("atum:oasis".equals(tMember.get("id").getAsString())) {
                tOasis = true;
                assertFalse(tMember.get("required").getAsBoolean(), "atum:oasis required:false");
            }
        }
        assertTrue(tOasis, "the coconut tag carries atum:oasis — the only BIOMES_COCONUT hit");
        JsonObject tBase = json("data/gt6/tags/blocks/atum_base_stone.json");
        JsonArray tValues = tBase.getAsJsonArray("values");
        assertEquals(2, tValues.size(), "limestone + karst");
        for (JsonElement tEntry : tValues) {
            JsonObject tMember = tEntry.getAsJsonObject();
            assertTrue(tMember.get("id").getAsString().startsWith("atum:"), "the #atum:base_stone_atum fill");
            assertFalse(tMember.get("required").getAsBoolean(), "required:false — the deadrock posture");
        }
    }

    /** The feature JSONs carry the masks: the spring rows, the vein columns, the atum stone placed chains. */
    @Test
    public void atumFeatureJsonsCarryTheMasks() throws Exception {
        // the spring table: the 4 atum rows lit, the rest dark
        JsonArray tSpringRows = json("data/gt6/worldgen/configured_feature/fluid_springs.json")
                .getAsJsonObject("config").getAsJsonArray("rows");
        int tLit = 0;
        for (JsonElement tEntry : tSpringRows) {
            JsonObject tRow = tEntry.getAsJsonObject();
            boolean tAtum = tRow.has("atum") && tRow.get("atum").getAsBoolean();
            if (tRow.get("name").getAsString().startsWith("atum.fluid.")) {
                assertTrue(tAtum, tRow.get("name").getAsString() + " ships atum:true");
                tLit++;
            } else {
                assertFalse(tAtum, tRow.get("name").getAsString() + " stays dark");
            }
        }
        assertEquals(4, tLit, "exactly the :789-792 band");
        // the vein table: 31 atum columns
        JsonArray tVeins = json("data/gt6/worldgen/configured_feature/large_veins.json")
                .getAsJsonObject("config").getAsJsonArray("veins");
        assertEquals(40, tVeins.size(), "the 40-row table");
        assertEquals(31, countTrue(tVeins, "atum"), "the :886-916 block");
        // the atum lens-stone placed chain: rarity 100 + uniform Y [0, 120] (the atum row numbers)
        JsonObject tMarble = json("data/gt6/worldgen/placed_feature/atum_stone_marble.json");
        assertEquals(100, tMarble.getAsJsonArray("modifiers").get(0).getAsJsonObject().get("chance").getAsInt(),
                "the 1/100 chunk gate");
        // the atum rocks twin: rarity 3 + count 3
        JsonObject tRocks = json("data/gt6/worldgen/placed_feature/atum_surface_rocks.json");
        JsonArray tRocksModifiers = tRocks.getAsJsonArray("modifiers");
        assertEquals(3, tRocksModifiers.get(0).getAsJsonObject().get("chance").getAsInt(), "atum.rocks rarity 3");
        assertEquals(3, tRocksModifiers.get(1).getAsJsonObject().get("count").getAsInt(), "atum.rocks count 3");
        // the atum small-ore host: the single tag arm
        JsonObject tCopper = json("data/gt6/worldgen/configured_feature/ore_small_atum/copper.json");
        JsonArray tTargets = tCopper.getAsJsonObject("config").getAsJsonArray("targets");
        assertEquals(1, tTargets.size(), "one atum host target");
        assertEquals("#gt6:atum_base_stone", tTargets.get(0).getAsJsonObject().get("target").getAsString(),
                "the OUR-tag indirection — a foreign id never appears");
    }

    private static int countTrue(JsonArray aRows, String aKey) {
        int r = 0;
        for (JsonElement tEntry : aRows) {
            if (tEntry.getAsJsonObject().has(aKey) && tEntry.getAsJsonObject().get(aKey).getAsBoolean()) r++;
        }
        return r;
    }
}
