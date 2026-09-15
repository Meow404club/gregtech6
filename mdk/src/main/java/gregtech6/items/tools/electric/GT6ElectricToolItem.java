package gregtech6.items.tools.electric;

import java.util.List;
import java.util.Locale;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.item.energy.GT6BatteryItem;
import gregtech6.item.energy.IItemEnergy;

/**
 * The electric-tool family base — task p29-w5-t6-electric-nineteen: NINETEEN flat item
 * ids (single steel tier × the three-voltage ladder), all sharing THIS class over a
 * data {@link Spec} table (the card's "基类+表驱动，注册行逐 id 可断言" ruling; the
 * upstream 19 ToolStats classes fold onto 19 spec rows, Loader_Tools.java:156-174).
 *
 * <p><b>The EU pool</b> (the W4 battery seam consumption, the card SPEC ①): the item
 * implements {@link IItemEnergy} over the SAME {@code gt.energy} stack carrier the
 * batteries use ({@link GT6BatteryItem} static helpers — one carrier rule for the whole
 * energy domain). Capacity = the LEAD-ACID representative literal per tier
 * (LV 64000 / MV 256000 / HV 1024000 — the GT6BatteryLadderTest EXPECTED column, the
 * declared 收敛 of the upstream capacity-sum face Loader_Tools.java:427-450 where the
 * sum folds to exactly the one re-battery the recipe installs); the packet band rides
 * V[tier] = 32/128/512 with the Base08 :62-66 derivation (the GT6BatteryItem formula).
 * The BatteryBox inventory accepts any IItemEnergy stack whose type matches (the :199
 * canInsertItem2 verbatim), so a drained tool recharges in the box — the W4 charge face
 * needs ZERO new wiring.
 *
 * <p><b>The wear semantics</b> (SPEC ②, MultiItemTool.doDamage :433-475 直译): every
 * action drains EU = the upstream tool-damage units of that action
 * ({@code useEnergy(TD.Energy.EU, aStack, aAmount, ...)}, the :436/:437 drain arm), and
 * ONLY the {@code RNGSUS.nextInt(max(10, quality*20)) == 0} roll pays durability — the
 * "有能量池→免常规耐久磨损、仅随机耐久点" face ({@link #rollsWear}, the :433 verbatim
 * denominator). The 10000-units=1-point family fold maps each roll hit to ONE vanilla
 * point; the shell stays the family value 512 (the card open-question ruling: the shell
 * carries the random wear, the EU pool carries the real attrition). EU empty → the tool
 * refuses (the :430 isItemStackUsable 对位: speed 0, {@code mineBlock} false, the swap
 * arm still fires so the monkey-wrench mode stays reachable).
 *
 * <p><b>The mining surface</b> (SPEC ⑤, the upstream {@code isMinableBlock} mappings):
 * a non-minable block reads dig speed ZERO (the upstream {@code getMiningSpeed} 0 →
 * {@code getDigSpeed} 0 gate — a GT6 tool cannot break outside its surface at all, the
 * jackhammer no-ores "对矿石块零破坏" face). The port folds: DRILL = the pickaxe surface
 * (GTPickaxeItem.mines: the pickaxe tag + glass/ice/flowerpot/cauldron arms — the
 * upstream Material.anvil/iron arms already live inside those sets) ∪ the shovel tag;
 * CHAINSAW = the axe tag + glass/ice (the saw's plant/leaves universe rides the axe tag
 * on 1.20.1); JACKHAMMER = the pickaxe tag + glass/ice, the NO_ORES form MINUS the
 * vanilla ore-block family (the upstream IPrefixBlock gate — this universe's ores ARE
 * the vanilla ore blocks); BUZZSAW = the iron-bars face (the BlockBaseBars/BlockPane
 * pair folds to the one vanilla bar block; "Not suitable for harvesting Blocks");
 * TRIMMER = leaves + vine (the grafter surface); the wrench/monkey-wrench/screwdriver/
 * hand-drill/mixer machine-tool surfaces have NO port block universe (the RED LINE:
 * zero machine face) and stay structurally empty.
 *
 * <p><b>The twin swap</b> (SPEC ④, Behavior_Switch_Metadata :30-48 verbatim): sneak
 * right-click a BARE target (mCheckTarget=true — a block WITHOUT a BlockEntity lets the
 * swap through, a BE target leaves the face to the tool interaction, the t3
 * monkey-wrench mCheckTarget ruling) exchanges the stack for its twin id carrying the
 * SAME damage and EU charge. Pairs = the upstream constructor cross-references :162-166:
 * wrench_lv↔monkey_wrench_lv (and the mv/hv twins), jackhammer normal↔no_ores.
 *
 * <p>Declared cuts (the card dont + boundary): the Place_Torch/Place_Sapling/
 * Place_Workbench/Place_Dynamite behaviour arms (the interaction pool), the spider
 * effective-list ×2 (the HandDrill mEffectiveList — no mob pipeline arm), the canCollect
 * /canBlock/death-message faces (the MultiItemTool attack pipeline cut, the t1 crowbar
 * precedent), the chainsaw whole-tree LOCK arm (upstream GT_Tool_Axe :102-110 — the t2
 * axe owns the tree felling; the electric chainsaw rides the plain surface), and the
 * per-material texture ladder (one model per id, the family single-tier ruling).
 */
