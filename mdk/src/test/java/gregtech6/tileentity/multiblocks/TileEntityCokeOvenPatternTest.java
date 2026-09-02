package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.multiblock.GTMultiBlockPattern;

/**
 * The Coke Oven pattern binding (task p12-ghost-pattern-api acceptance — offline):
 * ① the bound pattern is the LITERAL 26+1 expectation — 26 brick cells in the upstream
 * checkStructure2 loop order (TileEntityCokeOven.java:97-111, {@code i} outer / {@code j}
 * middle / {@code k} inner) plus the hollow air centre appended (:52), exactly the table
 * the POC renderer hardcoded (GTMultiBlockPreviewRenderer PATTERN_CELLS, task
 * p10-ghost-preview-poc) — this test is the byte-for-byte pin that the lift to the
 * declarative API changed nothing; ② the pattern's brick judgement rides the
 * {@link TileEntityCokeOven#getPartBlock()} hook (BRICKS on the offline fixture) and the
 * centre judgement is AIR; ③ the bounds fold to the 3x3x3 shell; ④ the interface default
 * is null (an unbound controller declares nothing); ⑤ the binding is lazy-stable and
 * read-only — checkStructure2 is never touched by it (its behavioural spec stays in
 * TileEntityBase10MultiBlockBaseTest, which must keep passing unchanged).
 */
public class TileEntityCokeOvenPatternTest extends GTMultiBlocksOfflineTestBase {

	/** The literal upstream loop order (i outer, j middle, k inner), (0,0,0) excluded. */
	private static final int[][] LITERAL_26 = {
			{ -1, -1, -1 }, { -1, -1, 0 }, { -1, -1, 1 },
			{ -1, 0, -1 }, { -1, 0, 0 }, { -1, 0, 1 },
			{ -1, 1, -1 }, { -1, 1, 0 }, { -1, 1, 1 },
			{ 0, -1, -1 }, { 0, -1, 0 }, { 0, -1, 1 },
			{ 0, 0, -1 }, { 0, 0, 1 },
			{ 0, 1, -1 }, { 0, 1, 0 }, { 0, 1, 1 },
			{ 1, -1, -1 }, { 1, -1, 0 }, { 1, -1, 1 },
			{ 1, 0, -1 }, { 1, 0, 0 }, { 1, 0, 1 },
			{ 1, 1, -1 }, { 1, 1, 0 }, { 1, 1, 1 }
	};

	@Test
	public void boundPatternIsTheLiteralUpstreamLoopPlusHollowCentre() {
		TileEntityCokeOven tOven = sCokeOvenType.create(C1, Blocks.BRICKS.defaultBlockState());
		GTMultiBlockPattern tPattern = tOven.getStructurePattern();
		assertNotNull(tPattern, "the coke oven declares a pattern");

		var tCells = tPattern.cells();
		assertEquals(27, tCells.size(), "26 bricks + the hollow centre");

		BlockState tBricks = Blocks.BRICKS.defaultBlockState();
		BlockState tAir = Blocks.AIR.defaultBlockState();
		int tAssertions = 0;
		for (int tCell = 0; tCell < 26; tCell++) {
			GTMultiBlockPattern.Cell tBrickCell = tCells.get(tCell);
			assertEquals(LITERAL_26[tCell][0], tBrickCell.x, "cell " + tCell + " x — literal transcription, upstream loop order");
			assertEquals(LITERAL_26[tCell][1], tBrickCell.y, "cell " + tCell + " y");
			assertEquals(LITERAL_26[tCell][2], tBrickCell.z, "cell " + tCell + " z");
			tAssertions += 3;
			assertFalse(tBrickCell.isHollow(), "cell " + tCell + " is a structural part");
			tAssertions++;
			assertTrue(tBrickCell.matches(tBricks), "cell " + tCell + " judges the part block (the getPartBlock hook)");
			tAssertions++;
			assertFalse(tBrickCell.matches(tAir), "cell " + tCell + " judges air a failure");
			tAssertions++;
		}
		// the centre: appended LAST (the POC draw order — 26 bricks, then the grey marker)
		GTMultiBlockPattern.Cell tCentre = tCells.get(26);
		assertEquals(0, tCentre.x); assertEquals(0, tCentre.y); assertEquals(0, tCentre.z);
		tAssertions += 3;
		assertTrue(tCentre.isHollow(), "the centre is the keep-this-hollow marker");
		tAssertions++;
		assertTrue(tCentre.matches(tAir), "the centre judgement is AIR (upstream :52 — non-air centre fails)");
		tAssertions++;
		assertFalse(tCentre.matches(tBricks), "the centre rejects a filled cell");
		tAssertions++;
		assertEquals(162, tAssertions, "the full literal pin: 26x6 + centre x6");
	}

	@Test
	public void boundsFoldToTheShell() {
		TileEntityCokeOven tOven = sCokeOvenType.create(C1, Blocks.BRICKS.defaultBlockState());
		GTMultiBlockPattern tPattern = tOven.getStructurePattern();
		assertEquals(-1, tPattern.minX()); assertEquals(1, tPattern.maxX());
		assertEquals(-1, tPattern.minY()); assertEquals(1, tPattern.maxY());
		assertEquals(-1, tPattern.minZ()); assertEquals(1, tPattern.maxZ());
	}

	@Test
	public void bindingIsLazyStable() {
		TileEntityCokeOven tOven = sCokeOvenType.create(C1, Blocks.BRICKS.defaultBlockState());
		assertSame(tOven.getStructurePattern(), tOven.getStructurePattern(),
				"the lazy cache returns the same immutable instance");
	}

	@Test
	public void interfaceDefaultIsNull() {
		TileEntityBase10MultiBlockBase tController = sTestControllerType.create(C1, Blocks.BRICKS.defaultBlockState());
		assertTrue(tController instanceof ITileEntityMultiBlockController, "the base implements the controller interface");
		assertEquals(null, ((ITileEntityMultiBlockController) tController).getStructurePattern(),
				"an unbound controller declares no pattern (the existence-probe seam)");
	}
}
