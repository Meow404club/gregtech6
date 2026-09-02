package gregtech6.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A declarative multiblock structure pattern (task p12-ghost-pattern-api — card 1 of the
 * ghost-preview family): an immutable list of cells, each one a relative offset
 * {@code (x, y, z)} plus a per-cell judgement in {@link Predicate}&lt;{@link BlockState}&gt;
 * shape, plus the pure facing anchor that rotates the whole structure around the
 * controller.
 *
 * <p><b>Where this comes from.</b> The upstream GT6 1.7.10 multiblocks (all 30 of them,
 * p12-ghost-upstream-census) carry their shape as the implicit
 * {@code (offset, partID, design, mode)} tuple table handwritten inside each
 * {@code checkStructure2} — there is no pattern object, builder or client ghost upstream
 * (negative, proven). This class makes that table explicit. The Coke Oven binding is the
 * word-for-word transcription of its loop (MultiTileEntityCokeOven.java:46-60, port
 * TileEntityCokeOven.java:97-111) — the same transcription the POC held as a hardcoded
 * renderer table, lifted to a declarative home. {@code checkStructure2} itself is NEVER
 * run to derive a pattern: it writes the world (the centre-cell {@code removeBlock} /
 * setBlockToAir pair, TileEntityCokeOven.java:100-101) and it embeds the builder-wand
 * auto-place (Util.checkAndSetTarget) — both are deliberately outside this API. A pattern
 * is pure display/sharing data; the server check remains hand-written per machine.
 *
 * <p><b>The facing anchor (the generalized POC rotation).</b> The upstream loops add
 * {@code i, j, k} in WORLD axes; the facing enters only through the structure-centre
 * offset {@code -OFF[facing]} (the {@code getOffsetXN/YN/ZN} pure arithmetic,
 * TileEntityBase01Root.java:174-176 tables / :194-206 accessors). That anchor rule is
 * the GT6-wide convention ("Main Block centered on Side and facing outwards"), so it
 * lives here as the static pure functions {@link #anchorOffset(byte)} and
 * {@link #cellOffset(byte, int, int, int)} — generalized from the POC's Coke-Oven-named
 * hardcode to any pattern's cells via {@link #worldOffset(byte, Cell)}. The offline
 * four-facing full-table test semantics of the POC carry over unchanged. Per-machine
 * extra centre offsets (the census "(上方1)" family — Autoclave and friends) are not
 * modeled yet; the second adopting machine adds a builder-time anchor constant.
 *
 * <p><b>The predicate seam (deliberate escape hatch, unimplemented on purpose).</b>
 * Judgements are {@code Predicate<BlockState>} shapes, never hardcoded block-id
 * enumerations — the seam the three classes of dynamic structure consumers will ride
 * (census q4, upstream references in parentheses):
 * <ol>
 * <li><b>Runtime interpolation</b> (Electrolyzer :63 {@code mActive ? 2 + rng(6) : 0},
 *     Sluice :54, Shredder :51, Fusion :99 — the part choice depends on live machine
 *     state): the predicate shape is the seam — a cell judgement may consult anything
 *     state-visible; where the offset SET itself is dynamic, the caller regenerates the
 *     pattern per evaluation (a pattern is cheap immutable data, the checker walks it).</li>
 * <li><b>Count quotas</b> (Fusion :52-72 core-part counting, LogisticsCore :113-144 —
 *     N-of-M across cells): NOT expressible per-cell; the seam is that a pattern is a
 *     dumb ordered cell list, so a future checker can consume it with cross-cell
 *     counters while the pattern object stays ignorant of quotas.</li>
 * <li><b>Existence probes</b> (LightningRod :84 unbounded {@code while} rod,
 *     BedrockDrill :87-108 terrain-dependent base layer): NOT expressible at all; the
 *     seam is that binding is optional — {@code getStructurePattern()} defaults to null
 *     and probe-style machines keep their hand-written checkStructure2 with no pattern
 *     (rigid cokeoven-shaped machines are the API's consumers).</li>
 * </ol>
 *
 * <p><b>No size cap</b> is baked in: upstream spans 3x3x3/26 cells (the Coke Oven
 * family) up to the 19x19 Fusion octagon (~600-1000 cells). The Coke Oven is this
 * card's only consumer; Fusion-grade consumers will need a cached mesh on top, not an
 * API change.
 */
public final class GTMultiBlockPattern {

	/**
	 * The GT6 side-offset tables mirrored from TileEntityBase01Root.java:174-176 (the
	 * {@code OFFX/OFFY/OFFZ} behind {@code getOffsetXN/YN/ZN}, :194-206). Index = the
	 * side byte ({@code DOWN UP NORTH SOUTH WEST EAST} = vanilla
	 * {@code Direction.get3DDataValue()}); the port's UP entry is +1 (upstream CS
	 * OFFY[0]=-1 is the 1.7.10 convention — the framework-early port decision, unreachable
	 * in the HORIZONTAL_FACING 2..5 domain).
	 */
	private static final int[] OFF_X = { 0, 0, 0, 0, -1, 1 };
	private static final int[] OFF_Y = { 0, 1, 0, 0, 0, 0 };
	private static final int[] OFF_Z = { 0, 0, -1, 1, 0, 0 };

	/**
	 * The canonical must-stay-hollow predicate — the "keep this empty" marker for air
	 * pockets (the Coke Oven centre, upstream :52: a non-air centre is a check FAILURE,
	 * not a silent clear).
	 */
	public static final Predicate<BlockState> AIR = BlockState::isAir;

	/** The part-block predicate — the upstream {@code checkAndSetTarget} part-id equality as a shape. */
	public static Predicate<BlockState> is(Block aBlock) {
		return aState -> aState.is(aBlock);
	}

	/** The block-set predicate — the upstream "try A then B" part family (Oven :62-63 casing|alt) as one shape. */
	public static Predicate<BlockState> anyOf(Block... aBlocks) {
		return aState -> {
			for (Block tBlock : aBlocks) if (aState.is(tBlock)) return true;
			return false;
		};
	}

	/** One pattern cell: a structure-centre-relative offset plus its judgement. */
	public static final class Cell {

		/** Structure-centre-relative offset (world axes — the upstream loop's {@code i, j, k}). */
		public final int x;
		/** Structure-centre-relative offset (world axes — the upstream loop's {@code i, j, k}). */
		public final int y;
		/** Structure-centre-relative offset (world axes — the upstream loop's {@code i, j, k}). */
		public final int z;

		/** The per-cell judgement (the predicate seam — see the class javadoc). */
		public final Predicate<BlockState> predicate;

		/**
		 * True = a must-stay-hollow marker (an air pocket), false = a structural part.
		 * Presentation classification, separate from {@link #predicate}: the ghost preview
		 * paints hollow cells as the grey "keep this hollow" marker.
		 */
		public final boolean hollow;

		private Cell(int aX, int aY, int aZ, Predicate<BlockState> aPredicate, boolean aHollow) {
			x = aX; y = aY; z = aZ;
			predicate = aPredicate;
			hollow = aHollow;
		}

		/** The judgement against one world state — the seam the green/red match card will drive per cell. */
		public boolean matches(BlockState aState) {
			return predicate.test(aState);
		}

		/** True = a must-stay-hollow marker, false = a structural part. */
		public boolean isHollow() {
			return hollow;
		}
	}

	private final List<Cell> mCells;

	private final int mMinX, mMaxX, mMinY, mMaxY, mMinZ, mMaxZ;

	private GTMultiBlockPattern(List<Cell> aCells) {
		mCells = Collections.unmodifiableList(aCells);
		int tMinX = Integer.MAX_VALUE, tMaxX = Integer.MIN_VALUE;
		int tMinY = Integer.MAX_VALUE, tMaxY = Integer.MIN_VALUE;
		int tMinZ = Integer.MAX_VALUE, tMaxZ = Integer.MIN_VALUE;
		for (Cell tCell : aCells) {
			tMinX = Math.min(tMinX, tCell.x); tMaxX = Math.max(tMaxX, tCell.x);
			tMinY = Math.min(tMinY, tCell.y); tMaxY = Math.max(tMaxY, tCell.y);
			tMinZ = Math.min(tMinZ, tCell.z); tMaxZ = Math.max(tMaxZ, tCell.z);
		}
		mMinX = tMinX; mMaxX = tMaxX;
		mMinY = tMinY; mMaxY = tMaxY;
		mMinZ = tMinZ; mMaxZ = tMaxZ;
	}

	/** The cells in declaration order — immutable; declaration order is draw order for the ghost preview. */
	public List<Cell> cells() {
		return mCells;
	}

	/** The bounding box over ALL cells (parts and hollow markers), structure-centre-relative. */
	public int minX() { return mMinX; }
	/** The bounding box over ALL cells (parts and hollow markers), structure-centre-relative. */
	public int maxX() { return mMaxX; }
	/** The bounding box over ALL cells (parts and hollow markers), structure-centre-relative. */
	public int minY() { return mMinY; }
	/** The bounding box over ALL cells (parts and hollow markers), structure-centre-relative. */
	public int maxY() { return mMaxY; }
	/** The bounding box over ALL cells (parts and hollow markers), structure-centre-relative. */
	public int minZ() { return mMinZ; }
	/** The bounding box over ALL cells (parts and hollow markers), structure-centre-relative. */
	public int maxZ() { return mMaxZ; }

	/**
	 * The world offset of one cell relative to the CONTROLLER position, for a controller
	 * facing {@code aFacing} (a {@code get3DDataValue()} byte): the anchor plus the cell's
	 * centre-relative offset. Allocation-per-call — the per-frame draw folds the anchor
	 * in once instead; this is the correctness reference (and the green/red card's lookup).
	 */
	public int[] worldOffset(byte aFacing, Cell aCell) {
		return cellOffset(aFacing, aCell.x, aCell.y, aCell.z);
	}

	/**
	 * The structure-centre offset relative to the CONTROLLER position for a facing byte —
	 * the {@code getOffsetXN/YN/ZN} pure mirror ({@code -OFF[facing]},
	 * TileEntityBase01Root.java:194-206). MC-free.
	 */
	public static int[] anchorOffset(byte aFacing) {
		return new int[] { -OFF_X[aFacing], -OFF_Y[aFacing], -OFF_Z[aFacing] };
	}

	/**
	 * The pure facing rotation — the world offset of one canonical cell
	 * {@code (aI, aJ, aK)} (structure-centre-relative) relative to the CONTROLLER
	 * position: {@code (i, j, k) - OFF[facing]} (the POC Coke-Oven arithmetic verbatim,
	 * generalized to any pattern via {@link #worldOffset(byte, Cell)}). MC-free — the
	 * offline four-facing full-table test drives it directly.
	 */
	public static int[] cellOffset(byte aFacing, int aI, int aJ, int aK) {
		return new int[] { aI - OFF_X[aFacing], aJ - OFF_Y[aFacing], aK - OFF_Z[aFacing] };
	}

	/** The builder — cells in declaration order, duplicate offsets rejected (a declaration bug). */
	public static Builder builder() {
		return new Builder();
	}

	/** The pattern builder; offsets are structure-centre-relative, axes world-aligned. */
	public static final class Builder {

		private final List<Cell> mCells = new ArrayList<>();
		private final Set<Long> mSeen = new HashSet<>();

		/** A structural part cell (drawn blue by the ghost preview). */
		public Builder part(int aX, int aY, int aZ, Predicate<BlockState> aPredicate) {
			return add(aX, aY, aZ, aPredicate, false);
		}

		/** A must-stay-hollow marker cell (drawn grey by the ghost preview). */
		public Builder hollow(int aX, int aY, int aZ, Predicate<BlockState> aPredicate) {
			return add(aX, aY, aZ, aPredicate, true);
		}

		private Builder add(int aX, int aY, int aZ, Predicate<BlockState> aPredicate, boolean aHollow) {
			if (aPredicate == null) throw new IllegalArgumentException("a cell needs a predicate");
			if (!mSeen.add(pack(aX, aY, aZ))) {
				throw new IllegalArgumentException("duplicate pattern cell (" + aX + "," + aY + "," + aZ + ")");
			}
			mCells.add(new Cell(aX, aY, aZ, aPredicate, aHollow));
			return this;
		}

		/** Freezes the pattern (an empty declaration is a bug, not a pattern). */
		public GTMultiBlockPattern build() {
			if (mCells.isEmpty()) throw new IllegalStateException("a pattern needs at least one cell");
			return new GTMultiBlockPattern(mCells);
		}

		private static long pack(int aX, int aY, int aZ) {
			return ((long)(aX + Short.MAX_VALUE) << 34) | ((long)(aY + Short.MAX_VALUE) << 17) | (long)(aZ + Short.MAX_VALUE);
		}
	}
}
