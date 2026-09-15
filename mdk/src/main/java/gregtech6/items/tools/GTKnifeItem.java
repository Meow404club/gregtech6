package gregtech6.items.tools;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * The formal GT6 knife — item id {@code gt6:knife} (task p29-w5-t2-blade-six). Upstream
 * GT_Tool_Knife.java:26-96 is a {@code GT_Tool_Sword} SUBCLASS (the Loader_Tools.java:135
 * registration row, {@code 1*U} material amount) — the port keeps the subclass face:
 * <ul>
 * <li><b>Damage</b> — base damage 2.0F (:56-59) verbatim; the attack-rate anchor stays
 *     the vanilla sword −2.4F (no 1.7.10 source); per-entity 200 (:49-52) folds to one
 *     point (the inherited {@link GTSwordItem#hurtEnemy}).</li>
 * <li><b>Mining face</b> — NO upstream override: the sword surface inherits
 *     ({@link GTSwordItem#mines}), at the ×0.5 speed multiplier (:63-66) →
 *     {@link #MINING_SPEED} = 3.0F (the 6.0F anchor × 0.5).</li>
 * <li><b>canCollect</b> (:75) — CUT (no 1.20.1 vanilla face; the scissors/shears pool).
 * </ul>
 *
 * <p>Durability 512 (the family value; the {@code 1*U} material amount is the upstream
 * per-material scaling, the standing pool cut).
 */
public class GTKnifeItem extends GTSwordItem {

	/** Upstream getBaseDamage :56-59 — 2.0F kept verbatim. */
	public static final float ATTACK_DAMAGE = 2.0F;

	/** The vanilla sword attack-rate anchor (the base constant; no upstream source). */
	public static final float ATTACK_SPEED = GTSwordItem.ATTACK_SPEED;

	/** Upstream getSpeedMultiplier :63-66 — the 6.0F anchor × 0.5. */
	public static final float MINING_SPEED = GTSwordItem.MINING_SPEED * 0.5F;

	//? if forge {
	private final com.google.common.collect.Multimap<Attribute, AttributeModifier> mAttackModifiers = buildAttackModifiers(ATTACK_DAMAGE, ATTACK_SPEED);
	//?} else {
	/*private final net.minecraft.world.item.component.ItemAttributeModifiers mAttackModifiers = buildAttackModifiers(ATTACK_DAMAGE, ATTACK_SPEED);
	*///?}

	public GTKnifeItem(Properties aProperties) {
		super(aProperties);
	}

	/** The dig-speed half — the knife speed on the inherited sword surface. */
	public static float destroySpeedBonus(net.minecraft.world.level.block.state.BlockState aState) {
		return mines(aState) ? MINING_SPEED : 1.0F;
	}

	@Override
	public float getDestroySpeed(ItemStack aStack, net.minecraft.world.level.block.state.BlockState aState) {
		return destroySpeedBonus(aState);
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
	 * The stack classifier — the gt6 KNIFE key replaces the sword key (the vanilla
	 * SWORD_DIG face inherits — upstream the knife stays a TOOL_sword-class harvester).
	 */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.KNIFE == aToolAction
				|| net.minecraftforge.common.ToolActions.SWORD_DIG == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
