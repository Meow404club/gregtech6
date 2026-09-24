package gregtech6.covers.covers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;

import gregtech6.covers.CoverData;

/**
 * The crafting-table cover — 1.20.1 port of gregapi/cover/covers/CoverCrafting.java:36-60
 * (task p37-covers-crafting-asphalt; upstream item id 1001 "Crafting Table Cover",
 * MultiItemTechnological.java:60). Right-clicking the covered face opens the VANILLA
 * workbench GUI anchored at the covered host — a machine face that carries a regular old
 * crafting table on it.
 *
 * <p>The upstream packet dance (:48-52 getNextWindowId/S2DPacketOpenWindow/openContainer/
 * addCraftingToCrafters) is the 1.7.10 hand-rolled form of the platform-native
 * {@code ServerPlayer.openMenu(MenuProvider)} face; the {@code ContainerWorkbench} with the
 * always-true {@code canInteractWith} (:50) is the {@link CraftingMenu} whose
 * {@code stillValid} the anonymous override pins to {@code true} verbatim — the menu never
 * dies while the host lives (the covered host is NOT a vanilla CraftingTableBlock, so the
 * default block-checking stillValid would insta-close it).
 *
 * <p>Declared folds: the upstream 6-variant texture row ({@code covers/crafting/0..5}) keeps
 * variant 0 (the single sprite the plate renderer paints, the p35 declared-fold posture);
 * {@code showsConnectorFront=F} (:59) rides the pooled connector-hook family (the P4 cut —
 * there is no connector face to show); {@code isDecorative=T} (:58) rides
 * {@link CoverTextureSimple} :63.
 */
public class CoverCrafting extends CoverTextureSimple {

	//? if forge {
	public static final ResourceLocation CRAFTING_SPRITE = new ResourceLocation("gt6", "block/crafting/0");
	//?} else {
	/*public static final ResourceLocation CRAFTING_SPRITE = ResourceLocation.fromNamespaceAndPath("gt6", "block/crafting/0");
	 *///?}

	public CoverCrafting() {
		super(CRAFTING_SPRITE); // the WOOD_PLACE/STONE_BREAK placeholder sounds (the p4 posture)
	}

	/** Upstream :46-55 — the server-player gate, the workbench menu at the covered pos, the consumed click. */
	@Override
	public boolean onCoverClickedRight(byte aSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (aPlayer instanceof ServerPlayer tPlayer) { // upstream :47 EntityPlayerMP gate
			BlockPos tPos = aData.mTileEntity.self().getBlockPos();
			Level tLevel = tPlayer.level();
			tPlayer.openMenu(new SimpleMenuProvider(
					(aId, aInventory, aOpener) -> new CraftingMenu(aId, aInventory, ContainerLevelAccess.create(tLevel, tPos)) {
						@Override public boolean stillValid(Player aInside) {return true;} // upstream :50 canInteractWith → T
					},
					Component.translatable("container.crafting"))); // the S2DPacketOpenWindow "Crafting" window title
		}
		return true; // :54 — the click is consumed
	}

	/** Upstream :57 — the crafting face never seals the host. */
	@Override public boolean isSealable(byte aCoverSide, CoverData aData) {return false;}
}
