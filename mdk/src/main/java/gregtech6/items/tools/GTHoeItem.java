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
 * <p>Durability ladder (task p31-dig-ladder): the {@link GT6ToolLadder} form over the
 * stack's identity — durability j/100, speed ×1.0 × mToolSpeed, the :482 quality gate;
 * the identity-less arm = Steel bit-exact (512 / 6.0F, the pre-ladder constants).
 */
public class GTHoeItem extends Item implements GT6ToolLadder.LadderTool {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :65-67 — 1.5F kept verbatim. */
	private static final float ATTACK_DAMAGE = 1.5F;

	/** Upstream getSpeedMultiplier :70-72 = 1.0 — the 6.0F anchor. */
	public static final float MINING_SPEED = 6.0F;

	/** The form durability multiplier (upstream :75-77 = 1.0). */
	public static final float DURABILITY_MULTIPLIER = 1.0F;

	/** The form speed multiplier (upstream getSpeedMultiplier :70-72 = 1.0). */
	public static final float SPEED_MULTIPLIER = 1.0F;

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

	/** The dig-speed seam — the full speed on the surface, ZERO off it (the upstream getDigSpeed face: a GT6 tool mines its surface only) — the stack-free steel arm. */
	public static float destroySpeedBonus(BlockState aState) {
		return mines(aState) ? MINING_SPEED : 0.0F;
	}

	/** The ladder dig-speed seam — ×1.0 × the stack's material speed on the surface, ZERO off it. */
	public static float destroySpeedBonus(ItemStack aStack, BlockState aState) {
		return mines(aState) ? GT6ToolLadder.speed(SPEED_MULTIPLIER, GT6ToolLadder.materialOf(aStack)) : 0.0F;
	}

	/**
	 * The drop authorization — the stack-aware face (forge 1.20.1 IForgeItem overload,
	 * 1.21.1 the vanilla signature) over the quality gate.
	 */
	@Override
	public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
		return !GT6ToolLadder.qualityGate(aStack, aState) && coarseFloor(aState);
	}

	/** The quality-blind floor (the family iron-tier gate). */
	static boolean coarseFloor(BlockState aState) {
		return mines(aState) && !aState.is(BlockTags.NEEDS_DIAMOND_TOOL);
	}

	//? if forge {
	/** The stackless floor the 1.20.1 break path consults (the family iron-tier gate). */
	@Override
	public boolean isCorrectToolForDrops(BlockState aState) {
		return coarseFloor(aState);
	}
	//?}

	/** The dig-speed half (the level gate first, the upstream :482 order). */
	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		if (GT6ToolLadder.qualityGate(aStack, aState)) return 0.0F;
		return destroySpeedBonus(aStack, aState);
	}

	/** The form multiplier read (the {@link GT6ToolLadder.LadderTool} face). */
	@Override
	public float durabilityMultiplier() {
		return DURABILITY_MULTIPLIER;
	}

	/** The per-material durability (the {@link GT6ToolLadder} j/100 points — Steel fallback = 512). */
	@Override
	public int getMaxDamage(ItemStack aStack) {
		return GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(aStack, durabilityMultiplier()));
	}

	/** The composed display name — "Hoe (Bronze)"; bare for identity-less stacks. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
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
