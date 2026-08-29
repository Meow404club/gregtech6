package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import gregtech6.gui.GTDebugMenu;
import gregtech6.gui.GTMenuTypes;

/**
 * {@code /gt6gui} debug command — opens the {@link GTDebugMenu} through an anonymous
 * {@link MenuProvider} (no block entity, no position payload) to exercise the server-side half of
 * the GUI chain: the vanilla {@code ServerPlayer.openMenu(MenuProvider)} path (ServerPlayer.java:976)
 * assigns the container counter, builds the menu via {@link MenuProvider#createMenu} and syncs it;
 * the client then resolves the screen from the registered MenuType. NetworkHooks.openScreen
 * (NetworkHooks.java:161/176/192) is only needed when extra data must ride along, which the
 * anonymous debug GUI does not — the example machine card will use it for the BlockPos payload.
 *
 * <p>Game-bus listener (default {@code Bus.FORGE}) and self-contained per ADR-P3-4.
 */
@Mod.EventBusSubscriber(modid = GTMenuTypes.MOD_ID)
public final class GTGuiCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    private GTGuiCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("gt6gui")
                .requires(source -> source.hasPermission(2))
                .executes(context -> openDebugGui(context.getSource().getPlayerOrException())));
        LOGGER.info("Registered GT6 debug command /gt6gui");
    }

    /** Opens the debug GUI for the executing player; returns {@link Command#SINGLE_SUCCESS}. */
    private static int openDebugGui(ServerPlayer player) throws CommandSyntaxException {
        player.openMenu(new MenuProvider() {
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new GTDebugMenu(containerId, playerInventory);
            }

            @Override
            public Component getDisplayName() {
                return GTDebugMenu.title();
            }
        });
        return Command.SINGLE_SUCCESS;
    }
}
