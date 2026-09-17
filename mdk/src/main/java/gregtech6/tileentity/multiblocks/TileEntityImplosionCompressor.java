package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

//? if forge {
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;

/**
 * The Implosion Compressor multiblock controller (task p31-implosion) — the 1.20.1 port
 * of gregtech/tileentity/multiblocks/MultiTileEntityImplosionCompressor.java over
 * {@link TileEntityBase10MultiBlockMachine} (the twelfth-plus-one machine: the same
 * Loader block as the W3 large-12 family, registered separately upstream at
 * Loader_MultiTileEntities.java:1228 — one row before the :1229-1240 twelve).
 *
 * <p><b>The :1228 registration columns</b> (registration config, never persisted — the
 * loadKeepsTheConstructorInjectedConfig contract):
 * <ul>
 * <li>meta 17110, TungstenSteel, hardness 12.5 == resistance 12.5 (the GTMultiBlocks
 *     block row);</li>
 * <li>energy TU, NBT_INPUT 1 / MIN 1 / MAX 16 (the base defaults already ARE the 1..16
 *     window — only the parallel needs a write), NBT_PARALLEL 64;</li>
 * <li>NBT_NO_CONSTANT_POWER T (the base default {@code mNoConstantEnergy = true}); no
 *     NBT_NEEDS_IGNITION key — {@code mRequiresIgnition = false} (the twelve-row
 *     reading, GT6LargeMachineBlockEntity ctor comment);</li>
 * <li>RM.ImplosionCompressor → {@link GT6RecipeMaps#IMPLOSION} (the lazy-override
 *     shape); auto-out SIDE_BOTTOM both item and tank (the item face is the gated
 *     slot surface this port carries; the fluid face below).</li>
 * </ul>
 *
 * <p><b>The structure</b> (upstream checkStructure2 :46-60): a 3x3x3 hollow of 26 Dense
 * Tungstensteel Wall (18023) cells with an AIR centre, the shell centred one cell in
 * front of the facing and one above the controller ({@code tX = getOffsetXN(mFacing),
 * tY = yCoord + 1, tZ = getOffsetZN(mFacing)}). IDENTICAL geometry to the upstream
 * Autoclave (:46-60 — the two classes differ only in the wall id 18022 vs 18023 and the
 * tooltips), so the pattern declaration is the Coke-Oven centre-relative form with the
 * +1 Y bake: cells (i, j+1, k) walked at {@code patternWalkFacing() == mFacing} (the
 * default), the (0, +1, 0) centre declared hollow AIR (fail-not-clear, the
 * TileEntityCokeOven.java:126 form). Every wall cell carries the FULL forming
 * expectation: part block dense_wall_tungstensteel, design 0, usage
 * {@link MultiBlockPartBlockEntity#ONLY_ITEM_FLUID_ENERGY} — the triple the upstream
 * :54 checkAndSetTarget call passed.
 *
 * <p><b>The process face</b>: onProcessStarted replays the upstream :62
 * {@code UT.Sounds.send(SFX.MC_EXPLODE, ...)} (the GT6AnvilBlockEntity SFX annotation
 * form); the output push targets the cell BELOW (upstream :84-91, both item and tank
 * auto-out SIDE_BOTTOM — the fluid half is the {@link #getFluidOutputTarget} override,
 * the item half rides the gated output slots this port keeps).
 *
 * <p>GUI face: none — the controller block carries no use-face (the W2 menu-null form,
 * the twelve large machines' shape); the recipe walk is RCON/data-visible through the
 * block NBT like every headless machine.
 */
public class TileEntityImplosionCompressor extends TileEntityBase10MultiBlockMachine {

	/** The declared structure pattern (lazy — {@link #getPartBlock()} is stable per instance). */
	@Nullable
	private GTMultiBlockPattern mStructurePattern = null;

	/** The registry-path constructor (the BlockEntityType.Builder.of factory form, the oven precedent). */
	public TileEntityImplosionCompressor(BlockPos aPos, BlockState aState) {
		this(GTMultiBlocks.IMPLOSION_COMPRESSOR_BE.get(), aPos, aState);
	}

	/** The test seam: offline fixtures build their own BET (the frozen registry keeps .get() out of reach). */
	public TileEntityImplosionCompressor(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
		// the :1228 row — NBT_PARALLEL 64 (the base default is the Coke Oven 16), and no
		// NBT_NEEDS_IGNITION key (the twelve-row reading: the machines start on their own)
		mParallel = 64;
		mRequiresIgnition = false;
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_implosion_compressor";
	}

	/**
	 * The wall block this structure is built from (upstream MTE id 18023, Dense Tungstensteel
	 * Wall, Loader :1162 — the GTMultiBlocks WALL_ROWS row). A hook so the offline tests can
	 * bind a fixture block without the frozen-registry dance (the TileEntityCokeOven form).
	 */
	protected Block getPartBlock() {
		return GTMultiBlocks.anyPartBlock("dense_wall_tungstensteel");
	}

	/**
	 * The lazy recipe map (the base's COKE_OVEN default overridden — the
	 * GTLargeMachineBlockEntity :641-648 form).
	 */
	@Override
	public RecipeMap recipes() {
		RecipeMap tMap = mRecipes;
		if (tMap == null) {
			tMap = GT6RecipeMaps.IMPLOSION;
			mRecipes = tMap;
		}
		return tMap;
	}

