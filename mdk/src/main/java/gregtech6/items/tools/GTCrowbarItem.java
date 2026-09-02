package gregtech6.items.tools;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.ToolAction;

import gregtech6.covers.ICover;
import gregtech6.covers.ICoverableTE;

/**
 * The formal GT6 crowbar — task p9-tool-crowbar spec ②, the ADR
 * 2026-09-01-p9-tool-crowbar ①/⑦ surface. Upstream rides
 * {@code Behavior_Tool(TOOL_crowbar, SFX.MC_BREAK, 100, ...)} mounted by
 * GT_Tool_Crowbar.onStatsAddedToTool :151-154, whose Behavior_Tool.onItemUseFirst
 * (:57-68) broadcasts IBlockToolable.Util.onToolClick (:73-80) to the block entity.
 *
 * <p>Declared port-ism (ADR ⑦): 1.20.1 has no per-item pre-use hook, so the broadcast
 * flattens to a {@link #useOn} direct dispatch — the target BE gets
 * {@link ICoverableTE#onCoverToolClick} with the reserved {@link ICover#TOOL_CROWBAR}
 * id, whose :246-247 OR gate (TOOL_CROWBAR id ∥ the legacy hoe substitute) was left
 * in place by the P4 first port (zero migration, ICover/ICoverableTE untouched).
 * Because 1.20.1 runs the host block's own {@code use} BEFORE {@code useOn},
 * GUI-owning hosts consume the click first — upstream used onItemUseFirst precisely
 * to pre-empt that; the authoritative driver here is the {@code /gt6tool dismantle}
 * acceptance command (the same {@link #crowbarToolClick} dispatch surface), while
 * hosts whose block use PASSes stay reachable in-game.
 *
 * <p>Classification red line: the item performs {@link GT6ToolActions#CROWBAR} and
 * nothing else — never {@code ToolActions.HOE_DIG} (the three wrench-substitute
 * predicates GTOvenBlock.use:109 / GTFluidPipeBlock.use:104 /
 * GTWrenchHighlightListener:85 must not see the crowbar, card spec ①).
 *
 * <p>Numbers (ADR ⑦, card spec ⑤): the upstream tool damage is internal units with
 * 10000 = one durability point (Behavior_Tool.doDamage units(…,10000,100) :63), so
 * the :151 return {@code 10000} maps to a single {@link ItemStack#hurtAndBreak} of 1
 * here; the per-block-break 50 / per-attack 200 figures (GT_Tool_Crowbar :48-50/:70-72)
 * fold into that single point (declared deviation). Durability 512, single steel tier
 * (upstream scales by material, Loader_Tools:128 — the ladder is a pool cut). Attack
 * damage 2.0 kept (getBaseDamage :75-77); the canBlock/isWeapon blocking semantics are
 * cut (1.20.1 has no item-blocking mechanic, ADR ①).
 *
 * <p>Mining half (task p10-tool-crowbar-mining, unlocking the formerly-pooled rails/
 * circuits arm — the javadoc here previously declared it pooled): upstream
 * isMinableBlock (GT_Tool_Crowbar.java:108-114) grants the crowbar a mining surface of
 * rails + circuits blocks, mapped to the 1.20.1 pair the vanilla decompile proves —
 * {@link #isCorrectToolForDrops} for the drop authorization and
 * {@link #getDestroySpeed} for the dig speed (SwordItem.java:44/:68 and
 * ShearsItem.java:45/:50 same shape). Arm-by-arm:
 * <ul>
 * <li>{@code aBlock instanceof BlockRailBase} → {@link BaseRailBlock} instanceof
 * (vanilla BaseRailBlock.java:22, the abstract root of all four rail blocks) —
 * verbatim 1:1.</li>
 * <li>{@code aBlock.getMaterial() == Material.circuits} → an explicit redstone-IO
 * block set ({@link #CIRCUITS_FAMILY}). Ruling: 1.20.1 has no server-side
 * {@code net.minecraft.world.level.material.Material} — the only decompiled
 * {@code Material} is the client atlas class (vanilla
 * client/resources/model/Material.java:15), so there is no material to compare
 * against; the GTCEu-modern tag route (GTToolType.java:181
 * {@code harvestTag(forge:mineable/crowbar)}) was examined and rejected — its
 * generated tag ships no vanilla redstone content (no mineable/crowbar.json under
 * gtceu-modern/src/generated/resources) and a tag predicate cannot be pinned by an
 * offline truth table (tag contents load with the datapack). The explicit set is the
 * redstone-IO semantics of the open 1.7.10 material test: power/transfer/logic
 * (wire, torch pair, repeater, comparator, lever, observer, daylight detector,
 * target, sculk sensor pair), the tripwire pair and the button family (the 1.7.10
 * stone/wood button two members unfolded by the 1.13 flattening). Pressure plates are
 * NOT included — their 1.7.10 material was STONE/WOOD, not circuits; neither is
 * REDSTONE_BLOCK (STONE). Declared deviation: the open material test admitted future
 * circuits blocks automatically, the closed set needs manual additions.</li>
 * <li>{@code IL.TC_Block_Air} / {@code IL.TG_Ore_Cluster_1/2} → Thaumcraft outer
 * domain, cut (including the :117-119 instant-mining-speed arm that only served the
 * TG clusters).</li>
 * <li>{@code BlocksGT.openableCrowbar} → the :122-139 convertBlockDrops
 * Unboxinator-swap arm; RM.Unboxinator/BlocksGT are not in this repo, stays pooled
 * (unchanged from the p9 statement).</li>
 * <li>{@code getHarvestTool == TOOL_crowbar} registry arm → cut: this repo has no
 * harvest-tool layer.</li>
 * <li>the :112-113 fallback (mineable unless some OTHER tool already claims it) →
 * cut: that is the upstream meta-tool mutual-exclusion design; a single item has no
 * family to exclude against.</li>
 * </ul>
 *
 * <p>Mining speed: upstream does NOT speed up — getMiningSpeed (GT_Tool_Crowbar
 * :117-119 minus the TG arm) falls through to ToolStats.getMiningSpeed :94-96 which
 * returns the vanilla {@code aDefault} untouched; the BreakSpeed channel rides
 * MultiItemTool.onBlockBreakSpeedEvent :224-233. The 1.20.1 equivalent of that
 * channel is {@link #getDestroySpeed}, and the card acceptance pins {@code speed>1}
 * for rails, so the mineable surface returns {@link #MINING_SPEED} (6.0F, the
 * iron-tier dig-speed scale — declared deviation: upstream kept the bare-hand
 * speed and only authorized the drops) and everything else stays at the vanilla
 * 1.0F hand speed. Vanilla instant-break blocks (wire, torches, lever, buttons,
 * repeater, comparator have hardness 0) stay instant regardless — the
 * MultiItemTool :227 {@code ST.instaharvest} Float.MAX_VALUE arm is vanilla
 * behavior now, not ported code.
 */
