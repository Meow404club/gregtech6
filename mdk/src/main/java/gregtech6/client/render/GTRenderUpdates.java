package gregtech6.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The scheduleRenderUpdate pair — the 1.20.1 counterpart of GTCEu
 * IGregtechBlockEntity.scheduleRenderUpdate (IGregtechBlockEntity.java:49-61), driving the
 * C-grade dynamic-render refresh cycle (render-route ADR).
 *
 * <p>THE PAIR (iron law of the render route): {@code requestModelDataUpdate()} alone does
 * NOT trigger a chunk rebuild — it only flags the BE inside the client
 * {@code ModelDataManager} (IForgeBlockEntity.java:153-165 → ModelDataManager.requestRefresh),
 * and the ModelData snapshot is read from that manager when a chunk rebuild task is
 * constructed. The rebuild is only scheduled by
 * {@code Level.sendBlockUpdated} (ClientLevel.java:554 → LevelRenderer.blockChanged:2532 →
 * setBlockDirty:2536 → setSectionDirty:2540). So a render update is only complete when
 * BOTH calls happen — that is what this helper guarantees:
 * <ul>
 * <li>client side: {@code sendBlockUpdated(pos, state, state, UPDATE_IMMEDIATE)} (schedules
 *     the chunk rebuild) + {@code requestModelDataUpdate()} (refreshes the snapshot cache
 *     the rebuild task reads);</li>
 * <li>server side: {@code level.blockEvent(pos, block, RENDER_UPDATE_EVENT_ID, 0)} — the
 *     vanilla block-event machinery delivers the event to the client, where the consuming
 *     Block forwards it back into {@link #scheduleRenderUpdate} (client branch).</li>
 * </ul>
 *
 * <p>CONSUMER-SIDE WIRING TEMPLATE (W3 p4-cover-core per GTCEu
 * MetaMachineBlock.java:216-219 / PipeBlockEntity.java:307-315; the BE-side variant):
 * <pre>{@code
 * // Block side (e.g. an override in the consumer's GTEntityBlock subclass):
 * @Override
 * public boolean triggerEvent(BlockState aState, Level aLevel, BlockPos aPos, int aId, int aParam) {
 *     if (aId == GTRenderUpdates.RENDER_UPDATE_EVENT_ID && aLevel.isClientSide) {
 *         BlockEntity tTile = aLevel.getBlockEntity(aPos);
 *         if (tTile != null) {
 *             GTRenderUpdates.scheduleRenderUpdate(tTile);
 *             return true;
 *         }
 *     }
 *     return super.triggerEvent(aState, aLevel, aPos, aId, aParam);
 * }
 * }</pre>
 * Server logic calls {@link #scheduleRenderUpdate(BlockEntity)} whenever the render-visible
 * state changes; the pair bounces server → blockEvent → client triggerEvent → client pair.
 */
public final class GTRenderUpdates {

	/**
	 * The blockEvent id reserved for render updates (GTCEu PipeBlockEntity.java:308
	 * "chunk re render" semantics). Parameters are unused.
	 */
	public static final int RENDER_UPDATE_EVENT_ID = 1;

	private GTRenderUpdates() {
	}

	/**
	 * Schedules a render refresh for the block at the BE's position — client pair or
	 * server blockEvent forward, per the class doc. No-op without a level (offline BEs,
	 * unloaded chunks).
	 */
	public static void scheduleRenderUpdate(BlockEntity aTile) {
		Level tLevel = aTile.getLevel();
		if (tLevel == null) {
			return;
		}
		BlockPos tPos = aTile.getBlockPos();
		BlockState tState = aTile.getBlockState();
		if (tLevel.isClientSide) {
			// The pair, in GTCEu order (IGregtechBlockEntity.java:55-56): the chunk rebuild
			// first, so the snapshot cache it reads is flagged in the same frame.
			tLevel.sendBlockUpdated(tPos, tState, tState, Block.UPDATE_IMMEDIATE);
			aTile.requestModelDataUpdate();
		} else {
			tLevel.blockEvent(tPos, tState.getBlock(), RENDER_UPDATE_EVENT_ID, 0);
		}
	}
}
