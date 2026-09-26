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
 * <li>parts — the dense walls × 8 designs, the new-form rows (designs>0 → the design
 *     ladder, else the singleton), the heat transmitter, the coke-oven bricks and,
 *     since the C5 clean-up, machine_wall_tungsten (partModel);</li>
 * <li>turbines/dynamo housings — turbine_main_{steam,gas,dynamo} (addTurbineFamily);</li>
 * <li>bridges/lasers/absorber/energizer — the six orientable textures
 *     (addBridgeFamily, model name = the texture token);</li>
 * <li>the magic absorber; the tank valves (tank_wood/tank_metal) and the crucible
 *     walls (tintedCube); the boiler tank (tintedCube, the C5 wiring).</li>
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
        // the new-form rows (the registration-side machine_wall_tungsten skip mirrored:
        // the lightning-rod registration emits the plain partModel, not the design ladder)
        for (var tRow : gregtech6.registry.GTMultiBlocks.NEW_PART_ROWS) {
            if (tRow.path().equals("machine_wall_tungsten")) continue;
            if (tRow.designs() > 0) {
                for (int d = 0; d <= tRow.designs(); d++) rModels.add(tRow.path() + "_design_" + d);
            } else {
                rModels.add(tRow.path());
            }
        }
        rModels.add("heat_transmitter");
        rModels.add("multiblock_coke_oven_bricks");
        rModels.add("machine_wall_tungsten"); // the C5 clean-up (the lightning-rod registration)
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
                "steam_boiler_tank")); // addBoilers (the C5 wiring, the tintedCube front overload)
        for (String tPath : gregtech6.registry.GT6Crucibles.CRUCIBLE_WALL_BLOCKS_BY_PATH.keySet()) {
            rModels.add(tPath); // the eight dedicated crucible walls (tintedCube)
        }
        // the bee trio (task r3-beehive-tint): the addHive/bumbliaryModel two-layer
        // shells over the tintindex-0 body — model names are bands, not registry paths
        // (bumbliary_adv vs the bumbliary_advanced block), hence the pinned literals
        rModels.add("bumble_hive");
        rModels.add("bumbliary");
        rModels.add("bumbliary_adv");
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
}
