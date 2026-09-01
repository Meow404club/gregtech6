package gregtech6.covers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.tileentity.machines.TileEntityOven;

/**
 * The test probe oven (task p4-cover-core): records the cover drops instead of spawning
 * ItemEntities (the offline doubles have no entity system) and narrows
 * {@link #allowCovers} per the {@link #mAllowMask} so the validity sweep and the
 * admission gate get exercised. Drops recorded per side keep the assertion order-safe.
 */
public class TileEntityOvenCoverProbe extends TileEntityOven {

	public final List<ItemStack> mDropped = new ArrayList<>();
	public final List<Byte> mDropSides = new ArrayList<>();
	public int mAllowMask = 0b111111;
	/**
	 * Records {@code setChanged()} (the chunk-dirty persistence mark) — task
	 * p10-debug-oven-cover-resurrect regression: the cover dispatch must reach it. The
	 * stub level makes the super call a documented no-op (hasChunkAt → false), so the
	 * count is the only observable.
	 */
	public int mChangedCount = 0;

	public TileEntityOvenCoverProbe(BlockEntityType<TileEntityOvenCoverProbe> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public void setChanged() {
		mChangedCount++;
		// super deliberately not called: the stub level's blockEntityChanged path is a
		// no-op by fixture design (GTMachinesOfflineTestBase.MachineLevel.hasChunkAt)
	}

	@Override
	public boolean allowCovers(byte aSide) {
		return ICoverableTE.validSide(aSide) && (mAllowMask >> aSide & 1) != 0;
	}

	@Override
	public void dropCoverStack(ItemStack aStack, byte aSide) {
		mDropped.add(aStack.copy());
		mDropSides.add(aSide);
	}
}
