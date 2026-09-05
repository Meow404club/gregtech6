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
 * becomes the action below (the "stack-to-id classifier" seam the research card pinned).
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

	/**
	 * The wire-cutter stack-classification action ("gt6_cutter" — task p10-tool-cutter
	 * spec ①). Upstream rides the {@code TOOL_cutter} behaviour string
	 * (GT_Tool_WireCutter.java:101 {@code Behavior_Tool(TOOL_cutter, …)}) while the wire
	 * block entities gate on {@code getFacingTool() == TOOL_cutter}
	 * (MultiTileEntityWireElectric.java:245); the port flattens the classification onto
	 * this Forge {@link ToolAction} and keeps the string for the dispatch seam as
	 * {@link #CUTTER_ID}. Consumer: {@link GTCutterItem}.
	 */
	public static final ToolAction CUTTER = ToolAction.get("gt6_cutter");

	/**
	 * The chisel stack-classification action ("gt6_chisel" — task p16-chisel-decalcify
	 * spec ①, the crowbar/cutter entry shape). Upstream rides the {@code TOOL_chisel}
	 * behaviour string (GT_Tool_Chisel.java:98 {@code Behavior_Tool(TOOL_chisel, …)}) and
	 * the boiler tank answers it on the onToolClick2 chain
	 * (MultiTileEntityBoilerTank.java:165-179); the port flattens the classification onto
	 * this Forge {@link ToolAction} and keeps the string for the dispatch seam as
	 * {@link #CHISEL_ID}. Consumer: {@link GTChiselItem}. The other upstream TOOL_chisel
	 * consumers (Basin/Mold/RailRoad/BlockStones...) stay the card's pool.
	 */
	public static final ToolAction CHISEL = ToolAction.get("gt6_chisel");

	/**
	 * The upstream {@code CS.TOOL_chisel} dispatch id ("chisel", CS.java:1060) — the
	 * Behaviour_Tool tool-name string the boiler's onToolClick2 arm keys on (:165); the
	 * port keeps it as the reserved dispatch id beside {@link #CHISEL} (the CUTTER_ID
	 * shape) so a future IBlockToolable-style relay cannot drift from the upstream
	 * constant.
	 */
	public static final String CHISEL_ID = "chisel";

	/**
	 * The upstream {@code CS.TOOL_cutter} dispatch id ("cutter", CS.java:1064) — the
	 * string the wire connection toggle and the cover tool relay key on. MUST stay
	 * identical to {@code CoverRedstoneEmitter.TOOL_CUTTER} (the strong-gate toggle is
	 * the live cover-side consumer of this exact string); the parity is pinned by the
	 * offline test while the emitter file itself stays zero-diff (card spec ①).
	 */
	public static final String CUTTER_ID = "cutter";

	private GT6ToolActions() {
	}
}
