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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
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
import gregtech6.registry.GT6Kitchen; // issue7 tail-append
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
import gregtech6.block.surface.GT6SurfaceVariants;
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
 * Blockstate + block model provider (task example-machine — the first blockstates this
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
 * <p>Task fluid-pipes (W1) appended the fluid pipes: a cube_all placeholder over
 * {@code gt6:textures/block/fluid_pipe_wood.png} with a variant per
 * {@link gregtech6.block.pipe.GTFluidPipeBlock} CONNECTIONS mask value (0..63, the 6-bit
 * connection state — W1 renders every mask with the same model, the per-connection model
 * picking is the render pool item). The texture was an inline-script generated placeholder
 * PNG (same hand-rolled style as mdk/tools/gen_gui_textures.py), not JSON. Task
 * tex-pipe-textures UPGRADED the three pipe connector families (fluid / item /
 * logistics wire) onto the shared per-set two-layer tinted models — see
 * {@link #tintedPipeModel} / {@link #pipeBlockstate}; the placeholder PNG generation and
 * the W1 model shape are retired (the LD wire placeholders keep their item_pipe.png
 * borrow-facing texture — that family is a separate card).
 */
public final class GT6BlockStates extends BlockStateProvider {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("gt6");

    /** The runData-side tint census counter (machineModel invocations — the datagen-JVM half of the pinned 21x3 audit). */
    private int mMachineTintModels;

    /** The runData-side barrel tint census counter (task barrel-paint-render — the datagen-JVM half of the pinned 16 audit). */
    private int mBarrelTintModels;

    public GT6BlockStates(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, GT6DataGenerators.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        Block tChest = GTBlockEntities.EXAMPLE_CHEST.get();
        simpleBlock(tChest, models().cubeAll("example_chest", modLoc("block/example_chest")));
        itemModels().withExistingParent("example_chest", mcLoc("item/generated")).texture("layer0", modLoc("block/example_chest"));
        // task tex-pipe-textures — the three pipe connector families leave the
        // cube_all placeholder era for the upstream material-set DUAL-LAYER tinted form.
        // Upstream every material-icon render is two passes: pass 0 = the set art
        // multiplied by mRGBa, pass 1 = the untinted <SET>_OVERLAY black outline
        // (TextureSet.java:145-181 getIcon/getIconPasses). The connector side segment
        // picks INDEX_BLOCK_PIPE_SIDE = the 'pipeSide' art added to every set
        // (GT_API.java:158 addToAll); the live sets here are wood (the two fluid rows —
        // the MT wood factory = SET_WOOD) and copper (the 18 item pipe rows — Brass/
        // Constantan/CobaltBrass ride the clloymachine factory = SET_COPPER, MT.java:716).
        // The logistics wire renders its own dedicated pair instead of the set art
        // (MultiTileEntityWireLogistics :48-49 = iconsets/LOGISTICS_WIRE x mRGBa +
        // LOGISTICS_WIRE_OVERLAY; the NBT_MATERIAL column is MT.NULL, Loader :1819, so
        // the base art stays white). The restrictive item rows add the upstream third
        // pass — the PIPE_RESTRICTOR plate (MultiTileEntityPipeItem.java:280-281
        // mRenderType 1; the :76-82 registration rows carry NBT_PIPERENDER 1) — as the
        // second decal band. The runtime tint is the row material's fRGBaSolid (the
        // NBT_COLOR getRGBInt(fRGBaSolid) registration column) through the
        // GTMachinePaintTint chain (the GTMachineTintModel bake walk, like every other
        // NBT_MATERIAL domain). Connection-aware geometry (the core + per-diameter arms,
        // TileEntityBase10ConnectorRendered :264-265) is the L-level render-pool card,
        // NOT this one — the fallback shows the pipeSide art on every face of the
        // placeholder cube (assets/README.md declaration).
        ModelFile tWoodPipe = tintedPipeModel("block/materialicons/wood/pipe_side",
                modLoc("block/materialicons/wood/pipe_side"), modLoc("block/materialicons/wood/pipe_side_overlay"));
        ModelFile tCopperPipe = tintedPipeModel("block/materialicons/copper/pipe_side",
                modLoc("block/materialicons/copper/pipe_side"), modLoc("block/materialicons/copper/pipe_side_overlay"));
        ModelFile tCopperPipeRestrictive = tintedPipeModel("block/materialicons/copper/pipe_side_restrictive",
                modLoc("block/materialicons/copper/pipe_side"), modLoc("block/materialicons/copper/pipe_side_overlay"),
                modLoc("block/iconsets/pipe_restrictor"));
        ModelFile tLogisticsWire = tintedPipeModel("block/iconsets/logistics_wire",
                modLoc("block/iconsets/logistics_wire"), modLoc("block/iconsets/logistics_wire_overlay"));
        pipeBlockstate(GTFluidPipes.WOOD_FLUID_PIPE_SMALL.get(), tWoodPipe);
        pipeBlockstate(GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM.get(), tWoodPipe);
        // task pipe-item — the item pipe family: one shared model per form, the six
        // restrictive variants over the restrictor-band twin (the upstream mRenderType 1)
        for (GTItemPipes.ItemPipeRow tItemRow : GTItemPipes.ROWS) {
            pipeBlockstate(GTItemPipes.BLOCKS_BY_PATH.get(tItemRow.path()).get(),
                    tItemRow.variant().suffix.startsWith("restrictive") ? tCopperPipeRestrictive : tCopperPipe);
        }
        pipeBlockstate(GT6Logistics.LOGISTICS_WIRE.get(), tLogisticsWire); // task logistics-lv2 — the single logistics connector row
        addWire(GTWires.WIRE_ELECTRIC_1X.get());
        addWire(GTWires.WIRE_ELECTRIC_2X.get());
        // task p10: the two wire loops SHARE one (set -> model) map — models().getBuilder
        // APPENDS to a same-named builder, so a second local map would stack a duplicate
        // element onto the shared copper/wire model JSON (caught by the runData diff).
        Map<String, ModelFile> tWireShared = new HashMap<>();
        addWireFamily(tWireShared); // task wire-family-w1 ⑥ — the 620-block loop, isolated section
        addRedstoneWireFamily(tWireShared); // task wire-redstone-family — the 6-block redstone loop
        addLaserWireFamily(tWireShared); // task wire-laser-placeholder — the 1-block laser loop (render target upgraded to the baked fiber model by wire-fiber-texture)
        addOven();
        addMachine(GTMachines.SHREDDER.get(), "shredder"); // task basicmachine-family ④
        addMachine(GTMachines.CRUSHER.get(), "crusher");
        addMachine(GTMachines.LATHE.get(), "lathe");
        // task machine-tiers-doinject ⑧: the T2-T4 ladder, +9 rows (the tier blocks share
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
        addDryer(); // task dryer-family
        addDistillery(); // task distillery-family
        addCanner(); // task canner-machine
        addKineticTrio(); // task w1-sifter-compressor-wiremill
        addPress(); // task w1-press-extruder-molds
        addExtruder(); // task w1-press-extruder-molds
        addUlvLadder(); // task c-ulv-machine-ladder — the six ULV rows (family textures, the addCanner shape)
        addRollLadders(); // task w1-kinetic-roll-ladder — the four roll-ladder RU families (family textures, the addCanner shape)
        addProcessMachines(); // task w1-kinetic-process-ladder — the six process families (family textures, the addCanner shape)
        addEuHuFamilies(); // task w1-eu-hu-families — the seven eu-hu families (family textures, the addCanner shape)
        addEuSpecialFamilies(); // task w2-eu-special — the Autocrafter/Lightning/Laminator families (family textures, the addCanner shape)
        addExoticFamilies(); // task w2-exotic-energy — the six exotic-energy families (family textures, the addCanner shape)
        addEuCoreMachines(); // task w2-eu-core-5tier — the five eu-core families (family textures, the addCanner shape)
        addMassfabMachines(); // task massfab — the small Matter Fabricator 5-ladder (family textures, the addCanner shape)
        addQuMachines(); // task qu-scanner-replicator — the Molecular Scanner T3 + the Matter Replicator T1-T3 (family textures, the addCanner shape)
        addP34MachinePair(); // task machines-burner-plantalyzer — the Burner Mixer ladder + the Plantalyzer 5-ladder (family textures, the addCanner shape)
        addHuTuFamilies(); // task w2-hu-tu-piggyback — the seven hu-tu families (family textures, the addCanner shape)
        addHeatSmelterFamilies(); // task w3-heat-smelter — the Smelter ladder + the Melter single (family textures, the addCanner shape)
        addRoastingFamilies(); // task w4-eu-bridge — the Roasting Oven ladder (the "roaster" family textures)
        addP34MachineFamilies(); // task machines-bumblelyzer-crucible — the Bumblelyzer 5-ladder + the Crystallisation Crucible 4-ladder (family textures, the addCanner shape)
        addElectricBridges(); // task w4-eu-bridge — the three EU-bridge converter families (the borrowed upstream colored front/side pairs)
        addLaserFamilies(); // task qu-laser-domain — the CO2 Laser + Laser Absorber families (the borrowed upstream colored front/side pairs)
        addMagicAbsorber(); // task magic-absorber — the Magic Field Absorber single (the six-way facing cube)
        addStaticStorages(); // task storage-static-batch
        addAdvancedCraftingTable(); // task act-machine
        // task paintable-tint-render: the datagen-JVM census half — 209 machine blocks x
        // 3 models (the six ULV rows joined at task c-ulv-machine-ladder; the roll ladders
        // and the six P29 W1 process families joined at their owning cards; the seven eu-hu
        // families at task w1-eu-hu-families; the three eu-special families at task
        // w2-eu-special; the six exotic-energy families at task
        // w2-exotic-energy; the five eu-core 5-tier families at task
        // w2-eu-core-5tier; the seven hu-tu piggyback families at task
        // w2-hu-tu-piggyback; the Smelter ladder + Melter single at task
        // w3-heat-smelter), // matching the paintableBlockArray() client registration census
        // (the offline JUnit half walks the generated tree and pins the same 627; the ACT
        // rides its own single-state model OUTSIDE the paint-array census — the
        // GTAdvancedCraftingTableBlock carries no ACTIVE/RUNNING payload, and the
        // family-wide paint extension stays pooled).
        addHeatExchanger(); // task w3-heat-smelter // task w3-heat-smelter — the Large Heat Exchanger controller (the two-layer front-bearing form)
        addReactorCore(); // task debt-reactor-b-2x2-be — the 2x2 reactor core (flat cube over the borrowed faces)
        LOGGER.info("GT6 machine paint tint: {} machine models tinted (209 blocks x 3 + the ACT single-state model, addOven/addMachine/addDryer/addDistillery/addCanner/addKineticTrio/addPress/addExtruder/addUlvLadder/addRollLadders/addProcessMachines/addEuHuFamilies/addEuSpecialFamilies/addExoticFamilies/addEuCoreMachines/addHuTuFamilies/addHeatSmelterFamilies/addAdvancedCraftingTable)", mMachineTintModels);
        addMultiBlocks();
        addBarrel();
        addEnergySource();
        addFeBattery(); // task eu-bridge-outbound (tail-append; shared serial file)
        addFeConverter(); // task b-fe-converter-machine (tail-append; shared serial file)
        addFeSource(); // task b-fe-converter-machine (tail-append; shared serial file)
        addTestMachines(); // task testmachine-blockstates — the two dev BE-framework blocks
        addPrefixBlocks(); // task prefixblock-render ①
        addCrank(); // task engine-crank
        addAxles(); // task axle-family
        addAttachments(); // task tap-funnel-attachment
        addSteamEngines(); // task engine-steam
        addDieselEngines(); // task engine-diesel
        addBurningBoxes(); // task burning-box-family
        addBoilers(); // task boiler-tank
        addHoppers(); // task storage-hopper-family
        addGearBoxTransformer(); // task gearbox-transformer
        addElectricTransformer(); // task c-ulv-lv-transformer
        addLDEnergyFamilies(); // task energy-tail-machines
        addWaterWheel(); // task c-water-wheel
        addLongDistancePipes(); // task long-distance-pipes — the 16 wire metas + the two endpoints
        addElectricDynamoUlv(); // task c-ulv-dynamo-row — the Electric Dynamo T0 row
        addDynamoLadders(); // task c1-dynamo-bowl-models — the Electric/Flux T1-T5 ladder rows
        addBatteryBoxes(); // task w4-battery-storage — the 12-box storage face
        addCrystalChargers(); // task energy-tail-machines — the 20-row LU charge face
        addZpmDechargers(); // task energy-zpm-dechargers — the two-row discharge face
        addLargeBoiler(); // task large-boiler
        addLightningRod(); // task lightning-rod
        addParts(); // task w3-nbtdesign-parts — the part-family expansion (per-design variants)
        addTanks(); // task w3-tank-valves — the 25 valve controllers
        addTurbinesDynamo(); // task w3-turbine-dynamo — the Large Turbine + Large Dynamo controllers
        addLargeMachines(); // task w3-large-12 — the twelve large-machine controllers
        addImplosionCompressor(); // task implosion — the Implosion Compressor controller
        addVonDaGraagg(); // task graagg — the Von da Graagg controller
        addBedrockDrill(); // task bedrock-drill — the Bedrock Mining Drill controller
        addLargeMassfab(); // task massfab — the Large Matter Fabricator controller
        addFusionReactor(); // task fusion — the Fusion Reactor controller
        addLogisticsCore(); // task logistics-lv3 — the Logistics Core controller
        addLargeCrucible(); // task crucible-multiblock
        addDistillationTowers(); // task w3-distill-crucible — the twin tower controllers
        addStoneBlocks(); // task stoneblocks-16item-registry-split — the 272 per-pair (stone, variant) blocks
        addStoneSlabs(); // task debt-slab-gap — the 272 per-pair stone slabs (the upstream mSlabs[0] face)
        addGrassBlocks(); // task grass-block — the 6 per-pair GT grass variants
        addTreeBlocks(); // task w6-t1-trees-nine — the 27 per-pair tree blocks
        addFoamBlocks(); // task c-foam-block-family — the C-Foam pair + slabs + the owned carrier
        addSensors(); // task sensors-core — the three pioneer sensor blocks
        addAnvils(); // task c-anvil — the stone anvil pair
        addSurfaceBand(); // task w6-rocks-sticks — the surface rock trio + the stick (shared models, no items)
        addSurfacePlants(); // task w6-t2-surface-blocks — the plant quartet + the four fallen-log woods
        addHive(); // task bees-lv2 — the bumble hive (the tinted body + the six-overlay two-layer form)
        addBumbliary(); // task bees-lv3-b-bumbliary — the Bumbliary pair (the hive two-layer grammar over the facing cube)
        addPlaceables(); // task placeables — the Greg o'Lantern (the carved-front cube)
        addRails(); // task rails-31-blocks — the 31-rail family (the vanilla rail grammar)
        addPortals(); // task portals-mini-nether-end — the two miniature portals (the ACTIVE cube swap)
        addKitchen(); // task issue7-kitchen-models — the four kitchen blocks (the upstream hollow-tub element forms)
        addMeasuringPot(); // task issue45-c3 — the Measuring Pot (the two-layer colored+overlay sub-cube tub)
    }

    /**
     * The rail family (task rails-31-blocks, 31 blocks): the vanilla rail grammar over
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
     * same model). Item models are the vanilla rail item form — {@code item/generated}
     * over the flat arm's own texture (the vanilla item/rail.json shape). The former
     * parent-the-flat-model form inherits the 16x1x16 {@code rail_flat} slab, which is
     * near-invisible under the inventory's 3D item camera (GitHub #22, the blank-slot
     * report); the world face is untouched (the block models keep the vanilla parents).
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
                itemModels().withExistingParent(tRow.path(), mcLoc("item/generated")).texture("layer0", modLoc("block/rail_straight_" + tSlug));
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
                itemModels().withExistingParent(tRow.path(), mcLoc("item/generated")).texture("layer0", modLoc("block/" + tBand + tSlug));
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
        itemModels().withExistingParent(GT6Rails.ROAD_PATH, mcLoc("item/generated")).texture("layer0", modLoc("block/rail_road_stripe"));
        LOGGER.info("GT6 rail family: {} material blockstates + the road stripe", GT6Rails.ROWS.size());
    }

    /** One rail model: the vanilla template parent + the rail texture override (the texture id is block/-prefixed). */
    private ModelFile railModel(String aName, String aParent, String aTextureBand) {
        return models().withExistingParent(aName, mcLoc(aParent)).texture("rail", modLoc("block/" + aTextureBand));
    }

    /**
     * Task portals-mini-nether-end — the two miniature portals ({@link GT6Portals}).
     * ONE blockstate per portal over TWO element models driven by the ACTIVE property:
     * the faithful transcription of the upstream 13-pass render
     * (MultiTileEntityMiniPortal.java:273-323, issue #23 / task 23a-portal-frame).
     * Upstream pass 0 = the 1px-inset face cube ({@code PX_P[1]..PX_N[1]}, :283) rendered
     * ONLY when active (:318 — the inactive arm answers the null inactive texture :323,
     * i.e. a hollow see-through frame); passes 1-12 = the twelve 2x2px cube-edge beams
     * (:285-296, four per axis). Frame textures: Nether = obsidian (Nether.java:133),
     * End = the end_portal_frame TOP face (End.java:128 — the former end_stone stand-in
     * was a mis-borrow). Portal faces: the animated vanilla nether_portal (on the
     * translucent layer, ItemBlockRenderTypes.java:269-271) / the owned near-black
     * mini_portal_end.png (vanilla ships no end-portal block texture, the special
     * end-portal effect is a tile renderer, not a texture). The see-through premise is
     * the noOcclusion block property (the GT6Portals row comment). No item-model
     * orientation (the portals are facing-free).
     */
    private void addPortals() {
        portalFrame(GT6Portals.PORTAL_NETHER.get(), "mini_portal_nether", "block/obsidian", "block/nether_portal", true);
        portalFrame(GT6Portals.PORTAL_END.get(), "mini_portal_end", "block/end_portal_frame_top", "gt6:block/mini_portal_end", false);
        // the BlockItem models parent the FRAME cage (the sensors walk shape)
        itemModels().withExistingParent("mini_portal_nether", modLoc("block/mini_portal_nether_frame"));
        itemModels().withExistingParent("mini_portal_end", modLoc("block/mini_portal_end_frame"));
        LOGGER.info("GT6 portals: 2 blockstates x ACTIVE 12-beam frame/inset-portal-face models");
    }

    /** The twelve 2px edge beams, {fromX, fromY, fromZ, toX, toY, toZ} in px — upstream sBlockBounds[1..12] (MultiTileEntityMiniPortal.java:285-296) verbatim. */
    private static final int[][] PORTAL_BEAMS = {
        { 0,  0,  0, 16,  2,  2}, { 0, 14,  0, 16, 16,  2}, { 0,  0, 14, 16,  2, 16}, { 0, 14, 14, 16, 16, 16}, // the four X edges
        { 0,  2,  0,  2, 14,  2}, {14,  2,  0, 16, 14,  2}, { 0,  2, 14,  2, 14, 16}, {14,  2, 14, 16, 14, 16}, // the four Y edges
        { 0,  0,  2,  2,  2, 14}, {14,  0,  2, 16,  2, 14}, { 0, 14,  2,  2, 16, 14}, {14, 14,  2, 16, 16, 14}, // the four Z edges
    };

    /** True when a beam face lies ON the block hull — the only faces a cullface may legitimately hide (interior cage faces must always draw). */
    private static boolean portalBeamOnHull(int[] aBeam, Direction aDir) {
        return switch (aDir.getAxis()) {
            case X -> aDir == Direction.EAST ? aBeam[3] == 16 : aBeam[0] == 0;
            case Y -> aDir == Direction.UP ? aBeam[4] == 16 : aBeam[1] == 0;
            case Z -> aDir == Direction.SOUTH ? aBeam[5] == 16 : aBeam[2] == 0;
        };
    }

    /** One beam element over the frame texture (cullface on the hull faces only). */
    private void portalBeam(BlockModelBuilder aModel, int[] aBeam) {
        aModel.element()
                .from(aBeam[0], aBeam[1], aBeam[2]).to(aBeam[3], aBeam[4], aBeam[5])
                .allFaces((aDir, aFace) -> {
                    aFace.texture("#frame");
                    if (portalBeamOnHull(aBeam, aDir)) aFace.cullface(aDir);
                }).end();
    }

    /**
     * One portal's ACTIVE pair: false = the 12-beam cage ONLY (no face — the hollow
     * upstream inactive form), true = the same cage + the 1px-inset face cube (upstream
     * pass 0, :283). Face UVs stay omitted → the vanilla per-element crop, which is the
     * 1.7.10 positional texture mapping.
     */
    private void portalFrame(Block aBlock, String aName, String aFrameTexture, String aPortalTexture, boolean aTranslucentFace) {
        ResourceLocation tFrame = aFrameTexture.startsWith("gt6:") ? modLoc(aFrameTexture.substring(4)) : mcLoc(aFrameTexture);
        // the block/block parent carries ONLY the display transforms — the standalone
        // element model would strip them from the BlockItem GUI/hand rendering (the old
        // cube_all inherited them through the cube_all → cube → block/block chain)
        BlockModelBuilder tInactive = models().getBuilder(aName + "_frame")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("frame", tFrame).texture("particle", tFrame);
        for (int[] tBeam : PORTAL_BEAMS) portalBeam(tInactive, tBeam);
        ResourceLocation tPortal = aPortalTexture.startsWith("gt6:") ? modLoc(aPortalTexture.substring(4)) : mcLoc(aPortalTexture);
        BlockModelBuilder tActive = models().getBuilder(aName + "_portal")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("frame", tFrame).texture("particle", tFrame).texture("portal", tPortal);
        for (int[] tBeam : PORTAL_BEAMS) portalBeam(tActive, tBeam);
        if (aTranslucentFace) tActive.renderType("translucent"); // the vanilla nether-portal layer (ItemBlockRenderTypes.java:271)
        tActive.element().from(1, 1, 1).to(15, 15, 15)
                .allFaces((aDir, aFace) -> aFace.texture("#portal")).end();
        getVariantBuilder(aBlock).partialState()
                .with(gregtech6.block.portals.GTMiniPortalBlock.ACTIVE, false)
                .addModels(new ConfiguredModel(tInactive));
        getVariantBuilder(aBlock).partialState()
                .with(gregtech6.block.portals.GTMiniPortalBlock.ACTIVE, true)
                .addModels(new ConfiguredModel(tActive));
    }

    /**
     * Task bees-lv2 — the bumble hive: ONE blockstate over ONE model, the
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
                .texture("overlay_bottom", modLoc("block/bumblehive_overlay_bottom"))
                // issue #15 (task beehive-tint): the six 0.01 overlay decals are
                // transparent-texel shells — cutout discards them so the tintindex-0
                // family body shows through (the C7' fix shape; SOLID plates them
                // opaque-white over the tint — the review-seat live-verdict root cause).
                .renderType("cutout");
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
     * Task bees-lv3-b-bumbliary — the Bumbliary pair: the addHive two-layer grammar
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
                .texture("overlay_bottom", modLoc("block/" + tBand + "_overlay_bottom"))
                // issue #15 (task beehive-tint): the same six-shell cutout form as
                // addHive above (the C7' fix shape) — the material tint on the
                // tintindex-0 body must survive the overlay shells.
                .renderType("cutout");
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
     * Task w6-rocks-sticks — the surface deco band: FOUR blocks over TWO shared
     * models (the research winner's shared-model+tint deviation — a 1.20.1 static model
     * cannot sample the block below, GTCEu SurfaceRockModelGenerator.java:29-51 same).
     * The rock model is ONE tinted micro box (vanilla stone texture, tintindex 0 — the
     * {@link #tintedCubeAll} grammar; the client BlockColor paints it per material), the
     * stick model a 12x2x2 lying bar over the vanilla oak-log side (MultiTileEntityStick
     * .java:51 Blocks.log SIDE_FRONT 0 borrow, no tint index). Textures are VANILLA
     * borrows — the upstream rocks/sticks copy Blocks.stone/Blocks.log verbatim
     * (MultiTileEntityRock.java:55), so no PNG lands.
     *
     * <p>Task issue1-4 (GitHub #1, the flat-full-pelt fix): the two boxes tightened
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
     *
     * <p>Task stick-shape-random (GitHub #12, the "不能千篇一律" face): every state now
     * carries a WEIGHTED variant list — the vanilla position-seeded random chain
     * (BlockRenderDispatcher.java:53-57 getSeed -> WeightedBakedModel.java:29-33) picks
     * per block position, same-position stable, zero new BlockState properties. The
     * stick band: the two rotationY arms (the upstream :58-68 50/50 X-long/Z-long
     * readFromNBT2 pair) plus slide/tilt tiers; the rock: three size tiers of the
     * upstream 2..8px-wide x 1..4px-high random micro box, the representative form
     * heaviest.
     *
     * <p>Task debt-issue12-shape-follow-tilt (GitHub #12 residual): the tables moved to
     * {@link gregtech6.block.surface.GT6SurfaceVariants} — this loop iterates the SAME
     * enums the block getShape picks from (declaration order == JSON array order ==
     * the WeightedRandom scan order, so selection box == rendered variant), and the
     * stick pool gains the two TILT models (element rotation about the bar's own
     * centre, the only angles the JSON grammar allows — BlockElement.java:100): 8
     * entries = centered arms x2 + one slide tier per arm + the tilt pair on both
     * arms (slide never stacks with tilt; the pool design lives in the Stick javadoc).
     */
    private void addSurfaceBand() {
        // one model per distinct id in the shared tables; the element boxes come from
        // the enums themselves so the model and the shape box cannot drift apart
        Map<String, ModelFile> tModels = new HashMap<>();
        for (GT6SurfaceVariants.Rock tVariant : GT6SurfaceVariants.Rock.values())
            tModels.put(tVariant.model(), microBoxModel(tVariant.model(), mcLoc("block/stone"), true, tVariant.box()));
        for (GT6SurfaceVariants.Stick tVariant : List.of( // the axis-aligned tiers only — the tilt models rotate the default bar
                GT6SurfaceVariants.Stick.CENTERED_X, GT6SurfaceVariants.Stick.SLIDE_X, GT6SurfaceVariants.Stick.SLIDE_Z))
            tModels.put(tVariant.model(), microBoxModel(tVariant.model(), mcLoc("block/oak_log"), false, tVariant.box()));
        tModels.put("surface_stick_t22", tiltedBarModel("surface_stick_t22", GT6SurfaceVariants.Stick.CENTERED_X.box(), 22.5F));
        tModels.put("surface_stick_t45", tiltedBarModel("surface_stick_t45", GT6SurfaceVariants.Stick.CENTERED_X.box(), 45.0F));
        for (var tRow : GT6SurfaceBlocks.ALL) {
            boolean tIsStick = tRow.get() == GT6SurfaceBlocks.SURFACE_STICK.get();
            GT6SurfaceVariants.Variant[] tTable = tIsStick ? GT6SurfaceVariants.Stick.values() : GT6SurfaceVariants.Rock.values();
            getVariantBuilder(tRow.get()).forAllStates(aState -> {
                Direction tFacing = aState.getValue(gregtech6.block.surface.GT6SurfaceRockBlock.FACING);
                int tX = GT6SurfaceVariants.xRotOf(tFacing), tY = GT6SurfaceVariants.yRotOf(tFacing);
                java.util.List<ConfiguredModel> tVariants = new java.util.ArrayList<>();
                for (GT6SurfaceVariants.Variant tVariant : tTable)
                    // (tY + arm) % 360 keeps WEST + 90 at 0 — the omitted JSON default
                    tVariants.add(new ConfiguredModel(tModels.get(tVariant.model()), tX, (tY + tVariant.armY()) % 360, false, tVariant.weight()));
                return tVariants.toArray(new ConfiguredModel[0]);
            });
        }
    }

    /** One tinted-or-plain micro box model: the aX1/aY1/aZ1..aX2/aY2/aZ2 ground box, tintindex 0 on every face when aTinted. */
    private ModelFile microBoxModel(String aName, ResourceLocation aTexture, boolean aTinted, int[] aBox) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("slab", aTexture)
                .texture("particle", "#slab");
        tModel.element()
                .from((float) aBox[0], (float) aBox[1], (float) aBox[2]).to((float) aBox[3], (float) aBox[4], (float) aBox[5])
                .allFaces((aDir, aFace) -> {
                    aFace.texture("#slab");
                    if (aTinted) aFace.tintindex(0);
                })
                .end();
        return tModel;
    }

    /**
     * The lying default bar (aBar = the raw model-space box) TILTED about its own
     * centre by aAngle — the debt-issue12-shape-follow-tilt diagonal stick variants.
     * Element rotation is the ONLY expression the JSON grammar offers and its angle is
     * hard-validated to {-45,-22.5,0,22.5,45} (BlockElement.java:100, the datagen
     * RotationBuilder :731 same check), so 15/30-degree sticks are impossible without
     * baked geometries. The rotation rides the bar's LOCAL Y axis (origin 8,1,8), so
     * the blockstate FACING rotations carry the tilt along naturally.
     */
    private ModelFile tiltedBarModel(String aName, int[] aBar, float aAngle) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("slab", mcLoc("block/oak_log"))
                .texture("particle", "#slab");
        tModel.element()
                .from((float) aBar[0], (float) aBar[1], (float) aBar[2]).to((float) aBar[3], (float) aBar[4], (float) aBar[5])
                .rotation().origin(8.0F, 1.0F, 8.0F).axis(Direction.Axis.Y).angle(aAngle).end()
                .allFaces((aDir, aFace) -> aFace.texture("#slab"))
                .end();
        return tModel;
    }

    /**
     * Task c-anvil — the two stone anvil rows (Loader_MultiTileEntities.java
     * :2185-2186). Task tex-placeholder-audit UPGRADED the target (the probe verdict:
     * upstream ships NO dedicated anvil PNG group — the anvil body renders the row
     * material's SMOOTH SET TEXTURE, {@code mMaterial.getTextureSmooth(mRGBa, T)},
     * MultiTileEntityAnvil.java:306, the material-icon system the mold/crucible body
     * researchcard mapped): ONE cube_all per ROW over the vanilla smooth-stone family
     * texture — stone → {@code minecraft:block/smooth_stone} (the SET_STONE ruling,
     * GT6CrucibleDatagen.bodyTexture), blackstone → {@code minecraft:block/blackstone}
     * (the row crafts from the vanilla BLACKSTONE item, the GT6Anvils doc — the port's
     * faithful material face). The top face is the same art (the single smooth texture
     * serves every body pass upstream), the FACING y-rotation stays (the upstream
     * SIDES_VALID :413 horizontal band — visually inert on the cube, load-bearing when
     * the anvil-silhouette geometry joins the pool). Both rows keep their own model. The
     * 2 BlockItem models parent their row model; the {@code anvil_top/anvil_side}
     * placeholder pair is retired.
     */
    private void addAnvils() {
        for (gregtech6.registry.GT6Anvils.AnvilRow tRow : gregtech6.registry.GT6Anvils.ROWS) {
            ModelFile tModel = models().cubeAll(tRow.path(), anvilBodyTexture(tRow.path()));
            Block tBlock = gregtech6.registry.GT6Anvils.BLOCKS_BY_PATH.get(tRow.path()).get();
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(gregtech6.block.tools.GTAnvilBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tModel).rotationX(0).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    /** The smooth material face of an anvil row (the getTextureSmooth mapping). */
    private net.minecraft.resources.ResourceLocation anvilBodyTexture(String aPath) {
        return mcLoc("stone_anvil".equals(aPath) ? "block/smooth_stone" : "block/blackstone");
    }

    /**
     * Task dryer-family — the four Dryer rows (Loader_MultiTileEntities.java
     * :1477-1480, all four NBT_TEXTURE "dryer"): the addMachine texture-base overload —
     * model names per path (dryer/dryer_t2/... own 16-variant blockstates + item
     * parents) while the FRONT TEXTURES stay on the family "dryer" set (the tier is not
     * a visual state upstream either, the p8 ruling — the ladder adds three PNGs total,
     * the placeholder fronts).
     */
    /**
     * Task crucible-multiblock — the LARGE crucible family (the wall "Steel Wall"
     * + the "Large Steel Crucible" controller): the WALLS over the borrowed metalwall part
     * textures with the material tint (task issue8-residual — the upstream :1145 "Steel
     * Wall" row is NBT_TEXTURE "metalwall" + NBT_MATERIAL Steel, so the dedicated
     * {@code GTCrucibleWallBlock} blocks ride the {@code tintedCube} tinted-body form over
     * the already-borrowed parts/metalwall/0 colored family, the tank_metal borrow; the
     * former large_boiler flat-gray placeholder tinted into a flat plate), while the
     * CONTROLLERS wear the two-layer front-bearing tinted grammar, task
     * tex-multiblockmains — the former {@code large_boiler/wall} borrow placeholder
     * and its "no borrowable crucible face" era are retired (the probe found the full
     * machines/multiblockmains/crucible group): the Base10 default {@code getTexture2}
     * semantics (MultiTileEntityCrucible.java:643-650 rides the same
     * {@code aSide==mFacing} front pair as the Base10 default,
     * TileEntityBase10MultiBlockBase.java:192-194) — the FRONT face the
     * {@code colored_front}+{@code overlay_front} pair, the other five faces the plain
     * pair, the colored body the tintindex-0 seat (the row NBT_MATERIAL through the
     * {@code GTCrucibleControllerBlock} row carrier, the p38-c2 form). The
     * formed/unformed and the molten-content faces are the declared render defer, the
     * controller blockstate still carries the full 8 FACING×FORMED state coverage (now
     * with the addLargeBoiler rotationY table — the front follows FACING).
     */
    private void addLargeCrucible() {
        Block tWall = gregtech6.registry.GT6Crucibles.CRUCIBLE_STEEL_WALL.get();
        simpleBlock(tWall, tintedCube("crucible_steel_wall",
                "block/parts/metalwall/0/colored/bottom", "block/parts/metalwall/0/colored/top", "block/parts/metalwall/0/colored/side"));
        itemModels().withExistingParent("crucible_steel_wall", modLoc("block/crucible_steel_wall"));
        // task w3-distill-crucible ③ — the seven ladder walls (the same metalwall
        // borrow; the composed names ride the metal-wall template, zero new keys)
        for (var tHandle : gregtech6.registry.GT6Crucibles.CRUCIBLE_WALL_BLOCKS_BY_PATH.values()) {
            Block tLadderWall = tHandle.get();
            simpleBlock(tLadderWall, tintedCube(tHandle.getId().getPath(),
                    "block/parts/metalwall/0/colored/bottom", "block/parts/metalwall/0/colored/top", "block/parts/metalwall/0/colored/side"));
            itemModels().withExistingParent(tHandle.getId().getPath(), modLoc("block/" + tHandle.getId().getPath()));
        }
        ModelFile tModel = boilerModel("large_crucible", "crucible", true);
        for (gregtech6.registry.GT6Crucibles.CrucibleRow tRow : gregtech6.registry.GT6Crucibles.CRUCIBLE_ROWS) {
            Block tBlock = gregtech6.registry.GT6Crucibles.CRUCIBLE_BLOCKS_BY_PATH.get(tRow.path()).get();
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
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
     * Task w3-distill-crucible ①② — the twin tower controllers (Loader:1226-1227,
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
            itemModels().withExistingParent(tRow.path(), mcLoc("item/generated")).texture("layer0", modLoc("block/parts/distillationtowerparts/0/colored/side"));
        }
    }

    private void addDryer() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.DRYER_ROWS) {
            addMachine(GTMachines.DRYER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task distillery-family — the four Distillery rows (Loader_MultiTileEntities.java
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
     * Task canner-machine — the four Canner rows (Loader_MultiTileEntities.java
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
     * Task w1-sifter-compressor-wiremill — the W1 Kinetic trio (Sifter/Compressor/
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
     * Task w1-press-extruder-molds — the four Press rows (Loader_MultiTileEntities.java
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
     * Task w1-press-extruder-molds — the four Extruder rows (Loader_MultiTileEntities
     * .java:1406-1409, all four NBT_TEXTURE "extruder"): the addPress shape verbatim (the
     * borrowed upstream basicmachines/extruder fronts).
     */
    private void addExtruder() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.EXTRUDER_ROWS) {
            addMachine(GTMachines.EXTRUDER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task c-ulv-machine-ladder — the six ULV rows (the five family extension rows +
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
     * Task w1-kinetic-roll-ladder — the four roll-ladder RU families (the RollingMill
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
     * Task w1-kinetic-process-ladder — the six process families (Buzzsaw/Squeezer/
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
     * Task w1-eu-hu-families — the seven eu-hu families (the Mixer RU ladder :1392-1395
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
     * Task w2-eu-special — the three eu-special families (the Autocrafter EU 5-tier
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
     * Task w2-exotic-energy — the six exotic-energy families (Polarizer/MagneticSeparator/
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
     * Task massfab — the small Matter Fabricator 5-ladder (Loader_MultiTileEntities
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
     * Task qu-scanner-replicator — the QU machine pair (Loader_MultiTileEntities.java
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
     * Task w2-eu-core-5tier — the five eu-core families (the Electrolyzer/Injector/
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
     * Task w2-hu-tu-piggyback — the seven hu-tu families (the SteamCracker/CatalyticCracker
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
     * Task w3-heat-smelter — the two heat families (the Smelter HU 4-ladder
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
     * Task storage-static-batch — the 28 static storage rows (GT6StaticStorages.ROWS).
     * Task tex-placeholder-audit UPGRADED four of the six kinds (the former "no upstream
     * borrowable iconset in this repo" claim here was proven false — the probe found the
     * dedicated groups): LOCKER over {@code machines/lockers/normal}, DRAWER over
     * {@code machines/drawers/quad}, both safes over {@code machines/safes/{mechanical,
     * keylocked}} — one {@link #storageModel} two-layer faceted cube per kind (the
     * addConverterModel grammar minus the tint seat — the rows are the declared unpaint
     * deviation, GT6StaticStorages ROWS doc, so the colored art displays its own colours;
     * the upstream getTexture2 is the BlockTextureMulti colored×mRGBa + overlay pair,
     * MultiTileEntityLocker.java:92-110 / DrawerQuad:124-142 / SafeMechanical:97-111).
     * The FACING (horizontal) drives the 4-variant y-rotation (north default, south 180,
     * west 270, east 90); the material/plank ladder folds to the kind model. The 28
     * BlockItem models parent their kind model.
     *
     * <p>BOOKSHELF and BOTTLECRATE keep the grayscale front/side placeholder pairs — the
     * probe verdict is TRUE NEGATIVE: upstream renders them from plank/material iconsets
     * plus NBT-driven content boxes (MultiTileEntityBookShelf mShelfIcon = PlankData.
     * PLANK_ICONS, MultiTileEntityBottleCrate :64-66 + the BOTTLECRATE_BOTTLE_* content
     * passes :202-208) — no dedicated colored/overlay group exists to borrow, the visible
     * content is the render pool.</p>
     */
    private void addStaticStorages() {
        for (gregtech6.registry.GT6StaticStorages.Kind tKind : gregtech6.registry.GT6StaticStorages.Kind.values()) {
            ModelFile tModel;
            if (tKind == gregtech6.registry.GT6StaticStorages.Kind.BOOKSHELF
                    || tKind == gregtech6.registry.GT6StaticStorages.Kind.BOTTLECRATE) {
                String tTex = "block/" + kindModelName(tKind) + "_";
                tModel = models().cube("gt6_" + kindModelName(tKind),
                        modLoc(tTex + "side"), modLoc(tTex + "side"),        // bottom/top
                        modLoc(tTex + "front"), modLoc(tTex + "side"),       // north(front)/south
                        modLoc(tTex + "side"), modLoc(tTex + "side"));       // west/east
            } else {
                tModel = storageModel("block/" + kindModelName(tKind),
                        tKind != gregtech6.registry.GT6StaticStorages.Kind.SAFE_MECHANICAL
                                && tKind != gregtech6.registry.GT6StaticStorages.Kind.SAFE_KEYLOCKED);
            }
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

    /**
     * One static-storage two-layer faceted model (task tex-placeholder-audit; the
     * {@link #sensorModel} grammar with an own top/bottom column): the borrowed grayscale
     * {@code <base>/colored_<face>} art on the six body faces (front on north — the FACING
     * face — and back on south, the locker/drawer quad distinct top/bottom or the safes'
     * side art on the vertical pair per the upstream getTexture2 index mapping, SafeMechanical
     * :100 front/back/side trio) plus the six 0.01 {@code <base>/overlay_<face>} decal
     * plates (untinted, cullface synced, cutout — all 16 overlay PNGs carry transparent
     * texels, all 16 colored PNGs fully opaque 16x16). NO tintindex — the unpaint deviation.
     */
    private ModelFile storageModel(String aBase, boolean aDistinctTopBottom) {
        // the "block/" prefix rides INSIDE the builder path: getBuilder only prepends the
        // folder to slash-free paths (Forge ModelProvider.extendWithFolder) — same lesson
        // as sensorModel.
        BlockModelBuilder tModel = models().getBuilder(aBase)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc(aBase + (aDistinctTopBottom ? "/colored_bottom" : "/colored_side")))
                .texture("up", modLoc(aBase + (aDistinctTopBottom ? "/colored_top" : "/colored_side")))
                .texture("north", modLoc(aBase + "/colored_front"))
                .texture("south", modLoc(aBase + "/colored_back"))
                .texture("west", modLoc(aBase + "/colored_side"))
                .texture("east", modLoc(aBase + "/colored_side"))
                .texture("particle", modLoc(aBase + "/colored_side"))
                .texture("overlay_down", modLoc(aBase + (aDistinctTopBottom ? "/overlay_bottom" : "/overlay_side")))
                .texture("overlay_up", modLoc(aBase + (aDistinctTopBottom ? "/overlay_top" : "/overlay_side")))
                .texture("overlay_north", modLoc(aBase + "/overlay_front"))
                .texture("overlay_south", modLoc(aBase + "/overlay_back"))
                .texture("overlay_west", modLoc(aBase + "/overlay_side"))
                .texture("overlay_east", modLoc(aBase + "/overlay_side"))
                .renderType("cutout");
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).cullface(aDir))
                .end();
        tModel.element() // north
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_north").cullface(Direction.NORTH)
                .end();
        tModel.element() // south
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_south").cullface(Direction.SOUTH)
                .end();
        tModel.element() // west
                .from(-0.01F, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                .face(Direction.WEST).texture("#overlay_west").cullface(Direction.WEST)
                .end();
        tModel.element() // east
                .from(16.0F, 0.0F, 0.0F).to(16.01F, 16.0F, 16.0F)
                .face(Direction.EAST).texture("#overlay_east").cullface(Direction.EAST)
                .end();
        tModel.element() // bottom
                .from(0.0F, -0.01F, 0.0F).to(16.0F, 0.0F, 16.0F)
                .face(Direction.DOWN).texture("#overlay_down").cullface(Direction.DOWN)
                .end();
        tModel.element() // top
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_up").cullface(Direction.UP)
                .end();
        return tModel;
    }

    /**
     * The texture/model band of a storage kind (the borrowed group directory, or the
     * placeholder PNG stem on the two true-negative wooden kinds).
     */
    private static String kindModelName(gregtech6.registry.GT6StaticStorages.Kind aKind) {
        return switch (aKind) {
            case LOCKER -> "locker";
            case DRAWER -> "drawer";
            case SAFE_MECHANICAL -> "safe_mechanical";
            case SAFE_KEYLOCKED -> "safe_keylocked";
            case BOOKSHELF -> "bookshelf";
            case BOTTLECRATE -> "bottlecrate";
        };
    }

    /**
     * Task w3-heat-smelter — the Large Heat Exchanger controller (Loader
     * :1245): the two-layer front-bearing tinted grammar over the borrowed upstream
     * multiblockmains/largeheatexchanger group, task tex-multiblockmains — the
     * former borrow-time-composite single-image cube and its "all faces composite to
     * the same pixels" claim are retired (the group's colored/colored_front split is
     * real art): the Base10 default {@code getTexture2} semantics
     * (TileEntityBase10MultiBlockBase.java:192-194), the boiler form — the FRONT face
     * the {@code colored_front}+{@code overlay_front} pair, the other five faces the
     * plain pair, the colored body the tintindex-0 seat (the :1245 NBT_MATERIAL ANY.W
     * row through the {@code HeatExchangerBlock.materialOf} carrier, the
     * GT6DynamoBlock dispatch shape). Every FORMED state maps to the same model (the
     * formed-look visual is the p9 pool). No facing rotation (the structure is
     * facing-independent — the controller is the centre cell of both layers; the front
     * pair lands on north, the default-facing view pin). The BlockItem is the 2D icon
     * over the composite sprite (tex-itemform-b).
     */
    private void addHeatExchanger() {
        ModelFile tMain = boilerModel("large_heat_exchanger", "large_heat_exchanger", true);
        Block tController = gregtech6.registry.GT6HeatExchangers.HEAT_EXCHANGER_BLOCK.get();
        getVariantBuilder(tController).forAllStates(aState -> ConfiguredModel.builder().modelFile(tMain).build());
        itemModels().withExistingParent("large_heat_exchanger", mcLoc("item/generated")).texture("layer0", modLoc("block/large_heat_exchanger/main"));
    }

    /**
     * Task debt-reactor-b-2x2-be — the 2x2 Nuclear Reactor Core (Loader :738): a plain
     * oriented cube over the six borrowed upstream faces (reactor_core_2x2_<face>.png,
     * the byte-level borrows of the assets README). DECLARED static assignment: the
     * upstream texture routing is facing-dependent (the mFacing face shows face1, the
     * mSecondFacing face face2, Core2x2:392-395) with the Pb material tint and the
     * 11-pass rod/fluid render stack (:320-390) — all of that rides the render-pool
     * card, so the port pins the default-facing view (down = face1 — the default SIDE_BOTTOM
     * facing pair routes the hot-port face onto the bottom, Core2x2:394) and the
     * side1/side2 alternation.
     */
    private void addReactorCore() {
        net.minecraft.world.level.block.Block tCore = gregtech6.registry.GT6Reactors.REACTOR_CORE_2X2_BLOCK.get();
        ModelFile tModel = models().cube("nuclear_reactor_core_2x2",
                modLoc("block/reactor_core_2x2_face1"), modLoc("block/reactor_core_2x2_top"),
                modLoc("block/reactor_core_2x2_side1"), modLoc("block/reactor_core_2x2_side2"),
                modLoc("block/reactor_core_2x2_side1"), modLoc("block/reactor_core_2x2_side2"));
        getVariantBuilder(tCore).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
        itemModels().withExistingParent("nuclear_reactor_core_2x2", tModel.getLocation());
    }

    /**
     * Task act-machine — the Advanced Crafting Table (Loader_MultiTileEntities.java
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
     * Task multiblock-framework (W3 provider order 1: multiblock→barrel→cover): the FORMED
     * blockstate fallback rendering (spec ⑧) — the Coke Oven controller carries the base-owned
     * FACING + FORMED properties (TileEntityBase10MultiBlockBase :188-189 bit-3 replacement),
     * 4 facings x 2 formed = 8 variants over two cube models.
     *
     * <p>Erratum (task render-d-formed-look; the earlier "mirrors the upstream getTexture2
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
     * ADR 2026-09-01-render-d-formed-look).
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
        // task w3-nbtdesign-parts — the firebricks retexture: multiblock_coke_oven_bricks
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
     * Task fluid-barrel (W3 provider addition, merge order multiblock→barrel→cover),
     * extended by task barrel-metal-plastic, re-textured by task tex-tank-family:
     * the barrel family over the BORROWED two-layer upstream art — the per-face
     * {@code barrel_parts/<family>/{colored,overlay}_{bottom,top,side}.png} borrows
     * (upstream {@code machines/tanks/<family>/}, BarrelWood.java:44-55 colored+overlay
     * x {bottom,top,side}; Plastic:42-48 / Metal:39-45 isomorphic) in the
     * {@link #barrelPartsModel} two-layer per-face grammar. The former
     * {@code barrel_<material>.png} single-texture shortcut (p20) is retired with its
     * three PNGs; the {@code barrel_logistics.png} row joined the same borrow with task
     * tank-render-tint (the declared p12/tex-tank-family follow-up — the 3+3
     * {@code machines/tanks/logistics/} group, the old single PNG retired, the TESR/lid
     * omission stays declared, MultiTileEntityBarrelWood.java:44-54).
     *
     * <p>Task barrel-high-tier-melt-bridge spec ④: the twelve high-tier metal drums
     * (Loader_MultiTileEntities.java:2159-2170) share the ONE {@code drum} family —
     * every model JSON references the same barrel_parts textures, so the model count
     * grows with the rows and the PNG count does not (upstream registers the high tiers
     * over the same drum icon set).
     *
     * <p>Task barrel-paint-render: the tint seat stays the body element's
     * {@code tintindex 0} — the upstream barrel renders its {@code colored/} texture
     * multiplied by mRGBa (MultiTileEntityBarrelWood.java:42-55
     * {@code new BlockTextureDefault(tTex, mRGBa)}), the {@code overlay/} decal shell
     * renders UNCOLOURED on top (BlockTextureMulti) and carries no tintindex. Unpainted
     * barrels ride the {@code -1} white-multiply identity sentinel exactly like the
     * machine face (P21: 0xFFFFFFFF ≡ vanilla no-tint).
     */
    private void addBarrel() {
        addBarrel(GTBarrels.BARREL.get(), "barrel");
        addBarrel(GTBarrels.BARREL_PLASTIC.get(), "plasticcan");
        addBarrel(GTBarrels.BARREL_METAL.get(), "drum");
        addBarrel(GTBarrels.BARREL_LOGISTICS.get(), "logistics"); // task tank-render-tint — the :2171 row joins the two-layer borrow
        for (var tDrum : GTBarrels.METAL_DRUM_BLOCKS.values())
            addBarrel(tDrum.get(), "drum");
        // task barrel-paint-render: the datagen-JVM census half — 16 barrel blocks,
        // matching the GTBarrels.paintableBlockArray() client registration census (the
        // offline JUnit half walks the generated tree and pins the same 16).
        LOGGER.info("GT6 barrel paint tint: {} barrel models tinted (4 rows + 12 high-tier drums, addBarrel)", mBarrelTintModels);
    }

    /** The r8 two-layer per-face barrel + its BlockItem parent (the borrowed barrel_parts family form). */
    private void addBarrel(Block aBarrel, String aFamily) {
        String tName = aBarrel.getDescriptionId().replace("block.gt6.", "");
        simpleBlock(aBarrel, barrelPartsModel(tName, aFamily));
        itemModels().withExistingParent(tName, modLoc("block/" + tName));
        mBarrelTintModels++;
    }

    /**
     * The two-layer per-face barrel model (task tex-tank-family, the addConverterModel
     * grammar without the front band): element 0 = the tinted body cube over the borrowed
     * grayscale {@code barrel_parts/<family>/colored_{bottom,top,side}} art (tintindex 0
     * = the paint/mRGBa seat, bottom/top/side each on their own face key), elements 1-6 =
     * the six 0.01-plate {@code overlay_{bottom,top,side}} decal shells (untinted,
     * cullface synced) — the upstream BlockTextureMulti(colored x mRGBa, overlay) stack
     * per face, MultiTileEntityBarrelWood.java:44-55. Cutout so the shells' transparent
     * texels discard (the C7' fix shape).
     */
    private ModelFile barrelPartsModel(String aName, String aFamily) {
        String tBand = "block/barrel_parts/" + aFamily + "/";
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc(tBand + "colored_bottom")).texture("up", modLoc(tBand + "colored_top"))
                .texture("north", modLoc(tBand + "colored_side")).texture("south", modLoc(tBand + "colored_side"))
                .texture("west", modLoc(tBand + "colored_side")).texture("east", modLoc(tBand + "colored_side"))
                .texture("particle", modLoc(tBand + "colored_side"))
                .texture("overlay_bottom", modLoc(tBand + "overlay_bottom"))
                .texture("overlay_top", modLoc(tBand + "overlay_top"))
                .texture("overlay_side", modLoc(tBand + "overlay_side"))
                .renderType("cutout");
        tintedBody(tModel);
        overlayShell(tModel, "overlay_side", "overlay_side", "overlay_side", "overlay_top", "overlay_bottom");
        return tModel;
    }

    /** The tinted body cube element (the tintedCube element 0 form, shared by the r8 two-layer builders). */
    private void tintedBody(BlockModelBuilder aModel) {
        aModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
    }

    /**
     * The six 0.01-plate decal shells (the addConverterModel elements 1-6 grammar): north
     * = {@code aFront}, south = {@code aBack}, west/east = {@code aSide}, up = {@code aTop},
     * down = {@code aBottom} — the texture KEYS, untinted and cullface-synced.
     */
    private void overlayShell(BlockModelBuilder aModel, String aFront, String aBack, String aSide, String aTop, String aBottom) {
        aModel.element() // north
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#" + aFront).cullface(Direction.NORTH)
                .end();
        aModel.element() // south
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#" + aBack).cullface(Direction.SOUTH)
                .end();
        aModel.element() // west
                .from(-0.01F, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                .face(Direction.WEST).texture("#" + aSide).cullface(Direction.WEST)
                .end();
        aModel.element() // east
                .from(16.0F, 0.0F, 0.0F).to(16.01F, 16.0F, 16.0F)
                .face(Direction.EAST).texture("#" + aSide).cullface(Direction.EAST)
                .end();
        aModel.element() // bottom
                .from(0.0F, -0.01F, 0.0F).to(16.0F, 0.0F, 16.0F)
                .face(Direction.DOWN).texture("#" + aBottom).cullface(Direction.DOWN)
                .end();
        aModel.element() // top
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#" + aTop).cullface(Direction.UP)
                .end();
    }

    /**
     * Task machine-oven (W2-exclusive provider addition), generalized by task
     * basicmachine-family into {@link #addMachine(Block, String)}: the A-tier machine
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
        // task oven-heat-t-ladder — the Heat_T ladder rows through the p8 texture-base
        // overload: the tier models derive from aBase (oven_t2/_active/_running) while the
        // FRONT TEXTURES stay on the family T1 "oven" set (upstream NBT_TEXTURE "oven" on
        // all four rows :1288-1291) — the ladder adds zero PNGs.
        addMachine(GTMachines.OVEN_T2.get(), "oven_t2", "oven");
        addMachine(GTMachines.OVEN_T3.get(), "oven_t3", "oven");
        addMachine(GTMachines.OVEN_T4.get(), "oven_t4", "oven");
    }

    /**
     * The addOven generalization (task basicmachine-family ④): one {@code aBase} machine
     * = three models ({@code aBase}, {@code aBase_active}, {@code aBase_running}), the
     * 16-variant blockstate, and the BlockItem model parenting the block model. Since task
     * b-port-overlay-render the three models come from {@link #familyMachineModel} —
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
     * Task w4-eu-bridge — the Roasting Oven 4-ladder (Loader_MultiTileEntities.java
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
     * Task machines-bumblelyzer-crucible — the two machine families (the Bumblelyzer
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
     * Task machines-burner-plantalyzer — the Burner Mixer 4-ladder (20521-20524,
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
     * Task w4-eu-bridge — the three EU-bridge converter families (Loader
     * :815-821/:831-837/:847-853, 15 blocks). NOT paintable machines — the reused
     * {@link gregtech6.block.energy.GT6DynamoBlock} carrier rides the converter
     * two-layer form instead of the addMachine paint walk: one {@link #addConverterModel}
     * shell pair per family over the borrowed upstream colored + overlay groups
     * (heaters/heat_electric, engines/kinetic_electric, motors/rotation_electric — the
     * assets/README.md faces), FRONT = the emission face (the dynamo-block geometry),
     * the six-way FACING x band + ACTIVE variant the converterBlockstate form. Only the
     * heater family carries upstream overlay_active art (the probe: kinetic_electric and
     * rotation_electric ship NO active groups — both ACTIVE values ride the inactive
     * shell there). The five rungs of a family share the models (the tier is not a
     * visual state upstream, the p8 texture ruling).
     */
    private void addElectricBridges() {
        addBridgeFamily(GTMachines.ELECTRIC_HEATER_BLOCKS_BY_PATH, "electric_heater", "bridge_heater", true);
        addBridgeFamily(GTMachines.ELECTRIC_ENGINE_BLOCKS_BY_PATH, "electric_engine", "bridge_engine", false);
        addBridgeFamily(GTMachines.ELECTRIC_MOTOR_BLOCKS_BY_PATH, "electric_motor", "bridge_motor", false);
    }

    /**
     * Task qu-laser-domain — the CO2 Laser + Laser Absorber families (Loader
     * :930-934/:976-980, 10 blocks): the SAME reused-dynamo-carrier converter form as the
     * bridges, over the borrowed upstream colored + overlay groups (lasers/laser_electric,
     * laserabsorbers/electric_laser — the assets/README.md faces), both with upstream
     * overlay_active art on the ACTIVE channel (the GT6DynamoBlock property, the BE
     * syncActiveToState arm). FRONT = the emission face (the laser pushes LU out the
     * front; the absorber takes the beam on the BACK and pushes EU out the front — the
     * face geometry rides the BE, the visual is this shared orientable). The beam itself
     * is NOT rendered (the task card: 光束 defer, visual = the static block face).
     */
    private void addLaserFamilies() {
        addBridgeFamily(GT6Lasers.CO2_LASER_BLOCKS_BY_PATH, "co2_laser", "laser_electric", true);
        addBridgeFamily(GT6Lasers.LASER_ABSORBER_BLOCKS_BY_PATH, "laser_absorber", "laser_absorber", true);
        // task qu-energizer — the third laser-converter family keeps the plain
        // single-layer orientable (the tex-bridge-kinetic probe: the upstream
        // quantum_laser overlay groups are alpha-0 EMPTY layers and the colored layer is
        // mRGBa-dependent gray noise — nothing to borrow, the committed derived amber
        // faces stay the rung identity, the assets/README.md probe verdict)
        addBridgeFamily(gregtech6.registry.GT6QuantumEnergizers.QUANTUM_ENERGIZER_BLOCKS_BY_PATH, "quantum_energizer", "quantum_energizer");
    }

    /**
     * One bridge family: the orientable cube + the five rung blockstates + the item parents
     * (the transformer form). Task c2-controller-tint: the re-declared body element
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
                modLoc("block/" + aTexture + "_side"), modLoc("block/" + aTexture + "_front"), modLoc("block/" + aTexture + "_side"))
                // issue #8 (task world-tint-render-type): uniform cutout over the paintable
                // family (the census convention — an opaque-texture body renders identically on
                // cutout, and the family stays shell-safe if decals join later)
                .renderType("cutout");
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture(aDir == Direction.NORTH ? "#front" : "#side").tintindex(0).cullface(aDir))
                .end();
        for (RegistryObject<Block> tHandle : aBlocks.values()) {
            getVariantBuilder(tHandle.get()).forAllStates(aState -> {
                // the vanilla horizontal-facing rotation map + the six-way x band (issue
                // #18: the shared GT6DynamoBlock carrier went six-way + ACTIVE — the
                // single-layer families carry no decal shell, so both ACTIVE values ride
                // this model)
                Direction tFacing = aState.getValue(gregtech6.block.energy.GT6DynamoBlock.FACING);
                int tX = tFacing == Direction.DOWN ? 90 : tFacing == Direction.UP ? 270 : 0;
                return ConfiguredModel.builder()
                        .modelFile(tModel)
                        .rotationX(tX)
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
     * The two-layer bridge family (task tex-bridge-kinetic — the census B2 ruling): the
     * {@link #addConverterModel} grammar the #18 converter band already speaks, over the
     * borrowed upstream {@code <band>_colored_*} + {@code <band>_overlay_*} groups — the
     * tintindex-0 body is the mRGBa seat, the six 0.01 decal shells ride untinted (the
     * upstream {@code BlockTextureMulti(colored x mRGBa, overlay)} stack, MultiTileEntity
     * HeaterElectric :56-59 and siblings). {@code aActive} borrows the upstream
     * {@code overlay_active} trio onto the {@link gregtech6.block.GTBlockProperties#ACTIVE}
     * channel (the GT6DynamoBlock property, the GT6DynamoBlockEntity syncActiveToState arm
     * — upstream {@code mActivity.mState} keys the same art, TransformerElectric :35-39);
     * families without the art pass false and both ACTIVE values ride the inactive shell
     * (the probe: kinetic_electric / rotation_electric ship no active groups).
     */
    private void addBridgeFamily(java.util.Map<String, RegistryObject<Block>> aBlocks, String aFamily, String aTexture, boolean aActive) {
        ModelFile tModel = addConverterModel(aTexture);
        ModelFile tActive = aActive ? addConverterActiveModel(tModel) : null;
        for (RegistryObject<Block> tHandle : aBlocks.values()) {
            converterBlockstate(tHandle.get(), tModel, gregtech6.block.energy.GT6DynamoBlock.FACING, tActive);
        }
        itemModels().withExistingParent(aFamily, modLoc("block/" + aTexture));
        for (String tPath : aBlocks.keySet()) {
            if (!tPath.equals(aFamily)) itemModels().withExistingParent(tPath, modLoc("item/" + aFamily));
        }
    }

    /**
     * Task magic-absorber — the Magic Field Absorber single (Loader :1005, id 10180):
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
                .texture("east", modLoc("block/magic_absorber_base"))
                // issue #8 (task world-tint-render-type): uniform cutout over the
                // paintable family (the census convention)
                .renderType("cutout");
        // the re-declared body element (tintindex 0 = the material tint, task
        // c2-controller-tint — the child's elements replace the cube_directional
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
     * The texture-base overload (task machine-tiers-doinject ⑧): the model names derive
     * from {@code aBase} (so shredder_t2 gets shredder_t2/_active/_running models + its own
     * 16-variant blockstate + the item parent) while the TEXTURES stay on the family's
     * T1 set ({@code aTextureBase_colored_*} + the {@code aTextureBase_overlay_*} state
     * trio, task paint-front-overlay-split extended to all six faces by task
     * b-port-overlay-render) — the tier is not a visual state upstream (the rows
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
     * The p22 two-element form. ACT-only since task b-port-overlay-render (the
     * Advanced Crafting Table is the one machine family with no borrowed side art — the
     * craftingtables/advanced upstream group ships fronts only — so it keeps the shared
     * oven placeholder body and the single front decal; every addMachine family moved to
     * {@link #familyMachineModel}).
     *
     * <p>Task paintable-tint-render: the vanilla {@code block/cube} element is re-declared
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
     * <p>Task paint-front-overlay-split: the former single BAKED front composite
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
                .texture("overlay", modLoc("block/" + aOverlayTexture))
                // issue #8 (task world-tint-render-type): the 0.01 front decal is a
                // transparent-texel overlay shell — the default SOLID chunk layer has no
                // alpha discard, so the shell's transparent texels paint their RGB residue
                // as an opaque plate over the tintindex-0 body. cutout discards them (the
                // tree-sapling precedent :2732; the D2 leg6 live verification).
                .renderType("cutout");
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
     * Task b-port-overlay-render — the full-family machine model over the borrowed
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
                .texture("overlay_bottom", modLoc("block/" + aTextureBase + "_overlay_bottom" + aStateSuffix))
                // issue #8 (task world-tint-render-type): the six 0.01 state decals are
                // transparent-texel overlay shells — cutout discards their transparent
                // texels so the tintindex-0 body shows through (the D2 leg6 fix shape).
                .renderType("cutout");
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
     * Task tex-pipe-textures — one shared two-layer tinted cube model per pipe texture
     * set: the {@link #tintedCubeAll} body (every face tintindex 0 over {@code #all}, the
     * mRGBa seat — the wire-family grammar) plus one 0.01 six-face decal band per overlay
     * texture in {@code aOverlays}, each face cullface-synced with the body and carrying
     * NO tintindex (the UNCOLOURED second layer, BlockTextureDefault.java:179-180 —
     * FaceBuilder's default -1 omits the key, so the paint chain can never tint the
     * outlines). The shell re-declares {@code cutout}: the overlay art is a
     * transparent-texel plate and the default SOLID chunk layer has no alpha discard
     * (issue #8, the world-tint-render-type root-cause pair). No explicit UVs — they
     * default to the element bounds. {@code aName} is the full {@code block/...} model
     * path; the restrictive twin stacks the PIPE_RESTRICTOR band as the second overlay
     * (the upstream third render pass, MultiTileEntityPipeItem.java:280).
     */
    private ModelFile tintedPipeModel(String aName, ResourceLocation aBase, ResourceLocation... aOverlays) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("all", aBase)
                .texture("particle", "#all")
                .renderType("cutout");
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#all").tintindex(0).cullface(aDir))
                .end();
        for (int i = 0; i < aOverlays.length; i++) {
            String tOverlayKey = "overlay" + i;
            tModel.texture(tOverlayKey, aOverlays[i]);
            float tOff = 0.01F * (i + 1); // the second band floats one more step out — no coplanar z-fight with the first
            tModel.element() // north
                    .from(0.0F, 0.0F, -tOff).to(16.0F, 16.0F, 0.0F)
                    .face(Direction.NORTH).texture("#" + tOverlayKey).cullface(Direction.NORTH)
                    .end();
            tModel.element() // south
                    .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.0F + tOff)
                    .face(Direction.SOUTH).texture("#" + tOverlayKey).cullface(Direction.SOUTH)
                    .end();
            tModel.element() // west
                    .from(-tOff, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                    .face(Direction.WEST).texture("#" + tOverlayKey).cullface(Direction.WEST)
                    .end();
            tModel.element() // east
                    .from(16.0F, 0.0F, 0.0F).to(16.0F + tOff, 16.0F, 16.0F)
                    .face(Direction.EAST).texture("#" + tOverlayKey).cullface(Direction.EAST)
                    .end();
            tModel.element() // bottom
                    .from(0.0F, -tOff, 0.0F).to(16.0F, 0.0F, 16.0F)
                    .face(Direction.DOWN).texture("#" + tOverlayKey).cullface(Direction.DOWN)
                    .end();
            tModel.element() // top
                    .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.0F + tOff, 16.0F)
                    .face(Direction.UP).texture("#" + tOverlayKey).cullface(Direction.UP)
                    .end();
        }
        return tModel;
    }

    /**
     * Task tex-pipe-textures — the pipe/connector blockstate form, UNCHANGED from the
     * W1/p26/p32 placeholder era (the three former addFluidPipe/addItemPipe/
     * addLogisticsWire one-model builders collapsed onto it): ONE blockstate JSON per
     * block whose variant per {@link gregtech6.block.GTBlockProperties#CONNECTIONS} mask
     * value (0..63, out of forAllStates — the 64-variant exhaustive listing stays dead,
     * the ADR red line) shares the per-set two-layer model, plus the BlockItem model
     * parenting the block model. What the task changed is the model TARGET (the shared
     * {@link #tintedPipeModel} bands) — the per-connection geometry (core + arms) is the
     * L-level render-pool card.
     */
    private void pipeBlockstate(Block aPipe, ModelFile aModel) {
        String tName = aPipe.getDescriptionId().replace("block.gt6.", "");
        getVariantBuilder(aPipe).forAllStates(aState -> ConfiguredModel.builder().modelFile(aModel).build());
        itemModels().withExistingParent(tName, aModel.getLocation());
    }

    /**
     * Task d2-cable spec ⑥ — the electric wires, the pipe section shape: one shared
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
     * Task wire-family-w1 spec ⑥, MODEL TARGET UPGRADED by task wire-family-w2
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
     * Task wire-redstone-family — the redstone-wire family (6 blocks over
     * {@link GTWireSpecs#REDSTONE_ROWS}), the addWireFamily pipe over the redstone variant
     * list. The three materials (RedAlloy/Signalum/Lumium) all resolve to the
     * {@code copper} texture set (clloy/clloymachine construct with SET_COPPER —
     * MT.java:697/701), which the W2 borrow already shipped, so zero new PNGs. The same
     * single property-less variant wildcard maps the 64 CONNECTIONS states per block onto
     * the shared tinted model.
     *
     * <p>Task wire-brightness UPGRADED the runtime target (the fiber-card shape): the
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
     * Task wire-laser-placeholder — the laser-wire family (1 block over
     * {@link GTWireSpecs#LASER_ROWS}), the addWireFamily pipe over the laser variant list.
     * The row material is MT.NULL (upstream NBT_MATERIAL MT.NULL, Loader:1815), which
     * resolves to the {@code none} set (GTWireTextures.blockSetOf empty-list rule) — the
     * W2 borrow already shipped that set (the Superconductor row), so zero new PNGs.
     *
     * <p>Task wire-fiber-texture UPGRADED the runtime target: the listener table
     * ({@link gregtech6.client.wire.GTWireClientListener#buildParams}) now covers
     * {@code wire_laser}, so every per-state key AND the item key bake into the
     * {@link gregtech6.client.wire.GTWireBakedModel} fiber form (the fixed FIBER_WIRE
     * + FIBER_WIRE_OVERLAY pair, MultiTileEntityWireLaser :121-122) — the block no longer
     * RENDERS through this JSON. The shared tinted cube stays generated UNCHANGED as the
     * per-state key carrier + item parent (the addWireFamily architecture, spec-pinned:
     * the blockstate's single wildcard variant is what mints the 64 per-state
     * {@code gt6:wire_laser#connections=N} keys the listener swaps), and as the baked
     * fallback safety net if the listener never fires. This is the exact shape the 620
     * electric rows and — since task wire-brightness — the 6 redstone rows have;
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
     * Task d4-energy-source spec ② — the test energy source: one cube_all over
     * {@code gt6:textures/block/energy_source.png} (the borrowed upstream
     * solarpanel_electric_8eu side texture, the gui-family byte-identical form) plus
     * the 2D BlockItem icon over the same sprite (tex-itemform-a — the former
     * parent-the-block-model form tiled the sprite over six inventory faces). No
     * properties, a single variant.
     */
    private void addEnergySource() {
        Block tSource = GTEnergySources.ENERGY_SOURCE.get();
        simpleBlock(tSource, models().cubeAll("energy_source", modLoc("block/energy_source")));
        itemModels().withExistingParent("energy_source", mcLoc("item/generated")).texture("layer0", modLoc("block/energy_source"));
    }

    /**
     * Task eu-bridge-outbound (TAIL-APPENDED row, the shared serial file) — the FE
     * battery fixture: the addEnergySource shape verbatim, one cube_all over the SHARED
     * placeholder {@code gt6:textures/block/energy_source.png} (no new PNG — the borrow
     * posture the p20 testmachine rows pinned), plus the 2D BlockItem icon over the same
     * sprite (tex-itemform-a). No properties, a single variant.
     */
    private void addFeBattery() {
        Block tBattery = GT6FeBatteries.FE_BATTERY.get();
        simpleBlock(tBattery, models().cubeAll("fe_battery", modLoc("block/energy_source")));
        itemModels().withExistingParent("fe_battery", mcLoc("item/generated")).texture("layer0", modLoc("block/energy_source"));
    }

    /**
     * Task b-fe-converter-machine (TAIL-APPENDED row, the shared serial file) — the
     * ULV FE→EU converter: the addFeBattery shape verbatim, one cube_all over the SHARED
     * placeholder {@code gt6:textures/block/energy_source.png} (no new PNG — the borrow
     * posture), plus the 2D BlockItem icon over the same sprite (tex-itemform-a). No
     * properties, a single variant.
     */
    private void addFeConverter() {
        Block tConverter = GT6FeConverters.FE_CONVERTER.get();
        simpleBlock(tConverter, models().cubeAll("fe_converter", modLoc("block/energy_source")));
        itemModels().withExistingParent("fe_converter", mcLoc("item/generated")).texture("layer0", modLoc("block/energy_source"));
    }

    /**
     * Task b-fe-converter-machine (TAIL-APPENDED row, the shared serial file) — the
     * FE source fixture: the addFeBattery shape verbatim, one cube_all over the SHARED
     * placeholder, plus the 2D BlockItem icon over the same sprite (tex-itemform-a).
     */
    private void addFeSource() {
        Block tSource = GT6FeBatteries.FE_SOURCE.get();
        simpleBlock(tSource, models().cubeAll("fe_source", modLoc("block/energy_source")));
        itemModels().withExistingParent("fe_source", mcLoc("item/generated")).texture("layer0", modLoc("block/energy_source"));
    }

    /**
     * Task testmachine-blockstates — the two dev BE-framework blocks (task be-framework,
     * {@link GTBlockEntities#TEST_MACHINE} / {@link GTBlockEntities#TEST_MACHINE_IDLE}): the last
     * model-less blocks in the registry face (P20 texture census, research card
     * tasks.p20-research-texture-census — every other registered blockstate+item model was
     * already zero-gap). The addEnergySource shape verbatim: one cube_all per block over the
     * SHARED placeholder {@code gt6:textures/block/example_chest.png} (no new PNG per the card
     * scope — the placeholder-to-upstream art swap stays a P20 wave item; the census pin d
     * resolves the layer0 against the static tree), plus the 2D item icon over the same
     * sprite (tex-itemform-a). Datagen-only JSON: the dev blocks register no BlockItem
     * (the census "dev blocks have no item" note), the item model row merely closes the
     * model-resolution loop the way TestMachineBlock.java:21-22 expected when it deferred
     * this datagen to the example machine card. No properties, a single variant each —
     * the ticking/idle split is the BE ticker (TestMachineBlock.java:44-60), not a
     * blockstate.
     */
    private void addTestMachines() {
        simpleBlock(GTBlockEntities.TEST_MACHINE.get(),
                models().cubeAll("test_machine", modLoc("block/example_chest")));
        itemModels().withExistingParent("test_machine", mcLoc("item/generated")).texture("layer0", modLoc("block/example_chest"));
        simpleBlock(GTBlockEntities.TEST_MACHINE_IDLE.get(),
                models().cubeAll("test_machine_idle", modLoc("block/example_chest")));
        itemModels().withExistingParent("test_machine_idle", mcLoc("item/generated")).texture("layer0", modLoc("block/example_chest"));
    }

    /**
     * Task engine-crank — the Hand Crank: one cube_all over
     * {@code gt6:textures/block/crank.png} (the borrowed upstream crank front icon, the
     * energy_source byte-identical-borrow form; assets/README.md attribution), single
     * model for every state — the FACING property drives the EMIT side, not the visuals
     * (the idle/spin dual texture of upstream getRenderPasses2 :125-130 is the render
     * pool item). The empty-partial variant key applies the model to all four facings.
     */
    private void addCrank() {
        Block tCrank = GT6Kinetics.CRANK.get();
        simpleBlock(tCrank, models().cubeAll("crank", modLoc("block/crank")));
        itemModels().withExistingParent("crank", mcLoc("item/generated")).texture("layer0", modLoc("block/crank"));
    }

    /**
     * Task axle-family — the 44 axle blocks (11 materials x 4 diameters): ONE shared
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
     * Task tap-funnel-attachment spec ⑤ — the 12 wall attachments: one cube_all
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
            itemModels().withExistingParent(tRow.path(), mcLoc("item/generated")).texture("layer0", modLoc("block/" + tTexture));
        }
    }

    /**
     * Task engine-steam — the Steam Engine family (all 28 rows of
     * {@link GT6Kinetics#STEAM_ENGINES}, the no-upstream-subset ruling): ONE shared
     * {@link #addConverterModel} two-layer shell for the whole family — front (the KU
     * emit face) / back (the steam face) / side over the borrowed upstream
     * {@code machines/engines/kinetic_steam/} colored + overlay groups (task
     * tex-bridge-kinetic; the overlay trio is the :285-294 sOverlays stack the p12
     * borrow left unborrowed — the census B3 ruling supersedes the old note). Rotated per
     * {@code FACING} exactly like the machine ladder ({@code addMachine}) with NO
     * active split — upstream keys the visuals on synced mState/mActive byte data
     * (getTexture2 :263-273, the seven-pass bespoke renderer), which is the render-pool
     * item; the port machine blocks carry no active-state blockstate property. The 26
     * variants are visually identical up to the y rotation; the grayscale colored body
     * is the tintindex-0 seat and the row's loader NBT_MATERIAL (Loader :584-612) is the
     * tint — the GTMachinePaintTint steam-engine arm resolves it off
     * {@code SteamEngineRow.matSlug}. The heat gauge ({@code sEngineColors[mState]},
     * :56/:269) and the engine core sprites stay the render pool (the declared debt).
     */
    private void addSteamEngines() {
        ModelFile tModel = addConverterModel("steam_engine");
        for (GT6Kinetics.SteamEngineRow tRow : GT6Kinetics.STEAM_ENGINES) {
            Block tBlock = GT6Kinetics.STEAM_ENGINE_BLOCKS.get(tRow.path()).get();
            converterBlockstate(tBlock, tModel, GT6Kinetics.SteamEngineBlock.FACING, null);
            itemModels().withExistingParent(tRow.path(), modLoc("block/steam_engine"));
        }
    }

    /**
     * Task engine-diesel — the 8 diesel engine tiers (Loader_MultiTileEntities.java
     * :721-729): ONE shared {@link #addConverterModel} two-layer shell over the borrowed
     * upstream {@code machines/generators/motor_liquid/} colored {front,back,sides} +
     * overlay groups (task tex-bridge-kinetic — the p12 single-cube_all borrow retired;
     * the census B3 ruling), the FACING property rotating the front exactly like the steam
     * family (upstream MultiTileEntityMotorLiquid.java:242-254 + getTexture2 :216-221 —
     * the front is the emit face, the back the exhaust). The upstream overlay_active
     * group stays unborrowed — the port block carries no ACTIVE property (the static-face
     * posture, declared defer with the steam family). The grayscale colored body is the
     * tintindex-0 seat and the row's loader NBT_MATERIAL (:721-729) is the tint — the
     * GTMachinePaintTint diesel arm resolves it off {@code DieselSpec.material}.
     * The 8 BlockItem models stay 2D icons over the shared front sprite (the
     * tex-itemform-b form per row), repointed to the renamed byte-identical
     * {@code diesel_engine_colored_front}.
     */
    private void addDieselEngines() {
        ModelFile tModel = addConverterModel("diesel_engine");
        for (var tBlock : GT6Kinetics.DIESEL_BLOCKS.values()) {
            converterBlockstate(tBlock.get(), tModel, GTDieselEngineBlock.FACING, null);
            itemModels().withExistingParent(tBlock.getId().getPath(), mcLoc("item/generated")).texture("layer0", modLoc("block/diesel_engine_colored_front"));
        }
    }

    /**
     * Task c-water-wheel — the Water Wheel: one cube_all over the ORIGINAL
     * {@code gt6:block/water_wheel} texture (the kTFRUAddon PNG is NOT borrowed — the
     * research-card license ruling: AGPL artwork never enters this repo, the wheel
     * texture is drawn for the port). ONE model over every AXIS state (simpleBlock =
     * partialState().setModels() matches all states, the diesel FACING precedent) — the
     * blade spin visual is the declared defer (the GT6Kinetics.WATER_WHEEL doc; the
     * functional ACTIVE output rides the BE). The BlockItem is the 2D icon over the
     * wheel sprite (tex-itemform-b).
     */
    private void addWaterWheel() {
        Block tWheel = GT6Kinetics.WATER_WHEEL.get();
        simpleBlock(tWheel, models().cubeAll("water_wheel", modLoc("block/water_wheel")));
        itemModels().withExistingParent("water_wheel", mcLoc("item/generated")).texture("layer0", modLoc("block/water_wheel"));
    }

    /**
     * Task c-ulv-dynamo-row — the Electric Dynamo T0 ULV row: the addFeConverter
     * shape (one cube_all over the SHARED placeholder {@code block/energy_source.png}, no
     * new PNG — the p20 borrow posture) with the empty-partial wildcard variant covering
     * the FACING property (the addAttachments convention — the partialState().setModels()
     * empty key matches every state, the water wheel "static facing" precedent). The
     * facing is a functional IO face (FRONT out EU / BACK in RU), not a visual state in
     * this placeholder — issue #18 (task 18-converter-tex-facing) upgraded the five
     * LV..IV rows to the borrowed upstream dynamos art; the T0 row KEEPS the placeholder
     * because upstream carries no T0 art at all (the Electric_T[0] slot has no machine,
     * MT.java:3691 — PLACEHOLDER, NO UPSTREAM COUNTERPART, the README ledger row).
     */
    /**
     * Task w4-battery-storage, re-formed by task tex-composite-family — the twelve
     * BatteryBox rows ride the true two-layer borrows (the p29 src-over side bake retired):
     * the compositeEnergyModel pair per size ({@code block/battery_box{,_large}} — the
     * tintindex-0 grayscale colored body + the six 0.01 overlay plates, cutout) with the
     * ACTIVE variant swapping the shell to the overlay_active art (upstream getTexture2
     * {@code sOverlays[mActiveState & 3]}, MultiTileEntityBatteryBox :31-:33 — the
     * trinary collapsed to the boolean, the blinking third state the #18 defer). Front
     * on the FACING face (the OUTPUT), side elsewhere (upstream has no back art, the
     * two-icon table). The rows tint Electric_T[i] through the block's material column
     * (Loader :894-:895 NBT_MATERIAL, the GTMachinePaintTint seat). The BlockItems parent
     * the two-layer block models again (the ok3D form — the tex-itemform-a transitional
     * 2D icons retired per that card's flip-back declaration).
     */
    private void addBatteryBoxes() {
        ModelFile tSmall = compositeEnergyModel("battery_box", false);
        ModelFile tLarge = compositeEnergyModel("battery_box_large", false);
        for (gregtech6.registry.GT6Batteries.BoxRow tRow : gregtech6.registry.GT6Batteries.BOX_ROWS) {
            net.minecraft.world.level.block.Block tBlock = gregtech6.registry.GT6Batteries.BATTERY_BOX_BLOCKS.get(tRow.path()).get();
            String tDir = tRow.slots() == 16 ? "battery_box_large" : "battery_box";
            converterBlockstate(tBlock, tRow.slots() == 16 ? tLarge : tSmall,
                    gregtech6.block.energy.GT6BatteryBoxBlock.FACING,
                    compositeEnergyActiveModel(tRow.slots() == 16 ? tLarge : tSmall, tDir, false));
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tDir + "/" + tDir));
        }
    }

    /**
     * Task energy-tail-machines, re-formed by task tex-composite-family — the
     * Crystal Chargers: the 20-row LU family rides the true two-layer borrows (the p36
     * src-over composites retired): the compositeEnergyModel pair per size
     * ({@code block/crystal_charger{,_large}} — the upstream crystal_laser{,_large} art,
     * MultiTileEntityCrystalCharger :39-:50) with the ACTIVE variant swapping the shell
     * to the overlay_active trio (upstream {@code sOverlays[mActiveState & 3]}, :33-:36
     * — the blinking third state the #18 defer). Front on the FACING face, side on the
     * other five (upstream has no back art, the two-icon table). The rows tint
     * Electric_T[i] (Loader :970-:971 NBT_MATERIAL).
     */
    private void addCrystalChargers() {
        ModelFile tSmall = compositeEnergyModel("crystal_charger", false);
        ModelFile tLarge = compositeEnergyModel("crystal_charger_large", false);
        for (gregtech6.registry.GT6CrystalChargers.ChargerRow tRow : gregtech6.registry.GT6CrystalChargers.ROWS) {
            net.minecraft.world.level.block.Block tBlock = gregtech6.registry.GT6CrystalChargers.BLOCKS_BY_PATH.get(tRow.path()).get();
            String tDir = tRow.slots() == 16 ? "crystal_charger_large" : "crystal_charger";
            converterBlockstate(tBlock, tRow.slots() == 16 ? tLarge : tSmall,
                    gregtech6.block.energy.GT6BatteryBoxBlock.FACING,
                    compositeEnergyActiveModel(tRow.slots() == 16 ? tLarge : tSmall, tDir, false));
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tDir + "/" + tDir));
        }
    }

    /**
     * Task energy-zpm-dechargers, re-formed by task tex-composite-family — the two
     * ZPM Decharger rows ride the true two-layer borrows (the p36 src-over composites
     * retired): the compositeEnergyModel pair ({@code block/zpm_decharger{,_quantum}} —
     * the upstream zpm_electricity/zpm_quantum art) with the ACTIVE variant swapping the
     * shell to the overlay_active trio (upstream {@code sOverlays[mActiveState & 3]},
     * MultiTileEntityZPMDechargerEU :39-:44 — the blinking third state the #18 defer).
     * Front on the FACING face, back on the opposite, side elsewhere (upstream index
     * 0/1/2). The {@code BI.ZPM_TOP} back decal (the {@code (mActiveState & 4)}
     * ZPM-inserted lamp, :42) stays the render-pool defer — the dynamic item-presence
     * decal, the barometer ruling. The rows tint Osmiridium (Loader :1000-:1001
     * NBT_MATERIAL, both rows).
     */
    private void addZpmDechargers() {
        ModelFile tElectric = compositeEnergyModel("zpm_decharger", true);
        ModelFile tQuantum = compositeEnergyModel("zpm_decharger_quantum", true);
        for (gregtech6.registry.GT6ZpmDechargers.DechargerRow tRow : gregtech6.registry.GT6ZpmDechargers.ROWS) {
            net.minecraft.world.level.block.Block tBlock = gregtech6.registry.GT6ZpmDechargers.BLOCKS_BY_PATH.get(tRow.path()).get();
            String tDir = tRow.path().equals("zpm_decharger_electric") ? "zpm_decharger" : tRow.path(); // the electric family drops the infix, the quantum family keeps it (the texture dir names)
            ModelFile tModel = tRow.path().equals("zpm_decharger_electric") ? tElectric : tQuantum;
            converterBlockstate(tBlock, tModel, gregtech6.block.energy.GT6BatteryBoxBlock.FACING,
                    compositeEnergyActiveModel(tModel, tDir, true));
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tDir + "/" + tDir));
        }
    }

    private void addElectricDynamoUlv() {
        Block tBlock = GT6ElectricDynamos.ELECTRIC_DYNAMO_ULV.get();
        simpleBlock(tBlock, models().cubeAll("electric_dynamo_ulv", modLoc("block/energy_source")));
        itemModels().withExistingParent("electric_dynamo_ulv", mcLoc("item/generated")).texture("layer0", modLoc("block/energy_source"));
    }

    /**
     * Task c1-dynamo-bowl-models — the Electric (T1-T5) and Flux (T1-T5) dynamo ladder
     * rows: the ten registered {@code GT6DynamoBlock}s had ZERO generated assets (placed
     * they fell to the missing-model checkerboard; the census probe). Each family's ladder
     * shares the ONE facing-cube model pair (issue #18, task 18-converter-tex-facing —
     * the per-tier visual is not a column of the upstream registration, Loader
     * :946-950/:953-957 all ride one icon set): the addConverterModel two-layer grammar —
     * the tintindex-0 grayscale colored body (the runtime mRGBa multiply, the row
     * NBT_MATERIAL through GTMachineTintModel) + the six untinted overlay decal plates,
     * ACTIVE switching the shell to the overlay_active art (upstream getTexture2
     * {@code mActivity.mState>0 ? sOverlaysActive : sOverlays}, DynamoElectric :44). The
     * FRONT face = the OUTPUT {@code mFacing}, the BACK = the INPUT {@code OPOS}, the four
     * side faces the plain side art (getTexture2 index 0/1/2). The T0 ULV row keeps its
     * placeholder above (no upstream T0 art exists — the declared port extension).
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

    /** One ladder walk — the shared two-layer model pair + the row BlockItem parents (the addElectricTransformer rotation form). */
    private void addDynamoFamily(String aFamily, List<Map.Entry<String, Block>> aRows) {
        ModelFile tModel = addConverterModel(aFamily);
        for (Map.Entry<String, Block> tRow : aRows) {
            converterBlockstate(tRow.getValue(), tModel, gregtech6.block.energy.GT6DynamoBlock.FACING);
            itemModels().withExistingParent(tRow.getKey(), tModel.getLocation());
        }
    }

    /**
     * Task gearbox-transformer — the GearBox (one cube_all over the borrowed
     * {@code gt6:block/gearbox} texture, the upstream iconsets/GEARBOX.png; assets/README.md
     * attribution) and the Rotation Transformer (the crank facing-cube posture: the FRONT =
     * input face, BACK = output face — MultiTileEntityTransformerRotation :42-45, now the
     * {@link #addConverterModel} two-layer shell over the borrowed transformer_rotation
     * colored + overlay groups, task tex-bridge-kinetic; the tint seat is the
     * WoodTreated row, Loader :1668 NBT_MATERIAL, the GTMachinePaintTint arm). The
     * upstream overlay trio is an alpha-0 EMPTY layer (the README probe) — the shell is
     * the upstream-faithful pass structure, visually inert. The animated colored_active/
     * overlay_active groups stay unborrowed — the port block carries no ACTIVE property
     * (the static-face posture); no connection-mask visual layer (the per-face gear
     * overlays are the render pool). The static texture carries no rotation animation —
     * declared with the axle.
     */
    private void addGearBoxTransformer() {
        Block tBox = GT6Kinetics.GEARBOX.get();
        simpleBlock(tBox, models().cubeAll("gearbox", modLoc("block/gearbox")));
        itemModels().withExistingParent("gearbox", mcLoc("item/generated")).texture("layer0", modLoc("block/gearbox"));
        Block tTrans = GT6Kinetics.TRANSFORMER_ROTATION.get();
        converterBlockstate(tTrans, addConverterModel("transformer_rotation"), GTTransformerRotationBlock.FACING, null);
        itemModels().withExistingParent("transformer_rotation", modLoc("block/transformer_rotation"));
    }

    /**
     * Task c-ulv-lv-transformer — the Electric Transformer ULV-LV: the p12 rotation
     * transformer's facing-cube posture (the FRONT = INPUT face — the Base11 :63
     * convention; ALL-BUT-FRONT = output), now over the BORROWED two-layer upstream art
     * (issue #18, task 18-converter-tex-facing — the p28 src-over bake is retired, it
     * dropped the per-tier mRGBa and painted all nine rows the same gray-white): the
     * addConverterModel tinted-body + overlay-shell grammar, the row's Electric_T[i]
     * casing multiply through GTMachineTintModel (upstream
     * BlockTextureMulti(colored x mRGBa, overlay[mActivity.mState]),
     * MultiTileEntityTransformerElectric :35-39), ACTIVE switching the shell to the
     * overlay_active art. The blinking shell (the animated :50-57 trio) stays unborrowed
     * — the port collapsed the upstream TE_Behavior_Active_Trinary to a boolean (the
     * README defer note).
     */
    /**
     * Task long-distance-pipes — the Long Distance pipes: the 16 wire metas ride the
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
                // the six-way dispenser-form x band (issue #18 — the six-way FACING property)
                int tX = tFacing == Direction.DOWN ? 90 : tFacing == Direction.UP ? 270 : 0;
                return ConfiguredModel.builder()
                        .modelFile(tEndpointModel)
                        .rotationX(tX).rotationY((int) (tFacing.toYRot() + 180) % 360)
                        .build();
            });
            itemModels().withExistingParent(tPath, modLoc("block/electric_transformer"));
        }
        for (int tMeta = 0; tMeta < 16; tMeta++) {
            Block tWire = gregtech6.registry.GT6LongDistPipes.wireBlockOf(tMeta);
            String tPath = gregtech6.registry.GT6LongDistPipes.pathOf(tMeta);
            simpleBlock(tWire, models().cubeAll(tPath, modLoc("block/item_pipe")));
            // tex-itemform-a — the 2D icon over the shared placeholder sprite (the former
            // parent-the-block-model form tiled it over six inventory faces).
            itemModels().withExistingParent(tPath, mcLoc("item/generated")).texture("layer0", modLoc("block/item_pipe"));
        }
    }

    /**
     * The converter two-layer model (issue #18, task 18-converter-tex-facing — the
     * addHive grammar over the addBridgeFamily front/back/side texture assignment):
     * element 0 = the tinted body cube over the borrowed grayscale
     * {@code <band>_colored_{front,back,side}} art (tintindex 0 = the material tint
     * seat), elements 1-6 = the six 0.01-plate {@code <band>_overlay_*} decal shells
     * (untinted, cullface synced) — the upstream BlockTextureMulti(colored x mRGBa,
     * overlay) stack, MultiTileEntityTransformerElectric :35-39. Cutout so the shells'
     * transparent texels discard (the C7' fix shape).
     */
    private ModelFile addConverterModel(String aBand) {
        BlockModelBuilder tModel = models().getBuilder(aBand)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc("block/" + aBand + "_colored_side"))
                .texture("up", modLoc("block/" + aBand + "_colored_side"))
                .texture("north", modLoc("block/" + aBand + "_colored_front"))
                .texture("south", modLoc("block/" + aBand + "_colored_back"))
                .texture("west", modLoc("block/" + aBand + "_colored_side"))
                .texture("east", modLoc("block/" + aBand + "_colored_side"))
                .texture("particle", modLoc("block/" + aBand + "_colored_side"))
                .texture("overlay_front", modLoc("block/" + aBand + "_overlay_front"))
                .texture("overlay_back", modLoc("block/" + aBand + "_overlay_back"))
                .texture("overlay_side", modLoc("block/" + aBand + "_overlay_side"))
                .renderType("cutout");
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        tModel.element() // north
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_front").cullface(Direction.NORTH)
                .end();
        tModel.element() // south
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_back").cullface(Direction.SOUTH)
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
                .face(Direction.DOWN).texture("#overlay_side").cullface(Direction.DOWN)
                .end();
        tModel.element() // top
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_side").cullface(Direction.UP)
                .end();
        return tModel;
    }

    /**
     * The two-model convenience form: the ACTIVE model = the inactive parent with the
     * three overlay textures swapped to the {@code _overlay_active} trio (render_type does
     * NOT ride the parent chain — the shell re-declares cutout).
     */
    private ModelFile addConverterActiveModel(ModelFile aInactive) {
        String tBand = aInactive.getLocation().getPath().replaceFirst("^block/", "");
        return models().getBuilder(tBand + "_active")
                .parent(aInactive)
                .texture("overlay_front", modLoc("block/" + tBand + "_overlay_active_front"))
                .texture("overlay_back", modLoc("block/" + tBand + "_overlay_active_back"))
                .texture("overlay_side", modLoc("block/" + tBand + "_overlay_active_side"))
                .renderType("cutout");
    }

    /**
     * One composite-energy two-layer model (task tex-composite-family, the
     * sensorModel subdirectory form over the {@link #addConverterModel} grammar): the
     * tintindex-0 body cube over the borrowed grayscale colored art ({@code block/<aFamily>/
     * colored_front} on north, {@code colored_side} on the other five — or
     * {@code colored_back} on south when {@code aBack}, the upstream getTexture2 face
     * trios: battery/crystal carry front+side only, zpm/LD carry front+back+side) + the
     * six 0.01 overlay plates, cutout. The tintindex-0 seat = the row's Electric_T[i] /
     * Osmiridium NBT_MATERIAL multiply (Loader :894-:895/:970-:971/:1000-:1001/:909-:913,
     * the GTMachinePaintTint dispatch).
     */
    private ModelFile compositeEnergyModel(String aFamily, boolean aBack) {
        String tBase = "block/" + aFamily;
        // the "block/..." prefix rides INSIDE the builder path (the sensorModel note:
        // getBuilder only prepends the folder to slash-free paths)
        BlockModelBuilder tModel = models().getBuilder(tBase + "/" + aFamily)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc(tBase + "/colored_side"))
                .texture("up", modLoc(tBase + "/colored_side"))
                .texture("north", modLoc(tBase + "/colored_front"))
                .texture("south", modLoc(tBase + (aBack ? "/colored_back" : "/colored_side")))
                .texture("west", modLoc(tBase + "/colored_side"))
                .texture("east", modLoc(tBase + "/colored_side"))
                .texture("particle", modLoc(tBase + "/colored_side"))
                .texture("overlay_front", modLoc(tBase + "/overlay_front"))
                .texture("overlay_back", modLoc(tBase + (aBack ? "/overlay_back" : "/overlay_side")))
                .texture("overlay_side", modLoc(tBase + "/overlay_side"))
                .renderType("cutout");
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        tModel.element() // north
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_front").cullface(Direction.NORTH)
                .end();
        tModel.element() // south
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_back").cullface(Direction.SOUTH)
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
                .face(Direction.DOWN).texture("#overlay_side").cullface(Direction.DOWN)
                .end();
        tModel.element() // top
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_side").cullface(Direction.UP)
                .end();
        return tModel;
    }

    /**
     * The composite-energy ACTIVE shell (the {@link #addConverterActiveModel} form over
     * the subdirectory layout): the inactive parent with the three overlay textures
     * swapped to the {@code overlay_active} trio (render_type re-declared — it does not
     * ride the parent chain).
     */
    private ModelFile compositeEnergyActiveModel(ModelFile aInactive, String aFamily, boolean aBack) {
        String tBase = "block/" + aFamily;
        return models().getBuilder(tBase + "/" + aFamily + "_active")
                .parent(aInactive)
                .texture("overlay_front", modLoc(tBase + "/overlay_active_front"))
                .texture("overlay_back", modLoc(tBase + (aBack ? "/overlay_active_back" : "/overlay_active_side")))
                .texture("overlay_side", modLoc(tBase + "/overlay_active_side"))
                .renderType("cutout");
    }

    /**
     * The converter blockstate walk (issue #18): SIX-WAY facing over the dispenser-form
     * rotation map (the addMagicAbsorber form — north identity, down x=90, up x=270, the
     * horizontal y band; the FACING face is the FRONT) and the ACTIVE variant switching to
     * the overlay_active shell — 12 variants per block.
     */
    private void converterBlockstate(Block aBlock, ModelFile aInactive, DirectionProperty aFacing) {
        converterBlockstate(aBlock, aInactive, aFacing, addConverterActiveModel(aInactive));
    }

    /**
     * The explicit-active walk (task tex-bridge-kinetic): {@code aActive == null} (the
     * families whose upstream art has no active group, or whose port carrier has no ACTIVE
     * property) pins BOTH property values to the inactive model — the static-face posture,
     * one model per facing.
     */
    private void converterBlockstate(Block aBlock, ModelFile aInactive, DirectionProperty aFacing, ModelFile aActive) {
        getVariantBuilder(aBlock).forAllStates(aState -> {
            Direction tFacing = aState.getValue(aFacing);
            int tX = tFacing == Direction.DOWN ? 90 : tFacing == Direction.UP ? 270 : 0;
            int tY = switch (tFacing) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0; // NORTH and the two verticals carry the x rotation only
            };
            return ConfiguredModel.builder()
                    .modelFile(aActive != null && aState.getValue(gregtech6.block.GTBlockProperties.ACTIVE) ? aActive : aInactive)
                    .rotationX(tX).rotationY(tY).build();
        });
    }

    private void addElectricTransformer() {
        // task p35 — the full :881-:889 ladder shares ONE model pair: upstream registers all
        // nine rows over the SAME icon set (machines/transformers/transformer_electric/*,
        // the per-tier visual is not a column of the registration), so the port shares the
        // addConverterModel two-layer form; the tint differentiates the rows at runtime.
        ModelFile tModel = addConverterModel("electric_transformer");
        for (GT6ElectricTransformers.TransformerRow tRow : GT6ElectricTransformers.ROWS) {
            Block tTrans = GT6ElectricTransformers.BLOCKS_BY_PATH.get(tRow.path()).get();
            converterBlockstate(tTrans, tModel, GT6ElectricTransformerBlock.FACING);
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    /**
     * Task energy-tail-machines — the Long Distance families. The five LD transformer
     * endpoints re-formed by task tex-composite-family: the true two-layer borrows
     * ({@code block/long_distance_transformer} — the upstream
     * longdistancetransformer_electric art, MultiTileEntityLongDistanceTransformer
     * :284-:299 — front = the INPUT face, back = the OUTPUT face) with the ACTIVE variant
     * swapping the shell to the overlay_active trio (upstream
     * {@code sOverlays[mActiveState & 3]}); the overlay_blinking and the LD-only
     * overlay_unloaded trios stay unborrowed (the trinary defer / no port unloaded
     * channel). The rows tint Electric_T[4..8] (Loader :909-:913 NBT_MATERIAL). The 16 LD
     * wire metas ride the property-less cube-all shape over their TIER sprite — the five
     * distinct LONG_DIST_WIRES_01 iconset art (Textures.java:638-655: metas 0-1=EV, 2=IV,
     * 3-7=LuV, 8-11=ZPM, 12-15=UV; the same split as the tier-byte table of
     * Loader_Blocks.java:160).
     */
    private void addLDEnergyFamilies() {
        ModelFile tLDModel = compositeEnergyModel("long_distance_transformer", true);
        for (gregtech6.registry.GT6LongDistanceTransformers.LDRow tRow : gregtech6.registry.GT6LongDistanceTransformers.ROWS) {
            Block tTrans = gregtech6.registry.GT6LongDistanceTransformers.BLOCKS_BY_PATH.get(tRow.path()).get();
            converterBlockstate(tTrans, tLDModel,
                    GT6ElectricTransformerBlock.FACING,
                    compositeEnergyActiveModel(tLDModel, "long_distance_transformer", true));
            itemModels().withExistingParent(tRow.path(), modLoc("block/long_distance_transformer/long_distance_transformer"));
        }
        for (gregtech6.registry.GT6LongDistWires.WireRow tRow : gregtech6.registry.GT6LongDistWires.ROWS) {
            String tPath = gregtech6.registry.GT6LongDistWires.pathOf(tRow.meta());
            Block tWire = gregtech6.registry.GT6LongDistWires.BLOCKS_BY_META.get(tRow.meta()).get();
            var tModel = models().cubeAll(tPath, modLoc("block/long_dist_wire_" + wireArtOf(tRow.tier())));
            getVariantBuilder(tWire).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
            // tex-itemform-a — the 2D icon over the row's own tier sprite (the former
            // parent-the-cube-all form stretched the 1px-ish wire art into six block faces).
            itemModels().withExistingParent(tPath, mcLoc("item/generated"))
                    .texture("layer0", modLoc("block/long_dist_wire_" + wireArtOf(tRow.tier())));
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
     * Task prefixblock-render spec ① — the 3773 material prefix blocks
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
     * MT.java:210-215 assigns the set name strings). Since task wire-family-w2 the
     * implementation lives in the MC-free single source
     * {@link gregtech6.client.wire.GTWireTextures} (shared with the client listener — this
     * class's statics bootstrap-gate the offline test JVM, that one does not).
     */
    public static String blockSetOf(OreDictMaterial aMaterial) {
        return gregtech6.client.wire.GTWireTextures.blockSetOf(aMaterial);
    }

    /**
     * Task boiler-tank — the 26 Steam Boiler Tank rows (Loader_MultiTileEntities.java
     * :553-579): ONE oriented cube model over the borrowed boiler_steam group, RE-BORROWED
     * by task tex-large-boilers (the former grayscale placeholder set and its "the
     * overlay iconset has no borrowable source" claim are retired — the
     * machines/tanks/boiler_steam overlay group ships upstream): the {@link #boilerModel}
     * two-layer grammar — the grayscale colored body is the tintindex-0 seat (the upstream
     * colored x mRGBa, MultiTileEntityBoilerTank :240) and the overlay plates are the
     * untinted decals. Upstream ships NO dedicated front art (FACES_TBS={0,1,2,2,2,2},
     * CS.java:618 — the barometer face renders the side sprite), so all four sides share
     * the side art and the former front placeholder is retired; the FACING y-rotation
     * stays (the FRONT drives the BE semantics, the per-state barometer visual is the
     * render pool — the burning-box ruling repeated). Both ladders share the model (the
     * SAME block class upstream, :552 aClass). The 26 BlockItem models parent it.
     *
     * <p>Task world-tint-render-type (the C5 clean-up) put the body on the
     * {@code GTMachineTintModel} bake + the {@code GTItemPaintTint} inventory half (the
     * 43f48149b burning-box form) — unchanged; the uniform cutout declaration joins the
     * paintable census (opaque textures render identically on cutout).
     */
    private void addBoilers() {
        ModelFile tModel = boilerModel("steam_boiler_tank", "boiler_steam", false);
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
     * Task storage-hopper-family — the 4 storage-hopper rows (Loader_MultiTileEntities
     * .java:145-146 over :191/:202, Bronze/Steel × hopper/queue). Task tex-placeholder-audit
     * UPGRADED the target (the "no borrowable source" claim was proven false — the
     * {@code machines/automation/hopper} and {@code queuehopper} groups exist in the snapshot,
     * MultiTileEntityHopper.java:284-293 / QueueHopper:266-275): ONE {@link #boilerModel}
     * two-layer TBS cube per KIND over the borrowed groups — the upstream getTexture2 is the
     * FACES_TBS trio (bottom/top/side, NO front art — the boiler-tank form, the front
     * placeholder retired), tint seat OFF (the unpaint deviation, the static-storages
     * ruling). The FACING drives the output semantics (the vanilla Piston 6-way blockstate
     * convention stays); the three-pass custom funnel shape of upstream :263-277 is the
     * render pool. The 4 BlockItem models parent their kind model.
     */
    private void addHoppers() {
        ModelFile tHopper = boilerModel("gt6_hopper", "hopper", false, false);
        ModelFile tQueue = boilerModel("gt6_queuehopper", "queuehopper", false, false);
        for (gregtech6.registry.GT6Hoppers.HopperRow tRow : gregtech6.registry.GT6Hoppers.ROWS) {
            Block tBlock = gregtech6.registry.GT6Hoppers.BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tModel = tRow.queue() ? tQueue : tHopper;
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
     * Task burning-box-family — the 97 burning-box rows (Loader_MultiTileEntities.java
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
                .texture("overlay_bottom", modLoc("block/burning_box_" + aGroup + "_overlay_bottom" + tSuffix))
                // issue #8 (task world-tint-render-type): the six burning decals are
                // transparent-texel overlay shells — cutout discards them off the body
                // (the D2 leg6 fix shape).
                .renderType("cutout");
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
     * Task large-boiler — the Large Boiler family (Loader_MultiTileEntities.java
     * :1159-1165/:1176/:1248-1252): the five boiler variant controllers over ONE shared
     * two-layer front-bearing model, task tex-large-boilers — the former single-layer
     * grayscale cube and its "no borrowable largeboiler group" claim are retired (the
     * probe found the full machines/multiblockmains/largeboiler group): the Base10 default
     * {@code getTexture2} semantics (TileEntityBase10MultiBlockBase.java:192-194) — the
     * FRONT face wears the {@code colored_front}+{@code overlay_front} pair, the other
     * five faces the plain pair; the colored body is the tintindex-0 seat (the row
     * NBT_MATERIAL through the {@code GTMultiBlockControllerBlock} carrier, the p38-c2
     * form) and the overlay plates untinted. The BI.BAROMETER dynamic gauge is the render
     * pool (the p13 ruling). The FORMED variants map to the same model (the formed-look
     * visual is the p9 pool). The 5 BlockItem models parent their block model. The Dense
     * Wall + transmitter parts ride addParts() (the w3-nbtdesign-parts walk).
     */
    private void addLargeBoiler() {
        ModelFile tMain = boilerModel("large_boiler", "large_boiler", true);
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
        // task w3-nbtdesign-parts — the Dense Wall parts + the transmitter moved to
        // addParts() (the dense walls carry the DESIGN property — per-design variants; the
        // transmitter rides the upstream heatacceptor borrow); addLargeBoilerPart retired
    }

    /**
     * One boiler two-layer model (task tex-large-boilers; the addConverterModel grammar
     * over the boiler groups' bottom/top/side face keys): the tinted body cube over the
     * borrowed grayscale {@code block/<band>/colored_<face>} art (tintindex 0 = the
     * material tint seat; {@code aFront} binds the north face to the
     * {@code colored_front_side} art — the Base10 front-face pair) plus the six 0.01-plate
     * {@code block/<band>/overlay[_front]_<face>} decal shells (untinted, cullface synced
     * — the P22 pairing; the upstream overlay pass is NOT multiplied by mRGBa). Cutout so
     * the shells' transparent texels discard (the C7' fix shape). The {@code aTint} arm
     * (task tex-placeholder-audit) drops the tint seat for the unpaint families (the
     * hoppers — the static-storages unpaint deviation row shape).
     */
    private ModelFile boilerModel(String aName, String aBand, boolean aFront) {
        return boilerModel(aName, aBand, aFront, true);
    }

    private ModelFile boilerModel(String aName, String aBand, boolean aFront, boolean aTint) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc("block/" + aBand + "/colored_bottom"))
                .texture("up", modLoc("block/" + aBand + "/colored_top"))
                .texture("north", modLoc("block/" + aBand + (aFront ? "/colored_front_side" : "/colored_side")))
                .texture("south", modLoc("block/" + aBand + "/colored_side"))
                .texture("west", modLoc("block/" + aBand + "/colored_side"))
                .texture("east", modLoc("block/" + aBand + "/colored_side"))
                .texture("particle", modLoc("block/" + aBand + "/colored_side"))
                .texture("overlay_down", modLoc("block/" + aBand + "/overlay_bottom"))
                .texture("overlay_up", modLoc("block/" + aBand + "/overlay_top"))
                .texture("overlay_north", modLoc("block/" + aBand + (aFront ? "/overlay_front_side" : "/overlay_side")))
                .texture("overlay_south", modLoc("block/" + aBand + "/overlay_side"))
                .texture("overlay_west", modLoc("block/" + aBand + "/overlay_side"))
                .texture("overlay_east", modLoc("block/" + aBand + "/overlay_side"))
                .renderType("cutout");
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> {
                    aFace.texture("#" + aDir.getName()).cullface(aDir);
                    if (aTint) aFace.tintindex(0);
                })
                .end();
        tModel.element() // north
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_north").cullface(Direction.NORTH)
                .end();
        tModel.element() // south
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_south").cullface(Direction.SOUTH)
                .end();
        tModel.element() // west
                .from(-0.01F, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                .face(Direction.WEST).texture("#overlay_west").cullface(Direction.WEST)
                .end();
        tModel.element() // east
                .from(16.0F, 0.0F, 0.0F).to(16.01F, 16.0F, 16.0F)
                .face(Direction.EAST).texture("#overlay_east").cullface(Direction.EAST)
                .end();
        tModel.element() // bottom (down)
                .from(0.0F, -0.01F, 0.0F).to(16.0F, 0.0F, 16.0F)
                .face(Direction.DOWN).texture("#overlay_down").cullface(Direction.DOWN)
                .end();
        tModel.element() // top (up)
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_up").cullface(Direction.UP)
                .end();
        return tModel;
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
     * Task lightning-rod — the Lightning Rod family (Loader_MultiTileEntities.java
     * :1151/:1168/:1179/:1282): the three part blocks as plain cube_all over the borrowed
     * upstream textures (the multiblockparts metalwall/coil/lightningrod colored faces, the
     * P20 ruling ①), and the single controller over the two-layer front-bearing tinted
     * grammar over the borrowed multiblockmains lightningrod group, task
     * tex-multiblockmains — the former borrow-time-composite single-image cube and
     * its "all faces composite to the same pixels" claim are retired (the group's
     * colored/colored_front split is real art): the Base10 default {@code getTexture2}
     * semantics (TileEntityBase10MultiBlockBase.java:192-194), the boiler form — the
     * FRONT face the {@code colored_front}+{@code overlay_front} pair, the other five
     * faces the plain pair, the colored body the tintindex-0 seat (the :1282
     * NBT_MATERIAL ANY.W row through the {@code GTLightningRodBlock} carrier ctor, the
     * p38-c2 form).
     * The facing is structurally meaningless (the rod is vertical), so every state maps to
     * the same model with no rotation (the front pair lands on north); the FORMED variants
     * map to the same model (the formed-look visual is the p9 pool). The controller and the
     * rod BlockItems are 2D icons over their composite/rod sprites (tex-itemform-b).
     */
    /**
     * Task w3-tank-valves — the Tank Main Valve family (Loader_MultiTileEntities.java
     * :1195-1222): the 25 variant controllers over ONE shared two-layer front-bearing
     * cube model per material family, re-textured by task tex-tank-family — the
     * borrowed {@code tank_valves/<family>_*} multiblockmains art (the wood valve over
     * the tankwood group, the 24 metal valves over the tankmetal group) in the
     * {@link #tankValveModel} Base10 front-layer-pair grammar: the north face carries
     * the {@code colored_front}/{@code overlay_front} pair, the remaining faces the
     * {@code colored}/{@code overlay} set (TileEntityBase10MultiBlockBase.java:192-194
     * getTexture2 semantics). The FACING + FORMED variants map to the same model with
     * the addLargeBoiler y-rotation band (model-space north = the front) — the
     * formed-look visual stays the p9 pool. Task issue8-residual: the body element
     * carries {@code tintindex 0} — the grayscale colored textures multiply the row's
     * NBT_MATERIAL (every :1195-1222 row carries the column, the upstream
     * {@code getTexture2} colored×mRGBa form; the bake/ItemColor consumers ride
     * GTMachineTintModel/GTItemPaintTint through the controller gate), the decal shells
     * stay untinted. The 25 BlockItem models parent their block models.
     */
    private void addTanks() {
        ModelFile tWood = tankValveModel("tank_wood", "tankwood");
        ModelFile tMetal = tankValveModel("tank_metal", "tankmetal");
        for (var tRow : gregtech6.registry.GT6Tanks.ROWS) {
            Block tBlock = gregtech6.registry.GT6Tanks.BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tModel = tRow.flammable() ? tWood : tMetal; // the wood valve is the flammable row
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
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
     * The valve two-layer front-bearing model (task tex-tank-family, the
     * addConverterModel grammar over the multiblockmains per-face key set): element 0 =
     * the tinted body cube (tintindex 0 = the material tint seat; down/up =
     * {@code colored_bottom}/{@code colored_top}, north = the {@code colored_front_side}
     * front art, south/west/east = {@code colored_side}), elements 1-6 = the six 0.01-plate
     * decal shells (north = {@code overlay_front_side}, the rest the {@code overlay_*}
     * set, untinted, cullface synced) — the upstream Base10 front-pair form,
     * TileEntityBase10MultiBlockBase.java:192-194. Cutout so the shells' transparent
     * texels discard (the C7' fix shape).
     */
    private ModelFile tankValveModel(String aName, String aFamily) {
        String tBand = "block/tank_valves/" + aFamily + "_";
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc(tBand + "colored_bottom")).texture("up", modLoc(tBand + "colored_top"))
                .texture("north", modLoc(tBand + "colored_front_side")).texture("south", modLoc(tBand + "colored_side"))
                .texture("west", modLoc(tBand + "colored_side")).texture("east", modLoc(tBand + "colored_side"))
                .texture("particle", modLoc(tBand + "colored_side"))
                .texture("overlay_front_side", modLoc(tBand + "overlay_front_side"))
                .texture("overlay_bottom", modLoc(tBand + "overlay_bottom"))
                .texture("overlay_top", modLoc(tBand + "overlay_top"))
                .texture("overlay_side", modLoc(tBand + "overlay_side"))
                .renderType("cutout");
        tintedBody(tModel);
        overlayShell(tModel, "overlay_front_side", "overlay_side", "overlay_side", "overlay_top", "overlay_bottom");
        return tModel;
    }

    /**
     * One tinted full-cube model (task issue8-residual; the partModel body form without
     * the decal overlays — the addBridgeFamily re-declared-element grammar): the borrowed
     * grayscale colored faces on the six texture keys, the single body cube element carries
     * {@code tintindex 0} so the GTMachineTintModel bake multiplies the carrier's
     * NBT_MATERIAL (the vanilla cube parent's own elements are replaced by the child's).
     */
    private ModelFile tintedCube(String aName, String aBottom, String aTop, String aSide) {
        return tintedCube(aName, aBottom, aTop, aSide, aSide);
    }

    /**
     * The front-bearing overload (task world-tint-render-type, the C5 boiler clean-up):
     * the north face carries {@code aFront} (the boiler's barometer face) while the other
     * four sides share {@code aSide} — the blockstate y-rotation moves the front with the
     * FACING, so north IS the model-space front.
     */
    private ModelFile tintedCube(String aName, String aBottom, String aTop, String aSide, String aFront) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc(aBottom)).texture("up", modLoc(aTop))
                .texture("north", modLoc(aFront)).texture("south", modLoc(aSide))
                .texture("west", modLoc(aSide)).texture("east", modLoc(aSide))
                // issue #8 (task world-tint-render-type): uniform cutout over the
                // paintable families (the census convention — tanks, crucible walls, boilers)
                .renderType("cutout");
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        return tModel;
    }

    private void addLightningRod() {
        ModelFile tMain = boilerModel("multiblock_lightning_rod", "lightningrod", true);
        Block tController = gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD.get();
        getVariantBuilder(tController).forAllStates(aState -> ConfiguredModel.builder().modelFile(tMain).build());
        itemModels().withExistingParent("multiblock_lightning_rod", mcLoc("item/generated")).texture("layer0", modLoc("block/lightningrod/main"));
        // the Tungsten Wall (task world-tint-render-type, the C5 clean-up; task
        // debt-tungsten-wall-designs): the row IS the :1151 machine_wall row (texture
        // "metalwall", NBT_DESIGNS 7, ANY.W) — its blockstate/item/models moved to the
        // addParts() new-form walk (the anyPartBlock lookup), where it emits the full
        // design 0..7 ladder like its ten siblings; the former single-design
        // partModel(metalwall, 0) special case is retired.
        // the Niobium-Titanium Coil (task debt-coil-design): the row IS the :1168 coil
        // row (texture "coil", NBT_DESIGNS 1 — the designs 0/1 range of all six siblings
        // :1167-1172) — the same anyPartBlock walk carries it over the two-step design
        // ladder; the former cube_all borrow (lightningrod/coil) simpleBlock special
        // case is retired (the family two-layer form replaces it).
        addLightningRodPart("lightning_rod", "block/lightningrod/rod");
    }

    /** One cube_all Lightning Rod part block + its 2D-icon BlockItem (the addLargeBoilerPart shape, tex-itemform-b). */
    private void addLightningRodPart(String aPath, String aTexture) {
        Block tBlock = gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD_PART_BLOCKS_BY_PATH.get(aPath).get();
        simpleBlock(tBlock, models().cubeAll(aPath, modLoc(aTexture)));
        itemModels().withExistingParent(aPath, mcLoc("item/generated")).texture("layer0", modLoc(aTexture));
    }

    /**
     * Task stoneblocks-16item-registry-split — the 272 GT6 stone VARIANT blocks
     * ({@link GTStoneBlocks#blockArray()}, stone-major in CS.java:1668 order and
     * variant-major in meta order, the per-pair registry split): each block is a degenerate
     * pure block with a FIXED {@link StoneVariant}, so each gets a plain single-state
     * blockstate over its OWN cube_all model (the P19 16-row {@code variant=<snake>} rows
     * retired with the EnumProperty — one property-free state per block now), and each gets
     * its OWN item model {@code withExistingParent} onto that block model (the id scheme is
     * {@link GTStoneBlocks#path}: variant 0 keeps the bare snake, the other 15 suffix the
     * variant segment). The model/texture keys are UNCHANGED from the P19 render card
     * ({@code gt6:block/stones/<stone>/<variant>}, one model per pair, assets/README.md
     * attribution, census 17x16 = 272 files, zero gaps, NO tintindex — the colored-PNG
     * route), so only the blockstate/item/loot faces re-key per pair. The TEXTURE of a pair
     * rides {@link #stoneTexture}: the granite/diorite/andesite trio's STONE/SMOTH variants
     * point at the vanilla current textures (task ore-tex-b), every other pair keeps its
     * borrowed PNG.
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
            ModelFile tModel = models().cubeAll(tModelName, stoneTexture(tStone.stoneSnake, tStone.variant));
            simpleBlock(tBlock, tModel); // the single default state -> the variants:{"": ...} form
            itemModels().withExistingParent(GTStoneBlocks.path(tStone.stoneSnake, tStone.variant), modLoc(tModelName));
        }
        LOGGER.info("GT6 stone blocks: {} per-pair blockstates over {} models (one item model each)",
                GTStoneBlocks.STONES.size() * StoneVariant.VALUES.length,
                GTStoneBlocks.STONES.size() * StoneVariant.VALUES.length);
    }

    /**
     * The texture a (stone, variant) pair renders: the borrowed per-pair PNG
     * {@code gt6:block/stones/<stone>/<variant>} — except the granite/diorite/andesite
     * trio where vanilla 1.20.1 HAS a counterpart (task ore-tex-b, the user B ruling
     * "same look both sides"): the STONE variant points at {@code minecraft:block/<stone>}
     * and the SMOOTH (= polished, the OP.stonePolished carrier) variant at
     * {@code minecraft:block/polished_<stone>}. The other 14 variants (cobble, bricks,
     * tiles, windmill, ...) have NO vanilla counterpart and keep the borrowed PNGs —
     * {@code cobble} additionally stays consumed by the crucible/mold machine bodies
     * (GT6CrucibleDatagen/GT6MoldDatagen BODY_TEXTURE). Static package seam so the
     * render test pins the mapping (the GT6OreBlockStates.modelNameOf posture).
     */
    static ResourceLocation stoneTexture(String aStoneSnake, StoneVariant aVariant) {
        if (VANILLA_MIGRATED_STONES.contains(aStoneSnake)) {
            if (aVariant == StoneVariant.STONE) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "block/" + aStoneSnake);
            }
            if (aVariant == StoneVariant.SMOTH) {
                return ResourceLocation.fromNamespaceAndPath("minecraft", "block/polished_" + aStoneSnake);
            }
        }
        // fromNamespaceAndPath, not modLoc (this seam is static for the offline test; the
        // concatenated arg escapes the 21.1 swap regex — the GTOreBakedModel.baseSpriteOf form)
        return ResourceLocation.fromNamespaceAndPath("gt6", "block/stones/" + aStoneSnake + "/" + aVariant.snake);
    }

    /** The trio with a vanilla 1.20.1 counterpart stone (task ore-tex-b; basalt is the same-name DIFFERENT stone and stays borrowed). */
    private static final List<String> VANILLA_MIGRATED_STONES = List.of("granite", "diorite", "andesite");

    /**
     * Task debt-slab-gap — the 272 per-pair stone SLABS ({@link gregtech6.registry.GTStoneSlabBlocks},
     * the upstream mSlabs[0] bottom-slab face of BlockMetaType.java:74-81): the
     * BOTTOM/TOP/DOUBLE triad per slab over the SAME texture the paired full block
     * renders ({@code block/stones/<stone>/<variant>} via {@link #stoneTexture} — the
     * trio STONE/SMOTH vanilla references ride along, zero new textures, no tint — the
     * pre-coloured-PNG route). BOTTOM/TOP are own {@code minecraft:block/slab[_top]}
     * parents; DOUBLE re-uses the full cube model the {@link #addStoneBlocks} pass
     * registers (a second {@code cubeAll} on the same name returns the SAME cached
     * builder — ModelProvider.getBuilder caches by name, so the run emits one file).
     * The upstream double is the full block itself (a separate block id, the :142-153
     * right-click merge) — the DOUBLE state pointing at the full model IS that face.
     * Each slab's item model parents its bottom model (the vanilla slab item form).
     * WATERLOGGED stays unspecified in the partial states (a wildcard — the foam-band
     * {@link #tintedSlabFamily} precedent).
     */
    private void addStoneSlabs() {
        for (Block tBlock : gregtech6.registry.GTStoneSlabBlocks.blockArray()) {
            gregtech6.block.stone.GTStoneSlabBlock tSlab = (gregtech6.block.stone.GTStoneSlabBlock)tBlock;
            String tTexture = "block/stones/" + tSlab.stoneSnake + "/" + tSlab.variant.snake;
            ResourceLocation tTexLoc = stoneTexture(tSlab.stoneSnake, tSlab.variant); // the trio STONE/SMOTH rides vanilla (ore-tex-b)
            ModelFile tBottom = models().getBuilder(tTexture + "_slab")
                    .parent(models().getExistingFile(mcLoc("block/slab")))
                    .texture("bottom", tTexLoc).texture("top", tTexLoc).texture("side", tTexLoc);
            ModelFile tTop = models().getBuilder(tTexture + "_slab_top")
                    .parent(models().getExistingFile(mcLoc("block/slab_top")))
                    .texture("bottom", tTexLoc).texture("top", tTexLoc).texture("side", tTexLoc);
            ModelFile tFull = models().cubeAll(tTexture, tTexLoc); // the cached addStoneBlocks builder
            getVariantBuilder(tSlab)
                    .partialState().with(SlabBlock.TYPE, SlabType.BOTTOM)
                    .setModels(ConfiguredModel.builder().modelFile(tBottom).build())
                    .partialState().with(SlabBlock.TYPE, SlabType.TOP)
                    .setModels(ConfiguredModel.builder().modelFile(tTop).build())
                    .partialState().with(SlabBlock.TYPE, SlabType.DOUBLE)
                    .setModels(ConfiguredModel.builder().modelFile(tFull).build());
            itemModels().withExistingParent(gregtech6.registry.GTStoneSlabBlocks.slabPath(tSlab.stoneSnake, tSlab.variant),
                    modLoc(tTexture + "_slab"));
        }
        LOGGER.info("GT6 stone slabs: {} per-pair blockstate triads over {} bottom/top model pairs (double re-uses the full cube)",
                GTStoneBlocks.STONES.size() * StoneVariant.VALUES.length,
                GTStoneBlocks.STONES.size() * StoneVariant.VALUES.length);
    }

    /**
     * Task grass-block — the 6 GT grass VARIANT blocks ({@link GTGrassBlocks#BLOCKS},
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
     * Task w6-t1-trees-nine — the 27 GT tree blocks ({@link GT6TreeBlocks}): saplings
     * render the vanilla cross idiom over the borrowed SAPLING_SMALL PNGs (cutout layer,
     * the vanilla sapling render type), logs the axis blockstate over
     * cube_column(side/end, the vanilla log idiom), leaves a cube_all over the borrowed
     * LEAVES PNG (cutout_mipped, the vanilla leaves layer). All 36 textures are the
     * upstream iconsets PNGs byte-borrowed ({@code gt6:block/tree/*}, the
     * assets/README.md ledger face; the grass card pre-coloured-PNG precedent). The
     * Rainbowood leaves carry tintindex 0 over their grayscale PNG (task issue1-4,
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
            getVariantBuilder(tLog).forAllStates(tState ->
                    switch (tState.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)) {
                // the vanilla axisBlock rotation map (the addAxles/addSurfacePlants form;
                // x-only-90 tips a Y-column onto Z and y-only-90 spins it in place —
                // GitHub #26, the #26 fallen-log fix applied to the standing rows)
                case X -> new ConfiguredModel[] {new ConfiguredModel(tLogModel, 90, 90, false)};
                case Y -> new ConfiguredModel[] {new ConfiguredModel(tLogModel)};
                case Z -> new ConfiguredModel[] {new ConfiguredModel(tLogModel, 90, 180, false)};
            });
            itemModels().withExistingParent(tSnake + "_log", modLoc("block/" + tSnake + "_log"));
            // leaves: cube_all + cutout_mipped. The Rainbowood row adds tintindex 0 on
            // every face (the tintedCubeAll grammar) over its GRAYSCALE PNG — the world/
            // inventory tint tables are the GT6TreeClientListener RAINBOW registrations
            // (task issue1-4, GitHub #4 — the upstream BlockTreeLeavesAB.java:129-139
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
     * Task w6-t2-surface-blocks — the obtainable surface band ({@link GT6SurfaceBlocks}
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
            getVariantBuilder(tLog).forAllStates(tState ->
                    switch (tState.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)) {
                // the vanilla axisBlock rotation map (the addAxles band form; x-only-90
                // tips a Y-column onto Z and y-only-90 spins it in place — GitHub #26)
                case X -> new ConfiguredModel[] {new ConfiguredModel(tLogModel, 90, 90, false)};
                case Y -> new ConfiguredModel[] {new ConfiguredModel(tLogModel)};
                case Z -> new ConfiguredModel[] {new ConfiguredModel(tLogModel, 90, 180, false)};
            });
            itemModels().withExistingParent(tPath, modLoc("block/" + tPath));
        }
        LOGGER.info("GT6 surface plants: 8 blockstate bands (1 plate + 3 cubes + 4 axis columns), 8 models");
    }

    /**
     * Task c-foam-block-family — the C-Foam block family band (upstream
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
     * Task tex-sensors (supersedes the sensors-core cube_all bake) — the 21
     * sensor families ({@link GT6Sensors#ROWS}) ride the front-bearing two-layer faceted
     * cube, the {@link #addConverterModel} grammar MINUS the tint seat: the body is the
     * upstream colored layer ({@code sensors/<family>/colored_{front,back,side}}, the
     * byte-identical borrows) and the six 0.01-plate shells the overlay layer — the
     * upstream BlockTextureMulti(colored, overlay) stack (MultiTileEntitySensor
     * .getTexture2 :218-228: FRONT = colored/front + overlay/front, BACK = the OPOS
     * pair, the four flanks the side pair). NO tintindex: the rows register NBT-less
     * (Loader_MultiTileEntities.java:1979-1999) so mRGBa is white and the colored art
     * shows its own colours. Cutout render type by the alpha census — all 63 colored
     * PNGs are fully opaque, all 63 overlay PNGs carry transparent texels (the overlay
     * front is the 64x64 digit strip). The old src-over front bake painted this two-layer
     * form over all six faces; now only the FACING face carries the front art. The FACING
     * property IS the display face mirror (GTSensorBlockEntity#wrenchSetFacing writes
     * it), the six-way dispenser rotation map rides unchanged; the BlockItem still
     * parents the block model (the block IS what 1.7.10 rendered for the held sensor).
     *
     * <p>Declared defer: the upstream pass1-6 LIVE digit boards (the CHAR_* sprite stack,
     * MultiTileEntitySensor :143-261) stay the render pool card (pool-gauges) — the
     * borrowed overlay/front is the static art, same ruling as the boiler barometer.
     */
    private void addSensors() {
        for (GT6Sensors.SensorRow tRow : GT6Sensors.ROWS) {
            Block tBlock = GT6Sensors.BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tModel = sensorModel(tRow.path());
            getVariantBuilder(tBlock).forAllStates(aState -> switch (aState.getValue(GTSensorBlock.FACING)) {
                case NORTH -> new ConfiguredModel[] {new ConfiguredModel(tModel)};
                case SOUTH -> new ConfiguredModel[] {new ConfiguredModel(tModel, 0, 180, false)};
                case WEST  -> new ConfiguredModel[] {new ConfiguredModel(tModel, 0, 270, false)};
                case EAST  -> new ConfiguredModel[] {new ConfiguredModel(tModel, 0, 90, false)};
                case UP    -> new ConfiguredModel[] {new ConfiguredModel(tModel, 270, 0, false)};
                case DOWN  -> new ConfiguredModel[] {new ConfiguredModel(tModel, 90, 0, false)};
            });
            itemModels().withExistingParent(tRow.path(), modLoc("block/sensors/" + tRow.path()));
        }
        LOGGER.info("GT6 sensors: {} faceted two-layer blockstates x 6 FACING variants (the oriented cube)", GT6Sensors.ROWS.size());
    }

    /**
     * One sensor family's two-layer faceted model ({@code block/sensors/<family>}):
     * the untinted body cube over the colored trio (front on north) + the six 0.01
     * overlay plates (front/back/side), cullface synced — the {@link #addConverterModel}
     * shells verbatim minus the tintindex (the tex-sensors NBT=null ruling).
     */
    private ModelFile sensorModel(String aFamily) {
        String tBase = "block/sensors/" + aFamily;
        // the "block/" prefix rides INSIDE the builder path: getBuilder only prepends the
        // folder to slash-free paths (Forge ModelProvider.extendWithFolder), and getPath
        // writes models/<path>.json verbatim — "block/sensors/<family>" is what lands the
        // model at models/block/sensors/<family>.json AND tracks it for the item parent.
        BlockModelBuilder tModel = models().getBuilder(tBase)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc(tBase + "/colored_side"))
                .texture("up", modLoc(tBase + "/colored_side"))
                .texture("north", modLoc(tBase + "/colored_front"))
                .texture("south", modLoc(tBase + "/colored_back"))
                .texture("west", modLoc(tBase + "/colored_side"))
                .texture("east", modLoc(tBase + "/colored_side"))
                .texture("particle", modLoc(tBase + "/colored_side"))
                .texture("overlay_front", modLoc(tBase + "/overlay_front"))
                .texture("overlay_back", modLoc(tBase + "/overlay_back"))
                .texture("overlay_side", modLoc(tBase + "/overlay_side"))
                .renderType("cutout");
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).cullface(aDir))
                .end();
        tModel.element() // north
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_front").cullface(Direction.NORTH)
                .end();
        tModel.element() // south
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_back").cullface(Direction.SOUTH)
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
                .face(Direction.DOWN).texture("#overlay_side").cullface(Direction.DOWN)
                .end();
        tModel.element() // top
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_side").cullface(Direction.UP)
                .end();
        return tModel;
    }
    /**
     * Task w3-nbtdesign-parts ③④ — the part-family expansion (Loader
     * :1138-1189). Every new-form part block gets ONE MODEL PER DESIGN VARIANT: the
     * upstream part renders {@code mTextures[mDesign][face]} with
     * {@code mTextures = new IIconContainer[bind8(NBT_DESIGNS)+1][6]}
     * (MultiTileEntityMultiBlockPart.java:138-146), the 1.20.1 form is the
     * {@code design} blockstate variant per model. Each model is the two-layer part
     * shape: the body cube over the BORROWED upstream colored textures with tintindex 0
     * on the body (the material tint, task issue8-multipart-tint — the row
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
            // anyPartBlock resolves every registration map — machine_wall_tungsten rides
            // the Lightning Rod family's registration (task debt-tungsten-wall-designs:
            // the row now walks HERE with its full design ladder like its siblings), and
            // since task debt-coil-design so does niobium_titanium_coil (the :1168 row,
            // the designs 0/1 ladder like its five coil siblings)
            Block tBlock = gregtech6.registry.GTMultiBlocks.anyPartBlock(tRow.path());
            if (tBlock == null) continue;
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
     * Task tex-large-machines — the twelve large-machine controllers go two-layer:
     * the {@link #familyMachineModel} state trio per row over the row's upstream
     * basicmachines texture arrays (MultiTileEntityBasicMachine.java:176-203, the
     * mTexturesMaterial + mTexturesInactive/Active/Running trio — the large controllers
     * extend the same BE class, so the same grammar applies; the colored six-sets are
     * the p31-era flat borrows, the overlay state trios the r8 borrow wave, the
     * assets/README.md ledger). The BLOCKSTATE stays static on the inactive model: the
     * controllers carry no ACTIVE property (GTMultiBlockControllerBlock keeps FACING +
     * FORMED, TileEntityBase10MultiBlockMachine :105 reads mActive/mRunning BE-side) and
     * the property seat rides the shared controller base — adding it is a BE+block change
     * outside this card's files, so the _active/_running models ride the tree with the
     * switch declared defer (the upstream :1014 pick is ready to wire when a property
     * card lands). The 8 FACING x FORMED states share the inactive model (the FORMED
     * dual-model was the coke-oven enhancement; the RCON formed assertion rides the
     * blockstate property, not the model).
     */
    private void addLargeMachines() {
        for (gregtech6.registry.GT6LargeMachines.LargeMachineRow tRow : gregtech6.registry.GT6LargeMachines.ROWS) {
            Block tBlock = gregtech6.registry.GT6LargeMachines.BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tInactive = familyMachineModel(tRow.path(), tRow.texture(), "");
            familyMachineModel(tRow.path() + "_active", tRow.texture(), "_active");
            familyMachineModel(tRow.path() + "_running", tRow.texture(), "_running");
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY;
                switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                    case SOUTH -> tY = 180;
                    case WEST -> tY = 270;
                    case EAST -> tY = 90;
                    default -> tY = 0; // NORTH
                }
                return ConfiguredModel.builder().modelFile(tInactive).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
        }
    }

    /**
     * Task tex-large-machines — the Large Matter Fabricator controller joins the
     * familyMachineModel state trio over the upstream NBT_TEXTURE "largemassfab" family
     * (Loader :1241; the colored six-set stays the p31 borrow, the overlay state trio the
     * r8 ledger). The blockstate stays static on the inactive model (the addLargeMachines
     * defer declaration), the 8 FACING x FORMED states share it.
     */
    /**
     * Task logistics-lv3 — the Logistics Core controller (upstream meta 17997, the
     * NBT_TEXTURE "logisticscore" family). Task tex-multiblockmains — the former
     * "the dedicated textures have no port face yet" galvanized-steel-wall borrow and
     * its outdated claim are retired (the probe found the full
     * machines/multiblockmains/logisticscore group): the two-layer front-bearing
     * tinted grammar, the Base10 default {@code getTexture2} semantics
     * (TileEntityBase10MultiBlockBase.java:192-194), the boiler form — the FRONT face
     * the {@code colored_front}+{@code overlay_front} pair, the other five faces the
     * plain pair, the colored body the tintindex-0 seat (the :1281 NBT_MATERIAL
     * SteelGalvanized column through the {@code GTLogisticsCoreBlock} carrier ctor,
     * the p38-c2 form). The upstream logistics content art (the
     * {@code logistics/fluid,item,generic} buffer faces) is NOT borrowed — the
     * NBT-dynamic content painting is the render pool. The 8 FACING x FORMED states
     * share the one oriented model (the massfab form, the rotationY table).
     */
    private void addLogisticsCore() {
        Block tBlock = gregtech6.registry.GT6Logistics.LOGISTICS_CORE.get();
        ModelFile tModel = boilerModel("logistics_core", "logistics_core", true);
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
        ModelFile tInactive = familyMachineModel("large_massfab", tFamily, "");
        familyMachineModel("large_massfab_active", tFamily, "_active");
        familyMachineModel("large_massfab_running", tFamily, "_running");
        getVariantBuilder(tBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            return ConfiguredModel.builder().modelFile(tInactive).rotationY(tY).build();
        });
        itemModels().withExistingParent("large_massfab", modLoc("block/large_massfab"));
    }

    /**
     * Task tex-large-machines — the Fusion Reactor controller joins the
     * familyMachineModel state trio over the upstream NBT_TEXTURE "fusionreactor" family
     * (Loader :1242; colored six-set = the p31 borrow, overlay state trio = the r8
     * ledger). The blockstate stays static on the inactive model (the addLargeMachines
     * defer declaration), the 8 FACING x FORMED states share it.
     */
    private void addFusionReactor() {
        Block tBlock = gregtech6.registry.GTMultiBlocks.FUSION_REACTOR.get();
        String tFamily = "fusionreactor";
        ModelFile tInactive = familyMachineModel("fusion_reactor", tFamily, "");
        familyMachineModel("fusion_reactor_active", tFamily, "_active");
        familyMachineModel("fusion_reactor_running", tFamily, "_running");
        getVariantBuilder(tBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            return ConfiguredModel.builder().modelFile(tInactive).rotationY(tY).build();
        });
        itemModels().withExistingParent("fusion_reactor", modLoc("block/fusion_reactor"));
    }

    /**
     * Task tex-large-machines — the Implosion Compressor controller joins the
     * familyMachineModel state trio over the upstream NBT_TEXTURE "implosioncompressor"
     * family (Loader :1228; colored six-set = the p31 borrow, overlay state trio = the
     * r8 ledger). The blockstate stays static on the inactive model (the addLargeMachines
     * defer declaration), the 8 FACING x FORMED states share it.
     */
    private void addImplosionCompressor() {
        Block tBlock = gregtech6.registry.GTMultiBlocks.IMPLOSION_COMPRESSOR.get();
        String tFamily = "implosioncompressor";
        ModelFile tInactive = familyMachineModel("implosion_compressor", tFamily, "");
        familyMachineModel("implosion_compressor_active", tFamily, "_active");
        familyMachineModel("implosion_compressor_running", tFamily, "_running");
        getVariantBuilder(tBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            return ConfiguredModel.builder().modelFile(tInactive).rotationY(tY).build();
        });
        itemModels().withExistingParent("implosion_compressor", modLoc("block/implosion_compressor"));
    }

    /**
     * Task graagg — the Von da Graagg controller: the addImplosionCompressor form over
     * the upstream NBT_TEXTURE "vondagraagg" family (Loader :1280). The upstream family
     * carries ONE texture (colored/side == colored_front/side == bottom == top, byte-equal)
     * so all six faces borrow the same PNG (the autoclave all-faces-equal precedent); the
     * 8 FACING x FORMED states share the one oriented model (the RCON formed assertion
     * rides the blockstate property, not the model).
     */
    /**
     * Task tex-large-machines — the Von da Graagg controller takes the boilerModel
     * front-pair form (the Base10 default getTexture2, TileEntityBase10MultiBlockBase
     * :192-194: the front face renders the colored_front+overlay_front pair, the other
     * five the plain pair) over the band dir borrowed from upstream multiblockmains/
     * "vondagraagg" (Loader :1280). The upstream family ships NO active group (the
     * census card's "bedrockdrill overlay_active" reading disprobed — neither main has
     * one), so ONE static model; the 8 FACING x FORMED states share it.
     */
    private void addVonDaGraagg() {
        Block tBlock = gregtech6.registry.GTMultiBlocks.VON_DA_GRAAGG.get();
        ModelFile tModel = boilerModel("von_da_graagg", "vondagraagg", true);
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

    /**
     * Task tex-large-machines — the Bedrock Mining Drill controller joins the graagg
     * boilerModel front-pair form over the upstream "bedrockdrill" band dir (Loader
     * :1283); the retired six-face flat spread is the band-dir TBS borrow (the README
     * ledger). No upstream active group — ONE static model, the 4 FACING x FORMED
     * states share it.
     */
    private void addBedrockDrill() {
        Block tBlock = gregtech6.registry.GTMultiBlocks.BEDROCK_DRILL.get();
        ModelFile tModel = boilerModel("bedrock_drill", "bedrockdrill", true);
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
        itemModels().withExistingParent("bedrock_drill", modLoc("block/bedrock_drill"));
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
                .texture("overlay_side", modLoc(tBase + "/overlay/side"))
                // issue #8 (task world-tint-render-type): the six 0.01 wall decals are
                // transparent-texel overlay shells — cutout discards them off the body
                // (the D2 leg6 fix shape).
                .renderType("cutout");
        // element 0 — the body cube (the colored layer; tintindex 0 = the material tint,
        // task issue8-multipart-tint: the shared grayscale wall textures multiply the
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
     * Task w3-turbine-dynamo — the twelve Large Turbine + Large Dynamo controllers
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
                .texture("overlay_front", modLoc(tBase + "/overlay_front/side"))
                // issue #8 (task world-tint-render-type): the six 0.01 controller decals
                // are transparent-texel overlay shells — cutout discards them off the body
                // (the D2 leg6 fix shape).
                .renderType("cutout");
        // element 0 — the body cube (tintindex 0 = the material tint, task
        // c2-controller-tint: the grayscale turbine_mains colored groups multiply the
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
     * Task placeables — the placeables band, half one: the Greg o'Lantern blockstate
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
        // the four placed piles (task placeables, half two): fixed silhouettes over the
        // borrowed upstream icon pairs (assets/README.md); tintindex 0 on the five
        // material-tinted faces (GT6PlaceableTint reads the BE material). The rock and
        // stick piles left this band for the surface-variant weighted bands (issue #47 +
        // follow-up, the two variant builders below).
        placedPile(gregtech6.registry.GT6Placeables.PLACED_INGOT.get(), "placed_ingot", "block/placeable/ingot_sides", "block/placeable/ingot_top", 2, true, false);
        placedPile(gregtech6.registry.GT6Placeables.PLACED_PLATE.get(), "placed_plate", "block/placeable/plate_sides", "block/placeable/plate_top", 1, true, false);
        placedPile(gregtech6.registry.GT6Placeables.PLACED_GEM_PLATE.get(), "placed_gem_plate", "block/placeable/plate_gem_sides", "block/placeable/plate_gem_top", 1, true, false);
        placedPile(gregtech6.registry.GT6Placeables.PLACED_SCRAP.get(), "placed_scrap", "block/placeable/scrap_sides", "block/placeable/scrap_top", 1, true, false);
        // issue #47: the sneak-placed rock pile (shift+rockGt, GT6PlaceablePlacement) rides
        // the SAME weighted pebble band as the worldgen surface rocks — upstream RockPlaced
        // 32074 EXTENDS the worldgen rock (placeables/MultiTileEntityRockPlaced over
        // misc/MultiTileEntityRock), so the placed form IS the surface-rock look. The old
        // full-footprint 16x16x3 slab was the wrong model AND the wrong face; the shared
        // models carry vanilla stone + tintindex 0, so GT6PlaceableTint keeps the material
        // seat (stone rockGt renders exactly like surface_rock_stone, flint falls to -1).
        getVariantBuilder(gregtech6.registry.GT6Placeables.PLACED_ROCK.get()).forAllStates(aState ->
                java.util.Arrays.stream(gregtech6.block.surface.GT6SurfaceVariants.Rock.values())
                        .map(tVariant -> new ConfiguredModel(models().getBuilder(tVariant.model()), 0, 0, false, tVariant.weight()))
                        .toArray(ConfiguredModel[]::new));
        // issue #47 follow-up: the sneak-placed stick (shift+stickGt / vanilla stick,
        // GT6PlaceablePlacement) rides the SAME weighted variant band as the worldgen
        // surface stick — upstream StickPlaced 32073 EXTENDS the worldgen stick
        // (placeables/MultiTileEntityStickPlaced over placeables/MultiTileEntityStick,
        // only getDefaultStick overridden), so the placed form IS the surface-stick look
        // (the random 12x2x2 lying bar, readFromNBT2 :56-70). The floor pose = the
        // worldgen facing=down band verbatim: x 0, y = the variant arm, the shared
        // models carry the untinted vanilla oak-log borrow (no tintindex — same as the
        // old placedPile call's aTinted=false). The old full-footprint 16x16x2 plank is
        // retired by the datagen stale sweep.
        getVariantBuilder(gregtech6.registry.GT6Placeables.PLACED_STICK.get()).forAllStates(aState ->
                java.util.Arrays.stream(gregtech6.block.surface.GT6SurfaceVariants.Stick.values())
                        .map(tVariant -> new ConfiguredModel(models().getBuilder(tVariant.model()), 0, tVariant.armY(), false, tVariant.weight()))
                        .toArray(ConfiguredModel[]::new));
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
     * Task issue7-kitchen-models — the kitchen family (GT6Kitchen.java:89-113), the
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
     * task c3-kitchen-tint-shape the reservation is WIRED — the baked
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

    /**
     * The Measuring Pot (task issue45-c3, issue #45): the upstream render-pass geometry
     * (MultiTileEntityMeasuringPot.setBlockBounds2 :103-108 verbatim) — the base slab
     * (5,0,5)-(11,1,11) plus the four 1px walls y 1..8 at the 4px inset ring — over the
     * TWO-LAYER colored+overlay grammar (getTexture2 :124-134 = BlockTextureMulti(colored,
     * overlay) per face): the body elements carry the colored band, each duplicated by a
     * 0.01-inflated overlay shell (the addHive/addBumbliary 0.01-plate form, cutout).
     * Face mapping rides the upstream world-side table: up=top, down=bottom, the wall's
     * outward face=sides, the cavity face=insides, the end caps=sides. The colored band
     * ships UN-TINTED — the #40-41 crucible bodyTexture declared deviation (upstream
     * tints it with mRGBa; {@code ponytail:} a tintindex-0 + dispatch row lands it
     * without model change when the render pool gets to it).
     */
    private void addMeasuringPot() {
        Block tBlock = gregtech6.registry.GT6MeasuringPot.MEASURING_POT.get();
        BlockModelBuilder tModel = models().getBuilder("measuring_pot")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("sides", modLoc("block/measuring_pot_colored_sides"))
                .texture("insides", modLoc("block/measuring_pot_colored_insides"))
                .texture("top", modLoc("block/measuring_pot_colored_top"))
                .texture("bottom", modLoc("block/measuring_pot_colored_bottom"))
                .texture("overlay_sides", modLoc("block/measuring_pot_overlay_sides"))
                .texture("overlay_insides", modLoc("block/measuring_pot_overlay_insides"))
                .texture("overlay_top", modLoc("block/measuring_pot_overlay_top"))
                .texture("overlay_bottom", modLoc("block/measuring_pot_overlay_bottom"))
                .texture("particle", "#sides")
                .renderType("cutout");
        // the base slab — upstream pass 4 (:107); no outward face (the walls ring it)
        potElement(tModel, 5.0F, 0.0F, 5.0F, 11.0F, 1.0F, 11.0F, null);
        // the four 1px walls — upstream passes 0-3 (:103-106)
        potElement(tModel,  4.0F, 1.0F,  5.0F,  5.0F, 8.0F, 11.0F, Direction.WEST);
        potElement(tModel, 11.0F, 1.0F,  5.0F, 12.0F, 8.0F, 11.0F, Direction.EAST);
        potElement(tModel,  5.0F, 1.0F,  4.0F, 11.0F, 8.0F,  5.0F, Direction.NORTH);
        potElement(tModel,  5.0F, 1.0F, 11.0F, 11.0F, 8.0F, 12.0F, Direction.SOUTH);
        simpleBlock(tBlock, tModel);
        itemModels().withExistingParent("measuring_pot", tModel.getLocation());
    }

    /** One pot element (body box + the 0.01-inflated overlay shell): aOutward null = the base slab (every side face = sides). */
    private void potElement(BlockModelBuilder aModel, float aMinX, float aMinY, float aMinZ,
            float aMaxX, float aMaxY, float aMaxZ, Direction aOutward) {
        potBox(aModel, aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ, aOutward, "");
        potBox(aModel, aMinX - 0.01F, aMinY - 0.01F, aMinZ - 0.01F,
                aMaxX + 0.01F, aMaxY + 0.01F, aMaxZ + 0.01F, aOutward, "overlay_");
    }

    /** One box: all six faces, the kitchen panel mapping (up/down/outward/cavity/caps) over the given band prefix, no tint. */
    private void potBox(BlockModelBuilder aModel, float aMinX, float aMinY, float aMinZ,
            float aMaxX, float aMaxY, float aMaxZ, Direction aOutward, String aBand) {
        BlockModelBuilder.ElementBuilder tElement = aModel.element().from(aMinX, aMinY, aMinZ).to(aMaxX, aMaxY, aMaxZ);
        for (Direction tDir : Direction.values()) {
            String tFace;
            if (tDir == Direction.UP) tFace = "top";
            else if (tDir == Direction.DOWN) tFace = "bottom";
            // the cavity face (the one inward-facing side per wall, upstream :126-129); the base
            // slab (aOutward null) and every other wall face tile sides (outer + end caps)
            else tFace = aOutward != null && tDir == aOutward.getOpposite() ? "insides" : "sides";
            tElement.face(tDir).texture("#" + aBand + tFace).end();
        }
        tElement.end();
    }
}
