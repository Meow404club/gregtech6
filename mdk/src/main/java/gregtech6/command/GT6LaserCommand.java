package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import gregtech6.tileentity.energy.converters.GT6LaserConverterBlockEntity;

/**
 * {@code /gt6laser} — the laser-domain acceptance command (task p32-qu-laser-domain;
 * card-local in command/ like GT6ElectricCommand — the /gt6bridge stat|reset shape over
 * the laser/absorber converter pair):
 *
 * <ul>
 * <li>{@code stat <pos>} — the accounting pair snapshot: {@code in} (the consumed
 *     intake mass) / {@code out} (the emitted mass) / {@code capacitor} / {@code half}
 *     (in − capacitor == 2×out, the WASTE=T per-tick pairing — the units() half-rate,
 *     the RCON frequency face: the LU hop carries the packet SIZE the next rung's
 *     inMin door tunes on);</li>
 * <li>{@code reset <pos>} — zeroes the pair (the next stat reads exactly the window
 *     since the reset).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6LaserCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6LaserCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tLaser = Commands.literal("gt6laser")
				.requires(source -> source.hasPermission(2));
		tLaser.then(Commands.literal("stat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> laserStat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		tLaser.then(Commands.literal("reset")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> laserReset(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		aEvent.getDispatcher().register(tLaser);
		LOGGER.info("Registered GT6 laser acceptance command /gt6laser (stat|reset, the EU->LU / LU->EU converter live face, task p32-qu-laser-domain)");
	}

	private static int laserStat(CommandSourceStack aSource, BlockPos aPos) {
		if (aSource.getLevel().getBlockEntity(aPos) instanceof GT6LaserConverterBlockEntity tLaser) {
			// the boundary-tolerant half (the bridge stat form): the LAST intake packet may
			// sit un-converted in the capacitor at stat time — subtract it from the in side
			boolean tHalf = tLaser.mLastIn - tLaser.mStorage == tLaser.mLastOut * 2;
			aSource.sendSuccess(() -> Component.literal("GT6 laser " + shortType(tLaser.inputType()) + "->"
					+ shortType(tLaser.outputType()) + " at " + aPos.toShortString()
					+ ": in " + tLaser.mLastIn + ", out " + tLaser.mLastOut
					+ ", capacitor " + tLaser.mStorage + ", half " + tHalf), false);
			return Command.SINGLE_SUCCESS;
		}
		aSource.sendFailure(Component.literal("No GT6 laser converter at " + aPos.toShortString()));
		return 0;
	}

	private static int laserReset(CommandSourceStack aSource, BlockPos aPos) {
		if (aSource.getLevel().getBlockEntity(aPos) instanceof GT6LaserConverterBlockEntity tLaser) {
			tLaser.resetAccounting();
			aSource.sendSuccess(() -> Component.literal("GT6 laser accounting reset at " + aPos.toShortString()), false);
			return Command.SINGLE_SUCCESS;
		}
		aSource.sendFailure(Component.literal("No GT6 laser converter at " + aPos.toShortString()));
		return 0;
	}

	/** The short energy-type word (the bridge stat's shortType face). */
	private static String shortType(gregapi.code.TagData aType) {
		if (aType == gregapi.data.TD.Energy.EU) return "EU";
		if (aType == gregapi.data.TD.Energy.LU) return "LU";
		return aType.toString();
	}
}
