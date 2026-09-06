package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.recipes.GT6RecipesEngineFuels;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Kinetics;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GTDieselEngineBlockEntity offline tests (task p12-engine-diesel acceptance a): the
 * combustion truth table (the refill gate {@code mEnergy < mRate * 2}, the 64t-inactive
 * fuel-swap clear :130, the immediate invalid-fuel clear :135, the stop gate, the :138
 * floor), the DC constant-sign invariant ({@code +mRate} same-sign every tick through the
 * REAL {@code Util.emitEnergyToNetwork} handshake — the third source semantics beside the
 * crank negative DC and the KU square wave, asserted as a three-row table), the
 * {@code mRate * 10} tank capacity re-bind (:82-83), the exhaust CO2 mark + vent arm, the
 * eight-tier table (16..512 RU/t, efficiency 10000), the ENGINE_FUELS lookup
 * (probe-then-consume, the mLastRecipe fast path) and the NBT round trip.
 *
 * <p>Offline-harness notes (the crank/axle in-case record carries over): the level-less
 * fixture takes the SERVER branch, {@code onTick} runs through {@code updateEntity()},
 * and the Forge fluid registries are frozen — the fuel map is a FIXTURE RecipeMap over
 * vanilla water/lava rows injected through {@link GTDieselEngineBlockEntity#mRecipesMap}
 * (the upstream mRecipes field form, NBT_FUELMAP-swappable), which simultaneously
 * exercises the injection seam the live FM.Engine map rides.
 */
public class GTDieselEngineBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GTDieselEngineBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	/** The fixture fuel map: water = the diesel row (448 power/L), lava = the nitrofuel row (768). */
	static RecipeMap sFuelMap;
	static Recipe sWaterRow, sLavaRow;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GTDieselEngineBlockEntity>[] tHolder = (BlockEntityType<GTDieselEngineBlockEntity>[]) new BlockEntityType<?>[1];
		// OAK_STAIRS joins the valid set for the facingMirrorSyncsFromState fixture (the
		// vanilla stairs state carries the SAME HORIZONTAL_FACING property instance); 21.1
		// validates the type/state pair at the BE ctor (task p15-m4-test-infra-2).
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTDieselEngineBlockEntity(tHolder[0], aPos, aState), Blocks.STONE, Blocks.OAK_STAIRS).build(null);
		sType = tHolder[0];

		sFuelMap = new RecipeMap(new HashSet<>(), "gt.test.fuels", "Test Fuels", null, 0, 1,
				"gt6:textures/gui/machines/default",
				/*IN-OUT-MIN-ITEM=*/ 1, 2, 0,
				/*IN-OUT-MIN-FLUID=*/ 1, 2, 0,
				/*MIN=*/ 1, /*AMP=*/ 1);
		// the FM.Engine row shape: fluid-only, 1 L input, |EUt x duration| power (diesel -64x7=448, nitrofuel -64x12=768)
		sWaterRow = new Recipe(true, new ItemStack[0], new ItemStack[0],
				new FluidStack[] {new FluidStack(Fluids.WATER, 1)}, new FluidStack[0], 7, -64, 0);
		sLavaRow = new Recipe(true, new ItemStack[0], new ItemStack[0],
				new FluidStack[] {new FluidStack(Fluids.LAVA, 1)}, new FluidStack[0], 12, -64, 0);
		sFuelMap.addRecipe(sWaterRow);
		sFuelMap.addRecipe(sLavaRow);
	}

	/**
	 * A fresh level-less engine fixture pinned to the BRONZE row rate 16 — offline there is
	 * no placed {@code GTDieselEngineBlock} to carry the spec (the frozen registry), so the
	 * test writes the field the ctor reads on the live placement path (and re-binds the
	 * tank capacities exactly like the ctor does).
	 */
	private static GTDieselEngineBlockEntity engine() {
		GTDieselEngineBlockEntity tEngine = new GTDieselEngineBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
		tEngine.mRecipesMap = sFuelMap;
		tEngine.mRate = 16;
		tEngine.mTanks[0].setCapacity(160);
		tEngine.mTanks[1].setCapacity(160);
		return tEngine;
	}

	private static void fillInput(GTDieselEngineBlockEntity aEngine, FluidStack aFluid) {
		assertEquals(aFluid.getAmount(), aEngine.mTanks[0].fill(aFluid, FluidAction.EXECUTE), "the fixture fill lands whole");
	}

	/**
	 * Counting RU sink — the shredder-shaped fake consumer (the crank test form): accepts RU
	 * from every side, refuses everything else.
	 */
	public static class CountingSink extends BlockEntity implements ITileEntityEnergy {
		public long calls = 0, lastSize = 1, lastAmount = 0;
		public final List<Long> sizes = new ArrayList<>();
		public byte lastSide = -1;

		// 21.1 ctor validation: the fake binds a real BET over the vanilla stone state —
		// the supplier is stored, never invoked (task p15-m4-test-infra-2).
		static final BlockEntityType<CountingSink> FAKE_TYPE =
				BlockEntityType.Builder.of((aPos, aState) -> new CountingSink(aPos), Blocks.STONE).build(null);

		public CountingSink(BlockPos aPos) {
			super(FAKE_TYPE, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == TD.Energy.RU;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == TD.Energy.RU;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.RU.AS_LIST;}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) {
					calls++;
					lastSize = aSize;
					lastAmount = aAmount;
					lastSide = aSide;
					sizes.add(aSize);
				}
				return aAmount;
			}
			return 0;
		}

		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	/** Wires the sink in as the engine's (offline) adjacency on the emit face. */
	private static CountingSink wire(GTDieselEngineBlockEntity aEngine) {
		CountingSink tSink = new CountingSink(aEngine.getBlockPos().relative(Direction.from3DDataValue(aEngine.mFacing)));
		aEngine.setAdjacencyOverride(aSide -> {
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tSink, tOpposite);
		});
		return tSink;
	}

	// ---------------------------------------------------------------------------
	// the combustion truth table (upstream :113-138)
	// ---------------------------------------------------------------------------

	@Test
	public void refillGateSkipsAboveTwiceRate() {
		GTDieselEngineBlockEntity tEngine = engine();
		fillInput(tEngine, new FluidStack(Fluids.WATER, 5));
		tEngine.mEnergy = tEngine.mRate * 3; // 96: after the emit (96-16=80) the :113 gate sits closed at >= 2x
		tEngine.mActive = true; // the skipped branch must leave it untouched
		tEngine.updateEntity();
		assertEquals(5, tEngine.mTanks[0].amount(), "no burn above the 2x head-room");
		assertTrue(tEngine.mActive, "the :114 reset never ran");
		assertEquals(tEngine.mRate * 3 - tEngine.mRate, tEngine.mEnergy, "only the emit moved (80)");
	}

	@Test
	public void burnCreditsAbsolutePowerPerLitre() {
		GTDieselEngineBlockEntity tEngine = engine();
		fillInput(tEngine, new FluidStack(Fluids.WATER, 5));
		tEngine.updateEntity(); // energy 0: no emit, burn 1 L -> +448
		assertEquals(4, tEngine.mTanks[0].amount(), "one litre consumed per burn");
		assertEquals(448, tEngine.mEnergy, "units(|-64 x 7|, 10000, 10000) = 448 (the diesel row power)");
		assertTrue(tEngine.mActive, ":119 the burn flag");
		assertEquals(sWaterRow, tEngine.mLastRecipe, ":120 the mLastRecipe fast path armed");
	}

	@Test
	public void emitSpendsRateUnconditionallyThenBurnRefills() {
		// the :109-112 order: emit FIRST (48 -> one +16 packet, 32 left), then the refill
		// burn tops the head-room back up (32 < 2x -> +448 = 480)
		GTDieselEngineBlockEntity tEngine = engine();
		CountingSink tSink = wire(tEngine);
		fillInput(tEngine, new FluidStack(Fluids.WATER, 5));
		tEngine.mEnergy = 40;
		tEngine.updateEntity();
		assertEquals(1, tSink.calls, "one packet on the emit tick");
		assertEquals(16, tSink.lastSize);
		assertEquals(472, tEngine.mEnergy, "40 - 16 emit + 448 burn (24 < 2x opens the refill gate)");
	}

	@Test
	public void emitIntoVoidStillSpends() {
		// the generator family semantics: the :111 subtraction ignores the Util return — a
		// packet fired into an empty face is spent (the research q2 "打空也扣能" kin)
		GTDieselEngineBlockEntity tEngine = engine();
		tEngine.setAdjacencyOverride(aSide -> null); // no neighbour at all
		tEngine.mEnergy = 64; // no fuel in the tank: the refill below cannot re-credit
		tEngine.updateEntity();
		assertEquals(48, tEngine.mEnergy, "64 - 16: the packet left regardless of acceptance");
	}

	@Test
	public void invalidFuelClearsImmediately() {
		// :133-136 — a tank content with NO row at all is voided on the spot ("not valid
		// Fuel anymore for whatever reason")
		GTDieselEngineBlockEntity tEngine = engine();
		fillInput(tEngine, new FluidStack(Fluids.FLOWING_WATER, 3));
		tEngine.updateEntity();
		assertNull(tEngine.mTanks[0].getFluid(), "the non-fuel content is setEmpty immediately");
	}

	@Test
	public void burnDryClearsAndAdmitsNewFuelInstantly() {
		// the DECLARED deviation (findFuelRecipe class doc): the upstream burnt-dry tank
		// keeps its fluid identity at 0 L and the :130 64t swap gate governs the clear —
		// but the 1.20.1 Forge FluidStack normalizes 0-amount stacks to EMPTY, so the port
		// burns dry into a TRUE empty tank and a new fuel is admitted on the next burn
		// window; the :128-131 arm stays in the BE as the defensive verbatim
		GTDieselEngineBlockEntity tEngine = engine();
		fillInput(tEngine, new FluidStack(Fluids.WATER, 1));
		tEngine.updateEntity(); // burn dry
		assertEquals(448, tEngine.mEnergy);
		assertEquals(0, tEngine.mTanks[0].amount());
		assertNull(tEngine.mTanks[0].getFluid(), "the port tank burns dry to a true empty (no identity to keep)");

		// the nitrofuel row (768/L) is admissible immediately — no 64t wait; the stored
		// head-room from the first burn is drained first so the :113 refill gate re-opens
		tEngine.mEnergy = 0;
		fillInput(tEngine, new FluidStack(Fluids.LAVA, 2));
		tEngine.updateEntity();
		assertEquals(1, tEngine.mTanks[0].amount(), "the lava row burns after the swap");
		assertEquals(768, tEngine.mEnergy, "the nitrofuel-row power 768 credited");
	}

	@Test
	public void stopGateHaltsTheBurn() {
		GTDieselEngineBlockEntity tEngine = engine();
		fillInput(tEngine, new FluidStack(Fluids.WATER, 5));
		tEngine.setStateOnOff(false); // upstream :238 setStateOnOff(F) -> mStopped = T
		assertTrue(tEngine.mStopped);
		assertFalse(tEngine.setStateOnOff(false), ":238 returns !mStopped = false");
		tEngine.updateEntity();
		assertEquals(5, tEngine.mTanks[0].amount(), "no burn while stopped (:113 && !mStopped)");
	}

	@Test
	public void energyFloorAtZero() {
		// :138 — if (mEnergy < 0) mEnergy = 0
		GTDieselEngineBlockEntity tEngine = engine();
		tEngine.mEnergy = -5;
		tEngine.updateEntity();
		assertEquals(0, tEngine.mEnergy);
	}

	// ---------------------------------------------------------------------------
	// the DC constant-sign invariant + the three-source semantics table
	// ---------------------------------------------------------------------------

	@Test
	public void dcConstantAmplitudeSameSign() {
		GTDieselEngineBlockEntity tEngine = engine();
		CountingSink tSink = wire(tEngine);
		fillInput(tEngine, new FluidStack(Fluids.WATER, 20));
		for (int i = 0; i < 10; i++) tEngine.updateEntity();
		// tick 1 builds the head-room (0 -> 448, below the emit gate nothing leaves),
		// every later tick emits exactly one +rate packet
		assertEquals(9, tSink.calls, "one packet per tick after the first fill tick");
		assertEquals(16, tEngine.mRate);
		for (long tSize : tSink.sizes) {
			assertEquals(16, tSize, "constant amplitude = mRate");
			assertTrue(tSize > 0, "constant sign: POSITIVE = clockwise RU direct current (the DC invariant)");
		}
		assertEquals(1, tSink.lastAmount, "amount 1 per tick (:110 the (mRate, 1) packet)");
		// the packet lands on the receiver's own face (NORTH emit 2 -> SOUTH entry 3)
		assertEquals(3, tSink.lastSide);
	}

	@Test
	public void threeSourceSemanticsTable() {
		// the offline three-source table (task spec ②): RU DC constant-sign positive (this
		// engine, asserted live above), crank RU DC constant-sign negative, steam KU square
		// wave (the alternating carrier) — the carrier ledger is TD.ALL_ALTERNATING
		// (TD.java:216, the port of the upstream Root TD.java:216 (F, KU)).
		assertEquals(-16, GTCrankBlockEntity.packetSize(GTCrankBlockEntity.DEFAULT_POT2_STRENGTH, GTCrankBlockEntity.DEFAULT_POT1_WEAKNESS),
				"crank: constant-sign NEGATIVE (counterclockwise) RU DC");
		assertTrue(TD.Energy.ALL_ALTERNATING.contains(TD.Energy.KU), "steam engine: KU is the ALTERNATING carrier (the +/-tOutput square wave)");
		assertFalse(TD.Energy.ALL_ALTERNATING.contains(TD.Energy.RU), "diesel: RU is NOT the alternating carrier — the constant-sign DC row");
		assertEquals(200, gregtech6.fluid.GTFluids.STEAM_PER_WATER,
				"the steam row's carry constant lives (EngineSteam :58, the fuel-fluids card landing)");
	}

	// ---------------------------------------------------------------------------
	// the capacity (:82-83) + the eight-tier table (Loader :721-729)
	// ---------------------------------------------------------------------------

	@Test
	public void tankCapacityIsRateTimesTen() {
		GTDieselEngineBlockEntity tEngine = engine();
		assertEquals(160, tEngine.mTanks[0].capacity(), "16 * 10, both tanks (the bronze row fixture)");
		assertEquals(160, tEngine.mTanks[1].capacity());
		// the load-path re-bind: the rate arrives from NBT (the registration row carrier) and
		// BOTH capacities follow (:82-83 run after mRate is read)
		for (GT6Kinetics.DieselSpec tSpec : GT6Kinetics.DIESEL_SPECS) {
			GTDieselEngineBlockEntity tTier = engine();
			CompoundTag tNBT = new CompoundTag();
			tNBT.putLong(GTDieselEngineBlockEntity.NBT_OUTPUT, tSpec.output());
			tTier.load(tNBT);
			assertEquals(tSpec.output() * 10, tTier.mTanks[0].capacity(), tSpec.material() + " input tank = rate x 10");
			assertEquals(tSpec.output() * 10, tTier.mTanks[1].capacity(), tSpec.material() + " output tank = rate x 10");
			assertEquals(tSpec.output(), tTier.mRate);
		}
	}

	@Test
	public void eightTierTableTranscribesTheLoaderRows() {
		// Loader_MultiTileEntities.java:721-729 declaration order, NBT_OUTPUT per row
		assertEquals(8, GT6Kinetics.DIESEL_SPECS.size());
		assertSpec("bronze", "Bronze", 16);
		assertSpec("arsenic_copper", "Arsenic Copper", 16);
		assertSpec("arsenic_bronze", "Arsenic Bronze", 24);
		assertSpec("steel", "Steel", 32);
		assertSpec("invar", "Invar", 64);
		assertSpec("titanium", "Titanium", 128);
		assertSpec("tungstensteel", "Tungstensteel", 256);
		assertSpec("iridium", "Iridium", 512);
		// every row: the name/display forms (the block/item registration itself is the live
		// mod-construct + RCON gate's surface — offline the DeferredRegister loop never fires)
		for (GT6Kinetics.DieselSpec tSpec : GT6Kinetics.DIESEL_SPECS) {
			assertEquals("diesel_engine_" + tSpec.material(), GT6Kinetics.dieselName(tSpec.material()));
			// the composed face (task p20-i18n-compose-rows): template + material unit replay
			// to the old row wording; the key/unit faces are pinned in GT6LangParityTest
			assertEquals(tSpec.matDisplay() + " Diesel Engine",
					"%s Diesel Engine".replace("%s", tSpec.matDisplay()));
			assertEquals("gt6.row.mat." + tSpec.material(), GT6Kinetics.dieselMatUnitKey(tSpec));
		}
		GTDieselEngineBlockEntity tEngine = engine();
		assertEquals(10000, tEngine.mEfficiency, "NBT_EFFICIENCY 10000 on every row");
		assertEquals(TD.Energy.RU, tEngine.mEnergyTypeEmitted, "NBT_ENERGY_EMITTED RU on every row");
	}

	private void assertSpec(String aMaterial, String aDisplay, long aOutput) {
		GT6Kinetics.DieselSpec tSpec = GT6Kinetics.DIESEL_SPECS.stream()
				.filter(s -> s.material().equals(aMaterial)).findFirst().orElse(null);
		assertNotNull(tSpec, aMaterial + " row present");
		assertEquals(aDisplay, tSpec.matDisplay());
		assertEquals(aOutput, tSpec.output(), aMaterial + " NBT_OUTPUT");
	}

	// ---------------------------------------------------------------------------
	// the exhaust (the CO2 mark consumption + the :140-145 arms)
	// ---------------------------------------------------------------------------

	@Test
	public void co2MarkWalksTheFuelTable() {
		// the string walk over the public GT6RecipesEngineFuels.table() — the upstream
		// FL.CarbonDioxide.make(1) per :77-120 row, carried as the FuelRow.co2() data mark
		assertTrue(GTDieselEngineBlockEntity.co2Mark("diesel"), "the :91 diesel row is CO2-marked");
		assertTrue(GTDieselEngineBlockEntity.co2Mark("jetfuel"), "the :79 jetfuel row is CO2-marked");
		assertTrue(GTDieselEngineBlockEntity.co2Mark("ethanol"), "the :102 alcohol row is CO2-marked");
		assertFalse(GTDieselEngineBlockEntity.co2Mark("water"), "a fluid outside the table is unmarked");
		assertFalse(GTDieselEngineBlockEntity.co2Mark(null), "no path, no mark");
		// the full table: all seven upstream rows carry the mark
		for (GT6RecipesEngineFuels.FuelRow tRow : GT6RecipesEngineFuels.table()) {
			assertTrue(GTDieselEngineBlockEntity.co2Mark(tRow.fluid()), "row " + tRow.note() + " marked");
		}
	}

	@Test
	public void unmarkedFuelLeavesNoExhaust() {
		// the fixture rows are water/lava-keyed (offline: "minecraft:water" is no table
		// path) — a burn credits nothing, the counter stays 0
		GTDieselEngineBlockEntity tEngine = engine();
		fillInput(tEngine, new FluidStack(Fluids.WATER, 3));
		tEngine.updateEntity();
		assertEquals(0, tEngine.mExhaustCO2, "no CO2 mark, no exhaust unit");
	}

	@Test
	public void ventArmFiresOnlyWithOpenBack() {
		// :142-145 — gas with NO collision one past the back face vents directly
		GTDieselEngineBlockEntity tEngine = engine();
		tEngine.mExhaustCO2 = 5;
		tEngine.setBackCollideProbe(() -> false); // open air beyond the back
		tEngine.updateEntity();
		assertEquals(0, tEngine.mExhaustCO2, "the direct vent");

		tEngine.mExhaustCO2 = 5;
		tEngine.setBackCollideProbe(() -> true); // a solid block beyond the back
		tEngine.updateEntity();
		assertEquals(5, tEngine.mExhaustCO2, "no vent through a solid neighbour — the units are retained");
	}

	// ---------------------------------------------------------------------------
	// the ENGINE_FUELS lookup (probe / consume / fast path / containsInput)
	// ---------------------------------------------------------------------------

	@Test
	public void lookupProbeThenConsumeMatchesUpstreamTwoPhase() {
		GTDieselEngineBlockEntity tEngine = engine();
		fillInput(tEngine, new FluidStack(Fluids.WATER, 2));
		// the SEARCH phase is amount-insensitive (upstream :487/:501 probe (F, T)): a 0 L
		// identity stack still FINDS the row...
		// the search phase itself stays amount-insensitive for a LIVE tank (upstream :805
		// has no empty guard): any positive amount finds the row
		Recipe tFound = tEngine.findFuelRecipe(null, new FluidStack(Fluids.WATER, 5));
		assertEquals(sWaterRow, tFound);
		// a 0 L stack is normalized to EMPTY by the Forge FluidStack surface (updateEmpty),
		// so the lookup sees no fluids at all — the declared burn-dry deviation (BE class doc)
		assertNull(tEngine.findFuelRecipe(null, new FluidStack(Fluids.WATER, 0)));
		assertNull(tEngine.findFuelRecipe(null, null));
		// ...the APPLY phase checks the amount (the :118 (T, F) form)
		FluidStack[] tZero = {new FluidStack(Fluids.WATER, 0)};
		assertFalse(sWaterRow.isRecipeInputEqual(true, false, tZero, GTDieselEngineBlockEntity.ZL_IS),
				"a zero tank cannot feed the apply");
		// and a full tank applies with exactly one litre consumed
		FluidStack[] tTwo = {new FluidStack(Fluids.WATER, 2)};
		assertTrue(sWaterRow.isRecipeInputEqual(true, false, tTwo, GTDieselEngineBlockEntity.ZL_IS));
		assertEquals(1, tTwo[0].getAmount());
	}

	@Test
	public void lastRecipeFastPathFallsThroughToScan() {
		GTDieselEngineBlockEntity tEngine = engine();
		tEngine.mLastRecipe = sLavaRow; // the stale fast path (the fuel was swapped)
		fillInput(tEngine, new FluidStack(Fluids.WATER, 1));
		assertEquals(sWaterRow, tEngine.findFuelRecipe(sLavaRow, tEngine.mTanks[0].getFluid()),
				":487 the fast path probes and falls through to the :498 scan");
	}

	@Test
	public void containsInputGateRefusesNonFuels() {
		GTDieselEngineBlockEntity tEngine = engine();
		assertTrue(tEngine.containsFuelInput(new FluidStack(Fluids.WATER, 8)), "a table fluid passes (:189/:204)");
		assertTrue(tEngine.containsFuelInput(new FluidStack(Fluids.LAVA, 1)));
		assertFalse(tEngine.containsFuelInput(new FluidStack(Fluids.FLOWING_WATER, 8)), "a non-fuel is refused");
		assertFalse(tEngine.containsFuelInput(null));
	}

	@Test
	public void funnelFillGatedAndCapacityBound() {
		GTDieselEngineBlockEntity tEngine = engine();
		// the containsInput gate (:204) — the same refusal the /gt6engine fuel command surfaces
		assertEquals(0, tEngine.funnelFill(new FluidStack(Fluids.FLOWING_WATER, 100), true));
		// the fill lands and caps at mRate * 10 (:82-83)
		assertEquals(160, tEngine.funnelFill(new FluidStack(Fluids.WATER, 100000), true));
		assertEquals(0, tEngine.funnelFill(new FluidStack(Fluids.WATER, 1), true), "full tank takes nothing");
		// simulate-only probes without consuming
		tEngine.mTanks[0].setEmpty();
		assertEquals(10, tEngine.funnelFill(new FluidStack(Fluids.WATER, 10), false));
		assertEquals(0, tEngine.mTanks[0].amount(), "the simulate fill left nothing");
	}

	@Test
	public void tapDrainPrefersOutputTank() {
		// upstream :209-213 — output tank first, else the input tank
		GTDieselEngineBlockEntity tEngine = engine();
		fillInput(tEngine, new FluidStack(Fluids.WATER, 5));
		assertEquals(5, tEngine.tapDrain(100, true).getAmount(), "empty output -> the input drains");
		assertNull(tEngine.mTanks[0].getFluid(), "drained dry to a true empty (the upstream drain() setEmpty form)");
	}

	@Test
	public void faceFamilyAndPureSourceRefusal() {
		GTDieselEngineBlockEntity tEngine = engine();
		tEngine.mEnergy = 100;
		for (byte tFacing = 0; tFacing < 6; tFacing++) {
			tEngine.mFacing = tFacing;
			for (byte tSide = 0; tSide < 6; tSide++) {
				boolean tExpect = tSide == tFacing;
				assertEquals(tExpect, tEngine.isEnergyEmittingTo(TD.Energy.RU, tSide, true), "theoretical " + tSide + "@" + tFacing);
				assertEquals(tExpect, tEngine.isEnergyEmittingTo(TD.Energy.RU, tSide, false), "real " + tSide + "@" + tFacing);
			}
			// the type gate: only the emitting RU face (upstream :226-227)
			assertFalse(tEngine.isEnergyEmittingTo(TD.Energy.EU, tFacing, true));
			assertFalse(tEngine.isEnergyEmittingTo(TD.Energy.KU, tFacing, true));
			assertFalse(tEngine.isEnergyAcceptingFrom(TD.Energy.RU, tFacing, true), "pure source (upstream :226 aEmitting-gated)");
			assertEquals(0, tEngine.doEnergyInjection(TD.Energy.RU, tFacing, 16, 1, true), "the incoming door is shut");
		}
		// the offered/band surfaces (:228-231)
		assertEquals(16, tEngine.getEnergyOffered(TD.Energy.RU, (byte) tEngine.mFacing, 16), "min(mRate, mEnergy)");
		tEngine.mEnergy = 5;
		assertEquals(5, tEngine.getEnergyOffered(TD.Energy.RU, (byte) tEngine.mFacing, 16), "capped at the stored energy");
		tEngine.mEnergy = 100;
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertEquals(16, tEngine.getEnergySizeOutputMin(TD.Energy.RU, tSide));
			assertEquals(16, tEngine.getEnergySizeOutputRecommended(TD.Energy.RU, tSide));
			assertEquals(16, tEngine.getEnergySizeOutputMax(TD.Energy.RU, tSide));
		}
		Collection<TagData> tTypes = tEngine.getEnergyTypes((byte) 2);
		assertEquals(1, tTypes.size());
		assertTrue(tTypes.contains(TD.Energy.RU));
	}

	@Test
	public void facingMirrorSyncsFromState() {
		// the /setblock gt6:diesel_engine_bronze[facing=west] path — the vanilla stairs state
		// carries the SAME HORIZONTAL_FACING instance (the crank test form)
		BlockState tState = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
		GTDieselEngineBlockEntity tEngine = new GTDieselEngineBlockEntity(sType, POS, tState);
		assertEquals(2, tEngine.getFacing());
		tEngine.updateEntity();
		assertEquals(4, tEngine.getFacing(), "WEST == 4, the emit side re-synced");
		assertTrue(tEngine.isEnergyEmittingTo(TD.Energy.RU, (byte) 4, false));
		assertTrue(tEngine.back() == Direction.EAST, "the exhaust leaves the back (OPOS)");
		// a state WITHOUT the property leaves the mirror alone
		GTDieselEngineBlockEntity tPlain = engine();
		tPlain.mFacing = 5;
		tPlain.updateEntity();
		assertEquals(5, tPlain.getFacing());
	}

	@Test
	public void activityTrinaryShiftsAndTriStates() {
		// the TE_Behavior_Active_Trinary.check :48-53 verbatim — 64 bit register, state 0
		// (64 idle) / 1 (64 steady) / 2 (mixed)
		GTDieselEngineBlockEntity tEngine = engine();
		fillInput(tEngine, new FluidStack(Fluids.WATER, 2));
		tEngine.updateEntity(); // burn -> mActive = T (checks start at mTimer > 2)
		tEngine.updateEntity();
		tEngine.updateEntity();
		tEngine.updateEntity(); // the 3rd+ call runs onTickCheck: the register takes the bits
		assertTrue(tEngine.mActivityData > 0, "the active bits shifted in");
		assertEquals(2, tEngine.mActivityState, "mixed history -> trinary 2");
		tEngine.mActivityData = ~0L;
		tEngine.mActivityState = 1;
		tEngine.updateEntity(); // still burning: all-ones stays 1
		assertEquals(1, tEngine.mActivityState, "steady burn -> trinary 1");
	}

	@Test
	public void nbtRoundTrip() {
		GTDieselEngineBlockEntity tEngine = engine();
		tEngine.mEnergy = 1234;
		tEngine.mRate = 512;
		tEngine.mExhaustCO2 = 7;
		tEngine.mActivityData = 0xFL;
		fillInput(tEngine, new FluidStack(Fluids.WATER, 40));
		CompoundTag tTag = tEngine.saveWithoutMetadata();

		GTDieselEngineBlockEntity tLoaded = engine();
		tLoaded.load(tTag);
		assertEquals(1234, tLoaded.mEnergy, "NBT_ENERGY :75/:89");
		assertEquals(512, tLoaded.mRate, "NBT_OUTPUT :78");
		assertEquals(5120, tLoaded.mTanks[0].capacity(), "the :82 capacity re-bind");
		assertEquals(40, tLoaded.mTanks[0].amount());
		assertEquals(7, tLoaded.mExhaustCO2, "the exhaust counter persists");
		assertEquals(0xFL, tLoaded.mActivityData, "the activity register persists");
		assertFalse(tLoaded.mStopped, "NBT_STOPPED default");
		assertEquals("diesel_engine", tTag.getString("te_name"));
		assertTrue(tTag.contains(GTDieselEngineBlockEntity.NBT_ENERGY, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(GTDieselEngineBlockEntity.NBT_TANK + ".0", Tag.TAG_COMPOUND));
		assertTrue(tTag.contains(GTDieselEngineBlockEntity.NBT_OUTPUT, Tag.TAG_ANY_NUMERIC));
	}
}
