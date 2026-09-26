package gregtech6.reactor.neutron;

/**
 * The 2x2 reactor core slot topology (task debt-reactor-a-neutron-core), the MC-free
 * tables of the upstream exchange pass (MultiTileEntityReactorCore2x2.java:53-99).
 * The core is a single block with 4 rod slots — not a multiblock:
 *
 * <pre>
 * 0 and 2 are at SIDE_Z_NEG    1 3      --&gt;X+
 * 1 and 3 are at SIDE_Z_POS  2|0 2|0   |0 2
 * 0 and 1 are at SIDE_X_NEG  3|1 3|1   v1 3
 * 2 and 3 are at SIDE_X_POS    0 2     Z+
 * </pre>
 *
 * (MultiTileEntityReactorCore.java:155-158.) Side indices follow the GT6 order
 * 0..5 = DOWN/UP/NORTH/SOUTH/WEST/EAST — identical to
 * {@code Direction.get3DDataValue()} (UT6 port doc).
 *
 * <p>Each of the 4 slots emits to exactly 4 reflectors: the 2 orthogonal in-block
 * slots plus the 2 horizontal neighbouring cores on its outward corner sides
 * (Core2x2.java:63-97). Emitters are processed sequentially but independent within
 * one exchange — emissions read {@code oNeutronCounts} (the previous tick's copy)
 * and reflections only add onto {@code mNeutronCounts}.
 */
public final class ReactorLattice2x2 {

	/** SIDE_Z_NEG = NORTH = 2 (GT6 side order). */
	public static final int SIDE_Z_NEG = 2;
	/** SIDE_Z_POS = SOUTH = 3. */
	public static final int SIDE_Z_POS = 3;
	/** SIDE_X_NEG = WEST = 4. */
	public static final int SIDE_X_NEG = 4;
	/** SIDE_X_POS = EAST = 5. */
	public static final int SIDE_X_POS = 5;

	/** MultiTileEntityReactorCore.java:153 verbatim — {@code {0,0,2,1,0,3,0}}, indexed by the side the neighbour sees us on. */
	public static final int[] S2103 = {0, 0, 2, 1, 0, 3, 0};
	/** MultiTileEntityReactorCore.java:153 verbatim — {@code {0,0,0,3,1,2,0}}, same indexing. */
	public static final int[] S0312 = {0, 0, 0, 3, 1, 2, 0};

	/** In-block reflection targets per emitting slot (Core2x2.java:66-67/75-76/84-85/93-94). */
	public static final int[][] IN_BLOCK_TARGETS = {{1, 2}, {0, 3}, {0, 3}, {1, 2}};

	/** Outward corner sides per emitting slot (Core2x2.java:68-69/77-78/86-87/95-96), Z-direction first then X-direction. */
	public static final int[][] OUTWARD_SIDES = {{SIDE_Z_NEG, SIDE_X_NEG}, {SIDE_Z_POS, SIDE_X_NEG}, {SIDE_Z_NEG, SIDE_X_POS}, {SIDE_Z_POS, SIDE_X_POS}};

	private ReactorLattice2x2() {/**/}

	/**
	 * The slot a horizontal neighbour core receives an emission on, Core2x2.java:68-69/
	 * 77-78/86-87/95-96 — the diagonal slots 0/3 pick their Z-direction target through
	 * {@link #S2103} and X-direction through {@link #S0312}, slots 1/2 the reverse;
	 * {@code aSideFromNeighbourView} is the side of the neighbour block that faces this
	 * core (upstream {@code mSideOfTileEntity}). Horizontal neighbour cores only
	 * (Core2x2.java:58-61 — vertical adjacencies never join the exchange).
	 */
	public static int neighbourReceivingSlot(int aEmittingSlot, int aOutwardSide, int aSideFromNeighbourView) {
		boolean tZDirection = aOutwardSide == SIDE_Z_NEG || aOutwardSide == SIDE_Z_POS;
		boolean tDiagonalSlot = aEmittingSlot == 0 || aEmittingSlot == 3;
		return (tZDirection == tDiagonalSlot ? S2103 : S0312)[aSideFromNeighbourView];
	}
}
