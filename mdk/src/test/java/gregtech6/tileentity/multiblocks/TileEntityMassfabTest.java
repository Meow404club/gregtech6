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

import gregapi.data.TD;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.RecipeMap;

/**
 * The Large Matter Fabricator offline acceptance (task p31-massfab): the :1241
 * registration-config pins (QU, the 1/1/2097152 explicit window, PARALLEL 64 +
 * PARALLEL_DURATION T, CHEAP_OC T, no ignition, no constant power, the TWO-tank output
 * bank), the hand-written :45-224 structure walk over the stub world (the greenfield
 * scaffold consumes exactly the 149 non-self cells, the ≥4 Control / ≥4 Conversion quota
 * arms, the wrong-part rejection, the centre-air rule), the isInsideStructure box and
 * the upstream BasicMachine:489-517 energy-injection window.
 *
 * <p>Fixture posture = the implosion test over the {@link GTMultiBlocksOfflineTestBase}
 * stub world: the six structure roles bind to vanilla stand-ins (walls BRICKS, coils
 * IRON_BLOCK, vents GOLD_BLOCK, the versatile PU DIAMOND_BLOCK, Control EMERALD_BLOCK,
 * Conversion LAPIS_BLOCK).
 */
public class TileEntityMassfabTest extends GTMultiBlocksOfflineTestBase {

	/** The controller cell for this suite — away from the shared C1/C2 arbitration fixtures. */
	static final BlockPos MFAB_POS = new BlockPos(200, 64, 200);

	static BlockEntityType<TestMassfab> sMassfabType;
	/** The suite's own part BET — valid over ALL six stand-in blocks (the 21.1
	 * BlockEntity.validateBlockState rejects a part BE whose state block is outside the
	 * BET valid list; the shared sPartType fixture is BRICKS-only). */
	static BlockEntityType<MultiBlockPartBlockEntity> sMassfabPartType;

	/** The concrete test BE — the Massfab controller over a vanilla-block BET, the six roles bound to stand-ins. */
	public static final class TestMassfab extends TileEntityMassfab {
		public TestMassfab(BlockPos aPos, BlockState aState) {
			super(sMassfabType, aPos, aState);
		}
		public TestMassfab(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}
		@Override protected Block getWallBlock() { return Blocks.BRICKS; }
		@Override protected Block getCoilBlock() { return Blocks.IRON_BLOCK; }
		@Override protected Block getVentBlock() { return Blocks.GOLD_BLOCK; }
		@Override protected Block getVersatileBlock() { return Blocks.DIAMOND_BLOCK; }
		@Override protected Block getControlBlock() { return Blocks.EMERALD_BLOCK; }
		@Override protected Block getConversionBlock() { return Blocks.LAPIS_BLOCK; }
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildMassfabFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		BlockEntityType<TestMassfab>[] tHolder = (BlockEntityType<TestMassfab>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(TestMassfab::new,
				Blocks.BRICKS, Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.LAPIS_BLOCK).build(null);
		sMassfabType = tHolder[0];
		// the SELF-REFERENCING holder (the base selfHolder form): the factory must bind the
		// THREE-ARG ctor — the (pos, state) two-arg ctor is the PRODUCTION BET carrier, and
		// 21.1's Builder.of binds it as-is (the frozen registry keeps .get() null offline)
		BlockEntityType<MultiBlockPartBlockEntity>[] tPartHolder = (BlockEntityType<MultiBlockPartBlockEntity>[]) new BlockEntityType<?>[1];
		tPartHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new MultiBlockPartBlockEntity(tPartHolder[0], aPos, aState),
				Blocks.BRICKS, Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.LAPIS_BLOCK).build(null);
		sMassfabPartType = tPartHolder[0];
	}

	/** The part-BE factory the stub world runs on setBlock (every stand-in creates a part). */
	private static void mountPartFactory(MultiBlockLevel aLevel) {
		aLevel.mBeFactory = (aPos, aState) -> sMassfabPartType.create(aPos, aState);
	}

