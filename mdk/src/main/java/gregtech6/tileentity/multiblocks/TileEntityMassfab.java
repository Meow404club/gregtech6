package gregtech6.tileentity.multiblocks;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.fluid.FluidTankGT;

/**
 * The Large Matter Fabricator multiblock controller (task p31-massfab) — the 1.20.1/1.21.1
 * port of gregtech/tileentity/multiblocks/MultiTileEntityMatterFabricator.java over
 * {@link TileEntityBase10MultiBlockMachine} (Loader_MultiTileEntities.java:1241: meta 17199,
 * item 17101, "Large Matter Fabricator", MT.Pb, NBT_HARDNESS 6.0F == NBT_RESISTANCE 6.0F,
 * NBT_TEXTURE "largemassfab").
 *
 * <p><b>The :1241 registration columns</b> (registration config, never persisted — the
 * loadKeepsTheConstructorInjectedConfig contract): NBT_INPUT 1 / NBT_INPUT_MIN 1 /
 * NBT_INPUT_MAX 2097152 (the explicit-override form, {@link #applyEnergyRowSpec}),
 * NBT_ENERGY_ACCEPTED TD.Energy.QU, NBT_RECIPEMAP RM.Massfab → {@link
 * GT6RecipeMaps#MASSFAB}, NBT_INV_SIDE_AUTO_OUT + NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM,
 * NBT_CHEAP_OVERCLOCKING T (the :773 overclock loop refuses the 4x/2x fold),
 * NBT_PARALLEL 64 + NBT_PARALLEL_DURATION T (so the :743 parallel-by-input bind is
 * SKIPPED — the parallel rides {@link #canOutput} alone), NBT_NO_CONSTANT_POWER T.
 * No NBT_NEEDS_IGNITION key → {@code mRequiresIgnition = false} (the twelve-row
 * reading, the implosion form). No NBT_EFFICIENCY key → the :96 10000 identity.
 *
 * <p><b>The structure</b> (upstream checkStructure2 :45-224, the :227 tooltip's own
 * arithmetic): the controller is the bottom-centre cell of a 5x5x5 ("Main Block centered
 * on Side-Bottom and facing outwards", :233) — the structure core sits 2 cells BEHIND the
 * front face ({@code tX = getOffsetXN(mFacing, 2) - 2}, WorldAndCoords.java:60 {@code
 * mX - OFFX[aSide]*n}; the port anchor {@link GTMultiBlockPattern#anchorOffset} doubled),
 * five full layers dy 0..4 plus the dy 5 top:
 * <ul>
 * <li>dy 0 and dy 4: full 5x5 Dense Lead Walls (18031, {@link
 *     MultiBlockPartBlockEntity#ONLY_ITEM_FLUID_ENERGY} — "Stuff can go in and out on any
 *     of the Dense Lead Walls", :234);</li>
 * <li>dy 1..3: the 16-wall ring + the inner 3x3 of Large Osmium Coils (18044, same
 *     usage), except the exact centre (dy 2, dx 2, dz 2) which must be AIR (upstream
 *     :114, fail-not-clear — the checker hollow semantics, nothing is removed);</li>
 * <li>dy 5: the 5x5 ring of 16 Ventilation Units (18299, NOTHING, :180-195), the centre
 *     Versatile Quadcore Processor Unit (18200, NOTHING, :197), and the 8 remaining
 *     inner cells each counted as Control (18202) OR Conversion (18204) — the
 *     {@code tCountA >= 4 && tCountB >= 4} quota (:199-219).</li>
 * </ul>
 * Totals: 98 walls + 26 coils + 1 air + 16 vents + 1 versatile + 8 PU cells = 150 cells.
 * (The upstream :227 tooltip says "97 Dense Lead Walls" — its own miscount; the checks
 * are the :50-178 lines verbatim and they walk 98.)
 *
 * <p><b>Why hand-written (no bound pattern)</b>: the 8 PU cells are an N-of-M quota (≥4
 * Control <i>and</i> ≥4 Conversion over the same cells), which the declarative pattern
 * API deliberately does not express (GTMultiBlockPattern class doc, quota case ②) —
 * the seam for quotas is the hand-written walk, so {@link #getStructurePattern()} stays
 * the interface default null (the LightningRod census class). This walk is the
 * upstream-verbatim loop via {@link ITileEntityMultiBlockController.Util#checkAndSetTarget}
 * (the wand auto-place, the occupation arbitration and the controller self-cell pass are
 * inherited, not re-coded); the unloaded guard is the checker's per-cell superset of the
 * upstream :47 four-corner {@code blockExists} probe (keep the last verdict).
 *
 * <p><b>The output face</b> (:257-265): auto-out SIDE_BOTTOM both halves — the fluid half
 * is the {@link #getFluidOutputTarget} override (the implosion below-cell form), the item
 * half rides the gated output slots this port keeps. The input targets (:267-268) return
 * null — folded away with the auto-IO surface.
 *
 * <p><b>The energy face</b> (MultiTileEntityBasicMachine.java:489-517): a QU capacitor
 * sink with the upstream window-capped {@link #doInject} arithmetic; the input sizes are
 * the :513-515 window trio. The wall→controller energy relay of the upstream multiblock
 * parts is NOT ported (MultiBlockPartBlockEntity relays the three forge capabilities
 * only; the ITileEntityEnergy routing is the declared energy-domain seam, the Graagg
 * handoff ledger) — the face exists for the energy-domain card, and the ops/RCON drive
 * is the persisted {@code energy} NBT (the base NBT_ENERGY key, the Graagg gt.energy
 * precedent). Oversize packets: upstream :493-496 calls {@code overcharge} (the
 * explosion subsystem is the cut ADR-D1 pool) — the port REFUSES the packet (returns 0,
 * nothing injected): the declared no-explosion narrowing.
 *
 * <p><b>The controller crafting row</b> "FFF"/"FMF"/"FFF" ('M' = the 18031 Dense Lead
 * Wall, 'F' = IL.FIELD_GENERATORS[5], Loader:1241 tail) is CUT — the 'F' item family has
 * no port identity (the W3 absent-input pool, the implosion/graagg CUT precedent); the
 * ledger lives on the GTMultiBlocks registration comment.
 *
 * <p>GUI face: none — the controller block carries no use-face (the W2 menu-null form,
 * the implosion shape); the recipe walk is RCON/data-visible through the block NBT.
 */
