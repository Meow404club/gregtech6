package gregtech6.items.tools;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The formal GT6 shovel — item id {@code gt6:shovel} (task p29-w5-t1-dig-six). Upstream
 * GT_Tool_Shovel.java:39-101:
 * <ul>
 * <li><b>Mining surface</b> (isMinableBlock :62-63): the {@code TOOL_shovel} harvest arm
 *     + the sand/grass/ground/snow/craftedSnow/clay materials + {@code Material.fire} →
 *     the vanilla {@code #minecraft:mineable/shovel} tag (which carries the whole dirt/
 *     sand/snow/clay/gravel universe) + the explicit {@link #FIRE_FAMILY} (fire is in no
 *     mineable tag — the upstream arm the flat port keeps).</li>
 * <li><b>Behavior arms</b> (:96-101): {@code Behavior_Plug_Leak} CUT (the pipe-leak face
 *     does not exist in this repo — the GTFluidPipeBlockEntity cut, see GTPickaxeItem);
 *     {@code Behavior_Place_Path(50)} → {@link GTPickaxeItem#createPath};
 *     {@code Behavior_Place_Paddy(50)} → CUT with the upstream-faithful verdict: the arm
 *     is gated on {@code IL.GrC_Paddy.exists()} (Behavior_Place_Paddy.java:48) — the
 *     GrowthCraft paddy block ABSENT in this universe means the upstream arm is INERT
 *     (the tooltip is not added, the use returns false); the port keeps it inert instead
 *     of inventing a vanilla target (revives with a rice/paddy domain card).
 *     {@code Behavior_Tool(TOOL_shovel)} = the mining/attack face itself;
 *     {@code Behavior_Place_Torch} → {@link GTPickaxeItem#placeTorchFromInventory}.</li>
 * <li><b>Damage</b>: base 1.5F (:60-62); per-block/per-attack figures fold into the
 *     single point (the family mapping).</li>
 * </ul>
 *
 * <p>Durability ladder (task p31-dig-ladder): the {@link GT6ToolLadder} form over the
 * stack's identity — durability j/100, speed ×1.0 × mToolSpeed, the :482 quality gate;
 * the identity-less arm = Steel bit-exact (512 / 6.0F, the pre-ladder constants). The
 * drop authorization stays the stackless coarse floor on 1.20.1 (no vanilla shovel
 * block sits behind {@code needs_diamond_tool}, the gate is uniform for the family).
 */
public class GTShovelItem extends Item implements GT6ToolLadder.LadderTool {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :60-62 — 1.5F kept verbatim. */
	private static final float ATTACK_DAMAGE = 1.5F;

	/** The iron-tier dig-speed scale (upstream getSpeedMultiplier :69-71 = 1.0 — the anchor). */
	public static final float MINING_SPEED = 6.0F;

	/** The form durability multiplier (ToolStats.java:71 default 1.0). */
	public static final float DURABILITY_MULTIPLIER = 1.0F;

	/** The form speed multiplier (upstream getSpeedMultiplier :69-71 = 1.0). */
	public static final float SPEED_MULTIPLIER = 1.0F;

	/** The upstream {@code Material.fire} arm — fire is shovel-minable upstream, in no tag here. */
	static final ImmutableSet<Block> FIRE_FAMILY = ImmutableSet.of(Blocks.FIRE, Blocks.SOUL_FIRE);

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

	public GTShovelItem(Properties aProperties) {
		super(aProperties);
	}

	/** The upstream isMinableBlock :62-63 modern form — the tag arm + the fire arm. */
	public static boolean mines(BlockState aState) {
		return aState.is(BlockTags.MINEABLE_WITH_SHOVEL) || FIRE_FAMILY.contains(aState.getBlock());
	}

	/** The dig-speed seam (the DiggerItem getDestroySpeed shape on the surface) — the stack-free steel arm. */
	public static float destroySpeedBonus(BlockState aState) {
		return mines(aState) ? MINING_SPEED : 1.0F;
	}

	/** The ladder dig-speed seam — ×1.0 × the stack's material speed (:483). */
	public static float destroySpeedBonus(ItemStack aStack, BlockState aState) {
		return mines(aState) ? GT6ToolLadder.speed(SPEED_MULTIPLIER, GT6ToolLadder.materialOf(aStack)) : 1.0F;
	}

	/**
	 * The drop authorization — the stack-aware face (forge 1.20.1 IForgeItem overload,
	 * 1.21.1 the vanilla signature): the surface minus the blocks the material quality
	 * cannot harvest (the {@link GT6ToolLadder#qualityGate} ZERO-speed floor backstops
	 * the 1.20.1 stackless break path).
	 */
	@Override
	public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
		return !GT6ToolLadder.qualityGate(aStack, aState) && coarseFloor(aState);
	}

	/** The quality-blind floor (the steel-or-better semantics, shared with the pickaxe). */
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

	/** The composed display name — "Shovel (Bronze)"; bare for identity-less stacks. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
	}

	/** Upstream getToolDamagePerBlockBreak :40-42 — 50 units fold into one point. */
	@Override
	public boolean mineBlock(ItemStack aStack, net.minecraft.world.level.Level aLevel, BlockState aState,
			net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack :56-58 — 200 units fold into one point. */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}

	/**
	 * The upstream arm row :96-101 minus the cuts (Plug_Leak — no leak face; Place_Paddy —
	 * the GrowthCraft-gated arm is inert in this universe): path conversion, then torch.
	 */
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

	/** The stack classifier — the gt6_shovel action + the vanilla SHOVEL_DIG face. */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.SHOVEL == aToolAction
				|| net.minecraftforge.common.ToolActions.SHOVEL_DIG == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
