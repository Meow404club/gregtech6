package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;

/**
 * The item filter cover — 1.20.1 port of gregapi/cover/covers/CoverFilterItem.java:42-148
 * (task p11-cover-shutter-filter; upstream item id 1023 "Item Filter",
 * MultiItemTechnological.java:82; the research card's "FilterItem" shorthand — the
 * upstream class name is CoverFilterItem). The covered face passes item transfer only
 * when the stack satisfies the filter: visual 0 = WHITELIST (only the filter item
 * passes), visual 1 = BLACKLIST (only the filter item is refused), toggled with the
 * screwdriver (:58-62). The filter item is set by right-clicking the plate with the
 * held stack (:89-112), stored in the {@link CoverData#mNBTs} lane under the verbatim
 * upstream key {@code gt.filter.item} (:95/:100/:104), and cleared with the soft
 * hammer (:63-66). NOT NBT sensitive — the upstream match is {@code ST.equal(filter,
 * stack, T)} (:119/:126), which this port translates to vanilla item identity
 * (count- and component-insensitive).
 *
 * <p>DECLARED DEVIATION — the exact↔wildcard meta cycle: upstream a second
 * right-click with the same item toggles the stored meta to {@code W} (any meta,
 * :98-108) and a third re-pins the exact meta. The 1.20.1 item model carries no meta
 * axis, so "exact meta" and "wildcard" coincide: the cycle collapses to an idempotent
 * re-set of the same item identity, and the {@code mVisuals}-independent match has no
 * wildcard state to represent. The saved stack keeps the upstream NBT shape
 * ({@code gt.filter.item} → a vanilla {@code ItemStack.save} tag) so the lane
 * round-trips through the CoverData {@code s}-{@code x} compounds untouched.
 *
 * <p>DECLARED CUT — the pipe arms: {@code interceptConnect} (:129-133, refusing
 * pipe-pipe connections behind the filter) and {@code interceptCoverPlacement}
 * (:135-137, refusing covers between item pipes) both key on
 * {@code ITileEntityItemPipe}. This port has no item-pipe domain, so both collapse to
 * the base defaults (no intercept / no placement refusal — identical behaviour in a
 * pipeless world). The {@code magnifyingglass} readback (:67-84) and the
 * {@code UT.Entities.sendchat} feedback lines (:97/:102/:106) have no chat channel on
 * the ported ICover signature (the CoverRedstoneEmitter declared-deviation precedent);
 * the {@code SFX.MC_CLICK} feedback maps onto the vanilla UI click (the
 * AbstractCoverDefault sound-placeholder precedent).
 */
public class CoverFilterItem extends AbstractCoverDefault {

	/** Upstream CS.TOOL_softhammer (the filter clear, :63-66) — in-class like the emitter's TOOL_CUTTER (the ICover tool-id constants are a frozen surface). */
	public static final String TOOL_SOFTHAMMER = "softhammer";

	/** The verbatim upstream filter key inside the per-face {@link CoverData#mNBTs} compound (:64/:93/:95). */
	public static final String FILTER_KEY = "gt.filter.item";

	/** The atlas sprite of the whitelist plate — visual 0. */
	public static final ResourceLocation SPRITE_WHITELIST = new ResourceLocation("gt6", "block/filteritem/normal");

	/** The atlas sprite of the blacklist plate — visual 1. */
	public static final ResourceLocation SPRITE_BLACKLIST = new ResourceLocation("gt6", "block/filteritem/inverted");

	/**
	 * Upstream :95/:100/:104 — the filter-lane write for the held stack: the vanilla
	 * {@code ItemStack.save} tag of a 1-count copy under {@link #FILTER_KEY}. Pure for
	 * the offline tests (the player-narrowed click arm needs the live entity system).
	 * The upstream meta bookkeeping (:95 exact, :104 wildcard) is the declared
	 * deviation collapse — a single identity write.
	 */
	public static CompoundTag filterTagFor(ItemStack aHeld) {
		CompoundTag tLane = new CompoundTag();
		//? if forge {
		tLane.put(FILTER_KEY, new ItemStack(aHeld.getItem(), 1).save(new CompoundTag())); // upstream ST.make(item, 1, meta)
		//?} else {
		/*tLane.put(FILTER_KEY, new ItemStack(aHeld.getItem(), 1).save(nbtAccess(), new CompoundTag())); // 21.1: the save face takes the registries
		*///?}
		return tLane;
	}

	/**
	 * Upstream :118-119/:125-126 — the NBT-insensitive filter match:
	 * {@code ST.equal(filter, stack, T)} over a {@code gt.filter.item} payload becomes
	 * vanilla item identity (the meta axis is dead in 1.20.1, so no wildcard branch).
	 */
	public static boolean matches(CoverData aData, byte aCoverSide, ItemStack aStack) {
		//? if forge {
		ItemStack tFilter = ItemStack.of(aData.mNBTs[aCoverSide].getCompound(FILTER_KEY)); // upstream ST.load
		//?} else {
		/*ItemStack tFilter = ItemStack.parseOptional(nbtAccess(), aData.mNBTs[aCoverSide].getCompound(FILTER_KEY)); // 21.1: the codec parse face
		*///?}
		return !tFilter.isEmpty() && tFilter.getItem() == aStack.getItem(); // ST.equal(filter, stack, T): item yes, NBT no, count no
	}

