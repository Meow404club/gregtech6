package gregtech6.client.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;

import gregtech6.client.GTClientHandlers;
import gregtech6.registry.GTWires;
import gregtech6.registry.GTWireSpecs;

import gregapi.data.MT;
import gregtech6.registry.GT6MaterialTestSupport;

/**
 * Offline census pin for task redstone-wire-tint-reg — the wire tint registration seams
 * {@link GTClientHandlers#wireTintItems()} / {@link GTClientHandlers#wireTintBlocks()}:
 * the tab-全灰 root cause (research r11-redstone-wire) is the registration清单 not having
 * grown with the redstone family — the model/bake chain is complete
 * ({@code GTWireBakedModel} tintIndex 0/1, {@code GTWireClientListener.buildParams}
 * covers the six redstone item keys), only the final
 * {@code RegisterColorHandlersEvent} hookup misses the redstone 6 (3 rows × wire/cable,
 * Loader:1893-1902) and the laser fiber (Loader:1814-1815). The upstream colour face is
 * the grayscale wire.png × the runtime mRGBa (MultiTileEntityWireRedstone.java:81-87,
 * the fRGBaSolid default, TileEntityBase07Paintable.java:83-84) — {@link GTWireTint}
 * already speaks it; this pin drives the membership.
 *
 * <p>The item census runs BOTH legs (the {@code getId().getPath()} offline-safe form, the
 * ToolIdentityItemColorsCensusTest posture). The block census is 21.1-leg-only (the forge
 * leg's RegistryObject {@code .get()} throws offline — the GT6BeeHivesTest
 * {@code suppliersAreDeferredNotRun} guard — while the 21.1 test JVM boots FML so the
 * registry resolves there, {@code paintableBlockArrayHoldsTheWholeFamily} precedent).
 */
public class GTWireTintCensusTest {

    /** The 6 redstone ids, redstoneVariants() order (3 rows × wire/cable, GTWireSpecs:243-250). */
    private static final List<String> REDSTONE_CENSUS = List.of(
            "wire_red_alloy", "cable_red_alloy",
            "wire_signalum", "cable_signalum",
            "wire_lumium", "cable_lumium");

    /** The 1 laser id (Loader:1814-1815 — a single registration, no cable form). */
    private static final String LASER_ID = "wire_laser";

    @BeforeAll
    static void boot() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline; registries are ready by now.
        }
        GT6MaterialTestSupport.materials(); // the hermetic bracket (task hermetic-pour-tests)
    }

    /** Acceptance: the ItemColor registration covers the electric + redstone + laser families. */
    @Test
    public void itemCensusCoversTheRedstoneAndLaserFamilies() {
        List<String> tPaths = GTClientHandlers.wireTintItems().stream()
                .map(tRow -> tRow.getId().getPath()).toList();
        assertTrue(tPaths.containsAll(REDSTONE_CENSUS),
                "the redstone 6 ride the wire ItemColor registration: " + tPaths);
        assertTrue(tPaths.contains(LASER_ID),
                "the laser fiber rides the wire ItemColor registration: " + tPaths);
        assertEquals(2 + GTWireSpecs.EXPECTED_VARIANTS + REDSTONE_CENSUS.size() + 1, tPaths.size(),
                "the full census: the legacy pair + the 620 electric family + the redstone 6 + the laser");
    }

    //? if neoforge {
    /*// Acceptance: the BlockColor registration array is the electric + redstone + laser
    // union — the 21.1 leg only (see the class javadoc).
    @Test
    public void blockCensusCoversTheRedstoneAndLaserFamilies() {
        Block[] tTinted = GTClientHandlers.wireTintBlocks();
        assertEquals(2 + GTWireSpecs.EXPECTED_VARIANTS + GTWires.REDSTONE_BLOCKS.size()
                + GTWires.LASER_BLOCKS.size(), tTinted.length,
                "the world-half census: the electric list + the redstone 6 + the laser fiber, one array");
        for (Block tBlock : GTWires.redstoneBlockArray()) {
            assertTrue(contains(tTinted, tBlock), tBlock + " rides the wire BlockColor registration");
        }
        for (Block tBlock : GTWires.laserBlockArray()) {
            assertTrue(contains(tTinted, tBlock), tBlock + " rides the wire BlockColor registration");
        }
    }

    private static boolean contains(Block[] aArray, Block aBlock) {
        for (Block tBlock : aArray) if (tBlock == aBlock) return true;
        return false;
    }
    *///?}
}
