package gregtech6.items.tools;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The formal GT6 club — item id {@code gt6:club} (task p29-w5-t2-blade-six). Upstream
 * GT_Tool_Club.java:47-142 is a {@code GT_Tool_HardHammer} SUBCLASS (the
 * Loader_Tools.java:130 registration row {@code 6*U}, NO OreDictToolNames — the pure
 * weapon + rock-crusher identity, registered with the {@code TOOL_hammer} behaviour):
 * <ul>
 * <li><b>Damage</b> — the club has NO getBaseDamage override: it inherits the
 *     HardHammer 5.0F (GT_Tool_HardHammer.java:73) VERBATIM (the card's "6.0F" literal
 *     is the AxeDouble's base damage — GT_Tool_AxeDouble.getBaseDamage :29-32 — and the
 *     6*U is the club's material AMOUNT; the test pins the source-truth 5.0F). Per-block
 *     50 (:48) folds to one point; per-entity 50 (:51) folds to one point.</li>
 * <li><b>Attack rate</b> — the vanilla heavy-tool anchor −3.0F (1.0 attacks/s, the
 *     iron AxeItem rate; no 1.7.10 source).</li>
 * <li><b>Mining surface</b> — the inherited HardHammer isMinableBlock (:83-86) modern
 *     form: the {@code TOOL_hammer}/{@code TOOL_pickaxe} harvest arms → the vanilla
 *     {@code #minecraft:mineable/pickaxe} tag, the rock/glass/ice material arms → the
 *     pickaxe family sets ({@link GTPickaxeItem#GLASS_FAMILY}/{@link #ICE_FAMILY};
 *     silverfish/spawner ride the tag). At the ×0.5 speed multiplier (:53) →
 *     {@link #MINING_SPEED} = 3.0F. The RM.Hammer ore-crush residual arm stays pooled
 *     (the p25 hammer ruling); obsidian-crush pools with the tier-ladder card (the
 *     iron-tier gate).</li>
 * <li><b>The rockGt conversion</b> (:61-110) — NOT here (the card RED LINE): it rides
 *     the {@code GT6ToolLootModifiers} loot seam, mode {@code CLUB_ROCK_CRUSH} (the
 *     {@code BlockStones}/{@code redstone-ore-oredict} mod arms cut — W6 domain).</li>
 * <li><b>canBlock</b> (:56) — CUT (the GTSwordItem ruling); hurtResistance ×3/2 (:52)
 *     — CUT (the ButcheryKnife pipeline precedent).</li>
 * </ul>
 *
 * <p>Durability 512 (the family value; the {@code 6*U} material amount is the upstream
 * per-material scaling, the standing pool cut).
 */
public class GTClubItem extends Item {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/**
	 * The inherited HardHammer base damage 5.0F (GT_Tool_HardHammer.java:73 — the club
	 * carries NO getBaseDamage override, GT_Tool_Club.java:47-142 verbatim).
	 */
	public static final float ATTACK_DAMAGE = 5.0F;

	/** The vanilla heavy-tool attack-rate anchor (1.0 attacks/s). */
	public static final float ATTACK_SPEED = -3.0F;

	/** Upstream getSpeedMultiplier :53 — the 6.0F anchor × 0.5. */
	public static final float MINING_SPEED = GTPickaxeItem.MINING_SPEED * 0.5F;

	//? if forge {
	private final com.google.common.collect.Multimap<Attribute, AttributeModifier> mAttackModifiers = com.google.common.collect.ImmutableMultimap.of(
			net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
			new AttributeModifier(Item.BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", (double) ATTACK_DAMAGE, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION),
			net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
			new AttributeModifier(Item.BASE_ATTACK_SPEED_UUID, "Weapon modifier", (double) ATTACK_SPEED, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
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

	public GTClubItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The inherited HardHammer isMinableBlock :83-86 modern form — the pickaxe tag +
	 * the glass/ice family sets (the GTPickaxeItem extension sets, same package).
	 */
	public static boolean mines(BlockState aState) {
		return aState.is(BlockTags.MINEABLE_WITH_PICKAXE)
				|| GTPickaxeItem.GLASS_FAMILY.contains(aState.getBlock())
				|| GTPickaxeItem.ICE_FAMILY.contains(aState.getBlock());
	}

	/** The dig-speed seam — the ×0.5 club speed on the surface. */
	public static float destroySpeedBonus(BlockState aState) {
		return mines(aState) ? MINING_SPEED : 1.0F;
	}

	/**
	 * The drop-authorization half (the family iron-tier gate — the obsidian crush pools
	 * with the tier-ladder card).
	 */
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

	/** Upstream getToolDamagePerBlockBreak :48 — 50 units fold into one point. */
	@Override
	public boolean mineBlock(ItemStack aStack, Level aLevel, BlockState aState,
			net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack :51 — 50 units fold into one point. */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}

	//? if forge {
	@Override
	public com.google.common.collect.Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot aSlot) {
		return aSlot == EquipmentSlot.MAINHAND ? mAttackModifiers : super.getDefaultAttributeModifiers(aSlot);
	}
	//?} else {
	/*@Override
	public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers() {
		return mAttackModifiers;
	}
	*///?}

	/**
	 * The stack classifier — the gt6 CLUB key + the vanilla pickaxe face (the inherited
	 * HardHammer hammer/pickaxe arms) + the HAMMER gt6 action (the Loader_Tools.java:130
	 * behaviour row {@code TOOL_hammer} — the machine dispatch must see the club exactly
	 * as the 1.7.10 behaviour row did).
	 */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.CLUB == aToolAction
				|| GT6ToolActions.HAMMER == aToolAction
				|| net.minecraftforge.common.ToolActions.PICKAXE_DIG == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
