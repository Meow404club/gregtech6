package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.tileentity.energy.GTCrankBlockEntity;

/**
 * {@code /gt6engine} — the engine-chain acceptance command home (task p12-engine-crank
 * spec ④). Game-bus listener, self-contained per ADR-P3-4, the GT6EnergyCommand template;
 * the later engine cards (steam/diesel/axle family) append their subcommands to this same
 * literal rather than growing new command roots.
 *
 * <ul>
 * <li>{@code stat <pos>} — the BE energy surface read: on the crank the RU emit direction
 *     (facing, name + byte) and amplitude (the signed packet — NEGATIVE =
 *     counterclockwise, the invariant the RCON chain asserts), on any other
 *     {@link ITileEntityEnergy} a generic emits/accepts per-side dump (the future engine
 *     cards' readback).</li>
 * <li>{@code crank <pos> <ticks>} — the server-driven crank window
 *     ({@link GTCrankBlockEntity#setDriveTicks}): the RCON acceptance channel, the
 *     DECLARED counterfactual of "a potionless player keeps cranking" — the player
 *     right-click ({@code GTCrankBlock.use}) stays the real-interaction path. ticks 0
 *     stops a running window (the chain's stop gate).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTEngineCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTEngineCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tEngine = Commands.literal("gt6engine")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("stat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("crank")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("ticks", IntegerArgumentType.integer(0, 1000000000))
						.executes(aContext -> crank(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								IntegerArgumentType.getInteger(aContext, "ticks"))))));
		aEvent.getDispatcher().register(tEngine);
		LOGGER.info("Registered GT6 engine-chain command /gt6engine (stat | crank) — the crank + future engine family acceptance home");
	}

	/** The BE energy-surface readback (spec ④ stat): crank detail, generic energy dump, failure otherwise. */
	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (tLevel.getBlockEntity(aPos) instanceof GTCrankBlockEntity tCrank) {
			String tFacing = Direction.from3DDataValue(tCrank.getFacing()).getName();
			String tLine = "GT6 crank at " + aPos.toShortString()
					+ ": facing=" + tFacing + "(" + tCrank.getFacing() + ") emit-side"
					+ ", active=" + tCrank.mActive
					+ ", drive=" + tCrank.mDriveTicks + " ticks"
					+ ", RU packet size=" + tCrank.mPacketSize + " (negative=counterclockwise DC)"
					+ ", amount=" + tCrank.mPacketAmount
					+ ", band " + GTCrankBlockEntity.OUTPUT_SIZE + "/" + GTCrankBlockEntity.OUTPUT_SIZE + "/" + GTCrankBlockEntity.OUTPUT_SIZE
					+ ", offered=" + tCrank.getEnergyOffered(TD.Energy.RU, (byte) tCrank.getFacing(), GTCrankBlockEntity.OUTPUT_SIZE);
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
			return Command.SINGLE_SUCCESS;
		}
		if (tLevel.getBlockEntity(aPos) instanceof ITileEntityEnergy tEnergy) {
			StringBuilder tEmits = new StringBuilder(), tAccepts = new StringBuilder();
			for (byte tSide = 0; tSide < 6; tSide++) {
				for (TagData tType : tEnergy.getEnergyTypes(tSide)) {
					if (tEnergy.isEnergyEmittingTo(tType, tSide, false)) tEmits.append(tType.mName).append('@').append(tSide).append(' ');
					if (tEnergy.isEnergyAcceptingFrom(tType, tSide, false)) tAccepts.append(tType.mName).append('@').append(tSide).append(' ');
				}
			}
			String tLine = "GT6 energy BE at " + aPos.toShortString()
					+ ": emits [" + tEmits.toString().trim() + "], accepts [" + tAccepts.toString().trim() + "]";
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
			return Command.SINGLE_SUCCESS;
		}
		aSource.sendFailure(Component.literal("STAT FAILED: no energy surface at " + aPos.toShortString()));
		return 0;
	}

	/** The RCON drive window (spec ④ crank): ticks > 0 arms, 0 stops (the chain's stop gate). */
	private static int crank(CommandSourceStack aSource, BlockPos aPos, int aTicks) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTCrankBlockEntity tCrank)) {
			aSource.sendFailure(Component.literal("CRANK FAILED: no hand crank BE at " + aPos.toShortString()));
			return 0;
		}
		tCrank.setDriveTicks(aTicks);
		String tLine = aTicks > 0
				? "GT6 crank drive at " + aPos.toShortString() + ": armed " + tCrank.mDriveTicks + " ticks"
						+ " (RU packet " + tCrank.mPacketSize + "x" + tCrank.mPacketAmount + " per tick)"
				: "GT6 crank drive at " + aPos.toShortString() + ": stopped (drive=" + tCrank.mDriveTicks + " ticks)";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}
}
