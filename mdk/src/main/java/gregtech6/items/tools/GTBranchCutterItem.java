package gregtech6.items.tools;

import java.util.List;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The formal GT6 branch cutter — item id {@code gt6:branch_cutter} (task
 * p29-w5-t4-field-five). Upstream GT_Tool_BranchCutter.java:42-138 — the Grafter-class
 * leaf tool:
 * <ul>
 * <li><b>Mining surface</b> (isMinableBlock :121-123): the {@code grafter} harvest arm
 *     (GT6-block-only — structurally empty in this universe, declared) + the vanilla
 *     leaves + {@code Blocks.vine} (the {@code Material.leaves} and vine arms; the
 *     {@code IL.TF_Mazehedge} arm is CUT — the mod gate, vanilla block universe only).</li>
 * <li><b>The leaf conversion</b> (convertBlockDrops :84-92) → {@link #convertLeaves}:
 *     breaking a vanilla leaf block with the cutter REPLACES the drops with the
 *     matching sapling — oak carries the apple roll (:86); leaves2 splits
 *     acacia/dark-oak (:87-89); the vine drops itself (:90-92); the mod arms
 *     (BlockBaseLeaves/IC2/Aether :93-115) are CUT (absent blocks). Rides the
 *     {@code GT6ToolLootModifiers} loot seam (mode {@code BRANCHCUTTER_LEAVES}, the
 *     per-tool GLM JSON gated by {@code gt6:holds_tool}).</li>
 * <li><b>The Grafter drop-chance floor</b> (:83): {@code dropChance = max(dropChance,
 *     (bind4(harvestLevel)+1)*0.2)} → {@link #dropChanceFloor} — the pure function is
 *     the port face; the live event-chance seam does not exist in the Global-Loot-
 *     Modifier chain (the modifier chain sees already-rolled drops), so the floor is
 *     declared formula-only (the FieldFiveTest pin). The single steel tier folds
 *     {@code getHarvestLevel(stack)} to the steel level 2 → the applied floor 0.6F.</li>
 * <li><b>Damage</b>: base 2.5F (:64-66); the 100-unit break/conversion/attack rows
 *     fold to one point; speed ×0.25 (:69-71) → 1.5F.</li>
 * </ul>
 *
 * <p>Durability 128 — 512 × the upstream ×0.25 multiplier (:74-76; the GTPickaxeGem
 * 128 precedent).
 */
public class GTBranchCutterItem extends Item {

	/** The ×0.25 multiplier fold (upstream getMaxDurabilityMultiplier :74-76). */
	public static final int DURABILITY_POINTS = 128;

	/** Upstream getBaseDamage :64-66 — 2.5F kept verbatim. */
	private static final float ATTACK_DAMAGE = 2.5F;

	/** Upstream getSpeedMultiplier :69-71 = 0.25 — the 6.0F anchor × 0.25. */
	public static final float MINING_SPEED = 6.0F * 0.25F;

	/** The single-steel-tier harvest level (the :83 getHarvestLevel fold; steel = 2). */
	public static final int STEEL_QUALITY = 2;

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

	public GTBranchCutterItem(Properties aProperties) {
		super(aProperties);
	}

	/** The upstream isMinableBlock :121-123 modern form — leaves + vine (the grafter arm is the declared empty face). */
	public static boolean mines(BlockState aState) {
		return aState.is(BlockTags.LEAVES) || aState.is(Blocks.VINE);
	}

	/** The dig-speed seam — full speed on the surface, ZERO off it (the upstream getDigSpeed face). */
	public static float destroySpeedBonus(BlockState aState) {
		return mines(aState) ? MINING_SPEED : 0.0F;
	}

	/** The drop-authorization half. */
	@Override
	//? if forge {
	public boolean isCorrectToolForDrops(BlockState aState) {
	//?} else {
	/*public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
	//21.1: the stack parameter joined the signature (the GTCrowbarItem fork).
	*///?}
		return mines(aState);
	}

	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		return destroySpeedBonus(aState);
	}

	/** Upstream getToolDamagePerBlockBreak :44-46 — 100 units fold into one point. */
	@Override
	public boolean mineBlock(ItemStack aStack, Level aLevel, BlockState aState,
			net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack :59-61 — 100 units fold into one point. */
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
	 * The Grafter drop-chance floor (:83 verbatim over the bound):
	 * {@code min(1.0, max(dropChance, (bind4(quality)+1)*0.2))} — quality 0..3 →
	 * 0.2/0.4/0.6/0.8 (bind4 clamps to 0..15, UT.java:1556). The caller's own
	 * dropChance rides the Math.max — the pure function returns the FLOOR value.
	 */
	public static float dropChanceFloor(int aQuality) {
		int tBound = Math.max(0, Math.min(15, aQuality)); // UT.Code.bind4
		return Math.min(1.0F, (tBound + 1) * 0.2F);
	}

	/** The steel-tier applied floor — getHarvestLevel folds to the steel level 2 → 0.6F. */
	public static float steelDropChanceFloor() {
		return dropChanceFloor(STEEL_QUALITY);
	}

	/**
	 * The leaf conversion (:84-92) — REPLACES the drops with the matching sapling; oak
	 * carries the apple roll (:86). The 1.7.10 leaves metadata splits into the modern
	 * block identities (oak/spruce/birch/jungle rode Blocks.leaves meta 0-3; acacia/
	 * dark-oak rode Blocks.leaves2 meta 0-1 + 4). Other leaf blocks (cherry/mangrove/
	 * azalea — modern additions outside the 1.7.10 universe) ride through untouched
	 * (declared cut).
	 *
	 * @return whether the conversion fired
	 */
	public static boolean convertLeaves(BlockState aState, List<ItemStack> aDrops, int aFortune, RandomSource aRandom) {
		if (aState == null) return false;
		Block tBlock = aState.getBlock();
		if (tBlock == Blocks.OAK_LEAVES) {
			aDrops.clear();
			if (appleArm(aFortune, aRandom)) {
				aDrops.add(new ItemStack(Items.APPLE)); // IL.Food_Apple_Red → the vanilla apple (the only apple identity)
			} else {
				aDrops.add(new ItemStack(Blocks.OAK_SAPLING));
			}
			return true;
		}
		if (tBlock == Blocks.SPRUCE_LEAVES) return replace(aDrops, Blocks.SPRUCE_SAPLING);
		if (tBlock == Blocks.BIRCH_LEAVES) return replace(aDrops, Blocks.BIRCH_SAPLING);
		if (tBlock == Blocks.JUNGLE_LEAVES) return replace(aDrops, Blocks.JUNGLE_SAPLING);
		if (tBlock == Blocks.ACACIA_LEAVES) return replace(aDrops, Blocks.ACACIA_SAPLING);
		if (tBlock == Blocks.DARK_OAK_LEAVES) return replace(aDrops, Blocks.DARK_OAK_SAPLING);
		if (tBlock == Blocks.VINE) return replace(aDrops, Blocks.VINE); // :90-92 the vine drops itself (the shears-only vanilla face)
		return false;
	}

	/**
	 * The apple roll (:86 verbatim {@code nextInt(9) <= fortune*2}) with the declared
	 * fortune-0 floor: upstream rolls 1-in-9 at fortune 0; the port floors it to NO
	 * apple (the deterministic RCON face — the acceptance ruling) and keeps the exact
	 * roll shape for fortune ≥ 1.
	 */
	public static boolean appleArm(int aFortune, RandomSource aRandom) {
		return aFortune > 0 && aRandom.nextInt(9) <= aFortune * 2;
	}

	private static boolean replace(List<ItemStack> aDrops, Block aReplacement) {
		aDrops.clear();
		aDrops.add(new ItemStack(aReplacement));
		return true;
	}

	/** The stack classifier — the gt6_branch_cutter action (no vanilla face; the grafter class has no vanilla action). */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.BRANCH_CUTTER == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
