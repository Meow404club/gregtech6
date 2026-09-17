package gregtech6.items.tools;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.network.chat.Component;
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

import gregapi.data.MT;

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
 *
 * <p>MATERIAL LADDER (task p31-blade-ladder): an identity stack carries its material in
 * {@code GT.ToolStats} (the p31-identity-seam) and the item reads it per stack:
 * <ul>
 * <li>durability — {@link GT6ToolLadder#durabilityPoints} over
 *     {@link GT6ToolLadder#statsOf} (the payload {@code j}; Steel → the family 512,
 *     the identity-less arm bit-exact);</li>
 * <li>attack — {@link GT6ToolLadder#attackDamage} (the upstream
 *     {@code getBaseDamage + mToolQuality} fold MultiItemTool.java:392, UNLOCKED here —
 *     the t1 pin was the identity-less bare face; a Steel sword → 6.0F);</li>
 * <li>dig speed — {@code getSpeedMultiplier × mToolSpeed} (MultiItemTool.getDigSpeed
 *     :483; Steel → the 6.0F anchor);</li>
 * <li>attack speed — NO material axis (no 1.7.10 source; the GTCEu-modern precedent
 *     IGTTool.java:208-211 defaults the material term to 0 — the shape anchor stands);</li>
 * <li>name — the material-filled {@link #NAME_TEMPLATE_KEY} template;</li>
 * <li>tint — {@link #tintARGB} index 0 = the head (primary, Steel fallback :119
 *     verbatim), index 2 = the handle (secondary, Spruce fallback :119 verbatim),
 *     the overlays -1.</li>
 * </ul>
 * Identity-less stacks (every pre-ladder stack) keep the legacy arm verbatim.
 */
public class GTSwordItem extends Item implements GT6ToolLadder.LadderTool {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :70-72 — 4.0F kept verbatim as the main-hand attribute. */
	public static final float ATTACK_DAMAGE = 4.0F;

	/** The vanilla sword attack-rate anchor (SwordItem.java:30 −2.4F on the 4.0 base). */
	public static final float ATTACK_SPEED = -2.4F;

	/** The dig speed on the sword surface — the iron-tier anchor (upstream speed ×1.0). */
	public static final float MINING_SPEED = 6.0F;

	/** Upstream getMaxDurabilityMultiplier :80-82 — 1.0F (the payload {@code j} factor). */
	public static final float DURABILITY_MULTIPLIER = 1.0F;

	/** Upstream getSpeedMultiplier :75-77 — 1.0F (the :483 dig-speed factor). */
	public static final float SPEED_MULTIPLIER = 1.0F;

	//? if forge {

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

	/** The ladder-form face (the RCON material arm's polymorphic read). */
	@Override
	public float durabilityMultiplier() {
		return DURABILITY_MULTIPLIER;
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

	/**
	 * The per-material dig speed (MultiItemTool.getDigSpeed :483, the level gate first):
	 * a quality-starved stack returns ZERO on the too-hard surface (the dig family
	 * order), else the surface speed = {@link #SPEED_MULTIPLIER} × the primary
	 * {@code mToolSpeed} — the {@code materialOf} Steel fallback reproduces the
	 * {@link #MINING_SPEED} anchor bit-exact for identity-less stacks; the hand speed
	 * elsewhere.
	 */
	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		if (!mines(aState)) return 1.0F;
		if (GT6ToolLadder.qualityGate(aStack, aState)) return 0.0F; // the :482 zero-speed floor
		return GT6ToolLadder.speed(SPEED_MULTIPLIER, GT6ToolLadder.materialOf(aStack));
	}

	/**
	 * The stack durability read (the ladder face — the GT6ToolLadder javadoc): the
	 * payload {@code j} at the pinned ratio, the family value when identity-less.
	 */
	@Override
	public int getMaxDamage(ItemStack aStack) {
		return GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(aStack, DURABILITY_MULTIPLIER));
	}

	/**
	 * The per-stack attack face (MultiItemTool.java:392): the material
	 * {@code mToolQuality} rides the DAMAGE term only — the speed term is the shape
	 * anchor {@link #ATTACK_SPEED} (no upstream material axis, the class javadoc).
	 * Per-stack override — forge: IForgeItem.getAttributeModifiers(EquipmentSlot,
	 * ItemStack) (IForgeItem.java:61-62, the ItemStack.getAttributeModifiers(slot) route).
	 */
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
		//21.1: the per-stack hook — IItemExtension.getDefaultAttributeModifiers(ItemStack)
		//(javap neoforge-21.1.249 universal jar); the routing proof: IItemStackExtension
		//.getAttributeModifiers falls back to it when the stack carries no
		//ATTRIBUTE_MODIFIERS component (disassembled default method).
		return buildAttackModifiers(GT6ToolLadder.attackDamage(aStack, ATTACK_DAMAGE), ATTACK_SPEED);
	}
	*///?}

	/** The per-material name — the shared composed face ("Sword (Steel)"). */
	@Override
	public Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
	}

	/**
	 * The runtime tint (upstream GT_Tool_Sword.getRGBa :118-120): index 0 = the head
	 * layer (primary, the {@code getPrimaryMaterial(aStack, MT.Steel)} fallback
	 * verbatim), index 2 = the handle layer (secondary, the Spruce fallback verbatim),
	 * the overlay layers = the {@code -1} no-tint sentinel.
	 */
	public static int tintARGB(ItemStack aStack, int aTintIndex) {
		return GT6ToolLadder.bladeTintARGB(aStack, aTintIndex, false);
	}

	/**
	 * The stackless floor the 1.20.1 break path consults (the dig family form: the
	 * quality-correct steel-or-better semantics; 21.1 has no stackless form — the
	 * stack-aware overload below is the only face).
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

	/** The stack-aware authorization (the dig 567491b97 ruling) — the RCON/test face. */
	public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
		return mines(aState) && !GT6ToolLadder.qualityGate(aStack, aState);
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
