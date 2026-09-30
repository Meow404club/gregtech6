package gregtech6.tooltip;

import net.minecraft.ChatFormatting;

/**
 * The upstream LH.Chat palette projected onto the modern {@link ChatFormatting} (task
 * tooltip-infra): the transcribed name keeps the upstream citation readable — a family
 * row table written {@code GT6TooltipStyle.ORANGE} reads exactly as the upstream row it
 * transcribes ({@code Chat.ORANGE}, gregapi/data/LH.java:692), while the value carries the
 * vanilla color the 1.20.1 {@code withStyle} face needs (ORANGE rode GOLD upstream, :692
 * — EnumChatFormatting.GOLD.toString()).
 *
 * <p>The mapping table is the upstream constant block VERBATIM (LH.java:685-710, the
 * EnumChatFormatting.toString() projections): CYAN→AQUA (:698), ORANGE/GOLD→GOLD (:692-693),
 * DRED→DARK_RED (:690), DGRAY→DARK_GRAY (:695), YELLOW→YELLOW (:701), WHITE→WHITE (:702),
 * GREEN→GREEN (:697), RED→RED (:699). {@link GT6TooltipsTest} pins each pair.
 *
 * <p>The semantic anchors the family row tables must respect (the upstream tooltip color
 * vocabulary the LH.addEnergyToolTips/addToolTipsEfficiency rows established): CYAN = the
 * specification header, GREEN = energy in, RED = energy out, ORANGE = requirements, DRED =
 * hazards, DGRAY = tool rows, YELLOW = efficiency, WHITE = the value slot inside a row.
 *
 * <p>ADR (design.r8-jade-tooltip): NO full LH constant class — the twelve family
 * vocabularies write their row keys straight into the family row-table constants (the
 * GTLightningRodBlock.Item inline precedent); a full LH port is a二期 move if the cover /
 * special-item tooltip faces ever need it.
 */
public final class GT6TooltipStyle {

	/** LH.Chat.CYAN (LH.java:698 → AQUA) — the specification header color. */
	public static final ChatFormatting CYAN = ChatFormatting.AQUA;
	/** LH.Chat.GREEN (:697) — energy in / the positive verdict. */
	public static final ChatFormatting GREEN = ChatFormatting.GREEN;
	/** LH.Chat.RED (:699) — energy out / the live failure. */
	public static final ChatFormatting RED = ChatFormatting.RED;
	/** LH.Chat.ORANGE (:692 → GOLD) — requirements and usage notes. */
	public static final ChatFormatting ORANGE = ChatFormatting.GOLD;
	/** LH.Chat.DRED (:690 → DARK_RED) — hazards (explosion, meltdown). */
	public static final ChatFormatting DRED = ChatFormatting.DARK_RED;
	/** LH.Chat.DGRAY (:695 → DARK_GRAY) — the tool rows. */
	public static final ChatFormatting DGRAY = ChatFormatting.DARK_GRAY;
	/** LH.Chat.YELLOW (:701) — efficiency rows. */
	public static final ChatFormatting YELLOW = ChatFormatting.YELLOW;
	/** LH.Chat.WHITE (:702) — the value slot inside a row. */
	public static final ChatFormatting WHITE = ChatFormatting.WHITE;

	private GT6TooltipStyle() {}
}
