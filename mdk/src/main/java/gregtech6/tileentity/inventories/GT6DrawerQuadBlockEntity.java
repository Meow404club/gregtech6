package gregtech6.tileentity.inventories;

import java.util.Arrays;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GTBlockEntities;

/**
 * The GT6 Compartment Drawer (Quad) — 1.20.1 counterpart of
 * {@code gregtech/tileentity/inventories/MultiTileEntityDrawerQuad.java:54-146}, the
 * 144-slot four-quadrant drawer of the metalset row :140 ("Compartment Drawer (Mat)",
 * id 4000+aID).
 *
 * <h2>The quadrant index math (the card's "SLOTS 四象限索引数学逐字")</h2>
 * <p>144 slots = 4 quadrants of 36, quadrant q occupying {@code q*36 .. q*36+35}. The
 * upstream face-quadrant pick :88 {@code (x > 0.5 ? 1 : 0) | (y > 0.5 ? 2 : 0)} and the
 * GUI window {@code (aGUIID % 4) * 36, 36} (:103-104) survive verbatim as
 * {@link #quadrantOfFace(double, double)} and {@link #quadrantBase}. The sided half tables
 * (:106-114 SLOTS composition) keep the literal block composition —
 * {@link #TOP_HALF}/{@link #BOTTOM_HALF} ({0..71}/{72..143}), {@link #LEFT_HALF}/
 * {@link #RIGHT_HALF} (the two 72-entry columns {0..35,72..107}/{36..71,108..143}) — the
 * same arrays the 1.7.10 table spelled out entry by entry.
 *
 * <h2>The sided access (declared modernization)</h2>
 * <p>Upstream :118 indexes {@code SLOTS[FACING_ROTATIONS[mFacing][aSide]]} — a 1.7.10
 * byte-table (CS.java:528) with no 1:1 modern counterpart. The port keeps its GEOMETRY,
 * re-derived onto {@link Direction} arithmetic and pinned by the offline quadrant test:
 * <ul>
 * <li>{@code mSidedAccess == false} (default, "Anywhere") — every side sees all 144
 *     (the SLOTS all-entry);</li>
 * <li>sided mode ("Sided", the monkey-wrench toggle :94-98) — the FRONT and BACK faces
 *     keep the full 144 (the GUI face and its opposite ride the all-entry), while each of
 *     the four lateral sides sees the physical half/column it stands beside: the side
 *     above the TOP half {q0,q1}, below the BOTTOM half {q2,q3}, viewer-left the LEFT
 *     column {q0,q2}, viewer-right the RIGHT column {q1,q3}.</li>
 * </ul>
 *
 * <h2>GUI ruling (the wave-4 declaration)</h2>
 * <p>The upstream click-a-quadrant-open-36 GUI (:88/:102-104) was a 1.7.10 GUI-size
 * compromise; the port opens ONE MUI panel with all 144 slots (the card's "MUI 一页 144
 * 槽四象限一页" deviation declaration, zero MenuType). {@link #quadrantOfFace} stays and is
 * tested — it is the loader's own index math, the seam a future quadrant-zoom page would
 * consume.
 *
 * <p>No tool arms beyond the monkey-wrench toggle (upstream :92-100 returns 10000
 * durability for it). DECLARED DEVIATION: the toggle is reachable from the FRONT face
 * only — it rides the block use arm behind the front-face gate, while the upstream
 * {@code onToolClick2} answered a monkey wrench on ANY face; the sided state itself is
 * identical and the RCON chain drives it through the mode NBT.
 * Insert/extract gates :120-121 = always true; stack limit 64.
 */
public class GT6DrawerQuadBlockEntity extends GT6StaticStorageBaseBlockEntity implements gregtech6.gui.machines.GT6MuiMachine {

	/** The slot count (upstream :117 getDefaultInventory = ItemStack[144]). */
	public static final int INVENTORY_SIZE = 144;

	/** One quadrant (upstream :103-104 — the 36-slot GUI window). */
	public static final int QUADRANT = 36;

	/** The quadrant base offset (upstream {@code (aGUIID % 4) * 36}). */
	public static int quadrantBase(int aQuadrant) {
		return aQuadrant * QUADRANT;
	}

	/**
	 * The face-quadrant pick (upstream :88 verbatim over the front-local coordinates):
	 * {@code (x > 0.5 ? 1 : 0) | (y > 0.5 ? 2 : 0)} — x/y in [0,1] front-local, x running
	 * viewer-left→right, y bottom→top (the hit-y IS the world-y fraction, the vertical is
	 * face-independent).
	 */
	public static int quadrantOfFace(double aLocalX, double aLocalY) {
		return (aLocalX > 0.5 ? 1 : 0) | (aLocalY > 0.5 ? 2 : 0);
	}

	/** The TOP half of the drawers — quadrants {0,1} (upstream SLOTS[1] row, 0..71). */
	public static final int[] TOP_HALF = ascending(0, QUADRANT * 2);

	/** The BOTTOM half — quadrants {2,3} (upstream SLOTS[0] row, 72..143). */
	public static final int[] BOTTOM_HALF = ascending(QUADRANT * 2, QUADRANT * 4);

	/** The LEFT column — quadrants {0,2} (upstream SLOTS[2] row: 0..35 + 72..107). */
	public static final int[] LEFT_HALF = concat(ascending(0, QUADRANT), ascending(QUADRANT * 2, QUADRANT * 3));

	/** The RIGHT column — quadrants {1,3} (upstream SLOTS[4] row: 36..71 + 108..143). */
	public static final int[] RIGHT_HALF = concat(ascending(QUADRANT, QUADRANT * 2), ascending(QUADRANT * 3, QUADRANT * 4));

