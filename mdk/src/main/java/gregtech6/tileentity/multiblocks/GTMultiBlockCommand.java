package gregtech6.tileentity.multiblocks;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

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
 * <li>{@code check <pos>} — the magnifying glass (upstream onMagnifyingGlass :160-170):
 *     cheap-path check, forced recheck on failure, verdict + linked-part census;</li>
 * <li>{@code tick <pos> <ticks>} — drives the dispatcher manually (the same updateEntity the
 *     real ticker runs), exercising the onTickFirst forced check and the 600-tick poll.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTMultiBlockCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The wand stock: the full 25-brick structure in one stack (27 cells = air centre + 25 bricks + the controller). */
	private static final int WAND_STOCK = 25;

	private GTMultiBlockCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tMulti = Commands.literal("gt6multiblock")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("place")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("frame")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> frame(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("hole")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> hole(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("wand")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> wand(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("check")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> check(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("tick")
				.then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20000))
					.executes(aContext -> tick(aContext.getSource(),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "ticks"), null))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> tick(aContext.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "ticks"),
								BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))));
		aEvent.getDispatcher().register(tMulti);
		LOGGER.info("Registered GT6 multiblock acceptance command /gt6multiblock (place|frame|hole|wand|check|tick)");
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

	/** The magnifying glass (:160-170) + a linked-part census over the 26 cells. */
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
		String tReport = String.format("GT6 coke oven at %s: %s okay=%s block_formed=%s linked_parts=%d/25",
				tOven.getBlockPos().toShortString(), tVerdict, tOven.mStructureOkay, tBlockFormed, tLinked);
		if (!tOven.mStructureOkay || !tBlockFormed) {
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
}
