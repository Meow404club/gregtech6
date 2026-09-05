package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GTMachines;
import gregtech6.util.GTSideTables;

/**
 * The Dryer-family row acceptance (task p14-dryer-family, the OFFLINE half — the
 * TileEntityBasicMachineTierLadderTest shape): the four row records pinned to the upstream
 * columns (Loader_MultiTileEntities.java:1477-1480 — the name/material/hardness/NBT_INPUT/
 * NBT_ENERGY_ACCEPTED HU/RM.Drying/NBT_TEXTURE "dryer"/NBT_PARALLEL 8/16/32/64 +
 * NBT_PARALLEL_DURATION T/NBT_CHEAP_OVERCLOCKING T columns, the connectivity masks through
 * the :151/:143/:144/:137/:138 read forms), the row→BE carrier assignment
 * ({@link GTMachines#applyRow}, the W1a handoff — mEnergyInputs/mFluidInputs/mFluidOutputs
 * set directly), the rotated face geometry (the :511/:566/:575 gates over every horizontal
 * facing) and the declared-empty-map no-start (the W1b DRYING declaration state — the W3
 * pour is the next card). The registration half (4 blocks + 4 items + the 1 family BET
 * with T1-T4 validBlocks multi-attach) only resolves on a live server: the runServer/RCON
 * gate (the p8 ladder precedent — the registration never runs in offline tests).
 */
public class GTDryerFamilyRowTest extends TileEntityBasicMachineOfflineTestBase {

