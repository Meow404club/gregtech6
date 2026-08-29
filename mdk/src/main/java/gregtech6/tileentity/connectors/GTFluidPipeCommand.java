package gregtech6.tileentity.connectors;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.slf4j.Logger;

import gregtech6.fluid.FluidTankGT;

/**
 * {@code /gt6pipe} — the automated fluid-pipe acceptance command (task p4-fluid-pipes
 * acceptance ②, the {@code /gt6machine} shape of GTExampleChestCommand applied to the
 * pressure-distribution chain). Game-bus listener, self-contained per ADR-P3-4.
 *
 * <ul>
 * <li>{@code accept <pos>} (console-safe) — the full card scenario against the pipe at
 *     pos (A) and its first neighbouring pipe (B): force the symmetric connect handshake,
 *     inject 100 L of water into A from a side that is not toward B (the upstream external
 *     fill, which records the source bit :487), drive {@link
 *     TileEntityBase03TicksAndSync#updateEntity} passes on A alone (its distribute rounds
 *     push into B, the divup split :415/:417 equalising both tanks), then assert
 *     equalisation (50/50), the anti-backflow marker on B toward A (:389) with A's own
 *     marker toward B still clear (no ping-pong), and the injection-side marker on A.
 *     Two further passes on B prove the equilibrium is stable (B skips the masked side
 *     :378 and clears its mask :331).</li>
 * <li>{@code stat <pos>} — dump capacity/content/connections/received-masks for debugging.</li>
 * </ul>
 *
 * <p>The command runs inside one server tick, so the manual dispatcher passes are not
 * interleaved with the live tickers — the assertions are deterministic (a real tick of B
 * cannot clear the mask between the push and the assertion).
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
						.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))));
		LOGGER.info("Registered GT6 fluid pipe command /gt6pipe (accept|stat)");
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
		String tLine = "GT6 pipe stat at " + aPos.toShortString() + ": connections " + aPipe.getConnections() + " " + tTanks;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
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

		// 1. the symmetric handshake (upstream onPlaced :82-96; onTickFirst already ran it — idempotent)
		boolean tConnected = tPipeA.connect(tSideToB, true);
		boolean tConnectedBack = tPipeB.connected(tSideToA);
		if (!tConnected || !tConnectedBack) {
			aSource.sendFailure(Component.literal("CONNECT FAILED: A.connect(" + tSideToB + ")=" + tConnected + ", B.connected(" + tSideToA + ")=" + tConnectedBack));
			return 0;
		}

		// 2. inject 100 L of water into A from a connected side that is not toward B (upstream external fill :480-490)
		byte tInjectSide = injectionSide(tPipeA, tSideToB);
		if (tInjectSide < 0) {
			aSource.sendFailure(Component.literal("INJECT SKIPPED: no connected open side on A (upstream :464 fill gate — leave one side facing air)"));
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

		// 4. equalisation (acceptance: 罐量均分)
		if (tAmountA != tAmountB || tAmountA != INJECTED / 2) {
			aSource.sendFailure(Component.literal("SPLIT FAILED: A=" + tAmountA + " L, B=" + tAmountB + " L (expected " + (INJECTED / 2) + "/" + (INJECTED / 2) + ")"));
			return 0;
		}
		// 5. anti-backflow markers (acceptance: 防回流位)
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
				+ "connections A=%d B=%d, received masks A=%d (fill side %d) B=%d (side %d), equilibrium stable",
				aPos.toShortString(), tInjected, tInjectSide, tAmountA, tAmountB,
				tPipeA.getConnections(), tPipeB.getConnections(),
				tMaskA, tInjectSide, tMaskB, tSideToA);
		aSource.sendSuccess(() -> Component.literal(tOk), false);
		LOGGER.info(tOk);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The first injection port: connected (the upstream :464 fill gate), not toward B, and
	 * not facing another pipe. -1 when the pipe has no usable open side (fully buried).
	 */
	private static byte injectionSide(GTFluidPipeBlockEntity aPipe, byte aSideToB) {
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (tSide == aSideToB) continue;
			if (!aPipe.canAcceptFluidsFrom(tSide)) continue;
			if (aPipe.hasLevel() && aPipe.getLevel().getBlockEntity(aPipe.getBlockPos().relative(Direction.from3DDataValue(tSide))) instanceof GTFluidPipeBlockEntity) continue;
			return tSide;
		}
		return -1;
	}
}
