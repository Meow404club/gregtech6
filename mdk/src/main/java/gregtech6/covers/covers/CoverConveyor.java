package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;
import gregtech6.covers.ICoverableTE;
import gregtech6.util.GTItemMover;

/**
 * The conveyor cover — 1.20.1 port of gregapi/cover/covers/CoverConveyor.java:40-95
 * (task p11-cover-conveyor-robotarm). Every {@link #mTiming} ticks
 * ({@code SERVER_TIME % mTiming == 0}, :66 — the timing tiers are PERIODS, verified at
 * the source: MultiItemTechnological.java:51 passes {@code 512>>i} into the constructor
 * whose {@code mTiming} only feeds the modulo at CoverConveyor.java:66, so tier 0 moves
 * one stack per 512 ticks and tier 9 one stack per tick) it moves one stack between the
 * host inventory and the covered face's neighbour, in the direction the visual lane
 * encodes: 0 = OUT (host gives, :68), 1 = IN (host takes, :70), toggled with the
 * screwdriver (:57-59).
 *
 * <p>The one-way gate rides the restored ICover item intercepts (:89-90, verbatim): the
 * out-face refuses incoming items, the in-face refuses outgoing items — the covered face
 * only ever carries items in the conveyor's direction, hoppers included.
 *
 * <p>The transfer itself is {@link GTItemMover#move} — the {@code ST.move} subset the
 * W1 helper card re-based onto the IItemHandler family (CoverConveyor.java:68/:70 two-arg
 * shape, defaults 64/1/64/1). The side views follow the delegator convention the mover's
 * javadoc pins: the host view is the capability at the covered face (the upstream
 * {@code delegator(aSide)}), the neighbour view is the capability at the face pointing
 * back at the host ({@code getAdjacentTileEntity} wraps the neighbour with
 * {@code OPOS[aSide]}, TileEntityBase01Root.java:224) — so a covered host answers through
 * its own cover gates and the neighbour's side rules apply as upstream's
 * {@code getAccessibleSlotsFromSide(delegator side)} did.
 *
 * <p>Upstream trims (declared): the {@code MultiTileEntityPipeItem} placement/visual
 * special cases (:51/:58) ride the item-pipe subsystem this repo does not ship — a pipe
 * host cannot exist, the branches are unreachable and are cut; the
 * {@code canTick() && instanceof IInventory} placement gate (:41) maps onto the item
 * handler half (the item surface IS the 1.20.1 inventory, and every ICoverableTE host in
 * this port ticks — the dead-host half of the upstream gate has no remaining referent);
 * the addToolTips channel (:76-81) rides the cut chat surface.
 */
public class CoverConveyor extends AbstractCoverDefault {

	/** The 10 upstream timing tiers, 512&gt;&gt;i — PERIODS in ticks (MultiItemTechnological.java:51). */
	public static final int[] TIMING_TIERS = {512, 256, 128, 64, 32, 16, 8, 4, 2, 1};

	/** The atlas sprite of the out-facing plate (visual 0). */
	public static final ResourceLocation CONVEYOR_OUT_SPRITE = new ResourceLocation("gt6", "block/conveyor/out");

	/** The atlas sprite of the in-facing plate (visual 1). */
	public static final ResourceLocation CONVEYOR_IN_SPRITE = new ResourceLocation("gt6", "block/conveyor/in");

	/** Upstream :43-47 — the constructor argument is the tick period, floored at 1. */
	public final int mTiming;

	public CoverConveyor(int aTiming) {
		mTiming = Math.max(1, aTiming);
	}

	/** Upstream :66 beat — {@code SERVER_TIME % mTiming == 0}; exposed for the offline tier table. */
	public static boolean isBeat(int aTiming, long aTimer) {
		return aTimer % Math.max(1, aTiming) == 0;
	}

