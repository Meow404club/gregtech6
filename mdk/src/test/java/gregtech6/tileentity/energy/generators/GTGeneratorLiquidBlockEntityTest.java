package gregtech6.tileentity.energy.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.fluid.GTFluidLists;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task p13-burning-box-family — the offline fixture for the Liquid/GAS Burning Box
 * family: the FM.Burn burn cycle (the :135-145 probe/consume/while-loop over an
 * injected fixture map), the invalid-fuel tank clear (:146-149), the Tap face
 * (:228-231), the gas/liquid fill-gate inversion (Liquid :214 vs Gas :41), the GAS
 * on-off stop face (Gas :44-46), the cooldown decay (:166), and the FluidBed
 * declared-empty surface (the calcite/ash primitives stay pooled).
 */
public class GTGeneratorLiquidBlockEntityTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(0, 0, 0);
	static BlockEntityType<FixtureLiquidBox> sLiquidType;
	static BlockEntityType<FixtureGasBox> sGasType;
	static BlockEntityType<FixtureFluidBed> sFluidBedType;

	public static final class FixtureLiquidBox extends GTGeneratorLiquidBlockEntity {
		public FixtureLiquidBox(BlockPos aPos, BlockState aState) {super(sLiquidType, aPos, aState);}
	}

	public static final class FixtureGasBox extends GTGeneratorGasBlockEntity {
		public FixtureGasBox(BlockPos aPos, BlockState aState) {super(sGasType, aPos, aState);}
	}

	public static final class FixtureFluidBed extends GTGeneratorFluidBedBlockEntity {
		public FixtureFluidBed(BlockPos aPos, BlockState aState) {super(sFluidBedType, aPos, aState);}
	}

	/** A one-row FM.Burn fixture: 1 L WATER-class in, |−64 × 5| = 320 GU raw (the diesel BURN row values, Loader_Fuels.java:90). */
	static RecipeMap oneRowBurnMap(String aName) {
		RecipeMap tMap = new RecipeMap(new java.util.HashSet<>(),
				aName, "Test Burn", null, 0, 1, "gt6:textures/gui/machines/default",
				1, 2, 0, 1, 2, 0, 1, 1);
		tMap.addRecipe(new Recipe(true,
				new ItemStack[0], new ItemStack[0],
				new FluidStack[] {new FluidStack(Fluids.WATER, 1)}, new FluidStack[0],
				-64, 5, 0));
		return tMap;
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		sLiquidType = BlockEntityType.Builder.of(FixtureLiquidBox::new, Blocks.STONE).build(null);
		sGasType = BlockEntityType.Builder.of(FixtureGasBox::new, Blocks.STONE).build(null);
		sFluidBedType = BlockEntityType.Builder.of(FixtureFluidBed::new, Blocks.STONE).build(null);
		GT6RecipeMaps.init();
	}

	@Test
	public void theBurnCycleChargesConsumesAndArmsTheCooldown() {
		FixtureLiquidBox tBox = new FixtureLiquidBox(POS, Blocks.STONE.defaultBlockState());
		tBox.mRate = 16;
		tBox.mBurnMapOverride = oneRowBurnMap("test.burn.cycle");
		tBox.mTank.fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE);
		tBox.mBurning = true; // the box was lit (ignite command / a front fire)
		tBox.mEnergy = 0;

		// one burning tick: buffer under 2×mRate → the :135 lookup finds the row, consumes
		// 1 L, charges 320 HU (the :142 while-loop: 320 ≥ 2×16 → stops after one)
		tBox.onTick(1, true);
		assertTrue(tBox.mBurning, ":137 — the found fuel re-arms the flame");
		assertEquals(99, tBox.mCooldown, ":138 arms 100, then the :166 decay runs in the SAME tick — 99 after one burning tick (the upstream order verbatim)");
		assertEquals(9, tBox.mTank.amount(), "1 L consumed");
		assertEquals(320, tBox.mEnergy, ":140 — units(320, 10000, eff 10000) = the full 320 HU");
	}

	@Test
	public void theWhileLoopBurnsUntilTheHeadroomFills() {
		FixtureLiquidBox tBox = new FixtureLiquidBox(POS, Blocks.STONE.defaultBlockState());
		tBox.mBurnMapOverride = oneRowBurnMap("test.burn.while");
		tBox.mTank.fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE);
		tBox.mBurning = true;
		tBox.mEnergy = 0;
		tBox.mRate = 1280; // 2×mRate 2560: the :142 loop keeps burning until the buffer reaches it
		tBox.onTick(1, true);
		assertEquals(2, tBox.mTank.amount(), "8 L burned (8 × 320 = 2560 ≥ 2560 — the loop stops with head-room)");
		assertEquals(2560, tBox.mEnergy, "8 × 320 HU");
	}

	@Test
	public void theInvalidFuelClearsTheTank() {
		FixtureLiquidBox tBox = new FixtureLiquidBox(POS, Blocks.STONE.defaultBlockState());
		tBox.mRate = 16;
		tBox.mBurnMapOverride = oneRowBurnMap("test.burn.invalid"); // a WATER-class row
		tBox.mTank.fill(new FluidStack(Fluids.LAVA, 10), IFluidHandler.FluidAction.EXECUTE);
		tBox.mBurning = true;
		tBox.mEnergy = 0;
		tBox.onTick(1, true);
		assertTrue(tBox.mTank.isEmpty(), ":148 — the fuel-type swap clear (no row answers a lava tank)");
		assertFalse(tBox.mBurning, ":156 — out of fuel (the charge never landed)");
	}

	@Test
	public void theTapFaceDrainsTheFuelTankVerbatim() {
		FixtureLiquidBox tBox = new FixtureLiquidBox(POS, Blocks.STONE.defaultBlockState());
		tBox.mTank.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
		// the SIMULATE arm
		FluidStack tProbe = tBox.tapDrain((byte)0, Integer.MAX_VALUE, false);
		assertEquals(100, tProbe.getAmount(), "the probe answers the content");
		assertEquals(100, tBox.mTank.amount(), "the probe books nothing");
		// the EXECUTE arm
		FluidStack tTaken = tBox.tapDrain((byte)2, 40, true);
		assertEquals(40, tTaken.getAmount(), "the take");
		assertEquals(60, tBox.mTank.amount(), "the tank ledger after the take");
		// the :218-220 burning gate is the PIPE face only — the tap bypasses it even while burning
		tBox.mBurning = true;
		assertEquals(60, tBox.tapDrain((byte)5, Integer.MAX_VALUE, true).getAmount(), "the tap face has no burning gate (upstream :228-231 verbatim)");
	}

	@Test
	public void theGasFillGateInvertsTheLiquidTail() {
		FixtureGasBox tGasBox = new FixtureGasBox(POS, Blocks.STONE.defaultBlockState());
		FixtureLiquidBox tLiquidBox = new FixtureLiquidBox(POS.offset(1, 0, 0), Blocks.STONE.defaultBlockState());
		RecipeMap tMap = oneRowBurnMap("test.burn.gas");
		tGasBox.mBurnMapOverride = tMap;
		tLiquidBox.mBurnMapOverride = tMap;
		// register the row fluid's registry path as a gas (the offline carrier of FL.gas; live: GTFluids.STEAM registration)
		GTFluidLists.GAS.add("water");
		try {
			FluidStack tRowFuel = new FluidStack(Fluids.WATER, 10);
			assertTrue(tGasBox.containsBurnInput(tRowFuel), "the row answers for both families");
			assertTrue(tGasBox.acceptsFuelFluidGas(tRowFuel), "Gas :41 — the isGas tail accepts");
			assertFalse(tLiquidBox.acceptsFuelFluid(tRowFuel), "Liquid :214 — the !isGas tail refuses");
			assertTrue(GTFluidLists.isGas("steam") == GTFluidLists.GAS.contains("steam"), "the set mechanics — the seeds are the truth");
		} finally {
			GTFluidLists.GAS.remove("water");
		}
	}

	@Test
	public void theGasStopFaceClearsBurningAndCooldown() {
		FixtureGasBox tBox = new FixtureGasBox(POS, Blocks.STONE.defaultBlockState());
		tBox.mBurning = true;
		tBox.mCooldown = 55;
		assertTrue(tBox.getStateOnOff(), ":46 — the live state");
		// upstream :44 returns the POST state (return mBurning after the flip)
		assertFalse(tBox.setAdjacentOnOff(false), ":44 — the live (post-flip) state, false");
		assertFalse(tBox.mBurning, "the flame is out");
		assertEquals(0, tBox.mCooldown, "the cooldown cleared with the flame");
		assertFalse(tBox.getStateOnOff(), ":46 — and stays out");
		assertFalse(tBox.setAdjacentOnOff(false), ":44 — an already-dead box answers false");
	}

	@Test
	public void theCooldownDecaysEachBurningTick() {
		FixtureLiquidBox tBox = new FixtureLiquidBox(POS, Blocks.STONE.defaultBlockState());
		tBox.mRate = 16;
		tBox.mBurning = true;
		tBox.mEnergy = 1000; // no refuel (1000−16 ≥ 32)
		tBox.mCooldown = 3;
		tBox.onTick(1, true);
		assertEquals(2, tBox.mCooldown, ":166 — one per tick");
	}

	@Test
	public void theFluidBedSurfaceIsDeclaredEmptyAndRefusesEverything() {
		FixtureFluidBed tBox = new FixtureFluidBed(POS, Blocks.STONE.defaultBlockState());
		assertTrue(tBox.fluidBedMap() == GT6RecipeMaps.FLUIDBED, "the live map");
		assertEquals(0, tBox.fluidBedMap().mRecipeList.size(), "the calcite/ash/burn-time primitives are pool-card content (Loader_Fuels.java:37-43 evidence in the pour class doc)");
		assertEquals(0, tBox.funnelFill(new FluidStack(Fluids.WATER, 100), true),
				":245 — the containsInput gate refuses (no rows → nothing is a fluid-bed input)");
		assertFalse(tBox.containsFluidBedItem(new ItemStack(Items.COAL)), "the :258 item gate — the empty map refuses everything");
	}
}
