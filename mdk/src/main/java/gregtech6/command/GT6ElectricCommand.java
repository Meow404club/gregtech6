package gregtech6.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import gregapi.data.TD;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import gregtech6.items.tools.electric.GT6ElectricToolItem;
import gregtech6.registry.GT6Tools;

/**
 * {@code /gt6electric} — the electric-tool acceptance command (task
 * p29-w5-t6-electric-nineteen; card-local in command/ like GT6DigToolCommand — the
 * fake-player channel: the RCON arms drive the EXACT item surfaces the keyboard player
 * hits, the "the command IS the acceptance channel" ruling):
 *
 * <ul>
 * <li>{@code speed <pos> <tool>} — the mining-face read: getDestroySpeed + the drop
 *     authorization (the isMinableBlock mapping, live). The jackhammer no-ores arm
 *     expects speed=0.0 on an ore block (对矿石块零破坏, the getDigSpeed-0 gate).</li>
 * <li>{@code mine <pos> <tool>} — full-EU tool (charged through the REAL
 *     setEnergyStored face), playerDestroy + removeBlock: the report names the EU
 *     delta (the drain arm = the upstream damage column), the shell damage (the random
 *     wear arm) and the drops (the jackhammer rockGt conversion rides the GLOBAL loot
 *     modifier chain — the seed for the per-tool GLM JSONs). The {@code empty} suffix
 *     drives the zero-EU refusal: mineBlock refuses, the block STAYS.</li>
 * <li>{@code switch <pos> <tool>} — the sneak swap: the fake player sneaks, useOn runs
 *     the Behavior_Switch_Metadata transcription on a bare (BE-less) target; the
 *     report names the twin id + the retained damage and EU charge.</li>
 * <li>{@code wear <tool> <n>} — the random-wear sampling: n seeded rolls through the
 *     SAME rollsWear seam the live break flow drives; the report is the hits/n ratio
 *     for the 1/max(10,quality*20) probability assertion.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6ElectricCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6ElectricCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tElectric = Commands.literal("gt6electric")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("speed")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> speed(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"))))))
			.then(Commands.literal("mine")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> mine(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"), false))
						.then(Commands.literal("empty")
							.executes(aContext -> mine(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"), true))))))
			.then(Commands.literal("switch")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> switchArm(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"))))))
			.then(Commands.literal("wear")
				.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
					.then(Commands.argument("n", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100000))
						.executes(aContext -> wear(aContext.getSource(),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "n"))))));
		aEvent.getDispatcher().register(tElectric);
		LOGGER.info("Registered GT6 electric-tool acceptance command /gt6electric (speed | mine [empty] | switch | wear)");
	}

	/** The snake id → the registered electric tool (the GT6Tools resolver). */
	private static GT6ElectricToolItem toolItem(String aTool) {
		var tRow = GT6Tools.electricTool(aTool.toLowerCase(Locale.ROOT));
		return tRow == null ? null : (GT6ElectricToolItem) tRow.get();
	}

	/** The synthetic centre hit the acceptance commands share (the GT6DigToolCommand shape). */
	private static BlockHitResult hitAt(BlockPos aPos, Direction aSide) {
		return new BlockHitResult(Vec3.atCenterOf(aPos), aSide, aPos, false);
	}

	private static String idOf(Item aItem) {
		return String.valueOf(ForgeRegistries.ITEMS.getKey(aItem));
	}

	/** The mining-face read: the dig speed + the drop authorization, live. */
	private static int speed(CommandSourceStack aSource, BlockPos aPos, String aTool) {
		GT6ElectricToolItem tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6electric: unknown tool id: " + aTool));
			return 0;
		}
		ServerLevel tLevel = aSource.getLevel();
		BlockState tState = tLevel.getBlockState(aPos);
		ItemStack tTool = new ItemStack(tItem);
		tItem.setEnergyStored(TD.Energy.EU, tTool, tItem.getEnergyCapacity(TD.Energy.EU, tTool)); // the CHARGED face read — the empty refusal is the mine-empty arm's
		float tSpeed = tItem.getDestroySpeed(tTool, tState);
		//? if forge {
		boolean tCorrect = tItem.isCorrectToolForDrops(tState);
		//?} else {
		/*boolean tCorrect = tItem.isCorrectToolForDrops(tTool, tState);
		//21.1: the stack parameter joined the signature (the GTPickaxeItem fork).
		*///?}
		String tReport = String.format("gt6electric speed %s on %s at %s: speed=%s, correctForDrops=%s",
				aTool, BuiltInRegistries.BLOCK.getKey(tState.getBlock()), aPos.toShortString(), tSpeed, tCorrect);
		aSource.sendSuccess(() -> Component.literal("gt6electric speed check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The break arm: full-EU (or empty) tool, the destroy drop face, the EU/shell/drops
	 * report. The EU payment rides the item's own mineBlock via the real drain face.
	 */
	private static int mine(CommandSourceStack aSource, BlockPos aPos, String aTool, boolean aEmpty) {
		GT6ElectricToolItem tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6electric: unknown tool id: " + aTool));
			return 0;
		}
		ServerLevel tLevel = aSource.getLevel();
		BlockState tState = tLevel.getBlockState(aPos);
		String tBlockId = String.valueOf(ForgeRegistries.BLOCKS.getKey(tState.getBlock()));
		ItemStack tTool = new ItemStack(tItem);
		if (!aEmpty) {
			tItem.setEnergyStored(TD.Energy.EU, tTool, tItem.getEnergyCapacity(TD.Energy.EU, tTool)); // the REAL face, full
		}
		long tEnergyCharged = tItem.getEnergyStored(TD.Energy.EU, tTool);
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		// snapshot the pre-existing drops so reruns/leftovers cannot double-count
		Set<Integer> tBefore = new HashSet<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(2.0))) {
			tBefore.add(tEntity.getId());
		}
		BlockEntity tBE = tLevel.getBlockEntity(aPos);
		boolean tUsable = tItem.usable(tTool);
		float tSpeed = tItem.getDestroySpeed(tTool, tState);
		boolean tCanBreak = tUsable && tSpeed > 0.0F; // the real break gate: EU + the surface speed
		boolean tBroke = false;
		if (tCanBreak) {
			// the destroy drop face — the GLOBAL loot modifier chain fires here (the
			// jackhammer rockGt conversion); then the item's own break face pays EU/wear
			tState.getBlock().playerDestroy(tLevel, tFakePlayer, aPos, tState, tBE, tTool);
			tLevel.removeBlock(aPos, false);
			tItem.mineBlock(tTool, tLevel, tState, aPos, tFakePlayer);
			tBroke = true;
		} else {
			tItem.mineBlock(tTool, tLevel, tState, aPos, tFakePlayer); // the refusal face — must NOT break
			tBroke = !tLevel.getBlockState(aPos).isAir();
		}
		long tEnergyAfter = tItem.getEnergyStored(TD.Energy.EU, tTool);
		@SuppressWarnings("unused")
		int tDamageBefore = tTool.getDamageValue();
		int tDamageAfter = tTool.getDamageValue();
		Set<String> tDrops = new java.util.TreeSet<>();
		if (tCanBreak) {
			for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(2.0))) {
				if (tBefore.contains(tEntity.getId()) || tEntity.isRemoved()) continue;
				ItemStack tStack = tEntity.getItem();
				tDrops.add(ForgeRegistries.ITEMS.getKey(tStack.getItem()) + " x" + tStack.getCount());
				tEntity.discard();
			}
		}
		tFakePlayer.getInventory().clearContent();
		// the REPORT FORMAT: the deterministic prefix (drained/speed/drops) BEFORE the
		// random shellDamage tail — the chain expects pin only the deterministic span
		String tReport;
		if (tCanBreak) {
			tReport = String.format("gt6electric mine %s on %s at %s: broke=%s, eu %d -> %d (drained %d), speed=%s, drops=[%s], shellDamage %d -> %d",
					aTool, tBlockId, aPos.toShortString(), tBroke, tEnergyCharged, tEnergyAfter, tEnergyCharged - tEnergyAfter,
					tSpeed, String.join(", ", tDrops), tDamageBefore, tDamageAfter);
			aSource.sendSuccess(() -> Component.literal("gt6electric mine check OK: " + tReport), false);
		} else if (tUsable) {
			// charged but OUTSIDE the surface (the no-ores ore arm) — the speed gate refuses
			tReport = String.format("gt6electric mine %s SURFACE-GATED on %s at %s: broke=false, block stays=%s, speed=%s, eu=%d",
					aTool, tBlockId, aPos.toShortString(), !tLevel.getBlockState(aPos).isAir(), tSpeed, tEnergyAfter);
			aSource.sendSuccess(() -> Component.literal("gt6electric mine surface check OK: " + tReport), false);
		} else {
			tReport = String.format("gt6electric mine %s EMPTY-EU on %s at %s: broke=false, block stays=%s, eu=%d",
					aTool, tBlockId, aPos.toShortString(), !tLevel.getBlockState(aPos).isAir(), tEnergyAfter);
			aSource.sendSuccess(() -> Component.literal("gt6electric mine empty check OK: " + tReport), false);
		}
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The sneak swap: sneaking fake player, bare target, the twin + retained charge/wear. */
	private static int switchArm(CommandSourceStack aSource, BlockPos aPos, String aTool) {
		GT6ElectricToolItem tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6electric: unknown tool id: " + aTool));
			return 0;
		}
		ServerLevel tLevel = aSource.getLevel();
		ItemStack tTool = new ItemStack(tItem);
		tItem.setEnergyStored(TD.Energy.EU, tTool, 7777L); // a distinctive charge through the real face
		tTool.setDamageValue(5);
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		tFakePlayer.setShiftKeyDown(true); // the sneak gate (mCheckTarget arm)
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		UseOnContext tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND, hitAt(aPos, Direction.UP));
		InteractionResult tResult = tItem.useOn(tContext);
		ItemStack tHeld = tFakePlayer.getMainHandItem();
		String tHeldId = tHeld.isEmpty() ? "empty" : idOf(tHeld.getItem());
		long tHeldEnergy = tHeld.getItem() instanceof GT6ElectricToolItem tHeldItem
				? tHeldItem.getEnergyStored(TD.Energy.EU, tHeld) : -1;
		int tHeldDamage = tHeld.isEmpty() ? -1 : tHeld.getDamageValue();
		tFakePlayer.setShiftKeyDown(false);
		tFakePlayer.getInventory().clearContent();
		String tExpect = "gt6:" + tItem.spec().aTwinPath();
		boolean tSwitched = tResult == InteractionResult.SUCCESS && tExpect.equals(tHeldId);
		// the verdict rides the switched= literal INSIDE the OK line: the positive arms
		// expect switched=True, the BE-negative arms switched=False (the t3 lesson: the
		// sendFailure FAILED marker is a sweep hard-fail flag, the negative arms cannot
		// ride it)
		String tReport = String.format("gt6electric switch check %s at %s: result=%s, held=%s (expect %s), damage=%d (5), eu=%d (7777), switched=%s",
				aTool, aPos.toShortString(), tResult, tHeldId, tExpect, tHeldDamage, tHeldEnergy, tSwitched);
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The random-wear sampling: n seeded rolls through the live seam, the ratio report. */
	private static int wear(CommandSourceStack aSource, String aTool, int aN) {
		GT6ElectricToolItem tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6electric: unknown tool id: " + aTool));
			return 0;
		}
		net.minecraft.util.RandomSource tRng = net.minecraft.util.RandomSource.create(1905L);
		int tHits = 0;
		for (int i = 0; i < aN; i++) if (GT6ElectricToolItem.rollsWear(tItem.spec(), tRng)) tHits++;
		int tDenominator = tItem.spec().wearDenominator();
		String tReport = String.format("gt6electric wear %s n=%d: hits=%d, ratio=%s, denominator=%d (max(10,quality*20))",
				aTool, aN, tHits, String.format(Locale.ROOT, "%.4f", (double) tHits / aN), tDenominator);
		aSource.sendSuccess(() -> Component.literal("gt6electric wear check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}
}
