package gregtech6.tileentity.multiblocks;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The controller state-machine tests (task p4-multiblock-framework acceptance ①): the
 * checkStructure template semantics (TileEntityBase10MultiBlockBase :177-185), the trigger
 * wiring (onTickFirst :112-115 + the 600-tick poll :121-124) and the facing arithmetic.
 */
public class TileEntityBase10MultiBlockBaseTest extends GTMultiBlocksOfflineTestBase {

	@Test
	void checkStructureTemplateFlipsOnDisagreementAndConsumesTheFlag() {
		TestController tController = sTestControllerType.create(C1, net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState());
		assertFalse(tController.mStructureOkay);

		// forced first check, result false == cached false -> no flip (upstream :179 flip only on disagreement)
		tController.mResults.push(false);
		assertFalse(tController.checkStructure(true));
		assertFalse(tController.mStructureOkay);
		assertEquals(1, tController.mCalls);
		assertFalse(tController.mStructureChanged, "the flag is consumed either way (:183)");

		// changed flag + result true -> flip to formed
		tController.onStructureChange();
		assertTrue(tController.mStructureChanged);
		tController.mResults.push(true);
		assertTrue(tController.checkStructure(false));
		assertTrue(tController.mStructureOkay);
		assertEquals(2, tController.mCalls);

		// no flag, no force -> the cheap cached answer, NO recheck (:179 short-circuit)
		assertTrue(tController.checkStructure(false));
		assertEquals(2, tController.mCalls, "the recheck must not run without flag or force");

		// forced recheck, result false -> flip back to unformed
		tController.mResults.push(false);
		assertFalse(tController.checkStructure(true));
		assertFalse(tController.mStructureOkay);
		assertEquals(3, tController.mCalls);
	}

	@Test
	void clientSideCheckReturnsTheCachedVerdict() {
		// upstream :178 — client side never rechecks; without a level the 01Root isServerSide()
		// resolves to the offline default (server), so this asserts the server path ran at all.
		TestController tController = sTestControllerType.create(C1, net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState());
		tController.mDefaultResult = true;
		assertTrue(tController.checkStructure(true));
		assertTrue(tController.mStructureOkay);
	}

	@Test
	void tickFirstForcesCheckAndPollTriggersAtTimerFive() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestController tController = placeController(tLevel, sTestControllerType, C1, (byte) 2);
		tController.mDefaultResult = false;

		tController.updateEntity(); // mTimer 0 -> onTickFirst forced check (:112-115), mTimer -> 1
		assertEquals(1, tController.mCalls);
		for (int i = 0; i < 3; i++) tController.updateEntity(); // mTimer 2..4 — no trigger
		assertEquals(1, tController.mCalls);

		tController.updateEntity(); // mTimer 5 -> the poll (:121-124): cheap check, then the forced fallback
		assertEquals(2, tController.mCalls, "the not-okay poll must force the recheck");
		assertEquals(5, tController.getTimer());
	}

	@Test
	void structureCenterSitsBehindTheFacing() {
		// getOffsetXN/YN/ZN arithmetic (CokeOven :47/:77): the 3x3x3 core is BEHIND the facing
		MultiBlockLevel tLevel = new MultiBlockLevel();
		BlockPos[][] tCases = {
				// facing (byte), centre, an outside probe
				{ new BlockPos(100, 64, 100), new BlockPos(100, 64, 101), new BlockPos(100, 64, 104) }, // north
				{ new BlockPos(100, 64, 100), new BlockPos(100, 64, 99), new BlockPos(100, 64, 96) },   // south
				{ new BlockPos(100, 64, 100), new BlockPos(101, 64, 100), new BlockPos(104, 64, 100) }, // west
				{ new BlockPos(100, 64, 100), new BlockPos(99, 64, 100), new BlockPos(96, 64, 100) },   // east
		};
		byte[] tFacings = {2, 3, 4, 5};
		for (int i = 0; i < tFacings.length; i++) {
			TestCokeOven tOven = placeController(tLevel, sCokeOvenType, tCases[i][0], tFacings[i]);
			assertTrue(tOven.isInsideStructure(tCases[i][1].getX(), tCases[i][1].getY(), tCases[i][1].getZ()),
					"centre behind facing " + tFacings[i]);
			assertTrue(tOven.isInsideStructure(tCases[i][0].getX(), tCases[i][0].getY(), tCases[i][0].getZ()),
					"the controller cell itself is inside its box");
			assertFalse(tOven.isInsideStructure(tCases[i][2].getX(), tCases[i][2].getY(), tCases[i][2].getZ()),
					"probe beyond the 3x3x3 for facing " + tFacings[i]);
		}
	}

	@Test
	void facingFlipFlagsTheStructureForRecheck() {
		// upstream :187 onFacingChange -> onStructureChange
		TestController tController = sTestControllerType.create(C1, net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState());
		tController.mStructureChanged = false;
		tController.setFacing((byte) 3);
		assertEquals(3, tController.mFacing);
		assertTrue(tController.mStructureChanged, "the facing flip must arm the recheck");
	}

	@Test
	void placementFacesThePlacerNotTheView() {
		// task p27-cokeoven-facing-fix — the p27 user scene pin: the placement mapping is
		// the VIEW OPPOSITE (the GT6 front towards the placer; upstream UT.java:1751 over
		// CS.java:639 COMPASS_DIRECTIONS {NORTH,EAST,SOUTH,WEST}: yaw 180 (view north)
		// yields SIDE_SOUTH; the vanilla furnace getHorizontalDirection().getOpposite()
		// idiom). GT6 side order == Direction 3D data (2 north / 3 south / 4 west / 5 east).
		assertEquals(3, TileEntityBase10MultiBlockBase.placementFacing(net.minecraft.core.Direction.NORTH), "view north (placer south) -> front south");
		assertEquals(2, TileEntityBase10MultiBlockBase.placementFacing(net.minecraft.core.Direction.SOUTH), "view south (placer north) -> front north");
		assertEquals(5, TileEntityBase10MultiBlockBase.placementFacing(net.minecraft.core.Direction.WEST),  "view west (placer east)  -> front east");
		assertEquals(4, TileEntityBase10MultiBlockBase.placementFacing(net.minecraft.core.Direction.EAST),  "view east (placer west)  -> front west");

		// the BE write path: the front lands OPPOSITE the view, and a front-facing placer
		// gets the structure BEHIND the controller (away from them — centre = pos - OFF[front]).
		MultiBlockLevel tLevel = new MultiBlockLevel();
		BlockPos tPos = new BlockPos(200, 64, 200);
		TestCokeOven tOven = placeController(tLevel, sCokeOvenType, tPos, (byte) 2);
		tOven.setFacingFromView(net.minecraft.core.Direction.EAST); // the placer at west looking east
		assertEquals(4, tOven.mFacing, "front west (towards the placer at west)");
		assertTrue(tOven.isInsideStructure(201, 64, 200), "the core sits EAST — behind the front, away from the placer");
		assertFalse(tOven.isInsideStructure(196, 64, 200), "nothing on the placer's side of the controller");
	}
}
