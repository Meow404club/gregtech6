package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;
import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * The redstone torch cover — 1.20.1 port of gregapi/cover/covers/CoverRedstoneTorch
 * .java:30-45 (task p34-covers-gameplay-10; upstream rides the vanilla redstone-torch
 * items, GT_API.java:799-801). The INVERTER face of the redstone wire: the torch is ON
 * (art + the full 15 emission) exactly while the wire carries NO signal — the wire
 * powered ({@code mRedstone > 0}, the :42-44 condition) drives the torch OFF. The
 * port's wire surface is {@code GTWireBlockEntity.mRedstone} (the full-range long; any
 * non-zero value is "powered", upstream the same > 0 literal).
 *
 * <p>The gate/art/emission machinery is the {@link AbstractCoverAttachmentTorch} base
 * (the class doc carries the declared gate deviation); this class answers the
 * condition and the ON/OFF art.
 *
 * <p>FIRST-HOST DECLARATION: the wire carrier implements no cover surface yet (the
 * declared host-composition follow-up card); the offline pins drive a real carrier BE.
 */
public class CoverRedstoneTorch extends AbstractCoverAttachmentTorch {

	/** Upstream :42-44 verbatim — the wire carries a signal. */
	@Override
	public boolean condition(byte aSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		return ((GTWireBlockEntity) aData.mTileEntity).mRedstone > 0;
	}

	/** Upstream :31 — the ON (visual 0) / OFF (visual 1) plate art. */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aSide, CoverData aData) {
		return spriteOf("redstonetorch", aData.mVisuals[aSide] == 0);
	}
}
