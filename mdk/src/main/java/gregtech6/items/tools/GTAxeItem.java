package gregtech6.items.tools;

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
 * The formal GT6 axe — item id {@code gt6:axe} (task p29-w5-t2-blade-six; the
 * {@link GTPickaxeItem} class shape). Upstream GT_Tool_Axe.java:51-171:
 * <ul>
 * <li><b>Damage</b> — base damage 3.0F (:78-80) verbatim; per-block 50 (:53-55) folds to
 *     one point; per-entity 200 (:58-60) folds to one point. Attack rate: the vanilla
 *     iron-axe anchor −3.0F (1.0 attacks/s; no 1.7.10 source).</li>
 * <li><b>Mining surface</b> (:98-100) — the {@code TOOL_axe} harvest arm → the vanilla
 *     {@code #minecraft:mineable/axe} tag (live/RCON; the wood barrel rides the gt6
 *     band), {@code Material.leaves}/{@code Material.vine}/{@code Material.cactus} →
 *     {@link BlockTags#LEAVES} + {@link Blocks#VINE} + {@link Blocks#CACTUS}, the
 *     {@code BlockHugeMushroom} instanceof → {@link #isFellable}'s mushroom arm. The
 *     plants/gourd/coral residual arms stay pooled.</li>
 * <li><b>The whole-tree felling</b> (:102-125) — NOT here (the card RED LINE): it rides
 *     the loot seam as the {@code GT6TreeFellModifier} (the GT6ToolSweep guard as the
 *     upstream {@code LOCK}; the TreeCap/dynamic-trees mod gates are the declared-F
 *     cut, vanilla trees only; the FAST_LEAF_DECAY arm is cut — vanilla leaf decay
 *     supersedes). The sneak gate (:107 {@code !isSneaking}) lives in the modifier.</li>
 * <li><b>Speed ×2 on Beam blocks / the mining-speed ladder</b> (:131-141) — CUT (no
 *     modern beam blocks; the ladder rides the vanilla-speed face).</li>
 * <li><b>the EntityEnt log drop (:144-148) / sapling+workbench placement arms</b> —
 *     CUT (the mod-entity face; the placement arms are the interaction-card pool).</li>
 * </ul>
 *
 * <p>Durability 512 (the family value).
 */
public class GTAxeItem extends Item {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :78-80 — 3.0F kept verbatim as the main-hand attribute. */
	public static final float ATTACK_DAMAGE = 3.0F;

	/** The vanilla iron-axe attack-rate anchor (1.0 attacks/s). */
	public static final float ATTACK_SPEED = -3.0F;

	/** The dig speed on the axe surface — the iron-tier anchor (upstream speed ×1.0). */
	public static final float MINING_SPEED = 6.0F;

	/** The registration-row desc (Loader_Tools.java:122 "Faster on Logs. Chops down whole Trees."). */
	public static final String TOOLTIP_KEY = "item.gt6.axe.tooltip";

	//? if forge {
	private final com.google.common.collect.Multimap<Attribute, AttributeModifier> mAttackModifiers = buildAttackModifiers(ATTACK_DAMAGE, ATTACK_SPEED);

	/** The dual-modifier builder — the DiggerItem.java:29-35 shape; the double-axe re-calls with its own constants. */
	protected static com.google.common.collect.Multimap<Attribute, AttributeModifier> buildAttackModifiers(float aDamage, float aSpeed) {
		return com.google.common.collect.ImmutableMultimap.of(
				Attributes.ATTACK_DAMAGE,
				new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", (double) aDamage, AttributeModifier.Operation.ADDITION),
				Attributes.ATTACK_SPEED,
				new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", (double) aSpeed, AttributeModifier.Operation.ADDITION));
	}
	//?} else {
	/*private final net.minecraft.world.item.component.ItemAttributeModifiers mAttackModifiers = buildAttackModifiers(ATTACK_DAMAGE, ATTACK_SPEED);

	// 21.1: the ItemAttributeModifiers form (the GTSwordItem fork verbatim).
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

	public GTAxeItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The upstream isMinableBlock :98-100 modern form — the axe tag + the
	 * leaves/vine/cactus arms (see the javadoc scoping note). Static pure seam.
	 */
	public static boolean mines(BlockState aState) {
		return aState.is(BlockTags.MINEABLE_WITH_AXE)
				|| aState.is(BlockTags.LEAVES)
				|| aState.is(net.minecraft.world.level.block.Blocks.VINE)
				|| aState.is(net.minecraft.world.level.block.Blocks.CACTUS)
				|| aState.getBlock() instanceof net.minecraft.world.level.block.HugeMushroomBlock;
	}

	/**
	 * The felling gate (upstream :107 — {@code isWood}/{@code OP.log}/the huge-mushroom
	 * instanceof; the WoodDictionary/TreeCap/dynamic-trees arms are the declared-F mod
	 * cuts) — the vanilla {@code #minecraft:logs} family + the huge mushroom blocks.
	 * Static pure seam (the modifier and the tests share it).
	 */
	public static boolean isFellable(BlockState aState) {
		return aState.is(BlockTags.LOGS)
				|| aState.getBlock() instanceof net.minecraft.world.level.block.HugeMushroomBlock;
	}

	/** The dig-speed seam — the axe speed on the surface, the hand speed elsewhere. */
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

	/** Upstream getToolDamagePerBlockBreak :53-55 — 50 units fold into one point. */
	@Override
	public boolean mineBlock(ItemStack aStack, Level aLevel, BlockState aState,
			net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack :58-60 — 200 units fold into one point. */
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

	/** The registration-row desc tooltip (the GT6LubricantBucket hover shape). */
	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, net.minecraft.world.level.Level aLevel, java.util.List<net.minecraft.network.chat.Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
		super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
		aTooltip.add(net.minecraft.network.chat.Component.translatable(TOOLTIP_KEY));
	}
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, java.util.List<net.minecraft.network.chat.Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
	//21.1: the hover signature carries the Item.TooltipContext (the GT6LubricantBucket fork).
		super.appendHoverText(aStack, aContext, aTooltip, aFlag);
		aTooltip.add(net.minecraft.network.chat.Component.translatable(TOOLTIP_KEY));
	}
	*///?}

	/** The stack classifier — the gt6 axe action + the vanilla AXE_DIG face. */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.AXE == aToolAction
				|| net.minecraftforge.common.ToolActions.AXE_DIG == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
