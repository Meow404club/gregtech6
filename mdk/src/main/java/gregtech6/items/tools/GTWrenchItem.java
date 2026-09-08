package gregtech6.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;

/**
 * The formal GT6 wrench — task p25-tool-hammer-wrench spec ①, the GT6FileItem form.
 * Upstream the tool is a crafting-domain meta id ({@code ToolsGT.WRENCH = 16},
 * CS.java:1736) mounted by the Loader_Tools.java:126 registration row (display name
 * "Wrench", the CS.java:1083 TOOL_LOCALISER row verbatim) over {@code GT_Tool_Wrench}
 * (gregtech/items/tools/machine/, GT_Tool_Wrench.java:46); the oredict crafting key is
 * {@code OreDictToolNames.wrench = "craftingToolWrench"} (CS.java:1876) — the snake
 * translation ruling names the ingredient tag {@code #gt6:tools/wrench} (GT6ItemTags).
 *
 * <p>The crafting-loss face: upstream the in-grid wrench rides the container-item
 * channel (MultiItemTool.getContainerItem :532-540) at
 * {@code getToolDamagePerContainerCraft() = 800} (GT_Tool_Wrench.java:59); the port
 * flattens that channel onto the {@link GT6FileItem#craftRemaining} static seam at the
 * shared one-point mapping (the same declared deviation as the hammer/file/saw trio,
 * decisions.p24-tool-system-damage-mapping), with the id410 has/get PAIRING iron law
 * ({@code hasCraftingRemainingItem} constant {@code true}).
 *
 * <p>Classification: the item performs {@link GT6ToolActions#WRENCH} and nothing else.
 * RED LINE (decisions.p25-tool-hammer-wrench-rulings ②, the WRENCH interaction pool
 * confirmation): ZERO {@code useOn} / world-interaction surface here — the upstream
 * {@code Behavior_Tool(TOOL_wrench, …)} arm (GT_Tool_Wrench.java:95) is the
 * machine-dismantle/rotation domain, a live POOL whose three vanilla-hoe substitute
 * predicates (GTOvenBlock.use:109 / GTFluidPipeBlock.use:104 /
 * GTWrenchHighlightListener:85) stay keyed on {@code ToolActions.HOE_DIG} untouched; a
 * wrench that classified as HOE_DIG would fire the wrench UI everywhere (the crowbar
 * card's regression wall, family-wide). The wrench enters recipes through the
 * {@code #gt6:tools/wrench} tag and nothing else until the interaction card lands.
 *
 * <p>Registration form: the file/saw row shape — {@code Item.Properties().durability(
 * DURABILITY_POINTS)}, single steel tier 512 (the pinned family value; upstream scales
 * per material via {@code 4*U}, Loader_Tools.java:126 — the ladder is the standing pool
 * cut).
 */
public class GTWrenchItem extends Item {

	/** The vanilla durability points — single steel tier (the crowbar/file/saw pinned family value). */
	public static final int DURABILITY_POINTS = 512;

	public GTWrenchItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The dispatch GATE — the has/get pairing iron law (S1 review id410), same shape and
	 * rationale as {@link GT6FileItem#hasCraftingRemainingItem}: without it the vanilla
	 * field test short-circuits the channel and every craft swallows the wrench.
	 */
	@Override
	public boolean hasCraftingRemainingItem(ItemStack aStack) {
		return true;
	}

	/**
	 * The container-item channel (upstream MultiItemTool.getContainerItem :532-540, the
	 * GT_Tool_Wrench.java:59 800-unit row) — the file's seam at the shared one-point
	 * mapping, see {@link GT6FileItem#craftRemaining}.
	 */
	@Override
	public ItemStack getCraftingRemainingItem(ItemStack aStack) {
		return GT6FileItem.craftRemaining(aStack, GT6FileItem.DAMAGE_PER_CRAFT);
	}

	/** The stack-classification face (the file/saw static-seam shape). */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.WRENCH == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
