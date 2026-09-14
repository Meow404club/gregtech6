package gregtech6.tileentity.multiblocks;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;

import javax.annotation.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

import gregtech6.registry.GT6Tanks;
import gregtech6.tileentity.multiblocks.GTTankValveBlockEntity;

/**
 * {@code /gt6tankvalve} — the Tank Main Valve acceptance command (task
 * p29-w3-tank-valves, the GTBarrelCommand/GTMultiBlockCommand RCON shape). Game-bus
 * listener, SELF-CONTAINED per ADR-P3-4: a new file beside the family, so the
 * GTMultiBlockCommand shared seam stays untouched this wave (the card FILES_SCOPE
 * ruling — cards ③④⑤⑥ own their own arms; a per-family command file eliminates the
 * append conflict the boiler arm would have created).
 *
 * <ul>
 * <li>{@code form <pos>} — the linking pass {@code checkStructure(true)} over the
 *     PRE-PLACED wall shell (the RCON setblock build; the 3x3x3's declared pattern also
 *     takes the generic /gt6multiblock form arm with its wand stock — this arm is the
 *     radius-2 5x5x5's only form path, the pattern-less escape hatch the class doc
 *     declares).</li>
 * <li>{@code fill <pos> <fluid> <amount>} / {@code draw <pos> <amount>} — the tank access
 *     through the fluid capability (the side-less query).</li>
 * <li>{@code stat <pos>} — amount/capacity/temperature/melting point/proof flags/formed/
 *     facing, the meltdown and the destruction arms' assertion surface.</li>
 * <li>{@code tick <pos> <ticks>} — the manual dispatcher drive (the updateEntity arm,
 *     the coke-oven tick shape).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTTankValveCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTTankValveCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tValve = Commands.literal("gt6tankvalve")
				.requires(aSource -> aSource.hasPermission(2))
				.then(Commands.literal("form")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> form(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("fill")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.then(Commands.argument("fluid", ResourceLocationArgument.id())
										.then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
												.executes(aContext -> fill(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
														ResourceLocationArgument.getId(aContext, "fluid"),
														com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "amount")))))))
				.then(Commands.literal("draw")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
										.executes(aContext -> draw(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
												com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "amount"))))))
				.then(Commands.literal("stat")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("tick")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 10000))
										.executes(aContext -> tick(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
												com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "ticks"))))));
		aEvent.getDispatcher().register(tValve);
	}

	@Nullable
	private static GTTankValveBlockEntity valveAt(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (tLevel.getBlockEntity(aPos) instanceof GTTankValveBlockEntity tValve) return tValve;
		return null;
	}

	/** {@code form <pos>} — the linking check over the pre-placed shell. */
	private static int form(CommandSourceStack aSource, BlockPos aPos) {
		GTTankValveBlockEntity tValve = valveAt(aSource, aPos);
		if (tValve == null) {
			aSource.sendFailure(Component.literal("No GTTankValveBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		boolean tOkay = tValve.checkStructure(true);
		String tReport = String.format("GT6 tankvalve form at %s: formed=%s radius=%d wall=%s",
				aPos.toShortString(), tOkay, tValve.radius(), tValve.getWallBlock());
		if (!tOkay) {
			aSource.sendFailure(Component.literal(tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** {@code fill <pos> <fluid> <amount>} — through the fluid capability, the executed fill. */
	private static int fill(CommandSourceStack aSource, BlockPos aPos, ResourceLocation aFluidId, int aAmount) {
		GTTankValveBlockEntity tValve = valveAt(aSource, aPos);
		if (tValve == null) {
			aSource.sendFailure(Component.literal("No GTTankValveBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		net.minecraft.world.level.material.Fluid tFluid = ForgeRegistries.FLUIDS.getValue(aFluidId);
		if (tFluid == null || tFluid.defaultFluidState() == null || tFluid.defaultFluidState().isEmpty()) {
			aSource.sendFailure(Component.literal("Unknown fluid: " + aFluidId));
			return 0;
		}
		//? if forge {
		IFluidHandler tHandler = tValve.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
		//?} else {
		/*IFluidHandler tHandler = tValve.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tValve.getBlockPos(), null);
		 *///?}
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the valve exposes no FLUID_HANDLER"));
			return 0;
		}
		int tFilled = tHandler.fill(new FluidStack(tFluid, aAmount), FluidAction.EXECUTE);
		String tLine = String.format("GT6 tankvalve fill at %s: filled %d/%d L of %s%s, tank holds %d L",
				aPos.toShortString(), tFilled, aAmount, aFluidId,
				tFilled == 0 ? " (REJECTED)" : " (ACCEPTED)", tValve.mTank.amount());
		if (tFilled == 0) {
			aSource.sendFailure(Component.literal(tLine));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** {@code draw <pos> <amount>} — through the fluid capability, the executed drain. */
	private static int draw(CommandSourceStack aSource, BlockPos aPos, int aAmount) {
		GTTankValveBlockEntity tValve = valveAt(aSource, aPos);
		if (tValve == null) {
			aSource.sendFailure(Component.literal("No GTTankValveBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		//? if forge {
		IFluidHandler tHandler = tValve.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
		//?} else {
		/*IFluidHandler tHandler = tValve.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tValve.getBlockPos(), null);
		 *///?}
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the valve exposes no FLUID_HANDLER"));
			return 0;
		}
		FluidStack tDrawn = tHandler.drain(aAmount, FluidAction.EXECUTE);
		int tGot = tDrawn == null ? 0 : tDrawn.getAmount();
		String tFluid = tDrawn == null || tDrawn.isEmpty() ? "nothing" : ForgeRegistries.FLUIDS.getKey(tDrawn.getFluid()).toString();
		String tLine = String.format("GT6 tankvalve draw at %s: drained %d/%d L of %s%s, tank holds %d L",
				aPos.toShortString(), tGot, aAmount, tFluid,
				tGot == 0 ? " (REJECTED)" : " (ACCEPTED)", tValve.mTank.amount());
		if (tGot == 0) {
			aSource.sendFailure(Component.literal(tLine));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** {@code stat <pos>} — the full assertion surface. */
	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		GTTankValveBlockEntity tValve = valveAt(aSource, aPos);
		if (tValve == null) {
			aSource.sendFailure(Component.literal("No GTTankValveBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		String tContent = tValve.mTank.isEmpty() ? "-" : tValve.mTank.amount() + "mB "
				+ ForgeRegistries.FLUIDS.getKey(tValve.mTank.fluid().getFluid());
		String tLine = String.format(
				"GT6 tankvalve stat at %s: tank=[%s] capacity=%d okay=%s facing=%d temp=%dK melt=%dK gasproof=%s acidproof=%s plasmaproof=%s magicproof=%s simple_only=%s",
				aPos.toShortString(), tContent, tValve.mTank.capacity(), tValve.mStructureOkay, tValve.mFacing,
				tValve.mTank.isEmpty() ? 0 : GTTankValveBlockEntity.fluidTemperature(tValve.mTank.getFluid()),
				tValve.meltingPoint(),
				tValve.mGasProof, tValve.mAcidProof, tValve.mPlasmaProof, tValve.mMagicProof,
				tValve.row() != null && tValve.row().onlySimple());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** {@code tick <pos> <ticks>} — the manual dispatcher drive (the meltdown/destruction arms' trigger). */
	private static int tick(CommandSourceStack aSource, BlockPos aPos, int aTicks) {
		GTTankValveBlockEntity tValve = valveAt(aSource, aPos);
		if (tValve == null) {
			// a destroyed valve (the acid/meltdown arms) reads as "no BE" — the
			// destruction assertion is the execute if block probe on the RCON side
			aSource.sendFailure(Component.literal("No GTTankValveBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		for (int i = 0; i < aTicks; i++) tValve.updateEntity();
		String tReport = String.format("GT6 tankvalve ticked %d at %s: timer=%d okay=%s tank=%d/%d",
				aTicks, aPos.toShortString(), tValve.getTimer(), tValve.mStructureOkay,
				tValve.mTank.amount(), tValve.mTank.capacity());
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}
}
