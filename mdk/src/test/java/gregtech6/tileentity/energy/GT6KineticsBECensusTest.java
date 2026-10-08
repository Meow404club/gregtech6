package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregapi.code.TagData;
import gregtech6.fluid.GTFluids;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GT6Kinetics;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The kinetics-BE functional census (task kinetics-be-function-family — the mc-D 遗留
 * B 案面): the rotation engine's BIPOLAR law (the ± pair out both axis poles, the
 * 16-tick oscillation, the V[t]/2 conversion), the SST's INVERSE law (the two-tick
 * whole-tank burn, the RU burst at the row output, the 蒸馏水回吐 at 1 L per
 * {@code STEAM_PER_WATER}), and the row-band shapes (readEnergyBehavior :73-78 over
 * the row values). The conversion fixtures ride the stone-state fallback rows (the
 * wooden V[0]=8→4 engine row, the Bronze 48/16 turbine row) — the offline forge leg
 * never binds the DeferredRegister, the row-adoption branch rides the registry-live
 * leg ({@code assumeTrue(registryLive())}, the GT6DatagenWalkLegTest form).
 */
public class GT6KineticsBECensusTest extends GTOfflineTestBase {

	private static final net.minecraft.core.BlockPos POS = new net.minecraft.core.BlockPos(2, 64, 2);

	private static BlockEntityType<GT6RotationEngineBlockEntity> sEngineType;
	private static BlockEntityType<GT6SteamTurbineBlockEntity> sTurbineType;

	/** True when this JVM actually registered the GT6 content (the neo junit-fml leg). */
	private static boolean registryLive() {
		return !GTMaterialItemsEmpty();
	}

	private static boolean GTMaterialItemsEmpty() {
		return gregtech6.registry.GTMaterialItems.items().isEmpty();
	}

