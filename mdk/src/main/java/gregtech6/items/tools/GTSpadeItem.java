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
 * <p>Durability ladder (task p31-dig-ladder): the {@link GT6ToolLadder} form over the
 * stack's identity — durability j/100, speed ×1.5 × mToolSpeed, the :482 quality gate;
 * the identity-less arm = Steel bit-exact (512 / 9.0F, the pre-ladder constants).
 */
public class GTSpadeItem extends Item implements GT6ToolLadder.LadderTool {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :65-67 — 1.5F kept verbatim. */
	private static final float ATTACK_DAMAGE = 1.5F;

	/** Upstream getSpeedMultiplier :70-73 — the 6.0F anchor × 1.5. */
	public static final float MINING_SPEED = GTPickaxeItem.MINING_SPEED * 1.5F;

	/** The form durability multiplier (upstream :76-77 = 1.0). */
	public static final float DURABILITY_MULTIPLIER = 1.0F;

	/** The form speed multiplier (upstream getSpeedMultiplier :70-73 = 1.5). */
	public static final float SPEED_MULTIPLIER = 1.5F;

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

	/** The dig-speed seam — the ×1.5 spade speed on the surface (the stack-free steel arm). */
	public static float destroySpeedBonus(BlockState aState) {
		return mines(aState) ? MINING_SPEED : 1.0F;
	}

	/** The ladder dig-speed seam — ×1.5 × the stack's material speed (:70-73 × :483). */
	public static float destroySpeedBonus(ItemStack aStack, BlockState aState) {
		return mines(aState) ? GT6ToolLadder.speed(SPEED_MULTIPLIER, GT6ToolLadder.materialOf(aStack)) : 1.0F;
	}

	/**
	 * The drop authorization — the stack-aware face (forge 1.20.1 IForgeItem overload,
	 * 1.21.1 the vanilla signature) over the quality gate.
	 */
	@Override
	public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
		return !GT6ToolLadder.qualityGate(aStack, aState) && coarseFloor(aState);
	}

	/** The quality-blind floor (the family iron-tier gate). */
	static boolean coarseFloor(BlockState aState) {
		return mines(aState) && !aState.is(BlockTags.NEEDS_DIAMOND_TOOL);
	}

	//? if forge {
	/** The stackless floor the 1.20.1 break path consults (the family iron-tier gate). */
	@Override
	public boolean isCorrectToolForDrops(BlockState aState) {
		return coarseFloor(aState);
	}
	//?}

	/** The dig-speed half (the level gate first, the upstream :482 order). */
	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		if (GT6ToolLadder.qualityGate(aStack, aState)) return 0.0F;
		return destroySpeedBonus(aStack, aState);
	}

	/** The form multiplier read (the {@link GT6ToolLadder.LadderTool} face). */
	@Override
	public float durabilityMultiplier() {
		return DURABILITY_MULTIPLIER;
	}

	/** The per-material durability (the {@link GT6ToolLadder} j/100 points — Steel fallback = 512). */
	@Override
	public int getMaxDamage(ItemStack aStack) {
		return GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(aStack, durabilityMultiplier()));
	}

	/** The composed display name — "Spade (Bronze)"; bare for identity-less stacks. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
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
