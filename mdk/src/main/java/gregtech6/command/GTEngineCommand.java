package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.tileentity.energy.GTAxleBlockEntity;
import gregtech6.tileentity.energy.GTCrankBlockEntity;
import gregtech6.tileentity.energy.GTDieselEngineBlockEntity;
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;

/**
 * {@code /gt6engine} — the engine-chain acceptance command home (task p12-engine-crank
 * spec ④). Game-bus listener, self-contained per ADR-P3-4, the GT6EnergyCommand template;
 * the later engine cards (steam/diesel/axle family) append their subcommands to this same
 * literal rather than growing new command roots.
 *
 * <ul>
 * <li>{@code stat <pos>} — the BE energy surface read: on the crank the RU emit direction
 *     (facing, name + byte) and amplitude (the signed packet — NEGATIVE =
 *     counterclockwise, the invariant the RCON chain asserts), on any other
 *     {@link ITileEntityEnergy} a generic emits/accepts per-side dump (the future engine
 *     cards' readback).</li>
 * <li>{@code crank <pos> <ticks>} — the server-driven crank window
 *     ({@link GTCrankBlockEntity#setDriveTicks}): the RCON acceptance channel, the
 *     DECLARED counterfactual of "a potionless player keeps cranking" — the player
 *     right-click ({@code GTCrankBlock.use}) stays the real-interaction path. ticks 0
 *     stops a running window (the chain's stop gate).</li>
 * <li>{@code mode <pos> on|off} — task p12-engine-steam: the steam-engine on/off gate
 *     ({@link GTSteamEngineBlockEntity#setStopped}, the upstream
 *     MultiTileEntityEngineSteam.java:225 setStateOnOff seam) — the RCON counterfactual
 *     of the :175 soft-hammer toggle (the tool face is the pool cut). {@code stat} gains
 *     the steam-engine detail branch (facing/stopped/active/heat-state/energy/tank).</li>
 * <li>{@code fill <pos> <amount>} — task p12-engine-steam, ADR
 *     2026-09-02-p12-steam-proof-deviation: the DIRECT steam-injection channel — pushes
 *     gt6:steam through the engine's back-face capability door (the canonical
 *     intake-face supply, the pipe-into-getFluidTankFillable2 :239 form); the door's
 *     gates (stopped / steam-only / back face) make a REJECTED echo a legitimate
 *     chain verdict.</li>
 * <li>{@code fuel <pos> <fluid> <amount>} — the diesel engine's funnel-face counterpart
 *     (task p12-engine-diesel spec ⑤): fills the input tank through
 *     {@link GTDieselEngineBlockEntity#funnelFill} under the containsInput gate — the
 *     DECLARED acceptance channel while the p12-tap-funnel-attachment card is in flight
 *     (no GUI, no funnel item in this port).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTEngineCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTEngineCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tEngine = Commands.literal("gt6engine")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("stat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("crank")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("ticks", IntegerArgumentType.integer(0, 1000000000))
						.executes(aContext -> crank(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								IntegerArgumentType.getInteger(aContext, "ticks"))))))
			// task p12-engine-steam — the steam-engine on/off gate: the
			// ITileEntityAdjacentOnOff.setStateOnOff seam (MultiTileEntityEngineSteam.java:225)
			// in command form, the RCON counterfactual of the soft-hammer toggle (:175, pool)
			.then(Commands.literal("mode")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.literal("on")
						.executes(aContext -> mode(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), true)))
					.then(Commands.literal("off")
						.executes(aContext -> mode(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), false)))))
			// task p12-engine-steam — the DIRECT steam-injection channel (ADR
			// 2026-09-02-p12-steam-proof-deviation): pushes gt6:steam through the engine's
			// BACK-face capability door — the canonical intake-face supply (what a 1.7.10
			// pipe did into getFluidTankFillable2), never a tank intermediate
			.then(Commands.literal("fill")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("amount", IntegerArgumentType.integer(1))
						.executes(aContext -> fill(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								IntegerArgumentType.getInteger(aContext, "amount"))))))
			// task p12-engine-diesel — the funnel-face supply channel (the RCON counterpart
			// of MotorLiquid funnelFill :203-207), the acceptance arm while the tap-funnel
			// attachment card is in flight
			.then(Commands.literal("fuel")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("fluid", ResourceLocationArgument.id())
						.then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000000))
							.executes(aContext -> fuel(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									ResourceLocationArgument.getId(aContext, "fluid"), IntegerArgumentType.getInteger(aContext, "amount")))))));
		aEvent.getDispatcher().register(tEngine);
		LOGGER.info("Registered GT6 engine-chain command /gt6engine (stat | crank | mode | fill | fuel) — the engine family acceptance home");
	}

	/**
	 * The BE energy-surface readback (spec ④ stat): crank detail, axle detail, generic
	 * energy dump, failure otherwise. The axle branch IS the tachometer channel (task
	 * p12-axle-family spec ⑤ — the upstream onToolClick2 :82-87
	 * {@code mTransferredLast + " RU/t"} readout is pooled to this command): axis, the
	 * VMAX speed rating, the bandwidth rating, and the last-tick transferred magnitude.
	 */
	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (tLevel.getBlockEntity(aPos) instanceof GTSteamEngineBlockEntity tEngine) {
			// the task p12-engine-steam readback: the conversion state surface the chain
			// asserts (facing/stopped/active/heat-state/energy/output band/piston/tank)
			String tFacing = Direction.from3DDataValue(tEngine.getFacing()).getName();
			String tTank = tEngine.mTank.isEmpty() ? "tank=empty" : "tank=" + tEngine.mTank.amount() + "/"
					+ tEngine.mTank.capacity() + "L "
					+ net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(tEngine.mTank.getFluid().getFluid());
			String tLine = "GT6 steam engine at " + aPos.toShortString()
					+ ": facing=" + tFacing + "(" + tEngine.getFacing() + ") emit-side"
					+ ", back(steam)=" + Direction.from3DDataValue(tEngine.backSide()).getName()
					+ ", stopped=" + tEngine.mStopped
					+ ", active=" + tEngine.mActive
					+ ", emitting=" + tEngine.mEmitsEnergy
					+ ", state=" + tEngine.mState + "/31"
					+ ", energy=" + tEngine.mEnergy + "/" + tEngine.mCapacity + " KU"
					+ ", output=" + tEngine.mOutput + " KU/t"
					+ ", efficiency=" + tEngine.mEfficiency + "/10000"
					+ ", piston=" + tEngine.mPiston
					+ ", " + tTank;
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
			return Command.SINGLE_SUCCESS;
		}
		if (tLevel.getBlockEntity(aPos) instanceof GTCrankBlockEntity tCrank) {
			String tFacing = Direction.from3DDataValue(tCrank.getFacing()).getName();
			String tLine = "GT6 crank at " + aPos.toShortString()
					+ ": facing=" + tFacing + "(" + tCrank.getFacing() + ") emit-side"
					+ ", active=" + tCrank.mActive
					+ ", drive=" + tCrank.mDriveTicks + " ticks"
					+ ", RU packet size=" + tCrank.mPacketSize + " (negative=counterclockwise DC)"
					+ ", amount=" + tCrank.mPacketAmount
					+ ", band " + GTCrankBlockEntity.OUTPUT_SIZE + "/" + GTCrankBlockEntity.OUTPUT_SIZE + "/" + GTCrankBlockEntity.OUTPUT_SIZE
					+ ", offered=" + tCrank.getEnergyOffered(TD.Energy.RU, (byte) tCrank.getFacing(), GTCrankBlockEntity.OUTPUT_SIZE);
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
			return Command.SINGLE_SUCCESS;
		}
		if (tLevel.getBlockEntity(aPos) instanceof GTAxleBlockEntity tAxle) {
			String tLine = "GT6 axle at " + aPos.toShortString()
					+ ": axis=" + tAxle.mAxis
					+ ", speed rating=" + tAxle.mSpeed + " RU"
					+ ", bandwidth=" + tAxle.mPower + " packets/t"
					+ ", transferred=" + tAxle.mTransferredLast + " RU/t (last tick magnitude)"
					+ ", break pending=" + tAxle.mBreakPending;
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
			return Command.SINGLE_SUCCESS;
		}
		if (tLevel.getBlockEntity(aPos) instanceof GTDieselEngineBlockEntity tEngine) {
			// the diesel readback (task p12-engine-diesel): the upstream :163-168 magnifying-glass
			// "Input/Output" click readout plus the surfaces the acceptance chain asserts — the
			// DC band (rate), the stored energy, the fuel-swap state, the activity trinary and
			// the CO2 exhaust counter with its back-face arm state.
			net.minecraftforge.fluids.FluidStack tInput = tEngine.mTanks[0].getFluid();
			String tBack;
			BlockPos tBackPos = aPos.relative(tEngine.back());
			if (tLevel.getBlockEntity(tBackPos) != null && !tLevel.getBlockEntity(tBackPos).isRemoved()) tBack = "tank@" + tBackPos.toShortString();
			else if (!tLevel.getBlockState(tBackPos).getCollisionShape(tLevel, tBackPos).isEmpty()) tBack = "solid";
			else tBack = "open";
			// the retention verdict mirrors the vent arm exactly (upstream :142): the probe is
			// the block PAST the back face (getOffset(OPOS[mFacing], 1)) — "retained" while
			// that block is solid and units are held, gone the moment it opens
			BlockPos tVentPos = tBackPos.relative(tEngine.back());
			boolean tVentBlocked = !tLevel.getBlockState(tVentPos).getCollisionShape(tLevel, tVentPos).isEmpty();
			String tExhaust = "exhaust=CO2 x" + tEngine.mExhaustCO2
					+ (tVentBlocked && tEngine.mExhaustCO2 > 0 ? " retained" : "")
					+ " (back " + tBack + ")";
			String tLine = "GT6 diesel engine at " + aPos.toShortString()
					+ ": facing=" + Direction.from3DDataValue(tEngine.getFacing()).getName() + "(" + tEngine.getFacing() + ") emit-side"
					+ ", rate=" + tEngine.mRate + " RU/t (DC constant-sign)"
					+ ", energy=" + tEngine.mEnergy
					+ ", stopped=" + tEngine.mStopped
					+ ", active=" + tEngine.mActive
					+ ", activity state=" + tEngine.mActivityState
					+ ", input=" + (tInput == null ? "empty" : ForgeRegistries.FLUIDS.getKey(tInput.getFluid()) + " x" + tEngine.mTanks[0].amount() + "/" + tEngine.mTanks[0].capacity())
					+ ", output tank=" + (tEngine.mTanks[1].has() && tEngine.mTanks[1].getFluid() != null ? ForgeRegistries.FLUIDS.getKey(tEngine.mTanks[1].getFluid().getFluid()) + " x" + tEngine.mTanks[1].amount() : "empty")
					+ ", " + tExhaust
					+ ", efficiency=" + tEngine.mEfficiency;
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
			return Command.SINGLE_SUCCESS;
		}
		if (tLevel.getBlockEntity(aPos) instanceof ITileEntityEnergy tEnergy) {
			StringBuilder tEmits = new StringBuilder(), tAccepts = new StringBuilder();
			for (byte tSide = 0; tSide < 6; tSide++) {
				for (TagData tType : tEnergy.getEnergyTypes(tSide)) {
					if (tEnergy.isEnergyEmittingTo(tType, tSide, false)) tEmits.append(tType.mName).append('@').append(tSide).append(' ');
					if (tEnergy.isEnergyAcceptingFrom(tType, tSide, false)) tAccepts.append(tType.mName).append('@').append(tSide).append(' ');
				}
			}
			String tLine = "GT6 energy BE at " + aPos.toShortString()
					+ ": emits [" + tEmits.toString().trim() + "], accepts [" + tAccepts.toString().trim() + "]";
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
			return Command.SINGLE_SUCCESS;
		}
		aSource.sendFailure(Component.literal("STAT FAILED: no energy surface at " + aPos.toShortString()));
		return 0;
	}

	/** The RCON drive window (spec ④ crank): ticks > 0 arms, 0 stops (the chain's stop gate). */
	private static int crank(CommandSourceStack aSource, BlockPos aPos, int aTicks) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTCrankBlockEntity tCrank)) {
			aSource.sendFailure(Component.literal("CRANK FAILED: no hand crank BE at " + aPos.toShortString()));
			return 0;
		}
		tCrank.setDriveTicks(aTicks);
		String tLine = aTicks > 0
				? "GT6 crank drive at " + aPos.toShortString() + ": armed " + tCrank.mDriveTicks + " ticks"
						+ " (RU packet " + tCrank.mPacketSize + "x" + tCrank.mPacketAmount + " per tick)"
				: "GT6 crank drive at " + aPos.toShortString() + ": stopped (drive=" + tCrank.mDriveTicks + " ticks)";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The steam-engine on/off gate (task p12-engine-steam spec ③): {@code on} clears
	 * {@link GTSteamEngineBlockEntity#mStopped}, {@code off} sets it — the upstream :225
	 * {@code setStateOnOff} pair in command form (the RCON counterfactual of the :175 soft
	 * hammer, which rides the tool pool). The overheat stop (:155) re-arms through the same
	 * latch; the chain's stop gate asserts {@code stopped=true} through {@code stat}.
	 */
	private static int mode(CommandSourceStack aSource, BlockPos aPos, boolean aOn) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTSteamEngineBlockEntity tEngine)) {
			aSource.sendFailure(Component.literal("MODE FAILED: no steam engine BE at " + aPos.toShortString()));
			return 0;
		}
		tEngine.setStopped(!aOn);
		String tLine = "GT6 steam engine mode at " + aPos.toShortString() + ": " + (aOn ? "on" : "off")
				+ " (stopped=" + tEngine.mStopped + ")";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The direct steam-injection channel (task p12-engine-steam, ADR
	 * 2026-09-02-p12-steam-proof-deviation): pushes gt6:steam through the engine's
	 * BACK-face capability door — the canonical intake-face supply, the 1.7.10
	 * pipe-into-{@code getFluidTankFillable2}(:239) form. The door itself carries the
	 * gates (stopped refusal, steam-only, back face), so a REJECTED echo is a legitimate
	 * verdict the chain asserts (the stop gate). No tank intermediate: the
	 * POWER_CONDUCTING destruction chain that kills steam in every upstream tank is
	 * sidestepped by never storing steam in a tank.
	 */
	private static int fill(CommandSourceStack aSource, BlockPos aPos, int aAmount) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTSteamEngineBlockEntity tEngine)) {
			aSource.sendFailure(Component.literal("FILL FAILED: no steam engine BE at " + aPos.toShortString()));
			return 0;
		}
		net.minecraftforge.fluids.capability.IFluidHandler tDoor = tEngine.getCapability(
				net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
				Direction.from3DDataValue(tEngine.backSide())).orElse(null);
		if (tDoor == null) {
			aSource.sendFailure(Component.literal("FILL FAILED: no intake-face door at " + aPos.toShortString()));
			return 0;
		}
		int tFilled = tDoor.fill(new net.minecraftforge.fluids.FluidStack(gregtech6.fluid.GTFluids.STEAM.source.get(), aAmount),
				net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		String tLine = String.format("GT6 steam engine fill at %s: filled %d/%d L of gt6:steam%s, tank holds %d L",
				aPos.toShortString(), tFilled, aAmount, tFilled == 0 ? " (REJECTED)" : " (ACCEPTED)", tEngine.mTank.amount());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The fluid id argument is the vanilla {@code ResourceLocationArgument.id()} form — a
	 * namespaced id ({@code gt6:diesel}); the brigadier string reader would reject the
	 * colon in an unquoted word.
	 *
	 * <p>The fuel subcommand (task p12-engine-diesel spec ⑤) — the RCON counterpart of the
	 * upstream funnel face (MultiTileEntityMotorLiquid.java:203-207): the diesel engine's
	 * input tank is filled through {@link GTDieselEngineBlockEntity#funnelFill}, gated on
	 * the same containsInput seam (a non-fuel fluid is REFUSED, the acceptance chain's
	 * negative arm). The fluid id carries the namespace ({@code gt6:diesel}); a bare path
	 * resolves against gt6. This is the DECLARED acceptance channel while the
	 * p12-tap-funnel-attachment card is in flight (no GUI, no funnel item in this port).
	 */
	private static int fuel(CommandSourceStack aSource, BlockPos aPos, ResourceLocation tId, int aAmount) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTDieselEngineBlockEntity tEngine)) {
			aSource.sendFailure(Component.literal("FUEL FAILED: no diesel engine BE at " + aPos.toShortString()));
			return 0;
		}
		net.minecraft.world.level.material.Fluid tFluid = ForgeRegistries.FLUIDS.getValue(tId);
		if (tFluid == null) {
			aSource.sendFailure(Component.literal("FUEL FAILED: unknown fluid " + tId));
			return 0;
		}
		net.minecraftforge.fluids.FluidStack tStack = new net.minecraftforge.fluids.FluidStack(tFluid, aAmount);
		if (!tEngine.containsFuelInput(tStack)) {
			aSource.sendFailure(Component.literal("FUEL FAILED: " + tId + " is not an ENGINE_FUELS input (containsInput gate)"));
			return 0;
		}
		int tFilled = tEngine.funnelFill(tStack, true);
		if (tFilled <= 0) {
			aSource.sendFailure(Component.literal("FUEL FAILED: input tank full or incompatible at " + aPos.toShortString()));
			return 0;
		}
		String tLine = "GT6 diesel engine at " + aPos.toShortString() + ": filled " + tFilled + " L of " + tId
				+ " (input " + tEngine.mTanks[0].amount() + "/" + tEngine.mTanks[0].capacity() + " L)";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}
}