	//? if neoforge {
	/*// 21.1: the ItemStack save/parse face needs a HolderLookup.Provider — the frozen builtin
	//registry view serves the offline tests and the live cover click alike (item id only).
	private static net.minecraft.core.HolderLookup.Provider nbtAccess() {
		return net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
	}
	*///?}

	/**
	 * Upstream :115-120 verbatim — the insert gate: another face is not gated
	 * (:116), a stopped controller refuses everything (:117), an EMPTY filter under
	 * the whitelist mode refuses everything while the blacklist mode admits everything
	 * (:118), and a set filter gates by the whitelist/blacklist polarity (:119).
	 */
	@Override
	public boolean interceptItemInsert(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {
		if (aCoverSide != aSide) return false; // :116
		if (aData.mStopped) return true; // :117
		if (aData.mNBTs[aCoverSide] == null || !aData.mNBTs[aCoverSide].contains(FILTER_KEY, Tag.TAG_COMPOUND)) return aData.mVisuals[aCoverSide] == 0; // :118
		return (aData.mVisuals[aCoverSide] == 0) != matches(aData, aCoverSide, aStack); // :119
	}

	/** Upstream :122-127 — the extract gate, same shape as the insert gate. */
	@Override
	public boolean interceptItemExtract(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {
		if (aCoverSide != aSide) return false; // :123
		if (aData.mStopped) return true; // :124
		if (aData.mNBTs[aCoverSide] == null || !aData.mNBTs[aCoverSide].contains(FILTER_KEY, Tag.TAG_COMPOUND)) return aData.mVisuals[aCoverSide] == 0; // :125
		return (aData.mVisuals[aCoverSide] == 0) != matches(aData, aCoverSide, aStack); // :126
	}

	/**
	 * Upstream :58-66 — the screwdriver flips whitelist ↔ blacklist (visual 0 ↔ 1,
	 * damage 1000) and the soft hammer clears the filter (damage 10000). The
	 * {@code magnifyingglass} readback (:67-84) has no chat channel — 0, not handled.
	 */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
			aData.visual(aCoverSide, (short) (aData.mVisuals[aCoverSide] == 0 ? 1 : 0)); // :59
			return 1000; // :61 (the :60 chat line has no channel on the ported ICover signature)
		}
		if (TOOL_SOFTHAMMER.equals(aToolId)) {
			if (aData.mNBTs[aCoverSide] != null) aData.mNBTs[aCoverSide].remove(FILTER_KEY); // :64
			return 10000; // :65
		}
		return 0; // the :67-84 magnifyingglass readback has no chat channel
	}

	/**
	 * Upstream :89-112 — the right-click-with-held-stack filter set. Server-side
	 * players only (:90); an EMPTY hand is ignored (:92); the upstream three-state
	 * cycle (:94-108) collapses to the declared-deviation idempotent identity set;
	 * the chat lines ride the cut channel. Always {@code true} (:111 — the click is
	 * consumed even when the body skipped, upstream included).
	 */
	@Override
	public boolean onCoverClickedRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		Player tPlayer = ICover.asPlayer(aPlayer); // upstream aPlayer instanceof EntityPlayer
		if (tPlayer != null && aData.mTileEntity.isServerSideTE()) { // :90
			ItemStack tHeld = tPlayer.getMainHandItem(); // upstream getCurrentEquippedItem
			if (!tHeld.isEmpty()) { // :92 ST.valid
				//? if forge {
				ItemStack tFilter = aData.mNBTs[aCoverSide] == null || !aData.mNBTs[aCoverSide].contains(FILTER_KEY, Tag.TAG_COMPOUND)
						? ItemStack.EMPTY : ItemStack.of(aData.mNBTs[aCoverSide].getCompound(FILTER_KEY)); // :93 ST.load
				//?} else {
				/*ItemStack tFilter = aData.mNBTs[aCoverSide] == null || !aData.mNBTs[aCoverSide].contains(FILTER_KEY, Tag.TAG_COMPOUND)
				? ItemStack.EMPTY : ItemStack.parseOptional(nbtAccess(), aData.mNBTs[aCoverSide].getCompound(FILTER_KEY)); // :93 ST.load (21.1: the codec parse face)
				*///?}
				if (tFilter.isEmpty() || tFilter.getItem() == tHeld.getItem()) { // :94 ST.invalid / :98 ST.equal(T) — the meta cycle collapses
					aData.mNBTs[aCoverSide] = filterTagFor(tHeld); // :95/:100/:104
					if (aData.mTileEntity.self().getLevel() != null) // :96/:101/:105 SFX.MC_CLICK → the vanilla UI click placeholder
						aData.mTileEntity.self().getLevel().playSound(null, aData.mTileEntity.self().getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
					// :97/:102/:106 UT.Entities.sendchat — no chat channel (declared deviation)
				}
			}
		}
		return true; // :111
	}

	/**
	 * Upstream :142 — the whitelist/blacklist mode survives the save (the CoverData
	 * writeToNBT :87 gate); the filter item itself rides the unconditional
	 * {@link CoverData#mNBTs} lane write (:86).
	 */
	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {
		return true;
	}

	/**
	 * Upstream :139 — the plate art follows the whitelist/blacklist mode. The upstream
	 * {@code BlockTextureMulti(BACKGROUND_COVER, plate)} stack lands as the single
	 * borrowed sprite on the single-sprite plate renderer (assets/README.md).
	 */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return aData.mVisuals[aCoverSide] == 0 ? SPRITE_WHITELIST : SPRITE_BLACKLIST;
	}
}
