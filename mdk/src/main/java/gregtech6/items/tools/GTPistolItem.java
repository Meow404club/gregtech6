package gregtech6.items.tools;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The GT6 gun family — item ids {@code gt6:pistol}/{@code gt6:carbine}/{@code gt6:rifle}
 * (task pistol-family-items, the R11-C row of the research.tool-crafting-audit matrix).
 * ONE class over the {@link Kind} table (the GTPocketMultitoolItem form); upstream the
 * three are ToolStats on the meta tool (GT_Tool_Pistol.java:34-56, the Carbine/Rifle
 * subclasses overriding only the icon + the {@code Behavior_Gun} ammo class + the death
 * message, GT_Tool_Carbine.java:28-40 / GT_Tool_Rifle.java:28-40).
 *
 * <p>What this card lands (the spec cut — the shooting chain exceeds S-M scale):
 * <ul>
 * <li><b>Registration + basic attributes</b> — durability 512 (the family value; the
 *     upstream {@code setMaterialAmount(U9*19)}/({@code U9*28}) per-material scaling,
 *     Loader_Tools.java:198-200, is the ladder card's face — LADDER CANDIDATE, the
 *     per-material rows :317-319 exist); melee damage 1.0F = the inherited
 *     {@code ToolStats.getBaseDamage} default (ToolStats.java:69 — the pistol family has
 *     NO getBaseDamage override, the bare-face pin, the GTSwordItem ruling form); the
 *     vanilla sword attack-rate anchor −2.4F (no 1.7.10 source).</li>
 * <li><b>The pistol-whip melee face</b> — upstream {@code getToolDamagePerEntityAttack}
 *     200 (GT_Tool_Pistol.java:38) folds to one durability point per hit (the club/sword
 *     fold convention). {@code isMiningTool=F} :42 → no mining surface, no
 *     {@code mineBlock}; {@code isWeapon=F} :40 → no ToolAction face, the census is
 *     structurally OFF (the BendingCylinderSmallTest reflection form).</li>
 * <li><b>The registration-row desc tooltip</b> — "Single Shot, Moderate Damage" /
 *     "Single Shot, Big Damage" / "Single Shot, Massive Damage" (Loader_Tools.java:198-200,
 *     the GTAxeItem hover shape).</li>
 * </ul>
 *
 * <p><b>DEFERRED (declared on the card)</b>: the entire shooting chain —
 * {@code Behavior_Gun} (BULLETS_SMALL/MEDIUM/LARGE per kind, GT_Tool_Pistol.onStatsAddedToTool
 * :47-50 / Carbine :31-34 / Rifle :31-34), the bullet item family, the ranged
 * {@code onItemRightClick} face and the per-kind death messages. The port lands them with
 * the gun-behaviour card; this item is obtainable + carryable + whip-able only.
 */
public class GTPistolItem extends Item {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** The inherited ToolStats.getBaseDamage default — 1.0F (ToolStats.java:69, no override in the family). */
	public static final float ATTACK_DAMAGE = 1.0F;

	/** The vanilla sword attack-rate anchor (SwordItem.java:30 −2.4F; no 1.7.10 source). */
	public static final float ATTACK_SPEED = -2.4F;

	/** The three registration rows (Loader_Tools.java:198-200) with their desc tooltips. */
	public enum Kind {
		/** {@code gt6:pistol} — "Single Shot, Moderate Damage" (:198), upstream BULLETS_SMALL. */
		PISTOL("item.gt6.pistol.tooltip"),
		/** {@code gt6:carbine} — "Single Shot, Big Damage" (:199), upstream BULLETS_MEDIUM. */
		CARBINE("item.gt6.carbine.tooltip"),
		/** {@code gt6:rifle} — "Single Shot, Massive Damage" (:200), upstream BULLETS_LARGE. */
		RIFLE("item.gt6.rifle.tooltip");

		/** The registration-row desc lang key. */
		public final String tooltipKey;

		Kind(String aTooltipKey) {
			tooltipKey = aTooltipKey;
		}
	}

	//? if forge {
	private static final com.google.common.collect.Multimap<Attribute, AttributeModifier> ATTACK_MODIFIERS = com.google.common.collect.ImmutableMultimap.of(
			net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
			new AttributeModifier(Item.BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", (double) ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION),
			net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
			new AttributeModifier(Item.BASE_ATTACK_SPEED_UUID, "Weapon modifier", (double) ATTACK_SPEED, AttributeModifier.Operation.ADDITION));
	//?} else {
	/*private static final net.minecraft.world.item.component.ItemAttributeModifiers ATTACK_MODIFIERS = net.minecraft.world.item.component.ItemAttributeModifiers
			.builder()
			.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
					new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, (double) ATTACK_DAMAGE, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
					net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
			.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
					new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_SPEED_ID, (double) ATTACK_SPEED, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
					net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
			.build();
	*///?}

	private final Kind mKind;

	public GTPistolItem(Kind aKind, Properties aProperties) {
		super(aProperties);
		mKind = aKind;
	}

	/** The kind this registration row carries (the tooltip + the deferred Behaviour_Gun axis). */
	public Kind kind() {
		return mKind;
	}

	/**
	 * Upstream getToolDamagePerEntityAttack :38 — 200 units fold into one point (the
	 * club/sword fold convention). The shot itself is the DEFERRED face.
	 */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}

	//? if forge {
	@Override
	public com.google.common.collect.Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot aSlot) {
		return aSlot == EquipmentSlot.MAINHAND ? ATTACK_MODIFIERS : super.getDefaultAttributeModifiers(aSlot);
	}
	//?} else {
	/*@Override
	public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers() {
		return ATTACK_MODIFIERS;
	}
	*///?}

	/** The registration-row desc tooltip (the GTClubItem hover shape). */
	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, net.minecraft.world.level.Level aLevel, java.util.List<Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
		super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
		aTooltip.add(Component.translatable(mKind.tooltipKey));
	}
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, java.util.List<Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
	//21.1: the hover signature carries the Item.TooltipContext (the GT6LubricantBucket fork).
		super.appendHoverText(aStack, aContext, aTooltip, aFlag);
		aTooltip.add(Component.translatable(mKind.tooltipKey));
	}
	*///?}

	/** The registration id of a kind (the GT6Tools registration seam). */
	public static String pathOf(Kind aKind) {
		return aKind.name().toLowerCase(Locale.ROOT);
	}
}
