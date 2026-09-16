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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
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
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?} else {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
//21.1: same simple names, neoforged package (the fluids subtree is NOT on the
//stonecutter swap table).
*///?}
import org.slf4j.Logger;

import gregtech6.registry.GT6Tools;

/**
 * {@code /gt6scene6} — the scene-tool acceptance command (task p29-w5-t5-scene-six;
 * card-local in command/ like GT6DigToolCommand/GT6ChiselCommand — the fake-player
 * channel: the RCON arms drive the EXACT item surfaces the keyboard player hits, the
 * "the command IS the acceptance channel" ruling):
 *
 * <ul>
 * <li>{@code ignite <pos>} — the flint-and-tinder chance strike: the item's
 *     {@code useOn} in a RETRY loop (the upstream 30% roll, GT6_Main.java:111, makes a
 *     single strike non-assertable; 60 strikes leave a {@code 0.7^60 ≈ 5e-10} failure
 *     rate), the report names the attempts and the landed fire.</li>
 * <li>{@code use <pos> <tool>} — the item's {@code useOn} (the plunger drain arm); the
 *     report names the state before → after AND the durability payment — the plunger's
 *     drain is an INTERNAL block-entity change, so the one-point payment IS the success
 *     signal (the upstream :58 unconditional-payment shape reads the other way).</li>
 * <li>{@code mine <pos> <tool>} — the {@code ServerPlayerGameMode.destroyBlock} face (NOT
 *     the t1 {@code Block.playerDestroy} form: the tripwire DISARM check lives in
 *     {@code Block.playerWillDestroy}, which only the game-mode walk invokes); the loot
 *     context carries the TOOL so the GLOBAL loot modifier chain fires; drops are listed
 *     and DISCARDED (rerun idempotency).</li>
 * <li>{@code shear <pos> <tool>} — the nearest sheep in a 3-block AABB, the vanilla
 *     {@code Player.interactOn} face (Player.java:998 routes the hand to
 *     {@code interactLivingEntity}, the patched shears arm); the new wool ItemEntities
 *     are counted and discarded.</li>
 * <li>{@code fill <pos> <mB>} — the test-fixture face: fills the block entity's fluid
 *     capability with water so the drain arm has something to void (the
 *     GT6CrucibleCommand fill/fillraw precedent).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6SceneSixCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6SceneSixCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tScene = Commands.literal("gt6scene6")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("ignite")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> ignite(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("use")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> use(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"))))))
			.then(Commands.literal("mine")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> mine(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"))))))
			.then(Commands.literal("shear")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> shear(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"))))))
			.then(Commands.literal("fill")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("mB", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64000))
						.executes(aContext -> fill(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "mB"))))));
		aEvent.getDispatcher().register(tScene);
		LOGGER.info("Registered GT6 scene-tool acceptance command /gt6scene6 (ignite | use | mine | shear | fill)");
	}

	/** The snake id → the registered scene-tool item (the GT6Tools registry face). */
	private static Item toolItem(String aTool) {
		return switch (aTool.toLowerCase(Locale.ROOT)) {
			case "scissors" -> GT6Tools.SCISSORS.get();
			case "scoop" -> GT6Tools.SCOOP.get();
			case "plunger" -> GT6Tools.PLUNGER.get();
			case "flint_and_tinder" -> GT6Tools.FLINT_AND_TINDER.get();
			case "rolling_pin" -> GT6Tools.ROLLING_PIN.get();
			case "bending_cylinder" -> GT6Tools.BENDING_CYLINDER.get();
			default -> null;
		};
	}

	/** The synthetic centre hit the acceptance commands share (the GT6DigToolCommand shape). */
	private static BlockHitResult hitAt(BlockPos aPos, Direction aSide) {
		return new BlockHitResult(Vec3.atCenterOf(aPos), aSide, aPos, false);
	}

	/**
	 * The chance strike — the upstream 30% roll makes ONE strike non-assertable, so the
	 * arm retries the item's own useOn until fire lands (the 0.7^60 bound above).
	 */
	private static int ignite(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		Item tItem = GT6Tools.FLINT_AND_TINDER.get();
		ItemStack tTool = new ItemStack(tItem);
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		BlockPos tFirePos = aPos.above();
		int tAttempts = 0;
		final int tMaxAttempts = 60;
		for (; tAttempts < tMaxAttempts; tAttempts++) {
			UseOnContext tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND, hitAt(aPos, Direction.UP));
			tItem.useOn(tContext);
			if (tLevel.getBlockState(tFirePos).is(BlockTags.FIRE)) break;
		}
		int tDamage = tTool.getDamageValue();
		tFakePlayer.getInventory().clearContent();
		boolean tLit = tLevel.getBlockState(tFirePos).is(BlockTags.FIRE);
		String tReport = String.format("gt6scene6 ignite at %s: lit=%s, attempts=%d, toolDamage=%d",
				tFirePos.toShortString(), tLit, tAttempts + (tLit ? 1 : 0), tDamage);
		if (!tLit) {
			aSource.sendFailure(Component.literal("gt6scene6 ignite FAILED (no fire in " + tMaxAttempts + " strikes): " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("gt6scene6 ignite check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The item's own useOn (the plunger drain arm) — the payment is the success signal. */
	private static int use(CommandSourceStack aSource, BlockPos aPos, String aTool) {
		ServerLevel tLevel = aSource.getLevel();
		Item tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6scene6: unknown tool id: " + aTool));
			return 0;
		}
		BlockState tBefore = tLevel.getBlockState(aPos);
		ItemStack tTool = new ItemStack(tItem);
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		UseOnContext tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND, hitAt(aPos, Direction.UP));
		InteractionResult tResult = tItem.useOn(tContext);
		int tDamage = tTool.getDamageValue();
		tFakePlayer.getInventory().clearContent();
		BlockState tAfter = tLevel.getBlockState(aPos);
		String tReport = String.format("gt6scene6 use %s on %s at %s: result=%s, state %s -> %s, toolDamage=%d",
				aTool, tBefore.getBlock(), aPos.toShortString(), tResult, tBefore.getBlock(), tAfter.getBlock(), tDamage);
		if (tResult == InteractionResult.PASS && tDamage == 0) {
			aSource.sendFailure(Component.literal("gt6scene6 use NO-OP: " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("gt6scene6 use check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The game-mode destroy face — the ONLY walk that runs {@code Block.playerWillDestroy}
	 * (the tripwire disarm gate) AND the loot context the GLOBAL loot modifier chain reads.
	 */
	private static int mine(CommandSourceStack aSource, BlockPos aPos, String aTool) {
		ServerLevel tLevel = aSource.getLevel();
		Item tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6scene6: unknown tool id: " + aTool));
			return 0;
		}
		BlockState tState = tLevel.getBlockState(aPos);
		String tBlockId = tState.getBlock().toString();
		ItemStack tTool = new ItemStack(tItem);
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		Set<Integer> tBefore = new HashSet<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(2.0))) {
			tBefore.add(tEntity.getId());
		}
		tFakePlayer.gameMode.destroyBlock(aPos);
		Set<String> tDrops = new java.util.TreeSet<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(2.0))) {
			if (tBefore.contains(tEntity.getId()) || tEntity.isRemoved()) continue;
			ItemStack tStack = tEntity.getItem();
			tDrops.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tStack.getItem()) + " x" + tStack.getCount());
			tEntity.discard();
		}
		int tDamage = tTool.getDamageValue();
		tFakePlayer.getInventory().clearContent();
		String tReport = String.format("gt6scene6 mine %s on %s at %s: drops=[%s], toolDamage=%d",
				aTool, tBlockId, aPos.toShortString(), String.join(", ", tDrops), tDamage);
		aSource.sendSuccess(() -> Component.literal("gt6scene6 mine check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The shear face — the vanilla {@code Player.interactOn} routing to the patched
	 * {@code interactLivingEntity} shears arm; the spawned wool entities are the verdict.
	 */
	private static int shear(CommandSourceStack aSource, BlockPos aPos, String aTool) {
		ServerLevel tLevel = aSource.getLevel();
		Item tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6scene6: unknown tool id: " + aTool));
			return 0;
		}
		Sheep tSheep = tLevel.getEntitiesOfClass(Sheep.class, new AABB(aPos).inflate(3.0))
				.stream().findFirst().orElse(null);
		if (tSheep == null) {
			aSource.sendFailure(Component.literal("gt6scene6 shear FAILED: no sheep near " + aPos.toShortString()));
			return 0;
		}
		ItemStack tTool = new ItemStack(tItem);
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		Set<Integer> tBefore = new HashSet<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, tSheep.getBoundingBox().inflate(2.0))) {
			tBefore.add(tEntity.getId());
		}
		InteractionResult tResult = tFakePlayer.interactOn(tSheep, InteractionHand.MAIN_HAND);
		int tWool = 0;
		Set<String> tDrops = new java.util.TreeSet<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, tSheep.getBoundingBox().inflate(2.0))) {
			if (tBefore.contains(tEntity.getId()) || tEntity.isRemoved()) continue;
			ItemStack tStack = tEntity.getItem();
			tDrops.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tStack.getItem()) + " x" + tStack.getCount());
			if (tStack.is(net.minecraft.world.item.Items.WHITE_WOOL) || tStack.getItem().toString().contains("wool")) tWool += tStack.getCount();
			tEntity.discard();
		}
		tFakePlayer.getInventory().clearContent();
		String tDropsText = String.join(", ", tDrops);
		String tReport = String.format("gt6scene6 shear %s on %s at %s: result=%s, wool=%d, drops=[%s], sheared=%s",
				aTool, net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(tSheep.getType()),
				tSheep.blockPosition().toShortString(), tResult, tWool, tDropsText, tSheep.isSheared());
		if (tWool == 0 && !tSheep.isSheared()) {
			aSource.sendFailure(Component.literal("gt6scene6 shear FAILED (no wool dropped): " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("gt6scene6 shear check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The fixture face — fill the block entity's fluid capability with water (the drain arm's feed). */
	private static int fill(CommandSourceStack aSource, BlockPos aPos, int aMilliBuckets) {
		ServerLevel tLevel = aSource.getLevel();
		BlockEntity tBE = tLevel.getBlockEntity(aPos);
		if (tBE == null) {
			aSource.sendFailure(Component.literal("gt6scene6 fill FAILED: no block entity at " + aPos.toShortString()));
			return 0;
		}
		//? if forge {
		IFluidHandler tHandler = tBE.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null);
		//?} else {
		/*IFluidHandler tHandler = tLevel.getCapability(
				net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, aPos, Direction.UP);
		*///?}
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("gt6scene6 fill FAILED: no fluid handler at " + aPos.toShortString()));
			return 0;
		}
		int tFilled = tHandler.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, aMilliBuckets), IFluidHandler.FluidAction.EXECUTE);
		String tReport = String.format("gt6scene6 fill %d mB water at %s: filled=%d, stored=%d", aMilliBuckets,
				aPos.toShortString(), tFilled, tHandler.getFluidInTank(0).getAmount());
		if (tFilled <= 0) {
			aSource.sendFailure(Component.literal("gt6scene6 fill FAILED (nothing accepted): " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("gt6scene6 fill check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}
}
