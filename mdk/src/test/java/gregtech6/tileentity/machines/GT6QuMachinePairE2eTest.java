package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.items.GT6UsbSticks;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.maps.GT6RecipeMapReplicator;
import gregtech6.registry.GTMaterialItems;

/**
 * The qu-scanner-replicator machine e2e (the TileEntityBasicMachineRecipeTest form):
 * the QU pair driven through the full BE dispatcher — the scanner scan writes the USB
 * carrier, the replicator replicates from it. The windows are the row values
 * (TIER_INPUTS[2] = {256,512,1024} for the scanner T3, TIER_INPUTS[1] = {64,128,256} for
 * the replicator T2 rung); the matter-fluid and item-walk seams ride the synthetic
 * stand-ins (the resolver-injection posture of the recipes-package tests).
 */
public class GT6QuMachinePairE2eTest extends TileEntityBasicMachineOfflineTestBase {

	// the per-class Unsafe latch folded onto GTOfflineTestBase.registerItemFixture
	// (the identical assume/unlock/register("gt6", key)/relock walk — task
	// probeitem-latch-hygiene; this class rides the machines base tree, hence the
	// qualified calls below).

	static GT6UsbSticks.GT6UsbStickItem sStick;
	static MaterialPrefixItem sScannedGem;

	static GT6UsbSticks.GT6UsbStickItem stick() {
		if (sStick == null) {
			sStick = GTOfflineTestBase.registerItemFixture("fixture_qu_e2e_usb_stick_3",
					() -> new GT6UsbSticks.GT6UsbStickItem(new Item.Properties(), (byte)3));
		}
		return sStick;
	}

	static MaterialPrefixItem scannedGem() {
		if (sScannedGem == null) {
			sScannedGem = GTOfflineTestBase.registerItemFixture("fixture_qu_e2e_scanned_gem",
					() -> new MaterialPrefixItem(new Item.Properties(), OP.gem, MT.H));
		}
		return sScannedGem;
	}

	/** The production resolver bindings, captured BEFORE any injection (the static-seam swap-back rule). */
	private static final java.util.function.Function<String, net.minecraft.world.level.material.Fluid> sProdFluidResolver = GT6RecipeMapReplicator.sMatterFluidResolver;
	private static final java.util.function.BiFunction<gregapi.oredict.OreDictPrefix, gregapi.oredict.OreDictMaterial, net.minecraft.world.item.Item> sProdItemResolver = GT6RecipeMapReplicator.sMaterialItemResolver;

	@AfterEach
	void restoreResolvers() {
		GT6RecipeMapReplicator.sMatterFluidResolver = sProdFluidResolver;
		GT6RecipeMapReplicator.sMaterialItemResolver = sProdItemResolver;
	}

	/** The synthetic seams: water/lava for the matter legs, dust(H)→stick / ingot(Fe)→iron_ingot for the walk. */
	private static void injectStubs() {
		GT6RecipeMapReplicator.sMatterFluidResolver = aHalf -> aHalf.equals("charged") ? Fluids.WATER : Fluids.LAVA;
		GT6RecipeMapReplicator.sMaterialItemResolver = (aPrefix, aMaterial) -> {
			if (aMaterial == MT.H && aPrefix == OP.dust) return Items.STICK;
			if (aMaterial == MT.Iron && aPrefix == OP.ingot) return Items.IRON_INGOT;
			return null;
		};
	}

	/** The scanner chain: gem + stick in → the stick back WITH the scan data, both inputs consumed. */
	@Test
	public void scannerChainWritesTheUsbData() {
		Assumptions.assumeTrue(GTOfflineTestBase.itemLatchArmed());
		GTMaterialItems.initMaterials();
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SCANNER_MOLECULAR, 1, false, TD.Energy.QU);
		tMachine.mInputMin = 256;
		tMachine.mInput = 512;
		tMachine.mInputMax = 1024; // the TIER_INPUTS[2] window (the :1551 NBT_INPUT 512)
		tMachine.getInventory().insertItem(0, new ItemStack(scannedGem(), 1), false);
		tMachine.getInventory().insertItem(1, new ItemStack(stick(), 1), false);

