package gregtech6.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;

/**
 * The formal GT6 hard hammer — task p25-tool-hammer-wrench spec ①, the GT6FileItem form.
 * Upstream the tool is a crafting-domain meta id ({@code ToolsGT.HARDHAMMER = 12},
 * CS.java:1736) mounted by the Loader_Tools.java:124 registration row (display name
 * "Hammer", the CS.java:1096 TOOL_LOCALISER row verbatim) over
 * {@code GT_Tool_HardHammer} (gregtech/items/tools/early/, GT_Tool_HardHammer.java:48);
 * the oredict crafting key is {@code OreDictToolNames.hammer = "craftingToolHardHammer"}
 * (CS.java:1890) — the snake translation ruling names the ingredient tag
 * {@code #gt6:tools/hard_hammer} (GT6ItemTags, decisions.p25-tool-hammer-wrench-rulings:
 * the "craftingTool" prefix strips and snakes, keeping the hard-hammer semantic against
 * the soft-hammer/electric-hammer ambiguity).
 *
 * <p>The crafting-loss face (the card's whole interaction arm): upstream the in-grid
 * hammer rides the container-item channel (MultiItemTool.getContainerItem :532-540) at
 * {@code getToolDamagePerContainerCraft() = 400} (GT_Tool_HardHammer.java:70); the port
 * flattens that channel onto the {@link GT6FileItem#craftRemaining} static seam —
 * {@link #getCraftingRemainingItem} delegates to it VERBATIM (the seam is item-agnostic:
 * stack in, damaged copy or {@link ItemStack#EMPTY} out), the zero-change generalization
 * the p24 ruling built the seam for. {@link #DAMAGE_PER_CRAFT} is ONE vanilla point per
 * craft on the single 512-point steel tier (decisions.p24-tool-system-damage-mapping,
 * the file/saw declared deviation carried forward; the has/get PAIRING is the id410
 * iron law — {@code hasCraftingRemainingItem} constant {@code true} keeps the
 * Recipe.getRemainingItems gate open for the whole tool life, the wear-out consumption
 * lives in the GET face).
 *
 * <p>Classification: the item performs {@link GT6ToolActions#HAMMER} and nothing else
 * (the file/saw red-line shape). Cut (card spec ⑥, zero code): the ENTIRE world arm —
 * the ore-crush convertBlockDrops (GT_Tool_HardHammer.java:94-119, the RM.Hammer
 * machine face), the mining surface (isMinableBlock :83-86) and the mob-spawner
 * speed-up (:89-91) — stays pooled with the world-interaction card; no {@code useOn},
 * no destroy-speed surface, no drop-conversion here. The attack face (getBaseDamage
 * :73 5.0F, the goom-effective doubling :62-66) is the cutter precedent cut (the tool
 * is not registered as a weapon here, no attribute map).
 *
 * <p>Registration form: the file/saw row shape — {@code Item.Properties().durability(
 * DURABILITY_POINTS)}, single steel tier 512 (the pinned family value; upstream scales
 * per material via {@code toolHeadHammer.mAmount}, the ladder is the standing pool cut).
 */
public class GTHammerItem extends Item {

	/** The vanilla durability points — single steel tier (the crowbar/file/saw pinned family value). */
	public static final int DURABILITY_POINTS = 512;

	/**
	 * The crafting-loss mapping — ONE vanilla point per craft (the shared
	 * {@link GT6FileItem#DAMAGE_PER_CRAFT} ruling; the upstream 400-unit row folds onto
	 * the single-tier axis, the declared deviation).
	 */
	public static final int DAMAGE_PER_CRAFT = GT6FileItem.DAMAGE_PER_CRAFT;

	public GTHammerItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The dispatch GATE — the has/get pairing iron law (S1 review id410), same shape and
	 * rationale as {@link GT6FileItem#hasCraftingRemainingItem}: without it the vanilla
	 * field test short-circuits the channel and every craft swallows the hammer.
	 */
	@Override
	public boolean hasCraftingRemainingItem(ItemStack aStack) {
		return true;
	}

	/**
	 * The container-item channel (upstream MultiItemTool.getContainerItem :532-540, the
	 * GT_Tool_HardHammer.java:70 400-unit row) — the file's seam at the shared one-point
	 * mapping, see {@link GT6FileItem#craftRemaining}.
	 */
	@Override
	public ItemStack getCraftingRemainingItem(ItemStack aStack) {
		return GT6FileItem.craftRemaining(aStack, DAMAGE_PER_CRAFT);
	}

	/** The stack-classification face (the file/saw static-seam shape). */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.HAMMER == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
