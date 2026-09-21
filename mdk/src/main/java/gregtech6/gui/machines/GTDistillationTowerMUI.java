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
import gregtech6.registry.GT6Distillation.TileEntityDistillationTower;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;

/**
 * The distillation-tower ModularUI panel factory (task p33-gui-distill-tower, Option A —
 * a NEW file; the shared {@link GTBasicMachineMUI}/{@link GT6MuiMachine} faces are only
 * consumed, never modified). The upstream contract is the generic machine container with
 * the tower's map shape (research.p32-r-distill-gui mapping table, ContainerCommonBasicMachine):
 * <ul>
 * <li>the input item seat case1 :55 → (53, 7);</li>
 * <li>the output item seats case3 :168-171 → (107/125/143, 7) with setCanPut(F);</li>
 * <li>the fluid display seats :267/:268 → the input tank at (53,63) and the NINE-tank
 *     output bank as a 3x3 grid (x 107/125/143, y 63/45/27 — the bank reads bottom-up);</li>
 * <li>the progress bar :277-288 three-state + the arrow at (78,24) 20x18
 *     (ContainerClientBasicMachine.java:57);</li>
 * <li>the background mGUITexture = mRecipes.mGUIPath (:114) — the borrowed tower PNG pair
 *     (assets/README.md, sha256-verified byte-identical).</li>
 * </ul>
 *
 * <p>The tower has NO special circuit slot (plain RecipeMap getSpecialSlot → null,
 * Recipe.java:400-402) and the upstream fluid seats are pure DISPLAY (Slot_Render, no
 * fill/drain interaction) — {@link FluidDisplayWidget} stays read-only here, the amount
 * text off ({@code displayAmount(false)}) to match the upstream icon-only seat.
 *
 * <p>The progress rides the family's three-state translation: the tower BE is a
 * {@link TileEntityBase10MultiBlockMachine}, so it IS a {@link GTBasicMachineMenu.Host}
 * and {@link GTBasicMachineMUI#progressRatio} serves it directly (zero re-implementation).
 * Fluid sync rides {@link GenericSyncValue#forFluid} (the forge leg; the neoforge leg's
 * builder carries the same call shape — ByteBufAdapters.FLUID_STACK both ways), the tank
 * capacity clamps through {@link FluidTankGT#bindInt} (the int-crossing convention).
 */
public final class GTDistillationTowerMUI {

	/** The panel name (the MUI2 main-panel key). */
	public static final String PANEL_NAME = "distillation_tower";

	/** The sync key of the progress ratio value (server getter = the family progressRatio). */
	public static final String SYNC_PROGRESS = "dt_progress";

	/** The sync-key prefix of the fluid seats: {@code dt_fluid_in} + {@code dt_fluid_out_<i>}. */
	public static final String SYNC_FLUID_IN = "dt_fluid_in";
	public static final String SYNC_FLUID_OUT = "dt_fluid_out_";

	/** The slot group of the input seat — the shift-transfer source face. */
	public static final String GROUP_INPUTS = "dt_inputs";

	/** The slot group of the output row — the shift-transfer destination face. */
	public static final String GROUP_OUTPUTS = "dt_outputs";

	/** The upstream geometry (the mapping table, ContainerCommonBasicMachine :55/:168/:267/:268). */
	public static final int INPUT_X = 53, INPUT_Y = 7;
	public static final int OUTPUT_X = 107, OUTPUT_Y = 7, OUTPUT_PITCH = 18;
	public static final int FLUID_IN_X = 53, FLUID_IN_Y = 63;
	public static final int FLUID_OUT_X = 107, FLUID_OUT_Y = 63;
	/** The progress arrow — ContainerClientBasicMachine.java:57 (78,24) 20x18. */
	public static final int PROGRESS_X = 78, PROGRESS_Y = 24, PROGRESS_W = 20, PROGRESS_H = 18;

	/** The fluid seat cell size (the standard 18px display slot). */
	private static final int FLUID_SIZE = 18;

	private GTDistillationTowerMUI() {
	}

