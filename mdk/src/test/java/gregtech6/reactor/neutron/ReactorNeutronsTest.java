package gregtech6.reactor.neutron;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The neutron/heat/durability arithmetic pins (task debt-reactor-a-neutron-core):
 * every {@link ReactorNeutrons} function against values hand-recomputed from the
 * upstream lines cited in its javadoc — RodNuclear.java:175-224 (emission, maximum,
 * durability), RodBreeder.java:82-93 (breeding), Core2x2.java:102-134/108-116
 * (heat divider, ambient radiation).
 */
public class ReactorNeutronsTest {

	private static final FuelRodSpec U235 = FuelRodSpec.byId(9221).orElseThrow();
	private static final FuelRodSpec CO60 = FuelRodSpec.byId(9250).orElseThrow();
	private static final FuelRodSpec NAQDRIA = FuelRodSpec.byId(9261).orElseThrow();

	@Test
	public void cadenceConstantsMatchCore2x2TickGates() {
		// Core2x2.java:54 exchange at %20==19, :224 rollback at ==18, :111 radiation at ==10,
		// :202 meltdown doubles the burst level
		assertEquals(19, ReactorNeutrons.EXCHANGE_TICK);
		assertEquals(18, ReactorNeutrons.NEUTRON_ROLLBACK_TICK);
		assertEquals(10, ReactorNeutrons.RADIATION_BURST_TICK);
		assertEquals(2, ReactorNeutrons.MELTDOWN_STRENGTH_MULTIPLIER);
	}

	@Test
	public void fuelEmissionFormulaPerCoolant() {
		// Nuclear.java:197-199: emission = other + divup(max(oNeutrons - self, 0), div);
		// U-235 base (self 32, other 32, div 4) with oNeutrons=128 on the slot:
		// plain water family 32+divup(96,4)=56; IC2 (128,128,8) -> 128+0=128;
		// CO2 (96,32,4) -> 32+8=40; He (32,16,4) -> 16+24=40; LiCl (160,16,4) -> 16+0=16
		// (negative self-excess clamps to 0); ThSalt (0,16,3) -> 16+divup(128,3)=16+43=59;
		// Sn/Na (32,32,3) -> 32+32=64
		assertEmission(ReactorCoolant.DISTILLED_WATER, 128, 56, 32);
		assertEmission(ReactorCoolant.IC2_COOLANT, 128, 128, 128);
		assertEmission(ReactorCoolant.CARBON_DIOXIDE, 128, 40, 96);
		assertEmission(ReactorCoolant.HELIUM, 128, 40, 32);
		assertEmission(ReactorCoolant.MOLTEN_LICL, 128, 16, 160);
		assertEmission(ReactorCoolant.THORIUM_SALT, 128, 59, 0);
		assertEmission(ReactorCoolant.MOLTEN_TIN, 128, 64, 32);
		assertEmission(ReactorCoolant.MOLTEN_SODIUM, 128, 64, 32);
		// IC2 with 256 on the slot: 128 + divup(128,8) = 144
		assertEmission(ReactorCoolant.IC2_COOLANT, 256, 144, 128);
	}

	@Test
	public void cobalt60SelfOnlyEmission() {
		// Co-60 (self 8, other 0, div 16), LME:760 — plain water: oN=8 -> 0+divup(0,16)=0;
		// oN=100 -> 0+divup(92,16)=0+6=6
		assertEmissionFuel(CO60, ReactorCoolant.DISTILLED_WATER, 8, 0, 8);
		assertEmissionFuel(CO60, ReactorCoolant.DISTILLED_WATER, 100, 6, 8);
	}

	@Test
	public void neutronMaximumPerCoolant() {
		// Nuclear.java:240-252 hand-computed on U-235 (max 2048) and Naquadria (max 16384)
		assertEquals(2048, ReactorNeutrons.neutronMaximum(U235, ReactorCoolant.DISTILLED_WATER));
		assertEquals(2048, ReactorNeutrons.neutronMaximum(U235, ReactorCoolant.IC2_COOLANT));
		assertEquals(2560, ReactorNeutrons.neutronMaximum(U235, ReactorCoolant.MOLTEN_LICL));
		assertEquals(8192, ReactorNeutrons.neutronMaximum(U235, ReactorCoolant.THORIUM_SALT));
		assertEquals(256, ReactorNeutrons.neutronMaximum(U235, ReactorCoolant.HEAVY_WATER));
		assertEquals(128, ReactorNeutrons.neutronMaximum(U235, ReactorCoolant.TRITIATED_WATER));
		assertEquals(2048, ReactorNeutrons.neutronMaximum(NAQDRIA, ReactorCoolant.HEAVY_WATER));
		assertEquals(1024, ReactorNeutrons.neutronMaximum(NAQDRIA, ReactorCoolant.TRITIATED_WATER));
	}

