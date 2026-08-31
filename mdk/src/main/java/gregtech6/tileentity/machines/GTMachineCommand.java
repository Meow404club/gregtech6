package gregtech6.tileentity.machines;

import java.util.function.Supplier;

import javax.annotation.Nullable;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.gui.machines.GTBasicMachinesMenus;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMachines;

/**
 * {@code /gt6machine} — the machine-family acceptance command (task p7-basicmachine-family
 * ⑥, RCON-drivable, GTOvenCommand :63-98 template, console-safe throughout): one literal per
 * registered machine ({@code <shredder|crusher|lathe>}) carrying the four subcommands
 *
 * <ul>
 * <li>{@code place [<pos>]} — setBlock a fresh machine (FACING north);</li>
 * <li>{@code input [<count>] [<pos>]} — push the default feed into the input slot (default
 *     8): shredder = cobblestone (Loader_Recipes_Vanilla.java:692), crusher = the first
 *     resolvable {@code gt6:gem_*} of the gem chain (Loader_Recipes_Handlers.java:72,
 *     gem → gemFlawed x2), lathe = stone (:524);</li>
 * <li>{@code run <ticks> [<pos>]} — drives the BE dispatcher tick by tick and samples the
 *     live menu ContainerData value ({@link GTBasicMachineMenu#computeProgressValue()}): the
 *     acceptance asserts all three states observed (progress &gt;0 &lt;32767, done 32767 via
 *     mSuccessful, idle -1) plus the output slots filling. The real server ticker keeps
 *     ticking alongside — this only accelerates the same dispatcher.</li>
 * <li>{@code check [<pos>]} — state report: progress/maxprogress/energy/active/running/
 *     parallel + the slot contents.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTMachineCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The acceptance feed size (the oven command default). */
	private static final int DEFAULT_INPUT_COUNT = 8;

	private GTMachineCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> tMachine = Commands.literal("gt6machine")
			.requires(source -> source.hasPermission(2))
			.then(machine("shredder", GTMachines.SHREDDER, () -> Items.COBBLESTONE)) // Loader_Recipes_Vanilla.java:692
			.then(machine("crusher", GTMachines.CRUSHER, GTMachineCommand::firstGemChainGem)) // Loader_Recipes_Handlers.java:72
			.then(machine("lathe", GTMachines.LATHE, () -> net.minecraft.world.level.block.Blocks.STONE.asItem())); // Loader_Recipes_Vanilla.java:524
		event.getDispatcher().register(tMachine);
		LOGGER.info("Registered GT6 machine acceptance command /gt6machine (shredder|crusher|lathe x place|input|run|check)");
	}

	/** One machine literal with its four subcommands (the oven command shape, parameterised). */
	private static LiteralArgumentBuilder<CommandSourceStack> machine(String aName, RegistryObject<Block> aBlock, Supplier<Item> aFeed) {
		LiteralArgumentBuilder<CommandSourceStack> tMachine = Commands.literal(aName);
		tMachine.then(Commands.literal("place")
			.executes(context -> place(context.getSource(), aBlock, null))
			.then(Commands.argument("pos", BlockPosArgument.blockPos())
				.executes(context -> place(context.getSource(), aBlock, BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		tMachine.then(Commands.literal("input")
			.executes(context -> input(context.getSource(), aFeed, DEFAULT_INPUT_COUNT, null))
			.then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64))
				.executes(context -> input(context.getSource(), aFeed,
						com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> input(context.getSource(), aFeed,
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"),
							BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
		tMachine.then(Commands.literal("run")
			.then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20000))
				.executes(context -> run(context.getSource(),
						com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> run(context.getSource(),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"),
							BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
		tMachine.then(Commands.literal("check")
			.executes(context -> check(context.getSource(), null))
			.then(Commands.argument("pos", BlockPosArgument.blockPos())
				.executes(context -> check(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		return tMachine;
	}

	/**
	 * The crusher acceptance feed: the first (prefix, material) pair of the gem-chain
	 * registration order whose gt6 item resolved (Loader_Recipes_Handlers.java:69-73 walk —
	 * the first row is gemLegendary, so the first resolvable material of OP.gem feeds :72's
	 * gem → gemFlawed x2). Null-feed rows are the offline/unregistered edge; the command
	 * reports them.
	 */
	private static Item firstGemChainGem() {
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() != OP.gem) continue;
			RegistryObject<Item> tHandle = GTMaterialItems.get(tPair.prefix(), tPair.material());
			if (tHandle != null && tHandle.isPresent()) return tHandle.get();
			OreDictMaterial tMaterial = tPair.material();
			LOGGER.info("GT6 machine feed: gem material {} has no registered item, walking on", tMaterial.mNameInternal);
		}
		throw new IllegalStateException("No gt6:gem item resolved for the crusher feed");
	}

	private static TileEntityBasicMachine machineAt(CommandSourceStack source, BlockPos pos) {
		ServerLevel tLevel = source.getLevel();
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		return tLevel.getBlockEntity(tTarget) instanceof TileEntityBasicMachine tMachine ? tMachine : null;
	}

	private static int place(CommandSourceStack source, RegistryObject<Block> aBlock, BlockPos pos) {
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		ServerLevel tLevel = source.getLevel();
		tLevel.setBlock(tTarget, aBlock.get().defaultBlockState(), 3);
		source.sendSuccess(() -> Component.literal("GT6 " + aBlock.getId().getPath() + " placed at " + tTarget.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int input(CommandSourceStack source, Supplier<Item> aFeed, int count, BlockPos pos) {
		TileEntityBasicMachine tMachine = machineAt(source, pos);
		if (tMachine == null) {
			source.sendFailure(Component.literal("No TileEntityBasicMachine at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		ItemStack tLeftover = tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(aFeed.get(), count), false);
		if (!tLeftover.isEmpty()) {
			source.sendFailure(Component.literal("Input slot rejected " + tLeftover.getCount() + " of the feed (blocked?)"));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("GT6 " + tMachine.getTileEntityName() + " input: " + count + "x "
				+ aFeed.get() + " into slot " + TileEntityBasicMachine.SLOT_INPUT), false);
		return Command.SINGLE_SUCCESS;
	}

	/** Drives the dispatcher and samples the three ContainerData states; asserts the acceptance trio + an output. */
	private static int run(CommandSourceStack source, int ticks, BlockPos pos) {
		TileEntityBasicMachine tMachine = machineAt(source, pos);
		if (tMachine == null) {
			source.sendFailure(Component.literal("No TileEntityBasicMachine at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		ServerPlayer tFakePlayer = FakePlayerFactory.getMinecraft(source.getLevel());
		GTBasicMachineMenu tMenu = (GTBasicMachineMenu) tMachine.createMenu(0, tFakePlayer.getInventory(), tFakePlayer);

		boolean tSeenProgress = false, tSeenDone = false, tSeenIdle = false;
		int tLastValue = tMenu.computeProgressValue();
		for (int i = 0; i < ticks; i++) {
			tMachine.updateEntity();
			int tValue = tMenu.computeProgressValue();
			if (tValue > 0 && tValue < GTBasicMachineMenu.PROGRESS_DONE) tSeenProgress = true;
			else if (tValue == GTBasicMachineMenu.PROGRESS_DONE) tSeenDone = true;
			else if (tValue == -1) tSeenIdle = true;
			tLastValue = tValue;
		}
		StringBuilder tOutputs = new StringBuilder();
		for (int i = 0; i < tMachine.getOutputSlotCount(); i++) {
			ItemStack tStack = tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount() + i);
			if (!tStack.isEmpty()) tOutputs.append(tStack.getCount()).append("x ").append(tStack.getItem()).append("; ");
		}
		tMenu.removed(tFakePlayer);

		boolean tOk = tSeenProgress && tSeenDone && tSeenIdle && tOutputs.length() > 0;
		String tReport = String.format(
			"GT6 %s run %d ticks at %s: ContainerData states progress=%s done=%s idle=%s, last=%d, outputs=[%s], progress=%d/%d, energy=%d, parallel=%d",
			tMachine.getTileEntityName(), ticks, tMachine.getBlockPos().toShortString(), tSeenProgress, tSeenDone, tSeenIdle, tLastValue,
			tOutputs, tMachine.mProgress, tMachine.mMaxProgress, tMachine.mEnergy, tMachine.mParallel);
		if (!tOk) {
			source.sendFailure(Component.literal("GT6 machine run check FAILED: " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("GT6 machine run check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	private static int check(CommandSourceStack source, BlockPos pos) {
		TileEntityBasicMachine tMachine = machineAt(source, pos);
		if (tMachine == null) {
			source.sendFailure(Component.literal("No TileEntityBasicMachine at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		ServerPlayer tFakePlayer = FakePlayerFactory.getMinecraft(source.getLevel());
		GTBasicMachineMenu tMenu = (GTBasicMachineMenu) tMachine.createMenu(0, tFakePlayer.getInventory(), tFakePlayer);
		int tDataValue = tMenu.computeProgressValue();
		tMenu.removed(tFakePlayer);

		StringBuilder tSlots = new StringBuilder();
		ItemStack tInput = tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT);
		tSlots.append("input=").append(tInput.getItem()).append("x").append(tInput.getCount());
		for (int i = 0; i < tMachine.getOutputSlotCount(); i++) {
			ItemStack tStack = tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount() + i);
			if (!tStack.isEmpty()) tSlots.append(" out[").append(i).append("]=").append(tStack.getCount()).append("x ").append(tStack.getItem());
		}

		CompoundTag tDebug = new CompoundTag();
		tMachine.saveAdditional(tDebug);
		String tReport = String.format(
			"GT6 %s at %s: %s data=%d progress=%d/%d energy=%d stopped=%s active=%s running=%s parallel=%d parallelDuration=%s facing=%d",
			tMachine.getTileEntityName(), tMachine.getBlockPos().toShortString(), tSlots, tDataValue,
			tMachine.mProgress, tMachine.mMaxProgress, tMachine.mEnergy,
			tMachine.mStopped, tMachine.mActive, tMachine.mRunning, tMachine.mParallel, tMachine.mParallelDuration, tMachine.getFacing());
		source.sendSuccess(() -> Component.literal(tReport), false);
		return Command.SINGLE_SUCCESS;
	}
}
