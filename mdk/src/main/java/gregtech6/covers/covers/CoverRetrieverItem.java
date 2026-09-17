package gregtech6.covers.covers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.block.entity.BlockEntity;

//? if forge {
import net.minecraftforge.items.IItemHandler;
//?} else {
/*import net.neoforged.neoforge.items.IItemHandler;
 *///?}

import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;
import gregtech6.tileentity.connectors.GTItemPipeBlockEntity;
import gregtech6.tileentity.connectors.TileEntityBase09Connector;
import gregtech6.util.GTItemMover;

/**
 * The item retriever cover — 1.20.1 port of gregapi/cover/covers/CoverRetrieverItem.java
 * (task p31-retriever-cover; upstream item id 1031 "Item Retriever Cover",
 * MultiItemTechnological.java:90). The plate mounts ONLY on a ticking item pipe
 * ({@code interceptCoverPlacement :50}, {@code canTick() && instanceof
 * ITileEntityItemPipe} — the port's {@code self().canUpdate() && instanceof
 * GTItemPipeBlockEntity}) and pulls items THROUGH the pipe network into the container
 * adjacent to the covered face: every 20 ticks ({@code SERVER_TIME % 20 == 15} → the
 * host timer, the port's per-BE counterpart) or immediately when the value lane is set
 * ({@code mValues != 0} — the controller-release fast path, :55-57), it scans the pipe
 * network with the capacity gate IGNORED (:69 {@code scanPipes(..., T, ...)}), walks the
 * pipes in ascending distance and for each pipe's accepting faces pulls one stack from
 * the adjacent inventory into the covered-face target ({@code ST.move(tDelegator,
 * tTarget, tFilter, F, F, mVisuals != 0, T, 64, 1, 64, 1)} :72). On the first successful
 * pull the whole scanned path prefix pays one transfer-counter unit and the pull returns
 * (:73-74) — the retriever rides the SAME capacity windows as the pipe's own flow, so a
 * spent window stops it until the next 20-tick reset (the acceptance "capacity window
 * exhaustion" arm).
 *
 * <p>The filter lives in the {@link CoverData#mNBTs} lane under the verbatim upstream
 * key {@code gt.filter.item} (:65/:129), written by the right-click set (:124-135 —
 * once; re-setting requires the soft hammer first) via the {@link CoverFilterItem#filterTagFor}
 * shared lane shape, and the screwdriver flips the visual lane normal ↔ inverted
 * (:94-97) which is the filter INVERT flag of the move (:72) and the plate art (:140).
 * The soft hammer clears the whole NBT lane (:99-101). NOT NBT sensitive — the single
 * item identity (the {@code ST.hashset(tStack)} single-item set).
 *
 * <p>DECLARED CUTS/FOLDS: the {@code magnifyingglass} readback (:103-119) has no chat
 * channel on the ported ICover signature (the CoverFilterItem precedent) → not handled;
 * the {@code UT.Entities.sendchat} feedback lines (:96/:114/:131) ride the same cut
 * channel; the {@code SFX.MC_CLICK} set-feedback maps onto the vanilla UI click (the
 * AbstractCoverDefault sound-placeholder precedent); the upstream {@code BACKGROUND_COVER}
 * attachment/holder layer stack folds into the single-sprite plate (the CoverFilterItem
 * precedent); the {@code ST.put} world-eject fallback for an inventory-less covered face
 * (ST.java:461) stays trimmed at the mover — a missing target container simply pulls
 * nothing; the per-face insert/extract intercepts (:138-139, the covered face refuses
 * foreign item traffic) are ported verbatim and reach the pipe's capability through the
 * ICoverableTE default dispatch wired at SideItemHandler.
 */
public class CoverRetrieverItem extends AbstractCoverDefault {

	/** Upstream CS.TOOL_softhammer (the filter clear, :99-101) — in-class like CoverFilterItem.TOOL_SOFTHAMMER. */
	public static final String TOOL_SOFTHAMMER = "softhammer";

