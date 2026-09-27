package gregtech6.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Task debt-emitter-sensor-generators — the offline census of the three technological
 * component families: the {@link GT6Emitters#ROWS} 30-row ladder against the upstream
 * registration loop (MultiItemTechnological.java:54-56 — {@code for (int i = 0; i < 10;
 * i++)}, ids 12100+i / 12120+i / 12140+i over {@code VN[0..9]}), plus the HV-rung
 * identity pins the Molecular Scanner T3 seam restore (the usb card's Q1 ruling:
 * {@code F/X/Y} = FIELD_GENERATORS[3]/EMITTERS[3]/SENSORS[3]) will consume at rebase
 * time. The offline boot before the registry-class touch (the
 * CreativeTabJoinCensusTest.boot shape — DeferredRegister.create walks
 * ForgeRegistries.Keys, which needs the bootstrapped vanilla registries).
 */
public class GT6EmittersTest {

	/** The offline boot (SharedConstants + Bootstrap — the CreativeTabJoinCensusTest shape). */
	@BeforeAll
	static void boot() {
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	/** The 30 rows: three families x ten rungs. */
	@Test
	public void thirtyRowsInThreeTenRungFamilies() {
		assertEquals(30, GT6Emitters.ROWS.size());
		assertEquals(30, GT6Emitters.ITEMS_BY_PATH.size());
		for (String tFamily : new String[]{GT6Emitters.FAMILY_FIELD_GENERATORS, GT6Emitters.FAMILY_EMITTERS, GT6Emitters.FAMILY_SENSORS}) {
			long tCount = GT6Emitters.ROWS.stream().filter(r -> r.family().equals(tFamily)).count();
			assertEquals(10, tCount, tFamily + " carries ten rungs");
		}
	}

	/** The upstream id ledger: 12100+i / 12120+i / 12140+i, the :54-56 addItem columns. */
	@Test
	public void upstreamIdLedgerMatchesTheRegistrationLoop() {
		for (GT6Emitters.ComponentRow tRow : GT6Emitters.ROWS) {
			int tExpected = switch (tRow.family()) {
				case GT6Emitters.FAMILY_FIELD_GENERATORS -> 12100 + tRow.tier(); // :54
				case GT6Emitters.FAMILY_EMITTERS -> 12120 + tRow.tier();        // :55
				default -> 12140 + tRow.tier();                                 // :56
			};
			assertEquals(tExpected, tRow.upstreamId(), tRow.path());
		}
	}

	/** The path ladder: every family over the ten VN[0..9] tier tokens, unique paths. */
	@Test
	public void pathLadderCoversTheTenTierTokens() {
		Set<String> tPaths = new HashSet<>();
		for (GT6Emitters.ComponentRow tRow : GT6Emitters.ROWS) {
			assertEquals(tRow.family() + "_" + GT6Emitters.TIER_TOKENS[tRow.tier()], tRow.path());
			assertTrue(tPaths.add(tRow.path()), "duplicate path " + tRow.path());
		}
		assertEquals(30, tPaths.size());
	}

	/**
	 * The Scanner T3 seam feed — the usb card's Q1 ruling: the {@code :1551} row's
	 * F/X/Y columns are FIELD_GENERATORS[3]/EMITTERS[3]/SENSORS[3], the HV rungs
	 * ({@code VN[3]}). The seam restore (deferred to this branch's rebase over the
	 * merged usb branch — see the GT6Emitters javadoc) consumes exactly these three
	 * registry paths.
	 */
	@Test
	public void theScannerT3ColumnsAreTheHvRungs() {
		assertEquals("field_generator_hv", GT6Emitters.FAMILY_FIELD_GENERATORS + "_" + GT6Emitters.TIER_TOKENS[3]);
		assertEquals("signal_emitter_hv", GT6Emitters.FAMILY_EMITTERS + "_" + GT6Emitters.TIER_TOKENS[3]);
		assertEquals("sensor_hv", GT6Emitters.FAMILY_SENSORS + "_" + GT6Emitters.TIER_TOKENS[3]);
		assertTrue(GT6Emitters.ITEMS_BY_PATH.containsKey("field_generator_hv"), "the 'F' column of the :1551 row");
		assertTrue(GT6Emitters.ITEMS_BY_PATH.containsKey("signal_emitter_hv"), "the 'X' column of the :1551 row");
		assertTrue(GT6Emitters.ITEMS_BY_PATH.containsKey("sensor_hv"), "the 'Y' column of the :1551 row");
		// the upstream tooltip column is the EMPTY string on all 30 rows — no tooltip keys exist
		assertFalse(GT6Emitters.ITEMS_BY_PATH.containsKey("sensor_hv.tooltip"));
	}
}
