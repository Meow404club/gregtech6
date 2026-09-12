package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

import gregtech6.registry.GT6FeBatteries;
import gregtech6.registry.GT6FeConverters;
import gregtech6.tileentity.energy.GT6FeConverterBlockEntity;
import gregtech6.tileentity.energy.GT6FeSourceBlockEntity;

//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?} else {
/*import net.neoforged.neoforge.capabilities.Capabilities;
 *///?}

/**
 * {@code /gt6feconverter} + {@code /gt6fesource} — the p28 inbound converter acceptance
 * commands (task p28-b-fe-converter-machine), the /gt6febattery template (game-bus
 * listener, self-contained per ADR-P3-4).
 *
 * <ul>
 * <li>{@code /gt6feconverter place <pos>} — headless placement of the ONE ULV converter
 *     block (the balance ruling killed the tier-word argument along with the ladder).</li>
 * <li>{@code /gt6feconverter stat <pos>} — tier voltage, buffered FE, capacity, implied
 *     EU (the acceptance assertion face).</li>
 * <li>{@code /gt6feconverter push <pos> <fe>} — THE PUSH ARM: a foreign FE cable stand-in.
 *     Resolves the block's platform FE capability through the LEVEL query (the exact face
 *     an external cable calls) and pushes the FE into it — the intake face, the ratio
 *     alignment and the buffer all live under the same call a mod cable makes. No energy
 *     is minted: the conversion only happens on the machine's own tick.</li>
 * <li>{@code /gt6feconverter reset <pos>} — drain the buffer.</li>
 * <li>{@code /gt6fesource place <pos>} — place the fixture PRE-FILLED to capacity (a pure
 *     source is useless empty).</li>
 * <li>{@code /gt6fesource stat|set|reset <pos>} — the stored FE faces; {@code set <fe>}
 *     dials the exact amount the pull-math arms need (e.g. 130 FE = one packet plus a
 *     stranded 2 FE tail).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6FeConverterCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6FeConverterCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tConverter = Commands.literal("gt6feconverter")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("place")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("stat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("push")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("fe", IntegerArgumentType.integer(1))
						.executes(aContext -> push(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								IntegerArgumentType.getInteger(aContext, "fe"))))))
			.then(Commands.literal("reset")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> reset(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		aEvent.getDispatcher().register(tConverter);

		LiteralArgumentBuilder<CommandSourceStack> tSource = Commands.literal("gt6fesource")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("place")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> placeSource(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("stat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> statSource(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("set")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("fe", IntegerArgumentType.integer(0))
						.executes(aContext -> setSource(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								IntegerArgumentType.getInteger(aContext, "fe"))))))
			.then(Commands.literal("reset")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> setSource(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), 0))));
		aEvent.getDispatcher().register(tSource);
		LOGGER.info("Registered GT6 FE inbound commands /gt6feconverter (place|stat|push|reset) and /gt6fesource (place|stat|set|reset)");
	}

	/** The tier word argument — a plain string, resolved against the registered blocks. */
	private static int place(CommandSourceStack aSource, BlockPos aPos) {
		aSource.getLevel().setBlock(aPos, GT6FeConverters.FE_CONVERTER.get().defaultBlockState(), Block.UPDATE_ALL);
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6FeConverterBlockEntity)) {
			aSource.sendFailure(Component.literal("PLACE FAILED: no FE converter BE at " + aPos.toShortString()));
			return 0;
		}
		String tLine = "GT6 FE converter placed at " + aPos.toShortString() + ": voltage "
				+ GT6FeConverters.VOLTAGE_ULV + " EU x " + GT6FeConverterBlockEntity.AMPS + " A";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6FeConverterBlockEntity tConverter)) {
			aSource.sendFailure(Component.literal("STAT FAILED: no FE converter BE at " + aPos.toShortString()));
			return 0;
		}
		long tVoltage = tConverter.mVoltage;
		String tLine = "GT6 FE converter stat at " + aPos.toShortString() + ": voltage " + tVoltage
				+ " EU, buffer " + tConverter.mBufferFe + " FE, capacity " + tConverter.capacityFe()
				+ " FE, implied EU " + (tConverter.mBufferFe / 4) + ", packets " + (tConverter.mBufferFe / tConverter.packetFe());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int push(CommandSourceStack aSource, BlockPos aPos, int aFe) {
		// THE PUSH ARM: the LEVEL capability query — the exact face a foreign FE cable
		// calls (forge: the BE capability with the side; 21.1: the level BlockCapability).
		boolean tCanReceive = false;
		int tAccepted = 0;
		//? if forge {
		BlockEntity tBe = aSource.getLevel().getBlockEntity(aPos);
		if (tBe != null) {
			net.minecraftforge.energy.IEnergyStorage tFe = tBe.getCapability(ForgeCapabilities.ENERGY, null).orElse(null);
			if (tFe != null && tFe.canReceive()) {
				tCanReceive = true;
				tAccepted = tFe.receiveEnergy(aFe, false);
			}
		}
		//?} else {
		/*net.neoforged.neoforge.energy.IEnergyStorage tFe = aSource.getLevel().getCapability(
				Capabilities.EnergyStorage.BLOCK, aPos, null);
		if (tFe != null && tFe.canReceive()) {
			tCanReceive = true;
			tAccepted = tFe.receiveEnergy(aFe, false);
		}
		*///?}
		if (!tCanReceive) {
			aSource.sendFailure(Component.literal("PUSH FAILED: no FE intake capability at " + aPos.toShortString()));
			return 0;
		}
		String tLine = "GT6 FE converter push at " + aPos.toShortString() + ": pushed " + aFe + " FE, accepted " + tAccepted + " FE";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int reset(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6FeConverterBlockEntity tConverter)) {
			aSource.sendFailure(Component.literal("RESET FAILED: no FE converter BE at " + aPos.toShortString()));
			return 0;
		}
		tConverter.mBufferFe = 0;
		tConverter.setChanged();
		String tLine = "GT6 FE converter reset at " + aPos.toShortString() + ": buffer 0 FE";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int placeSource(CommandSourceStack aSource, BlockPos aPos) {
		aSource.getLevel().setBlock(aPos, GT6FeBatteries.FE_SOURCE.get().defaultBlockState(), Block.UPDATE_ALL);
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6FeSourceBlockEntity tSourceBe)) {
			aSource.sendFailure(Component.literal("PLACE FAILED: no FE source BE at " + aPos.toShortString()));
			return 0;
		}
		tSourceBe.setStoredFe(GT6FeSourceBlockEntity.CAPACITY); // a pure source is useless empty
		String tLine = "GT6 FE source placed at " + aPos.toShortString() + ": stored " + GT6FeSourceBlockEntity.CAPACITY
				+ " FE, capacity " + GT6FeSourceBlockEntity.CAPACITY + " FE";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int statSource(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6FeSourceBlockEntity tSourceBe)) {
			aSource.sendFailure(Component.literal("STAT FAILED: no FE source BE at " + aPos.toShortString()));
			return 0;
		}
		int tStored = tSourceBe.storedFe();
		// packets = the whole ULV packets left (8 EU x 4 = 32 FE — the machine-side packet,
		// the value the converter's pull drains per tick)
		String tLine = "GT6 FE source stat at " + aPos.toShortString() + ": stored " + tStored
				+ " FE, capacity " + GT6FeSourceBlockEntity.CAPACITY + " FE, implied EU " + (tStored / 4)
				+ ", packets " + (tStored / 32) + ", drained " + (tStored <= 0);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int setSource(CommandSourceStack aSource, BlockPos aPos, int aFe) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof GT6FeSourceBlockEntity tSourceBe)) {
			aSource.sendFailure(Component.literal("SET FAILED: no FE source BE at " + aPos.toShortString()));
			return 0;
		}
		tSourceBe.setStoredFe(aFe);
		String tLine = "GT6 FE source set at " + aPos.toShortString() + ": stored " + aFe + " FE";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}
}
