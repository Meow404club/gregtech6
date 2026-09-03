package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The 1.20.1-side pin for {@link GT6CapabilityWiring} — the wiring class itself is a
 * 1.21.1-only file (its 1.20.1 leg is empty), so its references cannot be read from here.
 * What CAN be pinned is the other half of every reference: the registry rows the wiring
 * resolves through its holder fields. If a row is renamed, this test fails here first.
 * (RegistryObject.getId() on 1.20.1 / DeferredHolder.getId() on 21.1 both read the name
 * field set at construction — no registry binding needed, the GT6ToolsCreativeTabTest
 * javadoc precedent.)
 */
public class GT6CapabilityWiringSeamTest extends GTOfflineTestBase {

	/** The eight BlockEntityType handles the 21.1 wiring dereferences, per registry row. */
	@Test
	public void beWiringPathsPinnedToRegistryRows() {
		assertEquals("shredder", GTMachines.SHREDDER_BE.getId().getPath());
		assertEquals("crusher", GTMachines.CRUSHER_BE.getId().getPath());
		assertEquals("lathe", GTMachines.LATHE_BE.getId().getPath());
		assertEquals("oven", GTMachines.OVEN_BE.getId().getPath());
		assertEquals("steam_engine", GTBlockEntities.STEAM_ENGINE_BE.getId().getPath());
		assertEquals("boiler_tank", GTBlockEntities.BOILER_TANK_BE.getId().getPath());
		assertEquals("multiblock_large_boiler", GTMultiBlocks.LARGE_BOILER_BE.getId().getPath());
		assertEquals("fluid_pipe", GTFluidPipes.FLUID_PIPE_BE.getId().getPath());
	}

	/**
	 * The 21.1 wiring wires FLUID_HANDLER_ITEM to every GTBarrelBlockItem via a registry
	 * CLASS scan (mirroring the Forge initCapabilities face — the capability rides the item
	 * class, not an id list); pin the family membership and its registration paths.
	 */
	@Test
	public void barrelItemFamilyCoversTheCarrierSeam() {
		assertEquals("barrel_wood", GTBarrels.BARREL_ITEM.getId().getPath());
		assertEquals("barrel_plastic", GTBarrels.BARREL_PLASTIC_ITEM.getId().getPath());
		assertEquals("barrel_metal", GTBarrels.BARREL_METAL_ITEM.getId().getPath());
		assertEquals("barrel_logistics", GTBarrels.BARREL_LOGISTICS_ITEM.getId().getPath());
		assertEquals(12, GTBarrels.METAL_DRUM_ITEMS.size(), "the p7 high-tier drum ladder");
		for (String tPath : GTBarrels.METAL_DRUM_ITEMS.keySet()) {
			assertTrue(tPath.startsWith("barrel_"), "drum row id " + tPath + " must stay in the barrel_* path family the wiring scan covers");
		}
	}
}
