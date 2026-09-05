package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.multiblock.GTMultiBlockStructureChecker.FormedVerdict;

/**
 * The SET scaffold walk (task p16-form-scaffold — the ADR 2026-09-05-p16-formation-scoping
 * SET capability): {@link GTMultiBlockStructureChecker#form} completes a pattern-bound
 * structure from inventory. The four acceptance arms — scaffold, no-scaffold, short stock,
 * hollow never scaffolded — plus the inventory-free regressions: form(null, null) classifies
 * exactly like the pure check and writes nothing (the 600-tick poll and the onTickFirst
 * forced check keep their three-null arm), and a formed structure consumes nothing.
 */
public class GTMultiBlockStructureCheckerFormTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<FormController> sFormControllerType;

	/** A controller whose whole structure face is a settable pattern (null = unbound). */
	static class FormController extends TileEntityBase10MultiBlockBase {
		@Nullable
		GTMultiBlockPattern mPattern = null;

		FormController(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "test_form_controller";
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

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildFormFixtures() {
		BlockEntityType<FormController>[] tHolder = (BlockEntityType<FormController>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new FormController(tHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		sFormControllerType = tHolder[0];
	}

	// The layout, controller (100,64,100) facing north (2):
	//   cell A formingPart (0,0,1)  -> world (100,64,102)
	//   cell B formingPart (0,0,-1) -> world (100,64,100) = the controller's own cell
	//      (the self-cell arm — the scaffold never needs a block there)
	//   cell H hollow      (1,0,0)  -> world (101,64,101)
	private static final BlockPos CELL_A = new BlockPos(100, 64, 102);
	private static final BlockPos CELL_H = new BlockPos(101, 64, 101);

	private static FormController formController(MultiBlockLevel aLevel) {
		FormController tController = placeController(aLevel, sFormControllerType, C1, (byte)2);
		tController.mPattern = GTMultiBlockPattern.builder()
				.formingPart(0, 0, 1, Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0)
				.formingPart(0, 0, -1, Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0)
				.hollow(1, 0, 0, GTMultiBlockPattern.AIR)
				.build();
		return tController;
	}

	private static SimpleContainer brickStock(int aCount) {
		return new SimpleContainer(new ItemStack(Blocks.BRICKS, aCount));
	}

	/** The part-BE factory the stub world runs on setBlock (the CokeOvenCheckerTest recipe). */
	private static void mountPartFactory(MultiBlockLevel aLevel) {
		aLevel.mBeFactory = (aPos, aState) -> aState.is(Blocks.BRICKS) ? sPartType.create(aPos, aState) : null;
	}

	/** The oven cell for centre-relative (i,j,k) under facing north: controller + cellOffset(2,...) == (100+i, 64+j, 101+k). */
	private static BlockPos ovenCell(BlockPos aController, int aI, int aJ, int aK) {
		return aController.offset(aI, aJ, aK + 1);
	}

	// ------------------------------------------------------------------
	// arm 1 — the scaffold
	// ------------------------------------------------------------------

	@Test
	public void scaffoldCompletesAndConsumesExactlyTheMissingCells() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		FormController tController = formController(tLevel);
		SimpleContainer tStock = brickStock(2); // one cell missing, one spare

		FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tController, tController.mFacing, null, tStock);
		assertTrue(tVerdict.formed, "the scaffold forms: " + tVerdict.failedCells());

		// the placed cell rode the upstream path — bound, mode/design written
		MultiBlockPartBlockEntity tPart = (MultiBlockPartBlockEntity) tLevel.getBlockEntity(CELL_A);
		assertNotNull(tPart, "the missing cell got its part BE through the scaffold");
		assertSame(tController, tPart.mTarget, "the scaffolded part is claimed by THIS controller");
		assertEquals(MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, tPart.mMode, "the cell's usage mask got written");
		assertEquals(0, tPart.mDesign, "the cell's design index got written");
		assertEquals(1, tStock.getItem(0).getCount(), "exactly the missing count was consumed");
		assertFalse(tLevel.mBlockEntities.containsKey(CELL_H), "the hollow never gained anything");
	}

	@Test
	public void cokeOvenGreenfieldScaffoldFromScratch() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		TileEntityCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte)2);
		SimpleContainer tStock = brickStock(25); // the RCON positive arm: a stack covers the whole shell

		FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tOven, tOven.mFacing, null, tStock);
		assertTrue(tVerdict.formed, "the from-scratch scaffold forms: " + tVerdict.failedCells());
		assertEquals(0, tStock.getItem(0).getCount(), "all 25 bricks consumed (26 forming cells - the self-cell)");

		// 缺件格全绑定 — every shell cell is claimed by the oven
		int tLinked = 0;
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			BlockPos tCell = ovenCell(C1, i, j, k);
			if (tLevel.getBlockEntity(tCell) instanceof MultiBlockPartBlockEntity tPart && tPart.mTarget == tOven) tLinked++;
		}
		assertEquals(25, tLinked, "every scaffolded brick linked (the binding rode checkAndSetTarget)");
		assertTrue(tOven.checkStructure(true), "the linking pass confirms the scaffolded structure");
	}

	// ------------------------------------------------------------------
	// arm 2 — no scaffold without an inventory, and the pure check stays pure
	// ------------------------------------------------------------------

	@Test
	public void noInventoryNeverScaffoldsAndCheckPathStaysPure() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		FormController tController = formController(tLevel);
		Map<BlockPos, BlockState> tStatesBefore = new HashMap<>(tLevel.mStates);

		// the pure check (the 600t poll / onTickFirst shape) — the baseline classification
		FormedVerdict tCheck = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertFalse(tCheck.formed, "the missing cell fails the plain check");

		// form with the null triple: the same verdict, ZERO writes — the scaffold gate never opens
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tController, tController.mFacing, null, null);
		assertFalse(tVerdict.formed, "no inventory, no scaffold");
		assertEquals(tCheck.failedCells().size(), tVerdict.failedCells().size(), "the same failure count as the plain check");
		assertNotNull(tVerdict.firstFailedCell());
		assertEquals(tCheck.firstFailedCell().reason, tVerdict.firstFailedCell().reason, "the same raw reason");
		assertEquals(tCheck.firstFailedCell().pos, tVerdict.firstFailedCell().pos, "the same failing cell");
		assertEquals(tStatesBefore, tLevel.mStates, "zero world writes");
		assertFalse(tLevel.mBlockEntities.containsKey(CELL_A), "nothing was placed");

		// an EMPTY inventory is refused by the stock check — still zero writes
		SimpleContainer tEmpty = new SimpleContainer(1);
		tVerdict = GTMultiBlockStructureChecker.form(tController, tController.mFacing, null, tEmpty);
		assertFalse(tVerdict.formed, "an empty inventory cannot scaffold");
		assertNotNull(tVerdict.firstFailedCell());
		assertEquals("insufficient stock: needs 1, has 0 of " + Blocks.BRICKS.getDescriptionId(),
				tVerdict.firstFailedCell().reason, "the transactional refusal names the shortfall");
		assertEquals(tStatesBefore, tLevel.mStates, "still zero world writes");
	}

	// ------------------------------------------------------------------
	// arm 3 — short stock: not formed AND not consumed
	// ------------------------------------------------------------------

	@Test
	public void insufficientStockConsumesNothingAndPlacesNothing() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		TileEntityCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte)2);
		SimpleContainer tStock = brickStock(10); // 25 cells missing, 10 bricks in stock

		FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tOven, tOven.mFacing, null, tStock);
		assertFalse(tVerdict.formed, "short stock refuses the whole scaffold");
		assertNotNull(tVerdict.firstFailedCell());
		assertEquals("insufficient stock: needs 25, has 10 of " + Blocks.BRICKS.getDescriptionId(),
				tVerdict.firstFailedCell().reason, "the shortfall is the diagnosis, fronted by the first planned cell");
		assertEquals(10, tStock.getItem(0).getCount(), "NOTHING consumed (the transactional contract)");
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			BlockPos tCell = ovenCell(C1, i, j, k);
			if (tCell.equals(C1)) continue; // the self-cell holds the controller BE already
			assertFalse(tLevel.mBlockEntities.containsKey(tCell),
					"NOTHING placed (the transactional contract)");
		}
	}

	// ------------------------------------------------------------------
	// arm 4 — the hollow is never scaffolded (and a hard failure pre-empts execution)
	// ------------------------------------------------------------------

	@Test
	public void hollowNeverScaffoldedAndHardFailurePreemptsExecution() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		FormController tController = formController(tLevel);
		tLevel.setBlock(CELL_H, Blocks.STONE.defaultBlockState(), 3); // the hollow is FILLED
		SimpleContainer tStock = brickStock(8); // plenty for the one missing part cell

		FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tController, tController.mFacing, null, tStock);
		assertFalse(tVerdict.formed, "a filled hollow refuses the scaffold");
		assertNotNull(tVerdict.firstFailedCell());
		assertEquals("hollow cell must be air (never scaffolded)", tVerdict.firstFailedCell().reason,
				"the hollow is a hard failure, not a scaffold target");
		assertEquals(Blocks.STONE.defaultBlockState().getBlock(), tLevel.getBlockState(CELL_H).getBlock(),
				"the scaffold never clears what the CHECK would not");
		assertFalse(tLevel.mBlockEntities.containsKey(CELL_A), "the hard failure pre-empts the scaffold");
		assertEquals(8, tStock.getItem(0).getCount(), "nothing consumed by a refused form");
	}

	// ------------------------------------------------------------------
	// idempotence — a formed structure consumes nothing on a second form
	// ------------------------------------------------------------------

	@Test
	public void secondFormOnFormedStructureConsumesNothing() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		FormController tController = formController(tLevel);
		assertTrue(GTMultiBlockStructureChecker.form(tController, tController.mFacing, null, brickStock(2)).formed,
				"the first form scaffolds");

		SimpleContainer tRestock = brickStock(4);
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tController, tController.mFacing, null, tRestock);
		assertTrue(tVerdict.formed, "the formed structure answers formed");
		assertEquals(4, tRestock.getItem(0).getCount(), "the early return consumes nothing");
	}
}
