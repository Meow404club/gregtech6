package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.data.TD;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.RecipeMap;

/**
 * The Implosion Compressor offline acceptance (task p31-implosion): the pattern
 * declaration pins (the 26 dense-wall cells + the hollow air centre at (0, +1, 0) —
 * the shell centred front+up of the controller, the MultiTileEntityImplosionCompressor
 * :46-60 geometry IDENTICAL to the Autoclave except the wall id), the :1228
 * registration-config pins (PARALLEL 64, TU, the 1..16 window, no ignition, no constant
 * power), the map binding and the isInsideStructure box (:78-81), plus the four-facing
 * cellOffset arithmetic (the hollow lands at front+up for EVERY live facing — the
 * p27 facing-integrity invariant).
 */
public class TileEntityImplosionCompressorTest extends GTMultiBlocksOfflineTestBase {

	/** The controller cell for this suite — away from the shared C1/C2 arbitration fixtures. */
	static final BlockPos IMP_POS = new BlockPos(220, 64, 220);

	static BlockEntityType<TestImplosion> sImplosionType;

	/** The concrete test BE — the implosion controller over a vanilla-block BET, the wall bound to BRICKS. */
	public static final class TestImplosion extends TileEntityImplosionCompressor {
		public TestImplosion(BlockPos aPos, BlockState aState) {
			super(sImplosionType, aPos, aState);
		}
		public TestImplosion(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}
		@Override
		protected Block getPartBlock() {
			return Blocks.BRICKS;
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildImplosionFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		BlockEntityType<TestImplosion>[] tHolder = (BlockEntityType<TestImplosion>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(TestImplosion::new, Blocks.BRICKS).build(null);
		sImplosionType = tHolder[0];
	}

	// ------------------------------------------------------------------
	// the pattern declaration — the :46-60 geometry
	// ------------------------------------------------------------------

	@Test
	public void patternIsTheLiteralUpstreamLoopPlusHollowCentreOneUp() {
		TestImplosion tMachine = sImplosionType.create(IMP_POS, Blocks.BRICKS.defaultBlockState());
		GTMultiBlockPattern tPattern = tMachine.getStructurePattern();
		assertNotNull(tPattern, "the implosion compressor declares a pattern");

		var tCells = tPattern.cells();
		assertEquals(27, tCells.size(), "26 walls + the hollow centre");

		BlockState tBricks = Blocks.BRICKS.defaultBlockState();
		BlockState tAir = Blocks.AIR.defaultBlockState();
		// the upstream :50-55 loop order (i outer / j middle / k inner), each cell declared (i, j+1, k) — the +1 Y bake
		int tIndex = 0;
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue; // the centre is declared LAST, hollow
			GTMultiBlockPattern.Cell tCell = tCells.get(tIndex++);
			assertEquals(i, tCell.x, "cell x");
			assertEquals(j + 1, tCell.y, "cell y — the tY = yCoord + 1 bake");
			assertEquals(k, tCell.z, "cell z");
			assertFalse(tCell.isHollow(), "the cell is a structural part");
			assertTrue(tCell.forms(), "the cell carries the FULL forming expectation");
			assertSame(Blocks.BRICKS, tCell.partBlock, "the part block is the getPartBlock hook (production: dense_wall_tungstensteel 18023)");
			assertEquals(MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, tCell.usage, "the :54 usage mask (item+fluid+energy)");
			assertEquals(0, tCell.design, "the :54 design 0");
			assertTrue(tCell.matches(tBricks), "the cell judges the wall block");
			assertFalse(tCell.matches(tAir), "the cell judges air a failure");
		}
		// the centre: appended LAST (the Coke Oven draw order — 26 walls, then the grey marker)
		GTMultiBlockPattern.Cell tCentre = tCells.get(26);
		assertEquals(0, tCentre.x);
		assertEquals(1, tCentre.y, "the hollow rides the +1 Y bake (the :52 centre at tY = yCoord + 1)");
		assertEquals(0, tCentre.z);
		assertTrue(tCentre.isHollow(), "the centre is the keep-this-hollow marker");
		assertTrue(tCentre.matches(tAir), "the centre judgement is AIR (non-air centre fails)");
		assertFalse(tCentre.matches(tBricks), "the centre rejects a filled cell");
	}

