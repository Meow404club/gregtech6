package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;

import gregapi.tileentity.logistics.ITileEntityLogistics;
import gregtech6.covers.CoverData;

/**
 * The logistics attachment base — 1.20.1 counterpart of gregapi/cover/covers/
 * AbstractCoverAttachmentLogistics.java (113 lines), task p32-logistics-lv2 SKELETON
 * scope: the class exists for its PLACEMENT GATE only (upstream :39-40) — the host must
 * be an {@link ITileEntityLogistics} member whose SIDE_ANY family query answers
 * ({@code canLogistics(SIDE_ANY)}, the CS.java:698 SIDES_INVALID[6]=T query the wire
 * family answers unconditionally). The gate is the single hard seam the later 12-cover
 * family (Display CPU 1086-1089, Filtered Fluid/Item E/I/S 1090-1095, Generic E/I/S
 * 1096-1098, Dump 1099 — MultiItemTechnological.java:101-114) mounts on.
 *
 * <p>Declared trims against the upstream body (all ride the cards that land the
 * consumers): the {@code interceptConnect} TRUE (:41) and the onCoverPlaced
 * connector-disconnect (:43-47) — the port's ICover carries no connector-hook group
 * (ICover.java: the ":75-80/:183 connector hooks" are a declared pool cut) and no
 * connector-consuming cover exists yet; the screwdriver/cutter priority-and-stacksize
 * value lanes (:59-81, mValues low 2 bits + high 7 bits) and the magnetinglass readback
 * (:83-100) ride the 12-cover card; the tooltip lines (:49-55, LH) ride the LH
 * tooltip-surface card; the attachment/holder texture stack (:105-112) folds into the
 * single-sprite plate the port's covers render (the CoverRetrieverItem precedent).
 */
public abstract class AbstractCoverAttachmentLogistics extends AbstractCoverDefault {

	/** Upstream CS.SIDE_ANY = 6 — the side-less family query index of the placement gate. */
	public static final byte SIDE_ANY = 6;

	/**
	 * Upstream :40 — the pure decision half of the placement gate: {@code true} REFUSES.
	 * A host qualifies iff it is an {@link ITileEntityLogistics} member whose SIDE_ANY
	 * query answers. Factored out of {@link #interceptCoverPlacement} so the command
	 * surface (/gt6logistics gate) and the offline tests drive the exact predicate the
	 * cover dispatch runs — the host is typed only by the instanceof, exactly upstream.
	 */
	public static boolean refusesAttachment(@Nullable Object aHost) {
		return !(aHost instanceof ITileEntityLogistics tNode && tNode.canLogistics(SIDE_ANY));
	}

	/** Upstream :40 verbatim — the gate routes the cover dispatch through the pure decision. */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return refusesAttachment(aData.mTileEntity);
	}
}
