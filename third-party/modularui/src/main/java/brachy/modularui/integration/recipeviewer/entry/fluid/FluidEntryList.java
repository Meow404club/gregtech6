package brachy.modularui.integration.recipeviewer.entry.fluid;

import brachy.modularui.integration.recipeviewer.entry.EntryList;

//? if neoforge {
import net.neoforged.neoforge.fluids.FluidStack;
//?} else {
/*import net.minecraftforge.fluids.FluidStack;
*///?}

import java.util.List;

public sealed interface FluidEntryList extends EntryList<FluidStack> permits FluidStackList, FluidTagList, FluidHolderSetList {

    List<FluidStack> getStacks();

    boolean isEmpty();

    @Override
    default Class<FluidStack> getType() {
        return FluidStack.class;
    }
}
