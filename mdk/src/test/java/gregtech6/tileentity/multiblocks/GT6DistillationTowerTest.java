package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import gregapi.data.TD;
import gregtech6.fluid.FluidTankGT;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Distillation.TileEntityDistillationTower;

/**
 * The distillation tower offline acceptance (task p29-w3-distill-crucible ①② — the
 * OFFLINE half; the live half is the p29_w3_distillation_tower / p29_w3_cryo_tower RCON
 * chains):
 * <ul>
 * <li><b>ACCEPTANCE ①</b> — the declared pattern per facing: 9 transmitter cells at y-1
 *     (ONLY_ENERGY_IN, design 0), 9 part cells per layer y0..y7 (y0 ONLY_ITEM_FLUID, above
 *     ONLY_FLUID_OUT), the back-centre hole column design 1 — its WORLD cell lands at
 *     controller − 2·OFF[facing] for every horizontal facing (the upstream
 *     {@code mFacing == SIDE_X ? 1 : 0} back-centre arithmetic through the shared
 *     cellOffset rotation);</li>
 * <li><b>ACCEPTANCE ②</b> — the energy domain projection: the HU row and the CU row carry
 *     their NBT_ENERGY_ACCEPTED as the accepting face, the WRONG type is refused at
 *     doInject (the offline half of the CU/HU 互拒 arm);</li>
 * <li><b>ACCEPTANCE ⑤</b> — the CHEAP_OVERCLOCKING first-consumer对拍 (the card ①
 *     semantics-test fixture shape transplanted onto the TOWER's own overridden checkRecipe:
 *     a naive override could have dropped the :773 gate — this pins that it did not);</li>
 * <li>the no-self-generation tick face (the upstream :455 TU-gate the base dropped) and
 *     the routing table (:152-166);</li>
 * <li><b>task p30-distill-output-routing</b> — the NINE-tank output bank (the 2026-09-16
 *     ruling option a, the W3④ single-tank freeze undone): the bank shape (RM.java:65/:66
 *     fluids 1/9/0), the seven-fraction row passing canOutput and landing one fraction per
 *     tank, the routing-cell arithmetic, and the NBT round-trip with the legacy
 *     single-tank save compatibility.</li>
 * </ul>
 */
