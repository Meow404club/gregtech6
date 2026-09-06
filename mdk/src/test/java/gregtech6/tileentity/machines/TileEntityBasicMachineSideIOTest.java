package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.Test;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * Task p16-machine-side-io — the machine side-IO face over {@link TileEntityBasicMachine}:
 *
 * <ul>
 * <li>① the per-side item ACCESS masks (upstream :94 + updateAccessibleSlots :533-545):
 *     the truth table — a side with both masks touches every slot, input-only the input
 *     range, output-only the output range, neither nothing; the direction gates
 *     (canInsertItem2/canExtractItem2) stay on top of the table; the 127/127 default keeps
 *     the pre-p16 all-sides behaviour bit-for-bit;</li>
 * <li>② the fluid auto-IO: the :696-705 PULL arm (the neighbor capability drained into the
 *     input tank through the :701 SIDE_ANY fillable) and the :459/:994-996 PUSH arm (the
 *     output tanks pushed through the auto-output face, fill-then-deduct) plus the :565
 *     push-only refuse leg of getFluidTankFillable;</li>
 * <li>③ NBT_TANK_CAPACITY (:157-158) + mCanUseOutputTanks (:716-732 — the output tank as a
 *     direct recipe-input source) + the hasKey-guarded NBT load family.</li>
 * </ul>
 *
 * <p>The neighbor adjacency runs through the {@code getFluidInputTarget}/
 * {@code getFluidOutputTarget} seams (the upstream :966-980 overridable targets), so the
 * offline tests override them instead of stubbing the world — the same seams the live
 * p16_side_io RCON chain exercises through real neighbors.
 */
public class TileEntityBasicMachineSideIOTest extends TileEntityBasicMachineOfflineTestBase {

	/** A counter for unique map names — the RecipeMap ctor registers into RECIPE_MAPS (RecipeMap.java:95), so a shared name collides across tests. */
	private static int sMapOrdinal = 0;

	/**
	 * A fresh 1-in/1-out fluid map (no recipes — the auto-IO arms are recipe-independent).
	 * One input ITEM slot: findRecipe refuses a zero-length input array (RecipeMap.java:138),
	 * so the checkRecipe flow needs a slot to hand it an EMPTY stack.
	 */
	private static RecipeMap fluidMap() {
		return new RecipeMap(new HashSet<>(),
				"gt.recipe.sideio" + (++sMapOrdinal), "Side IO Test", null,
				0, 1,
				"gt6:textures/gui/machines/shredder",
				/*IN-OUT-MIN-ITEM=*/ 1, 1, 0,
				/*IN-OUT-MIN-FLUID=*/ 1, 1, 0,
				/*MIN=*/ 0,
				/*AMP=*/ 1);
	}

	/** A fluid machine at POS with a level (the fixture makeMachine is bound to the shared maps). */
	private static TileEntityBasicMachine makeFluidMachine(RecipeMap aMap) {
		TileEntityBasicMachine tMachine = new TileEntityBasicMachine(
				machineType(aMap, 1, false), POS, Blocks.BRICKS.defaultBlockState(), aMap, 1, false, null);
		tMachine.setLevel(emptyLevel());
		return tMachine;
	}

	// ------------------------------------------------------------------
	// ① the per-side item ACCESS truth table
	// ------------------------------------------------------------------

