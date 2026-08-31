package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import net.minecraftforge.common.extensions.IForgeMenuType;

import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.recipes.GT6RecipeMaps;

/**
 * Acceptance 4 (task p7-basicmachine-family ⑦): the data-driven slot shape — 1 input +
 * mOutputItemsCount outputs (12/12/2, RM.java:134/:135/:97), the P6 gated capability gate
 * (insert input-only / extract output-only, canInsertItem2 :549-554 / canExtractItem2
 * :556-559), and the menu layout derived from the RecipeMap (the upstream output grid
 * ContainerCommonBasicMachine.java:169-270) with the three-state progress ContainerData
 * (:279-297).
 */
public class TileEntityBasicMachineSlotTest extends TileEntityBasicMachineOfflineTestBase {

	@Test
	void inventorySizeIsDataDrivenFromTheRecipeMap() {
		assertEquals(13, makeMachine(GT6RecipeMaps.SHREDDER, 1, false).getInventory().getSlots(), "1 input + 12 outputs");
		assertEquals(13, makeMachine(GT6RecipeMaps.CRUSHER, 4, true).getInventory().getSlots(), "1 input + 12 outputs");
		assertEquals(3, makeMachine(GT6RecipeMaps.LATHE, 1, false).getInventory().getSlots(), "1 input + 2 outputs");
	}

	@Test
	void capabilityGateIsInsertInputExtractOutputs() {
		// ForgeCapabilities cannot class-init offline (the oven test-base seam note) — the two
		// gates are asserted through their direct methods; the getCapability routing is the P6
		// shape verbatim.
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		assertTrue(tMachine.canInsertItem2(0), ":549-554 — the input slot takes inserts");
		assertFalse(tMachine.canInsertItem2(1), "the output range rejects inserts");
		assertFalse(tMachine.canInsertItem2(12), "the output range rejects inserts");
		assertFalse(tMachine.canExtractItem2(0), "the input slot rejects extracts");
		assertTrue(tMachine.canExtractItem2(1), ":556-559 — the output range takes extracts");
		assertTrue(tMachine.canExtractItem2(12), "the last output slot takes extracts");
		assertFalse(tMachine.canExtractItem2(13), "out of range is not extractable");
	}

	@Test
	void menuSlotLayoutFollowsTheRecipeMap() {
		TileEntityBasicMachine tShredder = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		GTBasicMachineMenu tMenu = new GTBasicMachineMenu(menuType(), 0, clientInventory(), tShredder);
		assertEquals(13 + 36, tMenu.slots.size(), "1 input + 12 outputs + the 36 player slots");
		assertEquals(13, tMenu.contentSlotCount, "1 input + 12 outputs");

		// outputs reject placement (upstream setCanPut(F) :162)
		assertFalse(tMenu.slots.get(1).mayPlace(new ItemStack(Items.SAND)), "the first output slot is setCanPut(F)");
		assertFalse(tMenu.slots.get(12).mayPlace(new ItemStack(Items.SAND)), "the last output slot is setCanPut(F)");
		assertTrue(tMenu.slots.get(0).mayPlace(new ItemStack(Items.SAND)), "the input slot accepts");

		// the 12-output grid (the upstream default branch :247-269): 3 cols x 4 rows
		int[] tFirst = GTBasicMachineMenu.outputGridPos(0, 12);
		int[] tLast = GTBasicMachineMenu.outputGridPos(11, 12);
		assertEquals(107, tFirst[0]);
		assertEquals(7, tFirst[1]);
		assertEquals(143, tLast[0]);
		assertEquals(61, tLast[1]);

		// the 2-output row (the upstream case-2 branch :178-181)
		int[] tLathe0 = GTBasicMachineMenu.outputGridPos(0, 2);
		int[] tLathe1 = GTBasicMachineMenu.outputGridPos(1, 2);
		assertEquals(107, tLathe0[0]);
		assertEquals(25, tLathe0[1]);
		assertEquals(125, tLathe1[0]);
		assertEquals(25, tLathe1[1]);
	}

	@Test
	void menuProgressDataCarriesTheThreeStates() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		GTBasicMachineMenu tMenu = new GTBasicMachineMenu(menuType(), 0, clientInventory(), tMachine);

		assertEquals(-1, tMenu.computeProgressValue(), ":285 — idle");
		tMachine.mProgress = 64;
		tMachine.mMaxProgress = 256;
		assertEquals((int) TileEntityBasicMachine.units(64, 256, Short.MAX_VALUE, true), tMenu.computeProgressValue(), ":283 — normalized running");
		tMachine.mSuccessful = true;
		assertEquals(GTBasicMachineMenu.PROGRESS_DONE, tMenu.computeProgressValue(), ":281 — success flag wins");
	}

	private static MenuType<GTBasicMachineMenu> menuType() {
		// an unregistered MenuType is enough for direct menu construction offline
		return IForgeMenuType.create((aId, aInv, aData) -> null);
	}

	private static net.minecraft.world.entity.player.Inventory clientInventory() {
		return new net.minecraft.world.entity.player.Inventory(null);
	}
}
