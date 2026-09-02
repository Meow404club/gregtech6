package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.IItemHandler;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;
import gregtech6.covers.ICoverableTE;
import gregtech6.util.GTItemMover;

/**
 * The robot arm cover — 1.20.1 port of gregapi/cover/covers/CoverRobotArm.java:41-121
 * (task p11-cover-conveyor-robotarm). The conveyor's skeleton with a slot address: the
 * value lane ({@code mValues[aSide]}) addresses one fixed slot on one end of the transfer
 * — negative means TAKE from slot {@code -1 - mValues} (:85-90, {@code ST.moveFrom} with
 * {@code aIgnoreSideFrom = T}), non-negative means PUT into slot {@code mValues} (:91-97,
 * {@code ST.moveTo} with {@code aIgnoreSideTo = T}) — while the visual lane picks the
 * direction exactly like the conveyor (0 = OUT, 1 = IN) and the timing tiers are the same
 * 512&gt;&gt;i periods (MultiItemTechnological.java:53).
 *
 * <p>The fixed-slot half ignores the side rules (upstream SIDE_ANY, ST.java:523/:550) —
 * the 1.20.1 counterpart is the SIDE-LESS capability view (the raw handler, the
 * GTItemMover javadoc's "caller's view choice" note); the free-scan half keeps the normal
 * delegator-side view. The transfer routes are the {@link GTItemMover#moveFrom}/
 * {@link GTItemMover#moveTo} one-stack entries at the cover defaults (64/1/64/1,
 * CoverRobotArm.java:87-95 shape).
 *
 * <p>Upstream trims (declared): the {@code MultiTileEntityPipeItem} cases (:52-55,
 * :62-66) ride the item-pipe subsystem this repo does not ship — cut; the magnifying-glass
 * readback (:75-78) and the screwdriver chat return (:72) ride the cut tool-system/chat
 * surface (the emitter-precedent deviation — the value lane is observable through the
 * covers NBT); the {@code UT.Code.bind} of the screwdriver step (:71) keeps the short
 * range with the pipe-only upper bound (-1) cut to {@code Short.MAX_VALUE}.
 */
public class CoverRobotArm extends AbstractCoverDefault {

	/** Upstream CS.TOOL_monkeywrench — the arm's direction-lane tool id (the CS constant rides the cut tool system; the CoverRedstoneEmitter.TOOL_CUTTER precedent). */
	public static final String TOOL_MONKEYWRENCH = "monkeywrench";

	/** The atlas sprite of the out-facing plate (visual 0). */
	public static final ResourceLocation ROBOT_ARM_OUT_SPRITE = new ResourceLocation("gt6", "block/robotarm/out");

	/** The atlas sprite of the in-facing plate (visual 1). */
	public static final ResourceLocation ROBOT_ARM_IN_SPRITE = new ResourceLocation("gt6", "block/robotarm/in");

	/** Upstream :44-48 — the constructor argument is the tick period, floored at 1 (the conveyor's 512&gt;&gt;i table, MultiItemTechnological.java:53). */
	public final int mTiming;

	public CoverRobotArm(int aTiming) {
		mTiming = Math.max(1, aTiming);
	}

	/** Upstream :84 beat — {@code SERVER_TIME % mTiming == 0}; the same period semantics as the conveyor. */
	public static boolean isBeat(int aTiming, long aTimer) {
		return aTimer % Math.max(1, aTiming) == 0;
	}

