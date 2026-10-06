package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import gregtech6.registry.GT6Tools;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;

/**
 * {@code /gt6wrenchuse} — the wrench-click-chain acceptance driver (task
 * wrench-interaction-chain; card-local in command/, the GT6DrinkCommand shape: RCON
 * sessions have no player, so the seam that IS a player right-click gets the fake
 * player and the REAL {@code ServerPlayerGameMode.useItemOn} dispatch — the item-side
 * {@code useOn} road (GT6MachineFaceCommand) would bypass the vanilla sneak gate and
 * prove nothing for symptom18).
 *
 * <ul>
 * <li>{@code sneak <pos> <face>} — the fake player holds the formal wrench SNEAKING:
 *     the exact click vanilla gates away (1.20.1 ServerPlayerGameMode.useItemOn
 *     :312-315 flag1; the {@code GTWrenchSneakUseListener} un-gate must lift it). On a
 *     fluid pipe the report's ioMask is the truth — the shift arm toggles the output
 *     arrow on the clicked sub-face.</li>
 * <li>{@code plain <pos> <face>} — the same dispatch NOT sneaking (the never-gated
 *     leg, the regression arm: connections toggle / facing rotate still work).</li>
 * </ul>
 *
 * <p>The hit is the block centre on the named face — the in-face offsets land 0.5/0.5,
 * so {@code UT6.getSideWrenching} picks the clicked face itself (the centre-cell rule,
 * UT6.java:19) — the assertion surface stays deterministic.
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6WrenchUseCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6WrenchUseCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tUse = Commands.literal("gt6wrenchuse")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("sneak")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("face", StringArgumentType.word())
						.executes(aContext -> click(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								StringArgumentType.getString(aContext, "face"), true)))))
			.then(Commands.literal("plain")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("face", StringArgumentType.word())
						.executes(aContext -> click(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								StringArgumentType.getString(aContext, "face"), false)))));
		aEvent.getDispatcher().register(tUse);
		LOGGER.info("Registered GT6 wrench-click driver /gt6wrenchuse (sneak|plain <pos> <face>) — the wrench-interaction-chain acceptance home");
	}

	/** The one real right-click dispatch (sneak or plain) + the post-click state report. */
	private static int click(CommandSourceStack aSource, BlockPos aPos, String aFace, boolean aSneak) {
		Direction tFace = Direction.byName(aFace);
		if (tFace == null || tFace.getStepX() == 0 && tFace.getStepY() == 0 && tFace.getStepZ() == 0) {
			aSource.sendFailure(Component.literal("GT6 WRENCHUSE FAILED: no direction named " + aFace));
			return 0;
		}
		ServerLevel tLevel = aSource.getLevel();
		var tPlayer = FakePlayerFactory.getMinecraft(tLevel);
		tPlayer.getInventory().clearContent(); // a leftover from an earlier command would fake the arm
		tPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GT6Tools.WRENCH.get()));
		tPlayer.setShiftKeyDown(aSneak);
		// the centre hit — in-face offsets 0.5/0.5 → getSideWrenching returns the clicked face itself
		BlockHitResult tHit = new BlockHitResult(Vec3.atCenterOf(aPos), tFace, aPos, false);
		InteractionResult tResult = tPlayer.gameMode.useItemOn(tPlayer, tLevel,
				tPlayer.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, tHit);
		BlockState tState = tLevel.getBlockState(aPos);
		String tFacing = tState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)
				? String.valueOf(tState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) : "-";
		byte tIoMask = -1;
		byte tConnections = -1;
		BlockEntity tBE = tLevel.getBlockEntity(aPos);
		if (tBE instanceof GTFluidPipeBlockEntity tPipe) {
			tIoMask = tPipe.getIoMask();
			tConnections = tPipe.getConnections();
		}
		String tLine = "gt6wrenchuse " + (aSneak ? "sneak" : "plain") + " at " + aPos.toShortString() + " face " + aFace
				+ ": result=" + tResult
				+ " block=" + BuiltInRegistries.BLOCK.getKey(tState.getBlock())
				+ " facing=" + tFacing
				+ " ioMask=" + tIoMask
				+ " connections=" + tConnections;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return Command.SINGLE_SUCCESS;
	}
}
