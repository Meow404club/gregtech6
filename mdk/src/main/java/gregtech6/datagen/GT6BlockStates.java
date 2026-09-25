package gregtech6.datagen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTOvenBlock;
import gregtech6.block.multiblock.GTMultiBlockPartBlock;
import gregtech6.registry.GT6BeeHives;
import gregtech6.registry.GT6Portals; // p35 tail-append
import gregtech6.tileentity.bees.GT6BumbliaryBlock;
import gregtech6.registry.GT6ElectricTransformers;
import gregtech6.registry.GT6Lasers;
import gregtech6.registry.GT6Kitchen; // p38-issue7 tail-append
import gregtech6.registry.GT6MagicAbsorbers; // p32 tail-append
import gregtech6.block.foam.GT6CFoamOwnedBlock;
import gregtech6.block.energy.GT6ElectricTransformerBlock;
import gregtech6.block.energy.GTAxleBlock;
import gregtech6.block.energy.GTDieselEngineBlock;
import gregtech6.block.energy.GTTransformerRotationBlock;
import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.block.sensors.GTSensorBlock;
import gregtech6.block.stone.GTStoneBlock;
import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.registry.GTBarrels;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GTEnergySources;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTGrassBlocks;
import gregtech6.registry.GT6Logistics;
import gregtech6.registry.GTItemPipes;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GT6FeBatteries; // p26 tail-append
import gregtech6.registry.GT6ElectricDynamos; // p28 tail-append
import gregtech6.registry.GT6FeConverters; // p28 tail-append
import gregtech6.registry.GT6Attachments;
import gregtech6.registry.GT6FoamBlocks;
import gregtech6.registry.GT6Sensors;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Rails; // p35 tail-append
import gregtech6.registry.GTMultiBlocks;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWires;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;

/**
 * Blockstate + block model provider (task p3-example-machine — the first blockstates this
 * pack generates; GT6DataGenerators doc said "blockstates do not exist yet" until this card).
 * Appended to the provider set without restructuring the existing ones.
 *
 * <p>The example chest renders as a placeholder cube_all block (the chest TESR/lid is a
 * feature-layer omission, MultiTileEntityChest.java:344-417); the BlockItem model parents the
 * block model, the documented Forge blockstates idiom. Block model saving
 * (BlockStateProvider.run, BlockStateProvider.java:103-104) flushes block models before item
 * models, so the same-run parent resolves in the ExistingFileHelper. The layer texture
 * {@code gt6:textures/block/example_chest.png} is a script-generated placeholder PNG
 * (mdk/tools/gen_gui_textures.py), not JSON — no red-line conflict.
 *
 * <p>Task p4-fluid-pipes (W1) appends the fluid pipes: a cube_all placeholder over
 * {@code gt6:textures/block/fluid_pipe_wood.png} with a variant per
 * {@link gregtech6.block.pipe.GTFluidPipeBlock} CONNECTIONS mask value (0..63, the 6-bit
 * connection state — W1 renders every mask with the same model, the per-connection model
 * picking is the render pool item). The texture is an inline-script generated placeholder
 * PNG (same hand-rolled style as mdk/tools/gen_gui_textures.py), not JSON.
 */
public final class GT6BlockStates extends BlockStateProvider {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("gt6");

    /** The runData-side tint census counter (machineModel invocations — the datagen-JVM half of the pinned 21x3 audit). */
    private int mMachineTintModels;

    /** The runData-side barrel tint census counter (task p23-barrel-paint-render — the datagen-JVM half of the pinned 16 audit). */
    private int mBarrelTintModels;

    public GT6BlockStates(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, GT6DataGenerators.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        Block tChest = GTBlockEntities.EXAMPLE_CHEST.get();
        simpleBlock(tChest, models().cubeAll("example_chest", modLoc("block/example_chest")));
        itemModels().withExistingParent("example_chest", modLoc("block/example_chest"));
        addFluidPipe(GTFluidPipes.WOOD_FLUID_PIPE_SMALL.get());
        addFluidPipe(GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM.get());
        // task p26-pipe-item — the item pipe family: one cube_all placeholder per row, the
        // restrictive variants over their own PNG (the p20 placeholder ruling, no per-material art)
        for (GTItemPipes.ItemPipeRow tItemRow : GTItemPipes.ROWS) {
            addItemPipe(GTItemPipes.BLOCKS_BY_PATH.get(tItemRow.path()).get(),
                    tItemRow.variant().suffix.startsWith("restrictive"));
        }
        addLogisticsWire(GT6Logistics.LOGISTICS_WIRE.get()); // task p32-logistics-lv2 — the single logistics connector row (the pipe placeholder form)
        addWire(GTWires.WIRE_ELECTRIC_1X.get());
        addWire(GTWires.WIRE_ELECTRIC_2X.get());
        // task p10: the two wire loops SHARE one (set -> model) map — models().getBuilder
        // APPENDS to a same-named builder, so a second local map would stack a duplicate
        // element onto the shared copper/wire model JSON (caught by the runData diff).
        Map<String, ModelFile> tWireShared = new HashMap<>();
        addWireFamily(tWireShared); // task p9-wire-family-w1 ⑥ — the 620-block loop, isolated section
        addRedstoneWireFamily(tWireShared); // task p10-wire-redstone-family — the 6-block redstone loop
        addLaserWireFamily(tWireShared); // task p10-wire-laser-placeholder — the 1-block laser loop (render target upgraded to the baked fiber model by p11-wire-fiber-texture)
        addOven();
        addMachine(GTMachines.SHREDDER.get(), "shredder"); // task p7-basicmachine-family ④
        addMachine(GTMachines.CRUSHER.get(), "crusher");
        addMachine(GTMachines.LATHE.get(), "lathe");
        // task p8-machine-tiers-doinject ⑧: the T2-T4 ladder, +9 rows (the tier blocks share
        // the T1 front textures — the tier is not a visual state upstream either)
        addMachine(GTMachines.SHREDDER_T2.get(), "shredder_t2", "shredder");
        addMachine(GTMachines.SHREDDER_T3.get(), "shredder_t3", "shredder");
        addMachine(GTMachines.SHREDDER_T4.get(), "shredder_t4", "shredder");
        addMachine(GTMachines.CRUSHER_T2.get(), "crusher_t2", "crusher");
        addMachine(GTMachines.CRUSHER_T3.get(), "crusher_t3", "crusher");
        addMachine(GTMachines.CRUSHER_T4.get(), "crusher_t4", "crusher");
        addMachine(GTMachines.LATHE_T2.get(), "lathe_t2", "lathe");
        addMachine(GTMachines.LATHE_T3.get(), "lathe_t3", "lathe");
        addMachine(GTMachines.LATHE_T4.get(), "lathe_t4", "lathe");
        addDryer(); // task p14-dryer-family
        addDistillery(); // task p16-distillery-family
        addCanner(); // task p24-canner-machine
        addKineticTrio(); // task p26-w1-sifter-compressor-wiremill
        addPress(); // task p26-w1-press-extruder-molds
        addExtruder(); // task p26-w1-press-extruder-molds
        addUlvLadder(); // task p28-c-ulv-machine-ladder — the six ULV rows (family textures, the addCanner shape)
        addRollLadders(); // task p29-w1-kinetic-roll-ladder — the four roll-ladder RU families (family textures, the addCanner shape)
        addProcessMachines(); // task p29-w1-kinetic-process-ladder — the six process families (family textures, the addCanner shape)
        addEuHuFamilies(); // task p29-w1-eu-hu-families — the seven eu-hu families (family textures, the addCanner shape)
        addEuSpecialFamilies(); // task p29-w2-eu-special — the Autocrafter/Lightning/Laminator families (family textures, the addCanner shape)
        addExoticFamilies(); // task p29-w2-exotic-energy — the six exotic-energy families (family textures, the addCanner shape)
        addEuCoreMachines(); // task p29-w2-eu-core-5tier — the five eu-core families (family textures, the addCanner shape)
        addMassfabMachines(); // task p31-massfab — the small Matter Fabricator 5-ladder (family textures, the addCanner shape)
        addQuMachines(); // task p32-qu-scanner-replicator — the Molecular Scanner T3 + the Matter Replicator T1-T3 (family textures, the addCanner shape)
        addP34MachinePair(); // task p34-machines-burner-plantalyzer — the Burner Mixer ladder + the Plantalyzer 5-ladder (family textures, the addCanner shape)
        addHuTuFamilies(); // task p29-w2-hu-tu-piggyback — the seven hu-tu families (family textures, the addCanner shape)
        addHeatSmelterFamilies(); // task p29-w3-heat-smelter — the Smelter ladder + the Melter single (family textures, the addCanner shape)
        addRoastingFamilies(); // task p29-w4-eu-bridge — the Roasting Oven ladder (the "roaster" family textures)
        addP34MachineFamilies(); // task p34-machines-bumblelyzer-crucible — the Bumblelyzer 5-ladder + the Crystallisation Crucible 4-ladder (family textures, the addCanner shape)
        addElectricBridges(); // task p29-w4-eu-bridge — the three EU-bridge converter families (the borrowed upstream colored front/side pairs)
        addLaserFamilies(); // task p32-qu-laser-domain — the CO2 Laser + Laser Absorber families (the borrowed upstream colored front/side pairs)
        addMagicAbsorber(); // task p32-magic-absorber — the Magic Field Absorber single (the six-way facing cube)
        addStaticStorages(); // task p26-storage-static-batch
        addAdvancedCraftingTable(); // task p24-act-machine
        // task p21-paintable-tint-render: the datagen-JVM census half — 209 machine blocks x
        // 3 models (the six ULV rows joined at task p28-c-ulv-machine-ladder; the roll ladders
        // and the six P29 W1 process families joined at their owning cards; the seven eu-hu
        // families at task p29-w1-eu-hu-families; the three eu-special families at task
        // p29-w2-eu-special; the six exotic-energy families at task
        // p29-w2-exotic-energy; the five eu-core 5-tier families at task
        // p29-w2-eu-core-5tier; the seven hu-tu piggyback families at task
        // p29-w2-hu-tu-piggyback; the Smelter ladder + Melter single at task
        // p29-w3-heat-smelter), // matching the paintableBlockArray() client registration census
        // (the offline JUnit half walks the generated tree and pins the same 627; the ACT
        // rides its own single-state model OUTSIDE the paint-array census — the
        // GTAdvancedCraftingTableBlock carries no ACTIVE/RUNNING payload, and the
        // family-wide paint extension stays pooled).
        addHeatExchanger(); // task p29-w3-heat-smelter — the Large Heat Exchanger controller (the lightning-rod one-texture cube form)
        LOGGER.info("GT6 machine paint tint: {} machine models tinted (209 blocks x 3 + the ACT single-state model, addOven/addMachine/addDryer/addDistillery/addCanner/addKineticTrio/addPress/addExtruder/addUlvLadder/addRollLadders/addProcessMachines/addEuHuFamilies/addEuSpecialFamilies/addExoticFamilies/addEuCoreMachines/addHuTuFamilies/addHeatSmelterFamilies/addAdvancedCraftingTable)", mMachineTintModels);
        addMultiBlocks();
        addBarrel();
        addEnergySource();
        addFeBattery(); // task p26-eu-bridge-outbound (tail-append; shared serial file)
        addFeConverter(); // task p28-b-fe-converter-machine (tail-append; shared serial file)
        addFeSource(); // task p28-b-fe-converter-machine (tail-append; shared serial file)
        addTestMachines(); // task p20-testmachine-blockstates — the two dev BE-framework blocks
        addPrefixBlocks(); // task p8-prefixblock-render ①
        addCrank(); // task p12-engine-crank
        addAxles(); // task p12-axle-family
        addAttachments(); // task p12-tap-funnel-attachment
        addSteamEngines(); // task p12-engine-steam
        addDieselEngines(); // task p12-engine-diesel
        addBurningBoxes(); // task p13-burning-box-family
        addBoilers(); // task p13-boiler-tank
        addHoppers(); // task p26-storage-hopper-family
        addGearBoxTransformer(); // task p12-gearbox-transformer
        addElectricTransformer(); // task p28-c-ulv-lv-transformer
        addLDEnergyFamilies(); // task p35-energy-tail-machines
        addWaterWheel(); // task p28-c-water-wheel
        addLongDistancePipes(); // task p35-long-distance-pipes — the 16 wire metas + the two endpoints
        addElectricDynamoUlv(); // task p28-c-ulv-dynamo-row — the Electric Dynamo T0 row
        addDynamoLadders(); // task p38-c1-dynamo-bowl-models — the Electric/Flux T1-T5 ladder rows
        addBatteryBoxes(); // task p29-w4-battery-storage — the 12-box storage face
        addCrystalChargers(); // task p35-energy-tail-machines — the 20-row LU charge face
        addZpmDechargers(); // task p36-energy-zpm-dechargers — the two-row discharge face
        addLargeBoiler(); // task p13-large-boiler
        addLightningRod(); // task p24-lightning-rod
        addParts(); // task p29-w3-nbtdesign-parts — the part-family expansion (per-design variants)
        addTanks(); // task p29-w3-tank-valves — the 25 valve controllers
        addTurbinesDynamo(); // task p29-w3-turbine-dynamo — the Large Turbine + Large Dynamo controllers
        addLargeMachines(); // task p29-w3-large-12 — the twelve large-machine controllers
        addImplosionCompressor(); // task p31-implosion — the Implosion Compressor controller
        addVonDaGraagg(); // task p31-graagg — the Von da Graagg controller
        addLargeMassfab(); // task p31-massfab — the Large Matter Fabricator controller
        addFusionReactor(); // task p31-fusion — the Fusion Reactor controller
        addLogisticsCore(); // task p32-logistics-lv3 — the Logistics Core controller
        addLargeCrucible(); // task p26-crucible-multiblock
        addDistillationTowers(); // task p29-w3-distill-crucible — the twin tower controllers
        addStoneBlocks(); // task p21-stoneblocks-16item-registry-split — the 272 per-pair (stone, variant) blocks
        addGrassBlocks(); // task p24-grass-block — the 6 per-pair GT grass variants
        addTreeBlocks(); // task p30-w6-t1-trees-nine — the 27 per-pair tree blocks
        addFoamBlocks(); // task p26-c-foam-block-family — the C-Foam pair + slabs + the owned carrier
        addSensors(); // task p26-sensors-core — the three pioneer sensor blocks
        addAnvils(); // task p28-c-anvil — the stone anvil pair
        addSurfaceBand(); // task p30-w6-rocks-sticks — the surface rock trio + the stick (shared models, no items)
        addSurfacePlants(); // task p30-w6-t2-surface-blocks — the plant quartet + the four fallen-log woods
        addHive(); // task p32-bees-lv2 — the bumble hive (the tinted body + the six-overlay two-layer form)
        addBumbliary(); // task p33-bees-lv3-b-bumbliary — the Bumbliary pair (the hive two-layer grammar over the facing cube)
        addPlaceables(); // task p32-placeables — the Greg o'Lantern (the carved-front cube)
        addRails(); // task p35-rails-31-blocks — the 31-rail family (the vanilla rail grammar)
        addPortals(); // task p35-portals-mini-nether-end — the two miniature portals (the ACTIVE cube swap)
        addKitchen(); // task p38-issue7-kitchen-models — the four kitchen blocks (the upstream hollow-tub element forms)
    }

