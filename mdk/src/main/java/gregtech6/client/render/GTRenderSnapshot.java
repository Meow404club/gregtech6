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
 * <p>GT6 counterpart: the GTCEu side resolves the machine live inside getQuads via
 * GTModelProperties.LEVEL/POS (MachineModel.java:215 MetaMachine.getMachine(level, pos));
 * our red line forbids touching a live BE inside getQuads (render-thread snapshots must be
 * immutable), so the snapshot is pre-built by the BE at getModelData() time and the model
 * is read-only.
 */
public interface GTRenderSnapshot {
}
