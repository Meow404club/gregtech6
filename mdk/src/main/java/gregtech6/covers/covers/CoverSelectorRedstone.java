package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;
import gregtech6.tileentity.machines.ITileEntitySwitchableMode;
import gregtech6.util.UT6;

/**
 * The redstone selector cover — 1.20.1 port of gregapi/cover/covers/CoverSelectorRed
 * stone.java:36-79 (task p34-covers-gameplay-10; upstream item id 1007 "Redstone
 * Selector"). The host's mode dial BECOMES the redstone signal on the covered face:
 * placement (:40-43), chunk load (:45-48) and block updates (:50-52) write the face's
 * incoming redstone ({@code getRedstoneIncoming}, the ICoverableTE exit) into the dial
 * through {@code setStateMode} and fold the RETURN into the visual lane. Upstream the
 * dial then re-drives the wire output (MultiTileEntityWireRedstoneInsulated :124
 * {@code mMode * MAX_RANGE - mLoss} baseline).
 *
 * <p>FIRST-HOST DECLARATION: the port's redstone wire (GTWireBlockEntity mMode) carries
 * the matching constant-strength formula but implements no cover surface yet — the
 * live mounting rides the declared host-composition follow-up card; the offline pins
 * drive a probe host.
 *
 * <p>DECLARED CUT — the underlay texture stack folds into the single per-mode sprite
 * (the CoverSelectorTag class-doc cut).
 */
public class CoverSelectorRedstone extends AbstractCoverAttachmentSelector {

	/** The atlas sprite of the mode plates (upstream sTextures, :59-76). */
	//? if forge {
	public static ResourceLocation spriteOf(byte aMode) {
		return new ResourceLocation("gt6", "block/redstoneselector/" + UT6.bind4(aMode));
	}
	//?} else {
	/*public static ResourceLocation spriteOf(byte aMode) {
		return ResourceLocation.fromNamespaceAndPath("gt6", "block/redstoneselector/" + UT6.bind4(aMode));
	}
	 *///?}

	/** Upstream :40-43 — placement writes the face signal into the dial (blocked hosts do not mirror). */
	@Override
	public void onCoverPlaced(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer, ItemStack aCover) {
		super.onCoverPlaced(aCoverSide, aData, aPlayer, aCover);
		if (!aData.mStopped && aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable)
			aData.visual(aCoverSide, tSwitchable.setStateMode(aData.mTileEntity.getRedstoneIncoming(aCoverSide)));
	}

	/** Upstream :45-48 — a chunk load writes the face signal into the dial. */
	@Override
	public void onCoverLoaded(byte aSide, CoverData aData) {
		super.onCoverLoaded(aSide, aData);
		if (aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable)
			tSwitchable.setStateMode(aData.mTileEntity.getRedstoneIncoming(aSide));
	}

	/** Upstream :50-52 — a block update writes the face signal into the dial (blocked hosts do not mirror). */
	@Override
	public void onBlockUpdate(byte aSide, CoverData aData) {
		if (!aData.mStopped && aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable)
			aData.visual(aSide, tSwitchable.setStateMode(aData.mTileEntity.getRedstoneIncoming(aSide)));
	}

	/** Upstream :54 — the plate art follows the dial (the underlay stack is pre-composited, the class-doc cut). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aSide, CoverData aData) {
		return spriteOf((byte) aData.mVisuals[aSide]);
	}

	/** Upstream :57 — the mirrored mode survives the save (the CoverData writeToNBT :87 gate). */
	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {
		return true;
	}
}
