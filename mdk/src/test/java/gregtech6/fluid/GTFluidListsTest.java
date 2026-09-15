package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Task p13-steam-proof-repay acceptance ① — the name-list truth table, offline. The
 * seeds are the task-card literals: steam in BOTH lists (FL.java:85), natural_gas in
 * GAS only (the port registry path), water in neither.
 */
public class GTFluidListsTest {

	@Test
	public void steamIsInBothLists() {
		assertTrue(GTFluidLists.GAS.contains("steam"), "FL.java:85 — Steam registers into GAS");
		assertTrue(GTFluidLists.POWER_CONDUCTING.contains("steam"), "FL.java:85 — Steam registers into POWER_CONDUCTING");
		assertTrue(GTFluidLists.isGas("steam"));
		assertTrue(GTFluidLists.isPowerConducting("steam"), "the :184 allowFluid void + :250 item-fill refuse key");
	}

	@Test
	public void naturalGasIsGasOnly() {
		assertTrue(GTFluidLists.isGas("natural_gas"), "the port registry path (GTFluids NATURAL_GAS) seeds GAS");
		assertFalse(GTFluidLists.isPowerConducting("natural_gas"),
				"FL.java:410 Gas_Natural carries GAS only — a gas-proof barrel holds natural gas");
	}

	@Test
	public void waterIsInNeitherList() {
		assertFalse(GTFluidLists.isGas("water"), "water is a plain liquid");
		assertFalse(GTFluidLists.isPowerConducting("water"), "water never voids a barrel");
	}

	@Test
	public void ic2CompatNamesAreNotSeeded() {
		assertFalse(GTFluidLists.isGas("ic2steam"), "FL.java:86 — the IC2 compat name has no port fluid, pool");
		assertFalse(GTFluidLists.isPowerConducting("ic2superheatedsteam"), "FL.java:87 — same pool");
		assertFalse(GTFluidLists.isPowerConducting("gas_natural_gas"), "the upstream FL.java:410 spelling is not a port registry path");
	}

	@Test
	public void unknownAndNullNamesAreNeither() {
		assertFalse(GTFluidLists.isGas("lava"));
		assertFalse(GTFluidLists.isGas(null));
		assertFalse(GTFluidLists.isGas(""));
		assertFalse(GTFluidLists.isPowerConducting("distilled_water"));
		assertFalse(GTFluidLists.isPowerConducting(null));
	}

	/** The register API adds once and is idempotent — proven on a scratch set so the global seeds stay untouched for the other assertions. */
	@Test
	public void registerApiAddsIntoEverySetAndIsIdempotent() {
		Set<String> tFirst = new HashSet<>();
		Set<String> tSecond = new HashSet<>();
		GTFluidLists.register("test_only_scratch", tFirst, tSecond); // the FL.java:574 varargs add loop
		GTFluidLists.register("test_only_scratch", tFirst, tSecond);
		GTFluidLists.register("test_only_scratch", tFirst);
		assertEquals(1, tFirst.size(), "a repeat registration is absorbed by set semantics");
		assertEquals(1, tSecond.size());
		assertTrue(tFirst.contains("test_only_scratch"));
		assertTrue(tSecond.contains("test_only_scratch"));

		// the seeds themselves re-register cleanly (idempotent over the global lists too)
		int tGas = GTFluidLists.GAS.size();
		int tPc = GTFluidLists.POWER_CONDUCTING.size();
		GTFluidLists.register("steam", GTFluidLists.GAS, GTFluidLists.POWER_CONDUCTING);
		assertEquals(tGas, GTFluidLists.GAS.size(), "re-seeding steam adds nothing");
		assertEquals(tPc, GTFluidLists.POWER_CONDUCTING.size());
	}

	/** The seed census: the task-card literals + the card-④ hot-family POWER_CONDUCTING rows (FL.java:90+:95-102, NINE — the review-round correction), no hidden extras. */
	@Test
	public void seedCensusMatchesTheCardLiterals() {
		// THIS class stays a pure-JVM seat (GTFluidLists carries no vanilla statics; a
		// GTFluids class-init here would race the vanilla bootstrap) — the hot-family
		// POWER_CONDUCTING seed census (the FL.java:90+:95-102 nine rows + the three
		// unseeded :89/:93/:105 faces) lives in GTFluidsHotFamilyTest, the bootstrapped
		// seat that owns the GTFluids static block.
		// the p30-pool-gas-seeds-13 reconciliation: the eighteen gaseous chemical seeds join
		// the two originals (the per-row upstream anchors live in the GTFluidLists static
		// block; the CHEMICAL_SPECS-side invariant in GTFluidsChemicalFamilyTest)
		assertEquals(Set.of("steam", "natural_gas",
				"hydrogen", "nitrogen", "oxygen", "fluorine",
				"helium", "neon", "argon", "krypton", "xenon", "radon",
				"methane", "carbondioxide", "carbonmonoxide",
				"propane", "butane", "propylene", "ethylene",
				"chlorine"),
			GTFluidLists.GAS, "GAS = steam + natural_gas + the 18 gaseous chemical seeds");
		assertTrue(GTFluidLists.POWER_CONDUCTING.contains("steam"), "the original seed rides");
	}
}
