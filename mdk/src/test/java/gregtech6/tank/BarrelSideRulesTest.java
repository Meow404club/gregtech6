package gregtech6.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.tank.BarrelFluidHandler;
import gregtech6.tileentity.tank.GTBarrelBlockEntity;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;

/**
 * The p5 side-rule acceptance tables (task p5-barrel-side-rules acceptance ①/④/⑤/⑦),
 * all offline. The sign verdicts run through the raw {@code (byte, int)} seams — the
 * FluidType density lookup is live-registry territory (the P4 offline lesson), and the
 * live shape (water/iron/natural_gas against real faces) is the RCON chain's job. The
 * null-side path never consults the content, so the wrapper-level drain test is
 * offline-safe by construction.
 */
public class BarrelSideRulesTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(2, 3, 4);

	static BlockEntityType<GTBarrelBlockEntity> sType;

	@SuppressWarnings("unchecked")
	@BeforeAll
	static void buildOfflineFixtures() {
		BlockEntityType<GTBarrelBlockEntity>[] tHolder = (BlockEntityType<GTBarrelBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sType = tHolder[0];
	}

	static GTBarrelBlockEntity barrel() {
		return sType.create(POS, Blocks.STONE.defaultBlockState());
	}

	static BarrelFluidHandler handler(GTBarrelBlockEntity aBarrel, @Nullable Direction aSide) {
		return new BarrelFluidHandler(aBarrel, aSide);
	}

	// ---------------------------------------------------------------------------
	// acceptance ① — the drain side-rule table: side × density sign, the raw verdict form
	// ---------------------------------------------------------------------------

	@Test
	public void drainSideRuleTableSideBySign() {
		// -1 (the side-less query) is the all-open path, every sign passes (ruling ⑥)
		assertTrue(BarrelFluidHandler.drainAllowedBySide((byte) -1, -1), "null-side drains the lighter");
		assertTrue(BarrelFluidHandler.drainAllowedBySide((byte) -1, 0), "null-side drains a zero-sign content");
		assertTrue(BarrelFluidHandler.drainAllowedBySide((byte) -1, +1), "null-side drains the heavier");

		// bottom (0): only strictly heavier (density > 0 — the strict GT6 verdict, FL.java:780)
		assertTrue(BarrelFluidHandler.drainAllowedBySide((byte) 0, +1), "the bottom drains the heavier");
		assertFalse(BarrelFluidHandler.drainAllowedBySide((byte) 0, -1), "the bottom never drains the lighter");
		assertFalse(BarrelFluidHandler.drainAllowedBySide((byte) 0, 0), "density 0 passes neither face (no ≤0 reading)");

		// top (1): only strictly lighter (density < 0 — FL.java:775)
		assertTrue(BarrelFluidHandler.drainAllowedBySide((byte) 1, -1), "the top drains the lighter");
		assertFalse(BarrelFluidHandler.drainAllowedBySide((byte) 1, +1), "the top never drains the heavier");
		assertFalse(BarrelFluidHandler.drainAllowedBySide((byte) 1, 0), "density 0 passes neither face");

		// the four sides (2-5): take in, never give out
		for (byte tSide = 2; tSide <= 5; tSide++) {
			assertFalse(BarrelFluidHandler.drainAllowedBySide(tSide, -1), "side " + tSide + " refuses the lighter");
			assertFalse(BarrelFluidHandler.drainAllowedBySide(tSide, 0), "side " + tSide + " refuses a zero-sign content");
			assertFalse(BarrelFluidHandler.drainAllowedBySide(tSide, +1), "side " + tSide + " refuses the heavier");
		}
	}

	@Test
	public void drainSideRuleFluidStackFormShortCircuitsTheSidelessPath() {
		// the FluidStack overload answers the side-less query without touching the content —
		// null content passes (the offline-safe and /gt6tank-accept shape)
		assertTrue(BarrelFluidHandler.drainAllowedBySide((byte) -1, (FluidStack) null), "null-side + null content = all-open");

		// the gated faces refuse a null/empty content outright
		assertFalse(BarrelFluidHandler.drainAllowedBySide((byte) 0, (FluidStack) null), "no content, no bottom drain");
		assertFalse(BarrelFluidHandler.drainAllowedBySide((byte) 1, (FluidStack) null), "no content, no top drain");
	}

	// ---------------------------------------------------------------------------
	// acceptance ② — fill is face-open: every face and the null-side query take fluid in
	// ---------------------------------------------------------------------------

	@Test
	public void fillPassesThroughEveryFaceAndTheSidelessQuery() {
		GTBarrelBlockEntity tBarrel = barrel();
		int tTotal = 0;
		for (Direction tSide : Direction.values()) {
			int tFilled = handler(tBarrel, tSide).fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
			assertEquals(100, tFilled, "fill is face-open through " + tSide);
			tTotal += tFilled;
		}
		assertEquals(100, handler(tBarrel, null).fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE), "fill is open on the side-less query");
		assertEquals(700, tBarrel.mTank.amount(), "six faces + the null-side query landed everything in the one tank");
		assertEquals(600, tTotal);
	}

	// ---------------------------------------------------------------------------
	// acceptance ⑤ — the FL.move fill-then-drain pair: refusal costs the source nothing
	// ---------------------------------------------------------------------------

	/** A stub IFluidHandler: optional fill admission, a capacity, a real drain face. */
	static final class StubHandler implements IFluidHandler {
		private FluidStack mContent = null;
		private final long mCapacity;
		private final boolean mAccepts;

		StubHandler(long aCapacity, boolean aAccepts) {
			mCapacity = aCapacity;
			mAccepts = aAccepts;
		}

		long amount() {
			return mContent == null ? 0 : mContent.getAmount();
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int aTank) {
			return mContent == null ? FluidStack.EMPTY : mContent;
		}

		@Override
		public int getTankCapacity(int aTank) {
			return FluidTankGT.bindInt(mCapacity);
		}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {
			return aStack != null && !aStack.isEmpty();
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (!mAccepts || aResource == null || aResource.isEmpty()) return 0;
			if (mContent == null) {
				// the copy carries the FULL offered amount — cap it to what this stub takes
				long tAccepted = Math.min(aResource.getAmount(), mCapacity);
				if (aAction.execute() && tAccepted > 0) {
					mContent = aResource.copy();
					mContent.setAmount((int) tAccepted);
				}
				return (int) tAccepted;
			}
			if (!mContent.isFluidEqual(aResource)) return 0; // the single-fluid rule
			long tAccepted = Math.min(aResource.getAmount(), mCapacity - amount());
			if (aAction.execute() && tAccepted > 0) mContent.setAmount((int) (amount() + tAccepted));
			return (int) tAccepted;
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			if (mContent == null || aMaxDrain <= 0) return FluidStack.EMPTY;
			int tDrained = (int) Math.min(aMaxDrain, mContent.getAmount());
			FluidStack rStack = new FluidStack(mContent, tDrained);
			if (aAction.execute()) {
				mContent.setAmount(mContent.getAmount() - tDrained);
				if (mContent.getAmount() <= 0) mContent = null;
			}
			return rStack;
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty() || mContent == null || !mContent.isFluidEqual(aResource)) return FluidStack.EMPTY;
			return drain(aResource.getAmount(), aAction);
		}
	}

	@Test
	public void refusalMovesNothingAndCostsTheSourceNothing() {
		GTBarrelBlockEntity tBarrel = barrel();
		tBarrel.mTank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
		StubHandler tRefuser = new StubHandler(16000, false);

		assertEquals(0, TileEntityBase08Barrel.moveTankToHandler(tBarrel.mTank, tRefuser, TileEntityBase08Barrel.GRAVITY_TRANSFER_PER_TICK),
				"the refusing target moves 0 (upstream FL.java:846 — fill fails, nothing drains)");
		assertEquals(500, tBarrel.mTank.amount(), "the source paid nothing for the refusal (fill-then-drain)");
		assertTrue(tBarrel.mTank.contains(new FluidStack(Fluids.WATER, 1)), "no partial content churn");
	}

	@Test
	public void mixedFluidTargetRefusesAndTheSourceKeepsItsContent() {
		GTBarrelBlockEntity tBarrel = barrel();
		tBarrel.mTank.fill(new FluidStack(Fluids.LAVA, 500), FluidAction.EXECUTE);
		StubHandler tWaterHolder = new StubHandler(16000, true);
		tWaterHolder.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);

		assertEquals(0, TileEntityBase08Barrel.moveTankToHandler(tBarrel.mTank, tWaterHolder, 1000),
				"a single-fluid target holding another fluid accepts 0");
		assertEquals(500, tBarrel.mTank.amount(), "the lava stayed put");
	}

	@Test
	public void partialAcceptanceMovesOnlyWhatLanded() {
		GTBarrelBlockEntity tBarrel = barrel();
		tBarrel.mTank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
		StubHandler tSmall = new StubHandler(300, true);

		assertEquals(300, TileEntityBase08Barrel.moveTankToHandler(tBarrel.mTank, tSmall, 1000),
				"the move is the executed-fill amount, not the simulated-drain amount");
		assertEquals(200, tBarrel.mTank.amount(), "only what the target took leaves the source");
		assertEquals(300, tSmall.amount());
	}

	@Test
	public void budgetCapsTheMovePerCall() {
		GTBarrelBlockEntity tBarrel = barrel();
		tBarrel.mTank.fill(new FluidStack(Fluids.WATER, 5000), FluidAction.EXECUTE);
		StubHandler tSink = new StubHandler(16000, true);

		assertEquals(TileEntityBase08Barrel.GRAVITY_TRANSFER_PER_TICK,
				TileEntityBase08Barrel.moveTankToHandler(tBarrel.mTank, tSink, TileEntityBase08Barrel.GRAVITY_TRANSFER_PER_TICK),
				"one tick moves at most the 1000 L budget (ruling ②)");
		assertEquals(4000, tBarrel.mTank.amount(), "the rest of the content stays for the next ticks");
	}

	@Test
	public void fullDrainEmptiesTheSource() {
		GTBarrelBlockEntity tBarrel = barrel();
		tBarrel.mTank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
		StubHandler tSink = new StubHandler(16000, true);

		assertEquals(500, TileEntityBase08Barrel.moveTankToHandler(tBarrel.mTank, tSink, 1000));
		assertEquals(0, tBarrel.mTank.amount());
		assertNull(tBarrel.mTank.getFluid(), "the wood barrel drains to a true empty");
	}

	@Test
	public void reversedMoveCarriesHandlerToTankAndRespectsRefusal() {
		StubHandler tSource = new StubHandler(16000, true);
		tSource.fill(new FluidStack(Fluids.WATER, 800), FluidAction.EXECUTE);
		GTBarrelBlockEntity tBarrel = barrel();

		assertEquals(500, TileEntityBase08Barrel.moveHandlerToTank(tSource, tBarrel.mTank, 500),
				"the pump in-mode pulls the budget out of the neighbour");
		assertEquals(300, tSource.amount());
		assertEquals(500, tBarrel.mTank.amount());

		tBarrel.mTank.drain(500, FluidAction.EXECUTE); // empty the water (wood drains to a true empty)
		tBarrel.mTank.fill(new FluidStack(Fluids.LAVA, 15000), FluidAction.EXECUTE); // now the barrel holds lava
		assertEquals(0, TileEntityBase08Barrel.moveHandlerToTank(tSource, tBarrel.mTank, 100),
				"the tank's single-fluid rule refuses the water — 0 moves");
		assertEquals(300, tSource.amount(), "the neighbour kept its content");
	}

	// ---------------------------------------------------------------------------
	// acceptance ④ — the gravity branch table (lighter → UP, everything else → DOWN)
	// ---------------------------------------------------------------------------

	@Test
	public void gravityDirectionTable() {
		assertEquals(Direction.UP, TileEntityBase08Barrel.gravityDirection(-1), "the lighter rises (ruling ③)");
		assertEquals(Direction.DOWN, TileEntityBase08Barrel.gravityDirection(+1), "the heavier falls");
		assertEquals(Direction.DOWN, TileEntityBase08Barrel.gravityDirection(0), "density 0 falls with the upstream else catch-all");
	}

	// ---------------------------------------------------------------------------
	// the null-side wrapper path — all-open, offline-safe by construction
	// ---------------------------------------------------------------------------

	@Test
	public void sidelessWrapperDrainsWithoutSideRules() {
		GTBarrelBlockEntity tBarrel = barrel();
		tBarrel.mTank.fill(new FluidStack(Fluids.WATER, 1234), FluidAction.EXECUTE);

		FluidStack tDrawn = handler(tBarrel, null).drain(1000, FluidAction.EXECUTE);
		assertEquals(1000, tDrawn.getAmount(), "the null-side query is the all-open drain path");
		assertEquals(234, tBarrel.mTank.amount());

		assertEquals(234, handler(tBarrel, null).drain(1000, FluidAction.EXECUTE).getAmount(), "the rest drains too");
		assertEquals(0, tBarrel.mTank.amount());
		assertNull(tBarrel.mTank.getFluid());
	}

	@Test
	public void pumpSeamHandsOutTheTank() {
		GTBarrelBlockEntity tBarrel = barrel();
		assertTrue(tBarrel.getCoverPumpTank() == tBarrel.mTank, "the barrel overrides the pump seam with mTank (the direct-call exemption)");
	}
}
