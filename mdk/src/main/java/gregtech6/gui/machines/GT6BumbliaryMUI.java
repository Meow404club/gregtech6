package gregtech6.gui.machines;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

//? if forge {
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.LoggerFactory;
 *///?}

import brachy.modularui.api.IUIHolder;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.factory.AbstractUIFactory;
import brachy.modularui.factory.GuiManager;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import gregtech6.tileentity.bees.GT6BumbliaryBlockEntity;

/**
 * The Bumbliary ModularUI panel factory (task p34-bumbliary-gui) — the
 * {@code MultiTileEntityBumbliary.java:389-509} container pair as one factory over the
 * BE's two dispatch flags: {@code mAdvanced} picks the 36-slot primary 4x9 grid
 * (x=8..152 step 18, y=8/26/44/62 — the :396-434 rows) or the 20-slot advanced 4x5 grid
 * (x=44..116, the Advanced :397-419 rows), and the scoop flag picks the interaction flags
 * (the normal pair :396-434 vs the fully-open ROYAL/DRONE scoop pair :450-488/:435-457).
 * The per-seat geometry and flags are the BE's {@link GT6BumbliaryBlockEntity#guiSeat}
 * tables — the factory only renders them. Backgrounds are the borrowed upstream pair
 * {@code machines/Bumbliary.png} (:500/:507) and {@code machines/BumbliaryAdvanced.png}
 * (the Advanced :469).
 *
 * <p><b>The two-GUI channel</b>: the framework's own doctrine for several GUIs over one
 * holder is one factory per GUI ("to make sure they are same on client and server",
 * SimpleUIFactory javadoc) — the factory identity rides the OpenGuiPacket wire
 * (OpenGuiPacket.java:36/:66 resolve the name on the client), so {@link Factory#NORMAL}
 * and {@link Factory#SCOOP} carry the variant without touching the pos-only PosGuiData
 * wire format and without any MenuType (the P26 no-new-MenuType ruling; the factories
 * register through the GTBasicMachinesMenus construct seam). The normal GUI also opens
 * through the plain {@link GT6MuiMachine#tryOpen} chain (the BE's buildUI delegates here
 * with {@code scoop=false}).
 *
 * <p>The headless gate is the mui-a finding: {@code PanelSyncManager.getPlayer()}
 * dereferences the absent container menu offline, so the player face (bind + widget) is
 * gated on {@code getContainer() == null} (the GT6StorageMUI :52 form); at runtime both
 * sides always have one.
 */
public final class GT6BumbliaryMUI {

	/** The panel names (the MUI2 main-panel keys — the scoop pair gets its own). */
	public static final String PANEL_NAME = "bumbliary";
	public static final String PANEL_NAME_SCOOP = "bumbliary_scoop";

	/** The slot group of the whole hive (the shift-transfer face, rowSize follows the grid). */
	public static final String GROUP_SLOTS = "bumbliary_slots";

	private static final Logger LOGGER = log();

	private GT6BumbliaryMUI() {
	}

	/** The leg-generic logger (LogUtils vs LoggerFactory, the fork seam of the two mod buses). */
	private static Logger log() {
		//? if forge {
		return LogUtils.getLogger();
		//?} else {
		/*return LoggerFactory.getLogger("gt6");
		 *///?}
	}

