package gregtech6.tileentity.sensors;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

import gregtech6.registry.GT6Sensors;

/**
 * {@code /gt6sensor} — the sensor-family acceptance command (task p26-sensors-core, the
 * GTWireCommand template). The RCON channel for every tool arm whose ITEM is not ported
 * (the monkey-wrench second-face arm and the soft-hammer reset arm — the gearbox
 * monkey-wrench / card RCON stand-in rulings) and the read-out channel for the chain:
 *
 * <ul>
 * <li>{@code place <progressmeter|fluidometer|electrometer> <pos>} — setBlock a fresh
 *     sensor (FACING north, probe south);</li>
 * <li>{@code facing <pos> <side0-5>} — the wrench arm (GT6SensorBlockEntity#wrenchSetFacing,
 *     the display face + the OPOS probe fold);</li>
 * <li>{@code second <pos> <side0-5>} — the MONKEY-WRENCH stand-in
 *     ({@code monkeyWrenchSetSecondFacing}, upstream Sensor:103);</li>
 * <li>{@code mode <pos> <0-7>} — the screwdriver mode cycle target (the packed byte, hex
 *     bit preserved by the caller passing the base); {@code hex <pos> <0|1>} — the
 *     display-strip toggle; {@code window <pos> <n>} — the averaging-window resize;</li>
 * <li>{@code set <pos> <n>} — the KEYPAD stand-in: the raw setpoint write with the exact
 *     keypad clamps (hex → bind16, decimal → 0..9999);</li>
 * <li>{@code reset <pos>} — the SOFT-HAMMER stand-in (the full zeroing);</li>
 * <li>{@code read <pos>} — the acceptance read-out: mode/hex/setpoint/value/max/displayed/
 *     redstone/window/facing/probe.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTSensorCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTSensorCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		aEvent.getDispatcher().register(
			Commands.literal("gt6sensor")
				.requires(aSource -> aSource.hasPermission(2))
				.then(Commands.literal("place")
					.then(Commands.argument("sensor", StringArgumentType.word())
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(aContext -> place(aContext.getSource(), StringArgumentType.getString(aContext, "sensor"),
									BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))))
				.then(Commands.literal("facing")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("side", IntegerArgumentType.integer(0, 5))
							.executes(aContext -> facing(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									(byte) IntegerArgumentType.getInteger(aContext, "side"))))))
				.then(Commands.literal("second")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("side", IntegerArgumentType.integer(0, 5))
							.executes(aContext -> second(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									(byte) IntegerArgumentType.getInteger(aContext, "side"))))))
				.then(Commands.literal("mode")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("mode", IntegerArgumentType.integer(0, 7))
							.executes(aContext -> mode(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									IntegerArgumentType.getInteger(aContext, "mode"))))))
				.then(Commands.literal("hex")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("on", IntegerArgumentType.integer(0, 1))
							.executes(aContext -> hex(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									IntegerArgumentType.getInteger(aContext, "on") != 0)))))
				.then(Commands.literal("window")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("size", IntegerArgumentType.integer(1, GTSensorLogic.MAX_AVERAGING_VALUES))
							.executes(aContext -> window(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									IntegerArgumentType.getInteger(aContext, "size"))))))
				.then(Commands.literal("set")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("value", IntegerArgumentType.integer(0, 65535))
							.executes(aContext -> set(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									IntegerArgumentType.getInteger(aContext, "value"))))))
				.then(Commands.literal("reset")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> reset(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("read")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> read(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))));
		LOGGER.info("Registered GT6 sensor command /gt6sensor (place|facing|second|mode|hex|window|set|reset|read)");
	}

	private static int place(CommandSourceStack aSource, String aSensor, BlockPos aPos) {
		var tBlock = GT6Sensors.BLOCKS_BY_PATH.get(aSensor);
		if (tBlock == null) {
			aSource.sendFailure(Component.literal("PLACE FAILED: unknown sensor '" + aSensor + "' (rows: progressmeter, fluidometer, electrometer)"));
			return 0;
		}
		ServerLevel tLevel = aSource.getLevel();
		tLevel.setBlock(aPos, tBlock.get().defaultBlockState(), Block.UPDATE_ALL);
		if (!(tLevel.getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor)) {
			aSource.sendFailure(Component.literal("PLACE FAILED: no sensor BE at " + aPos.toShortString()));
			return 0;
		}
		tSensor.wrenchSetFacing((byte) 2); // FACING north + the probe folded south (the placement default)
		String tLine = "GT6 sensor placed at " + aPos.toShortString() + ": " + aSensor + ", facing 2 (north), probe 3 (south)";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int facing(CommandSourceStack aSource, BlockPos aPos, byte aSide) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor)) return noSensor(aSource, aPos);
		boolean tOk = tSensor.wrenchSetFacing(aSide);
		String tLine = "GT6 sensor facing at " + aPos.toShortString() + ": " + aSide + " " + (tOk ? "ok, probe " + tSensor.getSecondFacing() : "REFUSED");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return tOk ? Command.SINGLE_SUCCESS : 0;
	}

	private static int second(CommandSourceStack aSource, BlockPos aPos, byte aSide) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor)) return noSensor(aSource, aPos);
		boolean tOk = tSensor.monkeyWrenchSetSecondFacing(aSide);
		String tLine = "GT6 sensor probe at " + aPos.toShortString() + ": " + aSide + " " + (tOk ? "ok (monkey-wrench arm)" : "REFUSED");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return tOk ? Command.SINGLE_SUCCESS : 0;
	}

	private static int mode(CommandSourceStack aSource, BlockPos aPos, int aMode) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor)) return noSensor(aSource, aPos);
		// same-package write: the packed byte with the hex bit preserved (the cycle ring
		// would take 8 clicks — the command pins the index the chain asserts).
		tSensor.mMode = (tSensor.mMode & ~127) | aMode;
		tSensor.updateClientData();
		String tLine = "GT6 sensor mode at " + aPos.toShortString() + ": " + aMode + " (hex " + GTSensorLogic.isHexMode(tSensor.mMode) + "), redstone " + tSensor.getRedstone();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int hex(CommandSourceStack aSource, BlockPos aPos, boolean aOn) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor)) return noSensor(aSource, aPos);
		if (GTSensorLogic.isHexMode(tSensor.getMode()) != aOn) tSensor.screwdriverToggleHex();
		String tLine = "GT6 sensor hex at " + aPos.toShortString() + ": " + aOn + " (mode byte " + tSensor.getMode() + ")";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int window(CommandSourceStack aSource, BlockPos aPos, int aSize) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor)) return noSensor(aSource, aPos);
		while (tSensor.getAveragingLength() != aSize) {
			if (!tSensor.screwdriverResizeAveraging(2, 1)) break; // the +1 column until the size lands
		}
		if (tSensor.getAveragingLength() > aSize) {
			while (tSensor.getAveragingLength() > aSize) {
				if (!tSensor.screwdriverResizeAveraging(2, 0)) break; // the -1 column
			}
		}
		String tLine = "GT6 sensor window at " + aPos.toShortString() + ": " + tSensor.getAveragingLength();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int set(CommandSourceStack aSource, BlockPos aPos, int aValue) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor)) return noSensor(aSource, aPos);
		// the KEYPAD stand-in: the write IS the keypad-arithmetic outcome for the same
		// input (hex → the full bind16 range, decimal → the 0..9999 clamp), not a bypass.
		int tClamped = GTSensorLogic.isHexMode(tSensor.getMode()) ? GTSensorLogic.bind16(aValue) : (int) GTSensorLogic.bind(0, 9999, aValue);
		tSensor.mSetNumber = tClamped;
		tSensor.oDisplayedNumber = Short.MIN_VALUE; // the forced-sync arm (upstream :171-193)
		tSensor.updateClientData();
		String tLine = "GT6 sensor set at " + aPos.toShortString() + ": " + tClamped + " (clamped " + (GTSensorLogic.isHexMode(tSensor.getMode()) ? "bind16" : "0..9999") + ")";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int reset(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor)) return noSensor(aSource, aPos);
		tSensor.softHammerReset();
		String tLine = "GT6 sensor reset at " + aPos.toShortString() + " (soft-hammer arm): mode " + tSensor.getMode() + ", redstone " + tSensor.getRedstone() + ", window " + tSensor.getAveragingLength();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int read(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor)) return noSensor(aSource, aPos);
		String tLine = "GT6 sensor read at " + aPos.toShortString() + ": mode " + (tSensor.getMode() & 127) + (GTSensorLogic.isHexMode(tSensor.getMode()) ? " hex" : " dec")
				+ ", set " + tSensor.getSetNumber() + ", value " + tSensor.getCurrentValue() + ", max " + tSensor.getCurrentMax()
				+ ", displayed " + tSensor.getDisplayedNumber() + ", redstone " + tSensor.getRedstone()
				+ ", window " + tSensor.getAveragingLength() + ", facing " + tSensor.getFacing() + ", probe " + tSensor.getSecondFacing();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int noSensor(CommandSourceStack aSource, BlockPos aPos) {
		aSource.sendFailure(Component.literal("No GTSensorBlockEntity at " + aPos.toShortString()));
		return 0;
	}
}
