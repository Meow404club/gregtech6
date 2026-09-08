package gregtech6.tileentity.connectors;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.slf4j.Logger;

import gregtech6.fluid.FluidTankGT;
import gregtech6.registry.GTFluidPipes;

/**
 * {@code /gt6pipe} — the automated fluid-pipe acceptance command (task p4-fluid-pipes
 * acceptance ②, extended by task p4-pipe-flow-control spec ⑥). Game-bus listener,
 * self-contained per ADR-P3-4.
 *
 * <ul>
 * <li>{@code accept <pos>} (console-safe) — the full card scenario against the pipe at
 *     pos (A) and its first neighbouring pipe (B): the explicit symmetric connect
 *     handshake (there is NO auto-connect since p4-pipe-flow-control — the command must
 *     drive it exactly like the player's hoe right click does), then the explicit
 *     open-end connect of the injection side (connect into air always succeeds — the
 *     upstream :141 open-end semantics, the "manual pipe mouth"), inject 100 L of water
 *     through it (the upstream external fill, which records the source bit :487), drive
 *     {@link TileEntityBase03TicksAndSync#updateEntity} passes on A alone (its distribute
 *     rounds push into B, the divup split :415/:417 equalising both tanks), then assert
 *     equalisation (50/50), the anti-backflow marker on B toward A (:389) with A's own
 *     marker toward B still clear (no ping-pong), and the injection-side marker on A.
 *     Two further passes on B prove the equilibrium is stable (B skips the masked side
 *     :378 and clears its mask :331).</li>
 * <li>{@code stat <pos>} — dump capacity/content/connections/ioMask/received-masks
 *     (p24 adds the ownable/owner pair).</li>
 * <li>{@code place <pos> <againstFace>} — the headless placement driver: setBlock a
 *     wood small pipe at pos and call {@link GTFluidPipeBlockEntity#onPlaced(byte, UUID)}
 *     with the given CLICKED face (0..5 — the face of the neighbour that was "clicked";
 *     the neighbour must sit at pos.relative(OPOS[face])). The BlockItem.placeBlock chain
 *     equivalent for /setblock, which never reaches the placement hook. Console = null
 *     identity (a locked support pipe denies the connect, upstream 09Connector:86).</li>
 * <li>{@code toggle <pos> <side>} — the hoe right-click connection toggle
 *     ({@link GTFluidPipeBlockEntity#toggleConnection(byte, UUID)}, upstream onToolClick2
 *     :70-79). Console = null identity: a locked pipe rejects (the RCON verify arm,
 *     upstream 06Covers:141 counterpart) and a locked neighbour rejects the target side
 *     (upstream :75).</li>
 * <li>{@code output <pos> <side>} — the shift-right-click arrow toggle
 *     ({@link GTFluidPipeBlockEntity#toggleOutput(byte)}); {@code clear <pos>} drops
 *     every arrow ({@link GTFluidPipeBlockEntity#clearOutputs()}).</li>
 * <li>{@code ownable <pos> <0|1> [ownerUuid]} — the foam applyFoam stand-in (task
 *     p24-pipe-owner, upstream 10ConnectorRendered:159-166): the only live forced write
 *     point while the foam card sleeps in the P10 pool. 1 records ownable (with the
 *     owner UUID when given, null owner = everyone passes — the upstream :107 arm);
 *     0 resets both fields (the removeFoam :177-183 reset form). Console OP force write —
 *     the /setblock-style seam the card accepts.</li>
 * <li>{@code spray <pos> <owned> [dye] [ownerUuid]} / {@code dry <pos>} /
 *     {@code removefoam <pos> [ownerUuid]} — the foam trio (task p25-c-foam-pipe-spray
 *     spec ⑧): the SAME GATED BE method faces the item's useOn path calls —
 *     {@link GTFluidPipeBlockEntity#applyFoam} (upstream 10ConnectorRendered:159-166),
 *     {@link GTFluidPipeBlockEntity#dryFoam} (:169-174, the no-gate asymmetry) and
 *     {@link GTFluidPipeBlockEntity#removeFoam} (:177-183). {@code stat} gains the
 *     {@code foam/dried/foamOwned} triple.</li>
 * </ul>
 *
 * <p>The command runs inside one server tick, so the manual dispatcher passes are not
 * interleaved with the live tickers — the assertions are deterministic (a real tick of B
 * cannot clear the mask between the push and the assertion). The toggle/output/place
 * entries share the BE methods the interactive {@code use} path calls, so the command
 * acceptance exercises the real chain.
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTFluidPipeCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Injection volume; 100 L splits 50/50 into two equal tanks (divup(100,2)=50). */
	private static final long INJECTED = 100;

	private GTFluidPipeCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		aEvent.getDispatcher().register(
			Commands.literal("gt6pipe")
				.requires(aSource -> aSource.hasPermission(2))
				.then(Commands.literal("accept")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> accept(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("stat")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("place")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("againstFace", IntegerArgumentType.integer(0, 5))
							.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									(byte)IntegerArgumentType.getInteger(aContext, "againstFace"))))))
				.then(Commands.literal("toggle")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("side", IntegerArgumentType.integer(0, 5))
							.executes(aContext -> toggle(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									(byte)IntegerArgumentType.getInteger(aContext, "side"))))))
				.then(Commands.literal("output")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("side", IntegerArgumentType.integer(0, 5))
							.executes(aContext -> output(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									(byte)IntegerArgumentType.getInteger(aContext, "side"))))))
				.then(Commands.literal("clear")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> clear(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("inject")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("side", IntegerArgumentType.integer(0, 5))
							.then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000))
								.executes(aContext -> inject(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									(byte)IntegerArgumentType.getInteger(aContext, "side"), IntegerArgumentType.getInteger(aContext, "amount")))))))
					.then(Commands.literal("ownable")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.then(Commands.argument("value", IntegerArgumentType.integer(0, 1))
								.executes(aContext -> ownable(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
										IntegerArgumentType.getInteger(aContext, "value") != 0, null))
									.then(Commands.argument("owner", StringArgumentType.word())
										.executes(aContext -> ownableArg(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
												IntegerArgumentType.getInteger(aContext, "value") != 0,
												StringArgumentType.getString(aContext, "owner")))))))
				// task p25-c-foam-pipe-spray spec ⑧ — the foam trio over the SAME BE method
				// faces the item's useOn path calls (GATED faces, unlike the ownable stand-in)
				.then(Commands.literal("spray")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("owned", IntegerArgumentType.integer(0, 1))
							.executes(aContext -> spray(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									IntegerArgumentType.getInteger(aContext, "owned") != 0, (byte)0, null))
								.then(Commands.argument("dye", IntegerArgumentType.integer(0, 15))
									.executes(aContext -> spray(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
											IntegerArgumentType.getInteger(aContext, "owned") != 0,
											(byte)IntegerArgumentType.getInteger(aContext, "dye"), null))
										.then(Commands.argument("owner", StringArgumentType.word())
											.executes(aContext -> sprayArg(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
													IntegerArgumentType.getInteger(aContext, "owned") != 0,
													(byte)IntegerArgumentType.getInteger(aContext, "dye"),
													StringArgumentType.getString(aContext, "owner"))))))))
				.then(Commands.literal("dry")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> dry(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("removefoam")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> removefoam(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), null))
							.then(Commands.argument("owner", StringArgumentType.word())
								.executes(aContext -> removefoamArg(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
										StringArgumentType.getString(aContext, "owner")))))));
		LOGGER.info("Registered GT6 fluid pipe command /gt6pipe (accept|stat|place|toggle|output|clear|inject|ownable|spray|dry|removefoam)");
	}

	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTFluidPipeBlockEntity aPipe)) {
			aSource.sendFailure(Component.literal("No GTFluidPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		StringBuilder tTanks = new StringBuilder();
		for (FluidTankGT tTank : aPipe.mTanks) {
			tTanks.append("[tank ").append(tTank.mIndex).append(": ").append(tTank.amount()).append('/').append(tTank.capacity())
					.append(" L, received-from mask ").append(aPipe.mLastReceivedFrom[tTank.mIndex]).append("] ");
		}
		String tLine = "GT6 pipe stat at " + aPos.toShortString() + ": connections " + aPipe.getConnections()
				+ " ioMask " + aPipe.getIoMask() + " ownable " + aPipe.mOwnable
				+ " owner " + (aPipe.mOwner != null ? aPipe.mOwner : "none")
				// task p25-c-foam-pipe-spray — the foam triple rides the stat line
				+ " foam " + aPipe.mFoam + " dried " + aPipe.mFoamDried + " foamOwned " + aPipe.ownedFoam((byte)0)
				+ " " + tTanks;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The headless placement driver (spec ⑥): setBlock the wood small pipe at aPos, then
	 * the explicit onPlaced with the clicked face — the /setblock seam the card accepts.
	 */
	private static int place(CommandSourceStack aSource, BlockPos aPos, byte aAgainstFace) {
		ServerLevel tLevel = aSource.getLevel();
		tLevel.setBlock(aPos, GTFluidPipes.WOOD_FLUID_PIPE_SMALL.get().defaultBlockState(), Block.UPDATE_ALL);
		if (!(tLevel.getBlockEntity(aPos) instanceof GTFluidPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("PLACE FAILED: no pipe BE at " + aPos.toShortString()));
			return 0;
		}
		// console = null identity: a locked support-side neighbour denies the first connect
		// (upstream 09Connector:86) — the p24 placement-gate arm drives exactly this
		tPipe.onPlaced(aAgainstFace, null);
		String tLine = "GT6 pipe placed at " + aPos.toShortString() + " against face " + aAgainstFace
				+ " (support side " + gregtech6.util.UT6.OPOS[aAgainstFace] + "): connections " + tPipe.getConnections();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The hoe-right-click equivalent (spec ⑥ — the shared BE entry the use path calls). */
	private static int toggle(CommandSourceStack aSource, BlockPos aPos, byte aSide) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTFluidPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTFluidPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		// console = null identity (nobody): a locked pipe rejects the toggle itself and a
		// locked neighbour rejects the target side — the p24 RCON verify arms drive this
		boolean tResult = tPipe.toggleConnection(aSide, null);
		String tLine = "GT6 pipe toggle at " + aPos.toShortString() + " side " + aSide + ": "
				+ (tResult ? "ok" : "FAILED") + ", connections " + tPipe.getConnections();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return tResult ? Command.SINGLE_SUCCESS : 0;
	}

	/** The shift-right-click equivalent (spec ⑥ — the shared BE entry the use path calls). */
	private static int output(CommandSourceStack aSource, BlockPos aPos, byte aSide) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTFluidPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTFluidPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		tPipe.toggleOutput(aSide);
		String tLine = "GT6 pipe output at " + aPos.toShortString() + " side " + aSide + ": ioMask " + tPipe.getIoMask()
				+ (tPipe.isOutputFace(aSide) ? " (side " + aSide + " marked)" : " (side " + aSide + " cleared)");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** Drops every output arrow (spec ⑥). */
	private static int clear(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTFluidPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTFluidPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		tPipe.clearOutputs();
		String tLine = "GT6 pipe clear at " + aPos.toShortString() + ": ioMask " + tPipe.getIoMask();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The headless external-fill driver: pushes {@code amount} L of water through the
	 * pipe's side handler ({@link SideFluidHandler#fill} — the real external entry, the
	 * upstream :480-490 fill path). Makes the acceptance-side "fill=0" observable: a
	 * toggled-off (unconnected) side rejects the fill with 0.
	 */
	private static int inject(CommandSourceStack aSource, BlockPos aPos, byte aSide, int aAmount) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTFluidPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTFluidPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		int tFilled = new SideFluidHandler(tPipe, aSide).fill(new FluidStack(Fluids.WATER, aAmount), FluidAction.EXECUTE);
		String tLine = "GT6 pipe inject at " + aPos.toShortString() + " side " + aSide + ": filled " + tFilled
				+ " of " + aAmount + " L" + (tFilled == 0 ? " (REJECTED)" : "");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The foam applyFoam stand-in (task p24-pipe-owner — upstream 10ConnectorRendered:159-166
	 * is the only runtime owner write point, and the foam family sleeps in the P10 pool):
	 * a FORCED console write, no allowInteraction gate (the console OP is the accepted
	 * /setblock-style seam). {@code value=0} resets both fields — the removeFoam :177-183
	 * reset form; {@code value=1} with a null owner keeps everyone-pass (the upstream
	 * 03TicksAndSync:107 {@code mOwner == null} arm, ownable persisted).
	 */
	private static int ownable(CommandSourceStack aSource, BlockPos aPos, boolean aValue, @Nullable UUID aOwner) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTFluidPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTFluidPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		tPipe.mOwnable = aValue;
		tPipe.mOwner = aValue ? aOwner : null;
		tPipe.setChanged();
		tPipe.updateClientData(); // the two sync channels carry the pair on the next window
		String tLine = "GT6 pipe ownable at " + aPos.toShortString() + ": ownable " + tPipe.mOwnable
				+ ", owner " + (tPipe.mOwner != null ? tPipe.mOwner : "none")
				+ (aValue ? " (FORCED write — the applyFoam stand-in)" : " (reset, the removeFoam form)");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The {@code [ownerUuid]} arm — a malformed word is a command failure, not a crash. */
	private static int ownableArg(CommandSourceStack aSource, BlockPos aPos, boolean aValue, String aOwner) {
		try {
			return ownable(aSource, aPos, aValue, UUID.fromString(aOwner));
		} catch (IllegalArgumentException tException) {
			aSource.sendFailure(Component.literal("Not a UUID: " + aOwner));
			return 0;
		}
	}

	/**
	 * The foam spray driver (task p25-c-foam-pipe-spray spec ⑧) — the SAME gated
	 * {@link GTFluidPipeBlockEntity#applyFoam} face the item's useOn calls (upstream
	 * 10ConnectorRendered:159-166; Behavior_Spray_Foam.java:113-114 arm (1)), so the RCON
	 * chain exercises the real gates: a wet/dried pipe rejects, a locked pipe rejects a
	 * non-owner. The colour is the DYES_INT dye-index slot (default 0 = black).
	 */
	private static int spray(CommandSourceStack aSource, BlockPos aPos, boolean aOwned, byte aDye, @Nullable UUID aSprayer) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTFluidPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTFluidPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		int tRGB = gregtech6.item.spraycan.GTSprayCanItem.DYES_INT[aDye & 15];
		boolean tApplied = tPipe.applyFoam((byte)2, aSprayer, tRGB, aOwned);
		String tLine = "GT6 pipe spray at " + aPos.toShortString() + ": "
				+ (tApplied ? "foam applied" : "REJECTED") + ", foam " + tPipe.mFoam + " dried " + tPipe.mFoamDried
				+ " foamOwned " + tPipe.ownedFoam((byte)0) + " rgb " + String.format("%06x", tPipe.getPaint());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return tApplied ? Command.SINGLE_SUCCESS : 0;
	}

	/** The {@code [owner]} arm of spray — a malformed word is a command failure. */
	private static int sprayArg(CommandSourceStack aSource, BlockPos aPos, boolean aOwned, byte aDye, String aSprayer) {
		try {
			return spray(aSource, aPos, aOwned, aDye, UUID.fromString(aSprayer));
		} catch (IllegalArgumentException tException) {
			aSource.sendFailure(Component.literal("Not a UUID: " + aSprayer));
			return 0;
		}
	}

	/**
	 * The dry driver — the SAME no-gate {@link GTFluidPipeBlockEntity#dryFoam} face
	 * (upstream :169-174; the Hardening-Spray pool row's live seam). Console = null identity.
	 */
	private static int dry(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTFluidPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTFluidPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		boolean tDried = tPipe.dryFoam((byte)2, null);
		String tLine = "GT6 pipe dry at " + aPos.toShortString() + ": "
				+ (tDried ? "ok" : "FAILED") + ", foam " + tPipe.mFoam + " dried " + tPipe.mFoamDried;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return tDried ? Command.SINGLE_SUCCESS : 0;
	}

	/**
	 * The foam-removal driver — the SAME gated {@link GTFluidPipeBlockEntity#removeFoam}
	 * face (upstream :177-183): only a DRIED foam removes, only through allowInteraction
	 * (console = null identity = non-owner on a locked pipe, the RCON verify arm).
	 */
	private static int removefoam(CommandSourceStack aSource, BlockPos aPos, @Nullable UUID aRemover) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTFluidPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTFluidPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		boolean tRemoved = tPipe.removeFoam((byte)2, aRemover);
		String tLine = "GT6 pipe removefoam at " + aPos.toShortString() + ": "
				+ (tRemoved ? "ok" : "REJECTED") + ", foam " + tPipe.mFoam + " dried " + tPipe.mFoamDried
				+ " ownable " + tPipe.mOwnable + " owner " + (tPipe.mOwner != null ? tPipe.mOwner : "none");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return tRemoved ? Command.SINGLE_SUCCESS : 0;
	}

	/** The {@code [owner]} arm of removefoam — a malformed word is a command failure. */
	private static int removefoamArg(CommandSourceStack aSource, BlockPos aPos, String aRemover) {
		try {
			return removefoam(aSource, aPos, UUID.fromString(aRemover));
		} catch (IllegalArgumentException tException) {
			aSource.sendFailure(Component.literal("Not a UUID: " + aRemover));
			return 0;
		}
	}

	/** The acceptance scenario; every failure names the broken invariant. */
	private static int accept(CommandSourceStack aSource, BlockPos aPos) throws CommandSyntaxException {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTFluidPipeBlockEntity tPipeA)) {
			aSource.sendFailure(Component.literal("No GTFluidPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}

		// locate the neighbouring pipe B (first side with a pipe BE)
		byte tSideToB = -1;
		GTFluidPipeBlockEntity tPipeB = null;
		for (byte tSide = 0; tSide < 6 && tSideToB < 0; tSide++) {
			if (tLevel.getBlockEntity(aPos.relative(Direction.from3DDataValue(tSide))) instanceof GTFluidPipeBlockEntity tNeighbor) {
				tSideToB = tSide;
				tPipeB = tNeighbor;
			}
		}
		if (tPipeB == null) {
			aSource.sendFailure(Component.literal("No neighbouring GTFluidPipeBlockEntity next to " + aPos.toShortString() + " (place a second pipe first)"));
			return 0;
		}
		byte tSideToA = (byte)Direction.from3DDataValue(tSideToB).getOpposite().get3DDataValue();

		// 1. the explicit symmetric handshake (upstream onPlaced :82-96 / onToolClick2 :70-79).
		// Since p4-pipe-flow-control there is NO auto-connect anywhere — the command drives
		// the same connect(A→B) the player's hoe right click does.
		boolean tConnected = tPipeA.connect(tSideToB, true);
		boolean tConnectedBack = tPipeB.connected(tSideToA);
		if (!tConnected || !tConnectedBack) {
			aSource.sendFailure(Component.literal("CONNECT FAILED: A.connect(" + tSideToB + ")=" + tConnected + ", B.connected(" + tSideToA + ")=" + tConnectedBack));
			return 0;
		}

		// 2. pick the injection port (not toward B, not facing another pipe) and CONNECT it
		// explicitly — connect into air always succeeds (upstream :141 open-end, the manual
		// pipe mouth), and the upstream :464 fill gate needs the side connected.
		byte tInjectSide = injectionSide(tPipeA, tSideToB);
		if (tInjectSide < 0) {
			aSource.sendFailure(Component.literal("INJECT SKIPPED: no connectable open side on A (leave one side facing air)"));
			return 0;
		}
		if (!tPipeA.connect(tInjectSide, true)) {
			aSource.sendFailure(Component.literal("INJECT CONNECT FAILED: A.connect(" + tInjectSide + ") must succeed into air (upstream :141 open end)"));
			return 0;
		}
		int tInjected = new SideFluidHandler(tPipeA, tInjectSide).fill(new FluidStack(Fluids.WATER, (int)INJECTED), FluidAction.EXECUTE);
		if (tInjected != INJECTED) {
			aSource.sendFailure(Component.literal("INJECT FAILED: filled " + tInjected + " of " + INJECTED + " L from side " + tInjectSide));
			return 0;
		}
		// the fill marker is asserted NOW: it is consumed by A's next distribute round
		// (the round-end clear :331 is upstream verbatim — the mask guards exactly one round)
		byte tMaskAAfterFill = tPipeA.mLastReceivedFrom[0];
		if ((tMaskAAfterFill & TileEntityBase09Connector.SBIT[tInjectSide]) == 0) {
			aSource.sendFailure(Component.literal("FILL MARKER MISSING: A must carry SBIT[" + tInjectSide + "] right after the injection (upstream :487)"));
			return 0;
		}

		// 3. drive A alone: its distribute rounds push into B until the pressures equalise
		for (int i = 0; i < 14; i++) tPipeA.updateEntity();

		long tAmountA = tPipeA.mTanks[0].amount();
		long tAmountB = tPipeB.mTanks[0].amount();
		byte tMaskB = tPipeB.mLastReceivedFrom[0];
		byte tMaskA = tPipeA.mLastReceivedFrom[0];

		// 4. equalisation (acceptance: the tank contents split evenly)
		if (tAmountA != tAmountB || tAmountA != INJECTED / 2) {
			aSource.sendFailure(Component.literal("SPLIT FAILED: A=" + tAmountA + " L, B=" + tAmountB + " L (expected " + (INJECTED / 2) + "/" + (INJECTED / 2) + ")"));
			return 0;
		}
		// 5. anti-backflow markers (acceptance: the anti-backflow bits)
		if ((tMaskB & TileEntityBase09Connector.SBIT[tSideToA]) == 0) {
			aSource.sendFailure(Component.literal("BACKFLOW MARKER MISSING: B must carry SBIT[" + tSideToA + "] (upstream :389 records the receiver)"));
			return 0;
		}
		if ((tMaskA & TileEntityBase09Connector.SBIT[tSideToB]) != 0) {
			aSource.sendFailure(Component.literal("BACKFLOW VIOLATION: A must NOT carry SBIT[" + tSideToB + "] (fluid never flowed B -> A)"));
			return 0;
		}

		// 6. equilibrium is stable: B's next rounds skip the masked side (:378) and drain nothing
		tPipeB.updateEntity();
		tPipeB.updateEntity();
		if (tPipeB.mTanks[0].amount() != tAmountB || tPipeA.mTanks[0].amount() != tAmountA) {
			aSource.sendFailure(Component.literal("EQUILIBRIUM UNSTABLE after B rounds: A=" + tPipeA.mTanks[0].amount() + ", B=" + tPipeB.mTanks[0].amount()));
			return 0;
		}

		String tOk = String.format("GT6 fluid pipe check OK at %s: injected %d L from side %d, split %d/%d L, "
				+ "connections A=%d B=%d, ioMask A=%d B=%d, received masks A=%d (fill side %d) B=%d (side %d), equilibrium stable",
				aPos.toShortString(), tInjected, tInjectSide, tAmountA, tAmountB,
				tPipeA.getConnections(), tPipeB.getConnections(), tPipeA.getIoMask(), tPipeB.getIoMask(),
				tMaskA, tInjectSide, tMaskB, tSideToA);
		aSource.sendSuccess(() -> Component.literal(tOk), false);
		LOGGER.info(tOk);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The first injection port: not toward B, not facing another pipe, and connectable —
	 * a side whose neighbour is a fluid handler or air/liquid (the open end). -1 when the
	 * pipe has no usable side (fully buried).
	 */
	private static byte injectionSide(GTFluidPipeBlockEntity aPipe, byte aSideToB) {
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (tSide == aSideToB) continue;
			if (!aPipe.canConnect(tSide, aPipe.getLevel().getBlockEntity(aPipe.getBlockPos().relative(Direction.from3DDataValue(tSide))))
					&& !airOrLiquid(aPipe, tSide)) continue;
			return tSide;
		}
		return -1;
	}

	/** The open-end probe mirroring TileEntityBase09Connector.isAirOrLiquid. */
	private static boolean airOrLiquid(GTFluidPipeBlockEntity aPipe, byte aSide) {
		BlockPos tTarget = aPipe.getBlockPos().relative(Direction.from3DDataValue(aSide));
		return aPipe.getLevel().getBlockState(tTarget).isAir()
				|| !aPipe.getLevel().getFluidState(tTarget).isEmpty();
	}
}
