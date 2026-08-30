package gregtech6.client.render;

/**
 * Contract for the immutable render snapshots a GT6 BlockEntity hands to the render
 * thread through {@link GTModelProperties#RENDER_SNAPSHOT} (built via
 * {@link GTModelProperties#snapshot()}).
 *
 * <p>The model reads the snapshot inside {@code getQuads} — which Forge may call on a
 * chunk render worker thread (IForgeBlockEntity.java:171 "may be called on a chunk render
 * thread instead of the main client thread") — while the client tick thread is already
 * building the next snapshot. Implementations therefore MUST be deeply immutable and
 * thread-safe: the ModelData iron law (ModelData.java:28 "All objects stored in here
 * MUST BE IMMUTABLE OR THREAD-SAFE. Properties will be accessed from another thread.")
 * applies to everything reachable from a snapshot value, not just its top level.
 *
 * <p>Convention: implement this as a record (or a final class over frozen collections
 * produced by {@code List.copyOf}/{@code Map.copyOf}). Never hold a {@code BlockEntity}
 * reference here — snapshots are the render-thread's detached view of machine state.
 *
 * <p>GT6 对位：GTCEu 侧走 GTModelProperties.LEVEL/POS 在 getQuads 里现场查机（MachineModel.java:215
 * MetaMachine.getMachine(level, pos)）；我们的红线禁在 getQuads 里触碰活动 BE（渲染线程快照不可变），
 * 故快照由 BE 在 getModelData() 时预制，模型只读。
 */
public interface GTRenderSnapshot {
}
