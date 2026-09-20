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

import gregtech6.tileentity.energy.generators.GT6MagicAbsorberBlockEntity;

/**
 * {@code /gt6magicabsorber} — the Magic Field Absorber acceptance command (task
 * p32-magic-absorber; the /gt6laser stat|reset shape over the absorber's probe face):
 *
 * <ul>
 * <li>{@code stat <pos>} — the live state snapshot: {@code active} (the :82-97 trophy
 *     probe verdict) / {@code type} (the emitted type word) / {@code out} (the per-tick
 *     packet budget) / {@code facing} (the output face) / {@code delivered} (the
 *     cumulative ACCEPTED mass since the last reset — the in-network meter, the
 *     white-burn-door lesson: the Root gate counts a burned offer as used) /
 *     {@code rate} (delivered/window-ticks — the laser stat's "half true" ratio form,
 *     exact at 64 with the egg or 1 with a skull while the trophy emits every tick,
 *     tick-rate-independent for the RCON judge);</li>
 * <li>{@code reset <pos>} — zeroes the delivered meter (the next stat reads exactly the
 *     window since the reset).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6MagicAbsorberCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6MagicAbsorberCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tAbsorber = Commands.literal("gt6magicabsorber")
				.requires(source -> source.hasPermission(2));
		tAbsorber.then(Commands.literal("stat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> absorberStat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		tAbsorber.then(Commands.literal("reset")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> absorberReset(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		aEvent.getDispatcher().register(tAbsorber);
		LOGGER.info("Registered GT6 magic absorber acceptance command /gt6magicabsorber (stat|reset, task p32-magic-absorber)");
	}

	private static int absorberStat(CommandSourceStack aSource, BlockPos aPos) {
		if (aSource.getLevel().getBlockEntity(aPos) instanceof GT6MagicAbsorberBlockEntity tAbsorber) {
			// rate = delivered/ticks (the laser stat's "half true" ratio form — exact while the
			// trophy emits every window tick, tick-rate-independent for the RCON judge)
			aSource.sendSuccess(() -> Component.literal("GT6 magic absorber at " + aPos.toShortString()
					+ ": active " + tAbsorber.mActive
					+ ", type " + GT6MagicAbsorberBlockEntity.shortType(tAbsorber.mEnergyTypeEmitted)
					+ ", out " + tAbsorber.mOutput
					+ ", facing " + tAbsorber.mFacing
					+ ", delivered " + tAbsorber.mLastOut
					+ ", rate " + tAbsorber.meterRate()), false);
			return Command.SINGLE_SUCCESS;
		}
		aSource.sendFailure(Component.literal("No GT6 magic absorber at " + aPos.toShortString()));
		return 0;
	}

	private static int absorberReset(CommandSourceStack aSource, BlockPos aPos) {
		if (aSource.getLevel().getBlockEntity(aPos) instanceof GT6MagicAbsorberBlockEntity tAbsorber) {
			tAbsorber.resetAccounting();
			aSource.sendSuccess(() -> Component.literal("GT6 magic absorber accounting reset at " + aPos.toShortString()), false);
			return Command.SINGLE_SUCCESS;
		}
		aSource.sendFailure(Component.literal("No GT6 magic absorber at " + aPos.toShortString()));
		return 0;
	}
}
