package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import gregtech6.registry.GT6Molds;
import gregtech6.tileentity.tools.TileEntityFaucet;
import gregtech6.tileentity.tools.TileEntityMold;

/**
 * The card-B RCON driver (task p26-crucible-mold-faucet acceptance, the card-local
 * command/ form of {@link GT6CrucibleCommand}):
 * <ul>
 * <li>{@code place <pos> <variant> <facing>} — the faucet placement arm with the mount
 *     face (the p12 {@code /gt6tank tap} facing form: FACING rides the BLOCKSTATE, so
 *     {@code /setblock} and this arm behave identically).</li>
 * <li>{@code use <pos>} — the faucet right-click counterpart: the
 *     {@link TileEntityFaucet#activate} chain (a null player = the acceptance arm).</li>
 * <li>{@code wrench <pos>} — the faucet monkey wrench: auto-pull toggle (:156-159).</li>
 * <li>{@code wrench-mold <pos> <side>} — the mold monkey wrench: the horizontal
 *     sub-side toggle or the center redstone mode (:343-355; RCON has no hit coords,
 *     the sub-side IS the argument).</li>
 * <li>{@code softhammer-mold <pos>} — the mold soft hammer reset (:337-342).</li>
 * <li>{@code stat <pos>} — the faucet readback: facing, auto-pull, the delegate
 *     below (the DOWN walk target), required units.</li>
 * </ul>
 * The block placements of the crucible and the mold stay on /gt6crucible place|place-mold;
 * the raw-mold give is the vanilla /give; the furnace step is the vanilla block.
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6FaucetCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6FaucetCommand() {}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tFaucet = Commands.literal("gt6faucet")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("place")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.then(Commands.argument("variant", StringArgumentType.word())
						.then(Commands.argument("facing", StringArgumentType.word())
							.executes(aContext -> place(aContext.getSource(),
									net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									StringArgumentType.getString(aContext, "variant"),
									StringArgumentType.getString(aContext, "facing")))))))
			.then(Commands.literal("use")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.executes(aContext -> use(aContext.getSource(),
							net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("wrench")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.executes(aContext -> wrench(aContext.getSource(),
							net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("wrench-mold")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.then(Commands.argument("side", StringArgumentType.word())
						.executes(aContext -> wrenchMold(aContext.getSource(),
								net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								StringArgumentType.getString(aContext, "side"))))))
			.then(Commands.literal("softhammer-mold")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.executes(aContext -> softHammerMold(aContext.getSource(),
							net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("stat")
				.then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
					.executes(aContext -> stat(aContext.getSource(),
							net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		aEvent.getDispatcher().register(tFaucet);
		LOGGER.info("Registered GT6 faucet command /gt6faucet (place | use | wrench | wrench-mold | softhammer-mold | stat) — the card-B acceptance home");
	}

	/** {@code down|up|north|south|west|east} → Direction (the GTToolCommand parser shape). */
	private static Direction parseSide(String aWord) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Direction tSide = Direction.byName(aWord.toLowerCase());
		if (tSide == null) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Unknown side: " + aWord)).create();
		return tSide;
	}

	/** The placement arm: the faucet block + the FACING blockstate (the mount face toward the crucible). */
	private static int place(CommandSourceStack aSource, BlockPos aPos, String aVariant, String aFacing) {
		TileEntityFaucet.FaucetBlock tBlock = GT6Molds.faucetBlockByPath(aVariant);
		if (tBlock == null) {
			aSource.sendFailure(Component.literal("PLACE FAILED: unknown faucet variant " + aVariant));
			return 0;
		}
		Direction tFacing;
		try {
			tFacing = parseSide(aFacing);
		} catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
			aSource.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
		aSource.getLevel().setBlock(aPos, tBlock.defaultBlockState()
				.setValue(gregtech6.block.attachment.GTAttachmentSmallBlock.FACING, tFacing), 3);
		String tLine = "GT6 faucet placed at " + aPos.toShortString() + ": " + aVariant + " facing " + aFacing;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The right-click counterpart (the :138-146 activation, the null-player RCON arm). */
	private static int use(CommandSourceStack aSource, BlockPos aPos) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntityFaucet tFaucet)) {
			aSource.sendFailure(Component.literal("GT6 USE FAILED: no faucet at " + aPos.toShortString()));
			return 0;
		}
		String tReport = tFaucet.activate(null, tFaucet.mFacing, null);
		aSource.sendSuccess(() -> Component.literal("GT6 faucet use at " + aPos.toShortString() + ": " + tReport), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The faucet monkey wrench (:156-159). */
	private static int wrench(CommandSourceStack aSource, BlockPos aPos) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntityFaucet tFaucet)) {
			aSource.sendFailure(Component.literal("GT6 WRENCH FAILED: no faucet at " + aPos.toShortString()));
			return 0;
		}
		String tReport = tFaucet.toggleAutoPull();
		aSource.sendSuccess(() -> Component.literal("GT6 faucet wrench at " + aPos.toShortString() + ": " + tReport), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The mold monkey wrench with the sub-side as the argument (:343-355). */
	private static int wrenchMold(CommandSourceStack aSource, BlockPos aPos, String aSide) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntityMold tMold)) {
			aSource.sendFailure(Component.literal("GT6 WRENCH-MOLD FAILED: no mold at " + aPos.toShortString()));
			return 0;
		}
		Direction tSide;
		try {
			tSide = parseSide(aSide);
		} catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
			aSource.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
		String tReport = tMold.toolMonkeyWrench((byte)tSide.get3DDataValue());
		aSource.sendSuccess(() -> Component.literal("GT6 mold wrench at " + aPos.toShortString() + " " + aSide + ": " + tReport
				+ " (mask=" + tMold.mAutoPullDirections + ")"), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The mold soft hammer (:337-342). */
	private static int softHammerMold(CommandSourceStack aSource, BlockPos aPos) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntityMold tMold)) {
			aSource.sendFailure(Component.literal("GT6 SOFTHAMMER FAILED: no mold at " + aPos.toShortString()));
			return 0;
		}
		String tReport = tMold.toolSoftHammer();
		aSource.sendSuccess(() -> Component.literal("GT6 mold softhammer at " + aPos.toShortString() + ": " + tReport), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The faucet readback: facing, auto-pull, the DOWN walk verdict, the delegate's units. */
	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		BlockEntity tBE = aSource.getLevel().getBlockEntity(aPos);
		if (!(tBE instanceof TileEntityFaucet tFaucet)) {
			aSource.sendFailure(Component.literal("GT6 STAT FAILED: no faucet at " + aPos.toShortString()));
			return 0;
		}
		Object tBelow = aSource.getLevel().getBlockEntity(aPos.below());
		String tLine = "GT6 faucet at " + aPos.getX() + ", " + aPos.getY() + ", " + aPos.getZ()
				+ " facing=" + Direction.from3DDataValue(tFaucet.mFacing) + " autoPull=" + tFaucet.mAutoPull
				+ " acidProof=" + tFaucet.isAcidProof() + " maxTemp=" + tFaucet.getMoldMaxTemperature() + "K"
				+ " below=" + (tBelow instanceof TileEntityFaucet ? "faucet" : tBelow instanceof TileEntityMold ? "mold" : String.valueOf(tBelow))
				+ " required=" + tFaucet.getMoldRequiredMaterialUnits() + "u";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return Command.SINGLE_SUCCESS;
	}
}
