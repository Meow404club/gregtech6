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

import gregtech6.tileentity.tank.GT6CellBlockEntity;
import gregtech6.tileentity.tank.GT6CellItemFluidHandler;

/**
 * The cell BlockItem (task small-tank-cell) — the item carrier of the
 * Capsule-Cell-Container family, the GT6GasCylinderBlockItem face set with ONE
 * family-defining difference:
 *
 * <ul>
 * <li>NO {@code getMaxStackSize} override — upstream MultiTileEntityCell.java:76
 *     {@code getMaxStackSize(aStack, aDefault) → aDefault} overrides the base
 *     :423 content-kills-stacking form away, so a FILLED cell still stacks to 64
 *     (the one small-tank family that does; the 64 rides the row properties).</li>
 * <li>{@code FLUID_HANDLER_ITEM} rides every stack through {@link #initCapabilities}
 *     → a {@link GT6CellItemFluidHandler} over the fixed 1000 L tank with the
 *     LIQUIDPROOF F gas-only gate (the upstream IFluidContainerItem face — no
 *     NBT_MODE: the cell capacity is fixed, unlike the cylinder limit).</li>
 * <li>{@link #placeBlock} completes the round trip upstream ships for free (the MTE
 *     placement constructs the TE from the item NBT): the placed BE reads the item's
 *     {@code tank} back — break a filled cell, place it again, the content survives
 *     both ways.</li>
 * </ul>
 */
public class GT6CellBlockItem extends BlockItem {

	public GT6CellBlockItem(Block aBlock, Properties aProperties) {
		super(aBlock, aProperties);
	}

	//? if forge {
	@Override
	@Nullable
	public ICapabilityProvider initCapabilities(ItemStack aStack, @Nullable CompoundTag aNBT) {
		// the item-face gate rides the carrier: the fixed 1000 L row tank AND the gas-only admission
		return new GT6CellItemFluidHandler(aStack, GT6CellBlockEntity.CAPACITY);
	}
	//?} else {
	/*// (1.21.1: IForgeItem.initCapabilities does not exist — the W4 registration wave
	// exposes the same GT6CellItemFluidHandler through RegisterCapabilitiesEvent
	// .registerItem(FLUID_HANDLER_ITEM, item, provider), the gas-cylinder declared defer.)
	 *///?}

	/**
	 * The item → world half of the round trip (the upstream MTE placement read): after
	 * vanilla places the block, the fresh BE adopts the item's tank — the same key
	 * {@link gregtech6.block.tank.GT6CellBlock#getDrops} wrote into the drop.
	 */
	@Override
	protected boolean placeBlock(BlockPlaceContext aContext, BlockState aState) {
		if (!super.placeBlock(aContext, aState)) return false;
		BlockEntity tBE = aContext.getLevel().getBlockEntity(aContext.getClickedPos());
		if (tBE instanceof GT6CellBlockEntity tCell) applyItemNBT(aContext.getItemInHand(), tCell);
		return true;
	}

	/** The tank read (the GTBarrelBlockItem.applyItemNBT shape over the BE's own load gate — contains-guarded). */
	public static void applyItemNBT(ItemStack aStack, GT6CellBlockEntity aCell) {
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		if (tTag == null) return;
		aCell.readItemNBT(tTag);
		//?} else {
		/*CustomData tData = aStack.get(GT6DataComponents.BARREL_CONTENT);
		if (tData == null || tData.isEmpty()) return;
		aCell.readItemNBT(tData.copyTag());
		 *///?}
	}
}
