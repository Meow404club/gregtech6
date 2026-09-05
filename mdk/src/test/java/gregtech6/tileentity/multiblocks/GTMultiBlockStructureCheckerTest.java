package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.multiblock.GTMultiBlockStructureChecker.FormedVerdict;

/**
 * The shared structure checker (task p16-pattern-checker ②) over a generic
 * pattern-bound controller — the per-cell-kind semantics, the unloaded short-circuit
 * and the binding side effects. The Coke Oven production switch and the Large Boiler
 * five-arm equivalence pins live in {@link TileEntityCokeOvenCheckerTest}
 * (task ③).
 */
public class GTMultiBlockStructureCheckerTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<PatternController> sPatternControllerType;

	/** A controller whose whole structure face is a settable pattern (null = unbound). */
	static class PatternController extends TileEntityBase10MultiBlockBase {
		@Nullable
		GTMultiBlockPattern mPattern = null;

		PatternController(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "test_pattern_controller";
		}

		@Override
		@Nullable
		public GTMultiBlockPattern getStructurePattern() {
			return mPattern;
		}

		@Override
		public boolean isInsideStructure(int aX, int aY, int aZ) {
			return true;
		}
	}

	/**
	 * An isLoaded-switchable level — the unloaded arm flips the flag; the base stub is
	 * hardwired always-loaded (the subclass keeps the test file from touching the shared
	 * base's assertion surface).
	 */
	static class LoadedSwitchLevel extends MultiBlockLevel {
		boolean mLoaded = true;

		@Override
		public boolean isLoaded(BlockPos aPos) {
			return mLoaded;
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildCheckerFixtures() {
		BlockEntityType<PatternController>[] tHolder = (BlockEntityType<PatternController>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new PatternController(tHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		sPatternControllerType = tHolder[0];
	}

	// The generic layout, controller (100,64,100) facing north (2):
	//   cell A formingPart (0,0,1)  -> world (100,64,102)
	//   cell B formingPart (0,0,-1) -> world (100,64,100) = the controller's own cell
	//      (the self-cell arm — cellOffset(2, 0,0,-1) = (0,0,0))
	//   cell H hollow      (1,0,0)  -> world (101,64,101)
	private static final BlockPos CELL_A = new BlockPos(100, 64, 102);
	private static final BlockPos CELL_H = new BlockPos(101, 64, 101);

	private static PatternController patternController(MultiBlockLevel aLevel) {
		PatternController tController = placeController(aLevel, sPatternControllerType, C1, (byte)2);
		tController.mPattern = GTMultiBlockPattern.builder()
				.formingPart(0, 0, 1, Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0)
				.formingPart(0, 0, -1, Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0)
				.hollow(1, 0, 0, GTMultiBlockPattern.AIR)
				.build();
		return tController;
	}

	@Test
	public void unboundControllerFormsByDefinition() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		PatternController tController = placeController(tLevel, sPatternControllerType, C1, (byte)2);
		assertTrue(tController.mPattern == null, "fixture starts unbound");
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertTrue(tVerdict.formed, "no declaration, no expectation — the interface default");
		assertFalse(tVerdict.unloaded, "not an unloaded case");
		assertTrue(tVerdict.failedCells().isEmpty(), "nothing to report");
		assertNull(tVerdict.firstFailedCell(), "no first failure");
		assertEquals("none", tVerdict.describeFirstFailure(), "the diagnostic shape");
	}

	@Test
	public void formingCellsBindAndSelfCellPasses() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		PatternController tController = patternController(tLevel);
		placePart(tLevel, CELL_A); // cell B is the controller's own cell — no part BE there
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertTrue(tVerdict.formed, "formed: " + tVerdict.failedCells());
		assertTrue(tVerdict.failedCells().isEmpty(), "no failures");

		// the binding side effect rode the upstream path — mode/design written, target linked
		MultiBlockPartBlockEntity tPart = (MultiBlockPartBlockEntity) tLevel.getBlockEntity(CELL_A);
		assertNotNull(tPart, "the forming cell got its part BE through checkAndSetTarget");
		assertSame(tController, tPart.mTarget, "the part is claimed by THIS controller");
		assertEquals(MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, tPart.mMode, "the cell's usage mask is what got written");
		assertEquals(0, tPart.mDesign, "the cell's design index is what got written");
	}

	@Test
	public void missingPartCellFailsAtItsIndex() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		PatternController tController = patternController(tLevel);
		// cell A NOT placed — air there
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertFalse(tVerdict.formed, "a missing part fails");
		assertFalse(tVerdict.unloaded, "a loaded world is not the unloaded case");
		assertEquals(1, tVerdict.failedCells().size(), "exactly the missing cell");
		GTMultiBlockStructureChecker.FailedCell tFirst = tVerdict.firstFailedCell();
		assertNotNull(tFirst);
		assertEquals(0, tFirst.index, "cell A is the first declared cell");
		assertEquals(0, tFirst.x); assertEquals(0, tFirst.y); assertEquals(1, tFirst.z);
		assertEquals(CELL_A, tFirst.pos, "the world cell via cellOffset(facing)");
		assertEquals("part cell failed (missing, wrong block, or foreign claim)", tFirst.reason);
		assertTrue(tFirst.toString().startsWith("#0 (0,0,1)@100, 64, 102:"), "the compact RCON-embeddable form (toShortString spacing)");
	}

	@Test
	public void foreignClaimFails() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		PatternController tController = patternController(tLevel);
		MultiBlockPartBlockEntity tPart = placePart(tLevel, CELL_A);
		// another controller claims the cell first (the scripted controller is elsewhere,
		// its isInsideStructure answers true — the upstream arbitration predicate)
		TestController tRival = placeController(tLevel, sTestControllerType, new BlockPos(102, 64, 100), (byte)2);
		tPart.setTarget(tRival, 0, 0);
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertFalse(tVerdict.formed, "a foreign claim fails the check");
		assertEquals(1, tVerdict.failedCells().size(), "exactly the claimed cell");
		assertEquals(CELL_A, tVerdict.firstFailedCell().pos, "the claimed cell is the failure");
	}

	@Test
	public void hollowFailNotClear() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		PatternController tController = patternController(tLevel);
		placePart(tLevel, CELL_A);
		tLevel.setBlock(CELL_H, Blocks.STONE.defaultBlockState(), 3); // the hollow is FILLED
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertFalse(tVerdict.formed, "a filled hollow fails the check");
		assertEquals(1, tVerdict.failedCells().size(), "exactly the hollow");
		GTMultiBlockStructureChecker.FailedCell tFailure = tVerdict.firstFailedCell();
		assertNotNull(tFailure);
		assertEquals(2, tFailure.index, "the hollow is the last declared cell");
		assertEquals("hollow cell must be air (fail-not-clear)", tFailure.reason, "fail, not clear");
		assertEquals(Blocks.STONE.defaultBlockState().getBlock(), tLevel.getBlockState(CELL_H).getBlock(),
				"the CHECK never writes: the foreign block is still standing");
	}

	@Test
	public void hollowAirPassesWithoutWrite() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		PatternController tController = patternController(tLevel);
		placePart(tLevel, CELL_A);
		// CELL_H stays air (the stub default)
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertTrue(tVerdict.formed, "formed: " + tVerdict.failedCells());
		assertFalse(tLevel.mBlockEntities.containsKey(CELL_H), "the hollow cell never gained anything");
	}

	@Test
	public void declarationOnlyCellJudgesByPredicate() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		PatternController tController = placeController(tLevel, sPatternControllerType, C1, (byte)2);
		// a declaration-only pattern (the P12 calibre — NO forming expectation anywhere):
		// part (0,0,1) judges STONE, hollow (1,0,0)
		tController.mPattern = GTMultiBlockPattern.builder()
				.part(0, 0, 1, GTMultiBlockPattern.is(Blocks.STONE))
				.hollow(1, 0, 0, GTMultiBlockPattern.AIR)
				.build();
		tLevel.setBlock(new BlockPos(100, 64, 102), Blocks.STONE.defaultBlockState(), 3);
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertTrue(tVerdict.formed, "formed: " + tVerdict.failedCells());
		assertFalse(tLevel.mBlockEntities.containsKey(new BlockPos(100, 64, 102)),
				"the declaration-only walk binds nothing (the P12 display calibre)");

		tLevel.setBlock(new BlockPos(100, 64, 102), Blocks.DIRT.defaultBlockState(), 3);
		tVerdict = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertFalse(tVerdict.formed, "a wrong block fails the predicate");
		assertEquals("declaration-only cell does not match", tVerdict.firstFailedCell().reason, "the dedicated reason");
	}

	@Test
	public void unloadedShortCircuitsBeforeJudgementOrBinding() {
		LoadedSwitchLevel tLevel = new LoadedSwitchLevel();
		PatternController tController = patternController(tLevel);
		// NOTHING placed — a judgement walk would fail; the unloaded guard must short-circuit first
		tLevel.mLoaded = false;
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertTrue(tVerdict.unloaded, "the guard fired");
		assertFalse(tVerdict.formed, "formed carries no meaning under the guard");
		assertTrue(tVerdict.failedCells().size() >= 1, "the not-loaded cells are listed");
		assertEquals("chunk not loaded", tVerdict.firstFailedCell().reason, "the guard's reason");
		assertFalse(tLevel.mBlockEntities.containsKey(CELL_A), "no binding happened past the guard");
	}

	@Test
	public void facingRotatesTheWalk() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		PatternController tController = placeController(tLevel, sPatternControllerType, C1, (byte)3); // SOUTH
		tController.mPattern = GTMultiBlockPattern.builder()
				.formingPart(0, 0, 1, Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0)
				.build();
		// facing south (3): cellOffset(3, 0,0,1) = (0,0,1-1) = (0,0,0) -> the controller cell itself
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertTrue(tVerdict.formed, "the only cell is the self-cell under facing south: " + tVerdict.failedCells());
		assertSame(tController, tLevel.getBlockEntity(C1), "the controller BE is untouched by its own cell");
	}
}
