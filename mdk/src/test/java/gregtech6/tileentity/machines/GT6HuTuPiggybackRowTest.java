package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GTMachines;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The hu-tu piggyback row acceptance (task p29-w2-hu-tu-piggyback, the OFFLINE half — the
 * {@link GT6EuHuFamiliesRowTest} shape): the seven families pinned to the upstream columns
 * (Loader_MultiTileEntities.java :1576-1579 SteamCracker / :1570-1573 CatalyticCracker /
 * :1651 Coagulator / :1652 Generifier / :1653 Bath / :1655 Autoclave / :1412-1415 Loom),
 * the DOUBLE-VARIANT same-parameters 对拍 (both crackers: the IDENTICAL RM.java constants
 * row :67/:68 and the IDENTICAL :1570 masks, split only by map and texture), the TU four
 * card-① dynamics semantics ({@link GTMachines#TU_WINDOW} {1,1,16} + the
 * NBT_NO_CONSTANT_POWER T bind on {@code mNoConstantEnergy} + the 63→127 ALL-SIX-FACES
 * energy mask — the machine-side first carrier of the card-① type-guard assertions), the
 * Generifier parallel 100, the Loom SAME-map cross-proof with the W1 ElectricLoom and the
 * Bath P26 in-catalog map reuse.
 *
 * <p>The registration half (16 blocks + 16 items + 7 family BETs) only resolves on a live
 * server (the runServer/RCON gate — the sweep p29_w2_hu_tu group; the NO_CONSTANT_POWER
 * LIVE resume arm is the p29_w2_coagulator chain's, the card-① 遗留 obligation).
 */
public class GT6HuTuPiggybackRowTest extends TileEntityBasicMachineOfflineTestBase {

	/** The :1570 cracker masks, post-read (both crackers share the IDENTICAL row shape). */
	private static final byte CRACKER_ENERGY = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	private static final byte CRACKER_IN = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	private static final byte CRACKER_OUT = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A);
	/** The TU four energy mask: NBT_ENERGY_ACCEPTED_SIDES 63 through the :151 read = 63|SBIT_A = 127 (ALL SIX faces). */
	private static final byte SIX_FACES = (byte)127;
	/** :1652/:1653 masks (the Generifier/Bath pair). */
	private static final byte TU_IN = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	private static final byte TU_OUT = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A);
	/** :1655 Autoclave masks — item in U|L auto LEFT, item out B|R auto RIGHT, tank in D|L auto BOTTOM, tank out B|R auto BACK. */
	private static final byte AUTO_IN = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	private static final byte AUTO_OUT = (byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A);
	private static final byte AUTO_TANK_IN = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	private static final byte NO_KEY = (byte)127;
	private static final byte UNDEFINED = (byte)-1;

	// ------------------------------------------------------------------
	// the cracker DOUBLE VARIANT: same parameters, two maps (ACCEPTANCE ①)
	// ------------------------------------------------------------------

	@Test
	void crackerRowsMatchTheUpstreamColumnsAndEachOther() {
		String[] tSlugs = {"steel", "invar", "titanium", "tungsten_carbide"};
		float[] tHardness = {6.0F, 4.0F, 9.0F, 12.5F};
		int[] tWindow = {32, 128, 512, 2048};
		assertCrackerFamily(GTMachines.STEAM_CRACKER_ROWS, "steamcracker", GT6RecipeMaps.STEAM_CRACKING,
				GTMachines.MACHINE_STEAM_CRACKER_UNIT_KEY, 20491, tSlugs, tHardness, tWindow);
		assertCrackerFamily(GTMachines.CATALYTIC_CRACKER_ROWS, "catalyticcracker", GT6RecipeMaps.CATALYTIC_CRACKING,
				GTMachines.MACHINE_CATALYTIC_CRACKER_UNIT_KEY, 20481, tSlugs, tHardness, tWindow);
		// the 对拍 half: the two families' rows are COLUMN-IDENTICAL except map+texture
		// (the :1570 masks share one shape; the TIER_INPUTS windows and the hardness ladder
		// are the same arrays) — and the two MAPS are the IDENTICAL RM.java constants row
		assertNotSame(GT6RecipeMaps.STEAM_CRACKING, GT6RecipeMaps.CATALYTIC_CRACKING, "two maps, one constants row");
		for (int i = 0; i < 4; i++) {
			GTBasicMachineBlock.MachineRow tSteam = GTMachines.STEAM_CRACKER_ROWS.get(i);
			GTBasicMachineBlock.MachineRow tCat = GTMachines.CATALYTIC_CRACKER_ROWS.get(i);
			assertEquals(tSteam.energySides(), tCat.energySides());
			assertEquals(tSteam.itemIn(), tCat.itemIn());
			assertEquals(tSteam.itemOut(), tCat.itemOut());
			assertEquals(tSteam.fluidIn(), tCat.fluidIn());
			assertEquals(tSteam.fluidOut(), tCat.fluidOut());
			assertEquals(tSteam.fluidAutoIn(), tCat.fluidAutoIn());
			assertEquals(tSteam.fluidAutoOut(), tCat.fluidAutoOut());
			assertEquals(tSteam.itemAutoIn(), tCat.itemAutoIn());
			assertEquals(tSteam.itemAutoOut(), tCat.itemAutoOut());
			assertEquals(tSteam.hardness(), tCat.hardness());
			assertNotSame(tSteam.texture(), tCat.texture(), "split only by the art token");
			assertFalse(tSteam.parallelDuration(), "no NBT_PARALLEL_DURATION key on either family");
		}
		// the RM.java:67/:68 constants row — 1/3/0 items, 2/9/1 fluids, MIN 2 on BOTH maps
		assertMapShape(GT6RecipeMaps.STEAM_CRACKING);
		assertMapShape(GT6RecipeMaps.CATALYTIC_CRACKING);
	}

	private void assertCrackerFamily(java.util.List<GTBasicMachineBlock.MachineRow> aRows, String aTexture,
			RecipeMap aMap, String aUnitKey, int aFirstMetaId, String[] tSlugs, float[] tHardness, int[] tWindow) {
		assertEquals(4, aRows.size(), "the four cracker rows");
		for (int i = 0; i < aRows.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = aRows.get(i);
			assertEquals(aTexture + (i == 0 ? "" : "_t" + (i + 1)), tRow.path());
			assertEquals(aFirstMetaId + i, tRow.metaId(), "the MultiTile id column of tier " + (i + 1));
			assertEquals(i, tRow.tier(), "the TIER_INPUTS tier index");
			assertEquals(tHardness[i], tRow.hardness(), "the Heat_T hardness ladder");
			assertSame(TD.Energy.HU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.HU");
			assertSame(aMap, tRow.recipes().get(), "NBT_RECIPEMAP");
			assertEquals(aTexture, tRow.texture(), "NBT_TEXTURE");
			assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL key → 1");
			assertEquals(aUnitKey, tRow.displayKey(), "the material-word one-slot form");
			assertEquals(tSlugs[i], tRow.matSlug());
			assertEquals(CRACKER_ENERGY, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_D through the :151 read");
			assertEquals(CRACKER_IN, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_U|SBIT_L");
			assertEquals(CRACKER_OUT, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R|SBIT_B");
			assertEquals(CRACKER_IN, tRow.fluidIn(), "NBT_TANK_SIDE_IN SBIT_U|SBIT_L");
			assertEquals(CRACKER_OUT, tRow.fluidOut(), "NBT_TANK_SIDE_OUT SBIT_R|SBIT_B");
			assertEquals(2, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_RIGHT");
			assertEquals(1, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_TOP");
			assertEquals(5, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_BACK");
			// the BE half: the TIER_INPUTS window (the machine() assignment through the BET)
			TileEntityBasicMachine tMachine = rowMachine(tRow);
			assertEquals(GTMachines.TIER_INPUTS[i][0], tMachine.mInputMin, "the :126 window of tier " + (i + 1));
			assertEquals(GTMachines.TIER_INPUTS[i][1], tMachine.mInput);
			assertEquals(GTMachines.TIER_INPUTS[i][2], tMachine.mInputMax);
		}
	}

	private void assertMapShape(RecipeMap aMap) {
		assertEquals(1, aMap.mInputItemsCount, "1 item input slot (RM.java:67/:68)");
		assertEquals(3, aMap.mOutputItemsCount, "3 item output slots");
		assertEquals(2, aMap.mInputFluidCount, "2 fluid input slots");
		assertEquals(9, aMap.mOutputFluidCount, "9 fluid output slots");
		assertEquals(2, aMap.mMinimalInputs, "MIN 2 — the cracking maps' minimum ingredient count");
	}

	// ------------------------------------------------------------------
	// each cracker map runs its own smoke row (the datapack face, offline)
	// ------------------------------------------------------------------

	@Test
	void eachCrackerMapRunsItsOwnSmokeRow() {
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		// steamcracking.json: 1 coal + water 1000 → 1 charcoal, eUt 32, duration 128
		GT6RecipeMaps.STEAM_CRACKING.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.COAL, 1)},
				new ItemStack[] {new ItemStack(Items.CHARCOAL, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, null, 128, 32, 0));
		// catalyticcracking.json: 1 charcoal + water 1000 → 1 coal (the mirror-image plumbing proof)
		GT6RecipeMaps.CATALYTIC_CRACKING.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.CHARCOAL, 1)},
				new ItemStack[] {new ItemStack(Items.COAL, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, null, 128, 32, 0));

		TileEntityBasicMachine tSteam = rowMachine(GTMachines.STEAM_CRACKER_ROWS.get(0));
		tSteam.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.COAL, 1), false);
		tSteam.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tSteam.checkRecipe(true, true),
				"the steam cracker finds ITS map's row");
		assertEquals(4096L, tSteam.mMaxProgress, "units(32 × 128 × 1, 10000, 10000, T) = 4096");
		inject(tSteam, 32, 32);
		assertTrue(tSteam.getInventory().getStackInSlot(tSteam.getInputSlotCount()).getCount() >= 1,
				"the charcoal landed (32 ticks @ 32/tick)");

		TileEntityBasicMachine tCat = rowMachine(GTMachines.CATALYTIC_CRACKER_ROWS.get(0));
		tCat.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.CHARCOAL, 1), false);
		tCat.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tCat.checkRecipe(true, true),
				"the catalytic cracker finds ITS map's row — NOT the steam row");
		assertEquals(4096L, tCat.mMaxProgress, "the SAME budget — the identical parameters row");
		inject(tCat, 32, 32);
		assertTrue(tCat.getInventory().getStackInSlot(tCat.getInputSlotCount()).getCount() >= 1,
				"the coal landed");
	}

	// ------------------------------------------------------------------
	// the TU four: the card-① dynamics semantics on machine rows (ACCEPTANCE ②)
	// ------------------------------------------------------------------

	@Test
	void tuFourRowsCarryTheCard1DynamicsSemantics() {
		assertTuSingle(GTMachines.COAGULATOR_ROWS.get(0), "coagulator", GT6RecipeMaps.COAGULATOR,
				GTMachines.COAGULATOR_DISPLAY_KEY, 22000);
		assertTuSingle(GTMachines.GENERIFIER_ROWS.get(0), "generifier", GT6RecipeMaps.GENERIFIER,
				GTMachines.GENERIFIER_DISPLAY_KEY, 22001);
		assertTuSingle(GTMachines.BATH_ROWS.get(0), "bath", GT6RecipeMaps.BATH,
				GTMachines.BATH_DISPLAY_KEY, 22002);
		assertTuSingle(GTMachines.AUTOCLAVE_ROWS.get(0), "autoclave", GT6RecipeMaps.AUTOCLAVE,
				GTMachines.AUTOCLAVE_DISPLAY_KEY, 22004);
		// four DISTINCT maps (the BATH instance is the P26 in-catalog one, reused not rebuilt)
		assertNotSame(GT6RecipeMaps.COAGULATOR, GT6RecipeMaps.GENERIFIER);
		assertNotSame(GT6RecipeMaps.GENERIFIER, GT6RecipeMaps.BATH);
		assertNotSame(GT6RecipeMaps.BATH, GT6RecipeMaps.AUTOCLAVE);

		// the coagulator's distinctive :1651 mask face: NO NBT_INV_SIDE_IN key → the 127
		// field default, NO tank-out key → 127, item auto in UNDEFINED, out SIDE_BOTTOM
		GTBasicMachineBlock.MachineRow tCoag = GTMachines.COAGULATOR_ROWS.get(0);
		assertEquals(NO_KEY, tCoag.itemIn(), "the fluid-only map's zero-item face (the 127 field default)");
		assertEquals(NO_KEY, tCoag.fluidOut(), "no NBT_TANK_SIDE_OUT key");
		assertEquals(UNDEFINED, tCoag.itemAutoIn(), "no NBT_INV_SIDE_AUTO_IN key → SIDE_UNDEFINED");
		assertEquals(0, tCoag.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_BOTTOM");
		assertEquals(1, tCoag.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP");
		assertEquals(UNDEFINED, tCoag.fluidAutoOut(), "no NBT_TANK_SIDE_AUTO_OUT key");
		assertEquals(NO_KEY, tCoag.energySides(), "63 | SBIT_A = 127 — all six faces");

		// the autoclave's distinctive :1655 masks
		GTBasicMachineBlock.MachineRow tAuto = GTMachines.AUTOCLAVE_ROWS.get(0);
		assertEquals(AUTO_IN, tAuto.itemIn());
		assertEquals(AUTO_OUT, tAuto.itemOut());
		assertEquals(AUTO_TANK_IN, tAuto.fluidIn(), "NBT_TANK_SIDE_IN SBIT_D|SBIT_L");
		assertEquals(AUTO_OUT, tAuto.fluidOut(), "NBT_TANK_SIDE_OUT SBIT_B|SBIT_R");
		assertEquals(0, tAuto.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_BOTTOM");
		assertEquals(5, tAuto.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_BACK");
		assertEquals(NO_KEY, tAuto.energySides(), "all six faces");

		// the Generifier NBT_PARALLEL 100 column — NO NBT_PARALLEL_DURATION → the :770-771 arm
		GTBasicMachineBlock.MachineRow tGen = GTMachines.GENERIFIER_ROWS.get(0);
		assertEquals(GTMachines.GENERIFIER_PARALLEL, tGen.parallel(), "NBT_PARALLEL 100 (:1652)");
		assertFalse(tGen.parallelDuration(), "no NBT_PARALLEL_DURATION key");
		TileEntityBasicMachine tGenBe = tuRowMachine(GTMachines.GENERIFIER_ROWS.get(0));
		assertEquals(100, tGenBe.mParallel, "the BE carries the parallel count");
	}

	private void assertTuSingle(GTBasicMachineBlock.MachineRow aRow, String aPath, RecipeMap aMap,
			String aDisplayKey, int aMetaId) {
		assertEquals(aPath, aRow.path());
		assertEquals(aMetaId, aRow.metaId());
		assertEquals("stainless_steel", aRow.matSlug(), "the MT.StainlessSteel housing column");
		assertSame(TD.Energy.TU, aRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.TU");
		assertSame(aMap, aRow.recipes().get(), "NBT_RECIPEMAP");
		assertEquals(aPath, aRow.texture(), "NBT_TEXTURE");
		assertEquals(6.0F, aRow.hardness(), "NBT_HARDNESS 6.0F");
		assertEquals(aDisplayKey, aRow.displayKey(), "the atomic no-slot display template");
		// the BE half — the tuMachine/applyTuRow columns (the card-① semantics ON a machine row)
		TileEntityBasicMachine tMachine = tuRowMachine(aRow);
		assertTrue(tMachine.mNoConstantEnergy, "the :123 NBT_NO_CONSTANT_POWER T bind — the :894 idle reset is SKIPPED");
		assertEquals(GTMachines.TU_WINDOW[0], tMachine.mInputMin, "NBT_INPUT_MIN 1 (:1651-1655)");
		assertEquals(GTMachines.TU_WINDOW[1], tMachine.mInput, "NBT_INPUT 1");
		assertEquals(GTMachines.TU_WINDOW[2], tMachine.mInputMax, "NBT_INPUT_MAX 16 — the window edge the card-① chain dials");
		assertSame(TD.Energy.TU, tMachine.mEnergyTypeAccepted);
		assertEquals(SIX_FACES, tMachine.mEnergyInputs, "the :151 read of 63 — every world face accepts the TU carrier");
	}

	// ------------------------------------------------------------------
	// the TU window: 1-TU packets run, sub-1 is impossible, the :773 gate is inert
	// ------------------------------------------------------------------

	@Test
	void aOneTuPacketDrivesTheCoagulatorRow() {
		// coagulator.json: water 1000 → 1 snowball, eUt 1, duration 64 (the FLUID-ONLY row)
		GT6RecipeMaps.COAGULATOR.addRecipe(new Recipe(true,
				new ItemStack[0],
				new ItemStack[] {new ItemStack(Items.SNOWBALL, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, null, 64, 1, 0));
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		TileEntityBasicMachine tMachine = tuRowMachine(GTMachines.COAGULATOR_ROWS.get(0));
		tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true),
				"the zero-item row binds on the fluid leg alone");
		assertEquals(64L, tMachine.mMaxProgress, "units(1 × 64 × 1, 10000, 10000, T) = 64 — the :770 TU arm, mMinEnergy = mEUt = 1");
		inject(tMachine, 16, 1);
		assertTrue(tMachine.mProgress > 0, "1-TU packets drive the machine (the 1-包 arm)");
		assertEquals(16L, tMachine.mProgress, "each 1-EU tick banks exactly 1 progress");
	}

	// ------------------------------------------------------------------
	// the NO_CONSTANT_POWER retention, machine-row form (the card-① 遗留
	// obligation's offline twin of the p29_w2_coagulator live arm)
	// ------------------------------------------------------------------

	@Test
	void theTuRowRetainsProgressAcrossAPowerGap() {
		GT6RecipeMaps.COAGULATOR.addRecipe(new Recipe(true,
				new ItemStack[0],
				new ItemStack[] {new ItemStack(Items.SNOWBALL, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, null, 64, 1, 0));
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		TileEntityBasicMachine tMachine = tuRowMachine(GTMachines.COAGULATOR_ROWS.get(0));
		tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		inject(tMachine, 10, 1);
		assertEquals(10L, tMachine.mProgress, "mid-flight at 10/64");

		// the power gap: 60 dispatcher ticks with NO injection (the live sleep arm's shape —
		// a constant-power machine would reset to 0 through the :894 gate)
		for (int i = 0; i < 60; i++) tMachine.updateEntity();
		assertEquals(10L, tMachine.mProgress, "mNoConstantEnergy keeps the parked progress EXACTLY across the gap");

		// the resume: the retained tail shortens the bar (the 54 remaining ticks)
		inject(tMachine, 54, 1);
		assertEquals(0L, tMachine.mMaxProgress, "the process completed after the gap (the bar parked 0/0)");
		assertTrue(tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount()).getCount() >= 1,
				"the snowball landed — the retained progress WAS the work");
	}

	// ------------------------------------------------------------------
	// the Generifier parallel 100 (ACCEPTANCE ③)
	// ------------------------------------------------------------------

	@Test
	void generifierParallelHundredConsumesAHundredInputs() {
		// generifier.json: 1 sand + water 10 → 1 clay ball, eUt 16, duration 32
		GT6RecipeMaps.GENERIFIER.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.SAND, 1)},
				new ItemStack[] {new ItemStack(Items.CLAY_BALL, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 10)}, null, 32, 16, 0));
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		TileEntityBasicMachine tMachine = tuRowMachine(GTMachines.GENERIFIER_ROWS.get(0));
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.SAND, 100), false);
		tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		// the :770 TU arm: mMinEnergy = mEUt = 16 (the parallel count multiplies the
		// OUTPUTS, not the energy), the bar = units(16 × 32) = 512, gain 16/tick → 32 ticks
		assertEquals(512L, tMachine.mMaxProgress, "the TU parallel bar does NOT scale with the count (:770-771)");
		inject(tMachine, 32, 16);
		// the first bar completes 64 processes (the :647 output-slot stack cap 64/1), the
		// restart covers the remaining 36 on the second bar
		assertTrue(tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount()).getCount() >= 64,
				"the first bar banked the 64-stack cap of clay balls");
		inject(tMachine, 32, 16);
		assertTrue(tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount()).getCount() >= 100,
				"100 inputs → 100 outputs — the parallel-100 count consumed the whole stock");
	}

	// ------------------------------------------------------------------
	// the Loom SAME-map cross-proof with the W1 ElectricLoom (ACCEPTANCE ④)
	// ------------------------------------------------------------------

	@Test
	void loomRowsShareTheW1ElectricLoomMapAndRideTheKineticLadder() {
		String[] tSlugs = {"bronze", "steel", "titanium", "tungstensteel"};
		float[] tHardness = {7.0F, 6.0F, 9.0F, 12.5F};
		assertEquals(4, GTMachines.LOOM_ROWS.size(), "the four Loom rows (:1412-1415)");
		for (int i = 0; i < GTMachines.LOOM_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.LOOM_ROWS.get(i);
			assertEquals("loom" + (i == 0 ? "" : "_t" + (i + 1)), tRow.path());
			assertEquals(20211 + i, tRow.metaId());
			assertEquals(tSlugs[i], tRow.matSlug(), "the Kinetic_T word set");
			assertEquals(tHardness[i], tRow.hardness());
			assertSame(TD.Energy.RU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.RU");
			// THE cross-proof: the SAME GT6RecipeMaps.LOOM instance the W1 ElectricLoom rows
			// ride (GT6EuHuFamiliesRowTest pins the electric side of this identity)
			assertSame(GT6RecipeMaps.LOOM, tRow.recipes().get(), "the SHARED RM.Loom map — the 同图互证");
			assertEquals("loom", tRow.texture());
			assertEquals(1, tRow.parallel(), "no NBT_PARALLEL key");
			assertEquals(GTMachines.MACHINE_LOOM_UNIT_KEY, tRow.displayKey());
			// the :1412 masks — item top-in/bottom-out auto TOP/BOTTOM, BOTH side energy faces, zero tanks
			assertEquals((byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A), tRow.energySides(), "SBIT_L|SBIT_R — the ElectricLoom face");
			assertEquals(NO_KEY, tRow.fluidIn(), "zero tank keys → the 127 defaults");
			assertEquals(NO_KEY, tRow.fluidOut());
			assertEquals(UNDEFINED, tRow.fluidAutoIn());
			assertEquals(UNDEFINED, tRow.fluidAutoOut());
			assertEquals((byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A), tRow.itemIn());
			assertEquals((byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A), tRow.itemOut());
			assertEquals(1, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_TOP");
			assertEquals(0, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_BOTTOM");
		}
		// the kinetic rung RUNS the W1 loom.json smoke row (4x string → 1x white_wool,
		// eUt 16, duration 128) — the map drives BOTH carriers (the electric one at
		// efficiency 5000 is the W1 chain's live face)
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		GT6RecipeMaps.LOOM.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.STRING, 4)},
				new ItemStack[] {new ItemStack(Items.WHITE_WOOL, 1)},
				null, null, 128, 16, 0));
		TileEntityBasicMachine tMachine = rowMachine(GTMachines.LOOM_ROWS.get(0));
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.STRING, 4), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true),
				"the kinetic loom finds the SAME row the electric loom runs live");
		assertEquals(2048L, tMachine.mMaxProgress, "units(16 × 128 × 1, 10000, 10000, T) = 2048 — the 10000 identity (no NBT_EFFICIENCY key)");
		inject(tMachine, 32, 64);
		assertTrue(tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount()).getCount() >= 1,
				"the white wool landed (32 ticks @ 64/tick)");
	}

	// ------------------------------------------------------------------
	// the Bath P26 in-catalog map reuse (ACCEPTANCE ⑤)
	// ------------------------------------------------------------------

	@Test
	void bathEatsTheInCatalogBathMap() {
		// the P26 map is REUSED (never rebuilt): the base-RecipeMap instance — the
		// RecipeMapBath subclass arm is the pooled decisions.p29-w2-split-rulings cut
		assertSame(RecipeMap.class, GT6RecipeMaps.BATH.getClass(), "the base-RecipeMap form (the card-A BATH shape)");
		// the smoke-row shape drives the machine row (the "仅灌行" face; the LIVE p26
		// kitchen row walk is the p29_w2_bath chain's arm)
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		GT6RecipeMaps.BATH.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.WHITE_WOOL, 1)},
				new ItemStack[] {new ItemStack(Items.STRING, 4)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, null, 128, 16, 0));
		TileEntityBasicMachine tMachine = tuRowMachine(GTMachines.BATH_ROWS.get(0));
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.WHITE_WOOL, 1), false);
		tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true),
				"the TU bath machine binds the poured BATH row (1 TU package of 16 ≤ the window max)");
		assertEquals(2048L, tMachine.mMaxProgress, "units(16 × 128 × 1, 10000, 10000, T) = 2048 — the :770 TU arm keeps mMinEnergy = mEUt");
		inject(tMachine, 128, 16);
		assertTrue(tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount()).getCount() >= 4,
				"4x string out — the washed-wool smoke row completed");
	}

	// ------------------------------------------------------------------
	// the fixtures (the GT6EuHuFamiliesRowTest shape; the TU arm rides applyTuRow)
	// ------------------------------------------------------------------

	/** The BET-factory fixture: the machine() half + applyRow (the kineticMachine body). */
	private static TileEntityBasicMachine rowMachine(GTBasicMachineBlock.MachineRow aRow) {
		TileEntityBasicMachine tMachine = makeMachine(aRow.recipes().get(), aRow.parallel(), aRow.parallelDuration(), aRow.energyType());
		long[] tInputs = GTMachines.TIER_INPUTS[aRow.tier()]; // the machine() factory assignment (the kineticMachine body)
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return GTMachines.applyRow(tMachine, aRow);
	}

	/** The TU BET-factory fixture: the machine() half + applyTuRow (the tuMachine body — the {1,1,16} window + the NO_CONSTANT_POWER bind). */
	private static TileEntityBasicMachine tuRowMachine(GTBasicMachineBlock.MachineRow aRow) {
		TileEntityBasicMachine tMachine = makeMachine(aRow.recipes().get(), aRow.parallel(), aRow.parallelDuration(), aRow.energyType());
		long[] tInputs = GTMachines.TIER_INPUTS[aRow.tier()]; // the machine() half assigns tier 0 first
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return GTMachines.applyTuRow(tMachine, aRow); // then the {1,1,16} override + the mNoConstantEnergy bind
	}

	/** The direct-inject driver: one doInject + one dispatcher tick per iteration. */
	private static void inject(TileEntityBasicMachine aMachine, int aTicks, long aSize) {
		for (int i = 0; i < aTicks; i++) {
			aMachine.doInject(aMachine.mEnergyTypeAccepted, (byte)0, aSize, 1, true);
			aMachine.updateEntity();
		}
	}
}
