package gregtech6.crop;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The crop acceptance command (card cbc-1-cropstick-base)  --  the GTBarrelCommand/GTBurnerCommand
 * ruling: no player-click RCON seam exists, the admin command IS the acceptance channel. It
 * drives the SAME server halves the interaction runs (the BE faces directly, permission 2):
 * <ul>
 * <li>{@code plant <pos> <crop> [size]}  --  the {@code tryPlantIn} face with the optional size
 *     (the :464 signature takes size  --  the mature-arm smoke rides it; growth stays offline per
 *     the ADR-CB5 ruling, nothing here advances ticks).</li>
 * <li>{@code stick <pos>}  --  the crossing upgrade, the rightClick stick arm's command twin.</li>
 * <li>{@code harvest <pos>}  --  the rightClick harvest half; drops spawn as real item entities.</li>
 * <li>{@code readout <pos>}  --  the BE state line (the Cropnalyzer scan domain is cbc-4).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6CropCommand {

	private GT6CropCommand() {}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		aEvent.getDispatcher().register(
				Commands.literal("gt6crop")
						.requires(aSource -> aSource.hasPermission(2))
						.then(Commands.literal("plant")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.then(Commands.argument("crop", StringArgumentType.word())
												.executes(aContext -> plant(aContext, 1))
												.then(Commands.argument("size", IntegerArgumentType.integer(1, 31))
														.executes(aContext -> plant(aContext, IntegerArgumentType.getInteger(aContext, "size")))))))
						.then(Commands.literal("stick")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(GT6CropCommand::stick)))
						.then(Commands.literal("harvest")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(GT6CropCommand::harvest)))
						.then(Commands.literal("readout")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(GT6CropCommand::readout))));
	}

	private static int plant(CommandContext<CommandSourceStack> aContext, int aSize) {
		try {
			BlockPos tPos = BlockPosArgument.getLoadedBlockPos(aContext, "pos");
			String tName = StringArgumentType.getString(aContext, "crop");
			CropCardView tCard = GT6CropGrains.crop(tName);
			if (tCard == null) {
				aContext.getSource().sendFailure(Component.literal("unknown crop=" + tName));
				return 0;
			}
			if (!(aContext.getSource().getLevel().getBlockEntity(tPos) instanceof GT6CropBlockEntity tCrop)) {
				aContext.getSource().sendFailure(Component.literal("no crop sticks at " + tPos.toShortString()));
				return 0;
			}
			boolean tOk = tCrop.tryPlantIn(tCard, aSize, 1, 1, 1, 1); // the base-seed 1/1/1 stats (GT_BaseCrop.java:77)
			aContext.getSource().sendSuccess(() -> Component.literal(
					"planted=" + tCard.name() + " size=" + aSize + " ok=" + tOk), false);
			return tOk ? 1 : 0;
		} catch (Exception aE) {
			aContext.getSource().sendFailure(Component.literal("plant failed: " + aE.getMessage()));
			return 0;
		}
	}

	private static int stick(CommandContext<CommandSourceStack> aContext) {
		try {
			BlockPos tPos = BlockPosArgument.getLoadedBlockPos(aContext, "pos");
			if (!(aContext.getSource().getLevel().getBlockEntity(tPos) instanceof GT6CropBlockEntity tCrop)) {
				aContext.getSource().sendFailure(Component.literal("no crop sticks at " + tPos.toShortString()));
				return 0;
			}
			// the rightClick stick arm through the REAL interaction face (creative=true: no
			// consumption  --  the :415 creative gate), not a field write
			boolean tOk = tCrop.rightClick(aContext.getSource().getLevel(), tPos,
					new net.minecraft.world.item.ItemStack(gregtech6.registry.GT6CropSticks.CROP_STICK_ITEM.get()), true);
			aContext.getSource().sendSuccess(() -> Component.literal("crossing=" + tCrop.isCrossingBase() + " ok=" + tOk), false);
			return tOk ? 1 : 0;
		} catch (Exception aE) {
			aContext.getSource().sendFailure(Component.literal("stick failed: " + aE.getMessage()));
			return 0;
		}
	}

	private static int harvest(CommandContext<CommandSourceStack> aContext) {
		try {
			BlockPos tPos = BlockPosArgument.getLoadedBlockPos(aContext, "pos");
			if (!(aContext.getSource().getLevel().getBlockEntity(tPos) instanceof GT6CropBlockEntity tCrop)) {
				aContext.getSource().sendFailure(Component.literal("no crop sticks at " + tPos.toShortString()));
				return 0;
			}
			boolean tHarvested = tCrop.performManualHarvest(aContext.getSource().getLevel(), tPos);
			aContext.getSource().sendSuccess(() -> Component.literal(
					"harvested=" + tHarvested + " size=" + tCrop.getCurrentSize() + " crop=" + (tCrop.getCrop() == null ? "null" : tCrop.getCrop().name())), false);
			return tHarvested ? 1 : 0;
		} catch (Exception aE) {
			aContext.getSource().sendFailure(Component.literal("harvest failed: " + aE.getMessage()));
			return 0;
		}
	}

	private static int readout(CommandContext<CommandSourceStack> aContext) {
		try {
			BlockPos tPos = BlockPosArgument.getLoadedBlockPos(aContext, "pos");
			if (!(aContext.getSource().getLevel().getBlockEntity(tPos) instanceof GT6CropBlockEntity tCrop)) {
				aContext.getSource().sendFailure(Component.literal("no crop sticks at " + tPos.toShortString()));
				return 0;
			}
			BlockState tState = aContext.getSource().getLevel().getBlockState(tPos);
			String tCrossing = stateString(tState);
			aContext.getSource().sendSuccess(() -> Component.literal(tCrossing + " " + tCrop.readout()), false);
			return 1;
		} catch (Exception aE) {
			aContext.getSource().sendFailure(Component.literal("readout failed: " + aE.getMessage()));
			return 0;
		}
	}

	private static String stateString(BlockState aState) {
		StringBuilder tBuilder = new StringBuilder(aState.getBlock().toString());
		for (java.util.Map.Entry<Property<?>, Comparable<?>> tRow : aState.getValues().entrySet()) {
			tBuilder.append(' ').append(tRow.getKey().getName()).append('=').append(tRow.getValue().toString());
		}
		return tBuilder.toString();
	}
}
