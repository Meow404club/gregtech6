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
import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.gui.machines.GTBasicMachinesMenus;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMachines;

/**
 * {@code /gt6machine} — the machine-family acceptance command (task p7-basicmachine-family
 * ⑥, RCON-drivable, GTOvenCommand :63-98 template, console-safe throughout; extended by
 * task p8-machine-tiers-doinject ⑤): one literal per registered machine
 * ({@code shredder|crusher|lathe} × {@code [t2|t3|t4]}, 12 rows) carrying the subcommands
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
 *     ticking alongside — this only accelerates the same dispatcher. Needs the fake-source
 *     seam on ({@code fakesource on}): the shipped default is grid-fed via doInject;</li>
 * <li>{@code inject <ticks> [<size>] [<pos>]} — the FALSE-regime test rig: one loop
 *     iteration = one direct {@code doInject(mEnergyTypeAccepted, side, size, 1, true)}
 *     (size defaults to the machine's mInputMax; a NEGATIVE size is the AC half-cycle,
 *     the upstream EngineSteam :146 piston-phase ±alternation) followed by one dispatcher
 *     tick, so injection and consumption land in the same tick. The KU pulse acceptance
 *     drives the whole rig live: a positive train never delivers, the negative pulse
 *     delivers on its transition tick. size 0 is refused (the Root :717 aSize != 0 gate
 *     shape — a 0 packet would divide by zero in the verbatim :503 math);</li>
 * <li>{@code check [<pos>]} — state report: progress/maxprogress/energy/minenergy/the
 *     energy three values (minIn/recIn/maxIn)/state latch/active/running/parallel + the
 *     slot contents.</li>
 * </ul>
 *
 * <p>Plus the regime switch {@code /gt6machine fakesource on|off|stat} — flips
 * {@link TileEntityBasicMachine#ENERGY_FAKE_SOURCE} at runtime (task
 * p11-rotor-source-flip: {@code off} IS the shipped default — grid-fed via doInject with
 * the full upstream :815 semantics, the RU/KU machines fed by the /gt6energy source rig;
 * {@code on} re-arms the retired A-tier seam with the :815 alternating arm suspended).
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTMachineCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The acceptance feed size (the oven command default). */
	private static final int DEFAULT_INPUT_COUNT = 8;

	/** The GT6 side the rig injects through (doInject ignores the side — the :511 mask is the side-gated IO pool item). */
	private static final byte INJECT_SIDE = 2;

	private GTMachineCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> tMachine = Commands.literal("gt6machine")
			.requires(source -> source.hasPermission(2))
			.then(fakesource());
		// task p8-machine-tiers-doinject ⑤(a): the t2/t3/t4 selector variants — one literal
		// per registered block, the T1 feeds reused per family (same recipe chains).
		tMachine.then(machine("shredder", GTMachines.SHREDDER, () -> Items.COBBLESTONE)) // Loader_Recipes_Vanilla.java:692
			.then(machine("shredder_t2", GTMachines.SHREDDER_T2, () -> Items.COBBLESTONE))
			.then(machine("shredder_t3", GTMachines.SHREDDER_T3, () -> Items.COBBLESTONE))
			.then(machine("shredder_t4", GTMachines.SHREDDER_T4, () -> Items.COBBLESTONE))
			.then(machine("crusher", GTMachines.CRUSHER, GTMachineCommand::firstGemChainGem)) // Loader_Recipes_Handlers.java:72
			.then(machine("crusher_t2", GTMachines.CRUSHER_T2, GTMachineCommand::firstGemChainGem))
			.then(machine("crusher_t3", GTMachines.CRUSHER_T3, GTMachineCommand::firstGemChainGem))
			.then(machine("crusher_t4", GTMachines.CRUSHER_T4, GTMachineCommand::firstGemChainGem))
			.then(machine("lathe", GTMachines.LATHE, () -> net.minecraft.world.level.block.Blocks.STONE.asItem())) // Loader_Recipes_Vanilla.java:524
			.then(machine("lathe_t2", GTMachines.LATHE_T2, () -> net.minecraft.world.level.block.Blocks.STONE.asItem()))
			.then(machine("lathe_t3", GTMachines.LATHE_T3, () -> net.minecraft.world.level.block.Blocks.STONE.asItem()))
			.then(machine("lathe_t4", GTMachines.LATHE_T4, () -> net.minecraft.world.level.block.Blocks.STONE.asItem()));
		event.getDispatcher().register(tMachine);
		LOGGER.info("Registered GT6 machine acceptance command /gt6machine (shredder|crusher|lathe x t1..t4 | fakesource x place|input|run|inject|check)");
		// the p8 ladder registration line (the runServer gate asserts it): the three family
		// BETs resolve — proof the RegistryObjects bound.
		LOGGER.info("GT6 machine ladder registered: 12 blocks / 3 family BETs (T1-T4 validBlocks multi-attach), tiers "
			+ java.util.Arrays.deepToString(GTMachines.TIER_INPUTS) + " crusher parallel " + java.util.Arrays.toString(GTMachines.CRUSHER_PARALLEL));
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
		// task p8-machine-tiers-doinject ⑤(b): the FALSE-regime rig — inject+consume pairs.
		// finalSize = the OPTIONAL half-cycle pair: iteration ticks-1 runs at <finalSize>
		// instead of <size>, so a whole KU pulse cycle (positive train to completion + the
		// negative transition pair) lands inside ONE command — across RCON calls the idle
		// ticks reset the parked progress through doInactive's CONSTANT_ENERGY :894 (the
		// D3 wire-chain lesson, now proven for the machines too).
		tMachine.then(Commands.literal("inject")
			.then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20000))
				.executes(context -> inject(context.getSource(),
						com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"), null, null, null))
				.then(Commands.argument("size", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-4096, 4096))
					.executes(context -> inject(context.getSource(),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "size"), null, null))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> inject(context.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "size"), null,
								BlockPosArgument.getLoadedBlockPos(context, "pos"))))
					.then(Commands.argument("finalSize", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-4096, 4096))
						.executes(context -> inject(context.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "size"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "finalSize"), null))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(context -> inject(context.getSource(),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "size"),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "finalSize"),
									BlockPosArgument.getLoadedBlockPos(context, "pos"))))))));
		tMachine.then(Commands.literal("check")
			.executes(context -> check(context.getSource(), null))
			.then(Commands.argument("pos", BlockPosArgument.blockPos())
				.executes(context -> check(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		return tMachine;
	}

	/**
	 * The crusher acceptance feed: the first material of the gem-chain registration order
	 * that survives ALL THREE upstream gates — the both-side mat() resolution of :72
	 * (:209/:214, a gem material without a registered gemFlawed item has its row skipped in
	 * the pour), the poured row itself, and the canOutput chain-processing power cap
	 * :626-629 (a row with duration × mEUt × 4 > mInputMax × 600 would be bound BELOW 4
	 * parallel — e.g. zirconium, duration 1024 — and the feed would not demonstrate the
	 * 4-parallel one-cycle acceptance). All three gates read the LIVE poured map.
	 */
	private static Item firstGemChainGem() {
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() != OP.gem) continue;
			RegistryObject<Item> tIn = GTMaterialItems.get(OP.gem, tPair.material());
			RegistryObject<Item> tOut = GTMaterialItems.get(OP.gemFlawed, tPair.material());
			if (tIn == null || !tIn.isPresent() || tOut == null || !tOut.isPresent()) continue;
			// the T1 crusher shape: mEUt, mParallel 4, mInputMax 64 — the cap :628 must leave 4 intact
			gregtech6.recipes.Recipe tRecipe = crusherRowFor(tIn.get());
			if (tRecipe != null && tRecipe.mEUt * tRecipe.mDuration * 4 <= 64L * 600) return tIn.get();
			LOGGER.info("GT6 machine feed: gem material {} skipped (no poured :72 row or the :626-629 cap binds below 4 parallel)", tPair.material().mNameInternal);
		}
		throw new IllegalStateException("No gt6:gem→gemFlawed pair with a cap-safe :72 row resolved for the crusher feed");
	}

	/** The poured :72 row for a gem item, or null (the row carries input gem x1). */
	private static gregtech6.recipes.Recipe crusherRowFor(net.minecraft.world.item.Item aGemItem) {
		for (gregtech6.recipes.Recipe tRecipe : gregtech6.recipes.GT6RecipeMaps.CRUSHER.mRecipeList) {
			if (tRecipe.mInputs.length == 1 && tRecipe.mInputs[0].getItem() == aGemItem && tRecipe.mInputs[0].getCount() == 1) return tRecipe;
		}
		return null;
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
		String tOutputs = outputList(tMachine);
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

	/**
	 * The FALSE-regime rig (task p8-machine-tiers-doinject ⑤(b)): {@code ticks} iterations
	 * of one direct doInject (size defaults to the machine's mInputMax; negative = the AC
	 * half-cycle; {@code finalSize} = the optional LAST iteration's size, the negative
	 * transition pair) + one dispatcher tick per iteration, so each iteration IS one
	 * injection tick and a full KU pulse cycle closes inside one command.
	 */
	private static int inject(CommandSourceStack source, int ticks, Integer size, Integer finalSize, BlockPos pos) {
		TileEntityBasicMachine tMachine = machineAt(source, pos);
		if (tMachine == null) {
			source.sendFailure(Component.literal("No TileEntityBasicMachine at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		long tSize = size != null ? size : tMachine.mInputMax;
		long tFinalSize = finalSize != null ? finalSize : tSize;
		if (tSize == 0 || tFinalSize == 0) {
			source.sendFailure(Component.literal("inject refused: size 0 is gated by the Root :717 aSize != 0 rule (the verbatim :503 math divides by the size)"));
			return 0;
		}
		long tUsed = 0;
		for (int i = 0; i < ticks; i++) {
			long tIterationSize = (finalSize != null && i == ticks - 1) ? tFinalSize : tSize;
			tUsed += tMachine.doInject(tMachine.mEnergyTypeAccepted, INJECT_SIDE, tIterationSize, 1, true);
			tMachine.updateEntity();
		}
		String tOutputs = outputList(tMachine);
		String tReport = String.format(
			"GT6 %s inject ticks=%d size=%d finalSize=%s used=%d progress=%d/%d energy=%d state=new:%s/old:%s outputs=[%s] at %s",
			tMachine.getTileEntityName(), ticks, tSize, finalSize, tUsed,
			tMachine.mProgress, tMachine.mMaxProgress, tMachine.mEnergy,
			tMachine.mStateNew, tMachine.mStateOld, tOutputs, tMachine.getBlockPos().toShortString());
		source.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The output-slot listing shared by run/inject/check. */
	private static String outputList(TileEntityBasicMachine aMachine) {
		StringBuilder tOutputs = new StringBuilder();
		for (int i = 0; i < aMachine.getOutputSlotCount(); i++) {
			ItemStack tStack = aMachine.getInventory().getStackInSlot(aMachine.getInputSlotCount() + i);
			if (!tStack.isEmpty()) tOutputs.append(tStack.getCount()).append("x ").append(tStack.getItem()).append("; ");
		}
		return tOutputs.toString();
	}

	/** The regime switch (task p8-machine-tiers-doinject ⑤(c); default flipped by p11-rotor-source-flip): on|off|stat over ENERGY_FAKE_SOURCE. */
	private static LiteralArgumentBuilder<CommandSourceStack> fakesource() {
		return Commands.literal("fakesource")
			.then(Commands.literal("on").executes(context -> setFakeSource(context.getSource(), true)))
			.then(Commands.literal("off").executes(context -> setFakeSource(context.getSource(), false)))
			.then(Commands.literal("stat").executes(context -> {
				boolean tOn = TileEntityBasicMachine.ENERGY_FAKE_SOURCE;
				String tReport = "GT6 machine ENERGY_FAKE_SOURCE=" + tOn + (tOn
						? " (the RETIRED A-tier seam re-armed, the :815 alternating arm suspended)"
						: " (shipped default: grid-fed via doInject, the full upstream :815 semantics — feed the RU/KU machines with /gt6energy)");
				context.getSource().sendSuccess(() -> Component.literal(tReport), false);
				return Command.SINGLE_SUCCESS;
			}));
	}

	private static int setFakeSource(CommandSourceStack source, boolean aOn) {
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = aOn;
		String tReport = "GT6 machine ENERGY_FAKE_SOURCE set " + aOn;
		source.sendSuccess(() -> Component.literal(tReport), false);
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
			"GT6 %s at %s: %s data=%d progress=%d/%d energy=%d minenergy=%d minIn=%d recIn=%d maxIn=%d state=new:%s/old:%s stopped=%s active=%s running=%s parallel=%d parallelDuration=%s facing=%d",
			tMachine.getTileEntityName(), tMachine.getBlockPos().toShortString(), tSlots, tDataValue,
			tMachine.mProgress, tMachine.mMaxProgress, tMachine.mEnergy, tMachine.mMinEnergy,
			tMachine.mInputMin, tMachine.mInput, tMachine.mInputMax,
			tMachine.mStateNew, tMachine.mStateOld,
			tMachine.mStopped, tMachine.mActive, tMachine.mRunning, tMachine.mParallel, tMachine.mParallelDuration, tMachine.getFacing());
		source.sendSuccess(() -> Component.literal(tReport), false);
		return Command.SINGLE_SUCCESS;
	}
}
