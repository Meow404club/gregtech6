package gregtech6.tileentity.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.tools.GTKitchenBlock;
import gregtech6.fluid.FluidTankGT;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Kitchen;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The manual (no-energy) kitchen processing base — the port counterpart of the shared
 * body of upstream {@code MultiTileEntityBathingPot} (tmp/gt6-1.7.10 .../tools/
 * MultiTileEntityBathingPot.java:62-459) and {@code MultiTileEntityMixingBowl}
 * (.../MultiTileEntityMixingBowl.java:62-480), which are two near-identical upstream
 * copies differing only in the recipe map, the {@code canOutput} fluid doors, the
 * exhaustion divisor and the item-output slot census (the pot checks all 6 output slots
 * :172, the bowl only slot 6 :190 — generalised here over
 * {@code mRecipes.mOutputItemsCount}, which is 6 / 1 respectively, so the check is the
 * upstream one bit-for-bit).
 *
 * <p><b>The processing semantics (:186-209 pot / :204-228 bowl, verbatim):</b> a top-face
 * click runs one manual round — {@code findRecipe(mLastRecipe, V[1]=32, NI, mTanksInput,
 * slots 0..5)}, then on a hit {@code canOutput(recipe)} + {@code isRecipeInputEqual(T, F,
 * ...)} pay the inputs and drop the outputs into slots 6..(6+mOutputItemsCount-1) and the
 * output tanks, exhausting the player by {@code max(1, totalPower)/1000} (pot :202) or
 * {@code totalPower/250} (bowl :223). The NEI corner branch (:190/:210, the small
 * top-left pixel quadrant) opens NEI upstream — the port has no NEI, the branch is the
 * declared no-op (report arm only). The rest of the chain (:211-257/:230-278) is the
 * click-economy: collect an output slot, receive the held fluid container into ALL tanks
 * (the :219 {@code FL.fillAll_} face), move the held item into the input slots /
 * per-tank fills in the top-centre-vs-side order, and — with the output slots empty —
 * hand the input slots back to the player (:253/:274).
 *
 * <p><b>Tanks (:73-78):</b> the input/output tank ARRAYS are sized by the recipe map's
 * {@code mInputFluidCount}/{@code mOutputFluidCount}, every tank capped at the block
 * carrier's {@code NBT_TANK_CAPACITY} litres (4000 wood / 8000 steel / 8000 bowl). NBT
 * keys are the upstream shapes {@code tank.in.<i>} / {@code tank.out.<i>} (:76/:84).
 *
 * <p><b>The fluid doors (verbatim predicates, port FluidType carrier):</b>
 * <ul>
 * <li>{@code getFluidTankFillable2} (:302-307 pot): a tank already containing the fluid
 *     wins; else the door — {@code temperature >= mMaterial.mMeltingPoint - 100} (hot
 *     fluid in a wood pot melts the glaze) or {@code !heavier} (density <= 0, upstream
 *     FL.java:776) refuses; else the first EMPTY input tank.</li>
 * <li>{@code getFluidTankDrainable2} (:310-317): OUTPUT tanks only — the null probe
 *     rotates through them by SERVER_TIME/20 (port: level.getGameTime()/20), a named
 *     drain takes the containing one.</li>
 * <li>{@code canOutput} fluid half (:177-181 pot): an occupied output tank refuses the
 *     round when the recipe's fluid output does not match it, is hot (same -100 K door),
 *     {@code lighter} (density &lt; 0), {@code gas} or would overflow.</li>
 * </ul>
 * <b>Declared predicate deviations</b> (the port has no FluidsGT name-set bridge):
 * {@code gas} ≈ {@code FluidType.isLighterThanAir()} (upstream FL.java:768 = the
 * not-in-LIQUID-set && (isGaseous || in-GAS-set) composition; every port fluid encodes
 * its gas state through the carrier density, the chlorine −100 row the precedent) and
 * the bowl's {@code !FL.simple} door (:196/:325, the SIMPLE name-set) ≈
 * {@code isLighterThanAir()} too — SIMPLE is the non-gas non-plasma liquid set upstream,
 * so both doors collapse to the same carrier query on the port universe.
 *
 * <p><b>Pool cuts (task card):</b> the rain-collection tick (:127-139, the crucible
 * research pool), the plunger/magnifying-glass tool arms (:100-120, the tools pool), the
 * TOOL_mixer stick arm (bowl :100-117, the tool does not exist in the port — bare-right-
 * click processing stays, so the bowl remains fully drivable), the upstream
 * {@code NBT_RECIPEMAP} override (the MTE-registration NBT seam; the port BE classes pin
 * their map) and the mDisplay fluid-tint renderer (:63/:141-167, the visual pool — the
 * BE data the RCON channel reads is the live tank/inventory state, not the display
 * short).
 */
