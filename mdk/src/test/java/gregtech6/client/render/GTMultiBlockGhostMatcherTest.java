package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase.MultiBlockLevel;

/**
 * The green/red classifier matrix (task p12-ghost-render-match, offline): the
 * {@link GTMultiBlockGhostMatcher} three-valued semantics over the declared Coke Oven
 * shape (26 brick cells + the hollow centre, the TileEntityCokeOven binding with the
 * fixture part block BRICKS) against a stub Level — ① the all-green matrix over all
 * four facings (the POC 108-cell idiom: 4 facings x 27 cells, every verdict asserted);
 * ② the red family — one missing brick, one stone-substituted brick, the occupied
 * centre; ③ the SKIP family — the air centre (world air draws nothing); ④ the
 * unloaded-chunk read — VOID_AIR is missing (RED) for parts and air (SKIP) for the
 * hollow centre (VOID_AIR is an AirBlock, vanilla Blocks.java:5832, the
 * ClientChunkCache :75 read); ⑤ the predicate-level equality calibre — a same-block
 * different-property state still matches (the upstream checkAndSetTarget part-id
 * calibre, no full-state equality); ⑥ pattern-undeclared world blocks are never
 * judged.
 *
 * <p>Nothing here touches a real GL surface or a real world: the Level double is the
 * multiblock suite's {@link MultiBlockLevel} (a block-state map with AIR defaults).
 * The matcher is a pure function — no static mutable state is exercised or needed.
 */
public class GTMultiBlockGhostMatcherTest extends GTOfflineRenderTestBase {

	// the GT6 side-offset tables, test-local mirror (TileEntityBase01Root.java:174-176;
	// side order DOWN UP NORTH SOUTH WEST EAST = vanilla get3DDataValue) — the
	// specification the verdict positions are computed against, independent of the
	// pattern code (the POC test idiom)
	private static final int[] OFF_X = { 0, 0, 0, 0, -1, 1 };
	private static final int[] OFF_Y = { 0, 1, 0, 0, 0, 0 };
	private static final int[] OFF_Z = { 0, 0, -1, 1, 0, 0 };

	private static final BlockPos CONTROLLER = new BlockPos(100, 64, 100);
	private static final byte[] FACINGS = { 2, 3, 4, 5 };

