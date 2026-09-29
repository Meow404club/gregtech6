/**
 * The paintable-family {@code render_type} census (task r3-world-tint-render-type, the
 * issue #8 world-bake tint fix): every model the {@code GTMachineTintModel} walk wraps
 * must declare {@code "render_type": "minecraft:cutout"} — the D2 root cause pair is
 * (a) the P22 overlay shells' transparent texels paint as opaque plates on the default
 * SOLID chunk layer (no alpha discard) and (b) the wrapper buried the JSON declaration
 * until the {@code GTDynamicBakedModel.getRenderTypes} forward. This census pins leg
 * (a) against the committed generated tree: a family whose builder loses the
 * {@code .renderType("cutout")} call goes red here, not gray in a player's world.
 *
 * <p>The model universe mirrors the walk families (the GT6AssetCoverageGuardTest
 * filesystem shape — the JSONs are read from the static ∪ generated trees, the
 * registration row tables are read offline through plain class init):
 * <ul>
 * <li>machines — {@link GT6MachinePaintRenderDatagenTest#MACHINE_BASES} × 3 state
 *     models + the advanced crafting table (the machineModel/familyMachineModel
 *     builders);</li>
 * <li>burning boxes — the five group pairs (burningBoxModel);</li>
 * <li>parts — the dense walls x 8 designs, the new-form rows (designs>0 → the design
 *     ladder, else the singleton — machine_wall_tungsten walks the ladder since task
 *     debt-tungsten-wall-designs), the heat transmitter and the coke-oven bricks;</li>
 * <li>turbines/dynamo housings — turbine_main_{steam,gas,dynamo} (addTurbineFamily);</li>
 * <li>bridges/lasers/absorber/energizer — the six orientable textures
 *     (addBridgeFamily, model name = the texture token);</li>
 * <li>the magic absorber; the tank valves (tank_wood/tank_metal) and the crucible
 *     walls (tintedCube); the boiler tank + the large boiler band (the two-layer
 *     boilerModel shells, task r8-tex-large-boilers over the C5 wiring).</li>
 * <li>the bee trio — bumble_hive/bumbliary/bumbliary_adv (the addHive/
 *     bumbliaryModel two-layer grammar; since the r3-beehive-tint return-fix —
 *     the trio entered the {@code GTMachineTintModel} walk with the C4 card but
 *     this census did not know them, so the SOLID-layer white-plating regression
 *     shipped; the structural lesson: the universe must mirror the walk, not the
 *     last card's families).</li>
 * </ul>
 *
 * <p>EXEMPT (the declared deviation): the kitchen quartet — the #7 models carry
 * tintindex 0 and ARE wrapped, but their 18 borrowed PNGs are fully opaque (the PIL
 * census: zero texels below alpha 255) and the hollow-tub shapes ship no overlay shell,
 * so the SOLID layer renders them byte-identically (the card's census exemption).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6PaintableRenderTypeCensusTest {

    @BeforeAll
    public static void bootOffline() {
        // the row-table reads class-init the registry containers (the GTMultiBlocks
        // statics reach ForgeRegistries.Keys) — the GT6AssetCoverageGuardTest bracket
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
    }

    /** The kitchen exemption (the card's census ruling — opaque, shell-free). */
    private static final Set<String> KITCHEN_MODELS = Set.of(
            "bathing_pot_wood", "bathing_pot_steel", "mixing_bowl", "juicer");

    /** The addMachine three-model split. */
    private static final List<String> MODEL_SUFFIXES = List.of("", "_active", "_running");

    // ---------------------------------------------------------------------
    // the universe
    // ---------------------------------------------------------------------

    private static List<String> machineModels() {
        List<String> rModels = new ArrayList<>();
        for (String tBase : GT6MachinePaintRenderDatagenTest.MACHINE_BASES) {
            for (String tSuffix : MODEL_SUFFIXES) rModels.add(tBase + tSuffix);
        }
        rModels.add("advanced_crafting_table"); // machineModel (the front-decal form)
        return rModels;
    }

    private static List<String> burningBoxModels() {
        List<String> rModels = new ArrayList<>();
        for (String tGroup : new String[] {"solid", "liquid", "gas", "fluidbed", "brick"}) {
            rModels.add("burning_box_" + tGroup);
            rModels.add("burning_box_" + tGroup + "_lit");
        }
        return rModels;
    }

    private static List<String> partModels() {
        Set<String> rModels = new LinkedHashSet<>();
        // the dense walls — DESIGNS pinned at 7 by the wall-carrier ctor
        for (var tRow : gregtech6.registry.GTMultiBlocks.WALL_ROWS) {
            for (int d = 0; d <= 7; d++) rModels.add(tRow.path() + "_design_" + d);
        }
        // the new-form rows (machine_wall_tungsten included — task
        // debt-tungsten-wall-designs gave the Lightning Rod registration the :1151
        // NBT_DESIGNS 7 range, so the row emits the design ladder like its siblings)
        for (var tRow : gregtech6.registry.GTMultiBlocks.NEW_PART_ROWS) {
            if (tRow.designs() > 0) {
                for (int d = 0; d <= tRow.designs(); d++) rModels.add(tRow.path() + "_design_" + d);
            } else {
                rModels.add(tRow.path());
            }
        }
        rModels.add("heat_transmitter");
        rModels.add("multiblock_coke_oven_bricks");
        return new ArrayList<>(rModels);
    }

    private static List<String> controllerModels() {
        List<String> rModels = new ArrayList<>(List.of(
                "turbine_main_steam", "turbine_main_gas", "turbine_main_dynamo", // addTurbineFamily
                "bridge_heater", "bridge_engine", "bridge_motor", // addBridgeFamily (model = the texture token)
                "laser_electric", "laser_absorber", "quantum_energizer",
                "magic_absorber", // addMagicAbsorber
                "tank_wood", "tank_metal", // addTanks (tintedCube)
                "crucible_steel_wall", // addLargeCrucible (tintedCube)
                "steam_boiler_tank")); // addBoilers (the C5 wiring, the two-layer boilerModel form)
        for (String tPath : gregtech6.registry.GT6Crucibles.CRUCIBLE_WALL_BLOCKS_BY_PATH.keySet()) {
            rModels.add(tPath); // the eight dedicated crucible walls (tintedCube)
        }
        // the bee trio (task r3-beehive-tint): the addHive/bumbliaryModel two-layer
        // shells over the tintindex-0 body — model names are bands, not registry paths
        // (bumbliary_adv vs the bumbliary_advanced block), hence the pinned literals
        rModels.add("bumble_hive");
        rModels.add("bumbliary");
        rModels.add("bumbliary_adv");
        // the converter family (issue #18, task r4-18-converter-tex-facing): the
        // transformer + dynamo model pairs (the addConverterModel two-layer shells over
        // the tintindex-0 body — model names are bands, not registry paths)
        rModels.add("electric_transformer");
        rModels.add("electric_transformer_active");
        rModels.add("electric_dynamo");
        rModels.add("electric_dynamo_active");
        rModels.add("flux_dynamo");
        rModels.add("flux_dynamo_active");
        // task r8-tex-bridge-kinetic — the bridge/laser ACTIVE shells (heater + the two
        // lasers; the art-less engine/motor pairs stay on the inactive names above) and
        // the kinetic joiners: the steam/diesel engine shells + the rotation transformer
        rModels.add("bridge_heater_active");
        rModels.add("laser_electric_active");
        rModels.add("laser_absorber_active");
        rModels.add("steam_engine");
        rModels.add("diesel_engine");
        rModels.add("transformer_rotation");
        // the boiler domains (task r8-tex-large-boilers): the steam boiler tank already
        // rides the List.of above (since the C5 wiring, now the two-layer boilerModel
        // form); the large boiler band model joins — the two-layer front-bearing shell
        // the five controllers share
        rModels.add("large_boiler");
        // the four multiblockmains controller families (task r8-tex-multiblockmains):
        // the shared two-layer front-bearing boilerModel shells over the tintindex-0
        // body — the shared large_crucible band model is not a registry path, the other
        // three keep their registry-path model names
        rModels.add("large_crucible");
        rModels.add("logistics_core");
        rModels.add("multiblock_lightning_rod");
        rModels.add("large_heat_exchanger");
        // the pipe connector families (task r8-tex-pipe-textures): the four shared
        // tintedPipeModel two-layer shells over the tintindex-0 body — model names are
        // texture paths, not registry paths, hence the pinned literals
        rModels.add("materialicons/wood/pipe_side");
        rModels.add("materialicons/copper/pipe_side");
        rModels.add("materialicons/copper/pipe_side_restrictive");
        rModels.add("iconsets/logistics_wire");
        // the 17 large-controller domains (task r8-tex-large-machines): the
        // familyMachineModel state trios (the twelve large-12 rows + the massfab/fusion/
        // implosion controllers — the _active/_running bands ride the tree with the
        // blockstate switch declared defer) + the two boilerModel front pairs (no
        // upstream active group, one static band each)
        for (var tRow : gregtech6.registry.GT6LargeMachines.ROWS) {
            rModels.add(tRow.path());
            rModels.add(tRow.path() + "_active");
            rModels.add(tRow.path() + "_running");
        }
        for (String tBase : new String[] {"large_massfab", "fusion_reactor", "implosion_compressor"}) {
            rModels.add(tBase);
            rModels.add(tBase + "_active");
            rModels.add(tBase + "_running");
        }
        rModels.add("von_da_graagg");
        rModels.add("bedrock_drill");
        return rModels;
    }

    // ---------------------------------------------------------------------
    // the census
    // ---------------------------------------------------------------------

    /** Every paintable model declares the alpha-discarding cutout chunk layer. */
    @Test
    public void everyPaintableModelDeclaresCutout() throws Exception {
        List<String> tUniverse = new ArrayList<>();
        tUniverse.addAll(machineModels());
        tUniverse.addAll(burningBoxModels());
        tUniverse.addAll(partModels());
        tUniverse.addAll(controllerModels());
        assertTrue(tUniverse.size() > 700, "model universe implausibly small: " + tUniverse.size()
                + " — the census walk broke, this must never pass vacuously");

        List<String> tOffenders = new ArrayList<>();
        for (String tModel : tUniverse) {
            JsonObject tJson = blockModelJson(tModel);
            JsonElement tRenderType = tJson.get("render_type");
            if (tRenderType == null || !"minecraft:cutout".equals(tRenderType.getAsString())) {
                tOffenders.add(tModel + " -> " + (tRenderType == null ? "<missing>" : tRenderType.getAsString()));
            }
        }
        assertTrue(tOffenders.isEmpty(), "paintable models without the cutout declaration (the #8 "
                + "occlusion fix regressed — the SOLID layer plates the decal shells over the tint): "
                + tOffenders);
    }

    /** The kitchen exemption is exact: the quartet stays undeclared (the deviation stays visible). */
    @Test
    public void kitchenExemptionIsExact() throws Exception {
        for (String tModel : KITCHEN_MODELS) {
            JsonObject tJson = blockModelJson(tModel);
            // the quartet is wrapped and tintindex-0 but ships no shell — the declared
            // deviation; it must NOT declare a render type (a declaration would silently
            // narrow the exemption back to zero)
            assertTrue(tJson.get("render_type") == null,
                    tModel + " declares a render_type — fold it into the cutout census and retire the exemption");
        }
    }

    // ---------------------------------------------------------------------
    // the tree face
    // ---------------------------------------------------------------------

    /** Location of the mdk project root (the GT6AssetCoverageGuardTest walk). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
                return p;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    /** The block model JSON over the static ∪ generated trees (must exist — the coverage guard's face). */
    private static JsonObject blockModelJson(String aModel) throws IOException {
        for (String tTree : new String[] {"src/generated/resources", "src/main/resources"}) {
            Path tPath = mdkRoot().resolve(tTree).resolve("assets/gt6/models/block").resolve(aModel + ".json");
            if (Files.isRegularFile(tPath)) {
                try (var tReader = Files.newBufferedReader(tPath, StandardCharsets.UTF_8)) {
                    return JsonParser.parseReader(tReader).getAsJsonObject();
                }
            }
        }
        throw new AssertionError("block model JSON not found on disk: " + aModel
                + " — the datagen walk and this census disagree");
    }

    /**
     * The r8-tex-tank-family barrel addition (tail-append): the 15 two-layer barrel
     * models are NOT in the {@code GTMachineTintModel} walk (the p23 runtime-BlockColor
     * domain), but their {@code barrel_parts} shells carry the same alpha-discard need —
     * a barrel model losing its cutout declaration plates the decal shell on the SOLID
     * layer here, not in a player's world. The valve pair (tank_wood/tank_metal) stays
     * covered by {@link #controllerModels()} — the model names survived the r8 rewire.
     * The p12 logistics row is the declared single-layer exemption (no decal shell —
     * {@code tintedCubeAll} needs no alpha discard), pinned exact like the kitchen
     * quartet above.
     */
    @Test
    public void barrelModelsJoinTheCutoutCensus() throws Exception {
        List<String> tUniverse = new ArrayList<>(List.of("barrel_wood", "barrel_plastic", "barrel_metal"));
        for (var tRow : gregtech6.registry.GTBarrels.HIGH_TIER_METAL_DRUMS) tUniverse.add(tRow.path());
        org.junit.jupiter.api.Assertions.assertEquals(15, tUniverse.size(),
                "the two-layer barrel census stays 15 (3 rows + 12 high-tier drums)");
        List<String> tOffenders = new ArrayList<>();
        for (String tModel : tUniverse) {
            JsonObject tJson = blockModelJson(tModel);
            JsonElement tRenderType = tJson.get("render_type");
            if (tRenderType == null || !"minecraft:cutout".equals(tRenderType.getAsString())) {
                tOffenders.add(tModel + " -> " + (tRenderType == null ? "<missing>" : tRenderType.getAsString()));
            }
        }
        assertTrue(tOffenders.isEmpty(), "barrel models without the cutout declaration "
                + "(the two-layer overlay shells plate over the tint on the SOLID layer): " + tOffenders);
        // the declared single-layer exemption stays shell-free (one element, no render_type)
        JsonObject tLogistics = blockModelJson("barrel_logistics");
        assertTrue(tLogistics.get("render_type") == null,
                "barrel_logistics declares a render_type — fold it into the cutout census and retire the exemption");
        org.junit.jupiter.api.Assertions.assertEquals(1, tLogistics.getAsJsonArray("elements").size(),
                "barrel_logistics stays the single-layer form (the r8 two-layer borrow deferred)");
    }
}
