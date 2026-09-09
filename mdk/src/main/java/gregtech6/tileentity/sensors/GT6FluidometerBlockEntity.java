package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.fluids.capability.IFluidHandler; // the leg-native handler type (the "net.minecraftforge.fluids." swap entry shifts the package on 21.1)

/**
 * The Fluid-O-Meter Sensor (task p26-sensors-core pioneer ②) — the port of
 * MultiTileEntityFluidometer.java:41-97. Upstream read the 1.7.10
 * {@code IFluidHandler.getTankInfo} face (:46-64/:67-79); the modern equivalent is the
 * FLUID_HANDLER capability at the probe position — the TileEntityBase08Barrel :297-304
 * dual-leg query shape verbatim (the Forge BE {@code getCapability} vs the 21.1 level
 * {@code Capabilities.FluidHandler.BLOCK} lookup; the capability dispatch itself is the
 * RCON gate, the offline suite pins the arithmetic on the pure {@link #sumContents(int[])}
 * / {@link #sumCapacity(int[])} helpers). The still-fluid arm (:55-58, the water/lava
 * source read 1000) rides the vanilla source-state check. Values are liters as upstream
 * ("Measures Fluids (In Liters)" :42 — 1 mB = 1 L, the whole port's unit convention).
 */
public class GT6FluidometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6FluidometerBlockEntity(BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6FluidometerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.FLUIDOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "fluidometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		long[] tTanks = tankCensus(aTarget);
		if (tTanks != null) return sumContents(tTanks); // upstream :50-52 the tank sum
		return stillFluidValue(); // upstream :55-58 the water/lava source arm
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		long[] tTanks = tankCensus(aTarget);
		if (tTanks != null) return sumCapacity(tTanks); // upstream :70-72 the capacity sum
		return stillFluidValue(); // the source block's own 1000 scale
	}

	/**
	 * The probe read, per tank {@code [content, capacity]} pairs (null = no handler — the
	 * upstream {@code tInfo != null} gate :49/:69). The per-leg capability query lives in
	 * {@link #probeHandler} below.
	 */
	@Nullable
	private long[] tankCensus(@Nullable BlockEntity aTarget) {
		IFluidHandler tHandler = probeHandler(aTarget);
		if (tHandler == null) return null;
		long[] rCensus = new long[tHandler.getTanks() * 2];
		for (int i = 0; i < tHandler.getTanks(); i++) {
			rCensus[i * 2] = tHandler.getFluidInTank(i).getAmount();
			rCensus[i * 2 + 1] = tHandler.getTankCapacity(i);
		}
		return rCensus;
	}

	/** The pure content half of the census (the offline-testable arithmetic). */
	public static long sumContents(long[] aCensus) {
		long rFluid = 0;
		if (aCensus == null) return 0;
		for (int i = 0; i + 1 < aCensus.length; i += 2) rFluid += aCensus[i];
		return rFluid;
	}

	/** The pure capacity half of the census (the offline-testable arithmetic). */
	public static long sumCapacity(long[] aCensus) {
		long rCapacity = 0;
		if (aCensus == null) return 0;
		for (int i = 1; i < aCensus.length; i += 2) rCapacity += aCensus[i];
		return rCapacity;
	}

	/** The still-fluid arm (upstream :55-58): a water/lava SOURCE block reads 1000, everything else 0. */
	private long stillFluidValue() {
		if (hasLevel()) {
			BlockState tState = getLevel().getBlockState(getBlockPos().relative(Direction.from3DDataValue(mSecondFacing)));
			if ((tState.getBlock() == net.minecraft.world.level.block.Blocks.WATER || tState.getBlock() == net.minecraft.world.level.block.Blocks.LAVA)
					&& tState.getFluidState().isSource()) {
				return 1000;
			}
		}
		return 0;
	}

	// The leg-native handler type rides the import above (the swap table shifts the
	// package); the QUERY shape does not and is forked here.

	//? if forge {
	private net.minecraftforge.fluids.capability.IFluidHandler probeHandler(@Nullable BlockEntity aTarget) {
		if (aTarget == null || aTarget.isRemoved()) return null;
		return aTarget.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
				Direction.from3DDataValue(mSecondFacing).getOpposite()).orElse(null); // Barrel :297
	}
	//?} else {
	/*private net.neoforged.neoforge.fluids.capability.IFluidHandler probeHandler(@Nullable BlockEntity aTarget) {
		if (aTarget == null || aTarget.isRemoved()) return null;
		return getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
				aTarget.getBlockPos(), Direction.from3DDataValue(mSecondFacing).getOpposite()); // Barrel :304
	}
	*///?}
}
