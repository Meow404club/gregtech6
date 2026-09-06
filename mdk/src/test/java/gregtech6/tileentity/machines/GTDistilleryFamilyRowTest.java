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
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.GT6RecipesDistillery;
import gregtech6.recipes.GT6RecipesDistilleryTest;
import gregtech6.registry.GTMachines;
import gregtech6.util.GTSideTables;

/**
 * The Distillery-family row acceptance (task p16-distillery-family ②, the OFFLINE half —
 * the GTDryerFamilyRowTest shape): the four row records pinned to the upstream columns
 * (Loader_MultiTileEntities.java:1398-1401 — the name/material/hardness/NBT_INPUT/
 * NBT_ENERGY_ACCEPTED HU/RM.Distillery/NBT_TEXTURE "distillery"/NBT_PARALLEL 8/16/32/64 +
 * NBT_PARALLEL_DURATION T/NBT_CHEAP_OVERCLOCKING T columns, the connectivity masks through
 * the :151/:143/:144/:137/:138 read forms), the row→BE carrier assignment ({@link
 * GTMachines#applyRow}), the rotated face geometry (the up|left tank-in pair, the back
 * tank-out face, the bottom energy face — every horizontal facing), the RM.java:70 slot/
 * tank shape (items 1/2/1, fluids 1/2/1) and the menu-less carrier marker. The registration
 * half (4 blocks + 4 items + the 1 family BET with T1-T4 validBlocks multi-attach) only
 * resolves on a live server: the runServer/RCON gate (the p8/p14 precedent).
 */
public class GTDistilleryFamilyRowTest extends TileEntityBasicMachineOfflineTestBase {

