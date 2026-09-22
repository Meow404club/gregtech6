package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;
import gregtech6.tileentity.machines.ITileEntitySwitchableMode;
import gregtech6.util.UT6;

/**
 * The tag selector cover — 1.20.1 port of gregapi/cover/covers/CoverSelectorTag.java
 * :36-82 (task p34-covers-gameplay-10; upstream item = the integrated circuit
 * configurable item, ItemIntegratedCircuit.java:87 registers ONE CoverSelectorTag(i)
 * per meta 0..15). The cover's constructor mode is the COMMAND: placement (:44-47),
 * chunk load (:49-52) and every server tick (:55-57) assert
 * {@code setStateMode(mMode)} on the host — the host's dial cannot drift while the
 * selector sits on it.
 *
 * <p>The 16 upstream meta-instances become 16 dedicated items ({@code cover_selector_tag_0}
 * .. {@code _15}, the p11 conveyor ladder's one-item-per-constant form) — 1.20.1 items
 * carry no meta axis, so the per-meta cover instances ride per-item registrations.
 *
 * <p>DECLARED CUT — the underlay texture stack: upstream composites
 * {@code BlockTextureMulti(sTexturesBase, sTextures[mMode])} (:59); this port's plate
 * renderer is single-sprite (the CoverShutter precedent), so the 16 shipped sprites are
 * the pre-composited underlay+digit pairs. The {@code BACKGROUND_COVER} attachment/
 * holder faces (:60-61) fold into the same sprite.
 */
public class CoverSelectorTag extends AbstractCoverAttachmentSelector {

	/** The constructor mode — the constant the cover asserts on the host (upstream :39 {@code UT.Code.bind4(aMode)}). */
	public final byte mMode;

	public CoverSelectorTag(byte aMode) {
		mMode = UT6.bind4(aMode);
	}

	/** Upstream :44-47 — placement asserts the mode (the base reset (:31-34) runs on removal). */
	@Override
	public void onCoverPlaced(byte aSide, CoverData aData, @Nullable Entity aPlayer, ItemStack aCover) {
		super.onCoverPlaced(aSide, aData, aPlayer, aCover);
		if (aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable) tSwitchable.setStateMode(mMode);
	}

	/** Upstream :49-52 — a chunk load re-asserts the mode (the host reloads at 0). */
	@Override
	public void onCoverLoaded(byte aSide, CoverData aData) {
		super.onCoverLoaded(aSide, aData);
		if (aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable) tSwitchable.setStateMode(mMode);
	}

	/** Upstream :55-57 — every server tick keeps the dial pinned to the mode. */
	@Override
	public void onTickPre(byte aSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (aIsServerSide && aData.mTileEntity instanceof ITileEntitySwitchableMode tSwitchable) tSwitchable.setStateMode(mMode);
	}

	/** Upstream :59 — the plate art carries the mode (the underlay stack is pre-composited, the class-doc cut). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aSide, CoverData aData) {
		//? if forge {
		return new ResourceLocation("gt6", "block/selectortag/" + mMode);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/selectortag/" + mMode);
		 *///?}
	}
}
