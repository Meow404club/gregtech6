package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?} else {
/*import net.neoforged.neoforge.capabilities.Capabilities;
 *///?}
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;
import gregtech6.covers.ICoverableTE;
import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;

/**
 * The pump cover — 1.20.1 port of gregapi/cover/covers/CoverPump.java:42-98 (task
 * p5-barrel-side-rules spec ③, trimmed direct translation). Every second beat
 * ({@code aTimer % 20 == 5}, :68 — the phase offset keeps all pumps off the same tick)
 * it moves its throughput between the host tank and the adjacent fluid handler, in the
 * direction the visual lane encodes: 0 = pump OUT (the host gives), 1 = pump IN (the
 * host takes), toggled with the screwdriver (:59-62).
 *
 * <p>The one-way gate rides the restored ICover intercepts (:92-93, verbatim): the
 * out-face refuses incoming fluid, the in-face refuses outgoing fluid — the covered
 * face only ever carries fluid in the pump's direction.
 *
 * <p>The transfer itself is the {@link TileEntityBase08Barrel#moveTankToHandler} /
 * {@link TileEntityBase08Barrel#moveHandlerToTank} fill-then-drain pair straight through
 * {@link ICoverableTE#getCoverPumpTank()} — NOT through the host's BarrelFluidHandler
 * face: our wrapper gained side rules, and a pump mounted on a side face would dead-lock
 * against them (the architect correction; upstream's face-routed pump worked only because
 * upstream barrels have no side rules — FL.java:846 already calls the tank directly).
 * The same correction extends to the PULL end: in-mode drains the neighbour through its
 * side-less capability (the 1.20.1 all-open entry, ruling ⑥ — the natural counterpart of
 * upstream's direct tank access), because the neighbour's own side rules would otherwise
 * refuse every side-face drain ("sides take in, never give") and dead-lock the pull.
 * The PUSH end stays face-routed (the neighbour's back face): fill is face-open on
 * barrels, and pushing into a pipe must land as SideFluidHandler.fill(side) so the pipe
 * keeps its intake semantics (ruling ⑦).
 */
public class CoverPump extends AbstractCoverDefault {

	/** The single-tier throughput, 1000 L per second-beat (upstream the constructor's mThroughput; the GTCEu tier ladder 64·4^t is the comparison note). */
	public static final long THROUGHPUT = 1000;

	/** The atlas sprite of the out-facing plate (visual 0). */
	//? if forge {
	public static final ResourceLocation PUMP_OUT_SPRITE = new ResourceLocation("gt6", "block/cover_pump_out");
	//?} else {
	/*public static final ResourceLocation PUMP_OUT_SPRITE = ResourceLocation.fromNamespaceAndPath("gt6", "block/cover_pump_out");
	 *///?}

	/** The atlas sprite of the in-facing plate (visual 1). */
	//? if forge {
	public static final ResourceLocation PUMP_IN_SPRITE = new ResourceLocation("gt6", "block/cover_pump_in");
	//?} else {
	/*public static final ResourceLocation PUMP_IN_SPRITE = ResourceLocation.fromNamespaceAndPath("gt6", "block/cover_pump_in");
	 *///?}

	/** The upstream :68 server beat — {@code SERVER_TIME % 20 == 5}; exposed for the offline phase table. */
	public static boolean isPumpBeat(long aTimer) {
		return aTimer % 20 == 5;
	}

	/**
	 * Upstream :43 — the pump only mounts where it has something to pump through:
	 * {@code canTick() && instanceof IFluidHandler} becomes the host's pump-tank seam.
	 */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return aData.mTileEntity.getCoverPumpTank() == null;
	}

	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (!aIsServerSide || aData.mStopped || !isPumpBeat(aTimer)) return; // :68
		FluidTankGT tTank = aData.mTileEntity.getCoverPumpTank();
		if (tTank == null) return;
		// :71-73 — the while-loop keeps upstream fidelity; with a single tank and a
		// bucket-free fill surface one move already consumes the whole budget.
		if (aData.mVisuals[aCoverSide] == 0) { // out: host tank → the adjacent handler
			long tBudget = THROUGHPUT, tMoved = 1;
			while (tMoved > 0 && tBudget > 0) tBudget -= (tMoved = TileEntityBase08Barrel.moveTankToHandler(tTank, adjacentHandler(aData.mTileEntity, aCoverSide, false), tBudget));
		} else { // in: the adjacent tank → the host tank
			long tBudget = THROUGHPUT, tMoved = 1;
			while (tMoved > 0 && tBudget > 0) tBudget -= (tMoved = TileEntityBase08Barrel.moveHandlerToTank(adjacentHandler(aData.mTileEntity, aCoverSide, true), tTank, tBudget));
		}
	}

	/**
	 * Upstream getAdjacentTank(aSide) (WorldAndCoords.java:118-129) — the handler of the
	 * block at the covered face. The push probes the neighbour's back face (the same
	 * neighbour query the barrel gravity push and the pipe distribute use,
	 * GTFluidPipeBlockEntity.java:195); the pull uses the side-less query instead, because
	 * the neighbour's side rules would refuse every side-face drain and dead-lock the
	 * pump's own pull (the barrel null-side path is the all-open one, ruling ⑥).
	 */
	private static @Nullable IFluidHandler adjacentHandler(ICoverableTE aHost, byte aCoverSide, boolean aSideLessPull) {
		BlockEntity tBE = aHost.self();
		Level tLevel = tBE.getLevel();
		if (tLevel == null) return null;
		Direction tDir = Direction.from3DDataValue(aCoverSide);
		BlockEntity tNeighbor = tLevel.getBlockEntity(tBE.getBlockPos().relative(tDir));
		//? if forge {
		return tNeighbor == null ? null
				: tNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, aSideLessPull ? null : tDir.getOpposite()).orElse(null);
		//?} else {
		/*return tNeighbor == null ? null
				: tLevel.getCapability(Capabilities.FluidHandler.BLOCK,
						tNeighbor.getBlockPos(), aSideLessPull ? null : tDir.getOpposite());
		 *///?}
	}

	/** Upstream :59-62 — the screwdriver flips the visual lane 0 ↔ 1 (the pipe special case is cut with its subsystem); 1000 = the tool damage. */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
			aData.visual(aCoverSide, (short)(aData.mVisuals[aCoverSide] == 0 ? 1 : 0));
			return 1000;
		}
		return 0;
	}

	/** Upstream :92 — the out-face (visual 0) refuses incoming fluid on its own face. */
	@Override
	public boolean interceptFluidFill(byte aCoverSide, CoverData aData, byte aSide, @Nullable FluidStack aFluidToFill) {
		return aCoverSide == aSide && aData.mVisuals[aSide] == 0;
	}

	/** Upstream :93 — the in-face (visual 1) refuses outgoing fluid on its own face. */
	@Override
	public boolean interceptFluidDrain(byte aCoverSide, CoverData aData, byte aSide, @Nullable FluidStack aFluidToDrain) {
		return aCoverSide == aSide && aData.mVisuals[aSide] != 0;
	}

	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {return true;} // :90 — the direction survives the save

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return aData.mVisuals[aCoverSide] == 0 ? PUMP_OUT_SPRITE : PUMP_IN_SPRITE; // :93 textures pair
	}
}
