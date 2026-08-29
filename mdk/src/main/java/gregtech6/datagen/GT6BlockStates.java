package gregtech6.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import gregtech6.registry.GTBlockEntities;

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
    }
}
