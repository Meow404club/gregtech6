package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.widgets.FluidDisplayWidget;
import brachy.modularui.widgets.slot.ItemSlot;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.fluid.FluidTankGT;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The offline panel-factory gate (task p26-mui-a-panel-factory): {@link GTBasicMachineMUI}
 * builds the machine panel from a bare Host fake with no player, no screen and no registry
 * beyond the offline bootstrap (the GT6MenuInputSlotExpansionTest shape — since issue #3 the
 * player-inventory widget rides the tree unconditionally, it binds by sync key with no
 * container/player needed at build).
 *
 * <p>Pinned faces:
 * <ul>
 * <li><b>the progressRatio three-state table</b> — the success flag → 1.0, the idle −1 →
 *     0.0, the running arm divides by PROGRESS_DONE with the units() round-up quantization
 *     (1/2 maps to 16384/32767, NOT 0.5 — the ceil is load-bearing);</li>
 * <li><b>the seat topology</b> — the Shredder/Crusher shape lands 13 content seats (1+12),
 *     the Lathe 3 (1+2), each bound to its GTItemStackHandler index through the
 *     SLOT_INPUT offset exactly like the vanilla menu;</li>
 * <li><b>the output-only face</b> — every output seat refuses insertion
 *     (mayPlace=false, the upstream setCanPut(F) :162) while the input accepts;</li>
 * <li><b>the fluid-seat face</b> (task p34) — a Host with declared banks renders one
 *     read-only seat per tank at the upstream :267/:268 geometry with the named sync keys
 *     {@code bm_fluid_in_<i>}/{@code bm_fluid_out_<i>}, amount text off, the drawn capacity
 *     clamped through bindInt; the open arm's server half (the BE buildUI delegation) rides
 *     the same seam on a real Drying-machine fixture;</li>
 * <li><b>the zero-fluid-seat face</b> — a default-bank Host (the interface default, the
 *     multiblock/fake shape) renders zero fluid seats — the pre-p34 panel byte-identical.</li>
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

	/**
	 * The synthetic fluid-banked Host (task p34 acceptance ①): the shredder item shape plus
	 * the declared banks 1 input (a 1000-capacity tank) + 2 output tanks (the default
	 * capacity — the bindInt clamp face).
	 */
	private static final class FluidBankHost extends FakeHost {
		final FluidTankGT mInTank = new FluidTankGT(1000);
		final FluidTankGT[] mOutTanks = {new FluidTankGT(), new FluidTankGT()};

		FluidBankHost() {
			super(12);
		}

		@Override public FluidTankGT[] getFluidInputTanks() { return new FluidTankGT[] {mInTank}; }
		@Override public FluidTankGT[] getFluidOutputTanks() { return mOutTanks; }
	}

	/** A fresh headless sync manager (the widget binds by sync key — no container/player needed at build). */
	private static PanelSyncManager headlessSyncManager() {
		return new PanelSyncManager(new ModularSyncManager(false), true);
	}

	/** The widget by its build name ({@code null} = absent — the GT6DistillationTowerMUIPanelTest form). */
	@javax.annotation.Nullable
	private static IWidget named(ModularPanel<?> aPanel, String aName) {
		return allWidgets(aPanel).stream().filter(w -> aName.equals(w.getName())).findFirst().orElse(null);
	}

	/** The widget build position — pos() lands in the resizer's start Unit (the Area only resolves at layout, which needs an initialized ResizeNode tree the headless build lacks); read the stored pixel value directly. */
	private static int posOf(IWidget aWidget, boolean aX) throws Exception {
		brachy.modularui.api.widget.IPositioned tPositioned = (brachy.modularui.api.widget.IPositioned) aWidget;
		java.lang.reflect.Field tAxis = tPositioned.resizer().getClass().getDeclaredField(aX ? "x" : "y");
		tAxis.setAccessible(true);
		Object tSizer = tAxis.get(tPositioned.resizer());
		java.lang.reflect.Field tStart = tSizer.getClass().getDeclaredField("start");
		tStart.setAccessible(true);
		Object tUnit = tStart.get(tSizer);
		return (int) ((brachy.modularui.widget.sizer.Unit) tUnit).getValue();
	}

	private static void assertPos(ModularPanel<?> aPanel, String aName, int aX, int aY, String aWhat) throws Exception {
		IWidget tWidget = named(aPanel, aName);
		assertNotNull(tWidget, aWhat + " present");
		assertEquals(aX, posOf(tWidget, true), aWhat + " x");
		assertEquals(aY, posOf(tWidget, false), aWhat + " y");
	}

	/** Every ItemSlot in the panel tree, in child order. */
	private static List<ItemSlot> itemSeats(ModularPanel<?> aPanel) {
		List<ItemSlot> tSeats = new ArrayList<>();
		collectSeats(aPanel, tSeats);
		return tSeats;
	}

	private static void collectSeats(IWidget aWidget, List<ItemSlot> aSink) {
		// the player-inventory group subtree is NOT content (issue #3: the widget is now
		// unconditional, its 36 seats ride their own "player_inventory" group name)
		if ("player_inventory".equals(aWidget.getName())) return;
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
		// the player-inventory widget rides the tree UNCONDITIONALLY (issue #3: the old
		// always-true headless gate skipped it in game)
		assertNotNull(named(tPanel, "player_inventory"), "the player inventory widget");
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
	// the fluid seats — the p34 Option B face: geometry, named sync keys, read-only form
	// ---------------------------------------------------------------------------

	@Test
	void fluidBanksRenderSeatsAtTheUpstreamGeometryWithNamedSyncKeys() throws Exception {
		FluidBankHost tHost = new FluidBankHost();
		PanelSyncManager tSync = headlessSyncManager();
		ModularPanel<?> tPanel = GTBasicMachineMUI.buildPanel(tHost, tSync);

		// the seat count — one read-only display seat per declared tank
		assertEquals(3, allWidgets(tPanel).stream().filter(w -> w instanceof FluidDisplayWidget).count(),
				"1 input + 2 output fluid seats (the declared banks)");

		// the upstream :267/:268 Slot_Render geometry (fluidDisplayPos): in[0] at (53,63),
		// the outputs ascending from (107,63) left-to-right
		assertPos(tPanel, "fluid_in_0", 53, 63, "fluid in 0");
		assertPos(tPanel, "fluid_out_0", 107, 63, "fluid out 0");
		assertPos(tPanel, "fluid_out_1", 125, 63, "fluid out 1");

		// the named sync keys — registered explicitly under the bm_fluid_ prefixes
		// (an unregistered widget-carried handler would land under the nameless WidgetTree
		// auto key, the tower form)
		assertNotNull(tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_FLUID_IN + 0), "bm_fluid_in_0 registered");
		assertNotNull(tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_FLUID_OUT + 0), "bm_fluid_out_0 registered");
		assertNotNull(tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_FLUID_OUT + 1), "bm_fluid_out_1 registered");
		// the bank sizes the keys exactly: no phantom seat beyond the declared tanks
		assertNotNull(named(tPanel, "fluid_in_0"), "seat fluid_in_0");
		assertNotNull(named(tPanel, "fluid_out_0"), "seat fluid_out_0");
		assertNotNull(named(tPanel, "fluid_out_1"), "seat fluid_out_1");

		// the read-only display form: amount text off (the upstream icon-only seat), the
		// drawn capacity clamped through bindInt (1000 stays; the Long.MAX_VALUE default
		// clamps to Integer.MAX_VALUE)
		FluidDisplayWidget tIn = (FluidDisplayWidget) named(tPanel, "fluid_in_0");
		assertFalse(tIn.isDisplayAmount(), "the fluid seats draw no amount text");
		assertEquals(1000, tIn.getCapacity(), "the 1000 tank capacity rides bindInt unchanged");
		for (int i = 0; i < 2; i++) {
			FluidDisplayWidget tOut = (FluidDisplayWidget) named(tPanel, "fluid_out_" + i);
			assertFalse(tOut.isDisplayAmount(), "output seat " + i + " draws no amount text");
			assertEquals(FluidTankGT.bindInt(tHost.mOutTanks[i].getCapacity()), tOut.getCapacity(),
					"output seat " + i + " capacity is the bindInt clamp of its tank");
		}
	}

	// ---------------------------------------------------------------------------
	// the open arm's server half — the BE buildUI delegation (the /gt6machine open face)
	// ---------------------------------------------------------------------------

	/** The offline machine BET (the GT6MachineFluidDisplayTest fixture form): a DRYING-map machine (fluids 1/3) with a null menu supplier. */
	private static BlockEntityType<TileEntityBasicMachine> sMachineType;

	private static final BlockPos POS = new BlockPos(100, 64, 100);

	@BeforeAll
	static void buildMachineFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityBasicMachine>[] tHolder = (BlockEntityType<TileEntityBasicMachine>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(tHolder[0], aPos, aState, GT6RecipeMaps.DRYING, 8, true, null),
				Blocks.BRICKS).build(null);
		sMachineType = tHolder[0];
	}

	@BeforeEach
	void initRecipeMaps() {
		GT6RecipeMaps.init();
	}

	@AfterEach
	void resetRecipeMaps() {
		GT6RecipeMaps.reset();
	}

	/**
	 * The open-arm pin (task p34, the coordinator-approved /gt6machine open subcommand): the
	 * command's verbatim dispatch face is the BE {@code buildUI} → {@link GTBasicMachineMUI#buildPanel}
	 * over {@link GTBasicMachineMenu#hostOf} — on a real fluid-banked machine (the Drying
	 * shape, the in-册 RCON target family form) it builds the panel and registers the named
	 * fluid keys, no Player needed (the headless manager gate).
	 */
	@Test
	void theOpenArmsServerHalfBuildsThePanelOnARealFluidBankedMachine() throws Exception {
		TileEntityBasicMachine tMachine = sMachineType.create(POS, Blocks.BRICKS.defaultBlockState());
		PanelSyncManager tSync = headlessSyncManager();

		ModularPanel<?> tPanel = tMachine.buildUI(null, tSync, new UISettings());
		assertNotNull(tPanel, "buildUI (the open arm's dispatch face) returns the panel");

		// the Drying banks: 1 input + 3 output tanks → 4 read-only seats with named keys
		assertEquals(4, allWidgets(tPanel).stream().filter(w -> w instanceof FluidDisplayWidget).count(),
				"1 + 3 fluid seats on the Drying shape");
		assertNotNull(tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_FLUID_IN + 0), "the named input key");
		assertNotNull(tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_FLUID_OUT + 2), "the named output key");
		// and the item content seats are untouched (1 in + 1 out on the Drying item shape)
		assertEquals(2, itemSeats(tPanel).size(),
				"the Drying item shape rides the shared panel unchanged");
	}

	// ---------------------------------------------------------------------------
	// the zero-fluid-seat face — item seats + progress bar only, groups clean
	// ---------------------------------------------------------------------------

	@Test
	void panelCarriesNoFluidSeatsAndCleanGroups() {
		FakeHost tHost = shredderHost();
		PanelSyncManager tSync = headlessSyncManager();
		ModularPanel<?> tPanel = GTBasicMachineMUI.buildPanel(tHost, tSync);

		// the tree: the panel itself + 13 content item seats + the progress widget + the
		// player-inventory group (1 group widget + its 36 seats, issue #3 unconditional).
		// The p34 face: the Host banks are the interface DEFAULT (empty) here, so zero fluid
		// seats render — the pre-p34 panel byte-identical (the zero-bank regression).
		List<IWidget> tAll = allWidgets(tPanel);
		long tItemSeats = itemSeats(tPanel).size();
		long tProgress = tAll.stream().filter(w -> w instanceof brachy.modularui.widgets.ProgressWidget).count();
		assertEquals(13, tItemSeats, "exactly the content seats");
		assertEquals(1, tProgress, "exactly the progress bar");
		assertEquals(52, tAll.size(), "panel + seats + progress + the player group (1+36, issue #3)");

		// the slot groups are exactly the two content groups — the player GROUP only
		// registers at construct (the fork auto-bind), not at build
		Set<String> tGroupNames = new java.util.HashSet<>();
		tSync.getSlotGroups().forEach(g -> tGroupNames.add(g.getName()));
		assertEquals(Set.of(GTBasicMachineMUI.GROUP_INPUTS, GTBasicMachineMUI.GROUP_OUTPUTS), tGroupNames,
				"no fluid group; the player group rides the construct-time auto-bind");

		// no widget is named after a fluid seat, and no bm_fluid sync key exists
		assertTrue(tAll.stream().map(IWidget::getName).filter(java.util.Objects::nonNull)
				.noneMatch(n -> n.toLowerCase(java.util.Locale.ROOT).contains("fluid")), "no fluid-named widget");
		assertEquals(null, tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_FLUID_IN + 0), "no input fluid key on the default banks");
		assertEquals(null, tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_FLUID_OUT + 0), "no output fluid key on the default banks");

		// the background rides the Host GUI path (the same parse the vanilla screen blits)
		UITexture tBackground = assertInstanceOf(UITexture.class, tPanel.getBackground(), "the panel background is a texture");
		assertEquals(new ResourceLocation("gt6", "textures/gui/machines/shredder.png"), tBackground.location(),
				"the mGUIPath parse matches the vanilla screen face");
	}
}
