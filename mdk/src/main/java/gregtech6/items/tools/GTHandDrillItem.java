package gregtech6.items.tools;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The formal GT6 hand drill — item id {@code gt6:hand_drill} (task
 * p29-w5-t4-field-five). Upstream GT_Tool_HandDrill.java:33-80:
 * <ul>
 * <li><b>Mining surface</b> (isMinableBlock :58-60): the {@code TOOL_drill} harvest
 *     arm ONLY — the GT6 ore-vein-prospecting blocks. No vanilla block declares a
 *     drill harvest tool and the GT6 block universe is unported (W6 pool) — the face
 *     is the DECLARED EMPTY SET (the open-question ruling: keep the declaration, pin
 *     the emptiness). {@code isMiningTool F} (:55): the tool never accelerates mining
 *     and never authorises drops — {@code getDestroySpeed} returns ZERO everywhere
 *     (the verbatim face) and {@code isCorrectToolForDrops} is false.</li>
 * <li><b>Behavior_Tool(TOOL_drill)</b> (:74): the machine relay face — the machine
 *     interaction pool, zero useOn here.</li>
 * <li><b>Damage</b>: base 0.5F (:36-38); the 100-unit rows fold to one point; speed
 *     ×0.5 (:41-43) — declared, unreachable while the surface is empty.</li>
 * </ul>
 *
 * <p>Durability 128 — 512 × the upstream ×0.25 multiplier (:46-48; the GTPickaxeGem
 * fold precedent). Recipe: the arrow-head + 2 bolts row (Loader_Tools.java:152) over
 * the steel convergence (the t1 dig-tool family shape).
 */
public class GTHandDrillItem extends Item {

	/** The ×0.25 multiplier fold (upstream getMaxDurabilityMultiplier :46-48). */
	public static final int DURABILITY_POINTS = 128;

	/** Upstream getBaseDamage :36-38 — 0.5F kept verbatim. */
	private static final float ATTACK_DAMAGE = 0.5F;

	/** Upstream getSpeedMultiplier :41-43 = 0.5 — the 6.0F anchor × 0.5 (declared, unreachable: the surface is empty). */
	public static final float MINING_SPEED = 6.0F * 0.5F;

	//? if forge {
	private final Multimap<Attribute, AttributeModifier> mAttackModifiers = ImmutableMultimap.of(
			Attributes.ATTACK_DAMAGE,
			new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", (double) ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION));
	//?} else {
	/*// 21.1: the ItemAttributeModifiers form (the GTShovelItem fork verbatim).
	private final net.minecraft.world.item.component.ItemAttributeModifiers mAttackModifiers = net.minecraft.world.item.component.ItemAttributeModifiers
			.builder()
			.add(Attributes.ATTACK_DAMAGE,
					new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, (double) ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
					net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
			.build();
	*///?}

	public GTHandDrillItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The upstream isMinableBlock :58-60 — the TOOL_drill harvest arm. The DECLARED
	 * EMPTY SET: no vanilla block declares the drill tool and the GT6 vein blocks are
	 * unported (the W6 pool) — the method is the structural false.
	 */
	public static boolean mines(BlockState aState) {
		return false;
	}

	/** The upstream isMiningTool-F face :55 — zero speed everywhere (the tool never accelerates mining). */
	public static float destroySpeedBonus(BlockState aState) {
		return 0.0F;
	}

	/** The verbatim :55 face — the drill never authorises drops. */
	@Override
	//? if forge {
	public boolean isCorrectToolForDrops(BlockState aState) {
	//?} else {
	/*public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
	//21.1: the stack parameter joined the signature (the GTCrowbarItem fork).
	*///?}
		return false;
	}

	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		return destroySpeedBonus(aState);
	}

	/** The one-point payment per break (the family mapping; upstream default 100 units). */
	@Override
	public boolean mineBlock(ItemStack aStack, Level aLevel, BlockState aState,
			net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack (default 100) — one point. */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
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

	/** The stack classifier — the gt6_hand_drill action. */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.HAND_DRILL == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
