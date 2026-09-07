package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GT6SprayCans;

/**
 * {@code /gt6grass} — the grass-family acceptance command home (task p24-grass-block, the
 * GTBurnerCommand template). Game-bus listener, self-contained per ADR-P3-4. This port
 * has no player-click RCON seam, so the chain drives the spray-can ROUTE directly — the
 * same code face the {@code GTSprayCanItem.useOn} :216-221 server half runs:
 * <ul>
 * <li>{@code spray <pos> <dye>} — a FRESH full can of the dye index (0..15) over the
 *     target block: the {@code colorTarget} verdict first ({@code null} = the upstream
 *     :161 no-op — the chain arm pins the UNPAID remaining), else
 *     {@link GTSprayCanItem#spray} (the aPlayer == null arm: the payment lands on the
 *     stack, a depleted can shrinks to zero) — the report names the landed block id and
 *     the remaining units.</li>
 * <li>{@code unpaint <pos>} — the remover can over the {@code decolorTarget} verdict:
 *     any GT grass variant → the vanilla grass block (the Remover :104 swap), the
 *     report names the landed block.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTGrassCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTGrassCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tGrass = Commands.literal("gt6grass")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("spray")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("dye", IntegerArgumentType.integer(0, 15))
						.executes(aContext -> spray(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								(byte) IntegerArgumentType.getInteger(aContext, "dye"))))))
			.then(Commands.literal("unpaint")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> unpaint(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		aEvent.getDispatcher().register(tGrass);
		LOGGER.info("Registered GT6 grass command /gt6grass (spray | unpaint) — the grass family acceptance home");
	}

	/** The spray arm — the useOn :216-221 server-half route over a fresh full can of the dye. */
	private static int spray(CommandSourceStack aSource, BlockPos aPos, byte aDye) {
		ServerLevel tLevel = aSource.getLevel();
		ItemStack tStack = new ItemStack(GT6SprayCans.SPRAY_PAINTS.get(aDye).get());
		GTSprayCanItem tCan = (GTSprayCanItem) tStack.getItem();
		Block tTarget = tCan.colorTarget(tLevel.getBlockState(aPos).getBlock(), aDye);
		if (tTarget == null) {
			String tLine = "GT6 grass spray NO-OP at " + aPos.toShortString() + ": dye " + aDye
					+ " remaining=" + tCan.remainingOf(tStack) + " (unpaid)";
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
			return Command.SINGLE_SUCCESS;
		}
		long tBefore = tCan.remainingOf(tStack);
		boolean tHit = tCan.spray(tLevel, aPos, tStack, null, InteractionHand.MAIN_HAND);
		Block tLanded = tLevel.getBlockState(aPos).getBlock();
		ResourceLocation tId = ForgeRegistries.BLOCKS.getKey(tLanded);
		String tLine = "GT6 grass spray at " + aPos.toShortString() + ": dye " + aDye + " hit=" + tHit
				+ " landed=" + tId + " remaining=" + tCan.remainingOf(tStack) + " (was " + tBefore + ")";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The unpaint arm — the remover can over the decolorTarget verdict (the Remover :104 swap). */
	private static int unpaint(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		ItemStack tStack = new ItemStack(GT6SprayCans.SPRAY_PAINT_REMOVER.get());
		GTSprayCanItem tRemover = (GTSprayCanItem) tStack.getItem();
		Block tTarget = tRemover.decolorTarget(tLevel.getBlockState(aPos).getBlock());
		if (tTarget == null) {
			String tLine = "GT6 grass unpaint NO-OP at " + aPos.toShortString() + ": not removable remaining="
					+ tRemover.remainingOf(tStack) + " (unpaid)";
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
			return Command.SINGLE_SUCCESS;
		}
		long tBefore = tRemover.remainingOf(tStack);
		boolean tHit = tRemover.spray(tLevel, aPos, tStack, null, InteractionHand.MAIN_HAND);
		Block tLanded = tLevel.getBlockState(aPos).getBlock();
		ResourceLocation tId = ForgeRegistries.BLOCKS.getKey(tLanded);
		String tLine = "GT6 grass unpaint at " + aPos.toShortString() + ": hit=" + tHit + " landed=" + tId
				+ " remaining=" + tRemover.remainingOf(tStack) + " (was " + tBefore + ")";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}
}
