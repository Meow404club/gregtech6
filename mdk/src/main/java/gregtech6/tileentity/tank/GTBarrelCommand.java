package gregtech6.tileentity.tank;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTBarrels;

/**
 * {@code /gt6tank} — the automated fluid-barrel acceptance command (task p4-fluid-barrel
 * acceptance ②/③, the {@code /gt6pipe} shape of GTFluidPipeCommand). Game-bus listener,
 * self-contained per ADR-P3-4. Console-safe: the FluidUtil container machinery runs with
 * a null player (tryEmptyContainer/tryFillContainer, FluidUtil.java:179/:126 —
 * {@code @Nullable Player}), the full player-hand face of
 * {@code FluidUtil.interactWithFluidHandler} rides the barrel block's {@code use} and
 * stays a manual-visual item like the earlier cards' interaction checks.
 *
 * <ul>
 * <li>{@code accept <pos>} — the card scenario against the barrel at pos, through the
 *     real FluidUtil bucket machinery against the barrel capability: 16 water buckets
 *     emptied into the barrel reach the 16000 L capacity, the 17th is rejected
 *     (voidExcess is off), 16 empty buckets draw it back out, the 17th is rejected.</li>
 * <li>{@code melt <pos>} — the melt-down chain with the card's verification fluid
 *     ({@code gt6:iron_molten} at 1811 K): set the wood ceiling 340 K, fill 1000 L of
 *     molten iron through the capability, one dispatcher pass triggers the :162 judgment
 *     and {@link TileEntityBase08Barrel#meltdown()} — the tank is voided and the block
 *     replaced with fire (:227).</li>
 * <li>{@code stat <pos>} — dump amount/capacity/temperature/melting point for debugging.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTBarrelCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** A bucket is 1000 L; 16 buckets fill the 16000 L barrel exactly. */
	private static final int BUCKET = 1000;
	private static final int BUCKETS_TO_FULL = 16;
	/** The wood-barrel melt-down ceiling (GTBarrelBlock 340, upstream NBT_CAPACITY_HU row :2136). */
	private static final long WOOD_MELTING_POINT = 340;

	private GTBarrelCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		aEvent.getDispatcher().register(
			Commands.literal("gt6tank")
				.requires(aSource -> aSource.hasPermission(2))
				.then(Commands.literal("accept")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> accept(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("melt")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> melt(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("fill")
					// p5 spec ⑥ — /gt6tank fill <pos> [side] <fluid> <amount>; the side-less
					// form is the all-open path (mSide == -1), an explicit side exercises the
					// side rules and the cover intercepts through the side-wrapped handler.
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("fluid", ResourceLocationArgument.id())
							.then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
								.executes(aContext -> fill(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), null,
										ResourceLocationArgument.getId(aContext, "fluid"),
										com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "amount")))))
						.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
							.then(Commands.argument("fluid", ResourceLocationArgument.id())
								.then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
									.executes(aContext -> fill(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
											parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "side")),
											ResourceLocationArgument.getId(aContext, "fluid"),
											com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "amount"))))))))
				.then(Commands.literal("draw")
					// p5 spec ⑥ — /gt6tank draw <pos> [side] <amount>
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
							.executes(aContext -> draw(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"), null,
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "amount"))))
						.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
							.then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
								.executes(aContext -> draw(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
										parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "side")),
										com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "amount")))))))
				.then(Commands.literal("stat")
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))));
		LOGGER.info("Registered GT6 fluid barrel command /gt6tank (accept|melt|fill|draw|stat)");
	}

	/** {@code down|up|north|south|west|east} → Direction (the GTCoverCommand parse, mirrored here so the driver stays self-contained). */
	private static Direction parseSide(String aWord) throws CommandSyntaxException {
		Direction tSide = Direction.byName(aWord.toLowerCase());
		if (tSide == null) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Unknown side: " + aWord)).create();
		return tSide;
	}

	/**
	 * The p5 fill driver: inject through the (side-wrapped) capability and report the
	 * accepted amount — 0 with the REJECTED marker is a legitimate verdict (a side face,
	 * a refused pump face or a full/mixed tank), not a command failure, so the RCON chain
	 * can assert on it.
	 */
	private static int fill(CommandSourceStack aSource, BlockPos aPos, @javax.annotation.Nullable Direction aSide, ResourceLocation aFluidId, int aAmount) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof TileEntityBase08Barrel tBarrel)) {
			aSource.sendFailure(Component.literal("No GT6 barrel BlockEntity at " + aPos.toShortString()));
			return 0;
		}
		net.minecraft.world.level.material.Fluid tFluid = ForgeRegistries.FLUIDS.getValue(aFluidId);
		if (tFluid == null || tFluid.defaultFluidState() == null || tFluid.defaultFluidState().isEmpty()) {
			aSource.sendFailure(Component.literal("Unknown fluid: " + aFluidId));
			return 0;
		}
		IFluidHandler tHandler = tBarrel.getCapability(ForgeCapabilities.FLUID_HANDLER, aSide).orElse(null);
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the barrel exposes no FLUID_HANDLER on " + (aSide == null ? "the side-less query" : aSide)));
			return 0;
		}
		int tFilled = tHandler.fill(new FluidStack(tFluid, aAmount), FluidAction.EXECUTE);
		String tLine = String.format("GT6 tank fill at %s face %s: filled %d/%d L of %s%s, tank holds %d L",
				aPos.toShortString(), aSide == null ? "any" : aSide, tFilled, aAmount, aFluidId,
				tFilled == 0 ? " (REJECTED)" : " (ACCEPTED)", tBarrel.mTank.amount());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The p5 draw driver: withdraw through the (side-wrapped) capability — the mirror of {@link #fill}. */
	private static int draw(CommandSourceStack aSource, BlockPos aPos, @javax.annotation.Nullable Direction aSide, int aAmount) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof TileEntityBase08Barrel tBarrel)) {
			aSource.sendFailure(Component.literal("No GT6 barrel BlockEntity at " + aPos.toShortString()));
			return 0;
		}
		IFluidHandler tHandler = tBarrel.getCapability(ForgeCapabilities.FLUID_HANDLER, aSide).orElse(null);
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the barrel exposes no FLUID_HANDLER on " + (aSide == null ? "the side-less query" : aSide)));
			return 0;
		}
		FluidStack tDrawn = tHandler.drain(aAmount, FluidAction.EXECUTE);
		int tDrawnAmount = tDrawn == null ? 0 : tDrawn.getAmount();
		String tFluid = tDrawn == null || tDrawn.isEmpty() ? "nothing" : ForgeRegistries.FLUIDS.getKey(tDrawn.getFluid()).toString();
		String tLine = String.format("GT6 tank draw at %s face %s: drawn %d/%d L of %s%s, tank holds %d L",
				aPos.toShortString(), aSide == null ? "any" : aSide, tDrawnAmount, aAmount, tFluid,
				tDrawnAmount == 0 ? " (REJECTED)" : " (ACCEPTED)", tBarrel.mTank.amount());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		if (!(aSource.getLevel().getBlockEntity(aPos) instanceof TileEntityBase08Barrel aBarrel)) {
			aSource.sendFailure(Component.literal("No GT6 barrel BlockEntity at " + aPos.toShortString()));
			return 0;
		}
		String tLine = String.format("GT6 barrel stat at %s: %d/%d L of %s, temperature %d K, melting point %d K",
				aPos.toShortString(), aBarrel.mTank.amount(), aBarrel.mTank.capacity(),
				aBarrel.mTank.isEmpty() ? "nothing" : ForgeRegistries.FLUIDS.getKey(aBarrel.mTank.getFluid().getFluid()),
				TileEntityBase08Barrel.fluidTemperature(aBarrel.mTank.getFluid()), aBarrel.mMeltingPoint);
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	/** The acceptance scenario; every failure names the broken invariant. */
	private static int accept(CommandSourceStack aSource, BlockPos aPos) throws CommandSyntaxException {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof TileEntityBase08Barrel tBarrel)) {
			aSource.sendFailure(Component.literal("No GT6 barrel BlockEntity at " + aPos.toShortString()));
			return 0;
		}
		IFluidHandler tHandler = tBarrel.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the barrel exposes no FLUID_HANDLER"));
			return 0;
		}

		// 1. 16 water buckets -> the barrel reaches exactly its 16000 L capacity
		for (int i = 0; i < BUCKETS_TO_FULL; i++) {
			FluidActionResult tResult = FluidUtil.tryEmptyContainer(new ItemStack(Items.WATER_BUCKET), tHandler, BUCKET, null, true);
			if (!tResult.isSuccess() || !tResult.getResult().is(Items.BUCKET)) {
				aSource.sendFailure(Component.literal("FILL FAILED at bucket " + (i + 1) + ": tank holds " + tBarrel.mTank.amount() + " L"));
				return 0;
			}
		}
		if (tBarrel.mTank.amount() != tBarrel.mTank.capacity()) {
			aSource.sendFailure(Component.literal("CAPACITY BROKEN: " + tBarrel.mTank.amount() + " L of " + tBarrel.mTank.capacity() + " L after " + BUCKETS_TO_FULL + " buckets"));
			return 0;
		}

		// 2. the 17th water bucket is rejected (no voidExcess — the barrel never voids)
		if (FluidUtil.tryEmptyContainer(new ItemStack(Items.WATER_BUCKET), tHandler, BUCKET, null, true).isSuccess()) {
			aSource.sendFailure(Component.literal("OVERFLOW ACCEPTED: a full barrel must reject the 17th bucket (voidExcess off)"));
			return 0;
		}

		// 3. 16 empty buckets draw the water back out, each returning a water bucket
		for (int i = 0; i < BUCKETS_TO_FULL; i++) {
			FluidActionResult tResult = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), tHandler, BUCKET, null, true);
			if (!tResult.isSuccess() || !tResult.getResult().is(Items.WATER_BUCKET)) {
				aSource.sendFailure(Component.literal("DRAIN FAILED at bucket " + (i + 1) + ": tank holds " + tBarrel.mTank.amount() + " L"));
				return 0;
			}
		}

		// 4. the barrel is truly empty again (wood does not keep the filter — drains to null), the 17th draw fails
		if (tBarrel.mTank.amount() != 0 || tBarrel.mTank.getFluid() != null) {
			aSource.sendFailure(Component.literal("DRAIN RESIDUE: " + tBarrel.mTank.amount() + " L left after " + BUCKETS_TO_FULL + " draws"));
			return 0;
		}
		if (FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), tHandler, BUCKET, null, true).isSuccess()) {
			aSource.sendFailure(Component.literal("OVERDRAW: an empty barrel must reject the 17th draw"));
			return 0;
		}

		String tOk = String.format("GT6 tank check OK at %s: filled %d L from %d water buckets (17th rejected), "
				+ "drained back with %d empty buckets (17th rejected), barrel empty",
				aPos.toShortString(), tBarrel.mTank.capacity(), BUCKETS_TO_FULL, BUCKETS_TO_FULL);
		aSource.sendSuccess(() -> Component.literal(tOk), false);
		LOGGER.info(tOk);
		return Command.SINGLE_SUCCESS;
	}

	/** The melt-down chain with the card's verification fluid gt6:iron_molten (1811 K vs the 340 K wood ceiling). */
	private static int melt(CommandSourceStack aSource, BlockPos aPos) throws CommandSyntaxException {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof TileEntityBase08Barrel tBarrel)) {
			aSource.sendFailure(Component.literal("No GT6 barrel BlockEntity at " + aPos.toShortString()));
			return 0;
		}
		IFluidHandler tHandler = tBarrel.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
		if (tHandler == null) {
			aSource.sendFailure(Component.literal("CAPABILITY MISSING: the barrel exposes no FLUID_HANDLER"));
			return 0;
		}

		// the registration NBT row upstream: wood melts at 340 K (NBT_CAPACITY_HU=340, :2136)
		tBarrel.mMeltingPoint = WOOD_MELTING_POINT;

		// fill 1000 L of molten iron through the capability (the capability path allows the fill
		// — upstream fills first and the :162 judgment melts the barrel on the next tick)
		FluidStack tMoltenIron = new FluidStack(GTFluids.IRON_MOLTEN.get(), BUCKET);
		int tFilled = tHandler.fill(tMoltenIron, FluidAction.EXECUTE);
		if (tFilled != BUCKET) {
			aSource.sendFailure(Component.literal("IRON FILL FAILED: accepted " + tFilled + " of " + BUCKET + " L of gt6:iron_molten"));
			return 0;
		}

		// one dispatcher pass: the :162 judgment fires, meltdown() voids the tank and burns the barrel down
		tBarrel.updateEntity();

		if (tBarrel.mTank.amount() != 0) {
			aSource.sendFailure(Component.literal("MELTDOWN FAILED: " + tBarrel.mTank.amount() + " L of molten iron survived the tick"));
			return 0;
		}
		// the barrel block must be gone (setToFire :227). Mid-air fire self-extinguishes through the
		// vanilla BushBlock.canSurvive neighbour update (setBlock UPDATE_ALL resolves it synchronously),
		// so the stable observable is "no longer the barrel", not "is fire".
		if (tLevel.getBlockState(aPos).is(GTBarrels.BARREL.get())) {
			aSource.sendFailure(Component.literal("BARREL SURVIVED: the barrel must burn down (upstream setToFire :227)"));
			return 0;
		}

		String tOk = String.format("GT6 melt check OK at %s: %d L of gt6:iron_molten (%d K) against the %d K wood ceiling voided the tank and burned the barrel down",
				aPos.toShortString(), BUCKET, TileEntityBase08Barrel.fluidTemperature(tMoltenIron), WOOD_MELTING_POINT);
		aSource.sendSuccess(() -> Component.literal(tOk), false);
		LOGGER.info(tOk);
		return Command.SINGLE_SUCCESS;
	}
}
