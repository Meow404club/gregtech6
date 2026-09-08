package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.widgets.slot.ItemSlot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The offline panel-factory gate (task p26-mui-a-panel-factory): {@link GTBasicMachineMUI}
 * builds the machine panel from a bare Host fake with no player, no screen and no registry
 * beyond the offline bootstrap (the GT6MenuInputSlotExpansionTest shape — the panel-build
 * face skips the player-inventory bind on the null-player fixture).
 *
 * <p>Four pinned faces:
 * <ul>
 * <li><b>the progressRatio three-state table</b> — the success flag → 1.0, the idle −1 →
 *     0.0, the running arm divides by PROGRESS_DONE with the units() round-up quantization
 *     (1/2 maps to 16384/32767, NOT 0.5 — the ceil is load-bearing);</li>
 * <li><b>the seat topology</b> — the Shredder/Crusher shape lands 13 content seats (1+12),
 *     the Lathe 3 (1+2), each bound to its GTItemStackHandler index through the
 *     SLOT_INPUT offset exactly like the vanilla menu;</li>
 * <li><b>the output-only face</b> — every output seat refuses insertion
 *     (mayPlace=false, the upstream setCanPut(F) :162) while the input accepts;</li>
 * <li><b>the zero-fluid-seat face</b> — the panel tree carries only item seats plus the
 *     progress bar, the slot groups are exactly the two content groups (no fluid group,
 *     no player group on the headless fixture) — the batch-B boundary held shut.</li>
 * </ul>
 */
class GT6BasicMachineMUIPanelTest extends GTRecipesOfflineTestBase {

	/**
	 * The offline FML dist shaping (the GTRecipesOfflineTestBase.bootVanillaOffline spirit —
	 * shape the launcher environment the game normally provides): the vendored MUI widget
	 * classes read {@code FMLEnvironment.dist} during their static init (RichText →
	 * FontRenderHelper → ModularUI.isClientThread), and a bare test JVM leaves that static
	 * final at null. SERVER selects the headless text branch (no Minecraft dereference);
	 * it must run BEFORE the first widget class initializes, hence @BeforeAll.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6BasicMachineMUIPanelTest.class.getClassLoader());
		java.lang.reflect.Field tDist = tFmlEnv.getDeclaredField("dist");
		sun.misc.Unsafe tUnsafe;
		java.lang.reflect.Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		tTheUnsafe.setAccessible(true);
		tUnsafe = (sun.misc.Unsafe) tTheUnsafe.get(null);
		if (tUnsafe.getObject(tFmlEnv, tUnsafe.staticFieldOffset(tDist)) == null) {
			tUnsafe.putObject(tFmlEnv, tUnsafe.staticFieldOffset(tDist),
					net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER);
		}
	}

	/** The parametrisable offline Host fake — the interface-default single input (the multiblock/fake shape). */
	private static class FakeHost implements GTBasicMachineMenu.Host {
		final GTItemStackHandler mInventory;
		boolean mSuccessful;
		long mProgress;
		long mMaxProgress;

		FakeHost(int aOutputs) {
			// SLOT_INPUT=0, so the inventory covers index 0 (input) .. aOutputs (the last output) with slack
			mInventory = new GTItemStackHandler(aOutputs + 16);
		}

		@Override public GTItemStackHandler getInventory() { return mInventory; }
		// getInputSlotCount is NOT overridden — the interface default 1 is exactly the face the
		// multiblock base and the older fakes serve (GT6MenuInputSlotExpansionTest.defaultHostKeepsTheSingleInputMenu)
		@Override public int getOutputSlotCount() { return 12; }
		@Override public boolean isSuccessful() { return mSuccessful; }
		@Override public long getProgress() { return mProgress; }
		@Override public long getMaxProgress() { return mMaxProgress; }
		@Override public String getGuiTexture() { return "gt6:textures/gui/machines/shredder"; }
	}

	/** A 12-output (shredder/crusher) fake. */
	private static FakeHost shredderHost() {
		return new FakeHost(12);
	}

	/** A 2-output (lathe) fake — only the output count differs. */
	private static final class LatheHost extends FakeHost {
		LatheHost() {
			super(2);
		}

		@Override public int getOutputSlotCount() { return 2; }
	}

	/** A fresh headless sync manager (no player → the panel builds without the inventory bind). */
	private static PanelSyncManager headlessSyncManager() {
		return new PanelSyncManager(new ModularSyncManager(false), true);
	}

