package gregtech6.items.tools;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The formal GT6 spade — item id {@code gt6:spade} (task p29-w5-t1-dig-six). Upstream
 * GT_Tool_Spade.java:39-119 vs the plain shovel: the SPEED and the HARVEST.
 * <ul>
 * <li><b>Speed ×1.5</b> (:70-73) → {@link #MINING_SPEED} = 9.0F (the 6.0F anchor ×
 *     1.5).</li>
 * <li><b>Mining surface</b> (:92-95): the shovel-class harvest + fire — same shape as
 *     the shovel's minus the sand/snow EXCLUSION the upstream spade carries on its
 *     {@code TOOL_shovel} arm... the exclusion is the upstream meta-tool
 *     mutual-exclusion design (the spade refuses what the SHOVEL claims, GTCrowbarItem
 *     :98-100 lesson); the flat port registers BOTH as standalone items, so the spade
 *     keeps the full shovel-class surface ({@link GTShovelItem#mines}) — declared: the
 *     exclusion folds away with the meta-tool family.</li>
 * <li><b>The harvest conversion</b> (:81-89 {@code convertBlockDrops}) → the loot seam
 *     ({@code GT6ToolLootModifiers} mode {@code HARVESTABLE_SPADE}, the per-tool GLM
 *     JSON): the six {@code BlocksGT.harvestableSpade} blocks drop AS THEMSELVES at
 *     forced chance — NOT in this class (the card RED LINE).</li>
 * <li><b>Behavior arms</b> (:108-114): Plug_Leak CUT (no leak face), Place_Path(50) →
 *     {@link GTPickaxeItem#createPath}, Place_Paddy(50) CUT-inert (the GrowthCraft
 *     gate, the GTShovelItem verdict), the {@code Behavior_Tool(TOOL_shovel)} face =
 *     this item's mining face, Place_Torch → {@link GTPickaxeItem#placeTorchFromInventory}.</li>
 * <li><b>Damage</b>: base 1.5F (:65-67); per-block 50 / per-attack 200 fold to one point.</li>
 * </ul>
 *
 * <p>Durability 512 (the family value; upstream durability multiplier 1.0).
 */
public class GTSpadeItem extends Item {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :65-67 — 1.5F kept verbatim. */
	private static final float ATTACK_DAMAGE = 1.5F;

	/** Upstream getSpeedMultiplier :70-73 — the 6.0F anchor × 1.5. */
	public static final float MINING_SPEED = GTPickaxeItem.MINING_SPEED * 1.5F;

	//? if forge {
	private final Multimap<Attribute, AttributeModifier> mAttackModifiers = ImmutableMultimap.of(
			Attributes.ATTACK_DAMAGE,
			new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", (double) ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION));
	//?} else {
	/*// 21.1: the ItemAttributeModifiers form (the GTCrowbarItem fork verbatim).
	private final net.minecraft.world.item.component.ItemAttributeModifiers mAttackModifiers = net.minecraft.world.item.component.ItemAttributeModifiers
			.builder()
			.add(Attributes.ATTACK_DAMAGE,
					new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, (double) ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
					net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
			.build();
	*///?}

	public GTSpadeItem(Properties aProperties) {
		super(aProperties);
	}

	/** Upstream :92-95 — the shovel-class surface + fire (see the javadoc exclusion note). */
	public static boolean mines(BlockState aState) {
		return GTShovelItem.mines(aState);
	}

	/** The dig-speed seam — the ×1.5 spade speed on the surface. */
	public static float destroySpeedBonus(BlockState aState) {
		return mines(aState) ? MINING_SPEED : 1.0F;
	}

	/** The drop-authorization half (the family iron-tier gate). */
	@Override
	//? if forge {
	public boolean isCorrectToolForDrops(BlockState aState) {
	//?} else {
	/*public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
	//21.1: the stack parameter joined the signature (the GTCrowbarItem fork).
	*///?}
		return mines(aState) && !aState.is(BlockTags.NEEDS_DIAMOND_TOOL);
	}

	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		return destroySpeedBonus(aState);
	}

	/** Upstream getToolDamagePerBlockBreak :40-43 — 50 units fold into one point. */
	@Override
	public boolean mineBlock(ItemStack aStack, net.minecraft.world.level.Level aLevel, BlockState aState,
			net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack :55-58 — 200 units fold into one point. */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}

	/** The upstream arm row :108-114 minus the cuts: path, then torch (the shovel order). */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		if (GTPickaxeItem.createPath(aContext)) {
			return InteractionResult.sidedSuccess(aContext.getLevel().isClientSide());
		}
		if (GTPickaxeItem.placeTorchFromInventory(aContext)) {
			return InteractionResult.sidedSuccess(aContext.getLevel().isClientSide());
		}
		return InteractionResult.PASS;
	}

	//? if forge {
	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot aSlot) {
		return aSlot == EquipmentSlot.MAINHAND ? mAttackModifiers : super.getDefaultAttributeModifiers(aSlot);
	}
	//?} else {
	/*@Override
	public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers() {
		return mAttackModifiers;
	}
	*///?}

	/** The stack classifier — the gt6_spade action + the vanilla SHOVEL_DIG face. */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.SPADE == aToolAction
				|| net.minecraftforge.common.ToolActions.SHOVEL_DIG == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
