package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

import gregapi.data.TD;
import gregtech6.registry.GTWires;
import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * {@code /gt6wire} — the automated electric-wire acceptance command (task p7-d2-cable
 * spec ⑦, ADR 2026-08-31-p7-energy-network ruling 7). Game-bus listener, self-contained
 * per ADR-P3-4, the GTFluidPipeCommand template.
 *
 * <ul>
 * <li>{@code place <1x|2x> <pos>} — the headless placement driver: setBlock the wire
 *     variant at pos, then the automatic neighbour-scan connect (card wording): every
 *     side runs the {@link GTWireBlockEntity#connect(byte, boolean)} handshake, which
 *     gates itself on the connector-type intersection for wire neighbours
 *     (WIRE_ELECTRIC), the {@code canConnect} energy-acceptor probe for machines and the
 *     open-end air/liquid connect otherwise (upstream :141). This is the wire-form of the
 *     card-mandated auto connect — the BlockItem placement chain stays on the upstream
 *     support-face onPlaced.</li>
 * <li>{@code connect <pos> <side>} — the explicit one-side handshake (upstream
 *     onToolClick2 :70-79 form).</li>
 * <li>{@code neighbors <pos>} — the six-side neighbour census (BE class, block,
 *     connected bit) for chain debugging.</li>
 * <li>{@code inject <pos> <side> <size> <amount>} — direct
 *     {@link GTWireBlockEntity#doEnergyInjection} with aDoInject=true (the real push);
 *     reports the used amperage (0 = nothing flowed — the upstream :188 no-consumer
 *     semantics the card pins for the burn assertions).</li>
 * <li>{@code stat <pos>} — dump voltage/amperage/loss/burnCounter/wattageLast/
 *     transferredAmperes/connections/mTimer.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTWireCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTWireCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		aEvent.getDispatcher().register(
			Commands.literal("gt6wire")
				.requires(aSource -> aSource.hasPermission(2))
				.then(Commands.literal("place")
					.then(Commands.argument("tier", StringArgumentType.word())
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(aContext -> place(aContext.getSource(), StringArgumentType.getString(aContext, "tier"),
									BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))))
				.then(Commands.literal("connect")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("side", IntegerArgumentType.integer(0, 5))
							.executes(aContext -> connect(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									(byte)IntegerArgumentType.getInteger(aContext, "side"))))))
				.then(Commands.literal("neighbors")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> neighbors(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("inject")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("side", IntegerArgumentType.integer(0, 5))
							.then(Commands.argument("size", IntegerArgumentType.integer(1, 1000000))
								.then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000))
									.executes(aContext -> inject(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
											(byte)IntegerArgumentType.getInteger(aContext, "side"),
											IntegerArgumentType.getInteger(aContext, "size"),
											IntegerArgumentType.getInteger(aContext, "amount"))))))))
				.then(Commands.literal("stat")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))));
		LOGGER.info("Registered GT6 electric wire command /gt6wire (place|connect|neighbors|inject|stat)");
	}

	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTWireBlockEntity aWire)) {
			aSource.sendFailure(Component.literal("No GTWireBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		String tLine = "GT6 wire stat at " + aPos.toShortString() + ": voltage " + aWire.mVoltage + " EU, amperage "
				+ aWire.mAmperage + " A, loss " + aWire.mLoss + " EU, burnCounter " + aWire.mBurnCounter
				+ ", wattageLast " + aWire.mWattageLast + " EU/t, transferredAmperes " + aWire.mTransferredAmperes
				+ ", transferredWattage " + aWire.mTransferredWattage + ", connections " + aWire.getConnections()
				+ ", timer " + aWire.getTimer();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The headless placement driver (spec ⑦): setBlock, then the automatic neighbour-scan connect. */
	private static int place(CommandSourceStack aSource, String aTier, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		var tBlock = switch (aTier) {
			case "1x" -> GTWires.WIRE_ELECTRIC_1X.get();
			case "2x" -> GTWires.WIRE_ELECTRIC_2X.get();
			default -> null;
		};
		if (tBlock == null) {
			aSource.sendFailure(Component.literal("PLACE FAILED: unknown wire tier '" + aTier + "' (use 1x or 2x)"));
			return 0;
		}
		tLevel.setBlock(aPos, tBlock.defaultBlockState(), Block.UPDATE_ALL);
		if (!(tLevel.getBlockEntity(aPos) instanceof GTWireBlockEntity tWire)) {
			aSource.sendFailure(Component.literal("PLACE FAILED: no wire BE at " + aPos.toShortString()));
			return 0;
		}
		int tConnected = 0;
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (tWire.connect(tSide, true)) tConnected++;
		}
		String tLine = "GT6 wire placed at " + aPos.toShortString() + ": tier " + aTier + " (" + tWire.mVoltage + " EU/"
				+ tWire.mAmperage + " A/" + tWire.mLoss + " loss), connected sides " + tConnected
				+ ", connections " + tWire.getConnections();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The explicit one-side handshake (the hoe-right-click equivalent for wires). */
	private static int connect(CommandSourceStack aSource, BlockPos aPos, byte aSide) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTWireBlockEntity tWire)) {
			aSource.sendFailure(Component.literal("No GTWireBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		boolean tResult = tWire.connect(aSide, true);
		String tLine = "GT6 wire connect at " + aPos.toShortString() + " side " + aSide + ": "
				+ (tResult ? "ok" : "FAILED") + ", connections " + tWire.getConnections();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return tResult ? Command.SINGLE_SUCCESS : 0;
	}

	/** The six-side neighbour census (spec ⑦ — chain debugging). */
	private static int neighbors(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTWireBlockEntity tWire)) {
			aSource.sendFailure(Component.literal("No GTWireBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		StringBuilder tCensus = new StringBuilder();
		for (byte tSide = 0; tSide < 6; tSide++) {
			BlockPos tTarget = aPos.relative(Direction.from3DDataValue(tSide));
			BlockEntity tNeighbor = tLevel.getBlockEntity(tTarget);
			tCensus.append(Direction.from3DDataValue(tSide)).append('=')
					.append(tNeighbor == null ? "-" : tNeighbor.getClass().getSimpleName())
					.append(tWire.connected(tSide) ? "(connected)" : "")
					.append(' ');
		}
		String tLine = "GT6 wire neighbors at " + aPos.toShortString() + ": " + tCensus + "connections "
				+ tWire.getConnections();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The headless energy push (spec ⑦): the real doEnergyInjection with aDoInject=true.
	 * The used-amperage return is the acceptance observable: 0 = nothing flowed (no
	 * consumer, the upstream :188 semantics) and nothing is booked.
	 */
	private static int inject(CommandSourceStack aSource, BlockPos aPos, byte aSide, long aSize, long aAmount) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTWireBlockEntity tWire)) {
			aSource.sendFailure(Component.literal("No GTWireBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		long tUsed = tWire.doEnergyInjection(TD.Energy.EU, aSide, aSize, aAmount, true);
		String tLine = "GT6 wire inject at " + aPos.toShortString() + " side " + aSide + ": size " + aSize + " EU, "
				+ aAmount + " A, used " + tUsed + (tUsed == 0 ? " (NOTHING FLOWED)" : "");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}
}
