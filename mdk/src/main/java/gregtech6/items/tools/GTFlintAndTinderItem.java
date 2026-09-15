package gregtech6.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The formal GT6 flint and tinder — item id {@code gt6:flint_and_tinder} (task
 * p29-w5-t5-scene-six spec ④). Upstream GT_Tool_FlintAndTinder.java:35 (registration
 * :143 "Flint and Tinder", mAmount 0):
 * <ul>
 * <li><b>Ignition arm</b> (Behavior_FlintAndTinder.java:45-61): the right-click strike
 *     rides the vanilla {@code FlintAndSteelItem.useOn} faces — the campfire/candle/cake
 *     LIT toggles (:31/:49-51) and the {@code BaseFireBlock} fire placement (:33-37) —
 *     gated by the CHANCE roll: the port keeps the single steel tier, whose material is
 *     neither FLAMMABLE/BURNING, so the upstream :53 arm applies with the proxy default
 *     {@code FlintAndSteelChance = 30} (GT6_Main.java:111) — a 30% strike. The upstream
 *     pays the tool damage UNCONDITIONALLY per strike (:58, success or not) and plays the
 *     ignite sound — both kept. The TOOL_igniter block-broadcast half (:50) has no
 *     {@code IBlockToolable} consumers in the port (the machine cards' domain) — the
 *     vanilla-native faces above ARE the ignition surface (the declared mapping).</li>
 * <li><b>Creeper arm</b> (:69-78, the {@code onLeftClickEntity} ignite): the modern attack
 *     face is {@link #hurtEnemy} — one point (the upstream 100-unit row) and
 *     {@link Creeper#ignite()} (the {@code func_146079_cb} counterpart).</li>
 * </ul>
 *
 * <p>Durability <b>128</b> — the upstream {@code getMaxDurabilityMultiplier() = 0.25F}
 * (GT_Tool_FlintAndTinder.java:47-48) folds onto the 512 family value, the
 * {@code GTPickaxeGemItem} ×0.25 precedent. The recipe pair is the Loader_Tools
 * :207-208 Steel row convergence (the whole :209-247 material ladder is the pool cut).
 */
public class GTFlintAndTinderItem extends Item {

	/** The upstream durability multiplier 0.25F (:47-48) folded onto the 512 family value. */
	public static final int DURABILITY_POINTS = 128;

	/**
	 * The strike chance — the proxy default {@code FlintAndSteelChance = 30}
	 * (GT6_Main.java:111, bound 1..100). The port carries no config surface; the constant
	 * IS the declared default (the single-tier ruling).
	 */
	public static final int IGNITE_CHANCE_PERCENT = 30;

	public GTFlintAndTinderItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The chance strike — vanilla {@code FlintAndSteelItem.useOn} faces behind the 30% roll.
	 * The roll happens FIRST (the upstream chance gate :49-54); a failed strike still pays
	 * the point and still consumes the click (upstream :58/:60-61 return T).
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		Player tPlayer = aContext.getPlayer();
		BlockPos tPos = aContext.getClickedPos();
		BlockState tState = tLevel.getBlockState(tPos);
		if (!tLevel.isClientSide && tPlayer != null) {
			aContext.getItemInHand().hurtAndBreak(1, tPlayer, e -> e.broadcastBreakEvent(aContext.getHand())); // upstream :58 unconditional
		}
		if (CampfireBlock.canLight(tState) || CandleBlock.canLight(tState) || CandleCakeBlock.canLight(tState)) {
			tLevel.playSound(tPlayer, tPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, tLevel.getRandom().nextFloat() * 0.4F + 0.8F);
			if (!tLevel.isClientSide) {
				tLevel.setBlock(tPos, tState.setValue(BlockStateProperties.LIT, Boolean.TRUE), 11);
				tLevel.gameEvent(tPlayer, GameEvent.BLOCK_CHANGE, tPos);
			}
			return InteractionResult.sidedSuccess(tLevel.isClientSide);
		}
		BlockPos tIgnite = tPos.relative(aContext.getClickedFace());
		if (!BaseFireBlock.canBePlacedAt(tLevel, tIgnite, aContext.getHorizontalDirection())) {
			return InteractionResult.FAIL;
		}
		tLevel.playSound(tPlayer, tIgnite, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, tLevel.getRandom().nextFloat() * 0.4F + 0.8F);
		if (!tLevel.isClientSide) {
			if (tLevel.getRandom().nextInt(100) < IGNITE_CHANCE_PERCENT) { // the upstream :53 arm, the proxy default
				tLevel.setBlock(tIgnite, BaseFireBlock.getState(tLevel, tIgnite), 11);
				tLevel.gameEvent(tPlayer, GameEvent.BLOCK_PLACE, tPos);
			}
		}
		return InteractionResult.sidedSuccess(tLevel.isClientSide);
	}

	/** The creeper arm (Behavior_FlintAndTinder.java:69-78) — the attack face, one point + ignite. */
	@Override
	public boolean hurtEnemy(ItemStack aStack, LivingEntity aTarget, LivingEntity aAttacker) {
		if (!aTarget.level().isClientSide && aTarget instanceof Creeper tCreeper) {
			tCreeper.ignite(); // the upstream func_146079_cb counterpart
			tCreeper.level().playSound(null, tCreeper.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
			aStack.hurtAndBreak(1, aAttacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
			return true;
		}
		return super.hurtEnemy(aStack, aTarget, aAttacker);
	}
}
