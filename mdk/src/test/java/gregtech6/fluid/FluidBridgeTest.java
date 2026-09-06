package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * FluidBridge skeleton offline tests (task p4-fluid-pipes spec ⑥): the empty-bridge
 * semantics. The null-path assertions exercise the MAP-MISS path (materials the bridge
 * never seeded): leg-neutral by construction — on the 21.1 leg the test JVM boots through
 * FML (GTOfflineTestBase javadoc :25-33), so the seeded "iron" holder is really bound and
 * the old "seeded entry unregistered offline" premise is unreachable there.
 */
public class FluidBridgeTest extends GTOfflineTestBase {

	@Test
	public void unknownMaterialsResolveToNull() {
		assertNull(FluidBridge.moltenFluidForMaterial("cobalt"), "empty-table entries stay null");
		assertNull(FluidBridge.moltenFluidForMaterial(null));
		assertNull(FluidBridge.moltenStack("cobalt", 2), "no stack for unknown materials");
		assertNull(FluidBridge.moltenStack("copper", 2, 144), "a never-seeded material is a map miss on both legs — null regardless of registry state");
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
