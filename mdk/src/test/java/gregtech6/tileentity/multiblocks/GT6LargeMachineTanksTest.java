package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregtech6.fluid.FluidTankGT;
import gregtech6.registry.GT6LargeMachines;
import gregtech6.registry.GT6LargeMachines.GTLargeMachineBlockEntity;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The debt-tank-shadow pins — the large-machine input-tank bank over the base
 * {@code TileEntityBase10MultiBlockMachine.mTanksInput} (the shadow-field fix):
 * <ol>
 * <li>the multi-tank bank survives the NBT round-trip — before the fix the base
 *     save/load :1051/:1081 chains bound the shadow-zero-length array, so the base
 *     chain was dead (the subclass patch only reached tank 0);</li>
 * <li>the bank is the map row's {@code mInputFluidCount} — before the fix the shadow
 *     field hard-coded one tank, silently keeping the MIXER six-fluid rows
 *     unreachable (checkRecipe :768-775 walks the bank);</li>
 * <li>the zero-fluid rows (CRUSHER/SHREDDER/SQUEEZER, bank length 0) keep the
 *     capability face total.</li>
 * </ol>
 * The capacity rides the default FluidTankGT (Long.MAX — the fusion :147 /
 * distill-tower precedent; the upstream :159 1000 + mMinInputTankSizes adjustable leg
 * is unported — the card ruling).
 */
public class GT6LargeMachineTanksTest extends GTOfflineTestBase {

	static BlockEntityType<GTLargeMachineBlockEntity> sLargeType;
	private static final BlockPos P1 = new BlockPos(310, 64, 100);

	@BeforeAll
	static void bootFixtureBet() {
		// the idempotent bootstrap (GTMachinesOfflineTestBase.java:144 form) — the row
		// ctor resolves the row's real map through the lazy supplier
		gregtech6.recipes.GT6RecipeMaps.init();
		@SuppressWarnings("unchecked")
		BlockEntityType<GTLargeMachineBlockEntity>[] tHolder =
				(BlockEntityType<GTLargeMachineBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTLargeMachineBlockEntity(tHolder[0], aPos, aState, null),
				Blocks.BRICKS).build(null);
		sLargeType = tHolder[0];
	}

	private static GTLargeMachineBlockEntity machine(String aPath) {
		return new GTLargeMachineBlockEntity(sLargeType, P1, Blocks.BRICKS.defaultBlockState(), GT6LargeMachines.ROWS_BY_PATH.get(aPath));
	}

	/** ② — the bank is the map truth (the fusion :147 re-point form, per row). */
	@Test
	public void theBankIsTheMapRowSize() {
		for (GT6LargeMachines.LargeMachineRow tRow : GT6LargeMachines.ROWS) {
			GTLargeMachineBlockEntity tMachine = new GTLargeMachineBlockEntity(sLargeType, P1, Blocks.BRICKS.defaultBlockState(), tRow);
			assertEquals(tRow.recipes().get().mInputFluidCount, tMachine.mTanksInput.length,
					tRow.path() + ": the input bank = the map mInputFluidCount");
		}
		assertEquals(6, machine("large_batch_mixer").mTanksInput.length, "the MIXER six-fluid bank");
		assertEquals(2, machine("large_electrolyzer").mTanksInput.length, "the ELECTROLYZER two-fluid bank");
		assertEquals(1, machine("large_coagulator").mTanksInput.length, "the one-tank row shape");
		assertEquals(0, machine("large_crusher").mTanksInput.length, "the zero-fluid rows keep the empty base bank");
	}

	/** ① — the kill pin: two filled mixer tanks ride the base save/load chain both ways. */
	@Test
	public void theMultiTankBankSurvivesTheRoundTrip() {
		GTLargeMachineBlockEntity tMachine = machine("large_batch_mixer");
		FluidStack tWater = new FluidStack(Fluids.WATER, 1000);
		FluidStack tLava = new FluidStack(Fluids.LAVA, 2000);
		assertEquals(1000, tMachine.mTanksInput[0].fill(tWater, FluidAction.EXECUTE));
		assertEquals(2000, tMachine.mTanksInput[1].fill(tLava, FluidAction.EXECUTE));

		CompoundTag tSaved = tMachine.saveWithoutMetadata(); // the base :1051 loop binds the LIVE bank now

		GTLargeMachineBlockEntity tBack = machine("large_batch_mixer");
		tBack.load(tSaved); // the base :1081 loop reads input_tank / input_tank_1..5

		assertEquals(6, tBack.mTanksInput.length, "the bank size survives");
		assertTrue(tBack.mTanksInput[0].contains(tWater), "tank 0 keeps the water");
		assertEquals(1000, tBack.mTanksInput[0].amount(), "tank 0 keeps the 1000 L");
		assertTrue(tBack.mTanksInput[1].contains(tLava), "tank 1 keeps the lava — the shadowed field dropped this half");
		assertEquals(2000, tBack.mTanksInput[1].amount(), "tank 1 keeps the 2000 L");
	}

	/** The default capacity rides (the card ruling — fusion/distill precedent, not the upstream 1000). */
	@Test
	public void theBankCapacityIsTheDefaultLongMax() {
		GTLargeMachineBlockEntity tMachine = machine("large_batch_mixer");
		assertEquals(new FluidTankGT().getCapacity(), tMachine.mTanksInput[0].getCapacity(),
				"the fusion :147 default-ctor capacity (Long.MAX)");
	}

	/** ③ — the zero-bank rows keep the capability face total; the fed rows still fill. */
	@Test
	public void theZeroBankRowsKeepTheCapabilityFaceTotal() {
		GTLargeMachineBlockEntity tCrusher = machine("large_crusher");
		GTLargeMachineBlockEntity.LargeMachineFluidHandler tFace =
				new GTLargeMachineBlockEntity.LargeMachineFluidHandler(tCrusher, null);
		assertEquals(0, tFace.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE), "the empty bank refuses the fill");
		assertTrue(tFace.getFluidInTank(0).isEmpty(), "tank 0 reads EMPTY");
		assertEquals(0, tFace.getTankCapacity(0), "the empty tank capacity reads 0");

		// the fed single-tank row still fills through the face (the all-open face, unchanged)
		GTLargeMachineBlockEntity tCoagulator = machine("large_coagulator");
		GTLargeMachineBlockEntity.LargeMachineFluidHandler tFedFace =
				new GTLargeMachineBlockEntity.LargeMachineFluidHandler(tCoagulator, null);
		assertEquals(1000, tFedFace.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
		assertEquals(1000, tCoagulator.mTanksInput[0].amount(), "the fill landed in the live bank");
	}
}
