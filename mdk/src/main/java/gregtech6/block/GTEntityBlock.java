package gregtech6.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * Base for all GT6 blocks carrying a BlockEntity — the 1.20.1 block-side counterpart
 * of the GT6 MultiTileEntity system. Wires EntityBlock.newBlockEntity (EntityBlock.java:15,
 * mandatory) and the ticker (EntityBlock.getTicker :18, default null = notick chain
 * equivalent) onto the upstream canUpdate() semantics
 * (TileEntityBase01Root.java:440 = mIsTicking &amp;&amp; mShouldRefresh): the ticker runs
 * the 03 dispatcher only while canUpdate() holds, evaluated on the live instance every
 * tick, so a machine can stop and resume ticking by flipping mShouldRefresh.
 *
 * <p>GTCEu Modern precedent for the direct lambda without createTickerHelper:
 * ManagedSyncEntityBlock.getTicker :19-28. One ticker serves both sides — the upstream
 * updateEntity ran on client and server alike and branches on isServerSide internally.
 */
public abstract class GTEntityBlock extends BaseEntityBlock {

	protected GTEntityBlock(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The BlockEntityType this block mounts (ADR-P3-1: multiple blocks may return the
	 * same shared BET — the GT6 "one TE class, many material blocks" counterpart).
	 */
	protected abstract BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType();

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		// BlockEntityType.create -> factory (BlockEntityType.java:288-290)
		return tickerType().create(aPos, aState);
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level aLevel, BlockState aState, BlockEntityType<T> aType) {
		// identity gate flattened from BaseEntityBlock.createTickerHelper (:38-42)
		if (aType != tickerType()) {
			return null;
		}
		return (aTickerLevel, aPos, aTickerState, aTile) -> {
			if (aTile instanceof TileEntityBase03TicksAndSync tTile && tTile.canUpdate() && !tTile.isRemoved()) {
				tTile.updateEntity();
			}
		};
	}
}
