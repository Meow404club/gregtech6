package gregtech6.items.tools;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * The formal GT6 double axe — item id {@code gt6:axe_double} (task p29-w5-t2-blade-six).
 * Upstream GT_Tool_AxeDouble.java:29-62 is a {@code GT_Tool_Axe} SUBCLASS (the
 * Loader_Tools.java:123 registration row):
 * <ul>
 * <li><b>Damage</b> — getBaseDamage 6.0F (:29-32) verbatim (the port's hardest-hitting
 *     blade); the attack-rate anchor stays the axe's −3.0F (the upstream desc "has a
 *     slow Attack Rate" reads against the sword 1.6 rate — the vanilla iron-axe cadence
 *     is the slow face; no 1.7.10 source).</li>
 * <li><b>Durability ×1.5</b> (:34-37) — the family 512 × 1.5 = {@link #DURABILITY_POINTS}
 *     768 (the GTPickaxeGemItem multiplier-fold precedent, inverted).</li>
 * <li><b>Mining/felling faces</b> — no upstream override: the axe surface and the
 *     tree-fell modifier inherit (the PICKAXE family ruling).</li>
 * <li><b>hurtResistance 2→3 units</b> (:40-43) — CUT (the ButcheryKnife pipeline
 *     precedent).</li>
 * </ul>
 */
public class GTAxeDoubleItem extends GTAxeItem {

	/** Upstream getBaseDamage :29-32 — 6.0F kept verbatim. */
	public static final float ATTACK_DAMAGE = 6.0F;

	/** Upstream getMaxDurabilityMultiplier :34-37 — 1.0 × 1.5 over the family 512. */
	public static final int DURABILITY_POINTS = (int) (GTAxeItem.DURABILITY_POINTS * 1.5F);

	//? if forge {
	private final com.google.common.collect.Multimap<Attribute, AttributeModifier> mAttackModifiers = buildAttackModifiers(ATTACK_DAMAGE, ATTACK_SPEED);
	//?} else {
	/*private final net.minecraft.world.item.component.ItemAttributeModifiers mAttackModifiers = buildAttackModifiers(ATTACK_DAMAGE, ATTACK_SPEED);
	*///?}

	public GTAxeDoubleItem(Properties aProperties) {
		super(aProperties);
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
}
