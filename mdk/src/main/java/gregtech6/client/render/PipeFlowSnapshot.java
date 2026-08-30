package gregtech6.client.render;

import net.minecraft.core.Direction;

/**
 * The immutable flow-control render snapshot (task p4-pipe-flow-control spec ④) — the
 * {@link GTRenderSnapshot} record the pipe BE hands to the render thread through
 * {@code getModelData()}: one bit per face ({@code SBIT[side]}, the GT6 side order ==
 * {@code Direction.get3DDataValue()} order) telling the model where to bake the output
 * arrows. The arrow GEOMETRY stays model-side in {@code GTFluidPipeFlowModel}.
 *
 * <p>Zero-mask pipes keep {@code ModelData.EMPTY} (no snapshot property) and render
 * through the plain blockstate model. Thread safety per the snapshot contract: a byte
 * is immutable; the record is deeply frozen by construction.
 */
public record PipeFlowSnapshot(byte outputMask) implements GTRenderSnapshot {

	public PipeFlowSnapshot {
		outputMask = (byte)(outputMask & 63); // bit0-5 clamp, the mConnections form
	}

	/** Whether the given face carries the output arrow. */
	public boolean hasArrow(Direction aFace) {
		return (outputMask & (1 << aFace.get3DDataValue())) != 0;
	}
}
