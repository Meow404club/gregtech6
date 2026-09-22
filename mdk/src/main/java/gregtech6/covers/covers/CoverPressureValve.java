package gregtech6.covers.covers;

import java.util.ArrayList;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICoverableTE;
import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;
import gregtech6.util.UT6;

/**
 * The pressure valve cover — 1.20.1 port of gregapi/cover/covers/CoverPressureValve
 * .java:43-92 (task p34-covers-gameplay-10; upstream item id 2000 "Pressure Valve").
 * The safety valve rides a SINGLE-TANK fluid pipe: after the host ticked (:49, the
 * onTickPost slot; the {@code aTimer > 2} arm :50 keeps a fresh pipe from venting
 * before its first tick), the valve DISCONNECTS the covered face from the pipe network
 * (:52 — the valve face is an endpoint, never a through-pipe), and when the pipe tank
 * is FULL (:53) it pushes the backlog into the adjacent fluid handler — or, when there
 * is no handler and the content is a GAS over an open block, vents it into the air
 * (:57-60, the tank-trash; the FIZZ sound and the temperature-damage sweep ride the
 * cut sound/entity channel, the declared cut below).
 *
 * <p>DECLARED DEVIATION — the pipe-pipe refusal arms: upstream refuses placement
 * between two pipes (:44) and disconnects the pipe behind the valve on
 * {@code interceptConnect} (:45); the port's ICover surface has no
 * interceptConnect hook (the pooled connector-hooks group, the CoverShutter declared
 * cut) — the placement arm ports verbatim, the connect arm collapses.
 *
 * <p>DECLARED CUT — the live mounting: the pipe host does not implement
 * {@link gregtech6.covers.ICoverableTE} yet (the declared host-composition follow-up
 * card); the gate and the behaviour stay complete and the offline pins drive a real
 * pipe BE directly.
 */
public class CoverPressureValve extends AbstractCoverDefault {

	/** The upstream :78 BOXES_VALVES collapse — the port's ICover surface has no bounds hooks. */

	/** Upstream :44 — the valve only mounts on a single-tank fluid pipe, never facing another pipe. */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		if (!(aData.mTileEntity instanceof GTFluidPipeBlockEntity tPipe)) return true;
		if (tPipe.mTanks.length != 1) return true;
		return adjacentTileEntity(aData.mTileEntity, aCoverSide) instanceof GTFluidPipeBlockEntity;
	}

	/** Upstream :49-64 — the post-tick safety arm. */
	@Override
	public void onTickPost(byte aSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (!aIsServerSide || aData.mStopped || aTimer <= 2) return;
		if (!(aData.mTileEntity instanceof GTFluidPipeBlockEntity tPipe) || tPipe.mTanks.length != 1) return;
		FluidTankGT tTank = tPipe.mTanks[0];
		tPipe.disconnect(aSide, true); // :52 — the valve face is an endpoint
		if (tTank.isFull()) {
			IFluidHandler tHandler = adjacentHandler(aData.mTileEntity, aSide); // :54 getAdjacentTank
			if (tHandler != null) {
				TileEntityBase08Barrel.moveTankToHandler(tTank, tHandler, tTank.amount()); // :56 FL.move
			} else if (isGas(tTank.getFluid()) && !adjacentHasCollision(aData.mTileEntity, aSide)) { // :57
				tTank.setEmpty(); // :60 GarbageGT.trash — the gas vents
			}
		}
	}

	/** The gas predicate of upstream :57 FL.gas — the port's density proxy (lighter than air). */
	public static boolean isGas(@Nullable FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return false;
		try {
			return aFluid.getFluid().getFluidType().isLighterThanAir(); // the FluidType density proxy (the IForgeFluid 1.20.1 face)
		} catch (NullPointerException tUnbound) {
			// the offline doubles cannot bind the vanilla fluid-type RegistryObjects (the
			// ForgeMod boot face) — the live server always reads the exact density
			return false;
		}
	}

	/**
	 * Upstream :54 getAdjacentTank — the fluid handler of the block at the covered face
	 * (the CoverPump push-end query: the neighbour's back face, so pipes keep their
	 * intake semantics, the p5 ruling ⑦).
	 */
	private static @Nullable IFluidHandler adjacentHandler(ICoverableTE aHost, byte aCoverSide) {
		BlockEntity tBE = aHost.self();
		Level tLevel = tBE.getLevel();
		if (tLevel == null) return null;
		Direction tDir = Direction.from3DDataValue(aCoverSide);
		BlockEntity tNeighbor = tLevel.getBlockEntity(tBE.getBlockPos().relative(tDir));
		//? if forge {
		return tNeighbor == null ? null
				: tNeighbor.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, tDir.getOpposite()).orElse(null);
		//?} else {
		/*return tNeighbor == null ? null
				: tLevel.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
						tNeighbor.getBlockPos(), tDir.getOpposite());
		 *///?}
	}

	/** Upstream :57 {@code !tDelegator.hasCollisionBox()} — the open-block vent gate. */
	private static boolean adjacentHasCollision(ICoverableTE aHost, byte aCoverSide) {
		Level tLevel = aHost.self().getLevel();
		if (tLevel == null) return false;
		BlockPos tFront = aHost.self().getBlockPos().relative(Direction.from3DDataValue(aCoverSide));
		return !tLevel.getBlockState(tFront).getCollisionShape(tLevel, tFront).isEmpty();
	}

	/** Upstream :44 getAdjacentTileEntity — the BE of the block at the covered face. */
	private static BlockEntity adjacentTileEntity(ICoverableTE aHost, byte aCoverSide) {
		Level tLevel = aHost.self().getLevel();
		if (tLevel == null) return null;
		return tLevel.getBlockEntity(aHost.self().getBlockPos().relative(Direction.from3DDataValue(aCoverSide)));
	}

	/** Upstream :90 — the valve plate art (the side texture folds into the single sprite). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		//? if forge {
		return new ResourceLocation("gt6", "block/pressurevalve/front");
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/pressurevalve/front");
		 *///?}
	}
}
