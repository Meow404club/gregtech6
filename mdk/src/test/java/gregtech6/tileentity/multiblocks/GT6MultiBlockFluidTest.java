package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * The multiblock fluid-capability offline face (task p8-cokeoven-fluid-capability spec ⑦):
 * the side-rule truth table over the pure rotation+mask seams — 4 horizontal facings ×
 * 6 world faces + the side-less query, UP refuses, the five other faces and null admit —
 * and the fill-is-always-zero contract. The wrapper drain/fill semantics run against the
 * offline fixture oven (the GTMultiBlocksOfflineTestBase boot; the LIVE capability chain
 * through getCapability is the RCON gate, the card's 活体 ruling).
 */
class GT6MultiBlockFluidTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(100, 64, 100);

	private static final byte[] FACINGS = {(byte)Direction.NORTH.get3DDataValue(), (byte)Direction.SOUTH.get3DDataValue(),
			(byte)Direction.WEST.get3DDataValue(), (byte)Direction.EAST.get3DDataValue()};

	private static TileEntityCokeOven newOven(MultiBlockLevel aLevel, byte aFacing) {
		TileEntityCokeOven tOven = sCokeOvenType.create(P1, Blocks.BRICKS.defaultBlockState());
		tOven.setLevel(aLevel);
		tOven.mFacing = aFacing; // the rotation anchor (setFacingFromPlacement form, byte level)
		aLevel.mStates.put(P1, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(P1, tOven);
		return tOven;
	}

	/** The spec ② truth table: UP refuses, the five other faces admit, null admits — for every horizontal facing. */
	@Test
	void drainSideTruthTable() {
		for (byte tFacing : FACINGS) {
			assertFalse(MultiBlockFluidHandler.drainAllowedBySide(tFacing, Direction.UP), "facing " + tFacing + ": UP refuses");
			for (Direction tSide : Direction.values()) {
				if (tSide == Direction.UP) continue;
				assertTrue(MultiBlockFluidHandler.drainAllowedBySide(tFacing, tSide), "facing " + tFacing + " side " + tSide + " admits");
			}
			assertTrue(MultiBlockFluidHandler.drainAllowedBySide(tFacing, null), "facing " + tFacing + ": the side-less query admits");
		}
	}

	/** The table integrity: vertical facings keep the identity rows, and the relative top NEVER rotates off world UP. */
	@Test
	void rotationTableInvariants() {
		for (byte tFacing = 0; tFacing < 8; tFacing++) {
			assertEquals(0, MultiBlockFluidHandler.relativeSide(tFacing, (byte)0), "facing " + tFacing + ": bottom stays bottom");
			assertEquals(1, MultiBlockFluidHandler.relativeSide(tFacing, (byte)1), "facing " + tFacing + ": top stays top");
		}
		// the machine's own facing is the relative FRONT (3) — the FACING_ROTATIONS row spot check
		assertEquals(3, MultiBlockFluidHandler.relativeSide((byte)Direction.NORTH.get3DDataValue(), (byte)Direction.NORTH.get3DDataValue()));
		assertEquals(3, MultiBlockFluidHandler.relativeSide((byte)Direction.SOUTH.get3DDataValue(), (byte)Direction.SOUTH.get3DDataValue()));
		assertEquals(3, MultiBlockFluidHandler.relativeSide((byte)Direction.WEST.get3DDataValue(), (byte)Direction.WEST.get3DDataValue()));
		assertEquals(3, MultiBlockFluidHandler.relativeSide((byte)Direction.EAST.get3DDataValue(), (byte)Direction.EAST.get3DDataValue()));
		// facing north: front = world north, back = world south (the CS.java:528-537 rows)
		assertEquals(5, MultiBlockFluidHandler.relativeSide((byte)Direction.NORTH.get3DDataValue(), (byte)Direction.SOUTH.get3DDataValue()));
	}

	/** The spec ① fill contract: the rotated mask 0 refuses every face including the side-less probe. */
	@Test
	void fillIsAlwaysRefused() {
		for (byte tFacing : FACINGS) {
			for (Direction tSide : Direction.values()) {
				assertFalse(MultiBlockFluidHandler.fillAllowedBySide(tFacing, tSide), "facing " + tFacing + " side " + tSide + " refuses fill");
			}
			assertFalse(MultiBlockFluidHandler.fillAllowedBySide(tFacing, null), "facing " + tFacing + ": the side-less probe refuses fill");
		}
	}

	/** The wrapper face over the fixture oven: tanks=1, the int-contract capacity truncation, isFluidValid false. */
	@Test
	void wrapperTankFace() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TileEntityCokeOven tOven = newOven(tLevel, (byte)Direction.NORTH.get3DDataValue());
		tOven.mTanksOutput[0].add(500, new FluidStack(Fluids.WATER, 500));
		IFluidHandler tHandler = new MultiBlockFluidHandler(tOven, null);

		assertEquals(1, tHandler.getTanks());
		assertEquals(500, tHandler.getFluidInTank(0).getAmount());
		assertEquals(Fluids.WATER, tHandler.getFluidInTank(0).getFluid());
		assertEquals(FluidStack.EMPTY, tHandler.getFluidInTank(1), "the wrapper fronts exactly one tank");
		// the declared truncation: the upstream default capacity Long.MAX_VALUE reads Integer.MAX_VALUE through the int contract
		assertEquals(Integer.MAX_VALUE, tHandler.getTankCapacity(0));
		assertEquals(0, tHandler.getTankCapacity(1));
		assertFalse(tHandler.isFluidValid(0, new FluidStack(Fluids.WATER, 1)), "the take-nothing face");
	}

	/** The drain semantics: simulate never moves, execute deducts + flags, typed drain matches the fluid, UP refuses, five sides + null drain. */
	@Test
	void drainSemantics() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TileEntityCokeOven tOven = newOven(tLevel, (byte)Direction.NORTH.get3DDataValue());
		tOven.mTanksOutput[0].add(500, new FluidStack(Fluids.WATER, 500));

		// simulate: nothing moves, nothing flags
		IFluidHandler tAny = new MultiBlockFluidHandler(tOven, null);
		assertEquals(200, tAny.drain(200, FluidAction.SIMULATE).getAmount());
		assertEquals(500, tOven.mTanksOutput[0].amount(), "simulate keeps the tank");
		assertFalse(tOven.mInventoryChanged, "simulate keeps the change flag down");

		// execute: deduct + the tapDrain :919 updateInventory mirror (setChanged + mInventoryChanged)
		assertEquals(200, tAny.drain(200, FluidAction.EXECUTE).getAmount());
		assertEquals(300, tOven.mTanksOutput[0].amount(), "execute deducts");
		assertTrue(tOven.mInventoryChanged, "the executed drain feeds the re-check window");

		// typed drain: same fluid lands, a foreign fluid refuses (the :579 contains leg)
		assertEquals(100, tAny.drain(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE).getAmount());
		assertEquals(200, tOven.mTanksOutput[0].amount());
		assertEquals(FluidStack.EMPTY, tAny.drain(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE), "fluid mismatch refuses");
		FluidStack tOver = tAny.drain(new FluidStack(Fluids.WATER, 10000), FluidAction.EXECUTE);
		assertEquals(200, tOver.getAmount(), "the typed over-drain caps at the tank content");
		assertEquals(0, tOven.mTanksOutput[0].amount());
		assertEquals(FluidStack.EMPTY, tAny.drain(100, FluidAction.EXECUTE), "the empty tank refuses");

		// refill for the side sweep: UP refuses, the five other faces + null drain
		tOven.mTanksOutput[0].add(100, new FluidStack(Fluids.WATER, 100));
		IFluidHandler tUp = new MultiBlockFluidHandler(tOven, Direction.UP);
		assertEquals(FluidStack.EMPTY, tUp.drain(1000, FluidAction.EXECUTE), "UP refuses");
		assertEquals(100, tOven.mTanksOutput[0].amount(), "UP touched nothing");
		assertEquals(FluidStack.EMPTY, tUp.drain(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE), "the typed UP drain refuses too");
		for (Direction tSide : Direction.values()) {
			if (tSide == Direction.UP) continue;
			FluidStack tDrawn = new MultiBlockFluidHandler(tOven, tSide).drain(1, FluidAction.EXECUTE);
			assertEquals(1, tDrawn.getAmount(), "side " + tSide + " drains 1");
		}
		assertEquals(100 - 5, tOven.mTanksOutput[0].amount(), "the five faces took exactly 1 each");
	}
}
