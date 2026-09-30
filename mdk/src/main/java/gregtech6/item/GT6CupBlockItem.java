package gregtech6.item;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.ICapabilityProvider;
//?} else {
/*import net.minecraft.world.item.component.CustomData;
import gregtech6.registry.GT6DataComponents;
 *///?}

import gregtech6.tileentity.tank.GT6CupBlockEntity;
import gregtech6.tileentity.tank.GT6CupItemFluidHandler;

/**
 * The Porcelain Cup BlockItem (task small-tank-cup) — the item carrier of the cup row,
 * the {@link GT6CellBlockItem} face set with the small-tank-family stacking rule:
 *
 * <ul>
 * <li>{@code getMaxStackSize} → 1 when the stack carries content, else the registration
 *     column 16 (upstream TileEntityBase08FluidContainer:423 {@code mTank.has() ? 1 :
 *     aDefault} over the :2094 stack column — the cell family is the one stack-always-64
 *     exception, the cup rides the base rule).</li>
 * <li>{@code FLUID_HANDLER_ITEM} rides every stack through {@link #initCapabilities}
 *     → a {@link GT6CupItemFluidHandler} over the fixed 250 L tank with the liquid-only
 *     door (the :2094 LIQUIDPROOF T / GASPROOF F pair).</li>
 * <li>{@link #placeBlock} completes the round trip upstream ships for free (the MTE
 *     placement constructs the TE from the item NBT): the placed BE reads the item's
 *     {@code tank} back — break a filled cup, place it again, the content survives both
 *     ways.</li>
 * </ul>
 */
public class GT6CupBlockItem extends BlockItem {

	public GT6CupBlockItem(Block aBlock, Properties aProperties) {
		super(aBlock, aProperties);
	}

	/** The upstream :423 face — a filled cup stacks 1, the empty one rides the 16 registration column. */
	@Override
	public int getMaxStackSize(ItemStack aStack) {
		if (hasTankContent(aStack)) return 1;
		return super.getMaxStackSize(aStack);
	}

	/** The content probe over the shared key (the item handler's rehydration reads the same tag). */
	private static boolean hasTankContent(ItemStack aStack) {
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		return tTag != null && tTag.contains(GT6CupBlockEntity.NBT_TANK)
				&& tTag.getCompound(GT6CupBlockEntity.NBT_TANK).contains("Amount")
				&& tTag.getCompound(GT6CupBlockEntity.NBT_TANK).getInt("Amount") > 0;
		//?} else {
		/*CustomData tData = aStack.get(GT6DataComponents.BARREL_CONTENT);
		if (tData == null) return false;
		CompoundTag tTank = tData.copyTag().getCompound(GT6CupBlockEntity.NBT_TANK);
		return tTank.contains("Amount") && tTank.getInt("Amount") > 0;
		 *///?}
	}

	//? if forge {
	@Override
	@Nullable
	public ICapabilityProvider initCapabilities(ItemStack aStack, @Nullable CompoundTag aNBT) {
		// the item-face gate rides the carrier: the fixed 250 L row tank AND the liquid-only door
		return new GT6CupItemFluidHandler(aStack);
	}
	//?} else {
	/*// (1.21.1: IForgeItem.initCapabilities does not exist — the W4 registration wave
	// exposes the same GT6CupItemFluidHandler through RegisterCapabilitiesEvent
	// .registerItem(FLUID_HANDLER_ITEM, item, provider), the cell declared defer.)
	 *///?}

	/**
	 * The item → world half of the round trip (the upstream MTE placement read): after
	 * vanilla places the block, the fresh BE adopts the item's tank — the same key
	 * {@link gregtech6.block.tank.GT6CupBlock#getDrops} wrote into the drop.
	 */
	@Override
	protected boolean placeBlock(BlockPlaceContext aContext, BlockState aState) {
		if (!super.placeBlock(aContext, aState)) return false;
		BlockEntity tBE = aContext.getLevel().getBlockEntity(aContext.getClickedPos());
		if (tBE instanceof GT6CupBlockEntity tCup) applyItemNBT(aContext.getItemInHand(), tCup);
		return true;
	}

	/** The tank read (the GTBarrelBlockItem.applyItemNBT shape over the BE's own load gate — contains-guarded). */
	public static void applyItemNBT(ItemStack aStack, GT6CupBlockEntity aCup) {
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		if (tTag == null) return;
		aCup.readItemNBT(tTag);
		//?} else {
		/*CustomData tData = aStack.get(GT6DataComponents.BARREL_CONTENT);
		if (tData == null || tData.isEmpty()) return;
		aCup.readItemNBT(tData.copyTag());
		 *///?}
	}
}
