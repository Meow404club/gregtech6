package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import gregtech6.covers.CoverData;
import gregtech6.fluid.FluidTankGT;

/**
 * The air vent cover — 1.20.1 port of gregapi/cover/covers/CoverVent.java:40-85 (task
 * p34-covers-gameplay-10; upstream item id 1022 "Air Vent"). The vent is the AIR
 * INTAKE face of a tank: every 360-tick beat, phase-offset per covered side
 * ({@code aTimer % 360 == 30 + 60*aSide}, upstream :45 over SERVER_TIME — the host
 * timer is the port's beat carrier, the CoverPump :68 precedent), when the block in
 * front is collectable air, it pushes 256,000 L of air into the host tank — the
 * anti-implosion intake that keeps a steam boiler's tank from collapsing when the
 * steam is drawn off faster than it is made.
 *
 * <p>DECLARED DEVIATION — the air carrier: upstream fills {@code FL.Air}/
 * {@code FL.Air_Nether}/{@code FL.Air_End} (256000 L, the :48-62 dimension/biome
 * ladder). This port registers no air fluid (the research census fluid gap), so the
 * arm resolves the {@code gt6:air} still-fluid at tick time and no-ops while it is
 * absent — the p29 tank-valves declared-minimal precedent (a registered air fluid
 * springs the vent alive with zero code change). The dimension/biome variants collapse
 * into the single lookup (one air carrier covers all).
 *
 * <p>DECLARED DEVIATION — the placement gate: upstream {@code canTick() && instanceof
 * IFluidHandler} (:41); the port keys on the host's pump-tank seam
 * ({@link ICoverableTE#getCoverPumpTank()}, the CoverPump :43 gate form) — the vent
 * mounts where a pump mounts.
 */
public class CoverVent extends AbstractCoverDefault {

	/** The upstream :48 fill budget — 256,000 L of air per beat. */
	public static final long AIR_FILL_BUDGET = 256000;

	/** The air carrier id the fill arm resolves at tick time (the declared-minimal seam, the class doc). */
	//? if forge {
	public static final ResourceLocation AIR_FLUID_ID = new ResourceLocation("gt6", "air");
	//?} else {
	/*public static final ResourceLocation AIR_FLUID_ID = ResourceLocation.fromNamespaceAndPath("gt6", "air");
	 *///?}

	/** The upstream :45 server beat with the per-side phase offset — exposed for the offline pin. */
	public static boolean isVentBeat(long aTimer, byte aSide) {
		return aTimer % 360 == 30 + 60 * aSide;
	}

	/** The air carrier lookup — {@code null} while the port registers no air fluid (the declared-minimal seam). */
	@Nullable
	public static FluidStack airStack() {
		net.minecraft.world.level.material.Fluid tFluid = ForgeRegistries.FLUIDS().getValue(AIR_FLUID_ID);
		return tFluid == null || tFluid == Fluids.EMPTY ? null : new FluidStack(tFluid, (int) AIR_FILL_BUDGET);
	}

	/** Upstream :41 — the vent only mounts on hosts with a tank to breathe into (the pump-seam gate form). */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return aData.mTileEntity.getCoverPumpTank() == null;
	}

	/** Upstream :44-65 — the intake beat: air in front, air carrier resolved, tank filled. */
	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (!aIsServerSide || aData.mStopped || !isVentBeat(aTimer, aCoverSide)) return;
		FluidTankGT tTank = aData.mTileEntity.getCoverPumpTank();
		if (tTank == null) return;
		FluidStack tAir = airStack();
		if (tAir == null) return; // the declared-minimal no-op: the port has no air carrier yet
		BlockEntity tHost = aData.mTileEntity.self();
		Level tLevel = tHost.getLevel();
		if (tLevel == null) return;
		BlockPos tFront = tHost.getBlockPos().relative(Direction.from3DDataValue(aCoverSide));
		if (!tLevel.getBlockState(tFront).isAir()) return; // WD.collectable_air — the air-in-front gate
		tTank.fill(tAir, net.minecraftforge.fluids.FluidAction.EXECUTE); // FL.fill_ — the direct-tank form (the p5 ruling)
	}

	/** Upstream :77 — the vent plate art (the sides/back stack folds into the single sprite). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		//? if forge {
		return new ResourceLocation("gt6", "block/vent/front");
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/vent/front");
		 *///?}
	}
}
