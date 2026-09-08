package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

import gregtech6.registry.GT6FeBatteries;
import gregtech6.tileentity.energy.GT6FeBatteryBlockEntity;

/**
 * {@code /gt6febattery} — the FE battery fixture command (task p26-eu-bridge-outbound),
 * the /gt6energy template (game-bus listener, self-contained per ADR-P3-4).
 *
 * <ul>
 * <li>{@code place <pos>} — headless placement: setBlock the battery block at pos.</li>
 * <li>{@code stat <pos>} — dump stored FE / capacity and the implied EU at the 4:1 bridge
 *     ratio (the acceptance assertion face: the RCON chain reads this after emitting).</li>
 * <li>{@code reset <pos>} — drain the battery back to 0 FE (re-run the chain in place).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6FeBatteryCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6FeBatteryCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tBattery = Commands.literal("gt6febattery")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("place")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("stat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("reset")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> reset(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		aEvent.getDispatcher().register(tBattery);
		LOGGER.info("Registered GT6 FE battery fixture command /gt6febattery (place|stat|reset)");
	}

	private static int place(CommandSourceStack aSource, BlockPos aPos) {
		aSource.getLevel().setBlock(aPos, GT6FeBatteries.FE_BATTERY.get().defaultBlockState(), Block.UPDATE_ALL);
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6FeBatteryBlockEntity)) {
			aSource.sendFailure(Component.literal("PLACE FAILED: no FE battery BE at " + aPos.toShortString()));
			return 0;
		}
		String tLine = "GT6 FE battery placed at " + aPos.toShortString() + ": stored 0 FE, capacity "
				+ GT6FeBatteryBlockEntity.CAPACITY + " FE";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6FeBatteryBlockEntity tBattery)) {
			aSource.sendFailure(Component.literal("STAT FAILED: no FE battery BE at " + aPos.toShortString()));
			return 0;
		}
		int tStored = tBattery.storedFe();
		// the implied EU face: stored FE / 4 (the CS.RF_PER_EU ratio); "charged" is the
		// substring-stable state word the RCON chain judges (empty vs bridged-into)
		String tLine = "GT6 FE battery stat at " + aPos.toShortString() + ": stored " + tStored
				+ " FE, capacity " + GT6FeBatteryBlockEntity.CAPACITY + " FE, implied EU " + (tStored / 4)
				+ ", charged " + (tStored > 0);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int reset(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6FeBatteryBlockEntity tBattery)) {
			aSource.sendFailure(Component.literal("RESET FAILED: no FE battery BE at " + aPos.toShortString()));
			return 0;
		}
		tBattery.resetStoredFe(0); // the protected-field write face (the BE class doc)
		String tLine = "GT6 FE battery reset at " + aPos.toShortString() + ": stored 0 FE";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}
}