public class TileEntityMassfab extends TileEntityBase10MultiBlockMachine implements ITileEntityEnergy {

	/** The registry-path constructor (the BlockEntityType.Builder.of factory form, the implosion precedent). */
	public TileEntityMassfab(BlockPos aPos, BlockState aState) {
		this(GTMultiBlocks.MASSFAB_BE.get(), aPos, aState);
	}

	/** The test seam: offline fixtures build their own BET (the frozen registry keeps .get() out of reach). */
	public TileEntityMassfab(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
		// the :1241 row — the explicit window 1/1/2097152 (:127-128 overrides after the
		// :126 derive), NBT_PARALLEL 64, NBT_CHEAP_OVERCLOCKING T, NBT_PARALLEL_DURATION T;
		// no NBT_NEEDS_IGNITION key (the twelve-row reading). Not persisted (the contract).
		applyEnergyRowSpec(new EnergyRowSpec(1L, 1L, 2097152L, 64, true, true));
		mRequiresIgnition = false;
		// the :1241 NBT_ENERGY_ACCEPTED column — the BASE field assigned (fields do not
		// virtual-dispatch; a subclass shadow would leave the base checkRecipe on TU)
		mEnergyTypeAccepted = TD.Energy.QU;
		// the :161 readFromNBT2 row — the output bank re-points to the map's fluid-OUT
		// count (RM.java:144 fluids 1/2/0 → TWO tanks; the p30-distill-output-routing
		// form): the disintegration rows park charged + neutral, and the base one-tank
		// default would fail the canOutput :664 required-empty-tank gate forever.
		mTanksOutput = new FluidTankGT[] {new FluidTankGT(), new FluidTankGT()};
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_massfab";
	}

	/** The wall block (upstream part id 18031, Loader :1162 family — Dense Lead Wall). */
	protected Block getWallBlock() {
		return GTMultiBlocks.anyPartBlock("dense_wall_lead");
	}

