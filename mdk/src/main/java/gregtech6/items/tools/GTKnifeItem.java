package gregtech6.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import com.google.common.collect.Multimap;


/**
 * The formal GT6 knife — item id {@code gt6:knife} (task p29-w5-t2-blade-six). Upstream
 * GT_Tool_Knife.java:26-96 is a {@code GT_Tool_Sword} SUBCLASS (the Loader_Tools.java:135
 * registration row, {@code 1*U} material amount) — the port keeps the subclass face:
 * <ul>
 * <li><b>Damage</b> — base damage 2.0F (:56-59) verbatim; the attack-rate anchor stays
 *     the vanilla sword −2.4F (no 1.7.10 source); per-entity 200 (:49-52) folds to one
 *     point (the inherited {@link GTSwordItem#hurtEnemy}).</li>
 * <li><b>Mining face</b> — NO upstream override: the sword surface inherits
 *     ({@link GTSwordItem#mines}), at the ×0.5 speed multiplier (:63-66).</li>
 * <li><b>canCollect</b> (:75) — CUT (no 1.20.1 vanilla face; the scissors/shears pool).
 * </ul>
 *
 * <p>MATERIAL LADDER (task p31-blade-ladder — the {@link GTSwordItem} javadoc carries the
 * family face): durability/attack/dig-speed ride the shared {@link GT6ToolLadder} reads
 * with the knife constants; the attack speed stays the shape anchor. TINT: upstream
 * renders the knife through the sword {@code getRGBa} inheritance with the head pass
 * {@code VOID} (GT_Tool_Knife.getIcon :73-75) — the visible sprite takes the
 * SECONDARY (handle) colour, the Spruce fallback verbatim (the sword :119
 * {@code getRGBa(false)} face) — so {@link #tintARGB} index 0 = secondary.
 */
public class GTKnifeItem extends GTSwordItem {

	/** Upstream getBaseDamage :56-59 — 2.0F kept verbatim. */
	public static final float ATTACK_DAMAGE = 2.0F;

	/** The vanilla sword attack-rate anchor (the base constant; no upstream source). */
	public static final float ATTACK_SPEED = GTSwordItem.ATTACK_SPEED;

	/** Upstream getSpeedMultiplier :63-66 — 0.5F (the :483 dig-speed factor). */
	public static final float SPEED_MULTIPLIER = 0.5F;

	/** The legacy dig-speed anchor — the 6.0F anchor × 0.5 (the identity-less arm). */
	public static final float MINING_SPEED = GTSwordItem.MINING_SPEED * 0.5F;

	/** The legacy family value (512 — the identity-less arm; same as the sword). */
	public static final int DURABILITY_POINTS = GTSwordItem.DURABILITY_POINTS;

	/** Upstream getMaxDurabilityMultiplier :66-68 — 1.0F. */
	public static final float DURABILITY_MULTIPLIER = 1.0F;

	public GTKnifeItem(Properties aProperties) {
		super(aProperties);
	}

	/** The dig-speed seam — the knife speed on the inherited sword surface (the legacy anchor). */
	public static float destroySpeedBonus(net.minecraft.world.level.block.state.BlockState aState) {
		return mines(aState) ? MINING_SPEED : 1.0F;
	}

	/**
	 * The per-material dig speed (the :483 formula over the sword surface): the knife
	 * {@link #SPEED_MULTIPLIER} × the primary {@code mToolSpeed} — the Steel fallback
	 * reproduces the {@link #MINING_SPEED} anchor (3.0F) for identity-less stacks.
	 */
	@Override
	public float getDestroySpeed(ItemStack aStack, net.minecraft.world.level.block.state.BlockState aState) {
		if (!mines(aState)) return 1.0F;
		return GT6ToolLadder.speed(SPEED_MULTIPLIER, GT6ToolLadder.materialOf(aStack));
	}

	/** The per-stack attack face — the knife base over the shared :392 fold (the sword override shape). */
	//? if forge {
	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot aSlot, ItemStack aStack) {
		return aSlot == EquipmentSlot.MAINHAND
				? buildAttackModifiers(GT6ToolLadder.attackDamage(aStack, ATTACK_DAMAGE), ATTACK_SPEED)
				: super.getAttributeModifiers(aSlot, aStack);
	}
	//?} else {
	/*@Override
	public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack aStack) {
		//21.1: the per-stack hook (the GTSwordItem fork — the routing proof lives there).
		return buildAttackModifiers(GT6ToolLadder.attackDamage(aStack, ATTACK_DAMAGE), ATTACK_SPEED);
	}
	*///?}

	/** The per-material name — the shared composed face ("Knife (Steel)"). */
	@Override
	public Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
	}

	/**
	 * The runtime tint — the VISIBLE sprite takes the secondary colour (the class
	 * javadoc; the upstream head pass is {@code VOID}): index 0 = secondary (Spruce
	 * fallback), the overlay = the {@code -1} sentinel.
	 */
	public static int tintARGB(ItemStack aStack, int aTintIndex) {
		return GT6ToolLadder.bladeTintARGB(aStack, aTintIndex, true);
	}

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