class GT6DistillationTowerTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(120, 64, 100);

	/** The fixture part identities (vanilla registered blocks — a fresh Block would hit the frozen state registry offline; the getPartBlock seam only needs DISTINCT identities). */
	private static final Block TRANSMITTER = Blocks.IRON_BLOCK;
	private static final Block PART = Blocks.STONE;

	/**
	 * The tower fixture BET (the base's selfHolder recipe): the 21.1 BE ctor VALIDATES the
	 * state against the type (BlockEntity :46 validateBlockState) — a null type NPEs there,
	 * so the fixture binds a real BET over the three fixture blocks.
	 */
	static BlockEntityType<TestTower> sTowerType;

	@BeforeAll
	static void buildTowerFixtureBet() {
		// the base's @BeforeAll already ran (bootstrap + the BET-registry unfreeze)
		@SuppressWarnings("unchecked")
		BlockEntityType<TestTower>[] tHolder = (BlockEntityType<TestTower>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new TestTower(tHolder[0], aPos, aState),
				Blocks.IRON_BLOCK, Blocks.STONE, Blocks.BRICKS).build(null);
		sTowerType = tHolder[0];
	}

	/** The tower fixture: fixture part blocks, scripted structure verdict, no output targets. */
	static class TestTower extends TileEntityDistillationTower {
		TestTower(BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			super(sTowerType, aPos, aState);
		}

		TestTower(@Nullable BlockEntityType<TestTower> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		protected Block getTransmitterBlock() {
			return TRANSMITTER;
		}

		@Override
		protected Block getPartBlock() {
			return PART;
		}

		/** The protected-save shim for the NBT round-trip (no Level needed). */
		public net.minecraft.nbt.CompoundTag saveTag() {
			net.minecraft.nbt.CompoundTag tTag = new net.minecraft.nbt.CompoundTag();
			saveAdditional(tTag);
			return tTag;
		}
	}

	private static TestTower newTower() {
		return new TestTower(P1, Blocks.BRICKS.defaultBlockState());
	}

	/** A plain recipe: 1 coal → 1 diamond, eUt 16, duration 100. */
	private static Recipe recipe() {
		return new Recipe(true, new ItemStack[] {new ItemStack(Items.COAL, 1)},
				new ItemStack[] {new ItemStack(Items.DIAMOND, 1)},
				new FluidStack[0], new FluidStack[0], 100, 16, 0);
	}

	// ---------------------------------------------------------------------------
	// ACCEPTANCE ① — the pattern per facing
	// ---------------------------------------------------------------------------

	@Test
	public void thePatternIs9TransmittersPlus72PartsPerFacing() {
		for (byte tFacing = 2; tFacing <= 5; tFacing++) {
			TestTower tTower = newTower();
			tTower.mFacing = tFacing;
			GTMultiBlockPattern tPattern = tTower.getStructurePattern();
			assertNotNull(tPattern, "the tower declares a pattern");
			assertEquals(81, tPattern.cells().size(), "9 transmitters + 8 layers x 9 parts, facing " + tFacing);

			int tTransmitters = 0, tInputLayer = 0, tOutputLayers = 0, tHoles = 0;
			for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
				assertTrue(tCell.forms(), "every tower cell carries the forming expectation");
				if (tCell.y == -1) {
					tTransmitters++;
					assertSame(TRANSMITTER, tCell.partBlock, "the base layer is the Heat Transmitter 18101");
					assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_IN, tCell.usage, "the base layer takes energy only");
					assertEquals(0, tCell.design, "no holes on the base layer");
				} else {
					assertSame(PART, tCell.partBlock, "the column is the Distill Part 18102");
					if (tCell.y == 0) {
						tInputLayer++;
						assertEquals(MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, tCell.usage, "the bottom layer is the only input layer");
					} else {
						tOutputLayers++;
						assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_OUT, tCell.usage, "the layers above only hand fluids out");
					}
					if (tCell.design == 1) tHoles++;
				}
			}
			assertEquals(9, tTransmitters, "3x3 base of Heat Transmitters, facing " + tFacing);
			assertEquals(9, tInputLayer, "the y0 input layer, facing " + tFacing);
			assertEquals(63, tOutputLayers, "7 x 9 output layers, facing " + tFacing);
			assertEquals(8, tHoles, "one back-centre hole per part layer (the y0 item hole + the 7 fluid holes), facing " + tFacing);
		}
	}

	@Test
	public void theHoleColumnLandsTwoOffsetsBehindTheControllerForEveryFacing() {
		// the upstream design-1 cell is the back-centre of each layer — at
		// controller - 2*OFF[facing] through the shared cellOffset arithmetic
		for (byte tFacing = 2; tFacing <= 5; tFacing++) {
			TestTower tTower = newTower();
			tTower.mFacing = tFacing;
			GTMultiBlockPattern tPattern = tTower.getStructurePattern();
			Direction tBack = Direction.from3DDataValue(tFacing).getOpposite();
			int tFound = 0;
			for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
				if (tCell.design != 1) continue;
				int[] tOffset = tPattern.worldOffset(tFacing, tCell);
				assertEquals(2 * tBack.getStepX(), tOffset[0], "hole x = controller - 2*OFF, facing " + tFacing);
				assertEquals(2 * tBack.getStepZ(), tOffset[2], "hole z = controller - 2*OFF, facing " + tFacing);
				tFound++;
			}
			assertEquals(8, tFound, "the hole column (8 part layers), facing " + tFacing);
		}
	}

	@Test
	public void theStructureBoxIsTheTowerCylinder() {
		TestTower tTower = newTower(); // facing north (2): centre one cell south
		tTower.mFacing = 2;
		assertTrue(tTower.isInsideStructure(P1.getX(), P1.getY() - 1, P1.getZ() + 1), "the base centre");
		assertTrue(tTower.isInsideStructure(P1.getX() + 1, P1.getY() + 7, P1.getZ() + 2), "the top back corner");
		assertTrue(tTower.isInsideStructure(P1.getX(), P1.getY(), P1.getZ()), "the controller itself");
		assertFalse(tTower.isInsideStructure(P1.getX(), P1.getY() + 8, P1.getZ() + 1), "above the tower");
		assertFalse(tTower.isInsideStructure(P1.getX(), P1.getY() - 2, P1.getZ() + 1), "below the base");
		assertFalse(tTower.isInsideStructure(P1.getX() + 2, P1.getY(), P1.getZ() + 1), "beside the tower");
	}

	// ---------------------------------------------------------------------------
	// ACCEPTANCE ② — the energy domain + the 互拒 offline half
	// ---------------------------------------------------------------------------

	@Test
	public void theRowsCarryTheirEnergyDomains() {
		assertSame(TD.Energy.HU, gregtech6.registry.GT6Distillation.TOWER_ROW.energyType(), ":1226 the tower takes HU");
		assertSame(TD.Energy.CU, gregtech6.registry.GT6Distillation.CRYO_ROW.energyType(), ":1227 the cryo tower takes CU");
		assertTrue(gregtech6.registry.GT6Distillation.CRYO_ROW.cryo(), "the CU row is the cryo flag");
		assertFalse(gregtech6.registry.GT6Distillation.TOWER_ROW.cryo(), "the HU row is not");
		assertEquals(17101, gregtech6.registry.GT6Distillation.TOWER_ROW.metaId());
		assertEquals(17111, gregtech6.registry.GT6Distillation.CRYO_ROW.metaId());
	}

	@Test
	public void doInjectChargesTheAcceptedTypeAndRefusesTheOther() {
		TestTower tTower = newTower(); // the offline fixture carries the HU default (row-less)
		tTower.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(512L, 1L, 1024L, null, true, null));

		long tGot = tTower.doInject(TD.Energy.HU, (byte)2, 32, 10, true);
		assertEquals(10, tGot, "the packet is consumed (the :503-505 math)");
		assertEquals(320, tTower.mEnergy, "32 x 10 HU landed in the buffer");

		long tRefused = tTower.doInject(TD.Energy.CU, (byte)2, 32, 10, true);
		assertEquals(0, tRefused, "the wrong domain is refused — the CU 互拒 offline half");
		assertEquals(320, tTower.mEnergy, "the refused packet left nothing behind");

		assertEquals(1, tTower.getEnergySizeInputMin(TD.Energy.HU, (byte)2), ":513");
		assertEquals(512, tTower.getEnergySizeInputRecommended(TD.Energy.HU, (byte)2), ":514");
		assertEquals(1024, tTower.getEnergySizeInputMax(TD.Energy.HU, (byte)2), ":515");
	}

	// ---------------------------------------------------------------------------
	// the row config + ACCEPTANCE ⑤ — the CHEAP_OVERCLOCKING first-consumer对拍
	// ---------------------------------------------------------------------------

	@Test
	public void theTowerRowSpecIsTheLoaderColumns() {
		TestTower tTower = newTower();
		// the constructor projection: NBT_INPUT 512 + MIN 1 + MAX 1024 + CHEAP_OVERCLOCKING T
		assertEquals(512, tTower.mInput);
		assertEquals(1, tTower.mInputMin);
		assertEquals(1024, tTower.mInputMax);
		assertTrue(tTower.mCheapOverclocking, ":1226 NBT_CHEAP_OVERCLOCKING T — the card ① semantic bit's first consumer");
		assertFalse(tTower.mRequiresIgnition, "the towers register no NBT_NEEDS_IGNITION");
		assertFalse(tTower.mNoConstantEnergy, "the towers register no NBT_NO_CONSTANT_POWER");
	}

	@Test
	public void cheapOverclockingRefusesTheFoldThroughTheTowerRecipeCheck() {
		RecipeMap tMap = new RecipeMap(new java.util.HashSet<>(), "gt6.test.towercheap", "Tower Cheap Test", null,
				0, 1, "gt6:textures/gui/machines/default", 1, 9, 1, 0, 1, 0, 1, 1);
		tMap.addRecipe(recipe());
		TestTower tTower = newTower();
		tTower.mRecipes = tMap;
		// a window whose MINIMUM sits above the recipe eUt — the fold fuel (the card ①
		// fixture shape); CHEAP_OVERCLOCKING T rides the tower constructor
		tTower.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(null, 512L, 4096L, null, true, null));
		tTower.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 1));
		tTower.mIgnited = 40;
		tTower.mEnergyTypeAccepted = TD.Energy.RU; // a non-TU shape, parallel 1 → count 1

		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tTower.checkRecipe(true, false));
		assertEquals(16, tTower.mMinEnergy, "the :773 fold never fires — the recipe runs at its natural eUt");
		assertEquals(1600, tTower.mMaxProgress, "natural duration: 16 x 100");
	}

	@Test
	public void withoutCheapOverclockingTheTowerWindowFolds() {
		RecipeMap tMap = new RecipeMap(new java.util.HashSet<>(), "gt6.test.towerfold", "Tower Fold Test", null,
				0, 1, "gt6:textures/gui/machines/default", 1, 9, 1, 0, 1, 0, 1, 1);
		tMap.addRecipe(recipe());
		TestTower tTower = newTower();
		tTower.mRecipes = tMap;
		// the SAME window with the flag OFF — the fold fires (the card ① control case)
		tTower.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(null, 512L, 4096L, null, false, null));
		tTower.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 1));
		tTower.mIgnited = 40;
		tTower.mEnergyTypeAccepted = TD.Energy.RU;

		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tTower.checkRecipe(true, false));
		assertEquals(1024, tTower.mMinEnergy, "16 -> 64 -> 256 -> 1024, then 1024*4 > 4096: three folds");
		assertEquals(12800, tTower.mMaxProgress, "each fold halves the run time: 1600 -> 12800 at 1024/tick");
	}

	// ---------------------------------------------------------------------------
	// the gated tick face + the routing table
	// ---------------------------------------------------------------------------

	@Test
	public void theTowerDoesNotSelfGenerate() {
		TestTower tTower = newTower();
		tTower.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(512L, 1L, 1024L, null, true, null));
		tTower.mEnergy = 1029; // one full window drain (1024) + 5 — the observable residue
		tTower.onTick(1L, true);
		assertEquals(5, tTower.mEnergy, "no +1/t self-generation: the HU/CU rows only charge from injection (upstream :455 gate)");
	}

	@Test
	public void theRoutingTableIsTheUpstreamFluidClassColumn() {
		assertEquals(7, TileEntityDistillationTower.routingLayerByName("gt6:propane"), ":152 propane to y+7");
		assertEquals(7, TileEntityDistillationTower.routingLayerByName("minecraft:methane"), ":152 methane to y+7");
		assertEquals(6, TileEntityDistillationTower.routingLayerByName("gt6:butane"), ":154");
		assertEquals(5, TileEntityDistillationTower.routingLayerByName("gt6:petrol"), ":156");
		assertEquals(5, TileEntityDistillationTower.routingLayerByName("gt6:gasoline"), ":156");
		assertEquals(5, TileEntityDistillationTower.routingLayerByName("gt6:bioethanol"), ":156");
		assertEquals(4, TileEntityDistillationTower.routingLayerByName("gt6:kerosene"), ":158");
		assertEquals(4, TileEntityDistillationTower.routingLayerByName("gt6:glycerol"), ":158");
		assertEquals(3, TileEntityDistillationTower.routingLayerByName("gt6:diesel"), ":160");
		assertEquals(3, TileEntityDistillationTower.routingLayerByName("gt6:biodiesel"), ":160");
		assertEquals(2, TileEntityDistillationTower.routingLayerByName("gt6:fuel"), ":162");
		assertEquals(2, TileEntityDistillationTower.routingLayerByName("gt6:fueloil"), ":162");
		assertEquals(2, TileEntityDistillationTower.routingLayerByName("gt6:biofuel"), ":162");
		assertEquals(1, TileEntityDistillationTower.routingLayerByName("gt6:creosote"), ":165 the default hole");
		assertEquals(1, TileEntityDistillationTower.routingLayerByName("gt6:oil"), ":165 the default hole");
	}

	@Test
	public void theInputTankRefusesADifferentFluid() {
		// the input tank accepts its own fluid and refuses a DIFFERENT one (the contains gate)
		TestTower tTower = newTower();
		assertTrue(tTower.mTankInput.add(1000, new FluidStack(Fluids.WATER, 1000)) > 0, "the tank fills");
		assertEquals(0, tTower.mTankInput.add(1000, new FluidStack(Fluids.LAVA, 1000)), "a different fluid is refused");
	}

	// ---------------------------------------------------------------------------
	// the nine-tank output bank (task p30-distill-output-routing, ruling option a)
	// ---------------------------------------------------------------------------

	/** Seven DISTINCT in-memory fluids — the registry identity is never queried offline. */
	private static final Fluid[] FRACTIONS;

	static {
		// a Fluid instance registers its intrusive holder on the VANILLA fluid registry
		// (Fluid.<init> → BuiltInRegistries.FLUID.createIntrusiveHolder) — reopen that
		// registry's write window (the GTOfflineTestBase BET-unfreeze recipe, reflected
		// onto the fluid wrapper; silent no-op where nothing matches). Shared both legs.
		for (Class<?> tClass = net.minecraft.core.registries.BuiltInRegistries.FLUID.getClass();
				tClass != null && tClass != Object.class; tClass = tClass.getSuperclass()) {
			try {
				java.lang.reflect.Method tUnfreeze = tClass.getDeclaredMethod("unfreeze");
				tUnfreeze.setAccessible(true);
				tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.FLUID);
				break;
			} catch (NoSuchMethodException tNotFound) {
				// climb the hierarchy
			} catch (Throwable tDead) {
				break;
			}
		}
		// the seven stand-ins ride REAL FluidStacks, so the identity needs a registry face:
		// registered under test-only gt6 keys (the JVM-wide registry keeps them — distinct
		// identities are what the per-tank placement needs)
		StandInFluid[] tBuilt = new StandInFluid[] {
				new StandInFluid(), new StandInFluid(), new StandInFluid(), new StandInFluid(),
				new StandInFluid(), new StandInFluid(), new StandInFluid()
		};
		//? if forge {
		// the Forge delegate face: FluidStack's ctor reads ForgeRegistry delegates — unfreeze
		// the Forge fluid registry too, then register (the delegates bake with the entry)
		for (Class<?> tClass = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getClass();
				tClass != null && tClass != Object.class; tClass = tClass.getSuperclass()) {
			try {
				java.lang.reflect.Method tUnfreeze = tClass.getDeclaredMethod("unfreeze");
				tUnfreeze.setAccessible(true);
				tUnfreeze.invoke(net.minecraftforge.registries.ForgeRegistries.FLUIDS);
				break;
			} catch (NoSuchMethodException tNotFound) {
				// climb the hierarchy
			} catch (Throwable tDead) {
				break;
			}
		}
		for (int i = 0; i < tBuilt.length; i++) {
			net.minecraftforge.registries.ForgeRegistries.FLUIDS.register(
					new net.minecraft.resources.ResourceLocation("gt6", "tower_stand_in_" + i), tBuilt[i]);
		}
		//?} else {
		/*// 21.1: no delegates — the vanilla register face is enough (the built-in holder
		   // the ctor's registry lookups ride; the unfreeze above opened the window)
		for (int i = 0; i < tBuilt.length; i++) {
			net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.FLUID,
					net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.FLUID,
							net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "tower_stand_in_" + i)),
					tBuilt[i]);
		}
		*///?}
		FRACTIONS = tBuilt;
	}

	/** The identity-only Fluid stand-in (the vanilla abstracts stubbed to dummies — the row mechanics never read properties). */
	private static final class StandInFluid extends Fluid {
		@Override public net.minecraft.world.item.Item getBucket() {return Items.BUCKET;}
		@Override protected boolean canBeReplacedWith(net.minecraft.world.level.material.FluidState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, Fluid aFluid, Direction aSide) {return false;}
		@Override protected net.minecraft.world.phys.Vec3 getFlow(net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, net.minecraft.world.level.material.FluidState aState) {return net.minecraft.world.phys.Vec3.ZERO;}
		@Override public float getOwnHeight(net.minecraft.world.level.material.FluidState aState) {return 1.0F;}
		@Override protected float getExplosionResistance() {return 1.0F;}
		@Override public float getHeight(net.minecraft.world.level.material.FluidState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos) {return 1.0F;}
		@Override public int getTickDelay(net.minecraft.world.level.LevelReader aLevel) {return 5;}
		@Override protected net.minecraft.world.level.block.state.BlockState createLegacyBlock(net.minecraft.world.level.material.FluidState aState) {return Blocks.STONE.defaultBlockState();}
		@Override public boolean isSource(net.minecraft.world.level.material.FluidState aState) {return true;}
		@Override public int getAmount(net.minecraft.world.level.material.FluidState aState) {return 8;}
		@Override public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.material.FluidState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos) {return net.minecraft.world.phys.shapes.Shapes.block();}
	}

	/** A seven-fraction row: water 25 in → seven 5 L products (the :356 row shape, stand-in fluids). */
	private static Recipe sevenFractionRow() {
		FluidStack[] tProducts = new FluidStack[7];
		for (int i = 0; i < 7; i++) tProducts[i] = new FluidStack(FRACTIONS[i], 5);
		return new Recipe(true, new ItemStack[0], new ItemStack[0],
				new FluidStack[] {new FluidStack(Fluids.WATER, 25)}, tProducts, 128, 64, 0);
	}

	@Test
	public void theOutputBankIsTheUpstreamNineTankLibrary() {
		TestTower tTower = newTower();
		assertEquals(9, TileEntityDistillationTower.OUTPUT_TANK_COUNT, "RM.java:65/:66 IN-OUT-MIN-FLUID 1/9/0");
		assertEquals(9, tTower.mTanksOutput.length, "the bank re-points to the map's fluid-OUT count (upstream readFromNBT2 :161)");
		assertEquals(Long.MAX_VALUE, tTower.mTanksOutput[0].capacity(), "the rows carry no NBT_TANK_CAPACITY — the FluidTankGT default stands");
		for (int i = 0; i < 9; i++) assertTrue(tTower.mTanksOutput[i].isEmpty(), "a fresh bank is empty, tank " + i);
	}

	@Test
	public void theSevenFractionRowPassesCanOutputAndEachFractionLandsInItsOwnTank() {
		Recipe tRow = sevenFractionRow();
		RecipeMap tMap = new RecipeMap(new java.util.HashSet<>(), "gt6.test.towerbank", "Tower Bank Test", null,
				0, 1, "gt6:textures/gui/machines/default", 1, 3, 0, 1, 9, 0, 1, 1);
		tMap.addRecipe(tRow);
		TestTower tTower = newTower();
		tTower.mRecipes = tMap; // the HU default energy domain rides the fixture ctor
		tTower.mTankInput.add(400, new FluidStack(Fluids.WATER, 400)); // 16 stages x 25 L

		// THE gate the W3④ freeze used to fail: 7 fluid outputs need 7 free tanks — the
		// nine-tank bank has them (upstream canOutput :650-666, the old verdict was 0 at
		// TileEntityBase10MultiBlockMachine :735-:736)
		assertEquals(16, tTower.canOutput(tRow), "7 outputs x 9 tanks: the row is runnable again");
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tTower.checkRecipe(true, false));

		// complete the process — the placement half of doActive (:817-835) fills the bank.
		// The :743 energy bind (HU, window 512 / eUt 64) folds the parallel count to 8 →
		// eight stages of the 5 L products land per tank.
		tTower.mProgress = tTower.mMaxProgress;
		tTower.doActive(5L, 16);

		int tFilled = 0;
		for (int i = 0; i < 7; i++) {
			FluidStack tExpected = new FluidStack(FRACTIONS[i], 40);
			boolean tFound = false;
			for (FluidTankGT tTank : tTower.mTanksOutput) if (tTank.contains(tExpected) && tTank.amount() == 40) { tFound = true; break; }
			assertTrue(tFound, "fraction " + i + " sits alone in its own tank at 40 L");
		}
		for (FluidTankGT tTank : tTower.mTanksOutput) if (!tTank.isEmpty()) tFilled++;
		assertEquals(7, tFilled, "exactly seven of the nine tanks hold product");
	}

	@Test
	public void theRoutingCellSitsOnTheArmColumnAtTheClassLayer() {
		TestTower tTower = newTower();
		tTower.mFacing = 2; // north — the arm column runs +Z (the back of the facing)
		assertEquals(new BlockPos(120, 64, 103), tTower.offsetBy((byte) 2, 3), "offset 3 behind the facing");
		assertEquals(new BlockPos(120, 65, 103), tTower.routingCell(Fluids.WATER), "minecraft:water carries no class word → the default y+1 hole");
		tTower.mFacing = 5; // east — the arm column runs -X
		assertEquals(new BlockPos(117, 65, 100), tTower.routingCell(Fluids.LAVA), "minecraft:lava → default y+1 on its own column");
	}

	@Test
	public void theOutputBankRoundTripsAndLegacySingleTankSavesLoad() {
		TestTower tTower = newTower();
		tTower.mTanksOutput[1].add(500, new FluidStack(Fluids.WATER, 500));
		tTower.mTanksOutput[8].add(250, new FluidStack(Fluids.LAVA, 250));

		net.minecraft.nbt.CompoundTag tTag = tTower.saveTag();
		assertTrue(tTag.contains("output_tank_1"), "the per-index key rides (the upstream NBT_TANK.out.i form)");
		assertTrue(tTag.contains("output_tank_8"), "the tail key rides");

		TestTower tLoaded = newTower();
		tLoaded.load(tTag);
		assertEquals(500, tLoaded.mTanksOutput[1].amount(), "bank tank 1 restored");
		assertEquals(Fluids.WATER, tLoaded.mTanksOutput[1].fluid().getFluid());
		assertEquals(250, tLoaded.mTanksOutput[8].amount(), "bank tank 8 restored");
		assertTrue(tLoaded.mTanksOutput[0].isEmpty(), "tank 0 stayed empty");

		// the LEGACY save: one "output_tank" key only (the old single-tank shape) — tank 0
		// loads through the base key untouched, the tail stays empty (declared-compatible).
		// The payload is produced by the tank's OWN writer (the exact shape a legacy save
		// carries, leg-portable).
		FluidTankGT tLegacyTank = new FluidTankGT();
		tLegacyTank.add(300, new FluidStack(Fluids.WATER, 300));
		net.minecraft.nbt.CompoundTag tLegacy = new net.minecraft.nbt.CompoundTag();
		tLegacyTank.writeToNBT(tLegacy, "output_tank");
		TestTower tOld = newTower();
		tOld.load(tLegacy);
		assertEquals(300, tOld.mTanksOutput[0].amount(), "the legacy single tank lands in bank slot 0 verbatim");
		assertEquals(Fluids.WATER, tOld.mTanksOutput[0].fluid().getFluid());
		assertTrue(tOld.mTanksOutput[5].isEmpty(), "the tail tanks stay empty on a legacy save");
	}
}
