package gregtech6.items.tools;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
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
	 * <p>Classification: the item performs {@link GT6ToolActions#HAMMER} and the
	 * prospector second arm {@link GT6ToolActions#PROSPECTOR} (task p30-pool-prospector —
	 * upstream GT_Tool_HardHammer.java:132-135 mounts TWO Behavior_Tool rows on this one
	 * tool; the GTClubItem multi-action classifier precedent). The file/saw red-line shape
	 * otherwise holds. Cut (the p25 card spec ⑥, still standing): the ENTIRE world
	 * BREAKING arm — the ore-crush convertBlockDrops (GT_Tool_HardHammer.java:94-119, the
	 * RM.Hammer machine face), the mining surface (isMinableBlock :83-86) and the
	 * mob-spawner speed-up (:89-91) — stays pooled with the world-interaction card; no
	 * destroy-speed surface, no drop-conversion here. The attack face (getBaseDamage
	 * :73 5.0F, the golem-effective doubling :62-66) is the cutter precedent cut (the tool
	 * is not registered as a weapon here, no attribute map).
	 *
	 * <p>The prospector world arm (task p30-pool-prospector): the {@link #useOn} direct
	 * dispatch into the {@link GT6Prospector#prospect} static seam — the crowbar
	 * single-source shape, shared verbatim with the {@code /gt6tool prospect} acceptance
	 * command. Upstream rode Behavior_Tool.onItemUseFirst (Behavior_Tool.java:56-68,
	 * server-first dispatch + the :61-62 sendchat + the :63 10000-units-to-mDamage
	 * payment); the port's one-point-per-success mapping and the claim-on-client form are
	 * the {@link GTCrowbarItem} precedents. No sneak gate (Behavior_Tool.java:60 passes
	 * sneaking through, ToolCompat never branches on it — card spec ⑧), so the arm fires
	 * on the plain click and lets the vanilla block use have everything it wants first
	 * (stone has no use of its own, the acceptance drives the seam live).
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

	/** The stack-classification face — HAMMER + the prospector second arm (the GTClubItem multi-action shape). */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.HAMMER == aToolAction || GT6ToolActions.PROSPECTOR == aToolAction;
	}

	/**
	 * The prospector world arm — the flattened Behavior_Tool.onItemUseFirst dispatch into
	 * the {@link GT6Prospector#prospect} single-source seam. The gate is the pure
	 * ore-or-prospectable state test (client-knowable, the crowbar claim shape): PASS on
	 * everything that answers nothing, claim on the client, the server side executes and
	 * pays.
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		if (!GT6Prospector.isOreBlock(tLevel.getBlockState(aContext.getClickedPos()))
				&& !GT6Prospector.prospectable(tLevel.getBlockState(aContext.getClickedPos()))) {
			return InteractionResult.PASS;
		}
		if (tLevel.isClientSide) {
			return InteractionResult.CONSUME; // claim, the server side executes
		}
		return GT6Prospector.prospect(aContext, null) > 0 ? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	/** The prospector tooltip key — the hammer carries the second behavior's CS.java:1154 row. */
	public static final String TOOLTIP_KEY_PROSPECTOR = "item.gt6.hammer.tooltip_prospector";

	/**
	 * The prospector tooltip line (the Behavior_Tool.java:76-78 additional-tooltip face —
	 * the GTClubItem hover shape; en verbatim CS.java:1154, the ZH ratchet +1).
	 */
	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, Level aLevel, java.util.List<net.minecraft.network.chat.Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
		super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
		aTooltip.add(net.minecraft.network.chat.Component.translatable(TOOLTIP_KEY_PROSPECTOR));
	}
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, java.util.List<net.minecraft.network.chat.Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
	//21.1: the hover signature carries the Item.TooltipContext (the GTClubItem fork).
		super.appendHoverText(aStack, aContext, aTooltip, aFlag);
		aTooltip.add(net.minecraft.network.chat.Component.translatable(TOOLTIP_KEY_PROSPECTOR));
	}
	*///?}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