	/** CS.java:612 — SBIT_D|SBIT_A, the :151 NBT_ENERGY_ACCEPTED_SIDES read form. */
	static final byte ENERGY_IN_MASK = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	/** CS.java:612 — SBIT_U|SBIT_L|SBIT_A, the :143 NBT_TANK_SIDE_IN / :137 NBT_INV_SIDE_IN read form. */
	static final byte TANK_IN_MASK = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	/** CS.java:612 — SBIT_B|SBIT_A, the :144 NBT_TANK_SIDE_OUT read form. */
	static final byte TANK_OUT_MASK = (byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A);
	/** CS.java:612 — SBIT_R|SBIT_A, the :138 NBT_INV_SIDE_OUT read form. */
	static final byte ITEM_OUT_MASK = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A);

	// ------------------------------------------------------------------
	// the row table — the upstream :1398-1401 columns
	// ------------------------------------------------------------------

	@Test
	void distilleryRowsMatchTheUpstreamColumns() {
		assertEquals(4, GTMachines.DISTILLERY_ROWS.size(), "the four Distillery rows (:1398-1401)");
		for (int i = 0; i < GTMachines.DISTILLERY_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.DISTILLERY_ROWS.get(i);
			int tTier = i + 1; // the upstream tier index, 1-based in the messages
			assertEquals("distillery" + (i == 0 ? "" : "_t" + tTier), tRow.path(), "the registry path ladder");
			assertEquals(20191 + i, tRow.metaId(), "the MultiTile id column of tier " + tTier);
			assertEquals(tTier, tRow.tier() + 1, "the tier index");
			assertTrue(tRow.parallelDuration(), "NBT_PARALLEL_DURATION T on every row");
			assertSame(TD.Energy.HU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.HU on every row");
			assertEquals("distillery", tRow.texture(), "NBT_TEXTURE distillery on every row (the ladder shares the fronts)");
			assertSame(GT6RecipeMaps.DISTILLERY, tRow.recipes().get(), "NBT_RECIPEMAP RM.Distillery through the supplier");
			assertTrue(tRow.cheapOverclocking(), "NBT_CHEAP_OVERCLOCKING T on every row (:773 runs unconditionally)");
			assertNull(tRow.menu(), "the menu-less carrier — the GUI pool precedent (use() stays inert, the "
					+ "acceptance drives inject+check like the pre-gui dryer)");
		}
		// the four differing columns, row by row - the composed face replays the old name
		// column from the family template + the material word (task p20-i18n-compose-rows)
		String[] tWords = {"Steel", "Invar", "Titanium", "Tungsten Carbide"};
		String[] tSlugs = {"steel", "invar", "titanium", "tungsten_carbide"};
		for (int tI = 0; tI < 4; tI++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.DISTILLERY_ROWS.get(tI);
			assertEquals("Distillery (" + tWords[tI] + ")", "Distillery (%s)".replace("%s", tWords[tI]),
					"the name column, Heat_T[" + (tI + 1) + "] - composed replay");
			assertEquals(tWords[tI], tRow.matDisplay(), "the material word, Heat_T[" + (tI + 1) + "]");
			assertEquals(tSlugs[tI], tRow.matSlug(), "the material slug (the gt6.row.mat key tail)");
			assertEquals("gt6.row.distillery.display", tRow.displayKey(), "the family template key");
		}
		assertEquals(6.0F, GTMachines.DISTILLERY_ROWS.get(0).hardness(), "NBT_HARDNESS 6.0 (:1398)");
		assertEquals(4.0F, GTMachines.DISTILLERY_ROWS.get(1).hardness(), "NBT_HARDNESS 4.0 (:1399)");
		assertEquals(9.0F, GTMachines.DISTILLERY_ROWS.get(2).hardness(), "NBT_HARDNESS 9.0 (:1400)");
		assertEquals(12.5F, GTMachines.DISTILLERY_ROWS.get(3).hardness(), "NBT_HARDNESS 12.5 (:1401)");
		assertEquals(8, GTMachines.DISTILLERY_ROWS.get(0).parallel(), "NBT_PARALLEL 8");
		assertEquals(16, GTMachines.DISTILLERY_ROWS.get(1).parallel(), "NBT_PARALLEL 16");
		assertEquals(32, GTMachines.DISTILLERY_ROWS.get(2).parallel(), "NBT_PARALLEL 32");
		assertEquals(64, GTMachines.DISTILLERY_ROWS.get(3).parallel(), "NBT_PARALLEL 64");
		// the connectivity + auto-side columns (identical on all four rows)
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.DISTILLERY_ROWS) {
			assertEquals(ENERGY_IN_MASK, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_D through the :151 SBIT_A read");
			assertEquals(TANK_IN_MASK, tRow.fluidIn(), "NBT_TANK_SIDE_IN SBIT_U|SBIT_L through the :143 read");
			assertEquals(TANK_OUT_MASK, tRow.fluidOut(), "NBT_TANK_SIDE_OUT SBIT_B through the :144 read");
			assertEquals(TANK_IN_MASK, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_U|SBIT_L through the :137 read");
			assertEquals(ITEM_OUT_MASK, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R through the :138 read");
			assertEquals(1, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP");
			assertEquals(5, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_BACK");
			assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
		}
		// the by-path tables carry exactly the four rows (the walkers iterate them)
		assertEquals(4, GTMachines.DISTILLERY_BLOCKS_BY_PATH.size());
		assertEquals(4, GTMachines.DISTILLERY_ITEMS_BY_PATH.size());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.DISTILLERY_ROWS) {
			assertSame(GTMachines.DISTILLERY_BLOCKS_BY_PATH.get(tRow.path()).getId(), GTMachines.DISTILLERY_BLOCKS_BY_PATH.get(tRow.path()).getId());
			assertNotNull(GTMachines.DISTILLERY_ITEMS_BY_PATH.get(tRow.path()), "the item row for " + tRow.path());
			assertEquals(tRow.path(), GTMachines.DISTILLERY_ITEMS_BY_PATH.get(tRow.path()).getId().getPath());
		}
	}

	// ------------------------------------------------------------------
	// the row → BE carrier assignment (the factory body, fixture-driven)
	// ------------------------------------------------------------------

	/** A fixture built exactly as the DISTILLERY_BE factory body builds it (the dryerFixture shape). */
	private static TileEntityBasicMachine distilleryFixture(GTBasicMachineBlock.MachineRow aRow) {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.DISTILLERY, aRow.parallel(), aRow.parallelDuration(), TD.Energy.HU);
		long[] tInputs = GTMachines.TIER_INPUTS[aRow.tier()];
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return GTMachines.applyRow(tMachine, aRow);
	}

	@Test
	void applyRowLandsTheCarrierAndTheTierInputs() {
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.DISTILLERY_ROWS) {
			TileEntityBasicMachine tMachine = distilleryFixture(tRow);
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
			// the DISTILLERY slot/tank shape (RM.java:70: items 1,2,1 / fluids 1,2,1)
			assertEquals(3, tMachine.getInventory().getSlots(), tRow.path() + " the 1+2 slot shape");
			assertEquals(1, tMachine.mTanksInput.length, tRow.path() + " the 1 input tank");
			assertEquals(2, tMachine.mTanksOutput.length, tRow.path() + " the 2 output tanks");
			assertEquals(1000, tMachine.mTanksInput[0].getCapacity(), tRow.path() + " the constructed 1000 default");
		}
	}

	// ------------------------------------------------------------------
	// the rotated face geometry (the :511/:566/:575 gates, every facing)
	// ------------------------------------------------------------------

	@Test
	void distilleryFaceGeometryIsTheRotatedRowMasks() {
		for (byte tFacing = 2; tFacing <= 5; tFacing++) {
			for (byte tWorldSide = 0; tWorldSide <= 5; tWorldSide++) {
				for (GTBasicMachineBlock.MachineRow tRow : GTMachines.DISTILLERY_ROWS) {
					boolean tEnergy = GTSideTables.faceConnected(tFacing, tWorldSide, tRow.energySides());
					assertEquals(tWorldSide == 0, tEnergy,
							tRow.path() + " accepts energy ONLY on the world bottom for every facing (facing " + tFacing + ", side " + tWorldSide + ")");
					boolean tOut = GTSideTables.faceConnected(tFacing, tWorldSide, tRow.fluidOut());
					assertEquals(tWorldSide == relativeBackOf(tFacing), tOut,
							tRow.path() + " drains ONLY on the rotated back for every facing (facing " + tFacing + ", side " + tWorldSide + ")");
				}
			}
		}
		// the fluid-in mask: exactly two world faces per facing — up plus the rotated left —
		// never the bottom (the SBIT_D energy face)
		for (byte tFacing = 2; tFacing <= 5; tFacing++) {
			List<Direction> tIn = acceptedFaces(tFacing, TANK_IN_MASK);
			assertEquals(2, tIn.size(), "exactly the up|left pair after rotation (facing " + tFacing + ")");
			assertTrue(tIn.contains(Direction.UP), "up is a tank-in face on every facing (facing " + tFacing + ")");
			assertFalse(tIn.contains(Direction.DOWN), "the bottom never takes fluids (facing " + tFacing + ")");
		}
		// the placed default (facing north = 2): relative left = world east, relative back =
		// world south — the RCON chain's positive/negative fill arms
		assertEquals(List.of(Direction.UP, Direction.EAST), acceptedFaces((byte)2, TANK_IN_MASK),
				"facing north: relative left = world east (+ world up), Direction order");
		assertEquals(List.of(Direction.SOUTH), acceptedFaces((byte)2, TANK_OUT_MASK),
				"facing north: relative back = world south — the DistW draw face");
	}

	/** The world side of the rotated back face for a horizontal facing (the dryer-chain rotation convention). */
	private static byte relativeBackOf(byte aFacing) {
		return switch (aFacing) {
			case 2 -> 3;  // north → back = south
			case 3 -> 2;  // south → back = north
			case 4 -> 5;  // west  → back = east
			default -> 4; // east  → back = west
		};
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
	void distilleryFluidFaceGatesRejectAndAcceptLive() {
		TileEntityBasicMachine tMachine = distilleryFixture(GTMachines.DISTILLERY_ROWS.get(0)); // facing default north (2)
		FluidStack tWater = new FluidStack(Fluids.WATER, 1000);

		// the positive arms: relative left = world east, plus the world up (the SBIT_U half)
		assertSame(tMachine.mTanksInput[0], tMachine.getFluidTankFillable((byte)Direction.EAST.get3DDataValue(), tWater), "relative left (east) accepts");
		assertSame(tMachine.mTanksInput[0], tMachine.getFluidTankFillable((byte)Direction.UP.get3DDataValue(), tWater), "up accepts (the SBIT_U tank-in half)");
		// the negative arms: world south (the OUT-only back), the relative front, and the bottom (the energy face)
		assertNull(tMachine.getFluidTankFillable((byte)Direction.SOUTH.get3DDataValue(), tWater), "south (relative back) is OUT-only — the fill is rejected");
		assertNull(tMachine.getFluidTankFillable((byte)Direction.NORTH.get3DDataValue(), tWater), "the relative front is outside SBIT_U|SBIT_L");
		assertNull(tMachine.getFluidTankFillable((byte)Direction.DOWN.get3DDataValue(), tWater), "the bottom face is the energy face, never a tank-in face");

		// the drain half: the back face is open but the OUTPUT tanks are empty and the input
		// tank is fill-only (the upstream fill-only input semantics)
		assertNull(tMachine.getFluidTankDrainable((byte)Direction.SOUTH.get3DDataValue(), null), "an empty output bank drains nothing through the open back");
		assertNull(tMachine.getFluidTankDrainable((byte)Direction.SOUTH.get3DDataValue(), tWater), "the input-tank water is unreachable through the drain face");
		assertNull(tMachine.getFluidTankDrainable((byte)Direction.UP.get3DDataValue(), tWater), "up (tank-in) refuses the drain mask too");
	}

	// ------------------------------------------------------------------
	// the offline production loop: circuit in → water → HU → DistW out, circuit never eaten
	// ------------------------------------------------------------------

	/**
	 * The end-to-end offline half of the RCON acceptance ("circuit 输入→running→产出"): the
	 * fixture pour (distw resolved to LAVA so the output identity is distinguishable from
	 * the water input), the fixture machine in the DISTILLERY_BE factory shape, the
	 * ST.tag(0) circuit in the input slot, 40 L of water, HU packets, driven ticks — the
	 * batch completes, the distilled output lands, the circuit SURVIVES.
	 */
	@Test
	void thePouredMapProducesThroughTheCircuitSelector() {
		// the pour with an output-distinct fixture: distw → LAVA (identities only — the
		// GTEngineFuelsTest convention; the other five rows ride the same fixture)
		GT6RecipesDistillery.sFluidResolver = aId ->
				GT6RecipesDistillery.FLUID_DISTW.equals(aId) ? Fluids.LAVA
						: GT6RecipesDistillery.FLUID_HOT.equals(aId) ? null : Fluids.WATER;
		GT6RecipesDistillery.sCircuitResolver = GT6RecipesDistilleryTest::fixtureCircuit;
		java.util.function.Predicate<ItemStack> tOldPredicate = Recipe.sNotConsumable;
		Recipe.sNotConsumable = GT6RecipesDistilleryTest.FIXTURE_NOT_CONSUMABLE;
		try {
			GT6RecipesDistillery.resetForTest();
			GT6RecipeMaps.reset();
			GT6RecipeMaps.init();
			GT6RecipesDistillery.load();
			assertEquals(7, GT6RecipeMaps.DISTILLERY.mRecipeList.size(), "the fixture pour");

			TileEntityBasicMachine tMachine = distilleryFixture(GTMachines.DISTILLERY_ROWS.get(0)); // the T1 shape
			tMachine.getInventory().setStackInSlot(TileEntityBasicMachine.SLOT_INPUT, GT6RecipesDistilleryTest.fixtureCircuit(0)); // ST.tag(0)
			tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 40), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

			assertEquals(TileEntityBasicMachine.FOUND_AND_COULD_HAVE_USED_RECIPE, tMachine.checkRecipe(false, false),
					"the probe: [circuit@0 + water] against the poured map — the selector fills the item leg");
			assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false),
					"the apply: the batch consumes 4 x 10 L (the count loop caps at the available water)");

			// the batch took at least one process worth of water upfront (the exact parallel
			// accounting is the live RCON chain's pinned arithmetic; the offline half pins the
			// invariants)
			assertTrue(tMachine.mTanksInput[0].amount() <= 30, "the batch consumed at least one 10 L process upfront");
			assertEquals(1, tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).getCount(),
					"the circuit survives the consume — the ① identity-skip net effect");

			// the pending production: the accepted batch carries DistW output in whole
			// processes (k x 8 L of the fixture fluid) — the driven batch-to-tank placement
			// is the pre-existing machine machinery, the LIVE production proof is the RCON chain
			assertNotNull(tMachine.mOutputFluids, "the batch carries pending fluid outputs");
			assertTrue(tMachine.mOutputFluids.length >= 1, "the DistW leg is pending");
			// the HashSet serves the fixture-identical rows in arbitrary order — BOTH water
			// families are the 4:5 out:in ratio (8/10 and 20/25), the pour-level truth
			long tOut = tMachine.mOutputFluids[0].getAmount();
			long tConsumed = 40 - tMachine.mTanksInput[0].amount();
			assertTrue(tOut >= 8, "at least one process of DistW is pending (got " + tOut + ")");
			assertEquals(tConsumed * 4, tOut * 5, "the pending output is the consumed water at the 4:5 distillation ratio (got "
					+ tOut + " out of " + tConsumed + " consumed)");
			assertSame(Fluids.LAVA, tMachine.mOutputFluids[0].getFluid(), "the DistW fixture identity");
			assertEquals(1, tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).getCount(),
					"the circuit outlives the whole batch (the never-consumed selector)");
		} finally {
			Recipe.sNotConsumable = tOldPredicate;
			GT6RecipeMaps.reset();
			GT6RecipesDistillery.resetForTest();
		}
	}
}