	/** The coil block (upstream part id 18044, Loader :1170 — Large Osmium Coil). */
	protected Block getCoilBlock() {
		return GTMultiBlocks.anyPartBlock("large_osmium_coil");
	}

	/** The vent block (upstream part id 18299 — Ventilation Unit). */
	protected Block getVentBlock() {
		return GTMultiBlocks.anyPartBlock("ventilation_unit");
	}

	/** The centre PU (upstream part id 18200 — Versatile Quadcore Processor Unit). */
	protected Block getVersatileBlock() {
		return GTMultiBlocks.anyPartBlock("processor_unit_versatile");
	}

	/** The quota PU A (upstream part id 18202 — Control Quadcore Processor Unit). */
	protected Block getControlBlock() {
		return GTMultiBlocks.anyPartBlock("processor_unit_control");
	}

	/** The quota PU B (upstream part id 18204 — Conversion Quadcore Processor Unit). */
	protected Block getConversionBlock() {
		return GTMultiBlocks.anyPartBlock("processor_unit_conversion");
	}

	/** The lazy recipe map (the base's COKE_OVEN default overridden — the implosion form). */
	@Override
	public RecipeMap recipes() {
		RecipeMap tMap = mRecipes;
		if (tMap == null) {
			tMap = GT6RecipeMaps.MASSFAB;
			mRecipes = tMap;
		}
		return tMap;
	}

	// ---------------------------------------------------------------------------
	// the structure (:45-224 verbatim, hand-written — the quota case, see class doc)
	// ---------------------------------------------------------------------------

	/**
	 * The structure-corner arithmetic (upstream :46): the corner sits 2 cells behind the
	 * front face minus one further on each axis — {@code getOffsetXN(mFacing, 2) - 2} =
	 * {@code x - 2*OFFX[facing] - 2} = {@code x + 2*anchorX - 2} (the anchor IS
	 * {@code -OFF}, GTMultiBlockPattern.anchorOffset). Pure so the tests drive it.
	 */
	static int corner(int aCoord, int aAnchor, int aHalfSpan) {
		return aCoord + 2 * aAnchor - aHalfSpan;
	}

	/** The dy 5 quota pair (upstream :199-219): each of the 8 inner cells counts Control and/or Conversion. */
	static boolean quotaMet(int aCountA, int aCountB) {
		return aCountA >= 4 && aCountB >= 4; // :219
	}

	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay; // :133
		Level tLevel = getLevel();
		BlockPos tPos = getBlockPos();
		int[] tAnchor = GTMultiBlockPattern.anchorOffset(mFacing); // == -OFF[facing]
		int tX = corner(tPos.getX(), tAnchor[0], 2), tY = tPos.getY(), tZ = corner(tPos.getZ(), tAnchor[2], 2);

		// the unloaded guard — the per-cell superset of the upstream :47 four-corner
		// blockExists probe (the checker form): one not-loaded cell keeps the last verdict.
		for (int dy = 0; dy <= 5; dy++) for (int dx = 0; dx <= 4; dx++) for (int dz = 0; dz <= 4; dz++) {
			if (!tLevel.isLoaded(new BlockPos(tX + dx, tY + dy, tZ + dz))) return mStructureOkay; // :47/:59/:223
		}

		boolean tSuccess = true; // :48
		Block tWall = getWallBlock(), tCoil = getCoilBlock(), tVent = getVentBlock();
		Block tVersatile = getVersatileBlock(), tControl = getControlBlock(), tConversion = getConversionBlock();
		int tCountA = 0, tCountB = 0; // :199

