package gregtech6.gui.machines;

import brachy.modularui.drawable.GuiTextures;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.drawable.progress.ProgressDrawable;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.value.sync.GenericSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.FluidDisplayWidget;
import brachy.modularui.widgets.ProgressWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

//? if forge {
import net.minecraftforge.fluids.FluidStack;
//?} else {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?}

import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The basic-machine ModularUI panel factory (task p26-mui-a-panel-factory, batch A card 1 —
 * a NEW file, zero existing file touched). The {@link GTActMenu} shape: the panel builds on
 * SERVER and CLIENT inside {@link #buildPanel(GTBasicMachineMenu.Host, PanelSyncManager)}
 * (the sync handlers register there, IUIHolder.java:21-44 buildUI contract) — card 2's
 * open-chain wires the BE through this exact seam, so the signature is pinned.
 *
 * <p>Panel = the standard 176x166 machine GUI with the background texture taken from
 * {@link Host#getGuiTexture()} (the mGUITexture = mRecipes.mGUIPath semantics,
 * MultiTileEntityBasicMachine.java:114) through {@link GTBasicMachineScreen#backgroundOf} —
 * the same parse the vanilla {@link GTBasicMachineScreen} blit rides, so the MUI panel shows
 * byte-identical backgrounds (shredder/crusher/lathe, GT6RecipeMaps.java:97/:105/:113).
 *
 * <p>Slot topology is the {@link GTBasicMachineMenu} STATIC GEOMETRY REFERENCED, never
 * copied: {@link GTBasicMachineMenu#inputSlotPos} (ContainerCommonBasicMachine.java:51-60,
 * the (53,25) single-input arm) and {@link GTBasicMachineMenu#outputGridPos}
 * (:169-270, the mOutputFluidCount==0 arms — Shredder/Crusher land on the 12 case
 * :247-269, the Lathe on the 2 case :178-181). Inputs are open {@link ModularSlot}s; the
 * outputs carry {@code canPut(false)} — the upstream Slot_Normal.setCanPut(F),
 * ContainerCommonBasicMachine.java:162 (items come out, nothing goes in).
 *
 * <p>Progress rides the MUI sync-value face: a {@link DoubleSyncValue} fed by
 * {@link #progressRatio} (the normalization wrapper over the three-state
 * {@link GTBasicMachineMenu#progressValue} — the RCON-shared function stays untouched),
 * consumed by a {@link ProgressWidget#value}; the bar visual is the vendored MUI default
 * arrow drawable ({@link GuiTextures#PROGRESS_ARROW}, modularui's own texture — zero new
 * art; the borrowed machine PNGs carry no baked-in arrow, assets/README.md:8-13).
 *
 * <p>Fluid seats (settled, task p34-gui-basicmachine-fluids — the Option B ruling that
 * redeemed the batch-A boundary declared here before): the two {@link Host} fluid banks
 * (the {@link GTBasicMachineMenu.Host#getFluidInputTanks}/{@link GTBasicMachineMenu.Host#getFluidOutputTanks}
 * default-empty seams, GTBasicMachineMenu.java:117/:122) render one read-only display seat
 * per declared tank at the upstream {@code Slot_Render} geometry
 * ({@link GTBasicMachineMenu#fluidDisplayPos}: inputs descending from (53,63) right-to-left,
 * outputs ascending from (107,63) left-to-right, both wrapping upward every 3). The seat
 * form is the {@link GTDistillationTowerMUI} shape (GenericSyncValue.forFluid with the
 * null→EMPTY gate, the {@link FluidTankGT#bindInt} drawn-capacity clamp, the amount text
 * off — the upstream seat is icon-only), registered under the named sync keys
 * {@code bm_fluid_in_<i>} / {@code bm_fluid_out_<i>}. A zero-fluid RecipeMap Host (the
 * default banks) renders zero seats — the pre-p34 panel byte-identical.
 *
 * <p>NOT in this card: the vanilla MenuType deregistration (card 3, done), BE/Block wiring
 * (card 2, done). The player-inventory bind is skipped when the sync manager has no
 * container menu — the offline panel-build fixture (headless tests, GT6MenuInputSlotExpansionTest
 * shape); at runtime both sides always have one, so the branch never fires in game.
 */
public final class GTBasicMachineMUI {

	/** The panel name (the MUI2 main-panel key). */
	public static final String PANEL_NAME = "basic_machine";

	/** The sync key of the progress ratio value (server getter = {@link #progressRatio}). */
	public static final String SYNC_PROGRESS = "bm_progress";

	/**
	 * The sync-key prefix of the input fluid seats: {@code bm_fluid_in_} + the tank index
	 * (task p34, one seat per {@link Host#getFluidInputTanks} element).
	 */
	public static final String SYNC_FLUID_IN = "bm_fluid_in_";

	/**
	 * The sync-key prefix of the output fluid seats: {@code bm_fluid_out_} + the tank index
	 * (task p34, one seat per {@link Host#getFluidOutputTanks} element).
	 */
	public static final String SYNC_FLUID_OUT = "bm_fluid_out_";

	/** The slot group of the input seat(s) — the shift-transfer source face. */
	public static final String GROUP_INPUTS = "bm_inputs";

	/** The slot group of the output grid — the shift-transfer destination face. */
	public static final String GROUP_OUTPUTS = "bm_outputs";

	/** The progress arrow seat: centered in the 36px gap between the input slot (right edge 71) and the output column (107), on the slot row (center y 34) — the GTBasicMachineScreen BAR_X/Y stand-in geometry. */
	private static final int PROGRESS_X = 79, PROGRESS_Y = 24, PROGRESS_SIZE = 20;

	private GTBasicMachineMUI() {
	}

	/**
	 * The panel body — runs on SERVER and CLIENT (the sync handlers must exist on both,
	 * the GTActMenu.buildPanel :73-105 shape). This is the pinned seam card 2 consumes.
	 */
	public static ModularPanel<?> buildPanel(GTBasicMachineMenu.Host aHost, PanelSyncManager aSyncManager) {
		GTItemStackHandler tInventory = aHost.getInventory();
		int tInputs = aHost.getInputSlotCount();
		int tOutputs = aHost.getOutputSlotCount();

		aSyncManager.registerSlotGroup(GROUP_INPUTS, Math.max(1, tInputs));
		aSyncManager.registerSlotGroup(GROUP_OUTPUTS, 3);
		// the headless fixture (offline panel-build tests) has no container menu — getPlayer()
		// would NPE on the missing menu, so the player face (bind + widget) is gated on it; at
		// runtime the container always exists on both sides, the branch never fires in game
		boolean tHeadless = aSyncManager.getContainer() == null;
		if (!tHeadless) {
			aSyncManager.bindPlayerInventory(aSyncManager.getPlayer()); // ACT :77
		}

		DoubleSyncValue tProgress = new DoubleSyncValue(() -> progressRatio(aHost));
		aSyncManager.syncValue(SYNC_PROGRESS, tProgress);

		ModularPanel<?> tPanel = ModularPanel.defaultPanel(PANEL_NAME, 176, 166)
				// the mGUIPath background — the same ResourceLocation parse the vanilla screen blits
				.background(UITexture.fullImage(GTBasicMachineScreen.backgroundOf(aHost)));

		// the input seat(s) — the GTBasicMachineMenu static geometry, referenced not copied
		for (int i = 0; i < tInputs; i++) {
			int[] tPos = GTBasicMachineMenu.inputSlotPos(i, tInputs);
			tPanel.child(new ItemSlot()
					.slot(new ModularSlot(tInventory, TileEntityBasicMachine.SLOT_INPUT + i).slotGroup(GROUP_INPUTS))
					.pos(tPos[0], tPos[1])
					.name("input_" + i));
		}
		// the output grid — canPut(false) = the upstream setCanPut(F), :162
		for (int i = 0; i < tOutputs; i++) {
			int[] tPos = GTBasicMachineMenu.outputGridPos(i, tOutputs);
			tPanel.child(new ItemSlot()
					.slot(new ModularSlot(tInventory, TileEntityBasicMachine.SLOT_INPUT + tInputs + i).canPut(false).slotGroup(GROUP_OUTPUTS))
					.pos(tPos[0], tPos[1])
					.name("output_" + i));
		}
		// the fluid display seats — the Host bank seams consumed live (the
		// GTBasicMachineMenu.java:117/:122 default-empty faces): one read-only seat per
		// declared tank at the upstream :267/:268 Slot_Render geometry (fluidDisplayPos);
		// a default-bank Host renders zero seats — the pre-p34 panel byte-identical
		FluidTankGT[] tInTanks = aHost.getFluidInputTanks();
		for (int i = 0; i < tInTanks.length; i++) {
			int[] tPos = GTBasicMachineMenu.fluidDisplayPos(false, i);
			tPanel.child(fluidSeat(aSyncManager, tInTanks[i], SYNC_FLUID_IN + i)
					.pos(tPos[0], tPos[1])
					.name("fluid_in_" + i));
		}
		FluidTankGT[] tOutTanks = aHost.getFluidOutputTanks();
		for (int i = 0; i < tOutTanks.length; i++) {
			int[] tPos = GTBasicMachineMenu.fluidDisplayPos(true, i);
			tPanel.child(fluidSeat(aSyncManager, tOutTanks[i], SYNC_FLUID_OUT + i)
					.pos(tPos[0], tPos[1])
					.name("fluid_out_" + i));
		}
		// the progress bar — the vendored MUI default arrow drawable, zero new art
		tPanel.child(new ProgressWidget()
				.value(tProgress)
				.texture(GuiTextures.PROGRESS_ARROW, ProgressDrawable.Direction.RIGHT)
				.pos(PROGRESS_X, PROGRESS_Y)
				.size(PROGRESS_SIZE, PROGRESS_SIZE));

		// the player inventory at the standard 176x166 machine-panel offset 84
		// (bindPlayerInventory(84) semantics: the 3x9 block at (7,84), the hotbar at (7,142))
		if (!tHeadless) {
			tPanel.child(SlotGroupWidget.playerInventory((aIndex, aSlot) -> aSlot).pos(7, 84));
		}
		return tPanel;
	}

	/**
	 * The progress ratio in [0, 1] — the MUI widget face over the three-state
	 * {@link GTBasicMachineMenu#progressValue} (ContainerCommonBasicMachine.java:280-286
	 * verbatim, the RCON-shared function stays untouched): the success flag
	 * ({@code PROGRESS_DONE}) maps to 1.0, the idle {@code -1} maps to 0.0, anything else
	 * divides down by {@code PROGRESS_DONE} (the exact fill arithmetic the vanilla
	 * GTBasicMachineScreen bar runs, GTBasicMachineScreen.java:59-61).
	 */
	public static double progressRatio(GTBasicMachineMenu.Host aHost) {
		int tValue = GTBasicMachineMenu.progressValue(aHost);
		if (tValue >= GTBasicMachineMenu.PROGRESS_DONE) return 1.0D;
		if (tValue < 0) return 0.0D;
		return (double) tValue / GTBasicMachineMenu.PROGRESS_DONE;
	}

	/**
	 * One read-only fluid seat — the {@link GTDistillationTowerMUI} seat form (:156-171)
	 * mirrored: that file is frozen for this card (zero diff), so the three reuse elements
	 * ride here instead of a shared helper — the {@link GenericSyncValue#forFluid} sync over
	 * the tank's live stack (null → EMPTY: the getter must not return null), the drawn
	 * capacity clamped through {@link FluidTankGT#bindInt}, the amount text off (the upstream
	 * Slot_Render seat is icon-only, ContainerClientBasicMachine draws no amount text).
	 *
	 * <p>Unlike the tower form the value is REGISTERED under its named sync key here (the
	 * {@code bm_fluid_in_<i>}/{@code bm_fluid_out_<i>} contract, task p34): an unregistered
	 * widget-carried handler would land under the
	 * {@code WidgetTree.collectSyncValues} auto key ({@code auto:<panel>} + a running id) —
	 * the same shape the {@link #SYNC_PROGRESS} registration already rides.
	 */
	private static FluidDisplayWidget fluidSeat(PanelSyncManager aSyncManager, FluidTankGT aTank, String aSyncKey) {
		// the leg-generic declaration (the tower :157-160 note): the forge factory returns
		// GenericSyncValue<FluidStack>, the neoforge one
		// GenericSyncValue<RegistryFriendlyByteBuf, FluidStack> — the same call shape, the
		// var keeps one body over both (FluidDisplayWidget.value takes the IValue face both
		// implement)
		var tValue = GenericSyncValue.forFluid(
				() -> {
					FluidStack tContent = aTank.fluid();
					return tContent == null ? FluidStack.EMPTY : tContent;
				},
				null);
		aSyncManager.syncValue(aSyncKey, tValue);
		return new FluidDisplayWidget()
				.value(tValue)
				.capacity(FluidTankGT.bindInt(aTank.getCapacity()))
				.displayAmount(false);
	}
}