	@BeforeAll
	static void boot() {
		// the conversion fixtures ride the stone-state fallback rows (hardcoded constants,
		// no MT field reads) — no hermetic material bracket needed (the census-table ruling)
		sEngineType = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6RotationEngineBlockEntity(sEngineType, aPos, aState), Blocks.STONE).build(null);
		sTurbineType = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6SteamTurbineBlockEntity(sTurbineType, aPos, aState), Blocks.STONE).build(null);
	}

	// -------------------------------------------------------------------
	// the instruments (the GearBoxTest CappedSink / adjacency seam form)
	// -------------------------------------------------------------------

	/** A level-less RU/KU sink bound to the vanilla stone state; records each accepted packet. */
	public static class Sink extends net.minecraft.world.level.block.entity.BlockEntity implements ITileEntityEnergy {
		public final List<Long> packets = new ArrayList<>();
		public final TagData type;

		static final BlockEntityType<Sink> FAKE_TYPE =
				BlockEntityType.Builder.of((aPos, aState) -> new Sink(aPos, TD.Energy.RU), Blocks.STONE).build(null);

		public Sink(net.minecraft.core.BlockPos aPos, TagData aType) {
			super(FAKE_TYPE, aPos, Blocks.STONE.defaultBlockState());
			type = aType;
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == type;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == type;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public java.util.Collection<TagData> getEnergyTypes(byte aSide) {return type.AS_LIST;}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) {
					packets.add(aSize * aAmount);
					return aAmount;
				}
			}
			return 0;
		}

		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return Long.MAX_VALUE;}

		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 1;}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 16;}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return Long.MAX_VALUE;}

		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	/** The one-neighbour adjacency: the BE's given side leads to the sink, everything else dead. */
	private static IEnergyAdjacency only(byte aSide, Sink aSink) {
		return aQuery -> {
			if (aQuery != aSide) return null;
			byte tOpposite = GT6KineticsBECensusTest.oppositeOf(aSide);
			return new EnergyTarget(aSink, tOpposite);
		};
	}

	private static byte oppositeOf(byte aSide) {
		return (byte) net.minecraft.core.Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
	}

	// -------------------------------------------------------------------
	// the rotation engine's bipolar law
	// -------------------------------------------------------------------

	/** The bands (readEnergyBehavior :73-78 over the wooden V[0] row): 8→4, the axis faces. */
	@Test
	public void theRotationEngineRidesTheWoodRowBands() {
		GT6RotationEngineBlockEntity tEngine = new GT6RotationEngineBlockEntity(sEngineType, POS, Blocks.STONE.defaultBlockState());
		assertEquals(8, tEngine.inputSpeed(), "the wooden V[0] input (the stone-state fallback)");
		assertEquals(4, tEngine.outputSpeed(), "the V[0]/2 output");
		assertEquals(1, tEngine.inputSizeMin(), "tInput 8 ≤ 16 — the takesAnyLowerSize arm");
		assertEquals(16, tEngine.inputSizeMax(), "tInput × 2");
		assertEquals(2, tEngine.outputSizeMin(), "tOutput / 2");
		assertEquals(16, tEngine.storageCapacity(), "tInput × 2 capacitor");
		assertTrue(tEngine.isInput((byte) 0) && tEngine.isInput((byte) 1), "the UP/DOWN sides are inputs (off-axis)");
		assertTrue(tEngine.isInput((byte) 4) && tEngine.isInput((byte) 5), "the lateral sides are inputs");
		assertTrue(tEngine.isOutput((byte) 2) && tEngine.isOutput((byte) 3), "front+back = the bipolar poles");
		assertTrue(tEngine.wasteEnergy(), "the row's NBT_WASTE_ENERGY = T");
		assertEquals(TD.Energy.RU, tEngine.energyTypeIn());
		assertEquals(TD.Energy.KU, tEngine.energyTypeOut());
	}

	/** The bipolar conversion: ±tOutput out BOTH poles in one tick, the 16-tick sign flip, the funnel waste. */
	@Test
	public void theBipolarLawDrivesBothPolesWithOppositeSigns() {
		GT6RotationEngineBlockEntity tEngine = new GT6RotationEngineBlockEntity(sEngineType, POS, Blocks.STONE.defaultBlockState());
		Sink tFront = new Sink(POS.offset(net.minecraft.core.Direction.NORTH.getNormal()), TD.Energy.KU);
		Sink tBack = new Sink(POS.offset(net.minecraft.core.Direction.SOUTH.getNormal()), TD.Energy.KU);
		tEngine.setAdjacencyOverride(aQuery -> {
			// the poles: side 2 (NORTH) → tFront, side 3 (SOUTH) → tBack; the input sides dead
			if (aQuery == 2) return new EnergyTarget(tFront, (byte) 3);
			if (aQuery == 3) return new EnergyTarget(tBack, (byte) 2);
			return null;
		});
		// feed the capacitor to one output batch: an 8×1 RU packet = 8 stored → tOutput = 8×4/8 = 4
		tEngine.doInject(TD.Energy.RU, (byte) 0, 8, 1, true);
		assertEquals(8, tEngine.mStorage, "the capacitor holds the packet");
		// timer 0 = the first half — BOTH poles fire in the same tick: +4 out the BACK
		// (OPOS = the sidePos) and -4 out the FRONT (the sideNeg), the doBipolar pair
		tEngine.onTick(0, true);
		assertEquals(1, tBack.packets.size(), "the back pole took +4");
		assertEquals(1, tFront.packets.size(), "the front pole took -4");
		assertEquals(4L, tBack.packets.get(0), "timer 0: +4 back (the sidePos)");
		assertEquals(-4L, tFront.packets.get(0), "timer 0: -4 front (the sideNeg)");
		// the funnel: the second half of the storage vents (waste leg 8/tick)
		assertEquals(0, tEngine.mStorage, "the waste leg vents the remainder");
		// timer 16 = the flip — the FRONT pole now gets +4
		tEngine.doInject(TD.Energy.RU, (byte) 0, 16, 1, true);
		tEngine.onTick(16, true);
		assertEquals(8L, tFront.packets.get(tFront.packets.size() - 1), "timer 16: +8 front (the oscillation flip) — front=" + tFront.packets + " back=" + tBack.packets + " storage=" + tEngine.mStorage);
	}

	// -------------------------------------------------------------------
	// the SST's inverse law
	// -------------------------------------------------------------------

	/** The Bronze-row bands (48 SU → 16 RU) + the faces (steam back, RU front). */
	@Test
	public void theSteamTurbineRidesTheBronzeRowBands() {
		GT6SteamTurbineBlockEntity tTurbine = new GT6SteamTurbineBlockEntity(sTurbineType, POS, Blocks.STONE.defaultBlockState());
		assertEquals(48, tTurbine.inputSU(), "Bronze 24*STEAM_PER_EU (the stone-state fallback)");
		assertEquals(16, tTurbine.outputRU(), "the Bronze NBT_OUTPUT");
		assertEquals(24, tTurbine.inputSizeMin(), "48 > 16 — the tInput/2 arm");
		assertEquals(384, tTurbine.mTank.getCapacity(), "the input max × 4 tank (readFromNBT2 :59)");
		assertTrue(tTurbine.isInput((byte) 3) && !tTurbine.isInput((byte) 2), "the steam door = the BACK");
		assertTrue(tTurbine.isOutput((byte) 2) && !tTurbine.isOutput((byte) 3), "the RU emission = the FRONT");
		assertEquals(TD.Energy.STEAM, tTurbine.energyTypeIn());
		assertEquals(TD.Energy.RU, tTurbine.energyTypeOut());
	}

	/** The two-tick whole-tank burn → the RU burst at the row output + the 蒸馏水回吐 ledger. */
	@Test
	public void theInverseLawBurnsSteamIntoRUAndReturnsDistilledWater() {
		GT6SteamTurbineBlockEntity tTurbine = new GT6SteamTurbineBlockEntity(sTurbineType, POS, Blocks.STONE.defaultBlockState());
		Sink tFront = new Sink(POS.offset(net.minecraft.core.Direction.NORTH.getNormal()), TD.Energy.RU);
		tEngineAdjacency(tTurbine, tFront);
		// the offline fluid seam: the gt6 steam RegistryObject never binds on the bare JVM,
		// so the fixture rides the mSteamMatch/mByproductMake seams (the engine BE form)
		// over a vanilla fluid
		tTurbine.mSteamMatch = f -> f == net.minecraft.world.level.material.Fluids.WATER;
		tTurbine.mByproductMake = amount -> new FluidStack(net.minecraft.world.level.material.Fluids.WATER, amount);
		IFluidHandler tDoor = tTurbine.mFluidHandler;
		FluidStack tSteam = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 384); // the tank cap = 48×2×4
		assertTrue(tDoor.isFluidValid(0, tSteam), "steam is valid tank content");
		assertEquals(384, tDoor.fill(tSteam, IFluidHandler.FluidAction.EXECUTE), "the tank takes the steam to its cap");
		assertEquals(0, tDoor.fill(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 100),
				IFluidHandler.FluidAction.EXECUTE), "lava is NOT tank content (the steam-only gate)");
		// tick 1: the whole-tank burn — 384 SU → 192 EU into the capacitor, 192 banked;
		// THEN the plain converter fires in the SAME tick (upstream :108 super.doConversion):
		// tOutput = units(192, 48, 16) = 64 → ONE 64-RU burst out the front; the drain books
		// 64 back and the waste leg vents inputSizeMax = 96: 192 - 64 - 96 = 32
		tTurbine.onTick(0, true);
		assertEquals(192, tTurbine.mEnergyProducedNextTick, "the banked second half");
		assertEquals(0, tTurbine.mTank.amount(), "the tank drained whole (upstream :97)");
		assertEquals(1, tTurbine.mDistWaterPending, "384 SU / STEAM_PER_WATER(200) = 1 L dist-w");
		assertEquals(java.util.List.of(64L), tFront.packets, "the same-tick 64-RU burst");
		assertEquals(32, tTurbine.mStorage, "192 - 64 emitted - 96 vented");
		// tick 2: the banked half lands (224 in the capacitor) → tOutput = units(224,48,16) = 74
		tTurbine.onTick(1, true);
		assertTrue(tTurbine.mActive, "the RU burst emitted");
		assertEquals(74L, tTurbine.mLastOutSize, "the second-tick burst: units(224, 48, 16) = 74");
		assertEquals(java.util.List.of(64L, 74L), tFront.packets, "the two-tick burst train");
		assertEquals(224 - 74 - 96, tTurbine.mStorage, "224 - 74 emitted - 96 vented");
	}

	/** The fill door wiring: only the BACK face exposes the fluid capability (the :239 gate). */
	@Test
	public void theSteamDoorIsOnlyTheBackFace() {
		// the :239 gate rides fluidDoorOpen (the capability initializer needs the forge
		// registries — the offline JVM cannot bootstrap ForgeCapabilities, the door helper
		// is the same predicate getCapability consults)
		GT6SteamTurbineBlockEntity tTurbine = new GT6SteamTurbineBlockEntity(sTurbineType, POS, Blocks.STONE.defaultBlockState());
		assertTrue(tTurbine.fluidDoorOpen((byte) 3), "the back face exposes the fill door");
		assertTrue(!tTurbine.fluidDoorOpen((byte) 2), "the front face does not");
		assertTrue(!tTurbine.fluidDoorOpen((byte) 0), "the up face does not");
	}

	// -------------------------------------------------------------------
	// the row adoption (the registry-live leg)
	// -------------------------------------------------------------------

	/** The metal transformer-gearbox rows carry their V[t]→V[t-1] pair (the registry-live leg). */
	@Test
	public void theTransformerRowsAdoptTheirPairs() {
		org.junit.jupiter.api.Assumptions.assumeTrue(registryLive());
		GT6Kinetics.TransformerGearboxRow tBronze = GT6Kinetics.TRANSFORMER_GEARBOXES.get(0);
		var tBlock = GT6Kinetics.TRANSFORMER_GEARBOX_BLOCKS.get(tBronze.path()).get();
		GTTransformerRotationBlockEntity tBE = new GTTransformerRotationBlockEntity(
				POS, tBlock.defaultBlockState().setValue(GT6Kinetics.PowertrainBlock.FACING, net.minecraft.core.Direction.NORTH));
		assertEquals(32, tBE.inputSpeed(), "the Bronze row V[1] (the adoption, not the wooden 8)");
		assertEquals(8, tBE.outputSpeed(), "the Bronze row V[0]");
		assertEquals(64, tBE.storageCapacity(), "tInput × 2 over the adopted row");
	}

	/** The custom gearbox rows carry their VMAX[tier] rating (the registry-live leg). */
	@Test
	public void theCustomGearboxRowsAdoptTheirVmax() {
		org.junit.jupiter.api.Assumptions.assumeTrue(registryLive());
		GT6Kinetics.CustomGearboxRow tSteel = GT6Kinetics.CUSTOM_GEARBOXES.get(4);
		var tBlock = GT6Kinetics.CUSTOM_GEARBOX_BLOCKS.get(tSteel.path()).get();
		GTGearBoxBlockEntity tBE = new GTGearBoxBlockEntity(POS, tBlock.defaultBlockState());
		assertEquals(tSteel.maxThroughput(), tBE.maxThroughput(), "the VMAX[tier] adoption");
		assertEquals(tSteel.maxThroughput(), tBE.getEnergySizeInputMax(TD.Energy.RU, (byte) 2), "the input band rides the adopted rating");
	}

	/** The SST adjacency seam helper (the engine BE form). */
	private void tEngineAdjacency(GT6SteamTurbineBlockEntity aTurbine, Sink aFront) {
		aTurbine.setAdjacencyOverride(aQuery -> {
			if (aQuery == 2) return new EnergyTarget(aFront, (byte) 3);
			return null;
		});
	}
}
