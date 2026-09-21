package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;

import gregapi.tileentity.logistics.ITileEntityLogistics;
import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;

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

	/** Upstream CS.TOOL_cutter (:71) — the stacksize-lane tool id. */
	public static final String TOOL_CUTTER = "cutter";

	/**
	 * Upstream :59-81 — the two value lanes of the family (task p33-logistics-covers-12;
	 * the p32 skeleton declared them riding this card): the screwdriver cycles the
	 * PRIORITY bits 0-1 (damage 10000) and the cutter cycles the TARGET STACKSIZE bits
	 * 2-8 (damage 1000), gated by {@link #usePriorities()}/{@link #useTargetStackSize()}.
	 * The magnifyingglass readback (:83-101) has no chat channel on the ported ICover
	 * signature — the CoverRetrieverItem declared cut.
	 */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId) && usePriorities()) { // :59
			short tValue = aData.mValues[aCoverSide];
			aData.value(aCoverSide, (short) ((tValue & ~3) | ((tValue + 1) & 3))); // :60
			return 10000; // :69 (the :62-68 chat switch has no channel)
		}
		if (TOOL_CUTTER.equals(aToolId) && useTargetStackSize()) { // :71
			short tValue = aData.mValues[aCoverSide];
			aData.value(aCoverSide, (short) ((tValue & 3) | (((((tValue >> 2) + 1) % 65) << 2)))); // :72
			return 1000; // :81
		}
		return super.onToolClick(aCoverSide, aData, aToolId, aRemainingDurability, aPlayer, aSneaking, aSideClicked, aHitX, aHitY, aHitZ);
	}

	/** Upstream :110 — the priority lane answers by default. */
	public boolean usePriorities() {return true;}

	/** Upstream :109 — the stacksize lane defaults off (the filtered Export family flips it). */
	public boolean useTargetStackSize() {return false;}
}
