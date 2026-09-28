/**
 * Tests for task r6-33-geode (GitHub #33 phase 1): the gem-geode band — vanilla
 * {@code Feature.GEODE} re-used as pure datapack JSON, one configured+placed+biome-
 * modifier triple per material.
 *
 * <p>Two pin faces, both offline-safe (the GT6WorldgenDatagenTest posture for the
 * constant band; file reads for the datagen products, the BurningBoxRenderDatagenTest
 * mdk-root walk):
 * <ul>
 * <li><b>anti-crash pair</b> — the acceptance's out-of-bounds guard: the geode codec
 *     REQUIRES a non-empty {@code inner_placements} (ExtraCodecs.nonEmptyList,
 *     GeodeBlockSettings.java:29, field-identical 1.20.1/1.21.1 — an empty list would
 *     fail datapack parse), while the sealed rows' {@code use_potential_placements_chance}
 *     must be 0.0 so the GeodeFeature.java:135-149 placement list stays empty and
 *     {@code Util.getRandom(innerPlacements)} at :149 never fires. Both facts are
 *     pinned against the generated JSON. Task r7-amethyst-budding-json re-pins the
 *     amethyst row to the vanilla budding ecosystem (the 4-level bud/cluster
 *     placements at the vanilla 0.35, gated by the budding_amethyst alternate inner
 *     layer at 0.083 — GeodeFeature.java:135 opens only on the alternate roll).</li>
 * <li><b>block-reference existence</b> — every {@code Name} in the geode JSONs must be
 *     a member of the offline-safe registration-id set (the GTMaterialBlocks
 *     registrationOrder walk + the GTStoneBlocks STONE-variant paths), guarding the
 *     JSON against dangling block keys without any registry access (the forge offline
 *     harness never fires RegisterEvent).</li>
 * </ul>
 *
 * <p>Placement canon: vanilla amethyst geode (CavePlacements.java:227-233) is
 * RarityFilter 24 + InSquare + uniform(above_bottom 6, absolute 30) + BiomeFilter at
 * the LOCAL_MODIFICATIONS step (BiomeDefaultFeatures.java:396) over #minecraft:
 * is_overworld. The band sparsens to rarity 64 and re-anchors the Y band to
 * [-40, 24] (the modern-Y deep-ore zone).
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.stone.StoneVariant;
import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.datagen.GT6WorldgenDatagen.GeodeMaterial;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;

class GT6GeodeWorldgenTest {

    @BeforeAll
    static void boot() {
        // the material system must exist before the registration walk dereferences
        // material names (GT6WorldgenDatagenTest posture); vanilla bootstrap bracket
        // for the ResourceKey/registry-key classes (offline throwables ignored).
        GTMaterialItems.initMaterials();
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** Location of the mdk project root, walking up from the (leg-dependent) test working dir. */
    private static Path mdkRoot() {
        for (Path tPath = Path.of("").toAbsolutePath(); tPath != null; tPath = tPath.getParent()) {
            if (Files.isRegularFile(tPath.resolve("tools").resolve("gen_textures.py"))) {
                return tPath;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    private static JsonObject generated(String aPath) throws IOException {
        return JsonParser.parseString(Files.readString(
                mdkRoot().resolve("src/generated/resources").resolve(aPath))).getAsJsonObject();
    }

    /** The generated JSON paths of the band, per material (the acceptance's 3-file family). */
    private static String configuredPath(String aMat) { return "data/gt6/worldgen/configured_feature/geode_" + aMat + ".json"; }
    private static String placedPath(String aMat)     { return "data/gt6/worldgen/placed_feature/geode_" + aMat + ".json"; }
    private static String modifierPath(String aBrand, String aMat) {
        return "data/gt6/" + aBrand + "/biome_modifier/geode_" + aMat + ".json";
    }

    /**
     * The offline-safe registration-id set: every blockGem block-item id path the
     * GTMaterialBlocks walk registers + the 17 stone STONE-variant paths (the band's
     * shell variants) — the "referenced GT block registration keys exist" assertion
     * face without a registry.
     */
    private static Set<String> gtBlockIdPaths() {
        Set<String> tIds = new HashSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
            tIds.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        }
        for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) {
            tIds.add(GTStoneBlocks.path(tStone.snake(), StoneVariant.STONE));
        }
        return tIds;
    }

    /** The gem block-item id of a geode row (GTMaterialItems.java:201 composition). */
    private static String gemId(GeodeMaterial aRow) {
        return GTMaterialItems.itemIdOf(gregapi.data.OP.blockGem, aRow.gem());
    }

    /** Pin 1: the 4-row material table, order + gem identity + shell stones are real GT stones. */
    @Test
    void geodeTableIsPinned() {
        List<String> tMats = GT6WorldgenDatagen.GEODE_MATERIALS.stream().map(GeodeMaterial::mat).toList();
        assertEquals(List.of("amethyst", "diamond", "emerald", "nether_quartz"), tMats,
                "the card candidate table: amethyst required + the gem family with GT blockGem forms");
        OreDictMaterial tQuartz = GT6WorldgenDatagen.GEODE_MATERIALS.get(3).gem();
        assertEquals("NetherQuartz", tQuartz.mNameInternal,
                "the quartz row is MT.NetherQuartz (mNameInternal 'NetherQuartz' -> snake nether_quartz), NOT Quartzite/MilkyQuartz");
        Set<String> tStoneSnakes = new HashSet<>();
        for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) tStoneSnakes.add(tStone.snake());
        for (GeodeMaterial tRow : GT6WorldgenDatagen.GEODE_MATERIALS) {
            assertTrue(tStoneSnakes.contains(tRow.middleStone()), tRow.mat() + " middle shell must be a GT stone");
            assertTrue(tStoneSnakes.contains(tRow.outerStone()), tRow.mat() + " outer shell must be a GT stone");
        }
        // the shells: amethyst keeps the vanilla look (marble/basalt), one row each
        assertEquals("marble", GT6WorldgenDatagen.GEODE_MATERIALS.get(0).middleStone());
        assertEquals("basalt", GT6WorldgenDatagen.GEODE_MATERIALS.get(0).outerStone());
        assertEquals("granite_black", GT6WorldgenDatagen.GEODE_MATERIALS.get(1).outerStone());
        assertEquals("blueschist", GT6WorldgenDatagen.GEODE_MATERIALS.get(2).outerStone());
    }

    /** Pin 2: the anti-crash parameter pair + the sealed-cavity shape, on every generated configured JSON. */
    @Test
    void configuredJsonAntiCrashPins() throws IOException {
        for (GeodeMaterial tRow : GT6WorldgenDatagen.GEODE_MATERIALS) {
            JsonObject tJson = generated(configuredPath(tRow.mat()));
            assertEquals("minecraft:geode", tJson.get("type").getAsString(), tRow.mat() + ": the vanilla geode feature");
            JsonObject tConfig = tJson.getAsJsonObject("config");
            JsonObject tBlocks = tConfig.getAsJsonObject("blocks");
            // the out-of-bounds guard: chance 0 keeps GeodeFeature's placement list
            // empty; the amethyst row instead opens the budding ecosystem at the
            // vanilla 0.35 (task r7-amethyst-budding-json)
            boolean tBudding = "amethyst".equals(tRow.mat());
            assertEquals(tBudding ? GT6WorldgenDatagen.GEODE_BUDDING_PLACEMENT_CHANCE : 0.0,
                    tConfig.get("use_potential_placements_chance").getAsDouble(),
                    tRow.mat() + ": use_potential_placements_chance (0 = the Util.getRandom guard, GeodeFeature:135-149; amethyst = the vanilla 0.35, CaveFeatures.java:450)");
            // the codec trap: inner_placements is ExtraCodecs.nonEmptyList — an EMPTY
            // list is unparseable. Sealed rows carry the singleton gem state (never
            // sampled at chance 0); the amethyst row satisfies nonEmptyList with the
            // vanilla 4-level bud/cluster set (CaveFeatures.java:439-444, order pinned;
            // FACING is feature-set at GeodeFeature.java:151-154, so no Properties)
            JsonArray tPlacements = tBlocks.getAsJsonArray("inner_placements");
            if (tBudding) {
                assertEquals(4, tPlacements.size(), tRow.mat() + ": the vanilla 4-level bud/cluster set (nonEmptyList satisfied by 4)");
                List<String> tNames = new ArrayList<>();
                for (JsonElement tPlacement : tPlacements) {
                    tNames.add(tPlacement.getAsJsonObject().get("Name").getAsString());
                }
                assertEquals(List.of("minecraft:small_amethyst_bud", "minecraft:medium_amethyst_bud",
                        "minecraft:large_amethyst_bud", "minecraft:amethyst_cluster"), tNames,
                        tRow.mat() + ": the vanilla placement set, CaveFeatures.java:439-444 order");
            } else {
                assertEquals(1, tPlacements.size(),
                        tRow.mat() + ": inner_placements must be the non-empty singleton (codec nonEmptyList, GeodeBlockSettings:29)");
            }
            // the budding gate: placements_require_layer0_alternate stays the vanilla
            // true (CaveFeatures.java:452), so the 0.35 roll only fires where the
            // ALTERNATE inner layer rolled (GeodeFeature.java:135) — the amethyst row
            // must therefore also carry the budding substrate at the vanilla 0.083
            // (:451); without it the gate never opens and NO bud would ever place
            assertTrue(tConfig.get("placements_require_layer0_alternate").getAsBoolean(),
                    tRow.mat() + ": the vanilla gate form");
            assertEquals(tBudding ? GT6WorldgenDatagen.GEODE_BUDDING_ALTERNATE_CHANCE : 0.0,
                    tConfig.get("use_alternate_layer0_chance").getAsDouble(),
                    tRow.mat() + ": use_alternate_layer0_chance (the GeodeFeature:135 gate opener)");
            assertEquals(tBudding ? "minecraft:budding_amethyst" : "gt6:" + tRow.middleStone(),
                    tBlocks.getAsJsonObject("alternate_inner_layer_provider").getAsJsonObject("state").get("Name").getAsString(),
                    tRow.mat() + ": the alternate inner layer (inert middle stone / the vanilla budding substrate, CaveFeatures.java:436)");
            // the sealed static cavity: air filling + no crack (the r7 ruling: the crack
            // stays off on the budding row too — findability is a later one-number flip
            // to the vanilla 0.95, CaveFeatures.java:449)
            assertEquals("minecraft:air", tBlocks.getAsJsonObject("filling_provider").getAsJsonObject("state").get("Name").getAsString(),
                    tRow.mat() + ": filling = air (the sealed-cavity face; the vanilla fill IS air, CaveFeatures.java:434)");
            assertEquals(0.0, tConfig.getAsJsonObject("crack").get("generate_crack_chance").getAsDouble(),
                    tRow.mat() + ": crack disabled");
            // the vanilla layer radii verbatim
            JsonObject tLayers = tConfig.getAsJsonObject("layers");
            assertEquals(1.7, tLayers.get("filling").getAsDouble());
            assertEquals(2.2, tLayers.get("inner_layer").getAsDouble());
            assertEquals(3.2, tLayers.get("middle_layer").getAsDouble());
            assertEquals(4.2, tLayers.get("outer_layer").getAsDouble());
            // the cross-leg byte-identity pins: the vanilla uniform IntProviders are
            // CONSTANT-pinned (DFU6 wraps {"value":{min,max}}, DFU8 inlines — a uniform
            // provider forks the legs). ConstantInt serializes to the BARE INT both legs —
            // the shape itself proves the provider is constant, not uniform.
            assertEquals(5, tConfig.get("outer_wall_distance").getAsInt(),
                    tRow.mat() + ": outer_wall_distance bare-int constant 5 (the vanilla 4-6 mid)");
            assertEquals(4, tConfig.get("distribution_points").getAsInt(),
                    tRow.mat() + ": distribution_points bare-int constant 4 (the vanilla 3-4 top)");
            assertEquals(1, tConfig.get("point_offset").getAsInt(),
                    tRow.mat() + ": point_offset bare-int constant 1 (the vanilla 1-2 low)");
        }
    }

    /** Pin 3: every block reference in the geode JSON family resolves to a registered GT id (or the vanilla air face). */
    @Test
    void configuredJsonBlockReferencesExist() throws IOException {
        Set<String> tValid = gtBlockIdPaths();
        for (GeodeMaterial tRow : GT6WorldgenDatagen.GEODE_MATERIALS) {
            JsonObject tBlocks = generated(configuredPath(tRow.mat())).getAsJsonObject("config").getAsJsonObject("blocks");
            Set<String> tNames = new HashSet<>();
            for (String tKey : new String[] {"filling_provider", "inner_layer_provider",
                    "alternate_inner_layer_provider", "middle_layer_provider", "outer_layer_provider"}) {
                tNames.add(tBlocks.getAsJsonObject(tKey).getAsJsonObject("state").get("Name").getAsString());
            }
            for (JsonElement tPlacement : tBlocks.getAsJsonArray("inner_placements")) {
                String tName = tPlacement.getAsJsonObject().get("Name").getAsString();
                if (tName.startsWith("minecraft:")) continue; // the vanilla faces: air never (filling), the amethyst budding family — pinned verbatim in Pin 2
                tNames.add(tName);
            }
            for (String tName : tNames) {
                if (tName.startsWith("minecraft:")) continue; // the vanilla faces: air filling + the amethyst budding substrate (both pinned verbatim in Pin 2)
                assertTrue(tName.startsWith("gt6:"), tRow.mat() + ": " + tName + " must be GT-namespaced");
                assertTrue(tValid.contains(tName.substring("gt6:".length())),
                        tRow.mat() + ": " + tName + " is not a GT registration-order id (dangling block reference)");
            }
            // the inner layer IS the row's gem block
            assertEquals("gt6:" + gemId(tRow),
                    tBlocks.getAsJsonObject("inner_layer_provider").getAsJsonObject("state").get("Name").getAsString(),
                    tRow.mat() + ": inner shell must be the row's own blockGem block");
        }
    }

    /** Pin 4: the placed JSONs — the sparse rarity + the GT deep-zone Y band, vanilla chain shape. */
    @Test
    void placedJsonIsPinned() throws IOException {
        assertEquals(64, GT6WorldgenDatagen.GEODE_RARITY, "rarity 64 = ~2.7x sparser than vanilla amethyst 24");
        assertEquals(-40, GT6WorldgenDatagen.GEODE_MIN_Y, "the deep-zone band floor");
        assertEquals(24, GT6WorldgenDatagen.GEODE_MAX_Y, "the band ceiling (vanilla geodes end at absolute 30)");
        for (GeodeMaterial tRow : GT6WorldgenDatagen.GEODE_MATERIALS) {
            JsonObject tJson = generated(placedPath(tRow.mat()));
            assertEquals("gt6:geode_" + tRow.mat(), tJson.get("feature").getAsString());
            JsonArray tChain = tJson.getAsJsonArray("placement");
            assertEquals(4, tChain.size(), tRow.mat() + ": rarity+square+height+biome, the vanilla chain shape");
            JsonObject tRarity = tChain.get(0).getAsJsonObject();
            assertEquals("minecraft:rarity_filter", tRarity.get("type").getAsString());
            assertEquals(64, tRarity.get("chance").getAsInt(), tRow.mat() + ": the sparse rarity");
            assertEquals("minecraft:in_square", tChain.get(1).getAsJsonObject().get("type").getAsString());
            JsonObject tHeight = tChain.get(2).getAsJsonObject().getAsJsonObject("height");
            assertEquals("minecraft:uniform", tHeight.get("type").getAsString());
            assertEquals(-40, tHeight.getAsJsonObject("min_inclusive").get("absolute").getAsInt());
            assertEquals(24, tHeight.getAsJsonObject("max_inclusive").get("absolute").getAsInt());
            assertEquals("minecraft:biome", tChain.get(3).getAsJsonObject().get("type").getAsString());
        }
    }

    /** Pin 5: the 8 biome modifiers — per material x per brand, overworld + LOCAL_MODIFICATIONS. */
    @Test
    void biomeModifiersArePinned() throws IOException {
        List<String> tBrands = List.of("forge", "neoforge");
        for (GeodeMaterial tRow : GT6WorldgenDatagen.GEODE_MATERIALS) {
            for (String tBrand : tBrands) {
                JsonObject tJson = generated(modifierPath(tBrand, tRow.mat()));
                assertEquals(tBrand + ":add_features", tJson.get("type").getAsString(),
                        tRow.mat() + "/" + tBrand + ": the brand type string");
                assertEquals("#minecraft:is_overworld", tJson.get("biomes").getAsString());
                assertEquals("gt6:geode_" + tRow.mat(), tJson.get("features").getAsString());
                assertEquals("local_modifications", tJson.get("step").getAsString(),
                        tRow.mat() + ": the vanilla geode step (BiomeDefaultFeatures.java:396)");
            }
        }
    }

    /** Pin 6: the datagen key tables — paths and registry faces (the BUILDER_KEYS audit posture). */
    @Test
    void keyTablesArePinned() {
        assertEquals(GT6WorldgenDatagen.GEODE_MATERIALS.size(), GT6WorldgenDatagen.GEODE_CONFIGURED_KEYS.size());
        assertEquals(GT6WorldgenDatagen.GEODE_MATERIALS.size(), GT6WorldgenDatagen.GEODE_PLACED_KEYS.size());
        assertEquals(GT6WorldgenDatagen.GEODE_MATERIALS.size(), GT6WorldgenDatagen.GEODE_BIOME_MODIFIER_KEYS.size());
        for (int i = 0; i < GT6WorldgenDatagen.GEODE_MATERIALS.size(); i++) {
            String tMat = GT6WorldgenDatagen.GEODE_MATERIALS.get(i).mat();
            assertEquals("minecraft:worldgen/configured_feature",
                    GT6WorldgenDatagen.GEODE_CONFIGURED_KEYS.get(i).registry().toString());
            assertEquals("geode_" + tMat,
                    GT6WorldgenDatagen.GEODE_CONFIGURED_KEYS.get(i).location().getPath(),
                    "the configured key path (namespace gt6, the ResourceKey.toString 'gt6:geode_<mat>' face)");
            assertEquals("minecraft:worldgen/placed_feature",
                    GT6WorldgenDatagen.GEODE_PLACED_KEYS.get(i).registry().toString());
            assertEquals("geode_" + tMat, GT6WorldgenDatagen.GEODE_PLACED_KEYS.get(i).location().getPath());
            assertNotNull(GT6WorldgenDatagen.GEODE_BIOME_MODIFIER_KEYS.get(i).location());
            assertEquals("geode_" + tMat, GT6WorldgenDatagen.GEODE_BIOME_MODIFIER_KEYS.get(i).location().getPath());
        }
    }
}
