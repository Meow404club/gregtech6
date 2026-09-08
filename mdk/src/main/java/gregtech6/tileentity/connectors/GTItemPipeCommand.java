package gregtech6.tileentity.connectors;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;

import javax.annotation.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

//? if forge {
import net.minecraftforge.items.IItemHandler;
//?} else {
/*import net.neoforged.neoforge.items.IItemHandler;
 *///?}

import gregtech6.registry.GTItemPipes;

/**
 * {@code /gt6itempipe} — the automated item-pipe acceptance command (task p26-pipe-item
 * acceptance ③, the GTFluidPipeCommand shape; game-bus listener, self-contained per
 * ADR-P3-4, NEVER touching GTFluidPipeCommand — the p26-arch ruling).
 *
 * <ul>
 * <li>{@code place <pos> <againstFace> [path]} — the headless placement driver: setBlock
 *     the pipe row (default brass_item_pipe_medium) at pos and call
 *     {@link GTItemPipeBlockEntity#onPlaced(byte)} with the given CLICKED face (0..5 —
 *     the BlockItem.placeBlock chain equivalent for /setblock).</li>
 * <li>{@code stat <pos>} — connections, the latch pair (mlast/olast), the monkeywrench
 *     masks, stepSize/invSize, the window counter and the inventory content.</li>
 * <li>{@code insert <pos> <side> <item> <count>} — the real external push: walks the
 *     slots of the side's {@link SideItemHandler} (the capability face hoppers use), so
 *     the one-way latch and the disable masks gate it exactly like live traffic.</li>
 * <li>{@code wrench <pos> <side>} / {@code toggle <pos> <side>} — the monkeywrench
 *     face-disable cycle and the wrench connection toggle, the SAME BE entries the
 *     block {@code use} path calls.</li>
 * <li>{@code accept <pos>} — the full card scenario against the pipe at pos (A) with at
 *     least two connected containers: the ST.move→IItemHandler EMPIRICAL point
 *     (tasks.p26-pipe-item spec ②) — (a) the partial-insert remainder: a destination
 *     packed to 4 free slots takes exactly 4 of a 64-stack, the remaining 60 STAY in the
 *     pipe (the GTItemMover leftover-preservation invariant); (b) the one-way latch: a
 *     foreign side refuses while the pipe holds; (c) the drain: room restored, the
 *     remainder flows out and the pipe empties. Every arm names its broken invariant on
 *     failure.</li>
 * </ul>
 *
 * <p>The command runs inside one server tick (the manual updateEntity passes are not
 * interleaved with the live tickers), so the assertions are deterministic.
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTItemPipeCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The accept drive bound (the 10t cadence + the latch stability round need ~30 ticks; 200 is generous). */
	private static final int ACCEPT_MAX_PASSES = 200;

	private GTItemPipeCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		aEvent.getDispatcher().register(
			Commands.literal("gt6itempipe")
				.requires(aSource -> aSource.hasPermission(2))
				.then(Commands.literal("place")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("againstFace", IntegerArgumentType.integer(0, 5))
							.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									(byte)IntegerArgumentType.getInteger(aContext, "againstFace"), "brass_item_pipe_medium"))
								.then(Commands.argument("path", com.mojang.brigadier.arguments.StringArgumentType.word())
									.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
											(byte)IntegerArgumentType.getInteger(aContext, "againstFace"),
											com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "path")))))))
				.then(Commands.literal("stat")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("insert")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("side", IntegerArgumentType.integer(0, 5))
							.then(Commands.argument("item", ItemArgument.item(aEvent.getBuildContext()))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
									.executes(aContext -> insert(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
											(byte)IntegerArgumentType.getInteger(aContext, "side"),
											ItemArgument.getItem(aContext, "item"), IntegerArgumentType.getInteger(aContext, "count"))))))))
				.then(Commands.literal("wrench")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("side", IntegerArgumentType.integer(0, 5))
							.executes(aContext -> wrench(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									(byte)IntegerArgumentType.getInteger(aContext, "side"))))))
				.then(Commands.literal("toggle")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("side", IntegerArgumentType.integer(0, 5))
							.executes(aContext -> toggle(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									(byte)IntegerArgumentType.getInteger(aContext, "side"))))))
				.then(Commands.literal("accept")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> accept(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))));
		LOGGER.info("Registered GT6 item pipe command /gt6itempipe (place|stat|insert|wrench|toggle|accept)");
	}

	// ---------------------------------------------------------------------------
	// place / stat / insert / wrench / toggle
	// ---------------------------------------------------------------------------

	/** The headless placement driver (the /setblock seam — onPlaced is the placeBlock equivalent). */
	private static int place(CommandSourceStack aSource, BlockPos aPos, byte aAgainstFace, String aPath) {
		ServerLevel tLevel = aSource.getLevel();
		Block tBlock = GTItemPipes.blockByPath(aPath);
		if (tBlock == null) {
			aSource.sendFailure(Component.literal("Unknown item pipe path: " + aPath));
			return 0;
		}
		tLevel.setBlock(aPos, tBlock.defaultBlockState(), Block.UPDATE_ALL);
		if (!(tLevel.getBlockEntity(aPos) instanceof GTItemPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("PLACE FAILED: no item pipe BE at " + aPos.toShortString()));
			return 0;
		}
		tPipe.onPlaced(aAgainstFace);
		String tLine = "GT6 item pipe placed at " + aPos.toShortString() + " (" + aPath + ") against face " + aAgainstFace
				+ " (support side " + gregtech6.util.UT6.OPOS[aAgainstFace] + "): connections " + tPipe.getConnections()
				+ " stepSize " + tPipe.mStepSize + " invSize " + tPipe.mInventory.getSlots();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTItemPipeBlockEntity aPipe)) {
			aSource.sendFailure(Component.literal("No GTItemPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		StringBuilder tSlots = new StringBuilder();
		for (int i = 0; i < aPipe.mInventory.getSlots(); i++) {
			ItemStack tStack = aPipe.mInventory.getStackInSlot(i);
			if (!tStack.isEmpty()) {
				tSlots.append("[slot ").append(i).append(": ").append(tStack.getCount()).append("x ")
						.append(tStack.getItem()).append("] ");
			}
		}
		String tLine = "GT6 item pipe stat at " + aPos.toShortString() + ": connections " + aPipe.getConnections()
				+ " mlast " + aPipe.mLastReceivedFrom + " olast " + aPipe.oLastReceivedFrom
				+ " inputs " + aPipe.mDisabledInputs + " outputs " + aPipe.mDisabledOutputs
				+ " stepSize " + aPipe.mStepSize + " invSize " + aPipe.mInventory.getSlots()
				+ " transferred " + aPipe.mTransferredItems + " " + (tSlots.length() == 0 ? "(empty)" : tSlots);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The real external push through the side wrapper — the same face a hopper's capability
	 * insert lands on, latch and masks included. Walks the slots like the vanilla inserters.
	 */
	private static int insert(CommandSourceStack aSource, BlockPos aPos, byte aSide, ItemInput aItem, int aCount) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTItemPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTItemPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		ItemStack tStack;
		try {
			tStack = aItem.createItemStack(1, false);
		} catch (CommandSyntaxException tException) {
			aSource.sendFailure(Component.literal("Bad item: " + tException.getMessage()));
			return 0;
		}
		SideItemHandler tHandler = new SideItemHandler(tPipe, aSide);
		int tRemaining = aCount;
		for (int tSlot = 0; tSlot < tHandler.getSlots() && tRemaining > 0; tSlot++) {
			ItemStack tOffer = tStack.copy();
			tOffer.setCount(tRemaining);
			ItemStack tRest = tHandler.insertItem(tSlot, tOffer, false);
			tRemaining = tRest.isEmpty() ? 0 : tRest.getCount();
		}
		int tInserted = aCount - tRemaining;
		String tLine = "GT6 item pipe insert at " + aPos.toShortString() + " side " + aSide + ": inserted " + tInserted
				+ " of " + aCount + (tInserted == 0 ? " (REJECTED)" : "")
				+ ", mlast " + tPipe.mLastReceivedFrom + ", now holds " + countContent(tPipe);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return tInserted > 0 ? Command.SINGLE_SUCCESS : 0;
	}

	/** The monkeywrench face-disable cycle (the shared BE entry the block use path calls). */
	private static int wrench(CommandSourceStack aSource, BlockPos aPos, byte aSide) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTItemPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTItemPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		boolean tCycled = tPipe.monkeyWrench(aSide);
		String tLine = "GT6 item pipe wrench at " + aPos.toShortString() + " side " + aSide + ": "
				+ (tCycled ? "cycled" : "REFUSED (two pipes / bad side)")
				+ ", inputs " + tPipe.mDisabledInputs + " outputs " + tPipe.mDisabledOutputs;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return tCycled ? Command.SINGLE_SUCCESS : 0;
	}

	/** The wrench connection toggle (the shared BE entry the block use path calls). */
	private static int toggle(CommandSourceStack aSource, BlockPos aPos, byte aSide) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GTItemPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTItemPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		boolean tResult = tPipe.toggleConnection(aSide);
		String tLine = "GT6 item pipe toggle at " + aPos.toShortString() + " side " + aSide + ": "
				+ (tResult ? "ok" : "FAILED") + ", connections " + tPipe.getConnections();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return tResult ? Command.SINGLE_SUCCESS : 0;
	}

	// ---------------------------------------------------------------------------
	// accept — the ST.move→IItemHandler empirical point (spec ②) + latch + remainder
	// ---------------------------------------------------------------------------

	private static int accept(CommandSourceStack aSource, BlockPos aPos) throws CommandSyntaxException {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTItemPipeBlockEntity tPipe)) {
			aSource.sendFailure(Component.literal("No GTItemPipeBlockEntity at " + aPos.toShortString()));
			return 0;
		}

		// locate two container faces (connected, non-connector, real item handlers)
		byte tSourceSide = -1, tDestSide = -1;
		IItemHandler tSource = null, tDest = null;
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (!tPipe.connected(tSide)) continue;
			BlockEntity tNeighbor = tLevel.getBlockEntity(aPos.relative(Direction.from3DDataValue(tSide)));
			if (tNeighbor == null || tNeighbor instanceof TileEntityBase09Connector) continue;
			IItemHandler tHandler = GTItemPipeBlockEntity.itemHandlerOf(tNeighbor, Direction.from3DDataValue(tSide).getOpposite());
			if (tHandler == null || tHandler.getSlots() <= 0) continue;
			if (tSourceSide < 0) {
				tSourceSide = tSide;
				tSource = tHandler;
			} else {
				tDestSide = tSide;
				tDest = tHandler;
				break;
			}
		}
		if (tDest == null) {
			aSource.sendFailure(Component.literal("ACCEPT SKIPPED: the pipe needs TWO connected containers (connect a source and a destination first)"));
			return 0;
		}

		// fixture: the destination packed full, then exactly 4 free slots carved out of slot 0
		ItemStack tFiller = new ItemStack(net.minecraft.world.item.Items.STONE, 64);
		for (int tSlot = 0; tSlot < tDest.getSlots(); tSlot++) {
			tDest.extractItem(tSlot, 64, false); // start clean (a rerun over the same chest)
			tDest.insertItem(tSlot, tFiller.copy(), false);
		}
		tDest.extractItem(0, 4, false); // the 60/64 slot — the partial-insert tight spot

		// arm ①: a 64 stack into the pipe through the REAL side wrapper (the latch arms here)
		SideItemHandler tSourceFace = new SideItemHandler(tPipe, tSourceSide);
		int tInserted = insertAll(tSourceFace, new ItemStack(net.minecraft.world.item.Items.STONE, 64));
		if (tInserted != 64) {
			return fail(aSource, "INSERT FAILED: the empty pipe took " + tInserted + " of 64 through side " + tSourceSide);
		}

		// arm ②: the one-way latch — a foreign connected container face refuses while the pipe holds
		boolean tForeignAccepted = false;
		if (tPipe.canInsertItem(0, new ItemStack(net.minecraft.world.item.Items.DIRT), tDestSide)) {
			tForeignAccepted = true;
		}
		if (tForeignAccepted) {
			return fail(aSource, "LATCH FAILED: the foreign side " + tDestSide + " accepted while the pipe holds (mlast " + tPipe.mLastReceivedFrom + ")");
		}

		// arm ③: the transfer rounds — exactly the 4 free slots move, 60 STAY in the pipe
		driveRounds(tPipe);
		int tPipeHolds = countContent(tPipe);
		int tDestSlot0 = tDest.getStackInSlot(0).getCount();
		if (tDestSlot0 != 64 || tPipeHolds != 60) {
			return fail(aSource, "PARTIAL INSERT FAILED: destSlot0=" + tDestSlot0 + " (want 64), pipeHolds=" + tPipeHolds
					+ " (want 60) — the ST.move remainder-preservation invariant broke");
		}

		// arm ④: room restored → the remainder drains and the pipe empties
		tDest.extractItem(0, 64, false);
		for (int tSlot = 1; tSlot < tDest.getSlots(); tSlot++) {
			tDest.extractItem(tSlot, 64, false);
		}
		driveRounds(tPipe);
		int tPipeAfter = countContent(tPipe);
		if (tPipeAfter != 0) {
			return fail(aSource, "DRAIN FAILED: the pipe still holds " + tPipeAfter + " after the destination emptied");
		}

		String tOk = "GT6 item pipe accept OK at " + aPos.toShortString()
				+ ": inserted 64 via side " + tSourceSide + ", moved 4 (partial insert), remainder 60 kept then drained, latch held side " + tSourceSide;
		aSource.sendSuccess(() -> Component.literal(tOk), false);
		LOGGER.info(tOk);
		return Command.SINGLE_SUCCESS;
	}

	/** The deterministic drive: one updateEntity per call, bounded (the fluid accept shape). */
	private static void driveRounds(GTItemPipeBlockEntity aPipe) {
		for (int i = 0; i < ACCEPT_MAX_PASSES && aPipe.inventoryHasSomething(); i++) {
			aPipe.updateEntity();
		}
	}

	private static int insertAll(SideItemHandler aHandler, ItemStack aStack) {
		int tRemaining = aStack.getCount();
		for (int tSlot = 0; tSlot < aHandler.getSlots() && tRemaining > 0; tSlot++) {
			ItemStack tOffer = aStack.copy();
			tOffer.setCount(tRemaining);
			ItemStack tRest = aHandler.insertItem(tSlot, tOffer, false);
			tRemaining = tRest.isEmpty() ? 0 : tRest.getCount();
		}
		return aStack.getCount() - tRemaining;
	}

	private static int countContent(GTItemPipeBlockEntity aPipe) {
		int tCount = 0;
		for (int i = 0; i < aPipe.mInventory.getSlots(); i++) {
			tCount += aPipe.mInventory.getStackInSlot(i).getCount();
		}
		return tCount;
	}

	private static int fail(CommandSourceStack aSource, String aMessage) {
		aSource.sendFailure(Component.literal(aMessage));
		LOGGER.error(aMessage);
		return 0;
	}
}
