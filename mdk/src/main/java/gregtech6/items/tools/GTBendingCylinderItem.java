package gregtech6.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The formal LARGE bending cylinder — item id {@code gt6:bending_cylinder} (task
 * p29-w5-t5-scene-six spec ⑥, the {@link GT6BendingCylinderSmallItem} form with the
 * census OFF — the structure-empty master; the Small size landed with p25-food-can-row0,
 * this card adds only the missing large form). Upstream Loader_Tools.java:145, display
 * name "Bending Cylinder" verbatim, {@code 6*U}, oredict key
 * {@code OreDictToolNames.bendingcylinder} — the snake translation ruling names the
 * ingredient tag {@code #gt6:tools/bending_cylinder} (GT6ItemTags, the p24 band shape).
 *
 * <p>Classification: the item performs NO
 * {@link net.minecraftforge.common.ToolAction} at all — the upstream
 * {@code GT_Tool_BendingCylinder} is a pure CRAFTING consumable (zero world arms; the
 * plate→curved-plate work is the machine domain's), so the census is structurally empty.
 * The crafting-loss face rides {@link GT6FileItem#craftRemaining} at ONE vanilla point
 * per craft. The recipe is the {"sfh"/"III"/"III"} self-craft row (Loader_Tools.java:312
 * — the Small row :313 with ONE more ingot row, the 6*U amount made literal).
 */
public class GTBendingCylinderItem extends Item {

	/** The vanilla durability points — single steel tier (the crowbar/file/saw pinned family value). */
	public static final int DURABILITY_POINTS = 512;

	/** The crafting-loss mapping — ONE vanilla point per craft (the shared ruling). */
	public static final int DAMAGE_PER_CRAFT = GT6FileItem.DAMAGE_PER_CRAFT;

	public GTBendingCylinderItem(Properties aProperties) {
		super(aProperties);
	}

	/** The dispatch GATE — the has/get pairing iron law (S1 review id410, the SmallItem form). */
	@Override
	public boolean hasCraftingRemainingItem(ItemStack aStack) {
		return true;
	}

	/** The container-item channel — the file's seam at the shared one-point mapping. */
	@Override
	public ItemStack getCraftingRemainingItem(ItemStack aStack) {
		return GT6FileItem.craftRemaining(aStack, DAMAGE_PER_CRAFT);
	}
}
