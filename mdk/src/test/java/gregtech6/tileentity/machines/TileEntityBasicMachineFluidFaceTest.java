package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.util.GTSideTables;

/**
 * Task p14-machine-fluid-face — the offline acceptance fixture for the machine fluid tank
 * face + the :511 HU receiving gate. Four sections:
 *
 * <ol>
 * <li>the :511 truth table in the HuEnergyHandshakeTest SIDES_TOP shape: a
 *     bottom-face-only {@code mEnergyInputs} accepts ONLY the world bottom for every
 *     horizontal facing (FACING_ROTATIONS keeps bottom→bottom), the non-bottom faces are
 *     the refusal negative control, the stopped-machine arm closes even the open face for
 *     the real probe, and the default 127 mask accepts everywhere (the pre-p14 behaviour,
 *     zero regression);</li>
 * <li>the fluid round-trip: the per-side FLUID_HANDLER fills the input tank through the
 *     rotated mFluidInputs mask, checkRecipe consumes the recipe fluid from the REAL tank
 *     (the snapshot mirror), doActive :817-835 lands the output into mTanksOutput, and the
 *     capability drains it back out;</li>
 * <li>the drain/side-less rules: the mFluidOutputs mask, the side-less all-open drain /
 *     refused fill (the P5 barrel ruling), and the side-blind tank view (the
 *     p13-boiler-tank pipe-canConnect lesson);</li>
 * <li>the blocked-output parking (:837 containsSomething over BOTH pendings) and the tank
 *     NBT round-trip.</li>
 * </ol>
 *
 * <p>The fluid-bearing map is test-local (the SHCL maps the existing suite drives carry
 * mInputFluidCount 0 — the dormant face that must stay bit-for-bit) and poured ONCE: the
 * per-test {@code GT6RecipeMaps.reset()} clears the RECIPE_MAPS directory but never the
 * map object the machine is constructed with.
 */
public class TileEntityBasicMachineFluidFaceTest extends TileEntityBasicMachineOfflineTestBase {

	/** The vanilla bottom face — GT6 side byte 0 == Direction.DOWN (get3DDataValue). */
	static final byte SIDE_BOTTOM = 0;

	/** The machine-relative bottom bit (SBIT_D): FACE_CONNECTED[0][mask] == (mask & 1) != 0. */
	static final byte SBIT_D = 1;

	/** The water input amount (CS-recipe scale: the Dryer 10 L row shape, Loader_Recipes_Chem.java:525). */
	static final int WATER_IN = 10;
	/** The fluid output amount (8 L — the 10→8 drying ratio). */
	static final int FLUID_OUT = 8;

	/** The test-local fluid-bearing map (unique RECIPE_MAPS name; object survives the per-test reset()). */
	static RecipeMap sFluidMap;

