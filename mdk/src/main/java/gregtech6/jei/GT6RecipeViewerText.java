package gregtech6.jei;

import net.minecraft.network.chat.Component;

/**
 * The recipe-viewer-neutral text seam (task debt-emi-tier-b): the single home of the
 * structure-description lang key and its {@link Component} factory. Its consumer is the
 * shared preview widget's description line (task multiblock-preview-infra — the table
 * entry in {@link GT6MultiblockPreviews}; before that card both viewer plugins' text-info
 * pages consumed it). Before this class each plugin would have
 * carried its own copy of the key + construction (the card's copy-paste red line); the
 * holder keeps the reconciliation seam single — the key literal stays pinned by the
 * consumer-side tests (GT6JeiPluginTest.infoKeyPinnedLiteral / GT6EmiPluginTest) and the
 * producer side by the GT6EnUs provider test.
 *
 * <p>Deliberately vanilla-only imports (Component): the seam is loaded from viewer-linked
 * classes ({@code cokeOvenInfo()} is a method call, not an inlined constant), so the
 * holder must not drag either viewer's classes along — a JEI-only install reaches it
 * through GT6JeiPlugin, an EMI-only install through GT6EmiPlugin, and neither may
 * class-link a missing viewer.
 *
 * <p>The key literal keeps its historical {@code gt6.jei.*} path (data, not API): it is
 * already generated into the lang files and renaming it would orphan shipped translations
 * for zero gain.
 */
public final class GT6RecipeViewerText {

	/** The coke oven structure info-page lang key — the producer half lives in GT6EnUs. */
	public static final String INFO_KEY_COKE_OVEN = "gt6.jei.info.multiblock_coke_oven";

	/** The info-page body both viewers hang on the coke oven controller item. */
	public static Component cokeOvenInfo() {
		return Component.translatable(INFO_KEY_COKE_OVEN);
	}

	private GT6RecipeViewerText() { }
}
