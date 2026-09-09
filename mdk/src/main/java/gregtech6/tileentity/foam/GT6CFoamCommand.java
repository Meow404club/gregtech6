package gregtech6.tileentity.foam;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

import gregtech6.block.foam.GT6CFoamOwnedBlock;
import gregtech6.block.foam.GT6CFoamFreshBlock;
import gregtech6.block.foam.IBlockFoamable;
import gregtech6.item.foamspray.GT6FoamPlacement;
import gregtech6.item.foamspray.GT6FoamSprayItem;
import gregtech6.item.spraycan.GTSprayCanItem;

/**
 * {@code /gt6cfoam} — the C-Foam block-family acceptance command (task
 * p26-c-foam-block-family). Game-bus listener, self-contained per ADR-P3-4; the
 * {@code /gt6pipe} foam trio counterpart on the BLOCK family side — it drives the SAME
 * static faces the item's useOn path calls ({@link GT6FoamSprayItem#liveSink} — whose
 * javadoc pins this command as the RCON consumer — and the {@link IBlockFoamable} /
 * {@link ITileEntityFoamable} dry/remove faces), never a parallel write point.
 *
 * <ul>
 * <li>{@code spray <pos> <face> <owned> [dye] [owner]} — the mode-0 single block through
 *     {@link GT6FoamPlacement#foamArm} over the shared {@code liveSink} (the upstream
 *     Behavior_Spray_Foam :137-139 owned/plain ternary :138).</li>
 * <li>{@code mode <pos> <face> <playerSide> <mode> [dye]} — the raw mode 0-4 air-placement
 *     arm over the same sink (the modes 0-4 live proof — the SPEC pin ①), a full can of
 *     2560 internal units as the budget.</li>
 * <li>{@code dry <pos>} — the no-gate dry face ({@code IBlockFoamable#dryFoam}, the SAME
 *     method the fresh block's scheduled tick calls at {@code SIDE_ANY}; the owned BE rides
 *     {@link ITileEntityFoamable#dryFoam}, upstream MultiTileEntityCFoam :118-123).</li>
 * <li>{@code removefoam <pos> [owner]} — the GATED remove face: the plain blocks remove
 *     freely (upstream BlockCFoam :63-65), the dried owned BE rejects a non-owner
 *     (upstream :126-130, the three-clause gate live).</li>
 * <li>{@code stat <pos>} — the block id + the colour + the DRIED property + the BE
 *     flags/owner/paint (the acceptance assertion face).</li>
 * </ul>
 *
 * <p>The command runs as the console (a null player): the arm's {@code playerSide} rides
 * the explicit {@code playerSide} argument (the item derives it from the pitch/yaw of the
 * swing, UT.Code.getSideForPlayerPlacing :1746-1753 — the command form is the console
 * carrier of the same walk input, the p25 spray-command precedent).
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6CFoamCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** A full can's internal units (the spray budget; the upstream ctor :56 256*10). */
	private static final long FULL_CAN = 2560;

	private GT6CFoamCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		aEvent.getDispatcher().register(
			Commands.literal("gt6cfoam")
				.requires(aSource -> aSource.hasPermission(2))
				.then(Commands.literal("spray")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("face", IntegerArgumentType.integer(0, 5))
							.then(Commands.argument("owned", IntegerArgumentType.integer(0, 1))
								.executes(aContext -> spray(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
										IntegerArgumentType.getInteger(aContext, "face"), IntegerArgumentType.getInteger(aContext, "owned") != 0,
										0, null))
									.then(Commands.argument("dye", IntegerArgumentType.integer(0, 15))
										.executes(aContext -> spray(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
												IntegerArgumentType.getInteger(aContext, "face"), IntegerArgumentType.getInteger(aContext, "owned") != 0,
												IntegerArgumentType.getInteger(aContext, "dye"), null))
											.then(Commands.argument("owner", StringArgumentType.word())
												.executes(aContext -> spray(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
														IntegerArgumentType.getInteger(aContext, "face"), IntegerArgumentType.getInteger(aContext, "owned") != 0,
														IntegerArgumentType.getInteger(aContext, "dye"),
														StringArgumentType.getString(aContext, "owner")))))))))
				.then(Commands.literal("mode")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("face", IntegerArgumentType.integer(0, 5))
							.then(Commands.argument("playerSide", IntegerArgumentType.integer(0, 5))
								.then(Commands.argument("mode", IntegerArgumentType.integer(0, 4))
									.executes(aContext -> mode(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
											IntegerArgumentType.getInteger(aContext, "face"), IntegerArgumentType.getInteger(aContext, "playerSide"),
											IntegerArgumentType.getInteger(aContext, "mode"), 0))
										.then(Commands.argument("dye", IntegerArgumentType.integer(0, 15))
											.executes(aContext -> mode(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
													IntegerArgumentType.getInteger(aContext, "face"), IntegerArgumentType.getInteger(aContext, "playerSide"),
													IntegerArgumentType.getInteger(aContext, "mode"),
													IntegerArgumentType.getInteger(aContext, "dye")))))))))
				.then(Commands.literal("dry")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> dry(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("removefoam")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> removefoam(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), null))
							.then(Commands.argument("owner", StringArgumentType.word())
								.executes(aContext -> removefoam(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
										StringArgumentType.getString(aContext, "owner"))))))
				.then(Commands.literal("stat")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))));
		LOGGER.info("Registered GT6 cfoam block command /gt6cfoam (spray|mode|dry|removefoam|stat)");
	}

	// ------------------------------------------------------------------ the arms

	private static int spray(CommandSourceStack aSource, BlockPos aPos, int aFace, boolean aOwned, int aDye,
			@Nullable String aOwner) {
		Direction tFace = Direction.from3DDataValue(aFace);
		long[] tUnits = {0};
		int[] tLanded = {0};
		GT6FoamPlacement.Sink tSink = wrap(aSource.getLevel(), aDye, aOwned, parseOwner(aOwner), tUnits, tLanded);
		GT6FoamPlacement.foamArm(0, aPos, tFace, Direction.UP, 0.5F, FULL_CAN, tSink);
		aSource.sendSuccess(() -> Component.literal("cfoam spray at " + aPos.toShortString() + " face " + tFace
				+ ": " + tLanded[0] + " landed (" + tUnits[0] + " units)"), false);
		return 1;
	}

	private static int mode(CommandSourceStack aSource, BlockPos aPos, int aFace, int aPlayerSide, int aMode, int aDye) {
		Direction tFace = Direction.from3DDataValue(aFace);
		Direction tSide = Direction.from3DDataValue(aPlayerSide);
		long[] tUnits = {0};
		int[] tLanded = {0};
		GT6FoamPlacement.Sink tSink = wrap(aSource.getLevel(), aDye, false, null, tUnits, tLanded);
		GT6FoamPlacement.foamArm(aMode, aPos, tFace, tSide, 0.5F, FULL_CAN, tSink);
		aSource.sendSuccess(() -> Component.literal("cfoam mode " + aMode + " at " + aPos.toShortString()
				+ ": " + tLanded[0] + " landed (" + tUnits[0] + " units)"), false);
		return 1;
	}

	/** The counting wrapper over the shared live sink (the landed count + the paid units per command call). */
	private static GT6FoamPlacement.Sink wrap(net.minecraft.server.level.ServerLevel aLevel, int aDye, boolean aOwned,
			@Nullable UUID aOwner, long[] tUnits, int[] tLanded) {
		GT6FoamPlacement.Sink tLive = GT6FoamSprayItem.liveSink(aLevel, aDye, aOwned, aOwner);
		return aPlacement -> {
			boolean tOk = tLive.place(aPlacement);
			if (tOk) {
				tLanded[0]++;
				tUnits[0] += aPlacement.slab() ? GT6FoamPlacement.SLAB_COST : GT6FoamPlacement.BLOCK_COST;
			}
			return tOk;
		};
	}

	private static int dry(CommandSourceStack aSource, BlockPos aPos) {
		// the owned BE face (upstream :118-123 — the no-gate dry, the DRIED property flips)
		if (aSource.getLevel().getBlockEntity(aPos) instanceof ITileEntityFoamable tFoam) {
			boolean tOk = tFoam.dryFoam((byte)0, null);
			aSource.sendSuccess(() -> Component.literal("cfoam dry at " + aPos.toShortString() + ": "
					+ (tOk ? "ok, dried true" : "nothing to dry")), false);
			return 1;
		}
		// the block face (upstream BlockCFoamFresh :110-112 — the same method the scheduled tick calls)
		BlockState tState = aSource.getLevel().getBlockState(aPos);
		if (tState.getBlock() instanceof IBlockFoamable tFoamBlock) {
			boolean tOk = tFoamBlock.dryFoam(aSource.getLevel(), aPos, null);
			ResourceLocation tId = ForgeRegistries.BLOCKS.getKey(aSource.getLevel().getBlockState(aPos).getBlock());
			aSource.sendSuccess(() -> Component.literal("cfoam dry at " + aPos.toShortString() + ": "
					+ (tOk ? "ok, now " + tId : "nothing to dry")), false);
			return 1;
		}
		aSource.sendFailure(Component.literal("cfoam dry at " + aPos.toShortString() + ": no foam there"));
		return 0;
	}

	private static int removefoam(CommandSourceStack aSource, BlockPos aPos, @Nullable String aOwner) {
		UUID tOwner = parseOwner(aOwner);
		if (aSource.getLevel().getBlockEntity(aPos) instanceof ITileEntityFoamable tFoam) {
			// the GATED face (upstream :126-130): the dried owned foam rejects a non-owner
			if (!tFoam.removeFoam((byte)0, tOwner)) {
				aSource.sendFailure(Component.literal("cfoam removefoam at " + aPos.toShortString() + ": REJECTED, the dried owned foam denies "
						+ (tOwner != null ? tOwner : "console")));
				return 0;
			}
			aSource.sendSuccess(() -> Component.literal("cfoam removefoam at " + aPos.toShortString() + ": ok, foam removed"), false);
			return 1;
		}
		BlockState tState = aSource.getLevel().getBlockState(aPos);
		if (tState.getBlock() instanceof IBlockFoamable tFoamBlock) {
			boolean tOk = tFoamBlock.removeFoam(aSource.getLevel(), aPos, null);
			aSource.sendSuccess(() -> Component.literal("cfoam removefoam at " + aPos.toShortString() + ": "
					+ (tOk ? "ok, foam removed" : "nothing to remove")), false);
			return 1;
		}
		aSource.sendFailure(Component.literal("cfoam removefoam at " + aPos.toShortString() + ": no foam there"));
		return 0;
	}

	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		BlockState tState = aSource.getLevel().getBlockState(aPos);
		ResourceLocation tId = ForgeRegistries.BLOCKS.getKey(tState.getBlock());
		StringBuilder tLine = new StringBuilder("cfoam stat at " + aPos.toShortString() + ": block " + tId);
		if (tState.hasProperty(GT6CFoamFreshBlock.COLOR)) {
			tLine.append(" color ").append(tState.getValue(GT6CFoamFreshBlock.COLOR));
		}
		if (tState.hasProperty(GT6CFoamOwnedBlock.DRIED)) {
			tLine.append(" dried ").append(tState.getValue(GT6CFoamOwnedBlock.DRIED));
		}
		if (tState.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
			tLine.append(" slab ").append(tState.getValue(net.minecraft.world.level.block.SlabBlock.TYPE));
		}
		if (aSource.getLevel().getBlockEntity(aPos) instanceof GT6CFoamBlockEntity tFoam) {
			tLine.append(" foamDried ").append(tFoam.mFoamDried)
					.append(" ownable ").append(tFoam.mOwnable)
					.append(" owner ").append(tFoam.mOwner != null ? tFoam.mOwner : "none")
					.append(" paint ").append(tFoam.isPainted() ? tFoam.getPaint() : "none");
		}
		aSource.sendSuccess(() -> Component.literal(tLine.toString()), false);
		return 1;
	}

	@Nullable
	private static UUID parseOwner(@Nullable String aText) {
		if (aText == null) return null;
		try {
			return UUID.fromString(aText);
		} catch (IllegalArgumentException aE) {
			return null;
		}
	}
}
