package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.multiblock.GTMultiBlockPattern;

/**
 * The Large Boiler pattern binding + the structure-mode write pin (task p13-large-boiler
 * acceptance ①, the TileEntityCokeOvenPatternTest form):
 *
 * <p>① the bound pattern is the LITERAL 34+1 expectation — the 34 part cells in the
 * upstream :97-140 check order (9 transmitters :104-112, 9 middle walls :114-122, the
 * top centre :124, two 8-cell rings :125-135) plus the hollow air cell above the anchor
 * (:102) appended last; ② the predicates judge BLOCK IDENTITY only (the pattern seam
 * ruling — the ONLY_* modes and the design-1 pipe-hole markers are the checkAndSetTarget
 * write, verified per-cell in {@link #structureModesAreWrittenByTheCheck}); ③ the bounds
 * fold to the x/z 3x3, y -1..+2 shell; ④ the binding is lazy-stable; ⑤ the anchor is the
 * front-same-layer cell (:98 — NO getOffsetYN, unlike the CokeOven) and isInsideStructure
 * (:170-173) folds to that box.
 *
 * <p>The offline fixture binds BRICKS as the wall block and STONE as the transmitter
 * (the frozen registry forbids the real GTMultiBlocks blocks offline; the per-variant
 * wall PARAMETERISATION is pinned by the row-table test in
 * {@link LargeBoilerSemanticsTest} over the wallPath column).
 */
