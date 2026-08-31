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
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWires;
import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * {@code /gt6wire} — the automated electric-wire acceptance command (task p7-d2-cable
 * spec ⑦, ADR 2026-08-31-p7-energy-network ruling 7). Game-bus listener, self-contained
 * per ADR-P3-4, the GTFluidPipeCommand template.
 *
 * <ul>
 * <li>{@code place <1x|2x> <pos>} — the p7 legacy tier driver (the P8 RCON chain and the
 *     gen→wire→oven e2e regression keep driving it), and since task p9-wire-family-w1 also
 *     the full registry-path form {@code place wire_sn_gt04 <pos>} / {@code place cable_w_gt08 <pos>};</li>
 * <li>{@code place <material> <size> <pos>} and {@code place <material> <size> cable <pos>}
 *     — the 620-block selector (spec ③): material token = the snake-cased row token from
 *     {@link GTWireSpecs} ({@code sn}, {@code annealed_copper}, {@code si_c}, ...), size 1..16
 *     (cables 1/2/4/8/12), {@code cable} = the insulated form. Resolves through
 *     {@link GTWireSpecs#find} + {@link GTWires#FAMILY_BY_NAME};</li>
 * <li>every place form then runs the automatic neighbour-scan connect (card wording): every
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
					.then(Commands.argument("spec", StringArgumentType.word())
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(aContext -> place(aContext.getSource(), StringArgumentType.getString(aContext, "spec"),
									BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))
						.then(Commands.argument("size", IntegerArgumentType.integer(1, 16))
							.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> placeFamily(aContext.getSource(),
										StringArgumentType.getString(aContext, "spec"),
										IntegerArgumentType.getInteger(aContext, "size"), false,
										BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))
							.then(Commands.literal("cable")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
									.executes(aContext -> placeFamily(aContext.getSource(),
											StringArgumentType.getString(aContext, "spec"),
											IntegerArgumentType.getInteger(aContext, "size"), true,
											BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))))))
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

	/**
	 * The headless placement driver (spec ⑦): setBlock, then the automatic neighbour-scan
	 * connect. The spec is the p7 legacy tier ("1x"/"2x") or a family registry path
	 * ("wire_sn_gt04" / "cable_w_gt08", the p9-wire-family-w1 selector).
	 */
	private static int place(CommandSourceStack aSource, String aSpec, BlockPos aPos) {
		var tBlock = switch (aSpec) {
			case "1x" -> GTWires.WIRE_ELECTRIC_1X.get();
			case "2x" -> GTWires.WIRE_ELECTRIC_2X.get();
			default -> GTWires.FAMILY_BY_NAME.containsKey(aSpec) ? GTWires.FAMILY_BY_NAME.get(aSpec).get() : null;
		};
		if (tBlock == null) {
			aSource.sendFailure(Component.literal("PLACE FAILED: unknown wire spec '" + aSpec
					+ "' (use 1x, 2x, a registry path like wire_sn_gt04, or <material> <size> [cable] <pos>)"));
			return 0;
		}
		return placeWire(aSource, tBlock, aSpec, aPos);
	}

	/**
	 * The 620-block family selector (task p9-wire-family-w1 spec ③): material token + size
	 * (+ optional "cable" literal resolved upstream in the brigadier tree) — resolved through
	 * the GTWireSpecs table and the GTWires family index.
	 */
	private static int placeFamily(CommandSourceStack aSource, String aMaterial, long aSize, boolean aInsulated, BlockPos aPos) {
		GTWireSpecs.Variant tVariant = GTWireSpecs.find(aMaterial, (int)aSize, aInsulated);
		if (tVariant == null) {
			aSource.sendFailure(Component.literal("PLACE FAILED: no wire variant for material '" + aMaterial
					+ "', size " + aSize + (aInsulated ? " (cable)" : "") + " — sizes: wire 1..16, cable 1/2/4/8/12"));
			return 0;
		}
		String tName = GTWireSpecs.registryName(tVariant);
		var tRegistryObject = GTWires.FAMILY_BY_NAME.get(tName);
		if (tRegistryObject == null) {
			aSource.sendFailure(Component.literal("PLACE FAILED: variant " + tName + " is not registered"));
			return 0;
		}
		return placeWire(aSource, tRegistryObject.get(), tName, aPos);
	}

	/** The common placement tail: setBlock + the automatic neighbour-scan connect + the report line. */
	private static int placeWire(CommandSourceStack aSource, Block aBlock, String aLabel, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		tLevel.setBlock(aPos, aBlock.defaultBlockState(), Block.UPDATE_ALL);
		if (!(tLevel.getBlockEntity(aPos) instanceof GTWireBlockEntity tWire)) {
			aSource.sendFailure(Component.literal("PLACE FAILED: no wire BE at " + aPos.toShortString()));
			return 0;
		}
		int tConnected = 0;
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (tWire.connect(tSide, true)) tConnected++;
		}
		String tLine = "GT6 wire placed at " + aPos.toShortString() + ": tier " + aLabel + " (" + tWire.mVoltage + " EU/"
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
