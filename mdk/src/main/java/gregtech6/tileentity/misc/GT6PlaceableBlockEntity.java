package gregtech6.tileentity.misc;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GT6Placeables;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The placed-pile BlockEntity (task placeables) — the port of the upstream
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
 *
 * <p>The Nameable face (task r11-placed-rock-identity-drops, the surface_rock-parity
 * ruling): the pile's displayed identity IS the stored stack — {@link #getCustomName()}
 * hands out the stack's hover name so the Jade default ObjectNameProvider streams it as
 * the tooltip TITLE (both legs verified: jade-1201 ObjectNameProvider streamData javap
 * — Nameable + hasCustomName → {@code givenName} = Component.Serializer.toJson(
 * getCustomName()); jade-1211 :152-157 — → stream getDisplayName()). This is the
 * {@link gregtech6.block.surface.GT6SurfaceRockBlock} display canon transplanted to the
 * BE-carried identity: that family is one block instance per material so its
 * {@code getName()} override carries the material word; the piles are ONE block over six
 * carriers so the parameterized name must ride the BE. Empty pile → null custom name →
 * every consumer falls back to the block name (石头, the upstream Rock). Zero Jade code,
 * zero lang keys — the stack's own name component chain resolves per client locale.
 */
public class GT6PlaceableBlockEntity extends TileEntityBase03TicksAndSync implements Nameable {

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

	// --- the Nameable face (class doc): the Jade TITLE name = the stored stack ---

	/** The carrier block's own name (石头) — the empty-pile / generic fallback. */
	@Override
	public Component getName() {
		return this.getBlockState().getBlock().getName();
	}

	/** The stored stack's hover name (含铁岩石), or null when empty (the Jade fallback gate). */
	@Nullable
	@Override
	public Component getCustomName() {
		ItemStack tStack = mStack;
		return tStack.isEmpty() ? null : tStack.getHoverName();
	}

	/**
	 * The display name the Jade 1.21.1 leg streams (ObjectNameProvider.java:156) — the
	 * custom (stack) name when present, else the block name. The 1.20.1 leg streams
	 * {@link #getCustomName()} directly, so both legs carry the same word.
	 */
	@Override
	public Component getDisplayName() {
		Component tCustom = getCustomName();
		return tCustom != null ? tCustom : getName();
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
		ItemStack tWas = mStack;
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
		if (hasLevel() && isClientSide() && !ItemStack.matches(tWas, mStack)) {
			// task surface-rock-material-link: the BE-data channel never re-bakes chunks —
			// vanilla handleBlockEntityData only loads the tag (ClientPacketListener.java
			// :1226-1238), so a material arriving AFTER the section was compiled leaves the
			// pile untinted until some unrelated rebuild. The base's paint arm re-bakes via
			// requestModelDataUpdate (the ModelData consumers); the BlockColor consumers
			// need the section-dirty face — the client sendBlockUpdated IS that face
			// (ClientLevel.java:554 -> LevelRenderer.blockChanged :2532 -> setBlockDirty).
			getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 0);
		}
	}
}
