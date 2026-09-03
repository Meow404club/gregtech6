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
import net.minecraft.server.level.ServerLevel;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.slf4j.Logger;

import gregapi.data.TD;
import gregtech6.fluid.GTFluids;
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;

/**
 * {@code /gt6boiler} — the Steam Boiler Tank acceptance command home (task p13-boiler-tank
 * spec ⑦, the GTBurnerCommand template). Game-bus listener, self-contained per ADR-P3-4.
 * The five upstream tool faces (:161-196) plus the two pressurised-removal faces land here
 * as the RCON command gates — the "acceptance channel is not the upstream player semantics"
 * ruling (no tool items and no player exist on the RCON path).
 *
 * <ul>
 * <li>{@code place <pos> <variant>} — setBlock a fresh boiler (FACING north, the
 *     GTOvenCommand.place form); the variant is the gt6 block path tail
 *     ({@code steam_boiler_tank_lead}, {@code strong_steam_boiler_tank_ultimet}, ...).</li>
 * <li>{@code fill <pos> <amount>} — the water-intake channel: water through the BOTTOM-face
 *     capability door (the :262 canonical intake form — what a pipe did into
 *     getFluidTankFillable2); the door's water-only half makes a REJECTED echo a legitimate
 *     verdict. The top-face refusal is the door's own (no capability there).
 *     {@code fill <pos> <amount> distw} — the distilled-water half of the same door: upstream
 *     :262 is {@code FL.water} and the DistW fluid carries the WATER tag (FL.java:111), so the
 *     upstream door admits it; the P13 live door seam (mWaterMatch, GTBoilerTankBlockEntity
 *     .java:172) is the vanilla-water-only default, so the distilled half rides the acceptance
 *     channel as a direct mTanks[0] fill of the GTFluids.DISTILLED_WATER identity — exactly
 *     what the upstream door would have accepted, and the :119 mDistwMatch criterion then
 *     classifies the content.</li>
 * <li>{@code inject-hu <pos> <amount>} — the direct HU supply: one packet through
 *     doEnergyInjection (the Root synchronized gate, the firebox emit form aSize=1) — the
 *     acceptance counterfactual while no cable/wire HU carrier exists; the door's gates
 *     (HU-only) make a refused echo a legitimate chain verdict.</li>
 * <li>{@code barometer <pos>} — the synced 5-bit pressure gauge read (:145/:219-233).</li>
 * <li>{@code decalcify <pos>} — the TOOL_chisel arm (:165-178): above 15/31 pressure the
 *     DETONATION branch arms (explode(F), the deferred own-tick blast); below it the repair
 *     branch (vent + reset + the :171 heat formula reported — no player on the RCON path,
 *     the damage arm is the live-only half).</li>
 * <li>{@code plunge <pos>} — the TOOL_plunger arm (:161-164, the water tank first).</li>
 * <li>{@code stat <pos>} — the full readback: the TOOL_thermometer line (:182), the
 *     TOOL_magnifyingglass lines (:186-196), the tanks, the energy store, the gauge, the
 *     facing and the row path (the stored/capacity reporting surface of :257-258 — the
 *     capacitor interface half is the cut ADR-D1 subsystem).</li>
 * <li>{@code efficiency <pos>} — the calcification readout (task p14-boiler-distw-immunity):
 *     the :119/:188-192 state as ONE line carrying the PRISTINE/SCALED verdict token, so a
 *     chain asserts the efficiency verdict without string negation.</li>
 * <li>{@code dismantle <pos>} — the removedByPlayer arm (:202-205): barometer &gt; 4 (the
 *     null player IS the non-creative counterfactual) → explode(T) instant; then the block
 *     is removed either way (the :204 setBlockToAir).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTBoilerCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTBoilerCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tBoiler = Commands.literal("gt6boiler")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("place")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("variant", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "variant"))))))
			.then(Commands.literal("fill")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000000))
						.executes(aContext -> fill(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								IntegerArgumentType.getInteger(aContext, "amount")))
						.then(Commands.literal("distw")
							.executes(aContext -> fillDistw(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									IntegerArgumentType.getInteger(aContext, "amount")))))))
			.then(Commands.literal("inject-hu")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000000))
						.executes(aContext -> injectHu(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								IntegerArgumentType.getInteger(aContext, "amount"))))))
			.then(Commands.literal("barometer")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> barometer(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("decalcify")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> decalcify(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("plunge")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> plunge(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("stat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("efficiency")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> efficiency(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("dismantle")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> dismantle(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		aEvent.getDispatcher().register(tBoiler);
		LOGGER.info("Registered GT6 boiler command /gt6boiler (place | fill [distw] | inject-hu | barometer | decalcify | plunge | stat | efficiency | dismantle) — the steam boiler tank acceptance home");
	}

	/** The place arm — the GTOvenCommand.place form over the variant path lookup. */
	private static int place(CommandSourceStack aSource, BlockPos aPos, String aVariant) {
		net.minecraft.world.level.block.Block tBlock = gregtech6.registry.GT6Boilers.blockByPath(aVariant);
		if (tBlock == null) {
			aSource.sendFailure(Component.literal("PLACE FAILED: unknown boiler variant " + aVariant));
			return 0;
		}
		aSource.getLevel().setBlock(aPos, tBlock.defaultBlockState(), 3);
		String tLine = "GT6 boiler tank placed at " + aPos.toShortString() + ": " + aVariant;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The water-intake arm — water through the BOTTOM-face capability door (the :262
	 * canonical intake face; the door's own water-only + no-top-face halves gate the pour).
	 */
	private static int fill(CommandSourceStack aSource, BlockPos aPos, int aAmount) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler)) {
			aSource.sendFailure(Component.literal("FILL FAILED: no boiler tank BE at " + aPos.toShortString()));
			return 0;
		}
		IFluidHandler tDoor = tBoiler.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
				Direction.DOWN).orElse(null);
		if (tDoor == null) {
			aSource.sendFailure(Component.literal("FILL FAILED: no intake-face door at " + aPos.toShortString()));
			return 0;
		}
		int tFilled = tDoor.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, aAmount),
				IFluidHandler.FluidAction.EXECUTE);
		String tLine = String.format("GT6 boiler fill at %s: filled %d/%d L of minecraft:water%s, water tank holds %d L",
				aPos.toShortString(), tFilled, aAmount, tFilled == 0 ? " (REJECTED)" : " (ACCEPTED)", tBoiler.mTanks[0].amount());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The distilled-water half of the intake (task p14-boiler-distw-immunity). Upstream the
	 * :262 fill gate is {@code FL.water(aFluidToFill)} and the DistW fluid carries the WATER
	 * tag (FL.java:111) — the upstream door admits distilled water canonically. The P13 live
	 * door seam (mWaterMatch, GTBoilerTankBlockEntity.java:172) is the vanilla-water-only
	 * default, so the distilled half rides the acceptance channel as a DIRECT mTanks[0] fill
	 * of the GTFluids.DISTILLED_WATER identity — exactly the outcome the upstream door would
	 * have produced; the :119 mDistwMatch criterion then classifies the content (the immune
	 * arm of the P14 RCON chain).
	 */
	private static int fillDistw(CommandSourceStack aSource, BlockPos aPos, int aAmount) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler)) {
			aSource.sendFailure(Component.literal("FILL FAILED: no boiler tank BE at " + aPos.toShortString()));
			return 0;
		}
		net.minecraft.world.level.material.Fluid tDistw = GTFluids.DISTILLED_WATER.source.get();
		int tFilled = tBoiler.mTanks[0].fill(new FluidStack(tDistw, aAmount), IFluidHandler.FluidAction.EXECUTE);
		String tLine = String.format("GT6 boiler fill at %s: filled %d/%d L of %s%s, water tank holds %d L",
				aPos.toShortString(), tFilled, aAmount, net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(tDistw),
				tFilled == 0 ? " (REJECTED)" : " (ACCEPTED)", tBoiler.mTanks[0].amount());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The HU supply arm — one packet through the doEnergyInjection gate (the aSize=1
	 * firebox emit form); a wrong-type refusal is a legitimate verdict (the door gates).
	 */
	private static int injectHu(CommandSourceStack aSource, BlockPos aPos, int aAmount) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler)) {
			aSource.sendFailure(Component.literal("INJECT FAILED: no boiler tank BE at " + aPos.toShortString()));
			return 0;
		}
		long tBooked = tBoiler.doEnergyInjection(TD.Energy.HU, (byte)0, 1, aAmount, true);
		String tLine = String.format("GT6 boiler inject-hu at %s: booked %d/%d HU%s, store holds %d/%d HU",
				aPos.toShortString(), tBooked, aAmount, tBooked == 0 ? " (REJECTED)" : " (ACCEPTED)",
				tBoiler.mEnergy, tBoiler.mCapacity);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The pressure-gauge read (:145 — the synced 5-bit visual). */
	private static int barometer(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler)) {
			aSource.sendFailure(Component.literal("BAROMETER FAILED: no boiler tank BE at " + aPos.toShortString()));
			return 0;
		}
		String tLine = "GT6 boiler barometer at " + aPos.toShortString() + ": barometer=" + tBoiler.mBarometer + "/31"
				+ " (steam " + tBoiler.mTanks[1].amount() + "/" + tBoiler.mTanks[1].capacity() + " L)";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The TOOL_chisel arm (:165-178) — the detonation branch arms the deferred blast. */
	private static int decalcify(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler)) {
			aSource.sendFailure(Component.literal("DECALCIFY FAILED: no boiler tank BE at " + aPos.toShortString()));
			return 0;
		}
		int tResult = tBoiler.chisel(null); // the null-player arm: the heat damage half is live-only
		String tLine;
		if (tBoiler.mExplosionStrength > 0) {
			tLine = "GT6 boiler decalcify at " + aPos.toShortString() + ": OVERPRESSURE (barometer " + tBoiler.mBarometer
					+ " > 15) — the explosion is armed (" + tBoiler.mExplosionStrength + " strength)";
		} else if (tResult > 0) {
			tLine = "GT6 boiler decalcify at " + aPos.toShortString() + ": descaled (repair " + tResult + "), tank vented"
					+ ", efficiency=" + tBoiler.mEfficiency + "/10000"
					+ " (heat " + (tBoiler.mEnergy + tBoiler.mTanks[1].amount() / 2) / 2000.0F + " would hit a live player)";
		} else {
			tLine = "GT6 boiler decalcify at " + aPos.toShortString() + ": nothing to descale (efficiency="
					+ tBoiler.mEfficiency + "/10000)";
		}
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The TOOL_plunger arm (:161-164) — the water tank first, else the steam tank. */
	private static int plunge(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler)) {
			aSource.sendFailure(Component.literal("PLUNGE FAILED: no boiler tank BE at " + aPos.toShortString()));
			return 0;
		}
		long tTrashed = tBoiler.plunger();
		String tLine = "GT6 boiler plunge at " + aPos.toShortString() + ": trashed " + tTrashed + " L"
				+ " (water=" + tBoiler.mTanks[0].amount() + ", steam=" + tBoiler.mTanks[1].amount() + ")";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The full readback — the thermometer line (:182), the magnifying-glass lines
	 * (:186-196), the tanks, the store (:257-258 the stat carrier), the gauge, the facing.
	 */
	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler)) {
			aSource.sendFailure(Component.literal("STAT FAILED: no boiler tank BE at " + aPos.toShortString()));
			return 0;
		}
		StringBuilder tLine = new StringBuilder("GT6 boiler tank (").append(tBoiler.getTileEntityName()).append(") at ")
				.append(aPos.toShortString())
				.append(": facing=").append(Direction.from3DDataValue(tBoiler.getFacing()).getName())
				.append("(").append(tBoiler.getFacing()).append(") front-face=barometer")
				.append(", ").append(tBoiler.thermometer())
				.append(", energy booked=").append(tBoiler.mEnergy).append("/").append(tBoiler.mCapacity).append(" HU")
				.append(", demand=").append(tBoiler.mOutput / 2).append(" HU/t (any face, HU only)")
				.append(", output=").append(tBoiler.mOutput).append(" SU/t (top face, >half tank)")
				.append(", efficiency=").append(tBoiler.mEfficiency).append("/10000")
				.append(", barometer=").append(tBoiler.mBarometer).append("/31")
				.append(", water=");
		if (tBoiler.mTanks[0].isEmpty()) tLine.append("empty");
		else tLine.append(tBoiler.mTanks[0].amount()).append("/").append(tBoiler.mTanks[0].capacity()).append("L");
		tLine.append(", steam=");
		if (tBoiler.mTanks[1].isEmpty()) tLine.append("empty");
		else tLine.append(tBoiler.mTanks[1].amount()).append("/").append(tBoiler.mTanks[1].capacity()).append("L");
		tLine.append(", calcification=");
		if (tBoiler.mEfficiency < 10000) tLine.append((10000 - tBoiler.mEfficiency) / 100).append("%");
		else tLine.append("none");
		if (!tBoiler.mTanks[0].has()) tLine.append(", WARNING: NO WATER!!!");
		String tText = tLine.toString();
		aSource.sendSuccess(() -> Component.literal(tText), false);
		LOGGER.info(tText);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The calcification readout (task p14-boiler-distw-immunity) — the :119/:188-192 state
	 * as one line with the PRISTINE/SCALED verdict token: a chain asserts the verdict by
	 * substring (no negation), the exact efficiency number rides along for the transcript.
	 */
	private static int efficiency(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler)) {
			aSource.sendFailure(Component.literal("EFFICIENCY FAILED: no boiler tank BE at " + aPos.toShortString()));
			return 0;
		}
		String tVerdict = tBoiler.mEfficiency >= 10000 ? "PRISTINE" : "SCALED " + (10000 - tBoiler.mEfficiency);
		String tLine = "GT6 boiler efficiency at " + aPos.toShortString() + ": efficiency=" + tBoiler.mEfficiency
				+ "/10000 (" + tVerdict + "), calcification="
				+ (tBoiler.mEfficiency < 10000 ? (10000 - tBoiler.mEfficiency) / 100 + "%" : "none");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The removedByPlayer arm (:202-205): the null player IS the non-creative
	 * counterfactual — barometer &gt; 4 → explode(T) instant; the block is removed either
	 * way (the :204 setBlockToAir).
	 */
	private static int dismantle(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler)) {
			aSource.sendFailure(Component.literal("DISMANTLE FAILED: no boiler tank BE at " + aPos.toShortString()));
			return 0;
		}
		boolean tExploded = tBoiler.dismantle(null);
		tLevel.destroyBlock(aPos, false); // the :204 setBlockToAir (already gone on the exploded arm)
		String tLine = "GT6 boiler dismantle at " + aPos.toShortString() + ": barometer=" + tBoiler.mBarometer
				+ (tExploded ? " — pressurised, EXPLODED (instant)" : " — quiet, removed");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}
}
