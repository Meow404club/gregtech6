package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;
import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * The redstone repeater cover — 1.20.1 port of gregapi/cover/covers/CoverRedstoneRepeat
 * er.java:30-46 (task p34-covers-gameplay-10; upstream rides the vanilla repeater item,
 * GT_API.java:802). The FOLLOWER face of the redstone wire: the repeater is ON (art +
 * the full 15 emission) exactly while the wire CARRIES a signal — the mirror of the
 * torch family's inverter (the :42-44 condition is the wire-dead arm,
 * {@code mRedstone <= 0} → OFF). The port's wire surface is
 * {@code GTWireBlockEntity.mRedstone} (the full-range long).
 *
 * <p>The gate/art/emission machinery is the {@link AbstractCoverAttachmentTorch} base
 * (the class doc carries the declared gate deviation); this class answers the
 * condition and the ON/OFF art. The FIRST-HOST DECLARATION rides the base class doc.
 */
public class CoverRedstoneRepeater extends AbstractCoverAttachmentTorch {

	/** Upstream :42-44 verbatim — the wire is DEAD (the inverter mirror). */
	@Override
	public boolean condition(byte aSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		return ((GTWireBlockEntity) aData.mTileEntity).mRedstone <= 0;
	}

	/** Upstream :31 — the ON (visual 0) / OFF (visual 1) plate art. */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aSide, CoverData aData) {
		return spriteOf("redstonerepeater", aData.mVisuals[aSide] == 0);
	}
}
