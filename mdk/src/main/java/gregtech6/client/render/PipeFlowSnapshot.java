package gregtech6.client.render;

import net.minecraft.core.Direction;

/**
 * The immutable flow-control render snapshot (task pipe-flow-control spec ④) — the
 * {@link GTRenderSnapshot} record the pipe BE hands to the render thread through
 * {@code getModelData()}: one bit per face ({@code SBIT[side]}, the GT6 side order ==
 * {@code Direction.get3DDataValue()} order) telling the model where to bake the output
 * arrows. The arrow GEOMETRY stays model-side in {@code GTFluidPipeFlowModel}.
 *
 * <p>Task pipe-output-arrows — {@code rendersArms} splits the two carriers' semantics
 * (the user ruling, port-native): the FLUID pipe's mIoMask marks are explicit output
 * direction marks, so a marked face must ALSO render the connected-arm form
 * ({@code GTRodBakedModel} ORs the mask into its geometry — "the specified face looks
 * connected, the same as a plain-click connect"); the ITEM pipe's mask is DERIVED as the
 * complement of the four-state disable layer — a dense mask (five still-emitting faces
 * for one disable click) that must NOT spawn arms. Zero-mask pipes keep
 * {@code ModelData.EMPTY} (no snapshot property) and render through the plain blockstate
 * model. Thread safety per the snapshot contract: two bytes are immutable; the record is
 * deeply frozen by construction.
 */
public record PipeFlowSnapshot(byte outputMask, boolean rendersArms) implements GTRenderSnapshot {

	public PipeFlowSnapshot {
		outputMask = (byte)(outputMask & 63); // bit0-5 clamp, the mConnections form
	}

	/** The compat form: a snapshot that does not drive the arm-form override. */
	public PipeFlowSnapshot(byte outputMask) {
		this(outputMask, false);
	}

	/** Whether the given face carries the output arrow. */
	public boolean hasArrow(Direction aFace) {
		return (outputMask & (1 << aFace.get3DDataValue())) != 0;
	}
}
