package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.FluidStack;

import gregapi.code.TagData;
import gregapi.data.MT;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GT6RecipesFusion;
import gregtech6.recipes.Recipe;

/**
 * The Fusion Reactor offline acceptance (task p31-fusion): the OCTAGONS mask ledger (the
 * 72/72/36 ring sizes and the 4 OUT tips), the shell geometry (27 PU / 50 vents / 48 wall
 * cells + 6 arms), the full pre-placed rig FORM with the design-5 glass ring and the
 * mActive flip to design 6 (the :241 refresh semantics through the base onTickCheck
 * hook's checkStructure(true)), the wrong-cell refusal, the generator-row machine face
 * (mOutputEnergy 8192 / mMinEnergy 0 / maxProgress = duration, the :761-764 trio), and
 * the :233-236 ±10 remote launch.
 *
 * <p>The expected-cell derivation in this file is an INDEPENDENT re-derivation of the
 * upstream FusionReactor.java:47-126 geometry (not a call of the BE walk) — the pre-
 * placement is the test oracle.
 */
public class TileEntityFusionReactorTest extends GTMultiBlocksOfflineTestBase {

	// the fixture blocks — one distinct vanilla block per part row (the frozen-registry stand-ins)
	static final Block GLASS = Blocks.GLASS;            // 18003 machine_wall_tungstensteel
	static final Block WALL = Blocks.STONE;             // 18008 machine_wall_galvanized_steel
	static final Block SS = Blocks.STONE_BRICKS;        // 18002 machine_wall_stainless_steel
	static final Block COIL = Blocks.SEA_LANTERN;       // 18045 large_iridium_coil
	static final Block VENT = Blocks.HAY_BLOCK;         // 18299 ventilation_unit
	static final Block PU_V = Blocks.GLOWSTONE;         // 18200 versatile
	static final Block PU_L = Blocks.SHROOMLIGHT;       // 18201 logic
	static final Block PU_C = Blocks.REDSTONE_LAMP;     // 18202 control

	static BlockEntityType<TestFusionReactor> sFusionType;

	/** The offline fusion controller: the part rows map onto the vanilla stand-ins above. */
	static class TestFusionReactor extends TileEntityFusionReactor {
		TestFusionReactor(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override protected Block getGlassBlock() {return GLASS;}
		@Override protected Block getWallBlock() {return WALL;}
		@Override protected Block getSsBlock() {return SS;}
		@Override protected Block getCoilBlock() {return COIL;}
		@Override protected Block getVentBlock() {return VENT;}
		@Override protected Block getVersatileBlock() {return PU_V;}
		@Override protected Block getLogicBlock() {return PU_L;}
		@Override protected Block getControlBlock() {return PU_C;}
	}

	/** The recording EU sink fixture (the ±10 launch oracle; the CountingSink shape, all 14 abstract faces). */
	static class EnergySink extends BlockEntity implements ITileEntityEnergy {
		long mReceived = 0;
		TagData mLastType = null;

		EnergySink(BlockPos aPos, BlockState aState) {
			super(sSinkType, aPos, aState);
		}

