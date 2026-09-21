package gregtech6.covers.covers.logistics;

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
import gregtech6.covers.covers.AbstractCoverAttachmentLogistics;
import gregtech6.covers.covers.CoverFilterItem;

/**
 * The filtered logistics bus base — 1.20.1 port of the shared body of
 * gregapi/cover/covers/CoverLogisticsItemExport.java (:41-107) and its fluid siblings
 * (CoverLogisticsFluidExport/Import/Storage), task p33-logistics-covers-12. The 1.7.10
 * family repeats the SAME ~65-line body per cover with only the filter key
 * ({@code gt.filter.item} vs {@code gt.filter.fluid}) and the chat wording changing —
 * the port folds it into one parameterised base (the CoverRetrieverItem single-lane
 * precedent; the upstream per-class texture path stays a per-subclass sprite id).
 *
 * <p>Ported verbatim semantics:
 * <ul>
 * <li>softhammer clears the filter key from the lane (:57-60 — the KEY removal, unlike
 *     the Dump/Retriever whole-lane clear);</li>
 * <li>right-click-with-held-stack sets the filter ONCE (:88-100 — an already-set lane
 *     ignores further clicks, the soft hammer resets first; the :95 chat feedback has no
 *     channel on the ported ICover signature — the CoverRetrieverItem declared cut);</li>
 * <li>screwdriver/cutter value lanes (:59-81 of AbstractCoverAttachmentLogistics) ride
 *     {@link AbstractCoverAttachmentLogistics} in the port too — the priority bits 0-1
 *     and the target stacksize bits 2-8 of {@link CoverData#mValues}.</li>
 * </ul>
 *
 * <p>Declared cuts: the magnifyingglass readback (:61-83 — no chat channel, the
 * CoverRetrieverItem precedent); the tooltip family (:46-53) rides the LH card; the
 * attachment/holder texture stack folds into the single-sprite plate.
 */
public abstract class AbstractCoverLogisticsFiltered extends AbstractCoverAttachmentLogistics {

	/** The tool id of the filter clear (upstream TOOL_softhammer, :57). */
	public static final String TOOL_SOFTHAMMER = "softhammer";

	/** The verbatim upstream filter key for this family branch. */
	public final String filterKey;

	protected AbstractCoverLogisticsFiltered(String aFilterKey) {
		filterKey = aFilterKey;
	}

	/** The upstream filter key of the ITEM branch. */
	public static final String FILTER_KEY_ITEM = "gt.filter.item";
	/** The upstream filter key of the FLUID branch (upstream FL.save "gt.filter.fluid"). */
	public static final String FILTER_KEY_FLUID = "gt.filter.fluid";

	/**
	 * The lane write for a held stack — the ITEM branch stores the 1-count identity tag
	 * (the {@link CoverFilterItem#filterTagKeyOf} shared shape); the FLUID subclass
	 * overrides to the string-lane form. {@code null} = the held stack carries no
	 * filterable content — the click does NOT write the lane (upstream :98 ST.valid arm).
	 */
	@Nullable
	public CompoundTag filterLaneFor(ItemStack aHeld) {
		return CoverFilterItem.filterTagKeyOf(aHeld, filterKey);
	}

	/** Upstream :57-60 — the softhammer removes the filter KEY from the lane. */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (TOOL_SOFTHAMMER.equals(aToolId)) {
			if (aData.mNBTs[aCoverSide] != null) aData.mNBTs[aCoverSide].remove(filterKey);
			return 10000; // :59/:73 the fluid branch shares the damage
		}
		return super.onToolClick(aCoverSide, aData, aToolId, aRemainingDurability, aPlayer, aSneaking, aSideClicked, aHitX, aHitY, aHitZ);
	}

	/**
	 * Upstream :88-100 (item form) / the fluid branch — the right-click filter set, ONCE:
	 * an already-set lane ignores further clicks. Server-side players only; an EMPTY hand
	 * is ignored; always {@code true} (:99/:118 — the click is consumed).
	 */
	@Override
	public boolean onCoverClickedRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		Player tPlayer = ICover.asPlayer(aPlayer);
		if (tPlayer != null && aData.mTileEntity.isServerSideTE()) { // :89/:105
			if (aData.mNBTs[aCoverSide] == null || !aData.mNBTs[aCoverSide].contains(filterKey, Tag.TAG_COMPOUND)) { // :90
				ItemStack tHeld = tPlayer.getMainHandItem(); // :91 getCurrentEquippedItem
				if (!tHeld.isEmpty()) { // :92 ST.valid
					CompoundTag tLane = filterLaneFor(tHeld); // :93/:113 — null when the held stack has no filterable content
					if (tLane != null) {
						aData.mNBTs[aCoverSide] = tLane;
						if (aData.mTileEntity.self().getLevel() != null) // :94 SFX.MC_CLICK → the vanilla UI click placeholder
							aData.mTileEntity.self().getLevel().playSound(null, aData.mTileEntity.self().getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
						// :95/:116 UT.Entities.sendchat — no chat channel (declared cut)
					}
				}
			}
		}
		return true; // :99/:118
	}

	/**
	 * The stored filter of the lane — the item form. {@code null} when unset (the empty
	 * parse arm is the ST.invalid degenerate).
	 */
	@Nullable
	public ItemStack filterItemOf(CoverData aData, byte aSide) {
		if (aData.mNBTs[aSide] == null || !aData.mNBTs[aSide].contains(filterKey, Tag.TAG_COMPOUND)) return null;
		//? if forge {
		ItemStack tStack = ItemStack.of(aData.mNBTs[aSide].getCompound(filterKey)); // upstream ST.load
		//?} else {
		/*ItemStack tStack = ItemStack.parseOptional(CoverFilterItem.nbtAccess(), aData.mNBTs[aSide].getCompound(filterKey)); // 21.1: the codec parse face
		 *///?}
		return tStack.isEmpty() ? null : tStack;
	}

	/** The target stacksize of the value lane — the bits 2-8 of the AbstractCoverAttachmentLogistics cutter lane. */
	public static int targetStackSize(CoverData aData, byte aSide) {
		return (aData.mValues[aSide] >> 2) & 127;
	}

	/** The priority tier of the value lane — the low 2 bits (0 unmodified / 1 generic / 2 semi / 3 filtered). */
	public static int priorityOf(CoverData aData, byte aSide) {
		return aData.mValues[aSide] & 3;
	}

	/** The per-class plate sprite (upstream :106 {@code machines/covers/logistics/<family>/<role>}). */
	@Override
	public abstract ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData);

	@Override
	public boolean useTargetStackSize() {
		return true; // upstream :104 (the filtered bus family)
	}
}