	/**
	 * The panel body — runs on SERVER and CLIENT (the sync handlers register here, the
	 * IUIHolder.buildUI contract). Consumed by the BE's {@code buildUI} (the normal pair)
	 * and by {@link Factory#SCOOP} (the scoop pair).
	 */
	public static ModularPanel<?> buildPanel(GT6BumbliaryBlockEntity aBumbliary, PanelSyncManager aSyncManager, boolean aScoop) {
		aSyncManager.registerSlotGroup(GROUP_SLOTS, aBumbliary.advanced() ? 5 : 9);
		boolean tHeadless = aSyncManager.getContainer() == null;
		if (!tHeadless) {
			aSyncManager.bindPlayerInventory(aSyncManager.getPlayer());
		}

		ModularPanel<?> tPanel = ModularPanel.defaultPanel(aScoop ? PANEL_NAME_SCOOP : PANEL_NAME, 176, 166)
				.background(UITexture.fullImage("gt6", aBumbliary.advanced()
						? "textures/gui/machines/bumbliaryadvanced.png" // the Advanced :469
						: "textures/gui/machines/bumbliary.png")); // the :500/:507 pair

		for (int i = 0; i < aBumbliary.slotCount(); i++) {
			GT6BumbliaryBlockEntity.GuiSeat tSeat = aBumbliary.guiSeat(aScoop, i);
			tPanel.child(new ItemSlot()
					.slot(new ModularSlot(aBumbliary.inventory(), tSeat.slot())
							.canPut(tSeat.canPut())
							.canTake(tSeat.canTake())
							.slotGroup(GROUP_SLOTS))
					.pos(tSeat.x(), tSeat.y())
					.name("slot_" + tSeat.slot()));
		}

		if (!tHeadless) {
			// the player inventory at the standard 176x166 machine-panel offset 84
			tPanel.child(SlotGroupWidget.playerInventory((aIndex, aSlot) -> aSlot).pos(7, 84));
		}
		return tPanel;
	}

	/**
	 * The open chain of the GUI pair (the class doc carries the wire rationale): the scoop
	 * GUI has no MenuType and no PosGuiData variant — the factory IS the variant.
	 */
	public static final class Factory extends AbstractUIFactory<PosGuiData> {

		/** The normal GUI factory (the :291 openGUI 0). */
		public static final Factory NORMAL = new Factory(PANEL_NAME, false);
		/** The scoop GUI factory (the :286 creative shortcut and the :309 scoop arm). */
		public static final Factory SCOOP = new Factory(PANEL_NAME_SCOOP, true);

		private final boolean mScoop;

		private Factory(String aName, boolean aScoop) {
			super(new ResourceLocation("gt6", aName));
			mScoop = aScoop;
		}

		/** The variant flag — the panel dispatch key. */
		public boolean scoop() {
			return mScoop;
		}

		/**
		 * The server open (the {@code BlockEntityUIFactory.open} :36-42 shape over this
		 * factory's identity — GuiManager carries the wire packet).
		 */
		public void open(ServerPlayer aPlayer, GT6BumbliaryBlockEntity aBumbliary) {
			GuiManager.open(this, new PosGuiData(aPlayer, aBumbliary.getBlockPos()), aPlayer);
		}

		/** The panel dispatch — the variant rides the factory, not the wire data. */
		@Override
		public ModularPanel<?> createPanel(PosGuiData aData, PanelSyncManager aSyncManager, UISettings aSettings) {
			return GT6BumbliaryMUI.buildPanel((GT6BumbliaryBlockEntity)aData.getBlockEntity(), aSyncManager, mScoop);
		}

		@Override
		public @NotNull IUIHolder<PosGuiData> getGuiHolder(PosGuiData aData) {
			return (GT6BumbliaryBlockEntity)aData.getBlockEntity();
		}

		@Override
		public boolean canInteractWith(Player aPlayer, PosGuiData aGuiData) {
			// the BlockEntityUIFactory :70-73 body verbatim — the 64-sq range gate
			return aPlayer == aGuiData.getPlayer() && aGuiData.getBlockEntity() != null
					&& aGuiData.getSquaredDistance(aPlayer) <= 64;
		}

		/** The wire faces — the BlockEntityUIFactory :77/:86 bodies verbatim (pos-only; the variant rides the factory identity). */
		@Override
		//? if forge {
		public void writeGuiData(PosGuiData aGuiData, net.minecraft.network.FriendlyByteBuf aBuffer) {
			aBuffer.writeBlockPos(aGuiData.getBlockPos());
		}

		@Override
		public @NotNull PosGuiData readGuiData(Player aPlayer, net.minecraft.network.FriendlyByteBuf aBuffer) {
			return new PosGuiData(aPlayer, aBuffer.readBlockPos());
		}
		//?} else {
		/*public void writeGuiData(PosGuiData aGuiData, net.minecraft.network.RegistryFriendlyByteBuf aBuffer) {
			aBuffer.writeBlockPos(aGuiData.getBlockPos());
		}

		@Override
		public @NotNull PosGuiData readGuiData(Player aPlayer, net.minecraft.network.RegistryFriendlyByteBuf aBuffer) {
			return new PosGuiData(aPlayer, aBuffer.readBlockPos());
		}
		 *///?}
	}

