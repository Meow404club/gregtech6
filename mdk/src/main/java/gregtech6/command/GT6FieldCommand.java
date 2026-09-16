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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import gregtech6.registry.GT6Tools;

/**
 * {@code /gt6field} — the field-tool acceptance command (task p29-w5-t4-field-five;
 * card-local in command/ like {@link GT6DigToolCommand} — the fake-player channel: the
 * RCON arms drive the EXACT item surfaces the keyboard player hits, the "the command IS
 * the acceptance channel" ruling):
 *
 * <ul>
 * <li>{@code break <pos> <tool>} — the REAL {@code ServerPlayerGameMode.destroyBlock}
 *     face (the plow/sense sweep ride {@code stack.mineBlock} INSIDE that call, the
 *     GT6DigToolCommand mine arm cannot reach it — playerDestroy never invokes
 *     mineBlock). The report names the broken state, the 3x3x3 neighbourhood delta
 *     (the sweep verdict) and the spawned+discarded drops (the GLM conversion verdict,
 *     rerun-idempotent).</li>
 * <li>{@code speed <pos> <tool>} — the mining-face read: getDestroySpeed + the
 *     drop-authorization verdict (the hand-drill declared-empty face live).</li>
 * </ul>
 *
 * <p>Tool ids: hoe / plow / branch_cutter / sense / hand_drill (the GT6Tools registry
 * paths, snake).
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6FieldCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6FieldCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tField = Commands.literal("gt6field")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("break")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> breakBlock(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"))))))
			.then(Commands.literal("speed")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> speed(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"))))));
		aEvent.getDispatcher().register(tField);
		LOGGER.info("Registered GT6 field-tool acceptance command /gt6field (break | speed)");
	}

	/** The snake id → the registered field tool (the GT6Tools registry face). */
	private static Item toolItem(String aTool) {
		return switch (aTool.toLowerCase(java.util.Locale.ROOT)) {
			case "hoe" -> GT6Tools.HOE.get();
			case "plow" -> GT6Tools.PLOW.get();
			case "branch_cutter" -> GT6Tools.BRANCH_CUTTER.get();
			case "sense" -> GT6Tools.SENSE.get();
			case "hand_drill" -> GT6Tools.HAND_DRILL.get();
			default -> null;
		};
	}

	/**
	 * The real destroyBlock face — the sweep rides mineBlock inside this call; each
	 * neighbour break spawns its own (converted) drops. Drops collected + DISCARDED
	 * (rerun idempotency, the GT6DigToolCommand mine shape).
	 */
	private static int breakBlock(CommandSourceStack aSource, BlockPos aPos, String aTool) {
		ServerLevel tLevel = aSource.getLevel();
		Item tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6field: unknown tool id: " + aTool));
			return 0;
		}
		ServerPlayer tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent(); // a leftover stack would fake the conversion gate
		ItemStack tTool = new ItemStack(tItem);
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		// the 3x3x3 neighbourhood census (the sweep verdict): non-air count before vs after
		int tBefore = 0;
		for (BlockPos tNeighbour : BlockPos.betweenClosed(aPos.offset(-1, -1, -1), aPos.offset(1, 1, 1))) {
			if (!tLevel.getBlockState(tNeighbour).isAir()) tBefore++;
		}
		// snapshot the pre-existing drops so reruns/leftovers cannot double-count
		Set<Integer> tSeen = new HashSet<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(3.0))) {
			tSeen.add(tEntity.getId());
		}
		BlockState tState = tLevel.getBlockState(aPos);
		String tBlockId = String.valueOf(ForgeRegistries.BLOCKS.getKey(tState.getBlock()));
		boolean tDestroyed = tFakePlayer.gameMode.destroyBlock(aPos);
		int tAfter = 0;
		for (BlockPos tNeighbour : BlockPos.betweenClosed(aPos.offset(-1, -1, -1), aPos.offset(1, 1, 1))) {
			if (!tLevel.getBlockState(tNeighbour).isAir()) tAfter++;
		}
		Set<String> tDrops = new java.util.TreeSet<>();
		int tDropCount = 0;
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(3.0))) {
			if (tSeen.contains(tEntity.getId()) || tEntity.isRemoved()) continue;
			ItemStack tStack = tEntity.getItem();
			tDropCount += tStack.getCount();
			tDrops.add(ForgeRegistries.ITEMS.getKey(tStack.getItem()) + " x" + tStack.getCount());
			tEntity.discard();
		}
		tFakePlayer.getInventory().clearContent();
		String tReport = String.format("gt6field break %s on %s at %s: destroyed=%s, neighbours %d -> %d (broke %d), drops=[%s] (count %d), toolDamage=%d/%d",
				aTool, tBlockId, aPos.toShortString(), tDestroyed, tBefore, tAfter, tBefore - tAfter,
				String.join(", ", tDrops), tDropCount, tTool.getDamageValue(), tItem.getMaxDamage(new ItemStack(tItem)));
		if (!tDestroyed) {
			aSource.sendFailure(Component.literal("gt6field break FAILED (destroyBlock false): " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("gt6field break check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The mining-face read: the dig speed + the drop authorization, live. */
	private static int speed(CommandSourceStack aSource, BlockPos aPos, String aTool) {
		ServerLevel tLevel = aSource.getLevel();
		Item tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6field: unknown tool id: " + aTool));
			return 0;
		}
		BlockState tState = tLevel.getBlockState(aPos);
		ItemStack tTool = new ItemStack(tItem);
		float tSpeed = tItem.getDestroySpeed(tTool, tState);
		//? if forge {
		boolean tCorrect = tItem.isCorrectToolForDrops(tState);
		//?} else {
		/*boolean tCorrect = tItem.isCorrectToolForDrops(tTool, tState);
		//21.1: the stack parameter joined the signature (the GTCrowbarItem fork).
		*///?}
			String tReport = String.format("gt6field speed %s on %s at %s: speed=%s, correctForDrops=%s",
					aTool, BuiltInBlockId(tState), aPos.toShortString(), tSpeed, tCorrect);
		aSource.sendSuccess(() -> Component.literal("gt6field speed check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	private static String BuiltInBlockId(BlockState aState) {
		return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(aState.getBlock()).toString();
	}
}
