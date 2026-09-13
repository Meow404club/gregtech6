package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GTMachines;

/**
 * Task p29-w1-rm-maps-scaffold ② — the efficiency column and the :768/:771 progress
 * division. Upstream semantics (MultiTileEntityBasicMachine.java): {@code short
 * mEfficiency = 10000} (:96), the :125 read binds 0..10000, and the progress rows run
 * {@code mMaxProgress = max(1, UT.Code.units(minEnergy × duration [× parallelCount],
 * mEfficiency, 10000, T))} (:768/:771). UT.Code.units(a, orig, targ) = a × targ/orig
 * (the LH.java:334 efficiency-tooltip direction), so 10000 is the units() IDENTITY and
 * 5000 = 2× the REQUIRED progress (the Electric* rows :1504-1522 — 2× energy-time per
 * process; the same wall-clock sits at exactly half the bar).
 *
 * <p>The regression constraint is the card's headline: with the default 10000 the
 * progress math must equal the historical FOLDED expression
 * {@code units(x, 10000, 10000, true)} bit-for-bit — every pre-existing 25-map consumer
 * stays zero-drift (the acceptance "efficiency=10000→恒等").
 */
public class GTBasicMachineEfficiencyTest extends TileEntityBasicMachineOfflineTestBase {

	/** The pre-p29-w1 folded expression — the identity yardstick the default must reproduce. */
	private static long legacyMaxProgress(long aUnits) {
		return Math.max(1, TileEntityBasicMachine.units(aUnits, 10000, 10000, true));
	}

	/** The Shredder :689 chain (eUt 16 × duration 16 = 256 units at T1, the offline pour). */
	private static long shredderChainUnits() {
		return 16L * 16L;
	}

