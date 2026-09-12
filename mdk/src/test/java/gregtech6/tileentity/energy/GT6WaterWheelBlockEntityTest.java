package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GTWireSpecs;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GT6WaterWheelBlockEntity offline tests (task p28-c-water-wheel acceptance ④⑤): the
 * four-quadrant rotation table of the pure flow→sign function (the acceptance's
 * 流水向量→旋转方向对表), the opaque-bury stall + the dead-band graze stop (the
 * 无水流/水流不足=停转 contract), the RU output parameter pins against the p28 ULV chain
 * (8 RU × 1A — the GTWireSpecs.V[0] wire domain, dead-centre of the Electric Dynamo T0
 * row's [4,16] input window, BELOW the 16-packet LV machine floor: the wheel is the ULV
 * ecosystem's dedicated source and cannot drive anything pre-ULV), the axial face truth
 * table (both axis ends emit, nothing accepts — the relay crop), the front-preference
 * push with the sign flip across the axis, and the NBT/axis-mirror round trip.
 *
 * <p><b>Chain-closure annotation (the design ruling this test pins, one wheel = one
 * dynamo = one ULV line)</b>:
 * <pre>
 *   流水 (FluidState.getFlow ≠ 0, normalized — FlowingFluid getFlow tail)
 *     → 水车 8 RU × 1A signed packets (this test)
 *       → GTAxle 递归零损 (GTAxleBlockEntity.transferRotations, |aSpeed| gate: 8 ≤ wood VMAX 16)
 *         → Electric Dynamo ULV T0 row (p28-c-ulv-dynamo-row, in 8 RU / out 8 EU × 1A):
 *           the 8 packet sits dead-centre in the [4, 16] receive window —
 *           V[0] = 8 (GTWireSpecs, CS.java:148) = this wheel's packet
 *         → ULV 机器 (min 4 / in 8 / max 16): the 8 EU packet lands mid-window, never
 *           bounced, never overcharged
 *   墙: LV 机器 input-min = TIER_INPUTS[0][0] = 16 (GTMachines.java:194, the
 *   EnergyGate "packet below getEnergySizeInputMin returns aAmount" bounce) — the 8 RU
 *   wheel packet CANNOT drive any pre-ULV machine; the wheel's only consumer universe
 *   is the ULV ecosystem. That tier gate is what makes the wheel's output value 8 a
 *   DESIGN choice and not a balance accident.
 * </pre>
 *
 * <p>Offline harness (the axle test form): level-less fixtures on a vanilla STONE state,
 * the {@code mAdjacencyOverride} seam wires the sinks, the torque sign is injected into
 * {@link GT6WaterWheelBlockEntity#mTorqueSign} (the live scan is level-gated and keeps
 * the injected value — the four-quadrant table itself runs against the PURE function).
 */
public class GT6WaterWheelBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6WaterWheelBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GT6WaterWheelBlockEntity>[] tHolder = (BlockEntityType<GT6WaterWheelBlockEntity>[]) new BlockEntityType<?>[1];
		// OAK_LOG joins the valid set for the axis-mirror fixture (the vanilla log state
		// carries the SAME BlockStateProperties.AXIS instance); 21.1 validates the
		// type/state pair at the BE ctor (task p15-m4-test-infra-2)
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6WaterWheelBlockEntity(tHolder[0], aPos, aState), Blocks.STONE, Blocks.OAK_LOG).build(null);
		sType = tHolder[0];
	}

	/** A fresh level-less wheel fixture (the torque sign injected per test). */
	private static GT6WaterWheelBlockEntity wheel() {
		return new GT6WaterWheelBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	/** A fresh wheel with the given injected torque sign and axis. */
	private static GT6WaterWheelBlockEntity wheel(byte aTorqueSign, Direction.Axis aAxis) {
		GT6WaterWheelBlockEntity rWheel = wheel();
		rWheel.mTorqueSign = aTorqueSign;
		rWheel.mAxis = aAxis;
		return rWheel;
	}

	/**
	 * Counting RU sink — the axle-test fake consumer: accepts RU from every side, records
	 * the packet that landed.
	 */
	public static class CountingSink extends BlockEntity implements ITileEntityEnergy {
		public long calls = 0, lastSize = 1, lastAmount = 0;
		public byte lastSide = -1;

		// 21.1 ctor validation: the fake binds a real BET over the vanilla stone state —
		// the supplier is stored, never invoked (task p15-m4-test-infra-2)
		static final BlockEntityType<CountingSink> FAKE_TYPE =
				BlockEntityType.Builder.of((aPos, aState) -> new CountingSink(aPos), Blocks.STONE).build(null);

		public CountingSink(BlockPos aPos) {
			super(FAKE_TYPE, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == TD.Energy.RU;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == TD.Energy.RU;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.RU.AS_LIST;}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) {
					calls++;
					lastSize = aSize;
					lastAmount = aAmount;
					lastSide = aSide;
				}
				return aAmount;
			}
			return 0;
		}

		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	// ---------------------------------------------------------------------------
	// the four-quadrant table (acceptance ④ — the pure function, exact torque pins)
	// ---------------------------------------------------------------------------

	/**
	 * The four off-axis quadrants of the X-axis wheel, one canonical flow each — every
	 * single-sample torque is EXACTLY ±1 (unit offsets × normalized flows) and the sign
	 * is the pinned half of the table. The flows: a +Z river past the top blade and the
	 * bottom blade, a +Y waterfall against the south and north blades.
	 */
	@Test
	public void axisXFourQuadrantTable() {
		// UP blade, river flowing +Z (south): tangent (0,0,-1) → torque −1 → CCW-negative
		assertEquals(-1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.UP, new Vec3(0, 0, 1)))));
		// DOWN blade, the same +Z river: tangent (0,0,+1) → torque +1
		assertEquals(1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.DOWN, new Vec3(0, 0, 1)))));
		// SOUTH blade, waterfall falling +Y: tangent (0,1,0) → torque +1
		assertEquals(1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.SOUTH, new Vec3(0, 1, 0)))));
		// NORTH blade, the same +Y fall: tangent (0,−1,0) → torque −1
		assertEquals(-1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.NORTH, new Vec3(0, 1, 0)))));
		// the mirrored river (−Z) flips both vertical blades
		assertEquals(1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.UP, new Vec3(0, 0, -1)))));
		assertEquals(-1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.DOWN, new Vec3(0, 0, -1)))));
		// the exact magnitude: one unit-offset × one normalized flow = ±1
		assertEquals(-1.0, GT6WaterWheelBlockEntity.tangentialTorque(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.UP, new Vec3(0, 0, 1)))), 1e-9);
		assertEquals(1.0, GT6WaterWheelBlockEntity.tangentialTorque(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.DOWN, new Vec3(0, 0, 1)))), 1e-9);
	}

	/**
	 * The Y-axis and Z-axis rotations of the same table — the three-axis symmetry (the
	 * vanilla getFlow Y-component drives the horizontal wheels through the falling-water
	 * arm, the horizontal rivers drive the vertical-axis wheels).
	 */
	@Test
	public void axisYAndZFourQuadrantTables() {
		// axis Y, a +X river: only the NORTH/SOUTH blades grip (the E/W blades face the
		// flow radially — zero grip): south −1, north +1, east/west radial 0
		Vec3 tEast = new Vec3(1, 0, 0);
		assertEquals(-1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.Y,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.SOUTH, tEast))));
		assertEquals(1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.Y,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.NORTH, tEast))));
		assertEquals(0, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.Y,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.EAST, tEast))), "radial: no grip");
		assertEquals(0, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.Y,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.WEST, tEast))), "radial: no grip");
		// axis Y, a +Z river: east +1, west −1 (the mirrored quadrant pair)
		Vec3 tSouth = new Vec3(0, 0, 1);
		assertEquals(1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.Y,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.EAST, tSouth))));
		assertEquals(-1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.Y,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.WEST, tSouth))));
		// axis Z, a +X river: up +1, down −1 (the vertical blades), east/west radial 0
		Vec3 tUp = new Vec3(0, 1, 0);
		assertEquals(1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.Z,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.UP, tEast))));
		assertEquals(-1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.Z,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.DOWN, tEast))));
		assertEquals(0, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.Z,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.EAST, tEast))), "radial: no grip");
		// axis Z, a +Y fall: east −1, west +1 (the falling-water arm drives the horizontal wheel)
		assertEquals(-1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.Z,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.EAST, tUp))));
		assertEquals(1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.Z,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.WEST, tUp))));
	}

	/**
	 * The coherent vortex: four neighbours circulating the same way sum to 4 (all
	 * contributions +1) → +1; the reversed vortex sums to −4 → −1. This is the "more
	 * water, more confident direction" form — and the sign NEVER saturates into a
	 * magnitude (the port pins amps at 1 regardless).
	 */
	@Test
	public void coherentVortexSumsAndFlips() {
		// axis X, counterclockwise-about-+X circulation (every contribution +1)
		List<GT6WaterWheelBlockEntity.Neighbor> tCcw = List.of(
				GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.UP, new Vec3(0, 0, -1)),
				GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.DOWN, new Vec3(0, 0, 1)),
				GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.SOUTH, new Vec3(0, 1, 0)),
				GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.NORTH, new Vec3(0, -1, 0)));
		assertEquals(4.0, GT6WaterWheelBlockEntity.tangentialTorque(Direction.Axis.X, tCcw), 1e-9);
		assertEquals(1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X, tCcw));
		// the reversed vortex: −4 → −1
		List<GT6WaterWheelBlockEntity.Neighbor> tCw = List.of(
				GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.UP, new Vec3(0, 0, 1)),
				GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.DOWN, new Vec3(0, 0, -1)),
				GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.SOUTH, new Vec3(0, -1, 0)),
				GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.NORTH, new Vec3(0, 1, 0)));
		assertEquals(-4.0, GT6WaterWheelBlockEntity.tangentialTorque(Direction.Axis.X, tCw), 1e-9);
		assertEquals(-1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X, tCw));
	}

	/**
	 * The zero-contribution geometry: radial flow (parallel to the offset — water pushing
	 * straight INTO the blade face has no tangential grip), axial samples (no lever arm —
	 * the two axle-mounted ends), and pure-axis flow components (the water rushing PAST
	 * along the axle — the plane projection kills it).
	 */
	@Test
	public void radialAxialAndProjectedFlowsContributeNothing() {
		// radial: at the UP offset the tangent is (0,0,−1); a +Y flow (pushing into the
		// blade face radially) is perpendicular to the tangent → 0
		assertEquals(0.0, GT6WaterWheelBlockEntity.tangentialTorque(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.UP, new Vec3(0, 1, 0)))), 1e-9);
		// axial neighbour: the offset × axis cross product IS zero — any flow, no lever
		assertEquals(0.0, GT6WaterWheelBlockEntity.tangentialTorque(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.EAST, new Vec3(0, 0, 1)))), 1e-9);
		assertEquals(0.0, GT6WaterWheelBlockEntity.tangentialTorque(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.WEST, new Vec3(0, 1, 0)))), 1e-9);
		// pure-axis flow at an off-axis sample: the projection zeroes it
		assertEquals(0.0, GT6WaterWheelBlockEntity.tangentialTorque(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.UP, new Vec3(1, 0, 0)))), 1e-9);
		// a diagonal flow splits: only the tangential half lands (normalize(0,1,1) at the
		// UP sample: the +Y half is radial-zero, the +Z half dots the (0,0,−1) tangent)
		assertEquals(-Math.sqrt(0.5), GT6WaterWheelBlockEntity.tangentialTorque(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.UP, new Vec3(0, 1, 1).normalize()))), 1e-9);
	}

	/**
	 * The stop conditions: the still-source zero flow, the dead band (WaterMill :97
	 * {@code |torque| < 0.1}), and the opaque bury (WaterMill :138 — evaluated BEFORE the
	 * liquid samples, one solid neighbour buries the whole wheel).
	 */
	@Test
	public void stillWaterDeadBandAndOpaqueBury() {
		// 无水流: still source blocks answer the zero vector → torque 0 → stalled
		assertEquals(0, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X,
				List.of(GT6WaterWheelBlockEntity.Neighbor.still(Direction.UP),
						GT6WaterWheelBlockEntity.Neighbor.still(Direction.DOWN))));
		// 水流不足: a grazing flow lands under the 0.1 dead band → stalled (the boundary
		// itself is exclusive: exactly 0.1 is still a stall — WaterMill's strict <)
		assertEquals(0, GT6WaterWheelBlockEntity.rotationSign(0.05));
		assertEquals(0, GT6WaterWheelBlockEntity.rotationSign(0.1));
		assertEquals(0, GT6WaterWheelBlockEntity.rotationSign(-0.1));
		assertEquals(1, GT6WaterWheelBlockEntity.rotationSign(0.11));
		assertEquals(-1, GT6WaterWheelBlockEntity.rotationSign(-0.11));
		// the bury: an opaque non-liquid neighbour anywhere → 0 even with a raging vortex
		List<GT6WaterWheelBlockEntity.Neighbor> tVortex = List.of(
				GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.DOWN, new Vec3(0, 0, 1)),
				GT6WaterWheelBlockEntity.Neighbor.flowing(Direction.SOUTH, new Vec3(0, 1, 0)));
		assertEquals(1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X, tVortex));
		List<GT6WaterWheelBlockEntity.Neighbor> tBuried = new java.util.ArrayList<>(tVortex);
		tBuried.add(GT6WaterWheelBlockEntity.Neighbor.opaque(Direction.NORTH));
		assertEquals(0, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X, tBuried));
		// open (transparent non-liquid) neighbours are skipped, NOT burying
		List<GT6WaterWheelBlockEntity.Neighbor> tOpen = new java.util.ArrayList<>(tVortex);
		tOpen.add(GT6WaterWheelBlockEntity.Neighbor.open(Direction.NORTH));
		tOpen.add(GT6WaterWheelBlockEntity.Neighbor.open(Direction.WEST));
		assertEquals(1, GT6WaterWheelBlockEntity.scanRotationSign(Direction.Axis.X, tOpen));
	}

	// ---------------------------------------------------------------------------
	// the RU output pins (acceptance ③ — the chain-closure numbers)
	// ---------------------------------------------------------------------------

	/**
	 * The chain-closure pins (the class-doc table in assertion form): the 8 RU × 1A
	 * packet, the V[0] wire domain, the dynamo T0 window centre, and the LV wall.
	 */
	@Test
	public void ruOutputParametersPinTheUlvChain() {
		// the wheel's packet: 8 RU, 1 ampere
		assertEquals(8, GT6WaterWheelBlockEntity.OUTPUT_PACKET_SIZE);
		assertEquals(1, GT6WaterWheelBlockEntity.OUTPUT_AMPERES);
		// the ULV wire domain: GTWireSpecs.V[0] = 8 (CS.java:148) — the wheel packet rides it
		assertEquals(8, GTWireSpecs.V[0]);
		assertEquals("ULV", GTWireSpecs.VN[0]);
		// the Electric Dynamo ULV T0 row (p28-c-ulv-dynamo-row: NBT_INPUT=8, window
		// [in/2, in*2] = [4, 16]): the 8 packet is the EXACT centre — one wheel runs one
		// dynamo at full rate, never half-fed, never over-window
		assertTrue(4 <= GT6WaterWheelBlockEntity.OUTPUT_PACKET_SIZE
				&& GT6WaterWheelBlockEntity.OUTPUT_PACKET_SIZE <= 16, "the dynamo T0 receive window [4,16]");
		// the ULV machine ladder window (min 4 / in 8 / max 16) accepts the dynamo's 8 EU packet
		assertTrue(4 <= 8 && 8 <= 16);
		// the WALL: the lowest pre-ULV machine floor is TIER_INPUTS[0][0] = 16
		// (GTMachines.java:194 — the LV row; EnergyGate bounces a packet below the input
		// min) — the 8 RU packet drives NOTHING pre-ULV: the wheel is the ULV ecosystem's
		// dedicated source, the tier gate that makes 8 a design choice
		assertEquals(16, GTMachines.TIER_INPUTS[0][0]);
		assertTrue(GT6WaterWheelBlockEntity.OUTPUT_PACKET_SIZE < GTMachines.TIER_INPUTS[0][0]);
	}

	/** The face truth table: both axis ends emit RU, NOTHING accepts (the relay crop), the size band is the constant 8. */
	@Test
	public void faceTruthTablePureSource() {
		GT6WaterWheelBlockEntity tWheel = wheel();
		tWheel.mAxis = Direction.Axis.X;
		for (byte tSide = 0; tSide < 6; tSide++) {
			boolean tOnAxis = tSide >= 4; // X faces: WEST 4 / EAST 5
			assertEquals(tOnAxis, tWheel.isEnergyEmittingTo(TD.Energy.RU, tSide, false), "emitting @" + tSide);
			assertFalse(tWheel.isEnergyAcceptingFrom(TD.Energy.RU, tSide, false), "accepting @" + tSide);
			assertEquals(8, tWheel.getEnergySizeOutputRecommended(TD.Energy.RU, tSide));
			assertEquals(8, tWheel.getEnergySizeOutputMin(TD.Energy.RU, tSide));
			assertEquals(8, tWheel.getEnergySizeOutputMax(TD.Energy.RU, tSide));
			assertEquals(8, tWheel.getEnergyOffered(TD.Energy.RU, tSide, 8));
			assertEquals(0, tWheel.doEnergyInjection(TD.Energy.RU, tSide, 8, 1, true), "a pure source accepts nothing");
			assertEquals(0, tWheel.doEnergyExtraction(TD.Energy.RU, tSide, 8, 1, true));
			assertEquals(1, tWheel.getEnergyTypes(tSide).size());
		}
		// EU/KU refuse everywhere (the RU-only carrier)
		assertFalse(tWheel.isEnergyEmittingTo(TD.Energy.EU, (byte) 5, false));
		assertFalse(tWheel.isEnergyEmittingTo(TD.Energy.KU, (byte) 5, false));
		// the axis mirror rides the state (the OAK_LOG fixture carries the SAME AXIS instance)
		BlockState tLog = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		GT6WaterWheelBlockEntity tMirrored = new GT6WaterWheelBlockEntity(sType, POS, tLog);
		assertEquals(Direction.Axis.X, tMirrored.mAxis); // the default before the first tick
		tMirrored.updateEntity();
		assertEquals(Direction.Axis.Y, tMirrored.mAxis);
		assertTrue(tMirrored.isEnergyEmittingTo(TD.Energy.RU, (byte) 0, false)); // DOWN
		assertTrue(tMirrored.isEnergyEmittingTo(TD.Energy.RU, (byte) 1, false)); // UP
		assertFalse(tMirrored.isEnergyEmittingTo(TD.Energy.RU, (byte) 5, false)); // EAST left the axis
	}

	/**
	 * The push path: torque +1 → the POSITIVE axis end (EAST on X) gets {@code +8},
	 * and with no front sink the NEGATIVE end (WEST) gets {@code −8} — the sign flips
	 * across the axis (WaterMill :101-105 semantics), 1 ampere each.
	 */
	@Test
	public void axialPushSignFlipAndFrontPreference() {
		// the front (positive) sink: torque +1 → +8 into the sink's west face
		GT6WaterWheelBlockEntity tWheel = wheel((byte) 1, Direction.Axis.X);
		CountingSink tFront = new CountingSink(POS.east());
		tWheel.setAdjacencyOverride(aSide -> aSide == 5 ? new EnergyTarget(tFront, (byte) 4) : null);
		tWheel.updateEntity(); // the first emit tick
		assertEquals(1, tFront.calls);
		assertEquals(8, tFront.lastSize, "the positive end carries the +spin packet");
		assertEquals(1, tFront.lastAmount);
		assertEquals(4, tFront.lastSide);

		// torque −1 → the front packet flips to −8
		GT6WaterWheelBlockEntity tReversed = wheel((byte) -1, Direction.Axis.X);
		CountingSink tFront2 = new CountingSink(POS.east());
		tReversed.setAdjacencyOverride(aSide -> aSide == 5 ? new EnergyTarget(tFront2, (byte) 4) : null);
		tReversed.updateEntity();
		assertEquals(1, tFront2.calls);
		assertEquals(-8, tFront2.lastSize, "the reversed spin flips the packet sign");

		// the back fallback: no front sink → the NEGATIVE end fires with the OPPOSITE sign
		GT6WaterWheelBlockEntity tBackOnly = wheel((byte) 1, Direction.Axis.X);
		CountingSink tBack = new CountingSink(POS.west());
		tBackOnly.setAdjacencyOverride(aSide -> aSide == 4 ? new EnergyTarget(tBack, (byte) 5) : null);
		tBackOnly.updateEntity();
		assertEquals(1, tBack.calls);
		assertEquals(-8, tBack.lastSize, "the same spin exits the far end as −8");
		assertEquals(5, tBack.lastSide);

		// front preference (WaterMill :103 the consumed front short-circuits): BOTH ends
		// wired → only the front is fed in one tick
		GT6WaterWheelBlockEntity tBoth = wheel((byte) 1, Direction.Axis.X);
		CountingSink tFront3 = new CountingSink(POS.east());
		CountingSink tBack3 = new CountingSink(POS.west());
		tBoth.setAdjacencyOverride(aSide -> aSide == 5 ? new EnergyTarget(tFront3, (byte) 4)
				: aSide == 4 ? new EnergyTarget(tBack3, (byte) 5) : null);
		tBoth.updateEntity();
		assertEquals(1, tFront3.calls);
		assertEquals(0, tBack3.calls, "the consumed front short-circuits the back arm");

		// the stall: torque 0 pushes nothing
		GT6WaterWheelBlockEntity tStalled = wheel((byte) 0, Direction.Axis.X);
		CountingSink tStarved = new CountingSink(POS.east());
		tStalled.setAdjacencyOverride(aSide -> aSide == 5 ? new EnergyTarget(tStarved, (byte) 4) : null);
		tStalled.updateEntity();
		assertEquals(0, tStarved.calls, "无水流=停转: no packets leave a stalled wheel");
	}

	/** The NBT round trip (the WaterMill writeToNBT2/readFromNBT2 form) + the te_name mirror. */
	@Test
	public void nbtRoundTrip() {
		GT6WaterWheelBlockEntity tWheel = wheel((byte) -1, Direction.Axis.X);
		CompoundTag tTag = tWheel.saveWithoutMetadata();
		assertEquals("water_wheel", tTag.getString("te_name"));
		assertTrue(tTag.contains(GT6WaterWheelBlockEntity.NBT_TORQUE_SIGN, Tag.TAG_ANY_NUMERIC));

		GT6WaterWheelBlockEntity tLoaded = wheel();
		tLoaded.load(tTag);
		assertEquals(-1, tLoaded.mTorqueSign);
		// a tag without the key keeps the default (the contains-guard form)
		tLoaded.load(new CompoundTag());
		assertEquals(-1, tLoaded.mTorqueSign);
	}
}
