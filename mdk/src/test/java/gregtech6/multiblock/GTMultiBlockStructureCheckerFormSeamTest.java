package gregtech6.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase.MultiBlockLevel;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;

import gregtech6.multiblock.GTMultiBlockStructureChecker.FormedVerdict;

/**
 * The {@link GTMultiBlockStructureChecker#form} permission seam (task
 * p24-creative-form-seam) driven directly. This file lives in
 * {@code gregtech6.multiblock} because the seam overload is package-private to
 * {@link GTMultiBlockStructureChecker} — the sibling fixture base and its tests
 * ({@code gregtech6.tileentity.multiblocks}) cannot see it, so the fixture below is a
 * local replica (~40 lines) of the FormController recipe: every pinned class
 * ({@link TileEntityBase10MultiBlockBase}, {@link MultiBlockPartBlockEntity},
 * {@link MultiBlockLevel}) is public, only the holder constants were package-private.
 *
 * <p>The arms (no Player is constructible offline — the Forge-patched Entity ctor forces
 * {@code FluidType.SIZE}, the FormTest :74-80 wall; the boolean pair IS the testable form
 * of the creative/OP(2) chain):
 * <ul>
 * <li>(true, true) — creative: the beat-3 stock gate is WAIVED (:288), the plan never
 *     hard-refuses on stock. The beat-4 execution re-derives the rulings from the
 *     passed-through player (the wrapper_checker 透传不动 ruling), so offline the
 *     placement follows the Util's null-player arm; the fully free placement is only
 *     reachable through the Util seam (the FormTest utilSeamCreativePair arm);</li>
 * <li>(true, false) — OP(2): a sufficient stock forms with an exact consumption;</li>
 * <li>(true, false) + short stock — the transactional refusal: "insufficient stock",
 *     zero writes, zero consumption;</li>
 * <li>(false, *) — the "no permission to scaffold" hard class even with infinite items:
 *     zero writes, zero consumption, zero placement.</li>
 * </ul>
 */
public class GTMultiBlockStructureCheckerFormSeamTest extends GTMultiBlocksOfflineTestBase {

	/** The controller cell — the C1 recipe coordinates (the base constant is package-private). */
	private static final BlockPos CTR = new BlockPos(100, 64, 100);
	/** The one missing forming cell: pattern (0,0,1) under facing north -> CTR + (0,0,+1). */
	private static final BlockPos CELL_A = new BlockPos(100, 64, 101 + 1);
	/** The hollow cell: pattern (1,0,0) -> CTR + (1,0,+1). */
	private static final BlockPos CELL_H = new BlockPos(100 + 1, 64, 101 + 1);

	static BlockEntityType<SeamController> sSeamControllerType;
	static BlockEntityType<MultiBlockPartBlockEntity> sSeamPartType;

	/** The FormController recipe, replicated for this package. */
	public static final class SeamController extends TileEntityBase10MultiBlockBase {
		@Nullable
		GTMultiBlockPattern mPattern = null;

