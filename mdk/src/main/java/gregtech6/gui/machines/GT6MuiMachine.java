package gregtech6.gui.machines;

import brachy.modularui.api.IUIHolder;
import brachy.modularui.factory.BlockEntityUIFactory;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The GT6 face of the ModularUI machine open chain (task p26-mui-a-open-chain — the P25
 * batch-A ruling "machines opened after this default to ModularUI"): the ACT full-chain
 * precedent (decisions.p24-act-be-form, the wiring gate PASSED on both legs) lifted onto
 * a reusable default interface. The BE implements this and fills {@link #buildUI} with its
 * panel factory; the block dispatches {@link #tryOpen} — the factory's own network carries
 * the open, no vanilla MenuType of ours is involved (the GTAdvancedCraftingTableBlock :91
 * shape).
 *
 * <p>{@link #createScreen} is the generic client face: every GT6 MUI machine wraps its
 * main panel into a plain {@link ModularScreen} exactly as {@code GTActMenu.createScreen}
 * (:68-70), so the interface defaults it and a per-BE override is unnecessary (the ACT BE
 * override :843-847 was the pre-default spelling of this exact body). {@link #buildUI}
 * stays abstract on purpose — the panel body is per-machine (the basic-machine family
 * delegates to {@link GTBasicMachineMUI#buildPanel}, the gui/machine seam riding
 * {@link GTBasicMachineMenu#hostOf}).
 */
public interface GT6MuiMachine extends IUIHolder<PosGuiData> {

	/**
	 * The generic client wrapper (the {@code GTActMenu.createScreen} :68-70 shape): the MUI
	 * main panel under the MUI mod id, no per-machine state — the default serves every
	 * implementor.
	 */
	@Override
	default ModularScreen createScreen(PosGuiData aData, ModularPanel<?> aMainPanel) {
		return new ModularScreen(brachy.modularui.ModularUI.MOD_ID, aMainPanel);
	}

	/**
	 * The open chain (the {@code GTAdvancedCraftingTableBlock} :91 shape): one factory call,
	 * the open rides {@code BlockEntityUIFactory}'s own network. The intersection bound is
	 * the {@link BlockEntityUIFactory#open} contract (a GUI holder that is also a
	 * {@link BlockEntity}) — call sites pass the concrete BE and the inference closes it.
	 */
	static <T extends BlockEntity & GT6MuiMachine> void tryOpen(ServerPlayer aPlayer, T aMachine) {
		BlockEntityUIFactory.INSTANCE.open(aPlayer, aMachine);
	}
}
