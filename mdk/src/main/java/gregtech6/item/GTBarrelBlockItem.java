package gregtech6.item;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.ICapabilityProvider;
//?} else {
/*import net.minecraft.world.item.component.CustomData;
import gregtech6.registry.GT6DataComponents;
 *///?}

import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.tileentity.tank.GTBarrelItemFluidHandler;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;

/**
 * The barrel BlockItem (task p12-fluid-item-carrier spec ②) — the item carrier of the
 * fluid-barrel family, the 1.20.1 counterpart of the upstream MTE item shell
 * ({@code MultiTileEntityItemInternal}, the item face that delegates to the TE NBT):
 *
 * <ul>
 * <li>{@code FLUID_HANDLER_ITEM} rides every stack through {@link #initCapabilities}
 *     (IForgeItem.java:678, the Forge item-capability idiom) → a
 *     {@link GTBarrelItemFluidHandler} over the block-carried capacity — the
 *     {@code IFluidContainerItem} face (forge-api 1.7.10 :16-59 → ForgeCapabilities.java:22);</li>
 * <li>{@link #getMaxStackSize(ItemStack)}: a barrel with content stacks to 1, an empty
 *     one to the 16 default — upstream {@code getMaxStackSize(ItemStack, byte)}
 *     TileEntityBase08Barrel.java:290 {@code mTank.has() ? 1 : aDefault}; the empty
 *     default is the {@code Item.Properties().stacksTo(16)} the registry rows carry;</li>
 * <li>{@link #placeBlock} completes the round trip upstream ships for free (the MTE
 *     placement constructs the TE from the item NBT): the placed BE reads the item's
 *     {@code tank}/{@code covers} tags back through {@link #applyItemNBT} — break a
 *     filled barrel, place it again, the content survives both ways.</li>
 * </ul>
 */
public class GTBarrelBlockItem extends BlockItem {

	public GTBarrelBlockItem(GTBarrelBlock aBlock, Properties aProperties) {
		super(aBlock, aProperties);
	}

	//? if forge {
	@Override
	@Nullable
	public ICapabilityProvider initCapabilities(ItemStack aStack, @Nullable CompoundTag aNBT) {
		// the task-p13 item-face gates ride the same carrier: capacity AND the NBT_GASPROOF row flag
		return new GTBarrelItemFluidHandler(aStack, ((GTBarrelBlock) getBlock()).capacityL())
				.setGasProof(((GTBarrelBlock) getBlock()).gasProof());
	}
	//?} else {
	/*// (1.21.1: IForgeItem.initCapabilities does not exist — the item face moves to the W4
	// registration wave via RegisterCapabilitiesEvent.registerItem(FLUID_HANDLER_ITEM, item,
	// provider), constructing the same GTBarrelItemFluidHandler over capacityL()+gasProof().)
	 *///?}

	/** Upstream :290 — {@code mTank.has() ? 1 : aDefault}: content kills stacking. */
	@Override
	public int getMaxStackSize(ItemStack aStack) {
		return hasContent(aStack) ? 1 : super.getMaxStackSize(aStack);
	}

	/** True when the stack carries a tank compound — the :290 {@code mTank.has()} item-tag form (the FluidTankGT write gate keeps the key present iff content is). */
	public static boolean hasContent(ItemStack aStack) {
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		return tTag != null && tTag.contains(TileEntityBase08Barrel.NBT_TANK, CompoundTag.TAG_COMPOUND)
				&& !tTag.getCompound(TileEntityBase08Barrel.NBT_TANK).isEmpty();
		//?} else {
		/*CustomData tData = aStack.get(GT6DataComponents.BARREL_CONTENT);
		if (tData == null || tData.isEmpty()) return false;
		CompoundTag tTag = tData.copyTag();
		return tTag.contains(TileEntityBase08Barrel.NBT_TANK, CompoundTag.TAG_COMPOUND)
				&& !tTag.getCompound(TileEntityBase08Barrel.NBT_TANK).isEmpty();
		 *///?}
	}

	/**
	 * The item → world half of the round trip (the upstream MTE placement read): after
	 * vanilla places the block, the fresh BE adopts the item's tank and covers NBT —
	 * the same keys {@code GTBarrelBlock.writeItemNBT} wrote into the drop.
	 */
	@Override
	protected boolean placeBlock(BlockPlaceContext aContext, BlockState aState) {
		if (!super.placeBlock(aContext, aState)) return false;
		BlockEntity tBE = aContext.getLevel().getBlockEntity(aContext.getClickedPos());
		if (tBE instanceof TileEntityBase08Barrel tBarrel) applyItemNBT(aContext.getItemInHand(), tBarrel);
		return true;
	}

	/** The read seam over the same-key NBT pair: item tag → BE tank + covers (mirrors {@code TileEntityBase08Barrel.load} :129-136). */
	public static void applyItemNBT(ItemStack aStack, TileEntityBase08Barrel aBarrel) {
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		if (tTag == null) return;
		aBarrel.mTank.readFromNBT(tTag, TileEntityBase08Barrel.NBT_TANK);
		aBarrel.readCoversFromNBT(tTag); // upstream 06Covers :68 — covers ride the same item tag
		aBarrel.setChanged();
		//?} else {
		/*CustomData tData = aStack.get(GT6DataComponents.BARREL_CONTENT);
		CustomData tCovers = aStack.get(GT6DataComponents.COVER_PAYLOAD);
		if (tData == null && tCovers == null) return;
		if (tData != null) aBarrel.mTank.readFromNBT(tData.copyTag(), TileEntityBase08Barrel.NBT_TANK);
		if (tCovers != null) aBarrel.readCoversFromNBT(tCovers.copyTag()); // the 's'..'x' lane keys ride COVER_PAYLOAD
		aBarrel.setChanged();
		 *///?}
	}
}
