package gregtech6.client.render;

import java.util.Locale;

/**
 * The immutable oven render snapshot (task p9-render-c-oven-overlay) — the
 * {@link GTRenderSnapshot} record {@link gregtech6.tileentity.machines.TileEntityOven}
 * hands to the render thread through {@link GTModelProperties#OVEN_SNAPSHOT}. It is the
 * read-only projection of the BE's {@code mActive}/{@code mRunning} fields taken at the
 * {@code getModelData()} moment (single-writer discipline: the only writer of those
 * fields is the BE itself — the blockstate ACTIVE/RUNNING properties are driven from the
 * same fields via {@code applyVisualState}, and this snapshot is a second reader of the
 * identical bits, never a second write path).
 *
 * <p>Snapshot contract (ADR 2026-09-01-p9-render-c-oven-overlay ④): deeply immutable,
 * self-contained, never holds a {@code net.minecraft.world.level.block.entity.BlockEntity}
 * (the render thread may read it on a chunk worker thread while the tick thread mutates
 * the live BE — ModelData.java:28 iron law). Continuous quantities (mProgress, the GUI's
 * progress bar payload, upstream :1018-1019) are FORBIDDEN here — they would force a
 * chunk rebuild per tick; upstream shows no in-world progress either
 * (ContainerClientBasicMachine.java:53-65 is the GUI-only carrier). {@code mSuccessful}
 * carries no visual payload upstream (only the progress value snaps to max) and stays out.
 *
 * <p>Upstream: the visual byte {@code (mActive?1:0)|(mRunning?2:0)}
 * (MultiTileEntityBasicMachine.java:1010-1011) and the overlay pick
 * {@code mActive||worldObj==null ? Active : mRunning ? Running : Inactive} (:1014). The
 * {@code worldObj==null} branch is a declared non-port (the client BE always has a
 * world), so the :1014 quirk collapses to: {@code active} always wins over
 * {@code running} — see {@link #overlayGroup()}.
 */
public record GTOvenRenderSnapshot(boolean active, boolean running) implements GTRenderSnapshot {

	/** The state overlay the upstream texture pick renders for this machine state (:1014). */
	public enum OvenOverlayGroup {
		/** Inactive — upstream renders the plain {@code overlay} group; the port renders no overlay quad (the A-tier fallback already carries the inactive face). */
		NONE,
		/** Upstream {@code overlay_active} group (bit 0, mActive). */
		ACTIVE,
		/** Upstream {@code overlay_running} group (bit 1, mRunning). */
		RUNNING;

		/** The texture-path token ({@code block/oven_overlay_<key>_<face>}); lowercase per the 1.20.1 ResourceLocation charset. */
		public String textureKey() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * The upstream :1014 overlay pick, port-ism applied ({@code worldObj==null} branch
	 * dropped — client BEs always have a world): {@code mActive} wins over {@code mRunning}
	 * even when both bits are set (the {@code active&&running → Active overlay} quirk the
	 * offline truth table pins). The generated A-tier blockstate mirrors the same pick
	 * (GT6BlockStates addMachine: {@code ACTIVE ? oven_active : RUNNING ? oven_running}).
	 */
	public OvenOverlayGroup overlayGroup() {
		return active ? OvenOverlayGroup.ACTIVE : running ? OvenOverlayGroup.RUNNING : OvenOverlayGroup.NONE;
	}
}