	/** The scaffold stock: walls 97 (the 98-wall count includes the controller self-cell), coils 26, vents 16, the versatile 1, 4 + 4 quota PUs = 148 items (150 cells minus the self-cell). */
	private static SimpleContainer fullStock() {
		return new SimpleContainer(
				new ItemStack(Blocks.BRICKS, 97),
				new ItemStack(Blocks.IRON_BLOCK, 26),
				new ItemStack(Blocks.GOLD_BLOCK, 16),
				new ItemStack(Blocks.DIAMOND_BLOCK, 1),
				new ItemStack(Blocks.EMERALD_BLOCK, 4),
				new ItemStack(Blocks.LAPIS_BLOCK, 4));
	}

	private static void assertStockEmpty(SimpleContainer aStock) {
		for (int i = 0; i < aStock.getContainerSize(); i++) {
			assertEquals(0, aStock.getItem(i).getCount(), "stack " + i + " fully consumed");
		}
	}

	// ------------------------------------------------------------------
	// the :1241 registration-config pins
	// ------------------------------------------------------------------

	@Test
	public void registrationConfigPins() {
		TestMassfab tMachine = sMassfabType.create(MFAB_POS, Blocks.BRICKS.defaultBlockState());
		assertEquals(64, tMachine.mParallel, "Loader :1241 NBT_PARALLEL 64");
		assertTrue(tMachine.mParallelDuration, "Loader :1241 NBT_PARALLEL_DURATION T — the :743 parallel bind is skipped");
		assertTrue(tMachine.mCheapOverclocking, "Loader :1241 NBT_CHEAP_OVERCLOCKING T — the :773 fold is refused");
		assertFalse(tMachine.mRequiresIgnition, "no NBT_NEEDS_IGNITION key (the twelve-row reading)");
		assertTrue(tMachine.mNoConstantEnergy, "Loader :1241 NBT_NO_CONSTANT_POWER T");
		assertSame(TD.Energy.QU, tMachine.mEnergyTypeAccepted, "Loader :1241 NBT_ENERGY_ACCEPTED QU");
		assertEquals(1, tMachine.mInput, "the :1241 NBT_INPUT 1");
		assertEquals(1, tMachine.mInputMin, "the :1241 NBT_INPUT_MIN 1 (the explicit-override form)");
		assertEquals(2097152, tMachine.mInputMax, "the :1241 NBT_INPUT_MAX 2097152");
		assertEquals(2, tMachine.mTanksOutput.length, "the output bank re-pointed to the map's TWO fluid-OUT slots (RM.java:144 fluids 1/2/0)");
		RecipeMap tMap = tMachine.recipes();
		assertSame(GT6RecipeMaps.MASSFAB, tMap, "Loader :1241 NBT_RECIPEMAP RM.Massfab");
	}

	// ------------------------------------------------------------------
	// the :199-219 quota arithmetic
	// ------------------------------------------------------------------

	@Test
	public void quotaArithmeticPinsTheFourAndFourGate() {
		assertTrue(TileEntityMassfab.quotaMet(4, 4), "the :219 gate — 4 + 4 passes");
		assertTrue(TileEntityMassfab.quotaMet(5, 4), "5 + 4 passes");
		assertFalse(TileEntityMassfab.quotaMet(3, 5), "3 Control fails");
		assertFalse(TileEntityMassfab.quotaMet(8, 0), "0 Conversion fails (the all-Control stock)");
	}

	// ------------------------------------------------------------------
	// the :46 corner arithmetic (the doubled anchor)
	// ------------------------------------------------------------------

	@Test
	public void cornerArithmeticMirrorsGetOffsetXNMinusTwo() {
		// upstream tX = getOffsetXN(mFacing, 2) - 2 = x - 2*OFFX[facing] - 2 (WorldAndCoords.java:60)
		// the port anchor IS -OFF: corner = coord + 2*anchor - 2
		// facing SOUTH (3): OFFZ=+1, anchorZ=-1 → z - 4 (the core sits 2 BEHIND the front, then -2)
		assertEquals(100 - 4, TileEntityMassfab.corner(100, -1, 2), "facing SOUTH z corner");
		// facing NORTH (2): OFFZ=-1, anchorZ=+1 → z + 0
		assertEquals(100, TileEntityMassfab.corner(100, 1, 2), "facing NORTH z corner");
		// facing EAST (5): OFFX=+1, anchorX=-1 → x - 4
		assertEquals(100 - 4, TileEntityMassfab.corner(100, -1, 2), "facing EAST x corner");
		// facing WEST (4): OFFX=-1, anchorX=+1 → x + 0
		assertEquals(100, TileEntityMassfab.corner(100, 1, 2), "facing WEST x corner");
	}

