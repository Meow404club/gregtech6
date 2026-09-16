package gregtech6.items.tools;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The formal GT6 hoe — item id {@code gt6:hoe} (task p29-w5-t4-field-five). Upstream
 * GT_Tool_Hoe.java:38-113:
 * <ul>
 * <li><b>Mining surface</b> (isMinableBlock :85-87): the {@code TOOL_hoe} harvest arm +
 *     {@code Material.gourd} → the vanilla {@code #minecraft:mineable/hoe} tag (the
 *     hoe-class harvest universe: leaves, hay, wart blocks, sculk, sponge, moss —
 *     mcmeta data/minecraft/tags/block/mineable/hoe.json) + the {@link #GOURD_FAMILY}
 *     explicit set (the 1.7.10 gourd material = pumpkin/melon; the modern
 *     Material.VEGETABLE members ride along — carved_pumpkin/jack_o_lantern are the
 *     same family, declared unfold).</li>
 * <li><b>Damage</b>: base 1.5F (:65-67); per-block 50 / per-attack 200 fold to one
 *     point (the family mapping); the hurt-resistance halving (:80-82) is CUT — the
 *     MultiItemTool attack pipeline does not exist (the p29-w5-t1-dig-six verdict).</li>
 * <li><b>buildHoe achievement</b> (onToolCrafted :105-108,
 *     {@code AchievementList.buildHoe}): CUT — 1.20.1 carries no crafting-hoe
 *     advancement (the advancement tree has no build_hoe node), the recipe-unlock
 *     toast is the vanilla face. Revives with an advancement-domain card.</li>
 * <li><b>Behavior_Tool(TOOL_hoe)</b> (:101): the IBlockToolable machine relay face —
 *     the machine-interaction pool, zero useOn here.</li>
 * </ul>
 *
 * <p>Durability 512 (the family value; upstream getMaxDurabilityMultiplier 1.0,
 * :75-77). The {@code getDestroySpeed} surface speed stands in for the vanilla
 * hoe-class dig anchor.
 */
public class GTHoeItem extends Item {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :65-67 — 1.5F kept verbatim. */
	private static final float ATTACK_DAMAGE = 1.5F;

	/** Upstream getSpeedMultiplier :70-72 = 1.0 — the 6.0F anchor. */
	public static final float MINING_SPEED = 6.0F;

	/**
	 * The upstream {@code Material.gourd} arm — the 1.7.10 gourd material (pumpkin,
	 * melon); the modern Material.VEGETABLE members (carved, jack_o_lantern) ride along
	 * as the declared family unfold. Pumpkin/melon are NOT in mineable/hoe (mcmeta
	 * mineable/hoe.json: leaves+wart+hay+sculk+sponge+moss only) — the set is the live arm.
	 */
	static final ImmutableSet<Block> GOURD_FAMILY = ImmutableSet.of(
			Blocks.PUMPKIN, Blocks.CARVED_PUMPKIN, Blocks.MELON, Blocks.JACK_O_LANTERN);

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

	public GTHoeItem(Properties aProperties) {
		super(aProperties);
	}

	/** The upstream isMinableBlock :85-87 modern form — the hoe tag + the gourd set. */
	public static boolean mines(BlockState aState) {
		return aState.is(BlockTags.MINEABLE_WITH_HOE) || GOURD_FAMILY.contains(aState.getBlock());
	}

	/** The dig-speed seam — the full speed on the surface, ZERO off it (the upstream getDigSpeed face: a GT6 tool mines its surface only). */
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

	/** Upstream getToolDamagePerBlockBreak :40-42 — 50 units fold into one point. */
	@Override
	public boolean mineBlock(ItemStack aStack, net.minecraft.world.level.Level aLevel, BlockState aState,
			net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack :55-57 — 200 units fold into one point. */
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
	 * The stack classifier — the gt6_hoe action ONLY. RED LINE: never
	 * {@code ToolActions.HOE_DIG} — the three wrench-substitute predicates
	 * (GTOvenBlock.use / GTFluidPipeBlock.use / GTWrenchHighlightListener) key on
	 * HOE_DIG, and a hoe that classified there would fire the wrench UI everywhere
	 * (the GT6ToolActions crowbar ruling; the upstream hoe carries no TOOL_wrench face).
	 */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.HOE == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
