package gregtech6.tileentity.example;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.gui.GTExampleChestMenu;
import gregtech6.gui.GTMenuTypes;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 port of the upstream example chest, gregapi/block/multitileentity/example/
 * MultiTileEntityChest.java:81 ("An example implementation of a Chest with my
 * MultiTileEntity System") — the WAVE-2 vehicle that joins the BE framework
 * (TileEntityBase01Root/03TicksAndSync) and the Menu/Screen framework (GTGuiMenu/
 * GTGuiScreen) into one playable chain.
 *
 * <p>Port scope (task p3-example-machine):
 * <ul>
 * <li>inventory — the upstream ItemStack[] of TileEntityBase05Inventories (:42) becomes
 *     a {@link GTItemStackHandler} exposed to the menu through SlotItemHandler and to the
 *     world through the base ITEM_HANDLER capability (ADR-P3-2); contents persist in
 *     saveAdditional/load, the role of upstream readFromNBT2/writeToNBT2
 *     (TileEntityBase05Inventories.java:47-67, key "gt.invlist" CS.java:1249 — the vanilla
 *     ItemStackHandler list shape replaces the "s"-short list, so the key keeps the
 *     in-repo plain form consistent with the base "te_name");</li>
 * <li>facing — upstream NBT byte mFacing (:84, written :110, key "gt.facing" CS.java:1191)
 *     with the placement hook onPlaced (:128-131): the SIDES_HORIZONTAL form of
 *     UT.Code.getSideForPlayerPlacing (UT.java:1755) cannot hit its pitch branches, so the
 *     result is always the player's horizontal look direction (UT.java:1751-1753); the GT6
 *     side order equals Direction.getIndex() (TileEntityBase01Root port doc);</li>
 * <li>openers — mUsingPlayers (:84) driven by openInventoryGUI/closeInventoryGUI (:252-253)
 *     through the menu construction/removal hooks (ContainerCommon.java:57), with the
 *     onTickCheck/onTickResetChecks sync gate (:160-168) verbatim; the trapped-chest block
 *     update branch and the 1200-tick getOpenGUIs() resync (:145-147) are dropped together
 *     with mIsTrapped and the ITileEntityGUI open-GUI counter;</li>
 * <li>MenuProvider — {@link #createMenu} is the direct translation of getGUIServer (:320);
 *     getGUIClient (:319) is NOT ported — static MenuScreens registration replaced the
 *     per-open client dispatch. onBlockActivated2's open-side lives on
 *     gregtech6.block.GTExampleChestBlock.use.</li>
 * </ul>
 *
 * <p>Omissions relative to upstream (feature layer, later cards): paint/RGB state, dungeon
 * loot, comparator output, the chest TESR/lid animation (mUsingPlayers' only client
 * consumer) and the IMTE_* family (:81). getTileEntityName follows the in-repo convention
 * (upstream :251 "gt.multitileentity.chest" is the MTE-registry-qualified form; the BET
 * registry path mirrors this name like the base class doc prescribes).
 */
public class GTExampleChestBlockEntity extends TileEntityBase03TicksAndSync implements MenuProvider {

	/** NBT key of {@link #mFacing} (upstream "gt.facing", CS.java:1191). */
	public static final String NBT_FACING = "facing";

	/** NBT key of the slot contents (upstream "gt.invlist", CS.java:1249). */
	public static final String NBT_INVENTORY = "inventory";

	/** Slot count — the registration-parameter NBT_INV_SIZE, 54 (Loader_MultiTileEntities.java:132), 6 rows of 9. */
	public static final int INVENTORY_SIZE = 54;

	/** Upstream :84 — GT6 side order == Direction.getIndex() order. */
	protected byte mFacing = 3;

	/** Upstream :84 — live/opened state pair feeding the :160-168 sync gate. */
	protected byte mUsingPlayers = 0, oUsingPlayers = 0;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTExampleChestBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.EXAMPLE_CHEST_BE.get(), aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point: Builder.of(...).build(null) works without a registry. */
	public GTExampleChestBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		// the chest ticks: the upstream MTE sits on the 03 ticking chain (mIsTicking = true)
		super(true, aType, aPos, aState);
		// 05Inventories default 54-slot inventory; isItemValidForSlot = T upstream (:116), so no filter
		setInventory(new GTItemStackHandler(INVENTORY_SIZE, this::setChanged));
	}

	@Override
	public String getTileEntityName() {
		return "example_chest"; // upstream :251 — BET registry path mirrors it
	}

	// ---------------------------------------------------------------------------
	// placement (upstream onPlaced :128-131)
	// ---------------------------------------------------------------------------

	/** Upstream mFacing = UT.Code.getSideForPlayerPlacing(aPlayer, mFacing, SIDES_HORIZONTAL), see class doc. */
	public void setFacingFromPlacement(Player aPlayer) {
		// get3DDataValue = the GT6 side order 0..5 (Direction.java:119, TileEntityBase01Root port doc)
		mFacing = (byte) aPlayer.getDirection().get3DDataValue();
	}

	public byte getFacing() {
		return mFacing;
	}

	/** The menu binds this as the SlotItemHandler container (TestMachineBlockEntity.getInventory precedent). */
	public GTItemStackHandler getInventory() {
		return mInventory;
	}

	public int getUsingPlayers() {
		return mUsingPlayers;
	}

	// ---------------------------------------------------------------------------
	// openers (upstream :252-253 minus the mIsTrapped block-update branch)
	// ---------------------------------------------------------------------------

	/** Upstream :252 — called from the menu constructor (ContainerCommon.java:57), both sides like the 1.7.10 ctor. */
	public void openInventoryGUI() {
		mUsingPlayers++;
	}

	/** Upstream :253 — called when the menu is removed (both sides, vanilla menu lifecycle). */
	public void closeInventoryGUI() {
		mUsingPlayers--;
	}

	@Override
	public boolean onTickCheck(long aTimer) {
		// upstream :160-163 verbatim
		return mUsingPlayers != oUsingPlayers || super.onTickCheck(aTimer);
	}

	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		// upstream :164-168 verbatim
		super.onTickResetChecks(aTimer, aIsServerSide);
		oUsingPlayers = mUsingPlayers;
	}

	// ---------------------------------------------------------------------------
	// GUI (upstream :319-320)
	// ---------------------------------------------------------------------------

	@Override
	public AbstractContainerMenu createMenu(int aContainerId, Inventory aPlayerInventory, Player aPlayer) {
		return new GTExampleChestMenu(GTMenuTypes.exampleChest(), aContainerId, aPlayerInventory, this);
	}

	@Override
	public Component getDisplayName() {
		// BaseContainerBlockEntity.getName semantics: the container title is the block's name
		return getBlockState().getBlock().getName();
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :94-105 / writeToNBT2 :108-113, inventory :47-67)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_FACING, mFacing);
		//? if forge {
		aNBT.put(NBT_INVENTORY, mInventory.serializeNBT());
		//?} else {
		/*aNBT.put(NBT_INVENTORY, mInventory.serializeNBT(NBT_ACCESS)); // 21.1: ItemStackHandler NBT takes the registries
		*///?}
		// mUsingPlayers/mRGBa/hardness stay out of the disk NBT exactly like upstream :108-113
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_FACING, Tag.TAG_ANY_NUMERIC)) {
			mFacing = aNBT.getByte(NBT_FACING);
		}
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) {
			//? if forge {
			mInventory.deserializeNBT(aNBT.getCompound(NBT_INVENTORY));
			//?} else {
			/*mInventory.deserializeNBT(NBT_ACCESS, aNBT.getCompound(NBT_INVENTORY)); // 21.1: provider-first
			*///?}
		}
	}
}
