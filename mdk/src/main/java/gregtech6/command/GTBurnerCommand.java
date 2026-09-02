package gregtech6.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

import gregtech6.registry.GT6BurningBoxes;
import gregtech6.tileentity.energy.generators.GTGeneratorFluidBedBlockEntity;
import gregtech6.tileentity.energy.generators.GTGeneratorGasBlockEntity;
import gregtech6.tileentity.energy.generators.GTGeneratorLiquidBlockEntity;
import gregtech6.tileentity.energy.generators.GTGeneratorSolidBlockEntity;

/**
 * {@code /gt6burner} — the burning-box acceptance command home (task p13-burning-box-family,
 * the GTEngineCommand template). Game-bus listener, self-contained per ADR-P3-4.
 *
 * <ul>
 * <li>{@code place <pos> <variant>} — setBlock a fresh burning box (FACING north, the
 *     GTOvenCommand.place form); the variant is the gt6 block path tail
 *     ({@code brick_burning_box}, {@code burning_box_solid_lead}, ...).</li>
 * <li>{@code ignite <pos>} — the upstream TOOL_igniter arm
 *     (MultiTileEntityGeneratorSolid.java:226 {@code mBurning = T}) — the RCON
 *     counterpart of the igniter tool this port has no item seam for; the Liquid
 *     family arms the cooldown too (:180 of MotorLiquid's sibling
 *     MultiTileEntityGeneratorLiquid.java:180 {@code mCooldown = 100}).</li>
 * <li>{@code extinguish <pos>} — the TOOL_extinguisher arm (:227 {@code mBurning = F});
 *     on the GAS family it rides the ITileEntityAdjacentOnOff triple
 *     ({@code setStateOnOff(false)}, MultiTileEntityGeneratorGas.java:45, the cooldown
 *     clear included) — the card's "停机面=命令门".</li>
 * <li>{@code fuel <pos> <item> <count>} — the front-click insert arm
 *     (:196-201 + canInsertItem2 :264) as the acceptance channel (the P9
 *     "acceptance channel is not the upstream player semantics" ruling): the item id
 *     is namespaced ({@code minecraft:coal}); the insert goes through the BE's own
 *     fuel gate, so a non-fuel echo is a legitimate chain verdict.</li>
 * <li>{@code stat <pos>} — the full readback (the upstream magnifying-glass/tool
 *     readouts pooled here, the diesel stat form): burning, buffer, rate, efficiency,
 *     slots, tank, cooldown, facing, and the nearby-fire count within
 *     {@code FLAME_RANGE + 1}.</li>
 * <li>{@code fires <pos> <range>} — the fire-cluster scan for the spread arm: counts
 *     the fire blocks in the cube of the given radius and reports
 *     {@code spread=VISIBLE} when at least one burns, {@code spread=NONE} otherwise —
 *     the substring the RCON chain's 明火蔓延臂 asserts.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTBurnerCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTBurnerCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		net.minecraft.commands.CommandBuildContext tBuildContext = aEvent.getBuildContext(); // RegisterCommandsEvent.java:56
		LiteralArgumentBuilder<CommandSourceStack> tBurner = Commands.literal("gt6burner")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("place")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("variant", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "variant"))))))
			.then(Commands.literal("ignite")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> ignite(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("extinguish")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> extinguish(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("fuel")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("item", ItemArgument.item(tBuildContext))
						.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
							.executes(aContext -> fuel(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									ItemArgument.getItem(aContext, "item").createItemStack(1, false),
									IntegerArgumentType.getInteger(aContext, "count")))))))
			.then(Commands.literal("stat")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("fires")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("range", IntegerArgumentType.integer(1, 32))
						.executes(aContext -> fires(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								IntegerArgumentType.getInteger(aContext, "range"))))));
		aEvent.getDispatcher().register(tBurner);
		LOGGER.info("Registered GT6 burning-box command /gt6burner (place | ignite | extinguish | fuel | stat | fires) — the firebox family acceptance home");
	}

	/** The place arm — the GTOvenCommand.place form over the variant path lookup. */
	private static int place(CommandSourceStack aSource, BlockPos aPos, String aVariant) {
		Block tBlock = GT6BurningBoxes.blockByPath(aVariant);
		if (tBlock == null) {
			aSource.sendFailure(Component.literal("PLACE FAILED: unknown burning-box variant " + aVariant));
			return 0;
		}
		BlockState tState = tBlock.defaultBlockState();
		if (tState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			tState = tState.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
		}
		aSource.getLevel().setBlock(aPos, tState, 3);
		String tLine = "GT6 burning box placed at " + aPos.toShortString() + ": " + aVariant;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The ignite arm — the :226 TOOL_igniter body (the aPlayer == null arm of the side gate). */
	private static int ignite(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTGeneratorSolidBlockEntity tBox)) {
			aSource.sendFailure(Component.literal("IGNITE FAILED: no burning box BE at " + aPos.toShortString()));
			return 0;
		}
		tBox.mBurning = true; // :226
		if (tBox instanceof GTGeneratorLiquidBlockEntity tLiquid) tLiquid.mCooldown = 100; // Liquid :180
		String tLine = "GT6 burning box ignite at " + aPos.toShortString() + ": burning=true";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The extinguish arm — the :227 TOOL_extinguisher body; the GAS family via its on-off triple. */
	private static int extinguish(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTGeneratorSolidBlockEntity tBox)) {
			aSource.sendFailure(Component.literal("EXTINGUISH FAILED: no burning box BE at " + aPos.toShortString()));
			return 0;
		}
		if (tBox instanceof GTGeneratorGasBlockEntity tGas) {
			tGas.setStateOnOff(false); // Gas :45 — the ITileEntityAdjacentOnOff face, cooldown clear included
		} else {
			tBox.mBurning = false; // :227
		}
		String tLine = "GT6 burning box extinguish at " + aPos.toShortString() + ": burning=" + tBox.mBurning;
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The fuel arm — the front-click insert (:196-201) as the RCON channel: the item
	 * goes through the BE's own gate, so the echo names the accepted/rejected verdict.
	 */
	private static int fuel(CommandSourceStack aSource, BlockPos aPos, ItemStack aItem, int aCount) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTGeneratorSolidBlockEntity tBox)) {
			aSource.sendFailure(Component.literal("FUEL FAILED: no burning box BE at " + aPos.toShortString()));
			return 0;
		}
		ItemStack tStack = aItem.copy();
		tStack.setCount(aCount);
		ResourceLocation tId = ForgeRegistries.ITEMS.getKey(aItem.getItem());
		// the canInsertItem2 :264/:258 gate — the Solid family asks the furnace-fuel
		// synthesis, the FluidBed family its FM.FluidBed map (empty this wave → refused)
		boolean tAccepted = false;
		if (tBox instanceof GTGeneratorFluidBedBlockEntity tBed) {
			tAccepted = tBed.containsFluidBedItem(tStack);
		} else if (tBox.recipeMap() != null) {
			tAccepted = tBox.recipeMap().containsFuelInput(tStack);
		}
		if (!tAccepted) {
			aSource.sendFailure(Component.literal("FUEL FAILED: " + tId + " is not a fuel input of this burning box (containsInput gate)"));
			return 0;
		}
		if (tBox.mInventory.getStackInSlot(0).getCount() + aCount > Math.max(1, tBox.mInventory.getSlotLimit(0))) {
			aSource.sendFailure(Component.literal("FUEL FAILED: the fuel slot cannot take x" + aCount + " (slot limit)"));
			return 0;
		}
		// :196-201 — the insert (top-up merges, the :202-206 form; upstream allows it while burning too)
		ItemStack tExisting = tBox.mInventory.getStackInSlot(0);
		if (tExisting.isEmpty()) tBox.mInventory.setStackInSlot(0, tStack);
		else tExisting.grow(aCount);
		tBox.setChanged();
		String tLine = "GT6 burning box fuel at " + aPos.toShortString() + ": " + tId + " x" + aCount
				+ " (burn value " + (tBox.recipeMap() == null ? 0 : tBox.recipeMap().findFuelRecipe(tStack) == null ? 0
						: gregtech6.tileentity.energy.generators.GTGeneratorSolidBlockEntity.units(
								tBox.recipeMap().findFuelRecipe(tStack).getAbsoluteTotalPower(), 10000, tBox.mEfficiency, false))
				+ " HU at eff " + tBox.mEfficiency + ")";
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The stat readback — the family-shared surfaces + the per-family extras. */
	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof GTGeneratorSolidBlockEntity tBox)) {
			aSource.sendFailure(Component.literal("STAT FAILED: no burning box BE at " + aPos.toShortString()));
			return 0;
		}
		String tFamily = tBox instanceof GTGeneratorFluidBedBlockEntity ? "fluidbed"
				: tBox instanceof GTGeneratorGasBlockEntity ? "gas"
				: tBox instanceof GTGeneratorLiquidBlockEntity ? "liquid" : "solid";
		ItemStack tFuel = tBox.mInventory.getStackInSlot(0);
		ItemStack tOut = tBox.mInventory.getStackInSlot(1);
		StringBuilder tLine = new StringBuilder("GT6 burning box (").append(tFamily).append(") at ").append(aPos.toShortString())
				.append(": facing=").append(Direction.from3DDataValue(tBox.getFacing()).getName()).append("(").append(tBox.getFacing()).append(") front-face")
				.append(", burning=").append(tBox.mBurning)
				.append(", energy=").append(tBox.mEnergy)
				.append(", rate=").append(tBox.mRate).append(" HU/t")
				.append(", efficiency=").append(tBox.mEfficiency).append("/10000")
				.append(", emit-gate=top-face-only")
				.append(", fuel=").append(tFuel.isEmpty() ? "empty" : ForgeRegistries.ITEMS.getKey(tFuel.getItem()) + " x" + tFuel.getCount())
				.append(", output=").append(tOut.isEmpty() ? "empty" : ForgeRegistries.ITEMS.getKey(tOut.getItem()) + " x" + tOut.getCount());
		if (tBox instanceof GTGeneratorLiquidBlockEntity tLiquid) {
			String tTank = tLiquid.mTank.isEmpty() ? "tank=empty" : "tank=" + tLiquid.mTank.amount() + "/" + tLiquid.mTank.capacity() + "L "
					+ (tLiquid.mTank.getFluid() == null ? "?" : ForgeRegistries.FLUIDS.getKey(tLiquid.mTank.getFluid().getFluid()));
			tLine.append(", ").append(tTank).append(", cooldown=").append(tLiquid.mCooldown);
		}
		tLine.append(", nearbyFire=").append(countFire(tLevel, aPos, GTGeneratorSolidBlockEntity.FLAME_RANGE + 1));
		String tText = tLine.toString();
		aSource.sendSuccess(() -> Component.literal(tText), false);
		LOGGER.info(tText);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The fire-cluster scan (the 明火蔓延臂's aggregate read): fires in the cube of the
	 * given radius; the chain asserts {@code spread=VISIBLE} inside the site and
	 * {@code fires=0} outside it.
	 */
	private static int fires(CommandSourceStack aSource, BlockPos aPos, int aRange) {
		ServerLevel tLevel = aSource.getLevel();
		int tCount = countFire(tLevel, aPos, aRange);
		// the LIVE count is transient (fire dies as it consumes its support), so the
		// VISIBLE verdict rides the CUMULATIVE family telemetry — grown since boot, it
		// cannot un-land
		long tTotal = GTGeneratorSolidBlockEntity.sFiresSpreadTotal;
		String tLine = "GT6 burning box fire scan at " + aPos.toShortString() + " range " + aRange
				+ ": fires=" + tCount + " total=" + tTotal
				+ (tTotal > 0 ? " spread=VISIBLE" : " spread=NONE");
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The fire-block count in the cube of the given radius around the position. */
	private static int countFire(ServerLevel aLevel, BlockPos aPos, int aRange) {
		int rCount = 0;
		for (BlockPos tPos : BlockPos.betweenClosed(aPos.offset(-aRange, -aRange, -aRange), aPos.offset(aRange, aRange, aRange))) {
			if (aLevel.getBlockState(tPos).is(net.minecraft.world.level.block.Blocks.FIRE)) rCount++;
		}
		return rCount;
	}
}