	/** Every ItemSlot in the panel tree, in child order. */
	private static List<ItemSlot> itemSeats(ModularPanel<?> aPanel) {
		List<ItemSlot> tSeats = new ArrayList<>();
		collectSeats(aPanel, tSeats);
		return tSeats;
	}

	private static void collectSeats(IWidget aWidget, List<ItemSlot> aSink) {
		if (aWidget instanceof ItemSlot tSlot) {
			aSink.add(tSlot);
			return; // ItemSlot is a leaf
		}
		if (aWidget instanceof brachy.modularui.widget.ParentWidget<?> tParent) {
			for (IWidget tChild : tParent.getChildren()) {
				collectSeats(tChild, aSink);
			}
		}
	}

	/** Every widget in the panel tree (the zero-fluid-seat sweep). */
	private static List<IWidget> allWidgets(ModularPanel<?> aPanel) {
		List<IWidget> tAll = new ArrayList<>();
		collectAll(aPanel, tAll);
		return tAll;
	}

	private static void collectAll(IWidget aWidget, List<IWidget> aSink) {
		aSink.add(aWidget);
		if (aWidget instanceof brachy.modularui.widget.ParentWidget<?> tParent) {
			for (IWidget tChild : tParent.getChildren()) {
				collectAll(tChild, aSink);
			}
		}
	}

	/** A non-empty stack — insertion probing (bootstrap makes plain block items resolvable). */
	private static ItemStack probeStack() {
		return new ItemStack(Blocks.BRICKS.asItem());
	}

	// ---------------------------------------------------------------------------
	// the progressRatio three-state table
	// ---------------------------------------------------------------------------

	@Test
	void progressRatioMapsTheThreeStates() {
		FakeHost tHost = shredderHost();

		// state 1: the success flag → full (the PROGRESS_DONE sentinel, whatever the counters say)
		tHost.mSuccessful = true;
		tHost.mProgress = 0;
		tHost.mMaxProgress = 0;
		assertEquals(1.0D, GTBasicMachineMUI.progressRatio(tHost), "the success flag → 1.0");

		// state 2: idle (max 0 → the -1 sentinel) → empty
		tHost.mSuccessful = false;
		assertEquals(0.0D, GTBasicMachineMUI.progressRatio(tHost), "the -1 idle sentinel → 0.0");

		// state 3: running — the value divides by PROGRESS_DONE
		tHost.mMaxProgress = 100;
		tHost.mProgress = 0;
		assertEquals(0.0D, GTBasicMachineMUI.progressRatio(tHost), "zero progress → 0.0");

		tHost.mProgress = 100;
		assertEquals(1.0D, GTBasicMachineMUI.progressRatio(tHost), "completed → 1.0 (units saturates at PROGRESS_DONE)");

		// the units() ceil is load-bearing: 1/2 → ceil(16383.5) = 16384 → 16384/32767
		tHost.mProgress = 1;
		tHost.mMaxProgress = 2;
		assertEquals(16384.0D / GTBasicMachineMenu.PROGRESS_DONE, GTBasicMachineMUI.progressRatio(tHost),
				"half progress rides the round-up quantization, not 0.5 exactly");

		// 25/100 → ceil(8191.75) = 8192
		tHost.mProgress = 25;
		tHost.mMaxProgress = 100;
		assertEquals(8192.0D / GTBasicMachineMenu.PROGRESS_DONE, GTBasicMachineMUI.progressRatio(tHost),
				"quarter progress — the ceil table holds");
	}

	// ---------------------------------------------------------------------------
	// the seat topology — 13 (shredder/crusher) and 3 (lathe)
	// ---------------------------------------------------------------------------

