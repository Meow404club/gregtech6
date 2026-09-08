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
	 * The file stack-classification action ("gt6_file" — task p24-tool-system spec ①, the
	 * crowbar/cutter/chisel entry shape). Upstream rides the {@code TOOL_file} behaviour
	 * string (CS.java:1050, the GT_Tool_File tool row) and the {@code craftingToolFile}
	 * oredict key (CS.java:1867) as the crafting-tool ingredient face; the port flattens
	 * the classification onto this Forge {@link ToolAction} and keeps the string for the
	 * dispatch seam as {@link #FILE_ID}. Consumers: {@link GT6FileItem} (and the crafting
	 * ingredient route rides the {@code #gt6:tools/file} item tag, GT6ItemTags — the
	 * oredict-name snake translation ruling).
	 */
	public static final ToolAction FILE = ToolAction.get("gt6_file");

	/**
	 * The saw stack-classification action ("gt6_saw" — task p24-tool-system spec ①, the
	 * FILE entry shape). Upstream rides the {@code TOOL_saw} behaviour string
	 * (CS.java:1049, GT_Tool_Saw.java:197 {@code Behavior_Tool(TOOL_saw, …)}) and the
	 * {@code craftingToolSaw} oredict key (CS.java:1864); the port flattens the
	 * classification onto this Forge {@link ToolAction} and keeps the string for the
	 * dispatch seam as {@link #SAW_ID}. Consumer: {@link GTSawItem}. The upstream world
	 * arms (iron-bar mining / sapling-workbench placement, GT_Tool_Saw.java:184-186 and
	 * the isMinableBlock :135-144 surface) stay the interaction card's pool — zero
	 * {@code useOn} here by card cut.
	 */
	public static final ToolAction SAW = ToolAction.get("gt6_saw");

	/**
	 * The builder-wand stack-classification action ("gt6_builderwand" — task
	 * p24-builder-wand, the FILE/SAW entry shape). Upstream rides the
	 * {@code TOOL_builderwand} behaviour string (CS.java:1068, mounted by
	 * GT_Tool_Builderwand.onStatsAddedToTool :63 as
	 * {@code Behavior_Tool(TOOL_builderwand, SFX.MC_XP, 100, ...)}) and the
	 * {@code TOOL_builderwand} dispatch arm on the multiblock controllers
	 * (TileEntityBase10MultiBlockBase.java:130/:141); the port flattens the
	 * classification onto this Forge {@link ToolAction} and keeps the string for the
	 * dispatch seam as {@link #BUILDER_WAND_ID}. Consumers: {@link GT6BuilderWandItem}
	 * (and the tag face {@code #gt6:tools/builder_wand}, GT6ItemTags — the snake ruling;
	 * upstream carries no oredict crafting key, id402 proven, so the tag exists for the
	 * relay/code keying only).
	 */
	public static final ToolAction BUILDER_WAND = ToolAction.get("gt6_builderwand");

	/**
	 * The upstream {@code CS.TOOL_builderwand} dispatch id ("builderwand", CS.java:1068)
	 * — the reserved Behaviour_Tool tool-name string beside {@link #BUILDER_WAND} (the
	 * FILE_ID/SAW_ID shape), so a future IBlockToolable-style relay cannot drift from
	 * the upstream constant.
	 */
	public static final String BUILDER_WAND_ID = "builderwand";

	/**
	 * The screwdriver stack-classification action ("gt6_screwdriver" — task
	 * p24-screwdriver-item spec ①, the FILE/SAW entry shape). Upstream rides the
	 * {@code TOOL_screwdriver} behaviour string (CS.java:1057, the Loader_Tools.java:129
	 * registration row {@code new GT_Tool_Screwdriver() … , TOOL_screwdriver}) and the
	 * {@code craftingToolScrewdriver} oredict key (CS.java:1895) as the crafting-tool
	 * ingredient face; the port flattens the classification onto this Forge
	 * {@link ToolAction} and keeps the string for the dispatch seam as
	 * {@link #SCREWDRIVER_ID} (the {@code ICover.TOOL_SCREWDRIVER} reservation — the
	 * pump-cover direction toggle keys on the exact string, so the constant parity there
	 * is the zero-drift face; the item-to-id dispatch seam itself rides the machine
	 * interaction card). Consumer: {@link GT6ScrewdriverItem} (the crafting ingredient
	 * route rides the {@code #gt6:tools/screwdriver} item tag, GT6ItemTags — the
	 * oredict-name snake translation ruling). The upstream world arms (the
	 * {@code TOOL_screwdriver}-harvestable + Material.circuits surface,
	 * GT_Tool_Screwdriver.java:105-112) stay the machine interaction card's pool — zero
	 * {@code useOn} here by card cut.
	 */
	public static final ToolAction SCREWDRIVER = ToolAction.get("gt6_screwdriver");

	/**
	 * The hard-hammer stack-classification action ("gt6_hammer" — task
	 * p25-tool-hammer-wrench spec ②, the FILE/SAW entry shape). Upstream rides the
	 * {@code TOOL_hammer} behaviour string (CS.java:1051, the Loader_Tools.java:124
	 * registration row {@code new GT_Tool_HardHammer() … , TOOL_hammer}) and the
	 * {@code craftingToolHardHammer} oredict key (CS.java:1890) as the crafting-tool
	 * ingredient face; the port flattens the classification onto this Forge
	 * {@link ToolAction} and keeps the string for the dispatch seam as
	 * {@link #HAMMER_ID}. Consumer: {@link GTHammerItem} (the crafting ingredient route
	 * rides the {@code #gt6:tools/hard_hammer} item tag, GT6ItemTags — the hard_hammer
	 * naming ruling, decisions.p25-tool-hammer-wrench-rulings). The world arms (the
	 * ore-crush drop conversion + the mining surface, GT_Tool_HardHammer.java:83-119)
	 * stay the world-interaction card's pool — zero {@code useOn} here by card cut.
	 */
	public static final ToolAction HAMMER = ToolAction.get("gt6_hammer");

	/**
	 * The wrench stack-classification action ("gt6_wrench" — task p25-tool-hammer-wrench
	 * spec ②, the FILE/SAW entry shape). Upstream rides the {@code TOOL_wrench} behaviour
	 * string (CS.java:1038, the Loader_Tools.java:126 registration row
	 * {@code new GT_Tool_Wrench() … , TOOL_wrench}) and the {@code craftingToolWrench}
	 * oredict key (CS.java:1876) as the crafting-tool ingredient face; the port flattens
	 * the classification onto this Forge {@link ToolAction} and keeps the string for the
	 * dispatch seam as {@link #WRENCH_ID}. Consumer: {@link GTWrenchItem}. RED LINE
	 * (decisions.p25-tool-hammer-wrench-rulings ②): the item classifies on THIS action
	 * and NEVER on {@code ToolActions.HOE_DIG} — the three wrench-substitute predicates
	 * (GTOvenBlock.use:109 / GTFluidPipeBlock.use:104 / GTWrenchHighlightListener:85)
	 * stay HOE_DIG-keyed untouched, the whole {@code Behavior_Tool(TOOL_wrench, …)}
	 * interaction face (GT_Tool_Wrench.java:95) is the machine-interaction pool.
	 */
	public static final ToolAction WRENCH = ToolAction.get("gt6_wrench");

	/**
	 * The upstream {@code CS.TOOL_file} dispatch id ("file", CS.java:1050) — the reserved
	 * Behaviour_Tool tool-name string beside {@link #FILE} (the CHISEL_ID/CUTTER_ID
	 * shape), so a future IBlockToolable-style relay cannot drift from the upstream
	 * constant.
	 */
	public static final String FILE_ID = "file";

	/**
	 * The upstream {@code CS.TOOL_saw} dispatch id ("saw", CS.java:1049) — the reserved
	 * Behaviour_Tool tool-name string beside {@link #SAW} (the FILE_ID shape).
	 */
	public static final String SAW_ID = "saw";

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

	/**
	 * The upstream {@code CS.TOOL_screwdriver} dispatch id ("screwdriver", CS.java:1057)
	 * — the reserved Behaviour_Tool tool-name string beside {@link #SCREWDRIVER} (the
	 * FILE_ID shape). MUST stay identical to {@code ICover.TOOL_SCREWDRIVER}
	 * (ICover.java:70 — the pre-existing reservation; the pump cover's direction toggle
	 * keys on this exact string), which is why this card touches the constant only and
	 * leaves ICover.java at zero diff; the parity is pinned by the offline test.
	 */
	public static final String SCREWDRIVER_ID = "screwdriver";

	/**
	 * The upstream {@code CS.TOOL_hammer} dispatch id ("hammer", CS.java:1051) — the
	 * reserved Behaviour_Tool tool-name string beside {@link #HAMMER} (the FILE_ID/SAW_ID
	 * shape), so a future IBlockToolable-style relay cannot drift from the upstream
	 * constant.
	 */
	public static final String HAMMER_ID = "hammer";

	/**
	 * The upstream {@code CS.TOOL_wrench} dispatch id ("wrench", CS.java:1038) — the
	 * reserved Behaviour_Tool tool-name string beside {@link #WRENCH} (the HAMMER_ID
	 * shape).
	 */
	public static final String WRENCH_ID = "wrench";

	private GT6ToolActions() {
	}
}
