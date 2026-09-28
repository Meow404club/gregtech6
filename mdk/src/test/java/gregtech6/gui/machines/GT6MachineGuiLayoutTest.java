package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The layout-descriptor pin table (task r8-gui-layout-descriptor acceptance ①): every case
 * of {@link GT6MachineGuiLayout} asserted against the upstream coordinates VERBATIM — the
 * expected literals below are transcribed from gregapi/gui/ContainerCommonBasicMachine.java
 * addSlots (:51-156 inputs, :158-263 outputs), the fluid arms included (7 fires the >6 arm,
 * 4 fires the >3 arm; 6/3 ride the else arms — the boundary pin). Pure offline JUnit: the
 * descriptor is static data with no MC bootstrap.
 */
class GT6MachineGuiLayoutTest {

	/** The literal case-table assert — length and every xy pair. */
	private static void assertTable(int[][] aActual, int[][] aExpected, String aWhat) {
		assertEquals(aExpected.length, aActual.length, aWhat + ": the slot count");
		for (int i = 0; i < aExpected.length; i++) {
			assertArrayEquals(aExpected[i], aActual[i], aWhat + ": slot " + i);
		}
	}

	// ---------------------------------------------------------------------------
	// the input case table (:51-156)
	// ---------------------------------------------------------------------------

	@Test
	void inputCaseTableTranscribesUpstream() {
		// :52-53
		assertTable(GT6MachineGuiLayout.inputPositions(0, 0), new int[0][], "input case 0");
		// :55 — the fluid arm: >6 fires 7, the boundary 6 rides 25
		assertTable(GT6MachineGuiLayout.inputPositions(1, 0), new int[][] {{53, 25}}, "input case 1 f0");
		assertTable(GT6MachineGuiLayout.inputPositions(1, 6), new int[][] {{53, 25}}, "input case 1 f6 boundary");
		assertTable(GT6MachineGuiLayout.inputPositions(1, 7), new int[][] {{53, 7}}, "input case 1 f7 arm");
		// :58-59
		assertTable(GT6MachineGuiLayout.inputPositions(2, 0), new int[][] {{35, 25}, {53, 25}}, "input case 2 f0");
		assertTable(GT6MachineGuiLayout.inputPositions(2, 7), new int[][] {{35, 7}, {53, 7}}, "input case 2 f7 arm");
		// :62-64 — the case-3 shape the old extrapolation got WRONG (35/53/71)
		assertTable(GT6MachineGuiLayout.inputPositions(3, 0), new int[][] {{17, 25}, {35, 25}, {53, 25}}, "input case 3 f0");
		assertTable(GT6MachineGuiLayout.inputPositions(3, 7), new int[][] {{17, 7}, {35, 7}, {53, 7}}, "input case 3 f7 arm");
		// :67-70 — the >3 arm: 4 fires 7/25, the boundary 3 rides 16/34
		assertTable(GT6MachineGuiLayout.inputPositions(4, 0), new int[][] {{35, 16}, {53, 16}, {35, 34}, {53, 34}}, "input case 4 f0");
		assertTable(GT6MachineGuiLayout.inputPositions(4, 3), new int[][] {{35, 16}, {53, 16}, {35, 34}, {53, 34}}, "input case 4 f3 boundary");
		assertTable(GT6MachineGuiLayout.inputPositions(4, 4), new int[][] {{35, 7}, {53, 7}, {35, 25}, {53, 25}}, "input case 4 f4 arm");
		// :73-77
		assertTable(GT6MachineGuiLayout.inputPositions(5, 0), new int[][] {{17, 16}, {35, 16}, {53, 16}, {35, 34}, {53, 34}}, "input case 5 f0");
		assertTable(GT6MachineGuiLayout.inputPositions(5, 4), new int[][] {{17, 7}, {35, 7}, {53, 7}, {35, 25}, {53, 25}}, "input case 5 f4 arm");
		// :80-85 — the MIXER/BATH case
		assertTable(GT6MachineGuiLayout.inputPositions(6, 0), new int[][] {{17, 16}, {35, 16}, {53, 16}, {17, 34}, {35, 34}, {53, 34}}, "input case 6 f0");
		assertTable(GT6MachineGuiLayout.inputPositions(6, 4), new int[][] {{17, 7}, {35, 7}, {53, 7}, {17, 25}, {35, 25}, {53, 25}}, "input case 6 f4 arm");
		// :88-94 — the fluid arm DROPS from case 7 on (fixed grids)
		assertTable(GT6MachineGuiLayout.inputPositions(7, 0), new int[][] {
				{17, 7}, {35, 7}, {53, 7}, {17, 25}, {35, 25}, {53, 25}, {17, 43}}, "input case 7");
		assertTable(GT6MachineGuiLayout.inputPositions(7, 9), new int[][] {
				{17, 7}, {35, 7}, {53, 7}, {17, 25}, {35, 25}, {53, 25}, {17, 43}}, "input case 7 is fluid-blind");
		assertTable(GT6MachineGuiLayout.inputPositions(8, 0), new int[][] {
				{17, 7}, {35, 7}, {53, 7}, {17, 25}, {35, 25}, {53, 25}, {17, 43}, {35, 43}}, "input case 8");
		assertTable(GT6MachineGuiLayout.inputPositions(9, 0), new int[][] {
				{17, 7}, {35, 7}, {53, 7}, {17, 25}, {35, 25}, {53, 25}, {17, 43}, {35, 43}, {53, 43}}, "input case 9");
		// :118-127 — the tail is RIGHT-aligned: (53,61), NOT (17,61)
		assertTable(GT6MachineGuiLayout.inputPositions(10, 0), new int[][] {
				{17, 7}, {35, 7}, {53, 7}, {17, 25}, {35, 25}, {53, 25}, {17, 43}, {35, 43}, {53, 43}, {53, 61}}, "input case 10");
		// :130-140
		assertTable(GT6MachineGuiLayout.inputPositions(11, 0), new int[][] {
				{17, 7}, {35, 7}, {53, 7}, {17, 25}, {35, 25}, {53, 25}, {17, 43}, {35, 43}, {53, 43}, {35, 61}, {53, 61}}, "input case 11");
		// :143-154 — the default branch is the 12 case
		assertTable(GT6MachineGuiLayout.inputPositions(12, 0), new int[][] {
				{17, 7}, {35, 7}, {53, 7}, {17, 25}, {35, 25}, {53, 25}, {17, 43}, {35, 43}, {53, 43}, {17, 61}, {35, 61}, {53, 61}}, "input case 12");
	}

