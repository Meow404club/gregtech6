package gregtech6.reactor.neutron;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The rod spec tables and rod behaviour pins (task debt-reactor-a-neutron-core):
 * the 17 fuel rows of Loader_MultiTileEntities.java:746-762 and the 4 breeder rows
 * of :782-785 transcribed full, plus the 8 rod-class behaviours of
 * {@link ReactorRodKind} (RodBase/RodAbsorber/RodReflector/RodModerator/
 * RodNuclear/RodBreeder/RodProduct/RodDepleted).
 */
public class ReactorRodTableTest {

	@Test
	public void fuelRodTableIsLmeVerbatim() {
		// LME:746-762 — {id, durability, self, other, div, max, depletedId}, registration order
		long[][] tExpected = {
			{9210, 12_000_000_000L,   2,   2, 32,   128, 9310},
			{9219, 12_000_000_000L,   2,   2, 32,    64, 9319},
			{9220,  6_000_000_000L,   4,   4, 16,   512, 9320},
			{9221,  1_200_000_000L,  32,  32,  4,  2048, 9321},
			{9222,  6_000_000_000L,  32,  32,  4,  2048, 9322},
			{9229,  6_000_000_000L,   4,   4, 16,   256, 9329},
			{9230,  1_200_000_000L,  64,  64,  4,  2048, 9330},
			{9231,  1_200_000_000L, 128, 128,  3,  3072, 9331},
			{9232,  1_200_000_000L, 128, 128,  3,  4096, 9332},
			{9233,  2_400_000_000L, 128, 128,  3,  4096, 9333},
			{9239,  1_200_000_000L,  64,  64,  4,  1024, 9339},
			{9240,  1_200_000_000L,  64,  64,  4,  4096, 9340},
			{9241,  1_200_000_000L, 128, 128,  3,  4096, 9341},
			{9249,  1_200_000_000L, 128, 128,  3,  3072, 9349},
			{9250,    120_000_000L,   8,   0, 16,   256, 9350},
			{9260, 12_000_000_000L, 128, 128,  4,  8192, 9360},
			{9261, 12_000_000_000L, 512, 512,  3, 16384, 9361}};
		assertEquals(17, tExpected.length);
		assertEquals(tExpected.length, FuelRodSpec.RODS.size());
		for (int i = 0; i < tExpected.length; i++) {
			FuelRodSpec tRod = FuelRodSpec.RODS.get(i);
			assertEquals(tExpected[i][0], tRod.id(), "row " + i + " id");
			assertEquals(tExpected[i][1], tRod.durability(), "row " + i + " durability");
			assertEquals(tExpected[i][2], tRod.self(), "row " + i + " self");
			assertEquals(tExpected[i][3], tRod.other(), "row " + i + " other");
			assertEquals(tExpected[i][4], tRod.div(), "row " + i + " div");
			assertEquals(tExpected[i][5], tRod.max(), "row " + i + " max");
			assertEquals(tExpected[i][6], tRod.depletedId(), "row " + i + " depletedId");
		}
	}

	@Test
	public void fuelRodDerivedTooltipValues() {
		// Nuclear.java:83 — minutes = durability / 120000: Th-232 12e9 -> 100000, Co-60 12e7 -> 1000
		assertEquals(100_000, FuelRodSpec.byId(9210).orElseThrow().advertisedMinutes());
		assertEquals(1_000, FuelRodSpec.byId(9250).orElseThrow().advertisedMinutes());
		// Nuclear.java:92 — div <= 4 is Critical: U-235 (4), Pu-241 (3), Naquadria (3);
		// U-238 (16) and Cyanite (32) are not
		assertTrue(FuelRodSpec.byId(9221).orElseThrow().critical());
		assertTrue(FuelRodSpec.byId(9231).orElseThrow().critical());
		assertTrue(FuelRodSpec.byId(9261).orElseThrow().critical());
		assertFalse(FuelRodSpec.byId(9220).orElseThrow().critical());
		assertFalse(FuelRodSpec.byId(9219).orElseThrow().critical());
	}

	@Test
	public void breederRodTableIsLmeVerbatim() {
		// LME:782-785 — {id, needed, loss, productId}
		long[][] tExpected = {
			{9410,    64_000_000L,  1000, 9411},
			{9420,   256_000_000L,  2500, 9421},
			{9430,      640_000L,   250, 9431},
			{9440, 4_096_000_000L, 10000, 9441}};
		assertEquals(4, tExpected.length);
		assertEquals(4, BreederRodSpec.RODS.size());
		for (int i = 0; i < tExpected.length; i++) {
			BreederRodSpec tRod = BreederRodSpec.RODS.get(i);
			assertEquals(tExpected[i][0], tRod.id(), "row " + i + " id");
			assertEquals(tExpected[i][1], tRod.needed(), "row " + i + " needed");
			assertEquals(tExpected[i][2], tRod.loss(), "row " + i + " loss");
			assertEquals(tExpected[i][3], tRod.productId(), "row " + i + " productId");
		}
	}

