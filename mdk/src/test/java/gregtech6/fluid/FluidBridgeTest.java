package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * FluidBridge skeleton offline tests (task p4-fluid-pipes spec ⑥): the empty-bridge
 * semantics. The seeded "iron" entry resolves only against a live registry (runServer
 * smoke evidence, acceptance ④) — offline the RegistryObject is unregistered, which the
 * null-path assertions exercise on purpose.
 */
public class FluidBridgeTest extends GTOfflineTestBase {

	@Test
	public void unknownMaterialsResolveToNull() {
		assertNull(FluidBridge.moltenFluidForMaterial("cobalt"), "empty-table entries stay null");
		assertNull(FluidBridge.moltenFluidForMaterial(null));
		assertNull(FluidBridge.moltenStack("cobalt", 2), "no stack for unknown materials");
		assertNull(FluidBridge.moltenStack("iron", 2, 144), "the seeded entry is unregistered offline");
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