	/** The declared Coke Oven shape — the TileEntityCokeOven binding, fixture part block BRICKS. */
	private static GTMultiBlockPattern cokeOvenPattern() {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			tBuilder.part(i, j, k, GTMultiBlockPattern.is(Blocks.BRICKS));
		}
		tBuilder.hollow(0, 0, 0, GTMultiBlockPattern.AIR);
		return tBuilder.build();
	}

	/** The controller-relative world position of canonical cell (i, j, k) — controller - OFF[facing] + (i, j, k). */
	private static BlockPos worldPos(byte aFacing, int aI, int aJ, int aK) {
		return CONTROLLER.offset(aI - OFF_X[aFacing], aJ - OFF_Y[aFacing], aK - OFF_Z[aFacing]);
	}

	private static void fillBricks(MultiBlockLevel aLevel, byte aFacing) {
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			aLevel.mStates.put(worldPos(aFacing, i, j, k), Blocks.BRICKS.defaultBlockState());
		}
	}

	/** ① the all-green matrix — every brick cell GREEN, the air centre SKIP, over all four facings (the POC 108-cell idiom). */
	@Test
	public void allGreenWhenFullyBuiltFourFacings() {
		GTMultiBlockPattern tPattern = cokeOvenPattern();
		int tGreens = 0, tSkips = 0;
		for (byte tFacing : FACINGS) {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			fillBricks(tLevel, tFacing); // the centre stays AIR
			List<GTMultiBlockGhostMatcher.Verdict> tVerdicts =
					GTMultiBlockGhostMatcher.classify(tPattern, tLevel, CONTROLLER, tFacing);
			assertEquals(27, tVerdicts.size(), "one verdict per declared cell, facing " + tFacing);
			for (int tIndex = 0; tIndex < 26; tIndex++) {
				assertEquals(GTMultiBlockGhostMatcher.Verdict.GREEN, tVerdicts.get(tIndex),
						"brick cell " + tIndex + " GREEN, facing " + tFacing);
				tGreens++;
			}
			assertEquals(GTMultiBlockGhostMatcher.Verdict.SKIP, tVerdicts.get(26),
					"the air centre draws nothing, facing " + tFacing);
			tSkips++;
		}
		assertEquals(104, tGreens, "26 bricks x four facings all green");
		assertEquals(4, tSkips, "one hollow centre per facing, all skipped");
	}

	/** ②a one missing brick → exactly that cell RED, everything else unchanged. */
	@Test
	public void missingBrickIsRed() {
		GTMultiBlockPattern tPattern = cokeOvenPattern();
		byte tFacing = 2; // NORTH — the shell sits at +Z
		MultiBlockLevel tLevel = new MultiBlockLevel();
		fillBricks(tLevel, tFacing);
		BlockPos tGap = worldPos(tFacing, 0, -1, 0); // the bottom-centre brick
		tLevel.mStates.remove(tGap); // world AIR — the missing semantics

		List<GTMultiBlockGhostMatcher.Verdict> tVerdicts =
				GTMultiBlockGhostMatcher.classify(tPattern, tLevel, CONTROLLER, tFacing);
		for (int tIndex = 0; tIndex < 26; tIndex++) {
			GTMultiBlockPattern.Cell tCell = tPattern.cells().get(tIndex);
			boolean tIsGap = tCell.x == 0 && tCell.y == -1 && tCell.z == 0;
			assertEquals(tIsGap ? GTMultiBlockGhostMatcher.Verdict.RED : GTMultiBlockGhostMatcher.Verdict.GREEN,
					tVerdicts.get(tIndex), "cell " + tIndex + " verdict");
		}
		assertEquals(GTMultiBlockGhostMatcher.Verdict.SKIP, tVerdicts.get(26), "the centre still skips");
	}

	/** ②b a wrong block (stone substituting a brick) → exactly that cell RED — missing and wrong merge. */
	@Test
	public void wrongBlockIsRed() {
		GTMultiBlockPattern tPattern = cokeOvenPattern();
		byte tFacing = 2;
		MultiBlockLevel tLevel = new MultiBlockLevel();
		fillBricks(tLevel, tFacing);
		tLevel.mStates.put(worldPos(tFacing, 1, 0, 1), Blocks.STONE.defaultBlockState());

		List<GTMultiBlockGhostMatcher.Verdict> tVerdicts =
				GTMultiBlockGhostMatcher.classify(tPattern, tLevel, CONTROLLER, tFacing);
		for (int tIndex = 0; tIndex < 26; tIndex++) {
			GTMultiBlockPattern.Cell tCell = tPattern.cells().get(tIndex);
			boolean tIsWrong = tCell.x == 1 && tCell.y == 0 && tCell.z == 1;
			assertEquals(tIsWrong ? GTMultiBlockGhostMatcher.Verdict.RED : GTMultiBlockGhostMatcher.Verdict.GREEN,
					tVerdicts.get(tIndex), "cell " + tIndex + " verdict");
		}
		assertEquals(GTMultiBlockGhostMatcher.Verdict.SKIP, tVerdicts.get(26), "the centre still skips");
	}

	/** ②c/③ the centre pair — a non-air centre is RED (the upstream check-failure rule), the air centre SKIPs. */
	@Test
	public void occupiedCentreIsRedAndAirCentreSkips() {
		GTMultiBlockPattern tPattern = cokeOvenPattern();
		byte tFacing = 2;
		MultiBlockLevel tLevel = new MultiBlockLevel();
		fillBricks(tLevel, tFacing);
		tLevel.mStates.put(worldPos(tFacing, 0, 0, 0), Blocks.BRICKS.defaultBlockState()); // centre occupied

		assertEquals(GTMultiBlockGhostMatcher.Verdict.RED,
				GTMultiBlockGhostMatcher.classify(tPattern, tLevel, CONTROLLER, tFacing).get(26),
				"a non-air centre is a check failure upstream — the ghost must paint it red");

		tLevel.mStates.remove(worldPos(tFacing, 0, 0, 0)); // centre air again
		assertEquals(GTMultiBlockGhostMatcher.Verdict.SKIP,
				GTMultiBlockGhostMatcher.classify(tPattern, tLevel, CONTROLLER, tFacing).get(26),
				"the air centre draws nothing");
	}

	/** ④ the unloaded-chunk read — VOID_AIR classifies missing (RED) for parts, air (SKIP) for the hollow centre. */
	@Test
	public void voidAirReadsAsMissingForPartsAndAirForHollow() {
		GTMultiBlockPattern tPattern = cokeOvenPattern();
		byte tFacing = 2;
		MultiBlockLevel tLevel = new MultiBlockLevel();
		// the whole shell reads VOID_AIR — the ungenerated-chunk readback (ClientChunkCache :75)
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			tLevel.mStates.put(worldPos(tFacing, i, j, k), Blocks.VOID_AIR.defaultBlockState());
		}
		List<GTMultiBlockGhostMatcher.Verdict> tVerdicts =
				GTMultiBlockGhostMatcher.classify(tPattern, tLevel, CONTROLLER, tFacing);
		for (int tIndex = 0; tIndex < 26; tIndex++) {
			assertEquals(GTMultiBlockGhostMatcher.Verdict.RED, tVerdicts.get(tIndex),
					"brick cell " + tIndex + " reads missing over VOID_AIR");
		}
		assertEquals(GTMultiBlockGhostMatcher.Verdict.SKIP, tVerdicts.get(26),
				"the hollow centre's AIR predicate matches VOID_AIR (an AirBlock) — skip");
	}

	/**
	 * ⑤ the predicate-level equality calibre — a same-block different-property state
	 * still matches (the checkAndSetTarget part-id calibre, not full-state equality);
	 * a different block does not.
	 */
	@Test
	public void predicateEqualityIgnoresBlockStateProperties() {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		tBuilder.part(0, 0, 1, GTMultiBlockPattern.is(Blocks.OAK_STAIRS));
		GTMultiBlockPattern tPattern = tBuilder.build();
		MultiBlockLevel tLevel = new MultiBlockLevel();
		BlockPos tPos = CONTROLLER.offset(0, 0, 1);

		BlockState tDefault = Blocks.OAK_STAIRS.defaultBlockState(); // facing=north
		tLevel.mStates.put(tPos, tDefault);
		assertEquals(GTMultiBlockGhostMatcher.Verdict.GREEN,
				GTMultiBlockGhostMatcher.classifyCell(tPattern.cells().get(0), tLevel, tPos),
				"the default state matches");

		tLevel.mStates.put(tPos, tDefault.setValue(StairBlock.FACING, Direction.EAST)); // same block, other property
		assertEquals(GTMultiBlockGhostMatcher.Verdict.GREEN,
				GTMultiBlockGhostMatcher.classifyCell(tPattern.cells().get(0), tLevel, tPos),
				"the property-flipped same-block state still matches — predicate-level, not full-state");

		tLevel.mStates.put(tPos, Blocks.COBBLESTONE_STAIRS.defaultBlockState());
		assertEquals(GTMultiBlockGhostMatcher.Verdict.RED,
				GTMultiBlockGhostMatcher.classifyCell(tPattern.cells().get(0), tLevel, tPos),
				"a different block does not match");
	}

	/** ⑥ pattern-undeclared world blocks are never judged — the verdict list spans exactly the declared cells. */
	@Test
	public void undeclaredWorldBlocksNeverJudged() {
		GTMultiBlockPattern tPattern = cokeOvenPattern();
		byte tFacing = 2;
		MultiBlockLevel tLevel = new MultiBlockLevel();
		fillBricks(tLevel, tFacing);
		// a wall of junk one cell outside the shell — none of it may flip any verdict
		for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			tLevel.mStates.put(worldPos(tFacing, 2, j, k), Blocks.STONE.defaultBlockState());
		}
		List<GTMultiBlockGhostMatcher.Verdict> tVerdicts =
				GTMultiBlockGhostMatcher.classify(tPattern, tLevel, CONTROLLER, tFacing);
		assertEquals(27, tVerdicts.size(), "exactly the declared cells — nothing extra judged");
		for (int tIndex = 0; tIndex < 26; tIndex++) {
			assertEquals(GTMultiBlockGhostMatcher.Verdict.GREEN, tVerdicts.get(tIndex),
					"cell " + tIndex + " unaffected by undeclared neighbours");
		}
		assertEquals(GTMultiBlockGhostMatcher.Verdict.SKIP, tVerdicts.get(26));
	}
}
