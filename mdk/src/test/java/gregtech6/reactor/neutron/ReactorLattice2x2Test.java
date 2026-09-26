package gregtech6.reactor.neutron;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The 2x2 exchange topology pins (task debt-reactor-a-neutron-core): the verbatim
 * side-to-slot tables of MultiTileEntityReactorCore.java:153 and the per-slot
 * in-block/outward target structure of Core2x2.java:63-97, checked against the
 * corner geometry comment of Core.java:155-158.
 */
public class ReactorLattice2x2Test {

	@Test
	public void sideTablesAreCore153Verbatim() {
		int[] tS2103 = {0, 0, 2, 1, 0, 3, 0};
		int[] tS0312 = {0, 0, 0, 3, 1, 2, 0};
		assertEquals(7, ReactorLattice2x2.S2103.length);
		assertEquals(7, ReactorLattice2x2.S0312.length);
		for (int i = 0; i < 7; i++) {
			assertEquals(tS2103[i], ReactorLattice2x2.S2103[i], "S2103[" + i + "]");
			assertEquals(tS0312[i], ReactorLattice2x2.S0312[i], "S0312[" + i + "]");
		}
	}

	@Test
	public void inBlockTargetsAreTheOrthogonalPairs() {
		// Core2x2.java:66-67/75-76/84-85/93-94 — slot 0 hits 1+2, slot 1 hits 0+3,
		// slot 2 hits 0+3, slot 3 hits 1+2 (the orthogonal corners of the quadrant map)
		int[][] tExpected = {{1, 2}, {0, 3}, {0, 3}, {1, 2}};
		for (int tSlot = 0; tSlot < 4; tSlot++) {
			assertEquals(2, ReactorLattice2x2.IN_BLOCK_TARGETS[tSlot].length);
			assertEquals(tExpected[tSlot][0], ReactorLattice2x2.IN_BLOCK_TARGETS[tSlot][0], "slot " + tSlot);
			assertEquals(tExpected[tSlot][1], ReactorLattice2x2.IN_BLOCK_TARGETS[tSlot][1], "slot " + tSlot);
		}
	}

	@Test
	public void outwardSidesMatchTheQuadrantCorners() {
		// Core2x2.java:68-69/77-78/86-87/95-96 — Z-direction first, then X-direction:
		// slot 0 (Z_NEG,X_NEG), 1 (Z_POS,X_NEG), 2 (Z_NEG,X_POS), 3 (Z_POS,X_POS);
		// sides 2..5 = NORTH/SOUTH/WEST/EAST of the GT6 order
		int[][] tExpected = {{2, 4}, {3, 4}, {2, 5}, {3, 5}};
		for (int tSlot = 0; tSlot < 4; tSlot++) {
			assertEquals(tExpected[tSlot][0], ReactorLattice2x2.OUTWARD_SIDES[tSlot][0], "slot " + tSlot + " Z side");
			assertEquals(tExpected[tSlot][1], ReactorLattice2x2.OUTWARD_SIDES[tSlot][1], "slot " + tSlot + " X side");
		}
	}

	@Test
	public void neighbourReceivingSlotAllEightCorners() {
		// Core2x2.java:68-69/77-78/86-87/95-96 hand-traced: the view side is the side of
		// the neighbour facing this core — a Z_NEG neighbour sees us on its Z_POS (3),
		// a Z_POS neighbour on its Z_NEG (2), an X_NEG neighbour on its X_POS (5),
		// an X_POS neighbour on its X_NEG (4)
		assertEquals(1, ReactorLattice2x2.neighbourReceivingSlot(0, 2, 3), "slot0 -> Z_NEG core");
		assertEquals(2, ReactorLattice2x2.neighbourReceivingSlot(0, 4, 5), "slot0 -> X_NEG core");
		assertEquals(0, ReactorLattice2x2.neighbourReceivingSlot(1, 3, 2), "slot1 -> Z_POS core");
		assertEquals(3, ReactorLattice2x2.neighbourReceivingSlot(1, 4, 5), "slot1 -> X_NEG core");
		assertEquals(3, ReactorLattice2x2.neighbourReceivingSlot(2, 2, 3), "slot2 -> Z_NEG core");
		assertEquals(0, ReactorLattice2x2.neighbourReceivingSlot(2, 5, 4), "slot2 -> X_POS core");
		assertEquals(2, ReactorLattice2x2.neighbourReceivingSlot(3, 3, 2), "slot3 -> Z_POS core");
		assertEquals(1, ReactorLattice2x2.neighbourReceivingSlot(3, 5, 4), "slot3 -> X_POS core");
	}

	@Test
	public void facingCoresReflectIntoEachOthersSlots() {
		// Symmetry check of the hand-traced pairs: two cores adjacent along Z —
		// slot 0 firing Z_NEG lands on the neighbour's slot 1, and that neighbour's
		// slot 1 firing Z_POS lands back on this core's slot 0
		int tView = 3;
		int tBackView = 2;
		int tThere = ReactorLattice2x2.neighbourReceivingSlot(0, ReactorLattice2x2.SIDE_Z_NEG, tView);
		int tBack = ReactorLattice2x2.neighbourReceivingSlot(tThere, ReactorLattice2x2.SIDE_Z_POS, tBackView);
		assertEquals(1, tThere);
		assertEquals(0, tBack);
	}
}
