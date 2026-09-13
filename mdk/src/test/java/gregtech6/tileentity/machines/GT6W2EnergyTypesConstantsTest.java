package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GTMachines;

/**
 * The p29-w2-energy-types-5tier 5-tier 立行制 constants (the OFFLINE half):
 * {@link GTMachines#ELECTRIC_T5} = Ti (upstream MT.java:3691 index 5, the lazy-supplier
 * form), {@link GTMachines#EV_TIER_INPUTS} = {4096, 8192, 16384} (the :126 conversion
 * min = in/2 / max = in*2 over the Loader:1340 T5 NBT_INPUT 8192) as a PARALLEL constant
 * that leaves the 4-row {@link GTMachines#TIER_INPUTS} byte-identical, and
 * {@link GTMachines#CRYO_PARALLEL} = {4, 8, 16, 32, 64} (the Loader:1628-1632 CryoMixer
 * NBT_PARALLEL columns, declared before its card-④ consumer).
 *
 * <p>Naming erratum (the class doc on EV_TIER_INPUTS carries it): the constant keeps the
 * card's name, but the 5-tier word is VN[5] = "IV" (CS.java:154) — "ev" is already T4's
 * word in this repo.
 */
public class GT6W2EnergyTypesConstantsTest extends TileEntityBasicMachineOfflineTestBase {

	/** The T5 material rung resolves to Ti and is the SAME shared instance the rows will ride. */
	@Test
	void electricT5RungIsTitanium() {
		assertSame(MT.Ti, GTMachines.ELECTRIC_T5.get(), "MT.java:3691 Electric_T[5] = Ti");
		// the [1..4] ladder stays untouched (the supplier list form)
		assertEquals(4, GTMachines.ELECTRIC_T_LADDER.size(), "the W1 4-rung Electric_T ladder unchanged");
	}

	/** The 5-tier window: the :126 conversion over 8192, the TIER_INPUTS table untouched. */
	@Test
	void evTierInputsIsTheParallelWindowOver8192() {
		assertArrayEquals(new long[] {4096, 8192, 16384}, GTMachines.EV_TIER_INPUTS,
				"min = 8192/2, in = 8192, max = 8192*2 (the :126 conversion, the Loader:1340 T5 row)");
		// the PARALLEL-constant contract: the 4-row table keeps its exact bytes and its own
		// array identity (no row-indexed consumer drifts)
		assertEquals(4, GTMachines.TIER_INPUTS.length, "TIER_INPUTS stays the 4-row table");
		assertArrayEquals(new long[] {16, 32, 64}, GTMachines.TIER_INPUTS[0], "TIER_INPUTS[0] unchanged");
		assertArrayEquals(new long[] {1024, 2048, 4096}, GTMachines.TIER_INPUTS[3], "TIER_INPUTS[3] unchanged");
		assertNotSame(GTMachines.EV_TIER_INPUTS, GTMachines.ULV_TIER_INPUTS, "a distinct table, not an alias");
		// the window geometry: the T4 max (4096) is exactly the T5 min — the ladder is gapless
		assertEquals(GTMachines.TIER_INPUTS[3][2], GTMachines.EV_TIER_INPUTS[0],
				"the T4 window max == the T5 window min (the :126 doubling chain)");
	}

	/** The CryoMixer five-element parallel table (the card-④ consumer pins against this). */
	@Test
	void cryoParallelIsTheUpstreamFiveElementLadder() {
		assertArrayEquals(new int[] {4, 8, 16, 32, 64}, GTMachines.CRYO_PARALLEL,
				"the Loader:1628-1632 NBT_PARALLEL columns");
		// distinct from BOTH established tables (no accidental aliasing)
		assertNotSame(GTMachines.CRYO_PARALLEL, GTMachines.PARALLEL_4_32, "not the 4/8/16/32 ladder");
		assertNotSame(GTMachines.CRYO_PARALLEL, GTMachines.CENTRIFUGE_PARALLEL, "not the 1/2/4/8 ladder");
		assertEquals(4, GTMachines.PARALLEL_4_32[0], "PARALLEL_4_32 unaffected");
	}

	/** The Loader:1651-1655 TU-four energy face: 63 = the six physical faces, the auto bit outside the mask. */
	@Test
	void theTuEnergy63MaskIsAllSixPhysicalFaces() {
		byte tSixFaces = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_U
				| GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_F
				| GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B);
		assertEquals((byte)63, tSixFaces, "the six single-face bits OR to the Loader energy-63 mask");
		assertEquals((byte)64, GTBasicMachineBlock.SBIT_A, "the auto bit sits outside the 63 mask — six PHYSICAL faces");
		assertTrue(tSixFaces != GTBasicMachineBlock.SBIT_A, "the mask never carries the auto flag");
	}

	/**
	 * The [1,16] window behavior (the Loader:1651-1655 TU-four contract): the window is an
	 * EXPLICIT row override (the :126 auto-derivation over NBT_INPUT 1 would give
	 * {0, 1, 2}), and at mInputMin = 1 the :773 overclock fold is structurally DEAD —
	 * mMinEnergy (≥ 1 for any real row) is never below mInputMin, so a TU machine at this
	 * window runs every recipe at its raw mEUt (the TileEntityBase10MultiBlockMachine
	 * ":1193 zero-overclock" reading, the Coke-Oven judged precedent).
	 */
	@Test
	void theTuWindowKillsTheOverclockFold() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.TU);
		// the Loader:1651-1655 row window, set through the same fields the NBT bind writes
		tMachine.mInputMin = 1;
		tMachine.mInput = 1;
		tMachine.mInputMax = 16;
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		assertEquals(16L, tMachine.mMinEnergy, "mMinEnergy = mEUt 16 (the TU arm, no parallel multiply)");
		assertEquals(256L, tMachine.mMaxProgress, "16 EUt × 16 duration — the raw bar");
		// the fold: 16 < mInputMin(1) is false — the 4x/2x step NEVER fires at this window
		assertEquals(16L, tMachine.mMinEnergy, "the :773 fold is dead at mInputMin 1 (the zero-overclock window)");
	}
}
