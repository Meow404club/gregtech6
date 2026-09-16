package gregtech6.items.tools;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The formal GT6 butchery knife — item id {@code gt6:butchery_knife} (task
 * p29-w5-t2-blade-six). Upstream GT_Tool_ButcheryKnife.java:35-115 (the
 * Loader_Tools.java:136 registration row, {@code 4*U}):
 * <ul>
 * <li><b>Damage</b> — base damage 1.0F (:57-59) verbatim; per-entity 400 (:52-54) folds
 *     to one point; per-block 200 (:37-39) folds to one point. The attack-rate anchor:
 *     the upstream desc "Has a slow Attack Rate" → −3.4F (0.6 attacks/s, the slowest
 *     weapon rate this port carries; no 1.7.10 source — the 1.9 attribute postdates).</li>
 * <li><b>NO mining face</b> — isMinableBlock false :112-114 + isMiningTool F :82-84:
 *     {@code mines} is the constant false, {@link #isCorrectToolForDrops} never
 *     authorizes (the durability still pays on breaks, the vanilla mineBlock face).</li>
 * <li><b>The Looting face</b> (:87-94) — upstream returns the
 *     {@code LOOTING_ENCHANTMENT} at level {@code mToolQuality / 2 + 1}; the single
 *     steel tier carries mToolQuality 2 (MT.java:1713 {@code .qual(3, 6.0, 512, 2)}), so
 *     the level is the constant {@link #LOOTING_LEVEL} = 2. Modern face: the platform
 *     item-level enchantment hook ({@code IForgeItem.getEnchantmentLevel} :553 on 1.20.1
 *     forge / {@code IItemExtension.getEnchantmentLevel} :493 on 21.1 neo — the vanilla
 *     mob-loot read flows through {@code ItemStack.getEnchantmentLevel} on both legs)
 *     answers the looting enchantment at the constant level.</li>
 * <li><b>hurtResistance ×2</b> (:62-64) — CUT (the crowbar canPenetrate precedent: the
 *     upstream MultiItemTool attack pipeline is not carried).</li>
 * </ul>
 *
 * <p>Durability 512 (the family value).
 */
public class GTButcheryKnifeItem extends Item {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :57-59 — 1.0F kept verbatim. */
	public static final float ATTACK_DAMAGE = 1.0F;

	/** The "Has a slow Attack Rate" face — 0.6 attacks/s (the slowest weapon anchor). */
	public static final float ATTACK_SPEED = -3.4F;

	/**
	 * Upstream getEnchantmentLevels :92-94 — {@code mToolQuality / 2 + 1} at the single
	 * steel tier (mToolQuality 2, MT.java:1713) = the constant 2.
	 */
	public static final int LOOTING_LEVEL = 2;

	/** The registration-row desc (Loader_Tools.java:136 "Has a slow Attack Rate"). */
	public static final String TOOLTIP_KEY = "item.gt6.butchery_knife.tooltip";

	//? if forge {
	private final com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> mAttackModifiers = com.google.common.collect.ImmutableMultimap.of(
			net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
			new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", (double) ATTACK_DAMAGE, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION),
			net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
			new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_SPEED_UUID, "Weapon modifier", (double) ATTACK_SPEED, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
	//?} else {
	/*private final net.minecraft.world.item.component.ItemAttributeModifiers mAttackModifiers = net.minecraft.world.item.component.ItemAttributeModifiers
			.builder()
			.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
					new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, (double) ATTACK_DAMAGE, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
					net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
			.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
					new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_SPEED_ID, (double) ATTACK_SPEED, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
					net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
			.build();
	*///?}

	public GTButcheryKnifeItem(Properties aProperties) {
		super(aProperties);
	}

	/** NO mining face (upstream isMinableBlock :112-114 constant false). */
	public static boolean mines(net.minecraft.world.level.block.state.BlockState aState) {
		return false;
	}

	/** The never-authorizing drop half (the constant-false face). */
	@Override
	//? if forge {
	public boolean isCorrectToolForDrops(net.minecraft.world.level.block.state.BlockState aState) {
	//?} else {
	/*public boolean isCorrectToolForDrops(ItemStack aStack, net.minecraft.world.level.block.state.BlockState aState) {
	//21.1: the stack parameter joined the signature (the GTCrowbarItem fork).
	*///?}
		return false;
	}

	/** The hand-speed everywhere (no surface to speed up). */
	@Override
	public float getDestroySpeed(ItemStack aStack, net.minecraft.world.level.block.state.BlockState aState) {
		return 1.0F;
	}

	/** Upstream getToolDamagePerBlockBreak :37-39 — 200 units fold into one point. */
	@Override
	public boolean mineBlock(ItemStack aStack, net.minecraft.world.level.Level aLevel,
			net.minecraft.world.level.block.state.BlockState aState, net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack :52-54 — 400 units fold into one point. */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}

	//? if forge {
	@Override
	public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot aSlot) {
		return aSlot == EquipmentSlot.MAINHAND ? mAttackModifiers : super.getDefaultAttributeModifiers(aSlot);
	}
	//?} else {
	/*@Override
	public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers() {
		return mAttackModifiers;
	}
	*///?}

	/** The registration-row desc tooltip (the GTAxeItem hover shape). */
	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, net.minecraft.world.level.Level aLevel, java.util.List<net.minecraft.network.chat.Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
		super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
		aTooltip.add(net.minecraft.network.chat.Component.translatable(TOOLTIP_KEY));
	}
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, java.util.List<net.minecraft.network.chat.Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
	//21.1: the hover signature carries the Item.TooltipContext (the GT6LubricantBucket fork).
		super.appendHoverText(aStack, aContext, aTooltip, aFlag);
		aTooltip.add(net.minecraft.network.chat.Component.translatable(TOOLTIP_KEY));
	}
	*///?}

	/** The stack classifier — the gt6 butchery key only (never the sword/knife keys). */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.BUTCHERY_KNIFE == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}

	/**
	 * The Looting face (upstream getEnchantments/getEnchantmentLevels :87-94) — the
	 * platform item-level hook answers the mob-looting enchantment at the constant
	 * {@link #LOOTING_LEVEL} (the vanilla {@code ItemStack.getEnchantmentLevel} mob-loot
	 * read routes here on both legs).
	 */
	//? if forge {
	@Override
	public int getEnchantmentLevel(ItemStack aStack, net.minecraft.world.item.enchantment.Enchantment aEnchantment) {
		if (aEnchantment == net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING) return LOOTING_LEVEL;
		return super.getEnchantmentLevel(aStack, aEnchantment);
	}
	//?} else {
	/*@Override
	public int getEnchantmentLevel(ItemStack aStack, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> aEnchantment) {
		if (aEnchantment.is(net.minecraft.world.item.enchantment.Enchantments.LOOTING)) return LOOTING_LEVEL;
		return super.getEnchantmentLevel(aStack, aEnchantment);
	}
	*///?}
}
