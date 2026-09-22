package gregtech6.covers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.tileentity.machines.ITileEntitySwitchableMode;

/**
 * The test probe switchable-mode host (task p34-covers-gameplay-10) — the minimal
 * composition-contract BE: {@link ICoverableTE} (the mCovers store) plus
 * {@link ITileEntitySwitchableMode} (the selector dial). The dial and the redstone
 * answer are plain fields the tests drive; the first LIVE host is the declared
 * host-composition follow-up card.
 */
public class TileEntityModeDialProbe extends BlockEntity implements ICoverableTE, ITileEntitySwitchableMode {

	/** The dial the selectors drive. */
	public byte mMode = 0;

	/** The redstone answer of {@link #getRedstoneIncoming} (the stub-level override). */
	public byte mIncoming = 0;

	public TileEntityModeDialProbe(BlockEntityType<TileEntityModeDialProbe> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public byte setStateMode(byte aMode) {
		this.mMode = aMode;
		return aMode;
	}

	@Override
	public byte getStateMode() {
		return mMode;
	}

	@Override
	public byte getRedstoneIncoming(byte aSide) {
		return mIncoming;
	}

	@Override
	@Nullable
	public CoverData getCovers() {
		return mCovers;
	}

	@Override
	public void setCovers(@Nullable CoverData aCoverData) {
		mCovers = aCoverData;
	}
}