public class GT6ElectricToolItem extends Item implements IItemEnergy {

	/** The family value (the crowbar/pickaxe/... pinned 512; the card open-question ruling). */
	public static final int DURABILITY_POINTS = 512;

	/** The dig-speed anchor (the GTPickaxeItem.MINING_SPEED iron-tier scale) × the upstream getSpeedMultiplier. */
	public static final float MINING_SPEED_BASE = 6.0F;

	/** The lead-acid representative capacity per tier (the GT6BatteryLadderTest literals, SPEC ①). */
	public static final long[] CAPACITY_PER_TIER = {0L, 64000L, 256000L, 1024000L};

	/** The packet size per tier (the NBT_INPUT column V[1..3] = 32/128/512). */
	public static final long[] SIZE_PER_TIER = {0L, 32L, 128L, 512L};

	/** The mining-surface families (the SPEC ⑤ folds; static pure sets — the offline-test seam). */
	public static final ImmutableSet<Block> GLASS_FAMILY = ImmutableSet.of(Blocks.GLASS, Blocks.GLASS_PANE, Blocks.TINTED_GLASS);
	public static final ImmutableSet<Block> ICE_FAMILY = ImmutableSet.of(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);
	/** The vanilla ore-block family — the no-ores jackhammer's exclusion face (the upstream IPrefixBlock gate). */
	public static final ImmutableSet<Block> ORE_FAMILY = ImmutableSet.of(
			Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
			Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
			Blocks.NETHER_GOLD_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.EMERALD_ORE,
			Blocks.DEEPSLATE_EMERALD_ORE, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DIAMOND_ORE,
			Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS);
	/** The buzzsaw's whole harvesting surface (the bars/panes fold). */
	public static final ImmutableSet<Block> BARS_FAMILY = ImmutableSet.of(Blocks.IRON_BARS);

	/** The mining-surface kind of a spec row (the upstream isMinableBlock shapes). */
	public enum Surface {
		DRILL, CHAINSAW, JACKHAMMER, JACKHAMMER_NO_ORES, BUZZSAW, TRIMMER, NONE;

