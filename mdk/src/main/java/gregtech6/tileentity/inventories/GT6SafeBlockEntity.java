package gregtech6.tileentity.inventories;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import gregtech6.registry.GTBlockEntities;

/**
 * The GT6 Safe (the mechanical kind) — 1.20.1 counterpart of the abstract
 * {@code gregapi/tileentity/inventories/MultiTileEntitySafe.java:47-119} with the
 * {@code MultiTileEntitySafeMechanical} personality (the two metalset rows :134-135,
 * "Mechanical %s Safe" id 2000+aID / "Key Locked %s Safe" id 3000+aID,
 * NBT_INV_SIZE = 15, hardness = resistance = aHardness*2 — the blast-resistant storage).
 *
 * <h2>The inventory face</h2>
 * <ul>
 * <li>15 slots (the registration NBT_INV_SIZE); GUI-only: {@code :105
 *     getAccessibleSlotsFromSide2 = ZL_INTEGER}, insert/extract :106-107 = false — the
 *     port answers a 0-slot side view (the base override posture), automation-proof;</li>
 * <li>the GUI is the 15-slot MUI panel (the wave-4 menu=null ruling, zero MenuType),
 *     gated on the upstream open chain {@code onBlockActivated3 :80-87}: dungeon loot
 *     generates on FIRST OPEN, then the panel opens;</li>
 * <li>the open gate: the mechanical safe is always open ({@link #isOpen()} = true — the
 *     upstream owner-claim layer {@code MultiTileEntitySafeMechanical mOwner} is DROPPED
 *     with the 01Root ownership routing, a declared fold); the Key Locked personality
 *     ({@link GT6SafeKeyLockedBlockEntity}) gates on its {@code mOpened} latch;</li>
 * <li>{@code onExploded :111 setToAir()} is carried by the block carrier's explosion face
 *     (the vanilla explosion drops flow through the block's loot/drop path with the
 *     resistance column standing between the explosion and the block).</li>
 * </ul>
 *
 * <h2>The dungeon-loot seam (ChestGenHooks :68-73 → the 1.20.1 LootTable injection)</h2>
 * <p>Upstream: the item NBT {@code gt.dungeonloot} names a ChestGenHooks category; on
 * first open (or break) {@code generateDungeonLoot} fills every EMPTY slot with
 * {@code ChestGenHooks.getOneItem(category)} and clears the name. The port keeps the
 * clause-for-clause shape (fill empties only, clear the marker, fire on first open AND on
 * break) over the modern roll: the marker names a LOOT TABLE id, the default roller pulls
 * it from the server {@code LootDataManager} ({@code LootTable.getRandomItems} over a
 * CHEST-context {@link LootParams}, one uniform pick from the rolled stacks — the
 * getOneItem equivalent). The datapack face: the {@code gt6:chests/safe_*} tables the
 * datagen ships are the tier-a injection seam pack authors (and KJS) can touch, wrapping
 * the vanilla dungeon tables the upstream categories named.
 *
 * <p>The {@link #rollOne} seam is the offline test entry: tests override it with a canned
 * roller and drive {@link #generateDungeonLootFrom} directly.
 */
public class GT6SafeBlockEntity extends GT6StaticStorageBaseBlockEntity implements gregtech6.gui.machines.GT6MuiMachine {

	/** The slot count (the metalset Safe rows :134-135 NBT_INV_SIZE = 15). */
	public static final int INVENTORY_SIZE = 15;

	/** Upstream "gt.dungeonloot" (:54/:60/:69) verbatim — the marker key on item and BE NBT. */
	public static final String NBT_DUNGEON_LOOT = "gt.dungeonloot";

