package gregtech6.tileentity.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task p13-steam-proof-repay acceptance ① — the barrel fizz chain truth table, offline.
 *
 * <p>The live gt6 fluids (steam 373 K) are registry-side and exercised by the RCON chain
 * (the GTFluidsEngineFamilyTest offline/live split); offline the gates are driven through
 * the name seam — a vanilla stack renamed by the fixture probes ({@code overrideName})
 * while the melt verdict reads a NAME-keyed offline temperature table with the upstream
 * declaration values (steam 373 K melts the 340 K wood ceiling, water 300 K does not —
 * the melt/fizz ordering stays physically honest). The FluidType lookups themselves are
 * the established offline-NPE face (route around via narrow overrides, the
 * BarrelFluidHandler {@code (byte, int)} seam precedent).
 *
	 * <p>Truth table (upstream TileEntityBase08Barrel :162 melt first, :180 gas gate, :184
	 * allowFluid gate — the two doors independent, GASPROOF exempts the gas gate only):
	 * steam vs wood (gasProof F, 340 K) = melt fires, gas gate unreached; steam vs metal
	 * (gasProof T) = gas gate skipped, the allowFluid gate trash ONCE; water vs metal = both
	 * gates pass, zero consumption. Plus the four-family gasProof values and the item-face
	 * fill gate pair (:250 allowFluid first, then :251 gas, drain gate-free).
	 */
