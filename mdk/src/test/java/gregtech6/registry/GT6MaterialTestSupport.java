package gregtech6.registry;

import gregapi.oredict.MaterialRegistry;

/**
 * Test support (task hermetic-pour-tests): the hermetic material boot bracket, the r11e
 * reset-FIRST house rule (commit 443aaf8f3, the Mortar/B1 pour-test empirics). The neoforge
 * junit-fml leg boots the mod at JVM start (materials flooded, registry closed), so a bare
 * {@code MT.init()} there is a NO-OP (the MT.java:2711 NULL-presence guard) and the class
 * rides the boot generation plus whatever earlier classes leaked into it; on the offline
 * forge leg the same class runs on a cold JVM where an isolated run has no priming class at
 * all (the maxParallelForks=6 lottery — the GT6HopperFamilyTest solo red, known_bug
 * r11-hopper-mt-offline-latch). One {@code materials()} line in the class's
 * {@code @BeforeAll} makes the material universe class-local and order-independent:
 * {@code reset()} wipes the registry (including any prior class's createMaterial pollution)
 * and re-opens it, then {@code initMaterials()} floods a virgin generation and closes.
 */
public final class GT6MaterialTestSupport {

    private GT6MaterialTestSupport() {
    }

    /** The hermetic bracket: reset FIRST (the r11e house rule), then the full initMaterials refill. */
    public static void materials() {
        MaterialRegistry.INSTANCE.reset(); // wipe + re-open: the refill below can never ride boot residue
        GTMaterialItems.initMaterials(); // open -> MT.init full refill -> OP.init -> force-table -> close
    }
}