	@Test
	public void durabilityLossLadder() {
		// Nuclear.java:215-216: <= max -> flat 100/t (even at 0 neutrons); over ->
		// divup(400*n, max); moderated x4. U-235 plain max 2048:
		// n=0 -> 100; n=2048 -> 100; n=2049 -> divup(819600,2048)=401; n=2048 moderated -> 400.
		// T2O max 128: n=129 -> divup(51600,128)=404, moderated -> 1616.
		// ThSalt max 8192: n=8192 -> 100.
		assertEquals(100, ReactorNeutrons.durabilityLoss(0, 2048, false));
		assertEquals(100, ReactorNeutrons.durabilityLoss(2048, 2048, false));
		assertEquals(401, ReactorNeutrons.durabilityLoss(2049, 2048, false));
		assertEquals(400, ReactorNeutrons.durabilityLoss(2048, 2048, true));
		assertEquals(404, ReactorNeutrons.durabilityLoss(129, 128, false));
		assertEquals(1616, ReactorNeutrons.durabilityLoss(129, 128, true));
		assertEquals(100, ReactorNeutrons.durabilityLoss(8192, 8192, false));
	}

	@Test
	public void durabilityClampsToMinusOneSentinelThenDepletes() {
		// Nuclear.java:217/220-224: loss > remaining -> -1 sentinel; depletion swap fires at <= 0
		assertEquals(900, ReactorNeutrons.durabilityAfter(1000, 100));
		assertEquals(-1, ReactorNeutrons.durabilityAfter(100, 101));
		assertEquals(-1, ReactorNeutrons.durabilityAfter(100, 400));
		assertTrue(ReactorNeutrons.durabilityAfter(100, 101) <= 0, "sentinel must trigger depletion");
		assertTrue(ReactorNeutrons.durabilityAfter(100, 100) == 0, "exact zero also depletes");
	}

	@Test
	public void breederTickDrainsBudgetAndSwapsAtZero() {
		// RodBreeder.java:84-90 on the Lithium row (needed 640000, LME:784):
		// 250/t: 640000-250=639750 alive; reaching exactly 0 -> product, durability reset to 0
		ReactorNeutrons.BreederTick tTick = ReactorNeutrons.breederTick(640_000, 250);
		assertEquals(639_750, tTick.durability());
		assertFalse(tTick.becameProduct());
		tTick = ReactorNeutrons.breederTick(250, 250);
		assertEquals(0, tTick.durability());
		assertTrue(tTick.becameProduct());
		tTick = ReactorNeutrons.breederTick(1, 249);
		assertEquals(0, tTick.durability());
		assertTrue(tTick.becameProduct());
	}

	@Test
	public void heatDividerTickAppliesToGenerationOnly() {
		// Core2x2.java:122-134: stored 1000, generated 100 ->
		// plain (1100, 100); Na /6 divup(100,6)=17 -> (1017, 17); Sn /3 divup(100,3)=34 -> (1034, 34);
		// zero generation passes through untouched
		assertHeatTick(ReactorCoolant.DISTILLED_WATER, 1000, 100, 1100, 100);
		assertHeatTick(ReactorCoolant.MOLTEN_SODIUM, 1000, 100, 1017, 17);
		assertHeatTick(ReactorCoolant.MOLTEN_TIN, 1000, 100, 1034, 34);
		assertHeatTick(ReactorCoolant.MOLTEN_SODIUM, 1000, 0, 1000, 0);
	}

