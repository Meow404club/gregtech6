package gregtech6.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;

/**
 * The formal GT6 saw — task p24-tool-system spec ①/② (the GT6FileItem form). Upstream
 * the tool carries the {@code TOOL_saw} behaviour ({@code Behavior_Tool(TOOL_saw,
 * SFX.MC_DIG_WOOD, getToolDamagePerContainerCraft(), …)}, GT_Tool_Saw.java:197) whose
 * container-craft row is {@code = 100} units (:65-67) — the port folds it onto the
 * SAME one-point-per-craft ruling as the file ({@link GT6FileItem#DAMAGE_PER_CRAFT},
 * decisions.p24-tool-system-damage-mapping; the 400/100 ratio is the declared
 * deviation) through the shared {@link GT6FileItem#craftRemaining} seam: worn past
 * {@code maxDamage} on the single 512-point tier → {@link ItemStack#EMPTY} = consumed.
 *
 * <p>Cuts (card spec ①/⑥, zero code): the world arms stay pooled with the file/saw
 * interaction card — the sapling/workbench placement behaviors
 * (GT_Tool_Saw.java:198-199), the wood-mining surface (isMinableBlock) and the bark
 * strip (the TOOL_saw tooltip channel) — no {@code useOn}, no mining face here; the
 * attack face (getBaseDamage :80-82 1.75F) is the cutter precedent cut; the crafting
 * recipe is pooled with the tool-family card.
 *
 * <p>MATERIAL LADDER (task p31-machine-ladder): the stack's {@code GT.ToolStats} identity
 * scales durability (the {@link GT6ToolLadder} j/100 points), the composed display name
 * ("Saw (Bronze)") and the head tint ride the same seam; the IDENTITY-LESS arm reproduces
 * Steel bit-exact (so the pre-ladder 512 constant IS the steel fallback).
 *
 * <p>Registration form: the crowbar row shape, single steel tier 512.
 */
public class GTSawItem extends Item implements GT6ToolLadder.LadderTool {

	/** The vanilla durability points — single steel tier (the crowbar/cutter pinned family value). */
	public static final int DURABILITY_POINTS = 512;

	/** The form durability multiplier (upstream ToolStats.java:71 default 1.0). */
	public static final float DURABILITY_MULTIPLIER = 1.0F;

	public GTSawItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The dispatch GATE — the review-mandated has/get pairing (S1 review id410), same
	 * shape and rationale as {@link GT6FileItem#hasCraftingRemainingItem}: without it the
	 * vanilla field test short-circuits the channel and every craft swallows the saw.
	 */
	@Override
	public boolean hasCraftingRemainingItem(ItemStack aStack) {
		return true;
	}

	/**
	 * The container-item channel (upstream GT_Tool_Saw.java:65-67 via the :197 behavior
	 * row) — the file's seam at the shared one-point mapping, see
	 * {@link GT6FileItem#craftRemaining}.
	 */
	@Override
	public ItemStack getCraftingRemainingItem(ItemStack aStack) {
		return GT6FileItem.craftRemaining(aStack, GT6FileItem.DAMAGE_PER_CRAFT);
	}

	/** The stack-classification face (the cutter/crowbar static-seam shape). */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.SAW == aToolAction;
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

	/** The composed display name — "Saw (Bronze)"; bare for identity-less stacks. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
	}
}