		/** The upstream isMinableBlock mapping — static pure (the crowbar mines seam). */
		public boolean mines(BlockState aState) {
			Block tBlock = aState.getBlock();
			return switch (this) {
				case DRILL -> aState.is(BlockTags.MINEABLE_WITH_PICKAXE) || aState.is(BlockTags.MINEABLE_WITH_SHOVEL)
						|| GLASS_FAMILY.contains(tBlock) || ICE_FAMILY.contains(tBlock);
				case CHAINSAW -> aState.is(BlockTags.MINEABLE_WITH_AXE) || GLASS_FAMILY.contains(tBlock)
						|| ICE_FAMILY.contains(tBlock);
				case JACKHAMMER -> aState.is(BlockTags.MINEABLE_WITH_PICKAXE) || GLASS_FAMILY.contains(tBlock)
						|| ICE_FAMILY.contains(tBlock);
				case JACKHAMMER_NO_ORES -> (aState.is(BlockTags.MINEABLE_WITH_PICKAXE) || GLASS_FAMILY.contains(tBlock)
						|| ICE_FAMILY.contains(tBlock)) && !ORE_FAMILY.contains(tBlock);
				case BUZZSAW -> BARS_FAMILY.contains(tBlock);
				case TRIMMER -> aState.is(BlockTags.LEAVES) || tBlock == Blocks.VINE;
				case NONE -> false;
			};
		}
	}

	/**
	 * One electric-tool row — the 19 fields the base class and the recipes read (the
	 * upstream ToolStats numeric face, each value read off the named upstream class).
	 *
	 * @param aPath              the registry path (snake)
	 * @param aTwinPath          the sneak-swap twin ({@code null} = no swap arm)
	 * @param aTier              the voltage tier 1..3 (LV/MV/HV)
	 * @param aDamagePerBlock    the upstream getToolDamagePerBlockBreak (the EU cost of a break)
	 * @param aDamagePerCraft    the upstream getToolDamagePerContainerCraft
	 * @param aDamagePerAttack   the upstream getToolDamagePerEntityAttack (the EU cost of a hit)
	 * @param aQuality           the upstream getBaseQuality (the wear denominator input)
	 * @param aAttackDamage      the upstream getBaseDamage (the attribute)
	 * @param aSpeedMultiplier   the upstream getSpeedMultiplier
	 * @param aSurface           the mining-surface kind
	 * @param aTooltipKey        the tooltip lang key ({@code null} = no tooltip row)
	 * @param aModeSwitchTooltip the Behavior_Switch_Metadata mShowModeSwitchTooltip flag
	 */
	public record Spec(String aPath, String aTwinPath, int aTier, int aDamagePerBlock, int aDamagePerCraft,
			int aDamagePerAttack, int aQuality, float aAttackDamage, float aSpeedMultiplier, Surface aSurface,
			String aTooltipKey, boolean aModeSwitchTooltip) {

		/** The lead-acid representative capacity of the row's tier (the SPEC ① literal). */
		public long capacity() {
			return CAPACITY_PER_TIER[aTier()];
		}

		/** The packet size V[tier] (the band face). */
		public long sizeRec() {
			return SIZE_PER_TIER[aTier()];
		}

		/** The derived packet band floor (the Base08 :64 formula — the GT6BatteryItem mirror). */
		public long sizeMin() {
			return (sizeRec() / 2 <= 8 && sizeRec() > 0) ? 1 : sizeRec() / 2;
		}

		/** The derived packet band ceiling (the :63 verbatim). */
		public long sizeMax() {
			return sizeRec() * 2;
		}

		/** The wear-roll denominator (the :433 verbatim {@code max(10, quality*20)}). */
		public int wearDenominator() {
			return Math.max(10, aQuality() * 20);
		}

		/** The effective dig speed on the surface (the MINING_SPEED_BASE × multiplier). */
		public float destroySpeed(BlockState aState) {
			return aSurface().mines(aState) ? MINING_SPEED_BASE * aSpeedMultiplier() : 0.0F;
		}
	}

	// -------------------------------------------------------------------------
	// the 19-row spec table — the upstream registration order Loader_Tools.java:156-174
	// -------------------------------------------------------------------------

