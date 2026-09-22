package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.covers.covers.CoverDrain;
import gregtech6.covers.covers.CoverPressureValve;
import gregtech6.covers.covers.CoverVent;
import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;
import gregtech6.tileentity.tank.GTBarrelMetalBlockEntity;

/**
 * The fluid trio offline acceptance (task p34-covers-gameplay-10): the Vent intake
 * beat (the pump-seam gate + the declared-minimal air-carrier seam), the Drain
 * collection beats and the PressureValve threshold (the full-tank vent, the
 * gas-vs-liquid split, the endpoint disconnect). The host fixtures ride the pump-test
 * forms: the metal drum for the tank-seam covers, the pipe BE for the valve.
 */
public class CoverFluidTrioTest extends GTCoverTestBase {

	static final BlockPos PIPE_POS = new BlockPos(4, 4, 4);

	static BlockEntityType<GTBarrelMetalBlockEntity> sBarrelType;

	@SuppressWarnings("unchecked")
	@BeforeAll
	static void buildTrioFixtures() {
		// the base @BeforeAll already bootstrapped; the pump-test barrel fixture form
		SharedConstants.tryDetectVersion();
		BlockEntityType<GTBarrelMetalBlockEntity>[] tHolder = (BlockEntityType<GTBarrelMetalBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelMetalBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sBarrelType = tHolder[0];
		BlockEntityType<PipeCoverProbe>[] tPipeHolder = (BlockEntityType<PipeCoverProbe>[]) new BlockEntityType<?>[1];
		tPipeHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new PipeCoverProbe(tPipeHolder[0]),
				Blocks.BRICKS).build(null);
		sProbePipeType = tPipeHolder[0];
	}

	/** A leveled metal drum host with the cover mounted on face 3 through the real dispatch. */
	static GTBarrelMetalBlockEntity drumWith(ICover aCover) {
		GTBarrelMetalBlockEntity tDrum = sBarrelType.create(COVER_POS, Blocks.STONE.defaultBlockState());
		tDrum.setLevel(new MachineLevel(new TestRecipeManager()));
		CoverRegistry.put(Items.BRICK, aCover);
		assertTrue(tDrum.setCoverItem((byte) 3, new ItemStack(Items.BRICK), null, false, true),
				"the cover mounts on the metal drum (the mTank seam)");
		return tDrum;
	}

	/**
	 * The pipe carrier plus the cover store — the composition the declared
	 * host-composition card will land on the production carrier; the offline pins drive
	 * it directly (the valve behaviors cast the host to the pipe carrier).
	 */
	static class PipeCoverProbe extends GTFluidPipeBlockEntity implements ICoverableTE {

		/** The cover store (the composition contract field). */
		@javax.annotation.Nullable
		public CoverData mCovers;

		PipeCoverProbe(BlockEntityType<PipeCoverProbe> aType) {
			super(aType, PIPE_POS, Blocks.BRICKS.defaultBlockState());
		}

		@Override
		@javax.annotation.Nullable
		public CoverData getCovers() {
			return mCovers;
		}

		@Override
		public void setCovers(@javax.annotation.Nullable CoverData aCoverData) {
			mCovers = aCoverData;
		}
	}

	static BlockEntityType<PipeCoverProbe> sProbePipeType;

