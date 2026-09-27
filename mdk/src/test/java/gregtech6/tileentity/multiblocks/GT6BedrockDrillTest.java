package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.ore.GTBedrockOreBlock;

/**
 * The Bedrock Mining Drill offline acceptance (task p37-bedrock-drill): the probe-seam
 * structure (no declared pattern — the GTMultiBlockPattern.java:77-81 standing clause),
 * the hand-written :82-120 check (the y>=5 gate, the 3x3 y-5 floor probe with the
 * large-x2 / small-x1 material weighting, the bedrock-only floor gate, the four machine
 * layers through the Util offset walk), the :144-146 box, the :274-293 RU energy face,
 * and the :149-251 production loop (the 32768 RU + 100 L lubricant cycle gate, the
 * rng(128) ore/stone two-path draw, the 1/32 byproduct arm, the 1/1000 bedrock dust arm)
 * over the rng seam and the output-resolution seams.
 */
public class GT6BedrockDrillTest extends GTMultiBlocksOfflineTestBase {

	/** The controller cell for this suite — five above the floor probe, away from the shared fixtures. */
	static final BlockPos DRILL_POS = new BlockPos(240, 64, 240);

	static BlockEntityType<TestDrill> sDrillType;

	/** The part BET over BOTH fixture part blocks (1.21.1 BlockEntity validates the block-state pair — the graagg three-block form). */
	static BlockEntityType<MultiBlockPartBlockEntity> sDrillPartType;

	/** The concrete test BE — the drill controller over a vanilla-block BET, the two part hooks bound to distinct vanilla blocks. */
	public static final class TestDrill extends TileEntityBedrockDrill {
		public TestDrill(BlockPos aPos, BlockState aState) {
			super(sDrillType, aPos, aState);
		}
		public TestDrill(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}
		@Override
		protected Block getDrillHeadBlock() {
			return Blocks.IRON_BLOCK;
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
		/** The offline output seam — the GT block/item registries are frozen, the resolution arms return sentinels. */
		@Override
		ItemStack brokenOreItem(OreDictMaterial aMaterial) {
			return new ItemStack(Items.DIAMOND_ORE); // the ore-path sentinel
		}
		@Override
		ItemStack dustItem(OreDictMaterial aMaterial) {
			return new ItemStack(Items.GUNPOWDER); // the dust-fallback sentinel
		}
		@Override
		ItemStack stoneSkinItem() {
			return new ItemStack(Items.COBBLESTONE); // the stone-skin sentinel
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildDrillFixtures() {
		gregtech6.registry.GTMaterialItems.initMaterials(); // MT/OP must exist before any field dereference (the P31 posture)
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		// the floor-probe ore blocks are constructed in-test — the GT6BedrockOreBlocksRegistrationTest
		// unfreeze posture (Block.<init> creates an intrusive holder past the bootstrap freeze)
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Throwable aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		BlockEntityType<TestDrill>[] tHolder = (BlockEntityType<TestDrill>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(TestDrill::new, Blocks.BRICKS).build(null);
		sDrillType = tHolder[0];
		@SuppressWarnings("unchecked")
		BlockEntityType<MultiBlockPartBlockEntity>[] tPartHolder = (BlockEntityType<MultiBlockPartBlockEntity>[]) new BlockEntityType<?>[1];
		tPartHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new MultiBlockPartBlockEntity(tPartHolder[0], aPos, aState), Blocks.BRICKS, Blocks.IRON_BLOCK).build(null);
		sDrillPartType = tPartHolder[0];
	}

	// ------------------------------------------------------------------
	// the world fixtures — the floor probe + the four machine layers
	// ------------------------------------------------------------------

	private static void placePartWithState(MultiBlockLevel aLevel, BlockPos aPos, Block aBlock) {
		MultiBlockPartBlockEntity tPart = sDrillPartType.create(aPos, aBlock.defaultBlockState());
		tPart.setLevel(aLevel);
		aLevel.mStates.put(aPos.immutable(), aBlock.defaultBlockState());
		aLevel.mBlockEntities.put(aPos.immutable(), tPart);
	}

	/** A fully-formed drill: the bedrock floor + the four part layers, ores per the caller. */
	static TestDrill formedDrill(MultiBlockLevel aLevel, Object... aFloorCells) {
		TestDrill tDrill = sDrillType.create(DRILL_POS, Blocks.BRICKS.defaultBlockState());
		tDrill.setLevel(aLevel);
		aLevel.mStates.put(DRILL_POS, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(DRILL_POS, tDrill);
		for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) {
			BlockState tFloor = Blocks.BEDROCK.defaultBlockState();
			for (int k = 0; k < aFloorCells.length; k += 3) {
				if (aFloorCells[k].equals(i) && aFloorCells[k + 1].equals(j)) {
					tFloor = (BlockState)aFloorCells[k + 2];
				}
			}
			aLevel.mStates.put(DRILL_POS.offset(i, -5, j), tFloor);
			placePartWithState(aLevel, DRILL_POS.offset(i, -4, j), Blocks.IRON_BLOCK); // the y-4 drill heads
			placePartWithState(aLevel, DRILL_POS.offset(i, -3, j), Blocks.BRICKS);
			placePartWithState(aLevel, DRILL_POS.offset(i, -2, j), Blocks.BRICKS);
			placePartWithState(aLevel, DRILL_POS.offset(i, -1, j), Blocks.BRICKS);
			if (i != 0 || j != 0) placePartWithState(aLevel, DRILL_POS.offset(i, 0, j), Blocks.BRICKS); // the y-0 ring; the centre is the controller
		}
		return tDrill;
	}

	private static BlockState largeOre(OreDictMaterial aMaterial) {
		return new GTBedrockOreBlock(OP.oreBedrock, aMaterial, false).defaultBlockState();
	}

	private static BlockState smallOre(OreDictMaterial aMaterial) {
		return new GTBedrockOreBlock(OP.oreSmall, aMaterial, true).defaultBlockState();
	}

	// ------------------------------------------------------------------
	// the probe seam (:82-120)
	// ------------------------------------------------------------------

	@Test
	public void thePatternStaysNullTheProbeSeamMachine() {
		TestDrill tDrill = formedDrill(new MultiBlockLevel());
		assertNull(tDrill.getStructurePattern(), "the drill declares NO pattern — the hand-written check is the server truth (GTMultiBlockPattern.java:77-81)");
		assertEquals("multiblock_bedrock_drill", tDrill.getTileEntityName());
	}

	@Test
	public void belowYFiveRefuses() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestDrill tDrill = sDrillType.create(new BlockPos(DRILL_POS.getX(), 4, DRILL_POS.getZ()), Blocks.BRICKS.defaultBlockState());
		tDrill.setLevel(tLevel);
		assertFalse(tDrill.checkStructure2(null, null, null), ":83 — yCoord < 5 is a hard refuse (the y-5 probe would read below the world)");
	}

	@Test
	public void plainBedrockFloorFormsWithEmptyList() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestDrill tDrill = formedDrill(tLevel);
		assertTrue(tDrill.checkStructure2(null, null, null), "a full bedrock floor forms the machine");
		assertTrue(tDrill.mList.isEmpty(), "no ores on the floor — the stone path is the only producer");
	}

