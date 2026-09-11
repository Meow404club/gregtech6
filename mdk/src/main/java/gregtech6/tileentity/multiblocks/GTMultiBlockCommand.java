package gregtech6.tileentity.multiblocks;

import com.mojang.brigadier.Command;
import javax.annotation.Nullable;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.registry.GTMultiBlocks;

/**
 * {@code /gt6multiblock} — the multiblock acceptance command (task p4-multiblock-framework ②,
 * RCON-drivable like /gt6oven). Console-safe throughout.
 *
 * <ul>
 * <li>{@code place <pos>} — setBlock a fresh Coke Oven controller (FACING north → the
 *     structure core sits one cell south, the getOffsetXN "behind the facing" arithmetic);</li>
 * <li>{@code frame <pos>} — place all 26 brick cells around the computed centre (occupied
 *     cells keep their content);</li>
 * <li>{@code hole <pos>} — break ONE part cell with the breakBlock propagation (upstream
 *     MultiTileEntityMultiBlockPart :176-184), the command-side stand-in for a player break;</li>
 * <li>{@code wand <pos>} — the builder-wand simulation (task card ③): a FakePlayer inventory
 *     stocked with 26 bricks, then the upstream onToolClick2 sequence :141-146 verbatim —
 *     {@code checkStructure2(controllerPos, player=null, fakeInventory)} (the consume path:
 *     null player auto-approves the canEdit chain and ST.use-equivalent shrinks the stock)
 *     followed by the linking {@code checkStructure(true)} pass;</li>
 * <li>{@code form <pos> [stock]} — the SET scaffold trigger (task p16-form-scaffold): the
 *     source player (or the stocked fake player for console/RCON) completes a pattern-bound
 *     structure from inventory — the checker's SET walk places and consumes at the missing
 *     cells transactionally (short stock ⇒ nothing placed, nothing consumed), then the
 *     linking checkStructure(true) pass flips FORMED;</li>
 * <li>{@code check <pos>} — the magnifying glass (upstream onMagnifyingGlass :160-170):
 *     cheap-path check, forced recheck on failure, verdict + linked-part census;</li>
 * <li>{@code tick <pos> <ticks>} — drives the dispatcher manually (the same updateEntity the
 *     real ticker runs), exercising the onTickFirst forced check and the 600-tick poll;</li>
 * <li>{@code input <count> [item|prefix material] [pos]} — the p6/p7 acceptance feed:
 *     inserts through the gated item capability (slot 0 only); the default feed is
 *     gt6:gem_coal, the explicit {@code item} form covers the tag-path oak_log assertion,
 *     and the p7 {@code prefix material} form (case-insensitive internal names, e.g.
 *     {@code dust Oilshale}) drives the oil-shale rows through GTMaterialItems;</li>
 * <li>{@code ignite [pos]} — the TOOL_igniter branch (MultiTileEntityBasicMachine
 *     :373-379 → TileEntityBase10MultiBlockMachine.ignite());</li>
 * <li>{@code menu <pos>} — the GUI geometry/progress report (task p8-cokeoven-gui-menu ⑨):
 *     slot shapes, the player offset and the three-state progress, asserted through the
 *     static Host faces — no Menu instance (RCON has no Player);</li>
 * <li>{@code fluid <pos> [side] drain <mB>|fill <mB>} + {@code fluid <pos> stat} — the
 *     fluid-capability probe face (task p8-cokeoven-fluid-capability ⑥): drain walks the
 *     capability of the queried face (no side argument = the side-less query) and reports
 *     the drawn amount, fill is the always-zero acceptance probe (output-only), stat reports
 *     the tank content in the machine-report {@code tank=[...]} shape;</li>
 * <li>{@code check <pos>} additionally reports the processing state (progress/energy/
 *     ignited/tank/slots) since p6.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTMultiBlockCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The wand stock: the full 25-brick structure in one stack (27 cells = air centre + 25 bricks + the controller). */
	private static final int WAND_STOCK = 25;

	/**
	 * The default form stock (task p16-form-scaffold): the "give a stack" arm — the console
	 * fake player gets this many of EVERY distinct pattern part block; a 64 stack covers the
	 * 25-brick coke oven and leaves the shortfall visible on the negative arm.
	 */
	private static final int FORM_STOCK = 64;

	/** The boiler wand stocks (task p13-large-boiler): 9 transmitters + 25 walls (34 parts, the controller + the hollow excluded). */
	private static final int BOILER_WAND_TRANSMITTER_STOCK = 9;
	private static final int BOILER_WAND_WALL_STOCK = 25;

	private GTMultiBlockCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tMulti = Commands.literal("gt6multiblock")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("place")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))
					// task p27-cokeoven-facing-fix — the player-placement stand-in: VIEW is the
					// direction the (virtual) placer LOOKS, routed through the same
					// setFacingFromView mapping the real placement runs (the front lands
					// OPPOSITE the view, the structure behind it). The facing regression arm.
					.then(Commands.literal("view")
						.then(Commands.argument("view", com.mojang.brigadier.arguments.StringArgumentType.word())
							.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									parseView(com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "view"))))))))
			.then(Commands.literal("frame")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> frame(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("hole")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> hole(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("wand")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> wand(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("form")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> form(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), FORM_STOCK))
					.then(Commands.argument("stock", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 999))
						.executes(aContext -> form(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "stock"))))))
			.then(Commands.literal("check")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> check(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("input")
				.then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 999))
					.executes(aContext -> input(aContext.getSource(),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"), null, null, null, null))
					.then(Commands.argument("item", net.minecraft.commands.arguments.item.ItemArgument.item(aEvent.getBuildContext()))
						.executes(aContext -> input(aContext.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"),
								net.minecraft.commands.arguments.item.ItemArgument.getItem(aContext, "item"), null, null, null))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(aContext -> input(aContext.getSource(),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"),
									net.minecraft.commands.arguments.item.ItemArgument.getItem(aContext, "item"), null, null,
									BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
					.then(Commands.argument("prefix", com.mojang.brigadier.arguments.StringArgumentType.word())
						.then(Commands.argument("material", com.mojang.brigadier.arguments.StringArgumentType.word())
							.executes(aContext -> input(aContext.getSource(),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"), null,
									com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "prefix"),
									com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "material"), null))
							.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> input(aContext.getSource(),
										com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"), null,
										com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "prefix"),
										com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "material"),
										BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> input(aContext.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"),
								null, null, null, BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))))
			.then(Commands.literal("ignite")
				.executes(aContext -> ignite(aContext.getSource(), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> ignite(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("tick")
				.then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20000))
					.executes(aContext -> tick(aContext.getSource(),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "ticks"), null))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> tick(aContext.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "ticks"),
								BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))))
			.then(Commands.literal("crucible")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.literal("check")
						.executes(aContext -> crucibleCheck(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))
					.then(Commands.literal("stat")
						.executes(aContext -> crucibleStat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))
					.then(Commands.literal("feed")
						.then(Commands.argument("material", com.mojang.brigadier.arguments.StringArgumentType.word())
							.then(Commands.argument("units", com.mojang.brigadier.arguments.LongArgumentType.longArg(1))
								.executes(aContext -> crucibleFeed(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
										com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "material"),
										com.mojang.brigadier.arguments.LongArgumentType.getLong(aContext, "units"))))))
					.then(Commands.literal("heat")
						.then(Commands.argument("hu", com.mojang.brigadier.arguments.LongArgumentType.longArg(1))
							.executes(aContext -> crucibleHeat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									com.mojang.brigadier.arguments.LongArgumentType.getLong(aContext, "hu")))))
					.then(Commands.literal("pour")
						.then(Commands.argument("wallPos", BlockPosArgument.blockPos())
							.executes(aContext -> cruciblePour(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									BlockPosArgument.getLoadedBlockPos(aContext, "wallPos")))))))
			.then(Commands.literal("menu")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> menu(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("fluid")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.literal("stat")
						.executes(aContext -> fluidStat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))
					.then(Commands.literal("drain")
						.then(Commands.argument("mB", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
							.executes(aContext -> fluidDrain(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), null,
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "mB")))))
					.then(Commands.literal("fill")
						.then(Commands.argument("mB", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
							.executes(aContext -> fluidFill(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), null,
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "mB")))))
					.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
							.then(Commands.literal("drain")
								.then(Commands.argument("mB", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
									.executes(aContext -> fluidDrain(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
											parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "side")),
											com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "mB")))))
							.then(Commands.literal("fill")
								.then(Commands.argument("mB", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
									.executes(aContext -> fluidFill(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
											parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "side")),
											com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "mB"))))))));
		// task p13-large-boiler — the Large Boiler arm (append-only: the same place/wand/check
		// shape over the five variant rows; the existing CokeOven arms are untouched)
		tMulti.then(Commands.literal("boiler")
			.then(Commands.literal("place")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("variant", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> boilerPlace(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "variant"))))))
			.then(Commands.literal("frame")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("variant", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> boilerFrame(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "variant"))))))
			.then(Commands.literal("wand")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("variant", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> boilerWand(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "variant"))))))
			.then(Commands.literal("check")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> boilerCheck(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("stat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> boilerStat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("fill")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("mB", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
						.executes(aContext -> boilerFill(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "mB"))))))
			.then(Commands.literal("inject-hu")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 2000000000))
						.executes(aContext -> boilerInjectHu(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "amount"))))))
			.then(Commands.literal("dismantle")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> boilerDismantle(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("plunge")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> boilerPlunge(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))));
		aEvent.getDispatcher().register(tMulti);
		LOGGER.info("Registered GT6 multiblock acceptance command /gt6multiblock (place|frame|hole|wand|form|check|tick|input|ignite|menu|fluid|boiler place|frame|wand|check|stat|fill|inject-hu|dismantle|crucible check|stat|feed|heat|pour)");
	}

	private static TileEntityCokeOven ovenAt(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		BlockPos tTarget = aPos != null ? aPos : BlockPos.containing(aSource.getPosition());
		return tLevel.getBlockEntity(tTarget) instanceof TileEntityCokeOven tOven ? tOven : null;
	}

	/** The structure centre: one cell behind the facing (getOffsetXN/YN/ZN arithmetic). */
	private static BlockPos structureCenter(TileEntityCokeOven aOven) {
		return new BlockPos(aOven.getOffsetXN(aOven.mFacing), aOven.getOffsetYN(aOven.mFacing), aOven.getOffsetZN(aOven.mFacing));
	}

	private static int place(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		tLevel.setBlock(aPos, GTMultiBlocks.COKE_OVEN.get().defaultBlockState(), 3);
		aSource.sendSuccess(() -> Component.literal("GT6 coke oven controller placed at " + aPos.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The p27-cokeoven-facing-fix placement arm: the VIEW argument is the direction the
	 * (virtual) placer looks — the exact input {@link TileEntityBase10MultiBlockBase#setFacingFromPlacement}
	 * consumes. Routing through {@link TileEntityBase10MultiBlockBase#setFacingFromView}
	 * keeps the command byte-equivalent with a real player placement: the front lands
	 * OPPOSITE the view, the structure core behind it (away from the placer).
	 */
	private static int place(CommandSourceStack aSource, BlockPos aPos, @Nullable Direction aView) {
		ServerLevel tLevel = aSource.getLevel();
		tLevel.setBlock(aPos, GTMultiBlocks.COKE_OVEN.get().defaultBlockState(), 3);
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + aPos.toShortString()));
			return 0;
		}
		tOven.setFacingFromView(aView);
		aSource.sendSuccess(() -> Component.literal("GT6 coke oven controller placed at " + aPos.toShortString()
				+ " facing=" + Direction.from3DDataValue(tOven.mFacing).getName() + " (view " + aView.getName() + ")"), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The view-direction word (the parseSide form; byName covers the horizontal set the mapping consumes). */
	private static Direction parseView(String aWord) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Direction tView = Direction.byName(aWord.toLowerCase());
		if (tView == null) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Unknown side: " + aWord)).create();
		return tView;
	}

	/** Places the 25 brick cells of the 3x3x3 (the air centre and the controller cell excluded, occupied cells kept). */
	private static int frame(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + aPos.toShortString()));
			return 0;
		}
		ServerLevel tLevel = aSource.getLevel();
		BlockPos tCenter = structureCenter(tOven);
		BlockPos tController = tOven.getBlockPos();
		int[] tPlaced = {0};
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			BlockPos tCell = tCenter.offset(i, j, k);
			if (tCell.equals(tController)) continue; // the controller occupies one shell cell
			if (tLevel.getBlockState(tCell).isAir()) {
				tLevel.setBlock(tCell, GTMultiBlocks.COKE_OVEN_BRICKS.get().defaultBlockState(), 3);
				tPlaced[0]++;
			}
		}
		int tReported = tPlaced[0];
		aSource.sendSuccess(() -> Component.literal("GT6 coke oven frame: " + tReported + " bricks placed around " + tCenter.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	/** Breaks one part cell, running the breakBlock propagation the player-break hook runs. */
	private static int hole(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof MultiBlockPartBlockEntity tPart)) {
			aSource.sendFailure(Component.literal("No MultiBlockPartBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		ITileEntityMultiBlockController tTarget = tPart.getTarget(false); // upstream breakBlock :177
		tLevel.setBlock(aPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
		if (tTarget != null) {
			tPart.clearTarget();                 // :179-180
			tTarget.onStructureChange();         // :181
		}
		aSource.sendSuccess(() -> Component.literal("GT6 part broken at " + aPos.toShortString() + ", controller flagged: " + (tTarget != null)), false);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The builder-wand simulation: stocked FakePlayer inventory + the upstream onToolClick2
	 * builder-wand branch :141-146 (two calls: the placing checkStructure2 pass, then the
	 * linking checkStructure pass). The player argument stays null — the canEdit chain
	 * auto-approves non-players (UT.Entities.canEdit :3159) and the consume path shrinks the
	 * FakePlayer stock (ST.use :304-319 semantics).
	 */
	private static int wand(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + aPos.toShortString()));
			return 0;
		}
		net.minecraft.world.entity.player.Inventory tInventory = FakePlayerFactory.getMinecraft(aSource.getLevel()).getInventory();
		tInventory.items.set(0, new ItemStack(GTMultiBlocks.COKE_OVEN_BRICKS_ITEM.get(), WAND_STOCK));

		tOven.checkStructure2(tOven.getBlockPos(), null, tInventory); // the placing pass (:143)
		boolean tFormed = tOven.checkStructure(true);                 // the linking pass (:144)

		int tLeft = tInventory.items.get(0).getCount();
		String tReport = String.format("GT6 coke oven wand at %s: formed=%s, brick stock %d -> %d",
				tOven.getBlockPos().toShortString(), tFormed, WAND_STOCK, tLeft);
		if (!tFormed) {
			aSource.sendFailure(Component.literal("GT6 multiblock wand check FAILED: " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("GT6 multiblock wand check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * {@code form <pos> [stock]} — the SET scaffold trigger (task p16-form-scaffold, the ADR
	 * 2026-09-05-p16-formation-scoping ③): the three-piece path for pattern-bound controllers.
	 * The acting player rides {@code CommandSourceStack.getPlayer()} — a real player scaffolds
	 * from their own inventory (the {@code /give} path); console/RCON has no player, so the
	 * Minecraft fake player is stocked with {@code stock} (default {@value #FORM_STOCK}) of
	 * every distinct pattern part block (the wand arm's stocked-inventory recipe, plus a
	 * clearContent — the fake player outlives one command). Then the upstream onToolClick2
	 * shape: the checker's {@link GTMultiBlockStructureChecker#form} SET walk (the placing
	 * pass :143), followed by the linking {@code checkStructure(true)} pass (:144) that flips
	 * mStructureOkay and the FORMED blockstate.
	 *
	 * <p>Pattern-less controllers are refused — their hand-written check is the truth and the
	 * checker has nothing to scaffold from. The not-formed report goes through sendFailure
	 * WITHOUT the FAILED literal (the check arm's RCON contract): the negative arm asserts on
	 * {@code formed=false} and the unchanged stock pair instead.
	 */
	private static int form(CommandSourceStack aSource, BlockPos aPos, int aStock) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof TileEntityBase10MultiBlockBase tController)) {
			aSource.sendFailure(Component.literal("No multiblock controller at " + aPos.toShortString()));
			return 0;
		}
		GTMultiBlockPattern tPattern = tController.getStructurePattern();
		if (tPattern == null) {
			aSource.sendFailure(Component.literal("No declared structure pattern at " + aPos.toShortString()
					+ " (form scaffolds pattern-bound controllers only — the hand-written machines keep the wand arm)"));
			return 0;
		}
		ServerPlayer tPlayer = aSource.getPlayer();
		net.minecraft.world.entity.player.Inventory tInventory;
		if (tPlayer != null) {
			tInventory = tPlayer.getInventory(); // the real player spends their own stock
		} else {
			tInventory = FakePlayerFactory.getMinecraft(tLevel).getInventory();
			tInventory.clearContent(); // the fake player persists across passes — the stock is per-command
			java.util.LinkedHashSet<Block> tParts = new java.util.LinkedHashSet<>();
			for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) if (tCell.forms()) tParts.add(tCell.partBlock);
			int tSlot = 0;
			for (Block tPart : tParts) tInventory.items.set(tSlot++, new ItemStack(tPart, aStock));
		}
		int tBefore = countPartItems(tInventory, tPattern);

		GTMultiBlockStructureChecker.FormedVerdict tVerdict =
				GTMultiBlockStructureChecker.form(tController, tController.mFacing, tPlayer, tInventory); // the placing pass
		boolean tOkay = tController.checkStructure(true);                                                    // the linking pass
		int tAfter = countPartItems(tInventory, tPattern);

		String tReport = String.format("GT6 multiblock form at %s: formed=%s okay=%s stock %d -> %d first_failed_cell=%s",
				aPos.toShortString(), tVerdict.formed, tOkay, tBefore, tAfter, tVerdict.describeFirstFailure());
		if (!tVerdict.formed || !tOkay) {
			aSource.sendFailure(Component.literal(tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The part-item census — the consume path's own matcher, so the reported stock is what the scaffold can spend. Distinct part BLOCKS (26 forming cells of one brick type are one stock line, not 26). */
	private static int countPartItems(Container aInventory, GTMultiBlockPattern aPattern) {
		java.util.LinkedHashSet<Block> tParts = new java.util.LinkedHashSet<>();
		for (GTMultiBlockPattern.Cell tCell : aPattern.cells()) if (tCell.forms()) tParts.add(tCell.partBlock);
		int rCount = 0;
		for (Block tPart : tParts) {
			ItemStack tWanted = new ItemStack(tPart);
			for (int i = 0; i < aInventory.getContainerSize(); i++) {
				ItemStack tStack = aInventory.getItem(i);
				if (ItemStack.isSameItemSameTags(tWanted, tStack)) rCount += tStack.getCount();
			}
		}
		return rCount;
	}

	/**
	 * The magnifying glass (:160-170) + a linked-part census over the 26 cells. Since
	 * p16-pattern-checker the report ends with {@code first_failed_cell=} — the shared
	 * checker's first failed cell in declaration order (index, centre-relative offset,
	 * world cell, reason), or {@code none} when formed (the kTFRU CHECK-mode failedPos
	 * diagnostics, clean-room; the spec ④ surface).
	 */
	private static int check(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + aPos.toShortString()));
			return 0;
		}
		String tVerdict;
		if (tOven.checkStructure(false)) {
			tVerdict = "Structure is formed already!";
		} else {
			tVerdict = tOven.checkStructure(true) ? "Structure did form just now!" : "Structure did not form!";
		}
		int tLinked = 0;
		BlockPos tCenter = structureCenter(tOven);
		ServerLevel tLevel = aSource.getLevel();
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			BlockPos tCell = tCenter.offset(i, j, k);
			if (tLevel.getBlockEntity(tCell) instanceof MultiBlockPartBlockEntity tPart && tPart.getTarget(false) == tOven) tLinked++;
		}
		boolean tBlockFormed = tLevel.getBlockState(tOven.getBlockPos()).getValue(TileEntityBase10MultiBlockBase.FORMED);
		// the diagnostic walk (the same pattern the check just consumed — idempotent binding)
		GTMultiBlockStructureChecker.FormedVerdict tDiagnosis =
				GTMultiBlockStructureChecker.check(tOven, tOven.mFacing, null, null, null);
		String tReport = String.format("GT6 coke oven at %s: %s okay=%s block_formed=%s linked_parts=%d/25 first_failed_cell=%s",
				tOven.getBlockPos().toShortString(), tVerdict, tOven.mStructureOkay, tBlockFormed, tLinked,
				tDiagnosis.describeFirstFailure());
		String tMachine = machineReport(tOven);
		if (!tOven.mStructureOkay || !tBlockFormed) {
			aSource.sendFailure(Component.literal(tReport + " | " + tMachine));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		aSource.sendSuccess(() -> Component.literal(tMachine), false);
		LOGGER.info(tReport + " | " + tMachine);
		return Command.SINGLE_SUCCESS;
	}

	/** The processing-state report (task p6 acceptance: progress/energy/ignited/tank/slots). */
	private static String machineReport(TileEntityCokeOven aOven) {
		StringBuilder rSlots = new StringBuilder();
		for (int i = 0; i < aOven.INVENTORY_SIZE; i++) {
			ItemStack tStack = aOven.slot(i);
			if (tStack.isEmpty()) continue;
			if (rSlots.length() > 0) rSlots.append(", ");
			rSlots.append(i).append("=").append(tStack.getCount()).append("x ")
					.append(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tStack.getItem()));
		}
		String tTank = aOven.mTanksOutput[0].isEmpty()
				? "-"
				: aOven.mTanksOutput[0].amount() + "mB "
						+ net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(aOven.mTanksOutput[0].fluid().getFluid());
		return String.format("machine: progress=%d/%d energy=%d min_energy=%d ignited=%d active=%s running=%s stopped=%s tank=[%s] slots=[%s]",
				aOven.mProgress, aOven.mMaxProgress, aOven.mEnergy, aOven.mMinEnergy, aOven.mIgnited,
				aOven.mActive, aOven.mRunning, aOven.mStopped, tTank, rSlots.length() == 0 ? "-" : rSlots);
	}

	/**
	 * {@code input <count> [item|prefix material] [pos]} — fills the input slot through the
	 * gated item capability (the slot-0 insert gate, canInsertItem2 :549-554). The default
	 * feed is gt6:gem_coal (GTMaterialItems.get(OP.gem, MT.Coal), the p6 card ruling); an
	 * explicit {@code item} argument covers the tag-path acceptance (minecraft:oak_log), and
	 * the p7 {@code prefix material} form resolves the GT material universe (case-insensitive
	 * internal names via {@link #findPrefix}/{@link #findMaterial}, e.g. {@code dust OilShale})
	 * so the oil-shale rows are drivable without hand-naming ids. Since p8 the resolution
	 * falls back to {@code GTMaterialBlocks.get} when the item path misses, so block items
	 * ({@code input blockIngot Coal}) feed the oven too.
	 */
	private static int input(CommandSourceStack aSource, int aCount,
			@Nullable net.minecraft.commands.arguments.item.ItemInput aItem,
			@Nullable String aPrefixName, @Nullable String aMaterialName, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		ItemStack tStack;
		if (aItem != null) {
			try {
				tStack = new ItemStack(aItem.getItem(), aCount);
			} catch (Exception e) {
				aSource.sendFailure(Component.literal("Cannot resolve item: " + e));
				return 0;
			}
		} else if (aPrefixName != null && aMaterialName != null) {
			gregapi.oredict.OreDictPrefix tPrefix = findPrefix(aPrefixName);
			gregapi.oredict.OreDictMaterial tMaterial = findMaterial(aMaterialName);
			if (tPrefix == null || tMaterial == null) {
				aSource.sendFailure(Component.literal("Cannot resolve GT material pair: " + aPrefixName + " " + aMaterialName));
				return 0;
			}
			net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tHandle =
					gregtech6.registry.GTMaterialItems.get(tPrefix, tMaterial);
			//? if forge {
			if (tHandle == null || !tHandle.isPresent()) tHandle = gregtech6.registry.GTMaterialBlocks.get(tPrefix, tMaterial); // p8: block items (e.g. blockIngot Coal)
			//?} else {
			/*if (tHandle == null || !tHandle.isBound()) tHandle = gregtech6.registry.GTMaterialBlocks.get(tPrefix, tMaterial); // p8: block items
			 *///?}
			//? if forge {
			if (tHandle == null || !tHandle.isPresent()) {
			//?} else {
			/*if (tHandle == null || !tHandle.isBound()) {
			 *///?}
				aSource.sendFailure(Component.literal("No gt6 item for prefix '" + tPrefix.mNameInternal + "' + material '" + tMaterial.mNameInternal + "'"));
				return 0;
			}
			tStack = new ItemStack(tHandle.get(), aCount);
		} else {
			net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tHandle =
					gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.gem, gregapi.data.MT.Coal);
			//? if forge {
			if (tHandle == null || !tHandle.isPresent()) {
			//?} else {
			/*if (tHandle == null || !tHandle.isBound()) {
			 *///?}
				aSource.sendFailure(Component.literal("gt6:gem_coal is not registered"));
				return 0;
			}
			tStack = new ItemStack(tHandle.get(), aCount);
		}
		//? if forge {
		ItemStack tLeftover = tOven.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, null)
				.map(tHandler -> tHandler.insertItem(0, tStack, false))
				.orElse(tStack);
		//?} else {
		/*net.neoforged.neoforge.items.IItemHandler tItemDoor = tOven.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, tOven.getBlockPos(), null);
		ItemStack tLeftover = tItemDoor == null ? tStack : tItemDoor.insertItem(0, tStack, false);
		 *///?}
		int tInserted = aCount - tLeftover.getCount();
		String tReport = String.format("GT6 coke oven input %d %s at %s: inserted %d%s", aCount,
				net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tStack.getItem()),
				tOven.getBlockPos().toShortString(), tInserted, tLeftover.isEmpty() ? "" : ", leftover " + tLeftover.getCount());
		if (tInserted <= 0) {
			aSource.sendFailure(Component.literal(tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The case-insensitive internal-name lookup over the registered prefixes (RCON-friendly). */
	@Nullable
	private static gregapi.oredict.OreDictPrefix findPrefix(String aName) {
		for (gregapi.oredict.OreDictPrefix tPrefix : gregapi.oredict.OreDictPrefix.VALUES) {
			if (tPrefix.mNameInternal.equalsIgnoreCase(aName)) return tPrefix;
		}
		return null;
	}

	/**
	 * The case-insensitive internal-name lookup over the registered materials, merged onto the
	 * registration target (MaterialRegistry.get alias resolution). byName alone is exact-match
	 * AND can land on an id -1 auto-invalid placeholder shadowing the real name — e.g. the
	 * oredict name "Oil Shale" gives mNameInternal "OilShale" (item gt6:dust_oil_shale) while
	 * "Oilshale" sits in the map as an mID -1 husk — so the scan requires mID >= 0.
	 */
	@Nullable
	private static gregapi.oredict.OreDictMaterial findMaterial(String aName) {
		gregapi.oredict.OreDictMaterial tMaterial = gregapi.oredict.MaterialRegistry.INSTANCE.byName(aName);
		if (tMaterial == null) tMaterial = gregapi.oredict.MaterialRegistry.INSTANCE.byName(gregapi.oredict.MaterialRegistry.sanitize(aName));
		if (tMaterial != null && tMaterial.mID >= 0) return gregapi.oredict.MaterialRegistry.INSTANCE.get(tMaterial);
		for (gregapi.oredict.OreDictMaterial tCandidate : gregapi.oredict.OreDictMaterial.MATERIAL_MAP.values()) {
			if (tCandidate.mID >= 0 && tCandidate.mNameInternal.equalsIgnoreCase(aName)) {
				return gregapi.oredict.MaterialRegistry.INSTANCE.get(tCandidate);
			}
		}
		return null;
	}

	// ------------------------- the crucible chain (task p26-crucible-multiblock) -------------------------

	/** The crucible resolution arm of the acceptance chain — null with a failure message when absent. */
	@Nullable
	private static TileEntityCrucible crucibleAt(CommandSourceStack aSource, @Nullable BlockPos aPos) {
		if (aPos == null) return null;
		if (aSource.getLevel().getBlockEntity(aPos) instanceof TileEntityCrucible tCrucible) return tCrucible;
		return null;
	}

	/** {@code crucible <pos> check} — the structure verdict + the linked-part census over the 3x3x3 box. */
	private static int crucibleCheck(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCrucible tCrucible = crucibleAt(aSource, aPos);
		if (tCrucible == null) {
			aSource.sendFailure(Component.literal("No TileEntityCrucible at " + aPos.toShortString()));
			return 0;
		}
		String tVerdict;
		if (tCrucible.checkStructure(false)) {
			tVerdict = "Structure is formed already!";
		} else {
			tVerdict = tCrucible.checkStructure(true) ? "Structure did form just now!" : "Structure did not form!";
		}
		int tLinked = 0;
		ServerLevel tLevel = aSource.getLevel();
		BlockPos tBase = tCrucible.getBlockPos();
		for (int i = -1; i <= 1; i++) for (int j = 0; j <= 2; j++) for (int k = -1; k <= 1; k++) {
			BlockPos tCell = tBase.offset(i, j, k);
			if (tLevel.getBlockEntity(tCell) instanceof MultiBlockPartBlockEntity tPart && tPart.getTarget(false) == tCrucible) tLinked++;
		}
		String tReport = String.format("GT6 crucible at %s: %s okay=%s linked_parts=%d/24",
				tBase.toShortString(), tVerdict, tCrucible.mStructureOkay, tLinked);
		if (!tCrucible.mStructureOkay) {
			aSource.sendFailure(Component.literal(tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * {@code crucible <pos> feed <material> <units>} — the top-feed entry. Declared RCON
	 * equivalent of the upstream top-opening feed (the :204-234 slot/suck arm): the item
	 * → material-stack resolution (OM.anydata) has no port counterpart yet, so the RCON
	 * feeds raw material stacks (the chain's "top-feed ore" step at material granularity).
	 */
	private static int crucibleFeed(CommandSourceStack aSource, BlockPos aPos, String aMaterialName, long aUnits) {
		TileEntityCrucible tCrucible = crucibleAt(aSource, aPos);
		if (tCrucible == null) {
			aSource.sendFailure(Component.literal("No TileEntityCrucible at " + aPos.toShortString()));
			return 0;
		}
		gregapi.oredict.OreDictMaterial tMaterial = findMaterial(aMaterialName);
		if (tMaterial == null) {
			aSource.sendFailure(Component.literal("Cannot resolve GT material: " + aMaterialName));
			return 0;
		}
		java.util.List<gregapi.oredict.OreDictMaterialStack> tFeed = new java.util.ArrayList<>();
		// the argument counts MATERIAL UNITS — the stack mAmount rides the raw CS.U scale
		// (upstream :218 mTargetCrushing.mAmount semantics; the offline fixtures build
		// N * CS.U the same way). A bare aUnits would be ~4e-9 U of dust, not 4U of metal.
		tFeed.add(new gregapi.oredict.OreDictMaterialStack(tMaterial, aUnits * gregapi.data.CS.U));
		boolean tFed = tCrucible.addMaterialStacks(tFeed, tCrucible.envTemperature());
		String tReport = String.format("GT6 crucible feed %d %s at %s: fed=%s total=%d temp=%dK",
				aUnits, tMaterial.mNameInternal, tCrucible.getBlockPos().toShortString(), tFed,
				tCrucible.totalContent(), tCrucible.mTemperature);
		if (!tFed) {
			aSource.sendFailure(Component.literal(tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** {@code crucible <pos> heat <hu>} — the HU buffer charge entry (the burning-box feed probe). */
	private static int crucibleHeat(CommandSourceStack aSource, BlockPos aPos, long aHU) {
		TileEntityCrucible tCrucible = crucibleAt(aSource, aPos);
		if (tCrucible == null) {
			aSource.sendFailure(Component.literal("No TileEntityCrucible at " + aPos.toShortString()));
			return 0;
		}
		tCrucible.mEnergy += aHU;
		tCrucible.setChanged();
		String tReport = String.format("GT6 crucible heat +%d HU at %s: buffer=%d temp=%dK max=%dK",
				aHU, tCrucible.getBlockPos().toShortString(), tCrucible.mEnergy,
				tCrucible.mTemperature, tCrucible.getTemperatureMax((byte)0));
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** {@code crucible <pos> stat} — the thermometer + content census. */
	private static int crucibleStat(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCrucible tCrucible = crucibleAt(aSource, aPos);
		if (tCrucible == null) {
			aSource.sendFailure(Component.literal("No TileEntityCrucible at " + aPos.toShortString()));
			return 0;
		}
		StringBuilder tContent = new StringBuilder();
		for (gregapi.oredict.OreDictMaterialStack tStack : tCrucible.mContent) {
			if (tStack != null && tStack.mAmount > 0) {
				if (tContent.length() > 0) tContent.append(", ");
				tContent.append(tStack.mMaterial.mNameInternal).append(" ").append(tStack.mAmount / gregapi.data.CS.U100 / 100.0).append("U");
			}
		}
		String tReport = String.format("GT6 crucible at %s: temp=%dK max=%dK energy=%dHUs total=%d meltdown_warning=%s content=[%s]",
				tCrucible.getBlockPos().toShortString(), tCrucible.mTemperature, tCrucible.getTemperatureMax((byte)0),
				tCrucible.mEnergy, tCrucible.totalContent(), tCrucible.mMeltDown, tContent);
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	// ---------------------------------------------------------------------------
	// the through-wall pour probe (task p26-crucible-multiblock acceptance ④)
	// ---------------------------------------------------------------------------

	/**
	 * The recording pour sink of the {@code pour} probe — the offline suite's
	 * RecordingMold double, live-server form: takes at most one ingot ({@code U} units)
	 * per pour, never more (the real Mold contract, so the controller's stack walk ends).
	 * The physical Mold BE is the A-card min-face file — this probe proves the LIVE
	 * through-wall half (the y+1 wall relay → the controller pour → the amount
	 * subtraction); the block-level "click the mold, take the ingot" half composes at
	 * the A→C merge (A's row0 chain pours gt6:ingot_tin through this same seam).
	 */
	private static final class ProbeMold implements gregapi.tileentity.machines.ITileEntityMold {
		long mDemand = gregapi.data.CS.U;
		gregapi.oredict.OreDictMaterial mMaterial;
		long mTaken = -1;
		@Override public boolean isMoldInputSide(byte aSide) { return true; }
		@Override public long getMoldMaxTemperature() { return 3000; } // above the Steel ceiling — never refuses a pour
		@Override public long getMoldRequiredMaterialUnits() { return gregapi.data.CS.U; }
		@Override
		public long fillMold(gregapi.oredict.OreDictMaterialStack aMaterial, long aTemperature, byte aSide) {
			mMaterial = aMaterial.mMaterial;
			mTaken = Math.min(mDemand, aMaterial.mAmount);
			return mTaken;
		}
	}

	/**
	 * {@code crucible <pos> pour <wallPos>} — the through-wall mold-pour probe: the
	 * recording mold clicks the WALL block entity (the ITileEntityCrucible relay face —
	 * only the y+1 ONLY_CRUCIBLE ring answers, the NO_CRUCIBLE mode gate :688), the wall
	 * forwards controller-ward (getTarget(true)) and the controller pours its molten
	 * self-smelted content into the sink (TileEntityCrucible.fillMoldAtSide, upstream
	 * :547-556). The report carries the poured amount in U and the content total — the
	 * melt verdict (a cold or solid content answers poured=0, the RCON negative arm).
	 */
	private static int cruciblePour(CommandSourceStack aSource, BlockPos aPos, BlockPos aWallPos) {
		TileEntityCrucible tCrucible = crucibleAt(aSource, aPos);
		if (tCrucible == null) {
			aSource.sendFailure(Component.literal("No TileEntityCrucible at " + aPos.toShortString()));
			return 0;
		}
		if (!(aSource.getLevel().getBlockEntity(aWallPos) instanceof gregapi.tileentity.machines.ITileEntityCrucible tWall)) {
			aSource.sendFailure(Component.literal("No crucible-relay wall (ITileEntityCrucible) at " + aWallPos.toShortString()));
			return 0;
		}
		ProbeMold tMold = new ProbeMold();
		boolean tPoured = tWall.fillMoldAtSide(tMold, (byte)2, (byte)2);
		String tReport = String.format("GT6 crucible pour at wall %s -> %s: poured=%s total=%d temp=%dK",
				aWallPos.toShortString(), tCrucible.getBlockPos().toShortString(),
				tPoured && tMold.mTaken > 0
						? String.format("%.1fU of %s", tMold.mTaken / (double)gregapi.data.CS.U, tMold.mMaterial.mNameInternal)
						: "0",
				tCrucible.totalContent(), tCrucible.mTemperature);
		if (!tPoured || tMold.mTaken <= 0) {
			aSource.sendFailure(Component.literal(tReport + " poured=0"));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** {@code ignite [pos]} — the TOOL_igniter branch (:373-379), the acceptance-chain ignition entry. */
	private static int ignite(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		tOven.ignite();
		String tReport = String.format("GT6 coke oven ignited at %s: requires_ignition=%s ignited=%d",
				tOven.getBlockPos().toShortString(), tOven.mRequiresIgnition, tOven.mIgnited);
		if (!tOven.mRequiresIgnition) {
			aSource.sendFailure(Component.literal(tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** Drives the 03 dispatcher manually — onTickFirst forced check + the 600-tick poll. */
	private static int tick(CommandSourceStack aSource, int aTicks, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		for (int i = 0; i < aTicks; i++) tOven.updateEntity();
		String tReport = String.format("GT6 coke oven ticked %d at %s: timer=%d okay=%s",
				aTicks, tOven.getBlockPos().toShortString(), tOven.getTimer(), tOven.mStructureOkay);
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * {@code menu <pos>} — the GUI geometry/progress report (task p8-cokeoven-gui-menu ⑨,
	 * the machine-report shape :257-273): content slot count, the input slot's menu position,
	 * the first/last output grid positions, the player-inventory offset and the total slot
	 * count, the three-state progress value and the GUI texture path. The assertions run
	 * through the STATIC faces — {@link GTBasicMachineMenu#progressValue(GTBasicMachineMenu.Host)}
	 * and {@link GTBasicMachineMenu#outputGridPos} over the BE's Host implementation — no Menu
	 * instance is constructed (RCON has no Player/Inventory).
	 */
	private static int menu(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + aPos.toShortString()));
			return 0;
		}
		GTBasicMachineMenu.Host tHost = tOven;
		int tOutputs = tHost.getOutputSlotCount();
		int tContentSlots = 1 + tOutputs; // 1 input + N outputs (the menu's addSlot sequence)
		int[] tOut0 = GTBasicMachineMenu.outputGridPos(0, tOutputs);
		int[] tOutLast = GTBasicMachineMenu.outputGridPos(tOutputs - 1, tOutputs);
		int tProgress = GTBasicMachineMenu.progressValue(tHost);
		// (53,25) = the input slot position, offset 84 = the standard machine-panel player bind,
		// 46 = 10 content + 36 player slots (GTBasicMachineMenu ctor + bindPlayerInventory(84))
		String tReport = String.format(
				"menu: content_slots=%d slot0=(%d,%d) out0=(%d,%d) out%d=(%d,%d) player_offset=%d total_slots=%d progress=%d texture=%s",
				tContentSlots, 53, 25, tOut0[0], tOut0[1], tOutputs - 1, tOutLast[0], tOutLast[1],
				84, tContentSlots + 36, tProgress, tHost.getGuiTexture());
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	// ---------------------------------------------------------------------------
	// the fluid-capability probe face (task p8-cokeoven-fluid-capability ⑥)
	// ---------------------------------------------------------------------------

	/** {@code fluid <pos> stat} — the tank content in the machine-report {@code tank=[...]} shape, plus the side-less capability view. */
	private static int fluidStat(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + aPos.toShortString()));
			return 0;
		}
		//? if forge {
		IFluidHandler tHandler = tOven.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
		//?} else {
		/*IFluidHandler tHandler = tOven.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tOven.getBlockPos(), null);
		 *///?}
		String tTank = tOven.mTanksOutput[0].isEmpty() ? "-" : tOven.mTanksOutput[0].amount() + "mB "
				+ net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(tOven.mTanksOutput[0].fluid().getFluid()); // the machineReport tank shape
		String tLine = String.format("GT6 coke oven fluid stat at %s: tank=[%s] cap_tanks=%d capacity=%d",
				tOven.getBlockPos().toShortString(), tTank, tHandler == null ? 0 : tHandler.getTanks(), tHandler == null ? 0 : tHandler.getTankCapacity(0));
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * {@code fluid <pos> [side] drain <mB>} — draws through the capability of the queried
	 * face (no side argument = the side-less query); the UP face reports 0 (REJECTED), the
	 * five other faces pass (the rotated mask 61). A 0-draw is an assertable outcome, not a
	 * command error (the GTBarrelCommand.draw precedent).
	 */
	private static int fluidDrain(CommandSourceStack aSource, BlockPos aPos, @Nullable Direction aSide, int aAmount) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + aPos.toShortString()));
			return 0;
		}
		//? if forge {
		IFluidHandler tHandler = tOven.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, aSide).orElse(null);
		//?} else {
		/*IFluidHandler tHandler = tOven.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tOven.getBlockPos(), aSide);
		 *///?}
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the coke oven exposes no FLUID_HANDLER on "
					+ (aSide == null ? "the side-less query" : aSide)));
			return 0;
		}
		FluidStack tDrained = tHandler.drain(aAmount, FluidAction.EXECUTE);
		int tGot = tDrained == null ? 0 : tDrained.getAmount();
		String tFluid = tDrained == null || tDrained.isEmpty() ? "nothing" : net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(tDrained.getFluid()).toString();
		String tLine = String.format("GT6 coke oven fluid drain at %s face %s: drained %d/%d mB of %s%s, tank holds %d mB",
				tOven.getBlockPos().toShortString(), aSide == null ? "any" : aSide.getName(), tGot, aAmount, tFluid,
				tGot == 0 ? " (REJECTED)" : " (ACCEPTED)", tOven.mTanksOutput[0].amount());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * {@code fluid <pos> [side] fill <mB>} — the acceptance probe: the wrapper refuses every
	 * face and fluid (output-only, the upstream getFluidTankFillable2 :566 mask-0 leg), so the
	 * accepted amount is always 0. The probe fluid is the tank's own content when present
	 * (the honest "would it take more of itself" offer), else vanilla water.
	 */
	private static int fluidFill(CommandSourceStack aSource, BlockPos aPos, @Nullable Direction aSide, int aAmount) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + aPos.toShortString()));
			return 0;
		}
		//? if forge {
		IFluidHandler tHandler = tOven.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, aSide).orElse(null);
		//?} else {
		/*IFluidHandler tHandler = tOven.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tOven.getBlockPos(), aSide);
		 *///?}
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the coke oven exposes no FLUID_HANDLER on "
					+ (aSide == null ? "the side-less query" : aSide)));
			return 0;
		}
		FluidStack tProbe = !tOven.mTanksOutput[0].isEmpty() && tOven.mTanksOutput[0].fluid() != null
				? new FluidStack(tOven.mTanksOutput[0].fluid().getFluid(), aAmount)
				: new FluidStack(Fluids.WATER, aAmount);
		int tAccepted = tHandler.fill(tProbe, FluidAction.EXECUTE);
		String tLine = String.format("GT6 coke oven fluid fill at %s face %s: accepted %d/%d mB of %s%s, tank holds %d mB",
				tOven.getBlockPos().toShortString(), aSide == null ? "any" : aSide.getName(), tAccepted, aAmount,
				net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(tProbe.getFluid()).toString(),
				tAccepted == 0 ? " (REJECTED)" : " (ACCEPTED)", tOven.mTanksOutput[0].amount());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** {@code down|up|north|south|west|east} → Direction (the GTBarrelCommand parse, mirrored so the driver stays self-contained). */
	private static Direction parseSide(String aWord) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Direction tSide = Direction.byName(aWord.toLowerCase());
		if (tSide == null) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Unknown side: " + aWord)).create();
		return tSide;
	}

	// ---------------------------------------------------------------------------
	// the Large Boiler arm (task p13-large-boiler — the place/wand/check shape over the
	// five variant rows; every failure line carries the FAILED literal, the RCON contract)
	// ---------------------------------------------------------------------------

	/** The variant row lookup — null for an unknown path (the GT6Boilers.blockByPath form). */
	@Nullable
	private static gregtech6.registry.GTMultiBlocks.LargeBoilerRow boilerRow(String aVariant) {
		for (gregtech6.registry.GTMultiBlocks.LargeBoilerRow tRow : gregtech6.registry.GTMultiBlocks.LARGE_BOILER_ROWS) {
			if (tRow.path().equalsIgnoreCase(aVariant)) return tRow;
		}
		return null;
	}

	@Nullable
	private static TileEntityLargeBoiler boilerAt(CommandSourceStack aSource, @Nullable BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		BlockPos tTarget = aPos != null ? aPos : BlockPos.containing(aSource.getPosition());
		return tLevel.getBlockEntity(tTarget) instanceof TileEntityLargeBoiler tBoiler ? tBoiler : null;
	}

	/** The structure anchor: one cell in FRONT of the facing at the SAME layer (upstream :98 — no getOffsetYN). */
	private static BlockPos boilerAnchor(TileEntityLargeBoiler aBoiler) {
		return new BlockPos(aBoiler.getOffsetXN(aBoiler.mFacing), aBoiler.getBlockPos().getY(), aBoiler.getOffsetZN(aBoiler.mFacing));
	}

	private static int boilerPlace(CommandSourceStack aSource, BlockPos aPos, String aVariant) {
		gregtech6.registry.GTMultiBlocks.LargeBoilerRow tRow = boilerRow(aVariant);
		net.minecraft.world.level.block.Block tBlock = gregtech6.registry.GTMultiBlocks.boilerBlockByPath(aVariant);
		if (tRow == null || tBlock == null) {
			aSource.sendFailure(Component.literal("BOILER PLACE FAILED: unknown boiler variant " + aVariant));
			return 0;
		}
		aSource.getLevel().setBlock(aPos, tBlock.defaultBlockState(), 3);
		String tLine = "GT6 large boiler placed at " + aPos.toShortString() + ": " + aVariant
				+ " (output " + tRow.outputSteamPerTick() + " SU/t, walls " + tRow.wallPath() + ")";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * Places the 34 part cells around the anchor (occupied cells kept): the 9-transmitter
	 * base (y-1), the 8 middle-ring walls (the 9th middle cell IS the controller, the
	 * checkAndSetTarget self-cell arm passes it), the top centre and the two 8-cell rings —
	 * the hollow air cell above the anchor stays untouched.
	 */
	private static int boilerFrame(CommandSourceStack aSource, BlockPos aPos, String aVariant) {
		TileEntityLargeBoiler tBoiler = boilerAt(aSource, aPos);
		gregtech6.registry.GTMultiBlocks.LargeBoilerRow tRow = boilerRow(aVariant);
		if (tBoiler == null) {
			aSource.sendFailure(Component.literal("BOILER FRAME FAILED: no TileEntityLargeBoiler at " + aPos.toShortString()));
			return 0;
		}
		if (tRow == null) {
			aSource.sendFailure(Component.literal("BOILER FRAME FAILED: unknown boiler variant " + aVariant));
			return 0;
		}
		ServerLevel tLevel = aSource.getLevel();
		BlockPos tAnchor = boilerAnchor(tBoiler);
		net.minecraft.world.level.block.Block tWall = gregtech6.registry.GTMultiBlocks.WALL_BLOCKS_BY_PATH.get(tRow.wallPath()).get();
		net.minecraft.world.level.block.Block tTransmitter = gregtech6.registry.GTMultiBlocks.HEAT_TRANSMITTER.get();
		int[] tPlaced = {0};
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) tPlaced[0] += frameCell(tLevel, tAnchor.offset(tDX, -1, tDZ), tTransmitter);
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			BlockPos tCell = tAnchor.offset(tDX, 0, tDZ);
			if (!tCell.equals(tBoiler.getBlockPos())) tPlaced[0] += frameCell(tLevel, tCell, tWall);
		}
		tPlaced[0] += frameCell(tLevel, tAnchor.offset(0, 2, 0), tWall);
		for (int i = 1; i < 3; i++) {
			tPlaced[0] += frameCell(tLevel, tAnchor.offset(-1, i, -1), tWall);
			tPlaced[0] += frameCell(tLevel, tAnchor.offset(0, i, -1), tWall);
			tPlaced[0] += frameCell(tLevel, tAnchor.offset(1, i, -1), tWall);
			tPlaced[0] += frameCell(tLevel, tAnchor.offset(-1, i, 0), tWall);
			tPlaced[0] += frameCell(tLevel, tAnchor.offset(1, i, 0), tWall);
			tPlaced[0] += frameCell(tLevel, tAnchor.offset(-1, i, 1), tWall);
			tPlaced[0] += frameCell(tLevel, tAnchor.offset(0, i, 1), tWall);
			tPlaced[0] += frameCell(tLevel, tAnchor.offset(1, i, 1), tWall);
		}
		int tReported = tPlaced[0];
		aSource.sendSuccess(() -> Component.literal("GT6 large boiler frame: " + tReported + " parts placed around " + tAnchor.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	/** One frame cell: air cells get the block, occupied cells keep their content. */
	private static int frameCell(ServerLevel aLevel, BlockPos aPos, net.minecraft.world.level.block.Block aBlock) {
		if (aLevel.getBlockState(aPos).isAir()) {
			aLevel.setBlock(aPos, aBlock.defaultBlockState(), 3);
			return 1;
		}
		return 0;
	}

	/**
	 * The builder-wand simulation (the cokeoven wand shape): a FakePlayer inventory stocked
	 * with 9 transmitters + 25 walls of the variant, the placing checkStructure2 pass, then
	 * the linking checkStructure pass.
	 */
	private static int boilerWand(CommandSourceStack aSource, BlockPos aPos, String aVariant) {
		TileEntityLargeBoiler tBoiler = boilerAt(aSource, aPos);
		gregtech6.registry.GTMultiBlocks.LargeBoilerRow tRow = boilerRow(aVariant);
		if (tBoiler == null) {
			aSource.sendFailure(Component.literal("BOILER WAND FAILED: no TileEntityLargeBoiler at " + aPos.toShortString()));
			return 0;
		}
		if (tRow == null) {
			aSource.sendFailure(Component.literal("BOILER WAND FAILED: unknown boiler variant " + aVariant));
			return 0;
		}
		net.minecraft.world.entity.player.Inventory tInventory = FakePlayerFactory.getMinecraft(aSource.getLevel()).getInventory();
		ItemStack tTransmitters = new ItemStack(gregtech6.registry.GTMultiBlocks.PART_ITEMS_BY_PATH.get(gregtech6.registry.GTMultiBlocks.TRANSMITTER_ROW.path()).get(), BOILER_WAND_TRANSMITTER_STOCK);
		ItemStack tWalls = new ItemStack(gregtech6.registry.GTMultiBlocks.WALL_BLOCKS_BY_PATH.get(tRow.wallPath()).get().asItem(), BOILER_WAND_WALL_STOCK);
		tInventory.items.set(0, tTransmitters);
		tInventory.items.set(1, tWalls);

		tBoiler.checkStructure2(tBoiler.getBlockPos(), null, tInventory); // the placing pass
		boolean tFormed = tBoiler.checkStructure(true);                   // the linking pass

		int tLeftTransmitters = tInventory.items.get(0).getCount();
		int tLeftWalls = tInventory.items.get(1).getCount();
		String tReport = String.format("GT6 large boiler wand at %s: variant=%s formed=%s, stock tx %d -> %d, walls %d -> %d",
				tBoiler.getBlockPos().toShortString(), aVariant, tFormed, BOILER_WAND_TRANSMITTER_STOCK, tLeftTransmitters, BOILER_WAND_WALL_STOCK, tLeftWalls);
		if (!tFormed) {
			aSource.sendFailure(Component.literal("GT6 multiblock boiler wand check FAILED: " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("GT6 multiblock boiler wand check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The magnifying-glass verdict + a linked-part census over the 34 part cells. */
	private static int boilerCheck(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityLargeBoiler tBoiler = boilerAt(aSource, aPos);
		if (tBoiler == null) {
			aSource.sendFailure(Component.literal("BOILER CHECK FAILED: no TileEntityLargeBoiler at " + aPos.toShortString()));
			return 0;
		}
		String tVerdict;
		if (tBoiler.checkStructure(false)) {
			tVerdict = "Structure is formed already!";
		} else {
			tVerdict = tBoiler.checkStructure(true) ? "Structure did form just now!" : "Structure did not form!";
		}
		int tLinked = 0;
		BlockPos tAnchor = boilerAnchor(tBoiler);
		ServerLevel tLevel = aSource.getLevel();
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) tLinked += linkedCell(tLevel, tAnchor.offset(tDX, -1, tDZ), tBoiler);
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			BlockPos tCell = tAnchor.offset(tDX, 0, tDZ);
			if (!tCell.equals(tBoiler.getBlockPos())) tLinked += linkedCell(tLevel, tCell, tBoiler);
		}
		tLinked += linkedCell(tLevel, tAnchor.offset(0, 2, 0), tBoiler);
		for (int i = 1; i < 3; i++) {
			tLinked += linkedCell(tLevel, tAnchor.offset(-1, i, -1), tBoiler);
			tLinked += linkedCell(tLevel, tAnchor.offset(0, i, -1), tBoiler);
			tLinked += linkedCell(tLevel, tAnchor.offset(1, i, -1), tBoiler);
			tLinked += linkedCell(tLevel, tAnchor.offset(-1, i, 0), tBoiler);
			tLinked += linkedCell(tLevel, tAnchor.offset(1, i, 0), tBoiler);
			tLinked += linkedCell(tLevel, tAnchor.offset(-1, i, 1), tBoiler);
			tLinked += linkedCell(tLevel, tAnchor.offset(0, i, 1), tBoiler);
			tLinked += linkedCell(tLevel, tAnchor.offset(1, i, 1), tBoiler);
		}
		boolean tBlockFormed = tLevel.getBlockState(tBoiler.getBlockPos()).getValue(TileEntityBase10MultiBlockBase.FORMED);
		String tReport = String.format("GT6 large boiler at %s: %s okay=%s block_formed=%s linked_parts=%d/34",
				tBoiler.getBlockPos().toShortString(), tVerdict, tBoiler.mStructureOkay, tBlockFormed, tLinked);
		if (!tBoiler.mStructureOkay || !tBlockFormed) {
			aSource.sendFailure(Component.literal(tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	private static int linkedCell(ServerLevel aLevel, BlockPos aPos, TileEntityLargeBoiler aBoiler) {
		return aLevel.getBlockEntity(aPos) instanceof MultiBlockPartBlockEntity tPart && tPart.getTarget(false) == aBoiler ? 1 : 0;
	}

	/** The full readback: variant, walls, facing, formed, the thermometer line, tanks, gauge, calcification. */
	private static int boilerStat(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityLargeBoiler tBoiler = boilerAt(aSource, aPos);
		if (tBoiler == null) {
			aSource.sendFailure(Component.literal("BOILER STAT FAILED: no TileEntityLargeBoiler at " + aPos.toShortString()));
			return 0;
		}
		StringBuilder tLine = new StringBuilder("GT6 large boiler (").append(tBoiler.getTileEntityName()).append(") at ")
				.append(aPos.toShortString())
				.append(": facing=").append(Direction.from3DDataValue(tBoiler.mFacing).getName())
				.append("(").append(tBoiler.mFacing).append(") formed=").append(tBoiler.mStructureOkay)
				.append(", ").append(tBoiler.thermometer())
				.append(", demand=").append(tBoiler.mOutput / 2).append(" HU/t (heat transmitters)")
				.append(", output=").append(tBoiler.mOutput).append(" SU/t (five pipe holes, >half tank)")
				.append(", efficiency=").append(tBoiler.mEfficiency).append("/10000")
				.append(", barometer=").append(tBoiler.mBarometer).append("/31")
				.append(", water=");
		if (tBoiler.mTanks[0].isEmpty()) tLine.append("empty");
		else tLine.append(tBoiler.mTanks[0].amount()).append("/").append(tBoiler.mTanks[0].capacity()).append("L");
		tLine.append(", steam=");
		if (tBoiler.mTanks[1].isEmpty()) tLine.append("empty");
		else tLine.append(tBoiler.mTanks[1].amount()).append("/").append(tBoiler.mTanks[1].capacity()).append("L");
		if (!tBoiler.mTanks[0].has()) tLine.append(", WARNING: NO WATER!!!");
		String tText = tLine.toString();
		aSource.sendSuccess(() -> Component.literal(tText), false);
		LOGGER.info(tText);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The water-intake arm — water through the controller's fluid door (the :380 water-only
	 * gate; the door's own halves make a REJECTED echo a legitimate verdict).
	 */
	private static int boilerFill(CommandSourceStack aSource, BlockPos aPos, int aAmount) {
		TileEntityLargeBoiler tBoiler = boilerAt(aSource, aPos);
		if (tBoiler == null) {
			aSource.sendFailure(Component.literal("BOILER FILL FAILED: no TileEntityLargeBoiler at " + aPos.toShortString()));
			return 0;
		}
		//? if forge {
		IFluidHandler tDoor = tBoiler.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
				Direction.DOWN).orElse(null);
		//?} else {
		/*IFluidHandler tDoor = tBoiler.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
				Direction.DOWN);
		 *///?}
		if (tDoor == null) {
			aSource.sendFailure(Component.literal("BOILER FILL FAILED: no fluid door at " + aPos.toShortString()));
			return 0;
		}
		int tFilled = tDoor.fill(new FluidStack(Fluids.WATER, aAmount), FluidAction.EXECUTE);
		String tLine = String.format("GT6 large boiler fill at %s: filled %d/%d L of minecraft:water%s, water tank holds %d L",
				aPos.toShortString(), tFilled, aAmount, tFilled == 0 ? " (REJECTED)" : " (ACCEPTED)", tBoiler.mTanks[0].amount());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The direct HU supply arm — one packet through the doEnergyInjection gate (the aSize=1 firebox emit form). */
	private static int boilerInjectHu(CommandSourceStack aSource, BlockPos aPos, int aAmount) {
		TileEntityLargeBoiler tBoiler = boilerAt(aSource, aPos);
		if (tBoiler == null) {
			aSource.sendFailure(Component.literal("BOILER INJECT FAILED: no TileEntityLargeBoiler at " + aPos.toShortString()));
			return 0;
		}
		long tBooked = tBoiler.doEnergyInjection(gregapi.data.TD.Energy.HU, (byte)0, 1, aAmount, true);
		String tLine = String.format("GT6 large boiler inject-hu at %s: booked %d/%d HU%s, store holds %d/%d HU",
				aPos.toShortString(), tBooked, aAmount, tBooked == 0 ? " (REJECTED)" : " (ACCEPTED)",
				tBoiler.mEnergy, tBoiler.mCapacity);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The TOOL_plunger arm (:275-278) — the water tank first, else the steam tank; the
	 * teardown channel of the acceptance chain.
	 */
	private static int boilerPlunge(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityLargeBoiler tBoiler = boilerAt(aSource, aPos);
		if (tBoiler == null) {
			aSource.sendFailure(Component.literal("BOILER PLUNGE FAILED: no TileEntityLargeBoiler at " + aPos.toShortString()));
			return 0;
		}
		long tTrashed = tBoiler.plunger();
		String tLine = "GT6 large boiler plunge at " + aPos.toShortString() + ": trashed " + tTrashed + " L"
				+ " (water=" + tBoiler.mTanks[0].amount() + ", steam=" + tBoiler.mTanks[1].amount() + ")";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The removedByPlayer arm (:313-316): the null player IS the non-creative
	 * counterfactual — barometer &gt; 4 → explode(T) instant; the block is removed either
	 * way (the :315 setBlockToAir). The W3 review-note dismantle step, exercised on the
	 * Large Boiler's own controller.
	 */
	private static int boilerDismantle(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityLargeBoiler tBoiler = boilerAt(aSource, aPos);
		if (tBoiler == null) {
			aSource.sendFailure(Component.literal("BOILER DISMANTLE FAILED: no TileEntityLargeBoiler at " + aPos.toShortString()));
			return 0;
		}
		boolean tExploded = tBoiler.dismantle(null);
		aSource.getLevel().destroyBlock(aPos, false); // the :315 setBlockToAir (already gone on the exploded arm)
		String tLine = "GT6 large boiler dismantle at " + aPos.toShortString() + ": barometer=" + tBoiler.mBarometer
				+ (tExploded ? " — pressurised, EXPLODED (instant)" : " — quiet, removed");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}
}
