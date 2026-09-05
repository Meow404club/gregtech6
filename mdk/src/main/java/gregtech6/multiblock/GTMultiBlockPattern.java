package gregtech6.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import javax.annotation.Nullable;

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
 * <p><b>The forming expectation (task p16-pattern-checker — the P12 ruling consciously
 * lifted, ADR 2026-09-05-p16-formation-scoping).</b> "Display only" was the P12
 * scoping; it is undone by pure INCREMENT here: every cell now optionally carries the
 * upstream {@code (partBlock, design, mode)} triple as {@link Cell#partBlock}/{@link
 * Cell#design}/{@link Cell#usage} ({@link Builder#formingPart}; the sentinel-free
 * default — {@code partBlock == null} — keeps every pre-existing factory and call site
 * byte-identical: the {@code is}/{@code anyOf}/{@code AIR} factories and the ghost
 * matcher face are frozen). A cell with the triple is authoritative for the server
 * check: the shared {@code GTMultiBlockStructureChecker} walks the pattern and drives
 * the upstream {@code checkAndSetTarget} path per cell, ending the hand-written-twice
 * maintenance the Coke Oven carried (pattern binding + hand-written loop, port
 * :113-155). The existence-probe seam (above) is untouched — {@code
 * getStructurePattern()} still defaults to null, and machines that don't bind a
 * pattern never reach the checker.
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

		/**
		 * The forming expectation, part 1 of 3 (task p16-pattern-checker, the ADR
		 * 2026-09-05-p16-formation-scoping enrichment): the {@code ONLY_*} usage mask the
		 * forming check writes into the part BE (upstream checkAndSetTarget {@code aMode},
		 * MultiBlockPartBlockEntity :85-126 table — the {@code ~NOT} complement form, so
		 * negative values are the NORM). 0 when the cell carries no forming expectation.
		 */
		public final int usage;

		/**
		 * The forming expectation, part 2 of 3: the design index written into the part BE
		 * (upstream {@code aDesign} — the boiler pipe holes carry 1). 0 when the cell
		 * carries no forming expectation.
		 */
		public final int design;

		/**
		 * The forming expectation, part 3 of 3: the part {@link Block} identity the forming
		 * check feeds {@code checkAndSetTarget} (the upstream registry-id pair — the block
		 * judgement AND the wand auto-place stock). Null = no forming expectation.
		 */
		@Nullable
		public final Block partBlock;

		private Cell(int aX, int aY, int aZ, Predicate<BlockState> aPredicate, boolean aHollow, int aUsage, int aDesign, @Nullable Block aPartBlock) {
			x = aX; y = aY; z = aZ;
			predicate = aPredicate;
			hollow = aHollow;
			usage = aUsage;
			design = aDesign;
			partBlock = aPartBlock;
		}

		/** The judgement against one world state — the seam the green/red match card will drive per cell. */
		public boolean matches(BlockState aState) {
			return predicate.test(aState);
		}

		/** True = a must-stay-hollow marker, false = a structural part. */
		public boolean isHollow() {
			return hollow;
		}

		/**
		 * True = the cell carries the FULL forming expectation (the shared checker drives
		 * the upstream {@code checkAndSetTarget} path with the {@link #partBlock}/
		 * {@link #design}/{@link #usage} triple); false = a declaration-only display cell
		 * (the P12 calibre — predicate judgement and ghost drawing only, no check-side
		 * write). Hollow markers never carry one.
		 */
		public boolean forms() {
			return partBlock != null;
		}
	}

	/**
	 * One HORIZONTAL SLAB of the structure — the declaration unit of the layer-sequence DSL
	 * (task p16-pattern-layers). A layer is an immutable list of cells whose {@code y} is
	 * FIXED AT 0: the caller declares the in-layer {@code (x, z)} footprint only, and the
	 * {@link Builder#layer}/{@link Builder#repeatable} sequence assigns each layer its
	 * {@code y} by ORDER (layer k of the sequence lands on {@code y = k} — the stacking axis
	 * is Y throughout, the only axis the GT6 shape census uses).
	 *
	 * <p><b>Where this comes from (mechanism-level clean-room).</b> The kTFRUAddon
	 * {@code LayerStructure} (LayerStructure.java:72-89) walks a layer sequence and its
	 * {@code ExpandableLayer} (ExpandableLayer.java:47-58) probes the repeat count AT CHECK
	 * TIME by trial-validation, rotating through "variations". None of that machinery is
	 * portable here (AGPL — code stays; runtime probing — contradicts the P12 immutable
	 * pattern): what carries over is the IDEA that a shape is a sequence of slabs with a
	 * variable-length repeat in the middle, re-declared per ADR
	 * 2026-09-05-p16-formation-scoping ② as a BUILD-TIME expansion: the family asks the
	 * factory for layer {@code i} by index (the {@code IntFunction<Layer>} below), stamps
	 * out {@code min..max} copies, and freezes an ordinary dumb immutable cell list — the
	 * runtime never learns layers existed.
	 *
	 * <p>The layer judgement vocabulary is the SAME triple the flat builder carries —
	 * {@link #part} (display), {@link #hollow} (the keep-empty marker) and
	 * {@link #formingPart} (the full {@code (partBlock, usage, design)} forming expectation,
	 * the p16-pattern-checker enrichment) — so a layered declaration is cell-for-cell
	 * interchangeable with a flat one.
	 */
	public static final class Layer {

		private final List<Cell> mCells;

		private Layer(List<Cell> aCells) {
			mCells = Collections.unmodifiableList(aCells);
		}

		/** The layer cells at {@code y == 0} in declaration order — immutable. */
		public List<Cell> cells() {
			return mCells;
		}

		/** The layer builder — the in-layer {@code (x, z)} footprint, duplicate offsets rejected. */
		public static Builder builder() {
			return new Builder();
		}

		/** The layer builder; offsets are layer-relative (the {@code y} comes from the sequence). */
		public static final class Builder {

			private final List<Cell> mCells = new ArrayList<>();
			private final Set<Long> mSeen = new HashSet<>();

			/** A structural part cell (drawn blue by the ghost preview) — declaration-only, no forming expectation. */
			public Builder part(int aX, int aZ, Predicate<BlockState> aPredicate) {
				return add(aX, aZ, aPredicate, false, 0, 0, null);
			}

			/** A must-stay-hollow marker cell (drawn grey by the ghost preview) — never carries a forming expectation. */
			public Builder hollow(int aX, int aZ, Predicate<BlockState> aPredicate) {
				return add(aX, aZ, aPredicate, true, 0, 0, null);
			}

			/**
			 * A structural part cell with the FULL forming expectation — the same
			 * {@code (partBlock, usage, design)} triple as the flat
			 * {@link GTMultiBlockPattern.Builder#formingPart} (the usage mask in the
			 * {@code ONLY_*} complement form, negative values the norm; the display predicate
			 * is {@link GTMultiBlockPattern#is(Block)} on the same block).
			 */
			public Builder formingPart(int aX, int aZ, Block aPartBlock, int aUsage, int aDesign) {
				if (aPartBlock == null) throw new IllegalArgumentException("a forming expectation needs a part block");
				return add(aX, aZ, is(aPartBlock), false, aUsage, aDesign, aPartBlock);
			}

			private Builder add(int aX, int aZ, Predicate<BlockState> aPredicate, boolean aHollow, int aUsage, int aDesign, @Nullable Block aPartBlock) {
				if (aPredicate == null) throw new IllegalArgumentException("a cell needs a predicate");
				if (!mSeen.add(pack(aX, 0, aZ))) {
					throw new IllegalArgumentException("duplicate layer cell (" + aX + "," + aZ + ")");
				}
				mCells.add(new Cell(aX, 0, aZ, aPredicate, aHollow, aUsage, aDesign, aPartBlock));
				return this;
			}

			/** Freezes the layer (an empty layer is a bug, not a slab). */
			public Layer build() {
				if (mCells.isEmpty()) throw new IllegalStateException("a layer needs at least one cell");
				return new Layer(mCells);
			}
		}
	}

	private final List<Cell> mCells;

	private final int mMinX, mMaxX, mMinY, mMaxY, mMinZ, mMaxZ;

	/** The offset key behind the duplicate rule — shared by the flat builder and the layer builder. */
	private static long pack(int aX, int aY, int aZ) {
		return ((long)(aX + Short.MAX_VALUE) << 34) | ((long)(aY + Short.MAX_VALUE) << 17) | (long)(aZ + Short.MAX_VALUE);
	}

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

	/**
	 * The pattern builder; offsets are structure-centre-relative, axes world-aligned.
	 *
	 * <p><b>The layer sequence (task p16-pattern-layers).</b> Beyond the flat per-cell
	 * methods, a declaration may stack {@link Layer} slabs: {@link #layer} appends one
	 * fixed layer, {@link #repeatable} appends a variable-length segment expanded to the
	 * SAME layer count {@code n} that {@link #build(int)} receives (the family
	 * {@code GTMultiBlockPatternFamily} is the per-size front over exactly this builder).
	 * Layers are recorded unexpanded and flattened at build time — layer k of the sequence
	 * lands on {@code y = k} — so a repeatable segment's followers shift with {@code n}
	 * (the kTFRU {@code ExpandableLayer} idea, ADR 2026-09-05-p16-formation-scoping ②,
	 * minus the runtime probing and the variation rotation: the expansion happens at
	 * BUILD time and freezes an ordinary dumb immutable cell list — the P12 immutability
	 * is untouched). The duplicate-offset rule (the {@code mSeen} precedent) spans the
	 * WHOLE flattened result: two layers claiming the same offset is a declaration bug.
	 */
	public static final class Builder {

		private final List<Cell> mCells = new ArrayList<>();
		private final Set<Long> mSeen = new HashSet<>();

		/** The recorded layer sequence: plain {@link Layer} slabs and {@link Repeatable} segments, unexpanded. */
		private final List<Object> mSequence = new ArrayList<>();

		/** The {@code y} of layer slot 0 (default 0 — the slab sequence is 0-based unless re-anchored). */
		private int mOriginY = 0;

		/**
		 * Re-anchors the layer sequence: layer slot 0 lands on {@code aOriginY} instead of 0
		 * (subsequent slots step by 1 as always). The calibre a shape centred on its
		 * CONTROLLER layer needs — the Large Boiler's bottom transmitter slab sits at
		 * {@code y = -1} with the controller in the middle slab ({@code originY(-1)}); a
		 * ground-based shape keeps the 0 default.
		 */
		public Builder originY(int aOriginY) {
			mOriginY = aOriginY;
			return this;
		}

		/** One variable-length segment: {@code min..max} layers, the layer for index {@code i} asked per expansion. */
		private record Repeatable(int mMin, int mMax, IntFunction<Layer> mLayer) {}

		/** A structural part cell (drawn blue by the ghost preview) — declaration-only, no forming expectation. */
		public Builder part(int aX, int aY, int aZ, Predicate<BlockState> aPredicate) {
			return add(aX, aY, aZ, aPredicate, false, 0, 0, null);
		}

		/** A must-stay-hollow marker cell (drawn grey by the ghost preview) — never carries a forming expectation. */
		public Builder hollow(int aX, int aY, int aZ, Predicate<BlockState> aPredicate) {
			return add(aX, aY, aZ, aPredicate, true, 0, 0, null);
		}

		/**
		 * A structural part cell with the FULL forming expectation (task p16-pattern-checker,
		 * the ADR 2026-09-05-p16-formation-scoping enrichment): the shared checker drives the
		 * upstream {@code checkAndSetTarget} path with the given part block + usage mask +
		 * design index, and the block identity doubles as the wand auto-place stock. The
		 * cell's display predicate is {@link #is(Block)} on the same block — one declaration,
		 * one judgement calibre (the ghost preview and the server check see the same table).
		 * The usage mask is the {@code ONLY_*} complement form (negative values are the norm).
		 */
		public Builder formingPart(int aX, int aY, int aZ, Block aPartBlock, int aUsage, int aDesign) {
			if (aPartBlock == null) throw new IllegalArgumentException("a forming expectation needs a part block");
			return add(aX, aY, aZ, is(aPartBlock), false, aUsage, aDesign, aPartBlock);
		}

		private Builder add(int aX, int aY, int aZ, Predicate<BlockState> aPredicate, boolean aHollow, int aUsage, int aDesign, @Nullable Block aPartBlock) {
			if (aPredicate == null) throw new IllegalArgumentException("a cell needs a predicate");
			if (!mSeen.add(pack(aX, aY, aZ))) {
				throw new IllegalArgumentException("duplicate pattern cell (" + aX + "," + aY + "," + aZ + ")");
			}
			mCells.add(new Cell(aX, aY, aZ, aPredicate, aHollow, aUsage, aDesign, aPartBlock));
			return this;
		}

		/**
		 * Appends ONE fixed layer to the sequence — its cells land on the next {@code y}
		 * slot after the preceding sequence entries. Recorded unexpanded: a repeatable
		 * segment later in the sequence shifts this layer's followers at {@link #build(int)}
		 * time. Mixes with the flat per-cell methods (the flat cells keep their absolute
		 * {@code y}); the duplicate rule catches overlaps either way.
		 */
		public Builder layer(Layer aLayer) {
			if (aLayer == null) throw new IllegalArgumentException("a layer needs cells");
			mSequence.add(aLayer);
			return this;
		}

		/**
		 * Appends a VARIABLE-LENGTH segment (task p16-pattern-layers ① — the kTFRU
		 * {@code ExpandableLayer} repeat, re-scoped to build time): the stack expands to
		 * {@code n} layers — one per index {@code 0..n-1} of {@code aLayer} — where {@code n}
		 * is the value {@link #build(int)} receives, validated against the declared
		 * {@code [aMin, aMax]} window (out-of-window {@code n} is a declaration bug).
		 * The stacking axis is Y throughout (the {@code axis=Y} of the task card — the only
		 * axis the GT6 shape census stacks on, so it is the sequence's fixed convention,
		 * not a per-call parameter).
		 */
		public Builder repeatable(int aMin, int aMax, IntFunction<Layer> aLayer) {
			if (aMin < 0) throw new IllegalArgumentException("a negative repeat minimum is a bug");
			if (aMax < aMin) throw new IllegalArgumentException("the repeat maximum must not undercut the minimum");
			if (aLayer == null) throw new IllegalArgumentException("a repeatable segment needs a layer factory");
			mSequence.add(new Repeatable(aMin, aMax, aLayer));
			return this;
		}

		/** Freezes the pattern (an empty declaration is a bug, not a pattern). */
		public GTMultiBlockPattern build() {
			for (Object tEntry : mSequence) {
				if (tEntry instanceof Repeatable) throw new IllegalStateException("a repeatable segment needs a layer count — build(int)");
			}
			return freeze(expand(-1));
		}

		/**
		 * Freezes the pattern with every {@link #repeatable} segment expanded to
		 * {@code aSize} layers — the family's per-size expansion point (task
		 * p16-pattern-layers ②). Each segment's {@code [min, max]} window is enforced
		 * first (out-of-window = declaration bug); the result is an ordinary immutable
		 * dumb cell list, identical in kind to a flat declaration's.
		 */
		public GTMultiBlockPattern build(int aSize) {
			if (aSize < 0) throw new IllegalArgumentException("a negative layer count is a bug");
			for (Object tEntry : mSequence) {
				if (tEntry instanceof Repeatable tSegment && (aSize < tSegment.mMin || aSize > tSegment.mMax)) {
					throw new IllegalArgumentException("layer count " + aSize + " outside the declared [" + tSegment.mMin + "," + tSegment.mMax + "] window");
				}
			}
			return freeze(expand(aSize));
		}

		/**
		 * Flattens the declaration: the flat cells first (declaration order), then the
		 * sequence left-to-right — layer k of the flattened sequence onto {@code y = k}.
		 * The duplicate rule spans the whole result (the {@code mSeen} snapshot covers the
		 * flat cells; the layer walk extends it — a cross-layer repeat is a declaration
		 * bug). The snapshot is LOCAL: repeated {@code build}s of the same builder yield
		 * equal patterns instead of tripping over their own earlier expansion.
		 */
		private List<Cell> expand(int aSize) {
			List<Cell> tAll = new ArrayList<>(mCells);
			Set<Long> tSeen = new HashSet<>(mSeen);
			int tY = mOriginY;
			for (Object tEntry : mSequence) {
				if (tEntry instanceof Repeatable tSegment) {
					for (int i = 0; i < aSize; i++) tY = appendLayer(tAll, tSeen, tSegment.mLayer().apply(i), tY);
				} else {
					tY = appendLayer(tAll, tSeen, (Layer)tEntry, tY);
				}
			}
			return tAll;
		}

		/** Stamps one layer at {@code aY} — duplicate offsets against the WHOLE result rejected. */
		private static int appendLayer(List<Cell> aAll, Set<Long> aSeen, Layer aLayer, int aY) {
			for (Cell tCell : aLayer.cells()) {
				if (!aSeen.add(pack(tCell.x, aY, tCell.z))) {
					throw new IllegalArgumentException("duplicate pattern cell across layers (" + tCell.x + "," + aY + "," + tCell.z + ")");
				}
				aAll.add(new Cell(tCell.x, aY, tCell.z, tCell.predicate, tCell.hollow, tCell.usage, tCell.design, tCell.partBlock));
			}
			return aY + 1;
		}

		private static GTMultiBlockPattern freeze(List<Cell> aCells) {
			if (aCells.isEmpty()) throw new IllegalStateException("a pattern needs at least one cell");
			return new GTMultiBlockPattern(aCells);
		}
	}
}
