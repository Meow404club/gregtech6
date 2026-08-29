package gregtech6.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import gregtech6.block.GTOvenBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTMachines;

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
        addOven();
    }

    /**
     * Task p4-machine-oven (W2-exclusive provider addition): the A-tier Oven rendering — a
     * pure datagen blockstate over 6 facing x 2 active x 2 running = 24 variants. Three
     * models (inactive/active/running), each a {@code cube} with the four-texture key set
     * (top/bottom/side/front, spec 8): the front texture is the state carrier, mirroring the
     * upstream getTexture2 overlay pick (MultiTileEntityBasicMachine.java:1014, mActive →
     * mTexturesActive : mRunning → mTexturesRunning : mTexturesInactive); the y rotation maps
     * the FACING property (model-space north = front). Textures are script-generated
     * placeholder PNGs, not JSON.
     */
    private void addOven() {
        Block tOven = GTMachines.OVEN.get();
        ModelFile tInactive = ovenModel("oven", "oven_front");
        ModelFile tActive = ovenModel("oven_active", "oven_front_active");
        ModelFile tRunning = ovenModel("oven_running", "oven_front_running");
        getVariantBuilder(tOven).forAllStates(aState -> {
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
        itemModels().withExistingParent("oven", modLoc("block/oven"));
    }

    /** One cube model over the four-texture key set: down/up/north(front)/south+east+west(side). */
    private ModelFile ovenModel(String aName, String aFrontTexture) {
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
}