	/** The verbatim upstream filter key inside the per-face {@link CoverData#mNBTs} compound (:65/:126/:129) — the CoverFilterItem shared lane. */
	public static final String FILTER_KEY = CoverFilterItem.FILTER_KEY;

	/** The atlas sprite of the normal plate — visual 0 (upstream :148, the borrow under assets/README.md). */
	public static final ResourceLocation SPRITE_NORMAL = new ResourceLocation("gt6", "block/retrieveritem/normal");

	/** The atlas sprite of the inverted plate — visual 1 (upstream :147). */
	public static final ResourceLocation SPRITE_INVERTED = new ResourceLocation("gt6", "block/retrieveritem/inverted");

	/** Upstream :61 — the periodic trigger phase within the 20-tick window. */
	public static final long TRIGGER_PHASE = 15;

	/**
	 * Upstream :50 verbatim — the cover mounts only on a ticking item pipe; everything
	 * else refuses ({@code true} = prevent). {@code canTick()} maps onto the repo root's
	 * {@code canUpdate()} (the TileEntityBase01Root :180-185 mIsTicking && mShouldRefresh
	 * counterpart).
	 */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		if (!(aData.mTileEntity instanceof GTItemPipeBlockEntity tHost)) return true;
		return !tHost.canUpdate();
	}

	/** Upstream :55-57 — a controller release arms the value lane: the next tick pulls without waiting for the :61 phase gate. */
	@Override
	public void onStoppedUpdate(byte aCoverSide, CoverData aData, boolean aStopped) {
		if (!aStopped) aData.value(aCoverSide, (short) 1);
	}

	/**
	 * Upstream :61-78 — the pull round, see the class doc. The value lane clears BEFORE
	 * the scan (:62 — a failed round does not retry until the next trigger); a missing
	 * target container or an unset filter both degrade gracefully (no gate / no pull).
	 */
	@Override
	public void onTickPre(byte aSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (!aIsServerSide || aData.mStopped) return;
		if (aTimer % 20 != TRIGGER_PHASE && aData.mValues[aSide] == 0) return; // :61 the disjunctive trigger
		if (!(aData.mTileEntity instanceof GTItemPipeBlockEntity tHost) || !tHost.pipeCapacityCheck()) return; // :61
		aData.value(aSide, (short) 0); // :62

		ItemStack tFilter = filterOf(aData, aSide); // :65-66 — null when the lane is unset/empty
		IItemHandler tTarget = tHost.adjacentInventoryOf(tHost, aSide); // :67 getAdjacentTileEntity — the covered face's container
		if (tTarget == null) return; // the :461 ST.put eject fallback stays trimmed

		List<GTItemPipeBlockEntity> tUsedPipes = new ArrayList<>();
		for (GTItemPipeBlockEntity tPipe : GTItemPipeBlockEntity.pipesAscending(
				GTItemPipeBlockEntity.scanPipes(tHost, new LinkedHashMap<>(), 0, true))) { // :69 — the capacity gate IGNORED
			if (!tUsedPipes.add(tPipe)) continue; // :70 (defensive upstream shape — the scan keys are unique)
			for (byte tScanSide = 0; tScanSide < 6; tScanSide++) { // :70 ALL_SIDES_VALID
				if (!tPipe.canAcceptItemsFrom(tScanSide, tHost)) continue; // :70
				if (tScanSide == aSide && tPipe == tHost) continue; // :70 — never pull from the host's own covered face
				IItemHandler tFrom = tHost.adjacentInventoryOf(tPipe, tScanSide); // :71 getAdjacentInventory
				if (tFrom == null) continue; // :72 the non-inventory arm
				if (GTItemMover.move(tFrom, tTarget, tFilter, aData.mVisuals[aSide] != 0) > 0) { // :72 ST.move(filter, invert, 64,1,64,1)
					for (GTItemPipeBlockEntity tUsedPipe : tUsedPipes) tUsedPipe.incrementTransferCounter(1); // :73 the path prefix pays
					return; // :74
				}
			}
		}
	}

	/**
	 * Upstream :94-101 — the screwdriver flips normal ↔ inverted (the visual lane, damage
	 * 1000) and the soft hammer clears the whole filter lane (damage 10000). The
	 * {@code magnifyingglass} readback (:103-119) has no chat channel — 0, not handled.
	 */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
			aData.visual(aCoverSide, (short) (aData.mVisuals[aCoverSide] == 0 ? 1 : 0)); // :95
			return 1000; // :97 (the :96 chat line has no channel on the ported ICover signature)
		}
		if (TOOL_SOFTHAMMER.equals(aToolId)) {
			aData.mNBTs[aCoverSide] = null; // :100 — the whole lane, unlike the FilterItem key removal
			return 10000; // :101
		}
		return 0;
	}

	/**
	 * Upstream :124-135 — the right-click-with-held-stack filter set, ONCE: an already-set
	 * lane ignores further clicks (the soft hammer resets first). Server-side players
	 * only; an EMPTY hand is ignored; always {@code true} (:135 — the click is consumed).
	 */
	@Override
	public boolean onCoverClickedRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		Player tPlayer = ICover.asPlayer(aPlayer);
		if (tPlayer != null && aData.mTileEntity.isServerSideTE()) { // :125
			if (aData.mNBTs[aCoverSide] == null || !aData.mNBTs[aCoverSide].contains(FILTER_KEY, Tag.TAG_COMPOUND)) { // :126
				ItemStack tHeld = tPlayer.getMainHandItem(); // :127 getCurrentEquippedItem
				if (!tHeld.isEmpty()) { // :128 ST.valid
					aData.mNBTs[aCoverSide] = CoverFilterItem.filterTagFor(tHeld); // :129 — the shared single-item lane shape
					if (aData.mTileEntity.self().getLevel() != null) // :130 SFX.MC_CLICK → the vanilla UI click placeholder
						aData.mTileEntity.self().getLevel().playSound(null, aData.mTileEntity.self().getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
					// :131 UT.Entities.sendchat — no chat channel (declared cut)
				}
			}
		}
		return true; // :135
	}

	/**
	 * Upstream :138 — the covered face refuses foreign inserts (the target container's
	 * traffic does not pass through the plate into the pipe). Rides the ICoverableTE
	 * item-gate dispatch (SideItemHandler consults it before the latch).
	 */
	@Override
	public boolean interceptItemInsert(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {
		return aCoverSide == aSide;
	}

	/** Upstream :139 — the covered face refuses foreign extracts, same shape. */
	@Override
	public boolean interceptItemExtract(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {
		return aCoverSide == aSide;
	}

	/** Upstream :143 — the invert marker must survive the save (the CoverData writeToNBT :75 gate). */
	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {
		return true;
	}

	/** Upstream :140 — the plate art follows the normal/inverted mode. */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return aData.mVisuals[aCoverSide] == 0 ? SPRITE_NORMAL : SPRITE_INVERTED;
	}

	// ---------------------------------------------------------------------------
	// helpers (the offline tests drive the tick hook through the host adjacency seam)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :65-66 — the filter lane as a single filter stack, {@code null} when unset
	 * or empty ({@code ST.invalid(tStack) ? null : ST.hashset(tStack)} degenerates to the
	 * item-identity gate of {@link GTItemMover#move}).
	 */
	@Nullable
	public static ItemStack filterOf(CoverData aData, byte aSide) {
		if (aData.mNBTs[aSide] == null || !aData.mNBTs[aSide].contains(FILTER_KEY, Tag.TAG_COMPOUND)) return null;
		//? if forge {
		ItemStack tStack = ItemStack.of(aData.mNBTs[aSide].getCompound(FILTER_KEY)); // upstream ST.load
		//?} else {
		/*ItemStack tStack = ItemStack.parseOptional(CoverFilterItem.nbtAccess(), aData.mNBTs[aSide].getCompound(FILTER_KEY)); // 21.1: the codec parse face
		 *///?}
		return tStack.isEmpty() ? null : tStack;
	}
}
