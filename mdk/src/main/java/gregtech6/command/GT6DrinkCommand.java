package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.stream.Collectors;

import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.fluid.GTDrinks;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;

/**
 * The drink-seam RCON driver (task p33-food-fluids-b2 acceptance, the GT6FaucetCommand
 * card-local form): RCON sessions have no player, so the seam that IS a player right-click
 * gets the FakePlayerFactory stand-in and the real dispatch.
 * <ul>
 * <li>{@code hunger <n>} — the fake player's food level for the next {@code use} (the
 *     pre-drink state; FoodData.setFoodLevel).</li>
 * <li>{@code clear} — food level 20 + the effects stripped (the fresh-drinker reset).</li>
 * <li>{@code use <pos>} — ONE real right-click dispatch: the vanilla
 *     {@code ServerPlayerGameMode.useItemOn} chain (ServerPlayerGameMode.java:297 — the
 *     empty-hands gate passes, the state.use call reaches {@link GTBarrelBlock#use}), an
 *     EMPTY main hand, at the barrel. The report line names the dispatch result, the tank
 *     content after, the food level and every active effect — the live drink evidence.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6DrinkCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6DrinkCommand() {}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tDrink = Commands.literal("gt6drink")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("hunger")
				.then(Commands.argument("level", IntegerArgumentType.integer(0, 20))
					.executes(aContext -> hunger(aContext.getSource(), IntegerArgumentType.getInteger(aContext, "level")))))
			.then(Commands.literal("clear")
				.executes(aContext -> clear(aContext.getSource())))
			.then(Commands.literal("use")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> use(aContext.getSource(),
							BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		aEvent.getDispatcher().register(tDrink);
		LOGGER.info("Registered GT6 drink command /gt6drink (hunger | clear | use) — the p33-food-fluids-b2 acceptance home");
	}

	/** The fresh-drinker reset: food 20, no effects. */
	private static int clear(CommandSourceStack aSource) {
		ServerPlayer tPlayer = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(aSource.getLevel());
		tPlayer.getFoodData().setFoodLevel(20);
		tPlayer.removeAllEffects();
		aSource.sendSuccess(() -> Component.literal("GT6 drink driver reset: food=20 effects=0"), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The pre-drink food level. */
	private static int hunger(CommandSourceStack aSource, int aLevel) {
		ServerPlayer tPlayer = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(aSource.getLevel());
		tPlayer.getFoodData().setFoodLevel(aLevel);
		aSource.sendSuccess(() -> Component.literal("GT6 drink driver food level set: food=" + tPlayer.getFoodData().getFoodLevel()), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The one real right-click: the useItemOn dispatch, then the state report. */
	private static int use(CommandSourceStack aSource, BlockPos aPos) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntityBase08Barrel tBarrel)) {
			aSource.sendFailure(Component.literal("GT6 DRINK FAILED: no barrel at " + aPos.toShortString()));
			return 0;
		}
		ServerPlayer tPlayer = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(aSource.getLevel());
		BlockState tState = aSource.getLevel().getBlockState(aPos);
		if (!(tState.getBlock() instanceof GTBarrelBlock)) {
			aSource.sendFailure(Component.literal("GT6 DRINK FAILED: not a barrel block at " + aPos.toShortString()));
			return 0;
		}
		BlockHitResult tHit = new BlockHitResult(Vec3.atCenterOf(aPos), Direction.UP, aPos, false);
		InteractionResult tResult = tPlayer.gameMode.useItemOn(tPlayer, aSource.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, tHit);
		// the tank read rides the mTank primitives (isEmpty/getFluidAmount), NOT the getFluid
		// stack — at 0 L the live stack keeps its identity (the keepFilter contract), so the
		// stack face would still name the drained fluid
		String tTankLine = tBarrel.mTank.isEmpty() ? "empty"
				: BuiltInRegistries.FLUID.getKey(tBarrel.mTank.getFluid().getFluid()) + " " + tBarrel.mTank.getFluidAmount() + "L";
		String tStatLine = tBarrel.mTank.isEmpty() ? "null"
				: String.valueOf(GTDrinks.stat(BuiltInRegistries.FLUID.getKey(tBarrel.mTank.getFluid().getFluid()).getPath()));
		//? if forge {
		String tEffects = tPlayer.getActiveEffects().stream()
				.map(tInstance -> BuiltInRegistries.MOB_EFFECT.getKey(tInstance.getEffect()).getPath()
						+ ":" + tInstance.getDuration() + ":" + tInstance.getAmplifier())
				.collect(Collectors.joining(","));
		//?} else {
		/*String tEffects = tPlayer.getActiveEffects().stream()
				.map(tInstance -> tInstance.getEffect().getRegisteredName()
						+ ":" + tInstance.getDuration() + ":" + tInstance.getAmplifier())
				.collect(Collectors.joining(","));
		 *///?}
		String tLine = "GT6 drink at " + aPos.toShortString() + ": result=" + tResult
				+ " tank=" + tTankLine
				+ " food=" + tPlayer.getFoodData().getFoodLevel()
				+ " saturation=" + tPlayer.getFoodData().getSaturationLevel()
				+ " effects=" + (tEffects.isEmpty() ? "0" : tEffects)
				+ " stat=" + tStatLine;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return Command.SINGLE_SUCCESS;
	}
}
