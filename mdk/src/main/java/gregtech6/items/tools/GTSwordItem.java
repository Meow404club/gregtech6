package gregtech6.items.tools;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.tags.BlockTags;
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
 * The formal GT6 sword — item id {@code gt6:sword} (task p29-w5-t2-blade-six, the W5
 * tool wave card 2; the {@link GTPickaxeItem} class shape). Upstream
 * GT_Tool_Sword.java:43-127:
 * <ul>
 * <li><b>Damage</b> — base damage 4.0F (:70-72) kept VERBATIM as the main-hand
 *     attribute (the crowbar/pickaxe ruling: the upstream {@code mToolQuality} fold of
 *     MultiItemTool.getToolCombatDamage :392 is not carried — t1 pinned the bare
 *     {@code getBaseDamage}); per-entity attack 100 units (:60-62) folds into one
 *     vanilla point ({@link #hurtEnemy}).</li>
 * <li><b>Attack speed</b> — no 1.7.10 source (the attack-speed attribute postdates
 *     1.7.10): the vanilla SwordItem rate anchor −2.4F (1.6 attacks/s,
 *     SwordItem.java:30) on the DiggerItem dual-modifier shape (:30-34).</li>
 * <li><b>Mining surface</b> (:108-110) — the {@code TOOL_sword} harvest arm → the
 *     vanilla {@code #minecraft:mineable/sword} tag (live/RCON), the
 *     {@code Material.leaves}/{@code Material.vine} arms → {@link BlockTags#LEAVES} +
 *     {@link Blocks#VINE}, the {@code Material.cloth}/{@code Material.carpet} arms →
 *     {@link BlockTags#WOOL} + {@link BlockTags#WOOL_CARPETS} (the card-scoped
 *     leaves/vine/web/cloth face — the web rides the tag). The plants/cactus/cake/tnt/
 *     sponge/water residual arms stay pooled (the wave tool-face pool).</li>
 * <li><b>The grass/stick/vine drop conversion</b> (:94-105 harvestGrass/harvestStick/
 *     vine) — NOT here (the card RED LINE): it rides the
 *     {@code GT6ToolLootModifiers} loot seam, mode {@code SWORD_HARVEST}.</li>
 * <li><b>canBlock</b> (:84-87) — CUT (the crowbar canBlock precedent): 1.20.1 has no
 *     item-level right-click block face (the shield owns the mechanic); declared to the
 *     deviation pool, no mixin.</li>
 * </ul>
 *
 * <p>Durability 512 (the family value; upstream per-block 200 units folds to one point).
 */
public class GTSwordItem extends Item {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :70-72 — 4.0F kept verbatim as the main-hand attribute. */
	public static final float ATTACK_DAMAGE = 4.0F;

	/** The vanilla sword attack-rate anchor (SwordItem.java:30 −2.4F on the 4.0 base). */
	public static final float ATTACK_SPEED = -2.4F;

	/** The dig speed on the sword surface — the iron-tier anchor (upstream speed ×1.0). */
	public static final float MINING_SPEED = 6.0F;

	//? if forge {
	private final Multimap<Attribute, AttributeModifier> mAttackModifiers = buildAttackModifiers(ATTACK_DAMAGE, ATTACK_SPEED);

	/** The dual-modifier builder — the DiggerItem.java:29-35 shape; subclasses re-call with their own constants. */
	protected static Multimap<Attribute, AttributeModifier> buildAttackModifiers(float aDamage, float aSpeed) {
		return ImmutableMultimap.of(
				Attributes.ATTACK_DAMAGE,
				new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", (double) aDamage, AttributeModifier.Operation.ADDITION),
				Attributes.ATTACK_SPEED,
				new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", (double) aSpeed, AttributeModifier.Operation.ADDITION));
	}
	//?} else {
	/*private final net.minecraft.world.item.component.ItemAttributeModifiers mAttackModifiers = buildAttackModifiers(ATTACK_DAMAGE, ATTACK_SPEED);

	// 21.1: the ItemAttributeModifiers form (the GTCrowbarItem fork verbatim).
	protected static net.minecraft.world.item.component.ItemAttributeModifiers buildAttackModifiers(float aDamage, float aSpeed) {
		return net.minecraft.world.item.component.ItemAttributeModifiers
				.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, (double) aDamage, AttributeModifier.Operation.ADD_VALUE),
						net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED,
						new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, (double) aSpeed, AttributeModifier.Operation.ADD_VALUE),
						net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
				.build();
	}
	*///?}

	public GTSwordItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The upstream isMinableBlock :108-110 modern form — the leaves/vine/plants/gourd
	 * material arms fold into the native {@code #minecraft:sword_efficient} vanilla face
	 * (the tag the vanilla SwordItem speed-reads, SwordItem.java:48), the web rides the
	 * explicit block (the vanilla SwordItem.java:45 hardcode), the
	 * {@code Material.cloth}/{@code Material.carpet} arms → {@link BlockTags#WOOL} +
	 * {@link BlockTags#WOOL_CARPETS} (the card-scoped leaves/vine/web/cloth face). The
	 * plants residual double-coverage is free (set semantics). The cake/tnt/sponge/water
	 * arms stay pooled (the wave tool-face pool). Static pure seam (the {@code mines}
	 * ruling).
	 */
	public static boolean mines(BlockState aState) {
		return aState.is(BlockTags.SWORD_EFFICIENT)
				|| aState.is(BlockTags.WOOL)
				|| aState.is(BlockTags.WOOL_CARPETS)
				|| aState.is(net.minecraft.world.level.block.Blocks.COBWEB);
	}

	/** The dig-speed seam — the sword speed on the surface, the hand speed elsewhere. */
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

	/** The dig-speed half. */
	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		return destroySpeedBonus(aState);
	}

	/** Upstream getToolDamagePerBlockBreak :44-47 — 200 units fold into one point. */
	@Override
	public boolean mineBlock(ItemStack aStack, Level aLevel, BlockState aState,
			net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack :60-62 — 100 units fold into one point. */
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

	/**
	 * The stack classifier — the gt6 sword action + the vanilla SWORD_DIG face (the
	 * knife subclass overrides the gt6 key only).
	 */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.SWORD == aToolAction
				|| net.minecraftforge.common.ToolActions.SWORD_DIG == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
