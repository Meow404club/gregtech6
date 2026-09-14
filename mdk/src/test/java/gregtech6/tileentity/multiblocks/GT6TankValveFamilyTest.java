package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregtech6.fluid.GTFluidLists;
import gregtech6.registry.GT6Tanks;
import gregtech6.tileentity.attachment.GTAttachmentSmallBlockEntity;

/**
 * The Tank Main Valve family offline gate (task p29-w3-tank-valves). The row table is
 * re-pinned COLUMN BY COLUMN against the Loader_MultiTileEntities.java:1195-1222 lines
 * (the 25-line census — the 26-vs-25 correction, decisions.p29-w3-split-rulings); the
 * structure is the hollow 3x3x3/5x5x5 ONLY_FLUID wall walk; the four-flag destruction
 * chain, the meltdown, the allowFluid gate and the auto-emit run through the fixture
 * seams (the mutable name sets are the declared test-injection surface, the boiler
 * offline-seam shape). NO indexOf/first-marker probing — every lookup is by row path,
 * position or set membership.
 */
public class GT6TankValveFamilyTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<TestTank> sTankType;

	/** The offline tank: BRICKS walls (the fixture part block), seam-driven physics. */
	static class TestTank extends GTTankValveBlockEntity {
		int mRadius = 1;
		boolean mOnlySimple = false;

		TestTank(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "test_multiblock_tank_valve";
		}

		@Override
		protected net.minecraft.world.level.block.Block getWallBlock() {
			return Blocks.BRICKS;
		}

		@Override
		public int radius() {
			return mRadius;
		}

		@Override
		public boolean onlySimple() {
			return mOnlySimple;
		}
	}

	/** A counting fluid sink (the emit target stub — IFluidHandler is offline-safe). */
	static class FluidSink implements IFluidHandler {
		int mAccepted = 0;
		final int mCapacity;

		FluidSink(int aCapacity) {
			mCapacity = aCapacity;
		}

		@Override public int getTanks() {return 1;}
		@Override public net.minecraftforge.fluids.FluidStack getFluidInTank(int aTank) {return net.minecraftforge.fluids.FluidStack.EMPTY;}
		@Override public int getTankCapacity(int aTank) {return mCapacity;}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {return true;}
		@Override public int fill(FluidStack aResource, FluidAction aAction) {
			int tRoom = mCapacity - mAccepted;
			int tTake = Math.min(tRoom, aResource == null ? 0 : aResource.getAmount());
			if (aAction.execute()) mAccepted += tTake;
			return tTake;
		}
		@Override public net.minecraftforge.fluids.FluidStack drain(FluidStack aResource, FluidAction aAction) {return net.minecraftforge.fluids.FluidStack.EMPTY;}
		@Override public net.minecraftforge.fluids.FluidStack drain(int aMaxDrain, FluidAction aAction) {return net.minecraftforge.fluids.FluidStack.EMPTY;}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildTankFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// the GTMultiBlocksOfflineTestBase posture
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		BlockEntityType<TestTank>[] tHolder = (BlockEntityType<TestTank>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new TestTank(tHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		sTankType = tHolder[0];
	}

	@AfterAll
	static void restoreInjectedSets() {
		// the sets are shared statics — every injection this suite made is retracted
		GTTankValveBlockEntity.SIMPLE.remove("water");
		GTTankValveBlockEntity.PLASMA.remove("water");
		GTTankValveBlockEntity.MAGIC.remove("water");
		GTTankValveBlockEntity.SIMPLE.remove("lava");
		GTFluidLists.POWER_CONDUCTING.remove("water");
		GTFluidLists.GAS.remove("water");
		GTAttachmentSmallBlockEntity.Categories.ACIDS.remove("minecraft:water");
	}

	// ---------------------------------------------------------------------------
	// ① the row census — the Loader :1195-1222 table, column by column
	// ---------------------------------------------------------------------------

	@Test
	public void rowCensusTwentyFiveAndCapacityLadder() {
		assertEquals(25, GT6Tanks.ROWS.size(), "the 25-valve correction (1 wood + 24 metal, :1195-1222)");
		// the capacity ladder, registration order == loader line order
		long[] tCapacities = {
				432000,
				1728000, 1728000, 3456000, 6912000, 6912000, 110592000,
				6912000, 6912000, 13824000, 27648000, 27648000, 442368000,
				8000000, 8000000, 16000000, 32000000, 32000000, 512000000,
				32000000, 32000000, 64000000, 128000000, 128000000, 2048000000};
		int[] tMetaIds = {
				17001,
				17002, 17007, 17006, 17003, 17004, 17005,
				17022, 17027, 17026, 17023, 17024, 17025,
				17042, 17047, 17046, 17043, 17044, 17045,
				17062, 17067, 17066, 17063, 17064, 17065};
		int[] tSizes = {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5};
		for (int i = 0; i < 25; i++) {
			GT6Tanks.TankValveRow tRow = GT6Tanks.ROWS.get(i);
			assertEquals(tCapacities[i], tRow.capacity(), "capacity ladder rung " + i + " (" + tRow.path() + ")");
			assertEquals(tMetaIds[i], tRow.metaId(), "meta id rung " + i + " (" + tRow.path() + ")");
			assertEquals(tSizes[i], tRow.size(), "size rung " + i + " (" + tRow.path() + ")");
			assertNotNull(GT6Tanks.BLOCKS_BY_PATH.get(tRow.path()), "block registered: " + tRow.path());
			assertNotNull(GT6Tanks.ITEMS_BY_PATH.get(tRow.path()), "item registered: " + tRow.path());
		}
	}

	@Test
	public void rowWallChoicesAndProofColumns() {
		// the NBT_DESIGN wall column + the four proof flags, per loader line
		Object[][] tExpected = {
				// path, wallPath, gas, acid, plasma, magic, flammable, onlySimple
				{"tank_wood", "wood_wall", false, false, false, false, true, true},
				{"tank_small_stainless_steel", "machine_wall_stainless_steel", true, true, false, false, false, false},
				{"tank_small_invar", "machine_wall_invar", true, false, false, false, false, false},
				{"tank_small_titanium", "machine_wall_titanium", true, false, false, false, false, false},
				{"tank_small_tungstensteel", "machine_wall_tungstensteel", true, false, false, true, false, false},
				{"tank_small_tungsten", "machine_wall_tungsten", true, true, false, true, false, false},
				{"tank_small_adamantium", "machine_wall_adamantium", true, true, true, true, false, false},
				{"tank_small_dense_stainless_steel", "dense_wall_stainless_steel", true, true, false, false, false, false},
				{"tank_small_dense_invar", "dense_wall_invar", true, false, false, false, false, false},
				{"tank_small_dense_titanium", "dense_wall_titanium", true, false, false, false, false, false},
				{"tank_small_dense_tungstensteel", "dense_wall_tungstensteel", true, false, false, true, false, false},
				{"tank_small_dense_tungsten", "dense_wall_tungsten", true, true, false, true, false, false},
				{"tank_small_dense_adamantium", "dense_wall_adamantium", true, true, true, true, false, false},
				{"tank_large_stainless_steel", "machine_wall_stainless_steel", true, true, false, false, false, false},
				{"tank_large_invar", "machine_wall_invar", true, false, false, false, false, false},
				{"tank_large_titanium", "machine_wall_titanium", true, false, false, false, false, false},
				{"tank_large_tungstensteel", "machine_wall_tungstensteel", true, false, false, true, false, false},
				{"tank_large_tungsten", "machine_wall_tungsten", true, true, false, true, false, false},
				{"tank_large_adamantium", "machine_wall_adamantium", true, true, true, true, false, false},
				{"tank_large_dense_stainless_steel", "dense_wall_stainless_steel", true, true, false, false, false, false},
				{"tank_large_dense_invar", "dense_wall_invar", true, false, false, false, false, false},
				{"tank_large_dense_titanium", "dense_wall_titanium", true, false, false, false, false, false},
				{"tank_large_dense_tungstensteel", "dense_wall_tungstensteel", true, false, false, true, false, false},
				{"tank_large_dense_tungsten", "dense_wall_tungsten", true, true, false, true, false, false},
				{"tank_large_dense_adamantium", "dense_wall_adamantium", true, true, true, true, false, false},
		};
		Set<String> tSeen = new HashSet<>();
		for (Object[] tLine : tExpected) {
			String tPath = (String) tLine[0];
			GT6Tanks.TankValveRow tRow = findByPath(tPath);
			tSeen.add(tPath);
			assertEquals(tLine[1], tRow.wallPath(), "wall choice of " + tPath);
			assertEquals(tLine[2], tRow.gasProof(), "gasproof of " + tPath);
			assertEquals(tLine[3], tRow.acidProof(), "acidproof of " + tPath);
			assertEquals(tLine[4], tRow.plasmaProof(), "plasmaproof of " + tPath);
			assertEquals(tLine[5], tRow.magicProof(), "magicproof of " + tPath);
			assertEquals(tLine[6], tRow.flammable(), "flammable of " + tPath);
			assertEquals(tLine[7], tRow.onlySimple(), "onlySimple of " + tPath);
		}
		assertEquals(25, tSeen.size(), "every row pinned exactly once");
	}

	private static GT6Tanks.TankValveRow findByPath(String aPath) {
		for (GT6Tanks.TankValveRow tRow : GT6Tanks.ROWS) {
			if (tRow.path().equals(aPath)) return tRow;
		}
		throw new AssertionError("row missing from the table: " + aPath);
	}

	// ---------------------------------------------------------------------------
	// ② the structure — the hollow 3x3x3 and 5x5x5 ONLY_FLUID walls
	// ---------------------------------------------------------------------------

	/** The valve at (100,64,100) facing north — the centre sits at (100,64,101), the controller cell at centre-relative (0,0,-1). */
	private static final BlockPos V = new BlockPos(100, 64, 100);
	private static final BlockPos CENTRE3 = new BlockPos(100, 64, 101);

	/** Places the 25 wall BEs of the 3x3x3 shell (every cell around CENTRE3 except the centre and the valve cell). */
	private static void placeShell3(MultiBlockLevel aLevel, net.minecraft.world.level.block.Block aWall) {
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue; // the hollow centre
			BlockPos tPos = new BlockPos(CENTRE3.getX() + i, CENTRE3.getY() + j, CENTRE3.getZ() + k);
			if (tPos.equals(V)) continue; // the valve's own cell — the self-cell arm
			placePart(aLevel, tPos);
			aLevel.mStates.put(tPos, aWall.defaultBlockState());
		}
	}

	private static TestTank placedTank(MultiBlockLevel aLevel, byte aFacing, int aRadius) {
		TestTank tTank = sTankType.create(V, Blocks.BRICKS.defaultBlockState());
		tTank.setLevel(aLevel);
		tTank.mFacing = aFacing;
		tTank.mRadius = aRadius;
		tTank.setRngOverride(() -> 0); // the deterministic sweep/fire arms
		aLevel.mStates.put(V, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(V, tTank);
		return tTank;
	}

	@Test
	public void structure3x3x3FormsAndBindsOnlyFluidParts() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
		placeShell3(tLevel, Blocks.BRICKS);
		assertTrue(tTank.checkStructure(true), "the hollow 3x3x3 of walls forms");
		// the wall cells carry ONLY_FLUID + design 0 (the :69 verbatim arguments)
		BlockPos tProbe = new BlockPos(CENTRE3.getX() - 1, CENTRE3.getY(), CENTRE3.getZ()); // (99,64,101), not the valve cell
		MultiBlockPartBlockEntity tPart = (MultiBlockPartBlockEntity) tLevel.mBlockEntities.get(tProbe);
		assertNotNull(tPart, "the wall cell carries a part BE");
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID, tPart.mMode, "the wall mode is ONLY_FLUID");
		assertEquals(0, tPart.mDesign, "the design argument is the literal 0 (:69)");
		// the declared pattern exists for the 3x3x3 and declares 27 cells (26 walls + hollow)
		assertNotNull(tTank.getStructurePattern(), "the 3x3x3 binds a pattern");
		assertEquals(27, tTank.getStructurePattern().cells().size(), "26 forming cells + the hollow centre");
	}

	@Test
	public void structure3x3x3RejectsWrongWall() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
		placeShell3(tLevel, Blocks.STONE); // the wrong wall material — the form must refuse
		assertFalse(tTank.checkStructure(true), "a shell of the wrong material does not form (the design=wall-choice semantics)");
	}

	@Test
	public void structure3x3x3RejectsNonAirCentre() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
		placeShell3(tLevel, Blocks.BRICKS);
		tLevel.mStates.put(CENTRE3, Blocks.STONE.defaultBlockState()); // the hollow centre blocked
		assertFalse(tTank.checkStructure(true), "a non-air centre fails the check (fail-not-clear)");
	}

	/** Places the 97 wall BEs of the 5x5x5 shell (125 cells minus the 27-cell hollow minus the valve cell). */
	private static int placeShell5(MultiBlockLevel aLevel, net.minecraft.world.level.block.Block aWall) {
		BlockPos tCentre5 = V.relative(Direction.NORTH, -2); // (100,64,102)
		int tWalls = 0;
		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) for (int k = -2; k <= 2; k++) {
			if (i * i <= 1 && j * j <= 1 && k * k <= 1) continue; // the inner 3x3x3 hollow (:66)
			BlockPos tPos = new BlockPos(tCentre5.getX() + i, tCentre5.getY() + j, tCentre5.getZ() + k);
			if (tPos.equals(V)) continue;
			placePart(aLevel, tPos);
			aLevel.mStates.put(tPos, aWall.defaultBlockState());
			tWalls++;
		}
		return tWalls;
	}

	@Test
	public void structure5x5x5FormsAndPatternStaysUnbound() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 2);
		assertEquals(97, placeShell5(tLevel, Blocks.BRICKS), "125 - 27 hollow - 1 controller cell = 97 walls");
		assertTrue(tTank.checkStructure(true), "the hollow 5x5x5 forms");
		assertNull(tTank.getStructurePattern(), "the distance-2 anchor stays pattern-less (the class-doc ruling)");
	}

	@Test
	public void structure5x5x5RejectsWrongWall() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 2);
		placeShell5(tLevel, Blocks.STONE);
		assertFalse(tTank.checkStructure(true), "the wrong-material 5x5x5 shell refuses");
	}

	// ---------------------------------------------------------------------------
	// ③ the fluid verdicts — allowFluid, the four flags, the meltdown
	// ---------------------------------------------------------------------------

	@Test
	public void allowFluidGatesPowerConductingTemperatureAndSimple() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
		tTank.setMeltingPointOverride(500); // the WoodTreated heat() literal
		FluidStack tWater = new FluidStack(Fluids.WATER, 1000);

		tTank.mOnlySimple = false;
		assertTrue(tTank.allowFluid(tWater), "water past a non-simple tank (300K < 500K, not power conducting)");
		GTFluidLists.POWER_CONDUCTING.add("water");
		assertFalse(tTank.allowFluid(tWater), "power conducting fluids refuse (:102)");
		GTFluidLists.POWER_CONDUCTING.remove("water");
		tTank.setMeltingPointOverride(200);
		assertFalse(tTank.allowFluid(tWater), "a fluid hotter than the melting point refuses (:102)");
		tTank.setMeltingPointOverride(500);
		tTank.mOnlySimple = true;
		GTTankValveBlockEntity.SIMPLE.add("water");
		assertTrue(tTank.allowFluid(tWater), "a listed simple fluid passes the onlySimple gate");
		GTTankValveBlockEntity.SIMPLE.remove("water");
		assertFalse(tTank.allowFluid(tWater), "an unlisted fluid refuses the onlySimple tank (:102)");
	}

	@Test
	public void gasFlagTrashesUnprotectedAndSparesProtected() {
		GTFluidLists.GAS.add("water"); // the injection — the PATH-keyed name list (the declared test surface)
		// the isGas verdict ALSO reads the density sign through the FULL-key Categories.GASES
		GTAttachmentSmallBlockEntity.Categories.GASES.add("minecraft:water");
		try {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
			placeShell3(tLevel, Blocks.BRICKS);
			tTank.checkStructure(true);
			tTank.mTank.fill(new FluidStack(Fluids.WATER, 5000), FluidAction.EXECUTE);
			tTank.mGasProof = false;
			tTank.onTick(1, true);
			assertEquals(0, tTank.mTank.amount(), "the :115-117 gas arm voids the unprotected tank");
			assertEquals(V, tTank.getBlockPos(), "the valve survives (gas voids content only, no destruction)");

			MultiBlockLevel tLevel2 = new MultiBlockLevel();
			TestTank tSafe = placedTank(tLevel2, (byte)Direction.NORTH.get3DDataValue(), 1);
			placeShell3(tLevel2, Blocks.BRICKS);
			tSafe.checkStructure(true);
			tSafe.mTank.fill(new FluidStack(Fluids.WATER, 5000), FluidAction.EXECUTE);
			tSafe.mGasProof = true; // the SS column
			tSafe.onTick(1, true);
			assertEquals(5000, tSafe.mTank.amount(), "the gas-proof tank keeps its content");
		} finally {
			GTFluidLists.GAS.remove("water");
			GTAttachmentSmallBlockEntity.Categories.GASES.remove("minecraft:water");
		}
	}

	@Test
	public void acidFlagTrashesAndDissolvesProtectedSurvives() {
		GTAttachmentSmallBlockEntity.Categories.ACIDS.add("minecraft:water"); // the injection (the FULL-key form the Categories sets key on; the empty ACIDS list is the unbuilt dataset bridge)
		try {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
			placeShell3(tLevel, Blocks.BRICKS);
			tTank.checkStructure(true);
			tTank.mTank.fill(new FluidStack(Fluids.WATER, 5000), FluidAction.EXECUTE);
			tTank.mAcidProof = false;
			tTank.onTick(1, true); // the rng(3)==0 override dissolves EVERY wall cell
			assertEquals(0, tTank.mTank.amount(), "the :101-110 acid arm voids the tank");
			assertTrue(tLevel.getBlockState(V).isAir(), "the valve itself goes to air (:108 setToAir)");
			assertTrue(tLevel.getBlockState(new BlockPos(CENTRE3.getX() - 1, CENTRE3.getY(), CENTRE3.getZ())).isAir(),
					"the wall cells dissolve (:106)");

			MultiBlockLevel tLevel2 = new MultiBlockLevel();
			TestTank tSafe = placedTank(tLevel2, (byte)Direction.NORTH.get3DDataValue(), 1);
			placeShell3(tLevel2, Blocks.BRICKS);
			tSafe.checkStructure(true);
			tSafe.mTank.fill(new FluidStack(Fluids.WATER, 5000), FluidAction.EXECUTE);
			tSafe.mAcidProof = true; // the SS/W/Ad column
			tSafe.onTick(1, true);
			assertEquals(5000, tSafe.mTank.amount(), "the acid-proof tank survives");
			assertFalse(tLevel2.getBlockState(V).isAir(), "the acid-proof valve is untouched");
		} finally {
			GTAttachmentSmallBlockEntity.Categories.ACIDS.remove("minecraft:water");
		}
	}

	@Test
	public void plasmaAndMagicAndAllowFluidArmsTrash() {
		GTTankValveBlockEntity.PLASMA.add("water");
		try {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
			placeShell3(tLevel, Blocks.BRICKS);
			tTank.checkStructure(true);
			tTank.mTank.fill(new FluidStack(Fluids.WATER, 5000), FluidAction.EXECUTE);
			tTank.onTick(1, true);
			assertEquals(0, tTank.mTank.amount(), "the :111-113 plasma arm voids the unprotected tank");
		} finally {
			GTTankValveBlockEntity.PLASMA.remove("water");
		}

		GTTankValveBlockEntity.MAGIC.add("water");
		try {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
			placeShell3(tLevel, Blocks.BRICKS);
			tTank.checkStructure(true);
			tTank.mTank.fill(new FluidStack(Fluids.WATER, 5000), FluidAction.EXECUTE);
			tTank.onTick(1, true);
			assertEquals(0, tTank.mTank.amount(), "the :91-100 magic arm voids the unprotected tank (the flux scatter is the unported cut)");
		} finally {
			GTTankValveBlockEntity.MAGIC.remove("water");
		}

		GTFluidLists.POWER_CONDUCTING.add("water");
		try {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
			placeShell3(tLevel, Blocks.BRICKS);
			tTank.checkStructure(true);
			tTank.mTank.fill(new FluidStack(Fluids.WATER, 5000), FluidAction.EXECUTE);
			tTank.onTick(1, true);
			assertEquals(0, tTank.mTank.amount(), "the :119-121 allowFluid arm voids the power-conducting fill");
		} finally {
			GTFluidLists.POWER_CONDUCTING.remove("water");
		}
	}

	@Test
	public void meltdownFiresOnOverheatAndLeavesTheLavaBlock() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
		placeShell3(tLevel, Blocks.BRICKS);
		tTank.checkStructure(true);
		tTank.setMeltingPointOverride(200); // below the guarded offline ambient (the temperature seam)
		tTank.mTank.fill(new FluidStack(Fluids.LAVA, 2000), FluidAction.EXECUTE); // the vanilla lava identity
		tTank.onTick(1, true);
		// :136 — the 3x3x3 lava arm leaves a lava block in the centre; :138 — the valve to fire
		assertTrue(tLevel.getBlockState(CENTRE3).is(Blocks.LAVA), "the centre keeps the :136 lava block");
		assertTrue(tLevel.getBlockState(V).is(Blocks.FIRE), "the valve ends in fire (:138 setToFire)");
		assertEquals(0, tTank.mTank.amount(), "the meltdown trashes the content (:137)");
	}

	@Test
	public void meltdown5x5x5HasNoLavaArm() {
		// Tank5x5x5.java:130-139 carries NO :136 lava arm (Tank3x3x3.java:136 only — the
		// review-round red item: the first cut ran the arm at both radii) — the 5x5x5
		// meltdown leaves the hollow centre AIR and only fires/trashes
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 2);
		placeShell5(tLevel, Blocks.BRICKS);
		tTank.checkStructure(true);
		tTank.setMeltingPointOverride(200);
		tTank.mTank.fill(new FluidStack(Fluids.LAVA, 2000), FluidAction.EXECUTE);
		tTank.onTick(1, true);
		BlockPos tCentre5 = V.relative(Direction.NORTH, -2); // (100,64,102)
		// the lava arm is radius-1 only — the centre never becomes a lava block (the
		// deterministic rng(4)==0 override DOES place fire in the hollow cells, the
		// upstream :134 verbatim — so the negative is the LAVA absence, not air)
		assertFalse(tLevel.getBlockState(tCentre5).is(Blocks.LAVA), "the 5x5x5 centre never becomes lava — NO lava arm at radius 2");
		assertTrue(tLevel.getBlockState(V).is(Blocks.FIRE), "the valve still ends in fire (the shared :138 face)");
		assertEquals(0, tTank.mTank.amount(), "the meltdown trashes the content (the shared :137 face)");
	}

	@Test
	public void underTemperatureTankKeepsItsContent() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
		placeShell3(tLevel, Blocks.BRICKS);
		tTank.checkStructure(true);
		tTank.setMeltingPointOverride(5000); // far above the guarded offline ambient
		tTank.mTank.fill(new FluidStack(Fluids.WATER, 1234), FluidAction.EXECUTE);
		tTank.onTick(1, true);
		assertEquals(1234, tTank.mTank.amount(), "a cool, allowed, proofed fill survives the tick");
	}

	// ---------------------------------------------------------------------------
	// ④ the auto-emit and the funnel/tap faces
	// ---------------------------------------------------------------------------

	@Test
	public void autoEmitMovesIntoTheFacingNeighbour() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
		placeShell3(tLevel, Blocks.BRICKS);
		tTank.checkStructure(true);
		FluidSink tSink = new FluidSink(100000);
		tTank.setMeltingPointOverride(5000);
		tTank.mTank.fill(new FluidStack(Fluids.WATER, 7000), FluidAction.EXECUTE);
		tTank.setEmitTargetOverride(tSink); // the capability lookup is the offline wall (the boiler seam shape)
		tTank.onTick(1, true);
		assertEquals(7000, tSink.mAccepted, "the whole offer moves into the facing neighbour (:124)");
		assertEquals(0, tTank.mTank.amount(), "the tank empties into the neighbour");
	}

	@Test
	public void shouldEmitTruthTableMatchesTheSidesMasks() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
		FluidStack tWater = new FluidStack(Fluids.WATER, 1000);
		tTank.setDensitySignSeam(f -> 1); // heavier-than-air
		tTank.mFacing = (byte)Direction.NORTH.get3DDataValue();
		assertTrue(tTank.shouldEmit(tWater), "a horizontal facing emits");
		tTank.mFacing = (byte)Direction.DOWN.get3DDataValue();
		assertTrue(tTank.shouldEmit(tWater), "a heavier fluid emits DOWN (the SIDES_BOTTOM arm)");
		tTank.mFacing = (byte)Direction.UP.get3DDataValue();
		assertFalse(tTank.shouldEmit(tWater), "a heavier fluid refuses UP (emit against gravity)");
		tTank.setDensitySignSeam(f -> -1); // lighter-than-air
		assertTrue(tTank.shouldEmit(tWater), "a lighter fluid emits UP (the SIDES_TOP arm)");
		tTank.mFacing = (byte)Direction.DOWN.get3DDataValue();
		assertFalse(tTank.shouldEmit(tWater), "a lighter fluid refuses DOWN");
		GTFluidLists.GAS.add("water");
		GTAttachmentSmallBlockEntity.Categories.GASES.add("minecraft:water");
		try {
			tTank.setDensitySignSeam(f -> 1);
			tTank.mFacing = (byte)Direction.UP.get3DDataValue();
			assertTrue(tTank.shouldEmit(tWater), "a gaseous fluid emits at ANY facing (the :123 middle term)");
		} finally {
			GTFluidLists.GAS.remove("water");
			GTAttachmentSmallBlockEntity.Categories.GASES.remove("minecraft:water");
		}
	}

	@Test
	public void funnelAndTapFacesHitTheTankDirectly() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestTank tTank = placedTank(tLevel, (byte)Direction.NORTH.get3DDataValue(), 1);
		assertEquals(1000, tTank.funnelFill((byte)0, new FluidStack(Fluids.WATER, 1000), true), "funnelFill pours (:114-116)");
		FluidStack tDrawn = tTank.tapDrain((byte)0, 600, true);
		assertNotNull(tDrawn);
		assertEquals(600, tDrawn.getAmount(), "tapDrain draws (:119-121)");
		assertEquals(400, tTank.mTank.amount(), "the tank carries the remainder");
		FluidStack tProbe = tTank.tapDrain((byte)0, 600, false);
		assertEquals(400, tProbe.getAmount(), "the doDrain=false probe leaves the tank alone");
	}
}
