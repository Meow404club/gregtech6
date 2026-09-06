package gregtech6.command;

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

import gregtech6.items.tools.GTChiselItem;
import gregtech6.registry.GT6Tools;

/**
 * {@code /gt6chisel} — the chisel stone-gate acceptance command (task p19-chisel-recipes;
 * card-local in command/ like GTBoilerCommand/GTToolCommand). Drives the universal gate
 * through the item's own static dispatch — the P16 same-source convention (the /gt6boiler
 * decalcify arm calls the exact {@code GTBoilerTankBlockEntity.chisel} the item arm calls;
 * here {@code click} runs {@link GTChiselItem#stoneToolClick} through the same
 * {@link UseOnContext} shape {@code useOn} receives, a fake player holding a real
 * gt6:chisel so the durability payment is observable):
 *
 * <ul>
 * <li>{@code click [<pos>] [sneak]} — the fake player holds the chisel, a synthetic
 *     BlockHitResult pins the clicked face, the :224 sneak gate is exercisable through the
 *     trailing {@code sneak} word ({@code setShiftKeyDown(true)}); the report names the
 *     state before → after, the upstream 10000 tool-damage return and the resulting
 *     chisel damage (a conversion pays durabilityPoints(10000) = 25 points).</li>
 * </ul>
 *
 * <p>The variant-transformation assertion itself rides the chain's
 * {@code execute if block <pos> gt6:<stone>[variant=<v>]} predicates; this command's
 * report is the tool-side half (toolDamage + payment).
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6ChiselCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6ChiselCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> tChisel = Commands.literal("gt6chisel")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("click")
				.executes(context -> click(context.getSource(), null, false))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> click(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), false))
					.then(Commands.literal("sneak")
						.executes(context -> click(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), true)))));
		event.getDispatcher().register(tChisel);
		LOGGER.info("Registered GT6 chisel acceptance command /gt6chisel (click [pos] [sneak])");
	}

	/**
	 * The universal-gate click: the fake player holds a gt6:chisel and the item's own
	 * dispatch runs over the synthetic centre hit. The report asserts the upstream 10000
	 * return and the 25-point payment on a conversion, 0/0 on any declined leg.
	 */
	private static int click(CommandSourceStack source, BlockPos pos, boolean aSneak) {
		ServerLevel tLevel = source.getLevel();
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		var tStateBefore = tLevel.getBlockState(tTarget);
		ItemStack tChisel = new ItemStack(GT6Tools.CHISEL.get());
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent(); // a leftover from an earlier command would fake the landing
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tChisel);
		tFakePlayer.setShiftKeyDown(aSneak); // the ToolCompat.java:224 !aSneaking gate
		// the same UseOnContext shape useOn receives: the synthetic hit pins the clicked
		// face at the block pos (isInside=false, the plain four-arg constructor)
		var tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(tTarget), Direction.UP, tTarget, false));
		long tDamage = GTChiselItem.stoneToolClick(tContext);
		var tStateAfter = tLevel.getBlockState(tTarget);
		tFakePlayer.setShiftKeyDown(false);
		int tHeldDamage = tChisel.getDamageValue();
		boolean tConverted = !tStateAfter.equals(tStateBefore);
		String tReport = String.format("gt6chisel click %s sneak=%s at %s: toolDamage=%d, state %s -> %s, chiselDamage=%d/%d",
				tConverted ? "CONVERTED" : "same", aSneak, tTarget.toShortString(),
				tDamage, tStateBefore, tStateAfter,
				tHeldDamage, GTChiselItem.DURABILITY_POINTS);
		if (tConverted != (tDamage > 0) || (tConverted && tHeldDamage != GTChiselItem.durabilityPoints(GTChiselItem.TOOL_DAMAGE_UNIT))
				|| (!tConverted && tHeldDamage != 0)) {
			source.sendFailure(Component.literal("gt6chisel click FAILED: " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("gt6chisel click check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}
}
