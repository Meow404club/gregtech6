package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.multiblock.GTMultiBlockStructureChecker.FormedVerdict;

/**
 * The production switch + the equivalence pins (task p16-pattern-checker ③).
 *
 * <p><b>Coke Oven (the pilot).</b> checkStructure2 now walks the bound pattern through
 * the shared checker; these tests pin that the switch is behaviour-preserving against
 * the upstream :46-60 semantics — the greenfield miss list, the formed link, the
 * single-cell break, the wrong block, the fail-not-clear hollow (the foreign block
 * STAYS), the unloaded keep-last-verdict, and the wand two-pass semantics (the placing
 * pass consumes the stock and fails on the stale reference, the linking pass forms —
 * the Util quirk inherited, not re-coded).
 *
 * <p><b>Large Boiler (NOT switched — the ADR scoping).</b> The hand-written
 * checkStructure2 stays the server truth; the checker consumes its declaration-only
 * pattern and the FIVE arms pin the verdict equivalence: greenfield, single-cell
 * break, wrong block, unloaded, formed-then-broken. One arm, one
 * {@code handWrittenResult == checkerVerdict} assertion — the equivalence the ADR
 * demands before any future boiler switch (W3, out of scope here).
 */
public class TileEntityCokeOvenCheckerTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<CheckerBoiler> sBoilerType;
	static BlockEntityType<HeatTransmitterBlockEntity> sTransmitterType;

	/** The concrete test boiler — fixture blocks bound (the LargeBoilerPatternTest recipe). */
	public static final class CheckerBoiler extends TileEntityLargeBoiler {
		public CheckerBoiler(BlockPos aPos, BlockState aState) {
			super(sBoilerType, aPos, aState);
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
		@Override
		protected Block getTransmitterBlock() {
			return Blocks.STONE;
		}
	}

	/** The isLoaded-switchable level — the unloaded arms flip the flag. */
	static class LoadedSwitchLevel extends MultiBlockLevel {
		boolean mLoaded = true;

		@Override
		public boolean isLoaded(BlockPos aPos) {
			return mLoaded;
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildFixtures() {
		BlockEntityType<CheckerBoiler>[] tHolder = (BlockEntityType<CheckerBoiler>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(CheckerBoiler::new, Blocks.BRICKS, Blocks.STONE).build(null);
		sBoilerType = tHolder[0];
		BlockEntityType<HeatTransmitterBlockEntity>[] tTxHolder = (BlockEntityType<HeatTransmitterBlockEntity>[]) new BlockEntityType<?>[1];
		tTxHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new HeatTransmitterBlockEntity(tTxHolder[0], aPos, aState), Blocks.BRICKS, Blocks.STONE).build(null);
		sTransmitterType = tTxHolder[0];
	}

	// ------------------------------------------------------------------
	// shared plumbing
	// ------------------------------------------------------------------

	/** The part-BE factory keyed by block: BRICKS walls/bricks get the plain part BE, STONE the relaying transmitter. */
	private static BiFunction<BlockPos, BlockState, net.minecraft.world.level.block.entity.BlockEntity> partFactory() {
		return (aPos, aState) -> {
			if (aState.is(Blocks.BRICKS)) return sPartType.create(aPos, aState);
			if (aState.is(Blocks.STONE)) return sTransmitterType.create(aPos, aState);
			return null;
		};
	}

	/** The checker verdict straight from the controller's own binding and facing. */
	private static FormedVerdict check(TileEntityBase10MultiBlockBase aController) {
		return GTMultiBlockStructureChecker.check(aController, aController.mFacing, null, null, null);
	}

	// ------------------------------------------------------------------
	// the Coke Oven pilot
	// ------------------------------------------------------------------

	/** The oven cell for centre-relative (i,j,k) under facing north: controller + cellOffset(2,...) == (100+i, 64+j, 101+k). */
	private static BlockPos ovenCell(BlockPos aController, int aI, int aJ, int aK) {
		return aController.offset(aI, aJ, aK + 1);
	}

	/** Places the 25 brick cells (the 3x3x3 shell minus the centre and the controller's own cell). */
	private static int placeFrame(MultiBlockLevel aLevel, BlockPos aController) {
		int tPlaced = 0;
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			BlockPos tCell = ovenCell(aController, i, j, k);
			if (tCell.equals(aController)) continue; // the self-cell — the checker passes it without content
			aLevel.setBlock(tCell, Blocks.BRICKS.defaultBlockState(), 3);
			tPlaced++;
		}
		return tPlaced;
	}

	@Test
	public void ovenGreenfieldFailsWithTheUpstreamMissList() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = partFactory();
		TileEntityCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte)2);

		// the hand-written loop once failed 25 cells here (26 bricks, one self-cell pass);
		// the checker must produce the same verdict from the same pattern
		boolean tHandWritten = tOven.checkStructure2(null, null, null);
		assertFalse(tHandWritten, "the greenfield check fails");
		FormedVerdict tVerdict = check(tOven);
		assertFalse(tVerdict.formed, "the checker agrees");
		assertEquals(25, tVerdict.failedCells().size(), "the 25 miss cells (26 bricks - the self-cell)");
		GTMultiBlockStructureChecker.FailedCell tFirst = tVerdict.firstFailedCell();
		assertNotNull(tFirst);
		assertEquals(-1, tFirst.x); assertEquals(-1, tFirst.y); assertEquals(-1, tFirst.z);
		assertEquals(ovenCell(C1, -1, -1, -1), tFirst.pos, "the declaration-order first miss");
		assertEquals(0, tFirst.index, "cell (-1,-1,-1) is declared first");
	}

	@Test
	public void ovenFormedThenSingleCellBreakFailsExactlyThere() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = partFactory();
		TileEntityCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte)2);
		assertEquals(25, placeFrame(tLevel, C1), "the 25-cell frame");
		assertTrue(tOven.checkStructure(true), "the frame forms (and links)");

		FormedVerdict tVerdict = check(tOven);
		assertTrue(tVerdict.formed, "the checker agrees the formed structure: " + tVerdict.failedCells());

		// break ONE cell (the upstream breakBlock propagation — :177-181)
		BlockPos tBroken = ovenCell(C1, 1, 0, 1);
		tLevel.setBlock(tBroken, Blocks.AIR.defaultBlockState(), 3);
		tOven.onStructureChange();

		assertFalse(tOven.checkStructure2(null, null, null), "the hand-written verdict flips false");
		tVerdict = check(tOven);
		assertFalse(tVerdict.formed, "the checker flips false too");
		assertEquals(1, tVerdict.failedCells().size(), "exactly the broken cell");
		assertEquals(tBroken, tVerdict.firstFailedCell().pos, "the diagnostics point at the break");
	}

	@Test
	public void ovenWrongBlockFailsNotClears() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = partFactory();
		TileEntityCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte)2);
		placeFrame(tLevel, C1);
		BlockPos tWrong = ovenCell(C1, 0, 1, 0);
		tLevel.setBlock(tWrong, Blocks.STONE.defaultBlockState(), 3); // right cell, WRONG block

		assertFalse(tOven.checkStructure2(null, null, null), "a wrong block fails");
		FormedVerdict tVerdict = check(tOven);
		assertFalse(tVerdict.formed, "the checker agrees");
		assertEquals(1, tVerdict.failedCells().size(), "exactly the wrong cell");
		assertEquals(tWrong, tVerdict.firstFailedCell().pos, "the wrong cell is the report");
		assertEquals(Blocks.STONE.defaultBlockState().getBlock(), tLevel.getBlockState(tWrong).getBlock(),
				"the CHECK never writes: the wrong block is still standing");
	}

	@Test
	public void ovenHollowOccupiedIsFailNotClear() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = partFactory();
		TileEntityCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte)2);
		placeFrame(tLevel, C1);
		BlockPos tCentre = ovenCell(C1, 0, 0, 0);
		tLevel.setBlock(tCentre, Blocks.BRICKS.defaultBlockState(), 3); // the centre is FILLED

		assertFalse(tOven.checkStructure2(null, null, null), "a non-air centre fails (upstream :52)");
		FormedVerdict tVerdict = check(tOven);
		assertFalse(tVerdict.formed, "the checker agrees");
		assertEquals(1, tVerdict.failedCells().size(), "exactly the hollow");
		GTMultiBlockStructureChecker.FailedCell tFailure = tVerdict.firstFailedCell();
		assertNotNull(tFailure);
		assertEquals("hollow cell must be air (fail-not-clear)", tFailure.reason, "the fail-not-clear reason");
		assertEquals(tCentre, tFailure.pos, "the centre cell is the report");
		assertEquals(Blocks.BRICKS.defaultBlockState().getBlock(), tLevel.getBlockState(tCentre).getBlock(),
				"fail-NOT-CLEAR: the upstream setBlockToAir would clear an AIR cell only — a filled centre stays");
	}

	@Test
	public void ovenUnloadedKeepsTheLastVerdict() {
		LoadedSwitchLevel tLevel = new LoadedSwitchLevel();
		tLevel.mBeFactory = partFactory();
		TileEntityCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte)2);
		placeFrame(tLevel, C1);
		assertTrue(tOven.checkStructure(true), "formed while loaded");
		assertTrue(tOven.mStructureOkay, "the last verdict is formed");

		tLevel.mLoaded = false;
		// the upstream :59 form: an unloaded probe returns mStructureOkay regardless of the world
		assertTrue(tOven.checkStructure2(null, null, null), "unloaded keeps the last verdict (formed)");
		FormedVerdict tVerdict = check(tOven);
		assertTrue(tVerdict.unloaded, "the checker reports the unloaded case");
		assertEquals("chunk not loaded", tVerdict.firstFailedCell().reason, "the guard's reason");

		// now break the world BEHIND the unloaded guard — the verdict must STILL hold
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			tLevel.setBlock(ovenCell(C1, i, j, k), Blocks.AIR.defaultBlockState(), 3);
		}
		tOven.onStructureChange();
		assertTrue(tOven.checkStructure(false), "the last verdict STILL holds under the guard");
		tVerdict = check(tOven);
		assertTrue(tVerdict.unloaded, "the checker still reports unloaded, not a false verdict");
	}

	@Test
	public void ovenWandTwoPassSemanticsInherited() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = partFactory();
		TileEntityCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte)2);
		placeFrame(tLevel, C1);
		assertTrue(tOven.checkStructure(true), "the frame forms");

		// break ONE cell inside the wand's ±1 clicked-neighbourhood window (the
		// gt6multiblock wand chain's shape — the hole sits next to the controller)
		BlockPos tBroken = ovenCell(C1, 1, 0, 0);
		tLevel.setBlock(tBroken, Blocks.AIR.defaultBlockState(), 3);
		tOven.onStructureChange();

		// the wand triple: stocked inventory, null player (the canEdit auto-approve), the
		// clicked cell = the controller
		net.minecraft.world.SimpleContainer tInventory = new net.minecraft.world.SimpleContainer(new ItemStack(Blocks.BRICKS, 25));

		// the placing pass (upstream onToolClick2 :143): the brick lands in the world, but
		// the stale reference (Util — captured BEFORE the wand beat fills the cell) fails
		// THIS pass — the two-pass wand, inherited verbatim through checkAndSetTarget
		boolean tPlacingPass = tOven.checkStructure2(C1, null, tInventory);
		assertFalse(tPlacingPass, "the placing pass fails on the stale reference (the inherited quirk)");
		assertEquals(24, tInventory.getItem(0).getCount(), "exactly one brick consumed");

		// a FRESH walk (no stale reference) already sees the placed brick — formed
		assertTrue(check(tOven).formed, "the placing pass placed the brick: a fresh walk forms");

		// the linking pass (:144): forms and binds the last cell
		assertTrue(tOven.checkStructure(true), "the linking pass forms");
		int tLinked = 0;
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			BlockPos tCell = ovenCell(C1, i, j, k);
			if (tLevel.getBlockEntity(tCell) instanceof MultiBlockPartBlockEntity tPart && tPart.mTarget == tOven) tLinked++;
		}
		assertEquals(25, tLinked, "every brick linked (26 - the self-cell)");
	}

	// ------------------------------------------------------------------
	// the Large Boiler five-arm equivalence pins (NOT switched)
	// ------------------------------------------------------------------

	/** The boiler cell for anchor-relative (x,y,z) under facing north: controller + (x, y, z+1). */
	private static BlockPos boilerCell(BlockPos aController, int aX, int aY, int aZ) {
		return aController.offset(aX, aY, aZ + 1);
	}

	/** Places the 34 boiler part cells (the LargeBoilerPatternTest literal, anchor = front-same-layer). */
	private static void placeBoilerShell(MultiBlockLevel aLevel, BlockPos aController) {
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) aLevel.setBlock(boilerCell(aController, tDX, -1, tDZ), Blocks.STONE.defaultBlockState(), 3);
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			BlockPos tCell = boilerCell(aController, tDX, 0, tDZ);
			if (!tCell.equals(aController)) aLevel.setBlock(tCell, Blocks.BRICKS.defaultBlockState(), 3);
		}
		aLevel.setBlock(boilerCell(aController, 0, 2, 0), Blocks.BRICKS.defaultBlockState(), 3);
		for (int i = 1; i < 3; i++) {
			aLevel.setBlock(boilerCell(aController, -1, i, -1), Blocks.BRICKS.defaultBlockState(), 3);
			aLevel.setBlock(boilerCell(aController, 0, i, -1), Blocks.BRICKS.defaultBlockState(), 3);
			aLevel.setBlock(boilerCell(aController, 1, i, -1), Blocks.BRICKS.defaultBlockState(), 3);
			aLevel.setBlock(boilerCell(aController, -1, i, 0), Blocks.BRICKS.defaultBlockState(), 3);
			aLevel.setBlock(boilerCell(aController, 1, i, 0), Blocks.BRICKS.defaultBlockState(), 3);
			aLevel.setBlock(boilerCell(aController, -1, i, 1), Blocks.BRICKS.defaultBlockState(), 3);
			aLevel.setBlock(boilerCell(aController, 0, i, 1), Blocks.BRICKS.defaultBlockState(), 3);
			aLevel.setBlock(boilerCell(aController, 1, i, 1), Blocks.BRICKS.defaultBlockState(), 3);
		}
	}

	/** Arm plumbing: controller + verdict, asserting hand-written == checker. */
	private void assertEquivalence(TileEntityLargeBoiler aBoiler) {
		boolean tHandWritten = aBoiler.checkStructure2(null, null, null);
		FormedVerdict tVerdict = check(aBoiler);
		assertEquals(tHandWritten, tVerdict.formed,
				"checker == hand-written verdict; checker failures: " + tVerdict.failedCells());
	}

	@Test
	public void boilerArm1GreenfieldEquivalence() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = partFactory();
		TileEntityLargeBoiler tBoiler = placeController(tLevel, sBoilerType, C1, (byte)2);
		assertFalse(tBoiler.checkStructure2(null, null, null), "arm1: the hand-written greenfield verdict");
		FormedVerdict tVerdict = check(tBoiler);
		assertFalse(tVerdict.formed, "arm1: the checker agrees");
		assertEquals(34, tVerdict.failedCells().size(), "all 34 part cells miss (the declaration-only walk sees every miss)");
		assertEquals("declaration-only cell does not match", tVerdict.firstFailedCell().reason, "the declaration-only reason (the boiler pattern carries no forming expectation — the ADR scoping)");
	}

	@Test
	public void boilerArm2FormedThenSingleCellBreakEquivalence() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = partFactory();
		TileEntityLargeBoiler tBoiler = placeController(tLevel, sBoilerType, C1, (byte)2);
		placeBoilerShell(tLevel, C1);
		assertTrue(tBoiler.checkStructure(true), "arm2: the shell forms");
		assertTrue(check(tBoiler).formed, "arm2: the checker agrees formed (the equivalence prologue)");

		BlockPos tBroken = boilerCell(C1, 1, 0, 1); // a middle wall
		tLevel.setBlock(tBroken, Blocks.AIR.defaultBlockState(), 3);
		assertEquivalence(tBoiler);
		FormedVerdict tVerdict = check(tBoiler);
		assertEquals(1, tVerdict.failedCells().size(), "exactly the broken cell");
		assertEquals(tBroken, tVerdict.firstFailedCell().pos, "the diagnostics point at the break");
	}

	@Test
	public void boilerArm3WrongBlockEquivalence() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = partFactory();
		TileEntityLargeBoiler tBoiler = placeController(tLevel, sBoilerType, C1, (byte)2);
		placeBoilerShell(tLevel, C1);
		assertTrue(tBoiler.checkStructure(true), "arm3: the shell forms");
		BlockPos tWrong = boilerCell(C1, 0, 2, 0); // the top centre
		tLevel.setBlock(tWrong, Blocks.DIRT.defaultBlockState(), 3); // wrong block, no BE
		assertEquivalence(tBoiler);
		assertEquals(tWrong, check(tBoiler).firstFailedCell().pos, "the wrong cell is the report");
	}

	@Test
	public void boilerArm4UnloadedKeepsLastVerdictEquivalence() {
		LoadedSwitchLevel tLevel = new LoadedSwitchLevel();
		tLevel.mBeFactory = partFactory();
		TileEntityLargeBoiler tBoiler = placeController(tLevel, sBoilerType, C1, (byte)2);
		placeBoilerShell(tLevel, C1);
		assertTrue(tBoiler.checkStructure(true), "arm4: formed while loaded");
		assertTrue(tBoiler.mStructureOkay, "the last verdict is formed");

		tLevel.mLoaded = false;
		// the hand-written guard (:289-290) answers mStructureOkay — the checker's per-cell
		// superset must land on the same kept verdict via its unloaded flag
		assertTrue(tBoiler.checkStructure2(null, null, null), "arm4: the hand-written verdict holds");
		FormedVerdict tVerdict = check(tBoiler);
		assertTrue(tVerdict.unloaded, "arm4: the checker reports the guard");
		assertFalse(tVerdict.formed, "arm4: formed is meaningless under the guard");

		// the strong form: break the world behind the guard, the verdict STILL holds
		placeBoilerShell(tLevel, C1);
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			tLevel.setBlock(boilerCell(C1, tDX, -1, tDZ), Blocks.AIR.defaultBlockState(), 3);
			BlockPos tCell = boilerCell(C1, tDX, 0, tDZ);
			if (!tCell.equals(C1)) tLevel.setBlock(tCell, Blocks.AIR.defaultBlockState(), 3);
		}
		tLevel.setBlock(boilerCell(C1, 0, 2, 0), Blocks.AIR.defaultBlockState(), 3);
		for (int i = 1; i < 3; i++) for (int tDX = -1; tDX <= 1; tDX++) for (int tDZ = -1; tDZ <= 1; tDZ++) {
			if (tDX == 0 && tDZ == 0) continue;
			tLevel.setBlock(boilerCell(C1, tDX, i, tDZ), Blocks.AIR.defaultBlockState(), 3);
		}
		assertTrue(tBoiler.checkStructure2(null, null, null), "arm4: the last verdict STILL holds under the guard");
		assertTrue(check(tBoiler).unloaded, "arm4: the checker still reports the guard, not a false verdict");
	}

	@Test
	public void boilerArm5FormedThenFullyBrokenEquivalence() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = partFactory();
		TileEntityLargeBoiler tBoiler = placeController(tLevel, sBoilerType, C1, (byte)2);
		placeBoilerShell(tLevel, C1);
		assertTrue(tBoiler.checkStructure(true), "arm5: the shell forms");
		assertTrue(check(tBoiler).formed, "arm5: the checker agrees formed");

		// break EVERYTHING (34 parts + the hollow filled too — the harshest world state)
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			tLevel.setBlock(boilerCell(C1, tDX, -1, tDZ), Blocks.AIR.defaultBlockState(), 3);
			BlockPos tCell = boilerCell(C1, tDX, 0, tDZ);
			if (!tCell.equals(C1)) tLevel.setBlock(tCell, Blocks.AIR.defaultBlockState(), 3);
		}
		tLevel.setBlock(boilerCell(C1, 0, 2, 0), Blocks.AIR.defaultBlockState(), 3);
		for (int i = 1; i < 3; i++) for (int tDX = -1; tDX <= 1; tDX++) for (int tDZ = -1; tDZ <= 1; tDZ++) {
			if (tDX == 0 && tDZ == 0) continue;
			tLevel.setBlock(boilerCell(C1, tDX, i, tDZ), Blocks.AIR.defaultBlockState(), 3);
		}
		tLevel.setBlock(boilerCell(C1, 0, 1, 0), Blocks.BRICKS.defaultBlockState(), 3); // the hollow is FILLED
		assertEquivalence(tBoiler);
		FormedVerdict tVerdict = check(tBoiler);
		assertEquals(35, tVerdict.failedCells().size(), "34 missing parts + the filled hollow");
	}
}