	public static final Spec MINING_DRILL_LV = new Spec("mining_drill_lv", null, 1, 25, 100, 200, 0, 2.0F, 3.0F, Surface.DRILL, null, false);
	public static final Spec MINING_DRILL_MV = new Spec("mining_drill_mv", null, 2, 100, 3200, 800, 1, 2.5F, 6.0F, Surface.DRILL, null, false);
	public static final Spec MINING_DRILL_HV = new Spec("mining_drill_hv", null, 3, 400, 12800, 3200, 2, 3.0F, 9.0F, Surface.DRILL, null, false);
	public static final Spec CHAINSAW_LV = new Spec("chainsaw_lv", null, 1, 50, 200, 800, 1, 3.0F, 2.0F, Surface.CHAINSAW, "item.gt6.chainsaw_lv.tooltip", false);
	public static final Spec CHAINSAW_MV = new Spec("chainsaw_mv", null, 2, 50, 200, 800, 1, 3.5F, 3.0F, Surface.CHAINSAW, "item.gt6.chainsaw_mv.tooltip", false);
	public static final Spec CHAINSAW_HV = new Spec("chainsaw_hv", null, 3, 200, 200, 3200, 1, 4.0F, 4.0F, Surface.CHAINSAW, "item.gt6.chainsaw_hv.tooltip", false);
	public static final Spec WRENCH_LV = new Spec("wrench_lv", "monkey_wrench_lv", 1, 50, 800, 200, 0, 1.0F, 2.0F, Surface.NONE, "item.gt6.wrench_lv.tooltip", false);
	public static final Spec WRENCH_MV = new Spec("wrench_mv", "monkey_wrench_mv", 2, 200, 3200, 800, 1, 1.5F, 3.0F, Surface.NONE, "item.gt6.wrench_mv.tooltip", false);
	public static final Spec WRENCH_HV = new Spec("wrench_hv", "monkey_wrench_hv", 3, 800, 12800, 3200, 2, 2.0F, 4.0F, Surface.NONE, "item.gt6.wrench_hv.tooltip", false);
	public static final Spec JACKHAMMER_HV_NORMAL = new Spec("jackhammer_hv_normal", "jackhammer_hv_no_ores", 3, 200, 3200, 800, 1, 3.0F, 12.0F, Surface.JACKHAMMER, "item.gt6.jackhammer_hv_normal.tooltip", true);
	public static final Spec JACKHAMMER_HV_NO_ORES = new Spec("jackhammer_hv_no_ores", "jackhammer_hv_normal", 3, 200, 3200, 800, 1, 3.0F, 12.0F, Surface.JACKHAMMER_NO_ORES, "item.gt6.jackhammer_hv_no_ores.tooltip", true);
	public static final Spec BUZZSAW_LV = new Spec("buzzsaw_lv", null, 1, 50, 100, 300, 0, 1.0F, 1.0F, Surface.BUZZSAW, "item.gt6.buzzsaw_lv.tooltip", false);
	public static final Spec SCREWDRIVER_LV = new Spec("screwdriver_lv", null, 1, 200, 200, 200, 0, 1.5F, 1.0F, Surface.NONE, null, false);
	public static final Spec HAND_DRILL_LV = new Spec("hand_drill_lv", null, 1, 200, 100, 400, 0, 1.5F, 1.0F, Surface.NONE, null, false);
	public static final Spec HAND_MIXER_LV = new Spec("hand_mixer_lv", null, 1, 200, 100, 400, 0, 1.5F, 1.0F, Surface.NONE, "item.gt6.hand_mixer_lv.tooltip", false);
	public static final Spec MONKEY_WRENCH_LV = new Spec("monkey_wrench_lv", "wrench_lv", 1, 50, 800, 200, 0, 1.0F, 2.0F, Surface.NONE, "item.gt6.monkey_wrench_lv.tooltip", false);
	public static final Spec MONKEY_WRENCH_MV = new Spec("monkey_wrench_mv", "wrench_mv", 2, 200, 3200, 800, 1, 1.5F, 3.0F, Surface.NONE, "item.gt6.monkey_wrench_mv.tooltip", false);
	public static final Spec MONKEY_WRENCH_HV = new Spec("monkey_wrench_hv", "wrench_hv", 3, 800, 12800, 3200, 2, 2.0F, 4.0F, Surface.NONE, "item.gt6.monkey_wrench_hv.tooltip", false);
	public static final Spec TRIMMER_LV = new Spec("trimmer_lv", null, 1, 100, 100, 100, 0, 2.0F, 0.25F, Surface.TRIMMER, null, false);

