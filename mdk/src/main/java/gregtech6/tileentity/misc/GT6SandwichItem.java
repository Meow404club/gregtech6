package gregtech6.tileentity.misc;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Sandwich item (task p32-placeables) — the BlockItem of the MTE 32105 port, the
 * upstream "OnlyPlaceableWhenSneaking + OnItemRightClick eat" pair over the modern faces:
 * <ul>
 * <li><b>SNEAK-PLACE</b> — {@code useOn} places ONLY while sneaking (the upstream
 *     {@code IMTE_OnlyPlaceableWhenSneaking}, MultiTileEntitySandwich.java:189 + the
 *     {@code canPlace} :196 solid-floor door stays the block's {@code canSurvive} pool
 *     cut — the vanilla placement already refuses the replaceable-only spots); a
 *     non-sneaking click returns PASS so the vanilla food walk takes over.</li>
 * <li><b>EAT</b> — the item is food ({@code Item.use} default), nutrition 8 / saturation
 *     0.6 (the upstream item-form eat is the whole-sandwich {@code getTotalFood} sum, the
 *     default ten-layer sandwich ≈ 8 — the ingredient-sum face is the food-domain pool).</li>
 * </ul>
 * The placed form carries the default sandwich NBT (the BE {@code DEFAULT_SIZE}); the
 * upstream toast-slice placement arm (GT_Proxy.java:286-289, Food_Toast_Sliced) has no
 * port carrier — the food-domain pool.
 */
public class GT6SandwichItem extends BlockItem {

	/** The whole-sandwich item-form food (the default sandwich sum fold). */
	public static final FoodProperties SANDWICH_FOOD = new FoodProperties.Builder()
			.nutrition(8)
			//? if forge {
			.saturationMod(0.6F)
			//?} else {
			/*.saturationModifier(0.6F)
			//21.1: FoodProperties.Builder.saturationMod → saturationModifier (javap 21.1.249)
			*///?}
			.build();

	public GT6SandwichItem(Block aBlock, Properties aProperties) {
		super(aBlock, aProperties.food(SANDWICH_FOOD));
	}

	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		// sneak-gate: the non-sneaking click falls through to the vanilla eat walk (PASS),
		// the sneaking click places (the vanilla BlockItem walk)
		if (aContext.getPlayer() == null || !aContext.getPlayer().isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		return super.useOn(aContext);
	}

	/** The vanilla BlockItem placement — unchanged; the sneak gate lives in useOn. */
	@Override
	protected boolean placeBlock(BlockPlaceContext aContext, BlockState aState) {
		return super.placeBlock(aContext, aState);
	}
}