	/**
	 * Upstream :41 — the conveyor only mounts where it has an inventory to move from and
	 * to: the port maps {@code canTick() && instanceof IInventory} onto the host's
	 * item-handler surface (the class javadoc trims note).
	 */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return hostHandler(aData.mTileEntity, aCoverSide, false) == null;
	}

	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (!aIsServerSide || aData.mStopped || !isBeat(mTiming, aTimer)) return; // :66
		transfer(aData.mTileEntity, aCoverSide, aData.mVisuals[aCoverSide]);
	}

	/**
	 * The live :65-73 body — queries the two side views and routes the direction split.
	 * A view-less situation (no level / no neighbour / no item surface) degenerates to
	 * zero movement.
	 */
	public static void transfer(ICoverableTE aHost, byte aSide, short aVisual) {
		moveByDirection(aVisual,
				hostHandler(aHost, aSide, false), // delegator(aSide)
				neighbourHandler(aHost, aSide, false)); // adjacent(aSide) @ OPOS
	}

	/**
	 * The :67-71 direction split over the two side views — the offline seam: the tests
	 * feed handler doubles and assert the direction ordering (the live capability queries
	 * ride the class javadoc delegator convention).
	 */
	public static void moveByDirection(short aVisual, @Nullable IItemHandler aHostView, @Nullable IItemHandler aNeighbourView) {
		if (aHostView == null || aNeighbourView == null) return; // the :66 IInventory half / no neighbour = move 0
		if (aVisual == 0) {
			GTItemMover.move(aHostView, aNeighbourView); // :68 — out
		} else {
			GTItemMover.move(aNeighbourView, aHostView); // :70 — in
		}
	}

	/**
	 * The host's side view — upstream {@code aData.delegator(aSide)} (the delegator folds
	 * into the host reference, CoverData class note). {@code aSideLess} hands the raw
	 * un-gated handler (the upstream SIDE_ANY ignoreSide form, ST.java:467/:470) — the
	 * conveyor never uses it; the robot arm's fixed-slot half does.
	 */
	public static @Nullable IItemHandler hostHandler(ICoverableTE aHost, byte aSide, boolean aSideLess) {
		BlockEntity tBE = aHost.self();
		if (tBE.getLevel() == null) return null; // the offline guard (the CoverPump.adjacentHandler precedent)
		return tBE.getCapability(ForgeCapabilities.ITEM_HANDLER, aSideLess ? null : Direction.from3DDataValue(aSide)).orElse(null);
	}

	/**
	 * The covered face's neighbour view — upstream {@code getAdjacentTileEntity(aSide)},
	 * whose delegator side is OPOS[aSide] (TileEntityBase01Root.java:224), i.e. the
	 * neighbour's face pointing back at the host. {@code aSideLess} is the SIDE_ANY form
	 * again (the arm's fixed-slot half).
	 */
	public static @Nullable IItemHandler neighbourHandler(ICoverableTE aHost, byte aCoverSide, boolean aSideLess) {
		BlockEntity tBE = aHost.self();
		Level tLevel = tBE.getLevel();
		if (tLevel == null) return null;
		Direction tDir = Direction.from3DDataValue(aCoverSide);
		BlockEntity tNeighbour = tLevel.getBlockEntity(tBE.getBlockPos().relative(tDir));
		if (tNeighbour == null) return null;
		return tNeighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, aSideLess ? null : tDir.getOpposite()).orElse(null);
	}

	/** Upstream :57-59 — the screwdriver flips the visual lane 0 ↔ 1 (the pipe special case is cut with its subsystem); 1000 = the tool damage. */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
			aData.visual(aCoverSide, (short)(aData.mVisuals[aCoverSide] == 0 ? 1 : 0));
			return 1000;
		}
		return 0;
	}

	/** Upstream :89 — the out-face (visual 0) refuses incoming items on its own face. */
	@Override
	public boolean interceptItemInsert(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {
		return aCoverSide == aSide && aData.mVisuals[aSide] == 0;
	}

	/** Upstream :90 — the in-face (visual 1) refuses outgoing items on its own face. */
	@Override
	public boolean interceptItemExtract(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {
		return aCoverSide == aSide && aData.mVisuals[aSide] != 0;
	}

	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {return true;} // :87 — the direction survives the save

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return aData.mVisuals[aCoverSide] == 0 ? CONVEYOR_OUT_SPRITE : CONVEYOR_IN_SPRITE; // :83
	}
}