public class GTCrowbarItem extends Item {

	/** The tool damage the ICoverableTE dispatch returns for a successful dismantle (upstream :151). */
	public static final long TOOL_DAMAGE_PER_DISMANTLE = 10000;

	/** The vanilla durability points — declared deviation (upstream material-scaled, single steel tier here). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :75-77 — 2.0F kept verbatim as the main-hand attribute. */
	private static final float ATTACK_DAMAGE = 2.0F;

	/**
	 * The dig-speed multiplier the mineable surface gets (the acceptance-pinned
	 * {@code speed>1} for rails) — the iron-tier scale, declared in the class javadoc.
	 */
	public static final float MINING_SPEED = 6.0F;

	/**
	 * The circuits arm — the explicit redstone-IO family replacing the upstream
	 * {@code Material.circuits} test (ruling in the class javadoc). Verified against
	 * the vanilla Blocks.java constant list; offline truth table pins the members.
	 */
	private static final ImmutableSet<Block> CIRCUITS_FAMILY = ImmutableSet.of(
			// power / transfer / logic
			Blocks.REDSTONE_WIRE, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH,
			Blocks.REPEATER, Blocks.COMPARATOR, Blocks.LEVER,
			Blocks.OBSERVER, Blocks.DAYLIGHT_DETECTOR, Blocks.TARGET,
			Blocks.SCULK_SENSOR, Blocks.CALIBRATED_SCULK_SENSOR,
			// tripwire pair
			Blocks.TRIPWIRE, Blocks.TRIPWIRE_HOOK,
			// the button family (1.7.10 stone/wood buttons, unfolded by the flattening)
			Blocks.STONE_BUTTON, Blocks.POLISHED_BLACKSTONE_BUTTON,
			Blocks.OAK_BUTTON, Blocks.SPRUCE_BUTTON, Blocks.BIRCH_BUTTON, Blocks.JUNGLE_BUTTON,
			Blocks.ACACIA_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.MANGROVE_BUTTON, Blocks.CHERRY_BUTTON,
			Blocks.BAMBOO_BUTTON, Blocks.CRIMSON_BUTTON, Blocks.WARPED_BUTTON);

