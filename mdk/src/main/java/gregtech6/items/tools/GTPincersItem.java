package gregtech6.items.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraftforge.common.ToolAction;

/**
 * The formal pincers — item id {@code gt6:pincers} (task p29-w5-t3-machine-face-four spec
 * ④). Upstream meta id {@code ToolsGT.PINCERS = 66} (CS.java:1736), mounted by the
 * Loader_Tools.java:150 registration row (display name "Pincers", material amount
 * {@code U*2 + screw + 2*stick} — the recipe's scale) over {@code GT_Tool_Pincers}
 * (machine/GT_Tool_Pincers.java:34).
 *
 * <p>The dragon-egg material face: upstream {@code isMinableBlock} accepts
 * {@code Material.dragonEgg} (:108-110) beside the {@code TOOL_pincers} harvest tool, and
 * {@code canCollect() = true} (:105). In 1.20.1 the egg cannot be broken by hand AT ALL —
 * both {@code DragonEggBlock.attack} and {@code .use} teleport (:38/:32) — so the ported
 * face is the COLLECT arm: SNEAK right-click pops the egg through its own loot table
 * ({@code minecraft:blocks/dragon_egg} drops the egg unconditionally) and clears the block,
 * paying one durability point. The non-sneak click stays vanilla (the egg teleports — the
 * block side is zero-touch). The {@code TOOL_pincers}-harvestable break arm has no
 * 1.20.1-reachable block universe (no vanilla/GT6 block declares it) — the declared pool
 * cut. The machine-side wire-pulling face is the machine-interaction POOL (the RED LINE).
 *
 * <p>Sounds: the upstream click {@code SFX.MC_CLICK = "random.click"} (CS.java:2209) maps
 * to the in-world {@link SoundEvents#LEVER_CLICK} ("block.lever.click", the plain
 * SoundEvent both legs share); it plays on the collect. The crafting-loss face rides the
 * shared one-point mapping (the upstream :45-48 100-unit row folded).
 *
 * <p>MATERIAL LADDER (task p31-machine-ladder): the stack's {@code GT.ToolStats} identity
 * scales durability (the {@link GT6ToolLadder} j/100 points), the composed display name
 * ("Pincers (Bronze)") and the head tint ride the same seam; the IDENTITY-LESS arm
 * reproduces Steel bit-exact (the pre-ladder 512 constant IS the steel fallback).
 */
public class GTPincersItem extends Item implements GT6ToolLadder.LadderTool {

	/** The vanilla durability points — single steel tier 512 (the family value). */
	public static final int DURABILITY_POINTS = 512;

	/** One durability point per collect (the 100-unit click folded, the family mapping). */
	public static final int DAMAGE_PER_COLLECT = 1;

	/** The form durability multiplier (upstream ToolStats.java:71 default 1.0). */
	public static final float DURABILITY_MULTIPLIER = 1.0F;

	/** The stack-classification action — "gt6_pincers", the self-owned wave form. */
	public static final ToolAction ACTION = ToolAction.get("gt6_pincers");

	/** The upstream {@code CS.TOOL_pincers} dispatch id ("pincers", CS.java:1041). */
	public static final String ID = "pincers";

	public GTPincersItem(Properties aProperties) {
		super(aProperties);
	}

	/** The dispatch GATE — the has/get pairing iron law (the family shape). */
	@Override
	public boolean hasCraftingRemainingItem(ItemStack aStack) {
		return true;
	}

	/** The container-item channel — the shared one-point mapping (the upstream 100-unit row folded). */
	@Override
	public ItemStack getCraftingRemainingItem(ItemStack aStack) {
		return GT6FileItem.craftRemaining(aStack, GT6FileItem.DAMAGE_PER_CRAFT);
	}

	/** The stack-classification face (the family static-seam shape). */
	public static boolean classifies(ToolAction aToolAction) {
		return ACTION == aToolAction;
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

	/** The composed display name — "Pincers (Bronze)"; bare for identity-less stacks. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
	}

	/**
	 * The dragon-egg collect face: sneak right-click = the {@code canCollect} arm (the
	 * vanilla teleport owns the non-sneak click — {@code DragonEggBlock.use} consumes it
	 * before the item sees it, ServerPlayerGameMode.java:314-320). The collect runs
	 * server-side: the egg pops through its loot table, the point is paid.
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		if (!(tLevel.getBlockState(tPos).getBlock() instanceof DragonEggBlock)) {
			return InteractionResult.PASS;
		}
		Player tPlayer = aContext.getPlayer();
		if (tPlayer == null || !tPlayer.isSecondaryUseActive()) {
			return InteractionResult.PASS; // the vanilla teleport arm owns the non-sneak click
		}
		if (!tLevel.isClientSide) {
			tLevel.destroyBlock(tPos, true, tPlayer); // the loot table pops the egg (canCollect)
			ItemStack tStack = aContext.getItemInHand();
			if (tStack.getItem() instanceof GTPincersItem) {
				payCollect(tStack, tPlayer);
			}
			tLevel.playSound((Player) null, tPos, SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		return InteractionResult.sidedSuccess(tLevel.isClientSide);
	}

	/** The one-point payment (a null player — the acceptance channel — pays nothing). */
	private static void payCollect(ItemStack aStack, @Nullable Player aPlayer) {
		if (aPlayer != null) {
			aStack.hurtAndBreak(DAMAGE_PER_COLLECT, aPlayer, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
	}
}
