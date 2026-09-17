package gregtech6.command;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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

import gregtech6.items.tools.GT6ToolLadder;
import gregtech6.registry.GT6Tools;

import gregapi.oredict.OreDictMaterial;

/**
 * {@code /gt6dig} — the dig-tool acceptance command (task p29-w5-t1-dig-six; card-local
 * in command/ like GT6ChiselCommand/GTToolCommand — the fake-player channel: the RCON
 * arms drive the EXACT item surfaces the keyboard player hits, the
 * "the command IS the acceptance channel" ruling):
 *
 * <ul>
 * <li>{@code use <pos> <tool>} — the item's {@code useOn} (path/torch conversion arms);
 *     the report names the state before → after and the durability payment (one point
 *     per conversion, the 10000=1 mapping).</li>
 * <li>{@code place <pos> <tool>} — the torch arm: the fake player holds the tool AND a
 *     torch stack, {@code useOn} runs the inventory scan; the report names torchPlaced
 *     + the torch consumption. {@code bare} instead of a tool id runs the negative
 *     (empty inventory, no torch).</li>
 * <li>{@code mine <pos> <tool> [hand]} — the chisel-mine face (Block.playerDestroy +
 *     removeBlock — the ServerPlayerGameMode.destroyBlock drop face, dropResources
 *     consults the table with THIS_ENTITY + TOOL context, the p21-chisel-drop-conversion
 *     shape); the spawned drops are counted, DISCARDED (rerun idempotency) and listed —
 *     the report IS the conversion verdict.</li>
 * <li>{@code speed <pos> <tool>} — the mining-face read: getDestroySpeed + the
 *     drop-authorization verdict (the isMinableBlock mapping, live).</li>
 * </ul>
 *
 * <p>Tool ids: pickaxe / pickaxe_gem / pickaxe_construction / shovel / spade /
 * universal_spade (the GT6Tools registry paths, snake). The loot-conversion arms ride
 * the GLOBAL loot modifier chain — mine with the right tool and the spawned drops are
 * the converted ones (the seed for the per-tool GLM JSONs).
 *
 * <p>MATERIAL ARMS (task p31-dig-ladder): {@code speed <pos> <tool> <material>} and
 * {@code mine <pos> <tool> <material>} stamp the {@code GT.ToolStats} identity through
 * the {@link GT6ToolLadder#stampIdentity} seam (the same face the material-tool recipes
 * assemble through) before the read — the per-material level/speed/durability
 * assertions. The identity-less three-arg forms stay the steel fallback arms.
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6DigToolCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6DigToolCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tDig = Commands.literal("gt6dig")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("use")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> use(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"))))))
			.then(Commands.literal("place")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"))))))
			.then(Commands.literal("mine")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> mine(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"), false, null))
						.then(Commands.literal("hand")
							.executes(aContext -> mine(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									"bare", true, null)))
						.then(Commands.argument("material", com.mojang.brigadier.arguments.StringArgumentType.word())
							.executes(aContext -> mine(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"), false,
									com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "material")))))))
			.then(Commands.literal("speed")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> speed(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"), null))
						.then(Commands.argument("material", com.mojang.brigadier.arguments.StringArgumentType.word())
							.executes(aContext -> speed(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"),
									com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "material")))))));
		aEvent.getDispatcher().register(tDig);
		LOGGER.info("Registered GT6 dig-tool acceptance command /gt6dig (use | place | mine [hand] | speed)");
	}

	/** The snake id → the registered tool item (the GT6Tools registry face; "bare" = empty hand). */
	private static Item toolItem(String aTool) {
		return switch (aTool.toLowerCase(Locale.ROOT)) {
			case "pickaxe" -> GT6Tools.PICKAXE.get();
			case "pickaxe_gem" -> GT6Tools.PICKAXE_GEM.get();
			case "pickaxe_construction" -> GT6Tools.PICKAXE_CONSTRUCTION.get();
			case "shovel" -> GT6Tools.SHOVEL.get();
			case "spade" -> GT6Tools.SPADE.get();
			case "universal_spade" -> GT6Tools.UNIVERSAL_SPADE.get();
			default -> null;
		};
	}

	/**
	 * The tool stack builder: an identity-less stack when no material is named (the
	 * steel fallback arm), or the {@link GT6ToolLadder#stampIdentity}-stamped ladder
	 * stack (the form's own durability multiplier through the LadderTool face).
	 *
	 * @return null when the material name does not resolve (the caller reports it).
	 */
	private static ItemStack identityStack(Item aItem, String aMaterial) {
		ItemStack tStack = new ItemStack(aItem);
		if (aMaterial == null) return tStack;
		OreDictMaterial tMaterial = OreDictMaterial.get(aMaterial);
		if (tMaterial == null || tMaterial == gregapi.data.MT.NULL) return null;
		float tMultiplier = aItem instanceof GT6ToolLadder.LadderTool tTool ? tTool.durabilityMultiplier() : 1.0F;
		return GT6ToolLadder.stampIdentity(tStack, tMaterial, tMultiplier);
	}

	/** The synthetic centre hit the acceptance commands share (the GT6ChiselCommand shape). */
	private static BlockHitResult hitAt(BlockPos aPos, Direction aSide) {
		return new BlockHitResult(Vec3.atCenterOf(aPos), aSide, aPos, false);
	}

	/** The item's own useOn with the tool in the main hand (the path/torch arms). */
	private static int use(CommandSourceStack aSource, BlockPos aPos, String aTool) {
		ServerLevel tLevel = aSource.getLevel();
		Item tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6dig: unknown tool id: " + aTool));
			return 0;
		}
		BlockState tBefore = tLevel.getBlockState(aPos);
		ItemStack tTool = new ItemStack(tItem);
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent(); // a leftover torch would fake the torch arm
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		UseOnContext tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND, hitAt(aPos, Direction.UP));
		InteractionResult tResult = tItem.useOn(tContext);
		BlockState tAfter = tLevel.getBlockState(aPos);
		int tDamage = tTool.getDamageValue();
		tFakePlayer.getInventory().clearContent();
		boolean tChanged = !tBefore.equals(tAfter);
		String tReport = String.format("gt6dig use %s on %s at %s: result=%s, state %s -> %s, toolDamage=%d/%d",
				tChanged ? "CONVERTED" : "same", aTool, aPos.toShortString(), tResult, tBefore.getBlock(),
				tAfter.getBlock(), tDamage, tTool.getMaxDamage());
		if (!tChanged && tResult == InteractionResult.PASS && tDamage == 0) {
			aSource.sendFailure(Component.literal("gt6dig use NO-OP: " + tReport));
			return 0;
		}
		if (!tChanged && tDamage == 0) {
			aSource.sendFailure(Component.literal("gt6dig use FAILED (no conversion, no payment): " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("gt6dig use check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The torch arm: the tool + a torch stack in the inventory, then the item's useOn. */
	private static int place(CommandSourceStack aSource, BlockPos aPos, String aTool) {
		ServerLevel tLevel = aSource.getLevel();
		Item tTorch = net.minecraft.world.item.Items.TORCH;
		ItemStack tTool;
		if (!"bare".equalsIgnoreCase(aTool)) {
			Item tItem = toolItem(aTool);
			if (tItem == null) {
				aSource.sendFailure(Component.literal("gt6dig: unknown tool id: " + aTool));
				return 0;
			}
			tTool = new ItemStack(tItem);
		} else {
			tTool = ItemStack.EMPTY;
		}
		BlockPos tSpot = aPos.above(); // the torch lands on TOP of the clicked floor block
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		if (!tTool.isEmpty()) tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		tFakePlayer.getInventory().add(new ItemStack(tTorch, 4)); // the scan target (upstream hotbar walk)
		UseOnContext tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND, hitAt(aPos, Direction.UP));
		InteractionResult tResult = tTool.isEmpty() ? InteractionResult.PASS : tTool.getItem().useOn(tContext);
		BlockState tLanded = tLevel.getBlockState(tSpot);
		int tTorchesLeft = 0;
		for (int tSlot = 0; tSlot < tFakePlayer.getInventory().getContainerSize(); tSlot++) {
			ItemStack tStack = tFakePlayer.getInventory().getItem(tSlot);
			if (tStack.is(tTorch)) tTorchesLeft += tStack.getCount();
		}
		tFakePlayer.getInventory().clearContent();
		boolean tPlaced = tLanded.getBlock() == net.minecraft.world.level.block.Blocks.TORCH;
		String tReport = String.format("gt6dig place %s on %s at %s: torchPlaced=%s, torchesLeft=%d (of 4), landed=%s, result=%s",
				aTool, aPos.toShortString(), tSpot.toShortString(), tPlaced, tTorchesLeft, tLanded.getBlock(), tResult);
		if (!tPlaced && tTorchesLeft == 4 && !tTool.isEmpty()) {
			aSource.sendFailure(Component.literal("gt6dig place FAILED (torch untouched): " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("gt6dig place check " + (tPlaced ? "OK" : "negative-OK") + ": " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The mining-drop face (the chisel-mine shape): the drops list IS the verdict. */
	private static int mine(CommandSourceStack aSource, BlockPos aPos, String aTool, boolean aBareHand, String aMaterial) {
		ServerLevel tLevel = aSource.getLevel();
		BlockState tState = tLevel.getBlockState(aPos);
		String tBlockId = String.valueOf(ForgeRegistries.BLOCKS.getKey(tState.getBlock()));
		ItemStack tTool = ItemStack.EMPTY;
		if (!aBareHand) {
			Item tItem = toolItem(aTool);
			if (tItem == null) {
				aSource.sendFailure(Component.literal("gt6dig: unknown tool id: " + aTool));
				return 0;
			}
			tTool = identityStack(tItem, aMaterial);
			if (tTool == null) {
				aSource.sendFailure(Component.literal("gt6dig: unknown material: " + aMaterial));
				return 0;
			}
		}
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent(); // a leftover from an earlier command would fake the arm
		if (!tTool.isEmpty()) tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		// snapshot the pre-existing drops so reruns/leftovers cannot double-count
		Set<Integer> tBefore = new HashSet<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(2.0))) {
			tBefore.add(tEntity.getId());
		}
		// the ServerPlayerGameMode.destroyBlock drop face: dropResources consults the loot
		// table with THIS_ENTITY + TOOL context — the GLOBAL loot modifier chain fires here
		BlockEntity tBE = tLevel.getBlockEntity(aPos);
		tState.getBlock().playerDestroy(tLevel, tFakePlayer, aPos, tState, tBE, tTool);
		tLevel.removeBlock(aPos, false);
		// collect and DISCARD the new drops (the report is the assertion surface)
		Set<String> tDrops = new java.util.TreeSet<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(2.0))) {
			if (tBefore.contains(tEntity.getId()) || tEntity.isRemoved()) continue;
			ItemStack tStack = tEntity.getItem();
			tDrops.add(ForgeRegistries.ITEMS.getKey(tStack.getItem()) + " x" + tStack.getCount());
			tEntity.discard();
		}
		tFakePlayer.getInventory().clearContent();
		String tDropsText = String.join(", ", tDrops);
		String tReport = String.format("gt6dig mine %s hand=%s on %s at %s: drops=[%s], toolDamage=%d/%d",
				aBareHand ? "hand" : aTool, aBareHand, tBlockId, aPos.toShortString(), tDropsText,
				tTool.getDamageValue(), tTool.getMaxDamage());
		aSource.sendSuccess(() -> Component.literal("gt6dig mine check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The mining-face read: the dig speed + the drop authorization, live. */
	private static int speed(CommandSourceStack aSource, BlockPos aPos, String aTool, String aMaterial) {
		ServerLevel tLevel = aSource.getLevel();
		Item tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6dig: unknown tool id: " + aTool));
			return 0;
		}
		ItemStack tTool = identityStack(tItem, aMaterial);
		if (tTool == null) {
			aSource.sendFailure(Component.literal("gt6dig: unknown material: " + aMaterial));
			return 0;
		}
		BlockState tState = tLevel.getBlockState(aPos);
		float tSpeed = tItem.getDestroySpeed(tTool, tState);
		// the stack-aware drop authorization (forge: the IForgeItem overload; 1.21.1: the vanilla signature)
		boolean tCorrect = tItem.isCorrectToolForDrops(tTool, tState);
		String tReport = String.format("gt6dig speed %s[%s] on %s at %s: speed=%s, correctForDrops=%s, maxDamage=%d",
				aTool, aMaterial == null ? "steel-fallback" : aMaterial, BuiltInRegistries.BLOCK.getKey(tState.getBlock()),
				aPos.toShortString(), tSpeed, tCorrect, tTool.getMaxDamage());
		aSource.sendSuccess(() -> Component.literal("gt6dig speed check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}
}
