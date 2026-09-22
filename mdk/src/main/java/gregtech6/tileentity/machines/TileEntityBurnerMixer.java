package gregtech6.tileentity.machines;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.recipes.RecipeMap;

/**
 * The Burner Mixer family BET (task p34-machines-burner-plantalyzer) — the single-block
 * {@link TileEntityBasicMachine} with the NBT_NEEDS_IGNITION half the shared class cut,
 * revived in the FAMILY-SCOPED form the p32-ignition-gate ruling established on the
 * multiblock base ({@code TileEntityBase10MultiBlockMachine#ignite()} = the upstream
 * TOOL_igniter branch, MultiTileEntityBasicMachine.java:373-379) and the mIgnited_ruling
 * (the same field carries BOTH the ignition gate AND the post-action re-check window;
 * {@code TileEntityOven} is the mRequiresIgnition = false side and stays untouched).
 *
 * <p><b>Zero shared-class diff:</b> everything rides the subclass —
 * <ul>
 * <li>the gate :724/:737 ({@code aApplyRecipe = (!mRequiresIgnition || mIgnited > 0 ||
 *     mActive)}) folds into the {@link #checkRecipe} entry: a cold machine probes only
 *     (FOUND_AND_COULD_HAVE_USED_RECIPE, no consume), an ignited or already-running one
 *     consumes as normal — the fold placement is value-equivalent to the upstream
 *     mid-body position because every return code between the entry and the consume is
 *     aApplyRecipe-independent (findRecipe miss → DID_NOT_FIND_RECIPE either way, the
 *     canOutput blockage → FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS either way);</li>
 * <li>the keep-alive halves are already live in the base (mIgnited = 40 at :816/:822/:831/:851
 *     — the four output-placement sites — and the :792 decrement), so a RUNNING burner
 *     stays lit without re-ignition, exactly the upstream "burning while active" face;</li>
 * <li>{@link #ignite()} is the command-chain entry (the GTMultiBlockCommand {@code ignite}
 *     precedent — the port has no igniter item seam, GTBurnerCommand.java:44-46).</li>
 * </ul>
 */
public class TileEntityBurnerMixer extends TileEntityBasicMachine {

	/** Upstream NBT_NEEDS_IGNITION T (Loader_MultiTileEntities.java:1595-1598) — a registration constant, never persisted (the upstream :121 read only fires when the key exists). */
	private final boolean mRequiresIgnition = true;

	public TileEntityBurnerMixer(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState,
			RecipeMap aRecipes, int aParallel, boolean aParallelDuration, @Nullable Supplier<MenuType<GTBasicMachineMenu>> aMenuType) {
		super(aType, aPos, aState, aRecipes, aParallel, aParallelDuration, aMenuType);
	}

	/** The TOOL_igniter branch :373-379 verbatim (the multiblock base ignite() shape). */
	public void ignite() {
		if (mRequiresIgnition) mIgnited = 40;
	}

	/** The registration-constant read (the offline pin seam). */
	public boolean requiresIgnition() {
		return mRequiresIgnition;
	}

	@Override
	public int checkRecipe(boolean aApplyRecipe, boolean aUseAutoIO) {
		// :724/:737 — the ignition half the single-block cut keeps, folded at the entry
		// (the value-equivalence argument lives in the class doc)
		return super.checkRecipe(aApplyRecipe && (!mRequiresIgnition || mIgnited > 0 || mActive), aUseAutoIO);
	}
}