	/** The display-order table (the upstream :156-174 row order; the TAB_TABLE source). */
	public static final List<Spec> SPECS = List.of(
			MINING_DRILL_LV, MINING_DRILL_MV, MINING_DRILL_HV,
			CHAINSAW_LV, CHAINSAW_MV, CHAINSAW_HV,
			WRENCH_LV, WRENCH_MV, WRENCH_HV,
			JACKHAMMER_HV_NORMAL, JACKHAMMER_HV_NO_ORES,
			BUZZSAW_LV, SCREWDRIVER_LV, HAND_DRILL_LV, HAND_MIXER_LV,
			MONKEY_WRENCH_LV, MONKEY_WRENCH_MV, MONKEY_WRENCH_HV,
			TRIMMER_LV);

	/** The spec lookup by registry path (the swap twin + the command resolver seam). */
	public static Spec specOf(String aPath) {
		for (Spec tSpec : SPECS) if (tSpec.aPath().equals(aPath)) return tSpec;
		return null;
	}

	private final Spec mSpec;

	public GT6ElectricToolItem(Spec aSpec, Properties aProperties) {
		super(aProperties);
		mSpec = aSpec;
		//? if forge {
		mAttackModifiers = ImmutableMultimap.of(
				Attributes.ATTACK_DAMAGE,
				new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", (double) aSpec.aAttackDamage(), AttributeModifier.Operation.ADDITION));
		//?} else {
		/*mAttackModifiers = net.minecraft.world.item.component.ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, (double) aSpec.aAttackDamage(), AttributeModifier.Operation.ADD_VALUE),
						net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
				.build();
		*///?}
	}

	/** The row this item carries. */
	public Spec spec() {
		return mSpec;
	}

	// -------------------------------------------------------------------------
	// the EU pool (the IItemEnergy face — the W4 seam consumption)
	// -------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, ItemStack aStack, boolean aEmitting) {
		return aEnergyType == TD.Energy.EU || aEnergyType == null; // the EU-only domain
	}

	@Override
	public java.util.Collection<TagData> getEnergyTypes(ItemStack aStack) {
		return TD.Energy.EU.AS_LIST;
	}