	// ------------------------------------------------------------------
	// the :45-224 walk over the stub world
	// ------------------------------------------------------------------

	@Test
	public void greenfieldScaffoldFormsAndConsumesExactlyTheStructure() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		TestMassfab tMachine = sMassfabType.create(MFAB_POS, Blocks.BRICKS.defaultBlockState());
		tMachine.setLevel(tLevel);
		tMachine.mFacing = 2; // north — the structure sits south of the controller
		tLevel.mStates.put(MFAB_POS, Blocks.BRICKS.defaultBlockState());
		tLevel.mBlockEntities.put(MFAB_POS, tMachine);
		SimpleContainer tStock = fullStock();

		// pass A places and consumes (the Util :94-99 stale-reference quirk: a cell placed
		// in this pass fails its own arbitration); pass B binds the fresh cells
		tMachine.checkStructure2(null, null, tStock);
		assertTrue(tMachine.checkStructure2(null, null, tStock), "the greenfield scaffold forms (149 cells placed, the self-cell passed)");
		assertStockEmpty(tStock);
		// the linking pass confirms the scaffolded structure with NO inventory (the pure check)
		assertTrue(tMachine.checkStructure2(null, null, null), "the formed structure re-checks green");
		assertTrue(tMachine.checkStructure(true), "the forced re-check flips the cached verdict formed");
	}

	@Test
	public void allControlStockFailsTheConversionQuota() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		TestMassfab tMachine = sMassfabType.create(MFAB_POS, Blocks.BRICKS.defaultBlockState());
		tMachine.setLevel(tLevel);
		tMachine.mFacing = 2;
		tLevel.mStates.put(MFAB_POS, Blocks.BRICKS.defaultBlockState());
		tLevel.mBlockEntities.put(MFAB_POS, tMachine);
		// zero Conversion stock — the 8 quota cells all land Control, countB stays 0
		SimpleContainer tStock = new SimpleContainer(
				new ItemStack(Blocks.BRICKS, 98),
				new ItemStack(Blocks.IRON_BLOCK, 26),
				new ItemStack(Blocks.GOLD_BLOCK, 16),
				new ItemStack(Blocks.DIAMOND_BLOCK, 1),
				new ItemStack(Blocks.EMERALD_BLOCK, 8));
		assertFalse(tMachine.checkStructure2(null, null, tStock), "the :219 quota (tCountB >= 4) refuses the all-Control ring");
	}

	@Test
	public void aWrongPartRejectsTheFormedCheck() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		TestMassfab tMachine = sMassfabType.create(MFAB_POS, Blocks.BRICKS.defaultBlockState());
		tMachine.setLevel(tLevel);
		tMachine.mFacing = 2;
		tLevel.mStates.put(MFAB_POS, Blocks.BRICKS.defaultBlockState());
		tLevel.mBlockEntities.put(MFAB_POS, tMachine);
		SimpleContainer tStock = fullStock();
		tMachine.checkStructure2(null, null, tStock); // pass A: place
		assertTrue(tMachine.checkStructure2(null, null, tStock), "the scaffold forms first (the pass-B link)");

		// one coil cell (the dy=1 ring, one off the core axis) becomes stone — the check rejects
		BlockPos tCoilCell = MFAB_POS.offset(1, 1, 2); // behind the controller, one ring cell of the dy=1 inner ring
		tLevel.mStates.put(tCoilCell, Blocks.STONE.defaultBlockState());
		tLevel.mBlockEntities.remove(tCoilCell);
		assertFalse(tMachine.checkStructure2(null, null, null), "the wrong part fails the pure re-check");
	}

	@Test
	public void aFilledCentreRejectsTheCheck() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		mountPartFactory(tLevel);
		TestMassfab tMachine = sMassfabType.create(MFAB_POS, Blocks.BRICKS.defaultBlockState());
		tMachine.setLevel(tLevel);
		tMachine.mFacing = 2;
		tLevel.mStates.put(MFAB_POS, Blocks.BRICKS.defaultBlockState());
		tLevel.mBlockEntities.put(MFAB_POS, tMachine);
		SimpleContainer tStock = fullStock();
		// the core centre (dx=2, dy=2, dz=2 behind) is pre-filled — fail-not-clear
		BlockPos tCentre = MFAB_POS.offset(0, 2, 2);
		tLevel.mStates.put(tCentre, Blocks.STONE.defaultBlockState());
		assertFalse(tMachine.checkStructure2(null, null, tStock), "the :114 centre must be air");
		assertSame(Blocks.STONE, tLevel.getBlockState(tCentre).getBlock(), "the check never clears the centre (fail-not-clear)");
	}

	// ------------------------------------------------------------------
	// the :252-255 box
	// ------------------------------------------------------------------

	@Test
	public void isInsideStructureCoversTheCoreAndTopRing() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestMassfab tMachine = sMassfabType.create(MFAB_POS, Blocks.BRICKS.defaultBlockState());
		tMachine.setLevel(tLevel);
		tMachine.mFacing = 2; // north: the core axis sits at z + 2 (behind the front)
		assertTrue(tMachine.isInsideStructure(MFAB_POS.getX(), MFAB_POS.getY(), MFAB_POS.getZ() + 2), "the bottom-centre wall cell");
		assertTrue(tMachine.isInsideStructure(MFAB_POS.getX(), MFAB_POS.getY() + 5, MFAB_POS.getZ() + 2), "the top versatile cell");
		assertTrue(tMachine.isInsideStructure(MFAB_POS.getX() + 2, MFAB_POS.getY() + 5, MFAB_POS.getZ() + 2), "the top ring corner");
		assertFalse(tMachine.isInsideStructure(MFAB_POS.getX(), MFAB_POS.getY() + 7, MFAB_POS.getZ() + 2), "above the top ring");
		assertFalse(tMachine.isInsideStructure(MFAB_POS.getX(), MFAB_POS.getY() - 1, MFAB_POS.getZ() + 2), "below the controller");
	}

	// ------------------------------------------------------------------
	// the BasicMachine:489-517 energy-injection window
	// ------------------------------------------------------------------

	@Test
	public void theEnergyFaceInjectsQuThroughTheWindow() {
		TestMassfab tMachine = sMassfabType.create(MFAB_POS, Blocks.BRICKS.defaultBlockState());
		// a small accepted packet: 10 packets of 64 QU → 640 stored, 10 consumed
		assertEquals(10, tMachine.doInject(TD.Energy.QU, (byte)2, 64, 10, true), "the :504 consumed-packet count");
		assertEquals(640, tMachine.mEnergy, "the :503 stored energy");
		// a wrong-type packet is refused
		assertEquals(0, tMachine.doInject(TD.Energy.EU, (byte)2, 64, 10, true), "only QU is accepted (:500)");
		assertEquals(640, tMachine.mEnergy, "the refusal stored nothing");
		// an oversize packet is refused (the declared no-overcharge narrowing of :493-496)
		assertEquals(0, tMachine.doInject(TD.Energy.QU, (byte)2, 2097153, 1, true), "size > mInputMax refuses");
		// the input-size window queries ride the :513-515 trio
		assertEquals(1, tMachine.getEnergySizeInputMin(TD.Energy.QU, (byte)2));
		assertEquals(1, tMachine.getEnergySizeInputRecommended(TD.Energy.QU, (byte)2));
		assertEquals(2097152, tMachine.getEnergySizeInputMax(TD.Energy.QU, (byte)2));
		// the stopped machine accepts nothing (:490)
		tMachine.setStateOnOff(false);
		assertEquals(0, tMachine.doInject(TD.Energy.QU, (byte)2, 64, 10, true), "a stopped machine refuses (:490)");
	}
}
