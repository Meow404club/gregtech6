package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * FluidBridge offline tests (task fluid-pipes spec ⑥ skeleton; domain walk task
 * jade-molten-bridge-full): the outside-the-domain semantics. The null-path assertions
 * exercise the SEAM-MISS path (materials with no {@code gt6:<mat>_molten} row): leg-neutral
 * by construction — on the 21.1 leg the test JVM boots through FML (GTOfflineTestBase
 * javadoc :25-33), so domain rows are really bound there and the null can only come from
 * the convention lookup missing.
 */
public class FluidBridgeTest extends GTOfflineTestBase {

	@Test
	public void unknownMaterialsResolveToNull() {
		assertNull(FluidBridge.moltenFluidForMaterial("cobalt"), "no gt6:cobalt_molten row — the convention walk answers null");
		assertNull(FluidBridge.moltenFluidForMaterial(null));
		assertNull(FluidBridge.moltenStack("cobalt", 2), "no stack for unknown materials");
		assertNull(FluidBridge.moltenStack("copper", 2, 144), "a material outside the domain is a seam miss on both legs — null regardless of registry state");
	}

	@Test
	public void legacyFlShorthandIdsAliasToTheirTrueRows() {
		// task jade-molten-alias-seam: the five legacy FL shorthand rows ride the explicit
		// alias seam — material name (sanitized lowercase internal) → the TRUE registered id.
		assertEquals("plastic", FluidBridge.moltenIdForMaterial("plastic"),
				"Loader_Fluids.java:191 FL.create(\"plastic\", \"Molten Plastic\", MT.Plastic...) — no molten suffix at all");
		assertEquals("glass", FluidBridge.moltenIdForMaterial("glass"),
				"Loader_Fluids.java:192 FL.create(\"glass\", \"Molten Glass\", MT.Glass...) — no molten suffix at all");
		assertEquals("molten_latex", FluidBridge.moltenIdForMaterial("latex"),
				"Loader_Fluids.java:197 FL.create(\"molten.latex\", ...) — the prefix form kept (the :198 bare latex row is the BEE family)");
		assertEquals("molten_hsla", FluidBridge.moltenIdForMaterial("hslasteel"),
				"Loader_Fluids.java:199 FL.create(\"molten hsla\", ...) — the space folded to an underscore; HSLA-Steel sanitizes to HSLASteel");
		assertEquals("lithium_chloride_molten", FluidBridge.moltenIdForMaterial("lithiumchloride"),
				"FL.java:1077 createMolten over MT.LiCl (MT.java:1121 MOLTEN grant) — the underscore-carrying id ≠ the folded convention guess");
		// every other material keeps the specOf convention
		assertEquals("iron_molten", FluidBridge.moltenIdForMaterial("iron"));
		assertEquals("cobalt_molten", FluidBridge.moltenIdForMaterial("cobalt"));
	}

	@Test
	public void nonPositiveRequestsResolveToNull() {
		assertNull(FluidBridge.moltenStack("iron", 0), "zero units");
		assertNull(FluidBridge.moltenStack("iron", -1), "negative units");
		assertNull(FluidBridge.moltenStack("iron", 1, 0), "zero liters per unit");
	}

	@Test
	public void literConventionMatchesTheUpstreamBinding() {
		// FL.make("iron.molten", 144) — one material unit = 144 L (Loader_Fluids.java:161)
		assertEquals(144, FluidBridge.L_PER_MOLTEN_UNIT);
		// the long→int boundary behaviour matches the tank clamp
		assertEquals(Integer.MAX_VALUE, FluidTankGT.bindInt(144L * Integer.MAX_VALUE));
	}
}
