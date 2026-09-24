package gregtech6.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.context.UseOnContext;

import gregtech6.registry.GT6Books;

/**
 * The Dusty Guide Book — the loot carrier that opens into a random manual (task
 * p38-book-loot-first). Upstream {@code MultiItemBooks.java:67} meta 32765 "Dusty Guide
 * Book" carrying {@code Behavior_Drop_Loot("gt.books")}: right-click a block to CONSUME the
 * book and drop one random {@code gt.books} pool item at the clicked face
 * ({@code Behavior_Drop_Loot.java:37-47} — one use, one book, the cloth-dig sound). The
 * port pool is the 15 static written books ({@link GT6Books#manualPool()} — the upstream
 * 17-book {@code gt.books} table, Loader_Loot.java:340-356, minus the two p35-CUT dynamic
 * books), all upstream rows weight 144 [1,1] so the pick is uniform.
 *
 * <p>Not a written book itself (no static content — it is the sealed "Loot: Some random
 * Manual or so" package), so this is a plain {@link Item} with the useOn behavior, not a
 * {@link GT6WrittenBookItem}. The sibling Dusty Material Dictionary (32766) is NOT ported:
 * its {@code gt.matdicts} pool is the per-material dynamic book generator
 * ({@code UT.java:769-880}), the p35 dynamic-book CUT class — the declared pool.
 */
public class GT6LootBookItem extends Item {

	/** The upstream tooltip columns (MultiItemBooks.java:67 + Behavior_Drop_Loot.java:31). */
	public static final String TOOLTIP_LOOT_KEY = "item.gt6.book_loot_guide.tooltip";
	public static final String TOOLTIP_USE_KEY = "item.gt6.book_loot_guide.tooltip_loot";

	public GT6LootBookItem(Properties aProperties) {
		super(aProperties);
	}

	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		if (!tLevel.isClientSide()) {
			BlockPos tPos = aContext.getClickedPos();
			Block.popResource(tLevel, tPos.relative(aContext.getClickedFace()),
					new ItemStack(GT6Books.randomManual(tLevel.random)));
			Player tPlayer = aContext.getPlayer();
			if (tPlayer == null || !tPlayer.getAbilities().instabuild) aContext.getItemInHand().shrink(1);
			tLevel.playSound(null, tPos, SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		return InteractionResult.sidedSuccess(tLevel.isClientSide());
	}

	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
		tooltipLines(aTooltip);
	}
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
		//21.1: the hover signature carries the Item.TooltipContext (the GTSprayCanItem fork shape).
		tooltipLines(aTooltip);
	}
	*///?}

	/** The shared tooltip body — the two upstream tooltip columns. */
	private static void tooltipLines(List<Component> aTooltip) {
		aTooltip.add(Component.translatable(TOOLTIP_LOOT_KEY));
		aTooltip.add(Component.translatable(TOOLTIP_USE_KEY));
	}
}
