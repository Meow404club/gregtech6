package gregtech6.tileentity.example;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//? if forge {
import net.minecraftforge.network.NetworkHooks;
//?}
import org.slf4j.Logger;

import gregtech6.gui.GTMenuTypes;

/**
 * {@code /gt6chest} — the automated open-chain proof for the example chest
 * (task p3-example-machine acceptance 4), the {@code /gt6gui} shape (GTGuiCommand) applied to
 * a real MenuProvider block entity. Two subcommands:
 *
 * <ul>
 * <li>{@code open} (player) — aims at a GTExampleChestBlockEntity and calls
 *     NetworkHooks.openScreen(serverPlayer, chest, pos) (NetworkHooks.java:176) — the exact
 *     call GTExampleChestBlock.use makes, full server-side chain plus the OpenContainer
 *     packet; a success return plus a clean log line is the evidence (createMenu failures
 *     propagate through the command as exceptions).</li>
 * <li>{@code check [<pos>]} (console-safe) — drives the server-side MenuProvider factory
 *     directly through a Forge fake player (FakePlayerFactory, no network client involved):
 *     createMenu constructs the real GTExampleChestMenu (54 SlotItemHandler content slots +
 *     the 36-slot bindPlayerInventory), stillValid is probed, and menu removal closes the
 *     openers counter back — the whole server-side lifecycle observable in one log line.
 *     No packet is sent, so this is deterministic headless: the OpenContainer send of the
 *     NetworkHooks path is only meaningful with a real client, which is what {@code open}
 *     is for.</li>
 * </ul>
 *
 * <p>Game-bus listener (default {@code Bus.FORGE}) and self-contained per ADR-P3-4. The
 * blocked-above placement guard of the block's use() is deliberately not replayed here — this
 * exercises the open chain, not the placement guard.
 *
 * <p>Renamed from {@code /gt6machine} (task p11-gt6machine-literal-fix): the original root
 * literal was registered twice — here and by GTMachineCommand — and Brigadier's
 * CommandNode.addChild silently merges same-name literals into ONE dispatch node, so the
 * bare {@code /gt6machine check} slot resolved to this class's chest check while the
 * machine family advertised its own check subcommand. Per the port's one-feature-one-root
 * convention (gt6wire/gt6cover/gt6tool/gt6oven/...) the chest proof now owns
 * {@code /gt6chest}; {@code /gt6machine} belongs to the machine family alone.
 */
@Mod.EventBusSubscriber(modid = GTMenuTypes.MOD_ID)
public final class GTExampleChestCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final double REACH = 8.0D;
	private static final int PLAYER_SLOT_COUNT = 36;

	private GTExampleChestCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(
			Commands.literal("gt6chest")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("open")
					.executes(context -> openChest(context.getSource().getPlayerOrException())))
				.then(Commands.literal("check")
					.executes(context -> checkChest(context.getSource(), null))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> checkChest(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
		LOGGER.info("Registered GT6 open-chain command /gt6chest (open|check)");
	}

	/** Real-player path: full NetworkHooks.openScreen chain (same call site as the block's use()). */
	private static int openChest(ServerPlayer player) throws CommandSyntaxException {
		if (!(player.pick(REACH, 1.0F, false) instanceof BlockHitResult hit)) {
			player.sendSystemMessage(Component.literal("No block in reach (aim at the example chest)"));
			return 0;
		}
		BlockPos pos = hit.getBlockPos();
		if (!(player.level().getBlockEntity(pos) instanceof GTExampleChestBlockEntity chest)) {
			player.sendSystemMessage(Component.literal("No GTExampleChestBlockEntity in reach (aim at the example chest)"));
			return 0;
		}
		//? if forge {
		NetworkHooks.openScreen(player, chest, pos);
		//?} else {
		/*player.openMenu(chest, tBuf -> tBuf.writeBlockPos(pos));
		 *///?}
		LOGGER.info("GT6 open chain verified: MenuProvider.createMenu ran for GTExampleChestBlockEntity at {} (using players now {})", pos, chest.getUsingPlayers());
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * Console-safe path: the server-side MenuProvider lifecycle over a fake player. The fake
	 * player is parked next to the chest so the upstream distance rule
	 * (TileEntityBase05Inventories.java:104, distance&sup2; &le; 64) holds deterministically.
	 */
	private static int checkChest(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
		ServerLevel level = source.getLevel();
		BlockPos target = pos != null ? pos : BlockPos.containing(source.getPosition());
		if (!(level.getBlockEntity(target) instanceof GTExampleChestBlockEntity chest)) {
			source.sendFailure(Component.literal("No GTExampleChestBlockEntity at " + target.toShortString()));
			return 0;
		}
		ServerPlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
		fakePlayer.setPos(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);

		int usingBefore = chest.getUsingPlayers();
		AbstractContainerMenu menu = chest.createMenu(0, fakePlayer.getInventory(), fakePlayer); // getGUIServer semantics
		int slotCount = menu.slots.size();
		int usingOpen = chest.getUsingPlayers(); // the menu ctor hook incremented it (ContainerCommon.java:57)
		boolean valid = menu.stillValid(fakePlayer);
		menu.removed(fakePlayer); // closes the menu like a real close (closeInventoryGUI)
		int usingAfter = chest.getUsingPlayers();

		boolean slotShapeOk = slotCount == GTExampleChestBlockEntity.INVENTORY_SIZE + PLAYER_SLOT_COUNT;
		boolean counterOk = usingOpen == usingBefore + 1 && usingAfter == usingBefore;
		if (!slotShapeOk || !valid || !counterOk) {
			source.sendFailure(Component.literal(String.format(
				"GT6 open chain check FAILED at %s (slots %d, stillValid %s, using players %d -> %d -> %d)",
				target.toShortString(), slotCount, valid, usingBefore, usingBefore + 1, usingAfter)));
			return 0;
		}
		source.sendSuccess(() -> Component.literal(String.format(
			"GT6 open chain check OK at %s: createMenu built %d slots (%d content + 36 player), stillValid, openers counter %d -> %d -> %d",
			target.toShortString(), slotCount, GTExampleChestBlockEntity.INVENTORY_SIZE, usingBefore, usingBefore + 1, usingAfter)), false);
		LOGGER.info("GT6 open chain verified: server-side MenuProvider.createMenu ran without exception for GTExampleChestBlockEntity at {} ({} slots, openers counter closed back to {})",
			target.toShortString(), slotCount, usingAfter);
		return Command.SINGLE_SUCCESS;
	}
}
