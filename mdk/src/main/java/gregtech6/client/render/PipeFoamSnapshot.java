package gregtech6.client.render;

/**
 * The immutable foam render snapshot (task p25-c-foam-pipe-spray spec ⑤) — the {@link
 * GTRenderSnapshot} record the pipe BE hands to the render thread through
 * {@code getModelData()} whenever the pipe carries C-Foam. The texture selection stays
 * model-side in {@code GTFluidPipeFoamModel}: dried swaps the WHOLE body to the hardened
 * foam cube (upstream pass-0 swap, TileEntityBase10ConnectorRendered.java:114/:138), fresh
 * overlays the full-block fresh foam on top of the pipe body (the pass-7 overlay, :108/:137).
 * The owned variant rides the texture choice (upstream :261-262 mOwnable ternary); the tint
 * colour does NOT ride this snapshot — it is the PAINT property (applyFoam paints the pipe
 * the foam colour, upstream :161 mIsPainted=T + :163 mRGBa).
 *
 * <p>Own property key ({@code GTModelProperties#FOAM_SNAPSHOT}): the pipe also carries the
 * {@code RENDER_SNAPSHOT} flow arrows, and ModelProperties are single-valued — the
 * GTModelProperties:46-55 coexistence ruling sends each consumer model to its own key.
 * Thread safety per the snapshot contract: booleans are immutable, the record is deeply
 * frozen by construction. Absent property = no foam (the plain pipe).
 *
 * @param dried {@code mFoamDried} — hardened foam renders as the full-block body swap
 * @param owned {@code mOwnable} — the player-owned foam texture variant (upstream :261-262)
 */
public record PipeFoamSnapshot(boolean dried, boolean owned) implements GTRenderSnapshot {
}
