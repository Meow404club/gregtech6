package gregtech6.item;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
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
 *     filled barrel, place it again, the content survives both ways;</li>
 * <li>{@link #applyItemNBT} also rehydrates the paint root-key pair (task
 *     p23-barrel-paint-item-seam): a spray-painted barrel's drop carries
 *     {@code gt.color}/{@code gt.painted} (written by {@code GTBarrelBlock
 *     .writeItemNBT}, the 07Paintable :89 root-key seam), and placement reads the
 *     colour back — the 16-row family (4 barrels + 12 drums) shares this static
 *     seam, so the whole family rides it automatically.</li>
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
	 *
	 * <p>Task p23-barrel-paint-item-seam adds the paint keys to the same seam: the
	 * {@code gt.color}/{@code gt.painted} root pair rides the item, and the readback
	 * goes through the BE's public {@code load} gate with a paint-only tag — that runs
	 * the exact {@code TileEntityBase03TicksAndSync.load} :339-340 hasKey-guarded
	 * assignment pair (direct field writes), which even the painted-white case needs
	 * ({@code paint(UNCOLORED)} would no-op on the {@code aRGB != mRGBa} guard and
	 * drop the painted flag). Every other read in the {@code load} chain is
	 * contains-guarded (FluidTankGT.readFromNBT :79, ICoverableTE.readCoversFromNBT
	 * :519), so the paint-only tag touches nothing else.
	 */
	@Override
	protected boolean placeBlock(BlockPlaceContext aContext, BlockState aState) {
		if (!super.placeBlock(aContext, aState)) return false;
		BlockEntity tBE = aContext.getLevel().getBlockEntity(aContext.getClickedPos());
		if (tBE instanceof TileEntityBase08Barrel tBarrel) applyItemNBT(aContext.getItemInHand(), tBarrel);
		return true;
	}

	public static void applyItemNBT(ItemStack aStack, TileEntityBase08Barrel aBarrel) {
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		if (tTag == null) return;
		aBarrel.mTank.readFromNBT(tTag, TileEntityBase08Barrel.NBT_TANK);
		aBarrel.readCoversFromNBT(tTag); // upstream 06Covers :68 — covers ride the same item tag
		if (tTag.contains(TileEntityBase03TicksAndSync.NBT_PAINTED)
				|| tTag.contains(TileEntityBase03TicksAndSync.NBT_COLOR, Tag.TAG_ANY_NUMERIC)) {
			CompoundTag tPaint = new CompoundTag();
			if (tTag.contains(TileEntityBase03TicksAndSync.NBT_COLOR, Tag.TAG_ANY_NUMERIC)) tPaint.putInt(TileEntityBase03TicksAndSync.NBT_COLOR, tTag.getInt(TileEntityBase03TicksAndSync.NBT_COLOR));
			if (tTag.contains(TileEntityBase03TicksAndSync.NBT_PAINTED)) tPaint.putBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED, tTag.getBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED));
			aBarrel.load(tPaint); // the 03 load :339-340 gate pair — the same assignment code the chunk data runs
		}
		aBarrel.setChanged();
		//?} else {
		/*CustomData tData = aStack.get(GT6DataComponents.BARREL_CONTENT);
		CustomData tCovers = aStack.get(GT6DataComponents.COVER_PAYLOAD);
		CompoundTag tPaint = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				CustomData.EMPTY).copyTag(); // the 21.1 root-tag envelope (GTItemPaintTint read fork shape)
		if (tData == null && tCovers == null && !tPaint.contains(TileEntityBase03TicksAndSync.NBT_PAINTED)
				&& !tPaint.contains(TileEntityBase03TicksAndSync.NBT_COLOR, Tag.TAG_ANY_NUMERIC)) return;
		if (tData != null) aBarrel.mTank.readFromNBT(tData.copyTag(), TileEntityBase08Barrel.NBT_TANK);
		if (tCovers != null) aBarrel.readCoversFromNBT(tCovers.copyTag()); // the 's'..'x' lane keys ride COVER_PAYLOAD
		if (tPaint.contains(TileEntityBase03TicksAndSync.NBT_PAINTED)
				|| tPaint.contains(TileEntityBase03TicksAndSync.NBT_COLOR, Tag.TAG_ANY_NUMERIC)) {
			CompoundTag tPaintOnly = new CompoundTag();
			if (tPaint.contains(TileEntityBase03TicksAndSync.NBT_COLOR, Tag.TAG_ANY_NUMERIC)) tPaintOnly.putInt(TileEntityBase03TicksAndSync.NBT_COLOR, tPaint.getInt(TileEntityBase03TicksAndSync.NBT_COLOR));
			if (tPaint.contains(TileEntityBase03TicksAndSync.NBT_PAINTED)) tPaintOnly.putBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED, tPaint.getBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED));
			aBarrel.load(tPaintOnly); // the 03 load :339-340 gate pair
		}
		aBarrel.setChanged();
		 *///?}
	}
}
