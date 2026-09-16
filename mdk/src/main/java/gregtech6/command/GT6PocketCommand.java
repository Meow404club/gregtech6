package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import gregtech6.items.tools.GT6ToolActions;
import gregtech6.items.tools.pocket.GTPocketMultitoolItem;
import gregtech6.registry.GT6Tools;

/**
 * {@code /gt6pocket} — the pocket multitool acceptance command (task
 * p29-w5-t7-pocket-eight; card-local in command/ like GT6ChiselCommand/GTToolCommand —
 * the card's own-file extension, the RCON chains cannot reach a useOn dispatch without
 * it). Every arm drives the EXACT {@link GTPocketMultitoolItem#useOn} dispatch the item
 * runs for a player (the p19 {@code /gt6chisel click} same-source convention), a fake
 * player holding a real pocket stack so the durability axis is observable:
 *
 * <ul>
 * <li>{@code walk [<pos>] [damage]} — the :176-183 ring: the fake player holds the closed
 *     multitool worn to {@code damage} (default 5) and sneak-right-clicks the BARE target
 *     eight times; each hop reports {@code <form>@<damage>}; the check fails unless the
 *     walk visits all eight forms and lands back on the start with the damage KEPT (the
 *     switchForm carrier, the ItemStack.setItem replacement ruling).</li>
 * <li>{@code face <form> [<pos>]} — the per-form probe: the classification / attack /
 *     mining seams for the named form against the block at pos (the faces chain asserts
 *     the knife attack, the file's iron-bars ×3 and the saw's wood+ice set).</li>
 * <li>{@code use <form> [<pos>] [sneak]} — ONE useOn dispatch (the smoke chain): the
 *     zero-behavior arm (a non-sneak chisel-form click at a bare stone PASSes and the
 *     block stays) and the machine-target arm (a sneak click at a BE target must NOT
 *     switch — the mCheckTarget face; the held form survives).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6PocketCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6PocketCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tPocket = Commands.literal("gt6pocket")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("walk")
				.executes(aContext -> walk(aContext.getSource(), null, 5))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> walk(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), 5))
					.then(Commands.argument("damage", IntegerArgumentType.integer(0, 511))
						.executes(aContext -> walk(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								IntegerArgumentType.getInteger(aContext, "damage"))))))
			.then(Commands.literal("face")
				.then(Commands.argument("form", com.mojang.brigadier.arguments.StringArgumentType.word())
					.executes(aContext -> face(aContext.getSource(),
							com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "form"), null))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> face(aContext.getSource(),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "form"),
								BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))))
			.then(Commands.literal("use")
				.then(Commands.argument("form", com.mojang.brigadier.arguments.StringArgumentType.word())
					.executes(aContext -> use(aContext.getSource(),
							com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "form"), null, false))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> use(aContext.getSource(),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "form"),
								BlockPosArgument.getLoadedBlockPos(aContext, "pos"), false))
						.then(Commands.literal("sneak")
							.executes(aContext -> use(aContext.getSource(),
									com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "form"),
									BlockPosArgument.getLoadedBlockPos(aContext, "pos"), true))))));
		aEvent.getDispatcher().register(tPocket);
		LOGGER.info("Registered GT6 pocket acceptance command /gt6pocket (walk, face, use)");
	}

	/** {@code multitool|knife|saw|file|screwdriver|wire_cutter|scissors|chisel} → the form index. */
	private static int parseForm(String aWord) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Integer tForm = switch (aWord.toLowerCase()) {
			case "multitool" -> GTPocketMultitoolItem.MULTITOOL;
			case "knife" -> GTPocketMultitoolItem.KNIFE;
			case "saw" -> GTPocketMultitoolItem.SAW;
			case "file" -> GTPocketMultitoolItem.FILE;
			case "screwdriver" -> GTPocketMultitoolItem.SCREWDRIVER;
			case "wire_cutter" -> GTPocketMultitoolItem.WIRE_CUTTER;
			case "scissors" -> GTPocketMultitoolItem.SCISSORS;
			case "chisel" -> GTPocketMultitoolItem.CHISEL;
			default -> null;
		};
		if (tForm == null) {
			throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
					Component.literal("Unknown pocket form: " + aWord)).create();
		}
		return tForm;
	}

	private static String heldId(ItemStack aStack) {
		return BuiltInRegistries.ITEM.getKey(aStack.getItem()).getPath();
	}

	/**
	 * The ring walk — eight real useOn dispatches, each a FRESH UseOnContext (the context
	 * caches the held stack at construction, UseOnContext.java:24). Fails unless every hop
	 * switched and the eighth landed home with the damage kept.
	 */
	private static int walk(CommandSourceStack aSource, BlockPos aPos, int aDamage) {
		ServerLevel tLevel = aSource.getLevel();
		BlockPos tTarget = aPos != null ? aPos : BlockPos.containing(aSource.getPosition());
		Player tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		ItemStack tHeld = new ItemStack(GT6Tools.POCKET_MULTITOOL.get());
		tHeld.setDamageValue(aDamage);
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tHeld);
		tFakePlayer.setShiftKeyDown(true); // the switch arm's sneak gate
		StringBuilder tReport = new StringBuilder("gt6pocket walk start=pocket_multitool@" + aDamage + ":");
		int tForm = GTPocketMultitoolItem.MULTITOOL;
		boolean tOk = true;
		for (int tHop = 1; tHop <= 8; tHop++) {
			var tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atBottomCenterOf(tTarget), Direction.UP, tTarget, false));
			InteractionResult tResult = tContext.getItemInHand().getItem() instanceof GTPocketMultitoolItem
					? ((GTPocketMultitoolItem) tContext.getItemInHand().getItem()).useOn(tContext)
					: InteractionResult.PASS;
			ItemStack tAfter = tFakePlayer.getMainHandItem();
			tReport.append(" hop").append(tHop).append("=").append(heldId(tAfter)).append("@").append(tAfter.getDamageValue());
			int tExpected = GTPocketMultitoolItem.next(tForm);
			if (tResult != InteractionResult.CONSUME
					|| !tAfter.getItem().equals(GT6Tools.POCKET_FORMS.get(tExpected).get())
					|| tAfter.getDamageValue() != aDamage) {
				tOk = false;
				break;
			}
			tForm = tExpected;
		}
		tReport.append(" result=").append(tOk && tForm == GTPocketMultitoolItem.MULTITOOL ? "RING_OK damage=KEPT" : "BROKEN");
		String tLine = tReport.toString();
		if (!tOk || tForm != GTPocketMultitoolItem.MULTITOOL) {
			aSource.sendFailure(Component.literal(tLine));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The per-form face probe — classification, attack, and the mining seams against the target block. */
	private static int face(CommandSourceStack aSource, String aFormWord, BlockPos aPos) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		int tForm = parseForm(aFormWord);
		ServerLevel tLevel = aSource.getLevel();
		BlockPos tTarget = aPos != null ? aPos : BlockPos.containing(aSource.getPosition());
		BlockState tState = tLevel.getBlockState(tTarget);
		StringBuilder tActions = new StringBuilder();
		for (ToolActionProbe tProbe : ToolActionProbe.PROBES) {
			if (GTPocketMultitoolItem.classifies(tForm, tProbe.action())) {
				tActions.append(tActions.length() == 0 ? "" : ",").append(tProbe.name());
			}
		}
		String tReport = String.format("gt6pocket face %s: form=%d classifies=[%s] attack=%.1f block=%s mines=%s speed=%.1f",
				aFormWord.toLowerCase(), tForm, tActions, GTPocketMultitoolItem.attackDamageOf(tForm),
				String.valueOf(BuiltInRegistries.BLOCK.getKey(tState.getBlock())),
				GTPocketMultitoolItem.mines(tForm, tState),
				GTPocketMultitoolItem.destroySpeedBonus(tForm, tState));
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The face probe's action table (the five modern twin actions; name = the report token). */
	private record ToolActionProbe(String name, net.minecraftforge.common.ToolAction action) {
		private static final ToolActionProbe[] PROBES = {
				new ToolActionProbe("saw", GT6ToolActions.SAW),
				new ToolActionProbe("file", GT6ToolActions.FILE),
				new ToolActionProbe("screwdriver", GT6ToolActions.SCREWDRIVER),
				new ToolActionProbe("cutter", GT6ToolActions.CUTTER),
				new ToolActionProbe("chisel", GT6ToolActions.CHISEL),
		};
	}

	/**
	 * ONE useOn dispatch — the smoke arm: the report names the result, the held form after
	 * (a switch on a bare sneak target, a KEPT form everywhere else) and the block after.
	 */
	private static int use(CommandSourceStack aSource, String aFormWord, BlockPos aPos, boolean aSneak) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		int tForm = parseForm(aFormWord);
		ServerLevel tLevel = aSource.getLevel();
		BlockPos tTarget = aPos != null ? aPos : BlockPos.containing(aSource.getPosition());
		BlockState tBlockBefore = tLevel.getBlockState(tTarget);
		Player tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GT6Tools.POCKET_FORMS.get(tForm).get()));
		tFakePlayer.setShiftKeyDown(aSneak);
		var tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atBottomCenterOf(tTarget), Direction.UP, tTarget, false));
		InteractionResult tResult = tContext.getItemInHand().getItem() instanceof GTPocketMultitoolItem
				? ((GTPocketMultitoolItem) tContext.getItemInHand().getItem()).useOn(tContext)
				: InteractionResult.PASS;
		ItemStack tHeld = tFakePlayer.getMainHandItem();
		BlockState tBlockAfter = tLevel.getBlockState(tTarget);
		String tReport = String.format("gt6pocket use form=%s sneak=%s: result=%s held=%s@%d block %s->%s",
				aFormWord.toLowerCase(), aSneak, tResult, heldId(tHeld), tHeld.getDamageValue(),
				BuiltInRegistries.BLOCK.getKey(tBlockBefore.getBlock()),
				BuiltInRegistries.BLOCK.getKey(tBlockAfter.getBlock()));
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}
}
