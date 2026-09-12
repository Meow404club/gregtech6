/*
 * Offline tests for task p27-builder-wand-form-fix: the crucible's FORM SAMPLING must
 * cover the whole structure at EVERY controller facing. The regression: the crucible is
 * controller-anchored ("Main at Bottom-Center", upstream MultiTileEntityCrucible.java
 * :118-122 walks the walls straight off xCoord/yCoord/zCoord with no facing anywhere;
 * the :134-136 isInsideStructure is the controller box), but its checkStructure2 fed the
 * declared pattern walk the raw mFacing — and the walk resolves every cell as
 * {@code controller + cellOffset(facing, cell)} = {@code cell - OFF[facing]} (the
 * side-centred Coke-Oven convention). For every LIVE facing (HORIZONTAL_FACING 2..5 —
 * the only values a placed controller can carry) the whole check landed one block off
 * the machine: the wand scaffolded a displaced half-box, the far column of parts
 * silently refused the wand relay ({@code wandTarget} → isInsideStructure = false) and
 * no click sequence completed the machine the player could see — "the wand builds half
 * a multiblock". The fix rides {@link TileEntityBase10MultiBlockBase#patternWalkFacing()}
 * (the crucible walks at the zero-offset facing 0), so these tests pin:
 * <ol>
 * <li>the full wall box AROUND THE CONTROLLER forms at every facing 0..5 (the displaced
 *     walk failed this for all of 2..5 — the existing crucible suite only ever exercised
 *     facing 0, where OFF[0] is the zero vector and the displacement vanishes);</li>
 * <li>the walk is controller-anchored: {@code patternWalkFacing()} is 0 regardless of
 *     mFacing, and every walked cell satisfies isInsideStructure (the relay-reachability
 *     invariant the wand depends on);</li>
 * <li>the DISPLACED box is NOT the machine (pre-fix it formed — the RCON p26 chain's
 *     blind green: its stray true-box wall row was never declared, hence never judged);</li>
 * <li>the P28 ONE-CLICK builder-wand story (the user ruling 2026-09-12 deviating from
 *     the upstream nine-click window semantics — ADR 2026-09-12-p28-builder-wand-oneclick):
 *     ONE click on the controller scaffolds ALL 24 walls at the controller-anchored
 *     coordinates, forms, and consumes exactly 24 stock, at every live facing; a click on
 *     an already-formed crucible is an idempotent no-op (zero consumption), and a click
 *     with short stock leaves the world untouched and consumes nothing (the checker SET
 *     walk's transactional failure semantics). The relay
 *     ({@code GT6BuilderWandItem.scaffoldTarget} over a linked wall) stays pinned — the
 *     resolution face is upstream-faithful even though the placement is not.</li>
 * </ol>
 */
package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.items.tools.GT6BuilderWandItem;
import gregtech6.multiblock.GTMultiBlockPattern;

public class GTMultiBlockFacingIntegrityTest extends GTMultiBlocksOfflineTestBase {

	/** The live facings — HORIZONTAL_FACING's whole domain: a placed controller ALWAYS carries one of these. */
	static final byte[] LIVE_FACINGS = { 2, 3, 4, 5 };

	/** The controller cell for this suite — away from the shared C1/C2 arbitration fixtures. */
	static final BlockPos CRUCIBLE_POS = new BlockPos(200, 64, 200);

	static BlockEntityType<FacingCrucible> sFacingCrucibleType;

	/** The concrete test BE — the crucible over a vanilla-block BET, the wall bound to BRICKS. */
	public static final class FacingCrucible extends TileEntityCrucible {
		public FacingCrucible(BlockPos aPos, BlockState aState) {
			super(sFacingCrucibleType, aPos, aState);
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildFacingFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		BlockEntityType<FacingCrucible>[] tHolder = (BlockEntityType<FacingCrucible>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(FacingCrucible::new, Blocks.BRICKS).build(null);
		sFacingCrucibleType = tHolder[0];
	}

	// ------------------------------------------------------------------
	// the fixtures
	// ------------------------------------------------------------------

	private static SimpleContainer brickStock(int aCount) {
		return new SimpleContainer(new ItemStack(Blocks.BRICKS, aCount));
	}

	/** The part-BE factory the stub world runs on setBlock (the CheckerFormTest recipe). */
	private static void mountPartFactory(MultiBlockLevel aLevel) {
		aLevel.mBeFactory = (aPos, aState) -> aState.is(Blocks.BRICKS) ? sPartType.create(aPos, aState) : null;
	}

	/** Places the 24-wall ring set around the CONTROLLER (the upstream box, facing-independent). */
	private static MultiBlockLevel placeFullWallsAroundController(MultiBlockLevel aLevel) {
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) continue;
			for (int tY = 0; tY <= 2; tY++) placePart(aLevel, CRUCIBLE_POS.offset(tDX, tY, tDZ));
		}
		return aLevel;
	}

	/** The controller-anchored world cell of a declared (dx, y, dz) — the post-fix walk. */
	private static BlockPos anchoredCell(int aDX, int aY, int aDZ) {
		return CRUCIBLE_POS.offset(aDX, aY, aDZ);
	}