	// ---------------------------------------------------------------------------
	// the output case table (:158-263) — the input mirror, all canPut(F) upstream
	// ---------------------------------------------------------------------------

	@Test
	void outputCaseTableTranscribesUpstream() {
		// :159-160
		assertTable(GT6MachineGuiLayout.outputPositions(0, 0), new int[0][], "output case 0");
		// :162
		assertTable(GT6MachineGuiLayout.outputPositions(1, 0), new int[][] {{107, 25}}, "output case 1 f0");
		assertTable(GT6MachineGuiLayout.outputPositions(1, 7), new int[][] {{107, 7}}, "output case 1 f7 arm");
		// :165-166
		assertTable(GT6MachineGuiLayout.outputPositions(2, 0), new int[][] {{107, 25}, {125, 25}}, "output case 2 f0");
		assertTable(GT6MachineGuiLayout.outputPositions(2, 7), new int[][] {{107, 7}, {125, 7}}, "output case 2 f7 arm");
		// :169-171
		assertTable(GT6MachineGuiLayout.outputPositions(3, 0), new int[][] {{107, 25}, {125, 25}, {143, 25}}, "output case 3 f0");
		assertTable(GT6MachineGuiLayout.outputPositions(3, 7), new int[][] {{107, 7}, {125, 7}, {143, 7}}, "output case 3 f7 arm");
		// :174-177
		assertTable(GT6MachineGuiLayout.outputPositions(4, 0), new int[][] {{107, 16}, {125, 16}, {107, 34}, {125, 34}}, "output case 4 f0");
		assertTable(GT6MachineGuiLayout.outputPositions(4, 4), new int[][] {{107, 7}, {125, 7}, {107, 25}, {125, 25}}, "output case 4 f4 arm");
		// :180-184
		assertTable(GT6MachineGuiLayout.outputPositions(5, 0), new int[][] {{107, 16}, {125, 16}, {143, 16}, {107, 34}, {125, 34}}, "output case 5 f0");
		assertTable(GT6MachineGuiLayout.outputPositions(5, 4), new int[][] {{107, 7}, {125, 7}, {143, 7}, {107, 25}, {125, 25}}, "output case 5 f4 arm");
		// :187-192
		assertTable(GT6MachineGuiLayout.outputPositions(6, 0), new int[][] {{107, 16}, {125, 16}, {143, 16}, {107, 34}, {125, 34}, {143, 34}}, "output case 6 f0");
		assertTable(GT6MachineGuiLayout.outputPositions(6, 4), new int[][] {{107, 7}, {125, 7}, {143, 7}, {107, 25}, {125, 25}, {143, 25}}, "output case 6 f4 arm");
		// :195-201
		assertTable(GT6MachineGuiLayout.outputPositions(7, 0), new int[][] {
				{107, 7}, {125, 7}, {143, 7}, {107, 25}, {125, 25}, {143, 25}, {107, 43}}, "output case 7");
		// :204-211
		assertTable(GT6MachineGuiLayout.outputPositions(8, 0), new int[][] {
				{107, 7}, {125, 7}, {143, 7}, {107, 25}, {125, 25}, {143, 25}, {107, 43}, {125, 43}}, "output case 8");
		// :214-222
		assertTable(GT6MachineGuiLayout.outputPositions(9, 0), new int[][] {
				{107, 7}, {125, 7}, {143, 7}, {107, 25}, {125, 25}, {143, 25}, {107, 43}, {125, 43}, {143, 43}}, "output case 9");
		// :225-234 — the tail is RIGHT-aligned: (143,61), NOT (107,61)
		assertTable(GT6MachineGuiLayout.outputPositions(10, 0), new int[][] {
				{107, 7}, {125, 7}, {143, 7}, {107, 25}, {125, 25}, {143, 25}, {107, 43}, {125, 43}, {143, 43}, {143, 61}}, "output case 10");
		// :237-247
		assertTable(GT6MachineGuiLayout.outputPositions(11, 0), new int[][] {
				{107, 7}, {125, 7}, {143, 7}, {107, 25}, {125, 25}, {143, 25}, {107, 43}, {125, 43}, {143, 43}, {125, 61}, {143, 61}}, "output case 11");
		// :250-261 — the default branch is the 12 case (the cokeoven 9-output rides case 9)
		assertTable(GT6MachineGuiLayout.outputPositions(12, 0), new int[][] {
				{107, 7}, {125, 7}, {143, 7}, {107, 25}, {125, 25}, {143, 25}, {107, 43}, {125, 43}, {143, 43}, {107, 61}, {125, 61}, {143, 61}}, "output case 12");
	}

	// ---------------------------------------------------------------------------
	// the declared-but-unseated special slot (:49) and the shape contract
	// ---------------------------------------------------------------------------

	@Test
	void specialSlotCoordinateIsDeclaredUnseated() {
		assertArrayEquals(new int[] {80, 43}, GT6MachineGuiLayout.SPECIAL_SLOT, ":49 the special slot coordinate");
	}

	/** The descriptor always returns exactly the asked case's rows (the consumer loops index it 1:1). */
	@Test
	void everyCountReturnsExactlyThatManyPositions() {
		for (int i = 0; i <= 12; i++) {
			assertEquals(i, GT6MachineGuiLayout.inputPositions(i, 0).length, "input count " + i);
			assertEquals(i, GT6MachineGuiLayout.outputPositions(i, 0).length, "output count " + i);
		}
	}
}
