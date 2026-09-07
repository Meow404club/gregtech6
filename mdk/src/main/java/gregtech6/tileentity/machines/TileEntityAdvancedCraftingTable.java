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

package gregtech6.tileentity.machines;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.GTAdvancedCraftingTableBlock;
import gregtech6.item.GT6Circuits;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Advanced Crafting Table (task p24-act-machine) — the multi-material batch crafting
 * machine, port of upstream
 * gregapi/tileentity/tools/MultiTileEntityAdvancedCraftingTable.java:72 (753 lines,
 * 2025-maintained). ZERO energy, ZERO crafting tick: the whole machine is player-click
 * driven (upstream refill() is an empty stub :186-188 and no recipe ever auto-starts),
 * so this BE rides the 03 tick chain only for the :148-175 housekeeping (the
 * mUpdatedGrid output recompute + the flush latch), exactly like upstream onTick2.
 *
 * <p><b>The ghost-grid hybrid form</b> (decisions.p24-act-ghost-form, the card's #1
 * architecture ruling): upstream writes size-0 ghost stacks
 * ({@code ST.amount(0, stack)}, :212/:225-282) into the real crafting slots —
 * a size-0 stack cannot live in an IItemHandler slot on the port
 * ({@code ItemStack.isEmpty() == count <= 0}, the GT6Circuits.java:40-41 canon), so the
 * port carries the pattern in a bare {@link #mPattern} ItemStack[9] server backing
 * (item + components, count NORMALIZED to 1) that never touches {@link #mInventory}:
 * it cannot leak to automation, drops, or the GUI item face. The real 21-29 slots keep
 * their IItemHandler semantics (manual placement + the consumption stock); the recipe
 * lookup feeds a VIRTUAL 3x3 built as pattern-cell-or-real-slot (never "reads a ghost
 * from the real slots").
 *
 * <p><b>Declared deviations</b> (decisions.p24-act-deviations, all six confirmed + the
 * explosion-arm correction): the blueprint path (:190-207/:210-212) rides the Printer
 * pool — Paper_Blueprint_Empty/Used are unregistered here; the slot-30 whitelist is
 * therefore SELECTOR-ONLY through the frozen {@link GT6Circuits#isSelector}/{@link
 * GT6Circuits#configurationOf} public API (config inside [2, 9], upstream :515 shape —
 * GT6Circuits.java is zero-diff); the EXPLODES_IN_NONVANILLA_CRAFTING_GRID arm (:286
 * explode(4)) is cut — the vanilla RecipeManager {@code Recipe} carries no GT flag
 * position, so the arm has no consumption face and rides the "GT recipe metadata
 * carrier" pool; the ITileEntityConnectedInventory/Tank external-draw arms
 * (:306-312/:369-404/:443-449) are cut with the domain (the ACT eats only its own two
 * storage belts); the container-refill-from-connected-tank arm (:439-450) is cut with
 * it; the {@code gt:autocrafterinfinite} infinite-craft guard (:346-352) is cut — the
 * item is unported so the arm is unreachable; the mGUITexture reskin face (:74-87) is
 * cut with the ModularUI native panel layout; the {@code MultiItemTool}/achievement/
 * sound arms (:337-357/:418-426) are cut with the tool system pool — the player-free
 * consume core keeps the same inventory semantics. The monkeywrench/screwdriver MODE
 * FIELDS + NBT keys land NOW (:124-142 shape) while the tool-click interaction arm
 * rides the tool-system pool — the RCON chain flips the fields through {@code /gt6act}
 * (deviation ⑤).
 *
 * <p><b>Inventory map</b> (upstream :490-503 verbatim): 0-15 = 4x4 input belt;
 * 16-20 = 5 tool slots; 21-29 = 3x3 crafting grid (real stock); 30 = selector slot
 * (whitelisted, stack limit 1); 31 = output preview (holo — never drops, :511);
 * 32 = virtual holo double position (flush/sort buttons in the GUI, automation flush
 * target); 33 = drop slot (the only normal automation extract); 34 = neutral slot;
 * 35-70 = 9x4 storage belt. The SLOTS_36 quirk ({@code 62, 64, 63} at :494) is
 * preserved verbatim — it orders automation access only, SLOTS_CONSUMPTION (:499) is
 * its own explicit 70→0 list.
 */
public class TileEntityAdvancedCraftingTable extends TileEntityBase03TicksAndSync implements MenuProvider {

	// ---------------------------------------------------------------------------
	// constants (upstream :490-503 verbatim — the 71-slot map and walk orders)
	// ---------------------------------------------------------------------------

	public static final int INVENTORY_SIZE = 71;

	public static final int[] SLOTS              = new int[] {33};
	public static final int[] SLOTS_FLUSHING     = new int[] {33, 21, 22, 23, 24, 25, 26, 27, 28, 29};
	public static final int[] SLOTS_16           = new int[] {33,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15};
	public static final int[] SLOTS_16_FLUSHING  = new int[] {33, 21, 22, 23, 24, 25, 26, 27, 28, 29,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15};
	public static final int[] SLOTS_36           = new int[] {33, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 64, 63, 65, 66, 67, 68, 69, 70};
	public static final int[] SLOTS_36_FLUSHING  = new int[] {33, 21, 22, 23, 24, 25, 26, 27, 28, 29, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70};
	public static final int[] SLOTS_ALL          = new int[] {33,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 64, 63, 65, 66, 67, 68, 69, 70};
	public static final int[] SLOTS_ALL_FLUSHING = new int[] {33, 21, 22, 23, 24, 25, 26, 27, 28, 29,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70};

	/** The consumption priority walk — belts 70→35, then tools 20→16, then the 4x4 15→0 (upstream :499 verbatim). */
	public static final int[] SLOTS_CONSUMPTION  = new int[] {70, 69, 68, 67, 66, 65, 64, 63, 62, 61, 60, 59, 58, 57, 56, 55, 54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 44, 43, 42, 41, 40, 39, 38, 37, 36, 35, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10,  9,  8,  7,  6,  5,  4,  3,  2,  1,  0};
	/** Where container items land (upstream :500 verbatim). */
	public static final int[] SLOTS_INPUT        = new int[] { 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70};
	public static final int[] SLOTS_STORAGE      = new int[] { 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70};
	public static final int[] SLOTS_CRAFTING     = new int[] {21, 22, 23, 24, 25, 26, 27, 28, 29};
	public static final int[] SLOTS_TOOLS        = new int[] {16, 17, 18, 19, 20};

	/** Grid slot indices inside SLOTS_CRAFTING that each selector config fills with a GHOST
	 * cell (upstream :222-283 — the destination cell [3] keeps the REAL swept stack, so
	 * the effective grid is ghosts ∪ destination). Index order is the upstream case-arm
	 * order; config 2 / any out-of-band value falls into the upstream {@code default}
	 * arm (vertical 2, the stick recipe). */
	public static final int[][] PATTERN_CELLS = {
			{6},                        // 0/1/unset — the upstream default arm (vertical 2)
			{5, 4},                     // 3 — horizontal 2, the slab recipe
			{4, 1, 0},                  // 4 — 2x2
			{7, 5, 4, 1},               // 5 — star 5 (X shape)
			{5, 4, 2, 1, 0},            // 6 — 3x2, the wall/trapdoor recipes
			{7, 5, 4, 2, 1, 0},         // 7 — 6 ghosts, avoiding the armor recipes
			{8, 7, 6, 5, 2, 1, 0},      // 8 — ring 8, the furnace/chest recipes
			{8, 7, 6, 5, 4, 2, 1, 0},   // 9 — all 9 (the destination [3] excluded — it keeps the real stack)
	};

	/** The selector configs that carry a real pattern (the whitelist band, upstream :515). */
	public static final int SELECTOR_CONFIG_MIN = 2, SELECTOR_CONFIG_MAX = 9;

	/** The pattern-cells row of a selector config (default arm below 3 / above 9 — the upstream {@code switch} default). */
	public static int[] patternCells(int aConfig) {
		if (aConfig < 3 || aConfig > 9) return PATTERN_CELLS[0];
		return PATTERN_CELLS[aConfig - 2];
	}

	// ---------------------------------------------------------------------------
	// NBT keys — plain in-repo form (the oven precedent); the upstream "gt.mode.*"/
	// "gt.flush" names minus the gt. prefix that the vanilla "id" slot collision forced
	// (TileEntityOven NBT doc :141-142)
	// ---------------------------------------------------------------------------

	public static final String NBT_FACING = "facing";
	public static final String NBT_INVENTORY = "inventory";
	public static final String NBT_PATTERN = "pattern";
	public static final String NBT_BLOCKED_16 = "mode.16.blocked"; // upstream NBT_MODE+".16.blocked" :79
	public static final String NBT_BLOCKED_36 = "mode.36.blocked"; // upstream NBT_MODE+".36.blocked" :80
	public static final String NBT_FILTER_16 = "mode.16.filter";   // upstream NBT_MODE+".16.filter" :81
	public static final String NBT_FILTER_36 = "mode.36.filter";   // upstream NBT_MODE+".36.filter" :82
	public static final String NBT_FLUSH = "flush";                // upstream NBT_FLUSH :83

	// ---------------------------------------------------------------------------
	// fields (:73 minus the cut faces — mGUITexture/mDoSound/mSyncGUI ride the declared
	// deviations; the port syncs through setChanged/the vanilla two-channel sync)
	// ---------------------------------------------------------------------------

	/** Upstream :73 mFlushMode — the automation flush latch (clicks re-emit the grid stock; the :167-173 tick arm re-checks). */
	public boolean mFlushMode = false;
	/** Upstream :73 mBlocked16 — the 4x4 automation-access switch (monkeywrench top face; the tool-click arm rides the tool pool). */
	public boolean mBlocked16 = false;
	/** Upstream :73 mBlocked36 — the 9x4 automation-access switch. */
	public boolean mBlocked36 = false;
	/** Upstream :73 mFilter16 — the 4x4 diversity filter (screwdriver top face). */
	public boolean mFilter16 = false;
	/** Upstream :73 mFilter36 — the 9x4 diversity filter. */
	public boolean mFilter36 = false;
	/** Upstream :73 mUpdatedGrid — the output recompute flag; every 21-30 mutation sets it (see the inventory hook), the tick (:154-157) and the GUI clicks (:601) consume it. */
	public boolean mUpdatedGrid = true;

	/**
	 * The ghost-grid server backing (decisions.p24-act-ghost-form): item + components,
	 * count NORMALIZED to 1 — never the size-0 upstream form, never routed through
	 * {@link #mInventory} (no automation leak, no drop, no GUI item face). Cleared when
	 * the selector leaves slot 30, refilled by {@link #getCraftingOutput}.
	 */
	public final ItemStack[] mPattern = new ItemStack[9];

	/** Upstream :92 mFacing (byte, GT6 side order == Direction.getIndex()) — the oven precedent. */
	protected byte mFacing = 2;

	/**
	 * The 71-slot inventory. Slot faces (the GUI/automation split, upstream :505-533):
	 * {@link #isItemValid} = the GUI/insert validity face — slot 30 carries the
	 * selector-only whitelist, slots 31/32 (the holo pair) reject everything;
	 * {@link #canInsertItem2}/{@link #canExtractItem2} = the automation band/filter
	 * predicates — the capability-side enforcement rides the item-IO pool (the dryer
	 * masks precedent: data + predicates now, the wire-through later).
	 */
	protected final GTItemStackHandler mInv;

	public TileEntityAdvancedCraftingTable(BlockPos aPos, BlockState aState) {
		this(gregtech6.registry.GTMachines.ADVANCED_CRAFTING_TABLE_BE.get(), aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point: Builder.of(...).build(null) works without a registry. */
	public TileEntityAdvancedCraftingTable(@Nullable net.minecraft.world.level.block.entity.BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		// the ACT ticks the 03 housekeeping chain (the :148-175 recompute/flush arm rides onTick) — no energy, no recipes auto-start
		super(true, aType, aPos, aState);
		mInv = new GTItemStackHandler(INVENTORY_SIZE, this::onInventoryChanged) {
			@Override
			public boolean isItemValid(int aSlot, ItemStack aStack) {
				// upstream isItemValidForSlotGUI :515 — the selector-only whitelist band [2, 9]
				// (decisions.p24-act-deviations: the blueprint paper arms ride the Printer pool)
				if (aSlot == 30) return isSelectorStackValid(aStack);
				if (aSlot == 31 || aSlot == 32) return false; // the holo pair (output preview + virtual flush/sort) is not an item face
				return super.isItemValid(aSlot, aStack);
			}

			@Override
			public int getSlotLimit(int aSlot) {
				if (aSlot == 30) return 1; // upstream getInventoryStackLimitGUI :513
				return super.getSlotLimit(aSlot);
			}

			@Override
			public void onContentsChanged(int aSlot) {
				super.onContentsChanged(aSlot);
				// upstream :505-508 — every 21-30 mutation flags the output recompute
				// (31 is OUTSIDE the upstream band too, so the output write cannot re-flag)
				if (aSlot >= 21 && aSlot <= 30) mUpdatedGrid = true;
			}
		};
		setInventory(mInv);
	}

	@Override
	public String getTileEntityName() {
		return "advanced_crafting_table"; // upstream "gt.multitileentity.crafting.advanced" :682, the BET registry path
	}

	/** The menu binds this as the SlotItemHandler container (the oven/chest precedent). */
	public GTItemStackHandler getInventory() {
		return mInv;
	}

	/** The slot-30 whitelist arm — selector-only, configuration inside [2, 9] (upstream :515, GT6Circuits frozen API). */
	public static boolean isSelectorStackValid(ItemStack aStack) {
		return GT6Circuits.isSelector(aStack) && isSelectorConfigValid(GT6Circuits.configurationOf(aStack));
	}

	/** The config band probe ({@code UT.Code.inside(2, 9, ...)} upstream :515). */
	public static boolean isSelectorConfigValid(int aConfig) {
		return aConfig >= SELECTOR_CONFIG_MIN && aConfig <= SELECTOR_CONFIG_MAX;
	}

	// ---------------------------------------------------------------------------
	// tick chain (upstream onTick2 :148-175 — zero energy, zero auto-craft; the
	// adjacent-inventory notify arm rides the cut ConnectedInventory domain)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (aIsServerSide) {
			if (mUpdatedGrid) {
				getCraftingOutput(false);
				mUpdatedGrid = false;
			}
			refill();
			if (mFlushMode) {
				mFlushMode = false;
				for (int i : SLOTS_CRAFTING) if (!mInv.getStackInSlot(i).isEmpty()) {
					mFlushMode = true;
					break;
				}
			}
		}
	}

	/** Upstream :186-188 verbatim — the empty refill stub (no tick auto-crafting; the Charging variant rides the charging pool). */
	protected void refill() {
		//
	}

	/** Content-change hook bound into the handler (the oven shape). */
	protected void onInventoryChanged() {
		setChanged();
	}

	// ---------------------------------------------------------------------------
	// the sweep + the pattern table (upstream getCraftingOutput :209-288)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :209-288 with the blueprint arm cut (deviation ①): the selector branch
	 * sweeps the real grid stock into the destination slot 24 (:215-221) and fills the
	 * ghost pattern (:222-283); a non-selector slot 30 KILLS the ghosts (the upstream
	 * ghost stacks just get swept away — the port clears the backing); then the virtual
	 * grid feeds the recipe lookup (:285 CR.getany) and the result lands in slot 31.
	 * {@code aAllowCache} kept for API fidelity (the vanilla lookup is stateless here).
	 */
	public ItemStack getCraftingOutput(boolean aAllowCache) {
		ItemStack tSelector = mInv.getStackInSlot(30);
		if (GT6Circuits.isSelector(tSelector)) {
			sweepGridToDestination();
			fillPattern(GT6Circuits.configurationOf(tSelector));
		} else {
			java.util.Arrays.fill(mPattern, ItemStack.EMPTY);
		}
		ItemStack rStack = findCraftingResult(effectiveGrid());
		// upstream :286 explosion arm CUT (deviation ⑥ — no GT flag carrier on the vanilla Recipe)
		if (rStack == null || rStack.isEmpty()) rStack = ItemStack.EMPTY;
		mInv.setStackInSlot(31, rStack); // :287 slot(31, rStack)
		return rStack;
	}

	/**
	 * Upstream :215-221 — consolidate the real grid stock: merge equal stacks into the
	 * destination cell 24, overflow to a storage belt with equal content, then to any
	 * empty storage belt. (The upstream {@code slotNull(i)} before the move only kills
	 * size-0 ghost stacks — 05Inventories.java — which the port carries separately in
	 * {@link #mPattern}, so the sweep sees only real stock.)
	 */
	public void sweepGridToDestination() {
		int tDestination = SLOTS_CRAFTING[3]; // :214
		for (int i : SLOTS_CRAFTING) {
			if (i == tDestination) continue;
			ItemStack tStack = mInv.getStackInSlot(i);
			if (tStack.isEmpty()) continue;
			ItemStack tDest = mInv.getStackInSlot(tDestination);
			if (tDest.isEmpty()) {
				mInv.setStackInSlot(tDestination, tStack);
				mInv.setStackInSlot(i, ItemStack.EMPTY);
				continue;
			}
			if (ItemStack.isSameItemSameTags(tDest, tStack)) {
				int tMoved = Math.min(tStack.getCount(), tDest.getMaxStackSize() - tDest.getCount());
				if (tMoved > 0) {
					tDest.grow(tMoved);
					tStack.shrink(tMoved);
				}
			}
			if (tStack.isEmpty()) {
				mInv.setStackInSlot(i, ItemStack.EMPTY);
				continue;
			}
			for (int j : SLOTS_STORAGE) { // :219 — an equal storage belt slot
				ItemStack tBelt = mInv.getStackInSlot(j);
				if (!tBelt.isEmpty() && ItemStack.isSameItemSameTags(tBelt, tStack)) {
					int tMoved = Math.min(tStack.getCount(), tBelt.getMaxStackSize() - tBelt.getCount());
					if (tMoved > 0) {
						tBelt.grow(tMoved);
						tStack.shrink(tMoved);
					}
					if (tStack.isEmpty()) break;
				}
			}
			if (!tStack.isEmpty()) for (int j : SLOTS_STORAGE) { // :220 — any empty storage belt slot
				if (mInv.getStackInSlot(j).isEmpty()) {
					mInv.setStackInSlot(j, tStack);
					tStack = ItemStack.EMPTY;
					break;
				}
			}
			mInv.setStackInSlot(i, tStack.isEmpty() ? ItemStack.EMPTY : tStack);
		}
	}

	/**
	 * Upstream :222-283 — the meta pattern table over the swept destination material,
	 * written into {@link #mPattern} at count 1 (the size-0 ghost form is not portable,
	 * GT6Circuits.java:40-41). Cells already holding REAL stock stay out of the pattern
	 * (the upstream {@code if (!slotHas(...))} guard). The destination cell [3] never
	 * enters the pattern — it keeps the real stack.
	 */
	public void fillPattern(int aConfig) {
		ItemStack tMaterial = mInv.getStackInSlot(SLOTS_CRAFTING[3]);
		if (tMaterial.isEmpty()) return; // nothing swept — upstream amount(0, null) writes nothing
		for (int tCell : patternCells(aConfig)) {
			int tSlot = SLOTS_CRAFTING[tCell];
			if (mInv.getStackInSlot(tSlot).isEmpty()) {
				ItemStack tGhost = tMaterial.copy();
				tGhost.setCount(1); // count NORMALIZED to 1 — decisions.p24-act-ghost-form
				mPattern[tCell] = tGhost;
			}
		}
	}

	/** The effective 3x3 recipe grid (decisions.p24-act-ghost-form ③): pattern ghost where present, the real slot otherwise. */
	public ItemStack[] effectiveGrid() {
		ItemStack[] rGrid = new ItemStack[9];
		for (int i = 0; i < 9; i++) {
			ItemStack tGhost = mPattern[i];
			ItemStack tReal = mInv.getStackInSlot(SLOTS_CRAFTING[i]);
			rGrid[i] = tGhost != null && !tGhost.isEmpty() ? tGhost : tReal;
		}
		return rGrid;
	}

	/**
	 * Upstream :285 CR.getany — the vanilla RecipeManager lookup over the virtual grid.
	 * Level-less calls (offline fixtures without a manager) return EMPTY.
	 */
	@Nullable
	public ItemStack findCraftingResult(ItemStack[] aGrid) {
		if (!hasLevel()) return null;
		NonNullList<ItemStack> tCells = NonNullList.withSize(9, ItemStack.EMPTY);
		for (int i = 0; i < 9; i++) tCells.set(i, aGrid[i] == null ? ItemStack.EMPTY : aGrid[i]);
		//? if forge {
		TransientCraftingContainer tGrid = new TransientCraftingContainer(null, 3, 3, tCells);
		return getLevel().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, tGrid, getLevel())
				.map(tRecipe -> tRecipe.getResultItem(getLevel().registryAccess()))
				.orElse(null);
		//?} else {
		/*// 21.1: the grid is the CraftingInput record (Recipe typed <T extends RecipeInput>,
		//getRecipeFor returns Optional<RecipeHolder<T>> — unwrap .value(); the FileSawTest
		//CraftingInput.of(3, 3, cells) precedent.
		net.minecraft.world.item.crafting.CraftingInput tGrid = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, tCells);
		return getLevel().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, tGrid, getLevel())
				.map(tHolder -> tHolder.value().getResultItem(getLevel().registryAccess()))
				.orElse(null);
		*///?}
	}

	// ---------------------------------------------------------------------------
	// the craft gates (upstream :290-332)
	// ---------------------------------------------------------------------------

	/** Upstream :290-294 — the output is craftable when every grid item is stocked (grid + belts) at the per-craft cell count. */
	public boolean canDoCraftingOutput() {
		if (mInv.getStackInSlot(31).isEmpty()) return false;
		for (ItemStack tStack : recipeContent()) if (tStack.getCount() > getAmountOf(tStack)) return false;
		return true;
	}

	/** Upstream :296-314 minus the ConnectedInventory arm (deviation ③) — the total stocked count of one item; the :300/:304 cap is the upstream 9. */
	protected int getAmountOf(ItemStack aStack) {
		int tAmount = 0;
		for (int i : SLOTS_CRAFTING) if (equalTools(aStack, mInv.getStackInSlot(i))) {
			tAmount += mInv.getStackInSlot(i).getCount();
			if (tAmount >= SLOTS_CRAFTING.length) return tAmount;
		}
		for (int i : SLOTS_CONSUMPTION) if (equalTools(aStack, mInv.getStackInSlot(i))) {
			tAmount += mInv.getStackInSlot(i).getCount();
			if (tAmount >= SLOTS_CRAFTING.length) return tAmount;
		}
		return tAmount;
	}

	/** Upstream :316-332 over the EFFECTIVE grid (the ghost cells count, the real-stock merge) — one entry per distinct item, count = occupied cells. */
	protected List<ItemStack> recipeContent() {
		ArrayList<ItemStack> tList = new ArrayList<>();
		for (ItemStack tCell : effectiveGrid()) {
			if (tCell == null || tCell.isEmpty()) continue;
			boolean tTemp = false;
			for (ItemStack tEntry : tList) {
				if (equalTools(tCell, tEntry)) {
					tEntry.grow(1);
					tTemp = true;
					break;
				}
			}
			if (!tTemp) {
				ItemStack tEntry = tCell.copy();
				tEntry.setCount(1);
				tList.add(tEntry);
			}
		}
		return tList;
	}

	/** Upstream ST.equalTools(a, b, F) — item + components, size-insensitive; null-safe (the upstream ST.equal family accepts null on both sides, e.g. the :367 container probe). */
	public static boolean equalTools(@Nullable ItemStack aA, @Nullable ItemStack aB) {
		return aA != null && !aA.isEmpty() && aB != null && ItemStack.isSameItemSameTags(aA, aB);
	}

	// ---------------------------------------------------------------------------
	// the batch crafting core (upstream consumeMaterials :334-433 — the player-free
	// port; the event/sound/onCrafting arms ride the cut tool/achievement pools)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :334-433 minus the declared-cut arms: the gate trio (:335-353 minus the
	 * unported autocrafterinfinite probe), the per-cell consumption priority
	 * (:406-413), the container-item return (:364-367/:435-487), the hold-stack growth
	 * (:416), and the housekeeping tail (:422-431 minus the sound/event half). The
	 * crafted stack lands on the CALLER side through the return value — the GUI cursor
	 * and the batch sinks are upstream-free seams (the four click modes ride
	 * {@link #craftOnce}/{@link #craftFillCell}/{@link #craftTraverse}).
	 *
	 * @param aHoldStack the accumulating output stack (null/EMPTY = start a fresh one)
	 * @param aSubsequentClick true for every craft after the first of one gesture
	 *     (upstream gated the sound arm on it — the arm is cut, the flag kept for the
	 *     upstream call-shape fidelity)
	 */
	public ItemStack consumeMaterials(@Nullable ItemStack aHoldStack, boolean aSubsequentClick) {
		ItemStack tOutput = mInv.getStackInSlot(31);
		if (tOutput.isEmpty()) return aHoldStack; // :335

		if (aHoldStack != null && !aHoldStack.isEmpty()) {
			if (!ItemStack.isSameItemSameTags(aHoldStack, tOutput)) return aHoldStack; // :338
			if (aHoldStack.getCount() + tOutput.getCount() > aHoldStack.getMaxStackSize()) return aHoldStack; // :345
			// :346-352 autocrafterinfinite guard CUT (the item is unported — unreachable)
		}

		// :357 the firePlayerCraftingEvent arm CUT (the player-free seam; achievements pool)
		ItemStack[] tRecipeStacks = new ItemStack[9];
		for (int j = 0; j < 9; j++) tRecipeStacks[j] = amountOne(effectiveGrid()[j]);

		for (int j = 0; j < 9; j++) {
			if (tRecipeStacks[j] == null) continue;
			boolean tNeeds = true;
			ItemStack tContainer = containerItem(tRecipeStacks[j]);
			// :366-367 — contains itself, so it's an infinite-use container item anyways
			if (equalTools(tRecipeStacks[j], tContainer)) continue;

			// :369-404 the ConnectedInventory external-draw + container-into-network arms CUT (deviation ③)

			// :406-407 — First take from the Slot that actually indicates the Item.
			if (tNeeds && equalTools(tRecipeStacks[j], mInv.getStackInSlot(SLOTS_CRAFTING[j]))
					&& mInv.getStackInSlot(SLOTS_CRAFTING[j]).getCount() > 1
					&& consumeSlot(SLOTS_CRAFTING[j])) continue;
			// :408-409 — Then take from the Grid but always leave one in each Slot.
			if (tNeeds) for (int i : SLOTS_CRAFTING) {
				if (equalTools(tRecipeStacks[j], mInv.getStackInSlot(i))
						&& mInv.getStackInSlot(i).getCount() > 1 && consumeSlot(i)) {
					tNeeds = false;
					break;
				}
			}
			// :410-411 — Then draw from the ready Slots (the belts 70→0).
			if (tNeeds) for (int i : SLOTS_CONSUMPTION) {
				if (equalTools(tRecipeStacks[j], mInv.getStackInSlot(i))
						&& mInv.getStackInSlot(i).getCount() > 0 && consumeSlot(i)) {
					tNeeds = false;
					break;
				}
			}
			// :412-413 — And then pull from the Crafting Slots if needed, and mark the Grid as changed.
			if (tNeeds) for (int i : SLOTS_CRAFTING) {
				if (equalTools(tRecipeStacks[j], mInv.getStackInSlot(i))
						&& mInv.getStackInSlot(i).getCount() > 0 && consumeSlot(i)) {
					tNeeds = false;
					mUpdatedGrid = true;
					break;
				}
			}
		}

		if (aHoldStack == null || aHoldStack.isEmpty()) {
			aHoldStack = tOutput.copy(); // :416
		} else {
			aHoldStack.grow(tOutput.getCount());
		}

		// :418 the onCrafting arm CUT (player seam); :420 ST.check CUT (tool pool)
		refill(); // :422
		// :424-426 the tool-coords/sound tail CUT
		setChanged(); // :428 updateInventory → mInventoryChanged + the vanilla dirty flag
		return aHoldStack; // :432
	}

	/** Upstream ST.amount(1, aStack) — a count-1 copy of a stocked cell, null for an empty cell. */
	@Nullable
	private static ItemStack amountOne(@Nullable ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		ItemStack rStack = aStack.copy();
		rStack.setCount(1);
		return rStack;
	}

	/** Upstream ST.container(stack, F) — the crafting container item (bucket→empty bucket, the IForgeItemStack face). */
	@Nullable
	private static ItemStack containerItem(ItemStack aStack) {
		if (!aStack.hasCraftingRemainingItem()) return null;
		ItemStack rStack = aStack.getCraftingRemainingItem();
		if (rStack.isEmpty()) return null;
		return rStack;
	}

	/**
	 * Upstream consumeSlot :435-487 minus the ConnectedTank refill (:439-450) and
	 * ConnectedInventory (:451-461) arms — consume one item from a slot, returning the
	 * container item into the same slot or the input belts.
	 */
	public boolean consumeSlot(int aSlot) {
		ItemStack tStack = mInv.getStackInSlot(aSlot);
		ItemStack tContainer = containerItem(tStack);

		// :464-468 — Consume the Item (no container, or the container is damaged out).
		if (tContainer == null || (tContainer.isDamageableItem() && tContainer.getDamageValue() >= tContainer.getMaxDamage())) {
			mInv.setStackInSlot(aSlot, shrinkOne(tStack));
			return true;
		}
		if (tStack.getCount() == 1) { // :469-472 — the slot turns into the container
			mInv.setStackInSlot(aSlot, tContainer);
			return true;
		}
		mInv.setStackInSlot(aSlot, shrinkOne(tStack)); // :473
		for (int i : SLOTS_INPUT) { // :474-485 — the container lands in the input belts
			ItemStack tInput = mInv.getStackInSlot(i);
			if (tInput.isEmpty()) {
				mInv.setStackInSlot(i, tContainer);
				return true;
			}
			if (ItemStack.isSameItemSameTags(tContainer, tInput)
					&& tContainer.getCount() + tInput.getCount() <= tInput.getMaxStackSize()) {
				tInput.grow(tContainer.getCount());
				mInv.setStackInSlot(i, tInput);
				return true;
			}
		}
		return true;
	}

	/** The upstream decrStackSize(aSlot, 1) face over the handler. */
	private ItemStack shrinkOne(ItemStack aStack) {
		ItemStack rStack = aStack.copy();
		rStack.shrink(1);
		return rStack.isEmpty() ? ItemStack.EMPTY : rStack;
	}

	// ---------------------------------------------------------------------------
	// the four click modes (upstream slotClick :599-656) over the player-free sink seam
	// ---------------------------------------------------------------------------

	/**
	 * The output sink seam: where crafted stacks land. Upstream reads/writes the player
	 * cursor ({@code inventory.getItemStack}, :640/:645) or the main-inventory cells
	 * (:606-631) directly; the seam keeps the four loop modes runnable without a Player
	 * instance (the pipe-owner discipline — the offline tests and the {@code /gt6act}
	 * chain drive array sinks; the C2 GUI binds the cursor/inventory faces).
	 */
	public interface ICraftOutputSink {
		/** The traversal width (upstream mainInventory.length). */
		int cellCount();

		/** The current content of one destination cell (EMPTY = a fresh hold starts from slot 31). */
		ItemStack getHold(int aCell);

		/** Writes the hold back into the cell. */
		void setHold(int aCell, ItemStack aHold);
	}

	/** Whether the upstream per-cell guard (:607/:620) lets {@code aCrafted} land in the cell. */
	public static boolean sinkCanAccept(ICraftOutputSink aSink, int aCell, ItemStack aCrafted) {
		ItemStack tHold = aSink.getHold(aCell);
		return tHold.isEmpty()
				|| (ItemStack.isSameItemSameTags(tHold, aCrafted) && tHold.getCount() + aCrafted.getCount() <= tHold.getMaxStackSize());
	}

	/** The per-craft stability probe (:609/:623/:637) — the recipe output must not drift mid-gesture. */
	private boolean outputStill(ItemStack aCrafted) {
		ItemStack tNow = getCraftingOutput(true);
		return ItemStack.isSameItemSameTags(tNow, aCrafted) && tNow.getCount() == aCrafted.getCount();
	}

	/** The upstream LEFTCLICK arm (:644-646) — one craft into the sink cell. @return true when a craft happened. */
	public boolean craftOnce(ICraftOutputSink aSink, int aCell) {
		if (!canDoCraftingOutput()) return false;
		aSink.setHold(aCell, consumeMaterials(aSink.getHold(aCell), false));
		return true;
	}

	/**
	 * The upstream RIGHTCLICK arm (:634-643) — fill the sink cell to the crafted
	 * stack's vanilla cap, aborting on output drift. @return the craft count.
	 */
	public int craftFillCell(ICraftOutputSink aSink, int aCell) {
		ItemStack tCrafted = getCraftingOutput(true);
		if (tCrafted.isEmpty()) return 0;
		int tMax = Math.max(1, tCrafted.getMaxStackSize() / tCrafted.getCount());
		int rCrafts = 0;
		for (int i = 0; i < tMax && canDoCraftingOutput(); i++) {
			if (!outputStill(tCrafted)) return rCrafts; // :637 — the output drifted mid-gesture
			aSink.setHold(aCell, consumeMaterials(aSink.getHold(aCell), i != 0));
			rCrafts++;
		}
		return rCrafts;
	}

	/**
	 * The upstream SHIFT-click arms: {@code aSingleCell} = SHIFT+LEFT (:618-632, stop
	 * after the first productive inventory cell), else SHIFT+RIGHT (:604-617, traverse
	 * every cell). @return the total craft count.
	 */
	public int craftTraverse(ICraftOutputSink aSink, boolean aSingleCell) {
		ItemStack tCrafted = getCraftingOutput(true);
		if (tCrafted.isEmpty()) return 0;
		int tMax = Math.max(1, tCrafted.getMaxStackSize() / tCrafted.getCount());
		int rCrafts = 0;
		for (int i = 0; i < aSink.cellCount(); i++) {
			if (!sinkCanAccept(aSink, i, tCrafted)) continue;
			boolean tTemp = false;
			for (int j = 0; j < tMax && canDoCraftingOutput(); j++) {
				if (!outputStill(tCrafted)) return rCrafts; // :609/:623 — the output drifted mid-gesture
				aSink.setHold(i, consumeMaterials(aSink.getHold(i), i != 0 || j != 0));
				rCrafts++;
				tTemp = true;
			}
			if (tTemp && aSingleCell) return rCrafts; // :629 — SHIFT+LEFT stops at the first productive cell
		}
		return rCrafts;
	}

	/** Upstream sortIntoTheInputSlots :177-184 — the grid real stock sorts back into the belts (ghosts live in mPattern, nothing to kill). */
	public void sortIntoTheInputSlots() {
		for (int i : SLOTS_CRAFTING) {
			if (mInv.getStackInSlot(i).isEmpty()) continue;
			ItemStack tStack = mInv.getStackInSlot(i);
			for (int j : SLOTS_STORAGE) { // :180 — merge equal belt stacks
				ItemStack tBelt = mInv.getStackInSlot(j);
				if (!tBelt.isEmpty() && ItemStack.isSameItemSameTags(tBelt, tStack)) {
					int tMoved = Math.min(tStack.getCount(), tBelt.getMaxStackSize() - tBelt.getCount());
					if (tMoved > 0) {
						tBelt.grow(tMoved);
						tStack.shrink(tMoved);
						mInv.setStackInSlot(j, tBelt);
					}
					if (tStack.isEmpty()) break;
				}
			}
			if (!tStack.isEmpty()) for (int j : SLOTS_STORAGE) { // :181 — any empty belt slot
				if (mInv.getStackInSlot(j).isEmpty()) {
					mInv.setStackInSlot(j, tStack);
					tStack = ItemStack.EMPTY;
					break;
				}
			}
			mInv.setStackInSlot(i, tStack.isEmpty() ? ItemStack.EMPTY : tStack);
		}
		mUpdatedGrid = true; // :183
	}

	// ---------------------------------------------------------------------------
	// the automation faces (upstream :505-533 — the predicates; the capability
	// wire-through rides the item-IO pool, the dryer masks precedent)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream getAccessibleSlotsFromSide2 :510 verbatim — the 4-mode selection; the
	 * upstream body ignores the side (the modes are global).
	 */
	public int[] accessibleSlotsFromSide() {
		return mBlocked16
				? (mBlocked36 ? (mFlushMode ? SLOTS_FLUSHING : SLOTS) : (mFlushMode ? SLOTS_36_FLUSHING : SLOTS_36))
				: (mBlocked36 ? (mFlushMode ? SLOTS_16_FLUSHING : SLOTS_16) : (mFlushMode ? SLOTS_ALL_FLUSHING : SLOTS_ALL));
	}

	/** Upstream canInsertItem2 :518-528 — the band check plus the mFilter16/36 diversity filters. */
	public boolean canInsertItem2(int aSlot, ItemStack aStack) {
		if (aSlot < 16) {
			if (mFilter16) for (int i = 0; i < 16; i++) if (equalTools(aStack, mInv.getStackInSlot(i))) return aSlot == i;
			return true;
		}
		if (aSlot >= 35 && aSlot < 71) {
			if (mFilter36) for (int i = 35; i < 71; i++) if (equalTools(aStack, mInv.getStackInSlot(i))) return aSlot == i;
			return true;
		}
		return false;
	}

	/** Upstream canExtractItem2 :531-533 — slot 33, or the grid while flushing. */
	public boolean canExtractItem2(int aSlot) {
		return aSlot == 33 || (mFlushMode && aSlot > 20 && aSlot < 30);
	}

	/** Upstream canDrop :511 — the holo pair (31/32) never drops (the loot table drops the BLOCK self-drop anyway). */
	public boolean canDrop(int aInventorySlot) {
		return aInventorySlot < 31 || aInventorySlot > 32;
	}

	// ---------------------------------------------------------------------------
	// facing (the oven precedent — NBT authority + BlockState re-application)
	// ---------------------------------------------------------------------------

	public void setFacingFromPlacement(Player aPlayer) {
		mFacing = (byte) aPlayer.getDirection().get3DDataValue();
		applyVisualState();
	}

	public byte getFacing() {
		return mFacing;
	}

	public boolean setFrontFacing(byte aSide) {
		if (aSide < 2 || aSide > 5 || aSide == mFacing) return false;
		mFacing = aSide;
		setChanged();
		applyVisualState();
		return true;
	}

	/** FACING onto the BlockState (the vanilla furnace setBlock(state, 3) idiom — the ACT carries no ACTIVE/RUNNING bits). */
	public void applyVisualState() {
		if (!hasLevel() || isClientSide()) return;
		BlockState tState = getLevel().getBlockState(getBlockPos());
		if (!(tState.getBlock() instanceof GTAdvancedCraftingTableBlock)) return;
		BlockState tNew = tState.setValue(GTAdvancedCraftingTableBlock.FACING, Direction.from3DDataValue(mFacing));
		if (tNew != tState) getLevel().setBlock(getBlockPos(), tNew, 3);
	}

	// ---------------------------------------------------------------------------
	// GUI (upstream getGUIClient2/getGUIServer2 :585-586 → MenuProvider) — the C1
	// carrier declares the face; createMenu wires the C2 ModularUI menu
	// (decisions.p24-act-be-form: the mdk-first-consumer wiring gate; the fallback arm
	// is a vanilla MenuType, reported to the architect if the wiring fails)
	// ---------------------------------------------------------------------------

	@Override
	@Nullable
	public AbstractContainerMenu createMenu(int aContainerId, Inventory aPlayerInventory, Player aPlayer) {
		return null; // task p24-act-machine C2 — the ModularUI menu (menu/act/*) lands here
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :77-98 / writeToNBT2 :91-98 key-for-key in the
	// plain in-repo key form; the mPattern list rides the decision-pinned shape)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_FACING, mFacing);
		//? if forge {
		aNBT.put(NBT_INVENTORY, mInv.serializeNBT());
		//?} else {
		/*aNBT.put(NBT_INVENTORY, mInv.serializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS)); // 21.1: provider-first
		*///?}
		ListTag tPattern = new ListTag();
		for (int i = 0; i < 9; i++) {
			ItemStack tStack = mPattern[i];
			if (tStack != null && !tStack.isEmpty()) {
				//? if forge {
				CompoundTag tEntry = tStack.save(new CompoundTag());
				//?} else {
				/*CompoundTag tEntry = tStack.save(TileEntityBase03TicksAndSync.NBT_ACCESS, new CompoundTag());
				*///?}
				tEntry.putByte("Slot", (byte) i); // the slot-marked list form — the ItemStackHandler serialize shape
				tPattern.add(tEntry);
			}
		}
		aNBT.put(NBT_PATTERN, tPattern);
		aNBT.putBoolean(NBT_BLOCKED_16, mBlocked16);
		aNBT.putBoolean(NBT_BLOCKED_36, mBlocked36);
		aNBT.putBoolean(NBT_FILTER_16, mFilter16);
		aNBT.putBoolean(NBT_FILTER_36, mFilter36);
		aNBT.putBoolean(NBT_FLUSH, mFlushMode);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_FACING, Tag.TAG_ANY_NUMERIC)) mFacing = aNBT.getByte(NBT_FACING);
		//? if forge {
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) mInv.deserializeNBT(aNBT.getCompound(NBT_INVENTORY));
		//?} else {
		/*if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) mInv.deserializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS, aNBT.getCompound(NBT_INVENTORY));
		*///?}
		if (aNBT.contains(NBT_PATTERN, Tag.TAG_LIST)) {
			java.util.Arrays.fill(mPattern, ItemStack.EMPTY);
			ListTag tPattern = aNBT.getList(NBT_PATTERN, Tag.TAG_COMPOUND);
			for (int i = 0; i < tPattern.size(); i++) {
				CompoundTag tEntry = tPattern.getCompound(i);
				int tSlot = tEntry.contains("Slot", Tag.TAG_ANY_NUMERIC) ? tEntry.getByte("Slot") : i;
				if (tSlot < 0 || tSlot >= 9) continue;
				//? if forge {
				ItemStack tStack = ItemStack.of(tEntry);
				//?} else {
				/*ItemStack tStack = ItemStack.parseOptional(TileEntityBase03TicksAndSync.NBT_ACCESS, tEntry);
				*///?}
				if (!tStack.isEmpty()) mPattern[tSlot] = tStack;
			}
		}
		if (aNBT.contains(NBT_BLOCKED_16)) mBlocked16 = aNBT.getBoolean(NBT_BLOCKED_16);
		if (aNBT.contains(NBT_BLOCKED_36)) mBlocked36 = aNBT.getBoolean(NBT_BLOCKED_36);
		if (aNBT.contains(NBT_FILTER_16)) mFilter16 = aNBT.getBoolean(NBT_FILTER_16);
		if (aNBT.contains(NBT_FILTER_36)) mFilter36 = aNBT.getBoolean(NBT_FILTER_36);
		if (aNBT.contains(NBT_FLUSH)) mFlushMode = aNBT.getBoolean(NBT_FLUSH);
	}
}
