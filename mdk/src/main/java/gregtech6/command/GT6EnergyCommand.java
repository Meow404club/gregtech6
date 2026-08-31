package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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

import gregtech6.registry.GTEnergySources;
import gregtech6.tileentity.energy.GTEnergySourceBlockEntity;

/**
 * {@code /gt6energy} — the command-driven test generator acceptance command (task
 * p8-d4-energy-source spec ③, ADR 2026-08-31-p7-energy-network ruling 4). Game-bus
 * listener, self-contained per ADR-P3-4, the GTWireCommand template.
 *
 * <ul>
 * <li>{@code place <pos>} — headless placement: setBlock the source block at pos.</li>
 * <li>{@code mode <pos> <on|off>} — flip {@link GTEnergySourceBlockEntity#mEmitting};
 *     on = the server tick starts pushing size=voltage x amount=amperage EU packets
 *     into accepting neighbours (the upstream SolarPanelElectric :160-162 emit form).</li>
 * <li>{@code volt <pos> <v>} / {@code amp <pos> <a>} — the RCON-adjustable ratings.</li>
 * <li>{@code stat <pos>} — dump voltage/amperage/emitting/wattage.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6EnergyCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6EnergyCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		aEvent.getDispatcher().register(
			Commands.literal("gt6energy")
				.requires(aSource -> aSource.hasPermission(2))
				.then(Commands.literal("place")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("mode")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("state", StringArgumentType.word())
							.executes(aContext -> mode(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									StringArgumentType.getString(aContext, "state"))))))
				.then(Commands.literal("volt")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("voltage", IntegerArgumentType.integer(1, 1000000))
							.executes(aContext -> volt(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									IntegerArgumentType.getInteger(aContext, "voltage"))))))
				.then(Commands.literal("amp")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("amperage", IntegerArgumentType.integer(1, 1000000))
							.executes(aContext -> amp(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									IntegerArgumentType.getInteger(aContext, "amperage"))))))
				.then(Commands.literal("stat")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))));
		LOGGER.info("Registered GT6 energy source command /gt6energy (place|mode|volt|amp|stat)");
	}

	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTEnergySourceBlockEntity aEnergySource)) {
			aSource.sendFailure(Component.literal("No GTEnergySourceBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		String tLine = "GT6 energy source stat at " + aPos.toShortString() + ": voltage " + aEnergySource.mVoltage
				+ " EU, amperage " + aEnergySource.mAmperage + " A, emitting " + aEnergySource.mEmitting
				+ ", wattage " + (aEnergySource.mVoltage * aEnergySource.mAmperage) + " EU/t";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The headless placement driver (spec ③): setBlock, the BE rides the vanilla chain. */
	private static int place(CommandSourceStack aSource, BlockPos aPos) {
		aSource.getLevel().setBlock(aPos, GTEnergySources.ENERGY_SOURCE.get().defaultBlockState(), Block.UPDATE_ALL);
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTEnergySourceBlockEntity)) {
			aSource.sendFailure(Component.literal("PLACE FAILED: no energy source BE at " + aPos.toShortString()));
			return 0;
		}
		String tLine = "GT6 energy source placed at " + aPos.toShortString() + ": voltage 32 EU, amperage 1 A, emitting false";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The RCON mode gate (spec ③): on = the tick emit runs, off = it does not. */
	private static int mode(CommandSourceStack aSource, BlockPos aPos, String aState) {
		Boolean tOn = switch (aState) {
			case "on" -> Boolean.TRUE;
			case "off" -> Boolean.FALSE;
			default -> null;
		};
		if (tOn == null) {
			aSource.sendFailure(Component.literal("MODE FAILED: unknown state '" + aState + "' (use on or off)"));
			return 0;
		}
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTEnergySourceBlockEntity tSource)) {
			aSource.sendFailure(Component.literal("No GTEnergySourceBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		tSource.setEmitting(tOn);
		String tLine = "GT6 energy source mode at " + aPos.toShortString() + ": emitting " + tSource.mEmitting;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int volt(CommandSourceStack aSource, BlockPos aPos, long aVoltage) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTEnergySourceBlockEntity tSource)) {
			aSource.sendFailure(Component.literal("No GTEnergySourceBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		tSource.setVoltage(aVoltage);
		String tLine = "GT6 energy source volt at " + aPos.toShortString() + ": voltage " + tSource.mVoltage + " EU";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int amp(CommandSourceStack aSource, BlockPos aPos, long aAmperage) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTEnergySourceBlockEntity tSource)) {
			aSource.sendFailure(Component.literal("No GTEnergySourceBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		tSource.setAmperage(aAmperage);
		String tLine = "GT6 energy source amp at " + aPos.toShortString() + ": amperage " + tSource.mAmperage + " A";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}
}