		drive(tMachine, 540); // eUt 512 × duration 512 = 262144 progress = 512 ticks of mInputMax
		ItemStack tOutput = tMachine.getInventory().getStackInSlot(2); // 2 input slots, the output is slot 2
		assertTrue(tOutput.getItem() instanceof GT6UsbSticks.GT6UsbStickItem, "the stick came back as the output");
		assertEquals(GT6UsbSticks.TIER_SCANNER_WRITE, GT6UsbSticks.readTier(tOutput), "the tier-3 byte written");
		assertEquals(MT.H.mID, GT6UsbSticks.readMaterialId(tOutput), "the gt.replicator.data short = the scanned material id");
		assertTrue(tMachine.getInventory().getStackInSlot(0).isEmpty(), "the scanned item was consumed");
		assertTrue(tMachine.getInventory().getStackInSlot(1).isEmpty(), "the blank stick was consumed");
	}

	/** The replicator chain: data stick + charged matter in → the replicated material out, the stick retained. */
	@Test
	public void replicatorChainReplicatesFromTheStickData() {
		Assumptions.assumeTrue(GTOfflineTestBase.itemLatchArmed());
		GTMaterialItems.initMaterials();
		injectStubs();
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.REPLICATOR, 1, false, TD.Energy.QU);
		tMachine.mInputMin = 64;
		tMachine.mInput = 128;
		tMachine.mInputMax = 256; // the TIER_INPUTS[1] window — the hydrogen recipe eUt 256 lands exactly at max
		ItemStack tStick = new ItemStack(stick(), 1);
		GT6UsbSticks.writeMaterialData(tStick, MT.H);
		tMachine.getInventory().insertItem(0, tStick, false);
		assertEquals(1, tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE),
				"the charged-matter leg (1 mB = 1 proton) parked in input tank 0");

		drive(tMachine, 6); // duration 1 × eUt 256 = 256 progress = 1 tick of mInputMax
		ItemStack tOutput = tMachine.getInventory().getStackInSlot(3); // 3 input slots, the output is slot 3
		assertEquals(Items.STICK, tOutput.getItem(), "the replicated dust stand-in (the item walk over MT.H)");
		assertEquals(1, tOutput.getCount());
		assertFalse(tMachine.getInventory().getStackInSlot(0).isEmpty(), "THE DATA MEDIUM IS NEVER CONSUMED (the ST.amount(0, aUSB) face)");
		assertEquals(1, tMachine.getInventory().getStackInSlot(0).getCount(), "the stick rides at count 1");
		assertEquals(MT.H.mID, GT6UsbSticks.readMaterialId(tMachine.getInventory().getStackInSlot(0)), "the data survives the run");
		assertEquals(0, tMachine.mTanksInput[0].getFluidAmount(), "the matter leg was consumed");
		assertNotNull(GT6RecipeMaps.REPLICATOR, "the live map rode the whole chain");
	}

	/** The window gate: the synthesis refuses at a below-recipe rung (the anti-strand face). */
	@Test
	public void replicatorWindowGatesTheSynthesis() {
		Assumptions.assumeTrue(GTOfflineTestBase.itemLatchArmed());
		GTMaterialItems.initMaterials();
		injectStubs();
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.REPLICATOR, 1, false, TD.Energy.QU);
		tMachine.mInputMin = 16;
		tMachine.mInput = 32;
		tMachine.mInputMax = 64; // the T1 window — 64 < the hydrogen eUt 256 → the recipe is unreachable
		ItemStack tStick = new ItemStack(stick(), 1);
		GT6UsbSticks.writeMaterialData(tStick, MT.H);
		tMachine.getInventory().insertItem(0, tStick, false);
		tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

		// the p32 window gate (the anti-strand fix): the synthesis refuses BELOW its eUt —
		// the T1 rung (max 64 < 256) never sees the row, so no inputs can be eaten into a
		// starving process (the mMinEnergy > mInputMax doWork starvation face)
		assertEquals(TileEntityBasicMachine.DID_NOT_FIND_RECIPE, tMachine.checkRecipe(false, false),
				"the T1 rung cannot reach the hydrogen synthesis — the window gate refuses before any consume");
		assertTrue(tMachine.getInventory().getStackInSlot(0).getCount() == 1, "the refused run consumed nothing");
	}
}
