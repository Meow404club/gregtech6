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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import gregtech6.item.GT6Circuits;

/**
 * {@code /gt6act} — the Advanced Crafting Table acceptance command (task p24-act-machine,
 * the C1 "RCON 全验证" face; the GTMachineCommand :96 shape, console-safe throughout).
 * The C1 card acceptance is explicitly GUI-free, so every BE surface is drivable here:
 *
 * <ul>
 * <li>{@code place [<pos>]} — setBlock a fresh ACT (FACING north);</li>
 * <li>{@code fill <slot> <item> <count> [<pos>]} — direct setStackInSlot over the real
 *     inventory (storage belts, grid real stock, tools); the holo pair 31/32 refuses
 *     through the handler {@code isItemValid} face — the command reports the refusal
 *     instead of bypassing it;</li>
 * <li>{@code selector <config> [<pos>]} — writes {@link GT6Circuits#selector} into slot
 *     30 (config 0/1/10+ refuse through the whitelist — the five-arm RCON probe);</li>
 * <li>{@code clear <slot> [<pos>]} — empties one slot (grid/selector teardown arms);</li>
 * <li>{@code compute [<pos>]} — {@link TileEntityAdvancedCraftingTable#getCraftingOutput}:
 *     the sweep + the pattern table + the recipe lookup, reporting the effective grid
 *     (G=ghost/R=real/.) and the slot-31 result;</li>
 * <li>{@code craft <once|cursor|shiftleft|shiftright> [<pos>]} — the four click modes
 *     over an array {@link TileEntityAdvancedCraftingTable.ICraftOutputSink} (the
 *     player-free seam; once/cursor use a 1-cell sink, the shift modes a 36-cell
 *     one), reporting the hold stacks;</li>
 * <li>{@code sort [<pos>]} — {@link TileEntityAdvancedCraftingTable#sortIntoTheInputSlots};</li>
 * <li>{@code mode <flush|belt16|belt36|filter16|filter36> <on|off> [<pos>]} — the
 *     mFlushMode/mBlocked16/36/mFilter16/36 fields (deviation ⑤: the tool-click arm
 *     rides the tool-system pool, the field flip is the RCON-verifiable seam);</li>
 * <li>{@code stat [<pos>]} — the flags, the mode table ({@code accessibleSlotsFromSide}),
 *     every stocked slot and the pattern backing.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTAdvancedCraftingTableCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTAdvancedCraftingTableCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> tAct = Commands.literal("gt6act")
				.requires(source -> source.hasPermission(2));
		tAct.then(Commands.literal("place")
				.executes(context -> place(context.getSource(), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> place(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		tAct.then(Commands.literal("fill")
				.then(Commands.argument("slot", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 70))
						.then(Commands.argument("item", net.minecraft.commands.arguments.ResourceLocationArgument.id())
								.then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64))
										.executes(context -> fill(context.getSource(),
												com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "slot"),
												net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "item"),
												com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"), null))
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
												.executes(context -> fill(context.getSource(),
														com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "slot"),
														net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "item"),
														com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"),
														BlockPosArgument.getLoadedBlockPos(context, "pos"))))))));
		tAct.then(Commands.literal("selector")
				.then(Commands.argument("config", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 255))
						.executes(context -> selector(context.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "config"), null))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(context -> selector(context.getSource(),
										com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "config"),
										BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
		tAct.then(Commands.literal("clear")
				.then(Commands.argument("slot", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 70))
						.executes(context -> clear(context.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "slot"), null))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(context -> clear(context.getSource(),
										com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "slot"),
										BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
		tAct.then(Commands.literal("compute")
				.executes(context -> compute(context.getSource(), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> compute(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		tAct.then(Commands.literal("craft")
				.then(Commands.argument("mode", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(context -> craft(context.getSource(),
								com.mojang.brigadier.arguments.StringArgumentType.getString(context, "mode"), null))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(context -> craft(context.getSource(),
										com.mojang.brigadier.arguments.StringArgumentType.getString(context, "mode"),
										BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
		tAct.then(Commands.literal("sort")
				.executes(context -> sort(context.getSource(), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> sort(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		tAct.then(Commands.literal("mode")
				.then(Commands.argument("which", com.mojang.brigadier.arguments.StringArgumentType.word())
						.then(Commands.argument("value", com.mojang.brigadier.arguments.StringArgumentType.word())
								.executes(context -> mode(context.getSource(),
										com.mojang.brigadier.arguments.StringArgumentType.getString(context, "which"),
										com.mojang.brigadier.arguments.StringArgumentType.getString(context, "value"), null))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(context -> mode(context.getSource(),
												com.mojang.brigadier.arguments.StringArgumentType.getString(context, "which"),
												com.mojang.brigadier.arguments.StringArgumentType.getString(context, "value"),
												BlockPosArgument.getLoadedBlockPos(context, "pos")))))));
		tAct.then(Commands.literal("open")
				.executes(context -> open(context.getSource(), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> open(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		tAct.then(Commands.literal("stat")
				.executes(context -> stat(context.getSource(), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> stat(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		event.getDispatcher().register(tAct);
		LOGGER.info("Registered GT6 ACT acceptance command /gt6act (place|fill|selector|clear|compute|craft|sort|mode|stat) — task p24-act-machine");
		LOGGER.info("GT6 ACT registered: 1 block / 1 BET (gt6:advanced_crafting_table, the single-variant row), 71 slots + the mPattern ghost backing, zero energy");
	}

	@javax.annotation.Nullable
	private static TileEntityAdvancedCraftingTable tableAt(CommandSourceStack aSource, @javax.annotation.Nullable BlockPos aPos) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos != null ? aPos : BlockPos.containing(aSource.getPosition()));
		return tBE instanceof TileEntityAdvancedCraftingTable tTable ? tTable : null;
	}

	private static int place(CommandSourceStack aSource, @javax.annotation.Nullable BlockPos aPos) {
		BlockPos tTarget = aPos != null ? aPos : BlockPos.containing(aSource.getPosition());
		aSource.getLevel().setBlock(tTarget, gregtech6.registry.GTMachines.ADVANCED_CRAFTING_TABLE.get().defaultBlockState(), 3);
		aSource.sendSuccess(() -> Component.literal("GT6 advanced_crafting_table placed at " + tTarget.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int fill(CommandSourceStack aSource, int aSlot, net.minecraft.resources.ResourceLocation aItemId, int aCount, @javax.annotation.Nullable BlockPos aPos) {
		TileEntityAdvancedCraftingTable tTable = tableAt(aSource, aPos);
		if (tTable == null) {
			aSource.sendFailure(Component.literal("No TileEntityAdvancedCraftingTable at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		net.minecraft.world.item.Item tItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(aItemId);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("Unknown item: " + aItemId));
			return 0;
		}
		ItemStack tStack = new ItemStack(tItem, aCount);
		if (!tTable.getInventory().isItemValid(aSlot, tStack)) {
			aSource.sendFailure(Component.literal("Slot " + aSlot + " REJECTED " + tStack.getItem() + " (the isItemValid face — holo pair / selector whitelist)"));
			return 0;
		}
		tTable.getInventory().setStackInSlot(aSlot, tStack);
		aSource.sendSuccess(() -> Component.literal("GT6 ACT fill slot " + aSlot + ": " + tStack.getCount() + "x " + tStack.getItem()), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int selector(CommandSourceStack aSource, int aConfig, @javax.annotation.Nullable BlockPos aPos) {
		TileEntityAdvancedCraftingTable tTable = tableAt(aSource, aPos);
		if (tTable == null) {
			aSource.sendFailure(Component.literal("No TileEntityAdvancedCraftingTable at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		ItemStack tSelector = GT6Circuits.selector(aConfig);
		if (!tTable.getInventory().isItemValid(30, tSelector)) {
			aSource.sendFailure(Component.literal("Slot 30 REJECTED selector config " + aConfig + " (the whitelist band is [2, 9])"));
			return 0;
		}
		tTable.getInventory().setStackInSlot(30, tSelector);
		aSource.sendSuccess(() -> Component.literal("GT6 ACT selector: config " + aConfig + " into slot 30"), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int clear(CommandSourceStack aSource, int aSlot, @javax.annotation.Nullable BlockPos aPos) {
		TileEntityAdvancedCraftingTable tTable = tableAt(aSource, aPos);
		if (tTable == null) {
			aSource.sendFailure(Component.literal("No TileEntityAdvancedCraftingTable at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		tTable.getInventory().setStackInSlot(aSlot, ItemStack.EMPTY);
		aSource.sendSuccess(() -> Component.literal("GT6 ACT clear slot " + aSlot), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int compute(CommandSourceStack aSource, @javax.annotation.Nullable BlockPos aPos) {
		TileEntityAdvancedCraftingTable tTable = tableAt(aSource, aPos);
		if (tTable == null) {
			aSource.sendFailure(Component.literal("No TileEntityAdvancedCraftingTable at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		ItemStack tOutput = tTable.getCraftingOutput(true);
		String tGrid = gridReport(tTable);
		String tLine = "GT6 ACT compute at " + tTable.getBlockPos().toShortString() + ": grid=[" + tGrid + "], output=" + stackText(tTable.getInventory().getStackInSlot(31))
				+ ", canDo=" + tTable.canDoCraftingOutput() + ", pattern=" + patternReport(tTable);
		if (!tOutput.isEmpty()) aSource.sendSuccess(() -> Component.literal(tLine), false);
		else aSource.sendSuccess(() -> Component.literal(tLine + " (no recipe match)"), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int craft(CommandSourceStack aSource, String aMode, @javax.annotation.Nullable BlockPos aPos) {
		TileEntityAdvancedCraftingTable tTable = tableAt(aSource, aPos);
		if (tTable == null) {
			aSource.sendFailure(Component.literal("No TileEntityAdvancedCraftingTable at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		switch (aMode.toLowerCase(java.util.Locale.ROOT)) {
			case "once" -> {
				TileEntityAdvancedCraftingTable.ICraftOutputSink tSink = arraySink(1);
				boolean tCrafted = tTable.craftOnce(tSink, 0);
				String tLine = "GT6 ACT craft once: crafted=" + tCrafted + ", hold=[" + stackText(tSink.getHold(0)) + "]";
				aSource.sendSuccess(() -> Component.literal(tLine), false);
				LOGGER.info(tLine);
			}
			case "cursor" -> {
				TileEntityAdvancedCraftingTable.ICraftOutputSink tSink = arraySink(1);
				int tCrafts = tTable.craftFillCell(tSink, 0);
				String tLine = "GT6 ACT craft cursor: crafts=" + tCrafts + ", hold=[" + stackText(tSink.getHold(0)) + "]";
				aSource.sendSuccess(() -> Component.literal(tLine), false);
				LOGGER.info(tLine);
			}
			case "shiftleft", "shiftright" -> {
				TileEntityAdvancedCraftingTable.ICraftOutputSink tSink = arraySink(36);
				int tCrafts = tTable.craftTraverse(tSink, aMode.equalsIgnoreCase("shiftleft"));
				StringBuilder tHolds = new StringBuilder();
				for (int i = 0; i < 36; i++) if (!tSink.getHold(i).isEmpty()) tHolds.append(i).append("=").append(stackText(tSink.getHold(i))).append("; ");
				String tLine = "GT6 ACT craft " + aMode + ": crafts=" + tCrafts + ", holds=[" + tHolds + "]";
				aSource.sendSuccess(() -> Component.literal(tLine), false);
				LOGGER.info(tLine);
			}
			default -> {
				aSource.sendFailure(Component.literal("Unknown craft mode: " + aMode + " (once|cursor|shiftleft|shiftright)"));
				return 0;
			}
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int sort(CommandSourceStack aSource, @javax.annotation.Nullable BlockPos aPos) {
		TileEntityAdvancedCraftingTable tTable = tableAt(aSource, aPos);
		if (tTable == null) {
			aSource.sendFailure(Component.literal("No TileEntityAdvancedCraftingTable at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		tTable.sortIntoTheInputSlots();
		aSource.sendSuccess(() -> Component.literal("GT6 ACT sort done at " + tTable.getBlockPos().toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int mode(CommandSourceStack aSource, String aWhich, String aValue, @javax.annotation.Nullable BlockPos aPos) {
		TileEntityAdvancedCraftingTable tTable = tableAt(aSource, aPos);
		if (tTable == null) {
			aSource.sendFailure(Component.literal("No TileEntityAdvancedCraftingTable at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		boolean tValue = aValue.equalsIgnoreCase("on") || aValue.equalsIgnoreCase("true");
		Boolean tFlag = switch (aWhich.toLowerCase(java.util.Locale.ROOT)) {
			case "flush" -> tTable.mFlushMode = tValue;
			case "belt16" -> tTable.mBlocked16 = tValue;
			case "belt36" -> tTable.mBlocked36 = tValue;
			case "filter16" -> tTable.mFilter16 = tValue;
			case "filter36" -> tTable.mFilter36 = tValue;
			default -> null;
		};
		if (tFlag == null) {
			aSource.sendFailure(Component.literal("Unknown mode: " + aWhich + " (flush|belt16|belt36|filter16|filter36)"));
			return 0;
		}
		String tLine = "GT6 ACT mode " + aWhich + "=" + tValue + " at " + tTable.getBlockPos().toShortString();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The openGUI smoke arm (task p24-act-machine C2 — "runServer 冒烟 openGUI"): dispatches
	 * the ModularUI open chain for a FAKE player (the GTMachineCommand FakePlayerFactory
	 * shape) — the server half of the open (PosGuiData + the panel build + the sync-manager
	 * construct + the open packet dispatch) runs verbatim; a fake connection drops the
	 * client packet, which is the headless-legitimate verdict, a real exception is not.
	 */
	private static int open(CommandSourceStack aSource, @javax.annotation.Nullable BlockPos aPos) {
		TileEntityAdvancedCraftingTable tTable = tableAt(aSource, aPos);
		if (tTable == null) {
			aSource.sendFailure(Component.literal("No TileEntityAdvancedCraftingTable at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		net.minecraft.server.level.ServerPlayer tFake = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(aSource.getLevel());
		// the MUI open chain constructs the CLIENT screen behind the server half — a fake
		// player on a dedicated server cannot ride it (the LocalPlayer dist wall), so the
		// fake-player arm reports the sanctioned SKIP and the runClient visual check stays
		// the user task (the canner precedent); REAL players ride factory.open via the block use().
		if (net.minecraftforge.common.util.FakePlayer.class.isAssignableFrom(tFake.getClass())) {
			String tSkip = "GT6 ACT open SKIP for the fake player (the MUI open chain is client-boundary; use() rides factory.open for real players)";
			aSource.sendSuccess(() -> Component.literal(tSkip), false);
			LOGGER.info(tSkip);
			return Command.SINGLE_SUCCESS;
		}
		try {
			brachy.modularui.factory.BlockEntityUIFactory.INSTANCE.open(tFake, tTable);
			String tLine = "GT6 ACT open dispatched (server buildUI + sync construct OK)";
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
		} catch (Throwable tOpenFailure) {
			aSource.sendFailure(Component.literal("GT6 ACT open failed: " + tOpenFailure));
			return 0;
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int stat(CommandSourceStack aSource, @javax.annotation.Nullable BlockPos aPos) {
		TileEntityAdvancedCraftingTable tTable = tableAt(aSource, aPos);
		if (tTable == null) {
			aSource.sendFailure(Component.literal("No TileEntityAdvancedCraftingTable at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		StringBuilder tSlots = new StringBuilder();
		for (int i = 0; i < TileEntityAdvancedCraftingTable.INVENTORY_SIZE; i++) {
			ItemStack tStack = tTable.getInventory().getStackInSlot(i);
			if (!tStack.isEmpty()) tSlots.append(i).append("=").append(stackText(tStack)).append("; ");
		}
		StringBuilder tModes = new StringBuilder();
		for (int i : tTable.accessibleSlotsFromSide()) tModes.append(i).append(",");
		CompoundTag tDebug = new CompoundTag();
		tTable.saveAdditional(tDebug);
		String tLine = "GT6 ACT stat at " + tTable.getBlockPos().toShortString()
				+ ": flush=" + tTable.mFlushMode + " blocked16=" + tTable.mBlocked16 + " blocked36=" + tTable.mBlocked36
				+ " filter16=" + tTable.mFilter16 + " filter36=" + tTable.mFilter36
				+ " updatedGrid=" + tTable.mUpdatedGrid + " facing=" + tTable.getFacing()
				+ " accessible=[" + tModes + "]"
				+ " canDo=" + tTable.canDoCraftingOutput() + " output=" + stackText(tTable.getInventory().getStackInSlot(31))
				+ " pattern=[" + patternReport(tTable) + "]"
				+ " slots=[" + tSlots + "]"
				+ " nbtKeys=" + tDebug.getAllKeys();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** A simple growable array sink over {@code cellCount} cells (the /gt6act drive face of the seam). */
	private static TileEntityAdvancedCraftingTable.ICraftOutputSink arraySink(int aCells) {
		ItemStack[] tCells = new ItemStack[aCells];
		for (int i = 0; i < aCells; i++) tCells[i] = ItemStack.EMPTY;
		return new TileEntityAdvancedCraftingTable.ICraftOutputSink() {
			@Override public int cellCount() {
				return aCells;
			}

			@Override public ItemStack getHold(int aCell) {
				return tCells[aCell];
			}

			@Override public void setHold(int aCell, ItemStack aHold) {
				tCells[aCell] = aHold;
			}
		};
	}

	/** The 3x3 report: G=ghost cell, R=real stock, g=ghost over real (impossible by construction), .=empty. */
	private static String gridReport(TileEntityAdvancedCraftingTable aTable) {
		StringBuilder rGrid = new StringBuilder();
		for (int i = 0; i < 9; i++) {
			ItemStack tGhost = aTable.mPattern[i];
			ItemStack tReal = aTable.getInventory().getStackInSlot(TileEntityAdvancedCraftingTable.SLOTS_CRAFTING[i]);
			char tMark = tGhost != null && !tGhost.isEmpty() ? 'G' : !tReal.isEmpty() ? 'R' : '.';
			rGrid.append(tMark);
		}
		return rGrid.toString();
	}

	private static String patternReport(TileEntityAdvancedCraftingTable aTable) {
		StringBuilder rPattern = new StringBuilder();
		for (int i = 0; i < 9; i++) {
			ItemStack tGhost = aTable.mPattern[i];
			rPattern.append(i == 0 ? "" : ",").append(tGhost == null || tGhost.isEmpty() ? "-" : stackText(tGhost));
		}
		return rPattern.toString();
	}

	private static String stackText(ItemStack aStack) {
		return aStack == null || aStack.isEmpty() ? "empty" : aStack.getCount() + "x " + aStack.getItem();
	}
}
