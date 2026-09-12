/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.tileentity.tools;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.items.tools.GT6ToolActions;
import gregtech6.block.tools.GTAnvilBlock;
import gregtech6.datagen.GT6ItemTags;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GT6Anvils;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Anvil — the zero-energy manual hammering block (task p28-c-anvil), the port of the
 * upstream {@code MultiTileEntityAnvil} (tmp/gt6-1.7.10 .../tools/MultiTileEntityAnvil
 * .java:62-430), shared over the Stone + Blackstone rows (Loader_MultiTileEntities.java
 * :2185-2186, one BE class — the ADR-P3-1 shared-BET multi-mount; the rows differ ONLY in
 * the registration NBT the block carrier now holds). NO GUI, no tanks, no energy face:
 * the upstream tooltip line is {@code LH.NO_GUI_CLICK_TO_INTERACT} (:91) and the machine
 * is the zero-energy manual tier of the P28 lineup (decisions.p28-ulv-tier-rulings).
 *
 * <h2>The two-slot working surface (:63-65)</h2>
 * {@code mInventory[2]} — the two working halves. The side click even-split arm
 * (:249-270) redistributes a single occupied slot over both halves ({@code slotNull}
 * only clears a <= 0 slot, TileEntityBase05Inventories.java:95 — zero item loss, the
 * halving IS the semantic).
 *
 * <h2>The working strike (onToolClick2 :94-141 → {@link #hammerStrike})</h2>
 * A hammer click on the top face = the {@code RM.Anvil} map ({@code GT6RecipeMaps.ANVIL}
 * — the grinding + welding face); a hammer click on a horizontal side = the bend face.
 * <b>Declared fold</b>: upstream splits the side strike between RM.AnvilBendSmall/
 * AnvilBendBig by WHERE on the side the hammer lands (:100-115) — the port carries ONE
 * bend map (the task-card two-map freeze, GT6RecipeMaps.ANVIL_BEND field doc) and any
 * side strike bends (the hit-half aiming face folds away, the rows do not).
 * On a hit: the inputs pay ({@code isRecipeInputEqual(T, F, ...)} :118), the outputs go
 * to the player or spawn above the anvil ({@code ST.add / ST.place} :120), the player
 * exhausts by {@code totalPower / 5000} (:122 — the GT6 "fatigue" face: working the
 * anvil makes you hungry, {@link Player#causeFoodExhaustion}), and the ANVIL wears by
 * {@code max(10000, divup(max(1, totalPower), 4))} durability units (:124-125). At <= 0
 * the anvil BREAKS: up to 63 scrap pieces of the smashing target material drop
 * ({@code OP.scrapGt 48+rng(16)} :128) and the block vanishes (:129). The 10000-unit
 * floor means the row eUt/duration magnitudes never matter for the wear — one working
 * hit costs exactly one displayed durability point (the tooltip {@code D:} count, :89).
 *
 * <h2>Port deviations (declared)</h2>
 * <ul>
 * <li>The strike rides the right-click use() face (the port has no left-click tool
 *     dispatch; the hopper screwdriver/wrench precedent) with the slot-state
 *     disambiguator: hammer + occupied slots = strike, hammer + both slots empty = the
 *     :240 hammer-storage arm, anything else = the put/take chain.</li>
 * <li>{@code UT.Entities.getDurabilityUse} (:124 — the haste/fatigue entity-stat
 *     modifiers on the wear) folds to the identity — the fatigue face the port carries
 *     is the food exhaustion half above; the hammer's own wear is the family one-point
 *     mapping (GT6BuilderWandItem.payClick form).</li>
 * <li>The magnifying-glass durability probe (:136-139) and the client visual data
 *     (mShapeA/B material rendering :284-400, the placed-hammer shape) are the render/
 *     tool pool cuts — the magnifier tool does not exist in the port.</li>
 * <li>The placed-block anvil sound (:277-282) folds onto SoundType.STONE (both rows
 *     register aUtilStone).</li>
 * </ul>
 *
 * <p>The {@code aPlayer == null} arm is the RCON/offline channel (the tap
 * {@code activateChain} precedent): the upstream {@code SIDES_TOP_HORIZONTAL ||
 * aPlayer == null} strike gate (:98) accepts any side without a player, and every
 * method returns a human-readable report instead of a chat line.
 */
public class GT6AnvilBlockEntity extends TileEntityBase03TicksAndSync {

	/** The two working halves (upstream {@code getDefaultInventory :416} — {@code new ItemStack[2]}). */
	public static final int SLOTS = 2;

	/** The upstream {@code NBT_DURABILITY} key (readFromNBT2 :70 / writeToNBT2 :78). */
	public static final String NBT_DURABILITY = "gt.durability";

	/** The bottom-legs bound — upstream {@code aHitY < PX_P[4]} (:220, PX_P = px/16). */
	public static final float LEGS_BOUND = 4.0F / 16.0F;

	/** The wear floor — upstream {@code Math.max(10000, ...)} (:124): one hit = one displayed point. */
	public static final long WEAR_FLOOR_PER_HIT = 10000;

	/** The exhaustion divisor — upstream {@code totalPower / 5000.0F} (:122). */
	public static final long EXHAUST_DIVISOR = 5000;

	/** The broken-anvil scrap rain — upstream {@code 48 + rng(16)} (:128). */
	public static final int SCRAP_MIN = 48, SCRAP_SPAN = 16;

	/** The findRecipe stack-size argument — the kitchen {@code RECIPE_SIZE} (V[1] = 32) form. */
	public static final long RECIPE_SIZE = 32;

	/** The remaining anvil durability in upstream units (default 10000 — readFromNBT2 :70). */
	protected long mDurability = 10000;

	/** The 2-slot working surface. */
	protected final net.minecraftforge.items.ItemStackHandler mInventory =
			new net.minecraftforge.items.ItemStackHandler(SLOTS);

	/** The output-chance RNG — upstream rides RNGSUS; the seam keeps the Bernoulli rows testable. */
	protected Random mRandom = new Random();

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GT6AnvilBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GTBarrelBlockEntity null-type form). */
	public GT6AnvilBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : GT6Anvils.ANVIL_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GTAnvilBlock tAnvil) {
			mDurability = tAnvil.durability();
		}
	}

	/** The test seam for the chance-bearing rows (upstream RNGSUS). */
	void setRandomForTest(Random aRandom) {
		mRandom = aRandom;
	}

	/** The live inventory view (the onRemove pop + the offline fixtures). */
	public net.minecraftforge.items.ItemStackHandler inventory() {
		return mInventory;
	}

	/** The remaining durability in upstream units (the magnifying-glass read :137). */
	public long durability() {
		return mDurability;
	}

	/** The displayed durability points — upstream {@code divup(mDurability, 10000)} (:89/:137). */
	public long durabilityPoints() {
		return (mDurability + 9999) / 10000;
	}

	// ---------------------------------------------------------------------------
	// the hammer classification (the #gt6:tools/hard_hammer tag + the ToolAction)
	// ---------------------------------------------------------------------------

	/** The hammer membership — the tag is the open face, the ToolAction the formal one. */
	public static boolean isHammer(@Nullable ItemStack aStack) {
		return aStack != null && !aStack.isEmpty()
				&& (aStack.is(GT6ItemTags.TOOLS_HARD_HAMMER) || aStack.canPerformAction(GT6ToolActions.HAMMER));
	}

	/** The upstream :223 {@code tHasHammer} — a hammer STORED in either working half. */
	public boolean hasStoredHammer() {
		return isHammer(mInventory.getStackInSlot(0)) || isHammer(mInventory.getStackInSlot(1));
	}

	/**
	 * The insert gate (upstream canInsertItem2 :419): only rows of the two anvil maps, and
	 * never while a hammer occupies a half (the hammer-in-slot storage arm :148/:152).
	 */
	public boolean canInsert(ItemStack aStack) {
		if (isHammer(mInventory.getStackInSlot(0)) || isHammer(mInventory.getStackInSlot(1))) return false;
		return containsInput(aStack, GT6RecipeMaps.ANVIL) || containsInput(aStack, GT6RecipeMaps.ANVIL_BEND);
	}

	/** The upstream {@code RecipeMap.containsInput} port — an item match on any row input (linear scan). */
	public static boolean containsInput(ItemStack aStack, RecipeMap aMap) {
		if (aStack == null || aStack.isEmpty()) return false;
		for (Recipe tRecipe : aMap.mRecipeList) {
			for (ItemStack tInput : tRecipe.mInputs) {
				if (tInput != null && !tInput.isEmpty() && ItemStack.isSameItemSameTags(tInput, aStack)) return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// the working strike (upstream onToolClick2 :94-141)
	// ---------------------------------------------------------------------------

	/**
	 * The hammer strike. {@code aSide}: 1 = top (the ANVIL map), 2-5 = horizontal (the
	 * ANVIL_BEND map), 0 = bottom (only the null-player arm passes — the upstream
	 * {@code SIDES_TOP_HORIZONTAL || aPlayer == null} gate :98). Returns the report.
	 */
	public String hammerStrike(@Nullable Player aPlayer, byte aSide) {
		if (!isServerSide()) return "client side";
		if (aPlayer != null && (aSide == 0 || aSide == 6)) return "strike the top or a side (not the bottom)";
		if (!mInventory.getStackInSlot(0).isEmpty() || !mInventory.getStackInSlot(1).isEmpty()) {
			RecipeMap tMap = aSide == 1 ? GT6RecipeMaps.ANVIL : GT6RecipeMaps.ANVIL_BEND; // :99-116 (the folded selector)
			ItemStack[] tInputs = new ItemStack[] {mInventory.getStackInSlot(0), mInventory.getStackInSlot(1)};
			Recipe tRecipe = tMap.findRecipe(null, RECIPE_SIZE, ItemStack.EMPTY, new net.minecraftforge.fluids.FluidStack[0], tInputs);
			if (tRecipe != null && tRecipe.isRecipeInputEqual(true, false, new net.minecraftforge.fluids.FluidStack[0], tInputs)) { // :118
				int tGiven = 0;
				for (ItemStack tOutput : tRecipe.getOutputs(mRandom, 1)) { // the real chance semantics (RNGSUS)
					if (tOutput == null || tOutput.isEmpty()) continue;
					if (aPlayer != null && giveToPlayer(aPlayer, tOutput)) {tGiven += tOutput.getCount(); continue;}
					spawnAbove(tOutput.copy());
					tGiven += tOutput.getCount();
				}
				// :122 — the fatigue face: working the anvil makes you hungry
				long tTotalPower = tRecipe.getAbsoluteTotalPower();
				if (aPlayer != null) aPlayer.causeFoodExhaustion(Math.max(1, tTotalPower) / (float)EXHAUST_DIVISOR);
				// :124-125 — the anvil wears; getDurabilityUse folds to the identity (class doc)
				mDurability -= Math.max(WEAR_FLOOR_PER_HIT, (Math.max(1, tTotalPower) + 3) / 4);
				if (aPlayer != null) {
					ItemStack tHeld = aPlayer.getMainHandItem();
					if (isHammer(tHeld)) payHammerWear(aPlayer, tHeld); // the hammer's one-point wear rides the working hit
				}
				if (mDurability <= 0) { // :126-130 — the anvil breaks into a scrap rain
					breakAnvil();
					return "the anvil broke (+" + tGiven + " output items)";
				}
				setChanged();
				return "worked: " + tGiven + " output items (durability " + durabilityPoints() + ")";
			}
			return "no matching recipe on the working surface";
		}
		return "the working surface is empty";
	}

	/**
	 * :126-130 — the scrap rain ({@code OP.scrapGt 48+rng(16)} of the smashing target) and
	 * the block vanishes ({@code setToAir}; the onRemove pop does NOT fire — the BE is
	 * removed after the rain, the working surface is empty by construction on the strike
	 * path and any residue stays lost, the upstream :129 verbatim).
	 */
	private void breakAnvil() {
		if (hasLevel() && getLevel() != null) {
			dropScrapRain(SCRAP_MIN + mRandom.nextInt(SCRAP_SPAN));
			getLevel().playSound(null, getBlockPos(), SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, 1.0F); // SFX.MC_BREAK
			getLevel().removeBlock(getBlockPos(), false);
		}
	}

	/** The scrap rain drop (the ST.drop form — centre of the block space, zero delta). */
	private void dropScrapRain(int aCount) {
		gregapi.oredict.OreDictMaterial tTarget = mMaterialSmashingTarget();
		if (tTarget == null || !hasLevel() || getLevel() == null) return;
		net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tHandle =
				gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.scrapGt, tTarget);
		if (tHandle == null) return; // no scrap item for the target — the drop folds away (the mat() → null convention)
		ItemStack tStack = new ItemStack(tHandle.get(), aCount); // 48..63 — one stack by construction
		BlockPos tPos = getBlockPos();
		ItemEntity tEntity = new ItemEntity(getLevel(), tPos.getX() + 0.5, tPos.getY() + 0.5, tPos.getZ() + 0.5, tStack);
		tEntity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); // the p26 drop rule — no random scatter
		getLevel().addFreshEntity(tEntity);
	}

	@Nullable
	private gregapi.oredict.OreDictMaterial mMaterialSmashingTarget() {
		return hasLevel() && getLevel() != null && getBlockState().getBlock() instanceof GTAnvilBlock tAnvil
				? tAnvil.material().mTargetSmashing.mMaterial : null;
	}

	/**
	 * The :120 ST.add give-back — ALL-OR-NOTHING (upstream ST.java:1074-1093: the FULL
	 * stack must fit or nothing moves, which is exactly what keeps the :120 ST.place
	 * tail duplication-free). The vanilla {@code Inventory.add} is PARTIAL-fill (it
	 * returns true on ANY progress, S5 review R1): a naive true/false gate over it would
	 * let a nearly-full bag absorb part of the stack while the false-return tail spawns
	 * the FULL copy above — bag + ground from one output. So the fit is decided FIRST by
	 * {@link #freeCapacity}, and the real add only runs when the whole stack fits.
	 * Package-private: the R1 boundary test walks it (same package).
	 */
	boolean giveToPlayer(Player aPlayer, ItemStack aStack) {
		if (aStack.isEmpty()) return true;
		Inventory tInventory = aPlayer.getInventory();
		if (freeCapacity(tInventory, aStack) < aStack.getCount()) return false; // all-or-nothing
		ItemStack tRemainder = aStack.copy();
		tInventory.add(tRemainder);
		return tRemainder.isEmpty(); // belt-and-braces: the simulation above matches add()
	}

	/**
	 * The total remaining capacity of the main inventory ({@code items}, the 36-slot band
	 * Inventory.add fills) for {@code aStack}: empty slots take a full max stack, matching
	 * non-full slots their headroom. The capacity math is add-order-independent, so one
	 * pass reproduces the two-pass vanilla fill exactly.
	 */
	private static int freeCapacity(Inventory aInventory, ItemStack aStack) {
		int rCapacity = 0;
		for (int i = 0, n = aInventory.items.size(); i < n; i++) {
			ItemStack tSlot = aInventory.items.get(i);
			if (tSlot.isEmpty()) {
				rCapacity += aStack.getMaxStackSize();
			} else if (ItemStack.isSameItemSameTags(tSlot, aStack) && tSlot.getCount() < tSlot.getMaxStackSize()) {
				rCapacity += tSlot.getMaxStackSize() - tSlot.getCount();
			}
		}
		return rCapacity;
	}

	/** The :120 ST.place — outputs spawn above the anvil (y + 1.2), zero delta. */
	private void spawnAbove(ItemStack aStack) {
		if (!hasLevel() || getLevel() == null || aStack.isEmpty()) return;
		BlockPos tPos = getBlockPos();
		ItemEntity tEntity = new ItemEntity(getLevel(), tPos.getX() + 0.5, tPos.getY() + 1.2, tPos.getZ() + 0.5, aStack);
		tEntity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		getLevel().addFreshEntity(tEntity);
	}

	// ---------------------------------------------------------------------------
	// the activation chain (upstream onBlockActivated3 :218-274 → activateChain)
	// ---------------------------------------------------------------------------

	/**
	 * The whole server-side activation chain. {@code aHeld} = the clicked hand's stack;
	 * {@code aHitX/Y/Z} the hit coordinates inside the block. The hammer-with-occupied-
	 * slots click routes to {@link #hammerStrike} (the port strike face, class doc); the
	 * rest is the upstream put/take chain. Returns the report (the RCON channel).
	 */
	public String activateChain(@Nullable Player aPlayer, byte aSide, @Nullable ItemStack aHeld,
			float aHitX, float aHitY, float aHitZ) {
		if (!isServerSide()) return "client side";
		if (aHitY < LEGS_BOUND) return "the anvil legs (no action)"; // :220
		byte tSlot = pickSlot(aHitX, aHitZ); // :222
		boolean tTop = aSide == 1; // SIDES_TOP

		// the port strike face: hammer in hand + something on the surface = the working hit
		if (isHammer(aHeld) && (!mInventory.getStackInSlot(0).isEmpty() || !mInventory.getStackInSlot(1).isEmpty())) {
			return hammerStrike(aPlayer, aSide);
		}

		if (tTop) {
			// :226-237 — a STORED hammer means the top click is a give-back arm
			if (hasStoredHammer()) {
				for (int i = 0; i < SLOTS; i++) {
					ItemStack tSlotStack = mInventory.getStackInSlot(i);
					if (!tSlotStack.isEmpty() && (aPlayer == null || giveToPlayer(aPlayer, tSlotStack))) {
						mInventory.setStackInSlot(i, ItemStack.EMPTY);
						setChanged();
						return "collected " + tSlotStack.getCount() + "x " + tSlotStack.getItem();
					}
				}
				return "the hammer blocks the surface";
			}
			// :239-242 — the held item places into the picked half (hammer only onto BOTH-empty)
			if (aHeld != null && !aHeld.isEmpty()
					&& ((isHammer(aHeld) && mInventory.getStackInSlot(0).isEmpty() && mInventory.getStackInSlot(1).isEmpty())
						|| containsInput(aHeld, GT6RecipeMaps.ANVIL)
						|| containsInput(aHeld, GT6RecipeMaps.ANVIL_BEND))) {
				if (moveIntoSlot(tSlot, aHeld)) {
					playClick(); // :240 playClick
					return "placed " + aHeld.getCount() + "x " + aHeld.getItem();
				}
				return "the half is occupied";
			}
			// :243-247 — no valid held stack: the picked half comes back
			ItemStack tPicked = mInventory.getStackInSlot(tSlot);
			if (!tPicked.isEmpty() && (aPlayer == null || giveToPlayer(aPlayer, tPicked))) {
				mInventory.setStackInSlot(tSlot, ItemStack.EMPTY);
				setChanged();
				return "collected " + tPicked.getCount() + "x " + tPicked.getItem();
			}
			return tPicked.isEmpty() ? "nothing to collect" : "the inventory is full";
		}

		// :249-270 — the side click even-splits a single occupied half over both (the
		// slotNull-only-clears-zero semantics, TileEntityBase05Inventories.java:95)
		ItemStack tFirst = mInventory.getStackInSlot(0), tSecond = mInventory.getStackInSlot(1);
		if (!tFirst.isEmpty() && tSecond.isEmpty()) {
			return splitArm(0, aPlayer);
		}
		if (tFirst.isEmpty() && !tSecond.isEmpty()) {
			return splitArm(1, aPlayer);
		}
		return "no action";
	}

	/** The :222 slot pick — the Z-axis facing splits by hitX, the X-axis facing by hitZ. */
	private byte pickSlot(float aHitX, float aHitZ) {
		net.minecraft.core.Direction tFacing = getBlockState().hasProperty(GTAnvilBlock.FACING)
				? getBlockState().getValue(GTAnvilBlock.FACING) : net.minecraft.core.Direction.NORTH;
		boolean tAxisZ = tFacing.getAxis() == net.minecraft.core.Direction.Axis.Z;
		return (tAxisZ ? aHitX : aHitZ) < 0.5F ? (byte) 0 : (byte) 1;
	}

	/**
	 * The even-split arm body ({@code aSource} = the occupied half): the odd unit goes back
	 * to the player (or spawns above — the :251 ST.give with its drop-if-full tail), then
	 * the rest halves evenly; {@code slotNull} clears only a <= 0 half (:95).
	 */
	private String splitArm(int aSource, @Nullable Player aPlayer) {
		int tOther = 1 - aSource;
		ItemStack tStack = mInventory.getStackInSlot(aSource);
		if (tStack.getCount() % 2 != 0) {
			ItemStack tOdd = tStack.split(1);
			if (aPlayer == null || !giveToPlayer(aPlayer, tOdd)) spawnAbove(tOdd);
		}
		if (tStack.getCount() > 1) {
			int tHalf = tStack.getCount() / 2;
			tStack.setCount(tHalf);
			mInventory.setStackInSlot(tOther, tStack.copy());
		}
		if (tStack.getCount() <= 0) mInventory.setStackInSlot(aSource, ItemStack.EMPTY); // slotNull :95
		setChanged();
		return "split the working surface";
	}

	/** The :240 ST.move — the held stack (whole) into the picked half. Consumes from {@code aHeld}. */
	private boolean moveIntoSlot(byte aSlot, ItemStack aHeld) {
		ItemStack tSlot = mInventory.getStackInSlot(aSlot);
		if (tSlot.isEmpty()) {
			mInventory.setStackInSlot(aSlot, aHeld.copy());
			aHeld.setCount(0);
			return true;
		}
		if (ItemStack.isSameItemSameTags(tSlot, aHeld) && tSlot.getCount() + aHeld.getCount() <= tSlot.getMaxStackSize()) {
			tSlot.grow(aHeld.getCount());
			aHeld.setCount(0);
			return true;
		}
		return false;
	}

	/** The click pip (:240 playClick — the wooden click). */
	private void playClick() {
		if (hasLevel() && getLevel() != null) {
			getLevel().playSound(null, getBlockPos(), SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	/** The hammer's own wear — the family one-point mapping (GT6BuilderWandItem.payClick form). */
	private void payHammerWear(@Nullable Player aPlayer, @Nullable ItemStack aHammer) {
		if (aPlayer == null || aHammer == null || aHammer.isEmpty()) return;
		//? if forge {
		aHammer.hurtAndBreak(1, aPlayer, p -> p.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
		//?} else {
		/*aHammer.hurtAndBreak(1, aPlayer, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :68-73 / writeToNBT2 :76-79; the inventory rides the handler)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_DURABILITY, mDurability);
		//? if forge {
		aNBT.put("inventory", mInventory.serializeNBT());
		//?} else {
		/*aNBT.put("inventory", mInventory.serializeNBT(NBT_ACCESS)); // 21.1: ItemStackHandler NBT takes the registries
		 *///?}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_DURABILITY)) mDurability = aNBT.getLong(NBT_DURABILITY);
		//? if forge {
		if (aNBT.contains("inventory")) mInventory.deserializeNBT(aNBT.getCompound("inventory"));
		//?} else {
		/*if (aNBT.contains("inventory")) mInventory.deserializeNBT(NBT_ACCESS, aNBT.getCompound("inventory"));
		 *///?}
	}

	@Override
	public String getTileEntityName() {
		return "anvil";
	}
}
