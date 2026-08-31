package gregtech6.items.tools;

import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

/**
 * The GT6 tool-action entry — task p9-tool-crowbar spec ①, the ADR
 * 2026-09-01-p9-tool-crowbar ① surface. Upstream classifies tools through the
 * {@code TOOL_*} string family (CS.java:1040, e.g. {@code TOOL_crowbar}) plus the
 * ToolsGT meta ids (CS.java:1734-1755, CROWBAR=20); this port flattens both layers
 * onto Forge {@link ToolAction}s — the id string stays {@code ICover.TOOL_CROWBAR}
 * ("crowbar") at the ICoverableTE dispatch seam while the stack-classification layer
 * becomes the action below (the "栈→id 分类器" seam the research card pinned).
 *
 * <p>RED LINE (card spec ①): the crowbar action is deliberately NOT an alias of
 * {@link ToolActions#HOE_DIG}. {@code canPerformAction(HOE_DIG)} is the wrench
 * substitute in exactly three live predicates — GTOvenBlock.use:109 (the shift
 * rotation), GTFluidPipeBlock.use:104 (the connection grid) and
 * GTWrenchHighlightListener:85 (the nine-cell overlay) — and a crowbar that also
 * classified as HOE_DIG would fire the wrench UI everywhere. The crowbar rides its
 * own action and enters the dismantle path through the reserved tool id only.
 */
public final class GT6ToolActions {

	/**
	 * The crowbar stack-classification action ("gt6_crowbar" — the gt6 prefix keeps it
	 * clear of the vanilla/Forge action namespace). Consumers: {@link GTCrowbarItem}
	 * exposes it, {@code ICoverableTE.onCoverToolClick} keys on the id the item feeds.
	 */
	public static final ToolAction CROWBAR = ToolAction.get("gt6_crowbar");

	private GT6ToolActions() {
	}
}
