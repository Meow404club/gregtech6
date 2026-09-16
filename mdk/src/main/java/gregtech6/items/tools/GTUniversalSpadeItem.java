package gregtech6.items.tools;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.items.tools.loot.GT6ToolLootModifiers;

/**
 * The formal GT6 universal spade — item id {@code gt6:universal_spade} (task
 * p29-w5-t1-dig-six). Upstream GT_Tool_UniversalSpade.java:44-135 — the five-face
 * adventure tool ({@code canCollect/canBlock/isCrowbar/isWeapon} all true :85-88):
 * <ul>
 * <li><b>Mining surface</b> (:91-95), arm by arm:
 *   <ul>
 *   <li>{@code BlockRailBase} → {@link BaseRailBlock} instanceof (the crowbar verbatim
 *       mapping).</li>
 *   <li>{@code BlocksGT.openableCrowbar} → {@link GT6ToolLootModifiers#OPENABLE_CROWBAR}
 *       (the CS.java:1688 set — also the loot-seam key, below).</li>
 *   <li>harvest tools {@code shovel/axe/saw/crowbar} → the vanilla
 *       {@code #mineable/shovel} + {@code #mineable/axe} tags (the saw arm folds into
 *       the axe tag — the 1.7.10 saw-harvest universe IS the wood universe; the crowbar
 *       harvest arm = the openable set above). The TOOL_sword arm + the
 *       leaves/vine/web/plants/cloth/carpet materials → the vanilla
 *       {@code #leaves}/{@code #sword_efficient}/{@code #wool}/{@code #wool_carpets}
 *       tags + {@link #PLANT_FAMILY} (the explicit residual set: gourds, cactus, cake,
 *       tnt, sponges, cobweb, the vine pair — 1.20.1 has no Material).</li>
 *   <li>{@code Material.circuits} → CUT: the crowbar owns the redstone-IO set in this
 *       port (the GTCrowbarItem CIRCUITS_FAMILY javadoc; the flat universal spade's
 *       crowbar face = openable blocks + rails only — declared deviation, the set lives
 *       behind the forbidden 10-item wall).</li>
 *   </ul></li>
 * <li><b>The openable conversion</b> (:98-115 {@code convertBlockDrops}) → the loot seam
 *     ({@code GT6ToolLootModifiers} mode {@code UNBOXINATOR_OPEN}, the per-tool GLM
 *     JSON): the openable blocks' drops route through {@code RM.Unboxinator.findRecipe} —
 *     identity until unpack rows land. NOT in this class (the card RED LINE).</li>
 * <li><b>Behavior arms</b> (:128-135): Plug_Leak CUT (no leak face), Place_Path(50) →
 *     {@link GTPickaxeItem#createPath}, Place_Paddy(50) CUT-inert (the GrowthCraft
 *     gate), the two {@code Behavior_Tool} rows = the mining face itself,
 *     Place_Torch → {@link GTPickaxeItem#placeTorchFromInventory}.</li>
 * <li><b>canCollect / canBlock</b> (:85-86) → CUT (the entity-drop / item-blocking
 *     pipelines do not exist in the flat port; the crowbar precedents).</li>
 * <li><b>Damage</b>: base 3.0F (:71-73); per-block 100 / per-attack 100 (:46-47/:61-63)
 *     fold into the single point each.</li>
 * </ul>
 *
 * <p>Durability 512 (the family value; upstream durability multiplier 1.0). Classification
 * red line: NEVER {@link GT6ToolActions#CROWBAR} — the cover-dismantle dispatch keys on
 * the crowbar action, and a universal spade carrying it would fire the cover UI (the
 * GTCrowbarItem HOE_DIG red line, mirrored).
 */
public class GTUniversalSpadeItem extends Item {

	/** The family value (512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :71-73 — 3.0F kept verbatim (isWeapon :88). */
	private static final float ATTACK_DAMAGE = 3.0F;

	/** Upstream getSpeedMultiplier :76-78 — the 6.0F anchor × 0.75. */
	public static final float MINING_SPEED = GTPickaxeItem.MINING_SPEED * 0.75F;

