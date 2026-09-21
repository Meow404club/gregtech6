
package gregtech6.tileentity.tools;

import java.util.Locale;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.mojang.brigadier.Command;

import gregtech6.fluid.FluidTankGT;
import gregtech6.registry.GT6Kitchen;

/**
 * {@code /gt6kitchen} — the manual-kitchen acceptance command (task
 * p26-kitchen-pot-bowl; the GTMachineCommand :202 machine() shape, console-safe). One
 * literal per carrier — {@code pot} (the steel 8000 L body), {@code pot_wood} (the
 * wooden 4000 L row) and {@code bowl} (the ceramic RM.Mixer body) — carrying:
 *
 * <ul>
 * <li>{@code place [<pos>]} — setBlock the carrier (the GTMachineCommand.place form);</li>
 * <li>{@code input <item> <count> [<pos>]} — push a named stack into input slot 0 (the
 *     manual family has no default feed — the C-Foam pour legs take three distinct
 *     dusts, the row's exact recipe items);</li>
 * <li>{@code fluid fill|draw} — the p14 fluid-driver shape over the kitchen
 *     capability face (the fill goes through the :302 admission doors — the LIVE server
 *     proof of the door predicates the offline JVM cannot carry, see
 *     GT6KitchenBlockEntityTest);</li>
 * <li>{@code interact [<pos>]} — the TOP-face manual round with the null player (the
 *     RCON arm of {@link GT6ManualKitchenBlockEntity#activateChain} — findRecipe +
 *     isRecipeInputEqual + the output landing, the report string IS the verdict);</li>
 * <li>{@code check [<pos>]} — the tank+slot census report.</li>
 * </ul>
 *
 * <p>Capability note: the kitchen carriers expose their faces through the BE getCapability
 * override (forge) / the GT6CapabilityWiring.registerKitchenFaces seam (21.1) — this
 * command reads the SAME faces the tap/Faucet chain (P12 TapFillable) and the barrels do.
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6KitchenCommand {

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> tKitchen = Commands.literal("gt6kitchen")
				.requires(source -> source.hasPermission(2));
		tKitchen.then(carrier("pot", GT6Kitchen.BATHING_POT_STEEL))
				.then(carrier("pot_wood", GT6Kitchen.BATHING_POT_WOOD))
				.then(carrier("bowl", GT6Kitchen.MIXING_BOWL))
				// task p33-food-machines-kitchen — the Juicer joins (RM.Juicer, the manual top-face round)
				.then(carrier("juicer", GT6Kitchen.JUICER));
		event.getDispatcher().register(tKitchen);
	}

	/** One carrier literal with its subcommands (the GTMachineCommand.machine shape, manual-kitchen arms). */
	private static LiteralArgumentBuilder<CommandSourceStack> carrier(String aName, java.util.function.Supplier<? extends Block> aBlock) {
		LiteralArgumentBuilder<CommandSourceStack> tCarrier = Commands.literal(aName);

		// place [<pos>]
		LiteralArgumentBuilder<CommandSourceStack> tPlace = Commands.literal("place");
		tPlace.executes(context -> place(context.getSource(), aBlock, null));
		tPlace.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
				.executes(context -> place(context.getSource(), aBlock,
						net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(context, "pos"))));
		tCarrier.then(tPlace);

		// input <item> <count> [<pos>] — into input slot 0
		LiteralArgumentBuilder<CommandSourceStack> tInput = Commands.literal("input");
		com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> tItem =
				Commands.argument("item", net.minecraft.commands.arguments.ResourceLocationArgument.id());
		com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Integer> tCount =
				Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64));
		tCount.executes(context -> input(context.getSource(),
				net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "item"),
				com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"), null));
		tCount.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
				.executes(context -> input(context.getSource(),
						net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "item"),
						com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"),
						net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(context, "pos"))));
		tItem.then(tCount);
		tInput.then(tItem);
		tCarrier.then(tInput);

		// fluid fill <side> <fluid> <amount> [<pos>] / fluid draw <side> <amount> [<pos>] / fluid stat [<pos>]
		LiteralArgumentBuilder<CommandSourceStack> tFluid = Commands.literal("fluid");
		LiteralArgumentBuilder<CommandSourceStack> tFill = Commands.literal("fill");
		com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> tFillSide =
				Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word());
		com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> tFillFluid =
				Commands.argument("fluid", net.minecraft.commands.arguments.ResourceLocationArgument.id());
		com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Integer> tFillAmount =
				Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1));
		tFillAmount.executes(context -> fluidFill(context.getSource(),
				com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side"),
				net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "fluid"),
				com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount"), null));
		tFillAmount.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
				.executes(context -> fluidFill(context.getSource(),
						com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side"),
						net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "fluid"),
						com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount"),
						net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(context, "pos"))));
		tFillFluid.then(tFillAmount);
		tFillSide.then(tFillFluid);
		tFill.then(tFillSide);
		tFluid.then(tFill);
		LiteralArgumentBuilder<CommandSourceStack> tDraw = Commands.literal("draw");
		com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> tDrawSide =
				Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word());
		com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Integer> tDrawAmount =
				Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1));
		tDrawAmount.executes(context -> fluidDraw(context.getSource(),
				com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side"),
				com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount"), null));
		tDrawAmount.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
				.executes(context -> fluidDraw(context.getSource(),
						com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side"),
						com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount"),
						net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(context, "pos"))));
		tDrawSide.then(tDrawAmount);
		tDraw.then(tDrawSide);
		tFluid.then(tDraw);
		tCarrier.then(tFluid);

		// interact [<pos>] — the top-face manual round, null player (the RCON arm)
		LiteralArgumentBuilder<CommandSourceStack> tInteract = Commands.literal("interact");
		tInteract.executes(context -> interact(context.getSource(), null));
		tInteract.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
				.executes(context -> interact(context.getSource(),
						net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(context, "pos"))));
		tCarrier.then(tInteract);

		// check [<pos>] — the tank + slot census
		LiteralArgumentBuilder<CommandSourceStack> tCheck = Commands.literal("check");
		tCheck.executes(context -> check(context.getSource(), null));
		tCheck.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
				.executes(context -> check(context.getSource(),
						net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(context, "pos"))));
		tCarrier.then(tCheck);

		return tCarrier;
	}

	private static int place(CommandSourceStack aSource, java.util.function.Supplier<? extends Block> aBlock, BlockPos aPos) {
		BlockPos tTarget = aPos != null ? aPos : BlockPos.containing(aSource.getPosition());
		ServerLevel tLevel = aSource.getLevel();
		tLevel.setBlock(tTarget, aBlock.get().defaultBlockState(), 3);
		aSource.sendSuccess(() -> Component.literal("GT6 " + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(aBlock.get()).getPath() + " placed at " + tTarget.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	@javax.annotation.Nullable
	private static GT6ManualKitchenBlockEntity kitchenAt(CommandSourceStack aSource, @javax.annotation.Nullable BlockPos aPos) {
		BlockPos tPos = aPos != null ? aPos : BlockPos.containing(aSource.getPosition());
		BlockEntity tBe = aSource.getLevel().getBlockEntity(tPos);
		return tBe instanceof GT6ManualKitchenBlockEntity tKitchen ? tKitchen : null;
	}

	private static int input(CommandSourceStack aSource, ResourceLocation aItemId, int aCount, @javax.annotation.Nullable BlockPos aPos) throws CommandSyntaxException {
		GT6ManualKitchenBlockEntity tKitchen = kitchenAt(aSource, aPos);
		if (tKitchen == null) {
			aSource.sendFailure(Component.literal("No GT6 kitchen carrier at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		Item tItem = ForgeRegistries.ITEMS.getValue(aItemId);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("Unknown item: " + aItemId));
			return 0;
		}
		ItemStack tFeed = new ItemStack(tItem, aCount);
		for (int i = 0; i < GT6ManualKitchenBlockEntity.INPUT_SLOTS; i++) {
			ItemStack tLeftover = tKitchen.newItemHandler().insertItem(i, tFeed, false);
			if (tLeftover.isEmpty()) {
				String tLine = "GT6 " + tKitchen.getTileEntityName() + " input: " + aCount + "x " + aItemId + " into slot " + i;
				aSource.sendSuccess(() -> Component.literal(tLine), false);
				return Command.SINGLE_SUCCESS;
			}
		}
		aSource.sendFailure(Component.literal("No input slot accepted " + aCount + "x " + aItemId + " (all six blocked or type-mixed)"));
		return 0;
	}

	private static Direction parseSide(String aWord) throws CommandSyntaxException {
		Direction tSide = Direction.byName(aWord.toLowerCase(Locale.ROOT));
		if (tSide == null) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Unknown side: " + aWord)).create();
		return tSide;
	}

	/** The fill driver — through the capability face, i.e. through the :302 admission doors (the LIVE door evidence). */
	private static int fluidFill(CommandSourceStack aSource, String aSideWord, ResourceLocation aFluidId, int aAmount, @javax.annotation.Nullable BlockPos aPos) throws CommandSyntaxException {
		Direction tSide = parseSide(aSideWord);
		GT6ManualKitchenBlockEntity tKitchen = kitchenAt(aSource, aPos);
		if (tKitchen == null) {
			aSource.sendFailure(Component.literal("No GT6 kitchen carrier at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		Fluid tFluid = ForgeRegistries.FLUIDS.getValue(aFluidId);
		if (tFluid == null || tFluid.defaultFluidState() == null || tFluid.defaultFluidState().isEmpty()) {
			aSource.sendFailure(Component.literal("Unknown fluid: " + aFluidId));
			return 0;
		}
		//? if forge {
		net.minecraftforge.fluids.capability.IFluidHandler tHandler = tKitchen.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, tSide).orElse(null);
		//?} else {
		/*net.minecraftforge.fluids.capability.IFluidHandler tHandler = tKitchen.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tSide);
		 *///?}
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the carrier exposes no FLUID_HANDLER on " + tSide));
			return 0;
		}
		int tFilled = tHandler.fill(new net.minecraftforge.fluids.FluidStack(tFluid, aAmount), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		String tLine = String.format("GT6 %s fluid fill at %s face %s: filled %d/%d L of %s%s",
				tKitchen.getTileEntityName(), tKitchen.getBlockPos().toShortString(), tSide, tFilled, aAmount, aFluidId,
				tFilled == 0 ? " (REJECTED)" : " (ACCEPTED)");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int fluidDraw(CommandSourceStack aSource, String aSideWord, int aAmount, @javax.annotation.Nullable BlockPos aPos) throws CommandSyntaxException {
		Direction tSide = parseSide(aSideWord);
		GT6ManualKitchenBlockEntity tKitchen = kitchenAt(aSource, aPos);
		if (tKitchen == null) {
			aSource.sendFailure(Component.literal("No GT6 kitchen carrier at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		//? if forge {
		net.minecraftforge.fluids.capability.IFluidHandler tHandler = tKitchen.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, tSide).orElse(null);
		//?} else {
		/*net.minecraftforge.fluids.capability.IFluidHandler tHandler = tKitchen.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tSide);
		 *///?}
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the carrier exposes no FLUID_HANDLER on " + tSide));
			return 0;
		}
		net.minecraftforge.fluids.FluidStack tDrawn = tHandler.drain(aAmount, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		int tDrawnAmount = tDrawn == null ? 0 : tDrawn.getAmount();
		String tFluid = tDrawn == null || tDrawn.isEmpty() ? "nothing" : ForgeRegistries.FLUIDS.getKey(tDrawn.getFluid()).toString();
		String tLine = String.format("GT6 %s fluid draw at %s face %s: drawn %d/%d L of %s%s",
				tKitchen.getTileEntityName(), tKitchen.getBlockPos().toShortString(), tSide, tDrawnAmount, aAmount, tFluid,
				tDrawnAmount == 0 ? " (REJECTED)" : " (ACCEPTED)");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The TOP-face manual round with the null player — the RCON arm of the activation chain; the report IS the verdict. */
	private static int interact(CommandSourceStack aSource, @javax.annotation.Nullable BlockPos aPos) {
		GT6ManualKitchenBlockEntity tKitchen = kitchenAt(aSource, aPos);
		if (tKitchen == null) {
			aSource.sendFailure(Component.literal("No GT6 kitchen carrier at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		String tReport = tKitchen.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.0F, 0.2F);
		String tLine = "GT6 " + tKitchen.getTileEntityName() + " interact: " + tReport;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The tank + slot census (the acceptance verdict line the RCON chain pins). */
	private static int check(CommandSourceStack aSource, @javax.annotation.Nullable BlockPos aPos) {
		GT6ManualKitchenBlockEntity tKitchen = kitchenAt(aSource, aPos);
		if (tKitchen == null) {
			aSource.sendFailure(Component.literal("No GT6 kitchen carrier at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		StringBuilder tLine = new StringBuilder("GT6 " + tKitchen.getTileEntityName() + " check at " + tKitchen.getBlockPos().toShortString() + ":");
		GT6ManualKitchenBlockEntity tK = tKitchen;
		FluidTankGT[] tAll = tK.allTanks();
		for (int i = 0; i < tAll.length; i++) {
			tLine.append(" tank").append(i).append('=');
			if (tAll[i].has()) tLine.append(ForgeRegistries.FLUIDS.getKey(tAll[i].fluid().getFluid())).append(':').append(tAll[i].getFluidAmount());
			else tLine.append("empty");
			tLine.append('/').append(tAll[i].getCapacity()).append('L');
		}
		for (int i = 0; i < GT6ManualKitchenBlockEntity.SLOTS; i++) {
			ItemStack tStack = tK.inventory().getStackInSlot(i);
			if (!tStack.isEmpty()) tLine.append(" slot").append(i).append('=').append(tStack.getCount()).append('x').append(ForgeRegistries.ITEMS.getKey(tStack.getItem()));
		}
		String tReport = tLine.toString();
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		return Command.SINGLE_SUCCESS;
	}

	private GT6KitchenCommand() {}
}
