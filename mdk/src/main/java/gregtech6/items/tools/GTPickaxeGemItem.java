package gregtech6.items.tools;

/**
 * The formal GT6 gem pickaxe — item id {@code gt6:pickaxe_gem} (task p29-w5-t1-dig-six,
 * the single steel tier ruling d keeps the STEEL body; the gem identity rides the
 * recipe's diamond tip + the quarter durability). Upstream GT_Tool_PickaxeGem.java:28-30
 * is a two-line subclass of GT_Tool_Pickaxe:
 * <ul>
 * <li>{@code getMaxDurabilityMultiplier() / 4} (:29) → the flat durability
 *     {@link #DURABILITY_POINTS} = 512/4 = 128 (the multiplier through the P24 flat-item
 *     ruling — no material ladder to scale).</li>
 * <li>{@code getBrokenItem} → {@code toolHeadPickaxeGem of Empty} (:30) → CUT: the flat
 *     port has no tool-head item family to drop, the pickaxe breaks entirely (vanilla
 *     default; declared deviation — the head-drop revives with the tool-head family
 *     card).</li>
 * </ul>
 *
 * <p><b>The Silk-Touch question, closed by the source</b> (the card's open question ②):
 * GT_Tool_PickaxeGem.java:28-41 carries NO silk-touch semantic — the "Silk Touch usage
 * first" comment lives on the upstream RECIPE row (Loader_Tools.java:338, Amber
 * suggestion), not the tool; the gem pick's ONLY distinction is the 4x fragility (and
 * the upstream 0.5x durability HALF on the un-gemmed head). The harvest conversion face
 * of the family belongs to the SPADE's harvestableSpade loot arm. No enchantment
 * interaction exists upstream — the open question resolves with zero writeback.
 *
 * <p>Mining face, torch arm, attack damage 3.0F, 25-unit break/200-unit attack folding:
 * all inherited from {@link GTPickaxeItem} verbatim.
 */
public class GTPickaxeGemItem extends GTPickaxeItem {

	/** Upstream getMaxDurabilityMultiplier /4 (GT_Tool_PickaxeGem.java:29) — 512/4. */
	public static final int DURABILITY_POINTS = GTPickaxeItem.DURABILITY_POINTS / 4;

	public GTPickaxeGemItem(Properties aProperties) {
		super(aProperties);
	}
}