	/** CS.java:612 — SBIT_D|SBIT_A, the :151 NBT_ENERGY_ACCEPTED_SIDES read form. */
	static final byte ENERGY_IN_MASK = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	/** CS.java:612 — SBIT_B|SBIT_L|SBIT_A, the :143 NBT_TANK_SIDE_IN / :137 NBT_INV_SIDE_IN read form. */
	static final byte TANK_IN_MASK = (byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	/** CS.java:612 — SBIT_U|SBIT_A, the :144 NBT_TANK_SIDE_OUT read form. */
	static final byte TANK_OUT_MASK = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A);
	/** CS.java:612 — SBIT_R|SBIT_A, the :138 NBT_INV_SIDE_OUT read form. */
	static final byte ITEM_OUT_MASK = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A);

	// ------------------------------------------------------------------
	// the row table — the upstream :1477-1480 columns
	// ------------------------------------------------------------------

	@Test
	void dryerRowsMatchTheUpstreamColumns() {
		assertEquals(4, GTMachines.DRYER_ROWS.size(), "the four Dryer rows (:1477-1480)");
		for (int i = 0; i < GTMachines.DRYER_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.DRYER_ROWS.get(i);
			int tTier = i + 1; // the upstream tier index, 1-based in the messages
			assertEquals("dryer" + (i == 0 ? "" : "_t" + tTier), tRow.path(), "the registry path ladder");
			assertEquals(20311 + i, tRow.metaId(), "the MultiTile id column of tier " + tTier);
			assertEquals(tTier, tRow.tier() + 1, "the tier index");
			assertTrue(tRow.parallelDuration(), "NBT_PARALLEL_DURATION T on every row");
			assertSame(TD.Energy.HU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.HU on every row");
			assertEquals("dryer", tRow.texture(), "NBT_TEXTURE dryer on every row (the ladder shares the fronts)");
			assertSame(GT6RecipeMaps.DRYING, tRow.recipes().get(), "NBT_RECIPEMAP RM.Drying through the supplier");
			assertTrue(tRow.cheapOverclocking(), "NBT_CHEAP_OVERCLOCKING T on every row (:773 runs unconditionally)");
			assertNotNull(tRow.menu(), "the gt6:dryer menu supplier is bound on every row (task p16-machine-fluid-gui ① — "
					+ "the p14-dryer-family pool promise redeemed; the live registration resolves through the RCON gate, "
					+ "an offline .get() would touch the unbound RegistryObject)");
		}
		// the four differing columns, row by row
		assertEquals("Dryer (Steel)", GTMachines.DRYER_ROWS.get(0).displayName(), "the name column, Heat_T[1]");
		assertEquals("Dryer (Invar)", GTMachines.DRYER_ROWS.get(1).displayName(), "the name column, Heat_T[2]");
		assertEquals("Dryer (Titanium)", GTMachines.DRYER_ROWS.get(2).displayName(), "the name column, Heat_T[3]");
		assertEquals("Dryer (Tungsten Carbide)", GTMachines.DRYER_ROWS.get(3).displayName(), "the name column, Heat_T[4]");
		assertEquals("Steel", GTMachines.DRYER_ROWS.get(0).material(), "MT.DATA.Heat_T[1] = Steel (MT.java:3689)");
		assertEquals("Invar", GTMachines.DRYER_ROWS.get(1).material(), "Heat_T[2] = Invar");
		assertEquals("Titanium", GTMachines.DRYER_ROWS.get(2).material(), "Heat_T[3] = Titanium (MT.Ti local)");
		assertEquals("Tungsten Carbide", GTMachines.DRYER_ROWS.get(3).material(), "Heat_T[4] = TungstenCarbide local");
		assertEquals(6.0F, GTMachines.DRYER_ROWS.get(0).hardness(), "NBT_HARDNESS 6.0 (:1477)");
		assertEquals(4.0F, GTMachines.DRYER_ROWS.get(1).hardness(), "NBT_HARDNESS 4.0 (:1478)");
		assertEquals(9.0F, GTMachines.DRYER_ROWS.get(2).hardness(), "NBT_HARDNESS 9.0 (:1479)");
		assertEquals(12.5F, GTMachines.DRYER_ROWS.get(3).hardness(), "NBT_HARDNESS 12.5 (:1480)");
		assertEquals(8, GTMachines.DRYER_ROWS.get(0).parallel(), "NBT_PARALLEL 8");
		assertEquals(16, GTMachines.DRYER_ROWS.get(1).parallel(), "NBT_PARALLEL 16");
		assertEquals(32, GTMachines.DRYER_ROWS.get(2).parallel(), "NBT_PARALLEL 32");
		assertEquals(64, GTMachines.DRYER_ROWS.get(3).parallel(), "NBT_PARALLEL 64");
		// the connectivity + auto-side columns (identical on all four rows)
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.DRYER_ROWS) {
			assertEquals(ENERGY_IN_MASK, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_D through the :151 SBIT_A read");
			assertEquals(TANK_IN_MASK, tRow.fluidIn(), "NBT_TANK_SIDE_IN SBIT_B|SBIT_L through the :143 read");
			assertEquals(TANK_OUT_MASK, tRow.fluidOut(), "NBT_TANK_SIDE_OUT SBIT_U through the :144 read");
			assertEquals(TANK_IN_MASK, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_B|SBIT_L through the :137 read");
			assertEquals(ITEM_OUT_MASK, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R through the :138 read");
			assertEquals(5, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_BACK");
			assertEquals(1, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_TOP");
			assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
		}
	}

	// ------------------------------------------------------------------
	// the row → BE carrier assignment (the factory body, fixture-driven)
	// ------------------------------------------------------------------

	/** A fixture built exactly as the DRYER_BE factory body builds it (the tier-ladder tierMachine shape). */
	private static TileEntityBasicMachine dryerFixture(GTBasicMachineBlock.MachineRow aRow) {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.DRYING, aRow.parallel(), aRow.parallelDuration(), TD.Energy.HU);
		long[] tInputs = GTMachines.TIER_INPUTS[aRow.tier()];
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return GTMachines.applyRow(tMachine, aRow);
	}

	@Test
	void applyRowLandsTheCarrierAndTheTierInputs() {
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.DRYER_ROWS) {
			TileEntityBasicMachine tMachine = dryerFixture(tRow);
			long[] tInputs = GTMachines.TIER_INPUTS[tRow.tier()];
			assertEquals(tInputs[0], tMachine.mInputMin, tRow.path() + " InputMin = NBT_INPUT/2 (:126)");
			assertEquals(tInputs[1], tMachine.mInput, tRow.path() + " InputRec = NBT_INPUT (:126)");
			assertEquals(tInputs[2], tMachine.mInputMax, tRow.path() + " InputMax = NBT_INPUT*2 (:126)");
			assertSame(TD.Energy.HU, tMachine.mEnergyTypeAccepted, tRow.path() + " carrier");
			assertEquals(tRow.parallel(), tMachine.mParallel, tRow.path() + " NBT_PARALLEL (clamped at :130)");
			assertTrue(tMachine.mParallelDuration, tRow.path() + " NBT_PARALLEL_DURATION");
			assertEquals(ENERGY_IN_MASK, tMachine.mEnergyInputs, tRow.path() + " the :511 receiving mask (W1a carrier)");
			assertEquals(TANK_IN_MASK, tMachine.mFluidInputs, tRow.path() + " the :566 fill mask (W1a carrier)");
			assertEquals(TANK_OUT_MASK, tMachine.mFluidOutputs, tRow.path() + " the :575 drain mask (W1a carrier)");
			// the DRYING slot/tank shape (RM.java:71: items 1,1,0 / fluids 1,3,0)
			assertEquals(2, tMachine.getInventory().getSlots(), tRow.path() + " the 1+1 slot shape");
			assertEquals(1, tMachine.mTanksInput.length, tRow.path() + " the 1 input tank");
			assertEquals(3, tMachine.mTanksOutput.length, tRow.path() + " the 3 output tanks");
			assertEquals(1000, tMachine.mTanksInput[0].getCapacity(), tRow.path() + " the constructed 1000 default");
		}
	}

	// ------------------------------------------------------------------
	// the rotated face geometry (the :511/:566/:575 gates, every facing)
	// ------------------------------------------------------------------

	@Test
	void dryerFaceGeometryIsTheRotatedRowMasks() {
		for (byte tFacing = 2; tFacing <= 5; tFacing++) {
			for (byte tWorldSide = 0; tWorldSide <= 5; tWorldSide++) {
				for (GTBasicMachineBlock.MachineRow tRow : GTMachines.DRYER_ROWS) {
					boolean tEnergy = GTSideTables.faceConnected(tFacing, tWorldSide, tRow.energySides());
					assertEquals(tWorldSide == 0, tEnergy,
							tRow.path() + " accepts energy ONLY on the world bottom for every facing (facing " + tFacing + ", side " + tWorldSide + ")");
					boolean tOut = GTSideTables.faceConnected(tFacing, tWorldSide, tRow.fluidOut());
					assertEquals(tWorldSide == 1, tOut,
							tRow.path() + " drains ONLY on the world top for every facing (facing " + tFacing + ", side " + tWorldSide + ")");
				}
			}
		}
		// the fluid-in mask: exactly two HORIZONTAL world faces per facing (the rotated
		// back|left pair), the placed default (facing north) being east+south — the RCON
		// chain's positive fill arms
		for (byte tFacing = 2; tFacing <= 5; tFacing++) {
			List<Direction> tIn = acceptedFaces(tFacing, TANK_IN_MASK);
			assertEquals(2, tIn.size(), "exactly the back|left pair after rotation (facing " + tFacing + ")");
			assertFalse(tIn.contains(Direction.UP) || tIn.contains(Direction.DOWN),
					"the tank-in mask never opens a vertical face (facing " + tFacing + ")");
		}
		assertEquals(List.of(Direction.SOUTH, Direction.EAST), acceptedFaces((byte)2, TANK_IN_MASK),
				"facing north: relative back = world south, relative left = world east");
	}

	/** The world faces the rotated mask accepts, in Direction order. */
	private static List<Direction> acceptedFaces(byte aFacing, byte aMask) {
		java.util.List<Direction> rFaces = new java.util.ArrayList<>();
		for (Direction tSide : Direction.values()) {
			if (GTSideTables.faceConnected(aFacing, (byte)tSide.get3DDataValue(), aMask)) rFaces.add(tSide);
		}
		return rFaces;
	}

	// ------------------------------------------------------------------
	// the live fluid face gates (the getFluidTankFillable/Drainable shapes)
	// ------------------------------------------------------------------

	@Test
	void dryerFluidFaceGatesRejectAndAcceptLive() {
		TileEntityBasicMachine tMachine = dryerFixture(GTMachines.DRYER_ROWS.get(0)); // facing default north (2)
		FluidStack tWater = new FluidStack(Fluids.WATER, 1000);

		// the negative arms: the bottom (the ENERGY face) and the front refuse the fill
		assertNull(tMachine.getFluidTankFillable((byte)Direction.DOWN.get3DDataValue(), tWater), "the bottom face is the energy face, never a tank-in face");
		assertNull(tMachine.getFluidTankFillable((byte)Direction.NORTH.get3DDataValue(), tWater), "the relative front is outside SBIT_B|SBIT_L");
		// the positive arms: relative left = east, relative back = south
		assertSame(tMachine.mTanksInput[0], tMachine.getFluidTankFillable((byte)Direction.EAST.get3DDataValue(), tWater), "relative left accepts");
		assertSame(tMachine.mTanksInput[0], tMachine.getFluidTankFillable((byte)Direction.SOUTH.get3DDataValue(), tWater), "relative back accepts");
		// the filled input tank is the containing tank for a second fill
		tMachine.mTanksInput[0].fill(tWater, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		assertSame(tMachine.mTanksInput[0], tMachine.getFluidTankFillable((byte)Direction.EAST.get3DDataValue(), tWater), "the containing tank answers first (:567)");

		// the drain half: the top face is in the OUT mask but the OUTPUT tanks are empty and
		// the INPUT tank is not drainable (the upstream fill-only input semantics)
		assertNull(tMachine.getFluidTankDrainable((byte)Direction.UP.get3DDataValue(), null), "an empty output bank drains nothing through the open top");
		assertNull(tMachine.getFluidTankDrainable((byte)Direction.UP.get3DDataValue(), tWater), "the input-tank water is unreachable through the drain face");
		assertNull(tMachine.getFluidTankDrainable((byte)Direction.DOWN.get3DDataValue(), tWater), "the bottom refuses the drain mask too");
		assertEquals(1000, tMachine.mTanksInput[0].amount(), "the water stays in the input tank");
	}

	// ------------------------------------------------------------------
	// the declared-empty DRYING map does not start (the W1b declaration state)
	// ------------------------------------------------------------------

	@Test
	void dryerEmptyMapDoesNotStartEvenFedAndEnergised() {
		GTBasicMachineBlock.MachineRow tRow = GTMachines.DRYER_ROWS.get(3); // the T4 shape, the biggest row
		TileEntityBasicMachine tMachine = dryerFixture(tRow);
		// the RCON chain shape: water into the input tank (through the live fill gate) + HU in
		tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		tMachine.getInventory().setStackInSlot(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.BRICKS, 1)); // the stub feed
		// 40 packets at the 32-size (any size within the tier band injects; a mInputMax-size
		// packet would saturate at one — the :503 min(mInputMax - mEnergy) band)
		long tUsed = tMachine.doInject(TD.Energy.HU, (byte)2, 32, 40, true);
		assertEquals(40, tUsed, "the HU packets are accepted (the type + tier band admit them)");

		assertTrue(tMachine.mTanksInput[0].has(), "the water sits in the input tank");
		assertFalse(tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).isEmpty(), "the stub feed sits in the input slot");
		assertEquals(TileEntityBasicMachine.DID_NOT_FIND_RECIPE, tMachine.checkRecipe(true, true), "the declared-empty DRYING map finds nothing");
		drive(tMachine, 20);
		assertEquals(0, tMachine.mMaxProgress, "no recipe — no process is ever started");
		assertEquals(0, tMachine.mProgress, "no recipe — the progress stays at zero");
		assertFalse(tMachine.mActive, "no recipe — the machine never goes active");
		assertEquals(0, tMachine.mOutputItems.length, "no pending item outputs");
		assertEquals(0, tMachine.mOutputFluids.length, "no pending fluid outputs");
		assertTrue(tMachine.mTanksOutput[0].isEmpty() && tMachine.mTanksOutput[1].isEmpty() && tMachine.mTanksOutput[2].isEmpty(),
				"the output bank stays empty (产出零)");
	}
}
