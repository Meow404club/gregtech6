package gregtech6.tileentity.connectors;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

import gregtech6.block.logistics.GTLogisticsWireBlock;
import gregtech6.covers.covers.AbstractCoverAttachmentLogistics;
import gregtech6.registry.GT6Logistics;

/**
 * {@code /gt6logistics} — the automated logistics acceptance command (task
 * p32-logistics-lv2 acceptance ②, the GTItemPipeCommand shape; game-bus listener,
 * self-contained per ADR-P3-4, NEVER touching the other connector commands).
 *
 * <ul>
 * <li>{@code wire place <pos> <againstFace>} — the headless placement driver: setBlock
 *     the wire at pos and call {@link GTLogisticsWireBlockEntity#onPlaced(byte)} with the
 *     given CLICKED face (0..5 — the BlockItem.placeBlock chain equivalent for /setblock).</li>
 * <li>{@code wire stat <pos>} — the connection mask plus the per-side verdict row
 *     {@code side <n> connected=<0|1> member=<0|1> attach=<0|1>(<neighbour>)}: member is
 *     the {@link GTLogisticsWireBlockEntity#canLogistics(byte)} answer (the adjacency
 *     spread observable — connected sides propagate the member answer, upstream :47),
 *     attach is the non-connector acceptance gate ({@link GTLogisticsWireBlockEntity#canConnect(byte, BlockEntity)},
 *     upstream :42-45 — the "非成员拒绝" arm) with the neighbour BE class named.</li>
 * <li>{@code gate <pos>} — the {@link AbstractCoverAttachmentLogistics} placement gate
 *     driven live against the host at pos (acceptance ③): a member host answers allowed,
 *     everything else refused.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6LogisticsCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6LogisticsCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		aEvent.getDispatcher().register(
			Commands.literal("gt6logistics")
				.requires(aSource -> aSource.hasPermission(2))
				.then(
					Commands.literal("wire")
						.then(
							Commands.literal("place")
								.then(
									Commands.argument("pos", BlockPosArgument.blockPos())
										.then(
											Commands.argument("againstFace", IntegerArgumentType.integer(0, 5))
												.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
														(byte)IntegerArgumentType.getInteger(aContext, "againstFace")))
										)
								)
						)
						.then(
							Commands.literal("stat")
								.then(
									Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))
								)
						)
				)
				.then(
					Commands.literal("gate")
						.then(
							Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> gate(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))
						)
				)
		);
		LOGGER.info("Registered GT6 logistics command /gt6logistics (wire place|wire stat|gate)");
	}

	// ---------------------------------------------------------------------------
	// place / stat / gate
	// ---------------------------------------------------------------------------

	/** The headless placement driver (the /setblock seam — onPlaced is the placeBlock equivalent). */
	private static int place(CommandSourceStack aSource, BlockPos aPos, byte aAgainstFace) {
		ServerLevel tLevel = aSource.getLevel();
		tLevel.setBlock(aPos, GT6Logistics.LOGISTICS_WIRE.get().defaultBlockState(), Block.UPDATE_ALL);
		if (!(tLevel.getBlockEntity(aPos) instanceof GTLogisticsWireBlockEntity tWire)) {
			aSource.sendFailure(Component.literal("PLACE FAILED: no logistics wire BE at " + aPos.toShortString()));
			return 0;
		}
		tWire.onPlaced(aAgainstFace);
		String tLine = "GT6 logistics wire placed at " + aPos.toShortString() + " against face " + aAgainstFace
				+ " (support side " + gregtech6.util.UT6.OPOS[aAgainstFace] + "): connections " + tWire.getConnections();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTLogisticsWireBlockEntity tWire)) {
			aSource.sendFailure(Component.literal("No GTLogisticsWireBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		send(aSource, "GT6 logistics wire stat at " + aPos.toShortString() + ": connections " + tWire.getConnections());
		ServerLevel tLevel = aSource.getLevel();
		for (byte tSide = 0; tSide < 6; tSide++) {
			Direction tDirection = Direction.from3DDataValue(tSide);
			BlockEntity tNeighbor = tLevel.getBlockEntity(aPos.relative(tDirection));
			String tNeighbour = tNeighbor == null ? "air" : tNeighbor.getClass().getSimpleName();
			// attach: the base-handshake verdict a NEW connection would get on this side —
			// connector neighbours via the type intersection (upstream TileEntityBase09Connector :118),
			// non-connector BEs via the canConnect member gate (upstream MultiTileEntityWireLogistics :42-45)
			boolean tAttach;
			if (tNeighbor instanceof TileEntityBase09Connector tConnector) {
				tAttach = TileEntityBase09Connector.haveOneCommonElement(
						tConnector.getConnectorTypes((byte)tDirection.getOpposite().get3DDataValue()), tWire.getConnectorTypes(tSide));
			} else {
				tAttach = tNeighbor != null && tWire.canConnect(tSide, tNeighbor);
			}
			send(aSource, "side " + tSide + ": connected=" + (tWire.connected(tSide) ? 1 : 0)
					+ " member=" + (tWire.canLogistics(tSide) ? 1 : 0)
					+ " attach=" + (tAttach ? 1 : 0) + " (" + tNeighbour + ")");
		}
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * Acceptance ③ live — the cover placement gate against the host at pos, driven through
	 * the real {@link AbstractCoverAttachmentLogistics#refusesAttachment} decision (the
	 * pure half of interceptCoverPlacement, AbstractCoverAttachmentLogistics.java:40): a
	 * member host answers allowed, everything else refused.
	 */
	private static int gate(CommandSourceStack aSource, BlockPos aPos) {
		BlockEntity tHost = aSource.getLevel().getBlockEntity(aPos);
		if (tHost == null) {
			aSource.sendFailure(Component.literal("No block entity at " + aPos.toShortString()));
			return 0;
		}
		boolean tRefused = AbstractCoverAttachmentLogistics.refusesAttachment(tHost);
		String tLine = "GT6 logistics cover gate at " + aPos.toShortString() + ": " + (tRefused ? "refused" : "allowed")
				+ " (host " + tHost.getClass().getSimpleName() + ")";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static void send(CommandSourceStack aSource, String aLine) {
		aSource.sendSuccess(() -> Component.literal(aLine), false);
		LOGGER.info(aLine);
	}
}