	@Override
	public boolean canEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize) {
		return (aEnergyType == TD.Energy.EU || aEnergyType == null) && aStack.getCount() == 1
				&& aSize <= mSpec.sizeMax() && aSize >= mSpec.sizeMin(); // the :228 shape
	}

	@Override
	public boolean canEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize) {
		return canEnergyInjection(aEnergyType, aStack, aSize); // the :229 shape
	}

	@Override
	public long doEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, boolean aDoInject) {
		if (aAmount < 1) return 0;
		if (!canEnergyInjection(aEnergyType, aStack, Math.abs(aSize))) return 0;
		long tStored = getEnergyStored(TD.Energy.EU, aStack);
		long tRoom = mSpec.capacity() - tStored;
		if (tRoom <= 0) return 0;
		long rPackets = Math.min(aAmount, (tRoom + aSize - 1) / aSize); // the packets that fit
		if (aDoInject) setEnergyStored(TD.Energy.EU, aStack, Math.min(mSpec.capacity(), tStored + rPackets * aSize));
		return rPackets;
	}

	@Override
	public long doEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, boolean aDoExtract) {
		if (aAmount < 1) return 0;
		if (!canEnergyExtraction(aEnergyType, aStack, Math.abs(aSize))) return 0;
		long tStored = getEnergyStored(TD.Energy.EU, aStack);
		if (tStored < aSize) return 0;
		long rAmount = Math.min(aAmount, tStored / aSize);
		if (aDoExtract) setEnergyStored(TD.Energy.EU, aStack, tStored - rAmount * aSize);
		return rAmount;
	}

	@Override
	public boolean useEnergy(TagData aEnergyType, ItemStack aStack, long aEnergyAmount, boolean aDoUse) {
		if (aEnergyType != TD.Energy.EU && aEnergyType != null) return false;
		long tStored = getEnergyStored(TD.Energy.EU, aStack);
		if (tStored >= aEnergyAmount) {
			if (aDoUse) setEnergyStored(TD.Energy.EU, aStack, tStored - aEnergyAmount);
			return true;
		}
		if (aDoUse) setEnergyStored(TD.Energy.EU, aStack, 0); // the :204 zero-out
		return false;
	}

	@Override
	public ItemStack setEnergyStored(TagData aEnergyType, ItemStack aStack, long aAmount) {
		if ((aEnergyType != TD.Energy.EU && aEnergyType != null) || aStack.isEmpty()) return aStack;
		GT6BatteryItem.writeItemNBT(aStack, Math.max(0, Math.min(mSpec.capacity(), aAmount))); // the shared carrier + the clamp
		return aStack;
	}

	@Override
	public long getEnergyStored(TagData aEnergyType, ItemStack aStack) {
		return aEnergyType == TD.Energy.EU || aEnergyType == null ? Math.min(GT6BatteryItem.readStoredRaw(aStack), mSpec.capacity()) : 0;
	}

	@Override
	public long getEnergyCapacity(TagData aEnergyType, ItemStack aStack) {
		return aEnergyType == TD.Energy.EU || aEnergyType == null ? mSpec.capacity() : 0;
	}

	/** The isItemStackUsable 对位 (SPEC ②): any charge = usable; empty = the tool refuses. */
	public boolean usable(ItemStack aStack) {
		return getEnergyStored(TD.Energy.EU, aStack) > 0;
	}

	// -------------------------------------------------------------------------
	// the wear semantics (the doDamage :433-437 transcription)
	// -------------------------------------------------------------------------

	/**
	 * The :433 random-wear roll — {@code RNGSUS.nextInt(max(10, quality*20)) == 0}; the
	 * seeded rng parameter is the offline-test seam (live callers pass the level random).
	 */
	public static boolean rollsWear(Spec aSpec, RandomSource aRng) {
		return aRng.nextInt(aSpec.wearDenominator()) == 0;
	}

	/** One vanilla durability point per roll hit (the 10000=1 fold), the vanilla break broadcast. */
	private void payWear(ItemStack aStack, Level aLevel, LivingEntity aEntity) {
		if (rollsWear(mSpec, aLevel.random)) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
	}

	// -------------------------------------------------------------------------
	// the mining surface (the getDigSpeed 0 gate)
	// -------------------------------------------------------------------------

	//? if forge {
	@Override
	public boolean isCorrectToolForDrops(BlockState aState) {
	//?} else {
	/*public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
	//21.1: the stack parameter joined the signature (the GTPickaxeItem fork).
	*///?}
		return mSpec.aSurface().mines(aState); // the surface gate — the usable half rides getDestroySpeed/mineBlock
	}

	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		if (!usable(aStack)) return 0.0F; // the EU-empty refusal (the isItemStackUsable 对位)
		return mSpec.destroySpeed(aState); // 0 outside the surface — the "cannot break" gate
	}

	@Override
	public boolean mineBlock(ItemStack aStack, Level aLevel, BlockState aState, BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && usable(aStack) && mSpec.aSurface().mines(aState)
				&& aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			useEnergy(TD.Energy.EU, aStack, mSpec.aDamagePerBlock(), true); // the :436 drain arm
			payWear(aStack, aLevel, aEntity); // the :433 random arm — EU always, durability rarely
		}
		return usable(aStack);
	}

	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		if (!aAttacker.level().isClientSide && usable(aStack)) {
			useEnergy(TD.Energy.EU, aStack, mSpec.aDamagePerAttack(), true); // the :436 drain arm
			if (rollsWear(mSpec, aAttacker.level().random)) {
				aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
			}
		}
		return true;
	}

	// -------------------------------------------------------------------------
	// the twin swap (the Behavior_Switch_Metadata :30-48 transcription)
	// -------------------------------------------------------------------------

	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Player tPlayer = aContext.getPlayer();
		Level tLevel = aContext.getLevel();
		ItemStack tStack = aContext.getItemInHand();
		if (tPlayer != null && mSpec.aTwinPath() != null && tPlayer.isShiftKeyDown() && !tLevel.isClientSide) { // isSneaking 对位
			BlockPos tPos = aContext.getClickedPos();
			BlockEntity tBE = tLevel.getBlockEntity(tPos); // mCheckTarget=T: a BE target leaves the face to the tool
			if (tBE == null) {
				tPlayer.setItemInHand(aContext.getHand(), switchForm(mSpec, tStack));
				tLevel.playSound(null, tPos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 1.4F);
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}

	/**
	 * The ST.update_(ST.meta_(aStack, mSwitchIndex)) modern form: a NEW stack of the twin
	 * item carrying the SAME damage and charge carrier (the t7 ruling: setItem does not
	 * exist on either leg — the swap builds the twin stack explicitly).
	 */
	public static ItemStack switchForm(Spec aSpec, ItemStack aStack) {
		//? if forge {
		Item tTwin = net.minecraft.core.registries.BuiltInRegistries.ITEM
				.get(new net.minecraft.resources.ResourceLocation("gt6", aSpec.aTwinPath()));
		//?} else {
		/*Item tTwin = net.minecraft.core.registries.BuiltInRegistries.ITEM
				.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", aSpec.aTwinPath()));
		//21.1: the two-arg ResourceLocation ctor is private (the GTWireBakedModel fork face).
		*///?}
		ItemStack rTwin = new ItemStack(tTwin == null ? net.minecraft.world.item.Items.AIR : tTwin);
		//? if forge {
		CompoundTag tCarrier = aStack.getTag();
		if (tCarrier != null) rTwin.setTag(tCarrier.copy()); // the EU charge rides the shared carrier key
		//?} else {
		/*var tCarrier = aStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		if (tCarrier != null) rTwin.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, tCarrier);
		//21.1: the carrier is the opaque CUSTOM_DATA envelope (the GT6BatteryItem fork).
		*///?}
		rTwin.setDamageValue(aStack.getDamageValue()); // the same shell wear
		return rTwin;
	}

	// -------------------------------------------------------------------------
	// the attribute + tooltip faces
	// -------------------------------------------------------------------------

	/** Built in the ctor — the field initializer would run BEFORE mSpec is assigned. */
	//? if forge {
	private final Multimap<Attribute, AttributeModifier> mAttackModifiers;
	//?} else {
	/*private final net.minecraft.world.item.component.ItemAttributeModifiers mAttackModifiers;
	*///?}

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

	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, Level aLevel, java.util.List<Component> aTooltip, TooltipFlag aFlag) {
		super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
		tooltipLines(aTooltip);
	}
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aLevel, java.util.List<Component> aTooltip, TooltipFlag aFlag) {
		//21.1: the Level second parameter became Item.TooltipContext (the GTGrassBlock fork face).
		super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
		tooltipLines(aTooltip);
	}
	*///?}

	/** The shared tooltip body: the row tooltip + the jackhammer mode-switch line. */
	private void tooltipLines(java.util.List<Component> aTooltip) {
		if (mSpec.aTooltipKey() != null) aTooltip.add(Component.translatable(mSpec.aTooltipKey()));
		if (mSpec.aModeSwitchTooltip()) aTooltip.add(Component.translatable("item.gt6.mode_switch.tooltip"));
	}

	@Override
	public String getDescriptionId() {
		return "item.gt6." + mSpec.aPath(); // the spec-derived key (the GT6Tools naming rule)
	}

	// the ToolAction face: Item.canPerformAction defaults false — the machine-tool
	// actions stay unpinned with zero overrides (the RED LINE needs no code).

	static {
		// the locale guard: the path strings stay snake lowercase (the registration rule)
		for (Spec tSpec : SPECS) {
			if (!tSpec.aPath().equals(tSpec.aPath().toLowerCase(Locale.ROOT))) throw new IllegalStateException("gt6 electric spec path not snake: " + tSpec.aPath());
		}
	}
}
