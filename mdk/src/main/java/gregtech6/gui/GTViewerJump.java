package gregtech6.gui;

import javax.annotation.Nullable;

import gregtech6.emi.GT6EmiPlugin;
import gregtech6.jei.GT6JeiPlugin;
import gregtech6.recipes.RecipeMap;

/**
 * The viewer-jump router (task debt-jei-emi-batch4): routes the machine-GUI progress-bar
 * click ({@code GTBasicMachineScreen}, the modern counterpart of the upstream GT_RectHandler
 * click rect, NEI_RecipeMap.java:399-426) to whichever recipe viewer is installed. EMI has
 * priority (the r11-nei-corner-jump ruling, known_bugs.r11-batch2-render
 * .viewer_priority_emi — "同时装JEI和EMI显示EMI，因为EMI的优先级更高"; supersedes the old
 * JEI-priority ruling decisions.2026-09-26-debt-jei-emi-coverage ①), JEI is the fallback
 * (including the EMI-loaded-but-runtime-not-ready corner). Neither viewer → silent
 * no-op, the vanilla click handling continues.
 *
 * <p>The viewer plugin classes ({@link GT6JeiPlugin} / {@link GT6EmiPlugin}) load JEI/EMI
 * API on class-load, so every entry point is gated behind the ModList probe of the same
 * viewer id — the absent viewer's classes are never touched (the dormant-impl contract the
 * JEI/EMI plugins ride on a dedicated server). {@link #openRecipeMapPage(RecipeMap, boolean,
 * boolean)} is the offline-testable pure arm: the boolean overload below only feeds it the
 * live probe results. {@link #preferredViewer()} is the ONE predicate the jump and the
 * kitchen NEI corner glyph gate share — what you see is what you jump to.
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

	/**
	 * The ONE viewer decision — EMI first (see the class doc): {@code "emi"} when EMI is
	 * present, {@code "jei"} when only JEI is, {@code null} with neither. The kitchen NEI
	 * corner glyph picks its texture from the same answer (the GT6KitchenNeiModel gate).
	 */
	@Nullable
	public static String preferredViewer() {
		return preferredViewer(isLoaded("emi"), isLoaded("jei"));
	}

	/** The pure arm of the predicate — offline-testable decision table over the probe results (public for the census test). */
	public static String preferredViewer(boolean aEmiLoaded, boolean aJeiLoaded) {
		if (aEmiLoaded) return "emi";
		return aJeiLoaded ? "jei" : null;
	}

	/** The live route: EMI first, JEI fallback (see the class doc). */
	public static boolean openRecipeMapPage(RecipeMap aMap) {
		String tViewer = preferredViewer();
		return openRecipeMapPage(aMap, "jei".equals(tViewer), "emi".equals(tViewer));
	}

	/** The pure route arm — offline-testable decision table over the probe results (public for the census test). */
	public static boolean openRecipeMapPage(RecipeMap aMap, boolean aJeiLoaded, boolean aEmiLoaded) {
		if (aMap == null) return false;
		if (aEmiLoaded && GT6EmiPlugin.openRecipeMapPage(aMap)) return true;
		return aJeiLoaded && GT6JeiPlugin.openRecipeMapPage(aMap);
	}

	/**
	 * Runtime UI probe — KEEP (mdh-4 closeout): the division of labor with the unified
	 * mod-driver face is directional. GT6ModDrivers (mdh series) is the REGISTRATION
	 * driver (unknown domain defaults to present = register everything); this probe is
	 * the UI-side runtime query that needs a real absent-to-false answer (the JEI/EMI
	 * jump gates). Unifying the two must go through a pass-through query API — a separate
	 * card, not a migration.
	 */
	private static boolean isLoaded(String aModId) {
		//? if forge {
		return net.minecraftforge.fml.ModList.get().isLoaded(aModId);
		//?} else {
		/*return net.neoforged.fml.ModList.get().isLoaded(aModId);
		 *///?}
	}
}
