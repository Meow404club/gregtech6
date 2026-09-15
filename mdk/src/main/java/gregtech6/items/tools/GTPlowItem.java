package gregtech6.items.tools;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

import gregtech6.items.tools.loot.GT6ToolSweep;

/**
 * The formal GT6 plow — item id {@code gt6:plow} (task p29-w5-t4-field-five). Upstream
 * GT_Tool_Plow.java:41-95:
 * <ul>
 * <li><b>Mining surface</b> (isMinableBlock :62-64): the {@code TOOL_plow} harvest arm
 *     (GT6-block-only — structurally empty in this universe, declared) +
 *     {@code Material.fire} + {@code Material.snow} (+ craftedSnow = GT6 blocks, W6
 *     pool) → the {@link #SNOW_FAMILY}/{@link #FIRE_FAMILY} identity sets (the 1.7.10
 *     material arms are enumerable; powder_snow is the modern ride-along, declared).</li>
 * <li><b>The 3x3 sweep</b> (convertBlockDrops :67-75): every block break with the plow
 *     re-harvests the 26 neighbours that pass the dig-speed gate — rides
 *     {@link GT6ToolSweep#sweep} (the t1 shared walk, the sIsHarvestingRightNow
 *     ThreadLocal guard verbatim) hooked on {@link #mineBlock} (the
 *     ServerPlayerGameMode.destroyBlock face; NOT inside the loot modifier — the card
 *     RED LINE). The sweep gate needs {@code getDestroySpeed} ZERO off-surface (the
 *     upstream getDigSpeed face), which {@link #destroySpeedBonus} carries.</li>
 * <li><b>Snowman ×4 damage</b> (getNormalDamageAgainstEntity :50-52): the golem hit
 *     pays the remaining ×3 through the post-hit arm ({@link #hurtEnemy}) — the flat
 *     attribute map cannot multiply per-target, so the first hit rides the ×1
 *     attribute and the bonus lands as the follow-up damage (one hurt event per share,
 *     declared pipeline mapping).</li>
 * <li><b>Behavior_Tool(TOOL_shovel)</b> (:89): the machine relay face — the vanilla
 *     SHOVEL_DIG classification stands in (the GTSpadeItem shape); the path/torch arms
 *     are NOT the plow's (upstream adds only the shovel relay).</li>
 * <li><b>Damage</b>: base 1.0F (:55-57); the default 100-unit break/attack rows fold
 *     to one point.</li>
 * </ul>
 *
 * <p>Durability 512 (the family value; upstream getMaxDurabilityMultiplier 1.0
 * default).
 */
public class GTPlowItem extends Item {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :55-57 — 1.0F kept verbatim. */
	private static final float ATTACK_DAMAGE = 1.0F;

	/** Upstream getSpeedMultiplier — default 1.0 — the 6.0F anchor. */
	public static final float MINING_SPEED = 6.0F;

	/**
	 * The upstream {@code Material.snow} arm (GT_Tool_Plow.java:63) — the 1.7.10 snow
	 * material is precisely the layer + the block; powder_snow is the modern ride-along
	 * (declared). Identity set (not the {@code #minecraft:snow} tag) — the upstream arm
	 * is enumerable and the offline test pins it.
	 */
	static final ImmutableSet<Block> SNOW_FAMILY = ImmutableSet.of(Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW);

	/** The upstream {@code Material.fire} arm (:63) — fire is in no mineable tag. */
	static final ImmutableSet<Block> FIRE_FAMILY = ImmutableSet.of(Blocks.FIRE, Blocks.SOUL_FIRE);

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

	public GTPlowItem(Properties aProperties) {
		super(aProperties);
	}

	/** The upstream isMinableBlock :62-64 modern form — the snow + fire material sets (the TOOL_plow arm is the declared empty face). */
	public static boolean mines(BlockState aState) {
		return SNOW_FAMILY.contains(aState.getBlock()) || FIRE_FAMILY.contains(aState.getBlock());
	}

	/** The dig-speed seam — full speed on the surface, ZERO off it (the sweep gate + the upstream getDigSpeed face). */
	public static float destroySpeedBonus(BlockState aState) {
		return mines(aState) ? MINING_SPEED : 0.0F;
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

	/**
	 * The centre-break payment + the 3x3 sweep (the upstream convertBlockDrops :67-75
	 * arm — the walk re-harvests every dig-speed-passing neighbour; each neighbour pays
	 * its own point through this same mineBlock, the centre pays here).
	 */
	@Override
	public boolean mineBlock(ItemStack aStack, Level aLevel, BlockState aState,
			net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide) {
			if (aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
				aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
			}
			if (aEntity instanceof net.minecraft.server.level.ServerPlayer tPlayer) {
				GT6ToolSweep.sweep(tPlayer, aStack, aPos);
			}
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack (default 100) — one point. */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		// the Snowman ×4 (:50-52): the remaining ×3 rides the post-hit arm
		if (aTarget instanceof SnowGolem && aAttacker instanceof Player tPlayer && aTarget.isAlive()) {
			aTarget.hurt(aTarget.damageSources().playerAttack(tPlayer), ATTACK_DAMAGE * 3.0F);
		}
		return true;
	}

	/** The pure multiplier face (the :50-52 test pin): Snowman ×4, everything else ×1. */
	public static float snowmanDamageMultiplier(Entity aEntity) {
		return aEntity instanceof SnowGolem ? 4.0F : 1.0F;
	}

	/** The plow carries no useOn arms (the upstream relay is the machine pool). */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
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

	/** The stack classifier — the gt6_plow action + the vanilla SHOVEL_DIG face (the :89 TOOL_shovel relay). */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.PLOW == aToolAction
				|| net.minecraftforge.common.ToolActions.SHOVEL_DIG == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
