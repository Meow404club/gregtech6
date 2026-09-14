package gregtech6.tileentity.multiblocks;

import org.junit.jupiter.api.Test;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6LargeMachines.GTLargeMachineBlockEntity;

class CoagDebugTest extends GTMultiBlocksOfflineTestBase {
    @Test
    void coagulatorRealMapShape() {
        java.util.HashSet<Recipe> s = new java.util.HashSet<>();
        RecipeMap m = new RecipeMap(s, "gt6.test.coagdebug", "Coag Debug", null,
                0, 1, "gt6:textures/gui/machines/Oven", 0, 1, 0, 1, 0, 1, 0, 1);
        m.addRecipe(new Recipe(true, new ItemStack[0],
                new ItemStack[] {new ItemStack(Items.SNOWBALL, 1)},
                new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, new FluidStack[0], 64, 1, 0));
        gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
        @SuppressWarnings("unchecked")
        BlockEntityType<GTLargeMachineBlockEntity>[] holder = (BlockEntityType<GTLargeMachineBlockEntity>[]) new BlockEntityType<?>[1];
        holder[0] = BlockEntityType.Builder.of((aPos, aState) -> new GTLargeMachineBlockEntity(holder[0], aPos, aState, null), Blocks.BRICKS).build(null);
        GTLargeMachineBlockEntity be = new GTLargeMachineBlockEntity(holder[0], new BlockPos(9, 64, 9), Blocks.BRICKS.defaultBlockState(), null);
        be.mRecipes = m;
        be.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(1L, 1L, 16L, 64, false, false));
        be.mRequiresIgnition = false; // the live row config
        be.mInventory.getStackInSlot(0);
        be.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        int r = be.checkRecipe(true, false);
        System.out.println("COAGDEBUG result=" + r + " bar=" + be.mMaxProgress + " tank=" + be.mTanksInput[0].amount());
    }
}
