/**
 * task overlay-dedupe-block-leg — the BLOCK-domain overlay content-group alias table
 * (the second batch of task overlay-texture-dedupe, same method as
 * {@link GT6ItemModels#OVERLAY_CANONICAL}).
 *
 * <p>The {@code *overlay*.png} census over {@code textures/block/} collapsed 2057 files
 * into 550 content groups: every multi-member group now ships ONCE under
 * {@code block/overlay/} under the flattened name of its lexicographically-first
 * member (byte-identical to one former member), and every fully-transparent group
 * (pixel-empty passes, byte-different encodings) resolves onto the batch-1
 * {@code item/overlay/empty} canonical. Datagen and the one runtime sprite seam
 * ({@code GTOvenOverlayModel.spriteOf}) resolve former paths through
 * {@link #resolve}.</p>
 *
 * <p>NEVER-MERGE exclusions (semantic slots, byte-identical by upstream copying but
 * pinned distinct by identity): everything under {@code block/materialicons/} —
 * the 22 SETs x {ore, ore_small, ore_overlay, ore_small_overlay} atlas sources are
 * pinned as 88 DISTINCT sprite paths by GT6OreCensusTest, and the pipe_side /
 * block_plate_gem arts are the same per-material art-slot class.</p>
 *
 * <p>Row form: {@code <canonical> <member> <member> ...} (texture keys relative to
 * {@code assets/gt6/textures/}, no {@code .png}; a trailing {@code ""} closes the row's
 * varargs concat).</p>
 */
package gregtech6.datagen;

import java.util.HashMap;
import java.util.Map;

public final class GT6BlockOverlayAliases {

