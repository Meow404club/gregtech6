package gregtech6.tileentity.connectors;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

import gregtech6.block.logistics.GTLogisticsWireBlock;
import gregtech6.covers.covers.AbstractCoverAttachmentLogistics;
import gregtech6.registry.GT6Logistics;
import gregtech6.tileentity.logistics.ITileEntityLogisticsStorage;
import gregtech6.tileentity.multiblocks.GT6LogisticsCoreBlockEntity;
import gregtech6.tileentity.tank.GTBarrelLogisticsBlockEntity;

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
 * <li><b>task p32-logistics-lv3 — the core subtree</b> (the GTItemPipeCommand
 *     place|insert|accept|stat shape): {@code core form <pos>} (the forced structure
 *     check + the four CPU pools), {@code core stat <pos>} (power, the used-op counters,
 *     the tier registration, the protected-set size, the moved totals), {@code core
 *     import <pos> &lt;fluid&gt; &lt;amount&gt;} / {@code core export <pos> &lt;amount&gt;}
 *     (the endpoint intake/outtake drivers through the capability), {@code core accept
 *     <pos> &lt;fluid&gt;} (the endpoint tier + filter verdict).</li>
 * <li>{@code tank priority <pos> <auto|0..3>} — the persisted logistics-tier override on
 *     the logistics tank (the headless form of the cover-card screwdriver face).</li>
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
				.then(
					Commands.literal("core")
						.then(
							Commands.literal("form")
								.then(
									Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(aContext -> coreForm(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))
								)
						)
						.then(
							Commands.literal("stat")
								.then(
									Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(aContext -> coreStat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))
								)
						)
						.then(
							Commands.literal("import")
								.then(
									Commands.argument("pos", BlockPosArgument.blockPos())
										.then(
											Commands.argument("fluid", StringArgumentType.string())
												.then(
													Commands.argument("amount", IntegerArgumentType.integer(1))
														.executes(aContext -> coreImport(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
																StringArgumentType.getString(aContext, "fluid"), IntegerArgumentType.getInteger(aContext, "amount")))
												)
										)
								)
						)
						.then(
							Commands.literal("export")
								.then(
									Commands.argument("pos", BlockPosArgument.blockPos())
										.then(
											Commands.argument("amount", IntegerArgumentType.integer(1))
												.executes(aContext -> coreExport(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
														IntegerArgumentType.getInteger(aContext, "amount")))
										)
								)
						)
						.then(
							Commands.literal("accept")
								.then(
									Commands.argument("pos", BlockPosArgument.blockPos())
										.then(
											Commands.argument("fluid", StringArgumentType.string())
												.executes(aContext -> coreAccept(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
														StringArgumentType.getString(aContext, "fluid")))
										)
								)
						)
				)
				.then(
					Commands.literal("tank")
						.then(
							Commands.literal("priority")
								.then(
									Commands.argument("pos", BlockPosArgument.blockPos())
										.then(
											Commands.argument("tier", StringArgumentType.string())
												.executes(aContext -> tankPriority(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
														StringArgumentType.getString(aContext, "tier")))
										)
								)
						)
				)
		);
		LOGGER.info("Registered GT6 logistics command /gt6logistics (wire place|wire stat|gate|core form|core stat|core import|core export|core accept|tank priority)");
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

	// ---------------------------------------------------------------------------
	// core form / stat / import / export / accept + tank priority (task p32-logistics-lv3)
	// ---------------------------------------------------------------------------

	/** The structure driver — the forced checkStructure pass with the four CPU pools as the verdict. */
	private static int coreForm(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6LogisticsCoreBlockEntity tCore)) {
			aSource.sendFailure(Component.literal("No GT6 logistics core at " + aPos.toShortString()));
			return 0;
		}
		boolean tFormed = tCore.checkStructure(true);
		String tLine = String.format("GT6 logistics core form at %s: formed=%d logic=%d control=%d storage=%d conversion=%d",
				aPos.toShortString(), tFormed ? 1 : 0, tCore.mCPU_Logic, tCore.mCPU_Control, tCore.mCPU_Storage, tCore.mCPU_Conversion);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The scan report: energy, the per-op usage counters, the tier registration and the protected-set size. */
	private static int coreStat(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6LogisticsCoreBlockEntity tCore)) {
			aSource.sendFailure(Component.literal("No GT6 logistics core at " + aPos.toShortString()));
			return 0;
		}
		tCore.checkStructure(false);
		String tHead = String.format("GT6 logistics core stat at %s: power=%d formed=%d logic=%d(%d used) control=%d(range %d/%d) storage=%d conversion=%d(%d used)",
				aPos.toShortString(), tCore.mEnergy, tCore.checkStructure(false) ? 1 : 0,
				tCore.mCPU_Logic, tCore.oCPU_Logic, tCore.mCPU_Control, tCore.oCPU_Control, tCore.mCPU_Control + 2,
				tCore.mCPU_Storage, tCore.mCPU_Conversion, tCore.oCPU_Conversion);
		send(aSource, tHead);
		send(aSource, String.format("GT6 logistics core network: fluid generic=%d semi=%d filtered=%d | item generic=%d semi=%d filtered=%d | filters=%d | moved last=%d total=%d",
				tCore.mReportFluid[0], tCore.mReportFluid[1], tCore.mReportFluid[2],
				tCore.mReportItem[0], tCore.mReportItem[1], tCore.mReportItem[2],
				tCore.mReportFilters, tCore.mMovedLast, tCore.mMovedTotal));
		return Command.SINGLE_SUCCESS;
	}

	/** The intake driver — fill the logistics endpoint at pos through its fluid handler (the moved-content source). */
	private static int coreImport(CommandSourceStack aSource, BlockPos aPos, String aFluidId, int aAmount) {
		BlockEntity tEndpoint = aSource.getLevel().getBlockEntity(aPos);
		IFluidHandler tHandler = tEndpoint == null ? null : GT6LogisticsCoreBlockEntity.fluidHandler(tEndpoint);
		Fluid tFluid = resolveFluid(aFluidId);
		if (tHandler == null || tFluid == null) {
			aSource.sendFailure(Component.literal("IMPORT FAILED: no fluid endpoint or unknown fluid " + aFluidId + " at " + aPos.toShortString()));
			return 0;
		}
		int tFilled = tHandler.fill(new FluidStack(tFluid, aAmount), FluidAction.EXECUTE);
		String tLine = String.format("GT6 logistics import at %s: filled %d/%d L of %s", aPos.toShortString(), tFilled, aAmount, aFluidId);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The outtake driver — drain the endpoint at pos, the moved-amount observable at the destination. */
	private static int coreExport(CommandSourceStack aSource, BlockPos aPos, int aAmount) {
		BlockEntity tEndpoint = aSource.getLevel().getBlockEntity(aPos);
		IFluidHandler tHandler = tEndpoint == null ? null : GT6LogisticsCoreBlockEntity.fluidHandler(tEndpoint);
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("EXPORT FAILED: no fluid endpoint at " + aPos.toShortString()));
			return 0;
		}
		FluidStack tDrawnStack = tHandler.drain(aAmount, FluidAction.EXECUTE);
		int tDrawn = tDrawnStack == null ? 0 : tDrawnStack.getAmount();
		String tFluid = tDrawnStack == null || tDrawnStack.getAmount() <= 0 ? "nothing" : String.valueOf(ForgeRegistries.FLUIDS.getKey(tDrawnStack.getFluid()));
		String tLine = String.format("GT6 logistics export at %s: drained %d/%d L of %s", aPos.toShortString(), tDrawn, aAmount, tFluid);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The filter verdict — the endpoint's tier, its filter fluid and whether the queried fluid matches. */
	private static int coreAccept(CommandSourceStack aSource, BlockPos aPos, String aFluidId) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof ITileEntityLogisticsStorage tStorage)) {
			aSource.sendFailure(Component.literal("No logistics storage endpoint at " + aPos.toShortString()));
			return 0;
		}
		Fluid tQuery = resolveFluid(aFluidId);
		Fluid tFilter = tStorage.getLogisticsFilterFluid();
		boolean tMatch = tQuery != null && (tFilter == null || tFilter == tQuery);
		String tLine = String.format("GT6 logistics accept at %s: priority=%d filter=%s match=%d (%s)", aPos.toShortString(),
				tStorage.getLogisticsPriorityFluid(), tFilter == null ? "null" : String.valueOf(ForgeRegistries.FLUIDS.getKey(tFilter)),
				tMatch ? 1 : 0, aFluidId);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The endpoint tier-config seam — the persisted {@code gt.logistics.priority} override
	 * (the headless form of the cover-card screwdriver face): "auto" restores the upstream
	 * content-derived answer, 0..3 sets the tier.
	 */
	private static int tankPriority(CommandSourceStack aSource, BlockPos aPos, String aTier) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTBarrelLogisticsBlockEntity tTank)) {
			aSource.sendFailure(Component.literal("No GT6 logistics tank at " + aPos.toShortString()));
			return 0;
		}
		int tPriority;
		if ("auto".equals(aTier)) {
			tPriority = -1;
		} else {
			try {
				tPriority = Integer.parseInt(aTier);
			} catch (NumberFormatException aE) {
				aSource.sendFailure(Component.literal("Tier must be auto|0|1|2|3, got " + aTier));
				return 0;
			}
			if (tPriority < 0 || tPriority > 3) {
				aSource.sendFailure(Component.literal("Tier must be auto|0|1|2|3, got " + aTier));
				return 0;
			}
		}
		tTank.setLogisticsPriority(tPriority);
		String tLine = String.format("GT6 logistics tank priority at %s: %s (effective fluid priority %d)",
				aPos.toShortString(), tPriority < 0 ? "auto" : String.valueOf(tPriority), tTank.getLogisticsPriorityFluid());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	@javax.annotation.Nullable
	private static Fluid resolveFluid(String aFluidId) {
		ResourceLocation tId = ResourceLocation.tryParse(aFluidId);
		if (tId == null) return null;
		Fluid tFluid = ForgeRegistries.FLUIDS.getValue(tId);
		return tFluid != null && tFluid.defaultFluidState() != null && !tFluid.defaultFluidState().isEmpty() ? tFluid : null;
	}

	private static void send(CommandSourceStack aSource, String aLine) {
		aSource.sendSuccess(() -> Component.literal(aLine), false);
		LOGGER.info(aLine);
	}
}
