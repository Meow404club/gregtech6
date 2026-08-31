package gregtech6.tileentity.machines;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import gregtech6.gui.machines.GTOvenMenu;
import gregtech6.registry.GTMachines;

/**
 * {@code /gt6oven} — the machine-level acceptance command (task p4-machine-oven ⑨, W2
 * exclusive, RCON-drivable like the chest's /gt6machine). Console-safe throughout
 * (FakePlayerFactory for the menu path, no client involved):
 *
 * <ul>
 * <li>{@code place [<pos>]} — setBlock a fresh oven (FACING north);</li>
 * <li>{@code input [<count>] [<pos>]} — push cobblestone into the input slot (default 8,
 *     the acceptance feed);</li>
 * <li>{@code run <ticks> [<pos>]} — drives the BE dispatcher tick by tick and samples the
 *     live menu ContainerData value ({@link GTOvenMenu#computeProgressValue()}) every tick:
 *     the acceptance asserts all three states observed (progress &gt;0 &lt;32767, done
 *     32767 via mSuccessful, idle -1) plus the output slot filling. The real server ticker
 *     keeps ticking alongside — this only accelerates the same dispatcher.</li>
 * <li>{@code check [<pos>]} — state report: slots, progress trio, energy, stop flags;</li>
 * <li>{@code rotate &lt;side 0..6&gt; [<pos>]} — the front-facing rotation through the
 *     same BE entry the shift-hoe grid path calls ({@link TileEntityOven#setFrontFacing},
 *     task p6-oven-rotation): vertical sides (0/1), SIDE_INVALID (6) and the
 *     same-facing call are REJECTED.</li>
 * </ul>
 *
 * <p>The ContainerData assertions go through the server-side live computation — the same
 * function vanilla's broadcastChanges polls; the packet round-trip to a real client screen
 * is the runClient visual check left to the user (ADR-P3-8 line 4 precedent).
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTOvenCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The acceptance feed: cobblestone smelts to stone through the vanilla bridge. */
	private static final int DEFAULT_INPUT_COUNT = 8;

	private GTOvenCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> tOven = Commands.literal("gt6oven")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("place")
				.executes(context -> place(context.getSource(), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> place(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))
			.then(Commands.literal("input")
				.executes(context -> input(context.getSource(), DEFAULT_INPUT_COUNT, null))
				.then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64))
					.executes(context -> input(context.getSource(),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"), null))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> input(context.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"),
								BlockPosArgument.getLoadedBlockPos(context, "pos"))))))
			.then(Commands.literal("run")
				.then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20000))
					.executes(context -> run(context.getSource(),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"), null))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> run(context.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"),
								BlockPosArgument.getLoadedBlockPos(context, "pos"))))))
			.then(Commands.literal("check")
				.executes(context -> check(context.getSource(), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> check(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))
			.then(Commands.literal("rotate")
				.then(Commands.argument("side", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 6))
					.executes(context -> rotate(context.getSource(),
							(byte) com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "side"), null))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> rotate(context.getSource(),
								(byte) com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "side"),
								BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
		event.getDispatcher().register(tOven);
		LOGGER.info("Registered GT6 machine acceptance command /gt6oven (place|input|run|check|rotate)");
	}

	private static TileEntityOven ovenAt(CommandSourceStack source, BlockPos pos) {
		ServerLevel tLevel = source.getLevel();
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		return tLevel.getBlockEntity(tTarget) instanceof TileEntityOven tOven ? tOven : null;
	}

	private static int place(CommandSourceStack source, BlockPos pos) {
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		ServerLevel tLevel = source.getLevel();
		tLevel.setBlock(tTarget, GTMachines.OVEN.get().defaultBlockState(), 3);
		source.sendSuccess(() -> Component.literal("GT6 oven placed at " + tTarget.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int input(CommandSourceStack source, int count, BlockPos pos) {
		TileEntityOven tOven = ovenAt(source, pos);
		if (tOven == null) {
			source.sendFailure(Component.literal("No TileEntityOven at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		ItemStack tLeftover = tOven.getInventory().insertItem(TileEntityOven.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, count), false);
		if (!tLeftover.isEmpty()) {
			source.sendFailure(Component.literal("Input slot rejected " + tLeftover.getCount() + " cobblestone (blocked?)"));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("GT6 oven input: " + count + " cobblestone into slot " + TileEntityOven.SLOT_INPUT), false);
		return Command.SINGLE_SUCCESS;
	}

	/** Drives the dispatcher and samples the three ContainerData states; asserts the acceptance trio. */
	private static int run(CommandSourceStack source, int ticks, BlockPos pos) {
		TileEntityOven tOven = ovenAt(source, pos);
		if (tOven == null) {
			source.sendFailure(Component.literal("No TileEntityOven at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		ServerPlayer tFakePlayer = FakePlayerFactory.getMinecraft(source.getLevel());
		GTOvenMenu tMenu = (GTOvenMenu) tOven.createMenu(0, tFakePlayer.getInventory(), tFakePlayer);

		boolean tSeenProgress = false, tSeenDone = false, tSeenIdle = false;
		int tLastValue = tMenu.computeProgressValue();
		for (int i = 0; i < ticks; i++) {
			tOven.updateEntity();
			int tValue = tMenu.computeProgressValue();
			if (tValue > 0 && tValue < GTOvenMenu.PROGRESS_DONE) tSeenProgress = true;
			else if (tValue == GTOvenMenu.PROGRESS_DONE) tSeenDone = true;
			else if (tValue == -1) tSeenIdle = true;
			tLastValue = tValue;
		}
		ItemStack tOutput = tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_OUTPUT);
		tMenu.removed(tFakePlayer);

		boolean tOk = tSeenProgress && tSeenDone && tSeenIdle && !tOutput.isEmpty();
		String tReport = String.format(
			"GT6 oven run %d ticks at %s: ContainerData states progress=%s done=%s idle=%s, last=%d, output=%s, progress=%d/%d, energy=%d, stopped=%s",
			ticks, tOven.getBlockPos().toShortString(), tSeenProgress, tSeenDone, tSeenIdle, tLastValue,
			tOutput.isEmpty() ? "EMPTY" : tOutput.getCount() + "x " + tOutput.getItem(),
			tOven.mProgress, tOven.mMaxProgress, tOven.mEnergy, tOven.mStopped);
		if (!tOk) {
			source.sendFailure(Component.literal("GT6 oven run check FAILED: " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("GT6 oven run check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	private static int check(CommandSourceStack source, BlockPos pos) {
		TileEntityOven tOven = ovenAt(source, pos);
		if (tOven == null) {
			source.sendFailure(Component.literal("No TileEntityOven at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		ServerPlayer tFakePlayer = FakePlayerFactory.getMinecraft(source.getLevel());
		GTOvenMenu tMenu = (GTOvenMenu) tOven.createMenu(0, tFakePlayer.getInventory(), tFakePlayer);
		int tDataValue = tMenu.computeProgressValue();
		tMenu.removed(tFakePlayer);

		CompoundTag tDebug = new CompoundTag();
		tOven.saveAdditional(tDebug);
		String tReport = String.format(
			"GT6 oven at %s: input=%s output=%s data=%d progress=%d/%d energy=%d minenergy=%d stopped=%s redstone=%s active=%s running=%s facing=%d",
			tOven.getBlockPos().toShortString(),
			tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getItem() + "x" + tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount(),
			tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_OUTPUT).getItem() + "x" + tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_OUTPUT).getCount(),
			tDataValue, tOven.mProgress, tOven.mMaxProgress, tOven.mEnergy, tOven.mMinEnergy,
			tOven.mStopped, tOven.mRedstoneStopped, tOven.mActive, tOven.mRunning, tOven.getFacing());
		source.sendSuccess(() -> Component.literal(tReport), false);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The rotation acceptance entry (task p6-oven-rotation ⑦): the same BE entry the
	 * shift-hoe grid path calls — {@link TileEntityOven#setFrontFacing} — so the command
	 * asserts exactly what the interaction rotates. Rejections mirror the BE double
	 * guard: vertical sides (0/1), SIDE_INVALID (6) and the same-facing call all report
	 * REJECTED and leave the facing untouched (the RCON chain asserts the rejects and
	 * reads the resulting facing back through the check's {@code facing=%d}).
	 */
	private static int rotate(CommandSourceStack source, byte side, BlockPos pos) {
		TileEntityOven tOven = ovenAt(source, pos);
		if (tOven == null) {
			source.sendFailure(Component.literal("No TileEntityOven at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		byte tOld = tOven.getFacing();
		if (!tOven.setFrontFacing(side)) {
			source.sendFailure(Component.literal("GT6 oven rotate REJECTED: side " + side
					+ " (vertical/invalid/same-facing; facing stays " + tOld + ") at " + tOven.getBlockPos().toShortString()));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("GT6 oven rotate OK: facing " + tOld + " -> " + tOven.getFacing()
				+ " at " + tOven.getBlockPos().toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}
}