	/** The loot-table marker (the ChestGenHooks category name upstream, the table id here). */
	public String mDungeonLootName = "";

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type at runtime. */
	public GT6SafeBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.SAFE_BE.get(), aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6SafeBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "safe_mechanical"; // upstream "gt.multitileentity.safe.mechanical" — the BET path mirrors it
	}

	@Override
	protected int inventorySize(BlockState aState) {
		return INVENTORY_SIZE;
	}

	/**
	 * The 15-slot MUI panel (the wave-4 menu=null ruling, zero MenuType) — the block's
	 * front-face use arm opens it through {@link gregtech6.gui.machines.GT6MuiMachine#tryOpen}
	 * once {@link #isOpen()} holds (the KeyLocked latch gates there).
	 */
	@Override
	public brachy.modularui.screen.ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData aData,
			brachy.modularui.value.sync.PanelSyncManager aSyncManager, brachy.modularui.screen.UISettings aSettings) {
		return gregtech6.gui.machines.GT6StorageMUI.safePanel(this, aSyncManager);
	}

	/** The open gate of the GUI arm — the mechanical safe never locks (the mOwner fold, class doc). */
	public boolean isOpen() {
		return true;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(byte aSide) {
		return new int[0]; // upstream :105 ZL_INTEGER — no automation face at all
	}

	@Override
	public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {
		return false; // upstream :106
	}

	@Override
	public boolean canExtractItem(int aSlot, byte aSide) {
		return false; // upstream :107
	}

	// ---------------------------------------------------------------------------
	// NBT (the marker rides the vanilla two-channel sync like the inventory)
	// ---------------------------------------------------------------------------

	@Override
	public void load(net.minecraft.nbt.CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_DUNGEON_LOOT, net.minecraft.nbt.Tag.TAG_STRING)) {
			mDungeonLootName = aNBT.getString(NBT_DUNGEON_LOOT);
		}
	}

	@Override
	protected void saveAdditional(net.minecraft.nbt.CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (!mDungeonLootName.isEmpty()) {
			aNBT.putString(NBT_DUNGEON_LOOT, mDungeonLootName);
		}
	}

	// ---------------------------------------------------------------------------
	// the dungeon-loot seam (class doc)
	// ---------------------------------------------------------------------------

	/**
	 * The guarded fill — the block-carrier call face (use arm + onRemove, the upstream
	 * {@code onBlockActivated3 :80-87} / {@code breakBlock :89-92} pair): level/server
	 * guards first, the actual fill never runs offline or client-side.
	 */
	public void tryGenerateDungeonLoot() {
		if (!hasLevel() || isClientSide() || mDungeonLootName.isEmpty()) return;
		generateDungeonLootFrom(this::rollOne);
	}

	/**
	 * The upstream {@code generateDungeonLoot :68-76} clause-for-clause: fill every EMPTY
	 * slot from the roller, clear the marker, update. Package seam so the offline test can
	 * drive it with a canned roller.
	 */
	void generateDungeonLootFrom(java.util.function.Function<String, ItemStack> aRoller) {
		if (!mDungeonLootName.isEmpty()) {
			for (int i = 0, j = invsize(); i < j; i++) {
				if (!slotHas(i)) {
					ItemStack tStack = aRoller.apply(mDungeonLootName);
					if (tStack != null && !tStack.isEmpty()) {
						slot(i, tStack);
					}
				}
			}
			mDungeonLootName = "";
			updateInventory();
		}
	}

	// ---------------------------------------------------------------------------
	// the explosion face (the upstream onExploded :111 setToAir() semantic)
	// ---------------------------------------------------------------------------

	/**
	 * The explosion destroy arm on the BE seam (the {@code GTBoilerTankBlockEntity
	 * .onExploded} shape — the block carrier's {@code onBlockExploded} calls this BEFORE
	 * its air swap): the contents AND the {@link #mDungeonLootName} marker die together.
	 * The marker half is the load-bearing one: the air swap (setBlock flag 3) fires the
	 * carrier's onRemove, whose break face re-rolls {@code tryGenerateDungeonLoot} — a
	 * surviving marker would refill the just-cleared 15 slots mid-remove and the pop walk
	 * would then scatter the whole pack onto the ground, breaking the upstream
	 * {@code setToAir()} contract (the contents are DESTROYED, never scattered; the
	 * not-yet-generated dungeon loot is destroyed with them). Upstream never hit this
	 * because its {@code setToAir} bypasses breakBlock entirely.
	 */
	public void destroyForExplosion() {
		for (int i = 0, l = getInventory().getSlots(); i < l; i++) {
			getInventory().setStackInSlot(i, ItemStack.EMPTY); // destroyed, not dropped
		}
		mDungeonLootName = ""; // the marker dies with the contents — onRemove must not re-roll
	}

	/**
	 * The ChestGenHooks.getOneItem equivalent over the modern roll: resolve the marker as a
	 * loot table id, roll the CHEST context once, return ONE uniform pick of the rolled
	 * stacks (empty when the id is malformed, the table is absent, or the roll is empty).
	 */
	protected ItemStack rollOne(String aTableName) {
		if (!hasLevel() || isClientSide() || !(getLevel() instanceof ServerLevel tLevel)) return ItemStack.EMPTY;
		ResourceLocation tId = ResourceLocation.tryParse(aTableName);
		if (tId == null) return ItemStack.EMPTY;
		//? if forge {
		// LootDataResolver.getLootTable(ResourceLocation) — the 1.20.1 resolver face (vanilla
		// LootDataResolver.java:25; the absent id falls back to the EMPTY table, the roll
		// below then yields nothing and the slot stays empty)
		LootTable tTable = tLevel.getServer().getLootData().getLootTable(tId);
		//?} else {
		/*// 21.1: the resolver lives on ReloadableServerRegistries.Holder, ResourceKey keyed
		// (ReloadableServerRegistries.java:145) — same fail-soft semantics, the absent id
		// resolves to the EMPTY table.
		LootTable tTable = tLevel.getServer().reloadableRegistries().getLootTable(
				net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, tId));
		*///?}
		List<ItemStack> tRolls = new ArrayList<>();
		LootParams tParams = new LootParams.Builder(tLevel)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(getBlockPos()))
				.create(LootContextParamSets.CHEST);
		tTable.getRandomItems(tParams, tRolls::add);
		if (tRolls.isEmpty()) return ItemStack.EMPTY;
		return tRolls.get(tLevel.random.nextInt(tRolls.size()));
	}
}