public class BarrelFizzChainTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(2, 3, 4);
	static final long WOOD_MELTING_POINT = 340; // upstream NBT_CAPACITY_HU row, Loader_MultiTileEntities.java:2136

	static BlockEntityType<WoodFizzProbe> sWoodType;
	static BlockEntityType<MetalFizzProbe> sMetalType;
	static BlockEntityType<GTBarrelPlasticBlockEntity> sPlasticType;
	static BlockEntityType<GTBarrelLogisticsBlockEntity> sLogisticsType;

	/**
	 * The wood barrel with a counting pair of fizz/meltdown overrides and a rename seam.
	 * The FluidType lookups (temperature, density) need the live registry offline-NPE —
	 * the established route-around is a narrow override (the BarrelFluidHandler
	 * {@code (byte, int)} seam precedent): the melt verdict reads a NAME-keyed offline
	 * temperature table carrying the upstream declaration values, and the gravity push is
	 * a no-op (p5 territory with its own tests).
	 */
	static final class WoodFizzProbe extends GTBarrelBlockEntity {
		String overrideName = null;
		int fizzCount = 0;
		int meltdownCount = 0;

		WoodFizzProbe(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) { super(aType, aPos, aState); }

		@Override
		protected String fluidName(FluidStack aFluid) {
			return overrideName != null ? overrideName : super.fluidName(aFluid);
		}

		@Override
		public boolean meltsDown(FluidStack aFluid) {
			return meltsDown(offlineTemperature(overrideName != null ? overrideName : fluidName(aFluid)));
		}

		@Override
		protected void fizzTrash() {
			fizzCount++;
			super.fizzTrash();
		}

		@Override
		public boolean meltdown() {
			meltdownCount++;
			return super.meltdown();
		}

		@Override
		protected void pushByGravity() {
			// offline no-op: the FluidType density lookup needs the live registry
		}
	}

	/** The metal drum (gasProof=T through the class override) with the same counting pair. */
	static final class MetalFizzProbe extends GTBarrelMetalBlockEntity {
		String overrideName = null;
		int fizzCount = 0;
		int meltdownCount = 0;

		MetalFizzProbe(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) { super(aType, aPos, aState); }

		@Override
		protected String fluidName(FluidStack aFluid) {
			return overrideName != null ? overrideName : super.fluidName(aFluid);
		}

		@Override
		public boolean meltsDown(FluidStack aFluid) {
			return meltsDown(offlineTemperature(overrideName != null ? overrideName : fluidName(aFluid)));
		}

		@Override
		protected void fizzTrash() {
			fizzCount++;
			super.fizzTrash();
		}

		@Override
		public boolean meltdown() {
			meltdownCount++;
			return super.meltdown();
		}

		@Override
		protected void pushByGravity() {
			// offline no-op: the FluidType density lookup needs the live registry
		}
	}

	/** The offline FluidType stand-in (the GTFluids.ENGINE_SPECS declaration-table precedent): steam 373 K = FL.java:794 C+100, the liquids 300 K, lava 1300 K. */
	static long offlineTemperature(String aName) {
		return switch (aName) {
			case "steam" -> 373;
			case "lava" -> 1300;
			default -> 300;
		};
	}

	@SuppressWarnings("unchecked")
	@BeforeAll
	static void buildOfflineFixtures() {
		BlockEntityType<WoodFizzProbe>[] tWood = (BlockEntityType<WoodFizzProbe>[]) new BlockEntityType<?>[1];
		tWood[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new WoodFizzProbe(tWood[0], aPos, aState), Blocks.STONE).build(null);
		sWoodType = tWood[0];

		BlockEntityType<MetalFizzProbe>[] tMetal = (BlockEntityType<MetalFizzProbe>[]) new BlockEntityType<?>[1];
		tMetal[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new MetalFizzProbe(tMetal[0], aPos, aState), Blocks.STONE).build(null);
		sMetalType = tMetal[0];

		BlockEntityType<GTBarrelPlasticBlockEntity>[] tPlastic = (BlockEntityType<GTBarrelPlasticBlockEntity>[]) new BlockEntityType<?>[1];
		tPlastic[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelPlasticBlockEntity(tPlastic[0], aPos, aState), Blocks.STONE).build(null);
		sPlasticType = tPlastic[0];

		BlockEntityType<GTBarrelLogisticsBlockEntity>[] tLogistics = (BlockEntityType<GTBarrelLogisticsBlockEntity>[]) new BlockEntityType<?>[1];
		tLogistics[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelLogisticsBlockEntity(tLogistics[0], aPos, aState), Blocks.STONE).build(null);
		sLogisticsType = tLogistics[0];
	}

	// ---------------------------------------------------------------------------
	// the tick-chain order truth table
	// ---------------------------------------------------------------------------

	/** steam vs wood barrel: 373 K >= the 340 K ceiling — the MELT fires, the gas gate is unreached (fizz count 0). */
	@Test
	public void steamOnWoodBarrelMeltsBeforeTheGasGate() {
		WoodFizzProbe tBe = sWoodType.create(POS, Blocks.STONE.defaultBlockState());
		tBe.mMeltingPoint = WOOD_MELTING_POINT; // the wood row, :2136
		tBe.overrideName = "steam"; // the gates see steam; the offline temperature table says 373 K
		tBe.mTank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);

		tBe.onTick(1, true);

		assertEquals(1, tBe.meltdownCount, "the :162 melt judgment fired exactly once (373 >= 340)");
		assertEquals(0, tBe.fizzCount, "the :180 gas gate was never reached — melt returns first (upstream chain order)");
		assertEquals(0, tBe.mTank.amount(), "the tank is voided by the meltdown trash");
		assertNull(tBe.mTank.getFluid());
	}

	/** steam vs metal drum: gasProof skips the :180 gas gate, the :184 allowFluid gate voids — trash ONCE, not twice. */
	@Test
	public void steamOnMetalDrumFizzesOnceThroughTheAllowFluidGate() {
		MetalFizzProbe tBe = sMetalType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(Long.MAX_VALUE, tBe.mMeltingPoint, "the offline fixture never melts — the pure fizz pair is isolated");
		assertTrue(tBe.gasProof(), "the metal class is gas-proof (the :2151-2170 rows)");
		tBe.overrideName = "steam"; // POWER_CONDUCTING: the allowFluid gate must fire; 373 K < MAX: no melt
		tBe.mTank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);

		tBe.onTick(1, true);

		assertEquals(0, tBe.meltdownCount, "nothing melts — 373 K against MAX_VALUE");
		assertEquals(1, tBe.fizzCount, "the :184 allowFluid gate trash ran ONCE (the gas gate was skipped, not stacked)");
		assertEquals(0, tBe.mTank.amount(), "the steam is voided (the GarbageGT.trash half)");
	}

	/** water vs metal drum: both gates pass, zero consumption — the water-regression semantics. */
	@Test
	public void waterOnMetalDrumPassesBothGatesUntouched() {
		MetalFizzProbe tBe = sMetalType.create(POS, Blocks.STONE.defaultBlockState());
		tBe.mTank.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);

		tBe.onTick(1, true);

		assertEquals(0, tBe.meltdownCount);
		assertEquals(0, tBe.fizzCount, "water is in neither list — no melt, no fizz");
		assertEquals(100, tBe.mTank.amount(), "zero consumption — the content survives the tick");
	}

	/**
	 * The :180 gas gate in isolation: a NON-gas-proof barrel whose ceiling never melts
	 * (the port default MAX_VALUE — upstream only melts through the explicit
	 * NBT_CAPACITY_HU row) still voids steam through the gas gate alone. A wood barrel
	 * with its real 340 K row can NEVER show this path — 373 K melts first, which is the
	 * upstream semantics the melt-first test pins.
	 */
	@Test
	public void steamFizzesThroughTheGasGateWithoutMelting() {
		WoodFizzProbe tBe = sWoodType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(Long.MAX_VALUE, tBe.mMeltingPoint, "no NBT_CAPACITY_HU — the default ceiling never melts");
		assertFalse(tBe.gasProof(), "the wood family is NOT gas-proof (the :2136-2149 rows)");
		tBe.overrideName = "steam";
		tBe.mTank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);

		tBe.onTick(1, true);

		assertEquals(0, tBe.meltdownCount);
		assertEquals(1, tBe.fizzCount, "the :180 gas gate fired — !gasProof && isGas(steam)");
		assertEquals(0, tBe.mTank.amount());
	}

	/** natural gas: fizz only WITHOUT gas-proof; a gas-proof barrel holds it (natural_gas is GAS but not POWER_CONDUCTING). */
	@Test
	public void naturalGasFizzesWithoutGasProofAndSurvivesWithIt() {
		WoodFizzProbe tWood = sWoodType.create(POS, Blocks.STONE.defaultBlockState());
		tWood.overrideName = "natural_gas";
		tWood.mTank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
		tWood.onTick(1, true);
		assertEquals(1, tWood.fizzCount, "the gas gate reads the GAS list, not just steam");
		assertEquals(0, tWood.mTank.amount());

		MetalFizzProbe tMetal = sMetalType.create(POS, Blocks.STONE.defaultBlockState());
		tMetal.overrideName = "natural_gas";
		tMetal.mTank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
		tMetal.onTick(1, true);
		assertEquals(0, tMetal.fizzCount, "gas-proof exempts the gas gate; natural gas is not power-conducting");
		assertEquals(500, tMetal.mTank.amount(), "a gas-proof barrel holds natural gas");
	}

	// ---------------------------------------------------------------------------
	// the four-family gasProof values (offline class-truth fallback; the live carrier
	// values are the RCON chain's stat arms)
	// ---------------------------------------------------------------------------

	/** gasProof four-family assertion: wood F / plastic T / metal T / logistics T. */
	@Test
	public void gasProofCarriesTheFourFamilyRowValues() {
		assertFalse(sWoodType.create(POS, Blocks.STONE.defaultBlockState()).gasProof(),
				"the wood rows carry NBT_GASPROOF=F (Loader_MultiTileEntities.java:2136-2149)");
		assertTrue(sPlasticType.create(POS, Blocks.STONE.defaultBlockState()).gasProof(),
				"the plastic row carries NBT_GASPROOF=T (:2150)");
		assertTrue(sMetalType.create(POS, Blocks.STONE.defaultBlockState()).gasProof(),
				"every metal drum row carries NBT_GASPROOF=T (:2151-2170)");
		assertTrue(sLogisticsType.create(POS, Blocks.STONE.defaultBlockState()).gasProof(),
				"the logistics row carries NBT_GASPROOF=T (:2171)");
	}

	/**
	 * The base default is F — the upstream mGasProof field counterpart (the wood family
	 * rides it unchanged).
	 */
	@Test
	public void gasProofOverridePointDefaultsFalse() {
		WoodFizzProbe tBe = sWoodType.create(POS, Blocks.STONE.defaultBlockState());
		assertFalse(tBe.gasProof(), "the base default F is the wood-family truth");
		assertTrue(tBe.allowFluid("water"), "the port allowFluid: plain water is allowed");
		assertFalse(tBe.allowFluid("steam"), "the port allowFluid: power-conductor fluids are refused (UT.java:187)");
		assertTrue(tBe.allowFluid("natural_gas"), "natural gas is not power-conducting — allowed");
	}

	// ---------------------------------------------------------------------------
	// the item-face fill gate pair (upstream :250 allowFluid first, then :251 gas;
	// the drain face is gate-free — the pre-card stock clean-up semantics)
	// ---------------------------------------------------------------------------

	/** The item-face fixture: a rename seam over the real handler (the gasProof flag rides the real fluent arm). */
	static final class NamedItemHandler extends GTBarrelItemFluidHandler {
		String overrideName = null;

		NamedItemHandler(boolean aGasProof) {
			super(new ItemStack(Items.GLASS_BOTTLE), 16000);
			setGasProof(aGasProof);
		}

		@Override
		protected String fluidName(FluidStack aFluid) {
			return overrideName != null ? overrideName : super.fluidName(aFluid);
		}

		/** The "steam" fill probe: the gates see the renamed stack, the real vanilla water keeps the fill legal offline. */
		int runSteamFillProbe() {
			overrideName = "steam";
			return fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
		}
	}

	/** steam fills NOTHING on any barrel: refused on the gas-proof drums AND the plain wood barrel alike. */
	@Test
	public void itemFillRefusesSteamOnGasProofAndPlainBarrelsAlike() {
		assertEquals(0, new NamedItemHandler(false).runSteamFillProbe(),
				"wood (gasProof F): the :251 gas gate alone refuses steam");
		assertEquals(0, new NamedItemHandler(true).runSteamFillProbe(),
				"metal/plastic/logistics (gasProof T): the :250 allowFluid gate STILL refuses steam — the doors are independent");
	}

	/** natural gas fills only into the gas-proof container (the :251 gas gate, not the list gate). */
	@Test
	public void itemFillNaturalGasNeedsGasProof() {
		NamedItemHandler tWood = new NamedItemHandler(false);
		tWood.overrideName = "natural_gas";
		assertEquals(0, tWood.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE),
				"the :251 gas gate refuses natural gas without gas-proof");

		NamedItemHandler tMetal = new NamedItemHandler(true);
		tMetal.overrideName = "natural_gas";
		assertEquals(100, tMetal.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE),
				"a gas-proof item container takes natural gas (it is not power-conducting)");
	}

	/** water fills normally through both gate orders, and the drain face is gate-free (pre-card stock drains out). */
	@Test
	public void itemFillWaterNormalAndDrainFaceIsGateFree() {
		NamedItemHandler tHandler = new NamedItemHandler(true);
		assertEquals(100, tHandler.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE),
				"water passes the :250 list gate and the :251 gas gate");

		// the pre-card stock clean-up: a tank already holding "steam" (the name the gates see)
		// still drains — the drain face consults no list
		tHandler.overrideName = "steam";
		FluidStack tDrained = tHandler.drain(1000, FluidAction.EXECUTE);
		assertEquals(100, tDrained.getAmount(), "the legacy stock drains out in full");
		assertEquals(0, tHandler.getFluidInTank(0).getAmount());
	}
}