	private final Multimap<Attribute, AttributeModifier> mAttackModifiers = ImmutableMultimap.of(
			Attributes.ATTACK_DAMAGE,
			new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", (double) ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION));

	public GTCrowbarItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The flattened Behavior_Tool.onItemUseFirst — direct dispatch into the covered
	 * host. PASS on everything that is not an ICoverableTE; claims on the client only
	 * when the target is one (the client cannot know whether the face carries a cover,
	 * so the claim mirrors the HoeItem sided pattern and the server decides PASS).
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		if (!(aContext.getLevel().getBlockEntity(aContext.getClickedPos()) instanceof ICoverableTE)) {
			return InteractionResult.PASS;
		}
		if (aContext.getLevel().isClientSide) {
			return InteractionResult.SUCCESS; // claim, the server side executes
		}
		return crowbarToolClick(aContext) > 0 ? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	/**
	 * The single dispatch + payment surface, shared by {@link #useOn} and the
	 * {@code /gt6tool dismantle} acceptance command (single-source semantics). Runs the
	 * covered-host tool click with the reserved crowbar id, then pays one durability
	 * point per 10000 upstream units (the declared mapping) on a non-null player.
	 *
	 * @return the upstream tool damage (10000 per dismantle) or 0.
	 */
	public static long crowbarToolClick(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		if (!(tLevel.getBlockEntity(tPos) instanceof ICoverableTE tHost)) return 0;
		return crowbarToolClick(tHost, aContext.getPlayer(), aContext.getItemInHand(),
				(byte) aContext.getClickedFace().get3DDataValue(), aContext.isSecondaryUseActive());
	}

	/** The dispatch half without the context (the command and the offline doubles). */
	public static long crowbarToolClick(ICoverableTE aHost, @Nullable Player aPlayer, ItemStack aStack, byte aSide, boolean aSneaking) {
		long tDamage = aHost.onCoverToolClick(ICover.TOOL_CROWBAR, aPlayer, aStack, aSide, aSneaking);
		if (tDamage > 0 && aPlayer != null) {
			aStack.hurtAndBreak(1, aPlayer, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND)); // 10000 units → 1 point
		}
		return tDamage;
	}

	/** Upstream getToolDamagePerEntityAttack :70-72 — 200 units fold into one point (declared deviation). */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot aSlot) {
		return aSlot == EquipmentSlot.MAINHAND ? mAttackModifiers : super.getDefaultAttributeModifiers(aSlot);
	}

	/**
	 * The mining-surface seam — the upstream isMinableBlock :108-114 modern halves
	 * (the rails instanceof arm + the redstone-IO family set). Static pure function so
	 * the offline tests can pin it without constructing the item (the same mod-Item
	 * wall the classifier seam already rides).
	 */
	public static boolean mines(BlockState aState) {
		Block tBlock = aState.getBlock();
		return tBlock instanceof BaseRailBlock || CIRCUITS_FAMILY.contains(tBlock);
	}

	/**
	 * The dig-speed seam — {@link #MINING_SPEED} on the mineable surface, the vanilla
	 * 1.0F hand speed elsewhere (ruling in the class javadoc).
	 */
	public static float destroySpeedBonus(BlockState aState) {
		return mines(aState) ? MINING_SPEED : 1.0F;
	}

	/** The drop-authorization half of isMinableBlock (SwordItem.java:68 shape). */
	@Override
	public boolean isCorrectToolForDrops(BlockState aState) {
		return mines(aState);
	}

	/** The dig-speed half of isMinableBlock (SwordItem.java:44 shape). */
	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		return destroySpeedBonus(aState);
	}

	/**
	 * The stack classifier — the ONLY action this item performs is
	 * {@link GT6ToolActions#CROWBAR}. Never HOE_DIG: the three wrench-substitute
	 * predicates classify on it (the card's wrench-UI-regression red line), and the ICoverableTE
	 * :246-247 OR gate would fire the legacy substitute path instead of the id path.
	 *
	 * <p>The decision lives in this static seam so the offline tests can pin it without
	 * constructing the item — a mod Item cannot be instantiated in a bootstrapped-and-
	 * frozen test JVM (Item.java:61 builds the intrusive registry holder unconditionally,
	 * the same Forge wall as the mod-Block offline lesson).
	 */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.CROWBAR == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
