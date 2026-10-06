/**
 * The paintable-family {@code render_type} census (task world-tint-render-type, the
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
 *     boilerModel shells, task tex-large-boilers over the C5 wiring).</li>
 * <li>the bee trio — bumble_hive/bumbliary/bumbliary_adv (the addHive/
 *     bumbliaryModel two-layer grammar; since the beehive-tint return-fix —
 *     the trio entered the {@code GTMachineTintModel} walk with the C4 card but
 *     this census did not know them, so the SOLID-layer white-plating regression
 *     shipped; the structural lesson: the universe must mirror the walk, not the
 *     last card's families).</li>
 * </ul>
 *
 * <p>EXEMPT (the declared deviation): the kitchen shell-less pair (bathing_pot_steel +
 * juicer) — the #7 models carry tintindex 0 and ARE wrapped, their borrowed PNGs are
 * fully opaque (the PIL census: zero texels below alpha 255) and the hollow-tub shapes
 * ship no overlay shell, so the SOLID layer renders them byte-identically (the card's
 * census exemption). The other half of the quartet left the exemption with the
 * mixingbowl-bathingpot-fidelity merge (27372d1bf): bathing_pot_wood and mixing_bowl
 * grew the P22 overlay shells (the transparent-texel case this census exists for) and
 * declare cutout — pinned in the universe walk below.
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

    /**
     * The kitchen exemption (the card's census ruling — opaque, shell-free). Since the
     * mixingbowl-bathingpot-fidelity merge (27372d1bf — the user 2026-10-06 six-symptom
     * ruling) it covers ONLY the shell-less pair: the wood pot and the mixing bowl grew
     * the P22 overlay shells (transparent texels → SOLID-layer plating, this census's
     * exact root cause) and declare cutout — folded into the universe below. The steel
     * pot and the juicer remain fully-opaque shell-free tubs, the declared deviation.
     * (The review-seat rebase seam kept THIS named-set form as the single source of
     * truth — the task quantum-energizer-tint-overlay sweep folded the same pair out
     * with the same direction, the duplicate literals ride the set below.)
     */
    private static final Set<String> KITCHEN_MODELS = Set.of("bathing_pot_steel", "juicer");

    /** The shelled kitchen pair (merge 27372d1bf) — now under the cutout regime, not the exemption. */
    private static final Set<String> KITCHEN_SHELLED_MODELS = Set.of("bathing_pot_wood", "mixing_bowl");

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
        rModels.add("advanced_crafting_table"); // machineModel (the front-decal form) — act-matrix: ONE shared model over all 120 rows (the declared transitional render)
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
                // the shelled kitchen pair rides KITCHEN_SHELLED_MODELS (the universe
                // addAll below) — the review-seat rebase seam removed the duplicate
                // literals the task quantum-energizer-tint-overlay sweep added here
                "magic_absorber", // addMagicAbsorber
                "tank_wood", "tank_metal", // addTanks (tintedCube)
                "steam_boiler_tank")); // addBoilers (the C5 wiring, the two-layer boilerModel form)
        // the eight dedicated crucible walls (task mb-formed-crucible-wall — the metalwall
        // design ladder, the partModel two-layer form: 8 walls x designs 0..7)
        for (String tPath : crucibleWallPaths()) {
            for (int d = 0; d <= 7; d++) rModels.add(tPath + "_design_" + d);
        }
        // the bee trio (task beehive-tint): the addHive/bumbliaryModel two-layer
        // shells over the tintindex-0 body — model names are bands, not registry paths
        // (bumbliary_adv vs the bumbliary_advanced block), hence the pinned literals
        rModels.add("bumble_hive");
        rModels.add("bumbliary");
        rModels.add("bumbliary_adv");
        // the converter family (issue #18, task 18-converter-tex-facing): the
        // transformer + dynamo model pairs (the addConverterModel two-layer shells over
        // the tintindex-0 body — model names are bands, not registry paths)
        rModels.add("electric_transformer");
        rModels.add("electric_transformer_active");
        rModels.add("electric_dynamo");
        rModels.add("electric_dynamo_active");
        rModels.add("flux_dynamo");
        rModels.add("flux_dynamo_active");
        // task tex-bridge-kinetic — the bridge/laser ACTIVE shells (heater + the two
        // lasers; the art-less engine/motor pairs stay on the inactive names above) and
        // the kinetic joiners: the steam/diesel engine shells + the rotation transformer
        rModels.add("bridge_heater_active");
        rModels.add("laser_electric_active");
        rModels.add("laser_absorber_active");
        // task quantum-energizer-tint-overlay — the quantum ACTIVE shell joins the
        // two-layer arm (the former single-layer family)
        rModels.add("quantum_energizer_active");
        rModels.add("steam_engine");
        rModels.add("diesel_engine");
        rModels.add("transformer_rotation");
        // the boiler domains (task tex-large-boilers): the steam boiler tank already
        // rides the List.of above (since the C5 wiring, now the two-layer boilerModel
        // form); the large boiler band model joins — the two-layer front-bearing shell
        // the five controllers share
        rModels.add("large_boiler");
        // the four multiblockmains controller families (task tex-multiblockmains):
        // the shared two-layer front-bearing boilerModel shells over the tintindex-0
        // body — the shared large_crucible band model is not a registry path, the other
        // three keep their registry-path model names
        rModels.add("large_crucible");
        rModels.add("logistics_core");
        rModels.add("multiblock_lightning_rod");
        rModels.add("large_heat_exchanger");
        // task coke-oven-texture — the coke oven controller joins the walk (the last
        // placeholder multiblock main): the two-layer body+window-decal shell over the
        // tintindex-0 body
        rModels.add("multiblock_coke_oven");
        // the pipe connector families (task tex-pipe-textures): the four shared
        // tintedPipeModel two-layer shells over the tintindex-0 body — model names are
        // texture paths, not registry paths, hence the pinned literals
        rModels.add("materialicons/wood/pipe_side");
        rModels.add("materialicons/copper/pipe_side");
        rModels.add("materialicons/copper/pipe_side_restrictive");
        rModels.add("iconsets/logistics_wire");
        // the 17 large-controller domains (task tex-large-machines): the
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

    /** The 8 dedicated crucible wall paths (the steel rung + the seven ladder rows). */
    private static List<String> crucibleWallPaths() {
        List<String> rPaths = new ArrayList<>(List.of("crucible_steel_wall"));
        rPaths.addAll(gregtech6.registry.GT6Crucibles.CRUCIBLE_WALL_BLOCKS_BY_PATH.keySet());
        return rPaths;
    }

    /** Every paintable model declares the alpha-discarding cutout chunk layer. */
    @Test
    public void everyPaintableModelDeclaresCutout() throws Exception {
        List<String> tUniverse = new ArrayList<>();
        tUniverse.addAll(machineModels());
        tUniverse.addAll(burningBoxModels());
        tUniverse.addAll(partModels());
        tUniverse.addAll(controllerModels());
        // the shelled kitchen pair (mixingbowl-bathingpot-fidelity, merge 27372d1bf): the
        // overlay shells are the transparent-texel case this census exists for — the
        // universe must mirror the walk (the beehive-tint lesson), not the last card's
        // families
        tUniverse.addAll(KITCHEN_SHELLED_MODELS);
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

    /**
     * The kitchen exemption is exact: the shell-less pair (steel pot + juicer) stays
     * undeclared (the deviation stays visible) while the shelled pair (wood pot + mixing
     * bowl, the overlay shells since the mixingbowl-bathingpot-fidelity merge 27372d1bf)
     * sits under the cutout regime (pinned in the universe walk above) — the exemption
     * narrowed with the shells, it did not silently widen.
     */
    @Test
    public void kitchenExemptionIsExact() throws Exception {
        for (String tModel : KITCHEN_MODELS) {
            JsonObject tJson = blockModelJson(tModel);
            // the pair is wrapped and tintindex-0 but ships no shell — the declared
            // deviation; it must NOT declare a render type (a declaration would silently
            // narrow the exemption back to zero)
            assertTrue(tJson.get("render_type") == null,
                    tModel + " declares a render_type — fold it into the cutout census and retire the exemption");
        }
        for (String tModel : KITCHEN_SHELLED_MODELS) {
            JsonObject tJson = blockModelJson(tModel);
            // the shelled pair: the P22 overlay shells need alpha discard — the opposite
            // arm of the same exactness (a lost .renderType("cutout") call re-plates the
            // shells on the SOLID layer)
            assertTrue(tJson.has("render_type") && "minecraft:cutout".equals(tJson.get("render_type").getAsString()),
                    tModel + " lost its cutout declaration — the overlay shells would plate SOLID (merge 27372d1bf state)");
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
     * The tex-tank-family barrel addition (tail-append): the two-layer barrel
     * models are NOT in the {@code GTMachineTintModel} walk (the p23 runtime-BlockColor
     * domain), but their {@code barrel_parts} shells carry the same alpha-discard need —
     * a barrel model losing its cutout declaration plates the decal shell on the SOLID
     * layer here, not in a player's world. The valve pair (tank_wood/tank_metal) stays
     * covered by {@link #controllerModels()} — the model names survived the r8 rewire.
     * Task tank-render-tint: the p12 logistics row joined the two-layer borrow, so the
     * former single-layer exemption folded into the census (16 two-layer models).
     */
    @Test
    public void barrelModelsJoinTheCutoutCensus() throws Exception {
        List<String> tUniverse = new ArrayList<>(List.of("barrel_wood", "barrel_plastic", "barrel_metal", "barrel_logistics"));
        for (var tRow : gregtech6.registry.GTBarrels.HIGH_TIER_METAL_DRUMS) tUniverse.add(tRow.path());
        org.junit.jupiter.api.Assertions.assertEquals(16, tUniverse.size(),
                "the two-layer barrel census stays 16 (4 rows + 12 high-tier drums)");
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
    }
}
