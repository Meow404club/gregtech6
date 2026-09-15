package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import gregapi.data.TD;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import net.minecraftforge.fluids.FluidStack;

/**
 * Acceptance ③ of task p29-w4-f1-chemicals: the FM.Gas METHANE row BURNS in the Gas
 * Turbine — the {@link GT6MultiBlockConverterTest.TestGasConverter} fixture posture with
 * the row the shipped gas_fuels.json carries (GT6ChemicalRowsPourTest pins the SAME
 * parameters off the verbatim JSON bytes, so the two legs join into "the shipped row runs
 * the consumer"): Loader_Fuels.java:161 — methane 5 L, −64 EUt × 30, water 6 + CO2 3.
 * The byproduct leg is the p29-w4 closure's increment over the W3 fixture row (the
 * exhaust tanks split 42|42 under the stand-in identity), the LIMIT_CONSUMPTION cap
 * holds the packet at outMax, the deficit-ceiling parallel is 7.
 */
public class GT6ChemicalGasTurbineRowTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<GT6MultiBlockConverterTest.TestGasConverter> sChemGasType;

	/** The methane-row power: |−64 × 30| (the :161 column pair). */
	private static final long METHANE_ROW_POWER = 64L * 30;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildConverterFixture() {
		sChemGasType = holder((BlockEntityType<GT6MultiBlockConverterTest.TestGasConverter>[] t) ->
				t[0] = BlockEntityType.Builder.of(
						(aPos, aState) -> new GT6MultiBlockConverterTest.TestGasConverter(t[0], aPos, aState),
						Blocks.BRICKS).build(null));
	}

	/** The self-referencing BET holder (the GT6MultiBlockConverterTest.holder form). */
	private static <T extends BlockEntity> BlockEntityType<T> holder(
			java.util.function.Consumer<BlockEntityType<T>[]> aBuilder) {
		@SuppressWarnings("unchecked")
		BlockEntityType<T>[] tHolder = (BlockEntityType<T>[]) new BlockEntityType<?>[1];
		aBuilder.accept(tHolder);
		return tHolder[0];
	}

	/**
	 * The :161 row as a Recipe — vanilla water stands in for every fluid (the
	 * synthetic-universe convention; the shipped-JSON face of these exact numbers is the
	 * GT6ChemicalRowsPourTest.gasFuelRowsCarryTheFuelSemantics assertion).
	 */
	private static void pourTheMethaneRow() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.GAS_FUELS.addRecipe(new Recipe(true,
				new net.minecraft.world.item.ItemStack[0], new net.minecraft.world.item.ItemStack[0],
				new FluidStack[] {new FluidStack(Fluids.WATER, 5)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 6), new FluidStack(Fluids.LAVA, 3)},
				30, -64, 0));
	}

	private GT6MultiBlockConverterTest.TestGasConverter methaneTurbine() {
		GT6MultiBlockConverterTest.TestGasConverter tConverter =
				new GT6MultiBlockConverterTest.TestGasConverter(sChemGasType, BlockPos.ZERO, Blocks.BRICKS.defaultBlockState());
		tConverter.applyRow(6144, 4096, TD.Energy.HU, TD.Energy.RU, false, true);
		tConverter.rederiveTankCapacities();
		return tConverter;
	}

	/** The methane row burns: 5 L units, the deficit ceiling of 7 units, the exhaust split, the outMax cap. */
	@Test
	public void theMethaneRowBurnsIntoTheCapacitorWithTheExhaustSplit() {
		pourTheMethaneRow();
		GT6MultiBlockConverterTest.TestGasConverter tConverter = methaneTurbine();
		assertTrue(tConverter.mLimitConsumption, "the gas rows carry LIMIT_CONSUMPTION");
		// the containment gate answers the fuel over the poured map
		assertNotNull(GTGasTurbineBlockEntity.findFuelRecipeFor(new FluidStack(Fluids.WATER, 100)));
		tConverter.mInputTank.add(6144, new FluidStack(Fluids.WATER, 1));
		// the deficit ceiling: ceil(12288 / 1920) = 7; the tank holds 6144/5 = 1228 units → parallel 7
		tConverter.doConversion(3);
		assertEquals(7 * METHANE_ROW_POWER, tConverter.mStorage, "parallel × |EUt × duration| credited");
		assertEquals(6144 - 35, tConverter.mInputTank.amount(), "7 × 5 L consumed");
		assertEquals(42, tConverter.mTanksOutput[0].amount(), "7 × 6 L water exhaust (tank 0)");
		assertEquals(21, tConverter.mTanksOutput[1].amount(), "7 × 3 L CO2 exhaust (tank 1) — the restored byproduct leg");
		assertEquals(8192, tConverter.mLastConverted, "the LIMIT_CONSUMPTION cap holds the packet at outMax");
	}
}
