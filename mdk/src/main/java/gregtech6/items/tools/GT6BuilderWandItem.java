package gregtech6.items.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ToolAction;

import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;

/**
 * The formal GT6 Builder's Wand — task p24-builder-wand. Upstream the tool is a meta id
 * (Loader_Tools.java:153 {@code ToolsGT.sMetaTool.addTool(BUILDERWAND, "Builder Wand", ...)}
 * with the behaviour pair GT_Tool_Builderwand.onStatsAddedToTool :61-64 mounts:
 * {@code Behavior_Builderwand} (the surface-extension arm, the separate card
 * p24-wand-surface-arm) + {@code Behavior_Tool(TOOL_builderwand, SFX.MC_XP, 100, ...)}).
 * This card ports the MULTIBLOCK SCAFFOLD arm — the click flattens the upstream
 * IBlockToolable broadcast onto a {@link #useOn} direct dispatch (the GTCrowbarItem
 * port-ism shape, ADR 2026-09-01-p9-tool-crowbar ⑦), because 1.20.1 has no per-item
 * pre-use hook and the port has no tool-broadcast layer (the research gap ③ pool).
 *
 * <p>The dispatch (upstream TileEntityBase10MultiBlockBase.onToolClick2 :141-146, the
 * {@code TOOL_builderwand} arm): server-side only (:131/:142 {@code isClientSide() → 0}),
 * then {@code checkStructure2(clickedPos, player, playerInventory)} (the placing pass)
 * + {@code checkStructure(true)} (the linking pass) + return 10 raw units — UNCONDITIONALLY,
 * the formed verdict is not consulted, so the wand pays per CLICK, not per formation.
 * Behavior_Tool :62-65 folds the 10 units through {@code units(10, 10000, 100, T)} = 1
 * vanilla durability point (the UT.java:1677-1682 roundUp math) and plays the tool sound
 * at the clicked block. The same shape serves the linked-part click through the
 * {@link MultiBlockPartBlockEntity#wandTarget} relay (upstream part :261 — the wand and
 * the magnifier are the only part-relay tools; the magnifier face is not ported).
 *
 * <p>Declared deviations (the task card, the single-tier ruling 2026-09-07):
 * <ul>
 * <li>the upstream material ladder (radius = toolQuality+1, durability ×0.1) collapses
 *     to ONE tier — radius 2 (the mid-gem quality+1) and durability 512, the pinned
 *     crowbar/cutter/chisel/file/saw family value; the surface arm is card
 *     p24-wand-surface-arm;</li>
 * <li>the upstream no-controller chat line (part :256-258 "There is no Multiblock
 *     Controller for this Block.") is a silent {@code PASS} — the {@code aChatReturn}
 *     mechanism has no port counterpart;</li>
 * <li>the per-cell placement sound stays cut at the Util seam (Util :144 note) — the
 *     click sound lives HERE, mapped from the upstream SFX.MC_XP trio (GT_Tool_Builderwand
 *     :43-45) to vanilla {@link SoundEvents#EXPERIENCE_ORB_PICKUP} with the
 *     {@code SFX.RANDOM_PITCH} flag (:63) as a light pitch wobble.</li>
 * </ul>
 *
 * <p>The creative ruling (decisions.p24-builder-wand-op2-reform consumed at the Util/checker
 * feeds): any mayBuild player scaffolds from their inventory — survival pays the parts,
 * creative pays nothing for the parts (the Util infinite-items arm) but STILL pays the
 * wand wear: the upstream {@code doDamage} (Behavior_Tool :63) carries no creative
 * exemption, while vanilla {@link ItemStack#hurtAndBreak} silently no-ops for instabuild
 * (ItemStack.java:333-334) — so the payment rides the ungated {@link ItemStack#hurt}
 * channel ({@link #payClick}), the break tail mirrored from the hurtAndBreak body
 * (:336-345).
 */
public class GT6BuilderWandItem extends Item {

	/** The vanilla durability points — the single tier (the pinned tool-family value). */
	public static final int DURABILITY_POINTS = 512;

	/**
	 * The single-tier scaffold radius — the upstream {@code getPrimaryMaterial()
	 * .mToolQuality+1} (Behavior_Builderwand :86) collapsed to the mid-gem quality+1
	 * (declared deviation; consumed by the surface-extension card p24-wand-surface-arm).
	 */
	public static final int SCAFFOLD_RADIUS = 2;

	/**
	 * The upstream units the scaffold dispatch returns per server click
	 * (TileEntityBase10MultiBlockBase.java:135/:145 — unconditional).
	 */
	public static final long SCAFFOLD_TOOL_DAMAGE = 10;

	/**
	 * The upstream internal-units scalar — 10000 units = one vanilla durability point
	 * (Behavior_Tool :63 {@code units(tDamage, 10000, mDamage=100, T)} with the :135 ten
	 * units folds to exactly 1: {@link #WEAR_PER_CLICK}).
	 */
	public static final long UNITS_PER_DURABILITY_POINT = 10000;

	/** The folded per-click wear — roundUp(10 * 100 / 10000) = 1 point, click NOT formation. */
	public static final int WEAR_PER_CLICK = 1;

