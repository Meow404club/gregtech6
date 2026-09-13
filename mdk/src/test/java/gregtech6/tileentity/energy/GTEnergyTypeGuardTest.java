package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.jade.GT6MachineProvider;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The P29 W2 energy-type census guard (task p29-w2-energy-types-5tier ③) — an
 * ASSERTION card, not a definition card (decisions.p29-w2-split-rulings): the four
 * exotic types ALREADY live in the root gregapi (TD.java:109/:116/:123/:144 —
 * CRYO/LIGHT/MAGNETIC/TIME), the source-block dial
 * ({@link GTEnergySourceBlockEntity#resolveEnergyType}, :293-308) and the Jade short
 * codes ({@link GT6MachineProvider#ENERGY_SHORT_CODES}, :84) are both already wired.
 * This test pins the shared-instance identity so a future regression cannot mint
 * lookalike types: the upstream :501 machine gate is REFERENCE equality
 * ({@code aEnergyType == mEnergyTypeAccepted}), so two instances of the "same" type
 * would silently never meet.
 *
 * <p>Coverage: ① the TD.Energy alias pairs are the SAME object (CRYO==CU, LIGHT==LU,
 * MAGNETIC==MU, TIME==TU); ② the short-name dial round-trips to that one instance for
 * all four W2 types (plus the EU/RU/KU/HU carriers as the regression face); ③ the
 * registered mName form resolves through the TAGS walk to the same instance; ④ the
 * four instances are pairwise DISTINCT (a type gate cannot false-accept its sibling);
 * ⑤ an unknown name returns null and mints NOTHING (the TagData.TAGS census is
 * unchanged — the no-rogue-mint contract of the resolveEnergyType doc); ⑥ the Jade
 * short codes map all four to their upstream aLocalShort verbatim words.
 */
public class GTEnergyTypeGuardTest extends GTOfflineTestBase {

	/** The four W2 types as {short name, mName suffix} — the dial vocabulary this card guards. */
	private static final String[][] W2_TYPES = {
		{"MU", "MAGNETIC"},
		{"LU", "LIGHT"},
		{"CU", "CRYO"},
		{"TU", "TIME"}
	};

	@BeforeAll
	static void boot() {
		// TagData.TAGS + the TD.Energy constants are static state; a light touch of the
		// material universe keeps the TD class loading deterministic across test orders
		// (the GTMaterialItemsBoot offline convention, no vanilla registry needed here).
		gregtech6.registry.GTMaterialItems.initMaterials();
		// force the TD class load BEFORE any TAGS census: TD's <clinit> mints its TagData
		// constants into TagData.TAGS mid-flight, and a lazy load inside a TAGS stream walk
		// would CME (the first test method to run measured the census while TD was still
		// unloaded). One dial round-trip walks the loaded set and settles it.
		GTEnergySourceBlockEntity.resolveEnergyType("TU");
	}

	/** ① the TD alias pairs are shared singletons (the root TD.java:109/:116/:123/:144 shape). */
	@Test
	void tdEnergyAliasesAreSharedInstances() {
		assertSame(TD.Energy.CRYO, TD.Energy.CU, "TD.java:109 — CRYO and CU are ONE TagData");
		assertSame(TD.Energy.LIGHT, TD.Energy.LU, "TD.java:116 — LIGHT and LU are ONE TagData");
		assertSame(TD.Energy.MAGNETIC, TD.Energy.MU, "TD.java:123 — MAGNETIC and MU are ONE TagData");
		assertSame(TD.Energy.TIME, TD.Energy.TU, "TD.java:144 — TIME and TU are ONE TagData");
	}

	/** ② the short-name dial round-trips to the same shared instance (the :501 reference-equality contract). */
	@Test
	void resolveEnergyTypeRoundTripsTheW2ShortNames() {
		TagData[] tFields = {TD.Energy.MU, TD.Energy.LU, TD.Energy.CU, TD.Energy.TU};
		for (int i = 0; i < W2_TYPES.length; i++) {
			TagData tResolved = GTEnergySourceBlockEntity.resolveEnergyType(W2_TYPES[i][0]);
			assertNotNull(tResolved, W2_TYPES[i][0] + " resolves");
			assertSame(tFields[i], tResolved,
					W2_TYPES[i][0] + " resolves to the SHARED TD.Energy instance (the :501 gate is reference equality)");
			assertSame(tResolved, GTEnergySourceBlockEntity.resolveEnergyType(W2_TYPES[i][0].toLowerCase()),
					W2_TYPES[i][0] + " resolves case-insensitively to the SAME instance");
			// the registered mName form walks the TAGS loop to the same instance
			assertSame(tResolved, GTEnergySourceBlockEntity.resolveEnergyType("ENERGY." + W2_TYPES[i][1]),
					"ENERGY." + W2_TYPES[i][1] + " resolves to the same instance");
		}
		// the four established carriers stay put (the regression face)
		assertSame(TD.Energy.EU, GTEnergySourceBlockEntity.resolveEnergyType("EU"));
		assertSame(TD.Energy.RU, GTEnergySourceBlockEntity.resolveEnergyType("RU"));
		assertSame(TD.Energy.KU, GTEnergySourceBlockEntity.resolveEnergyType("KU"));
		assertSame(TD.Energy.HU, GTEnergySourceBlockEntity.resolveEnergyType("HU"));
	}

	/** ④ the four W2 instances are pairwise distinct — a type gate cannot false-accept a sibling. */
	@Test
	void theFourW2TypesArePairwiseDistinct() {
		assertNotEquals(TD.Energy.MU, TD.Energy.LU, "MU != LU");
		assertNotEquals(TD.Energy.MU, TD.Energy.CU, "MU != CU");
		assertNotEquals(TD.Energy.MU, TD.Energy.TU, "MU != TU");
		assertNotEquals(TD.Energy.LU, TD.Energy.CU, "LU != CU");
		assertNotEquals(TD.Energy.LU, TD.Energy.TU, "LU != TU");
		assertNotEquals(TD.Energy.CU, TD.Energy.TU, "CU != TU");
		// and none of them is the EU carrier (the cross-card mix-up face)
		assertNotEquals(TD.Energy.EU, TD.Energy.MU);
		assertNotEquals(TD.Energy.EU, TD.Energy.TU);
	}

	/** ⑤ an unknown name returns null AND mints nothing — the no-rogue-mint contract (the resolveEnergyType doc). */
	@Test
	void unknownTypeNamesReturnNullAndMintNothing() {
		int tCensusBefore = TagData.TAGS.size();
		assertNull(GTEnergySourceBlockEntity.resolveEnergyType("XY"));
		assertNull(GTEnergySourceBlockEntity.resolveEnergyType(""));
		assertNull(GTEnergySourceBlockEntity.resolveEnergyType(null));
		assertEquals(tCensusBefore, TagData.TAGS.size(), "the TAGS census unchanged — no rogue type minted");
		// and the four stay registered (nothing was lost either)
		assertTrue(TagData.TAGS.stream().anyMatch(t -> t == TD.Energy.MU), "MU stays a registered TagData");
		assertTrue(TagData.TAGS.stream().anyMatch(t -> t == TD.Energy.LU), "LU stays a registered TagData");
		assertTrue(TagData.TAGS.stream().anyMatch(t -> t == TD.Energy.CU), "CU stays a registered TagData");
		assertTrue(TagData.TAGS.stream().anyMatch(t -> t == TD.Energy.TU), "TU stays a registered TagData");
	}

	/** ⑥ the Jade short codes: the four W2 types map to their upstream aLocalShort words (GT6MachineProvider.java:84). */
	@Test
	void jadeShortCodesCarryTheW2Types() {
		Set<String> tCodes = Set.of(
				GT6MachineProvider.energyTypeShortCode(TD.Energy.MU),
				GT6MachineProvider.energyTypeShortCode(TD.Energy.LU),
				GT6MachineProvider.energyTypeShortCode(TD.Energy.CU),
				GT6MachineProvider.energyTypeShortCode(TD.Energy.TU));
		assertEquals(Set.of("MU", "LU", "CU", "TU"), tCodes,
				"the upstream aLocalShort words verbatim (root TD.java:123/:116/:109/:144)");
	}
}
