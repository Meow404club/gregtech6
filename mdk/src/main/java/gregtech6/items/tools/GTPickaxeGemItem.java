package gregtech6.items.tools;

/**
 * The formal GT6 gem pickaxe — item id {@code gt6:pickaxe_gem} (task p29-w5-t1-dig-six,
 * the STEEL body keeps the pre-ladder fallback; the gem identity rides the recipe's
 * diamond tip + the quarter durability). Upstream GT_Tool_PickaxeGem.java:28-30 is a
 * two-line subclass of GT_Tool_Pickaxe:
 * <ul>
 * <li>{@code getMaxDurabilityMultiplier() / 4} (:29) → {@link #DURABILITY_MULTIPLIER}
 *     0.25F — the ladder face (task p31-dig-ladder): an identity-less stack falls back
 *     to the steel stats at ×0.25 = the flat 128 (512/4, bit-exact the pre-ladder
 *     constant), an identity-carrying stack gets {@code mToolDurability * 100 * 0.25}.</li>
 * <li>{@code getBrokenItem} → {@code toolHeadPickaxeGem of Empty} (:30) → CUT: the port
 *     has no tool-head item family to drop, the pickaxe breaks entirely (vanilla
 *     default; declared deviation — the head-drop revives with the tool-head family
 *     card). The upstream gem-head row family (Loader_Tools.java:293-330) carries NO
 *     toolHeadPickaxeGem OreProcessing row, so the gem pick has NO per-material grid
 *     rows — its ladder face rides the diamond-tip steel row + the identity seam.</li>
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
 * <p>Mining face, torch arm, attack damage 3.0F, 25-unit break/200-unit attack folding,
 * speed ×1.0, base quality 0: all inherited from {@link GTPickaxeItem} verbatim.
 */
public class GTPickaxeGemItem extends GTPickaxeItem {

	/** The pre-ladder flat value (the identity-less fallback at ×0.25 = 512/4). */
	public static final int DURABILITY_POINTS = GTPickaxeItem.DURABILITY_POINTS / 4;

	/** Upstream getMaxDurabilityMultiplier /4 (GT_Tool_PickaxeGem.java:29) — the ladder form constant. */
	public static final float DURABILITY_MULTIPLIER = GTPickaxeItem.DURABILITY_MULTIPLIER / 4.0F;

	public GTPickaxeGemItem(Properties aProperties) {
		super(aProperties);
	}

	/** The quarter-durability re-point (the ONLY gem distinction upstream; the {@link GT6ToolLadder.LadderTool} face). */
	@Override
	public float durabilityMultiplier() {
		return DURABILITY_MULTIPLIER;
	}
}