	/**
	 * Upstream :70-74 — the screwdriver steps the slot address by ±1 (sneaking inverts),
	 * clamped to the short range (the :71 {@code UT.Code.bind(Short.MIN_VALUE, ..., +1)}).
	 * Exposed for the offline truth table.
	 */
	public static short stepSlot(short aCurrent, boolean aSneaking) {
		int tStepped = aCurrent + (aSneaking ? -1 : +1);
		return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, tStepped));
	}

	/** Upstream :42 — same gate as the conveyor: the host must expose the item surface. */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return CoverConveyor.hostHandler(aData.mTileEntity, aCoverSide, false) == null;
	}

	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (!aIsServerSide || aData.mStopped || !isBeat(mTiming, aTimer)) return; // :84
		ICoverableTE tHost = aData.mTileEntity;
		// the delegator convention (CoverConveyor class javadoc): normal views keyed on the
		// covered face / OPOS, side-less views for the fixed-slot half (upstream SIDE_ANY)
		SideViews tHostViews = new SideViews(
				CoverConveyor.hostHandler(tHost, aCoverSide, false),
				CoverConveyor.hostHandler(tHost, aCoverSide, true));
		SideViews tNeighbourViews = new SideViews(
				CoverConveyor.neighbourHandler(tHost, aCoverSide, false),
				CoverConveyor.neighbourHandler(tHost, aCoverSide, true));
		armTransfer(aData.mValues[aCoverSide], aData.mVisuals[aCoverSide], tHostViews, tNeighbourViews);
	}

	/**
	 * The per-end view pair — the normal delegator-side view for the free-scan half and
	 * the side-less raw view for the fixed-slot half (the upstream ignoreSide flags).
	 * Either member may be null (no level / no neighbour / no handler); the transfer
	 * degenerates to zero movement.
	 */
	public record SideViews(@Nullable IItemHandler normal, @Nullable IItemHandler sideLess) {
	}

	/**
	 * The :85-97 four-quadrant table over the two ends — the offline seam: the tests feed
	 * handler doubles and assert the fixed-slot semantics (which end is fixed, which slot,
	 * which end scans).
	 */
	public static void armTransfer(short aValue, short aVisual, SideViews aHost, SideViews aNeighbour) {
		if (aValue < 0) {
			int tSlot = -1 - aValue; // the :87/:89 slot encoding
			if (aVisual == 0) {
				GTItemMover.moveFrom(aHost.sideLess(), aNeighbour.normal(), tSlot); // :87 — take from the HOST slot
			} else {
				GTItemMover.moveFrom(aNeighbour.sideLess(), aHost.normal(), tSlot); // :89 — take from the NEIGHBOUR slot
			}
		} else {
			if (aVisual == 0) {
				GTItemMover.moveTo(aHost.normal(), aNeighbour.sideLess(), aValue); // :93 — put into the NEIGHBOUR slot
			} else {
				GTItemMover.moveTo(aNeighbour.normal(), aHost.sideLess(), aValue); // :95 — put into the HOST slot
			}
		}
	}

	/**
	 * Upstream :60-74 with the pipe branch cut: the monkeywrench flips the direction lane
	 * (:67-68, 1000), the screwdriver steps the slot address (:71-73, 200).
	 */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (TOOL_MONKEYWRENCH.equals(aToolId)) {
			aData.visual(aCoverSide, (short)(aData.mVisuals[aCoverSide] == 0 ? 1 : 0));
			return 1000;
		}
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
			aData.value(aCoverSide, stepSlot(aData.mValues[aCoverSide], aSneaking));
			return 200;
		}
		return 0;
	}

	/** Upstream :115 — the conveyor's insert gate, verbatim (the out-face refuses incoming items). */
	@Override
	public boolean interceptItemInsert(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {
		return aCoverSide == aSide && aData.mVisuals[aSide] == 0;
	}

	/** Upstream :116 — the conveyor's extract gate, verbatim (the in-face refuses outgoing items). */
	@Override
	public boolean interceptItemExtract(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {
		return aCoverSide == aSide && aData.mVisuals[aSide] != 0;
	}

	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {return true;} // :113 — the direction survives the save

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return aData.mVisuals[aCoverSide] == 0 ? ROBOT_ARM_OUT_SPRITE : ROBOT_ARM_IN_SPRITE; // :110
	}
}
