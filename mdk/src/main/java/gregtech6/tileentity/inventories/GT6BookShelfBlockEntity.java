package gregtech6.tileentity.inventories;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.datagen.GT6ItemTags;
import gregtech6.registry.GTBlockEntities;

/**
 * The GT6 Bookshelf — 1.20.1 counterpart of
 * {@code gregapi/tileentity/inventories/MultiTileEntityBookShelf.java:54-367}, the 28-slot
 * double-faced book shelf (the metalset row :142 "Bookshelf (%s)" id 7100+aID over the
 * wooden 300-ladder :177-179 — the port rows are the vanilla-planks subset, the declared
 * deviation of the wave-4 ruling: PlankData has no 1.20.1 counterpart, 300 ladders fold
 * into the row pool).
 *
 * <h2>The inventory face</h2>
 * <ul>
 * <li>28 slots ({@code mDisplay[28]} :55), face-split 0..13 FRONT / 14..27 BACK (the
 *     upstream slot picker {@code tIndex = (v < PX_P[8] ? 6/20 : 13/27) - ...} ranges);</li>
 * <li>sided access: the base-default ascending full table (upstream :375 06Covers default
 *     — the bookshelf overrides nothing, hoppers see all 28);</li>
 * <li>insert gate :364 port: empty slot + a BOOK ({@link GT6ItemTags#BOOKS}, the
 *     {@code BooksGT.BOOK_REGISTER.containsKey} modern equivalent — a tag, so pack authors
 *     extend it) + not a redstone arm; extract :363: occupied + not a redstone arm (the
 *     cobblestone/redstone-torch/lever/button arm-hold exclusions verbatim);</li>
 * <li>stack limit 1 (:362).</li>
 * </ul>
 *
 * <h2>The no-tick folds (the card headline — upstream DID tick, the port does not)</h2>
 * <ul>
 * <li>dungeon loot (front/back markers, :65-66/:135-159): filled on first open/break
 *     exactly like the Safe — the upstream 300-tick proximity timer (:164-166) folds;</li>
 * <li>the redstone arms (button/lever/redstone torch in a slot → weak power, :164-200 /
 *     :257-259) DEFER: the countdown needs the tick the card rules away; the gates above
 *     keep automation from eating arm items. The click arms fold with them;</li>
 * <li>the pixel slot picker (:168-172/:191-200) folds to the card's shift-all/single-take
 *     ruling — the interaction lives on the BLOCK use face and picks by range, not by
 *     pixel;</li>
 * <li>the mDisplay[] book-render sync (client 3D books) is the render pool — the port
 *     block art is a placeholder (the hopper-family precedent).</li>
 * </ul>
 */
public class GT6BookShelfBlockEntity extends GT6StaticStorageBaseBlockEntity {

	/** The slot count (upstream {@code mDisplay[28]} :55 — 14 per face). */
	public static final int INVENTORY_SIZE = 28;

	/** The front face occupies slots 0..13, the back face 14..27 (the upstream picker ranges). */
	public static final int SLOTS_PER_FACE = 14;

	/** The shelf-book tag (the BooksGT.BOOK_REGISTER gate, class doc). */
	public static final TagKey<net.minecraft.world.item.Item> BOOKS = GT6ItemTags.BOOKS;

	/** Upstream "gt.dungeonloot.front" (:62/:135) verbatim — the front-face loot marker. */
	public static final String NBT_DUNGEON_LOOT_FRONT = "gt.dungeonloot.front";

	/** Upstream "gt.dungeonloot.back" (:63/:135) verbatim — the back-face loot marker. */
	public static final String NBT_DUNGEON_LOOT_BACK = "gt.dungeonloot.back";

