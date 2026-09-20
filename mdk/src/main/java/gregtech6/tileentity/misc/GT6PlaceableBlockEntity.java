package gregtech6.tileentity.misc;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GT6Placeables;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The placed-pile BlockEntity (task p32-placeables) — the port of the upstream
 * {@code MultiTileEntityPlaceable} base (gregapi/tileentity/misc/MultiTileEntityPlaceable
 * .java:48-139) shared over the six placement faces (the Ingot/Plate/GemPlate/Scrap/Rock/
 * Stick placed piles, Loader_MultiTileEntities.java:2033-2040; the ADR-P3-1 shared-BET
 * multi-mount — one BE class over six block carriers, the kind read off the mounted
 * block). The upstream class split (Ingot/Plate/PlateGem/Scrap/RockPlaced/StickPlaced)
 * carried no behavioural difference beyond the item the pile renders/eats — the kind now
 * lives on the block carrier.
 *
 * <p>State: ONE persisted ItemStack (the upstream {@code mStack} NBT_VALUE :56-69) — the
 * pile's contents and identity; {@code mMaterial} (:60) derives live from the stack's
 * {@link MaterialPrefixItem} (the port has no OreDictItemData NBT face — the material item
 * system IS the identity carrier); {@code mSize} folds into the stack count. The
 * upstream right-click economy (:77-113) ports as the take/merge pair on the block: an
 * equal held stack MERGES into the pile (the :81-96 arm), any other click gives ONE back
 * (the :98 give arm; the :100-105 top-of-column walk is the declared cut — a nicety over
 * the pile-column nicety it serves upstream).
 */
public class GT6PlaceableBlockEntity extends TileEntityBase03TicksAndSync {

	/** The persisted contents key (the upstream NBT_VALUE fold). */
	public static final String NBT_VALUE = "gt6.value";

	private ItemStack mStack = ItemStack.EMPTY;

	public GT6PlaceableBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GTBarrelBlockEntity null-type form). */
	public GT6PlaceableBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : GT6Placeables.PLACEABLE_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "placed_pile"; // BET registry path mirrors it (GT6Placeables.PLACEABLE_BE)
	}

	/** The pile contents (never mutated in place — callers set whole stacks). */
	public ItemStack stack() {
		return mStack;
	}

	public void setStack(ItemStack aStack) {
		mStack = aStack.copy();
		setChanged();
		sendClientData(); // non-ticking BE: flush the sync window directly
	}

	/**
	 * The pile's tint material (the upstream mMaterial :60 derivation) — the
	 * {@link MaterialPrefixItem} identity, or null for the vanilla borrows (flint/stick).
	 */
	@Nullable
	public gregapi.oredict.OreDictMaterial material() {
		return mStack.getItem() instanceof MaterialPrefixItem tItem ? tItem.material : null;
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (!mStack.isEmpty()) {
			//? if forge {
			aNBT.put(NBT_VALUE, mStack.save(new CompoundTag()));
			//?} else {
			/*aNBT.put(NBT_VALUE, mStack.save(NBT_ACCESS, new CompoundTag())); // 21.1: provider-first save (the TileEntityOven fork face)
			*///?}
		}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_VALUE, Tag.TAG_COMPOUND)) {
			//? if forge {
			mStack = ItemStack.of(aNBT.getCompound(NBT_VALUE));
			//?} else {
			/*mStack = ItemStack.parse(NBT_ACCESS, aNBT.getCompound(NBT_VALUE)).orElse(ItemStack.EMPTY);
			//21.1: ItemStack.of died with components — the provider-first parse face (javap 21.1.249)
			*///?}
		} else {
			mStack = ItemStack.EMPTY;
		}
	}
}
