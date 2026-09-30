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