	@Test
	void shredderShapeBuildsThirteenSeats() {
		FakeHost tHost = shredderHost();
		PanelSyncManager tSync = headlessSyncManager();
		ModularPanel<?> tPanel = GTBasicMachineMUI.buildPanel(tHost, tSync);

		List<ItemSlot> tSeats = itemSeats(tPanel);
		assertEquals(13, tSeats.size(), "1 input + 12 outputs = the shredder/crusher content shape");

		// the input rides inventory index SLOT_INPUT+0; the outputs SLOT_INPUT+1..12
		assertEquals(TileEntityBasicMachine.SLOT_INPUT, tSeats.get(0).getSlot().getSlotIndex(), "the input index");
		for (int i = 0; i < 12; i++) {
			assertEquals(TileEntityBasicMachine.SLOT_INPUT + 1 + i, tSeats.get(1 + i).getSlot().getSlotIndex(),
					"output " + i + " index");
		}
		// the sync value seam is registered (card 2's open chain rides the same key)
		assertNotNull(tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_PROGRESS), "the progress sync value");
	}

	@Test
	void latheShapeBuildsThreeSeats() {
		LatheHost tHost = new LatheHost();
		PanelSyncManager tSync = headlessSyncManager();
		ModularPanel<?> tPanel = GTBasicMachineMUI.buildPanel(tHost, tSync);

		List<ItemSlot> tSeats = itemSeats(tPanel);
		assertEquals(3, tSeats.size(), "1 input + 2 outputs = the lathe content shape");
		assertEquals(TileEntityBasicMachine.SLOT_INPUT, tSeats.get(0).getSlot().getSlotIndex(), "the input index");
		assertEquals(TileEntityBasicMachine.SLOT_INPUT + 1, tSeats.get(1).getSlot().getSlotIndex(), "output 0 index");
		assertEquals(TileEntityBasicMachine.SLOT_INPUT + 2, tSeats.get(2).getSlot().getSlotIndex(), "output 1 index");
	}

	// ---------------------------------------------------------------------------
	// the output-only face — mayPlace=false on every output seat
	// ---------------------------------------------------------------------------

	@Test
	void outputSeatsRefuseInsertionInputsAccept() {
		FakeHost tHost = shredderHost();
		ModularPanel<?> tPanel = GTBasicMachineMUI.buildPanel(tHost, headlessSyncManager());

		List<ItemSlot> tSeats = itemSeats(tPanel);
		assertEquals(13, tSeats.size());
		ItemStack tProbe = probeStack();

		assertTrue(tSeats.get(0).getSlot().mayPlace(tProbe), "the input seat accepts items");
		for (int i = 1; i < 13; i++) {
			assertFalse(tSeats.get(i).getSlot().mayPlace(tProbe), "output seat " + (i - 1) + " refuses insertion (setCanPut(F))");
		}
	}

	// ---------------------------------------------------------------------------
	// the zero-fluid-seat face — item seats + progress bar only, groups clean
	// ---------------------------------------------------------------------------

	@Test
	void panelCarriesNoFluidSeatsAndCleanGroups() {
		FakeHost tHost = shredderHost();
		PanelSyncManager tSync = headlessSyncManager();
		ModularPanel<?> tPanel = GTBasicMachineMUI.buildPanel(tHost, tSync);

		// the tree: the panel itself + 13 item seats + the progress widget — nothing else
		// (no fluid seat widget of any kind)
		List<IWidget> tAll = allWidgets(tPanel);
		long tItemSeats = tAll.stream().filter(w -> w instanceof ItemSlot).count();
		long tProgress = tAll.stream().filter(w -> w instanceof brachy.modularui.widgets.ProgressWidget).count();
		assertEquals(13, tItemSeats, "exactly the content seats");
		assertEquals(1, tProgress, "exactly the progress bar");
		assertEquals(15, tAll.size(), "panel + seats + progress only — no fluid widget, no player group on the headless fixture");

		// the slot groups are exactly the two content groups
		Set<String> tGroupNames = new java.util.HashSet<>();
		tSync.getSlotGroups().forEach(g -> tGroupNames.add(g.getName()));
		assertEquals(Set.of(GTBasicMachineMUI.GROUP_INPUTS, GTBasicMachineMUI.GROUP_OUTPUTS), tGroupNames,
				"no fluid group, no player group (the headless fixture)");

		// no widget is named after a fluid seat
		assertTrue(tAll.stream().map(IWidget::getName).filter(java.util.Objects::nonNull)
				.noneMatch(n -> n.toLowerCase(java.util.Locale.ROOT).contains("fluid")), "no fluid-named widget");

		// the background rides the Host GUI path (the same parse the vanilla screen blits)
		UITexture tBackground = assertInstanceOf(UITexture.class, tPanel.getBackground(), "the panel background is a texture");
		assertEquals(new ResourceLocation("gt6", "textures/gui/machines/shredder.png"), tBackground.location(),
				"the mGUIPath parse matches the vanilla screen face");
	}
}
