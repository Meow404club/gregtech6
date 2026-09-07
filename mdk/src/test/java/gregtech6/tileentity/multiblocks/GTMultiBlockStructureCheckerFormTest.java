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
import gregtech6.tileentity.multiblocks.ITileEntityMultiBlockController.Util;

/**
 * The SET scaffold walk (task p16-form-scaffold — the ADR 2026-09-05-p16-formation-scoping
 * SET capability): {@link GTMultiBlockStructureChecker#form} completes a pattern-bound
 * structure from inventory. The four acceptance arms — scaffold, no-scaffold, short stock,
 * hollow never scaffolded — plus the inventory-free regressions: form(null, null) classifies
 * exactly like the pure check and writes nothing (the 600-tick poll and the onTickFirst
 * forced check keep their three-null arm), and a formed structure consumes nothing.
 *
 * <p>Task p22-boiler-form-reject-creative-fixture adds the refusal calibres: a
 * declaration-only {@code part()} mismatch is a HARD failure — never scaffolded, never
 * consumed, zero world writes (the :226-227 classification, arms 5 and 6, the boiler
 * carrying the whole-shell arm). The creative Player arm was PROBED and cut
 * (probe-first task ruling, both outcomes close): the vanilla ctor chain is pure
 * (Player.java:180-188 — bootstrapped EntityType, EMPTY-list Inventory, null-MenuType
 * InventoryMenu), but the FORGE PATCH layer blocks structurally — the patched Entity
 * ctor (Entity.java:3464) forces {@code FluidType.SIZE} (FluidType.java:71:
 * {@code ForgeRegistries.FLUID_TYPES.get().getKeys()}), and that registry supplier is
 * null offline (created only by ForgeMod's NewRegistryEvent — the NetworkHooks lesson
 * layer). No production signature was touched; the :259-260 creative consumption
 * stays covered by the Util rulings instead.
 */
public class GTMultiBlockStructureCheckerFormTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<FormController> sFormControllerType;
	static BlockEntityType<FormBoiler> sFormBoilerType;

	/**
	 * The boiler fixture — the declaration-only calibre (the TileEntityCokeOvenCheckerTest
	 * CheckerBoiler recipe): fixture blocks bound on the wall/transmitter hooks, the lazy
	 * pattern is the real {@link TileEntityLargeBoiler} declaration (34 part cells + the
	 * hollow, zero forming expectations — the ADR scoping).
	 */
	public static final class FormBoiler extends TileEntityLargeBoiler {
		FormBoiler(BlockPos aPos, BlockState aState) {
			super(sFormBoilerType, aPos, aState);
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

	// The offline creative probe RESULT (recorded in place of the cut fixture): the vanilla
	// ctor chain is pure (Player.java:180-188 — bootstrapped EntityType, EMPTY-list
	// Inventory, null-MenuType InventoryMenu, the two abstract seams Player.java:1845/:1857
	// plus the protected Entity.getPermissionLevel() hook Entity.java:2978-2982), but the
	// FORGE PATCH layer blocks structurally: the patched Entity ctor (Entity.java:3464)
	// forces FluidType.SIZE (FluidType.java:71), whose registry supplier is null offline.
	// The arm was cut per the task card; no production signature was touched.

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
		BlockEntityType<FormBoiler>[] tBoilerHolder = (BlockEntityType<FormBoiler>[]) new BlockEntityType<?>[1];
		tBoilerHolder[0] = BlockEntityType.Builder.of(FormBoiler::new, Blocks.BRICKS, Blocks.STONE).build(null);
		sFormBoilerType = tBoilerHolder[0];
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

	// ------------------------------------------------------------------
	// arm 5 — a declaration-only part() mismatch is a HARD failure (never
	// scaffolded, never consumed, zero writes) — the :226-227 classification
	// ------------------------------------------------------------------

	// the layout, controller (100,64,100) facing north (2):
	//   cell A formingPart      (0,0,1) -> world (100,64,102) = CELL_A, standing AIR
	//   cell B formingPart      (0,0,-1) -> the controller's own cell (the self-cell pass)
	//   cell D declaration-only (1,0,0) -> world (101,64,101) = CELL_D, standing the WRONG block
	private static final BlockPos CELL_D = new BlockPos(101, 64, 101);

	@Test
	public void declarationOnlyMismatchHardRejectsFormWithZeroWritesAndZeroConsumption() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		FormController tController = placeController(tLevel, sFormControllerType, C1, (byte)2);
		tController.mPattern = GTMultiBlockPattern.builder()
				.formingPart(0, 0, 1, Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0)
				.formingPart(0, 0, -1, Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0)
				.part(1, 0, 0, aState -> aState.is(Blocks.BRICKS))
				.build();
		tLevel.setBlock(CELL_D, Blocks.STONE.defaultBlockState(), 3); // the wrong block in the declaration-only cell
		SimpleContainer tStock = brickStock(2); // plenty for the missing forming cell — irrelevant to a hard failure
		Map<BlockPos, BlockState> tStatesBefore = new HashMap<>(tLevel.mStates);

		// the diagnosis beat: the walk classifies cell D with the raw reason (:191)
		FormedVerdict tCheck = GTMultiBlockStructureChecker.check(tController, tController.mFacing, null, null, null);
		assertFalse(tCheck.formed, "the walk fails both cells");
		assertEquals(2, tCheck.failedCells().size(), "the missing part and the declaration-only mismatch");
		assertEquals("declaration-only cell does not match", tCheck.failedCells().get(1).reason,
				"the declaration-only walk reason (GTMultiBlockStructureChecker :191)");
		assertEquals(CELL_D, tCheck.failedCells().get(1).pos, "the wrong cell is the report");

		// form() reclassifies it as the never-scaffolded hard class (:283) — the scaffoldable
		// forming cell was PLANNED (beat 2 demand), but one hard failure refuses the whole form
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tController, tController.mFacing, null, tStock);
		assertFalse(tVerdict.formed, "the declaration-only mismatch refuses the form");
		assertEquals(1, tVerdict.failedCells().size(),
				"only the hard cell fronts the refusal — the scaffoldable cell is not a failure");
		assertNotNull(tVerdict.firstFailedCell());
		assertEquals("declaration-only cell does not match (never scaffolded)", tVerdict.firstFailedCell().reason,
				"the form classification (GTMultiBlockStructureChecker :283)");
		assertEquals(CELL_D, tVerdict.firstFailedCell().pos, "the wrong cell fronts the refusal");
		assertEquals(tStatesBefore, tLevel.mStates, "zero world writes (the hard failure pre-empts execution)");
		assertEquals(2, tStock.getItem(0).getCount(), "zero consumption (the transactional contract)");
		assertFalse(tLevel.mBlockEntities.containsKey(CELL_A), "nothing was placed");
	}

	// ------------------------------------------------------------------
	// arm 6 — the boiler calibre: the whole declaration-only shell refuses
	// (the TileEntityCokeOvenCheckerTest boiler arm 1 context, now through form)
	// ------------------------------------------------------------------

	@Test
	public void boilerDeclarationOnlyShellRefusesTheWholeScaffold() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		FormBoiler tBoiler = placeController(tLevel, sFormBoilerType, C1, (byte)2);
		// the whole shell WRONG: every part cell of the bound pattern stands DIRT; the hollow
		// and the controller's own cell stay as the pattern expects them
		for (GTMultiBlockPattern.Cell tCell : tBoiler.getStructurePattern().cells()) {
			if (tCell.isHollow()) continue;
			BlockPos tCellPos = C1.offset(tCell.x, tCell.y, tCell.z + 1); // anchor-relative, facing north
			if (tCellPos.equals(C1)) continue; // the self-cell holds the controller BE
			tLevel.setBlock(tCellPos, Blocks.DIRT.defaultBlockState(), 3);
		}
		SimpleContainer tStock = brickStock(64); // a full stack — nothing is scaffoldable from a declaration-only miss
		Map<BlockPos, BlockState> tStatesBefore = new HashMap<>(tLevel.mStates);

		// the diagnosis beat: the boiler pattern carries no forming expectation (the ADR
		// scoping) — the walk sees every shell cell as a declaration-only miss
		FormedVerdict tCheck = GTMultiBlockStructureChecker.check(tBoiler, tBoiler.mFacing, null, null, null);
		assertFalse(tCheck.formed, "the wrong shell fails the walk");
		assertEquals(34, tCheck.failedCells().size(), "all 34 part cells miss (the TileEntityCokeOvenCheckerTest arm 1 count)");
		assertEquals("declaration-only cell does not match", tCheck.firstFailedCell().reason,
				"the declaration-only reason (the boiler pattern carries no forming expectation)");

		// form(): every miss is the never-scaffolded hard class — the whole form refuses
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tBoiler, tBoiler.mFacing, null, tStock);
		assertFalse(tVerdict.formed, "the declaration-only shell refuses the whole form");
		assertEquals(34, tVerdict.failedCells().size(), "the WHOLE shell fronts the refusal");
		assertTrue(tVerdict.failedCells().stream().allMatch(tCell ->
						"declaration-only cell does not match (never scaffolded)".equals(tCell.reason)),
				"every refusal is the hard class, zero scaffold targets");
		assertEquals(tStatesBefore, tLevel.mStates, "zero world writes (the DIRT shell still stands)");
		assertEquals(64, tStock.getItem(0).getCount(), "zero consumption (the transactional contract)");
		assertEquals(1, tLevel.mBlockEntities.size(), "nothing was placed (the refusal happened in the plan beat)");
	}

	// ------------------------------------------------------------------
	// task p24-creative-form-seam — the Util.checkAndSetTarget boolean seam:
	// the creative/OP(2) rulings injected as a boolean pair (no Player is
	// constructible offline — the Forge FluidType.SIZE wall, the probe record
	// above), so the permission chain becomes directly drivable
	// ------------------------------------------------------------------

	/** A C1 shell cell (air, within the null-clickedAt scope) — the CheckAndSetTargetTest partCell recipe. */
	private static BlockPos seamCell(int aI, int aJ, int aK) {
		return new BlockPos(100 + aI, 64 + aJ, 101 + aK);
	}

	@Test
	public void utilSeamCreativePairPlacesForFreeAndConsumesNothing() {
		// (T, T) — the creative calibre: the free-placement branch opens even with a stocked
		// inventory present, so the stock pays nothing
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		TestCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte) 2);
		BlockPos tCell = seamCell(0, -1, -1);
		SimpleContainer tStock = new SimpleContainer(new ItemStack(Blocks.BRICKS, 4));

		// pass 1 places (free) and fails on the stale reference — the two-pass quirk
		assertFalse(Util.checkAndSetTarget(tOven, tCell.getX(), tCell.getY(), tCell.getZ(),
				Blocks.BRICKS, 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, null, null, tStock, true, true),
				"pass 1 fails on the stale pre-placement reference");
		assertEquals(4, tStock.getItem(0).getCount(), "creative consumes NOTHING (the free branch)");
		assertNotNull(tLevel.getBlockEntity(tCell), "the cell was scaffolded anyway");

		// pass 2 links the fresh part
		assertTrue(Util.checkAndSetTarget(tOven, tCell.getX(), tCell.getY(), tCell.getZ(),
				Blocks.BRICKS, 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, null, null, tStock, true, true),
				"pass 2 binds the placed part");
		assertEquals(4, tStock.getItem(0).getCount(), "still nothing consumed");
	}

	@Test
	public void utilSeamMayEditWithoutInfiniteItemsConsumesExactlyOne() {
		// (T, F) — the OP(2) calibre, the survivalConsumeWithNullPlayerTakesExactlyOne mirror:
		// the free branch is shut, the inventory scan shrinks exactly one
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		TestCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte) 2);
		BlockPos tCell = seamCell(-1, -1, -1);
		SimpleContainer tStock = new SimpleContainer(new ItemStack(Blocks.BRICKS, 5));

		assertFalse(Util.checkAndSetTarget(tOven, tCell.getX(), tCell.getY(), tCell.getZ(),
				Blocks.BRICKS, 0, 0, null, null, tStock, true, false),
				"pass 1 fails on the stale pre-placement reference");
		assertEquals(4, tStock.getItem(0).getCount(), "the consume path shrank exactly one");
		assertNotNull(tLevel.getBlockEntity(tCell), "the cell was scaffolded");
	}

	@Test
	public void utilSeamPermissionDeniedBlocksPlacementEvenWithInfiniteItems() {
		// (F, *) — the scaffold gate stays shut even when easyRep is satisfied (the cell IS
		// air); the star instance even carries infinite items: zero writes, zero consumption
		for (boolean tInfinite : new boolean[] {true, false}) {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			mountPartFactory(tLevel);
			TestCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte) 2);
			BlockPos tCell = seamCell(1, -1, -1);
			SimpleContainer tStock = new SimpleContainer(new ItemStack(Blocks.BRICKS, 4));

			assertFalse(Util.checkAndSetTarget(tOven, tCell.getX(), tCell.getY(), tCell.getZ(),
					Blocks.BRICKS, 0, 0, null, null, tStock, false, tInfinite),
					"nothing was placed, so the arbitration finds no part");
			assertTrue(tLevel.getBlockState(tCell).isAir(), "the easyRep cell was NOT written (mayEdit=false)");
			assertFalse(tLevel.mBlockEntities.containsKey(tCell), "no part BE appeared");
			assertEquals(4, tStock.getItem(0).getCount(), "nothing consumed");
		}
	}

	@Test
	public void utilWrapperNullPlayerMatchesSeamTrueFalse() {
		// the wrapper(null, inv) parse: canEdit(null) = T, hasInfiniteItems(null) = F — the
		// public wrapper over a fresh fixture must be indistinguishable from the seam fed
		// (T, F) directly over an identical one
		MultiBlockLevel tWrapperLevel = new MultiBlockLevel();
		mountPartFactory(tWrapperLevel);
		TestCokeOven tWrapperOven = placeController(tWrapperLevel, sCokeOvenType, C1, (byte) 2);
		BlockPos tWrapperCell = seamCell(0, -1, 1);
		SimpleContainer tWrapperStock = new SimpleContainer(new ItemStack(Blocks.BRICKS, 5));

		MultiBlockLevel tSeamLevel = new MultiBlockLevel();
		mountPartFactory(tSeamLevel);
		TestCokeOven tSeamOven = placeController(tSeamLevel, sCokeOvenType, C1, (byte) 2);
		BlockPos tSeamCell = seamCell(0, -1, 1);
		SimpleContainer tSeamStock = new SimpleContainer(new ItemStack(Blocks.BRICKS, 5));

		boolean tWrapper = Util.checkAndSetTarget(tWrapperOven, tWrapperCell.getX(), tWrapperCell.getY(), tWrapperCell.getZ(),
				Blocks.BRICKS, 0, 0, null, null, tWrapperStock);
		boolean tSeam = Util.checkAndSetTarget(tSeamOven, tSeamCell.getX(), tSeamCell.getY(), tSeamCell.getZ(),
				Blocks.BRICKS, 0, 0, null, null, tSeamStock, true, false);

		assertEquals(tSeam, tWrapper, "the verdicts agree (both the stale first pass)");
		assertEquals(tSeamStock.getItem(0).getCount(), tWrapperStock.getItem(0).getCount(), "the consumption agrees");
		assertEquals(tWrapperLevel.mBlockEntities.containsKey(tWrapperCell), tSeamLevel.mBlockEntities.containsKey(tSeamCell),
				"the placement agrees");
		assertEquals(4, tWrapperStock.getItem(0).getCount(), "the wrapper(null) IS the OP(2) consume arm");
	}
}
