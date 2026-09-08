package gregtech6.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The formal small bending cylinder — task p25-food-can-row0 spec ②, the GT6FileItem form
 * with the census OFF. Upstream the tool is a crafting-domain meta id
 * ({@code ToolsGT.BENDING_CYLINDER_SMALL = 56}, CS.java:1736) mounted by the
 * Loader_Tools.java:146 registration row (display name "Small Bending Cylinder" verbatim)
 * over {@code GT_Tool_BendingCylinderSmall} (gregtech/items/tools/crafting/); the oredict
 * crafting key is {@code OreDictToolNames.bendingcylindersmall = "craftingToolBendingCylinderSmall"}
 * (CS.java:1903) — the snake translation ruling names the ingredient tag
 * {@code #gt6:tools/bending_cylinder_small} (GT6ItemTags, the TOOLS_FILE/TOOLS_SAW shape).
 *
 * <p>The crafting-loss face: upstream the in-grid cylinder rides the container-item channel
 * (MultiItemTool.getContainerItem :532-540) at
 * {@code getToolDamagePerContainerCraft() = 25} (GT_Tool_BendingCylinderSmall.java:44-46);
 * the port flattens that channel onto the {@link GT6FileItem#craftRemaining} static seam —
 * {@link #getCraftingRemainingItem} delegates to it VERBATIM (the seam is item-agnostic,
 * the zero-change generalization the p24 ruling built the seam for).
 * {@link #DAMAGE_PER_CRAFT} is ONE vanilla point per craft on the single 512-point steel
 * tier (decisions.p24-tool-system-damage-mapping, the file/saw declared deviation carried
 * forward; the has/get PAIRING is the id410 iron law — {@code hasCraftingRemainingItem}
 * constant {@code true} keeps the Recipe.getRemainingItems gate open for the whole tool
 * life, the wear-out consumption lives in the GET face).
 *
 * <p>Classification: the item performs NO {@link net.minecraftforge.common.ToolAction} at
 * all (card spec ② — the upstream bending cylinder is a pure CRAFTING tool: zero
 * isMinableBlock (:59-60 returns false verbatim), zero Behavior_Tool machine face, so the
 * census finds nothing to port). No {@code canPerformAction} override here — the vanilla
 * default (false for every action) IS the declared surface, and the same default keeps the
 * three HOE_DIG wrench-substitute predicates blind (the p25-tool-hammer-wrench red line
 * shape, inherited for free).
 *
 * <p>Registration form: the file/saw row shape — {@code Item.Properties().durability(
 * DURABILITY_POINTS)}, single steel tier 512 (the pinned family value; upstream scales per
 * material via {@code setMaterialAmount(3*U)}, Loader_Tools.java:146 — the ladder is the
 * standing pool cut). The crafting recipe is the {"sfh"/"III"} self-craft row
 * (Loader_Tools.java:313, GT6CraftingRecipes) — the row that CONSUMES the p25
 * hammer/file/saw trio through their own 's'/'f'/'h' keys (the live consumption chain).
 */
public class GT6BendingCylinderSmallItem extends Item {

	/** The vanilla durability points — single steel tier (the crowbar/file/saw pinned family value). */
	public static final int DURABILITY_POINTS = 512;

	/**
	 * The crafting-loss mapping — ONE vanilla point per craft (the shared
	 * {@link GT6FileItem#DAMAGE_PER_CRAFT} ruling; the upstream 25-unit
	 * GT_Tool_BendingCylinderSmall.java:44-46 row folds onto the single-tier axis, the
	 * declared deviation).
	 */
	public static final int DAMAGE_PER_CRAFT = GT6FileItem.DAMAGE_PER_CRAFT;

	public GT6BendingCylinderSmallItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The dispatch GATE — the has/get pairing iron law (S1 review id410), same shape and
	 * rationale as {@link GT6FileItem#hasCraftingRemainingItem}: without it the vanilla
	 * field test short-circuits the channel and every craft swallows the cylinder.
	 */
	@Override
	public boolean hasCraftingRemainingItem(ItemStack aStack) {
		return true;
	}

	/**
	 * The container-item channel (upstream MultiItemTool.getContainerItem :532-540, the
	 * GT_Tool_BendingCylinderSmall.java:44-46 25-unit row) — the file's seam at the shared
	 * one-point mapping, see {@link GT6FileItem#craftRemaining}.
	 */
	@Override
	public ItemStack getCraftingRemainingItem(ItemStack aStack) {
		return GT6FileItem.craftRemaining(aStack, DAMAGE_PER_CRAFT);
	}
}
