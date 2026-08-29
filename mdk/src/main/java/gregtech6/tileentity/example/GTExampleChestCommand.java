package gregtech6.tileentity.example;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;

import gregtech6.gui.GTMenuTypes;

/**
 * {@code /gt6machine open} — the automated open-chain proof for the example chest
 * (task p3-example-machine acceptance 4), the {@code /gt6gui} shape (GTGuiCommand) applied to
 * a real MenuProvider block entity: the command aims at a GTExampleChestBlockEntity and calls
 * NetworkHooks.openScreen(serverPlayer, chest, pos) (NetworkHooks.java:176) — the exact call
 * GTExampleChestBlock.use makes — so the server-side MenuProvider.createMenu factory runs for
 * real; a success return plus a clean log line is the evidence (createMenu failures propagate
 * through the command as exceptions).
 *
 * <p>Game-bus listener (default {@code Bus.FORGE}) and self-contained per ADR-P3-4. The
 * blocked-above placement guard of the block's use() is deliberately not replayed here — this
 * exercises the open chain, not the placement guard.
 */
@Mod.EventBusSubscriber(modid = GTMenuTypes.MOD_ID)
public final class GTExampleChestCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final double REACH = 8.0D;

	private GTExampleChestCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(
			Commands.literal("gt6machine")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("open")
					.executes(context -> openChest(context.getSource().getPlayerOrException()))));
		LOGGER.info("Registered GT6 open-chain command /gt6machine open");
	}

	private static int openChest(ServerPlayer player) throws CommandSyntaxException {
		if (!(player.pick(REACH, 1.0F, false) instanceof BlockHitResult hit)) {
			player.sendSystemMessage(Component.literal("No block in reach (aim at the example chest)"));
			return 0;
		}
		BlockPos pos = hit.getBlockPos();
		if (!(player.level().getBlockEntity(pos) instanceof GTExampleChestBlockEntity chest)) {
			player.sendSystemMessage(Component.literal("No GTExampleChestBlockEntity in reach (aim at the example chest)"));
			return 0;
		}
		// the real chain: createMenu(server side) + OpenContainer packet + client menu rebuild
		NetworkHooks.openScreen(player, chest, pos);
		LOGGER.info("GT6 open chain verified: MenuProvider.createMenu ran for GTExampleChestBlockEntity at {} (using players now {})", pos, chest.getUsingPlayers());
		return Command.SINGLE_SUCCESS;
	}
}
