package gregtech6.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The formal rolling pin — item id {@code gt6:rolling_pin} (task p29-w5-t5-scene-six
 * spec ⑤, the {@link GT6BendingCylinderSmallItem} form with the census OFF — the
 * structure-empty master). Upstream the tool is a crafting-domain meta id
 * ({@code ToolsGT.ROLLING_PIN}, mounted at Loader_Tools.java:141, display name
 * "Rolling Pin" verbatim, {@code 2*U}) with the oredict crafting key
 * {@code OreDictToolNames.rollingpin} — the snake translation ruling names the
 * ingredient tag {@code #gt6:tools/rolling_pin} (GT6ItemTags, the p24 band shape).
 *
 * <p>Classification: the item performs NO
 * {@link net.minecraftforge.common.ToolAction} at all — the upstream
 * {@code GT_Tool_RollingPin} is a pure CRAFTING consumable (zero world arms; the dough/
 * clay flatten face is the recipe domain's, not a useOn), so the census is structurally
 * empty; no {@code canPerformAction} override exists. The crafting-loss face rides
 * {@link GT6FileItem#craftRemaining} at ONE vanilla point per craft (the shared mapping,
 * the upstream container-item channel folded). The recipe is the wood route
 * (Loader_Recipes_Woods.java:237, one tag-keyed row — the knife variant :238 and the
 * :250-253 metal/plastic ladder are the pool cuts).
 */
public class GTRollingPinItem extends Item {

	/** The vanilla durability points — single steel tier (the crowbar/file/saw pinned family value). */
	public static final int DURABILITY_POINTS = 512;

	/** The crafting-loss mapping — ONE vanilla point per craft (the shared ruling). */
	public static final int DAMAGE_PER_CRAFT = GT6FileItem.DAMAGE_PER_CRAFT;

	public GTRollingPinItem(Properties aProperties) {
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