	@BeforeAll
	static void buildFluidMap() {
		if (sFluidMap != null) return;
		// the RecipeMap 15-param form (RecipeMap.java:73): 1 item-in slot (the findRecipe
		// non-empty aInputs contract, RecipeMap.java:138), 1 in/1 out fluid, both minimal 1
		sFluidMap = new RecipeMap(new HashSet<>(),
				"gt.recipe.test.p14fluidface", "P14 Fluid Face", null,
				0, 1,
				"gt6:textures/gui/machines/shredder",
				/*IN-OUT-MIN-ITEM=*/ 1, 0, 0,
				/*IN-OUT-MIN-FLUID=*/ 1, 1, 1,
				/*MIN=*/ 1,
				/*AMP=*/ 1);
		// the single row: 1 gravel + 10 L water → 8 L milk (vanilla identities, offline-safe)
		sFluidMap.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.GRAVEL, 1)}, null,
				new FluidStack[] {new FluidStack(Fluids.WATER, WATER_IN)},
				new FluidStack[] {new FluidStack(Fluids.LAVA, FLUID_OUT)},
				16, 16, 0));
	}

	/** A machine carrying the fluid map (the fluid tank arrays sized 1+1 by the map shape). */
	static TileEntityBasicMachine makeFluidMachine() {
		return makeMachine(sFluidMap, 1, false);
	}

	private static IFluidHandler fluidHandler(TileEntityBasicMachine aMachine, Direction aSide) {
		// the wrapper directly — the ForgeCapabilities tokens are transformer-resolved and
		// unresolvable offline (the GT6MultiBlockFluidTest direct-construction precedent)
		return aMachine.newFluidHandler(aSide);
	}

	// ---------------------------------------------------------------------------
	// 1. the :511 HU receiving gate (the HuEnergyHandshakeTest SIDES_TOP shape)
	// ---------------------------------------------------------------------------

	@Test
	public void huBottomFaceTruthTableOverEveryFacing() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.mEnergyTypeAccepted = TD.Energy.HU;
		tMachine.mEnergyInputs = SBIT_D; // the relative-bottom-only mask (the upstream SBIT_D row form)
		tMachine.mStopped = false;

		// the rotation invariance: FACING_ROTATIONS maps bottom→bottom for EVERY horizontal
		// facing, so only the world bottom face (byte 0) carries the gate — both probes
		for (byte tFacing = 2; tFacing <= 5; tFacing++) {
			tMachine.mFacing = tFacing;
			for (byte tSide = 0; tSide < 6; tSide++) {
				assertEquals(tSide == SIDE_BOTTOM, tMachine.isEnergyAcceptingFrom(TD.Energy.HU, tSide, true),
						"theoretical probe, facing " + tFacing + " side " + tSide
								+ (tSide == SIDE_BOTTOM ? " — the bottom face alone stays connected" : " — a non-bottom face is dead"));
				assertEquals(tSide == SIDE_BOTTOM, tMachine.isEnergyAcceptingFrom(TD.Energy.HU, tSide, false),
						"real probe (running), facing " + tFacing + " side " + tSide);
			}
		}
	}

	@Test
	public void huNonBottomFacesRefuseAndTheTypeGateHolds() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.mEnergyTypeAccepted = TD.Energy.HU;
		tMachine.mEnergyInputs = SBIT_D;

		// the refusal negative control, every non-bottom face, both probes
		for (byte tSide = 1; tSide < 6; tSide++) {
			assertFalse(tMachine.isEnergyAcceptingFrom(TD.Energy.HU, tSide, true), "theoretical refusal, side " + tSide);
			assertFalse(tMachine.isEnergyAcceptingFrom(TD.Energy.HU, tSide, false), "real refusal, side " + tSide);
		}
		// the foreign-type lock holds on the OPEN face (the super isEnergyType arm, :596)
		assertFalse(tMachine.isEnergyAcceptingFrom(TD.Energy.RU, SIDE_BOTTOM, true), "RU is locked out of the HU face");
		assertFalse(tMachine.isEnergyAcceptingFrom(TD.Energy.EU, SIDE_BOTTOM, true), "EU is locked out of the HU face");
		// the real probe books through doInject on the open face only (the :489-508 body;
		// the :511 gate is the conductor-facing probe — upstream doInject checks no side)
		assertEquals(5, tMachine.doInject(TD.Energy.HU, SIDE_BOTTOM, 1, 5, true), "the open face books the packet");
		assertEquals(5, tMachine.mEnergy, "the booked energy");
	}

	@Test
	public void stoppedMachineClosesEvenTheOpenFaceForTheRealProbe() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.mEnergyTypeAccepted = TD.Energy.HU;
		tMachine.mEnergyInputs = SBIT_D;
		tMachine.mStopped = true; // the :511 (aTheoretical || !mStopped) arm

		assertTrue(tMachine.isEnergyAcceptingFrom(TD.Energy.HU, SIDE_BOTTOM, true), "the theoretical probe stays (a conductor keeps visual contact)");
		assertFalse(tMachine.isEnergyAcceptingFrom(TD.Energy.HU, SIDE_BOTTOM, false), "the real probe closes with the machine stopped");
	}

	@Test
	public void defaultMaskAcceptsEverywhere() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.mEnergyTypeAccepted = TD.Energy.HU;
		assertEquals(127, tMachine.mEnergyInputs, "the :93 field default");

		// zero regression: 127 = every relative side connected = the pre-p14 all-sides
		// Root behaviour, bit-for-bit, for BOTH probes and every facing
		for (byte tFacing = 2; tFacing <= 5; tFacing++) {
			tMachine.mFacing = tFacing;
			for (byte tSide = 0; tSide < 6; tSide++) {
				assertTrue(tMachine.isEnergyAcceptingFrom(TD.Energy.HU, tSide, true), "default mask theoretical, facing " + tFacing + " side " + tSide);
				assertTrue(tMachine.isEnergyAcceptingFrom(TD.Energy.HU, tSide, false), "default mask real, facing " + tFacing + " side " + tSide);
			}
		}
		// the FACE_CONNECTED bit identity the table literal encodes (CS.java:598 javadoc)
		for (byte tSide = 0; tSide < 7; tSide++) {
			assertEquals((127 & (1 << tSide)) != 0, GTSideTables.FACE_CONNECTED[tSide][127], "the bit identity at relative side " + tSide);
		}
	}

	// ---------------------------------------------------------------------------
	// 2. the fluid round-trip: capability fill → checkRecipe consume → doActive place → drain
	// ---------------------------------------------------------------------------

	@Test
	public void fluidRoundTripThroughTheRealTanks() {
		TileEntityBasicMachine tMachine = makeFluidMachine();

		// the :157-162 construction: 1 input tank at the 1000 default, 1 output tank default
		assertEquals(1, tMachine.mTanksInput.length);
		assertEquals(1000, tMachine.mTanksInput[0].getCapacity(), "the :157/:160 input-tank default capacity");
		assertEquals(1, tMachine.mTanksOutput.length);
		assertEquals(2, fluidHandler(tMachine, Direction.DOWN).getTanks(), "the side-blind tank view spans input + output");

		// capability fill through the default 127 input mask
		IFluidHandler tHandler = fluidHandler(tMachine, Direction.DOWN);
		assertEquals(1000, tHandler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE), "the input tank takes the full liter load");

		// the item catalyst + the fake-source fixture drive: EUt 16 × duration 16 = 256
		// progress = 4 ticks at 64/tick
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);
		drive(tMachine, 4);

		assertEquals(1000 - WATER_IN, tMachine.mTanksInput[0].amount(), "checkRecipe consumed exactly one recipe-worth from the REAL input tank");
		assertEquals(Fluids.WATER, tMachine.mTanksInput[0].fluid().getRawFluid(), "the input identity is untouched");
		assertEquals(FLUID_OUT, tMachine.mTanksOutput[0].amount(), "doActive :817-835 landed the output into the output tank");
		assertEquals(Fluids.LAVA, tMachine.mTanksOutput[0].fluid().getRawFluid(), "the output identity");
		assertTrue(tMachine.mSuccessful, "the process reported success");
		assertEquals(0, tMachine.mOutputFluids.length, "the placed pending is cleared (:849)");

		// drain the product back out through the capability
		FluidStack tDrained = tHandler.drain(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
		assertNotNull(tDrained);
		assertEquals(FLUID_OUT, tDrained.getAmount(), "the output tank gives exactly its content");
		assertTrue(tMachine.mTanksOutput[0].isEmpty(), "the output tank is empty after the draw");
	}

	// ---------------------------------------------------------------------------
	// 3. the fill/drain side rules
	// ---------------------------------------------------------------------------

	@Test
	public void fillMaskRotatesAndRefusesTheMaskedOutFaces() {
		TileEntityBasicMachine tMachine = makeFluidMachine();
		tMachine.mFluidInputs = SBIT_D; // bottom-relative only

		IFluidHandler tFromTop = fluidHandler(tMachine, Direction.UP);
		IFluidHandler tFromBottom = fluidHandler(tMachine, Direction.DOWN);
		assertNotNull(tFromTop);
		assertNotNull(tFromBottom);
		assertEquals(0, tFromTop.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE), "the top face is mask-refused");
		assertTrue(tMachine.mTanksInput[0].isEmpty(), "a refused fill books nothing");
		assertEquals(500, tFromBottom.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE), "the bottom face fills");

		// the side-less fill probe is refused (the P5 barrel ruling), the drain one all-open
		IFluidHandler tSideLess = fluidHandler(tMachine, null);
		assertNotNull(tSideLess);
		assertEquals(0, tSideLess.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE), "the side-less fill is refused");
		assertTrue(tSideLess.drain(10, FluidAction.EXECUTE).isEmpty(), "the side-less DRAIN is all-open but the OUTPUT tank is empty (nothing to give)");
	}

	@Test
	public void drainMaskAndSimulateForms() {
		TileEntityBasicMachine tMachine = makeFluidMachine();
		tMachine.mTanksOutput[0].fill(new FluidStack(Fluids.LAVA, 50), FluidAction.EXECUTE);

		// simulate books nothing
		FluidStack tProbe = fluidHandler(tMachine, Direction.UP).drain(new FluidStack(Fluids.LAVA, 10), FluidAction.SIMULATE);
		assertEquals(10, tProbe.getAmount(), "the simulate probe answers");
		assertEquals(50, tMachine.mTanksOutput[0].amount(), "the simulate books nothing");

		// the output mask rotates: top-bit-only (SBIT_U = bit 1) lets the top drain, kills the bottom
		tMachine.mFluidOutputs = 2;
		assertEquals(10, fluidHandler(tMachine, Direction.UP).drain(10, FluidAction.EXECUTE).getAmount(), "the masked-in top drains");
		assertEquals(0, fluidHandler(tMachine, Direction.DOWN).drain(10, FluidAction.EXECUTE).getAmount(), "the masked-out bottom refuses");
		assertTrue(fluidHandler(tMachine, Direction.DOWN).drain(10, FluidAction.EXECUTE).isEmpty(), "the refusal is an EMPTY stack");

		// the side-less drain reads all-open (P5) — the remaining 40 L leave
		assertEquals(40, fluidHandler(tMachine, null).drain(100, FluidAction.EXECUTE).getAmount(), "the side-less drain takes the rest");
		assertTrue(tMachine.mTanksOutput[0].isEmpty());
	}

	@Test
	public void tankViewIsSideBlindForThePipeHandshake() {
		TileEntityBasicMachine tMachine = makeFluidMachine();
		tMachine.mFluidInputs = 0;
		tMachine.mFluidOutputs = 0; // a FULLY masked machine

		// every face still reports the tanks (the p13-boiler-tank lesson: the pipe canConnect
		// handshake probes handler.getTanks() > 0 — a masked-to-zero view would dead-end it)
		for (Direction tSide : Direction.values()) {
			IFluidHandler tHandler = fluidHandler(tMachine, tSide);
			assertEquals(2, tHandler.getTanks(), "the tank view stays whole on side " + tSide);
			assertEquals(1000, tHandler.getTankCapacity(0), "the input capacity is visible on side " + tSide);
			assertTrue(tHandler.getFluidInTank(0).isEmpty(), "the empty view is EMPTY");
		}
		assertEquals(0, fluidHandler(tMachine, Direction.DOWN).fill(new FluidStack(Fluids.WATER, 5), FluidAction.EXECUTE), "mask 0 refuses every fill");
		assertTrue(fluidHandler(tMachine, Direction.DOWN).drain(5, FluidAction.EXECUTE).isEmpty(), "mask 0 refuses every drain");
	}

	// ---------------------------------------------------------------------------
	// 4. the blocked-output parking + the NBT round-trip
	// ---------------------------------------------------------------------------

	@Test
	public void blockedFluidOutputsParkAtMaxProgress() {
		TileEntityBasicMachine tMachine = makeFluidMachine();
		// a foreign fluid occupies the ONLY output tank: no containing and no empty tank
		tMachine.mTanksOutput[0].fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
		tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);

		drive(tMachine, 4); // the process completes, the LAVA product cannot land

		assertEquals(100 - WATER_IN, tMachine.mTanksInput[0].amount(), "the input was consumed");
		assertNotEquals(Fluids.LAVA, tMachine.mTanksOutput[0].fluid() == null ? null : tMachine.mTanksOutput[0].fluid().getRawFluid(), "the output tank keeps its occupant");
		assertEquals(500, tMachine.mTanksOutput[0].amount(), "the occupant amount is untouched");
		assertEquals(1, tMachine.mOutputFluids.length, "the pending output stays pending");
		assertEquals(tMachine.mMaxProgress, tMachine.mProgress, "the :837 parking arm holds progress at max");

		// freeing the tank lets the parked output land on the next active tick
		assertEquals(500, fluidHandler(tMachine, Direction.UP).drain(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE).getAmount(), "the occupant drains out");
		drive(tMachine, 1);
		assertEquals(FLUID_OUT, tMachine.mTanksOutput[0].amount(), "the pending LAVA lands once the tank frees");
		assertEquals(Fluids.LAVA, tMachine.mTanksOutput[0].fluid().getRawFluid());
	}

	@Test
	public void nbtRoundTripPersistsTankContentAndPendingFluids() {
		TileEntityBasicMachine tMachine = makeFluidMachine();
		tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 990), FluidAction.EXECUTE);
		tMachine.mTanksOutput[0].fill(new FluidStack(Fluids.LAVA, 8), FluidAction.EXECUTE);
		tMachine.mOutputFluids = new FluidStack[] {new FluidStack(Fluids.LAVA, 16)};

		CompoundTag tTag = new CompoundTag();
		tMachine.saveAdditional(tTag);
		assertTrue(tTag.contains(NBT_KEY_INPUT_TANK), "the input tank key (:160 form)");
		assertTrue(tTag.contains(NBT_KEY_OUTPUT_TANK), "the output tank key (:162 form)");
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_OUTPUT_FLUIDS), "the pending-fluid list key");

		TileEntityBasicMachine tRestored = makeFluidMachine();
		tRestored.load(tTag);
		assertEquals(990, tRestored.mTanksInput[0].amount(), "the input content survives");
		assertEquals(Fluids.WATER, tRestored.mTanksInput[0].fluid().getRawFluid());
		assertEquals(1000, tRestored.mTanksInput[0].getCapacity(), "the capacity is the constructor ruling, never NBT-driven");
		assertEquals(8, tRestored.mTanksOutput[0].amount(), "the output content survives");
		assertEquals(Fluids.LAVA, tRestored.mTanksOutput[0].fluid().getRawFluid());
		assertEquals(1, tRestored.mOutputFluids.length, "the pending fluids survive");
		assertEquals(16, tRestored.mOutputFluids[0].getAmount());

		// an empty tank writes no key and loads empty
		TileEntityBasicMachine tEmpty = makeFluidMachine();
		CompoundTag tEmptyTag = new CompoundTag();
		tEmpty.saveAdditional(tEmptyTag);
		assertFalse(tEmptyTag.contains(NBT_KEY_INPUT_TANK), "an empty tank writes no key (FluidTankGT.writeToNBT removes)");
		tEmpty.load(tEmptyTag);
		assertTrue(tEmpty.mTanksInput[0].isEmpty());
	}

	static final String NBT_KEY_INPUT_TANK = TileEntityBasicMachine.NBT_TANK + ".in.0";
	static final String NBT_KEY_OUTPUT_TANK = TileEntityBasicMachine.NBT_TANK + ".out.0";
}
