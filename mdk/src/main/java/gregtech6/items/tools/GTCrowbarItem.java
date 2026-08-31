package gregtech6.items.tools;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.ToolAction;

import gregtech6.covers.ICover;
import gregtech6.covers.ICoverableTE;

/**
 * The formal GT6 crowbar — task p9-tool-crowbar spec ②, the ADR
 * 2026-09-01-p9-tool-crowbar ①/⑦ surface. Upstream rides
 * {@code Behavior_Tool(TOOL_crowbar, SFX.MC_BREAK, 100, ...)} mounted by
 * GT_Tool_Crowbar.onStatsAddedToTool :151-154, whose Behavior_Tool.onItemUseFirst
 * (:57-68) broadcasts IBlockToolable.Util.onToolClick (:73-80) to the block entity.
 *
 * <p>Declared port-ism (ADR ⑦): 1.20.1 has no per-item pre-use hook, so the broadcast
 * flattens to a {@link #useOn} direct dispatch — the target BE gets
 * {@link ICoverableTE#onCoverToolClick} with the reserved {@link ICover#TOOL_CROWBAR}
 * id, whose :246-247 OR gate (TOOL_CROWBAR id ∥ the legacy hoe substitute) was left
 * in place by the P4 first port (zero migration, ICover/ICoverableTE untouched).
 * Because 1.20.1 runs the host block's own {@code use} BEFORE {@code useOn},
 * GUI-owning hosts consume the click first — upstream used onItemUseFirst precisely
 * to pre-empt that; the authoritative driver here is the {@code /gt6tool dismantle}
 * acceptance command (the same {@link #crowbarToolClick} dispatch surface), while
 * hosts whose block use PASSes stay reachable in-game.
 *
 * <p>Classification red line: the item performs {@link GT6ToolActions#CROWBAR} and
 * nothing else — never {@code ToolActions.HOE_DIG} (the three wrench-substitute
 * predicates GTOvenBlock.use:109 / GTFluidPipeBlock.use:104 /
 * GTWrenchHighlightListener:85 must not see the crowbar, card spec ①).
 *
 * <p>Numbers (ADR ⑦, card spec ⑤): the upstream tool damage is internal units with
 * 10000 = one durability point (Behavior_Tool.doDamage units(…,10000,100) :63), so
 * the :151 return {@code 10000} maps to a single {@link ItemStack#hurtAndBreak} of 1
 * here; the per-block-break 50 / per-attack 200 figures (GT_Tool_Crowbar :48-50/:70-72)
 * fold into that single point (declared deviation). Durability 512, single steel tier
 * (upstream scales by material, Loader_Tools:128 — the ladder is a pool cut). Attack
 * damage 2.0 kept (getBaseDamage :75-77); the canBlock/isWeapon blocking semantics are
 * cut (1.20.1 has no item-blocking mechanic, ADR ①). The rails/circuits mining
 * extension and the Unboxinator openableCrowbar conversion (:108-139) stay pooled.
 */
public class GTCrowbarItem extends Item {

	/** The tool damage the ICoverableTE dispatch returns for a successful dismantle (upstream :151). */
	public static final long TOOL_DAMAGE_PER_DISMANTLE = 10000;

	/** The vanilla durability points — declared deviation (upstream material-scaled, single steel tier here). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :75-77 — 2.0F kept verbatim as the main-hand attribute. */
	private static final float ATTACK_DAMAGE = 2.0F;

	private final Multimap<Attribute, AttributeModifier> mAttackModifiers = ImmutableMultimap.of(
			Attributes.ATTACK_DAMAGE,
			new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", (double) ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION));

	public GTCrowbarItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The flattened Behavior_Tool.onItemUseFirst — direct dispatch into the covered
	 * host. PASS on everything that is not an ICoverableTE; claims on the client only
	 * when the target is one (the client cannot know whether the face carries a cover,
	 * so the claim mirrors the HoeItem sided pattern and the server decides PASS).
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		if (!(aContext.getLevel().getBlockEntity(aContext.getClickedPos()) instanceof ICoverableTE)) {
			return InteractionResult.PASS;
		}
		if (aContext.getLevel().isClientSide) {
			return InteractionResult.SUCCESS; // claim, the server side executes
		}
		return crowbarToolClick(aContext) > 0 ? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	/**
	 * The single dispatch + payment surface, shared by {@link #useOn} and the
	 * {@code /gt6tool dismantle} acceptance command (single-source semantics). Runs the
	 * covered-host tool click with the reserved crowbar id, then pays one durability
	 * point per 10000 upstream units (the declared mapping) on a non-null player.
	 *
	 * @return the upstream tool damage (10000 per dismantle) or 0.
	 */
	public static long crowbarToolClick(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		if (!(tLevel.getBlockEntity(tPos) instanceof ICoverableTE tHost)) return 0;
		return crowbarToolClick(tHost, aContext.getPlayer(), aContext.getItemInHand(),
				(byte) aContext.getClickedFace().get3DDataValue(), aContext.isSecondaryUseActive());
	}

	/** The dispatch half without the context (the command and the offline doubles). */
	public static long crowbarToolClick(ICoverableTE aHost, @Nullable Player aPlayer, ItemStack aStack, byte aSide, boolean aSneaking) {
		long tDamage = aHost.onCoverToolClick(ICover.TOOL_CROWBAR, aPlayer, aStack, aSide, aSneaking);
		if (tDamage > 0 && aPlayer != null) {
			aStack.hurtAndBreak(1, aPlayer, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND)); // 10000 units → 1 point
		}
		return tDamage;
	}

	/** Upstream getToolDamagePerEntityAttack :70-72 — 200 units fold into one point (declared deviation). */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot aSlot) {
		return aSlot == EquipmentSlot.MAINHAND ? mAttackModifiers : super.getDefaultAttributeModifiers(aSlot);
	}

	/**
	 * The stack classifier — the ONLY action this item performs is
	 * {@link GT6ToolActions#CROWBAR}. Never HOE_DIG: the three wrench-substitute
	 * predicates classify on it (the card's 扳手 UI 回归 red line), and the ICoverableTE
	 * :246-247 OR gate would fire the legacy substitute path instead of the id path.
	 *
	 * <p>The decision lives in this static seam so the offline tests can pin it without
	 * constructing the item — a mod Item cannot be instantiated in a bootstrapped-and-
	 * frozen test JVM (Item.java:61 builds the intrusive registry holder unconditionally,
	 * the same Forge wall as the mod-Block offline lesson).
	 */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.CROWBAR == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
