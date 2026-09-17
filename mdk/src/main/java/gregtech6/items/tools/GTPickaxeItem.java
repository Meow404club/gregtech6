package gregtech6.items.tools;

import java.util.List;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The formal GT6 pickaxe — item id {@code gt6:pickaxe} (task p29-w5-t1-dig-six spec;
 * the MATERIAL LADDER face is task p31-dig-ladder, the {@link GT6ToolLadder} form).
 * Upstream GT_Tool_Pickaxe.java:41-72:
 * <ul>
 * <li><b>Mining surface</b> (:54-56): the {@code TOOL_pickaxe} harvest arm + the
 *     rock/iron/anvil materials → the vanilla {@code #minecraft:mineable/pickaxe} tag
 *     (which carries the stone/metal/anvil/infested universe on both legs); the
 *     {@code Material.glass} / packedIce+ice / flower_pot arms → the explicit extension
 *     sets {@link #GLASS_FAMILY} / {@link #ICE_FAMILY} / {@link #FLOWER_POT_FAMILY} /
 *     {@link #CAULDRON_FAMILY} (the GTCrowbarItem.CIRCUITS_FAMILY explicit-set ruling —
 *     1.20.1 has no server-side Material; the 1.7.10 button-flattening lesson applies:
 *     the single {@code Blocks.flower_pot} unfolds to the potted family). Rails ride
 *     the TAG arm too — the vanilla {@code #minecraft:mineable/pickaxe} carries the
 *     nested {@code #minecraft:rails} reference (the 1.20.x client-extra tag data,
 *     S14 correction of this javadoc's earlier "rails are NOT in the surface" claim:
 *     the first census grep missed the {@code #}-prefixed tag-nested entry), so the
 *     rails face is faithful to the upstream {@code Material.iron} arm and the crowbar
 *     holds it as its EXPLICIT instanceof arm beside the circuits set (the overlap is
 *     upstream-true: 1.7.10 rails were Material.iron).</li>
 * <li><b>Damage</b>: per-block 25 / per-attack 200 (:42-43) fold into the single
 *     vanilla point each (the crowbar declared-deviation mapping); base damage 3.0F
 *     (:44) kept as the attribute.</li>
 * <li><b>Behavior arms</b> (:68-72): {@code Behavior_Plug_Leak} is CUT — the repo pipe
 *     BlockEntity pool-cuts the corrosion-leak mechanic itself
 *     (GTFluidPipeBlockEntity.java:74-75), so there is no leak face to plug (revives
 *     with the pipe-domain card); {@code Behavior_Place_Torch} →
 *     {@link #placeTorchFromInventory} (the upstream hotbar-first scan + first-fail-stop,
 *     the modern {@link BlockPlaceContext} placement face).</li>
 * <li><b>canPenetrate / hurtResistance×2</b> (:45-51): CUT — both ride the upstream
 *     MultiItemTool attack pipeline this port does not carry (the crowbar canBlock cut
 *     precedent, ADR ①).</li>
 * </ul>
 *
 * <p>Durability ladder (task p31-dig-ladder): the stack's {@code GT.ToolStats} identity
 * scales durability ({@code j}/100 points), dig speed ({@code mToolSpeed}), mining
 * level ({@code baseQuality + mToolQuality}, the :482 gate that also returns ZERO speed
 * on too-hard surfaces) and the head tint; the IDENTITY-LESS arm reproduces Steel
 * bit-exact ({@code getPrimaryMaterial(stack, MT.Steel)} :60/:65 is the upstream read,
 * so the pre-ladder 512/6.0F constants ARE the steel fallback). Form parameters
 * (upstream ToolStats defaults): durability ×1.0, speed ×1.0, base quality 0; the
 * attack damage stays the flat 3.0F constant (the stack-dependent attack face is cut,
 * the {@link GT6ToolLadder} javadoc). Tier semantics: the level gate replaces the old
 * flat {@code needs_diamond_tool} refusal — steel (quality 2) still refuses diamond
 * and authorizes stone/iron exactly as before, so the legacy arm is zero-migration.
 */
public class GTPickaxeItem extends Item implements GT6ToolLadder.LadderTool {

	/** The family value (the crowbar/cutter/... pinned 512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Upstream getBaseDamage :44 — 3.0F kept verbatim as the main-hand attribute. */
	private static final float ATTACK_DAMAGE = 3.0F;

	/**
	 * The dig speed the mineable surface gets — the iron-tier scale (the crowbar
	 * MINING_SPEED 6.0F anchor; upstream getSpeedMultiplier is the 1.0 default).
	 */
	public static final float MINING_SPEED = 6.0F;

	/** The form durability multiplier (upstream getMaxDurabilityMultiplier, ToolStats.java:71 = 1.0). */
	public static final float DURABILITY_MULTIPLIER = 1.0F;

	/** The form speed multiplier (upstream getSpeedMultiplier, ToolStats.java:70 = 1.0). */
	public static final float SPEED_MULTIPLIER = 1.0F;

	/** The upstream {@code Material.glass} arm — the glass block/pane family (incl. tinted). */
	static final ImmutableSet<Block> GLASS_FAMILY = buildGlassFamily();

	private static ImmutableSet<Block> buildGlassFamily() {
		ImmutableSet.Builder<Block> tGlass = ImmutableSet.builder();
		tGlass.add(Blocks.GLASS, Blocks.GLASS_PANE, Blocks.TINTED_GLASS);
		for (net.minecraft.world.item.DyeColor tColor : net.minecraft.world.item.DyeColor.values()) {
			tGlass.add(stainedGlassOf(tColor), stainedGlassPaneOf(tColor)); // the 16+16 flattening unfold
		}
		return tGlass.build();
	}

	private static Block stainedGlassOf(net.minecraft.world.item.DyeColor aColor) {
		return switch (aColor) {
		case WHITE -> Blocks.WHITE_STAINED_GLASS;
		case ORANGE -> Blocks.ORANGE_STAINED_GLASS;
		case MAGENTA -> Blocks.MAGENTA_STAINED_GLASS;
		case LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS;
		case YELLOW -> Blocks.YELLOW_STAINED_GLASS;
		case LIME -> Blocks.LIME_STAINED_GLASS;
		case PINK -> Blocks.PINK_STAINED_GLASS;
		case GRAY -> Blocks.GRAY_STAINED_GLASS;
		case LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS;
		case CYAN -> Blocks.CYAN_STAINED_GLASS;
		case PURPLE -> Blocks.PURPLE_STAINED_GLASS;
		case BLUE -> Blocks.BLUE_STAINED_GLASS;
		case BROWN -> Blocks.BROWN_STAINED_GLASS;
		case GREEN -> Blocks.GREEN_STAINED_GLASS;
		case RED -> Blocks.RED_STAINED_GLASS;
		case BLACK -> Blocks.BLACK_STAINED_GLASS;
		};
	}

	private static Block stainedGlassPaneOf(net.minecraft.world.item.DyeColor aColor) {
		return switch (aColor) {
		case WHITE -> Blocks.WHITE_STAINED_GLASS_PANE;
		case ORANGE -> Blocks.ORANGE_STAINED_GLASS_PANE;
		case MAGENTA -> Blocks.MAGENTA_STAINED_GLASS_PANE;
		case LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS_PANE;
		case YELLOW -> Blocks.YELLOW_STAINED_GLASS_PANE;
		case LIME -> Blocks.LIME_STAINED_GLASS_PANE;
		case PINK -> Blocks.PINK_STAINED_GLASS_PANE;
		case GRAY -> Blocks.GRAY_STAINED_GLASS_PANE;
		case LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS_PANE;
		case CYAN -> Blocks.CYAN_STAINED_GLASS_PANE;
		case PURPLE -> Blocks.PURPLE_STAINED_GLASS_PANE;
		case BLUE -> Blocks.BLUE_STAINED_GLASS_PANE;
		case BROWN -> Blocks.BROWN_STAINED_GLASS_PANE;
		case GREEN -> Blocks.GREEN_STAINED_GLASS_PANE;
		case RED -> Blocks.RED_STAINED_GLASS_PANE;
		case BLACK -> Blocks.BLACK_STAINED_GLASS_PANE;
		};
	}

	/** The upstream packedIce+ice arm — the three permanent ice blocks (frosted is ephemeral, cut). */
	static final ImmutableSet<Block> ICE_FAMILY = ImmutableSet.of(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);

	/**
	 * The upstream {@code aBlock == Blocks.flower_pot} arm, unfolded by the 1.13
	 * flattening (the crowbar button-family lesson): the empty pot + the potted family.
	 */
	static final ImmutableSet<Block> FLOWER_POT_FAMILY = ImmutableSet.<Block>builder()
			.add(Blocks.FLOWER_POT)
			.add(Blocks.POTTED_AZALEA, Blocks.POTTED_FLOWERING_AZALEA)
			.add(Blocks.POTTED_OAK_SAPLING, Blocks.POTTED_SPRUCE_SAPLING, Blocks.POTTED_BIRCH_SAPLING,
					Blocks.POTTED_JUNGLE_SAPLING, Blocks.POTTED_ACACIA_SAPLING, Blocks.POTTED_DARK_OAK_SAPLING,
					Blocks.POTTED_MANGROVE_PROPAGULE, Blocks.POTTED_CHERRY_SAPLING)
			.add(Blocks.POTTED_RED_MUSHROOM, Blocks.POTTED_BROWN_MUSHROOM)
			.add(Blocks.POTTED_FERN, Blocks.POTTED_DANDELION, Blocks.POTTED_POPPY, Blocks.POTTED_BLUE_ORCHID,
					Blocks.POTTED_ALLIUM, Blocks.POTTED_AZURE_BLUET, Blocks.POTTED_RED_TULIP, Blocks.POTTED_ORANGE_TULIP,
					Blocks.POTTED_WHITE_TULIP, Blocks.POTTED_PINK_TULIP, Blocks.POTTED_OXEYE_DAISY,
					Blocks.POTTED_CORNFLOWER, Blocks.POTTED_LILY_OF_THE_VALLEY, Blocks.POTTED_WITHER_ROSE)
			.add(Blocks.POTTED_DEAD_BUSH, Blocks.POTTED_CRIMSON_FUNGUS, Blocks.POTTED_WARPED_FUNGUS,
					Blocks.POTTED_BAMBOO, Blocks.POTTED_CACTUS, Blocks.POTTED_TORCHFLOWER)
			.build();

	/** The upstream {@code Material.iron} residual arm — the cauldron family (not in the tag). */
	static final ImmutableSet<Block> CAULDRON_FAMILY = ImmutableSet.of(
			Blocks.CAULDRON, Blocks.WATER_CAULDRON, Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON);

	/** The upstream ST.torch scan face — the three placeable torches (Behavior_Place_Torch :44). */
	static final ImmutableSet<Item> TORCH_ITEMS = ImmutableSet.of(
			net.minecraft.world.item.Items.TORCH, net.minecraft.world.item.Items.REDSTONE_TORCH,
			net.minecraft.world.item.Items.SOUL_TORCH);

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

	public GTPickaxeItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The upstream isMinableBlock :54-56 modern form — the tag arm + the four explicit
	 * material arms. Static pure function (the crowbar {@code mines} seam) so the offline
	 * tests pin it without constructing the item.
	 */
	public static boolean mines(BlockState aState) {
		return aState.is(BlockTags.MINEABLE_WITH_PICKAXE)
				|| GLASS_FAMILY.contains(aState.getBlock())
				|| ICE_FAMILY.contains(aState.getBlock())
				|| FLOWER_POT_FAMILY.contains(aState.getBlock())
				|| CAULDRON_FAMILY.contains(aState.getBlock());
	}

	/**
	 * The dig-speed seam — {@link #MINING_SPEED} on the mineable surface, the vanilla
	 * 1.0F hand speed elsewhere (the DiggerItem.getDestroySpeed :39-41 shape). The
	 * STACK-FREE steel arm (the legacy fallback and the offline pin); the ladder face
	 * is {@link #destroySpeedBonus(ItemStack, BlockState)}.
	 */
	public static float destroySpeedBonus(BlockState aState) {
		return mines(aState) ? MINING_SPEED : 1.0F;
	}

	/** The ladder dig-speed seam — the form multiplier × the stack's material speed (:483). */
	public static float destroySpeedBonus(ItemStack aStack, BlockState aState) {
		return mines(aState) ? GT6ToolLadder.speed(SPEED_MULTIPLIER, GT6ToolLadder.materialOf(aStack)) : 1.0F;
	}

	/** The level gate (upstream :482) — ZERO speed when the material is too soft for the block. */
	public static boolean qualityGate(ItemStack aStack, BlockState aState) {
		return GT6ToolLadder.qualityGate(aStack, aState);
	}

	/**
	 * The drop authorization — the stack-aware face (forge 1.20.1 IForgeItem overload,
	 * 1.21.1 the vanilla signature): the surface minus the blocks the material quality
	 * cannot harvest. The LIVE vanilla break path on 1.20.1 reaches only the STACKLESS
	 * form below (Player.hasCorrectToolForDrops is stack-free there); the quality face
	 * bites through the ZERO-speed gate, so the coarse floor stays.
	 */
	@Override
	public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
		return !qualityGate(aStack, aState) && coarseFloor(aState);
	}

	/** The quality-blind floor (the steel-or-better semantics, shared both legs). */
	static boolean coarseFloor(BlockState aState) {
		return mines(aState) && !aState.is(BlockTags.NEEDS_DIAMOND_TOOL);
	}

	//? if forge {
	/**
	 * The stackless floor the 1.20.1 break path consults (DiggerItem.isCorrectToolForDrops
	 * :68-77 tag-gate shape with tier level 2 — quality-correct for every steel-or-better
	 * material). 1.21.1 has no stackless form (the stack joined the vanilla signature).
	 */
	@Override
	public boolean isCorrectToolForDrops(BlockState aState) {
		return coarseFloor(aState);
	}
	//?}

	/**
	 * The dig-speed half (the level gate first, the upstream :482 order). The speed
	 * shape itself is the instance face {@link #destroySpeedLadder} — the construction
	 * pick re-points it (the ore-stone penalty).
	 */
	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		if (qualityGate(aStack, aState)) return 0.0F;
		return destroySpeedLadder(aStack, aState);
	}

	/** The instance ladder-speed face (the construction pick re-points the shape). */
	protected float destroySpeedLadder(ItemStack aStack, BlockState aState) {
		return destroySpeedBonus(aStack, aState);
	}

	/** Upstream getToolDamagePerBlockBreak :42 — 25 units fold into one point. */
	@Override
	public boolean mineBlock(ItemStack aStack, Level aLevel, BlockState aState, BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** Upstream getToolDamagePerEntityAttack :43 — 200 units fold into one point. */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}

	/**
	 * The pickaxe arm row: {@code Behavior_Place_Torch} only (Plug_Leak cut — see the
	 * class javadoc; the pickaxe carries no path arm upstream).
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		if (placeTorchFromInventory(aContext)) {
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
	 * The stack classifier — the gt6 pickaxe action + the vanilla PICKAXE_DIG face (the
	 * family shares both; the Gem/Construction subclasses inherit the same classifier).
	 */
	public static boolean classifies(net.minecraftforge.common.ToolAction aToolAction) {
		return GT6ToolActions.PICKAXE == aToolAction
				|| net.minecraftforge.common.ToolActions.PICKAXE_DIG == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, net.minecraftforge.common.ToolAction aToolAction) {
		return classifies(aToolAction);
	}

	/**
	 * The upstream {@code Behavior_Place_Torch.onItemUse} (Behavior_Place_Torch.java:37-59),
	 * modern placement face: scan the player inventory HOTBAR-FIRST (upstream
	 * {@code mainInventory.length-i-1} reversed walk :42-44), the first torch stack tries
	 * {@link BlockItem#place} at the clicked face (the {@code tryPlaceItemIntoWorld}
	 * counterpart — the liquid/legality checks ride the vanilla canPlace), FIRST-FAIL-STOP
	 * (:56 {@code return F}). The stack consumption rides the vanilla placement (creative
	 * exempt). The 1.7.10 tall-grass replace arm (:45) is cut — that was a placement-
	 * validity hack the modern {@code canPlace} supersedes. No durability payment
	 * (upstream charges none).
	 *
	 * @return whether a torch was placed (server verdict; client callers get the
	 *         sidedSuccess claim from {@link #useOn}).
	 */
	public static boolean placeTorchFromInventory(UseOnContext aContext) {
		Player tPlayer = aContext.getPlayer();
		Level tLevel = aContext.getLevel();
		if (tPlayer == null) return false;
		if (tLevel.isClientSide) return true; // claim — the server half decides (the HoeItem sided pattern)
		List<ItemStack> tItems = tPlayer.getInventory().items;
		for (int i = tItems.size() - 1; i >= 0; i--) { // hotbar-first, the upstream reversed walk
			ItemStack tStack = tItems.get(i);
			if (tStack.isEmpty() || !TORCH_ITEMS.contains(tStack.getItem())) continue;
			BlockPlaceContext tPlace = new BlockPlaceContext(tPlayer, aContext.getHand(), tStack,
					new net.minecraft.world.phys.BlockHitResult(aContext.getClickLocation(), aContext.getClickedFace(),
							aContext.getClickedPos(), false)); // the synthetic hit (the GTToolCommand form — getHitResult is protected)
			if (tStack.getItem() instanceof BlockItem tBlockItem) {
				return tBlockItem.place(tPlace).consumesAction(); // vanilla shrinks the stack on success
			}
			return false; // the first torch fails placement → stop (upstream :56)
		}
		return false;
	}

	/**
	 * The upstream {@code Behavior_Place_Path} (Behavior_Place_Path.java:50-74) over the
	 * vanilla-native target: 1.7.10 {@code BlocksGT.Paths} does not exist here, so the
	 * arm lands {@link Blocks#DIRT_PATH} (the vanilla ShovelItem FLATTENABLES
	 * ShovelItem.java:22-31 face — grass/dirt/podzol/coarse/mycelium/rooted). Shared by
	 * the shovel/spade/universal spade (the upstream behavior rows GT_Tool_Spade.java:110
	 * / GT_Tool_Shovel :96 / GT_Tool_UniversalSpade.java:130). One durability point per
	 * conversion (the upstream 50-unit cost through the 10000=1 mapping, folded).
	 *
	 * @return whether the block under the click became a path.
	 */
	public static boolean createPath(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		Block tTarget = FLATTENABLES.get(tLevel.getBlockState(tPos).getBlock());
		if (tTarget == null || aContext.getClickedFace() == Direction.DOWN) return false; // vanilla :42
		if (!tLevel.getBlockState(tPos.above()).isAir()) return false; // upstream :51 the opaque-above gate, vanilla form
		if (tLevel.isClientSide) return true; // claim (vanilla sidedSuccess)
		tLevel.setBlock(tPos, tTarget.defaultBlockState(), 11);
		tLevel.playSound(null, tPos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
		tLevel.gameEvent(aContext.getPlayer(), GameEvent.BLOCK_CHANGE, tPos);
		Player tPlayer = aContext.getPlayer();
		if (tPlayer != null) {
			aContext.getItemInHand().hurtAndBreak(1, tPlayer, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	/** The vanilla ShovelItem FLATTENABLES map (ShovelItem.java:22-31 — the shared 6-block face). */
	static final java.util.Map<Block, Block> FLATTENABLES = java.util.Map.of(
			Blocks.GRASS_BLOCK, Blocks.DIRT_PATH,
			Blocks.DIRT, Blocks.DIRT_PATH,
			Blocks.PODZOL, Blocks.DIRT_PATH,
			Blocks.COARSE_DIRT, Blocks.DIRT_PATH,
			Blocks.MYCELIUM, Blocks.DIRT_PATH,
			Blocks.ROOTED_DIRT, Blocks.DIRT_PATH);

	// ------------------------------ the GT6ItemData identity seams (task p31-dig-ladder) ------------------------------

	/** The per-material durability (the {@link GT6ToolLadder} j/100 points — Steel fallback = 512). */
	@Override
	public int getMaxDamage(ItemStack aStack) {
		return GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(aStack, durabilityMultiplier()));
	}

	/** The form durability multiplier (the gem pick re-points it; ToolStats.java:71 default 1.0). */
	@Override
	public float durabilityMultiplier() {
		return DURABILITY_MULTIPLIER;
	}

	/**
	 * The runtime tint (upstream GT_Tool_Pickaxe.getRGBa :63-65 — the head pass): tint
	 * index 0 = the head layer, the material {@code mRGBaSolid} with the VERBATIM steel
	 * fallback; every other index = the {@code -1} no-tint sentinel.
	 */
	public static int tintARGB(ItemStack aStack, int aTintIndex) {
		return GT6ToolLadder.tintARGB(aStack, aTintIndex);
	}

	/** The composed display name — "Pickaxe (Bronze)"; bare for identity-less stacks. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
	}
}
