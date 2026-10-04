package gregtech6.registry;

import gregapi.data.MT;
import gregapi.oredict.MaterialRegistry;

/**
 * Test support (task hermetic-pour-tests): the hermetic material boot bracket, the r11e
 * reset-FIRST house rule (commit 443aaf8f3, the Mortar/B1 pour-test empirics). The neoforge
 * junit-fml leg boots the mod at JVM start (materials flooded, registry closed), so a bare
 * {@code MT.init()} there is a NO-OP (the MT.java:2709 NULL-presence guard) and the class
 * rides the boot generation plus whatever earlier classes leaked into it; on the offline
 * forge leg the same class runs on a cold JVM where an isolated run has no priming class at
 * all (the maxParallelForks=6 lottery — the GT6HopperFamilyTest solo red, known_bug
 * r11-hopper-mt-offline-latch). One {@code materials()} line in the class's
 * {@code @BeforeAll} makes the material universe class-local and order-independent:
 * a complete closed generation is kept (single-flood, below), anything else is
 * {@code reset()} — wiping the registry including any prior class's createMaterial
 * pollution — and re-opened, then {@code initMaterials()} floods a virgin generation and
 * closes.
 *
 * <p>The single-flood gate (task forge-order-pollution-hygiene): within one test fork a
 * second full flood would reassign every {@code MT.*} field to a fresh object set, and the
 * first generation is already frozen into MT-identity statics by whichever class touched
 * them earlier in the fork — {@code GT6WorldgenDatagen.LARGE_VEIN_TABLE/BEDROCK_ORE_TABLE}
 * (GT6WorldgenDatagen.java:1444/:1591, clinit at first table touch),
 * {@code OreDistributionInfo.ENTRIES/INDEX} (OreDistributionInfo.java:85-87, built once at
 * class init), {@code GT6VeinGenerator.sAxis} (GT6VeinGenerator.java:218-229). A mid-fork
 * reflood orphans those objects: victims read stale-table slots against fresh {@code MT.*}
 * and red out in rotating classes — the 159→215 double-pour, the double-Diamond identity,
 * the empty drawable list (known_bugs.forge-fullrun-order-reds, reproduced red at
 * /tmp/pophy_prefix_red_forge.log with the exact fingerprints). A complete closed
 * universe is a finished full flood by construction (initMaterials floods fully before
 * it closes), so keeping it is the order-independent form of the same hermeticity: every
 * class still sees one whole, unpolluted universe — just never a second one.
 */
public final class GT6MaterialTestSupport {

    private GT6MaterialTestSupport() {
    }

    /**
     * The hermetic bracket: reset FIRST (the r11e house rule), then the full initMaterials
     * refill — unless the registry already holds a complete closed generation, which must
     * be kept (the single-flood gate, the class javadoc).
     */
    public static void materials() {
        if (!MaterialRegistry.INSTANCE.isOpen() && MaterialRegistry.INSTANCE.MATERIAL_MAP.get("NULL") == MT.NULL) return;
        MaterialRegistry.INSTANCE.reset(); // wipe + re-open: the refill below can never ride boot residue
        GTMaterialItems.initMaterials(); // open -> MT.init full refill -> OP.init -> force-table -> close
    }
}