	@Test
	public void theFloorProbeCollectsMaterialsWithTheDoubleWeighting() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestDrill tDrill = formedDrill(tLevel,
				-1, -1, largeOre(MT.Diamond), // the large ore counts twice (:89-91)
				1, 1, smallOre(MT.Coal));     // the small ore counts once (:92-94)
		assertTrue(tDrill.checkStructure2(null, null, null));
		assertEquals(3, tDrill.mList.size(), "2 (large) + 1 (small) — the duplication IS the yield weighting");
		assertEquals(MT.Diamond, tDrill.mList.get(0));
		assertEquals(MT.Diamond, tDrill.mList.get(1));
		assertEquals(MT.Coal, tDrill.mList.get(2));
	}

	@Test
	public void aNonBedrockFloorCellFailsTheCheck() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestDrill tDrill = formedDrill(tLevel, 0, 0, Blocks.STONE.defaultBlockState()); // :106-107
		assertFalse(tDrill.checkStructure2(null, null, null), "a foreign floor cell fails (WD.bedrock gate)");
	}

	@Test
	public void aMissingMachineCellFailsTheCheck() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestDrill tDrill = formedDrill(tLevel);
		tLevel.mStates.put(DRILL_POS.offset(1, -4, 1), Blocks.AIR.defaultBlockState()); // a drill head removed
		tLevel.mBlockEntities.remove(DRILL_POS.offset(1, -4, 1));
		assertFalse(tDrill.checkStructure2(null, null, null), ":109 — every machine cell must hold its part");
	}

	@Test
	public void isInsideStructureIsTheUpstreamBox() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestDrill tDrill = formedDrill(tLevel);
		int tX = DRILL_POS.getX(), tY = DRILL_POS.getY(), tZ = DRILL_POS.getZ();
		assertTrue(tDrill.isInsideStructure(tX, tY, tZ), "the controller cell");
		assertTrue(tDrill.isInsideStructure(tX + 1, tY - 5, tZ + 1), "the floor corner");
		assertTrue(tDrill.isInsideStructure(tX - 1, tY - 1, tZ + 1), "the wall ring");
		assertFalse(tDrill.isInsideStructure(tX + 2, tY, tZ), "three across is outside");
		assertFalse(tDrill.isInsideStructure(tX, tY - 6, tZ), "six below is outside");
		assertFalse(tDrill.isInsideStructure(tX, tY + 1, tZ), "above the controller is outside");
	}

	// ------------------------------------------------------------------
	// the energy face (:274-293)
	// ------------------------------------------------------------------

	@Test
	public void energyFaceIsTheRuInputWindow() {
		TestDrill tDrill = sDrillType.create(DRILL_POS, Blocks.BRICKS.defaultBlockState());
		assertSame(TD.Energy.RU, tDrill.mEnergyTypeAccepted, "the :1283 NBT_ENERGY_ACCEPTED column");
		assertTrue(tDrill.isEnergyType(TD.Energy.RU, (byte)2, false), "accepts RU in");
		assertFalse(tDrill.isEnergyType(TD.Energy.RU, (byte)2, true), "never emits");
		assertFalse(tDrill.isEnergyType(TD.Energy.EU, (byte)2, false), "RU only");

		// :274-281 — the packet train accumulation
		assertEquals(2, tDrill.doInject(TD.Energy.RU, (byte)1, 2048, 2, true), "the whole train reported used");
		assertEquals(4096, tDrill.mEnergy, "mEnergy += aAmount * |aSize|");
		assertEquals(2, tDrill.doInject(TD.Energy.RU, (byte)1, -2048, 2, true), "a negative size folds through Math.abs");
		assertEquals(8192, tDrill.mEnergy);
		assertEquals(2, tDrill.doInject(TD.Energy.RU, (byte)1, 2048, 2, false), ":277 — the simulation returns aAmount untouched");
		assertEquals(8192, tDrill.mEnergy, "the simulation pass leaves the store alone");

		assertEquals(2048, tDrill.getEnergySizeInputRecommended(TD.Energy.RU, (byte)1), ":287");
		assertEquals(1024, tDrill.getEnergySizeInputMin(TD.Energy.RU, (byte)1), ":288");
		assertEquals(4096, tDrill.getEnergySizeInputMax(TD.Energy.RU, (byte)1), ":289");
		assertEquals(4096, tDrill.getEnergyDemanded(TD.Energy.RU, (byte)1, 2048), ":286");

		tDrill.mEnergy = TileEntityBedrockDrill.ENERGY_CAPACITY + 1;
		assertEquals(0, tDrill.doInject(TD.Energy.RU, (byte)1, 2048, 2, true), ":275 — over the cap the inject refuses");
	}

	// ------------------------------------------------------------------
	// the production loop (:149-251)
	// ------------------------------------------------------------------

	/** The producing-drill fixture: formed, energy-charged, lubricant-loaded. */
	private TestDrill producingDrill() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestDrill tDrill = formedDrill(tLevel, -1, -1, largeOre(MT.Diamond));
		tDrill.mStructureOkay = true;
		tDrill.mTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE); // the lube stand-in
		tDrill.mEnergy = TileEntityBedrockDrill.ENERGY_PER_CYCLE;
		return tDrill;
	}

	@Test
	public void theCycleConsumesEnergyLubeAndProduces() {
		TestDrill tDrill = producingDrill();
		tDrill.setRngOverride(() -> 1000); // retype no (1000 != 0), selector 1000 (>= list size 2 -> the stone path), stone-path rng 1000 (no bedrock dust)
		tDrill.onTick(1, true);
		assertEquals(0, tDrill.mEnergy, "the 32768 RU cycle deduct");
		assertEquals(900, tDrill.mTank.amount(), "the 100 L lubricant deduct");
		assertNotNull(tDrill.mOutput, "a block was produced");
	}

	@Test
	public void theOrePathDrawsTheListSlot() {
		TestDrill tDrill = producingDrill();
		tDrill.mList.clear();
		tDrill.mList.add(MT.Diamond);
		tDrill.setRngOverride(() -> 31); // rng(32) != 0 — no byproduct
		ItemStack tDrawn = tDrill.draw(0); // 0 < 1 — the ore path
		assertEquals(Items.DIAMOND_ORE, tDrawn.getItem(), "the broken-ore sentinel (the live resolution arm)");

		// :163 — the 1/32 byproduct arm (rng(32) == 0 picks a random mByProducts entry; an
		// empty byproduct list keeps the material — either way the same item face answers)
		tDrill.setRngOverride(() -> 0);
		ItemStack tByproduct = tDrill.draw(0);
		assertEquals(Items.DIAMOND_ORE, tByproduct.getItem(), "the byproduct arm resolves through the same item face");
	}

	@Test
	public void theStonePathDrawsBedrockDustAtOneInAThousand() {
		TestDrill tDrill = producingDrill();
		tDrill.mList.clear();
		tDrill.setRngOverride(() -> 0); // rng(1000) == 0 — the bedrock dust arm (:205-207)
		ItemStack tDrawn = tDrill.draw(200);
		assertEquals(Items.GUNPOWDER, tDrawn.getItem(), "the dust sentinel — the bedrock dust rides the dust face");

		tDrill.setRngOverride(() -> 1); // rng(1000) != 0 — the stone skin arm
		ItemStack tStone = tDrill.draw(200);
		assertEquals(Items.COBBLESTONE, tStone.getItem(), "the stone-skin sentinel");
	}

	@Test
	public void aHeldOutputOrAnEmptyTankOrNoEnergyStallsTheCycle() {
		TestDrill tDrill = producingDrill();
		tDrill.mOutput = new ItemStack(Items.STONE);
		tDrill.onTick(1, true);
		assertEquals(TileEntityBedrockDrill.ENERGY_PER_CYCLE, tDrill.mEnergy, ":155 — a held output blocks the cycle");
		assertEquals(1000, tDrill.mTank.amount());

		tDrill.mOutput = null;
		tDrill.mTank.setEmpty();
		tDrill.onTick(2, true);
		assertEquals(TileEntityBedrockDrill.ENERGY_PER_CYCLE, tDrill.mEnergy, "no lubricant — no cycle");
		assertNull(tDrill.mOutput);

		tDrill.mTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		tDrill.mEnergy = TileEntityBedrockDrill.ENERGY_PER_CYCLE - 1;
		tDrill.onTick(3, true);
		assertEquals(TileEntityBedrockDrill.ENERGY_PER_CYCLE - 1, tDrill.mEnergy, "under 32768 RU — no cycle");
		assertNull(tDrill.mOutput);
	}

	@Test
	public void nbtRoundTripsTheProductionState() {
		TestDrill tDrill = producingDrill();
		tDrill.mType = 5;
		tDrill.mOutput = new ItemStack(Items.COBBLESTONE, 3);
		CompoundTag aNBT = new CompoundTag();
		tDrill.saveAdditional(aNBT);
		TestDrill tLoaded = sDrillType.create(DRILL_POS, Blocks.BRICKS.defaultBlockState());
		tLoaded.load(aNBT);
		assertEquals(tDrill.mEnergy, tLoaded.mEnergy);
		assertEquals(5, tLoaded.mType);
		assertEquals(1000, tLoaded.mTank.amount(), "the tank content survives (the fixture never ran a cycle)");
		assertEquals(3, tLoaded.mOutput.getCount(), "the held output survives");
	}

	// ------------------------------------------------------------------
	// the faces (the extract-only output + the fill-gated tank)
	// ------------------------------------------------------------------

	@Test
	public void theOutputFaceIsExtractOnly() {
		TestDrill tDrill = sDrillType.create(DRILL_POS, Blocks.BRICKS.defaultBlockState());
		TileEntityBedrockDrill.DrillOutputHandler tHandler = new TileEntityBedrockDrill.DrillOutputHandler(tDrill);
		assertEquals(1, tHandler.getSlots(), ":303 the single output slot");
		assertFalse(tHandler.isItemValid(0, new ItemStack(Items.STONE)), ":307 canInsertItem2 = F");
		assertEquals(2, tHandler.insertItem(0, new ItemStack(Items.STONE, 2), false).getCount(), "inserts bounce back untouched");
		assertTrue(tHandler.getStackInSlot(0).isEmpty());

		tDrill.mOutput = new ItemStack(Items.COBBLESTONE, 5);
		assertEquals(2, tHandler.extractItem(0, 2, false).getCount(), "the extraction drains the held stack");
		assertEquals(3, tDrill.mOutput.getCount(), "the remainder stays held");
		assertEquals(3, tHandler.extractItem(0, 64, true).getCount(), "the simulation leaves the stack");
		assertEquals(3, tDrill.mOutput.getCount());
	}

	@Test
	public void theTankFaceFillsThroughTheLubricantGate() {
		TestDrill tDrill = sDrillType.create(DRILL_POS, Blocks.BRICKS.defaultBlockState());
		TileEntityBedrockDrill.DrillFluidHandler tHandler = new TileEntityBedrockDrill.DrillFluidHandler(tDrill);
		assertEquals(16000, tHandler.getTankCapacity(0), ":61 the tank size");
		assertEquals(0, tHandler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
				":295 — the fill gate is lubricant-only (water refused; the live gate resolves gt6:lubricant)");
	}
}