	/**
	 * The declared structure pattern — the 26 dense-wall cells + the hollow air centre,
	 * the shell centre-relative cells (i, j+1, k) so the walk lands the hollow at
	 * controller + front + up (see the class doc). Declared in the upstream :50-55 loop
	 * order ({@code i} outer / {@code j} middle / {@code k} inner).
	 */
	@Override
	@Nullable
	public GTMultiBlockPattern getStructurePattern() {
		if (mStructurePattern == null) {
			GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
			for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
				if (i == 0 && j == 0 && k == 0) continue; // the centre — declared hollow below (the :52 arm)
				tBuilder.formingPart(i, j + 1, k, getPartBlock(), MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0);
			}
			tBuilder.hollow(0, 1, 0, GTMultiBlockPattern.AIR); // the :52 centre — tY = yCoord + 1
			mStructurePattern = tBuilder.build();
		}
		return mStructurePattern;
	}

	/**
	 * Upstream :46-60, walked from the pattern (the Coke Oven :144-150 form): the shared
	 * checker resolves every cell through cellOffset, drives the checkAndSetTarget path
	 * for the 26 walls and judges the hollow centre fail-not-clear; an unloaded probe
	 * keeps the last verdict (:59 {@code return mStructureOkay}).
	 */
	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay; // :133
		GTMultiBlockStructureChecker.FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(
				this, mFacing, aCoordinates, aPlayer, aInventory);
		if (tVerdict.unloaded) return mStructureOkay; // :59
		return tVerdict.formed;
	}

	/** Upstream :78-81 — the 3x3 shell box one front of the facing, controller Y..Y+2. */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		int tX = getOffsetXN(mFacing), tY = getOffsetYN(mFacing), tZ = getOffsetZN(mFacing);
		return aX >= tX - 1 && aY >= tY && aZ >= tZ - 1 && aX <= tX + 1 && aY <= tY + 2 && aZ <= tZ + 1;
	}

	/** Upstream :62 — the process-start explosion sound (the SFX.MC_EXPLODE replay). */
	@Override
	public void onProcessStarted() {
		if (hasLevel() && !isClientSide()) {
			getLevel().playSound(null, getBlockPos(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		super.onProcessStarted();
	}

	/**
	 * The map-aware canOutput (the GTLargeMachineBlockEntity :789-833 override — the base
	 * copy walks the outputs from the FIXED slot 1, the Coke-Oven one-input shape; this
	 * map carries THREE input slots, so the output slots start at
	 * {@code mInputItemsCount}). The :626-629 chain cap and the stack-cap arithmetic are
	 * the base body verbatim.
	 */
	@Override
	public int canOutput(Recipe aRecipe) {
		int rMaxTimes = (int) mParallel; // :621 — the :1228 NBT_PARALLEL 64

		for (int i = 0, j = recipes().mInputItemsCount; i < recipes().mOutputItemsCount && i < aRecipe.mOutputs.length; i++, j++) {
			ItemStack tOutput = aRecipe.mOutputs[i];
			if (tOutput == null || tOutput.isEmpty()) continue;
			ItemStack tSlot = slot(j);
			if (tSlot != null && !tSlot.isEmpty()) {
				if (aRecipe.mNeedsEmptyOutput) return 0; // :633-636
				//? if forge {
				if (!ItemStack.isSameItemSameTags(tSlot, tOutput)) {mOutputBlocked++; return 0;} // :637-640
				//?}
				//? if neoforge {
				/*if (!ItemStack.isSameItemSameComponents(tSlot, tOutput)) {mOutputBlocked++; return 0;} // 21.1
				*///?}
				rMaxTimes = Math.min(rMaxTimes, (tSlot.getMaxStackSize() - tSlot.getCount()) / tOutput.getCount()); // :641
				if (rMaxTimes <= 0) {mOutputBlocked++; return 0;} // :642-645
			} else {
				rMaxTimes = Math.min(rMaxTimes, Math.max(1, 64 / tOutput.getCount())); // :647
			}
		}
		if (aRecipe.mFluidOutputs.length > 0) { // :650-666 — dormant (the map carries no fluid outputs)
			for (FluidTankGT tTank : mTanksOutput) if (tTank.isEmpty()) return rMaxTimes;
			return 0;
		}
		return rMaxTimes; // :667
	}

	/**
	 * Upstream :84-86 — the fluid output push targets the cell BELOW
	 * ({@code getAdjacentTank(SIDE_BOTTOM)}), queried at its top face (the
	 * GTLargeMachineBlockEntity below-cell form). Dormant in practice: the map carries
	 * zero fluid-output slots — the face exists for shape fidelity.
	 */
	@Override
	@Nullable
	protected IFluidHandler getFluidOutputTarget(Fluid aOutput) {
		if (!hasLevel() || isClientSide()) return null;
		net.minecraft.world.level.block.entity.BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().below());
		if (tNeighbor == null) return null;
		//? if forge {
		return tNeighbor.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, net.minecraft.core.Direction.UP).resolve().orElse(null);
		//?} else {
		/*// 21.1: the query goes through the level (the TileEntityCokeOven seam form).
		return getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, getBlockPos().below(), net.minecraft.core.Direction.UP);
		*///?}
	}
}