	/** ALL — the ascending 144 (upstream the getAscendingArray(144) rows). */
	public static final int[] ALL = ascending(0, INVENTORY_SIZE);

	private static int[] ascending(int aFromInclusive, int aToExclusive) {
		int[] rArray = new int[aToExclusive - aFromInclusive];
		for (int i = 0; i < rArray.length; i++) rArray[i] = aFromInclusive + i;
		return rArray;
	}

	private static int[] concat(int[] aA, int[] aB) {
		int[] rArray = Arrays.copyOf(aA, aA.length + aB.length);
		System.arraycopy(aB, 0, rArray, aA.length, aB.length);
		return rArray;
	}

	/** NBT key of {@link #mSidedAccess} (upstream NBT_MODE, :60/:66). */
	public static final String NBT_MODE = "mode";

	/** Upstream :55 — false = "Anywhere" (every side sees all), true = "Sided". */
	public boolean mSidedAccess = false;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type at runtime. */
	public GT6DrawerQuadBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.DRAWER_QUAD_BE.get(), aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6DrawerQuadBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "drawer_quad"; // upstream :145 "gt.multitileentity.drawer.quad" — the BET path mirrors it
	}

	@Override
	protected int inventorySize(BlockState aState) {
		return INVENTORY_SIZE;
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_MODE)) {
			mSidedAccess = aNBT.getBoolean(NBT_MODE);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_MODE, mSidedAccess); // upstream :66 verbatim (always written)
	}

	/** Upstream :94-98 — the monkey-wrench toggle; the caller reports the chat line. */
	public void monkeyWrench() {
		mSidedAccess = !mSidedAccess;
		setChanged();
	}

	/** The upstream :96 feedback line, verbatim. */
	public String accessChatLine() {
		return "Automation-Access: " + (mSidedAccess ? "Sided" : "Anywhere");
	}

	/**
	 * The viewer-local left/right Directions of a horizontal facing (the geometric seam of
	 * the class doc): left = the side on the VIEWER'S left when standing AT the front face,
	 * looking at it (facing NORTH the viewer looks south, so their left is world EAST; the
	 * upstream decode FACING_ROTATIONS[2] CS.java:531 maps side east->2 = left). The
	 * compass arms anchor on the upstream rows: [3] (south, CS.java:532) world west->left,
	 * [4] (west, CS.java:533) world north->left, [5] (east, CS.java:534) world south->left.
	 */
	public static Direction viewerLeftOf(Direction aFacing) {
		return switch (aFacing) {
			case NORTH -> Direction.EAST;
			case SOUTH -> Direction.WEST;
			case EAST -> Direction.SOUTH; // FACING_ROTATIONS[5]: world south->left
			case WEST -> Direction.NORTH; // FACING_ROTATIONS[4]: world north->left
			default -> aFacing;
		};
	}

	/**
	 * The front-local u coordinate (viewer-left→right in [0,1]) of a hit on the front face
	 * (the {@code UT.Code.getFacingCoordsClicked} counterpart for the horizontal face,
	 * UT.java:1734-1743: north face u=1-hitX, south u=hitX, west u=hitZ, east u=1-hitZ —
	 * the texture-left edge is the viewer's left on every horizontal face).
	 */
	public static double frontLocalU(Direction aFacing, double aHitX, double aHitZ) {
		return switch (aFacing) {
			case NORTH -> 1.0 - aHitX;
			case SOUTH -> aHitX;
			case EAST -> 1.0 - aHitZ; // upstream side-5 arm verbatim
			case WEST -> aHitZ; // upstream side-4 arm verbatim
			default -> aHitX;
		};
	}

	@Override
	public int[] getAccessibleSlotsFromSide(byte aSide) {
		if (!mSidedAccess || aSide == SIDE_ANY) return ALL; // upstream the all-entry rows
		Direction tFacing = Direction.from3DDataValue(getFacing());
		Direction tSide = Direction.from3DDataValue(aSide);
		if (tSide.getAxis() == Direction.Axis.Y) {
			return tSide == Direction.UP ? TOP_HALF : BOTTOM_HALF; // the physical halves
		}
		if (tSide == tFacing || tSide == tFacing.getOpposite()) return ALL; // front/back: the full set
		return tSide == viewerLeftOf(tFacing) ? LEFT_HALF : RIGHT_HALF; // the physical columns
	}

	@Override
	public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {
		return true; // upstream :120
	}

	@Override
	public boolean canExtractItem(int aSlot, byte aSide) {
		return true; // upstream :121
	}

	@Override
	public int stackLimit() {
		return 64;
	}

	/** Tag-twin state read (the seam test census reads the flag, not the block). */
	public boolean sidedAccess() {
		return mSidedAccess;
	}

	/**
	 * The one-page MUI panel — the wave-4 GUI ruling (144 slots, four quadrants, zero
	 * MenuType): the click-a-quadrant-open-36 window (:88/:103-104) was the 1.7.10 GUI-size
	 * compromise; the port opens everything at once. The block's front-face use arm calls
	 * {@link gregtech6.gui.machines.GT6MuiMachine#tryOpen}.
	 */
	@Override
	public brachy.modularui.screen.ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData aData,
			brachy.modularui.value.sync.PanelSyncManager aSyncManager, brachy.modularui.screen.UISettings aSettings) {
		return gregtech6.gui.machines.GT6StorageMUI.drawerPanel(this, aSyncManager);
	}
}