	@Test
	public void boundsFoldToTheShiftedShell() {
		TestImplosion tMachine = sImplosionType.create(IMP_POS, Blocks.BRICKS.defaultBlockState());
		GTMultiBlockPattern tPattern = tMachine.getStructurePattern();
		assertEquals(-1, tPattern.minX()); assertEquals(1, tPattern.maxX());
		assertEquals(0, tPattern.minY());  assertEquals(2, tPattern.maxY(), "the shell spans controller Y..Y+2 (the +1 centre bake)");
		assertEquals(-1, tPattern.minZ()); assertEquals(1, tPattern.maxZ());
	}

	// ------------------------------------------------------------------
	// the :1228 registration-config pins
	// ------------------------------------------------------------------

	@Test
	public void registrationConfigPins() {
		TestImplosion tMachine = sImplosionType.create(IMP_POS, Blocks.BRICKS.defaultBlockState());
		assertEquals(64, tMachine.mParallel, "Loader :1228 NBT_PARALLEL 64");
		assertFalse(tMachine.mRequiresIgnition, "no NBT_NEEDS_IGNITION key (the twelve-row reading)");
		assertTrue(tMachine.mNoConstantEnergy, "Loader :1228 NBT_NO_CONSTANT_POWER T");
		assertSame(TD.Energy.TU, tMachine.mEnergyTypeAccepted, "Loader :1228 NBT_ENERGY_ACCEPTED TU");
		assertEquals(1, tMachine.mInputMin, "the :1228 1..16 window (min)");
		assertEquals(1, tMachine.mInput, "the :1228 1..16 window (input)");
		assertEquals(16, tMachine.mInputMax, "the :1228 1..16 window (max)");
	}

	@Test
	public void mapBindingIsImplosion() {
		GT6RecipeMaps.init();
		TestImplosion tMachine = sImplosionType.create(IMP_POS, Blocks.BRICKS.defaultBlockState());
		RecipeMap tMap = tMachine.recipes();
		assertNotNull(tMap, "the map resolves");
		assertSame(GT6RecipeMaps.IMPLOSION, tMap, "the :1228 NBT_RECIPEMAP binding (the lazy override shape)");
		assertEquals("gt.recipe.implosioncompressor", tMap.mNameInternal, "RM.java:86 map name");
	}

	// ------------------------------------------------------------------
	// the box and the four-facing arithmetic
	// ------------------------------------------------------------------

	@Test
	public void isInsideStructureIsTheUpstreamBox() {
		TestImplosion tMachine = sImplosionType.create(IMP_POS, Blocks.BRICKS.defaultBlockState());
		// facing north (2): the shell centre sits one cell south (getOffsetZN) and one up
		int tX = IMP_POS.getX(), tY = IMP_POS.getY(), tZ = IMP_POS.getZ();
		assertTrue(tMachine.isInsideStructure(tX, tY, tZ), "the controller cell is inside its own box");
		assertTrue(tMachine.isInsideStructure(tX, tY + 1, tZ + 1), "the shell centre (front+up) is inside");
		assertTrue(tMachine.isInsideStructure(tX - 1, tY + 2, tZ + 2), "the far top corner of the shell is inside");
		assertFalse(tMachine.isInsideStructure(tX + 2, tY + 1, tZ + 1), "two across is outside");
		assertFalse(tMachine.isInsideStructure(tX, tY + 3, tZ + 1), "three up is outside");
		assertFalse(tMachine.isInsideStructure(tX, tY + 1, tZ - 1), "one BEHIND the facing is outside");
	}

	@Test
	public void hollowLandsAtFrontUpForEveryLiveFacing() {
		// the p27 facing-integrity invariant, pure-arithmetic form: cellOffset(f, 0, 1, 0)
		// must equal (getOffsetXN, +1, getOffsetZN) relative to the controller — the
		// centre-relative declaration walked at mFacing (the Coke Oven default walk)
		// lands the hollow at front+up for EVERY live facing.
		TestImplosion tMachine = sImplosionType.create(IMP_POS, Blocks.BRICKS.defaultBlockState());
		for (byte tFacing = 2; tFacing <= 5; tFacing++) {
			tMachine.mFacing = tFacing;
			int tFrontX = tMachine.getOffsetXN(tFacing) - IMP_POS.getX();
			int tFrontZ = tMachine.getOffsetZN(tFacing) - IMP_POS.getZ();
			int[] tHollow = GTMultiBlockPattern.cellOffset(tFacing, 0, 1, 0);
			assertEquals(tFrontX, tHollow[0], "facing " + tFacing + ": the hollow x lands at the facing front");
			assertEquals(1, tHollow[1], "facing " + tFacing + ": the hollow y lands one above");
			assertEquals(tFrontZ, tHollow[2], "facing " + tFacing + ": the hollow z lands at the facing front");
		}
	}