public class LargeBoilerPatternTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<PatternBoiler> sBoilerType;
	static BlockEntityType<HeatTransmitterBlockEntity> sTransmitterType;

	/** The concrete test BE — the boiler class over a vanilla-block BET, fixture blocks bound. */
	public static final class PatternBoiler extends TileEntityLargeBoiler {
		public PatternBoiler(BlockPos aPos, BlockState aState) {
			super(sBoilerType, aPos, aState);
		}
		@Override
		protected net.minecraft.world.level.block.Block getWallBlock() {
			return Blocks.BRICKS;
		}
		@Override
		protected net.minecraft.world.level.block.Block getTransmitterBlock() {
			return Blocks.STONE;
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildFixtures() {
		BlockEntityType<PatternBoiler>[] tHolder = (BlockEntityType<PatternBoiler>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(PatternBoiler::new, Blocks.BRICKS, Blocks.STONE).build(null);
		sBoilerType = tHolder[0];
		BlockEntityType<HeatTransmitterBlockEntity>[] tTxHolder = (BlockEntityType<HeatTransmitterBlockEntity>[]) new BlockEntityType<?>[1];
		tTxHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new HeatTransmitterBlockEntity(tTxHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		sTransmitterType = tTxHolder[0];
	}

	// the literal upstream check order, cells relative to the ANCHOR (structure centre):
	// y -1 transmitters (:104-112), y 0 middle walls (:114-122), then (0,2,0) (:124),
	// then the two rings (:125-135), then the hollow air cell (:102) appended last.
	private static final int[][] LITERAL_PARTS = {
			// :104-112 — the bottom 3x3, z rows outer (-1, 0, +1)
			{ -1, -1, -1 }, { 0, -1, -1 }, { 1, -1, -1 },
			{ -1, -1,  0 }, { 0, -1,  0 }, { 1, -1,  0 },
			{ -1, -1,  1 }, { 0, -1,  1 }, { 1, -1,  1 },
			// :114-122 — the middle 3x3 (the controller cell included — the self-cell arm passes it)
			{ -1, 0, -1 }, { 0, 0, -1 }, { 1, 0, -1 },
			{ -1, 0,  0 }, { 0, 0,  0 }, { 1, 0,  0 },
			{ -1, 0,  1 }, { 0, 0,  1 }, { 1, 0,  1 },
			// :124 — the top centre (pipe hole 1)
			{ 0, 2, 0 },
			// :125-135 — the first ring (i == 1: the four side-middles are pipe holes 2..5)
			{ -1, 1, -1 }, { 0, 1, -1 }, { 1, 1, -1 },
			{ -1, 1,  0 },               { 1, 1,  0 },
			{ -1, 1,  1 }, { 0, 1,  1 }, { 1, 1,  1 },
			// :125-135 — the second ring (i == 2, no holes)
			{ -1, 2, -1 }, { 0, 2, -1 }, { 1, 2, -1 },
			{ -1, 2,  0 },               { 1, 2,  0 },
			{ -1, 2,  1 }, { 0, 2,  1 }, { 1, 2,  1 }
	};

	/** The bottom 9 + the middle 9 are FULL 3x3s; the first 18 entries judge the transmitter block. */
	private static final boolean[] IS_TRANSMITTER = {
			true, true, true, true, true, true, true, true, true,
			false, false, false, false, false, false, false, false, false,
			false,
			false, false, false, false, false, false, false, false,
			false, false, false, false, false, false, false, false
	};

	@Test
	public void boundPatternIsTheLiteralUpstreamCheckPlusHollowAir() {
		PatternBoiler tBoiler = sBoilerType.create(C1, Blocks.BRICKS.defaultBlockState());
		GTMultiBlockPattern tPattern = tBoiler.getStructurePattern();
		assertNotNull(tPattern, "the large boiler declares a pattern");

		var tCells = tPattern.cells();
		assertEquals(36, tCells.size(), "35 part cells (the middle 3x3 INCLUDING the controller's own cell) + the hollow air cell");

		BlockState tWalls = Blocks.BRICKS.defaultBlockState();
		BlockState tTx = Blocks.STONE.defaultBlockState();
		BlockState tAir = Blocks.AIR.defaultBlockState();
		int tAssertions = 0;
		for (int tCell = 0; tCell < 35; tCell++) {
			GTMultiBlockPattern.Cell tPart = tCells.get(tCell);
			assertEquals(LITERAL_PARTS[tCell][0], tPart.x, "cell " + tCell + " x — literal transcription, upstream check order");
			assertEquals(LITERAL_PARTS[tCell][1], tPart.y, "cell " + tCell + " y");
			assertEquals(LITERAL_PARTS[tCell][2], tPart.z, "cell " + tCell + " z");
			tAssertions += 3;
			assertFalse(tPart.isHollow(), "cell " + tCell + " is a structural part");
			tAssertions++;
			BlockState tExpected = IS_TRANSMITTER[tCell] ? tTx : tWalls;
			assertTrue(tPart.matches(tExpected), "cell " + tCell + " judges " + (IS_TRANSMITTER[tCell] ? "the transmitter" : "the wall block") + " (the getWallBlock/getTransmitterBlock hooks)");
			tAssertions++;
			assertFalse(tPart.matches(tAir), "cell " + tCell + " judges air a failure");
			tAssertions++;
			assertFalse(tPart.matches(IS_TRANSMITTER[tCell] ? tWalls : tTx), "cell " + tCell + " rejects the other block");
			tAssertions++;
		}
		// the hollow: appended LAST (:102 — the front-upper cell must ALREADY be air)
		GTMultiBlockPattern.Cell tHollow = tCells.get(35);
		assertEquals(0, tHollow.x); assertEquals(1, tHollow.y); assertEquals(0, tHollow.z);
		tAssertions += 3;
		assertTrue(tHollow.isHollow(), "the front-upper cell is the keep-this-hollow marker");
		tAssertions++;
		assertTrue(tHollow.matches(tAir), "the hollow judgement is AIR (upstream :102 — a non-air cell FAILS the check)");
		tAssertions++;
		assertFalse(tHollow.matches(tWalls), "the hollow rejects a filled cell");
		tAssertions++;
		assertEquals(251, tAssertions, "the full literal pin: 35x7 + hollow x6");
	}

	@Test
	public void boundsFoldToTheShell() {
		PatternBoiler tBoiler = sBoilerType.create(C1, Blocks.BRICKS.defaultBlockState());
		GTMultiBlockPattern tPattern = tBoiler.getStructurePattern();
		assertEquals(-1, tPattern.minX()); assertEquals(1, tPattern.maxX());
		assertEquals(-1, tPattern.minY()); assertEquals(2, tPattern.maxY()); // the y -1..+2 tall shell
		assertEquals(-1, tPattern.minZ()); assertEquals(1, tPattern.maxZ());
	}

	@Test
	public void bindingIsLazyStable() {
		PatternBoiler tBoiler = sBoilerType.create(C1, Blocks.BRICKS.defaultBlockState());
		assertSame(tBoiler.getStructurePattern(), tBoiler.getStructurePattern(),
				"the lazy cache returns the same immutable instance");
	}

	/**
	 * The anchor (:98): the structure centre sits ONE cell in FRONT of the facing at the
	 * SAME layer. Facing north (2): the front is +Z (south in world terms — the CokeOven
	 * command comment), so the anchor is (x, y, z+1) and isInsideStructure (:170-173) folds
	 * to x±1, y -1..+2, z±1 AROUND the anchor.
	 */
	@Test
	public void anchorIsFrontSameLayerAndIsInsideStructureFoldsAroundIt() {
		PatternBoiler tBoiler = sBoilerType.create(new BlockPos(100, 64, 100), Blocks.BRICKS.defaultBlockState());
		tBoiler.mFacing = 2; // NORTH
		// anchor = (100, 64, 101): front-upper hollow at (100, 65, 101)
		assertTrue(tBoiler.isInsideStructure(99, 63, 100), "the corner (anchor-1, anchor.y-1, anchor.z-1) is inside");
		assertTrue(tBoiler.isInsideStructure(101, 66, 102), "the corner (anchor+1, anchor.y+2, anchor.z+1) is inside");
		assertTrue(tBoiler.isInsideStructure(100, 64, 100), "the CONTROLLER's own cell is inside (the middle 3x3 north cell)");
		assertFalse(tBoiler.isInsideStructure(100, 62, 101), "below the base layer is outside");
		assertFalse(tBoiler.isInsideStructure(100, 67, 101), "above the top ring is outside");
		assertFalse(tBoiler.isInsideStructure(102, 64, 101), "beyond the front rim is outside");
		assertFalse(tBoiler.isInsideStructure(97, 64, 101), "far behind the controller is outside");
	}

	/**
	 * The structure-mode write pin (the pattern-seam ruling's other half): the hand-written
	 * checkStructure2 writes the ONLY_* part modes and the design-1 pipe-hole markers
	 * through checkAndSetTarget — the bottom layer ONLY_ENERGY_IN, the middle layer
	 * ONLY_FLUID_IN, the top shell ONLY_FLUID_OUT with design 1 on the top centre and the
	 * four first-ring side-middles only.
	 */
	@Test
	public void structureModesAreWrittenByTheCheck() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		// the part-BE factory keyed by block: BRICKS walls get the plain part BE, STONE
		// transmitters get the relaying HeatTransmitter BE
		tLevel.mBeFactory = (aPos, aState) -> {
			if (aState.is(Blocks.BRICKS)) return sPartType.create(aPos, aState);
			if (aState.is(Blocks.STONE)) return sTransmitterType.create(aPos, aState);
			return null;
		};
		PatternBoiler tBoiler = placeController(tLevel, sBoilerType, new BlockPos(100, 64, 100), (byte)2);
		BlockPos tAnchor = new BlockPos(100, 64, 101); // front of facing north, same layer
		// pre-place the 34 part cells (the frame arm's world state); the hollow stays air
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) tLevel.setBlock(tAnchor.offset(tDX, -1, tDZ), Blocks.STONE.defaultBlockState(), 3);
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			BlockPos tCell = tAnchor.offset(tDX, 0, tDZ);
			if (!tCell.equals(tBoiler.getBlockPos())) tLevel.setBlock(tCell, Blocks.BRICKS.defaultBlockState(), 3);
		}
		tLevel.setBlock(tAnchor.offset(0, 2, 0), Blocks.BRICKS.defaultBlockState(), 3);
		for (int i = 1; i < 3; i++) {
			tLevel.setBlock(tAnchor.offset(-1, i, -1), Blocks.BRICKS.defaultBlockState(), 3);
			tLevel.setBlock(tAnchor.offset(0, i, -1), Blocks.BRICKS.defaultBlockState(), 3);
			tLevel.setBlock(tAnchor.offset(1, i, -1), Blocks.BRICKS.defaultBlockState(), 3);
			tLevel.setBlock(tAnchor.offset(-1, i, 0), Blocks.BRICKS.defaultBlockState(), 3);
			tLevel.setBlock(tAnchor.offset(1, i, 0), Blocks.BRICKS.defaultBlockState(), 3);
			tLevel.setBlock(tAnchor.offset(-1, i, 1), Blocks.BRICKS.defaultBlockState(), 3);
			tLevel.setBlock(tAnchor.offset(0, i, 1), Blocks.BRICKS.defaultBlockState(), 3);
			tLevel.setBlock(tAnchor.offset(1, i, 1), Blocks.BRICKS.defaultBlockState(), 3);
		}
		assertTrue(tBoiler.checkStructure(true), "the pre-placed shell passes the check and links every part");

		// the bottom layer: ONLY_ENERGY_IN, design 0 (the :104-112 call)
		MultiBlockPartBlockEntity tBase = (MultiBlockPartBlockEntity) tLevel.getBlockEntity(tAnchor.offset(0, -1, 0));
		assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_IN, tBase.mMode, "the transmitter base relays ENERGY IN only");
		assertEquals(0, tBase.mDesign);
		// the middle layer: ONLY_FLUID_IN, design 0 (the :114-122 call). NOTE: the NORTH
		// middle cell is the controller's own cell when facing north — the self-cell arm
		// passes it WITHOUT writing a part BE; assert the SOUTH middle cell instead
		MultiBlockPartBlockEntity tMiddle = (MultiBlockPartBlockEntity) tLevel.getBlockEntity(tAnchor.offset(0, 0, 1));
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_IN, tMiddle.mMode, "the middle ring relays FLUID IN only");
		assertEquals(0, tMiddle.mDesign);
		assertTrue(tLevel.getBlockEntity(tAnchor.offset(0, 0, -1)) instanceof TileEntityLargeBoiler,
				"the middle cell behind the facing IS the controller (the :115 self-cell pass, no part BE there)");
		// the top centre: ONLY_FLUID_OUT + design 1 = pipe hole (the :124 call)
		MultiBlockPartBlockEntity tTop = (MultiBlockPartBlockEntity) tLevel.getBlockEntity(tAnchor.offset(0, 2, 0));
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_OUT, tTop.mMode, "the top shell relays FLUID OUT only");
		assertEquals(TileEntityLargeBoiler.DESIGN_PIPE_HOLE, tTop.mDesign, "the top centre IS the first pipe hole");
		// the first ring: the side-middles design 1 (the :127/:129/:131/:133 calls), corners design 0
		assertEquals(TileEntityLargeBoiler.DESIGN_PIPE_HOLE, ((MultiBlockPartBlockEntity) tLevel.getBlockEntity(tAnchor.offset(0, 1, -1))).mDesign, "the north hole");
		assertEquals(TileEntityLargeBoiler.DESIGN_PIPE_HOLE, ((MultiBlockPartBlockEntity) tLevel.getBlockEntity(tAnchor.offset(-1, 1, 0))).mDesign, "the west hole");
		assertEquals(TileEntityLargeBoiler.DESIGN_PIPE_HOLE, ((MultiBlockPartBlockEntity) tLevel.getBlockEntity(tAnchor.offset(1, 1, 0))).mDesign, "the east hole");
		assertEquals(TileEntityLargeBoiler.DESIGN_PIPE_HOLE, ((MultiBlockPartBlockEntity) tLevel.getBlockEntity(tAnchor.offset(0, 1, 1))).mDesign, "the south hole");
		assertEquals(0, ((MultiBlockPartBlockEntity) tLevel.getBlockEntity(tAnchor.offset(1, 1, 1))).mDesign, "the corners are NOT holes");
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_OUT, ((MultiBlockPartBlockEntity) tLevel.getBlockEntity(tAnchor.offset(0, 1, 1))).mMode);
		// the second ring: no holes at all
		assertEquals(0, ((MultiBlockPartBlockEntity) tLevel.getBlockEntity(tAnchor.offset(0, 2, -1))).mDesign, "the second ring carries no hole");
		assertEquals(0, ((MultiBlockPartBlockEntity) tLevel.getBlockEntity(tAnchor.offset(1, 2, -1))).mDesign);
	}
}
