package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;
import gregtech6.tileentity.machines.ITileEntitySwitchableOnOff;

/**
 * The controller base — 1.20.1 port of gregapi/cover/covers/AbstractCoverAttachmentController.java
 * (:32-62, task p35-covers-display-scale-6). A controller cover OWNS a switchable
 * machine's ON/OFF state: it refuses non-switchable hosts (:33), releases the machine to
 * ON when dismantled (:36-39), re-derives the state from its own answer on mount/load
 * (:41-49), re-polls on a block update (:52-54) and polls every server tick (:57-59).
 * The per-cover half is the single abstract {@link #getStateOnOff} — the state formula.
 *
 * <p>The P10/P11 controller covers (CoverControllerRedstone/CoverControllerAutoRedstone)
 * carry this exact five-arm shape as their VERIFIED INLINED copies — the P11 "second
 * inline" ruling kept the base unported with one consumer; this card's three controller
 * consumers land the base instead.
 */
public abstract class AbstractCoverAttachmentController extends AbstractCoverAttachment {

	/** Upstream :33 — a controller only mounts on a switchable host. */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return !(aData.mTileEntity instanceof ITileEntitySwitchableOnOff);
	}

	/** Upstream :36-39 — the dismantled controller releases the machine to ON. */
	@Override
	public void onCoverRemove(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		super.onCoverRemove(aCoverSide, aData, aPlayer);
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && aData.mTileEntity.self().getLevel() != null) {
			tTE.setStateOnOff(true);
		}
	}

	/** Upstream :41-44 — the load arm re-derives the machine state from the answer. */
	@Override
	public void onCoverLoaded(byte aCoverSide, CoverData aData) {
		super.onCoverLoaded(aCoverSide, aData);
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && aData.mTileEntity.self().getLevel() != null) {
			tTE.setStateOnOff(getStateOnOff(aCoverSide, aData));
		}
	}

	/** Upstream :46-49 — the mount arm, same shape as the load arm. */
	@Override
	public void onCoverPlaced(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer, ItemStack aCover) {
		super.onCoverPlaced(aCoverSide, aData, aPlayer, aCover);
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && aData.mTileEntity.self().getLevel() != null) {
			tTE.setStateOnOff(getStateOnOff(aCoverSide, aData));
		}
	}

	/** Upstream :52-54 — the block-update drive (the GTOvenBlock.neighborChanged seam). */
	@Override
	public void onBlockUpdate(byte aCoverSide, CoverData aData) {
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && aData.mTileEntity.self().getLevel() != null) {
			tTE.setStateOnOff(getStateOnOff(aCoverSide, aData));
		}
	}

	/** Upstream :57-59 — the server-side tick poll. */
	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (aIsServerSide && aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE) {
			tTE.setStateOnOff(getStateOnOff(aCoverSide, aData));
		}
	}

	/** The per-cover state formula — the machine state the controller holds. */
	public abstract boolean getStateOnOff(byte aCoverSide, CoverData aData);
}