	// ------------------------------------------------------------------
	// the recipe walk e2e (level-less — the machine face over the poured fixture map)
	// ------------------------------------------------------------------

	/**
	 * The three-input walk: dust + TNT + selector(0) in slots 0..2 consumes to the tier-0
	 * output landing on slot 3 (the map-aware canOutput — the base's fixed slot-1 walk
	 * would have read the TNT input slot as the output), eUt 0 → minEnergy 1, duration
	 * 256, and the never-consumed selector. The resolvers ride the
	 * {@link gregtech6.recipes.GT6RecipesImplosion} fixture seams (the Distillery test
	 * convention); the production item identities are the RCON chain's proof.
	 */
	@Test
	public void recipeWalkConsumesAndLandsTheOutput() {
		// pour the fixture map — capture and restore every seam (the Distillery test form)
		java.util.function.BiFunction<gregapi.oredict.OreDictPrefix, gregapi.oredict.OreDictMaterial, net.minecraft.world.item.Item> tDefaultResolver =
				gregtech6.recipes.GT6RecipesImplosion.sMaterialItemResolver;
		java.util.function.Supplier<net.minecraft.world.item.Item> tDefaultTnt = gregtech6.recipes.GT6RecipesImplosion.sTntResolver;
		java.util.function.Function<Integer, ItemStack> tDefaultCircuit = gregtech6.recipes.GT6RecipesImplosion.sCircuitResolver;
		java.util.function.Predicate<ItemStack> tDefaultNotConsumable = gregtech6.recipes.Recipe.sNotConsumable;
		gregtech6.recipes.GT6RecipesImplosion.sMaterialItemResolver = gregtech6.recipes.GT6RecipesImplosionTest.FIXTURE_RESOLVER;
		gregtech6.recipes.GT6RecipesImplosion.sTntResolver = () -> net.minecraft.world.item.Items.TNT;
		gregtech6.recipes.GT6RecipesImplosion.sCircuitResolver = gregtech6.recipes.GT6RecipesImplosionTest::fixtureCircuit;
		gregtech6.recipes.Recipe.sNotConsumable = gregtech6.recipes.GT6RecipesImplosionTest.FIXTURE_NOT_CONSUMABLE;
		try {
			gregtech6.recipes.GT6RecipeMaps.init();
			gregtech6.recipes.GT6RecipesImplosion.resetForTest();
			gregtech6.recipes.GT6RecipesImplosion.load();

			TestImplosion tMachine = sImplosionType.create(IMP_POS, Blocks.BRICKS.defaultBlockState());
			tMachine.mInventory.setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.CLAY_BALL, 1)); // the dust stand-in
			tMachine.mInventory.setStackInSlot(1, new ItemStack(net.minecraft.world.item.Items.TNT, 8));
			tMachine.mInventory.setStackInSlot(2, gregtech6.recipes.GT6RecipesImplosionTest.fixtureCircuit(0));

			assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE,
					tMachine.checkRecipe(true, false), "the tier-0 row applies");
			assertEquals(1, tMachine.mMinEnergy, "eUt 0 → the TU constant-per-process floor of 1");
			assertEquals(256, tMachine.mMaxProgress, "duration 256 at the zero-overclock window (mInputMin 1)");
			assertEquals(0, tMachine.mInventory.getStackInSlot(0).getCount(), "the dust consumed");
			assertEquals(0, tMachine.mInventory.getStackInSlot(1).getCount(), "the TNT consumed");
			assertEquals(1, tMachine.mInventory.getStackInSlot(2).getCount(), "the selector never consumed");
			assertNotNull(tMachine.mOutputItems, "the pending output is staged");
			assertEquals(net.minecraft.world.item.Items.BRICK, tMachine.mOutputItems[0].getItem(), "the plateGem stand-in output");
		} finally {
			gregtech6.recipes.GT6RecipesImplosion.sMaterialItemResolver = tDefaultResolver;
			gregtech6.recipes.GT6RecipesImplosion.sTntResolver = tDefaultTnt;
			gregtech6.recipes.GT6RecipesImplosion.sCircuitResolver = tDefaultCircuit;
			gregtech6.recipes.Recipe.sNotConsumable = tDefaultNotConsumable;
			gregtech6.recipes.GT6RecipesImplosion.resetForTest();
			gregtech6.recipes.GT6RecipeMaps.reset();
		}
	}
}
