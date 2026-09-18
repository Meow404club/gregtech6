package gregtech6.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;

/**
 * The formal GT6 screwdriver — task p24-screwdriver-item spec ①/② (the GT6FileItem
 * form). Upstream the tool is a crafting-domain meta id ({@code GT_Tool_Screwdriver},
 * gregtech/items/tools/machine/, registered Loader_Tools.java:129): its container-craft
 * row is {@code getToolDamagePerContainerCraft() = 400} (GT_Tool_Screwdriver.java:70-72),
 * the container-item channel MultiItemTool.getContainerItem serves
 * (MultiItemTool.java:532-550 — the tool rides along through vanilla crafting, paying
 * units per craft and returning {@code null} = consumed when worn out). The port folds
 * that channel onto the SAME shared seam as the file/saw pair
 * ({@link GT6FileItem#craftRemaining}) under the declared one-point-per-craft mapping
 * from decisions.p24-tool-system-damage-mapping: ONE vanilla point per craft
 * ({@link GT6FileItem#DAMAGE_PER_CRAFT}) on the single 512-point steel tier — an
 * explicit deviation from the upstream 400 units (this javadoc face; the class ratio
 * mapping ruling lives on the ADR). A craft that would push the new damage PAST
 * {@code maxDamage} hands back {@link ItemStack#EMPTY} = the tool is consumed.
 *
 * <p>The has/get PAIRED override is load-bearing here exactly as on the file/saw (the
 * S1 review id410 dead-gate lesson): the real crafting loop keys
 * {@code stack.hasCraftingRemainingItem()} first (Recipe.getRemainingItems, the
 * ItemStack-sensitive forge-patch shape) whose extension default funnels to the vanilla
 * {@code Item.craftingRemainingItem} FIELD test — an un-paired get override would never
 * run and every craft would swallow the tool. The {@code #gt6:tools/screwdriver}
 * ingredient face plus the {@link #hasCraftingRemainingItem}/{@link
 * #getCraftingRemainingItem} channel are both pinned through the REAL dispatch by
 * ScrewdriverTest's {@code Recipe.getRemainingItems} probe.
 *
 * <p>Classification: the item performs {@link GT6ToolActions#SCREWDRIVER} and nothing
 * else (the crowbar red-line shape). Cuts (card spec ①/⑥, zero code): the world arms —
 * the {@code TOOL_screwdriver}-harvestable + Material.circuits mining surface
 * (GT_Tool_Screwdriver.java:105-112) and the ~30 {@code IBlockToolable}-style machine
 * consumers — stay pooled with the machine interaction card; the attack face
 * (getBaseDamage :85-87 1.5F, spider-family 2x :52-57) and the LV electric variant
 * (GT_Tool_Screwdriver_LV.java:31) are the same pool; the crafting recipe (the
 * {@code 'hS'/'Sf'} tool-head rows, Loader_Tools.java:306) is pooled with the
 * tool-family card. ZERO {@code useOn} here by card cut.
 *
 * <p>MATERIAL LADDER (task p31-machine-ladder): the stack's {@code GT.ToolStats} identity
 * scales durability (the {@link GT6ToolLadder} j/100 points), the composed display name
 * ("Screwdriver (Bronze)") and the head tint ride the same seam; the IDENTITY-LESS arm
 * reproduces Steel bit-exact (so the pre-ladder 512 constant IS the steel fallback).
 *
 * <p>Registration form: the crowbar row shape — {@code Item.Properties().durability(
 * DURABILITY_POINTS)}, single steel tier 512 (the pinned family value; upstream scales
 * per material via {@code setMaterialAmount(toolHeadScrewdriver.mAmount)},
 * Loader_Tools.java:129).
 */
public class GT6ScrewdriverItem extends Item implements GT6ToolLadder.LadderTool {

	/** The vanilla durability points — single steel tier (the crowbar/cutter pinned family value). */
	public static final int DURABILITY_POINTS = 512;

	/** The form durability multiplier (upstream ToolStats.java:71 default 1.0). */
	public static final float DURABILITY_MULTIPLIER = 1.0F;

	public GT6ScrewdriverItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The dispatch GATE for the container-item channel — the review-mandated twin of
	 * {@link #getCraftingRemainingItem} (the id410 dead-gate lesson, the
	 * GT6FileItem/GTSawItem pairing): without it the vanilla field test short-circuits
	 * the channel and every craft swallows the screwdriver. Constant {@code true} keeps
	 * the gate open for the whole tool life; the wear-out consumption lives in the GET
	 * face ({@link ItemStack#EMPTY} return = consumed), the GTCEu IGTTool.java:530-549
	 * has+get pairing.
	 */
	@Override
	public boolean hasCraftingRemainingItem(ItemStack aStack) {
		return true;
	}

	/**
	 * The container-item channel (upstream GT_Tool_Screwdriver.java:70-72, the 400-unit
	 * craft row) — the file/saw seam at the shared one-point mapping, see
	 * {@link GT6FileItem#craftRemaining}.
	 */
	@Override
	public ItemStack getCraftingRemainingItem(ItemStack aStack) {
		return GT6FileItem.craftRemaining(aStack, GT6FileItem.DAMAGE_PER_CRAFT);
	}

	/** The stack-classification face (the cutter/crowbar static-seam shape). */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.SCREWDRIVER == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}

	// ------------------------------ the GT6ToolLadder identity faces (task p31-machine-ladder) ------------------------------

	/** The per-material durability (the {@link GT6ToolLadder} j/100 points — Steel fallback = 512). */
	@Override
	public int getMaxDamage(ItemStack aStack) {
		return GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(aStack, durabilityMultiplier()));
	}

	/** The form durability multiplier (ToolStats.java:71 default 1.0). */
	@Override
	public float durabilityMultiplier() {
		return DURABILITY_MULTIPLIER;
	}

	/** The runtime tint (the head pass, the material mRGBaSolid with the steel fallback). */
	public static int tintARGB(ItemStack aStack, int aTintIndex) {
		return GT6ToolLadder.tintARGB(aStack, aTintIndex);
	}

	/** The composed display name — "Screwdriver (Bronze)"; bare for identity-less stacks. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
	}
}