	/** The two loot markers (front/back, upstream :56-57). */
	public String mDungeonLootNameFront = "", mDungeonLootNameBack = "";

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type at runtime. */
	public GT6BookShelfBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.BOOKSHELF_BE.get(), aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6BookShelfBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "bookshelf"; // upstream "gt.multitileentity.bookshelf" family — the BET path mirrors it
	}

	@Override
	protected int inventorySize(BlockState aState) {
		return INVENTORY_SIZE;
	}

	@Override
	public int stackLimit() {
		return 1; // upstream :362
	}

	@Override
	public int[] getAccessibleSlotsFromSide(byte aSide) {
		// the base-default ascending table (upstream 06Covers:375 — no override upstream)
		int[] rSlots = new int[INVENTORY_SIZE];
		for (int i = 0; i < INVENTORY_SIZE; i++) rSlots[i] = i;
		return rSlots;
	}

	/** The redstone-arm exclusions (:363-364 — cobblestone, redstone torch, lever, button). */
	public static boolean isRedstoneArm(ItemStack aStack) {
		return aStack.is(Items.COBBLESTONE) || aStack.is(Items.REDSTONE_TORCH)
				|| aStack.is(Items.LEVER) || aStack.is(Items.STONE_BUTTON);
	}

	/** Upstream :364 — empty slot + a book + not an arm. */
	@Override
	public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {
		return !slotHas(aSlot) && aStack.is(BOOKS) && !isRedstoneArm(aStack);
	}

	/** Upstream :363 — occupied + not an arm. */
	@Override
	public boolean canExtractItem(int aSlot, byte aSide) {
		return slotHas(aSlot) && !isRedstoneArm(slot(aSlot));
	}

	/**
	 * The face-local insert gate (the upstream slot picker's face ranges): front slots
	 * 0..13, back slots 14..27 — the click arm picks the first empty of ITS face.
	 */
	public int firstEmptyOfFace(boolean aBackFace) {
		int tBase = aBackFace ? SLOTS_PER_FACE : 0;
		for (int i = 0; i < SLOTS_PER_FACE; i++) {
			if (!slotHas(tBase + i)) return tBase + i;
		}
		return -1;
	}

	/** The last occupied slot of the face (the single-take pick of the shift-all/single-take ruling). */
	public int lastOccupiedOfFace(boolean aBackFace) {
		int tBase = aBackFace ? SLOTS_PER_FACE : 0;
		for (int i = SLOTS_PER_FACE - 1; i >= 0; i--) {
			if (slotHas(tBase + i)) return tBase + i;
		}
		return -1;
	}

	// ---------------------------------------------------------------------------
	// the dungeon-loot seam (the Safe's, face-split: front fills 0..13, back 14..27 —
	// the upstream generateDungeonLoot :135-159 ranges; each roll lands as ONE book
	// (the BOOK_REGISTER/lost-books branch folds to the plain rolled stack))
	// ---------------------------------------------------------------------------

	/**
	 * The guarded fill — the block-carrier call face (use arm + onRemove, the upstream
	 * onBlockActivated3/breakBlock pair): level/server guards, then both faces.
	 */
	public void tryGenerateDungeonLoot() {
		if (!hasLevel() || isClientSide()) return;
		generateDungeonLootFrom(false, this::rollOne);
		generateDungeonLootFrom(true, this::rollOne);
	}

	/** One face of the upstream fill loop: the face's empty slots take the rolls (the upstream rng-pick order folds to the sequential fill — the same result set, a deterministic test seam). */
	void generateDungeonLootFrom(boolean aBackFace, java.util.function.Function<String, ItemStack> aRoller) {
		String tName = aBackFace ? mDungeonLootNameBack : mDungeonLootNameFront;
		if (tName.isEmpty()) return;
		int tBase = aBackFace ? SLOTS_PER_FACE : 0;
		for (int i = 0; i < SLOTS_PER_FACE; i++) {
			int tSlot = tBase + i;
			if (!slotHas(tSlot)) {
				ItemStack tStack = aRoller.apply(tName);
				if (tStack != null && !tStack.isEmpty()) {
					slot(tSlot, tStack);
				}
			}
		}
		if (aBackFace) mDungeonLootNameBack = ""; else mDungeonLootNameFront = "";
		updateInventory();
	}

	/** The Safe's rollOne over the shared LootTable seam (same modern roll, class doc). */
	protected ItemStack rollOne(String aTableName) {
		if (!hasLevel() || isClientSide() || !(getLevel() instanceof net.minecraft.server.level.ServerLevel tLevel)) return ItemStack.EMPTY;
		net.minecraft.resources.ResourceLocation tId = net.minecraft.resources.ResourceLocation.tryParse(aTableName);
		if (tId == null) return ItemStack.EMPTY;
		//? if forge {
		net.minecraft.world.level.storage.loot.LootTable tTable = tLevel.getServer().getLootData().getLootTable(tId);
		//?} else {
		/*net.minecraft.world.level.storage.loot.LootTable tTable = tLevel.getServer().reloadableRegistries().getLootTable(
				net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, tId)); // 21.1 resolver, ResourceKey keyed
		*///?}
		java.util.List<ItemStack> tRolls = new java.util.ArrayList<>();
		net.minecraft.world.level.storage.loot.LootParams tParams = new net.minecraft.world.level.storage.loot.LootParams.Builder(tLevel)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
						net.minecraft.world.phys.Vec3.atCenterOf(getBlockPos()))
				.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
		tTable.getRandomItems(tParams, tRolls::add);
		if (tRolls.isEmpty()) return ItemStack.EMPTY;
		return tRolls.get(tLevel.random.nextInt(tRolls.size()));
	}

	@Override
	public void load(net.minecraft.nbt.CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_DUNGEON_LOOT_FRONT, net.minecraft.nbt.Tag.TAG_STRING)) {
			mDungeonLootNameFront = aNBT.getString(NBT_DUNGEON_LOOT_FRONT);
		}
		if (aNBT.contains(NBT_DUNGEON_LOOT_BACK, net.minecraft.nbt.Tag.TAG_STRING)) {
			mDungeonLootNameBack = aNBT.getString(NBT_DUNGEON_LOOT_BACK);
		}
	}

	@Override
	protected void saveAdditional(net.minecraft.nbt.CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (!mDungeonLootNameFront.isEmpty()) aNBT.putString(NBT_DUNGEON_LOOT_FRONT, mDungeonLootNameFront);
		if (!mDungeonLootNameBack.isEmpty()) aNBT.putString(NBT_DUNGEON_LOOT_BACK, mDungeonLootNameBack);
	}
}
