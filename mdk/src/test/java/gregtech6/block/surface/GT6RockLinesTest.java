package gregtech6.block.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The rock magnifier census pins (task easter-s4-rock-lines) — the upstream
 * MultiTileEntityRock.java:95-107 chat face as three pin groups over the pure seam
 * ({@link GT6RockLines}):
 * <ul>
 * <li>the TEN dimension quips + the two flint rows, string-verbatim (the census
 *     read directly off placeables/MultiTileEntityRock.java:95-107);</li>
 * <li>the dispatch: the vanilla trio by {@code Level.dimension()} key, the fallback
 *     for everything else (the six modded arms stay unreachable in this port — their
 *     LINES stay pinned above);</li>
 * <li>the Flintstones probability table: the divisor ternary (bday 10 / Xmas-in-July
 *     100 / plain 1000), the April-Fools short-circuit (the die never rolls), and the
 *     fresh-seed band walks (the MachineFaceFourTest inspectSound form).</li>
 * </ul>
 * Plus the date rows (CS.java:870-872 — the S2 local-degradation seam) and the stick
 * silence wire (upstream MultiTileEntityStick carries no onToolClick).
 */
class GT6RockLinesTest {

	@BeforeAll
	static void boot() {
		// the version detect must precede bootStrap (the GT6SurfaceVariantsTest recipe —
		// a bare-JVM first boot poisons DataFixers for every later suite in this JVM)
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline bootstrap noise is expected; the census seam needs no registries
		}
	}

	// ------------------------------------------------- the ten-dimension quip census

	@Test
	void theTenDimensionQuipsAreVerbatim() {
		assertEquals("This is definitely a Rack", GT6RockLines.NETHER, ":95 — the dimensionId -1 pun");
		assertEquals("This is definitely a Rock", GT6RockLines.OVERWORLD, ":96 — the dimensionId 0 row");
		assertEquals("There is definitely an End", GT6RockLines.END, ":97 — the dimensionId +1 row");
		assertEquals("Holy $#!T, it's a Rock..", GT6RockLines.AETHER, ":98 — the WD.dimAETHER row");
		assertEquals("Wait that Rock is alive?!", GT6RockLines.ALFHEIM, ":99 — the WD.dimALF row");
		assertEquals("Seems to be a Chunk o'Head", GT6RockLines.TROPIC, ":100 — the WD.dimTROPIC row");
		assertEquals("This is definitely not made of Cheese", GT6RockLines.MOON, ":101 — the BIOMES_MOON row");
		assertEquals("This is definitely from Mars", GT6RockLines.MARS, ":102 — the BIOMES_MARS row");
		assertEquals("This is definitely a Space Rock", GT6RockLines.SPACE, ":103 — the BIOMES_SPACE row");
		assertEquals("This definitely is a Rock", GT6RockLines.FALLBACK, ":104 — the word-order-flipped fallback");
	}

	@Test
	void theFlintRowLiteralsAreVerbatim() {
		assertEquals("It's a Flint", GT6RockLines.FLINT, ":107 — the quiet row");
		assertEquals("Flintstones, meet the Flintstones, they're the modern Stone Age family",
				GT6RockLines.FLINTSTONES, ":107 — the theme-song row");
	}

	@Test
	void theDimensionDispatchFollowsTheUpstreamLadder() {
		assertEquals(GT6RockLines.NETHER, GT6RockLines.dimensionLine(Level.NETHER), ":95 — dimensionId -1");
		assertEquals(GT6RockLines.OVERWORLD, GT6RockLines.dimensionLine(Level.OVERWORLD), ":96 — dimensionId 0");
		assertEquals(GT6RockLines.END, GT6RockLines.dimensionLine(Level.END), ":97 — dimensionId +1");
		ResourceKey<Level> tProbe = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("gt6", "probe_dim"));
		assertEquals(GT6RockLines.FALLBACK, GT6RockLines.dimensionLine(tProbe), ":104 — every other dimension falls through");
	}

	// ------------------------------------------------- the Flintstones probability table

	@Test
	void theDivisorTableFollowsTheDateFlags() {
		assertEquals(10, GT6RockLines.flintDivisor(true, false), ":107 — WOODMANS_BDAY 1/10");
		assertEquals(100, GT6RockLines.flintDivisor(false, true), ":107 — XMAS_IN_JULY 1/100");
		assertEquals(1000, GT6RockLines.flintDivisor(false, false), ":107 — the plain 1/1000");
		assertEquals(10, GT6RockLines.flintDivisor(true, true), ":107 — the bday ternary arm wins the tie");
	}

	@Test
	void aprilFoolsShortCircuitsTheDraw() {
		for (int i = 0; i < 256; i++) {
			assertEquals(GT6RockLines.FLINTSTONES, GT6RockLines.flintLine(RandomSource.create(i), true, false, false),
					":107 — APRIL_FOOLS || rng(...) : the theme song is constant, the die never rolls");
		}
	}

	@Test
	void theProbabilityBandsHold() {
		int tHits = 0;
		for (int i = 0; i < 1000; i++) {
			if (GT6RockLines.flintLine(RandomSource.create(i), false, true, false) == GT6RockLines.FLINTSTONES) tHits++;
		}
		assertTrue(tHits >= 60 && tHits <= 140, "the bday band ≈ 1/10 — saw " + tHits + "/1000");
		tHits = 0;
		for (int i = 0; i < 10000; i++) {
			if (GT6RockLines.flintLine(RandomSource.create(i), false, false, true) == GT6RockLines.FLINTSTONES) tHits++;
		}
		assertTrue(tHits >= 70 && tHits <= 130, "the Xmas-in-July band ≈ 1/100 — saw " + tHits + "/10000");
		tHits = 0;
		for (int i = 0; i < 10000; i++) {
			if (GT6RockLines.flintLine(RandomSource.create(i), false, false, false) == GT6RockLines.FLINTSTONES) tHits++;
		}
		assertTrue(tHits >= 3 && tHits <= 20, "the plain band ≈ 1/1000 — saw " + tHits + "/10000");
		tHits = 0;
		for (int i = 0; i < 64; i++) {
			if (GT6RockLines.flintLine(RandomSource.create(i), false, false, false) == GT6RockLines.FLINT) tHits++;
		}
		assertTrue(tHits > 0, "the quiet \"It's a Flint\" row is reachable");
	}

	// ------------------------------------------------- the CS date rows (the S2 seam)
	// The window truth (CS.java:870-873) is the GT6CalendarsTest pin (the landed S2 card);
	// this suite pins the CONSUMPTION face only: flintLine/flintDivisor over the passed
	// flags (the review-seat alignment folded the cut-time local date trio into
	// gregtech6.easter.GT6Calendars — the caller in GT6PlaceableBlock reads the flags).

	// ------------------------------------------------- the stick silence wire

	@Test
	void theStickSubclassOverridesTheCensusOff() throws Exception {
		assertNotEquals(GT6SurfaceRockBlock.class.getDeclaredMethod("magnifierLine", Level.class),
				GT6SurfaceStickBlock.class.getDeclaredMethod("magnifierLine", Level.class),
				"MultiTileEntityStick has no onToolClick — the stick overrides the census arm silent");
	}
}
