package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.items.IItemHandler;

import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * The item-intercept framework tests (task p10-cover-item-intercept, ADR
 * 2026-09-01-p10-cover-item-intercept) — the offline half of the acceptance:
 * the three-gate truth table at the host level (intercept hit refuses / override hit
 * lets the cover decide / the plain default passes through — the upstream
 * TileEntityBase04Covers:343-365 host-final dispatch shape), the side isolation
 * (a cover on face A never intercepts face B) and the oven capability loop (the
 * side-aware decorator mounts the gates before the inner inventory, and a pass
 * actually reaches it). The ForgeCapabilities getCapability dispatch itself needs the
 * live transformer stack (TestMachineBlockEntityNBTTest:71 precedent) and is exercised
 * by runServer/RCON; the framework card ships no consuming cover (the B-card precedent),
 * so these doubles stand in for the pooled five (Shutter/Conveyor/RobotArm/FilterItem/
 * RetrieverItem).
 */
public class CoverItemInterceptTest extends GTCoverTestBase {

	/** Upstream :209/:210 — refuses both directions outright. */
	static final class InterceptCover extends CoverTextureSimple {
		InterceptCover() {super(TEST_SPRITE);}
		@Override public boolean interceptItemInsert(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {return true;}
		@Override public boolean interceptItemExtract(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {return true;}
	}

	/** Upstream :212/:213 + :215/:216 — claims both answers and decides per the ctor flags. */
	static final class OverrideDecideCover extends CoverTextureSimple {
		final boolean mInsert, mExtract;
		OverrideDecideCover(boolean aInsert, boolean aExtract) {super(TEST_SPRITE); mInsert = aInsert; mExtract = aExtract;}
		@Override public boolean canInsertItemOverride(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {return true;}
		@Override public boolean canInsertItem(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {return mInsert;}
		@Override public boolean canExtractItemOverride(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {return true;}
		@Override public boolean canExtractItem(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {return mExtract;}
	}

	/** Upstream :211 + :214 — claims the slot visibility and narrows it to slot 1. */
	static final class NarrowSlotsCover extends CoverTextureSimple {
		NarrowSlotsCover() {super(TEST_SPRITE);}
		static final int[] SLOTS = {1};
		@Override public boolean getAccessibleSlotsFromSideOverride(byte aCoverSide, CoverData aData, byte aSide) {return true;}
		@Override public int[] getAccessibleSlotsFromSide(byte aCoverSide, CoverData aData, byte aSide, int[] aDefault) {return SLOTS;}
	}

	static ItemStack stack(int aCount) {
		return new ItemStack(Items.IRON_INGOT, aCount);
	}

	/** Installs the given cover behaviour on the face (re-binds the fixture item first-wins). */
	static void mount(TileEntityOvenCoverProbe aOven, byte aSide, ICover aCover) {
		CoverRegistry.put(COVER_ITEMS[aSide], aCover);
		assertTrue(aOven.setCoverItem(aSide, new ItemStack(COVER_ITEMS[aSide]), null, false, true), "the fixture cover installs");
		assertTrue(aOven.isCovered(aSide), "the face carries the cover under test");
	}

	// ---------------------------------------------------------------------------
	// gate 1+2 — the intercept pair: a hit refuses, only on its own face
	// ---------------------------------------------------------------------------

	@Test
	void interceptHitRefusesInsertAndExtract() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 1, new InterceptCover());

		assertFalse(tOven.canInsertItem((byte) 1, 0, stack(8)), ":352 — the intercept hit refuses the insert");
		assertFalse(tOven.canExtractItem((byte) 1, 1, stack(1)), ":361 — the intercept hit refuses the extract");
	}

	@Test
	void interceptIsSideIsolated() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 0, new InterceptCover()); // DOWN refuses

		// face A refuses both directions...
		assertFalse(tOven.canInsertItem((byte) 0, 0, stack(8)));
		assertFalse(tOven.canExtractItem((byte) 0, 0, stack(1)));
		// ...faces B..F stay untouched (no cover there — the :351 null-behaviour branch)
		for (byte tSide = 1; tSide < 6; tSide++) {
			assertTrue(tOven.canInsertItem(tSide, 0, stack(8)), "face " + tSide + " passes: A face's cover never gates another face");
			assertTrue(tOven.canExtractItem(tSide, 0, stack(1)));
		}
	}

	@Test
	void plainCoverGatesPassThrough() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 2, testCover()); // the AbstractCoverDefault defaults

