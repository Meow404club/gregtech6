package gregtech6.gui;

import gregtech6.emi.GT6EmiPlugin;
import gregtech6.jei.GT6JeiPlugin;
import gregtech6.recipes.RecipeMap;

/**
 * The viewer-jump router (task debt-jei-emi-batch4): routes the machine-GUI progress-bar
 * click ({@code GTBasicMachineScreen}, the modern counterpart of the upstream GT_RectHandler
 * click rect, NEI_RecipeMap.java:399-426) to whichever recipe viewer is installed. JEI has
 * priority (the user-base ruling, decisions.2026-09-26-debt-jei-emi-coverage ①); EMI is the
 * fallback (including the JEI-loaded-but-runtime-not-ready corner). Neither viewer → silent
 * no-op, the vanilla click handling continues.
 *
 * <p>The viewer plugin classes ({@link GT6JeiPlugin} / {@link GT6EmiPlugin}) load JEI/EMI
 * API on class-load, so every entry point is gated behind the ModList probe of the same
 * viewer id — the absent viewer's classes are never touched (the dormant-impl contract the
 * JEI/EMI plugins ride on a dedicated server). {@link #openRecipeMapPage(RecipeMap, boolean,
 * boolean)} is the offline-testable pure arm: the boolean overload below only feeds it the
 * live probe results.
 *
 * <p>Upstream note: the left/right click arms both opened the SAME NEI handler page
 * (NEI_RecipeMap.java:75-76 dual registration) — this port keeps the single destination,
 * so the router carries no use/recipe axis.
 */
public final class GTViewerJump {

	/** NEI's fixed hover label (NEI_RecipeMap.java:420 — the rect tooltip literal). */
	public static final String RECT_TOOLTIP_TEXT = "Recipes";

	private GTViewerJump() {}

	/** Any viewer present? — the tooltip gate (upstream canHandle implied the NEI plugin). */
	public static boolean canJumpToViewer() {
		return isLoaded("jei") || isLoaded("emi");
	}

	/** The live route: JEI first, EMI fallback (see the class doc). */
	public static boolean openRecipeMapPage(RecipeMap aMap) {
		return openRecipeMapPage(aMap, isLoaded("jei"), isLoaded("emi"));
	}

	/** The pure route arm — offline-testable decision table over the probe results (public for the census test). */
	public static boolean openRecipeMapPage(RecipeMap aMap, boolean aJeiLoaded, boolean aEmiLoaded) {
		if (aMap == null) return false;
		if (aJeiLoaded && GT6JeiPlugin.openRecipeMapPage(aMap)) return true;
		return aEmiLoaded && GT6EmiPlugin.openRecipeMapPage(aMap);
	}

	private static boolean isLoaded(String aModId) {
		//? if forge {
		return net.minecraftforge.fml.ModList.get().isLoaded(aModId);
		//?} else {
		/*return net.neoforged.fml.ModList.get().isLoaded(aModId);
		 *///?}
	}
}
