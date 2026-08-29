package gregtech6.gui;

import javax.annotation.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.SlotItemHandler;

import gregtech6.tileentity.example.GTExampleChestBlockEntity;

/**
 * Chest menu — direct translation of the upstream chest GUI pair onto the 1.20.1 Menu model:
 * getGUIServer (MultiTileEntityChest.java:320, {@code new ContainerCommonChest(...)}) becomes
 * this constructor, and the client half (getGUIClient :319 / ContainerClientChest) is replaced
 * by the statically registered {@link GTExampleChestScreen}.
 *
 * <p>The dynamic row layout is ContainerCommonChest.java:39-43 verbatim — slot pitch 18px from
 * (8,18), partial last rows clipped by {@code i < tSize}, and the returned player-inventory
 * offset {@code 103+(tRows-4)*18} is exactly what vanilla ChestMenu computes
 * (ChestMenu.java:56-66). The upstream Slot_Normal over IInventory becomes
 * {@link SlotItemHandler} over the BE's GTItemStackHandler.
 *
 * <p>Openers counting follows the upstream Container lifecycle (ContainerCommon.java:57 ctor
 * hook → openInventoryGUI, removal → closeInventoryGUI), running on both sides because the
 * menu is constructed on both — the 1.7.10 ContainerCommon did the same.
 */
public class GTExampleChestMenu extends GTGuiMenu {

	/** The BE bound as the menu's backing container (upstream mTileEntity, ContainerCommon.java:42). */
	public final GTExampleChestBlockEntity tileEntity;

	/** Content rows — {@code tRows} of ContainerCommonChest.java:40, kept for the screen's panel height. */
	public final int containerRows;

	/** Server path (upstream getGUIServer): the BE hands out its own menu in createMenu. */
	public GTExampleChestMenu(MenuType<?> aMenuType, int aContainerId, Inventory aPlayerInventory, GTExampleChestBlockEntity aTileEntity) {
		super(aMenuType, aContainerId, aPlayerInventory);
		this.tileEntity = aTileEntity;
		aTileEntity.openInventoryGUI(); // ContainerCommon.java:57 (upstream ctor hook)

		// ContainerCommonChest.java:39-43 verbatim, Slot_Normal -> SlotItemHandler
		int tSize = aTileEntity.getInventory().getSlots();
		this.containerRows = tSize / 9 + (tSize % 9 == 0 ? 0 : 1);
		for (int y = 0, i = 0; y < this.containerRows; y++) {
			for (int x = 0; x < 9 && i < tSize; x++) {
				addSlot(new SlotItemHandler(aTileEntity.getInventory(), i++, 8 + x * 18, 18 + y * 18));
			}
		}
		bindPlayerInventory(103 + (this.containerRows - 4) * 18); // :42 return value (ContainerCommon.java:62-63)
	}

	/**
	 * IContainerFactory shape (IContainerFactory.java:15): the network path constructs through
	 * this ctor, resolving the BE from the BlockPos payload that NetworkHooks.openScreen
	 * (NetworkHooks.java:176) writes for exactly this purpose. Both sides always carry the
	 * payload — the vanilla two-arg MenuSupplier bridge ({@code data = null}) has no position
	 * to resolve and fails loudly instead of silently building a detached menu.
	 */
	public GTExampleChestMenu(int aContainerId, Inventory aPlayerInventory, @Nullable FriendlyByteBuf aExtraData) {
		this(GTMenuTypes.exampleChest(), aContainerId, aPlayerInventory, resolveTileEntity(aPlayerInventory, aExtraData));
	}

	private static GTExampleChestBlockEntity resolveTileEntity(Inventory aPlayerInventory, @Nullable FriendlyByteBuf aExtraData) {
		if (aExtraData == null) {
			throw new IllegalArgumentException("gt6:example_chest menu requires the NetworkHooks.openScreen BlockPos payload (no data bridge)");
		}
		if (aPlayerInventory.player.level().getBlockEntity(aExtraData.readBlockPos()) instanceof GTExampleChestBlockEntity tTileEntity) {
			return tTileEntity;
		}
		throw new IllegalArgumentException("gt6:example_chest menu opened without its block entity (chunk not loaded?)");
	}

	/**
	 * Upstream canInteractWith → mTileEntity.isUseableByPlayerGUI (ContainerCommon.java:326);
	 * isUseableByPlayer (TileEntityBase05Inventories.java:104) minus the mOwner-gated
	 * allowInteraction (dropped with the IPacket stack): !isDead and distance&sup2; &le; 64.
	 */
	@Override
	public boolean stillValid(Player aPlayer) {
		return !this.tileEntity.isDead() && !this.tileEntity.isRemoved()
			&& aPlayer.distanceToSqr(
				this.tileEntity.getBlockPos().getX() + 0.5D,
				this.tileEntity.getBlockPos().getY() + 0.5D,
				this.tileEntity.getBlockPos().getZ() + 0.5D) <= 64.0D;
	}

	@Override
	public ItemStack quickMoveStack(Player aPlayer, int aIndex) {
		return quickMoveBetween(aPlayer, aIndex, this.tileEntity.getInventory().getSlots());
	}

	@Override
	public void removed(Player aPlayer) {
		super.removed(aPlayer);
		this.tileEntity.closeInventoryGUI(); // upstream closeInventoryGUI :253
	}
}
