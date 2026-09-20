package gregtech6.tileentity.misc;

import java.util.Locale;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.items.behaviors.GT6PlaceablePlacement;

/**
 * {@code /gt6placeables} — the placeables acceptance command (task p32-placeables; the
 * GT6KitchenCommand family form + the GT6SceneSixCommand fake-player channel — the
 * "the command IS the acceptance channel" ruling). The arms drive the REAL faces the
 * keyboard player hits:
 * <ul>
 * <li>{@code place <pos> <face> <item> [count]} — a sneaking fake-player holds the named
 *     stack and drives {@link GT6PlaceablePlacement#trySneakPlace} (the unified dispatch,
 *     the exact event-path code); the report names the resulting block + the pile
 *     contents.</li>
 * <li>{@code stat <pos>} — the census report: the lantern's facing + emitted light (the
 *     光级 assertion), the sandwich's size + bites comparator, the pile's stored stack.</li>
 * <li>{@code eat <pos>} — the sandwich bite face (the BE.bite walk, one bite).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6PlaceablesCommand {

	private GT6PlaceablesCommand() {}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tCmd = Commands.literal("gt6placeables")
				.requires(aSource -> aSource.hasPermission(2));

		// place <clicked> <face> <item[:id]> [count] — the item tail is a GREEDY string
		// (brigadier word() cannot carry the "gt6:" colon; the greedy tail parses the
		// optional count in place())
		LiteralArgumentBuilder<CommandSourceStack> tPlace = Commands.literal("place")
				.then(Commands.argument("clicked", BlockPosArgument.blockPos())
						.then(Commands.argument("face", com.mojang.brigadier.arguments.StringArgumentType.word())
								.then(Commands.argument("item", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
										.executes(aContext -> place(aContext,
												com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "item"))))));
		tCmd.then(tPlace);

		// stat <pos> / eat <pos>
		tCmd.then(Commands.literal("stat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(GT6PlaceablesCommand::stat)));
		tCmd.then(Commands.literal("eat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(GT6PlaceablesCommand::eat)));
		aEvent.getDispatcher().register(tCmd);
	}

	/** The unified-dispatch arm — the sneak-place through GT6PlaceablePlacement.trySneakPlace. */
	private static int place(CommandContext<CommandSourceStack> aContext, String aItemTail) throws CommandSyntaxException {
		// the greedy tail: "<itemId> [count]" — word() cannot carry the namespace colon
		String[] tParts = aItemTail.trim().split("\s+");
		String aItemId = tParts[0];
		int aCount = tParts.length > 1 ? Integer.parseInt(tParts[1]) : 0;
		CommandSourceStack tSource = aContext.getSource();
		ServerLevel tLevel = tSource.getLevel();
		BlockPos tClicked = BlockPosArgument.getLoadedBlockPos(aContext, "clicked");
		Direction tFace = Direction.byName(aContext.getArgument("face", String.class).toLowerCase(Locale.ROOT));
		if (tFace == null) {
			tSource.sendFailure(Component.literal("gt6placeables: unknown face (use down|up|north|south|west|east)"));
			return 0;
		}
		Item tItem = BuiltInRegistries.ITEM.get(new ResourceLocation(aItemId));
		if (tItem == net.minecraft.world.item.Items.AIR) {
			tSource.sendFailure(Component.literal("gt6placeables: unknown item id: " + aItemId));
			return 0;
		}
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		tFakePlayer.setShiftKeyDown(true); // the sneak gate is the face being asserted
		ItemStack tHeld = new ItemStack(tItem, aCount > 0 ? aCount : 1);
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tHeld);
		InteractionResult tResult = GT6PlaceablePlacement.trySneakPlace(tLevel, tFakePlayer, tClicked, tFace, tHeld);
		ItemStack tAfterHand = tFakePlayer.getItemInHand(InteractionHand.MAIN_HAND);
		tFakePlayer.getInventory().clearContent();
		BlockPos tTarget = tClicked.relative(tFace);
		BlockState tPlaced = tLevel.getBlockState(tTarget);
		String tReport = String.format("gt6placeables place %s x%d onto %s face %s: result=%s, target=%s, handLeft=%d",
				aItemId, aCount > 0 ? aCount : 1, tClicked.toShortString(), tFace.getSerializedName(),
				tResult.toString(), tPlaced.getBlock() instanceof GT6PlaceableBlock tPile
						? gregtech6.registry.GT6Placeables.placedId(tPile.kind()) + stackOf(tLevel, tTarget) : tPlaced.getBlock().toString(),
				tAfterHand.getCount());
		if (tResult != InteractionResult.SUCCESS) {
			tSource.sendFailure(Component.literal("gt6placeables place FAILED: " + tReport));
			return 0;
		}
		tSource.sendSuccess(() -> Component.literal("gt6placeables place check OK: " + tReport), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The census report — the 光级/比较器/pile readouts in one arm. */
	private static int stat(CommandContext<CommandSourceStack> aContext) throws CommandSyntaxException {
		CommandSourceStack tSource = aContext.getSource();
		ServerLevel tLevel = tSource.getLevel();
		BlockPos tPos = BlockPosArgument.getLoadedBlockPos(aContext, "pos");
		BlockState tState = tLevel.getBlockState(tPos);
		String tReport;
		if (tState.getBlock() instanceof GT6GregOLanternBlock) {
			tReport = String.format("lantern at %s: facing=%s, lightEmission=%d, blockLightAt=%d",
					tPos.toShortString(),
					tState.getValue(GT6GregOLanternBlock.FACING).getSerializedName(),
					tState.getLightEmission(), tLevel.getMaxLocalRawBrightness(tPos));
		} else if (tState.getBlock() instanceof GT6SandwichBlock tSandwichBlock) {
			GT6SandwichBlockEntity tSandwich = (GT6SandwichBlockEntity) tLevel.getBlockEntity(tPos);
			tReport = String.format("sandwich at %s: size=%d, comparator=%d", tPos.toShortString(),
					tSandwich.size(), tSandwich.comparatorValue());
		} else if (tState.getBlock() instanceof GT6PlaceableBlock tPileBlock) {
			tReport = String.format("pile %s at %s:%s, blockLightAt=%d",
					gregtech6.registry.GT6Placeables.placedId(tPileBlock.kind()), tPos.toShortString(),
					stackOf(tLevel, tPos), tLevel.getMaxLocalRawBrightness(tPos));
		} else {
			tSource.sendFailure(Component.literal("gt6placeables stat FAILED: not a placeables block at "
					+ tPos.toShortString() + " (" + tState.getBlock() + ")"));
			return 0;
		}
		String tFinal = tReport;
		tSource.sendSuccess(() -> Component.literal("gt6placeables stat OK: " + tFinal), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The bite arm — one click of the placed sandwich (the BE.bite face). */
	private static int eat(CommandContext<CommandSourceStack> aContext) throws CommandSyntaxException {
		CommandSourceStack tSource = aContext.getSource();
		ServerLevel tLevel = tSource.getLevel();
		BlockPos tPos = BlockPosArgument.getLoadedBlockPos(aContext, "pos");
		if (!(tLevel.getBlockEntity(tPos) instanceof GT6SandwichBlockEntity tSandwich)) {
			tSource.sendFailure(Component.literal("gt6placeables eat FAILED: no sandwich at " + tPos.toShortString()));
			return 0;
		}
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getFoodData().setFoodLevel(10); // below the cake gate so the bite lands
		boolean tBit = tSandwich.bite(tFakePlayer);
		if (tSandwich.isEmpty() && tBit) tLevel.removeBlock(tPos, false);
		else if (tBit) tLevel.updateNeighbourForOutputSignal(tPos, tLevel.getBlockState(tPos).getBlock());
		int tComparator = tSandwich.comparatorValue();
		String tReport = String.format("gt6placeables eat at %s: bit=%s, sizeNow=%d, comparatorNow=%d",
				tPos.toShortString(), tBit, tSandwich.size(), tComparator);
		if (!tBit) {
			tSource.sendFailure(Component.literal("gt6placeables eat FAILED: " + tReport));
			return 0;
		}
		tSource.sendSuccess(() -> Component.literal("gt6placeables eat check OK: " + tReport), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The pile contents suffix (the stat report face). */
	private static String stackOf(ServerLevel aLevel, BlockPos aPos) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6PlaceableBlockEntity tPile && !tPile.stack().isEmpty()) {
			return ", stack=" + BuiltInRegistries.ITEM.getKey(tPile.stack().getItem()) + " x" + tPile.stack().getCount();
		}
		return ", stack=EMPTY";
	}
}
