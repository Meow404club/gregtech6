package gregtech6.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;

/**
 * The formal GT6 file — task p24-tool-system spec ①/②. Upstream the tool is a
 * crafting-domain meta id ({@code GT_Tool_File}, gregtech/items/tools/crafting/):
 * {@code getToolDamagePerContainerCraft() = 400} (GT_Tool_File.java:47-49) is the
 * container-item channel MultiItemTool.getContainerItem serves
 * (MultiItemTool.java:532-540 — the tool rides along through vanilla crafting, paying
 * units per craft and returning {@code null} = consumed when worn out). The port
 * flattens that channel onto {@link #getCraftingRemainingItem} — the 1.20.1/1.21.1
 * signature pair is identical (forge IForgeItem.java:237 / NeoForge 21.1
 * IItemExtension.java:193, both ItemStack-sensitive extension defaults), so the
 * override is single-source with zero {@code //?}. The per-craft payment is the
 * declared mapping from decisions.p24-tool-system-damage-mapping: ONE vanilla point
 * per craft ({@link #DAMAGE_PER_CRAFT}) on the single 512-point steel tier — an
 * explicit deviation from the upstream 400/100-unit 4:1 file/saw ratio, which cannot
 * fold onto the single-tier axis without the upstream unit-durability scalar (the P9
 * crowbar "one vanilla point per 10000 upstream units" normalization shape). A craft
 * that would push the new damage PAST {@link #getMaxDamage maxDamage} hands back
 * {@link ItemStack#EMPTY} = the tool is consumed (the upstream worn-out {@code null}
 * return). The exact boundary (strictly greater) and the copy-then-damage order are
 * pinned by FileSawTest through the {@link #craftRemaining} static seam — the mod-Item
 * intrusive-holder wall keeps the instance unconstructible offline (the CutterTest
 * boot NOTE), so the seam carries the testable surface.
 *
 * <p>Classification: the item performs {@link GT6ToolActions#FILE} and nothing else
 * (the crowbar red-line shape). Cuts (card spec ①/⑥, zero code): the world arms —
 * upstream isMinableBlock (GT_Tool_File.java:66-70, iron bars/panes) and the :82-85
 * {@code iron_bars * 3} speed arm stay pooled with the file/saw interaction card — no
 * {@code useOn}, no destroy-speed surface here; the attack face (getBaseDamage :66-68
 * 1.5F) is the cutter precedent cut (the tool is not a weapon, no attribute map); the
 * crafting recipe (Loader_Tools tool-head rows) is pooled with the tool-family card —
 * the FILE here is forged by players only in the sense of being tab/give reachable
 * until that card.
 *
 * <p>Registration form: the crowbar row shape — {@code Item.Properties().durability(
 * DURABILITY_POINTS)}, single steel tier 512 (the pinned family value; upstream scales
 * per material, the ladder is the standing pool cut).
 */
public class GT6FileItem extends Item {

	/** The vanilla durability points — single steel tier (the crowbar/cutter pinned family value). */
	public static final int DURABILITY_POINTS = 512;

	/**
	 * The crafting-loss mapping (decisions.p24-tool-system-damage-mapping): one vanilla
	 * point per craft, both file and saw (the upstream 400/100 units fold onto the
	 * single-tier axis — declared deviation, see the class javadoc).
	 */
	public static final int DAMAGE_PER_CRAFT = 1;

	public GT6FileItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The dispatch GATE for the container-item channel — the review-mandated twin of
	 * {@link #getCraftingRemainingItem} (S1 review id410, the dead-gate fix): the real
	 * crafting loop keys on {@code stack.hasCraftingRemainingItem()} first
	 * (Recipe.getRemainingItems, the forge patch's ItemStack-sensitive shape), whose
	 * extension default funnels to the vanilla {@code Item.craftingRemainingItem} FIELD
	 * test (IForgeItem:253-256 → Item.java:244-246) — and the registration row
	 * {@code Properties().durability(512)} leaves that field null, so an un-paired get
	 * override would NEVER run and the tool would be swallowed whole by every craft.
	 * Constant {@code true} keeps the gate open for the whole tool life; the wear-out
	 * consumption lives in the GET face ({@link ItemStack#EMPTY} return = consumed),
	 * exactly the GTCEu IGTTool.java:530-549 has+get pairing.
	 */
	@Override
	public boolean hasCraftingRemainingItem(ItemStack aStack) {
		return true;
	}

	/**
	 * The container-item channel (upstream MultiItemTool.getContainerItem :532-540, the
	 * GT_Tool_File :47-49 400-unit row) — the tool follows the crafted stack, one point
	 * wearier, until a craft would push it past {@code maxDamage}, which consumes it.
	 */
	@Override
	public ItemStack getCraftingRemainingItem(ItemStack aStack) {
		return craftRemaining(aStack, DAMAGE_PER_CRAFT);
	}

	/**
	 * The single loss-computation seam — item-agnostic so the offline test can pin the
	 * boundary through vanilla stacks (the mod-Item wall), and the saw shares it
	 * verbatim ({@link GTSawItem}, the identical 1-point ruling). {@code aNewDamage >
	 * maxDamage} → {@link ItemStack#EMPTY} (consumed); otherwise a copy at the new
	 * damage — the original stack (the crafting-grid input) is never mutated.
	 *
	 * @return the damaged copy, or {@link ItemStack#EMPTY} when the tool is spent.
	 */
	public static ItemStack craftRemaining(ItemStack aStack, int aPerCraft) {
		int tNewDamage = aStack.getDamageValue() + aPerCraft;
		if (tNewDamage > aStack.getMaxDamage()) {
			return ItemStack.EMPTY;
		}
		ItemStack rCopy = aStack.copy();
		rCopy.setDamageValue(tNewDamage);
		return rCopy;
	}

	/** The stack-classification face (the cutter/crowbar static-seam shape). */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.FILE == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