	/**
	 * {@code /gt6bumbliary} — the acceptance arms (the card-local command form,
	 * GT6DrinkCommand shape): the penalty walk + the sting run verbatim and the panel
	 * construct half runs headless; the MUI network half is client-boundary (the
	 * sanctioned fake-player SKIP face, GTAdvancedCraftingTableCommand :306).
	 */
	@Mod.EventBusSubscriber(modid = "gt6")
	public static final class Command {

		private Command() {
		}

		/** Leg-generic logger (the outer log() stays private-static, reused here). */
		private static final Logger CMD_LOGGER = log();

		@SubscribeEvent
		public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
			LiteralArgumentBuilder<CommandSourceStack> tCmd = Commands.literal("gt6bumbliary")
					.requires(aSource -> aSource.hasPermission(2));
			tCmd.then(Commands.literal("use")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(aContext -> poke(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), false))));
			tCmd.then(Commands.literal("scoop")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(aContext -> poke(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), true))));
			tCmd.then(Commands.literal("open")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(aContext -> open(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
			aEvent.getDispatcher().register(tCmd);
			CMD_LOGGER.info("Registered GT6 bumbliary command /gt6bumbliary (use|scoop|open) — the p34-bumbliary-gui acceptance home");
		}

		/**
		 * The use/scoop driver arm — the survival walk the block use face runs
		 * ({@code penalize} + {@code sting}), then the headless panel construct (the
		 * server half of the open chain verbatim; the network half is the sanctioned
		 * fake-player SKIP — the real-player open rides the block use face).
		 */
		private static int poke(CommandSourceStack aSource, BlockPos aPos, boolean aScoop) {
			if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6BumbliaryBlockEntity tBumbliary)) {
				aSource.sendFailure(Component.literal("No bumbliary at " + aPos.toShortString()));
				return 0;
			}
			//? if forge {
			ServerPlayer tFake = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(aSource.getLevel());
			//?} else {
			/*ServerPlayer tFake = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(aSource.getLevel());
			 *///?}
			tBumbliary.penalize(aScoop && tFake.isCreative()); // :289 (use) / :307 (scoop, the creative exemption inside)
			boolean tStung = tBumbliary.sting(tFake); // :290/:308
			ModularPanel<?> tPanel = GT6BumbliaryMUI.buildPanel(tBumbliary, headlessSyncManager(), aScoop);
			String tLine = "GT6 bumbliary " + (aScoop ? "scoop" : "use") + " at " + aPos.toShortString()
					+ ": penalty=" + tBumbliary.mBreedingCountDown + " stung=" + tStung
					+ " panel=" + tPanel.getName() + " slots=" + tBumbliary.slotCount()
					+ " (the MUI open half SKIPs for the fake player)";
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			CMD_LOGGER.info(tLine);
			return com.mojang.brigadier.Command.SINGLE_SUCCESS;
		}

		/**
		 * The openGUI smoke arm — the panel construct + the sync wiring verbatim (the
		 * server half of the GuiManager.open chain); the network half is client-boundary.
		 */
		private static int open(CommandSourceStack aSource, BlockPos aPos) {
			if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6BumbliaryBlockEntity tBumbliary)) {
				aSource.sendFailure(Component.literal("No bumbliary at " + aPos.toShortString()));
				return 0;
			}
			ModularPanel<?> tPanel = GT6BumbliaryMUI.buildPanel(tBumbliary, headlessSyncManager(), false);
			String tLine = "GT6 bumbliary open at " + aPos.toShortString()
					+ ": panel=" + tPanel.getName() + " slots=" + tBumbliary.slotCount()
					+ " advanced=" + tBumbliary.advanced()
					+ " (the MUI open chain server half; the network half is client-boundary)";
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			CMD_LOGGER.info(tLine);
			return com.mojang.brigadier.Command.SINGLE_SUCCESS;
		}

		/** The headless sync manager (the GT6StorageMUI offline form — no player to bind). */
		private static PanelSyncManager headlessSyncManager() {
			return new PanelSyncManager(new ModularSyncManager(false), true);
		}
	}
}