public abstract class GT6ManualKitchenBlockEntity extends TileEntityBase03TicksAndSync {

	/** The input slot count (upstream slots 0-5, the :193 {@code ST.array(slot(0)..slot(5))} literal). */
	public static final int INPUT_SLOTS = 6;
	/** The output slot count (upstream slots 6-11, the :211 {@code for (i = 6; i < 12; i++)} literal). */
	public static final int OUTPUT_SLOTS = 6;
	/** The total inventory size (upstream the 12-slot IInventory). */
	public static final int SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;

	/** The findRecipe stack-size argument — upstream {@code V[1]} (CS.java:151: V = {8, 32, ...}). */
	public static final long RECIPE_SIZE = 32;

	/** The upstream NBT key shapes (:76/:84 — {@code NBT_TANK+".in."+i}). */
	public static final String NBT_TANK_IN = "tank.in.";
	/** Upstream {@code NBT_TANK+".out."+i} (:78/:85). */
	public static final String NBT_TANK_OUT = "tank.out.";

	/** The per-tank litres — the block carrier's NBT_TANK_CAPACITY (4000 wood / 8000 steel / 8000 bowl). */
	protected final long mCapacityL;
	/** The upstream NBT_MATERIAL — only {@code mMeltingPoint} is consumed (the -100 K doors). */
	protected final OreDictMaterial mMaterial;

	/** Upstream :65 — the last recipe buffer (the findRecipe cache argument). */
	@Nullable
	protected Recipe mLastRecipe = null;
	/** Upstream :66 — the input tanks, sized by the map's mInputFluidCount. */
	@Nullable
	protected FluidTankGT[] mTanksInput = null;
	/** Upstream :66 — the output tanks, sized by the map's mOutputFluidCount. */
	@Nullable
	protected FluidTankGT[] mTanksOutput = null;
	/** The 12-slot manual inventory (slots 0-5 in, 6-11 out). */
	protected final ItemStackHandler mInventory;