	@Test
	public void reactionHeatPerRodKind() {
		// Nuclear.java:205 (1 HU/neutron), Absorber.java:46 (2), Breeder.java:83 /
		// Product.java:55 (integer half), the other four rods add nothing
		assertEquals(1000, ReactorRodKind.FUEL.reactionHeat(1000));
		assertEquals(2000, ReactorRodKind.ABSORBER.reactionHeat(1000));
		assertEquals(500, ReactorRodKind.BREEDER.reactionHeat(1001));
		assertEquals(500, ReactorRodKind.BREEDER.reactionHeat(1000));
		assertEquals(500, ReactorRodKind.PRODUCT.reactionHeat(1001));
		assertEquals(0, ReactorRodKind.EMPTY.reactionHeat(1000));
		assertEquals(0, ReactorRodKind.DEPLETED.reactionHeat(1000));
		assertEquals(0, ReactorRodKind.REFLECTOR.reactionHeat(1000));
		assertEquals(0, ReactorRodKind.MODERATOR.reactionHeat(1000));
	}

	@Test
	public void reactionRunningFlagPerRodKind() {
		for (ReactorRodKind tKind : ReactorRodKind.values()) {
			boolean tExpected = tKind == ReactorRodKind.FUEL || tKind == ReactorRodKind.ABSORBER
				|| tKind == ReactorRodKind.BREEDER || tKind == ReactorRodKind.PRODUCT;
			assertEquals(tExpected, tKind.reactionKeepsCoreRunning(), tKind.name());
		}
	}

	@Test
	public void absorptionPerRodKind() {
		// Fuel/Absorber/Product take everything (Nuclear.java:235, Absorber.java:52, Product.java:61);
		// breeder subtracts the per-side loss but only from unmoderated emitters
		// (Breeder.java:97 — 251-250=1, 250 not > 250 -> 0, moderated -> 0)
		assertEquals(100, ReactorRodKind.FUEL.absorbedNeutrons(100, true, 0));
		assertEquals(100, ReactorRodKind.ABSORBER.absorbedNeutrons(100, false, 0));
		assertEquals(100, ReactorRodKind.PRODUCT.absorbedNeutrons(100, false, 0));
		assertEquals(1, ReactorRodKind.BREEDER.absorbedNeutrons(251, false, 250));
		assertEquals(0, ReactorRodKind.BREEDER.absorbedNeutrons(250, false, 250));
		assertEquals(0, ReactorRodKind.BREEDER.absorbedNeutrons(100, false, 250));
		assertEquals(0, ReactorRodKind.BREEDER.absorbedNeutrons(10_000, true, 250));
		assertEquals(0, ReactorRodKind.EMPTY.absorbedNeutrons(100, false, 0));
		assertEquals(0, ReactorRodKind.DEPLETED.absorbedNeutrons(100, false, 0));
		assertEquals(0, ReactorRodKind.REFLECTOR.absorbedNeutrons(100, false, 0));
		assertEquals(0, ReactorRodKind.MODERATOR.absorbedNeutrons(100, false, 0));
	}

	@Test
	public void reflectionPerRodKind() {
		// Reflector.java:51 — full bounce; Moderator.java:88 — latched count × n;
		// everything else returns 0
		assertEquals(64, ReactorRodKind.REFLECTOR.reflectedNeutrons(64, 0));
		assertEquals(192, ReactorRodKind.MODERATOR.reflectedNeutrons(64, 3));
		assertEquals(0, ReactorRodKind.MODERATOR.reflectedNeutrons(64, 0));
		assertEquals(0, ReactorRodKind.FUEL.reflectedNeutrons(64, 3));
		assertEquals(0, ReactorRodKind.ABSORBER.reflectedNeutrons(64, 3));
		assertEquals(0, ReactorRodKind.BREEDER.reflectedNeutrons(64, 3));
		assertEquals(0, ReactorRodKind.EMPTY.reflectedNeutrons(64, 3));
		assertEquals(0, ReactorRodKind.DEPLETED.reflectedNeutrons(64, 3));
	}

	@Test
	public void moderationFlagsPerRodKind() {
		// Moderator.java:84-87/:92-94 and Nuclear.java:231-234
		for (ReactorRodKind tKind : ReactorRodKind.values()) {
			assertEquals(tKind == ReactorRodKind.MODERATOR, tKind.countsModeratorTouch(), tKind.name());
			assertEquals(tKind == ReactorRodKind.FUEL, tKind.moderatedByTouch(), tKind.name());
			assertEquals(tKind == ReactorRodKind.MODERATOR, tKind.alwaysModerated(), tKind.name());
		}
	}
}
