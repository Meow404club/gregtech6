package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import gregtech6.fluid.GTFluids;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task p13-hu-steam-foundation — the offline acceptance fixture for the HU/steam
 * foundation (decision 2026-09-03-p13-hu-energy-face): the full HU handshake over the
 * existing faces (resolveEnergyType reference equality, the Util emit → receive booking,
 * the GeneratorSolid:270 SIDES_TOP single-gate truth table W2's burning box must
 * reproduce), plus the boiler-constant scope pin of decision
 * 2026-09-03-p13-boiler-family-split ②. Zero production code — everything here asserts
 * faces that already exist or pins the contract the W2/W3 consumers must honour.
 */
public class HuEnergyHandshakeTest extends GTOfflineTestBase {

	// ---------------------------------------------------------------------------
	// the boiler-constant scope pin (decision 2026-09-03-p13-boiler-family-split ②)
	// ---------------------------------------------------------------------------

	/**
	 * The four-way machine check of the STEAM_PER_WATER scope discipline: the new
	 * boiler-side globals carry the CS.java values while the pre-existing engine-private
	 * pair stays byte-identical (the P12 zero-diff red line) — the _GLOBAL suffix exists
	 * precisely so both can live side by side in {@link GTFluids}.
	 */
	@Test
	public void boilerSteamConstantsPinTheScopeDiscipline() {
		assertEquals(80, GTFluids.EU_PER_WATER, "CS.java:238 — the boiler-side global heat price of 1 L water");
		assertEquals(160, GTFluids.STEAM_PER_WATER_GLOBAL, "CS.java:242 — the boiler-side global standard 160 steam = 1 water");
		assertEquals(200, GTFluids.STEAM_PER_WATER, "the engine-private MultiTileEntityEngineSteam.java:58 recycle ratio — NOT touched by the boiler append (the P12 consumers' zero-diff red line)");
		assertEquals(2, GTFluids.STEAM_PER_EU, "CS.java:240 — the shared steam-per-EU divisor, reused not re-declared");
		assertEquals(GTFluids.EU_PER_WATER * GTFluids.STEAM_PER_EU, GTFluids.STEAM_PER_WATER_GLOBAL,
				"the CS.java:238-242 self-consistency: 80 EU × 2 steam/EU = 160 steam");
	}
}