	/** The default mEfficiency is the upstream :96 10000 and checkRecipe is the IDENTITY. */
	@Test
	void defaultEfficiencyIsTheUpstreamIdentity() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		assertEquals(10000, tMachine.mEfficiency, "the upstream :96 field default");
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);

		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		assertEquals(256L, tMachine.mMaxProgress, "16 EUt × 16 duration, the :773 overclock loop inert at T1 (mMinEnergy == mInputMin)");
		assertEquals(legacyMaxProgress(shredderChainUnits()), tMachine.mMaxProgress,
				"ZERO-DRIFT: the default-efficiency progress equals the pre-p29 folded expression bit-for-bit");
	}

	/** The parallelDuration arm (:768) carries the same divisor — pinned with the Crusher 4-parallel chain shape. */
	@Test
	void defaultEfficiencyIdentityHoldsOnTheParallelDurationArm() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.CRUSHER, 4, true);
		assertEquals(10000, tMachine.mEfficiency);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(sGemItem, 4), false);

		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		// 4 gems in stock, parallel cap 4 → the consumed count is 4 (output 8 ≤ slot cap, the
		// TileEntityBasicMachineRecipeTest.crusherChainGemsToFlawedWithParallel census)
		assertEquals(legacyMaxProgress(16L * 16L * 4), tMachine.mMaxProgress,
				"the :768 arm (minEnergy × duration × parallelCount) equals the folded expression at the default");
	}

	/**
	 * efficiency 5000 (the Electric* rows :1504-1522) = 2× the REQUIRED progress per
	 * process — the upstream UT.Code.units direction is amount × target/original
	 * (a × 10000/mEfficiency), so 5000 DOUBLES the bar: the same wall-clock the identity
	 * machine finishes a process, the 5000 machine sits at EXACTLY half the bar (半程 —
	 * the card-D 共图对拍 form), i.e. 2× the energy-time per recipe.
	 */
	@Test
	void efficiency5000DoublesTheRequiredProgress() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.mEfficiency = 5000; // the Electric* NBT_EFFICIENCY 5000 form (:1504-1522)
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);

		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		assertEquals(512L, tMachine.mMaxProgress, "units(256, 5000, 10000, T) = 256 × 2 — the upstream units() direction (amount × targ/orig)");
		assertEquals(2 * legacyMaxProgress(shredderChainUnits()), tMachine.mMaxProgress, "exactly twice the identity bar — same wall-clock = exactly half the bar (半程)");
	}

	/** The ceil semantics of UT.Code.units against a non-divisor efficiency (units = ceil(x × 10000 / e)). */
	@Test
	void nonDivisorEfficiencyRoundsUp() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.mEfficiency = 3000;
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);

		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		// units(256, 3000, 10000, T): neither operand divides the other → (256 × 10000) / 3000 = 853 r 1000 → ceil 854
		assertEquals(854L, tMachine.mMaxProgress, "the :771 row is a ROUND-UP division (UT.Code.units aRoundUp T)");
	}

	/** applyRow rides the efficiency column through the upstream :125 bind form (the mask-carrier seam). */
	@Test
	void applyRowRidesTheEfficiencyColumn() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		GTMachines.applyRow(tMachine, row(5000));
		assertEquals(5000, tMachine.mEfficiency, "the Electric*-form row column lands on the BE");

		TileEntityBasicMachine tLegacy = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		GTMachines.applyRow(tLegacy, GTMachines.WIREMILL_ROWS.get(0));
		assertEquals(10000, tLegacy.mEfficiency, "a null-column row keeps the :96 default — the zero-drift face");

		GTMachines.applyRow(tLegacy, row(20000));
		assertEquals(10000, tLegacy.mEfficiency, "the :125 bind ceiling (0..10000)");

		GTMachines.applyRow(tLegacy, row(-5));
		assertEquals(0, tLegacy.mEfficiency, "the :125 bind floor 0 kept verbatim (units() treats 0 as the identity — the upstream quirk, TileEntityBasicMachine.mEfficiency doc)");
	}

	/** EVERY existing row carries a null efficiency column — the 25-map consumer zero-drift census. */
	@Test
	void everyLegacyRowCarriesANullEfficiencyColumn() {
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.DRYER_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.CANNER_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.PRESS_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.EXTRUDER_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.SIFTER_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.COMPRESSOR_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.WIREMILL_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.DISTILLERY_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.CANNER_ULV_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.SHREDDER_ULV_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.CRUSHER_ULV_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.SIFTER_ULV_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.WIREMILL_ULV_ROWS) assertNull(tRow.efficiency(), tRow.path());
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLLINGMILL_ROWS) assertNull(tRow.efficiency(), tRow.path());
	}

	/** The Centrifuge non-standard parallel table (card spec ③) — the batch-C consumer pins against this. */
	@Test
	void centrifugeParallelTableIsTheUpstreamLadder() {
		assertEquals(4, GTMachines.CENTRIFUGE_PARALLEL.length);
		assertEquals(1, GTMachines.CENTRIFUGE_PARALLEL[0], "Loader_MultiTileEntities.java:1330 — the T1 = 1 arm makes the ladder NON-standard");
		assertEquals(2, GTMachines.CENTRIFUGE_PARALLEL[1]);
		assertEquals(4, GTMachines.CENTRIFUGE_PARALLEL[2]);
		assertEquals(8, GTMachines.CENTRIFUGE_PARALLEL[3]);
		// the standard-ladder contrast (the 4/8/16/32 shape) stays untouched
		assertEquals(4, GTMachines.PARALLEL_4_32[0], "PARALLEL_4_32 unaffected");
		assertTrue(GTMachines.CENTRIFUGE_PARALLEL != GTMachines.PARALLEL_4_32, "a distinct table — the Centrifuge rows must NOT alias the 4/8/16/32 ladder");
	}

	// ------------------------------------------------------------------
	// row fixtures
	// ------------------------------------------------------------------

	/** A synthetic row carrying the given raw efficiency value (the 27-arg canonical form). */
	private static GTBasicMachineBlock.MachineRow row(int aEfficiency) {
		return new GTBasicMachineBlock.MachineRow("efficiency_probe", "steel", "Steel",
				() -> null, "gt6.row.machine.shredder", 20999, 7.0F,
				0, 1, false,
				() -> GT6RecipeMaps.SHREDDER, TD.Energy.RU, "shredder",
				(byte)127, (byte)127, (byte)127, (byte)127, (byte)127,
				(byte)-1, (byte)-1, (byte)-1, (byte)-1,
				null, true, null, false,
				aEfficiency);
	}
}