	// ------------------------------------------------------------------
	// ① the whole box forms at every facing
	// ------------------------------------------------------------------

	@Test
	public void fullWallSetFormsAtEveryFacing() {
		for (byte tFacing = 0; tFacing <= 5; tFacing++) {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			FacingCrucible tCrucible = placeController(tLevel, sFacingCrucibleType, CRUCIBLE_POS, tFacing);
			placeFullWallsAroundController(tLevel);
			tCrucible.onStructureChange();
			assertTrue(tCrucible.checkStructure(false),
					"the controller-anchored box forms at facing " + tFacing);
		}
	}

	/**
	 * The relay-reachability invariant: EVERY walked cell satisfies isInsideStructure at
	 * every live facing — the wand can click any wall of the structure and reach the
	 * controller (MultiBlockPartBlockEntity.wandTarget refuses outside the box). Pre-fix
	 * the nine cells of the displaced far column failed this per facing.
	 */
	@Test
	public void everyWalkedCellIsInsideTheStructureAtEveryLiveFacing() {
		for (byte tFacing : LIVE_FACINGS) {
			FacingCrucible tCrucible = sFacingCrucibleType.create(CRUCIBLE_POS, Blocks.BRICKS.defaultBlockState());
			tCrucible.mFacing = tFacing;
			GTMultiBlockPattern tPattern = tCrucible.getStructurePattern();
			byte tWalk = tCrucible.patternWalkFacing();
			for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
				int[] tOffset = tPattern.worldOffset(tWalk, tCell);
				BlockPos tWorld = CRUCIBLE_POS.offset(tOffset[0], tOffset[1], tOffset[2]);
				assertTrue(tCrucible.isInsideStructure(tWorld.getX(), tWorld.getY(), tWorld.getZ()),
						"cell " + tCell.x + "," + tCell.y + "," + tCell.z + " walks inside the machine box at facing " + tFacing);
			}
		}
	}

	/** The walk-facing pin: the crucible is controller-anchored at ANY mFacing; the side-centred machines keep mFacing. */
	@Test
	public void patternWalkFacingPins() {
		FacingCrucible tCrucible = sFacingCrucibleType.create(CRUCIBLE_POS, Blocks.BRICKS.defaultBlockState());
		tCrucible.mFacing = 5;
		assertEquals(0, tCrucible.patternWalkFacing(), "the crucible walks controller-anchored regardless of mFacing");
		TestCokeOven tOven = sCokeOvenType.create(C1, Blocks.BRICKS.defaultBlockState());
		tOven.mFacing = 2;
		assertEquals(2, tOven.patternWalkFacing(), "the side-centred default is mFacing (Coke Oven unchanged)");
	}

	// ------------------------------------------------------------------
	// ② the displaced box is NOT the machine
	// ------------------------------------------------------------------

	/**
	 * Walls standing at the DISPLACED box (the pre-fix walk: controller + cell −
	 * OFF[mFacing]) do not form the machine — the exact regression that slipped through
	 * the p26 RCON chain (its stray true-box row was outside the declared walk, hence
	 * unjudged, and the verdict read green over a machine one block off the controller).
	 */
	@Test
	public void displacedBoxDoesNotFormAtAnyLiveFacing() {
		for (byte tFacing : LIVE_FACINGS) {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			FacingCrucible tCrucible = placeController(tLevel, sFacingCrucibleType, CRUCIBLE_POS, tFacing);
			GTMultiBlockPattern tPattern = tCrucible.getStructurePattern();
			for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
				if (!tCell.forms()) continue;
				int[] tOffset = tPattern.worldOffset(tFacing, tCell); // the WRONG feed — the pre-fix walk
				BlockPos tWorld = CRUCIBLE_POS.offset(tOffset[0], tOffset[1], tOffset[2]);
				if (tWorld.equals(CRUCIBLE_POS)) continue; // the displaced ring cell landing on the controller passes via the self-cell arm — nobody overwrites the controller BE
				placePart(tLevel, tWorld);
			}
			tCrucible.onStructureChange();
			assertFalse(tCrucible.checkStructure(false),
					"the displaced box is not the machine at facing " + tFacing);
		}
	}

	// ------------------------------------------------------------------
	// ③ the P28 ONE-CLICK builder-wand story at every live facing
	// ------------------------------------------------------------------

	/**
	 * ONE click on the controller — the wand scaffolds ALL 24 walls at the
	 * controller-anchored coordinates (never displaced), forms, links every part and
	 * consumes exactly 24 stock, at every live facing. This is the P28 user ruling
	 * (2026-09-12): a single click forms the COMPLETE multiblock, replacing the upstream
	 * nine-click semantics the pre-P28 pin nailed here (the ±1 click window covered the
	 * y+0 and y+1 rings only and the y+2 ring took eight relay clicks — research
	 * research.p28-r-builder-wand-second-root confirmed that window is upstream-faithful,
	 * so the one-click form is a DECLARED deviation, ADR
	 * 2026-09-12-p28-builder-wand-oneclick, riding the checker's SET walk).
	 */
	@Test
	public void wandClickFormsTheWholeCrucibleAtEveryLiveFacing() {
		for (byte tFacing : LIVE_FACINGS) {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			mountPartFactory(tLevel);
			FacingCrucible tCrucible = placeController(tLevel, sFacingCrucibleType, CRUCIBLE_POS, tFacing);
			SimpleContainer tStock = brickStock(24); // exactly the shell (no self cell for the crucible)

			// THE one click — on the controller: the whole structure is the target (aClickedAt
			// rides only the resolution and the sound; the placement no longer narrows to its
			// ±1 neighbourhood)
			long tClick = GT6BuilderWandItem.builderWandScaffold(tCrucible, CRUCIBLE_POS, null, tStock, ItemStack.EMPTY);
			assertEquals(GT6BuilderWandItem.SCAFFOLD_TOOL_DAMAGE, tClick, "the dispatch returns the unconditional 10 units");
			assertEquals(0, tStock.getItem(0).getCount(), "every wall was paid for in ONE click at facing " + tFacing + " (24 stock, 24 cells)");
			assertTrue(tCrucible.checkStructure(false), "one click formed the crucible at facing " + tFacing);

			// the shell stands COMPLETE at the controller-anchored coordinates
			for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
				if (tDX == 0 && tDZ == 0) continue;
				for (int tY = 0; tY <= 2; tY++) {
					BlockPos tCell = anchoredCell(tDX, tY, tDZ);
					assertTrue(tLevel.getBlockState(tCell).is(Blocks.BRICKS),
							"the wall stands at the anchored cell " + tCell.toShortString() + " at facing " + tFacing);
					if (tLevel.getBlockEntity(tCell) instanceof MultiBlockPartBlockEntity tPart) {
						assertSame(tCrucible, tPart.mTarget, "the wall linked to THIS controller");
					}
				}
			}
		}
	}

	/**
	 * The idempotency pin: clicking an ALREADY-FORMED crucible is a no-op — the SET walk's
	 * beat-1 diagnosis short-circuits (zero side effects), nothing is consumed, the
	 * structure stays formed. Rides the relay face: the second click goes through a LINKED
	 * wall ({@code scaffoldTarget} resolves the same controller — the upstream part :261
	 * arm kept faithful by the P28 card).
	 */
	@Test
	public void wandClickOnFormedCrucibleIsAnIdempotentNoOp() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		FacingCrucible tCrucible = placeController(tLevel, sFacingCrucibleType, CRUCIBLE_POS, (byte) 3);
		SimpleContainer tStock = brickStock(24);
		GT6BuilderWandItem.builderWandScaffold(tCrucible, CRUCIBLE_POS, null, tStock, ItemStack.EMPTY);
		assertTrue(tCrucible.checkStructure(false), "the first click formed the crucible");
		tStock.addItem(new ItemStack(Blocks.BRICKS, 2)); // spare stock the no-op must NOT touch

		// the second click — through the linked-wall relay, the upstream-faithful resolution
		BlockPos tWall = anchoredCell(1, 1, -1);
		assertSame(tCrucible, GT6BuilderWandItem.scaffoldTarget(tLevel, tWall), "the linked wall relays to THIS controller");
		long tClick = GT6BuilderWandItem.builderWandScaffold(tCrucible, tWall, null, tStock, ItemStack.EMPTY);
		assertEquals(GT6BuilderWandItem.SCAFFOLD_TOOL_DAMAGE, tClick, "the dispatch still returns the unconditional 10 units");
		assertEquals(2, tStock.getItem(0).getCount(), "the formed structure consumed NOTHING (the diagnosis short-circuit)");
		assertTrue(tCrucible.checkStructure(false), "the crucible stays formed");
	}

	/**
	 * The failure-semantics pin (the SPEC ruling: "沿用 checker.form 现行为"): short stock
	 * is a hard failure BEFORE anything is placed or consumed — zero world writes, zero
	 * consumption, the crucible stays unformed (the RCON contract "insufficient stock ⇒
	 * not formed AND not consumed", inherited by the wand path verbatim).
	 */
	@Test
	public void wandClickWithShortStockPlacesAndConsumesNothing() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		FacingCrucible tCrucible = placeController(tLevel, sFacingCrucibleType, CRUCIBLE_POS, (byte) 2);
		SimpleContainer tStock = brickStock(10); // 10 of the 24 walls — short

		GT6BuilderWandItem.builderWandScaffold(tCrucible, CRUCIBLE_POS, null, tStock, ItemStack.EMPTY);
		assertEquals(10, tStock.getItem(0).getCount(), "nothing was consumed (the transactional refusal)");
		assertFalse(tCrucible.checkStructure(false), "the crucible did not form");
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) continue;
			for (int tY = 0; tY <= 2; tY++) {
				assertTrue(tLevel.getBlockState(anchoredCell(tDX, tY, tDZ)).isAir(),
						"no wall was placed at " + anchoredCell(tDX, tY, tDZ).toShortString() + " (zero world writes)");
			}
		}
	}
}
