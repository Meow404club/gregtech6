package gregtech6.tileentity.energy.reactors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregtech6.reactor.IReactorRodItem;
import gregtech6.reactor.ReactorCoolantFluids;
import gregtech6.reactor.ReactorRadioactivity;
import gregtech6.reactor.ReactorRodNbt;
import gregtech6.reactor.neutron.BreederRodSpec;
import gregtech6.reactor.neutron.FuelRodSpec;
import gregtech6.reactor.neutron.ReactorCoolant;
import gregtech6.reactor.neutron.ReactorRodKind;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.sensors.GT6GeigerCounterBlockEntity;

/**
 * The 2x2 reactor core offline acceptance (task debt-reactor-b-2x2-be) — the
 * table-driven pin that the BE WIRING reproduces the upstream tick over the A-card
 * neutron domain: the coolant conversion (Core2x2:136-195, every number from the
 * {@link ReactorCoolant} row), the Na heat divider (:129-134), the fuel/absorber/
 * breeder reaction heat (Nuclear:205/Absorber:46/Breeder:83), the in-block + cross-core
 * exchange over {@link gregtech6.reactor.neutron.ReactorLattice2x2} (Core2x2:53-97),
 * the durability/depletion and breeder-product in-place swaps (Nuclear:215-224,
 * Breeder:84-90), the commented-explosion meltdown (:197-210 — the rods die, the block
 * does NOT explode) and the geiger read chain (MultiTileEntityGeigerCounter:45-66 over
 * {@code oNeutronCounts} + the coolant-modulated neutron maximums).
 *
 * <p>The rod items are UNCREATABLE TEST FIXTURES ({@link FixtureRodItem} over
 * {@link IReactorRodItem}) — the 46 real rod items and recipes are the C card. The
 * coolant fluids are the vanilla WATER/LAVA stand-ins through
 * {@link ReactorCoolantFluids}' override seams (the identity comparison is the wiring
 * under test, the actual port fluids ride the live registry).
 */
