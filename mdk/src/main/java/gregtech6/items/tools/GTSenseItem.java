package gregtech6.items.tools;

import java.util.List;

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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.items.tools.loot.GT6ToolSweep;

/**
 * The formal GT6 sense — item id {@code gt6:sense} (task p29-w5-t4-field-five; the
 * upstream tooltip: "Because a Scythe doesn't make Sense"). Upstream GT_Tool_Sense.java
 * :46-111:
 * <ul>
 * <li><b>Mining surface</b> (isMinableBlock :67-73): the {@code TOOL_sense}/
 *     {@code TOOL_scythe} harvest arms (GT6-block-only — structurally empty in this
 *     universe, declared) + the lily-pad EXCLUSION (:68) + {@code Material.plants} +
 *     {@code Material.leaves} + {@code Material.vine}. Modern form: the
 *     {@code #minecraft:leaves} tag + {@code Blocks.VINE} + the plant composition —
 *     {@link #GRASS_FAMILY} (the grass/fern/dead-bush/cane identity set) + the vanilla
 *     plant tags ({@code small_flowers}/{@code tall_flowers}/{@code crops}/
 *     {@code saplings}; the tag membership rides the modern vanilla tag as-is — members
 *     the 1.7.10 universe lacked ride along). The lily pad is in NO arm — the exclusion
 *     is pinned by the FieldFiveTest (upstream :68 returned F for the vanilla
 *     {@code BlockLilyPad} arm). The water plants (kelp/seagrass — the modern
 *     water-plant material, not 1.7.10 Material.plants) and bamboo are CUT.</li>
 * <li><b>The 3x3 sweep</b> (convertBlockDrops :78-86): every break re-harvests the 26
 *     dig-speed-passing neighbours — {@link GT6ToolSweep#sweep} on
 *     {@link #mineBlock}, the same face as the plow.</li>
 * <li><b>The grass/stick conversion</b> (harvestGrass/harvestStick :87-88 →
 *     ToolStats.java:111-176) → {@link #convertVegetal}: the grass/fern break drops
 *     the plant item itself (the IL.Grass fodder food is UNPORTED — MultiItemFood.java
 *     :53 — so the arm rides the vanilla block item, the OD.itemGrassTall→OD.itemGrass
 *     re-registration face, LoaderOreDictReRegistrations.java:624: the vanilla grass
 *     item IS a "grass" member); the 2-tall plants pay the double (:118-123); the dead
 *     bush pays the guaranteed stick (the upstream 1+nextInt(2+fortune) count folded
 *     to the deterministic minimum 1 — the modifier chain carries no fortune; the
 *     vanilla 0-2 stick roll is replaced). Rides the loot seam (mode
 *     {@code SENSE_VEGETAL}).</li>
 * <li><b>Weapon face</b>: base damage 3.0F (:50-52) kept; isWeapon/canBehead (:58-64)
 *     CUT — no attack pipeline. Durability ×4.0 (:54-56).</li>
 * </ul>
 *
 * <p>Durability 2048 — 512 × the upstream ×4.0 multiplier (the GTPickaxeGem ×0.25 = 128
 * fold precedent).
 */
public class GTSenseItem extends Item {

	/** The ×4.0 multiplier fold (upstream getMaxDurabilityMultiplier :54-56). */
	public static final int DURABILITY_POINTS = 2048;

	/** Upstream getBaseDamage :50-52 — 3.0F kept verbatim. */
	private static final float ATTACK_DAMAGE = 3.0F;

	/** Upstream getSpeedMultiplier — default 1.0 — the 6.0F anchor. */
	public static final float MINING_SPEED = 6.0F;

	/**
	 * The grass/fern/dead-bush identity arm — the 1.7.10 tallgrass/double_plant/deadbush
	 * faces with no covering vanilla tag. 1.20.3+ rename fork: the 1-block grass plant
	 * is {@code Blocks.GRASS} on this leg, {@code Blocks.SHORT_GRASS} on 21.1 (the swap
	 * table carries no Blocks rename).
	 */
	static final ImmutableSet<Block> GRASS_FAMILY = ImmutableSet.of(
			//? if forge {
			Blocks.GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
			//?} else {
			/*Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
			*///?}
			Blocks.DEAD_BUSH, Blocks.SUGAR_CANE, Blocks.NETHER_WART, Blocks.COCOA);

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

	public GTSenseItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The upstream isMinableBlock :67-73 modern form — leaves + vine + the plant
	 * composition. The lily pad sits in NO arm (the :68 exclusion face).
	 */
	public static boolean mines(BlockState aState) {
		return aState.is(BlockTags.LEAVES) || aState.is(Blocks.VINE)
				|| GRASS_FAMILY.contains(aState.getBlock())
				|| aState.is(BlockTags.SMALL_FLOWERS) || aState.is(BlockTags.TALL_FLOWERS)
				|| aState.is(BlockTags.CROPS) || aState.is(BlockTags.SAPLINGS);
	}

	/** The dig-speed seam — full speed on the surface, ZERO off it (the sweep gate + the upstream getDigSpeed face). */
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

	/**
	 * The centre-break payment + the 3x3 sweep (the upstream convertBlockDrops :78-86
	 * arm; each neighbour pays its own point through this same mineBlock).
	 */
	@Override
	public boolean mineBlock(ItemStack aStack, Level aLevel, BlockState aState,
			net.minecraft.core.BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide) {
			if (aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
				aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
			}
			if (aEntity instanceof net.minecraft.server.level.ServerPlayer tPlayer) {
				GT6ToolSweep.sweep(tPlayer, aStack, aPos);
			}
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack (default 100) — one point. */
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
	 * The grass/stick conversion (the harvestGrass/harvestStick port) — REPLACES the
	 * vanilla drops: the grass/fern faces pay the plant item itself (1, or 2 for the
	 * 2-tall plants — ToolStats.java:120), the dead bush pays the guaranteed stick.
	 * Everything else rides through (returns false).
	 *
	 * @return whether the conversion fired
	 */
	public static boolean convertVegetal(BlockState aState, List<ItemStack> aDrops) {
		if (aState == null) return false;
		Block tBlock = aState.getBlock();
		if (tBlock == Blocks.DEAD_BUSH) { // harvestStick :161-164 (the 1.7.10 tallgrass-shrub face rides here with its block)
			aDrops.clear();
			aDrops.add(new ItemStack(Items.STICK)); // the 1+nextInt(2+fortune) count folded to the deterministic minimum
			return true;
		}
		// the 1-block grass name forks per leg (GRASS here / SHORT_GRASS on 21.1)
		boolean tIsGrass = false;
		//? if forge {
		tIsGrass = tBlock == Blocks.GRASS;
		//?} else {
		/*tIsGrass = tBlock == Blocks.SHORT_GRASS;
		*///?}
		// harvestGrass :112-123 — grass/fern (1) and the 2-tall plants (2)
		if (tIsGrass || tBlock == Blocks.FERN) return replaceSelf(aState, aDrops, 1);
		if (tBlock == Blocks.TALL_GRASS || tBlock == Blocks.LARGE_FERN) return replaceSelf(aState, aDrops, 2);
		return false;
	}

	private static boolean replaceSelf(BlockState aState, List<ItemStack> aDrops, int aCount) {
		aDrops.clear();
		aDrops.add(new ItemStack(aState.getBlock().asItem(), aCount));
		return true;
	}

	/** The stack classifier — the gt6_sense action (no vanilla face; the scythe class has no vanilla action). */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.SENSE == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