	public GT6BuilderWandItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The flattened Behavior_Tool.onItemUseFirst — the scaffold click. PASS on everything
	 * that does not resolve to a scaffold target (the declared silent deviation); claims
	 * on the client only when the target resolves (the crowbar claim-mirrors-the-server
	 * shape, GTCrowbarItem.useOn :180-188), the server executes and CONSUMEs.
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		TileEntityBase10MultiBlockBase tController = scaffoldTarget(tLevel, aContext.getClickedPos());
		if (tController == null) {
			return InteractionResult.PASS; // the no-controller arm — silent (the javadoc deviation)
		}
		if (tLevel.isClientSide) {
			return InteractionResult.SUCCESS; // claim, the server side executes
		}
		Player tPlayer = aContext.getPlayer();
		builderWandScaffold(tController, aContext.getClickedPos(), tPlayer,
				tPlayer == null ? null : tPlayer.getInventory(), aContext.getItemInHand());
		return InteractionResult.CONSUME;
	}

	/**
	 * The scaffold-target resolution — the controller itself (the upstream
	 * onToolClick2 direct arm) or a linked multiblock part (the :261 relay arm through
	 * {@link MultiBlockPartBlockEntity#wandTarget}). Static so the offline tests can pin
	 * it over the stub level (the mod-Item wall keeps the item unconstructible there).
	 */
	@Nullable
	public static TileEntityBase10MultiBlockBase scaffoldTarget(Level aLevel, BlockPos aClickedAt) {
		BlockEntity tTileEntity = aLevel.getBlockEntity(aClickedAt);
		if (tTileEntity instanceof TileEntityBase10MultiBlockBase tController) {
			return tController;
		}
		if (tTileEntity instanceof MultiBlockPartBlockEntity tPart) {
			return tPart.wandTarget();
		}
		return null;
	}

	/**
	 * The single dispatch + payment surface, shared by {@link #useOn} and the offline
	 * doubles (the crowbarToolClick single-source-semantics shape). Server-side gate,
	 * the two-pass upstream arm, the click sound, then the wear — and the unconditional
	 * upstream {@code return 10} (the formed verdict rides the controller's own
	 * mStructureOkay state, not the click's return).
	 *
	 * @return the upstream tool damage ({@link #SCAFFOLD_TOOL_DAMAGE}) or 0 on the client.
	 */
	public static long builderWandScaffold(TileEntityBase10MultiBlockBase aController, BlockPos aClickedAt,
			@Nullable Player aPlayer, @Nullable Container aInventory, ItemStack aStack) {
		if (aController.isClientSide()) return 0; // upstream :131/:142
		aController.checkStructure2(aClickedAt, aPlayer, aInventory); // the placing pass (:132/:143)
		aController.checkStructure(true);                            // the linking pass (:133/:144)
		scaffoldSound(aController.getLevel(), aClickedAt);
		if (aPlayer != null) {
			payClick(aStack, aPlayer); // 10 units → 1 point, per click, no creative exemption
		}
		return SCAFFOLD_TOOL_DAMAGE;
	}

	/**
	 * The click sound — SFX.MC_XP (GT_Tool_Builderwand :43-45, played at the clicked
	 * block by Behavior_Tool :64) mapped to the vanilla XP-orb pickup, the :63
	 * {@code SFX.RANDOM_PITCH} flag as a ±10% wobble.
	 */
	private static void scaffoldSound(@Nullable Level aLevel, BlockPos aClickedAt) {
		if (aLevel != null) {
			aLevel.playSound(null, aClickedAt, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
					1.0F, 0.9F + aLevel.random.nextFloat() * 0.2F);
		}
	}

	/**
	 * The per-click payment. The survival face rides the vanilla
	 * {@link ItemStack#hurtAndBreak} channel (the card's {@code hurtAndBreak(1)} face);
	 * the CREATIVE face is the raw {@link #wearOne} arm — the vanilla channels silently
	 * skip instabuild (forge 1.20.1 ItemStack.java:333-334; 1.21.1 the
	 * {@code hasInfiniteMaterials} gate ItemStack.java:456) while the upstream
	 * {@code doDamage} exempts nobody (Behavior_Tool :63). The instabuild break
	 * announcement is the leg's break face: {@code broadcastBreakEvent} on 1.20.1,
	 * {@code onEquippedItemBroken} on 21.1 (LivingEntity.java:3505).
	 */
	public static void payClick(ItemStack aStack, Player aPlayer) {
		if (aPlayer.level().isClientSide) return; // the vanilla client gate (:334)
		//? if forge {
		aStack.hurtAndBreak(WEAR_PER_CLICK, aPlayer, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		//?} else {
		/*aStack.hurtAndBreak(WEAR_PER_CLICK, aPlayer, EquipmentSlot.MAINHAND); // the break event rides inside
		*///?}
		if (aPlayer.getAbilities().instabuild && aStack.getCount() > 0 && wearOne(aStack)) {
			//? if forge {
			aPlayer.broadcastBreakEvent(EquipmentSlot.MAINHAND);
			//?} else {
			/*aPlayer.onEquippedItemBroken(aStack.getItem(), EquipmentSlot.MAINHAND);
			*///?}
		}
	}

	/**
	 * The raw wear arm — one UNGATED point (the upstream doDamage shape): exactly
	 * +{@link #WEAR_PER_CLICK} damage, nobody exempted, break = shrink + damage reset
	 * (the hurtAndBreak tail, forge 1.20.1 ItemStack.java:339-344). Player-free so the
	 * offline tests can pin the arithmetic and the exact wear-out boundary over vanilla
	 * damageable stacks (the FileSawTest craftRemaining-seam shape; a mod Item cannot be
	 * constructed in the bootstrapped-and-frozen test JVM).
	 *
	 * @return true when this wear consumed the wand.
	 */
	public static boolean wearOne(ItemStack aStack) {
		if (!aStack.isDamageableItem() || aStack.getCount() <= 0) return false;
		int tNext = aStack.getDamageValue() + WEAR_PER_CLICK;
		if (tNext >= aStack.getMaxDamage()) {
			aStack.shrink(1); // :339
			aStack.setDamageValue(0); // :344
			return true;
		}
		aStack.setDamageValue(tNext);
		return false;
	}

	/** The stack-classification face (the crowbar/file static-seam shape). */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.BUILDER_WAND == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
