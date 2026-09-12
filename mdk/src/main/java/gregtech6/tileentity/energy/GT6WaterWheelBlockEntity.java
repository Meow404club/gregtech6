package gregtech6.tileentity.energy;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Water Wheel BlockEntity (task p28-c-water-wheel) — a RU DIRECT-CURRENT source with
 * SIGNED packets: river flow turns the wheel, the turning becomes one {@code ±8} RU
 * packet per tick pushed out along the block axis, the sign carrying the rotation
 * direction (the fourth RU source semantics beside the crank's negative DC, the diesel
 * engine's positive DC and the steam engine's KU square wave). Clean-room port of the
 * kTFRUAddon WaterMill behaviour contract (WaterMill.java:45-153, AGPL-3.0 — the
 * SEMANTICS only: flow-vector scan → torque sign → axial packet push; not one line of
 * that code is reproduced, the math below is re-derived in the port's own vector form).
 *
 * <p><b>The rotation scan (WaterMill :91 + :126-153 semantics)</b>: every
 * {@link #TORQUE_SCAN_PERIOD} ticks the six neighbours are classified — a liquid
 * neighbour contributes its {@code FluidState.getFlow} vector (FluidState.java:91, the
 * 1.20.1 counterpart of BlockLiquid.getFlowVector), an opaque non-liquid neighbour
 * BURIES the wheel (torque = 0 immediately, the :138 stall) and anything else is skipped.
 * The per-sample torque is the tangential drive
 * {@code flow⊥axis · (offset × axis)}: the flow is projected onto the rotation plane
 * (the axis component is the water rushing PAST the wheel, no blade contact) and dotted
 * with the blade tangent at that offset (the cross product with the axis — a sample ON
 * the axis has a zero tangent, no lever arm, so the two axle-mounted ends contribute
 * nothing by construction). The scan is a PURE static function ({@link #tangentialTorque}
 * + {@link #rotationSign}) over {@link Neighbor} records — the four-quadrant table lives
 * in the offline test.
 *
 * <p><b>The dead band (WaterMill :97 {@code |rotateTorque| < 0.1f})</b>: a torque sum
 * below {@link #TORQUE_DEAD_BAND} means the wheel stalls — still source blocks answer a
 * zero flow vector (the "无水流" stop) and a barely-grazing flow does not overcome the
 * friction ("水流不足"). Vanilla {@code getFlow} answers NORMALIZED vectors (FlowingFluid
 * getFlow tail {@code return $$12.normalize()}), so 0.1 is a coarse-but-honest graze gate.
 *
 * <p><b>The output (WaterMill :101-105 semantics)</b>: one {@link #OUTPUT_PACKET_SIZE}
 * RU packet ({@link #OUTPUT_AMPERES} = 1) per running tick, tried into the POSITIVE axis
 * end first (the upstream front-preference: consumed there = the other end is NOT fed,
 * the wheel feeds ONE line at a time) and falling back to the NEGATIVE end; the packet
 * sign flips across the axis (the same spin is +clockwise out of one end and
 * −clockwise out of the other — the axle's mRotationDir math and a future relay's
 * reverse-sign check read exactly this).
 *
 * <p><b>Chain closure (the p28 design ruling, pinned by the test)</b>: {@code 8} RU sits
 * dead-centre in the Electric Dynamo ULV T0 row's input window {@code [4, 16]} (in8/out8
 * × 1A — one wheel runs one dynamo at full rate), it is the {@code GTWireSpecs.V[0] = 8}
 * wire domain, and it is BELOW the LV machine input-min 16 — the wheel cannot drive any
 * pre-ULV machine directly (the tier gate that makes the wheel the ULV ecosystem's
 * dedicated source).
 *
 * <p><b>Cropped with declaration (the research-card v1 rulings)</b>: the RU RELAY
 * (WaterMill :108-124 isEnergyAcceptingFrom/doInject forwarding + the |packet|>16 and
 * reverse-sign overcharge explosions — the wheel is a pure source, it accepts nothing),
 * the TFC wood rot + bronze-ring repair (:53/:162-171), the empty-hand torque readout
 * (:157-161) and the blade spin visuals (the blockstate rotation visual is the declared
 * defer — the ACTIVE output rides the BE, one shared static texture renders for now).
 *
 * <p><b>Axis</b>: the BlockState AXIS is the authority, {@link #mAxis} the runtime mirror
 * re-synced at each tick head (the axle syncAxisFromState form) — the RCON
 * {@code /setblock} path lands through the state.
 */
public class GT6WaterWheelBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The NBT key of the cached rotation sign (the WaterMill writeToNBT2 form). */
	public static final String NBT_TORQUE_SIGN = "gt.torque.sign";

	/**
	 * The RU packet size pushed per running tick (the p28 design ruling: 8 — the V[0]
	 * wire domain, the dynamo T0 window centre, below every pre-ULV machine's input-min;
	 * the class doc chain-closure block carries the evidence web).
	 */
	public static final long OUTPUT_PACKET_SIZE = 8;

	/** The packets per running tick (the WaterMill :101 {@code torque>2?2:1} folds to 1 — no torque-magnitude amps in this port). */
	public static final long OUTPUT_AMPERES = 1;

	/** The stall dead band (WaterMill :97 — {@code Math.abs(rotateTorque) < 0.1f} stopped the wheel). */
	public static final double TORQUE_DEAD_BAND = 0.1;

	/** The torque re-scan cadence (WaterMill :91 {@code aTimer % 10 == 0}). */
	public static final int TORQUE_SCAN_PERIOD = 10;

	/**
	 * One neighbour sample of the rotation scan — the pure-function record (the live walk
	 * {@link #collectNeighbors()} builds these off the level; the offline tests author
	 * them by hand).
	 *
	 * @param direction the neighbour direction OFF the wheel position
	 * @param liquid    the neighbour carries a non-empty FluidState
	 * @param opaque    the neighbour is a solid-render block (only meaningful when !liquid)
	 * @param flow      the neighbour's flow vector (only meaningful when liquid — the
	 *                  FluidState.getFlow answer, normalized or zero)
	 */
	public record Neighbor(Direction direction, boolean liquid, boolean opaque, Vec3 flow) {
		/** A liquid sample with a zero flow (a still source block). */
		public static Neighbor still(Direction aDirection) {
			return new Neighbor(aDirection, true, false, Vec3.ZERO);
		}

		/** A liquid sample with the given flow vector. */
		public static Neighbor flowing(Direction aDirection, Vec3 aFlow) {
			return new Neighbor(aDirection, true, false, aFlow);
		}

		/** An opaque non-liquid sample (the burying block). */
		public static Neighbor opaque(Direction aDirection) {
			return new Neighbor(aDirection, false, true, Vec3.ZERO);
		}

		/** A transparent non-liquid sample (skipped — open air, glass, an axle). */
		public static Neighbor open(Direction aDirection) {
			return new Neighbor(aDirection, false, false, Vec3.ZERO);
		}
	}

	/** The axis mirror (the axle mAxis form: the BlockState AXIS is the authority). */
	public Direction.Axis mAxis = Direction.Axis.X;

	/**
	 * The cached rotation sign ∈ {-1, 0, +1} — re-scanned every
	 * {@link #TORQUE_SCAN_PERIOD} ticks (only with a level; the offline fixture keeps its
	 * injected value), +1 = the counterclockwise-about-the-positive-axis spin of the
	 * canonical test vortex.
	 */
	public byte mTorqueSign = 0;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GT6WaterWheelBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the axle form). */
	public GT6WaterWheelBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBlockEntities.WATER_WHEEL_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "water_wheel"; // BET registry path mirrors it (GTBlockEntities.WATER_WHEEL_BE)
	}

	// ---------------------------------------------------------------------------
	// the tick (WaterMill :86-106 server branch semantics)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		syncAxisFromState();
		// WaterMill :91 — the periodic re-scan (level-gated: a level-less offline fixture
		// has nothing to scan and keeps its injected sign)
		if (aTimer % TORQUE_SCAN_PERIOD == 0 && hasLevel()) {
			mTorqueSign = (byte) scanRotationSign(mAxis, collectNeighbors());
		}
		if (mTorqueSign == 0) return; // WaterMill :97 — the stall gate

		// WaterMill :101-105 — the axial push, front (positive end) first, the sign
		// flipping across the axis; the back end only fires when the front is refused
		byte tFront = (byte) Direction.fromAxisAndDirection(mAxis, Direction.AxisDirection.POSITIVE).get3DDataValue();
		byte tBack = (byte) Direction.fromAxisAndDirection(mAxis, Direction.AxisDirection.NEGATIVE).get3DDataValue();
		long tFrontSize = mTorqueSign * OUTPUT_PACKET_SIZE;
		long tUsed = ITileEntityEnergy.Util.emitEnergyToSide(TD.Energy.RU, tFront, tFrontSize, OUTPUT_AMPERES, this, adjacency());
		if (tUsed == 0) {
			ITileEntityEnergy.Util.emitEnergyToSide(TD.Energy.RU, tBack, -tFrontSize, OUTPUT_AMPERES, this, adjacency());
		}
	}

	// ---------------------------------------------------------------------------
	// the pure rotation math (the four-quadrant table's subject)
	// ---------------------------------------------------------------------------

	/** The unit axis vector ({@code offset × axis} and the projection need it in Vec3 form). */
	public static Vec3 axisVec(Direction.Axis aAxis) {
		return switch (aAxis) {
			case X -> new Vec3(1, 0, 0);
			case Y -> new Vec3(0, 1, 0);
			case Z -> new Vec3(0, 0, 1);
		};
	}

	/**
	 * The torque SUM (pure): for every liquid neighbour,
	 * {@code (flow − axis·(flow·axis)) · (offset × axis)} — the plane-projected flow
	 * dotted with the blade tangent at the offset. Non-liquid neighbours contribute zero
	 * HERE (the opaque stall is the caller's early-out, {@link #scanRotationSign}) so the
	 * sum itself never mixes concerns. A sample ON the axis has a zero
	 * {@code offset × axis} — no lever arm, no contribution (the axle-mounted ends).
	 */
	public static double tangentialTorque(Direction.Axis aAxis, List<Neighbor> aNeighbors) {
		Vec3 tAxis = axisVec(aAxis);
		double rTorque = 0;
		for (Neighbor tNeighbor : aNeighbors) {
			if (!tNeighbor.liquid()) continue;
			Vec3 tOffset = offsetVec(tNeighbor.direction());
			Vec3 tFlow = tNeighbor.flow();
			Vec3 tFlowT = tFlow.subtract(tAxis.scale(tFlow.dot(tAxis))); // the plane projection
			rTorque += tFlowT.dot(tOffset.cross(tAxis)); // the tangential drive
		}
		return rTorque;
	}

	/** The dead-band sign of a torque sum: +1 / −1 / 0 (WaterMill :97). */
	public static int rotationSign(double aTorque) {
		if (aTorque > TORQUE_DEAD_BAND) return 1;
		if (aTorque < -TORQUE_DEAD_BAND) return -1;
		return 0;
	}

	/**
	 * The full scan (pure): the opaque non-liquid neighbour BURIES the wheel — 0
	 * immediately (WaterMill :138 {@code isOpaqueCube → return 0}, evaluated BEFORE the
	 * liquid samples accumulate); otherwise the dead-band sign of the torque sum.
	 */
	public static int scanRotationSign(Direction.Axis aAxis, List<Neighbor> aNeighbors) {
		for (Neighbor tNeighbor : aNeighbors) {
			if (!tNeighbor.liquid() && tNeighbor.opaque()) return 0;
		}
		return rotationSign(tangentialTorque(aAxis, aNeighbors));
	}

	/** The neighbour direction as the offset vector (Direction.getStepX/Y/Z). */
	public static Vec3 offsetVec(Direction aDirection) {
		return new Vec3(aDirection.getStepX(), aDirection.getStepY(), aDirection.getStepZ());
	}

	// ---------------------------------------------------------------------------
	// the live walk (the only level-touching half of the scan)
	// ---------------------------------------------------------------------------

	/**
	 * All six neighbours as {@link Neighbor} records. NO axis filtering: the math handles
	 * the axial degeneracy (an axial liquid has a zero lever arm, an axle neighbour is
	 * non-liquid non-opaque = open). WaterMill scanned its five non-front neighbours; the
	 * six-face walk is the port's strictly-cleaner equivalent.
	 */
	private List<Neighbor> collectNeighbors() {
		List<Neighbor> rNeighbors = new ArrayList<>(6);
		for (Direction tDirection : Direction.values()) {
			BlockPos tNeighborPos = getBlockPos().relative(tDirection);
			BlockState tState = getLevel().getBlockState(tNeighborPos);
			if (!tState.getFluidState().isEmpty()) {
				rNeighbors.add(Neighbor.flowing(tDirection, tState.getFluidState().getFlow(getLevel(), tNeighborPos)));
			} else if (tState.isSolidRender(getLevel(), tNeighborPos)) {
				rNeighbors.add(Neighbor.opaque(tDirection)); // WaterMill :138 — the burying block
			} else {
				rNeighbors.add(Neighbor.open(tDirection));
			}
		}
		return rNeighbors;
	}

	// ---------------------------------------------------------------------------
	// the energy face family (the diesel engine form)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(gregapi.code.TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting && aEnergyType == gregapi.data.TD.Energy.RU; // pure source: the emitting flag kills every accept face
	}

	@Override
	public boolean isEnergyEmittingTo(gregapi.code.TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isEnergyType(aEnergyType, aSide, true) && Direction.from3DDataValue(aSide).getAxis() == mAxis; // the two axis ends
	}

	@Override
	public java.util.Collection<gregapi.code.TagData> getEnergyTypes(byte aSide) {
		return gregapi.data.TD.Energy.RU.AS_LIST;
	}

	@Override
	public long getEnergyOffered(gregapi.code.TagData aEnergyType, byte aSide, long aSize) {
		return OUTPUT_PACKET_SIZE;
	}

	@Override
	public long getEnergySizeOutputRecommended(gregapi.code.TagData aEnergyType, byte aSide) {
		return OUTPUT_PACKET_SIZE;
	}

	@Override
	public long getEnergySizeOutputMin(gregapi.code.TagData aEnergyType, byte aSide) {
		return OUTPUT_PACKET_SIZE; // the diesel-engine fixed-band form (:230) — the packet size is a constant, not a window
	}

	@Override
	public long getEnergySizeOutputMax(gregapi.code.TagData aEnergyType, byte aSide) {
		return OUTPUT_PACKET_SIZE;
	}

	// ---------------------------------------------------------------------------
	// the adjacency seam (the axle/diesel D1 form)
	// ---------------------------------------------------------------------------

	/** The offline test seam (the axle mAdjacencyOverride form). */
	private gregapi.tileentity.energy.IEnergyAdjacency mAdjacencyOverride = null;

	void setAdjacencyOverride(@Nullable gregapi.tileentity.energy.IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	private gregapi.tileentity.energy.IEnergyAdjacency adjacency() {
		if (mAdjacencyOverride != null) return mAdjacencyOverride;
		return aSide -> {
			if (!hasLevel()) return null;
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (tNeighbor == null || tNeighbor.isRemoved()) return null;
			byte tOpposite = (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new gregapi.tileentity.energy.EnergyTarget(tNeighbor, tOpposite);
		};
	}

	/** The axis mirror re-sync (the axle syncAxisFromState form, keyed on the PROPERTY). */
	void syncAxisFromState() {
		if (getBlockState().hasProperty(BlockStateProperties.AXIS)) {
			mAxis = getBlockState().getValue(BlockStateProperties.AXIS);
		}
	}

	// ---------------------------------------------------------------------------
	// NBT (the WaterMill writeToNBT2/readFromNBT2 form — the cached sign rides the save)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_TORQUE_SIGN, mTorqueSign);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_TORQUE_SIGN, Tag.TAG_ANY_NUMERIC)) mTorqueSign = aNBT.getByte(NBT_TORQUE_SIGN);
	}
}