		int[] tDefault = {0, 1, 2, 3, 4};
		assertTrue(tOven.canInsertItem((byte) 2, 0, stack(8)), ":355 — no intercept, no override claim → the host surface is unobstructed");
		assertTrue(tOven.canExtractItem((byte) 2, 0, stack(1)));
		assertSame(tDefault, tOven.getAccessibleSlotsFromSide((byte) 2, tDefault), ":346 — no override claim → the host array passes by reference");
		// the invalid side never consults a cover (the upstream SIDES_INVALID face)
		assertTrue(tOven.canInsertItem((byte) -1, 0, stack(8)));
		assertSame(tDefault, tOven.getAccessibleSlotsFromSide((byte) -1, tDefault));
	}

	// ---------------------------------------------------------------------------
	// gate 3 — the override pair: the cover answers, ANDed with the host half later
	// ---------------------------------------------------------------------------

	@Test
	void overrideLetsTheCoverDecideInsert() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 1, new OverrideDecideCover(false, false));

		assertFalse(tOven.canInsertItem((byte) 1, 0, stack(8)), ":353 — the cover answers no");
		assertFalse(tOven.canExtractItem((byte) 1, 0, stack(1)), ":362 — the cover answers no");

		mount(tOven, (byte) 2, new OverrideDecideCover(true, true));
		assertTrue(tOven.canInsertItem((byte) 2, 0, stack(8)), ":353 — the cover answers yes (the host half joins in the wrapper)");
		assertTrue(tOven.canExtractItem((byte) 2, 0, stack(1)));
		// a YES cover on face 2 does not move face 1's NO cover answer
		assertFalse(tOven.canInsertItem((byte) 1, 0, stack(8)), "the face-2 answer never leaks to face 1");
	}

	@Test
	void overrideClaimsNarrowTheAccessibleSlots() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 3, new NarrowSlotsCover());

		int[] tDefault = {0, 1, 2, 3, 4};
		assertSame(NarrowSlotsCover.SLOTS, tOven.getAccessibleSlotsFromSide((byte) 3, tDefault), ":345 — the claimed answer replaces the host array");
		// the neighbouring face with the plain fixture cover still passes through
		mount(tOven, (byte) 4, testCover());
		assertSame(tDefault, tOven.getAccessibleSlotsFromSide((byte) 4, tDefault));
		// and the intercept cover never claims slots (its claim flag stays false)
		mount(tOven, (byte) 5, new InterceptCover());
		assertSame(tDefault, tOven.getAccessibleSlotsFromSide((byte) 5, tDefault));
	}

	@Test
	void gateDefaultsOnABareHost() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		int[] tDefault = {0, 1};
		assertTrue(tOven.canInsertItem((byte) 0, 0, stack(8)), "no covers at all → the gate is a pure pass");
		assertTrue(tOven.canExtractItem((byte) 0, 0, stack(1)));
		assertSame(tDefault, tOven.getAccessibleSlotsFromSide((byte) 0, tDefault));
	}

	// ---------------------------------------------------------------------------
	// the oven capability loop — the side-aware decorator gates before the inventory
	// ---------------------------------------------------------------------------

	@Test
	void wrapperLoopsThroughWithAPlainCover() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 0, testCover()); // the plain default: transparent to transfer

		IItemHandler tDown = tOven.newCoverGatedHandler(Direction.DOWN);
		ItemStack tLeftover = tDown.insertItem(0, stack(8), false);
		assertTrue(tLeftover.isEmpty(), "the plain cover passes the insert");
		assertEquals(8, tOven.getInventory().getStackInSlot(0).getCount(), "the loop closes: the stack reached the inner inventory");

		ItemStack tDrawn = tDown.extractItem(0, 8, false);
		assertEquals(8, tDrawn.getCount(), "the plain cover passes the extract");
		assertTrue(tOven.getInventory().getStackInSlot(0).isEmpty(), "the inner inventory really surrendered the stack");
	}

	@Test
	void wrapperRefusesOnlyOnTheCoveredFace() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 0, new InterceptCover()); // DOWN refuses

		// the DOWN decorator: insert returns the stack untouched, extract nothing — the inner slot stays clean
		IItemHandler tDown = tOven.newCoverGatedHandler(Direction.DOWN);
		ItemStack tRefused = tDown.insertItem(0, stack(8), false);
		assertEquals(8, tRefused.getCount(), "the refused stack comes back whole");
		assertTrue(tOven.getInventory().getStackInSlot(0).isEmpty(), "the gate fired BEFORE the inner inventory");
		assertTrue(tDown.extractItem(0, 8, false).isEmpty(), "the covered face refuses the extract");

		// the UP decorator on the same oven: face A's cover never gates face B (side isolation, capability face)
		IItemHandler tUp = tOven.newCoverGatedHandler(Direction.UP);
		assertTrue(tUp.insertItem(0, stack(8), false).isEmpty(), "the uncovered face inserts freely");
		assertEquals(8, tOven.getInventory().getStackInSlot(0).getCount());
		assertEquals(8, tUp.extractItem(0, 8, false).getCount(), "the uncovered face extracts freely");
	}

	@Test
	void wrapperRespectsTheAccessibleSlotsClaim() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 1, new NarrowSlotsCover()); // UP only exposes slot 1

		IItemHandler tUp = tOven.newCoverGatedHandler(Direction.UP);
		assertEquals(4, tUp.insertItem(0, stack(4), false).getCount(), "slot 0 is outside the claimed slots → refused");
		assertTrue(tOven.getInventory().getStackInSlot(0).isEmpty());
		assertFalse(tUp.isItemValid(0, stack(1)), "isItemValid mirrors the slot-visibility gate");
		assertTrue(tUp.isItemValid(1, stack(1)));

		assertTrue(tUp.insertItem(1, stack(4), false).isEmpty(), "slot 1 is the claimed slot → passes");
		assertEquals(4, tOven.getInventory().getStackInSlot(1).getCount(), "the loop closes through the claimed slot");
		assertEquals(4, tUp.extractItem(1, 8, false).getCount());
		assertTrue(tUp.extractItem(0, 8, false).isEmpty(), "the hidden slot never extracts");
		assertArrayEquals(new int[]{1}, NarrowSlotsCover.SLOTS, "the narrow claim is the only slot face");
	}

	@Test
	void wrapperSimulateNeverMutatesEitherBranch() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 0, testCover());

		IItemHandler tDown = tOven.newCoverGatedHandler(Direction.DOWN);
		assertTrue(tDown.insertItem(0, stack(8), true).isEmpty(), "the pass-branch simulate reports success");
		assertTrue(tOven.getInventory().getStackInSlot(0).isEmpty(), "the simulate left the inner inventory untouched");
		assertTrue(tDown.extractItem(0, 64, true).isEmpty(), "nothing to draw yet");

		// remounting runs the :294 occupied-face refusal — dismantle first
		assertTrue(tOven.setCoverItem((byte) 0, ItemStack.EMPTY, null, false, true), "the dismantle clears the face");
		mount(tOven, (byte) 0, new InterceptCover());
		assertEquals(8, tDown.insertItem(0, stack(8), true).getCount(), "the refuse-branch simulate refuses exactly like the real call");
		assertTrue(tOven.getInventory().getStackInSlot(0).isEmpty());
	}

	@Test
	void wrapperOnABareOvenMatchesTheRawHandler() {
		TileEntityOvenCoverProbe tOven = leveledOven(); // no covers anywhere — the negative regression anchor

		IItemHandler tNorth = tOven.newCoverGatedHandler(Direction.NORTH);
		assertTrue(tNorth.insertItem(0, stack(8), false).isEmpty());
		assertEquals(8, tOven.getInventory().getStackInSlot(0).getCount());
		assertEquals(8, tNorth.extractItem(0, 8, false).getCount());
		assertTrue(tOven.getInventory().getStackInSlot(0).isEmpty());
	}
}
