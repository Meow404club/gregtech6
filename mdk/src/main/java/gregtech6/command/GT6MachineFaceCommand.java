package gregtech6.command;

import java.util.HashSet;
import java.util.Set;

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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import gregtech6.registry.GT6Tools;

/**
 * {@code /gt6machineface} — the machine-face-four acceptance command (task
 * p29-w5-t3-machine-face-four; card-local in command/, the GT6ChiselCommand shape). RCON
 * sessions have no player, so each arm drives the item's own {@code useOn} dispatch through
 * a fake player holding a REAL registered stack (the p19_chisel "same-source convention":
 * the exact UseOnContext the click delivers):
 *
 * <ul>
 * <li>{@code rotate <pos>} — the soft hammer stand-in (the :125 "rotate vanilla-ish things"
 *     face): the fake player holds gt6:soft_hammer, the report names the state before →
 *     after plus the one-point payment; a block outside the cut list PASSes with the state
 *     unchanged (the RED LINE external pool verdict).</li>
 * <li>{@code inspect <pos>} — the magnifying-glass zero-change face: the fake player holds
 *     gt6:magnifying_glass; the arm FAILS unless the blockstate, the item-entity set AND
 *     the held stack's damage are all untouched (the acceptance's zero-change assertions,
 *     the AHA/HMM sound is the only observable).</li>
 * <li>{@code collect <pos>} — the pincers dragon-egg face (the canCollect arm): the fake
 *     player holds gt6:pincers SNEAKING (the vanilla teleport owns the non-sneak click);
 *     the egg must pop as an item entity (counted, DISCARDED for rerun idempotency) and the
 *     block must clear (the one-point payment observable on the held stack).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6MachineFaceCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6MachineFaceCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> tFace = Commands.literal("gt6machineface")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("rotate")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> rotate(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))
			.then(Commands.literal("inspect")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> inspect(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))
			.then(Commands.literal("collect")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> collect(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		event.getDispatcher().register(tFace);
		LOGGER.info("Registered GT6 machine-face acceptance command /gt6machineface (rotate|inspect|collect <pos>)");
	}

	/** A fake player + synthetic centre hit, the exact UseOnContext the click delivers (the GT6ChiselCommand shape). */
	private static UseOnContext clickContext(ServerLevel aLevel, ItemStack aStack, BlockPos aPos, boolean aSneak) {
		var tFakePlayer = FakePlayerFactory.getMinecraft(aLevel);
		tFakePlayer.getInventory().clearContent(); // a leftover from an earlier command would fake the arm
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, aStack);
		tFakePlayer.setShiftKeyDown(aSneak);
		return new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(aPos), Direction.UP, aPos, false));
	}

	private static Set<Integer> itemEntityIds(ServerLevel aLevel, BlockPos aPos) {
		Set<Integer> rIds = new HashSet<>();
		for (ItemEntity tEntity : aLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(2.0))) {
			rIds.add(tEntity.getId());
		}
		return rIds;
	}

	/** The soft-hammer rotation arm: the state before → after plus the payment, the pass-through verdict on no-ops. */
	private static int rotate(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		BlockState tBefore = tLevel.getBlockState(aPos);
		ItemStack tHammer = new ItemStack(GT6Tools.SOFT_HAMMER.get());
		InteractionResult tResult = GT6Tools.SOFT_HAMMER.get().useOn(clickContext(tLevel, tHammer, aPos, false));
		BlockState tAfter = tLevel.getBlockState(aPos);
		boolean tRotated = !tAfter.equals(tBefore);
		String tReport = String.format("gt6machineface rotate %s at %s: state %s -> %s, result=%s, softHammerDamage=%d/%d",
				tRotated ? "ROTATED" : "same", aPos.toShortString(), tBefore, tAfter, tResult, tHammer.getDamageValue(),
				gregtech6.items.tools.GTSoftHammerItem.DURABILITY_POINTS);
		if (tRotated != (tResult.consumesAction()) || (tRotated && tHammer.getDamageValue() != 1) || (!tRotated && tHammer.getDamageValue() != 0)) {
			aSource.sendFailure(Component.literal("gt6machineface rotate FAILED: " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("gt6machineface rotate check OK: " + tReport), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The magnifying-glass arm: FAILS unless state + entity set + held damage are all untouched. */
	private static int inspect(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		BlockState tBefore = tLevel.getBlockState(aPos);
		Set<Integer> tBeforeEntities = itemEntityIds(tLevel, aPos);
		ItemStack tGlass = new ItemStack(GT6Tools.MAGNIFYING_GLASS.get());
		InteractionResult tResult = GT6Tools.MAGNIFYING_GLASS.get().useOn(clickContext(tLevel, tGlass, aPos, false));
		BlockState tAfter = tLevel.getBlockState(aPos);
		boolean tStateSame = tAfter.equals(tBefore);
		boolean tEntitiesSame = tBeforeEntities.equals(itemEntityIds(tLevel, aPos));
		boolean tStackUntouched = tGlass.getDamageValue() == 0 && tGlass.getCount() == 1;
		boolean tPass = tStateSame && tEntitiesSame && tStackUntouched && !tResult.consumesAction();
		String tReport = String.format("gt6machineface inspect at %s: state_same=%s entities_same=%s stack_untouched=%s result=%s",
				aPos.toShortString(), tStateSame, tEntitiesSame, tStackUntouched, tResult);
		if (!tPass) {
			aSource.sendFailure(Component.literal("gt6machineface inspect FAILED: " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("gt6machineface inspect check OK: " + tReport), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The pincers dragon-egg arm: the sneaking fake player pops exactly one egg item, the block clears. */
	private static int collect(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		BlockState tBefore = tLevel.getBlockState(aPos);
		Set<Integer> tBeforeEntities = itemEntityIds(tLevel, aPos);
		ItemStack tPincers = new ItemStack(GT6Tools.PINCERS.get());
		InteractionResult tResult = GT6Tools.PINCERS.get().useOn(clickContext(tLevel, tPincers, aPos, true)); // sneak = the collect arm
		Set<String> tDrops = new HashSet<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(2.0))) {
			if (tBeforeEntities.contains(tEntity.getId()) || tEntity.isRemoved()) continue;
			tDrops.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tEntity.getItem().getItem()) + " x" + tEntity.getItem().getCount());
			tEntity.discard(); // the report is the assertion surface (rerun idempotency)
		}
		boolean tEggCleared = !tLevel.getBlockState(aPos).equals(tBefore);
		boolean tVerdict = tDrops.equals(Set.of("minecraft:dragon_egg x1")) && tEggCleared && tResult.consumesAction();
		String tReport = String.format("gt6machineface collect at %s on %s: drops=[%s], egg_cleared=%s, result=%s, pincersDamage=%d",
				aPos.toShortString(), tBefore.getBlock(), String.join(", ", tDrops), tEggCleared, tResult, tPincers.getDamageValue());
		if (!tVerdict) {
			aSource.sendFailure(Component.literal("gt6machineface collect FAILED: " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("gt6machineface collect check OK: " + tReport), false);
		return Command.SINGLE_SUCCESS;
	}
}