		@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return aEnergyType == TD.Energy.EU;}
		@Override public java.util.Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.EU.AS_LIST;}
		@Override public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {return aEnergyType == TD.Energy.EU;}
		@Override public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}
		@Override public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aDoInject) {mReceived += aSize * aAmount; mLastType = aEnergyType;}
			return aAmount;
		}
		@Override public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}
		@Override public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}
		@Override public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}
		@Override public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 8192;}
		@Override public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return Integer.MAX_VALUE;}
		@Override public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	static BlockEntityType<EnergySink> sSinkType;

	@BeforeAll
	static void buildFusionFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// expected offline
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		gregtech6.registry.GTMaterialItems.initMaterials(); // the MT walk (the fixture resolvers read MT.D/MT.T/MT.Vb)
		// the 21.1 BlockEntity ctor validates the state against the BET's block set —
		// the rig places part BEs over NINE distinct stand-in blocks, so the fixture
		// part BET must bind the union (the base BRICKS-only binding throws on 21.1)
		sPartType = BlockEntityType.Builder.of((aPos, aState) -> new MultiBlockPartBlockEntity(sPartType, aPos, aState),
				Blocks.BRICKS, GLASS, WALL, SS, COIL, VENT, PU_V, PU_L, PU_C).build(null);
		{
			@SuppressWarnings("unchecked")
			BlockEntityType<TestFusionReactor>[] tHolder = (BlockEntityType<TestFusionReactor>[]) new BlockEntityType<?>[1];
			tHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new TestFusionReactor(tHolder[0], aPos, aState), Blocks.BRICKS).build(null);
			sFusionType = tHolder[0];
		}
		{
			@SuppressWarnings("unchecked")
			BlockEntityType<EnergySink>[] tHolder = (BlockEntityType<EnergySink>[]) new BlockEntityType<?>[1];
			tHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new EnergySink(aPos, aState), Blocks.BRICKS).build(null);
			sSinkType = tHolder[0];
		}
	}

	@AfterEach
	void resetRecipes() {
		GT6RecipesFusion.sCircuitResolver = aConfig -> new ItemStack(Items.PAPER);
		GT6RecipesFusion.sFluidResolver = (aMaterial, aMolten) -> aMolten ? Fluids.LAVA : Fluids.WATER;
		GT6RecipesFusion.sMaterialItemResolver = (aPrefix, aMaterial) ->
				aPrefix == gregapi.data.OP.dust && aMaterial == MT.Vb ? Items.IRON_INGOT : null;
		GT6RecipeMaps.reset();
		GT6RecipesFusion.resetForTest();
	}

	// ------------------------------------------------------------------
	// the independent geometry derivation (upstream :47-126)
	// ------------------------------------------------------------------

	private record Cell(int x, int y, int z, Block block) {}

	/** The expected cell list for a controller at (cx, cy, cz) facing north (2) — the structure BEHIND (z+2 core centre). */
	private static List<Cell> expectedCells(int aCX, int aCY, int aCZ) {
		int tCX = aCX, tCY = aCY, tCZ = aCZ + 2; // the core centre (anchor doubled)
		List<Cell> rCells = new ArrayList<>();
		// the 5x5x5 core
		int tPU = 0;
		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) for (int k = -2; k <= 2; k++) {
			int d2 = i * i + j * j + k * k;
			Block tBlock;
			if (d2 < 4) {
				// the quota assignment: 3 versatile, then 12 logic, then 12 control (any split >= quotas passes)
				tBlock = tPU < 3 ? PU_V : tPU < 15 ? PU_L : PU_C;
				tPU++;
			} else if (d2 > 6 || (j == 0 && (((i == -2 || i == 2) && k == 0) || ((k == -2 || k == 2) && i == 0)))) {
				tBlock = WALL;
			} else {
				tBlock = VENT;
			}
			rCells.add(new Cell(tCX + i, tCY + j, tCZ + k, tBlock));
		}
		// the arms: the facing (north, z-) direction skipped — X ±3/±4 and Z+3/+4
		for (int d = 3; d <= 4; d++) {
			rCells.add(new Cell(tCX - d, tCY, tCZ, WALL));
			rCells.add(new Cell(tCX + d, tCY, tCZ, WALL));
			rCells.add(new Cell(tCX, tCY, tCZ + d, WALL));
		}
		// the OCTAGONS rings (the masks re-walked from the transcribed constants)
		int tRX = tCX - 9, tRZ = tCZ - 9;
		for (int i = 0; i < 19; i++) for (int j = 0; j < 19; j++) {
			if (TileEntityFusionReactor.OCTAGONS[0][i][j]) {
				rCells.add(new Cell(tRX + i, tCY - 1, tRZ + j, GLASS));
				rCells.add(new Cell(tRX + i, tCY, tRZ + j, GLASS));
				rCells.add(new Cell(tRX + i, tCY + 1, tRZ + j, GLASS));
			}
			if (TileEntityFusionReactor.OCTAGONS[1][i][j]) {
				rCells.add(new Cell(tRX + i, tCY - 2, tRZ + j, GLASS));
				rCells.add(new Cell(tRX + i, tCY - 1, tRZ + j, GLASS));
				rCells.add(new Cell(tRX + i, tCY, tRZ + j, COIL));
				rCells.add(new Cell(tRX + i, tCY + 1, tRZ + j, GLASS));
				rCells.add(new Cell(tRX + i, tCY + 2, tRZ + j, GLASS));
			}
			if (TileEntityFusionReactor.OCTAGONS[2][i][j]) {
				rCells.add(new Cell(tRX + i, tCY - 2, tRZ + j, GLASS));
				rCells.add(new Cell(tRX + i, tCY - 1, tRZ + j, COIL));
				rCells.add(new Cell(tRX + i, tCY, tRZ + j, SS));
				rCells.add(new Cell(tRX + i, tCY + 1, tRZ + j, COIL));
				rCells.add(new Cell(tRX + i, tCY + 2, tRZ + j, GLASS));
			}
		}
		return rCells;
	}

	private static final MultiBlockLevel[] sLevelHolder = new MultiBlockLevel[1];

	/** Places the full rig and returns the formed-or-not controller (facing north at (100, 64, 40)). */
	private TestFusionReactor placeRig(MultiBlockLevel aLevel) {
		sLevelHolder[0] = aLevel;
		BlockPos tCtrl = new BlockPos(100, 64, 40);
		for (Cell tCell : expectedCells(tCtrl.getX(), tCtrl.getY(), tCtrl.getZ())) {
			placePartAt(aLevel, new BlockPos(tCell.x(), tCell.y(), tCell.z()), tCell.block());
		}
		TestFusionReactor tController = sFusionType.create(tCtrl, Blocks.BRICKS.defaultBlockState());
		tController.setLevel(aLevel);
		tController.mFacing = 2; // north
		aLevel.mStates.put(tCtrl, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(tCtrl, tController);
		return tController;
	}

	/** The local part placement with a per-block state (the base fixture only answers BRICKS). */
	private static void placePartAt(MultiBlockLevel aLevel, BlockPos aPos, Block aBlock) {
		MultiBlockPartBlockEntity tPart = sPartType.create(aPos, aBlock.defaultBlockState());
		tPart.setLevel(aLevel);
		aLevel.mStates.put(aPos, aBlock.defaultBlockState());
		aLevel.mBlockEntities.put(aPos, tPart);
	}

	// ------------------------------------------------------------------
	// the mask ledger and the shell geometry
	// ------------------------------------------------------------------

	@Test
	void theOctagonMasksCarryTheUpstreamLedger() {
		assertEquals(72, countMask(0), "OCTAGONS[0] = the 72-cell glass ring");
		assertEquals(72, countMask(1), "OCTAGONS[1] = the 72-cell coil ring");
		assertEquals(36, countMask(2), "OCTAGONS[2] = the 36-cell SS ring");
		int tTips = 0;
		for (int i = 0; i < 19; i++) for (int j = 0; j < 19; j++) if (TileEntityFusionReactor.isRingOutTip(i, j)) tTips++;
		assertEquals(4, tTips, "the four orthogonal edge midpoints are the OUT tips");
		// the shell: 27 PU + 50 vents + 48 walls over the 5x5x5 (the 125 cells partition)
		int tPU = 0, tVent = 0, tWall = 0;
		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) for (int k = -2; k <= 2; k++) {
			int tKind = TileEntityFusionReactor.shellKind(i, j, k);
			if (tKind == -1) tPU++; else if (tKind == 0) tWall++; else tVent++;
		}
		assertEquals(27, tPU, "the :197 tooltip 27 PU");
		assertEquals(50, tVent, "the :196 tooltip 50 vents");
		assertEquals(48, tWall, "the core GalvSteel walls (48 + the 6 arms = 54 checks; the :196 '53' is the upstream tooltip's own arithmetic)");
		assertEquals(125, tPU + tVent + tWall, "the 5x5x5 partition is total");
	}

	private static int countMask(int aIndex) {
		int rCount = 0;
		for (boolean[] tRow : TileEntityFusionReactor.OCTAGONS[aIndex]) for (boolean tCell : tRow) if (tCell) rCount++;
		return rCount;
	}

	// ------------------------------------------------------------------
	// the form, the design flip, and the refusal
	// ------------------------------------------------------------------

	@Test
	void theRigFormsAndTheRingRewritesOnTheActiveFlip() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestFusionReactor tController = placeRig(tLevel);
		assertTrue(tController.checkStructure(true), "the fully pre-placed rig forms");
		assertTrue(tController.checkStructure(false), "the verdict caches");
		// the glass ring y0: design 5 (inactive) + ONLY_ENERGY_IN, the tips design 2 + ONLY_ENERGY_OUT
		int tCX = 100, tCY = 64, tCZ = 42; // the core centre
		MultiBlockPartBlockEntity tTip = partAt(tLevel, tCX - 9, tCY, tCZ); // i=0, j=9 — the X- tip
		assertNotNull(tTip, "the ring tip part exists");
		assertEquals(2, tTip.mDesign, "the tip carries design 2");
		assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_OUT, tTip.mMode, "the tip mode is ONLY_ENERGY_OUT");
		// a plain IN cell: ring offset (9, 5) → (tCX + 0, tCZ - 4)
		MultiBlockPartBlockEntity tPlain = partAt(tLevel, tCX - 3, tCY, tCZ - 4); // i=6, j=5 — a real mask cell
		assertNotNull(tPlain, "the plain ring cell exists");
		assertEquals(5, tPlain.mDesign, "the inactive ring reads design 5");
		assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_IN, tPlain.mMode, "the ring mode is ONLY_ENERGY_IN");
		// the active flip rewrites 5 -> 6 (the base onTickCheck :105 hook drives checkStructure(true))
		tController.mActive = true;
		assertTrue(tController.checkStructure(true), "the rig still forms in the active walk");
		assertEquals(6, tPlain.mDesign, "the active ring reads design 6");
		tController.mActive = false;
		assertTrue(tController.checkStructure(true));
		assertEquals(5, tPlain.mDesign, "and back to 5");
		// the refresh hook returns true (the :241 override the base hook consults)
		assertTrue(tController.refreshStructureOnActiveStateChange());
	}

	@Test
	void aWrongCellRefusesTheForm() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestFusionReactor tController = placeRig(tLevel);
		// swap one coil for a plain wall — the walk must refuse
		// a coil ring cell: OCTAGONS[1] row 1, col 8 (true) → world (100-9+8, 64, 33+1) = (99, 64, 34)
		BlockPos tCoilCell = new BlockPos(99, 64, 34);
		assertEquals(COIL, tLevel.getBlockState(tCoilCell).getBlock(), "the cell starts as the coil stand-in");
		((MultiBlockPartBlockEntity) tLevel.getBlockEntity(tCoilCell)).setBlockState(Blocks.BRICKS.defaultBlockState());
		tLevel.mStates.put(tCoilCell, Blocks.BRICKS.defaultBlockState());
		assertFalse(tController.checkStructure(true), "the wrong-cell form is refused");
		// restore
		((MultiBlockPartBlockEntity) tLevel.getBlockEntity(tCoilCell)).setBlockState(COIL.defaultBlockState());
		tLevel.mStates.put(tCoilCell, COIL.defaultBlockState());
		assertTrue(tController.checkStructure(true), "the restored rig forms");
	}

	@Test
	void thePartRelayGatesByTheModeMask() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestFusionReactor tController = placeRig(tLevel);
		assertTrue(tController.checkStructure(true));
		// the glass IN face relays into the controller; the wall (ONLY_ITEM_FLUID) refuses
		MultiBlockPartBlockEntity tInPart = partAt(tLevel, 97, 64, 38); // the (6,5) ring cell
		assertNotNull(tInPart);
		assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_IN, tInPart.mMode);
		long tAcceptedPackets = tInPart.doEnergyInjection(TD.Energy.TU, (byte)3, 1024, 1, true);
		assertEquals(1, tAcceptedPackets, "one packet accepted through the part relay");
		assertEquals(1024, tController.mEnergy, "the TU packet landed in the machine buffer through the part");
		// the LU face probes accepting but the injection refuses (the inert charged ledger)
		assertTrue(tInPart.isEnergyAcceptingFrom(TD.Energy.LU, (byte)3, false), "the glass ring advertises LU acceptance");
		assertEquals(0, tInPart.doEnergyInjection(TD.Energy.LU, (byte)3, 512, 1, true), "the LU injection falls through the :501 type check (the unported ignition ledger)");
		// a wall part refuses by mask (the X-3 arm wall, mode NOTHING)
		MultiBlockPartBlockEntity tArmPart = partAt(tLevel, 100 - 3, 64, 42);
		assertNotNull(tArmPart, "the X- arm wall exists");
		assertEquals(0, tArmPart.doEnergyInjection(TD.Energy.TU, (byte)3, 512, 1, true), "the ONLY_ITEM_FLUID mask refuses energy");
	}

	// ------------------------------------------------------------------
	// the generator row and the ±10 remote launch
	// ------------------------------------------------------------------

	@Test
	void theGeneratorRowDrivesTheMachineFace() {
		// a DETERMINISTIC single-row pour: only T and He resolve, so the map holds exactly
		// the :950 T+T row (in [lava 2000] out [water 1000], the gas stand-in identities) —
		// the full-18 parity rides GT6RecipesFusionTest
		GT6RecipesFusion.sCircuitResolver = aConfig -> new ItemStack(Items.PAPER);
		GT6RecipesFusion.sFluidResolver = (aMaterial, aMolten) ->
				aMaterial == MT.T ? Fluids.LAVA : aMaterial == MT.He ? Fluids.WATER : null;
		GT6RecipesFusion.sMaterialItemResolver = (aPrefix, aMaterial) -> null;
		GT6RecipeMaps.init();
		GT6RecipesFusion.load();
		assertEquals(1, GT6RecipeMaps.FUSION.mRecipeList.size(), "only the T+T row resolves under the single-row fixture");

		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestFusionReactor tController = placeRig(tLevel);
		assertTrue(tController.checkStructure(true));
		// the selector input + the fluid input (gas T 2000 -> lava)
		tController.getInventory().setStackInSlot(0, new ItemStack(Items.PAPER));
		tController.mTanksInput[0].fill(new FluidStack(Fluids.LAVA, 2000), FluidAction.EXECUTE);
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE,
				tController.checkRecipe(true, false), "the T+T row consumes");
		// the :761-764 generator trio
		assertEquals(8192, tController.mOutputEnergy, "mOutputEnergy = |EUt|");
		assertEquals(1130, tController.mMaxProgress, "mMaxProgress = the recipe duration");
		assertEquals(0, tController.mMinEnergy, "mMinEnergy = 0 (the generator row)");
		assertEquals(0, tController.mTanksInput[0].amount(), "the fluid input drained");
		// the :738 short-supply refusal: 1999 mB does not consume (the fixture selector was
		// CONSUMED by the first run — the never-consumed identity rides the real GT6Circuits
		// item, the PAPER stand-in is a normal stack — so the slot is re-stocked first)
		tController.getInventory().setStackInSlot(0, new ItemStack(Items.PAPER));
		tController.mTanksInput[0].fill(new FluidStack(Fluids.LAVA, 1999), FluidAction.EXECUTE);
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS,
				tController.checkRecipe(true, false), "the short supply refuses at the consume half");
	}

	@Test
	void theRemoteLaunchHitsOnlyTheTenOffPoints() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestFusionReactor tController = placeRig(tLevel);
		assertTrue(tController.checkStructure(true));
		tController.mOutputEnergy = 8192; // the generator state
		// the core centre is (100, 64, 42); the first push side is north (z-) → the sink at z-10
		EnergySink tFar = placeSink(tLevel, new BlockPos(100, 64, 32));
		EnergySink tNear = placeSink(tLevel, new BlockPos(100, 64, 35)); // 7 off — NOT a launch point
		EnergySink tEast = placeSink(tLevel, new BlockPos(110, 64, 42)); // the +10 X point (side 5, tried after north)
		// the direct hook (one packet out of the north point)
		tController.doOutputEnergy();
		assertEquals(8192, tFar.mReceived, "the north ±10 point received the packet");
		assertEquals(TD.Energy.EU, tFar.mLastType, "the packet type is EU");
		assertEquals(0, tNear.mReceived, "the 7-off point receives nothing");
		assertEquals(0, tEast.mReceived, "the first accepting side short-circuits the loop (:235)");
		// the doActive :812 spot fires the same hook while a generator process progresses
		tController.mMaxProgress = 730;
		tController.mProgress = 0;
		tController.doActive(5, 100);
		assertEquals(2 * 8192, tFar.mReceived, "the :812 per-tick emission rode the progressing process");
		assertEquals(100, tController.mProgress, "progress advanced by the energy unit");
	}

	@Test
	void theMachinesOwnEnergyFaceMirrorsTheWindow() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestFusionReactor tController = placeRig(tLevel);
		assertEquals(1, tController.getEnergySizeInputMin(TD.Energy.TU, (byte)3), "the :513 window min");
		assertEquals(8192, tController.getEnergySizeInputRecommended(TD.Energy.TU, (byte)3), "the :514 window recommended");
		assertEquals(16384, tController.getEnergySizeInputMax(TD.Energy.TU, (byte)3), "the :515 window max");
		// the direct TU arm accepts into mEnergy; an oversize packet is refused (no explosion)
		assertEquals(1, tController.doEnergyInjection(TD.Energy.TU, (byte)3, 4, 1, true), "a 4-EU packet accepts (the return is the PACKET count)");
		assertEquals(4, tController.mEnergy, "the buffer gains size x packets");
		assertEquals(0, tController.doEnergyInjection(TD.Energy.TU, (byte)3, 16385, 1, true), "the overcharge face refuses (the declared narrowing)");
		assertEquals(0, tController.doEnergyInjection(TD.Energy.LU, (byte)3, 512, 1, true), "LU refuses at the body too (the glass ring relay is the LU face)");
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	private static MultiBlockPartBlockEntity partAt(MultiBlockLevel aLevel, int aX, int aY, int aZ) {
		BlockEntity tBE = aLevel.getBlockEntity(new BlockPos(aX, aY, aZ));
		return tBE instanceof MultiBlockPartBlockEntity tPart ? tPart : null;
	}

	private static EnergySink placeSink(MultiBlockLevel aLevel, BlockPos aPos) {
		EnergySink tSink = sSinkType.create(aPos, Blocks.BRICKS.defaultBlockState());
		tSink.setLevel(aLevel);
		aLevel.mStates.put(aPos, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(aPos, tSink);
		return tSink;
	}
}
