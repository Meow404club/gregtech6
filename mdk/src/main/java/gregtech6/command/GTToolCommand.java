package gregtech6.command;

import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import gregtech6.covers.ICoverableTE;
import gregtech6.items.tools.GTCrowbarItem;
import gregtech6.registry.GT6Tools;

/**
 * {@code /gt6tool} — the tool acceptance command (task p9-tool-crowbar spec ③; the
 * RCON entry that drives the formal crowbar without a player at the keyboard,
 * card-local in command/ like GTWireCommand/GT6EnergyCommand). The dismantle runs the
 * exact {@link GTCrowbarItem#crowbarToolClick(UseOnContext)} dispatch the item's
 * {@code useOn} runs — a fake player holds the crowbar so the cover item lands in the
 * inventory (upstream :149 ST.add) and the durability payment is observable:
 *
 * <ul>
 * <li>{@code dismantle [<pos>] [<side>]} — give the fake player a gt6:crowbar, aim it
 *     at the face (a synthetic BlockHitResult, the miss-free constructor form), run the
 *     shared dispatch and assert the upstream 10000 return, the one-point durability
 *     payment and the cover leaving the store; the report names the inventory landing.</li>
 * </ul>
 *
 * <p>This is the TOOL_CROWBAR-id half of the ICoverableTE :246-247 OR gate — the
 * legacy hoe half keeps its own {@code /gt6cover dismantle} (GTCoverCommand), so both
 * classification branches stay independently asserted in an RCON chain.
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTToolCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTToolCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> tTool = Commands.literal("gt6tool")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("dismantle")
				.executes(context -> dismantle(context.getSource(), null, Direction.UP))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> dismantle(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), Direction.UP))
					.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(context -> dismantle(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"),
								parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side")))))));
		event.getDispatcher().register(tTool);
		LOGGER.info("Registered GT6 tool acceptance command /gt6tool (dismantle)");
	}

	/** {@code down|up|north|south|west|east} → Direction (the GTCoverCommand parser shape). */
	private static Direction parseSide(String aWord) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Direction tSide = Direction.byName(aWord.toLowerCase());
		if (tSide == null) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Unknown side: " + aWord)).create();
		return tSide;
	}

	/**
	 * Crowbar-dismantle the cover through the item's own dispatch: the fake player
	 * holds a gt6:crowbar, the report asserts the 10000 upstream return, the 1-point
	 * durability payment and the cover landing in the fake player's inventory.
	 */
	private static int dismantle(CommandSourceStack source, BlockPos pos, Direction side) {
		ServerLevel tLevel = source.getLevel();
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		if (!(tLevel.getBlockEntity(tTarget) instanceof ICoverableTE tHost)) {
			source.sendFailure(Component.literal("No coverable GT6 BlockEntity at " + tTarget.toShortString()));
			return 0;
		}
		if (!tHost.isCovered((byte) side.get3DDataValue())) {
			source.sendFailure(Component.literal("gt6tool dismantle FAILED: no cover on face " + side + " at " + tTarget.toShortString()));
			return 0;
		}
		ItemStack tCoverBefore = tHost.getCoverItem((byte) side.get3DDataValue());
		ItemStack tCrowbar = new ItemStack(GT6Tools.CROWBAR.get());
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent(); // a leftover from an earlier command would fake the landing
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tCrowbar);
		// the same UseOnContext shape useOn receives: the synthetic hit pins the clicked
		// face at the block pos (isInside=false, the plain four-arg constructor)
		var tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(tTarget), side, tTarget, false));
		long tDamage = GTCrowbarItem.crowbarToolClick(tContext);
		// the cover item landed in the fake player's inventory (upstream :149 ST.add —
		// the give half; the drop half is the /gt6cover dismantle null-player path)
		boolean tCoverInInventory = !tCoverBefore.isEmpty() && tFakePlayer.getInventory().items.stream()
				.anyMatch(t -> !t.isEmpty() && t.getItem() == tCoverBefore.getItem());
		int tHeldDamage = tCrowbar.getDamageValue();
		String tReport = String.format("gt6tool dismantle %s face %s at %s: toolDamage=%d, crowbarDamage=%d/%d, coverInInventory=%s",
				tHost, side, tTarget.toShortString(), tDamage, tHeldDamage, GTCrowbarItem.DURABILITY_POINTS, tCoverInInventory);
		if (tDamage != GTCrowbarItem.TOOL_DAMAGE_PER_DISMANTLE || tHeldDamage != 1 || !tCoverInInventory) {
			source.sendFailure(Component.literal("gt6tool dismantle FAILED: " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("gt6tool dismantle check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}
}