public class GT6ReactorCore2x2Test extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(0, 0, 0);

	static BlockEntityType<TestCore> sCoreType;
	static BlockEntityType<GT6GeigerCounterBlockEntity> sGeigerType;

	static FixtureRod sFuel, sReflector, sAbsorber, sBreeder, sDepleted, sProduct;

	/** The concrete test BE — the reactor class over a vanilla-block fixture BET. */
	public static final class TestCore extends GT6ReactorCore2x2BlockEntity {
		public TestCore(BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			super(sCoreType, aPos, aState);
		}
	}

	/** The uncraftable test rod (the C-card rod items' seam double). */
	static final class FixtureRod extends Item implements IReactorRodItem {
		final ReactorRodKind mKind;
		@Nullable final FuelRodSpec mFuel;
		@Nullable final BreederRodSpec mBreeder;
		@Nullable final java.util.function.IntFunction<ItemStack> mSwap;

		FixtureRod(ReactorRodKind aKind, @Nullable FuelRodSpec aFuel, @Nullable BreederRodSpec aBreeder, @Nullable java.util.function.IntFunction<ItemStack> aSwap) {
			super(new Item.Properties());
			mKind = aKind;
			mFuel = aFuel;
			mBreeder = aBreeder;
			mSwap = aSwap;
		}

		@Override public ReactorRodKind rodKind(ItemStack aStack) {return mKind;}
		@Override public FuelRodSpec fuelSpec(ItemStack aStack) {return mFuel;}
		@Override public BreederRodSpec breederSpec(ItemStack aStack) {return mBreeder;}
		@Override public ItemStack rodSwapTarget(int aTargetId) {return mSwap == null ? null : mSwap.apply(aTargetId);}
	}

	// the fixture fuel: self 4 / other 4 / div 16 / max 2048 (the U-235 shape, tiny durability for the depletion arm)
	static final FuelRodSpec FIXTURE_FUEL = new FuelRodSpec(9221, "Fixture", 1_000_000L, 4, 4, 16, 2048, 9321);
	static final BreederRodSpec FIXTURE_BREEDER = new BreederRodSpec(9410, "Fixture", 50, 10, 9411);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildFixtures() {
		// the fixture BETs (the bedrock-drill lesson: 21.1 validates the BE type x block binding)
		BlockEntityType<TestCore>[] tHolder = (BlockEntityType<TestCore>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(TestCore::new, Blocks.BRICKS).build(null);
		sCoreType = tHolder[0];
		BlockEntityType<GT6GeigerCounterBlockEntity>[] tGeiger = (BlockEntityType<GT6GeigerCounterBlockEntity>[]) new BlockEntityType<?>[1];
		tGeiger[0] = BlockEntityType.Builder.of(GT6GeigerCounterBlockEntity::new, Blocks.BRICKS).build(null);
		sGeigerType = tGeiger[0];

		// the rod items (the ItemStack ctor needs a registry delegate — the offline latch)
		sFuel = registerItemFixture("reactor_fixture_fuel", () -> new FixtureRod(ReactorRodKind.FUEL, FIXTURE_FUEL, null, aId -> new ItemStack(sDepleted)));
		sReflector = registerItemFixture("reactor_fixture_reflector", () -> new FixtureRod(ReactorRodKind.REFLECTOR, null, null, null));
		sAbsorber = registerItemFixture("reactor_fixture_absorber", () -> new FixtureRod(ReactorRodKind.ABSORBER, null, null, null));
		sBreeder = registerItemFixture("reactor_fixture_breeder", () -> new FixtureRod(ReactorRodKind.BREEDER, null, FIXTURE_BREEDER, aId -> new ItemStack(sProduct)));
		sDepleted = registerItemFixture("reactor_fixture_depleted", () -> new FixtureRod(ReactorRodKind.DEPLETED, null, null, null));
		sProduct = registerItemFixture("reactor_fixture_product", () -> new FixtureRod(ReactorRodKind.PRODUCT, null, null, null));
	}

	@BeforeEach
	void freshCoolantBinding() {
		ReactorCoolantFluids.INPUT_OVERRIDES.clear();
		ReactorCoolantFluids.HOT_OVERRIDES.clear();
	}

	private static TestCore core() {
		return new TestCore(POS, Blocks.BRICKS.defaultBlockState());
	}

	/** A fresh fuel rod: full burn budget on the stack NBT (the upstream canner recipe output carries NBT_MAXDURABILITY, Nuclear.java:49). */
	private static ItemStack fuelRod() {
		ItemStack tRod = new ItemStack(sFuel);
		ReactorRodNbt.setDurability(tRod, FIXTURE_FUEL.durability());
		return tRod;
	}

	private static void bindWater() {
		ReactorCoolantFluids.INPUT_OVERRIDES.put(ReactorCoolant.DISTILLED_WATER, Fluids.WATER);
		ReactorCoolantFluids.HOT_OVERRIDES.put(ReactorCoolant.DISTILLED_WATER, Fluids.LAVA); // the "steam" stand-in
	}

	/** The no-meltdown neutral coolant: SEMIHEAVY carries the identity emission params, so the plain-triple arithmetic is unchanged. */
	private static void bindNeutral(TestCore aCore) {
		ReactorCoolantFluids.INPUT_OVERRIDES.put(ReactorCoolant.SEMIHEAVY_WATER, Fluids.WATER);
		ReactorCoolantFluids.HOT_OVERRIDES.put(ReactorCoolant.SEMIHEAVY_WATER, Fluids.LAVA);
		aCore.mTanks[0].fill(new FluidStack(Fluids.WATER, 64000), FluidAction.EXECUTE);
	}

	// -------------------------------------------------------------------------
	// structure (Core:57-62 + the NBT round trip :64-98)
	// -------------------------------------------------------------------------

	@Test
	public void structureCarriesTheDoubleTanksAndFourSingleRodSlots() {
		TestCore tCore = core();
		assertEquals(64000, tCore.mTanks[0].capacity(), "Core:62 — both tanks 64000 L");
		assertEquals(64000, tCore.mTanks[1].capacity(), "the plain-fluid output capacity");
		bindWater();
		FluidStack tSteam = new FluidStack(Fluids.LAVA, 1);
		assertTrue(ReactorCoolantFluids.isSteam(tSteam), "the bound hot output IS the steam identity");
		assertEquals(GT6ReactorCore2x2BlockEntity.STEAM_TANK_CAPACITY, tCore.mTanks[1].capacity(tSteam), "Core:62 — 64000 * STEAM_PER_WATER for steam");
		assertEquals(4, tCore.mInventory.getSlots());
		assertEquals(1, tCore.mInventory.getSlotLimit(0), "Core:312 — single-stack slots");
		assertTrue(tCore.isReactorRod(new ItemStack(sFuel)));
		assertFalse(tCore.isReactorRod(new ItemStack(Blocks.BRICKS.asItem())));
	}

	@Test
	public void nbtRoundTripCarriesTheUpstreamKeySet() {
		TestCore tCore = core();
		tCore.mMode = 5;
		tCore.mEnergy = 12345;
		tCore.mRunning = true;
		tCore.mStopped = true;
		tCore.mFacing = 3;
		tCore.mSecondFacing = 2;
		tCore.mNeutronCounts = new int[] {11, 22, 33, 44};
		tCore.oNeutronCounts = new int[] {1, 2, 3, 4};
		tCore.mInventory.setStackInSlot(2, new ItemStack(sFuel));
		bindWater();
		tCore.mTanks[0].fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
		CompoundTag tNBT = tCore.saveWithoutMetadata();

		TestCore tBack = core();
		tBack.load(tNBT);
		assertEquals(5, tBack.mMode, "gt.mode");
		assertEquals(12345, tBack.mEnergy, "gt.energy");
		assertTrue(tBack.mRunning, "gt.active");
		assertTrue(tBack.mStopped, "gt.stopped");
		assertEquals(3, tBack.mFacing, "gt.facing");
		assertEquals(2, tBack.mSecondFacing, "gt.facing.2nd");
		assertEquals(11, tBack.mNeutronCounts[0]);
		assertEquals(44, tBack.mNeutronCounts[3]);
		assertEquals(4, tBack.oNeutronCounts[3]);
		assertEquals(sFuel, tBack.mInventory.getStackInSlot(2).getItem());
		assertEquals(500, tBack.mTanks[0].amount(), "gt.tank.0");
	}

	// -------------------------------------------------------------------------
	// the coolant conversion (Core2x2:136-195 over the A-card table)
	// -------------------------------------------------------------------------

	@Test
	public void waterConvertsAtEightyHuPerLitreIntoSteamTimes160() {
		TestCore tCore = core();
		tCore.mInventory.setStackInSlot(0, fuelRod());
		tCore.mNeutronCounts[0] = 100;
		bindWater();
		tCore.mTanks[0].fill(new FluidStack(Fluids.WATER, 64000), FluidAction.EXECUTE);
		tCore.mTimeSeam = 1; // %20==1: no exchange, the plain reaction tick
		tCore.onTick(1, true);

		// Nuclear:205 — 100 neutrons = 100 HU; water: 100/80 = 1 L at 80 HU, steam 1*160
		assertEquals(100, tCore.oEnergy, ":134 oEnergy — this tick's converted output");
		assertEquals(20, tCore.mEnergy, ":146 mEnergy -= 80 * 1");
		assertEquals(63999, tCore.mTanks[0].amount(), ":146 the remove(tEnergy) half");
		FluidStack tHot = tCore.mTanks[1].getFluid();
		assertNotNull(tHot);
		assertEquals(Fluids.LAVA, tHot.getFluid(), "the bound hot identity");
		assertEquals(160, tHot.getAmount(), "Core2x2:145 — tEnergy * STEAM_PER_WATER");
	}

	@Test
	public void sodiumDividesGeneratedHeatBySixAndConvertsAtThirty() {
		TestCore tCore = core();
		tCore.mInventory.setStackInSlot(0, fuelRod());
		tCore.mNeutronCounts[0] = 600;
		ReactorCoolantFluids.INPUT_OVERRIDES.put(ReactorCoolant.MOLTEN_SODIUM, Fluids.WATER);
		ReactorCoolantFluids.HOT_OVERRIDES.put(ReactorCoolant.MOLTEN_SODIUM, Fluids.LAVA);
		tCore.mTanks[0].fill(new FluidStack(Fluids.WATER, 64000), FluidAction.EXECUTE);
		tCore.mTimeSeam = 1;
		tCore.onTick(1, true);

		// :129-132 — only the heat GENERATED this tick is divided: divup(600, 6) = 100
		assertEquals(100, tCore.oEnergy, ":134 — the Na /6 divider");
		// :154-156 — 100/30 = 3 L at 30 HU
		assertEquals(10, tCore.mEnergy, "100 - 30*3");
		assertEquals(3, tCore.mTanks[1].getFluid().getAmount());
	}

	@Test
	public void ic2CoolantConvertsAtTwentyHuPerLitre() {
		TestCore tCore = core();
		tCore.mInventory.setStackInSlot(0, fuelRod());
		tCore.mNeutronCounts[0] = 45;
		ReactorCoolantFluids.INPUT_OVERRIDES.put(ReactorCoolant.IC2_COOLANT, Fluids.WATER);
		ReactorCoolantFluids.HOT_OVERRIDES.put(ReactorCoolant.IC2_COOLANT, Fluids.LAVA);
		tCore.mTanks[0].fill(new FluidStack(Fluids.WATER, 64000), FluidAction.EXECUTE);
		tCore.mTimeSeam = 1;
		tCore.onTick(1, true);
		assertEquals(2, tCore.mTanks[1].getFluid().getAmount(), ":139-141 — 45/20 = 2 L");
		assertEquals(5, tCore.mEnergy, "45 - 20*2");
	}

	@Test
	public void thoriumSaltOutputsPlainLiclNotTheHotVariant() {
		TestCore tCore = core();
		tCore.mInventory.setStackInSlot(0, fuelRod());
		tCore.mNeutronCounts[0] = 2_560_000; // exactly one litre at the suspicious EU_PER_THORIUM_SALT
		ReactorCoolantFluids.INPUT_OVERRIDES.put(ReactorCoolant.THORIUM_SALT, Fluids.WATER);
		ReactorCoolantFluids.HOT_OVERRIDES.put(ReactorCoolant.THORIUM_SALT, Fluids.LAVA); // the PLAIN LiCl binding (Core2x2:190)
		tCore.mTanks[0].fill(new FluidStack(Fluids.WATER, 64000), FluidAction.EXECUTE);
		tCore.mTimeSeam = 1;
		tCore.onTick(1, true);
		assertEquals(1, tCore.mTanks[1].getFluid().getAmount(), ":190 — 1 L plain molten LiCl");
		assertEquals(0, tCore.mEnergy, "2_560_000 - 2_560_000*1");
	}

	@Test
	public void unwhitelistedCoolantDoesNotConvertAndFullHotTankExplodes() {
		TestCore tCore = core();
		tCore.mInventory.setStackInSlot(0, fuelRod());
		tCore.mNeutronCounts[0] = 100;
		tCore.mTanks[0].fill(new FluidStack(Fluids.WATER, 64000), FluidAction.EXECUTE); // WATER unbound = off the whitelist
		tCore.mTimeSeam = 1;
		tCore.onTick(1, true);
		// off the whitelist: no conversion branch runs, no explosion trigger (:193 only fires on the EMPTY tank)
		assertEquals(100, tCore.mEnergy);
		assertTrue(tCore.mTanks[1].isEmpty());

		// the full-hot-tank arm: the conversion cannot fit its output -> tIsExploding (:142 etc.)
		// (the hot stand-in IS the steam identity here, so the prefill rides the stretched
		// capacity: 10_240_000 - 100 leaves no room for the 160 L steam share)
		TestCore tFull = core();
		tFull.mInventory.setStackInSlot(0, fuelRod());
		tFull.mNeutronCounts[0] = 100;
		ReactorCoolantFluids.INPUT_OVERRIDES.put(ReactorCoolant.DISTILLED_WATER, Fluids.WATER);
		ReactorCoolantFluids.HOT_OVERRIDES.put(ReactorCoolant.DISTILLED_WATER, Fluids.WATER);
		tFull.mTanks[0].fill(new FluidStack(Fluids.WATER, 64000), FluidAction.EXECUTE);
		tFull.mTanks[1].fill(new FluidStack(Fluids.WATER, (int) (GT6ReactorCore2x2BlockEntity.STEAM_TANK_CAPACITY - 100)), FluidAction.EXECUTE);
		tFull.mTimeSeam = 1;
		tFull.onTick(1, true);
		assertTrue(tFull.inventoryEmpty(), ":200 — the meltdown killed all four rods");
	}

	// -------------------------------------------------------------------------
	// the reaction heat + the meltdown semantics (Core2x2:197-210)
	// -------------------------------------------------------------------------

	@Test
	public void reactionHeatIsOneHuPerNeutronOnFuel() {
		TestCore tCore = core();
		tCore.mInventory.setStackInSlot(0, fuelRod());
		tCore.mNeutronCounts[0] = 77; // no coolant bound -> no conversion, heat just books
		tCore.mTimeSeam = 1;
		tCore.onTick(1, true);
		assertEquals(77, tCore.mEnergy, "Nuclear:205");
		assertTrue(tCore.mRunning, "Nuclear:225 — the reaction keeps the core running");
	}

	@Test
	public void absorberEmitsTwiceTheHeatAndBreederHalf() {
		TestCore tCore = core();
		tCore.mInventory.setStackInSlot(1, new ItemStack(sAbsorber));
		tCore.mNeutronCounts[1] = 10;
		tCore.mTimeSeam = 1;
		tCore.onTick(1, true);
		assertEquals(20, tCore.mEnergy, "Absorber:46 — 2 HU per neutron");

		TestCore tBreeder = core();
		tBreeder.mInventory.setStackInSlot(2, new ItemStack(sBreeder));
		tBreeder.mNeutronCounts[2] = 10;
		tBreeder.mTimeSeam = 1;
		tBreeder.onTick(1, true);
		assertEquals(5, tBreeder.mEnergy, "Breeder:83 — half heat");
	}

	@Test
	public void emptyTankWithHeatMeltsDownWithoutExplodingTheBlock() {
		TestCore tCore = core();
		tCore.mInventory.setStackInSlot(0, fuelRod());
		tCore.mInventory.setStackInSlot(3, new ItemStack(sReflector));
		tCore.mNeutronCounts[0] = 50;
		tCore.mTimeSeam = 1;
		tCore.onTick(1, true);
		// :193-194 — empty tank with oEnergy > 0 explodes; :197+ the COMMENTED explode(10):
		// the rods die (slotKill), the sound plays, the radiation doubles — the BLOCK SURVIVES
		assertTrue(tCore.inventoryEmpty(), ":200 slotKill(0..3)");
		assertFalse(tCore.isRemoved(), ":199 — explode(10) stays commented upstream, the core block survives");
		assertEquals(50, tCore.mEnergy, "the meltdown does not clear the stored heat");
	}

	// -------------------------------------------------------------------------
	// the exchange (Core2x2:53-97 over ReactorLattice2x2)
	// -------------------------------------------------------------------------

	@Test
	public void inBlockReflectorsReturnTheEmissionToItsSource() {
		TestCore tCore = core();
		tCore.mInventory.setStackInSlot(0, fuelRod());   // emits
		tCore.mInventory.setStackInSlot(1, new ItemStack(sReflector)); // IN_BLOCK_TARGETS[0] = {1, 2}
		tCore.mInventory.setStackInSlot(2, new ItemStack(sReflector));
		tCore.mNeutronCounts[0] = 100;
		bindNeutral(tCore); // a live coolant keeps the empty-tank meltdown out of the way
		tCore.mTimeSeam = 1;
		tCore.onTick(1, true); // books oNeutronCounts[0] = 100

		tCore.mTimeSeam = 19; // the exchange tick
		tCore.onTick(19, true);
		// Nuclear:198 — emission = other + divup(max(100 - self, 0), div) = 4 + divup(96, 16) = 10
		// (no coolant -> the plain unmodulated triple)
		// self lands on the own count (Nuclear:197), the two in-block reflectors bounce 10 each
		assertEquals(100 + 4 + 10 + 10, tCore.mNeutronCounts[0], "the exchange pass over IN_BLOCK_TARGETS[0]");
	}

	@Test
	public void crossCoreEmissionLandsOnTheS2103ReceivingSlot() {
		TestCore tA = core();
		TestCore tB = core();
		tA.mInventory.setStackInSlot(0, fuelRod());
		tB.mInventory.setStackInSlot(1, new ItemStack(sReflector)); // S2103[3] = 1 — the Z_NEG receiving slot of slot 0
		tA.mNeutronCounts[0] = 100;
		bindNeutral(tA); // a live coolant keeps the empty-tank meltdown out of the way
		GT6ReactorCore2x2BlockEntity[] tAdjacents = new GT6ReactorCore2x2BlockEntity[6];
		tAdjacents[2] = tB; // OUTWARD_SIDES[0] = {Z_NEG, X_NEG} — the Z_NEG neighbour
		tA.setAdjacentCoresOverride(tAdjacents);
		tA.mTimeSeam = 1;
		tA.onTick(1, true);

		tA.mTimeSeam = 19;
		tA.onTick(19, true);
		// emission 10: 4 self + 0 in-block (slots 1/2 empty) + 10 bounced off B's slot 1
		assertEquals(100 + 4 + 10, tA.mNeutronCounts[0], "the neighbour reflection lands back on the emitter");
		assertEquals(0, tB.mNeutronCounts[1], "the reflector absorbs nothing (Reflector.java:51)");
	}

	// -------------------------------------------------------------------------
	// durability / depletion / breeding (Nuclear:215-224, Breeder:84-90)
	// -------------------------------------------------------------------------

	@Test
	public void fuelBurnsAtTheAdvertisedRateAndSwapsToTheDepletedRod() {
		TestCore tCore = core();
		ItemStack tRod = new ItemStack(sFuel);
		ReactorRodNbt.setDurability(tRod, 100); // exactly one tick of the <=max floor (Nuclear:215)
		tCore.mInventory.setStackInSlot(0, tRod);
		tCore.mNeutronCounts[0] = 0; // 0 neutrons still cost the 100/t floor
		tCore.mTimeSeam = 1;
		tCore.onTick(1, true);
		assertEquals(sDepleted, tCore.mInventory.getStackInSlot(0).getItem(), "Nuclear:220-224 — the in-place depleted swap");
		assertEquals(0, ReactorRodNbt.durability(tCore.mInventory.getStackInSlot(0)), "ST.nbt(aStack, null) — the swap clears the NBT");
	}

	@Test
	public void moderatedFuelLastsOnlyAQuarterAsLong() {
		TestCore tCore = core();
		ItemStack tRod = new ItemStack(sFuel);
		ReactorRodNbt.setDurability(tRod, 401);
		ReactorRodNbt.setModerated(tRod, false, true); // oModerated latched
		tCore.mInventory.setStackInSlot(0, tRod);
		tCore.mTimeSeam = 1;
		tCore.onTick(1, true);
		assertEquals(1, ReactorRodNbt.durability(tCore.mInventory.getStackInSlot(0)), "Nuclear:216 — the x4 moderated loss (unmoderated would leave 301)");
		assertEquals(sFuel, tCore.mInventory.getStackInSlot(0).getItem(), "no depletion at 401 under the x4 loss");
	}

	@Test
	public void breederAdmitsNeutronsMinusLossPerSideAndSwapsAtZero() {
		TestCore tCore = core();
		tCore.mInventory.setStackInSlot(0, fuelRod()); // the emitter
		tCore.mInventory.setStackInSlot(1, new ItemStack(sBreeder));
		ItemStack tBreeder = tCore.mInventory.getStackInSlot(1);
		ReactorRodNbt.setDurability(tBreeder, 50); // FIXTURE_BREEDER.needed
		tCore.mNeutronCounts[0] = 100;
		bindNeutral(tCore); // a live coolant keeps the empty-tank meltdown out of the way
		tCore.mTimeSeam = 1;
		tCore.onTick(1, true); // o0 = 100
		tCore.mTimeSeam = 19;
		tCore.onTick(19, true);
		// emission 10 into the breeder: unmoderated, 10 > loss 10 is FALSE -> admits 0 (Breeder:97)
		assertEquals(0, tCore.mNeutronCounts[1], "Breeder.java:97 — aNeutrons > mNeutronLoss is strict");
		assertEquals(sBreeder, tCore.mInventory.getStackInSlot(1).getItem());

		// one tick with 30 incoming: admits 20, breeds 20 of the 50 budget
		tCore.mNeutronCounts[1] = 20;
		tCore.mTimeSeam = 20;
		tCore.onTick(20, true);
		assertEquals(30, ReactorRodNbt.durability(tCore.mInventory.getStackInSlot(1)), "Breeder:84-90 — the budget drains");
		tCore.mNeutronCounts[1] = 40;
		tCore.mTimeSeam = 21;
		tCore.onTick(21, true);
		assertEquals(sProduct, tCore.mInventory.getStackInSlot(1).getItem(), "Breeder:86 — the enriched product swap");
	}

	// -------------------------------------------------------------------------
	// the geiger read chain (MultiTileEntityGeigerCounter:45-66)
	// -------------------------------------------------------------------------

	@Test
	public void geigerCounterReadsTheONeutronSumAndTheCoolantModulatedMaximum() {
		TestCore tCore = core();
		tCore.mInventory.setStackInSlot(0, fuelRod());
		tCore.mInventory.setStackInSlot(1, new ItemStack(sFuel));
		tCore.oNeutronCounts = new int[] {100, 200, 0, 0};
		ReactorCoolantFluids.INPUT_OVERRIDES.put(ReactorCoolant.HEAVY_WATER, Fluids.WATER);
		tCore.mTanks[0].fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);

		GT6GeigerCounterBlockEntity tMeter = new GT6GeigerCounterBlockEntity(sGeigerType, POS, Blocks.BRICKS.defaultBlockState());
		assertEquals(300, tMeter.getCurrentValue(tCore), ":46-48 — UT.Code.sum(oNeutronCounts)");
		assertEquals(512, tMeter.getCurrentMax(tCore), ":53-66 + Nuclear:246 — D2O: divup(2048, 8) per rod, x2 rods");
		assertEquals(0, tMeter.getCurrentValue(null), ":49");
		assertEquals(0, tMeter.getCurrentMax(null), ":65");
	}

	// -------------------------------------------------------------------------
	// the radiation potion group (UT.Entities.applyRadioactivity, UT.java:3033-3053)
	// -------------------------------------------------------------------------

	/** The Unsafe-allocated LivingEntity double (the CoverCraftingAsphaltTest form) with an effect-recording override. */
	private static class RadEntity extends LivingEntity {
		java.util.Map<Object, MobEffectInstance> mApplied; // lazy — the Unsafe allocator skips field initializers

		RadEntity() {
			super(EntityType.PIG, null);
		}

		@Override public boolean isAlive() {return true;} // the Unsafe carrier has no syncher data
		//? if forge {
		@Override public boolean hasEffect(MobEffect aEffect) {return false;}
		@Override public MobEffectInstance getEffect(MobEffect aEffect) {return null;}
		//?} else {
		/*@Override public boolean hasEffect(net.minecraft.core.Holder<MobEffect> aEffect) {return false;}
		@Override public MobEffectInstance getEffect(net.minecraft.core.Holder<MobEffect> aEffect) {return null;}
		 *///?}
		@Override public boolean addEffect(MobEffectInstance aEffect, @Nullable net.minecraft.world.entity.Entity aSource) {
			// the 1-arg addEffect is final on both legs — the 2-arg carrier is the override point
			if (mApplied == null) mApplied = new java.util.IdentityHashMap<>();
			mApplied.put(aEffect.getEffect(), aEffect);
			return true;
		}
//? if forge {
		@Override protected void defineSynchedData() {/**/}
//?} else {
		/*@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder aBuilder) {}
*///?}
		@Override public void readAdditionalSaveData(CompoundTag aTag) {/**/}
		@Override public void addAdditionalSaveData(CompoundTag aTag) {/**/}
		@Override public Iterable<ItemStack> getArmorSlots() {return java.util.List.of();}
		@Override public ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot aSlot) {return ItemStack.EMPTY;}
		@Override public void setItemSlot(net.minecraft.world.entity.EquipmentSlot aSlot, ItemStack aStack) {/**/}
		@Override public net.minecraft.world.entity.HumanoidArm getMainArm() {return net.minecraft.world.entity.HumanoidArm.RIGHT;}
	}

	private static RadEntity radEntity() {
		try {
			java.lang.reflect.Field tUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			tUnsafeField.setAccessible(true);
			sun.misc.Unsafe tUnsafe = (sun.misc.Unsafe) tUnsafeField.get(null);
			return (RadEntity) tUnsafe.allocateInstance(RadEntity.class);
		} catch (ReflectiveOperationException aE) {
			throw new IllegalStateException("offline entity allocator failed", aE);
		}
	}

	@Test
	public void radioactivityAppliesThePotionGroupWithTheUpstreamConstants() {
		RadEntity tEntity = radEntity();
		assertTrue(ReactorRadioactivity.apply(tEntity, 3, 2), "the gates pass on a plain living entity");
		// amplifier bind(0, 5, 5*3/7) = 2; durations level * base * amount (UT.java:3041-3045/:3049-3050)
		assertEquals(6, tEntity.mApplied.size(), "the five vanilla potions + the wither fallback arm");
		assertEquals(3 * 140 * 2, tEntity.mApplied.get(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN).getDuration());
		assertEquals(3 * 150 * 2, tEntity.mApplied.get(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN).getDuration());
		assertEquals(3 * 130 * 2, tEntity.mApplied.get(net.minecraft.world.effect.MobEffects.CONFUSION).getDuration());
		assertEquals(3 * 150 * 2, tEntity.mApplied.get(net.minecraft.world.effect.MobEffects.WEAKNESS).getDuration());
		assertEquals(3 * 130 * 2, tEntity.mApplied.get(net.minecraft.world.effect.MobEffects.HUNGER).getDuration());
		assertEquals(3 * 130 * 2, tEntity.mApplied.get(net.minecraft.world.effect.MobEffects.WITHER).getDuration(), "the radiation-potion-absent fallback");
		assertEquals(2, tEntity.mApplied.get(net.minecraft.world.effect.MobEffects.WITHER).getAmplifier());
		// the level gate
		assertFalse(ReactorRadioactivity.apply(radEntity(), 0, 100), ":3033 — aLevel > 0");
	}

	//? if forge {
	/** The undead gate (UT.java:3034) — the 1.20.1 MobType carrier; the 21.1 leg reads EntityTypeTags instead. */
	@Test
	public void undeadCreaturesAreImmune() {
		class UndeadEntity extends RadEntity {
			@Override public net.minecraft.world.entity.MobType getMobType() {return net.minecraft.world.entity.MobType.UNDEAD;}
		}
		RadEntity tUndead;
		try {
			java.lang.reflect.Field tUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			tUnsafeField.setAccessible(true);
			sun.misc.Unsafe tUnsafe = (sun.misc.Unsafe) tUnsafeField.get(null);
			tUndead = (UndeadEntity) tUnsafe.allocateInstance(UndeadEntity.class);
		} catch (ReflectiveOperationException aE) {
			throw new IllegalStateException("offline entity allocator failed", aE);
		}
		assertFalse(ReactorRadioactivity.apply(tUndead, 3, 2));
	}
	//?}
}