	/**
	 * Full constructor — the GTBarrelBlockEntity carrier-read shape: capacity and material
	 * come from the block (a non-kitchen block leaves the 1000 L / MT.Wood defaults, the
	 * offline-fixture arm). NOT ticking: the manual family has no per-tick behaviour in
	 * the port scope (the rain collector is the pool cut).
	 */
	protected GT6ManualKitchenBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : kitchenBET(aState), aPos, aState);
		if (aState.getBlock() instanceof GTKitchenBlock tKitchen) {
			mCapacityL = tKitchen.capacityL();
			mMaterial = tKitchen.material();
		} else {
			mCapacityL = 1000; // upstream :73 — the readFromNBT2 default when no NBT_TANK_CAPACITY
			mMaterial = MT.Wood; // the upstream ANY.Wood row representative (the pot family default)
		}
		mInventory = new ItemStackHandler(SLOTS);
	}

	/** The BET the block mounts (the GTKitchenBlock carrier read; the pot fallback covers the offline-fixture arm). */
	private static BlockEntityType<?> kitchenBET(BlockState aState) {
		return aState.getBlock() instanceof GTKitchenBlock tKitchen ? tKitchen.betType() : GT6Kitchen.BATHING_POT_BE.get();
	}

	/** The recipe map this member processes ({@code RM.Bath} / {@code RM.Mixer} — upstream :64). */
	protected abstract RecipeMap recipeMap();

	/** The exhaustion divisor — 1000 on the pot (:202), 250 on the bowl (:223). */
	protected abstract long exhaustDivisor();

	/** The bowl-only extra fill door — {@code !FL.simple} (:325) ≈ gas on the port universe; the pot arm is false. */
	protected boolean refuseFillFluid(FluidStack aFluid) {
		return false;
	}

	/** The bowl-only extra canOutput door — {@code !FL.simple} (:196); the pot arm is the {@code FL.gas} verbatim (:178). */
	protected boolean refuseOutputFluid(FluidStack aFluid) {
		return isGasFluid(aFluid); // the pot :178 FL.gas verbatim
	}

	// ---------------------------------------------------------------------------
	// the fluid predicate set (upstream FL.java, port FluidType carrier)
	// ---------------------------------------------------------------------------

	/** Upstream FL.temperature — the FluidType carrier temperature (the FL.create literal side). */
	protected static int fluidTemperature(FluidStack aFluid) {
		return aFluid.getFluid().getFluidType().getTemperature();
	}

	/** Upstream FL.heavier (FL.java:776 verbatim) — density &gt; 0. */
	protected static boolean isHeavierFluid(FluidStack aFluid) {
		return aFluid.getFluid().getFluidType().getDensity() > 0;
	}

	/** Upstream FL.lighter (FL.java:775 verbatim) — density &lt; 0. */
	protected static boolean isLighterFluid(FluidStack aFluid) {
		return aFluid.getFluid().getFluidType().getDensity() < 0;
	}

	/** Upstream FL.gas (FL.java:768) ≈ the carrier's lighter-than-air flag (the declared deviation, class doc). */
	protected static boolean isGasFluid(FluidStack aFluid) {
		return aFluid.getFluid().getFluidType().isLighterThanAir();
	}

	// ---------------------------------------------------------------------------
	// tank/inventory plumbing
	// ---------------------------------------------------------------------------

	/** The lazy tank build (upstream :75-78): array sizes follow the map, capacity follows the block. */
	protected void ensureTanks() {
		if (mTanksInput != null) return;
		RecipeMap tMap = recipeMap();
		mTanksInput = new FluidTankGT[(int)tMap.mInputFluidCount];
		for (int i = 0; i < mTanksInput.length; i++) mTanksInput[i] = new FluidTankGT(mCapacityL);
		mTanksOutput = new FluidTankGT[(int)tMap.mOutputFluidCount];
		for (int i = 0; i < mTanksOutput.length; i++) mTanksOutput[i] = new FluidTankGT(mCapacityL);
	}

	/** The findRecipe fluid argument shape (the TileEntityBasicMachine.tankSnapshot form): live stacks or null. */
	@Nullable
	protected FluidStack[] fluidSnapshot(@Nullable FluidTankGT[] aTanks) {
		if (aTanks == null) return new FluidStack[0];
		FluidStack[] rSnapshot = new FluidStack[aTanks.length];
		for (int i = 0; i < aTanks.length; i++) rSnapshot[i] = aTanks[i].fluid();
		return rSnapshot;
	}

	/** Upstream :73-78 lazily materialised before any tank access. */
	@Nullable
	protected FluidTankGT tankInput(int aIndex) {
		ensureTanks();
		return aIndex >= 0 && aIndex < mTanksInput.length ? mTanksInput[aIndex] : null;
	}

	@Nullable
	protected FluidTankGT tankOutput(int aIndex) {
		ensureTanks();
		return aIndex >= 0 && aIndex < mTanksOutput.length ? mTanksOutput[aIndex] : null;
	}

	/** Upstream getFluidTanks2 (:320-325) — input tanks then output tanks, the side-blind view. */
	public FluidTankGT[] allTanks() {
		ensureTanks();
		FluidTankGT[] rTanks = new FluidTankGT[mTanksInput.length + mTanksOutput.length];
		System.arraycopy(mTanksInput, 0, rTanks, 0, mTanksInput.length);
		System.arraycopy(mTanksOutput, 0, rTanks, mTanksInput.length, mTanksOutput.length);
		return rTanks;
	}

	/** The 12-slot manual inventory (the capability surface + the command face). */
	public ItemStackHandler inventory() {
		return mInventory;
	}

	/** The -100 K hot door (upstream :304/:178) — {@code temperature >= mMaterial.mMeltingPoint - 100}. */
	protected boolean tooHotForPot(FluidStack aFluid) {
		return fluidTemperature(aFluid) >= mMaterial.mMeltingPoint - 100;
	}

	/**
	 * Upstream :302-307 (pot) / :323-327 (bowl) — the fill admission: a containing input
	 * tank wins over the doors; the hot / not-heavier / gas(bowl: not-simple) door refuses;
	 * else the first empty input tank.
	 */
	@Nullable
	public FluidTankGT getFluidTankFillable(@Nullable FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return null;
		ensureTanks();
		for (FluidTankGT tTank : mTanksInput) if (tTank.contains(aFluid)) return tTank;
		if (tooHotForPot(aFluid) || !isHeavierFluid(aFluid) || refuseFillFluid(aFluid)) return null;
		for (FluidTankGT tTank : mTanksInput) if (tTank.isEmpty()) return tTank;
		return null;
	}

	/**
	 * Upstream :310-317 — OUTPUT tanks only: the null probe rotates by time (the
	 * SERVER_TIME/20 round-robin, port game-time), a named drain takes the containing tank.
	 */
	@Nullable
	public FluidTankGT getFluidTankDrainable(@Nullable FluidStack aFluid) {
		ensureTanks();
		if (aFluid == null || aFluid.isEmpty()) {
			long tTime = hasLevel() && getLevel() != null ? getLevel().getGameTime() : 0;
			for (int i = 0; i < mTanksOutput.length; i++) {
				FluidTankGT tTank = mTanksOutput[(int)((tTime / 20 + i) % mTanksOutput.length)];
				if (tTank.has()) return tTank;
			}
		} else {
			for (FluidTankGT tTank : mTanksOutput) if (tTank.contains(aFluid)) return tTank;
		}
		return null;
	}

	/**
	 * Upstream :171-183 (pot) / :189-200 (bowl) — the canOutput gate: every occupied
	 * output slot must be stack-compatible with the recipe's output at that index (or the
	 * recipe needs empty outputs), every occupied output tank must match the recipe's
	 * fluid output and pass the hot/lighter/gas(pot)|not-simple(bowl)/overflow doors.
	 */
	protected boolean canOutput(Recipe aRecipe) {
		int tOutSlots = (int)recipeMap().mOutputItemsCount;
		for (int i = 0; i < tOutSlots; i++) {
			ItemStack tSlot = mInventory.getStackInSlot(i + INPUT_SLOTS);
			if (tSlot.isEmpty()) continue;
			if (aRecipe.mNeedsEmptyOutput || (aRecipe.mOutputs.length > 0 && i < aRecipe.mOutputs.length
					&& aRecipe.mOutputs[i] != null
					&& (!isSameItemSameTags(tSlot, aRecipe.mOutputs[i])
						|| tSlot.getCount() + aRecipe.mOutputs[i].getCount() > tSlot.getMaxStackSize()))) {
				return false;
			}
		}
		ensureTanks();
		for (int i = 0; i < mTanksOutput.length && i < aRecipe.mFluidOutputs.length; i++) {
			if (!mTanksOutput[i].has()) continue;
			if (aRecipe.mNeedsEmptyOutput || (aRecipe.mFluidOutputs[i] != null
					&& (!mTanksOutput[i].contains(aRecipe.mFluidOutputs[i])
						|| tooHotForPot(aRecipe.mFluidOutputs[i])
						|| isLighterFluid(aRecipe.mFluidOutputs[i])
						|| refuseOutputFluid(aRecipe.mFluidOutputs[i])
						|| mTanksOutput[i].has(Math.max(1000, 1 + aRecipe.mFluidOutputs[i].getAmount()))))) {
				return false;
			}
		}
		return true;
	}

	/** Upstream ST.equal(a, b, F) — the NBT-comparing equality (the GTChiselItem fork form). */
	protected static boolean isSameItemSameTags(ItemStack aA, ItemStack aB) {
		//? if forge {
		return ItemStack.isSameItemSameTags(aA, aB);
		//?} else {
		/*return ItemStack.isSameItemSameComponents(aA, aB);
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// the activation chain (upstream onBlockActivated3 :186-275 pot / :204-296 bowl)
	// ---------------------------------------------------------------------------

	/**
	 * The whole server-side activation chain. Returns the human-readable report (the RCON
	 * acceptance channel — the GTTapBlockEntity.activateChain precedent; the player path
	 * ignores the string). {@code aPlayer == null} = the empty-hand RCON arm. The client
	 * side is a no-op (upstream played the pour sounds client-side :258-273 — the sound
	 * face rides the pool cut with the mDisplay renderer).
	 */
	public String activateChain(@Nullable Player aPlayer, byte aSide, ItemStack aHeld, float aHitX, float aHitY, float aHitZ) {
		if (!isServerSide()) return "client side";
		ensureTanks();
		boolean tTop = (aSide == 1); // SIDES_TOP — the CS side order == Direction.get3DDataValue (P4)

		// :188-209 — the top-face manual processing round
		if (tTop) {
			// :190/:210 — the NEI corner quadrant opens NEI upstream; no NEI in the port (declared no-op)
			if (aHitX <= PX_CORNER && aHitZ <= PX_CORNER) return "NEI corner (no NEI in the port — no-op)";

			RecipeMap tMap = recipeMap();
			ItemStack[] tInputItems = inputStacks();
			Recipe tRecipe = tMap.findRecipe(mLastRecipe, RECIPE_SIZE, ItemStack.EMPTY, fluidSnapshot(mTanksInput), tInputItems);
			if (tRecipe != null) {
				if (tRecipe.mCanBeBuffered) mLastRecipe = tRecipe;
				if (canOutput(tRecipe) && tRecipe.isRecipeInputEqual(true, false, fluidSnapshot(mTanksInput), tInputItems)) {
					ItemStack[] tOutputs = tRecipe.getOutputs();
					FluidStack[] tFluidOutputs = tRecipe.getFluidOutputs();
					int tOutSlots = (int)tMap.mOutputItemsCount;
					for (int i = 0; i < tOutSlots && i < tOutputs.length; i++) addStackToSlot(INPUT_SLOTS + i, tOutputs[i]);
					for (int i = 0; i < mTanksOutput.length && i < tFluidOutputs.length; i++) mTanksOutput[i].fill(tFluidOutputs[i], IFluidHandler.FluidAction.EXECUTE);
					// :202/:223 — UT.Entities.exhaust(aPlayer, max(1, totalPower) / <divisor>)
					if (aPlayer != null) aPlayer.causeFoodExhaustion(
							Math.max(1, tRecipe.getAbsoluteTotalPower()) / (float)exhaustDivisor());
					String tMain = tOutputs.length > 0 ? tOutputs[0].getCount() + "x " + tOutputs[0].getItem() : "fluids";
					return "processed: " + tMain + " (+tanks)";
				}
				return "recipe found but the output side is blocked (canOutput failed)";
			}
		}

		// :211-215/:230-234 — collect ONE output slot into the player (the RCON null-player arm skips)
		if (aPlayer != null) {
			for (int i = INPUT_SLOTS; i < SLOTS; i++) {
				ItemStack tOut = mInventory.getStackInSlot(i);
				if (!tOut.isEmpty() && giveToPlayer(aPlayer, tOut)) {
					mInventory.setStackInSlot(i, ItemStack.EMPTY);
					playCollect();
					return "collected " + tOut.getCount() + "x " + tOut.getItem();
				}
			}
		}

		// :216-223/:235-244 — the held fluid container pours into ALL tanks (the FL.fillAll_ face)
		FluidStack tHeld = containerFluid(aHeld);
		if (tHeld != null) {
			int tFilled = fillAllTanks(tHeld);
			if (tFilled > 0) {
				shrinkAndGiveContainer(aPlayer, aHeld);
				return "filled " + tFilled + " L of " + tHeld.getFluid().getFluidType() + " from the container";
			}
		}

		// :224-252 — the held item into the input slots / the per-tank fills.
		// :224 top-centre = items FIRST then fluids; :238 sides = fluids FIRST then items.
		if (aHeld != null && !aHeld.isEmpty()) {
			if (tTop && aHitX > PX_CORNER && aHitX < PX_INNER && aHitZ > PX_CORNER && aHitZ < PX_INNER) {
				if (aPlayer != null && moveHeldIntoInputSlots(aPlayer)) return "inserted " + aHeld.getItem();
				FluidStack tPerTank = containerFluid(aHeld);
				if (tPerTank != null && fillPerTankOrder(tPerTank) > 0) {
					shrinkAndGiveContainer(aPlayer, aHeld);
					return "filled a tank from the container";
				}
			} else {
				FluidStack tPerTank = containerFluid(aHeld);
				if (tPerTank != null && fillPerTankOrder(tPerTank) > 0) {
					shrinkAndGiveContainer(aPlayer, aHeld);
					return "filled a tank from the container";
				}
				if (aPlayer != null && moveHeldIntoInputSlots(aPlayer)) return "inserted " + aHeld.getItem();
			}
		}

		// :253-257/:274-278 — with the output side clear, hand the input slots back
		if (aPlayer != null && outputsAllClear()) {
			for (int i = 0; i < INPUT_SLOTS; i++) {
				ItemStack tIn = mInventory.getStackInSlot(i);
				if (!tIn.isEmpty() && giveToPlayer(aPlayer, tIn)) {
					mInventory.setStackInSlot(i, ItemStack.EMPTY);
					playCollect();
					return "collected input " + tIn.getCount() + "x " + tIn.getItem();
				}
			}
		}
		return "no action";
	}

	/** The corner-quadrant pixel bound — upstream {@code PX_P[2]} = 3/16 (CS PX_P = 2 px/step form). */
	protected static final float PX_CORNER = 3.0F / 16.0F;
	/** The centre-quadrant bound — upstream {@code PX_N[2]} = 13/16. */
	protected static final float PX_INNER = 13.0F / 16.0F;

	/** The input slots as the findRecipe argument (upstream :193). */
	private ItemStack[] inputStacks() {
		ItemStack[] rStacks = new ItemStack[INPUT_SLOTS];
		for (int i = 0; i < INPUT_SLOTS; i++) rStacks[i] = mInventory.getStackInSlot(i);
		return rStacks;
	}

	/** Upstream addStackToSlot — stack onto a matching output slot, else fill the empty slot. */
	private void addStackToSlot(int aSlot, ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return;
		ItemStack tSlot = mInventory.getStackInSlot(aSlot);
		if (tSlot.isEmpty()) {
			mInventory.setStackInSlot(aSlot, aStack.copy());
		} else if (isSameItemSameTags(tSlot, aStack) && tSlot.getCount() + aStack.getCount() <= tSlot.getMaxStackSize()) {
			tSlot.grow(aStack.getCount());
		}
		// the overflow arm drops silently — the upstream addStackToSlot growth-cap form
	}

	/** Upstream :211 ST.add — give to the player inventory; false keeps the slot (the bag-full verdict). */
	private boolean giveToPlayer(Player aPlayer, ItemStack aStack) {
		ItemStack tRemainder = aStack.copy();
		boolean tAdded = aPlayer.getInventory().add(tRemainder);
		return tAdded && tRemainder.isEmpty();
	}

	/** The :212 collect sound — upstream playCollect (the ITEM_PICKUP pip). */
	private void playCollect() {
		if (hasLevel() && getLevel() != null) {
			getLevel().playSound(null, getBlockPos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	/** The :253 output-clear gate — pot: all six output slots empty (bowl overrides to slot 6 only). */
	protected boolean outputsAllClear() {
		for (int i = INPUT_SLOTS; i < SLOTS; i++) if (!mInventory.getStackInSlot(i).isEmpty()) return false;
		return true;
	}

	/** Upstream :216 ST.container — the crafting-remaining form (water bucket → empty bucket). */
	@Nullable
	private ItemStack containerRemainder(ItemStack aHeld) {
		return aHeld.getItem().getCraftingRemainingItem(aHeld);
	}

	/** Upstream :217 FL.getFluid(ST.amount(1, aStack), T) — one container's worth of fluid. */
	@Nullable
	private FluidStack containerFluid(@Nullable ItemStack aHeld) {
		if (aHeld == null || aHeld.isEmpty()) return null;
		return net.minecraftforge.fluids.FluidUtil.getFluidContained(aHeld).orElse(null);
	}

	/** Upstream :219-222 — fillAll_ over ALL tanks (input + output), the container pays one unit. */
	private int fillAllTanks(FluidStack aFluid) {
		int rFilled = 0;
		FluidStack tRemaining = aFluid.copy();
		for (FluidTankGT tTank : allTanks()) {
			int tFilled = tTank.fill(tRemaining, IFluidHandler.FluidAction.EXECUTE);
			if (tFilled > 0) {
				rFilled += tFilled;
				tRemaining.shrink(tFilled);
				if (tRemaining.isEmpty()) break;
			}
		}
		return rFilled;
	}

	/** Upstream :228-248 — the per-tank order: output tanks FIRST, then input tanks (one container unit each). */
	private int fillPerTankOrder(FluidStack aFluid) {
		ensureTanks();
		int rFilled = 0;
		for (FluidTankGT tTank : mTanksOutput) {rFilled = tTank.fill(aFluid, IFluidHandler.FluidAction.EXECUTE); if (rFilled > 0) return rFilled;}
		for (FluidTankGT tTank : mTanksInput) {rFilled = tTank.fill(aFluid, IFluidHandler.FluidAction.EXECUTE); if (rFilled > 0) return rFilled;}
		return rFilled;
	}

	/** Upstream :220-221 — the container pays one item and the player gets the empty form back. */
	private void shrinkAndGiveContainer(@Nullable Player aPlayer, ItemStack aHeld) {
		ItemStack tRemainder = containerRemainder(aHeld);
		aHeld.shrink(1);
		if (aPlayer != null && tRemainder != null && !tRemainder.isEmpty()) aPlayer.getInventory().placeItemBackInInventory(tRemainder);
	}

	/** Upstream :225-227/:249-251 ST.move — the held stack tries the input slots, first success wins. */
	private boolean moveHeldIntoInputSlots(Player aPlayer) {
		ItemStack tHeld = aPlayer.getMainHandItem();
		if (tHeld.isEmpty()) return false;
		for (int i = 0; i < INPUT_SLOTS; i++) {
			ItemStack tSlot = mInventory.getStackInSlot(i);
			if (tSlot.isEmpty()) {
				mInventory.setStackInSlot(i, tHeld.copy());
				aPlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				return true;
			}
			if (isSameItemSameTags(tSlot, tHeld) && tSlot.getCount() < tSlot.getMaxStackSize()) {
				int tMove = Math.min(tHeld.getCount(), tSlot.getMaxStackSize() - tSlot.getCount());
				tSlot.grow(tMove);
				tHeld.shrink(tMove);
				if (tHeld.isEmpty()) aPlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream :81-86 — the tank bank keys; the inventory rides the handler)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		ensureTanks();
		for (int i = 0; i < mTanksInput.length; i++) mTanksInput[i].writeToNBT(aNBT, NBT_TANK_IN + i);
		for (int i = 0; i < mTanksOutput.length; i++) mTanksOutput[i].writeToNBT(aNBT, NBT_TANK_OUT + i);
//? if forge {
		aNBT.put("inventory", mInventory.serializeNBT());
//?} else {
		/*aNBT.put("inventory", mInventory.serializeNBT(NBT_ACCESS)); // 21.1: ItemStackHandler NBT takes the registries (the TileEntityBasicMachine:1542 form)
		 *///?}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		ensureTanks();
		for (int i = 0; i < mTanksInput.length; i++) mTanksInput[i].readFromNBT(aNBT, NBT_TANK_IN + i);
		for (int i = 0; i < mTanksOutput.length; i++) mTanksOutput[i].readFromNBT(aNBT, NBT_TANK_OUT + i);
//? if forge {
		if (aNBT.contains("inventory")) mInventory.deserializeNBT(aNBT.getCompound("inventory"));
//?} else {
		/*if (aNBT.contains("inventory")) mInventory.deserializeNBT(NBT_ACCESS, aNBT.getCompound("inventory")); // 21.1: provider-first
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// capability seam (the TileEntityBasicMachine fork shape)
	// ---------------------------------------------------------------------------

	/** The per-call fluid wrapper — all tanks visible, fill via the :302 doors, drain via the :310 output rotation. */
	public IFluidHandler newFluidHandler() {
		return new KitchenFluidHandler(this);
	}

	/** The 12-slot item face (the upstream full IInventory exposure). */
	public IItemHandler newItemHandler() {
		return mInventory;
	}

	/** The IFluidHandler over the manual tank banks (the BasicMachineFluidHandler side-blind form). */
	private static final class KitchenFluidHandler implements IFluidHandler {
		private final GT6ManualKitchenBlockEntity mKitchen;

		KitchenFluidHandler(GT6ManualKitchenBlockEntity aKitchen) {
			mKitchen = aKitchen;
		}

		@Override
		public int getTanks() {
			return mKitchen.allTanks().length;
		}

		@Override
		public FluidStack getFluidInTank(int aTank) {
			FluidTankGT tTank = tank(aTank);
			FluidStack tFluid = tTank == null ? null : tTank.fluid();
			return tFluid == null ? FluidStack.EMPTY : tFluid;
		}

		@Override
		public int getTankCapacity(int aTank) {
			FluidTankGT tTank = tank(aTank);
			return tTank == null ? 0 : (int)tTank.capacity();
		}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {
			return aStack != null && !aStack.isEmpty();
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			FluidTankGT tTank = mKitchen.getFluidTankFillable(aResource);
			return tTank == null ? 0 : tTank.fill(aResource, aAction);
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			FluidTankGT tTank = mKitchen.getFluidTankDrainable(aResource);
			if (tTank == null) return FluidStack.EMPTY;
			FluidStack tDrained = tTank.drain(aResource.getAmount(), aAction);
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			FluidTankGT tTank = mKitchen.getFluidTankDrainable(null);
			if (tTank == null) return FluidStack.EMPTY;
			FluidStack tDrained = tTank.drain(aMaxDrain, aAction);
			return tDrained == null ? FluidStack.EMPTY : tDrained;
		}

		@Nullable
		private FluidTankGT tank(int aTank) {
			FluidTankGT[] tAll = mKitchen.allTanks();
			return aTank >= 0 && aTank < tAll.length ? tAll[aTank] : null;
		}
	}

	//? if forge {
	/** The cached all-sides item face (the side-less probe keeps one LazyOptional, the machine form). */
	private net.minecraftforge.common.util.LazyOptional<IItemHandler> mItemCap = net.minecraftforge.common.util.LazyOptional.of(this::newItemHandler);

	@Override
	public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> aCapability, @Nullable net.minecraft.core.Direction aSide) {
		if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
			if (aSide == null) return mItemCap.cast();
			return net.minecraftforge.common.util.LazyOptional.of(this::newItemHandler).cast();
		}
		if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
			// the fresh-wrapper-per-call form (TileEntityBasicMachine:1417-1421) — the wrapper is stateless per query
			return net.minecraftforge.common.util.LazyOptional.of(this::newFluidHandler).cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mItemCap.invalidate();
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 removed BlockEntity#getCapability/LazyOptional — this
	// member is the provider seam; the GT6CapabilityWiring registerBlockEntity delegates
	// to it. No @Override: the parent method does not exist on 21.1.)
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, net.minecraft.core.Direction> aCapability, @Nullable net.minecraft.core.Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK) {
			return (T) newItemHandler();
		}
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
			return (T) newFluidHandler();
		}
		return null;
	}
	 *///?}

	@Override
	public String getTileEntityName() {
		return "kitchen";
	}
}