	/**
	 * The panel body — runs on SERVER and CLIENT (the sync handlers register here, the
	 * IUIHolder.buildUI contract). Consumed by the tower BE's {@code buildUI}.
	 */
	public static ModularPanel<?> buildPanel(TileEntityDistillationTower aTower, PanelSyncManager aSyncManager) {
		var tInventory = aTower.getInventory();
		int tInputs = aTower.getInputSlotCount(); // the map's mInputItemsCount — 1 on both tower rows
		int tOutputs = aTower.getOutputSlotCount(); // the map's mOutputItemsCount — 3 on both rows

		aSyncManager.registerSlotGroup(GROUP_INPUTS, Math.max(1, tInputs));
		aSyncManager.registerSlotGroup(GROUP_OUTPUTS, 3);
		// the headless fixture (offline panel-build tests) has no container menu — getPlayer()
		// would NPE on the missing menu, so the player face is gated on it (the
		// GTBasicMachineMUI :82-88 form); at runtime both sides always have one
		boolean tHeadless = aSyncManager.getContainer() == null;
		if (!tHeadless) {
			aSyncManager.bindPlayerInventory(aSyncManager.getPlayer());
		}

		// the progress — the family three-state ratio over the Host face the tower already is
		DoubleSyncValue tProgress = new DoubleSyncValue(() -> GTBasicMachineMUI.progressRatio(aTower));
		aSyncManager.syncValue(SYNC_PROGRESS, tProgress);

		ModularPanel<?> tPanel = ModularPanel.defaultPanel(PANEL_NAME, 176, 166)
				.background(UITexture.fullImage(GTBasicMachineScreen.backgroundOf(aTower)));

		// the input item seat — case1 :55 → (53,7)
		for (int i = 0; i < tInputs; i++) {
			tPanel.child(new ItemSlot()
					.slot(new ModularSlot(tInventory, TileEntityBase10MultiBlockMachine.SLOT_INPUT + i).slotGroup(GROUP_INPUTS))
					.pos(INPUT_X + i * OUTPUT_PITCH, INPUT_Y)
					.name("input_" + i));
		}
		// the output item row — case3 :168-171 → (107/125/143, 7), canPut(false)
		for (int i = 0; i < tOutputs && i < 3; i++) {
			tPanel.child(new ItemSlot()
					.slot(new ModularSlot(tInventory, TileEntityBase10MultiBlockMachine.SLOT_INPUT + tInputs + i).canPut(false).slotGroup(GROUP_OUTPUTS))
					.pos(OUTPUT_X + i * OUTPUT_PITCH, OUTPUT_Y)
					.name("output_" + i));
		}
		// the input fluid display — :267 → (53,63), read-only
		tPanel.child(fluidSeat(aTower.mTankInput, SYNC_FLUID_IN)
				.pos(FLUID_IN_X, FLUID_IN_Y).name("fluid_in"));
		// the output bank — :268 → nine seats, 3x3 reading bottom-up (y 63/45/27)
		for (int i = 0; i < TileEntityDistillationTower.OUTPUT_TANK_COUNT; i++) {
			tPanel.child(fluidSeat(aTower.mTanksOutput[i], SYNC_FLUID_OUT + i)
					.pos(FLUID_OUT_X + (i % 3) * OUTPUT_PITCH, FLUID_OUT_Y - (i / 3) * OUTPUT_PITCH)
					.name("fluid_out_" + i));
		}
		// the progress arrow — (78,24) 20x18, the vendored MUI default drawable, zero new art
		tPanel.child(new ProgressWidget()
				.value(tProgress)
				.texture(GuiTextures.PROGRESS_ARROW, ProgressDrawable.Direction.RIGHT)
				.pos(PROGRESS_X, PROGRESS_Y)
				.size(PROGRESS_W, PROGRESS_H));

		// the player inventory at the standard 176x166 machine-panel offset 84
		if (!tHeadless) {
			tPanel.child(SlotGroupWidget.playerInventory((aIndex, aSlot) -> aSlot).pos(7, 84));
		}
		return tPanel;
	}

	/**
	 * One read-only fluid seat: the {@link GenericSyncValue#forFluid} sync (the tank's live
	 * stack, EMPTY when null — the getter must not return null), the drawn capacity clamped
	 * through {@link FluidTankGT#bindInt}, the amount text off (the upstream display seat is
	 * icon-only, ContainerClientBasicMachine draws no liquid-level/amount text).
	 */
	private static FluidDisplayWidget fluidSeat(FluidTankGT aTank, String aSyncKey) {
		// the leg-generic declaration: the forge factory returns GenericSyncValue<FluidStack>,
		// the neoforge one GenericSyncValue<RegistryFriendlyByteBuf, FluidStack> — the same
		// call shape, the var keeps one body over both (FluidDisplayWidget.value takes the
		// IValue face both implement)
		var tValue = GenericSyncValue.forFluid(
				() -> {
					FluidStack tContent = aTank.fluid();
					return tContent == null ? FluidStack.EMPTY : tContent;
				},
				null);
		return new FluidDisplayWidget()
				.value(tValue)
				.capacity(FluidTankGT.bindInt(aTank.getCapacity()))
				.displayAmount(false);
	}
}