	@Test
	public void coolantConversionArithmetic() {
		// Core2x2.java:139 etc. — litres = mEnergy / EU_PER (floor); 1000 HU stored:
		// IC2/20 -> 50, water/80 -> 12, Sn/40 -> 25, Na/30 -> 33, HDO/40 -> 25, D2O/50 -> 20,
		// T2O/60 -> 16, LiCl/15 -> 66, CO2/20 -> 50, He/30 -> 33, ThSalt/2560000 -> 0
		assertEquals(50, ReactorNeutrons.convertibleLiters(1000, ReactorCoolant.IC2_COOLANT));
		assertEquals(12, ReactorNeutrons.convertibleLiters(1000, ReactorCoolant.DISTILLED_WATER));
		assertEquals(25, ReactorNeutrons.convertibleLiters(1000, ReactorCoolant.MOLTEN_TIN));
		assertEquals(33, ReactorNeutrons.convertibleLiters(1000, ReactorCoolant.MOLTEN_SODIUM));
		assertEquals(25, ReactorNeutrons.convertibleLiters(1000, ReactorCoolant.SEMIHEAVY_WATER));
		assertEquals(20, ReactorNeutrons.convertibleLiters(1000, ReactorCoolant.HEAVY_WATER));
		assertEquals(16, ReactorNeutrons.convertibleLiters(1000, ReactorCoolant.TRITIATED_WATER));
		assertEquals(66, ReactorNeutrons.convertibleLiters(1000, ReactorCoolant.MOLTEN_LICL));
		assertEquals(50, ReactorNeutrons.convertibleLiters(1000, ReactorCoolant.CARBON_DIOXIDE));
		assertEquals(33, ReactorNeutrons.convertibleLiters(1000, ReactorCoolant.HELIUM));
		assertEquals(0, ReactorNeutrons.convertibleLiters(1000, ReactorCoolant.THORIUM_SALT));
		// Core2x2.java:141 etc. — IC2 1000 HU minus 50 L × 20 = 0; ThSalt 5120000 minus 2 L × 2560000 = 0
		assertEquals(0, ReactorNeutrons.storedAfterCoolant(1000, ReactorCoolant.IC2_COOLANT, 50));
		assertEquals(0, ReactorNeutrons.storedAfterCoolant(5_120_000, ReactorCoolant.THORIUM_SALT, 2));
		// Core2x2.java:145 — water 12 L makes 12 × 160 = 1920 steam; others pass litres through
		assertEquals(1920, ReactorNeutrons.hotOutputAmount(12, ReactorCoolant.DISTILLED_WATER));
		assertEquals(50, ReactorNeutrons.hotOutputAmount(50, ReactorCoolant.IC2_COOLANT));
	}

	@Test
	public void ambientRadiationArithmetic() {
		// Core2x2.java:108 — tCalc = divup(sum(oN), 256): 0 -> 0 (mRunning false),
		// 255 -> 1, 256 -> 1, 257 -> 2
		assertEquals(0, ReactorNeutrons.ambientNeutronLevel(0, 0, 0, 0));
		assertEquals(1, ReactorNeutrons.ambientNeutronLevel(255, 0, 0, 0));
		assertEquals(1, ReactorNeutrons.ambientNeutronLevel(64, 64, 64, 64));
		assertEquals(2, ReactorNeutrons.ambientNeutronLevel(257, 0, 0, 0));
		// Core2x2.java:115 — (long)(tCalc - distance) truncates toward zero:
		// 10-3.75 -> 6, 10-10.5 -> 0 (not -1), 10-12.7 -> -2
		assertEquals(6, ReactorNeutrons.ambientStrength(10, 3.75));
		assertEquals(0, ReactorNeutrons.ambientStrength(10, 10.5));
		assertEquals(-2, ReactorNeutrons.ambientStrength(10, 12.7));
		// Core2x2.java:116 — amplifier = divup(strength, 10)
		assertEquals(1, ReactorNeutrons.radiationAmplifier(6));
		assertEquals(1, ReactorNeutrons.radiationAmplifier(10));
		assertEquals(2, ReactorNeutrons.radiationAmplifier(20));
		assertEquals(0, ReactorNeutrons.radiationAmplifier(0));
	}

	private static void assertEmission(ReactorCoolant aCoolant, int aOnRod, int aEmitted, int aSelfAdded) {
		assertEmissionFuel(U235, aCoolant, aOnRod, aEmitted, aSelfAdded);
	}

	private static void assertEmissionFuel(FuelRodSpec aFuel, ReactorCoolant aCoolant, int aOnRod, int aEmitted, int aSelfAdded) {
		ReactorNeutrons.FuelEmission tEmission = ReactorNeutrons.fuelEmission(aFuel, aCoolant, aOnRod);
		assertEquals(aEmitted, tEmission.emitted(), aFuel.name() + "/" + aCoolant.name() + " emission");
		assertEquals(aSelfAdded, tEmission.selfAdded(), aFuel.name() + "/" + aCoolant.name() + " self");
	}

	private static void assertHeatTick(ReactorCoolant aCoolant, long aStored, long aGenerated, long aStoredAfter, long aOutput) {
		ReactorNeutrons.HeatTick tTick = ReactorNeutrons.heatDividerTick(aStored, aGenerated, aCoolant);
		assertEquals(aStoredAfter, tTick.stored(), aCoolant.name() + " stored");
		assertEquals(aOutput, tTick.outputThisTick(), aCoolant.name() + " output");
	}
}
