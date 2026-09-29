
package gregtech6.tileentity.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The Measuring Pot BE offline tests (task r8-issue45-c3, issue #45) — the hitY
 * fill-limit band walk (upstream MultiTileEntityMeasuringPot.onBlockActivated3 :73-93,
 * the GT6 signature interaction) driven straight against the server-side chain: the
 * offline JVM has no level and {@code isServerSide()} answers true
 * (TileEntityBase01Root.java:188 — the GT6KitchenBlockEntityTest precedent), so the
 * bands, the bind clamp and the NBT_MODE persistence are asserted offline. The LIVE
 * server leg is not exercised (no RCON group on this card — the card face declares it).
 */
class GT6MeasuringPotBlockEntityTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	static BlockEntityType<GT6MeasuringPotBlockEntity> sType;

	@BeforeAll
	static void buildOfflineFixture() {
		// the offline holder-array form (the GT6KitchenBlockEntityTest.buildOfflineFixtures
		// shape) — the base @BeforeAll already booted vanilla AND reopened the BE-type
		// registry write window (the 21.1 FML boot freezes them, the unfreeze seam)
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6MeasuringPotBlockEntity>[] tHolder = (BlockEntityType<GT6MeasuringPotBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6MeasuringPotBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	private static GT6MeasuringPotBlockEntity pot() {
		return new GT6MeasuringPotBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the hitY band walk (upstream :76-88, coarse steps — the null player never sneaks)
	// ---------------------------------------------------------------------------

	/** Upper-upper band (hitY > 6/16): +50 (upstream :78) — from a lowered start, the ceiling test rides theBindClamps. */
	@Test
	void theTopBandRaisesFifty() {
		GT6MeasuringPotBlockEntity tPot = pot();
		tPot.mTank.setCapacity(500);
		assertEquals("Limit: 550L", tPot.limitChain(null, 7.0F / 16.0F), "the >6px band commits +50");
		assertEquals(550, tPot.mTank.capacity());
	}

	/** Upper-middle band (4/16 < hitY <= 6/16): +10 (upstream :80). */
	@Test
	void theUpperMiddleBandRaisesTen() {
		GT6MeasuringPotBlockEntity tPot = pot();
		tPot.mTank.setCapacity(500);
		tPot.limitChain(null, 5.0F / 16.0F);
		assertEquals(510, tPot.mTank.capacity(), "the 4-6px band commits +10");
	}

	/** Lower-middle band (2/16 < hitY <= 4/16): -10 (upstream :84). */
	@Test
	void theLowerMiddleBandLowersTen() {
		GT6MeasuringPotBlockEntity tPot = pot();
		tPot.limitChain(null, 3.0F / 16.0F);
		assertEquals(990, tPot.mTank.capacity(), "the 2-4px band commits -10");
	}

	/** Lower-lower band (hitY <= 2/16): -50 (upstream :86). */
	@Test
	void theBottomBandLowersFifty() {
		GT6MeasuringPotBlockEntity tPot = pot();
		tPot.limitChain(null, 1.0F / 16.0F);
		assertEquals(950, tPot.mTank.capacity(), "the <=2px band commits -50");
	}

	/** The exact band-boundary pixels ride the lower band (the upstream strict > comparisons: PX_P[4]/PX_P[6]/PX_P[2]). */
	@Test
	void theBoundaryPixelsStayStrict() {
		GT6MeasuringPotBlockEntity tPot = pot();
		tPot.mTank.setCapacity(100);
		tPot.limitChain(null, 6.0F / 16.0F);
		assertEquals(110, tPot.mTank.capacity(), "hitY == 6px is NOT > 6px — the +10 band (upstream :77)");
		tPot.limitChain(null, 4.0F / 16.0F);
		assertEquals(100, tPot.mTank.capacity(), "hitY == 4px is NOT > 4px — the -10 band (upstream :83)");
		tPot.limitChain(null, 2.0F / 16.0F);
		assertEquals(50, tPot.mTank.capacity(), "hitY == 2px is NOT > 2px — the -50 band (upstream :85)");
	}

	/** The bind clamp: [1, 1000] — the upstream UT.Code.bind(1, mCapacity, ...) against the ORIGINAL capacity (:78). */
	@Test
	void theBindClampsToOneThousandAndOne() {
		GT6MeasuringPotBlockEntity tPot = pot();
		tPot.mTank.setCapacity(995);
		tPot.limitChain(null, 7.0F / 16.0F);
		assertEquals(1000, tPot.mTank.capacity(), "the ceiling is the class default 1000, not 995+50");
		tPot.mTank.setCapacity(30);
		assertEquals("Limit: 1L", tPot.limitChain(null, 1.0F / 16.0F), "the floor is 1 (upstream bind(1, ...))");
		assertEquals(1, tPot.mTank.capacity());
	}

	/** The sneak pairs (upstream :78/:80/:84/:86 ternaries) — the static step face (a live sneaking Player is not constructible offline). */
	@Test
	void theSneakStepsDivideByTen() {
		assertEquals(GT6MeasuringPotBlockEntity.STEP_FINE, GT6MeasuringPotBlockEntity.step(true, GT6MeasuringPotBlockEntity.STEP_COARSE, GT6MeasuringPotBlockEntity.STEP_FINE), "sneaking swaps 50 -> 5");
		assertEquals(GT6MeasuringPotBlockEntity.STEP_FINEST, GT6MeasuringPotBlockEntity.step(true, GT6MeasuringPotBlockEntity.STEP_MIDDLE, GT6MeasuringPotBlockEntity.STEP_FINEST), "sneaking swaps 10 -> 1");
		assertEquals(GT6MeasuringPotBlockEntity.STEP_COARSE, GT6MeasuringPotBlockEntity.step(false, GT6MeasuringPotBlockEntity.STEP_COARSE, GT6MeasuringPotBlockEntity.STEP_FINE), "plain keeps 50");
		assertEquals(GT6MeasuringPotBlockEntity.STEP_MIDDLE, GT6MeasuringPotBlockEntity.step(false, GT6MeasuringPotBlockEntity.STEP_MIDDLE, GT6MeasuringPotBlockEntity.STEP_FINEST), "plain keeps 10");
	}

	// ---------------------------------------------------------------------------
	// NBT_MODE persistence (upstream writeToNBT2 :55-58 / readFromNBT2 :48-52)
	// ---------------------------------------------------------------------------

	/** The adjusted capacity rides NBT_MODE — written only off the default, restored on load, the content rides NBT_TANK. */
	@Test
	void theModeAndTankRoundTripThroughNbt() {
		GT6MeasuringPotBlockEntity tPot = pot();
		tPot.mTank.setCapacity(500);
		tPot.limitChain(null, 7.0F / 16.0F); // 500 -> 550 (the 1050-at-the-ceiling case clamps — the bind face)
		assertEquals(550, tPot.mTank.capacity());
		assertNotNull(tPot.mTank.fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE) > 0 ? tPot : null,
				"the water pre-fill lands");

		CompoundTag tNbt = new CompoundTag();
		tPot.saveAdditional(tNbt);
		assertEquals(550, tNbt.getLong(GT6MeasuringPotBlockEntity.NBT_MODE), "the off-default capacity rides NBT_MODE (:56)");
		assertTrue(tNbt.contains(GT6MeasuringPotBlockEntity.NBT_TANK), "the content rides NBT_TANK (:58)");

		GT6MeasuringPotBlockEntity tRestored = pot();
		tRestored.load(tNbt);
		assertEquals(550, tRestored.mTank.capacity(), "the limit survives the load (:51)");
		assertEquals(200, tRestored.mTank.amount(), "the content survives the load");
	}

	/** The class-default capacity writes NO NBT_MODE (the upstream :56 guard). */
	@Test
	void theDefaultCapacityWritesNoMode() {
		GT6MeasuringPotBlockEntity tPot = pot();
		CompoundTag tNbt = new CompoundTag();
		tPot.saveAdditional(tNbt);
		assertTrue(!tNbt.contains(GT6MeasuringPotBlockEntity.NBT_MODE), "the untouched pot carries no mode override");
	}

	// ---------------------------------------------------------------------------
	// the TapFillable + capability faces
	// ---------------------------------------------------------------------------

	/** The P12 TapFillable face — simulate probes, execute commits (the GT6KitchenBlockEntityTest.tapFillRidesTheFillAdmission shape). */
	@Test
	void tapFillFillsTheTank() {
		GT6MeasuringPotBlockEntity tPot = pot();
		FluidStack tWater = new FluidStack(Fluids.WATER, 400);
		assertEquals(400, tPot.tapFill((byte) 2, tWater, false), "the simulate phase accepts the offer");
		assertEquals(0, tPot.mTank.amount(), "…without committing");
		assertEquals(400, tPot.tapFill((byte) 2, tWater, true), "the execute phase commits");
		assertEquals(400, tPot.mTank.amount());
	}

	/** The single-tank IFluidHandler face — fill and drain straight (the FluidUtil container-click carrier). */
	@Test
	void theFluidHandlerDrainsBack() {
		GT6MeasuringPotBlockEntity tPot = pot();
		tPot.mTank.fill(new FluidStack(Fluids.WATER, 600), IFluidHandler.FluidAction.EXECUTE);
		IFluidHandler tHandler = tPot.newFluidHandler();
		assertEquals(1, tHandler.getTanks(), "one tank");
		assertEquals(1000, tHandler.getTankCapacity(0), "the capacity reads through (the class default)");
		assertEquals(600, tHandler.getFluidInTank(0).getAmount(), "the content reads through");
		assertEquals(600, tHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount(), "the drain walks the same tank");
		assertEquals(0, tPot.mTank.amount());
	}

	/** The limit adjust DOWNSIZES live: an over-limit fill refuses past the shrunken capacity (the measuring semantics). */
	@Test
	void theShrunkenLimitRefusesOverflow() {
		GT6MeasuringPotBlockEntity tPot = pot();
		tPot.limitChain(null, 1.0F / 16.0F);
		assertEquals(950, tPot.mTank.capacity(), "the limit walks to 950");
		assertEquals(950, tPot.mTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
				"the fill stops at the adjusted limit");
		assertEquals(0, tPot.mTank.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE),
				"the 951st litre refuses (the measuring-pot point)");
	}
}