	static PipeCoverProbe pipeWith(FluidStack aContent) {
		PipeCoverProbe tPipe = new PipeCoverProbe(sProbePipeType);
		if (aContent != null && !aContent.isEmpty()) tPipe.mTanks[0].fill(aContent, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		return tPipe;
	}

	// ------------------------------------------------------------------
	// the vent (upstream CoverVent)
	// ------------------------------------------------------------------

	@Test
	public void ventRefusesHostsWithoutATank() {
		TileEntityOvenCoverProbe tOven = bareOven();
		tOven.setCovers(new CoverData(tOven));
		assertTrue(new CoverVent().interceptCoverPlacement((byte) 3, tOven.getCovers(), null),
				"the oven probe hands out no pump tank — refused (the pump-seam gate)");
	}

	@Test
	public void ventBeatTableMatchesTheUpstreamPhaseOffsets() {
		assertTrue(CoverVent.isVentBeat(390, (byte) 0), "390 % 360 == 30 + 60*0");
		assertFalse(CoverVent.isVentBeat(390, (byte) 3), "the side phases differ");
		assertTrue(CoverVent.isVentBeat(450, (byte) 1), "30 + 60*1 = 90, +360");
	}

	@Test
	public void ventBeatNoOpsWithoutTheAirCarrier() {
		GTBarrelMetalBlockEntity tDrum = drumWith(new CoverVent());
		assertNull(CoverVent.airStack(), "the gt6:air carrier is not registered (the census fluid gap)");
		tDrum.getCovers().tickPre(390, true, false, false); // the face-3 beat
		assertTrue(tDrum.getCoverPumpTank().isEmpty(), "no air carrier in the port — the declared-minimal no-op");
	}

	// ------------------------------------------------------------------
	// the drain (upstream CoverDrain)
	// ------------------------------------------------------------------

	@Test
	public void drainBeatTablesMatchTheUpstreamPhases() {
		assertTrue(CoverDrain.isRainBeat(110), "110 % 100 == 10");
		assertTrue(CoverDrain.isSweepBeat(25), "25 % 20 == 5");
		assertFalse(CoverDrain.isSweepBeat(26), "the sweep beat is 5 mod 20");
	}

	@Test
	public void drainSweepBeatNoOpsWithoutAWorldWaterSource() {
		GTBarrelMetalBlockEntity tDrum = drumWith(new CoverDrain());
		tDrum.getCovers().tickPre(25, true, false, false);
		assertTrue(tDrum.getCoverPumpTank().isEmpty(), "the stub level exposes no water source in front");
	}

	@Test
	public void drainRatesMatchTheUpstreamTable() {
		assertEquals(1000, CoverDrain.SOURCE_FILL, "the per-source rate (upstream :129)");
		assertEquals(16000, CoverDrain.INFINITE_WATER_FILL, "the infinite-pool bonus rate (upstream :127)");
	}

	@Test
	public void drainInfiniteWaterRuleIsTheSeaLevelRiverBand() {
		// upstream WD.java:690 verbatim shape — UT.Code.inside(waterLevel-15, waterLevel, y)
		// over the waterLevel table (WD.java:417-440): sky-lit 62, no-sky 31. The BIOMES_RIVER_LAKE
		// half rides the Level (the BiomeTags.IS_RIVER approximation, the method doc) — the
		// band + level formula are the offline-pinnable rule core.
		assertTrue(CoverDrain.inSeaLevelBand(62, CoverDrain.waterLevelOf(true)), "sea level itself is in the band");
		assertTrue(CoverDrain.inSeaLevelBand(47, CoverDrain.waterLevelOf(true)), "waterLevel-15 is the band floor");
		assertFalse(CoverDrain.inSeaLevelBand(46, CoverDrain.waterLevelOf(true)), "one below the floor is out — a sky-built pool above the band DRAINS the source");
		assertFalse(CoverDrain.inSeaLevelBand(63, CoverDrain.waterLevelOf(true)), "one above the ceiling is out");
		assertEquals(31, CoverDrain.waterLevelOf(false), "the no-sky water level (WD.java:433 hasNoSky arm)");
		assertTrue(CoverDrain.inSeaLevelBand(31, CoverDrain.waterLevelOf(false)), "the no-sky band pins its own level");
	}

	// ------------------------------------------------------------------
	// the pressure valve (upstream CoverPressureValve)
	// ------------------------------------------------------------------

	@Test
	public void pressureValveRefusesMultiTankPipesAndNonPipes() {
		CoverPressureValve tValve = new CoverPressureValve();
		PipeCoverProbe tPipe = pipeWith(null);
		CoverData tCovers = new CoverData(tPipe);
		assertFalse(tValve.interceptCoverPlacement((byte) 3, tCovers, null),
				"the single-tank pipe host admits the valve (upstream :44)");
		tPipe.mTanks = new FluidTankGT[] {new FluidTankGT(1000), new FluidTankGT(1000)};
		assertTrue(tValve.interceptCoverPlacement((byte) 3, tCovers, null),
				"a two-tank pipe is refused (upstream :44 mTanks.length != 1)");
	}

	@Test
	public void pressureValveGasTruthTable() {
		assertFalse(CoverPressureValve.isGas(new FluidStack(Fluids.WATER, 100)), "water is not a gas");
		assertFalse(CoverPressureValve.isGas(null), "the null stack is not a gas");
		assertFalse(CoverPressureValve.isGas(FluidStack.EMPTY), "the empty stack is not a gas");
	}

	@Test
	public void pressureValveFreshPipeGateKeepsTheTank() {
		CoverPressureValve tValve = new CoverPressureValve();
		PipeCoverProbe tPipe = pipeWith(new FluidStack(Fluids.WATER, 1000));
		CoverData tCovers = new CoverData(tPipe);
		tCovers.mBehaviours[3] = tValve;
		tCovers.tickPost(1, true, false, false);
		assertEquals(1000, tPipe.mTanks[0].amount(), "the fresh-pipe arm keeps the tank (aTimer > 2 gate)");
		assertFalse(tPipe.connected((byte) 3), "and no disconnect ran");
	}

	@Test
	public void pressureValveLiquidBacklogStaysWithoutAHandler() {
		CoverPressureValve tValve = new CoverPressureValve();
		PipeCoverProbe tPipe = pipeWith(new FluidStack(Fluids.WATER, 1000));
		CoverData tCovers = new CoverData(tPipe);
		tCovers.mBehaviours[3] = tValve;
		tCovers.tickPost(10, true, false, false);
		assertEquals(1000, tPipe.mTanks[0].amount(), "a liquid backlog with no handler and no vent keeps its tank");
		assertFalse(tPipe.connected((byte) 3), "the valve face stays an endpoint (the disconnect arm)");
	}
}