    /**
     * The rail family (task p35-rails-31-blocks, 31 blocks): the vanilla rail grammar over
     * the 61 borrowed upstream PNGs (tmp/gt6-1.7.10 assets gregtech/textures/blocks/
     * iconsets RAIL_* family, byte-identical copies). Model shapes:
     * <ul>
     * <li>normal rail x10 — the 10-shape blockstate (the vanilla rail.json variant table,
     *     client-extra.jar assets/minecraft/blockstates/rail.json: flat, flat y90, the four
     *     raised_ne/sw ascends, the four rail_corner quadrants) over 4 models per material
     *     (flat/raised_ne/raised_sw carry the STRAIGHT texture — the upstream meta&lt;6 arm,
     *     BlockBaseRail.java:119 — and the corner the TURNED texture);</li>
     * <li>booster x10 / detector x10 — the STRAIGHT-only shape property (6 shapes: the two
     *     flats + the four ascends), a POWERED arm each (the active texture row);</li>
     * <li>road stripe — the straight-only 2-flat-shapes blockstate over one model (the
     *     RAIL_ROAD_STRIPE texture; the reflector-toggle arm is cut, the GT6RoadRailBlock
     *     javadoc ruling).</li>
     * </ul>
     * Every partial state covers BOTH WATERLOGGED arms (the VariantBlockStateBuilder
     * completeness check demands the full property cross-product; waterlogging renders the
     * same model). Item models parent the material's flat model (the vanilla rail item form).
     */
    private void addRails() {
        for (int tIndex = 0; tIndex < GT6Rails.ROWS.size(); tIndex++) {
            GT6Rails.RailRow tRow = GT6Rails.ROWS.get(tIndex);
            String tSlug = GT6Rails.SLUGS[tIndex % GT6Rails.SLUGS.length]; // the material slug (ROWS = kinds x materials ascending)
            if (tRow.kind() == GT6Rails.RailKind.NORMAL) {
                ModelFile tFlat = railModel("rail_" + tSlug + "_flat", "block/rail_flat", "rail_straight_" + tSlug);
                ModelFile tRaisedNe = railModel("rail_" + tSlug + "_raised_ne", "block/template_rail_raised_ne", "rail_straight_" + tSlug);
                ModelFile tRaisedSw = railModel("rail_" + tSlug + "_raised_sw", "block/template_rail_raised_sw", "rail_straight_" + tSlug);
                ModelFile tCorner = railModel("rail_" + tSlug + "_corner", "block/rail_corner", "rail_turned_" + tSlug);
                for (boolean tWet : new boolean[] {false, true}) {
                    getVariantBuilder(GT6Rails.BLOCKS_BY_PATH.get(tRow.path()).get())
                            .partialState().with(RailBlock.SHAPE, RailShape.NORTH_SOUTH).with(BaseRailBlock.WATERLOGGED, tWet).addModels(new ConfiguredModel(tFlat))
                            .partialState().with(RailBlock.SHAPE, RailShape.EAST_WEST).with(BaseRailBlock.WATERLOGGED, tWet).addModels(new ConfiguredModel(tFlat, 0, 90, false))
                            .partialState().with(RailBlock.SHAPE, RailShape.ASCENDING_EAST).with(BaseRailBlock.WATERLOGGED, tWet).addModels(new ConfiguredModel(tRaisedNe, 0, 90, false))
                            .partialState().with(RailBlock.SHAPE, RailShape.ASCENDING_WEST).with(BaseRailBlock.WATERLOGGED, tWet).addModels(new ConfiguredModel(tRaisedSw, 0, 90, false))
                            .partialState().with(RailBlock.SHAPE, RailShape.ASCENDING_NORTH).with(BaseRailBlock.WATERLOGGED, tWet).addModels(new ConfiguredModel(tRaisedNe))
                            .partialState().with(RailBlock.SHAPE, RailShape.ASCENDING_SOUTH).with(BaseRailBlock.WATERLOGGED, tWet).addModels(new ConfiguredModel(tRaisedSw))
                            .partialState().with(RailBlock.SHAPE, RailShape.SOUTH_EAST).with(BaseRailBlock.WATERLOGGED, tWet).addModels(new ConfiguredModel(tCorner))
                            .partialState().with(RailBlock.SHAPE, RailShape.SOUTH_WEST).with(BaseRailBlock.WATERLOGGED, tWet).addModels(new ConfiguredModel(tCorner, 0, 90, false))
                            .partialState().with(RailBlock.SHAPE, RailShape.NORTH_WEST).with(BaseRailBlock.WATERLOGGED, tWet).addModels(new ConfiguredModel(tCorner, 0, 180, false))
                            .partialState().with(RailBlock.SHAPE, RailShape.NORTH_EAST).with(BaseRailBlock.WATERLOGGED, tWet).addModels(new ConfiguredModel(tCorner, 0, 270, false));
                }
                itemModels().withExistingParent(tRow.path(), modLoc("block/rail_" + tSlug + "_flat"));
            } else {
                boolean tBooster = tRow.kind() == GT6Rails.RailKind.BOOSTER;
                String tBand = tBooster ? "rail_booster_" : "rail_detector_";
                String tTex = tBooster ? "rail_booster_" : "rail_detector_";
                for (boolean tWet : new boolean[] {false, true}) {
                    for (boolean tOn : new boolean[] {false, true}) {
                        String tArm = tBand + tSlug + (tOn ? "_powered" : "_off") + (tWet ? "_wet" : "");
                        String tTexture = tTex + (tOn ? "active_" : "") + tSlug; // the RAIL_BOOSTER_ACTIVE_<mat> iconset order
                        ModelFile tFlat = railModel(tArm + "_flat", "block/rail_flat", tTexture);
                        ModelFile tRaisedNe = railModel(tArm + "_raised_ne", "block/template_rail_raised_ne", tTexture);
                        ModelFile tRaisedSw = railModel(tArm + "_raised_sw", "block/template_rail_raised_sw", tTexture);
                        getVariantBuilder(GT6Rails.BLOCKS_BY_PATH.get(tRow.path()).get())
                                .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.NORTH_SOUTH).addModels(new ConfiguredModel(tFlat))
                                .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.EAST_WEST).addModels(new ConfiguredModel(tFlat, 0, 90, false))
                                .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.ASCENDING_EAST).addModels(new ConfiguredModel(tRaisedNe, 0, 90, false))
                                .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.ASCENDING_WEST).addModels(new ConfiguredModel(tRaisedSw, 0, 90, false))
                                .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.ASCENDING_NORTH).addModels(new ConfiguredModel(tRaisedNe))
                                .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.ASCENDING_SOUTH).addModels(new ConfiguredModel(tRaisedSw));
                    }
                }
                itemModels().withExistingParent(tRow.path(), modLoc("block/" + tBand + tSlug + "_off_flat"));
            }
        }
        // the road stripe — the SAME 6-shape straight table as the booster/detector lanes
        // (RAIL_SHAPE_STRAIGHT carries the four ascends) over both POWERED arms (the false
        // arm is unreachable: the updateState no-op welds true — but the completeness check
        // still demands the cross-product)
        for (boolean tWet : new boolean[] {false, true}) {
            for (boolean tOn : new boolean[] {false, true}) {
                String tArm = "rail_road_" + (tOn ? "on" : "off") + (tWet ? "_wet" : "");
                ModelFile tFlat = railModel(tArm + "_flat", "block/rail_flat", "rail_road_stripe");
                ModelFile tRaisedNe = railModel(tArm + "_raised_ne", "block/template_rail_raised_ne", "rail_road_stripe");
                ModelFile tRaisedSw = railModel(tArm + "_raised_sw", "block/template_rail_raised_sw", "rail_road_stripe");
                getVariantBuilder(GT6Rails.ROAD_BLOCK.get())
                        .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.NORTH_SOUTH).addModels(new ConfiguredModel(tFlat))
                        .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.EAST_WEST).addModels(new ConfiguredModel(tFlat, 0, 90, false))
                        .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.ASCENDING_EAST).addModels(new ConfiguredModel(tRaisedNe, 0, 90, false))
                        .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.ASCENDING_WEST).addModels(new ConfiguredModel(tRaisedSw, 0, 90, false))
                        .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.ASCENDING_NORTH).addModels(new ConfiguredModel(tRaisedNe))
                        .partialState().with(PoweredRailBlock.POWERED, tOn).with(BaseRailBlock.WATERLOGGED, tWet).with(PoweredRailBlock.SHAPE, RailShape.ASCENDING_SOUTH).addModels(new ConfiguredModel(tRaisedSw));
            }
        }
        itemModels().withExistingParent(GT6Rails.ROAD_PATH, modLoc("block/rail_road_off_flat")); // the dry idle arm model
        LOGGER.info("GT6 rail family: {} material blockstates + the road stripe", GT6Rails.ROWS.size());
    }

    /** One rail model: the vanilla template parent + the rail texture override (the texture id is block/-prefixed). */
    private ModelFile railModel(String aName, String aParent, String aTextureBand) {
        return models().withExistingParent(aName, mcLoc(aParent)).texture("rail", modLoc("block/" + aTextureBand));
    }

    /**
     * Task p35-portals-mini-nether-end — the two miniature portals ({@link GT6Portals}).
     * ONE blockstate per portal over TWO cube_all models driven by the ACTIVE property
     * (the upstream 13-pass frame render collapsed to a frame/portal cube swap — declared
     * cosmetic deviation): inactive = the frame material face (obsidian / end_stone, the
     * vanilla textures referenced in place — zero borrowed art), active = the portal face
     * (the animated vanilla nether_portal / the owned near-black mini_portal_end.png —
     * vanilla ships no end-portal block texture, the special end-portal effect is a tile
     * renderer, not a texture). No item-model orientation (the portals are facing-free).
     */
    private void addPortals() {
        portalSwap(GT6Portals.PORTAL_NETHER.get(), "mini_portal_nether", "block/obsidian", "block/nether_portal");
        portalSwap(GT6Portals.PORTAL_END.get(), "mini_portal_end", "block/end_stone", "gt6:block/mini_portal_end");
        // the BlockItem models parent the FRAME face (the sensors walk shape)
        itemModels().withExistingParent("mini_portal_nether", modLoc("block/mini_portal_nether_frame"));
        itemModels().withExistingParent("mini_portal_end", modLoc("block/mini_portal_end_frame"));
        LOGGER.info("GT6 portals: 2 blockstates x ACTIVE frame/portal swap");
    }

    /** One portal's ACTIVE swap: false = the frame cube, true = the portal cube. */
    private void portalSwap(Block aBlock, String aName, String aFrameTexture, String aPortalTexture) {
        ModelFile tFrame = models().cubeAll(aName + "_frame",
                aFrameTexture.startsWith("gt6:") ? modLoc(aFrameTexture.substring(4)) : mcLoc(aFrameTexture));
        ModelFile tPortal = models().cubeAll(aName + "_portal",
                aPortalTexture.startsWith("gt6:") ? modLoc(aPortalTexture.substring(4)) : mcLoc(aPortalTexture));
        getVariantBuilder(aBlock).partialState()
                .with(gregtech6.block.portals.GTMiniPortalBlock.ACTIVE, false)
                .addModels(new ConfiguredModel(tFrame));
        getVariantBuilder(aBlock).partialState()
                .with(gregtech6.block.portals.GTMiniPortalBlock.ACTIVE, true)
                .addModels(new ConfiguredModel(tPortal));
    }

    /**
     * Task p32-bees-lv2 — the bumble hive: ONE blockstate over ONE model, the
     * familyMachineModel two-layer grammar collapsed to the UNFACING cube (the upstream
     * hive renders {@code BlockTextureMulti(colored[FACES_TBS[side]], overlay[FACES_TBS[side]])},
     * MultiTileEntityBumbleHive.java:78 — bottom/top/side triples, no facing):
     * element 0 = the tinted body over the borrowed grayscale colored art (tintindex 0 =
     * the p21 paint seat; worldgen paints the family colour, the client tint resolves the
     * BE PAINT), elements 1-6 = the six overlay decals (the p22 0.01-plate form, no
     * tintindex, cullface synced). No BlockItem model (the loot shell is never an item).
     */
    private void addHive() {
        Block tHive = GT6BeeHives.HIVE.get();
        BlockModelBuilder tModel = models().getBuilder("bumble_hive")
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc("block/bumblehive_colored_bottom"))
                .texture("up", modLoc("block/bumblehive_colored_top"))
                .texture("north", modLoc("block/bumblehive_colored_side"))
                .texture("south", modLoc("block/bumblehive_colored_side"))
                .texture("west", modLoc("block/bumblehive_colored_side"))
                .texture("east", modLoc("block/bumblehive_colored_side"))
                .texture("particle", modLoc("block/bumblehive_colored_side"))
                .texture("overlay_side", modLoc("block/bumblehive_overlay_side"))
                .texture("overlay_top", modLoc("block/bumblehive_overlay_top"))
                .texture("overlay_bottom", modLoc("block/bumblehive_overlay_bottom"));
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        tModel.element() // north
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_side").cullface(Direction.NORTH)
                .end();
        tModel.element() // south
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_side").cullface(Direction.SOUTH)
                .end();
        tModel.element() // west
                .from(-0.01F, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                .face(Direction.WEST).texture("#overlay_side").cullface(Direction.WEST)
                .end();
        tModel.element() // east
                .from(16.0F, 0.0F, 0.0F).to(16.01F, 16.0F, 16.0F)
                .face(Direction.EAST).texture("#overlay_side").cullface(Direction.EAST)
                .end();
        tModel.element() // bottom
                .from(0.0F, -0.01F, 0.0F).to(16.0F, 0.0F, 16.0F)
                .face(Direction.DOWN).texture("#overlay_bottom").cullface(Direction.DOWN)
                .end();
        tModel.element() // top
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_top").cullface(Direction.UP)
                .end();
        getVariantBuilder(tHive).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
    }

    /**
     * Task p33-bees-lv3-b-bumbliary — the Bumbliary pair: the addHive two-layer grammar
     * (the tinted colored body + the six 0.01-plate overlay decals) over the FACING cube
     * (the upstream Bumbliary renders the same
     * {@code BlockTextureMulti(colored[FACES_TBS[side]], overlay[FACES_TBS[side]])}
     * triple, MultiTileEntityBumbliary.java:317-327 — with the standard horizontal
     * housing spin). The advanced variant swaps the texture band (the upstream
     * bumbliary_adv art, :318-324 counterpart).
     */
    private void addBumbliary() {
        bumbliaryModel(GT6BeeHives.BUMBLIARY.get(), "bumbliary");
        bumbliaryModel(GT6BeeHives.BUMBLIARY_ADVANCED.get(), "bumbliary_adv");
    }

    private void bumbliaryModel(Block tBlock, String tBand) {
        BlockModelBuilder tModel = models().getBuilder(tBand)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc("block/" + tBand + "_colored_bottom"))
                .texture("up", modLoc("block/" + tBand + "_colored_top"))
                .texture("north", modLoc("block/" + tBand + "_colored_sides"))
                .texture("south", modLoc("block/" + tBand + "_colored_sides"))
                .texture("west", modLoc("block/" + tBand + "_colored_sides"))
                .texture("east", modLoc("block/" + tBand + "_colored_sides"))
                .texture("particle", modLoc("block/" + tBand + "_colored_sides"))
                .texture("overlay_side", modLoc("block/" + tBand + "_overlay_sides"))
                .texture("overlay_top", modLoc("block/" + tBand + "_overlay_top"))
                .texture("overlay_bottom", modLoc("block/" + tBand + "_overlay_bottom"));
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        tModel.element() // north
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_side").cullface(Direction.NORTH)
                .end();
        tModel.element() // south
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_side").cullface(Direction.SOUTH)
                .end();
        tModel.element() // west
                .from(-0.01F, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                .face(Direction.WEST).texture("#overlay_side").cullface(Direction.WEST)
                .end();
        tModel.element() // east
                .from(16.0F, 0.0F, 0.0F).to(16.01F, 16.0F, 16.0F)
                .face(Direction.EAST).texture("#overlay_side").cullface(Direction.EAST)
                .end();
        tModel.element() // bottom
                .from(0.0F, -0.01F, 0.0F).to(16.0F, 0.0F, 16.0F)
                .face(Direction.DOWN).texture("#overlay_bottom").cullface(Direction.DOWN)
                .end();
        tModel.element() // top
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_top").cullface(Direction.UP)
                .end();
        getVariantBuilder(tBlock).forAllStates(aState -> ConfiguredModel.builder()
                .modelFile(tModel)
                .rotationY((int) aState.getValue(GT6BumbliaryBlock.FACING).toYRot())
                .build());
    }

    /**
     * Task p30-w6-rocks-sticks — the surface deco band: FOUR blocks over TWO shared
     * models (the research winner's shared-model+tint deviation — a 1.20.1 static model
     * cannot sample the block below, GTCEu SurfaceRockModelGenerator.java:29-51 same).
     * The rock model is ONE tinted micro box (vanilla stone texture, tintindex 0 — the
     * {@link #tintedCubeAll} grammar; the client BlockColor paints it per material), the
     * stick model a 12x2x2 lying bar over the vanilla oak-log side (MultiTileEntityStick
     * .java:51 Blocks.log SIDE_FRONT 0 borrow, no tint index). Textures are VANILLA
     * borrows — the upstream rocks/sticks copy Blocks.stone/Blocks.log verbatim
     * (MultiTileEntityRock.java:55), so no PNG lands.
     *
     * <p>Task p38-issue1-4 (GitHub #1, the flat-full-pelt fix): the two boxes tightened
     * to the upstream forms. The stick: the 12x2x2 ground bar (the MultiTileEntityStick.java
     * :53 default bounds — PX_P[2]..PX_N[2] = 2..14 long axis x PX_P[7]..PX_N[7] = 7..9
     * thickness, height 2, the review-seat endpoint-notation correction; the :58-68
     * readFromNBT2 random X-long/Z-long arm pair runs the 14-long variant); the model pins
     * the default centered-in-Z
     * pose and the FACING dispatch below still emits both rotationY orientations (the
     * EAST/WEST arms = the Z-long arm), replacing the old 12x2x12 full-pelt slab. The
     * rock: the 8x3x8 fixed representative of the upstream 2..8px-wide x 1..4px-high
     * random micro box (MultiTileEntityRock.java:58-67 — a static model cannot randomise
     * per placement NBT, the GT6SurfaceRockBlock "random micro box" declared deviation;
     * the p30 research-card 3px pebble height kept).
     *
     * <p>The FACING dispatch follows the GTCEu :38-48 variant table shape, with the
     * x-rotation arms corrected to what the rotations actually express (the blockstate
     * variant grammar has NO rotationZ, so EAST/WEST stay y-only — a floor-lying box
     * quirk GTCEu's table shares; worldgen and player placement are DOWN/UP anyway).
     * No BlockItem models: the blocks are never obtainable as items (the pickup loot is
     * the only item path).
     */
    private void addSurfaceBand() {
        ModelFile tRockModel = microBoxModel("surface_rock", mcLoc("block/stone"), true, 4, 0, 4, 12, 3, 12);
        ModelFile tStickModel = microBoxModel("surface_stick", mcLoc("block/oak_log"), false, 2, 0, 7, 14, 2, 9);
        for (var tRow : GT6SurfaceBlocks.ALL) {
            Block tBlock = tRow.get();
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tX = 0, tY = 0;
                switch (aState.getValue(gregtech6.block.surface.GT6SurfaceRockBlock.FACING)) {
                    case UP -> tX = 180;
                    case NORTH -> tX = 270;
                    case SOUTH -> tX = 90;
                    case WEST -> tY = 270; // no rotationZ in the variant grammar (see javadoc)
                    case EAST -> tY = 90;
                    default -> {} // DOWN: the floor form, no rotation
                }
                return ConfiguredModel.builder().modelFile(
                        tBlock == GT6SurfaceBlocks.SURFACE_STICK.get() ? tStickModel : tRockModel)
                        .rotationX(tX).rotationY(tY).build();
            });
        }
    }

    /** One tinted-or-plain micro box model: the aX1/aY1/aZ1..aX2/aY2/aZ2 ground box, tintindex 0 on every face when aTinted. */
    private ModelFile microBoxModel(String aName, ResourceLocation aTexture, boolean aTinted,
            int aX1, int aY1, int aZ1, int aX2, int aY2, int aZ2) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("slab", aTexture)
                .texture("particle", "#slab");
        tModel.element()
                .from((float) aX1, (float) aY1, (float) aZ1).to((float) aX2, (float) aY2, (float) aZ2)
                .allFaces((aDir, aFace) -> {
                    aFace.texture("#slab");
                    if (aTinted) aFace.tintindex(0);
                })
                .end();
        return tModel;
    }

    /**
     * Task p28-c-anvil — the two stone anvil rows (Loader_MultiTileEntities.java
     * :2185-2186): ONE oriented cube model over the two grayscale placeholders (the
     * upstream stonetype texture has no borrowable source in this repo — the hopper
     * no-borrow precedent; the top face carries the working-surface texture, FACING is
     * HORIZONTAL only — the upstream SIDES_VALID :413 — so the y-mapping is the horizontal
     * band of the boiler form). Both rows share the model. The 2 BlockItem models parent it.
     */
    private void addAnvils() {
        ModelFile tModel = models().cube("gt6_anvil",
                modLoc("block/anvil_top"), modLoc("block/anvil_top"), // bottom/top
                modLoc("block/anvil_side"), modLoc("block/anvil_side"), // north/side rows (the FACING front is not a texture state)
                modLoc("block/anvil_side"), modLoc("block/anvil_side"));
        for (Block tBlock : new Block[] {
                gregtech6.registry.GT6Anvils.STONE_ANVIL.get(),
                gregtech6.registry.GT6Anvils.BLACKSTONE_ANVIL.get()}) {
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(gregtech6.block.tools.GTAnvilBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tModel).rotationX(0).rotationY(tY).build();
            });
        }
        itemModels().withExistingParent("stone_anvil", tModel.getLocation());
        itemModels().withExistingParent("blackstone_anvil", tModel.getLocation());
    }

    /**
     * Task p14-dryer-family — the four Dryer rows (Loader_MultiTileEntities.java
     * :1477-1480, all four NBT_TEXTURE "dryer"): the addMachine texture-base overload —
     * model names per path (dryer/dryer_t2/... own 16-variant blockstates + item
     * parents) while the FRONT TEXTURES stay on the family "dryer" set (the tier is not
     * a visual state upstream either, the p8 ruling — the ladder adds three PNGs total,
     * the placeholder fronts).
     */
    /**
     * Task p26-crucible-multiblock — the LARGE crucible family (the wall "Steel Wall"
     * + the "Large Steel Crucible" controller): the WALLS over the borrowed metalwall part
     * textures with the material tint (task issue8-residual — the upstream :1145 "Steel
     * Wall" row is NBT_TEXTURE "metalwall" + NBT_MATERIAL Steel, so the dedicated
     * {@code GTCrucibleWallBlock} blocks ride the {@code tintedCube} tinted-body form over
     * the already-borrowed parts/metalwall/0 colored family, the tank_metal borrow; the
     * former large_boiler flat-gray placeholder tinted into a flat plate), while the
     * CONTROLLERS keep the boiler-wall placeholder cube (the formed/unformed and the
     * molten-content faces are the declared render defer, the controller blockstate still
     * carries the full 8 FACING×FORMED state coverage).
     */
    private void addLargeCrucible() {
        Block tWall = gregtech6.registry.GT6Crucibles.CRUCIBLE_STEEL_WALL.get();
        simpleBlock(tWall, tintedCube("crucible_steel_wall",
                "block/parts/metalwall/0/colored/bottom", "block/parts/metalwall/0/colored/top", "block/parts/metalwall/0/colored/side"));
        itemModels().withExistingParent("crucible_steel_wall", modLoc("block/crucible_steel_wall"));
        // task p29-w3-distill-crucible ③ — the seven ladder walls (the same metalwall
        // borrow; the composed names ride the metal-wall template, zero new keys)
        for (var tHandle : gregtech6.registry.GT6Crucibles.CRUCIBLE_WALL_BLOCKS_BY_PATH.values()) {
            Block tLadderWall = tHandle.get();
            simpleBlock(tLadderWall, tintedCube(tHandle.getId().getPath(),
                    "block/parts/metalwall/0/colored/bottom", "block/parts/metalwall/0/colored/top", "block/parts/metalwall/0/colored/side"));
            itemModels().withExistingParent(tHandle.getId().getPath(), modLoc("block/" + tHandle.getId().getPath()));
        }
        for (gregtech6.registry.GT6Crucibles.CrucibleRow tRow : gregtech6.registry.GT6Crucibles.CRUCIBLE_ROWS) {
            Block tBlock = gregtech6.registry.GT6Crucibles.CRUCIBLE_BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tModel = models().cubeAll(tRow.path(), modLoc("block/large_boiler/wall"));
            getVariantBuilder(tBlock).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    /**
     * Task p29-w3-distill-crucible ①② — the twin tower controllers (Loader:1226-1227,
     * NBT_TEXTURE "distillationtower"/"cryodistillationtower"): placeholder cubes over the
     * BORROWED distillation-tower-part texture (the card ① texture table carries no
     * controller family — the machine-wall placeholder convention; the FACING×FORMED block
     * state coverage rides forAllStates like the crucible controllers).
     */
    private void addDistillationTowers() {
        for (gregtech6.registry.GT6Distillation.TowerRow tRow : gregtech6.registry.GT6Distillation.ROWS) {
            Block tBlock = gregtech6.registry.GT6Distillation.TOWER_BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tModel = models().cubeAll(tRow.path(), modLoc("block/parts/distillationtowerparts/0/colored/side"));
            getVariantBuilder(tBlock).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    private void addDryer() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.DRYER_ROWS) {
            addMachine(GTMachines.DRYER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p16-distillery-family — the four Distillery rows (Loader_MultiTileEntities.java
     * :1398-1401, all four NBT_TEXTURE "distillery"): the addDryer shape verbatim —
     * model names per path, the FRONT TEXTURES stay on the family "distillery" set
     * (three placeholder PNGs total, the p14 dryer ruling).
     */
    private void addDistillery() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.DISTILLERY_ROWS) {
            addMachine(GTMachines.DISTILLERY_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p24-canner-machine — the four Canner rows (Loader_MultiTileEntities.java
     * :1379-1382, all four NBT_TEXTURE "canner"): the addDistillery shape verbatim —
     * model names per path, the FRONT TEXTURES stay on the family "canner" set (the
     * p22 split-front borrow: canner_colored_front + the three overlay decals).
     */
    private void addCanner() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CANNER_ROWS) {
            addMachine(GTMachines.CANNER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p26-w1-sifter-compressor-wiremill — the W1 Kinetic trio (Sifter/Compressor/
     * Wiremill rows, Loader_MultiTileEntities.java :1312-1315/:1343-1346/:1373-1376, all
     * rows NBT_TEXTURE "sifter"/"compressor"/"wiremill" per family): the addCanner shape
     * verbatim — model names per path, the FRONT TEXTURES stay on the family set (the
     * split-front borrow: {family}_colored_front + the three overlay decals each).
     */
    private void addKineticTrio() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SIFTER_ROWS) {
            addMachine(GTMachines.SIFTER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.COMPRESSOR_ROWS) {
            addMachine(GTMachines.COMPRESSOR_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.WIREMILL_ROWS) {
            addMachine(GTMachines.WIREMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p26-w1-press-extruder-molds — the four Press rows (Loader_MultiTileEntities.java
     * :1425-1428, all four NBT_TEXTURE "press"): the addCanner shape verbatim — model names
     * per path, the FRONT TEXTURES stay on the family "press" set (the borrowed upstream
     * basicmachines/press fronts, the p22 split-front borrow).
     */
    private void addPress() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.PRESS_ROWS) {
            addMachine(GTMachines.PRESS_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p26-w1-press-extruder-molds — the four Extruder rows (Loader_MultiTileEntities
     * .java:1406-1409, all four NBT_TEXTURE "extruder"): the addPress shape verbatim (the
     * borrowed upstream basicmachines/extruder fronts).
     */
    private void addExtruder() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.EXTRUDER_ROWS) {
            addMachine(GTMachines.EXTRUDER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p28-c-ulv-machine-ladder — the six ULV rows (the five family extension rows +
     * the Rolling Mill rung, all NBT_TEXTURE riding their family/upstream tokens
     * "shredder"/"crusher"/"canner"/"sifter"/"wiremill"/"rollingmill"): the addCanner
     * shape verbatim — model names per path, the FRONT TEXTURES stay on the family sets
     * (the rollingmill fronts are the borrowed upstream basicmachines/rollingmill split,
     * the p22 borrow pipeline).
     */
    private void addUlvLadder() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SHREDDER_ULV_ROWS) {
            addMachine(GTMachines.SHREDDER_ULV.get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CRUSHER_ULV_ROWS) {
            addMachine(GTMachines.CRUSHER_ULV.get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CANNER_ULV_ROWS) {
            addMachine(GTMachines.CANNER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SIFTER_ULV_ROWS) {
            addMachine(GTMachines.SIFTER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.WIREMILL_ULV_ROWS) {
            addMachine(GTMachines.WIREMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLLINGMILL_ROWS) {
            addMachine(GTMachines.ROLLINGMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p29-w1-kinetic-roll-ladder — the four roll-ladder RU families (the RollingMill
     * RU ladder + RollBender + RollFormer + ClusterMill, upstream Loader_MultiTileEntities
     * .java:1349-1370, all NBT_TEXTURE riding their family tokens "rollingmill"/
     * "rollbender"/"rollformer"/"clustermill"): the addCanner shape verbatim — model
     * names per path, the FRONT TEXTURES stay on the family sets (the rollingmill set is
     * the borrowed upstream basicmachines/rollingmill split shared with the p28 ULV rung;
     * the rollbender/rollformer/clustermill sets join via the same borrow pipeline). The
     * RU RollingMill rows keep their tier-suffixed paths (the p28 ULV rung owns the bare
     * "rollingmill" path/model) while sharing the family texture set.
     */
    private void addRollLadders() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLLINGMILL_RU_ROWS) {
            addMachine(GTMachines.ROLLINGMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLL_BENDER_ROWS) {
            addMachine(GTMachines.ROLLBENDER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLL_FORMER_ROWS) {
            addMachine(GTMachines.ROLLFORMER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CLUSTER_MILL_ROWS) {
            addMachine(GTMachines.CLUSTERMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p29-w1-kinetic-process-ladder — the six process families (Buzzsaw/Squeezer/
     * Centrifuge/Sluice/Sanding Machine/Pressure Washer, Loader_MultiTileEntities.java
     * :1318-1321/:1324-1327/:1330-1333/:1464-1467/:1589-1592/:1615-1618, the rows of each
     * family sharing one NBT_TEXTURE): the addCanner shape verbatim — model names per
     * path, the FRONT TEXTURES stay on the family set (the borrowed upstream
     * basicmachines/{buzzsaw,squeezer,centrifuge,sluice,sander,debarker} split fronts).
     * The Sanding Machine rows ride the upstream "sander" art token and the Pressure
     * Washer rows the upstream "debarker" token (the NBT_TEXTURE fidelity over the
     * registry path — the row.texture() column carries it).
     */
    private void addProcessMachines() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.BUZZSAW_ROWS) {
            addMachine(GTMachines.BUZZSAW_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SQUEEZER_ROWS) {
            addMachine(GTMachines.SQUEEZER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CENTRIFUGE_ROWS) {
            addMachine(GTMachines.CENTRIFUGE_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SLUICE_ROWS) {
            addMachine(GTMachines.SLUICE_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SANDING_ROWS) {
            addMachine(GTMachines.SANDING_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.PRESSURE_WASHER_ROWS) {
            addMachine(GTMachines.PRESSURE_WASHER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p29-w1-eu-hu-families — the seven eu-hu families (the Mixer RU ladder :1392-1395
     * + the ElectricMixer/ElectricLoom/ElectricSifter/Boxinator/Unboxinator EU ladders
     * :1504-1522/:1635-1646 + the single-variant Fermenter :1654, every row NBT_TEXTURE
     * riding its family token): the addCanner shape verbatim — model names per path, the
     * FRONT TEXTURES stay on the family sets (the borrowed upstream basicmachines/
     * {mixer,electricmixer,electricloom,electricsifter,boxinator,unboxinator,fermenter}
     * fronts, the p22 borrow pipeline — the borrow_port_overlays.py census lands them).
     */
    private void addEuHuFamilies() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.MIXER_ROWS) {
            addMachine(GTMachines.MIXER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ELECTRIC_MIXER_ROWS) {
            addMachine(GTMachines.ELECTRIC_MIXER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ELECTRIC_LOOM_ROWS) {
            addMachine(GTMachines.ELECTRIC_LOOM_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ELECTRIC_SIFTER_ROWS) {
            addMachine(GTMachines.ELECTRIC_SIFTER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.BOXINATOR_ROWS) {
            addMachine(GTMachines.BOXINATOR_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.UNBOXINATOR_ROWS) {
            addMachine(GTMachines.UNBOXINATOR_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.FERMENTER_ROWS) {
            addMachine(GTMachines.FERMENTER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());        }
    }

    /**
     * Task p29-w2-eu-special — the three eu-special families (the Autocrafter EU 5-tier
     * ladder :1497-1501 + the Lightning Processor EU 5-tier ladder :1582-1586 + the
     * Laminator HU ladder :1532-1535, every row NBT_TEXTURE riding its family token
     * "autocrafter"/"lightning"/"laminator"): the addEuHuFamilies shape verbatim — model
     * names per path (the FIRST _t5 rungs in the census), the FRONT TEXTURES stay on the
     * family sets (the borrowed upstream basicmachines/{autocrafter,lightning,laminator}
     * six-face bodies + state trios, the borrow_port_overlays census lands them).
     */
    private void addEuSpecialFamilies() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.AUTOCRAFTER_ROWS) {
            addMachine(GTMachines.AUTOCRAFTER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.LIGHTNING_ROWS) {
            addMachine(GTMachines.LIGHTNING_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.LAMINATOR_ROWS) {
            addMachine(GTMachines.LAMINATOR_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p29-w2-exotic-energy — the six exotic-energy families (Polarizer/MagneticSeparator/
     * LaserEngraver/LaserWelder/Freezer/CryoMixer, Loader_MultiTileEntities.java :1418-1422/
     * :1470-1474/:1483-1487/:1490-1494/:1621-1625/:1628-1632, the rows of each family sharing
     * one NBT_TEXTURE): the addCanner shape verbatim — model names per path, the FRONT
     * TEXTURES stay on the family set (the borrowed upstream basicmachines/{polarizer,
     * magneticseparator,laserengraver,laserwelder,freezer,cryomixer} fronts). The registry
     * paths keep the snake-case family names while the art tokens stay the upstream camel
     * joins (the row.texture() column carries them — the pressure_washer/debarker form).
     */
    private void addExoticFamilies() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.POLARIZER_ROWS) {
            addMachine(GTMachines.POLARIZER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.MAGNETIC_SEPARATOR_ROWS) {
            addMachine(GTMachines.MAGNETIC_SEPARATOR_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.LASER_ENGRAVER_ROWS) {
            addMachine(GTMachines.LASER_ENGRAVER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.LASER_WELDER_ROWS) {
            addMachine(GTMachines.LASER_WELDER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.FREEZER_ROWS) {
            addMachine(GTMachines.FREEZER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CRYO_MIXER_ROWS) {
            addMachine(GTMachines.CRYO_MIXER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p31-massfab — the small Matter Fabricator 5-ladder (Loader_MultiTileEntities
     * .java:1542-1546, the rows of the family sharing the one NBT_TEXTURE "massfab"): the
     * addCanner shape verbatim — model names per path, the front textures stay on the
     * family set (the borrowed upstream basicmachines/massfab fronts, the animated
     * overlay strips flattened to their frame 0 — the W1 borrow pipeline).
     */
    private void addMassfabMachines() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.MASSFAB_SMALL_ROWS) {
            addMachine(GTMachines.MASSFAB_SMALL_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p32-qu-scanner-replicator — the QU machine pair (Loader_MultiTileEntities.java
     * :1551 the Molecular Scanner T3 single, :1556-1558 the Matter Replicator T1-T3 rungs;
     * the rows of each family sharing the one NBT_TEXTURE "scannermolecular"/"replicator"):
     * the addCanner shape verbatim — model names per path, the front textures stay on the
     * family set (the borrowed upstream basicmachines/{scannermolecular,replicator} split
     * fronts, the animated overlay strips flattened to their frame 0 — the W1 borrow
     * pipeline).
     */
    private void addQuMachines() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.MOLECULAR_SCANNER_ROWS) {
            addMachine(GTMachines.MOLECULAR_SCANNER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.REPLICATOR_ROWS) {
            addMachine(GTMachines.REPLICATOR_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p29-w2-eu-core-5tier — the five eu-core families (the Electrolyzer/Injector/
     * Printer/Scanner(Visuals)/Slicer EU 5-tier ladders, Loader_MultiTileEntities.java
     * :1336-1340/:1443-1447/:1450-1454/:1457-1461/:1525-1529, the rows of each family
     * sharing one NBT_TEXTURE): the addCanner shape verbatim — model names per path, the
     * FRONT TEXTURES stay on the family set (the borrowed upstream
     * basicmachines/{electrolyzer,injector,printer,scannervisuals,slicer} split fronts,
     * the animated overlay strips flattened to their frame 0 — the W1 borrow pipeline).
     * The FIRST 5-TIER ladders of the port: each walk covers the five tier blocks T1-T5
     * (the T5 rung shares the family texture set — the tier is not a visual state
     * upstream, so the ladder adds zero PNGs beyond the family set).
     */
    private void addEuCoreMachines() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ELECTROLYZER_ROWS) {
            addMachine(GTMachines.ELECTROLYZER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.INJECTOR_ROWS) {
            addMachine(GTMachines.INJECTOR_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.PRINTER_ROWS) {
            addMachine(GTMachines.PRINTER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SCANNER_VISUALS_ROWS) {
            addMachine(GTMachines.SCANNER_VISUALS_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SLICER_ROWS) {
            addMachine(GTMachines.SLICER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p29-w2-hu-tu-piggyback — the seven hu-tu families (the SteamCracker/CatalyticCracker
     * HU 4-ladders :1576-1579/:1570-1573 + the TU four singles :1651-1655 + the Loom RU
     * 4-ladder :1412-1415, every row NBT_TEXTURE riding its family token): the addCanner
     * shape verbatim — model names per path, the FRONT TEXTURES stay on the family sets
     * (the borrowed upstream basicmachines/{steamcracker,catalyticcracker,coagulator,
     * generifier,bath,autoclave,loom} fronts — the borrow-or-declare rule, zero hand-drawn).
     */
    private void addHuTuFamilies() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.STEAM_CRACKER_ROWS) {
            addMachine(GTMachines.STEAM_CRACKER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CATALYTIC_CRACKER_ROWS) {
            addMachine(GTMachines.CATALYTIC_CRACKER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.COAGULATOR_ROWS) {
            addMachine(GTMachines.COAGULATOR_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.GENERIFIER_ROWS) {
            addMachine(GTMachines.GENERIFIER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.BATH_ROWS) {
            addMachine(GTMachines.BATH_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.AUTOCLAVE_ROWS) {
            addMachine(GTMachines.AUTOCLAVE_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.LOOM_ROWS) {
            addMachine(GTMachines.LOOM_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p29-w3-heat-smelter — the two heat families (the Smelter HU 4-ladder
     * :1431-1434 + the Melter HU single :1657, every row NBT_TEXTURE riding its family
     * token): the addCanner shape verbatim — model names per path, the FRONT TEXTURES
     * stay on the family sets (the borrowed upstream basicmachines/{smelter,melter}
     * fronts — the borrow-or-declare rule, zero hand-drawn).
     */
    private void addHeatSmelterFamilies() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SMELTER_ROWS) {
            addMachine(GTMachines.SMELTER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.MELTER_ROWS) {
            addMachine(GTMachines.MELTER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p26-storage-static-batch — the 28 static storage rows (GT6StaticStorages.ROWS):
     * ONE oriented cube model per KIND over the placeholder front/side PNG pair (the
     * script-generated placeholder ruling — no upstream borrowable iconset in this repo),
     * the FRONT face carries the kind's front texture, the FACING (horizontal) drives the
     * 4-variant y-rotation (the ACT single-state arm; north default, south 180, west 270,
     * east 90). All six kinds share the model within the kind (the material/plank ladder
     * folds to the row, the same placeholder per material — the p20 placeholder ruling).
     * The 28 BlockItem models parent the kind model.
     */
    private void addStaticStorages() {
        for (gregtech6.registry.GT6StaticStorages.Kind tKind : gregtech6.registry.GT6StaticStorages.Kind.values()) {
            String tTex = "block/" + kindModelName(tKind) + "_";
            ModelFile tModel = models().cube("gt6_" + kindModelName(tKind),
                    modLoc(tTex + "side"), modLoc(tTex + "side"),        // bottom/top
                    modLoc(tTex + "front"), modLoc(tTex + "side"),       // north(front)/south
                    modLoc(tTex + "side"), modLoc(tTex + "side"));       // west/east
            for (gregtech6.registry.GT6StaticStorages.StaticRow tRow : gregtech6.registry.GT6StaticStorages.ROWS) {
                if (tRow.kind() != tKind) continue;
                Block tBlock = gregtech6.registry.GT6StaticStorages.BLOCKS_BY_PATH.get(tRow.path()).get();
                getVariantBuilder(tBlock).forAllStates(aState -> {
                    int tY;
                    switch (aState.getValue(gregtech6.registry.GT6StaticStorages.GT6StorageBlock.FACING)) {
                        case SOUTH -> tY = 180;
                        case WEST -> tY = 270;
                        case EAST -> tY = 90;
                        default -> tY = 0; // NORTH
                    }
                    return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
                });
                itemModels().withExistingParent(tRow.path(), tModel.getLocation());
            }
        }
    }

    /** The texture/model stem of a storage kind (the PNG pair naming). */
    private static String kindModelName(gregtech6.registry.GT6StaticStorages.Kind aKind) {
        return switch (aKind) {
            case LOCKER -> "locker";
            case DRAWER -> "drawer";
            case SAFE_MECHANICAL, SAFE_KEYLOCKED -> "safe";
            case BOOKSHELF -> "bookshelf";
            case BOTTLECRATE -> "bottlecrate";
        };
    }

    /**
     * Task p29-w3-heat-smelter — the Large Heat Exchanger controller (Loader
     * :1245): ONE cube_all model over the borrowed upstream multiblockmains/
     * largeheatexchanger composite (the colored base alpha-over the overlay layer
     * composited at borrow time — all three upstream faces composite to the SAME
     * visible pixels, the lightningrod ruling; one texture serves all six faces),
     * every FORMED state maps to the same model (the formed-look visual is the p9
     * pool). No facing (the structure is facing-independent — the controller is the
     * centre cell of both layers). The BlockItem parents the block model.
     */
    private void addHeatExchanger() {
        ModelFile tMain = models().cubeAll("large_heat_exchanger", modLoc("block/large_heat_exchanger/main"));
        Block tController = gregtech6.registry.GT6HeatExchangers.HEAT_EXCHANGER_BLOCK.get();
        getVariantBuilder(tController).forAllStates(aState -> ConfiguredModel.builder().modelFile(tMain).build());
        itemModels().withExistingParent("large_heat_exchanger", tMain.getLocation());
    }

    /**
     * Task p24-act-machine — the Advanced Crafting Table (Loader_MultiTileEntities.java
     * :136, the single-variant row): FACING-ONLY blockstate (the upstream machine has no
     * ACTIVE/RUNNING visual payload — the craftingtables/advanced texture group ships no
     * overlay_active/overlay_running layers, the borrow-or-declare rule landed exactly
     * the two borrowable fronts), 4 facing variants over ONE tinted machine model
     * (machineModel = the shared oven body + the advanced_colored/overlay fronts), and
     * the BlockItem parent. The model joins the tint census count but NOT the
     * paintableBlockArray (the family-wide paint extension stays pooled, the class doc
     * of the block).
     */
    private void addAdvancedCraftingTable() {
        Block tBlock = GTMachines.ADVANCED_CRAFTING_TABLE.get();
        ModelFile tModel = machineModel("advanced_crafting_table", "advanced_colored_front", "advanced_overlay_front");
        getVariantBuilder(tBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(GTOvenBlock.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        itemModels().withExistingParent("advanced_crafting_table", modLoc("block/advanced_crafting_table"));
    }

    /**
     * Task p4-multiblock-framework (W3 provider order 1: multiblock→barrel→cover): the FORMED
     * blockstate fallback rendering (spec ⑧) — the Coke Oven controller carries the base-owned
     * FACING + FORMED properties (TileEntityBase10MultiBlockBase :188-189 bit-3 replacement),
     * 4 facings x 2 formed = 8 variants over two cube models.
     *
     * <p>Erratum (task p9-render-d-formed-look; the earlier "mirrors the upstream getTexture2
     * mStructureOkay pick" wording here was wrong): upstream getTexture2
     * (TileEntityBase10MultiBlockBase.java:192-194) picks the front-vs-side texture GROUPS by
     * {@code aSide == mFacing} over a colored+overlay two-layer stack, and NO upstream
     * consumer picks the front/side texture groups by mStructureOkay — every getTexture2,
     * including the Crucible's own (MultiTileEntityCrucible.java:643-651), keys the group on
     * {@code aSide == mFacing}. The only known VISUAL mStructureOkay consumers are the render
     * pass count (MultiTileEntityLargeTurbine.getRenderPasses2 :109-111, formed renders an
     * extra pass) and the render bounds (MultiTileEntityCrucible.setBlockBounds2 :628-638) —
     * neither touches the texture groups. The formed/unformed dual model below is therefore
     * a DECLARED this-port enhancement beyond upstream, kept as-is: the FORMED
     * blockstate is the RCON {@code execute if block ...[formed=true]} assertion surface, and
     * the GTCEu IS_FORMED ModelProperty technique is not adopted (zero-benefit refactor,
     * ADR 2026-09-01-p9-render-d-formed-look).
     *
     * <p>Texture census (same task, negative): upstream ships NO
     * {@code machines/multiblockmains/cokeoven/} PNG group at all — the colored/overlay/
     * colored_front/overlay_front icon paths the upstream controller registers
     * (TileEntityBase10MultiBlockBase.java:66-81, NBT_TEXTURE "cokeoven",
     * Loader_MultiTileEntities.java:1193) are missing resources in the upstream snapshot, so
     * there is nothing to borrow. Per the borrow-or-declare rule nothing was redrawn: the
     * script-generated placeholder PNGs stay (see assets README). The bricks part is a plain
     * cube_all (no properties).
     */
    private void addMultiBlocks() {
        Block tCokeOven = GTMultiBlocks.COKE_OVEN.get();
        ModelFile tUnformed = cokeOvenModel("multiblock_coke_oven", "multiblock_coke_oven_front");
        ModelFile tFormed = cokeOvenModel("multiblock_coke_oven_formed", "multiblock_coke_oven_front_formed");
        getVariantBuilder(tCokeOven).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            ModelFile tModel = aState.getValue(TileEntityBase10MultiBlockBase.FORMED) ? tFormed : tUnformed;
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        itemModels().withExistingParent("multiblock_coke_oven", modLoc("block/multiblock_coke_oven"));

        Block tBricks = GTMultiBlocks.COKE_OVEN_BRICKS.get();
        // task p29-w3-nbtdesign-parts — the firebricks retexture: multiblock_coke_oven_bricks
        // IS the upstream Fire Bricks (MTE 18000, the reuse ruling), so the placeholder
        // cube_all gives way to the borrowed two-layer firebricks textures
        simpleBlock(tBricks, partModel("multiblock_coke_oven_bricks", "firebricks", 0));
        itemModels().withExistingParent("multiblock_coke_oven_bricks", modLoc("block/multiblock_coke_oven_bricks"));
    }

    /** One cube model over the four-texture key set: down/up/north(front)/south+east+west(side). */
    private ModelFile cokeOvenModel(String aName, String aFrontTexture) {
        return models().cube(aName,
                modLoc("block/multiblock_coke_oven_bottom"), modLoc("block/multiblock_coke_oven_top"),
                modLoc("block/" + aFrontTexture), modLoc("block/multiblock_coke_oven_side"),
                modLoc("block/multiblock_coke_oven_side"), modLoc("block/multiblock_coke_oven_side"));
    }

    /**
     * Task p4-fluid-barrel (W3 provider addition, merge order multiblock→barrel→cover),
     * extended by task p6-barrel-metal-plastic: the three-barrel family (wood/plastic/
     * metal) over their {@code gt6:textures/block/barrel_<material>.png} (the barrel
     * TESR/lid is a feature-layer omission, MultiTileEntityBarrelWood.java:44-54) plus
     * the BlockItem model parenting the block model. The textures are script-generated
     * placeholder PNGs, not JSON.
     *
     * <p>Task p7-barrel-high-tier-melt-bridge spec ④: the twelve high-tier metal drums
     * (Loader_MultiTileEntities.java:2159-2170) share the ONE {@code barrel_metal.png} —
     * every model JSON references the same PNG, so the model count grows with the rows
     * and the PNG count does not.
     *
     * <p>Task p23-barrel-paint-render: the former plain {@code cube_all} placeholder
     * becomes the {@link #tintedCubeAll} single-element form (tintindex 0 on every face,
     * the p21 machine-model grammar over the barrel's ONE-texture shortcut) — the
     * upstream barrel renders its {@code colored/} texture multiplied by mRGBa
     * (MultiTileEntityBarrelWood.java:42-55 {@code new BlockTextureDefault(tTex, mRGBa)};
     * Plastic:42 / Metal:39 / Logistics:45 isomorphic), and the port's placeholder PNGs
     * are full-grayscale (verified), so whole-barrel single-element tinting IS the
     * colored/ layer equivalence. Unpainted barrels ride the {@code -1} white-multiply
     * identity sentinel exactly like the machine face (P21: 0xFFFFFFFF ≡ vanilla no-tint).
     * DECLARED DEVIATION (pool): upstream is TWO-layer — the {@code overlay/} decal
     * texture renders UNCOLOURED on top (BlockTextureMulti); the port borrows a single
     * texture per barrel and has no overlay decal pool yet, so v1 tints the whole
     * barrel with no decal layer (the p22 front-overlay-split precedent for the
     * two-layer follow-up).
     */
    private void addBarrel() {
        addBarrel(GTBarrels.BARREL.get());
        addBarrel(GTBarrels.BARREL_PLASTIC.get());
        addBarrel(GTBarrels.BARREL_METAL.get());
        addBarrel(GTBarrels.BARREL_LOGISTICS.get()); // task p12-barrel-keepfilter-logistics — the :2171 row, own PNG
        for (var tDrum : GTBarrels.METAL_DRUM_BLOCKS.values())
            addBarrel(tDrum.get(), "barrel_metal");
        // task p23-barrel-paint-render: the datagen-JVM census half — 16 barrel blocks,
        // matching the GTBarrels.paintableBlockArray() client registration census (the
        // offline JUnit half walks the generated tree and pins the same 16).
        LOGGER.info("GT6 barrel paint tint: {} barrel models tinted (4 rows + 12 high-tier drums, addBarrel)", mBarrelTintModels);
    }

    /** One tinted cube_all barrel + its BlockItem parent (the p4 wood barrel shape, reused per material row). */
    private void addBarrel(Block aBarrel) {
        addBarrel(aBarrel, aBarrel.getDescriptionId().replace("block.gt6.", ""));
    }

    /** The same shape over an explicit texture tail (the p7 shared-PNG drum family form). */
    private void addBarrel(Block aBarrel, String aTexture) {
        String tName = aBarrel.getDescriptionId().replace("block.gt6.", "");
        // the full "block/..." model path: getBuilder skips the folder prefix for
        // "/"-bearing names (ModelProvider.extendWithFolder — the addPrefixBlocks note).
        simpleBlock(aBarrel, tintedCubeAll("block/" + tName, modLoc("block/" + aTexture)));
        itemModels().withExistingParent(tName, modLoc("block/" + tName));
        mBarrelTintModels++;
    }

    /**
     * Task p4-machine-oven (W2-exclusive provider addition), generalized by task
     * p7-basicmachine-family into {@link #addMachine(Block, String)}: the A-tier machine
     * rendering — a pure datagen blockstate over 4 horizontal facings x 2 active x 2
     * running = 16 variants (HORIZONTAL_FACING, A-tier furnace idiom). Three
     * models (inactive/active/running), each a {@code cube} with the four-texture key set
     * (top/bottom/side/front, spec 8): the front texture is the state carrier, mirroring the
     * upstream getTexture2 overlay pick (MultiTileEntityBasicMachine.java:1014, mActive →
     * mTexturesActive : mRunning → mTexturesRunning : mTexturesInactive); the y rotation maps
     * the FACING property (model-space north = front). Textures are script-generated
     * placeholder PNGs, not JSON.
     */
    private void addOven() {
        addMachine(GTMachines.OVEN.get(), "oven");
        // task p27-oven-heat-t-ladder — the Heat_T ladder rows through the p8 texture-base
        // overload: the tier models derive from aBase (oven_t2/_active/_running) while the
        // FRONT TEXTURES stay on the family T1 "oven" set (upstream NBT_TEXTURE "oven" on
        // all four rows :1288-1291) — the ladder adds zero PNGs.
        addMachine(GTMachines.OVEN_T2.get(), "oven_t2", "oven");
        addMachine(GTMachines.OVEN_T3.get(), "oven_t3", "oven");
        addMachine(GTMachines.OVEN_T4.get(), "oven_t4", "oven");
    }

    /**
     * The addOven generalization (task p7-basicmachine-family ④): one {@code aBase} machine
     * = three models ({@code aBase}, {@code aBase_active}, {@code aBase_running}), the
     * 16-variant blockstate, and the BlockItem model parenting the block model. Since task
     * p28-b-port-overlay-render the three models come from {@link #familyMachineModel} —
     * the family colored six-face body plus the six static state decals (the upstream
     * :1014 two-layer form over the full upstream texture arrays :176-203, not the
     * front-only split the p22 borrow started from).
     *
     * <p>Property identity: both GTOvenBlock.FACING and GTBasicMachineBlock.FACING are the
     * BlockStateProperties.HORIZONTAL_FACING instance, and GTOvenBlock.ACTIVE/RUNNING and
     * GTBasicMachineBlock.ACTIVE/RUNNING are aliases of the same GTBlockProperties single
     * instances (ADR-P16-2) — so the GTOvenBlock property reads below cover the machine
     * blocks too. (The old "BooleanProperty interning" wording here was a wrong theory:
     * vanilla never interned by name; 1.20.1 matched same-named foreign instances only by
     * value equality inside StateHolder, which 1.21.x replaced with identity lookup —
     * distinct instances crash there, hence the single-owner aliases.)
     */
    /**
     * Task p29-w4-eu-bridge — the Roasting Oven 4-ladder (Loader_MultiTileEntities.java
     * :1386-1389): the addMachine three-model walk over the "roaster" family texture set
     * (the borrowed upstream basicmachines/roaster colored 6-set + the overlay state trio
     * x6; the front_active strip is cropped to its first frame — the port carries no
     * animated fronts, the assets/README.md note). The NBT_TEXTURE column is "roaster"
     * over the roasting_oven registry paths, the art-token-fidelity overload.
     */
    private void addRoastingFamilies() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ROASTING_ROWS) {
            addMachine(GTMachines.ROASTING_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p34-machines-bumblelyzer-crucible — the two machine families (the Bumblelyzer
     * EU 5-ladder :1608-1612 + the Crystallisation Crucible HU 4-ladder :1437-1440, every
     * row NBT_TEXTURE riding its family token): the addHeatSmelterFamilies shape verbatim —
     * model names per path, the FRONT TEXTURES stay on the family sets (the borrowed
     * upstream basicmachines/{bumblelyzer,crystallisationcrucible} fronts — the
     * borrow-or-declare rule, zero hand-drawn).
     */
    private void addP34MachineFamilies() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.BUMBLELYZER_ROWS) {
            addMachine(GTMachines.BUMBLELYZER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CRYSTALLISATION_ROWS) {
            addMachine(GTMachines.CRYSTALLISATION_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p34-machines-burner-plantalyzer — the Burner Mixer 4-ladder (20521-20524,
     * NBT_TEXTURE "burnmixer") + the Plantalyzer 5-ladder (20531-20535, "plantalyzer"):
     * the addCanner shape verbatim — model names per path, the front textures stay on the
     * family set (the borrowed upstream basicmachines/{burnmixer,plantalyzer} split
     * fronts, the animated overlay strips flattened to their frame 0 — the W1 borrow
     * pipeline).
     */
    private void addP34MachinePair() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.BURNER_MIXER_ROWS) {
            addMachine(GTMachines.BURNER_MIXER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.PLANTALYZER_ROWS) {
            addMachine(GTMachines.PLANTALYZER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p29-w4-eu-bridge — the three EU-bridge converter families (Loader
     * :815-821/:831-837/:847-853, 15 blocks). NOT paintable machines — the reused
     * {@link gregtech6.block.energy.GT6DynamoBlock} carrier rides the transformer
     * orientable-cube form instead of the addMachine paint walk: one orientable model
     * per family over the borrowed upstream colored front/side pairs (heaters/heat_electric,
     * engines/kinetic_electric, motors/rotation_electric — the assets/README.md faces),
     * FRONT = the emission face (the dynamo-block geometry), the FACING y-rotation the
     * addGearBoxTransformer/transformer form. The five rungs of a family share the model
     * (the tier is not a visual state upstream, the p8 texture ruling).
     */
    private void addElectricBridges() {
        addBridgeFamily(GTMachines.ELECTRIC_HEATER_BLOCKS_BY_PATH, "electric_heater", "bridge_heater");
        addBridgeFamily(GTMachines.ELECTRIC_ENGINE_BLOCKS_BY_PATH, "electric_engine", "bridge_engine");
        addBridgeFamily(GTMachines.ELECTRIC_MOTOR_BLOCKS_BY_PATH, "electric_motor", "bridge_motor");
    }

    /**
     * Task p32-qu-laser-domain — the CO2 Laser + Laser Absorber families (Loader
     * :930-934/:976-980, 10 blocks): the SAME reused-dynamo-carrier orientable form as the
     * bridges, over the borrowed upstream colored front/side pairs (lasers/laser_electric,
     * laserabsorbers/electric_laser — the assets/README.md faces). FRONT = the emission
     * face (the laser pushes LU out the front; the absorber takes the beam on the BACK and
     * pushes EU out the front — the face geometry rides the BE, the visual is this shared
     * orientable). The beam itself is NOT rendered (the task card: 光束 defer, visual =
     * the static block face).
     */
    private void addLaserFamilies() {
        addBridgeFamily(GT6Lasers.CO2_LASER_BLOCKS_BY_PATH, "co2_laser", "laser_electric");
        addBridgeFamily(GT6Lasers.LASER_ABSORBER_BLOCKS_BY_PATH, "laser_absorber", "laser_absorber");
        // task p32-qu-energizer — the third laser-converter family rides its own texture
        // base (the amber-tinted quantum_energizer pair, the assets/README.md face)
        addBridgeFamily(gregtech6.registry.GT6QuantumEnergizers.QUANTUM_ENERGIZER_BLOCKS_BY_PATH, "quantum_energizer", "quantum_energizer");
    }

    /**
     * One bridge family: the orientable cube + the five rung blockstates + the item parents
     * (the transformer form). Task p38-c2-controller-tint: the re-declared body element
     * carries {@code tintindex 0} on every face (the machineModel grammar — the child's
     * elements replace the parent's, the partModel precedent) — the grayscale colored
     * front/side pairs multiply the row's NBT_MATERIAL (the upstream
     * {@code BlockTextureMulti(BlockTextureDefault(colored, mRGBa), overlay)} form:
     * heater :56-59 / engine :244-249 / motor :39-42 / laser :46-49 / absorber :41-44 —
     * the census ruling: the gray plate IS the tint seat, not a colour declaration); the
     * bake/ItemColor consumers ride GTMachineTintModel/GTItemPaintTint. The quantum
     * energizer rides this same builder with NO material column and NO registration —
     * the tintindex stays the white identity there (its amber art is pre-tinted, the
     * card exemption).
     */
    private void addBridgeFamily(java.util.Map<String, RegistryObject<Block>> aBlocks, String aFamily, String aTexture) {
        BlockModelBuilder tModel = models().orientable(aTexture,
                modLoc("block/" + aTexture + "_side"), modLoc("block/" + aTexture + "_front"), modLoc("block/" + aTexture + "_side"));
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture(aDir == Direction.NORTH ? "#front" : "#side").tintindex(0).cullface(aDir))
                .end();
        for (RegistryObject<Block> tHandle : aBlocks.values()) {
            getVariantBuilder(tHandle.get()).forAllStates(aState -> {
                // the vanilla horizontal-facing rotation map (the transformer verbatim form)
                Direction tFacing = aState.getValue(gregtech6.block.energy.GT6DynamoBlock.FACING);
                return ConfiguredModel.builder()
                        .modelFile(tModel)
                        .rotationY((int) (tFacing.toYRot() + 180) % 360)
                        .build();
            });
        }
        itemModels().withExistingParent(aFamily, modLoc("block/" + aTexture));
        for (String tPath : aBlocks.keySet()) {
            if (!tPath.equals(aFamily)) itemModels().withExistingParent(tPath, modLoc("item/" + aFamily));
        }
    }

    /**
     * Task p32-magic-absorber — the Magic Field Absorber single (Loader :1005, id 10180):
     * the cube_directional model over the borrowed upstream colored base (assets/README.md
     * — the four upstream colored faces are one byte-identical grayscale file) with the
     * SIX-WAY facing rotation map (the vanilla dispenser form: north = identity,
     * down x=90, up x=270, the horizontal y band) — the FACING face is the output face,
     * the trophy seat is the TOP. The overlay/overlay_active activity visual is the W2
     * render pool (the static-face posture).
     */
    private void addMagicAbsorber() {
        BlockModelBuilder tModel = models().getBuilder("magic_absorber")
                .parent(models().getExistingFile(mcLoc("block/cube_directional")))
                .texture("particle", modLoc("block/magic_absorber_base"))
                .texture("down", modLoc("block/magic_absorber_base"))
                .texture("up", modLoc("block/magic_absorber_base"))
                .texture("north", modLoc("block/magic_absorber_base"))
                .texture("south", modLoc("block/magic_absorber_base"))
                .texture("west", modLoc("block/magic_absorber_base"))
                .texture("east", modLoc("block/magic_absorber_base"));
        // the re-declared body element (tintindex 0 = the material tint, task
        // p38-c2-controller-tint — the child's elements replace the cube_directional
        // parent's): the grayscale base PNG multiplies the row's NBT_MATERIAL MT.Pd (the
        // upstream MultiTileEntityMagicFieldAbsorber.getTexture2 :133-136
        // BlockTextureMulti(BlockTextureDefault(colored, mRGBa), overlay) form); the
        // bake/ItemColor consumers ride GTMachineTintModel/GTItemPaintTint
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        getVariantBuilder(GT6MagicAbsorbers.MAGIC_ABSORBER_BLOCKS_BY_PATH.get("magic_absorber").get()).forAllStates(aState -> {
            Direction tFacing = aState.getValue(gregtech6.block.energy.GT6MagicAbsorberBlock.FACING);
            int tX = tFacing == Direction.DOWN ? 90 : tFacing == Direction.UP ? 270 : 0;
            int tY = switch (tFacing) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0; // NORTH and the two verticals carry the x rotation only
            };
            return ConfiguredModel.builder().modelFile(tModel).rotationX(tX).rotationY(tY).build();
        });
        itemModels().withExistingParent("magic_absorber", modLoc("block/magic_absorber"));
    }

    private void addMachine(Block aBlock, String aBase) {
        addMachine(aBlock, aBase, aBase);
    }

    /**
     * The texture-base overload (task p8-machine-tiers-doinject ⑧): the model names derive
     * from {@code aBase} (so shredder_t2 gets shredder_t2/_active/_running models + its own
     * 16-variant blockstate + the item parent) while the TEXTURES stay on the family's
     * T1 set ({@code aTextureBase_colored_*} + the {@code aTextureBase_overlay_*} state
     * trio, task p22-paint-front-overlay-split extended to all six faces by task
     * p28-b-port-overlay-render) — the tier is not a visual state upstream (the rows
     * :1294-1309 share the NBT_TEXTURE per family), so the ladder adds zero PNGs.
     */
    private void addMachine(Block aBlock, String aBase, String aTextureBase) {
        ModelFile tInactive = familyMachineModel(aBase, aTextureBase, "");
        ModelFile tActive = familyMachineModel(aBase + "_active", aTextureBase, "_active");
        ModelFile tRunning = familyMachineModel(aBase + "_running", aTextureBase, "_running");
        getVariantBuilder(aBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(GTOvenBlock.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            ModelFile tModel = aState.getValue(GTOvenBlock.ACTIVE) ? tActive
                    : aState.getValue(GTOvenBlock.RUNNING) ? tRunning : tInactive;
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        itemModels().withExistingParent(aBase, modLoc("block/" + aBase));
    }

    /**
     * One cube model over the four-texture key set: down/up/north(front)/south+east+west(side).
     * The p22 two-element form. ACT-only since task p28-b-port-overlay-render (the
     * Advanced Crafting Table is the one machine family with no borrowed side art — the
     * craftingtables/advanced upstream group ships fronts only — so it keeps the shared
     * oven placeholder body and the single front decal; every addMachine family moved to
     * {@link #familyMachineModel}).
     *
     * <p>Task p21-paintable-tint-render: the vanilla {@code block/cube} element is re-declared
     * in the child with {@code tintindex 0} on EVERY face — the machine cube is six-texture,
     * so the {@link #tintedCubeAll} {@code #all} shortcut does not apply and the per-face
     * element form is required. Upstream canonical: every faced face multiplies the
     * grayscale texture by mRGBa (MultiTileEntityBasicMachine.java:1014 getTexture2,
     * white = unpainted = unchanged), so all three models (inactive/active/running — built
     * from {@link #addMachine}) tint identically; the runtime consumer is the
     * {@code GTMachinePaintTint} BlockColor. Output is otherwise equivalent to the former
     * parent-only {@code models().cube} form (no explicit UVs — they default to the element
     * bounds; the {@code block/cube} parent keeps the display transforms and its
     * {@code particle = #down} binding).
     *
     * <p>Task p22-paint-front-overlay-split: the former single BAKED front composite
     * (colored base + state overlay flattened, the P20 bake) is retired for the upstream
     * TWO-LAYER form (:1014 = BlockTextureMulti(BlockTextureDefault(colored, mRGBa),
     * BlockTextureDefault(state overlay)) with the second layer UNCOLOURED,
     * BlockTextureDefault.java:179-180 — the state decal is never tinted by the paint).
     * The north body face now carries the plain grayscale {@code aFrontTexture}
     * ({@code <family>_colored_front}), and a second thin element — 16x16x0.01 floating
     * 0.01 north of the body plane (inside the vanilla [-16,32] element tolerance) —
     * carries the state decal {@code aOverlayTexture} with NO tintindex (FaceBuilder's
     * default -1 omits the key), so {@code GTMachinePaintTint} (which only maps
     * tintindex 0) cannot re-tint it. The decal's single north face keeps
     * {@code cullface north}, mirroring the body cube's own north cullface: a solid
     * neighbor to the north hides body face and decal together (no decal floating
     * behind a wall), and the 0.01 offset keeps the decal off the body plane (no
     * coplanar z-fighting). The old baked fronts are retired in assets/README.md.
     */
    private ModelFile machineModel(String aName, String aFrontTexture, String aOverlayTexture) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc("block/oven_bottom"))
                .texture("up", modLoc("block/oven_top"))
                .texture("north", modLoc("block/" + aFrontTexture))
                .texture("south", modLoc("block/oven_side"))
                .texture("west", modLoc("block/oven_side"))
                .texture("east", modLoc("block/oven_side"))
                .texture("overlay", modLoc("block/" + aOverlayTexture));
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        tModel.element()
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay").cullface(Direction.NORTH)
                .end();
        mMachineTintModels++;
        return tModel;
    }

    /**
     * Task p28-b-port-overlay-render — the full-family machine model over the borrowed
     * upstream basicmachines texture arrays (MultiTileEntityBasicMachine.java:176-203:
     * mTexturesMaterial + the mTexturesInactive/Active/Running trio, each the six-entry
     * array [bottom, top, left, front, right, back]). The upstream :1014 two-layer form
     * over ALL six faces, not the p22 front-only split:
     * <ul>
     * <li><b>body (element 0)</b> — the full 0..16 cube, every face tintindex 0 (the
     *     mRGBa paint seat, unchanged from p21), bound to the family's OWN colored art
     *     ({@code <family>_colored_bottom/top/front/back/left/right}) instead of the shared
     *     oven placeholders. The side art is no longer the oven's: every family ships its
     *     own colored six-set upstream (the A-card borrow, assets/README.md).</li>
     * <li><b>six state decals (elements 1-6)</b> — thin 0.01 plates floating 0.01 outside
     *     each face (the p22 front-decal geometry generalized), each a single face with NO
     *     tintindex (the UNCOLOURED second layer, BlockTextureDefault.java:179-180) and
     *     cullface synced with the body's own face (the p22 anti-z-fight + cull pairing).
     *     The decal state trio is selected per model by {@code aStateSuffix} — "" /
     *     {@code _active} / {@code _running} = the upstream :1014 pick
     *     {@code (mActive ? mTexturesActive : mRunning ? mTexturesRunning : mTexturesInactive)}
     *     — driven purely by the existing ACTIVE/RUNNING blockstate variants (no new
     *     state, no BE read: the port art is statically baked, the census ruling).</li>
     * </ul>
     *
     * <p>Face mapping: the texture keys are named by the UPSTREAM art token
     * ({@code overlay_front/back/left/right/top/bottom}), the model faces map per the
     * upstream FACING_ROTATIONS table (CS.java:528-537) with the model-space front at
     * north: for a north-facing machine the table binds west→right and east→left, so the
     * west face carries {@code #overlay_right}/{@code _colored_right} and the east face
     * {@code #overlay_left}/{@code _colored_left}; the blockstate y rotations reproduce
     * the remaining facings exactly (the same mapping the GTOvenOverlayModel
     * textureFaceOf table mirrors at runtime). UVs stay the vanilla defaults (the p22
     * precedent); the 1.7.10 "stupidly mirrored" north/east icon quirk (CS.java:516-518)
     * is NOT compensated — the side-decal orientations join the P28 runClient目验池.
     *
     * <p>Oven interaction note: the P9 GTOvenOverlayModel wraps the oven ladder's baked
     * models and stacks its own snapshot-driven cutout overlay quads on the solid layer —
     * with these static decals the active/running oven draws the same upstream art twice
     * (byte-same source PNGs, 0.002 vs 0.01 offsets). Declared overlap: retiring the P9
     * dynamic layer is a separate card's call (render code, out of this card's scope).
     */
    private ModelFile familyMachineModel(String aName, String aTextureBase, String aStateSuffix) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc("block/" + aTextureBase + "_colored_bottom"))
                .texture("up", modLoc("block/" + aTextureBase + "_colored_top"))
                .texture("north", modLoc("block/" + aTextureBase + "_colored_front"))
                .texture("south", modLoc("block/" + aTextureBase + "_colored_back"))
                .texture("west", modLoc("block/" + aTextureBase + "_colored_right")) // FACING_ROTATIONS[north][west]=4=right
                .texture("east", modLoc("block/" + aTextureBase + "_colored_left")) // FACING_ROTATIONS[north][east]=2=left
                .texture("overlay_front", modLoc("block/" + aTextureBase + "_overlay_front" + aStateSuffix))
                .texture("overlay_back", modLoc("block/" + aTextureBase + "_overlay_back" + aStateSuffix))
                .texture("overlay_left", modLoc("block/" + aTextureBase + "_overlay_left" + aStateSuffix))
                .texture("overlay_right", modLoc("block/" + aTextureBase + "_overlay_right" + aStateSuffix))
                .texture("overlay_top", modLoc("block/" + aTextureBase + "_overlay_top" + aStateSuffix))
                .texture("overlay_bottom", modLoc("block/" + aTextureBase + "_overlay_bottom" + aStateSuffix));
        // element 0 — the tinted body cube (p21/p22 shape, only the texture bindings changed).
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        // elements 1-6 — the six state decals: one thin plate per face, the p22 front-decal
        // form generalized (0.01 offset out, single face, no tintindex, cullface synced).
        tModel.element() // front (north)
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_front").cullface(Direction.NORTH)
                .end();
        tModel.element() // back (south)
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_back").cullface(Direction.SOUTH)
                .end();
        tModel.element() // left art (east face — FACING_ROTATIONS[north][east]=2=left)
                .from(16.0F, 0.0F, 0.0F).to(16.01F, 16.0F, 16.0F)
                .face(Direction.EAST).texture("#overlay_left").cullface(Direction.EAST)
                .end();
        tModel.element() // right art (west face — FACING_ROTATIONS[north][west]=4=right)
                .from(-0.01F, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                .face(Direction.WEST).texture("#overlay_right").cullface(Direction.WEST)
                .end();
        tModel.element() // bottom (down)
                .from(0.0F, -0.01F, 0.0F).to(16.0F, 0.0F, 16.0F)
                .face(Direction.DOWN).texture("#overlay_bottom").cullface(Direction.DOWN)
                .end();
        tModel.element() // top (up)
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_top").cullface(Direction.UP)
                .end();
        mMachineTintModels++;
        return tModel;
    }

    /**
     * One cube_all model per item pipe row (the blockstate name mirrors the block registry
     * path), a variant per CONNECTIONS mask value (0..63), and the BlockItem model
     * parenting the block model — the addFluidPipe shape over the two shared placeholders.
     */
    /**
     * Task p32-logistics-lv2 — the logistics wire: the addItemPipe shape (one cube_all
     * model over the blockstate-name PNG, a variant per CONNECTIONS mask value 0..63
     * out of forAllStates, the BlockItem model parenting the block model) over its own
     * placeholder PNG.
     */
    private void addLogisticsWire(Block aWire) {
        String tName = aWire.getDescriptionId().replace("block.gt6.", "");
        var tModel = models().cubeAll(tName, modLoc("block/logistics_wire"));
        getVariantBuilder(aWire).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
        itemModels().withExistingParent(tName, modLoc("block/" + tName));
    }

    private void addItemPipe(Block aPipe, boolean aRestrictive) {
        String tName = aPipe.getDescriptionId().replace("block.gt6.", "");
        var tModel = models().cubeAll(tName, modLoc(aRestrictive ? "block/item_pipe_restrictive" : "block/item_pipe"));
        getVariantBuilder(aPipe).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
        itemModels().withExistingParent(tName, modLoc("block/" + tName));
    }

    /**
     * One cube_all model per pipe tier (the blockstate name mirrors the block registry
     * path), a variant per CONNECTIONS mask value (0..63), and the BlockItem model
     * parenting the block model.
     */
    private void addFluidPipe(Block aPipe) {
        String tName = aPipe.getDescriptionId().replace("block.gt6.", "");
        var tModel = models().cubeAll(tName, modLoc("block/fluid_pipe_wood"));
        getVariantBuilder(aPipe).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
        itemModels().withExistingParent(tName, modLoc("block/" + tName));
    }

    /**
     * Task p7-d2-cable spec ⑥ — the electric wires, the pipe section shape: one shared
     * cube_all model over {@code gt6:textures/block/wire_electric.png} (both variants
     * reference the same PNG, the p7 shared-PNG drum family form) with a variant per
     * {@link gregtech6.block.wire.GTWireBlock} CONNECTIONS mask value (0..63, the 6-bit
     * connection state — the 64-variant exhaustive listing comes out of forAllStates, no
     * hand-written JSON) plus the BlockItem model parenting the block model. The texture
     * is an inline-script generated placeholder PNG, not JSON.
     */
    private void addWire(Block aWire) {
        String tName = aWire.getDescriptionId().replace("block.gt6.", "");
        var tModel = models().cubeAll(tName, modLoc("block/wire_electric"));
        getVariantBuilder(aWire).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
        itemModels().withExistingParent(tName, modLoc("block/" + tName));
    }

    /**
     * Task p9-wire-family-w1 spec ⑥, MODEL TARGET UPGRADED by task p9-wire-family-w2
     * (the card's "swap the model target to GTWireBakedModel + textured models"): the 620-block wire family,
     * looped over the {@link GTWireSpecs} table. The blockstate form is UNCHANGED from W1 —
     * per block ONE blockstate JSON whose SINGLE property-less variant (the {@code ""} key,
     * emitted by {@code partialState().setModels}) maps ALL 64
     * {@link gregtech6.block.wire.GTWireBlock} CONNECTIONS states onto a shared model
     * (vanilla resolves an empty variant key as no predicates = a wildcard over every
     * state, ModelBakery.java:173). What changed is the shared model TARGET: instead of the
     * W1 placeholder cube, the target is one SHARED tinted model per live texture set
     * (the addPrefixBlocks merge rule — the distinct {@code materialicons/<set>/wire.png}
     * first entries across the 30 wire rows; census 2026-09-01: exactly
     * {@code copper, shiny, metallic, dull, quartz, rad, none} — 7 sets, "none" =
     * upstream SET_NONE for the empty-list Superconductor row, every PNG borrowed
     * byte-identical, assets/README.md). The shared models carry {@code tintindex 0} (the
     * runtime {@link gregtech6.client.wire.GTWireTint} material dye); the true
     * connection-aware geometry (core + arms) is installed at bake time by
     * {@link gregtech6.client.wire.GTWireClientListener}, which replaces the per-state
     * AND item baked keys — this JSON model stays as the fallback carrier and the item
     * parent. A cable's shared fallback shows no insulation shell (the tier texture is
     * per-diameter) — a fallback-only simplification, the world form is the baked model.
     * Still NO 64-variant x 620 listing (the ADR red line).
     */
    private void addWireFamily(Map<String, ModelFile> aShared) {
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) {
            String tName = GTWireSpecs.registryName(tVariant);
            String tSet = blockSetOf(tVariant.row().material().get());
            String tModelName = "block/materialicons/" + tSet + "/wire";
            ModelFile tModel = aShared.computeIfAbsent(tModelName,
                    tKey -> tintedCubeAll(tKey, modLoc("block/materialicons/" + tSet + "/wire")));
            getVariantBuilder(GTWires.FAMILY_BY_NAME.get(tName).get())
                    .partialState().setModels(new ConfiguredModel(tModel));
            itemModels().withExistingParent(tName, modLoc(tModelName));
        }
        LOGGER.info("GT6 wire family: {} blockstates over {} shared (set) models",
                GTWireSpecs.EXPECTED_VARIANTS, aShared.size());
    }

    /**
     * Task p10-wire-redstone-family — the redstone-wire family (6 blocks over
     * {@link GTWireSpecs#REDSTONE_ROWS}), the addWireFamily pipe over the redstone variant
     * list. The three materials (RedAlloy/Signalum/Lumium) all resolve to the
     * {@code copper} texture set (clloy/clloymachine construct with SET_COPPER —
     * MT.java:697/701), which the W2 borrow already shipped, so zero new PNGs. The same
     * single property-less variant wildcard maps the 64 CONNECTIONS states per block onto
     * the shared tinted model.
     *
     * <p>Task p11-wire-brightness UPGRADED the runtime target (the fiber-card shape): the
     * listener table ({@link gregtech6.client.wire.GTWireClientListener#buildParams}) now
     * covers the six redstone paths, so every per-state key
     * {@code gt6:wire_red_alloy#connections=0..63} et al AND the item keys bake into the
     * {@link gregtech6.client.wire.GTWireBakedModel} electric form — the block no longer
     * RENDERS through this JSON. The shared tinted cube stays generated UNCHANGED as the
     * per-state key carrier + item parent (spec-pinned: the blockstate's single wildcard
     * variant is what mints the 64 per-state keys the listener swaps), and as the baked
     * fallback safety net if the listener never fires. The family's render specifics live
     * elsewhere: the jacket colour is the per-family tint ({@code 96,64,64},
     * MultiTileEntityWireRedstoneInsulated :184-185, in gregtech6.client.wire.GTWireTint)
     * and the bare-wire {@code mState > 0} texture-fullbright (:81-82) is the declared
     * GTWireBakedModel deviation.
     */
    private void addRedstoneWireFamily(Map<String, ModelFile> aShared) {
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) {
            String tName = GTWireSpecs.registryName(tVariant);
            String tSet = blockSetOf(tVariant.row().material().get());
            String tModelName = "block/materialicons/" + tSet + "/wire";
            ModelFile tModel = aShared.computeIfAbsent(tModelName,
                    tKey -> tintedCubeAll(tKey, modLoc("block/materialicons/" + tSet + "/wire")));
            getVariantBuilder(GTWires.REDSTONE_BY_NAME.get(tName).get())
                    .partialState().setModels(new ConfiguredModel(tModel));
            itemModels().withExistingParent(tName, modLoc(tModelName));
        }
        LOGGER.info("GT6 redstone wire family: {} blockstates over the shared (set) model pool",
                GTWireSpecs.EXPECTED_REDSTONE_VARIANTS);
    }

    /**
     * Task p10-wire-laser-placeholder — the laser-wire family (1 block over
     * {@link GTWireSpecs#LASER_ROWS}), the addWireFamily pipe over the laser variant list.
     * The row material is MT.NULL (upstream NBT_MATERIAL MT.NULL, Loader:1815), which
     * resolves to the {@code none} set (GTWireTextures.blockSetOf empty-list rule) — the
     * W2 borrow already shipped that set (the Superconductor row), so zero new PNGs.
     *
     * <p>Task p11-wire-fiber-texture UPGRADED the runtime target: the listener table
     * ({@link gregtech6.client.wire.GTWireClientListener#buildParams}) now covers
     * {@code wire_laser}, so every per-state key AND the item key bake into the
     * {@link gregtech6.client.wire.GTWireBakedModel} fiber form (the fixed FIBER_WIRE
     * + FIBER_WIRE_OVERLAY pair, MultiTileEntityWireLaser :121-122) — the block no longer
     * RENDERS through this JSON. The shared tinted cube stays generated UNCHANGED as the
     * per-state key carrier + item parent (the addWireFamily architecture, spec-pinned:
     * the blockstate's single wildcard variant is what mints the 64 per-state
     * {@code gt6:wire_laser#connections=N} keys the listener swaps), and as the baked
     * fallback safety net if the listener never fires. This is the exact shape the 620
     * electric rows and — since task p11-wire-brightness — the 6 redstone rows have;
     * every wire family now renders through the per-state baked model.
     */
    private void addLaserWireFamily(Map<String, ModelFile> aShared) {
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.laserVariants()) {
            String tName = GTWireSpecs.registryName(tVariant);
            String tSet = blockSetOf(tVariant.row().material().get());
            String tModelName = "block/materialicons/" + tSet + "/wire";
            ModelFile tModel = aShared.computeIfAbsent(tModelName,
                    tKey -> tintedCubeAll(tKey, modLoc("block/materialicons/" + tSet + "/wire")));
            getVariantBuilder(GTWires.LASER_BY_NAME.get(tName).get())
                    .partialState().setModels(new ConfiguredModel(tModel));
            itemModels().withExistingParent(tName, modLoc(tModelName));
        }
        LOGGER.info("GT6 laser wire family: {} blockstates over the shared (set) model pool",
                GTWireSpecs.EXPECTED_LASER_VARIANTS);
    }

    /**
     * Task p8-d4-energy-source spec ② — the test energy source: one cube_all over
     * {@code gt6:textures/block/energy_source.png} (the borrowed upstream
     * solarpanel_electric_8eu side texture, the p7-gui-family byte-identical form) plus
     * the BlockItem model parenting the block model. No properties, a single variant.
     */
    private void addEnergySource() {
        Block tSource = GTEnergySources.ENERGY_SOURCE.get();
        simpleBlock(tSource, models().cubeAll("energy_source", modLoc("block/energy_source")));
        itemModels().withExistingParent("energy_source", modLoc("block/energy_source"));
    }

    /**
     * Task p26-eu-bridge-outbound (TAIL-APPENDED row, the shared serial file) — the FE
     * battery fixture: the addEnergySource shape verbatim, one cube_all over the SHARED
     * placeholder {@code gt6:textures/block/energy_source.png} (no new PNG — the borrow
     * posture the p20 testmachine rows pinned), plus the BlockItem model parenting the block
     * model. No properties, a single variant.
     */
    private void addFeBattery() {
        Block tBattery = GT6FeBatteries.FE_BATTERY.get();
        simpleBlock(tBattery, models().cubeAll("fe_battery", modLoc("block/energy_source")));
        itemModels().withExistingParent("fe_battery", modLoc("block/fe_battery"));
    }

    /**
     * Task p28-b-fe-converter-machine (TAIL-APPENDED row, the shared serial file) — the
     * ULV FE→EU converter: the addFeBattery shape verbatim, one cube_all over the SHARED
     * placeholder {@code gt6:textures/block/energy_source.png} (no new PNG — the borrow
     * posture), plus the BlockItem model parenting the block model. No properties, a
     * single variant.
     */
    private void addFeConverter() {
        Block tConverter = GT6FeConverters.FE_CONVERTER.get();
        simpleBlock(tConverter, models().cubeAll("fe_converter", modLoc("block/energy_source")));
        itemModels().withExistingParent("fe_converter", modLoc("block/fe_converter"));
    }

    /**
     * Task p28-b-fe-converter-machine (TAIL-APPENDED row, the shared serial file) — the
     * FE source fixture: the addFeBattery shape verbatim, one cube_all over the SHARED
     * placeholder, plus the BlockItem model parenting the block model.
     */
    private void addFeSource() {
        Block tSource = GT6FeBatteries.FE_SOURCE.get();
        simpleBlock(tSource, models().cubeAll("fe_source", modLoc("block/energy_source")));
        itemModels().withExistingParent("fe_source", modLoc("block/fe_source"));
    }

    /**
     * Task p20-testmachine-blockstates — the two dev BE-framework blocks (task p3-be-framework,
     * {@link GTBlockEntities#TEST_MACHINE} / {@link GTBlockEntities#TEST_MACHINE_IDLE}): the last
     * model-less blocks in the registry face (P20 texture census, research card
     * tasks.p20-research-texture-census — every other registered blockstate+item model was
     * already zero-gap). The addEnergySource shape verbatim: one cube_all per block over the
     * SHARED placeholder {@code gt6:textures/block/example_chest.png} (no new PNG per the card
     * scope — the placeholder-to-upstream art swap stays a P20 wave item; the census pin d
     * resolves the layer0 against the static tree), plus the item model parenting the block
     * model. Datagen-only JSON: the dev blocks register no BlockItem (the census "dev blocks
     * have no item" note), the item model row merely closes the model-resolution loop the way
     * TestMachineBlock.java:21-22 expected when it deferred this datagen to the example machine
     * card. No properties, a single variant each — the ticking/idle split is the BE ticker
     * (TestMachineBlock.java:44-60), not a blockstate.
     */
    private void addTestMachines() {
        simpleBlock(GTBlockEntities.TEST_MACHINE.get(),
                models().cubeAll("test_machine", modLoc("block/example_chest")));
        itemModels().withExistingParent("test_machine", modLoc("block/test_machine"));
        simpleBlock(GTBlockEntities.TEST_MACHINE_IDLE.get(),
                models().cubeAll("test_machine_idle", modLoc("block/example_chest")));
        itemModels().withExistingParent("test_machine_idle", modLoc("block/test_machine_idle"));
    }

    /**
     * Task p12-engine-crank — the Hand Crank: one cube_all over
     * {@code gt6:textures/block/crank.png} (the borrowed upstream crank front icon, the
     * energy_source byte-identical-borrow form; assets/README.md attribution), single
     * model for every state — the FACING property drives the EMIT side, not the visuals
     * (the idle/spin dual texture of upstream getRenderPasses2 :125-130 is the render
     * pool item). The empty-partial variant key applies the model to all four facings.
     */
    private void addCrank() {
        Block tCrank = GT6Kinetics.CRANK.get();
        simpleBlock(tCrank, models().cubeAll("crank", modLoc("block/crank")));
        itemModels().withExistingParent("crank", modLoc("block/crank"));
    }

    /**
     * Task p12-axle-family — the 44 axle blocks (11 materials x 4 diameters): ONE shared
     * {@code cube_column} model over the borrowed static {@code gt6:block/axle} texture
     * (end == side, the GTWires shared-texture form; assets/README.md attribution) and a
     * 3-variant blockstate per block driving the AXIS property — the vanilla
     * {@code axisBlock} rotation map (y = none, x = 90/90, z = 90/180) without the
     * RotatedPillarBlock type coupling. The 44 BlockItem models parent the shared block
     * model (44 one-line JSONs, the crank {@code itemModels()} precedent carried per row).
     */
    private void addAxles() {
        ModelFile tModel = models().cubeColumn("axle", modLoc("block/axle"), modLoc("block/axle"));
        for (var tAxle : GT6Kinetics.AXLE_BLOCKS.values()) {
            getVariantBuilder(tAxle.get()).forAllStates(aState -> switch (aState.getValue(GTAxleBlock.AXIS)) {
                case X -> new ConfiguredModel[] {new ConfiguredModel(tModel, 90, 90, false)};
                case Y -> new ConfiguredModel[] {new ConfiguredModel(tModel)};
                case Z -> new ConfiguredModel[] {new ConfiguredModel(tModel, 90, 180, false)};
            });
            itemModels().withExistingParent(tAxle.getId().getPath(), modLoc("block/axle"));
        }
    }

    /**
     * Task p12-tap-funnel-attachment spec ⑤ — the 12 wall attachments: one cube_all
     * per row over the TWO family textures, {@code tap.png} (borrowed from upstream
     * {@code machines/tools/tap/colored/side.png}) and {@code funnel.png} (upstream
     * {@code machines/tools/funnel/colored/side.png}), the barrel_metal shared-PNG
     * precedent — every row's model JSON references the family PNG, the model count
     * grows with the rows and the PNG count does not. The upstream texture stack is the
     * mRGBa-tinted colored layer + an overlay pass (MultiTileEntityFluidTap.java:181-208
     * three-pass faucet/spout boxes); the port shows the grayscale side icon un-tinted
     * over the WHOLE cube (the thin-plate getShape is the collision-free outline), the
     * per-face rotated plate model and the tint riding the render pool card (the crank
     * single-model deviation repeated). The empty-partial variant key applies the model
     * to all six facings.
     */
    private void addAttachments() {
        for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) {
            Block tBlock = GT6Attachments.BLOCKS_BY_PATH.get(tRow.path()).get();
            String tTexture = tRow.family() == gregtech6.block.attachment.GTAttachmentSmallBlock.Family.TAP ? "tap" : "funnel";
            simpleBlock(tBlock, models().cubeAll(tRow.path(), modLoc("block/" + tTexture)));
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
        }
    }

    /**
     * Task p12-engine-steam — the Steam Engine family (all 28 rows of
     * {@link GT6Kinetics#STEAM_ENGINES}, the no-upstream-subset ruling): ONE shared
     * oriented cube model for the whole family — front (the KU emit face) / back (the
     * steam face) / side over the three borrowed upstream
     * {@code machines/engines/kinetic_steam/colored/} icons (assets/README.md sha256
     * attribution), rotated per {@code FACING} exactly like the machine ladder
     * ({@code addMachine}) but with NO active/running split — upstream keys the visuals
     * on synced mState/mActive byte data (getTexture2 :263-273, the seven-pass bespoke
     * renderer), which is the render-pool item; the port machine blocks carry no
     * active-state blockstate property. The 26 variants are visually identical here
     * (upstream tints the grayscale icons per material mRGBa — the crank-card un-tinted
     * deviation, the runtime tint rides the render pool).
     */
    private void addSteamEngines() {
        ModelFile tModel = models().cube("steam_engine",
                modLoc("block/steam_engine_side"), modLoc("block/steam_engine_side"),   // bottom/top
                modLoc("block/steam_engine_front"), modLoc("block/steam_engine_back"),  // north(front)/south(back)
                modLoc("block/steam_engine_side"), modLoc("block/steam_engine_side"));  // west/east
        for (GT6Kinetics.SteamEngineRow tRow : GT6Kinetics.STEAM_ENGINES) {
            Block tBlock = GT6Kinetics.STEAM_ENGINE_BLOCKS.get(tRow.path()).get();
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(GT6Kinetics.SteamEngineBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), modLoc("block/steam_engine"));
        }
    }

    /**
     * Task p12-engine-diesel — the 8 diesel engine tiers (Loader_MultiTileEntities.java
     * :721-729): ONE shared cube_all over the borrowed upstream motor_liquid front icon
     * {@code gt6:textures/block/diesel_engine.png} (assets/README.md attribution) and ONE
     * blockstate variant per block — the FACING property drives the EMIT side, not the
     * visuals, exactly the crank ruling (the upstream front/back/sides colored +
     * overlay_active texture family, MultiTileEntityMotorLiquid.java:242-254 +
     * getTexture2 :216-221, is the render pool item; the per-material mRGBa tint is the
     * same pool). The 8 BlockItem models parent the shared block model (the crank
     * {@code itemModels()} form per row).
     */
    private void addDieselEngines() {
        for (var tBlock : GT6Kinetics.DIESEL_BLOCKS.values()) {
            simpleBlock(tBlock.get(), models().cubeAll("diesel_engine", modLoc("block/diesel_engine")));
            itemModels().withExistingParent(tBlock.getId().getPath(), modLoc("block/diesel_engine"));
        }
    }

    /**
     * Task p28-c-water-wheel — the Water Wheel: one cube_all over the ORIGINAL
     * {@code gt6:block/water_wheel} texture (the kTFRUAddon PNG is NOT borrowed — the
     * research-card license ruling: AGPL artwork never enters this repo, the wheel
     * texture is drawn for the port). ONE model over every AXIS state (simpleBlock =
     * partialState().setModels() matches all states, the diesel FACING precedent) — the
     * blade spin visual is the declared defer (the GT6Kinetics.WATER_WHEEL doc; the
     * functional ACTIVE output rides the BE). The BlockItem parents the shared block
     * model (the crank form).
     */
    private void addWaterWheel() {
        Block tWheel = GT6Kinetics.WATER_WHEEL.get();
        simpleBlock(tWheel, models().cubeAll("water_wheel", modLoc("block/water_wheel")));
        itemModels().withExistingParent("water_wheel", modLoc("block/water_wheel"));
    }

    /**
     * Task p28-c-ulv-dynamo-row — the Electric Dynamo T0 ULV row: the addFeConverter
     * shape (one cube_all over the SHARED placeholder {@code block/energy_source.png}, no
     * new PNG — the p20 borrow posture) with the empty-partial wildcard variant covering
     * the FACING property (the addAttachments convention — the partialState().setModels()
     * empty key matches all four facings, the water wheel "static facing" precedent). The
     * facing is a functional IO face (FRONT out EU / BACK in RU), not a visual state in
     * this placeholder — the W2 render card upgrades the whole dynamo family to the
     * borrowed upstream dynamos art (the family's five LV..IV rows stay the W2 surface;
     * this card owns ONLY the new tier per its SPEC).
     */
    /**
     * Task p29-w4-battery-storage — the twelve BatteryBox rows: one cube_all per row over
     * the BAKED upstream energystorages side sprites (small/large, the
     * bake_battery_textures.py composites; assets/README.md attribution). The FACING
     * property is a functional IO face (FRONT out EU / ALL-BUT-FRONT in — the
     * addElectricDynamoUlv placeholder convention, the per-face art is the render pool);
     * the item models parent the block models.
     */
    private void addBatteryBoxes() {
        for (gregtech6.registry.GT6Batteries.BoxRow tRow : gregtech6.registry.GT6Batteries.BOX_ROWS) {
            net.minecraft.world.level.block.Block tBlock = gregtech6.registry.GT6Batteries.BATTERY_BOX_BLOCKS.get(tRow.path()).get();
            simpleBlock(tBlock, models().cubeAll(tRow.path(),
                    modLoc(tRow.slots() == 16 ? "block/battery_box_large" : "block/battery_box")));
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
        }
    }

    /**
     * Task p35-energy-tail-machines — the Crystal Chargers: the 20-row LU family rides
     * one baked-art model per size (task p36-render-texture-bake retired the battery-box
     * stand-in): the src-over colored+overlay composites of the upstream crystal_laser{,_large}
     * iconsets (the bake_render_pool_textures.py products, assets/README.md attribution)
     * with the laser FRONT art on the FACING face and the shared side art on the other
     * five (upstream getTexture2, MultiTileEntityCrystalCharger.java:33-36 — index 0 =
     * front only on mFacing, index 1 = side everywhere else). The overlay_active/
     * overlay_blinking trios stay unborrowed — the port blocks carry no ACTIVE property
     * (the static-face posture, the GT6BatteryBoxBlock doc).
     */
    private void addCrystalChargers() {
        for (gregtech6.registry.GT6CrystalChargers.ChargerRow tRow : gregtech6.registry.GT6CrystalChargers.ROWS) {
            net.minecraft.world.level.block.Block tBlock = gregtech6.registry.GT6CrystalChargers.BLOCKS_BY_PATH.get(tRow.path()).get();
            String tTex = tRow.slots() == 16 ? "block/crystal_charger_large_" : "block/crystal_charger_";
            ModelFile tModel = models().cube(tRow.path(),
                    modLoc(tTex + "side"), modLoc(tTex + "side"),          // bottom/top
                    modLoc(tTex + "front"), modLoc(tTex + "side"),         // north(front)/south
                    modLoc(tTex + "side"), modLoc(tTex + "side"));         // west/east
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(gregtech6.block.energy.GT6BatteryBoxBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    /**
     * Task p36-energy-zpm-dechargers — the two ZPM Decharger rows over the REAL baked art
     * (review-fix: the rebase binding the p36-render-texture-bake contract paths — the
     * battery-box stand-in yields): the src-over zpm_electricity/zpm_quantum composites
     * (assets/README.md attribution), front on the FACING face, back on the opposite
     * (upstream getTexture2, MultiTileEntityZPMDechargerEU.java:39-44 — index 0 = front
     * on mFacing, index 1 = back on OPOS, index 2 = side elsewhere; the ZPM_TOP active
     * decal stays unborrowed, the port carries no ACTIVE property), the FACING four-way
     * rotationY (the addCrystalChargers form).
     */
    private void addZpmDechargers() {
        for (gregtech6.registry.GT6ZpmDechargers.DechargerRow tRow : gregtech6.registry.GT6ZpmDechargers.ROWS) {
            net.minecraft.world.level.block.Block tBlock = gregtech6.registry.GT6ZpmDechargers.BLOCKS_BY_PATH.get(tRow.path()).get();
            String tTexBase = tRow.path().equals("zpm_decharger_electric") ? "zpm_decharger" : tRow.path(); // the render-card PNG names: the electric family drops the infix, the quantum family keeps it
            ModelFile tModel = models().cube(tRow.path(),
                    modLoc("block/" + tTexBase + "_side"), modLoc("block/" + tTexBase + "_side"),   // bottom/top
                    modLoc("block/" + tTexBase + "_front"), modLoc("block/" + tTexBase + "_back"),  // north(front)/south(back)
                    modLoc("block/" + tTexBase + "_side"), modLoc("block/" + tTexBase + "_side"));  // west/east
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(gregtech6.block.energy.GT6BatteryBoxBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    private void addElectricDynamoUlv() {
        Block tBlock = GT6ElectricDynamos.ELECTRIC_DYNAMO_ULV.get();
        simpleBlock(tBlock, models().cubeAll("electric_dynamo_ulv", modLoc("block/energy_source")));
        itemModels().withExistingParent("electric_dynamo_ulv", modLoc("block/electric_dynamo_ulv"));
    }

    /**
     * Task p38-c1-dynamo-bowl-models — the Electric (T1-T5) and Flux (T1-T5) dynamo ladder
     * rows: the ten registered {@code GT6DynamoBlock}s had ZERO generated assets (placed
     * they fell to the missing-model checkerboard; the census probe). Each family's ladder
     * shares ONE facing-cube model over the BAKED colored+overlay composites (the p28
     * electric-transformer posture — the per-tier visual is not a column of the upstream
     * registration, Loader :946-950/:953-957 all ride one icon set). The FRONT face = the
     * OUTPUT {@code mFacing}, the BACK = the INPUT {@code OPOS}, the four side faces the
     * plain side art (MultiTileEntityDynamoFlux/Electric getTexture2 index 0/1/2); the
     * {@code overlay_active/} trio stays unborrowed (no ACTIVE property on the port block —
     * the W2 render card). The bake is the canonical flat look (the p19 src-over treatment;
     * assets/README.md carries source + product sha256), the runtime mRGBa tint the render
     * pool. The T0 ULV row keeps its placeholder above.
     */
    private void addDynamoLadders() {
        addDynamoFamily("electric_dynamo", List.of(
                Map.entry("electric_dynamo", (Block) GT6ElectricDynamos.ELECTRIC_DYNAMO.get()),
                Map.entry("electric_dynamo_t2", (Block) GT6ElectricDynamos.ELECTRIC_DYNAMO_T2.get()),
                Map.entry("electric_dynamo_t3", (Block) GT6ElectricDynamos.ELECTRIC_DYNAMO_T3.get()),
                Map.entry("electric_dynamo_t4", (Block) GT6ElectricDynamos.ELECTRIC_DYNAMO_T4.get()),
                Map.entry("electric_dynamo_t5", (Block) GT6ElectricDynamos.ELECTRIC_DYNAMO_T5.get())));
        addDynamoFamily("flux_dynamo", List.of(
                Map.entry("flux_dynamo", (Block) gregtech6.registry.GT6FluxDynamos.FLUX_DYNAMO.get()),
                Map.entry("flux_dynamo_t2", (Block) gregtech6.registry.GT6FluxDynamos.FLUX_DYNAMO_T2.get()),
                Map.entry("flux_dynamo_t3", (Block) gregtech6.registry.GT6FluxDynamos.FLUX_DYNAMO_T3.get()),
                Map.entry("flux_dynamo_t4", (Block) gregtech6.registry.GT6FluxDynamos.FLUX_DYNAMO_T4.get()),
                Map.entry("flux_dynamo_t5", (Block) gregtech6.registry.GT6FluxDynamos.FLUX_DYNAMO_T5.get())));
    }

    /** One ladder walk — the shared facing cube (the addZpmDechargers rotation form) + the row BlockItem parents. */
    private void addDynamoFamily(String aFamily, List<Map.Entry<String, Block>> aRows) {
        ModelFile tModel = models().cube(aFamily,
                modLoc("block/" + aFamily + "_side"), modLoc("block/" + aFamily + "_side"),   // bottom/top
                modLoc("block/" + aFamily + "_front"), modLoc("block/" + aFamily + "_back"),  // north(front/output)/south(back/input)
                modLoc("block/" + aFamily + "_side"), modLoc("block/" + aFamily + "_side"));  // west/east
        for (Map.Entry<String, Block> tRow : aRows) {
            getVariantBuilder(tRow.getValue()).forAllStates(aState -> {
                int tY = switch (aState.getValue(gregtech6.block.energy.GT6DynamoBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.getKey(), tModel.getLocation());
        }
    }

    /**
     * Task p12-gearbox-transformer — the GearBox (one cube_all over the borrowed
     * {@code gt6:block/gearbox} texture, the upstream iconsets/GEARBOX.png; assets/README.md
     * attribution) and the Rotation Transformer (the crank facing-cube shape: an
     * {@code orientable} model over the borrowed transformer_rotation colored front/side
     * textures, the FRONT = input face, BACK = output face —
     * MultiTileEntityTransformerRotation :42-45). Both single wood-row variants (the
     * material fan-out is the pool), no connection-mask visual layer (the per-face gear
     * overlays are the render pool). The static placeholder texture carries no rotation
     * animation — declared with the axle.
     */
    private void addGearBoxTransformer() {
        Block tBox = GT6Kinetics.GEARBOX.get();
        simpleBlock(tBox, models().cubeAll("gearbox", modLoc("block/gearbox")));
        itemModels().withExistingParent("gearbox", modLoc("block/gearbox"));
        Block tTrans = GT6Kinetics.TRANSFORMER_ROTATION.get();
        ModelFile tTransModel = models().orientable("transformer_rotation",
                modLoc("block/transformer_rotation_side"), modLoc("block/transformer_rotation_front"), modLoc("block/transformer_rotation_side"));
        getVariantBuilder(tTrans).forAllStates(aState -> {
            // the vanilla horizontal-facing rotation map (Direction.getFrontRotationYaw form)
            Direction tFacing = aState.getValue(GTTransformerRotationBlock.FACING);
            return ConfiguredModel.builder()
                    .modelFile(tTransModel)
                    .rotationY((int) (tFacing.toYRot() + 180) % 360)
                    .build();
        });
        itemModels().withExistingParent("transformer_rotation", modLoc("block/transformer_rotation"));
    }

    /**
     * Task p28-c-ulv-lv-transformer — the Electric Transformer ULV-LV: the p12 rotation
     * transformer's orientable facing-cube shape verbatim (the FRONT = INPUT face — the
     * Base11 :63 convention; ALL-BUT-FRONT = output) over the BAKED upstream textures:
     * colored/front + overlay/front and colored/side + overlay/side composited src-over
     * into single-layer opaque PNGs (the p19 distillery bake treatment — the upstream
     * two-layer colored+overlay stack with the mRGBa tint is the render pool card;
     * assets/README.md carries the source + product sha256 attribution). The active
     * overlay (MultiTileEntityTransformerElectric :50-57) is the render pool defer.
     */
    /**
     * Task p35-long-distance-pipes — the Long Distance pipes: the 16 wire metas ride the
     * cube-all shape over the shared item-pipe texture (the dedicated upstream
     * LONG_DIST_PIPES_01 iconset is the render pool — the energy-tail wire posture), the
     * two endpoints share the p28 electric-transformer orientable model (the facing-cube
     * posture is the same; the per-endpoint visual is not a column of the registration).
     */
    private void addLongDistancePipes() {
        ModelFile tEndpointModel = models().getExistingFile(modLoc("block/electric_transformer"));
        for (Block tEndpoint : new Block[] {gregtech6.registry.GT6LongDistPipes.ITEM_PIPE_BLOCK.get(),
                gregtech6.registry.GT6LongDistPipes.FLUID_PIPE_BLOCK.get()}) {
            String tPath = tEndpoint.getDescriptionId().replace("block.gt6.", "");
            getVariantBuilder(tEndpoint).forAllStates(aState -> {
                Direction tFacing = aState.getValue(GT6ElectricTransformerBlock.FACING);
                return ConfiguredModel.builder()
                        .modelFile(tEndpointModel)
                        .rotationY((int) (tFacing.toYRot() + 180) % 360)
                        .build();
            });
            itemModels().withExistingParent(tPath, modLoc("block/electric_transformer"));
        }
        for (int tMeta = 0; tMeta < 16; tMeta++) {
            Block tWire = gregtech6.registry.GT6LongDistPipes.wireBlockOf(tMeta);
            String tPath = gregtech6.registry.GT6LongDistPipes.pathOf(tMeta);
            simpleBlock(tWire, models().cubeAll(tPath, modLoc("block/item_pipe")));
            itemModels().withExistingParent(tPath, modLoc("block/" + tPath));
        }
    }

    private void addElectricTransformer() {
        // task p35 — the full :881-:889 ladder shares ONE model: upstream registers all
        // nine rows over the SAME icon set (machines/transformers/transformer_electric/*,
        // the per-tier visual is not a column of the registration), so the port shares
        // the p28 baked model verbatim.
        ModelFile tModel = models().orientable("electric_transformer",
                modLoc("block/electric_transformer_side"), modLoc("block/electric_transformer_front"), modLoc("block/electric_transformer_side"));
        for (GT6ElectricTransformers.TransformerRow tRow : GT6ElectricTransformers.ROWS) {
            Block tTrans = GT6ElectricTransformers.BLOCKS_BY_PATH.get(tRow.path()).get();
            getVariantBuilder(tTrans).forAllStates(aState -> {
                // the vanilla horizontal-facing rotation map (the addGearBoxTransformer form)
                Direction tFacing = aState.getValue(GT6ElectricTransformerBlock.FACING);
                return ConfiguredModel.builder()
                        .modelFile(tModel)
                        .rotationY((int) (tFacing.toYRot() + 180) % 360)
                        .build();
            });
            itemModels().withExistingParent(tRow.path(), modLoc("block/electric_transformer"));
        }
    }

    /**
     * Task p35-energy-tail-machines — the Long Distance families, their dedicated
     * upstream art landed by task p36-render-texture-bake (the stand-in clearance): the
     * five LD transformer endpoints ride the INPUT/OUTPUT facing-cube model over the
     * baked longdistancetransformer_electric composites (front = the INPUT face, back =
     * the OUTPUT face — MultiTileEntityLongDistanceTransformer.java:284-285/:295-299,
     * index 0/1/2; the overlay_active/blinking/unloaded trios stay unborrowed — the
     * port blocks carry no ACTIVE property), and the 16 LD wire metas ride the
     * property-less cube-all shape over their TIER sprite — the five distinct
     * LONG_DIST_WIRES_01 iconset art (Textures.java:638-655: metas 0-1=EV, 2=IV,
     * 3-7=LuV, 8-11=ZPM, 12-15=UV; the same split as the tier-byte table of
     * Loader_Blocks.java:160).
     */
    private void addLDEnergyFamilies() {
        ModelFile tLDModel = models().cube("long_distance_transformer",
                modLoc("block/long_distance_transformer_side"), modLoc("block/long_distance_transformer_side"), // bottom/top
                modLoc("block/long_distance_transformer_front"), modLoc("block/long_distance_transformer_back"), // north(input)/south(output)
                modLoc("block/long_distance_transformer_side"), modLoc("block/long_distance_transformer_side")); // west/east
        for (gregtech6.registry.GT6LongDistanceTransformers.LDRow tRow : gregtech6.registry.GT6LongDistanceTransformers.ROWS) {
            Block tTrans = gregtech6.registry.GT6LongDistanceTransformers.BLOCKS_BY_PATH.get(tRow.path()).get();
            getVariantBuilder(tTrans).forAllStates(aState -> {
                Direction tFacing = aState.getValue(GT6ElectricTransformerBlock.FACING);
                return ConfiguredModel.builder()
                        .modelFile(tLDModel)
                        .rotationY((int) (tFacing.toYRot() + 180) % 360)
                        .build();
            });
            itemModels().withExistingParent(tRow.path(), tLDModel.getLocation());
        }
        for (gregtech6.registry.GT6LongDistWires.WireRow tRow : gregtech6.registry.GT6LongDistWires.ROWS) {
            String tPath = gregtech6.registry.GT6LongDistWires.pathOf(tRow.meta());
            Block tWire = gregtech6.registry.GT6LongDistWires.BLOCKS_BY_META.get(tRow.meta()).get();
            var tModel = models().cubeAll(tPath, modLoc("block/long_dist_wire_" + wireArtOf(tRow.tier())));
            getVariantBuilder(tWire).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
            itemModels().withExistingParent(tPath, modLoc("block/" + tPath));
        }
    }

    /** The tier byte -> LONG_DIST_WIRES_01 sprite token (Textures.java:638-655; VN[4..8] = EV/IV/LuV/ZPM/UV). */
    private static String wireArtOf(int aTier) {
        return switch (aTier) {
            case 4 -> "ev";
            case 5 -> "iv";
            case 6 -> "luv";
            case 7 -> "zpm";
            case 8 -> "uv";
            default -> throw new IllegalArgumentException("unknown LD wire tier byte: " + aTier);
        };
    }

    /**
     * Task p8-prefixblock-render spec ① — the 3773 material prefix blocks
     * ({@link GTMaterialBlocks#blockArray()}, the card-A census order).
     *
     * <p><b>Model merging rule (pinned, anti-bloat)</b>: one SHARED block model per
     * (prefix, live-texture-set) pair — for each of the seven storage prefixes the live
     * set is the distinct {@code material.mTextureSetsBlock} first entry across that
     * prefix's registered pairs (upstream OreDictMaterial.java:252 + TextureSet.java:188-228;
     * port: MT.setTextures, MT.java:210-215). Measured 2026-08-31 (pinned by
     * GT6PrefixBlockRenderDatagenTest): 29+22+36+22+23+21+22 = 175 shared models, so the
     * total JSON output is 175 models + 3773 blockstates + 3773 item models — NOT a model
     * per material pair (per-pair models are an explicit red line, and a per-material
     * texture would not tint anyway). <b>Fallback</b>: a material with an empty/blank set
     * list resolves to {@code "none"} = upstream SET_NONE (TextureSet.java:188; the
     * GT6ItemModels.iconsetOf mirror on the block list) — measured over the registered
     * 3773 pairs the fallback never fires today, but it keeps the generator total: no set
     * name is dereferenced blindly. The grayscale texture carries the SHAPE, the material
     * colour comes from the runtime tint ({@code fRGBa[prefix.mState]},
     * RegisterColorHandlersEvent.Block) — so a fallback loses only the texture detail,
     * never the material identity.
     *
     * <p>Each model is an element-built cube_all with {@code tintindex 0} on all six faces
     * (the vanilla grass/leaves idiom — the parent {@code block/block} carries the item
     * display transforms, the faces carry {@code #all} + cullface, UVs default to the
     * element bounds exactly like vanilla {@code cube_all}). Blockstate = one JSON per
     * block, a single variant onto the shared model. Item model = one JSON per block,
     * {@code withExistingParent} onto the shared block model — same provider, same pass,
     * so the parent resolves in the ExistingFileHelper (BlockStateProvider.run flushes
     * block models before item models, the GT6BlockStates.java:29-33 precedent — the very
     * reason the ADR moved the models off card A). Textures are the borrowed upstream
     * grayscale PNGs ({@code gt6:textures/block/materialicons/<set>/<prefix>.png},
     * lowercased per the 1.20.1 ResourceLocation constraint; assets/README.md attribution).
     */
    private void addPrefixBlocks() {
        Map<String, ModelFile> tShared = new HashMap<>(); // one shared model per (prefix, set), built on first use
        for (Block tBlock : GTMaterialBlocks.blockArray()) {
            GTMaterialPrefixBlock tPrefixBlock = (GTMaterialPrefixBlock)tBlock;
            String tPrefixSnake = GTMaterialItems.snakeCase(tPrefixBlock.prefix.mNameInternal);
            String tSetSnake = blockSetOf(tPrefixBlock.material);
            // Full "block/..." model path: getBuilder skips the folder prefix for "/"-bearing
            // names (ModelProvider.extendWithFolder), so the block/ segment must be explicit —
            // that keeps the tracked/built location == models/block/materialicons/... == the
            // item-parent lookup below.
            String tModelName = "block/materialicons/" + tSetSnake + "/" + tPrefixSnake;
            ModelFile tModel = tShared.computeIfAbsent(tModelName,
                    tKey -> tintedCubeAll(tKey, modLoc("block/materialicons/" + tSetSnake + "/" + tPrefixSnake)));
            simpleBlock(tBlock, tModel);
            itemModels().withExistingParent(GTMaterialItems.itemIdOf(tPrefixBlock.prefix, tPrefixBlock.material),
                    modLoc(tModelName));
        }
        LOGGER.info("GT6 prefix blocks: {} blocks over {} shared (prefix x set) models",
                GTMaterialBlocks.blockArray().length, tShared.size());
    }

    /**
     * One tinted cube_all block model: parent block/block (the 3D item display transforms),
     * a full 0..16 element with all six faces on {@code #all}, cullface per side and
     * {@code tintindex 0} (the vanilla grass_block/leaves element idiom; no explicit UVs —
     * they default to the element bounds, byte-equivalent to vanilla cube_all output).
     * {@code aName} must be the full {@code block/...} model path (see addPrefixBlocks).
     */
    private BlockModelBuilder tintedCubeAll(String aName, ResourceLocation aTexture) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("all", aTexture)
                .texture("particle", "#all");
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#all").tintindex(0).cullface(aDir))
                .end();
        return tModel;
    }

    /**
     * The material's BLOCK texture-set name, lower-snaked; an empty/blank list falls back
     * to {@code "none"} = upstream SET_NONE (TextureSet.java:188; MT.setTextures
     * MT.java:210-215 assigns the set name strings). Since task p9-wire-family-w2 the
     * implementation lives in the MC-free single source
     * {@link gregtech6.client.wire.GTWireTextures} (shared with the client listener — this
     * class's statics bootstrap-gate the offline test JVM, that one does not).
     */
    public static String blockSetOf(OreDictMaterial aMaterial) {
        return gregtech6.client.wire.GTWireTextures.blockSetOf(aMaterial);
    }

    /**
     * Task p13-boiler-tank — the 26 Steam Boiler Tank rows (Loader_MultiTileEntities.java
     * :553-579): ONE oriented cube model over the boiler_steam texture set (the grayscale
     * placeholders — the upstream machines/tanks/boiler_steam colored+overlay iconsets and
     * the BI.BAROMETER gauge have no borrowable source in this repo, the assets precedent;
     * the FRONT face carries the gauge texture, FACING drives the front semantics, the
     * barometer 5-bit visual is the synced BE payload — the per-state gauge rendering is
     * the render pool, the burning-box ruling repeated). Both ladders share the model (the
     * SAME block class upstream, :552 aClass). The 26 BlockItem models parent it.
     */
    private void addBoilers() {
        String tTex = "block/boiler_steam/";
        ModelFile tModel = models().cube("steam_boiler_tank",
                modLoc(tTex + "bottom"), modLoc(tTex + "top"),          // bottom/top
                modLoc(tTex + "front"), modLoc(tTex + "side"),          // north(front = the barometer face)/south
                modLoc(tTex + "side"), modLoc(tTex + "side"));          // west/east
        for (gregtech6.registry.GT6Boilers.BoilerRow tRow : gregtech6.registry.GT6Boilers.allRows()) {
            Block tBlock = gregtech6.registry.GT6Boilers.BLOCKS_BY_PATH.get(tRow.path()).get();
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(gregtech6.registry.GT6Boilers.BoilerTankBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    /**
     * Task p26-storage-hopper-family — the 4 storage-hopper rows (Loader_MultiTileEntities
     * .java:145-146 over :191/:202, Bronze/Steel × hopper/queue): ONE oriented cube model
     * over the two grayscale placeholders (the upstream machines/automation/hopper and
     * queuehopper colored+overlay iconsets have no borrowable source in this repo — the
     * boiler/burning-box assets precedent; the FRONT face carries the output-face texture,
     * FACING drives the output semantics, the three-pass custom funnel shape of upstream
     * :263-277 is the render pool). Both kinds share the model (the SAME shapes upstream
     * :259-282/:241-264). The 4 BlockItem models parent it. The FACING is all-six (the
     * vanilla Piston blockstate convention): down x=90, up x=270, the horizontals the
     * boiler y-mapping.
     */
    private void addHoppers() {
        String tTex = "block/hopper_";
        ModelFile tModel = models().cube("gt6_hopper",
                modLoc(tTex + "side"), modLoc(tTex + "side"),        // bottom/top
                modLoc(tTex + "front"), modLoc(tTex + "side"),       // north(front = the output face)/south
                modLoc(tTex + "side"), modLoc(tTex + "side"));       // west/east
        for (gregtech6.registry.GT6Hoppers.HopperRow tRow : gregtech6.registry.GT6Hoppers.ROWS) {
            Block tBlock = gregtech6.registry.GT6Hoppers.BLOCKS_BY_PATH.get(tRow.path()).get();
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tX = 0, tY = 0;
                switch (aState.getValue(gregtech6.registry.GT6Hoppers.GT6HopperBlock.FACING)) {
                    case DOWN -> tX = 90;   // the vanilla Piston blockstate convention
                    case UP -> tX = 270;
                    case SOUTH -> tY = 180;
                    case WEST -> tY = 270;
                    case EAST -> tY = 90;
                    default -> {} // NORTH
                }
                return ConfiguredModel.builder().modelFile(tModel).rotationX(tX).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    /**
     * Task p13-burning-box-family — the 97 burning-box rows (Loader_MultiTileEntities.java
     * :517-704). Task issue11-burningbox UPGRADED the target (the former "the upstream
     * colored/overlay iconsets have no borrowable source in this repo" claim here was
     * proven false — all five burning_* groups exist in the snapshot): per GROUP one
     * two-state model pair over the upstream burning_{solid,liquid,gas,fluidbed,brick}
     * texture groups. Model grammar (the {@link #familyMachineModel} shape): the body
     * cube re-declared with {@code tintindex 0} on every face (the p21 machine tint
     * seat — the upstream colored × mRGBa, MultiTileEntityGeneratorMetal.java:38,
     * baked into the vertex colours by GTMachineTintModel since p32; the row material
     * resolves through the common GTBasicMachineBlock.materialOf dispatch) plus six
     * 0.01 face decals with NO tintindex carrying the borrowed overlay/overlay_active
     * art (the p22 UNCOLOURED second layer). The body texture is ONE grayscale PNG —
     * every colored face of every group hashes identical (assets/README.md) — while
     * the family identity lives entirely in the per-group decals, exactly the p20
     * probe's record. The FACING-front cube rotation is the steam-engine y-mapping
     * (below), and the FACING_ROTATIONS[north] art binding west→right / east→left is
     * the familyMachineModel table verbatim. The lit state picks the {@code _lit}
     * model — the overlay_active decals, the upstream mBurning texture switch — driven
     * by the LIT blockstate property the BE applies on every mBurning flip (issue #11
     * behavior half; the vanilla CampfireBlock LIT convention). The Brick row takes
     * its own burning_brick group (the p13 "Brick shares the SOLID model" ruling
     * closed). The 97 BlockItem models parent their group's UNLIT model (the creative
     * icon never burns).
     */
    private void addBurningBoxes() {
        java.util.Map<String, ModelFile> tUnlit = new java.util.HashMap<>(), tLit = new java.util.HashMap<>();
        for (String tGroup : new String[] {"solid", "liquid", "gas", "fluidbed", "brick"}) {
            tUnlit.put(tGroup, burningBoxModel("burning_box_" + tGroup, tGroup, false));
            tLit.put(tGroup, burningBoxModel("burning_box_" + tGroup + "_lit", tGroup, true));
        }
        for (gregtech6.registry.GT6BurningBoxes.BurningBoxRow tRow : gregtech6.registry.GT6BurningBoxes.allRows()) {
            Block tBlock = gregtech6.registry.GT6BurningBoxes.BLOCKS_BY_PATH.get(tRow.path()).get();
            // the Brick row is Family.SOLID behaviourally (the same BE class) but wears
            // its own upstream texture group — the one row keyed off the path
            String tGroup = tRow.path().equals(gregtech6.registry.GT6BurningBoxes.BRICK_ROW.path()) ? "brick"
                    : tRow.family().name().toLowerCase(java.util.Locale.ROOT);
            ModelFile tOff = tUnlit.get(tGroup), tOn = tLit.get(tGroup);
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(gregtech6.registry.GT6BurningBoxes.BurningBoxBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder()
                        .modelFile(aState.getValue(gregtech6.registry.GT6BurningBoxes.BurningBoxBlock.LIT) ? tOn : tOff)
                        .rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), tOff.getLocation());
        }
    }

    /**
     * One burning-box model (the {@link #familyMachineModel} geometry with a shared
     * body texture): the tinted body cube (every face {@code tintindex 0}, bound to
     * the ONE grayscale body PNG — upstream all colored sets hash identical, see
     * {@link #addBurningBoxes}) plus the six 0.01 face decals (single face, NO
     * tintindex, cullface synced — the p22 pairing) over the group's
     * {@code overlay[_active]} art. The {@code aLit} arm binds the {@code _active}
     * decal set (the burning glow).
     */
    private ModelFile burningBoxModel(String aName, String aGroup, boolean aLit) {
        String tSuffix = aLit ? "_active" : "";
        ResourceLocation tBody = modLoc("block/burning_box_solid");
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", tBody).texture("up", tBody)
                .texture("north", tBody).texture("south", tBody)
                .texture("west", tBody).texture("east", tBody)
                .texture("overlay_front", modLoc("block/burning_box_" + aGroup + "_overlay_front" + tSuffix))
                .texture("overlay_back", modLoc("block/burning_box_" + aGroup + "_overlay_back" + tSuffix))
                .texture("overlay_left", modLoc("block/burning_box_" + aGroup + "_overlay_left" + tSuffix))
                .texture("overlay_right", modLoc("block/burning_box_" + aGroup + "_overlay_right" + tSuffix))
                .texture("overlay_top", modLoc("block/burning_box_" + aGroup + "_overlay_top" + tSuffix))
                .texture("overlay_bottom", modLoc("block/burning_box_" + aGroup + "_overlay_bottom" + tSuffix));
        // element 0 — the tinted body cube (the p21 shape, the mRGBa material seat)
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        // elements 1-6 — the burning decals: thin plate per face, 0.01 out, single face,
        // no tintindex (the UNCOLOURED second layer), cullface synced (the p22 pairing).
        tModel.element() // front (north)
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_front").cullface(Direction.NORTH)
                .end();
        tModel.element() // back (south)
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_back").cullface(Direction.SOUTH)
                .end();
        tModel.element() // left art (east face — FACING_ROTATIONS[north][east]=2=left)
                .from(16.0F, 0.0F, 0.0F).to(16.01F, 16.0F, 16.0F)
                .face(Direction.EAST).texture("#overlay_left").cullface(Direction.EAST)
                .end();
        tModel.element() // right art (west face — FACING_ROTATIONS[north][west]=4=right)
                .from(-0.01F, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                .face(Direction.WEST).texture("#overlay_right").cullface(Direction.WEST)
                .end();
        tModel.element() // bottom (down)
                .from(0.0F, -0.01F, 0.0F).to(16.0F, 0.0F, 16.0F)
                .face(Direction.DOWN).texture("#overlay_bottom").cullface(Direction.DOWN)
                .end();
        tModel.element() // top (up)
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_top").cullface(Direction.UP)
                .end();
        return tModel;
    }

    /**
     * Task p13-large-boiler — the Large Boiler family (Loader_MultiTileEntities.java
     * :1159-1165/:1176/:1248-1252): the five Dense Wall part blocks + the Heat Transmitter
     * part block as plain cube_all over the generated grayscale placeholders (upstream has
     * no borrowable "largeboiler"/"metalwalldense" texture group in this snapshot — the
     * W2 burning-box ruling), and the five boiler variant controllers over ONE shared
     * oriented cube model (front = the barometer face) rotated per FACING exactly like
     * addBoilers; the FORMED variants map to the same model (the formed-look visual is the
     * p9 pool). The 11 BlockItem models parent their block models.
     */
    private void addLargeBoiler() {
        ModelFile tMain = models().cube("large_boiler_main",
                modLoc("block/large_boiler/wall"), modLoc("block/large_boiler/wall"),
                modLoc("block/large_boiler/main"), modLoc("block/large_boiler/main"),
                modLoc("block/large_boiler/main"), modLoc("block/large_boiler/main"));
        for (var tRow : gregtech6.registry.GTMultiBlocks.LARGE_BOILER_ROWS) {
            Block tBlock = gregtech6.registry.GTMultiBlocks.LARGE_BOILER_BLOCKS_BY_PATH.get(tRow.path()).get();
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tMain).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), tMain.getLocation());
        }
        // task p29-w3-nbtdesign-parts — the Dense Wall parts + the transmitter moved to
        // addParts() (the dense walls carry the DESIGN property — per-design variants; the
        // transmitter rides the upstream heatacceptor borrow); addLargeBoilerPart retired
    }

    /** One cube_all part block + its BlockItem parent (the coke-oven-bricks shape). */
    private void addLargeBoilerPart(String aPath, String aTexture) {
        Block tBlock = gregtech6.registry.GTMultiBlocks.WALL_BLOCKS_BY_PATH.get(aPath) != null
                ? gregtech6.registry.GTMultiBlocks.WALL_BLOCKS_BY_PATH.get(aPath).get()
                : gregtech6.registry.GTMultiBlocks.HEAT_TRANSMITTER.get();
        simpleBlock(tBlock, models().cubeAll(aPath, modLoc(aTexture)));
        itemModels().withExistingParent(aPath, modLoc("block/" + aPath));
    }

    /**
     * Task p24-lightning-rod — the Lightning Rod family (Loader_MultiTileEntities.java
     * :1151/:1168/:1179/:1282): the three part blocks as plain cube_all over the borrowed
     * upstream textures (the multiblockparts metalwall/coil/lightningrod colored faces, the
     * P20 ruling ①), and the single controller over ONE cube model (the borrowed
     * multiblockmains lightningrod group — the colored base alpha-over the overlay_front
     * decal, composited at borrow time; all three upstream faces composite to the SAME
     * visible pixels, so one texture serves all six faces, the large_boiler/main.png form).
     * The facing is structurally meaningless (the rod is vertical), so every state maps to
     * the same model with no rotation; the FORMED variants map to the same model (the
     * formed-look visual is the p9 pool). The four BlockItem models parent their block
     * models.
     */
    /**
     * Task p29-w3-tank-valves — the Tank Main Valve family (Loader_MultiTileEntities.java
     * :1195-1222): the 25 variant controllers over ONE shared cube model per material
     * family — the wood valve over the borrowed woodwall part texture, the 24 metal valves
     * over the borrowed metalwall part texture (the port has no multiblockmains "tankwood"/
     * "tankmetal" group in this snapshot — the large-boiler borrow ruling; the formed-look
     * visual is the p9 pool). Task issue8-residual: the re-declared body element carries
     * {@code tintindex 0} on every face (the {@code tintedCube} grammar) — the grayscale
     * colored textures multiply the row's NBT_MATERIAL (every :1195-1222 row carries the
     * column, the upstream {@code getTexture2} colored×mRGBa form; the bake/ItemColor
     * consumers ride GTMachineTintModel/GTItemPaintTint through the controller gate). The
     * FACING + FORMED variants map to the same model like every
     * controller; the 25 BlockItem models parent their block models.
     */
    private void addTanks() {
        ModelFile tWood = tintedCube("tank_wood", "block/parts/woodwall/0/colored/bottom", "block/parts/woodwall/0/colored/top", "block/parts/woodwall/0/colored/side");
        ModelFile tMetal = tintedCube("tank_metal", "block/parts/metalwall/0/colored/bottom", "block/parts/metalwall/0/colored/top", "block/parts/metalwall/0/colored/side");
        for (var tRow : gregtech6.registry.GT6Tanks.ROWS) {
            Block tBlock = gregtech6.registry.GT6Tanks.BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tModel = tRow.flammable() ? tWood : tMetal; // the wood valve is the flammable row
            getVariantBuilder(tBlock).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    /**
     * One tinted full-cube model (task issue8-residual; the partModel body form without
     * the decal overlays — the addBridgeFamily re-declared-element grammar): the borrowed
     * grayscale colored faces on the six texture keys, the single body cube element carries
     * {@code tintindex 0} so the GTMachineTintModel bake multiplies the carrier's
     * NBT_MATERIAL (the vanilla cube parent's own elements are replaced by the child's).
     */
    private ModelFile tintedCube(String aName, String aBottom, String aTop, String aSide) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc(aBottom)).texture("up", modLoc(aTop))
                .texture("north", modLoc(aSide)).texture("south", modLoc(aSide))
                .texture("west", modLoc(aSide)).texture("east", modLoc(aSide));
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        return tModel;
    }

    private void addLightningRod() {
        ModelFile tMain = models().cubeAll("multiblock_lightning_rod", modLoc("block/lightningrod/main"));
        Block tController = gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD.get();
        getVariantBuilder(tController).forAllStates(aState -> ConfiguredModel.builder().modelFile(tMain).build());
        itemModels().withExistingParent("multiblock_lightning_rod", tMain.getLocation());
        addLightningRodPart("machine_wall_tungsten", "block/lightningrod/wall");
        addLightningRodPart("niobium_titanium_coil", "block/lightningrod/coil");
        addLightningRodPart("lightning_rod", "block/lightningrod/rod");
    }

    /** One cube_all Lightning Rod part block + its BlockItem parent (the addLargeBoilerPart shape). */
    private void addLightningRodPart(String aPath, String aTexture) {
        Block tBlock = gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD_PART_BLOCKS_BY_PATH.get(aPath).get();
        simpleBlock(tBlock, models().cubeAll(aPath, modLoc(aTexture)));
        itemModels().withExistingParent(aPath, modLoc("block/" + aPath));
    }

    /**
     * Task p21-stoneblocks-16item-registry-split — the 272 GT6 stone VARIANT blocks
     * ({@link GTStoneBlocks#blockArray()}, stone-major in CS.java:1668 order and
     * variant-major in meta order, the per-pair registry split): each block is a degenerate
     * pure block with a FIXED {@link StoneVariant}, so each gets a plain single-state
     * blockstate over its OWN cube_all model (the P19 16-row {@code variant=<snake>} rows
     * retired with the EnumProperty — one property-free state per block now), and each gets
     * its OWN item model {@code withExistingParent} onto that block model (the id scheme is
     * {@link GTStoneBlocks#path}: variant 0 keeps the bare snake, the other 15 suffix the
     * variant segment). The model/texture keys are UNCHANGED from the P19 render card
     * ({@code gt6:block/stones/<stone>/<variant>}, one model per dedicated borrowed PNG,
     * assets/README.md attribution, census 17x16 = 272 files, zero gaps, NO tintindex — the
     * colored-PNG route), so only the blockstate/item/loot faces re-key per pair.
     *
     * <p>Same provider, same pass, so the parent resolves in the ExistingFileHelper (the
     * GT6BlockStates.java:29-33 BlockStateProvider.run ordering precedent). The P19 declared
     * deviation "the inventory shows the STONE look for every state" is RETIRED: with one
     * item per variant, every inventory stack now shows its own variant's look — the
     * upstream 1.7.10 ItemBlock per-meta icon face, restored.
     */
    private void addStoneBlocks() {
        for (Block tBlock : GTStoneBlocks.blockArray()) {
            GTStoneBlock tStone = (GTStoneBlock)tBlock;
            // Full "block/..." model path: getBuilder skips the folder prefix for
            // "/"-bearing names (ModelProvider.extendWithFolder), so the block/ segment
            // must be explicit — the addPrefixBlocks pin, same builder mechanics here.
            String tModelName = "block/stones/" + tStone.stoneSnake + "/" + tStone.variant.snake;
            ModelFile tModel = models().cubeAll(tModelName,
                    modLoc("block/stones/" + tStone.stoneSnake + "/" + tStone.variant.snake));
            simpleBlock(tBlock, tModel); // the single default state -> the variants:{"": ...} form
            itemModels().withExistingParent(GTStoneBlocks.path(tStone.stoneSnake, tStone.variant), modLoc(tModelName));
        }
        LOGGER.info("GT6 stone blocks: {} per-pair blockstates over {} models (one item model each)",
                GTStoneBlocks.STONES.size() * StoneVariant.VALUES.length,
                GTStoneBlocks.STONES.size() * StoneVariant.VALUES.length);
    }

    /**
     * Task p24-grass-block — the 6 GT grass VARIANT blocks ({@link GTGrassBlocks#BLOCKS},
     * upstream meta order): one single-state blockstate per pair (the addStoneBlocks
     * degenerate-pure-block form) over a {@code cube_bottom_top} model — top/side ride the
     * BORROWED pre-coloured PNGs ({@code gt6:block/grass/top_<colour>}/
     * {@code side_<colour>}, byte-identical per the assets/README.md ledger) and the
     * BOTTOM face references the VANILLA {@code minecraft:block/dirt} model texture
     * directly (the upstream {@code IconContainerCopied(Blocks.dirt, 0, SIDE_BOTTOM)}
     * semantics, BlockGrass.java:102-104 — no PNG is copied). ZERO tintindex anywhere:
     * the colour is baked into the PNGs (Textures.java:530-565 pre-coloured sets) and a
     * GrassBlock-style biome tint is forbidden (decisions.p24-grass-behavior-trim).
     * Each pair gets its own item model parenting the block model. Same provider, same
     * pass, so the parent resolves in the ExistingFileHelper (the :29-33 precedent).
     *
     * <p>NAMING TRAP (the research card pin): the borrow is by CODE mapping, not by file
     * name — upstream meta 3 "LightGray" renders the {@code NORMAL} PNG and meta 0
     * "Green" the {@code MEDIUM} PNG (Textures.java:530-565). The borrowed files already
     * carry the VARIANT-semantics names ({@code top_green.png} et al), so this walk is
     * blind to the trap.
     */
    private void addGrassBlocks() {
        for (int i = 0; i < GTGrassBlocks.PATHS.size(); i++) {
            String tPath = GTGrassBlocks.PATHS.get(i);
            String tColour = GTGrassBlocks.textureOf(tPath);
            ModelFile tModel = models().cubeBottomTop(tPath,
                    modLoc("block/grass/side_" + tColour), // the forge (name, side, bottom, top) parameter order
                    mcLoc("block/dirt"), // the vanilla dirt bottom, the upstream copied-icon face
                    modLoc("block/grass/top_" + tColour));
            simpleBlock(GTGrassBlocks.BLOCKS.get(i).get(), tModel);
            itemModels().withExistingParent(tPath, modLoc("block/" + tPath));
        }
        LOGGER.info("GT6 grass blocks: {} per-pair blockstates over {} cube_bottom_top models (no tint)",
                GTGrassBlocks.PATHS.size(), GTGrassBlocks.PATHS.size());
    }

    /**
     * Task p30-w6-t1-trees-nine — the 27 GT tree blocks ({@link GT6TreeBlocks}): saplings
     * render the vanilla cross idiom over the borrowed SAPLING_SMALL PNGs (cutout layer,
     * the vanilla sapling render type), logs the axis blockstate over
     * cube_column(side/end, the vanilla log idiom), leaves a cube_all over the borrowed
     * LEAVES PNG (cutout_mipped, the vanilla leaves layer). All 36 textures are the
     * upstream iconsets PNGs byte-borrowed ({@code gt6:block/tree/*}, the
     * assets/README.md ledger face; the grass card pre-coloured-PNG precedent). The
     * Rainbowood leaves carry tintindex 0 over their grayscale PNG (task p38-issue1-4,
     * GitHub #4): the dynamic RAINBOW tint renders through the GT6TreeClientListener
     * BlockColor/ItemColor registrations (the upstream BlockTreeLeavesAB.java:129-139
     * face). The RED LINE
     * question (render_type in model JSON) is the FORGE 1.20.1 + NeoForge 21.1 shared
     * model face — no client code, no ItemBlockRenderTypes call. Item models parent the
     * block models (the grass walk shape).
     */
    private void addTreeBlocks() {
        for (int i = 0; i < gregtech6.registry.GT6TreeBlocks.KINDS.size(); i++) {
            gregtech6.block.tree.GT6TreeKind tKind = gregtech6.registry.GT6TreeBlocks.KINDS.get(i);
            String tSnake = tKind.snake();
            // sapling: cross + cutout
            ModelFile tSaplingModel = models().cross(tSnake + "_sapling",
                    modLoc("block/tree/sapling_" + tSnake)).renderType("cutout");
            simpleBlock(gregtech6.registry.GT6TreeBlocks.SAPLINGS.get(i).get(), tSaplingModel);
            itemModels().withExistingParent(tSnake + "_sapling", modLoc("block/" + tSnake + "_sapling"));
            // log: the axis blockstate (hand-built variants, the vanilla column idiom)
            // + cube_column model
            ModelFile tLogModel = models().cubeColumn(tSnake + "_log",
                    modLoc("block/tree/log_side_" + tSnake), modLoc("block/tree/log_top_" + tSnake));
            net.minecraft.world.level.block.RotatedPillarBlock tLog =
                    (net.minecraft.world.level.block.RotatedPillarBlock) gregtech6.registry.GT6TreeBlocks.LOGS.get(i).get();
            getVariantBuilder(tLog).forAllStates(tState -> ConfiguredModel.builder()
                    .modelFile(tLogModel)
                    .rotationX(tState.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)
                            == net.minecraft.core.Direction.Axis.X ? 90 : 0)
                    .rotationY(tState.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)
                            == net.minecraft.core.Direction.Axis.Z ? 90 : 0)
                    .build());
            itemModels().withExistingParent(tSnake + "_log", modLoc("block/" + tSnake + "_log"));
            // leaves: cube_all + cutout_mipped. The Rainbowood row adds tintindex 0 on
            // every face (the tintedCubeAll grammar) over its GRAYSCALE PNG — the world/
            // inventory tint tables are the GT6TreeClientListener RAINBOW registrations
            // (task p38-issue1-4, GitHub #4 — the upstream BlockTreeLeavesAB.java:129-139
            // face; the other 8 kinds keep their pre-coloured PNGs untinted).
            ModelFile tLeavesModel;
            if (tKind == gregtech6.block.tree.GT6TreeKind.RAINBOWOOD) {
                tLeavesModel = tintedCubeAll(tSnake + "_leaves",
                        modLoc("block/tree/leaves_" + tSnake)).renderType("cutout_mipped");
            } else {
                tLeavesModel = models().cubeAll(tSnake + "_leaves",
                        modLoc("block/tree/leaves_" + tSnake)).renderType("cutout_mipped");
            }
            simpleBlock(gregtech6.registry.GT6TreeBlocks.LEAVES.get(i).get(), tLeavesModel);
            itemModels().withExistingParent(tSnake + "_leaves", modLoc("block/" + tSnake + "_leaves"));
        }
        LOGGER.info("GT6 tree blocks: 27 per-pair blockstates over 9 cross + 9 column + 9 leaves models");
    }

    /**
     * Task p30-w6-t2-surface-blocks — the obtainable surface band ({@link GT6SurfaceBlocks}
     * PLANT_BAND + FALLEN_LOGS, 8 per-pair Block+Item blocks). The glowtus renders the
     * lily-pad face (BlockGlowtus = BlockBaseLilyPad): a hand-built 1px flat plate over the
     * borrowed GLOWTUS_RED.png (cutout — the texture carries transparency; the tintedSlab
     * element grammar), the bush/sand/turf are cube_all over the borrowed PNGs (the bush
     * grayscale pre-coloured at borrow time, the grass-card precedent), and the four
     * fallen-log woods ride the t1 log idiom verbatim (axis variants + cube_column over
     * the borrowed LOG_SIDE/TOP iconsets, renamed to the block ids at borrow time). All
     * 13 textures are upstream iconsets byte-borrows (assets/README.md rows this card).
     */
    private void addSurfacePlants() {
        // glowtus: the flat water plate (the vanilla lily-pad form)
        BlockModelBuilder tGlowtus = models().getBuilder("glowtus")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("pad", modLoc("block/glowtus"))
                .texture("particle", "#pad")
                .renderType("cutout");
        tGlowtus.element()
                .from(1.0F, 0.0F, 1.0F).to(15.0F, 1.0F, 15.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#pad"))
                .end();
        simpleBlock(GT6SurfaceBlocks.GLOWTUS.get(), tGlowtus);
        itemModels().withExistingParent("glowtus", modLoc("block/glowtus"));
        // the three cubes: bush (the leafy ball), black sand, turf
        for (String tPath : new String[] {"berry_bush", "black_sand", "turf"}) {
            Block tBlock = tPath.equals("berry_bush") ? GT6SurfaceBlocks.BERRY_BUSH.get()
                    : tPath.equals("black_sand") ? GT6SurfaceBlocks.BLACK_SAND.get() : GT6SurfaceBlocks.TURF.get();
            simpleBlock(tBlock, models().cubeAll(tPath, modLoc("block/" + tPath)));
            itemModels().withExistingParent(tPath, modLoc("block/" + tPath));
        }
        // the four fallen-log woods: the t1 log idiom (axis blockstate + cube_column)
        for (int i = 0; i < GT6SurfaceBlocks.FALLEN_LOGS.size(); i++) {
            String tPath = GT6SurfaceBlocks.FALLEN_LOGS.get(i).getId().getPath();
            String tWood = new String[] {"dead", "rotten", "mossy", "frozen"}[i];
            ModelFile tLogModel = models().cubeColumn(tPath,
                    modLoc("block/tree/log_side_" + tWood), modLoc("block/tree/log_top_" + tWood));
            net.minecraft.world.level.block.RotatedPillarBlock tLog =
                    (net.minecraft.world.level.block.RotatedPillarBlock) GT6SurfaceBlocks.FALLEN_LOGS.get(i).get();
            getVariantBuilder(tLog).forAllStates(tState -> ConfiguredModel.builder()
                    .modelFile(tLogModel)
                    .rotationX(tState.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)
                            == net.minecraft.core.Direction.Axis.X ? 90 : 0)
                    .rotationY(tState.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)
                            == net.minecraft.core.Direction.Axis.Z ? 90 : 0)
                    .build());
            itemModels().withExistingParent(tPath, modLoc("block/" + tPath));
        }
        LOGGER.info("GT6 surface plants: 8 blockstate bands (1 plate + 3 cubes + 4 axis columns), 8 models");
    }

    /**
     * Task p26-c-foam-block-family — the C-Foam block family band (upstream
     * BlockCFoamFresh/BlockCFoam + the MTE 32765 carrier). The four-state texture set of
     * upstream MultiTileEntityCFoam.getTexture2 (:132 — CFOAM_FRESH/CFOAM_FRESH_OWNED/
     * CFOAM_HARDENED/CFOAM_HARDENED_OWNED) maps onto:
     * <ul>
     * <li>{@code cfoam_fresh}/{@code cfoam} — one tinted cube per state, the 16
     *     {@code color} states SHARE the model (the colour rides the BlockState tint,
     *     the {@link gregtech6.client.foam.GTCFoamTintListener} BlockColor, tintindex 0
     *     — the ruling_color_dim form, NOT a 16-instance ladder);</li>
     * <li>{@code cfoam_owned} — the DRIED property swaps the sprite pair (fresh_owned/
     *     hardened_owned), the paint colour rides the BE tint (the same BlockColor);
     *     no BlockItem exists (the upstream showInCreative false), so no item model;</li>
     * <li>the two slabs — hand-built half-cube elements with tintindex 0 (the vanilla
     *     slab parents carry NO tintindex, so the tinted family needs own models); the
     *     DOUBLE variant shares the full-cube model (these slabs are spray-placed only,
     *     the double form is unreachable but must not miss its variant). The fresh forms
     *     have no BlockItem either (spray-only intermediate states).</li>
     * </ul>
     */
    private void addFoamBlocks() {
        // the two full blocks — 16 colour states over one tinted model each
        ModelFile tFresh = tintedCubeAll("block/cfoam_fresh", modLoc("block/cfoam_fresh"));
        getVariantBuilder(GT6FoamBlocks.CFOAM_FRESH.get())
                .forAllStates(aState -> ConfiguredModel.builder().modelFile(tFresh).build());
        ModelFile tHardened = tintedCubeAll("block/cfoam_hardened", modLoc("block/cfoam_hardened"));
        getVariantBuilder(GT6FoamBlocks.CFOAM.get())
                .forAllStates(aState -> ConfiguredModel.builder().modelFile(tHardened).build());
        itemModels().withExistingParent("cfoam", modLoc("block/cfoam_hardened"));

        // the owned carrier — the DRIED property over the owned sprite pair (upstream :132)
        ModelFile tOwnedWet = tintedCubeAll("block/cfoam_fresh_owned", modLoc("block/cfoam_fresh_owned"));
        ModelFile tOwnedDry = tintedCubeAll("block/cfoam_hardened_owned", modLoc("block/cfoam_hardened_owned"));
        getVariantBuilder(GT6FoamBlocks.CFOAM_OWNED.get())
                .partialState().with(GT6CFoamOwnedBlock.DRIED, false)
                .setModels(ConfiguredModel.builder().modelFile(tOwnedWet).build())
                .partialState().with(GT6CFoamOwnedBlock.DRIED, true)
                .setModels(ConfiguredModel.builder().modelFile(tOwnedDry).build());

        // the two slabs — half-cube tinted elements (BOTTOM/TOP/DOUBLE triads over the family
        // textures); the DRIED item parents its bottom-slab model (the vanilla slab item form)
        tintedSlabFamily("cfoam_fresh_slab", modLoc("block/cfoam_fresh"), GT6FoamBlocks.CFOAM_FRESH_SLAB.get());
        tintedSlabFamily("cfoam_slab", modLoc("block/cfoam_hardened"), GT6FoamBlocks.CFOAM_SLAB.get());
        itemModels().withExistingParent("cfoam_slab", modLoc("block/cfoam_slab_bottom"));
        LOGGER.info("GT6 cfoam blocks: 5 blockstate bands (2 full + owned DRIED pair + 2 slab triads), 8 tinted models");
    }

    /** The three-variant slab band (BOTTOM/TOP/DOUBLE) over one texture — the half-cube elements carry tintindex 0. */
    private void tintedSlabFamily(String aName, ResourceLocation aTexture, Block aSlab) {
        ModelFile tBottom = tintedHalfSlab(aName + "_bottom", aTexture, false);
        ModelFile tTop = tintedHalfSlab(aName + "_top", aTexture, true);
        ModelFile tDouble = tintedCubeAll("block/" + aName + "_double", aTexture);
        getVariantBuilder(aSlab)
                .partialState().with(SlabBlock.TYPE, SlabType.BOTTOM)
                .setModels(ConfiguredModel.builder().modelFile(tBottom).build())
                .partialState().with(SlabBlock.TYPE, SlabType.TOP)
                .setModels(ConfiguredModel.builder().modelFile(tTop).build())
                .partialState().with(SlabBlock.TYPE, SlabType.DOUBLE)
                .setModels(ConfiguredModel.builder().modelFile(tDouble).build());
    }

    /** One tinted half-cube slab model — the {@link #tintedCubeAll} shape cut to the lower/upper half. */
    private ModelFile tintedHalfSlab(String aName, ResourceLocation aTexture, boolean aTop) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("all", aTexture)
                .texture("particle", "#all");
        tModel.element()
                .from(0.0F, aTop ? 8.0F : 0.0F, 0.0F).to(16.0F, aTop ? 16.0F : 8.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#all").tintindex(0).cullface(aDir))
                .end();
        return tModel;
    }

    /**
     * Task p26-sensors-core — the three pioneer sensor blocks ({@link GT6Sensors#ROWS},
     * the Registration face): each one cube_all model over its OWN baked texture
     * ({@code gt6:block/<path>}, the assets/README.md bake-ledger entries) and ONE
     * 6-variant blockstate driving the full {@link GTSensorBlock#FACING} property — the
     * vanilla dispenser/observer rotation map (y = 0/90/180/270 on the horizontal ring,
     * x = 270/90 on up/down; the axle AXIS precedent for a property-driven variant
     * blockstate without the vanilla block-type coupling). The FACING property IS the
     * display/keypad face mirror (GTSensorBlockEntity#wrenchSetFacing writes it, the
     * TileEntityOven.setFrontFacing shape), so the baked digit-strip face re-orients with
     * the block exactly as the upstream thin-plate front icon did. Each BlockItem model
     * parents its block model (the crank one-line-per-row precedent).
     */
    private void addSensors() {
        for (GT6Sensors.SensorRow tRow : GT6Sensors.ROWS) {
            Block tBlock = GT6Sensors.BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tModel = models().cubeAll(tRow.path(), modLoc("block/" + tRow.path()));
            getVariantBuilder(tBlock).forAllStates(aState -> switch (aState.getValue(GTSensorBlock.FACING)) {
                case NORTH -> new ConfiguredModel[] {new ConfiguredModel(tModel)};
                case SOUTH -> new ConfiguredModel[] {new ConfiguredModel(tModel, 0, 180, false)};
                case WEST  -> new ConfiguredModel[] {new ConfiguredModel(tModel, 0, 270, false)};
                case EAST  -> new ConfiguredModel[] {new ConfiguredModel(tModel, 0, 90, false)};
                case UP    -> new ConfiguredModel[] {new ConfiguredModel(tModel, 270, 0, false)};
                case DOWN  -> new ConfiguredModel[] {new ConfiguredModel(tModel, 90, 0, false)};
            });
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
        }
        LOGGER.info("GT6 sensors: {} pioneer blockstates x 6 FACING variants (the oriented cube)", GT6Sensors.ROWS.size());
    }
    /**
     * Task p29-w3-nbtdesign-parts ③④ — the part-family expansion (Loader
     * :1138-1189). Every new-form part block gets ONE MODEL PER DESIGN VARIANT: the
     * upstream part renders {@code mTextures[mDesign][face]} with
     * {@code mTextures = new IIconContainer[bind8(NBT_DESIGNS)+1][6]}
     * (MultiTileEntityMultiBlockPart.java:138-146), the 1.20.1 form is the
     * {@code design} blockstate variant per model. Each model is the two-layer part
     * shape: the body cube over the BORROWED upstream colored textures with tintindex 0
     * on the body (the material tint, task p38-issue8-multipart-tint — the row
     * NBT_MATERIAL bakes in through GTMachineTintModel/ItemColor, the machine-domain
     * route; the crank grayscale deviation is retired for this family, the lightning-rod
     * part borrows keep their untinted cube_all) plus six 0.01-offset overlay decals (the
     * familyMachineModel decal form, the upstream colored/overlay two-texture pair — the
     * overlay layer stays UNCOLOURED, the BlockTextureMulti outer pass). The borrowed
     * texture path is
     * {@code block/parts/<family>/<design>/{colored,overlay}/{bottom,top,side}} — the
     * upstream {@code machines/multiblockparts/<family>/<design>/...} files verbatim
     * (assets/README.md attribution). DESIGNS-0 rows emit the property-less singleton.
     *
     * <p>Two retextures ride along (the borrow table's 18000/18101 rows): the Heat
     * Transmitter drops its large-boiler placeholder for the upstream
     * {@code heatacceptor} textures, and the coke oven bricks (= the upstream Fire
     * Bricks 18000, the reuse ruling) drop the placeholder for {@code firebricks}.
     */
    private void addParts() {
        // the DENSE WALLS (the MultiblockPartRow rows — the ctor convention pins their
        // family DESIGNS at 7, the metalwalldense texture family)
        for (var tRow : gregtech6.registry.GTMultiBlocks.WALL_ROWS) {
            GTMultiBlockPartBlock tBlock = (GTMultiBlockPartBlock) gregtech6.registry.GTMultiBlocks.WALL_BLOCKS_BY_PATH.get(tRow.path()).get();
            for (int d = 0; d <= tBlock.maxDesign(); d++) {
                ModelFile tModel = partModel(tRow.path() + "_design_" + d, "metalwalldense", d);
                getVariantBuilder(tBlock).partialState().with(tBlock.DESIGN, d).setModels(new ConfiguredModel(tModel));
            }
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path() + "_design_0"));
        }
        for (var tRow : gregtech6.registry.GTMultiBlocks.NEW_PART_ROWS) {
            if (!gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.containsKey(tRow.path())) continue; // machine_wall_tungsten — the reused Lightning Rod registration
            Block tBlock = gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get(tRow.path()).get();
            if (tRow.designs() > 0) {
                GTMultiBlockPartBlock tPart = (GTMultiBlockPartBlock) tBlock;
                for (int d = 0; d <= tRow.designs(); d++) {
                    ModelFile tModel = partModel(tRow.path() + "_design_" + d, tRow.textureFamily(), d);
                    getVariantBuilder(tBlock).partialState().with(tPart.DESIGN, d).setModels(new ConfiguredModel(tModel));
                }
            } else {
                ModelFile tModel = partModel(tRow.path(), tRow.textureFamily(), 0);
                getVariantBuilder(tBlock).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
            }
            // the BlockItem shows design 0 (the placed look)
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path() + (tRow.designs() > 0 ? "_design_0" : "")));
        }
        // the transmitter retexture (the heatacceptor borrow — the ONLY_ENERGY_IN base layer face)
        ModelFile tTransmitter = partModel("heat_transmitter", "heatacceptor", 0);
        getVariantBuilder(gregtech6.registry.GTMultiBlocks.HEAT_TRANSMITTER.get())
                .forAllStates(aState -> ConfiguredModel.builder().modelFile(tTransmitter).build());
        itemModels().withExistingParent("heat_transmitter", modLoc("block/heat_transmitter"));
    }

    /** One two-layer part model (body cube + six overlay decals) over the borrowed design textures. */
    /**
     * Task p29-w3-large-12 — the twelve large-machine controllers: ONE oriented cube
     * model per machine over the six borrowed colored faces (the basicmachines/&lt;family&gt;
     * frame 0 byte copies, the borrow_port_overlays naming shape), the 8 FACING x FORMED
     * states sharing the one model (the FORMED dual-model was the coke-oven enhancement;
     * the RCON formed assertion rides the blockstate property, not the model). The
     * overlay/active layers stay unborrowed — the ACTIVE visual is the render wave's
     * surface (the card exclusion), the borrow-or-declare rule keeps the byte count at
     * the six colored frames per family.
     */
    private void addLargeMachines() {
        for (gregtech6.registry.GT6LargeMachines.LargeMachineRow tRow : gregtech6.registry.GT6LargeMachines.ROWS) {
            Block tBlock = gregtech6.registry.GT6LargeMachines.BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tModel = models().cube(tRow.path(),
                    modLoc("block/" + tRow.texture() + "_colored_bottom"),
                    modLoc("block/" + tRow.texture() + "_colored_top"),
                    modLoc("block/" + tRow.texture() + "_colored_front"),
                    modLoc("block/" + tRow.texture() + "_colored_back"),
                    modLoc("block/" + tRow.texture() + "_colored_left"),
                    modLoc("block/" + tRow.texture() + "_colored_right"));
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY;
                switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                    case SOUTH -> tY = 180;
                    case WEST -> tY = 270;
                    case EAST -> tY = 90;
                    default -> tY = 0; // NORTH
                }
                return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
        }
    }

    /**
     * Task p31-massfab — the Large Matter Fabricator controller: the addImplosionCompressor
     * form over the upstream NBT_TEXTURE "largemassfab" family (Loader :1241, the six
     * borrowed basicmachines/largemassfab/colored faces). The 8 FACING x FORMED states
     * share the one oriented model (the RCON formed assertion rides the blockstate
     * property, not the model). Overlay/active layers stay unborrowed — the ACTIVE visual
     * is the render wave's surface.
     */
    /**
     * Task p32-logistics-lv3 — the Logistics Core controller (upstream meta 17997, the
     * NBT_TEXTURE "logisticscore" family). The dedicated textures have no port face yet,
     * so the model borrows the galvanized-steel wall family (the core's registered
     * material — the parts-datagen borrow form); the logisticscore wave retextures later.
     * The 8 FACING x FORMED states share the one oriented model (the massfab form).
     */
    private void addLogisticsCore() {
        Block tBlock = gregtech6.registry.GT6Logistics.LOGISTICS_CORE.get();
        String tBase = "block/parts/metalwall/0/colored/";
        ModelFile tModel = models().cube("logistics_core",
                modLoc(tBase + "bottom"), modLoc(tBase + "top"),
                modLoc(tBase + "side"), modLoc(tBase + "side"),
                modLoc(tBase + "side"), modLoc(tBase + "side"));
        getVariantBuilder(tBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        itemModels().withExistingParent("logistics_core", modLoc("block/logistics_core"));
    }

    private void addLargeMassfab() {
        Block tBlock = gregtech6.registry.GTMultiBlocks.MASSFAB.get();
        String tFamily = "largemassfab";
        ModelFile tModel = models().cube("large_massfab",
                modLoc("block/" + tFamily + "_colored_bottom"),
                modLoc("block/" + tFamily + "_colored_top"),
                modLoc("block/" + tFamily + "_colored_front"),
                modLoc("block/" + tFamily + "_colored_back"),
                modLoc("block/" + tFamily + "_colored_left"),
                modLoc("block/" + tFamily + "_colored_right"));
        getVariantBuilder(tBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        itemModels().withExistingParent("large_massfab", modLoc("block/large_massfab"));
    }

    /**
     * Task p31-fusion — the Fusion Reactor controller: the addLargeMassfab form over the
     * upstream NBT_TEXTURE "fusionreactor" family (Loader :1242, the six borrowed
     * basicmachines/fusionreactor/colored faces). The 8 FACING x FORMED states share the
     * one oriented model (the RCON formed assertion rides the blockstate property, not
     * the model). Overlay/active layers stay unborrowed — the ACTIVE visual is the render
     * wave's surface.
     */
    private void addFusionReactor() {
        Block tBlock = gregtech6.registry.GTMultiBlocks.FUSION_REACTOR.get();
        String tFamily = "fusionreactor";
        ModelFile tModel = models().cube("fusion_reactor",
                modLoc("block/" + tFamily + "_colored_bottom"),
                modLoc("block/" + tFamily + "_colored_top"),
                modLoc("block/" + tFamily + "_colored_front"),
                modLoc("block/" + tFamily + "_colored_back"),
                modLoc("block/" + tFamily + "_colored_left"),
                modLoc("block/" + tFamily + "_colored_right"));
        getVariantBuilder(tBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        itemModels().withExistingParent("fusion_reactor", modLoc("block/fusion_reactor"));
    }

    /**
     * Task p31-implosion — the Implosion Compressor controller: ONE oriented cube model
     * over the six borrowed colored faces (the upstream NBT_TEXTURE "implosioncompressor"
     * family, the addLargeMachines form — the 8 FACING x FORMED states share the one
     * model, the FORMED dual-model was the coke-oven enhancement and the RCON formed
     * assertion rides the blockstate property, not the model). Overlay/active layers stay
     * unborrowed — the ACTIVE visual is the render wave's surface.
     */
    private void addImplosionCompressor() {
        Block tBlock = gregtech6.registry.GTMultiBlocks.IMPLOSION_COMPRESSOR.get();
        String tFamily = "implosioncompressor";
        ModelFile tModel = models().cube("implosion_compressor",
                modLoc("block/" + tFamily + "_colored_bottom"),
                modLoc("block/" + tFamily + "_colored_top"),
                modLoc("block/" + tFamily + "_colored_front"),
                modLoc("block/" + tFamily + "_colored_back"),
                modLoc("block/" + tFamily + "_colored_left"),
                modLoc("block/" + tFamily + "_colored_right"));
        getVariantBuilder(tBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        itemModels().withExistingParent("implosion_compressor", modLoc("block/implosion_compressor"));
    }

    /**
     * Task p31-graagg — the Von da Graagg controller: the addImplosionCompressor form over
     * the upstream NBT_TEXTURE "vondagraagg" family (Loader :1280). The upstream family
     * carries ONE texture (colored/side == colored_front/side == bottom == top, byte-equal)
     * so all six faces borrow the same PNG (the autoclave all-faces-equal precedent); the
     * 8 FACING x FORMED states share the one oriented model (the RCON formed assertion
     * rides the blockstate property, not the model).
     */
    private void addVonDaGraagg() {
        Block tBlock = gregtech6.registry.GTMultiBlocks.VON_DA_GRAAGG.get();
        String tFamily = "vondagraagg";
        ModelFile tModel = models().cube("von_da_graagg",
                modLoc("block/" + tFamily + "_colored_bottom"),
                modLoc("block/" + tFamily + "_colored_top"),
                modLoc("block/" + tFamily + "_colored_front"),
                modLoc("block/" + tFamily + "_colored_back"),
                modLoc("block/" + tFamily + "_colored_left"),
                modLoc("block/" + tFamily + "_colored_right"));
        getVariantBuilder(tBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        itemModels().withExistingParent("von_da_graagg", modLoc("block/von_da_graagg"));
    }

    private ModelFile partModel(String aName, String aFamily, int aDesign) {
        String tBase = "block/parts/" + aFamily + "/" + aDesign;
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc(tBase + "/colored/bottom"))
                .texture("up", modLoc(tBase + "/colored/top"))
                .texture("north", modLoc(tBase + "/colored/side"))
                .texture("south", modLoc(tBase + "/colored/side"))
                .texture("west", modLoc(tBase + "/colored/side"))
                .texture("east", modLoc(tBase + "/colored/side"))
                .texture("overlay_bottom", modLoc(tBase + "/overlay/bottom"))
                .texture("overlay_top", modLoc(tBase + "/overlay/top"))
                .texture("overlay_side", modLoc(tBase + "/overlay/side"));
        // element 0 — the body cube (the colored layer; tintindex 0 = the material tint,
        // task p38-issue8-multipart-tint: the shared grayscale wall textures multiply the
        // row's NBT_MATERIAL — the upstream getTexture2 BlockTextureDefault(colored, mRGBa)
        // form, MultiTileEntityMultiBlockPart.java:234-236; the bake/ItemColor consumers
        // ride GTMachineTintModel/GTItemPaintTint, the machine-domain route)
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        // elements 1-6 — the overlay decals (the familyMachineModel 0.01-offset form)
        tModel.element() // north
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_side").cullface(Direction.NORTH)
                .end();
        tModel.element() // south
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_side").cullface(Direction.SOUTH)
                .end();
        tModel.element() // east
                .from(16.0F, 0.0F, 0.0F).to(16.01F, 16.0F, 16.0F)
                .face(Direction.EAST).texture("#overlay_side").cullface(Direction.EAST)
                .end();
        tModel.element() // west
                .from(-0.01F, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                .face(Direction.WEST).texture("#overlay_side").cullface(Direction.WEST)
                .end();
        tModel.element() // top
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_top").cullface(Direction.UP)
                .end();
        tModel.element() // bottom
                .from(0.0F, -0.01F, 0.0F).to(16.0F, 0.0F, 16.0F)
                .face(Direction.DOWN).texture("#overlay_bottom").cullface(Direction.DOWN)
                .end();
        return tModel;
    }

    /**
     * Task p29-w3-turbine-dynamo — the twelve Large Turbine + Large Dynamo controllers
     * (Loader_MultiTileEntities.java:1254-1257/:1259-1262/:1264-1267). ONE oriented cube
     * model per family over the borrowed multiblockmains groups (the front face composes
     * colored_front + overlay_front, the other five faces the plain pair — the partModel
     * two-layer form); the FACING states rotate (the addLargeBoiler table), the FORMED
     * variants map to the same model (the formed-look visual is the p9 pool, the
     * render-pass animation the render wave's). The BlockItem models parent their block
     * model. The textures are the NBT_TEXTURE upstream borrow (assets/README.md
     * attribution, the turbine_mains batch).
     */
    private void addTurbinesDynamo() {
        addTurbineFamily("steam", "largeturbine", gregtech6.registry.GT6Turbines.STEAM_ROWS.stream()
                .map(r -> Map.entry(r.path(), (net.minecraft.world.level.block.Block) gregtech6.registry.GT6Turbines.BLOCKS_BY_PATH.get(r.path()).get())).toList());
        addTurbineFamily("gas", "gasturbine", gregtech6.registry.GT6Turbines.GAS_ROWS.stream()
                .map(r -> Map.entry(r.path(), (net.minecraft.world.level.block.Block) gregtech6.registry.GT6Turbines.BLOCKS_BY_PATH.get(r.path()).get())).toList());
        addTurbineFamily("dynamo", "largedynamo", gregtech6.registry.GT6DynamoHousings.DYNAMO_ROWS.stream()
                .map(r -> Map.entry(r.path(), (net.minecraft.world.level.block.Block) gregtech6.registry.GT6DynamoHousings.BLOCKS_BY_PATH.get(r.path()).get())).toList());
    }

    /** One family walk — the shared model over the four variant blocks (the row paths name the item models). */
    private void addTurbineFamily(String aFamily, String aTexture, java.util.List<Map.Entry<String, net.minecraft.world.level.block.Block>> aRows) {
        String tBase = "block/turbine_mains/" + aTexture;
        BlockModelBuilder tModel = models().getBuilder("turbine_main_" + aFamily)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc(tBase + "/colored/bottom"))
                .texture("up", modLoc(tBase + "/colored/top"))
                .texture("north", modLoc(tBase + "/colored_front/side"))
                .texture("south", modLoc(tBase + "/colored/side"))
                .texture("west", modLoc(tBase + "/colored/side"))
                .texture("east", modLoc(tBase + "/colored/side"))
                .texture("overlay_bottom", modLoc(tBase + "/overlay/bottom"))
                .texture("overlay_top", modLoc(tBase + "/overlay/top"))
                .texture("overlay_side", modLoc(tBase + "/overlay/side"))
                .texture("overlay_front", modLoc(tBase + "/overlay_front/side"));
        // element 0 — the body cube (tintindex 0 = the material tint, task
        // p38-c2-controller-tint: the grayscale turbine_mains colored groups multiply the
        // row's NBT_MATERIAL — the upstream TileEntityBase10MultiBlockBase.getTexture2
        // :193 BlockTextureMulti(BlockTextureDefault(colored, mRGBa), overlay) form; the
        // bake/ItemColor consumers ride GTMachineTintModel/GTItemPaintTint, the
        // machine-domain route)
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        // elements 1-6 — the overlay decals (the partModel 0.01-offset form; the front decal
        // rides the overlay_front group)
        tModel.element() // north
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_front").cullface(Direction.NORTH)
                .end();
        tModel.element() // south
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_side").cullface(Direction.SOUTH)
                .end();
        tModel.element() // east
                .from(16.0F, 0.0F, 0.0F).to(16.01F, 16.0F, 16.0F)
                .face(Direction.EAST).texture("#overlay_side").cullface(Direction.EAST)
                .end();
        tModel.element() // west
                .from(-0.01F, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                .face(Direction.WEST).texture("#overlay_side").cullface(Direction.WEST)
                .end();
        tModel.element() // top
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_top").cullface(Direction.UP)
                .end();
        tModel.element() // bottom
                .from(0.0F, -0.01F, 0.0F).to(16.0F, 0.0F, 16.0F)
                .face(Direction.DOWN).texture("#overlay_bottom").cullface(Direction.DOWN)
                .end();
        ModelFile tFile = new ModelFile.UncheckedModelFile(modLoc("block/turbine_main_" + aFamily));
        for (Map.Entry<String, net.minecraft.world.level.block.Block> tRow : aRows) {
            getVariantBuilder(tRow.getValue()).forAllStates(aState -> {
                int tY = switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tFile).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.getKey(), modLoc("block/turbine_main_" + aFamily));
        }
    }

    /**
     * Task p32-placeables — the placeables band, half one: the Greg o'Lantern blockstate
     * (upstream MTE 32758, Loader_MultiTileEntities.java:2031). ONE model over the four
     * horizontal facings: the FRONT (facing) face carries the borrowed upstream
     * GREG_O_LANTERN icon (assets/README.md), the other five faces the vanilla jack_o_lantern
     * texture (the modern name of the upstream 1.7.10 Blocks.lit_pumpkin copy —
     * getTexture2 :38 = the GREG icon on mFacing + BlockTextureCopied elsewhere). The rotation table is the vanilla jack_o_lantern.json one (north=0,
     * east=90, south=180, west=270 — the carved face rotates WITH the facing).
     */
    private void addPlaceables() {
        BlockModelBuilder tModel = models().getBuilder("greg_o_lantern")
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("particle", mcLoc("block/jack_o_lantern"))
                .texture("north", modLoc("block/greg_o_lantern"))
                .texture("south", mcLoc("block/jack_o_lantern"))
                .texture("east", mcLoc("block/jack_o_lantern"))
                .texture("west", mcLoc("block/jack_o_lantern"))
                .texture("up", mcLoc("block/jack_o_lantern"))
                .texture("down", mcLoc("block/jack_o_lantern"));
        getVariantBuilder(gregtech6.registry.GT6Placeables.GREG_O_LANTERN.get()).forAllStates(aState -> {
            int tY = switch (aState.getValue(gregtech6.tileentity.misc.GT6GregOLanternBlock.FACING)) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0; // NORTH: the carved face at -z, no rotation
            };
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        // the sandwich: ONE fixed 12/16 layered box over the port-original sandwich art
        // (the per-ingredient display model band is the declared render cut)
        BlockModelBuilder tSandwich = models().getBuilder("sandwich")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("sandwich", modLoc("block/sandwich"))
                .texture("particle", "#sandwich");
        tSandwich.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 12.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#sandwich").cullface(aDir))
                .end();
        simpleBlock(gregtech6.registry.GT6Placeables.SANDWICH.get(), tSandwich);
        // the six placed piles (task p32-placeables, half two): fixed silhouettes over the
        // borrowed upstream icon pairs (assets/README.md) + the vanilla stone/log borrows;
        // tintindex 0 on the five material-tinted faces (GT6PlaceableTint reads the BE
        // material), the stick un-tinted (the W6 oak_log borrow form)
        placedPile(gregtech6.registry.GT6Placeables.PLACED_INGOT.get(), "placed_ingot", "block/placeable/ingot_sides", "block/placeable/ingot_top", 2, true, false);
        placedPile(gregtech6.registry.GT6Placeables.PLACED_PLATE.get(), "placed_plate", "block/placeable/plate_sides", "block/placeable/plate_top", 1, true, false);
        placedPile(gregtech6.registry.GT6Placeables.PLACED_GEM_PLATE.get(), "placed_gem_plate", "block/placeable/plate_gem_sides", "block/placeable/plate_gem_top", 1, true, false);
        placedPile(gregtech6.registry.GT6Placeables.PLACED_SCRAP.get(), "placed_scrap", "block/placeable/scrap_sides", "block/placeable/scrap_top", 1, true, false);
        placedPile(gregtech6.registry.GT6Placeables.PLACED_ROCK.get(), "placed_rock", "block/stone", "block/stone", 3, true, true);
        placedPile(gregtech6.registry.GT6Placeables.PLACED_STICK.get(), "placed_stick", "block/oak_log", "block/oak_log", 2, false, true);
    }

    /** One placed-pile model: an inset full-footprint box of aHeight/16, tinted per aTinted; aVanilla textures resolve against minecraft. */
    private void placedPile(Block aBlock, String aName, String aSides, String aTop, int aHeight, boolean aTinted, boolean aVanilla) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("sides", aVanilla ? mcLoc(aSides) : modLoc(aSides))
                .texture("top", aVanilla ? mcLoc(aTop) : modLoc(aTop))
                .texture("particle", "#sides");
        float tTop = aHeight;
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, tTop, 16.0F)
                .allFaces((aDir, aFace) -> {
                    aFace.texture(aDir.getAxis() == net.minecraft.core.Direction.Axis.Y ? "#top" : "#sides");
                    if (aTinted) aFace.tintindex(0);
                })
                .end();
        simpleBlock(aBlock, tModel);
    }

    /**
     * Task p38-issue7-kitchen-models — the kitchen family (GT6Kitchen.java:89-113), the
     * four manual kitchen blocks that rendered as the magenta-black missing-model
     * checkerboard (the datagen face had zero kitchen coverage; the GT6Kitchen.java:58-59
     * plain-cube placeholder declaration never landed). This is the real upstream form
     * instead, the render geometry replicated as vanilla element models:
     * <ul>
     * <li>the pot pair + the bowl: the hollow tub — 2px-thick walls 8px tall over a 2px
     *     base slab (MultiTileEntityBathingPot.setBlockBounds2 :345-356; the
     *     MultiTileEntityMixingBowl boxes :368-373 are the same shape); the walls tile as
     *     four non-overlapping panels, so the only coplanar faces left are
     *     opposite-facing pairs (backface culling keeps them from fighting);</li>
     * <li>the juicer: the low tub — 2px walls 4px tall over a 1px base (setBlockBounds2
     *     :228-237) plus the central 4x7x4 pestle column (pass 6, the middle
     *     textures).</li>
     * </ul>
     * The interior fluid-surface pass (the mDisplay renderer, pot/juicer pass 5) is the
     * declared pool cut — the static model is the EMPTY vessel, the cavity floor being
     * the base slab's up face. Textures: the borrowed upstream grayscale colored/ tile
     * sets (assets/README.md, the kitchen section) with tintindex 0 on every face; since
     * task p38-c3-kitchen-tint-shape the reservation is WIRED — the baked
     * GTMachineTintModel world half and the GTItemPaintTint inventory half resolve the row
     * material's mRGBa (WoodTreated/StainlessSteel/Ceramic, the GT6Kitchen
     * .paintableBlockArray census) over these faces, the upstream getTexture2 multiply
     * (MultiTileEntityBathingPot.java:375-379).
     */
    private void addKitchen() {
        addKitchenBlock(GT6Kitchen.BATHING_POT_WOOD.get(), "bathing_pot_wood", "bathing_pot_wood", false);
        addKitchenBlock(GT6Kitchen.BATHING_POT_STEEL.get(), "bathing_pot_steel", "bathing_pot", false);
        addKitchenBlock(GT6Kitchen.MIXING_BOWL.get(), "mixing_bowl", "mixing_bowl", false);
        addKitchenBlock(GT6Kitchen.JUICER.get(), "juicer", "juicer", true);
    }

    /** One kitchen block: its element model + the property-free single-state blockstate + the BlockItem parent (the addAnvils form). */
    private void addKitchenBlock(Block aBlock, String aName, String aFamily, boolean aJuicer) {
        ModelFile tModel = aJuicer ? kitchenJuicerModel(aName, aFamily) : kitchenTubModel(aName, aFamily);
        simpleBlock(aBlock, tModel);
        itemModels().withExistingParent(aName, tModel.getLocation());
    }

    /** The kitchen texture band (block/tools/&lt;family&gt;/&lt;face&gt;.png, the borrowed upstream colored/ tiles). */
    private BlockModelBuilder kitchenTextures(BlockModelBuilder aModel, String aFamily, boolean aJuicer) {
        aModel.texture("sides", modLoc("block/tools/" + aFamily + "/sides"))
                .texture("insides", modLoc("block/tools/" + aFamily + "/insides"))
                .texture("top", modLoc("block/tools/" + aFamily + "/top"))
                .texture("bottom", modLoc("block/tools/" + aFamily + "/bottom"))
                .texture("particle", "#sides");
        if (aJuicer) aModel.texture("middleside", modLoc("block/tools/" + aFamily + "/middleside"))
                .texture("middletop", modLoc("block/tools/" + aFamily + "/middletop"));
        return aModel;
    }

    /** The panel face texture: up "top", down "bottom", the cavity side "insides", everything else (outer + end-caps) "sides". */
    private String kitchenPanelTexture(Direction aDir, Direction aOutward) {
        if (aDir == Direction.UP) return "#top";
        if (aDir == Direction.DOWN) return "#bottom";
        return aDir.getOpposite() == aOutward ? "#insides" : "#sides";
    }

    /** The hollow-tub model (pot pair + bowl): 2px walls y 2..8 + the 2px base slab (its up face is the cavity floor). */
    private ModelFile kitchenTubModel(String aName, String aFamily) {
        BlockModelBuilder tModel = kitchenTextures(models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block"))), aFamily, false);
        tModel.element().from(0.0F, 0.0F, 0.0F).to(16.0F, 2.0F, 16.0F)
                .allFaces((aDir, aFace) -> {
                    aFace.texture(aDir == Direction.UP ? "#top"
                            : aDir == Direction.DOWN ? "#bottom" : "#sides");
                    aFace.tintindex(0);
                }).end();
        kitchenTubWall(tModel, 0.0F, 0.0F, 16.0F, 2.0F, Direction.NORTH);
        kitchenTubWall(tModel, 0.0F, 14.0F, 16.0F, 16.0F, Direction.SOUTH);
        kitchenTubWall(tModel, 0.0F, 2.0F, 2.0F, 14.0F, Direction.WEST);
        kitchenTubWall(tModel, 14.0F, 2.0F, 16.0F, 14.0F, Direction.EAST);
        return tModel;
    }

    /** One tub wall panel: (aMinX, aMinZ)-(aMaxX, aMaxZ) footprint, y 2..8 (the down face lands opposite-facing on the base slab). */
    private void kitchenTubWall(BlockModelBuilder aModel, float aMinX, float aMinZ, float aMaxX, float aMaxZ, Direction aOutward) {
        aModel.element().from(aMinX, 2.0F, aMinZ).to(aMaxX, 8.0F, aMaxZ)
                .allFaces((aDir, aFace) -> {
                    aFace.texture(kitchenPanelTexture(aDir, aOutward));
                    aFace.tintindex(0);
                }).end();
    }

    /**
     * The juicer model (MultiTileEntityJuicer.java:228-237): the low tub — 2px walls y
     * 0..4 over a 1px base (footprint inset 2..14) — plus the central 4x7x4 pestle column
     * (pass 6). The walls carry NO down face (coplanar same-facing with the base slab's
     * own), and the base slab carries side faces only as down/up (its side ring is tiled
     * by the walls' own outer faces).
     */
    private ModelFile kitchenJuicerModel(String aName, String aFamily) {
        BlockModelBuilder tModel = kitchenTextures(models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block"))), aFamily, true);
        tModel.element().from(2.0F, 0.0F, 2.0F).to(14.0F, 1.0F, 14.0F)
                .face(Direction.DOWN).texture("#bottom").tintindex(0).end()
                .face(Direction.UP).texture("#top").tintindex(0).end()
                .end();
        kitchenJuicerWall(tModel, 2.0F, 2.0F, 14.0F, 4.0F, Direction.NORTH);
        kitchenJuicerWall(tModel, 2.0F, 12.0F, 14.0F, 14.0F, Direction.SOUTH);
        kitchenJuicerWall(tModel, 2.0F, 4.0F, 4.0F, 12.0F, Direction.WEST);
        kitchenJuicerWall(tModel, 12.0F, 4.0F, 14.0F, 12.0F, Direction.EAST);
        tModel.element().from(6.0F, 0.0F, 6.0F).to(10.0F, 7.0F, 10.0F)
                .face(Direction.UP).texture("#middletop").tintindex(0).end()
                .face(Direction.NORTH).texture("#middleside").tintindex(0).end()
                .face(Direction.SOUTH).texture("#middleside").tintindex(0).end()
                .face(Direction.WEST).texture("#middleside").tintindex(0).end()
                .face(Direction.EAST).texture("#middleside").tintindex(0).end()
                .end();
        return tModel;
    }

    /** One juicer wall panel: (aMinX, aMinZ)-(aMaxX, aMaxZ) footprint, y 0..4, the down face omitted (the base slab owns the y=0 plane). */
    private void kitchenJuicerWall(BlockModelBuilder aModel, float aMinX, float aMinZ, float aMaxX, float aMaxZ, Direction aOutward) {
        BlockModelBuilder.ElementBuilder tElement = aModel.element().from(aMinX, 0.0F, aMinZ).to(aMaxX, 4.0F, aMaxZ);
        for (Direction tDir : Direction.values()) {
            if (tDir == Direction.DOWN) continue;
            tElement.face(tDir).texture(kitchenPanelTexture(tDir, aOutward)).tintindex(0).end();
        }
        tElement.end();
    }
}
