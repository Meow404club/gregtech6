package gregtech6.block.multiblock;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.TileEntityLightningRod;

/**
 * The Lightning Rod controller block — the concrete {@link GTMultiBlockControllerBlock}
 * of the single variant (upstream "Lightning Rod Electric Output", MTE 17998,
 * Loader_MultiTileEntities.java:1282 re-read verbatim: ANY.W, hardness == resistance 10.0,
 * texture key "lightningrod"). Everything visual/behavioural is base-owned (FACING +
 * FORMED; the facing is structurally MEANINGLESS here — the rod is vertical — but the
 * shared controller base carries the mirror harmlessly, the boiler form). The class mounts
 * the shared LIGHTNING_ROD_BE.
 *
 * <p>NO use override (no GUI by census, the boiler form), NO explosion family (the rod
 * never explodes upstream), NO placement-face gate (the :180-181 SIDE_BOTTOM placement
 * sides ride the ported convention cut, the BE class doc).
 *
 * <p>The tooltip face (upstream addToolTips :101-115): the BlockItem inner class replays
 * the nine structure/efficiency keys (lang, both locales — the zh rows reference
 * tmp/gregtech.lang :17758-17766) plus the two composed energy lines (the :112
 * "32768 EU/p (up to 16 Amps)" offer and the :113 capacity-per-strike line — the
 * decision-capacity constant).
 */
public class GTLightningRodBlock extends GTMultiBlockControllerBlock {

	public GTLightningRodBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTLightningRodBlock> codec() {
		return simpleCodec(GTLightningRodBlock::new);
	}
	*///?}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTMultiBlocks.LIGHTNING_ROD_BE.get();
	}

	/** The controller item with the upstream addToolTips replay (the :101-115 line order verbatim). */
	public static class Item extends net.minecraft.world.item.BlockItem {

		public static final String KEY_STRUCTURE = "gt6.tooltip.lightningrod.structure";
		public static final String KEY_LINE = "gt6.tooltip.lightningrod.%d";
		public static final String KEY_ENERGY = "gt6.tooltip.lightningrod.energy";
		public static final String KEY_CAPACITY = "gt6.tooltip.lightningrod.capacity";

		public Item(net.minecraft.world.level.block.Block aBlock, Properties aProperties) {
			super(aBlock, aProperties);
		}

		@Override
		public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
			aTooltip.add(Component.translatable(KEY_STRUCTURE).withStyle(ChatFormatting.AQUA)); // the LH.STRUCTURE header, Chat.CYAN
			for (int i = 1; i <= 7; i++) {
				aTooltip.add(Component.translatable(String.format(KEY_LINE, i)).withStyle(ChatFormatting.WHITE)); // :103-109
			}
			aTooltip.add(Component.translatable(String.format(KEY_LINE, 8)).withStyle(ChatFormatting.YELLOW)); // :110
			aTooltip.add(Component.translatable(String.format(KEY_LINE, 9)).withStyle(ChatFormatting.GOLD));   // :111, Chat.ORANGE
			aTooltip.add(Component.translatable(KEY_ENERGY, TileEntityLightningRod.VOLTAGE).withStyle(ChatFormatting.GREEN)); // :112
			aTooltip.add(Component.translatable(KEY_CAPACITY, TileEntityLightningRod.CAPACITY).withStyle(ChatFormatting.WHITE)); // :113
		}
	}
}