    private static final String[] CONTENT_GROUPS = {
    "block/overlay/advanced_overlay_back " + "block/advanced_overlay_back " +
        "block/advanced_overlay_front " +
        "",
    "block/overlay/autoclave_overlay_back " + "block/autoclave_overlay_back " +
        "block/autoclave_overlay_back_active " + "block/autoclave_overlay_back_running " +
        "block/bath_overlay_bottom " + "block/bath_overlay_bottom_active " +
        "block/bath_overlay_bottom_running " + "block/burnmixer_overlay_back " +
        "block/burnmixer_overlay_back_active " + "block/burnmixer_overlay_back_running " +
        "block/canner_overlay_bottom " + "block/canner_overlay_bottom_active " +
        "block/canner_overlay_bottom_running " + "block/cryomixer_overlay_back " +
        "block/cryomixer_overlay_back_active " + "block/cryomixer_overlay_back_running " +
        "block/distillery_overlay_back " + "block/distillery_overlay_back_active " +
        "block/distillery_overlay_back_running " + "block/dryer_overlay_top " +
        "block/dryer_overlay_top_active " + "block/dryer_overlay_top_running " +
        "block/electricmixer_overlay_back " + "block/electricmixer_overlay_back_active " +
        "block/electricmixer_overlay_back_running " + "block/fermenter_overlay_top " +
        "block/fermenter_overlay_top_active " + "block/fermenter_overlay_top_running " +
        "block/freezer_overlay_bottom " + "block/freezer_overlay_bottom_active " +
        "block/freezer_overlay_bottom_running " + "block/generifier_overlay_bottom " +
        "block/generifier_overlay_bottom_active " + "block/generifier_overlay_bottom_running " +
        "block/lightning_overlay_bottom " + "block/lightning_overlay_bottom_active " +
        "block/lightning_overlay_bottom_running " + "block/massfab_overlay_bottom " +
        "block/massfab_overlay_bottom_active " + "block/massfab_overlay_bottom_running " +
        "block/mixer_overlay_back " + "block/mixer_overlay_back_active " + "block/mixer_overlay_back_running " +
        "block/replicator_overlay_bottom " + "block/replicator_overlay_bottom_active " +
        "block/replicator_overlay_bottom_running " + "block/roaster_overlay_top " +
        "block/roaster_overlay_top_active " + "block/roaster_overlay_top_running " +
        "block/sluice_overlay_bottom " + "block/sluice_overlay_bottom_active " +
        "block/sluice_overlay_bottom_running " +
        "",
    "block/overlay/autoclave_overlay_bottom " + "block/autoclave_overlay_bottom " +
        "block/autoclave_overlay_bottom_active " + "block/autoclave_overlay_bottom_running " +
        "block/bath_overlay_top " + "block/bath_overlay_top_active " + "block/bath_overlay_top_running " +
        "block/burnmixer_overlay_top " + "block/burnmixer_overlay_top_active " +
        "block/burnmixer_overlay_top_running " + "block/canner_overlay_top " +
        "block/canner_overlay_top_active " + "block/canner_overlay_top_running " +
        "block/cryomixer_overlay_top " + "block/cryomixer_overlay_top_active " +
        "block/cryomixer_overlay_top_running " + "block/distillery_overlay_top " +
        "block/distillery_overlay_top_active " + "block/distillery_overlay_top_running " +
        "block/dryer_overlay_back " + "block/dryer_overlay_back_active " + "block/dryer_overlay_back_running " +
        "block/electricmixer_overlay_top " + "block/electricmixer_overlay_top_active " +
        "block/electricmixer_overlay_top_running " + "block/fermenter_overlay_back " +
        "block/fermenter_overlay_back_active " + "block/fermenter_overlay_back_running " +
        "block/freezer_overlay_top " + "block/freezer_overlay_top_active " +
        "block/freezer_overlay_top_running " + "block/generifier_overlay_top " +
        "block/generifier_overlay_top_active " + "block/generifier_overlay_top_running " +
        "block/lightning_overlay_top " + "block/lightning_overlay_top_active " +
        "block/lightning_overlay_top_running " + "block/massfab_overlay_top " +
        "block/massfab_overlay_top_active " + "block/massfab_overlay_top_running " +
        "block/mixer_overlay_top " + "block/mixer_overlay_top_active " + "block/mixer_overlay_top_running " +
        "block/replicator_overlay_top " + "block/replicator_overlay_top_active " +
        "block/replicator_overlay_top_running " + "block/roaster_overlay_back " +
        "block/roaster_overlay_back_active " + "block/roaster_overlay_back_running " +
        "block/sluice_overlay_top " + "block/sluice_overlay_top_active " + "block/sluice_overlay_top_running " +
        "",
    "block/overlay/autoclave_overlay_front " + "block/autoclave_overlay_front " +
        "block/autoclave_overlay_front_running " + "block/largeautoclave_overlay_front " +
        "block/largeautoclave_overlay_front_running " +
        "",
    "block/overlay/autoclave_overlay_front_active " + "block/autoclave_overlay_front_active " +
        "block/largeautoclave_overlay_front_active " +
        "",
    "block/overlay/autoclave_overlay_left " + "block/autoclave_overlay_left " +
        "block/autoclave_overlay_left_active " + "block/autoclave_overlay_left_running " +
        "block/autocrafter_overlay_left " + "block/autocrafter_overlay_left_active " +
        "block/autocrafter_overlay_left_running " + "block/bath_overlay_left " +
        "block/bath_overlay_left_active " + "block/bath_overlay_left_running " +
        "block/boxinator_overlay_left " + "block/boxinator_overlay_left_active " +
        "block/boxinator_overlay_left_running " + "block/burnmixer_overlay_left " +
        "block/burnmixer_overlay_left_active " + "block/burnmixer_overlay_left_running " +
        "block/buzzsaw_overlay_left " + "block/buzzsaw_overlay_left_active " +
        "block/buzzsaw_overlay_left_running " + "block/canner_overlay_left " +
        "block/canner_overlay_left_active " + "block/canner_overlay_left_running " +
        "block/catalyticcracker_overlay_top " + "block/catalyticcracker_overlay_top_active " +
        "block/catalyticcracker_overlay_top_running " + "block/centrifuge_overlay_top " +
        "block/centrifuge_overlay_top_active " + "block/centrifuge_overlay_top_running " +
        "block/clustermill_overlay_left " + "block/clustermill_overlay_left_active " +
        "block/clustermill_overlay_left_running " + "block/compressor_overlay_top " +
        "block/compressor_overlay_top_active " + "block/compressor_overlay_top_running " +
        "block/crusher_overlay_top " + "block/crusher_overlay_top_active " +
        "block/crusher_overlay_top_running " + "block/cryomixer_overlay_left " +
        "block/cryomixer_overlay_left_active " + "block/cryomixer_overlay_left_running " +
        "block/debarker_overlay_left " + "block/debarker_overlay_left_active " +
        "block/debarker_overlay_left_running " + "block/distillery_overlay_left " +
        "block/distillery_overlay_left_active " + "block/distillery_overlay_left_running " +
        "block/dryer_overlay_left " + "block/dryer_overlay_left_active " + "block/dryer_overlay_left_running " +
        "block/electricloom_overlay_top " + "block/electricloom_overlay_top_active " +
        "block/electricloom_overlay_top_running " + "block/electricmixer_overlay_left " +
        "block/electricmixer_overlay_left_active " + "block/electricmixer_overlay_left_running " +
        "block/electricsifter_overlay_top " + "block/electricsifter_overlay_top_active " +
        "block/electricsifter_overlay_top_running " + "block/extruder_overlay_left " +
        "block/extruder_overlay_left_active " + "block/extruder_overlay_left_running " +
        "block/fermenter_overlay_left " + "block/fermenter_overlay_left_active " +
        "block/fermenter_overlay_left_running " + "block/freezer_overlay_left " +
        "block/freezer_overlay_left_active " + "block/freezer_overlay_left_running " +
        "block/generifier_overlay_left " + "block/generifier_overlay_left_active " +
        "block/generifier_overlay_left_running " + "block/laminator_overlay_left " +
        "block/laminator_overlay_left_active " + "block/laminator_overlay_left_running " +
        "block/laserengraver_overlay_left " + "block/laserengraver_overlay_left_active " +
        "block/laserengraver_overlay_left_running " + "block/laserwelder_overlay_left " +
        "block/laserwelder_overlay_left_active " + "block/laserwelder_overlay_left_running " +
        "block/lathe_overlay_left " + "block/lathe_overlay_left_active " + "block/lathe_overlay_left_running " +
        "block/lightning_overlay_left " + "block/lightning_overlay_left_active " +
        "block/lightning_overlay_left_running " + "block/loom_overlay_top " + "block/loom_overlay_top_active " +
        "block/loom_overlay_top_running " + "block/magneticseparator_overlay_left " +
        "block/magneticseparator_overlay_left_active " + "block/magneticseparator_overlay_left_running " +
        "block/massfab_overlay_left " + "block/massfab_overlay_left_active " +
        "block/massfab_overlay_left_running " + "block/melter_overlay_top " +
        "block/melter_overlay_top_active " + "block/melter_overlay_top_running " + "block/mixer_overlay_left " +
        "block/mixer_overlay_left_active " + "block/mixer_overlay_left_running " +
        "block/oven_overlay_active_left " + "block/oven_overlay_left " + "block/oven_overlay_left_active " +
        "block/oven_overlay_left_running " + "block/oven_overlay_running_left " +
        "block/polarizer_overlay_left " + "block/polarizer_overlay_left_active " +
        "block/polarizer_overlay_left_running " + "block/press_overlay_left " +
        "block/press_overlay_left_active " + "block/press_overlay_left_running " +
        "block/replicator_overlay_left " + "block/replicator_overlay_left_active " +
        "block/replicator_overlay_left_running " + "block/roaster_overlay_left " +
        "block/roaster_overlay_left_active " + "block/roaster_overlay_left_running " +
        "block/rollbender_overlay_left " + "block/rollbender_overlay_left_active " +
        "block/rollbender_overlay_left_running " + "block/rollformer_overlay_left " +
        "block/rollformer_overlay_left_active " + "block/rollformer_overlay_left_running " +
        "block/rollingmill_overlay_left " + "block/rollingmill_overlay_left_active " +
        "block/rollingmill_overlay_left_running " + "block/sander_overlay_left " +
        "block/sander_overlay_left_active " + "block/sander_overlay_left_running " +
        "block/scannermolecular_overlay_left " + "block/scannermolecular_overlay_left_active " +
        "block/scannermolecular_overlay_left_running " + "block/shredder_overlay_top " +
        "block/shredder_overlay_top_active " + "block/shredder_overlay_top_running " +
        "block/sifter_overlay_top " + "block/sifter_overlay_top_active " + "block/sifter_overlay_top_running " +
        "block/sluice_overlay_left " + "block/sluice_overlay_left_active " +
        "block/sluice_overlay_left_running " + "block/smelter_overlay_top " +
        "block/smelter_overlay_top_active " + "block/smelter_overlay_top_running " +
        "block/squeezer_overlay_left " + "block/squeezer_overlay_left_active " +
        "block/squeezer_overlay_left_running " + "block/steamcracker_overlay_top " +
        "block/steamcracker_overlay_top_active " + "block/steamcracker_overlay_top_running " +
        "block/unboxinator_overlay_left " + "block/unboxinator_overlay_left_active " +
        "block/unboxinator_overlay_left_running " + "block/wiremill_overlay_left " +
        "block/wiremill_overlay_left_active " + "block/wiremill_overlay_left_running " +
        "",
    "block/overlay/autoclave_overlay_right " + "block/autoclave_overlay_right " +
        "block/autoclave_overlay_right_active " + "block/autoclave_overlay_right_running " +
        "block/autocrafter_overlay_right " + "block/autocrafter_overlay_right_active " +
        "block/autocrafter_overlay_right_running " + "block/bath_overlay_right " +
        "block/bath_overlay_right_active " + "block/bath_overlay_right_running " +
        "block/boxinator_overlay_right " + "block/boxinator_overlay_right_active " +
        "block/boxinator_overlay_right_running " + "block/burnmixer_overlay_right " +
        "block/burnmixer_overlay_right_active " + "block/burnmixer_overlay_right_running " +
        "block/buzzsaw_overlay_right " + "block/buzzsaw_overlay_right_active " +
        "block/buzzsaw_overlay_right_running " + "block/canner_overlay_right " +
        "block/canner_overlay_right_active " + "block/canner_overlay_right_running " +
        "block/catalyticcracker_overlay_back " + "block/catalyticcracker_overlay_back_active " +
        "block/catalyticcracker_overlay_back_running " + "block/centrifuge_overlay_right " +
        "block/centrifuge_overlay_right_active " + "block/centrifuge_overlay_right_running " +
        "block/clustermill_overlay_right " + "block/clustermill_overlay_right_active " +
        "block/clustermill_overlay_right_running " + "block/coagulator_overlay_bottom " +
        "block/coagulator_overlay_bottom_active " + "block/coagulator_overlay_bottom_running " +
        "block/compressor_overlay_bottom " + "block/compressor_overlay_bottom_active " +
        "block/compressor_overlay_bottom_running " + "block/crusher_overlay_bottom " +
        "block/crusher_overlay_bottom_active " + "block/crusher_overlay_bottom_running " +
        "block/cryomixer_overlay_right " + "block/cryomixer_overlay_right_active " +
        "block/cryomixer_overlay_right_running " + "block/debarker_overlay_right " +
        "block/debarker_overlay_right_active " + "block/debarker_overlay_right_running " +
        "block/distillery_overlay_right " + "block/distillery_overlay_right_active " +
        "block/distillery_overlay_right_running " + "block/dryer_overlay_right " +
        "block/dryer_overlay_right_active " + "block/dryer_overlay_right_running " +
        "block/electricloom_overlay_bottom " + "block/electricloom_overlay_bottom_active " +
        "block/electricloom_overlay_bottom_running " + "block/electricmixer_overlay_right " +
        "block/electricmixer_overlay_right_active " + "block/electricmixer_overlay_right_running " +
        "block/electricsifter_overlay_bottom " + "block/electricsifter_overlay_bottom_active " +
        "block/electricsifter_overlay_bottom_running " + "block/extruder_overlay_right " +
        "block/extruder_overlay_right_active " + "block/extruder_overlay_right_running " +
        "block/fermenter_overlay_right " + "block/fermenter_overlay_right_active " +
        "block/fermenter_overlay_right_running " + "block/freezer_overlay_right " +
        "block/freezer_overlay_right_active " + "block/freezer_overlay_right_running " +
        "block/generifier_overlay_right " + "block/generifier_overlay_right_active " +
        "block/generifier_overlay_right_running " + "block/laminator_overlay_right " +
        "block/laminator_overlay_right_active " + "block/laminator_overlay_right_running " +
        "block/laserengraver_overlay_right " + "block/laserengraver_overlay_right_active " +
        "block/laserengraver_overlay_right_running " + "block/laserwelder_overlay_right " +
        "block/laserwelder_overlay_right_active " + "block/laserwelder_overlay_right_running " +
        "block/lathe_overlay_right " + "block/lathe_overlay_right_active " +
        "block/lathe_overlay_right_running " + "block/lightning_overlay_right " +
        "block/lightning_overlay_right_active " + "block/lightning_overlay_right_running " +
        "block/loom_overlay_bottom " + "block/loom_overlay_bottom_active " +
        "block/loom_overlay_bottom_running " + "block/magneticseparator_overlay_right " +
        "block/magneticseparator_overlay_right_active " + "block/magneticseparator_overlay_right_running " +
        "block/massfab_overlay_right " + "block/massfab_overlay_right_active " +
        "block/massfab_overlay_right_running " + "block/massstorage_standard/overlay_bottom " +
        "block/melter_overlay_left " + "block/melter_overlay_left_active " +
        "block/melter_overlay_left_running " + "block/mixer_overlay_right " +
        "block/mixer_overlay_right_active " + "block/mixer_overlay_right_running " +
        "block/oven_overlay_active_right " + "block/oven_overlay_right " + "block/oven_overlay_right_active " +
        "block/oven_overlay_right_running " + "block/oven_overlay_running_right " +
        "block/polarizer_overlay_right " + "block/polarizer_overlay_right_active " +
        "block/polarizer_overlay_right_running " + "block/press_overlay_right " +
        "block/press_overlay_right_active " + "block/press_overlay_right_running " +
        "block/replicator_overlay_right " + "block/replicator_overlay_right_active " +
        "block/replicator_overlay_right_running " + "block/roaster_overlay_right " +
        "block/roaster_overlay_right_active " + "block/roaster_overlay_right_running " +
        "block/rollbender_overlay_right " + "block/rollbender_overlay_right_active " +
        "block/rollbender_overlay_right_running " + "block/rollformer_overlay_right " +
        "block/rollformer_overlay_right_active " + "block/rollformer_overlay_right_running " +
        "block/rollingmill_overlay_right " + "block/rollingmill_overlay_right_active " +
        "block/rollingmill_overlay_right_running " + "block/sander_overlay_right " +
        "block/sander_overlay_right_active " + "block/sander_overlay_right_running " +
        "block/scannermolecular_overlay_right " + "block/scannermolecular_overlay_right_active " +
        "block/scannermolecular_overlay_right_running " + "block/shredder_overlay_bottom " +
        "block/shredder_overlay_bottom_active " + "block/shredder_overlay_bottom_running " +
        "block/sifter_overlay_bottom " + "block/sifter_overlay_bottom_active " +
        "block/sifter_overlay_bottom_running " + "block/sluice_overlay_right " +
        "block/sluice_overlay_right_active " + "block/sluice_overlay_right_running " +
        "block/smelter_overlay_left " + "block/smelter_overlay_left_active " +
        "block/smelter_overlay_left_running " + "block/squeezer_overlay_right " +
        "block/squeezer_overlay_right_active " + "block/squeezer_overlay_right_running " +
        "block/steamcracker_overlay_back " + "block/steamcracker_overlay_back_active " +
        "block/steamcracker_overlay_back_running " + "block/unboxinator_overlay_right " +
        "block/unboxinator_overlay_right_active " + "block/unboxinator_overlay_right_running " +
        "block/wiremill_overlay_right " + "block/wiremill_overlay_right_active " +
        "block/wiremill_overlay_right_running " +
        "",
    "block/overlay/autocrafter_overlay_bottom " + "block/autocrafter_overlay_bottom " +
        "block/autocrafter_overlay_top " + "block/boxinator_overlay_bottom " + "block/canner_overlay_back " +
        "block/electricloom_overlay_left " + "block/electricloom_overlay_right " +
        "block/electricmixer_overlay_bottom " + "block/electricsifter_overlay_back " +
        "block/lightning_overlay_back " + "block/unboxinator_overlay_bottom " +
        "",
    "block/overlay/autocrafter_overlay_bottom_active " + "block/autocrafter_overlay_bottom_active " +
        "block/autocrafter_overlay_bottom_running " + "block/autocrafter_overlay_top_active " +
        "block/autocrafter_overlay_top_running " + "block/battery_box/overlay_active_front " +
        "block/boxinator_overlay_bottom_active " + "block/boxinator_overlay_bottom_running " +
        "block/canner_overlay_back_active " + "block/canner_overlay_back_running " +
        "block/electric_transformer_overlay_active_back " + "block/electricloom_overlay_left_active " +
        "block/electricloom_overlay_left_running " + "block/electricloom_overlay_right_active " +
        "block/electricloom_overlay_right_running " + "block/electricmixer_overlay_bottom_active " +
        "block/electricmixer_overlay_bottom_running " + "block/electricsifter_overlay_back_active " +
        "block/electricsifter_overlay_back_running " + "block/lightning_overlay_back_active " +
        "block/lightning_overlay_back_running " + "block/unboxinator_overlay_bottom_active " +
        "block/unboxinator_overlay_bottom_running " + "block/zpm_decharger/overlay_active_front " +
        "",
    "block/overlay/barrel_parts_logistics_overlay_bottom " +
        "block/barrel_parts/logistics/overlay_bottom " + "block/barrel_parts/logistics/overlay_top " +
        "",
    "block/overlay/bath_overlay_back " + "block/bath_overlay_back " + "block/bath_overlay_back_running " +
        "block/bath_overlay_front " + "block/bath_overlay_front_running " +
        "",
    "block/overlay/bath_overlay_back_active " + "block/bath_overlay_back_active " +
        "block/bath_overlay_front_active " +
        "",
    "block/overlay/battery_box_large_overlay_active_front " +
        "block/battery_box_large/overlay_active_front " +
        "block/electric_transformer_overlay_active_front " +
        "block/long_distance_transformer/overlay_active_front " +
        "",
    "block/overlay/battery_box_large_overlay_front " + "block/battery_box_large/overlay_front " +
        "block/electric_transformer_overlay_front " + "block/long_distance_transformer/overlay_front " +
        "",
    "block/overlay/battery_box_overlay_front " + "block/battery_box/overlay_front " +
        "block/electric_transformer_overlay_back " + "block/zpm_decharger/overlay_front " +
        "",
    "block/overlay/boiler_steam_overlay_top " + "block/boiler_steam/overlay_top " +
        "block/steam_engine_overlay_back " + "block/steam_turbine_overlay_back " +
        "",
    "block/overlay/bridge_motor_overlay_front " + "block/bridge_motor_overlay_front " +
        "block/diesel_engine_overlay_front " +
        "",
    "block/overlay/bumblelyzer_overlay_back_active " + "block/bumblelyzer_overlay_back_active " +
        "block/bumblelyzer_overlay_back_running " +
        "",
    "block/overlay/bumblelyzer_overlay_bottom " + "block/bumblelyzer_overlay_bottom " +
        "block/bumblelyzer_overlay_bottom_active " + "block/bumblelyzer_overlay_bottom_running " +
        "",
    "block/overlay/bumblelyzer_overlay_left " + "block/bumblelyzer_overlay_left " +
        "block/bumblelyzer_overlay_left_active " + "block/bumblelyzer_overlay_left_running " +
        "",
    "block/overlay/bumblelyzer_overlay_right " + "block/bumblelyzer_overlay_right " +
        "block/bumblelyzer_overlay_right_active " + "block/bumblelyzer_overlay_right_running " +
        "",
    "block/overlay/bumblelyzer_overlay_top " + "block/bumblelyzer_overlay_top " +
        "block/bumblelyzer_overlay_top_active " + "block/bumblelyzer_overlay_top_running " +
        "",
    "block/overlay/burning_box_fluidbed_overlay_back " + "block/burning_box_fluidbed_overlay_back " +
        "block/burning_box_fluidbed_overlay_back_active " + "block/burning_box_gas_overlay_back " +
        "block/burning_box_gas_overlay_back_active " + "block/burning_box_liquid_overlay_back " +
        "block/burning_box_liquid_overlay_back_active " + "block/burning_box_solid_overlay_back " +
        "block/burning_box_solid_overlay_back_active " +
        "",
    "block/overlay/burning_box_fluidbed_overlay_bottom " + "block/burning_box_fluidbed_overlay_bottom " +
        "block/burning_box_fluidbed_overlay_bottom_active " + "block/burning_box_gas_overlay_bottom " +
        "block/burning_box_gas_overlay_bottom_active " + "block/burning_box_liquid_overlay_bottom " +
        "block/burning_box_liquid_overlay_bottom_active " + "block/burning_box_solid_overlay_bottom " +
        "block/burning_box_solid_overlay_bottom_active " +
        "",
    "block/overlay/burning_box_fluidbed_overlay_left " + "block/burning_box_fluidbed_overlay_left " +
        "block/burning_box_gas_overlay_left " + "block/burning_box_liquid_overlay_left " +
        "block/burning_box_solid_overlay_left " +
        "",
    "block/overlay/burning_box_fluidbed_overlay_left_active " +
        "block/burning_box_fluidbed_overlay_left_active " + "block/burning_box_gas_overlay_left_active " +
        "block/burning_box_liquid_overlay_left_active " + "block/burning_box_solid_overlay_left_active " +
        "",
    "block/overlay/burning_box_fluidbed_overlay_right " + "block/burning_box_fluidbed_overlay_right " +
        "block/burning_box_gas_overlay_right " + "block/burning_box_liquid_overlay_right " +
        "block/burning_box_solid_overlay_right " +
        "",
    "block/overlay/burning_box_fluidbed_overlay_right_active " +
        "block/burning_box_fluidbed_overlay_right_active " + "block/burning_box_gas_overlay_right_active " +
        "block/burning_box_liquid_overlay_right_active " + "block/burning_box_solid_overlay_right_active " +
        "",
    "block/overlay/burning_box_fluidbed_overlay_top " + "block/burning_box_fluidbed_overlay_top " +
        "block/burning_box_gas_overlay_top " + "block/burning_box_liquid_overlay_top " +
        "block/burning_box_solid_overlay_top " + "block/catalyticcracker_overlay_bottom " +
        "block/catalyticcracker_overlay_bottom_active " + "block/catalyticcracker_overlay_bottom_running " +
        "block/distillery_overlay_bottom " + "block/dryer_overlay_bottom " + "block/extruder_overlay_bottom " +
        "block/fermenter_overlay_bottom " + "block/laminator_overlay_bottom " + "block/melter_overlay_bottom " +
        "block/oven_overlay_bottom " + "block/roaster_overlay_bottom " + "block/smelter_overlay_bottom " +
        "block/steamcracker_overlay_bottom " + "block/steamcracker_overlay_bottom_active " +
        "block/steamcracker_overlay_bottom_running " +
        "",
    "block/overlay/burning_box_fluidbed_overlay_top_active " +
        "block/burning_box_fluidbed_overlay_top_active " + "block/burning_box_gas_overlay_top_active " +
        "block/burning_box_liquid_overlay_top_active " + "block/burning_box_solid_overlay_top_active " +
        "block/distillery_overlay_bottom_active " + "block/distillery_overlay_bottom_running " +
        "block/dryer_overlay_bottom_active " + "block/dryer_overlay_bottom_running " +
        "block/extruder_overlay_bottom_active " + "block/extruder_overlay_bottom_running " +
        "block/fermenter_overlay_bottom_active " + "block/fermenter_overlay_bottom_running " +
        "block/laminator_overlay_bottom_active " + "block/laminator_overlay_bottom_running " +
        "block/melter_overlay_bottom_active " + "block/melter_overlay_bottom_running " +
        "block/oven_overlay_active_bottom " + "block/oven_overlay_bottom_active " +
        "block/oven_overlay_bottom_running " + "block/oven_overlay_running_bottom " +
        "block/roaster_overlay_bottom_active " + "block/roaster_overlay_bottom_running " +
        "block/smelter_overlay_bottom_active " + "block/smelter_overlay_bottom_running " +
        "",
    "block/overlay/burnmixer_overlay_bottom " + "block/burnmixer_overlay_bottom " +
        "block/burnmixer_overlay_bottom_active " + "block/burnmixer_overlay_bottom_running " +
        "block/buzzsaw_overlay_back " + "block/buzzsaw_overlay_back_active " +
        "block/buzzsaw_overlay_back_running " + "block/centrifuge_overlay_bottom " +
        "block/centrifuge_overlay_bottom_active " + "block/centrifuge_overlay_bottom_running " +
        "block/clustermill_overlay_back " + "block/clustermill_overlay_back_active " +
        "block/clustermill_overlay_back_running " + "block/debarker_overlay_back " +
        "block/debarker_overlay_back_active " + "block/debarker_overlay_back_running " +
        "block/electric_dynamo_overlay_active_back " + "block/electric_dynamo_overlay_back " +
        "block/flux_dynamo_overlay_active_back " + "block/flux_dynamo_overlay_back " +
        "block/lathe_overlay_bottom " + "block/lathe_overlay_bottom_active " +
        "block/lathe_overlay_bottom_running " + "block/loom_overlay_left " + "block/loom_overlay_left_active " +
        "block/loom_overlay_left_running " + "block/loom_overlay_right " + "block/loom_overlay_right_active " +
        "block/loom_overlay_right_running " + "block/mixer_overlay_bottom " +
        "block/mixer_overlay_bottom_active " + "block/mixer_overlay_bottom_running " +
        "block/rollbender_overlay_back " + "block/rollbender_overlay_back_active " +
        "block/rollbender_overlay_back_running " + "block/rollformer_overlay_back " +
        "block/rollformer_overlay_back_active " + "block/rollformer_overlay_back_running " +
        "block/rollingmill_overlay_back " + "block/rollingmill_overlay_back_active " +
        "block/rollingmill_overlay_back_running " + "block/rotation_engine_overlay_side " +
        "block/sander_overlay_top " + "block/sander_overlay_top_active " + "block/sander_overlay_top_running " +
        "block/shredder_overlay_left " + "block/shredder_overlay_left_active " +
        "block/shredder_overlay_left_running " + "block/shredder_overlay_right " +
        "block/shredder_overlay_right_active " + "block/shredder_overlay_right_running " +
        "block/sluice_overlay_back " + "block/sluice_overlay_back_active " +
        "block/sluice_overlay_back_running " + "block/turbine_mains/largedynamo/overlay_front/bottom " +
        "block/turbine_mains/largedynamo/overlay_front/side " +
        "block/turbine_mains/largedynamo/overlay_front/top " + "block/wiremill_overlay_back " +
        "block/wiremill_overlay_back_active " + "block/wiremill_overlay_back_running " +
        "",
    "block/overlay/buzzsaw_overlay_bottom " + "block/buzzsaw_overlay_bottom " +
        "block/buzzsaw_overlay_bottom_active " + "block/buzzsaw_overlay_bottom_running " +
        "block/catalyticcracker_overlay_left " + "block/catalyticcracker_overlay_left_active " +
        "block/catalyticcracker_overlay_left_running " + "block/coagulator_overlay_top " +
        "block/coagulator_overlay_top_active " + "block/coagulator_overlay_top_running " +
        "block/debarker_overlay_top " + "block/debarker_overlay_top_active " +
        "block/debarker_overlay_top_running " + "block/steamcracker_overlay_left " +
        "block/steamcracker_overlay_left_active " + "block/steamcracker_overlay_left_running " +
        "",
    "block/overlay/catalyticcracker_overlay_right " + "block/catalyticcracker_overlay_right " +
        "block/catalyticcracker_overlay_right_active " + "block/catalyticcracker_overlay_right_running " +
        "block/centrifuge_overlay_left " + "block/centrifuge_overlay_left_active " +
        "block/centrifuge_overlay_left_running " + "block/magneticseparator_overlay_bottom " +
        "block/magneticseparator_overlay_bottom_active " +
        "block/magneticseparator_overlay_bottom_running " + "block/melter_overlay_right " +
        "block/melter_overlay_right_active " + "block/melter_overlay_right_running " +
        "block/smelter_overlay_right " + "block/smelter_overlay_right_active " +
        "block/smelter_overlay_right_running " + "block/squeezer_overlay_bottom " +
        "block/squeezer_overlay_bottom_active " + "block/squeezer_overlay_bottom_running " +
        "block/steamcracker_overlay_right " + "block/steamcracker_overlay_right_active " +
        "block/steamcracker_overlay_right_running " +
        "",
    "block/overlay/centrifuge_overlay_back " + "block/centrifuge_overlay_back " +
        "block/centrifuge_overlay_front " +
        "",
    "block/overlay/centrifuge_overlay_back_active " + "block/centrifuge_overlay_back_active " +
        "block/centrifuge_overlay_back_running " + "block/centrifuge_overlay_front_active " +
        "block/centrifuge_overlay_front_running " +
        "",
    "block/overlay/charging_locker_overlay_front " + "block/charging_locker/overlay_front " +
        "block/locker/overlay_front " +
        "",
    "block/overlay/coagulator_overlay_front " + "block/coagulator_overlay_front " +
        "block/coagulator_overlay_front_running " +
        "",
    "block/overlay/compressor_overlay_left " + "block/compressor_overlay_left " +
        "block/compressor_overlay_left_active " + "block/compressor_overlay_left_running " +
        "block/crusher_overlay_back " + "block/crusher_overlay_back_active " +
        "block/crusher_overlay_back_running " + "block/press_overlay_top " + "block/press_overlay_top_active " +
        "block/press_overlay_top_running " + "block/sifter_overlay_back " +
        "block/sifter_overlay_back_active " + "block/sifter_overlay_back_running " +
        "block/squeezer_overlay_top " + "block/squeezer_overlay_top_active " +
        "block/squeezer_overlay_top_running " +
        "",
    "block/overlay/cryodistillationtower_overlay_back " + "block/cryodistillationtower_overlay_back " +
        "block/cryodistillationtower_overlay_back_active " +
        "block/cryodistillationtower_overlay_back_running " + "block/cryodistillationtower_overlay_left " +
        "block/cryodistillationtower_overlay_left_active " +
        "block/cryodistillationtower_overlay_left_running " + "block/cryodistillationtower_overlay_right " +
        "block/cryodistillationtower_overlay_right_active " +
        "block/cryodistillationtower_overlay_right_running " + "block/distillationtower_overlay_back " +
        "block/distillationtower_overlay_back_active " + "block/distillationtower_overlay_back_running " +
        "block/distillationtower_overlay_left " + "block/distillationtower_overlay_left_active " +
        "block/distillationtower_overlay_left_running " + "block/distillationtower_overlay_right " +
        "block/distillationtower_overlay_right_active " + "block/distillationtower_overlay_right_running " +
        "block/parts/distillationtowerparts/0/overlay/side " +
        "",
    "block/overlay/cryodistillationtower_overlay_bottom " +
        "block/cryodistillationtower_overlay_bottom " +
        "block/cryodistillationtower_overlay_bottom_active " +
        "block/cryodistillationtower_overlay_bottom_running " + "block/distillationtower_overlay_bottom " +
        "block/distillationtower_overlay_bottom_active " +
        "block/distillationtower_overlay_bottom_running " +
        "block/parts/distillationtowerparts/0/overlay/bottom " +
        "block/parts/distillationtowerparts/1/overlay/bottom " +
        "",
    "block/overlay/cryodistillationtower_overlay_front " + "block/cryodistillationtower_overlay_front " +
        "block/distillationtower_overlay_front " +
        "",
    "block/overlay/cryodistillationtower_overlay_front_active " +
        "block/cryodistillationtower_overlay_front_active " +
        "block/distillationtower_overlay_front_active " +
        "",
    "block/overlay/cryodistillationtower_overlay_front_running " +
        "block/cryodistillationtower_overlay_front_running " +
        "block/distillationtower_overlay_front_running " +
        "",
    "block/overlay/cryodistillationtower_overlay_top " + "block/cryodistillationtower_overlay_top " +
        "block/cryodistillationtower_overlay_top_active " +
        "block/cryodistillationtower_overlay_top_running " + "block/distillationtower_overlay_top " +
        "block/distillationtower_overlay_top_active " + "block/distillationtower_overlay_top_running " +
        "block/parts/distillationtowerparts/0/overlay/top " +
        "block/parts/distillationtowerparts/1/overlay/top " +
        "",
    "block/overlay/cryomixer_overlay_front " + "block/cryomixer_overlay_front " +
        "block/electricmixer_overlay_front " + "block/mixer_overlay_front " +
        "",
    "block/overlay/crystal_charger_overlay_active_front " +
        "block/crystal_charger/overlay_active_front " + "block/crystal_charger_large/overlay_active_front " +
        "block/laser_electric_overlay_active_front " +
        "",
    "block/overlay/crystal_charger_overlay_front " + "block/crystal_charger/overlay_front " +
        "block/crystal_charger_large/overlay_front " + "block/laser_electric_overlay_front " +
        "",
    "block/overlay/crystallisationcrucible_overlay_back " +
        "block/crystallisationcrucible_overlay_back " +
        "block/crystallisationcrucible_overlay_back_active " +
        "block/crystallisationcrucible_overlay_back_running " +
        "block/crystallisationcrucible_overlay_left " +
        "block/crystallisationcrucible_overlay_left_active " +
        "block/crystallisationcrucible_overlay_left_running " +
        "",
    "block/overlay/crystallisationcrucible_overlay_bottom_active " +
        "block/crystallisationcrucible_overlay_bottom_active " +
        "block/crystallisationcrucible_overlay_bottom_running " +
        "",
    "block/overlay/crystallisationcrucible_overlay_front " +
        "block/crystallisationcrucible_overlay_front " +
        "block/crystallisationcrucible_overlay_front_running " +
        "",
    "block/overlay/crystallisationcrucible_overlay_right " +
        "block/crystallisationcrucible_overlay_right " +
        "block/crystallisationcrucible_overlay_right_active " +
        "block/crystallisationcrucible_overlay_right_running " +
        "",
    "block/overlay/crystallisationcrucible_overlay_top " + "block/crystallisationcrucible_overlay_top " +
        "block/crystallisationcrucible_overlay_top_active " +
        "block/crystallisationcrucible_overlay_top_running " +
        "",
    "block/overlay/electric_dynamo_overlay_active_front " +
        "block/electric_dynamo_overlay_active_front " + "block/electric_transformer_overlay_active_side " +
        "",
    "block/overlay/electric_dynamo_overlay_active_side " + "block/electric_dynamo_overlay_active_side " +
        "block/electric_dynamo_overlay_side " +
        "",
    "block/overlay/electric_dynamo_overlay_front " + "block/electric_dynamo_overlay_front " +
        "block/electric_transformer_overlay_side " + "block/laser_absorber_overlay_front " +
        "",
    "block/overlay/electricloom_overlay_front " + "block/electricloom_overlay_front " +
        "block/loom_overlay_front " +
        "",
    "block/overlay/electricmixer_overlay_front_active " + "block/electricmixer_overlay_front_active " +
        "block/mixer_overlay_front_active " +
        "",
    "block/overlay/electricmixer_overlay_front_running " + "block/electricmixer_overlay_front_running " +
        "block/mixer_overlay_front_running " +
        "",
    "block/overlay/electricsifter_overlay_front " + "block/electricsifter_overlay_front " +
        "block/sifter_overlay_front " +
        "",
    "block/overlay/electricsifter_overlay_front_active " + "block/electricsifter_overlay_front_active " +
        "block/sifter_overlay_front_active " +
        "",
    "block/overlay/electricsifter_overlay_front_running " +
        "block/electricsifter_overlay_front_running " + "block/sifter_overlay_front_running " +
        "",
    "block/overlay/electrolyzer_overlay_back " + "block/electrolyzer_overlay_back " +
        "block/electrolyzer_overlay_front " +
        "",
    "block/overlay/electrolyzer_overlay_back_active " + "block/electrolyzer_overlay_back_active " +
        "block/electrolyzer_overlay_front_active " +
        "",
    "block/overlay/electrolyzer_overlay_back_running " + "block/electrolyzer_overlay_back_running " +
        "block/electrolyzer_overlay_front_running " +
        "",
    "block/overlay/electrolyzer_overlay_bottom " + "block/electrolyzer_overlay_bottom " +
        "block/injector_overlay_back " + "block/printer_overlay_back " + "block/scannervisuals_overlay_back " +
        "block/slicer_overlay_back " +
        "",
    "block/overlay/electrolyzer_overlay_bottom_active " + "block/electrolyzer_overlay_bottom_active " +
        "block/electrolyzer_overlay_bottom_running " + "block/injector_overlay_back_active " +
        "block/injector_overlay_back_running " + "block/printer_overlay_back_active " +
        "block/printer_overlay_back_running " + "block/scannervisuals_overlay_back_active " +
        "block/scannervisuals_overlay_back_running " + "block/slicer_overlay_back_active " +
        "block/slicer_overlay_back_running " +
        "",
    "block/overlay/electrolyzer_overlay_left " + "block/electrolyzer_overlay_left " +
        "block/electrolyzer_overlay_left_active " + "block/electrolyzer_overlay_left_running " +
        "block/injector_overlay_bottom " + "block/injector_overlay_bottom_active " +
        "block/injector_overlay_bottom_running " +
        "",
    "block/overlay/electrolyzer_overlay_right " + "block/electrolyzer_overlay_right " +
        "block/electrolyzer_overlay_right_active " + "block/electrolyzer_overlay_right_running " +
        "block/injector_overlay_right " + "block/injector_overlay_right_active " +
        "block/injector_overlay_right_running " + "block/printer_overlay_right " +
        "block/printer_overlay_right_active " + "block/printer_overlay_right_running " +
        "block/scannervisuals_overlay_right " + "block/scannervisuals_overlay_right_active " +
        "block/scannervisuals_overlay_right_running " + "block/slicer_overlay_right " +
        "block/slicer_overlay_right_active " + "block/slicer_overlay_right_running " +
        "",
    "block/overlay/electrolyzer_overlay_top " + "block/electrolyzer_overlay_top " +
        "block/electrolyzer_overlay_top_active " + "block/electrolyzer_overlay_top_running " +
        "block/injector_overlay_left " + "block/injector_overlay_left_active " +
        "block/injector_overlay_left_running " + "block/printer_overlay_left " +
        "block/printer_overlay_left_active " + "block/printer_overlay_left_running " +
        "block/scannervisuals_overlay_left " + "block/scannervisuals_overlay_left_active " +
        "block/scannervisuals_overlay_left_running " + "block/slicer_overlay_left " +
        "block/slicer_overlay_left_active " + "block/slicer_overlay_left_running " +
        "",
    "block/overlay/generifier_overlay_back " + "block/generifier_overlay_back " +
        "block/generifier_overlay_back_running " +
        "",
    "block/overlay/generifier_overlay_front " + "block/generifier_overlay_front " +
        "block/generifier_overlay_front_running " +
        "",
    "block/overlay/implosioncompressor_overlay_bottom " + "block/implosioncompressor_overlay_bottom " +
        "block/implosioncompressor_overlay_bottom_active " +
        "block/implosioncompressor_overlay_bottom_running " + "block/largeautoclave_overlay_bottom " +
        "block/largeautoclave_overlay_bottom_active " + "block/largeautoclave_overlay_bottom_running " +
        "block/largebath_overlay_bottom " + "block/largebath_overlay_bottom_active " +
        "block/largebath_overlay_bottom_running " + "block/largecoagulator_overlay_bottom " +
        "block/largecoagulator_overlay_bottom_active " + "block/largecoagulator_overlay_bottom_running " +
        "block/largecrusher_overlay_bottom " + "block/largecrusher_overlay_bottom_active " +
        "block/largecrusher_overlay_bottom_running " + "block/largeelectrolyzer_overlay_bottom " +
        "block/largeelectrolyzer_overlay_bottom_active " +
        "block/largeelectrolyzer_overlay_bottom_running " + "block/largemassfab_overlay_bottom " +
        "block/largemassfab_overlay_bottom_active " + "block/largemassfab_overlay_bottom_running " +
        "block/largemixer_overlay_bottom " + "block/largemixer_overlay_bottom_active " +
        "block/largemixer_overlay_bottom_running " + "block/largeoven_overlay_bottom " +
        "block/largeoven_overlay_bottom_active " + "block/largeoven_overlay_bottom_running " +
        "block/largeshredder_overlay_bottom " + "block/largeshredder_overlay_bottom_active " +
        "block/largeshredder_overlay_bottom_running " + "block/largesluice_overlay_bottom " +
        "block/largesluice_overlay_bottom_active " + "block/largesluice_overlay_bottom_running " +
        "block/largesqueezer_overlay_bottom " + "block/largesqueezer_overlay_bottom_active " +
        "block/largesqueezer_overlay_bottom_running " +
        "",
    "block/overlay/injector_overlay_top " + "block/injector_overlay_top " +
        "block/injector_overlay_top_active " + "block/injector_overlay_top_running " +
        "block/printer_overlay_top " + "block/printer_overlay_top_active " +
        "block/printer_overlay_top_running " +
        "",
    "block/overlay/laminator_overlay_back_active " + "block/laminator_overlay_back_active " +
        "block/laminator_overlay_back_running " + "block/laminator_overlay_front_active " +
        "block/laminator_overlay_front_running " +
        "",
    "block/overlay/large_heat_exchanger_overlay_front_bottom " +
        "block/large_heat_exchanger/overlay_front_bottom " +
        "block/tank_valves/tankmetal_overlay_front_bottom " +
        "block/tank_valves/tankwood_overlay_front_bottom " +
        "",
    "block/overlay/large_heat_exchanger_overlay_front_side " +
        "block/large_heat_exchanger/overlay_front_side " + "block/large_heat_exchanger/overlay_front_top " +
        "block/tank_valves/tankmetal_overlay_front_side " +
        "block/tank_valves/tankmetal_overlay_front_top " + "block/tank_valves/tankwood_overlay_front_top " +
        "",
    "block/overlay/largebath_overlay_front " + "block/largebath_overlay_front " +
        "block/largebath_overlay_front_running " + "block/largecoagulator_overlay_front " +
        "block/largecoagulator_overlay_front_running " +
        "",
    "block/overlay/largecentrifuge_overlay_back " + "block/largecentrifuge_overlay_back " +
        "block/largecentrifuge_overlay_back_active " + "block/largecentrifuge_overlay_back_running " +
        "",
    "block/overlay/largecentrifuge_overlay_bottom " + "block/largecentrifuge_overlay_bottom " +
        "block/largecentrifuge_overlay_bottom_active " + "block/largecentrifuge_overlay_bottom_running " +
        "",
    "block/overlay/largecentrifuge_overlay_left " + "block/largecentrifuge_overlay_left " +
        "block/largecentrifuge_overlay_left_active " + "block/largecentrifuge_overlay_left_running " +
        "",
    "block/overlay/largecentrifuge_overlay_right " + "block/largecentrifuge_overlay_right " +
        "block/largecentrifuge_overlay_right_active " + "block/largecentrifuge_overlay_right_running " +
        "",
    "block/overlay/largecentrifuge_overlay_top " + "block/largecentrifuge_overlay_top " +
        "block/largecentrifuge_overlay_top_active " + "block/largecentrifuge_overlay_top_running " +
        "",
    "block/overlay/largemassfab_overlay_front " + "block/largemassfab_overlay_front " +
        "block/massfab_overlay_front " +
        "",
    "block/overlay/largemassfab_overlay_front_active " + "block/largemassfab_overlay_front_active " +
        "block/massfab_overlay_front_active " +
        "",
    "block/overlay/largemassfab_overlay_front_running " + "block/largemassfab_overlay_front_running " +
        "block/massfab_overlay_front_running " +
        "",
    "block/overlay/laser_absorber_overlay_active_back " + "block/laser_absorber_overlay_active_back " +
        "block/laserengraver_overlay_top_active " + "block/laserwelder_overlay_top_active " +
        "block/quantum_energizer_overlay_active_back " +
        "",
    "block/overlay/laser_absorber_overlay_back " + "block/laser_absorber_overlay_back " +
        "block/laserengraver_overlay_top " + "block/laserwelder_overlay_top " +
        "block/quantum_energizer_overlay_back " +
        "",
    "block/overlay/laserengraver_overlay_back " + "block/laserengraver_overlay_back " +
        "block/laserengraver_overlay_front " +
        "",
    "block/overlay/laserengraver_overlay_back_active " + "block/laserengraver_overlay_back_active " +
        "block/laserengraver_overlay_back_running " + "block/laserengraver_overlay_front_active " +
        "block/laserengraver_overlay_front_running " +
        "",
    "block/overlay/laserengraver_overlay_top_running " + "block/laserengraver_overlay_top_running " +
        "block/laserwelder_overlay_top_running " +
        "",
    "block/overlay/laserwelder_overlay_bottom " + "block/laserwelder_overlay_bottom " +
        "block/laserwelder_overlay_bottom_active " + "block/laserwelder_overlay_bottom_running " +
        "",
    "block/overlay/lightningrod_overlay_front_side " + "block/lightningrod/overlay_front_side " +
        "block/vondagraagg/overlay_front_side " +
        "",
    "block/overlay/magneticseparator_overlay_top " + "block/magneticseparator_overlay_top " +
        "block/polarizer_overlay_bottom " + "block/polarizer_overlay_top " +
        "",
    "block/overlay/magneticseparator_overlay_top_active " +
        "block/magneticseparator_overlay_top_active " + "block/magneticseparator_overlay_top_running " +
        "block/polarizer_overlay_bottom_active " + "block/polarizer_overlay_bottom_running " +
        "block/polarizer_overlay_top_active " + "block/polarizer_overlay_top_running " +
        "",
    "block/overlay/massfab_overlay_back " + "block/massfab_overlay_back " +
        "block/quantum_energizer_overlay_front " + "block/replicator_overlay_back " +
        "block/scannermolecular_overlay_back " + "block/zpm_decharger_quantum/overlay_front " +
        "",
    "block/overlay/massfab_overlay_back_active " + "block/massfab_overlay_back_active " +
        "block/massfab_overlay_back_running " + "block/quantum_energizer_overlay_active_front " +
        "block/replicator_overlay_back_active " + "block/replicator_overlay_back_running " +
        "block/scannermolecular_overlay_back_active " + "block/scannermolecular_overlay_back_running " +
        "block/zpm_decharger_quantum/overlay_active_front " +
        "",
    "block/overlay/melter_overlay_back " + "block/melter_overlay_back " + "block/smelter_overlay_back " +
        "",
    "block/overlay/melter_overlay_back_active " + "block/melter_overlay_back_active " +
        "block/smelter_overlay_back_active " +
        "",
    "block/overlay/melter_overlay_back_running " + "block/melter_overlay_back_running " +
        "block/smelter_overlay_back_running " +
        "",
    "block/overlay/melter_overlay_front " + "block/melter_overlay_front " + "block/smelter_overlay_front " +
        "",
    "block/overlay/melter_overlay_front_active " + "block/melter_overlay_front_active " +
        "block/smelter_overlay_front_active " +
        "",
    "block/overlay/melter_overlay_front_running " + "block/melter_overlay_front_running " +
        "block/smelter_overlay_front_running " +
        "",
    "block/overlay/oven_overlay_back " + "block/oven_overlay_back " + "block/oven_overlay_front " +
        "",
    "block/overlay/oven_overlay_back_running " + "block/oven_overlay_back_running " +
        "block/oven_overlay_front_running " + "block/oven_overlay_running_back " +
        "block/oven_overlay_running_front " +
        "",
    "block/overlay/parts_centrifugeparts_0_overlay_side " +
        "block/parts/centrifugeparts/0/overlay/side " + "block/parts/centrifugeparts/1/overlay/side " +
        "block/parts/centrifugeparts/2/overlay/side " + "block/parts/centrifugeparts/3/overlay/side " +
        "block/parts/centrifugeparts/4/overlay/side " + "block/parts/centrifugeparts/5/overlay/side " +
        "block/parts/centrifugeparts/6/overlay/side " + "block/parts/centrifugeparts/7/overlay/side " +
        "block/parts/centrifugeparts/8/overlay/side " +
        "",
    "block/overlay/parts_centrifugeparts_1_overlay_top " + "block/parts/centrifugeparts/1/overlay/top " +
        "block/parts/centrifugeparts/6/overlay/bottom " +
        "",
    "block/overlay/parts_metalwall_1_overlay_bottom " + "block/parts/metalwall/1/overlay/bottom " +
        "block/parts/metalwall/1/overlay/side " + "block/parts/metalwall/1/overlay/top " +
        "block/parts/metalwall/7/overlay/side " + "block/parts/metalwalldense/1/overlay/bottom " +
        "block/parts/metalwalldense/1/overlay/side " + "block/parts/metalwalldense/1/overlay/top " +
        "block/parts/metalwalldense/7/overlay/side " +
        "",
    "block/overlay/parts_metalwall_2_overlay_bottom " + "block/parts/metalwall/2/overlay/bottom " +
        "block/parts/metalwall/2/overlay/side " + "block/parts/metalwall/2/overlay/top " +
        "block/parts/metalwalldense/2/overlay/bottom " + "block/parts/metalwalldense/2/overlay/side " +
        "block/parts/metalwalldense/2/overlay/top " +
        "",
    "block/overlay/parts_metalwall_3_overlay_bottom " + "block/parts/metalwall/3/overlay/bottom " +
        "block/parts/metalwall/3/overlay/side " + "block/parts/metalwall/3/overlay/top " +
        "block/parts/metalwalldense/3/overlay/bottom " + "block/parts/metalwalldense/3/overlay/side " +
        "block/parts/metalwalldense/3/overlay/top " +
        "",
    "block/overlay/parts_metalwall_5_overlay_side " + "block/parts/metalwall/5/overlay/side " +
        "block/parts/metalwalldense/5/overlay/side " +
        "",
    "block/overlay/parts_metalwall_6_overlay_side " + "block/parts/metalwall/6/overlay/side " +
        "block/parts/metalwalldense/6/overlay/side " +
        "",
    "block/overlay/parts_woodwall_0_overlay_side " + "block/parts/woodwall/0/overlay/side " +
        "block/tank_valves/tankwood_overlay_side " +
        "",
    "block/overlay/press_overlay_back " + "block/press_overlay_back " + "block/press_overlay_front " +
        "",
    "block/overlay/press_overlay_back_running " + "block/press_overlay_back_running " +
        "block/press_overlay_front_running " +
        "",
    "block/overlay/printer_overlay_front " + "block/printer_overlay_front " +
        "block/printer_overlay_front_running " +
        "",
    "block/overlay/rollformer_overlay_front " + "block/rollformer_overlay_front " +
        "block/rollingmill_overlay_front " +
        "",
    "block/overlay/rollformer_overlay_front_active " + "block/rollformer_overlay_front_active " +
        "block/rollingmill_overlay_front_active " +
        "",
    "block/overlay/rollformer_overlay_front_running " + "block/rollformer_overlay_front_running " +
        "block/rollingmill_overlay_front_running " +
        "",
    "block/overlay/squeezer_overlay_back " + "block/squeezer_overlay_back " +
        "block/squeezer_overlay_front " +
        "",
    "block/overlay/squeezer_overlay_back_running " + "block/squeezer_overlay_back_running " +
        "block/squeezer_overlay_front_running " +
        "",
    "block/overlay/turbine_mains_gasturbine_overlay_front_bottom " +
        "block/turbine_mains/gasturbine/overlay_front/bottom " +
        "block/turbine_mains/largeturbine/overlay_front/bottom " +
        "",
    "block/overlay/turbine_mains_gasturbine_overlay_front_side " +
        "block/turbine_mains/gasturbine/overlay_front/side " +
        "block/turbine_mains/largeturbine/overlay_front/side " +
        "",
    "block/overlay/turbine_mains_gasturbine_overlay_front_top " +
        "block/turbine_mains/gasturbine/overlay_front/top " +
        "block/turbine_mains/largeturbine/overlay_front/top " +
        "",
    "block/overlay/zpm_decharger_overlay_active_back " + "block/zpm_decharger/overlay_active_back " +
        "block/zpm_decharger/overlay_back " + "block/zpm_decharger_quantum/overlay_active_back " +
        "block/zpm_decharger_quantum/overlay_back " +
        "",
    "item/overlay/empty " + "block/advanced_overlay_bottom " + "block/advanced_overlay_side " +
        "block/advanced_overlay_top " + "block/autoclave_overlay_top " + "block/autoclave_overlay_top_active " +
        "block/autoclave_overlay_top_running " + "block/barrel_parts/barrel/overlay_bottom " +
        "block/barrel_parts/barrel/overlay_top " + "block/barrel_parts/drum/overlay_bottom " +
        "block/barrel_parts/drum/overlay_side " + "block/barrel_parts/plasticcan/overlay_bottom " +
        "block/barrel_parts/plasticcan/overlay_side " + "block/bedrockdrill/overlay_bottom " +
        "block/bedrockdrill/overlay_side " + "block/bedrockdrill/overlay_top " +
        "block/boiler_steam/overlay_bottom " + "block/boiler_steam/overlay_side " +
        "block/boxinator_overlay_top " + "block/boxinator_overlay_top_active " +
        "block/boxinator_overlay_top_running " + "block/bridge_engine_overlay_front " +
        "block/bridge_engine_overlay_side " + "block/bridge_heater_overlay_active_back " +
        "block/bridge_heater_overlay_active_side " + "block/bridge_heater_overlay_back " +
        "block/bridge_heater_overlay_side " + "block/bridge_motor_overlay_back " +
        "block/bridge_motor_overlay_side " + "block/bumblehive_overlay_bottom " +
        "block/bumblehive_overlay_side " + "block/bumblehive_overlay_top " + "block/bumbliary_overlay_bottom " +
        "block/bumbliary_overlay_sides " + "block/bush_parts/overlay_berries " +
        "block/bush_parts/overlay_berries_immature " + "block/bush_parts/overlay_bush " +
        "block/buzzsaw_overlay_top " + "block/buzzsaw_overlay_top_active " +
        "block/buzzsaw_overlay_top_running " + "block/cap_nozzle_overlay " + "block/cell/overlay_bottom " +
        "block/cell/overlay_insides " + "block/cell/overlay_sides " + "block/cell/overlay_top " +
        "block/charging_locker/overlay_back " + "block/charging_locker/overlay_bottom " +
        "block/charging_locker/overlay_side " + "block/charging_locker/overlay_top " +
        "block/charging_overlay_bottom " + "block/charging_overlay_side " + "block/charging_overlay_top " +
        "block/clustermill_overlay_bottom " + "block/clustermill_overlay_bottom_active " +
        "block/clustermill_overlay_bottom_running " + "block/clustermill_overlay_top " +
        "block/clustermill_overlay_top_active " + "block/clustermill_overlay_top_running " +
        "block/coagulator_overlay_back " + "block/coagulator_overlay_back_active " +
        "block/coagulator_overlay_back_running " + "block/coagulator_overlay_left " +
        "block/coagulator_overlay_left_active " + "block/coagulator_overlay_left_running " +
        "block/coagulator_overlay_right " + "block/coagulator_overlay_right_active " +
        "block/coagulator_overlay_right_running " + "block/compressor_overlay_right " +
        "block/compressor_overlay_right_active " + "block/compressor_overlay_right_running " +
        "block/crucible/overlay_bottom " + "block/crucible/overlay_front_bottom " +
        "block/crucible/overlay_front_side " + "block/crucible/overlay_front_top " +
        "block/crucible/overlay_side " + "block/crucible/overlay_top " + "block/crusher_overlay_left " +
        "block/crusher_overlay_left_active " + "block/crusher_overlay_left_running " +
        "block/crusher_overlay_right " + "block/crusher_overlay_right_active " +
        "block/crusher_overlay_right_running " + "block/cup/overlay_bottom " + "block/cup/overlay_insides " +
        "block/cup/overlay_sides " + "block/cup/overlay_top " + "block/debarker_overlay_bottom " +
        "block/debarker_overlay_bottom_active " + "block/debarker_overlay_bottom_running " +
        "block/drawer/overlay_back " + "block/drawer/overlay_bottom " + "block/drawer/overlay_side " +
        "block/drawer/overlay_top " + "block/dynamite_overlay_back " + "block/electricloom_overlay_back " +
        "block/electricloom_overlay_back_active " + "block/electricloom_overlay_back_running " +
        "block/electricsifter_overlay_left " + "block/electricsifter_overlay_left_active " +
        "block/electricsifter_overlay_left_running " + "block/electricsifter_overlay_right " +
        "block/electricsifter_overlay_right_active " + "block/electricsifter_overlay_right_running " +
        "block/extruder_overlay_top " + "block/extruder_overlay_top_active " +
        "block/extruder_overlay_top_running " + "block/funnel_overlay " + "block/fusionreactor_overlay_back " +
        "block/fusionreactor_overlay_back_active " + "block/fusionreactor_overlay_back_running " +
        "block/fusionreactor_overlay_bottom " + "block/fusionreactor_overlay_bottom_active " +
        "block/fusionreactor_overlay_bottom_running " + "block/fusionreactor_overlay_left " +
        "block/fusionreactor_overlay_left_active " + "block/fusionreactor_overlay_left_running " +
        "block/fusionreactor_overlay_right " + "block/fusionreactor_overlay_right_active " +
        "block/fusionreactor_overlay_right_running " + "block/fusionreactor_overlay_top " +
        "block/fusionreactor_overlay_top_active " + "block/fusionreactor_overlay_top_running " +
        "block/gas_cylinder/overlay_bottom " + "block/gas_cylinder/overlay_sides " +
        "block/gas_cylinder/overlay_top " + "block/hopper/overlay_bottom " + "block/hopper/overlay_side " +
        "block/hopper/overlay_top " + "block/implosioncompressor_overlay_back " +
        "block/implosioncompressor_overlay_back_active " +
        "block/implosioncompressor_overlay_back_running " + "block/implosioncompressor_overlay_left " +
        "block/implosioncompressor_overlay_left_active " +
        "block/implosioncompressor_overlay_left_running " + "block/implosioncompressor_overlay_right " +
        "block/implosioncompressor_overlay_right_active " +
        "block/implosioncompressor_overlay_right_running " + "block/implosioncompressor_overlay_top " +
        "block/implosioncompressor_overlay_top_active " + "block/implosioncompressor_overlay_top_running " +
        "block/jug/overlay_bottom " + "block/jug/overlay_insides " + "block/jug/overlay_sides " +
        "block/laminator_overlay_top " + "block/laminator_overlay_top_active " +
        "block/laminator_overlay_top_running " + "block/large_boiler/overlay_bottom " +
        "block/large_boiler/overlay_front_bottom " + "block/large_boiler/overlay_front_side " +
        "block/large_boiler/overlay_front_top " + "block/large_boiler/overlay_side " +
        "block/large_boiler/overlay_top " + "block/large_heat_exchanger/overlay_bottom " +
        "block/large_heat_exchanger/overlay_side " + "block/large_heat_exchanger/overlay_top " +
        "block/largeautoclave_overlay_back " + "block/largeautoclave_overlay_back_active " +
        "block/largeautoclave_overlay_back_running " + "block/largeautoclave_overlay_left " +
        "block/largeautoclave_overlay_left_active " + "block/largeautoclave_overlay_left_running " +
        "block/largeautoclave_overlay_right " + "block/largeautoclave_overlay_right_active " +
        "block/largeautoclave_overlay_right_running " + "block/largeautoclave_overlay_top " +
        "block/largeautoclave_overlay_top_active " + "block/largeautoclave_overlay_top_running " +
        "block/largebath_overlay_back " + "block/largebath_overlay_back_active " +
        "block/largebath_overlay_back_running " + "block/largebath_overlay_left " +
        "block/largebath_overlay_left_active " + "block/largebath_overlay_left_running " +
        "block/largebath_overlay_right " + "block/largebath_overlay_right_active " +
        "block/largebath_overlay_right_running " + "block/largebath_overlay_top " +
        "block/largebath_overlay_top_active " + "block/largebath_overlay_top_running " +
        "block/largecoagulator_overlay_back " + "block/largecoagulator_overlay_back_active " +
        "block/largecoagulator_overlay_back_running " + "block/largecoagulator_overlay_left " +
        "block/largecoagulator_overlay_left_active " + "block/largecoagulator_overlay_left_running " +
        "block/largecoagulator_overlay_right " + "block/largecoagulator_overlay_right_active " +
        "block/largecoagulator_overlay_right_running " + "block/largecoagulator_overlay_top " +
        "block/largecoagulator_overlay_top_active " + "block/largecoagulator_overlay_top_running " +
        "block/largecrusher_overlay_back " + "block/largecrusher_overlay_back_active " +
        "block/largecrusher_overlay_back_running " + "block/largecrusher_overlay_left " +
        "block/largecrusher_overlay_left_active " + "block/largecrusher_overlay_left_running " +
        "block/largecrusher_overlay_right " + "block/largecrusher_overlay_right_active " +
        "block/largecrusher_overlay_right_running " + "block/largecrusher_overlay_top " +
        "block/largecrusher_overlay_top_active " + "block/largecrusher_overlay_top_running " +
        "block/largeelectrolyzer_overlay_back " + "block/largeelectrolyzer_overlay_back_active " +
        "block/largeelectrolyzer_overlay_back_running " + "block/largeelectrolyzer_overlay_left " +
        "block/largeelectrolyzer_overlay_left_active " + "block/largeelectrolyzer_overlay_left_running " +
        "block/largeelectrolyzer_overlay_right " + "block/largeelectrolyzer_overlay_right_active " +
        "block/largeelectrolyzer_overlay_right_running " + "block/largeelectrolyzer_overlay_top " +
        "block/largeelectrolyzer_overlay_top_active " + "block/largeelectrolyzer_overlay_top_running " +
        "block/largefermenter_overlay_back " + "block/largefermenter_overlay_back_active " +
        "block/largefermenter_overlay_back_running " + "block/largefermenter_overlay_bottom " +
        "block/largefermenter_overlay_bottom_active " + "block/largefermenter_overlay_bottom_running " +
        "block/largefermenter_overlay_left " + "block/largefermenter_overlay_left_active " +
        "block/largefermenter_overlay_left_running " + "block/largefermenter_overlay_right " +
        "block/largefermenter_overlay_right_active " + "block/largefermenter_overlay_right_running " +
        "block/largefermenter_overlay_top " + "block/largefermenter_overlay_top_active " +
        "block/largefermenter_overlay_top_running " + "block/largemassfab_overlay_back " +
        "block/largemassfab_overlay_back_active " + "block/largemassfab_overlay_back_running " +
        "block/largemassfab_overlay_left " + "block/largemassfab_overlay_left_active " +
        "block/largemassfab_overlay_left_running " + "block/largemassfab_overlay_right " +
        "block/largemassfab_overlay_right_active " + "block/largemassfab_overlay_right_running " +
        "block/largemassfab_overlay_top " + "block/largemassfab_overlay_top_active " +
        "block/largemassfab_overlay_top_running " + "block/largemixer_overlay_back " +
        "block/largemixer_overlay_back_active " + "block/largemixer_overlay_back_running " +
        "block/largemixer_overlay_left " + "block/largemixer_overlay_left_active " +
        "block/largemixer_overlay_left_running " + "block/largemixer_overlay_right " +
        "block/largemixer_overlay_right_active " + "block/largemixer_overlay_right_running " +
        "block/largemixer_overlay_top " + "block/largemixer_overlay_top_active " +
        "block/largemixer_overlay_top_running " + "block/largeoven_overlay_back " +
        "block/largeoven_overlay_back_active " + "block/largeoven_overlay_back_running " +
        "block/largeoven_overlay_left " + "block/largeoven_overlay_left_active " +
        "block/largeoven_overlay_left_running " + "block/largeoven_overlay_right " +
        "block/largeoven_overlay_right_active " + "block/largeoven_overlay_right_running " +
        "block/largeoven_overlay_top " + "block/largeoven_overlay_top_active " +
        "block/largeoven_overlay_top_running " + "block/largeshredder_overlay_back " +
        "block/largeshredder_overlay_back_active " + "block/largeshredder_overlay_back_running " +
        "block/largeshredder_overlay_left " + "block/largeshredder_overlay_left_active " +
        "block/largeshredder_overlay_left_running " + "block/largeshredder_overlay_right " +
        "block/largeshredder_overlay_right_active " + "block/largeshredder_overlay_right_running " +
        "block/largeshredder_overlay_top " + "block/largeshredder_overlay_top_active " +
        "block/largeshredder_overlay_top_running " + "block/largesluice_overlay_back " +
        "block/largesluice_overlay_back_active " + "block/largesluice_overlay_back_running " +
        "block/largesluice_overlay_left " + "block/largesluice_overlay_left_active " +
        "block/largesluice_overlay_left_running " + "block/largesluice_overlay_right " +
        "block/largesluice_overlay_right_active " + "block/largesluice_overlay_right_running " +
        "block/largesluice_overlay_top " + "block/largesluice_overlay_top_active " +
        "block/largesluice_overlay_top_running " + "block/largesqueezer_overlay_back " +
        "block/largesqueezer_overlay_back_active " + "block/largesqueezer_overlay_back_running " +
        "block/largesqueezer_overlay_left " + "block/largesqueezer_overlay_left_active " +
        "block/largesqueezer_overlay_left_running " + "block/largesqueezer_overlay_right " +
        "block/largesqueezer_overlay_right_active " + "block/largesqueezer_overlay_right_running " +
        "block/largesqueezer_overlay_top " + "block/largesqueezer_overlay_top_active " +
        "block/largesqueezer_overlay_top_running " + "block/laser_electric_overlay_active_back " +
        "block/laser_electric_overlay_active_side " + "block/laser_electric_overlay_back " +
        "block/laser_electric_overlay_side " + "block/laserengraver_overlay_bottom " +
        "block/laserengraver_overlay_bottom_active " + "block/laserengraver_overlay_bottom_running " +
        "block/lathe_overlay_top " + "block/lathe_overlay_top_active " + "block/lathe_overlay_top_running " +
        "block/lightningrod/overlay_bottom " + "block/lightningrod/overlay_side " +
        "block/lightningrod/overlay_top " + "block/locker/overlay_back " + "block/locker/overlay_bottom " +
        "block/locker/overlay_side " + "block/locker/overlay_top " + "block/logistics_core/overlay_bottom " +
        "block/logistics_core/overlay_side " + "block/logistics_core/overlay_top " +
        "block/loom_overlay_back " + "block/loom_overlay_back_active " + "block/loom_overlay_back_running " +
        "block/massstorage_standard/overlay_back " + "block/massstorage_standard/overlay_side " +
        "block/massstorage_standard/overlay_top " + "block/measuring_pot_overlay_bottom " +
        "block/measuring_pot_overlay_insides " + "block/measuring_pot_overlay_sides " +
        "block/measuring_pot_overlay_top " + "block/nozzle_overlay " + "block/oven_overlay_active_top " +
        "block/oven_overlay_running_top " + "block/oven_overlay_top " + "block/oven_overlay_top_active " +
        "block/oven_overlay_top_running " + "block/parts/bedrockdrill/0/overlay/side " +
        "block/parts/coil/0/overlay/bottom " + "block/parts/coil/0/overlay/side " +
        "block/parts/coil/0/overlay/top " + "block/parts/coil/1/overlay/bottom " +
        "block/parts/coil/1/overlay/side " + "block/parts/coil/1/overlay/top " +
        "block/parts/crusherwheels/0/overlay/bottom " + "block/parts/crusherwheels/0/overlay/side " +
        "block/parts/crusherwheels/1/overlay/bottom " + "block/parts/crusherwheels/1/overlay/side " +
        "block/parts/crusherwheels/2/overlay/bottom " + "block/parts/crusherwheels/2/overlay/side " +
        "block/parts/crusherwheels/3/overlay/bottom " + "block/parts/crusherwheels/3/overlay/side " +
        "block/parts/electrolyzerparts/1/overlay/bottom " + "block/parts/electrolyzerparts/1/overlay/top " +
        "block/parts/lightningrod/0/overlay/side " + "block/parts/metalwall/0/overlay/bottom " +
        "block/parts/metalwall/0/overlay/side " + "block/parts/metalwall/0/overlay/top " +
        "block/parts/metalwall/4/overlay/bottom " + "block/parts/metalwall/4/overlay/side " +
        "block/parts/metalwall/4/overlay/top " + "block/parts/metalwall/5/overlay/bottom " +
        "block/parts/metalwall/5/overlay/top " + "block/parts/metalwall/6/overlay/bottom " +
        "block/parts/metalwall/6/overlay/top " + "block/parts/metalwall/7/overlay/bottom " +
        "block/parts/metalwall/7/overlay/top " + "block/parts/metalwalldense/0/overlay/bottom " +
        "block/parts/metalwalldense/0/overlay/side " + "block/parts/metalwalldense/0/overlay/top " +
        "block/parts/metalwalldense/4/overlay/bottom " + "block/parts/metalwalldense/4/overlay/side " +
        "block/parts/metalwalldense/4/overlay/top " + "block/parts/metalwalldense/5/overlay/bottom " +
        "block/parts/metalwalldense/5/overlay/top " + "block/parts/metalwalldense/6/overlay/bottom " +
        "block/parts/metalwalldense/6/overlay/top " + "block/parts/metalwalldense/7/overlay/bottom " +
        "block/parts/metalwalldense/7/overlay/top " + "block/parts/shredderblades/0/overlay/bottom " +
        "block/parts/shredderblades/0/overlay/side " + "block/parts/shredderblades/1/overlay/bottom " +
        "block/parts/shredderblades/1/overlay/side " + "block/parts/shredderblades/2/overlay/bottom " +
        "block/parts/shredderblades/2/overlay/side " + "block/parts/shredderblades/3/overlay/bottom " +
        "block/parts/shredderblades/3/overlay/side " + "block/parts/sluiceparts/0/overlay/bottom " +
        "block/parts/sluiceparts/0/overlay/side " + "block/parts/sluiceparts/1/overlay/bottom " +
        "block/parts/sluiceparts/1/overlay/side " + "block/parts/sluiceparts/2/overlay/bottom " +
        "block/parts/sluiceparts/2/overlay/side " + "block/parts/sluiceparts/3/overlay/bottom " +
        "block/parts/sluiceparts/3/overlay/side " + "block/parts/sluiceparts/4/overlay/bottom " +
        "block/parts/sluiceparts/4/overlay/side " + "block/parts/sluiceparts/5/overlay/bottom " +
        "block/parts/sluiceparts/5/overlay/side " + "block/parts/sluiceparts/6/overlay/bottom " +
        "block/parts/sluiceparts/6/overlay/side " + "block/parts/sluiceparts/7/overlay/bottom " +
        "block/parts/sluiceparts/7/overlay/side " + "block/parts/ventilationunit/0/overlay/bottom " +
        "block/parts/ventilationunit/0/overlay/side " + "block/parts/ventilationunit/0/overlay/top " +
        "block/parts/woodwall/0/overlay/bottom " + "block/parts/woodwall/0/overlay/top " +
        "block/plant_pot/overlay_bottom " + "block/plant_pot/overlay_side " + "block/press_overlay_bottom " +
        "block/press_overlay_bottom_active " + "block/press_overlay_bottom_running " +
        "block/printer_overlay_bottom " + "block/printer_overlay_bottom_active " +
        "block/printer_overlay_bottom_running " + "block/queuehopper/overlay_bottom " +
        "block/queuehopper/overlay_side " + "block/queuehopper/overlay_top " +
        "block/rollbender_overlay_bottom " + "block/rollbender_overlay_bottom_active " +
        "block/rollbender_overlay_bottom_running " + "block/rollbender_overlay_top " +
        "block/rollbender_overlay_top_active " + "block/rollbender_overlay_top_running " +
        "block/rollformer_overlay_bottom " + "block/rollformer_overlay_bottom_active " +
        "block/rollformer_overlay_bottom_running " + "block/rollformer_overlay_top " +
        "block/rollformer_overlay_top_active " + "block/rollformer_overlay_top_running " +
        "block/rollingmill_overlay_bottom " + "block/rollingmill_overlay_bottom_active " +
        "block/rollingmill_overlay_bottom_running " + "block/rollingmill_overlay_top " +
        "block/rollingmill_overlay_top_active " + "block/rollingmill_overlay_top_running " +
        "block/rotation_engine_overlay_back " + "block/rotation_engine_overlay_front " +
        "block/safe_keylocked/overlay_back " + "block/safe_keylocked/overlay_side " +
        "block/safe_mechanical/overlay_back " + "block/safe_mechanical/overlay_side " +
        "block/sander_overlay_back " + "block/sander_overlay_back_active " +
        "block/sander_overlay_back_running " + "block/sander_overlay_bottom " +
        "block/sander_overlay_bottom_active " + "block/sander_overlay_bottom_running " +
        "block/sap_bag/overlay_bottom " + "block/sap_bag/overlay_full_bottom " +
        "block/sap_bag/overlay_full_side " + "block/sap_bag/overlay_side " + "block/sap_bag/overlay_top " +
        "block/scannermolecular_overlay_bottom " + "block/scannermolecular_overlay_bottom_active " +
        "block/scannermolecular_overlay_bottom_running " + "block/scannermolecular_overlay_top " +
        "block/scannermolecular_overlay_top_active " + "block/scannermolecular_overlay_top_running " +
        "block/scannervisuals_overlay_bottom " + "block/scannervisuals_overlay_bottom_active " +
        "block/scannervisuals_overlay_bottom_running " + "block/scannervisuals_overlay_top " +
        "block/scannervisuals_overlay_top_active " + "block/scannervisuals_overlay_top_running " +
        "block/sensors/bucketometer/overlay_back " + "block/sensors/bucketometer/overlay_side " +
        "block/sensors/chronometer/overlay_back " + "block/sensors/chronometer/overlay_side " +
        "block/sensors/electrometer/overlay_back " + "block/sensors/electrometer/overlay_side " +
        "block/sensors/fluidometer/overlay_back " + "block/sensors/fluidometer/overlay_side " +
        "block/sensors/geigercounter/overlay_back " + "block/sensors/geigercounter/overlay_side " +
        "block/sensors/gibblometer/overlay_back " + "block/sensors/gibblometer/overlay_side " +
        "block/sensors/heavyweightometer/overlay_back " + "block/sensors/heavyweightometer/overlay_side " +
        "block/sensors/itemometer/overlay_back " + "block/sensors/itemometer/overlay_side " +
        "block/sensors/kilobucketometer/overlay_back " + "block/sensors/kilobucketometer/overlay_side " +
        "block/sensors/kilogibblometer/overlay_back " + "block/sensors/kilogibblometer/overlay_side " +
        "block/sensors/laserometer/overlay_back " + "block/sensors/laserometer/overlay_side " +
        "block/sensors/lightweightometer/overlay_back " + "block/sensors/lightweightometer/overlay_side " +
        "block/sensors/luminometer/overlay_back " + "block/sensors/luminometer/overlay_side " +
        "block/sensors/mediumweightometer/overlay_back " + "block/sensors/mediumweightometer/overlay_side " +
        "block/sensors/playercounter/overlay_back " + "block/sensors/playercounter/overlay_side " +
        "block/sensors/progressmeter/overlay_back " + "block/sensors/progressmeter/overlay_side " +
        "block/sensors/stackometer/overlay_back " + "block/sensors/stackometer/overlay_side " +
        "block/sensors/superheavyweightometer/overlay_back " +
        "block/sensors/superheavyweightometer/overlay_side " + "block/sensors/tachometer/overlay_back " +
        "block/sensors/tachometer/overlay_side " + "block/sensors/thermometer/overlay_back " +
        "block/sensors/thermometer/overlay_side " + "block/sensors/tpsmeter/overlay_back " +
        "block/sensors/tpsmeter/overlay_side " + "block/shredder_overlay_back " +
        "block/shredder_overlay_back_active " + "block/shredder_overlay_back_running " +
        "block/sifter_overlay_left " + "block/sifter_overlay_left_active " +
        "block/sifter_overlay_left_running " + "block/sifter_overlay_right " +
        "block/sifter_overlay_right_active " + "block/sifter_overlay_right_running " +
        "block/slicer_overlay_bottom " + "block/slicer_overlay_bottom_active " +
        "block/slicer_overlay_bottom_running " + "block/slicer_overlay_top " +
        "block/slicer_overlay_top_active " + "block/slicer_overlay_top_running " +
        "block/steam_engine_overlay_front " + "block/steam_engine_overlay_side " +
        "block/tank_valves/tankmetal_overlay_bottom " + "block/tank_valves/tankmetal_overlay_side " +
        "block/tank_valves/tankmetal_overlay_top " + "block/tank_valves/tankwood_overlay_bottom " +
        "block/tank_valves/tankwood_overlay_top " + "block/tap_overlay " +
        "block/tools/grindstone/overlay/axle " + "block/tools/grindstone/overlay/bottom " +
        "block/tools/grindstone/overlay/legs " + "block/tools/mortar_overlay/bottom " +
        "block/tools/mortar_overlay/insides " + "block/tools/mortar_overlay/middleside " +
        "block/tools/mortar_overlay/middletop " + "block/tools/mortar_overlay/sides " +
        "block/tools/mortar_overlay/top " + "block/tools/sifting_table/overlay/border " +
        "block/tools/sifting_table/overlay/legs " + "block/tools/sifting_table/overlay/plate " +
        "block/transformer_rotation_overlay_back " + "block/transformer_rotation_overlay_front " +
        "block/transformer_rotation_overlay_side " + "block/turbine_mains/gasturbine/overlay/bottom " +
        "block/turbine_mains/gasturbine/overlay/side " + "block/turbine_mains/gasturbine/overlay/top " +
        "block/turbine_mains/largedynamo/overlay/bottom " + "block/turbine_mains/largedynamo/overlay/side " +
        "block/turbine_mains/largedynamo/overlay/top " + "block/turbine_mains/largeturbine/overlay/bottom " +
        "block/turbine_mains/largeturbine/overlay/side " + "block/turbine_mains/largeturbine/overlay/top " +
        "block/unboxinator_overlay_top " + "block/unboxinator_overlay_top_active " +
        "block/unboxinator_overlay_top_running " + "block/vondagraagg/overlay_bottom " +
        "block/vondagraagg/overlay_side " + "block/vondagraagg/overlay_top " +
        "block/wiremill_overlay_bottom " + "block/wiremill_overlay_bottom_active " +
        "block/wiremill_overlay_bottom_running " + "block/wiremill_overlay_top " +
        "block/wiremill_overlay_top_active " + "block/wiremill_overlay_top_running " +
        "block/zpm_decharger/overlay_active_side " + "block/zpm_decharger/overlay_side " +
        "block/zpm_decharger_quantum/overlay_active_side " + "block/zpm_decharger_quantum/overlay_side " +
        "",
    };

    static final Map<String, String> ALIAS = new HashMap<>();

    static {
        for (String row : CONTENT_GROUPS) {
            String[] parts = row.trim().split("\\s+");
            if (parts.length < 2) {
                throw new IllegalStateException("degenerate alias row: " + row);
            }
            for (int i = 1; i < parts.length; i++) {
                String prev = ALIAS.put(parts[i], parts[0]);
                if (prev != null) {
                    throw new IllegalStateException("duplicate alias member: " + parts[i]);
                }
            }
        }
    }

    /** The canonical texture key for one former overlay path, or the path itself. */
    public static String resolve(String key) {
        return ALIAS.getOrDefault(key, key);
    }

    private GT6BlockOverlayAliases() {
    }
}
