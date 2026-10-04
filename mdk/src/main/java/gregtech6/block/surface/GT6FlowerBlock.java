package gregtech6.block.surface;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The GT6 indicator flower (task flower-blocks-indicator-family) — the blockstate-form
 * port of the upstream two-meta deco blocks {@code BlockFlowersA} (BlockFlowersA.java:40,
 * gt.block.flower.a, 10 metas) and {@code BlockFlowersB} (:44, gt.block.flower.b, 8 metas)
 * over the shared base {@code BlockBaseFlower} (gregapi/block/misc/BlockBaseFlower.java):
 * the 18 upstream item faces land as 18 INDEPENDENT blocks (the 57 {@code INDICATOR_ROCKS}
 * precedent — one id per meta row, the GT6SurfaceBlocks spec-list walk), one shared class,
 * the two soils split on the upstream canBlockStay override pair (the base
 * BlockBaseFlower.java:131 yellow-flower dirt face vs the BlockFlowersB.java:136-138
 * cactus sand face).
 *
 * <p>The BlockBaseFlower semantics verbatim (the vanilla {@link BushBlock} keeps the
 * cross-shape/no-collision/ground-attach faces):
 * <ul>
 * <li>hardness 0 + blast resistance 0 (:86-:88) — the vanilla {@code instabreak()} row;</li>
 * <li>drops itself, no seeds (damageDropped :97, getItemDropped :101 — the
 *     {@code dropSelf} loot face);</li>
 * <li><b>bone meal drops a COPY of the flower</b> (the IGrowable trio :132-:134 —
 *     target/success always true, perform drops one copy; NOT growth — the upstream
 *     flowers never grow);</li>
 * <li>canBlockStay = oxygen + soil sustain (:131/:136-138) — the {@link #mayPlaceOn}
 *     dirt/sand split below.</li>
 * </ul>
 *
 * <p>DECLARED DEVIATIONS (none silent):
 * <ol>
 * <li><b>Oxygen face CUT</b>: the upstream WD.oxygen gate (:131) and the
 *     oxygen-removal dissolve-to-air (:111) ride the GT oxygen-dimension sim, which the
 *     port has not built — the vanilla overworld is always oxygenated, so the seat is
 *     vacuous until that domain lands.</li>
 * <li><b>1.7.10 light gate CUT</b>: the upstream base canBlockStay also required
 *     light >= 8 or sky (the 1.7.10 BlockFlower form); modern vanilla flowers survive in
 *     the dark (vanilla BushBlock semantics) and the port follows the vanilla face.</li>
 * <li><b>Flower-pot face = the vanilla FlowerPotBlock pairing</b>: the upstream
 *     TileEntityFlowerPot intercept (onItemUse :152-160, filling the vanilla pot with the
 *     flower item) translates as the 18 {@code potted_*} companion blocks on the vanilla
 *     empty-pot pairing (the GT6SurfaceBlocks.POTTED_FLOWERS walk) — the platform-native
 *     form, zero custom code.</li>
 * <li><b>Sword harvest-tool face CUT</b> (getHarvestTool :89 TOOL_sword): the modern
 *     seat is the loot table — instabreak + unconditional self-drop covers the pickup
 *     face; the tool preference has no modern property.</li>
 * <li><b>canBeReplacedByLeaves face CUT</b> (:94): modern tree growth never replaces
 *     blocks — no seat.</li>
 * </ol>
 *
 * <p>The indicator tell rides the item tooltip (the upstream hardcoded addInformation
 * rows, BlockFlowersA.java:62-81 + BlockFlowersB.java:65-84 — hardcoded literals by the
 * upstream form, the zh dump carries no tooltip keys for these faces).
 */
public final class GT6FlowerBlock extends BushBlock implements BonemealableBlock {

	/** True = the BlockFlowersB sand soil (the :136-138 cactus sustain face); false = the
	 * base :131 dirt face (the vanilla BushBlock soil, the yellow-flower sustain arg). */
	private final boolean mSandSoil;

	public GT6FlowerBlock(Properties aProperties, boolean aSandSoil) {
		super(aProperties);
		mSandSoil = aSandSoil;
	}

	//? if neoforge {
	/*
	// 21.1 made BlockBehaviour.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch — the GT6WildBushBlock fork). The flowers are STATELESS (no blockstate
	// property), so the unit codec is the exact face; world save/load never runs through
	// it (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6FlowerBlock> codec() {
		return com.mojang.serialization.MapCodec.unit(this);
	}
	*///?}

	/** The soil split: BlockFlowersB.java:136-138 (sand, the vanilla CactusBlock seat
	 * BlockTags.SAND) vs the base BlockBaseFlower.java:131 (the vanilla BushBlock dirt
	 * row, the Blocks.yellow_flower sustain arg). */
	@Override
	protected boolean mayPlaceOn(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		return mSandSoil ? aState.is(BlockTags.SAND) : super.mayPlaceOn(aState, aLevel, aPos);
	}

	//? if forge {
	@Override
	public boolean isValidBonemealTarget(LevelReader aLevel, BlockPos aPos, BlockState aState, boolean aClient) {
	//?} else {
	/*@Override
	public boolean isValidBonemealTarget(LevelReader aLevel, BlockPos aPos, BlockState aState) {
	//21.1: the isClientSide param dropped (javap BonemealableBlock 21.1.209).
	*///?}
		return true; // func_149851_a :132
	}

	@Override
	public boolean isBonemealSuccess(Level aLevel, RandomSource aRandom, BlockPos aPos, BlockState aState) {
		return true; // func_149852_a :133
	}

	@Override
	public void performBonemeal(ServerLevel aLevel, RandomSource aRandom, BlockPos aPos, BlockState aState) {
		// func_149853_b :134 — ST.drop(this, 1, meta): the bone meal drops a COPY of the
		// flower at the block (NOT growth — the upstream flowers never grow)
		popResource(aLevel, aPos, bonemealDrop());
	}

	/** The :134 copy face, the offline-pinnable seam (one self stack). */
	public ItemStack bonemealDrop() {
		return new ItemStack(this);
	}

	/** The block item with the indicator tooltip (the upstream hardcoded addInformation
	 * rows — the indicator line + the dark-gray "* exists in Real Life" row). */
	public static final class Item extends BlockItem {

		/** The upstream indicator row (BlockFlowersA/B addInformation). */
		private final String mIndicator;
		/** Whether the "* exists in Real Life" row rides it (LH.Chat.DGRAY + the row). */
		private final boolean mRealLife;

		public Item(Block aBlock, Properties aProperties, String aIndicator, boolean aRealLife) {
			super(aBlock, aProperties);
			mIndicator = aIndicator;
			mRealLife = aRealLife;
		}

		//? if forge {
		@Override
		public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
		//?} else {
		/*// 21.1: the Level second parameter became Item.TooltipContext (vanilla 1.21.1
		//Item.java:292 — the same four-argument shape, a different carrier type).
		@Override
		public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aContext, aTooltip, aFlag);
		*///?}
			aTooltip.addAll(tooltipLines(mIndicator, mRealLife));
		}

		/**
		 * The indicator rows (the seam the offline pin drives): the upstream addInformation
		 * literal verbatim + the dark-gray "* exists in Real Life" row (LH.Chat.DGRAY) when
		 * the upstream case carries it.
		 */
		static List<Component> tooltipLines(String aIndicator, boolean aRealLife) {
			if (aRealLife) {
				return List.of(Component.literal(aIndicator),
						Component.literal("* exists in Real Life").withStyle(ChatFormatting.DARK_GRAY));
			}
			return List.of(Component.literal(aIndicator));
		}
	}
}