		for (int dy = 0; dy <= 5; dy++) for (int dx = 0; dx <= 4; dx++) for (int dz = 0; dz <= 4; dz++) {
			int tWX = tX + dx, tWY = tY + dy, tWZ = tZ + dz;
			boolean tEdge = dx == 0 || dx == 4 || dz == 0 || dz == 4;
			if (dy <= 4) {
				if (dy == 2 && dx == 2 && dz == 2) {
					// :114 — the centre must be air (fail-not-clear; the upstream
					// setBlockToAir arm is the idempotent no-op on the air case)
					if (!tLevel.getBlockState(new BlockPos(tWX, tWY, tWZ)).isAir()) tSuccess = false;
				} else if (tEdge || dy == 0 || dy == 4) {
					// :50-74/:76-100 ring/:102-126 ring/:128-152 ring/:154-178 — the 98 walls
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tWX, tWY, tWZ, tWall, 0,
							MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				} else {
					// :82-84/:87-89/:92-94/:108-110/:113-115(+air)/:118-120/:134-136/:139-141/:144-146 — the 26 coils
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tWX, tWY, tWZ, tCoil, 0,
							MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				}
			} else if (tEdge) {
				// :180-195 — the 16 vents (NOTHING)
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tWX, tWY, tWZ, tVent, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			} else if (dx == 2 && dz == 2) {
				// :197 — the centre Versatile PU (NOTHING)
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tWX, tWY, tWZ, tVersatile, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			} else {
				// :201-217 — the 8 quota cells: each counts Control (A) and/or Conversion (B)
				if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tWX, tWY, tWZ, tControl, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tCountA++;
				if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tWX, tWY, tWZ, tConversion, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tCountB++;
			}
		}
		if (!quotaMet(tCountA, tCountB)) tSuccess = false; // :219
		return tSuccess; // :221
	}

	/** Upstream :252-255 — the box over the 5x5 core, controller Y..Y+6, centred on the core axis. */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		BlockPos tPos = getBlockPos();
		int[] tAnchor = GTMultiBlockPattern.anchorOffset(mFacing);
		int tCX = tPos.getX() + 2 * tAnchor[0], tCY = tPos.getY(), tCZ = tPos.getZ() + 2 * tAnchor[2];
		return aX >= tCX - 2 && aY >= tCY && aZ >= tCZ - 2 && aX <= tCX + 2 && aY <= tCY + 6 && aZ <= tCZ + 2;
	}

	// ---------------------------------------------------------------------------
	// the output face (:257-265)
	// ---------------------------------------------------------------------------

	/**
	 * The map-aware canOutput — the base copy walks the outputs from the FIXED slot 1
	 * (the Coke-Oven one-input shape); this map carries TWO input slots, so the output
	 * slots start at {@code mInputItemsCount} (the GTLargeMachineBlockEntity :789-833
	 * override, the implosion form). The :650-666 fluid branch is LIVE here (the map
	 * carries two fluid outputs — the implosion copy's dormant variant becomes the base
	 * body verbatim). The :626-629 chain cap and the stack-cap arithmetic are the base
	 * body verbatim.
	 */
	@Override
	public int canOutput(Recipe aRecipe) {
		int rMaxTimes = (int) mParallel; // :621 — the :1241 NBT_PARALLEL 64

		if (mParallelDuration) {
			// :626-629 verbatim — chain processing: don't take more than 600 ticks worth of
			// input at a time (the total power must stay inside mInputMax * 600)
			while (rMaxTimes > 1 && aRecipe.getAbsoluteTotalPower() * rMaxTimes > mInputMax * 600) rMaxTimes--;
		}

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
				/* // 1.21.1: isSameItemSameTags renamed to isSameItemSameComponents
				   // (1.21.1 ItemStack javap).
				if (!ItemStack.isSameItemSameComponents(tSlot, tOutput)) {mOutputBlocked++; return 0;} // :637-640
				 *///?}
				rMaxTimes = Math.min(rMaxTimes, (tSlot.getMaxStackSize() - tSlot.getCount()) / tOutput.getCount()); // :641
				if (rMaxTimes <= 0) {mOutputBlocked++; return 0;} // :642-645
			} else {
				rMaxTimes = Math.min(rMaxTimes, Math.max(1, 64 / tOutput.getCount())); // :647
			}
		}
		if (aRecipe.mFluidOutputs.length > 0) { // :650-666 — LIVE (two matter fluids out)
			int tEmptyOutputTanks = 0, tRequiredEmptyTanks = aRecipe.mFluidOutputs.length;
			for (FluidTankGT tTank : mTanksOutput) if (tTank.isEmpty()) tEmptyOutputTanks++;
			for (int j = 0; j < aRecipe.mFluidOutputs.length; j++) {
				if (aRecipe.mFluidOutputs[j] == null) {
					tRequiredEmptyTanks--;
				} else for (FluidTankGT tTank : mTanksOutput) if (tTank.contains(aRecipe.mFluidOutputs[j])) {
					//? if forge {
					if (tTank.has(Math.max(16000, 1 + (long)aRecipe.mFluidOutputs[j].getAmount() * mParallel)) && !FLUIDS_VOID_OVERFLOW.contains(String.valueOf(net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(aRecipe.mFluidOutputs[j].getFluid())))) return 0; // :659
					//?}
					//? if neoforge {
					/* // 1.21.1: the registry handle is BuiltInRegistries.FLUID (the swap's
					   // Registries.FLUID is the ResourceKey, not the Registry — no getKey).
					if (tTank.has(Math.max(16000, 1 + (long)aRecipe.mFluidOutputs[j].getAmount() * mParallel)) && !FLUIDS_VOID_OVERFLOW.contains(String.valueOf(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(aRecipe.mFluidOutputs[j].getFluid())))) return 0; // :659
					 *///?}
					tRequiredEmptyTanks--;
					break;
				}
			}
			if (tRequiredEmptyTanks > tEmptyOutputTanks) return 0; // :664
		}
		return rMaxTimes; // :667
	}

	/**
	 * Upstream :258-260 — the fluid auto-out targets the tank BELOW (queried at its top
	 * face; the implosion below-cell form). The matter fluids park here when a tank sits
	 * under the controller.
	 */
	@Override
	@Nullable
	protected net.minecraftforge.fluids.capability.IFluidHandler getFluidOutputTarget(Fluid aOutput) {
		if (!hasLevel() || isClientSide()) return null;
		net.minecraft.world.level.block.entity.BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().below());
		if (tNeighbor == null) return null;
		//? if forge {
		return tNeighbor.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, Direction.UP).resolve().orElse(null);
		//?} else {
		/*// 21.1: the query goes through the level (the TileEntityCokeOven seam form).
		return getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, getBlockPos().below(), net.minecraft.core.Direction.UP);
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// NBT — the second output tank (the GT6Distillation outputTankKey form)
	// ---------------------------------------------------------------------------

	/** The second tank's key ({@code output_tank_1}); tank 0 rides the base NBT_OUTPUT_TANK. */
	public static String outputTankKey(int aIndex) {
		return TileEntityBase10MultiBlockMachine.NBT_OUTPUT_TANK + "_" + aIndex;
	}

	//? if forge {
	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		for (int i = 1; i < mTanksOutput.length; i++) mTanksOutput[i].writeToNBT(aNBT, outputTankKey(i)); // the Distillation :727-733 form
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		for (int i = 1; i < mTanksOutput.length; i++) mTanksOutput[i].readFromNBT(aNBT, outputTankKey(i));
	}
	//?}
	//? if neoforge {
	/* // 21.1: the shared-chain (CompoundTag) signatures ride unchanged — the base
	   // load/save are the shared-tree methods, so the overrides above are leg-invariant
	   // apart from the provider-first serialization the base already carries.
	 *///?}

	// ---------------------------------------------------------------------------
	// the energy face (MultiTileEntityBasicMachine.java:489-517)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return !aEmitting && aEnergyType == mEnergyTypeAccepted; // :510
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeAccepted.AS_LIST; // :517
	}

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (mStopped) return 0; // :490
		aSize = Math.abs(aSize); // :492
		if (aSize > mInputMax) return 0; // :493-496 — the overcharge face is the cut ADR-D1 pool; refuse (declared narrowing)
		if (aEnergyType != mEnergyTypeAccepted) return 0; // :500 (the mEnergyTypeCharged branch :497-499 is cut with charging)
		long tInput = Math.min(mInputMax - mEnergy, aSize * aAmount), tConsumed = Math.min(aAmount, (tInput / aSize) + (tInput % aSize != 0 ? 1 : 0)); // :502
		if (aDoInject) mEnergy += tConsumed * aSize; // :503
		return tConsumed; // :504
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return mInputMin; // :513
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return mInput; // :514
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return mInputMax; // :515
	}
}
