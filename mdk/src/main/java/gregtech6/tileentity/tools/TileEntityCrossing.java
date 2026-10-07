package gregtech6.tileentity.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.oredict.OreDictMaterial;
import gregapi.tileentity.machines.ITileEntityCrucible;
import gregapi.tileentity.machines.ITileEntityMold;
import gregtech6.registry.GT6Crucibles;

/**
 * The Crucible Crossing — the molten-pour BRIDGE (task material-mc-c-crucible-rows, the
 * Loader_MultiTileEntities.java:471-513 family). Upstream
 * {@code MultiTileEntityCrossing implements ITileEntityCrucible}: it holds nothing itself —
 * a {@link #fillMoldAtSide} ask relays HORIZONTALLY to the neighbouring crucibles until one
 * pours (the recursion guard being the static lock trio), so a mold's pull route crosses a
 * road through the bridge. The redstone half rides along: signal on the TOP or BOTTOM face
 * lights {@link #mRedstone}, which the block answers as a horizontal weak-power source
 * (upstream isProvidingWeakPower2, SIDES_HORIZONTAL, value 1).
 */
public class TileEntityCrossing extends gregtech6.tileentity.TileEntityBase03TicksAndSync implements ITileEntityCrucible {

	/** Upstream :63 — the redstone relay face (top/bottom incoming, horizontal emission). */
	public boolean mRedstone = false;

	/** Upstream :61-62 — the pour-relay recursion guard (one walk at a time, static-wide). */
	private long mLock = 0;
	private static long sLockID = 0;
	private static boolean sLock = false;

	public TileEntityCrossing(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	public TileEntityCrossing(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GT6Crucibles.CROSSING_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "gt6.crossing"; // BET registry path mirrors it (GT6Crucibles.CROSSING_BE)
	}

	/** The shell material rides the {@link GT6Crucibles.CrossingBlock} carrier. */
	@Nullable
	public OreDictMaterial material() {
		BlockState tState = getBlockState();
		if (tState.getBlock() instanceof GT6Crucibles.CrossingBlock tBlock) return tBlock.row().material().get();
		return null;
	}

	// ---------------------------------------------------------------------------
	// the tick (upstream onTick2 :62-75) — the lock reset + the redstone edge track
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		mLock = 0;
		if (mBlockUpdated || aTimer % 50 == 0) {
			boolean tPowered = hasLevel() && (getLevel().hasNeighborSignal(getBlockPos().above())
					|| getLevel().hasNeighborSignal(getBlockPos().below()));
			if (tPowered != mRedstone) {
				mRedstone = tPowered;
				if (hasLevel()) getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
			}
		}
	}

	// ---------------------------------------------------------------------------
	// the pour relay (upstream fillMoldAtSide :77-96) — the static-lock guarded walk
	// ---------------------------------------------------------------------------

	@Override
	public boolean fillMoldAtSide(ITileEntityMold aMold, byte aSide, byte aSideOfMold) {
		if (!hasLevel() || aMold == null) return false;
		boolean rReturn = false;
		if (sLock) {
			if (mLock == sLockID) return false; // this crossing already answered this walk
			mLock = sLockID;
			for (Direction tSide : Direction.Plane.HORIZONTAL) {
				if (tSide.get3DDataValue() == aSide) continue; // never back into the asking side
				BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(tSide));
				if (tNeighbor instanceof ITileEntityCrucible tCrucible
						&& tCrucible.fillMoldAtSide(aMold, (byte)tSide.getOpposite().get3DDataValue(), aSideOfMold)) return true;
			}
		} else {
			mLock = 0;
			sLockID++;
			sLock = true;
			rReturn = fillMoldAtSide(aMold, aSide, aSideOfMold); // the guarded re-entry (upstream :89)
			sLock = false;
		}
		return rReturn;
	}
}
