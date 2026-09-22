package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.covers.CoverData;
import gregtech6.fluid.FluidTankGT;

/**
 * The drain cover — 1.20.1 port of gregapi/cover/covers/CoverDrain.java:63-254 (task
 * p34-covers-gameplay-10; upstream item id 1020 "Drain"). Three collection arms on the
 * ported surface: the rain arm (:70-84 — every 100-tick beat, phase-offset per beat
 * slot, a top/horizontal face with sky access fills {@code 1000 * (thundering?2:1)} L
 * of water in precipitating biomes above 0.2 temperature), and the fluid-block arms
 * (:109-156 — a vanilla water source in front fills 1000 L and the block goes air,
 * unless the water is a 2+-source infinite pool, which refills forever at the 16000 L
 * upstream bonus rate; a vanilla lava source fills 1000 L and goes air).
 *
 * <p>DECLARED DEVIATION — the placement gate keys on the pump-tank seam (the CoverVent
 * class doc; upstream {@code canTick() && instanceof IFluidHandler} :64), the beat
 * carrier is the host timer (the CoverPump :68 precedent), and the biome rainfall
 * float folds onto the precipitation boolean (the GT6BumbleGenes.rainfallOf classifier
 * precedent — 1.20.1 biomes carry no live rainfall axis).
 *
 * <p>DECLARED CUT — the mob-fluid zoo: the XP-orb arm (:85-108, FL.XP/FL.Mob), the
 * walk-over arms (:162-224, sewage/ink/slime), the GT fluid-block family
 * (BlockBaseFluid/River/Ocean/Swamp, :113-145), the generic {@code IFluidBlock} drain
 * and the OpenBlocks XP drain all key on fluids/entities this port does not register
 * (the census fluid gaps); the slab/stairs rain-geometry special cases (:76-80) fold
 * into the sky-access check. {@code onWalkOver} has no hook on the ported ICover
 * surface at all.
 */
public class CoverDrain extends AbstractCoverDefault {

	/** The upstream :129/:134 per-source fill — one bucket per beat. */
	public static final long SOURCE_FILL = 1000;

	/** The upstream :127/:137 infinite-water bonus rate. */
	public static final long INFINITE_WATER_FILL = 16000;

	/** The upstream :70 rain beat. */
	public static boolean isRainBeat(long aTimer) {
		return aTimer % 100 == 10;
	}

	/** The upstream :109 fluid-block sweep beat. */
	public static boolean isSweepBeat(long aTimer) {
		return aTimer % 20 == 5;
	}

	/** Upstream :64 — the drain only mounts on hosts with a tank to collect into (the pump-seam gate form). */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return aData.mTileEntity.getCoverPumpTank() == null;
	}

	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (!aIsServerSide || aData.mStopped) return;
		FluidTankGT tTank = aData.mTileEntity.getCoverPumpTank();
		if (tTank == null) return;
		BlockEntity tHost = aData.mTileEntity.self();
		Level tLevel = tHost.getLevel();
		if (tLevel == null) return;
		Direction tDir = Direction.from3DDataValue(aCoverSide);
		BlockPos tFront = tHost.getBlockPos().relative(tDir);

		// :70-84 — the rain arm: top and horizontal faces, precipitating and warm enough, sky above
		if (isRainBeat(aTimer) && aCoverSide != Direction.DOWN.get3DDataValue() && tLevel.isRaining()
				&& tLevel.canSeeSky(tFront)) {
			Biome tBiome = tLevel.getBiome(tFront).value();
			if (tBiome.hasPrecipitation() && tBiome.getBaseTemperature() >= 0.2F) {
				long tFill = SOURCE_FILL * (tLevel.isThundering() ? 2 : 1); // :81 — the thundering doubling (the rainfall float folds, the class doc)
				tTank.fill(new FluidStack(Fluids.WATER, (int) tFill), net.minecraftforge.fluids.FluidAction.EXECUTE);
			}
		}

		// :109-156 — the vanilla fluid-block sweep (aReceivedBlockUpdate folds into the beat, the CoverPump trim precedent)
		if (isSweepBeat(aTimer)) {
			BlockState tState = tLevel.getBlockState(tFront);
			boolean tWater = tState.getBlock() == Blocks.WATER && tState.getValue(LiquidBlock.LEVEL) == 0; // :124-125 — the full source
			boolean tLava = tState.getBlock() == Blocks.LAVA && tState.getValue(LiquidBlock.LEVEL) == 0; // :133-134 — the full source
			// :147 — the face orientation gate: liquids drain through horizontal and top faces only
			boolean tFaceOkay = aCoverSide != Direction.DOWN.get3DDataValue();
			if (tFaceOkay && tWater) {
				if (isInfiniteWater(tLevel, tFront)) { // :126 — the 2+-source pool refills forever
					tTank.fill(new FluidStack(Fluids.WATER, (int) INFINITE_WATER_FILL), net.minecraftforge.fluids.FluidAction.EXECUTE);
				} else if (tTank.fill(new FluidStack(Fluids.WATER, (int) SOURCE_FILL), net.minecraftforge.fluids.FluidAction.SIMULATE) == SOURCE_FILL) {
					tTank.fill(new FluidStack(Fluids.WATER, (int) SOURCE_FILL), net.minecraftforge.fluids.FluidAction.EXECUTE);
					tLevel.removeBlock(tFront, false); // :152 — setBlockToAir
				}
			} else if (tFaceOkay && tLava) {
				if (tTank.fill(new FluidStack(Fluids.LAVA, (int) SOURCE_FILL), net.minecraftforge.fluids.FluidAction.SIMULATE) == SOURCE_FILL) {
					tTank.fill(new FluidStack(Fluids.LAVA, (int) SOURCE_FILL), net.minecraftforge.fluids.FluidAction.EXECUTE);
					tLevel.removeBlock(tFront, false); // :152
				}
			}
		}
	}

	/**
	 * Upstream :126 WD.infiniteWater — the water block has at least two horizontal
	 * neighbours that are themselves full water sources (the vanilla spring-forming
	 * rule): draining one cell does not consume the pool.
	 */
	static boolean isInfiniteWater(Level aLevel, BlockPos aPos) {
		int tSources = 0;
		for (Direction tDir : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
			BlockState tNeighbour = aLevel.getBlockState(aPos.relative(tDir));
			if (tNeighbour.getBlock() == Blocks.WATER && tNeighbour.getValue(LiquidBlock.LEVEL) == 0) tSources++;
		}
		return tSources >= 2;
	}

	/** Upstream :252 — the drain plate art (the sides/back stack folds into the single sprite). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		//? if forge {
		return new ResourceLocation("gt6", "block/drain/front");
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/drain/front");
		 *///?}
	}
}