	@Test
	void defaultMasksKeepTheAllSidesBehaviour() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		// the 127/127 default: every side reads the full slot list (:536 both masks → ACCESSIBLE_SLOTS)
		assertAscendingRow(tMachine.getAccessibleSlotsFromSide((byte)3), 0, 12);
		// insert into the input slot and extract from an output slot through ANY face
		assertTrue(tMachine.newItemHandler(Direction.SOUTH).insertItem(0, new ItemStack(Items.SAND, 4), true).isEmpty());
		tMachine.getInventory().setStackInSlot(1, new ItemStack(Items.SAND, 7));
		assertFalse(tMachine.newItemHandler(Direction.NORTH).extractItem(1, 4, true).isEmpty());
		assertFalse(tMachine.newItemHandler(Direction.DOWN).extractItem(1, 4, true).isEmpty());
	}

	@Test
	void perSideMasksGateInsertAndExtractByFace() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.mItemInputs = 8; // relative FRONT (bit 3)
		tMachine.mItemOutputs = 4; // relative LEFT (bit 2)
		tMachine.updateAccessibleSlots();
		// facing north (2): FACING_ROTATIONS[2] = {0,1,3,5,4,2} — front(3) → world 2 (north),
		// left(2) → world 5 (east); the truth table rows:
		assertAscendingRow(tMachine.getAccessibleSlotsFromSide((byte)2), 0, 0); // north = the input range
		assertAscendingRow(tMachine.getAccessibleSlotsFromSide((byte)5), 1, 12); // east = the output range
		assertEquals(0, tMachine.getAccessibleSlotsFromSide((byte)3).length, "south touches nothing (:538 ZL_INTEGER)");
		// north (input row): inserts land, extractions are refused
		assertTrue(tMachine.newItemHandler(Direction.NORTH).insertItem(0, new ItemStack(Items.SAND, 4), true).isEmpty());
		assertTrue(tMachine.newItemHandler(Direction.NORTH).extractItem(1, 1, true).isEmpty());
		// east (output row): extractions land, inserts are refused — the output-only side
		// still cannot INSERT into its accessible output slots (:549-559 on top of the table)
		tMachine.getInventory().setStackInSlot(1, new ItemStack(Items.SAND, 7));
		assertEquals(4, tMachine.newItemHandler(Direction.EAST).extractItem(1, 4, true).getCount());
		assertFalse(tMachine.newItemHandler(Direction.EAST).insertItem(0, new ItemStack(Items.SAND, 1), true).isEmpty());
		// south (no access): both directions refused (the untouched remainder/full stack comes back)
		assertTrue(tMachine.newItemHandler(Direction.SOUTH).extractItem(1, 1, true).isEmpty());
		assertEquals(2, tMachine.newItemHandler(Direction.SOUTH).insertItem(0, new ItemStack(Items.SAND, 2), true).getCount());
	}

	@Test
	void updateAccessibleSlotsFollowsTheFacing() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.mItemInputs = 8; // relative FRONT
		tMachine.mItemOutputs = 4; // relative LEFT
		assertTrue(tMachine.setFrontFacing((byte)5)); // face east (the :1005 onFacingChange beat)
		// FACING_TO_SIDE[5] = {0,1,3,5,2,4} — front(3) → world 5 (east), left(2) → world 3 (south)
		assertAscendingRow(tMachine.getAccessibleSlotsFromSide((byte)5), 0, 0);
		assertAscendingRow(tMachine.getAccessibleSlotsFromSide((byte)3), 1, 12);
		assertEquals(0, tMachine.getAccessibleSlotsFromSide((byte)2).length, "the old input face dropped with the rotation");
		assertTrue(tMachine.newItemHandler(Direction.EAST).insertItem(0, new ItemStack(Items.SAND, 4), true).isEmpty());
		tMachine.getInventory().setStackInSlot(1, new ItemStack(Items.SAND, 7));
		assertFalse(tMachine.newItemHandler(Direction.SOUTH).extractItem(1, 4, true).isEmpty());
	}

	// ------------------------------------------------------------------
	// ② the fluid auto-IO arms
	// ------------------------------------------------------------------

	@Test
	void fluidAutoInputPullDrainsTheNeighborIntoTheInputTank() {
		RecipeMap tMap = fluidMap();
		TileEntityBasicMachine tSource = makeFluidMachine(tMap);
		tSource.mTanksOutput[0].add(800, new FluidStack(Fluids.WATER, 800));

		TileEntityBasicMachine tSink = new TileEntityBasicMachine(
				machineType(tMap, 1, false), POS2, Blocks.BRICKS.defaultBlockState(), tMap, 1, false, null) {
			@Override
			protected IFluidHandler getFluidInputTarget(byte aWorldSide) {
				return tSource.newFluidHandler(null); // the side-less all-open drain face (the P5 ruling)
			}
		};
		tSink.setLevel(emptyLevel());
		tSink.mFluidAutoInput = 0; // relative bottom → world bottom (facing north), the :696 translation
		drive(tSink, 1); // the doActive re-check beat runs checkRecipe(T, T) → the :696-705 pull

		assertEquals(0, tSource.mTanksOutput[0].amount(), "the neighbor capability was drained empty");
		assertEquals(800, tSink.mTanksInput[0].amount(), "the :701 SIDE_ANY fillable adopted the pull");
		assertTrue(tSink.mTanksInput[0].contains(new FluidStack(Fluids.WATER, 1)));
	}

	@Test
	void fluidAutoInputPullRespectsTheTankCapacity() {
		RecipeMap tMap = fluidMap();
		TileEntityBasicMachine tSource = makeFluidMachine(tMap);
		tSource.mTanksOutput[0].add(1500, new FluidStack(Fluids.WATER, 1500));

		TileEntityBasicMachine tSink = new TileEntityBasicMachine(
				machineType(tMap, 1, false), POS2, Blocks.BRICKS.defaultBlockState(), tMap, 1, false, null) {
			@Override
			protected IFluidHandler getFluidInputTarget(byte aWorldSide) {
				return tSource.newFluidHandler(null);
			}
		};
		tSink.setLevel(emptyLevel());
		tSink.mFluidAutoInput = 0;
		drive(tSink, 1);
		assertEquals(1000, tSink.mTanksInput[0].amount(), "the SIMULATE fill bounds the pull at the tank capacity (the 1000 default)");
		assertEquals(500, tSource.mTanksOutput[0].amount(), "the drain EXECUTE took exactly what landed (never over-draws)");
	}

	@Test
	void theUndefinedAutoInputSideKeepsThePullOff() {
		RecipeMap tMap = fluidMap();
		TileEntityBasicMachine tSource = makeFluidMachine(tMap);
		tSource.mTanksOutput[0].add(800, new FluidStack(Fluids.WATER, 800));

		TileEntityBasicMachine tSink = new TileEntityBasicMachine(
				machineType(tMap, 1, false), POS2, Blocks.BRICKS.defaultBlockState(), tMap, 1, false, null) {
			@Override
			protected IFluidHandler getFluidInputTarget(byte aWorldSide) {
				return tSource.newFluidHandler(null);
			}
		};
		tSink.setLevel(emptyLevel());
		// SIDE_UNDEFINED (the registered default) folds the :697 arm away — nothing moves
		drive(tSink, 3);
		assertEquals(0, tSink.mTanksInput[0].amount(), "the undefined auto-input side keeps the pull off");
		assertEquals(800, tSource.mTanksOutput[0].amount());
	}

	@Test
	void fluidAutoOutputPushDrainsTheOutputTankIntoTheNeighbor() {
		RecipeMap tMap = fluidMap();
		List<FluidStack> tReceived = new ArrayList<>();
		IFluidHandler tBarrel = new IFluidHandler() {
			@Override public int getTanks() {return 0;}
			@Override public FluidStack getFluidInTank(int aTank) {return FluidStack.EMPTY;}
			@Override public int getTankCapacity(int aTank) {return 0;}
			@Override public boolean isFluidValid(int aTank, FluidStack aStack) {return false;}
			@Override public int fill(FluidStack aResource, FluidAction aAction) {
				if (aResource == null || aResource.isEmpty()) return 0;
				if (aAction.execute()) tReceived.add(aResource.copy());
				return aResource.getAmount(); // an infinitely thirsty sink
			}
			@Override public FluidStack drain(FluidStack aResource, FluidAction aAction) {return FluidStack.EMPTY;}
			@Override public FluidStack drain(int aMaxDrain, FluidAction aAction) {return FluidStack.EMPTY;}
		};

		TileEntityBasicMachine tPusher = new TileEntityBasicMachine(
				machineType(tMap, 1, false), POS, Blocks.BRICKS.defaultBlockState(), tMap, 1, false, null) {
			@Override
			protected IFluidHandler getFluidOutputTarget(byte aWorldSide) {
				return tBarrel; // the :978-980 seam — the Fluid argument folds away
			}
		};
		tPusher.setLevel(emptyLevel());
		tPusher.mTanksOutput[0].add(500, new FluidStack(Fluids.WATER, 500));
		tPusher.mFluidAutoOutput = 3; // relative front → world north at facing north
		drive(tPusher, 1); // the :459 arm runs BEFORE doWork

		assertEquals(0, tPusher.mTanksOutput[0].amount(), "the :994-996 push drained the output tank");
		assertEquals(1, tReceived.size());
		assertEquals(500, tReceived.get(0).getAmount());
	}

	@Test
	void theAutoOutputFaceRefusesExternalFill() {
		TileEntityBasicMachine tMachine = makeFluidMachine(fluidMap());
		FluidStack tWater = new FluidStack(Fluids.WATER, 500);
		assertNotNull(tMachine.getFluidTankFillable((byte)3, tWater), "no auto-output configured: every face fills");

		tMachine.mFluidAutoOutput = 3; // relative front → world 2 (north) at facing north
		assertNull(tMachine.getFluidTankFillable((byte)2, tWater), ":565 — the push-only face refuses external fill");
		assertNotNull(tMachine.getFluidTankFillable((byte)3, tWater), "the other faces keep filling");
		assertEquals(500, tMachine.newFluidHandler(Direction.SOUTH).fill(tWater, FluidAction.EXECUTE), "the mask-gated faces fill as before");
		assertEquals(0, tMachine.newFluidHandler(Direction.NORTH).fill(tWater, FluidAction.EXECUTE), "the capability surface carries the refuse leg");
	}

	// ------------------------------------------------------------------
	// ③ NBT_TANK_CAPACITY + mCanUseOutputTanks
	// ------------------------------------------------------------------

	@Test
	void canUseOutputTanksFeedsTheRecipeFromTheOutputTank() {
		RecipeMap tMap = fluidMap();
		tMap.addRecipe(new Recipe(true, null, null,
				new FluidStack[] {new FluidStack(Fluids.WATER, 100)}, new FluidStack[0], 16, 16, 0));

		TileEntityBasicMachine tMachine = makeFluidMachine(tMap);
		tMachine.mTanksOutput[0].add(100, new FluidStack(Fluids.WATER, 100));

		// default OFF: the empty input tanks find nothing and the output tank is untouched
		assertEquals(TileEntityBasicMachine.DID_NOT_FIND_RECIPE, tMachine.checkRecipe(true, false));
		assertEquals(100, tMachine.mTanksOutput[0].amount());

		// :716-732 — the fallback re-runs the lookup AND the consume against mTanksOutput
		tMachine.mCanUseOutputTanks = true;
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		assertEquals(0, tMachine.mTanksOutput[0].amount(), "the recipe drained the OUTPUT tank directly");
		assertNotNull(tMachine.mCurrentRecipe, "the fallback recipe drives the process chain");
		assertEquals(256, tMachine.mMaxProgress, "the :766-774 energy math armed (16 EUt × 16 t)");
	}

	@Test
	void nbtKeysArmTheRegistrationConfigFamily() {
		TileEntityBasicMachine tMachine = makeFluidMachine(fluidMap());
		CompoundTag tTag = new CompoundTag();
		tTag.putByte(TileEntityBasicMachine.NBT_ITEM_SIDE_IN, (byte)8);
		tTag.putByte(TileEntityBasicMachine.NBT_ITEM_SIDE_OUT, (byte)4);
		tTag.putByte(TileEntityBasicMachine.NBT_TANK_SIDE_IN, (byte)2);
		tTag.putByte(TileEntityBasicMachine.NBT_TANK_SIDE_AUTO_IN, (byte)3);
		tTag.putByte(TileEntityBasicMachine.NBT_TANK_SIDE_AUTO_OUT, (byte)5);
		tTag.putBoolean(TileEntityBasicMachine.NBT_USE_OUTPUT_TANK, true);
		tTag.putInt(TileEntityBasicMachine.NBT_TANK_CAPACITY, 5000);
		tMachine.load(tTag);

		assertEquals((byte)(8 | TileEntityBasicMachine.SBIT_A), tMachine.mItemInputs, ":137 — SBIT_A is ORed in");
		assertEquals((byte)(4 | TileEntityBasicMachine.SBIT_A), tMachine.mItemOutputs, ":138");
		assertEquals((byte)(2 | TileEntityBasicMachine.SBIT_A), tMachine.mFluidInputs, ":143");
		assertEquals(3, tMachine.mFluidAutoInput, ":145 — the auto sides take the raw byte");
		assertEquals(5, tMachine.mFluidAutoOutput, ":146");
		assertTrue(tMachine.mCanUseOutputTanks, ":132");
		assertEquals(5000, tMachine.mTanksInput[0].getCapacity(), ":157-160 — applied BEFORE the content read");
	}

	@Test
	void outOfDomainAutoIOSidesFoldToUndefinedAndMergeAsNoOp() {
		RecipeMap tMap = fluidMap();
		TileEntityBasicMachine tSource = makeFluidMachine(tMap);
		tSource.mTanksOutput[0].add(800, new FluidStack(Fluids.WATER, 800));

		TileEntityBasicMachine tSink = new TileEntityBasicMachine(
				machineType(tMap, 1, false), POS2, Blocks.BRICKS.defaultBlockState(), tMap, 1, false, null) {
			@Override
			protected IFluidHandler getFluidInputTarget(byte aWorldSide) {
				return tSource.newFluidHandler(null); // the side-less all-open drain face (the P5 ruling)
			}
		};
		tSink.setLevel(emptyLevel());
		// the /data merge form with out-of-domain bytes: 99 would alias through the &7 lookup
		// (99 & 7 = 3 = the relative front), 6 is already past the 0..5 relative face domain
		CompoundTag tTag = new CompoundTag();
		tTag.putByte(TileEntityBasicMachine.NBT_TANK_SIDE_AUTO_IN, (byte)99);
		tTag.putByte(TileEntityBasicMachine.NBT_TANK_SIDE_AUTO_OUT, (byte)6);
		tSink.load(tTag);
		assertEquals(TileEntityBasicMachine.SIDE_UNDEFINED, tSink.mFluidAutoInput, ":145 — 99 folds to the off sentinel instead of a real face");
		assertEquals(TileEntityBasicMachine.SIDE_UNDEFINED, tSink.mFluidAutoOutput, ":146 — 6 folds too (the relative domain is 0..5)");

		// the merged no-op: the folded sides keep both arms shut — nothing pulls, nothing moves
		drive(tSink, 3);
		assertEquals(0, tSink.mTanksInput[0].amount(), "the folded auto-input side keeps the pull off (the unguarded value would have aliased onto front and drained)");
		assertEquals(800, tSource.mTanksOutput[0].amount());
	}

	@Test
	void inDomainAutoIOSidesLoadByteIdenticalToThePreGuardBehaviour() {
		TileEntityBasicMachine tMachine = makeFluidMachine(fluidMap());
		CompoundTag tTag = new CompoundTag();
		tTag.putByte(TileEntityBasicMachine.NBT_TANK_SIDE_AUTO_IN, (byte)-1);
		tTag.putByte(TileEntityBasicMachine.NBT_TANK_SIDE_AUTO_OUT, (byte)0);
		tMachine.load(tTag);
		assertEquals(TileEntityBasicMachine.SIDE_UNDEFINED, tMachine.mFluidAutoInput, "the off value -1 loads as-is");
		assertEquals(0, tMachine.mFluidAutoOutput, "a valid relative face 0 loads as-is");

		for (byte tSide = 1; tSide <= 5; tSide++) {
			tTag.putByte(TileEntityBasicMachine.NBT_TANK_SIDE_AUTO_IN, tSide);
			tMachine.load(tTag);
			assertEquals(tSide, tMachine.mFluidAutoInput, "every valid relative face 0..5 loads byte-identical");
		}
	}

	@Test
	void loadKeepsTheConstructorInjectedConfigAndCapacitySurvivesARoundTrip() {
		TileEntityBasicMachine tMachine = makeFluidMachine(fluidMap());
		tMachine.mItemInputs = 8; // the applyRow/carrier injection
		tMachine.mTanksInput[0].add(300, new FluidStack(Fluids.WATER, 300));

		CompoundTag tTag = new CompoundTag();
		tMachine.saveAdditional(tTag);
		assertFalse(tTag.contains(TileEntityBasicMachine.NBT_ITEM_SIDE_IN), "the registration-config keys are never persisted (the upstream writeToNBT2 form)");
		assertFalse(tTag.contains(TileEntityBasicMachine.NBT_TANK_CAPACITY));

		// a plain round trip keeps the carrier values (the hasKey-guarded legs)
		TileEntityBasicMachine tReloaded = makeFluidMachine(fluidMap());
		tReloaded.load(tTag);
		assertEquals(8, tMachine.mItemInputs, "absent keys keep the constructor/applyRow value");
		assertEquals(300, tReloaded.mTanksInput[0].amount(), "the tank content round-trips");
		assertEquals(1000, tReloaded.mTanksInput[0].getCapacity());

		// the /data merge form: the config key rides the merged tag and re-arms the capacity
		tTag.putInt(TileEntityBasicMachine.NBT_TANK_CAPACITY, 5000);
		tReloaded.load(tTag);
		assertEquals(5000, tReloaded.mTanksInput[0].getCapacity());
		assertEquals(300, tReloaded.mTanksInput[0].amount(), "the content survives the capacity re-arm");
	}

	/** The ascending-row assertion (the upstream UT.Code.getAscendingArray shapes). */
	private static void assertAscendingRow(int[] aRow, int aStart, int aEnd) {
		assertEquals(aEnd - aStart + 1, aRow.length);
		for (int i = 0; i < aRow.length; i++) assertEquals(aStart + i, aRow[i]);
	}
}
