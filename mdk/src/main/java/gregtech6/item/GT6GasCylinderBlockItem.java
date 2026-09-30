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

import gregtech6.tileentity.tank.GT6GasCylinderBlockEntity;
import gregtech6.tileentity.tank.GT6GasCylinderItemFluidHandler;

/**
 * The gas-cylinder BlockItem (task small-tank-gas-cylinder) — the item carrier of the
 * barometer gas cylinder family, the GTBarrelBlockItem face set minus the barrel-base
 * entanglement (the cover/paint/tooltip-family rows are TileEntityBase08Barrel seams;
 * the cylinder family has none of them upstream — the plain-BlockItem pot form plus the
 * two faces this family DOES carry):
 *
 * <ul>
 * <li>{@code FLUID_HANDLER_ITEM} rides every stack through {@link #initCapabilities}
 *     (IForgeItem.java:678, the Forge item-capability idiom) → a
 *     {@link GT6GasCylinderItemFluidHandler} over the block-carried 8000 L ceiling with
 *     the LIQUIDPROOF F gate and the NBT_MODE re-bind (the upstream IFluidContainerItem
 *     face — the ForgeCapabilities.java:22 counterpart);</li>
 * <li>{@link #getMaxStackSize(ItemStack)}: a cylinder with content stacks to 1, an empty
 *     one to the 16 row default (upstream getMaxStackSize :423 {@code mTank.has() ? 1 :
 *     aDefault} — the GTBarrelBlockItem shape);</li>
 * <li>{@link #placeBlock} completes the round trip upstream ships for free (the MTE
 *     placement constructs the TE from the item NBT): the placed BE reads the item's
 *     {@code tank}/{@code mode} pair back — break a limited/filled cylinder, place it
 *     again, the limit AND the content survive both ways.</li>
 * </ul>
 */
public class GT6GasCylinderBlockItem extends BlockItem {

	public GT6GasCylinderBlockItem(Block aBlock, Properties aProperties) {
		super(aBlock, aProperties);
	}

	//? if forge {
	@Override
	@Nullable
	public ICapabilityProvider initCapabilities(ItemStack aStack, @Nullable CompoundTag aNBT) {
		// the item-face gates ride the carrier: the 8000 L row ceiling AND the gas-only admission
		return new GT6GasCylinderItemFluidHandler(aStack, GT6GasCylinderBlockEntity.DEFAULT_CAPACITY);
	}
	//?} else {
	/*// (1.21.1: IForgeItem.initCapabilities does not exist — the W4 registration wave
	// exposes the same GT6GasCylinderItemFluidHandler through RegisterCapabilitiesEvent
	// .registerItem(FLUID_HANDLER_ITEM, item, provider), the GTBarrelItemFluidHandler
	// declared defer.)
	 *///?}

	/** Upstream :423 — {@code mTank.has() ? 1 : aDefault}: content kills stacking. */
	@Override
	public int getMaxStackSize(ItemStack aStack) {
		return hasContent(aStack) ? 1 : super.getMaxStackSize(aStack);
	}

	/** True when the stack carries a tank compound — the :423 {@code mTank.has()} item-tag form (the FluidTankGT write gate keeps the key present iff content is). */
	public static boolean hasContent(ItemStack aStack) {
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		return tTag != null && tTag.contains(GT6GasCylinderBlockEntity.NBT_TANK, CompoundTag.TAG_COMPOUND)
				&& !tTag.getCompound(GT6GasCylinderBlockEntity.NBT_TANK).isEmpty();
		//?} else {
		/*CustomData tData = aStack.get(GT6DataComponents.BARREL_CONTENT);
		if (tData == null || tData.isEmpty()) return false;
		CompoundTag tTag = tData.copyTag();
		return tTag.contains(GT6GasCylinderBlockEntity.NBT_TANK, CompoundTag.TAG_COMPOUND)
				&& !tTag.getCompound(GT6GasCylinderBlockEntity.NBT_TANK).isEmpty();
		 *///?}
	}

	/**
	 * The item → world half of the round trip (the upstream MTE placement read): after
	 * vanilla places the block, the fresh BE adopts the item's tank+mode pair — the same
	 * keys {@link GT6GasCylinderBlock#getDrops} wrote into the drop.
	 */
	@Override
	protected boolean placeBlock(BlockPlaceContext aContext, BlockState aState) {
		if (!super.placeBlock(aContext, aState)) return false;
		BlockEntity tBE = aContext.getLevel().getBlockEntity(aContext.getClickedPos());
		if (tBE instanceof GT6GasCylinderBlockEntity tCylinder) applyItemNBT(aContext.getItemInHand(), tCylinder);
		return true;
	}

	/** The pair read (the GTBarrelBlockItem.applyItemNBT shape over the BE's own load gate — every read contains-guarded). */
	public static void applyItemNBT(ItemStack aStack, GT6GasCylinderBlockEntity aCylinder) {
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		if (tTag == null) return;
		aCylinder.readItemNBT(tTag);
		//?} else {
		/*CustomData tData = aStack.get(GT6DataComponents.BARREL_CONTENT);
		if (tData == null || tData.isEmpty()) return;
		aCylinder.readItemNBT(tData.copyTag());
		 *///?}
	}
}
