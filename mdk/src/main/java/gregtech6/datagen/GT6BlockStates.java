package gregtech6.datagen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTOvenBlock;
import gregtech6.block.energy.GTAxleBlock;
import gregtech6.block.energy.GTDieselEngineBlock;
import gregtech6.block.energy.GTTransformerRotationBlock;
import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.registry.GTBarrels;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GTEnergySources;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GT6Attachments;
import gregtech6.registry.GT6Kinetics;
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
        addMultiBlocks();
        addBarrel();
        addEnergySource();
        addPrefixBlocks(); // task p8-prefixblock-render ①
        addCrank(); // task p12-engine-crank
        addAxles(); // task p12-axle-family
        addAttachments(); // task p12-tap-funnel-attachment
        addSteamEngines(); // task p12-engine-steam
        addDieselEngines(); // task p12-engine-diesel
        addGearBoxTransformer(); // task p12-gearbox-transformer
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
        simpleBlock(tBricks, models().cubeAll("multiblock_coke_oven_bricks", modLoc("block/multiblock_coke_oven_bricks")));
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
     * metal) each as a cube_all placeholder over its
     * {@code gt6:textures/block/barrel_<material>.png} (the barrel TESR/lid is a
     * feature-layer omission, MultiTileEntityBarrelWood.java:44-54) plus the BlockItem
     * model parenting the block model. The textures are script-generated placeholder
     * PNGs, not JSON.
     *
     * <p>Task p7-barrel-high-tier-melt-bridge spec ④: the twelve high-tier metal drums
     * (Loader_MultiTileEntities.java:2159-2170) share the ONE {@code barrel_metal.png} —
     * every model JSON references the same PNG, so the model count grows with the rows
     * and the PNG count does not.
     */
    private void addBarrel() {
        addBarrel(GTBarrels.BARREL.get());
        addBarrel(GTBarrels.BARREL_PLASTIC.get());
        addBarrel(GTBarrels.BARREL_METAL.get());
        addBarrel(GTBarrels.BARREL_LOGISTICS.get()); // task p12-barrel-keepfilter-logistics — the :2171 row, own PNG
        for (RegistryObject<GTBarrelBlock> tDrum : GTBarrels.METAL_DRUM_BLOCKS.values())
            addBarrel(tDrum.get(), "barrel_metal");
    }

    /** One cube_all barrel + its BlockItem parent (the p4 wood barrel shape, reused per material row). */
    private void addBarrel(Block aBarrel) {
        addBarrel(aBarrel, aBarrel.getDescriptionId().replace("block.gt6.", ""));
    }

    /** The same shape over an explicit texture tail (the p7 shared-PNG drum family form). */
    private void addBarrel(Block aBarrel, String aTexture) {
        String tName = aBarrel.getDescriptionId().replace("block.gt6.", "");
        simpleBlock(aBarrel, models().cubeAll(tName, modLoc("block/" + aTexture)));
        itemModels().withExistingParent(tName, modLoc("block/" + tName));
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
    }

    /**
     * The addOven generalization (task p7-basicmachine-family ④): one {@code aBase} machine
     * = three models ({@code aBase}, {@code aBase_active}, {@code aBase_running}) with the
     * front textures {@code aBase_front}/{@code _active}/{@code _running} and the shared
     * oven body textures (the in-scope placeholder set is the per-machine fronts only), the
     * 16-variant blockstate, and the BlockItem model parenting the block model. The oven
     * output is byte-identical to the pre-generalization shape (base "oven").
     *
     * <p>Property interning: both GTOvenBlock.FACING and GTBasicMachineBlock.FACING are the
     * BlockStateProperties.HORIZONTAL_FACING instance, and GTOvenBlock.ACTIVE/RUNNING and
     * GTBasicMachineBlock.ACTIVE/RUNNING are the BooleanProperty.create("active"/"running")
     * interned instances (BooleanProperty.java BY_NAME cache) — so the GTOvenBlock property
     * reads below cover the machine blocks too.
     */
    private void addMachine(Block aBlock, String aBase) {
        addMachine(aBlock, aBase, aBase);
    }

    /**
     * The texture-base overload (task p8-machine-tiers-doinject ⑧): the model names derive
     * from {@code aBase} (so shredder_t2 gets shredder_t2/_active/_running models + its own
     * 16-variant blockstate + the item parent) while the FRONT TEXTURES stay on the family's
     * T1 set ({@code aTextureBase_front*}) — the tier is not a visual state upstream (the
     * rows :1294-1309 share the NBT_TEXTURE per family), so the ladder adds zero PNGs.
     */
    private void addMachine(Block aBlock, String aBase, String aTextureBase) {
        ModelFile tInactive = machineModel(aBase, aTextureBase + "_front");
        ModelFile tActive = machineModel(aBase + "_active", aTextureBase + "_front_active");
        ModelFile tRunning = machineModel(aBase + "_running", aTextureBase + "_front_running");
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

    /** One cube model over the four-texture key set: down/up/north(front)/south+east+west(side). */
    private ModelFile machineModel(String aName, String aFrontTexture) {
        return models().cube(aName,
                modLoc("block/oven_bottom"), modLoc("block/oven_top"),
                modLoc("block/" + aFrontTexture), modLoc("block/oven_side"),
                modLoc("block/oven_side"), modLoc("block/oven_side"));
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
        for (RegistryObject<GTAxleBlock> tAxle : GT6Kinetics.AXLE_BLOCKS.values()) {
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
        for (RegistryObject<GTDieselEngineBlock> tBlock : GT6Kinetics.DIESEL_BLOCKS.values()) {
            simpleBlock(tBlock.get(), models().cubeAll("diesel_engine", modLoc("block/diesel_engine")));
            itemModels().withExistingParent(tBlock.getId().getPath(), modLoc("block/diesel_engine"));
        }
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
    private ModelFile tintedCubeAll(String aName, ResourceLocation aTexture) {
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
}
