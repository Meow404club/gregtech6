package gregtech6.gui.machines;

/**
 * The basic-machine GUI layout descriptor — the FAITHFUL per-case transcription of the
 * upstream slot-position case table (task r8-gui-layout-descriptor, design card B): every
 * coordinate below is transcribed case-by-case from gregapi/gui/ContainerCommonBasicMachine.java
 * addSlots (:45-271), the switch that upstream runs over {@code mRecipes.mInputItemsCount}
 * (:51-156) and {@code mRecipes.mOutputItemsCount} (:158-263).
 *
 * <p>The upstream shape: inputs live in the left three columns x 17/35/53, outputs mirrored
 * in the right three columns x 107/125/143. Counts 1-3 stack in one row whose y rides the
 * fluid arm {@code fluidCount > 6 ? 7 : 25} (:55/:58-64 inputs, :162-171 outputs); counts
 * 4-6 stack two rows riding {@code fluidCount > 3 ? 7 : 16} / {@code 25 : 34} (:67-85/
 * :174-192); counts 7+ drop the fluid arm for the fixed 3-column grid (:87-155/:194-263)
 * whose bottom-row tail is RIGHT-aligned — the 10th input lands (53,61) not (17,61)
 * (:127), the 10th output (143,61) not (107,61) (:234), the 11th pair x 35/53 inputs
 * (:139-140) and x 125/143 outputs (:246-247). The 12 case is the upstream default branch
 * (:142-155/:249-262); no served map declares more.
 *
 * <p>The special slot (:49, {@link #SPECIAL_SLOT}) is a DECLARED COORDINATE ONLY — this
 * repo's panel topology has no seat for it (the declaration deviation settled on the design
 * card; a future fidelity card may revive it).
 *
 * <p>Consumers: {@link GTBasicMachineMUI#buildPanel} (both banks, live) and the
 * {@link GTBasicMachineMenu#inputSlotPos} >=3 arm (the upstream-extrapolation WARNING debt
 * cleared there). The fluid arm reads the SAME tank-bank length the fluid display seats
 * render ({@code Host.getFluidInputTanks()}/{@code getFluidOutputTanks()}) — on every
 * single-block machine that array is sized by the same RM counts upstream reads
 * (TileEntityBasicMachine :325-328).
 *
 * <p>Pure static data — no MC bootstrap, offline-pinnable (GT6MachineGuiLayoutTest pins
 * every case against the upstream coordinates verbatim).
 */
public final class GT6MachineGuiLayout {

	/** The :49 special-slot coordinate — declared, never seated (see the class doc). */
	public static final int[] SPECIAL_SLOT = {80, 43};

	private GT6MachineGuiLayout() {
	}

	/**
	 * The input-slot positions in inventory order — the upstream :51-156 case table verbatim.
	 * {@code aItemCount} selects the case (0-12, the 12 case is the upstream default branch),
	 * {@code aFluidCount} is the map's input-fluid declaration driving the 1-6 y arms.
	 */
	public static int[][] inputPositions(int aItemCount, int aFluidCount) {
		boolean tHigh = aFluidCount > 6; // the :55-64 arm
		boolean tTight = aFluidCount > 3; // the :67-85 arm
		return switch (aItemCount) {
			case 0 -> new int[0][];
			case 1 -> new int[][] {{53, tHigh ? 7 : 25}};
			case 2 -> new int[][] {{35, tHigh ? 7 : 25}, {53, tHigh ? 7 : 25}};
			case 3 -> new int[][] {{17, tHigh ? 7 : 25}, {35, tHigh ? 7 : 25}, {53, tHigh ? 7 : 25}};
			case 4 -> new int[][] {{35, tTight ? 7 : 16}, {53, tTight ? 7 : 16},
					{35, tTight ? 25 : 34}, {53, tTight ? 25 : 34}};
			case 5 -> new int[][] {{17, tTight ? 7 : 16}, {35, tTight ? 7 : 16}, {53, tTight ? 7 : 16},
					{35, tTight ? 25 : 34}, {53, tTight ? 25 : 34}};
			case 6 -> new int[][] {{17, tTight ? 7 : 16}, {35, tTight ? 7 : 16}, {53, tTight ? 7 : 16},
					{17, tTight ? 25 : 34}, {35, tTight ? 25 : 34}, {53, tTight ? 25 : 34}};
			case 7 -> grid3(7, 17); // :88-94
			case 8 -> grid3(8, 17); // :97-104
			case 9 -> grid3(9, 17); // :107-115
			case 10 -> grid3(10, 17); // :118-127 — the tail is (53,61), right-aligned
			case 11 -> grid3(11, 17); // :130-140 — the tail x 35/53
			default -> grid3(12, 17); // :143-154 — the upstream default branch is the 12 case
		};
	}

	/**
	 * The output-slot positions in inventory order — the upstream :158-263 case table
	 * verbatim (the input mirror at x 107/125/143; the {@code setCanPut(F)} face stays the
	 * consumer's concern, every one of these seats is output-only upstream).
	 */
	public static int[][] outputPositions(int aItemCount, int aFluidCount) {
		boolean tHigh = aFluidCount > 6; // the :162-171 arm
		boolean tTight = aFluidCount > 3; // the :174-192 arm
		return switch (aItemCount) {
			case 0 -> new int[0][];
			case 1 -> new int[][] {{107, tHigh ? 7 : 25}};
			case 2 -> new int[][] {{107, tHigh ? 7 : 25}, {125, tHigh ? 7 : 25}};
			case 3 -> new int[][] {{107, tHigh ? 7 : 25}, {125, tHigh ? 7 : 25}, {143, tHigh ? 7 : 25}};
			case 4 -> new int[][] {{107, tTight ? 7 : 16}, {125, tTight ? 7 : 16},
					{107, tTight ? 25 : 34}, {125, tTight ? 25 : 34}};
			case 5 -> new int[][] {{107, tTight ? 7 : 16}, {125, tTight ? 7 : 16}, {143, tTight ? 7 : 16},
					{107, tTight ? 25 : 34}, {125, tTight ? 25 : 34}};
			case 6 -> new int[][] {{107, tTight ? 7 : 16}, {125, tTight ? 7 : 16}, {143, tTight ? 7 : 16},
					{107, tTight ? 25 : 34}, {125, tTight ? 25 : 34}, {143, tTight ? 25 : 34}};
			case 7 -> grid3(7, 107); // :195-201
			case 8 -> grid3(8, 107); // :204-211
			case 9 -> grid3(9, 107); // :214-222
			case 10 -> grid3(10, 107); // :225-234 — the tail is (143,61), right-aligned
			case 11 -> grid3(11, 107); // :237-247 — the tail x 125/143
			default -> grid3(12, 107); // :250-261 — the upstream default branch is the 12 case
		};
	}

	/**
	 * The fluid-independent 3-column grid of the 7+ cases: the first 9 cells at
	 * ({@code aX0} + 18*(i%3), 7 + 18*(i/3)), the 9-count tail right-aligned on the y 61
	 * row — upstream skips toward x max (inputs :127/:139-140, outputs :234/:246-247),
	 * it never left-aligns the bottom row.
	 */
	private static int[][] grid3(int aCount, int aX0) {
		int[][] tPos = new int[aCount][];
		for (int i = 0; i < Math.min(aCount, 9); i++) {
			tPos[i] = new int[] {aX0 + 18 * (i % 3), 7 + 18 * (i / 3)};
		}
		for (int i = 9; i < aCount; i++) {
			tPos[i] = new int[] {aX0 + 18 * (3 - (aCount - 9) + (i - 9)), 61};
		}
		return tPos;
	}
}