		SeamController(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "test_form_seam_controller";
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
	static void buildSeamFixtures() {
		BlockEntityType<SeamController>[] tHolder = (BlockEntityType<SeamController>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new SeamController(tHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		sSeamControllerType = tHolder[0];
		BlockEntityType<MultiBlockPartBlockEntity>[] tPartHolder = (BlockEntityType<MultiBlockPartBlockEntity>[]) new BlockEntityType<?>[1];
		tPartHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new MultiBlockPartBlockEntity(tPartHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		sSeamPartType = tPartHolder[0];
	}

	/** The controller + pattern + part factory, mounted on a fresh stub world (the FormTest formController recipe). */
	private static SeamController seamController(MultiBlockLevel aLevel) {
		aLevel.mBeFactory = (aPos, aState) -> aState.is(Blocks.BRICKS) ? sSeamPartType.create(aPos, aState) : null;
		SeamController tController = sSeamControllerType.create(CTR, Blocks.BRICKS.defaultBlockState());
		tController.setLevel(aLevel);
		tController.mFacing = (byte) 2;
		aLevel.mStates.put(CTR, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(CTR, tController);
		tController.mPattern = GTMultiBlockPattern.builder()
				.formingPart(0, 0, 1, Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0)
				.formingPart(0, 0, -1, Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0)
				.hollow(1, 0, 0, GTMultiBlockPattern.AIR)
				.build();
		return tController;
	}

	// ------------------------------------------------------------------
	// arm 1 — (T, T): creative waives the beat-3 stock gate (:288 skip)
	// ------------------------------------------------------------------

	@Test
	public void creativePairWaivesTheStockGateAndForms() {
		// with a stocked inventory the form completes — the gate never even looks at the stock
		MultiBlockLevel tLevel = new MultiBlockLevel();
		SeamController tController = seamController(tLevel);
		SimpleContainer tStock = new SimpleContainer(new ItemStack(Blocks.BRICKS, 4));
		FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tController, tController.mFacing, null, tStock, true, true);
		assertTrue(tVerdict.formed, "the creative pair forms: " + tVerdict.failedCells());
		assertNotNull(tLevel.getBlockEntity(CELL_A), "the missing cell was scaffolded");
		// the execution re-derives the rulings from the passed-through player (null ->
		// the Util's consume arm), so the stock pays — the wrapper_checker 透传不动 ruling
		assertEquals(3, tStock.getItem(0).getCount(), "the null-player execution consumed one (the transparent pass-through)");
		assertFalse(tLevel.mBlockEntities.containsKey(CELL_H), "the hollow never gained anything");

		// the :288 skip IS the proof point: an EMPTY inventory draws NO "insufficient stock"
		// hard class under (T, T) — the form proceeds to the execution beat and misses there
		// (the fully free placement needs a real creative player, the FormTest :74-80 wall)
		MultiBlockLevel tEmptyLevel = new MultiBlockLevel();
		SeamController tEmptyController = seamController(tEmptyLevel);
		Map<BlockPos, BlockState> tStatesBefore = new HashMap<>(tEmptyLevel.mStates);
		SimpleContainer tEmpty = new SimpleContainer(1);
		FormedVerdict tEmptyVerdict = GTMultiBlockStructureChecker.form(tEmptyController, tEmptyController.mFacing, null, tEmpty, true, true);
		assertFalse(tEmptyVerdict.formed, "with nothing to place the linking pass still fails");
		assertTrue(tEmptyVerdict.failedCells().stream().noneMatch(tCell -> tCell.reason.startsWith("insufficient stock")),
				"the creative pair waived the stock gate entirely (:288)");
		assertEquals(tStatesBefore, tEmptyLevel.mStates, "zero world writes (nothing was placeable)");
	}

	// ------------------------------------------------------------------
	// arm 2 — (T, F): the OP(2) calibre forms with an exact consumption
	// ------------------------------------------------------------------

	@Test
	public void mayEditWithoutInfiniteItemsFormsConsumingExactlyOne() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		SeamController tController = seamController(tLevel);
		SimpleContainer tStock = new SimpleContainer(new ItemStack(Blocks.BRICKS, 4)); // one cell, three spares

		FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tController, tController.mFacing, null, tStock, true, false);
		assertTrue(tVerdict.formed, "a sufficient stock forms: " + tVerdict.failedCells());
		assertEquals(3, tStock.getItem(0).getCount(), "exactly the missing count was consumed");
		assertNotNull(tLevel.getBlockEntity(CELL_A), "the missing cell was scaffolded");
	}

	// ------------------------------------------------------------------
	// arm 3 — (T, F) + short stock: the transactional refusal
	// ------------------------------------------------------------------

	@Test
	public void shortStockWithOp2PairHardRefusesWithZeroWritesAndZeroConsumption() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		SeamController tController = seamController(tLevel);
		Map<BlockPos, BlockState> tStatesBefore = new HashMap<>(tLevel.mStates);
		SimpleContainer tStock = new SimpleContainer(1); // an EMPTY slot where a brick is demanded

		FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tController, tController.mFacing, null, tStock, true, false);
		assertFalse(tVerdict.formed, "short stock refuses the whole scaffold");
		assertNotNull(tVerdict.firstFailedCell());
		assertEquals("insufficient stock: needs 1, has 0 of " + Blocks.BRICKS.getDescriptionId(),
				tVerdict.firstFailedCell().reason, "the transactional refusal names the shortfall");
		assertEquals(tStatesBefore, tLevel.mStates, "zero world writes");
		assertFalse(tLevel.mBlockEntities.containsKey(CELL_A), "nothing was placed");
	}

	// ------------------------------------------------------------------
	// arm 4 — (F, *): the "no permission to scaffold" hard class — even
	// infinite items scaffold nothing
	// ------------------------------------------------------------------

	@Test
	public void permissionDeniedHardRefusesEvenWithInfiniteItems() {
		for (boolean tInfinite : new boolean[] {true, false}) {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			SeamController tController = seamController(tLevel);
			Map<BlockPos, BlockState> tStatesBefore = new HashMap<>(tLevel.mStates);
			SimpleContainer tStock = new SimpleContainer(new ItemStack(Blocks.BRICKS, 4));

			FormedVerdict tVerdict = GTMultiBlockStructureChecker.form(tController, tController.mFacing, null, tStock, false, tInfinite);
			assertFalse(tVerdict.formed, "a closed permission chain refuses the form");
			assertNotNull(tVerdict.firstFailedCell());
			assertEquals("no permission to scaffold", tVerdict.firstFailedCell().reason,
					"the hard class, fronted by the scaffoldable cell");
			assertEquals(tStatesBefore, tLevel.mStates, "zero world writes");
			assertFalse(tLevel.mBlockEntities.containsKey(CELL_A), "nothing was placed");
			assertEquals(4, tStock.getItem(0).getCount(), "nothing consumed");
		}
	}
}