	/**
	 * The explicit residual plant set — the upstream Material tests with no tag carrier:
	 * vine pair, cobweb, cactus, the gourd four, cake, tnt, the sponge pair.
	 */
	static final ImmutableSet<Block> PLANT_FAMILY = ImmutableSet.of(
			Blocks.VINE, Blocks.GLOW_LICHEN, Blocks.COBWEB, Blocks.CACTUS,
			Blocks.PUMPKIN, Blocks.CARVED_PUMPKIN, Blocks.JACK_O_LANTERN, Blocks.MELON,
			Blocks.CAKE, Blocks.TNT, Blocks.SPONGE, Blocks.WET_SPONGE);

	/**
	 * The upstream snow/craftedSnow Material arms (GT_Tool_UniversalSpade.java:94) — the
	 * layer + the full block as an explicit set (offline-pinnable; the mineable/shovel
	 * tag arm carries them at runtime too).
	 */
	static final ImmutableSet<Block> SNOW_FAMILY = ImmutableSet.of(Blocks.SNOW, Blocks.SNOW_BLOCK);

	//? if forge {
	private final Multimap<Attribute, AttributeModifier> mAttackModifiers = ImmutableMultimap.of(
			Attributes.ATTACK_DAMAGE,
			new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", (double) ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION));
	//?} else {
	/*// 21.1: the ItemAttributeModifiers form (the GTCrowbarItem fork verbatim).
	private final net.minecraft.world.item.component.ItemAttributeModifiers mAttackModifiers = net.minecraft.world.item.component.ItemAttributeModifiers
			.builder()
			.add(Attributes.ATTACK_DAMAGE,
					new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, (double) ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
					net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
			.build();
	*///?}

	public GTUniversalSpadeItem(Properties aProperties) {
		super(aProperties);
	}

	/** The upstream isMinableBlock :91-95 modern form — the five-face union. */
	public static boolean mines(BlockState aState) {
		Block tBlock = aState.getBlock();
		return tBlock instanceof BaseRailBlock
				|| GT6ToolLootModifiers.OPENABLE_CROWBAR.contains(tBlock)
				|| aState.is(BlockTags.MINEABLE_WITH_SHOVEL)
				|| aState.is(BlockTags.MINEABLE_WITH_AXE)
				|| aState.is(BlockTags.LEAVES)
				|| aState.is(BlockTags.SWORD_EFFICIENT)
				|| aState.is(BlockTags.WOOL)
				|| aState.is(BlockTags.WOOL_CARPETS)
				|| PLANT_FAMILY.contains(tBlock)
				|| SNOW_FAMILY.contains(tBlock)
				|| GTShovelItem.FIRE_FAMILY.contains(tBlock);
	}

	/** The dig-speed seam — the ×0.75 universal speed on the surface. */
	public static float destroySpeedBonus(BlockState aState) {
		return mines(aState) ? MINING_SPEED : 1.0F;
	}

	/** The drop-authorization half (the family iron-tier gate; openable blocks are not gated). */
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

	/** Upstream getToolDamagePerBlockBreak :46-48 — 100 units fold into one point. */
	@Override
	public boolean mineBlock(ItemStack aStack, net.minecraft.world.level.Level aLevel, BlockState aState,
			net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack :61-63 — 100 units fold into one point. */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}

	/** The upstream arm row :128-135 minus the cuts: path, then torch. */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		if (GTPickaxeItem.createPath(aContext)) {
			return InteractionResult.sidedSuccess(aContext.getLevel().isClientSide());
		}
		if (GTPickaxeItem.placeTorchFromInventory(aContext)) {
			return InteractionResult.sidedSuccess(aContext.getLevel().isClientSide());
		}
		return InteractionResult.PASS;
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
	 * The stack classifier — the gt6_universal_spade action + the three vanilla faces
	 * (SHOVEL/AXE/SWORD dig). NEVER the crowbar action (the classification red line, the
	 * class javadoc tail).
	 */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.UNIVERSAL_SPADE == aToolAction
				|| net.minecraftforge.common.ToolActions.SHOVEL_DIG == aToolAction
				|| net.minecraftforge.common.ToolActions.AXE_DIG == aToolAction
				|| net.minecraftforge.common.ToolActions.SWORD_DIG == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
