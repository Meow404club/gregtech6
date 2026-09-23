package gregtech6.tileentity.portals;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * {@code /gt6portal} — the portal acceptance command (task p35-portals-mini-nether-end,
 * the /gt6heatexchanger self-contained command shape):
 *
 * <ul>
 * <li>{@code ignite <pos>} — the upstream TOOL_igniter toggle on the Nether portal
 *     (Nether.java:118-121);</li>
 * <li>{@code eye <pos>} — the upstream onBlockActivated2 Ender-Eye activation on the End
 *     portal (End.java:112-123) without consuming an item (console stand-in, the
 *     /gt6machine ignite precedent);</li>
 * <li>{@code extinguish <pos>} — the upstream TOOL_extinguisher arm (:123-126);</li>
 * <li>{@code find <pos>} — forces {@link GTMiniPortalBlockEntity#findTargetPortal()} (the
 *     100-tick rescan beat, hand-driven for the chains);</li>
 * <li>{@code check <pos>} — the upstream TOOL_magnifyingglass readout (:228-241) plus the
 *     relay state: active / target coords / the emission buffers (mRedstone+mComparator)
 *     and the inbound relay + watchdog buffers (xRedstone/xComparator/wRedstone,
 *     the upstream :108-135 cross-dimension relay surface).</li>
 * </ul>
 *
 * <p>Game-bus listener (default Bus.FORGE), self-contained per ADR-P3-4.
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTMiniPortalCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTMiniPortalCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tPortal = Commands.literal("gt6portal")
				.requires(aSource -> aSource.hasPermission(2))
				.then(Commands.literal("ignite")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> ignite(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("eye")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> eye(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("extinguish")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> extinguish(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("find")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> find(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("check")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> check(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		aEvent.getDispatcher().register(tPortal);
		LOGGER.info("Registered GT6 portal acceptance command /gt6portal (ignite|eye|extinguish|find|check <pos>)");
	}

	private static int ignite(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTMiniPortalNetherBlockEntity tPortal)) {
			return fail(aSource, "IGNITE FAILED: no GTMiniPortalNetherBlockEntity at " + aPos.toShortString());
		}
		tPortal.igniteToggle(); // the upstream :119 toggle
		aSource.sendSuccess(() -> Component.literal("GT6 portal ignite at " + aPos.toShortString() + ": active=" + tPortal.mActive), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int eye(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTMiniPortalEndBlockEntity tPortal)) {
			return fail(aSource, "EYE FAILED: no GTMiniPortalEndBlockEntity at " + aPos.toShortString());
		}
		tPortal.setPortalActive(); // the upstream :116 arm, item-free (the console stand-in)
		aSource.sendSuccess(() -> Component.literal("GT6 portal eye at " + aPos.toShortString() + ": active=" + tPortal.mActive), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int extinguish(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTMiniPortalBlockEntity tPortal)) {
			return fail(aSource, "EXTINGUISH FAILED: no GTMiniPortalBlockEntity at " + aPos.toShortString());
		}
		tPortal.setPortalInactive(); // the upstream :124 arm
		aSource.sendSuccess(() -> Component.literal("GT6 portal extinguish at " + aPos.toShortString() + ": active=" + tPortal.mActive), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int find(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTMiniPortalBlockEntity tPortal)) {
			return fail(aSource, "FIND FAILED: no GTMiniPortalBlockEntity at " + aPos.toShortString());
		}
		tPortal.findTargetPortal();
		return check(aSource, aPos);
	}

	private static int check(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTMiniPortalBlockEntity tPortal)) {
			return fail(aSource, "CHECK FAILED: no GTMiniPortalBlockEntity at " + aPos.toShortString());
		}
		// the magnifyingglass readout (:232-237) + the relay buffers (the RCON pins)
		String tTarget = tPortal.mTarget == null
				? "No Target"
				: "X: " + tPortal.mTarget.getBlockPos().getX() + "   Y: " + tPortal.mTarget.getBlockPos().getY() + "   Z: " + tPortal.mTarget.getBlockPos().getZ()
						+ " dim=" + tPortal.mTarget.getLevel().dimension().location();
		String tLine = "GT6 portal at " + aPos.toShortString() + " (" + tPortal.getTileEntityName() + "): active=" + tPortal.mActive
				+ " target=" + tTarget
				+ " redstone=" + java.util.Arrays.toString(tPortal.mRedstone)
				+ " comparator=" + java.util.Arrays.toString(tPortal.mComparator)
				+ " xRedstone=" + java.util.Arrays.toString(tPortal.xRedstone)
				+ " xComparator=" + java.util.Arrays.toString(tPortal.xComparator)
				+ " wRedstone=" + java.util.Arrays.toString(tPortal.wRedstone);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int fail(CommandSourceStack aSource, String aLine) {
		aSource.sendFailure(Component.literal(aLine));
		return 0;
	}
}
