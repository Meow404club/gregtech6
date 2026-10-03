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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import gregtech6.gui.GTViewerJump;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Sifting Table — the zero-energy manual sifting block (task sifting-table-family),
 * the port of the upstream {@code MultiTileEntitySiftingTable} (tmp/gt6-1.7.10
 * .../tools/MultiTileEntitySiftingTable.java:54-456), the manual-devices chain tail
 * (research.manual-devices-port; Loader_MultiTileEntities.java:2227, ID 32702, single
 * variant, NBT_MATERIAL ANY.Steel + NBT_RECIPEMAP RM.Sifting). NO GUI, no tanks, no
 * energy face: the upstream tooltip line is {@code LH.NO_GUI_CLICK_TO_INTERACT} (:79)
 * and the machine is the zero-energy manual tier of the P28 lineup.
 *
 * <h2>The thirteen-slot surface (:441-443)</h2>
 * {@code mInventory[13]} — slot 0 is the input, slots 1..12 the output ladder; the
 * stack limit is 1 everywhere ({@code getInventoryStackLimit} :443), so one working
 * cycle holds up to twelve distinct output stacks.
 *
 * <h2>The activation + watch-work cycle (:236-264 + :275-299)</h2>
 * A TOP click with material on the table sets the ACTIVE flag (:281); a top click on
 * the 2px corner ({@code PX_P[2]}, :279) is the NEI jump seat (the client arm, the
 * {@link #openNei} router). Every 5th tick with the flag set, the table looks for a
 * watching player — the upstream {@code UT.Entities.getPlayersWithLastTarget} trace,
 * ported as the range scan + {@code player.pick} hit seam below ({@link #watchers},
 * the declared equivalence, javadoc there) — and while watched: each watching player
 * advances the progress by one hit ({@code mClickCount}, the haste/fatigue potion
 * modifiers fold to the identity — the anvil fold precedent,
 * GT6AnvilBlockEntity.java:84-87), at four hits (or a creative watcher, :245
 * {@code hasInfiniteItems}) the {@code RM.Sifting} row executes: the input pays
 * (:252-253), the outputs land positionally on slots 1..12 capped at twelve (:255),
 * the worker exhausts {@code totalPower / 1000} (:256) and swings (:257). A material
 * with NO row relocates to the first free output slot instead (:247-249). The arm
 * only counts while ALL output slots are clear (:242). Unwatched, the flag drops
 * (:238 — the loop never re-sets it) and the table sleeps.
 *
 * <h2>The collect arm (:286-288)</h2>
 * Any non-top click takes the output slots back: {@code ST.give} to the bag with the
 * full-bag remainder spawning above the table.
 *
 * <h2>Port deviations (declared)</h2>
 * <ul>
 * <li>The watch arm: upstream rides the client-traced "last targeted TE" channel
 *     ({@code UT.Entities.getPlayersWithLastTarget}); the port scans the level's
 *     players within {@link #WATCH_RANGE} and requires a {@code player.pick} ray hit
 *     on THIS block position — the 1.20.1 equivalence seam (Entity.pick
 *     tmp/vanilla-1.20.1 .../Entity.java:1496), the range fixed at 8 blocks (the
 *     vanilla interaction reach bands + margin; the client's per-tick trace folded
 *     into a server-side scan).</li>
 * <li>The client DIG_SAND loop sound (:266-268) and the displayed input/output pile
 *     rendering (:336-424, the material-dust visual data) are the render pool cut —
 *     no client visual data sync rides this BE.</li>
 * <li>The {@code NBT_RECIPEMAP} rebind (:65) folds to the hardwired
 *     {@code GT6RecipeMaps.SIFTING} — the single registration row (the anvil two-map
 *     freeze form).</li>
 * <li>The progress arm requires material on the table; upstream also counts hits over
 *     an empty table (a no-op quirk, the findRecipe miss then re-arms to zero).</li>
 * </ul>
 *
 * <p>The {@code aPlayer == null} arms are the RCON/offline channel (the anvil
 * {@code activateChain} precedent): every method returns a human-readable report
 * instead of a chat line, and the collect arm spawns above the table.
 */
public class GT6SiftingTableBlockEntity extends TileEntityBase03TicksAndSync {

	/** The working surface size — slot 0 input + slots 1..12 output (upstream :441). */
	public static final int SLOTS = 13;
	/** The output-ladder width (the :255 {@code min(outputs, 12)} cap). */
	public static final int OUTPUT_SLOTS = 12;
	/** The input slot index (upstream slot(0), :88). */
	public static final int INPUT_SLOT = 0;

	/** The upstream {@code NBT_STATE} key (CS.java:1173; readFromNBT2 :63 / writeToNBT2 :71). */
	public static final String NBT_STATE = "gt.state";
	/** The upstream {@code NBT_PROGRESS} key (CS.java:1175; readFromNBT2 :64 / writeToNBT2 :72). */
	public static final String NBT_PROGRESS = "gt.progress";

	/** The state bits — upstream {@code B[0]} has-input / {@code B[1]} has-output / {@code B[2]} active (:55-56). */
	public static final byte HAS_INPUT = 1, HAS_OUTPUT = 2, ACTIVE = 4;

	/** The hits per working cycle — upstream {@code >= 4*pot2Fatique} with the fatigue fold identity (:245). */
	public static final int HITS_PER_WORK = 4;
	/** The {@code UT.Code.bind7} progress cap (:245). */
	public static final int PROGRESS_CAP = 127;
	/** The NEI corner bound — upstream {@code PX_P[2]} (:279). */
	public static final float CORNER_BOUND = 2.0F / 16.0F;
	/** The watch range — the declared 1.20.1 trace seam (the class doc deviation). */
	public static final double WATCH_RANGE = 8.0;
	/** The exhaustion divisor — upstream {@code getAbsoluteTotalPower() / 1000.0} (:256). */
	public static final float EXHAUST_DIVISOR = 1000.0F;
	/** The findRecipe stack-size argument — the kitchen {@code RECIPE_SIZE} (V[1] = 32) form. */
	public static final long RECIPE_SIZE = 32;
	/** The shared zero-fluid argument (the anvil {@code new FluidStack[0]} form). */
	public static final net.minecraftforge.fluids.FluidStack[] NO_FLUIDS = new net.minecraftforge.fluids.FluidStack[0];

	/** The thirteen-slot surface (upstream :441; the stack limit rides {@link ItemStackHandler#getSlotLimit}). */
	protected final ItemStackHandler mInventory = new ItemStackHandler(SLOTS) {
		@Override
		public int getSlotLimit(int aSlot) {
			return 1; // the upstream getInventoryStackLimit :443
		}
	};

	/** The visual/working state bits (upstream {@code mState} :56). */
	protected byte mState = 0;
	/** The progress hits toward the next cycle (upstream {@code mClickCount} :56). */
	protected byte mClickCount = 0;

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GT6SiftingTableBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GT6AnvilBlockEntity null-type form). */
	public GT6SiftingTableBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : gregtech6.registry.GT6SiftingTables.SIFTING_TABLE_BE.get(), aPos, aState);
	}

	/** The live inventory view (the collect arm + the offline fixtures). */
	public ItemStackHandler inventory() {
		return mInventory;
	}

	/** The ACTIVE flag read (the :281 seat — the tick cycle runs only while set). */
	public boolean isActive() {
		return (mState & ACTIVE) != 0;
	}

	// ---------------------------------------------------------------------------
	// the NEI corner (the :279 PX_P[2] seat + the :292-295 client arm)
	// ---------------------------------------------------------------------------

	/** The :279 corner probe — both in-plane hit coords within the 2px seat. */
	public static boolean neiCorner(float aHitX, float aHitZ) {
		return aHitX <= CORNER_BOUND && aHitZ <= CORNER_BOUND;
	}

	/**
	 * The client NEI corner arm — upstream {@code mRecipes.openNEI()} (:293, the
	 * Recipe.openNEI face): the viewer jump. Silent when no viewer is installed or its
	 * runtime is not ready — the GTViewerJump router bottom-arms both.
	 */
	protected void openNei() {
		GTViewerJump.openRecipeMapPage(GT6RecipeMaps.SIFTING);
	}

	// ---------------------------------------------------------------------------
	// the tick cycle (upstream onTick2 :83-270)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return; // the client DIG_SAND loop sound = the render cut (the class doc)
		// :85-163 — the state recompute
		mState &= ~(HAS_INPUT | HAS_OUTPUT);
		if (!mInventory.getStackInSlot(INPUT_SLOT).isEmpty()) mState |= HAS_INPUT;
		for (int i = 1; i < SLOTS; i++) if (!mInventory.getStackInSlot(i).isEmpty()) { mState |= HAS_OUTPUT; break; }
		// :236-264 — the 5th-tick watch cycle
		if (aTimer % 5 == 0 && isActive()) {
			tickWork(watchers());
		}
	}

	/**
	 * One 5th-tick work pass over the watcher list — the :236-264 loop body (the live
	 * caller is {@link #onTick}; the offline fixture drives this directly, the test seam).
	 */
	public void tickWork(List<Player> aWatchers) {
		if (aWatchers.isEmpty()) {
			mState &= ~ACTIVE; // :238 — the loop never re-sets the flag = the table sleeps
			return;
		}
		for (Player tPlayer : aWatchers) siftWork(tPlayer); // the per-watcher arm (the upstream loop body)
	}

	/**
	 * The watch scan — the declared 1.20.1 equivalence seam (the class doc deviation):
	 * every player within {@link #WATCH_RANGE} of the table centre whose
	 * {@code pick(WATCH_RANGE)} ray lands on THIS block position. Upstream consumed the
	 * client's per-tick "last targeted TE" trace; the port re-traces server-side.
	 */
	protected List<Player> watchers() {
		if (!hasLevel() || getLevel() == null) return List.of();
		List<Player> rList = new ArrayList<>();
		BlockPos tPos = getBlockPos();
		for (Player tPlayer : getLevel().players()) {
			if (tPlayer.distanceToSqr(tPos.getX() + 0.5, tPos.getY() + 0.5, tPos.getZ() + 0.5) > WATCH_RANGE * WATCH_RANGE) continue;
			HitResult tHit = tPlayer.pick(WATCH_RANGE, 1.0F, false);
			if (tHit.getType() == HitResult.Type.BLOCK && tPos.equals(((BlockHitResult) tHit).getBlockPos())) rList.add(tPlayer);
		}
		return rList;
	}

	/**
	 * One watched arm — the upstream :241-263 loop body. Each watching player advances
	 * the progress; a completion pays the row and lands the outputs. Test seam: the
	 * watcher list is injected (the offline fixture drives this directly, the tick
	 * cycle above is the live caller).
	 */
	public void siftWork(@Nullable Player aPlayer) {
		mState |= ACTIVE; // :239 — a watched arm keeps the cycle armed
		boolean tOutputsClear = true;
		for (int i = 1; i < SLOTS; i++) if (!mInventory.getStackInSlot(i).isEmpty()) { tOutputsClear = false; break; } // :241-242
		ItemStack aStack = mInventory.getStackInSlot(INPUT_SLOT); // :243
		if (!tOutputsClear || aStack.isEmpty()) return; // :245 — the temp gate (the input-present micro-fold, class doc)
		mClickCount = (byte) Math.min(PROGRESS_CAP, mClickCount + 1); // :245 — pot1Haste folds to +1
		boolean tCreative = aPlayer != null && aPlayer.getAbilities().instabuild; // :245 hasInfiniteItems
		if (mClickCount < HITS_PER_WORK && !tCreative) return;
		mClickCount = 0; // :246
		Recipe tRecipe = GT6RecipeMaps.SIFTING == null ? null : GT6RecipeMaps.SIFTING.findRecipe(null, RECIPE_SIZE, ItemStack.EMPTY, NO_FLUIDS, aStack); // :247
		if (tRecipe == null) {
			// :248-249 — no row: the input relocates to the first free output slot
			for (int i = 1; i < SLOTS; i++) if (mInventory.getStackInSlot(i).isEmpty()) {
				mInventory.setStackInSlot(i, aStack.copy());
				mInventory.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY); // slotKill(0)
				setChanged();
				return;
			}
			return; // all twelve slots occupied — the input stays on the table (the :249 all-fail form)
		}
		if (!tRecipe.isRecipeInputEqual(true, false, NO_FLUIDS, aStack)) return; // :252 — the pay gate
		if (aStack.getCount() <= 0) mInventory.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY); // :253 slotKill(0)
		ItemStack[] tOutputs = tRecipe.getOutputs(); // :254 — the deterministic overload (the upstream call face)
		for (int i = 0, j = Math.min(tOutputs.length, OUTPUT_SLOTS); i < j; i++) addStackToSlot(i + 1, tOutputs[i]); // :255
		if (aPlayer != null) aPlayer.causeFoodExhaustion(exhaustionOf(tRecipe.getAbsoluteTotalPower())); // :256
		if (aPlayer != null) aPlayer.swing(InteractionHand.MAIN_HAND); // :257
		setChanged(); // :258 updateInventory — the pile faces are the render cut, the chunk sync suffices
	}

	/** The :256 exhaustion value — totalPower / 1000 (the test seam pins the math). */
	public static float exhaustionOf(long aTotalPower) {
		return Math.max(1, aTotalPower) / EXHAUST_DIVISOR;
	}

	/** The :255 addStackToSlot — the ladder is empty by the temp gate, so a positional fill is exact. */
	private void addStackToSlot(int aSlot, ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return;
		if (mInventory.getStackInSlot(aSlot).isEmpty()) mInventory.setStackInSlot(aSlot, aStack.copy());
		// the occupied-slot tail drops silently — the upstream return-ignored form (:255)
	}

	// ---------------------------------------------------------------------------
	// the activation chain (upstream onBlockActivated3 :275-299)
	// ---------------------------------------------------------------------------

	/**
	 * The whole activation chain. {@code aSide}: 1 = top (place/activate/NEI seat),
	 * anything else = the collect arm. {@code aHeld} = the clicked hand's stack;
	 * {@code aHitX/Y/Z} the hit coordinates inside the block. Returns the report (the
	 * RCON channel).
	 */
	public String activateChain(@Nullable Player aPlayer, byte aSide, @Nullable ItemStack aHeld,
			float aHitX, float aHitY, float aHitZ) {
		if (!isServerSide()) {
			if (aSide == 1 && neiCorner(aHitX, aHitZ)) openNei(); // :290-296 — the client arm
			return "client side";
		}
		if (aSide == 1) {
			if (neiCorner(aHitX, aHitZ)) return "NEI corner (the recipe-viewer jump is the client arm)"; // :279
			if (!mInventory.getStackInSlot(INPUT_SLOT).isEmpty()) {
				mState |= ACTIVE; // :281 — the material is already down, this click arms the cycle
				return "activated — keep watching the table";
			}
			mClickCount = 0; // :283
			if (aHeld != null && !aHeld.isEmpty() && canInsertItem(INPUT_SLOT, aHeld, aSide)) { // :284 — the :452 gate
				ItemStack tPlaced = aHeld.copy();
				tPlaced.setCount(1); // the slot limit 1 (:443) — the ST.move one-unit form
				mInventory.setStackInSlot(INPUT_SLOT, tPlaced);
				aHeld.shrink(1);
				setChanged();
				return "placed 1x " + tPlaced.getItem();
			}
			return aHeld == null || aHeld.isEmpty() ? "nothing held to place" : "that is not sifting material";
		}
		// :286-288 — the collect arm: the output ladder comes back (bag first, remainder above)
		int tCollected = 0;
		for (int i = 1; i < SLOTS; i++) {
			ItemStack tStack = mInventory.getStackInSlot(i);
			if (tStack.isEmpty()) continue;
			mInventory.setStackInSlot(i, ItemStack.EMPTY); // slotTake
			tCollected += tStack.getCount();
			give(aPlayer, tStack);
		}
		if (tCollected > 0) {
			setChanged();
			return "collected " + tCollected + " output items";
		}
		return "nothing to collect";
	}

	/**
	 * The :287 {@code ST.give} — bag first, the full-bag remainder spawns above the
	 * table (y + 1, centre; the vanilla partial add + remainder-drop = no duplication).
	 */
	private void give(@Nullable Player aPlayer, ItemStack aStack) {
		if (aPlayer == null) { spawnAbove(aStack); return; }
		ItemStack tRemainder = aStack.copy();
		aPlayer.getInventory().add(tRemainder);
		if (!tRemainder.isEmpty()) spawnAbove(tRemainder);
	}

	/** The drop tail (the ST.give coords form — centre of the block space, one block up, zero delta). */
	private void spawnAbove(ItemStack aStack) {
		if (!hasLevel() || getLevel() == null || aStack.isEmpty()) return;
		BlockPos tPos = getBlockPos();
		net.minecraft.world.entity.item.ItemEntity tEntity =
				new net.minecraft.world.entity.item.ItemEntity(getLevel(), tPos.getX() + 0.5, tPos.getY() + 1.0, tPos.getZ() + 0.5, aStack);
		tEntity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); // the p26 drop rule — no random scatter
		getLevel().addFreshEntity(tEntity);
	}

	// ---------------------------------------------------------------------------
	// the insert/extract gates (upstream canInsertItem2 :452 / canExtractItem2 :453)
	// ---------------------------------------------------------------------------

	/** The :452 insert gate — the input slot only, and only SIFTING row inputs. */
	public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {
		return aSlot == INPUT_SLOT && GT6RecipeMaps.SIFTING != null && GT6AnvilBlockEntity.containsInput(aStack, GT6RecipeMaps.SIFTING);
	}

	/** The :453 extract gate — the output ladder only. */
	public boolean canExtractItem(int aSlot, byte aSide) {
		return aSlot != INPUT_SLOT;
	}

	/** The external side view (all 13 slots, the gates above — the upstream :445-453 posture). */
	public IItemHandler newSideHandler() {
		return new SideItemHandler(this);
	}

	/** The capability face — the upstream getAccessibleSlotsFromSide2/canInsertItem2/canExtractItem2 triple (the hopper SideItemHandler form). */
	public static final class SideItemHandler implements IItemHandler {

		private final GT6SiftingTableBlockEntity mTable;

		SideItemHandler(GT6SiftingTableBlockEntity aTable) {
			mTable = aTable;
		}

		@Override
		public int getSlots() {
			return SLOTS; // the :445 ACCESSIBLE_SLOTS
		}

		@Override
		public ItemStack getStackInSlot(int aSlot) {
			return mTable.mInventory.getStackInSlot(aSlot);
		}

		@Override
		public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
			if (!mTable.canInsertItem(aSlot, aStack, (byte) 0)) return aStack;
			return mTable.mInventory.insertItem(aSlot, aStack, aSimulate);
		}

		@Override
		public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) {
			if (!mTable.canExtractItem(aSlot, (byte) 0)) return ItemStack.EMPTY;
			return mTable.mInventory.extractItem(aSlot, aAmount, aSimulate);
		}

		@Override
		public int getSlotLimit(int aSlot) {
			return 1; // the :443 stack limit
		}

		@Override
		public boolean isItemValid(int aSlot, ItemStack aStack) {
			return mTable.canInsertItem(aSlot, aStack, (byte) 0);
		}
	}

	// ---------------------------------------------------------------------------
	// the capability seam (the kitchen BE fork shape)
	// ---------------------------------------------------------------------------

	//? if forge {
	/** The cached all-sides item face (the side-less probe keeps one LazyOptional — every side answers the same handler). */
	private net.minecraftforge.common.util.LazyOptional<IItemHandler> mItemCap = net.minecraftforge.common.util.LazyOptional.of(this::newSideHandler);

	@Override
	public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
			return mItemCap.cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mItemCap.invalidate();
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 removed BlockEntity#getCapability — this member is
	// the provider seam; the GT6CapabilityWiring registerBlockEntity delegates to it.
	// No @Override: the parent method does not exist on 21.1.)
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK) {
			return (T) newSideHandler();
		}
		return null;
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :61-66 / writeToNBT2 :68-73; the inventory rides the handler)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_STATE, mState);
		aNBT.putByte(NBT_PROGRESS, mClickCount);
		//? if forge {
		aNBT.put("inventory", mInventory.serializeNBT());
		//?} else {
		/*aNBT.put("inventory", mInventory.serializeNBT(NBT_ACCESS)); // 21.1: ItemStackHandler NBT takes the registries (the anvil form)
		 *///?}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_STATE)) mState = aNBT.getByte(NBT_STATE);
		if (aNBT.contains(NBT_PROGRESS)) mClickCount = aNBT.getByte(NBT_PROGRESS);
		//? if forge {
		if (aNBT.contains("inventory")) mInventory.deserializeNBT(aNBT.getCompound("inventory"));
		//?} else {
		/*if (aNBT.contains("inventory")) mInventory.deserializeNBT(NBT_ACCESS, aNBT.getCompound("inventory"));
		 *///?}
	}

	@Override
	public String getTileEntityName() {
		return "gt.multitileentity.sifter.table"; // the upstream :456 verbatim
	}
}
